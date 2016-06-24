package it.unibz.inf.ontop.injection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * General properties.
 *
 * Focuses on implementation class declaration
 * for the core module of Ontop.
 *
 * Immutable!
 *
 */
public class OBDAProperties {

    public static final String JDBC_URL = "JDBC_URL";
    public static final String DB_NAME = "DB_NAME";
    public static final String DB_USER = "DBUSER";
    public static final String DB_PASSWORD = "DBPASSWORD";
    public static final String JDBC_DRIVER = "JDBC_DRIVER";

    public static final String OBTAIN_FULL_METADATA = "OBTAIN_FULL_METADATA";

    public static final String DB_CONSTRAINTS = "DB_CONSTRAINTS";

    public static final String MAPPING_FILE_PATH = "MAPPING_FILE_PATH";
    /**
     * Expects a File object, not a String!
     */
    public static final String MAPPING_FILE_OBJECT = "MAPPING_FILE_OBJECT";

    /**
     * Expects a Reader object, not a String!
     */
    public static final String MAPPING_FILE_READER = "MAPPING_FILE_READER";

    /**
     * Expects a Model object, not a String!
     */
    public static final String MAPPING_FILE_MODEL = "MAPPING_FILE_MODEL";

    /**
     * Expects an OBDAModel object, not a string!
     * Not it is not the standard way to access the OBDA model
     */
    public static final String PREDEFINED_OBDA_MODEL = "PREDEFINED_OBDA_MODEL";

    public static final String DEFAULT_OBDA_PROPERTIES_FILE = "default_implementations.properties";
    private static Logger LOG = LoggerFactory.getLogger(OBDAProperties.class);
    private final Properties properties;

    /**
     * Beware: immutable class!
     *
     * --> Only default properties.
     */
    public OBDAProperties() {
        this(new Properties());
    }

    /**
     * Beware: immutable class!
     *
     * Recommended constructor.
     *
     * Changing the Properties object afterwards will not have any effect
     * on this OBDAProperties object.
     */
    public OBDAProperties(Properties userProperties) {
        /**
         * Loads default properties
         */
        properties = loadDefaultPropertiesFromFile(OBDAProperties.class, DEFAULT_OBDA_PROPERTIES_FILE);
        /**
         * Overloads the default properties.
         */
        properties.putAll(userProperties);
    }

    protected static Properties loadDefaultPropertiesFromFile(Class localClass, String fileName) {
        Properties properties = new Properties();
        InputStream in = localClass.getResourceAsStream(fileName);
        if (in == null)
            throw new RuntimeException("Configuration " + fileName + " not found.");

        try {

            properties.load(in);
        } catch (IOException e1) {
            LOG.error("Error reading default OBDA properties.");
            LOG.debug(e1.getMessage(), e1);
            throw new RuntimeException("Impossible to extract configuration from " + fileName);
        }
        return properties;
    }

    /**
     * Returns the value of the given key.
     *
     * Returns null if not available.
     */
    public Object get(Object key) {
        return properties.get(key);
    }

    /**
     * Returns the boolean value of the given key.
     *
     * Returns null if not available.
     */
    public boolean getBoolean(String key) {
        String value = (String) get(key);
        return Boolean.parseBoolean(value);
    }

    /**
     * Returns the integer value of the given key.
     *
     * Returns null if not available.
     */
    public int getInteger(String key) {
        String value = (String) get(key);
        return Integer.parseInt(value);
    }

    /**
     * Returns the string value of the given key.
     *
     * Returns null if not available.
     */
    public String getProperty(String key) {
        return (String) get(key);
    }

    public boolean contains(Object key) {
        return properties.contains(key);
    }

    protected Properties copyProperties() {
        Properties p = new Properties();
        p.putAll(properties);
        return p;
    }
}
