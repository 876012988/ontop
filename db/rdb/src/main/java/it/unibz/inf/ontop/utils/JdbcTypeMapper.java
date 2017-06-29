package it.unibz.inf.ontop.utils;

/*
 * #%L
 * ontop-obdalib-core
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

import com.google.inject.Inject;
import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.Predicate.COL_TYPE;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * This class maps SQL datatypes to XML datatypes.
 * Details look at this link: https://docs.google.com/spreadsheet/ccc?key=0AoDXwrYLJ2lvdFAtRXBPcUs2UjMtY08tZ3NYTEp5dVE&usp=sharing
 */
public class JdbcTypeMapper {

	private static JdbcTypeMapper INSTANCE;
	private final Map<Integer, COL_TYPE> sqlToQuest = new HashMap<Integer, COL_TYPE>();
	private final Map<COL_TYPE, Integer> datatypeMap = new HashMap<COL_TYPE, Integer>();

	@Inject
	private JdbcTypeMapper() {
		sqlToQuest.put(Types.VARCHAR, COL_TYPE.STRING);
		sqlToQuest.put(Types.CHAR, COL_TYPE.STRING);
		sqlToQuest.put(Types.LONGNVARCHAR, COL_TYPE.STRING);
		sqlToQuest.put(Types.LONGVARCHAR, COL_TYPE.STRING);
		sqlToQuest.put(Types.NVARCHAR, COL_TYPE.STRING);
		sqlToQuest.put(Types.NCHAR, COL_TYPE.STRING);
		sqlToQuest.put(Types.INTEGER, COL_TYPE.INTEGER);
		sqlToQuest.put(Types.BIGINT, COL_TYPE.INTEGER);
		sqlToQuest.put(Types.SMALLINT, COL_TYPE.INTEGER);
		sqlToQuest.put(Types.TINYINT, COL_TYPE.INTEGER);
		sqlToQuest.put(Types.NUMERIC, COL_TYPE.DECIMAL);
		sqlToQuest.put(Types.DECIMAL, COL_TYPE.DECIMAL);
		sqlToQuest.put(Types.FLOAT, COL_TYPE.DOUBLE); // R: why not FLOAT?
		sqlToQuest.put(Types.DOUBLE, COL_TYPE.DOUBLE);
		sqlToQuest.put(Types.REAL, COL_TYPE.DOUBLE);
		sqlToQuest.put(Types.DATE, COL_TYPE.DATE);
		sqlToQuest.put(Types.TIME, COL_TYPE.TIME);
		sqlToQuest.put(Types.TIMESTAMP, COL_TYPE.DATETIME); //GX: needs check
		sqlToQuest.put(Types.TIMESTAMP_WITH_TIMEZONE, COL_TYPE.DATETIME); //BC: needs check
		sqlToQuest.put(Types.BOOLEAN, COL_TYPE.BOOLEAN);
		sqlToQuest.put(Types.BIT, COL_TYPE.BOOLEAN);
//		typeMapper.put(Types.BINARY, dfac.getDataTypePredicateBinary());
//		typeMapper.put(Types.VARBINARY, dfac.getDataTypePredicateBinary());
//		typeMapper.put(Types.BLOB, dfac.getDataTypePredicateBinary());
		sqlToQuest.put(Types.CLOB, COL_TYPE.STRING);
		sqlToQuest.put(Types.OTHER, COL_TYPE.STRING);

		datatypeMap.put(COL_TYPE.BOOLEAN, Types.BOOLEAN);
		datatypeMap.put(COL_TYPE.INT, Types.INTEGER);
		datatypeMap.put(COL_TYPE.INTEGER, Types.BIGINT);
		datatypeMap.put(COL_TYPE.DECIMAL, Types.DECIMAL); //BC: needs to be checked
		datatypeMap.put(COL_TYPE.LONG, Types.BIGINT);
		datatypeMap.put(COL_TYPE.NEGATIVE_INTEGER, Types.BIGINT);
		datatypeMap.put(COL_TYPE.POSITIVE_INTEGER, Types.BIGINT);
		datatypeMap.put(COL_TYPE.NON_POSITIVE_INTEGER, Types.BIGINT);
		datatypeMap.put(COL_TYPE.UNSIGNED_INT, Types.INTEGER);
		datatypeMap.put(COL_TYPE.FLOAT, Types.FLOAT);
		datatypeMap.put(COL_TYPE.DOUBLE, Types.DOUBLE);
		datatypeMap.put(COL_TYPE.STRING, Types.VARCHAR);
		datatypeMap.put(COL_TYPE.LITERAL, Types.VARCHAR);
		datatypeMap.put(COL_TYPE.DATETIME_STAMP, Types.TIMESTAMP);
		// all other COL_TYPEs are mapped to Types.VARCHAR by default
	}

	@Deprecated
	public static JdbcTypeMapper getInstance() {
		if (INSTANCE == null)
			INSTANCE = new JdbcTypeMapper();
		return INSTANCE;
	}

	public Predicate.COL_TYPE getPredicate(int sqlType) {
		Predicate.COL_TYPE type = sqlToQuest.get(sqlType);
		if (type == null)
			type = COL_TYPE.STRING; //treat them as simple literal

		return type;
	}

	public int getSQLType(Predicate.COL_TYPE type) {
		if (type != null) {
			Integer sqlType = datatypeMap.get(type);
			if (sqlType != null)
				return sqlType;
		}
		// Return varchar for unknown
		return Types.VARCHAR;
	}
	
}
