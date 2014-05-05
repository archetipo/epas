package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.VacationPeriod;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QVacationPeriod is a Querydsl query type for VacationPeriod
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QVacationPeriod extends EntityPathBase<VacationPeriod> {

    private static final long serialVersionUID = -1413922014L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVacationPeriod vacationPeriod = new QVacationPeriod("vacationPeriod");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final DatePath<org.joda.time.LocalDate> beginFrom = createDate("beginFrom", org.joda.time.LocalDate.class);

    public final QContract contract;

    public final DatePath<org.joda.time.LocalDate> endTo = createDate("endTo", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QVacationCode vacationCode;

    public QVacationPeriod(String variable) {
        this(VacationPeriod.class, forVariable(variable), INITS);
    }

    public QVacationPeriod(Path<? extends VacationPeriod> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QVacationPeriod(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QVacationPeriod(PathMetadata<?> metadata, PathInits inits) {
        this(VacationPeriod.class, metadata, inits);
    }

    public QVacationPeriod(Class<? extends VacationPeriod> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.vacationCode = inits.isInitialized("vacationCode") ? new QVacationCode(forProperty("vacationCode")) : null;
    }

}

