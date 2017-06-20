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

package de.uka.ipd.idaho.plugins.modsReferencer;


import java.io.File;
import java.io.FileReader;
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
public class ModsReferencerTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		//http://atbi.biosci.ohio-state.edu:210/hymenoptera/hym_utilities.format_ref/style=MODS&id=0978
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
//		URL url = new URL("http://atbi.biosci.ohio-state.edu:210/hymenoptera/hym_utilities.format_ref?style=MODS&id=21330");
//		Parser parser = new Parser();
//		TreeNode root = parser.parse(new FilterReader(new InputStreamReader(url.openStream(), "UTF-8")) {
//			public int read() throws IOException {
//				int r = super.read();
//				System.out.print((char) r);
//				return r;
//			}
//			public int read(char[] cbuf, int off, int len) throws IOException {
//				int r = super.read(cbuf, off, len);
//				if (r != -1)
//					System.out.print(new String(cbuf, off, r));
//				return r;
//			}
//		});
//		System.out.println(root.treeToCode());
//		if (true) return;
		
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Testdaten/21330.citations.xml"));
//		doc.setAttribute(ModsConstants.MODS_ID_ATTRIBUTE, "21330");
		ModsReferencer mr = new ModsReferencer();
		mr.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/ModsReferencerData/")));
		Properties prop = new Properties();
		prop.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
		prop.setProperty(Analyzer.ONLINE_PARAMETER, Analyzer.ONLINE_PARAMETER);
		mr.process(doc, prop);
		AnnotationUtils.writeXML(doc, new PrintWriter(System.out));
	}

}
