package controllers;

import java.sql.SQLException;

import models.Person;
import models.PersonMonth;

import it.cnr.iit.epas.FromMysqlToPostgres;
import it.cnr.iit.epas.PopulatePersonDay;
import play.mvc.Controller;
import play.mvc.results.RenderText;

public class Administration extends Controller {

    public static void index() {
        render();
    }
    
    public static void beforeImportAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
    	FromMysqlToPostgres.createQualifications();
    	FromMysqlToPostgres.importAbsenceTypes();
    	FromMysqlToPostgres.createAbsenceTypeToQualificationRelations();
    	FromMysqlToPostgres.importWorkingTimeTypes();
    	FromMysqlToPostgres.createWorkinTimeTypeNormaleMod();
    	FromMysqlToPostgres.createStampModificationType();
    	
    	renderText("Importate le qualifiche, i tipi di assenza e i tipi di orari di lavoro");
    }
    
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    	
    	FromMysqlToPostgres.importAll();
    	renderText("Importate tutte le persone dalla vecchia applicazione + aggiunti i workingtimetypeday e aggiunti i permessi" +
    			"di amministrazione per l'utente con id 139.");
    }
    
    /**
     * aggiunto metodo di popolamento iniziale del personDay di modo da rendere tutte le informazioni su ciascuna persona persistenti
     * sul db già al momento della prima visualizzazione.
     */
    public static void populatePersonDay(){
    	//PopulatePersonDay.PopulatePersonDayForAll();
    	renderText("Calcolate tutte le informazioni su tempi di lavoro, progressivo e differenza per i person day del range di utenti selezionati");
    }
    
    /**
     * metodo per aggiungere i workingTimeTypeDay per il workingTimeType normale-mod 
     */
    public static void addWorkingTimeType(){
    	PopulatePersonDay.fillWorkingTimeTypeDays();
    	renderText("Aggiunti days per il working time type normal-mod");
    }
        
    public static void manageStampType(){
    	PopulatePersonDay.manageStampType();
    	renderText("Sistemata situazione degli stamp type");
    }
    
    public static void populatePermissions(){
    	PopulatePersonDay.personPermissions();
    	renderText("Aggiunti permessi a person con id 139");
    }
    
	public static void test(){
		PersonMonth pm = new PersonMonth(Person.em().getReference(Person.class, 140L), 2012,6);
		long n = pm.getMaximumCoupleOfStampings();
		render(n);
	}
    
}
