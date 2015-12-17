package controllers;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import com.google.gson.GsonBuilder;

import dao.PersonDao;
import dao.UserDao;

import helpers.deserializers.ImageToByteArrayDeserializer;

import manager.ReportCentreManager;

import models.Person;
import models.User;
import models.exports.ReportData;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import play.libs.Mail;
import play.mvc.Controller;

import java.io.InputStreamReader;
import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.validation.Valid;


public class ReportCentre extends Controller {

  @Inject
  static UserDao userDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static ReportCentreManager reportCentreManager;

  public static void javascript() {
    response.contentType = MediaType.JAVASCRIPT_UTF_8.toString();
    response.setHeader("Cache-Control", "max-age=" + 31536000);
    response.setHeader("Expires", LocalDateTime.now().plusYears(1).toString());
    render("/feedback.js");
  }

  /**
   * Invia un report via email leggendo la segnalazione via post json.
   */
  public static void sendReport() {

    final ReportData data = new GsonBuilder()
        .registerTypeHierarchyAdapter(byte[].class,
            new ImageToByteArrayDeserializer()).create()
        .fromJson(new InputStreamReader(request.body), ReportData.class);

    ReportMailer.feedback(data, session, Security.getUser());
  }

  public static void generateReport(String actionInfected, Long personId, @Valid Integer month,
                                    @Valid Integer year, @Valid Integer day) {
    Person person = personDao.getPersonById(personId);
    User userLogged = Security.getUser().get();
    render(userLogged, person, actionInfected, year, month, day);
  }


  public static void sendProblem(Long userId, String report,
                                 @Valid String month, @Valid String year, String actionInfected) {
    User user = userDao.getUserByIdAndPassword(userId, Optional.<String>absent());
    if (user == null)
      notFound();

    SimpleEmail email = new SimpleEmail();
    String sender = user.person != null ? user.person.fullName() : user.username;
    try {
      email.addTo("epas@iit.cnr.it");
      //email.setFrom("epas@iit.cnr.it");
      if (user.person != null && !user.person.email.equals(""))
        email.addReplyTo(user.person.email);
      email.setSubject("Segnalazione malfunzionamento ");
      email.setMsg("E' stata riscontrata una anomalia dalla pagina: " + actionInfected + '\n'
              + " con mese uguale a: " + month + '\n'
              + " con anno uguale a: " + year + '\n'
              + " visitata da: " + sender + '\n'
              + " in data: " + LocalDate.now() + '\n'
              + " con il seguente messaggio: " + report);
      Mail.send(email);
      flash.success("Mail inviata con successo");
      Application.index();

    } catch (EmailException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      flash.error("Errore durante l'invio della mail");
      Application.index();
    }

  }

}
