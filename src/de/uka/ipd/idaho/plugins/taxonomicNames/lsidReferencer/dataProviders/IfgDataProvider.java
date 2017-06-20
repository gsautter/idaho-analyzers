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
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider;

/**
 * LSID data provider for Index Fungorum (IFG)
 * 
 * @author sautter
 */
public class IfgDataProvider extends LsidDataProvider {

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getProviderCode()
	 */
	public String getProviderCode() {
		return "IFG";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getProviderName()
	 */
	public String getProviderName() {
		return "Index Fungorum";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getLsidUrnPrefix()
	 */
	public String getLsidUrnPrefix() {
		return "urn:lsid:indexfungorum.org:names:";
	}

	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#init()
	 */
	protected void init() {
		
		//	get request url
		this.requestBaseUrl = this.getParameter(REQUEST_URL_ATTRIBUTE);
	}
	
	private static final String REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
	private String requestBaseUrl = "http://www.indexfungorum.org/IXFWebService/Fungus.asmx/NameSearch?AnywhereInText=false&MaxNumber=100&SearchText=";
	
	private static final Grammar grammar = new StandardGrammar();
	private static final Parser parser = new Parser(grammar); 
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getLsidData(java.lang.String, boolean)
	 */
	public LsidDataSet[] getLsidData(String epithet, boolean useWildcard) throws IOException {
		System.out.println("IFG LsidDataProvider: doing lookup for '" + epithet + "'");
		final ArrayList lsidData = new ArrayList();
		
		URL url = this.dataProvider.getURL(this.requestBaseUrl + URLEncoder.encode(epithet, "UTF-8"));
		
		TokenReceiver tr = new TokenReceiver() {
			
			private String data = null;
			
			private String taxonName = null;
			private String lsid = null;
			
			public void close() throws IOException {}
			
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					String type = grammar.getType(token);
					
					if (grammar.isEndTag(token)) {
						
						if ("IndexFungorum".equals(type)) {
							if ((this.taxonName != null) && (this.lsid != null))
								lsidData.add(new LsidDataSet(IfgDataProvider.this, this.lsid, this.taxonName, null));
							
							this.taxonName = null;
							this.lsid = null;
						}
						else if ("NAME_x0020_OF_x0020_FUNGUS".equals(type))
							this.taxonName = data;
						
						else if ("RECORD_x0020_NUMBER".equals(type))
							this.lsid = data;
					}
					
					else this.data = null;
				}
				
				else this.data = IoTools.prepareForPlainText(token);
			}
		};
		
		InputStream is = null;
		try {
			is = url.openStream();
			parser.stream(is, tr);
		}
		finally {
			if (is != null)
				is.close();
		}
		
		return ((LsidDataSet[]) lsidData.toArray(new LsidDataSet[lsidData.size()]));
	}
}
