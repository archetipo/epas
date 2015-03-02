package dao.wrapper;

import java.util.List;

import manager.PersonManager;
import models.Contract;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * @author marco
 *
 */
public class WrapperContract implements IWrapperContract {

	private final PersonManager personManager;
	
	private final Contract value;

	@Inject
	WrapperContract(@Assisted Contract contract, PersonManager personManager) {
		value = contract;
		this.personManager = personManager;
	}

	@Override
	public Contract getValue() {
		return value;
	}
	
	/**
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean isLastInMonth(int month, int year) {
		List<Contract> contractInMonth = personManager.getMonthContracts(this.value.person, month, year);
		if (contractInMonth.size() == 0)
			return false;
		if (contractInMonth.get(contractInMonth.size()-1).id.equals(this.value.id))
			return true;
		else
			return false;
	}

}
