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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * Higher taxonomy provider backed by GBIF.
 * 
 * @author sautter
 */
public class GbifHigherTaxonomyProvider extends HigherTaxonomyProvider {
	private TaxonomicRankSystem rankSystem;
	private TreeMap ranksByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private Rank[] primaryRanks;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "GBIF";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#init()
	 */
	protected void init() {
		this.rankSystem = TaxonomicRankSystem.getRankSystem(null);
		Rank[] ranks = this.rankSystem.getRanks();
		for (int r = 0; r < ranks.length; r++)
			this.ranksByName.put(ranks[r].name, ranks[r]);
		this.primaryRanks = new Rank[rankNames.length];
		for (int n = 0; n < rankNames.length; n++)
			this.primaryRanks[n] = ((Rank) this.ranksByName.get(rankNames[n]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	protected Properties loadHierarchy(String epithet, String rank) throws IOException {
		return this.loadHierarchy(epithet, rank, null);
	}
	
	private Map parentRecursionCache = new LinkedHashMap(32, 09.f,true) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (this.size() > 128);
		}
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	private Properties loadHierarchy(String epithet, String rank, Properties parentRecursionChild) throws IOException {
		if (epithet == null)
			return null;
		
		//	check parent lookup cache if applicable
		if (parentRecursionChild != null)
			synchronized (this.parentRecursionCache) {
				if (this.parentRecursionCache.containsKey(epithet.toLowerCase()))
					return ((Properties) this.parentRecursionCache.get(epithet.toLowerCase()));
			}
		
		//	get data from GBIF
		URL dataUrl = this.dataProvider.getURL(baseUrl + epithet);
		HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
		if (0 < this.lookupTimeout)
			dataCon.setReadTimeout(this.lookupTimeout);
		Reader dataReader = new BufferedReader(new InputStreamReader(dataCon.getInputStream(), "UTF-8"));
		Object dataObj = JsonParser.parseJson(dataReader);
		dataReader.close();
		dataCon.disconnect();
		if (!(dataObj instanceof Map)) {
			System.out.println("Strange result: " + dataObj);
			return null;
		}
		
		//	parse result
		List resultList = JsonParser.getArray(((Map) dataObj), "results");
		if ((resultList == null) || resultList.isEmpty()) {
			System.out.println("Empty result list");
			return null;
		}
		
		//	extract taxonomic hierarchies
		ArrayList hierarchies = new ArrayList();
		for (int r = 0; r < resultList.size(); r++) {
			Map result = JsonParser.getObject(resultList, r);
			if (result == null)
				continue;
			
			//	check name type and rank
			String dNameType = JsonParser.getString(result, "nameType");
			if (!"WELLFORMED".equalsIgnoreCase(dNameType) && !"SCIENTIFIC".equalsIgnoreCase(dNameType))
				continue;
			String dRankName = JsonParser.getString(result, "rank");
			if (dRankName == null)
				continue;
			if ((rank != null) && !rank.equalsIgnoreCase(dRankName))
				continue;
			Rank dRank = ((Rank) this.ranksByName.get(dRankName));
			if (dRank == null)
				continue;
			if (dRank.getRelativeSignificance() > this.primaryRanks[this.primaryRanks.length-1].getRelativeSignificance())
				continue;
			
			//	get higher primary ranks
			Properties hierarchy = new Properties();
			Properties rHierarchy = new Properties();
			TreeSet rEpithets = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int n = 0; n < rankNames.length; n++) {
				
				//	we have gotten to the data indicated rank, set the rank and we're done
				if ((rank == null) && (this.primaryRanks[n].getRelativeSignificance() > dRank.getRelativeSignificance())) {
					hierarchy.setProperty(dRank.name, epithet);
					hierarchy.setProperty(RANK_ATTRIBUTE, dRank.name);
					break;
				}
				
				//	test primary rank (allow substituting epithet from child lookup if in parent recursion)
				String dEpithet = JsonParser.getString(result, rankNames[n]);
				if ((dEpithet == null) || dEpithet.toLowerCase().matches("not\\s+assigned")) {
					if (parentRecursionChild != null) // use child a substitute on parent recursion
						dEpithet = parentRecursionChild.getProperty(rankNames[n]);
				}
				if ((dEpithet == null) || dEpithet.toLowerCase().matches("not\\s+assigned")) {
					hierarchy = null;
					break;
				}
				
				//	remember current primary rank epithet
				hierarchy.setProperty(rankNames[n], dEpithet);
				rHierarchy.setProperty(rankNames[n], dEpithet);
				rEpithets.add(dEpithet);
				
				//	we're arrived at data indicated rank, we're done
				if (rankNames[n].equals(dRank.name)) {
					hierarchy.setProperty(RANK_ATTRIBUTE, dRank.name);
					break;
				}
			}
			
			//	some primary epithet missing
			if (hierarchy == null) {
				
				//	get parent epithet for recursive lookup
				String pEpithet = JsonParser.getString(result, "parent");
				if (pEpithet == null)
					continue;
				
				// no use recursing with higher-up parent if intermediate primary rank missing
				if (rEpithets.contains(pEpithet))
					continue;
				
				// no use recursing with identical epithet
				if (pEpithet.equalsIgnoreCase(epithet))
					continue;
				
				//	do parent recursion
				System.out.println(this.getDataSourceName() + ": recursing with non-primary rank parent epithet " + pEpithet);
				Properties pHierarchy = this.loadHierarchy(pEpithet, null, rHierarchy);
				if (pHierarchy == null)
					continue;
				
				//	cache parent hierarchy
				synchronized (this.parentRecursionCache) {
					this.parentRecursionCache.put(epithet.toLowerCase(), pHierarchy);
				}
				
				//	switch to reserve hierarchy
				hierarchy = rHierarchy;
				
				//	copy over primary ranks from parent hierarchy
				for (int n = 0; n < rankNames.length; n++) {
					if (pHierarchy.containsKey(rankNames[n]))
						hierarchy.setProperty(rankNames[n], pHierarchy.getProperty(rankNames[n]));
				}
				
				//	add child and rank
				hierarchy.setProperty(dRank.name, epithet);
				hierarchy.setProperty(RANK_ATTRIBUTE, dRank.name);
			}
			
			//	no need to look any further if we're in a parent recursion
			if (parentRecursionChild != null)
				return hierarchy;
			
			//	store hierarchy and parse authority
			hierarchies.add(hierarchy);
			String dAuthority = JsonParser.getString(result, "authorship");
			if (dAuthority != null) {
				hierarchy.setProperty(AUTHORITY_ATTRIBUTE, dAuthority.trim());
				if (dAuthority.matches(".*[12][0-9]{3}")) {
					String dAuthorityName = dAuthority.substring(0, (dAuthority.length() - 4)).trim();
					while (dAuthorityName.endsWith(","))
						dAuthorityName = dAuthorityName.substring(0, (dAuthorityName.length() - ",".length())).trim();
					hierarchy.setProperty(AUTHORITY_NAME_ATTRIBUTE, dAuthorityName);
					hierarchy.setProperty(AUTHORITY_YEAR_ATTRIBUTE, dAuthority.substring(dAuthority.length() - 4).trim());
				}
				else hierarchy.setProperty(AUTHORITY_NAME_ATTRIBUTE, dAuthority.trim());
			}
		}
		
		//	do we have a clear result already?
		if (hierarchies.isEmpty())
			return null;
		else if (hierarchies.size() == 1)
			return ((Properties) hierarchies.get(0));
		
		//	eliminate duplicates
		for (int h = 0; h < hierarchies.size(); h++)
			for (int ch = (h+1); ch < hierarchies.size(); ch++) {
				if (this.isSubsetOf(((Properties) hierarchies.get(h)), ((Properties) hierarchies.get(ch))))
					hierarchies.remove(ch--);
				else if (this.isSubsetOf(((Properties) hierarchies.get(ch)), ((Properties) hierarchies.get(h)))) {
					hierarchies.set(h, hierarchies.get(ch));
					hierarchies.remove(ch--);
				}
			}
		
		//	do we have a clear result now?
		if (hierarchies.isEmpty())
			return null;
		else if (hierarchies.size() == 1)
			return ((Properties) hierarchies.get(0));
		
		//	bundle results otherwise
		Properties hierarchy = new Properties();
		for (int h = 0; h < hierarchies.size(); h++) {
			Properties resultHierarchy = ((Properties) hierarchies.get(h));
			for (Iterator rit = resultHierarchy.keySet().iterator(); rit.hasNext();) {
				String dRank = ((String) rit.next());
				String dEpithet = resultHierarchy.getProperty(dRank);
				hierarchy.setProperty((h + "." + dRank), dEpithet);
			}
		}
		
		return hierarchy;
	}
	
	private boolean isSubsetOf(Properties full, Properties subset) {
		for (int r = 0; r < rankNames.length; r++) {
			String fEpithet = full.getProperty(rankNames[r]);
			String sEpithet = subset.getProperty(rankNames[r]);
			if ((fEpithet == null) && (sEpithet == null))
				continue;
			if (fEpithet == null)
				return false;
			if (!fEpithet.equalsIgnoreCase(sEpithet))
				return false;
		}
		String fAuthorityName = full.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		String sAuthorityName = subset.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		if ((fAuthorityName != null) && (sAuthorityName != null)) {
			if (fAuthorityName.endsWith("."))
				fAuthorityName = fAuthorityName.substring(0, (fAuthorityName.length() - ".".length())).trim();
			if (sAuthorityName.endsWith("."))
				sAuthorityName = sAuthorityName.substring(0, (sAuthorityName.length() - ".".length())).trim();
			if (!fAuthorityName.equalsIgnoreCase(sAuthorityName) && !StringUtils.isAbbreviationOf(fAuthorityName, sAuthorityName, false))
				return false;
		}
		else if (fAuthorityName == null)
			return false;
		String fAuthorityYear = full.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		String sAuthorityYear = subset.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		if ((fAuthorityYear != null) && (sAuthorityYear != null) && !fAuthorityYear.equals(sAuthorityYear))
			return false;
		else if ((fAuthorityYear == null) && (sAuthorityYear != null))
			return false;
		return true;
	}
	
	private static String baseUrl = "http://api.gbif.org/v1/species?limit=100&name=";
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		GbifHigherTaxonomyProvider gbifHhp = new GbifHigherTaxonomyProvider();
		gbifHhp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/HigherTaxonomyData/GbifData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
			public boolean isDataAvailable(String dataName) {
				return false;
			}
		});
		
		/* some test cases:
		 * 
		 * EXTANT:
		 * - tribe Polyrhachidini
		 * - tribe Prenolepidii
		 * - tribe Brachymyrmicini
		 * - genus Dinomyrmex
		 * - genus Myrmacantha
		 * - genus Myrmorhachis
		 * - genus Dolophra
		 * 
		 * FOSSILE:
		 * - genus Curtipalpulus
		 * - genus Eoleptocerites
		 * - genus Eurytarsites
		 * - genus Fushuniformica
		 * - genus Huaxiaformica
		 * - genus Leptogasteritus
		 * - genus Liaoformica
		 * - genus Longiformica
		 * - genus Magnogasterites
		 * - genus Orbicapitia
		 * - genus Ovalicapito
		 * - genus Ovaligastrula
		 * - genus Sinoformica
		 * - genus Sinotenuicapito
		 */
		
		String testEpithet = "Dinomyrmex";//"Hymenoptera";//"Braunsia";//"Chenopodium";
		String testRank = GENUS_ATTRIBUTE;
		Properties hierarchy = gbifHhp.getHierarchy(testEpithet, testRank, true);
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