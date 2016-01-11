package manager.services.vacations;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;

import manager.services.vacations.VacationsTypeResult.TypeVacation;

import models.Absence;
import models.Contract;
import models.VacationCode;
import models.VacationPeriod;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * @author alessandro
 *
 */
public class VacationsRecapBuilder {
  
  public static final int YEAR_VACATION_UPPER_BOUND = 28;

  /**
   * Costruisce il VacationRecap. 
   * @param year
   * @param contract
   * @param absencesToConsider
   * @param accruedDate
   * @param expireDateLastYear
   * @param expireDateCurrentYear
   * @return
   */
  public VacationsRecap buildVacationRecap(int year, Contract contract, 
      List<Absence> absencesToConsider, LocalDate accruedDate, LocalDate expireDateLastYear, 
      LocalDate expireDateCurrentYear) {

    VacationsRecap vacationsRecap = new VacationsRecap();
    
    DateInterval contractDateInterval =
        new DateInterval(contract.getBeginDate(), contract.calculatedEnd());
    
    //Vacation Last Year Expired
    vacationsRecap.setExpireLastYear(false);
    if (year < LocalDate.now().getYear()) {
      vacationsRecap.setExpireLastYear(true);
    } else if (year == LocalDate.now().getYear()
            && accruedDate.isAfter(expireDateLastYear)) {
      vacationsRecap.setExpireLastYear(true);
    }

    //Contract Expire Before End Of Year / Active After Begin Of Year
    LocalDate startRequestYear = new LocalDate(year, 1, 1);
    LocalDate endRequestYear = new LocalDate(year, 12, 31);
    if (contractDateInterval.getEnd().isBefore(endRequestYear)) {
      vacationsRecap.setExpireBeforeEndYear(true);
    }
    if (contractDateInterval.getBegin().isAfter(startRequestYear)) {
      vacationsRecap.setActiveAfterBeginYear(true);
    }
    
    VacationsRecapTempData tempData = VacationsRecapTempData.builder()
        .year(year)
        .accruedDate(accruedDate)
        .expireDate(expireDateLastYear)
        .absencesToConsider(absencesToConsider)
        .contract(contract).build();
    
    vacationsRecap.setVacationsRequest(VacationsRequest.builder()
        .year(year)
        .contract(contract)
        .contractDateInterval(contractDateInterval)
        .accruedDate(Optional.fromNullable(accruedDate))
        .contractVacationPeriod(contract.getVacationPeriods())
        .postPartumUsed(tempData.getPostPartum())
        .expireDateLastYear(expireDateLastYear)
        .expireDateCurrentYear(expireDateCurrentYear)
        .build());

    vacationsRecap.setVacationsLastYear(buildVacationsTypeResult(
        vacationsRecap.getVacationsRequest(),
        TypeVacation.VACATION_LAST_YEAR,
        FluentIterable.from(tempData.getList32PreviouYear())
        .append(tempData.getList31RequestYear())
        .append(tempData.getList37RequestYear()).toList(),
        tempData.getSourceVacationLastYearUsed()));

    vacationsRecap.setVacationsCurrentYear(buildVacationsTypeResult(
        vacationsRecap.getVacationsRequest(),
        TypeVacation.VACATION_CURRENT_YEAR,
        FluentIterable.from(tempData.getList32RequestYear())
        .append(tempData.getList31NextYear())
        .append(tempData.getList37NextYear()).toList(),
        tempData.getSourceVacationCurrentYearUsed()));

    vacationsRecap.setPermissions(buildVacationsTypeResult(
        vacationsRecap.getVacationsRequest(),
        TypeVacation.PERMISSION_CURRENT_YEAR,
        FluentIterable.from(tempData.getList94RequestYear()).toList(),
        tempData.getSourcePermissionUsed()));

    return vacationsRecap;
  }
  
  
  /**
   * Costruisce il risultato della richiesta per il TypeVacation specifico.
   * @param vacationsRequest
   * @param typeVacation
   * @param absencesUsed
   * @param sourced
   * @return
   */
  private VacationsTypeResult buildVacationsTypeResult(VacationsRequest vacationsRequest, 
      TypeVacation typeVacation, ImmutableList<Absence> absencesUsed, int sourced) {
    
    VacationsTypeResult vacationsTypeResult = new VacationsTypeResult();
    vacationsTypeResult.setVacationsRequest(vacationsRequest);
    vacationsTypeResult.setAbsencesUsed(absencesUsed);
    vacationsTypeResult.setSourced(sourced);
    vacationsTypeResult.setTypeVacation(typeVacation);

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
    vacationsTypeResult.setTotalResult(buildAccruedResult(vacationsTypeResult, totalInterval));
    
    // Fix dei casi particolari nel caso del riepilogo totali quando capita il cambio piano.
    adjustDecision(vacationsTypeResult.getTotalResult());

    // Costruisco il riepilogo delle maturate.
    vacationsTypeResult.setAccruedResult(buildAccruedResult(vacationsTypeResult, accruedInterval));

    return vacationsTypeResult;
  }
  
