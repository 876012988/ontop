package it.unibz.inf.ontop.injection.impl;

import it.unibz.inf.ontop.injection.InvalidOntopConfigurationException;
import it.unibz.inf.ontop.injection.OntopModelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class OntopModelPropertiesImpl implements OntopModelProperties {

    private static final String DEFAULT_PROPERTIES_FILE = "default.properties";
    private static final Logger LOG = LoggerFactory.getLogger(OntopModelProperties.class);
    private final Properties properties;

    /**
     * Beware: immutable class!
     *
     * Recommended constructor.
     *
     * Changing the Properties object afterwards will not have any effect
     * on this OntopModelProperties object.
     */
    protected OntopModelPropertiesImpl(Properties userProperties) {
        /**
         * Loads default properties
         */
        properties = loadDefaultPropertiesFromFile(OntopModelProperties.class, DEFAULT_PROPERTIES_FILE);
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
     */
    public Object get(Object key) {
        return properties.get(key);
    }

    /**
     * Returns the boolean value of the given key.
     */
    @Override
    public Optional<Boolean> getBoolean(String key) {
        Object value = get(key);

        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Boolean) {
            return Optional.of((Boolean) value);
        }
        else if (value instanceof String) {
            return Optional.of(Boolean.parseBoolean((String)value));
        }
        else {
            throw new InvalidOntopConfigurationException("A boolean was expected: " + value);
        }
    }

    @Override
    public boolean getRequiredBoolean(String key) {
        return getBoolean(key)
                .orElseThrow(() -> new IllegalStateException(key + " is required but missing " +
                        "(must have a default value)"));
    }

    /**
     * Returns the integer value of the given key.
     */
    @Override
    public Optional<Integer> getInteger(String key) {
        String value = (String) get(key);
        return Optional.ofNullable(Integer.parseInt(value));
    }

    @Override
    public int getRequiredInteger(String key) {
        return getInteger(key)
                .orElseThrow(() -> new IllegalStateException(key + " is required but missing " +
                        "(must have a default value)"));
    }

    /**
     * Returns the string value of the given key.
     */
    @Override
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable((String) get(key));
    }

    @Override
    public String getRequiredProperty(String key) {
        return getProperty(key)
                .orElseThrow(() -> new IllegalStateException(key + " is required but missing " +
                        "(must have a default value)"));
    }

    @Override
    public boolean contains(Object key) {
        return properties.containsKey(key);
    }

    protected Properties copyProperties() {
        Properties p = new Properties();
        p.putAll(properties);
        return p;
    }


}
