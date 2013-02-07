package models;

import it.cnr.iit.epas.PersonUtility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

import lombok.Cleanup;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import models.Stamping.WayType;

/**
 * @author cristian
 *
 */
@Audited
@Table(name="person_months")
@Entity
public class PersonMonth extends Model {

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;	

	@Column
	public Integer year;
	@Column
	public Integer month;

	@Transient
	private PersonMonth mesePrecedenteCache;
	
	@Column(name = "recuperi_ore_da_anno_precedente")
	public int recuperiOreDaAnnoPrecedente;
	
	@Column(name = "riposi_compensativi_da_anno_precedente")
	public int riposiCompensativiDaAnnoPrecedente;
	
	@Column(name = "riposi_compensativi_da_anno_corrente")
	public int riposiCompensativiDaAnnoCorrente;
	
	@Column
	public int straordinari;
	
	/**
	 * Minuti derivanti dalla somma dei progressivi giornalieri del mese 
	 */
	@Column
	@Deprecated
	public Integer progressiveAtEndOfMonthInMinutes = 0;

	/**
	 * Totale residuo minuti alla fine del mese
	 * 
	 * Per i livelli I - III, deriva da:
	 * 
	 *  progressiveAtEndOfMonthInMinutes + 
	 *  residuo mese precedente (che non si azzera all'inizio dell'anno) -
	 *  recuperi (codice 91)
	 * 
	 *  
	 *  Per i livelli IV - IX, deriva da:
	 *  
	 *  progressiveAtEndOfMonthInMinutes +
	 *  residuo anno precedente (totale anno precedente se ancora utilizzabile) -
	 *  remainingMinutePastYearTaken -
	 *  totale remainingMinutePastYearTaken dei mesi precedenti a questo +
	 *  residuo mese precedente -
	 *  recuperi (91) -
	 *  straordinari (notturni, diurni, etc)  
	 */
	@Column(name = "total_remaining_minutes")
	@Deprecated
	public Integer totalRemainingMinutes = 0;

	/**
	 * Minuti di tempo residuo dell'anno passato utilizzati questo
	 * mese (come riposo compensativo o come ore in negativo)
	 */
	@Column(name = "remaining_minute_past_year_taken")
	public Integer remainingMinutesPastYearTaken = 0;

	@Column(name = "compensatory_rest_in_minutes")
	public Integer compensatoryRestInMinutes = 0;

	@Column(name = "residual_past_year")
	public Integer residualPastYear = 0;

	@Transient
	public List<PersonDay> days = null;

	@Transient
	private Map<AbsenceType, Integer> absenceCodeMap;

	@Transient
	private List<StampModificationType> stampingCodeList = new ArrayList<StampModificationType>();

	/**
	 * aggiunta la date per test di getMaximumCoupleOfStampings ---da eliminare
	 * @param person
	 * @param year
	 * @param month
	 */
	public PersonMonth(Person person, int year, int month){
		this.person = person;	
		this.year = year;
		this.month = month;

	}



