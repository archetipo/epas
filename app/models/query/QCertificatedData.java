package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.CertificatedData;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QCertificatedData is a Querydsl query type for CertificatedData
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCertificatedData extends EntityPathBase<CertificatedData> {

    private static final long serialVersionUID = -122268133;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCertificatedData certificatedData = new QCertificatedData("certificatedData");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath absencesSent = createString("absencesSent");

    public final StringPath cognomeNome = createString("cognomeNome");

    public final StringPath competencesSent = createString("competencesSent");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isOk = createBoolean("isOk");

    public final StringPath matricola = createString("matricola");

    public final NumberPath<Integer> mealTicketSent = createNumber("mealTicketSent", Integer.class);

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath problems = createString("problems");

    public final StringPath trainingHoursSent = createString("trainingHoursSent");

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QCertificatedData(String variable) {
        this(CertificatedData.class, forVariable(variable), INITS);
    }

    public QCertificatedData(Path<? extends CertificatedData> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCertificatedData(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCertificatedData(PathMetadata<?> metadata, PathInits inits) {
        this(CertificatedData.class, metadata, inits);
    }

    public QCertificatedData(Class<? extends CertificatedData> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

