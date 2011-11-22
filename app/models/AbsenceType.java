package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;
/**
 * 
 * @author dario
 *
 */

public class AbsenceType extends Model{
	@Column
	public String code;
	@Column
	public String certificateCode;
	@Column
	public String description;
	@Column
	public Date validFrom;
	@Column
	public Date validTo;
	@Column
	public boolean internalUse;
	@Column
	public boolean multipleUse;
	@Column
	public int justifiedWorkTime;
	@Column
	public boolean mealTicketCalculation;
	@Column
	public boolean ignoreStamping;
	
	@OneToOne
	@JoinColumn(name ="absenceTypeGroup_id")
	public AbsenceTypeGroup absenceTypeGroup;
	@Column
	public int groupValue;
	
}
