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

package de.uka.ipd.idaho.plugins.quantityTagger;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class QuantityTaggerTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		MutableAnnotation doc = Gamta.newDocument(Gamta.newTokenSequence("", Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
		SgmlDocumentReader.readDocument(new StringReader("" + 
//				"Testing quantities for the first time at 37.000,8 feet with 0,505 mm air pressure." +
//				" " +
				"Testing quantities for the first time at 37,000.8 feet with 0.505 to 0.506 mm air pressure." +
				" " +
				"Now up to 37,123.4 feet, with 0.5010 +/- 0.01 mm air pressure." +
				" " +
				"Now down to 37,121 feet, with 0.05010 cm +/- 0.1 mm air pressure." +
//				" " +
//				"And still 1,000,000.0000002 miles from home." +
			""), doc);
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("Testing quantities for the first time at 37.000,8 feet with 0,505 mm air pressure. Testing quantities for the first time at 37,000.8 feet with 0.505 mm air pressure."));
		System.out.println("Document loaded");
		
		Analyzer qt = new QuantityTagger();
		qt.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/QuantityTaggerData/")));
		qt.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}
}
