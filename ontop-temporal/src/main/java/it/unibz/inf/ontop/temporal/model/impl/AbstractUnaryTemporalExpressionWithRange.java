package it.unibz.inf.ontop.temporal.model.impl;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.term.VariableOrGroundTerm;
import it.unibz.inf.ontop.temporal.model.DatalogMTLExpression;
import it.unibz.inf.ontop.temporal.model.TemporalRange;
import it.unibz.inf.ontop.temporal.model.UnaryTemporalExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractUnaryTemporalExpressionWithRange extends AbstractTemporalExpressionWithRange implements UnaryTemporalExpression  {

    private final DatalogMTLExpression operand;

    AbstractUnaryTemporalExpressionWithRange(TemporalRange range, DatalogMTLExpression operand){
        super(range);
        this.operand = operand;
    }

    @Override
    public Iterable<DatalogMTLExpression> getChildNodes() {
        return Arrays.asList(operand);
    }

    @Override
    public DatalogMTLExpression getOperand() {
        return operand;
    }

    @Override
    public ImmutableList<VariableOrGroundTerm> getAllVariableOrGroundTerms(){
        return ImmutableList.copyOf(operand.getAllVariableOrGroundTerms());
    }
}