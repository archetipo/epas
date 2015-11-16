package it.cnr.iit.epas;

import com.google.common.base.Optional;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class DateUtility {

	final static int MINUTE_IN_HOUR = 60;

	public final static int DECEMBER = 12;
	public final static int JANUARY = 1;
	
	final static LocalDate MAX_DATE = new LocalDate(9999, 12, 1);
	
	/**
	 * 
	 * @param year
	 * @return il giorno in cui cade la pasqua
	 */
	private static final LocalDate findEaster(int year) {
	    if (year <= 1582) {
	      throw new IllegalArgumentException(
	          "Algorithm invalid before April 1583");
	    }
	    int golden, century, x, z, d, epact, n;
	    LocalDate easter = null;
	    golden = (year % 19) + 1; /* E1: metonic cycle */
	    century = (year / 100) + 1; /* E2: e.g. 1984 was in 20th C */
	    x = (3 * century / 4) - 12; /* E3: leap year correction */
	    z = ((8 * century + 5) / 25) - 5; /* E3: sync with moon's orbit */
	    d = (5 * year / 4) - x - 10;
	    epact = (11 * golden + 20 + z - x) % 30; /* E5: epact */
	    if ((epact == 25 && golden > 11) || epact == 24)
	      epact++;
	    n = 44 - epact;
	    n += 30 * (n < 21 ? 1 : 0); /* E6: */
	    n += 7 - ((d + n) % 7);
	    
	    if (n > 31) /* E7: */{
	    	easter = new LocalDate(year, 4 , n - 31);
	    	
	      return easter; /* April */
	    }
	    else{
	    	easter = new LocalDate(year, 3 , n);
	    	
	      return easter; /* March */
	    }
	}
	
	/**
	 * 
	 * @param officePatron
	 * @param date
	 * @return
	 */
	public static boolean isGeneralHoliday(
			final Optional<MonthDay> officePatron, final LocalDate date) {
		 
		LocalDate easter = findEaster(date.getYear());
		LocalDate easterMonday = easter.plusDays(1);
		if (date.getDayOfMonth() == easter.getDayOfMonth() 
				&& date.getMonthOfYear() == easter.getMonthOfYear()) {
			return true;
		}
		if (date.getDayOfMonth() == easterMonday.getDayOfMonth() 
				&& date.getMonthOfYear() == easterMonday.getMonthOfYear()) {
			return true;
		}
		//if((date.getDayOfWeek() == 7)||(date.getDayOfWeek() == 6))
		//	return true;		
		if ((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 25)) {
			return true;
		}
		if ((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 26)) {
			return true;
		}
		if ((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 8)) {
			return true;
		}
		if ((date.getMonthOfYear() == 6) && (date.getDayOfMonth() == 2)) {
			return true;
		}
		if ((date.getMonthOfYear() == 4) && (date.getDayOfMonth() == 25)) {
			return true;
		}
		if ((date.getMonthOfYear() == 5) && (date.getDayOfMonth() == 1)) {
			return true;
		}
		if ((date.getMonthOfYear() == 8) && (date.getDayOfMonth() == 15)) {
			return true;
		}
		if ((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 1)) {
			return true;
		}
		if ((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 6)) {
			return true;
		}
		if ((date.getMonthOfYear() == 11) && (date.getDayOfMonth() == 1)) {
			return true;
		}
		
		if (officePatron.isPresent()) { 
			
			return (date.getMonthOfYear() == officePatron.get().getMonthOfYear()
					&& 
					date.getDayOfMonth() == officePatron.get().getDayOfMonth());
		}

		/**
		 * ricorrenza centocinquantenario dell'unità d'Italia
		 */
		if (date.isEqual(new LocalDate(2011, 3, 17))) {
			return true;
		}			
			
		return false;
	}

	/**
	 * @param begin data iniziale
	 * @param end data finale
	 * @return lista di tutti i giorni fisici contenuti nell'intervallo 
	 * [begin,end] estremi compresi, escluse le general holiday
	 */
	public static List<LocalDate> getGeneralWorkingDays(final LocalDate begin,
			final LocalDate end) {
	
		LocalDate day = begin;
		List<LocalDate> generalWorkingDays = new ArrayList<LocalDate>();
		while (!day.isAfter(end)) {
			if (!DateUtility.isGeneralHoliday(
					Optional.<MonthDay>absent(), day)) {
				generalWorkingDays.add(day);
			}
			day = day.plusDays(1);
		}
		return generalWorkingDays;
	}

	
	/**
	 * 
	 * @param date data
	 * @param interval intervallo
	 * @return true se la data ricade nell'intervallo estremi compresi
	 */
	public static boolean isDateIntoInterval(final LocalDate date,
			final DateInterval interval) {
		LocalDate dateToCheck = date;
		if (dateToCheck == null) {
			dateToCheck = MAX_DATE;
		}
		
		if (dateToCheck.isBefore(interval.getBegin()) 
				|| dateToCheck.isAfter(interval.getEnd())) {
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param inter1
	 * @param inter2
	 * @return l'intervallo contenente l'intersezione fra inter1 e inter2,
	 * null in caso di intersezione vuota
	 */
	public static DateInterval intervalIntersection(DateInterval inter1, DateInterval inter2)
	{
		if (inter1 == null || inter2 == null) {
			return null;
		}
		//ordino
		if (!inter1.getBegin().isBefore(inter2.getBegin())) {
			DateInterval aux = 
					new DateInterval(inter1.getBegin(), inter1.getEnd());
			inter1 = inter2;
			inter2 = aux;
		}
		
		
		//un intervallo contenuto nell'altro
		if (isIntervalIntoAnother(inter1, inter2)) {
			return inter1;
		}
		
		if (isIntervalIntoAnother(inter2, inter1)) {
			return inter2;
		}
		
		//fine di inter1 si interseca con inizio di inter2
		if (inter1.getEnd().isBefore(inter2.getBegin())) {
			return null;
		} else {
			return new DateInterval(inter2.getBegin(), inter1.getEnd());
		}
		
	}
	
	/**
	 * 
	 * @param inter l'intervallo
	 * @return conta il numero di giorni appartenenti 
	 * all'intervallo estremi compresi
	 */
	public static int daysInInterval(final DateInterval inter) {
		return inter.getEnd().getDayOfYear() 
				- inter.getBegin().getDayOfYear() + 1;
	}
	
	/**
	 * 
	 * @param first il primo intervallo
	 * @param second il secondo intervallo
	 * @return true se il primo intervallo e' contenuto nel secondo intervallo 
	 * (estremi compresi), false altrimenti
	 */
	public static boolean isIntervalIntoAnother(final DateInterval first, 
			final DateInterval second) {
		
		if (first.getBegin().isBefore(second.getBegin()) 
				|| first.getEnd().isAfter(second.getEnd())) {
			return false;
		}
		return true;
	}
	
	/**
	 * @return la data infinito
	 */
	public static LocalDate setInfinity() {
		return MAX_DATE;
	}
	
	/**
	 * @param date la data da confrontare
	 * @return se la data è molto molto lontana...
	 */
	public static boolean isInfinity(final LocalDate date) {
		return date.equals(MAX_DATE);
	}
	


	/**
	 * 
	 * @param monthNumber mese da formattare
	 * @return il nome del mese con valore monthNumber,
	 * 	null in caso di argomento non valido 
	 */
	public static String fromIntToStringMonth(final Integer monthNumber) {
		LocalDate date = new LocalDate().withMonthOfYear(monthNumber);
		return date.monthOfYear().getAsText();
	}
	
	/**
	 * @param minute minuti da formattare
	 * @return stringa contente la formattazione -?HH:MM
	 */
	public static String fromMinuteToHourMinute(final int minute) {
		if (minute == 0) { 
			return "00:00";
		}
		String s = "";
		int positiveMinute = minute;
		if (minute < 0) {
			s = s + "-";
			positiveMinute = minute * -1;
		}
		int hour = positiveMinute / MINUTE_IN_HOUR;
		int min  = positiveMinute % MINUTE_IN_HOUR;
		
		if (hour < 10) {
			s = s + "0" + hour;
		} else {
			s = s + hour;
		}
		s = s + ":";
		if (min < 10) {
			s = s + "0" + min;
		} else {
			s = s + min;
		}
		return s;
	}
	

	/**
	 * @param date data
	 * @param pattern : default dd/MM
	 * @return effettua il parsing di una stringa 
	 * che contiene solo giorno e Mese
	 */
	public static LocalDate dayMonth(final String date,
			final Optional<String> pattern) {
		
		DateTimeFormatter dtf;
		if (pattern.isPresent()) {
			dtf = DateTimeFormat.forPattern(pattern.get());
		} else {
			dtf = DateTimeFormat.forPattern("dd/MM");
		}
		return LocalDate.parse(date, dtf);
	}
	
	/**
	 * @param yearMonth il mese da considerare 
	 * @return il primo giorno del mese da considerare formato LocalDate
	 */
	public static LocalDate getMonthFirstDay(final YearMonth yearMonth) {
		return new LocalDate(yearMonth.getYear(), 
				yearMonth.getMonthOfYear(), 1);
	}
	
	/**
	 * @param yearMonth il mese da considerare
	 * @return l'ultimo giorno del mese da considerare formato LocalDate
	 */
	public static LocalDate getMonthLastDay(final YearMonth yearMonth) {
		return new LocalDate(yearMonth.getYear(), 
				yearMonth.getMonthOfYear(), 1).dayOfMonth().withMaximumValue();
	}
	
	/**
	 * @param time ora
	 * @return il numero di minuti trascorsi dall'inizio del giorno all'ora
	 */
	public static int toMinute(final LocalDateTime time) {
		int dateToMinute = 0;
		if (time != null) {
			int hour = time.get(DateTimeFieldType.hourOfDay());
			int minute = time.get(DateTimeFieldType.minuteOfHour());
			dateToMinute = (MINUTE_IN_HOUR * hour) + minute;
		}
		return dateToMinute;
	}
	
	/**
	 * 
	 * @param begin orario di ingresso
	 * @param end orario di uscita
	 * @return minuti lavorati
	 */
	 public static Integer getDifferenceBetweenLocalTime(final LocalTime begin,
			 final LocalTime end) {

		int timeToMinute = 0;
		if (end != null && begin != null) {
			int hourBegin = begin.getHourOfDay();
			int minuteBegin = begin.getMinuteOfHour();
			int hourEnd = end.getHourOfDay();
			int minuteEnd = end.getMinuteOfHour();
			timeToMinute = ((MINUTE_IN_HOUR * hourEnd + minuteEnd) 
					- (MINUTE_IN_HOUR * hourBegin + minuteBegin));
		} 
		
		return timeToMinute;
	}
}
