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

package de.uka.ipd.idaho.plugins.pageNormalization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class PageNumbererTest {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		PageNumberer pn = new PageNumberer();
		pn.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/PageNormalizationData/")));
		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Testdaten/EcologyTestbed/spelda2001.pnt.htm.xml"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Projektdaten/SMNK-Projekt/EcologyTestbed/Greven_Rüther_DHaese_2001.pnt.htm.xml"));
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("" +
				"<page pageId=\"0\">MISSING</page>" +
				"<page pageId=\"1\">23 28</page>" +
				"<page pageId=\"2\">29</page>" +
				"<page pageId=\"3\">80</page>" +
				"<page pageId=\"4\">NOPAGENUMBER</page>" +
				"<page pageId=\"5\">32</page>" +
				"<page pageId=\"6\">NONE</page>" +
				"<page pageId=\"7\">34</page>" +
				"<page pageId=\"8\">37</page>" +
				"<page pageId=\"9\">38</page>" +
				"<page pageId=\"10\">S9</page>" +
				"<page pageId=\"11\">NONE</page>" +
				"<page pageId=\"12\">41</page>" +
				"<page pageId=\"13\">42</page>" +
				"<page pageId=\"14\">48 43</page>" +
				""));
		pn.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}
}
