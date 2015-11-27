package controllers.rest;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

import helpers.JsonResponse;
import helpers.rest.JacksonModule;

import manager.AbsenceManager;
import manager.cache.AbsenceTypeManager;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;

import models.Absence;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import cnr.sync.dto.AbsenceAddedRest;
import cnr.sync.dto.AbsenceRest;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

import javax.inject.Inject;

@With(Resecure.class)
public class Absences extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static AbsenceManager absenceManager;
  @Inject
  static AbsenceTypeDao absenceTypeDao;
  @Inject
  static ObjectMapper mapper;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static AbsenceTypeManager absenceTypeManager;

  @BasicAuth
  public static void absencesInPeriod(String email, LocalDate begin, LocalDate end) {
    Person person = personDao.byEmail(email).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }
    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    List<AbsenceRest> absences = FluentIterable.from(absenceDao.getAbsencesInPeriod(
            Optional.fromNullable(person), begin, Optional.fromNullable(end), false))
            .transform(new Function<Absence, AbsenceRest>() {
              @Override
              public AbsenceRest apply(Absence absence) {
                AbsenceRest ar = new AbsenceRest();
                ar.absenceCode = absence.absenceType.code;
                ar.description = absence.absenceType.description;
                ar.date = absence.personDay.date.toString();
                ar.name = absence.personDay.person.name;
                ar.surname = absence.personDay.person.surname;
                return ar;
              }
            }).toList();
    renderJSON(absences);
  }

  @BasicAuth
  public static void insertAbsence(String email, String absenceCode, LocalDate begin, LocalDate end) {
    Person person = personDao.byEmail(email).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }
    if (begin == null || end == null || begin.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }
    List<AbsenceAddedRest> list = Lists.newArrayList();
    try {
      AbsenceInsertReport air = absenceManager.insertAbsenceRecompute(person, begin, Optional.fromNullable(end),
              absenceTypeDao.getAbsenceTypeByCode(absenceCode).get(),
              Optional.<Blob>absent(), Optional.<String>absent(), Optional.<Integer>absent());
      for (AbsencesResponse ar : air.getAbsences()) {
        AbsenceAddedRest aar = new AbsenceAddedRest();
        aar.absenceCode = ar.getAbsenceCode();
        aar.date = ar.getDate().toString();
        aar.isOK = ar.isInsertSucceeded();
        aar.reason = ar.getWarning();
        list.add(aar);
      }
      renderJSON(list);
    } catch (Exception e) {
      JsonResponse.badRequest("Errore nei parametri passati al server");
    }


  }

  @BasicAuth
  public static void checkAbsence(String email, String absenceCode,
                                  LocalDate begin, LocalDate end) throws JsonProcessingException {
    Optional<Person> person = personDao.byEmail(email);
    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca.");
    }
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

      AbsenceInsertReport air = absenceManager.insertAbsenceSimulation(person.get(), begin,
              Optional.fromNullable(end), absenceTypeManager.getAbsenceType(absenceCode)
              , Optional.<Blob>absent(), Optional.<String>absent(), Optional.<Integer>absent());

      renderJSON(mapper.writer(JacksonModule
              .filterProviderFor(SimpleBeanPropertyFilter
                      .serializeAllExcept("absenceAdded")))
              .writeValueAsString(air.getAbsences()));
    }
  }

}
