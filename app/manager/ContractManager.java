package manager;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.VacationCodeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

/**
 * 
 * Manager per Contract
 * 
 * @author alessandro
 *
 */
public class ContractManager {

	@Inject
	public ContractManager(ConfGeneralManager confGeneralManager,
			ConsistencyManager consistencyManager, 
			PersonDayDao personDayDao,
			PersonDayManager personDayManager,
			ContractMonthRecapManager contractMonthRecapManager,
			IWrapperFactory wrapperFactory, 
			VacationCodeDao vacationCodeDao,
			ContractDao contractDao, 
			PersonDao personDao) {

		this.confGeneralManager = confGeneralManager;
		this.consistencyManager = consistencyManager;
		this.personDayDao = personDayDao;
		this.personDayManager = personDayManager;
		this.contractMonthRecapManager = contractMonthRecapManager;
		this.wrapperFactory = wrapperFactory;
		this.vacationCodeDao = vacationCodeDao;
		this.contractDao = contractDao;
		this.personDao = personDao;
	}

	private final ConfGeneralManager confGeneralManager;
	private final ConsistencyManager consistencyManager;
	private final PersonDayDao personDayDao;
	private final PersonDayManager personDayManager;
	private final ContractMonthRecapManager contractMonthRecapManager;
	private final IWrapperFactory wrapperFactory;
	private final VacationCodeDao vacationCodeDao;
	private final ContractDao contractDao;
	private final PersonDao personDao;
	
	private final static Logger log = LoggerFactory.getLogger(ContractManager.class);
	/**
	 * Validatore per il contratto. Controlla la consistenza delle date all'interno del contratto
	 * e la coerenza con gli altri contratti della persona.
	 * @param contract
	 * @return
	 */
	public boolean contractCrossFieldValidation(Contract contract) {

		if(contract.expireContract != null 
				&& contract.expireContract.isBefore(contract.beginContract))
			return false;

		if(contract.endContract != null 
				&& contract.endContract.isBefore(contract.beginContract))
			return false;

		if(contract.expireContract != null && contract.endContract != null 
				&& contract.expireContract.isBefore(contract.endContract))
			return false;

		if(!isProperContract(contract) ) 
			return false;

		return true;
	}

	/**
	 * Costruisce in modo controllato tutte le strutture dati associate al contratto
	 * appena creato passato come argomento.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * @param contract
	 */
	public void properContractCreate(Contract contract, WorkingTimeType wtt) {

		buildVacationPeriods(contract);

		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contract.beginContract;
		cwtt.endDate = contract.expireContract;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();

		ContractStampProfile csp = new ContractStampProfile();
		csp.contract = contract;
		csp.startFrom = contract.beginContract;
		csp.endTo = contract.expireContract;
		csp.fixedworkingtime = false;
		csp.save();
		contract.save();

	}

	/**
	 * Aggiorna in modo appropriato tutte le strutture dati associate al contratto modificato.
	 * (1) I piani ferie associati al contratto
	 * (2) Il periodo con tipo orario Normale per la durata del contratto
	 * (3) Il periodo con timbratura default impostata a false.
	 * 
	 * @param contract
	 */
	public void properContractUpdate(Contract contract) {

		buildVacationPeriods(contract);
		updateContractWorkingTimeType(contract);
		updateContractStampProfile(contract);
	}

