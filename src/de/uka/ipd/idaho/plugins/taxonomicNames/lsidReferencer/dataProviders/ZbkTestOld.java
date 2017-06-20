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

package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.dataProviders;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider.LsidDataSet;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * @author sautter
 *
 */
public class ZbkTestOld {
	public static void main(String[] args) throws Exception {
		ZbkDataProviderDev zbk = new ZbkDataProviderDev();
		zbk.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv2/Plugins/AnaylzerData/TnuluLSIDData/")));
//		LsidDataSet[] lds = zbk.getLsidData("Chromis", true);
		LsidDataSet[] lds = zbk.getLsidData("Ec", true);
		for (int l = 0; l < lds.length; l++)
			System.out.println(lds[l].toString());
		if (true) return;
		
		String baseUrl = "http://zoobank.org/test1/Search.aspx?search=";
		String query = "Chromis";
		URL url = new URL(baseUrl + query);
		final Html html = new Html();
		Parser p = new Parser(html);
		
		final StringRelation genusData = new StringRelation();
		
		p.stream(url.openStream(), new TokenReceiver() {
			private boolean inData = false;
			private boolean inTupel = false;
			private String lsid = null;
			private String dataString = null;
			public void close() throws IOException {
				// TODO Auto-generated method stub
			}
			public void storeToken(String token, int treeDepth) throws IOException {
				// TODO Auto-generated method stub
				if (html.isTag(token)) {
					String type = html.getType(token);
					if (html.isEndTag(token)) {
						if ("span".equals(type))
							this.inData = false;
						else if (this.inData && "a".equals(type)) {
							this.inTupel = false;
							System.out.println(this.lsid + "\n  " + this.dataString);
							
							String data = IoTools.prepareForPlainText(this.dataString).trim();
							if (data.matches("[A-Za-z\\-\\_]++\\,.++")) {
								
								String species = data.substring(0, data.indexOf(',')).trim();
								
								String genus = data.substring(data.indexOf(',') + 1).trim();
								genus = genus.substring(0, genus.indexOf(' ')).trim();
								
								String author = data.substring(data.indexOf(',') + 1).trim();
								author = author.substring(author.indexOf(' ') + 1).trim();
								
								String lsid = this.lsid.substring(this.lsid.lastIndexOf(':') + 1);
								
								String lsidName = (genus + " " + species + " " + author);
								System.out.println(lsidName);
								
								StringTupel st = new StringTupel();
								st.setValue("genus", genus);
								st.setValue("species", species);
								st.setValue("name", lsidName);
								st.setValue("lsid", lsid);
								st.setValue("rank", "species");
								genusData.addElement(st);
							}
							else {
								String genus = data.substring(0, data.indexOf(' ')).trim();
								String author = data.substring(data.indexOf(' ') + 1).trim();
								
								String lsid = this.lsid.substring(this.lsid.lastIndexOf(':') + 1);
								
								String lsidName = (genus + " " + author);
								System.out.println(lsidName);
								
								StringTupel st = new StringTupel();
								st.setValue("genus", genus);
								st.setValue("name", lsidName);
								st.setValue("lsid", lsid);
								st.setValue("rank", "genus");
								genusData.addElement(st);
							}
							
							this.lsid = null;
							this.dataString = null;
						}
					}
					else if (html.isSingularTag(token)) {
						
					}
					else {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						if ("span".equals(type)) {
							//ctl00_ContentPlaceHolder_ActResults
							if ("ctl00_ContentPlaceHolder_ActResults".equals(tnas.getAttribute("id")))
								this.inData = true;
						}
						else if (this.inData) {
							if ("a".equals(type)) {
								String href = tnas.getAttribute("href");
								if (href != null) {
									this.lsid = href.substring(href.lastIndexOf('/') + 1);
									this.inTupel = true;
									this.dataString = "";
								}
							}
						}
					}
				}
				else if (this.inTupel) this.dataString += token;
			}
		});
		StringRelation.writeCsvData(new PrintWriter(System.out), genusData);
	}
}
