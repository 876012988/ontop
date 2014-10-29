package it.unibz.krdb.obda.owlrefplatform.core.basicoperations;

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

import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.Term;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.URIConstant;
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.Variable;
import it.unibz.krdb.obda.model.impl.AnonymousVariable;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.VariableImpl;
import it.unibz.krdb.obda.ontology.DataType;
import it.unibz.krdb.obda.ontology.Description;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.Property;
import it.unibz.krdb.obda.ontology.PropertySomeDataTypeRestriction;
import it.unibz.krdb.obda.ontology.PropertySomeRestriction;
import it.unibz.krdb.obda.ontology.SubDescriptionAxiom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * A class that allows you to perform different operations related to query
 * containment on conjunctive queries.
 * 
 * @author Mariano Rodriguez Muro
 * 
 */
public class CQCUtilities {

	private CQIE canonicalQuery = null;

	private static Unifier unifier = new Unifier();

	private static QueryAnonymizer anonymizer = new QueryAnonymizer();

	private static OBDADataFactory termFactory = OBDADataFactoryImpl.getInstance();

	List<Function> canonicalbody = null;

	Function canonicalhead = null;

	Set<Predicate> canonicalpredicates = new HashSet<Predicate>(50);

	/***
	 * An index of all the facts obtained by freezing this query.
	 */
	private Map<Predicate, List<Function>> factMap = new HashMap<Predicate, List<Function>>();

	static Logger log = LoggerFactory.getLogger(CQCUtilities.class);

	private Ontology sigma = null;
	
	private List<CQIE> rules = null;

	final private OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

	/***
	 * Constructs a CQC utility using the given query. If Sigma is not null and
	 * not empty, then it will also be used to verify containment w.r.t.\ Sigma.
	 * 
	 * @param query
	 *            A conjunctive query
	 * @param sigma
	 *            A set of ABox dependencies
	 */
	public CQCUtilities(CQIE query, Ontology sigma) {
		this.sigma = sigma;
		if (sigma != null) {
			// log.debug("Using dependencies to chase the query");
			query = chaseQuery(query, sigma);
		}
		this.canonicalQuery = getCanonicalQuery(query);
		canonicalbody = canonicalQuery.getBody();
		canonicalhead = canonicalQuery.getHead();
		factMap = new HashMap<Predicate, List<Function>>(canonicalbody.size() * 2);
		for (Function atom : canonicalbody) {
			Function fact = (Function) atom;
			if (!fact.isDataFunction())
				continue;
			
			Predicate predicate = fact.getPredicate();
			canonicalpredicates.add(predicate);
			List<Function> facts = factMap.get(predicate);
			if (facts == null) {
				facts = new LinkedList<Function>();
				factMap.put(predicate, facts);
			}
			facts.add(fact);
		}
	}

	public CQCUtilities(CQIE query, List<CQIE> rules) {
		for (Function b : query.getBody())
			if (b.isBooleanFunction())
				return;
		
		this.rules = rules;
		if (rules != null && !rules.isEmpty()) {
			factMap = new HashMap<Predicate, List<Function>>();
			List<Function> generatedFacts = chaseQuery(query, rules);
			
			// Map the facts
			for (Function fact : generatedFacts) {
				Predicate p = fact.getPredicate();
				canonicalpredicates.add(p);
				List<Function> facts = factMap.get(p);
				if (facts == null) {
					facts = new LinkedList<Function>();
					factMap.put(p, facts);
				}
				facts.add(fact);
			}
		}
	}

