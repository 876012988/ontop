package it.unibz.inf.ontop.owlrefplatform.core.unfolding;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fj.*;
import fj.data.HashMap;
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.model.impl.OBDAVocabulary;
import it.unibz.inf.ontop.owlrefplatform.core.basicoperations.SubstitutionImpl;
import it.unibz.inf.ontop.model.impl.TermUtils;
import it.unibz.inf.ontop.owlrefplatform.core.basicoperations.SubstitutionUtilities;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Collections of functions for lifting types.
 */
public class TypeLiftTools {

    /**
     * Thrown after receiving an SubstitutionException.
     *
     * This indicates that the predicate for which the type propagation
     * has been tried should be considered as multi-typed.
     */
    protected static class MultiTypeException extends Exception {
    }

    /**
     * Returns true if one term of the atom corresponds to a URI template.
     */
    public static boolean containsURITemplate(it.unibz.inf.ontop.model.Function atom) {
        for(Term term : atom.getTerms()) {
            if (isURITemplate(term))
                return true;
        }
        return false;
    }

    /**
     * A URI template is composed of one string and of variables.
     * It has the URI function symbol.
     */
    public static boolean isURITemplate(Term term) {
        if (!(term instanceof Function))
            return false;

        Function functionalTerm = (Function) term;

        if (functionalTerm.getFunctionSymbol().getName().equals(OBDAVocabulary.QUEST_URI)) {
            /**
             * Distinguish normal URI types from URI templates
             */
            return functionalTerm.getTerms().size() > 1;
        }
        return false;
    }


    /**
     * Looks for predicates are not yet declared as multi-typed (while they should).
     *
     * This tests relies on the ability of rules defining one predicate to be unified.
     *
     * This class strongly relies on the assumption that the multi-typed predicate index is complete.
     * This method offers such a protection against non-detections by previous components.
     */
    protected static Multimap<Predicate, Integer> updateMultiTypedFunctionSymbolIndex(final TreeBasedDatalogProgram initialDatalogProgram,
                                                                                      final Multimap<Predicate, Integer> multiTypedFunctionSymbolIndex) {
        // Mutable index (may be updated)
        final Multimap<Predicate, Integer> newIndex = ArrayListMultimap.create(multiTypedFunctionSymbolIndex);

        final Stream<P2<Predicate, List<CQIE>>> ruleEntries = Stream.iterableStream(initialDatalogProgram.getRuleTree());
        /**
         * Applies the following effect on each rule entry:
         *   If the predicate has not been declared as multi-typed, checks if it really is.
         *
         *   When a false negative is detected, adds it to the index (side-effect).
         */
        ruleEntries.foreach(new F<P2<Predicate, List<CQIE>>, Unit>() {
            @Override
            public Unit f(P2<Predicate, List<CQIE>> ruleEntry) {
                Predicate predicate = ruleEntry._1();
                if (multiTypedFunctionSymbolIndex.containsKey(predicate))
                    return Unit.unit();

                List<CQIE> rules = ruleEntry._2();
                if (isMultiTypedPredicate(rules)) {
                    // TODO: Is there some usage for this count?
                    int count = 1;
                    newIndex.put(predicate, count);
                }
                return Unit.unit();
            }
        });
        return newIndex;
    }

    /**
     * Tests if the rules defining one predicate cannot be unified
     * because they have different types.
     *
     * Returns true if the predicate is detected as multi-typed
     * (or some of its rules are not supported).
     *
     * TODO: make a clear distinction between multi-typed and having some unsupported rules.
     *
     */
    private static boolean isMultiTypedPredicate(List<CQIE> predicateDefinitionRules) {
        /**
         * TODO: after removing isRuleSupportedForTypeLift, test len(predicateDefRules <= 1
         */
        if (predicateDefinitionRules.isEmpty())
            return false;

        CQIE currentRule = predicateDefinitionRules.head();

        Function headFirstRule = currentRule.getHead();

        return isMultiTypedPredicate(constructTypeProposal(headFirstRule), predicateDefinitionRules.tail());
    }

    /**
     * Tail recursive sub-method that "iterates" over the rules.
     */
    private static boolean isMultiTypedPredicate(TypeProposal currentTypeProposal, List<CQIE> remainingRules) {
        if (remainingRules.isEmpty())
            return false;

        CQIE currentRule = remainingRules.head();

        Function ruleHead = currentRule.getHead();
        try {
            Function newType = applyTypeProposal(ruleHead, currentTypeProposal);

            // Tail recursion
            return isMultiTypedPredicate(new BasicTypeProposal(newType), remainingRules.tail());
            /**
             * Multi-type problem detected
             */
        } catch (SubstitutionUtilities.SubstitutionException e) {
            return true;
        }
    }

