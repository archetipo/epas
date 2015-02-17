import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

import manager.ConsistencyManager;
import manager.ContractYearRecapManager;
import manager.PersonManager;
import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Contract;
import models.Person;
import models.rendering.VacationsRecap;

import org.apache.commons.mail.EmailException;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.base.Optional;

import play.db.jpa.JPAPlugin;
import play.test.UnitTest;

public class ResidualTest extends UnitTest {
	
    @Test
    public void residualLucchesi() throws EmailException {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	Person person = Person.find("bySurname", "Lucchesi").first();
    	assertEquals(Double.valueOf(146), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday

     	ConsistencyManager.fixPersonSituation(person.id, 2013, 1, person.user, false);

    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = PersonManager.getMonthContracts(person,month, year);
    	for(Contract contract : monthContracts)
		{
    		ContractYearRecapManager.buildContractYearRecap(contract);
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
		for(Contract contract : monthContracts)
		{
			PersonResidualYearRecap c = 
					PersonResidualYearRecap.factory(contract, year, dateToTest);
			if(c.getMese(month)!=null)
				contractMonths.add(c.getMese(month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = VacationsRecap.Factory.build(2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	PersonResidualMonthRecap february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 0);
    	assertEquals(february.monteOreAnnoCorrente, 1445);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(25));	   //maturate(tutte) meno usate 27 - 1	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 28-0
    	assertEquals(februaryVacation.permissionUsed.size(), 2);
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(2));

    }
    
    @Test
    public void residualSanterini() throws EmailException {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.find("bySurnameAndName", "Santerini", "Paolo").first();
    	assertEquals(Double.valueOf(32), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday

    	ConsistencyManager.fixPersonSituation(person.id, 2013, 1, person.user, false);

    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = PersonManager.getMonthContracts(person, month, year);
    	for(Contract contract : monthContracts)
		{
    		ContractYearRecapManager.buildContractYearRecap(contract);
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
		for(Contract contract : monthContracts)
		{
			PersonResidualYearRecap c = 
					PersonResidualYearRecap.factory(contract, year, dateToTest);
			if(c.getMese(month)!=null)
				contractMonths.add(c.getMese(month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = VacationsRecap.Factory.build(2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	PersonResidualMonthRecap february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 3207);
    	assertEquals(february.monteOreAnnoCorrente, 2453);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(28));	   //maturate(tutte) meno usate 	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 
    	assertEquals(februaryVacation.permissionUsed.size(), 0);
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
    
    @Test
    public void residualMartinelli() throws EmailException {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.find("bySurnameAndName", "Martinelli", "Maurizio").first();
    	assertEquals(Double.valueOf(25), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday

    	ConsistencyManager.fixPersonSituation(person.id, 2013, 1, person.user, false);

    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = PersonManager.getMonthContracts(person, month, year);
    	for(Contract contract : monthContracts)
		{
    		ContractYearRecapManager.buildContractYearRecap(contract);
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
		for(Contract contract : monthContracts)
		{
			PersonResidualYearRecap c = 
					PersonResidualYearRecap.factory(contract, year, dateToTest);
			if(c.getMese(month)!=null)
				contractMonths.add(c.getMese(month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = VacationsRecap.Factory.build(2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	PersonResidualMonthRecap february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 29196);
    	assertEquals(february.monteOreAnnoCorrente, 2166);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(25));	   //maturate(tutte) meno usate 	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 
    	assertEquals(februaryVacation.permissionUsed.size(), 0);
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
    
    @Test
    public void residualSuccurro() throws EmailException {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.find("bySurname", "Succurro").first();
    	assertEquals(Double.valueOf(224), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday

    	ConsistencyManager.fixPersonSituation(person.id, 2013, 1, person.user, false);

    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = PersonManager.getMonthContracts(person, month, year);
    	for(Contract contract : monthContracts)
		{
    		ContractYearRecapManager.buildContractYearRecap(contract);
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
		for(Contract contract : monthContracts)
		{
			PersonResidualYearRecap c = 
					PersonResidualYearRecap.factory(contract, year, dateToTest);
			if(c.getMese(month)!=null)
				contractMonths.add(c.getMese(month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = VacationsRecap.Factory.build(2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	PersonResidualMonthRecap february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 32991);
    	assertEquals(february.monteOreAnnoCorrente, 6487);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(24));	   //maturate(tutte) meno usate 	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 
    	assertEquals(februaryVacation.permissionUsed.size(), 0);
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
    
    @Test
    public void residualAbba() throws EmailException {
    	LocalDate dateToTest = new LocalDate(2014,2,28);
    	int month = 2;
    	int year = 2014;
    	
    	JPAPlugin.startTx(false);
    	Person person = Person.find("bySurname", "Abba").first();
    	assertEquals(Double.valueOf(2), Double.valueOf(person.id));
    
    	
    	//Ricalcolo tutti i personday

    	ConsistencyManager.fixPersonSituation(person.id, 2013, 1, person.user, false);

    	JPAPlugin.startTx(false);

    	//Ricalcolo tutti i contract year recap
    	List<Contract> monthContracts = PersonManager.getMonthContracts(person,month, year);
    	for(Contract contract : monthContracts)
		{
    		ContractYearRecapManager.buildContractYearRecap(contract);
		}
    	assertEquals(monthContracts.size(),1);

    	//Costruisco la situazione residuale al 28 febbraio (già concluso)
		List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
		for(Contract contract : monthContracts)
		{
			PersonResidualYearRecap c = 
					PersonResidualYearRecap.factory(contract, year, dateToTest);
			if(c.getMese(month)!=null)
				contractMonths.add(c.getMese(month));
		}
		
		//Costruisco la situazione ferie al 28 febbraio (già concluso)
		List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
		for(Contract contract : monthContracts)
		{
			VacationsRecap vr = VacationsRecap.Factory.build(2014, contract, dateToTest, true);
			contractVacationRecap.add(vr);
		}
		JPAPlugin.closeTx(false);
    	
	
		assertEquals(contractMonths.size(),1);
		assertEquals(contractVacationRecap.size(),1);
		
		//asserzioni sui residui
    	PersonResidualMonthRecap february = contractMonths.get(0);
    	assertEquals(february.monteOreAnnoPassato, 0);
    	assertEquals(february.monteOreAnnoCorrente, 0);
    	
    	VacationsRecap februaryVacation = contractVacationRecap.get(0);
    	//asserzioni sui vacation recap
    	assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(19));	   //maturate(tutte) meno usate 	
    	assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28)); //totali meno usate 
    	assertEquals(februaryVacation.permissionUsed.size(), 0);
    	assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
 

}
