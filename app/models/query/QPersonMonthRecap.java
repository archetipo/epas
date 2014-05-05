package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonMonthRecap;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonMonthRecap is a Querydsl query type for PersonMonthRecap
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonMonthRecap extends EntityPathBase<PersonMonthRecap> {

    private static final long serialVersionUID = -642314632L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonMonthRecap personMonthRecap = new QPersonMonthRecap("personMonthRecap");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> fromDate = createDate("fromDate", org.joda.time.LocalDate.class);

    public final BooleanPath hoursApproved = createBoolean("hoursApproved");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final DatePath<org.joda.time.LocalDate> toDate = createDate("toDate", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> trainingHours = createNumber("trainingHours", Integer.class);

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QPersonMonthRecap(String variable) {
        this(PersonMonthRecap.class, forVariable(variable), INITS);
    }

    public QPersonMonthRecap(Path<? extends PersonMonthRecap> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonMonthRecap(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonMonthRecap(PathMetadata<?> metadata, PathInits inits) {
        this(PersonMonthRecap.class, metadata, inits);
    }

    public QPersonMonthRecap(Class<? extends PersonMonthRecap> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

