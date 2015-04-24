package controllers;

import helpers.ModelQuery.SimpleResults;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.YearlyAbsencesManager;
import models.Absence;
import models.AbsenceType;
import models.Person;
import models.User;
import models.enumerate.JustifiedTimeAtWork;
import models.rendering.YearlyAbsencesRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.AbsenceDao;
import dao.OfficeDao;
import dao.PersonDao;

@With( {Resecure.class, RequestInit.class} )
public class YearlyAbsences extends Controller{

	@Inject
	static SecurityRules rules;
		
	@Inject
	static OfficeDao officeDao;
	
	public static void yearlyAbsences(Long personId, int year) {
		//controllo sui parametri
		Person person = null;
		if(personId == null)
			person = Security.getUser().get().person;
		else
			person = PersonDao.getPersonById(personId);
					
		Integer anno = params.get("year", Integer.class);
		Logger.debug("L'id della persona è: %s", personId);
		Logger.debug("La persona è: %s %s", person.name, person.surname);
		Logger.trace("Anno: "+anno);
		
		//rendering 
		if(anno==null){
			LocalDate now = new LocalDate();
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(person, (short)now.getYear());
			render(yearlyAbsencesRecap, year);
		}
		else{
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(person, (short)anno.intValue());
			render(yearlyAbsencesRecap, year, personId, person);
		}		
		
	}

	public static void showGeneralMonthlyAbsences(int year, int month, String name, Integer page) {
		
		if(page==null)
			page=0;
				
		Table<Person, AbsenceType, Integer> tableMonthlyAbsences = TreeBasedTable.create(YearlyAbsencesManager.PersonNameComparator, YearlyAbsencesManager.AbsenceCodeComparator);
		AbsenceType abt = new AbsenceType();
		abt.code = "Totale";		
		
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), 
				false, new LocalDate(year, month,1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true);

		List<Person> persons = simpleResults.paginated(page).getResults();
		LocalDate begin = new LocalDate(year, month, 1);
		LocalDate end = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
		tableMonthlyAbsences = YearlyAbsencesManager.populateMonthlyAbsencesTable(persons, abt, begin, end);
		int numberOfDifferentAbsenceType = tableMonthlyAbsences.columnKeySet().size();
		
		if (!Strings.isNullOrEmpty(name)) {
			Logger.info("filtrare per nome qui... %s", name);
			// TODO: filtrare per nome tableMonthly...
		}

		render(tableMonthlyAbsences, year, month,numberOfDifferentAbsenceType, simpleResults, name, page);

	}
	

	/**
	 * 
	 * @param personId
	 * @param year
	 * Render della pagina absencePerPerson.html che riassume le assenze annuali di una persona
	 */
	
	public static void absencesPerPerson(Integer year){
		
		//controllo sui parametri
		Optional<User> currentUser = Security.getUser();
		if( !currentUser.isPresent() || currentUser.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		User user = currentUser.get();
		//rendering 
		if(year==null){
			LocalDate now = new LocalDate();
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(user.person, (short)now.getYear());
			render(yearlyAbsencesRecap);
		}
		else{
			YearlyAbsencesRecap yearlyAbsencesRecap = new YearlyAbsencesRecap(user.person, (short)year.intValue());
			render(yearlyAbsencesRecap);
		}
	}
	
	
	public static void showPersonMonthlyAbsences(Long personId, Integer year, Integer month, String absenceTypeCode) throws InstantiationException, IllegalAccessException
	{
		
		LocalDate monthBegin = new LocalDate(year, month, 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		
		Person person = PersonDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona inesistente");
			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null, null);
		}
			
		rules.checkIfPermitted(person.office);		
		List<Absence> absenceToRender = new ArrayList<Absence>();
		
		if(absenceTypeCode.equals("Totale"))
		{
			absenceToRender = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), Optional.<String>absent(), monthBegin, monthEnd, Optional.<JustifiedTimeAtWork>absent(), false, true);
		}
		else
		{
			absenceToRender = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), Optional.fromNullable(absenceTypeCode), monthBegin, monthEnd, Optional.<JustifiedTimeAtWork>absent(), false, true);
		}
		
		render(person, absenceToRender);
	}


}
