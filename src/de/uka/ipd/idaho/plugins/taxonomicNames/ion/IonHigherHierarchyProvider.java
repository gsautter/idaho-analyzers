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

package de.uka.ipd.idaho.plugins.taxonomicNames.ion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;

/**
 * @author sautter
 */
public class IonHigherHierarchyProvider  implements TaxonomicNameConstants {
	
	private AnalyzerDataProvider dataProvider;
	
	/**
	 * Constructor
	 * @param dataProvider the data provider to use for local caching and for
	 *            obtaining URLs for online lookups
	 */
	public IonHigherHierarchyProvider(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}
	
	private HashMap cache = new HashMap();
	
	/**
	 * Obtain the higher taxonomic ranks for a given genus from ITIS. If ITIS
	 * does not provide the higher ranks for the specified genus, or if the ITIS
	 * server is unreachable and the higher ranks for the specified genus are
	 * not cached, this method returns null.
	 * @param genus the genus to get the higher ranks for
	 * @param allowWebAccess allow downloading data from ITIS in case of a file
	 *            cache miss?
	 * @return a Properties object containing the higher taxonomic ranks for the
	 *         argument genus
	 */
	public Properties getHierarchy(String genus, boolean allowWebAccess) {
		Properties hierarchy = ((Properties) this.cache.get(genus));
		if (hierarchy != null)
			return hierarchy;
		
		String cacheDataName = ("cache/" + genus + ".txt");
		
		if (this.dataProvider.isDataAvailable(cacheDataName)) try {
			BufferedReader cacheReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8"));
			String line;
			while ((line = cacheReader.readLine()) != null) {
				int split = line.indexOf('=');
				if (split == -1)
					continue;
				String rank = line.substring(0, split);
				String epithet = line.substring(split + 1);
				if (hierarchy == null)
					hierarchy = new Properties();
				hierarchy.setProperty(rank, epithet);
			}
		}
		catch (IOException ioe) {
			System.out.println("Error loading cached data for genus '" + genus + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			hierarchy = null;
		}
		
		if (hierarchy != null) {
			this.cache.put(genus, hierarchy);
			return hierarchy;
		}
		
		if (allowWebAccess) try {
			hierarchy = loadHierarchy(genus);
		}
		catch (IOException ioe) {
			System.out.println("Error loading data for genus '" + genus + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			hierarchy = null;
		}
		catch (Exception e) {
			System.out.println("Error loading data for genus '" + genus + "': " + e.getMessage());
			e.printStackTrace(System.out);
			hierarchy = null;
		}
		
		if (hierarchy != null) {
			if (this.dataProvider.isDataEditable()) try {
				BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(cacheDataName), "UTF-8"));
				for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
					String rank = ((String) rit.next());
					String epithet = hierarchy.getProperty(rank);
					cacheWriter.write(rank + "=" + epithet);
					cacheWriter.newLine();
				}
				cacheWriter.flush();
				cacheWriter.close();
			}
			catch (IOException ioe) {
				System.out.println("Error caching data for genus '" + genus + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
			this.cache.put(genus, hierarchy);
		}
		
		return hierarchy;
	}
	
	private Properties loadHierarchy(final String genus) throws IOException {
		if (genus == null)
			return null;
		
		String hUrlString = (baseUrl + hierarchyQueryBase + URLEncoder.encode(genus, "UTF-8"));
		System.out.println(hUrlString);
		URL hUrl = this.dataProvider.getURL(hUrlString);
		BufferedReader hBr = new BufferedReader(new InputStreamReader(hUrl.openStream(), "UTF-8"));
		QueriableAnnotation hierarchyDoc = SgmlDocumentReader.readDocument(hBr);
		hBr.close();
		QueriableAnnotation[] tables = hierarchyDoc.getAnnotations("table");
		String hLink = null;
		for (int t = 0; t < tables.length; t++) {
			if (!"resultstable".equalsIgnoreCase((String) tables[t].getAttribute("class")))
				continue;
			QueriableAnnotation[] rows = tables[t].getAnnotations("tr");
			for (int r = 0; r < rows.length; r++) {
				if (!genus.equalsIgnoreCase(rows[r].firstValue()))
					continue;
				QueriableAnnotation[] links = rows[r].getAnnotations("a");
				for (int l = 0; l < links.length; l++)
					if (genus.equalsIgnoreCase(links[l].getValue())) {
						hLink = ((String) links[l].getAttribute("href"));
						break;
					}
				if (hLink != null)
					break;
			}
			if (hLink != null)
				break;
		}
		System.out.println(hLink);
		if (hLink == null)
			return null;
		
		hUrlString = (baseUrl + hLink);
		System.out.println(hUrlString);
		hUrl = this.dataProvider.getURL(hUrlString);
		hBr = new BufferedReader(new InputStreamReader(hUrl.openStream(), "UTF-8"));
		hierarchyDoc = SgmlDocumentReader.readDocument(hBr);
		hBr.close();
		tables = hierarchyDoc.getAnnotations("table");
		for (int t = 0; t < tables.length; t++) {
			if ("portal-columns".equalsIgnoreCase((String) tables[t].getAttribute("id")))
				continue;
			int tableScore = 0;
			if (TokenSequenceUtils.indexOf(tables[t], "Kingdom") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Phylum") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Class") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Order") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Family") != -1)
				tableScore++;
			if (tableScore < 4)
				continue;
			Properties hierarchy = new Properties();
			QueriableAnnotation[] divs = tables[t].getAnnotations("div");
			for (int d = 0; d < divs.length; d++) {
				String epithet = divs[d].valueAt(0);
				String rank = divs[d].valueAt(2);
				hierarchy.setProperty(rank, epithet);
			}
			return hierarchy;
		}
		
