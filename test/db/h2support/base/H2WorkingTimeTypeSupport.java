package db.h2support.base;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.User;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

import db.h2support.base.AbsenceDefinitions.AbsenceTypeDefinition;
import db.h2support.base.AbsenceDefinitions.ComplationBehaviourDefinition;
import db.h2support.base.AbsenceDefinitions.GroupAbsenceTypeDefinition;
import db.h2support.base.AbsenceDefinitions.TakableBehaviourDefinition;
import db.h2support.base.AbsenceDefinitions.WorkingDayDefinition;
import db.h2support.base.AbsenceDefinitions.WorkingDefinition;

public class H2WorkingTimeTypeSupport {

  private final WorkingTimeTypeDao workingTimeTypeDao;
  
  @Inject
  public H2WorkingTimeTypeSupport(WorkingTimeTypeDao workingTimeTypeDao) {
    this.workingTimeTypeDao = workingTimeTypeDao;
    
  }

  private WorkingTimeTypeDay createWorkingTimeTypeDay(
      WorkingDayDefinition workingDayDefinition, WorkingTimeType workingTimeType) {
    
    WorkingTimeTypeDay workingTimeTypeDay = new WorkingTimeTypeDay();
    workingTimeTypeDay.workingTimeType = workingTimeType;
    workingTimeTypeDay.dayOfWeek = workingDayDefinition.dayOfWeek;
    workingTimeTypeDay.workingTime = workingDayDefinition.workingTime;
    workingTimeTypeDay.holiday = workingDayDefinition.holiday;
    workingTimeTypeDay.mealTicketTime = workingDayDefinition.mealTicketTime;
    workingTimeTypeDay.breakTicketTime = workingDayDefinition.breakTicketTime;
    workingTimeTypeDay.ticketAfternoonThreshold = workingDayDefinition.ticketAfternoonThreshold;
    workingTimeTypeDay.ticketAfternoonWorkingTime = workingDayDefinition.ticketAfternoonWorkingTime;
    workingTimeTypeDay.save();
    return workingTimeTypeDay;
  }
  
  private List<WorkingTimeTypeDay> createWorkingTimeTypeDays(
      List<WorkingDayDefinition> workingDayDefinitions, WorkingTimeType workingTimeType) {
    List<WorkingTimeTypeDay> list = Lists.newArrayList();
    for (WorkingDayDefinition definition : workingDayDefinitions) {
      list.add(createWorkingTimeTypeDay(definition, workingTimeType));
    }
    return list;
  }

  public WorkingTimeType getWorkingTimeType(WorkingDefinition workingDefinition) {
    
    WorkingTimeType workingTimeType = workingTimeTypeDao
        .workingTypeTypeByDescription(workingDefinition.name(), Optional.absent());
    
    if (workingTimeType != null) {
      return workingTimeType;
    }
    
    workingTimeType = new WorkingTimeType();
    workingTimeType.description = workingDefinition.name();
    workingTimeType.horizontal = workingDefinition.horizontal;
    workingTimeType.save();
    workingTimeType.workingTimeTypeDays = 
        createWorkingTimeTypeDays(workingDefinition.orderedWorkingDayDefinition, workingTimeType);
    workingTimeType.refresh();
    return workingTimeType;
  }
  
}