  /**
   * Risultato per la rischiesta in vacationsTypeResult rispetto all'interval passato. 
   * @param vacationsTypeResult
   * @param interval
   * @return
   */
  private AccruedResult buildAccruedResult(VacationsTypeResult vacationsTypeResult, 
      DateInterval interval) {
    
    AccruedResult accruedResult = new AccruedResult();
    accruedResult.setVacationsResult(vacationsTypeResult);
    accruedResult.setInterval(interval);
    accruedResult.setAccruedConverter(AccruedConverter.builder().build());
    
    for (VacationPeriod vp : vacationsTypeResult.getVacationsRequest()
        .getContractVacationPeriod()) {

      AccruedResultInPeriod accruedResultInPeriod = buildAccruedResultInPeriod(
          accruedResult,
          DateUtility.intervalIntersection(interval, vp.getDateInterval()),
          vp.vacationCode,
          vacationsTypeResult.getVacationsRequest().getPostPartumUsed());
      
      addResult(accruedResult, accruedResultInPeriod); 
    }
    
    return accruedResult;
  }
  
  
  /**
   * Costruisce il sotto risultato relativo al vacationCode per l'intervallo interval.
   * @param parentAccruedResult
   * @param interval
   * @param vacationCode
   * @param absences
   * @return
   */
  private AccruedResultInPeriod buildAccruedResultInPeriod(AccruedResult parentAccruedResult,
      DateInterval interval, VacationCode vacationCode, List<Absence> absences) {
    
    AccruedConverter accruedConverter = new AccruedConverter();
    
    AccruedResultInPeriod accruedResultInPeriod = new AccruedResultInPeriod();
    accruedResultInPeriod.setVacationsResult(parentAccruedResult.getVacationsResult());
    accruedResultInPeriod.setInterval(interval);
    accruedResultInPeriod.setAccruedConverter(AccruedConverter.builder().build());
    accruedResultInPeriod.setVacationCode(vacationCode);

    if (accruedResultInPeriod.getInterval() == null) {
      return accruedResultInPeriod;
    }
    
    //set post partum absences
    for (Absence ab : absences) {
      if (DateUtility.isDateIntoInterval(ab.personDay.date, accruedResultInPeriod.getInterval())) {
        accruedResultInPeriod.getPostPartum().add(ab);
      }
    }
    
    //computation
    
    //TODO: verificare che nel caso dei permessi non considero i giorni postPartum.
    accruedResultInPeriod.setDays(DateUtility.daysInInterval(accruedResultInPeriod.getInterval()) 
        - accruedResultInPeriod.getPostPartum().size());

    int accrued = 0;
    
    //calcolo i giorni maturati col metodo di conversione
    if (accruedResultInPeriod.getVacationsResult().getTypeVacation()
        .equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {

      //this.days = DateUtility.daysInInterval(this.interval);

      if (accruedResultInPeriod.getVacationCode().description.equals("21+3")
          || accruedResultInPeriod.getVacationCode().description.equals("22+3")) {

        accrued = accruedConverter.permissionsPartTime(accruedResultInPeriod.getDays());
      } else {
        accrued = accruedConverter.permissions(accruedResultInPeriod.getDays());
      }

    } else {

      //this.days = DateUtility.daysInInterval(this.interval) - this.postPartum.size();

      if (accruedResultInPeriod.getVacationCode().description.equals("26+4")) {
        accrued = accruedConverter.vacationsLessThreeYears(accruedResultInPeriod.getDays());
      }
      if (accruedResultInPeriod.getVacationCode().description.equals("28+4")) {
        accrued = accruedConverter.vacationsMoreThreeYears(accruedResultInPeriod.getDays());
      }
      if (accruedResultInPeriod.getVacationCode().description.equals("21+3")) {
        accrued = accruedConverter.vacationsPartTimeLessThreeYears(accruedResultInPeriod.getDays());
      }
      if (accruedResultInPeriod.getVacationCode().description.equals("22+3")) {
        accrued = accruedConverter.vacationsPartTimeMoreThreeYears(accruedResultInPeriod.getDays());
      }
    }
    
    accruedResultInPeriod.setAccrued(accrued);
    
    return accruedResultInPeriod;
  }
  
 /**
  * Somma il risultato in period a accruedResult.
  * @param accruedResult il padre
  * @param accruedResultInPeriod il sotto-risultato da sommare
  * @return
  */
 private AccruedResult addResult(AccruedResult accruedResult, 
     AccruedResultInPeriod accruedResultInPeriod) {
   
   accruedResult.getAccruedResultsInPeriod().add(accruedResultInPeriod);
   accruedResult.getPostPartum().addAll(accruedResultInPeriod.getPostPartum());
   accruedResult.setDays(accruedResult.getDays() + accruedResultInPeriod.getDays());
   accruedResult.setAccrued(accruedResult.getAccrued() + accruedResultInPeriod.getAccrued());
   return accruedResult;
 }
  
 /**
  * Aggiusta il calcolo di ferie e permessi totali.
  * 
  * @param accruedResult il risultato da aggiustare.
  * @return il risultato aggiustato.
  */
  private AccruedResult adjustDecision(AccruedResult accruedResult) {

    if (accruedResult.getInterval() == null) {
      return accruedResult;  //niente da aggiustare..
    }

    // per ora i permessi non li aggiusto.
    if (accruedResult.getVacationsResult().getTypeVacation().equals(TypeVacation.PERMISSION_CURRENT_YEAR)) {
      return accruedResult;
    }

    if (accruedResult.getAccruedResultsInPeriod().isEmpty()) {
      return accruedResult;
    }

    DateInterval yearInterval = DateUtility
        .getYearInterval(accruedResult.getVacationsResult().getVacationsRequest().getYear());

    int totalYearPostPartum = 0;
    int totalVacationAccrued = 0;

    AccruedResultInPeriod minAccruedResultInPeriod = null;


    for (AccruedResultInPeriod accruedResultInPeriod : accruedResult.getAccruedResultsInPeriod()) {

      if (minAccruedResultInPeriod == null) {

        minAccruedResultInPeriod = accruedResultInPeriod;

      } else if (accruedResultInPeriod.getVacationCode().vacationDays
          < minAccruedResultInPeriod.getVacationCode().vacationDays ) {

        minAccruedResultInPeriod = accruedResultInPeriod;
      }
      totalYearPostPartum += accruedResultInPeriod.getPostPartum().size();
      totalVacationAccrued += accruedResultInPeriod.getAccrued();

    }

    //Aggiusto perchè l'algoritmo ne ha date troppe.
    if (totalVacationAccrued > YEAR_VACATION_UPPER_BOUND) {
      accruedResult.setFixed(YEAR_VACATION_UPPER_BOUND - totalVacationAccrued);  //negative
    }

    //Aggiusto perchè l'algoritmo ne ha date troppo poche.
    //Condizione: no assenze post partum e periodo che copre tutto l'anno.
    if (totalYearPostPartum == 0
        && accruedResult.getInterval().getBegin().equals(yearInterval.getBegin())
            && accruedResult.getInterval().getEnd().equals(yearInterval.getEnd())) {

      if (minAccruedResultInPeriod.getVacationCode().vacationDays
          > totalVacationAccrued) {

        accruedResult.setFixed(minAccruedResultInPeriod.getVacationCode().vacationDays
            - totalVacationAccrued); //positive
      }
    }

    return accruedResult;
  }
}
