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

package de.uka.ipd.idaho.plugins.paragraphStructuring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.pageNormalization.PageBorderFinder;
import de.uka.ipd.idaho.plugins.pageNormalization.PageMarker;

/**
 * @author sautter
 *
 */
public class ParagraphCheckerOnlineTest {
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final BufferedReader br = new BufferedReader(new FileReader("E:/Projektdaten/Novitates0045/N0045.xml"));
		final Reader r = new Reader() {
			public void close() throws IOException {
				br.close();
			}
			public int read(char[] cbuf, int off, int len) throws IOException {
				int o = 0;
				int c = this.read();
				if (c == -1) return -1;
				while ((o < len) && (c != -1)) {
					cbuf[off + o] = ((char) c);
					o++;
					c = this.read();
				}
				return o;
			}
			private int offset = 0;
			private String buffer = "";
			public int read() throws IOException {
				if (this.offset >= this.buffer.length()) {
					String line = br.readLine();
					if (line == null)
						return -1;
					else {
						if (line.trim().startsWith("<pb"))
							line = "<hr>";
						this.buffer = (line + "\n");
						this.offset = 0;
					}
				}
				return this.buffer.charAt(this.offset++);
			}
		};
		final MutableAnnotation doc = SgmlDocumentReader.readDocument(r);
		r.close();
		
		for (int t = 0; t < doc.size(); t++)
			if (doc.getWhitespaceAfter(t).indexOf('\n') != -1)
				doc.tokenAt(t).setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
		AnnotationFilter.renameAnnotations(doc, "p", "P");
		AnnotationFilter.renameAnnotations(doc, "P", MutableAnnotation.PARAGRAPH_TYPE);
		
		Properties params = new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		};
		
		Analyzer pageBorderFinder = new PageBorderFinder();
		pageBorderFinder.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/PageNormalizationData/")));
		pageBorderFinder.process(doc, params);
		
		Analyzer pageMarker = new PageMarker();
		pageMarker.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/PageNormalizationData/")));
		pageMarker.process(doc, params);
		
		ParagraphCheckerOnline pco = new ParagraphCheckerOnline();
		pco.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/ParagraphsData/")));
		pco.process(doc, params);
		
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}
}
