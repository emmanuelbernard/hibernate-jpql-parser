package org.hibernate.jpql.mongodb;

import org.hibernate.sql.ast.origin.hql.resolve.EntityNamesResolver;

import java.util.HashMap;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class MapBasedEntityNamesResolver implements EntityNamesResolver {

	private final HashMap<String, Class> entityNames;

	public MapBasedEntityNamesResolver(HashMap<String, Class> entityNames) {
		this.entityNames = entityNames;
	}

	@Override
	public Class getClassFromName(String entityName) {
		return entityNames.get( entityName );
	}

}
