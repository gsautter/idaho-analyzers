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
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.taxonomicNames.authority.AuthorityProvider;

/**
 * Authority provider backed by Catalog of Life.
 * 
 * @author sautter
 */
public class ColAuthorityProvider extends AuthorityProvider {
//	
//	private static Properties rankNameNormalizer = new Properties();
//	static {
//		rankNameNormalizer.setProperty("Kingdom", KINGDOM_ATTRIBUTE);
//		rankNameNormalizer.setProperty("Phylum", PHYLUM_ATTRIBUTE);
//		rankNameNormalizer.setProperty("Division", PHYLUM_ATTRIBUTE);
//		rankNameNormalizer.setProperty("Class", CLASS_ATTRIBUTE);
//		rankNameNormalizer.setProperty("Order", ORDER_ATTRIBUTE);
//		rankNameNormalizer.setProperty("Family", FAMILY_ATTRIBUTE);
//		rankNameNormalizer.setProperty("Tribe", TRIBE_ATTRIBUTE);
//	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "CoL";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.authority.AuthorityProvider#loadAuthority(java.lang.String[], java.lang.String, java.lang.String, int)
	 */
	protected Properties loadAuthority(String[] epithets, String rank, String aName, int aYear) throws IOException {
		if ((epithets == null) || (rank == null))
			return null;
		
		URL dataUrl = this.dataProvider.getURL(baseUrl + concatEpithets(epithets, "+"));
		HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
		if (0 < this.lookupTimeout)
			dataCon.setReadTimeout(this.lookupTimeout);
		Reader dataReader = new BufferedReader(new InputStreamReader(dataCon.getInputStream(), "UTF-8"));
		MutableAnnotation data = SgmlDocumentReader.readDocument(dataReader);
		dataReader.close();
		dataCon.disconnect();
		
		ArrayList authorities = new ArrayList();
		MutableAnnotation[] results = data.getMutableAnnotations("result");
		for (int r = 0; r < results.length; r++) {
			MutableAnnotation[] nameStatus = results[r].getMutableAnnotations("name_status");
			if ((nameStatus.length != 0) && "common".equalsIgnoreCase(nameStatus[0].firstValue()))
				continue;
			
			//	find end of direct child nodes (start of classification (for valid names) or accepted_name (for invalid names))
			int dataEnd = results[r].size();
			MutableAnnotation[] acceptedNames = results[r].getMutableAnnotations("accepted_name");
			if (acceptedNames.length != 0)
				dataEnd = acceptedNames[0].getStartIndex();
			MutableAnnotation[] classification = results[r].getMutableAnnotations("classification");
			if (classification.length == 0)
				dataEnd = classification[0].getStartIndex();
			
			//	check rank
			MutableAnnotation[] ranks = results[r].getMutableAnnotations("rank");
			if ((ranks.length == 0) || (dataEnd < ranks[0].getEndIndex()))
				continue;
			String dRank = ranks[0].firstValue();
			if ("Infraspecies".equals(dRank)) {
				MutableAnnotation[] infspecMarkers = results[r].getMutableAnnotations("infraspecies_marker");
				if ((infspecMarkers.length == 0) || (dataEnd < infspecMarkers[0].getEndIndex()))
					continue;
				String infspecMarker = infspecMarkers[0].valueAt(0);
				if ("subsp".equalsIgnoreCase(infspecMarker))
					dRank = SUBSPECIES_ATTRIBUTE;
				else if ("var".equalsIgnoreCase(infspecMarker))
					dRank = VARIETY_ATTRIBUTE;
				else if ("f".equalsIgnoreCase(infspecMarker))
					dRank = rank;
			}
			if (!rank.equalsIgnoreCase(dRank))
				continue;
			
			//	match epithets against canonicalName
			MutableAnnotation[] names = results[r].getMutableAnnotations("name");
			if ((names.length == 0) || (dataEnd < names[0].getEndIndex()))
				continue;
			String canonicalName = TokenSequenceUtils.concatTokens(names[0], true, true);
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
			
			//	get references and extract year
			MutableAnnotation[] references = results[r].getMutableAnnotations("references");
			if ((references.length == 0) || (dataEnd < references[0].getEndIndex()))
				continue;
			MutableAnnotation[] refYears = references[0].getMutableAnnotations("year");
			if (refYears.length == 0)
				continue;
			String authorityYear = refYears[0].firstValue();
			
			//	get author
			MutableAnnotation[] authors = results[r].getMutableAnnotations("author");
			if ((authors.length == 0) || (dataEnd < authors[0].getEndIndex()))
				continue;
			String authorityName = this.extractCData(TokenSequenceUtils.concatTokens(authors[0], true, true));
			if (authorityName.indexOf(", nom") != -1)
				authorityName = authorityName.substring(0, authorityName.indexOf(", nom"));
			authorityName = authorityName.trim();
			
			//	expand abbreviated (current) authority names from reference
			authorityName = this.expandAbbreviations(authorityName, references[0]);
			
			String authorityString = (authorityName + ", " + authorityYear);
			
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
	
	private String extractCData(String cData) {
		if (cData == null)
			return null;
		if (cData.matches("\\<\\!\\s*\\[CDATA\\s*\\[.*")) {
			cData = cData.replace("<! [CDATA", "<![CDATA");
			cData = cData.replace("<![CDATA [", "<![CDATA[");
			cData = cData.substring("<![CDATA[".length()).trim();
			cData = cData.substring(0, cData.lastIndexOf("]]>")).trim();
			System.out.println("CDATA: " + cData);
		}
		return cData;
	}
	
	private String expandAbbreviations(String authorityName, MutableAnnotation reference) {
		MutableTokenSequence authorityTokens = Gamta.newTokenSequence(authorityName, reference.getTokenizer());
		Annotation[] authorityAbbrevs = Gamta.extractAllMatches(authorityTokens, "[A-Za-z][A-Za-z\\-]+[A-Za-z]\\.");
		if (authorityAbbrevs.length == 0)
			return authorityName; // nothing to fill in
		
		MutableAnnotation[] refAuthors = reference.getMutableAnnotations("author");
		if (refAuthors.length == 0)
			return authorityName; // nothing to work with
		String refAuthorityName = this.extractCData(TokenSequenceUtils.concatTokens(refAuthors[0], true, true));
		MutableTokenSequence refAuthorityTokens = Gamta.newTokenSequence(refAuthorityName, reference.getTokenizer());
		
		int startIndex = (TokenSequenceUtils.indexOf(authorityTokens, ")") + 1);
		for (int a = authorityAbbrevs.length; a > 0; a--) {
			if (authorityAbbrevs[a-1].getStartIndex() < startIndex)
				break;
			for (int t = refAuthorityTokens.size(); t > 0; t--)
				if (refAuthorityTokens.valueAt(t-1).startsWith(authorityAbbrevs[a-1].firstValue())) {
					authorityTokens.setValueAt(refAuthorityTokens.valueAt(t-1), authorityAbbrevs[a-1].getStartIndex());
					authorityTokens.removeTokensAt((authorityAbbrevs[a-1].getStartIndex() + 1), 1);
					break;
				}
		}
		return TokenSequenceUtils.concatTokens(authorityTokens, true, true);
	}
	
	private static String baseUrl = "http://www.catalogueoflife.org/col/webservice?response=full&name=";
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		ColAuthorityProvider colAp = new ColAuthorityProvider();
		colAp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AuthorityAugmenterData/ColData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
		
		/* some test cases:
		 * - tribe Formicini
		 * - species Orchis maculata (need to use data URL "http://www.catalogueoflife.org/col/webservice?response=full&id=fbe4bfa0ed3a1316bf9864b2b3d6871a" for this one to prevent read timeout)
		 * - subspecies Orchis brancifortii maculata
		 * - subspecies Dactylorchis maculata arduennensis
		 */
		String testEpithet = "Dactylorchis maculata arduennensis";//"Hemiptera";//"Braunsia";//"Chenopodium";
		String testRank = SUBSPECIES_ATTRIBUTE;
		Properties hierarchy = colAp.getAuthority(testEpithet.split("\\s+"), testRank, null, -1, true);
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
				System.out.println(rank.substring(prefix.length()) + ": " + hierarchy.getProperty(rank));
			}
			if (prefixEmpty)
				break;
		}
		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21330.complete.xml"), "UTF-8"));
//		ihi.process(doc, new Properties());
//		Annotation[] taxonNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		for (int t = 0; t < taxonNames.length; t++)
//			System.out.println(taxonNames[t].toXML());
	}
}