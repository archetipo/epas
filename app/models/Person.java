/**
 * 
 */
package models;



import java.util.Date;

import java.util.List;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Audited
@Table(name = "persons")
public class Person extends Model {
	
	/**
	 * relazione con la tabella delle timbrature
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<Stamping> stampings;
	
	/**
	 * relazione con la tabella di storico YearRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<YearRecap> yearRecap;
	
	/**
	 * relazione con la tabella di storico MonthRecap
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<MonthRecap> monthRecap;
	
	/**
	 * relazione con la tabella dei contratti
	 */
	@Transient
	public Contract contract;
	
	/**
	 * relazione con la tabella delle absence
	 */
	@NotAudited
	@OneToMany(mappedBy="person")
	public List<Absence> absence;
	
	/**
	 * relazione con la tabella di person vacation
	 */
	@OneToMany(mappedBy="person")
	public List <PersonVacation> personVacation;
	
	/**
	 * relazione con la tabella delle tipologie di orario di lavoro
	 */
	@ManyToOne
	@JoinColumn(name="working_time_type_id")
	public WorkingTimeType workingTimeType;
	
	/**
	 * relazione con la tabella delle locazioni degli utenti
	 */
	@NotAudited
	@OneToOne
	@JoinColumn(name="location_id")
	public Location location;
	
	/**
	 * relazione con la tabella delle info di contatto
	 */
	@NotAudited
	@OneToOne
	@JoinColumn(name="contact_data_id")
	public ContactData contactData;
	
	@Required
	public String name;
	
	@Required
	public String surname;
	
	@Column(name = "born_date")
	public Date bornDate;
	
	@Email
	public String email;
		
	/**
	 * Numero di matricola
	 */
	public Integer number;
	
}
