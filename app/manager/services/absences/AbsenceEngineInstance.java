package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.AbsenceDao;
import dao.PersonChildrenDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngine.AbsenceEngineProblem;
import manager.services.absences.AbsenceEngine.AbsencePeriod;
import manager.services.absences.AbsenceEngine.ResponseItem;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsenceEngineInstance {

  //Dependencies Injected
  private final AbsenceDao absenceDao;
  private final AbsenceMigration absenceMigration;
  private final PersonChildrenDao personChildrenDao;
  
  // Dati della richiesta
  public LocalDate date;
  public GroupAbsenceType groupAbsenceType;
  public Person person;
  
  // Errori
  public Optional<AbsenceEngineProblem> absenceEngineProblem = Optional.absent();

  // Risultato richiesta
  public List<ResponseItem> responseItems = Lists.newArrayList();
  
  // Strutture ausiliare lazy
  
  private LocalDate from = null;
  private LocalDate to = null;
  private List<Contract> contracts = null;
  private List<PersonChildren> orderedChildren = null;
  private List<Absence> absences = null;
  private InitializationGroup initializationGroup = null;
  
  // AbsencePeriod
  protected AbsencePeriod absencePeriod;
  
  protected AbsenceEngineInstance(AbsenceDao absenceDao, AbsenceMigration absenceMigration,
      PersonChildrenDao personChildrenDao, Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate date) {
    this.absenceDao = absenceDao;
    this.absenceMigration = absenceMigration;
    this.personChildrenDao = personChildrenDao;
    this.person = person;
    this.groupAbsenceType = groupAbsenceType;
    this.date = date;
  }
  
  public List<PersonChildren> getOrderedChildren() {
    if (this.orderedChildren == null) {
      this.orderedChildren = 
          personChildrenDao.getAllPersonChildren(this.person);
    }
    return this.orderedChildren;
  }
  
  public int workingTime(LocalDate date) {
    if (this.contracts == null) {
      this.contracts = Lists.newArrayList();
      for (Contract contract : person.contracts) {
        if (DateUtility.intervalIntersection(
            contract.periodInterval(), new DateInterval(getFrom(), getTo())) != null) {
          this.contracts.add(contract);
        }
      }
    }
    for (Contract contract : this.contracts) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
        if (DateUtility.isDateIntoInterval(date, cwtt.periodInterval())) {
          return cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1)
              .workingTime;
        }
      }
    }
    return 0;
  }
  
  public LocalDate getFrom() {
    if (this.from == null) {
      buildInterval();
    }
    return this.from;
  }
  
  public LocalDate getTo() {
    if (this.to == null) {
      buildInterval();
    }
    return this.to;
  }
  
  private void buildInterval() {
 
    this.from = this.absencePeriod.from;
    this.to = this.absencePeriod.to;
    AbsencePeriod currentAbsencePeriod = this.absencePeriod;
    while (currentAbsencePeriod.nextAbsencePeriod != null) {
      if (currentAbsencePeriod.nextAbsencePeriod.from.isBefore(this.from)) {
        this.from = currentAbsencePeriod.nextAbsencePeriod.from;
      }
      if (currentAbsencePeriod.nextAbsencePeriod.to.isAfter(this.to)) {
        this.to = currentAbsencePeriod.nextAbsencePeriod.to;
      }
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
  }
  
  public List<Absence> getAbsences() {
    if (this.absences == null) {
      // 1) Prendere tutti i codici (anche quelli ricorsivi)
      Set<AbsenceType> absenceTypes = Sets.newHashSet();
      AbsencePeriod currentAbsencePeriod = this.absencePeriod;
      while (currentAbsencePeriod != null) {
        if (currentAbsencePeriod.takableComponent.isPresent()) {
          absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takenCodes);
          //absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takableCodes);
        }
        if (currentAbsencePeriod.complationComponent.isPresent()) {
          absenceTypes.addAll(currentAbsencePeriod.complationComponent.get().replacingCodes);
          absenceTypes.addAll(currentAbsencePeriod.complationComponent.get().complationCodes);
        }
        currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
      }

      // 2) Scaricare le assenze
      this.absences = this.absenceDao.getAbsencesInCodeList(this.person, 
          this.getFrom(), this.getTo(), Lists.newArrayList(absenceTypes), true);

      // 2bis) Deve diventare un job efficiente da fare al bootstrap.
      for (Absence absence : this.absences) {
        if (absence.justifiedType == null) {
          absenceMigration.migrateAbsence(absence);
        }
      }
      
      // 3) Popolare le liste taken e complation absences.
//      currentAbsencePeriod = this.absencePeriod;
//      while (currentAbsencePeriod != null) {
//
//        for (Absence absence : this.absences) {
//          // stesso period
//          if (DateUtility.isDateIntoInterval(absence.getAbsenceDate(), currentAbsencePeriod.periodInterval())) {
//            
//            // codice appartenente agli elenchi
//            if (currentAbsencePeriod.takableComponent.isPresent()) {
//              if (currentAbsencePeriod.takableComponent.get().takenCodes.contains(absence.absenceType)) {
//                currentAbsencePeriod.takableComponent.get().takenAbsences.add(absence);
//              }
//            }
//            if (currentAbsencePeriod.complationComponent.isPresent()) {
//              if (currentAbsencePeriod.complationComponent.get().complationCodes.contains(absence.absenceType)) {
//                currentAbsencePeriod.complationComponent.get().complationAbsences.add(absence);
//              }
//              if (currentAbsencePeriod.complationComponent.get().replacingCodes.contains(absence.absenceType)) {
//                currentAbsencePeriod.complationComponent.get().replacingAbsences.add(absence);
//              }
//            }
//          }
//
//        }
//        currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
//      }
    }
    
    return this.absences;
  }
  
}