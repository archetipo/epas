package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Contract;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContract is a Querydsl query type for Contract
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContract extends EntityPathBase<Contract> {

    private static final long serialVersionUID = 2041582646;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContract contract = new QContract("contract");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> beginContract = createDate("beginContract", org.joda.time.LocalDate.class);

    public final SimplePath<it.cnr.iit.epas.DateInterval> contractDatabaseDateInterval = createSimple("contractDatabaseDateInterval", it.cnr.iit.epas.DateInterval.class);

    public final SimplePath<it.cnr.iit.epas.DateInterval> contractDateInterval = createSimple("contractDateInterval", it.cnr.iit.epas.DateInterval.class);

    public final SetPath<models.ContractStampProfile, QContractStampProfile> contractStampProfile = this.<models.ContractStampProfile, QContractStampProfile>createSet("contractStampProfile", models.ContractStampProfile.class, QContractStampProfile.class, PathInits.DIRECT2);

    public final ListPath<models.VacationPeriod, QVacationPeriod> contractVacationPeriods = this.<models.VacationPeriod, QVacationPeriod>createList("contractVacationPeriods", models.VacationPeriod.class, QVacationPeriod.class, PathInits.DIRECT2);

    public final SetPath<models.ContractWorkingTimeType, QContractWorkingTimeType> contractWorkingTimeType = this.<models.ContractWorkingTimeType, QContractWorkingTimeType>createSet("contractWorkingTimeType", models.ContractWorkingTimeType.class, QContractWorkingTimeType.class, PathInits.DIRECT2);

    public final QVacationPeriod currentVacationPeriod;

    public final DatePath<org.joda.time.LocalDate> endContract = createDate("endContract", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> expireContract = createDate("expireContract", org.joda.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath onCertificate = createBoolean("onCertificate");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final BooleanPath properContract = createBoolean("properContract");

    public final ListPath<models.ContractYearRecap, QContractYearRecap> recapPeriods = this.<models.ContractYearRecap, QContractYearRecap>createList("recapPeriods", models.ContractYearRecap.class, QContractYearRecap.class, PathInits.DIRECT2);

    public final DatePath<org.joda.time.LocalDate> sourceDate = createDate("sourceDate", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> sourcePermissionUsed = createNumber("sourcePermissionUsed", Integer.class);

    public final NumberPath<Integer> sourceRecoveryDayUsed = createNumber("sourceRecoveryDayUsed", Integer.class);

    public final NumberPath<Integer> sourceRemainingMinutesCurrentYear = createNumber("sourceRemainingMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> sourceRemainingMinutesLastYear = createNumber("sourceRemainingMinutesLastYear", Integer.class);

    public final NumberPath<Integer> sourceVacationCurrentYearUsed = createNumber("sourceVacationCurrentYearUsed", Integer.class);

    public final NumberPath<Integer> sourceVacationLastYearUsed = createNumber("sourceVacationLastYearUsed", Integer.class);

    public final SetPath<models.VacationPeriod, QVacationPeriod> vacationPeriods = this.<models.VacationPeriod, QVacationPeriod>createSet("vacationPeriods", models.VacationPeriod.class, QVacationPeriod.class, PathInits.DIRECT2);

    public QContract(String variable) {
        this(Contract.class, forVariable(variable), INITS);
    }

    public QContract(Path<? extends Contract> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContract(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContract(PathMetadata<?> metadata, PathInits inits) {
        this(Contract.class, metadata, inits);
    }

    public QContract(Class<? extends Contract> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.currentVacationPeriod = inits.isInitialized("currentVacationPeriod") ? new QVacationPeriod(forProperty("currentVacationPeriod"), inits.get("currentVacationPeriod")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

