package manager.services.vacations.impl;

import com.google.common.collect.ImmutableList;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;
import lombok.Getter;

import manager.services.vacations.IVacationsTypeResult;
import manager.services.vacations.impl.VacationsRecap.VacationsRequest;

import models.Absence;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import play.Logger;

/**
 * Il risultato per il TypeVacation per la richiesta vacationsRequest, 
 * considerando le absenceUsed e i sourced.
 * 
 * @author alessandro
 */
public class VacationsTypeResult implements IVacationsTypeResult {
  
  public enum TypeVacation {
    VACATION_LAST_YEAR,
    VACATION_CURRENT_YEAR,
    PERMISSION_CURRENT_YEAR,
  }

  // DATI INPUT
  @Getter private final VacationsRequest vacationsRequest;
  @Getter private final TypeVacation typeVacation;
  @Getter private ImmutableList<Absence> absencesUsed;
  @Getter private int sourced;
  
  //DATI OUTPUT
  @Getter private AccruedResult totalResult;
  @Getter private AccruedResult accruedResult;
  @Getter private LocalDate expire;
  
  /**
   * Costruttore del risultato.
   * @param vacationsRequest dati della richiesta (comuni a ogni tipo).
   * @param typeVacation il tipo di assenza.
   * @param absencesUsed assenze usate del tipo di assenza.
   * @param sourced assenze da inizializzazion del tipo di assenza.
   */
  @Builder
  public VacationsTypeResult(VacationsRequest vacationsRequest, TypeVacation typeVacation,
      ImmutableList<Absence> absencesUsed, int sourced) {
    
    this.vacationsRequest = vacationsRequest;
    this.absencesUsed = absencesUsed;
    this.sourced = sourced;
    this.typeVacation = typeVacation;
       
    // Intervallo totale
    DateInterval totalInterval = new DateInterval(new LocalDate(vacationsRequest.getYear(), 1, 1), 
        new LocalDate(vacationsRequest.getYear(), 12, 31));
    if (typeVacation.equals(TypeVacation.VACATION_LAST_YEAR)) {
      totalInterval = new DateInterval(new LocalDate(vacationsRequest.getYear() - 1, 1, 1), 
          new LocalDate(vacationsRequest.getYear() - 1, 12, 31));
    }
    
    // Intervallo accrued
    DateInterval accruedInterval = new DateInterval(totalInterval.getBegin(), 
        totalInterval.getEnd());
    if (typeVacation.equals(TypeVacation.VACATION_CURRENT_YEAR) 
        || typeVacation.equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {
      accruedInterval = new DateInterval(new LocalDate(vacationsRequest.getYear(), 1, 1), 
          vacationsRequest.getAccruedDate());
    }

    //Intersezioni col contratto.
    accruedInterval = DateUtility.intervalIntersection(accruedInterval, 
        vacationsRequest.getContractDateInterval());
    totalInterval = DateUtility.intervalIntersection(totalInterval, 
        vacationsRequest.getContractDateInterval());

    // Costruisco il riepilogo delle totali.
    this.totalResult = AccruedResult.builder()
        .vacationsTypeResult(this)
        .interval(totalInterval)
        .build();

    for (VacationPeriod vp : vacationsRequest.getContractVacationPeriod()) {
      this.totalResult.addResult(

          AccruedResultInPeriod.buildAccruedResultInPeriod(
              this.totalResult,
              DateUtility.intervalIntersection(totalInterval, vp.getDateInterval()),
              vp.vacationCode)

          .setPostPartumAbsences(vacationsRequest.getPostPartumUsed())
          .compute());
    }
    // Fix dei casi particolari nel caso del riepilogo totali quando capita il cambio piano.
    this.totalResult.adjustDecision();

    // Costruisco il riepilogo delle maturate.
    this.accruedResult = AccruedResult.builder()
        .vacationsTypeResult(this)
        .interval(accruedInterval)
        .build();

    for (VacationPeriod vp : vacationsRequest.getContractVacationPeriod()) {
      this.accruedResult.addResult(

          AccruedResultInPeriod.buildAccruedResultInPeriod(
              this.totalResult,
              DateUtility.intervalIntersection(accruedInterval, vp.getDateInterval()),
              vp.vacationCode)

          .setPostPartumAbsences(vacationsRequest.getPostPartumUsed())
          .compute());
    }

    return;
  }
  
  /**
   * Numero di assenze usate.
   */
  public Integer getUsed() {
    return this.absencesUsed.size() + this.sourced;
  }
  
  /**
   * Numero di assenze totali.
   */
  public Integer getTotal() {
    return this.totalResult.accrued + this.totalResult.fixed;
  }
  
  /**
   * Numero di assenze maturate.
   */
  public Integer getAccrued() {
    
    return this.accruedResult.accrued + this.totalResult.fixed; 
  }
  
  /**
   * Rimanenti totali (indipendentemente che siano prendibili, non maturate o scadute).
   */
  public Integer getNotYetUsedTotal() {
    return this.getTotal() - this.getUsed();
  }

  /**
   * Rimanenti maturate (prendibili). Per i determinati solo le maturate, per gli indeterminati
   * tutte. Per le ferie dell'anno precedente considera la data di scadenza ferie della sede.
   */
  public Integer getNotYetUsedAccrued() {

    // caso delle ferie anno passato.
    if (this.typeVacation.equals(TypeVacation.VACATION_LAST_YEAR)) {
      LocalDate expireDate = this.vacationsRequest.getExpireDateLastYear();

      if (this.vacationsRequest.isConsiderExpireDate()) {
        if (this.vacationsRequest.getAccruedDate().isAfter(expireDate)) {
          return 0;
        }
      }
      return this.getAccrued() - this.getUsed();
    }

    //altri casi permessi e ferie anno corrente
    if (DateUtility.isInfinity(this.vacationsRequest.getContractDateInterval().getEnd())) {  
      //per i determinati considero le maturate (perchè potrebbero decidere di cambiare contratto)
      return this.getAccrued() - this.getUsed();
    } else {
      return this.getTotal() - this.getUsed();
    }

  }
  
  /**
   * La data di scadenza utilizzo ferie.
   */
  public LocalDate getExpireDate() {
    
    LocalDate computedEndContract = this.vacationsRequest.getContractDateInterval().getEnd();
   
    if (this.typeVacation.equals(TypeVacation.VACATION_LAST_YEAR)) {
      if (computedEndContract.isBefore(this.vacationsRequest.getExpireDateLastYear())) {
        return computedEndContract;
      } 
      return this.vacationsRequest.getExpireDateLastYear(); 
    }
    
    if (this.typeVacation.equals(TypeVacation.VACATION_CURRENT_YEAR)) {
      if (computedEndContract.isBefore(this.vacationsRequest.getExpireDateCurrentYear())) {
        return computedEndContract;
      } 
      return this.vacationsRequest.getExpireDateCurrentYear(); 
    }
    
    if (this.typeVacation.equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {
      LocalDate endYear = new LocalDate(this.vacationsRequest.getYear(), 12, 31);
      if (computedEndContract.isBefore(endYear)) {
        return computedEndContract;
      } 
      return endYear; 
    }
    return null;
  }

}



