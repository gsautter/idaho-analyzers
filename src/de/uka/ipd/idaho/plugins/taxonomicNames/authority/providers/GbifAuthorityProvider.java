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
package de.uka.ipd.idaho.plugins.taxonomicNames.authority.providers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.plugins.taxonomicNames.authority.AuthorityProvider;

/**
 * Authority provider backed by GBIF.
 * 
 * @author sautter
 */
public class GbifAuthorityProvider extends AuthorityProvider {
	private TaxonomicRankSystem rankSystem;
	private TreeMap ranksByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private Rank speciesRank;
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
		for (int r = 0; r < ranks.length; r++) {
			this.ranksByName.put(ranks[r].name, ranks[r]);
			if (SPECIES_ATTRIBUTE.equals(ranks[r].name))
				this.speciesRank = ranks[r];
		}
		this.primaryRanks = new Rank[rankNames.length];
		for (int n = 0; n < rankNames.length; n++)
			this.primaryRanks[n] = ((Rank) this.ranksByName.get(rankNames[n]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.authority.AuthorityProvider#loadAuthority(java.lang.String[], java.lang.String, java.lang.String, int)
	 */
	protected Properties loadAuthority(String[] epithets, String rank, String aName, int aYear) throws IOException {
		if ((epithets == null) || (epithets.length == 0) || (rank == null))
			return null;
		
		//	get data from GBIF
		URL dataUrl = this.dataProvider.getURL(baseUrl + concatEpithets(epithets, "+"));
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
		
		//	extract authorities
		ArrayList authorities = new ArrayList();
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
			if ("INFRASPECIFIC_NAME".equals(dRankName)) {
				Rank lRank = ((Rank) this.ranksByName.get(rank));
				if (lRank == null)
					continue;
				if (lRank.getRelativeSignificance() > this.speciesRank.getRelativeSignificance())
					dRankName = rank;
				else continue;
			}
			if (!rank.equalsIgnoreCase(dRankName))
				continue;
			
			//	match epithets against canonicalName
			String canonicalName = JsonParser.getString(result, "canonicalName");
			if (canonicalName == null)
				continue;
			int lastEpithetEnd = -1;
			String mCanonicalName = canonicalName.toLowerCase();
			for (int e = 0; e < epithets.length; e++) {
				int epithetIndex = mCanonicalName.indexOf(epithets[e].toLowerCase(), lastEpithetEnd);
				if (epithetIndex == -1) {
					mCanonicalName = null;
					break;
				}
				else lastEpithetEnd = (epithetIndex + epithets[e].length());
			}
			if (mCanonicalName == null)
				continue;
			
			//	extract authority
			String dAuthorship = JsonParser.getString(result, "authorship");
			if (dAuthorship == null)
				continue;
			dAuthorship = dAuthorship.trim();
			if (dAuthorship.startsWith("(") && dAuthorship.endsWith(")") && (dAuthorship.indexOf('(') == dAuthorship.lastIndexOf('(')) && (dAuthorship.indexOf(')') == dAuthorship.lastIndexOf(')')))
				dAuthorship = dAuthorship.substring("(".length(), (dAuthorship.length() - ")".length()));
			if (dAuthorship.startsWith("[") && dAuthorship.endsWith("]") && (dAuthorship.indexOf('[') == dAuthorship.lastIndexOf('[')) && (dAuthorship.indexOf(']') == dAuthorship.lastIndexOf(']')))
				dAuthorship = dAuthorship.substring("[".length(), (dAuthorship.length() - "]".length()));
			String authorityString = null;
			String authorityName = null;
			String authorityYear = null;
			if (dAuthorship.matches(".*[12][0-9]{3}")) {
				authorityString = dAuthorship;
				String dAuthorityName = dAuthorship.substring(0, (dAuthorship.length() - 4)).trim();
				while (dAuthorityName.endsWith(","))
					dAuthorityName = dAuthorityName.substring(0, (dAuthorityName.length() - ",".length())).trim();
				authorityName = dAuthorityName;
				authorityYear = dAuthorship.substring(dAuthorship.length() - 4).trim();
			}
			else authorityName = dAuthorship;
			
			//	try publishedIn and accordingTo as fallback for year
			if (authorityYear == null)
				authorityYear = this.getYearFromString(JsonParser.getString(result, "publishedIn"));
//			if (authorityYear == null) TOO DANGEROUS, SOURCE MIGHT BE WAY YOUNGER
//				authorityYear = this.getYearFromString(JsonParser.getString(result, "accordingTo"));
			
			//	do we have a year, and does it fit?
			if (authorityYear == null)
				continue;
			if ((aYear > 0) && !authorityYear.equals("" + aYear))
				continue;
			
			//	assemble overall authority string if we got year from fallback source
			if (authorityString == null)
				authorityString = (authorityName + ", " + authorityYear);
			
			//	truncate authority name at any embedded closing parenthesis (we want the _current_ author name there)
			authorityName = getCurrentAuthorityName(authorityName);
			
			//	finally ...
			Properties authority = new Properties();
			authority.setProperty(AUTHORITY_ATTRIBUTE, authorityString);
			authority.setProperty(AUTHORITY_NAME_ATTRIBUTE, authorityName);
			authority.setProperty(AUTHORITY_YEAR_ATTRIBUTE, authorityYear);
			authorities.add(authority);
		}
		
		//	aggregate and return results
		return aggregateLookupResults(authorities);
	}
	
	private static Pattern yearPattern = Pattern.compile("(1[789][0-9]{2})|(20[0-9]{2})");
	private String getYearFromString(String str) {
		if (str == null)
			return null;
		String year = null;
		for (Matcher m = yearPattern.matcher(str); m.find();)
			year = m.group();
		return year;
	}
	
	private static String baseUrl = "http://api.gbif.org/v1/species?limit=100&name=";
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		GbifAuthorityProvider gbifAp = new GbifAuthorityProvider();
		gbifAp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AuthorityAugmenterData/GbifData/")) {
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
		 * - tribe Formicini
		 * - species Orchis maculata
		 * - subspecies Dactylorchis maculata arduennensis
		 * - species Catapaguroides pectinipes
		 */
		String testEpithet = "Catapaguroides pectinipes";//"Hymenoptera";//"Braunsia";//"Chenopodium";
		String testRank = SPECIES_ATTRIBUTE;
		Properties hierarchy = gbifAp.getAuthority(testEpithet.split("\\s+"), testRank, null, -1, true);
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