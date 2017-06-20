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

package de.uka.ipd.idaho.plugins.materialsCitations;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * @author sautter
 *
 */
public class CollectionCodeDataFetcher {
	public static void main(String[] args) throws Exception {
		final Properties mappings = new Properties();
		BufferedReader br = new BufferedReader(new FileReader("E:/GoldenGATEv2/Resources/AnalyzerData/Analyzer.builtinData/mappings.txt"));
		while (br.ready()) {
			String line = br.readLine().trim();
			if ((line.length() > 2) && !line.startsWith("//")) {
				String original = line.substring(0, 1).trim();
				String substitute = line.substring(2).trim();
				
				if ((original.length() != 0) && (substitute.length() != 0))
					mappings.setProperty(original, substitute);
			}
		}
		br.close();
		
		final StringRelation data = new StringRelation();
		
		final Grammar html = new Html();
		Parser parser = new Parser(html);
		parser.stream(new URL("http://hbs.bishopmuseum.org/codens/codens-inst.html").openStream(), new TokenReceiver() {
			boolean inData = false;
			int column = 0;
			String collCode = "";
			String collName = "";
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					if (html.isEndTag(token)) {
						if ("table".equalsIgnoreCase(type)) this.inData = false;
						else if ("tr".equalsIgnoreCase(type)) {
							System.out.println(this.collCode + "\t" + this.collName);
							if ((this.collCode.length() != 0) && (this.collName.length() != 0)) {
								StringTupel record = new StringTupel();
								record.setValue("code", this.collCode);
								record.setValue("name", this.collName);
								data.addElement(record);
							}
							this.column = 0;
							this.collCode = "";
							this.collName = "";
						}
						else if ("td".equalsIgnoreCase(type)) this.column++;
					}
					else if (html.isSingularTag(token)) {}
					else {
						if ("table".equalsIgnoreCase(type)) this.inData = true;
					}
				}
				else if (this.inData) {
					String value = IoTools.prepareForPlainText(token);
					StringBuffer sb = new StringBuffer();
					for (int c = 0; c < value.length(); c++) {
						String s = value.substring(c, (c+1));
						sb.append(mappings.getProperty(s, s));
					}
					value = sb.toString().trim();
					
					if (value.length() != 0) {
						if (this.column == 0) this.collCode += value;
						else if (this.column == 1) this.collName += value;
					}
				}
			}
		});
		StringVector keys = new StringVector();
		keys.addElement("code");
		keys.addElement("name");
		StringRelation.writeCsvData(new PrintWriter(System.out), data, keys);
	}
}
