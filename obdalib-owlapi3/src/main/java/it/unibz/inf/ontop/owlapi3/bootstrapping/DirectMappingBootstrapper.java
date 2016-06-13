package it.unibz.inf.ontop.owlapi3.bootstrapping;

/*
 * #%L
 * ontop-obdalib-owlapi3
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.injection.NativeQueryLanguageComponentFactory;
import it.unibz.inf.ontop.injection.OBDAFactoryWithException;
import it.unibz.inf.ontop.io.PrefixManager;
import it.unibz.inf.ontop.model.OBDADataFactory;
import it.unibz.inf.ontop.model.OBDADataSource;
import it.unibz.inf.ontop.model.OBDAMappingAxiom;
import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.sql.DBMetadata;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.net.URI;
import java.util.HashMap;

public class DirectMappingBootstrapper extends AbstractDBMetadata{
	
	
	public DirectMappingBootstrapper(String baseuri, String url, String user, String password, String driver,
                                     NativeQueryLanguageComponentFactory nativeQLFactory,
                                     OBDAFactoryWithException obdaFactory) throws Exception{
        super(nativeQLFactory, obdaFactory);
		OBDADataFactory fact = OBDADataFactoryImpl.getInstance();
		OBDADataSource source = fact.getJDBCDataSource(url, user, password, driver);
		//create empty ontology and model, add source to model
		OWLOntologyManager mng = OWLManager.createOWLOntologyManager();
		OWLOntology onto = mng.createOntology(IRI.create(baseuri));
        //TODO: avoid creating a model without mappings
        PrefixManager prefixManager = nativeQLFactory.create(new HashMap<String, String>());
		OBDAModel model = obdaFactory.createOBDAModel(ImmutableSet.of(source),
                new HashMap<URI, ImmutableList<OBDAMappingAxiom>>(), prefixManager);
		getOntologyAndDirectMappings(baseuri, onto, model, source);
	}

	public DirectMappingBootstrapper(String baseUri, OWLOntology ontology, OBDAModel model, OBDADataSource source,
                                     NativeQueryLanguageComponentFactory nativeQLFactory,
                                     OBDAFactoryWithException obdaFactory) throws Exception{
        super(nativeQLFactory, obdaFactory);
		getOntologyAndDirectMappings(baseUri, ontology, model, source);
	}
	
	public DirectMappingBootstrapper(DBMetadata metadata, String baseUri, OWLOntology ontology, OBDAModel model,
                                     OBDADataSource source, NativeQueryLanguageComponentFactory nativeQLFactory,
                                     OBDAFactoryWithException obdaFactory)
            throws Exception{
        super(nativeQLFactory, obdaFactory);
		getOntologyAndDirectMappings(metadata, baseUri, ontology, model, source);
	}

	/***
	 * Creates an OBDA model using direct mappings
	 */
	public OBDAModel getModel() {
		return getOBDAModel();
	}

	/***
	 * Creates an OBDA file using direct mappings. Internally this one calls the
	 * previous one and just renders the file.
	 */
	public OWLOntology getOntology() {
		return getOWLOntology();
	}

}
