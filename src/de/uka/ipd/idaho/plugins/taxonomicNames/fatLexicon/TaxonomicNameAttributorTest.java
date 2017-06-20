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
import java.io.FileReader;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

public class TaxonomicNameAttributorTest {
	public static void main(String[] args) throws Exception {
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Projektdaten/TaxonxTest/3933_gg00bt.xml"));
		Annotation[] taxNames = doc.getAnnotations("taxonomicName");
		for (int t = 0; t < taxNames.length; t++)
			taxNames[t].clearAttributes();
//		LexiconRules lr = new LexiconRules();
//		lr.setDataPath("E:/GoldenGATE/Resources/Analyzer/NewFatData/");
//		lr.process(doc, true);
		TaxonomicNameAttributor tna = new TaxonomicNameAttributor();
		tna.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATE/Resources/Analyzer/LexiconFATData/")));
		
		TaxonomicNameCompleter tnc = new TaxonomicNameCompleter();
		tnc.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATE/Resources/Analyzer/LexiconFATData/")));
		
		tna.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		tnc.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		
//		FAT fat = new FAT();
//		fat.setDataPath("E:/GoldenGATE/Resources/Analyzer/LexiconFATData/");
//		fat.process(doc, true);
		taxNames = doc.getAnnotations("taxonomicName");
		for (int t = 0; t < taxNames.length; t++)  {
			System.out.println(taxNames[t].getStartIndex() + ", " + taxNames[t].size() + ": " + taxNames[t].toXML());
//			System.out.println(taxNames[t].getValue());
//			System.out.println(new TaxonomicName(taxNames[t]).toString());
		}
//		doc.writeXML(new OutputStreamWriter(System.out));
	}
}
