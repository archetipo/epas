package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.types.Projections;
import com.mysema.query.types.QBean;

import dao.filter.QFilters;

import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;

import it.cnr.iit.epas.DateInterval;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import manager.configurations.EpasParam;

import models.BadgeReader;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.flows.query.QGroup;
import models.query.QBadge;
import models.query.QConfiguration;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import models.query.QOffice;
import models.query.QPerson;
import models.query.QPersonCompetenceCodes;
import models.query.QPersonDay;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonShift;
import models.query.QPersonShiftShiftType;
import models.query.QUser;
import models.query.QVacationPeriod;
import models.query.QWorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * DAO per le person.
 *
 * @author marco
 */
public final class PersonDao extends DaoBase {


  @Inject
  public OfficeDao officeDao;
  @Inject
  public PersonDayDao personDayDao;

  @Inject
  PersonDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @param offices   la lista degli uffici
   * @param yearMonth l'oggetto anno/mese
   * @return la lista delle persone di un certo ufficio attive in quell'anno/mese.
   */
  public List<Person> getActivePersonInMonth(Set<Office> offices, YearMonth yearMonth) {
    final QPerson person = QPerson.person;
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthOfYear();

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());

    JPQLQuery query = personQuery(Optional.<String>absent(), offices, false, beginMonth, endMonth,
        true, Optional.<CompetenceCode>absent(), Optional.<Person>absent(), false);

