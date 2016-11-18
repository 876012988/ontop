package it.unibz.inf.ontop.pivotalrepr.impl.jgrapht;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.pivotalrepr.*;
import it.unibz.inf.ontop.pivotalrepr.impl.IllegalTreeException;
import it.unibz.inf.ontop.pivotalrepr.impl.IllegalTreeUpdateException;
import it.unibz.inf.ontop.pivotalrepr.impl.QueryTreeComponent;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.stream.Stream;

/**
 * Basic implementation based on a JGrapht DAG.
 *
 * TODO: debug it
 *
 */
public class JgraphtQueryTreeComponent implements QueryTreeComponent {

    private static final Optional<NonCommutativeOperatorNode.ArgumentPosition> NO_POSITION = Optional.empty();
    private static final Optional<NonCommutativeOperatorNode.ArgumentPosition> LEFT_POSITION = Optional.of(NonCommutativeOperatorNode.ArgumentPosition.LEFT);
    private static final Optional<NonCommutativeOperatorNode.ArgumentPosition> RIGHT_POSITION = Optional.of(NonCommutativeOperatorNode.ArgumentPosition.RIGHT);

    /**
     * TODO: explain
     */
    public static class LabeledEdge extends DefaultEdge implements Comparable<LabeledEdge> {

        private final Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition;

        public LabeledEdge() {
            this.optionalPosition = Optional.empty();
        }

        public LabeledEdge(Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition) {
            this.optionalPosition = optionalPosition;
        }

        public LabeledEdge(NonCommutativeOperatorNode.ArgumentPosition position) {
            this.optionalPosition = Optional.of(position);
        }

        public Optional<NonCommutativeOperatorNode.ArgumentPosition> getOptionalPosition() {
            return optionalPosition;
        }

        @Override
        public int compareTo(LabeledEdge o) {
            Optional<NonCommutativeOperatorNode.ArgumentPosition> otherOptionalPosition = o.getOptionalPosition();

            if (optionalPosition.isPresent()) {
                if (otherOptionalPosition.isPresent()) {
                    return optionalPosition.get().compareTo(otherOptionalPosition.get());
                }
                else {
                    return -1;
                }
            }
            else if (otherOptionalPosition.isPresent()) {
                return 1;
            }

            return 0;
         }
    }


    /**
     * TODO: explain.
     *
     * Implementation detail: this object must NOT BE SHARED with the other classes.
     */
    private final DirectedAcyclicGraph<QueryNode, LabeledEdge> queryDAG;

    /**
     * MAKE SURE it remains the "root" of the tree.
     * MAY BE NULL!
     *
     * TODO: mark it as Nullable.
     */
    private ConstructionNode rootConstructionNode;

    /**
     * Cached value (non final). MAY BE NULL
     *
     * * TODO: mark it as Nullable.
     */
    private ImmutableList<QueryNode> bottomUpOrderedNodes;


    protected JgraphtQueryTreeComponent(DirectedAcyclicGraph<QueryNode, LabeledEdge> queryDAG)
            throws IllegalTreeException {
        this.queryDAG = queryDAG;
        /**
         * Cache attributes.
         * May throw an IllegalTreeException during their computation.
         *
         */
        this.bottomUpOrderedNodes = null;
        this.rootConstructionNode = null;
        computeNodeTopologyCache();
    }

    @Override
    public ImmutableList<QueryNode> getChildren(QueryNode node) {
        return getSubNodesOf(queryDAG, node);
    }

    @Override
    public Stream<QueryNode> getChildrenStream(QueryNode node) {
        return getChildren(node).stream();
    }

    @Override
    public ConstructionNode getRootConstructionNode() throws IllegalTreeException {
        if (rootConstructionNode == null) {
            computeNodeTopologyCache();
        }
        return rootConstructionNode;
    }

    @Override
    public ImmutableList<QueryNode> getNodesInBottomUpOrder() throws IllegalTreeException {

        /**
         * Computes the list if not cached
         */
        if (bottomUpOrderedNodes == null) {
            computeNodeTopologyCache();
        }

        return bottomUpOrderedNodes;
    }

    @Override
    public ImmutableList<QueryNode> getNodesInTopDownOrder() throws IllegalTreeException {
        return getNodesInBottomUpOrder().reverse();
    }

