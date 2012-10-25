package org.hibernate.jpql.mongodb;

import junit.framework.Assert;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.sql.ast.common.ParserContext;
import org.hibernate.sql.ast.origin.hql.parse.HQLLexer;
import org.hibernate.sql.ast.origin.hql.parse.HQLParser;
import org.hibernate.sql.ast.origin.hql.resolve.LuceneJPQLWalker;
import org.junit.Test;

import java.util.HashMap;

/**
 * Test that the JSON representation of various MongoDB queries
 * match what is expected for a given JP-QL query
 *
 * Inspired by TreeWalkerTest
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoDBTreeWalkTest {

	private static boolean USE_STDOUT = true;

	@Test
	public void walkTest1() {
		transformationAssert(
				"from SomeEntity" ,
				"{ }" );
	}

	private void transformationAssert(String jpaql, String expectedLuceneQuery) {
		if ( USE_STDOUT ) {
			System.out.println( jpaql );
		}
		HashMap<String,Class> entityNames = new HashMap<String,Class>();
		entityNames.put( "com.acme.SomeEntity", SomeEntity.class );
		entityNames.put( "SomeEntity", SomeEntity.class );
		//generated alias:
		MongoDBJPQLWalker walker = assertTreeParsed( null, jpaql, entityNames );
		Assert.assertTrue( SomeEntity.class.equals( walker.getTargetEntity() ) );
		Assert.assertEquals( expectedLuceneQuery, walker.getMongoDBQuery().toString() );
		if ( USE_STDOUT ) {
			System.out.println( expectedLuceneQuery );
			System.out.println();
		}
	}

	private MongoDBJPQLWalker assertTreeParsed(ParserContext context, String input, HashMap<String,Class> entityNames) {
		HQLLexer lexed = new HQLLexer( new ANTLRStringStream( input ) );
		Assert.assertEquals( 0, lexed.getNumberOfSyntaxErrors() );
		CommonTokenStream tokens = new CommonTokenStream( lexed );

		CommonTree tree = null;
		HQLParser parser = new HQLParser( tokens );
		if ( context != null ) {
			parser.setParserContext( context );
		}
		try {
			HQLParser.statement_return r = parser.statement();
			Assert.assertEquals( 0, parser.getNumberOfSyntaxErrors() );
			tree = (CommonTree) r.getTree();
		}
		catch (RecognitionException e) {
			Assert.fail( e.getMessage() );
		}

		if ( tree != null ) {
			if ( USE_STDOUT ) {
				System.out.println( tree.toStringTree() );
			}
			// To walk the resulting tree we need a treenode stream:
			CommonTreeNodeStream treeStream = new CommonTreeNodeStream( tree );

			// AST nodes have payloads referring to the tokens from the Lexer:
			treeStream.setTokenStream( tokens );

			MapBasedEntityNamesResolver nameResolver = new MapBasedEntityNamesResolver( entityNames );
			// Finally create the treewalker:
			MongoDBJPQLWalker walker = new MongoDBJPQLWalker( treeStream, nameResolver );
			try {
				walker.statement();
				Assert.assertEquals( 0, walker.getNumberOfSyntaxErrors() );
				return walker;
			}
			catch (RecognitionException e) {
				Assert.fail( e.getMessage() );
			}
		}
		return null; // failed
	}
}
