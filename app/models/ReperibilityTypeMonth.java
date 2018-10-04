package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import org.joda.time.YearMonth;
import play.data.validation.Required;

@Entity
@Audited
@Table(name = "reperibility_type_month", uniqueConstraints = @UniqueConstraint(columnNames = {
    "person_reperibility_type_id", "year_month"}))
public class ReperibilityTypeMonth extends MutableModel {

  private static final long serialVersionUID = 4745667554574561506L;

  @Required
  @Column(name = "year_month", nullable = false)
  public YearMonth yearMonth;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_reperibility_type_id", nullable = false)
  public PersonReperibilityType personReperibilityType;

  public boolean approved;
}
