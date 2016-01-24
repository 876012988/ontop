package org.semanticweb.ontop.pivotalrepr.impl.tree;

import com.google.common.collect.ImmutableList;
import org.semanticweb.ontop.pivotalrepr.NonCommutativeOperatorNode;
import org.semanticweb.ontop.pivotalrepr.NonCommutativeOperatorNode.ArgumentPosition;
import org.semanticweb.ontop.pivotalrepr.ConstructionNode;
import org.semanticweb.ontop.pivotalrepr.QueryNode;
import org.semanticweb.ontop.pivotalrepr.impl.IllegalTreeUpdateException;

import java.util.*;

/**
 * TODO: explain
 *
 * Mutable
 *
 */
public class DefaultTree implements QueryTree {

    /**
     * Final but MUTABLE attributes
     */
    private final TreeNode rootNode;
    private final Map<QueryNode, TreeNode> nodeIndex;
    private final Map<TreeNode, ChildrenRelation> childrenIndex;
    private final Map<TreeNode, TreeNode> parentIndex;


    protected DefaultTree(ConstructionNode rootQueryNode) {
        nodeIndex = new HashMap<>();
        childrenIndex = new HashMap<>();
        parentIndex = new HashMap<>();

        // Adds the root node
        rootNode = new TreeNode(rootQueryNode);
        nodeIndex.put(rootQueryNode, rootNode);
        childrenIndex.put(rootNode, createChildrenRelation(rootNode));
        // No parent
    }

    @Override
    public ConstructionNode getRootNode() {
        return (ConstructionNode) rootNode.getQueryNode();
    }

    @Override
    public void addChild(QueryNode parentQueryNode, QueryNode childQueryNode, Optional<ArgumentPosition> optionalPosition,
                         boolean mustBeNew, boolean canReplace) throws IllegalTreeUpdateException {
        TreeNode parentNode = accessTreeNode(parentQueryNode);

        TreeNode childNode;
        if (nodeIndex.containsKey(childQueryNode)) {
            if (mustBeNew) {
                throw new IllegalTreeUpdateException("Node " + childQueryNode + " already in the graph");
            }
            else {
                childNode = accessTreeNode(childQueryNode);

                TreeNode previousParent = getParentTreeNode(childNode);
                if (previousParent != null) {
                    removeChild(previousParent, childNode);
                }
                parentIndex.put(childNode, parentNode);
                accessChildrenRelation(parentNode).addChild(childNode, optionalPosition, canReplace);
            }
        }
        /**
         * New node
         */
        else {
            createNewNode(childQueryNode, parentNode, optionalPosition, canReplace);
        }
    }

    /**
     * Low-level
     */
    private void createNewNode(QueryNode childQueryNode, TreeNode parentNode,
                               Optional<ArgumentPosition> optionalPosition, boolean canReplace)
            throws IllegalTreeUpdateException {
        TreeNode childNode = new TreeNode(childQueryNode);
        nodeIndex.put(childQueryNode, childNode);

        childrenIndex.put(childNode, createChildrenRelation(childNode));

        parentIndex.put(childNode, parentNode);
        accessChildrenRelation(parentNode).addChild(childNode, optionalPosition, canReplace);
    }

    private static ChildrenRelation createChildrenRelation(TreeNode parentTreeNode) {
        if (parentTreeNode.getQueryNode() instanceof NonCommutativeOperatorNode) {
            return new BinaryChildrenRelation(parentTreeNode);
        }
        else {
            return new StandardChildrenRelation(parentTreeNode);
        }
    }


    @Override
    public ImmutableList<QueryNode> getChildren(QueryNode node) {
        ChildrenRelation childrenRelation = accessChildrenRelation(accessTreeNode(node));
        if (childrenRelation == null) {
            return ImmutableList.of();
        }
        else {
            return childrenRelation.getChildQueryNodes();
        }
    }

    @Override
    public boolean contains(QueryNode node) {
        return nodeIndex.containsKey(node);
    }

    @Override
    public ImmutableList<QueryNode> getNodesInBottomUpOrder() {
        return getNodesInTopDownOrder().reverse();
    }

