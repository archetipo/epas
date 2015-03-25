/**
 * 
 */
package controllers;

import static play.modules.pdf.PDF.renderPDF;
import play.modules.pdf.PDF.Options;
import helpers.BadRequest;
import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.JsonReperibilityChangePeriodsBinder;
import it.cnr.iit.epas.JsonReperibilityPeriodsBinder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import manager.AbsenceManager;
import manager.PersonManager;
import manager.ReperibilityManager;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.ShiftType;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.postgresql.translation.messages_it;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.mysql.jdbc.Messages;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.ShiftDao;


/**
 * @author cristian
 *
 */
public class Reperibility extends Controller {

	public static String codFr = "207";
	public static String codFs = "208";
	
	
	/*
	 * @author arianna
	 * Restituisce la lista dei reperibili attivi al momento di un determinato tipo
	 */
	public static void personList() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.devel.iit.cnr.it");
		
		Long type = Long.parseLong(params.get("type"));
		Logger.debug("Esegue la personList con type=%s", type);
		
	//	List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE r.personReperibilityType.id = ? AND (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())", type).fetch();
		List<Person> personList = PersonDao.getPersonForReperibility(type);
		Logger.debug("Reperibility personList called, found %s reperible person", personList.size());
		render(personList);
	}

	/**
	 * @author cristian, arianna
	 * Fornisce i periodi di reperibilità del personale reperibile di tipo 'type'
	 * nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
	 * 
	 * per provarlo: curl -H "Accept: application/json" http://localhost:9001/reperibility/1/find/2012/11/26/2013/01/06
	 */
	public static void find() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		// reperibility type validation
		Long type = Long.parseLong(params.get("type"));
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		PersonReperibilityType prt = PersonReperibilityDayDao.getPersonReperibilityTypeById(type);
		
		// date interval construction
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = PersonReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(from, to, reperibilityType, Optional.<PersonReperibility>absent());
		//		PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND prd.reperibilityType = ? ORDER BY prd.date", from, to, reperibilityType).fetch();

		Logger.debug("Reperibility find called from %s to %s, found %s reperibility days", from, to, reperibilityDays.size());
		// Manager ReperibilityManager called to find out the reperibilityPeriods
		List<ReperibilityPeriod> reperibilityPeriods = ReperibilityManager.getPersonReperibilityPeriods(reperibilityDays, prt);
		Logger.debug("Find %s reperibilityPeriods. ReperibilityPeriods = %s", reperibilityPeriods.size(), reperibilityPeriods);
		
		render(reperibilityPeriods);
	}
	
	
	/**
	 * @author arianna
	 * Fornisce la lista del personale reperibile di tipo 'type' 
	 * nell'intervallo di tempo da 'yearFrom/monthFrom/dayFrom'  a 'yearTo/monthTo/dayTo'
	 * 
	 */
	public static void who() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		List<Person> personList = new ArrayList<Person>();
		
		// reperibility type validation
		Long type = Long.parseLong(params.get("type"));
		PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		// date interval construction
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		List<PersonReperibilityDay> reperibilityDays = PersonReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(from, to, reperibilityType, Optional.<PersonReperibility>absent());
		//		PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND prd.reperibilityType = ? ORDER BY prd.date", from, to, reperibilityType).fetch();

		Logger.debug("Reperibility who called from %s to %s, found %s reperibility days", from, to, reperibilityDays.size());

		personList = ReperibilityManager.getPersonsFromReperibilityDays(reperibilityDays);
		Logger.debug("trovati %s reperibili: %s", personList.size(), personList);
		
		render(personList);
	}
	
	
	/**
	 * @author arianna
	 * Legge le assenze dei reperibili di una determinata tipologia in un dato intervallo di tempo
	 * (portale sistorg)
	 */
	public static void absence() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");

		Logger.debug("Sono nella absebce");
		
		Long type = Long.parseLong(params.get("type"));
		
		PersonReperibilityType repType = PersonReperibilityType.findById(type);
		if (repType == null) {
			notFound(String.format("PersonReperibilityType type = %s doesn't exist", type));			
		}
	
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		// read the reperibility person list 
		//List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE r.personReperibilityType.id = ? AND (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())", type).fetch();
		List<Person> personList = PersonDao.getPersonForReperibility(type);
		Logger.debug("Reperibility personList called, found %s reperible person of type %s", personList.size(), type);
		
		// Lists of absence for a single reperibility person and for all persons
		List<Absence> absencePersonReperibilityDays = new ArrayList<Absence>();
		
		// List of absence periods
		List<AbsenceReperibilityPeriod> absenceReperibilityPeriods = new ArrayList<AbsenceReperibilityPeriod>();

		if (personList.size() == 0) {
			render(absenceReperibilityPeriods);
			return;
		}
				
		