	/**
	 * Ricalcola completamente tutti i dati del contratto da dateFrom a dateTo.
	 *  
	 * 1) CheckHistoryError 
	 * 2) Ricalcolo tempi lavoro
	 * 3) Ricalcolo riepiloghi annuali 
	 * 
	 * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. 
	 *   Se null ricalcola dall'inizio del contratto.
	 *   
	 * @param dateTo ultimo giorno coinvolto nel ricalcolo. 
	 *   Se null ricalcola fino alla fine del contratto (utile nel caso in cui si 
	 *   modifica la data fine che potrebbe non essere persistita)
	 */
	public void recomputeContract(Contract contract, LocalDate dateFrom, LocalDate dateTo) {

		// (0) Definisco l'intervallo su cui operare
		// Decido la data inizio
		String dateInitUse = confGeneralManager.getFieldValue(Parameter.INIT_USE_PROGRAM, contract.person.office);
		LocalDate initUse = new LocalDate(dateInitUse);
		LocalDate date = contract.beginContract;
		if(date.isBefore(initUse))
			date = initUse;
		DateInterval contractInterval = wrapperFactory.create(contract).getContractDatabaseInterval();
		if( dateFrom != null && contractInterval.getBegin().isBefore(dateFrom)) {
			contractInterval = new DateInterval(dateFrom, contractInterval.getEnd());
		}
		// Decido la data di fine
		if(dateTo != null && dateTo.isBefore(contractInterval.getEnd())) {
			contractInterval = new DateInterval(contractInterval.getBegin(), dateTo);
		}

		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
		LocalDate today = new LocalDate();
		log.info("CheckPersonDay (creazione ed history error) DA {} A {}", date, today);
		while(true) {
			log.debug("RecomputePopulate {}", date);

			if(date.isEqual(today))
				break;

			if(!DateUtility.isDateIntoInterval(date, contractInterval)) {
				date = date.plusDays(1);
				continue;
			}

			consistencyManager.checkPersonDay(contract.person, date);

			date = date.plusDays(1);

		}

		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		LocalDate actualMonth = contractInterval.getBegin().withDayOfMonth(1).minusMonths(1);
		LocalDate endMonth = new LocalDate().withDayOfMonth(1);

		log.debug("PopulatePersonDay (ricalcoli ed history error) DA {} A {}", actualMonth, endMonth);

		while( !actualMonth.isAfter(endMonth) )
		{
			List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(contract.person, actualMonth, Optional.fromNullable(actualMonth.dayOfMonth().withMaximumValue()), true);

			for(PersonDay pd : pdList){

				PersonDay pd1 = personDayDao.getPersonDayById(pd.id);

				log.debug("RecomputePopulate {}", pd1.date);	

				personDayManager.populatePersonDay(wrapperFactory.create(pd1));
			}

			actualMonth = actualMonth.plusMonths(1);
		}

		log.info("Calcolato il riepilogo per il contratto {}",contract);

		//(3) Ricalcolo dei riepiloghi mensili
		contractMonthRecapManager.populateContractMonthRecapByPerson(contract.person,
				new YearMonth(contractInterval.getBegin()));

	}

	/**
	 * Costruisce la struttura dei periodi ferie associati al contratto 
	 * applicando la normativa vigente.
	 * 
	 * @param contract
	 */
	private void buildVacationPeriods(Contract contract){

		//Tempo indeterminato, creo due vacatio 3 anni più infinito
		if(contract.expireContract == null)
		{

			VacationPeriod first = new VacationPeriod();
			first.beginFrom = contract.beginContract;
			first.endTo = contract.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = vacationCodeDao.getVacationCodeByDescription("26+4");
			//first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = contract;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = contract.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = vacationCodeDao.getVacationCodeByDescription("28+4");
			//second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract = contract;
			second.save();
			contract.save();
			return;
		}

		//Tempo determinato più lungo di 3 anni
		if(contract.expireContract.isAfter(contract.beginContract.plusYears(3).minusDays(1))){
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = contract.beginContract;
			first.endTo = contract.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = vacationCodeDao.getVacationCodeByDescription("26+4");
			//first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = contract;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = contract.beginContract.plusYears(3);
			second.endTo = contract.expireContract;
			second.vacationCode = vacationCodeDao.getVacationCodeByDescription("28+4");
			//second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract = contract;
			second.save();
			contract.save();
			return;
		}

		//Tempo determinato più corto di 3 anni
		VacationPeriod first = new VacationPeriod();
		first.beginFrom = contract.beginContract;
		first.endTo = contract.expireContract;
		first.contract = contract;
		first.vacationCode = vacationCodeDao.getVacationCodeByDescription("26+4");
		//first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
		first.save();
		contract.save();
	}

