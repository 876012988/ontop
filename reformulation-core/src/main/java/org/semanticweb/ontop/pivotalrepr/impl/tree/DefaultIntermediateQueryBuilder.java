package org.semanticweb.ontop.pivotalrepr.impl.tree;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.semanticweb.ontop.pivotalrepr.*;
import org.semanticweb.ontop.pivotalrepr.NonCommutativeOperatorNode.ArgumentPosition;
import org.semanticweb.ontop.pivotalrepr.impl.IllegalTreeUpdateException;
import org.semanticweb.ontop.pivotalrepr.impl.IntermediateQueryImpl;
import org.semanticweb.ontop.pivotalrepr.impl.QueryTreeComponent;

/**
 * TODO: explain
 */
public class DefaultIntermediateQueryBuilder implements IntermediateQueryBuilder {

    private final MetadataForQueryOptimization metadata;
    private QueryTree tree;
    private boolean canEdit;

    public DefaultIntermediateQueryBuilder(MetadataForQueryOptimization metadata) {
        this.metadata = metadata;
        tree = null;
        canEdit = true;
    }


    @Override
    public void init(ConstructionNode rootConstructionNode){
        if (tree != null)
            throw new IllegalArgumentException("Already initialized IntermediateQueryBuilder.");

        // TODO: use Guice to construct this tree
        tree = new DefaultTree(rootConstructionNode);
        canEdit = true;
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode childNode) throws IntermediateQueryBuilderException {
        checkEditMode();
        try {
            tree.addChild(parentNode, childNode, Optional.<ArgumentPosition>absent(), true, false);
        } catch (IllegalTreeUpdateException e) {
            throw new IntermediateQueryBuilderException(e.getMessage());
        }
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode childNode,
                         ArgumentPosition position)
            throws IntermediateQueryBuilderException {
        checkEditMode();
        try {
            tree.addChild(parentNode, childNode, Optional.of(position), true, false);
        } catch (IllegalTreeUpdateException e) {
            throw new IntermediateQueryBuilderException(e.getMessage());
        }
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode child,
                         Optional<ArgumentPosition> optionalPosition)
            throws IntermediateQueryBuilderException {
        if (optionalPosition.isPresent()) {
            addChild(parentNode, child, optionalPosition.get());
        }
        else {
            addChild(parentNode, child);
        }
    }

    @Override
    public IntermediateQuery build() throws IntermediateQueryBuilderException{
        checkInitialization();

        IntermediateQuery query = buildQuery(metadata, new DefaultQueryTreeComponent(tree));
        canEdit = false;
        return query;
    }

    /**
     * Can be overwritten to use another constructor
     */
    protected IntermediateQuery buildQuery(MetadataForQueryOptimization metadata, QueryTreeComponent treeComponent) {
        return new IntermediateQueryImpl(metadata, treeComponent);
    }

    private void checkInitialization() throws IntermediateQueryBuilderException {
        if (tree == null)
            throw new IntermediateQueryBuilderException("Not initialized!");
    }

    private void checkEditMode() throws IntermediateQueryBuilderException {
        checkInitialization();

        if (!canEdit)
            throw new IllegalArgumentException("Cannot be edited anymore (the query has already been built).");
    }

    @Override
    public ConstructionNode getRootConstructionNode() throws IntermediateQueryBuilderException {
        checkInitialization();
        return tree.getRootNode();
    }

    @Override
    public ImmutableList<QueryNode> getSubNodesOf(QueryNode node)
            throws IntermediateQueryBuilderException {
        checkInitialization();
        return tree.getChildren(node);
    }
}