//		absencePersonReperibilityDays = JPA.em().createQuery("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date BETWEEN :from AND :to AND pd.person IN (:personList) ORDER BY pd.person.id, pd.date")
//			.setParameter("from", from)
//			.setParameter("to", to)
//			.setParameter("personList", personList)
//			.getResultList();
		absencePersonReperibilityDays = AbsenceDao.getAbsenceForPersonListInPeriod(personList, from, to);
		
		Logger.debug("Trovati %s giorni di assenza", absencePersonReperibilityDays.size());
		
		// get the absent reperibility periods from the absent days
		absenceReperibilityPeriods = ReperibilityManager.getAbsentReperibilityPeriodsFromAbsentReperibilityDays(absencePersonReperibilityDays, repType);
		
		Logger.debug("Find %s absenceReperibilityPeriod. AbsenceReperibilityPeriod = %s", absenceReperibilityPeriods.size(), absenceReperibilityPeriods.toString());
		render(absenceReperibilityPeriods);
	}
	
	
	/**
	 * @author arianna
	 * Restituisce la lista delle persone reperibili assenti di una determinata tipologia in un dato intervallo di tempo
	 * (portale sistorg)
	 */
	public static void whoIsAbsent() {
		response.accessControl("*");
		//response.setHeader("Access-Control-Allow-Origin", "http://sistorg.iit.cnr.it");
		
		List<Person> absentPersonsList = new ArrayList<Person>();

		Long type = Long.parseLong(params.get("type"));
		
		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		// read the reperibility person list 
		//List<Person> personList = Person.find("SELECT p FROM Person p JOIN p.reperibility r WHERE r.personReperibilityType.id = ? AND (r.startDate IS NULL OR r.startDate <= now()) and (r.endDate IS NULL OR r.endDate >= now())", type).fetch();
		List<Person> personList = PersonDao.getPersonForReperibility(type);
		Logger.debug("Reperibility personList called, found %s reperible person of type %s", personList.size(), type);
		
		// Lists of absence for a single reperibility person and for all persons
		List<Absence> absencePersonReperibilityDays = new ArrayList<Absence>();
		
		if (personList.size() == 0) {
			render(personList);
			return;
		}
		
//		absencePersonReperibilityDays = JPA.em().createQuery("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date BETWEEN :from AND :to AND pd.person IN (:personList) ORDER BY pd.person.id, pd.date")
//			.setParameter("from", from)
//			.setParameter("to", to)
//			.setParameter("personList", personList)
//			.getResultList();

		absencePersonReperibilityDays = AbsenceDao.getAbsenceForPersonListInPeriod(personList, from, to);		
		Logger.debug("Trovati %s giorni di assenza", absencePersonReperibilityDays.size());
		
		absentPersonsList = AbsenceManager.getPersonsFromAbsentDays(absencePersonReperibilityDays);
		
		Logger.debug("Find %s person. absentPersonsList = %s", absentPersonsList.size(), absentPersonsList.toString());
		render(absentPersonsList);
	}
	
	
	/**
	 * @author cristian, arianna
	 * Aggiorna le informazioni relative alla Reperibilità del personale
	 * 
	 * Per provarlo è possibile effettuare una chiamata JSON come questa:
	 * 	$  curl -H "Content-Type: application/json" -X PUT \
	 * 			-d '[ {"id" : "49","start" : 2012-12-05,"end" : "2012-12-10", "reperibility_type_id" : "1"}, { "id" : "139","start" : "2012-12-12" , "end" : "2012-12-14", "reperibility_type_id" : "1" } , { "id" : "139","start" : "2012-12-17","end" : "2012-12-18", "reperibility_type_id" : "1" } ]' \ 
	 * 			http://localhost:9000/reperibility/1/update/2012/12
	 * 
	 * @param body
	 */
	public static void update(Long type, Integer year, Integer month, @As(binder=JsonReperibilityPeriodsBinder.class) ReperibilityPeriods body) {

		Logger.debug("update: Received reperebilityPeriods %s", body);	
		if (body == null) {
			badRequest();	
		}
		
		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		PersonReperibilityType reperibilityType = PersonReperibilityDayDao.getPersonReperibilityTypeById(type);
		if (reperibilityType == null) {
			throw new IllegalArgumentException(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		//Conterrà i giorni del mese che devono essere attribuiti a qualche reperibile 
		Set<Integer> repDaysOfMonthToRemove = new HashSet<Integer>();	
		
		
		repDaysOfMonthToRemove = ReperibilityManager.savePersonReperibilityDaysFromReperibilityPeriods(reperibilityType, year, month, body.periods);
		
		Logger.debug("Giorni di reperibilità da rimuovere = %s", repDaysOfMonthToRemove);
		
		int deletedRep = ReperibilityManager.deleteReperibilityDaysFromMonth(reperibilityType, year, month, repDaysOfMonthToRemove);
		
	}
	
	
	/**
	 * @author arianna
	 * Scambia due periodi di reperibilità di due persone reperibili diverse
	 * 
	 * Per provarlo è possibile effettuare una chiamata JSON come questa:
	 * 	$  curl -H "Content-Type: application/json" -X PUT \
	 * 			-d '[ {"mail_req" : "ruberti@iit.cnr.it", "mail_sub" : "lorenzo.rossi@iit.cnr.it", "req_start_date" : "2012-12-10", "req_end_date" : "2012-12-10", "sub_start_date" : "2012-12-10", "sub_end_date" : "2012-12-10"} ]' \ 
	 * 			http://scorpio.nic.it:9001/reperibility/1/changePeriods
	 * 
	 * @param body
	 */
	public static void changePeriods(Long type, @As(binder=JsonReperibilityChangePeriodsBinder.class) ReperibilityPeriods body) {

		Logger.debug("update: Received reperebilityPeriods %s", body);	
		if (body == null) {
			badRequest();	
		}
		
		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		PersonReperibilityType reperibilityType = PersonReperibilityDayDao.getPersonReperibilityTypeById(type);
		if (reperibilityType == null) {
			throw new IllegalArgumentException(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		
		Boolean changed = ReperibilityManager.changeTwoReperibilityPeriods(reperibilityType, body.periods);
		
		if (changed) {
			Logger.info("Periodo di reperibilità cambiato con successo!");
		} else {
			Logger.info("Il cambio di reperibilità non è stato effettuato");
		}
		
	}
	
	
	/**
	 * @author arianna, cristian
	 * crea il file PDF con il calendario annuale delle reperibilità di tipi 'type' per l'anno 'year'
	 * (portale sistorg)
	 */
	public static void exportYearAsPDF() {
		int year = params.get("year", Integer.class);
		Long reperibilityId = params.get("type", Long.class);
		
		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		PersonReperibilityType reperibilityType = PersonReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}
		
		// build the reperibility calendar 
		List<Table<Person, Integer, String>> reperibilityMonths = new ArrayList<Table<Person, Integer, String>>();
		reperibilityMonths = ReperibilityManager.buildYearlyReperibilityCalendar(year, reperibilityType);
		
		// build the reperibility summary report
		Table<Person, String, Integer> reperibilitySumDays = HashBasedTable.<Person, String, Integer>create();
		reperibilitySumDays = ReperibilityManager.buildYearlyReperibilityReport(reperibilityMonths);
		Logger.info("Creazione del documento PDF con il calendario annuale delle reperibilità per l'anno %s", year);

		
		LocalDate firstOfYear = new LocalDate(year, 1, 1);
		Options options = new Options();
		options.pageSize = IHtmlToPdfTransformer.A4L;
		renderPDF(options, year, firstOfYear, reperibilityMonths, reperibilitySumDays);
	}

    
	/**
	 * @author arianna
	 * restituisce una tabella con le eventuali inconsistenze tra le timbrature dei reperibili di un certo tipo e i
	 * turni di reperibilità svolti in un determinato periodo di tempo
	 * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence], List<'gg MMM'>)
	 */
	public static Table<Person, String, List<String>> getInconsistencyTimestamps2Reperibilities (Long reperibilityId, LocalDate startDate, LocalDate endDate) {
		// for each person contains days with absences and no-stamping  matching the reperibility days 
		Table<Person, String, List<String>> inconsistentAbsence = TreeBasedTable.<Person, String, List<String>>create();				
		
		
		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		PersonReperibilityType reperibilityType = PersonReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}
		
		
		List<PersonReperibilityDay> personReperibilityDays = 
				PersonReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(startDate, endDate, reperibilityType, Optional.<PersonReperibility>absent());
//				JPA.em().createQuery("SELECT prd FROM PersonReperibilityDay prd WHERE date BETWEEN :startDate AND :endDate AND reperibilityType = :reperibilityType ORDER by date")
//				.setParameter("firstOfMonth", startDate)
//				.setParameter("endOfMonth", endDate)
//				.setParameter("reperibilityType", reperibilityType)
//				.getResultList();
		
		inconsistentAbsence = CompetenceUtility.getReperibilityInconsistenceAbsenceTable(personReperibilityDays, startDate, endDate);
			
		return inconsistentAbsence;
	}
	
	
	/*
	 * @author arianna
	 * crea il file PDF con il resoconto mensile delle reperibilità di tipo 'type' per
	 * il mese 'month' dell'anno 'year'
	 * Segnala le eventuali inconsistenze con le assenze o le mancate timbrature
	 * (portale sistorg)
	 */
	public static void exportMonthAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
		Long reperibilityId = params.get("type", Long.class);
		
		LocalDate today = new LocalDate();
				
		// for each person contains the number of rep days fr o fs (feriali o festivi)
		Table<Person, String, Integer> reperibilitySumDays = TreeBasedTable.<Person, String, Integer>create();
		
		// for each person contains the list of the rep periods divided by fr o fs
		Table<Person, String, List<String>> reperibilityDateDays = TreeBasedTable.<Person, String, List<String>>create();
		
		// for each person contains days with absences and no-stamping  matching the reperibility days 
		Table<Person, String, List<String>> inconsistentAbsence = TreeBasedTable.<Person, String, List<String>>create();				
		
		// get the Competence code for the reperibility working or non-working days  
		//CompetenceCode competenceCodeFS = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFs).first();
		CompetenceCode competenceCodeFS = CompetenceCodeDao.getCompetenceCodeByCode(codFs); 
		
		//CompetenceCode competenceCodeFR = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFr).first();
		CompetenceCode competenceCodeFR = CompetenceCodeDao.getCompetenceCodeByCode(codFr);
		
		Logger.debug("Creazione dei  competenceCodeFS competenceCodeFR %s/%s", competenceCodeFS, competenceCodeFR);
			
		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(reperibilityId);
		PersonReperibilityType reperibilityType = PersonReperibilityDayDao.getPersonReperibilityTypeById(reperibilityId);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", reperibilityId));			
		}
			
		// get all the reperibility of a certain type in a certain month
		LocalDate firstOfMonth = new LocalDate(year, month, 1);
			
		List<PersonReperibilityDay> personReperibilityDays = 
				PersonReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), reperibilityType, Optional.<PersonReperibility>absent());
