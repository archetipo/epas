package dao.wrapper.function;

import javax.inject.Inject;

import models.Contract;
import models.Office;
import models.Person;
import models.WorkingTimeType;

import com.google.common.base.Function;

import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperWorkingTimeType;

public class WrapperModelFunctionFactory {
	
	private final IWrapperFactory factory;

	@Inject
	WrapperModelFunctionFactory(IWrapperFactory factory) {
		this.factory = factory;
	}
	
	public Function<WorkingTimeType, IWrapperWorkingTimeType> workingTimeType() {
		return new Function<WorkingTimeType, IWrapperWorkingTimeType>() {

			@Override
			public IWrapperWorkingTimeType apply(WorkingTimeType input) {
				return factory.create(input);
			}
		};
	}

	public Function<Person, IWrapperPerson> person() {
		return new Function<Person, IWrapperPerson>() {

			@Override
			public IWrapperPerson apply(Person input) {
				return factory.create(input);
			}
		};
	}
	
	public Function<Office, IWrapperOffice> office() {
		return new Function<Office, IWrapperOffice>() {

			@Override
			public IWrapperOffice apply(Office input) {
				return factory.create(input);
			}
		};
	}
	
	public Function<Contract, IWrapperContract> contract() {
		return new Function<Contract, IWrapperContract>() {

			@Override
			public IWrapperContract apply(Contract input) {
				return factory.create(input);
			}
		};
	}
}

