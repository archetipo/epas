/**
 * 
 */
package controllers;

import java.util.List;

import models.Office;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import dao.OfficeDao;

/**
 * @author cristian
 *
 */
@With(Secure.class)
public class JsonExport extends Controller {

	final static class PersonInfo {
		private final String nome;
		private final String cognome;
		private final String password;
		
		public PersonInfo(String nome, String cognome, String password) {
			this.nome = nome;
			this.cognome = cognome;
			this.password = password;
		}

		public String getNome() { return nome; }
		public String getCognome() { return cognome; }
		public String getPassword() { return password; }

	}
	
	//TODO: serve un permesso più specifico?
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void activePersons() {
		List<Office> offices = OfficeDao.getAllOffices();
		//List<Office> offices = Office.findAll();
		List<Person> activePersons = Person.getActivePersonsInDay(LocalDate.now(), offices, false);
		Logger.debug("activePersons.size() = %d", activePersons.size());
		
		List<PersonInfo> activePersonInfos = FluentIterable.from(activePersons).transform(new Function<Person, PersonInfo>() {
			@Override
			public PersonInfo apply(Person person) {
				return new PersonInfo(
					Joiner.on(" ").skipNulls().join(person.name, person.othersSurnames), 
					Joiner.on(" ").skipNulls().join(person.surname, person.othersSurnames), 
					person.user.password);
			}
		}).toList();
		
		renderJSON(activePersonInfos);
	}
	
}
