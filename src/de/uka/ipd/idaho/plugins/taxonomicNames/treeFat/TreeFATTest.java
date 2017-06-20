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
package de.uka.ipd.idaho.plugins.taxonomicNames.treeFat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTreeNode;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.locations.CountryHandler.RegionHandler;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Taxonomic name handling facility using online catalogs as well as regular
 * expression patterns and document internal inference.
 * 
 * @author sautter
 */
public class TreeFATTest implements TaxonomicNameConstants, ImagingConstants {
	
	/**
	 * 
	 */
	public TreeFATTest() {
		// TODO Auto-generated constructor stub
	}
	
	private static final String[] primaryRankNames = {
		KINGDOM_ATTRIBUTE,
		PHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
		GENUS_ATTRIBUTE,
	};
	private static final Properties primaryChildRanks = new Properties();
	static {
		primaryChildRanks.setProperty(KINGDOM_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		primaryChildRanks.setProperty(PHYLUM_ATTRIBUTE, CLASS_ATTRIBUTE);
		primaryChildRanks.setProperty(CLASS_ATTRIBUTE, ORDER_ATTRIBUTE);
		primaryChildRanks.setProperty(ORDER_ATTRIBUTE, FAMILY_ATTRIBUTE);
		primaryChildRanks.setProperty(FAMILY_ATTRIBUTE, GENUS_ATTRIBUTE);
		primaryChildRanks.setProperty(GENUS_ATTRIBUTE, SPECIES_ATTRIBUTE);
	}
	
	//	TODO move this to stopWords.suffixes.txt
	private static final String[] stopSuffixes = {
		//	common English
		"ing",
		"tion",
		"ology",
		"rnal",
		"ical",
		
		//	common German
		"ung",
		"heit",
		"keit",
		"lich",
		"chen",
		"artig",
		
		//	location names
		"ville",
		"town",
		"port",
		"stadt",
		"dorf",
		"heim",
		"burg",
		"wood",
		"ham",
		"grad",
	};
	
	private static final String[] countryNameLanguages = {
		"English",
		"German",
		"French",
		"Italian",
		"Spanish",
		"Portuguese",
	};
	
	private static class Seed {
		final String epithet;
		String validEpithet = null;
		final int firstOccurrenceIndex;
		int count = 0;
		int boldCount = 0;
		int italicsCount = 0;
		Properties higherTaxonomy = null;
		String rank = null;
		TreeSet childEpithets = null;
		Seed(String epithet, int firstOccurrenceIndex) {
			this.epithet = epithet;
			this.firstOccurrenceIndex = firstOccurrenceIndex;
		}
		public String toString() {
			return (this.epithet + " (" + this.count + " times, " + this.boldCount + " times in bold, " + this.italicsCount + " times in italics, first at " + this.firstOccurrenceIndex + ")");
		}
		void addChildEpithet(String childEpithet) {
			if (this.childEpithets == null)
				this.childEpithets = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			this.childEpithets.add(childEpithet);
		}
	}
	
	private static Seed[] getSeedsFromHighlights(MutableAnnotation doc, CountryHandler countryHandler) {
		Map seeds = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		Set ignoreIndexes = new HashSet();
		collectCountryRegionTokens(doc, countryHandler, ignoreIndexes);
		collectSeeds(doc.getAnnotations(EMPHASIS_TYPE), seeds, ignoreIndexes);
		collectSeeds(doc.getAnnotations(HEADING_TYPE), seeds, ignoreIndexes);
		return ((Seed[]) seeds.values().toArray(new Seed[seeds.size()]));
	}
	
	private static void collectCountryRegionTokens(MutableAnnotation doc, CountryHandler countryHandler, Set tokenIndexes) {
		System.out.println("Getting country names:");
		Annotation[] countries = Gamta.extractAllContained(doc, countryHandler);
		Map regionHandlers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int c = 0; c < countries.length; c++) {
			String country = TokenSequenceUtils.concatTokens(countries[c], true, true);
			if ((country.length() < 4) && !country.equals(country.toUpperCase()))
				continue;
			for (int t = 0; t < countries[c].size(); t++)
				tokenIndexes.add(new Integer(countries[c].getStartIndex() + t));
			String eCountry = countryHandler.getEnglishName(country);
			if (regionHandlers.containsKey(eCountry))
				continue;
			System.out.println(" --> " + country + " (" + eCountry + ")");
			RegionHandler rh = countryHandler.getRegionHandler(eCountry, countryNameLanguages);
			if (rh != null)
				regionHandlers.put(eCountry, rh);
		}
		for (Iterator cit = regionHandlers.keySet().iterator(); cit.hasNext();) {
			String eCountry = ((String) cit.next());
			RegionHandler regionHandler = ((RegionHandler) regionHandlers.get(eCountry));
			System.out.println(" - getting region names from " + eCountry + ":");
			Annotation[] regions = Gamta.extractAllContained(doc, regionHandler);
			for (int r = 0; r < regions.length; r++) {
				for (int t = 0; t < regions[r].size(); t++)
					tokenIndexes.add(new Integer(regions[r].getStartIndex() + t));
				System.out.println("   --> " + TokenSequenceUtils.concatTokens(regions[r], true, true));
			}
		}
	}
	
