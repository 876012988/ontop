package org.semanticweb.ontop.pivotalrepr.proposal;

import com.google.common.collect.ImmutableList;

/**
 * TODO: explain
 */
public interface SubstitutionLiftProposal extends QueryOptimizationProposal<ProposalResults> {

    ImmutableList<BindingTransfer> getBindingTransfers();

    ImmutableList<ConstructionNodeUpdate> getNodeUpdates();
}