	/***
	 * This method will "chase" a query with respect to a set of ABox
	 * dependencies. This will introduce atoms that are implied by the presence
	 * of other atoms. This operation may introduce an infinite number of new
	 * atoms, since there might be A ISA exists R and exists inv(R) ISA A
	 * dependencies. To avoid infinite cyles we will chase up to a certain depth
	 * only.
	 * 
	 * To improve performance, sigma should already be saturated.
	 * 
	 * @param query
	 * @param sigma
	 * @return
	 */
	public CQIE chaseQuery(CQIE query, Ontology sigma) {
		sigma.saturate();
		Function head = (Function) query.getHead();

		LinkedHashSet<Function> body = new LinkedHashSet<Function>();
		body.addAll(query.getBody());

		LinkedHashSet<Function> newbody = new LinkedHashSet<Function>();
		newbody.addAll(query.getBody());

		boolean loop = true;
		while (loop) {
			loop = false;
			for (Function atom : body) {
				if (atom == null) {
					continue;
				}

				Function patom = (Function) atom;
				Predicate predicate = atom.getPredicate();

				Term oldTerm1 = null;
				Term oldTerm2 = null;

				Set<SubDescriptionAxiom> pis = sigma.getByIncluded(predicate);
				if (pis == null) {
					continue;
				}
				for (SubDescriptionAxiom pi : pis) {

					Description left = pi.getSub();
					if (left instanceof Property) {

						if (patom.getArity() != 2)
							continue;

						Property lefttRoleDescription = (Property) left;

						if (lefttRoleDescription.isInverse()) {
							oldTerm1 = patom.getTerm(1);
							oldTerm2 = patom.getTerm(0);
						} else {
							oldTerm1 = patom.getTerm(0);
							oldTerm2 = patom.getTerm(1);
						}
					} else if (left instanceof OClass) {

						if (patom.getArity() != 1)
							continue;

						oldTerm1 = patom.getTerm(0);

					} else if (left instanceof PropertySomeRestriction) {
						if (patom.getArity() != 2)
							continue;

						PropertySomeRestriction lefttAtomicRole = (PropertySomeRestriction) left;
						if (lefttAtomicRole.isInverse()) {
							oldTerm1 = patom.getTerm(1);
						} else {
							oldTerm1 = patom.getTerm(0);
						}

					} else {
						throw new RuntimeException("ERROR: Unsupported dependnecy: " + pi.toString());
					}

					Description right = pi.getSuper();

					Term newTerm1 = null;
					Term newTerm2 = null;
					Predicate newPredicate = null;
					Function newAtom = null;

					if (right instanceof Property) {
						Property rightRoleDescription = (Property) right;
						newPredicate = rightRoleDescription.getPredicate();
						if (rightRoleDescription.isInverse()) {
							newTerm1 = oldTerm2;
							newTerm2 = oldTerm1;
						} else {
							newTerm1 = oldTerm1;
							newTerm2 = oldTerm2;
						}
						newAtom = fac.getFunction(newPredicate, newTerm1, newTerm2);
					} else if (right instanceof OClass) {
						OClass rightAtomicConcept = (OClass) right;
						newTerm1 = oldTerm1;
						newPredicate = rightAtomicConcept.getPredicate();
						newAtom = fac.getFunction(newPredicate, newTerm1);

					} else if (right instanceof PropertySomeRestriction) {
						// Here we need to introduce new variables, for the
						// moment
						// we only do it w.r.t.\ non-anonymous variables.
						// hence we are incomplete in containment detection.

						PropertySomeRestriction rightExistential = (PropertySomeRestriction) right;
						newPredicate = rightExistential.getPredicate();
						if (rightExistential.isInverse()) {
							if (newTerm2 instanceof AnonymousVariable)
								continue;
							newTerm1 = fac.getVariableNondistinguished();
							newTerm2 = oldTerm1;
							newAtom = fac.getFunction(newPredicate, newTerm1, newTerm2);
						} else {
							if (newTerm1 instanceof AnonymousVariable)
								continue;
							newTerm1 = oldTerm1;
							newTerm2 = fac.getVariableNondistinguished();
							newAtom = fac.getFunction(newPredicate, newTerm1, newTerm2);
						}
					} else if (right instanceof DataType) {
						// Does nothing
					} else if (right instanceof PropertySomeDataTypeRestriction) {
						PropertySomeDataTypeRestriction rightExistential = (PropertySomeDataTypeRestriction) right;
						newPredicate = rightExistential.getPredicate();
						if (rightExistential.isInverse()) {
							throw new RuntimeException("ERROR: The data property of some restriction should not have an inverse!");
						} else {
							if (newTerm1 instanceof AnonymousVariable)
								continue;
							newTerm1 = oldTerm1;
							newTerm2 = fac.getVariableNondistinguished();
							newAtom = fac.getFunction(newPredicate, newTerm1, newTerm2);
						}
					}

					else {
						throw new RuntimeException("ERROR: Unsupported dependency: " + pi.toString());
					}

					if (!newbody.contains(newAtom)) {
						newbody.add(newAtom);
					}

				}
			}
			if (body.size() != newbody.size()) {
				loop = true;
				body = newbody;
				newbody = new LinkedHashSet<Function>();
				newbody.addAll(body);
			}
		}

		LinkedList<Function> bodylist = new LinkedList<Function>();
		bodylist.addAll(body);
		CQIE newquery = fac.getCQIE(head, bodylist);
		newquery.setQueryModifiers(query.getQueryModifiers());

		return newquery;
	}

