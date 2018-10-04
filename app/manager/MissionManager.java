package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import controllers.Security;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import lombok.extern.slf4j.Slf4j;

import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;

import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonDay;
import models.User;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.exports.MissionFromClient;
import models.exports.StampingFromClient;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.collections.Lists;

import play.db.jpa.JPA;




@Slf4j
public class MissionManager {

  private final PersonDao personDao;
  private final AbsenceService absenceService;
  private final AbsenceManager absenceManager;
  private final PersonDayManager personDayManager;
  private final ConsistencyManager consistencyManager;
  private final NotificationManager notificationManager;
  private final AbsenceDao absenceDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final ConfigurationManager configurationManager;
  private final OfficeDao officeDao;
  private final IWrapperFactory wrapperFactory;

  @Inject
  public MissionManager(PersonDao personDao, AbsenceService absenceService, 
      AbsenceManager absenceManager, PersonDayManager personDayManager,
      ConsistencyManager consistencyManager, NotificationManager notificationManager,
      AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao, 
      ConfigurationManager configurationManager, OfficeDao officeDao, 
      IWrapperFactory wrapperFactory) {
    this.personDao = personDao;
    this.absenceService = absenceService;
    this.absenceManager = absenceManager;
    this.personDayManager = personDayManager;
    this.consistencyManager = consistencyManager;
    this.notificationManager = notificationManager;
    this.absenceDao = absenceDao;
    this.absenceTypeDao = absenceTypeDao;
    this.configurationManager = configurationManager;
    this.officeDao = officeDao;
    this.wrapperFactory = wrapperFactory;
  }

  private static final DateTimeFormatter DT_FORMATTER =
      DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
  
  /**
   * Metodo che verifica se al numero di matricola passato come parametro nel dto corrisponde
   * effettivamente una persona in anagrafica.
   * @param mission il dto creato dal json arrivato dal listener
   * @return la person, se esiste, associata alla matricola contenuta nel dto.
   */
  public Optional<Person> linkToPerson(MissionFromClient mission) {

    Optional<User> user = Security.getUser();
    if (!user.isPresent()) {
      log.error("Impossibile recuperare l'utente che ha inviato la missione: {}", mission);
      return Optional.absent();
    }

    final Optional<Person> person = Optional.fromNullable(personDao
        .getPersonByNumber(mission.matricola));

    if (person.isPresent()) {
      mission.person = person.get();      
    } else {
      log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la missione,"
          + " matricola={}. Controllare il database.", mission.matricola);
    }

