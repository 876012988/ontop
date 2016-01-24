package org.semanticweb.ontop.executor.join;

import java.util.Optional;
import com.google.common.collect.ImmutableList;
import org.semanticweb.ontop.executor.NodeCentricInternalExecutor;
import org.semanticweb.ontop.model.ImmutableBooleanExpression;
import org.semanticweb.ontop.pivotalrepr.*;
import org.semanticweb.ontop.pivotalrepr.impl.IllegalTreeException;
import org.semanticweb.ontop.pivotalrepr.impl.IllegalTreeUpdateException;
import org.semanticweb.ontop.pivotalrepr.impl.InnerJoinNodeImpl;
import org.semanticweb.ontop.pivotalrepr.impl.QueryTreeComponent;
import org.semanticweb.ontop.pivotalrepr.proposal.*;
import org.semanticweb.ontop.pivotalrepr.proposal.impl.NodeCentricOptimizationResultsImpl;
import org.semanticweb.ontop.pivotalrepr.proposal.impl.ReactToChildDeletionProposalImpl;

import static org.semanticweb.ontop.executor.join.JoinExtractionUtils.*;

/**
* TODO: explain
*/
public class JoinBooleanExpressionExecutor implements NodeCentricInternalExecutor<InnerJoinNode, InnerJoinOptimizationProposal> {

    /**
     * Standard method (InternalProposalExecutor)
     */
    @Override
    public NodeCentricOptimizationResults<InnerJoinNode> apply(InnerJoinOptimizationProposal proposal, IntermediateQuery query,
                                              QueryTreeComponent treeComponent)
            throws InvalidQueryOptimizationProposalException, EmptyQueryException {

        InnerJoinNode originalTopJoinNode = proposal.getFocusNode();

        /**
         * Will remain the sames, whatever happens
         */
        Optional<QueryNode> optionalParent = query.getParent(originalTopJoinNode);
        Optional<QueryNode> optionalNextSibling = query.getNextSibling(originalTopJoinNode);

        /**
         * Optimizes
         */
        Optional<InnerJoinNode> optionalNewJoinNode = transformJoin(originalTopJoinNode, query, treeComponent);

        if (optionalNewJoinNode.isPresent()) {
            return new NodeCentricOptimizationResultsImpl<>(query, optionalNewJoinNode.get());
        }
        else {
            ReactToChildDeletionProposal reactionProposal = new ReactToChildDeletionProposalImpl(originalTopJoinNode,
                    optionalParent.get(), optionalNextSibling);

            ReactToChildDeletionResults deletionResults = query.applyProposal(reactionProposal);

            return new NodeCentricOptimizationResultsImpl<>(deletionResults.getResultingQuery(),
                    deletionResults.getOptionalNextSibling(), java.util.Optional.of(deletionResults.getClosestRemainingAncestor()));
        }
    }

    /**
     * TODO: explain
     */
    private Optional<InnerJoinNode> transformJoin(InnerJoinNode topJoinNode, IntermediateQuery query,
                                          QueryTreeComponent treeComponent) {


        ImmutableList<JoinOrFilterNode> filterOrJoinNodes = extractFilterAndInnerJoinNodes(topJoinNode, query);

        Optional<ImmutableBooleanExpression> optionalAggregatedFilterCondition;
        try {
            optionalAggregatedFilterCondition = extractFoldAndOptimizeBooleanExpressions(filterOrJoinNodes);
        }
        /**
         * The filter condition can be satisfied --> the join node and its sub-tree is thus removed from the tree.
         * Returns no join node.
         */
        catch (InsatisfiedExpressionException e) {
            treeComponent.removeSubTree(topJoinNode);
            return Optional.empty();
        }

        InnerJoinNode newJoinNode = new InnerJoinNodeImpl(optionalAggregatedFilterCondition);

        try {
            QueryNode parentNode = treeComponent.getParent(topJoinNode).get();
            Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition = treeComponent.getOptionalPosition(parentNode, topJoinNode);
            treeComponent.replaceNodesByOneNode(ImmutableList.<QueryNode>copyOf(filterOrJoinNodes), newJoinNode, parentNode, optionalPosition);

        } catch (IllegalTreeUpdateException | IllegalTreeException e) {
            throw new RuntimeException("Internal error: " + e.getMessage());
        }

        return Optional.of(newJoinNode);
    }



}
