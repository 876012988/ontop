package org.semanticweb.ontop.owlrefplatform.core.dag;

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
import java.util.Set;

import org.semanticweb.ontop.ontology.OntologyFactory;
import org.semanticweb.ontop.ontology.PropertySomeRestriction;
import org.semanticweb.ontop.ontology.impl.OntologyFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to transform/manipulate DAGs into "chain-reachability" DAGs.
 */
@Deprecated
public class DAGChain {

	private final static Logger	log	= LoggerFactory.getLogger(DAGChain.class);

	/***
	 * Modifies the DAG so that \exists R = \exists R-, so that the reachability
	 * relation of the original DAG gets extended to the reachability relation
	 * of T and Sigma chains.
	 * 
	 * @param dag
	 */
	public static void getChainDAG(DAG dag) {
		Collection<DAGNode> nodes = new HashSet<DAGNode>(dag.getAllnodes().values());
		OntologyFactory fac = OntologyFactoryImpl.getInstance();
		HashSet<DAGNode> processedNodes = new HashSet<DAGNode>();
		for (DAGNode node : nodes) {
			if (!(node.getDescription() instanceof PropertySomeRestriction) || processedNodes.contains(node)) {
				continue;
			}

			/*
			 * Adding a cycle between exists R and exists R- for each R.
			 */

			PropertySomeRestriction existsR = (PropertySomeRestriction) node.getDescription();
			PropertySomeRestriction existsRin = fac.createPropertySomeRestriction(existsR.getPredicate(), !existsR.isInverse());
			DAGNode existsNode = node;
			DAGNode existsInvNode = dag.getNode(existsRin);
			Set<DAGNode> childrenExist = new HashSet<DAGNode>(existsNode.getChildren());
			Set<DAGNode> childrenExistInv = new HashSet<DAGNode>(existsInvNode.getChildren());

			for (DAGNode child : childrenExist) {
				DAGOperations.addParentEdge(child, existsInvNode);
			}
			for (DAGNode child : childrenExistInv) {
				DAGOperations.addParentEdge(child, existsNode);
			}
			
			Set<DAGNode> parentExist = new HashSet<DAGNode>(existsNode.getParents());
			Set<DAGNode> parentsExistInv = new HashSet<DAGNode>(existsInvNode.getParents());

			for (DAGNode parent : parentExist) {
				DAGOperations.addParentEdge(existsInvNode, parent);
			}
			for (DAGNode parent : parentsExistInv) {
				DAGOperations.addParentEdge(existsNode,parent);
			}

			processedNodes.add(existsInvNode);
			processedNodes.add(existsNode);
		}

		/* Collapsing the cycles */

		dag.clean();
//		DAGOperations.computeTransitiveReduct(dag.getAllnodes());
//		DAGOperations.buildDescendants(dag.getAllnodes());
	}

}
