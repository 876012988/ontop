package it.unibz.krdb.obda.reformulation.tests;

/*
 * #%L
 * ontop-quest-owlapi3
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


import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.Description;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.OntologyFactory;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
import it.unibz.krdb.obda.owlapi3.OWLAPI3Translator;
import it.unibz.krdb.obda.owlrefplatform.core.EquivalenceMap;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import it.unibz.krdb.obda.owlrefplatform.core.tboxprocessing.EquivalenceTBoxOptimizer;

import java.io.File;
import java.util.Map;

import junit.framework.TestCase;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class EquivalenceSimplificationTest extends TestCase {

	final String	path	= "src/test/resources/test/equivalence/";

	public void test_equivalence_namedclasses() throws Exception {

		/*
		 * The ontology contains A1 = A2 = A3, B1 ISA A1, B1 = B2 = B3, this
		 * gives 9 inferences and R1 = R2 = R3, S1 ISA R1, S1 = S2 = S3, this
		 * gives 36 inferences (counting inverse related inferences, and exist
		 * related inferences. Total, 45 inferences
		 */

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		File file = new File(path + "test_401.owl");
		OWLOntology owlonto = man.loadOntologyFromOntologyDocument(file);
		OWLAPI3Translator translator = new OWLAPI3Translator();
		Ontology ontology = translator.translate(owlonto);

		TBoxReasoner reasoner = new TBoxReasonerImpl(ontology);
		EquivalenceMap eqMap = EquivalenceMap.getEquivalenceMap(reasoner);
		Ontology simpleonto = EquivalenceTBoxOptimizer.getOptimalTBox(reasoner, eqMap, ontology.getVocabulary());

		assertEquals(3, simpleonto.getConcepts().size());
		assertEquals(0, simpleonto.getRoles().size());
		assertEquals(3, simpleonto.getAssertions().size());
		assertEquals(3, simpleonto.getVocabulary().size());

		OntologyFactory ofac = OntologyFactoryImpl.getInstance();
		OBDADataFactory odfac = OBDADataFactoryImpl.getInstance();

		assertEquals(6, eqMap.keySetSize());
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A2")));
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B2")));
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // Roman: instead of B1
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C2")));
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
		
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A2")));
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B2"))); // Roman: B3 -> B1
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // Roman: B3 <-> B1
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C2")));
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));

	}
	
	
	public void test_equivalence_namedproperties() throws Exception {

		/*
		 * The ontology contains A1 = A2 = A3, B1 ISA A1, B1 = B2 = B3, this
		 * gives 9 inferences and R1 = R2 = R3, S1 ISA R1, S1 = S2 = S3, this
		 * gives 36 inferences (counting inverse related inferences, and exist
		 * related inferences. Total, 45 inferences
		 */

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		File file = new File(path + "test_402.owl");
		OWLOntology owlonto = man.loadOntologyFromOntologyDocument(file);
		OWLAPI3Translator translator = new OWLAPI3Translator();
		Ontology ontology = translator.translate(owlonto);
		
		TBoxReasoner reasoner = new TBoxReasonerImpl(ontology);
		EquivalenceMap eqMap = EquivalenceMap.getEquivalenceMap(reasoner);
		Ontology simpleonto = EquivalenceTBoxOptimizer.getOptimalTBox(reasoner, eqMap, ontology.getVocabulary());

		assertEquals(0, simpleonto.getConcepts().size());
		assertEquals(3, simpleonto.getRoles().size());
		assertEquals(12, simpleonto.getAssertions().size());
		assertEquals(3, simpleonto.getVocabulary().size());

		OntologyFactory ofac = OntologyFactoryImpl.getInstance();
		OBDADataFactory odfac = OBDADataFactoryImpl.getInstance();
		
		assertEquals(6, eqMap.keySetSize());
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A2")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B2")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // ROMAN: B1 and B3 ARE SYMMETRIC
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C2")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
		
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A2")));
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B2"))); // ROMAN: B3 -> B1
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // ROMAN: B3 <-> B1
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C2")));
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
	}
	
	
	public void test_equivalence_namedclassesandexists() throws Exception {

		/*
		 * The ontology contains A1 = A2 = A3, B1 ISA A1, B1 = B2 = B3, this
		 * gives 9 inferences and R1 = R2 = R3, S1 ISA R1, S1 = S2 = S3, this
		 * gives 36 inferences (counting inverse related inferences, and exist
		 * related inferences. Total, 45 inferences
		 */

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		File file = new File(path + "test_403.owl");
		OWLOntology owlonto = man.loadOntologyFromOntologyDocument(file);
		OWLAPI3Translator translator = new OWLAPI3Translator();
		Ontology ontology = translator.translate(owlonto);

		TBoxReasoner reasoner = new TBoxReasonerImpl(ontology);
		EquivalenceMap eqMap = EquivalenceMap.getEquivalenceMap(reasoner);
		Ontology simpleonto = EquivalenceTBoxOptimizer.getOptimalTBox(reasoner, eqMap, ontology.getVocabulary());

		assertEquals(simpleonto.getConcepts().toString(), 3, simpleonto.getConcepts().size());
		assertEquals(3, simpleonto.getRoles().size());
		assertEquals(9, simpleonto.getAssertions().size());
		assertEquals(6, simpleonto.getVocabulary().size());

		OntologyFactory ofac = OntologyFactoryImpl.getInstance();
		OBDADataFactory odfac = OBDADataFactoryImpl.getInstance();

		assertEquals(3, eqMap.keySetSize());
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // Roman: instead of B1
		assertTrue(eqMap.containsKey(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
		
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // Roman B1 <-> B3
		assertEquals(ofac.createClass("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1"),eqMap.getValue(odfac.getClassPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
		
	}
	
	public void test_equivalence_namedproperties_and_inverses() throws Exception {

		/*
		 * The ontology contains A1 = A2 = A3, B1 ISA A1, B1 = B2 = B3, this
		 * gives 9 inferences and R1 = R2 = R3, S1 ISA R1, S1 = S2 = S3, this
		 * gives 36 inferences (counting inverse related inferences, and exist
		 * related inferences. Total, 45 inferences
		 */

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		File file = new File(path + "test_404.owl");
		OWLOntology owlonto = man.loadOntologyFromOntologyDocument(file);
		OWLAPI3Translator translator = new OWLAPI3Translator();
		Ontology ontology = translator.translate(owlonto);

		TBoxReasoner reasoner = new TBoxReasonerImpl(ontology);
		EquivalenceMap eqMap = EquivalenceMap.getEquivalenceMap(reasoner);
		Ontology simpleonto = EquivalenceTBoxOptimizer.getOptimalTBox(reasoner, eqMap, ontology.getVocabulary());

		assertEquals(12, simpleonto.getAssertions().size());
		assertEquals(0, simpleonto.getConcepts().size());
		assertEquals(3, simpleonto.getRoles().size());
		assertEquals(3, simpleonto.getVocabulary().size());

		OntologyFactory ofac = OntologyFactoryImpl.getInstance();
		OBDADataFactory odfac = OBDADataFactoryImpl.getInstance();

		assertEquals(6, eqMap.keySetSize());
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A2")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B2")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3"))); // ROMAN: again, B1 and B3 are symmetric
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C2")));
		assertTrue(eqMap.containsKey(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
		
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1",true),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A2")));
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#A3")));
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1", true),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B2"))); // B3 -> B1
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#B3")));  //  B1 <-> B3
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1", true),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C2")));
		assertEquals(ofac.createObjectProperty("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C1"),eqMap.getValue(odfac.getObjectPropertyPredicate("http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#C3")));
	}

}
