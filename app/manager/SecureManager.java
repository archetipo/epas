package manager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import models.Office;
import models.Role;
import models.User;

import java.util.Set;
import java.util.stream.Collectors;

public class SecureManager {

  /**
   * @return la lista degli uffici permessi per l'utente user passato come parametro.
   */
  private Set<Office> getOfficeAllowed(User user, ImmutableList<String> rolesNames) {

    Preconditions.checkNotNull(user);
    Preconditions.checkState(user.isPersistent());

    return user.usersRolesOffices.stream().filter(uro -> rolesNames.contains(uro.role.name))
        .map(uro -> uro.office).distinct().collect(Collectors.toSet());
  }

  /**
   * Le sedi per le quali l'utente dispone di almeno il ruolo di sola lettura.
   */
  public Set<Office> officesReadAllowed(User user) {

    ImmutableList<String> rolesNames = ImmutableList.of(
        Role.ADMIN,
        Role.DEVELOPER,
        Role.PERSONNEL_ADMIN,
        Role.PERSONNEL_ADMIN_MINI);

    return getOfficeAllowed(user, rolesNames);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di scrittura.
   */
  public Set<Office> officesWriteAllowed(User user) {

    ImmutableList<String> rolesNames = ImmutableList.of(
        Role.ADMIN,
        Role.DEVELOPER,
        Role.PERSONNEL_ADMIN);

    return getOfficeAllowed(user, rolesNames);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di badge reader.
   */
  public Set<Office> officesBadgeReaderAllowed(User user) {

    ImmutableList<String> roles = ImmutableList.of(Role.BADGE_READER);

    return getOfficeAllowed(user, roles);
  }

  /**
   * Le sedi per le quali l'utente dispone del ruolo di amministratore di sistema.
   */
  public Set<Office> officesSystemAdminAllowed(User user) {

    ImmutableList<String> roles =
        ImmutableList.of(Role.ADMIN, Role.DEVELOPER, Role.PERSONNEL_ADMIN);

    return getOfficeAllowed(user, roles);

  }

  public Set<Office> officesTecnicalAdminAllowed(User user) {
    ImmutableList<String> roles = ImmutableList.of(Role.TECNICAL_ADMIN, Role.DEVELOPER);

    return getOfficeAllowed(user, roles);
  }

  /**
   * @param user L'utente per il quale restituire la lista delle sedi
   * @return un Set contenente tutti gli uffici sul quale si ha un ruolo qualsiasi.
   */
  public Set<Office> ownOffices(User user) {
    ImmutableList<String> roles = ImmutableList.of(
        Role.DEVELOPER, Role.ADMIN, Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI, Role.EMPLOYEE,
        Role.BADGE_READER, Role.REST_CLIENT, Role.TECNICAL_ADMIN, Role.SHIFT_MANAGER,
        Role.REPERIBILITY_MANAGER, Role.TECNICAL_ADMIN);

    return getOfficeAllowed(user, roles);
  }
}
