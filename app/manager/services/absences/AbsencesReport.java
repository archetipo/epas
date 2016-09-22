package manager.services.absences;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceTrouble.AbsenceTypeProblem;
import models.absences.AbsenceTrouble.ImplementationProblem;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;

@Slf4j
public class AbsencesReport {

  // List degli errori
  public Map<Absence, List<AbsenceTrouble>> absenceTroublesMap = Maps.newHashMap();
  public List<ReportAbsenceTypeProblem> absenceTypeProblems = Lists.newArrayList();
  public List<ReportRequestProblem> requestProblems = Lists.newArrayList();
  public List<ReportImplementationProblem> implementationProblems = Lists.newArrayList();
  
  // Esiti degli inserimenti
  public List<InsertResultItem> insertResultItems = Lists.newArrayList();

  /**
   * Errori non dipendenti dall'user (tipi assenza, implementazione, form di richiesta).
   * @return
   */
  public boolean containsCriticalProblems() {
    return !absenceTypeProblems.isEmpty()
        || !implementationProblems.isEmpty()
        || !requestProblems.isEmpty();
  }
  
  public boolean containsProblems() {
    //TODO: to implement
    return !absenceTroublesMap.keySet().isEmpty() 
        || !absenceTypeProblems.isEmpty()
        || !requestProblems.isEmpty()
        || !implementationProblems.isEmpty();
  }
  
  public boolean absenceHasProblems(Absence absence) {
    return this.absenceTroublesMap.get(absence) != null;
  }
  
  public boolean absenceTypeHasProblem(AbsenceType absenceType) {
    //TODO: map
    for (ReportAbsenceTypeProblem reportAbsenceTypeProblem : this.absenceTypeProblems) {
      if (reportAbsenceTypeProblem.absenceType.equals(absenceType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Aggiunge un nuovo problema sulle assenze alla lista. La generazione della mappa blocca
   * @param reportAbsenceProblem
   */
  public void addAbsenceTrouble(AbsenceTrouble absenceTrouble) {
    List<AbsenceTrouble> problems = absenceTroublesMap.get(absenceTrouble.absence);
    if (problems == null) {
      problems = Lists.newArrayList();
      absenceTroublesMap.put(absenceTrouble.absence, problems);
    }
    problems.add(absenceTrouble);
    log.debug("Aggiunto a report.absenceProblems: " + absenceTrouble.toString());
  }
  
  
  
  public void addAbsenceTypeProblem(ReportAbsenceTypeProblem reportAbsenceTypeProblem) {
    this.absenceTypeProblems.add(reportAbsenceTypeProblem );
  }
  
  public void addAbsenceAndImplementationProblem(AbsenceTrouble absenceTrouble) {
    this.addAbsenceTrouble(absenceTrouble);
    this.addImplementationProblem(ReportImplementationProblem.builder()
        .date(absenceTrouble.absence.getAbsenceDate())
        .implementationProblem(ImplementationProblem.UnespectedProblem)
        .build());
  };
  
  public void addRequestProblem(ReportRequestProblem reportRequestProblem) {
    this.requestProblems.add(reportRequestProblem);
  }
  
  public void addImplementationProblem(ReportImplementationProblem implementationProblem) {
    this.implementationProblems.add(implementationProblem);
  }
  
  public void addInsertResultItem(InsertResultItem insertResultItem) {
    log.debug("Aggiunto a report.insertResultItems: " + insertResultItem.toString());
    this.insertResultItems.add(insertResultItem);
  }

  @Builder
  public static class ReportAbsenceProblem {
    public AbsenceProblem absenceProblem;
    public Absence absence;
    
    public String toString() {
      return MoreObjects.toStringHelper(ReportAbsenceProblem.class)
      .add("date", absence.getAbsenceDate())
      .add("code", absence.absenceType.code)
      .add("problem", absenceProblem)
      .toString();
    }
  }
  
  @Builder
  public static class ReportAbsenceTypeProblem {
    public AbsenceTypeProblem absenceTypeProblem;
    public AbsenceType absenceType;
    public AbsenceType conflictingAbsenceType;
  }
  
  @Builder
  public static class ReportRequestProblem {
    public RequestProblem requestProblem;
    public LocalDate date;
  }
  
  @Builder
  public static class ReportImplementationProblem {
    public ImplementationProblem implementationProblem;
    public LocalDate date;
  }
  
}
