package it.unibz.inf.ontop.pivotalrepr;

/**
 * Visits QueryNodes without having effect on them and the intermediate query.
 *
 * If you want to make optimization proposals to the nodes/query, use an Optimizer instead.
 *
 */
public interface QueryNodeVisitor {

    void visit(ConstructionNode constructionNode);

    void visit(UnionNode unionNode);

    void visit(InnerJoinNode innerJoinNode);

    void visit(LeftJoinNode leftJoinNode);

    void visit(FilterNode filterNode);

    void visit(IntensionalDataNode intensionalDataNode);

    void visit(ExtensionalDataNode extensionalDataNode);

    void visit(GroupNode groupNode);

    void visit(EmptyNode emptyNode);
}