	private static void collectSeeds(QueriableAnnotation[] seekAnnots, Map seeds, Set collected) {
		for (int s = 0; s < seekAnnots.length; s++)
			collectSeeds(seekAnnots[s], seeds, collected);
	}
	private static void collectSeeds(QueriableAnnotation seekAnnot, Map seeds, Set collected) {
		for (int t = 0; t < seekAnnot.size(); t++) {
			String token = seekAnnot.valueAt(t);
			
			//	test for sequence of basic Latin letters with capital start
			if (!token.matches("[A-Z][a-zA-Z]{2,}"))
				continue;
			
			//	skip over 'North, South, East, West, Central' plus subsequent word (will be geographic name)
			//	TODO move this to stopWords.trace.txt
			if (";North;South;East;West;Central;".indexOf(";" + token + ";") != -1) {
				t++;
				continue;
			}
			
			//	test for full word
			boolean noVowel = true;
			boolean noConsonant = true;
			for (int c = 0; c < token.length(); c++) {
				if ("yY".indexOf(token.charAt(c)) != -1) {
					noVowel = false;
					noConsonant = false;
				}
				else if ("aeiouAEIOU".indexOf(token.charAt(c)) == -1)
					noConsonant = false;
				else noVowel = false;
			}
			if (noVowel || noConsonant)
				continue;
			
			//	Roman number
			if (Gamta.isRomanNumber(token))
				continue;
			
			//	we've seen this very one
			if (!collected.add(new Integer(seekAnnot.getAbsoluteStartIndex() + t)))
				continue;
			
			//	this one looks OK for starters
			Seed seed = ((Seed) seeds.get(token));
			if (seed == null) {
				seed = new Seed(token, (seekAnnot.getAbsoluteStartIndex() + t));
				seeds.put(token, seed);
			}
			seed.count++;
			if (seekAnnot.hasAttribute(BOLD_ATTRIBUTE))
				seed.boldCount++;
			if (seekAnnot.hasAttribute(ITALICS_ATTRIBUTE))
				seed.italicsCount++;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File dataPath = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/TreeFATData");
		
		TreeSet stopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		try {
			BufferedReader swBr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dataPath, "stopWords.txt")), "utf-8"));
			for (String sw; (sw = swBr.readLine()) != null;) {
				sw = sw.trim();
				if ((sw.length() != 0) && !sw.startsWith("//"))
					stopWords.add(sw);
			}
			swBr.close();
		} catch (Exception e) {}
		
		CountryHandler countryHandler = CountryHandler.getCountryHandler(((File) null), countryNameLanguages);
		
		TaxonomicRankSystem trs = TaxonomicRankSystem.getRankSystem("ICZN");
		String testFileName;
