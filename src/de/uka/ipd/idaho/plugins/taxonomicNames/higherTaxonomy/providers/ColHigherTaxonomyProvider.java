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
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;

/**
 * Higher taxonomy provider backed by Catalog of Life.
 * 
 * @author sautter
 */
public class ColHigherTaxonomyProvider extends HigherTaxonomyProvider {
	
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
		return "CoL";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	protected Properties loadHierarchy(String epithet, String rank) throws IOException {
		if ((epithet == null) || (rank == null))
			return null;
		
		URL dataUrl = this.dataProvider.getURL(baseUrl + epithet);
		HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
		if (0 < this.lookupTimeout)
			dataCon.setReadTimeout(this.lookupTimeout);
		Reader dataReader = new BufferedReader(new InputStreamReader(dataCon.getInputStream(), "UTF-8"));
		MutableAnnotation data = SgmlDocumentReader.readDocument(dataReader);
		dataReader.close();
		dataCon.disconnect();
		
		Properties hierarchy = new Properties();
		MutableAnnotation[] results = data.getMutableAnnotations("result");
		for (int r = 0; r < results.length; r++) {
			MutableAnnotation[] nameStatus = results[r].getMutableAnnotations("name_status");
			if ((nameStatus.length != 0) && "common".equalsIgnoreCase(nameStatus[0].firstValue()))
				continue;
			MutableAnnotation[] acceptedNames = results[r].getMutableAnnotations("accepted_name");
			if (acceptedNames.length != 0)
				continue;
			MutableAnnotation[] classification = results[r].getMutableAnnotations("classification");
			if (classification.length == 0)
				continue;
			MutableAnnotation[] ranks = results[r].getMutableAnnotations("rank");
			if ((ranks.length == 0) || (classification[0].getStartIndex() < ranks[0].getEndIndex()))
				continue;
			if (!rank.equalsIgnoreCase(ranks[0].firstValue()))
				continue;
			
			MutableAnnotation[] taxa = classification[0].getMutableAnnotations("taxon");
			for (int t = 0; t < taxa.length; t++) {
				Annotation[] dRank = taxa[t].getAnnotations("rank");
				if (dRank.length == 0)
					continue;
				String ndRank = rankNameNormalizer.getProperty(dRank[0].firstValue());
				if (ndRank == null)
					continue;
				Annotation[] name = taxa[t].getAnnotations("name");
				if (name.length == 0)
					continue;
				hierarchy.setProperty((((results.length == 1) ? "" : (r + ".")) + ndRank), name[0].firstValue());
			}
			hierarchy.setProperty((((results.length == 1) ? "" : (r + ".")) + RANK_ATTRIBUTE), rank);
			
//			MAKES NO SENSE, WE'RE FETCHING GENERA AND CHILD SPECIES _LISTS_ ==> NO SPECIES AUTHORITY
//			Annotation[] author = results[r].getAnnotations("author");
//			if ((author.length != 0) && (author[0].getEndIndex() <= classification[0].getStartIndex())) {
//				String dAuthority = TokenSequenceUtils.concatTokens(author[0], false, false);
//				if (dAuthority != null) {
//					hierarchy.setProperty(AUTHORITY_ATTRIBUTE, dAuthority.trim());
//					if (dAuthority.matches(".*[12][0-9]{3}")) {
//						String dAuthorityName = dAuthority.substring(0, (dAuthority.length() - 4)).trim();
//						while (dAuthorityName.endsWith(","))
//							dAuthorityName = dAuthorityName.substring(0, (dAuthorityName.length() - ",".length())).trim();
//						hierarchy.setProperty(AUTHORITY_NAME_ATTRIBUTE, dAuthorityName);
//						hierarchy.setProperty(AUTHORITY_YEAR_ATTRIBUTE, dAuthority.substring(dAuthority.length() - 4).trim());
//					}
//					else hierarchy.setProperty(AUTHORITY_NAME_ATTRIBUTE, dAuthority.trim());
//				}
//			}
//			
			String childRank = childRanks.getProperty(rank);
			if (childRank == null)
				continue;
			MutableAnnotation[] childList = results[r].getMutableAnnotations("child_taxa");
			if (childList.length == 0)
				continue;
			MutableAnnotation[] children = childList[0].getMutableAnnotations("taxon");
			StringBuffer childrenString = new StringBuffer();
			for (int c = 0; c < children.length; c++) {
				Annotation[] cRank = children[c].getAnnotations("rank");
				if (cRank.length == 0)
					continue;
				Annotation[] cName = children[c].getAnnotations("name");
				if (cName.length == 0)
					continue;
				if ("Not".equalsIgnoreCase(cName[0].firstValue()) && "assigned".equalsIgnoreCase(cName[0].lastValue()))
					continue;
				if (("super" + childRank).equalsIgnoreCase(cRank[0].firstValue()))
					this.appendTransitiveChildren(cName[0].lastValue(), childRank, childrenString);
				else if (childRank.equalsIgnoreCase(cRank[0].firstValue())) {
					//	we need to cut off occasional punctuation marks from end of name (e.g. 'Juncus alpigenus.', CoL isn't all that clean, after all)
					for (int ci = (cName[0].size() - 1); ci >= 0; ci--)
						if (!Gamta.isPunctuation(cName[0].valueAt(ci))) {
							if (childrenString.length() != 0)
								childrenString.append(";");
							childrenString.append(cName[0].valueAt(ci));
							break;
						}
				}
			}
			if (childrenString.length() > 1)
				hierarchy.setProperty((((results.length == 1) ? "" : (r + ".")) + childRank), childrenString.toString());
		}
		
		return hierarchy;
	}
	
