package controllers;

import static play.modules.pdf.PDF.renderPDF;
import it.cnr.iit.epas.JsonShiftPeriodsBinder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import manager.ShiftManager;
import models.Absence;
import models.Competence;
import models.Person;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.User;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.joda.time.LocalDate;
import org.slf4j.LoggerFactory;

import play.Logger;
import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.modules.pdf.PDF.Options;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import controllers.Resecure.BasicAuth;
import dao.AbsenceDao;
import dao.PersonDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;


/**
 * 
 * @author arianna, dario
 * Implements work shifts
 *
 */
@With(Resecure.class)
public class Shift extends Controller {
	
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(Shift.class);
	
	@Inject
	private static ShiftDao shiftDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static ShiftManager shiftManager;
	@Inject
	private static PersonShiftDayDao personShiftDayDao;

	@Inject
	private static AbsenceDao absenceDao;


	/*
	 * @author arianna
	 * Restituisce la lista delle persone in un determinato turno
	 * 
	 */
	@BasicAuth
	public static void personList(){
		response.accessControl("*");

		String type = params.get("type");		
		Logger.debug("Cerco persone del turno %s", type);

		//ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		ShiftType shiftType = shiftDao.getShiftTypeByType(type);
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco Turnisti di tipo %s", shiftType.type);

		List<Person> personList = new ArrayList<Person>();
		personList = personDao.getPersonForShift(type);

		Logger.debug("Shift personList called, found %s shift person", personList.size());

		for (Person p: personList) {
			Logger.debug("name=%s surname=%s id=%di jolly=%s", p.name, p.surname, p.id, p.personShift.jolly);
		}
		render(personList);
	}


	/*
	 * @author arianna
	 * Get shifts from the DB and render to the sistorg portal calendar
	 */
	@BasicAuth
	public static void timeTable(){
		response.accessControl("*");

		Logger.debug("Cercata la time table di un turno");

		// type validation
		String type = params.get("type");
		ShiftType shiftType = shiftDao.getShiftTypeByType(type);
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco la time table del turno di tipo %s", shiftType.type);

		ShiftTimeTable shiftTimeTable = shiftType.shiftTimeTable;

		render(shiftTimeTable);

	}

