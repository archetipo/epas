package dao;

import java.util.List;

import javax.persistence.EntityManager;

import models.Person;
import models.PersonDayInTrouble;
import models.query.QPersonDayInTrouble;

import org.joda.time.LocalDate;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

public class PersonDayInTroubleDao extends DaoBase {

	@Inject
	PersonDayInTroubleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param fixed
	 * @return la lista dei personDayInTrouble relativi alla persona person 
	 * nel periodo begin-end. E' possibile specificare se si vuole
	 * ottenere quelli fixati (fixed = true) o no (fixed = false)
	 */
	public List<PersonDayInTrouble> getPersonDayInTroubleInPeriod(Person person, LocalDate begin, LocalDate end, boolean fixed){
		
		QPersonDayInTrouble pdit = QPersonDayInTrouble.personDayInTrouble;
		
		final JPQLQuery query = getQueryFactory()
				.from(pdit)
				.where(pdit.personDay.person.eq(person)
				.and(pdit.personDay.date.between(begin, end))
				.and(pdit.fixed.eq(fixed)));
		
		return query.list(pdit);
	}
}
