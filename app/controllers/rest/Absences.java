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

package controllers.rest;

import cnr.sync.dto.AbsenceAddedRest;
import cnr.sync.dto.AbsenceRest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.rest.v2.Persons;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.AbsenceManager;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;
import security.SecurityRules;

/**
 * Controller per la gestione/consultazione della assenze via REST.
 */
@Slf4j
@With(Resecure.class)
public class Absences extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static AbsenceManager absenceManager;
  @Inject
  static AbsenceService absenceService;
  @Inject
  static AbsenceTypeDao absenceTypeDao;
  @Inject
  static AbsenceComponentDao absenceComponentDao;
  @Inject
  static ObjectMapper mapper;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  private static SecurityRules rules;

  /**
   * Restituisce un Json con la lista delle assenze corrispondenti ai parametri passati.
   *
   * @param email email della persona di cui cercare le assenze
   * @param begin data di inizio delle assenze da cercare
   * @param end data di fine della assenze da cercare
   */
  @BasicAuth
  public static void absencesInPeriod(Long id, String eppn, String email, Long personPerseoId,
      String fiscalCode, LocalDate begin, LocalDate end) {
    Person person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCode(
            id, eppn, email, personPerseoId, fiscalCode).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.office);

    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    List<AbsenceRest> absences = FluentIterable.from(absenceDao.getAbsencesInPeriod(
        Optional.fromNullable(person), begin, Optional.fromNullable(end), false))
        .transform(new Function<Absence, AbsenceRest>() {
          @Override
          public AbsenceRest apply(Absence absence) {
            return AbsenceRest.build(absence);
          }
        }).toList();
    renderJSON(absences);
  }

  /**
   * Inserisce un assenza con il codice specificato in tutti i giorni compresi nel periodo indicato
   * (saltando i feriali se l'assenza non è prendibile nei giorni feriali).
   * Restituisce un Json con la lista dei giorni in cui è stata inserita l'assenza ed gli effetti
   * codici inseriti.
   * Il campo eppn se passato viene usato come preferenziale per cercare la persona.
   *
   * @param eppn eppn della persona di cui inserire l'assenza
   * @param email email della persona di cui inserire l'assenza
   * @param personPerseoId perseoId della persona di cui inserire l'assenza
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param hours (opzionale) le ore giustificate in caso di assenza oraria
   * @param minutes (opzionale) i muniti giustificati in caso di assenza oraria
   */
  @BasicAuth
  public static void insertAbsence(
      Long id, String eppn, String email, Long personPerseoId, String fiscalCode,
      String absenceCode, LocalDate begin, 
      LocalDate end, Integer hours, Integer minutes) {
    Person person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCode(id, 
            eppn, email, personPerseoId, fiscalCode).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.office);
    log.debug("Richiesto inserimento assenza via REST -> eppn = {}, email = {}, absenceCode = {}, "
        + "begin = {}, end = {}, hours = {}, minutes = {}", 
        eppn, email, absenceCode, begin, end, hours, minutes);

    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    try {
      val absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
      if (!absenceType.isPresent()) {
        JsonResponse.badRequest("Tipo assenza non valido");
      }

      val justifiedType = absenceType.get().justifiedTypesPermitted.iterator().next();
      val groupAbsenceType = absenceType.get().defaultTakableGroup(); 
      val report = absenceService.insert(person, groupAbsenceType, begin, end, absenceType.get(),
          justifiedType, hours, minutes, false, absenceManager);

      val list = report.insertTemplateRows.stream()
          .map(AbsenceAddedRest::build)
          .collect(Collectors.toList());

      absenceManager.saveAbsences(report, person, begin, null, justifiedType, groupAbsenceType);

      renderJSON(list);
    } catch (Exception ex) {
      log.warn("Eccezione durante inserimento assenza via REST", ex);
      JsonResponse.badRequest("Errore nei parametri passati al server");
    }


  }

  /**
   * Verifica se è possibile prendere il tipo di assenza passato nel periodo indicato.
   * La persona viene individuata tramite il suo indirizzo email.
   * Il campo eppn se passato viene usato come preferenziale per cercare la persona. 
   *
   * @param eppn eppn della persona di cui inserire l'assenza
   * @param email email della persona di cui inserire l'assenza
   * @param personPerseoId l'identificativo della persona per cui inserire l'assenza
   * @param absenceCode il codice dell'assenza
   * @param begin la data di inizio del periodo
   * @param end la data di fine del periodo
   * @param hours le eventuali ore d'assenza 
   * @param minutes gli eventuali minuti di assenza
   */
  @BasicAuth
  public static void checkAbsence(
      Long id, String eppn, String email, Long personPerseoId,
      String fiscalCode, String absenceCode, LocalDate begin, LocalDate end, 
      Integer hours, Integer minutes) 
          throws JsonProcessingException {

    Optional<Person> person = 
        personDao.byIdOrEppnOrEmailOrPerseoIdOrFiscalCode(
            id, eppn, email, personPerseoId, fiscalCode);
    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
          + "mail cnr che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().office);

    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    Optional<Contract> contract = wrapperFactory
        .create(person.get()).getCurrentContract();
    Optional<ContractMonthRecap> recap = wrapperFactory.create(contract.get())
        .getContractMonthRecap(new YearMonth(end.getYear(),
            end.getMonthOfYear()));

    if (!recap.isPresent()) {
      JsonResponse.notFound("Non esistono riepiloghi per" + person.get().name + " "
          + person.get().surname + " da cui prender le informazioni per il calcolo");
    } else {
      val absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
      if (!absenceType.isPresent()) {
        JsonResponse.badRequest("Tipo assenza non valido");
      }
      val justifiedType = absenceType.get().justifiedTypesPermitted.iterator().next();
      val groupAbsenceType = absenceType.get().defaultTakableGroup(); 
      val report = absenceService.insert(person.get(), groupAbsenceType, begin, end, 
          absenceType.get(), justifiedType, hours, minutes, false, absenceManager);

      val list = report.insertTemplateRows.stream()
                    .map(AbsenceAddedRest::build)
                    .collect(Collectors.toList());

      renderJSON(list);
    }
  }

  /**
   * Effettua la cancellazione di una assenza individuata con i 
   * parametri HTTP passati.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void delete(Long id) {
    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val absence = getAbsenceFromRequest(id);

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione delle assenze sull'office della persona passata.
    rules.checkIfPermitted(absence.personDay.person.office);

    absenceManager.removeAbsencesInPeriod(
        absence.personDay.person, absence.personDay.date, 
        absence.personDay.date, absence.absenceType);

    log.info("Deleted absence {} via REST", absence);
    JsonResponse.ok();
  }

  /**
   * Effettua la cancellazione di tutte le assenze di una persona con tipo dell'assenza
   * corrispondente all'absenceCode passato e nel periodo indicato.
   * Questo metodo può essere chiamato solo via HTTP DELETE.
   */
  public static void deleteAbsencesInPeriod(Long id, String eppn, String email, Long personPerseoId,
      String fiscalCode, @Required String absenceCode,
      @Required LocalDate begin, @Required LocalDate end) {

    RestUtils.checkMethod(request, HttpMethod.DELETE);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);

    if (Validation.hasErrors()) {
      JsonResponse.badRequest("Mandatory parameters missing (absenceCode, begin, end)");
    }

    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione delle assenze sull'office della persona passata.
    rules.checkIfPermitted(person.office);

    val absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);
    
    if (!absenceType.isPresent()) {
      JsonResponse.notFound(String.format("AbsenceType code %s not found", absenceType));
      return;
    }

    val deletedAbsences = absenceManager.removeAbsencesInPeriod(
        person, begin, end, absenceType.get());

    log.info("Deleted %s absences via REST for {}, code = {}, from {} to {}", 
        deletedAbsences, person.getFullname(), absenceCode, begin, end);
    JsonResponse.ok(String.format("Deleted %s absences", deletedAbsences));
  }

  /**
   * Inserimento di giorni di ferie con seleziona automatica dei codici da parte di ePAS.
   */
  public static void insertVacation(Long id, String eppn, String email, Long personPerseoId,
      String fiscalCode, @Required LocalDate begin, @Required LocalDate end) {

    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);

    if (Validation.hasErrors()) {
      JsonResponse.badRequest("Mandatory parameters missing (begin, end)");
    }

    rules.checkIfPermitted(person.office);

    val groupAbsenceType = 
        absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();

    AbsenceType absenceType = null;
    LocalDate recoveryDate = null;
    boolean forceInsert = false;

    val justifiedType = absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.all_day);

    InsertReport insertReport = absenceService.insert(person, groupAbsenceType, begin, end,
        absenceType, justifiedType, null, null, forceInsert, absenceManager);

    log.debug("Richiesto inserimento assenze per {}. "
        + "Codice/Tipo {}, dal {} al {}", 
        person.getFullname(), absenceType != null ? absenceType.code : groupAbsenceType,
            begin, end);

    val absences = 
        absenceManager.saveAbsences(insertReport, person, begin, recoveryDate, 
            justifiedType, groupAbsenceType);

    log.info("Effettuato inserimento assenze per {}. "
        + "Codice/Tipo {}, dal {} al {}", 
        person.getFullname(), absenceType != null ? absenceType.code : groupAbsenceType, 
        begin, end);

    renderJSON(
        gsonBuilder.create().toJson(
            absences.stream().map(AbsenceRest::build).collect(Collectors.toList())));
  }


  /**
   * Cerca il contratto in funzione del id passato.
   *
   * @return il contratto se trovato, altrimenti torna direttamente 
   *     una risposta HTTP 404.
   */
  @Util
  private static Absence getAbsenceFromRequest(Long id) {
    if (id == null) {
      JsonResponse.notFound();
    }
    
    val absence = absenceDao.getAbsenceById(id);
    
    if (absence == null) {
      JsonResponse.notFound();
    }
    
    //Controlla anche che l'utente corrente abbia
    //i diritti di gestione anagrafica sull'office attuale 
    //della persona associata all'assenza
    rules.checkIfPermitted(absence.personDay.person.office);
    return absence;
  }

}