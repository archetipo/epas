package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateInterval;

import java.util.List;
import java.util.Set;

import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.query.QAbsence;
import models.query.QCompetenceCode;
import models.query.QContract;
import models.query.QPerson;
import models.query.QPersonDay;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonShift;
import models.query.QUser;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

/**
 * DAO per le person.
 * 
 * @author marco
 *
 */
public final class PersonDao {

	private final static QPerson person = QPerson.person;
	private final static QContract contract = QContract.contract;
	private PersonDao() {}
	
	/**
	 * @param name
	 * @param offices obbligatorio
	 * @param onlyTechnician
	 * @return la lista delle person corrispondenti
	 */
	public static SimpleResults<Person> list(Optional<String> name, Set<Office> offices, 
			boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {
		
		Preconditions.checkState(!offices.isEmpty());
		
		// TODO: completare con l'intervallo
		//final LocalDate start = new LocalDate();
		//final LocalDate end = start;
				
					
		 final JPQLQuery query = ModelQuery.queryFactory().from(person)
					.leftJoin(person.contracts, contract).fetch()
					.leftJoin(contract.contractWorkingTimeType).fetch()
					.leftJoin(contract.contractStampProfile).fetch()
					.leftJoin(contract.vacationPeriods).fetch()
					.leftJoin(person.personHourForOvertime).fetch()
					.leftJoin(person.reperibility).fetch()
					.leftJoin(person.personShift).fetch()
					.orderBy(person.surname.asc(), person.name.asc())
					.distinct();
		
		
		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(person.office.in(offices));
		
		if (onlyTechnician) {
			// i livelli sopra al 3 sono dei tecnici:
			condition.and(person.qualification.qualification.gt(3));
		}
		
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(person.name.startsWithIgnoreCase(name.get()),
					person.surname.startsWithIgnoreCase(name.get()));
		}
		
		if(onlyOnCertificate)
			condition.and(contract.onCertificate.isTrue());
		
		condition.andAnyOf(
				
				//contratto terminato
				contract.endContract.isNotNull().and(contract.beginContract.loe(end)).and(contract.endContract.goe(start)),

				//contratto non terminato
				contract.endContract.isNull().and(

						//contratto tempo indeterminato
						contract.expireContract.isNull().and(contract.beginContract.loe(end))
						
						.or(
						
						//contratto tempo determinato
						contract.expireContract.isNotNull().and(contract.beginContract.loe(end)).and(contract.expireContract.goe(start))
						
						)
					)
				
				);
		
		/*
		condition.and(qc.beginContract.before(end));
		
		condition.andAnyOf(
				qc.endContract.isNull().and(qc.expireContract.isNull()),											//contratto indeterminato non terminato
				
				qc.endContract.isNull().and( qc.expireContract.isNotNull().and(qc.expireContract.goe(start)) ), 	//contratto determinato non terminato
				
				qc.endContract.isNotNull().and(qc.endContract.goe(start)) );										//contratto terminato 
				
				*/
				

		query.where(condition);
		
		return ModelQuery.simpleResults(query, person);
	}
	
	
	/**
	 * @param name
	 * @param offices obbligatorio
	 * @param onlyTechnician
	 * @return la lista delle person corrispondenti
	 */
	public static SimpleResults<Person> listForCompetence(CompetenceCode compCode, Optional<String> name, Set<Office> offices, 
			boolean onlyTechnician, LocalDate start, LocalDate end) {
		
		Preconditions.checkState(!offices.isEmpty());
		
		final QCompetenceCode qcc = QCompetenceCode.competenceCode;
		// TODO: completare con l'intervallo
		//final LocalDate start = new LocalDate();
		//final LocalDate end = start;
				
		final JPQLQuery query = ModelQuery.queryFactory().from(person)
				.leftJoin(person.contracts, contract)
				.leftJoin(person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
				//.leftJoin(qp.location, QLocation.location)
				.leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetch()
				.leftJoin(person.personShift, QPersonShift.personShift).fetch()
				.leftJoin(person.user, QUser.user)
				.leftJoin(person.competenceCode, qcc)
				.orderBy(person.surname.asc(), person.name.asc())
				.distinct();
		
		
		
		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(person.office.in(offices));
		
		if (onlyTechnician) {
			// i livelli sopra al 3 sono dei tecnici:
			condition.and(person.qualification.qualification.gt(3));
		}
		
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(person.name.startsWithIgnoreCase(name.get()),
					person.surname.startsWithIgnoreCase(name.get()));
		}
		condition.and(person.competenceCode.contains(compCode));
		condition.and(contract.onCertificate.isTrue());
		condition.and(contract.beginContract.before(end));
		condition.andAnyOf(contract.endContract.isNull().and(contract.expireContract.isNull()),
				contract.expireContract.isNotNull().and(contract.expireContract.goe(start)),
				contract.endContract.isNotNull().and(contract.endContract.goe(start)));
		
		query.where(condition);
		
		return ModelQuery.simpleResults(query, person);
	}
	
