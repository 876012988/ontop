package org.semanticweb.ontop.owlrefplatform.core;


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.semanticweb.ontop.injection.NativeQueryLanguageComponentFactory;
import org.semanticweb.ontop.mapping.MappingSplitter;
import org.semanticweb.ontop.model.impl.AtomPredicateImpl;
import org.semanticweb.ontop.owlrefplatform.core.mappingprocessing.TMappingExclusionConfig;
import org.semanticweb.ontop.model.*;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.model.impl.OBDAVocabulary;
import org.semanticweb.ontop.model.impl.TermUtils;
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
import org.semanticweb.ontop.model.DataSourceMetadata;
import org.semanticweb.ontop.utils.IMapping2DatalogConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import java.net.URI;

public class QuestUnfolder {

	private final NativeQueryLanguageComponentFactory nativeQLFactory;
	/* The active unfolding engine */
	private UnfoldingMechanism unfolder;
    
	/* As unfolding OBDAModel, but experimental */
	private List<CQIE> unfoldingProgram;

	/*
	 * These are pattern matchers that will help transforming the URI's in
	 * queries into Functions, used by the SPARQL translator.
	 */
	private UriTemplateMatcher uriTemplateMatcher = new UriTemplateMatcher();
	
	private static final Logger log = LoggerFactory.getLogger(QuestUnfolder.class);
	
	private static final OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

	private ImmutableMultimap<AtomPredicate, ImmutableList<Integer>> primaryKeys;

	/** Davide> Whether to exclude the user-supplied predicates from the
	 *          TMapping procedure (that is, the mapping assertions for 
	 *          those predicates should not be extended according to the 
	 *          TBox hierarchies
	 */
	//private boolean applyExcludeFromTMappings = false;
	public QuestUnfolder(ImmutableList<CQIE> mappingRules, NativeQueryLanguageComponentFactory nativeQLFactory) throws Exception{

		this.nativeQLFactory = nativeQLFactory;
		unfoldingProgram = new ArrayList<>(mappingRules);
	}


	public int getRulesSize() {
		return unfoldingProgram.size();
	}

	@Deprecated
	public List<CQIE> getRules() {
		return unfoldingProgram;
	}


    /**
     * Only in version 2. TODO: see if still relevant.
     */
    public Multimap<Predicate, Integer> processMultipleTemplatePredicates() {
        return unfolder.processMultipleTemplatePredicates(unfoldingProgram);

    }


    /**
	 * Setting up the unfolder and SQL generation
	 */

	public void setupUnfolder(DataSourceMetadata metadata) {
		
		// Collecting URI templates
		uriTemplateMatcher = createURITemplateMatcher(unfoldingProgram);

		// Adding "triple(x,y,z)" mappings for support of unbounded
		// predicates and variables as class names (implemented in the
		// sparql translator)
		unfoldingProgram.addAll(generateTripleMappings(unfoldingProgram));
		
		Multimap<Predicate, List<Integer>> pkeys = metadata.extractPKs(unfoldingProgram);

        log.debug("Final set of mappings: \n {}", Joiner.on("\n").join(unfoldingProgram));
//		for(CQIE rule : unfoldingProgram){
//			log.debug("{}", rule);
//		}

		unfolder = new DatalogUnfolder(unfoldingProgram, pkeys);

		primaryKeys = convertPrimaryKeys(pkeys);
	}

	private static ImmutableMultimap<AtomPredicate, ImmutableList<Integer>> convertPrimaryKeys(
			Multimap<Predicate, List<Integer>> pkeys) {
		Map<Predicate, AtomPredicate> predicateMap = new HashMap<>();

		ImmutableMultimap.Builder<AtomPredicate, ImmutableList<Integer>> multimapBuilder = ImmutableMultimap.builder();

		for(Map.Entry<Predicate, List<Integer>> entry : pkeys.entries()) {
			Predicate originalPredicate = entry.getKey();
			AtomPredicate atomPredicate;
			if (originalPredicate instanceof AtomPredicate) {
				atomPredicate = (AtomPredicate) originalPredicate;
			}
			else if (predicateMap.containsKey(originalPredicate)) {
				atomPredicate = predicateMap.get(originalPredicate);
			}
			else {
				atomPredicate = new AtomPredicateImpl(originalPredicate);
				predicateMap.put(originalPredicate, atomPredicate);
			}

			multimapBuilder.put(atomPredicate, ImmutableList.copyOf(entry.getValue()));
		}
		return multimapBuilder.build();
	}

