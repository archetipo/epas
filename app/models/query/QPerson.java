package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.Person;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.DateTimePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;


/**
 * QPerson is a Querydsl query type for Person
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPerson extends EntityPathBase<Person> {

    private static final long serialVersionUID = -1261627527L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPerson person = new QPerson("person");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath badgeNumber = createString("badgeNumber");

    public final DatePath<org.joda.time.LocalDate> birthday = createDate("birthday", org.joda.time.LocalDate.class);

    public final DateTimePath<java.util.Date> bornDate = createDateTime("bornDate", java.util.Date.class);

    public final ListPath<models.CertificatedData, QCertificatedData> certificatedData = this.<models.CertificatedData, QCertificatedData>createList("certificatedData", models.CertificatedData.class, QCertificatedData.class, PathInits.DIRECT2);

    public final StringPath cnr_email = createString("cnr_email");

    public final ListPath<models.CompetenceCode, QCompetenceCode> competenceCode = this.<models.CompetenceCode, QCompetenceCode>createList("competenceCode", models.CompetenceCode.class, QCompetenceCode.class, PathInits.DIRECT2);

    public final ListPath<models.Competence, QCompetence> competences = this.<models.Competence, QCompetence>createList("competences", models.Competence.class, QCompetence.class, PathInits.DIRECT2);

    public final ListPath<models.Contract, QContract> contracts = this.<models.Contract, QContract>createList("contracts", models.Contract.class, QContract.class, PathInits.DIRECT2);

    public final StringPath department = createString("department");

    public final StringPath email = createString("email");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath fax = createString("fax");

    public final StringPath headOffice = createString("headOffice");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<models.InitializationAbsence, QInitializationAbsence> initializationAbsences = this.<models.InitializationAbsence, QInitializationAbsence>createList("initializationAbsences", models.InitializationAbsence.class, QInitializationAbsence.class, PathInits.DIRECT2);

    public final ListPath<models.InitializationTime, QInitializationTime> initializationTimes = this.<models.InitializationTime, QInitializationTime>createList("initializationTimes", models.InitializationTime.class, QInitializationTime.class, PathInits.DIRECT2);

    public final ListPath<models.MealTicket, QMealTicket> mealTicketsAdmin = this.<models.MealTicket, QMealTicket>createList("mealTicketsAdmin", models.MealTicket.class, QMealTicket.class, PathInits.DIRECT2);

    public final StringPath mobile = createString("mobile");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> number = createNumber("number", Integer.class);

    public final QOffice office;

    public final NumberPath<Long> oldId = createNumber("oldId", Long.class);

    public final StringPath othersSurnames = createString("othersSurnames");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonChildren, QPersonChildren> personChildren = this.<models.PersonChildren, QPersonChildren>createList("personChildren", models.PersonChildren.class, QPersonChildren.class, PathInits.DIRECT2);

    public final ListPath<models.PersonDay, QPersonDay> personDays = this.<models.PersonDay, QPersonDay>createList("personDays", models.PersonDay.class, QPersonDay.class, PathInits.DIRECT2);

    public final QPersonHourForOvertime personHourForOvertime;

    public final ListPath<models.PersonMonthRecap, QPersonMonthRecap> personMonths = this.<models.PersonMonthRecap, QPersonMonthRecap>createList("personMonths", models.PersonMonthRecap.class, QPersonMonthRecap.class, PathInits.DIRECT2);

    public final QPersonShift personShift;

    public final ListPath<models.PersonYear, QPersonYear> personYears = this.<models.PersonYear, QPersonYear>createList("personYears", models.PersonYear.class, QPersonYear.class, PathInits.DIRECT2);

    public final QQualification qualification;

    public final QPersonReperibility reperibility;

    public final StringPath room = createString("room");

    public final ListPath<models.ShiftType, QShiftType> shiftTypes = this.<models.ShiftType, QShiftType>createList("shiftTypes", models.ShiftType.class, QShiftType.class, PathInits.DIRECT2);

    public final StringPath surname = createString("surname");

    public final StringPath telephone = createString("telephone");

    public final QUser user;

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public final BooleanPath wantEmail = createBoolean("wantEmail");

    public QPerson(String variable) {
        this(Person.class, forVariable(variable), INITS);
    }

    public QPerson(Path<? extends Person> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPerson(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPerson(PathMetadata<?> metadata, PathInits inits) {
        this(Person.class, metadata, inits);
    }

    public QPerson(Class<? extends Person> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.personHourForOvertime = inits.isInitialized("personHourForOvertime") ? new QPersonHourForOvertime(forProperty("personHourForOvertime"), inits.get("personHourForOvertime")) : null;
        this.personShift = inits.isInitialized("personShift") ? new QPersonShift(forProperty("personShift"), inits.get("personShift")) : null;
        this.qualification = inits.isInitialized("qualification") ? new QQualification(forProperty("qualification")) : null;
        this.reperibility = inits.isInitialized("reperibility") ? new QPersonReperibility(forProperty("reperibility"), inits.get("reperibility")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

