/**
 * 
 */
package models;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;
import net.sf.oval.constraint.MinLength;

import org.hibernate.envers.Audited;

import play.data.validation.Required;


/**
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name = "stamp_types")
public class StampType extends BaseModel {

	private static final long serialVersionUID = 1953695910638450365L;

	/**
	 * Utilizzato come riferimento da passare da parte del client che invia le timbrature al server
	 */
	@Required
	public String code;
	
	@Required
	@MinLength(value=2)
	public String description;	
	
	public String identifier;
	
	@OneToMany(mappedBy="stampType")
	public Set<Stamping> stampings; 
}
