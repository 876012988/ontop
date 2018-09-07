package it.unibz.inf.ontop.datalog;

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

import it.unibz.inf.ontop.model.term.Function;
import it.unibz.inf.ontop.model.term.Variable;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public interface CQIE extends Serializable {

	Function getHead();

	List<Function> getBody();

	void updateBody(List<Function> body);

	CQIE clone();
	
	Set<Variable> getReferencedVariables();
}
