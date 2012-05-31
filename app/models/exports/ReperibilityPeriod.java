/**
 * 
 */
package models.exports;

import org.joda.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import models.Person;

/**
 * Classe di supporto per l'esportazione delle informazioni relative
 * alla reperibilità delle persone.
 * 
 * @author cristian
 *
 */
public class ReperibilityPeriod {

	public final Person person;
	public final LocalDate start;
	public LocalDate end;
	
	public ReperibilityPeriod(Person person, LocalDate start, LocalDate end) {
		this.person = person;
		this.start = start;
		this.end = end;
	}
}
