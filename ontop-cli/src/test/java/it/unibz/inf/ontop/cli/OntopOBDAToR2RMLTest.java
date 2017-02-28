package it.unibz.inf.ontop.cli;

import org.junit.Test;

public class OntopOBDAToR2RMLTest {

    @Test
    public void testOntopHelp (){
        String[] argv = {"help", "mapping", "to-r2rml"};
        Ontop.main(argv);
    }

    @Test
    public void testOntopOBDAToR2RML (){
        String[] argv = {"mapping", "to-r2rml",
                "-i", "src/test/resources/books/exampleBooks.obda",
                "-t", "src/test/resources/mapping-northwind.owl"
        };
        Ontop.main(argv);
    }

    @Test
    public void testOntopOBDAToR2RML2 (){
        String[] argv = {"mapping", "to-r2rml",
                "-i", "src/test/resources/mapping-northwind.obda",
                "-t", "src/test/resources/mapping-northwind.owl",
                "-o", "src/test/resources/mapping-northwind.r2rml"
        };
        Ontop.main(argv);
    }

    @Test
    public void testOntopOBDAToR2RML3 (){
        String[] argv = {"mapping", "to-r2rml",
                "-i", "src/test/resources/booktutorial.obda",
                "-t", "src/test/resources/booktutorial.owl",
                "-o", "src/test/resources/booktutorial.r2rml"
        };
        Ontop.main(argv);
    }

    @Test
    public void testOntopOBDAToR2RML_NPD (){
        String[] argv = {"mapping", "to-r2rml",
                "-i", "src/test/resources/npd-v2-ql-mysql-ontop1.17.obda",
                "-o", "src/test/resources/npd-v2-ql-mysql-ontop1.17.ttl",
        };
        Ontop.main(argv);
    }



}
