package it.unibz.inf.ontop.pivotalrepr.impl.jgrapht;


import java.util.Optional;
import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.DistinctVariableOnlyDataAtom;
import it.unibz.inf.ontop.pivotalrepr.impl.IllegalTreeException;
import it.unibz.inf.ontop.pivotalrepr.impl.IntermediateQueryImpl;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import it.unibz.inf.ontop.pivotalrepr.*;

public class JgraphtIntermediateQueryBuilder implements IntermediateQueryBuilder {

    private final MetadataForQueryOptimization metadata;
    private DistinctVariableOnlyDataAtom projectionAtom;
    private DirectedAcyclicGraph<QueryNode,JgraphtQueryTreeComponent.LabeledEdge> queryDAG;
    private ConstructionNode rootConstructionNode;
    private boolean canEdit;
    private boolean hasBeenInitialized;

    /**
     * TODO: construct with Guice?
     */
    public JgraphtIntermediateQueryBuilder(MetadataForQueryOptimization metadata) {
        this.metadata =  metadata;
        queryDAG = new DirectedAcyclicGraph<>(JgraphtQueryTreeComponent.LabeledEdge.class);
        rootConstructionNode = null;
        canEdit = false;
        hasBeenInitialized = false;
    }

    @Override
    public void init(DistinctVariableOnlyDataAtom projectionAtom, ConstructionNode rootConstructionNode){
        if (hasBeenInitialized)
            throw new IllegalArgumentException("Already initialized IntermediateQueryBuilder.");
        hasBeenInitialized = true;

        this.projectionAtom = projectionAtom;

        queryDAG.addVertex(rootConstructionNode);
        this.rootConstructionNode = rootConstructionNode;
        canEdit = true;
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode childNode) throws IntermediateQueryBuilderException {
        checkEditMode();

        if (parentNode instanceof NonCommutativeOperatorNode) {
            throw new IntermediateQueryBuilderException("A position is required for adding a child " +
                    "to a BinaryAsymetricOperatorNode");
        }

        if (!queryDAG.addVertex(childNode)) {
            throw new IntermediateQueryBuilderException("Node " + childNode + " already in the graph");
        }
        try {
            // child --> parent!!
            queryDAG.addDagEdge(childNode, parentNode);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IntermediateQueryBuilderException(e.getMessage());
        }
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode childNode,
                         NonCommutativeOperatorNode.ArgumentPosition position)
            throws IntermediateQueryBuilderException {
        checkEditMode();

        if (!queryDAG.addVertex(childNode)) {
            throw new IntermediateQueryBuilderException("Node " + childNode + " already in the graph");
        }
        try {
            // child --> parent!!
            JgraphtQueryTreeComponent.LabeledEdge edge = new JgraphtQueryTreeComponent.LabeledEdge(position);
            queryDAG.addDagEdge(childNode, parentNode, edge);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IntermediateQueryBuilderException(e.getMessage());
        }
    }

    @Override
    public void addChild(QueryNode parentNode, QueryNode child,
                         Optional<NonCommutativeOperatorNode.ArgumentPosition> optionalPosition)
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

        IntermediateQuery query;
        try {
            query = new IntermediateQueryImpl(metadata, projectionAtom, new JgraphtQueryTreeComponent(queryDAG));
        } catch (IllegalTreeException e) {
            throw new IntermediateQueryBuilderException(e.getMessage());
        }
        canEdit = false;
        return query;
    }

    private void checkInitialization() throws IntermediateQueryBuilderException {
        if (!hasBeenInitialized)
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
        return rootConstructionNode;
    }

    @Override
    public ImmutableList<QueryNode> getSubNodesOf(QueryNode node)
            throws IntermediateQueryBuilderException {
        checkInitialization();
        return JgraphtQueryTreeComponent.getSubNodesOf(queryDAG, node);
    }
}
