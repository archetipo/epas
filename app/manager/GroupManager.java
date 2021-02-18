/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager;


import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.PersonReperibilityDayDao;
import dao.RoleDao;
import dao.ShiftDao;
import dao.UsersRolesOfficesDao;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.dto.SeatSituationDto;
import models.flows.Affiliation;
import models.flows.Group;
import org.assertj.core.util.Lists;


/**
 * Manager per la gestione dei gruppi di persone.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 *
 */
@Slf4j
public class GroupManager {

  private final RoleDao roleDao;
  private final UsersRolesOfficesDao uroDao;
  private final GroupDao groupDao;

  /**
   * Injection.
   *
   * @param roleDao il dao sui ruoli
   * @param uroDao il dao sugli usersRolesOffices
   * @param groupDao il dao sui gruppi
   */
  @Inject
  public GroupManager(RoleDao roleDao, UsersRolesOfficesDao uroDao, 
      GroupDao groupDao) {
    this.roleDao = roleDao;
    this.uroDao = uroDao;
    this.groupDao = groupDao;
  }

  /**
   * Metodo di utilità per creare il ruolo manager da associare al responsabile del 
   * gruppo di lavoro.
   *
   * @param office la sede cui appartiene il ruolo
   * @param group il gruppo di cui fa parte il manager
   * @param uro l'user role office da creare
   */
  public void createManager(Office office, Group group, UsersRolesOffices uro) {
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    Optional<UsersRolesOffices> uroPresent = 
        uroDao.getUsersRolesOffices(group.manager.user, role, group.office);
    if (uroPresent.isPresent()) {
      return;
    }
    uro.office = office;
    uro.role = role;
    uro.user = group.manager.user;
    uro.save();   
    log.debug("Creato ruolo {} per l'utente {}", role.name, uro.user.person.fullName());
  }

  /**
   * Metodo che elimina il ruolo manager al responsabile di gruppo durante la fase di 
   * eliminazione del gruppo.
   *
   * @param group il gruppo di cui si vuole rimuvoere il ruolo di manager
   * @return true se è stato eliminato il manager del gruppo, false altrimenti.
   */
  public boolean deleteManager(Group group) {
    Role role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    List<Group> managerGroups = groupDao.groupsByManager(Optional.fromNullable(group.manager));
    if (managerGroups.size() > 1) {
      log.debug("Non elimino il ruolo perchè {} ha almeno un altro gruppo su cui è responsabile", 
          group.manager.fullName());
      return true;
    }
    Optional<UsersRolesOffices> uro = 
        uroDao.getUsersRolesOffices(group.manager.user, role, group.manager.office);
    if (uro.isPresent()) {
      uro.get().delete();
      log.debug("Eliminato ruolo {} per l'utente {}", 
          uro.get().role.name, uro.get().user.person.fullName());
      return true;
    }
    return false;
  }
  
  /**
   * Inserisce ed elimina le affiliazioni ad un gruppo con data 
   * corrente in funzione della lista delle persone passate.
   */
  public void updatePeople(Group group, Set<Person> people) {
    log.info("current people = {}, new people = {}", group.getPeople(), people);
    if (people == null) {
      people = Sets.newHashSet();
    }
    val toDisable = Sets.difference(Sets.newHashSet(group.getPeople()), people);
    log.info("Person toDisable = {}", toDisable);
    val currentAffiliationsToDisable = 
        group.getAffiliations().stream()
          .filter(a -> !a.isActive() && toDisable.contains(a.getPerson()))
          .collect(Collectors.toSet());
    currentAffiliationsToDisable.stream().forEach(a ->  {      
      a.setEndDate(LocalDate.now());
      a.save();
      log.info("Disabilita associazione di {} al gruppo {}", 
          a.getPerson().getFullname(), a.getGroup().getName());
    });
    val toInsert = Sets.difference(people, Sets.newHashSet(group.getPeople()));
    toInsert.forEach(person -> {
      val affiliation = new Affiliation();
      affiliation.setPerson(person);
      affiliation.setGroup(group);
      affiliation.setBeginDate(LocalDate.now());
      affiliation.save();
      log.info("Inserita nuova associazione tra {} al gruppo {}", 
          person.getFullname(), group.getName());
    });
  }
  
