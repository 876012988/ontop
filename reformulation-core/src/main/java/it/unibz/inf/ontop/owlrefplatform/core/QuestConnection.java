package it.unibz.inf.ontop.owlrefplatform.core;

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

import java.sql.Connection;
import java.sql.SQLException;

import it.unibz.inf.ontop.model.OBDAException;
import it.unibz.inf.ontop.injection.QuestCoreSettings;
import it.unibz.inf.ontop.reformulation.OBDAQueryProcessor;

/***
 * Quest connection is responsible for wrapping a JDBC connection to the data
 * source. It will translate calls to OBDAConnection into JDBC Connection calls
 * (in most cases directly).
 *
 * SQL-specific implementation (specific to the JDBCConnector)!
 *
 * TODO: rename it SQLQuestConnection
 *
 * @author mariano
 * 
 */
public class QuestConnection implements IQuestConnection {

	private final QuestCoreSettings questCoreSettings;
	private final OBDAQueryProcessor queryProcessor;
	private Connection conn;

	private final JDBCConnector jdbcConnector;
	private boolean isClosed;


	public QuestConnection(JDBCConnector jdbcConnector, OBDAQueryProcessor queryProcessor, Connection connection,
						   QuestCoreSettings questCoreSettings) {
		this.jdbcConnector = jdbcConnector;
		this.queryProcessor = queryProcessor;
		this.conn = connection;
		this.isClosed = false;
		this.questCoreSettings = questCoreSettings;
	}

	public Connection getSQLConnection() {
		return conn;
	}
	
	@Override
	public void close() throws OBDAException {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			//throw new OBDAException(e);
		}
	}

	/**
	 * For both the virtual and classic modes.
	 */
	@Override
	public IQuestStatement createStatement() throws OBDAException {
		/**
		 * If in the classic mode, creates a SIQuestStatement.
		 * Why? Because the insertData method is not implemented by SQLQuestStatement while
		 * it is by SISQLQuestStatementImpl.
		 */
//		if (questCoreSettings.getProperty(QuestCoreSettings.ABOX_MODE).equals(QuestConstants.CLASSIC)) {
//			return createSIStatement();
//		}

		/**
		 * Virtual mode.
		 */
		try {
			if (conn.isClosed()) {
				// Sometimes it gets dropped, reconnect
				conn = jdbcConnector.getSQLPoolConnection();
			}
			IQuestStatement st = new SQLQuestStatement(this.queryProcessor, this,
					conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
							java.sql.ResultSet.CONCUR_READ_ONLY));
			//st.setFetchSize(400);
			return st;

		} catch (SQLException e1) {
			OBDAException obdaException = new OBDAException(e1);
			throw obdaException;
		}
	}


//	/**
//	 * Only for the classic mode!
//	 */
//	@Override
//	public SIQuestStatement createSIStatement() throws OBDAException {
//		try {
//			if (conn.isClosed()) {
//				// Sometimes it gets dropped, reconnect
//				conn = jdbcConnector.getSQLPoolConnection();
//			}
//			SIQuestStatement st = new SISQLQuestStatementImpl(this.questinstance, this,
//					conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
//							java.sql.ResultSet.CONCUR_READ_ONLY));
//			//st.setFetchSize(400);
//			return st;
//
//		} catch (SQLException e1) {
//			OBDAException obdaException = new OBDAException(e1);
//			throw obdaException;
//		}
//	}
		
	@Override
	public void commit() throws OBDAException {
		try {
			conn.commit();
		} catch (Exception e) {
			OBDAException obdaException = new OBDAException(e.getMessage());
			obdaException.setStackTrace(e.getStackTrace());
			throw obdaException;
		}
	}

	@Override
	public void setAutoCommit(boolean autocommit) throws OBDAException {
		try {
			conn.setAutoCommit(autocommit);
		} catch (Exception e) {
			OBDAException obdaException = new OBDAException(e.getMessage());
			obdaException.setStackTrace(e.getStackTrace());
			throw obdaException;
		}

	}

	@Override
	public boolean getAutoCommit() throws OBDAException {
		try {
			return conn.getAutoCommit();
		} catch (Exception e) {
			OBDAException obdaException = new OBDAException(e.getMessage());
			obdaException.setStackTrace(e.getStackTrace());
			throw obdaException;
		}
	}

	@Override
	public boolean isClosed() throws OBDAException {
		try {
			isClosed = conn.isClosed();
			return isClosed;
		} catch (Exception e) {
			
			OBDAException obdaException = new OBDAException(e);
			throw obdaException;
		}
	}

	@Override
	public boolean isReadOnly() throws OBDAException {
		return true;
//		/**
//		 * Write is currently supported by the classic mode.
//		 */
//		if (!this.questinstance.getOptionalSemanticIndexRepository().isPresent())
//			return true;
//		try {
//			return conn.isReadOnly();
//		} catch (Exception e) {
//			OBDAException obdaException = new OBDAException(e.getMessage());
//			obdaException.setStackTrace(e.getStackTrace());
//			throw obdaException;
//		}
	}

	@Override
	public void rollBack() throws OBDAException {
		try {
			conn.rollback();
		} catch (Exception e) {
			OBDAException obdaException = new OBDAException(e.getMessage());
			obdaException.setStackTrace(e.getStackTrace());
			throw obdaException;
		}
	}

}
