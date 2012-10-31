package controllers;

import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.SQLException;
import java.util.List;

import models.Person;
import models.PersonMonth;
import models.WorkingTimeType;
import play.mvc.Controller;

public class Administration extends Controller {
	
    public static void index() {
        render();
    }
        
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    
    	final int NUMERO_PERSONE_DA_IMPORTARE = 0;
    	
    	final int ANNO_DA_CUI_INIZIARE_IMPORTAZIONE = 2007;
    	
    	int absenceTypes = FromMysqlToPostgres.importAbsenceTypes();
    	FromMysqlToPostgres.createAbsenceTypeToQualificationRelations();
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
		FromMysqlToPostgres.updateCompetence();
		renderText("Aggiunti gli straordinari diurni feriali alle persone nella tabella competenze");
	}
    
}
