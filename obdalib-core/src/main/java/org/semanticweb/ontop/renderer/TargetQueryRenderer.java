package org.semanticweb.ontop.renderer;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.semanticweb.ontop.io.PrefixManager;
import org.semanticweb.ontop.io.SimplePrefixManager;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.DataTypePredicate;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.OBDAQuery;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.Term;
import org.semanticweb.ontop.model.URIConstant;
import org.semanticweb.ontop.model.URITemplatePredicate;
import org.semanticweb.ontop.model.ValueConstant;
import org.semanticweb.ontop.model.Variable;
import org.semanticweb.ontop.model.impl.FunctionalTermImpl;
import org.semanticweb.ontop.model.impl.OBDAVocabulary;

/**
 * A utility class to render a Target Query object into its representational
 * string.
 */
public class TargetQueryRenderer {

	/**
	 * Transforms the given <code>OBDAQuery</code> into a string. The method requires
	 * a prefix manager to shorten full IRI name.
	 */
	public static String encode(OBDAQuery input, PrefixManager prefixManager) {
		if (!(input instanceof CQIE)) {
			return "";
		}
		TurtleWriter turtleWriter = new TurtleWriter();
		List<Function> body = ((CQIE) input).getBody();
		for (Function atom : body) {
			String subject, predicate, object = "";
			String originalString = atom.getFunctionSymbol().toString();
			if (isUnary(atom)) {
				Term subjectTerm = atom.getTerm(0);
				subject = getDisplayName(subjectTerm, prefixManager);
				predicate = "a";
				object = getAbbreviatedName(originalString, prefixManager, false);
				if (originalString.equals(object)) {
					object = "<" + object + ">";
				}
			}
			else if (originalString.equals("triple")) {
					Term subjectTerm = atom.getTerm(0);
					subject = getDisplayName(subjectTerm, prefixManager);
					Term predicateTerm = atom.getTerm(1);
					predicate = getDisplayName(predicateTerm, prefixManager);
					Term objectTerm = atom.getTerm(2);
					object = getDisplayName(objectTerm, prefixManager);
			}			
			else {
				Term subjectTerm = atom.getTerm(0);
				subject = getDisplayName(subjectTerm, prefixManager);
				predicate = getAbbreviatedName(originalString, prefixManager, false);
				if (originalString.equals(predicate)) {
					predicate = "<" + predicate + ">";
				}
				Term objectTerm = atom.getTerm(1);
				object = getDisplayName(objectTerm, prefixManager);
			}
			turtleWriter.put(subject, predicate, object);
		}
		return turtleWriter.print();
	}

	/**
	 * Checks if the atom is unary or not.
	 */
	private static boolean isUnary(Function atom) {
		return atom.getArity() == 1 ? true : false;
	}

	/**
	 * Prints the short form of the predicate (by omitting the complete URI and
	 * replacing it by a prefix name).
	 *
	 */
	private static String getAbbreviatedName(String uri, PrefixManager pm, boolean insideQuotes) {
		return pm.getShortForm(uri, insideQuotes);
	}

	/**
	 * Prints the text representation of different terms.
	 */
	private static String getDisplayName(Term term, PrefixManager prefixManager) {
		StringBuilder sb = new StringBuilder();
		if (term instanceof FunctionalTermImpl) {
			FunctionalTermImpl function = (FunctionalTermImpl) term;
			Predicate functionSymbol = function.getFunctionSymbol();
			String fname = getAbbreviatedName(functionSymbol.toString(), prefixManager, false);
			if (functionSymbol instanceof DataTypePredicate) {
				// if the function symbol is a data type predicate
				if (isLiteralDataType(functionSymbol)) {
					// if it is rdfs:Literal
					int arity = function.getArity();
					if (arity == 1) {
						// without the language tag
						Term var = function.getTerms().get(0);
						sb.append(getDisplayName(var, prefixManager));
						sb.append("^^rdfs:Literal");
					} else if (arity == 2) {
						// with the language tag
						Term var = function.getTerms().get(0);
						Term lang = function.getTerms().get(1);
						sb.append(getDisplayName(var, prefixManager));
						sb.append("@");
						if (lang instanceof ValueConstant) {
							// Don't pass this to getDisplayName() because 
							// language constant is not written as @"lang-tag"
							sb.append(((ValueConstant) lang).getValue());
						} else {
							sb.append(getDisplayName(lang, prefixManager));
						}
					}
				} else { // for the other data types
					Term var = function.getTerms().get(0);
					sb.append(getDisplayName(var, prefixManager));
					sb.append("^^");
					sb.append(fname);
				}
			} else if (functionSymbol instanceof URITemplatePredicate) {
				String template = ((ValueConstant) function.getTerms().get(0)).getValue();
				
				// Utilize the String.format() method so we replaced placeholders '{}' with '%s'
				String templateFormat = template.replace("{}", "%s");
				List<String> varNames = new ArrayList<String>();
				for (Term innerTerm : function.getTerms()) {
					if (innerTerm instanceof Variable) {
						varNames.add(getDisplayName(innerTerm, prefixManager));
					}
				}
				String originalUri = String.format(templateFormat, varNames.toArray());
				if(originalUri.equals(OBDAVocabulary.RDF_TYPE))
				{
					sb.append("a");
				}
				else{
				String shortenUri = getAbbreviatedName(originalUri, prefixManager, false); // shorten the URI if possible
				if (!shortenUri.equals(originalUri)) {
					sb.append(shortenUri);
				} else {
					// If the URI can't be shorten then use the full URI within brackets
					sb.append("<");
					sb.append(originalUri);
					sb.append(">");
				}		
				}
			} else { // for any ordinary function symbol
				sb.append(fname);
				sb.append("(");
				boolean separator = false;
				for (Term innerTerm : function.getTerms()) {
					if (separator) {
						sb.append(", ");
					}
					sb.append(getDisplayName(innerTerm, prefixManager));
					separator = true;
				}
				sb.append(")");
			}
		} else if (term instanceof Variable) {
			sb.append("{");
			sb.append(((Variable) term).getName());
			sb.append("}");
		} else if (term instanceof URIConstant) {
			String originalUri = term.toString();
			
			String shortenUri = getAbbreviatedName(originalUri, prefixManager, false); // shorten the URI if possible
			if (!shortenUri.equals(originalUri)) {
				sb.append(shortenUri);
			} else {
				// If the URI can't be shorten then use the full URI within brackets
				sb.append("<");
				sb.append(originalUri);
				sb.append(">");
			}
		
		} else if (term instanceof ValueConstant) {
			sb.append("\"");
			sb.append(((ValueConstant) term).getValue());
			sb.append("\"");
		}
		return sb.toString();
	}

	private static boolean isLiteralDataType(Predicate predicate) {
		return predicate.equals(OBDAVocabulary.RDFS_LITERAL) || predicate.equals(OBDAVocabulary.RDFS_LITERAL_LANG);
	}

	private TargetQueryRenderer() {
		// Prevent initialization
	}
}