	/**
	 * This method is used to chase foreign key constraint rule in which the rule
	 * has only one atom in the body.
	 * 
	 * @param query
	 * @param rules
	 * @return
	 */
	public List<Function> chaseQuery(CQIE query, List<CQIE> rules) {
		List<Function> facts = new ArrayList<Function>();

		CQIE canonicalQuery = getCanonicalQuery(query);
		canonicalhead = canonicalQuery.getHead();
		canonicalbody = canonicalQuery.getBody();
		
		for (Function fact : canonicalbody) {
			facts.add(fact);
			for (CQIE rule : rules) {
				Function ruleBody = rule.getBody().get(0);
				Map<Variable, Term> theta = Unifier.getMGU(ruleBody, fact);
				if (theta != null && !theta.isEmpty()) {
					Function ruleHead = rule.getHead();
					Function newFact = (Function)ruleHead.clone();
					Unifier.applyUnifierToGetFact(newFact, theta);
					facts.add(newFact);
				}
			}
		}
		return facts;
	}
	
	/***
	 * Computes a query in which all variables have been replaced by
	 * ValueConstants that have the no type and have the same 'name' as the
	 * original variable.
	 * 
	 * This new query can be used for query containment checking.
	 * 
	 * @param q
	 */
	public CQIE getCanonicalQuery(CQIE q) {
		CQIE canonicalquery = q.clone();

		int constantcounter = 1;

		Map<Variable, Term> substitution = new HashMap<Variable, Term>(50);
		Function head = canonicalquery.getHead();
		constantcounter = getCanonicalAtom(head, constantcounter, substitution);

		for (Function atom : canonicalquery.getBody()) {
			constantcounter = getCanonicalAtom(atom, constantcounter, substitution);
		}
		return canonicalquery;
	}

