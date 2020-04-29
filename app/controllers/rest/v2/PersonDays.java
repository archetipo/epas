package controllers.rest.v2;

import cnr.sync.dto.v2.AbsenceDto;
import cnr.sync.dto.v2.PersonDayDto;
import cnr.sync.dto.v2.StampingDto;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.absences.Absence;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormatter;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class PersonDays extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;

  /**
   * Metodo rest che ritorna la situazione della persona (passata per email o eppn) in un giorno 
   * specifico (date).
   * Nel caso venga passato sia eppn che email la precedenza nella ricerca della persona va al 
   * campo eppn.
   */
  @BasicAuth
  public static void getDaySituation(String email, String eppn, 
      Long personPersoId, LocalDate date) {
    log.debug("getDaySituation -> email={}, eppn={}, date={}", email, date);
    if ((email == null && eppn == null) || date == null) {
      notFound();
    }
    Optional<Person> person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPersoId);

    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente in ePAS la "
              + "mail che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().office);

    PersonDay pd = personDayDao.getPersonDay(person.get(), date).orNull();
    if (pd == null) {
      JsonResponse.notFound("Non sono presenti informazioni per "
              + person.get().name + " " + person.get().surname + " nel giorno " + date);
    }
    PersonDayDto pdDto = generateDayDto(pd);
    renderJSON(pdDto);
  }
  
  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di tutti i dipendenti
   * di una certa sede nell'anno/mese passati come parametro.
   * @param sedeId l'identificativo della sede di cui ricercare la situazione delle persone
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  @BasicAuth
  public static void getMonthSituationByOffice(String sedeId, Integer year, Integer month) {
    log.debug("getMonthSituationByOffice -> sedeId={}, year={}, month={}", sedeId, year, month);
    if ((year == null && month == null) || sedeId == null) {
      notFound();
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      notFound();
    }
    rules.checkIfPermitted(office.get());
    Set<Office> offices = Sets.newHashSet();
    offices.add(office.get());
    List<Person> personList = personDao.getActivePersonInMonth(offices, new YearMonth(year, month));
    Map<Person, List<PersonDayDto>> map = Maps.newHashMap();
    for (Person person : personList) {
      List<PersonDayDto> list = Lists.newArrayList();
      LocalDate date = new LocalDate(year, month, 1);
      LocalDate until = date.dayOfMonth().withMaximumValue();
      while (!date.isAfter(until)) {
        PersonDay pd = personDayDao.getPersonDay(person, date).orNull();
        if (pd == null) {
          JsonResponse.notFound("Non sono presenti informazioni per "
                  + person.name + " " + person.surname + " nel giorno " + date);
        }
        PersonDayDto pdDto = generateDayDto(pd);
        
        if (map.containsKey(person)) {
          list = map.get(person);          
        } 
        list.add(pdDto);
        map.put(person, list);
        date = date.plusDays(1);
      }
    }
    log.debug("Terminato invio di informazioni della sede {} per l'anno {} mese {}", 
        office.get().name, year, month);
    renderJSON(map);
  }
  
  /**
   * Metodo che ritorna la mappa delle situazioni giornaliere di tutti 
   * i dipendenti della sede passata come parametro alla data passata come parametro.
   * @param sedeId l'identificativo della sede di cui cercare le persone
   * @param date la data per cui cercare i dati
   */
  @BasicAuth
  public static void getDaySituationByOffice(String sedeId, LocalDate date) {
    log.debug("getDaySituationByOffice -> sedeId={}, data={}", sedeId, date);
    if (sedeId == null || date == null) {
      notFound();
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      notFound();
    }
    rules.checkIfPermitted(office.get());
    Set<Office> offices = Sets.newHashSet();
    offices.add(office.get());
    List<Person> personList = personDao
        .list(Optional.<String>absent(), offices, false, date, date, true).list();
    Map<Person, PersonDayDto> map = Maps.newHashMap();
    for (Person person : personList) {
      PersonDay pd = personDayDao.getPersonDay(person, date).orNull();
      if (pd == null) {
        JsonResponse.notFound("Non sono presenti informazioni per "
                + person.name + " " + person.surname + " nel giorno " + date);
      }
      PersonDayDto pdDto = generateDayDto(pd);
      map.put(person, pdDto);
    }
    log.debug("Terminato invio di informazioni della sede {} per il giorno {}", 
        office.get().name, date);
    renderJSON(map);
  }

  /**
   * Ritorna il dto generato a partire dal person day passato come parametro.
   * @return il personDayDTO costruito sulla base del personDay passato come 
   *     parametro da ritornare alle funzioni rest.
   */
  private static PersonDayDto generateDayDto(PersonDay pd) {
    PersonDayDto pdDto = 
        PersonDayDto.builder()
          .data(pd.date.toString())
          .buonoPasto(pd.isTicketAvailable)
          .differenza(pd.difference)
          .progressivo(pd.progressive)
          .tempoLavoro(pd.timeAtWork)
          .giornoLavorativo(!pd.isHoliday)
          .build();
    if (pd.absences != null && pd.absences.size() > 0) {
      for (Absence abs : pd.absences) {
        pdDto.getCodiciAssenza().add(AbsenceDto.build(abs));
      }
    }
    if (pd.stampings != null && pd.stampings.size() > 0) {
      for (Stamping s : pd.stampings) {
        pdDto.getTimbrature().add(StampingDto.build(s));
      }
    }
    return pdDto;
  }

}
