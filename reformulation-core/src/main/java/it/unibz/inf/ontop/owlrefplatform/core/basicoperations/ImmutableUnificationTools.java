package it.unibz.inf.ontop.owlrefplatform.core.basicoperations;

import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.model.impl.AtomPredicateImpl;
import it.unibz.inf.ontop.model.impl.ImmutabilityTools;

import java.util.*;

import static it.unibz.inf.ontop.owlrefplatform.core.basicoperations.ImmutableSubstitutionTools.convertSubstitution;

/**
 * Tools for new-gen immutable unifying substitutions.
 */
public class ImmutableUnificationTools {

    /**
     * TODO: explain
     */
    private static class UnificationException extends Exception {
    }

    /**
     * TODO: explain
     *
     * MUTABLE
     */
    private static class TermPair {
        private boolean canBeRemoved;
        private Variable leftVariable;
        private ImmutableTerm rightTerm;

        private TermPair(Variable variable, ImmutableTerm rightTerm) {
            this.leftVariable = variable;
            this.rightTerm = rightTerm;
            this.canBeRemoved = false;
        }

        /**
         *
         * TODO: explain it
         *
         * May update itself.
         * Returns a list of new pairs
         *
         */
        public ImmutableList<TermPair> applySubstitution(ImmutableSubstitution<? extends ImmutableTerm> substitution)
                throws UnificationException {
            if (canBeRemoved) {
                return ImmutableList.of();
            }

            ImmutableTerm transformedLeftTerm = substitution.apply(leftVariable);
            ImmutableTerm transformedRightTerm = substitution.apply(rightTerm);

            if (transformedLeftTerm instanceof Variable) {
                leftVariable = (Variable) transformedLeftTerm;
                rightTerm = transformedRightTerm;
                return ImmutableList.of();
            }
            else if (transformedRightTerm instanceof Variable) {
                leftVariable = (Variable) transformedRightTerm;
                rightTerm = transformedLeftTerm;
                return ImmutableList.of();
            }
            else {
                Optional<ImmutableSubstitution<ImmutableTerm>> optionalUnifier =
                        computeDirectedMGU(transformedLeftTerm, transformedRightTerm);
                if (!optionalUnifier.isPresent()) {
                    throw new UnificationException();
                }
                /**
                 * TODO: explain
                 */
                else {
                    canBeRemoved = true;
                    leftVariable = null;
                    rightTerm = null;
                    return convertIntoPairs(optionalUnifier.get());
                }
            }
        }

        public ImmutableSubstitution<ImmutableTerm> getSubstitution() {
            if (canBeRemoved) {
                return EMPTY_SUBSTITUTION;
            }
            return new ImmutableSubstitutionImpl<>(ImmutableMap.of(leftVariable, rightTerm));
        }

        public boolean canBeRemoved() {
            return canBeRemoved;
        }

        public Variable getLeftVariable() {
            return leftVariable;
        }

        public ImmutableTerm getRightTerm() {
            return rightTerm;
        }

    }


    private static ImmutableSubstitution<ImmutableTerm> EMPTY_SUBSTITUTION = new NeutralSubstitution();
    private static String PREDICATE_STR = "pred";

    /**
     * TODO: explain
     *
     */
    public static Optional<ImmutableSubstitution<ImmutableTerm>> computeMGU(ImmutableFunctionalTerm term1, ImmutableFunctionalTerm term2) {
        Substitution mutableSubstitution = UnifierUtilities.getMGU(term1, term2);

        if (mutableSubstitution == null) {
            return Optional.empty();
        }
        return Optional.of(convertSubstitution(mutableSubstitution));
    }

    public static Optional<ImmutableSubstitution<VariableOrGroundTerm>> computeAtomMGU(DataAtom atom1, DataAtom atom2) {
        Substitution mutableSubstitution = UnifierUtilities.getMGU(atom1, atom2);

        if (mutableSubstitution == null) {
            return Optional.empty();
        }

        ImmutableMap.Builder<Variable, VariableOrGroundTerm> substitutionMapBuilder = ImmutableMap.builder();
        for (Map.Entry<Variable, Term> entry : mutableSubstitution.getMap().entrySet()) {
            VariableOrGroundTerm value = ImmutabilityTools.convertIntoVariableOrGroundTerm(entry.getValue());

            substitutionMapBuilder.put(entry.getKey(), value);
        }

        ImmutableSubstitution<VariableOrGroundTerm> immutableSubstitution = new ImmutableSubstitutionImpl<>(substitutionMapBuilder.build());
        return Optional.of(immutableSubstitution);

    }

