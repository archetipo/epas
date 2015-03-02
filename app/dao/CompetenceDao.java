package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import java.util.List;

import manager.recaps.PersonResidualYearRecapFactory;
import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.PersonHourForOvertime;
import models.PersonReperibilityType;
import models.TotalOvertime;
import models.query.QCompetence;
import models.query.QPerson;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibilityType;
import models.query.QTotalOvertime;
import play.Logger;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import dao.wrapper.IWrapperFactory;

/**
 * 
 * @author dario
 *
 */
public class CompetenceDao {

	@Inject
	public IWrapperFactory wrapperFactory;
	
	@Inject
	public PersonResidualYearRecapFactory yearFactory;
	
	/**
	 * 
	 * @param id
	 * @return la competenza relativa all'id passato come parametro
	 */
	public static Competence getCompetenceById(Long id){
		QCompetence competence = QCompetence.competence;
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.id.eq(id));
		
		return query.singleResult(competence);
		
	}
	
	/**
	 * 
	 * @param person
	 * @param code
	 * @param year
	 * @param month
	 * @return
	 */
	public static SimpleResults<Competence> list(
			Optional<Person> person, Optional<String> code, 
			Optional<Integer> year, Optional<Integer> month) {
		
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if (person.isPresent()) {
			condition.and(competence.person.eq(person.get()));
		}
		if (code.isPresent()) {
			condition.and(competence.competenceCode.code.eq(code.get()));
		}
		if (year.isPresent()) {
			condition.and(competence.year.eq(year.get()));
		}
		if (month.isPresent()) {
			condition.and(competence.month.eq(month.get()));
		}
		final JPQLQuery query = ModelQuery.queryFactory().from(competence).where(condition);
		return ModelQuery.simpleResults(query, competence);
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param person
	 * @return sulla base dei parametri passati alla funzione ritorna la quantità di ore approvate di straordinario
	 * (sommando i codici S1 S2 e S3) 
	 */
	public static Optional<Integer> valueOvertimeApprovedByMonthAndYear(Integer year, Optional<Integer> month, Optional<Person> person, 
			List<CompetenceCode> codeList){
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if(month.isPresent())
			condition.and(competence.month.eq(month.get()));
		if(person.isPresent())
			condition.and(competence.person.eq(person.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(condition.and(competence.year.eq(year).and(competence.competenceCode.in(codeList))));
		return Optional.fromNullable(query.singleResult(competence.valueApproved.sum()));
		
	}
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @param code
	 * @return la competenza relativa ai parametri passati alla funzione
	 */
	public static Optional<Competence> getCompetence(Person person, Integer year, Integer month, CompetenceCode code){
		QCompetence competence = QCompetence.competence;
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.person.eq(person).
						and(competence.year.eq(year).and(competence.month.eq(month).and(competence.competenceCode.eq(code)))));
		
		return Optional.fromNullable(query.singleResult(competence));
		
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param code
	 * @param office
	 * @param untilThisMonth
	 * @return la lista delle competenze che hanno come validità l'anno year, che sono di persone che appartengono
	 * all'office office e che sono relative ai codici di competenza passati nella lista di stringhe code.
	 * Se il booleano untilThisMonth è true, viene presa la lista delle competenze dall'inizio dell'anno fino a quel mese compreso, se è false
	 * solo quelle del mese specificato
	 */
	public static List<Competence> getCompetences(Optional<Person> person, Integer year, Integer month, List<String> code, Office office, boolean untilThisMonth){
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if(person.isPresent())
			condition.and(competence.person.eq(person.get()));
		if(untilThisMonth)
			condition.and(competence.month.loe(month));
		else
			condition.and(competence.month.eq(month));
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(condition.and(competence.year.eq(year)
						.and(competence.competenceCode.code.in(code)
								.and(competence.person.office.eq(office)))));
		return query.list(competence);
	}
	
	
	/**
	 * 
	 * @param year
	 * @return la lista delle competenze presenti nell'anno
	 */
	public static List<Competence> getCompetenceInYear(Integer year){
		QCompetence competence = QCompetence.competence;
		JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.year.eq(year));
		query.orderBy(competence.competenceCode.code.asc());
		return query.list(competence);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return la lista di tutte le competenze di una persona nel mese month e nell'anno year che abbiano un valore approvato > 0
	 */
	public static List<Competence> getAllCompetenceForPerson(Person person, Integer year, Integer month){
		QCompetence competence = QCompetence.competence;
		JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.year.eq(year).and(competence.person.eq(person)
						.and(competence.month.eq(month).and(competence.valueApproved.gt(0)))));
		return query.list(competence);
	}
	
	

	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
	 */
	public static List<Competence> getCompetenceInMonthForUploadSituation(Person person, Integer year, Integer month){
		List<Competence> competenceList = CompetenceDao.getAllCompetenceForPerson(person, year, month);
//		List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? " +
//				"and comp.year = ? and comp.valueApproved > 0", person, month, year).fetch();
		Logger.trace("Per la persona %s %s trovate %d competenze approvate nei mesi di %d/%d", person.surname, person.name, competenceList.size(), month, year );
		return competenceList;
	}
	
	
	/**
	 * 
	 * @param type
	 * @param year
	 * @param month
	 * @param code
	 * @return la lista di competenze relative all'anno year, al mese month e al codice code di persone che hanno reperibilità 
	 * di tipo type associata
	 */
	public static List<Competence> getCompetenceInReperibility(PersonReperibilityType type, int year, int month, CompetenceCode code){
	      QCompetence competence = QCompetence.competence;
	      QPerson person = QPerson.person;
	      QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
	      JPQLQuery query = ModelQuery.queryFactory().from(competence)
	              .leftJoin(competence.person, person)
	              .leftJoin(person.reperibility.personReperibilityType, prt)
	              .where(prt.eq(type)
	            		  .and(competence.year.eq(year)
	            				  .and(competence.month.eq(month)
	            						  .and(competence.competenceCode.eq(code)))))
	            						  .orderBy(competence.person.surname.asc());

	      return query.list(competence);
	  }
	
	/**
	 * 
	 * @param year
	 * @param office
	 * @return dei quantitativi di straordinario assegnati per l'ufficio office nell'anno year
	 */
	public static List<TotalOvertime> getTotalOvertime(Integer year, Office office){
		QTotalOvertime totalOvertime = QTotalOvertime.totalOvertime;
		final JPQLQuery query = ModelQuery.queryFactory().from(totalOvertime)
				.where(totalOvertime.year.eq(year).and(totalOvertime.office.eq(office)));
		return query.list(totalOvertime);
	}
	
	/**
	 * 
	 * @param person
	 * @return il personHourForOvertime relativo alla persona person passata come parametro
	 */
	public static PersonHourForOvertime getPersonHourForOvertime(Person person){
		QPersonHourForOvertime personHourForOvertime = QPersonHourForOvertime.personHourForOvertime;
		final JPQLQuery query = ModelQuery.queryFactory().from(personHourForOvertime)
				.where(personHourForOvertime.person.eq(person));
		return query.singleResult(personHourForOvertime);
	}
	
}
