package models;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import models.base.BaseModel;
import models.enumerate.ShiftSlot;
import models.enumerate.ShiftTroubles;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

@Entity
@Audited
@Table(name = "person_shift_days")
public class PersonShiftDay extends BaseModel {

  private static final long serialVersionUID = -2441219908198684741L;

  // morning or afternoon slot
  @Column(name = "shift_slot")
  @Enumerated(EnumType.STRING)
  public ShiftSlot shiftSlot;

  // shift date

  public LocalDate date;

  @ManyToOne
  @JoinColumn(name = "shift_type_id")
  public ShiftType shiftType;

  @ManyToOne
  @JoinColumn(name = "person_shift_id", nullable = false)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  public PersonShift personShift;

  //  Nuova relazione con gli errori associati ai personShiftDay
  @OneToMany(mappedBy = "personShiftDay", cascade = CascadeType.REMOVE)
  public Set<PersonShiftDayInTrouble> troubles = Sets.newHashSet();


  @Transient
  public String getSlotTime() {
    String timeFormatted = "HH:mm";
    switch (shiftSlot) {
      case MORNING:
        return shiftType.shiftTimeTable.startMorning.toString(timeFormatted) + " - "
            + shiftType.shiftTimeTable.endMorning.toString(timeFormatted);

      case AFTERNOON:
        return shiftType.shiftTimeTable.startAfternoon.toString(timeFormatted) + " - "
            + shiftType.shiftTimeTable.endAfternoon.toString(timeFormatted);

      default:
        return null;
    }
  }

  @Transient
  public boolean hasError(ShiftTroubles trouble) {
    return troubles.stream().anyMatch(error -> error.cause == trouble);
  }

  @PostPersist
  @PostUpdate
  @PostRemove
  private void onChange() {
    final Optional<ShiftTypeMonth> monthStatus = shiftType.monthStatusByDate(date);
    if (monthStatus.isPresent()) {
      monthStatus.get().save();
    } else {
      ShiftTypeMonth newStatus = new ShiftTypeMonth();
      newStatus.yearMonth = new YearMonth(date);
      newStatus.shiftType = shiftType;
      newStatus.save();
    }
  }
}
