package models;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.sql.Time;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

public class PersonTags extends JavaExtensions {


	public static String toDateTime(LocalDate localDate) {
		return String.format("%1$td %1$tB %1$tY", localDate.toDate());
	}
	
	public static String toCalendarTime(LocalDateTime ldt) {
        Number hour = ldt.getHourOfDay();
        Number minute = ldt.getMinuteOfHour();
        return String.format("%02d:%02d", hour, minute);          
	}
        
	public static String toHourTime(Integer minutes) {
		int min =  Math.abs(minutes%60);
		int hour= Math.abs((int)minutes/60);
		if((minutes.intValue()<0))
			
			return String.format("-%02d:%02d", hour, min);
        return String.format("%02d:%02d", hour, min);
	}
	
	public static String toHourTimeWithPlus(Integer minutes) {
		if (minutes < 0) {
			return toHourTime(minutes);
		}
		return "+" + toHourTime(minutes);
	}
	
	public static String toHourTimeWithMinus(Integer minutes) {
		if (minutes < 0) {
			return toHourTime(minutes);
		}
		return "-" + toHourTime(minutes);
	}
	
//	public static String convertIntToHour(int numberOfCompensatoryRest, Person person){
//		int timeAtWork = person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(1).workingTime;
//		return toHourTime(numberOfCompensatoryRest*timeAtWork);
//	}
	
	public static String toHour(Integer minutes){
		int hour = Math.abs((int)minutes/60);
		return String.format("%d", hour);
	}
	
	public static LocalDate convertToLocalDate(Date date){
		return new LocalDate(date);
	}

}
