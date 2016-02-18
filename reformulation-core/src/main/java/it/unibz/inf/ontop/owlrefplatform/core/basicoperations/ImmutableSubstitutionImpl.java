package it.unibz.inf.ontop.owlrefplatform.core.basicoperations;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.model.ImmutableSubstitution;
import it.unibz.inf.ontop.model.ImmutableTerm;
import it.unibz.inf.ontop.model.Term;
import it.unibz.inf.ontop.model.Variable;

/**
 * Wrapper above a ImmutableMap<Variable, ImmutableTerm> map.
 */
public class ImmutableSubstitutionImpl<T extends ImmutableTerm> extends AbstractImmutableSubstitutionImpl<T> {

    private final ImmutableMap<Variable, T> map;

    public ImmutableSubstitutionImpl(ImmutableMap<Variable, ? extends T> substitutionMap) {
        this.map = (ImmutableMap<Variable, T>) substitutionMap;
    }

    @Override
    public T get(Variable var) {
        return map.get(var);
    }

    @Override
    public ImmutableMap<Variable, T> getImmutableMap() {
        return map;
    }

    @Override
    public boolean isDefining(Variable variable) {
        return map.containsKey(variable);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public final ImmutableMap<Variable, Term> getMap() {
        return (ImmutableMap<Variable, Term>)(ImmutableMap<Variable, ?>) map;
    }

    @Override
    public ImmutableTerm applyToVariable(Variable variable) {
        if (map.containsKey(variable))
            return map.get(variable);
        return variable;
    }

    @Override
    public String toString() {
        return Joiner.on(", ").withKeyValueSeparator("/").join(map);
    }

    @Override
    protected ImmutableSubstitution<T> constructNewSubstitution(ImmutableMap<Variable, T> map) {
        return new ImmutableSubstitutionImpl<>(map);
    }

}
