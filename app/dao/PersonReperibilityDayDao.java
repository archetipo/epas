package dao;

import java.util.List;

import helpers.ModelQuery;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.query.QPersonReperibility;
import models.query.QPersonReperibilityDay;
import models.query.QPersonReperibilityType;

/**
 * 
 * @author dario
 *
 */
public class PersonReperibilityDayDao {

	private final static QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;
	private final static QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
	private final static QPersonReperibility pr = QPersonReperibility.personReperibility;
	
	
	/******************************************************************************************************************************/
	/**Query DAO relative al PersonReperibilityDay																				 **/
	/******************************************************************************************************************************/
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return un personReperibilityDay nel caso in cui la persona person in data date fosse reperibile.
	 * Null altrimenti
	 */
	public static PersonReperibilityDay getPersonReperibilityDay(Person person, LocalDate date){
		
		JPQLQuery query = ModelQuery.queryFactory().from(prd)
				.where(prd.personReperibility.person.eq(person).and(prd.date.eq(date)));
		
		return query.singleResult(prd);
		
	}
	
	
	/**
	 * 
	 * @param type
	 * @param date
	 * @return il personReperibilityDay relativo al tipo e alla data passati come parametro
	 */
	public static PersonReperibilityDay getPersonReperibilityDayByTypeAndDate(PersonReperibilityType type, LocalDate date){
		JPQLQuery query = ModelQuery.queryFactory().from(prd).where(prd.date.eq(date).and(prd.reperibilityType.eq(type)));
		return query.singleResult(prd);
	}
	
	/**
	 * 
	 * @param begin
	 * @param to
	 * @param type
	 * @return la lista dei personReperibilityDay nel periodo compreso tra begin e to e con tipo type
	 */
	public static List<PersonReperibilityDay> getPersonReperibilityDayFromPeriodAndType(LocalDate begin, LocalDate to, PersonReperibilityType type, Optional<PersonReperibility> pr){
		BooleanBuilder condition = new BooleanBuilder();
		if(pr.isPresent())
			condition.and(prd.personReperibility.eq(pr.get()));
		JPQLQuery query = ModelQuery.queryFactory().from(prd).where(condition.and(prd.date.between(begin, to)
				.and(prd.reperibilityType.eq(type)))).orderBy(prd.date.asc());
		return query.list(prd);
	}
	
	
	/**
	 * 
	 * @param type
	 * @param day
	 * @return il numero di personReperibilityDay cancellati che hanno come parametri il tipo type e il giorno day 
	 */
	public static long deletePersonReperibilityDay(PersonReperibilityType type, LocalDate day){
		Long deleted = ModelQuery.queryFactory().delete(prd).where(prd.reperibilityType.eq(type).and(prd.date.eq(day))).execute();
		return deleted;		
	}
	
	/*********************************************************************************************************************************/
	/**Query DAO relative al personReperibilityType																					**/
	/*********************************************************************************************************************************/
	
	/**
	 * 
	 * @param id
	 * @return il personReperibilityType relativo all'id passato come parametro
	 */
	public static PersonReperibilityType getPersonReperibilityTypeById(Long id){
		JPQLQuery query = ModelQuery.queryFactory().from(prt).where(prt.id.eq(id));
		return query.singleResult(prt);
	}
	
	/*********************************************************************************************************************************/
	/**Query DAO relative al personReperibility																						**/
	/*********************************************************************************************************************************/
	
	/**
	 * 
	 * @param person
	 * @param type
	 * @return il PersonReperibility relativo alla persona person e al tipo type passati come parametro
	 */
	public static PersonReperibility getPersonReperibilityByPersonAndType(Person person, PersonReperibilityType type){
		JPQLQuery query = ModelQuery.queryFactory().from(pr).where(pr.person.eq(person).and(pr.personReperibilityType.eq(type)));
		return query.singleResult(pr);
				
	}
	
	
	/**
	 * 
	 * @param type
	 * @return la lista dei personReperibility che hanno come personReperibilityType il tipo passato come parametro
	 */
	public static List<PersonReperibility> getPersonReperibilityByType(PersonReperibilityType type){
		JPQLQuery query = ModelQuery.queryFactory().from(pr).where(pr.personReperibilityType.eq(type));
		return query.list(pr);
	}
	
	
}
