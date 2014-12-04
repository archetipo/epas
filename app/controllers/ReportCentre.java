package controllers;

import it.cnr.iit.epas.JsonReportBinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import models.Person;
import models.exports.ReportFromJson;

import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;

import play.Logger;
import play.data.binding.As;
import play.libs.Mail;
import play.mvc.Controller;


public class ReportCentre extends Controller{

	public static void sendReport(@As(binder=JsonReportBinder.class) ReportFromJson body){

		Logger.debug("report....");
		if (body == null) {
			badRequest();	
		}

		try {
			Person person = Security.getUser().get().person;
			File theDir = new File("/tmp/immagini-mail/");
			if(!theDir.exists())
			{	
				boolean success = (new File("/tmp/immagini-mail/")).mkdirs();

				if (!success) {
					Logger.error("Errore in creazione della cartella");
				}
				Logger.debug("Creata cartella ");
			}
			else{
				Logger.debug("Cartella esistente");
			}
			
			FileOutputStream imageOutFile = new FileOutputStream(
					"/tmp/immagini-mail/image"+person.id+".png");
			imageOutFile.write(body.image); 

			imageOutFile.close();

			EmailAttachment attachment = new EmailAttachment();
			attachment.setPath("/tmp/immagini-mail/image"+person.id+".png");
			attachment.setDisposition(EmailAttachment.ATTACHMENT);
			attachment.setDescription("Foto anomalia");
			attachment.setName("Foto");

			MultiPartEmail email = new MultiPartEmail();

			email.addTo("epas@iit.cnr.it");
			email.setFrom("segnalazioni@epas.tools.iit.cnr.it");
			if(!person.email.equals(""))
				email.addReplyTo(person.email);
			email.attach(attachment);

			email.setSubject("Segnalazione malfunzionamento ");
			email.setMsg("E' stata riscontrata una anomalia dalla pagina: "+body.url+" visitata da: "+person.name+" "+person.surname+'\n'+"Con il seguente messaggio: "+body.note);
			Mail.send(email); 



		} catch (EmailException e) {
			Logger.error("Errore in invio mail. %s", e.toString());

		} catch (FileNotFoundException e) {
			Logger.error("Errore nel caricamento del file immagine da inviare. %s", e.toString());

		} catch (IOException e) {
			Logger.error("Errore di I/O. %s", e.toString());

		}		


	}


}
