package it.unibz.inf.ontop.owlrefplatform.core.translator;

/*
 * #%L
 * ontop-reformulation-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.*;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;

import java.util.ArrayList;
import java.util.List;

public class SesameConstructTemplate {
    private TupleExpr projection = null;
	private TupleExpr extension = null;

    public SesameConstructTemplate(String strquery) throws MalformedQueryException {
		QueryParser qp = QueryParserUtil.createParser(QueryLanguage.SPARQL);
		ParsedQuery pq = qp.parseQuery(strquery, null); // base URI is null
        TupleExpr sesameAlgebra = pq.getTupleExpr();
		Reduced r = (Reduced) sesameAlgebra;
		projection = r.getArg();
		TupleExpr texpr = null;
		if (projection instanceof MultiProjection) {
			 texpr = ((MultiProjection) projection).getArg();
		} else {
			 texpr = ((Projection) projection).getArg();
		}
		if (texpr!= null && texpr instanceof Extension) 
			extension = texpr;
		
	}
	
	public List<ProjectionElemList> getProjectionElemList() {
		List<ProjectionElemList> projElemList = new ArrayList<>();
		if (projection instanceof Projection) {
			projElemList.add(((Projection) projection).getProjectionElemList());
		}
		else if (projection instanceof MultiProjection) {
			projElemList = ((MultiProjection) projection).getProjections();
		}
		return projElemList;
	}

	public Extension getExtension() {
		return (Extension) extension;
	}
}
