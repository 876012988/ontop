package org.semanticweb.ontop.owlrefplatform.core.optimization;

import com.google.common.base.Optional;
import org.semanticweb.ontop.pivotalrepr.EmptyQueryException;
import org.semanticweb.ontop.pivotalrepr.InnerJoinNode;
import org.semanticweb.ontop.pivotalrepr.IntermediateQuery;
import org.semanticweb.ontop.pivotalrepr.QueryNode;
import org.semanticweb.ontop.pivotalrepr.proposal.InnerJoinOptimizationProposal;
import org.semanticweb.ontop.pivotalrepr.proposal.NodeCentricOptimizationResults;
import org.semanticweb.ontop.pivotalrepr.proposal.impl.InnerJoinOptimizationProposalImpl;

/**
 * TODO: explain
 *
 * Top-down exploration.
 */
public class BasicJoinOptimizer extends TopDownOptimizer {

    /**
     * TODO: explain
     */
    @Override
    public IntermediateQuery optimize(IntermediateQuery initialQuery) throws EmptyQueryException {

        // Non-final
        Optional<QueryNode> optionalNextNode = Optional.of((QueryNode)initialQuery.getRootConstructionNode());

        // Non-final
        IntermediateQuery currentQuery = initialQuery;

        while (optionalNextNode.isPresent()) {
            QueryNode currentNode = optionalNextNode.get();

            if (currentNode instanceof InnerJoinNode) {
                InnerJoinOptimizationProposal joinProposal = new InnerJoinOptimizationProposalImpl((InnerJoinNode) currentNode);
                NodeCentricOptimizationResults<InnerJoinNode> optimizationResults = currentQuery.applyProposal(joinProposal);

                currentQuery = optimizationResults.getResultingQuery();
                optionalNextNode = getNextNodeFromOptimizationResults(optimizationResults);
            }
            /**
             * Non-join node
             */
            else {
                optionalNextNode = getNaturalNextNode(currentQuery, currentNode);
            }
        }
        return currentQuery;
    }

    /**
     * TODO: explain
     *
     */
    private Optional<QueryNode> getNextNodeFromOptimizationResults(NodeCentricOptimizationResults optimizationResults) {
        IntermediateQuery query = optimizationResults.getResultingQuery();

        /**
         * First look at the "new current node" (if any)
         */
        Optional<QueryNode> optionalNewCurrentNode = optimizationResults.getOptionalNewNode();
        if (optionalNewCurrentNode.isPresent()) {
            return getNaturalNextNode(query, optionalNewCurrentNode.get());
        }
        /**
         * The current node (and thus its sub-tree) is not part of the query anymore.
         */
        else {
            Optional<QueryNode> optionalNextSibling = optimizationResults.getOptionalNextSibling();

            /**
             * Looks first for the next sibling
             */
            if (optionalNextSibling.isPresent()) {
                return optionalNextSibling;
            } else {
                Optional<QueryNode> optionalAncestor = optimizationResults.getOptionalClosestAncestor();
                /**
                 * If no sibling of the optimized node, looks for a sibling of an ancestor.
                 */
                if (optionalAncestor.isPresent()) {
                    return getNextNodeSameOrUpperLevel(query, optionalAncestor.get());
                }
                /**
                 * No ancestor ---> should have thrown an EmptyQueryException
                 */
                else {
                    // TODO: find a better exception
                    throw new RuntimeException("Internal error: No ancestor --> " +
                            "an EmptyQueryException should have been thrown by the join optimization executor");
                }
            }
        }
    }
}