    /**
     * Propagates type from a typeProposal to one target atom.
     */
    public static Function applyTypeProposal(Function targetAtom, TypeProposal typeProposal) throws SubstitutionUtilities.SubstitutionException {
        Substitution substitution = computeTypePropagatingSubstitution(typeProposal.getExtendedTypedAtom(), targetAtom);

        // Mutable object
        Function newHead = (Function) targetAtom.clone();
        // Limited side-effect
        SubstitutionUtilities.applySubstitution(newHead, substitution);

        return newHead;
    }

    /**
     * Sometimes rule bodies contains algebra functions (e.g. left joins).
     * These should not be considered as atoms.
     *
     * These method makes sure only real (non algebra) atoms are returned.
     * Some of these atoms may be found inside algebra functions.
     *
     */
    public static List<Function> extractBodyAtoms(CQIE rule) {
        List<Function> directBody = List.iterableList(rule.getBody());

        return directBody.bind(new F<Function, List<Function>>() {
            @Override
            public List<Function> f(Function atom) {
                return extractAtoms(atom);
            }
        });
    }

    /**
     * Extracts real atoms from a functional term.
     *
     * If this functional term is not algebra, it is an atom and is
     * thus directly returned.
     *
     * Otherwise, looks for atoms recursively by looking
     * at the functional sub terms for the algebra function.
     *
     * Recursive function.
     *
     * TODO: Improvement: transform into a F() object.
     */
    private static List<Function> extractAtoms(Function atom) {
        /**
         * Normal case: not an algebra function (e.g. left join).
         */
        if (!atom.isAlgebraFunction()) {
            return List.cons(atom, List.<Function>nil());
        }

        /**
         * Sub-terms that are functional.
         */
        List<Function> subAtoms = List.iterableList(atom.getTerms()).filter(new F<Term, Boolean>() {
            @Override
            public Boolean f(Term term) {
                return term instanceof Function;
            }
        }).map(new F<Term, Function>() {
            @Override
            public Function f(Term term) {
                return (Function) term;
            }
        });

        /**
         * Recursive call over these sub-atoms.
         * The atoms they returned are then joined.
         * Their union is then returned.
         */
        return subAtoms.bind(new F<Function, List<Function>>() {
            @Override
            public List<Function> f(Function subAtom) {
                return extractAtoms(subAtom);
            }
        });

    }

    public static Function removeTypeFromAtom(Function atom) {
        List<Term> initialHeadTerms =  List.iterableList(atom.getTerms());

        /**
         * Computes untyped arguments for the head predicate.
         */
        List<Term> newHeadTerms = Option.somes(initialHeadTerms.map(new F<Term, Option<Term>>() {
            @Override
            public Option<Term> f(Term term) {
                return untypeTerm(term);
            }
        }));

        /**
         * Builds a new atom.
         */
        Function newAtom = (Function)atom.clone();
        newAtom.updateTerms(new ArrayList<>(newHeadTerms.toCollection()));
        return newAtom;
    }

    /**
     * Removes the type for a given term.
     * This method also deals with special cases that should not be untyped.
     *
     * Note that type removal only concern functional terms.
     * If the returned value is None, the term must be eliminated.
     *
     */
    public static Option<Term> untypeTerm(Term term) {
        /**
         * Types are assumed to functional terms.
         *
         * Other type of terms are not concerned.
         */
        if (!(term instanceof Function)) {
            return Option.some(term);
        }

        /**
         * Special case that should not be untyped:
         *   - Aggregates
         */
        if (DatalogUnfolder.detectAggregateInArgument(term))
            return Option.some(term);


        /**
         * Special case: URI templates --> to be removed.
         *
         */
        if (isURITemplate(term)) {
            return Option.none();
        }

        /**
         * Other functional terms are expected to be type
         * and to have an arity of 1.
         *
         * Raises an exception if it is not the case.
         */
        Function functionalTerm = (Function) term;
        java.util.List<Term> functionArguments = functionalTerm.getTerms();
        if (functionArguments.size() != 1) {
            throw new RuntimeException("Removing types of non-unary functional terms is not supported.");
        }
        return Option.some(functionArguments.get(0));
    }

