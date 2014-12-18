package dao;

import java.util.List;

import helpers.ModelQuery;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.query.QContract;
import models.query.QContractStampProfile;
import models.query.QContractWorkingTimeType;
import models.query.QInitializationAbsence;
import models.query.QInitializationTime;

/**
 * 
 * @author dario
 *
 */
public class ContractDao {

	/**
	 * 
	 * @param id
	 * @return il contratto corrispondente all'id passato come parametro
	 */
	public static Contract getContractById(Long id){
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract)
				.where(contract.id.eq(id));
		return query.singleResult(contract);
	}
	
	/**
	 * 
	 * @param person
	 * @return la lista di contratti associati alla persona person passata come parametro ordinati per data inizio contratto
	 */
	public static List<Contract> getPersonContractList(Person person){
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract)
				.where(contract.person.eq(person)).orderBy(contract.beginContract.asc());
		return query.list(contract);
	}
	
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative ai ContractStampProfile per evitare di creare una classe specifica che contenga     */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/
	
	/**
	 * 
	 * @param person
	 * @return la lista dei contractStampProfile relativi alla persona person o al contratto contract passati come parametro 
	 * e ordinati per data inizio del contractStampProfile
	 * La funzione permette di scegliere quale dei due parametri indicare per effettuare la ricerca. Sono mutuamente esclusivi
	 */
	public static List<ContractStampProfile> getPersonContractStampProfile(Optional<Person> person, Optional<Contract> contract){
		QContractStampProfile csp = QContractStampProfile.contractStampProfile;
		final BooleanBuilder condition = new BooleanBuilder();
		if(person.isPresent())
			condition.and(csp.contract.person.eq(person.get()));
		if(contract.isPresent())
			condition.and(csp.contract.eq(contract.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(csp)
				.where(condition).orderBy(csp.startFrom.asc());
		return query.list(csp);
		
	}
	
	/**
	 * 
	 * @param id
	 * @return il contractStampProfile relativo all'id passato come parametro
	 */
	public static ContractStampProfile getContractStampProfileById(Long id){
		QContractStampProfile csp = QContractStampProfile.contractStampProfile;
		final JPQLQuery query = ModelQuery.queryFactory().from(csp)
				.where(csp.id.eq(id));
		return query.singleResult(csp);
	}
	
	
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative agli InitializationTime per evitare di creare una classe specifica che contenga     */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/
	
	/**
	 * 
	 * @param person
	 * @return l'initializationTime relativo alla persona passata come parametro
	 */
	public static InitializationTime getInitializationTime(Person person){
		QInitializationTime init = QInitializationTime.initializationTime;
		final JPQLQuery query = ModelQuery.queryFactory().from(init)
				.where(init.person.eq(person));
		return query.singleResult(init);
		
	}
	
	/**
	 * 
	 * @param id
	 * @return l'initializationTime relativo all'id passato come parametro
	 */
	public static InitializationTime getInitializationTimeById(Long id){
		QInitializationTime init = QInitializationTime.initializationTime;
		final JPQLQuery query = ModelQuery.queryFactory().from(init)
				.where(init.id.eq(id));
		return query.singleResult(init);
	}
	
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative agli InitializationAbsence per evitare di creare una classe specifica che contenga  */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/

	/**
	 * 
	 * @param id
	 * @return l'initializationTime relativo all'id passato come parametro
	 */
	public static InitializationAbsence getInitializationAbsenceById(Long id){
		QInitializationAbsence init = QInitializationAbsence.initializationAbsence;
		final JPQLQuery query = ModelQuery.queryFactory().from(init)
				.where(init.id.eq(id));
		return query.singleResult(init);
	}
	
	/******************************************************************************************************************************************/
	/*Inserisco in questa parte del Dao le query relative ai ContractWorkingTimeType per evitare di creare una classe specifica che contenga  */
	/*una o al più due query e risulti pertanto troppo dispersiva                                                                             */
	/******************************************************************************************************************************************/
	
	
	/**
	 * 
	 * @param contract
	 * @return la lista di contractWorkingTimeType associati al contratto passato come parametro
	 */
	public static List<ContractWorkingTimeType> getContractWorkingTimeTypeList(Contract contract){
		QContractWorkingTimeType cwtt = QContractWorkingTimeType.contractWorkingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(cwtt)
				.where(cwtt.contract.eq(contract));
		return query.list(cwtt);
		
	}
}
