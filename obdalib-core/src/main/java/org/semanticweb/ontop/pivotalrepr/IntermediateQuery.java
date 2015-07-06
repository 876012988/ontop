package org.semanticweb.ontop.pivotalrepr;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public interface IntermediateQuery {

    ConstructionNode getRootConstructionNode();

    ImmutableList<QueryNode> getNodesInBottomUpOrder();

    ImmutableList<QueryNode> getCurrentSubNodesOf(QueryNode node);

    /**
     * EXCLUDES the root of the sub-tree (currentNode).
     * TODO: find a better name
     */
    ImmutableList<QueryNode> getSubTreeNodesInTopDownOrder(QueryNode currentNode);

    boolean contains(QueryNode node);

    /**
     * TODO: describe
     *
     * Returns the QueryNode at the same position, which might be new.
     */
    QueryNode applySubNodeSelectionProposal(NewSubNodeSelectionProposal proposal)
            throws InvalidLocalOptimizationProposalException;

    /**
     * TODO: describe
     *
     * Returns the QueryNode at the same position, which might be new.
     */
    QueryNode applyReplaceNodeProposal(ReplaceNodeProposal proposal)
            throws InvalidLocalOptimizationProposalException;

    /**
     * TODO: find an exception to throw
     */
    void mergeSubQuery(IntermediateQuery subQuery) throws QueryMergingException;

    /**
     * TODO: explain
     */
    Optional<BinaryAsymmetricOperatorNode.ArgumentPosition> getOptionalPosition(QueryNode parentNode, QueryNode child);

    ImmutableList<QueryNode> getAncestors(QueryNode descendantNode);
}