    /**
     * Computes one Most General Unifier (MGU) of (two) substitutions.
     */
    public static Optional<ImmutableSubstitution<ImmutableTerm>> computeMGUS(ImmutableSubstitution<? extends ImmutableTerm> substitution1,
                                                                             ImmutableSubstitution<? extends ImmutableTerm> substitution2) {

        ImmutableList.Builder<ImmutableTerm> firstArgListBuilder = ImmutableList.builder();
        ImmutableList.Builder<ImmutableTerm> secondArgListBuilder = ImmutableList.builder();

        for (Map.Entry<Variable, ? extends ImmutableTerm> entry : substitution1.getImmutableMap().entrySet()) {
            firstArgListBuilder.add(entry.getKey());
            secondArgListBuilder.add(entry.getValue());
        }

        for (Map.Entry<Variable, ? extends ImmutableTerm> entry : substitution2.getImmutableMap().entrySet()) {
            firstArgListBuilder.add(entry.getKey());
            secondArgListBuilder.add(entry.getValue());
        }

        ImmutableList<ImmutableTerm> firstArgList = firstArgListBuilder.build();
        ImmutableList<ImmutableTerm> secondArgList = secondArgListBuilder.build();

        OBDADataFactory factory = OBDADataFactoryImpl.getInstance();
        Predicate predicate = new AtomPredicateImpl(PREDICATE_STR, firstArgList.size());

        ImmutableFunctionalTerm functionalTerm1 = factory.getImmutableFunctionalTerm(predicate, firstArgList);
        ImmutableFunctionalTerm functionalTerm2 = factory.getImmutableFunctionalTerm(predicate, secondArgList);

        return computeMGU(functionalTerm1, functionalTerm2);
    }

    public static Optional<ImmutableSubstitution<VariableOrGroundTerm>> computeAtomMGUS(
            ImmutableSubstitution<VariableOrGroundTerm> substitution1,
            ImmutableSubstitution<VariableOrGroundTerm> substitution2) {
        Optional<ImmutableSubstitution<ImmutableTerm>> optionalMGUS = computeMGUS(substitution1, substitution2);
        if (optionalMGUS.isPresent()) {
            return Optional.of(ImmutableSubstitutionTools.convertIntoVariableOrGroundTermSubstitution(
                    optionalMGUS.get()));
        }
        else {
            return Optional.empty();
        }
    }