    @Override
    public ImmutableList<QueryNode> getNodesInTopDownOrder() {
        Queue<TreeNode> nodesToExplore = new LinkedList<>();
        ImmutableList.Builder<QueryNode> builder = ImmutableList.builder();
        nodesToExplore.add(rootNode);
        builder.add(rootNode.getQueryNode());

        while (!nodesToExplore.isEmpty()) {
            TreeNode node = nodesToExplore.poll();
            for (TreeNode childNode : accessChildrenRelation(node).getChildren()) {
                nodesToExplore.add(childNode);
                builder.add(childNode.getQueryNode());
            }
        }
        return builder.build();
    }

    @Override
    public void replaceNode(QueryNode previousNode, QueryNode replacingNode) {
        TreeNode treeNode = accessTreeNode(previousNode);
        if (treeNode == null) {
            throw new IllegalArgumentException("The previous query node must be in the tree");
        }
        if (contains(replacingNode)) {
            throw new IllegalArgumentException("The replacing node must not be already in the tree");
        }

        treeNode.changeQueryNode(replacingNode);
        nodeIndex.remove(previousNode);
        nodeIndex.put(replacingNode, treeNode);
    }

    @Override
    public void removeSubTree(QueryNode subQueryTreeRoot) {
        TreeNode subTreeRoot = accessTreeNode(subQueryTreeRoot);

        Queue<TreeNode> nodesToRemove = new LinkedList<>();
        nodesToRemove.add(subTreeRoot);

        while(!nodesToRemove.isEmpty()) {
            TreeNode treeNode = nodesToRemove.poll();
            nodesToRemove.addAll(accessChildrenRelation(treeNode).getChildren());

            removeNode(treeNode);
        }
    }

    @Override
    public ImmutableList<QueryNode> getSubTreeNodesInTopDownOrder(QueryNode currentQueryNode) {
        TreeNode currentTreeNode = accessTreeNode(currentQueryNode);

        Queue<TreeNode> nodesToExplore = new LinkedList<>();
        ImmutableList.Builder<QueryNode> builder = ImmutableList.builder();
        nodesToExplore.add(currentTreeNode);
        // The root is excluded from the list

        while (!nodesToExplore.isEmpty()) {
            TreeNode node = nodesToExplore.poll();
            for (TreeNode childNode : accessChildrenRelation(node).getChildren()) {
                nodesToExplore.add(childNode);
                builder.add(childNode.getQueryNode());
            }
        }
        return builder.build();
    }

    @Override
    public Optional<QueryNode> getParent(QueryNode childQueryNode) {
        TreeNode childTreeNode = accessTreeNode(childQueryNode);

        TreeNode parentTreeNode = getParentTreeNode(childTreeNode);
        if (parentTreeNode == null) {
            return Optional.empty();
        }
        else {
            return Optional.of(parentTreeNode.getQueryNode());
        }
    }

    @Override
    public void removeOrReplaceNodeByUniqueChild(QueryNode parentQueryNode) throws IllegalTreeUpdateException {
        TreeNode parentTreeNode = accessTreeNode(parentQueryNode);

        ImmutableList<TreeNode> children = accessChildrenRelation(parentTreeNode).getChildren();

        if (children.size() == 1) {
            TreeNode childTreeNode = children.get(0);
            childrenIndex.remove(parentTreeNode);
            // May be null
            TreeNode grandParentTreeNode = getParentTreeNode(parentTreeNode);
            parentIndex.remove(parentTreeNode);
            parentIndex.put(childTreeNode, grandParentTreeNode);

            ChildrenRelation grandParentRelation = accessChildrenRelation(grandParentTreeNode);
            grandParentRelation.replaceChild(parentTreeNode, childTreeNode);
        }
        else {
            throw new IllegalTreeUpdateException("The query node " + parentQueryNode + " does not have a unique child");
        }
    }

