package it.unibz.inf.ontop.si.dag;

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


import com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.spec.ontology.*;
import it.unibz.inf.ontop.spec.ontology.impl.TBoxReasonerImpl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Representation of the named part of the property and class DAGs  
 *     based on the DAGs provided by a TBoxReasonerImpl
 * 
 * WARNING: THIS CLASS IS FOR TESTING ONLY 
 */
@Deprecated
public class TestTBoxReasonerImpl_Named implements TBoxReasoner {

	private final TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<ObjectPropertyExpression, ObjectPropertyExpression> objectPropertyDAG;
	private final TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<DataPropertyExpression, DataPropertyExpression> dataPropertyDAG;
	private final TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<ClassExpression, OClass> classDAG;
	private final TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<DataRangeExpression, Datatype> dataRangeDAG;
	private final TBoxReasoner reasoner;

	public TestTBoxReasonerImpl_Named(TBoxReasoner reasoner) {
		this.objectPropertyDAG = new TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<>(ImmutableMap.of(),
				new EquivalencesDAGImpl<>(reasoner.objectProperties().dag()));
		this.dataPropertyDAG = new TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<>(ImmutableMap.of(),
				new EquivalencesDAGImpl<>(reasoner.dataProperties().dag()));
		this.classDAG = new TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<>(ImmutableMap.of(),
				new EquivalencesDAGImpl<>(reasoner.classes().dag()));
		this.dataRangeDAG = new TBoxReasonerImpl.ClassifiedOntologyVocabularyCategoryImpl<>(ImmutableMap.of(),
				new EquivalencesDAGImpl<>(reasoner.dataRanges().dag()));
		this.reasoner = reasoner;
	}


	@Override
	public ClassifiedOntologyVocabularyCategory<ObjectPropertyExpression, ObjectPropertyExpression> objectProperties() {
		return objectPropertyDAG;
	}

	@Override
	public ClassifiedOntologyVocabularyCategory<DataPropertyExpression, DataPropertyExpression> dataProperties() {
		return dataPropertyDAG;
	}

	@Override
	public ClassifiedOntologyVocabularyCategory<ClassExpression, OClass> classes() {
		return classDAG;
	}

	@Override
	public ClassifiedOntologyVocabularyCategory<DataRangeExpression, Datatype> dataRanges() {
		return dataRangeDAG;
	}

	// DUMMY

	@Override
	public ClassifiedOntologyVocabularyCategory<AnnotationProperty, AnnotationProperty> annotationProperties() {
		return null;
	}


	/**
	 * Reconstruction of the Named DAG (as EquivalencesDAG) from a DAG
	 *
	 * @param <T> Property or BasicClassDescription
	 */
	
	public static final class EquivalencesDAGImpl<T> implements EquivalencesDAG<T> {

		private final EquivalencesDAG<T> reasonerDAG;
		
		EquivalencesDAGImpl(EquivalencesDAG<T> reasonerDAG) {
			this.reasonerDAG = reasonerDAG;
		}
		
		@Override
		public Iterator<Equivalences<T>> iterator() {
			LinkedHashSet<Equivalences<T>> result = new LinkedHashSet<>();
			
			for (Equivalences<T> e : reasonerDAG) {
				Equivalences<T> nodes = getVertex(e.getRepresentative());
				if (nodes != null)
					result.add(nodes);			
			}
			return result.iterator();
		}

		@Override
		public Equivalences<T> getVertex(T desc) {

			// either all elements of the equivalence set are there or none!
			Equivalences<T> vertex = reasonerDAG.getVertex(desc);
			if (vertex.isIndexed())
				return vertex;
			else
				return null;
		}

		
		@Override
		public Set<Equivalences<T>> getDirectSub(Equivalences<T> v) {
			LinkedHashSet<Equivalences<T>> result = new LinkedHashSet<>();

			for (Equivalences<T> e : reasonerDAG.getDirectSub(v)) {
				T child = e.getRepresentative();
				
				// get the child node and its equivalent nodes
				Equivalences<T> namedEquivalences = getVertex(child);
				if (namedEquivalences != null)
					result.add(namedEquivalences);
				else 
					result.addAll(getDirectSub(e)); // recursive call if the child is not empty
			}
			return result;
		}

		@Override
		public Set<Equivalences<T>> getSub(Equivalences<T> v) {
			LinkedHashSet<Equivalences<T>> result = new LinkedHashSet<>();
			
			for (Equivalences<T> e : reasonerDAG.getSub(v)) {
				Equivalences<T> nodes = getVertex(e.getRepresentative());
				if (nodes != null)
					result.add(nodes);			
			}
			return result;
		}

		@Override
		public Set<T> getSubRepresentatives(T v) {
			Equivalences<T> eq = reasonerDAG.getVertex(v);
			LinkedHashSet<T> result = new LinkedHashSet<>();
			
			for (Equivalences<T> e : reasonerDAG.getSub(eq)) {
				Equivalences<T> nodes = getVertex(e.getRepresentative());
				if (nodes != null)
					result.add(nodes.getRepresentative());			
			}
			return result;
		}		

		@Override
		public Set<Equivalences<T>> getDirectSuper(Equivalences<T> v) {
			LinkedHashSet<Equivalences<T>> result = new LinkedHashSet<>();
			
			for (Equivalences<T> e : reasonerDAG.getDirectSuper(v)) {
				T parent = e.getRepresentative();
				
				// get the child node and its equivalent nodes
				Equivalences<T> namedEquivalences = getVertex(parent);
				if (namedEquivalences != null)
					result.add(namedEquivalences);
				else 
					result.addAll(getDirectSuper(e)); // recursive call if the parent is not named
			}
			return result;
		}
		
		@Override
		public Set<Equivalences<T>> getSuper(Equivalences<T> v) {
			LinkedHashSet<Equivalences<T>> result = new LinkedHashSet<>();

			for (Equivalences<T> e : reasonerDAG.getSuper(v)) {
				Equivalences<T> nodes = getVertex(e.getRepresentative());
				if (nodes != null)
					result.add(nodes);			
			}
			
			return result;
		}

		@Override
		public T getCanonicalForm(T v) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
