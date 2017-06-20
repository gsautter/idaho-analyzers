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
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider;

/**
 * LSID data provider for FishBase (HNS)
 * 
 * @author sautter
 */
public class FsbDataProvider extends LsidDataProvider implements TaxonomicNameConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider#getProviderCode()
	 */
	public String getProviderCode() {
		return "FSB";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider#getProviderName()
	 */
	public String getProviderName() {
		return "FishBase";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider#getLsidUrnPrefix()
	 */
	public String getLsidUrnPrefix() {
		// TODO verify this
		return "urn:lsid:fishbase.org:concepts:";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#init()
	 */
	protected void init() {
		
		//	get request url
		this.requestBaseUrl = this.getParameter(REQUEST_URL_ATTRIBUTE, this.requestBaseUrl);
	}
	
	private static final String REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
	private String requestBaseUrl = "http://www.fishbase.org/NomenClature/ScientificNameSearchList.php?group=summary&backstep=-2&crit2_fieldname=SYNONYMS.SynSpecies&crit2_fieldtype=CHAR&crit2_operator=contains&crit2_value=&crit1_fieldname=SYNONYMS.SynGenus&crit1_fieldtype=CHAR&crit1_operator=EQUAL&crit1_value=";
	
	private Html html = new Html();
	private Parser parser = new Parser(this.html);
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider#getLsidData(java.lang.String, boolean)
	 */
	public LsidDataSet[] getLsidData(String epithet, boolean useWildcard) throws IOException {
		if ((epithet == null) || (epithet.trim().length() == 0))
			return new LsidDataSet[0];
		
		final ArrayList lsidDataSets = new ArrayList();
//		URL url = new URL(this.requestBaseUrl + URLEncoder.encode(epithet, "UTF-8"));
		URL url = this.dataProvider.getURL(this.requestBaseUrl + URLEncoder.encode(epithet, "UTF-8"));
		TokenReceiver tr = new TokenReceiver() {
			private boolean inData = false;
			private int dataFieldIndex = 0;
			private String dataString = null;
			
			private String lsidNumber = null;
			private String taxonName = null;
			private String taxonRank = null;
			
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					
					if ("td".equalsIgnoreCase(type)) {
						if (html.isEndTag(token)) {
							if (this.dataString != null) {
								if (this.dataFieldIndex == 0) {
									this.taxonName = this.dataString;
									this.taxonRank = ((this.taxonName.indexOf(' ') == -1) ? GENUS_ATTRIBUTE : SPECIES_ATTRIBUTE);
								}
								//	TODO maybe parse more fields as it becomes necessary
							}
							this.dataString = null;
							this.dataFieldIndex++;
						}
					}
					
					else if ("tr".equalsIgnoreCase(type)) {
						if (html.isEndTag(token)) {
							if ((this.lsidNumber != null) && (this.taxonName != null))
								lsidDataSets.add(new LsidDataSet(FsbDataProvider.this, this.lsidNumber, this.taxonName, this.taxonRank));
							
							this.lsidNumber = null;
							this.taxonName = null;
							this.taxonRank = null;
							this.inData = false;
							this.dataFieldIndex = 0;
						}
						else this.inData = true;
					}
					
					else if ("a".equalsIgnoreCase(type) && !html.isEndTag(token)) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						String rawLsid = tnas.getAttribute("href");
						if ((rawLsid != null) && (rawLsid.indexOf("ID=") != -1)) {
							rawLsid = rawLsid.substring(rawLsid.indexOf("ID=") + "ID=".length());
							if (rawLsid.indexOf('&') != -1)
								rawLsid = rawLsid.substring(0, rawLsid.indexOf('&'));
							if (Gamta.isNumber(rawLsid))
								this.lsidNumber = rawLsid;
						}
					}
				}
				else if (this.inData) {
					token = token.trim();
					if (token.length() != 0)
						this.dataString = html.unescape(token).trim();
				}
			}
		};
		
		InputStream is = null;
		try {
			is = url.openStream();
			this.parser.stream(is, tr);
		}
		
		finally {
			if (is != null)
				is.close();
		}
		
		return ((LsidDataSet[]) lsidDataSets.toArray(new LsidDataSet[lsidDataSets.size()]));
	}
	
	/*
<tr bgcolor='FFFFFF'>
<td width="20%" valign="top"><font size="2">
<a href='../Summary/speciesSummary.php?ID=58941&genusname=Chromis&speciesname=abrupta'><i>Chromis abrupta</i></a>
</td>	
<td valign="top" width="20%"><font size="2">Randall, 2001</td>
<td valign="top" width="20%"><font size="2"><i>Chromis abrupta</i></td>
<td valign="top" width="20%"><font size="2">Pomacentridae</td>
<td valign="top" width="20%"><font size="2"></td>
</tr>
	 */
	
	/*
http://www.fishbase.org/NomenClature/ScientificNameSearchList.php
?crit1_fieldname=SYNONYMS.SynGenus
&crit1_fieldtype=CHAR
&crit1_operator=EQUAL
&crit1_value=Chromis
&crit2_fieldname=SYNONYMS.SynSpecies
&crit2_fieldtype=CHAR
&crit2_operator=contains
&crit2_value=
&group=summary
&backstep=-2
	 */
	
	// FOR TEST PURPOSES ONLY 
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		FsbDataProvider fsb = new FsbDataProvider();
		fsb.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/LsidReferencerData/FsbData/")));
		String genus = "Chromis";
		LsidDataSet[] ldss = fsb.getLsidData(genus, true);
		for (int d = 0; d < ldss.length; d++)
			System.out.println(ldss[d].toString());
	}
}
