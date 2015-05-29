package dao.wrapper;

import org.joda.time.YearMonth;

import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.VacationPeriod;
import models.WorkingTimeType;

import com.google.common.base.Optional;

/**
 * @author marco
 *
 */
public interface IWrapperPerson extends IWrapperModel<Person> {

	/**
	 * Il contratto attuale. Istanzia una variabile Lazy.
	 * 
	 * @return
	 */
	Optional<Contract> getCurrentContract();

	
	/**
	 * Il piano ferie attuale. Istanzia una variabile Lazy.
	 * 
	 * @return
	 */
	Optional<VacationPeriod> getCurrentVacationPeriod();

	
	/**
	 * Il tipo orario attuale. Istanzia una variabile Lazy.
	 * 
	 * @return
	 */
	Optional<WorkingTimeType> getCurrentWorkingTimeType();
	
	/**
	 * Il periodo del tipo orario attuale. Istanzia una variabile Lazy.
	 * 
	 * @return
	 */
	Optional<ContractWorkingTimeType> getCurrentContractWorkingTimeType();

	/**
	 * Il tipo timbratura attuale. Istanzia una variabile Lazy.
	 * 
	 * @return
	 */
	Optional<ContractStampProfile> getCurrentContractStampProfile();
	
	/**
	 * L'ultimo contratto attivo della persona nel mese. 
	 * 
	 * @param year
	 * @param month
	 * @return
	 */
	Optional<Contract> getLastContractInMonth(int year, int month);
	
	/**
	 * Il primo contratto attivo della persona nel mese. 
	 * 
	 * @param year
	 * @param month
	 * @return
	 */
	Optional<Contract> getFirstContractInMonth(int year, int month);
	
	/**
	 * L'ultimo mese con contratto attivo.
	 * 
	 * @return
	 */
	YearMonth getLastActiveMonth();
	
	/**
	 * True se la persona è passata da determinato a indeterminato durante l'anno.
	 * 
	 * @param year
	 * @return
	 */
	public boolean hasPassToIndefiniteInYear(int year);
	
	/**
	 * L'esito dell'invio attestati per la persona (null se non è ancora stato effettuato).
	 * @param year
	 * @param month
	 * @return 
	 */
	public CertificatedData getCertificatedData(int year, int month);
	
	/**
	 * Getter per la competenza della persona <CompetenceCode, year, month>
	 * @param code
	 * @return 
	 */
	public Competence competence(final CompetenceCode code, final int year, final int month);
	
	/**
	 * Il residuo positivo del mese fatto dalla person.
	 * @param year
	 * @param month
	 * @return 
	 */
	public Integer getPositiveResidualInMonth(int year, int month);
	
	/**
	 * Diagnostiche sui dati della persona.
	 * 
	 * @return
	 */
	public boolean currentContractInitializationMissing();
	public boolean currentContractMonthRecapMissing();

}
