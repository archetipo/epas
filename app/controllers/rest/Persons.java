package controllers.rest;

import cnr.sync.dto.CompetenceDto;
import cnr.sync.dto.DayRecap;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import controllers.Resecure;
import controllers.Resecure.BasicAuth;

import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

import helpers.JsonResponse;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import javax.inject.Inject;

import manager.PersonDayManager;

import models.Competence;
import models.Person;
import models.PersonDay;
import models.absences.Absence;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

@With(Resecure.class)
public class Persons extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  private static CompetenceDao competenceDao;

  @BasicAuth
  public static void days(String email, LocalDate start, LocalDate end) {

    if (email == null) {
      email = "cristian.lucchesi@iit.cnr.it";
    }
    Optional<Person> person = personDao.byEmail(email);
    if (!person.isPresent()) {
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente la "
              + "mail cnr che serve per la ricerca. Assicurarsi di aver"
              + "lanciato il job per la sincronizzazione delle email dei dipendenti");
    }
    if (start == null || end == null || start.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }

    List<DayRecap> personDays = FluentIterable.from(personDao.getPersonDayIntoInterval(
            person.get(), new DateInterval(start, end), false))
            .transform(personday -> {
              DayRecap dayRecap = new DayRecap();

              dayRecap.workingMinutes = personday.getAssignableTime();
              dayRecap.date = personday.date.toString();
              dayRecap.mission = personDayManager.isOnMission(personday);
              dayRecap.workingTime =
                  wrapperFactory.create(personday).getWorkingTimeTypeDay().get().workingTime;
              return dayRecap;
            }).toList();

    renderJSON(personDays);
  }

  @BasicAuth
  public static void missions(String email, LocalDate start, LocalDate end, boolean forAttachment) {

    if (email == null) {
      email = "cristian.lucchesi@iit.cnr.it";
    }
    Optional<Person> person = personDao.byEmail(email);
    List<DayRecap> personDays = Lists.newArrayList();
    if (person.isPresent()) {

      personDays = FluentIterable.from(absenceDao.getAbsencesInPeriod(
                  person, start, Optional.fromNullable(end), forAttachment))
              .transform(absence -> {
                DayRecap dayRecap = new DayRecap();
                dayRecap.workingMinutes = 0;
                dayRecap.date = absence.personDay.date.toString();
                if (personDayManager.isOnMission(absence.personDay)) {
                  dayRecap.mission = true;
                } else {
                  dayRecap.mission = false;
                }
                return dayRecap;
              }).toList();
    }
    renderJSON(personDays);
  }

  @BasicAuth
  public static void competences(String email, LocalDate start, LocalDate end, List<String> code) {

    Person person = personDao.byEmail(email).orNull();
    if (person == null) {
      JsonResponse.notFound("Indirizzo email incorretto");
    }
    if (start == null || end == null || start.isAfter(end)) {
      JsonResponse.badRequest("Date non valide");
    }

    List<Competence> competences = Lists.newArrayList();

    while (!start.isAfter(end)) {

      competences.addAll(competenceDao.competenceInMonth(person, start.getYear(),
              start.getMonthOfYear(), Optional.fromNullable(code)));

      start = start.plusMonths(1);
      // Il caso in cui non vengano specificate delle date che coincidono con l'inizio e la fine
      // di un mese
      if (start.isAfter(end) && start.getMonthOfYear() == end.getMonthOfYear()) {
        competences.addAll(competenceDao.competenceInMonth(person, start.getYear(),
                start.getMonthOfYear(), Optional.fromNullable(code)));
      }
    }

    List<CompetenceDto> competencesList = FluentIterable.from(competences)
            .transform(CompetenceDto.FromCompetence.ISTANCE).toList();

    renderJSON(competencesList);
  }
}
