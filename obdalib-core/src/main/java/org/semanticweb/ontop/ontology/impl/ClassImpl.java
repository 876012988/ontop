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

import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.ontology.OClass;

public class ClassImpl implements OClass {

	private static final long serialVersionUID = -4930755519806785384L;

	private final Predicate predicate;
	private final String string;

	ClassImpl(Predicate p) {
		this.predicate = p;
		string = predicate.toString();
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClassImpl)) {
			return false;
		}
		ClassImpl concept2 = (ClassImpl) obj;
		return (predicate.equals(concept2.getPredicate()));
	}
	
	@Override
	public int hashCode() {
		return string.hashCode();
	}

	@Override
	public String toString() {
		return string;
	}
}
