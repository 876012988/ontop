package it.unibz.krdb.obda.owlrefplatform.owlapi3;

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

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPI3TranslatorUtility;
import it.unibz.krdb.obda.owlapi3.QuestOWLIndividualIterator;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.WriterDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class QuestOWLMaterializerCMD {

	public static void main(String args[]) {

		if (args.length != 2 && args.length != 3) {
			System.out.println("Usage");
			System.out.println(" QuestOWLMaterializerCMD  obdafile owlfile->yes/no [outputfile]");
			System.out.println("");
			System.out.println(" obdafile   The full path to the OBDA file");
			System.out.println(" owlfile    yes/no to use the OWL file or not");
			System.out.println(" outputfile [OPTIONAL] The full path to the output file");
			System.out.println("");
			return;
		}

		String obdafile = args[0].trim();
		String yesno = args[1].trim();
		String owlfile = null;
		if (yesno.toLowerCase().equals("yes"))
			owlfile = obdafile.substring(0, obdafile.length()-4) + "owl";
		
		String out = null;
		BufferedOutputStream output = null;
		BufferedWriter writer = null;
		if (args.length == 3) {
			out = args[2].trim();
		}

		try {
			final long startTime = System.currentTimeMillis();
			
			if (out != null) {
				output = new BufferedOutputStream(new FileOutputStream(out)); 
			} else {
				output = new BufferedOutputStream(System.out);
			}
			writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
			
			
			OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
			OBDAModel obdaModel = fac.getOBDAModel();
			ModelIOManager ioManager = new ModelIOManager(obdaModel);
			ioManager.load(obdafile);

			OWLOntology ontology = null;
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLAPI3Materializer materializer = null;
			
			if (owlfile != null) {
			// Loading the OWL ontology from the file as with normal OWLReasoners
				ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));
				Ontology onto =  OWLAPI3TranslatorUtility.translate(ontology);
				obdaModel.declareAll(onto.getVocabulary());
				materializer = new OWLAPI3Materializer(obdaModel, onto);
			}
			else {
				ontology = manager.createOntology();
				materializer = new OWLAPI3Materializer(obdaModel);
			}
			
			//OBDAModelSynchronizer.declarePredicates(ontology, obdaModel);

			QuestOWLIndividualIterator iterator = materializer.getIterator();
	
			while(iterator.hasNext()) 
				manager.addAxiom(ontology, iterator.next());
			manager.saveOntology(ontology, new OWLXMLOntologyFormat(), new WriterDocumentTarget(writer));	
			
			System.out.println("NR of TRIPLES: "+materializer.getTriplesCount());
			System.out.println("VOCABULARY SIZE (NR of QUERIES): "+materializer.getVocabularySize());
			
			materializer.disconnect();
			if (out!=null)
				output.close();
			
			final long endTime = System.currentTimeMillis();
			final long time = endTime - startTime;
			System.out.println("Elapsed time to materialize: "+time + " {ms}");
			
		} catch (Exception e) {
			System.out.println("Error materializing ontology:");
			e.printStackTrace();
		} 

	}

	
}