	/***
	 * Replaces each Variable inside the atom with a constant. It will return
	 * the counter updated + n, where n is the number of constants introduced by
	 * the method.
	 * 
	 * @param atom
	 * @param constantcounter
	 *            an initial counter for the naming of the new constants. This
	 *            is needed to provide a numbering to each of the new constants
	 * @return
	 */
	public static int getCanonicalAtom(Function atom, int constantcounter, Map<Variable, Term> currentMap) {
		List<Term> headterms = ((Function) atom).getTerms();
		for (int i = 0; i < headterms.size(); i++) {
			Term term = headterms.get(i);
			if (term instanceof Variable) {
				Term substitution = null;
				if (term instanceof Variable) {
					substitution = currentMap.get(term);
					if (substitution == null) {
						ValueConstant newconstant = termFactory.getConstantLiteral("CAN" + ((Variable) term).getName() + constantcounter);
						constantcounter += 1;
						currentMap.put((Variable) term, newconstant);
						substitution = newconstant;
					}
				} else if (term instanceof ValueConstant) {
					ValueConstant newconstant = termFactory.getConstantLiteral("CAN" + ((ValueConstant) term).getValue() + constantcounter);
					constantcounter += 1;
					substitution = newconstant;
				} else if (term instanceof URIConstant) {
					ValueConstant newconstant = termFactory.getConstantLiteral("CAN" + ((URIConstant) term).getURI() + constantcounter);
					constantcounter += 1;
					substitution = newconstant;
				}
				headterms.set(i, substitution);
			} else if (term instanceof Function) {
				Function function = (Function) term;
				List<Term> functionterms = function.getTerms();
				for (int j = 0; j < functionterms.size(); j++) {
					Term fterm = functionterms.get(j);
					if (fterm instanceof Variable) {
						Term substitution = null;
						if (fterm instanceof VariableImpl) {
							substitution = currentMap.get(fterm);
							if (substitution == null) {
								ValueConstant newconstant = termFactory.getConstantLiteral("CAN" + ((VariableImpl) fterm).getName()
										+ constantcounter);
								constantcounter += 1;
								currentMap.put((Variable) fterm, newconstant);
								substitution = newconstant;

							}
						} else {
							ValueConstant newconstant = termFactory.getConstantLiteral("CAN" + ((ValueConstant) fterm).getValue()
									+ constantcounter);
							constantcounter += 1;
							substitution = newconstant;
						}
						functionterms.set(j, substitution);
					}
				}
			}
		}
		return constantcounter;
	}

	/***
	 * True if the query used to construct this CQCUtilities object is is
	 * contained in 'query'.
	 * 
	 * @param query
	 * @return
	 */
	public boolean isContainedIn(CQIE query) {
//		CQIE duplicate1 = query.clone();

		if (!query.getHead().getFunctionSymbol().equals(canonicalhead.getFunctionSymbol()))
			return false;

        List<Function> body = query.getBody();
        if(body.isEmpty()){
            return false;
        }
        for (Function queryatom : query.getBody()) {
			if (!canonicalpredicates.contains(((Function) queryatom).getFunctionSymbol())) {
				return false;
			}
		}

		return hasAnswer(query);
	}

	/***
	 * Returns true if the query can be answered using the ground atoms in the
	 * body of our canonical query. The method is recursive, hence expensive.
	 * 
	 * @param query
	 * @return
	 */
	private boolean hasAnswerRecursive(CQIE query) {

		List<Function> body = query.getBody();

		int atomidx = -1;
		for (Function currentAtomTry : body) {
			atomidx += 1;
			List<Function> relevantFacts = factMap.get(currentAtomTry.getPredicate());
			if (relevantFacts == null)
				return false;

			for (Function currentGroundAtom : relevantFacts) {

				Map<Variable, Term> mgu = unifier.getMGU(currentAtomTry, currentGroundAtom);
				if (mgu == null)
					continue;

				CQIE satisfiedquery = unifier.applyUnifier(query, mgu);
				satisfiedquery.getBody().remove(atomidx);

				if (satisfiedquery.getBody().size() == 0)
					if (canonicalhead.equals(satisfiedquery.getHead()))
						return true;
				/*
				 * Stopping early if we have chosen an MGU that has no
				 * possibility of being successful because of the head.
				 */
				if (unifier.getMGU(canonicalhead, satisfiedquery.getHead()) == null) {
					continue;
				}
				if (hasAnswerRecursive(satisfiedquery))
					return true;

			}
		}
		return false;
	}

