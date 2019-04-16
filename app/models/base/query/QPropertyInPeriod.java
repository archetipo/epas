package models.base.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.base.PropertyInPeriod;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QPropertyInPeriod is a Querydsl query type for PropertyInPeriod
 */
@Generated("com.querydsl.codegen.SupertypeSerializer")
public class QPropertyInPeriod extends EntityPathBase<PropertyInPeriod> {

    private static final long serialVersionUID = -380616364L;

    public static final QPropertyInPeriod propertyInPeriod = new QPropertyInPeriod("propertyInPeriod");

    public final QPeriodModel _super = new QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPropertyInPeriod(String variable) {
        super(PropertyInPeriod.class, forVariable(variable));
    }

    public QPropertyInPeriod(Path<? extends PropertyInPeriod> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPropertyInPeriod(PathMetadata metadata) {
        super(PropertyInPeriod.class, metadata);
    }

}

