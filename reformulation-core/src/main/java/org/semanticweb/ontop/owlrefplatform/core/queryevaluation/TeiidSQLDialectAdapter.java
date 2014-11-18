package org.semanticweb.ontop.owlrefplatform.core.queryevaluation;

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

import java.sql.Types;

public class TeiidSQLDialectAdapter extends SQL99DialectAdapter {

	@Override
	public String sqlCast(String value, int type) {
		String strType = null;
		if (type == Types.VARCHAR) {
			strType = "VARCHAR";
		} else {
			throw new RuntimeException("Unsupported SQL type");
		}
		return "CAST(" + value + " AS " + strType + ")";
	}

	/**
	 * See https://docs.jboss.org/teiid/7.7.0.Final/reference/en-US/html/sql_clauses.html#limit_clause
	 */
	@Override
	public String sqlSlice(long limit, long offset) {
		if ((limit < 0) && (offset < 0)) {
			return "";
		}
		else if ((limit >= 0) && (offset >= 0)) {
			return String.format("LIMIT %d, %d", offset, limit);
		}
		else if (offset < 0) {
			return String.format("LIMIT %d", limit);
		}
		// Else -> (limit < 0)
		else {
			return String.format("OFFSET %d ROWS", offset);
		}
	}
}
