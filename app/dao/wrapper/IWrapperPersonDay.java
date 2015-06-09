package dao.wrapper;

import models.Contract;
import models.PersonDay;
import models.WorkingTimeTypeDay;

import com.google.common.base.Optional;

/**
 * @author alessandro
 *
 */
public interface IWrapperPersonDay extends IWrapperModel<PersonDay> {

	/**
	 * Il contratto cui appartiene il person day. Istanzia una variabile Lazy.
	 * 
	 * @return Optional.absent() se non esiste contratto alla data.
	 */
	Optional<Contract> getPersonDayContract();

	/**
	 * True se il PersonDay cade in un tipo tirmbratura fixed. Istanzia una 
	 * variabile Lazy.
	 * 
	 * @return
	 */
	boolean isFixedTimeAtWork();

	/**
	 * Il tipo orario giornaliero del personDay. Istanzia una variabile Lazy. 
	 * 
	 * @return Optional.absent() in caso di mancanza contratto o di tipo orario.
	 */
	Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay();

	/**
	 * Il personDay precedente solo se immediatamente consecutivo.
	 * 
	 * @return Optiona.absent() in caso di giorno non consecutivo 
	 * o primo giorno del contratto
	 */
	Optional<PersonDay> getPreviousForNightStamp();

	/**
	 * Il personDay precedente per il calcolo del progressivo.
	 * 
	 * @return
	 */
	Optional<PersonDay> getPreviousForProgressive();

}