	private void appendTransitiveChildren(String superChildEpithet, String childRank, StringBuffer childrenString) throws IOException {
		
		URL dataUrl = this.dataProvider.getURL(baseUrl + superChildEpithet);
		HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
		if (0 < this.lookupTimeout)
			dataCon.setReadTimeout(this.lookupTimeout);
		Reader dataReader = new BufferedReader(new InputStreamReader(dataCon.getInputStream(), "UTF-8"));
		MutableAnnotation data = SgmlDocumentReader.readDocument(dataReader);
		dataReader.close();
		dataCon.disconnect();
		
		MutableAnnotation[] results = data.getMutableAnnotations("result");
		for (int r = 0; r < results.length; r++) {
			MutableAnnotation[] childList = results[r].getMutableAnnotations("child_taxa");
			if (childList.length == 0)
				continue;
			MutableAnnotation[] children = childList[0].getMutableAnnotations("taxon");
			for (int c = 0; c < children.length; c++) {
				Annotation[] cRank = children[c].getAnnotations("rank");
				if (cRank.length == 0)
					continue;
				if (!childRank.equalsIgnoreCase(cRank[0].firstValue()))
					continue;
				Annotation[] cName = children[c].getAnnotations("name");
				if (cName.length == 0)
					continue;
				if ("Not".equalsIgnoreCase(cName[0].firstValue()) && "assigned".equalsIgnoreCase(cName[0].lastValue()))
					continue;
				//	we need to cut off occasional punctuation marks from end of name (e.g. 'Juncus alpigenus.', CoL isn't all that clean, after all)
				for (int ci = (cName[0].size() - 1); ci >= 0; ci--)
					if (!Gamta.isPunctuation(cName[0].valueAt(ci))) {
						if (childrenString.length() != 0)
							childrenString.append(";");
						childrenString.append(cName[0].valueAt(ci));
						break;
					}
			}
		}
	}
	
	private static String baseUrl = "http://www.catalogueoflife.org/col/webservice?response=full&name=";
	
	// !!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		ColHigherTaxonomyProvider colHhp = new ColHigherTaxonomyProvider();
		colHhp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/HigherTaxonomyData/ColData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
		// genus
		String testEpithet = "Mimosa";//"Hemiptera";//"Braunsia";//"Chenopodium";
		String testRank = GENUS_ATTRIBUTE;
		Properties hierarchy = colHhp.getHierarchy(testEpithet, testRank, true);
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