	public void applyTMappings(TBoxReasoner reformulationReasoner, boolean full, DataSourceMetadata metadata,
							   DBConnector dbConnector, TMappingExclusionConfig excludeFromTMappings) throws OBDAException  {
		
		final long startTime = System.currentTimeMillis();

		// for eliminating redundancy from the unfolding program
		LinearInclusionDependencies foreignKeyRules = dbConnector.generateFKRules(metadata);
		CQContainmentCheckUnderLIDs foreignKeyCQC = new CQContainmentCheckUnderLIDs(foreignKeyRules);
		// Davide> Here now I put another TMappingProcessor taking
		//         also a list of Predicates as input, that represents
		//         what needs to be excluded from the T-Mappings
		//if( applyExcludeFromTMappings )
			unfoldingProgram = TMappingProcessor.getTMappings(unfoldingProgram, reformulationReasoner, full,  foreignKeyCQC, excludeFromTMappings);
		//else
		//	unfoldingProgram = TMappingProcessor.getTMappings(unfoldingProgram, reformulationReasoner, full);
		
		// Eliminating redundancy from the unfolding program
		// TODO: move the foreign-key optimisation inside t-mapping generation 
		//              -- at this point it has little effect
		
/*		
		int s0 = unfoldingProgram.size();
		Collections.sort(unfoldingProgram, CQCUtilities.ComparatorCQIE);
		CQCUtilities.removeContainedQueries(unfoldingProgram, foreignKeyCQC);		
		if (s0 != unfoldingProgram.size())
			System.err.println("CQC REMOVED: " + s0 + " - " + unfoldingProgram.size());
*/
		
		final long endTime = System.currentTimeMillis();
		log.debug("TMapping size: {}", unfoldingProgram.size());
		log.debug("TMapping processing time: {} ms", (endTime - startTime));
	}

	/***
	 * Adding data typing on the mapping axioms.
	 */
	
	public void extendTypesWithMetadata(TBoxReasoner tBoxReasoner, DataSourceMetadata metadata) throws OBDAException {
		if (metadata instanceof DBMetadata) {
			MappingDataTypeRepair typeRepair = new MappingDataTypeRepair((DBMetadata)metadata, nativeQLFactory);
			typeRepair.insertDataTyping(unfoldingProgram, tBoxReasoner);
		}
		/**
		 * TODO: refactor so as to support this case
		 */
		else {
			log.warn("data-type reparation not supported for not SQL DBMetadata");
		}
	}

	/***
	 * Adding NOT NULL conditions to the variables used in the head
	 * of all mappings to preserve SQL-RDF semantics
	 */
	
	public void addNOTNULLToMappings() {

		for (CQIE mapping : unfoldingProgram) {
			Set<Variable> headvars = new HashSet<>();
			TermUtils.addReferencedVariablesTo(headvars, mapping.getHead());
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
			// no blank nodes are supported here
			URIConstant c = (URIConstant)ca.getIndividual();
			Predicate p = ca.getConcept().getPredicate();
			Function head = fac.getFunction(p, 
							fac.getUriTemplate(fac.getConstantLiteral(c.getURI())));
			CQIE rule = fac.getCQIE(head, Collections.<Function> emptyList());
				
			unfoldingProgram.add(rule);
			count++;
		}
		log.debug("Appended {} class assertions from ontology as fact rules", count);
	}		
	
