package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.StampType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QStampType is a Querydsl query type for StampType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QStampType extends EntityPathBase<StampType> {

    private static final long serialVersionUID = 1278906105L;

    public static final QStampType stampType = new QStampType("stampType");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final StringPath code = createString("code");

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath identifier = createString("identifier");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final SetPath<models.Stamping, QStamping> stampings = this.<models.Stamping, QStamping>createSet("stampings", models.Stamping.class, QStamping.class, PathInits.DIRECT2);

    public QStampType(String variable) {
        super(StampType.class, forVariable(variable));
    }

    public QStampType(Path<? extends StampType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStampType(PathMetadata<?> metadata) {
        super(StampType.class, metadata);
    }

}

