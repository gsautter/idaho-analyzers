/*
 * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uka.ipd.idaho.plugins.docVariables;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import javax.swing.JDialog;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This Analyzer extracts global information that applies to an entire document
 * from sections like the introduction and makes it available as document
 * properties.
 * 
 * @author sautter
 */
public class DocVariableExtractor extends AbstractConfigurableAnalyzer {
	
	private static class DocVariable {
		String name;
		GPath[] paths;
		DocVariable(String name, GPath[] paths) {
			this.name = name;
			this.paths = paths;
		}
	}
	
	private GPath[] contextPaths = {};
	private DocVariable[] variables = {};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			StringVector lines = this.loadList("DocVariableDefinitions.cnfg");
			String variable = null;
			ArrayList paths = new ArrayList();
			HashSet pathSet = new HashSet();
			ArrayList variables = new ArrayList();
			for (int l = 0; l < lines.size(); l++) {
				String line = lines.get(l).trim();
				
				//	jump empty lines and comments
				if ((line.length() == 0) || line.startsWith("//"))
					continue;
				
				//	CONTEXT keyword or variable name
				if (line.startsWith("@")) {
					variable = line.substring(1).trim();
					if (variable.length() == 0)
						variable = null;
					else if (variable.indexOf('{') != -1)
						variable = variable.substring(0, variable.indexOf('{')).trim();
					paths.clear();
					pathSet.clear();
					continue;
				}
				
				//	end of definition
				if (line.startsWith("}")) {
					if ((variable != null) && (paths.size() != 0)) {
						if ("CONTEXT".equals(variable))
							this.contextPaths = ((GPath[]) paths.toArray(new GPath[paths.size()]));
						else variables.add(new DocVariable(variable, ((GPath[]) paths.toArray(new GPath[paths.size()]))));
					}
					variable = null;
					paths.clear();
					pathSet.clear();
					continue;
				}
				
				//	we're in a definition, store path
				if ((variable != null) && pathSet.add(line)) try {
					paths.add(new GPath(line));
				}
				catch (GPathException gpe) {
					System.out.println("DocVariableExtractor: invalid path: " + line);
				}
			}
			
			//	store variable definitions
			this.variables = ((DocVariable[]) variables.toArray(new DocVariable[variables.size()]));
		}
		catch (IOException ioe) {
			System.out.println("DocVariableExtractor: could not load variable definitions.");
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		//	TODO use analyzer config API
		return super.getConfigPanels(dialog);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	running this analyzer only makes sense if we have a document root
		DocumentRoot doc = ((data instanceof DocumentRoot) ? ((DocumentRoot) data) : null);
		if (doc == null)
			return;
		
		//	get context
		QueriableAnnotation[] contexts = {doc};
		if (this.contextPaths.length != 0) {
			ArrayList contextList = new ArrayList();
			HashSet contextIDs = new HashSet();
			for (int c = 0; c < this.contextPaths.length; c++) {
				QueriableAnnotation[] cas = this.contextPaths[c].evaluate(doc, null);
				for (int a = 0; a < cas.length; a++) {
					if (contextIDs.add(cas[a].getAnnotationID()))
						contextList.add(cas[a]);
				}
			}
			if (contextList.size() != 0)
				contexts = ((QueriableAnnotation[]) contextList.toArray(new QueriableAnnotation[contextList.size()]));
		}
		
		//	extract and store variables
		for (int v = 0; v < this.variables.length; v++) {
			
			//	first try more precise/specific paths on all contexts, then proceed to less specific fallback paths
			for (int p = 0; p < this.variables[v].paths.length; p++) {
				
				//	try all contexts
				for (int c = 0; c < contexts.length; c++) {
					Annotation[] res = this.variables[v].paths[p].evaluate(contexts[c], null);
					
					//	found the sought data, we're done here
					if (res.length != 0) {
						//	TODO in case of multiple results, consider concatenatenating values
						doc.setDocumentProperty(this.variables[v].name, res[0].getValue());
						c = contexts.length;
						p = this.variables[v].paths.length;
					}
				}
			}
		}
	}
	
	//	FOR TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		DocumentRoot dr = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/SMNK-Projekt/EcologyTestbed/Willmann1952_gg1.scopeTest.xml"), "UTF-8"));
		DocVariableExtractor dve = new DocVariableExtractor();
		dve.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/DocVariablesData/")));
		dve.process(dr, new Properties());
		String[] dpNames = dr.getDocumentPropertyNames();
		for (int p = 0; p < dpNames.length; p++)
			System.out.println(dpNames[p] + " = " + dr.getDocumentProperty(dpNames[p]));
	}
}
