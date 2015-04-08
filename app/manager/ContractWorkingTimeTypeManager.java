package manager;

import java.util.Iterator;

import models.Contract;
import models.ContractWorkingTimeType;

import org.joda.time.LocalDate;

public class ContractWorkingTimeTypeManager {

	/**
	 * 
	 * @param cwtt
	 * @param splitDate
	 */
	public static void saveSplitContractWorkingTimeType(ContractWorkingTimeType cwtt, LocalDate splitDate){
		ContractWorkingTimeType cwtt2 = new ContractWorkingTimeType();
		cwtt2.contract = cwtt.contract;
		cwtt2.beginDate = splitDate;
		cwtt2.endDate = cwtt.endDate;
		cwtt2.workingTimeType = cwtt.workingTimeType;
		cwtt2.save();

		cwtt.endDate = splitDate.minusDays(1);
		cwtt.save();
	}
	
	/**
	 * 
	 * @param contract
	 * @param index
	 * @param cwtt
	 */
	public static void deleteContractWorkingTimeType(Contract contract, int index, ContractWorkingTimeType cwtt){
		ContractWorkingTimeType previous = contract.getContractWorkingTimeTypeAsList().get(index-1);
		previous.endDate = cwtt.endDate;
		previous.save();

		//Safe remove from hibernate set of elements (pattern da riutilizzare quando serve)
		for (Iterator<ContractWorkingTimeType> i = contract.contractWorkingTimeType.iterator(); i.hasNext();) {
		    ContractWorkingTimeType cwttt = i.next();
		    if(cwttt.id.equals(cwtt.id)) {
				i.remove();
			}
		}

		cwtt.delete();
	}
}
