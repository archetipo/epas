package manager.recaps.personStamping;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.YearMonth;

import manager.ContractManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.AbsenceType;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampType;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.wrapper.IRecapWrapperFactory;
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

	private final PersonManager personManager;
	private final PersonStampingDayRecapFactory stampingDayRecapFactory;
	
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
			PersonResidualYearRecapFactory yearFactory,
			ContractManager contractManager,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			IWrapperFactory wrapperFactory,
			
			int year, int month, Person person) {
		
		//this.personDayManager = personDayManager;
		this.personManager = personManager;
		this.stampingDayRecapFactory = stampingDayRecapFactory;
		
		this.month = month;
		this.year = year;
		
		this.numberOfInOut = Math.max(MIN_IN_OUT_COLUMN, personDayManager.getMaximumCoupleOfStampings(person, year, month));

		//Costruzione dati da renderizzare
		
		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = personDayManager.getTotalPersonDayInMonth(person, year, month);
		
		//calcolo del valore valid per le stamping del mese (persistere??)
		for(PersonDay pd : totalPersonDays) {
			personDayManager.computeValidStampings(pd);
		}
		
		PersonStampingDayRecap.stampModificationTypeSet = Sets.newHashSet(); 
		PersonStampingDayRecap.stampTypeSet = Sets.newHashSet();
		for(PersonDay pd : totalPersonDays ) {
			PersonStampingDayRecap dayRecap = this.stampingDayRecapFactory.create(pd, this.numberOfInOut);
			this.daysRecap.add(dayRecap);
		}
		this.stampModificationTypeSet = PersonStampingDayRecap.stampModificationTypeSet;
		this.stampTypeSet = PersonStampingDayRecap.stampTypeSet;

		this.numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		this.numberOfMealTicketToUse = personDayManager.numberOfMealTicketToUse(person, year, month);
		this.numberOfMealTicketToRender = personDayManager.numberOfMealTicketToRender(person, year, month);
		this.basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		this.absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		List<Contract> monthContracts = this.personManager.getMonthContracts(person,month, year);
		
		for(Contract contract : monthContracts)
		{
			Optional<ContractMonthRecap> cmr = contractManager.getContractMonthRecap(contract, new YearMonth(year, month));
			if (cmr.isPresent())
				this.contractMonths.add(wrapperFactory.create(cmr.get()));
		}

		this.month_capitalized = DateUtility.fromIntToStringMonth(month);
		
		for(PersonDay pd : totalPersonDays)
			this.totalWorkingTime = this.totalWorkingTime + pd.timeAtWork;
		
	}
	
}
