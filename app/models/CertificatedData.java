package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;


/**
 * Contiene le informazioni relative alla richiesta/risposta di elaborazione
 * dati delle assenze/competenze/buoni mensa inviati al sistema degli attestati
 * del CNR.
 * 
 * @author cristian
 *
 */
@Audited
@Entity()
@Table(name="certificated_data")
public class CertificatedData extends BaseModel {
	
	public int year;
	public int month;
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	@Column(name="cognome_nome")
	public String cognomeNome;
	
	@Column(name="matricola")
	public String matricola;
	
	@Column(name="absences_sent")
	public String absencesSent = null;
	
	@Column(name="competences_sent")
	public String competencesSent = null;
	
	@Column(name="meal_ticket_sent")
	public String mealTicketSent = null;
	
	@Column(name="traininghours_sent")
	public String trainingHoursSent = null;
	
	@Column(name="problems")
	public String problems = null;
	
	@Column(name="is_ok")
	public boolean isOk = false;
	
	public CertificatedData(Person person, String cognomeNome, String matricola, Integer year, Integer month) {
		this.year = year;
		this.month = month;
		this.person = person;
		this.cognomeNome = cognomeNome;
		this.matricola = matricola;
	}
	
	/*
	public String getCognomeNome() { return cognomeNome; }
	public String getMatricola() { return matricola; }
	public boolean getOk() {return ok; }
	public void setOk(boolean ok) { this.ok = ok; }
	public String getAbsencesSent() { return absencesSent; }
	public void setAbsencesSent(String absencesSent) { this.absencesSent = absencesSent; }
	public String getCompetencesSent() { return competencesSent; }
	public void setCompetencesSent(String compentecesSent) { this.competencesSent = compentecesSent; }
	public Integer getMealTicketSent() { return mealTicketSent; }
	public void setMealTicketSent(Integer mealTicketSent) { this.mealTicketSent = mealTicketSent; }
	public String getProblems() {	return problems; }
	public void setProblems(String errors) { this.problems = errors; }
	*/
}