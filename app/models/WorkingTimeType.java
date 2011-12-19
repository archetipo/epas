/**
 * 
 */
package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * Tipologia di orario di lavoro relativa ad un singolo giorno
 * 	(per esempio: Normale, Maternità, 50%...)
 * 
 * @author cristian
 * @author dario
 * 
 */
@Entity
@Audited
@Table(name="working_time_types")
public class WorkingTimeType extends Model {
	
	@Required
	public String description;
	
	/**
	 * True se il tipo di orario corrisponde ad un "turno di lavoro"
	 * false altrimenti 
	 */
	public boolean shift = false;
	
	/**
	 * relazione con la tabella persone
	 */
	@OneToMany( mappedBy = "workingTimeType")
	public List<Person> persons;
	
	/**
	 * relazione con la tabella di specifiche di orario di lavoro
	 */
	@OneToMany(mappedBy = "workingTimeType")
	public List<WorkingTimeTypeDay> workingTimeTypeDay;
	

}