//		testFileName = "arac-38-02-328.pdf.imf.xml";
		testFileName = "zt00872.pdf.imf.xml";
//		testFileName = "zt03652p155.pdf.imf.xml";
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Testdaten/PdfExtract/" + testFileName));
		
		//	collect potential genera and families from emphases and headings as SEEDS
		Seed[] seeds = getSeedsFromHighlights(doc, countryHandler);
		LinkedList seedList = new LinkedList();
		System.out.println("Got " + seeds.length + " initial seeds in " + doc.size() + " tokens");
		
		//	try and cut off bibliography (if any), as journal names are italicized in quite a few reference styles
		int cutoff = doc.size();
		for (int s = 0; s < seeds.length; s++) {
			if ((seeds[s].firstOccurrenceIndex * 4) < (doc.size() * 3))
				continue;
			if ("References".equalsIgnoreCase(seeds[s].epithet) || "Literature".equalsIgnoreCase(seeds[s].epithet) || "Bibliography".equalsIgnoreCase(seeds[s].epithet))
				cutoff = Math.min(seeds[s].firstOccurrenceIndex, cutoff);
		}
		if (cutoff < doc.size()) {
			System.out.println("Cutting off bibliography after " + cutoff + ":");
			seedList.clear();
			for (int s = 0; s < seeds.length; s++) {
				if (seeds[s].firstOccurrenceIndex < cutoff)
					seedList.add(seeds[s]);
				else System.out.println(" - " + seeds[s]);
			}
			if (seedList.size() < seeds.length)
				seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		}
		
		//	filter out stop words
		System.out.println("Filtering out stop words:");
		seedList.clear();
		for (int s = 0; s < seeds.length; s++) {
			if (!stopWords.contains(seeds[s].epithet))
				seedList.add(seeds[s]);
			else System.out.println(" - " + seeds[s]);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		
		//	filter seeds based on specific endings
		System.out.println("Filtering based on suffixes:");
		seedList.clear();
		for (int s = 0; s < seeds.length; s++) {
			boolean retain = true;
			for (int f = 0; f < stopSuffixes.length; f++)
				if (seeds[s].epithet.toLowerCase().endsWith(stopSuffixes[f])) {
					retain = false;
					break;
				}
			if (retain)
				seedList.add(seeds[s]);
			else System.out.println(" - " + seeds[s]);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		
		//	sort seeds by frequency
		Arrays.sort(seeds, new Comparator() {
			public int compare(Object o1, Object o2) {
				Seed s1 = ((Seed) o1);
				Seed s2 = ((Seed) o2);
				return ((s1.count == s2.count) ? s1.epithet.compareTo(s2.epithet) : (s2.count - s1.count));
			}
		});
		
		//	lookup higher taxonomy for seeds
		HigherTaxonomyProvider htp = HigherTaxonomyProvider.getTaxonomyProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/HigherTaxonomyData")));
		Properties epithetRanks = new Properties();
		seedList.clear();
		TreeMap epithetsToHierarchies = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		CountingSet epithetFrequencies = new CountingSet();
		for (int s = 0; s < seeds.length; s++) {
			System.out.println("Getting catalog data for " + seeds[s].epithet + ((seeds[s].validEpithet == null) ? "" : (" (" + seeds[s].validEpithet + ")")));
			String epithet = ((seeds[s].validEpithet == null) ? seeds[s].epithet : seeds[s].validEpithet);
			
			//	get rank (might be known from prior lookups)
			String rank = epithetRanks.getProperty(epithet, (epithet.toLowerCase().matches(".*idae") ? FAMILY_ATTRIBUTE : GENUS_ATTRIBUTE));
			System.out.println(" - rank is " + rank);
			
			//	do lookup
			Properties seh = htp.getHierarchy(epithet, rank, true);
			if (seh == null) {
				System.out.println("NOT FOUND");
				continue;
			}
			
			//	extract individual hierarchies
			Properties backupSeh = null;
			if (seh.containsKey(rank) && seh.containsKey("0." + rank)) {
				backupSeh = new Properties();
				for (Iterator kit = seh.keySet().iterator(); kit.hasNext();) {
					String key = ((String) kit.next());
					if (key.matches("[0-9]+\\..+"))
						continue;
					backupSeh.setProperty(key, seh.getProperty(key));
					kit.remove();
				}
			}
			Properties[] sehs = HigherTaxonomyProvider.extractHierarchies(seh);
			if (backupSeh != null) {
				Properties[] restoreSehs = new Properties[sehs.length+1];
				System.arraycopy(sehs, 0, restoreSehs, 0, sehs.length);
				restoreSehs[sehs.length] = backupSeh;
				sehs = restoreSehs;
			}
			
			//	evaluate hierarchies
			int juniorSynonymCount = 0;
			TreeSet seniorSynonyms = null;
			String childRank = primaryChildRanks.getProperty(rank);
			for (int h = 0; h < sehs.length; h++) {
				TreeMap sh = new TreeMap();
				sh.putAll(sehs[h]);
				System.out.println("" + sh);
				
				//	make sure families and orders are looked up with correct rank (as we're going by decreasing frequency, we can safely assume the first lookup should actually be a genus or family)
				for (int r = 0; r < primaryRankNames.length; r++) {
					if (primaryRankNames[r].equals(childRank))
						break;
					if (sehs[h].containsKey(primaryRankNames[r])) {
						epithetRanks.setProperty(sehs[h].getProperty(primaryRankNames[r]), primaryRankNames[r]);
						epithetFrequencies.add(sehs[h].getProperty(primaryRankNames[r]));
					}
				}
				
				if ((seeds[s].validEpithet == null) && sehs[h].containsKey(rank) && !seeds[s].epithet.equalsIgnoreCase(sehs[h].getProperty(rank))) {
					System.out.println(" ==> junior synonym of " + sehs[h].getProperty(rank));
					juniorSynonymCount++;
					if (seniorSynonyms == null)
						seniorSynonyms = new TreeSet();
					seniorSynonyms.add(sehs[h].getProperty(rank));
				}
				
				//	get list of (primary rank) children
				if (sehs[h].containsKey(childRank)) {
					String childEpithetString = sehs[h].getProperty(childRank);
					String[] childEpithets = childEpithetString.split("\\s*\\;\\s*");
					for (int c = 0; c < childEpithets.length; c++)
						seeds[s].addChildEpithet(childEpithets[c]);
				}
			}
			
			//	do we have a junior synonym here?
			if (((juniorSynonymCount * 2) > sehs.length) && (seniorSynonyms.size() == 1)) {
				seeds[s--].validEpithet = ((String) seniorSynonyms.first());
				continue;
			}
			
			//	store seed for further processing
			seedList.add(seeds[s]);
			
			//	store rank and hierarchies
			seeds[s].rank = rank;
			seeds[s].higherTaxonomy = HigherTaxonomyProvider.bundleHierarchies(sehs);
			epithetsToHierarchies.put(seeds[s].epithet, sehs);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		
		//	what do we got?
		System.out.println("Got " + seeds.length + " seeds with catalog results:");
		for (int s = 0; s < seeds.length; s++) {
			System.out.println(" - " + seeds[s]);
			if (seeds[s].childEpithets != null)
				System.out.println("   " + seeds[s].childEpithets);
		}
		
		//	build dictionaries from seeds TODO consider also using child epithet list from family-rank seeds
		StringVector orderDict = new StringVector();
		StringVector familyDict = new StringVector();
		StringVector genusDict = new StringVector();
		StringVector speciesDict = new StringVector();
		for (int s = 0; s < seeds.length; s++) {
			if (GENUS_ATTRIBUTE.equals(seeds[s].rank)) {
				genusDict.addElementIgnoreDuplicates(seeds[s].epithet);
				if (seeds[s].childEpithets != null) {
					for (Iterator ceit = seeds[s].childEpithets.iterator(); ceit.hasNext();)
						speciesDict.addElementIgnoreDuplicates((String) ceit.next());
				}
			}
			else if (FAMILY_ATTRIBUTE.equals(seeds[s].rank)) {
				familyDict.addElementIgnoreDuplicates(seeds[s].epithet);
				if (seeds[s].childEpithets != null) {
					for (Iterator ceit = seeds[s].childEpithets.iterator(); ceit.hasNext();)
						genusDict.addElementIgnoreDuplicates((String) ceit.next());
				}
			}
			else if (ORDER_ATTRIBUTE.equals(seeds[s].rank))
				orderDict.addElementIgnoreDuplicates(seeds[s].epithet);
		}
		
		//	collect what we have so far
		AnnotationIndex ai = new AnnotationIndex(doc, null);
		
		//	collect orders, families, genera, and species in AnnotationIndex (genus abbreviations as well)
		Annotation[] orders = Gamta.extractAllContained(doc, orderDict, 1);
		ai.addAnnotations(orders, ORDER_ATTRIBUTE);
		Annotation[] families = Gamta.extractAllContained(doc, familyDict, 1);
		ai.addAnnotations(families, FAMILY_ATTRIBUTE);
		Annotation[] genera = Gamta.extractAllContained(doc, genusDict, 1);
		ai.addAnnotations(genera, GENUS_ATTRIBUTE);
		Properties abbrevsToGenera = new Properties();
		for (int g = 0; g < genusDict.size(); g++) {
			String genus = genusDict.get(g);
			String regEx = (genus.substring(0, 1) + "[" + genus.substring(1).replaceAll("e", "") + "]?\\.?");
			Annotation[] abbrevGenera = Gamta.extractAllMatches(doc, regEx, 2);
			ai.addAnnotations(abbrevGenera, "abbrevGenus");
			for (int a = 0; a < abbrevGenera.length; a++)
				abbrevsToGenera.setProperty(abbrevGenera[a].getValue(), genus);
		}
		Annotation[] species = Gamta.extractAllContained(doc, speciesDict, 1);
		ai.addAnnotations(species, SPECIES_ATTRIBUTE);
		
		//	collect name labels in AnnotationIndex
		StringVector labelDict = new StringVector();
		StringVector newLabelDict = new StringVector();
		Rank[] ranks = trs.getRanks();
		boolean genusToCome = true;
		boolean speciesToCome = true;
		for (int r = 0; r < ranks.length; r++) {
			
			//	work genus and below only
			if (GENUS_ATTRIBUTE.equals(ranks[r].name))
				genusToCome = false;
			else if (genusToCome)
				continue;
			
			//	collect spelled-out labels
			labelDict.addElementIgnoreDuplicates(ranks[r].name);
			newLabelDict.addElementIgnoreDuplicates("new " + ranks[r].name);
			newLabelDict.addElementIgnoreDuplicates(ranks[r].name + " nov.");
			newLabelDict.addElementIgnoreDuplicates(ranks[r].name + " nov");
			
			//	collect short labels
			String[] rankLabels = ranks[r].getAbbreviations();
			StringBuffer labelMatcher = new StringBuffer();
			for (int l = 0; l < rankLabels.length; l++) {
				labelDict.addElementIgnoreDuplicates(ranks[r].name);
				newLabelDict.addElementIgnoreDuplicates("new " + ranks[r].name);
				newLabelDict.addElementIgnoreDuplicates("n. " + ranks[r].name);
				newLabelDict.addElementIgnoreDuplicates("n " + ranks[r].name);
				newLabelDict.addElementIgnoreDuplicates(ranks[r].name + " nov.");
				newLabelDict.addElementIgnoreDuplicates(ranks[r].name + " nov");
				newLabelDict.addElementIgnoreDuplicates(ranks[r].name + " n.");
				newLabelDict.addElementIgnoreDuplicates(ranks[r].name + " n");
				if (labelMatcher.length() != 0)
					labelMatcher.append('|');
				labelMatcher.append("'" + rankLabels[l] + "'");
			}
			
			//	collecting unknown, yet labeled below-species epithets
			if (SPECIES_ATTRIBUTE.equals(ranks[r].name)) {
				speciesToCome = false;
				continue;
			}
			else if (speciesToCome)
				continue;
			
			Annotation[] rankEpithets = AnnotationPatternMatcher.getMatches(doc, ("(" + labelMatcher.toString() + ") \"[a-z]{3,}\""));
			ai.addAnnotations(rankEpithets, ranks[r].name);
		}
		Annotation[] tnLabels = Gamta.extractAllContained(doc, labelDict, 2);
		ai.addAnnotations(tnLabels, "label");
		Annotation[] newTnLabels = Gamta.extractAllContained(doc, newLabelDict, 4);
		ai.addAnnotations(newTnLabels, "newLabel");
		
		//	collect authorities in AnnotationIndex
		Annotation[] authorityNames = Gamta.extractAllMatches(doc, "([A-Z]\\.\\s*)*[A-Z][a-z]{2,}(\\-[A-Z][a-z]{2,})?");
		ai.addAnnotations(authorityNames, "authorityName");
		Annotation[] authorityYears = Gamta.extractAllMatches(doc, "[12][0-9]{3}");
		ai.addAnnotations(authorityYears, "authorityYear");
		Annotation[] authorities = AnnotationPatternMatcher.getMatches(doc, ai, "<authorityName> (\"[\\\\,\\\\&]\" <authorityName>)* ','? <authorityYear>");
		ai.addAnnotations(authorities, "authority");
		for (int a = 0; a < authorities.length; a++)
			System.out.println("Authority: " + authorities[a].getValue());
		authorities = AnnotationPatternMatcher.getMatches(doc, ai, "'('<authority>')'");
		ai.addAnnotations(authorities, "authority");
		for (int a = 0; a < authorities.length; a++)
			System.out.println("Authority: " + authorities[a].getValue());
		
		//	annotate possible sub genera
		Annotation[] subGenera = Gamta.extractAllMatches(doc, "[A-Z][a-z]+");
		ai.addAnnotations(subGenera, SUBGENUS_ATTRIBUTE);
		
		//	use annotation patterns to assemble full names (optional sub genus, etc. !!!)
		MatchTree[] taxonNames;
		taxonNames = AnnotationPatternMatcher.getMatchTrees(doc, ai, "<order> <authority>?");
		for (int t = 0; t < taxonNames.length; t++) {
			System.out.println("TaxonName: " + taxonNames[t].getMatch().getValue() + " at " + taxonNames[t].getMatch().getStartIndex());
			System.out.println(taxonNames[t].toString("  "));
			annotateTaxonName(doc, taxonNames[t], trs, abbrevsToGenera);
		}
		taxonNames = AnnotationPatternMatcher.getMatchTrees(doc, ai, "<family> <authority>?");
		for (int t = 0; t < taxonNames.length; t++) {
			System.out.println("TaxonName: " + taxonNames[t].getMatch().getValue() + " at " + taxonNames[t].getMatch().getStartIndex());
			System.out.println(taxonNames[t].toString("  "));
			annotateTaxonName(doc, taxonNames[t], trs, abbrevsToGenera);
		}
		taxonNames = AnnotationPatternMatcher.getMatchTrees(doc, ai, "<abbrevGenus> ('(' <subGenus> ')')? <species> <subSpecies>? <variety>? (<newLabel>|<authority>)?");
		for (int t = 0; t < taxonNames.length; t++) {
			System.out.println("TaxonName: " + taxonNames[t].getMatch().getValue() + " at " + taxonNames[t].getMatch().getStartIndex());
			System.out.println(taxonNames[t].toString("  "));
			annotateTaxonName(doc, taxonNames[t], trs, abbrevsToGenera);
		}
		taxonNames = AnnotationPatternMatcher.getMatchTrees(doc, ai, "<genus> (<newLabel>|<authority>)?");
		for (int t = 0; t < taxonNames.length; t++) {
			System.out.println("TaxonName: " + taxonNames[t].getMatch().getValue() + " at " + taxonNames[t].getMatch().getStartIndex());
			System.out.println(taxonNames[t].toString("  "));
			annotateTaxonName(doc, taxonNames[t], trs, abbrevsToGenera);
		}
		
		//	add higher taxonomy, using frequencies of epithets for disambiguation
		Annotation[] taxonNameAnnots = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		TreeMap epithetsToHigherTaxonomies = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNameAnnots.length; t++) {
			
			//	find lowest given primary rank from genus upward
			String primaryRankEpithet = null;
			String primaryRank = null;
			for (int r = (primaryRankNames.length - 1); r >= 0; r--) {
				primaryRankEpithet = ((String) taxonNameAnnots[t].getAttribute(primaryRankNames[r]));
				if (primaryRankEpithet != null) {
					primaryRank = primaryRankNames[r];
					break;
				}
			}
			if (primaryRankEpithet == null)
				continue;
			
			//	get higher taxonomy
			Properties higherTaxonomy = ((Properties) epithetsToHigherTaxonomies.get(primaryRankEpithet));
			if (higherTaxonomy == null)
				higherTaxonomy = selectHigherTaxonomy(primaryRankEpithet, primaryRank, ((Properties[]) epithetsToHierarchies.get(primaryRankEpithet)), epithetFrequencies);
			if (higherTaxonomy == null)
				continue;
			epithetsToHigherTaxonomies.put(primaryRankEpithet, higherTaxonomy);
			
			//	add higher taxonomy to taxon name
			for (int r = 0; r < primaryRankNames.length; r++) {
				if (primaryRankNames[r].equals(GENUS_ATTRIBUTE))
					break;
				if (higherTaxonomy.containsKey(primaryRankNames[r]))
					taxonNameAnnots[t].setAttribute(primaryRankNames[r], higherTaxonomy.getProperty(primaryRankNames[r]));
			}
			System.out.println(taxonNameAnnots[t].toXML());
		}
		
		//	TODO turn this whole thing into an Analyzer
	}
	
	private static Properties selectHigherTaxonomy(String epithet, String rank, Properties[] hierarchies, CountingSet epithetFrequencies) {
		if ((hierarchies == null) || (hierarchies.length == 0))
			return null;
		Properties higherTaxonomy = null;
		int higherTaxonomyScore = 0;
		for (int h = 0; h < hierarchies.length; h++) {
			System.out.println("Scoring for " + rank + " '" + epithet + "': " + hierarchies[h]);
			int hierarchyScore = 0;
			for (int r = 0; r < primaryRankNames.length; r++) {
				if (primaryRankNames[r].equals(GENUS_ATTRIBUTE))
					break;
				if (hierarchies[h].containsKey(primaryRankNames[r]))
					hierarchyScore += epithetFrequencies.getCount(hierarchies[h].getProperty(primaryRankNames[r]));
			}
			if (hierarchyScore > higherTaxonomyScore) {
				System.out.println(" ==> score is " + hierarchyScore + ", new top score");
				higherTaxonomy = hierarchies[h];
				higherTaxonomyScore = hierarchyScore;
			}
			else System.out.println(" ==> score is " + hierarchyScore + ", less than current best");
		}
		return higherTaxonomy;
	}
	
	private static void annotateTaxonName(MutableAnnotation doc, MatchTree taxonName, TaxonomicRankSystem trs, Properties abbrevsToGenera) {
		Annotation taxonNameAnnot = taxonName.getMatch();
		taxonNameAnnot.changeTypeTo(TAXONOMIC_NAME_ANNOTATION_TYPE);
		Annotation newLabelAnnot = addAttributes(taxonNameAnnot, taxonName.getChildren(), trs, abbrevsToGenera);
		if (newLabelAnnot == null) {
			taxonNameAnnot.changeTypeTo(TAXONOMIC_NAME_ANNOTATION_TYPE);
			taxonNameAnnot = doc.addAnnotation(taxonNameAnnot);
			System.out.println(taxonNameAnnot.toXML());
		}
		else {
			Annotation cTaxonNameAnnot = doc.addAnnotation(TAXONOMIC_NAME_ANNOTATION_TYPE, taxonNameAnnot.getStartIndex(), (newLabelAnnot.getStartIndex() - taxonNameAnnot.getStartIndex()));
			cTaxonNameAnnot.copyAttributes(taxonNameAnnot);
			newLabelAnnot.changeTypeTo(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
			newLabelAnnot = doc.addAnnotation(newLabelAnnot);
			taxonNameAnnot = cTaxonNameAnnot;
			System.out.println(taxonNameAnnot.toXML());
			System.out.println(newLabelAnnot.toXML());
		}
	}
	
	private static Annotation addAttributes(Annotation taxonName, MatchTree mt, TaxonomicRankSystem trs, Properties abbrevsToGenera) {
		return addAttributes(taxonName, mt.getChildren(), trs, abbrevsToGenera);
	}
	
	private static Annotation addAttributes(Annotation taxonName, MatchTreeNode[] mtns, TaxonomicRankSystem trs, Properties abbrevsToGenera) {
		Annotation newLabel = null;
		for (int n = 0; n < mtns.length; n++) {
			if (mtns[n].getPattern().startsWith("<")) {
				System.out.println("Got attribute '" + mtns[n].getPattern() + "': " + mtns[n].getMatch().getValue());
				String an = mtns[n].getPattern().replaceAll("[\\<\\>]", "");
				if ("abbrevGenus".equals(an))
					taxonName.setAttribute(GENUS_ATTRIBUTE, abbrevsToGenera.getProperty(mtns[n].getMatch().getValue()));
				else if (trs.getRank(an) != null) {
					taxonName.setAttribute(an, mtns[n].getMatch().lastValue());
					taxonName.setAttribute(RANK_ATTRIBUTE, an);
				}
				else if ("authority".equals(an)) {
					String authority = mtns[n].getMatch().getValue();
					if (authority.startsWith("("))
						authority = authority.substring("(".length()).trim();
					if (authority.endsWith(")"))
						authority = authority.substring(0, (authority.length() - ")".length())).trim();
					taxonName.setAttribute(AUTHORITY_ATTRIBUTE, authority);
					taxonName.setAttribute(AUTHORITY_YEAR_ATTRIBUTE, authority.substring(authority.length() - 4).trim());
					String authorityName = authority.substring(0, (authority.length() - 4)).trim();
					if (authorityName.endsWith(","))
						authorityName = authorityName.substring(0, (authority.length() - ",".length())).trim();
					taxonName.setAttribute(AUTHORITY_NAME_ATTRIBUTE, authorityName);
				}
				else if ("newLabel".equals(an))
					newLabel = mtns[n].getMatch();
			}
			else {
				Annotation cNewLabel = addAttributes(taxonName, mtns[n].getChildren(), trs, abbrevsToGenera);
				if (newLabel == null)
					newLabel = cNewLabel;
			}
		}
		return newLabel;
	}
}