    return query.list(person);
  }


  /**
   * La lista di persone una volta applicati i filtri dei parametri. (Dovrà sostituire list
   * deprecata). TODO: Perseo significa che utilizza i metodi puliti di paginazione implementati da
   * Marco (PerseoSimpleResult e ModelQuery) che dovranno sostituire i deprecati SimpleResult e
   * ModelQuery di epas.
   */
  public SimpleResults<Person> listPerseo(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    return ModelQuery.wrap(
        // JPQLQuery
        personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
            Optional.fromNullable(end), onlyOnCertificate, Optional.<CompetenceCode>absent(),
            Optional.<Person>absent(), false),
        // Expression
        person);
  }

  /**
   * Tutte le persone di epas (possibile filtrare sulla sede).
   */
  public SimpleResults<Person> list(Optional<Office> office) {
    final QPerson person = QPerson.person;

    Set<Office> offices = Sets.newHashSet();
    if (office.isPresent()) {
      offices.add(office.get());
    }
    return ModelQuery.wrap(
        // JPQLQuery
        personQuery(Optional.<String>absent(), offices, false, Optional.<LocalDate>absent(),
            Optional.<LocalDate>absent(), false, Optional.<CompetenceCode>absent(),
            Optional.<Person>absent(), false),
        // Expression
        person);

  }


  /**
   * La lista di persone una volta applicati i filtri dei parametri.
   */
  public SimpleResults<Person> list(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    return ModelQuery.wrap(
        // JPQLQuery
        personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
            Optional.fromNullable(end), onlyOnCertificate, Optional.<CompetenceCode>absent(),
            Optional.<Person>absent(), false),
        // Expression
        person);
  }

  /**
   * @param office Ufficio
   * @return Restituisce la lista delle persone appartenenti all'ufficio specificato.
   */
  public List<Person> byOffice(Office office) {
    final QPerson person = QPerson.person;

    return personQuery(Optional.absent(), ImmutableSet.of(office), false, Optional.absent(),
        Optional.absent(), false, Optional.absent(), Optional.absent(), false).list(person);
  }


  /**
   * Permette la fetch automatica di tutte le informazioni delle persone filtrate. TODO: e' usata
   * solo in Persons.list ma se serve in altri metodi rendere parametrica la funzione
   * PersonDao.list.
   *
   * @param name              l'eventuale nome da filtrare
   * @param offices           la lista degli uffici su cui cercare
   * @param onlyTechnician    true se cerco solo i tecnici, false altrimenti
   * @param start             da quando iniziare la ricerca
   * @param end               quando terminare la ricerca
   * @param onlyOnCertificate true se voglio solo gli strutturati, false altrimenti
   * @return la lista delle persone trovate con queste retrizioni
   */
  public SimpleResults<Person> listFetched(Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {

    final QPerson person = QPerson.person;

    JPQLQuery query = personQuery(name, offices, onlyTechnician, Optional.fromNullable(start),
        Optional.fromNullable(end), onlyOnCertificate, Optional.<CompetenceCode>absent(),
        Optional.<Person>absent(), false);

    SimpleResults<Person> result = ModelQuery.wrap(
        // JPQLQuery
        query,
        // Expression
        person);

    fetchContracts(Sets.newHashSet(result.list()), Optional.fromNullable(start),
        Optional.fromNullable(end));

    return result;

  }

  /**
   * La lista delle persone abilitate alla competenza competenceCode. E che superano i filtri dei
   * parametri.
   *
   * @param competenceCode codice competenza
   * @param name           name
   * @param offices        offices
   * @param onlyTechnician solo tecnologhi
   * @param start          attivi da
   * @param end            attivi a
   * @return model query delle persone selezionte.
   */
  public SimpleResults<Person> listForCompetence(CompetenceCode competenceCode,
      Optional<String> name, Set<Office> offices, boolean onlyTechnician,
      LocalDate start, LocalDate end, Optional<Person> personInCharge) {

    Preconditions.checkState(!offices.isEmpty());
    Preconditions.checkNotNull(competenceCode);

    final QPerson person = QPerson.person;

    return ModelQuery.wrap(personQuery(name, offices, onlyTechnician,
        Optional.fromNullable(start), Optional.fromNullable(end), true,
        Optional.fromNullable(competenceCode), personInCharge, false), person);

  }

  /**
   *
   * @param offices Uffici dei quali verificare le persone
   * @param yearMonth Il mese interessato
   * @param code  Il codice di competenza da considerare
   * @return La lista delle persone con il codice di competenza abilitato nel mese specificato.
   */
  public List<Person> listForCompetence(
      Set<Office> offices, YearMonth yearMonth, CompetenceCode code) {
    final QPerson person = QPerson.person;
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthOfYear();

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());
    JPQLQuery query = personQuery(Optional.<String>absent(), offices, false, beginMonth, endMonth,
        true, Optional.fromNullable(code), Optional.<Person>absent(), false);
    return query.list(person);
  }

  /**
   * Una mappa contenente le persone con perseoId valorizzato. La chiave è il perseoId.
   *
   * @param office sede opzionale, se absente tutte le persone.
   * @return mappa
   */
  public Map<Long, Person> mapSynchronized(Optional<Office> office) {

    final QPerson person = QPerson.person;

    //Il metodo personQuery considera la lista solo se non è ne null ne vuota.
    Set<Office> offices = Sets.newHashSet();
    if (office.isPresent()) {
      offices.add(office.get());
    }

    return personQuery(Optional.<String>absent(), offices, false, Optional.<LocalDate>absent(),
        Optional.<LocalDate>absent(), false, Optional.<CompetenceCode>absent(),
        Optional.<Person>absent(), true).map(person.perseoId, person);

  }

  /**
   * L'ultimo contratto inserito in ordine di data inizio. (Tendenzialmente quello attuale)
   *
   * @param person la persona di cui si richiede il contratto
   * @return l'ultimo contratto in ordine temporale.
   */
  public Optional<Contract> getLastContract(Person person) {

    final QContract contract = QContract.contract;

    final JPQLQuery query = getQueryFactory().from(contract).where(contract.person.eq(person))
        .orderBy(contract.beginDate.desc());

    List<Contract> contracts = query.list(contract);
    if (contracts.size() == 0) {
      return Optional.<Contract>absent();
    }
    return Optional.fromNullable(contracts.get(0));

  }

  /**
   * Il contratto precedente in ordine temporale rispetto a quello passato come argomento.
   */
  public Contract getPreviousPersonContract(Contract contract) {

    final QContract qcontract = QContract.contract;

    final JPQLQuery query =
        getQueryFactory().from(qcontract).where(qcontract.person.eq(contract.person))
            .orderBy(qcontract.beginDate.desc());

    List<Contract> contracts = query.list(qcontract);

    final int indexOf = contracts.indexOf(contract);
    if (indexOf + 1 < contracts.size()) {
      return contracts.get(indexOf + 1);
    } else {
      return null;
    }
  }

  /**
   * @param person   la persona di cui si vogliono i contratti
   * @param fromDate la data di inizio da cui cercare
   * @param toDate   la data di fine in cui cercare
   * @return la lista di contratti che soddisfa le seguenti condizioni.
   */
  public List<Contract> getContractList(Person person, LocalDate fromDate, LocalDate toDate) {

    final QContract contract = QContract.contract;

    BooleanBuilder conditions =
        new BooleanBuilder(contract.person.eq(person).and(contract.beginDate.loe(toDate)));

    conditions.andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
        contract.endContract.isNull().and(contract.endDate.goe(fromDate)),
        contract.endContract.isNotNull().and(contract.endContract.goe(fromDate)));

    return getQueryFactory().from(contract).where(conditions).orderBy(contract.beginDate.asc())
        .list(contract);
  }

  /**
   * Ritorna la lista dei person day della persona nella finestra temporale specificata ordinati per
   * data con ordinimento crescente.
   *
   * @param person             la persona di cui si chiedono i personday
   * @param interval           l'intervallo dei personday
   * @param onlyWithMealTicket se con i mealticket associati
   * @return la lista dei personday che soddisfano i parametri
   */
  public List<PersonDay> getPersonDayIntoInterval(Person person, DateInterval interval,
      boolean onlyWithMealTicket) {

    final QPersonDay qpd = QPersonDay.personDay;

    final JPQLQuery query = getQueryFactory().from(qpd).orderBy(qpd.date.asc());

    final BooleanBuilder condition = new BooleanBuilder();
    condition.and(qpd.person.eq(person).and(qpd.date.goe(interval.getBegin()))
        .and(qpd.date.loe(interval.getEnd())));

    if (onlyWithMealTicket) {
      condition.and(qpd.isTicketAvailable.eq(true));
    }
    query.where(condition);

    return query.list(qpd);
  }

  /**
   * @param personId l'id della persona.
   * @return la persona corrispondente all'id passato come parametro.
   */
  public Person getPersonById(Long personId) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QContractStampProfile csp = QContractStampProfile.contractStampProfile;

    final JPQLQuery query = getQueryFactory().from(person).leftJoin(person.contracts, contract)
        .fetchAll().leftJoin(contract.contractStampProfile, csp).fetchAll()
        .where(person.id.eq(personId)).distinct();

    return query.singleResult(person);
  }

  /**
   * @param email della persona
   * @return la persona corrispondente alla email.
   */
  @Deprecated // email non è un campo univoco... decidere
  public Person getPersonByEmail(String email) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.email.eq(email));

    return query.singleResult(person);
  }

  /**
   * @param number la matricola passata come parametro.
   * @return la persona corrispondente alla matricola passata come parametro.
   */
  @Deprecated
  public Person getPersonByNumber(Integer number, Optional<Set<Office>> officeList) {

    final BooleanBuilder condition = new BooleanBuilder();
    final QPerson person = QPerson.person;
    if (officeList.isPresent()) {
      condition.and(person.office.in(officeList.get()));
    }

    condition.and(person.number.eq(number));

    final JPQLQuery query = getQueryFactory().from(person).where(condition);

    return query.singleResult(person);
  }

  public Person getPersonByNumber(Integer number) {
    return getPersonByNumber(number, Optional.<Set<Office>>absent());
  }

  /**
   * La lista di persone con matricola valida associata. Se office present le sole persone di quella
   * sede.
   *
   * @return persone con matricola valida
   */
  public List<Person> getPersonsWithNumber(Optional<Office> office) {

    final QPerson person = QPerson.person;

    BooleanBuilder condition =
        new BooleanBuilder(person.number.isNotNull().and(person.number.ne(0)));

    if (office.isPresent()) {
      condition.and(person.office.eq(office.get()));
    }

    return getQueryFactory().from(person)
        .where(condition).orderBy(person.number.asc()).list(person);
  }

  /**
   * @param email la mail della persona.
   * @return la persona che ha associata la mail email.
   */
  public Optional<Person> byEmail(String email) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.email.eq(email));

    return Optional.fromNullable(query.singleResult(person));
  }


  /**
   * @param eppn il parametro eppn per autenticazione via shibboleth.
   * @return la persona se esiste associata al parametro eppn.
   */
  public Optional<Person> byEppn(String eppn) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.eppn.eq(eppn));

    return Optional.fromNullable(query.singleResult(person));
  }

  /**
   * @param perseoId l'id della persona sull'applicazione perseo.
   * @return la persona identificata dall'id con cui è salvata sul db di perseo.
   */
  public Person getPersonByPerseoId(Long perseoId) {

    final QPerson person = QPerson.person;

    final JPQLQuery query = getQueryFactory().from(person).where(person.perseoId.eq(perseoId));

    return query.singleResult(person);
  }

  /**
   * Il proprietario del badge.
   *
   * @param badgeNumber codice del badge
   * @param badgeReader badge reader
   * @return il proprietario del badge
   */
  public Person getPersonByBadgeNumber(String badgeNumber, BadgeReader badgeReader) {

    final QPerson person = QPerson.person;
    final QBadge badge = QBadge.badge;

    //Rimuove tutti gli eventuali 0 iniziali alla stringa
    // http://stackoverflow.com/questions/2800739/how-to-remove-leading-zeros-from-alphanumeric-text
    final String cleanedBadgeNumber = badgeNumber.replaceFirst("^0+(?!$)", "");

    return getQueryFactory().from(person)
        .leftJoin(person.badges, badge)
        .where(badge.badgeReader.eq(badgeReader)
            .andAnyOf(badge.code.eq(badgeNumber), badge.code.eq(cleanedBadgeNumber)))
        .singleResult(person);
  }

  /**
   * @param type il tipo della reperibilità.
   * @return la lista di persone in reperibilità con tipo type.
   */
  public List<Person> getPersonForReperibility(Long type) {

    final QPerson person = QPerson.person;
    final QPersonReperibility rep = QPersonReperibility.personReperibility;

    final JPQLQuery query = getQueryFactory().from(person)
        .leftJoin(person.reperibility, rep)
        .where(rep.personReperibilityType.id.eq(type)
            .and(rep.startDate.isNull()
                .or(rep.startDate.loe(LocalDate.now())
                    .and(rep.endDate.isNull()
                        .or(rep.endDate.goe(LocalDate.now()))))));
    return query.list(person);

  }

  /**
   * @param type il tipo di turno
   * @return la lista di persone che hanno come tipo turno quello passato come parametro.
   */
  public List<Person> getPersonForShift(String type, LocalDate date) {

    final QPerson person = QPerson.person;
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    final QPersonShift ps = QPersonShift.personShift;

    final JPQLQuery query = getQueryFactory().from(person).leftJoin(person.personShifts, ps)
        .leftJoin(ps.personShiftShiftTypes, psst)
        .where(psst.shiftType.type.eq(type)
            .and(ps.beginDate.loe(date).andAnyOf(ps.endDate.isNull(), ps.endDate.goe(date)))
            .and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now()))
                .and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())))));
    return query.list(person);
  }


  /**
   * Le persone attive della sede con il campo matricola popolato.
   *
   * @return persone
   */
  public List<Person> activeWithNumber(Office office) {

    final QPerson person = QPerson.person;

    JPQLQuery query = personQuery(Optional.<String>absent(), Sets.newHashSet(office), false,
        Optional.fromNullable(LocalDate.now()), Optional.fromNullable(LocalDate.now()),
        true, Optional.<CompetenceCode>absent(), Optional.<Person>absent(), false)
        .where(person.number.isNotNull());

    return query.list(person);

  }

  /**
   * La query per la ricerca delle persone. Versione con JPQLQuery injettata per selezionare le
   * fetch da utilizzare con la proiezione desiderata.
   */
  private JPQLQuery personQuery(JPQLQuery injectedQuery, Optional<String> name, Set<Office> offices,
      boolean onlyTechnician, Optional<LocalDate> start, Optional<LocalDate> end,
      boolean onlyOnCertificate, Optional<CompetenceCode> compCode,
      /*Optional<Person> personInCharge,*/ boolean onlySynchronized) {

    final BooleanBuilder condition = new BooleanBuilder();

    filterOffices(condition, offices);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    if (start.isPresent()) {
      filterCompetenceCodeEnabled(condition, compCode, start.get());
    }    
    //filterPersonInCharge(condition, personInCharge);
    filterOnlySynchronized(condition, onlySynchronized);

    return injectedQuery.where(condition);

  }

  /**
   * La query per la ricerca delle persone. Versione da utilizzare per proiezione esatta Person.
   *
   * @param name              l'eventuale nome
   * @param offices           la lista degli uffici
   * @param onlyTechnician    true se si chiedono solo i tecnici, false altrimenti
   * @param start             da quando iniziare la ricerca
   * @param end               quando terminare la ricerca
   * @param onlyOnCertificate true se si chiedono solo gli strutturati, false altrimenti
   * @param compCode          il codice di competenza
   * @param personInCharge    il responsabile della persona
   * @param onlySynchronized  le persone con perseoId valorizzato
   * @return la lista delle persone corrispondente ai criteri di ricerca
   */
  private JPQLQuery personQuery(Optional<String> name, Set<Office> offices, boolean onlyTechnician,
      Optional<LocalDate> start, Optional<LocalDate> end, boolean onlyOnCertificate,
      Optional<CompetenceCode> compCode, Optional<Person> personInCharge,
      boolean onlySynchronized) {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;

    final JPQLQuery query = getQueryFactory().from(person)

        // join one to many or many to many (only one bag fetchable!!!)
        .leftJoin(person.contracts, contract).fetch()
        .leftJoin(person.personCompetenceCodes, QPersonCompetenceCodes.personCompetenceCodes)
        .leftJoin(person.user, QUser.user)
        .leftJoin(person.groups, QGroup.group)
        // join one to one
        .leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetch()
        .leftJoin(
            person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
        .leftJoin(person.qualification).fetch()
        // order by
        .orderBy(person.surname.asc(), person.name.asc())
        .distinct();

    final BooleanBuilder condition = new BooleanBuilder();

    filterOffices(condition, offices);
    filterOnlyTechnician(condition, onlyTechnician);
    condition.and(new QFilters().filterNameFromPerson(QPerson.person, name));
    filterOnlyOnCertificate(condition, onlyOnCertificate);
    filterContract(condition, start, end);
    if (start.isPresent()) {
      filterCompetenceCodeEnabled(condition, compCode, start.get());
    }    
    filterPersonInCharge(condition, personInCharge);
    filterOnlySynchronized(condition, onlySynchronized);

    return query.where(condition);
  }


  /**
   * Filtro sugli uffici.
   */
  private void filterOffices(BooleanBuilder condition, Set<Office> offices) {

    final QPerson person = QPerson.person;

    if (offices != null && !offices.isEmpty()) {
      condition.and(person.office.in(offices));
    }
  }

  /**
   * Filtro sulle date contrattuali.
   *
   * @param condition il booleanbuilder contenente eventuali altre condizioni
   * @param start     absent() no limit
   * @param end       absent() no limit
   */
  private void filterContract(BooleanBuilder condition, Optional<LocalDate> start,
      Optional<LocalDate> end) {

    final QContract contract = QContract.contract;

    if (end.isPresent()) {

      condition.and(contract.beginDate.loe(end.get()));
    }

    if (start.isPresent()) {
      // entrambe le date nulle
      condition.andAnyOf(contract.endContract.isNull().and(contract.endDate.isNull()),
          // una nulla e l'altra successiva
          contract.endContract.isNull().and(contract.endDate.goe(start.get())),
          // viceversa rispetto alla precedente
          contract.endDate.isNull().and(contract.endContract.goe(start.get())),
          //entrambe valorizzate ed entrambe successive
          contract.endDate.goe(start.get()).and(contract.endContract.goe(start.get()))
      );
    }
  }


  /**
   * Filtra solo i livelli IV-VIII.
   * @param condition la condizione
   * @param value true se vogliamo solo i tecnici/amministrativi, false altrimenti
   */
  private void filterOnlyTechnician(BooleanBuilder condition, boolean value) {
    if (value == true) {
      final QPerson person = QPerson.person;
      condition.and(person.qualification.qualification.gt(3));
    }
  }

  /**
   * Filtra solo le persone che devono andare su attestati.
   * @param condition la condizione
   * @param value true se vogliamo quelli che vanno su attestati, false altrimenti
   */
  private void filterOnlyOnCertificate(BooleanBuilder condition, boolean value) {
    if (value) {
      final QContract contract = QContract.contract;
      condition.and(contract.onCertificate.isTrue());
    }
  }

  /**
   * Filtra le persone che appartengono al gruppo di lavoro del personInCharge.
   * @param condition la condizione
   * @param personInCharge il responsabile se presente
   */
  private void filterPersonInCharge(BooleanBuilder condition, Optional<Person> personInCharge) {
    if (personInCharge.isPresent()) {      
      final QGroup group = QGroup.group;
      condition.and(group.manager.eq(personInCharge.get()));
    }
  }

  /**
   * Filtra le persone sincronizzate con perseo.
   * @param condition la condizione
   * @param value true se vogliamo solo i sincronizzati con perseo, false altrimenti 
   */
  private void filterOnlySynchronized(BooleanBuilder condition, boolean value) {
    if (value == true) {
      final QPerson person = QPerson.person;
      condition.and(person.perseoId.isNotNull());
    }
  }


  /**
   * Filtro su competenza abilitata.
   */
  private void filterCompetenceCodeEnabled(BooleanBuilder condition,
      Optional<CompetenceCode> compCode, LocalDate date) {

    if (compCode.isPresent()) {
      final QPersonCompetenceCodes pcc = QPersonCompetenceCodes.personCompetenceCodes;
      condition.and(pcc.competenceCode.eq(compCode.get())).and(pcc.beginDate.loe(date)
          .andAnyOf(pcc.endDate.goe(date), pcc.endDate.isNull()));
    }
  }


  /**
   * Importa tutte le informazioni della persona necessarie alla business logic ottimizzando il
   * numero di accessi al db.
   */
  public Person fetchPersonForComputation(Long id, Optional<LocalDate> begin,
      Optional<LocalDate> end) {


    QPerson qperson = QPerson.person;

    // Fetch della persona e dei suoi contratti

    JPQLQuery query = getQueryFactory().from(qperson).leftJoin(qperson.contracts).fetch()
        .where(qperson.id.eq(id)).distinct();

    Person person = query.singleResult(qperson);

    fetchContracts(Sets.newHashSet(person), begin, end);
    
    // Fetch dei buoni pasto (non necessaria, una query)
    // Fetch dei personday

    personDayDao.getPersonDayInPeriod(person, begin.get(), end);

    return person;

  }

  /**
   * Fetch di tutti dati dei contratti attivi nella finestra temporale specificata. Si può filtrare
   * su una specifica persona.
   */
  private void fetchContracts(Set<Person> person, Optional<LocalDate> start,
      Optional<LocalDate> end) {


    // Fetch dei contratti appartenenti all'intervallo
    QContract contract = QContract.contract;
    QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
    QVacationPeriod vp = QVacationPeriod.vacationPeriod;
    QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;

    final BooleanBuilder condition = new BooleanBuilder();
    if (!person.isEmpty()) {
      condition.and(contract.person.in(person));
    }
    filterContract(condition, start, end);

    JPQLQuery query2 = getQueryFactory().from(contract).leftJoin(contract.contractMonthRecaps)
        .fetch().leftJoin(contract.contractStampProfile).fetch()
        .leftJoin(contract.contractWorkingTimeType, cwtt).fetch()
        .orderBy(contract.beginDate.asc()).distinct();
    List<Contract> contracts = query2.where(condition).list(contract);

    // fetch contract multiple bags (1) vacation periods
    JPQLQuery query2b = getQueryFactory().from(contract).leftJoin(contract.vacationPeriods, vp)
        .fetch().orderBy(contract.beginDate.asc())/*.orderBy(vp.beginDate.asc())*/.distinct();
    contracts = query2b.where(condition).list(contract);
    // TODO: riportare a List tutte le relazioni uno a molti di contract
    // e inserire singolarmente la fetch.
    // TODO 2: in realtà questo è opinabile. Anche i Set
    // sono semanticamente corretti. Decidere.

    if (!person.isEmpty()) {
      // Fetch dei tipi orario associati ai contratti (verificare l'utilità)
      JPQLQuery query3 = getQueryFactory().from(cwtt).leftJoin(cwtt.workingTimeType, wtt).fetch()
          .where(cwtt.contract.in(contracts).and(cwtt.contract.person.in(person))).distinct();
      query3.list(cwtt);
    }
  }

  /**
   * Genera la lista di PersonLite contenente le persone attive nel mese specificato appartenenti ad
   * un office in offices. Importante: utile perchè non sporca l'entity manager con oggetti
   * parziali.
   */
  public List<PersonLite> liteList(Set<Office> offices, int year, int month) {

    final QPerson person = QPerson.person;

    Optional<LocalDate> beginMonth = Optional.fromNullable(new LocalDate(year, month, 1));
    Optional<LocalDate> endMonth =
        Optional.fromNullable(beginMonth.get().dayOfMonth().withMaximumValue());


    JPQLQuery lightQuery =
        getQueryFactory().from(person).leftJoin(person.contracts, QContract.contract)
            .orderBy(person.surname.asc(), person.name.asc()).distinct();


    lightQuery = personQuery(lightQuery, Optional.<String>absent(), offices, false, beginMonth,
        endMonth, true, Optional.<CompetenceCode>absent(), /*Optional.<Person>absent(),*/ false);

    QBean<PersonLite> bean =
        Projections.bean(PersonLite.class, person.id, person.name, person.surname);

    return lightQuery.list(bean);

  }

  /**
   * Questo metodo ci è utile per popolare le select delle persone.
   *
   * @param offices gli uffici di appartenenza delle persone richieste
   * @return la Lista delle persone appartenenti agli uffici specificati
   */
  public List<PersonLite> peopleInOffices(Set<Office> offices) {

    final QPerson person = QPerson.person;


    JPQLQuery lightQuery =
        getQueryFactory().from(person).leftJoin(person.contracts, QContract.contract)
            .orderBy(person.surname.asc(), person.name.asc()).distinct();


    lightQuery = personQuery(lightQuery, Optional.absent(), offices, false, Optional.absent(),
        Optional.absent(), true, Optional.absent(), /*Optional.absent(),*/ false);

    QBean<PersonLite> bean =
        Projections.bean(PersonLite.class, person.id, person.name, person.surname);

    return lightQuery.list(bean);
  }

  /**
   * Query ad hoc fatta per i Jobs che inviano le email di alert per segnalare problemi sui giorni.
   *
   * @return la Lista di tutte le persone con i requisiti adatti per poter effettuare le
   *         segnalazioni dei problemi.
   */
  public List<Person> eligiblesForSendingAlerts() {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QOffice office = QOffice.office;
    final QConfiguration config = QConfiguration.configuration;

    final BooleanBuilder baseCondition = new BooleanBuilder();

    // Requisiti della Persona
    baseCondition.and(person.wantEmail.isTrue()); // la persona non ha l'invio mail disabilitato

    // Requisiti sul contratto
    // il contratto è attivo per l'invio attestati
    baseCondition.and(contract.onCertificate.isTrue());
    // il contratto deve essere attivo oggi
    final LocalDate today = LocalDate.now();
    filterContract(baseCondition, Optional.of(today), Optional.of(today));

    final BooleanBuilder sendEmailCondition = new BooleanBuilder();
    // Requisiti sulla configurazione dell'office
    // L'ufficio ha l'invio mail attivo
    sendEmailCondition
        .and(config.epasParam.eq(EpasParam.SEND_EMAIL).and(config.fieldValue.eq("true")));
    // Se l'ufficio ha il parametro per l'autocertificazione disabilitato coinvolgo
    // tutti i dipendenti

    final BooleanBuilder trAutoCertificationDisabledCondition = new BooleanBuilder();
    trAutoCertificationDisabledCondition.and(config.epasParam.eq(EpasParam.TR_AUTOCERTIFICATION)
        .and(config.fieldValue.eq("false")));

    // Se il parametro è attivo escludo i tecnologi e i ricercatori
    final BooleanBuilder trAutoCertificationEnabledCondition = new BooleanBuilder();
    trAutoCertificationEnabledCondition.and(config.epasParam.eq(EpasParam.TR_AUTOCERTIFICATION)
        .and(config.fieldValue.eq("true")).and(person.qualification.qualification.gt(3)));

    final JPASubQuery personSendEmailTrue = new JPASubQuery().from(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(baseCondition, sendEmailCondition);

    final JPASubQuery personAutocertDisabled = new JPASubQuery().from(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(baseCondition, trAutoCertificationDisabledCondition);

    final JPASubQuery autocertEnabledOnlyTecnicians = new JPASubQuery().from(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(baseCondition, trAutoCertificationEnabledCondition);

    return queryFactory.from(person)
        .where(
            person.id.in(personSendEmailTrue.list(person.id)),
            person.id.in(personAutocertDisabled.list(person.id))
                .or(person.id.in(autocertEnabledOnlyTecnicians.list(person.id))))
        .distinct().list(person);
  }

  /**
   * Restituisce tutti i tecnologi e ricercatori delle sedi sulle quali è abilitata
   * l'autocertificazione, verificando i requisiti per l'invio della mail
   * (contratto attivo, in attestati, parametro wantEmail true etc...)
   *
   * @return La lista contenente tutti i tecnologi e ricercatori delle sedi nelle quali è
   *         attiva l'autocertificazione.
   */
  public List<Person> trWithAutocertificationOn() {

    final QPerson person = QPerson.person;
    final QContract contract = QContract.contract;
    final QOffice office = QOffice.office;
    final QConfiguration config = QConfiguration.configuration;

    final BooleanBuilder baseCondition = new BooleanBuilder();

    // Requisiti della Persona
    baseCondition.and(person.wantEmail.isTrue()); // la persona non ha l'invio mail disabilitato

    // Requisiti sul contratto
    // il contratto è attivo per l'invio attestati
    baseCondition.and(contract.onCertificate.isTrue());
    // il contratto deve essere attivo oggi
    final LocalDate today = LocalDate.now();
    filterContract(baseCondition, Optional.of(today), Optional.of(today));

    final BooleanBuilder sendEmailCondition = new BooleanBuilder();
    // Requisiti sulla configurazione dell'office
    // L'ufficio ha l'invio mail attivo
    sendEmailCondition
        .and(config.epasParam.eq(EpasParam.SEND_EMAIL).and(config.fieldValue.eq("true")));

    // Prendo solo i tecnologi e i ricercatori delle sedi dove è stato attivato il parametro
    // per l'autocertificazione
    final BooleanBuilder trAutoCertificationEnabledCondition = new BooleanBuilder();
    trAutoCertificationEnabledCondition.and(config.epasParam.eq(EpasParam.TR_AUTOCERTIFICATION)
        .and(config.fieldValue.eq("true")).and(person.qualification.qualification.loe(3)));

    final JPASubQuery personSendEmailTrue = new JPASubQuery().from(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(baseCondition, sendEmailCondition);

    final JPASubQuery trAutocertEnabled = new JPASubQuery().from(person)
        .leftJoin(person.contracts, contract)
        .leftJoin(person.office, office)
        .leftJoin(office.configurations, config)
        .where(baseCondition, trAutoCertificationEnabledCondition);

    return queryFactory.from(person)
        .where(
            person.id.in(personSendEmailTrue.list(person.id)),
            person.id.in(trAutocertEnabled.list(person.id)))
        .distinct().list(person);
  }

  /**
   * Dto contenente le sole informazioni della persona richieste dalla select nel template menu.
   */
  public static class PersonLite {

    public Long id;
    public String name;
    public String surname;

    public Person person = null;

    /**
     * Costruttore.
     *
     * @param id      id
     * @param name    nome
     * @param surname cognome
     */
    public PersonLite(Long id, String name, String surname) {
      this.id = id;
      this.name = name;
      this.surname = surname;
    }

    @Override
    public String toString() {
      return surname + ' ' + name;
    }

  }

}
