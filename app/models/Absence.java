package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.db.jpa.Blob;


/**
 * 
 * @author dario
 *
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends BaseModel {
	
	private static final long serialVersionUID = -1963061850354314327L;
	
	@ManyToOne
	@JoinColumn(name = "absence_type_id")
	public AbsenceType absenceType;
	
	
	@ManyToOne(optional=false)
	@JoinColumn(name="personDay_id", nullable=false)
	public PersonDay personDay;
	
	@Column (name = "absence_file", nullable = true )
	public Blob absenceFile;

	@Override
	public String toString() {
		return String.format("Absence[%d] - personDay.id = %d, absenceType.id = %s", 
			id, personDay.id, absenceType.id);
	}
}
