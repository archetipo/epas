package synch.perseoconsumers.people;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Maps;

import dao.QualificationDao;

import lombok.extern.slf4j.Slf4j;

import models.Person;
import models.Qualification;

import org.assertj.core.util.Lists;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import synch.perseoconsumers.PerseoApis;

import java.util.List;
import java.util.Map;

@Slf4j
public class PeoplePerseoConsumer {

  private final QualificationDao qualificationDao;

  @Inject
  public PeoplePerseoConsumer(QualificationDao qualificationDao) {
    this.qualificationDao = qualificationDao;
  }

  /**
   * Json relativo alla persona di perseo con id perseoId nel formato utile a epas.
   *
   * @param perseoId id perseo persona
   * @return json
   */
  private Optional<String> perseoPersonJson(Long perseoId) throws NoSuchFieldException {

    String endPoint = PerseoApis.getPersonForEpasEndpoint() + perseoId;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la persona da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare la persona da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception exp) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * Json relativo a tutte le persone di perseo nel formato utile a epas.
   *
   * @return json
   */
  private Optional<String> perseoAllPeopleJson() throws NoSuchFieldException {

    String endPoint = PerseoApis.getAllPeopleForEpasEndpoint();
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la lista di tutti gli istituti presenti da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare la lista degli istituti presenti da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception exp) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }

  /**
   * Json relativo a tutte le persone di perseo nel perseoDepartment nel formato utile a epas.
   *
   * @param perseoId id perseo department
   * @return json
   */
  private Optional<String> perseoAllDepartmentPeopleJson(Long perseoId) throws NoSuchFieldException {

    String endPoint = PerseoApis.getAllDepartmentPeopleForEpasEndpoint() + perseoId;
    HttpResponse restResponse = WS.url(endPoint).get();
    log.info("Perseo: prelevo la lista di tutti gli istituti presenti da {}.", endPoint);

    if (!restResponse.success()) {
      log.error("Impossibile prelevare la lista degli istituti presenti da {}", endPoint);
      return Optional.<String>absent();
    }

    try {
      return Optional.fromNullable(restResponse.getJson().toString());
    } catch (Exception exp) {
      log.info("Url={} non json.", endPoint);
    }

    return null;
  }


  /**
   * La lista delle PerseoPerson nel perseoDepartment.
   *
   * @param perseoId perseo id department.
   * @return lista
   */
  private List<PerseoPerson> getAllPerseoDepartmentPeople(Long perseoId) throws NoSuchFieldException {

    //Json della richiesta
    Optional<String> json = perseoAllDepartmentPeopleJson(perseoId);
    if (json == null || !json.isPresent()) {
      return null;
    }

    List<PerseoPerson> perseoSimplePeople = null;
    try {
      perseoSimplePeople = new Gson()
          .fromJson(json.get(), new TypeToken<List<PerseoPerson>>() {
          }.getType());
    } catch (Exception exp) {
      log.info("Impossibile caricare da perseo la lista degli istituti.");
      return Lists.newArrayList();
    }

    return perseoSimplePeople;
  }

  /**
   * La lista delle PerseoPerson in Perseo.
   *
   * @return lista
   */
  private List<PerseoPerson> getAllPerseoPeople() throws NoSuchFieldException {

    //Json della richiesta
    Optional<String> json = perseoAllPeopleJson();
    if (json == null || !json.isPresent()) {
      return null;
    }

    List<PerseoPerson> perseoSimplePeople = null;
    try {
      perseoSimplePeople = new Gson()
          .fromJson(json.get(), new TypeToken<List<PerseoPerson>>() {
          }.getType());
    } catch (Exception exp) {
      log.info("Impossibile caricare da perseo la lista degli istituti.");
      return Lists.newArrayList();
    }

    return perseoSimplePeople;
  }

  /**
   * La PerseoPerson con quel perseoId in Perseo.
   *
   * @param perseoId perseo id person.
   * @return persona
   */
  private Optional<PerseoPerson> getPerseoPersonByPerseoId(Long perseoId) throws NoSuchFieldException {

    //Json della richiesta
    Optional<String> json = perseoPersonJson(perseoId);
    if (json == null || !json.isPresent()) {
      return null;
    }

    PerseoPerson perseoPerson = null;
    try {
      perseoPerson = new Gson()
          .fromJson(json.get(), new TypeToken<PerseoPerson>() {
          }.getType());
    } catch (Exception exp) {
      log.info("Impossibile caricare da perseo la sede con perseoId={}.", perseoId);
      return Optional.<PerseoPerson>absent();
    }
    if (perseoPerson == null) {
      return Optional.<PerseoPerson>absent();
    }
    return Optional.fromNullable(perseoPerson);

  }

  /**
   * Conversione a oggetti epas. PerseoPerson.
   *
   * @param perseoPerson      da convertire
   * @param qualificationsMap mappa delle qualifiche epas
   * @return person
   */
  private Person epasConverter(PerseoPerson perseoPerson,
                               Map<Integer, Qualification> qualificationsMap) {

    Person person = new Person();
    person.name = perseoPerson.firstname;
    person.surname = perseoPerson.surname;
    person.number = perseoPerson.number;
    person.email = perseoPerson.email; //per adesso le email non combaciano @iit.cnr.it vs @cnr.it
    //person.eppn = perseoPerson.email;
    person.qualification = qualificationsMap.get(perseoPerson.qualification);
    person.perseoId = perseoPerson.id;

    person.perseoOfficeId = perseoPerson.departmentId;

    return person;
  }

  /**
   * Conversione di una lista di oggetti epas. PerseoPerson
   *
   * @param perseoPeople lista di perseoPeople
   * @return persone epas
   */
  private List<Person> epasConverter(List<PerseoPerson> perseoPeople) {
    Map<Integer, Qualification> qualificationsMap = qualificationDao.allQualificationMap();
    List<Person> people = Lists.newArrayList();
    for (PerseoPerson perseoPerson : perseoPeople) {
      Person person = epasConverter(perseoPerson, qualificationsMap);
      if (person.number == null) {
        //non dovrebbe succedere...
        log.info("Giunta da Siper persona senza matricola... {}.", person.toString());
      } else {
        people.add(person);
      }
    }
    return people;
  }

  /**
   * Tutte le persone in perseo.<br> Formato mappa: perseoId -> person
   *
   * @return mappa
   */
  public Map<Long, Person> perseoPeopleByPerseoId() throws NoSuchFieldException {
    List<PerseoPerson> perseoPeople = getAllPerseoPeople();
    Map<Long, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : epasConverter(perseoPeople)) {
      perseoPeopleMap.put(person.perseoId, person);
    }
    return perseoPeopleMap;
  }

  /**
   * Tutte le persone in perseo.<br> Formato mappa: number -> person
   *
   * @return mappa
   */
  public Map<Integer, Person> perseoPeopleByNumber() throws NoSuchFieldException {

    Map<Integer, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : perseoPeopleByPerseoId().values()) {
      perseoPeopleMap.put(person.number, person);
    }
    return perseoPeopleMap;
  }

  /**
   * Tutte le persone del department con quel perseoId.<br> Formato mappa: perseoId -> person
   *
   * @param perseoDepartmentId department perseo id
   * @return mappa
   */
  public Map<Long, Person> perseoDepartmentPeopleByPerseoId(Long perseoDepartmentId) throws NoSuchFieldException {

    List<PerseoPerson> perseoPeople = getAllPerseoDepartmentPeople(perseoDepartmentId);
    Map<Long, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : epasConverter(perseoPeople)) {
      perseoPeopleMap.put(person.perseoId, person);
    }

    return perseoPeopleMap;
  }

  /**
   * Tutte le persone del department con quel perseoId.<br> Formato mappa: number -> person
   *
   * @param perseoDepartmentId department perseo id
   * @return mappa
   */
  public Map<Integer, Person> perseoDepartmentPeopleByNumber(Long perseoDepartmentId) throws NoSuchFieldException {

    Map<Integer, Person> perseoPeopleMap = Maps.newHashMap();
    for (Person person : perseoDepartmentPeopleByPerseoId(perseoDepartmentId).values()) {
      perseoPeopleMap.put(person.number, person);
    }
    return perseoPeopleMap;
  }


  /**
   * La persona con quel perseoId.
   *
   * @return persona
   */
  public Optional<Person> perseoPersonByPerseoId(Long personPerseoId) throws NoSuchFieldException {
    Optional<PerseoPerson> perseoPerson = getPerseoPersonByPerseoId(personPerseoId);
    if (!perseoPerson.isPresent()) {
      return Optional.<Person>absent();
    }
    Map<Integer, Qualification> qualificationsMap = qualificationDao.allQualificationMap();
    return Optional.fromNullable(epasConverter(perseoPerson.get(), qualificationsMap));
  }

}
