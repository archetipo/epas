package controllers.rest.v3;

import cnr.sync.dto.v3.CompetenceCodeGroupShowDto;
import cnr.sync.dto.v3.CompetenceCodeGroupShowTerseDto;
import cnr.sync.dto.v3.CompetenceCodeShowDto;
import cnr.sync.dto.v3.CompetenceCodeShowTerseDto;
import cnr.sync.dto.v3.PersonCompetenceCodeShowDto;
import cnr.sync.dto.v3.PersonDayShowTerseDto;
import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import controllers.rest.v2.Persons;
import dao.CompetenceCodeDao;
import dao.PersonDao;
import helpers.JodaConverters;
import helpers.JsonResponse;
import helpers.rest.RestUtils;
import helpers.rest.RestUtils.HttpMethod;
import java.time.LocalDate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import models.CompetenceCode;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

/**
 * API Rest per l'esportazione delle informazioni sulle competenze
 * e sulla loro assegnazione alle persona.
 *
 * @author Cristian Lucchesi
 *
 */
@With(Resecure.class)
public class Competences extends Controller {

  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static PersonDao personDao;
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  /**
   * Metodo rest che ritorna un json contenente la lista dei codici di competenza
   * assegnati ad una persona con le rispettive date di validatà.
   */
  public static void personCompetenceCodes(Long id, String email, String eppn, Long personPerseoId, 
      String fiscalCode, String number, LocalDate date) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    val person = 
        Persons.getPersonFromRequest(id, email, eppn, personPerseoId, fiscalCode, number);
    rules.checkIfPermitted(person.office);
    val pccs = competenceCodeDao.listByPerson(
        person, Optional.fromNullable(JodaConverters.javaToJodaLocalDate(date)));

    renderJSON(gsonBuilder.create().toJson(pccs.stream().map(
        pcc -> PersonCompetenceCodeShowDto.build(pcc)).collect(Collectors.toList())));
  }

  /**
   * Metodo rest che restituisce un json con la lista di tutti i codici di competenza 
   * attivi con alcune informazioni minimali per ogni codice.
   */
  public static void list() {
    RestUtils.checkMethod(request, HttpMethod.GET);
    renderJSON(gsonBuilder.create().toJson(
        competenceCodeDao.getAllCompetenceCode().stream()
          .map(cc -> CompetenceCodeShowTerseDto.build(cc))
          .collect(Collectors.toList())));
  }

  /**
   * Metodo Rest che ritorna il json con tutte le informazioni relative ad una tipologia
   * di competenza.
   */
  public static void show(Long id, String code) {
    RestUtils.checkMethod(request, HttpMethod.GET);
    if (id == null && code == null) {
      JsonResponse.badRequest("E' obbligatorio indicare il parametro id oppure"
          + "il parametro code");
    }
    CompetenceCode competenceCode = null;
    if (id != null) {
      competenceCode = competenceCodeDao.getCompetenceCodeById(id); 
    } else {
      competenceCode = competenceCodeDao.getCompetenceCodeByCode(code);
    }
    RestUtils.checkIfPresent(competenceCode);
    renderJSON(gsonBuilder.create().toJson(CompetenceCodeShowDto.build(competenceCode)));
  }

}
