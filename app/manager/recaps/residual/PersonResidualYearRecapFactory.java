package manager.recaps.residual;

import javax.annotation.Nullable;

import models.Contract;

import org.joda.time.LocalDate;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import dao.MealTicketDao;

public class PersonResidualYearRecapFactory {
	
	private final PersonResidualMonthRecapFactory personResidualMonthRecapFactory;
	private final MealTicketDao mealTicketDao;
	
	@Inject
	public PersonResidualYearRecapFactory(PersonResidualMonthRecapFactory monthFactory,
			MealTicketDao mealTicketDao) {
		personResidualMonthRecapFactory = monthFactory;
		this.mealTicketDao = mealTicketDao;
	}
	
	/**
	 * Costruisce la situazione annuale residuale della persona.
	 * @param contract
	 * @param year
	 * @param calcolaFinoA valorizzare questo campo per fotografare la situazione residuale in un certo momento 
	 *   (ad esempio se si vuole verificare la possibilità di prendere riposo compensativo in un determinato giorno). 
	 *   Null se si desidera la situazione residuale a oggi. 
	 * @return un nuovo PersonResidualYearRecap costruito dai parametri
	 * forniti.
	 */
	public PersonResidualYearRecap create(Contract contract, int year, @Nullable LocalDate finoA) {
		
		Preconditions.checkNotNull(contract, "è richiesto un contratto non nullo");
		
		return new PersonResidualYearRecap(mealTicketDao, 
				contract, year, finoA, personResidualMonthRecapFactory);
	}
}