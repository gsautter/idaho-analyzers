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
 *     * Neither the name of the Universit�t Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSIT�T KARLSRUHE (TH) AND CONTRIBUTORS 
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

package de.uka.ipd.idaho.plugins.pageNormalization;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.ImmutableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.TestDocumentProvider;

/**
 * @author sautter
 *
 */
public class ParagraphClassifyerOnlineTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Projektdaten/TaxonxTest/21112_gg-1.xml"));
		//doc.writeXML(new OutputStreamWriter(System.out));
		Gamta.addTestDocumentProvider(new TestDocumentProvider() {
			public QueriableAnnotation getTestDocument() {
				return new ImmutableAnnotation(doc);
			}
		});
		System.out.println("Got document");
		
//		//	eliminate subSubSections because they're not part of TaxonX level 1
//		MutableAnnotation[] ssss = doc.getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
//		for (int d = 0; d < ssss.length; d++)
//			doc.removeAnnotation(ssss[d]);
		
		ParagraphClassifyerOnline pco = new ParagraphClassifyerOnline();
		pco.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/PageNormalizationData/")));
		System.out.println("ParagraphClassifyer initialized");
		
//		tds.configureProcessor();
		pco.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		pco.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		tds.process(doc, new Properties() {
//			public synchronized boolean containsKey(Object key) {
//				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
//			}
//		});
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}

}
