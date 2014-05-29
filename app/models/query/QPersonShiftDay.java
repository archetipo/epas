package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonShiftDay;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonShiftDay is a Querydsl query type for PersonShiftDay
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonShiftDay extends EntityPathBase<PersonShiftDay> {

    private static final long serialVersionUID = -1230493101;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonShiftDay personShiftDay = new QPersonShiftDay("personShiftDay");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonShift personShift;

    public final QShiftTimeTable shiftTimeTable;

    public final QShiftType shiftType;

    public QPersonShiftDay(String variable) {
        this(PersonShiftDay.class, forVariable(variable), INITS);
    }

    public QPersonShiftDay(Path<? extends PersonShiftDay> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShiftDay(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShiftDay(PathMetadata<?> metadata, PathInits inits) {
        this(PersonShiftDay.class, metadata, inits);
    }

    public QPersonShiftDay(Class<? extends PersonShiftDay> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personShift = inits.isInitialized("personShift") ? new QPersonShift(forProperty("personShift"), inits.get("personShift")) : null;
        this.shiftTimeTable = inits.isInitialized("shiftTimeTable") ? new QShiftTimeTable(forProperty("shiftTimeTable")) : null;
        this.shiftType = inits.isInitialized("shiftType") ? new QShiftType(forProperty("shiftType")) : null;
    }

}

