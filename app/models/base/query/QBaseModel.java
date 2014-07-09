package models.base.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.base.BaseModel;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QBaseModel is a Querydsl query type for BaseModel
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
public class QBaseModel extends EntityPathBase<BaseModel> {

    private static final long serialVersionUID = 721081311;

    public static final QBaseModel baseModel = new QBaseModel("baseModel");

    public final play.db.jpa.query.QGenericModel _super = new play.db.jpa.query.QGenericModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QBaseModel(String variable) {
        super(BaseModel.class, forVariable(variable));
    }

    public QBaseModel(Path<? extends BaseModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseModel(PathMetadata<?> metadata) {
        super(BaseModel.class, metadata);
    }

}

