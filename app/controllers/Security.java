package controllers;

import java.util.List;
import java.util.Set;

import play.Logger;
import play.cache.Cache;
import play.db.jpa.GenericModel.JPAQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import models.Permission;
import models.Person;

public class Security extends Secure.Security {
	
	public final static String VIEW_PERSON_LIST = "viewPersonList";
	public final static String INSERT_AND_UPDATE_PERSON = "insertAndUpdatePerson";
	public final static String DELETE_PERSON = "deletePerson";
	public final static String INSERT_AND_UPDATE_STAMPING = "insertAndUpdateStamping";
	public final static String INSERT_AND_UPDATE_PASSWORD = "insertAndUpdatePassword";
	public final static String INSERT_AND_UPDATE_WORKINGTIME = "insertAndUpdateWorkingTime";
	public final static String INSERT_AND_UPDATE_ABSENCE = "insertAndUpdateAbsence";
	public final static String INSERT_AND_UPDATE_CONFIGURATION = "insertAndUpdateConfiguration";
	public final static String INSERT_AND_UPDATE_ADMINISTRATOR = "insertAndUpdateAdministrator";
	public final static String INSERT_AND_UPDATE_COMPETENCES = "insertAndUpdateCompetences";
	public final static String INSERT_AND_UPDATE_VACATIONS = "insertAndUpdateVacations";
	public final static String VIEW_PERSONAL_SITUATION ="viewPersonalSituation";
	
	private final static String PERMISSION_CACHE_PREFIX = "permission.";
		
	static boolean authenticate(String username, String password) {
		Person person = Person.find("SELECT p FROM Person p where username = ? and password = md5(?)", username, password).first();
		
		if(person != null){
			Cache.set(username, person, "30mn");
			Cache.set(PERMISSION_CACHE_PREFIX + username, person.getAllPermissions(), "30mn");
			Cache.set("personId", person.id, "30mn");
			            
            flash.success("Welcome, " + person.name + person.surname);
            Logger.info("person %s successfully logged in", person.username);
            //Logger.info("Permission list for %s %s: %s", person.name, person.surname, person.permissions);
			return true;
		}
		
        // Oops
        flash.put("username", username);
        flash.error("Login failed");
        return false;
    }
	
	
	static boolean check(String profile) {
		String username = connected();
		Logger.trace("checking permission %s for user %s", profile, username);
		
		for (Permission permission : getPersonAllPermissions(username)) {
			if (permission.description.equals(profile)) {
				return true;
			}
		}
		return false;
    }    
	
	private static Person getPerson(String username){
		Person person = Cache.get(username, Person.class);
		if(person == null){
			person = Person.find("byUsername", username).first();
			Cache.set(username, person, "30mn");
			Cache.set(PERMISSION_CACHE_PREFIX + username, person.getAllPermissions(), "30mn");
		}
		return person;
	}
	
	private static Set<Permission> getPersonAllPermissions(String username) {
		Person person = getPerson(username);
		Set<Permission> permissions = Cache.get(PERMISSION_CACHE_PREFIX + username, Set.class);
		if (permissions == null) {
			person.refresh();
			permissions = person.getAllPermissions();
			Cache.set(PERMISSION_CACHE_PREFIX + username, permissions, "30mn");
		}
		return permissions;
	}
	
	public static Person getPerson() {
		return getPerson(connected());
	}
	
	public static Set<Permission> getPersonAllPermissions() {
		return getPersonAllPermissions(connected());
	}
}
