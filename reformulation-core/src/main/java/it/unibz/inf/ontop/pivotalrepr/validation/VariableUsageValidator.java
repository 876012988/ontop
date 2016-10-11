package it.unibz.inf.ontop.pivotalrepr.validation;

import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.pivotalrepr.impl.VariableCollector;
import it.unibz.inf.ontop.pivotalrepr.*;

/**
 * TODO: explain
 */
public class VariableUsageValidator implements IntermediateQueryValidator {

    private static class VariableUsageException extends RuntimeException {
        protected VariableUsageException(String message) {
            super(message);
        }
    }

    protected static class VariableUsageVisitor implements QueryNodeVisitor {

        private final IntermediateQuery query;

        protected VariableUsageVisitor(IntermediateQuery query) {
            this.query = query;
        }

        @Override
        public void visit(IntensionalDataNode intensionalDataNode) {
        }

        @Override
        public void visit(ExtensionalDataNode extensionalDataNode) {
        }

        @Override
        public void visit(GroupNode groupNode) {
        }

        @Override
        public void visit(EmptyNode emptyNode) {
        }

        @Override
        public void visit(TrueNode trueNode) {
        }

        @Override
        public void visit(InnerJoinNode innerJoinNode) {
        }

        @Override
        public void visit(LeftJoinNode leftJoinNode) {
        }

        @Override
        public void visit(FilterNode filterNode) {
        }

        @Override
        public void visit(ConstructionNode constructionNode) {

            ImmutableSet<Variable> variablesInSubTreeNodes = VariableCollector.collectVariables(
                    query.getSubTreeNodesInTopDownOrder(constructionNode));

            // TODO: continue
        }

        @Override
        public void visit(UnionNode unionNode) {
        }

    }


    @Override
    public void validate(IntermediateQuery query) throws InvalidIntermediateQueryException {

        try {
            for (QueryNode node : query.getNodesInBottomUpOrder()) {
                VariableUsageVisitor visitor = new VariableUsageVisitor(query);
                node.acceptVisitor(visitor);
            }
        } catch (VariableUsageException e) {
            throw new InvalidIntermediateQueryException(e.getMessage());
        }

    }

}
