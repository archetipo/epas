package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import controllers.Security;
import dao.PersonDao;
import play.db.jpa.Model;
 
 
@Entity
@Audited
@Table(name = "office")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="discriminator",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue(value="O")
public class Office extends Model{
 
    @Column(name = "name")
    public String name;
    
    @Column(name = "contraction")
    public String contraction;
    
    @Column(name = "address")
    public String address = "";
    
    @Column(name = "code")
    public Integer code = 0;
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<RemoteOffice> remoteOffices = new ArrayList<RemoteOffice>();
     
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Person> persons = new ArrayList<Person>();
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<ConfGeneral> confGeneral = new ArrayList<ConfGeneral>();
    
    @OneToMany(mappedBy="office", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<ConfYear> confYear = new ArrayList<ConfYear>();
    
    @NotAudited
    @OneToMany(mappedBy="office", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
    public List<UsersRolesOffices> usersRolesOffices = new ArrayList<UsersRolesOffices>();
    
    @NotAudited
	@OneToMany(mappedBy="office", fetch=FetchType.LAZY)
	public List<WorkingTimeType> workingTimeType = new ArrayList<WorkingTimeType>();
    
    
    /**
     * Ritorna il numero di dipendenti attivi registrati nella sede e nelle sottosedi
     * @return
     */
    public List<Person> getActivePersons() {
    
    	List<Office> officeList = this.getSubOfficeTree();
    	LocalDate date = new LocalDate();
    	
    	List<Person> activePerson = Person.getActivePersonsSpeedyInPeriod(date, date,
    			officeList, false);
    	
    	//TODOOFF capire perchè con person dao non funziona!!!
    	/*
		List<Person> activePerson = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(this.getSubOfficeTree()), 
				false, 
				date, 
				date)
				.list();
				*/
    	return activePerson;
    	
    }
    
    /**
     * Ritorna la lista di tutte le sedi gerarchicamente sotto a Office
     * @return
     */
    private List<Office> getSubOfficeTree() {
    	
    	List<Office> officeToCompute = new ArrayList<Office>();
    	List<Office> officeComputed = new ArrayList<Office>();
    	
    	officeToCompute.add(this);
    	while(officeToCompute.size() != 0) {
    		
    		Office office = officeToCompute.get(0);
    		officeToCompute.remove(office);
    		
    		for(Office remoteOffice : office.remoteOffices) {
    			
    			//Office temp = Office.find("byId", remoteOffice.id).first();
    			//officeToCompute.add(temp);
    			officeToCompute.add((Office)remoteOffice);
    		}
    		
    		officeComputed.add(office);
    	}
    	return officeComputed;
    }
    
    

}