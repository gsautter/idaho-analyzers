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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

public class FatTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		FAT fat = new FAT();
		System.out.println("FAT instantiated");
		fat.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/LexiconFATData/")));
		System.out.println("FAT initialized");
		JOptionPane.showMessageDialog(null, "FAT initialized", "FAT initialized", JOptionPane.PLAIN_MESSAGE);
		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/4075_gg1.xml"), "UTF-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/8127_1_gg1.xml"), "UTF-8"));
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21330.htm.xml"), "UTF-8"));
		System.out.println("Got document");
		Annotation[] taxNames = doc.getAnnotations("taxonomicName");
		for (int t = 0; t < taxNames.length; t++)
			doc.removeAnnotation(taxNames[t]);
		System.out.println("taxonomic names removed");
//		LexiconRules lr = new LexiconRules();
//		lr.setDataPath("E:/GoldenGATE/Resources/Analyzer/NewFatData/");
//		lr.process(doc, true);
		
		doc.addAnnotationListener(new AnnotationListener() {
			public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
				if ("Hymenoptera".equals(annotation.firstValue())) {
					System.out.println("'Hymenoptera' annotated as '" + annotation.getType() + "'.");
				}
//				else if ("Collingwood".equals(annotation.firstValue())) {
//					System.out.println("'" + annotation.getValue() + "' annotated as '" + annotation.getType() + "'.");
////					if (FAT.TAXONOMIC_NAME_ANNOTATION_TYPE.equals(annotation.getType()))
////						throw new RuntimeException("'Collingwood ...' became match.");
//				}
			}
			public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {}
			public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
				if ("Hymenoptera".equals(annotation.firstValue())) {
					System.out.println("'Hymenoptera' removed.");
//					throw new RuntimeException("'Hymenoptera' removed.");
				}
			}
			public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
				if ("Hymenoptera".equals(annotation.firstValue())) {
					System.out.println("'Hymenoptera' re-annotated as '" + annotation.getType() + "'.");
//					if (FAT.TAXONOMIC_NAME_ANNOTATION_TYPE.equals(annotation.getType()))
//						throw new RuntimeException("'Pro ...' became match.");
				}
//				else if ("Collingwood".equals(annotation.firstValue())) {
//					System.out.println("'" + annotation.getValue() + "' re-annotated as '" + annotation.getType() + "'.");
//				}
			}
		});
		
		
		fat.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		System.out.println("FAT finished");
		taxNames = doc.getAnnotations("taxonomicName");
		for (int t = 0; t < taxNames.length; t++)  {
			System.out.println(taxNames[t].toXML());
//			System.out.println(taxNames[t].getValue());
//			System.out.println(new TaxonomicName(taxNames[t]).toString());
		}
		//doc.writeXML(new OutputStreamWriter(System.out));
	}
}
