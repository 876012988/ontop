package org.semanticweb.ontop.model;

import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.model.impl.VariableImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * Generates new variables that are guaranteed to not conflict with
 * already variables in a given scope.
 *
 * The typical scope for variables is the body of a rule.
 */
public class VariableGenerator {

    private int count;
    private final OBDADataFactory dataFactory;
    private final Set<Variable> knownVariables;

    private static String SUFFIX_PREFIX = "f";


    public VariableGenerator(Set<Variable> knownVariables) {
        count = 0;
        dataFactory = OBDADataFactoryImpl.getInstance();
        this.knownVariables = new HashSet<>(knownVariables);
    }

    /**
     * Rule-level variable generator.
     */
    public VariableGenerator(CQIE initialRule) {
        count = 0;
        dataFactory = OBDADataFactoryImpl.getInstance();
        knownVariables = initialRule.getReferencedVariables();
    }

    /**
     * Generates a new non-conflicting variable from a previous one.
     * It will reuse its name.
     */
    public Variable generateNewVariableFromVar(Variable previousVariable) {
        Variable newVariable;
        do {
            newVariable = dataFactory.getVariable(previousVariable.getName() + SUFFIX_PREFIX + (count++));
        } while(knownVariables.contains(newVariable));

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Generates a new variable if a conflict is detected.
     */
    public Variable generateNewVariableIfConflicting(Variable previousVariable) {
        Variable newVariable = previousVariable;
        while(knownVariables.contains(newVariable)) {
            newVariable = dataFactory.getVariable(previousVariable.getName() + SUFFIX_PREFIX + (count++));
        }

        knownVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Generates a new non-conflicting variable.
     */
    public Variable generateNewVariable() {
        Variable newVariable;
        do {
            newVariable = dataFactory.getVariable(SUFFIX_PREFIX + (count++));
        } while(knownVariables.contains(newVariable));

        knownVariables.add(newVariable);
        return newVariable;
    }
}