	/***
	 * Implements a stack based, non-recursive query answering mechanism.
	 * 
	 * @param query2
	 * @return
	 */
	private boolean hasAnswerNonRecursive1(CQIE query2) {

		query2 = QueryAnonymizer.deAnonymize(query2);

		LinkedList<CQIE> querystack = new LinkedList<CQIE>();
		querystack.addLast(query2);
		LinkedList<Integer> lastAttemptIndexStack = new LinkedList<Integer>();
		lastAttemptIndexStack.addLast(0);

		while (!querystack.isEmpty()) {

			CQIE currentquery = querystack.pop();
			List<Function> body = currentquery.getBody();
			int atomidx = lastAttemptIndexStack.pop();
			Function currentAtomTry = body.get(atomidx);

			for (int groundatomidx = 0; groundatomidx < canonicalbody.size(); groundatomidx++) {
				Function currentGroundAtom = (Function) canonicalbody.get(groundatomidx);

				Map<Variable, Term> mgu = unifier.getMGU(currentAtomTry, currentGroundAtom);
				if (mgu != null) {
					CQIE satisfiedquery = unifier.applyUnifier(currentquery.clone(), mgu);
					satisfiedquery.getBody().remove(atomidx);

					if (satisfiedquery.getBody().size() == 0) {
						if (canonicalhead.equals(satisfiedquery.getHead())) {
							return true;
						}
					}

				}
			}

		}

		return false;
	}

    /**
     * TODO!!!
     *
     * @param query
     * @return
     */
	public boolean hasAnswer(CQIE query) {
		query = query.clone();
		QueryAnonymizer.deAnonymize(query);


		int bodysize = query.getBody().size();
		int maxIdxReached = -1;
		Stack<CQIE> queryStack = new Stack<CQIE>();
		CQIE currentQuery = query;
		List<Function> currentBody = currentQuery.getBody();
		Function currentAtom = null;
		queryStack.push(null);

		HashMap<Integer, Stack<Function>> choicesMap = new HashMap<Integer, Stack<Function>>(bodysize * 2);

		if (currentBody.size() == 0)
			return true;

		int currentAtomIdx = 0;
		while (currentAtomIdx >= 0) {

			if (currentAtomIdx > maxIdxReached)
				maxIdxReached = currentAtomIdx;

			currentAtom = currentBody.get(currentAtomIdx);
			
				
			Predicate currentPredicate = currentAtom.getPredicate();

			/* Looking for options for this atom */
			Stack<Function> factChoices = choicesMap.get(currentAtomIdx);
			if (factChoices == null) {
				/*
				 * we have never reached this atom, setting up the initial list
				 * of choices from the original fact list.
				 */
				factChoices = new Stack<Function>();
				factChoices.addAll(factMap.get(currentPredicate));
				choicesMap.put(currentAtomIdx, factChoices);
			}

			boolean choiceMade = false;
			CQIE newquery = null;
			while (!factChoices.isEmpty()) {
				Map<Variable, Term> mgu = Unifier.getMGU(currentAtom, factChoices.pop());
				if (mgu == null) {
					/* No way to use the current fact */
					continue;
				}
				newquery = Unifier.applyUnifier(currentQuery, mgu);
				/*
				 * Stopping early if we have chosen an MGU that has no
				 * possibility of being successful because of the head.
				 */
				if (Unifier.getMGU(canonicalhead, newquery.getHead()) == null) {
					/*
					 * There is no chance to unifiy the two heads, hence this
					 * fact is not good.
					 */
					continue;
				}

				/*
				 * The current fact was applicable, no conflicts so far, we can
				 * advance to the next atom
				 */
				choiceMade = true;
				break;

			}
			if (!choiceMade) {
				/*
				 * Reseting choices state and backtracking and resetting the set
				 * of choices for the current position
				 */
				factChoices.addAll(factMap.get(currentPredicate));
				currentAtomIdx -= 1;
				if (currentAtomIdx == -1)
					break;
				currentQuery = queryStack.pop();
				currentBody = currentQuery.getBody();

			} else {

				if (currentAtomIdx == bodysize - 1) {
					/* we found a succesful set of facts */
					return true;
				}

				/* Advancing to the next index */
				queryStack.push(currentQuery);
				currentAtomIdx += 1;
				currentQuery = newquery;
				currentBody = currentQuery.getBody();

			}

		}

		return false;

	}

