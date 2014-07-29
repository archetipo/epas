package controllers;

import java.util.List;

import models.Office;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
public class Application extends Controller {
    
    public static void indexAdmin() {
		Logger.debug("chiamato metodo indexAdmin dell'Application controller");
		render();
       
    }
    
	
    public static void index() {
    	
    	List<Office> officeList = Office.findAll();
    	boolean seatExist = false;
    	for(Office office : officeList) {
    		
    		if(office.isSeat()) {
    			seatExist = true;
    			break;
    		}
    	}
    	
    	if(!seatExist) {
    		
    		Offices.showOffices();
    	}
    	
//		Office office = Office.findById(1L);
//		if(office.code.intValue() == 0){
//			Wizard.wizard(0);
//		}
	
    	if( Security.getUser().get().username.equals("epas.clocks") ){
    		
    		Clocks.show();
    		return;
    	}
    	
    	if( Security.getUser().get().username.equals("admin") ){
    		
    		Persons.list(null);
    		return;
    	}
    	
    	//inizializzazione functional menu dopo login
    	session.put("monthSelected", new LocalDate().getMonthOfYear());
    	session.put("yearSelected", new LocalDate().getYear());
    	session.put("personSelected", Security.getUser().get().person.id);
    	
    	session.put("methodSelected", "stampingsAdmin");
		session.put("actionSelected", "Stampings.stampings");
    	Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
		

    }
    
    
    
}

