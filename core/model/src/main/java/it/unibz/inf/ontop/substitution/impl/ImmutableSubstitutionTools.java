package it.unibz.inf.ontop.substitution.impl;

import java.util.AbstractMap;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import fj.P;
import fj.P2;
import it.unibz.inf.ontop.model.term.impl.ImmutabilityTools;
import it.unibz.inf.ontop.model.term.TermConstants;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.substitution.InjectiveVar2VarSubstitution;
import it.unibz.inf.ontop.substitution.Substitution;
import it.unibz.inf.ontop.substitution.Var2VarSubstitution;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.Map;

import static it.unibz.inf.ontop.model.term.impl.GroundTermTools.isGroundTerm;

/**
 * Tools for the new generation of (immutable) substitutions
 */
public class ImmutableSubstitutionTools {

    private static final ImmutableSubstitution<ImmutableTerm> EMPTY_SUBSTITUTION = new NeutralSubstitution();


    /**
     * Extracts the sub-set of the substitution entries that are var-to-var mappings.
     */
    public static Var2VarSubstitution extractVar2VarSubstitution(Substitution substitution) {
        /**
         * Saves an unnecessary computation.
         */
        if (substitution instanceof Var2VarSubstitution)
            return (Var2VarSubstitution) substitution;

        ImmutableMap.Builder<Variable, Variable> substitutionMapBuilder = ImmutableMap.builder();

        for (Map.Entry<Variable, Term> entry : substitution.getMap().entrySet()) {
            Term target = entry.getValue();
            if (target instanceof Variable) {
                substitutionMapBuilder.put(entry.getKey(), (Variable) target);
            }
        }
        return new Var2VarSubstitutionImpl(substitutionMapBuilder.build());
    }

    /**
     * Splits the substitution into two substitutions:
     *         (i) One without functional term
     *         (ii) One containing the rest
     */
    public static P2<ImmutableSubstitution<NonFunctionalTerm>, ImmutableSubstitution<ImmutableFunctionalTerm>> splitFunctionFreeSubstitution(
            ImmutableSubstitution substitution) {

        ImmutableMap.Builder<Variable, NonFunctionalTerm> functionFreeMapBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Variable, ImmutableFunctionalTerm> otherMapBuilder = ImmutableMap.builder();

        for (Map.Entry<Variable, Term> entry : substitution.getMap().entrySet()) {
            Term target = entry.getValue();
            if (target instanceof NonFunctionalTerm) {
                functionFreeMapBuilder.put(entry.getKey(), (NonFunctionalTerm) target);
            } else if (target instanceof ImmutableFunctionalTerm) {
                otherMapBuilder.put(entry.getKey(), (ImmutableFunctionalTerm) target);
            }
            else {
                throw new IllegalArgumentException("Unknown type of term detected in the substitution: "
                        + target.getClass());
            }
        }

        ImmutableSubstitution<NonFunctionalTerm> functionFreeSubstitution = new ImmutableSubstitutionImpl<>(
                functionFreeMapBuilder.build());

        // TODO: consider adding typing to the ImmutableSubstitutionImpl.
        ImmutableSubstitution<ImmutableFunctionalTerm> otherSubstitution = new ImmutableSubstitutionImpl<>(otherMapBuilder.build());

        return P.p(functionFreeSubstitution, otherSubstitution);
    }

    /**
     * TODO: explain
     */
    public static ImmutableSubstitution<ImmutableTerm> convertSubstitution(Substitution substitution) {
        ImmutableMap.Builder<Variable, ImmutableTerm> substitutionMapBuilder = ImmutableMap.builder();
        for (Map.Entry<Variable, Term> entry : substitution.getMap().entrySet()) {
            ImmutableTerm immutableValue = ImmutabilityTools.convertIntoImmutableTerm(entry.getValue());

            substitutionMapBuilder.put(entry.getKey(), immutableValue);

        }
        return new ImmutableSubstitutionImpl<>(substitutionMapBuilder.build());
    }


    /**
     * Returns a substitution theta (if it exists) such as :
     *    theta(s) = t
     *
     * with
     *    s : source term
     *    t: target term
     *
     */
    public static Optional<ImmutableSubstitution<ImmutableTerm>> computeUnidirectionalSubstitution(ImmutableTerm sourceTerm,
                                                                                                   ImmutableTerm targetTerm) {
        /**
         * Variable
         */
        if (sourceTerm instanceof Variable) {
            Variable sourceVariable = (Variable) sourceTerm;

            // Constraint
            if ((!sourceVariable.equals(targetTerm))
                    && (targetTerm instanceof ImmutableFunctionalTerm)
                    && ((ImmutableFunctionalTerm)targetTerm).getVariables().contains(sourceVariable)) {
                return Optional.empty();
            }

            ImmutableSubstitution<ImmutableTerm> substitution = new ImmutableSubstitutionImpl<>(
                    ImmutableMap.of(sourceVariable, targetTerm));
            return Optional.of(substitution);
        }
        /**
         * Functional term
         */
        else if (sourceTerm instanceof ImmutableFunctionalTerm) {
            if (targetTerm instanceof ImmutableFunctionalTerm) {
                return computeUnidirectionalSubstitutionOfFunctionalTerms((ImmutableFunctionalTerm) sourceTerm,
                        (ImmutableFunctionalTerm) targetTerm);
            }
            else {
                return Optional.empty();
            }
        }
        /**
         * Constant
         */
        else if(sourceTerm.equals(targetTerm)) {
            return Optional.of(EMPTY_SUBSTITUTION);
        }
        else {
            return Optional.empty();
        }
    }

