package models;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.db.jpa.Model;
 
@Entity
@Audited
@Table(name="remote_office")
@DiscriminatorValue("R")
public class RemoteOffice extends Office {
 
    @Column(name="joining_date")
    @Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
    public LocalDate joiningDate;
     
    @ManyToOne
    @JoinColumn(name="office_id")
    public Office office;
}
