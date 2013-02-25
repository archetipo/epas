package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name="shift_type")
public class ShiftType extends Model{

	public String type;
	public String description;
	
	@ManyToMany(mappedBy="shiftTypes")
	public List<PersonShift> personShifts = new ArrayList<PersonShift>();
	
	@OneToMany(mappedBy="shiftType", fetch=FetchType.LAZY)
	public List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();
	
	@OneToMany(mappedBy="type", fetch=FetchType.LAZY)
	public List<ShiftCancelled> shiftCancelled = new ArrayList<ShiftCancelled>();
}