	/*
	 * @author arianna, dario
	 * Get shifts from the DB and render to the sistorg portal calendar
	 */
	@BasicAuth
	public static void find(){
		response.accessControl("*");

		// type validation
		String type = params.get("type");
		ShiftType shiftType = shiftDao.getShiftTypeByType(type);
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco turni di tipo %s", shiftType.type);

		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));


		// get the normal shift  ????????????????????????? 
		//		List<PersonShiftDay> personShiftDay = PersonShiftDay.find("" +
		//				"SELECT psd FROM PersonShiftDay psd WHERE psd.date BETWEEN ? AND ? " +
		//				"AND psd.shiftType = ? " +
		//				"ORDER BY psd.shiftSlot, psd.date",
		//			//	"ORDER BY psd.shiftTimeTable.startShift, psd.date", 
		//				from, to, shiftType).fetch();
		List<PersonShiftDay> personShiftDays = shiftDao.getShiftDaysByPeriodAndType(from, to, shiftType);	
		Logger.debug("Shift find called from %s to %s, type %s - found %s shift days", from, to, shiftType.type, personShiftDays.size());

		List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
		List<ShiftPeriod> deletedShiftPeriods = new ArrayList<ShiftPeriod>();

		// get the shift periods
		shiftPeriods = shiftManager.getPersonShiftPeriods(personShiftDays);

		// get the cancelled shifts of type shiftType
		List<ShiftCancelled> shiftCancelled = shiftDao.getShiftCancelledByPeriodAndType(from, to, shiftType);
		Logger.debug("ShiftCancelled find called from %s to %s, type %s - found %s shift days", from, to, shiftType.type, shiftCancelled.size());

		// get the period of cancelled shifts
		deletedShiftPeriods = shiftManager.getDeletedShiftPeriods(shiftCancelled);

		// add the deleted period to the worked one
		shiftPeriods.addAll(deletedShiftPeriods);

		Logger.debug("Find %s shiftPeriods.", shiftPeriods.size());
		render(shiftPeriods);

	}


	/*
	 * @author arianna
	 * Update working shifts in the DB that have been red from the sistorg portal calendar
	 */
	@BasicAuth
	public static void update(String type, Integer year, Integer month, @As(binder=JsonShiftPeriodsBinder.class) ShiftPeriods body) {
		Logger.debug("update: Received shiftPeriods %s", body);

		if (body == null) {
			badRequest();	
		}

		// type validation
		//ShiftType shiftType = ShiftType.find("SELECT st FROM ShiftType st WHERE st.type = ?", type).first();
		ShiftType shiftType = shiftDao.getShiftTypeByType(type);
		if (shiftType == null) {
			throw new IllegalArgumentException(String.format("ShiftType type = %s doesn't exist", type));			
		}

		Logger.debug("shiftType=%s", shiftType.description);

		// save the recived shift periods with type shiftType in the month "month" of the "year" year
		shiftManager.savePersonShiftDaysFromShiftPeriods(shiftType, year, month, body);

	}


	/**
	 * @author arianna
	 * crea una tabella con le eventuali inconsistenze tra le timbrature di un turnista e le fasce di orario
	 * da rispettare per un determinato turno, in un dato periodo di tempo
	 * (Person, [thNoStampings, thBadStampings, thAbsences], List<gg MMM>)
	 */
	@BasicAuth
	public static void getInconsistencyTimestamps2Timetable (ShiftType shiftType, LocalDate startDate, LocalDate endDate) {

		// crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
		Table<Person, String, List<String>> inconsistentAbsence = TreeBasedTable.<Person, String, List<String>>create();

		// seleziona le persone nel turno 'shiftType' da inizio a fine mese
		List<PersonShiftDay> personShiftDays = personShiftDayDao.getPersonShiftDayByTypeAndPeriod(startDate, endDate, shiftType);

		//inconsistentAbsence = CompetenceUtility.getShiftInconsistencyTimestampTable(personShiftDays);
		shiftManager.getShiftInconsistencyTimestampTable(personShiftDays, inconsistentAbsence);

		//return inconsistentAbsence;
	}

	
	/**
	 * @author arianna
	 * crea il file PDF con il resoconto mensile dei turni dello IIT
	 * il mese 'month' dell'anno 'year'
	 * (portale sistorg)
	 * 
	 */
	@BasicAuth
	public static void exportMonthAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
		Long shiftCategoryId = params.get("type", Long.class);

		Logger.debug("sono nella exportMonthAsPDF con shiftCategory=%s year=%s e month=%s", shiftCategoryId, year, month);

		final LocalDate firstOfMonth = new LocalDate(year, month, 1);
		final LocalDate lastOfMonth = firstOfMonth.dayOfMonth().withMaximumValue();

		//  Used TreeBasedTable becouse of the alphabetical name order (persona, A/B, num. giorni)
		Table<Person, String, Integer> personsShiftsWorkedDays = TreeBasedTable.<Person, String, Integer>create();

		// Contains the number of the effective hours of worked shifts 
		Table<Person, String, Integer> totalPersonShiftWorkedTime = TreeBasedTable.<Person, String, Integer>create();

		// Contains for each person the numer of days and hours of worked shift
		Table<Person, String, String> totalShiftInfo = TreeBasedTable.<Person, String, String>create();

		// crea la tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
		// (person, [thAbsences, thNoStampings,thBadStampings], <giorni/fasce orarie inconsistenti>)
		Table<Person, String, List<String>> personsShiftInconsistentAbsences = TreeBasedTable.<Person, String, List<String>>create();

		ShiftCategories shiftCategory = ShiftCategories.findById(shiftCategoryId);
		if (shiftCategory == null) {
			notFound(String.format("shiftCategory shiftCategory = %s doesn't exist", shiftCategory));			
		}

		Logger.debug("shiftCategory = %s", shiftCategory);

		// Legge i turni associati alla categoria (es: A, B)
		List<ShiftType> shiftTypes = ShiftType.find("SELECT st FROM ShiftType st WHERE st.shiftCategories = ?", shiftCategory).fetch();

		// for each shift
		for (ShiftType shiftType: shiftTypes)
		{	
			String type = shiftType.type;
			Logger.debug("ELABORA TURNO TYPE=%s", type);

			// seleziona i giorni di turno di tutte le persone associate al turno 'shiftType' da inizio a fine mese
			List<PersonShiftDay> personsShiftDays = personShiftDayDao.getPersonShiftDayByTypeAndPeriod(firstOfMonth, lastOfMonth, shiftType);
			//PersonShiftDay.find("SELECT psd FROM PersonShiftDay psd WHERE date BETWEEN ? AND ? AND psd.shiftType = ? ORDER by date", firstOfMonth, lastOfMonth, shiftType).fetch();

			Logger.debug("CALCOLA IL NUM DI GIORNI EFFETTUATI NEL TURNO PER OGNI PERSONA");
			// conta e memorizza i giorni di turno per ogni persona
			shiftManager.countPersonsShiftsDays(personsShiftDays, personsShiftsWorkedDays);


			// Memorizzo le inconsistenze del turno
			Logger.debug("Chiamo la getShiftInconsistencyTimestampTable PER TROVARE LE INCONSISTENZE del turno %s e memorizzarle", type);
			shiftManager.getShiftInconsistencyTimestampTable(personsShiftDays, personsShiftInconsistentAbsences);

		}

		Logger.debug("CALCOLA I MINUTI MANCANTI DA personsShiftInconsistentAbsences E LI METTE in totalShiftSumHours");
		// Calcola i giorni totali di turno effettuati e le eventuali ore mancanti
		totalPersonShiftWorkedTime = shiftManager.calcShiftWorkedDaysAndLackTime(personsShiftsWorkedDays, personsShiftInconsistentAbsences);

		// save the total requested Shift Hours in the DB
		Logger.debug("AGGIORNA IL DATABASE");
		List<Competence> savedCompetences = shiftManager.updateDBShiftCompetences(totalPersonShiftWorkedTime, year, month);

		// crea la tabella con le informazioni per il report PDF mensile
		totalShiftInfo = shiftManager.getPersonsReportShiftInfo (totalPersonShiftWorkedTime, savedCompetences);

		Options options = new Options();
		options.pageSize = IHtmlToPdfTransformer.A4L;

		ArrayList<String> thInconsistence = new ArrayList<String>(Arrays.asList(Messages.get("PDFReport.thAbsences"), Messages.get("PDFReport.thNoStampings"), Messages.get("PDFReport.thMissingTime"), Messages.get("PDFReport.thBadStampings"), Messages.get("PDFReport.thMissions"), Messages.get("PDFReport.thIncompleteTime"), Messages.get("PDFReport.thWarnStampings")));
		ArrayList<String> thShift = new ArrayList<String>(Arrays.asList(Messages.get("PDFReport.thDays"), Messages.get("PDFReport.thLackTime"), Messages.get("PDFReport.thReqHour"), Messages.get("PDFReport.thAppHour"), Messages.get("PDFReport.thExceededMin")));

		Logger.debug("thInconsistence=%s - thShift=%s", thInconsistence, thShift);

		LocalDate today = new LocalDate();
		String shiftDesc = shiftCategory.description;
		String supervisor = shiftCategory.supervisor.name.concat(" ").concat(shiftCategory.supervisor.surname);

		renderPDF(options, today, firstOfMonth, totalShiftInfo, personsShiftInconsistentAbsences, thInconsistence, thShift, shiftDesc, supervisor);
	}


	/**
	 * @author arianna
	 * crea il file PDF con il calendario mensile dei turni di tipo 'A, B' per
	 * il mese 'month' dell'anno 'year'.
	 * (portale sistorg)
	 * 
	 */
	@BasicAuth
	public static void exportMonthCalAsPDF() {
		int year = params.get("year", Integer.class);
		int month = params.get("month", Integer.class);
		Long shiftCategoryId = params.get("type", Long.class);

		Logger.debug("sono nella exportMonthCalAsPDF con shiftCategory=%s year=%s e month=%s", shiftCategoryId, year, month);

		ShiftCategories shiftCategory = ShiftCategories.findById(shiftCategoryId);
		if (shiftCategory == null) {
			notFound(String.format("shiftCategory shiftCategory = %s doesn't exist", shiftCategory));			
		}

		//ArrayList<String> shiftTypes = new ArrayList<String>();
		List<ShiftType> shiftTypes = ShiftType.find("SELECT st FROM ShiftType st WHERE st.shiftCategories = ?", shiftCategory).fetch();
		Logger.debug("shiftTypes=%s", shiftTypes);

		// crea la tabella dei turni mensile (tipo turno, giorno) -> (persona turno mattina, persona turno pomeriggio)
		Table<String, Integer, ShiftManager.SD> shiftCalendar = HashBasedTable.<String, Integer, ShiftManager.SD>create();


		// prende il primo giorno del mese
		LocalDate firstOfMonth = new LocalDate(year, month, 1);

		for (ShiftType shiftType: shiftTypes){	
			Logger.debug("controlla type=%s", shiftType.type);

			// put the shift information i ìn the calendar shiftCalendar
			shiftManager.buildMonthlyShiftCalendar(firstOfMonth, shiftType, shiftCalendar);

		}

		LocalDate today = new LocalDate();
		String shiftDesc = shiftCategory.description;
		String supervisor = shiftCategory.supervisor.name.concat(" ").concat(shiftCategory.supervisor.surname);
		renderPDF(today, firstOfMonth, shiftCalendar, shiftDesc, supervisor);
	}


	/*
	 * @author arianna
	 * Restituisce la lista delle assenze delle persone di un certo turno in un certo periodo di tempo
	 * 
	 */
	@BasicAuth
	public static void absence() {
		response.accessControl("*");

		String type = params.get("type");

		LocalDate from = new LocalDate(Integer.parseInt(params.get("yearFrom")), Integer.parseInt(params.get("monthFrom")), Integer.parseInt(params.get("dayFrom")));
		LocalDate to = new LocalDate(Integer.parseInt(params.get("yearTo")), Integer.parseInt(params.get("monthTo")), Integer.parseInt(params.get("dayTo")));

		ShiftType shiftType = shiftDao.getShiftTypeByType(type);
		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));			
		}
		Logger.debug("Cerco Turnisti di tipo %s", shiftType.type);

		// get the list of persons involved in the shift of type 'type'
		List<Person> personList = new ArrayList<Person>();
		personList = JPA.em().createQuery("SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p WHERE psst.shiftType.type = :type AND (psst.beginDate IS NULL OR psst.beginDate <= now()) AND (psst.endDate IS NULL OR psst.endDate >= now())")
				.setParameter("type", type)
				.getResultList(); 

		Logger.debug("Shift personList called, found %s shift person", personList.size());

		// Lists of absence for a single shift person and for all persons
		List<Absence> absencePersonShiftDays = new ArrayList<Absence>();

		// List of absence periods
		List<AbsenceShiftPeriod> absenceShiftPeriods = new ArrayList<AbsenceShiftPeriod>();

		if (personList.size() == 0) {
			render(absenceShiftPeriods);
			return;
		}


		absencePersonShiftDays = absenceDao.getAbsenceForPersonListInPeriod(personList, from, to);

		Logger.debug("Trovati %s giorni di assenza", absencePersonShiftDays.size());

		absenceShiftPeriods = shiftManager.getAbsentShiftPeriodsFromAbsentShiftDays(absencePersonShiftDays, shiftType);

		Logger.debug("Find %s absenceShiftPeriod. AbsenceShiftPeriod = %s", absenceShiftPeriods.size(), absenceShiftPeriods.toString());
		render(absenceShiftPeriods);
	}
	

	@BasicAuth
	public static void iCal(@Required String type, @Required int year, Long personId) {
		if (validation.hasErrors()) {
			badRequest("Parametri mancanti. " + validation.errors());
		}
		Optional<User> currentUser = Security.getUser();
//		if (personId == null && currentUser.isPresent() && currentUser.get().person != null) {
//			personId = currentUser.get().person.id;
//		}
		//TODO: fare controlli che il personId corrisponda ad una persona esistente e con
		//un turno del tipo indicato

		response.accessControl("*");
		
		ShiftType shiftType = shiftDao.getShiftTypeByType(type);

		if (shiftType == null) {
			notFound(String.format("ShiftType type = %s doesn't exist", type));
		}
		
		ImmutableList<Person> canAccess =  
				ImmutableList.<Person>builder()
					.addAll(personDao.getPersonForShift(type))
					.add(shiftType.shiftCategories.supervisor).build();
		
		
		if (!currentUser.isPresent() || currentUser.get().person == null 
				|| !canAccess.contains(currentUser.get().person)) {
			log.debug("Accesso all'iCal dei turni non autorizzato: Type = {}, Current User = {}, "
					+ "canAccess = {}", 
				type, currentUser.get(), canAccess, currentUser.get());
			unauthorized();
		}
		
		try {
			Optional<Calendar> calendar = shiftManager.createCalendar(type, Optional.fromNullable(personId), year);
			if (!calendar.isPresent()) {
				notFound(String.format("Person id = %d is not associated to a reperibility of type = %s", personId, type));
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			CalendarOutputter outputter = new CalendarOutputter();
			outputter.output(calendar.get(), bos);
			
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
