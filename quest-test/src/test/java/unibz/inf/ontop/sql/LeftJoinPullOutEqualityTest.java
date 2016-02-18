package unibz.inf.ontop.sql;


import unibz.inf.ontop.model.OBDADataFactory;
import unibz.inf.ontop.model.OBDAModel;
import unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import unibz.inf.ontop.io.ModelIOManager;
import unibz.inf.ontop.model.OBDAException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unibz.inf.ontop.owlrefplatform.owlapi3.*;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * TODO: describe
 */
public class LeftJoinPullOutEqualityTest {

    Logger log = LoggerFactory.getLogger(this.getClass());

    final String owlFileName = "resources/pullOutEq/pullOutEq.ttl";
    final String obdaFileName = "resources/pullOutEq/pullOutEq.obda";

    private QuestOWL reasoner;
    private QuestOWLConnection conn;

    @Before
    public void setUp() throws Exception {

        // Loading the OWL file
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument((new File(owlFileName)));

        // Loading the OBDA data
        OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
        OBDAModel obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdaFileName);


        Properties p = new Properties();
        p.put(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.put(QuestPreferences.OBTAIN_FULL_METADATA, QuestConstants.FALSE);

        QuestPreferences preferences = new QuestPreferences(p);
        // Creating a new instance of the reasoner
        QuestOWLFactory factory = new QuestOWLFactory();
        factory.setOBDAController(obdaModel);
        factory.setPreferenceHolder(preferences);

        reasoner = factory.createReasoner(ontology, new SimpleConfiguration());
    }

    @After
    public void tearDown() throws Exception{
        conn.close();
        reasoner.dispose();
    }


    private void runQuery(String query, int expectedCount) throws OBDAException, OWLException {

        // Now we are ready for querying
        conn = reasoner.getConnection();

        QuestOWLStatement st = conn.createStatement();
        QuestOWLResultSet results = st.executeTuple(query);
        int count = 0;
        while (results.nextRow()) {
            count++;
        }
        assertEquals(expectedCount, count);
    }

    @Test
    public void testFlatLeftJoins() throws OBDAException, OWLException {
        runQuery("PREFIX : <http://example.com/vocab#>" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "SELECT ?p ?firstName ?lastName " +
                "WHERE { " +
                "    ?p :age \"33\"^^xsd:int . " +
                "    OPTIONAL { ?p :firstName ?firstName }" +
                "    OPTIONAL { ?p :lastName ?lastName }" +
                "}", 1);
    }

    @Test
    public void testNestedLeftJoins() throws OBDAException, OWLException {
        runQuery("PREFIX : <http://example.com/vocab#>" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "SELECT ?p ?firstName ?lastName " +
                "WHERE { " +
                "    ?p :age \"33\"^^xsd:int . " +
                "    OPTIONAL { ?p :firstName ?firstName " +
                "               OPTIONAL { ?p :lastName ?lastName }" +
                "    }" +
                "}", 1);
    }

    @Test
    public void testJoinAndFlatLeftJoins() throws OBDAException, OWLException {
        runQuery("PREFIX : <http://example.com/vocab#>" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "SELECT ?p ?firstName ?lastName " +
                "WHERE { " +
                "    ?p :gender ?g . " +
                "    ?p :age \"33\"^^xsd:int . " +
                "    FILTER (str(?g) = \"F\") " +
                "    OPTIONAL { ?p :firstName ?firstName }" +
                "    OPTIONAL { ?p :lastName ?lastName }" +
                "}", 1);
    }

    @Test
    public void testBasic() throws OBDAException, OWLException {
        runQuery("PREFIX : <http://example.com/vocab#>" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "SELECT ?p " +
                "WHERE { " +
                "    ?p :age \"33\"^^xsd:int . " +
                "}", 1);
    }
}
