package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.base.BaseModel;



@Entity
@Table(name="person_shift")
public class PersonShift extends BaseModel{

	private static final long serialVersionUID = 651448817233184716L;

	public boolean jolly;
	
	public String description;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id", unique=true, nullable=false, updatable=false)
	public Person person;

	@OneToMany(mappedBy="personShift")
	public List<PersonShiftShiftType> personShiftShiftTypes;
	
	@OneToMany(mappedBy="personShift", fetch=FetchType.LAZY)
	public List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();
}
