package models;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import models.base.BaseModel;

import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;


/**
 *
 * @author dario
 *
 */
@Entity
@Table(name="contracts")
public class Contract extends BaseModel {

	private static final long serialVersionUID = -4472102414284745470L;


	@Column(name="source_date")
	public LocalDate sourceDate = null;

	@Column(name="source_vacation_last_year_used")
	public Integer sourceVacationLastYearUsed = null;

	@Column(name="source_vacation_current_year_used")
	public Integer sourceVacationCurrentYearUsed = null;

	@Column(name="source_permission_used")
	public Integer sourcePermissionUsed = null;

	@Column(name="source_recovery_day_used")
	public Integer sourceRecoveryDayUsed = null;

	@Column(name="source_remaining_minutes_last_year")
	public Integer sourceRemainingMinutesLastYear = null;

	@Column(name="source_remaining_minutes_current_year")
	public Integer sourceRemainingMinutesCurrentYear = null;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;

	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	@OrderBy("beginFrom")
	public Set<VacationPeriod> vacationPeriods = Sets.newHashSet();

	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<ContractYearRecap> recapPeriods;

	@Required @NotNull
	@Column(name="begin_contract")
	public LocalDate beginContract;

	@Column(name="expire_contract")
	public LocalDate expireContract;

	//data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...

	@Column(name="end_contract")
	public LocalDate endContract;

	@NotAudited
	@OneToMany(mappedBy = "contract", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	@OrderBy("beginDate")
	public Set<ContractWorkingTimeType> contractWorkingTimeType = Sets.newHashSet();

	@NotAudited
	@OneToMany(mappedBy="contract")
	@OrderBy("startFrom")
	public Set<ContractStampProfile> contractStampProfile = Sets.newHashSet();

	@NotAudited
	@OneToMany(mappedBy="contract", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<MealTicket> mealTickets;

	@Transient
	private List<ContractWorkingTimeType> contractWorkingTimeTypeAsList;


	public void setSourceDate(String date){
		this.sourceDate = new LocalDate(date);
	}

	/**
	 * I contratti con onCertificate = true sono quelli dei dipendenti CNR e
	 * corrispondono a quelli con l'obbligo dell'attestato di presenza
	 * da inviare a Roma
	 */
	@Required
	public boolean onCertificate = false;

	@Override
	public String toString() {
		return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
				id, person.id, beginContract, expireContract, endContract);
	}

	/**
	 * Ritorna il riepilogo annule del contatto.
	 * @param year
	 * @return
	 */
	public ContractYearRecap yearRecap(int year)	{
		for(ContractYearRecap cyr : recapPeriods) {

			if(cyr.year==year)
				return cyr;
		}
		return null;
	}

	/**
	 * Ritorna il ContractStampProfile attivo alla data.
	 * @param contract
	 * @param date
	 * @return
	 */
	public Optional<ContractStampProfile> getContractStampProfileFromDate(LocalDate date) {

		for(ContractStampProfile csp : contractStampProfile) {
			if(csp.dateRange().contains(date)){
				return Optional.fromNullable(csp);
			}
		}
		return Optional.absent();
	}

}



