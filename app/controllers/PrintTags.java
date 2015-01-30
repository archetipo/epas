package controllers;

import static play.modules.pdf.PDF.renderPDF;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.AbsenceType;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampType;
import models.User;
import models.enumerate.ConfigurationFields;
import models.rendering.PersonStampingDayRecap;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;

import dao.OfficeDao;
import dao.PersonDao;

@With( {Resecure.class, RequestInit.class} )
public class PrintTags extends Controller{
	
	@Inject
	static SecurityRules rules;
	
	public static void showTag(Long personId){
		if(personId == null){
			flash.error("Malissimo! ci vuole un id! Seleziona una persona!");
			Application.indexAdmin();
		}

		if(personId == -1){
			/**
			 * è il caso in cui ho chiesto la stampa cartellino di tutti...vediamo come gestirla in un secondo momento
			 */
		}
		Person person = PersonDao.getPersonById(personId);
		
		rules.checkIfPermitted(person.office);
		int month = params.get("month", Integer.class);
		int year = params.get("year", Integer.class);
	
		int minInOutColumn = Integer.parseInt(ConfGeneralManager.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, person.office));
		int numberOfInOut = Math.max(minInOutColumn, PersonUtility.getMaximumCoupleOfStampings(person, year, month));
		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);
		
		//Costruzione dati da renderizzare
		for(PersonDay pd : totalPersonDays)
		{
			PersonDayManager.computeValidStampings(pd); //calcolo del valore valid per le stamping del mese (persistere??)
		}
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : totalPersonDays )
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;
		
		int numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		int numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		int numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		int basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		Map<AbsenceType,Integer> absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		//RTODO il contratto attivo nel mese (quello più recente)
		/*
		Contract contract = person.getCurrentContract();
		PersonResidualYearRecap c = PersonResidualYearRecap.build(contract, year, null);
		PersonResidualMonthRecap mese = c.getMese(month);
		*/
		
		String titolo = "Situazione presenze mensile " +  DateUtility.fromIntToStringMonth(month) + " " + year + " di " + person.surname + " " + person.name;
		
		//Render
		renderPDF(person, year, month, numberOfInOut, numberOfCompensatoryRestUntilToday,numberOfMealTicketToUse,numberOfMealTicketToRender,
				daysRecap, stampModificationTypeList, stampTypeList, basedWorkingDays, absenceCodeMap, titolo);
		
	}
	
	//@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void listPersonForPrintTags(int year, int month){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		LocalDate date = new LocalDate(year, month,1);
		List<Person> personList = PersonDao.list(Optional.<String>absent(), 
				OfficeDao.getOfficeAllowed(Optional.<User>absent()), false, date, date.dayOfMonth().withMaximumValue(), true).list();
		render(personList, date, year, month);
	}
	
	public static void showPersonTag(Integer year, Integer month){
		
		Person person = Security.getUser().get().person;
		if(!PersonManager.isActiveInMonth(person, month, year, false))
		{
			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}
	
		int minInOutColumn = Integer.parseInt(ConfGeneralManager.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, person.office));
		int numberOfInOut = Math.max(minInOutColumn, PersonUtility.getMaximumCoupleOfStampings(person, year, month));

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);
		
		//Costruzione dati da renderizzare
		for(PersonDay pd : totalPersonDays)
		{
			PersonDayManager.computeValidStampings(pd); //calcolo del valore valid per le stamping del mese (persistere??)
		}
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : totalPersonDays )
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;
		
		int numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		int numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		int numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		int basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		Map<AbsenceType,Integer> absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		String titolo = "Situazione presenze mensile " +  DateUtility.fromIntToStringMonth(month) + " " + year + " di " + person.surname + " " + person.name;
		
		//Render
		renderPDF(person, year, month, numberOfInOut, numberOfCompensatoryRestUntilToday,numberOfMealTicketToUse,numberOfMealTicketToRender,
				daysRecap, stampModificationTypeList, stampTypeList, basedWorkingDays, absenceCodeMap, titolo);
		//renderPDF(person, year, month, numberOfInOut, daysRecap,stampModificationTypeList, stampTypeList);
		
		
	}

}