//				JPA.em().createQuery("SELECT prd FROM PersonReperibilityDay prd WHERE date BETWEEN :firstOfMonth AND :endOfMonth AND reperibilityType = :reperibilityType ORDER by date")
//				.setParameter("firstOfMonth", firstOfMonth)
//				.setParameter("endOfMonth", firstOfMonth.dayOfMonth().withMaximumValue())
//				.setParameter("reperibilityType", reperibilityType)
//				.getResultList();
		
		Logger.debug("dimensione personReperibilityDays = %s", personReperibilityDays.size());
		
		// update the reperibility days in the DB
		int updatedCompetences = ReperibilityManager.updateDBReperibilityCompetences(personReperibilityDays, year, month);
		Logger.debug("Salvate o aggiornate %d competences", updatedCompetences);

		// builds the table with the summary of days and reperibility periods description
		// reading data from the Competence table in the DB
		//List<Competence> frCompetences = Competence.find("SELECT com FROM Competence com JOIN com.person p WHERE p.reperibility.personReperibilityType = ? AND com.year = ? AND com.month = ? AND com.competenceCode = ? ORDER by p.surname", reperibilityType, year, month, competenceCodeFR).fetch();
		List<Competence> frCompetences = CompetenceDao.getCompetenceInReperibility(reperibilityType, year, month, competenceCodeFR);
		Logger.debug("Trovate %d competences di tipo %s nel mese %d/%d", frCompetences.size(), reperibilityType,  month, year);
		
		// update  reports for the approved days and reasons for the working days
		ReperibilityManager.updateReperibilityDaysReportFromCompetences(reperibilitySumDays, frCompetences);
		ReperibilityManager.updateReperibilityDatesReportFromCompetences(reperibilityDateDays, frCompetences);
		
		
		// builds the table with the summary of days and reperibility periods description
		// reading data from the Competence table in the DB
		//List<Competence> fsCompetences = Competence.find("SELECT com FROM Competence com JOIN com.person p WHERE p.reperibility.personReperibilityType = ? AND com.year = ? AND com.month = ? AND com.competenceCode = ? ORDER by p.surname", reperibilityType, year, month, competenceCodeFS).fetch();
		List<Competence> fsCompetences = CompetenceDao.getCompetenceInReperibility(reperibilityType, year, month, competenceCodeFS);
		Logger.debug("Trovate %d competences di tipo %s nel mese %d/%d", fsCompetences.size(), reperibilityType,  month, year);
		
		// update  reports for the approved days and reasons for the holidays 
		ReperibilityManager.updateReperibilityDaysReportFromCompetences(reperibilitySumDays, fsCompetences);
		ReperibilityManager.updateReperibilityDatesReportFromCompetences(reperibilityDateDays, fsCompetences);
					
		// get the table with the absence and no stampings inconsistency 
		inconsistentAbsence = CompetenceUtility.getReperibilityInconsistenceAbsenceTable(personReperibilityDays, firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue());
				
		Logger.info("Creazione del documento PDF con il resoconto delle reperibilità per il periodo %s/%s Fs=%s Fr=%s", firstOfMonth.plusMonths(0).monthOfYear().getAsText(), firstOfMonth.plusMonths(0).year().getAsText(), codFs, codFr);
		
		String cFr = codFr;
		String cFs = codFs;
		String thNoStamp = Messages.getString("PDFReport.thNoStampings");
		String thAbs = Messages.getString("PDFReport.thAbsences");
		
		renderPDF(today, firstOfMonth, reperibilitySumDays, reperibilityDateDays, inconsistentAbsence, cFs, cFr, thNoStamp, thAbs);
		
	}

	
	/*
	 * Export the reperibility calendar in iCal for the person with id = personId with reperibility 
	 * of type 'type' for the 'year' year
	 * If the personId=0, it exports the calendar for all  the reperibility persons of type 'type'
	 */
	private static Calendar createCalendar(Long type, Long personId, int year) {
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, reperibility type %s", year, personId, type);
		
		List<PersonReperibility> personsInTheCalList = new ArrayList<PersonReperibility>();
		String eventLabel;
		
		// check for the parameter
		//---------------------------
		//PersonReperibilityType reperibilityType = PersonReperibilityType.findById(type);
		PersonReperibilityType reperibilityType = PersonReperibilityDayDao.getPersonReperibilityTypeById(type);
		if (reperibilityType == null) {
			notFound(String.format("ReperibilityType id = %s doesn't exist", type));			
		}
		
		if (personId == 0) {
			// read the reperibility person 
			//List<PersonReperibility> personsReperibility = PersonReperibility.find("SELECT pr FROM PersonReperibility pr WHERE pr.personReperibilityType.id = ?", type).fetch();
			List<PersonReperibility> personsReperibility = PersonReperibilityDayDao.getPersonReperibilityByType(PersonReperibilityDayDao.getPersonReperibilityTypeById(type));
			if (personsReperibility.isEmpty()) {
				notFound(String.format("No person associated to a reperibility of type = %s", reperibilityType));
			}
			personsInTheCalList = personsReperibility;
		} else {
			// read the reperibility person 
			//PersonReperibility personReperibility = PersonReperibility.find("SELECT pr FROM PersonReperibility pr WHERE pr.personReperibilityType.id = ? AND pr.person.id = ?", type, personId).first();
			PersonReperibility personReperibility = PersonReperibilityDayDao.getPersonReperibilityByPersonAndType(PersonDao.getPersonById(personId), PersonReperibilityDayDao.getPersonReperibilityTypeById(type));
			if (personReperibility == null) {
				notFound(String.format("Person id = %d is not associated to a reperibility of type = %s", personId, reperibilityType));
			}
			personsInTheCalList.add(personReperibility);
		}

	
		Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar = ReperibilityManager.createicsReperibilityCalendar(Integer.parseInt(params.get("year")), personsInTheCalList);
		
		Logger.debug("Find %s periodi di reperibilità.", icsCalendar.getComponents().size());
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, reperibility type %s", year, personId, type);
		
        return icsCalendar;
	}
	
	
	public static void iCal() {
		Long type = params.get("type", Long.class);
		Long personId = params.get("personId", Long.class);
		int year = params.get("year", Integer.class);
		
		response.accessControl("*");
		
		//response.setHeader("Access-Control-Allow-Origin", "*");
		
		try {
			Calendar calendar = createCalendar(type, personId, year);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        CalendarOutputter outputter = new CalendarOutputter();
	        outputter.output(calendar, bos);
	        response.setHeader("Content-Type", "application/ics");
	        InputStream is = new ByteArrayInputStream(bos.toByteArray());
	        renderBinary(is,"reperibilitaRegistro.ics");
	        bos.close();
	        is.close();
		} catch (IOException e) {
			Logger.error("Io exception building ical", e);
		} catch (ValidationException e) {
			Logger.error("Validation exception generating ical", e);
		}
	}

}