	/**
	 * Removes all atoms that are equalt (syntactically) and then all the atoms
	 * that are redundant due to CQC.
	 * 
	 * @param queries
	 * @throws Exception
	 */
	public static HashSet<CQIE> removeDuplicateAtoms(Collection<CQIE> queries) {
		HashSet<CQIE> newqueries = new HashSet<CQIE>(queries.size() * 2);
		for (CQIE cq : queries) {
			List<Function> body = cq.getBody();
			for (int i = 0; i < body.size(); i++) {
				Function currentAtom = (Function) body.get(i);
				for (int j = i + 1; j < body.size(); j++) {
					Function comparisonAtom = (Function) body.get(j);
					if (currentAtom.getPredicate().equals(comparisonAtom.getPredicate())) {
						if (currentAtom.equals(comparisonAtom)) {
							body.remove(j);
						}
					}
				}
			}
			newqueries.add(anonymizer.anonymize(removeRundantAtoms(cq)));
		}
		return newqueries;
	}

	/***
	 * Removes all atoms that are redundant w.r.t to query containment.This is
	 * done by going through all unifyiable atoms, attempting to unify them. If
	 * they unify with a MGU that is empty, then one of the atoms is redundant.
	 * 
	 * 
	 * @param q
	 * @throws Exception
	 */
	public static CQIE removeRundantAtoms(CQIE q) {
		CQIE result = q;
		for (int i = 0; i < result.getBody().size(); i++) {
			Function currentAtom = result.getBody().get(i);
			for (int j = i + 1; j < result.getBody().size(); j++) {
				Function nextAtom = result.getBody().get(j);
				Map<Variable, Term> map = Unifier.getMGU(currentAtom, nextAtom);
				if (map != null && map.isEmpty()) {
					result = Unifier.unify(result, i, j);
				}
			}

		}
		return result;
	}

	/***
	 * Removes queries that are contained syntactically, using the method
	 * isContainedInSyntactic(CQIE q1, CQIE 2). To make the process more
	 * efficient, we first sort the list of queries as to have longer queries
	 * first and shorter queries last.
	 * 
	 * Removal of queries is done in two main double scans. The first scan goes
	 * top-down/down-top, the second scan goes down-top/top-down
	 * 
	 * @param queries
	 */
	public static void removeContainedQueriesSyntacticSorter(List<CQIE> queries, boolean twopasses) {

		int initialsize = queries.size();
//		log.debug("Removing trivially redundant queries. Initial set size: {}:", initialsize);
		long startime = System.currentTimeMillis();

		Comparator<CQIE> lenghtComparator = new Comparator<CQIE>() {

			@Override
			public int compare(CQIE o1, CQIE o2) {
				return o2.getBody().size() - o1.getBody().size();
			}
		};

		Collections.sort(queries, lenghtComparator);
		// queries = new LinkedList<CQIE>(queries);
		//
		//
		// Iterator<CQIE> forwardIt = queries.iterator();
		// while (forwardIt.hasNext()) {
		// CQIE cq = forwardIt.next();
		// Iterator<CQIE> backwardIt =
		// ((LinkedList)queries).descendingIterator();
		// while (backwardIt.hasNext()) {
		// if (isContainedInSyntactic(cq, backwardIt.next())) {
		// forwardIt.remove();
		// break;
		// }
		// }
		// }

		for (int i = 0; i < queries.size(); i++) {
			for (int j = queries.size() - 1; j > i; j--) {
				if (isContainedInSyntactic(queries.get(i), queries.get(j))) {
//					log.debug("REMOVE: " + queries.get(i));
					queries.remove(i);
					i = -1;
					break;
				}
			}
		}

		if (twopasses) {
			for (int i = queries.size() - 1; i > 0; i--) {
				for (int j = 0; j < i; j++) {
					if (isContainedInSyntactic(queries.get(i), queries.get(j))) {
//						log.debug("REMOVE: " + queries.get(i));
						queries.remove(i);
						i = +1;
						break;
					}
				}
			}
		}

		int newsize = queries.size();
		int queriesremoved = initialsize - newsize;
		long endtime = System.currentTimeMillis();
		long time = (endtime - startime) / 1000;
//		log.debug("Done. Time elapse: {}s", time);
//		log.debug("Resulting size: {}   Queries removed: {}", newsize, queriesremoved);

	}