    private static Optional<ImmutableSubstitution<ImmutableTerm>> computeUnidirectionalSubstitutionOfFunctionalTerms(
            ImmutableFunctionalTerm sourceFunctionalTerm, ImmutableFunctionalTerm targetFunctionalTerm) {

        /**
         * Function symbol equality
         */
        if (!sourceFunctionalTerm.getFunctionSymbol().equals(
                targetFunctionalTerm.getFunctionSymbol())) {
            return Optional.empty();
        }


        /**
         * Source is ground term
         */
        if (isGroundTerm(sourceFunctionalTerm)) {
            if (sourceFunctionalTerm.equals(targetFunctionalTerm)) {
                return Optional.of(EMPTY_SUBSTITUTION);
            }
            else {
                return Optional.empty();
            }
        }

        ImmutableList<? extends ImmutableTerm> sourceChildren = sourceFunctionalTerm.getArguments();
        ImmutableList<? extends ImmutableTerm> targetChildren = targetFunctionalTerm.getArguments();

        /**
         * Arity equality
         */
        int sourceArity = sourceChildren.size();
        if (sourceArity != targetChildren.size()) {
            return Optional.empty();
        }

        /**
         * Children
         */
        // Non-final
        ImmutableSubstitution<ImmutableTerm> unifier = EMPTY_SUBSTITUTION;
        for(int i=0; i < sourceArity ; i++) {

            /**
             * Recursive call
             */
            Optional<ImmutableSubstitution<ImmutableTerm>> optionalChildUnifier = computeUnidirectionalSubstitution(
                    sourceChildren.get(i), targetChildren.get(i));

            if (!optionalChildUnifier.isPresent())
                return Optional.empty();

            ImmutableSubstitution<ImmutableTerm> childUnifier = optionalChildUnifier.get();

            Optional<ImmutableSubstitution<ImmutableTerm>> optionalMergedUnifier = unifier.union(childUnifier);
            if (optionalMergedUnifier.isPresent()) {
                unifier = optionalMergedUnifier.get();
            }
            else {
                return Optional.empty();
            }
        }

        // Present optional
        return Optional.of(unifier);
    }

    /**
     * TODO: explain
     */
    public static ImmutableSubstitution<ImmutableTerm> renameSubstitution(final ImmutableSubstitution<ImmutableTerm> substitutionToRename,
                                                                          final ImmutableList<InjectiveVar2VarSubstitution> renamingSubstitutions) {

        // Non-final
        ImmutableSubstitution<ImmutableTerm> renamedSubstitution = substitutionToRename;
        for (InjectiveVar2VarSubstitution renamingSubstitution : renamingSubstitutions) {
            renamedSubstitution = renamingSubstitution.applyRenaming(renamedSubstitution);
        }

        return renamedSubstitution;
    }

    public static ImmutableSubstitution<VariableOrGroundTerm> convertIntoVariableOrGroundTermSubstitution(
            ImmutableSubstitution<ImmutableTerm> substitution) {
        ImmutableMap.Builder<Variable, VariableOrGroundTerm> substitutionMapBuilder = ImmutableMap.builder();
        for (Map.Entry<Variable, Term> entry : substitution.getMap().entrySet()) {
            VariableOrGroundTerm value = ImmutabilityTools.convertIntoVariableOrGroundTerm(entry.getValue());

            substitutionMapBuilder.put(entry.getKey(), value);
        }
        return new ImmutableSubstitutionImpl<>(substitutionMapBuilder.build());
    }

    public static boolean isInjective(ImmutableSubstitution<? extends VariableOrGroundTerm> substitution) {
        return isInjective(substitution.getImmutableMap());
    }

    public static boolean isInjective(Map<Variable, ? extends VariableOrGroundTerm> substitutionMap) {
        ImmutableSet<VariableOrGroundTerm> valueSet = ImmutableSet.copyOf(substitutionMap.values());
        return valueSet.size() == substitutionMap.keySet().size();
    }

    public static ImmutableSubstitution<Constant> computeNullSubstitution(ImmutableSet<Variable> nullVariables) {
        ImmutableMap<Variable, Constant> map = nullVariables.stream()
                .map(v -> new AbstractMap.SimpleEntry<Variable, Constant>(v, TermConstants.NULL))
                .collect(ImmutableCollectors.toMap());
        return new ImmutableSubstitutionImpl<>(map);
    }
}
