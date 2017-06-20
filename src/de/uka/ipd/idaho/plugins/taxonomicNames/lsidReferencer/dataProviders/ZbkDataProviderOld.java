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


import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class ZbkDataProviderOld extends LsidDataProvider implements TaxonomicNameConstants {
	
	private static final String REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
	
	private String requestBaseUrl = "http://zoobank.org/test1/Search.aspx?search=";
	
	private static final Html html = new Html();
	private static final Parser parser = new Parser(html);
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#init()
	 */
	protected void init() {
		this.requestBaseUrl = this.getParameter(REQUEST_URL_ATTRIBUTE, this.requestBaseUrl);
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getProviderCode()
	 */
	public String getProviderCode() {
		return "ZBK";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getProviderName()
	 */
	public String getProviderName() {
		return "Zoobank";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getLsidUrnPrefix()
	 */
	public String getLsidUrnPrefix() {
		return "urn:lsid:zoobank.org:act:";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getLsidData(java.lang.String, boolean)
	 */
	public LsidDataSet[] getLsidData(String epithet, boolean useWildcard) throws IOException {
		final ArrayList dataSets = new ArrayList();
		URL url = this.dataProvider.getURL(this.requestBaseUrl + URLEncoder.encode((epithet + (useWildcard ? "%" : "")), "UTF-8"));
		
		parser.stream(new InputStreamReader(url.openStream(), "UTF-8"), new TokenReceiver() {
			private boolean inData = false;
			private String lsid = null;
			private String dataString = null;
			private StringVector nameParts = new StringVector();
			private boolean inNamePart = false;
			private String authorName = null;
			public void close() throws IOException {
				// nothing to do here
			}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					
					if (html.isEndTag(token)) {
						if ("span".equals(type))
							this.inData = false;
						
						else if (this.inData) {
							
							if ("a".equals(type)) {
								String data = IoTools.prepareForPlainText(this.dataString).trim();
								if (data.matches("[^\\,\\s]++\\,.++")) {
									this.nameParts.addElement(this.nameParts.remove(0));
									
									String lsid = this.lsid.substring(this.lsid.lastIndexOf(':') + 1);
									String lsidName = (this.nameParts.concatStrings(" ") + " " + this.authorName);
									
									dataSets.add(new LsidDataSet(ZbkDataProviderOld.this, lsid, lsidName, SPECIES_ATTRIBUTE));
								}
								else {
									String lsid = this.lsid.substring(this.lsid.lastIndexOf(':') + 1);
									String lsidName = (this.nameParts.concatStrings(" ") + " " + this.authorName);
									
									dataSets.add(new LsidDataSet(ZbkDataProviderOld.this, lsid, lsidName, GENUS_ATTRIBUTE));
								}
								
								this.lsid = null;
								this.dataString = null;
								this.nameParts.clear();
								this.authorName = null;
							}
							else if ("i".equals(type) || "b".equals(type)) this.inNamePart = false;
						}
					}
					
					else if (html.isSingularTag(token)) {}
					
					else {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						
						if ("span".equals(type)) {
							if ("ctl00_ContentPlaceHolder_ActResults".equals(tnas.getAttribute("id")))
								this.inData = true;
						}
						
						else if (this.inData) {
							if ("a".equals(type)) {
								String href = tnas.getAttribute("href");
								if (href != null) {
									this.lsid = href.substring(href.lastIndexOf('/') + 1);
									this.dataString = "";
								}
							}
							else if ("i".equals(type) || "b".equals(type)) this.inNamePart = true;
						}
					}
				}
				else if (this.dataString != null) {
					
					this.dataString += token;
					
					token = IoTools.prepareForPlainText(token).trim();
					if (token.startsWith(")")) // cut closing bracket from preceeding sub genus
						token = token.substring(1).trim();
					
					if (this.inNamePart)
						this.nameParts.addElement(token);
					
					else if (this.authorName != null)
						this.authorName += (" " + token);
					
					else if (token.matches("[A-Za-z]++.*+"))
						this.authorName = token;
					
				}
			}
		});
		
		return ((LsidDataSet[]) dataSets.toArray(new LsidDataSet[dataSets.size()]));
	}
}
