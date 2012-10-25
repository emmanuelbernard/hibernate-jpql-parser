package org.hibernate.jpql.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.hibernate.sql.ast.common.JoinType;
import org.hibernate.sql.ast.origin.hql.resolve.EntityNamesResolver;
import org.hibernate.sql.ast.origin.hql.resolve.GeneratedHQLResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * This extends the ANTLR generated AST walker to transform a parsed tree
 * into a MongoDB query and collect the target entity types of the query.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoDBJPQLWalker extends GeneratedHQLResolver {
	private BasicDBObject query = new BasicDBObject();
	private Class<?> targetEntity;
	/**
	 * Persister space: keep track of aliases and entity names.
	 */
	private final Map<String, String> aliasToEntityType = new HashMap<String, String>();

	/**
	 * How to resolve entity names to class instances
	 */
	private final EntityNamesResolver entityNames;

	/**
	 * Set to true when we are walking in the tree area defining a SELECT type/options
	 */
	private boolean definingSelectStrategy;

	public MongoDBJPQLWalker(TreeNodeStream input, EntityNamesResolver entityNames) {
		super( input );
		this.entityNames = entityNames;
	}

	public DBObject getMongoDBQuery() {
		return query;
	}

	public Class<?> getTargetEntity() {
		return targetEntity;
	}

	//code that might be shared by several walkers


	/**
	 * See rule entityName
	 */
	protected void registerPersisterSpace(Tree entityName, Tree alias) {
		String put = aliasToEntityType.put( alias.getText(), entityName.getText() );
		if ( put != null && !put.equalsIgnoreCase( entityName.getText() ) ) {
			throw new UnsupportedOperationException(
					"Alias reuse currently not supported: alias " + alias.getText()
							+ " already assigned to type " + put );
		}
		Class targetedType = entityNames.getClassFromName( entityName.getText() );
		if ( targetedType == null ) {
			throw new IllegalStateException( "Unknown entity name " + entityName.getText() );
		}
		if ( targetEntity != null ) {
			throw new IllegalStateException( "Can't target multiple types: " + targetEntity + " already selected before " + targetedType );
		}
		targetEntity = targetedType;
//		queryBuilder = queryBuildContext.forEntity( targetedType ).get();
	}

	protected boolean isPersisterReferenceAlias() {
		if ( aliasToEntityType.size() == 1 ) {
			return true; // should be safe
		}
		else {
			throw new UnsupportedOperationException( "Unexpected use case: not implemented yet?" );
		}
	}

	protected void pushFromStrategy(
			JoinType joinType,
			Tree assosiationFetchTree,
			Tree propertyFetchTree,
			Tree alias) {
		throw new UnsupportedOperationException( "must be overridden!" );
	}

	protected void pushSelectStrategy() {
		definingSelectStrategy = true;
	}

	protected void popStrategy() {
		definingSelectStrategy = false;
	}
}
