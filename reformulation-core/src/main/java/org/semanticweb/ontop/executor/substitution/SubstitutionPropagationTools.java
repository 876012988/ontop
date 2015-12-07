package org.semanticweb.ontop.executor.substitution;


import com.google.common.base.Optional;
import org.semanticweb.ontop.model.ImmutableSubstitution;
import org.semanticweb.ontop.model.ImmutableTerm;
import org.semanticweb.ontop.model.VariableOrGroundTerm;
import org.semanticweb.ontop.pivotalrepr.IntermediateQuery;
import org.semanticweb.ontop.pivotalrepr.QueryNode;
import org.semanticweb.ontop.pivotalrepr.QueryNodeSubstitutionException;
import org.semanticweb.ontop.pivotalrepr.SubstitutionResults;
import org.semanticweb.ontop.pivotalrepr.impl.IllegalTreeUpdateException;
import org.semanticweb.ontop.pivotalrepr.impl.QueryTreeComponent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * These methods are only accessible by InternalProposalExecutors (requires access to the QueryTreeComponent).
 */
public class SubstitutionPropagationTools {

    /**
     * Propagates the substitution to the descendants of the focus node.
     *
     * THE SUBSTITUTION IS NOT APPLIED TO THE FOCUS NODE.
     *
     * Has-side effect on the tree component.
     * Returns the updated tree component
     *
     */
    public static QueryTreeComponent propagateDown(final QueryNode focusNode,
                                     final ImmutableSubstitution<? extends VariableOrGroundTerm> currentSubstitutionToPropagate,
                                     final QueryTreeComponent treeComponent)
            throws QueryNodeSubstitutionException {

        Queue<QueryNode> nodesToVisit = new LinkedList<>();
        nodesToVisit.addAll(treeComponent.getCurrentSubNodesOf(focusNode));

        while (!nodesToVisit.isEmpty()) {
            QueryNode formerSubNode = nodesToVisit.poll();

            SubstitutionResults<? extends QueryNode> substitutionResults =
                    formerSubNode.applyDescendentSubstitution(currentSubstitutionToPropagate);

            Optional<? extends QueryNode> optionalNewSubNode = substitutionResults.getOptionalNewNode();
            Optional<? extends ImmutableSubstitution<? extends VariableOrGroundTerm>> optionalNewSubstitution
                    = substitutionResults.getSubstitutionToPropagate();

            /**
             * Still a substitution to propagate
             */
            if (optionalNewSubstitution.isPresent()) {
                ImmutableSubstitution<? extends VariableOrGroundTerm> newSubstitution = optionalNewSubstitution.get();

                /**
                 * No substitution change
                 */
                if (newSubstitution.equals(currentSubstitutionToPropagate)) {
                    /**
                     * Normal case: applies the same substitution to the children
                     */
                    if (optionalNewSubNode.isPresent()) {
                        QueryNode newSubNode = optionalNewSubNode.get();

                        nodesToVisit.addAll(treeComponent.getCurrentSubNodesOf(formerSubNode));
                        treeComponent.replaceNode(formerSubNode, newSubNode);
                    }
                    /**
                     * The sub-node is not needed anymore
                     */
                    else {
                        nodesToVisit.addAll(treeComponent.getCurrentSubNodesOf(formerSubNode));
                        try {
                            treeComponent.removeOrReplaceNodeByUniqueChildren(formerSubNode);
                        }
                        catch (IllegalTreeUpdateException e1) {
                            throw new RuntimeException(e1.getMessage());
                        }
                    }
                }
                /**
                 * New substitution: applies it to the children
                 */
                else if (optionalNewSubNode.isPresent())  {

                    QueryNode newSubNode = optionalNewSubNode.get();
                    treeComponent.replaceNode(formerSubNode, newSubNode);

                    // Recursive call
                    propagateDown(newSubNode, newSubstitution, treeComponent);
                }
                /**
                 * Unhandled case: new substitution to apply to the children of
                 * a not-needed node.
                 *
                 * TODO: should we handle this case
                 */
                else {
                    throw new RuntimeException("Unhandled case: new substitution to apply to the children of " +
                            "a not-needed node.");
                }
            }
            /**
             * Stops the propagation
             */
            else {
                if (optionalNewSubNode.isPresent()) {
                    QueryNode newSubNode = optionalNewSubNode.get();
                    treeComponent.replaceNode(formerSubNode, newSubNode);
                }
                else {
                    throw new RuntimeException("Unhandled case: the stopping node for the propagation" +
                            "is not needed anymore. Should we support it?");
                }
            }
        }
        return treeComponent;
    }

    /**
     * Propagates the substitution to the ancestors of the focus node.
     *
     * THE SUBSTITUTION IS NOT APPLIED TO THE FOCUS NODE.
     *
     * Has-side effect on the tree component.
     * Returns the updated tree component
     *
     */
    public static QueryTreeComponent propagateUp(QueryNode focusNode, ImmutableSubstitution<? extends VariableOrGroundTerm> substitutionToPropagate,
                                                  IntermediateQuery query, QueryTreeComponent treeComponent) throws QueryNodeSubstitutionException {
        Queue<QueryNode> nodesToVisit = new LinkedList<>();

        Optional<QueryNode> optionalParent = query.getParent(focusNode);
        if (optionalParent.isPresent()) {
            nodesToVisit.add(optionalParent.get());
        }

        while (!nodesToVisit.isEmpty()) {
            QueryNode formerAncestor = nodesToVisit.poll();

            /**
             * Applies the substitution and analyses the results
             */
            SubstitutionResults<? extends QueryNode> substitutionResults = formerAncestor.applyAscendentSubstitution(
                    substitutionToPropagate, focusNode, query);

            Optional<? extends ImmutableSubstitution<? extends ImmutableTerm>> optionalNewSubstitution =
                    substitutionResults.getSubstitutionToPropagate();
            Optional<? extends QueryNode> optionalNewAncestor = substitutionResults.getOptionalNewNode();

            if (optionalNewSubstitution.isPresent()) {
                /**
                 * TODO: refactor so that to remove this assumption
                 */
                if (!substitutionToPropagate.equals(optionalNewSubstitution.get())) {
                    throw new RuntimeException("Updating the substitution is not supported (yet) in" +
                            "the ascendent substitution mode");
                }

                /**
                 * Normal case: replace the ancestor by an updated version
                 */
                if (optionalNewAncestor.isPresent()) {
                    QueryNode newAncestor = optionalNewAncestor.get();

                    nodesToVisit.addAll(query.getChildren(formerAncestor));
                    treeComponent.replaceNode(formerAncestor, newAncestor);
                }
                /**
                 * The ancestor is not needed anymore
                 */
                else {
                    nodesToVisit.addAll(query.getChildren(formerAncestor));
                    try {
                        treeComponent.removeOrReplaceNodeByUniqueChildren(formerAncestor);
                    } catch (IllegalTreeUpdateException e1) {
                        throw new RuntimeException(e1.getMessage());
                    }
                }
            }
            /**
             * Stops the propagation
             */
            else {
                if (optionalNewAncestor.isPresent()) {
                    treeComponent.replaceNode(formerAncestor, optionalNewAncestor.get());
                }
                else {
                    throw new RuntimeException("Unexpected case where the propagation is stopped and" +
                            "the stopping node not needed anymore. Should we support this case?");
                }
            }
        }

        return treeComponent;
    }
}
