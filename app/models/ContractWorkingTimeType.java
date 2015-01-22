package models;

import it.cnr.iit.epas.DateInterval;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;


/**
 * 
 * @author alessandro
 */
@Entity
@Table(name = "contracts_working_time_types")
public class ContractWorkingTimeType extends BaseModel implements Comparable<ContractWorkingTimeType> {

	private static final long serialVersionUID = 3730183716240278997L;

	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="contract_id")
	public Contract contract;
	
	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="working_time_type_id")
	public WorkingTimeType workingTimeType;
	
	//  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_date")
	public LocalDate beginDate;
	
	//  @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_date")
	public LocalDate endDate;
	
	/**
	 * Comparator ContractWorkingTimeType
	 */
	public int compareTo(ContractWorkingTimeType compareCwtt)
	{
		if (beginDate.isBefore(compareCwtt.beginDate))
			return -1;
		else if (beginDate.isAfter(compareCwtt.beginDate))
			return 1;
		else
			return 0; 
	}
	
	public DateInterval getCwttDateInterval() {
		
		return new DateInterval(beginDate, endDate);
	}
	
}
