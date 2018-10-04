package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonDayInTrouble;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonDayInTrouble is a Querydsl query type for PersonDayInTrouble
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonDayInTrouble extends EntityPathBase<PersonDayInTrouble> {

    private static final long serialVersionUID = 1711963887L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonDayInTrouble personDayInTrouble = new QPersonDayInTrouble("personDayInTrouble");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.enumerate.Troubles> cause = createEnum("cause", models.enumerate.Troubles.class);

    public final BooleanPath emailSent = createBoolean("emailSent");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonDay personDay;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonDayInTrouble(String variable) {
        this(PersonDayInTrouble.class, forVariable(variable), INITS);
    }

    public QPersonDayInTrouble(Path<? extends PersonDayInTrouble> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonDayInTrouble(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonDayInTrouble(PathMetadata<?> metadata, PathInits inits) {
        this(PersonDayInTrouble.class, metadata, inits);
    }

    public QPersonDayInTrouble(Class<? extends PersonDayInTrouble> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personDay = inits.isInitialized("personDay") ? new QPersonDay(forProperty("personDay"), inits.get("personDay")) : null;
    }

}

