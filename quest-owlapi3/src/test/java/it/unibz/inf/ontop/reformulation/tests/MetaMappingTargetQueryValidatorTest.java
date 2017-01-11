package it.unibz.inf.ontop.reformulation.tests;

/*
 * #%L
 * ontop-quest-owlapi
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import it.unibz.inf.ontop.exception.InvalidMappingException;
import it.unibz.inf.ontop.injection.QuestConfiguration;
import it.unibz.inf.ontop.io.InvalidDataSourceException;
import junit.framework.TestCase;

import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.owlapi.OBDAModelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This test is adapted from SimpleMappingVirtualABoxTest.
 *
 * A simple test that check if the system is able to handle Mappings for
 * classes/roles and attributes even if there are no URI templates. i.e., the
 * database stores URI's directly.
 * 
 * We are going to create an H2 DB, the .sql file is fixed. We will map directly
 * there and then query on top.
 */
public class MetaMappingTargetQueryValidatorTest extends TestCase {


	private Connection conn;

	Logger log = LoggerFactory.getLogger(this.getClass());

	final String owlfile = "src/test/resources/test/metamapping.owl";
	final String obdafile = "src/test/resources/test/metamapping.obda";

	@Override
	public void setUp() throws Exception {
		
		
		/*
		 * Initializing and H2 database with the stock exchange data
		 */
		// String driver = "org.h2.Driver";
		String url = "jdbc:h2:mem:questjunitdb;DATABASE_TO_UPPER=FALSE";
		String username = "sa";
		String password = "";

		conn = DriverManager.getConnection(url, username, password);
		Statement st = conn.createStatement();

		FileReader reader = new FileReader("src/test/resources/test/metamapping-create-h2.sql");
		BufferedReader in = new BufferedReader(reader);
		StringBuilder bf = new StringBuilder();
		String line = in.readLine();
		while (line != null) {
			bf.append(line);
			line = in.readLine();
		}

		st.executeUpdate(bf.toString());
		conn.commit();
	}

	@Override
	public void tearDown() throws Exception {
	
			dropTables();
			conn.close();

	}

	private void dropTables() throws SQLException, IOException {

		Statement st = conn.createStatement();

		FileReader reader = new FileReader("src/test/resources/test/metamapping-drop-h2.sql");
		BufferedReader in = new BufferedReader(reader);
		StringBuilder bf = new StringBuilder();
		String line = in.readLine();
		while (line != null) {
			bf.append(line);
			line = in.readLine();
		}

		st.executeUpdate(bf.toString());
		st.close();
		conn.commit();
	}

	public void testValidate() throws InvalidDataSourceException, IOException, InvalidMappingException {

		QuestConfiguration configuration = QuestConfiguration.defaultBuilder()
				.nativeOntopMappingFile(obdafile)
				.build();

		OBDAModel obdaModel = configuration.loadProvidedMapping();

		// run validator
		try {
			OBDAModelValidator.validate(obdaModel);
		}
		catch (Exception e) {
			fail("The target query has problem:" + e.getMessage());
		}
		
	}



}
