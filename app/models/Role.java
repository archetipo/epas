package models;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;



@Entity
@Audited
@Table(name="roles")
public class Role extends BaseModel{

	private static final long serialVersionUID = 6717202212924325368L;
	
	public final static String EMPLOYEE = "employee";
	public final static String PERSONNEL_ADMIN = "personnelAdmin";
	public final static String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
	public final static String BADGE_READER = "badgeReader";
	public final static String SUPER_ADMIN = "superAdmin";
	public final static String REST_CLIENT = "restClient";
	
	public String name;
	
	@ManyToMany(fetch = FetchType.EAGER)
	public Set<Permission> permissions = Sets.newHashSet();
	
    @NotAudited
    @OneToMany(mappedBy="role", cascade = {CascadeType.REMOVE}, orphanRemoval=true)
    public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();
    
}
