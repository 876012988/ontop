package org.semanticweb.ontop.ontology.impl;

/*
 * #%L
 * ontop-obdalib-core
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

import java.util.Collections;
import java.util.Set;

import org.semanticweb.ontop.ontology.ClassExpression;
import org.semanticweb.ontop.ontology.DisjointClassesAxiom;

public class DisjointClassesAxiomImpl implements DisjointClassesAxiom {

	private static final long serialVersionUID = 4576840836473365808L;
	
	private final Set<ClassExpression> classes;
	
	DisjointClassesAxiomImpl(Set<ClassExpression> classes) {
		if (classes.size() < 2)
			throw new IllegalArgumentException("At least 2 classes are expeccted in DisjointClassAxiom");

		this.classes = classes;
	}
	
	@Override
	public Set<ClassExpression> getClasses() {
		return Collections.unmodifiableSet(classes);
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DisjointClassesAxiomImpl) {
			DisjointClassesAxiomImpl other = (DisjointClassesAxiomImpl)obj;
			return classes.equals(other.classes);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return classes.hashCode();
	}
	
	@Override
	public String toString() {
		return "disjoint(" + classes + ")";
	}
}
