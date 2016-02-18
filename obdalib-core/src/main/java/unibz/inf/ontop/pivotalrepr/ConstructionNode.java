package unibz.inf.ontop.pivotalrepr;

import java.util.Optional;

import unibz.inf.ontop.model.ImmutableSubstitution;
import unibz.inf.ontop.model.ImmutableTerm;
import unibz.inf.ontop.model.VariableOrGroundTerm;

/**
 * Head node an IntermediateQuery
 *
 * TODO: further explain
 *
 */
public interface ConstructionNode extends SubTreeDelimiterNode {

    /**
     * Projected variables --> transformed variable
     */
    ImmutableSubstitution<ImmutableTerm> getSubstitution();

    /**
     * TODO: explain
     */
    Optional<ImmutableQueryModifiers> getOptionalModifiers();

    @Override
    ConstructionNode clone();

    @Override
    ConstructionNode acceptNodeTransformer(HomogeneousQueryNodeTransformer transformer)
            throws QueryNodeTransformationException;

    /**
     * TODO: find a better name and a better explanation.
     *
     * Equivalent to the regular substitution.
     * All projected variables that are defined from other variables
     * defined in the ancestor nodes have a explicit binding to them
     * in this substitution.
     *
     * In the regular substitution, they could just be bound to another
     * projected variable (INDIRECT).
     *
     */
    ImmutableSubstitution<ImmutableTerm> getDirectBindingSubstitution();

    @Override
    SubstitutionResults<ConstructionNode> applyAscendentSubstitution(
            ImmutableSubstitution<? extends VariableOrGroundTerm> substitution,
            QueryNode descendantNode, IntermediateQuery query);

    @Override
    SubstitutionResults<ConstructionNode> applyDescendentSubstitution(
            ImmutableSubstitution<? extends VariableOrGroundTerm> substitution) throws QueryNodeSubstitutionException;
}
