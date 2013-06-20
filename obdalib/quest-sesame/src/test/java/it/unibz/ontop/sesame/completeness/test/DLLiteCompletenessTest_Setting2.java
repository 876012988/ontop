package it.unibz.ontop.sesame.completeness.test;

import junit.framework.Test;

/**
 * Test the sound and completeness of Quest reasoner with respect to DL-Lite
 * semantic.
 * 
 * Setting: With Optimizing Equivalences and Without Using TBox Sigma. 
 */
public class DLLiteCompletenessTest_Setting2 extends CompletenessTest {

	public DLLiteCompletenessTest_Setting2(String tid, String name, String resf,
			String propf, String owlf, String sparqlf) throws Exception {
		super(tid, name, resf, propf, owlf, sparqlf);
	}

	public static Test suite() throws Exception {
		return CompletenessTestUtils.suite(new Factory() {
			@Override
			public CompletenessTest createCompletenessTest(String testId,
					String testName, String testResultPath,
					String testParameterPath, String testOntologyPath,
					String testQueryPath) throws Exception {
				return new DLLiteCompletenessTest_Setting2(testId, testName,
						testResultPath, testParameterPath, testOntologyPath,
						testQueryPath);
			}

			@Override
			public String getMainManifestFile() {
				return "/completeness/manifest-eq-nosig.ttl";
			}
		});
	}
}
