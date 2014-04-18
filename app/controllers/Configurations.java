package controllers;

/*
 * Variabile config						|	Parametro html							|	Restrizioni implementate
 * _____________________________________|___________________________________________|_________________________________________________________
 * 
 * inUse								|	inUse									|
 * beginDate							|	inizioValiditaParametri					|
 * endDate	 							|	fineValiditaParametri					|
 * initUseProgram	 					|	inizioUsoProgramma						|
 * instituteName						|	nomeIstituto							|
 * emailToContact	  					|	email									|
 * seatCode								|	codiceSede								|	>0										html5
 * urlToPresence	 					|	urlPresenze								|
 * userToPresence	 					|	userPresenze							|
 * passwordToPresence			 		|	passwordPresenze						|
 * numberOfViewingCoupleColumn	 		|	colonneEntrataUscita					|	>0
 * dayOfPatron							|	giornoPatrono							|	data valida					
 * monthOfPatron						|	mesePatrono								|	data valida
 * 
 * mealTimeStartHour					|	pausaPranzoInizioOre					|											html5
 * mealTimeStartMinute 					|	pausaPranzoInizioMinuti					|											html5	
 * mealTimeEndHour 						|	pausaPranzoFineOre						|											html5
 * mealTimeEndMinute 					|	pausaPranzoFineMinuti					|											html5
 *
 * insertAndModifyWorkingTimeWith		|											|
 * PlusToReduceAtRealWorkingTime		|	configurazioneSegnoPiuPerModifica		|					
 * 
 * addWorkingTimeInExcess 				|	configurazioneTempoLavoroInEccesso		|					
 * isLastDayBeforeXmasEntire			|	configurazioneGiornoInteroPrimaNatale	|					
 * isLastDayBeforeEasterEntire			|	configurazioneGiornoInteroPrimaPasqua	|			
 * isLastDayOfTheYearEntire				|	configurazioneGiornoInteroUltimoDellAnno|				
 * isFirstOrLastMissionDayAHoliday		|	configurazioneMissioneInizioFineFestivo	|				
 * isHolidayInMissionAWorkingDay		|	configurazioneFestivoMissione			|							
 * monthExpiryVacationPastYear		 	|	meseScadenzaFerieAP						|	data valida					
 * dayExpiryVacationPastYear		 	|	giornoScadenzaFerieAP					|	data valida
 * 
 * minimumRemainingTime					|											|
 * ToHaveRecoveryDay					|	tempoMinimoPerAvereRiposoCompensativo	|	>0
 * 
 * monthExpireRecoveryDaysOneThree		|	meseUtilizzoResiduiAP13					|	0 || mese valido
 * monthExpireRecoveryDaysFourNine		|	meseUtilizzoResiduiAP49					|	0 || mese valido
 * maxRecoveryDaysOneThree				|	maxGiorniRecupero13						|	0 || >0
 * maxRecoveryDaysFourNine				|	maxGiorniRecupero49						|	0 || >0
 * maximumOvertimeHours					|	oreMassimeStraordinarioMensili			|	0 || >0
 * 
 * residual								|	configurazioneCompensazioneResidui		|
 * 										|	ConAnnoPrecedente						|	ResidualWithPastYear Enum
 * 								
 * holydaysAndVacationsOverPermitted 	|	configurazioneInserimento				|				
 * 										|	ForzatoFeriePermessi					|
 * 
 * capacityOneThree						|	configurazioneCapienzaRiposi13			|	CapacityCompensatoryRestOneThree Enum
 * capacityFourEight					|	configurazioneCapienzaRiposi49			|	CapacityCompensatoryRestFourEight Enum
 * hourMaxToCalculateWorkTime			|	oraMaxEntroCuiCalcolareUscita			|	ora valida [0-23]							html5
 * canPeopleAutoDeclareWorkingTime		|	configurazioneAutoDichiarazione			|		
 * canPeopleAutoDeclareAbsences			|	configurazioneAutoAssenze				|	
 * canPeopleUseWebStamping				|	configurazioneTimbraturaWeb				|
 *  
 */

