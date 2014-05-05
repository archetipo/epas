package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonHourForOvertime;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonHourForOvertime is a Querydsl query type for PersonHourForOvertime
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonHourForOvertime extends EntityPathBase<PersonHourForOvertime> {

    private static final long serialVersionUID = -1600219059L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonHourForOvertime personHourForOvertime = new QPersonHourForOvertime("personHourForOvertime");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> numberOfHourForOvertime = createNumber("numberOfHourForOvertime", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public QPersonHourForOvertime(String variable) {
        this(PersonHourForOvertime.class, forVariable(variable), INITS);
    }

    public QPersonHourForOvertime(Path<? extends PersonHourForOvertime> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonHourForOvertime(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonHourForOvertime(PathMetadata<?> metadata, PathInits inits) {
        this(PersonHourForOvertime.class, metadata, inits);
    }

    public QPersonHourForOvertime(Class<? extends PersonHourForOvertime> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