    @Override
    public ImmutableSet<EmptyNode> getEmptyNodes() {
        return getNodesInTopDownOrder().stream()
                .filter(n -> n instanceof EmptyNode)
                .map(n -> (EmptyNode) n)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public ImmutableSet<TrueNode> getTrueNodes() {
        return getNodesInTopDownOrder().stream()
                .filter(n -> n instanceof TrueNode)
                .map(n -> (TrueNode) n)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public ImmutableSet<IntensionalDataNode> getIntensionalNodes() {
        throw new RuntimeException("TO DO: support getting intensional nodes");
    }

    @Override
    public boolean contains(QueryNode node) {
        return queryDAG.containsVertex(node);
    }


    /**
     * TODO: explain
     */
    @Override
    public void replaceNode(QueryNode previousNode, QueryNode replacingNode) {
        resetNodeTopologyCache();

        queryDAG.addVertex(replacingNode);
        try {
            for (LabeledEdge incomingEdge : queryDAG.incomingEdgesOf(previousNode)) {
                QueryNode child = queryDAG.getEdgeSource(incomingEdge);
                queryDAG.addDagEdge(child, replacingNode, new LabeledEdge(incomingEdge.getOptionalPosition()));
            }

            for (LabeledEdge outgoingEdge : queryDAG.outgoingEdgesOf(previousNode)) {
                QueryNode parent = queryDAG.getEdgeTarget(outgoingEdge);
                queryDAG.addDagEdge(replacingNode, parent, new LabeledEdge(outgoingEdge.getOptionalPosition()));
            }

        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new RuntimeException("BUG: " + e.getLocalizedMessage());
        }
        queryDAG.removeVertex(previousNode);
    }

    @Override
    public void replaceSubTree(QueryNode subTreeRootNode, QueryNode replacingNode) {
        throw new RuntimeException("TODO: support replaceSubTree()");
    }

    /**
     * TODO: explain
     * TODO: replace this recursive implementation but iterative one
     * Low-level. Tail recursive.
     */
    @Override
    public void addSubTree(IntermediateQuery subQuery, QueryNode externalParent, QueryNode localParent) throws IllegalTreeUpdateException {
        for (QueryNode externalChild : subQuery.getChildren(externalParent)) {
            QueryNode localChild = externalChild.clone();
            queryDAG.addVertex(localChild);
            try {
                Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition = subQuery.getOptionalPosition(externalParent, externalChild);
                queryDAG.addDagEdge(localChild, localParent, new LabeledEdge(optionalPosition));
            } catch (DirectedAcyclicGraph.CycleFoundException e) {
                throw new RuntimeException("BUG (internal error)" + e.getLocalizedMessage());
            }
            // Recursive call
            addSubTree(subQuery, externalChild, localChild);
        }
    }

    @Deprecated
    public void setChildrenNodes(QueryNode parentNode, List<QueryNode> allChildrenNodes) throws IllegalTreeException {
        boolean isAsymmetric = (parentNode instanceof NonCommutativeOperatorNode);

        if (isAsymmetric && allChildrenNodes.size() != 2) {
            throw new IllegalTreeException("A BinaryAsymmetricOperatorNode requires two children, " +
                    "not " + allChildrenNodes);
        }

        Set<QueryNode> proposedSubNodesToConsider = new HashSet<>(allChildrenNodes);

        /**
         * Existing sub-nodes: keep or remove
         */
        Set<LabeledEdge> incomingEdges = queryDAG.incomingEdgesOf(parentNode);
        for (LabeledEdge subNodeEdge : incomingEdges) {
            QueryNode subNode = queryDAG.getEdgeSource(subNodeEdge);
            // Kept
            if (proposedSubNodesToConsider.contains(subNode)) {
                proposedSubNodesToConsider.remove(subNode);
            }
            // Removed
            else {
                removeSubTree(subNode);
            }
        }

        /**
         * New sub-nodes: added to the DAG
         */
        int i = 0;
        for (QueryNode newSubNode : proposedSubNodesToConsider) {
            if (!queryDAG.containsVertex(newSubNode)) {
                queryDAG.addVertex(newSubNode);
            }
            LabeledEdge edge;
            if (isAsymmetric) {
                if (i == 0) {
                    edge = new LabeledEdge(LEFT_POSITION);
                }
                else {
                    edge = new LabeledEdge(RIGHT_POSITION);
                }
            }
            else {
                edge = new LabeledEdge(NO_POSITION);
            }
            try {
                queryDAG.addDagEdge(parentNode, newSubNode, edge);
            } catch (DirectedAcyclicGraph.CycleFoundException ex) {
                // Inconsistent proposal (should not introduce a cycle in the DAG) --> throw an exception.
                // TODO: return a non- RuntimeException.
                throw new IllegalTreeException(ex.getMessage());
            }
            i++;
        }
    }

    /**
     * Removes all the nodes of a sub-tree,
     * all the edges between them and WITH THE REST OF TREE.
     *
     * Recursive
     */
    @Override
    public void removeSubTree(QueryNode subTreeRoot) {
        for (LabeledEdge subNodeEdge : queryDAG.incomingEdgesOf(subTreeRoot)) {
            QueryNode childNode = queryDAG.getEdgeSource(subNodeEdge);
            /**
             * Recursive call.
             * Removing this edge is the responsibility of the child node.
             */
            removeSubTree(childNode);
        }

        for (LabeledEdge parentEdge : queryDAG.outgoingEdgesOf(subTreeRoot)) {
            queryDAG.removeEdge(parentEdge);
        }

        queryDAG.removeVertex(subTreeRoot);
    }

    /**
     * The root is EXCLUDED
     */
    @Override
    public ImmutableList<QueryNode> getSubTreeNodesInTopDownOrder(QueryNode topNode) {

        ImmutableList.Builder<QueryNode> nodeBuilder = ImmutableList.builder();

        Queue<QueryNode> nodesToVisit = new LinkedList<>(getChildren(topNode));
        while(!nodesToVisit.isEmpty()) {
            QueryNode node = nodesToVisit.poll();
            nodeBuilder.add(node);
            nodesToVisit.addAll(getChildren(node));
        }
        return nodeBuilder.build();
    }

    @Override
    public Optional<NonCommutativeOperatorNode.ArgumentPosition> getOptionalPosition(QueryNode parentNode, QueryNode childNode) {
        LabeledEdge edge = queryDAG.getEdge(childNode, parentNode);
        if (edge == null)
            return Optional.empty();

        return edge.getOptionalPosition();
    }

    @Override
    public ImmutableList<QueryNode> getAncestors(final QueryNode descendantNode) throws IllegalTreeException {
        ImmutableList.Builder<QueryNode> ancestorBuilder = ImmutableList.builder();

        QueryNode parentNode;
        Set<LabeledEdge> toParentEdges = queryDAG.outgoingEdgesOf(descendantNode);
        while (!toParentEdges.isEmpty()) {
            if (toParentEdges.size() > 1)
                throw new IllegalTreeException("A tree node must not have more than one parent!");

            parentNode = queryDAG.getEdgeTarget(toParentEdges.iterator().next());
            ancestorBuilder.add(parentNode);
            toParentEdges = queryDAG.outgoingEdgesOf(parentNode);
        }

        return ancestorBuilder.build();
    }

    @Override
    public Optional<QueryNode> getParent(QueryNode node) throws IllegalTreeException {
        Set<LabeledEdge> toParentEdges = queryDAG.outgoingEdgesOf(node);

        switch (toParentEdges.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(queryDAG.getEdgeTarget(toParentEdges.iterator().next()));
            default:
                List<QueryNode> parents = new ArrayList<>();
                for (LabeledEdge toParentEdge : toParentEdges) {
                    parents.add(queryDAG.getEdgeTarget(toParentEdge));
                }
                throw new IllegalTreeException("More than one parent found! " + parents);
        }
    }

    @Override
    public QueryNode removeOrReplaceNodeByUniqueChildren(QueryNode node) throws IllegalTreeUpdateException {
        ImmutableList<QueryNode> children = getChildren(node);
        int nbChildren = children.size();
        switch(nbChildren) {
            case 0:
                removeSubTree(node);
                throw new IllegalTreeUpdateException("Don't have a child");
            case 1:
                QueryNode child = children.get(0);
                replaceNodeByUniqueChildren(node, child);
                return child;
            default:
                throw new IllegalTreeUpdateException(node.toString() + " has more children. Cannot be replaced");
        }
    }

    @Override
    public void replaceNodesByOneNode(ImmutableList<QueryNode> nodesToRemove, QueryNode replacingNode, QueryNode parentNode,
                                      Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition)
            throws IllegalTreeUpdateException {
        addChild(parentNode, replacingNode, optionalPosition, true);
        if (replacingNode instanceof NonCommutativeOperatorNode) {
            throw new RuntimeException("Using a BinaryAsymmetricOperatorNode as a replacingNode is not yet supported");
        }

        for(QueryNode nodeToRemove : nodesToRemove) {
            boolean isParentBinaryAsymmetricOperator = (nodeToRemove instanceof NonCommutativeOperatorNode);

            for (QueryNode child : getChildren(nodeToRemove)) {
                if (!nodesToRemove.contains(child)) {
                    if (isParentBinaryAsymmetricOperator) {
                        throw new RuntimeException("Re-integrating children of a BinaryAsymmetricOperatorNode " +
                                "is not yet supported");
                    }
                    else {
                        addChild(replacingNode, child, false);
                    }
                }
            }
            removeNode(nodeToRemove);
        }
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode child,
                         Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition, boolean canReplace)
            throws IllegalTreeUpdateException {
        if (optionalPosition.isPresent()) {
            addChild(parentNode, child, optionalPosition.get());
        }
        else {
            addChild(parentNode, child, true);
        }
    }

    /**
     * Weak guarantee about the ordering with Jgrapht...
     * TODO: avoid using it
     */
    @Override
    public Optional<QueryNode> nextSibling(QueryNode node) throws IllegalTreeException {
        Optional<QueryNode> optionalParent = getParent(node);
        if (optionalParent.isPresent()) {
            ImmutableList<QueryNode> siblings = getChildren(optionalParent.get());
            int index = siblings.indexOf(node);
            int nextIndex = index + 1;
            if (nextIndex < siblings.size()) {
                QueryNode nextSibling = siblings.get(nextIndex);

                /**
                 * Checks if the next sibling object
                 * have not appear before in the list
                 */
                if (siblings.indexOf(nextSibling) < nextIndex) {
                    throw new IllegalTreeException("The node " + nextSibling + " appears more than once in " +
                            "the children list");
                }

                return Optional.of(nextSibling);
            }
            else {
                return Optional.empty();
            }
        }
        /**
         * No parent, no sibling.
         */
        else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<QueryNode> getFirstChild(QueryNode node) {
        ImmutableList<QueryNode> children = getChildren(node);
        if (children.isEmpty()) {
            return Optional.empty();
        }
        else {
            return Optional.of(children.get(0));
        }
    }

    @Override
    public void insertParent(QueryNode childNode, QueryNode newParentNode) {
        insertParent(childNode, newParentNode, Optional.empty());
    }

    @Override
    public void insertParent(QueryNode childNode, QueryNode newParentNode,
                             Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition) throws IllegalTreeUpdateException {
        throw new RuntimeException("TODO: support insertParent()");
    }

    @Override
    public void transferChild(QueryNode childNode, QueryNode formerParentNode, QueryNode newParentNode, Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition) throws IllegalTreeUpdateException {
        throw new RuntimeException("TODO: support transferChild()");
    }

    @Override
    public Variable generateNewVariable() {
        throw new RuntimeException("TODO: support generateNewVariable()");
    }

    @Override
    public Variable generateNewVariable(Variable formerVariable) {
        throw new RuntimeException("TODO: support generateNewVariable()");
    }

    @Override
    public ImmutableSet<Variable> getKnownVariables() {
        throw new RuntimeException("TODO: support getKnownVariables()");
    }

    @Override
    public QueryNode replaceNodeByChild(QueryNode parentNode, Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalReplacingChildPosition) {
        throw new RuntimeException("TODO: support replaceNodeByChild");
    }

    @Override
    public QueryTreeComponent createSnapshot() {
        throw new RuntimeException("TODO: support createSnapshot()");
    }

    @Override
    public ImmutableSet<Variable> getVariables(QueryNode node) {
        throw new RuntimeException("TODO: support getVariables");
    }

    @Override
    public int getVersionNumber() {
        throw new RuntimeException("TODO: support getVersionNumber");
    }

    private void addChild(QueryNode parentNode, QueryNode childNode, boolean isNew) throws IllegalTreeUpdateException {

        if (parentNode instanceof NonCommutativeOperatorNode) {
            throw new IllegalTreeUpdateException("A position is required for adding a child " +
                    "to a BinaryAsymetricOperatorNode");
        }

        if (isNew && (!queryDAG.addVertex(childNode))) {
            throw new IllegalTreeUpdateException("Node " + childNode + " already in the graph");
        }
        try {
            // child --> parent!!
            queryDAG.addDagEdge(childNode, parentNode);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IllegalTreeUpdateException(e.getMessage());
        }
    }

    private void addChild(QueryNode parentNode, QueryNode childNode,
                         NonCommutativeOperatorNode.ArgumentPosition position)
            throws IllegalTreeUpdateException {

        if (!queryDAG.addVertex(childNode)) {
            throw new IllegalTreeUpdateException("Node " + childNode + " already in the graph");
        }
        try {
            // child --> parent!!
            LabeledEdge edge = new LabeledEdge(position);
            queryDAG.addDagEdge(childNode, parentNode, edge);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IllegalTreeUpdateException(e.getMessage());
        }
    }

    private void replaceNodeByUniqueChildren(QueryNode nodeToReplace, QueryNode replacingChild) {
        ImmutableSet<LabeledEdge> toParentEdges = ImmutableSet.copyOf(queryDAG.outgoingEdgesOf(nodeToReplace));

        /**
         * Links to parents
         */
        for (LabeledEdge outgoingEdge : toParentEdges) {
            QueryNode parent = queryDAG.getEdgeTarget(outgoingEdge);
            try {
                queryDAG.addDagEdge(replacingChild, parent, new LabeledEdge(outgoingEdge.getOptionalPosition()));
            } catch (DirectedAcyclicGraph.CycleFoundException e) {
                throw new RuntimeException(e.getMessage());
            }
            queryDAG.removeEdge(outgoingEdge);
            removeNode(nodeToReplace);
        }
    }


    /**
     * TODO: describe
     */
    private void computeNodeTopologyCache() throws IllegalTreeException {
        bottomUpOrderedNodes = extractBottomUpOrderedNodes(queryDAG);
        rootConstructionNode = extractRootProjectionNode(bottomUpOrderedNodes);
    }

    /**
     * TODO: describe
     */
    private void resetNodeTopologyCache() {
        bottomUpOrderedNodes = null;
        rootConstructionNode = null;
    }

    /**
     * TODO: describe
     */
    private static ImmutableList<QueryNode> extractBottomUpOrderedNodes(
            DirectedAcyclicGraph<QueryNode, LabeledEdge> queryDAG) {
        TopologicalOrderIterator<QueryNode, LabeledEdge> it =
                new TopologicalOrderIterator<>(queryDAG);

        return ImmutableList.copyOf(it);
    }

    /**
     * TODO: describe
     */
    private static ConstructionNode extractRootProjectionNode(ImmutableList<QueryNode> topDownOrderedNodes)
            throws IllegalTreeException {
        if (topDownOrderedNodes.isEmpty()) {
            throw new IllegalTreeException("Empty DAG!");
        }

        QueryNode rootNode = topDownOrderedNodes.get(topDownOrderedNodes.size() - 1);
        if (!(rootNode instanceof ConstructionNode)) {
            throw new IllegalTreeException("The root node is not a ConstructionNode: " + rootNode);
        }

        return (ConstructionNode) rootNode;
    }

    @Override
    public String toString() {
        return queryDAG.toString();
    }


    /**
     * Edges are directed from the child to the parent.
     */
    protected static ImmutableList<QueryNode> getSubNodesOf(DirectedAcyclicGraph<QueryNode, LabeledEdge> queryDAG,
                                                            QueryNode node) {

        Collection<LabeledEdge> incomingEdges = sortEdgesIfNecessary(queryDAG.incomingEdgesOf(node), node);
        ImmutableList.Builder<QueryNode> nodeListBuilder = ImmutableList.builder();
        for (LabeledEdge edge : incomingEdges) {
            nodeListBuilder.add(queryDAG.getEdgeSource(edge));
        }

        return nodeListBuilder.build();
    }

    private static Collection<LabeledEdge> sortEdgesIfNecessary(Set<LabeledEdge> edges, QueryNode parentNode) {
        if (parentNode instanceof NonCommutativeOperatorNode) {
            List<LabeledEdge> edgeList = new ArrayList<>(edges);
            Collections.sort(edgeList);
            return edgeList;
        }
        /**
         * By default, does nothing
         */
        return edges;
    }

    private void removeNode(QueryNode node) {
        ImmutableList<LabeledEdge> incomingEdges = ImmutableList.copyOf(queryDAG.incomingEdgesOf(node));
        for (LabeledEdge subNodeEdge : incomingEdges) {
            queryDAG.removeEdge(subNodeEdge);
        }

        ImmutableList<LabeledEdge> outgoingEdges = ImmutableList.copyOf(queryDAG.outgoingEdgesOf(node));
        for (LabeledEdge parentEdge : outgoingEdges) {
            queryDAG.removeEdge(parentEdge);
        }

        queryDAG.removeVertex(node);
    }

}
