package models;

import com.google.common.collect.Sets;

import models.base.BaseModel;

import net.sf.oval.constraint.NotNull;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;


/**
 * @author cristian.
 */
@Entity
@Table(name = "badge_readers")
@Audited
public class BadgeReader extends BaseModel {

  private static final long serialVersionUID = -3508739971079270193L;

  @Unique
  @NotNull
  @Required
  public String code;

  public String description;

  public String location;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  public User user;


  @OrderBy("code ASC")
  @OneToMany(mappedBy = "badgeReader")
  public Set<Badge> badges = Sets.newHashSet();

  @Required
  @NotNull
  @ManyToOne
  @JoinColumn(name = "office_owner_id")
  public Office owner;




  public boolean enabled = true;

  @Override
  public String toString() {
    return this.code;
  }
}