	/**
	 * L'ultimo contratto inserito in ordine di data inizio. 
	 * (Tendenzialmente quello attuale)
	 * @param person
	 * @return
	 */
	public Contract getLastContract(Person person) {
				
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(contract)
				.where(contract.person.eq(person))
				.orderBy(contract.beginContract.desc());
		
		List<Contract> contracts = query.list(contract);
		if(contracts.size() == 0)
			return null;
		return contracts.get(0);
		
	}
	
	/**
	 * Il contratto precedente in ordine temporale rispetto a quello passato
	 * come argomento.
	 * @param c
	 * @return
	 */
	public static Contract getPreviousPersonContract(Contract c) {
			
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(contract)
				.where(contract.person.eq(c.person))
				.orderBy(contract.beginContract.desc());
		
		List<Contract> contracts = query.list(contract);
		
		final int indexOf = contracts.indexOf(c);
		if(indexOf + 1 < contracts.size())
			return contracts.get(indexOf + 1);
		else 
			return null;
	}
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return la lista di contratti che soddisfa le seguenti condizioni:
	 */
	public static List<Contract> getContractList(Person person,LocalDate fromDate,LocalDate toDate){

		BooleanBuilder conditions = new BooleanBuilder(contract.person.eq(person).and(contract.beginContract.loe(toDate)));
		
		conditions.andAnyOf(
				contract.endContract.isNull().and(contract.expireContract.isNull()),
				contract.endContract.isNull().and(contract.expireContract.goe(fromDate)),
				contract.endContract.isNotNull().and(contract.endContract.goe(fromDate))
				);
		
		return ModelQuery.queryFactory().from(contract).where(conditions).list(contract);
	}

	/**
	 * Ritorna la lista dei person day della persona nella finestra temporale specificata
	 * ordinati per data con ordinimento crescente.
	 * @param person
	 * @param interval
	 * @param onlyWithMealTicket 
	 * @return
	 */
	public static List<PersonDay> getPersonDayIntoInterval(
			Person person, DateInterval interval, boolean onlyWithMealTicket) {
		
		final QPersonDay qpd = QPersonDay.personDay;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(qpd)
				.orderBy(qpd.date.asc());

		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(qpd.person.eq(person)
				.and(qpd.date.goe(interval.getBegin()))
				.and(qpd.date.loe(interval.getEnd())));
		
		if(onlyWithMealTicket) {
			condition.and(qpd.isTicketAvailable.eq(true));
		}
		query.where(condition);
		
		return query.list(qpd);
	}

	/**
	 * 
	 * @param personId
	 * @return la persona corrispondente all'id passato come parametro
	 */
	public static Person getPersonById(Long personId) {

		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.id.eq(personId));
		
		return query.singleResult(person);
		
	
	}
	
	/**
	 * 
	 * @param number
	 * @return la persona corrispondente alla matricola passata come parametro
	 */
	public static Person getPersonByNumber(Integer number){

		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.number.eq(number));
		
		return query.singleResult(person);
		
	}
	
	/**
	 * 
	 * @return la lista di persone che hanno una matricola associata
	 */
	public static List<Person> getPersonsByNumber(){

		final JPQLQuery query = ModelQuery.queryFactory().from(person)
				.where(person.number.isNotNull().and(person.number.ne(0)));
		query.orderBy(person.number.asc());
		return query.list(person);
	}
	
	/**
	 * 
	 * @param email
	 * @return la persona che ha associata la mail email
	 */
	public static Person getPersonByEmail(String email){

		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.email.eq(email));
		
		return query.singleResult(person);
	}
	
	/**
	 * 
	 * @param oldId
	 * @return la persona associata al vecchio id (se presente in anagrafica) passato come parametro
	 */
	public static Person getPersonByOldID(Long oldId){

		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.oldId.eq(oldId));
		
		return query.singleResult(person);
	}
	
	/**
	 * 
	 * @param badgeNumber
	 * @return la persona associata al badgeNumber passato come parametro
	 */
	public static Person getPersonByBadgeNumber(String badgeNumber){

		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.badgeNumber.eq(badgeNumber));
		
		return query.singleResult(person);
	}
	
	
	
	
}
