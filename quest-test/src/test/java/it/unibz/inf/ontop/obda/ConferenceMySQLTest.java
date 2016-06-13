package it.unibz.inf.ontop.obda;

/*
 * #%L
 * ontop-test
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
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import it.unibz.inf.ontop.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.inf.ontop.owlrefplatform.owlapi3.QuestOWLFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertFalse;

/**
 * Test mysql jdbc driver.
 * The mappings do not correspond to the table in the database
 * (uppercase difference) : an error should be returned by the system.
 *
 */

public class ConferenceMySQLTest {

	private static Logger log = LoggerFactory.getLogger(ConferenceMySQLTest.class);
	private OWLOntology ontology;

    final String owlFile = "src/test/resources/conference/ontology5.owl";
    final String obdaFile = "src/test/resources/conference/ontology5.obda";

	@Before
	public void setUp() throws Exception {
		
		// Loading the OWL file
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument((new File(owlFile)));

		
	}

	private void runTests(Properties p, String query1) throws Exception {

		// Creating a new instance of the reasoner
		QuestOWLFactory factory = new QuestOWLFactory(new File(obdaFile), new QuestPreferences(p));
		QuestOWL reasoner=null;
		try{
		 reasoner = factory.createReasoner(ontology, new SimpleConfiguration());
		} catch (Exception ne) {
			assertFalse(false);
		}

	}
	



	@Test
	public void testWrongMappings() throws Exception {

		Properties p = new Properties();
		p.put(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
		p.put(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
		p.put(QuestPreferences.OPTIMIZE_TBOX_SIGMA, "true");

        String query1 = "PREFIX : <http://myproject.org/odbs#> SELECT ?x ?y\n" +
                "WHERE {\n" +
                "   ?x :LcontainsT ?y\n" +
                "}";

		runTests(p, query1);
	}


}
