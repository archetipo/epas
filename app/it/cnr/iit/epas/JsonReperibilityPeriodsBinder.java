/**
 * 
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import models.Person;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author cristian
 *
 */
@Global
public class JsonReperibilityPeriodsBinder implements TypeBinder<ReperibilityPeriods> {

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {
		
		Logger.debug("binding ReperibilityPeriods: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			
			List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();
			
			JsonArray jsonArray = new JsonParser().parse(value).getAsJsonArray();
			Logger.debug("jsonArray = %s", jsonArray);

			for (JsonElement jsonElement : jsonArray) {
				JsonObject ob0 = jsonElement.getAsJsonObject();
				Logger.trace("jsonArray.get(0) = %s", ob0);
				Person person = Person.findById(ob0.get("id").getAsLong());
				LocalDate start = new LocalDate(ob0.get("start").getAsString());
				LocalDate end = new LocalDate(ob0.get("end").getAsString());
				
				ReperibilityPeriod reperibilityPeriod =	new ReperibilityPeriod(person, start, end);
				reperibilityPeriods.add(reperibilityPeriod);
			}
			
			Logger.debug("reperibilityPeriods = %s", reperibilityPeriods);
			
			return new ReperibilityPeriods(reperibilityPeriods);
			
		} catch (Exception e) {
			Logger.error("Problem during binding List<ReperibilityPeriod>. Exception: %s", e);
			throw e;
		}
	}
	
}