    /**
     * Computes a MGU that reuses as much as possible the variables from the target part.
     *
     */
    public static Optional<ImmutableSubstitution<ImmutableTerm>> computeDirectedMGU(ImmutableTerm sourceTerm,
                                                                                    ImmutableTerm targetTerm) {
        /**
         * Variable
         */
        if (sourceTerm instanceof Variable) {
            Variable sourceVariable = (Variable) sourceTerm;

            // Constraint
            if ((targetTerm instanceof ImmutableFunctionalTerm)
                    && ((ImmutableFunctionalTerm) targetTerm).getVariables().contains(sourceVariable)) {
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
            ImmutableFunctionalTerm sourceFunctionalTerm = (ImmutableFunctionalTerm) sourceTerm;

            if (targetTerm instanceof Variable) {
                Variable targetVariable = (Variable) targetTerm;

                // Constraint
                if (sourceFunctionalTerm.getVariables().contains(targetVariable)) {
                    return Optional.empty();
                }
                else {
                    ImmutableSubstitution<ImmutableTerm> substitution = new ImmutableSubstitutionImpl<>(
                            ImmutableMap.of(targetVariable, sourceTerm));
                    return Optional.of(substitution);
                }
            }
            else if (targetTerm instanceof ImmutableFunctionalTerm) {
                return computeDirectedMGUOfTwoFunctionalTerms((ImmutableFunctionalTerm) sourceTerm,
                        (ImmutableFunctionalTerm) targetTerm);
            }
            else {
                return Optional.empty();
            }
        }
        /**
         * Constant
         */
        else if(sourceTerm instanceof Constant) {
            if (targetTerm instanceof Variable) {
                Variable targetVariable = (Variable) targetTerm;
                ImmutableSubstitution<ImmutableTerm> substitution = new ImmutableSubstitutionImpl<>(
                        ImmutableMap.of(targetVariable, sourceTerm));
                return Optional.of(substitution);
            }
            else if (sourceTerm.equals(targetTerm)) {
                return Optional.of(EMPTY_SUBSTITUTION);
            }
            else {
                return Optional.empty();
            }
        }
        else {
            throw new RuntimeException("Unexpected term: " + sourceTerm + " (" + sourceTerm.getClass() + ")");
        }
    }

    /**
     * TODO: explain
     */
    private static Optional<ImmutableSubstitution<ImmutableTerm>> computeDirectedMGUOfTwoFunctionalTerms(
            ImmutableFunctionalTerm sourceTerm, ImmutableFunctionalTerm targetTerm) {
        /**
         * Function symbol equality
         */
        if (!sourceTerm.getFunctionSymbol().equals(
                targetTerm.getFunctionSymbol())) {
            return Optional.empty();
        }

        ImmutableList<? extends ImmutableTerm> sourceChildren = sourceTerm.getArguments();
        ImmutableList<? extends ImmutableTerm> targetChildren = targetTerm.getArguments();

        int childNb = sourceChildren.size();
        if (targetChildren.size() != childNb) {
            return Optional.empty();
        }

        final ImmutableList.Builder<TermPair> pairBuilder = ImmutableList.builder();
        for (int i=0; i < childNb; i++) {
            /**
             * Recursive
             */
            Optional<ImmutableSubstitution<ImmutableTerm>> optionalChildTermUnifier =
                    computeDirectedMGU(sourceChildren.get(i), targetChildren.get(i));

            /**
             * If the unification of one pair of sub-terms is not possible,
             * no global unification is possible.
             */
            if (!optionalChildTermUnifier.isPresent()) {
                return Optional.empty();
            }

            /**
             * Adds all its pairs
             */
            pairBuilder.addAll(convertIntoPairs(optionalChildTermUnifier.get()));
        }

        return unifyPairs(pairBuilder.build());
    }

    /**
     * TODO: explain
     *
     */
    private static Optional<ImmutableSubstitution<ImmutableTerm>> unifyPairs(ImmutableList<TermPair> originalPairs) {

        /**
         * Some pairs will be removed, some others will be added.
         */
        List<TermPair> allPairs = new LinkedList<>(originalPairs);
        Queue<TermPair> pairsToVisit = new LinkedList<>(originalPairs);

        try {
            /**
             * TODO: explain
             */
            while (!pairsToVisit.isEmpty()) {
                TermPair currentPair = pairsToVisit.poll();
                if (currentPair.canBeRemoved()) {
                    continue;
                }
                ImmutableSubstitution<ImmutableTerm> substitution = currentPair.getSubstitution();

                List<TermPair> additionalPairs = new ArrayList<>();
                /**
                 * TODO: explain
                 */
                Iterator<TermPair> it = allPairs.iterator();
                while (it.hasNext()) {
                    TermPair pairToUpdate = it.next();
                    if (pairToUpdate == currentPair) {
                        continue;
                    } else {
                        additionalPairs.addAll(pairToUpdate.applySubstitution(substitution));
                        if (pairToUpdate.canBeRemoved()) {
                            it.remove();
                        }
                    }
                }
                allPairs.addAll(additionalPairs);
                pairsToVisit.addAll(additionalPairs);
            }
            return Optional.of(convertPairs2Substitution(allPairs));
        }
        catch (UnificationException e) {
            return Optional.empty();
        }
    }

    private static ImmutableList<TermPair> convertIntoPairs(ImmutableSubstitution<? extends ImmutableTerm> substitution) {
        ImmutableList.Builder<TermPair> listBuilder = ImmutableList.builder();
        for (Map.Entry<Variable, ? extends ImmutableTerm> entry : substitution.getImmutableMap().entrySet()) {
            listBuilder.add(new TermPair(entry.getKey(), entry.getValue()));
        }
        return listBuilder.build();
    }

    private static ImmutableSubstitution<ImmutableTerm> convertPairs2Substitution(List<TermPair> pairs)
            throws UnificationException {
        Map<Variable, ImmutableTerm> substitutionMap = new HashMap<>();
        for(TermPair pair : pairs) {
            Variable leftVariable = pair.getLeftVariable();
            ImmutableTerm rightTerm = pair.getRightTerm();
            if (!substitutionMap.containsKey(leftVariable)) {
                substitutionMap.put(leftVariable, rightTerm);
            }
            else if (!substitutionMap.get(leftVariable).equals(rightTerm)) {
                throw new UnificationException();
            }
        }
        return new ImmutableSubstitutionImpl<>(ImmutableMap.copyOf(substitutionMap));
    }

}
