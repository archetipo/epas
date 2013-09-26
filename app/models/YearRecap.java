package models;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.criteria.Fetch;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditQuery;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import com.google.common.collect.Multiset.Entry;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;
import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.db.jpa.Model;


/**
 * 
 * @author dario
 * 
 * Per adesso la classe Year recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista.
 */
@Entity
@Table(name = "year_recaps")
public class YearRecap extends Model{

	private static final long serialVersionUID = -5721503493068567394L;

	@Required
	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;

	@Required
	@Column
	public short year;
	@Column
	public int remaining;
	@Column
	public int remainingAp;
	@Column
	public int recg;
	@Column
	public int recgap;
	@Column
	public int overtime;
	@Column
	public int overtimeAp;
	@Column
	public int recguap;
	@Column
	public int recm;
	@Column
	public Timestamp lastModified;

	@Transient
	private List<String> months = null;
	@Transient
	private Map<AbsenceType,Integer> mappaAssenze = new HashMap<AbsenceType,Integer>();

	@Transient
	private LocalDate beginYear;

	protected YearRecap(){


	}
	/**
	 * Construttore di default con i parametri obbligatori
	 * 
	 * @param person la persona associata al riepilogo annuale
	 * @param year l'anno di riferimento
	 * 
	 */
	public YearRecap(
			@NotNull Person person, 
			@Min(1970) short year
			) {
		this.person = person;
		this.year = year;	

	}

	/**
	 * Preleva dallo storage le il YearRecap relativo ai dati passati.
	 * Se il yearRecap non è presente sul db ritorna un'istanza vuota
	 * ma con associati i dati passati.
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @return il riepilogo annuale, se non è presente nello storage viene 
	 * 	restituito un riepilogo annuale vuoto
	 */
	public static YearRecap byPersonAndYear(
			@NotNull Person person, 
			@Min(1970) short year
			) {
		if (person == null) {
			throw new IllegalArgumentException("Person mandatory");
		}
		YearRecap yearRecap = YearRecap.find("byPersonAndYear", person, year).first();
		if (yearRecap == null) {
			return new YearRecap(person, year);
		}

		return yearRecap;
	}

	private LocalDate getBeginYear(){
		if(beginYear == null)
			beginYear = new LocalDate(year,1,1);
		return beginYear;
	}


	

	/**
	 * QUI INIZIA LA PARTE DI FUNZIONI RELATIVE AL CALCOLO DELLE FERIE IN UN ANNO, SIA TRA QUELLE PREVISTE DA CONTRATTO, SIA TRA QUELLE
	 * CHE NON SONO STATE UTILIZZATE TRA QUELLE DELL'ANNO PRECEDENTE.
	 */