    @Override
    public void replaceNodesByOneNode(ImmutableList<QueryNode> nodesToRemove, QueryNode replacingNode,
                                      QueryNode parentNode, Optional<ArgumentPosition> optionalPosition) throws IllegalTreeUpdateException {
        if (replacingNode instanceof NonCommutativeOperatorNode) {
            throw new RuntimeException("Having a BinaryAsymmetricOperatorNode replacing node is not yet supported");
        }
        addChild(parentNode, replacingNode, optionalPosition, true, true);


        for(QueryNode nodeToRemove : nodesToRemove) {
            boolean isParentBinaryAsymmetricOperator = (nodeToRemove instanceof NonCommutativeOperatorNode);

            TreeNode treeNodeToRemove = accessTreeNode(nodeToRemove);

            for (QueryNode child : accessChildrenRelation(treeNodeToRemove).getChildQueryNodes()) {
                if (!nodesToRemove.contains(child)) {
                    if (isParentBinaryAsymmetricOperator) {
                        throw new RuntimeException("Re-integrating children of a BinaryAsymmetricOperatorNode " +
                                "is not yet supported");
                    }
                    else {
                        addChild(replacingNode, child, Optional.<ArgumentPosition>empty(), false, true);
                    }
                }
            }
            removeNode(treeNodeToRemove);
        }
    }

    @Override
    public Optional<ArgumentPosition> getOptionalPosition(QueryNode parentNode, QueryNode childNode) {
        TreeNode parentTreeNode = accessTreeNode(parentNode);
        TreeNode childTreeNode = accessTreeNode(childNode);

        ChildrenRelation childrenRelation = accessChildrenRelation(parentTreeNode);
        return childrenRelation.getOptionalPosition(childTreeNode);
    }

    @Override
    public void insertParent(QueryNode childNode, QueryNode newParentNode) throws IllegalTreeUpdateException {
        TreeNode childTreeNode = accessTreeNode(childNode);

        Optional<QueryNode> optionalFormerParent = getParent(childNode);
        if (!optionalFormerParent.isPresent()) {
            throw new IllegalTreeUpdateException("Inserting a parent to the current root is not supported");
        }
        QueryNode formerParentNode = optionalFormerParent.get();
        TreeNode formerParentTreeNode = accessTreeNode(formerParentNode);

        Optional<ArgumentPosition> optionalPosition = getOptionalPosition(formerParentNode, childNode);

        // Does not delete the child node, just disconnect it from its parent
        removeChild(formerParentTreeNode, childTreeNode);

        // Adds the new parent (must be new)
        addChild(formerParentNode, newParentNode, optionalPosition, true, false);

        addChild(newParentNode, childNode, Optional.<ArgumentPosition>empty(), false, false);
    }

    /**
     * Low-level
     */
    private void removeNode(TreeNode treeNode) {
        nodeIndex.remove(treeNode.getQueryNode());
        TreeNode parentNode = getParentTreeNode(treeNode);
        if (parentNode != null) {
            accessChildrenRelation(parentNode).removeChild(treeNode);
        }
        parentIndex.remove(treeNode);

        /**
         * Its children have no parent anymore
         */
        for (TreeNode childTreeNode : childrenIndex.get(treeNode).getChildren()) {
            parentIndex.remove(childTreeNode);
        }
        childrenIndex.remove(treeNode);
    }

    private void removeChild(TreeNode parentNode, TreeNode childNodeToRemove) {
        if (getParentTreeNode(childNodeToRemove) == parentNode) {
            parentIndex.remove(childNodeToRemove);
        }

        if (childrenIndex.containsKey(parentNode)) {
            accessChildrenRelation(parentNode).removeChild(childNodeToRemove);
        }
    }

    private TreeNode accessTreeNode(QueryNode node) {
        TreeNode treeNode = nodeIndex.get(node);
        if (treeNode == null) {
            throw new IllegalArgumentException("The given query node is not in the tree");
        }
        return treeNode;
    }

    private ChildrenRelation accessChildrenRelation(TreeNode node) {
        ChildrenRelation relation = childrenIndex.get(node);
        if (relation == null) {
            throw new RuntimeException("Internal error: the tree node does not have a children relation.");
        }
        return relation;
    }

    /**
     * The returned value might be null.
     *
     * The point of this structure is to enforce the use of a TreeNode as argument.
     */
    private TreeNode getParentTreeNode(TreeNode child) {
        TreeNode parentTreeNode = parentIndex.get(child);

        if (parentTreeNode == null)
            return null;

        // Makes sure the parent node is still present in the tree
        else if (contains(parentTreeNode.getQueryNode()))
            return parentTreeNode;
        else
            throw new RuntimeException("Internal error: points to a parent that is not (anymore) in the tree");
    }
}