import it.cnr.iit.epas.DateUtility;

import java.util.List;

import models.ConfGeneral;
import models.ConfYear;
import models.Configuration;
import models.Office;
import models.enumerate.CapacityCompensatoryRestFourEight;
import models.enumerate.CapacityCompensatoryRestOneThree;
import models.enumerate.ConfigurationFields;
import models.enumerate.ResidualWithPastYear;

import org.joda.time.LocalDate;

import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Configurations extends Controller{
	
	

	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void showConfGeneral(){
		Office office = Security.getUser().person.office;
//		ConfGeneral confGeneral = ConfGeneral.find("select cg from ConfGeneral cg").first();
		LocalDate initUseProgram = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, office));
		String instituteName = ConfGeneral.getFieldValue(ConfigurationFields.InstituteName.description, office);
		Integer seatCode = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.SeatCode.description, office));
		Integer dayOfPatron = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.DayOfPatron.description, office));
		Integer monthOfPatron = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.MonthOfPatron.description, office));
		Boolean webStampingAllowed = new Boolean(ConfGeneral.getFieldValue(ConfigurationFields.WebStampingAllowed.description, office));
		String urlToPresence = ConfGeneral.getFieldValue(ConfigurationFields.UrlToPresence.description, office);
		String userToPresence = ConfGeneral.getFieldValue(ConfigurationFields.UserToPresence.description, office);
		String passwordToPresence = ConfGeneral.getFieldValue(ConfigurationFields.PasswordToPresence.description, office);
		Integer numberOfViewingCouple = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, office));
