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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider;

/**
 * @author sautter
 *
 */
public class ZbkDataProviderSOAP extends LsidDataProvider implements TaxonomicNameConstants {
	
	private static final String REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
	
	private String requestBaseUrl = "http://zoobank.org/test1/Search.aspx?search=";
	
	private static final Grammar grammar = new StandardGrammar();
	private static final Parser parser = new Parser(grammar);
	
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
		
		URL url = this.dataProvider.getURL("http://dev.zoobank.org/srv/lu.asmx");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		con.setRequestProperty("SOAPAction", "http://zoobank.org/srv/luAct2");
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		out.newLine();
		out.write("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		out.newLine();
		out.write("<soap:Body>");
		out.newLine();
		out.write("<luAct2 xmlns=\"http://zoobank.org/srv/\">");
		out.newLine();
		out.write("<tn>" + epithet + (useWildcard ? "%" : "") + "</tn>");
		out.newLine();
		out.write("</luAct2>");
		out.newLine();
		out.write("</soap:Body>");
		out.newLine();
		out.write("</soap:Envelope>");
		out.newLine();
		out.flush();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
		parser.stream(in, new TokenReceiver() {
			private boolean inData = false;
			private Properties data = new Properties();
			private String dataType = null;
			
			public void close() throws IOException {}
			
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					String type = grammar.getType(token);
					if (grammar.isEndTag(token)) {
						if ("LUA".equals(type)) {
							this.inData = false;
							
							String lsid = this.data.getProperty("actLSID");
							String rank = this.data.getProperty("rank");
							
							if ((lsid != null) && (rank != null)) {
								lsid = lsid.substring(lsid.lastIndexOf(':') + 1);
								rank = normalizeRank(rank);
								
								String author = this.data.getProperty("author");
								if (author != null) {
									if (author.indexOf(',') != -1)
										author = author.substring(0, author.indexOf(','));
								}
								String year = this.data.getProperty("year");
								
								String lsidName = (this.data.getProperty("taxonName") + ((author == null) ? "" : (" " + author + ((year == null) ? "" : (" (" + year + ")")))));
								
								dataSets.add(new LsidDataSet(ZbkDataProviderSOAP.this, lsid, lsidName, SPECIES_ATTRIBUTE));
							}
							
							this.data.clear();
						}
						else this.dataType = null;
					}
					else {
						if ("LUA".equals(type))
							this.inData = true;
						else if (this.inData)
							this.dataType = type;
					}
				}
				else if (this.dataType != null)
					this.data.setProperty(this.dataType, IoTools.prepareForPlainText(token));
			}
		});
		
		return ((LsidDataSet[]) dataSets.toArray(new LsidDataSet[dataSets.size()]));
	}
	
	private Properties ranks = new Properties();
	{
		this.ranks.setProperty(TaxonomicNameConstants.FAMILY_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.FAMILY_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.SUBFAMILY_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.SUBFAMILY_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.TRIBE_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.TRIBE_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.GENUS_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.GENUS_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.SUBGENUS_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.SUBGENUS_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.SPECIES_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.SPECIES_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.SUBSPECIES_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.SUBSPECIES_ATTRIBUTE);
		this.ranks.setProperty(TaxonomicNameConstants.VARIETY_ATTRIBUTE.toLowerCase(), TaxonomicNameConstants.VARIETY_ATTRIBUTE);
	}
	private final String normalizeRank(String rank) {
		return this.ranks.getProperty(rank.toLowerCase(), rank);
	}
}
