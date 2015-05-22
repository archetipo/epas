package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import manager.ConfGeneralManager;
import manager.ContractMonthRecapManager;
import manager.PersonManager;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.VacationPeriod;
import models.enumerate.Parameter;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.VacationPeriodDao;

/**
 * @author marco
 *
 */
public class WrapperContract implements IWrapperContract {

	private final PersonManager personManager;
	private final Contract value;
	private final VacationPeriodDao vacationPeriodDao;
	private final ConfGeneralManager confGeneralManager;

	@Inject
	WrapperContract(@Assisted Contract contract, PersonManager personManager,
			VacationPeriodDao vacationPeriodDao,
			ConfGeneralManager confGeneralManager) {
		value = contract;
		this.personManager = personManager;
		this.vacationPeriodDao = vacationPeriodDao;
		this.confGeneralManager = confGeneralManager;
	}

	@Override
	public Contract getValue() {
		return value;
	}

	/**
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	@Override
	public boolean isLastInMonth(int month, int year) {
		
		List<Contract> contractInMonth = 
				personManager.getMonthContracts(this.value.person, month, year);
		if (contractInMonth.size() == 0) {
			return false;
		}
		if (contractInMonth.get(contractInMonth.size()-1).id.equals(this.value.id)){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * La lista dei VacationPeriod associati al contratto 
	 * in ordine crescente per data di inizio periodo.
	 * 
	 * @param contract
	 * @return
	 */
	@Override
	public List<VacationPeriod> getContractVacationPeriods() {

		List<VacationPeriod> vpList = vacationPeriodDao
				.getVacationPeriodByContract(this.value);
		return vpList;
	}

	/**
	 * True se il contratto è a tempo determinato.
	 * 
	 * @return
	 */
	@Override
	public boolean isDefined() {

		return this.value.expireContract != null;
	}

	/**
	 * Conversione della lista dei contractWorkingtimeType da Set a List
	 * @param contract
	 * * @return
	 */
	@Override
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
		return Lists.newArrayList(this.value.contractWorkingTimeType);
	}

	/**
	 * L'intervallo attivo per il contratto.
	 * 
	 * @return
	 */
	@Override
	public DateInterval getContractDateInterval() {
		if (value.endContract != null)
			return new DateInterval(value.beginContract, value.endContract);
		else
			return new DateInterval(value.beginContract, value.expireContract);
	}
	
	/**
	 * L'intervallo dei giorni da considerare per le computazioni nel database ePAS.
	 * 
	 */
	@Override 
	public DateInterval getContractDatabaseInterval() {
		
		// TODO: verificare il funzionamento.
		// Assumo che initUse in configurazione sia ininfluente perchè se definita
		// allora automaticamente deve essere definito sourceContract.
		
		DateInterval contractInterval = getContractDateInterval();
		if (value.sourceDate != null) {
			return new DateInterval(value.sourceDate.plusDays(1),
					contractInterval.getEnd());
		}
		return contractInterval;
	}
	
	/**
	 * Il mese del primo riepilogo esistente per il contratto.
	 * 
	 */
	@Override
	public YearMonth getFirstMonthToRecap() {
		if (value.sourceDate != null) {
			return new YearMonth(value.sourceDate);
		}
		return new YearMonth(value.beginContract);
	}
	
	/**
	 * Il mese dell'ultimo riepilogo esistente per il contratto (al momento 
	 * della chiamata).
	 * 
	 * @return
	 */
	@Override
	public YearMonth getLastMonthToRecap() {
		YearMonth currentMonth = new YearMonth(LocalDate.now());
		YearMonth lastMonth = new YearMonth( getContractDateInterval().getEnd() );
		if ( currentMonth.isAfter(lastMonth) ) {
			return lastMonth;
		}
		return currentMonth;
	}
	
	/**
	 * 
	 * @param yearMonth
	 * @return
	 */
	@Override
	public Optional<ContractMonthRecap> getContractMonthRecap(YearMonth yearMonth) {
		
		for (ContractMonthRecap cmr : value.contractMonthRecaps) {
			if ( cmr.year == yearMonth.getYear() 
					&& cmr.month == yearMonth.getMonthOfYear() ) {
				return Optional.fromNullable(cmr);
			}
		}
		return Optional.absent();
	}
	
	/**
	 * Diagnostiche sul contratto.
	 * 
	 * @return
	 */
	@Override
	public boolean initializationMissing() {
		
		// Se la data di inizio contratto è successiva alla data di utilizzo del 
		// programma allora ci sono problemi di inizializzazione.
		
		LocalDate officeInstallation = 
				new LocalDate(confGeneralManager.getFieldValue(
						Parameter.INIT_USE_PROGRAM, value.person.office));
		
		if( value.beginContract.isBefore(officeInstallation) 
				&& value.sourceDate == null) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean monthRecapMissing(Optional<YearMonth> yearMonth) {

		// Mese specifico
		if( yearMonth.isPresent()) {
			if ( getContractMonthRecap(yearMonth.get()).isPresent() ) {
				return false;
			} else {
				return true;
			}
		}
		
		// Se non ho tutti i riepiloghi mensili previsti
		YearMonth monthToCheck = getFirstMonthToRecap();
		YearMonth nowMonth = YearMonth.now();
		while( !monthToCheck.isAfter( nowMonth)) {
			// FIXME: renderlo efficiente, un dao che li ordina.
			if ( !getContractMonthRecap(monthToCheck).isPresent() ) {
				return true;
			}
			monthToCheck = monthToCheck.plusMonths(1);
		}

		return false;
	}
	
	@Override
	public boolean hasMonthRecapForVacationsRecap(int yearToRecap) {
		
		// se source date cade nell'anno non ho bisogno del recap.
		if( value.sourceDate != null 
				&& value.sourceDate.getYear() == yearToRecap) {
			return false;
		}
		// Altrimenti ho bisogno del riepilogo finale dell'anno precedente.
		Optional<YearMonth> yearMonthToCheck = 
				Optional.fromNullable( new YearMonth(yearToRecap-1, 12));
		if( monthRecapMissing(yearMonthToCheck) ) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean noRelevant() {
		
		LocalDate officeInstallation = 
				new LocalDate(confGeneralManager.getFieldValue(
						Parameter.INIT_USE_PROGRAM, value.person.office));
		
		if (officeInstallation.isAfter( getContractDateInterval().getEnd() )) {
			return true;
		}
		return false;
	}
	
}
