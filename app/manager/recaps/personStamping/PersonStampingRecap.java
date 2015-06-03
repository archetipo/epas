package manager.recaps.personStamping;

import it.cnr.iit.epas.DateUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import manager.ContractManager;
import manager.ContractMonthRecapManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.AbsenceType;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeValue;
import models.StampType;
import models.Stamping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;

/**
 * Oggetto che modella il contenuto della vista contenente il tabellone timbrature.
 * Gerarchia:
 * PersonStampingRecap (tabella mese) 
 * 	  -> PersonStampingDayRecap (riga giorno) 
 * 		-> StampingTemplate (singola timbratura)
 * 
 * @author alessandro
 *
 */
public class PersonStampingRecap {

	private static final int MIN_IN_OUT_COLUMN = 2;

	public Person person;
	public int year;
	public int month;

	//Informazioni sul mese
	public int numberOfCompensatoryRestUntilToday = 0;
	public int numberOfMealTicketToRender = 0;
	public int numberOfMealTicketToUse = 0;
	public int basedWorkingDays = 0;
	public int totalWorkingTime = 0;

	//I riepiloghi di ogni giorno
	public List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();

	//I riepiloghi codici sul mese
	public Set<StampModificationType> stampModificationTypeSet = Sets.newHashSet();
	public Set<StampType> stampTypeSet = Sets.newHashSet();
	public Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();

	//I riepiloghi mensili (uno per ogni contratto attivo nel mese)
	public List<IWrapperContractMonthRecap> contractMonths = Lists.newArrayList();
	
	//Template
	public String month_capitalized;	//FIXME toglierlo e metterlo nel messages
	public int numberOfInOut = 0;

	/**
	 * @param personDayManager
	 * @param personManager
	 * @param yearFactory
	 * @param year
	 * @param month
	 * @param person
	 */
	public PersonStampingRecap(PersonDayManager personDayManager,
			PersonManager personManager,
			ContractMonthRecapManager contractMonthRecapManager,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			IWrapperFactory wrapperFactory,
			DateUtility dateUtility,
			int year, int month, Person person) {
		
		this.month = month;
		this.year = year;

		this.numberOfInOut = Math.max(MIN_IN_OUT_COLUMN, personDayManager.getMaximumCoupleOfStampings(person, year, month));

		//Costruzione dati da renderizzare
		
		//Contratti del mese
		List<Contract> monthContracts = personManager.getMonthContracts(person,month, year);
		for(Contract contract : monthContracts) {
			Optional<ContractMonthRecap> cmr = wrapperFactory.create(contract)
					.getContractMonthRecap(new YearMonth(year, month));
			if (cmr.isPresent()) {
				this.contractMonths.add(wrapperFactory.create(cmr.get()));
			}
		}

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = personDayManager.getTotalPersonDayInMonth(person, year, month);

		LocalDate today = LocalDate.now();
		//calcolo del valore valid per le stamping del mese (persistere??)
		for(PersonDay pd : totalPersonDays) {
			personDayManager.computeValidStampings(pd);

			PersonStampingDayRecap dayRecap = stampingDayRecapFactory
					.create(pd, this.numberOfInOut, monthContracts);
			this.daysRecap.add(dayRecap);

			this.totalWorkingTime = this.totalWorkingTime + pd.timeAtWork;


			if(stampingDayRecapFactory.wrapperFactory.create(pd).isFixedTimeAtWork()){
				StampModificationType smt = stampingDayRecapFactory.stampingDao.getStampModificationTypeById(
						StampModificationTypeValue.FIXED_WORKINGTIME.getId());

				stampModificationTypeSet.add(smt);
			}

			if(pd.date.equals(today) && !pd.isHoliday && !personDayManager.isAllDayAbsences(pd)){

				StampModificationType smt = stampingDayRecapFactory.stampingDao
						.getStampModificationTypeById(StampModificationTypeValue.ACTUAL_TIME_AT_WORK.getId());
				stampModificationTypeSet.add(smt);
			}
			if(pd.stampModificationType!=null && !pd.date.isAfter(today)){

				stampModificationTypeSet.add(pd.stampModificationType);
			}

			//this.stampModificationTypeSet.add(day.stampModificationType);

			for(Stamping stamp : pd.stampings){

				if(stamp.stampType!=null && stamp.stampType.identifier!=null){

					stampTypeSet.add(stamp.stampType);
				}

				if(stamp.markedByAdmin){

					StampModificationType smt = stampingDayRecapFactory.stampingDao.getStampModificationTypeById(StampModificationTypeValue.MARKED_BY_ADMIN.getId());
					stampModificationTypeSet.add(smt);
				}

				Optional<StampModificationType> smtMidnight = 
						personDayManager.checkMissingExitStampBeforeMidnight(stamp);

				if( smtMidnight.isPresent() ) {

					stampModificationTypeSet.add(smtMidnight.get());
				}

				//this.stampTypeSet.add(stamp.stampType);
				//this.stampModificationTypeSet.add(stamp.stampModificationType);
			}


		}

		this.numberOfCompensatoryRestUntilToday = personManager.numberOfCompensatoryRestUntilToday(person, year, month);
		this.numberOfMealTicketToUse = personDayManager.numberOfMealTicketToUse(person, year, month);
		this.numberOfMealTicketToRender = personDayManager.numberOfMealTicketToRender(person, year, month);
		this.basedWorkingDays = personManager.basedWorkingDays(totalPersonDays);
		this.absenceCodeMap = personManager.getAllAbsenceCodeInMonth(totalPersonDays);

		

		this.month_capitalized = DateUtility.fromIntToStringMonth(month);

	}

	//	private void calculateStampAndModificationTypeUsed(List<PersonDay> personDays){
	//
	//		LocalDate today = LocalDate.now();
	//
	//		for(PersonDay day : personDays){
	//
	//
	//		}
	//
	//	}

}
