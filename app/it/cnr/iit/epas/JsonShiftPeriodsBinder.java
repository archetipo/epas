/**
 * 
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityType;
import models.ShiftTimeTable;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;
import net.fortuna.ical4j.model.DateTime;

import org.apache.log4j.jmx.LoggerDynamicMBean;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.db.jpa.JPA;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author arianna
 *
 * Read json sent from the sist-org shift calendar. Json consist of periods of shift of a certain type and slot
 * [{
 * 	id: id of the person in the shift
 * 	start : start date
 * 	end: end date
 *  cancelled: true/false
 * 	shiftSlot: slot of the shift (morning/afternoon) 
 * }]
 */
@Global
public class JsonShiftPeriodsBinder implements TypeBinder<ShiftPeriods> {

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	/* (non-Javadoc)
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {
		
		Logger.debug("binding ShiftPeriods: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
			
			JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
			Logger.debug("jsonArray di shift period letti = %s", jsonArray);

			JsonObject jsonObject = null;
			Person person = null;
			ShiftType shiftType = null;
			
			Long personId = null;
			
			for (JsonElement jsonElement : jsonArray) {
				jsonObject = jsonElement.getAsJsonObject();
				Logger.trace("jsonObject (shift period letto) = %s", jsonObject);
				
				// read the start and end data of the period
				LocalDate start = new LocalDate(jsonObject.get("start").getAsString());
				LocalDate end = new LocalDate(jsonObject.get("end").getAsString());

				// get a real shift period
				if (!jsonObject.get("cancelled").getAsBoolean()) {
					// validate person id
					personId = jsonObject.get("id").getAsLong();
					person = Person.findById(personId);
					if (person == null) {
						throw new IllegalArgumentException(String.format("Person with id = %s not found", personId));
					}
					Logger.debug("letto id = %s corrispondente a person = %s", personId, person.name);
					
					
					// read and validate the shift slot (MORNING/AFTERNOON)
					String shiftSlotDesc = jsonObject.get("shiftSlot").getAsString();
					Logger.debug("Leggo dal json shiftSlotDesc=%s", shiftSlotDesc);
					
					ShiftSlot shiftSlot = ShiftSlot.getEnum(shiftSlotDesc);
					Logger.debug("Cerca e controlla shiftSlot=%s", shiftSlot);
					if (shiftSlot == null) {
						throw new IllegalArgumentException(String.format("ShiftSlot with name = %s not found2", shiftSlotDesc));
					}
					
					ShiftPeriod shiftPeriod =	new ShiftPeriod(person, start, end, shiftType, false, shiftSlot);
					Logger.debug("Creato ShiftPeriod person = %s, start=%s, end=%s, shiftType=%s, shiftSlot=%s", person.name, start, end, shiftType, shiftSlot);
					
					shiftPeriods.add(shiftPeriod);
					Logger.debug("letto id = %s corrispondente a person = %s", personId, person.name);
				} else {
					ShiftPeriod shiftPeriod =	new ShiftPeriod(start, end, shiftType, true);
					shiftPeriods.add(shiftPeriod);
				}
				
			}
			
			Logger.debug("shiftPeriods = %s", shiftPeriods);
			
			return new ShiftPeriods(shiftPeriods);
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding List<ShiftPeriod>.");
			throw e;
		}
	}

}

