/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;

/**
 * Higher taxonomy provider backed by IPNI.
 * 
 * @author sautter
 */
public class IpniHigherTaxonomyProvider extends HigherTaxonomyProvider {
	
	private static class TaxonTreeNode extends TreeMap {
		final TaxonTreeNode parent;
		final String epithet;
		final String rank;
		Properties intermediateRankEpithets = null;
		TaxonTreeNode(TaxonTreeNode parent, String epithet, String rank) {
			super(String.CASE_INSENSITIVE_ORDER);
			this.parent = parent;
			this.epithet = epithet;
			this.rank = rank;
		}
	}
	
	private TaxonTreeNode taxonHierarchyRoot = new TaxonTreeNode(null, "Plantae", KINGDOM_ATTRIBUTE);
	private TreeMap classNodeIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeMap orderNodeIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeMap familyNodeIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeMap genusNodeIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "IPNI";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#init()
	 */
	protected void init() {
		try {
			InputStream htin;
			if (this.dataProvider.isDataAvailable("IPNI.higherTaxonomy.xml"))
				htin = this.dataProvider.getInputStream("IPNI.higherTaxonomy.xml");
			else {
				String dataPath = IpniHigherTaxonomyProvider.class.getName();
				dataPath = dataPath.substring(0, dataPath.lastIndexOf('.'));
				dataPath = dataPath.replaceAll("\\.", "/");
				htin = IpniHigherTaxonomyProvider.class.getClassLoader().getResourceAsStream(dataPath + "/IPNI.higherTaxonomy.xml");
			}
			BufferedReader htr = new BufferedReader(new InputStreamReader(htin, "UTF-8"));
			parser.stream(htr, new TokenReceiver() {
				TaxonTreeNode node;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (!grammar.isTag(token))
						return;
					String rank = grammar.getType(token);
					if (SPECIES_ATTRIBUTE.equals(rank))
						return;
					if (grammar.isEndTag(token)) {
						this.node = this.node.parent;
						return;
					}
					if (KINGDOM_ATTRIBUTE.equals(rank)) {
						this.node = taxonHierarchyRoot;
						return;
					}
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
					TaxonTreeNode childNode = new TaxonTreeNode(this.node, tnas.getAttribute("name", "UNDEFINED"), rank);
					this.node.put(childNode.epithet, childNode);
					if (!grammar.isSingularTag(token))
						this.node = childNode;
					String synonymStr = tnas.getAttribute("synonyms");
					String[] synonyms = ((synonymStr == null) ? new String[0] : synonymStr.trim().split("\\s*\\;\\s*"));
					if (CLASS_ATTRIBUTE.equals(childNode.rank)) {
						classNodeIndex.put(childNode.epithet, childNode);
						for (int s = 0; s < synonyms.length; s++)
							classNodeIndex.put(synonyms[s], childNode);
					}
					else if (ORDER_ATTRIBUTE.equals(childNode.rank)) {
						orderNodeIndex.put(childNode.epithet, childNode);
						for (int s = 0; s < synonyms.length; s++)
							orderNodeIndex.put(synonyms[s], childNode);
					}
					else if (FAMILY_ATTRIBUTE.equals(childNode.rank)) {
						familyNodeIndex.put(childNode.epithet, childNode);
						for (int s = 0; s < synonyms.length; s++)
							familyNodeIndex.put(synonyms[s], childNode);
					}
					else if (GENUS_ATTRIBUTE.equals(childNode.rank)) {
						genusNodeIndex.put(childNode.epithet, childNode);
						for (int s = 0; s < synonyms.length; s++)
							genusNodeIndex.put(synonyms[s], childNode);
					}
					String[] ans = tnas.getAttributeNames();
					for (int a = 0; a < ans.length; a++) {
						if ("name".equals(ans[a]) || "synonyms".equals(ans[a]))
							continue;
						if (childNode.intermediateRankEpithets == null)
							childNode.intermediateRankEpithets = new Properties();
						childNode.intermediateRankEpithets.setProperty(ans[a], tnas.getAttribute(ans[a]));
					}
				}
				public void close() throws IOException {}
			});
			htr.close();
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	protected Properties loadHierarchy(String epithet, String rank) throws IOException {
		if ((epithet == null) || (rank == null))
			return null;
		Properties hierarchy = this.doLoadHierarchy(epithet, rank);
		if (hierarchy != null)
			hierarchy.setProperty(RANK_ATTRIBUTE, rank);
		return hierarchy;
	}
	private Properties doLoadHierarchy(final String epithet, String rank) throws IOException {
		if (CLASS_ATTRIBUTE.equals(rank) && this.classNodeIndex.containsKey(epithet)) {
			System.out.println("IPNI: Index hit for " + rank + " '" + epithet + "'");
			return this.fillHierarchy(((TaxonTreeNode) this.classNodeIndex.get(epithet)), new Properties());
		}
		else if (ORDER_ATTRIBUTE.equals(rank) && this.orderNodeIndex.containsKey(epithet)) {
			System.out.println("IPNI: Index hit for " + rank + " '" + epithet + "'");
			return this.fillHierarchy(((TaxonTreeNode) this.orderNodeIndex.get(epithet)), new Properties());
		}
		else if (FAMILY_ATTRIBUTE.equals(rank) && this.familyNodeIndex.containsKey(epithet)) {
			System.out.println("IPNI: Index hit for " + rank + " '" + epithet + "'");
			return this.fillHierarchy(((TaxonTreeNode) this.familyNodeIndex.get(epithet)), new Properties());
		}
		
		if (!GENUS_ATTRIBUTE.equals(rank))
			return null;
		
		if (this.genusNodeIndex.containsKey(epithet)) {
			System.out.println("IPNI: Index hit for genus '" + epithet + "'");
			return this.fillHierarchy(((TaxonTreeNode) this.genusNodeIndex.get(epithet)), new Properties());
		}
		
		URL listUrl = this.dataProvider.getURL(baseUrl + queryBase + epithet);
		HttpURLConnection listCon = ((HttpURLConnection) listUrl.openConnection());
		if (0 < this.lookupTimeout)
			listCon.setReadTimeout(this.lookupTimeout);
		final InputStream listIn = listCon.getInputStream();
		final String[] family = {null};
		InputStream filterListIn = new InputStream() {
			public int read() throws IOException {
				if (family[0] == null)
					return listIn.read();
				else return -1;
			}
		};
		try {
			parser.stream(filterListIn, new TokenReceiver() {
				SgmlDocumentReader resultReader = null;
				public void close() throws IOException {}
				public void storeToken(String token, int treeDepth) throws IOException {
					if (grammar.isTag(token) && "li".equals(grammar.getType(token))) {
						if (grammar.isEndTag(token)) {
							if (this.resultReader != null) {
								this.resultReader.close();
								MutableAnnotation resDoc = this.resultReader.getDocument();
								Annotation[] resFamily = GPath.evaluatePath(resDoc, ("//p[./@class = 'result' and ./a/i/#first = '" + epithet + "']/span[./@class = 'family' and matches(., '[A-Z][a-z]+aceae')]"), null);
								if (resFamily.length != 0) {
									family[0] = resFamily[0].firstValue();
									throw new RuntimeException("GOT FAMILY");
								}
							}
							this.resultReader = null;
						}
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							if ("result".equals(tnas.getAttribute("class")))
								this.resultReader = new SgmlDocumentReader(null, grammar, null, null, null);
						}
					}
					else if (this.resultReader != null)
						this.resultReader.storeToken(token, treeDepth);
				}
			});
		}
		catch (RuntimeException re) {
			if (!"GOT FAMILY".equals(re.getMessage()))
				throw re;
		}
		finally {
			listIn.close();
			listCon.disconnect();
		}
		
		if (family[0] == null) {
			System.out.println("IPNI: Family for genus '" + epithet + "' not found at IPNI.");
			return null;
		}
		System.out.println("IPNI: Got family '" + family[0] + "' for genus '" + epithet + "' from IPNI");
		
		Properties hierarchy = new Properties();
		hierarchy.setProperty(GENUS_ATTRIBUTE, epithet);
		hierarchy.setProperty(FAMILY_ATTRIBUTE, family[0]);
		return (this.familyNodeIndex.containsKey(family[0]) ? this.fillHierarchy(((TaxonTreeNode) this.familyNodeIndex.get(family[0])), hierarchy) : hierarchy);
	}
	
	private Properties fillHierarchy(TaxonTreeNode node, Properties hierarchy) {
		for (; node != null; node = node.parent) {
			if ("UNDEFINED".equals(node.epithet))
				continue;
			hierarchy.put(node.rank, node.epithet);
			if (node.intermediateRankEpithets != null)
				hierarchy.putAll(node.intermediateRankEpithets);
		}
		return hierarchy;
	}
	
	//	http://www.ipni.org/ipni/advPlantNameSearch.do?output_format=normal&query_type=by_query&find_genus=Chenopodium
	private static String baseUrl = "http://www.ipni.org/ipni/";
	private static String queryBase = "advPlantNameSearch.do?output_format=normal&query_type=by_query&find_genus=";
	
	private static final Grammar grammar = new Html();
	private static final Parser parser = new Parser(grammar);
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		IpniHigherTaxonomyProvider ipniHhp = new IpniHigherTaxonomyProvider();
		ipniHhp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/IpniHierarchyData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
		
		String testGenus = "Ypresiomyrma";//"Chenopodium";
		Properties hierarchy = ipniHhp.getHierarchy(testGenus, true);
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