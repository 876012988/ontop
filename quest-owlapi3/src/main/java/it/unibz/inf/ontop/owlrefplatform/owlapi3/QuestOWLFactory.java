package it.unibz.inf.ontop.owlrefplatform.owlapi3;

/*
 * #%L
 * ontop-quest-owlapi3
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

import com.google.inject.Guice;
import com.google.inject.Injector;
import it.unibz.inf.ontop.exception.DuplicateMappingException;
import it.unibz.inf.ontop.exception.InvalidMappingException;
import it.unibz.inf.ontop.injection.NativeQueryLanguageComponentFactory;
import it.unibz.inf.ontop.injection.OBDACoreModule;
import it.unibz.inf.ontop.io.InvalidDataSourceException;
import it.unibz.inf.ontop.mapping.MappingParser;
import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import it.unibz.inf.ontop.owlrefplatform.injection.QuestComponentFactory;
import it.unibz.inf.ontop.owlrefplatform.injection.QuestComponentModule;


import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 *
 * Mutable (OWLAPI design and usage in Protege).
 *
 * Implementation of an OWLReasonerFactory that can create instances of Quest.
 * Note, to create an instance of Quest, QuestPreferences are required.
 * Also, if you use Quest in Virtual ABox mode you must set a link to the mapping file.
 * 
 * @see OBDAModel
 * 
 * @author Mariano Rodriguez Muro <mariano.muro@gmail.com>
 * Updated by Benjamin Cogrel
 * 
 */
public class QuestOWLFactory implements OWLReasonerFactory {

	private OBDAModel obdaModel;
	private QuestPreferences preferences;
    private QuestComponentFactory componentFactory;
    private File mappingFile;
	
	private String name = "Quest";

	private final Logger log = LoggerFactory.getLogger(QuestOWLFactory.class);


    /**
     * Virtual mode (because there is a mapping file)
     * TODO: further explain
     *
     * @param mappingFile
     * @param preferences
     */
    public QuestOWLFactory(File mappingFile, QuestPreferences preferences)
            throws IOException, InvalidMappingException, InvalidDataSourceException, DuplicateMappingException {
        init(mappingFile, preferences);
    }

    /**
     * Virtual mode (because there is readable mappings)
     * TODO: further explain
     */
    public QuestOWLFactory(QuestPreferences preferences, Reader mappingReader)
            throws IOException, InvalidMappingException, InvalidDataSourceException, DuplicateMappingException {
        mappingFile = null;
        init(mappingReader, preferences);
    }

    /**
     * This method is isolated from the constructor because it also used for reloading the preferences and the mappings.
     */
    private void init(Reader mappingReader, QuestPreferences preferences) throws DuplicateMappingException, InvalidMappingException,
            InvalidDataSourceException, IOException {
        Injector injector = Guice.createInjector(new OBDACoreModule(preferences), new QuestComponentModule(preferences));
        this.componentFactory = injector.getInstance(QuestComponentFactory.class);

        /**
         * OBDA model extraction (virtual mode)
         */
        if (mappingReader != null) {
            NativeQueryLanguageComponentFactory nativeQLFactory = injector.getInstance(NativeQueryLanguageComponentFactory.class);
            MappingParser mappingParser = nativeQLFactory.create(mappingReader);
            this.obdaModel = mappingParser.getOBDAModel();
        }
        else {
            this.obdaModel = null;
        }

        this.preferences = preferences;
        // Does not touch the mapping file.
    }

    /**
     * This method is isolated from the constructor because it also used for reloading the preferences and the mappings.
     */
    private void init(File mappingFile, QuestPreferences preferences) throws DuplicateMappingException, InvalidMappingException, InvalidDataSourceException, IOException {
        Injector injector = Guice.createInjector(new OBDACoreModule(preferences), new QuestComponentModule(preferences));
        this.componentFactory = injector.getInstance(QuestComponentFactory.class);

        /**
         * OBDA model extraction (virtual mode)
         */
        if (mappingFile != null) {
            NativeQueryLanguageComponentFactory nativeQLFactory = injector.getInstance(NativeQueryLanguageComponentFactory.class);
            MappingParser mappingParser = nativeQLFactory.create(mappingFile);
            this.obdaModel = mappingParser.getOBDAModel();
        }
        else {
            this.obdaModel = null;
        }

        this.preferences = preferences;
        this.mappingFile = mappingFile;
    }

