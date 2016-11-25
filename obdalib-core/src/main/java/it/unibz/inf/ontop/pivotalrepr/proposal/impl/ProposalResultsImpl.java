package it.unibz.inf.ontop.pivotalrepr.proposal.impl;

import it.unibz.inf.ontop.pivotalrepr.IntermediateQuery;
import it.unibz.inf.ontop.pivotalrepr.proposal.ProposalResults;


public class ProposalResultsImpl implements ProposalResults {

    private final IntermediateQuery resultingQuery;

    public ProposalResultsImpl(IntermediateQuery resultingQuery) {
        this.resultingQuery = resultingQuery;
    }

    @Override
    public IntermediateQuery getResultingQuery() {
        return resultingQuery;
    }
}