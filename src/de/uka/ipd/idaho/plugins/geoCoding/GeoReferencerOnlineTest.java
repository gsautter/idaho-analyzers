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

package de.uka.ipd.idaho.plugins.geoCoding;

import java.io.File;
import java.io.PrintWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class GeoReferencerOnlineTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/21330_gg0.win.xml"));
		
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Corpora/AquaintNYT_NE/19980601_NYT.text/text.008.xml"));
		
		Annotation[] nes = doc.getAnnotations("ne");
		for (int n = 0; n < nes.length; n++)
			if ("l".equals(nes[n].getAttribute("type")))
				nes[n].changeTypeTo(Location.LOCATION_TYPE);
		AnnotationFilter.renameAnnotations(doc, "P", MutableAnnotation.PARAGRAPH_TYPE);
		
		Analyzer analyzer = new GeoReferencerOnline();
		analyzer.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/")));
		analyzer.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		AnnotationUtils.writeXML(doc, new PrintWriter(System.out));
	}
}
