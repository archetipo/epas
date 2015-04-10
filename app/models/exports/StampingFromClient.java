/**
 * 
 */
package models.exports;

import models.BadgeReader;
import models.StampType;

import org.joda.time.LocalDateTime;

/**
 * @author cristian
 *
 */
public class StampingFromClient {

	public Integer inOut;
	public BadgeReader badgeReader;
	public StampType stampType;
	public Long personId;
	
	public LocalDateTime dateTime;
	
}
