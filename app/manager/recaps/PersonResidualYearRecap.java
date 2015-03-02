package manager.recaps;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import manager.ContractManager;
import models.Contract;
import models.ContractYearRecap;

import org.joda.time.LocalDate;

import com.google.common.collect.ImmutableList;

import dao.MealTicketDao;

/**
 * 
 * @author alessandro
 *
 */
public class PersonResidualYearRecap {
	
	public final List<PersonResidualMonthRecap> mesi;

	public PersonResidualYearRecap(Contract contract, int year, LocalDate calcolaFinoA,
			PersonResidualMonthRecapFactory factory) {
		LocalDate dateStartMealTicket = MealTicketDao.getMealTicketStartDate(contract.person.office);
		
		int firstMonthToCompute = 1;
		LocalDate firstDayInDatabase = new LocalDate(year,1,1);
		DateInterval contractInterval = contract.getContractDateInterval();
		DateInterval requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
		DateInterval mealTicketInterval = new DateInterval(dateStartMealTicket, calcolaFinoA);
		int initMonteOreAnnoPassato = 0;
		int initMonteOreAnnoCorrente = 0;
		int initMealTicket = 0;

		//Recupero situazione iniziale dell'anno richiesto
		ContractYearRecap recapPreviousYear = ContractManager.getContractYearRecap(contract, year-1);
		if(recapPreviousYear!=null)	
		{
			initMonteOreAnnoPassato = recapPreviousYear.remainingMinutesCurrentYear + recapPreviousYear.remainingMinutesLastYear;
			initMealTicket = recapPreviousYear.remainingMealTickets;
		}
		if(contract.sourceDate!=null && contract.sourceDate.getYear()==year)
		{
			initMonteOreAnnoPassato = contract.sourceRemainingMinutesLastYear;
			initMonteOreAnnoCorrente = contract.sourceRemainingMinutesCurrentYear;
			firstDayInDatabase = contract.sourceDate.plusDays(1);
			firstMonthToCompute = contract.sourceDate.getMonthOfYear();
			//TODO initMealTickets da source contract
		}

		final ImmutableList.Builder<PersonResidualMonthRecap> builder = 
				ImmutableList.<PersonResidualMonthRecap> builder();
		
		PersonResidualMonthRecap previous = null;
		int actualMonth = firstMonthToCompute;
		int endMonth = 12;
		if(new LocalDate().getYear()==year)
			endMonth = Math.min(endMonth, new LocalDate().getMonthOfYear());
		while(actualMonth<=endMonth)
		{
			//Prendo la situazione iniziale del mese (se previous è null sono i valori calcolati precedentemente)
			if(previous!=null)
			{
				initMonteOreAnnoPassato = previous.monteOreAnnoPassato;
				initMonteOreAnnoCorrente = previous.monteOreAnnoCorrente;
			}

			LocalDate today = LocalDate.now();

			//////////////////////////////////////////////////////////////////////////////////////////////////////////
			//	Intervallo per progressivi
			//////////////////////////////////////////////////////////////////////////////////////////////////////////

			// 1) Tutti i giorni del mese

			LocalDate monthBeginForPersonDay = new LocalDate(year, actualMonth, 1);
			LocalDate monthEndForPersonDay = monthBeginForPersonDay.dayOfMonth().withMaximumValue();
			DateInterval monthIntervalForPersonDay = new DateInterval(monthBeginForPersonDay, monthEndForPersonDay);

			// 2) Nel caso del calcolo del mese attuale

			if( DateUtility.isDateIntoInterval(today, monthIntervalForPersonDay) )
			{
				// 2.1) Se oggi non è il primo gPersonResidualYearRecap csap = new PersonResidualYearRecap();iorno del mese allora tutti i giorni del mese fino a ieri.
				
				if ( today.getDayOfMonth() != 1 )
				{
					monthEndForPersonDay = today.minusDays(1);
					monthIntervalForPersonDay = new DateInterval(monthBeginForPersonDay, monthEndForPersonDay);
				}

				// 2.2) Se oggi è il primo giorno del mese allora null.

				else
				{
					monthIntervalForPersonDay = null;
				}
			}

			// 3) Filtro per dati nel database e estremi del contratto

			DateInterval validDataForPersonDay = null;
			if(monthIntervalForPersonDay != null)
			{
				validDataForPersonDay = DateUtility.intervalIntersection(monthIntervalForPersonDay, requestInterval);
				validDataForPersonDay = DateUtility.intervalIntersection(validDataForPersonDay, contractInterval);
			}


			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//	Intervallo per riposi compensativi
			////////////////////////////////////////////////////////////////////////////////////////////////////////////

			// 1) Tutti i giorni del mese

			LocalDate monthBeginForCompensatoryRest = new LocalDate(year, actualMonth, 1);
			LocalDate monthEndForCompensatoryRest = monthBeginForCompensatoryRest.dayOfMonth().withMaximumValue();
			DateInterval monthIntervalForCompensatoryRest = new DateInterval(monthBeginForCompensatoryRest, monthEndForCompensatoryRest);

			// 2) Nel caso del mese attuale considero anche il mese successivo

			if( DateUtility.isDateIntoInterval(today, monthIntervalForCompensatoryRest) ) 
			{
				monthEndForCompensatoryRest = monthEndForCompensatoryRest.plusMonths(1).dayOfMonth().withMaximumValue();
				monthIntervalForCompensatoryRest = new DateInterval(monthBeginForCompensatoryRest, monthEndForCompensatoryRest);
			}

			// 3) Filtro per dati nel database e estremi del contratto

			DateInterval validDataForCompensatoryRest = null;
			
			validDataForCompensatoryRest = DateUtility.intervalIntersection(monthIntervalForCompensatoryRest, contractInterval);
			
			//////////////////////////////////////////////////////////////////////////////////////////////////////////
			//	Intervallo per mealTickets
			//////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			// 1) Tutti i giorni del mese
			
			LocalDate monthBeginForMealTickets = new LocalDate(year, actualMonth, 1);
			LocalDate monthEndForMealTickets = monthBeginForMealTickets.dayOfMonth().withMaximumValue();
			DateInterval monthIntervalForMealTickets = new DateInterval(monthBeginForMealTickets, monthEndForMealTickets);
			
			// 2) Nel caso del calcolo del mese attuale
			
			if( DateUtility.isDateIntoInterval(today, monthIntervalForMealTickets) )
			{


				// 2.1) Se oggi non è il primo giorno del mese allora tutti i giorni del mese fino a ieri.
				
				if ( today.getDayOfMonth() != 1 )
				{
					monthEndForMealTickets = today;
					monthIntervalForMealTickets = new DateInterval(monthBeginForMealTickets, monthEndForMealTickets);
				}
				
				// 2.2) Se oggi è il primo giorno del mese allora null.
				
				else
				{
					monthIntervalForMealTickets = null;
				}
			}
			
			// 3) Filtro per dati nel database, estremi del contratto, inizio utilizzo buoni pasto
			
			DateInterval validDataForMealTickets = null;
			if(monthIntervalForMealTickets != null)
			{
				validDataForMealTickets = DateUtility.intervalIntersection(monthIntervalForMealTickets, requestInterval);
				validDataForMealTickets = DateUtility.intervalIntersection(validDataForMealTickets, contractInterval);
				validDataForMealTickets = DateUtility.intervalIntersection(validDataForMealTickets, mealTicketInterval);
			}

			//Costruisco l'oggetto
			PersonResidualMonthRecap mese = factory.create(previous, year, actualMonth, contract, 
					initMonteOreAnnoPassato, initMonteOreAnnoCorrente, initMealTicket,
					validDataForPersonDay, validDataForCompensatoryRest, validDataForMealTickets);
			
			builder.add(mese);
			previous = mese;
			actualMonth++;	
		}
		
		mesi = builder.build();
	}

	/**
	 * 
	 * @param month
	 * @return
	 */
	public PersonResidualMonthRecap getMese(int month){
		if(this.mesi==null)
			return null;
		for(PersonResidualMonthRecap mese : this.mesi)
			if(mese.mese==month)
				return mese;
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public List<PersonResidualMonthRecap> getMesi() {
		return this.mesi;
	}

}
