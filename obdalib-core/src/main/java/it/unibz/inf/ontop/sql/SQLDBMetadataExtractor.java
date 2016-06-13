package it.unibz.inf.ontop.sql;


import net.sf.jsqlparser.JSQLParserException;
import it.unibz.inf.ontop.injection.OBDAProperties;
import it.unibz.inf.ontop.mapping.sql.SQLTableNameExtractor;
import it.unibz.inf.ontop.model.OBDADataSource;
import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.nativeql.DBConnectionWrapper;
import it.unibz.inf.ontop.nativeql.DBMetadataException;
import it.unibz.inf.ontop.nativeql.DBMetadataExtractor;
import it.unibz.inf.ontop.sql.api.RelationJSQL;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * DBMetadataExtractor for JDBC-enabled DBs.
 */
public class SQLDBMetadataExtractor implements DBMetadataExtractor {

    /**
     * If we have to parse the full metadata or just the table list in the mappings.
     */
    private final Boolean obtainFullMetadata;

    /**
     * This represents user-supplied constraints, i.e. primary
     * and foreign keys not present in the database metadata
     *
     * Can be useful for eliminating self-joins
     */
    private final ImplicitDBConstraints userConstraints;

    @Inject
    private SQLDBMetadataExtractor(OBDAProperties preferences, @Nullable ImplicitDBConstraints userConstraints) {
        this.obtainFullMetadata = preferences.getBoolean(OBDAProperties.OBTAIN_FULL_METADATA);
        this.userConstraints = userConstraints;
    }

    /**
     * Expects the DBConnectionWrapper to wrap a JDBC connection.
     */
    @Override
    public DBMetadata extract(OBDADataSource dataSource, OBDAModel obdaModel, DBConnectionWrapper dbConnection) throws DBMetadataException {
        boolean applyUserConstraints = (userConstraints != null);

        if (dbConnection == null) {
            throw new IllegalArgumentException("dbConnection is required by " + getClass().getCanonicalName());
        }

        Object abstractConnection = dbConnection.getConnection();

        if (!(abstractConnection instanceof Connection)) {
            throw new IllegalArgumentException("The connection must correspond to a java.sql.connection (" + getClass().getCanonicalName() + ")");
        }
        Connection connection = (Connection) abstractConnection;

        try {
            DBMetadata metadata;
            if (obtainFullMetadata) {
                 metadata = JDBCConnectionManager.getMetaData(connection);
            } else {
                // This is the NEW way of obtaining part of the metadata
                // (the schema.table names) by parsing the mappings

                // Parse mappings. Just to get the table names in use
                SQLTableNameExtractor mParser = new SQLTableNameExtractor(connection, obdaModel.getMappings(dataSource.getSourceID()));

                List<RelationJSQL> realTables = mParser.getRealTables();

                if (applyUserConstraints) {
                    // Add the tables referred to by user-supplied foreign keys
                    userConstraints.addReferredTables(realTables);
                }

                metadata = JDBCConnectionManager.getMetaData(connection, realTables);
            }

            //Adds keys from the text file
            if (userConstraints != null) {
                userConstraints.addConstraints(metadata);
            }

            return metadata;

        } catch (JSQLParserException e) {
            throw new DBMetadataException("Error obtaining the tables" + e);
        } catch (SQLException e) {
            throw new DBMetadataException("Error obtaining the Metadata" + e);
        }
    }
}
