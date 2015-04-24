package controllers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ChartsManager;
import manager.ChartsManager.Month;
import manager.ChartsManager.RenderList;
import manager.ChartsManager.RenderResult;
import manager.ChartsManager.Year;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.exports.PersonOvertime;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import exceptions.EpasExceptionNoSourceData;

@With( {Secure.class, RequestInit.class} )
public class Charts extends Controller{

	@Inject
	static SecurityRules rules;
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static ChartsManager chartsManager;
	
	@Inject
	static CompetenceDao competenceDao;

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtimeOnPositiveResidual(Integer year, Integer month){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);
		List<Month> meseList = ChartsManager.populateMonthList();	

		if(params.get("yearChart") == null || params.get("monthChart") == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList, meseList);
		}

		year = params.get("yearChart", Integer.class);
		month = params.get("monthChart", Integer.class);
		
		List<Person> personeProva = PersonDao.list(Optional.<String>absent(),
				officeDao.getOfficeAllowed(Security.getUser().get()), true, 
				new LocalDate(year,month,1), new LocalDate(year, month,1).dayOfMonth().withMaximumValue(), true).list();
		
		List<CompetenceCode> codeList = ChartsManager.populateOvertimeCodeList();
		List<PersonOvertime> poList = chartsManager.populatePersonOvertimeList(personeProva, codeList, year, month);
		
		render(poList, year, month, annoList, meseList);
	}

	
	public static void indexCharts(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		render();
	}

	
	public static void overtimeOnPositiveResidualInYear(Integer year){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);

		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}
		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);
		
		List<CompetenceCode> codeList = ChartsManager.populateOvertimeCodeList();
		Long val = null;
		Optional<Integer> result = competenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.<Integer>absent(), Optional.<Person>absent(), codeList);
		if(result.isPresent())
			val = result.get().longValue();

		List<Person> personeProva = PersonDao.list(Optional.<String>absent(),
				officeDao.getOfficeAllowed(Security.getUser().get()), true, new LocalDate(year,1,1), new LocalDate(year,12,31), true).list();
		int totaleOreResidue = chartsManager.calculateTotalResidualHour(personeProva, year);

		render(annoList, val, totaleOreResidue);

	}

	
	public static void whichAbsenceInYear(Integer year){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);


		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}

		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);

		
		List<String> absenceCode = Lists.newArrayList();
		absenceCode.add("92");
		absenceCode.add("91");
		absenceCode.add("111");
		LocalDate beginYear = new LocalDate(year, 1,1);
		LocalDate endYear = beginYear.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue();
		Long missioniSize = AbsenceDao.howManyAbsenceInPeriod(beginYear, endYear, "92");
		Long riposiCompensativiSize = AbsenceDao.howManyAbsenceInPeriod(beginYear, endYear, "91");
		Long malattiaSize = AbsenceDao.howManyAbsenceInPeriod(beginYear, endYear, "111");
		Long altreSize = AbsenceDao.howManyAbsenceInPeriodNotInList(beginYear, endYear, absenceCode);

		render(annoList, missioniSize, riposiCompensativiSize, malattiaSize, altreSize);

	}

	
	public static void checkLastYearAbsences(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		render();
	}

	
	public static void processLastYearAbsences(Blob file){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		RenderList render = ChartsManager.checkSituationPastYear(file);
		List<RenderResult> listTrueFalse = render.getListTrueFalse();
		List<RenderResult> listNull = render.getListNull();
		
		render(listTrueFalse, listNull);
	}

	public static void exportHourAndOvertime(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);

		render(annoList);
	}

	public static void export(Integer year) throws IOException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		List<Person> personList = PersonDao.list(Optional.<String>absent(), 
				officeDao.getOfficeAllowed(Security.getUser().get()), true, new LocalDate(year,1,1), LocalDate.now(), true).list();
		Logger.debug("Esporto dati per %s persone", personList.size());
		FileInputStream inputStream = chartsManager.export(year, personList);
		
		renderBinary(inputStream, "straordinariOreInPiuERiposiCompensativi"+year+".csv");
	}

	public static void exportFinalSituation(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		Set<Office> offices = Sets.newHashSet();
		offices.add(Security.getUser().get().person.office);
		String name = null;
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, LocalDate.now(), LocalDate.now(), true).list();
		render(personList);
	}

	public static void exportDataSituation(Long personId) throws IOException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		Person person = PersonDao.getPersonById(personId);
		
		try {
			
			FileInputStream inputStream = chartsManager.exportDataSituation(person);
			renderBinary(inputStream, "exportDataSituation"+person.surname+".csv");
			
		} catch (EpasExceptionNoSourceData e) {
    		flash.error("Mancano i dati di inizializzazione per " 
    				+ person.fullName());
    		renderTemplate("Application/indexAdmin.html");
		}
		

	}
}
