package manager.recaps.personStamping;

import it.cnr.iit.epas.DateUtility;

import javax.inject.Inject;

import dao.wrapper.IWrapperFactory;
import manager.ContractManager;
import manager.ContractMonthRecapManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.ContractMonthRecap;
import models.Person;

public class PersonStampingRecapFactory {

	private final PersonDayManager personDayManager;
	private final ContractMonthRecapManager contractMonthRecapManager;
	private final PersonManager personManager;
	private final PersonStampingDayRecapFactory stampingDayRecapFactory;
	private final IWrapperFactory wrapperFactory;
	private final DateUtility dateUtility;
	
	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonManager personManager,
			ContractMonthRecapManager contractMonthRecapManager,
			PersonResidualYearRecapFactory yearFactory,
			IWrapperFactory wrapperFactory,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			DateUtility dateUtility) {

		this.personDayManager = personDayManager;
		this.contractMonthRecapManager = contractMonthRecapManager;
		this.personManager = personManager;
		this.stampingDayRecapFactory = stampingDayRecapFactory;
		this.wrapperFactory = wrapperFactory;
		this.dateUtility = dateUtility;
	}

	/**
	 * Costruisce il riepilogo mensile delle timbrature. 
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public PersonStampingRecap create(Person person, int year, int month) {

		return new PersonStampingRecap(personDayManager,  personManager,
				contractMonthRecapManager, stampingDayRecapFactory, wrapperFactory, dateUtility,
				year, month, person);
	}

}
