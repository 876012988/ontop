package it.unibz.krdb.obda.owlrefplatform.core.tboxprocessing;

/*
 * #%L
 * ontop-reformulation-core
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

import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.ontology.BasicClassDescription;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.OntologyFactory;
import it.unibz.krdb.obda.ontology.Property;
import it.unibz.krdb.obda.ontology.PropertySomeRestriction;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prune Ontology for redundant assertions based on dependencies
 */
public class SigmaTBoxOptimizer {

	private final TBoxReasoner isa;
	private final TBoxReasoner isaChain;
	private final TBoxReasoner sigmaChain;

	private static final OntologyFactory fac = OntologyFactoryImpl.getInstance();
	private static final Logger	log	= LoggerFactory.getLogger(SigmaTBoxOptimizer.class);

	private final Set<Predicate> vocabulary;
	private Ontology optimizedTBox = null;

	public SigmaTBoxOptimizer(TBoxReasoner isa, Set<Predicate> vocabulary, TBoxReasoner sigma) {
		
		this.vocabulary = vocabulary;
		this.isa = isa;
		
		//isa = new TBoxReasonerImpl(isat);
		isaChain = TBoxReasonerImpl.getChainReasoner((TBoxReasonerImpl)isa);
		
		//TBoxReasonerImpl reasonerSigma = new TBoxReasonerImpl(sigmat);		
		sigmaChain = TBoxReasonerImpl.getChainReasoner((TBoxReasonerImpl)sigma);
	}

	public Ontology getReducedOntology() {
		if (optimizedTBox == null) {
			optimizedTBox = fac.createOntology("http://it.unibz.krdb/obda/auxontology");
			optimizedTBox.addEntities(vocabulary);

			log.debug("Starting semantic-reduction");

			TBoxTraversal.traverse(isa, new TBoxTraverseListener() {

				@Override
				public void onInclusion(Property sub, Property sup) {
					if (!check_redundant_role(sup, sub)) 
						optimizedTBox.addAssertion(fac.createSubPropertyAxiom(sub, sup));
				}

				@Override
				public void onInclusion(BasicClassDescription sub, BasicClassDescription sup) {
					if (!check_redundant(sup, sub)) 
						optimizedTBox.addAssertion(fac.createSubClassAxiom(sub, sup));
				}
			});
		}
		return optimizedTBox;
	}

	
	
	
	
	
	
	
	
	private boolean check_redundant_role(Property parent, Property child) {

		if (check_directly_redundant_role(parent, child))
			return true;
		else {
//			log.debug("Not directly redundant role {} {}", parent, child);
			for (Equivalences<Property> children_prime : isa.getProperties().getDirectSub(isa.getProperties().getVertex(parent))) {
				Property child_prime = children_prime.getRepresentative();

				if (!child_prime.equals(child) && 
						check_directly_redundant_role(child_prime, child) && 
						!check_redundant(child_prime, parent)) {
					return true;
				}
			}
		}
//		log.debug("Not redundant role {} {}", parent, child);

		return false;
	}

	private boolean check_directly_redundant_role(Property parent, Property child) {

		PropertySomeRestriction existParentDesc = 
				fac.getPropertySomeRestriction(parent.getPredicate(), parent.isInverse());
		PropertySomeRestriction existChildDesc = 
				fac.getPropertySomeRestriction(child.getPredicate(), child.isInverse());

		return check_directly_redundant(parent, child) && 
				check_directly_redundant(existParentDesc, existChildDesc);
	}

	private boolean check_redundant(Property parent, Property child) {
		if (check_directly_redundant(parent, child))
			return true;
		else {
			for (Equivalences<Property> children_prime : isa.getProperties().getDirectSub(isa.getProperties().getVertex(parent))) {
				Property child_prime = children_prime.getRepresentative();

				if (!child_prime.equals(child) && 
						check_directly_redundant(child_prime, child) && 
						!check_redundant(child_prime, parent)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean check_redundant(BasicClassDescription parent, BasicClassDescription child) {
		if (check_directly_redundant(parent, child))
			return true;
		else {
			for (Equivalences<BasicClassDescription> children_prime : isa.getClasses().getDirectSub(isa.getClasses().getVertex(parent))) {
				BasicClassDescription child_prime = children_prime.getRepresentative();

				if (!child_prime.equals(child) && 
						check_directly_redundant(child_prime, child) && 
						!check_redundant(child_prime, parent)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean check_directly_redundant(Property parent, Property child) {
		
		Equivalences<Property> sp = sigmaChain.getProperties().getVertex(parent);
		Equivalences<Property> sc = sigmaChain.getProperties().getVertex(child);
		
		// if one of them is not in the respective DAG
		if (sp == null || sc == null) 
			return false;

		Set<Equivalences<Property>> spChildren =  sigmaChain.getProperties().getDirectSub(sp);
		
		if (!spChildren.contains(sc))
			return false;
		
		
		
		Equivalences<Property> tc = isaChain.getProperties().getVertex(child);
		// if one of them is not in the respective DAG
		if (tc == null) 
			return false;
		
		Set<Equivalences<Property>> scChildren = sigmaChain.getProperties().getSub(sc);
		Set<Equivalences<Property>> tcChildren = isaChain.getProperties().getSub(tc);

		return scChildren.containsAll(tcChildren);
	}
	
	private boolean check_directly_redundant(BasicClassDescription parent, BasicClassDescription child) {
		
		Equivalences<BasicClassDescription> sp = sigmaChain.getClasses().getVertex(parent);
		Equivalences<BasicClassDescription> sc = sigmaChain.getClasses().getVertex(child);
		
		// if one of them is not in the respective DAG
		if (sp == null || sc == null) 
			return false;

		Set<Equivalences<BasicClassDescription>> spChildren =  sigmaChain.getClasses().getDirectSub(sp);
		
		if (!spChildren.contains(sc))
			return false;
		
		
		
		Equivalences<BasicClassDescription> tc = isaChain.getClasses().getVertex(child);
		// if one of them is not in the respective DAG
		if (tc == null) 
			return false;
		
		Set<Equivalences<BasicClassDescription>> scChildren = sigmaChain.getClasses().getSub(sc);
		Set<Equivalences<BasicClassDescription>> tcChildren = isaChain.getClasses().getSub(tc);

		return scChildren.containsAll(tcChildren);
	}
	
	public static Ontology getSigmaOntology(TBoxReasoner reasoner) {
		return TBoxReasonerToOntology.getOntology(reasoner, true);
	}
}
