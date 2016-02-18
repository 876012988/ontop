package it.unibz.inf.ontop.owlrefplatform.core.unfolding;

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


import java.io.Serializable;
import java.util.List;

import com.google.common.collect.Multimap;

import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.CQIE;
import it.unibz.inf.ontop.model.DatalogProgram;
import it.unibz.inf.ontop.model.OBDAException;

/**
 * This interface should be implemented by any class which implements an
 * unfolding Mechanism which should be integrated into a technique wrapper
 * 
 * @author Manfred Gerstgrasser, mrezk
 * 
 */

public interface UnfoldingMechanism extends Serializable {

	/**
	 * unfolds the the given datalog program applying partial evaluation. It will do the partial evaluation following 
	 * a specified strategy, for instance, bottom up or top down. The parameter includeMappings tells you if the unfolder should take the mappings
	 * into account, or just work with the query
	 * @param query
	 * @param targetPredicate
	 * @param strategy
	 * @param includeMappings
	 * @return
	 * @throws OBDAException
	 */
	public DatalogProgram unfold(DatalogProgram query, String targetPredicate, String strategy,boolean includeMappings, Multimap<Predicate, Integer> multiplePredIdx)
			throws OBDAException;

	public Multimap<Predicate,Integer>   processMultipleTemplatePredicates(
			List<CQIE> unfoldingProgram);



}
