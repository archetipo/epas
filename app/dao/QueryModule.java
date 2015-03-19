package dao;

import javax.persistence.EntityManager;

import play.db.jpa.JPA;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

public class QueryModule extends AbstractModule {

	@Provides
	public EntityManager getEntityManager() {
		return JPA.em();
	}
	
	@Provides
	public JPQLQueryFactory createJPQLQueryFactory(Provider<EntityManager> emp) {
		return new JPAQueryFactory(emp);
	}
	
	@Override
	protected void configure() {
		
	}

}
