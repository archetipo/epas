package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import com.mysema.query.SearchResults;

import dao.OfficeDao;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.history.HistoryValue;
import dao.history.UserHistoryDao;

import helpers.Web;

import manager.UserManager;

import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Lists;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Users extends Controller{

  private static final Logger log = LoggerFactory.getLogger(BadgeReaders.class);

  @Inject
  private static SecurityRules rules;

  @Inject
  private static UserDao userDao;
  @Inject
  private static RoleDao roleDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static UsersRolesOfficesDao uroDao;
  @Inject
  private static UserHistoryDao historyDao;
  @Inject
  private static UserManager userManager;

  public static void index() {
    flash.keep();
    list(null);
  }

  /**
   * metodo che renderizza la lista di utenti fisici.
   * @param name il nome utente su cui filtrare.
   */
  public static void list(String name) {
    
    User user = Security.getUser().get();
    SearchResults<?> results =
        userDao.listUsersByOffice(Optional.<String>fromNullable(name), 
            user, true).listResults();   

    render(results, name);
  }

  /**
   * metodo che renderizza la lista di utenti di sistema.
   * @param name il nome utente su cui filtrare.
   */
  public static void systemList(String name) {
    
    User user = Security.getUser().get();
    SearchResults<?> results =
        userDao.listUsersByOffice(Optional.<String>fromNullable(name), user,
            false).listResults();

    render(results, name);
  }

  /**
   * 
   * @param name il nome utente su cui filtrare.
   */
  public static void disabledList(String name) {
    
    SearchResults<?> results =
        userDao.disabledUsersList(Optional.<String>fromNullable(name)).listResults();    

    render(results, name);
  }
  
  
  public static void systemBlank() {
    render();
  }

  public static void blank() {
    render();
  }

  /**
   * metodo che renderizza l'utente e gli userRoleOffice ad esso associati.
   * @param id l'identificativo dell'utente da editare
   */
  public static void edit(Long id) {
    User user = userDao.getUserByIdAndPassword(id, Optional.<String>absent());
    List<UsersRolesOffices> uroList = user.usersRolesOffices;
    render(user, uroList);
  }

  /**
   * metodo che permette il salvataggio delle modifiche di un user.
   * @param user l'utente modificato da salvare
   */
  public static void updateInfo(User user) {
    rules.checkIfPermitted(user);
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", user);
    }
    
    user.save();

    flash.success(Web.msgSaved(User.class));
    edit(user.id);
  }


  /**
   * metodo che salva l'utente sulla base dei parametri recuperati dalla form.
   * @param user l'utente da salvare
   * @param roles la lista di id dei ruoli da assegnare
   * @param offices la lista di id degli uffici su cui assegnare gli uro
   */
  public static void save(@Valid User user, List<Long> roleIds, List<Long> officeIds, 
      boolean systemUser) {
    
    // Binding dei parametri e check permitted // TODO: implementare un binder più furbo
    Set<Office> offices = Sets.newHashSet();
    Set<Role> roles = Sets.newHashSet();
    
    if (officeIds != null) {
      for (Long officeId : officeIds) {
        Office office = officeDao.getOfficeById(officeId);
        notFoundIfNull(office);
        rules.checkIfPermitted(office);
        offices.add(office);
      }
    }
    if (roleIds != null) {
      
      for (Long roleId : roleIds) {
        Role role = roleDao.getRoleById(roleId);
        notFoundIfNull(role);
        roles.add(role);
      }
    }
    
    //Validazione particolare
    if (!Validation.hasErrors()) {
      if (user.password.length() < 5) {
        validation.addError("user.password", "almeno 5 caratteri");
      }
      if (roles.isEmpty()) {
        validation.addError("roleIds", "Almeno un ruolo");
      }
      if (offices.isEmpty()) {
        validation.addError("officeIds", "Almeno una sede");
      }
    }
    
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      if (systemUser)  {
        render("@systemBlank", user, offices, roles);
      } else {
        render("@blank", user, offices, roles);
      }
    }    
    rules.checkIfPermitted(user.person);
    // Save    
    userManager.saveUser(user, offices, roles, false);
    
    flash.success(Web.msgSaved(User.class));
    if (systemUser) {
      systemList(null);
    } else {
      list(null);
    }
  }


  /**
   * metodo che cancella un user recuperato tramite l'id passato come parametro.
   * @param id l'id dell'utente da cancellare
   */
  public static void disable(Long id) {
    User user = userDao.getUserByIdAndPassword(id, Optional.<String>absent());
    notFoundIfNull(user);

    if (user.owner != null) {
      rules.checkIfPermitted(user.owner);
    } else if (user.person != null) {
      rules.checkIfPermitted(user.person.office);
    } else{
      rules.checkIfPermitted();
    }
    
    
    if (user.person != null) {

      flash.error(Messages.get("crud.userDeleteError"));
      edit(id);
    }
    
    List<UsersRolesOffices> uroList = uroDao.getUsersRolesOfficesByUser(user);
    for (UsersRolesOffices uro : uroList) {
      uro.delete();
    }
    try {
      user.disabled = true;
      user.owner = null;
      user.expireDate = LocalDate.now();
      user.save();
             
    } catch(Exception e) {
      flash.error("Utente non eliminabile in quando relazionato con lo storico dell'applicazione.");
      edit(id);
    }

    flash.success(Web.msgDisabled(User.class));
    index();
  }
  
  /**
   * 
   * @param id
   */
  public static void enable(Long id, List<Long> officeIds, List<Long> roleIds, 
      boolean systemUser) {
    User user = userDao.getUserByIdAndPassword(id, Optional.<String>absent());
    notFoundIfNull(user);
    
    Set<Office> offices = Sets.newHashSet();
    Set<Role> roles = Sets.newHashSet();
    
    if (officeIds != null) {
      for (Long officeId : officeIds) {
        Office office = officeDao.getOfficeById(officeId);
        notFoundIfNull(office);
        rules.checkIfPermitted(office);
        offices.add(office);
      }
    }
    if (roleIds != null) {
      
      for (Long roleId : roleIds) {
        Role role = roleDao.getRoleById(roleId);
        notFoundIfNull(role);
        roles.add(role);
      }
    }
    
    //Validazione particolare
    if (!Validation.hasErrors()) {
      if (user.password.length() < 5) {
        validation.addError("user.password", "almeno 5 caratteri");
      }
      if (roles.isEmpty()) {
        validation.addError("roleIds", "Almeno un ruolo");
      }
      if (offices.isEmpty()) {
        validation.addError("officeIds", "Almeno una sede");
      }
    }
    
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      if (systemUser)  {
        render("@systemBlank", user, offices, roles);
      } else {
        render("@blank", user, offices, roles);
      }
    }    

    rules.checkIfPermitted(user.person);
    userManager.saveUser(user, offices, roles, true);
    flash.success(Web.msgSaved(User.class));
    if (systemUser) {
      systemList(null);
    } else {
      list(null);
    }
  }
}
