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

package de.uka.ipd.idaho.plugins.abbreviationHandling;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class AbbreviationTest implements AbbreviationConstants {
	
	//	!!! FOR TESTING ONLY !!!
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		DocumentRoot doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/EcologyTestbed/schatz_gerecke_1996.normalized.xml"), "UTF-8"));
		Properties params = new Properties();
		params.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
		
		AbbreviationTaggerOnline ato = new AbbreviationTaggerOnline();
		ato.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AbbreviationHandlerData/")));
		ato.process(doc, params);
		
		Annotation[] abbreviations = doc.getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		for (int a = 0; a < abbreviations.length; a++) {
			if ("1".equals(abbreviations[a].getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE)))
				abbreviations[a].setAttribute("test", ("" + Math.random()));
		}
		MutableAnnotation[] abbreviationDatas = doc.getMutableAnnotations(ABBREVIATION_DATA_ANNOTATION_TYPE);
		for (int a = 0; a < abbreviationDatas.length; a++)
			AnnotationUtils.writeXML(abbreviationDatas[a], new PrintWriter(System.out));
		
		AbbreviationAugmenter aa = new AbbreviationAugmenter();
		aa.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AbbreviationHandlerData/")));
		aa.process(doc, params);
		
		AbbreviationReferenceResolver arr = new AbbreviationReferenceResolver();
		arr.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AbbreviationHandlerData/")));
		arr.process(doc, params);
		
		abbreviations = doc.getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		for (int a = 0; a < abbreviations.length; a++)
			System.out.println(abbreviations[a].toXML());
	}
}
