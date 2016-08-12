package models;

import models.base.BaseModel;
import models.enumerate.LimitType;
import models.enumerate.LimitUnit;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.google.common.collect.Lists;


/**
 * Tabella di decodifica dei codici di competenza.
 *
 * @author dario.
 */
@Audited
@Entity
@Table(name = "competence_codes")
public class CompetenceCode extends BaseModel {

  private static final long serialVersionUID = 9211205948423608460L;

  @NotAudited
  @OneToMany(mappedBy = "competenceCode")
  public List<Competence> competence = Lists.newArrayList();

  @ManyToMany(mappedBy = "competenceCode")
  public List<Person> persons = Lists.newArrayList();
  
  @ManyToOne
  @JoinColumn(name = "competence_code_group_id")
  public CompetenceCodeGroup competenceCodeGroup;

  @Required
  @Unique
  public String code;

  @Column
  public String codeToPresence;

  @Required
  public String description;
  
  public boolean disabled;
  
  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type")
  public LimitType limitType;
  
  @Column(name = "limit_value")
  public Integer limitValue;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "limit_unit")
  public LimitUnit limitUnit;
  

  @Override
  public String toString() {
    return String.format("%s - %s", code, description);
  }
  
  @Override
  public String getLabel() {
    return String.format("%s - %s", this.code, this.description);
  }
  

}
