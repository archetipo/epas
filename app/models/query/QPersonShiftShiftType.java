package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonShiftShiftType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonShiftShiftType is a Querydsl query type for PersonShiftShiftType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonShiftShiftType extends EntityPathBase<PersonShiftShiftType> {

    private static final long serialVersionUID = -228204461L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonShiftShiftType personShiftShiftType = new QPersonShiftShiftType("personShiftShiftType");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonShift personShift;

    public final QShiftType shiftType;

    public QPersonShiftShiftType(String variable) {
        this(PersonShiftShiftType.class, forVariable(variable), INITS);
    }

    public QPersonShiftShiftType(Path<? extends PersonShiftShiftType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShiftShiftType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShiftShiftType(PathMetadata<?> metadata, PathInits inits) {
        this(PersonShiftShiftType.class, metadata, inits);
    }

    public QPersonShiftShiftType(Class<? extends PersonShiftShiftType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personShift = inits.isInitialized("personShift") ? new QPersonShift(forProperty("personShift"), inits.get("personShift")) : null;
        this.shiftType = inits.isInitialized("shiftType") ? new QShiftType(forProperty("shiftType"), inits.get("shiftType")) : null;
    }

}

