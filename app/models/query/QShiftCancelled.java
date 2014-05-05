package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ShiftCancelled;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QShiftCancelled is a Querydsl query type for ShiftCancelled
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QShiftCancelled extends EntityPathBase<ShiftCancelled> {

    private static final long serialVersionUID = 306636563L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftCancelled shiftCancelled = new QShiftCancelled("shiftCancelled");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QShiftType type;

    public QShiftCancelled(String variable) {
        this(ShiftCancelled.class, forVariable(variable), INITS);
    }

    public QShiftCancelled(Path<? extends ShiftCancelled> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QShiftCancelled(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QShiftCancelled(PathMetadata<?> metadata, PathInits inits) {
        this(ShiftCancelled.class, metadata, inits);
    }

    public QShiftCancelled(Class<? extends ShiftCancelled> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.type = inits.isInitialized("type") ? new QShiftType(forProperty("type")) : null;
    }

}

