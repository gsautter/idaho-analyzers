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

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider;

/**
 * @author sautter
 *
 */
public class ZbkDataProvider extends LsidDataProvider implements TaxonomicNameConstants {
	
	private static final String REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
	
//	private String requestBaseUrl = "http://www.zoobank.org/Search.aspx?search=";
	private String requestBaseUrl = "http://www.zoobank.org/Search?search_term=";
	
	private static final Html html = new Html() {
		public String getProcessingInstructionEndMarker() {
			return ">"; // we have to be this fuzzy here because the ZooBank template engine uses <?VariableName> variables to insert content from the database, and if a value is missing, the variables remain in the HTML
		}
	};
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
	public LsidDataSet[] getLsidData(final String epithet, boolean useWildcard) throws IOException {
		final ArrayList dataSets = new ArrayList();
		URL url = this.dataProvider.getURL(this.requestBaseUrl + URLEncoder.encode((epithet + (useWildcard ? "%" : "")), "UTF-8"));
		
		parser.stream(new InputStreamReader(url.openStream(), "UTF-8"), new TokenReceiver() {
			private String lsid = null;
			private String nameString = "";
			private String lastNameStringToken = "";
			private int epithetCount = 0;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					if (html.isEndTag(token)) {
						if ("a".equalsIgnoreCase(type)) {
							if ((this.lsid != null) && (this.epithetCount != 0) && this.nameString.toLowerCase().startsWith(epithet.toLowerCase() + " "))
								dataSets.add(new LsidDataSet(ZbkDataProvider.this, this.lsid, this.nameString, ((this.epithetCount == 1) ? GENUS_ATTRIBUTE : ((this.epithetCount == 2) ? SPECIES_ATTRIBUTE : SUBSPECIES_ATTRIBUTE))));
							this.lsid = null;
							this.nameString = "";
							this.lastNameStringToken = "";
							this.epithetCount = 0;
						}
					}
					else if (html.isSingularTag(token)) {}
					else {
						if ("a".equalsIgnoreCase(type)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							String href = tnas.getAttribute("href");
							if ((href != null) && href.startsWith("/NomenclaturalActs/")) {
								this.lsid = href.substring("/NomenclaturalActs/".length());
							}
						}
						else if ((this.lsid != null) && "em".equalsIgnoreCase(type))
							this.epithetCount++;
					}
				}
				else if (this.lsid != null) {
					token = IoTools.prepareForPlainText(token).trim();
					if (token.length() != 0) {
						if ((this.lastNameStringToken != "") && Gamta.insertSpace(this.lastNameStringToken, token))
							this.nameString += " ";
						this.nameString += token;
						if (token.lastIndexOf(' ') != -1)
							token = token.substring(token.lastIndexOf(' ')).trim();
						this.lastNameStringToken = token;
					}
				}
			}
		});
		
		return ((LsidDataSet[]) dataSets.toArray(new LsidDataSet[dataSets.size()]));
	}
}