package it.unibz.inf.ontop.owlrefplatform.core.basicoperations;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMultimap;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.ontology.ClassExpression;
import it.unibz.inf.ontop.ontology.DataPropertyExpression;
import it.unibz.inf.ontop.ontology.DataSomeValuesFrom;
import it.unibz.inf.ontop.ontology.OClass;
import it.unibz.inf.ontop.ontology.ObjectPropertyExpression;
import it.unibz.inf.ontop.ontology.ObjectSomeValuesFrom;
import it.unibz.inf.ontop.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.inf.ontop.owlrefplatform.core.dagjgrapht.TBoxReasoner;

public class LinearInclusionDependencies {

    private static final OBDADataFactory ofac = OBDADataFactoryImpl.getInstance();
    
	private final Map<Predicate, List<CQIE>> rules;

	public LinearInclusionDependencies() {
		rules = new HashMap<>();
	}

	public LinearInclusionDependencies(ImmutableMultimap<AtomPredicate, CQIE> predicateRuleMap) {
		rules = predicateRuleMap.asMap().entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> new ArrayList<>(e.getValue())
				));
	}

	public List<CQIE> getRules(Predicate pred) {
		List<CQIE> rrs = rules.get(pred);
		if (rrs == null)
			return Collections.emptyList();
		
		return rrs;
	}
		
	/*
	 * adds a rule to the indexed linear dependencies
	 * 
	 * @param head: atom
	 * @param body: atom
	 */
	public void addRule(Function head, Function body) {
        CQIE rule = ofac.getCQIE(head, body);
		
        List<CQIE> list = rules.get(body.getFunctionSymbol());
        if (list == null) {
        	list = new LinkedList<>();
        	rules.put(body.getFunctionSymbol(), list);
        }
		
        list.add(rule);		
	}
	
	public static LinearInclusionDependencies getABoxDependencies(TBoxReasoner reasoner, boolean full) {
		LinearInclusionDependencies dependencies = new LinearInclusionDependencies();
		
		for (Equivalences<ObjectPropertyExpression> propNode : reasoner.getObjectPropertyDAG()) {
			// super might be more efficient
			for (Equivalences<ObjectPropertyExpression> subpropNode : reasoner.getObjectPropertyDAG().getSub(propNode)) {
				for (ObjectPropertyExpression subprop : subpropNode) {
					if (subprop.isInverse())
						continue;
					
	                Function body = translate(subprop);

	                for (ObjectPropertyExpression prop : propNode)  {
	                	if (prop == subprop)
	                		continue;
	                	
		                Function head = translate(prop);	
		                dependencies.addRule(head, body);
					}
				}
			}
		}
		for (Equivalences<DataPropertyExpression> propNode : reasoner.getDataPropertyDAG()) {
			// super might be more efficient
			for (Equivalences<DataPropertyExpression> subpropNode : reasoner.getDataPropertyDAG().getSub(propNode)) {
				for (DataPropertyExpression subprop : subpropNode) {
					
	                Function body = translate(subprop);

	                for (DataPropertyExpression prop : propNode)  {
	                	if (prop == subprop)
	                		continue;
	                	
		                Function head = translate(prop);	
		                dependencies.addRule(head, body);
					}
				}
			}
		}
		for (Equivalences<ClassExpression> classNode : reasoner.getClassDAG()) {
			// super might be more efficient
			for (Equivalences<ClassExpression> subclassNode : reasoner.getClassDAG().getSub(classNode)) {
				for (ClassExpression subclass : subclassNode) {

	                Function body = translate(subclass, variableYname);
                	//if (!(subclass instanceof OClass) && !(subclass instanceof PropertySomeRestriction))
	                if (body == null)
	                	continue;

	                for (ClassExpression cla : classNode)  {
	                	if (!(cla instanceof OClass) && !(!full && ((cla instanceof ObjectSomeValuesFrom) || (cla instanceof DataSomeValuesFrom))))
	                		continue;
	                	
	                	if (cla == subclass)
	                		continue;

	                	// use a different variable name in case the body has an existential as well
		                Function head = translate(cla, variableZname);	
		                dependencies.addRule(head, body);
					}
				}
			}
		}
		
		return dependencies;
	}

	private static final String variableXname = "x";
	private static final String variableYname = "y";
	private static final String variableZname = "z";
	
    private static Function translate(ObjectPropertyExpression property) {
		final Variable varX = ofac.getVariable(variableXname);
		final Variable varY = ofac.getVariable(variableYname);

		if (property.isInverse()) 
			return ofac.getFunction(property.getPredicate(), varY, varX);
		else 
			return ofac.getFunction(property.getPredicate(), varX, varY);
	}
    
    private static Function translate(DataPropertyExpression property) {
		final Variable varX = ofac.getVariable(variableXname);
		final Variable varY = ofac.getVariable(variableYname);

		return ofac.getFunction(property.getPredicate(), varX, varY);
	}
	
    private static Function translate(ClassExpression description, String existentialVariableName) {
		final Variable varX = ofac.getVariable(variableXname);
		if (description instanceof OClass) {
			OClass klass = (OClass) description;
			return ofac.getFunction(klass.getPredicate(), varX);
		} 
		else if (description instanceof ObjectSomeValuesFrom) {
			final Variable varY = ofac.getVariable(existentialVariableName);
			ObjectPropertyExpression property = ((ObjectSomeValuesFrom) description).getProperty();
			if (property.isInverse()) 
				return ofac.getFunction(property.getPredicate(), varY, varX);
			else 
				return ofac.getFunction(property.getPredicate(), varX, varY);
		} 
		else {
			assert (description instanceof DataSomeValuesFrom);
			final Variable varY = ofac.getVariable(existentialVariableName);
			DataPropertyExpression property = ((DataSomeValuesFrom) description).getProperty();
			return ofac.getFunction(property.getPredicate(), varX, varY);
		} 
	}

    @Override
    public String toString() {
    	return rules.toString();
    }
}
