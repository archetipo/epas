package personDays;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import manager.PersonDayManager;
import manager.services.PairStamping;

import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.enumerate.StampTypes;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.testng.annotations.Test;

import java.util.List;

public class PersonDaysTest {

  public static LocalTime startLunch = new LocalTime(1,0,0);
  public static LocalTime endLunch = new LocalTime(23,0,0);
 
  public static LocalDate first = new LocalDate(2016, 1, 2);
  public static LocalDate second = new LocalDate(2016, 1, 3);
  
  public static StampTypes lunchST = StampTypes.PAUSA_PRANZO;
  public static StampTypes serviceST = StampTypes.MOTIVI_DI_SERVIZIO;
  
  public static PersonDayManager personDayManager = new PersonDayManager(
      null, null, null, null, null, null, null);
  
  /**
   * Test su un giorno Normale.
   */
  @Test
  public void test() {
    
    PersonDay previousForProgressive = new PersonDay(null, first, 0, 0, 60);
    PersonDay personDay = new PersonDay(null, second);
    
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 9, 30, WayType.in, null));
    stampings.add(stampings(personDay, 16, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, startLunch, endLunch, null, null);
    personDayManager.updateDifference(personDay, normalDay(), false);
    personDayManager.updateProgressive(personDay, Optional.fromNullable(previousForProgressive));
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    assertThat(personDay.getTimeAtWork()).isEqualTo(390);   //6:30 ore
    assertThat(personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    assertThat(personDay.getDecurted()).isEqualTo(30);      //30 minuti
    assertThat(personDay.getDifference()).isEqualTo(-42);
    assertThat(personDay.getProgressive()).isEqualTo(18);
    assertThat(personDay.isTicketAvailable).isEqualTo(true);
    
  }
  
  /**
   * Quando la pausa pranzo contiene interamente la fascia pranzo dell'istituto va conteggiata.
   */
  @Test
  public void tagliaferriIsHungry() {
    
    LocalTime startLunch = new LocalTime(12, 0, 0);
    LocalTime endLunch = new LocalTime(15, 0, 0);
    
    PersonDay personDay = new PersonDay(null, second);
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 30, WayType.in, null));
    stampings.add(stampings(personDay, 11, 30, WayType.out, null));
    
    stampings.add(stampings(personDay, 15, 30, WayType.in, null));
    stampings.add(stampings(personDay, 19, 30, WayType.out, null));
    
    personDay.setStampings(stampings);
    
    personDayManager.updateTimeAtWork(personDay, normalDay(), false, startLunch, endLunch, null, null);
    personDayManager.updateTicketAvailable(personDay, normalDay(), false);
    
    assertThat(personDay.getTimeAtWork()).isEqualTo(420);   //7:00 ore
    assertThat(personDay.getStampingsTime()).isEqualTo(420);//7:00 ore     
    assertThat(personDay.getDecurted()).isEqualTo(null);      //00 minuti
    assertThat(personDay.isTicketAvailable).isEqualTo(true);
    
  }
  
  /**
   * Le pause pranzo da considerare sono tutte quelle che hanno:
   * #1 Uscita pr Ingresso pr
   * Uscita pr Ingresso 
   * Uscita    Ingrssso pr
   * Uscita    Ingresso    (e sono in istituto non di servizio) 
   */
  @Test
  public void consideredGapLunchPairs() { 

    PersonDay personDay = new PersonDay(null, second);
    List<PairStamping> validPairs;
    List<PairStamping> gapLunchPair;

    // #1
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));

    personDay.setStampings(stampings);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);

    assertThat(gapLunchPair.size()).isEqualTo(1);
    assertThat(gapLunchPair.get(0).timeInPair).isEqualTo(60);

    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.computeValidPairStampings(personDay);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(validPairs.size()).isEqualTo(1);
    assertThat(validPairs.get(0).timeInPair).isEqualTo(180);
    assertThat(gapLunchPair.size()).isEqualTo(0);


    // #2
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(gapLunchPair.size()).isEqualTo(1);
    assertThat(gapLunchPair.get(0).timeInPair).isEqualTo(60);
    
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.computeValidPairStampings(personDay);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(validPairs.size()).isEqualTo(1);
    assertThat(validPairs.get(0).timeInPair).isEqualTo(180);
    assertThat(gapLunchPair.size()).isEqualTo(0);
     
    // #3
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, null));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(gapLunchPair.size()).isEqualTo(1);
    assertThat(gapLunchPair.get(0).timeInPair).isEqualTo(60);
    
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 13, 00, WayType.out, null));
    stampings.add(stampings(personDay, 14, 00, WayType.in, lunchST));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.computeValidPairStampings(personDay);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(validPairs.size()).isEqualTo(1);
    assertThat(validPairs.get(0).timeInPair).isEqualTo(180);
    assertThat(gapLunchPair.size()).isEqualTo(0);
    
    // # L'ingresso post pranzo deve essere coerente.
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 13, 00, WayType.out, lunchST));
    stampings.add(stampings(personDay, 14, 00, WayType.in, StampTypes.MOTIVI_PERSONALI));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.computeValidPairStampings(personDay);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(validPairs.size()).isEqualTo(2);
    assertThat(gapLunchPair.size()).isEqualTo(0);
    
    // # Il test che secondo Daniele fallisce
    LocalTime startLunch = new LocalTime(12,0,0);
    LocalTime endLunch = new LocalTime(15,0,0);
    
    stampings = Lists.newArrayList();
    stampings.add(stampings(personDay, 8, 00, WayType.in, null));
    stampings.add(stampings(personDay, 12, 30, WayType.out, lunchST));
    stampings.add(stampings(personDay, 13, 00, WayType.in, StampTypes.MOTIVI_PERSONALI));
    stampings.add(stampings(personDay, 13, 30, WayType.out, StampTypes.MOTIVI_PERSONALI));
    stampings.add(stampings(personDay, 14, 00, WayType.in, null));
    stampings.add(stampings(personDay, 17, 00, WayType.out, null));
    personDay.setStampings(stampings);
    
    validPairs = personDayManager.computeValidPairStampings(personDay);
    gapLunchPair = personDayManager.getGapLunchPairs(personDay, startLunch, endLunch);
    
    assertThat(gapLunchPair.size()).isEqualTo(0);
    
    
  }

  /**
   * Supporto alla creazione di un WorkingTimeType da non mockare.
   * @return
   */
  public WorkingTimeTypeDay normalDay() {
    WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
    wttd.breakTicketTime = 30;
    wttd.mealTicketTime = 360;
    wttd.workingTime = 432;
    wttd.ticketAfternoonThreshold = null;
    wttd.holiday = false;
    return wttd;
  }
  
  /**
   * Supporto alla creazione di una stamping da non mockare.
   * @param personDay
   * @param hour
   * @param minute
   * @param way
   * @param stampType
   * @return
   */
  public Stamping stampings(PersonDay personDay, int hour, int minute, 
      WayType way, StampTypes stampType) {
    LocalDateTime time = new LocalDateTime(personDay.getDate().getYear(), 
        personDay.getDate().getMonthOfYear(), personDay.getDate().getDayOfMonth(), hour, minute);
    Stamping stamping = new Stamping(personDay, time);
    stamping.way = way;
    stamping.stampType = stampType;
    return stamping;
  }
  
}
