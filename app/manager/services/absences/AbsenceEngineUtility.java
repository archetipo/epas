package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.AbsencePeriod.ComplationComponent;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class AbsenceEngineUtility {
  
  private final AbsenceComponentDao absenceComponentDao;
  
  private final Integer UNIT_REPLACING_AMOUNT = 1 * 100;

  @Inject
  public AbsenceEngineUtility(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }
  
  /**
   * Le operazioni univocamente identificabili dal justifiedType. Devo riuscire a derivare
   * l'assenza da inserire attraverso il justifiedType.
   *  Lista con priorità:
   *  - se esiste un solo codice allDay  -> lo metto tra le opzioni
   *  - se esiste un solo codice halfDay -> lo metto tra le opzioni
   *  - se esiste: un solo codice absence_type_minutes con Xminute
   *               un solo codice absence_type_minutes con Yminute
   *               ...
   *               un solo codice absence_type_minutes con Zminute
   *               un solo codice specifiedMinutes 
   *               -> metto specifiedMinutes tra le opzioni
   *  
   *  TODO: decidere come gestire il quanto manca               
   *                
   * @param groupAbsenceType
   * @return
   */
  public List<JustifiedType> automaticJustifiedType(GroupAbsenceType groupAbsenceType) {
    
    // TODO: gruppo ferie, riposi compensativi
    
    List<JustifiedType> justifiedTypes = Lists.newArrayList();
    
    //TODO: Copia che mi metto da parte... ma andrebbe cachata!!
    final JustifiedType specifiedMinutesVar = 
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    JustifiedType allDayVar = null;
    JustifiedType halfDayVar = null;

    Map<Integer, Integer> specificMinutesFinded = Maps.newHashMap(); //(minute, count)
    boolean specificMinutesDenied = false;
    Integer allDayFinded = 0;
    Integer halfDayFinded = 0;
    Integer specifiedMinutesFinded = 0;

    if (groupAbsenceType.takableAbsenceBehaviour == null) {
      return justifiedTypes;
    }
    
    for (AbsenceType absenceType : groupAbsenceType.takableAbsenceBehaviour.takableCodes) {
      for (JustifiedType justifiedType : absenceType.justifiedTypesPermitted) { 
        if (justifiedType.name.equals(JustifiedTypeName.all_day)) {
          allDayFinded++;
          allDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.half_day)) {
          halfDayFinded++;
          halfDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
          specifiedMinutesFinded++;
        }
        if (justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
          Integer minuteKey = absenceType.justifiedTime;
          Integer count = specificMinutesFinded.get(minuteKey);
          if (count == null) {
            specificMinutesFinded.put(minuteKey, 1);
          } else {
            count++;
            if (count > 1) {
              specificMinutesDenied = true;
            }
            specificMinutesFinded.put(minuteKey, count);
          }
        }
      }
    }
    
    if (allDayFinded == 1) {
      justifiedTypes.add(allDayVar);
    }
    if (halfDayFinded == 1) {
      justifiedTypes.add(halfDayVar);
    }
    if (specifiedMinutesFinded == 1 && specificMinutesDenied == false) {
      justifiedTypes.add(specifiedMinutesVar);
    }
    
    return justifiedTypes;
  }

  /**
   * Quanto giustifica l'assenza passata.
   * Se non si riesce a stabilire il tempo giustificato si ritorna un numero negativo.
   * @param person
   * @param date
   * @param absence
   * @param amountType
   * @return
   */
  public int absenceJustifiedAmount(AbsenceEngine absenceEngine, Absence absence, 
      AmountType amountType) {
    
    int amount = 0;

    if (absence.justifiedType.name.equals(JustifiedTypeName.nothing)) {
      amount = 0;
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      amount = absenceEngine.workingTime(absence.getAbsenceDate());
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.half_day)) {
      amount = absenceEngine.workingTime(absence.getAbsenceDate()) / 2;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.missing_time) ||
        absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      // TODO: quello che manca va implementato. Occorre persistere la dacisione di quanto manca
      // se non si vogliono fare troppi calcoli.
      if (absence.justifiedMinutes == null) {
        amount = 0;
      } else {
        amount = absence.justifiedMinutes;
      }
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      amount = absence.absenceType.justifiedTime;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
      amount = -1;
    }
    
    if (amountType.equals(AmountType.units)) {
      int work = absenceEngine.workingTime(absence.getAbsenceDate());
      if (work == 0) {
        return 0;
      }
      int result = amount * 100 / work;
      return result;
    } else {
      return amount;
    }
  }
  
  /**
   * Quanto completa l'assenza passata.
   * Se non si riesce a stabilire il tempo di completamento si ritorna un numero negativo.
   * @param person
   * @param date
   * @param absence
   * @param amountType
   * @return
   */
  public int replacingAmount(AbsenceEngine absenceEngine, AbsenceType absenceType, 
      AmountType amountType) {

    //TODO: studiare meglio questa parte... 
    //Casi trattati:
    // 1) tipo completamento unità -> codice completamento un giorno
    //    ex:  89, 09B, 23H7, 25H7 
    // 2) tipo completamento minuti -> codice completamento minuti specificati assenza
    //    ex:  661H1C, 18H1C, 19H1C 
    
    if (absenceType == null) {
      return -1;
    }
    if (amountType.equals(AmountType.units) 
        && absenceType.replacingType.name.equals(JustifiedTypeName.all_day)) {
        return UNIT_REPLACING_AMOUNT; //una unità
    } 
    if (amountType.equals(AmountType.minutes) 
        && absenceType.replacingType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      return absenceType.replacingTime;
    }
    
    return -1;

  }
  
  /**
   * Prova a inferire l'absenceType dal gruppo e dal justifiedTime e specifiedMinutes
   * @param absencePeriod
   * @param absence
   * @return
   */
  public Absence inferAbsenceType(AbsencePeriod absencePeriod, Absence absence, 
      JustifiedType requestedJustifiedType) {

    if (requestedJustifiedType == null || !absencePeriod.takableComponent.isPresent()) {
      return absence;
    }
    
    // Controllo che il tipo sia inferibile
    if (!automaticJustifiedType(absencePeriod.groupAbsenceType).contains(requestedJustifiedType)) {
      return absence;
    }

    //Cerco il codice
    if (requestedJustifiedType.name.equals(JustifiedTypeName.all_day)) {
      for (AbsenceType absenceType : absencePeriod.takableComponent.get().takableCodes) { 
        if (absenceType.justifiedTypesPermitted.contains(requestedJustifiedType)) {
          absence.absenceType = absenceType;
          return absence;
        }
      }
    }
    if (requestedJustifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      
      AbsenceType specifiedMinutes = null;
      for (AbsenceType absenceType : absencePeriod.takableComponent.get().takableCodes) {
        for (JustifiedType absenceTypeJustifiedType : absenceType.justifiedTypesPermitted) {
          if (absenceTypeJustifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
            if (absence.justifiedMinutes != null) {
              absence.absenceType = absenceType;
              return absence; 
            }
            specifiedMinutes = absenceType;
          }
          if (absenceTypeJustifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
            if (absenceType.justifiedTime.equals(absence.justifiedMinutes)) { 
              absence.absenceType = absenceType;
              absence.justifiedType = absenceTypeJustifiedType;
              return absence;
            }
          }
        }
      }
      absence.absenceType = specifiedMinutes;
      return absence; 
    }
    // TODO: quanto manca?
    return absence;
  }
 
  /**
   * 
   * @param hours
   * @param minutes
   * @return
   */
  public Integer getMinutes(Integer hours, Integer minutes) {
    Integer selectedSpecifiedMinutes = null;
    if (hours == null) {
      hours = 0;
    }
    if (minutes == null) {
      minutes = 0;
    }
    selectedSpecifiedMinutes = (hours * 60) + minutes; 
    
    return selectedSpecifiedMinutes;
  }
  
  /**
   * 
   * @param complationComponent
   * @param complationAmount
   * @return
   */
  public Optional<AbsenceType> whichReplacingCode(AbsenceEngine absenceEngine, 
      ComplationComponent complationComponent, LocalDate date, int complationAmount) {
    
    for (Integer replacingTime : complationComponent.replacingCodesDesc.keySet()) {
      int amountToCompare = replacingTime;
//      if (complationComponent.complationAmountType.equals(AmountType.units)) {
//        amountToCompare = absenceEngine.workingTime(LocalDate.now());  
//      }
      if (amountToCompare <= complationAmount) {
        return Optional.of(complationComponent.replacingCodesDesc.get(replacingTime));
      }
    }
    
    return Optional.absent();
  }
  
  /**
   * I gruppi coinvolti nel tipo di assenza.
   * @param absenceType
   * @return
   */
  public Set<GroupAbsenceType> involvedGroup(AbsenceType absenceType) {
    Set<GroupAbsenceType> involvedGroup = Sets.newHashSet();
    for (TakableAbsenceBehaviour takableAbsenceBehaviour : absenceType.takableGroup) {
      involvedGroup.addAll(takableAbsenceBehaviour.groupAbsenceTypes);
    }
    for (TakableAbsenceBehaviour takableAbsenceBehaviour : absenceType.takenGroup) {
      involvedGroup.addAll(takableAbsenceBehaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour complationAbsenceBehaviour : absenceType.complationGroup) {
      involvedGroup.addAll(complationAbsenceBehaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour complationAbsenceBehaviour : absenceType.replacingGroup) {
      involvedGroup.addAll(complationAbsenceBehaviour.groupAbsenceTypes);
    }
    return involvedGroup;
  }
  
  
      
}
