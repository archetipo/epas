package manager;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import controllers.Security;
import dao.OfficeDao;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

public class OfficeManager {

	@Inject
	public OfficeDao officeDao;
	
	@Inject
	public UsersRolesOfficesDao usersRolesOfficesDao;
	
	/**
	 * 
	 * @param area
	 * @param name
	 * @param contraction
	 */
	public static void saveInstitute(Office office, Office area, String name, String contraction){

		office.name = name;
		office.contraction = contraction;
		office.office = area;
		office.save();
	}	

	/**
	 * 
	 * @param name
	 * @param address
	 * @param code
	 * @param date
	 * @param institute
	 */
	public static void saveSeat(Office office, String name, String address, String code, String date, Office institute){

		office.name = name;
		office.address = address;
		office.code = getInteger(code);
		office.joiningDate = getLocalDate(date);
		office.office = institute;
		office.save();
	}

	/**
	 * 
	 * @param office
	 * @param name
	 * @param address
	 * @param code
	 * @param date
	 */
	public static void updateSeat(Office office, String name, String address, String code, String date){
		office.name = name;
		office.address = address;
		office.code = getInteger(code);
		office.joiningDate = getLocalDate(date);

		office.save();

	}
	
	/**
	 * 
	 * @param permission
	 * @return true se permission è presente in almeno un office del sottoalbero, radice compresa, 
	 * false altrimenti
	 */
	public boolean isRightPermittedOnOfficeTree(Office office, Role role) {
		
		if(checkUserRoleOffice(Security.getUser().get(), role, office))
			return true;
		
		for(Office subOff : office.subOffices) {
			
			if(isRightPermittedOnOfficeTree(subOff, role))
				return true;
		}
		
		return false;
	}
	
	/**
	 * 	Check del Permesso
	 */
	private boolean checkUserRoleOffice(User user, Role role, Office office) {
		
		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOffices(user, role, office);
				
		if(!uro.isPresent())
			return false;
		else
			return true;		
		
	}
	
	/**
	 * Assegna i diritti agli amministratori. Da chiamare successivamente alla creazione.
	 * @param office
	 */
	public void setPermissionAfterCreation(Office office) {

		User userLogged = Security.getUser().get();
		User admin = UserDao.getUserByUsernameAndPassword("admin", Optional.<String>absent());

		Role roleAdmin = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		Role roleAdminMini = RoleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);

		setUro(admin, office, roleAdmin);
		setUro(userLogged, office, roleAdmin);

		List<Office> officeList = Lists.newArrayList();
		if(isInstitute(office)) {
			officeList.add(getSuperArea(office));
		}
		if(isSeat(office)) {
			officeList.add(getSuperArea(office));
			officeList.add(getSuperInstitute(office));
		}

