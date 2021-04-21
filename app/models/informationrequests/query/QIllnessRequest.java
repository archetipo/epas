package models.informationrequests.query;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.*;
import com.querydsl.core.types.dsl.PathInits;
import javax.annotation.Generated;
import models.informationrequests.IllnessRequest;



/**
 * QIllnessRequest is a Querydsl query type for IllnessRequest.
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QIllnessRequest extends EntityPathBase<IllnessRequest> {

  private static final long serialVersionUID = 199906257L;

  private static final PathInits INITS = PathInits.DIRECT2;

  public static final QIllnessRequest illnessRequest = new QIllnessRequest("illnessRequest");

  public final models.base.query.QInformationRequest _super;

  //inherited
  public final BooleanPath administrativeApprovalRequired;

  //inherited
  public final DateTimePath<java.time.LocalDateTime> administrativeApproved;

  public final DatePath<java.time.LocalDate> beginDate = 
      createDate("beginDate", java.time.LocalDate.class);

  public final DatePath<java.time.LocalDate> endDate = 
      createDate("endDate", java.time.LocalDate.class);

  //inherited
  public final DateTimePath<java.time.LocalDateTime> endTo;

  //inherited
  public final SimplePath<Object> entityId;

  //inherited
  public final ListPath<models.informationrequests.InformationRequestEvent, 
      QInformationRequestEvent> events;

  //inherited
  public final BooleanPath flowEnded;

  //inherited
  public final BooleanPath flowStarted;

  //inherited
  public final BooleanPath fullyApproved;

  //inherited
  public final NumberPath<Long> id;

  //inherited
  public final EnumPath<models.enumerate.InformationType> informationType;

  public final StringPath name = createString("name");

  //inherited
  public final BooleanPath officeHeadApprovalRequired;

  //inherited
  public final DateTimePath<java.time.LocalDateTime> officeHeadApproved;

  //inherited
  public final BooleanPath persistent;

  // inherited
  public final models.query.QPerson person;

  //inherited
  public final DateTimePath<java.time.LocalDateTime> startAt;

  //inherited
  public final NumberPath<Integer> version;

  public QIllnessRequest(String variable) {
    this(IllnessRequest.class, forVariable(variable), INITS);
  }

  public QIllnessRequest(Path<? extends IllnessRequest> path) {
    this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
  }

  public QIllnessRequest(PathMetadata metadata) {
    this(metadata, PathInits.getFor(metadata, INITS));
  }

  public QIllnessRequest(PathMetadata metadata, PathInits inits) {
    this(IllnessRequest.class, metadata, inits);
  }

  /**
   * Costruttore.
   * @param type il tipo
   * @param metadata metadati
   * @param inits dati iniziali
   */
  public QIllnessRequest(Class<? extends IllnessRequest> type, PathMetadata metadata, 
      PathInits inits) {
    super(type, metadata, inits);
    this._super = new models.base.query.QInformationRequest(type, metadata, inits);
    this.administrativeApprovalRequired = _super.administrativeApprovalRequired;
    this.administrativeApproved = _super.administrativeApproved;
    this.endTo = _super.endTo;
    this.entityId = _super.entityId;
    this.events = _super.events;
    this.flowEnded = _super.flowEnded;
    this.flowStarted = _super.flowStarted;
    this.fullyApproved = _super.fullyApproved;
    this.id = _super.id;
    this.informationType = _super.informationType;
    this.officeHeadApprovalRequired = _super.officeHeadApprovalRequired;
    this.officeHeadApproved = _super.officeHeadApproved;
    this.persistent = _super.persistent;
    this.person = _super.person;
    this.startAt = _super.startAt;
    this.version = _super.version;
  }

}

