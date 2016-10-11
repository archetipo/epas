package models.exports;

import models.BadgeReader;
import models.enumerate.StampTypes;

import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative ad una timbratura.
 * 
 * @author cristian
 */
public class StampingFromClient {

  public Integer inOut;
  public BadgeReader badgeReader;
  public StampTypes stampType;
  public Long personId;
  public boolean markedByAdmin = false;
  public LocalDateTime dateTime;

}
