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

package de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.providers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;

/**
 * Higher taxonomy provider backed by ITIS.
 * 
 * @author sautter
 */
public class ItisHigherTaxonomyProvider extends HigherTaxonomyProvider {
	
	private static Properties rankNameNormalizer = new Properties();
	static {
		rankNameNormalizer.setProperty("Kingdom", KINGDOM_ATTRIBUTE);
		rankNameNormalizer.setProperty("Phylum", PHYLUM_ATTRIBUTE);
		rankNameNormalizer.setProperty("Division", PHYLUM_ATTRIBUTE);
		rankNameNormalizer.setProperty("Class", CLASS_ATTRIBUTE);
		rankNameNormalizer.setProperty("Order", ORDER_ATTRIBUTE);
		rankNameNormalizer.setProperty("Family", FAMILY_ATTRIBUTE);
		rankNameNormalizer.setProperty("Tribe", TRIBE_ATTRIBUTE);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "ITIS";
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	protected Properties loadHierarchy(final String epithet, String rank) throws IOException {
		if (epithet == null)
			return null;
		
		URL listUrl = this.dataProvider.getURL(baseUrl + queryBase + epithet);
		HttpURLConnection listCon = ((HttpURLConnection) listUrl.openConnection());
		if (0 < this.lookupTimeout)
			listCon.setReadTimeout(this.lookupTimeout);
		final InputStream listIn = listCon.getInputStream();
		final String[] dataLink = {null};
		InputStream filterListIn = new InputStream() {
			public int read() throws IOException {
				if (dataLink[0] == null)
					return listIn.read();
				else return -1;
			}
		};
		parser.stream(filterListIn, new TokenReceiver() {
			String lastLink = null;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					if ("a".equals(grammar.getType(token)) && !grammar.isEndTag(token)) {
						TreeNodeAttributeSet tokenAttributes = TreeNodeAttributeSet.getTagAttributes(token, grammar);
						this.lastLink = tokenAttributes.getAttribute("href");
					}
				}
				else if (epithet.equals(token.trim()))
					dataLink[0] = this.lastLink;
			}
		});
		listIn.close();
		
		if (dataLink[0] == null) {
			System.out.println("ITIS: Hierarchy for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "' not found.");
			return null;
		}
		
		//next?v_tsn=154211&taxa=&p_king=every&p_string=containing&p_ifx=cbif&p_lang=
		String itisGenusNumber = dataLink[0];
		try {
			itisGenusNumber = itisGenusNumber.substring(itisGenusNumber.indexOf('=') + 1);
			itisGenusNumber = itisGenusNumber.substring(0, itisGenusNumber.indexOf('&'));
		}
		catch (Exception e) {
			System.out.println("ITIS: Error parsing number for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "' from " + dataLink[0] + ": " + e.getMessage());
			e.printStackTrace(System.out);
			return null;
		}
		
		URL dataUrl = this.dataProvider.getURL(baseUrl + dataBase + itisGenusNumber);
		HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
		if (0 < this.lookupTimeout)
			dataCon.setReadTimeout(this.lookupTimeout);
		TreeNode dataRoot = parser.parse(dataCon.getInputStream());
		TreeNode[] hierarchyNodes = TreeTools.getAllNodesOfType(dataRoot, "parent");
		Properties hierarchy = new Properties();
		for (int h = 0; h < hierarchyNodes.length; h++) {
			TreeNode rankNode = hierarchyNodes[h].getChildNode("rank", 0);
			TreeNode epithetNode = hierarchyNodes[h].getChildNode("concatenatedname", 0);
			if ((rankNode == null) || (epithetNode == null))
				continue;
			String dRank = rankNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0).getNodeValue();
			String ndRank = rankNameNormalizer.getProperty(dRank);
			if (ndRank == null)
				continue;
			if ((rank != null) && !ndRank.equals(rank))
				continue;
			String dEpithet = epithetNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0).getNodeValue();
			hierarchy.setProperty(ndRank, dEpithet);
		}
		return hierarchy;
	}
	
	//	TODO adjust to this new URL: www.cbif.gc.ca/acp/eng/itis/browse?itisSearchFormComm=&itisSearchFormAny=&itisSearchFormMod=&itisSearchFormSci=Chromis
	
	//	TODO adjust to this new detail URL: http://www.cbif.gc.ca/acp/eng/itis/view?tsn=154193
	
	//	Search extremely slow, have to consider refraining from using ITIS
	
	private static String baseUrl = "http://www.cbif.gc.ca/pls/itisca/";
	private static String queryBase = "taxastep?king=every&p_action=containing&p_format=&p_ifx=cbif&p_lang=&taxa=";
	private static String dataBase = "taxa_xml.upwards?p_type=y&p_lang=&p_tsn=";
	
	private static final Grammar grammar = new Html();
	private static final Parser parser = new Parser(grammar);
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		ItisHigherTaxonomyProvider itisHhp = new ItisHigherTaxonomyProvider();
		itisHhp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/ItisHierarchyData/")));
		
		String testGenus = "Chromis";
		Properties hierarchy = itisHhp.getHierarchy(testGenus, true);
		for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			System.out.println(rank + ": " + hierarchy.getProperty(rank));
		}
		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21330.complete.xml"), "UTF-8"));
//		ihi.process(doc, new Properties());
//		Annotation[] taxonNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		for (int t = 0; t < taxonNames.length; t++)
//			System.out.println(taxonNames[t].toXML());
	}
}