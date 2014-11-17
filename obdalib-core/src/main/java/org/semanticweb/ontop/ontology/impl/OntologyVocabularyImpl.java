package org.semanticweb.ontop.ontology.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.impl.OBDAVocabulary;
import org.semanticweb.ontop.ontology.*;

public class OntologyVocabularyImpl implements OntologyVocabulary {

	private static OntologyFactory ofac = OntologyFactoryImpl.getInstance();
	
	// signature
	
	private final Set<OClass> concepts = new HashSet<OClass>();

	// private final Set<Datatype> datatypes = new HashSet<Datatype>();
	
	private final Set<ObjectPropertyExpression> objectProperties = new HashSet<ObjectPropertyExpression>();

	private final Set<DataPropertyExpression> dataProperties = new HashSet<DataPropertyExpression>();
	
	// auxiliary symbols and built-in datatypes 
	
	private final static Set<Predicate> builtinDatatypes = initializeReserved();

	private static Set<Predicate> initializeReserved() { // static block
		Set<Predicate> datatypes = new HashSet<Predicate>();
		datatypes.add(OBDAVocabulary.RDFS_LITERAL);
		datatypes.add(OBDAVocabulary.XSD_STRING);
		datatypes.add(OBDAVocabulary.XSD_INTEGER);
		datatypes.add(OBDAVocabulary.XSD_NEGATIVE_INTEGER);
		datatypes.add(OBDAVocabulary.XSD_NON_NEGATIVE_INTEGER);
		datatypes.add(OBDAVocabulary.XSD_POSITIVE_INTEGER);
		datatypes.add(OBDAVocabulary.XSD_NON_POSITIVE_INTEGER);
		datatypes.add(OBDAVocabulary.XSD_INT);
		datatypes.add(OBDAVocabulary.XSD_UNSIGNED_INT);
		datatypes.add(OBDAVocabulary.XSD_FLOAT);
		datatypes.add(OBDAVocabulary.XSD_LONG);
		datatypes.add(OBDAVocabulary.XSD_DECIMAL);
		datatypes.add(OBDAVocabulary.XSD_DOUBLE);
		datatypes.add(OBDAVocabulary.XSD_DATETIME);
		datatypes.add(OBDAVocabulary.XSD_BOOLEAN);
		return datatypes;
	}
	
	public static final OClass owlThing = ofac.createClass("http://www.w3.org/2002/07/owl#Thing");
	public static final OClass owlNothing = ofac.createClass("http://www.w3.org/2002/07/owl#Nothing");
	public static final ObjectPropertyExpression owlTopObjectProperty = ofac.createObjectProperty("http://www.w3.org/2002/07/owl#topObjectProperty");
	public static final ObjectPropertyExpression owlBottomObjectProperty = ofac.createObjectProperty("http://www.w3.org/2002/07/owl#bottomObjectProperty");
	public static final DataPropertyExpression owlTopDataProperty = ofac.createDataProperty("http://www.w3.org/2002/07/owl#topDataProperty");
	public static final DataPropertyExpression owlBottomDataProperty = ofac.createDataProperty("http://www.w3.org/2002/07/owl#bottomDataProperty");
	
	
	@Override
	public void declareClass(String uri) {
		OClass cd = ofac.createClass(uri);
		if (!cd.equals(owlThing) && !cd.equals(owlNothing))
			concepts.add(cd);
	}

	@Override
	public void declareObjectProperty(String uri) {
		ObjectPropertyExpression rd = ofac.createObjectProperty(uri);
		if (!rd.equals(owlTopObjectProperty) && !rd.equals(owlBottomObjectProperty))
			objectProperties.add(rd);
	}
	
	@Override
	public void declareDataProperty(String uri) {
		DataPropertyExpression rd = ofac.createDataProperty(uri);
		if (!rd.equals(owlTopDataProperty) && !rd.equals(owlBottomDataProperty))
			dataProperties.add(rd);
	}

	@Override
	public Set<OClass> getClasses() {
		return Collections.unmodifiableSet(concepts);
	}

	@Override
	public Set<ObjectPropertyExpression> getObjectProperties() {
		return Collections.unmodifiableSet(objectProperties);
	}