    /**
     * Constructs a TypeProposal of the proper type.
     */
    public static TypeProposal constructTypeProposal(Function unextendedTypedAtom) {
        /**
         * Special case: multi-variate URI template.
         */
        if (containsURITemplate(unextendedTypedAtom)) {
            return new UriTemplateTypeProposal(unextendedTypedAtom);
        }
        /**
         * Default case
         */
        return new BasicTypeProposal(unextendedTypedAtom);
    }

    /**
     * Makes a TypeProposal by applying the substitution to the head of the rule.
     */
    public static TypeProposal makeTypeProposal(CQIE untypedRule, Substitution typePropagationSubstitution) {
        final Function unextendedTypeAtom = (Function) untypedRule.getHead().clone();
        // Side-effect!
        SubstitutionUtilities.applySubstitution(unextendedTypeAtom, typePropagationSubstitution);
        final TypeProposal newProposal = constructTypeProposal(unextendedTypeAtom);

        return newProposal;
    }

    /**
     * Computes a substitution able to propagate types from a source atom to a target atom.
     *
     * Assumption: the target atom is only composed of (possibly typed) variables!
     */
    public static Substitution computeTypePropagatingSubstitution(final Function sourceAtom, final Function targetAtom)
            throws SubstitutionUtilities.SubstitutionException {

        /**
         * Predicate and arity checks
         */
        if ((!sourceAtom.getFunctionSymbol().equals(targetAtom.getFunctionSymbol()))
                || (sourceAtom.getTerms().size() != targetAtom.getTerms().size())) {
            throw new SubstitutionUtilities.SubstitutionException();
        }

        /**
         * The target atom is expected to be only composed of variables.
         * Extracts them in a list.
         */
        List<Variable> targetVariables = extractVariablesFromTargetAtom(targetAtom);

        /**
         * Gets a cleaned atom that uses the same variables that the target one.
         */
        Function renamedSourceAtom = renameAndCleanSourceAtomForTypeProg(sourceAtom, targetVariables);

        /**
         * Computes a new substitution that replaces some target variables by their typed versions.
         *
         * Note that their typed versions are functional terms.
         *
         *  { Target variable --> typed version }
         */
        List<P2<Variable, Term>> targetSourceFunctionPairs = targetVariables.zip(List.iterableList(renamedSourceAtom.getTerms()));
        HashMap<Variable, Term> variableToTypeMappings = HashMap.from(targetSourceFunctionPairs.filter(new F<P2<Variable, Term>, Boolean>() {
            @Override
            public Boolean f(P2<Variable, Term> pair) {
                return pair._2() instanceof Function;
            }
        }));

        return new SubstitutionImpl(variableToTypeMappings.toMap());
    }

    /**
     * Extracts the variable from an atom.
     *
     * It must be only composed of (possibly typed) variables.
     */
    private static List<Variable> extractVariablesFromTargetAtom(final Function targetAtom) {
        List<Option<Variable>> variableList = List.iterableList(targetAtom.getTerms()).map(new F<Term, Option<Variable>>() {
            @Override
            public Option<Variable> f(Term term) {
                if (term instanceof Variable)
                    return Option.some((Variable) term);

                else if (term instanceof Function) {
                    Function functionalTerm = (Function) term;
                    Predicate functionSymbol = functionalTerm.getFunctionSymbol();

                    // Yes, this is horrible
                    if (functionalTerm.getTerms().size() == 0)
                        throw new IllegalArgumentException("Inconsistent functional term " + functionalTerm);

                    /**
                     * Datatype or URI the first sub-term must be a variable.
                     *
                     * URI templates are considered here.
                     */
                    if (functionalTerm.isDataTypeFunction() || functionSymbol.getName().equals(OBDAVocabulary.QUEST_URI)) {
                        Term firstTerm = functionalTerm.getTerm(0);
                        if (firstTerm instanceof Variable)
                            return Option.some((Variable) firstTerm);
                    }
                }
                return Option.none();
            }
        });

        return Option.somes(variableList);
    }

