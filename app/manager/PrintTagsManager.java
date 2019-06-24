package manager;

import com.google.common.collect.Lists;

import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;

import java.util.List;

import javax.inject.Inject;

import manager.charts.ChartsManager;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;

import models.Stamping;
import models.dto.OffSiteWorkingTemp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PrintTagsManager {

  private static final Logger log = LoggerFactory.getLogger(PrintTagsManager.class);
  private final StampingHistoryDao stampingHistoryDao;

  @Inject
  public PrintTagsManager(StampingHistoryDao stampingHistoryDao) {
    this.stampingHistoryDao = stampingHistoryDao;
  }

  /**
   * Metodo che ritorna le informazioni sullo storico delle timbrature del dipendente.
   * @param psDto il person stamping recap delle timbrature del dipendente
   * @return la lista di liste contenente le informazioni sullo storico delle timbrature.
   */
  public List<List<HistoryValue<Stamping>>> getHistoricalList(PersonStampingRecap psDto) {
    List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();

    for (PersonStampingDayRecap day : psDto.daysRecap) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.stampings) {
          if (stamping.markedByAdmin) {
            historyStampingsList.add(stampingHistoryDao.stampings(stamping.id));
          }
        }
      }
    }
    return historyStampingsList;
  }
  
  /**
   * Metodo che ritorna le informazioni sulle timbrature fuori sede.
   * @param psDto il personStampingRecap contenente il recap delle timbrature del dipendente
   * @return la lista contenente tutte le timbrature fuori sede per la persona.
   */
  public List<OffSiteWorkingTemp> getOffSiteStampings(PersonStampingRecap psDto) {
    List<OffSiteWorkingTemp> list = Lists.newArrayList();
    for (PersonStampingDayRecap day : psDto.daysRecap) {
      if (!day.ignoreDay) {
        for (Stamping stamping : day.personDay.stampings) {
          if (stamping.isOffSiteWork()) {
            OffSiteWorkingTemp temp = new OffSiteWorkingTemp();
            temp.date = stamping.personDay.date;
            temp.stamping = stamping;
            temp.reason = stamping.reason;
            temp.place = stamping.place;
            temp.note = stamping.note;
            list.add(temp);
          }            
        }
      }
    }
    
    return list;
  }
}