	public void addObjectPropertyAssertionsAsFacts(Iterable<ObjectPropertyAssertion> assertions) {
		
		int count = 0;
		for (ObjectPropertyAssertion pa : assertions) {
			// no blank nodes are supported here
			URIConstant s = (URIConstant)pa.getSubject();
			URIConstant o = (URIConstant)pa.getObject();
			Predicate p = pa.getProperty().getPredicate();
			Function head = fac.getFunction(p, 
							fac.getUriTemplate(fac.getConstantLiteral(s.getURI())), 
							fac.getUriTemplate(fac.getConstantLiteral(o.getURI())));
			CQIE rule = fac.getCQIE(head, Collections.<Function> emptyList());
				
			unfoldingProgram.add(rule);
			count++;
		}
		log.debug("Appended {} object property assertions as fact rules", count);
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
		

	
	
	private static UriTemplateMatcher createURITemplateMatcher(List<CQIE> unfoldingProgram) {

		HashSet<String> templateStrings = new HashSet<String>();
        ImmutableMap.Builder<Pattern, Function> matcherBuilder = ImmutableMap.builder();

		for (CQIE mapping : unfoldingProgram) { 
			
			Function head = mapping.getHead();

			 // Collecting URI templates and making pattern matchers for them.
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
					matcherBuilder.put(matcher, templateFunction);
					templateStrings.add("(.+)");
				} 
				else {
					ValueConstant template = (ValueConstant) fun.getTerms().get(0);
					String templateString = template.getValue();
                    templateString = templateString.replace("{}", "(.+)");

					if (templateStrings.contains(templateString)) {
						continue;
					}
					Pattern matcher = Pattern.compile(templateString);
					matcherBuilder.put(matcher, fun);
					templateStrings.add(templateString);
				}
			}
		}
		return new UriTemplateMatcher(matcherBuilder.build());
	}
	
	
	public void updateSemanticIndexMappings(List<OBDAMappingAxiom> mappings, TBoxReasoner reformulationReasoner,
											DBConnector dbConnector, DataSourceMetadata metadata) throws OBDAException {

		IMapping2DatalogConverter mapping2DatalogConverter = nativeQLFactory.create(metadata);
		unfoldingProgram = mapping2DatalogConverter.constructDatalogProgram(mappings);
		
		// this call is required to complete the T-mappings by rules taking account of 
		// existential quantifiers and inverse roles
		applyTMappings(reformulationReasoner, false, metadata, dbConnector, TMappingExclusionConfig.empty());
		
		setupUnfolder(metadata);

		log.debug("Mappings and unfolder have been updated after inserts to the semantic index DB");
	}

	
	/***
	 * Creates mappings with heads as "triple(x,y,z)" from mappings with binary
	 * and unary atoms"
	 *
	 * @return
	 */
	private static List<CQIE> generateTripleMappings(List<CQIE> unfoldingProgram) {
		List<CQIE> newmappings = new LinkedList<CQIE>();

		for (CQIE mapping : unfoldingProgram) {
			Function newhead = null;
			Function currenthead = mapping.getHead();
			if (currenthead.getArity() == 1) {
				/*
				 * head is Class(x) Forming head as triple(x,uri(rdf:type),
				 * uri(Class))
				 */
				Function rdfTypeConstant = fac.getUriTemplate(fac.getConstantLiteral(OBDAVocabulary.RDF_TYPE));

				String classname = currenthead.getFunctionSymbol().getName();
				Term classConstant = fac.getUriTemplate(fac.getConstantLiteral(classname));
				
				newhead = fac.getTripleAtom(currenthead.getTerm(0), rdfTypeConstant, classConstant);
			} 
			else if (currenthead.getArity() == 2) {
				/*
				 * head is Property(x,y) Forming head as triple(x,uri(Property),
				 * y)
				 */
				String propname = currenthead.getFunctionSymbol().getName();
				Function propConstant = fac.getUriTemplate(fac.getConstantLiteral(propname));
				
				newhead = fac.getTripleAtom(currenthead.getTerm(0), propConstant, currenthead.getTerm(1));
			}
			else {
				/*
				 * head is triple(x,uri(Property),y)
				 */
				newhead = (Function) currenthead.clone();
			}
			CQIE newmapping = fac.getCQIE(newhead, mapping.getBody());
			newmappings.add(newmapping);
		}
		return newmappings;
	}

	public UriTemplateMatcher getUriTemplateMatcher() {
		return uriTemplateMatcher;
	}

	public UnfoldingMechanism getDatalogUnfolder(){
		return unfolder;
	}

	public ImmutableMultimap<AtomPredicate, ImmutableList<Integer>> getPrimaryKeys() {
		return primaryKeys;
	}
}