    /**
     * Cleans out the aggregation functional terms from the source atom and replaces its variables
     * by the ones used by the target atom.
     */
    private static Function renameAndCleanSourceAtomForTypeProg(Function sourceAtom, List<Variable> targetVariables) {
        /**
         * Cleans out the aggregation functional terms from the source atom.
         */
        Function cleanedSourceAtom = cleanTypedAtom(sourceAtom);

        /**
         * Computes the substitution that maps the source variable to the target variables.
         *
         * { Source variable --> target variable }
         *
         * This is only possible for source terms that contains exactly 1 variable. Others are ignored.
         *
         */
        List<P2<Term, Variable>> sourceTargetPairs = List.iterableList(sourceAtom.getTerms()).zip(targetVariables);
        HashMap<Variable, Term> variableMappings = HashMap.from(Option.somes(sourceTargetPairs.map(
                new F<P2<Term, Variable>, Option<P2<Variable, Term>>>() {
                    @Override
                    public Option<P2<Variable, Term>> f(P2<Term, Variable> pair) {
                        Term sourceTerm = pair._1();
                        Variable targetVariable = pair._2();

                        /**
                         * Extracts the variable if it is unique.
                         * Otherwise, ignores this pair.
                         */
                        Set<Variable> sourceVars = new HashSet<>();
                        TermUtils.addReferencedVariablesTo(sourceVars,sourceTerm);

                        if (sourceVars.size() != 1)
                            return Option.none();
                        Variable sourceVariable = sourceVars.iterator().next();

                        return Option.some(P.p(sourceVariable , (Term) targetVariable));
                    }
                })));
        Substitution targetToSourceVarSubstitution = new SubstitutionImpl(variableMappings.toMap());

        /**
         * Applies this substitution to the cleaned source atom.
         */
        Function renamedSourceAtom = (Function) cleanedSourceAtom.clone();
        //SIDE-EFFECT!
        SubstitutionUtilities.applySubstitution(renamedSourceAtom, targetToSourceVarSubstitution);
        return renamedSourceAtom;
    }

    /**
     * Cleans out the aggregation functional terms.
     * Just keep types and variables.
     *
     * Very fragile code... A better data structure is needed!
     */
    private static Function cleanTypedAtom(final Function atom) {
        List<Term> newTerms = List.iterableList(atom.getTerms()).map(new F<Term, Term>() {
            @Override
            public Term f(Term term) {
                if (term instanceof Function) {
                    Function functionalTerm = (Function) term;
                    Predicate functionSymbol = functionalTerm.getFunctionSymbol();

                    if (functionalTerm.isDataTypeFunction()) {
                        java.util.List<Term> newSubTerms = cleanAlreadyTypedSubTerms(functionalTerm.getTerms());
                        Function newTerm = (Function) functionalTerm.clone();
                        newTerm.updateTerms(newSubTerms);
                        return newTerm;
                    }
                    /**
                     * MIN, MAX, SUM, AVG, etc. but NOT COUNT
                     */
                    else if (functionSymbol instanceof ExpressionOperation) {

                        switch((ExpressionOperation) functionSymbol) {
                            case AVG:
                            case SUM:
                            case MAX:
                            case MIN:
                                /**
                                 * TODO: make it stronger.
                                 */
                                java.util.List<Term> subTerms = functionalTerm.getTerms();
                                if (subTerms.size() != 1)
                                    throw new RuntimeException("Non unary aggregation functions are not yet supported" + atom);

                                /**
                                 * Returns the unique sub-term
                                 */
                                return subTerms.get(0);
                            case COUNT:
                                throw new RuntimeException("COUNT functional term should already be typed! " + atom);
                            default:
                                break;
                        }
                    }
                }
                /**
                 * Otherwise, keeps the term.
                 */
                return term;
            }
        });

        Function newAtom = OBDADataFactoryImpl.getInstance().getFunction(atom.getFunctionSymbol(),
                new ArrayList<>(newTerms.toCollection()));
        return newAtom;
    }

    /**
     * Cleans each (already typed) sub-term.
     */
    private static java.util.List<Term> cleanAlreadyTypedSubTerms(java.util.List<Term> terms) {
        java.util.List<Term> cleanedTerms = new ArrayList<>();
        for (Term term : terms) {
            cleanedTerms.add(cleanAlreadyTypedSubTerm(term));
        }
        return cleanedTerms;
    }

    /**
     * Cleaning an already typed sub-term only affects functional sub-terms: they are
     * replaced by new variables.
     */
    private static Term cleanAlreadyTypedSubTerm(Term subTerm) {
        if (subTerm instanceof Function) {
            // New variable
            return OBDADataFactoryImpl.getInstance().getVariable("v" + UUID.randomUUID());
        }
        /**
         * Otherwise, keeps the term.
         */
        return subTerm;
    }

}
