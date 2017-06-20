///*
// * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer;
//
//
//import java.io.File;
//import java.util.Properties;
//
//import javax.swing.UIManager;
//
//import de.uka.ipd.idaho.gamta.AnnotationUtils;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.util.Analyzer;
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
//import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
//import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
//
//public class TnuluLsidTest {
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		//	set platform L&F
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		// 
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/8200_gg0.xml"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/PonerinaeV10.xml"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/3994_gg1.xml"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TutorialExampleSolutionWithErrors.xml"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/3712_gg1.xml"));
//		AnnotationFilter.removeAnnotationAttribute(doc, "taxonomicName", "LSID-HNS");
//		AnnotationFilter.removeAnnotationAttribute(doc, "taxonomicName", "lsidName-HNS");
//		Analyzer lookup = new TnuluLsidReferencer();
//		lookup.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv2/Resources/AnalyzerData/TnuluLSIDData/")));
//		lookup.process(doc, new Properties() {
//			public synchronized boolean containsKey(Object key) {
//				return (Analyzer.INTERACTIVE_PARAMETER.equals(key) || Analyzer.ONLINE_PARAMETER.equals(key));
//			}
//		});
//		System.out.println(AnnotationUtils.toXML(doc));
////		lookup.process(doc, new Properties() {
////			public synchronized boolean containsKey(Object key) {
////				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
////			}
////		});
////		System.out.println(AnnotationUtils.toXML(doc));
//	}
//}
