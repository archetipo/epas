package controllers;

import helpers.ModelQuery.SimpleResults;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import manager.CompetenceManager;
import manager.recaps.competence.PersonMonthCompetenceRecap;
import manager.recaps.competence.PersonMonthCompetenceRecapFactory;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.TotalOvertime;
import models.User;
import models.rendering.PersonCompetenceRecap;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import controllers.Resecure.NoCheck;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperCompetenceCode;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class} )
public class Competences extends Controller{

	@Inject
	static SecurityRules rules;
	@Inject
	static IWrapperFactory wrapperFactory;
	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory; 
	@Inject
	static OfficeDao officeDao;
	@Inject
	static CompetenceManager competenceManager;
	@Inject 
	static PersonMonthCompetenceRecapFactory personMonthCompetenceRecapFactory;
	@Inject
	static CompetenceDao competenceDao;
	
	private final static Logger log = LoggerFactory.getLogger(Competences.class);
	
	public static void competences(int year, int month) {

		Optional<User> user = Security.getUser();
		
		if( ! user.isPresent() || user.get().person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		Person person = user.get().person;
		
		Optional<Contract> contract = wrapperFactory.create(person)
				.getLastContractInMonth(year, month);
		
		if(! contract.isPresent() ) {
			flash.error("Nessun contratto attivo nel mese.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		PersonMonthCompetenceRecap personMonthCompetenceRecap =
				personMonthCompetenceRecapFactory.create(contract.get(), month, year);

		render(personMonthCompetenceRecap, person, year, month);

	}


	public static void showCompetences(Integer year, Integer month, Long officeId, String name, String codice, Integer page){
		
		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		rules.checkIfPermitted(office);
		if(page==null)
			page = 0;
				
		List<CompetenceCode> activeCompetenceCodes = competenceManager.activeCompetence(year);
		
		IWrapperCompetenceCode competenceCode = null;
		
		if(activeCompetenceCodes.size() == 0) {
			flash.error("Per visualizzare la sezione Competenze è necessario abilitare almeno un codice competenza ad un dipendente.");
			Competences.enabledCompetences(officeId, null);
		}
		
		if(codice==null || codice=="") {
			competenceCode = wrapperFactory.create(activeCompetenceCodes.get(0));
		}
		else
		{
			for(CompetenceCode compCode : activeCompetenceCodes)
			{
				if(compCode.code.equals(codice))
					competenceCode = wrapperFactory.create(compCode);
			}
		}		
		
		SimpleResults<Person> simpleResults = PersonDao.listForCompetence(competenceCode.getValue(),
				Optional.fromNullable(name), Sets.newHashSet(office), false, 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());

		List<IWrapperPerson> activePersons = FluentIterable
				.from(simpleResults.paginated(page).getResults())
				.transform(wrapperFunctionFactory.person()).toList();
		
		if(activePersons == null)
			activePersons = new ArrayList<IWrapperPerson>();
		
		//Redirect in caso di mese futuro
		LocalDate today = new LocalDate();
		if(today.getYear()==year && month>today.getMonthOfYear())
		{
			flash.error("Impossibile accedere a situazione futura, redirect automatico a mese attuale");
			month = today.getMonthOfYear();
			//FIXME impostare il redirect!!
		}
		
		for(IWrapperPerson p : activePersons){
			Competence competence = null;
			for(CompetenceCode c : p.getValue().competenceCode){
				Optional<Competence> comp = competenceDao.getCompetence(p.getValue(), year, month, c);
				if(!comp.isPresent()){
					competence = new Competence(p.getValue(), c, year, month);
					competence.valueApproved = 0;
					competence.save();
				}
					
			}
		}
		List<String> code = CompetenceManager.populateListWithOvertimeCodes();		
				
		List<Competence> competenceList = competenceDao.getCompetences(Optional.<Person>absent(),year, month, code, office, false);
		int totaleOreStraordinarioMensile = CompetenceManager.getTotalMonthlyOvertime(competenceList);
		
		List<Competence> competenceYearList = competenceDao.getCompetences(Optional.<Person>absent(),year, month, code, office, true);		
		int totaleOreStraordinarioAnnuale = CompetenceManager.getTotalYearlyOvertime(competenceYearList);
		
		List<TotalOvertime> total = competenceDao.getTotalOvertime(year, office);				
		int totaleMonteOre = CompetenceManager.getTotalOvertime(total);		
		
		render(year, month, office, offices, activePersons, totaleOreStraordinarioMensile, totaleOreStraordinarioAnnuale, 
				totaleMonteOre, simpleResults, name, codice, activeCompetenceCodes, competenceCode);

	}
	

	public static void updateCompetence(long pk, String name, Integer value){
		final Competence competence = competenceDao.getCompetenceById(pk);
				
		notFoundIfNull(competence);
		if (validation.hasErrors()) {
			error(Messages.get(Joiner.on(",").join(validation.errors())));
		}
		rules.checkIfPermitted(competence.person.office);
		
		log.info("Anno competenza: {} Mese competenza: {}", 
				new Object[]{competence.year, competence.month});
		
		competence.valueApproved = value;

		log.info("value approved before = {}", 
				new Object[]{competence.valueApproved});
		
		competence.save();
		
		log.info("saved id={} (person={}) code={} (value={})", 
				new Object[] { competence.id, competence.person, 
				competence.competenceCode.code, competence.valueApproved} );

		renderText("ok");
	}
		
	@NoCheck
	public static void manageCompetenceCode(){
		rules.checkIfPermitted();
		List<CompetenceCode> compCodeList = CompetenceCodeDao.getAllCompetenceCode();
		render(compCodeList);
	}
	
	public static void insertCompetenceCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CompetenceCode code = new CompetenceCode();
		render(code);
	}

	
	public static void edit(Long competenceCodeId){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		CompetenceCode code = CompetenceCodeDao.getCompetenceCodeById(competenceCodeId);
		render(code);
	}

	
	public static void save(Long competenceCodeId){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		String codice = params.get("codice");
		String descrizione = params.get("descrizione");
		String codiceAtt = params.get("codiceAttPres");
		if(CompetenceManager.setNewCompetenceCode(competenceCodeId, codice, descrizione, codiceAtt)){
			flash.success(String.format("Codice %s aggiunto con successo", codice));
		}
		else{
			flash.error(String.format("Il codice competenza %s è già presente nel database. Cambiare nome al codice.", codice));
		}
		Application.indexAdmin();
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void discard(){
		manageCompetenceCode();
	}
	
	
	public static void totalOvertimeHours(int year, Long officeId){
	
		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());		
		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}
		
		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		
		rules.checkIfPermitted(office);
		
		List<TotalOvertime> totalList = competenceDao.getTotalOvertime(year, office);
		int totale = CompetenceManager.getTotalOvertime(totalList);		
		
		render(totalList, totale, year, office, offices);
	}

	public static void saveOvertime(Integer year, String numeroOre, Long officeId){

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		
		rules.checkIfPermitted(office);
		if(competenceManager.saveOvertime(year, numeroOre, officeId)){
			flash.success(String.format("Aggiornato monte ore per l'anno %s", year));
		}
		else{
			flash.error("Inserire il segno (+) o (-) davanti al numero di ore da aggiungere (sottrarre)");
		}		
		
		Competences.totalOvertimeHours(year, officeId);
	}

	public static void overtime(int year, int month, Long officeId, String name, Integer page){
		
		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);

		rules.checkIfPermitted(office);
		
		if(page == null)
			page = 0;
		Table<Person, String, Integer> tableFeature = null;
		LocalDate beginMonth = null;
		if(year == 0 && month == 0){
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			beginMonth = new LocalDate(yearParams, monthParams, 1);
		}
		else{
			beginMonth = new LocalDate(year, month, 1);
		}
		CompetenceCode code = CompetenceCodeDao.getCompetenceCodeByCode("S1");
		SimpleResults<Person> simpleResults = PersonDao.listForCompetence(code, Optional.fromNullable(name), 
				Sets.newHashSet(office), 
				false, 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
		tableFeature = competenceManager.composeTableForOvertime(year, month, page, name, office, beginMonth, simpleResults, code);
		
		if(year != 0 && month != 0)
			render(tableFeature, year, month, simpleResults, name, office, offices);
		else{
			int yearParams = params.get("year", Integer.class);
			int monthParams = params.get("month", Integer.class);
			render(tableFeature,yearParams,monthParams,simpleResults, name, office, offices );
		}
	}

	/**
	 * funzione che ritorna la tabella contenente le competenze associate a ciascuna persona
	 */
	public static void enabledCompetences(Long officeId, String name){

		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());

		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}

		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		rules.checkIfPermitted(office);
		LocalDate date = new LocalDate();		
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(office), 
				false, date, date.dayOfMonth().withMaximumValue(), true);
				
		List<Person> personList = simpleResults.list();		
		Table<Person, String, Boolean> tableRecapCompetence = CompetenceManager.getTableForEnabledCompetence(personList);		
		int month = date.getMonthOfYear();
		int year = date.getYear();
		render(tableRecapCompetence, month, year, office, offices, simpleResults, name);
	}

	/**
	 * 
	 * @param personId render della situazione delle competenze per la persona nella form updatePersonCompetence
	 */
	public static void updatePersonCompetence(Long personId){
		
		if(personId == null){
			
			flash.error("Persona inesistente");
			Application.indexAdmin();
		}		
		Person person = PersonDao.getPersonById(personId);
		rules.checkIfPermitted(person.office);
		PersonCompetenceRecap pcr = new PersonCompetenceRecap(person);
		
		render(pcr,person);
	}

	/**
	 *  salva la nuova configurazione di competenze per la persona
	 */
	public static void saveNewCompetenceConfiguration(Long personId, Map<String, Boolean> competence){
		final Person person = PersonDao.getPersonById(personId);
		notFoundIfNull(person);
		rules.checkIfPermitted(person.office);
		
		List<CompetenceCode> competenceCode = CompetenceCodeDao.getAllCompetenceCode();
		if(CompetenceManager.saveNewCompetenceEnabledConfiguration(competence, competenceCode, person))
			flash.success(String.format("Aggiornate con successo le competenze per %s %s", person.name, person.surname));
		Competences.enabledCompetences( person.office.id, null);

	}
	
	
	public static void exportCompetences(){
		rules.checkIfPermitted("");
		render();
	}	
	
	public static void getOvertimeInYear(int year) throws IOException{
		
		rules.checkIfPermitted("");
		Office office = Security.getUser().get().person.office;
		SimpleResults<Person> simpleResults = PersonDao.listForCompetence(CompetenceCodeDao.getCompetenceCodeByCode("S1"), 
				Optional.fromNullable(""), 
				Sets.newHashSet(office), 
				false, 
				new LocalDate(year, 1, 1), 
				new LocalDate(year, 12, 1).dayOfMonth().withMaximumValue());
		
		List<Person> personList = simpleResults.list();
		FileInputStream inputStream = competenceManager.getOvertimeInYear(year, personList);
		renderBinary(inputStream, "straordinari"+year+".csv");
	}
	
	/**
	 * TODO: implementare un metodo nel manager nel quale spostare la business logic
	 * di questa azione.
	 * 
	 * @param officeId
	 * @param year
	 * @param onlyDefined true se si vuole applicare il calcolo ai soli tempi determinati
	 */
	public static void approvedCompetenceInYear(Long officeId, int year, boolean onlyDefined) {
		
		Office office = officeDao.getOfficeById(officeId);
		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());
		
		notFoundIfNull(office);
		rules.checkIfPermitted(office);
		
		Set<Person> personSet = Sets.newHashSet();
		
		Map<CompetenceCode, Integer> totalValueAssigned = Maps.newHashMap();
		
		Map<Person, Map<CompetenceCode, Integer>> mapPersonCompetenceRecap = Maps.newHashMap();
		
		List<Competence> competenceInYear = competenceDao
				.getCompetenceInYear(year, Optional.fromNullable(office));
				
		for (Competence competence: competenceInYear) {
			
			//Filtro tipologia del primo contratto nel mese della competenza
			if (onlyDefined) {
				IWrapperPerson wperson = wrapperFactory.create(competence.person);
				Optional<Contract> firstContract = wperson.getFirstContractInMonth(year, competence.month);
				if (!firstContract.isPresent())
					continue;	//questo errore andrebbe segnalato, competenza senza che esista contratto
				
				IWrapperContract wcontract = wrapperFactory.create(firstContract.get());
				
				if (!wcontract.isDefined()) {
					continue;	//scarto la competence.
				}
					
			}
			
			//Filtro competenza non approvata
			if (competence.valueApproved == 0)
				continue;
			
			personSet.add(competence.person);
			
			//aggiungo la competenza alla mappa della persona
			Person person = competence.person;
			Map<CompetenceCode, Integer> personCompetences = mapPersonCompetenceRecap.get(person);
			
			if (personCompetences == null) 
				personCompetences = Maps.newHashMap();
			
			Integer value = personCompetences.get(competence.competenceCode);
			if (value != null) 
				value = value + competence.valueApproved;
			else
				value = competence.valueApproved;
			
			personCompetences.put(competence.competenceCode, value);
			
			mapPersonCompetenceRecap.put( person, personCompetences);
			
			//aggiungo la competenza al valore totale per la competenza
			value = totalValueAssigned.get(competence.competenceCode);
			
			if (value != null)
				value = value + competence.valueApproved;
			else
				value = competence.valueApproved;
			
			totalValueAssigned.put(competence.competenceCode, value);
			
		}
		
		List<IWrapperPerson> personList = FluentIterable
				.from(personSet)
				.transform(wrapperFunctionFactory.person()).toList();
		
		//FIXME inserisco il month per la navigazione delle tab competenze.
		// andrebbe un pò rifattorizzata questa parte...
		
		int month = LocalDate.now().getMonthOfYear();
		
		render(personList, totalValueAssigned, mapPersonCompetenceRecap, office, offices, year, month);
		
	}
	
}
