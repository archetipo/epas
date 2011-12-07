package models;


import java.sql.Timestamp;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.db.jpa.JPA;

/**
 * 
 * @author dario
 * 
 * Per adesso la classe Year recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista.
 */
@Entity
@Table(name = "year_recaps")
public class YearRecap extends Model{
	
	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	@Column
	public short year;
	@Column
	public int remaining;
	@Column
	public int remainingAp;
	@Column
	public int recg;
	@Column
	public int recgap;
	@Column
	public int overtime;
	@Column
	public int overtimeAp;
	@Column
	public int recguap;
	@Column
	public int recm;
	@Column
	public Timestamp lastModified;

}
