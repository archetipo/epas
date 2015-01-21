package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import models.ConfGeneral;
import models.Office;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.ConfigurationFields;
import models.rendering.PersonStampingDayRecap;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.google.common.base.Optional;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.UserDao;
import play.Logger;
import play.mvc.Controller;

public class Clocks extends Controller{

	public static void show(){
		LocalDate data = new LocalDate();
		//TODO Capire quali office saranno visibili a questo livello
		List<Office> officeAllowed = OfficeDao.getAllOffices();
		//List<Office> officeAllowed = Office.findAll();
		MainMenu mainMenu = new MainMenu(data.getYear(),data.getMonthOfYear());
		//List<Person> personList = Person.getActivePersonsInMonth(data.getMonthOfYear(), data.getYear(), officeAllowed, false);
		List<Person> personList = PersonDao.list(Optional.<String>absent(), new HashSet(officeAllowed), false, data, data, true).list();
		render(data, personList,mainMenu);
	}
	
	
	public static void clockLogin(Long userId, String password)
	{
		LocalDate today = new LocalDate();
		if(userId==0)
		{
			flash.error("Utente non selezionato");
			Clocks.show();
		}
		
		User user = UserDao.getUserById(userId, Optional.fromNullable(password));
		//User user = User.find("select u from User u where id = ? and password = md5(?)", userId, password).first();
		
		if(user == null)
		{
			flash.error("Password non corretta");
			Clocks.show();
		}
	
		PersonDay personDay = null;			
		Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(user.person, today);
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", user.person, today).first();
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", user.person.name, user.person.surname);
			personDay = new PersonDay(user.person, today);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}
		
		//numero di colonne da visualizzare
		//Configuration conf = Configuration.getCurrentConfiguration();
		//ConfGeneral conf = ConfGeneral.getConfGeneral();
		int minInOutColumn = Integer.parseInt(ConfGeneralManager.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, user.person.office));
		//int minInOutColumn = conf.numberOfViewingCoupleColumn;
		int numberOfInOut = Math.max(minInOutColumn,  PersonUtility.numberOfInOutInPersonDay(personDay));
		
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();				
		PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(personDay,numberOfInOut);
		
		render(user, dayRecap, numberOfInOut);
	}
	
	

	/**
	 * 
	 * @param personId. Con questo metodo si permette l'inserimento della timbratura per la persona contrassegnata da id personId.
	 */
	public static void insertStamping(Long personId){
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		LocalDateTime ldt = new LocalDateTime();
		LocalDateTime time = new LocalDateTime(ldt.getYear(),ldt.getMonthOfYear(),ldt.getDayOfMonth(),ldt.getHourOfDay(),ldt.getMinuteOfHour(),0);
		PersonDay personDay = null;
		Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(person, ldt.toLocalDate());
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, ldt.toLocalDate()).first();
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			personDay = new PersonDay(person, ldt.toLocalDate());
			personDay.create();
		}
		else{
			personDay = pd.get();
		}
		
		//Se la stamping esiste già mostro il riepilogo
		int minNew = time.getMinuteOfHour();
		int hourNew = time.getHourOfDay();
		for(Stamping s : personDay.stampings){
			int hour = s.date.getHourOfDay();
			int min = s.date.getMinuteOfHour();
			int minMinusOne = s.date.plusMinutes(1).getMinuteOfHour();
			if( hour==hourNew && (minNew==min || minNew==minMinusOne) )
			{
				
				flash.error("Timbratura ore %s gia' inserita, prossima timbratura accettata a partire da %s",
						DateUtility.fromLocalDateTimeHourTime(s.date),
						DateUtility.fromLocalDateTimeHourTime(s.date.plusMinutes(2)));
				
				Clocks.showRecap(personId);
			}
		}

		
		//Altrimenti la inserisco
		Stamping stamp = new Stamping();
		stamp.date = time;
		if(params.get("type").equals("true")){
			stamp.way = WayType.in;
		}
		else
			stamp.way = WayType.out;
		stamp.personDay = personDay;
		stamp.markedByAdmin = false;
		stamp.save();
		personDay.stampings.add(stamp);
		personDay.save();
		
		Logger.debug("Faccio i calcoli per %s %s sul personday %s chiamando la populatePersonDay", person.name, person.surname, pd);
		PersonDayManager.populatePersonDay(personDay);
		PersonDayManager.updatePersonDaysInMonth(personDay);
		//pd.save();
		flash.success("Aggiunta timbratura per %s %s", person.name, person.surname);
		
		Clocks.showRecap(personId);
	}
	
	public static void showRecap(Long personId)
	{
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		
		LocalDate today = new LocalDate();
		PersonDay personDay = null;
		Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(person, today);
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, today).first();
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			personDay = new PersonDay(person, today);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}
		
		//numero di colonne da visualizzare
		//Configuration conf = Configuration.getCurrentConfiguration();
		//ConfGeneral conf = ConfGeneral.getConfGeneral();
		//int minInOutColumn = conf.numberOfViewingCoupleColumn;
		int minInOutColumn = Integer.parseInt(ConfGeneralManager.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, person.office));
		int numberOfInOut = Math.max(minInOutColumn,  PersonUtility.numberOfInOutInPersonDay(personDay));
		
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();				
		PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(personDay,numberOfInOut);
		
		render(person, dayRecap, numberOfInOut);
		
	}

}
