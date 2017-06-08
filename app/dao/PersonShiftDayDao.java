package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCategories;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import models.query.QPersonShift;
import models.query.QPersonShiftDay;
import models.query.QShiftCategories;
import org.joda.time.LocalDate;

/**
 * Dao per i PersonShift.
 *
 * @author dario
 */
public class PersonShiftDayDao extends DaoBase {

  @Inject
  PersonShiftDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return il personShiftDay relativo alla persona person nel caso in cui in data date fosse in
   * turno Null altrimenti.
   */
  public Optional<PersonShiftDay> getPersonShiftDay(Person person, LocalDate date) {
    final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;
    final QPersonShift personShift = QPersonShift.personShift;

    JPQLQuery query = getQueryFactory().from(personShiftDay)
        .join(personShiftDay.personShift, personShift)
        .where(personShift.person.eq(person).and(personShiftDay.date.eq(date)));
    PersonShiftDay psd = query.singleResult(personShiftDay);

    return Optional.fromNullable(psd);
  }

  /**
   * @return la lista dei personShiftDay presenti nel periodo compreso tra 'from' e 'to' aventi lo
   * shiftType 'type'. Se specificato filtra sulla persona richiesta.
   */
  public List<PersonShiftDay> byTypeInPeriod(
      LocalDate from, LocalDate to, ShiftType type, Optional<Person> person) {
    final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;

    final BooleanBuilder condition = new BooleanBuilder()
        .and(personShiftDay.date.goe(from))
        .and(personShiftDay.date.loe(to))
        .and(personShiftDay.shiftType.eq(type));

    if (person.isPresent()) {
      condition.and(personShiftDay.personShift.person.eq(person.get()));
    }
    
    return getQueryFactory().from(personShiftDay)
        .where(condition).orderBy(personShiftDay.date.asc()).list(personShiftDay);
  }

  /**
   * @return il personShiftDay relativo al tipo 'shiftType' nel giorno 'date' con lo slot
   * 'shiftSlot'.
   */
  public PersonShiftDay getPersonShiftDayByTypeDateAndSlot(
      ShiftType shiftType, LocalDate date, ShiftSlot shiftSlot) {
    final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;

    JPQLQuery query = getQueryFactory().from(personShiftDay).where(personShiftDay.date.eq(date)
        .and(personShiftDay.shiftType.eq(shiftType)
            .and(personShiftDay.shiftSlot.eq(shiftSlot))));
    return query.singleResult(personShiftDay);
  }


  /**
   * @return il personShift associato alla persona passata come parametro.
   */
  public PersonShift getPersonShiftByPerson(Person person) {
    final QPersonShift personShift = QPersonShift.personShift;
    JPQLQuery query = getQueryFactory().from(personShift).where(personShift.person.eq(person));
    return query.singleResult(personShift);
  }

  /**
   * @return la lista di tutti i PersonReperibilityType presenti sul db.
   */
  public List<ShiftCategories> getAllShiftType() {
    QShiftCategories shift = QShiftCategories.shiftCategories;
    JPQLQuery query = getQueryFactory().from(shift).orderBy(shift.description.asc());
    return query.list(shift);
  }

  public Optional<PersonShiftDay> byPersonAndDate(Person person, LocalDate date) {
    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;

    return Optional.fromNullable(getQueryFactory().from(shiftDay)
        .where(shiftDay.personShift.person.eq(person).and(shiftDay.date.eq(date)))
        .singleResult(shiftDay));
  }

  public long countByPersonAndDate(Person person, LocalDate date) {

    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;
    return getQueryFactory().from(shiftDay)
        .where(shiftDay.personShift.person.eq(person).and(shiftDay.date.eq(date))).count();
  }
}
