package controllers;

import it.cnr.iit.epas.ExportToYaml;
import it.cnr.iit.epas.FromMysqlToPostgres;
import it.cnr.iit.epas.PersonUtility;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.yaml.snakeyaml.Yaml;

import controllers.shib.Shibboleth;
import models.AbsenceType;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.VacationCode;
import models.VacationPeriod;
import models.WorkingTimeType;
import play.Logger;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;


//@With(Shibboleth.class)
public class Administration extends Controller {
	
	
    public static void index() {
        render();
    }
        
    
    public static void importOreStraordinario() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
    	
    	FromMysqlToPostgres.importOreStraordinario();
    	renderText("Importati tutti i dati relativi ai monte ore straordinari");
    }
    
    public static void addPermissionToAll(){
    	
    	FromMysqlToPostgres.addPermissiontoAll();
    	renderText("Aggiunto permesso in sola lettura per tutti gli utenti");
    }
    
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    
    	final int NUMERO_PERSONE_DA_IMPORTARE = 0;
    	
    	final int ANNO_DA_CUI_INIZIARE_IMPORTAZIONE = 2007;
    	
    	int absenceTypes = FromMysqlToPostgres.importAbsenceTypes();
    	//FromMysqlToPostgres.createAbsenceTypeToQualificationRelations();
    	int workingTimeTypes = FromMysqlToPostgres.importWorkingTimeTypes();
    	
    	FromMysqlToPostgres.importAll(NUMERO_PERSONE_DA_IMPORTARE, ANNO_DA_CUI_INIZIARE_IMPORTAZIONE);
      	renderText(
        		String.format("Importate dalla vecchia applicazione %d tipi di assenza con i relativi gruppi e %d tipi di orari di lavoro.\n" +
        			"Importate %d persone con i relativi dati (contratti, dati personali, assenze, timbrature, ...", 
        			absenceTypes, workingTimeTypes, NUMERO_PERSONE_DA_IMPORTARE));
    }
    
    
	public static void test(){
		PersonMonth pm = new PersonMonth(Person.em().getReference(Person.class, 140L), 2012,6);
		long n = pm.getMaximumCoupleOfStampings();
		render(n);
	}
	
	public static void upgradePerson(){
		FromMysqlToPostgres.upgradePerson();
		renderText("Modificati i permessi per l'utente");
	}
	
	public static void updateCompetence() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		//Person person = Person.find("bySurnameAndName", "Lucchesi", "Cristian").first();
	//	FromMysqlToPostgres.updateCompetence();
		renderText("Aggiunti gli straordinari diurni feriali alle persone nella tabella competenze");
	}
	
	public static void updatePersonDay(){
		FromMysqlToPostgres.checkFixedWorkingTime();
		renderText("Aggiornati i person day delle persone con timbratura fissa");
	}
	
	public static void updateVacationPeriodRelation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
		List<Person> personList = Person.getActivePersons(new LocalDate());
		for(Person p : personList){
			Logger.debug("Cerco i contratti per %s %s", p.name, p.surname);
			List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", p).fetch();
			for(Contract con : contractList){
				Logger.debug("Sto analizzando il contratto %s", con.toString());
				Logger.debug("Inizio a creare i periodi di ferie per %s", con.person);
				if(con.expireContract == null){
					VacationPeriod first = new VacationPeriod();
					first.beginFrom = con.beginContract;
					first.endTo = con.beginContract.plusYears(3).minusDays(1);
					first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
					first.contract = con;
					first.save();
					VacationPeriod second = new VacationPeriod();
					second.beginFrom = con.beginContract.plusYears(3);
					second.endTo = null;
					second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
					second.contract =con;
					second.save();
				}
				else{
					if(con.expireContract.isAfter(con.beginContract.plusYears(3).minusDays(1))){
						VacationPeriod first = new VacationPeriod();
						first.beginFrom = con.beginContract;
						first.endTo = con.beginContract.plusYears(3).minusDays(1);
						first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
						first.contract = con;
						first.save();
						VacationPeriod second = new VacationPeriod();
						second.beginFrom = con.beginContract.plusYears(3);
						second.endTo = con.expireContract;
						second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
						second.contract =con;
						second.save();
					}
					else{
						VacationPeriod first = new VacationPeriod();
						first.beginFrom = con.beginContract;
						first.endTo = con.expireContract;
						first.contract = con;
						first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
						first.save();
					}
				}
				
			}
		}
		
	}


	/**
	 * Ricalcolo della situazione di ogni persona a partire da gennaio 2013
	 */
	public static void fixPersonSituationBrowser()
	{ 
		fixPersonSituation(null, 2013, 1);
	}
	
	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personid la persona da fixare, null per fixare tutte le persone
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 */
	public static void fixPersonSituation(Long personid, int year, int month){
		
		// (1) Porto il db in uno stato consistente costruendo tutti gli eventuali person day mancanti
//		JPAPlugin.startTx(false);
//		if(personid==null)
//			PersonUtility.checkHistoryError(null, year, month);
//		else
//			PersonUtility.checkHistoryError(personid, year, month);
//		JPAPlugin.closeTx(false);
		
		// (2) Ricalcolo i valori dei person day aggregandoli per mese
		JPAPlugin.startTx(true);
		List<Person> personList = new ArrayList<Person>();
		if(personid == null)
		{
			personList = Person.findAll();
		}
		else
		{
			Person person = Person.findById(personid);
			personList.add(person);
		}
		JPAPlugin.closeTx(false);
		
		int i = 1;
		
		for(Person p : personList){
			Logger.info("Update person %s (%s di %s)", p.surname, i++, personList.size());
			
			LocalDate actualMonth = new LocalDate(year, month, 1);
			LocalDate endMonth = new LocalDate().withDayOfMonth(1);
			JPAPlugin.startTx(false);
			while(!actualMonth.isAfter(endMonth))
			{
			
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
						p,
						actualMonth, 
						actualMonth.dayOfMonth().withMaximumValue())
						.fetch();
				for(PersonDay pd : pdList){
					pd.populatePersonDay();
				}
				actualMonth = actualMonth.plusMonths(1);
				
				
				
			}
			JPAPlugin.closeTx(false);
		}
		/*
		// (3) Ricalcolo dei residui mensili
		i = 1;
		for(Person p: personList)
		{
			Logger.info("Update residui per %s (%s di %s)", p.surname, i++, personList.size());
			LocalDate actualMonth = new LocalDate(year, month, 1);
			LocalDate endMonth = new LocalDate().withDayOfMonth(1);
			JPAPlugin.startTx(false);
			
			
			//distruggere i personMonth
			//List<PersonMonth> pmList = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = 2013 and pm.month > 0", p).fetch();
			//for(PersonMonth pm : pmList)
			//{
			//	pm.delete();
			
			//}
			
			while(!actualMonth.isAfter(endMonth))
			{
				PersonMonth pm = PersonMonth.build(p, actualMonth.getYear(), actualMonth.getMonthOfYear());
				//pm.aggiornaRiepiloghi();
				pm.save();
				
				actualMonth = actualMonth.plusMonths(1);
				
			}
			JPAPlugin.closeTx(false);
			
		}
		*/
	}
	
	
	
	
	public static void buildYaml()
	{
		//general
		ExportToYaml.buildAbsences("test/dataTest/general/absences.yml");
		ExportToYaml.buildCompetenceCodes("test/dataTest/general/competenceCodes.yml");
		
		//person
		Person person = Person.findById(146l);
		ExportToYaml.buildPerson(person, "test/dataTest/persons/lucchesi.yml");
		
		//test stampings
		ExportToYaml.buildPersonMonth(person, 2013,  9, "test/dataTest/stampings/lucchesiStampingsSettembre2013.yml");
		ExportToYaml.buildPersonMonth(person, 2013, 10, "test/dataTest/stampings/lucchesiStampingsOttobre2013.yml");
		
		//test vacations
		ExportToYaml.buildYearlyAbsences(person, 2012, "test/dataTest/absences/lucchesiAbsences2012.yml");
		ExportToYaml.buildYearlyAbsences(person, 2013, "test/dataTest/absences/lucchesiAbsences2013.yml");
		
	}

    
}
