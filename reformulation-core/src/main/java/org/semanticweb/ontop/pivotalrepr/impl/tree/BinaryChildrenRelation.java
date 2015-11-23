package org.semanticweb.ontop.pivotalrepr.impl.tree;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.semanticweb.ontop.pivotalrepr.NonCommutativeOperatorNode;
import org.semanticweb.ontop.pivotalrepr.NonCommutativeOperatorNode.ArgumentPosition;
import org.semanticweb.ontop.pivotalrepr.QueryNode;
import org.semanticweb.ontop.pivotalrepr.impl.IllegalTreeUpdateException;

/**
 * TODO: explain
 */
public class BinaryChildrenRelation implements ChildrenRelation {

    private final TreeNode parent;
    private Optional<TreeNode> optionalLeftChild;
    private Optional<TreeNode> optionalRightChild;


    protected BinaryChildrenRelation(TreeNode parent) {
        if (!(parent.getQueryNode() instanceof NonCommutativeOperatorNode)) {
            throw new IllegalArgumentException("The StandardChildrenRelation requires " +
                    "BinaryAsymmetricOperatorNode as parents");
        }

        this.parent = parent;
        this.optionalLeftChild = Optional.absent();
        this.optionalRightChild = Optional.absent();
    }


    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public ImmutableList<TreeNode> getChildren() {
        ImmutableList.Builder<TreeNode> builder = ImmutableList.builder();
        if (optionalLeftChild.isPresent()) {
            builder.add(optionalLeftChild.get());
        }
        if (optionalRightChild.isPresent()) {
            builder.add(optionalRightChild.get());
        }
        return builder.build();
    }

    @Override
    public boolean contains(TreeNode node) {
        return getChildren().contains(node);
    }

    @Override
    public void addChild(TreeNode childNode, Optional<ArgumentPosition> optionalPosition, boolean canReplace)
            throws IllegalTreeUpdateException {
        if (!optionalPosition.isPresent()) {
            throw new IllegalArgumentException("The StandardChildrenRelation requires argument positions");
        }

        switch (optionalPosition.get()) {
            case LEFT:
                if (optionalLeftChild.isPresent() && (!canReplace) && (optionalLeftChild.get() != childNode)) {
                    throw new IllegalTreeUpdateException("Left child node is already present");
                }
                else {
                    optionalLeftChild = Optional.of(childNode);
                }
                break;
            case RIGHT:
                if (optionalRightChild.isPresent() && (!canReplace) && (optionalRightChild.get() != childNode)) {
                        throw new IllegalTreeUpdateException("Right child node is already present");
                }
                else {
                    optionalRightChild = Optional.of(childNode);
                }
                break;
        }
    }

    @Override
    public void replaceChild(TreeNode formerChild, TreeNode newChild) {
        if (optionalLeftChild.isPresent() && (optionalLeftChild.get() == formerChild)) {
            optionalLeftChild = Optional.of(newChild);
        }
        else if (optionalRightChild.isPresent() && (optionalRightChild.get() == formerChild)) {
            optionalRightChild = Optional.of(newChild);
        }
        else {
            throw new IllegalArgumentException("Unknown former child " + formerChild);
        }
    }

    @Override
    public void removeChild(TreeNode childNode) {
        if (optionalLeftChild.isPresent() && (optionalLeftChild.get() == childNode)) {
            optionalLeftChild = Optional.absent();
        }
        // Compatible with the crazy case where the same node appears on the two sides.
        if (optionalRightChild.isPresent() && (optionalRightChild.get() == childNode)) {
            optionalRightChild = Optional.absent();
        }
    }

    @Override
    public ImmutableList<QueryNode> getChildQueryNodes() {
        ImmutableList.Builder<QueryNode> builder = ImmutableList.builder();
        for (TreeNode treeNode : getChildren()) {
            builder.add(treeNode.getQueryNode());
        }
        return builder.build();
    }

    @Override
    public Optional<ArgumentPosition> getOptionalPosition(TreeNode childNode) {
        if (optionalLeftChild.isPresent() && (optionalLeftChild.get() == childNode)) {
            return Optional.of(ArgumentPosition.LEFT);
        }
        else if (optionalRightChild.isPresent() && (optionalRightChild.get() == childNode)) {
            return Optional.of(ArgumentPosition.RIGHT);
        }
        else {
            throw new IllegalArgumentException(childNode.getQueryNode() + " does not appear as a child.");
        }
    }
}
