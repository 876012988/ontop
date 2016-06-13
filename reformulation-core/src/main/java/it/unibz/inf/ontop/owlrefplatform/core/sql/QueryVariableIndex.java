package it.unibz.inf.ontop.owlrefplatform.core.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unibz.inf.ontop.model.CQIE;
import it.unibz.inf.ontop.model.Function;
import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.Term;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.sql.DBMetadata;
import it.unibz.inf.ontop.sql.DataDefinition;
import it.unibz.inf.ontop.sql.api.Attribute;

import com.google.common.collect.ImmutableMap;

/**
 * 
 * An object of this class is created for a CQ with the purpose to relate 
 * atoms and their variables to tables and column names.
 * 
 * E.g., for a CQ
 *   
 * 		ans1(t1) :- student(t1, t2, t3, t4), EQ(t4,2)
 * 
 * and a DB table 
 * 
 * 		student(id, name, email, type)
 * 
 * it would relate atom student(t1, t2, t3, t4) to the table student(id, name, email, type), and
 * variable t1 to "id", t2 to "name", t3 to "email" and t4 to "type".
 * 
 */
public class QueryVariableIndex {

	private final ImmutableMap<Variable, String> variableColumnIndex;

	/**
	 * A simple variable-column name index is created in the constructor.
	 */
	public QueryVariableIndex(CQIE cq, DBMetadata metadata) {
		
		Map<Variable, String> varColumnIndex = computeColumnIndex(cq, metadata);
		variableColumnIndex = ImmutableMap.copyOf(varColumnIndex);
	}
	
	protected Map<Variable, String> computeColumnIndex(CQIE cq, DBMetadata metadata) {
		Map<Variable, String> varColIndex = new HashMap<>();
		
		List<Function> body = cq.getBody();
		for (Function atom : body) {
			varColIndex.putAll(computeColumnIndexFromAtom(atom, metadata));
		}
		
		return varColIndex;
	}

	private Map<Variable, String> computeColumnIndexFromAtom(Function atom, DBMetadata metadata) {
		Map<Variable, String> varColIndex = new HashMap<>();
		
		if (!atom.isDataFunction()) {
			return varColIndex;
		}
	
		Predicate tablePredicate = atom.getFunctionSymbol();
		String tableName = tablePredicate.getName();
		DataDefinition def = metadata.getDefinition(tableName);

		if (def == null) {
			return varColIndex;
		}
		
		if (atom.getTerms().size() != def.getAttributes().size()) {
			throw new RuntimeException("Mismatch between " + atom + " and database metadata " + metadata + " arities!");
		}
		
		int i=0;
		for (Term term : atom.getTerms()) {
			if ( term instanceof Variable ) {
				Attribute attribute = def.getAttribute(i+1);
				varColIndex.put((Variable)term, attribute.getName());
			}
			i++;
		}
		
		return varColIndex;
	}

	public String getColumnName(Variable var) {
		return variableColumnIndex.containsKey(var) ? variableColumnIndex.get(var) : null;
	}

}
