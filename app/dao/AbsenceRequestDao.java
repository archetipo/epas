package dao;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.query.QAbsenceRequest;
import org.joda.time.LocalDateTime;

import java.util.List;

/**
 * Dao per l'accesso alle richiesta di assenza.
 * 
 * @author cristian
 *
 */
public class AbsenceRequestDao extends DaoBase {

  @Inject
  AbsenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Lista delle richiesta di assenza per persona e data.
   * 
   * @param person La persona della quale recuperare le richieste di assenza
   * @param fromDate La data iniziale dell'intervallo temporale da considerare
   * @param toDate La data finale dell'intervallo temporale da considerare (opzionale)
   * @param absenceRequestType Il tipo di richiesta di assenza specifico
   * @return La lista delle richieste di assenze sull'intervallo e la persona specificati.
   */
  public List<AbsenceRequest> findByPersonAndDate(Person person,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType, boolean active) {

    Preconditions.checkNotNull(person);
    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder(absenceRequest.person.eq(person)
        .and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType)));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    if (active) {
      conditions.and(absenceRequest.flowEnded.eq(false));
    } else {
      conditions.and(absenceRequest.flowEnded.eq(true));
    }
    JPQLQuery query = getQueryFactory().from(absenceRequest)
        .where(conditions);
    return query.list(absenceRequest);
  }

  /**
   * Lista di richieste da approvare per ruolo, data e tipo.
   * 
   * @param uros la lista degli user_role_office associati alla persona pr cui si cercano le 
   *     richieste da approvare.
   * @param fromDate la data da cui cercare
   * @param toDate (opzionale) la data entro cui cercare
   * @param absenceRequestType il tipo di richiesta da cercare
   * @return La lista delle richieste di assenza da approvare per il ruolo passato. 
   */
  public List<AbsenceRequest> findRequestsToApprove(
      List<UsersRolesOffices> uros,
      LocalDateTime fromDate, Optional<LocalDateTime> toDate, 
      AbsenceRequestType absenceRequestType) {

    Preconditions.checkNotNull(fromDate);

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;

    BooleanBuilder conditions = new BooleanBuilder();

    for (UsersRolesOffices uro : uros) {
      if (uro.role.name.equals(Role.PERSONNEL_ADMIN)) {
        personnelAdminQuery(conditions);
      } else if (uro.role.name.equals(Role.SEAT_SUPERVISOR)) {
        seatSupervisorQuery(conditions);
      } else if (uro.role.name.equals(Role.GROUP_MANAGER)) {
        managerQuery(conditions);
      }
    }
    conditions.and(absenceRequest.startAt.after(fromDate))
        .and(absenceRequest.type.eq(absenceRequestType));
    if (toDate.isPresent()) {
      conditions.and(absenceRequest.endTo.before(toDate.get()));
    }
    JPQLQuery query = getQueryFactory()
        .from(absenceRequest).where(conditions);
    return query.list(absenceRequest);
  }

  private void seatSupervisorQuery(BooleanBuilder condition) {
    
    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(condition.and(absenceRequest.officeHeadApprovalRequired.isTrue()
          .and(absenceRequest.officeHeadApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse())))));
    } else {
      condition.and(absenceRequest.officeHeadApprovalRequired.isTrue()
          .and(absenceRequest.officeHeadApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }
    
  }

  private void personnelAdminQuery(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(absenceRequest.administrativeApprovalRequired.isTrue()
          .and(absenceRequest.administrativeApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    } else {
      condition.and(absenceRequest.administrativeApprovalRequired.isTrue()
          .and(absenceRequest.administrativeApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }
    
  }

  private void managerQuery(BooleanBuilder condition) {

    final QAbsenceRequest absenceRequest = QAbsenceRequest.absenceRequest;
    if (condition.hasValue()) {
      condition.or(absenceRequest.managerApprovalRequired.isTrue()
          .and(absenceRequest.managerApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    } else {
      condition.and(absenceRequest.managerApprovalRequired.isTrue()
          .and(absenceRequest.managerApproved.isNull()
              .and(absenceRequest.flowStarted.isTrue().and(absenceRequest.flowEnded.isFalse()))));
    }
    
  }
}
