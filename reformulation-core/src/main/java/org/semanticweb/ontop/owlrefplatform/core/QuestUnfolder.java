package org.semanticweb.ontop.owlrefplatform.core;


import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import org.semanticweb.ontop.model.*;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.model.impl.OBDAVocabulary;
import org.semanticweb.ontop.ontology.ClassAssertion;
import org.semanticweb.ontop.ontology.DataPropertyAssertion;
import org.semanticweb.ontop.ontology.ObjectPropertyAssertion;
import org.semanticweb.ontop.owlrefplatform.core.basicoperations.*;
import org.semanticweb.ontop.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import org.semanticweb.ontop.owlrefplatform.core.mappingprocessing.MappingDataTypeRepair;
import org.semanticweb.ontop.owlrefplatform.core.mappingprocessing.TMappingProcessor;
import org.semanticweb.ontop.owlrefplatform.core.unfolding.DatalogUnfolder;
import org.semanticweb.ontop.owlrefplatform.core.unfolding.UnfoldingMechanism;
import org.semanticweb.ontop.sql.DBMetadata;
import org.semanticweb.ontop.utils.Mapping2DatalogConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class QuestUnfolder {

	/* The active unfolding engine */
	private UnfoldingMechanism unfolder;

	/**
	 * TODO: this structure is not thread-safe at all...
	 */
	private final DBMetadata metadata;

	private final Mapping2DatalogConverter analyzer;

	/* As unfolding OBDAModel, but experimental */
	private List<CQIE> unfoldingProgram;

	/*
	 * These are pattern matchers that will help transforming the URI's in
	 * queries into Functions, used by the SPARQL translator.
	 */
	private UriTemplateMatcher uriTemplateMatcher = new UriTemplateMatcher();


	private static final Logger log = LoggerFactory.getLogger(QuestUnfolder.class);

	private static final OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

	public QuestUnfolder(List<OBDAMappingAxiom> mappings, DBMetadata metadata)
	{
		this.metadata = metadata;

		analyzer = new Mapping2DatalogConverter(metadata);

		unfoldingProgram = analyzer.constructDatalogProgram(mappings);
	}

	public List<CQIE> getRules() {
		return unfoldingProgram;
	}


	/**
	 * Should only be called by the Quest instance
	 *
	 */
	protected void setup() {
		setupUnfolder();
	}

	/**
	 * Setting up the unfolder and SQL generation
	 */
	private void setupUnfolder() {

		// Collecting URI templates
		generateURITemplateMatchers();

		// Adding "triple(x,y,z)" mappings for support of unbounded
		// predicates and variables as class names (implemented in the
		// sparql translator)
		unfoldingProgram.addAll(generateTripleMappings());

		Map<Predicate, List<Integer>> pkeys = DBMetadata.extractPKs(metadata, unfoldingProgram);

		log.debug("Final set of mappings: \n {}", Joiner.on("\n").join(unfoldingProgram));


		unfolder = new DatalogUnfolder(unfoldingProgram, pkeys);
	}

	/**
	 * Only in version 2. TODO: see if still relevant.
	 */
	public Multimap<Predicate, Integer> processMultipleTemplatePredicates() {

		return unfolder.processMultipleTemplatePredicates(unfoldingProgram);

	}

	public void applyTMappings(/*boolean optimizeMap, */ TBoxReasoner reformulationReasoner, boolean full) throws OBDAException  {

		final long startTime = System.currentTimeMillis();

		unfoldingProgram = TMappingProcessor.getTMappings(unfoldingProgram, reformulationReasoner, full);

		// Eliminating redundancy from the unfolding program
		// TODO: move the foreign-key optimisation inside t-mapping generation 
		//              -- at this point it has little effect
		LinearInclusionDependencies foreignKeyRules = DBMetadataUtil.generateFKRules(metadata);
		CQContainmentCheckUnderLIDs foreignKeyCQC = new CQContainmentCheckUnderLIDs(foreignKeyRules);

		Collections.sort(unfoldingProgram, CQCUtilities.ComparatorCQIE);
		CQCUtilities.removeContainedQueries(unfoldingProgram, foreignKeyCQC);

		final long endTime = System.currentTimeMillis();
		log.debug("TMapping size: {}", unfoldingProgram.size());
		log.debug("TMapping processing time: {} ms", (endTime - startTime));
	}

	/***
	 * Adding data typing on the mapping axioms.
	 */

	public void extendTypesWithMetadata(TBoxReasoner tBoxReasoner) throws OBDAException {

		MappingDataTypeRepair typeRepair = new MappingDataTypeRepair(metadata);
		typeRepair.insertDataTyping(unfoldingProgram, tBoxReasoner);
	}

	/***
	 * Adding NOT NULL conditions to the variables used in the head
	 * of all mappings to preserve SQL-RDF semantics
	 */

	public void addNOTNULLToMappings() {

		for (CQIE mapping : unfoldingProgram) {
			Set<Variable> headvars = mapping.getHead().getReferencedVariables();
			for (Variable var : headvars) {
				Function notnull = fac.getFunctionIsNotNull(var);
				List<Function> body = mapping.getBody();
				if (!body.contains(notnull)) {
					body.add(notnull);
				}
			}
		}
	}

	/**
	 * Normalizing language tags. Making all LOWER CASE
	 */

	public void normalizeLanguageTagsinMappings() {
		for (CQIE mapping : unfoldingProgram) {
			Function head = mapping.getHead();
			for (Term term : head.getTerms()) {
				if (!(term instanceof Function)) {
					continue;
				}
				Function typedTerm = (Function) term;
				Predicate type = typedTerm.getFunctionSymbol();

				if (typedTerm.getTerms().size() != 2 || !type.getName().toString().equals(OBDAVocabulary.RDFS_LITERAL_URI))
					continue;
				/*
				 * changing the language, its always the second inner term
				 * (literal,lang)
				 */
				Term originalLangTag = typedTerm.getTerm(1);
				Term normalizedLangTag = null;

				if (originalLangTag instanceof Constant) {
					ValueConstant originalLangConstant = (ValueConstant) originalLangTag;
					normalizedLangTag = fac.getConstantLiteral(originalLangConstant.getValue().toLowerCase(), originalLangConstant.getType());
				} else {
					normalizedLangTag = originalLangTag;
				}
				typedTerm.setTerm(1, normalizedLangTag);
			}
		}
	}

	/**
	 * Normalizing equalities
	 */

	public void normalizeEqualities() {

		for (CQIE cq: unfoldingProgram)
			EQNormalizer.enforceEqualities(cq);

	}

	/***
	 * Adding ontology assertions (ABox) as rules (facts, head with no body).
	 */
	public void addClassAssertionsAsFacts(Iterable<ClassAssertion> assertions) {

		int count = 0;
		for (ClassAssertion ca : assertions) {
			ObjectConstant c = ca.getIndividual();
			Predicate p = ca.getConcept().getPredicate();
			Function head = fac.getFunction(p,
					fac.getUriTemplate(fac.getConstantLiteral(c.getName())));
			CQIE rule = fac.getCQIE(head, Collections.<Function> emptyList());

			unfoldingProgram.add(rule);
			count++;
		}
		log.debug("Appended {} ABox assertions as fact rules", count);
	}

	public void addObjectPropertyAssertionsAsFacts(Iterable<ObjectPropertyAssertion> assertions) {

		int count = 0;
		for (ObjectPropertyAssertion pa : assertions) {
			ObjectConstant s = pa.getSubject();
			ObjectConstant o = pa.getObject();
			Predicate p = pa.getProperty().getPredicate();
			Function head = fac.getFunction(p,
					fac.getUriTemplate(fac.getConstantLiteral(s.getName())),
					fac.getUriTemplate(fac.getConstantLiteral(o.getName())));
			CQIE rule = fac.getCQIE(head, Collections.<Function> emptyList());

			unfoldingProgram.add(rule);
			count++;
		}
		log.debug("Appended {} ABox assertions as fact rules", count);
	}

	public void addDataPropertyAssertionsAsFacts(Iterable<DataPropertyAssertion> assertions) {

//		int count = 0;
//		for (DataPropertyAssertion a : assertions) {
		// WE IGNORE DATA PROPERTY ASSERTIONS UNTIL THE NEXT RELEASE
//			DataPropertyAssertion ca = (DataPropertyAssertion) assertion;
//			ObjectConstant s = ca.getObject();
//			ValueConstant o = ca.getValue();
//			String typeURI = getURIType(o.getType());
//			Predicate p = ca.getPredicate();
//			Predicate urifuction = factory.getUriTemplatePredicate(1);
//			head = factory.getFunction(p, factory.getFunction(urifuction, s), factory.getFunction(factory.getPredicate(typeURI,1), o));
//			rule = factory.getCQIE(head, new LinkedList<Function>());
//		} 	

//		}
//		log.debug("Appended {} ABox assertions as fact rules", count);		
	}




	private void generateURITemplateMatchers() {

		Set<String> templateStrings = new HashSet<>();
		Map<Pattern, Function> matchers = new HashMap<>();

		for (CQIE mapping : unfoldingProgram) { // int i = 0; i < unfoldingProgram.getRules().size(); i++) {

			// Looking for mappings with exactly 2 data atoms
			// CQIE mapping = unfoldingProgram.getRules().get(i);
			Function head = mapping.getHead();

			/*
			 * Collecting URI templates and making pattern matchers for them.
			 */
			for (Term term : head.getTerms()) {
				if (!(term instanceof Function)) {
					continue;
				}
				Function fun = (Function) term;
				if (!(fun.getFunctionSymbol() instanceof URITemplatePredicate)) {
					continue;
				}
				/*
				 * This is a URI function, so it can generate pattern matchers
				 * for the URIS. We have two cases, one where the arity is 1,
				 * and there is a constant/variable. <p> The second case is
				 * where the first element is a string template of the URI, and
				 * the rest of the terms are variables/constants
				 */
				if (fun.getTerms().size() == 1) {
					/*
					 * URI without template, we get it directly from the column
					 * of the table, and the function is only f(x)
					 */
					if (templateStrings.contains("(.+)")) {
						continue;
					}
					Function templateFunction = fac.getUriTemplate(fac.getVariable("x"));
					Pattern matcher = Pattern.compile("(.+)");
					matchers.put(matcher, templateFunction);
					templateStrings.add("(.+)");
				} else {
					ValueConstant template = (ValueConstant) fun.getTerms().get(0);
					String templateString = template.getValue();
					templateString = templateString.replace("{}", "(.+)");

					if (templateStrings.contains(templateString)) {
						continue;
					}
					Pattern matcher = Pattern.compile(templateString);
					matchers.put(matcher, fun);
					templateStrings.add(templateString);
				}
			}
		}
		this.uriTemplateMatcher = new UriTemplateMatcher(matchers);
	}


	/**
	 * Has side-effects! Dangerous for concurrency!
	 *
	 * TODO: isolate it if this feature is really needed
	 */
	public void updateSemanticIndexMappings(List<OBDAMappingAxiom> mappings, TBoxReasoner reformulationReasoner) throws OBDAException {

		unfoldingProgram = analyzer.constructDatalogProgram(mappings);

		applyTMappings(reformulationReasoner, false);

		setupUnfolder();

		log.debug("Mappings and unfolder have been updated after inserts to the semantic index DB");
	}


	/***
	 * Creates mappings with heads as "triple(x,y,z)" from mappings with binary
	 * and unary atoms"
	 *
	 * @return
	 */
	private List<CQIE> generateTripleMappings() {
		List<CQIE> newmappings = new LinkedList<CQIE>();

		for (CQIE mapping : unfoldingProgram) {
			Function newhead = null;
			Function currenthead = mapping.getHead();
			Predicate pred = OBDAVocabulary.QUEST_TRIPLE_PRED;
			LinkedList<Term> terms = new LinkedList<Term>();
			if (currenthead.getArity() == 1) {
				/*
				 * head is Class(x) Forming head as triple(x,uri(rdf:type),
				 * uri(Class))
				 */
				terms.add(currenthead.getTerm(0));
				Function rdfTypeConstant = fac.getUriTemplate(fac.getConstantLiteral(OBDAVocabulary.RDF_TYPE));
				terms.add(rdfTypeConstant);

				String classname = currenthead.getFunctionSymbol().getName();
				terms.add(fac.getUriTemplate(fac.getConstantLiteral(classname)));
				newhead = fac.getFunction(pred, terms);

			} else if (currenthead.getArity() == 2) {
				/*
				 * head is Property(x,y) Forming head as triple(x,uri(Property),
				 * y)
				 */
				terms.add(currenthead.getTerm(0));

				String propname = currenthead.getFunctionSymbol().getName();
				Function propconstant = fac.getUriTemplate(fac.getConstantLiteral(propname));
				terms.add(propconstant);
				terms.add(currenthead.getTerm(1));
				newhead = fac.getFunction(pred, terms);
			}
			CQIE newmapping = fac.getCQIE(newhead, mapping.getBody());
			newmappings.add(newmapping);
		}
		return newmappings;
	}

	public UriTemplateMatcher getUriTemplateMatcher() {
		return uriTemplateMatcher;
	}

	public DatalogProgram unfold(DatalogProgram query, String targetPredicate) throws OBDAException {
		return unfolder.unfold(query, targetPredicate);
	}


	public UnfoldingMechanism getDatalogUnfolder(){
		return unfolder;
	}
}
