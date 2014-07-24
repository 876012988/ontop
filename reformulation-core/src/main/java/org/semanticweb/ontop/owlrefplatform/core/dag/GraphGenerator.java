package org.semanticweb.ontop.owlrefplatform.core.dag;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.ontop.ontology.Axiom;
import org.semanticweb.ontop.ontology.Ontology;
import org.semanticweb.ontop.ontology.Property;
import org.semanticweb.ontop.ontology.PropertySomeRestriction;
import org.semanticweb.ontop.ontology.impl.OntologyFactoryImpl;


/**
 * Helper class to dump isa relationships into .dot format for processing them
 * into png
 */
@Deprecated
public class GraphGenerator {

	static String isaClsFile = "isaClasses";
	static String isaRoleFile = "isaRolles";

	public static boolean debugInfoDump = false;

	public static void dumpISA(DAG isa) throws IOException {
		dumpISA(isa, "base");
	}

	public static void dumpISA(DAG isa, String prefix) throws IOException {

		FileWriter out = new FileWriter(new File(prefix + isaClsFile));

		out.write("digraph classISA {\n" + "graph [bgcolor=aliceblue];" + "  node [color=lightblue2, style=filled, shape=record];");

		for (DAGNode node : isa.getClasses()) {

			String style = "";
			if (node.getDescription() instanceof PropertySomeRestriction) {
				PropertySomeRestriction ec = (PropertySomeRestriction) node.getDescription();

				if (ec.isInverse()) {
					style = "style=\"filled\" fillcolor=\"darkgoldenrod3\" ";
				} else {
					style = "style=\"filled\" fillcolor=\"darkgoldenrod1\" ";
				}
			}
			out.write(processISANode(node, style));
		}
		out.write("}");
		out.close();

		out = new FileWriter(new File(prefix + isaRoleFile));
		out.write("digraph rolleISA {\n" + " graph [bgcolor=\"aliceblue\"];\n" + "  node [shape=\"record\"];\n");

		for (DAGNode node : isa.getRoles()) {
			String style = "";

			Property ec = (Property) node.getDescription();

			if (ec.isInverse()) {
				style = "style=\"filled\" fillcolor=\"darkgoldenrod3\"";
			} else {
				style = "style=\"filled\" fillcolor=\"darkgoldenrod1\"";
			}

			out.write(processISANode(node, style));

		}
		out.write("}");
		out.close();

		List<String> commands = new ArrayList<String>(2);
		commands.add("/usr/bin/dot");
		commands.add("-Tpng");
		commands.add("-o");
		commands.add(prefix + isaClsFile + ".png");
		commands.add(prefix + isaClsFile);

		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.redirectErrorStream(true);
		builder.directory(new File("./"));
		Process clsProcess = builder.start();

		commands = new ArrayList<String>(2);
		commands.add("/usr/bin/dot");
		commands.add("-Tpng");
		commands.add("-o");
		commands.add(prefix + isaRoleFile + ".png");
		commands.add(prefix + isaRoleFile);

		builder = new ProcessBuilder(commands);
		builder.redirectErrorStream(true);
		builder.directory(new File("./"));
		Process roleProcess = builder.start();

	}

	private static String processISANode(DAGNode node, String style) {
		StringBuilder out = new StringBuilder();
		String name = node.getDescription().toString();
		if (name.contains("#")) {
			name = node.getDescription().toString().split("#")[1];
		}
		int idx = node.getIndex();
		SemanticIndexRange range = node.getRange();

		String equiTable = "";
		for (DAGNode equiNode : node.getEquivalents()) {

			String equiname = equiNode.getDescription().toString();
			if (equiname.contains("#")) {
				equiname = equiname.split("#")[1];
			}
			equiTable += "| " + equiname;
		}
		String label = String.format("label=\" <n> %s %d %s %s \"", name, idx, range, equiTable);

		out.append(String.format("\"%s\" [%s %s] ;\n", name, label, style));

		for (DAGNode child : node.getChildren()) {
			String childName = child.getDescription().toString();
			if (childName.contains("#")) {
				childName = child.getDescription().toString().split("#")[1];
			}
			out.append(String.format("\"%s\":n -> \"%s\":n ;\n", name, childName));
		}

		return out.toString();
	}

	// public static void dumpMappings(List<RDBMSSIRepositoryManager.MappingKey>
	// mappings) {
	// String mappingFile = "mappings";
	// try {
	// FileWriter out = new FileWriter(new File(mappingFile));
	// for (RDBMSSIRepositoryManager.MappingKey map : mappings) {
	//
	//
	// String uri = map.uri.split("#")[1];
	// String project = map.projection;
	// SemanticIndexRange range = map.range;
	//
	// try {
	// out.write(String.format("%s %s %s \n", uri, project, range));
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// }
	// out.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// }

	public static void dumpReducedOnto(List<Axiom> reducedOnto) throws IOException {
		Ontology ontology = OntologyFactoryImpl.getInstance().createOntology("");
		ontology.addAssertions(reducedOnto);
		DAG reducedIsa = DAGConstructor.getISADAG(ontology);
		reducedIsa.index();

		dumpISA(reducedIsa, "reduced");

	}
}
