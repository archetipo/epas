package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.envers.Audited;

import play.db.jpa.Model;

/**
 * questa classe estende AbsenceType ereditandone i campi e soprattutto ereditandone l'estensione del Model
 * inoltre definisce quelle assenze di ordine giornaliero
 * @author dario
 *
 */
@Audited
@Entity
@PrimaryKeyJoinColumn(name="absence_type_id")
public class DailyAbsenceType extends AbsenceType{	

	@OneToOne
	@JoinColumn(name="absenceType_id")
	public AbsenceType absenceType;


}