	@Override
	public Set<DataPropertyExpression> getDataProperties() {
		return Collections.unmodifiableSet(dataProperties);
	}
	
	public static final String AUXROLEURI = "ER.A-AUXROLE"; // TODO: make private
	
	public static boolean isAuxiliaryProperty(PropertyExpression role) {
		return role.getPredicate().getName().toString().startsWith(AUXROLEURI);	
	}

	
	void addReferencedEntries(BasicClassDescription desc) {
		if (desc instanceof OClass) {
			OClass cl = (OClass)desc;
			if (!cl.equals(owlThing) && !cl.equals(owlNothing))
				concepts.add(cl);
		}
		else if (desc instanceof SomeValuesFrom) 
			addReferencedEntries(((SomeValuesFrom) desc).getProperty());
		else if (desc instanceof Datatype)  {
			// NO-OP
			// datatypes.add((Datatype) desc);
		}
		else if (desc instanceof DataPropertyRangeExpression) {
			addReferencedEntries(((DataPropertyRangeExpression) desc).getProperty());			
		}
		else 
			throw new UnsupportedOperationException("Cant understand: " + desc.toString());
	}
	
	void addReferencedEntries(PropertyExpression prop) {
		if (prop instanceof ObjectPropertyExpression) {
			ObjectPropertyExpression p = (ObjectPropertyExpression)prop;
			if (p.isInverse())
				p = p.getInverse();
			if (!p.equals(owlTopObjectProperty) && !p.equals(owlBottomObjectProperty))
				objectProperties.add(p);
		}
		else {
			DataPropertyExpression p = (DataPropertyExpression)prop;
			if (p.isInverse())
				p = p.getInverse();
			if (!p.equals(owlTopDataProperty) && !p.equals(owlBottomDataProperty))
				dataProperties.add(p);
		}
	}
	
	
	
	void checkSignature(BasicClassDescription desc) {
		
		if (desc instanceof OClass) {
			if (!concepts.contains(desc) && !desc.equals(owlThing) && !desc.equals(owlNothing))
				throw new IllegalArgumentException("Class predicate is unknown: " + desc);
		}	
		else if (desc instanceof Datatype) {
			Predicate pred = ((Datatype) desc).getPredicate();
			if (!builtinDatatypes.contains(pred)) 
				throw new IllegalArgumentException("Datatype predicate is unknown: " + pred);
		}
		else if (desc instanceof SomeValuesFrom) {
			checkSignature(((SomeValuesFrom) desc).getProperty());
		}
		else if (desc instanceof DataPropertyRangeExpression) {
			checkSignature(((DataPropertyRangeExpression) desc).getProperty());
		}
		else 
			throw new UnsupportedOperationException("Cant understand: " + desc);
	}

	void checkSignature(PropertyExpression prop) {

		if (prop.isInverse()) {
			checkSignature(prop.getInverse());
		}
		else {
			// Make sure we never validate against auxiliary roles introduced by
			// the translation of the OWL ontology
			if (isAuxiliaryProperty(prop)) 
				return;
			
			if ((prop instanceof ObjectPropertyExpression) && 
					!objectProperties.contains(prop) && 
					!prop.equals(owlTopObjectProperty) &&
					!prop.equals(owlBottomObjectProperty)) 
				throw new IllegalArgumentException("At least one of these predicates is unknown: " + prop);
			
			if ((prop instanceof DataPropertyExpression) && 
					!dataProperties.contains(prop) &&
					!prop.equals(owlTopDataProperty) && 
					!prop.equals(owlBottomDataProperty)) 
				throw new IllegalArgumentException("At least one of these predicates is unknown: " + prop);
		}
	}

	@Override
	public void merge(OntologyVocabulary v) {
		concepts.addAll(v.getClasses());
		objectProperties.addAll(v.getObjectProperties());
		dataProperties.addAll(v.getDataProperties());
	}

	@Override
	public boolean isEmpty() {
		return concepts.isEmpty() && objectProperties.isEmpty() && dataProperties.isEmpty();
	}
		
	
}