	/**
	 * 
	 * @return il numero di giorni di ferie maturati nell'anno corrente con l'ausilio di un metodo di codifica privato
	 */
	public int vacationCurrentYear(int year){
		int days = 0;
		int vacationDays = 0;
		Contract contract = person.getContract(new LocalDate(year,1,1));
		//Contract contract = Contract.find("Select con from Contract con where con.person = ? order by beginContract desc", person).first();
		LocalDate beginContract = contract.beginContract;
		LocalDate now = new LocalDate().withYear(year);
		

		//	int difference = now.getYear()-beginContract.getYear();
		//	Logger.warn("difference is:" +difference+ "now.getYear is: "+now.getYear()+ "beginContract.getYear is: "+beginContract.getYear());
		if(now.getYear()-beginContract.getYear() < 3){
			if(now.getYear()-beginContract.getYear() < 1 && beginContract.getMonthOfYear() != 1 && beginContract.getDayOfMonth() != 1){
				LocalDate newDate = now.minusYears(beginContract.getYear()).minusMonths(beginContract.getMonthOfYear()).minusDays(beginContract.getDayOfMonth());
				days = newDate.getDayOfMonth();
				vacationDays = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days);

			}
			else{
				days = now.getDayOfYear();
				vacationDays = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days);
			}
		}
		else{
			days = now.getDayOfYear();
			vacationDays = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysMoreThreeYears(days);
		}
		Logger.debug("I giorni di ferie maturati per %s %s nell'anno %d sono %d", person.name, person.surname, year, vacationDays);
		return vacationDays;
	}

	/**
	 * 
	 * @return il numero di giorni di permesso legge maturati nell'anno corrente
	 */
	public int permissionCurrentYear(int year){
		int days = 0;
		int permissionDays = 0;
		LocalDate now = new LocalDate().withYear(year);
		
		days = now.getDayOfYear();

		permissionDays = VacationsPermissionsDaysAccrued.convertWorkDaysToPermissionDays(days);
		return permissionDays;
	}



	/**
	 * 
	 * @return il numero di giorni di ferie avanzati da quelli maturati l'anno precedente e non ancora utilizzati
	 */
	public Integer vacationLastYearNotYetUsed(int year){
		
		List<PersonDay> vacationsLastYear = listVacationDaysLastYear(year);		
		Logger.debug("Il numero di giorni dell'anno precedente usati è: %d", vacationsLastYear.size());
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		VacationPeriod period = null;
		
		Contract contract = person.getCurrentContract();		
		/**
		 * se l'anno precedente non c'era contratto ritorna zero.
		 */
		if(contract == null)
			return 0;
		int days = 0;
		/**
		 * se la data di inizio del contratto è precedente all'anno scorso
		 */
		if(contract.beginContract != null && contract.beginContract.getYear()<year-1){
			days = daysBetweenTwoDates(beginLastYear, endLastYear);
		}
		/**
		 * se la data di inizio contratto ricade nell'anno passato
		 */
		if(contract.beginContract != null && contract.beginContract.getYear()==year-1){
			days = daysBetweenTwoDates(contract.beginContract, endLastYear);
		}
		period = VacationPeriod.find("Select vp from VacationPeriod vp where vp.person = ? and ((vp.beginFrom <= ? and " +
				"vp.endTo >= ?) or (vp.endTo = null))", person, beginLastYear, endLastYear).first();
		if(period == null){
			period = VacationPeriod.find("Select vp from VacationPeriod vp where vp.person = ? and vp.beginFrom between ? and ? and vp.endTo >= ?", 
					person, beginLastYear, endLastYear, endLastYear).first();
		}
		Logger.debug("Anno passato come parametro: %d", year);
		Logger.debug("Period per %s %s: %s", person.name, person.surname, period.toString());
		
		//int vacationDaysAccrued = VacationsPermissionsDaysAccrued.convertWorkDaysToVacationDaysLessThreeYears(days);
		if(period.vacationCode != null){
			int residualVacationDays = period.vacationCode.vacationDays-vacationsLastYear.size();
			return residualVacationDays;
		}
		else 
			return null;
		
	}

	/**
	 * 
	 * @param begin
	 * @param end
	 * @return il numero di giorni tra due date
	 */
	private int daysBetweenTwoDates(LocalDate begin, LocalDate end){
		int diffInDays = (int)(end.toDate().getTime() - begin.toDate().getTime()) 
				/ (1000 * 60 * 60 * 24);
		return diffInDays;
	}

	/**
	 * 
	 * @return il numero di giorni di permesso che la persona ha da contratto
	 */
	public int personalPermission(){
		int permissionDays = 0;
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		permissionDays = vacCode.permissionDays;
		return permissionDays;
	}


	/**
	 * 
	 * @return i giorni di permesso che la persona ha utilizzato nell'anno 
	 * @param year
	 */
	public int personalPermissionUsed(int year){
		int permissionDays = 0;
		LocalDate now = new LocalDate().withYear(year);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, getBeginYear(), now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("94"))
				permissionDays++;
		}
		//		List<Absence> absence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? and " +
		//				"abs.date between ? and ? and abs.absenceType = abt and abt.code = ?", person, getBeginYear(), now, "94").fetch();
		//		permissionDays = absence.size();
		return permissionDays;
	}

	/**
	 * 
	 * @return il numero di giorni di permesso che la persona ha da contratto
	 */
	public int personalVacationDays(){
		int vacationDays = 0;
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		if(vacCode != null)
			vacationDays = vacCode.vacationDays;
		else 
			vacationDays = 0;
		return vacationDays;
	}

	/**
	 * 
	 * @param currentYear
	 * @return il numero di giorni di ferie presi l'anno precedente. Il numero di giorni di ferie corrisponde a tutte quelle giornate
	 * di assenza registrate sul database con codice 31, ovvero "ferie anno precedente", fatte nell'anno corrente, più le giornate
	 * di assenza registrate sul database con codice 32, ovvero "ferie anno corrente", fatte nell'anno precedente.
	 */
	public int vacationDaysLastYear(int year){
		int vacationDaysLastYear = 0;
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		LocalDate now = new LocalDate().minusYears(new LocalDate().getYear()-year);
		List<PersonDay> pdListPast = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginLastYear, endLastYear).fetch();
		for(PersonDay pd : pdListPast){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("32")){
				vacationDaysLastYear++;
			}
		}
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, getBeginYear(), now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("31")){
				vacationDaysLastYear++;
			}
		}

		return vacationDaysLastYear;
	}

	/**
	 * 
	 * @param currentYear
	 * @return la lista delle assenze che utilizzerò nella finestra di popup per elencare le date in cui sono state fatte le assenze
	 * 
	 */
	public List<PersonDay> listVacationDaysLastYear(int year){
		Logger.debug("Anno passato alla funzione listVacationDaysLastYear: %d", year);
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		LocalDate beginYear = new LocalDate(year, 1, 1);
		LocalDate now = new LocalDate().minusYears(new LocalDate().getYear()-year);
		List<PersonDay> vacations = new ArrayList<PersonDay>();
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginLastYear, endLastYear).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() > 0 && pd.absences.get(0).absenceType.code.equals("32"))
				vacations.add(pd);			
		}
		List<PersonDay> pdListNow = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginYear, now).fetch();
		for(PersonDay pd : pdListNow){

			if(pd.absences.size() > 0 && pd.absences.get(0).absenceType.code.equals("31"))
				vacations.add(pd);

		}
		//		Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? " +
		//				"and (abs.date between ? and ? and abs.absenceType = abt and abt.code = ? or abs.date between ? and ? and abs.absenceType = abt and abt.code = ?)", 
		//				person, beginLastYear, endLastYear, "32", getBeginYear(), now,  "31").fetch();
		//Logger.debug("Il numero di ferie dell'anno  scorso è: %s", vacations.size());
		return vacations;
	}

	/**
	 * 
	 * @return la lista delle assenze che utilizzerò nella finestra di popup per elencare le date in cui sono state fatte le assenze
	 * con codice "32"
	 *  
	 */
	public List<PersonDay> listVacationDaysCurrentYear(){
		LocalDate now = new LocalDate();
		List<PersonDay> vacations = new ArrayList<PersonDay>();
		Logger.debug("La persona di cui si chiedono le ferie di quest'anno è: %s %s", person.name, person.surname);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, getBeginYear(), now).fetch();
		
		for(PersonDay pd : pdList){
			
			if(pd.absences.size() > 0 && pd.absences.get(0).absenceType.code.equals("32")){
				vacations.add(pd);
				
			}
		}	
		//Logger.debug("Il numero di ferie di quest'anno è: %s", vacations.size());
		return vacations;
	}

	/**
	 * 
	 * @return il numero di giorni di ferire per l'anno corrente. Il numero di giorni di ferie corrisponde a tutte quelle giornate di
	 * assenza registrate sul database col codice 32 ovvero "ferie anno corrente"
	 */
	public int vacationDaysCurrentYear(){
		int vacationDaysCurrentYear = 0;
		LocalDate now = new LocalDate();
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?",
				person, getBeginYear(), now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() == 1 && pd.absences.get(0).absenceType.code.equals("32"))
				vacationDaysCurrentYear ++;
		}

		return vacationDaysCurrentYear;
	}

	@Override
	public String toString() {
		return String.format("YearRecap[%d] - person.id = %d, year = %d, overtime = %d, overtimeAp = %d, remaining = %d, remainingAp = %d, recg = %d, recgap = %d, " +
				"recguap = %d, recm = %d",
				id, person.id, year, overtime, overtimeAp, remaining, remainingAp, recg, recgap, recguap, recm);
	}

}