	/**
	 * Quando vengono modificate le date di inizio o fine del contratto 
	 * occorre rivedere la struttura dei periodi di tipo orario.
	 * (1)Eliminare i periodi non più appartenenti al contratto
	 * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 */
	private void updateContractWorkingTimeType(Contract contract){
		//Aggiornare i periodi workingTimeType
		//1) Cancello quelli che non appartengono più a contract
		List<ContractWorkingTimeType> toDelete = new ArrayList<ContractWorkingTimeType>();
		IWrapperContract wrappedContract = wrapperFactory.create(contract);
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType)
		{
			DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);
			if(DateUtility.intervalIntersection(wrappedContract.getContractDateInterval(), cwttInterval) == null)
			{
				toDelete.add(cwtt);
			}
		}
		for(ContractWorkingTimeType cwtt : toDelete)
		{
			cwtt.delete();
			contract.contractWorkingTimeType.remove(cwtt);
			contract.save();
		}

		//Conversione a List per avere il metodo get()
		List<ContractWorkingTimeType> cwttList = Lists.newArrayList(contract.contractWorkingTimeType);

		//Sistemo il primo		
		ContractWorkingTimeType first = cwttList.get(0);
		first.beginDate = wrappedContract.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractWorkingTimeType last = 
				cwttList.get(contract.contractWorkingTimeType.size()-1);
		last.endDate = wrappedContract.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endDate))
			last.endDate = null;
		last.save();
		contract.save();
	}

	/**
	 * Quando vengono modificate le date di inizio o fine del contratto 
	 * occorre rivedere la struttura dei periodi di stampProfile.
	 * (1)Eliminare i periodi non più appartenenti al contratto
	 * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
	 * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
	 * 
	 */
	private void updateContractStampProfile(Contract contract){
		//Aggiornare i periodi stampProfile
		//1) Cancello quelli che non appartengono più a contract
		List<ContractStampProfile> toDelete = new ArrayList<ContractStampProfile>();
		IWrapperContract wrappedContract = wrapperFactory.create(contract);
		for(ContractStampProfile csp : contract.contractStampProfile)
		{
			DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.intervalIntersection(wrappedContract.getContractDateInterval(), cspInterval) == null)
			{
				toDelete.add(csp);
			}
		}
		for(ContractStampProfile csp : toDelete)
		{
			csp.delete();
			contract.contractWorkingTimeType.remove(csp);
			contract.save();
		}

		//Conversione a List per avere il metodo get()
		List<ContractStampProfile> cspList = Lists.newArrayList(contract.contractStampProfile);

		//Sistemo il primo		
		ContractStampProfile first = cspList.get(0);
		first.startFrom = wrappedContract.getContractDateInterval().getBegin();
		first.save();
		//Sistemo l'ultimo
		ContractStampProfile last = 
				cspList.get(contract.contractStampProfile.size()-1);
		last.endTo = wrappedContract.getContractDateInterval().getEnd();
		if(DateUtility.isInfinity(last.endTo))
			last.endTo = null;
		last.save();
		contract.save();
	}

	/**
	 * Il ContractWorkingTimeType a cui appartiene la data.
	 * @param date
	 * @return
	 */
	public ContractWorkingTimeType getContractWorkingTimeTypeFromDate(Contract contract, LocalDate date) {

		for(ContractWorkingTimeType cwtt: contract.contractWorkingTimeType) {

			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate) ))
				return cwtt;
		}
		return null;
	}


	/**
	 * Conversione della lista dei contractStampProfile da Set a List
	 * @param contract
	 * @return
	 */
	public List<ContractStampProfile> getContractStampProfileAsList(Contract contract) {

		return Lists.newArrayList(contract.contractStampProfile);
	}

	/**
	 * La lista con tutti i contratti attivi nel periodo selezionato.
	 * @return
	 */
	public List<Contract> getActiveContractInPeriod(LocalDate begin, LocalDate end) {

		if(end == null)
			end = new LocalDate(9999,1,1);

		List<Contract> activeContract = contractDao.getActiveContractsInPeriod(begin, end);

		return activeContract;

	}

	/**
	 * True se il contratto non si interseca con nessun altro contratto per la persona. False altrimenti
	 * @return
	 */
	public boolean isProperContract(Contract contract) {

		DateInterval contractInterval = wrapperFactory.create(contract).getContractDateInterval();
		for(Contract c : contract.person.contracts) {

			if(contract.id != null && c.id.equals(contract.id)) {
				continue;
			}

			if(DateUtility.intervalIntersection(contractInterval, wrapperFactory.create(c).getContractDateInterval()) != null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param contract
	 */
	public void saveSourceContract(Contract contract){
		if(contract.sourceVacationLastYearUsed==null) contract.sourceVacationLastYearUsed=0;
		if(contract.sourceVacationCurrentYearUsed==null) contract.sourceVacationCurrentYearUsed=0;
		if(contract.sourcePermissionUsed==null) contract.sourcePermissionUsed=0;
		if(contract.sourceRemainingMinutesCurrentYear==null) contract.sourceRemainingMinutesCurrentYear=0;
		if(contract.sourceRemainingMinutesLastYear==null) contract.sourceRemainingMinutesLastYear=0;
		if(contract.sourceRecoveryDayUsed==null) contract.sourceRecoveryDayUsed=0;

		contract.save();

	}

	/**
	 * 
	 * @param dataInizio
	 * @param dataFine
	 * @param onCertificate
	 * @param person
	 * @param wtt
	 * @return una stringa contenente il messaggio da passare al template nel caso di impossibilità a inserire un contratto nuovo.
	 * Stringa vuota altrimenti che corrisponde al corretto inserimento del contratto
	 */
	public String saveContract(LocalDate dataInizio, LocalDate dataFine, boolean onCertificate, Person person, WorkingTimeType wtt){
		String result = "";
		Contract contract = new Contract();
		contract.beginContract = dataInizio;
		contract.expireContract = dataFine;
		contract.onCertificate = onCertificate;
		contract.person = person;

		//Date non si sovrappongono con gli altri contratti della persona
		if( !isProperContract(contract) ) {

			result = "Il nuovo contratto si interseca con contratti precedenti. Controllare le date di inizio e fine. Operazione annullata.";
			return result;
		}

		contract.save();
		properContractCreate(contract, wtt);
		return result;
	}


	/**
	 * Utilizzata nel metodo delete del controller Persons, elimina contratti, orari di lavoro e stamp profile
	 * @param person
	 */
	public void deletePersonContracts(Person person){
		List<Contract> helpList = contractDao.getPersonContractList(person);
		for(Contract c : helpList){

			log.debug("Elimino contratto di {} che va da {} a {}", 
					new Object[]{person.getFullname(), c.beginContract, c.expireContract});

			// Eliminazione orari di lavoro
			List<ContractWorkingTimeType> cwttList = contractDao.getContractWorkingTimeTypeList(c);
			for(ContractWorkingTimeType cwtt : cwttList){
				cwtt.delete();
			}
			// Eliminazione stamp profile
			List<ContractStampProfile> cspList = contractDao.getPersonContractStampProfile(Optional.<Person>absent(), Optional.fromNullable(c));
			for(ContractStampProfile csp : cspList){
				csp.delete();
			}

			c.delete();
			person = personDao.getPersonById(person.id);
			person.contracts.remove(c);

			person.save();

		}
	}

	/**
	 * utilizzata nel metodo delete del controller Persons, elimina le eventuali inizializzazioni di tempi e assenze
	 * @param person
	 */
	public void deleteInitializations(Person person){
		for(InitializationAbsence ia : person.initializationAbsences){
			long id = ia.id;
			ia = contractDao.getInitializationAbsenceById(id);
			ia.delete();
		}
		for(InitializationTime ia : person.initializationTimes){
			long id = ia.id;
			ia = contractDao.getInitializationTimeById(id);
			ia.delete();
		}
	}


}