    /**
     * Classic mode (no mapping)
     *TODO: further explain
     *
     * @param preferences
     */
    public QuestOWLFactory(QuestPreferences preferences) throws IOException, InvalidMappingException,
            InvalidDataSourceException, DuplicateMappingException {
        this(null, preferences);
    }


//	/***
//	 * Sets the mappings that will be used to create instances of Quest. If this
//	 * is not set, mappings will be null and Quest will be started in
//	 * "classic ABox" mode. If the mappings are not null, then the mode must be
//	 * "Virtual ABox" model.
//	 *
//	 * @param apic
//	 */
//    @Deprecated
//	public void setOBDAController(OBDAModel apic) {
//		this.mappingManager = apic;
//	}

//    @Deprecated
//	public void setPreferenceHolder(Properties preference) {
//		this.preferences = preference;
//	}

    /**
     * Re-parses the mapping file and changes the preferences.
     */
    public void reload(QuestPreferences newPreferences) throws DuplicateMappingException, InvalidMappingException,
            InvalidDataSourceException, IOException {
        init(mappingFile, newPreferences);
    }

    /**
     * Re-parses the mapping file and changes the preferences.
     */
    public void reload(File mappingFile, QuestPreferences newPreferences) throws DuplicateMappingException, InvalidMappingException,
            InvalidDataSourceException, IOException {
        init(mappingFile, newPreferences);
    }

    /**
     * Re-parses the readable mappings and changes the preferences.
     */
    public void reload(Reader mappingReader, QuestPreferences newPreferences) throws DuplicateMappingException, InvalidMappingException,
            InvalidDataSourceException, IOException {
        init(mappingReader, newPreferences);
    }



	@Override
	public String getReasonerName() {
		return name;
	}

	@Override
	public QuestOWL createNonBufferingReasoner(OWLOntology ontology) {
        checkAboxMode();
        try {
            return new QuestOWL(ontology, obdaModel, new SimpleConfiguration(), BufferingMode.NON_BUFFERING, preferences,
                        componentFactory);
        } catch (Exception e) {
            /**
             * Unfortunately this OWLAPI interface does not allow exception declaration.
             */
            throw new RuntimeException(e.getMessage());
        }
	}

    private void checkAboxMode() throws RuntimeException {
        if (obdaModel == null && preferences.get(QuestPreferences.ABOX_MODE).equals(QuestConstants.VIRTUAL)) {
            String errorMessage = "You didn't specified mappings, but they are required for using the 'Virtual ABox' mode. " +
            "If you want to work in 'classic ABox' mode', you have to set the ABox mode to: '"
            + QuestConstants.CLASSIC + "'";
            log.error(errorMessage);
            // TODO: find a better exception
            throw new RuntimeException(errorMessage);
        }
        else if (obdaModel != null
                && preferences.get(QuestPreferences.ABOX_MODE).equals(QuestConstants.CLASSIC)
                && (!preferences.get(QuestPreferences.OBTAIN_FROM_MAPPINGS).equals("true"))) {
            String errorMessage = "You have specified mappings in the 'Classic ABox' mode " +
                    "but you did not set the OBTAIN_FROM_MAPPINGS to true. They are thus useless. " +
            "If you want to work in 'Virtual ABox' mode', you have to set the ABox mode to: '"
            + QuestConstants.VIRTUAL + "'";
            log.error(errorMessage);
            // TODO: find a better exception
            throw new RuntimeException(errorMessage);
        }
    }

    @Override
	public QuestOWL createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config)
			throws IllegalConfigurationException {
        checkAboxMode();
        try {
            return new QuestOWL(ontology, obdaModel, config, BufferingMode.NON_BUFFERING, preferences, componentFactory);
        } catch (Exception e) {
            /**
             * Unfortunately this OWLAPI interface does not allow exception declaration.
             */
            throw new RuntimeException(e.getMessage());
        }
	}
	
	@Override
	public QuestOWL createReasoner(OWLOntology ontology) {
        checkAboxMode();
		return new QuestOWL(ontology, obdaModel, new SimpleConfiguration(), BufferingMode.BUFFERING, preferences,
                    componentFactory);
	}

	@Override
	public QuestOWL createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) throws IllegalConfigurationException {
		checkAboxMode();
		return new QuestOWL(ontology, obdaModel, config, BufferingMode.BUFFERING, preferences, componentFactory);
	}

}
