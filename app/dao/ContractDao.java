package dao;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.val;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.ContractStampProfile;
import models.Office;
import models.Person;
import models.query.QContract;
import models.query.QContractMandatoryTimeSlot;
import models.query.QContractStampProfile;
import org.joda.time.LocalDate;

/**
 * Dao per i contract.
 *
 * @author dario
 */
public class ContractDao extends DaoBase {

  private final IWrapperFactory factory;

  @Inject
  ContractDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }

  /**
   * @return il contratto corrispondente all'id passato come parametro.
   */
  public Contract getContractById(Long id) {
    QContract contract = QContract.contract;
    return getQueryFactory().selectFrom(contract)
        .where(contract.id.eq(id)).fetchOne();
  }

  /**
   * @return la lista di contratti che sono attivi nel periodo compreso tra begin e end.
   */
  private List<Contract> getActiveContractsInPeriod(Optional<List<Person>> people,
      LocalDate begin, Optional<LocalDate> end, Optional<Office> office) {

    final QContract contract = QContract.contract;

    final BooleanBuilder condition = new BooleanBuilder()
        .andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
            contract.endContract.isNull().and(contract.endDate.goe(begin)),
            contract.endDate.isNull().and(contract.endContract.goe(begin)),
            contract.endDate.goe(begin).and(contract.endContract.goe(begin)));

    if (end.isPresent()) {
      condition.and(contract.beginDate.loe(end.get()));
    }

    if (people.isPresent()) {
      condition.and(contract.person.in(people.get()));
    }

    if (office.isPresent()) {
      condition.and(contract.person.office.eq(office.get()));
    }

    return getQueryFactory().selectFrom(contract).where(condition).fetch();
  }

  public List<Contract> getActiveContractsInPeriod(LocalDate begin, Optional<LocalDate> end,
      Optional<Office> office) {
    return getActiveContractsInPeriod(Optional.absent(), begin, end, office);
  }

  public List<Contract> getActiveContractsInPeriod(Person person, LocalDate begin,
      Optional<LocalDate> end) {
    return getActiveContractsInPeriod(
        Optional.of(ImmutableList.of(person)), begin, end, Optional.absent());
  }

  /**
   * @return la lista di contratti associati alla persona person passata come parametro ordinati per
   * data inizio contratto.
   */
  public List<Contract> getPersonContractList(Person person) {
    QContract contract = QContract.contract;
    return getQueryFactory().selectFrom(contract)
        .where(contract.person.eq(person)).orderBy(contract.beginDate.asc()).fetch();
  }

  /**
   * @return il contratto attivo per quella persona alla data date.
   */
  public Contract getContract(LocalDate date, Person person) {
    // FIXME ci sono alcuni casi nei quali i valori in person.contracts (in questo metodo) non sono
    // allineati con tutti i record presenti sul db e capita che viene restituito un valore nullo
    // incongruente con i dati presenti
    // TODO da sostituire con una query?
    for (Contract c : person.contracts) {
      if (DateUtility.isDateIntoInterval(date, factory.create(c).getContractDateInterval())) {
        return c;
      }
    }
    return null;
  }

  /**
   * Se presente preleva l'eventuale fascia di presenza obbligatoria di una persona 
   * in una data indicata.
   * 
   * @param date la data in cui cercare la fascia obbligatoria
   * @param person la persona di cui cercare la fascia obbligatoria
   * @return la fascia oraria obbligatoria se presente, Optional.absent() altrimenti.
   */
  public Optional<ContractMandatoryTimeSlot> getContractMandatoryTimeSlot(LocalDate date, Long personId) {
    QContract contract = QContract.contract;
    QContractMandatoryTimeSlot contractMandatoryTimeSlot = QContractMandatoryTimeSlot.contractMandatoryTimeSlot;
    Person person = Person.findById(personId);
    val cmts = getQueryFactory().selectFrom(contractMandatoryTimeSlot).join(contractMandatoryTimeSlot.contract, contract)
        .where(
            contract.person.eq(person), contract.beginDate.before(date).or(contract.beginDate.eq(date)),
            contract.endContract.isNull().or(contract.endContract.after(date).or(contract.endContract.eq(date))),
            contract.endDate.isNull().or(contract.endDate.after(date).or(contract.endDate.eq(date))),
            contractMandatoryTimeSlot.beginDate.before(date).or(contractMandatoryTimeSlot.beginDate.eq(date)),
            contractMandatoryTimeSlot.endDate.isNull()
              .or(contractMandatoryTimeSlot.endDate.after(date))
              .or(contractMandatoryTimeSlot.endDate.eq(date)))
        .fetchOne();
    return Optional.fromNullable(cmts);
  }
  
  /**
   * @return la lista dei contractStampProfile relativi alla persona person o al contratto contract
   * passati come parametro e ordinati per data inizio del contractStampProfile La funzione permette
   * di scegliere quale dei due parametri indicare per effettuare la ricerca. Sono mutuamente
   * esclusivi.
   */
  public List<ContractStampProfile> getPersonContractStampProfile(Optional<Person> person,
      Optional<Contract> contract) {
    QContractStampProfile csp = QContractStampProfile.contractStampProfile;
    final BooleanBuilder condition = new BooleanBuilder();
    if (person.isPresent()) {
      condition.and(csp.contract.person.eq(person.get()));
    }
    if (contract.isPresent()) {
      condition.and(csp.contract.eq(contract.get()));
    }
    return getQueryFactory().selectFrom(csp)
        .where(condition).orderBy(csp.beginDate.asc()).fetch();

  }

  /**
   * @return il contractStampProfile relativo all'id passato come parametro.
   */
  public ContractStampProfile getContractStampProfileById(Long id) {
    QContractStampProfile csp = QContractStampProfile.contractStampProfile;
    return getQueryFactory().selectFrom(csp)
        .where(csp.id.eq(id)).fetchOne();
  }

}