  /**
   * Genera il dto contenente le liste dei possibili modificatori dello stato delle info
   * della persona passata come parametro.
   * @param person la persona di cui conoscere tutti i possibili modificatori delle proprie info
   * @return il dto contenente tutte le informazioni degli utenti che possono in qualche modo
   *     modificare lo stato delle informazioni della persona passata come parametro.
   */
  public Map<Role, List<User>> createOrganizationChart(Person person) {
    
    Map<Role, List<User>> map = Maps.newHashMap();
    for (Role role: roleDao.getAll()) {
      if (role.name.equals(Role.BADGE_READER)) {
        continue;
      }
      if (role.name.equals(Role.EMPLOYEE)) {
        continue;
      }
      if (role.name.equals(Role.GROUP_MANAGER)) {
        if (!groupDao.myGroups(person).isEmpty()) {
          map.put(role, groupDao.myGroups(person).stream()
              .map(g -> g.manager.user).collect(Collectors.toList()));
        } else {
          map.put(role, Lists.emptyList());
        }
      }
      if (role.name.equals(Role.MEAL_TICKET_MANAGER)) {
        map.put(role, getMealTicketsManager(person.office));
      }
      if (role.name.equals(Role.PERSON_DAY_READER)) {
        continue;
      }
      if (role.name.equals(Role.PERSONNEL_ADMIN)) {
        map.put(role, getPersonnelAdminInSeat(person.office));
      }
      if (role.name.equals(Role.PERSONNEL_ADMIN_MINI)) {
        map.put(role, getPersonnelAdminMiniInSeat(person.office));
      }
      if (role.name.equals(Role.REGISTRY_MANAGER)) {
        map.put(role, getRegistryManager(person.office));
      }
      if (role.name.equals(Role.REPERIBILITY_MANAGER)) {
        if (!person.reperibility.isEmpty()) {
          map.put(role, person.reperibility.stream()
              .map(pr -> pr.personReperibilityType.supervisor.user).collect(Collectors.toList()));
        } 
      }
      if (role.name.equals(Role.REST_CLIENT)) {
        continue;
      }
      if (role.name.equals(Role.SEAT_SUPERVISOR)) {
        map.put(role, getSeatSupervisor(person.office));
      }
      if (role.name.equals(Role.SHIFT_MANAGER)) {
        if (!person.personShifts.isEmpty()) {
          map.put(role, person.personShifts.stream()
              .flatMap(ps -> ps.personShiftShiftTypes.stream()
                  .map(psst -> psst.shiftType.shiftCategories.supervisor.user))
              .collect(Collectors.toList()));              
        } 
      }
      if (role.name.equals(Role.TECHNICAL_ADMIN)) {
        map.put(role, getTechnicalAdminInSeat(person.office));
      }
    }
        
    return map;
  }
  
  private List<User> getTechnicalAdminInSeat(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.TECHNICAL_ADMIN), office);
  }
  
  private List<User> getPersonnelAdminInSeat(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.PERSONNEL_ADMIN), office);
  }
  
  private List<User> getSeatSupervisor(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.SEAT_SUPERVISOR), office);
  }
  
  private List<User> getMealTicketsManager(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.MEAL_TICKET_MANAGER), office);
  }
  
  private List<User> getRegistryManager(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao.getRoleByName(Role.REGISTRY_MANAGER), office);
  }
  
  private List<User> getPersonnelAdminMiniInSeat(Office office) {
    return uroDao.getUsersWithRoleOnOffice(roleDao
        .getRoleByName(Role.PERSONNEL_ADMIN_MINI), office);
  }

}