		for(Office superOffice : officeList) {

			//Attribuire roleAdminMini a coloro che hanno roleAdminMini su il super office
			for(User user : getUserByOfficeAndRole(superOffice, roleAdminMini)) {

				setUroIfImprove(user, office, roleAdminMini, true);
			}

			//Attribuire roleAdmin a coloro che hanno roleAdmin su area il super office
			for(User user : getUserByOfficeAndRole(superOffice, roleAdmin)) {

				setUroIfImprove(user, office, roleAdmin, true);
			}

		}

	}
	
	/**
	 * Ritorna la lista degli utenti che hanno ruolo role nell'ufficio office
	 * @param office
	 * @param role
	 * @return
	 */
	public static List<User> getUserByOfficeAndRole(Office office, Role role) {
		
		List<User> userList = Lists.newArrayList();
		
		for(UsersRolesOffices uro : office.usersRolesOffices) {
			
			if(uro.role.id.equals(role.id)) {
				
				userList.add(uro.user);
			}
		}
		return userList;
	}

	/**
	 * Setta il ruolo per la tripla <user,office,role>. Se non esiste viene creato.
	 * Se ifImprove è false il precedente ruolo viene sovrascritto. Se ifImprove è true 
	 * il ruolo viene sovrascritto solo se assegna maggiori diritti rispetto al precedente. 
	 * @param user
	 * @param office
	 * @param role
	 * @param ifImprove
	 * @return true se il ruolo è stato assegnato, false se il ruolo non è stato assegnato (perchè peggiorativo)
	 */
	public Boolean setUroIfImprove(User user, Office office, Role role, boolean ifImprove) {

		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);
			
		if( !uro.isPresent() || !ifImprove ) {

			setUro(user, office, role);
			return true;
		}

		if(ifImprove) {

			/* implementare la logica di confronto fra ruolo */
			Role previous = uro.get().role;

			if(previous.name.equals(Role.PERSONNEL_ADMIN_MINI)) {

				setUro(user, office, role);
				return true;
			}

		}		
		return false;		 
	}

	/**
	 * 
	 * @param user
	 * @param office
	 * @param role
	 */
	public void setUro(User user, Office office, Role role){

		UsersRolesOffices newUro = null;
		Optional<UsersRolesOffices> uro = usersRolesOfficesDao.getUsersRolesOfficesByUserAndOffice(user, office);

		if(!uro.isPresent()) {

			newUro = new UsersRolesOffices();
			newUro.user = user;
			newUro.office = office;
			newUro.role = role;
			newUro.save();
		}
		else{
			newUro = uro.get();
			newUro.role = role;
			newUro.save();
		}		

	}
	
	 /**
     * Ritorna la lista di tutte le sedi gerarchicamente sotto a Office
     * @return
     */
    public static List<Office> getSubOfficeTree(Office o) {
    	
    	List<Office> officeToCompute = new ArrayList<Office>();
    	List<Office> officeComputed = new ArrayList<Office>();
    	
    	officeToCompute.add(o);
    	while(officeToCompute.size() != 0) {
    		
    		Office office = officeToCompute.get(0);
    		officeToCompute.remove(office);
    		
    		for(Office remoteOffice : office.subOffices) {

    			officeToCompute.add((Office)remoteOffice);
    		}
    		
    		officeComputed.add(office);
    	}
    	return officeComputed;
    }

	/**
	 * Ritorna l'area padre se office è un istituto o una sede
	 * @return
	 */
	public static Office getSuperArea(Office office) {

		if(isSeat(office))
			return office.office.office;

		if(isInstitute(office))
			return office.office;

		return null;
	}

	/**
	 * Ritorna l'istituto padre se this è una sede
	 * @return 
	 */
	public static Office getSuperInstitute(Office office) {

		if(!isSeat(office))
			return null;
		return office.office;
	}

	/**
	 * Area livello 0
	 * @return true se this è una Area, false altrimenti
	 */
	public static boolean isArea(Office office) {

		if(office.office != null) 
			return false;

		return true;
	}

	/**
	 * Istituto livello 1
	 * @return true se this è un Istituto, false altrimenti
	 */
	public static boolean isInstitute(Office office) {

		if(isArea(office))
			return false;

		if(office.office.office != null)
			return false;

		return true;
	}

	/**
	 * Sede livello 2
	 * @return
	 */
	public static boolean isSeat(Office office) {

		if(isArea(office))
			return false;

		if(isInstitute(office))
			return false;

		return true;

	}


	public static Integer getInteger(String parameter)
	{
		try{
			Integer i = Integer.parseInt(parameter);
			return i;

		}catch(Exception e)
		{
			return null;
		}
	}

	public static LocalDate getLocalDate(String parameter)
	{
		try{
			LocalDate date = new LocalDate(parameter);
			return date;

		}catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * 
	 * @param name
	 * @param contraction
	 * @return un messaggio che descrive se ci sono stati errori nel passaggio dei parametri name e contraction
	 */
	public String checkIfExists(Office office, String name, String contraction){
		String result = "";

		if( office != null ) {
			result = "Esiste gia' un istituto con nome "+name+" operazione annullata.";
		}

		office = officeDao.getOfficeByContraction(contraction);
		if( office != null ) {
			result = "Esiste gia' un istituto con sigla "+contraction+ ", operazione annullata.";
		}
		return result;
	}

	/**
	 * 
	 * @param code
	 * @param date
	 * @return un messaggio che descrive se ci sono stati errori nel passaggio dei parametri code e date
	 */
	public static String checkIfExistsSeat(String code, String date){
		String result = "";
		//errore campo data
		if(getLocalDate(date)==null){
			result = "Errore nell'inserimento del campo data. Valorizzare correttamente tutti i parametri.";
		}
		//errore campo sede
		if(OfficeManager.getInteger(code)==null){
			result = "Errore nell'inserimento del campo codice sede. Valorizzare correttamente tutti i parametri.";
		}
		return result;
	}
}