		//	TODO figure out what to do with synonyms
		return null;
		
//		String urlString = (baseUrl + queryBase + URLEncoder.encode(genus, "UTF-8"));
//		System.out.println(urlString);
//		URL url = this.dataProvider.getURL(urlString);
//		InputStream in = url.openStream();
//		final ArrayList links = new ArrayList();
//		parser.stream(in, new TokenReceiver() {
//			String lastLink = null;
//			public void close() throws IOException {}
//			public void storeToken(String token, int treeDepth) throws IOException {
//				if (token.trim().length() == 0)
//					return;
////				System.out.println(token);
//				if (grammar.isTag(token)) {
//					if ("a".equals(grammar.getType(token)) && !grammar.isEndTag(token)) {
//						TreeNodeAttributeSet tokenAttributes = TreeNodeAttributeSet.getTagAttributes(token, grammar);
////						if (tokenAttributes == null)
////							this.lastLink = null;
////						else this.lastLink = tokenAttributes.getAttribute("href");
////						this.lastLink = tokenAttributes.getAttribute("href");
//						this.lastLink = tokenAttributes.getAttribute("href");
//					}
//				}
////				else if (genus.equals(token.trim())) {
//////					dataLink[0] = this.lastLink;
////				}
//				else if ((this.lastLink != null) && this.lastLink.matches("\\/details\\.html?\\?lsid\\=[1-9][0-9]*")) {
//					System.out.println(token.trim() + " - " + this.lastLink);
//					links.add(this.lastLink);
//					this.lastLink = null;
//				}
//			}
//		});
//		in.close();
//		
//		for (int l = 0; l < links.size();l++) {
//			String link = ((String) links.get(l));
//			while (link.startsWith("/"))
//				link = link.substring(1);
//			urlString = (baseUrl + link);
//			System.out.println(urlString);
//			url = this.dataProvider.getURL(urlString);
//			in = url.openStream();
//			parser.stream(in, new TokenReceiver() {
//				String lastLink = null;
//				boolean lastWasTaxon = false;
//				public void close() throws IOException {}
//				public void storeToken(String token, int treeDepth) throws IOException {
//					if (token.trim().length() == 0)
//						return;
////					System.out.println(token);
//					if (grammar.isTag(token)) {
//						if ("a".equals(grammar.getType(token)) && !grammar.isEndTag(token)) {
//							TreeNodeAttributeSet tokenAttributes = TreeNodeAttributeSet.getTagAttributes(token, grammar);
////							if (tokenAttributes == null)
////								this.lastLink = null;
////							else this.lastLink = tokenAttributes.getAttribute("href");
////							this.lastLink = tokenAttributes.getAttribute("href");
//							this.lastLink = tokenAttributes.getAttribute("href");
//						}
//					}
////					else if (genus.equals(token.trim())) {
//////						dataLink[0] = this.lastLink;
////					}
//					else if ((this.lastLink != null) && this.lastLink.matches("query\\.html?\\?searchType\\=tree\\&(amp\\;)?q\\=[A-Z][a-z]+")) {
//						System.out.println(token.trim() + " - " + this.lastLink);
//						this.lastLink = null;
//						this.lastWasTaxon = true;
//					}
//					else if (this.lastWasTaxon && token.matches("\\s*\\([A-Z][a-z]+\\)\\s*")) {
//						token = token.trim();
//						token = token.substring(1, (token.length() - 1));
//						System.out.println("Rank: " + token);
//						this.lastWasTaxon = false;
//					}
//				}
//				//<div style="white-space:nowrap;margin-left:0px;"><a href="query.htm?searchType=tree&amp;q=Animalia">Animalia</a> (Kingdom)</div>
//			});
//			in.close();
//		}
//		return null;
//		
//		final InputStream listIn = listUrl.openStream();
//		final String[] dataLink = {null};
//		InputStream filterListIn = new InputStream() {
//			public int read() throws IOException {
//				if (dataLink[0] == null)
//					return listIn.read();
//				else return -1;
//			}
//		};
//		parser.stream(filterListIn, new TokenReceiver() {
//			String lastLink = null;
//			public void close() throws IOException {}
//			public void storeToken(String token, int treeDepth) throws IOException {
//				if (grammar.isTag(token)) {
//					if ("a".equals(grammar.getType(token)) && !grammar.isEndTag(token)) {
//						TreeNodeAttributeSet tokenAttributes = TreeNodeAttributeSet.getTagAttributes(token, grammar);
////						if (tokenAttributes == null)
////							this.lastLink = null;
////						else this.lastLink = tokenAttributes.getAttribute("href");
//						this.lastLink = tokenAttributes.getAttribute("href");
//					}
//				}
//				else if (genus.equals(token.trim()))
//					dataLink[0] = this.lastLink;
//			}
//		});
//		listIn.close();
//		
//		if (dataLink[0] == null) {
//			System.out.println("Hierarchy for '" + genus + "' not found.");
//			return null;
//		}
//		
//		//next?v_tsn=154211&taxa=&p_king=every&p_string=containing&p_ifx=cbif&p_lang=
//		String itisGenusNumber = dataLink[0];
//		try {
//			itisGenusNumber = itisGenusNumber.substring(itisGenusNumber.indexOf('=') + 1);
//			itisGenusNumber = itisGenusNumber.substring(0, itisGenusNumber.indexOf('&'));
//		}
//		catch (Exception e) {
//			System.out.println("Error parsing number for genus '" + genus + "' from " + dataLink[0] + ": " + e.getMessage());
//			e.printStackTrace(System.out);
//			return null;
//		}
//		
//		URL dataUrl = this.dataProvider.getURL(baseUrl + dataBase + itisGenusNumber);
//		TreeNode dataRoot = parser.parse(dataUrl.openStream());
//		TreeNode[] hierarchyNodes = TreeTools.getAllNodesOfType(dataRoot, "parent");
//		Properties hierarchy = new Properties();
//		for (int h = 0; h < hierarchyNodes.length; h++) {
//			TreeNode rankNode = hierarchyNodes[h].getChildNode("rank", 0);
//			TreeNode epithetNode = hierarchyNodes[h].getChildNode("concatenatedname", 0);
//			if ((rankNode == null) || (epithetNode == null))
//				continue;
//			String rank = rankNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0).getNodeValue();
//			String epithet = epithetNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0).getNodeValue();
//			hierarchy.setProperty(rank, epithet);
//		}
//		return hierarchy;
	}
	//http://www.organismnames.com/advancedquery.htm?Submit.x=0&Submit.y=0&searchType=complete&so=a0&q=M*+dentatum
//	private static String baseUrl = "http://www.organismnames.com/";
//	private static String queryBase = "advancedquery.htm?Submit.x=0&Submit.y=0&searchType=complete&so=a0&q=";
//	private static String dataBase = "taxa_xml.upwards?p_type=y&p_lang=&p_tsn=";
	private static String baseUrl = "http://www.organismnames.com/";
	private static String hierarchyQueryBase = "query.htm?searchType=simple&so=a0&pp=100&q=";
	private static String childrenQueryBase = "query.htm?searchType=simple&so=a0&pp=10000&q=";
	
	private static final Grammar grammar = new Html();
	private static final Parser parser = new Parser(grammar);
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
//		System.getProperties().put("proxySet", "true");
//		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
//		System.getProperties().put("proxyPort", "3128");
		
		IonHigherHierarchyProvider ionHhp = new IonHigherHierarchyProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/IonHierarchyData/")));
		
		String testGenus = "Chromis";
		Properties hierarchy = ionHhp.getHierarchy(testGenus, true);
		if (hierarchy == null)
			System.out.println("Hierarchy for '" + testGenus + " not found'");
		else for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
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