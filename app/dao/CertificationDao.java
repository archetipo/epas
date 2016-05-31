package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQueryFactory;

import models.Certification;
import models.Person;
import models.enumerate.CertificationType;
import models.query.QCertification;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * @author alessandro
 */
public class CertificationDao extends DaoBase {


  @Inject
  CertificationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Le certificazioni epas della persona nel mese e nell'anno.
   * @param person
   * @param year
   * @param month
   * @return
   */
  public List<Certification> personCertifications(Person person, int year, int month) {
    
    QCertification certification = QCertification.certification;
    
    return getQueryFactory()
        .from(certification)
        .where(certification.person.eq(person)
            .and(certification.year.eq(year))
            .and(certification.month.eq(month)))
        .list(certification);
  }
  
  /**
   * Le certificationi epas della persona nel mese e nell'anno per il tipo type.
   * @param person
   * @param year
   * @param month
   * @param type
   * @return
   */
  public List<Certification> personCertificationsByType(Person person, int year, int month, CertificationType type) {
    
    QCertification certification = QCertification.certification;
    return getQueryFactory()
        .from(certification)
        .where(certification.person.eq(person)
            .and(certification.year.eq(year))
            .and(certification.month.eq(month))
            .and(certification.certificationType.eq(type)))
        .list(certification);
  }

  
}
