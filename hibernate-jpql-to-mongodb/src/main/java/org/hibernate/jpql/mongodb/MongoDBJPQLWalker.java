package org.hibernate.jpql.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.sql.ast.common.JoinType;
import org.hibernate.sql.ast.origin.hql.resolve.EntityNamesResolver;
import org.hibernate.sql.ast.origin.hql.resolve.GeneratedHQLResolver;
import org.hibernate.sql.ast.origin.hql.resolve.path.PathedPropertyReference;
import org.hibernate.sql.ast.origin.hql.resolve.path.PathedPropertyReferenceSource;

import java.util.Collections;
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
	
	private final Map<String, Object> namedParameters;
	
	private String propertyName;

	public MongoDBJPQLWalker(TreeNodeStream input,
							 EntityNamesResolver entityNames) {
		this( input, entityNames, Collections.EMPTY_MAP );
	}

	public MongoDBJPQLWalker(TreeNodeStream input,
							 EntityNamesResolver entityNames, 
							 Map<String,Object> namedParameters) {
		super( input );
		this.entityNames = entityNames;
		this.namedParameters = namedParameters;
	}

	public DBObject getMongoDBQuery() {
		return query;
	}

	public Class<?> getTargetEntity() {
		return targetEntity;
	}

	protected Tree normalizePropertyPathTerminus(PathedPropertyReferenceSource source, Tree propertyNameNode) {
		// receives the property name on a specific entity reference _source_
		this.propertyName = propertyNameNode.toString();
		return null;
	}
	
	/**
	 * This implements the equality predicate; the comparison
	 * predicate could be a constant, a subfunction or
	 * some random type parameter.
	 * The tree node has all details but with current tree rendering
	 * it just passes it's text so we have to figure out the options again.
	 */
	protected void predicateEquals(final String comparativePredicate) {
		final Object comparison = fromNamedQuery( comparativePredicate );
		String comparisonTerm = valueToString( comparison );
		//FIXME do something with the boolean and / or
		query.append( propertyName, comparisonTerm );
	}


	
	//TODO is it a helper method?	
	private Object fromNamedQuery(String comparativePredicate) {
		if ( comparativePredicate.startsWith( ":" ) ) {
			return namedParameters.get( comparativePredicate.substring( 1 ) );
		}
		else {
			return comparativePredicate;
		}
	}

	//TODO should be shared?
	// might need to be valueToMongoDBValue
	private String valueToString(Object comparison) {
		return comparison.toString();
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

	protected PathedPropertyReferenceSource normalizeQualifiedRoot(Tree identifier381) {
		return new PathedPropertyReference( identifier381.getText(), aliasToEntityType );
	}
}
