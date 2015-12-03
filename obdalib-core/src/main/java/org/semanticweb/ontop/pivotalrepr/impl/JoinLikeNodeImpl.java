package org.semanticweb.ontop.pivotalrepr.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.semanticweb.ontop.model.ImmutableBooleanExpression;
import org.semanticweb.ontop.model.Variable;
import org.semanticweb.ontop.pivotalrepr.JoinLikeNode;

public abstract class JoinLikeNodeImpl extends JoinOrFilterNodeImpl implements JoinLikeNode {

    protected JoinLikeNodeImpl(Optional<ImmutableBooleanExpression> optionalJoinCondition) {
        super(optionalJoinCondition);
    }

}
