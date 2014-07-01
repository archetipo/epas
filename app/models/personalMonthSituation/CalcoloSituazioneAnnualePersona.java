package models.personalMonthSituation;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import models.Contract;
import models.ContractYearRecap;

import org.joda.time.LocalDate;

public class CalcoloSituazioneAnnualePersona {

		public List<Mese> mesi;
		
		/**
		 * Costruisce la situazione annuale residuale della persona.
		 * @param contract
		 * @param year
		 * @param initializationTime
		 * @param calcolaFinoA valorizzare questo campo per fotografare la situazione residuale in un certo momento 
		 *   (ad esempio se si vuole verificare la possibilità di prendere riposo compensativo in un determinato giorno). 
		 *   Null se si desidera la situazione residuale a oggi. 
		 */
		public CalcoloSituazioneAnnualePersona(Contract contract, int year, LocalDate calcolaFinoA)
		{
			int firstMonthToCompute = 1;
			LocalDate firstDayInDatabase = new LocalDate(year,1,1);
			int initMonteOreAnnoPassato = 0;
			int initMonteOreAnnoCorrente = 0;
			
			if(contract==null)
			{
				return;
			}	

			//Recupero situazione iniziale dell'anno richiesto
			ContractYearRecap recapPreviousYear = contract.getContractYearRecap(year-1);
			if(recapPreviousYear!=null)	
			{
				initMonteOreAnnoPassato = recapPreviousYear.remainingMinutesCurrentYear + recapPreviousYear.remainingMinutesLastYear;
			}
			if(contract.sourceDate!=null && contract.sourceDate.getYear()==year)
			{
				initMonteOreAnnoPassato = contract.sourceRemainingMinutesLastYear;
				initMonteOreAnnoCorrente = contract.sourceRemainingMinutesCurrentYear;
				firstDayInDatabase = contract.sourceDate.plusDays(1);
				firstMonthToCompute = contract.sourceDate.getMonthOfYear();
			}

			this.mesi = new ArrayList<Mese>();
			Mese previous = null;
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
					// 2.1) Se oggi non è il primo giorno del mese allora tutti i giorni del mese fino a ieri.
					
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
					DateInterval requestInterval = new DateInterval(firstDayInDatabase, calcolaFinoA);
					DateInterval contractInterval = contract.getContractDateInterval();
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
				DateInterval contractInterval = contract.getContractDateInterval();
				validDataForCompensatoryRest = DateUtility.intervalIntersection(monthIntervalForCompensatoryRest, contractInterval);
				
				//Costruisco l'oggetto
				Mese mese = new Mese(previous, year, actualMonth, contract, initMonteOreAnnoPassato, initMonteOreAnnoCorrente, validDataForPersonDay, validDataForCompensatoryRest);
				this.mesi.add(mese);
				previous = mese;
				actualMonth++;	
			}
		}
		
		public Mese getMese(int year, int month){
			if(this.mesi==null)
				return null;
			for(Mese mese : this.mesi)
				if(mese.mese==month)
					return mese;
			return null;
		}
		
			
		
}