    return person;
  }

  /**
   * Metodo che crea la missione a partire dai dati del dto inviato da Missioni.
   * @param body il dto ricavato dal json arrivato dal listener
   * @param recompute se deve essere avviato il ricalcolo
   * @return true se riesce a inserire la missione con i parametri esplicitati nel body, 
   *     false altrimenti.
   */
  public boolean createMissionFromClient(MissionFromClient body, boolean recompute) {

    AbsenceForm absenceForm = buildAbsenceForm(body);
    //controllo che la missione sia su più giorni
    if (body.dataFine.toLocalDate().isAfter(body.dataInizio.toLocalDate())) {
            
      Optional<Office> office = officeDao.byCodeId(body.codiceSede + "");
      if (!office.isPresent()) {
        log.warn("Sede di lavoro associata a {} non trovata!", body.person.fullName());
        return false;
      }
      //verifico il parametro di ora inizio lavoro in sede
      LocalTime beginWorkHour = (LocalTime)configurationManager
          .configValue(office.get(), EpasParam.HOUR_START_WORK_LAST_MISSION_DAY, LocalDate.now());
      if (beginWorkHour == null) {
        log.warn("Il parametro di inizio orario di lavoro per ultimo giorno di missione "
            + "non è valorizzato per la sede {}", office.get().name);
        return false;
      }
      //WorkingTimeTypeDay day = getFromDayOfMission(body, body.dataFine.toLocalDate());
      LocalDateTime beginMission = body.dataInizio;
      LocalDateTime secondLast = body.dataFine.minusDays(1).withHourOfDay(23).withMinuteOfHour(59);
      LocalDateTime beginWork = body.dataFine
          .withTime(beginWorkHour.getHourOfDay(), beginWorkHour.getMinuteOfHour(), 0, 0);
      boolean check1 = insertMission(body, absenceForm, null, null, beginMission, secondLast);
      boolean check2 = insertMission(body, absenceForm, null, null, beginWork, body.dataFine);
      if (check1 && check2) {
        recalculate(body, Optional.<List<Absence>>absent());
        return true;
      }
      
    } else {
      if (insertMission(body, absenceForm, null, null, body.dataInizio, body.dataFine)) {
        recalculate(body, Optional.<List<Absence>>absent());
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Metodo di utilità.
   * @param body il dto proveniente da Missioni
   * @param end l'ultimo giorno di missione
   * @return il workingTimeTypeDay dell'ultimo giorno di missione.
   */
  private WorkingTimeTypeDay getFromDayOfMission(MissionFromClient body, LocalDate end) {
    IWrapperPerson wrappedPerson = wrapperFactory.create(body.person);  
    Optional<ContractWorkingTimeType> cwtt = wrappedPerson.getCurrentContractWorkingTimeType();
    WorkingTimeTypeDay dayNumber = null;
    if (cwtt.isPresent()) {
      WorkingTimeType wtt = cwtt.get().workingTimeType;
      int day = end.getDayOfWeek();
      
      for (WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
        if (wttd.dayOfWeek == day) {
          dayNumber = wttd;
        }
      }
    }
    return dayNumber;
  }
  
  
  /**
   * Metodo che ricontrolla le date della missione dopo le info pervenute da Missioni in occasione
   * del rimborso.
   * @param body il dto contenente i dati della missione
   * @param recompute se deve essere effettuato il ricalcolo
   * @return true se la gestione della missione è andata a buon fine, false altrimenti.
   */
  public boolean manageMissionFromClient(MissionFromClient body, boolean recompute) {
    if (body.idOrdine == null) {
      return false;
    }
    List<Absence> missions = absenceDao.absencesPersistedByMissions(body.idOrdine);
    if (missions.isEmpty()) {
      return false;
    }
    AbsenceForm absenceForm = buildAbsenceForm(body);
    
    List<LocalDate> dates = datesToCompute(body);
    List<Absence> toRemove = missions.stream()
        .filter(abs -> !dates.contains(abs.personDay.date)).collect(Collectors.toList());
    List<LocalDate> missionsDate = missions.stream()
        .map(a -> a.personDay.date).collect(Collectors.toList());
    List<LocalDate> toAdd = dates.stream()
        .filter(p -> !missionsDate.contains(p)).collect(Collectors.toList());        

    if (!performDeleteMission(toRemove, absenceForm)) {
      return false;
    }
    
    boolean result = false;
    for (LocalDate date : toAdd) {
      result = insertMission(body, absenceForm, null, null, 
          date.toLocalDateTime(LocalTime.MIDNIGHT), date.toLocalDateTime(LocalTime.MIDNIGHT));
    }
    recalculate(body, Optional.fromNullable(missions));
    if (result) {
      return true;
    }
    return false;
  }
  
  /**
   * Metodo che cancella una missione inviata precedentemente da Missioni.
   * @param body il dto contenente i dati della missione
   * @param recompute se devono essere effettuati i ricalcoli
   * @return true se la missione viene cancellata, false altrimenti.
   */
  public boolean deleteMissionFromClient(MissionFromClient body, boolean recompute) {
    List<Absence> missions = absenceDao.absencesPersistedByMissions(body.idOrdine);
    if (missions.isEmpty()) {
      return false;
    }
    AbsenceForm absenceForm = buildAbsenceForm(body);
    if (!performDeleteMission(missions, absenceForm)) {
      return false;
    }
    recalculate(body, Optional.fromNullable(missions));
    return true;
  }
  
  /**
   * Metodo privato che ritorna la lista delle date su cui fare i controlli.
   * @param body il dto contenente i dati della missione
   * @return la lista delle date da considerare per gestire la missione.
   */
  private List<LocalDate> datesToCompute(MissionFromClient body) {
    List<LocalDate> dates = Lists.newArrayList();
    LocalDate current = body.dataInizio.toLocalDate();
    while (!current.isAfter(body.dataFine)) {
      dates.add(current);
      current = current.plusDays(1);
    }
    return dates;
  }
  
  /**
   * Metodo privato che inserisce i giorni di missione.
   * @param body il dto contenente le informazioni della missione
   * @param absenceForm l'absenceForm relativo alla missione
   * @param hours le ore 
   * @param minutes i minuti
   * @param from la data di inizio da cui inserire la missione
   * @param to la data di fine fino a cui inserire la missione
   * @return true se la missione è stata inserita, false altrimenti.
   */
  private boolean insertMission(MissionFromClient body, AbsenceForm absenceForm, 
      Integer hours, Integer minutes, LocalDateTime from, LocalDateTime to) {
    
    AbsenceType mission = null;

    CategoryTab tab = absenceForm.categoryTabSelected;
    GroupAbsenceType type = absenceForm.groupSelected;
    JustifiedType justifiedType = absenceForm.justifiedTypeSelected;
    Integer localHours = hours;
    Integer localMinutes = minutes;
    if (from.toLocalDate().isEqual(to.toLocalDate())) {
       
      localHours = to.getHourOfDay() - from.getHourOfDay();
      localMinutes = to.getMinuteOfHour() - from.getMinuteOfHour();
//      if (localMinutes < 0) {
//        localHours = localHours - 1;
//      }
//      
//      if (localMinutes > DateTimeConstants.MINUTES_PER_HOUR / 2) {
//        localHours = localHours + 1;
//      }
      int day = getFromDayOfMission(body, to.toLocalDate()).dayOfWeek;
      if (day == DateTimeConstants.SATURDAY || day == DateTimeConstants.SUNDAY) {
        mission = absenceTypeDao.getAbsenceTypeByCode("92").get();
      } else {
        mission = absenceTypeDao.getAbsenceTypeByCode("92M").get();
      }      
            
    }
    if (mission == null) {
      mission = absenceForm.absenceTypeSelected;
    }
    absenceForm =
        absenceService.buildAbsenceForm(body.person, from.toLocalDate(), tab,

            to.toLocalDate(), null, type, false, mission, 
            justifiedType, hours, minutes, false);

    InsertReport insertReport = 
        absenceService.insert(body.person, absenceForm.groupSelected, from.toLocalDate(), 
            to.toLocalDate(), mission, absenceForm.justifiedTypeSelected, 
            localHours, localMinutes, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty() || insertReport.warningsPreviousVersion.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(body.person, absence.getAbsenceDate());
        absence.personDay = personDay;
        if (body.idOrdine != null) {
          absence.externalIdentifier = body.idOrdine;
        } else {
          absence.externalIdentifier = body.id;
        }
        absence.note = "Inizio: " + DT_FORMATTER.print(body.dataInizio)
            + System.lineSeparator() + "Fine: " + DT_FORMATTER.print(body.dataFine);
        personDay.absences.add(absence);
        absence.save();
        personDay.save();

        final User currentUser = Security.getUser().get();
        notificationManager.notificationAbsencePolicy(currentUser, 
            absence, absenceForm.groupSelected, true, false, false);
        log.info("inserita assenza {} del {} per {}", absence.absenceType.code, 
            absence.personDay.date, absence.personDay.person.fullName());

      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        absenceManager
        .sendReperibilityShiftEmail(body.person, insertReport.reperibilityShiftDate());
        log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.",
            body.person.fullName(), insertReport.reperibilityShiftDate());
      }
      JPA.em().flush();
      return true;
    }
    
    return false;
  }
  
  /**
   * Metodo che si occupa di cancellare i singoli giorni di missione.
   * @param missions la lista delle assenze da cancellare
   * @param absenceForm l'absenceForm per notificare la cancellazione dell'assenza
   * @return true se la cancellazione è andata a buon fine, false altrimenti.
   */
  private boolean performDeleteMission(List<Absence> missions, AbsenceForm absenceForm) {
    boolean result = false;
    for (Absence abs : missions) {
      result = atomicRemoval(abs, result);
      final User currentUser = Security.getUser().get();
      notificationManager.notificationAbsencePolicy(currentUser, 
          abs, absenceForm.groupSelected, false, true, false);
      log.info("rimossa assenza {} del {}", abs.absenceType.code, abs.personDay.date);
      
    }
    if (result) {
      return true;
    }
    log.error("Errore in rimozione della missione}");
    return false;
  }
  
  /**
   * Metodo che cancella dal personDay l'assenza.
   * @param abs l'assenza (missione) da cancellare
   * @param result se la rimozione precedente è andata a buon fine
   * @return true se l'assenza è stata cancellata, false altrimenti.
   */
  private boolean atomicRemoval(Absence abs, boolean result) {
    try {
      abs.delete();
      abs.personDay.absences.remove(abs);
      abs.personDay.save();
      result = true;
    } catch (Exception ex) {
      result = false;
      throw new PersistenceException("Error in removing absence");
    }
    
    return result;
  }
  
  private AbsenceForm buildAbsenceForm(MissionFromClient body) {
    CategoryTab categoryTab = null;
    GroupAbsenceType groupAbsenceType = null;
    AbsenceType absenceType = null;
    JustifiedType justifiedType = null;
    Integer hours = null;
    Integer minutes = null;
    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(body.person, body.dataInizio.toLocalDate(), 
            categoryTab, body.dataFine.toLocalDate(), null, 
            groupAbsenceType, false, absenceType, justifiedType, hours, minutes, false);
    return absenceForm;
  }

  
  /**
   * ricalcola la situazione del dipendente.
   * @param body il dto contenente le info dell'ordine/rimborso di missione
   */
  private void recalculate(MissionFromClient body, Optional<List<Absence>> missions) {
    LocalDate begin = body.dataInizio.toLocalDate();
    if (missions.isPresent()) {
      
      for (Absence abs : missions.get()) {
        if (abs.personDay.date.isBefore(begin)) {
          begin = abs.personDay.date;
        }
      }
    }
    consistencyManager.updatePersonSituation(body.person.id, begin);
  }
  
  /**
   * Metodo privato che conteggia quante ore devono essere date di missione (o intera giornata).
   * @param hours le ore da considerare per cercare il codice di assenza giusto
   * @return il codice d'assenza corretto sulla base della quantità oraria passata come parametro.
   */
  private AbsenceType selectMissionHour(int hours) {
    AbsenceType missionHours = null;
    switch (hours) {
      case 1:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h1").get();
        break;
      case 2:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h2").get();
        break;
      case 3:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h3").get();
        break;
      case 4:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h4").get();
        break;
      case 5:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h5").get();
        break;
      case 6:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h6").get();
        break;
      case 7:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92h7").get();
        break;
      default:
        missionHours = absenceTypeDao.getAbsenceTypeByCode("92").get();
        break;
    }
    return missionHours;
  }
}
