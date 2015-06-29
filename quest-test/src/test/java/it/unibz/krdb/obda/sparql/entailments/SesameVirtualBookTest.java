package it.unibz.krdb.obda.sparql.entailments;

/*
 * #%L
 * ontop-quest-sesame
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.Before;
import org.junit.Test;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import sesameWrapper.SesameVirtualRepo;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test the ontology exampleBooks for sparql owl entailments.
 * rdfs:subclass, rdfs:subProperty, owl:inverseof owl:equivalentclass owl:equivalentProperty 
 * owl:disjointWith owl:propertyDisjointWith rdfs:domain rdfs:range
 * QuestPreferences has SPARQL_OWL_ENTAILMENT  set to true.
 *
 */
		
public class SesameVirtualBookTest  {

	
	RepositoryConnection con = null;
	Repository repo = null;

	@Before
	public void setUp() {

		try {

			String owlfile = "src/test/resources/subclass/exampleBooks.owl";
			String obdafile = "src/test/resources/subclass/exampleBooks.obda";
			File f = new File("src/test/resources/subclass/subDescription.properties");
			String pref = "file:" + f.getAbsolutePath();

			repo = new SesameVirtualRepo("my_name", owlfile, obdafile, pref);

			repo.initialize();

			con = repo.getConnection();

		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}
	@Test
	public void testSubClasses() throws Exception
	{

		try {
			String queryString = "select * where {?x rdfs:subClassOf ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(18, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");
	}
	@Test
	public void testOneSubClass() {

		try {
			String queryString = "select * where {?x rdfs:subClassOf <http://meraka/moss/exampleBooks.owl#Edition> }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			List<String> valuesResult = new LinkedList<String>();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {

					BindingSet bindingSet = result.next();
					for (String b : bindings) {
						System.out.println(bindingSet.getBinding(b));
						valuesResult.add(bindingSet.getBinding(b).getValue().stringValue());
					}
					countResult++;
				}

				assertEquals(3, countResult);
				assertEquals("http://meraka/moss/exampleBooks.owl#Edition", valuesResult.get(2));
				assertEquals("http://meraka/moss/exampleBooks.owl#SpecialEdition", valuesResult.get(1));
				assertEquals("http://meraka/moss/exampleBooks.owl#EconomicEdition", valuesResult.get(0));
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");
	}
	@Test
	public void testEquivalences() {

		try {
			String queryString = "select * where {?x owl:equivalentClass ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(11, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");

	}
	@Test
	public void testOneEquivalence() {

		try {
			String queryString = "select * where {?x owl:equivalentClass <http://meraka/moss/exampleBooks.owl#Edition> }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			List<String> valuesResult = new LinkedList<String>();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {

					BindingSet bindingSet = result.next();
					for (String b : bindings) {
						System.out.println(bindingSet.getBinding(b));
						valuesResult.add(bindingSet.getBinding(b).getValue().stringValue());
					}
					countResult++;
				}

				assertEquals(1, countResult);

			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");
	}
	@Test
	public void testEquivalentProperties() {

		try {
			String queryString = "select * where {?x owl:equivalentProperty ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(12, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");

	}
	@Test
	public void testRanges() {

		try {
			String queryString = "select * where {?x rdfs:range ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(12, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");

	}
	@Test
	public void testDomains() {

		try {
			String queryString = "select * where {?x rdfs:domain ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(11, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");

	}
	@Test
	public void testDisjoints() {

		try {
			String queryString = "select * where {?x owl:disjointWith ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(6, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");
	}
	@Test
	public void testInverseOf() {

		try {
			String queryString = "select * where {?x owl:inverseOf ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				List<String> bindings = result.getBindingNames();

				int countResult = 0;
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (String b : bindings)
						System.out.println(bindingSet.getBinding(b));
					countResult++;
				}
				assertEquals(6, countResult);
			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");
	}
	@Test
	public void testPropertyDisjoints() {

		try {
			String queryString = "select * where {?x owl:propertyDisjointWith ?y }";

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			try {

				assertFalse(result.hasNext());

			} finally {
				result.close();
			}

			System.out.println("Closing...");

			con.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Done.");
	}

}