//		LocalDate initUseProgram = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, office));
//		LocalDate initUseProgram = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, office));
//		LocalDate initUseProgram = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, office));
//		LocalDate initUseProgram = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, office));
//		LocalDate initUseProgram = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, office));
//		render(confGeneral);
		render(initUseProgram, instituteName, seatCode, dayOfPatron, monthOfPatron, webStampingAllowed, urlToPresence, userToPresence,
				passwordToPresence, numberOfViewingCouple);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void showConfYear(){
		LocalDate date = new LocalDate();
		Office office = Security.getUser().person.office;
		
		//last year (non modificabile)
		ConfYear lastConfYear = ConfYear.getConfYear(new LocalDate().getYear()-1);
		Integer lastYearDayExpiryVacationPastYear = ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear()-1, office);
		Integer lastYearMonthExpiryVacationPastYear = ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear()-1, office);
		Integer lastYearMonthExpireRecoveryDaysOneThree = ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear()-1, office);
		Integer lastYearMonthExpireRecoveryDaysFourNine = ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear()-1, office);
		Integer lastYearMaxRecoveryDaysOneThree = ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays13.description, date.getYear()-1, office);
		Integer lastYearMaxRecoveryDaysFourNine = ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays49.description, date.getYear()-1, office);
		Integer lastYearHourMaxToCalculateWorkTime = ConfYear.getFieldValue(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear()-1, office);
		
		//current year (modificabile)
		ConfYear confYear = ConfYear.getConfYear(new LocalDate().getYear());
		Integer dayExpiryVacationPastYear = ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear(), office);
		Integer monthExpiryVacationPastYear = ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear(), office);
		Integer monthExpireRecoveryDaysOneThree = ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear(), office);
		Integer monthExpireRecoveryDaysFourNine = ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear(), office);
		Integer maxRecoveryDaysOneThree = ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays13.description, date.getYear(), office);
		Integer maxRecoveryDaysFourNine = ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays49.description, date.getYear(), office);
		Integer hourMaxToCalculateWorkTime = ConfYear.getFieldValue(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear(), office);
		
		Integer nextYear = new LocalDate().getYear()+1;
		
		render(lastYearDayExpiryVacationPastYear, lastYearMonthExpiryVacationPastYear, lastYearMonthExpireRecoveryDaysOneThree,
				lastYearMonthExpireRecoveryDaysFourNine, lastYearMaxRecoveryDaysOneThree, lastYearMaxRecoveryDaysFourNine,
				lastYearHourMaxToCalculateWorkTime, lastConfYear, dayExpiryVacationPastYear, monthExpiryVacationPastYear,
				monthExpireRecoveryDaysOneThree, monthExpireRecoveryDaysFourNine, monthExpireRecoveryDaysFourNine, maxRecoveryDaysOneThree,
				maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, confYear, nextYear);

	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void saveConfGeneral(String pk, String value){
	//	ConfGeneral confGeneral = ConfGeneral.find("select cg from ConfGeneral cg").first();

		try
		{
			if(pk.equals("webStampingAllowed"))
			{
				Boolean webStampingAllowed = Boolean.parseBoolean(value);
				if(webStampingAllowed!=null){
					ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.office = ? and conf.field = ?", 
							Security.getUser().person.office, ConfigurationFields.WebStampingAllowed.description).first();
					conf.fieldValue = webStampingAllowed.toString();
					conf.save();
					Cache.set(ConfigurationFields.WebStampingAllowed.description, conf.fieldValue);
				}
				//confGeneral.webStampingAllowed = webStampingAllowed;
				//confGeneral.save();
			}
			if(pk.equals("urlToPresence"))
			{
				ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.office = ? and conf.field = ?", 
					Security.getUser().person.office, ConfigurationFields.UrlToPresence.description).first();
				conf.fieldValue = value;
				conf.save();
				Cache.set(ConfigurationFields.UrlToPresence.description, conf.fieldValue);
			}
			if(pk.equals("userToPresence"))
			{
				ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.office = ? and conf.field = ?", 
						Security.getUser().person.office, ConfigurationFields.UserToPresence.description).first();
				conf.fieldValue = value;
				conf.save();
				Cache.set(ConfigurationFields.UserToPresence.description, conf.fieldValue);
			}
			if(pk.equals("passwordToPresence"))
			{
				ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.office = ? and conf.field = ?", 
						Security.getUser().person.office, ConfigurationFields.PasswordToPresence.description).first();
				conf.fieldValue = value;
				conf.save();
				Cache.set(ConfigurationFields.PasswordToPresence.description, conf.fieldValue);
			}
			if(pk.equals("numberOfViewingCoupleColumn"))
			{
				ConfGeneral conf = ConfGeneral.find("Select conf from ConfGeneral conf where conf.office = ? and conf.field = ?", 
						Security.getUser().person.office, ConfigurationFields.NumberOfViewingCouple.description).first();
				Integer numberOfViewingCoupleColumn = Integer.parseInt(value);
				conf.fieldValue = numberOfViewingCoupleColumn.toString();
				conf.save();
				Cache.set(ConfigurationFields.NumberOfViewingCouple.description, conf.fieldValue);
			}
		}		
		catch(Exception e)
		{
			response.status = 500;
			renderText("Bad request");
		}
		//Cache.set("confGeneral", confGeneral);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void saveConfYear(String pk, String value){
		Integer year = new LocalDate().getYear();