	/***
	 * Check if query cq1 is contained in cq2, sintactically. That is, if the
	 * head of cq1 and cq2 are equal according to toString().equals and each
	 * atom in cq2 is also in the body of cq1 (also by means of
	 * toString().equals().
	 * 
	 * @param cq1
	 * @param cq2
	 * @return
	 */
	public static boolean isContainedInSyntactic(CQIE cq1, CQIE cq2) {
		if (!cq2.getHead().equals(cq1.getHead())) {
			return false;
		}

		// HashSet<Function> body1 = new HashSet<Function>(cq1.getBody().size());
		// body1.addAll(cq1.getBody());

		for (Function atom : cq2.getBody()) {
			// if (!body1.contains(atom))
			// return false;
			if (!cq1.getBody().contains(atom))
				return false;
		}
		return true;
	}

	public static DatalogProgram removeContainedQueriesSorted(DatalogProgram program, boolean twopasses) {
		DatalogProgram result = OBDADataFactoryImpl.getInstance().getDatalogProgram();
		result.setQueryModifiers(program.getQueryModifiers());
		List<CQIE> rules = new LinkedList<CQIE>();
		rules.addAll(program.getRules());
		rules = removeContainedQueries(rules, twopasses, null, null, true);
		result.appendRule(rules);
		return result;
	}

	public static DatalogProgram removeContainedQueriesSorted(DatalogProgram program, boolean twopasses, Ontology sigma) {
		DatalogProgram result = OBDADataFactoryImpl.getInstance().getDatalogProgram();
		result.setQueryModifiers(program.getQueryModifiers());
		List<CQIE> rules = removeContainedQueriesSorted(program.getRules(), twopasses, sigma);
		result.appendRule(rules);
		return result; 
	}
	
	public static DatalogProgram removeContainedQueriesSorted(DatalogProgram program, boolean twopasses, List<CQIE> foreignKeyRules) {
		DatalogProgram result = OBDADataFactoryImpl.getInstance().getDatalogProgram();
		result.setQueryModifiers(program.getQueryModifiers());
		List<CQIE> rules = new LinkedList<CQIE>();
		rules.addAll(program.getRules());
		rules = removeContainedQueriesSorted(rules, twopasses, foreignKeyRules);
		result.appendRule(rules);
		return result;
	}

	/***
	 * Removes queries that are contained syntactically, using the method
	 * isContainedInSyntactic(CQIE q1, CQIE 2). To make the process more
	 * efficient, we first sort the list of queries as to have longer queries
	 * first and shorter queries last.
	 * 
	 * Removal of queries is done in two main double scans. The first scan goes
	 * top-down/down-top, the second scan goes down-top/top-down
	 * 
	 * @param queries
	 */
	public static List<CQIE> removeContainedQueriesSorted(List<CQIE> queries, boolean twopasses, List<CQIE> rules) {
		return removeContainedQueries(queries, twopasses, null, rules, true);
	}
	
	/***
	 * Removes queries that are contained syntactically, using the method
	 * isContainedInSyntactic(CQIE q1, CQIE 2). To make the process more
	 * efficient, we first sort the list of queries as to have longer queries
	 * first and shorter queries last.
	 * 
	 * Removal of queries is done in two main double scans. The first scan goes
	 * top-down/down-top, the second scan goes down-top/top-down
	 * 
	 * @param queries
	 */
	public static List<CQIE> removeContainedQueriesSorted(List<CQIE> queries, boolean twopasses, Ontology sigma) {
		return removeContainedQueries(queries, twopasses, sigma, null, true);
	}

