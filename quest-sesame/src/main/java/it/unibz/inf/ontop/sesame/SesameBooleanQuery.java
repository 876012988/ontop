package it.unibz.inf.ontop.sesame;

/*
 * #%L
 * ontop-quest-sesame
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

import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import it.unibz.inf.ontop.model.OBDAQueryModifiers;
import it.unibz.inf.ontop.model.TupleResultSet;
import it.unibz.inf.ontop.owlrefplatform.core.QuestDBConnection;
import it.unibz.inf.ontop.owlrefplatform.core.QuestDBStatement;

public class SesameBooleanQuery extends SesameAbstractQuery implements BooleanQuery {

	public SesameBooleanQuery(String queryString, String baseURI, QuestDBConnection conn) throws MalformedQueryException {
        super(queryString, conn);
		// check if valid query string
//		if (queryString.contains("ASK")) {
//		} else
//			throw new MalformedQueryException("Boolean Query expected!");
	}

	public boolean evaluate() throws QueryEvaluationException {
		TupleResultSet rs = null;
		QuestDBStatement stm = null;
		try {
			stm = conn.createStatement();
			rs = (TupleResultSet) stm.execute(getQueryString());
			boolean next = rs.nextRow();
			if (next){
				return true;
				
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			throw new QueryEvaluationException(e);
		} finally {
			try {
				if (rs != null)
				rs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (stm != null)
				stm.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public OBDAQueryModifiers getQueryModifiers() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setQueryModifiers(OBDAQueryModifiers modifiers) {
		// TODO Auto-generated method stub

	}

    @Override
    public void setMaxExecutionTime(int maxExecTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMaxExecutionTime() {
        throw new UnsupportedOperationException();
    }
}