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
import java.util.HashMap;
import java.util.Map;

public class OracleSQLDialectAdapter extends SQL99DialectAdapter {

	private static Map<Integer, String> SqlDatatypes;
	static {
		SqlDatatypes = new HashMap<Integer, String>();
		SqlDatatypes.put(Types.DECIMAL, "NUMBER");
		SqlDatatypes.put(Types.FLOAT, "FLOAT");
		SqlDatatypes.put(Types.CHAR, "CHAR");
		SqlDatatypes.put(Types.VARCHAR, "VARCHAR(4000)");
		SqlDatatypes.put(Types.CLOB, "CLOB");
		SqlDatatypes.put(Types.TIMESTAMP, "TIMESTAMP");
		SqlDatatypes.put(Types.INTEGER, "INTEGER");
		SqlDatatypes.put(Types.REAL, "NUMBER");
		SqlDatatypes.put(Types.FLOAT, "NUMBER");
		SqlDatatypes.put(Types.DOUBLE, "NUMBER");
//		SqlDatatypes.put(Types.DOUBLE, "DECIMAL"); // it fails aggregate test with double
		SqlDatatypes.put(Types.DATE, "TIMESTAMP");
		SqlDatatypes.put(Types.TIME, "TIME");
		SqlDatatypes.put(Types.BOOLEAN, "BOOLEAN");
	}
	
	@Override
	public String sqlSlice(long limit, long offset) {
		return String.format("WHERE ROWNUM <= %s", limit);
	}

	@Override
	public String sqlCast(String value, int type) {
		String strType = SqlDatatypes.get(type);
		boolean noCast = strType.equals("BOOLEAN");

		if (strType != null && !noCast ) {	
			return "CAST(" + value + " AS " + strType + ")";
		} else	if (noCast){
				return value;
			
		}
		throw new RuntimeException("Unsupported SQL type");
	}
	
	@Override
	public String sqlRegex(String columnname, String pattern, boolean caseinSensitive, boolean multiLine, boolean dotAllMode) {
		pattern = pattern.substring(1, pattern.length() - 1); // remove the
																// enclosing
																// quotes
		String flags = "";
		if(caseinSensitive)
			flags += "i";
		else
			flags += "c";
		if (multiLine)
			flags += "m";
		if(dotAllMode)
			flags += "n";
		
		String sql = " REGEXP_LIKE " + "( " + columnname + " , '" + pattern + "' , '" + flags  + "' )";
		return sql;
	}
}
