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

package de.uka.ipd.idaho.plugins.materialsCitations;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class MaterialsCitationParserOnlineTest {
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
//		
//		String[] csss = {"feedbackClientLayout.css"};
//		FeedbackPanelHtmlTester fpht = new FeedbackPanelHtmlTester(8888, new File("E:/GoldenGATEv3.WebApp/WEB-INF/feedbackData/"), "feedback.html", csss, null, null);
//		fpht.setActive(true);
		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/20351_gg2d.xml"), "UTF-8"));
		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/21330.MC.xml"), "UTF-8"));
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/22836_supplemen.xml"), "UTF-8"));
		System.out.println("Document loaded");
		
		Analyzer cet = new MaterialsCitationTaggerOnline();
		cet.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/MaterialsCitationTaggerData/")));
		cet.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		
		Analyzer mcp = new MaterialsCitationParserOnline();
		mcp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/MaterialsCitationTaggerData/")));
		mcp.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		
		AnnotationUtils.writeXML(doc, new PrintWriter(System.out));
		
		mcp.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		
		AnnotationUtils.writeXML(doc, new PrintWriter(System.out));
	}
}
