package unibz.inf.ontop.pivotalrepr.impl;

import java.util.Optional;
import com.google.common.collect.ImmutableList;
import unibz.inf.ontop.model.OrderCondition;
import unibz.inf.ontop.pivotalrepr.ImmutableQueryModifiers;
import unibz.inf.ontop.pivotalrepr.QueryModifiers;

public class ImmutableQueryModifiersImpl implements ImmutableQueryModifiers {

    private final boolean isDistinct;
    private final long limit;
    private final long offset;
    private final ImmutableList<OrderCondition> sortConditions;

    public ImmutableQueryModifiersImpl(boolean isDistinct, long limit,
                                       long offset, ImmutableList<OrderCondition> sortConditions) {
        this.isDistinct = isDistinct;
        this.limit = limit;
        this.offset = offset;
        this.sortConditions = sortConditions;
    }

    /**
     * Tip: use a mutable implementation of QueryModifiers
     * as a builder and then create an immutable object with this constructor.
     */
    public ImmutableQueryModifiersImpl(QueryModifiers modifiers) {

        isDistinct = modifiers.isDistinct();
        limit = modifiers.getLimit();
        offset = modifiers.getOffset();
        sortConditions = ImmutableList.copyOf(modifiers.getSortConditions());

        if (!hasModifiers()) {
            throw new IllegalArgumentException("Empty QueryModifiers given." +
                    "Please use an Optional instead of creating an empty object.");
        }

    }

    @Override
    public boolean isDistinct() {
        return isDistinct;
    }


    @Override
    public boolean hasOrder() {
        return !sortConditions.isEmpty() ? true : false;
    }

    @Override
    public boolean hasLimit() {
        return limit != -1 ? true : false;
    }

    @Override
    public long getLimit() {
        return limit;
    }

    @Override
    public boolean hasOffset() {
        return offset != -1 ? true : false;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public ImmutableList<OrderCondition> getSortConditions() {
        return sortConditions;
    }

    @Override
    public Optional<ImmutableQueryModifiers> newSortConditions(ImmutableList<OrderCondition> newSortConditions) {
        if (isDistinct || hasLimit() || hasOffset() || (!newSortConditions.isEmpty())) {
            ImmutableQueryModifiers newModifiers = new ImmutableQueryModifiersImpl(isDistinct, limit, offset,
                    newSortConditions);
            return Optional.of(newModifiers);
        }

        return Optional.empty();
    }

    private boolean hasModifiers() {
        return isDistinct || hasLimit() || hasOffset() || hasOrder();
    }
}
