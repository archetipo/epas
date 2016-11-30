package absences;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;

import java.util.List;
import java.util.Set;

public class AbsenceDefinitions {

  public static Integer ONE_HOUR = 60;
  public static Integer TWO_HOUR = 120;
  public static Integer THREE_HOUR = 180;
  public static Integer FOUR_HOUR = 240;
  public static Integer FIVE_HOUR = 300;
  public static Integer SIX_HOUR = 360;
  public static Integer SEVEN_HOUR = 420;

  public static Set<JustifiedTypeName> PERMITTED_NOTHING = 
      ImmutableSet.of(JustifiedTypeName.nothing);
  public static Set<JustifiedTypeName> PERMITTED_SPECIFIED_MINUTES = 
      ImmutableSet.of(JustifiedTypeName.specified_minutes);

  public enum AbsenceTypeDefinition {

    _661H1(0, false, false, PERMITTED_NOTHING, ONE_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H2(0, false, false, PERMITTED_NOTHING, TWO_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H3(0, false, false, PERMITTED_NOTHING, THREE_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H4(0, false, false, PERMITTED_NOTHING, FOUR_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H5(0, false, false, PERMITTED_NOTHING, FIVE_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H6(0, false, false, PERMITTED_NOTHING, SIX_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H7(0, false, false, PERMITTED_NOTHING, SEVEN_HOUR, JustifiedTypeName.absence_type_minutes),
    _661M(0, false, false, PERMITTED_SPECIFIED_MINUTES, null, 
        JustifiedTypeName.absence_type_minutes);

    public Integer justifiedTime;
    public boolean consideredWeekEnd;
    public boolean timeForMealTicket;
    public Set<JustifiedTypeName> justifiedTypeNamesPermitted;
    public Integer replacingTime;
    public JustifiedTypeName replacingType;

    private AbsenceTypeDefinition(Integer justifiedTime, 
        boolean consideredWeekEnd, boolean timeForMealTicket, 
        Set<JustifiedTypeName> justifiedTypeNamesPermitted, 
        Integer replacingTime, JustifiedTypeName replacingType) {
      this.justifiedTime = justifiedTime;
      this.consideredWeekEnd = consideredWeekEnd;
      this.timeForMealTicket = timeForMealTicket;
      this.justifiedTypeNamesPermitted = justifiedTypeNamesPermitted;
      this.replacingTime = replacingTime;
      this.replacingType = replacingType;

    }
  }

  public enum TakableBehaviourDefinition {

    Takable_661(AmountType.minutes, 
        ImmutableSet.of(AbsenceTypeDefinition._661M),
        ImmutableSet.of(AbsenceTypeDefinition._661M),
        1080, TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent);

    public AmountType amountType;
    public Set<AbsenceTypeDefinition> takenCodes;
    public Set<AbsenceTypeDefinition> takableCodes;
    public Integer fixedLimit;
    public TakeAmountAdjustment takableAmountAdjustment;

    private TakableBehaviourDefinition(AmountType amountType,
        Set<AbsenceTypeDefinition> takenCodes, Set<AbsenceTypeDefinition> takableCodes, 
        Integer fixedLimit, TakeAmountAdjustment takableAmountAdjustment) {
      this.amountType = amountType;
      this.takenCodes = takenCodes;
      this.takenCodes = takenCodes;
      this.takableCodes = takableCodes;
      this.fixedLimit = fixedLimit;
      this.takableAmountAdjustment = takableAmountAdjustment;

    }
  }

  public enum ComplationBehaviourDefinition {

    Complation_661(AmountType.minutes, 
        ImmutableSet.of(AbsenceTypeDefinition._661M),
        ImmutableSet.of(AbsenceTypeDefinition._661H1, AbsenceTypeDefinition._661H2,
            AbsenceTypeDefinition._661H3, AbsenceTypeDefinition._661H4, 
            AbsenceTypeDefinition._661H5, AbsenceTypeDefinition._661H6,
            AbsenceTypeDefinition._661H7));

    public AmountType amountType;
    public Set<AbsenceTypeDefinition> complationCodes;
    public Set<AbsenceTypeDefinition> replacingCodes;

    private ComplationBehaviourDefinition(AmountType amountType,
        Set<AbsenceTypeDefinition> complationCodes, Set<AbsenceTypeDefinition> replacingCodes) {
      this.amountType = amountType;
      this.complationCodes = complationCodes;
      this.replacingCodes = replacingCodes;
    }
  }

  public enum GroupAbsenceTypeDefinition {

    Group_661(GroupAbsenceTypePattern.programmed, PeriodType.year, 
        TakableBehaviourDefinition.Takable_661, ComplationBehaviourDefinition.Complation_661,
        null);

    public GroupAbsenceTypePattern pattern;
    public PeriodType periodType;
    public TakableBehaviourDefinition takableAbsenceBehaviour;
    public ComplationBehaviourDefinition complationAbsenceBehaviour;
    public GroupAbsenceTypeDefinition next;

    private GroupAbsenceTypeDefinition(GroupAbsenceTypePattern pattern, 
        PeriodType periodType, TakableBehaviourDefinition takableAbsenceBehaviour,
        ComplationBehaviourDefinition complationAbsenceBehaviour, GroupAbsenceTypeDefinition next) {
      this.pattern = pattern;
      this.periodType = periodType;
      this.takableAbsenceBehaviour = takableAbsenceBehaviour;
      this.complationAbsenceBehaviour = complationAbsenceBehaviour;
      this.next = next;
    }
  }

  public enum WorkingDayDefinition {

    Normal_1(1, 432, false, 360, 30, null, null),
    Normal_2(2, 432, false, 360, 30, null, null),
    Normal_3(3, 432, false, 360, 30, null, null),
    Normal_4(4, 432, false, 360, 30, null, null),
    Normal_5(5, 432, false, 360, 30, null, null),
    Normal_6(6, 0, true, 0, 0, null, null),
    Normal_7(7, 0, true, 0, 0, null, null);

    public Integer dayOfWeek;
    public Integer workingTime;
    public boolean holiday;
    public Integer mealTicketTime;
    public Integer breakTicketTime;
    public Integer ticketAfternoonThreshold;
    public Integer ticketAfternoonWorkingTime;

    private WorkingDayDefinition(Integer dayOfWeek, Integer workingTime, 
        boolean holiday, Integer mealTicketTime, Integer breakTicketTime, 
        Integer ticketAfternoonThreshold, Integer ticketAfternoonWorkingTime) {
      this.dayOfWeek = dayOfWeek;
      this.workingTime = workingTime;
      this.holiday = holiday;
      this.mealTicketTime = mealTicketTime;
      this.breakTicketTime = breakTicketTime;
      this.ticketAfternoonThreshold = ticketAfternoonThreshold;
      this.ticketAfternoonWorkingTime = ticketAfternoonWorkingTime;
    }

  }

  public enum WorkingDefinition {

    Normal(true, 
        ImmutableList.of(WorkingDayDefinition.Normal_1, WorkingDayDefinition.Normal_2, 
            WorkingDayDefinition.Normal_3, WorkingDayDefinition.Normal_4,
            WorkingDayDefinition.Normal_5, WorkingDayDefinition.Normal_6, 
            WorkingDayDefinition.Normal_7));

    public boolean horizontal;
    public List<WorkingDayDefinition> orderedWorkingDayDefinition;

    private WorkingDefinition(boolean horizontal, 
        List<WorkingDayDefinition> orderedWorkingDayDefinition) {
      this.horizontal = horizontal;
      this.orderedWorkingDayDefinition = orderedWorkingDayDefinition;
    }
  }


}
