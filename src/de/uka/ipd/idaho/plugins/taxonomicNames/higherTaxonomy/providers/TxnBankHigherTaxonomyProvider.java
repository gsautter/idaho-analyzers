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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Higher taxonomy provider backed by Taxon Name Bank.
 * 
 * @author sautter
 */
public class TxnBankHigherTaxonomyProvider extends HigherTaxonomyProvider {
	
	private String txnNodeUrl = "http://plazi2.cs.umb.edu:8080/TnuBank/txn/find?";
	
	private static Properties rankMappings = new Properties();
	static {
		rankMappings.setProperty("kingdom", KINGDOM_ATTRIBUTE);
		rankMappings.setProperty("phylum", PHYLUM_ATTRIBUTE);
		rankMappings.setProperty("class", CLASS_ATTRIBUTE);
		rankMappings.setProperty("order", ORDER_ATTRIBUTE);
		rankMappings.setProperty("family", FAMILY_ATTRIBUTE);
		rankMappings.setProperty("subfamily", SUBFAMILY_ATTRIBUTE);
		rankMappings.setProperty("tribe", TRIBE_ATTRIBUTE);
		rankMappings.setProperty("subtribe", SUBTRIBE_ATTRIBUTE);
		rankMappings.setProperty("genus", GENUS_ATTRIBUTE);
		rankMappings.setProperty("subgenus", SUBGENUS_ATTRIBUTE);
		rankMappings.setProperty("species", SPECIES_ATTRIBUTE);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "TxnBank";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#init()
	 */
	protected void init() {
		if (this.dataProvider.isDataAvailable("config.cnfg")) try {
			Reader configIn = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("config.cnfg")));
			StringVector configLines = StringVector.loadList(configIn);
			configIn.close();
			for (int c = 0; c < configLines.size(); c++) {
				String cl = configLines.get(c).trim();
				if ((cl.length() == 0) || cl.startsWith("//"))
					continue;
				if (cl.startsWith("txnBankNodeUrl=")) {
					this.txnNodeUrl = cl.substring("txnBankNodeUrl=".length()).trim();
					if (!this.txnNodeUrl.endsWith("?"))
						this.txnNodeUrl = (this.txnNodeUrl + "?");
				}
			}
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	protected Properties loadHierarchy(String epithet, final String rank) throws IOException {
		if ((epithet == null) || (rank == null))
			return null;
		
		//	get child rank
		final String childRank = childRanks.getProperty(rank);
		
		//	collect hierarchies individually first
		final TreeMap hierarchiesByIdString = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		//	stream data from TxnBank
		URL dataUrl = this.dataProvider.getURL(this.txnNodeUrl + rank + "=" + epithet);
		HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
		if (0 < this.lookupTimeout)
			dataCon.setReadTimeout(this.lookupTimeout);
		Reader dataReader = new BufferedReader(new InputStreamReader(dataCon.getInputStream(), "UTF-8"));
		parser.stream(dataReader, new TokenReceiver() {
			private Properties data = null;
			private String dataRank = null;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					String type = grammar.getType(token);
					if (TAXONOMIC_NAME_ANNOTATION_TYPE.equals(type)) {
						if (grammar.isEndTag(token)) {
							if (this.data != null) {
								String hierarchyId = this.getHierarchyIdString(this.data);
								Hierarchy hierarchy = ((Hierarchy) hierarchiesByIdString.get(hierarchyId));
								if (hierarchy == null) {
									hierarchy = new Hierarchy(this.data, rank, childRank);
									hierarchiesByIdString.put(hierarchyId, hierarchy);
								}
								hierarchy.addChildEpithet(this.data.getProperty(childRank));
							}
							this.data = null;
						}
						else this.data = new Properties();
					}
					else if (this.data != null) {
						if (grammar.isEndTag(token))
							this.dataRank = null;
						else this.dataRank = rankMappings.getProperty(type.substring(type.indexOf(':') + 1).toLowerCase());
					}
				}
				else if (this.dataRank != null)
					this.data.setProperty(this.dataRank, grammar.unescape(token.trim()));
			}
			private String getHierarchyIdString(Properties hierarchy) {
				StringBuffer sb = new StringBuffer();
				for (int r = 0; r < rankNames.length; r++) {
					sb.append(hierarchy.getProperty(rankNames[r], "--"));
					if (rankNames[r].equals(rank))
						break;
				}
				return sb.toString();
			}
			public void close() throws IOException {}
		});
		dataReader.close();
		dataCon.disconnect();
		
		//	finish hierarchies
		LinkedList hierarchyList = new LinkedList();
		for (Iterator hidit = hierarchiesByIdString.keySet().iterator(); hidit.hasNext();) {
			String hierarchyId = ((String) hidit.next());
			Hierarchy hierarchy = ((Hierarchy) hierarchiesByIdString.get(hierarchyId));
			hierarchy.storeChildEpithets();
			hierarchyList.addLast(hierarchy);
		}
		
		//	aggregate hierarchies
		Properties[] hierarchies = aggregateHierarchies(((Properties[]) hierarchyList.toArray(new Properties[hierarchyList.size()])), rank);
		
		//	bundle and return hierarchies
		return bundleHierarchies(hierarchies);
	}
	
	private static final Grammar grammar = new StandardGrammar();
	private static final Parser parser = new Parser(grammar);
	
	private static class Hierarchy extends Properties {
		String childRank;
		TreeSet childEpithets = null;
		Hierarchy(Properties data, String rank, String childRank) {
			for (int r = 0; r < rankNames.length; r++) {
				String epithet = data.getProperty(rankNames[r]);
				if (epithet != null)
					this.setProperty(rankNames[r], epithet);
				if (rankNames[r].equals(rank))
					break;
			}
			this.childRank = childRank;
		}
		void addChildEpithet(String childEpithet) {
			if (childEpithet == null)
				return;
			if (this.childEpithets == null)
				this.childEpithets = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			this.childEpithets.add(childEpithet);
		}
		void storeChildEpithets() {
			if ((this.childRank == null) || (this.childEpithets == null))
				return;
			StringBuffer childEpithets = new StringBuffer();
			for (Iterator ceit = this.childEpithets.iterator(); ceit.hasNext();) {
				String childEpithet = ((String) ceit.next());
				if (childEpithets.length() != 0)
					childEpithets.append(";");
				childEpithets.append(childEpithet);
			}
			this.setProperty(this.childRank, childEpithets.toString());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TxnBankHigherTaxonomyProvider txnHhp = new TxnBankHigherTaxonomyProvider();
		txnHhp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/TxnHierarchyData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
		
		String testEpithet = "Formica";
		String testRank = GENUS_ATTRIBUTE;
		Properties hierarchy = txnHhp.getHierarchy(testEpithet, testRank, true);
		for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1)
				break;
			System.out.println(rank + ": " + hierarchy.getProperty(rank));
			if (!rit.hasNext())
				return;
		}
		for (int r = 0;; r++) {
			String prefix = (r + ".");
			boolean prefixEmpty = true;
			for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
				String rank = ((String) rit.next());
				if (!rank.startsWith(prefix))
					continue;
				prefixEmpty = false;
				System.out.println(rank + ": " + hierarchy.getProperty(rank));
			}
			if (prefixEmpty)
				break;
		}
	}
}