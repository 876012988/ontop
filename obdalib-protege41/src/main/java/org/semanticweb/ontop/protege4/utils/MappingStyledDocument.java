package org.semanticweb.ontop.protege4.utils;

/*
 * #%L
 * ontop-protege4
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
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

import javax.swing.JTextPane;
import javax.swing.text.DefaultStyledDocument;

import org.semanticweb.ontop.io.TargetQueryVocabularyValidator;
import org.semanticweb.ontop.protege4.core.OBDAModelFacade;

public class MappingStyledDocument extends DefaultStyledDocument {

	private static final long serialVersionUID = -1541062682306964359L;

	public MappingStyledDocument(JTextPane parent, OBDAModelFacade apic, TargetQueryVocabularyValidator validator) {
		super();
	}
}