	/**
	 * 
	 * @param month, year
	 * @return il residuo di ore all'ultimo giorno del mese se visualizzo un mese passato, al giorno attuale se visualizzo il mese
	 * attuale, ovvero il progressivo orario all'ultimo giorno del mese (se passato) o al giorno attuale (se il mese è quello attuale)
	 */
	@Deprecated
	public int getMonthResidual(){
		int residual = 0;
		LocalDate date = new LocalDate();

		if(month == date.getMonthOfYear() && year == date.getYear()){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date < ? and pd.progressive != ? " +
					"order by pd.date desc", person, date, 0).first();
			if(pd == null){
				pd = new PersonDay(person, date.minusDays(1));
			}
			residual = pd.progressive;
		}
		else{
			LocalDate hotDate = new LocalDate(year,month,1).dayOfMonth().withMaximumValue();
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date <= ? and pd.date > ?" +
					" order by pd.date desc", person, hotDate, hotDate.dayOfMonth().withMinimumValue()).first();
			if(pd == null){
				/**
				 * si sta cercando il personDay di una data ancora non realizzata (ad esempio il personDay dell'ultimo giorno di un mese ancora da 
				 * completare...es.: siamo al 4 gennaio 2013 e si cerca il personDay del 31 gennaio, che ancora non è stato realizzato
				 */
				residual = 0;
			}
			else
				residual = pd.progressive;

		}

		return residual;
	}

	/**
	 * 
	 * @param month
	 * @param year
	 * @return il numero di minuti di riposo compensativo utilizzati in quel mese 
	 */
	@Deprecated
	public int getCompensatoryRestInMinutes(){

		int compensatoryRest = getCompensatoryRest();

		Logger.debug("NUmero di giorni di riposo compensativo nel mese: %s", compensatoryRest);
		int minutesOfCompensatoryRest = compensatoryRest * person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(1).workingTime;
		if(minutesOfCompensatoryRest != compensatoryRestInMinutes && compensatoryRestInMinutes != null){
			compensatoryRestInMinutes = minutesOfCompensatoryRest;
			save();
		}

		return compensatoryRestInMinutes;

	}

	/**
	 * 
	 * @return il numero di giorni di riposo compensativo nel mese
	 */
	@Deprecated
	public int getCompensatoryRest(){
		int compensatoryRest = 0;
		LocalDate beginMonth = new LocalDate(year, month, 1);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginMonth, beginMonth.dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() > 0){
				for(Absence abs : pd.absences){
					if(abs.absenceType.code.equals("91"))
						compensatoryRest = compensatoryRest +1;
				}
			}
		}
		return compensatoryRest;
	}

	/**
	 * 
	 * @param month
	 * @param year
	 * @return il totale derivante dalla differenza tra le ore residue e le eventuali ore di riposo compensativo
	 */
	@Deprecated
	public int getTotalOfMonth(){
		int total = 0;
		int compensatoryRest = getCompensatoryRestInMinutes();
		//Logger.debug("CompensatoryRest in getTotalOfMonth: %s", compensatoryRest);
		int monthResidual = getMonthResidual();
		//Logger.debug("MonthResidual in getTotalOfMonth: %s", monthResidual);
		LocalDate date = new LocalDate(year, month, 1);
		/**
		 * TODO: devo farlo qui il controllo di quale sia la qualifica per poter aggiungere o meno il valore del residuo dell'anno precedente!?!?!?
		 */
		int residualFromPastMonth = PersonUtility.getResidual(person, date.dayOfMonth().withMaximumValue());

		total = residualFromPastMonth+monthResidual-(compensatoryRest); 

		return total;
	}



	/**
	 * 
	 * @return il numero massimo di coppie di colonne ingresso/uscita ricavato dal numero di timbrature di ingresso e di uscita di quella
	 * persona per quel mese
	 */
	public long getMaximumCoupleOfStampings(){
		//EntityManager em = em();
		LocalDate begin = new LocalDate(year, month, 1);

		List<PersonDay> personDayList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, begin, begin.dayOfMonth().withMaximumValue()).fetch();
		int maxExitStamp = 0;
		int maxInStamp = 0;
		for(PersonDay pd : personDayList){
			int localMaxExitStamp = 0;
			int localMaxInStamp = 0;
			for(Stamping st :pd.stampings){
				if(st.way == WayType.out)
					localMaxExitStamp ++;
				if(st.way == WayType.in)
					localMaxInStamp ++;
			}
			if(localMaxExitStamp > maxExitStamp)
				maxExitStamp = localMaxExitStamp;			
			if(localMaxInStamp > maxInStamp)
				maxInStamp = localMaxInStamp;
		}
		return Math.max(maxExitStamp, maxInStamp);

	}



	/**
	 * @return la lista di giorni (PersonDay) associato alla persona nel mese di riferimento
	 */
	public List<PersonDay> getDays() {

		if (days != null) {
			return days;
		}
		days = new ArrayList<PersonDay>();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		//Nel calendar i mesi cominciano da zero
		firstDayOfMonth.set(year, month - 1, 1);

		Logger.trace(" %s-%s-%s : maximum day of month = %s", 
				year, month, 1, firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH));

		for (int day = 1; day <= firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {

			Logger.trace("generating PersonDay: person = %s, year = %d, month = %d, day = %d", person.username, year, month, day);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
					person, new LocalDate(year, month, day)).first();
			if(pd == null)
				days.add(new PersonDay(person, new LocalDate(year, month, day), 0, 0, 0));
			else
				days.add(pd);
			Logger.debug("Inserito in days il person day: %s", pd);
		}
		return days;
	}	

	/**
	 * 
	 * !!!!IMPORTANTE!!!! QUANDO SI PASSA A UN NUOVO CONTRATTO NELL'ARCO DI UN MESE, DEVO RICORDARMI DI AZZERARE I RESIDUI DELLE ORE PERCHÈ
	 * NON SONO CUMULABILI CON QUELLE CHE EVENTUALMENTE AVRÒ COL NUOVO CONTRATTO
	 * 
	 * 
	 */

	/**
	 * Aggiorna le variabili d'istanza in funzione dei valori presenti sul db.
	 * Non effettua il salvataggio sul database.
	 */
	@Deprecated
	public void refreshPersonMonth(){

		Configuration config = Configuration.getCurrentConfiguration();

		LocalDate date = new LocalDate(year, month, 1);
		PersonDay lastPersonDayOfMonth = 
				PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date <= ? and pd.date > ? ORDER BY pd.date DESC",
						person, date.dayOfMonth().withMinimumValue(), date.dayOfMonth().withMaximumValue()).first();

		Logger.trace("%s, lastPersonDayOfMonth = %s", toString(), lastPersonDayOfMonth);
		if(lastPersonDayOfMonth != null)
			progressiveAtEndOfMonthInMinutes = lastPersonDayOfMonth.progressive;  
		this.merge();
		LocalDate startOfMonth = new LocalDate(year, month, 1);

		List<Absence> compensatoryRestAbsences = JPA.em().createQuery(
				"SELECT a FROM Absence a JOIN a.personDay pd WHERE a.absenceType.compensatoryRest IS TRUE AND pd.date BETWEEN :startOfMonth AND :endOfMonth AND pd.person = :person")
				.setParameter("startOfMonth", startOfMonth)
				.setParameter("endOfMonth", startOfMonth.dayOfMonth().withMaximumValue())
				.setParameter("person", person)
				.getResultList();

		compensatoryRestInMinutes = 0;

		for (Absence absence : compensatoryRestAbsences) {
			switch (absence.absenceType.justifiedTimeAtWork) {
			case AllDay:
				compensatoryRestInMinutes += absence.personDay.getWorkingTimeTypeDay().workingTime;
				break;
			case HalfDay:
				compensatoryRestInMinutes += absence.personDay.getWorkingTimeTypeDay().workingTime / 2;
				break;
			case ReduceWorkingTimeOfTwoHours:
				throw new IllegalStateException("Il tipo di assenza ReduceWorkingTimeOfTwoHours e' stato impostato come compensatoryRest (Riposo compensativo) ma questo non e' corretto.");
			case TimeToComplete:
				if (absence.personDay.timeAtWork < absence.personDay.getWorkingTimeTypeDay().workingTime) {
					compensatoryRestInMinutes += (absence.personDay.getWorkingTimeTypeDay().workingTime - absence.personDay.timeAtWork);
				}
				break;
			case Nothing:
				//Non c'è riposo compensativo da aggiungere al calcolo
				break;
			default:
				compensatoryRestInMinutes += absence.absenceType.justifiedTimeAtWork.minutesJustified; 
			}
		}

		Logger.trace("%s compensatoryRestInMinutes = %s", toString(), compensatoryRestInMinutes);

		//TODO: aggiungere eventuali riposi compensativi derivanti da inizializzazioni 
		//InitializationAbsence initAbsence = new InitializationAbsence();
		//int recoveryDays = initAbsence.recoveryDays;


		PersonMonth previousPersonMonth = PersonMonth.find("byPersonAndYearAndMonth", person, startOfMonth.minusMonths(1).getYear(), startOfMonth.minusMonths(1).getMonthOfYear()).first();
		Logger.trace("%s, previousPersonMonth = %s", toString(), previousPersonMonth);

		int totalRemainingMinutesPreviousMonth = previousPersonMonth == null ? 0 : previousPersonMonth.totalRemainingMinutes;

		if (person.qualification.qualification <= 3) {
			/**
			 * si vanno a guardare i residui recuperati dall'anno precedente e si controlla che esista per quella persona sia l'initTime che
			 * il campo dei minuti residui valorizzato. In tal caso vengono aggiunti al totalRemainingMinutes
			 */
			InitializationTime initTime = InitializationTime.find("Select initTime from InitializationTime initTime where initTime.person = ? " +
					"and initTime.date = ?", person, new LocalDate(year-1,12,31)).first();
			if(initTime != null && initTime.residualMinutes > 0)
				totalRemainingMinutes = initTime.residualMinutes + progressiveAtEndOfMonthInMinutes + totalRemainingMinutesPreviousMonth - compensatoryRestInMinutes;
			else
				totalRemainingMinutes = progressiveAtEndOfMonthInMinutes + totalRemainingMinutesPreviousMonth - compensatoryRestInMinutes;
		}
		else{
			PersonYear py = PersonYear.find("byPersonAndYear", person, year-1).first();
			//Se personYyear anno precedente non devo fare niente e totalRemainingMinutePastYearTaken = 0

			totalRemainingMinutes = progressiveAtEndOfMonthInMinutes + totalRemainingMinutesPreviousMonth - compensatoryRestInMinutes;

			/**
			 * TODO: c'è il caso di persone che hanno terminato il rapporto di lavoro in una certa data e che ritornano a lavoro a causa del 
			 * badge ancora attivo in date successive alla fine del rapporto di lavoro (vedi Fabrizio Leonardi che va in pensione il 31/12/2010
			 * e torna a lavoro timbrando col badge 3 volte nel 2011. Secondo la nostra idea di personMonth e personYear questi sono casi 
			 * spinosi poichè non esistono i personMonth pregressi per fare i calcoli sui residui
			 */
			if (py != null) {
				if(month < config.monthExpireRecoveryDaysFourNine){
					int totalRemainingMinutePastYearTaken = 0;

					for(int i = 1; i < month; i++){
						PersonMonth pm = PersonMonth.find("byPersonAndYearAndMonth", person, year, i).first();
						totalRemainingMinutePastYearTaken += pm.remainingMinutesPastYearTaken;						
					}
					int remainingMinutesResidualLastYear = 0;
					if(py.remainingMinutes != null && totalRemainingMinutePastYearTaken != 0){
						remainingMinutesResidualLastYear =  py.remainingMinutes - totalRemainingMinutePastYearTaken;
					}
					else
						remainingMinutesResidualLastYear = 0;

					if (remainingMinutesResidualLastYear < 0) {
						throw new IllegalStateException(
								String.format("Il valore dei minuti residui dell'anno precedente per %s nel mese %s %s e' %s. " +
										"Non ci dovrebbero essere valori negativi per le ore residue dell'anno precedente", person, year, month, remainingMinutesResidualLastYear));
					}

					if (compensatoryRestInMinutes > 0 && remainingMinutesResidualLastYear > 0) {
						remainingMinutesPastYearTaken = Math.min(compensatoryRestInMinutes, remainingMinutesResidualLastYear);
					}

					//Se non sono nell'ultimo mese in cui sono valide le ore residue dell'anno passato allora mi porto dietro
					// le ore residue che non ho ancora preso
					if (month < (config.monthExpireRecoveryDaysFourNine - 1)) {
						totalRemainingMinutes += remainingMinutesResidualLastYear - remainingMinutesPastYearTaken;
					}

				}
			}
			this.save();
		}

		this.save();
	}

	/**
	 * 
	 * @return il numero di buoni pasto usabili per quel mese
	 */
	public int numberOfMealTicketToUse(){
		int tickets=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			if(pd.mealTicket()==true)
				tickets++;
		}

		return tickets;
	}

	/**
	 * 
	 * @return il numero di buoni pasto da restituire per quel mese
	 */
	public int numberOfMealTicketToRender(){
		int ticketsToRender=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			if(pd.mealTicket()==false && (pd.isHoliday()==false))
				ticketsToRender++;
		}

		return ticketsToRender;
	}

	/**
	 * 
	 * @return il numero di giorni lavorati in sede. Per stabilirlo si controlla che per ogni giorno lavorativo, esista almeno una 
	 * timbratura.
	 */
	public int basedWorkingDays(){
		int basedDays=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			List<Stamping> stamp = pd.stampings;
			if(stamp.size()>0 && pd.isHoliday()==false)
				basedDays++;
		}
		return basedDays;
	}

	/**
	 * 
	 * @param days lista di PersonDay
	 * @return la lista contenente le assenze fatte nell'arco di tempo dalla persona
	 */

	public Map<AbsenceType,Integer> getAbsenceCode(){

		if(days == null){
			days = getDays();
		}
		absenceCodeMap = new HashMap<AbsenceType, Integer>();
		if(absenceCodeMap.isEmpty()){
			int i = 0;
			for(PersonDay pd : days){
				for (Absence absence : pd.absences) {
					AbsenceType absenceType = absence.absenceType;
					if(absenceType != null){
						boolean stato = absenceCodeMap.containsKey(absenceType);
						if(stato==false){
							i=1;
							absenceCodeMap.put(absenceType,i);            	 
						} else{
							i = absenceCodeMap.get(absenceType);
							absenceCodeMap.remove(absenceType);
							absenceCodeMap.put(absenceType, i+1);
						}
					}            
				}	 
			}       
		}

		return absenceCodeMap;	

	}


	public Map<AbsenceType, Integer> getAbsenceCodeMap() {
		return absenceCodeMap;
	}

	/**
	 * 
	 * @param days
	 * @return lista dei codici delle timbrature nel caso in cui ci siano particolarità sulle timbrature dovute a mancate timbrature
	 * per pausa mensa ecc ecc...
	 */
	public List<StampModificationType> getStampingCode(){
		if(days==null){
			days= getDays();
		}
		List<StampModificationType> stampCodeList = new ArrayList<StampModificationType>();
		for(PersonDay pd : days){

			StampModificationType smt = pd.checkTimeForLunch();
			Logger.debug("Lo stamp modification type è: %s", smt);

			if(smt != null && !stampCodeList.contains(smt)){
				Logger.debug("Aggiunto %s alla lista", smt.description);
				stampCodeList.add(smt);
			}
			StampModificationType smtMarked = pd.checkMarkedByAdmin();
			if(smtMarked != null && !stampCodeList.contains(smtMarked)){
				stampCodeList.add(smtMarked);
				Logger.debug("Aggiunto %s alla lista", smtMarked.description);
			}
			StampModificationType smtMidnight = pd.checkMissingExitStampBeforeMidnight();
			if(smtMidnight != null && !stampCodeList.contains(smtMidnight)){
				stampCodeList.add(smtMidnight);
				Logger.debug("Aggiunto %s alla lista", smtMidnight.description);
			}

		}
		Logger.debug("La lista degli stamping code per questo mese contiene: %s", stampingCodeList);
		return stampCodeList;
	}

	/**
	 * 
	 * @return il numero di riposi compensativi fatti dall'inizio dell'anno a quel momento
	 */
	public int getCompensatoryRestInYear(){
		LocalDate beginYear = new LocalDate(year, 1, 1);
		LocalDate now = new LocalDate();
		int numberOfCompensatoryRest = 0;
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, beginYear, now).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() > 0){
				for(Absence abs : pd.absences){
					if(abs.absenceType.code.equals("91"))
						numberOfCompensatoryRest = numberOfCompensatoryRest + 1;
				}
			}
		}
		return numberOfCompensatoryRest;

	}

	/**
	 * 
	 * @return il numero di ore di straordinario fatte dall'inizio dell'anno
	 */
	public int getOvertimeHourInYear(){
		Logger.debug("Chiamata funzione di controllo straordinari...");
		int overtimeHour = 0;
		List<Competence> compList = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.person = ? and comp.year = ? and " +
				"comp.competenceCode = code and code.code = ?", person, year, "S1").fetch();
		Logger.debug("La lista degli straordinari da inizio anno : %s", compList);
		if(compList != null){
			for(Competence comp : compList){
				overtimeHour = overtimeHour + comp.value;
			}
		}
		Logger.debug("Il numero di ore di straordinari è: ", overtimeHour);
		return overtimeHour;
	}

	@Override
	public String toString() {
		//FIXME: da sistemare
		return String.format("PersonMonth[%d] - person.id = %d, year = %s, month = %d, totalRemainingMinutes = %d, " +
				"progressiveAtEndOfMonthInMinutes = %d, compensatoryRestInMinutes = %d, remainingMinutesPastYearTakes = %d",
				id, person.id, year, month, totalRemainingMinutes, progressiveAtEndOfMonthInMinutes, compensatoryRestInMinutes, remainingMinutesPastYearTaken);
	}

	public static PersonMonth build(Person person, int year, int month){

		PersonMonth pm = new PersonMonth(person, year, month);
		pm.create();
		pm.aggiornaRiepiloghi();
		return pm;
		
	}

	public static PersonMonth getInstance(Person person, int year, int month) {
		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, year, month).first();
		if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
		return personMonth;
	}


	/**
	 * @return la somma dei minuti dei giorni (entro una certa data) che hanno una differenza negativa rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInNegativoAllaData(LocalDate date) {
		LocalDate startOfMonth = new LocalDate(year, month, 1);
		Long residuo = JPA.em().createQuery("SELECT sum(pd.difference) FROM PersonDay pd WHERE pd.date BETWEEN :startOfMonth AND :endOfMonth and pd.person = :person " +
				"AND pd.difference < 0 and pd.date <= :date", Long.class)
				.setParameter("startOfMonth", startOfMonth)
				.setParameter("endOfMonth", startOfMonth.dayOfMonth().withMaximumValue())
				.setParameter("person", person)
				.setParameter("date", date)
				.getSingleResult();
		int res = 0;
		if(residuo != null)
			res = residuo.intValue();
		
		return res;
	}
	
	/**
	 * @return la somma dei minuti dei giorni (alla fine del mese) che hanno una differenza negativa rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInNegativo() {
		LocalDate startOfMonth = new LocalDate(year, month, 1);
		return residuoDelMeseInNegativoAllaData(startOfMonth.dayOfMonth().withMaximumValue());
	}

	/**
	 * @return la somma dei minuti dei giorni (entro una certa data) che hanno una differenza positiva rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInPositivoAllaData(LocalDate date) {
		Long residuo = JPA.em().createQuery("SELECT sum(pd.difference) FROM PersonDay pd WHERE pd.date BETWEEN :startOfMonth AND :endOfMonth and pd.person = :person " +
				"AND pd.difference > 0 and pd.date <= :date", Long.class)
				.setParameter("startOfMonth", new LocalDate(year, month, 1))
				.setParameter("endOfMonth", (new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()))
				.setParameter("person", person)
				.setParameter("date", date)
				.getSingleResult();
		int res = 0;
		if(residuo != null)
			res = residuo.intValue();
		return res;
	}
	
	/**
	 * @return la somma dei minuti dei giorni (alla fine del mese) che hanno una differenza positiva rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInPositivo() {
		return residuoDelMeseInPositivoAllaData(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
	}
	
	/**
	 * @return i minuti di lavoro residui dell'anno precedente (il fatto di poterli utilizzare o meno può dipendere dal tipo di contratto e dal mese corrente
	 */
	public int residuoAnnoPrecedente() {
		Contract contractLastYear = person.getContract(new LocalDate(year - 1, 12, 31));
		
		//Se il contratto della persona era attivo anche l'anno scorso si prende il personYear dell'anno precedente altrimenti il residuo dell'anno precedente
		if (contractLastYear != null && contractLastYear.equals(person.getCurrentContract())) {
			PersonYear personYear = PersonYear.find("SELECT py FROM PersonYear py WHERE py.year = ? AND py.person = ?", year - 1, person).first();
			return personYear != null ? personYear.getRemainingMinutes() : 0;
		} else {
			return 0;
		}
	}
	
	/**
	 * @return il PersonMonth del mese precedente
	 */
	public PersonMonth mesePrecedente() {
		Contract currentContract = person.getContract(new LocalDate(year, month, 1));
		LocalDate fineMesePrecedente = (new LocalDate(year, month, 1)).minusMonths(1).dayOfMonth().withMaximumValue();
		
		if(currentContract == null)
			return null;
		if (currentContract.beginContract != null && fineMesePrecedente.isBefore(currentContract.beginContract)) {
			return null;
		}
		
		if (mesePrecedenteCache != null) {
			return mesePrecedenteCache;
		}
		LocalDate date = new LocalDate(year, month, 1);
		date = date.minusMonths(1);
		return PersonMonth.find("SELECT pm FROM PersonMonth pm WHERE pm.year = ? and pm.month = ? AND pm.person = ?", date.getYear(), date.getMonthOfYear(), person).first();
	}
	
	public boolean possibileUtilizzareResiduoAnnoPrecedente() {
		//Dipende dal livello.... e da
		Qualification qualification = Qualification.find("SELECT p.qualification FROM Person p WHERE p = ?", person).first(); 
		if(qualification == null)
			return false;
			
		return month <= 3 || qualification.qualification <= 3;
	}
	
	/**
	 * @return la somma dei residui positivi e di quelli negativi
	 */
	public int residuoDelMese() {
		return residuoDelMeseInPositivo() + residuoDelMeseInNegativo();
	}

	/**
	 * @return la somma dei residui positivi e di quelli negativi alla data specificata
	 */
	public int residuoDelMeseAllaData(LocalDate date) {
		return residuoDelMeseInPositivoAllaData(date) + residuoDelMeseInNegativoAllaData(date);
	}
	
	/**
	 * @return il tempo di lavoro dai mese precedenti eventualmente comprensivo di quello derivante
	 * 	dall'anno precedente
	 */
	public int totaleResiduoAnnoCorrenteAlMesePrecedente() {
		//Deve esistere un mese precedente ed essere dello stesso anno (quindi a gennaio il mese precedente di questo anno non esiste)
		if (mesePrecedente() != null && month != 1) {
			return mesePrecedente().totaleResiduoAnnoCorrenteAFineMese();
		}
		return 0;

	}
	
	public int residuoAnnoPrecedenteDisponibileAllInizioDelMese() {
		if (possibileUtilizzareResiduoAnnoPrecedente()) {
			
			if (month.equals(1)) {
				return residuoAnnoPrecedente();
			} 
			if (mesePrecedente() == null) {
				return 0;
			}
			return mesePrecedente().residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		} 
		
		return 0;
		
	}
	
	public int residuoAnnoPrecedenteDisponibileAllaFineDelMese() {
		int residuoAnnoPrecedenteDisponibileAllInizioDelMese = residuoAnnoPrecedenteDisponibileAllInizioDelMese();
		//System.out.println("mese: " + month + ". residuoAnnoPrecedenteDisponibileAllInizioDelMese = " + residuoAnnoPrecedenteDisponibileAllInizioDelMese);
		
		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllInizioDelMese + recuperiOreDaAnnoPrecedente + riposiCompensativiDaAnnoPrecedente;
		
		//System.out.println("mese: " + month + ". residuoAnnoPrecedenteDisponibileAllaFineDelMese() = " + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
		return residuoAnnoPrecedenteDisponibileAllaFineDelMese;
	}
	
	public int totaleResiduoAnnoCorrenteAFineMese() {
		return residuoDelMese() + totaleResiduoAnnoCorrenteAlMesePrecedente() + riposiCompensativiDaAnnoCorrente - straordinari - recuperiOreDaAnnoPrecedente;  
	}
	
	public int totaleResiduoAnnoCorrenteAllaData(LocalDate date) {
		return residuoDelMeseAllaData(date) + totaleResiduoAnnoCorrenteAlMesePrecedente() + riposiCompensativiDaAnnoCorrente - straordinari - recuperiOreDaAnnoPrecedente;  
	}
	
	public int totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese() {
		return totaleResiduoAnnoCorrenteAFineMese() + residuoAnnoPrecedenteDisponibileAllaFineDelMese();
	}
	
	public void aggiornaRiepiloghi() {
		Logger.debug("Aggiornamento dei riepiloghi del mese %s per %s", month, person);
		
		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		
		int residuoDelMeseInNegativo = residuoDelMeseInNegativo();
		int residuoDelMeseInPositivo = residuoDelMeseInPositivo();
		
		if (residuoDelMeseInNegativo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese > 0) {
			
			 Logger.debug("mese = %s. Residuo del mese in negativo (%s) != 0 e residuoAnnoPrecedenteDisponibileAllaFineDelMese (%s) > 0, recupero dall'anno scorso il recuperabile",
					month , residuoDelMeseInNegativo, residuoAnnoPrecedenteDisponibileAllaFineDelMese);
			 
			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese > -residuoDelMeseInNegativo) {
				Logger.debug("mese = %s. residuoAnnoPrecedenteDisponibileAllaFineDelMese > del residuo del mese in negativo, aumento i recuperiOreDaAnnoPrecedente (adesso %s) di %s minuti",
					month, recuperiOreDaAnnoPrecedente, residuoDelMeseInNegativo);
				
				recuperiOreDaAnnoPrecedente += residuoDelMeseInNegativo;
				
				Logger.debug("mese = %s. recuperiOreDaAnnoPrecedente = %s minuti", month, recuperiOreDaAnnoPrecedente);
			} else {
				recuperiOreDaAnnoPrecedente -= residuoAnnoPrecedenteDisponibileAllaFineDelMese;
			}
			this.save();
		}
		
		if (residuoDelMeseInPositivo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
			if (residuoDelMeseInPositivo > -residuoAnnoPrecedenteDisponibileAllaFineDelMese) {
				recuperiOreDaAnnoPrecedente -= residuoAnnoPrecedenteDisponibileAllaFineDelMese;
			} else {
				recuperiOreDaAnnoPrecedente += residuoDelMeseInPositivo;
			}
			this.save();
		}
		this.save();
	}
	
	public int tempoDisponibilePerRecuperi(LocalDate date) {
		int totaleResiduoAnnoCorrenteAllaData = totaleResiduoAnnoCorrenteAllaData(date);
		
		//System.out.println("totaleResiduoAnnoCorrenteAllaData = " + totaleResiduoAnnoCorrenteAllaData);
		
		int tempoDisponibile = totaleResiduoAnnoCorrenteAllaData + residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		
		if (tempoDisponibile <= 0) {
			tempoDisponibile = 0;
		}
		//System.out.println("Data = " + date + ". Tempo disponibile per recuperi = " + tempoDisponibile);
		return tempoDisponibile;
		
	}	
	
	public int tempoDisponibilePerStraordinari() {
		
		int residuoDelMeseInPositivo = residuoDelMeseInPositivo();
		
		if (residuoDelMeseInPositivo <= 0) {
			return 0;
		}
		
		int residuoAllaDataRichiesta = residuoDelMese();
		
		int tempoDisponibile = residuoAnnoPrecedenteDisponibileAllaFineDelMese() + mesePrecedente().totaleResiduoAnnoCorrenteAFineMese() + residuoAllaDataRichiesta;
		
		if (tempoDisponibile <= 0) {
			return 0;
		}
		
		return Math.min(residuoDelMeseInPositivo, tempoDisponibile);
					
	}
	
	public boolean assegnaStraordinari(int ore) {
		if (tempoDisponibilePerStraordinari() > ore * 60) {
			straordinari = ore * 60;
			this.save();
			return true;
		}
		return false;
	}
	
	public boolean prendiRiposoCompensativo(LocalDate date) {
		int minutiRiposoCompensativo = minutiRiposoCompensativo(date);
		
		if (-minutiRiposoCompensativo > tempoDisponibilePerRecuperi(date)) {
			return false;
		}
		
		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		
		if (residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
			throw new IllegalStateException(
				String.format("Richiesto riposo compensativo per l'utente %s nella data %s: ci sono ore disponibili " +
					"ma il residuo dell'anno scorso è negativo, questo non dovrebbe essere possibile, contattare Dario <dario.tagliaferri@iit.cnr.it>",
					person, date));
		}
		
		//System.out.println("residuoAnnoPrecedenteDisponibileAllaFineDelMese = " + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
		if (residuoAnnoPrecedenteDisponibileAllaFineDelMese == 0) {
			//Per esempio per i tecnici/amministrativi da aprile in poi
			riposiCompensativiDaAnnoCorrente += minutiRiposoCompensativo;
		} else {
			if (minutiRiposoCompensativo < residuoAnnoPrecedenteDisponibileAllaFineDelMese) {
				riposiCompensativiDaAnnoPrecedente += minutiRiposoCompensativo;
			} else {
				riposiCompensativiDaAnnoPrecedente += residuoAnnoPrecedenteDisponibileAllaFineDelMese;
				riposiCompensativiDaAnnoCorrente += (minutiRiposoCompensativo + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
			}				
		}
		save();
		//Creare l'assenza etc....
		aggiornaRiepiloghi();
		return true;
	}
	
	/**
	 * @return il valore (negativo) dei minuti a cui corrisponde un riposo compensativo
	 */
	public int minutiRiposoCompensativo(LocalDate date) {
		//Cambia in funzione del tipo di orario di lavoro
		return - person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;

	}
}
