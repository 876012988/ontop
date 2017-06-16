package it.unibz.inf.ontop.protege.gui.action;

/*
 * #%L
 * ontop-protege
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

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.model.OBDADataSource;
import it.unibz.inf.ontop.model.impl.SQLPPMappingImpl;
import it.unibz.inf.ontop.model.impl.RDBMSourceParameterConstants;
import it.unibz.inf.ontop.owlapi.directmapping.DirectMappingEngine;
import it.unibz.inf.ontop.protege.core.OBDAModelManager;
import it.unibz.inf.ontop.protege.core.OBDAModel;
import it.unibz.inf.ontop.protege.utils.OBDAProgressListener;
import it.unibz.inf.ontop.protege.utils.OBDAProgressMonitor;
import org.protege.editor.core.ui.action.ProtegeAction;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLWorkspace;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class BootstrapAction extends ProtegeAction {

	private static final long serialVersionUID = 8671527155950905524L;
	
	private OWLEditorKit editorKit = null;
	private OWLWorkspace workspace;
	private OWLModelManager owlManager;
	private OBDAModelManager modelManager;
	private String baseUri = "";
	private OWLOntology currentOnto;
	private OBDAModel currentModel;
	private OBDADataSource currentSource;

	private Logger log = LoggerFactory.getLogger(BootstrapAction.class);

	@Override
	public void initialise() throws Exception {
		editorKit = (OWLEditorKit) getEditorKit();
		workspace = editorKit.getWorkspace();
		owlManager = editorKit.getOWLModelManager();
		modelManager = ((OBDAModelManager) editorKit.get(SQLPPMappingImpl.class
				.getName()));
	}

	@Override
	public void dispose() throws Exception {
		// NOP
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		currentOnto = owlManager.getActiveOntology();
		currentModel = modelManager.getActiveOBDAModel();

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		JLabel dsource = new JLabel("Choose a datasource to bootstrap: ");
		dsource.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(dsource);
		List<String> options = new ArrayList<String>();
		for (OBDADataSource source : currentModel.getSources())
			options.add(source.getSourceID().toString());
		JComboBox combo = new JComboBox(options.toArray());
		combo.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(combo);
		Dimension minsize = new Dimension(10, 10);
		panel.add(new Box.Filler(minsize, minsize, minsize));
		JLabel ouri = new JLabel(
				"Base URI - the prefix to be used for all generated classes and properties: ");
		ouri.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(ouri);
		JTextField base_uri = new JTextField();
		base_uri.setText(currentModel.getPrefixManager().getDefaultPrefix()
				.replace("#", "/"));
		base_uri.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(base_uri);
		int res = JOptionPane.showOptionDialog(workspace, panel,
				"Bootstrapping", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (res == JOptionPane.OK_OPTION) {
			int index = combo.getSelectedIndex();
			currentSource = currentModel.getSources().get(index);
			if (currentSource != null) {
				this.baseUri = base_uri.getText().trim();
				if (baseUri.contains("#")) {
					JOptionPane.showMessageDialog(workspace,
							"Base Uri cannot contain the character '#'");
					throw new RuntimeException("Base URI " + baseUri
							+ " contains '#' character!");
				} else {
					Thread th = new Thread("Bootstrapper Action Thread"){
						@Override
						public void run() {
							try {
								OBDAProgressMonitor monitor = new OBDAProgressMonitor(
										"Bootstrapping ontology and mappings...");
								BootstrapperThread t = new BootstrapperThread();
								monitor.addProgressListener(t);
								monitor.start();
								t.run(baseUri, currentOnto, currentModel,
										currentSource);
								monitor.stop();
								JOptionPane.showMessageDialog(workspace,
										"Task is completed.", "Done",
										JOptionPane.INFORMATION_MESSAGE);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
								JOptionPane
										.showMessageDialog(null,
												"Error occured during bootstrapping data source.");
							}
						}
					};
					th.start();
				}
			}
		}
	}

	private class BootstrapperThread implements OBDAProgressListener {

		@Override
		public void actionCanceled() throws Exception {
			throw new Exception("Cancelling boostrapping is not implemented.");
		}

		public void run(String baseUri, OWLOntology currentOnto,
                        OBDAModel currentModel, OBDADataSource currentSource)
				throws Exception {

			String url = currentSource.getParameter(RDBMSourceParameterConstants.DATABASE_URL);
			String username = currentSource.getParameter(RDBMSourceParameterConstants.DATABASE_USERNAME);
			String password = currentSource.getParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD);
			String driver = currentSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);

			// TODO: Retrieve the effective properties (not just the default ones).
			OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
					.jdbcUrl(url)
					.jdbcUser(username)
					.jdbcPassword(password)
					.jdbcDriver(driver)
					.ppMapping(currentModel.getCurrentPPMapping())
					.ontology(currentOnto)
					.build();

			// Side-effect on the mapping and ontology objects
			DirectMappingEngine.bootstrap(configuration, baseUri);
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isErrorShown() {
			return false;
		}

	}

}
