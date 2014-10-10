package org.semanticweb.ontop.sql;

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

import org.semanticweb.ontop.sql.api.Attribute;

import java.util.Map;

public class ViewDefinition extends DataDefinition {


	private static final long serialVersionUID = 3312336193514797486L;

	private String statement;
	
	/*
	 * TODO: Check if really needed
	 */
	public ViewDefinition() {
		super();
	}
	
	public ViewDefinition(String name) {
		super(name);
	}

    public ViewDefinition(String name, Map<Integer, Attribute> attributes, String statement) {
        super(name, attributes);
        this.statement = statement;
    }

    @Override
    public DataDefinition cloneDefinition() {
        return new ViewDefinition(name, attributes, statement);
    }


    @Deprecated
	public void copy(String statement) {
		this.statement = statement;
	}
	
	public void setSQL(String statement) {
		this.statement = statement;
	}

	public String getStatement() {
		return statement;
	}

	@Override
	public String toString() {
		StringBuilder bf = new StringBuilder();
		bf.append(name);
		bf.append("[");
		boolean comma = false;
		for (Integer i : attributes.keySet()) {
			if (comma) {
				bf.append(",");
			}
			bf.append(attributes.get(i));
			comma = true;
		}
		bf.append("]");

		bf.append(String.format("   (%s)", statement));
		return bf.toString();
	}
}
