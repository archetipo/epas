package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateUtility;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import manager.MonthRecapManager;

import manager.PersonDayManager;
import models.Competence;
import models.Person;
import models.PersonDay;
import models.User;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;

@With( {Resecure.class, RequestInit.class} )
public class MonthRecaps extends Controller{

	@Inject
	static SecurityRules rules;
	
	/**
	 * Controller che gestisce il calcolo del Riepilogo Mensile.
	 * @param year
	 * @param month
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	
	public static void show(int year, int month, String name, Integer page) {

		if(page == null)
			page = 0;
		
		rules.checkIfPermitted("");
		
		LocalDate today = new LocalDate();
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		LocalDate lastDayOfMonth = monthBegin;

		//numero di giorni lavorativi in month
		int generalWorkingDaysOfMonth = 0;
		while(!lastDayOfMonth.isAfter(monthEnd))
		{
			if(!lastDayOfMonth.isBefore(today))
				break;
			if(lastDayOfMonth.getDayOfWeek()==6 || lastDayOfMonth.getDayOfWeek()==7)
			{
				lastDayOfMonth = lastDayOfMonth.plusDays(1);
				continue;
			}
			if( ! DateUtility.isGeneralHoliday(null, lastDayOfMonth) )
			{
				generalWorkingDaysOfMonth++;
			}
			lastDayOfMonth = lastDayOfMonth.plusDays(1);
		}
		lastDayOfMonth = lastDayOfMonth.minusDays(1);
		
		Table<Person, String, Integer> tableMonthRecap = TreeBasedTable.create(MonthRecapManager.PersonNameComparator, MonthRecapManager.AbsenceCodeComparator);

		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				OfficeDao.getOfficeAllowed(Optional.<User>absent()), false, monthBegin, monthEnd, true);

		List<Person> activePersons = simpleResults.paginated(page).getResults();
		
		//logica mese attuale
		if(today.getYear()==year && today.getMonthOfYear()==month)
		{
			//Se oggi e' il primo giorno del mese stampo la tabella vuota 
			if(today.getDayOfMonth()==1)
			{
				tableMonthRecap = MonthRecapManager.populateDefaultTable(activePersons);
				render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth, year, month, simpleResults, name);
				return;
			}
			//Considero il riepilogo fino a ieri
			monthEnd = today.minusDays(1);			
		}	
		
		tableMonthRecap = MonthRecapManager.populateRealValueTable(activePersons, monthBegin, monthEnd, year, generalWorkingDaysOfMonth);

		render(tableMonthRecap, generalWorkingDaysOfMonth, today, lastDayOfMonth, year, month, simpleResults, name);

	}
	
	/**
	 * Controller che ritorna la lista dei Giorni di assenza non giustificati.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void notJustifiedAbsences(Long personId, int year, int month){
		
		Person person = PersonDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> notJustifiedAbsences = MonthRecapManager.getPersonDayListRecap(personId, year, month, "notJustifiedAbsences");

		render(notJustifiedAbsences, person);
	}
	
	/**
	 * Controller che ritorna la lista dei Giorni di assenza giustificati.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void justifiedAbsences(Long personId, int year, int month){
		
		Person person = PersonDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> justifiedAbsences = MonthRecapManager.getPersonDayListRecap(personId, year, month, "justifiedAbsences");

		render(justifiedAbsences, person);
	}

	/**
	 * Controller che ritorna la lista dei Giorni di presenza al lavoro nei giorni festivi.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void workingDayHoliday(Long personId, int year, int month){
		
		Person person = PersonDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> workingDayHoliday = MonthRecapManager.getPersonDayListRecap(personId, year, month, "workingDayHoliday");

		render(workingDayHoliday, person);
	}
	
	/**
	 * Controller che ritorna la lista dei Giorni di presenza al lavoro nei giorni lavorativi.
	 * @param id
	 * @param year
	 * @param month
	 */	
	public static void workingDayNotHoliday(Long personId, int year, int month){
		
		Person person = PersonDao.getPersonById(personId);
		if(person == null){
			flash.error("Persona non presente in anagrafica");
			MonthRecaps.show(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<PersonDay> workingDayNotHoliday = MonthRecapManager.getPersonDayListRecap(personId, year, month, "workingDayNotHoliday");

		render(workingDayNotHoliday, person);
	}
	
}