	public static List<CQIE> removeContainedQueries(List<CQIE> queriesInput, boolean twopasses, List<CQIE> rules) {
		return removeContainedQueries(queriesInput, twopasses, null, rules, false);
	}
	
	public static List<CQIE> removeContainedQueries(List<CQIE> queriesInput, boolean twopasses, Ontology sigma) {
		return removeContainedQueries(queriesInput, twopasses, sigma, null, false);
	}
	
	/***
	 * Removes queries that are contained syntactically, using the method
	 * isContainedInSyntactic(CQIE q1, CQIE 2). To make the process more
	 * efficient, we first sort the list of queries as to have longer queries
	 * first and shorter queries last.
	 * 
	 * Removal of queries is done in two main double scans. The first scan goes
	 * top-down/down-top, the second scan goes down-top/top-down
	 * 
	 * @param queriesInput
	 */
	public static List<CQIE> removeContainedQueries(List<CQIE> queriesInput, boolean twopasses, Ontology sigma, List<CQIE> rules, boolean sort) {

		// queries = new LinkedList<CQIE>(queries);
		LinkedList<CQIE> queries = new LinkedList<CQIE>();
		queries.addAll(queriesInput);
		
		int initialsize = queries.size();
//		log.debug("Optimzing w.r.t. CQC. Initial size: {}:", initialsize);

		double startime = System.currentTimeMillis();

		Comparator<CQIE> lenghtComparator = new Comparator<CQIE>() {

			@Override
			public int compare(CQIE o1, CQIE o2) {
				return o2.getBody().size() - o1.getBody().size();
			}
		};

		if (sort) {
			// log.debug("Sorting...");
			Collections.sort(queries, lenghtComparator);
		}

		if (sigma != null) {
			for (int i = 0; i < queries.size(); i++) {
				CQIE query = queries.get(i);
				CQCUtilities cqc = new CQCUtilities(query, sigma);
				for (int j = queries.size() - 1; j > i; j--) {
					CQIE query2 = queries.get(j);
					if (cqc.isContainedIn(query2)) {
//						log.debug("REMOVE (SIGMA): " + queries.get(i));
						queries.remove(i);
						i -= 1;
						break;
					}
				}
			}
	
			if (twopasses) {
				for (int i = (queries.size() - 1); i >= 0; i--) {
					CQCUtilities cqc = new CQCUtilities(queries.get(i), sigma);
					for (int j = 0; j < i; j++) {
						if (cqc.isContainedIn(queries.get(j))) {
//							log.debug("REMOVE (SIGMA): " + queries.get(i));
							queries.remove(i);
							break;
						}
					}
				}
			}
		}
		
		if (rules != null && !rules.isEmpty()) {
			for (int i = 0; i < queries.size(); i++) {
				CQIE query = queries.get(i);
				CQCUtilities cqc = new CQCUtilities(query, rules);
				if (cqc.rules != null)
				for (int j = queries.size() - 1; j > i; j--) {
					CQIE query2 = queries.get(j);
					if (cqc.isContainedIn(query2)) {
//						log.debug("REMOVE (FK): " + queries.get(i));
						queries.remove(i);
						i -= 1;
						break;
					}
				}
			}

			if (twopasses) {
				for (int i = (queries.size() - 1); i >= 0; i--) {
					CQCUtilities cqc = new CQCUtilities(queries.get(i), rules);
					if (cqc.rules != null)
					for (int j = 0; j < i; j++) {
						if (cqc.isContainedIn(queries.get(j))) {
//							log.debug("REMOVE (FK): " + queries.get(i));
							queries.remove(i);
							break;
						}
					}
				}
			}
		}
		int newsize = queries.size();

		double endtime = System.currentTimeMillis();
		double time = (endtime - startime) / 1000;

//		log.debug("Resulting size: {}  Time elapsed: {}", newsize, time);
		
		return queries;
	}
}
