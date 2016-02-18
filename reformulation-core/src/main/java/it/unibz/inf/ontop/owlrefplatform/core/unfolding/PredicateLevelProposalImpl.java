package it.unibz.inf.ontop.owlrefplatform.core.unfolding;

import fj.F;
import fj.P;
import fj.P2;
import fj.data.HashMap;
import fj.data.List;
import fj.data.Option;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.owlrefplatform.core.basicoperations.SubstitutionUtilities;
import it.unibz.inf.ontop.model.Substitution;

/**
 * From a high-level point of view, this proposal is done by looking at (i) the children proposals and (ii) the rules defining the parent predicate.
 *
 * Its implementation relies on RuleLevelProposals.
 *
 * Fundamental assumption: definition rules (parent rules) USE THE SAME VARIABLE NAMES.
 * It seems to be OK with the way the SPARQL-to-Datalog works (in March 2015).
 * ---> the predicate-level substitution makes sense (union of rule-level ones).
 *
 */
public class PredicateLevelProposalImpl implements PredicateLevelProposal {

    private final List<RuleLevelProposal> ruleProposals;
    private final TypeProposal typeProposal;

    /**
     * Constructs the RuleLevelProposals and makes a TypeProposal.
     *
     * May throw a MultiTypeException.
     */
    public PredicateLevelProposalImpl(List<CQIE> parentRules, HashMap<Predicate, PredicateLevelProposal> childProposalIndex)
            throws TypeLiftTools.MultiTypeException {
        if (parentRules.isEmpty()) {
            throw new IllegalArgumentException("Parent rules are required for making a proposal.");
        }

        /**
         * Computes the RuleLevelProposals and the global substitution.
         *
         */
        P2<List<RuleLevelProposal>, Substitution> results = computeRuleProposalsAndSubstitution(parentRules, childProposalIndex);
        ruleProposals = results._1();
        Substitution globalSubstitution = results._2();

        /**
         * Derives the type proposal from the first untyped definition rule and the global substitution.
         */
        typeProposal = TypeLiftTools.makeTypeProposal(parentRules.head(), globalSubstitution);
    }

    @Override
    public TypeProposal getTypeProposal() {
        return typeProposal;
    }

    @Override
    public Predicate getPredicate() {
        return getTypeProposal().getPredicate();
    }

    /**
     * Returns the typed rules produced by the RuleLevelProposals.
     */
    @Override
    public List<CQIE> getTypedRules() {
        return ruleProposals.map(new F<RuleLevelProposal, CQIE>() {
            @Override
            public CQIE f(RuleLevelProposal ruleLevelProposal) {
                return ruleLevelProposal.getTypedRule();
            }
        });
    }

    @Override
    public List<CQIE> getDetypedRules() {
        return ruleProposals.map(new F<RuleLevelProposal, CQIE>() {
            @Override
            public CQIE f(RuleLevelProposal ruleLevelProposal) {
                return ruleLevelProposal.getDetypedRule();
            }
        });
    }

    /**
     * Entry point of the homonym recursive function.
     */
    private static P2<List<RuleLevelProposal>, Substitution> computeRuleProposalsAndSubstitution(List<CQIE> parentRules,
                                                                                            HashMap<Predicate, PredicateLevelProposal> childProposalIndex)
            throws TypeLiftTools.MultiTypeException {
        return computeRuleProposalsAndSubstitution(Option.<Substitution>none(), parentRules, List.<RuleLevelProposal>nil(),
                childProposalIndex);
    }

    /**
     * Creates RuleLevelProposals and computes the global substitution as the union of the substitutions they propose.
     *
     * Tail-recursive function.
     *
     * TODO: Make sure the notion of global substitution makes sense.
     * What if definitions of the same ans() atom use different variables?
     */
    private static P2<List<RuleLevelProposal>, Substitution> computeRuleProposalsAndSubstitution(Option<Substitution> optionalSubstitution,
                                                                                            List<CQIE> remainingRules,
                                                                                            List<RuleLevelProposal> ruleProposals,
                                                                                            HashMap<Predicate, PredicateLevelProposal> childProposalIndex)
            throws TypeLiftTools.MultiTypeException {
        /**
         * Stop condition (no more rule to consider).
         */
        if (remainingRules.isEmpty()) {
            if (optionalSubstitution.isNone()) {
                throw new IllegalArgumentException("Do not give a None substitution with an empty list of rules");
            }
            return P.p(ruleProposals, optionalSubstitution.some());
        }

        /**
         * Makes a RuleLevelProposal out of the current rule.
         */
        CQIE currentRule = remainingRules.head();
        RuleLevelProposal newRuleLevelProposal = new RuleLevelProposalImpl(currentRule, childProposalIndex);

        /**
         * Updates the global substitution by computes the union of it with the substitution proposed by the rule.
         *
         * If the union is impossible (i.e. does not produce a valid substitution), throws a MultiTypedException.
         *
         */
        Option<Substitution> proposedSubstitution;
        if (optionalSubstitution.isNone()) {
            proposedSubstitution = Option.some(newRuleLevelProposal.getTypingSubstitution());
        }
        else {
            try {
                proposedSubstitution = Option.some(SubstitutionUtilities.union(optionalSubstitution.some(), newRuleLevelProposal.getTypingSubstitution()));
            }
            /**
             * Impossible to compute the union of two substitutions.
             * This happens when multiple types are proposed for this predicate.
             */
            catch(SubstitutionUtilities.SubstitutionException e) {
                throw new TypeLiftTools.MultiTypeException();
            }
        }

        // Appends the new RuleLevelProposal to the list
        List<RuleLevelProposal> newRuleProposalList = ruleProposals.append(List.cons(newRuleLevelProposal,
                List.<RuleLevelProposal>nil()));

        /**
         * Tail recursion
         */
        return computeRuleProposalsAndSubstitution(proposedSubstitution, remainingRules.tail(),
                newRuleProposalList, childProposalIndex);
    }
}
