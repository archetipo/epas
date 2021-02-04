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

package controllers.rest.v3;

import cnr.sync.dto.v2.PersonShowTerseDto;
import cnr.sync.dto.v3.PersonDayShowDto;
import cnr.sync.dto.v3.PersonDayShowTerseDto;
import cnr.sync.dto.v3.PersonMonthRecapDto;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.rest.v2.Persons;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import helpers.JodaConverters;
import helpers.JsonResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Office;
import models.Person;
import models.PersonDay;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

/**
 * Controller per la visualizzazione via REST di dati relativi alla situazione giornaliera.
 */
@Slf4j
@With(Resecure.class)
public class PersonDays extends Controller {

  @Inject
  static PersonDao personDao;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  static GsonBuilder gsonBuilder;

  /**
   * Metodo rest che ritorna la situazione della persona (passata per id, email, eppn, 
   * personPerseoId o fiscalCode) in un giorno specifico (date).
   */
  public static void getDaySituation(
      Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, 
      LocalDate date) {
    log.debug("Chiamata getDaySituation, email = {}, date = {}", email, date);
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    if (date == null) {
      JsonResponse.badRequest("Il parametro date è obbligatorio");
    }
    rules.checkIfPermitted(person.office);

    PersonDay pd = 
        personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(date)).orNull();
    if (pd == null) {
      JsonResponse.notFound(
          String.format("Non sono presenti informazioni per %s nel giorno %s",
              person.getFullname(), date));
    }

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(PersonDayShowTerseDto.build(pd)));
  }

  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di tutti i dipendenti
   * di una certa sede nell'anno/mese passati come parametro.
   *
   * @param sedeId l'identificativo della sede di cui ricercare la situazione delle persone
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void getMonthSituationByOffice(String sedeId, Integer year, Integer month) {
    log.debug("getMonthSituationByOffice -> sedeId={}, year={}, month={}", sedeId, year, month);
    if (year == null || month == null || sedeId == null) {
      JsonResponse.badRequest("I parametri sedeId, year e month sono tutti obbligatori");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Office non trovato con il sedeId passato per parametro");
    }
    rules.checkIfPermitted(office.get());    
    
    org.joda.time.LocalDate date = new org.joda.time.LocalDate(year, month, 1);

    List<PersonDay> personDays = 
        personDayDao.getPersonDaysByOfficeInPeriod(
            office.get(), date, date.dayOfMonth().withMaximumValue());
    
    val personDayMap = 
        personDays.stream().collect(Collectors.groupingBy(PersonDay::getPerson));
    
    List<PersonMonthRecapDto> monthRecaps = Lists.newArrayList();
    personDayMap.forEach((p, pds) -> {      
      monthRecaps.add(PersonMonthRecapDto.builder()
          .person(PersonShowTerseDto.build(p))
          .year(year).month(month)
          .personDays(
              pds.stream().map(pd -> PersonDayShowTerseDto.build(pd))
                .collect(Collectors.toList()))
          .build());
    });

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(monthRecaps));
  }

  /**
   * Metodo che ritorna la lista delle situazioni giornaliere di tutti 
   * i dipendenti della sede passata come parametro alla data passata come parametro.
   *
   * @param sedeId l'identificativo della sede di cui cercare le persone
   * @param date la data per cui cercare i dati
   */
  public static void getDaySituationByOffice(String sedeId, LocalDate date) {
    log.debug("getDaySituationByOffice -> sedeId={}, data={}", sedeId, date);
    if (sedeId == null || date == null) {
      JsonResponse.badRequest("I parametri sedeId e date sono obbligatori.");
    }
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      JsonResponse.notFound("Office non trovato con il sedeId passato per parametro");
    }
    rules.checkIfPermitted(office.get());
    Set<Office> offices = Sets.newHashSet(office.get());
    
    List<Person> personList = personDao
        .list(Optional.<String>absent(), offices, false, 
            JodaConverters.javaToJodaLocalDate(date), JodaConverters.javaToJodaLocalDate(date), 
            true).list();

    List<PersonDayShowDto> personDayDtos = Lists.newArrayList();
    
    for (Person person : personList) {
      PersonDay pd = 
          personDayDao.getPersonDay(person, JodaConverters.javaToJodaLocalDate(date)).orNull();
      if (pd == null) {
        log.info("Non sono presenti informazioni per {} nel giorno {}", person.getFullname(), date);
      }
      personDayDtos.add(PersonDayShowDto.build(pd));
    }

    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(personDayDtos));
  }

  /**
   * Metodo rest che ritorna un json contenente la lista dei person day di un dipendente
   * nell'anno/mese passati come parametro.
   */
  public static void getMonthSituationByPerson(Long id, String email, String eppn, 
      Long personPerseoId, String fiscalCode, Integer year, Integer month) {
    val person = Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode);
    if (year == null || month == null) {
      JsonResponse.badRequest("I parametri year e month sono tutti obbligatori");
    }
    rules.checkIfPermitted(person.office);
    val personDays = personDayDao.getPersonDayInMonth(person, new YearMonth(year, month));
    val monthRecap = 
        PersonMonthRecapDto.builder().year(year).month(month)
        .person(PersonShowTerseDto.build(person))
        .personDays(personDays.stream().map(pd -> PersonDayShowTerseDto.build(pd))
            .collect(Collectors.toList()))
        .build();
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(monthRecap));    
  }

}