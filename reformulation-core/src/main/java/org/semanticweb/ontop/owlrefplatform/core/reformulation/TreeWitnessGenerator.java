package org.semanticweb.ontop.owlrefplatform.core.reformulation;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.ontop.ontology.BasicClassDescription;
import org.semanticweb.ontop.ontology.OClass;
import org.semanticweb.ontop.ontology.OntologyFactory;
import org.semanticweb.ontop.ontology.Property;
import org.semanticweb.ontop.ontology.PropertySomeRestriction;
import org.semanticweb.ontop.ontology.impl.OntologyFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeWitnessGenerator {
	private final Property property;
	private final OClass filler;

	private final Set<BasicClassDescription> concepts = new HashSet<BasicClassDescription>();
	private Set<BasicClassDescription> subconcepts;
	private PropertySomeRestriction existsRinv;

	private final TreeWitnessReasonerCache reasoner;

	private static final Logger log = LoggerFactory.getLogger(TreeWitnessGenerator.class);	
	private static final OntologyFactory ontFactory = OntologyFactoryImpl.getInstance();
	
	public TreeWitnessGenerator(TreeWitnessReasonerCache reasoner, Property property, OClass filler) {
		this.reasoner = reasoner;
		this.property = property;
		this.filler = filler;
	}

	void addConcept(BasicClassDescription con) {
		concepts.add(con);
	}
	
	public static Set<BasicClassDescription> getMaximalBasicConcepts(Collection<TreeWitnessGenerator> gens, TreeWitnessReasonerCache reasoner) {
		Set<BasicClassDescription> concepts = new HashSet<BasicClassDescription>();
		for (TreeWitnessGenerator twg : gens) 
			concepts.addAll(twg.concepts);

		if (concepts.isEmpty())
			return concepts;
		
		if (concepts.size() == 1 && concepts.iterator().next() instanceof OClass)
			return concepts;
		
		log.debug("MORE THAN ONE GENERATING CONCEPT: {}", concepts);
		// add all sub-concepts of all \exists R
		Set<BasicClassDescription> extension = new HashSet<BasicClassDescription>();
		for (BasicClassDescription b : concepts) 
			if (b instanceof PropertySomeRestriction)
				extension.addAll(reasoner.getSubConcepts(b));
		concepts.addAll(extension);
		
		// use all concept names to subsume their sub-concepts
		{
			boolean modified = true; 
			while (modified) {
				modified = false;
				for (BasicClassDescription b : concepts) 
					if (b instanceof OClass) {
						Set<BasicClassDescription> bsubconcepts = reasoner.getSubConcepts(b);
						Iterator<BasicClassDescription> i = concepts.iterator();
						while (i.hasNext()) {
							BasicClassDescription bp = i.next();
							if ((b != bp) && bsubconcepts.contains(bp)) { 
								i.remove();
								modified = true;
							}
						}
						if (modified)
							break;
					}
			}
		}
		
		// use all \exists R to subsume their sub-concepts of the form \exists R
		{
			boolean modified = true;
			while (modified) {
				modified = false;
				for (BasicClassDescription b : concepts) 
					if (b instanceof PropertySomeRestriction) {
						PropertySomeRestriction some = (PropertySomeRestriction)b;
						Set<Property> bsubproperties = reasoner.getSubProperties(some.getPredicate(), some.isInverse());
						Iterator<BasicClassDescription> i = concepts.iterator();
						while (i.hasNext()) {
							BasicClassDescription bp = i.next();
							if ((b != bp) && (bp instanceof PropertySomeRestriction)) {
								PropertySomeRestriction somep = (PropertySomeRestriction)bp;
								Property propp = ontFactory.createProperty(somep.getPredicate(), somep.isInverse());
								
								if (bsubproperties.contains(propp)) {
									i.remove();
									modified = true;
								}
							}
						}
						if (modified)
							break;
					}
			}
		}
		
		return concepts;
	}
	
	
	public Set<BasicClassDescription> getSubConcepts() {
		if (subconcepts == null) {
			subconcepts = new HashSet<BasicClassDescription>();
			for (BasicClassDescription con : concepts)
				subconcepts.addAll(reasoner.getSubConcepts(con));
		}
		return subconcepts;
	}
	
	
	public Property getProperty() {
		return property;
	}
	
	public boolean endPointEntailsAnyOf(Set<BasicClassDescription> subc) {
		if (existsRinv == null)
			existsRinv = ontFactory.createPropertySomeRestriction(property.getPredicate(), !property.isInverse());	
		
		return subc.contains(existsRinv) || subc.contains(filler);
	}
	
	@Override 
	public String toString() {
		return "tw-generator E" + property.toString() + "." + filler.toString();
	}
	
	@Override
	public int hashCode() {
		return property.hashCode() ^ filler.hashCode();
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other instanceof TreeWitnessGenerator) {
			TreeWitnessGenerator o = (TreeWitnessGenerator)other;
			return (this.property.equals(o.property) && this.filler.equals(o.filler));		
		}
		return false;
	}
}