//		ConfYear confYear = ConfGeneral.find("select cy from ConfYear cy where cy.year = ?", year).first();
//		if(confYear==null)
//		{
//			//TODO va creato
//			return;
//		}
		
		try
		{
			if(pk.equals("dayExpiryVacationPastYear"))
			{
				Integer day = Integer.parseInt(value);
				try
				{
					ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.year = ? and conf.office = ?",
							ConfigurationFields.DayExpiryVacationPastYear.description, year, Security.getUser().person.office).first();
				//	new LocalDate(year, confYear.monthExpiryVacationPastYear, day);
					conf.fieldValue = day;
					conf.save();
					Cache.set(ConfigurationFields.DayExpiryVacationPastYear.description+year, conf.fieldValue);
				}
				catch(Exception e)
				{
					response.status = 500;
					renderText(day+"/"+ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, Security.getUser().person.office)+"/"+year+" data non valida. Settare correttamente i parametri.");
				}
				
			}
			if(pk.equals("monthExpiryVacationPastYear"))
			{
				Integer month = Integer.parseInt(value);
				try
				{
					ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.year = ? and conf.office = ?",
							ConfigurationFields.MonthExpiryVacationPastYear.description, year, Security.getUser().person.office).first();
					//new LocalDate(year, month, confYear.dayExpiryVacationPastYear);
					conf.fieldValue = month;
					conf.save();
					Cache.set(ConfigurationFields.MonthExpiryVacationPastYear.description+year, conf.fieldValue);
				}
				catch(Exception e)
				{
					response.status = 500;
					renderText(ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, Security.getUser().person.office)+"/"+month+"/"+year+" data non valida. Settare correttamente i parametri.");
				}
			}
			if(pk.equals("monthExpireRecoveryDaysOneThree"))
			{
				Integer val = Integer.parseInt(value);
				if(val<0||val>12)
				{
					response.status = 500;
					renderText("Bad request");
				}
				ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.year = ? and conf.office = ?",
						ConfigurationFields.MonthExpireRecoveryDays13.description, year, Security.getUser().person.office).first();
				conf.fieldValue = val;
				conf.save();
				Cache.set(ConfigurationFields.MonthExpireRecoveryDays13.description+year, conf.fieldValue);
			}
			if(pk.equals("monthExpireRecoveryDaysFourNine"))
			{
				Integer val = Integer.parseInt(value);
				if(val<0||val>12)
				{
					response.status = 500;
					renderText("Bad request");
				}
				ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.year = ? and conf.office = ?",
						ConfigurationFields.MonthExpireRecoveryDays49.description, year, Security.getUser().person.office).first();
				conf.fieldValue = val;
				conf.save();
				Cache.set(ConfigurationFields.MonthExpireRecoveryDays49.description+year, conf.fieldValue);
			}
			if(pk.equals("maxRecoveryDaysOneThree"))
			{
				Integer val = Integer.parseInt(value);
				if(val<0||val>31)
				{
					response.status = 500;
					renderText("Bad request");
				}
				ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.year = ? and conf.office = ?",
						ConfigurationFields.MaxRecoveryDays13.description, year, Security.getUser().person.office).first();
				conf.fieldValue = val;
				conf.save();
				Cache.set(ConfigurationFields.MaxRecoveryDays13.description+year, conf.fieldValue);
			}
			if(pk.equals("maxRecoveryDaysFourNine"))
			{
				Integer val = Integer.parseInt(value);
				if(val<0||val>31)
				{
					response.status = 500;
					renderText("Bad request");
				}
				ConfYear conf = ConfYear.find("Select conf from ConfYear conf where conf.field = ? and conf.year = ? and conf.office = ?",
						ConfigurationFields.MaxRecoveryDays49.description, year, Security.getUser().person.office).first();
				conf.fieldValue = val;
				conf.save();
				Cache.set(ConfigurationFields.MaxRecoveryDays49.description+year, conf.fieldValue);
			}
		}
		catch(Exception e)
		{
			response.status = 500;
			renderText("Bad request");
		}	
		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void show(){

		Configuration configurations = Configuration.getConfiguration(new LocalDate());

		render(configurations);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void update(Configuration conf)
	{
		System.out.println();
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void save(){
		
		Configuration config = null;
		List<Configuration> list = null;
		
		Long configId = params.get("configId", Long.class);
		if(configId==null)
		{
			//nuova configurazione
			 config = new Configuration();
			 config.inUse = false;
		}
		else
		{
			//modifica configurazione
			config = Configuration.findById(configId);
			if(config==null)
			{
				flash.error(String.format("La configurazione che si vuole modificare e' inesistente"));
				render("@Stampings.redirectToIndex");
			}
		}

		Boolean inUse = params.get("inUse", Boolean.class);
		if(inUse==true)
		{
			//mettere not in used tutte le altre
			list = Configuration.getAllConfiguration();
			for(Configuration c : list)
			{
				if(c.id!=configId)
					c.inUse = false;
			}
		}

		if(config.inUse==true && inUse==false)
		{
			flash.error(String.format("La configurazione attuale non può transire dallo stato inUse allo stato notInUse, operazione annullata"));
			render("@list", config);
		}
		config.inUse = inUse;
		
		
		//**********************************************//
		//	Dati di connessione e di visualizzazione	//
		//**********************************************//
		if(!isParamPositiveNumber("codiceSede"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'codice sede'"));
			render("@goback");
		}
		if(!isParamPositiveNumber("colonneEntrataUscita"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'numero colonne entrata uscita'"));
			render("@goback");
		}
		
		config.beginDate = new LocalDate(params.get("inizioValiditaParametri")).toDate();				
		config.endDate = new LocalDate(params.get("fineValiditaParametri")).toDate();
		config.initUseProgram = new LocalDate(params.get("inizioUsoProgramma")).toDate();		
		config.instituteName = params.get("nomeIstituto");										
		config.emailToContact  = params.get("email");											
		config.seatCode = Integer.parseInt(params.get("codiceSede"));
		config.urlToPresence = params.get("urlPresenze");										
		config.userToPresence = params.get("userPresenze");									
		config.passwordToPresence = params.get("passwordPresenze");							
		config.numberOfViewingCoupleColumn = Integer.parseInt(params.get("colonneEntrataUscita"));	
		
		
		
		//**********************************//
		//	Festivita						//
		//**********************************//
		if(!isParamProperData("mesePatrono", "giornoPatrono"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'Festa del Patrono'"));
			render("@goback");
		}
		
		config.dayOfPatron = Integer.parseInt(params.get("giornoPatrono"));					
		config.monthOfPatron = Integer.parseInt(params.get("mesePatrono"));		
	

		//**********************************//
		//	Pausa pranzo e buoni (Disabled)	//
		//**********************************//
		//		if(!isParamPositiveNumber("tempoLavoroGiornalieroPerBuono"))
		//		{
		//			flash.error(String.format("Errore nell'inserimento dati del campo 'tempo lavoro giornaliero per buono'"));
		//			Application.indexAdmin();
		//		}
		//		if(!isParamPositiveNumber("tempoIntervalloPasto"))
		//		{
		//			flash.error(String.format("Errore nell'inserimento dati del campo 'tempo intervallo pasto'"));
		//			Application.indexAdmin();
		//		}
		//		config.workingTime = Integer.parseInt(params.get("tempoLavoroGiornalieroPerBuono"));
		//		config.workingTimeToHaveMealTicket = Integer.parseInt(params.get("tempoIntervalloPasto"));	
		
		config.mealTimeStartHour = Integer.parseInt(params.get("pausaPranzoInizioOre"));
		config.mealTimeStartMinute = Integer.parseInt(params.get("pausaPranzoInizioMinuti"));
		config.mealTimeEndHour = Integer.parseInt(params.get("pausaPranzoFineOre"));
		config.mealTimeEndMinute = Integer.parseInt(params.get("pausaPranzoFineMinuti"));
		
		//**********************************//
		//	Orari e tempi di lavoro			//
		//**********************************//
			
		config.insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime = params.get("configurazioneSegnoPiuPerModifica", Boolean.class);								
		config.addWorkingTimeInExcess = params.get("configurazioneTempoLavoroInEccesso", Boolean.class);							
		config.isLastDayBeforeXmasEntire = params.get("configurazioneGiornoInteroPrimaNatale", Boolean.class);						
		config.isLastDayBeforeEasterEntire = params.get("configurazioneGiornoInteroPrimaPasqua", Boolean.class);						
		config.isLastDayOfTheYearEntire = params.get("configurazioneGiornoInteroUltimoDellAnno", Boolean.class);				
		config.isFirstOrLastMissionDayAHoliday = params.get("configurazioneMissioneInizioFineFestivo", Boolean.class);					
		config.isHolidayInMissionAWorkingDay = params.get("configurazioneFestivoMissione", Boolean.class);										
		
		//TODO params.get("tempoMedioLavoroGiornaliero"); 
		//(Tempo di lavoro giornaliero default (in minuti) valore medio se il tempo di lavoro cambia di giorno in giorno: 432)	
		//per adesso non viene considerato
		
		//**************************************************//
		//	Ferie, permessi, straordinari e residui			//
		//**************************************************//
		if(!isParamProperData("meseScadenzaFerieAP", "giornoScadenzaFerieAP"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'giorno scadenza ferie dell'anno precedente'"));
			render("@goback");
		}
		if(!isParamPositiveNumber("tempoMinimoPerAvereRiposoCompensativo"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'tempo minimo per avere riposo compensativo'"));
			render("@goback");
		}	
		if(!isParamZero("meseUtilizzoResiduiAP13") && !isParamProperMonth("meseUtilizzoResiduiAP13"))	//ne 0 ne mese
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'mese utilizzo residui livelli 1-3'"));
			render("@goback");
		}
		if(!isParamZero("meseUtilizzoResiduiAP49") && !isParamProperMonth("meseUtilizzoResiduiAP49"))	//ne 0 ne mese
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'mese utilizzo residui livelli 4-9'"));
			render("@goback");
		}
		if(!isParamZero("maxGiorniRecupero13") && !isParamPositiveNumber("maxGiorniRecupero13"))		//ne 0 ne positivo
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'massimo giorni di recupero livelli 1-3'"));
			render("@goback");
		}
		if(!isParamZero("maxGiorniRecupero49") && !isParamPositiveNumber("maxGiorniRecupero49"))		//ne 0 ne positivo
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'massimo giorni di recupero livelli 4-9'"));
			render("@goback");
		}
		if(!isParamZero("oreMassimeStraordinarioMensili") && !isParamPositiveNumber("oreMassimeStraordinarioMensili"))		//ne 0 ne positivo
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'ore massime straordinario mensili'"));
			render("@goback");
		}
		
		
		config.monthExpiryVacationPastYear = Integer.parseInt(params.get("meseScadenzaFerieAP"));										
		config.dayExpiryVacationPastYear = Integer.parseInt(params.get("giornoScadenzaFerieAP"));		
		config.minimumRemainingTimeToHaveRecoveryDay = Integer.parseInt(params.get("tempoMinimoPerAvereRiposoCompensativo"));	
		config.monthExpireRecoveryDaysOneThree = Integer.parseInt(params.get("meseUtilizzoResiduiAP13"));	
		config.monthExpireRecoveryDaysFourNine = Integer.parseInt(params.get("meseUtilizzoResiduiAP49"));	
		config.maxRecoveryDaysOneThree = Integer.parseInt(params.get("maxGiorniRecupero13"));	
		config.maxRecoveryDaysFourNine = Integer.parseInt(params.get("maxGiorniRecupero49"));	

		String configurazioneCompensazioneResiduiConAnnoPrecedente = params.get("configurazioneCompensazioneResiduiConAnnoPrecedente");	
		if(configurazioneCompensazioneResiduiConAnnoPrecedente.equals("entroMese"))
			config.residual = ResidualWithPastYear.atMonthInWhichCanUse;
		if(configurazioneCompensazioneResiduiConAnnoPrecedente.equals("mese"))
			config.residual = ResidualWithPastYear.atMonth;
		if(configurazioneCompensazioneResiduiConAnnoPrecedente.equals("giorno"))
			config.residual = ResidualWithPastYear.atDay;

		config.maximumOvertimeHours = Integer.parseInt(params.get("oreMassimeStraordinarioMensili"));	
		config.holydaysAndVacationsOverPermitted  = params.get("configurazioneInserimentoForzatoFeriePermessi", Boolean.class);

		String configurazioneCapienzaRiposi13 = params.get("configurazioneCapienzaRiposi13");	
		if(configurazioneCapienzaRiposi13.equals("residuoGiorno"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onDayResidual;
		if(configurazioneCapienzaRiposi13.equals("residuoFineMese"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndOfMonthResidual;
		if(configurazioneCapienzaRiposi13.equals("residuoFineMesePrecedente"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastMonthResidual;
		if(configurazioneCapienzaRiposi13.equals("residuoFineTrimestre"))
			config.capacityOneThree = CapacityCompensatoryRestOneThree.onEndPastQuarterResidual;


		String configurazioneCapienzaRiposi49 = params.get("configurazioneCapienzaRiposi49");
		if(configurazioneCapienzaRiposi49.equals("residuoGiorno"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onDayResidual;
		if(configurazioneCapienzaRiposi49.equals("residuoFineMese"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndOfMonthResidual;
		if(configurazioneCapienzaRiposi49.equals("residuoFineMesePrecedente"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastMonthResidual;
		if(configurazioneCapienzaRiposi49.equals("residuoFineTrimestre"))
			config.capacityFourEight = CapacityCompensatoryRestFourEight.onEndPastQuarterResidual;
		
		
		
		//**************************************************//
		//	Uscite e autodichiarazioni						//
		//**************************************************//
		if(!isParamProperHour("oraMaxEntroCuiCalcolareUscita"))
		{
			flash.error(String.format("Errore nell'inserimento dati del campo 'ora massima entro cui calcolare l'uscita mancante'"));
			render("@goback");
		}
		config.hourMaxToCalculateWorkTime = params.get("oraMaxEntroCuiCalcolareUscita", Integer.class);		
		config.canPeopleAutoDeclareWorkingTime = params.get("configurazioneAutoDichiarazione", Boolean.class);	
		config.canPeopleAutoDeclareAbsences = params.get("configurazioneAutoAssenze", Boolean.class);	
		
		//**************************************************//
		//	Timbratura via web								//
		//**************************************************//
		
		config.canPeopleUseWebStamping = params.get("configurazioneTimbraturaWeb", Boolean.class);			
		//get text
	
		//**************************************************//
		//	Info per stampa in pdf							//
		//**************************************************//
		// get text
		
		//salvataggio
		
		config.save();
		if(list!=null)
		{
			for(Configuration c : list)
			{
				if(configId!=c.id)
					c.save();
			}
		}
		
		flash.success(String.format("Configurazione modificata con successo!"));
		render("@Stampings.redirectToIndex");
		
	}

	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void insertConfig(){
		Configuration configurations = new Configuration();
		render(configurations);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void edit(Long configId){
		Configuration configurations = Configuration.findById(configId);
			
		render(configurations);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void discard(){
		Configurations.list();
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void list(){
		flash.clear();
		List<Configuration> configList = Configuration.findAll();
		render(configList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void manageExitCausal(){
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void insertExitCausal(){
		
	}
	
	
	/**
	 * 
	 * @param param
	 * @return true se il parametro e' stringa vuota, false altrimenti
	 */
	public static boolean isParamNull(String param)
	{
		String paramString = params.get(param);
		if(paramString.equals(""))
		{
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param param
	 * @return true se il parametro e' un intero maggiore di 0, false altrimenti
	 */
	public static boolean isParamPositiveNumber(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt>=0)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	/**
	 * 
	 * @param param
	 * @return true se il parametro e' 0, false altrimenti
	 */
	public static boolean isParamZero(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt==0)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	/**
	 * 
	 * @param paramMonth
	 * @param paramDay
	 * @return true se i parametri day e month compongono una data valida, false altrimenti
	 */
	public static boolean isParamProperData(String paramMonth, String paramDay)
	{
		try
		{
			Integer month = Integer.parseInt(params.get(paramMonth));										
			Integer day = Integer.parseInt(params.get(paramDay));					
			if(! DateUtility.isFebruary29th(month, day))
			{
				new LocalDate().withMonthOfYear(month).withDayOfMonth(day);
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param param
	 * @return true se il paramentro passato e' compreso fra 1 e 12, false altrimenti
	 */
	public static boolean isParamProperMonth(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt>0 && paramInt<13)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	/**
	 * 
	 * @param param
	 * @return true se il paramentro passato e' compreso fra 0 e 23, false altrimenti
	 */
	public static boolean isParamProperHour(String param)
	{
		try
		{
			Integer paramInt = Integer.parseInt(params.get(param));
			if(paramInt>-1 && paramInt<24)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
}
