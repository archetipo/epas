/**
 * 
 */
package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;


/**
 * Per ogni giorno della settimana ci sono riportate le informazioni necessarie 
 * all'utilizzo di questa tipologia di orario nel giorno specificato
 * 
 * @author cristian
 * @author dario
 */
@Audited
@Entity
@Table(name = "working_time_type_days")
public class WorkingTimeTypeDay extends BaseModel {

	private static final long serialVersionUID = 4622948996966018754L;

	@Required
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@JoinColumn(name = "working_time_type_id", nullable = false)
	public WorkingTimeType workingTimeType;
	
	@Required
	@Min(1)
	@Max(7)
	public int dayOfWeek;
		
	/**
	 * tempo di lavoro giornaliero espresso in minuti 
	 */
	public Integer workingTime;
	
	/**
	 * tempo di lavoro espresso in minuti che conteggia se possibile usufruire del buono pasto
	 */
	public Integer mealTicketTime;
	
	public Integer breakTicketTime;	
	/**
	 * booleano per controllo se il giorno in questione è festivo o meno
	 */
	public boolean holiday = false;
	/**
	 * tempo di inizio finestra di entrata
	 */
	public Integer timeSlotEntranceFrom;
	/**
	 * tempo di fine finestra di entrata
	 */
	public Integer timeSlotEntranceTo;
	public Integer timeSlotExitFrom;
	public Integer timeSlotExitTo;
	/**
	 * tempo inizio pausa pranzo
	 */
	public Integer timeMealFrom;
	/**
	 * tempo fine pausa pranzo
	 */
	public Integer timeMealTo;
		
	public void setWorkingTime(Integer workingTime)
	{
		if(workingTime==null)
			this.workingTime = 0;
		else
			this.workingTime = workingTime;
	}
	
	public void setBreakTicketTime(Integer breakTicketTime)
	{
		if(breakTicketTime==null)
			this.breakTicketTime = 0;
		else
			this.breakTicketTime = breakTicketTime;
		
		if(this.breakTicketTime < 30)
			this.breakTicketTime = 30;
	}
	
	public void setMealTicketTime(Integer mealTicketTime)
	{
		if(mealTicketTime==null)
			this.mealTicketTime = 0;
		else
			this.mealTicketTime = mealTicketTime;
	}
	
	public void setHoliday(String holiday)
	{
		if(holiday != null && holiday.equals("true"))
			this.holiday = true;
		else
			this.holiday = false;
	}
	
	
	/**
	 * True se è ammesso il calcolo del buono pasto per la persona, false altrimenti (il campo mealTicketTime
	 *  che rappresenta il tempo minimo di lavoro per avere diritto al buono pasto è pari a zero)
	 * @return
	 */
	public boolean mealTicketEnabled() {
		if( holiday )
			return false;
		
		if( mealTicketTime > 0)
			return true;
		else 
			return false;
		
	}
	
	
	@Override
	public String toString() {
		return String.format("WorkingTimeTypeDay[%d] - dayOfWeek = %d, workingTimeType.id = %d, workingTime = %d, mealTicketTime = %d, breakTicketTime = %d, holiday = %s, " +
			"timeSlotEntranceFrom = %d, timeSlotEntranceTo = %d, timeSlotExitFrom = %d, timeSlotExitTo = %d, timeMealFrom = %d, timeMealTo = %d",
			id, dayOfWeek, workingTimeType.id, workingTime, mealTicketTime, breakTicketTime, holiday, timeSlotEntranceFrom, timeSlotEntranceTo, 
			timeSlotExitFrom, timeSlotExitTo, timeMealFrom, timeMealTo);
	}
	
}
