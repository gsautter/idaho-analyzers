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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTreeNode;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner.ParallelFor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor.SynchronizedProgressMonitor;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.locations.CountryHandler.RegionHandler;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;
import de.uka.ipd.idaho.plugins.taxonomicNames.treeFat.TreeFAT.CaseHalfSensitiveDictionary;
import de.uka.ipd.idaho.plugins.taxonomicNames.treeFat.TreeFAT.MapDictionary;
import de.uka.ipd.idaho.plugins.taxonomicNames.treeFat.TreeFAT.SetDictionary;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TreeFatTagger extends AbstractConfigurableAnalyzer implements TaxonomicNameConstants, ImagingConstants, MonitorableAnalyzer {
	
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
		int firstOccurrenceIndex;
		int lastOccurrenceIndex;
		int count = 0;
		int boldCount = 0;
		int boldStartCount = 0;
		int italicsCount = 0;
		int italicsStartCount = 0;
		int allCapsCount = 0;
		Properties[] higherTaxonomies = null;
		String rank = null;
		TreeSet childEpithets = null;
		Seed(String epithet, int firstOccurrenceIndex) {
			this.epithet = (epithet.substring(0,1).toUpperCase() + epithet.substring(1).toLowerCase());
			this.firstOccurrenceIndex = firstOccurrenceIndex;
			this.lastOccurrenceIndex = firstOccurrenceIndex;
		}
		public String toString() {
			return (this.epithet + " (" + rank + ", " + this.count + " times, " + this.boldCount + " times in bold (" + this.boldStartCount + " at start), " + this.italicsCount + " times in italics (" + this.italicsStartCount + " at start), " + this.allCapsCount + " times in all-caps, first at " + this.firstOccurrenceIndex + ", last at " + this.lastOccurrenceIndex + ")");
		}
		void addChildEpithet(String childEpithet) {
			if (this.childEpithets == null)
				this.childEpithets = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			this.childEpithets.add(childEpithet);
		}
	}
	
	private static final Properties[] HIGHER_TAXONOMIES_NON_EXISTENT = {};
	private static final Properties[] HIGHER_TAXONOMIES_UNAVAILABLE = {};
	
	private TreeFAT treeFat;
//	
//	private HigherTaxonomyProvider higherTaxonomyProvider;
	
//	private TreeSet stopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	private TreeSet traceStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	private TreeSet backwardStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	private String[] stopSuffixes = new String[0];
	private GPath[] stopWordSelectors = new GPath[0];
	
//	private TreeSet authorityPrefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	private TreeSet authorityStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	private GPath[] authoritySelectors = new GPath[0];
	
	private CountryHandler countryHandler;
//	
//	private TaxonomicRankSystem rankSystem;
//	private Rank[] ranks;
//	private Rank[] primaryRanks;
//	private Rank[] belowSpeciesRanks;
//	private TreeMap ranksByName = new TreeMap();
//	private int speciesRankRelativeSignificance = -1;
//	
//	private TreeMap labelsToRanks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//	private Dictionary labelDict = new MapDictionary(this.labelsToRanks);
//	private TreeMap newLabelsToRanks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//	private Dictionary newLabelDict = new MapDictionary(this.newLabelsToRanks);
//	private Properties rankLabelMatchers = new Properties();
//	private TreeMap sensuLabelsToSensus = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//	private Dictionary sensuLabelDict = new MapDictionary(this.sensuLabelsToSensus);
	
	private boolean emptyResultLookupsDirty = false;
	private Map emptyResultLookups = Collections.synchronizedMap(new TreeMap(String.CASE_INSENSITIVE_ORDER) {
		public boolean containsKey(Object key) {
			Long llt = ((Long) this.get(key));
			return ((llt != null) && (System.currentTimeMillis() < (llt.longValue() + (1000 * 60 * 60 * 24))));
		}
		public Object put(Object key, Object value) {
			if (value instanceof String)
				value = new Long((String) value);
			emptyResultLookupsDirty = true;
			return super.put(key, value);
		}
	});
	
	private boolean nonCatalogSpeciesByGeneraDirty = false;
	private TreeMap nonCatalogSpeciesByGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
		public Object get(Object genus) {
			Object species = super.get(genus);
			if (species == null) {
				species = new TreeSet(String.CASE_INSENSITIVE_ORDER) {
					public boolean add(Object species) {
						if (super.add(species)) {
							nonCatalogSpeciesByGeneraDirty = true;
							return true;
						}
						else return false;
					}
				};
				this.put(genus, species);
				nonCatalogSpeciesByGeneraDirty = true;
			}
			return species;
		}
	};
	
	/** public zero-argument constructor for class loading */
	public TreeFatTagger() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	get TreeFAT instance
		this.treeFat = TreeFAT.getInstance(this.dataProvider);
		
		//	load tagger specific stop word lists
		this.treeFat.fillList("stopWords.trace.txt", this.traceStopWords);
		this.treeFat.fillList("stopWords.backward.txt", this.backwardStopWords);
		TreeSet stopSuffixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		this.treeFat.fillList("stopWords.suffix.txt", stopSuffixes);
		this.stopSuffixes = ((String[]) stopSuffixes.toArray(new String[stopSuffixes.size()]));
		
		//	load stop word selectors
		TreeSet stopWordSelectorList = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		this.treeFat.fillList("stopWords.selectors.txt", stopWordSelectorList);
		ArrayList stopWordSelectors = new ArrayList();
		for (Iterator swsit = stopWordSelectorList.iterator(); swsit.hasNext();) try {
			stopWordSelectors.add(new GPath((String) swsit.next())); 
		}
		catch (RuntimeException re) {
			re.printStackTrace(System.out);
		}
		this.stopWordSelectors = ((GPath[]) stopWordSelectors.toArray(new GPath[stopWordSelectors.size()]));
		
		//	load country handler
		this.countryHandler = CountryHandler.getCountryHandler(((AnalyzerDataProvider) null), countryNameLanguages);
		
		//	load authority stop word list
		this.treeFat.fillList("authority.stopWords.txt", this.authorityStopWords);
		
		//	load authority selectors
		TreeSet authoritySelectorList = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		this.treeFat.fillList("authority.selectors.txt", authoritySelectorList);
		ArrayList authoritySelectors = new ArrayList();
		for (Iterator swsit = authoritySelectorList.iterator(); swsit.hasNext();) try {
			authoritySelectors.add(new GPath((String) swsit.next())); 
		}
		catch (RuntimeException re) {
			re.printStackTrace(System.out);
		}
		this.authoritySelectors = ((GPath[]) authoritySelectors.toArray(new GPath[authoritySelectors.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		
		//	store empty-result lookups and their last time
		if (this.emptyResultLookupsDirty && this.dataProvider.isDataEditable("emptyResultLookups.txt")) try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("emptyResultLookups.txt"), "UTF-8"));
			for (Iterator git = this.emptyResultLookups.keySet().iterator(); git.hasNext();) {
				String epithet = ((String) git.next());
				Long lastLookup = ((Long) this.emptyResultLookups.get(epithet));
				bw.write(epithet + " " + lastLookup.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
			this.emptyResultLookupsDirty = false;
		} catch (IOException ioe) {}
		
		//	store catalog supplements
		if (this.nonCatalogSpeciesByGeneraDirty && this.dataProvider.isDataEditable("nonCatalogBinomials.txt")) try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("nonCatalogBinomials.txt"), "UTF-8"));
			for (Iterator git = this.nonCatalogSpeciesByGenera.keySet().iterator(); git.hasNext();) {
				String genus = ((String) git.next());
				TreeSet species = ((TreeSet) this.nonCatalogSpeciesByGenera.get(genus));
				for (Iterator spit = species.iterator(); spit.hasNext();) {
					bw.write(genus + " " + spit.next());
					bw.newLine();
				}
			}
			bw.flush();
			bw.close();
			this.nonCatalogSpeciesByGeneraDirty = false;
		} catch (IOException ioe) {}
		
		//	exit TreeFAT instance
		this.treeFat.exitTreeFAT();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		this.process(data, parameters, ProgressMonitor.dummy);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void process(MutableAnnotation doc, Properties parameters, ProgressMonitor pm) {
		if (pm == null)
			pm = ProgressMonitor.dummy;
		
		//	if we're running in fully automated mode, wait for provider responses somewhat longer
		if (parameters.containsKey(INTERACTIVE_PARAMETER) && (this.treeFat.getHigherTaxonomyLookupTimeout() > (10 * 1000)))
			this.treeFat.setHigherTaxonomyLookupTimeout(10 * 1000);
		else if (!parameters.containsKey(INTERACTIVE_PARAMETER) && (this.treeFat.getHigherTaxonomyLookupTimeout() < (120 * 1000)))
			this.treeFat.setHigherTaxonomyLookupTimeout(120 * 1000);
		
		//	collect country and region names, as well as start indexes, for filtering
		StringVector countryRegionStartDict = new StringVector();
		Set countryRegionStartIndexes = new HashSet();
		this.collectCountryRegionTokens(doc, countryRegionStartDict, countryRegionStartIndexes);
		
		//	collect table cell end indices
		Set tableCellEndIndices = new HashSet();
		Annotation[] tcs = doc.getAnnotations("td");
		for (int c = 0; c < tcs.length; c++)
			tableCellEndIndices.add(new Integer(tcs[c].getEndIndex()));
		
		//	collect indices of bold and italics tokens from emphases
		Set boldEmphasisTokenIndices = new HashSet();
		Set italicsEmphasisTokenIndices = new HashSet();
		Set emphasisTokenIndices = new HashSet();
		
		//	collect potential genera and families from emphases and headings as SEEDS
		pm.setStep("Collecting seed families and genera");
		pm.setBaseProgress(0);
		pm.setMaxProgress(8);
		pm.setProgress(0);
		Seed[] seeds = this.getSeedsFromHighlights(doc, countryRegionStartIndexes, boldEmphasisTokenIndices, italicsEmphasisTokenIndices);
		emphasisTokenIndices.addAll(boldEmphasisTokenIndices);
		emphasisTokenIndices.addAll(italicsEmphasisTokenIndices);
		boolean seedsFromEmphases = true;
		ArrayList seedList = new ArrayList();
		pm.setInfo("Found " + seeds.length + " seeds from emphases");
		if (seeds.length == 0) {
			seeds = this.getSeedFamilies(doc); // fallback for documents without emphasis annotations
			pm.setInfo("Found " + seeds.length + " seed families");
			pm.setStep("Doing catalog lookups for " + seeds.length + " seed families");
			pm.setProgress(0);
			this.addHigherTaxonomies(seeds, new TreeSet(), pm);
			TreeSet seedGenusList = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int s = 0; s < seeds.length; s++) {
				seedList.add(seeds[s]);
				if (seeds[s].childEpithets != null) {
					for (Iterator ceit = seeds[s].childEpithets.iterator(); ceit.hasNext();)
						seedGenusList.add((String) ceit.next());
				}
			}
			seeds = this.getSeedGenera(doc, seedGenusList);
			pm.setInfo("Found " + seeds.length + " seed genera from child lists of families");
			for (int s = 0; s < seeds.length; s++)
				seedList.add(seeds[s]);
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
			seedsFromEmphases = false;
		}
		
		//	collect potential above-genus SEEDS based on suffixes
		pm.setStep("Collecting above-genus seeds");
		pm.setBaseProgress(8);
		pm.setMaxProgress(10);
		pm.setProgress(0);
		seeds = this.getSuffixBasedSeeds(doc, seeds, countryRegionStartIndexes, boldEmphasisTokenIndices, italicsEmphasisTokenIndices);
		
		//	try and cut off bibliography (if any), as journal names are italicized in quite a few reference styles
		pm.setStep("Eliminating seeds from document head and bibliography");
		pm.setBaseProgress(10);
		pm.setProgress(0);
		pm.setMaxProgress(15);
		int docHeadCutoff = 0;
		Annotation[] emphases = doc.getAnnotations(EMPHASIS_TYPE);
		for (int e = 0; e < emphases.length; e++) {
			if ((emphases[e].getStartIndex() * 10) > doc.size())
				break;
			if ("Abstract".equalsIgnoreCase(emphases[e].firstValue()) || "Summary".equalsIgnoreCase(emphases[e].firstValue()) || "Introduction".equalsIgnoreCase(emphases[e].firstValue())) {
				docHeadCutoff = emphases[e].getStartIndex();
				break;
			}
		}
		if (0 < docHeadCutoff) {
			pm.setInfo("Eliminating non-family seeds last found before " + docHeadCutoff);
			seedList.clear();
			for (int s = 0; s < seeds.length; s++) {
				if (seeds[s].lastOccurrenceIndex > docHeadCutoff)
					seedList.add(seeds[s]);
				else if (seeds[s].epithet.endsWith("idae") || seeds[s].epithet.endsWith("aceae"))
					seedList.add(seeds[s]);
				else pm.setInfo(" - eliminating " + seeds[s]);
			}
			if (seedList.size() < seeds.length)
				seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
			pm.setInfo(seeds.length + " seeds remaining after document head cutoff");
		}
		int bibliographyCutoff = doc.size();
		Annotation[] headings = doc.getAnnotations(HEADING_TYPE);
		for (int h = 0; h < headings.length; h++) {
			if ((headings[h].getStartIndex() * 3) < (doc.size() * 2))
				continue;
			if ("References".equalsIgnoreCase(headings[h].firstValue()) || "Literature".equalsIgnoreCase(headings[h].firstValue()) || "Bibliography".equalsIgnoreCase(headings[h].firstValue()))
				bibliographyCutoff = Math.min(headings[h].getStartIndex(), bibliographyCutoff);
		}
		for (int s = 0; s < seeds.length; s++) {
			if ((seeds[s].firstOccurrenceIndex * 4) < (doc.size() * 3))
				continue;
			if ("References".equalsIgnoreCase(seeds[s].epithet) || "Literature".equalsIgnoreCase(seeds[s].epithet) || "Bibliography".equalsIgnoreCase(seeds[s].epithet))
				bibliographyCutoff = Math.min(seeds[s].firstOccurrenceIndex, bibliographyCutoff);
		}
		if (bibliographyCutoff < doc.size()) {
			pm.setInfo("Eliminating seeds first found after " + bibliographyCutoff);
			seedList.clear();
			for (int s = 0; s < seeds.length; s++) {
				if (seeds[s].firstOccurrenceIndex < bibliographyCutoff)
					seedList.add(seeds[s]);
				else pm.setInfo(" - eliminating " + seeds[s]);
			}
			if (seedList.size() < seeds.length)
				seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
			pm.setInfo(seeds.length + " seeds remaining after bibliography cutoff");
		}
		
		//	filter out stop words
		pm.setStep("Eliminating stop words");
		pm.setBaseProgress(15);
		pm.setProgress(0);
		pm.setMaxProgress(18);
		seedList.clear();
		for (int s = 0; s < seeds.length; s++) {
			if (!this.treeFat.stopWords.contains(seeds[s].epithet))
				seedList.add(seeds[s]);
			else pm.setInfo(" - eliminating " + seeds[s]);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		pm.setInfo(seeds.length + " seeds remaining after stop word elimination");
		
		//	filter seeds based on specific endings
		pm.setStep("Eliminating seeds with inappropriate suffixes");
		pm.setBaseProgress(18);
		pm.setProgress(0);
		pm.setMaxProgress(20);
		seedList.clear();
		for (int s = 0; s < seeds.length; s++) {
			boolean retain = true;
			for (int f = 0; f < this.stopSuffixes.length; f++)
				if (seeds[s].epithet.toLowerCase().endsWith(this.stopSuffixes[f])) {
					retain = false;
					break;
				}
			if (retain)
				seedList.add(seeds[s]);
			else pm.setInfo(" - eliminating " + seeds[s]);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		pm.setInfo(seeds.length + " seeds remaining after suffix filtering");
		
		//	evaluate stop word selectors
		Set docLocalStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int s = 0; s < this.stopWordSelectors.length; s++) try {
			Annotation[] stopWords = GPath.evaluatePath(doc, this.stopWordSelectors[s], null);
			for (int w = 0; w < stopWords.length; w++)
				docLocalStopWords.add(stopWords[w].firstValue());
		}
		catch (RuntimeException re) {
			re.printStackTrace(System.out);
		}
		
		//	subtract document local stop words from authority stop words (author names, etc.)
		TreeSet authorityStopWords = new TreeSet(this.authorityStopWords);
		authorityStopWords.removeAll(docLocalStopWords);
		
		//	filter out document local stop words
		pm.setStep("Eliminating document local stop words");
		pm.setBaseProgress(20);
		pm.setProgress(0);
		pm.setMaxProgress(23);
		seedList.clear();
		for (int s = 0; s < seeds.length; s++) {
			if (!docLocalStopWords.contains(seeds[s].epithet))
				seedList.add(seeds[s]);
			else pm.setInfo(" - eliminating " + seeds[s]);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		pm.setInfo(seeds.length + " seeds remaining after document local stop word elimination");
		
		//	filter out seeds that are not identified by their suffix and don't occur at the start of an emphasis (epithets of above-family primary ranks will come back from lookup result higher taxonomies)
		ArrayList styleEliminatedSeedList = new ArrayList();
		if (seedsFromEmphases) {
			
			//	get document style to find out which of bold and italics to check for
			DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
			DocumentStyle taxonNameStyle = docStyle.getSubset(TAXONOMIC_NAME_ANNOTATION_TYPE);
			boolean dsBinomialsAreBold = taxonNameStyle.getBooleanProperty("binomialsAreBold", false);
			boolean dsBinomialsAreItalics = taxonNameStyle.getBooleanProperty("binomialsAreItalics", false);
			
			//	do filtering
			pm.setStep("Eliminating non-emphasis-start words");
			seedList.clear();
			for (int s = 0; s < seeds.length; s++) {
				if (seeds[s].epithet.toLowerCase().endsWith("idae") || seeds[s].epithet.toLowerCase().endsWith("aceae"))
					seedList.add(seeds[s]);
				else if (seeds[s].epithet.toLowerCase().endsWith("inae") || seeds[s].epithet.toLowerCase().endsWith("oideae"))
					seedList.add(seeds[s]);
				else if (seeds[s].epithet.toLowerCase().endsWith("ini") || seeds[s].epithet.toLowerCase().endsWith("eae"))
					seedList.add(seeds[s]);
				else if (seeds[s].epithet.toLowerCase().endsWith("ina") || seeds[s].epithet.toLowerCase().endsWith("inae"))
					seedList.add(seeds[s]);
				else if (dsBinomialsAreBold && (seeds[s].boldStartCount == 0)) {
					styleEliminatedSeedList.add(seeds[s]);
					pm.setInfo(" - eliminating (for no bold starts) " + seeds[s]);
				}
				else if (dsBinomialsAreItalics && (seeds[s].italicsStartCount == 0)) {
					styleEliminatedSeedList.add(seeds[s]);
					pm.setInfo(" - eliminating (for no italics starts) " + seeds[s]);
				}
				else if ((seeds[s].boldStartCount + seeds[s].italicsStartCount) != 0)
					seedList.add(seeds[s]);
				else {
					styleEliminatedSeedList.add(seeds[s]);
					pm.setInfo(" - eliminating (for no emphasis starts) " + seeds[s]);
				}
			}
			if (seedList.size() < seeds.length)
				seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
			pm.setInfo(seeds.length + " seeds remaining after emphasis start elimination");
		}
		
		//	index document word occurrences by lower and capitalized case
		CountingSet docWordsByCase = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		for (int t = docHeadCutoff; t < bibliographyCutoff; t++) {
			String docWord = doc.valueAt(t);
			if (Gamta.isPunctuation(docWord) || Gamta.isNumber(docWord))
				continue;
			char docWordStart = docWord.charAt(0);
			if (Character.isUpperCase(docWordStart))
				docWordsByCase.add(("C:" + docWord));
			else if (Character.isLowerCase(docWordStart))
				docWordsByCase.add(("L:" + docWord));
		}
		
		//	filter seeds that occur in lower case more often than in capitalized
		pm.setStep("Eliminating seeds occurring in lower case more often than capitalized");
		pm.setBaseProgress(23);
		pm.setProgress(0);
		pm.setMaxProgress(24);
		seedList.clear();
		for (int s = 0; s < seeds.length; s++) {
			int lcCount = docWordsByCase.getCount("L:" + seeds[s].epithet);
			int ccCount = docWordsByCase.getCount("C:" + seeds[s].epithet);
			if ((lcCount == 0) || (lcCount < ccCount))
				seedList.add(seeds[s]);
			else pm.setInfo(" - eliminating (" + lcCount + "L/" + ccCount + "C) " + seeds[s]);
		}
		if (seedList.size() < seeds.length)
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
		pm.setInfo(seeds.length + " seeds remaining after case frequency elimination");
		
		//	sort seeds by frequency
		Arrays.sort(seeds, new Comparator() {
			public int compare(Object o1, Object o2) {
				Seed s1 = ((Seed) o1);
				Seed s2 = ((Seed) o2);
				return ((s1.count == s2.count) ? s1.epithet.compareTo(s2.epithet) : (s2.count - s1.count));
			}
		});
		
		//	collect labeled positives ...
		pm.setStep("Collecting labeled positives");
		pm.setBaseProgress(24);
		pm.setProgress(0);
		pm.setMaxProgress(25);
		AnnotationIndex labeledPositivePartIndex = new AnnotationIndex(doc, null);
		this.treeFat.indexStatusLabels(doc, labeledPositivePartIndex);
		TreeSet seedEpithetDict = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int s = 0; s < seeds.length; s++)
			seedEpithetDict.add(seeds[s].epithet);
		labeledPositivePartIndex.addAnnotations(Gamta.extractAllContained(doc, new SetDictionary(seedEpithetDict)), "seedGenus");
		TreeSet labeledPositives = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		MatchTree[] labeledPositiveMatches;
		
		//	... for genera ...
		labeledPositiveMatches = AnnotationPatternMatcher.getMatchTrees(doc, labeledPositivePartIndex,
				"<seedGenus>" +
				" ','? <newLabel test=\"./@rank = 'genus'\">");
		for (int p = 0; p < labeledPositiveMatches.length; p++) {
			if (labeledPositives.add(labeledPositiveMatches[p].getMatch().firstValue()))
				pm.setInfo(" - got labeled positive seed " + labeledPositiveMatches[p].getMatch().firstValue());
		}
		
		//	... as well as species ...
		labeledPositiveMatches = AnnotationPatternMatcher.getMatchTrees(doc, labeledPositivePartIndex,
				"<seedGenus>" +
				" \"[a-z]{4,}\"" +
				" ','? <newLabel test=\"./@rank = 'species'\">");
		for (int p = 0; p < labeledPositiveMatches.length; p++) {
			if (labeledPositives.add(labeledPositiveMatches[p].getMatch().firstValue()))
				pm.setInfo(" - got labeled positive seed " + labeledPositiveMatches[p].getMatch().firstValue());
		}
		
		//	... and new species combinations ...
		labeledPositiveMatches = AnnotationPatternMatcher.getMatchTrees(doc, labeledPositivePartIndex,
				"<seedGenus>" +
				" \"[a-z]{4,}\"" +
				" ','? <newLabel test=\"not(./@rank)\">"); // TODO use rank 'comb'
		for (int p = 0; p < labeledPositiveMatches.length; p++) {
			if (labeledPositives.add(labeledPositiveMatches[p].getMatch().firstValue()))
				pm.setInfo(" - got labeled positive seed " + labeledPositiveMatches[p].getMatch().firstValue());
		}
		
		//	lookup higher taxonomy for seeds
		pm.setStep("Doing catalog lookups for " + seeds.length + " seeds:");
		for (int s = 0; s < seeds.length; s++)
			pm.setInfo(" - " + seeds[s]);
		pm.setBaseProgress(25);
		pm.setProgress(0);
		pm.setMaxProgress(50);
		this.addHigherTaxonomies(seeds, labeledPositives, pm);
		
		//	index higher hierarchies by epithets, and count epithet frequencies
		pm.setStep("Assessing catalog lookup results");
		pm.setBaseProgress(50);
		pm.setProgress(0);
		pm.setMaxProgress(52);
		CountingSet epithetFrequencies = new CountingSet();
		TreeMap epithetsToPotentialHigherTaxonomies = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap ancestorEpithetsToPotentialHigherTaxonomies = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap nonCatalogPotentialGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap nonCatalogGeneraToFamilies = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap nonPrimaryRankEpithets = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		Properties catalogSpeciesToGenera = new Properties();
		seeds = this.assessHigherTaxonomyLookupResult(seeds, epithetFrequencies, nonPrimaryRankEpithets, nonCatalogPotentialGenera, epithetsToPotentialHigherTaxonomies, ancestorEpithetsToPotentialHigherTaxonomies);
		pm.setInfo(seeds.length + " seeds remaining after catalog lookups");
		
		//	build dictionaries from seeds, amended with lists of know child epithets from catalogs
		pm.setStep("Building primary rank dictionaries");
		pm.setBaseProgress(52);
		pm.setProgress(0);
		pm.setMaxProgress(53);
		StringVector orderDict = new StringVector();
		StringVector familyDict = new StringVector();
		StringVector genusDict = new StringVector();
		StringVector speciesDict = new StringVector();
		StringVector genusAndAboveEpithetDict = new StringVector();
		this.updatePrimaryRankDicts(seeds, genusAndAboveEpithetDict, orderDict, familyDict, genusDict, speciesDict, epithetsToPotentialHigherTaxonomies, catalogSpeciesToGenera);
		
		//	salvage seeds (primarily genera) that were eliminated based on style (e.g. on-italics), but came back as catalog children of families
		if (styleEliminatedSeedList.size() != 0) {
			ArrayList salvagedSeedList = new ArrayList();
			
			//	check style eliminated seeds against dictionaries built from initial catalog lookup
			pm.setStep("Salvaging style eliminated seeds using lookup results");
			pm.setBaseProgress(53);
			pm.setProgress(0);
			pm.setMaxProgress(54);
			salvagedSeedList.clear();
			for (int s = 0; s < styleEliminatedSeedList.size(); s++) {
				Seed styleEliminatedSeed = ((Seed) styleEliminatedSeedList.get(s));
				if (orderDict.containsIgnoreCase(styleEliminatedSeed.epithet)) {
					pm.setInfo(" - salvaged catalog validated order " + styleEliminatedSeed.epithet);
					salvagedSeedList.add(styleEliminatedSeed);
					styleEliminatedSeedList.remove(s--);
				}
				else if (genusDict.containsIgnoreCase(styleEliminatedSeed.epithet)) {
					pm.setInfo(" - salvaged catalog validated genus " + styleEliminatedSeed.epithet);
					salvagedSeedList.add(styleEliminatedSeed);
					styleEliminatedSeedList.remove(s--);
				}
			}
			Seed[] catalogSalvagedSeeds = ((Seed[]) salvagedSeedList.toArray(new Seed[salvagedSeedList.size()]));
			
			//	lookup higher taxonomy for seeds
			pm.setStep("Doing catalog lookups for " + catalogSalvagedSeeds.length + " catalog salvaged seeds:");
			for (int s = 0; s < catalogSalvagedSeeds.length; s++)
				pm.setInfo(" - " + catalogSalvagedSeeds[s]);
			pm.setBaseProgress(54);
			pm.setProgress(0);
			pm.setMaxProgress(56);
			this.addHigherTaxonomies(catalogSalvagedSeeds, labeledPositives, pm);
			
			//	index higher hierarchies by epithets, and count epithet frequencies
			catalogSalvagedSeeds = this.assessHigherTaxonomyLookupResult(catalogSalvagedSeeds, epithetFrequencies, nonPrimaryRankEpithets, nonCatalogPotentialGenera, epithetsToPotentialHigherTaxonomies, ancestorEpithetsToPotentialHigherTaxonomies);
			pm.setInfo(catalogSalvagedSeeds.length + " catalog salvaged seeds remaining after catalog lookups");
			
			//	add salvaged seeds to dictionaries, amended with lists of know child epithets from catalogs
			pm.setStep("Extending primary rank dictionaries");
			pm.setBaseProgress(56);
			pm.setProgress(0);
			pm.setMaxProgress(57);
			this.updatePrimaryRankDicts(catalogSalvagedSeeds, genusAndAboveEpithetDict, orderDict, familyDict, genusDict, speciesDict, epithetsToPotentialHigherTaxonomies, catalogSpeciesToGenera);
			
			//	assess style of catalog salvaged seeds
			int cssBoldCount = 0;
			int cssItalicsCount = 0;
			int cssAllCapsCount = 0;
			for (int s = 0; s < catalogSalvagedSeeds.length; s++) {
				if (catalogSalvagedSeeds[s].boldCount != 0)
					cssBoldCount++;
				if (catalogSalvagedSeeds[s].italicsCount != 0)
					cssItalicsCount++;
				if (catalogSalvagedSeeds[s].allCapsCount != 0)
					cssAllCapsCount++;
			}
			
			//	check remaining style eliminated seeds against style of catalog salvaged ones
			pm.setStep("Salvaging style eliminated seeds using style of catalog salvaged seeds");
			pm.setBaseProgress(57);
			pm.setProgress(0);
			pm.setMaxProgress(58);
			salvagedSeedList.clear();
			for (int s = 0; s < styleEliminatedSeedList.size(); s++) {
				Seed styleEliminatedSeed = ((Seed) styleEliminatedSeedList.get(s));
				int sesScore = 0;
				if ((cssBoldCount * 4) > (catalogSalvagedSeeds.length * 3) && (styleEliminatedSeed.boldCount != 0))
					sesScore++;
				if ((cssItalicsCount * 4) > (catalogSalvagedSeeds.length * 3) && (styleEliminatedSeed.italicsCount != 0))
					sesScore++;
				if ((cssAllCapsCount * 4) > (catalogSalvagedSeeds.length * 3) && (styleEliminatedSeed.allCapsCount != 0))
					sesScore++;
				if (sesScore >= 2) {
					pm.setInfo(" - salvaged seed " + styleEliminatedSeed.epithet + " for style matching catalog salvaged ones");
					salvagedSeedList.add(styleEliminatedSeed);
					styleEliminatedSeedList.remove(s--);
				}
			}
			Seed[] styleSalvagedSeeds = ((Seed[]) salvagedSeedList.toArray(new Seed[salvagedSeedList.size()]));
			
			//	lookup higher taxonomy for seeds
			pm.setStep("Doing catalog lookups for " + styleSalvagedSeeds.length + " style salvaged seeds:");
			for (int s = 0; s < styleSalvagedSeeds.length; s++)
				pm.setInfo(" - " + styleSalvagedSeeds[s]);
			pm.setBaseProgress(58);
			pm.setProgress(0);
			pm.setMaxProgress(59);
			this.addHigherTaxonomies(styleSalvagedSeeds, labeledPositives, pm);
			
			//	index higher hierarchies by epithets, and count epithet frequencies
			styleSalvagedSeeds = this.assessHigherTaxonomyLookupResult(styleSalvagedSeeds, epithetFrequencies, nonPrimaryRankEpithets, nonCatalogPotentialGenera, epithetsToPotentialHigherTaxonomies, ancestorEpithetsToPotentialHigherTaxonomies);
			pm.setInfo(styleSalvagedSeeds.length + " style salvaged seeds remaining after catalog lookups");
			
			//	add salvaged seeds to dictionaries, amended with lists of know child epithets from catalogs
			pm.setStep("Extending primary rank dictionaries");
			pm.setBaseProgress(59);
			pm.setProgress(0);
			pm.setMaxProgress(60);
			this.updatePrimaryRankDicts(styleSalvagedSeeds, genusAndAboveEpithetDict, orderDict, familyDict, genusDict, speciesDict, epithetsToPotentialHigherTaxonomies, catalogSpeciesToGenera);
			
			//	add salvaged seeds to others, and restore sort order
			seedList.clear();
			seedList.addAll(Arrays.asList(seeds));
			seedList.addAll(Arrays.asList(catalogSalvagedSeeds));
			seedList.addAll(Arrays.asList(styleSalvagedSeeds));
			seeds = ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
			Arrays.sort(seeds, new Comparator() {
				public int compare(Object o1, Object o2) {
					Seed s1 = ((Seed) o1);
					Seed s2 = ((Seed) o2);
					return ((s1.count == s2.count) ? s1.epithet.compareTo(s2.epithet) : (s2.count - s1.count));
				}
			});
		}
		
		//	make higher taxonomies into arrays for easier handling
		for (Iterator aeit = ancestorEpithetsToPotentialHigherTaxonomies.keySet().iterator(); aeit.hasNext();) {
			String ancestorEpithet = ((String) aeit.next());
			if (epithetsToPotentialHigherTaxonomies.containsKey(ancestorEpithet))
				continue;
			ArrayList ancestorHigherTaxonomies = ((ArrayList) ancestorEpithetsToPotentialHigherTaxonomies.get(ancestorEpithet));
			epithetsToPotentialHigherTaxonomies.put(ancestorEpithet, ((Properties[]) ancestorHigherTaxonomies.toArray(new Properties[ancestorHigherTaxonomies.size()])));
		}
		
		//	what do we got?
		System.out.println("Got " + seeds.length + " seeds with catalog lookup results:");
		for (int s = 0; s < seeds.length; s++) {
			System.out.println(" - " + seeds[s]);
			for (int t = 0; t < seeds[s].higherTaxonomies.length; t++)
				System.out.println("   " + seeds[s].higherTaxonomies[t]);
			if (seeds[s].childEpithets != null)
				System.out.println("   " + seeds[s].childEpithets);
		}
		
		//	remove from local dictionaries all words that occur in lower case more often than capitalized
		for (int o = 0; o < orderDict.size(); o++) {
			String order = orderDict.get(o);
			int lcCount = docWordsByCase.getCount("L:" + order);
			int ccCount = docWordsByCase.getCount("C:" + order);
			if (ccCount < lcCount)
				orderDict.remove(o--);
		}
		for (int f = 0; f < familyDict.size(); f++) {
			String family = familyDict.get(f);
			int lcCount = docWordsByCase.getCount("L:" + family);
			int ccCount = docWordsByCase.getCount("C:" + family);
			if (ccCount < lcCount)
				familyDict.remove(f--);
		}
		for (int g = 0; g < genusDict.size(); g++) {
			String genus = genusDict.get(g);
			int lcCount = docWordsByCase.getCount("L:" + genus);
			int ccCount = docWordsByCase.getCount("C:" + genus);
			if (ccCount < lcCount)
				genusDict.remove(g--);
		}
		for (Iterator pgit = nonCatalogPotentialGenera.keySet().iterator(); pgit.hasNext();) {
			String genus = ((String) pgit.next());
			int lcCount = docWordsByCase.getCount("L:" + genus);
			int ccCount = docWordsByCase.getCount("C:" + genus);
			if (ccCount < lcCount)
				pgit.remove();
		}
		
		//	collect what we have so far
		pm.setStep("Indexing epithet occurrences");
		pm.setBaseProgress(55);
		pm.setProgress(0);
		pm.setMaxProgress(60);
		AnnotationIndex taxonNamePartIndex = new AnnotationIndex(doc, null);
		
		//	collect orders, families, genera, and species in AnnotationIndex (as well as genus abbreviations)
		Annotation[] orders = Gamta.extractAllContained(doc, new CaseHalfSensitiveDictionary(orderDict), 1);
		taxonNamePartIndex.addAnnotations(orders, ORDER_ATTRIBUTE);
		Annotation[] families = Gamta.extractAllContained(doc, new CaseHalfSensitiveDictionary(familyDict), 1);
		taxonNamePartIndex.addAnnotations(families, FAMILY_ATTRIBUTE);
		Annotation[] genera = Gamta.extractAllContained(doc, new CaseHalfSensitiveDictionary(genusDict), 1);
		taxonNamePartIndex.addAnnotations(genera, GENUS_ATTRIBUTE);
		for (int s = 0; s < seeds.length; s++) {
			if (!GENUS_ATTRIBUTE.equals(seeds[s].rank))
				continue;
			String abbrevGenusRegEx = (seeds[s].epithet.substring(0, 1) + "[" + seeds[s].epithet.substring(1).replaceAll("[e\\-\\']", "") + "]?\\.?");
			Annotation[] abbrevGenera = Gamta.extractAllMatches(doc, abbrevGenusRegEx, 2);
			taxonNamePartIndex.addAnnotations(abbrevGenera, (TreeFAT.ABBREVIATED_EPITHET_PREFIX + GENUS_ATTRIBUTE));
		}
		Annotation[] species = Gamta.extractAllContained(doc, speciesDict, 1);
		taxonNamePartIndex.addAnnotations(species, SPECIES_ATTRIBUTE);
		
		//	collect labeled below-species epithets, as well as non-labeled potential ones
		pm.setStep("Indexing labeled epithets and potential epithets");
		pm.setBaseProgress(60);
		pm.setProgress(0);
		pm.setMaxProgress(65);
		this.treeFat.indexLabeledAndPotentialEpithets(doc, taxonNamePartIndex);
		
		//	collect name labels
		pm.setStep("Indexing taxonomic status labels");
		pm.setBaseProgress(65);
		pm.setProgress(0);
		pm.setMaxProgress(67);
		this.treeFat.indexStatusLabels(doc, taxonNamePartIndex);
		
		//	collect authorities
		pm.setStep("Indexing potential authorities");
		pm.setBaseProgress(67);
		pm.setProgress(0);
		pm.setMaxProgress(69);
		this.treeFat.indexAuthorities(doc, taxonNamePartIndex, genusAndAboveEpithetDict, countryRegionStartDict, authorityStopWords, true);
		
		//	collect in-name symbols for bridging
		pm.setStep("Indexing name infixes and in-name symbols");
		pm.setBaseProgress(69);
		pm.setProgress(0);
		pm.setMaxProgress(70);
		this.treeFat.indexInfixesAndInNameSymbols(doc, taxonNamePartIndex);
		
		//	annotate possible genera and sub genera
		System.out.println("NonCatalog potential genera: " + nonCatalogPotentialGenera);
		Annotation[] potentialGenera = Gamta.extractAllContained(doc, new MapDictionary(nonCatalogPotentialGenera));
		taxonNamePartIndex.addAnnotations(potentialGenera, (TreeFAT.POTENTIAL_EPITHET_PREFIX + GENUS_ATTRIBUTE));
		Annotation[] subGenera = Gamta.extractAllMatches(doc, "[A-Z][a-z]{3,}");
		taxonNamePartIndex.addAnnotations(subGenera, SUBGENUS_ATTRIBUTE);
		Annotation[] abbrevSubGenera = Gamta.extractAllMatches(doc, "[A-Z][a-z]?\\.");
		taxonNamePartIndex.addAnnotations(abbrevSubGenera, (TreeFAT.ABBREVIATED_EPITHET_PREFIX + SUBGENUS_ATTRIBUTE));
		
		//	use annotation patterns to assemble full names (optional sub genus, etc. !!!)
		pm.setStep("Assembling taxonomic names");
		StringVector docGenusDict = new StringVector();
		StringVector docSpeciesDict = new StringVector();
		StringVector docSubSpeciesDict = new StringVector();
		StringVector docVarietyDict = new StringVector();
		StringVector docAuthorityDict = new StringVector();
		HashSet annotatedTaxonNameKeys = new HashSet();
		TreeMap annotatedTaxonNameStrings = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		MatchTree[] taxonNameMatches;
		boolean binomialsAreBold = false;
		boolean binomialsAreItalics = false;
		TreeSet catalogGeneraWithSpecies = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		boolean assessBinomialStyleAndExtendAuthorities = true;
		
		//	we need to run this twice: first for style-filtering catalog genera and extending authorities, then to include extended authorities in tagging
		while (true) {
			if (assessBinomialStyleAndExtendAuthorities) {
				pm.setInfo("Marking orders");
				pm.setBaseProgress(70);
				pm.setProgress(0);
				pm.setMaxProgress(71);
			}
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<order> (','? <authority>)?");
			for (int t = 0; t < taxonNameMatches.length; t++)
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], null, tableCellEndIndices, pm, "order");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				}
			
			if (assessBinomialStyleAndExtendAuthorities) {
				pm.setInfo("Marking families");
				pm.setBaseProgress(71);
				pm.setProgress(0);
				pm.setMaxProgress(72);
			}
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<family>" +
					" (','? <authority>)?");
			for (int t = 0; t < taxonNameMatches.length; t++)
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], null, tableCellEndIndices, pm, "family");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				}
			
			//	mark standalone non-primary rank taxon names
			if (assessBinomialStyleAndExtendAuthorities)
				pm.setInfo("Marking above-genus taxa of non-primary ranks");
			Annotation[] nonPrimaryRankTaxonNames = Gamta.extractAllContained(doc, new MapDictionary(nonPrimaryRankEpithets));
			for (int t = 0; t < nonPrimaryRankTaxonNames.length; t++)
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(nonPrimaryRankTaxonNames[t]))) {
					Annotation taxonName = doc.addAnnotation(TAXONOMIC_NAME_ANNOTATION_TYPE, nonPrimaryRankTaxonNames[t].getStartIndex(), nonPrimaryRankTaxonNames[t].size());
					if (taxonName == null)
						continue;
					pm.setInfo(taxonName.getValue() + " at " + taxonName.getStartIndex());
					taxonName.setAttribute("_step", "nonPrimaryHigher");
					taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					
					//	get seed to add rank and lowest primary rank ancestor
					Seed taxonNameSeed = ((Seed) nonPrimaryRankEpithets.get(taxonName.getValue()));
					if (taxonNameSeed == null)
						continue;
					taxonName.setAttribute(RANK_ATTRIBUTE, taxonNameSeed.rank);
					taxonName.setAttribute(taxonNameSeed.rank, taxonNameSeed.epithet);
					for (int h = 0; h < taxonNameSeed.higherTaxonomies.length; h++) {
						for (int r = (TreeFAT.primaryRankNames.length - 1); r >= 0; r--)
							if (taxonNameSeed.higherTaxonomies[h].containsKey(TreeFAT.primaryRankNames[r])) {
								taxonName.setAttribute(TreeFAT.primaryRankNames[r], taxonNameSeed.higherTaxonomies[h].getProperty(TreeFAT.primaryRankNames[r]));
								h = taxonNameSeed.higherTaxonomies.length;
								break;
							}
					}
				}
			
			if (assessBinomialStyleAndExtendAuthorities) {
				pm.setInfo("Marking subgenera");
				pm.setBaseProgress(72);
				pm.setProgress(0);
				pm.setMaxProgress(73);
			}
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<genus>" +
					" ('(' <subGenus> ')')" +
					" <inNameSymbol>?" +
					" ','?" +
					" <authority>" +
					" ((','|':')? (<newLabel>|<sensuLabel>))?");
			for (int t = 0; t < taxonNameMatches.length; t++)
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					
					//	get subgenus epithet & check if spelled out
					Annotation subGenusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], SUBGENUS_ATTRIBUTE);
					if ((subGenusAnnot == null) || (subGenusAnnot.firstValue().length() < 3))
						continue;
					
					//	annotate taxon name and set meta attributes
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBGENUS_ATTRIBUTE, tableCellEndIndices, pm, "subGenus");
					if (taxonName == null)
						continue;
					if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "statusLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "sensuLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				}
			
			if (assessBinomialStyleAndExtendAuthorities) {
				pm.setInfo("Marking species and lower rank names");
				pm.setBaseProgress(73);
				pm.setProgress(0);
				pm.setMaxProgress(74);
			}
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<genus>" +
					" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
					" <nameInfix>?" +
					" <species>" +
					" (','? <authority>? <subSpecies>)?" +
					" (','? <authority>? <variety>)?" +
					" <inNameSymbol>?" +
					" ((','|':')? <authority>)?" +
					" ((','|':')? (<newLabel>|<sensuLabel>))?");
			for (int t = 0; t < taxonNameMatches.length; t++) {
//				System.out.println("S1: " + taxonNameMatches[t].toString("  "));
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "species1");
					if (taxonName == null) {
//						System.out.println(" ==> failed to annotate");
						continue;
					}
//					System.out.println(" ==> annotated");
					if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "statusLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "sensuLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(VARIETY_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "varietyLabel");
					else if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "subSpeciesLabel");
					else taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
				}
//				else System.out.println(" ==> annotated before");
			}
			if (assessBinomialStyleAndExtendAuthorities) {
				pm.setBaseProgress(74);
				pm.setProgress(0);
				pm.setMaxProgress(75);
			}
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<genus>" +
					" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
					" <nameInfix>?" +
					" <species>" +
					" (','? <authority>? <subSpecies>)?" +
					" (','? <authority>? <variety>)?" +
					" <inNameSymbol>?" +
					" ((','|':')? (<newLabel>|<sensuLabel>|<authority>))?");
			for (int t = 0; t < taxonNameMatches.length; t++) {
//				System.out.println("S2: " + taxonNameMatches[t].toString("  "));
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "species2");
					if (taxonName == null) {
//						System.out.println(" ==> failed to annotate");
						continue;
					}
//					System.out.println(" ==> annotated");
					if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "statusLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "sensuLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(VARIETY_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "varietyLabel");
					else if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "subSpeciesLabel");
					else taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
					updateDocAuthorityDict(taxonName, docAuthorityDict);
				}
//				else System.out.println(" ==> annotated before");
			}
			
			//	assess font style of taxon names, and collect genera occurring in cataloged binomials
			if (assessBinomialStyleAndExtendAuthorities) {
				Annotation[] catalogTaxonNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
				CountingSet catalogTaxonNameStyles = new CountingSet();
				for (int t = 0; t < catalogTaxonNames.length; t++) {
					System.out.println("Testing for style: " + catalogTaxonNames[t].toXML());
					if (catalogTaxonNames[t].size() < 2) {
						System.out.println(" ==> too few tokens");
						continue;
					}
					if (catalogTaxonNames[t].firstValue().length() < 3) {
						System.out.println(" ==> abbreviated first token");
						continue;
					}
					if (!catalogTaxonNames[t].hasAttribute(SPECIES_ATTRIBUTE)) {
						System.out.println(" ==> no species epithet");
						continue;
					}
					catalogGeneraWithSpecies.add(catalogTaxonNames[t].firstValue());
					if (boldEmphasisTokenIndices.contains(new Integer(catalogTaxonNames[t].getStartIndex()))) {
						catalogTaxonNameStyles.add("B");
						System.out.println(" ==> bold");
					}
					else {
						catalogTaxonNameStyles.add("NB");
						System.out.println(" ==> non-bold");
					}
					if (italicsEmphasisTokenIndices.contains(new Integer(catalogTaxonNames[t].getStartIndex()))) {
						catalogTaxonNameStyles.add("I");
						System.out.println(" ==> italics");
					}
					else {
						catalogTaxonNameStyles.add("NI");
						System.out.println(" ==> non-italics");
					}
				}
				binomialsAreBold = ((catalogTaxonNameStyles.getCount("NB") * 5) < catalogTaxonNameStyles.getCount("B"));
				pm.setInfo("Found binomials to be " + (binomialsAreBold ? "" : "non-") + "bold (" + catalogTaxonNameStyles.getCount("NB") + "/" + catalogTaxonNameStyles.getCount("B") + ")");
				binomialsAreItalics = ((catalogTaxonNameStyles.getCount("NI") * 5) < catalogTaxonNameStyles.getCount("I"));
				pm.setInfo("Found binomials to be " + (binomialsAreItalics ? "" : "non-") + "italics (" + catalogTaxonNameStyles.getCount("NI") + "/" + catalogTaxonNameStyles.getCount("I") + ")");
				
				//	check document style template in addition to findings
				DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
				DocumentStyle taxonNameStyle = docStyle.getSubset(TAXONOMIC_NAME_ANNOTATION_TYPE);
				boolean dsBinomialsAreBold = taxonNameStyle.getBooleanProperty("binomialsAreBold", false);
				if (!binomialsAreBold && dsBinomialsAreBold) {
					pm.setInfo("Document style specifies binomials to be bold");
					binomialsAreBold = true;
				}
				boolean dsBinomialsAreItalics = taxonNameStyle.getBooleanProperty("binomialsAreItalics", false);
				if (!binomialsAreItalics && dsBinomialsAreItalics) {
					pm.setInfo("Document style specifies binomials to be italics");
					binomialsAreItalics = true;
				}
			}
			
			//	mark standalone genera from catalogs
			boolean gotAuthoritiesToExtend = false;
			if (assessBinomialStyleAndExtendAuthorities) {
				pm.setInfo("Marking genera");
				pm.setBaseProgress(75);
				pm.setProgress(0);
				pm.setMaxProgress(76);
			}
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<genus>" +
					" <inNameSymbol>?" +
					" ((','|':')? (<newLabel>|<sensuLabel>|<authority>))?");
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	if length if 1 (plain genus catalog match) compare style (there is a Lepidoptera genus names 'Data', for instance ...)
				Annotation genusTaxonName = taxonNameMatches[t].getMatch();
				if ((genusTaxonName.size() == 1) && !catalogGeneraWithSpecies.contains(genusTaxonName.firstValue())) {
					if (binomialsAreBold && !boldEmphasisTokenIndices.contains(new Integer(genusTaxonName.getStartIndex()))) {
						pm.setInfo("Standalone catalog genus filtered for non-bold style: " + genusTaxonName.firstValue());
						if ((genusTaxonName.getStartIndex() > docHeadCutoff) && (genusTaxonName.getStartIndex() < bibliographyCutoff)) {
							genusAndAboveEpithetDict.removeAll(genusTaxonName.firstValue());
							gotAuthoritiesToExtend = true;
						}
						continue;
					}
					if (binomialsAreItalics && !italicsEmphasisTokenIndices.contains(new Integer(genusTaxonName.getStartIndex()))) {
						pm.setInfo("Standalone catalog genus filtered for non-italics style: " + genusTaxonName.firstValue());
						if ((genusTaxonName.getStartIndex() > docHeadCutoff) && (genusTaxonName.getStartIndex() < bibliographyCutoff)) {
							genusAndAboveEpithetDict.removeAll(genusTaxonName.firstValue());
							gotAuthoritiesToExtend = true;
						}
						continue;
					}
				}
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], null, tableCellEndIndices, pm, "genera");
					if (taxonName == null)
						continue;
					if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "statusLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "sensuLabel");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else taxonName.setAttribute("_evidence", "catalogs");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				}
			}
			
			//	we're in the first run, extend authorities and loop back (if anything to extend)
			if (assessBinomialStyleAndExtendAuthorities && gotAuthoritiesToExtend) {
				this.treeFat.indexAuthorities(doc, taxonNamePartIndex, genusAndAboveEpithetDict, countryRegionStartDict, authorityStopWords, true);
				assessBinomialStyleAndExtendAuthorities = false;
			}
			
			//	second run, we're done here
			else break;
		}
		
		//	if binomials use distinctive font style, mark non-catalog genera and sub genera labeled as new
		if (binomialsAreBold || binomialsAreItalics || seedsFromEmphases) {
			TreeMap labeledGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			pm.setInfo("Marking genera and sub genera labeled as new taxa");
			pm.setBaseProgress(76);
			pm.setProgress(0);
			pm.setMaxProgress(77);
			
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" (','? <authority>)?" +
					" ','? <newLabel test=\"./@rank = 'genus'\">");
			for (int t = 0; t < taxonNameMatches.length; t++) {
				System.out.println("Potential new genus: " + taxonNameMatches[t].toString("  "));
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff)) {
					System.out.println(" ==> outside main text");
					continue;
				}
				
				//	get genus epithet & check style (catch matches on intermediate authorities)
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null))) {
					System.out.println(" ==> style mismatch");
					continue;
				}
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], null, tableCellEndIndices, pm, "newLabel1");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
			}
			
			//	TODO make this work if genus absent (test: 'Oculogaster' in http://doi.org/10.11646/zootaxa.4107.4.2)
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" '(' <subGenus> ')'" +
					" ','?" +
					" <newLabel test=\"./@rank = 'subGenus'\">");
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	check subgenus style
				if (!checkEpithetStyle(taxonNameMatches[t], SUBGENUS_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBGENUS_ATTRIBUTE, tableCellEndIndices, pm, "newLabel1");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
			}
			
			//	extend annotation index
			if (labeledGenera.size() != 0)
				taxonNamePartIndex.addAnnotations(Gamta.extractAllContained(doc, new MapDictionary(labeledGenera)), GENUS_ATTRIBUTE);
		}
		
		//	TODO also observe rank 'form'
		//	TODO make observed ranks configurable ...
		//	TODO ... and generate patterns on the fly
		
		//	extract species (and even lower-ranked names) labeled as new (only with known genus, though, and only now, as known epithets are done with)
		pm.setInfo("Marking species and lower rank names labeled as new taxa");
		pm.setBaseProgress(77);
		pm.setProgress(0);
		pm.setMaxProgress(79);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<genus>" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <species>" +
				" (','? <authority>? <subSpecies>)?" +
				" (','? <authority>? <variety>)?" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" ','? <newLabel>");
		for (int t = 0; t < taxonNameMatches.length; t++) {
//			System.out.println("NL1: " + taxonNameMatches[t].toString("  "));
			if (!annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
//				System.out.println(" ==> annotated before");
				continue;
			}
			Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "newLabel1");
			if (taxonName == null) {
//				System.out.println(" ==> failed to annotate");
				continue;
			}
//			System.out.println(" ==> annotated");
			taxonName.setAttribute("_evidence", "statusLabel");
			annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
			String taxonNameSpecies = ((String) taxonName.getAttribute(SPECIES_ATTRIBUTE));
			updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
			updateDocAuthorityDict(taxonName, docAuthorityDict);
			if (speciesDict.contains(taxonNameSpecies))
				continue;
			((TreeSet) this.nonCatalogSpeciesByGenera.get((String) taxonName.getAttribute(GENUS_ATTRIBUTE))).add(taxonNameSpecies);
			MatchTree[] speciesTaxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex, (
					"<genus>" +
					" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
					" '" + taxonNameSpecies + "'" +
					" (','? <authority>? <subSpecies>)?" +
					" (','? <authority>? <variety>)?" +
					" ((','|':')? <authority>)?"));
			for (int st = 0; st < speciesTaxonNameMatches.length; st++)
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(speciesTaxonNameMatches[st]))) {
					Annotation speciesTaxonName = this.treeFat.annotateTaxonName(doc, speciesTaxonNameMatches[st], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "newLabel2");
					if (speciesTaxonName == null)
						continue;
					speciesTaxonName.setAttribute(SPECIES_ATTRIBUTE, taxonNameSpecies);
					speciesTaxonName.setAttribute("_evidence", "statusLabelOccurrence");
					annotatedTaxonNameStrings.put(speciesTaxonName.getValue(), speciesTaxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
					updateDocAuthorityDict(taxonName, docAuthorityDict);
				}
		}
		
		pm.setInfo("Marking subspecies names with labeled subspecies epithet");
		pm.setBaseProgress(79);
		pm.setProgress(0);
		pm.setMaxProgress(80);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<genus>" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <nameInfix>?" +
				" <potential_species>" +
				" ','? <authority>? <subSpecies>" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" (','? <newLabel test=\"./@rank = 'subSpecies'\">)?");
		for (int t = 0; t < taxonNameMatches.length; t++)
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, tableCellEndIndices, pm, "labeledSubspecies");
				if (taxonName == null)
					continue;
				if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "statusLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
					taxonName.setAttribute("_evidence", "subSpeciesLabel");
				else taxonName.setAttribute("_evidence", "catalogs");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
				updateDocAuthorityDict(taxonName, docAuthorityDict);
			}
		if (binomialsAreBold || binomialsAreItalics) {
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" <potential_species>" +
					" <subSpecies>" +
					" (','? <authority>)?" +
					" (','? <newLabel test=\"./@rank = 'subSpecies'\">)?");
			TreeMap labeledGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				
				//	check style of further epithets
				if (!checkEpithetStyle(taxonNameMatches[t], SPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				if (!checkEpithetStyle(taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, tableCellEndIndices, pm, "labeledSubspecies");
					if (taxonName == null)
						continue;
					if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "subSpeciesLabel+fontStyle");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
			}
			
			//	extend annotation index
			if (labeledGenera.size() != 0)
				taxonNamePartIndex.addAnnotations(Gamta.extractAllContained(doc, new MapDictionary(labeledGenera)), GENUS_ATTRIBUTE);
		}
		
		pm.setInfo("Marking variety names with labeled variety epithet");
		pm.setBaseProgress(80);
		pm.setProgress(0);
		pm.setMaxProgress(81);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<genus>" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <nameInfix>?" +
				" <potential_species>" +
				" (','? <authority>? (<subSpecies>|<potential_subSpecies>))?" +
				" (','? <authority>)? <variety>" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" (','? <newLabel test=\"./@rank = 'variety'\">)?");
		for (int t = 0; t < taxonNameMatches.length; t++)
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], VARIETY_ATTRIBUTE, tableCellEndIndices, pm, "labeledVariety");
				if (taxonName == null)
					continue;
				if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "statusLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else if (taxonName.hasAttribute(VARIETY_ATTRIBUTE))
					taxonName.setAttribute("_evidence", "varietyLabel");
				else if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
					taxonName.setAttribute("_evidence", "subSpeciesLabel");
				else taxonName.setAttribute("_evidence", "catalogs");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
				updateDocAuthorityDict(taxonName, docAuthorityDict);
			}
		if (binomialsAreBold || binomialsAreItalics) {
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" <potential_species>" +
					" (<subSpecies>|<potential_subSpecies>)?" +
					" <variety>" +
					" (','? <authority>)?" +
					" (','? <newLabel test=\"./@rank = 'variety'\">)?");
			TreeMap labeledGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				
				//	check style of further epithets
				if (!checkEpithetStyle(taxonNameMatches[t], SPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				Annotation subSpeciesAnnot = TreeFAT.findEpithet(taxonNameMatches[t], SUBSPECIES_ATTRIBUTE);
				if ((subSpeciesAnnot != null) && !checkEpithetStyle(subSpeciesAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				if (!checkEpithetStyle(taxonNameMatches[t], VARIETY_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				
				//	annotate taxon name and set meta attributes
//				System.out.println("LV: " + taxonNameMatches[t].toString("  "));
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], VARIETY_ATTRIBUTE, tableCellEndIndices, pm, "labeledVariety");
					if (taxonName == null) {
//						System.out.println(" ==> failed to annotate");
						continue;
					}
//					System.out.println(" ==> annotated");
					if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
						taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
						annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
					}
					else if (taxonName.hasAttribute(VARIETY_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "varietyLabel+fontStyle");
					else if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
						taxonName.setAttribute("_evidence", "subSpeciesLabel+fontStyle");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
//				else System.out.println(" ==> annotated before");
			}
			
			//	extend annotation index
			if (labeledGenera.size() != 0)
				taxonNamePartIndex.addAnnotations(Gamta.extractAllContained(doc, new MapDictionary(labeledGenera)), GENUS_ATTRIBUTE);
		}
		
		pm.setInfo("Marking species names labeled as new taxa");
		pm.setBaseProgress(81);
		pm.setProgress(0);
		pm.setMaxProgress(82);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<genus>" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <potential_species>" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" ','? (<newLabel test=\"./@rank = 'species'\">|<newLabel test=\"not(./@rank)\">)");
		for (int t = 0; t < taxonNameMatches.length; t++)
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "newSpecies");
				if (taxonName == null)
					continue;
				taxonName.setAttribute("_evidence", "statusLabel");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
				updateDocAuthorityDict(taxonName, docAuthorityDict);
			}
		if (binomialsAreBold || binomialsAreItalics || seedsFromEmphases) {
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" <potential_species>" +
					" (','? <authority>)?" +
					" ','? <newLabel test=\"./@rank = 'species'\">");
			TreeMap labeledGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	check style of further epithets
				if (!checkEpithetStyle(taxonNameMatches[t], SPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "newSpecies");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
			}
			
			//	extend annotation index
			if (labeledGenera.size() != 0)
				taxonNamePartIndex.addAnnotations(Gamta.extractAllContained(doc, new MapDictionary(labeledGenera)), GENUS_ATTRIBUTE);
		}
		
		pm.setInfo("Marking subspecies names labeled as new taxa");
		pm.setBaseProgress(82);
		pm.setProgress(0);
		pm.setMaxProgress(83);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<genus>" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <potential_species>" +
				" ','? <authority>?" +
				" (<subSpecies>|<potential_subSpecies>)" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" ','? (<newLabel test=\"./@rank = 'subSpecies'\">|<newLabel test=\"not(./@rank)\">)");
		for (int t = 0; t < taxonNameMatches.length; t++)
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, tableCellEndIndices, pm, "newSubSpecies");
				if (taxonName == null)
					continue;
				taxonName.setAttribute("_evidence", "statusLabel");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
				updateDocAuthorityDict(taxonName, docAuthorityDict);
			}
		if (binomialsAreBold || binomialsAreItalics || seedsFromEmphases) {
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" <potential_species>" +
					" (<subSpecies>|<potential_subSpecies>)" +
					" (','? <authority>)?" +
					" ','? <newLabel test=\"./@rank = 'subSpecies'\">");
			TreeMap labeledGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	check style of further epithets
				if (!checkEpithetStyle(taxonNameMatches[t], SPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				if (!checkEpithetStyle(taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, tableCellEndIndices, pm, "newSubSpecies");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
			}
			
			//	extend annotation index
			if (labeledGenera.size() != 0)
				taxonNamePartIndex.addAnnotations(Gamta.extractAllContained(doc, new MapDictionary(labeledGenera)), GENUS_ATTRIBUTE);
		}
		
		pm.setInfo("Marking variety names labeled as new taxa");
		pm.setBaseProgress(83);
		pm.setProgress(0);
		pm.setMaxProgress(84);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<genus>" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <potential_species>" +
				" (','? <authority>? (<subSpecies>|<potential_subSpecies>))?" +
				" (','? <authority>)?" +
				" (<variety>|<potential_variety>)" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" ','? (<newLabel test=\"./@rank = 'variety'\">|<newLabel test=\"not(./@rank)\">)");
		for (int t = 0; t < taxonNameMatches.length; t++) {
//			System.out.println("NV: " + taxonNameMatches[t].toString("  "));
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], VARIETY_ATTRIBUTE, tableCellEndIndices, pm, "newVariety");
				if (taxonName == null) {
//					System.out.println(" ==> failed to annotate");
					continue;
				}
//				System.out.println(" ==> annotated");
				taxonName.setAttribute("_evidence", "statusLabel");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
				updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict); 
				updateDocAuthorityDict(taxonName, docAuthorityDict);
			}
//			else System.out.println(" ==> annotated before");
		}
		if (binomialsAreBold || binomialsAreItalics || seedsFromEmphases) {
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" <potential_species>" +
					" (<subSpecies>|<potential_subSpecies>)?" +
					" (<variety>|<potential_variety>)" +
					" (','? <authority>)?" +
					" ','? <newLabel test=\"./@rank = 'variety'\">");
			TreeMap labeledGenera = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	check style of further epithets
				if (!checkEpithetStyle(taxonNameMatches[t], SPECIES_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				Annotation subSpeciesAnnot = TreeFAT.findEpithet(taxonNameMatches[t], SUBSPECIES_ATTRIBUTE);
				if ((subSpeciesAnnot != null) && !checkEpithetStyle(subSpeciesAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				if (!checkEpithetStyle(taxonNameMatches[t], VARIETY_ATTRIBUTE, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], VARIETY_ATTRIBUTE, tableCellEndIndices, pm, "newVariety");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "statusLabel+fontStyle");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
					labeledGenera.put(genusAnnot.getValue(), nonCatalogPotentialGenera.get(genusAnnot.getValue()));
					docGenusDict.addElementIgnoreDuplicates(genusAnnot.firstValue());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
				}
			}
			
			//	extend annotation index
			if (labeledGenera.size() != 0)
				taxonNamePartIndex.addAnnotations(Gamta.extractAllContained(doc, new MapDictionary(labeledGenera)), GENUS_ATTRIBUTE);
		}
		
		//	evaluate authority selectors
		for (int s = 0; s < this.authoritySelectors.length; s++) try {
			Annotation[] authorities = GPath.evaluatePath(doc, this.authoritySelectors[s], null);
			for (int w = 0; w < authorities.length; w++) {
				String authorityName = getFirstLastName(TokenSequenceUtils.concatTokens(authorities[w], true, true));
				if (authorityName != null)
					docAuthorityDict.addElementIgnoreDuplicates(authorityName);
			}
		}
		catch (RuntimeException re) {
			re.printStackTrace(System.out);
		}
		
		//	index start of taxon status labels to filter below
		HashSet taxonNameLabelStartIndices = new HashSet();
		Annotation[] taxonNameLabels = Gamta.extractAllContained(doc, this.treeFat.labelDict, 2);
		for (int l = 0; l < taxonNameLabels.length; l++)
			taxonNameLabelStartIndices.add(new Integer(taxonNameLabels[l].getStartIndex()));
		Annotation[] newTaxonNameLabels = Gamta.extractAllContained(doc, this.treeFat.newLabelDict, 4);
		for (int l = 0; l < newTaxonNameLabels.length; l++)
			taxonNameLabelStartIndices.add(new Integer(newTaxonNameLabels[l].getStartIndex()));
		Annotation[] sensuLabels = Gamta.extractAllContained(doc, this.treeFat.sensuLabelDict, 4);
		for (int l = 0; l < sensuLabels.length; l++)
			taxonNameLabelStartIndices.add(new Integer(sensuLabels[l].getStartIndex()));
		
		//	if taxon names use distinctive font style, use that to tag non-catalog species not yet identified by other means
		if (binomialsAreBold || binomialsAreItalics || (docAuthorityDict.size() != 0)) {
			//	TODO also allow this if we have a 'recall' flag set, but insist on authority in that case
			
			//	TODO maybe collect authority names above and also allow non-styled match if authority known
			
			//	mark non-catalog species
			pm.setInfo("Marking non-catalog species based on font style");
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"(<genus>|<abbreviated_genus>)" + // TODO permitting abbreviated genera here might be too imprecise in the general case, maybe use a document style based switch
					" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
					" <potential_species>" +
					" (<inNameSymbol>? (','|':')? <authority>)?");
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get species epithet
				Annotation speciesAnnot = TreeFAT.findEpithet(taxonNameMatches[t], SPECIES_ATTRIBUTE);
				if ((speciesAnnot == null) || !checkEpithetStyle(speciesAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				
				//	check species epithet for stop word (in case of style error)
				if (this.treeFat.stopWords.contains(speciesAnnot.firstValue()))
					continue;
				
				//	check species epithet for status label
				if (taxonNameLabelStartIndices.contains(new Integer(speciesAnnot.getStartIndex())))
					continue;
				
				//	check authority if we don't have emphases
				if (!binomialsAreBold && !binomialsAreItalics && !checkAuthority(taxonNameMatches[t], docAuthorityDict))
					continue;
				
				//	style matches, add annotation
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, ((binomialsAreBold || binomialsAreItalics) ? "styledSpecies" : "authorizedSpecies"));
					if (taxonName == null)
						continue;
					if (binomialsAreBold || binomialsAreItalics)
						taxonName.setAttribute("_evidence", "catalog+fontStyle");
					else taxonName.setAttribute("_evidence", "catalog+authority");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
					
					//	TODO_not learn non-catalog species ==> TOO DANGEROUS, PROPAGATES THE OCCASIONAL ERROR ACROSS DOCUMENTS
				}
			}
			
			//	TODO also allow this if we have a 'recall' flag set, but insist on authority in that case
			
			//	TODO maybe collect authority names above and also allow non-styled match if authority known
			
			//	mark non-catalog subspecies
			pm.setInfo("Marking non-catalog subspecies based on font style");
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"(<genus>|<abbreviated_genus>)" + // TODO permitting abbreviated genera here might be too imprecise in the general case, maybe use a document style based switch
					" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
					" <potential_species>" +
					" <potential_subSpecies>" +
					" (<inNameSymbol>? (','|':')? <authority>)?");
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get species and subspecies epithets
				Annotation speciesAnnot = TreeFAT.findEpithet(taxonNameMatches[t], SPECIES_ATTRIBUTE);
				if ((speciesAnnot == null) || !checkEpithetStyle(speciesAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				Annotation subSpeciesAnnot = TreeFAT.findEpithet(taxonNameMatches[t], SUBSPECIES_ATTRIBUTE);
				if ((subSpeciesAnnot == null) || !checkEpithetStyle(subSpeciesAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), null))
					continue;
				
				//	check species and subspecies epithets for stop word (in case of style error)
				if (this.treeFat.stopWords.contains(speciesAnnot.firstValue()))
					continue;
				if (this.treeFat.stopWords.contains(subSpeciesAnnot.firstValue()))
					continue;
				
				//	check species and subspecies epithet for status label
				if (taxonNameLabelStartIndices.contains(new Integer(speciesAnnot.getStartIndex())))
					continue;
				if (taxonNameLabelStartIndices.contains(new Integer(subSpeciesAnnot.getStartIndex())))
					continue;
				
				//	check authority if we don't have emphases
				if (!binomialsAreBold && !binomialsAreItalics && !checkAuthority(taxonNameMatches[t], docAuthorityDict))
					continue;
				
				//	style matches, add annotation
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, tableCellEndIndices, pm, ((binomialsAreBold || binomialsAreItalics) ? "styledSpecies" : "authorizedSpecies"));
					if (taxonName == null)
						continue;
					if (binomialsAreBold || binomialsAreItalics)
						taxonName.setAttribute("_evidence", "catalog+fontStyle");
					else taxonName.setAttribute("_evidence", "catalog+authority");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					updateDocEpithetDicts(taxonName, docSpeciesDict, docSubSpeciesDict, docVarietyDict);
					updateDocAuthorityDict(taxonName, docAuthorityDict);
				}
			}
			
			//	mark potential genera preceding known species (helps with binomials whose species was recombined)
			taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
					"<potential_genus>" +
					" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
					" <nameInfix>?" +
					" <species>" +
					" (','? <authority>)?" +
					" <subSpecies>?" +
					" (','? <authority>)?" +
					" <variety>?" +
					" (','? <authority>)?");
			for (int t = 0; t < taxonNameMatches.length; t++) {
				
				//	do not accept style-based matches before doc head cutoff or after bibliography cutoff (occurrence expansion will get those later on)
				if ((taxonNameMatches[t].getMatch().getStartIndex() < docHeadCutoff) || (taxonNameMatches[t].getMatch().getEndIndex() > bibliographyCutoff))
					continue;
				
				//	get genus and check style
				Annotation genusAnnot = TreeFAT.findEpithet(taxonNameMatches[t], GENUS_ATTRIBUTE);
				if ((genusAnnot == null) || !checkEpithetStyle(genusAnnot, (binomialsAreBold ? boldEmphasisTokenIndices : null), (binomialsAreItalics ? italicsEmphasisTokenIndices : null), (seedsFromEmphases ? emphasisTokenIndices : null)))
					continue;
				
				//	check document-wide genus capitalization (catches sentence starts followed by standalone species)
				int lcCount = docWordsByCase.getCount("L:" + genusAnnot.firstValue());
				int ccCount = docWordsByCase.getCount("C:" + genusAnnot.firstValue());
				if (lcCount > ccCount)
					continue;
				
				//	annotate taxon name and set meta attributes
				if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
					Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], null, tableCellEndIndices, pm, "recombinedSpecies");
					if (taxonName == null)
						continue;
					taxonName.setAttribute("_evidence", "species+fontStyle");
					annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
					System.out.println("RecombinedSpecies: " + taxonName.toXML());
					
					//	try and infer family to facilitate higher taxonomy linkup
					if (!nonCatalogGeneraToFamilies.containsKey(genusAnnot.firstValue())) {
						Annotation family = null;
						for (int f = 0; f < families.length; f++) {
							if (families[f].getEndIndex() < genusAnnot.getStartIndex())
								family = families[f];
							else break;
						}
						if (family != null) {
							taxonName.setAttribute(FAMILY_ATTRIBUTE, family.firstValue());
							nonCatalogGeneraToFamilies.put(genusAnnot.firstValue(), family.firstValue());
						}
					}
					
					//	TODO maybe also search backward for names with same species epithet and get family from there
				}
			}
		}
		
		//	generate species abbreviations
		for (int s = 0; s < docSpeciesDict.size(); s++) {
			String docSpecies = docSpeciesDict.get(s);
			String abbrevSpeciesRegExSecondLetter = docSpecies.substring(1).replaceAll("[e\\-\\']", "");
			String abbrevSpeciesRegEx = (docSpecies.substring(0, 1) + ((abbrevSpeciesRegExSecondLetter.length() == 0) ? "" : ("[" + abbrevSpeciesRegExSecondLetter + "]?")) + "\\.?");
			Annotation[] abbrevSpecies = Gamta.extractAllMatches(doc, abbrevSpeciesRegEx, 2);
			taxonNamePartIndex.addAnnotations(abbrevSpecies, (TreeFAT.ABBREVIATED_EPITHET_PREFIX + SPECIES_ATTRIBUTE));
		}
		
		//	index all doc-vetted epithets
		Annotation[] docSpecies = Gamta.extractAllContained(doc, docSpeciesDict);
		taxonNamePartIndex.addAnnotations(docSpecies, SPECIES_ATTRIBUTE);
		Annotation[] docSubSpecies = Gamta.extractAllContained(doc, docSubSpeciesDict);
		taxonNamePartIndex.addAnnotations(docSubSpecies, SUBSPECIES_ATTRIBUTE);
		Annotation[] docVarieties = Gamta.extractAllContained(doc, docVarietyDict);
		taxonNamePartIndex.addAnnotations(docVarieties, VARIETY_ATTRIBUTE);
		
		//	index abbreviated versions of genera identified from labels, species epithets, and font style
		for (int g = 0; g < docGenusDict.size(); g++) {
			String docGenus = docGenusDict.get(g);
			String abbrevDocGenusRegEx = (docGenus.substring(0, 1) + "[" + docGenus.substring(1).replaceAll("[e\\-\\']", "") + "]?\\.?");
			Annotation[] abbrevDocGenera = Gamta.extractAllMatches(doc, abbrevDocGenusRegEx, 2);
			taxonNamePartIndex.addAnnotations(abbrevDocGenera, (TreeFAT.ABBREVIATED_EPITHET_PREFIX + GENUS_ATTRIBUTE));
		}
		
		//	mark all combinations of all epithets we have now
		pm.setInfo("Marking non-labeled occurrences of names labeled as new taxa");
		pm.setBaseProgress(84);
		pm.setProgress(0);
		pm.setMaxProgress(86);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"(<genus>|<abbreviated_genus>)?" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <nameInfix>?" +
				" <species>" +
				" ((','? <authority>)? <subSpecies>)?" +
				" ((','? <authority>)? <variety>)?" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" (','? (<newLabel>|<sensuLabel>))?");
		for (int t = 0; t < taxonNameMatches.length; t++) {
//			System.out.println("NL1: " + taxonNameMatches[t].toString("  "));
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SPECIES_ATTRIBUTE, tableCellEndIndices, pm, "nonLabeled1");
				if (taxonName == null) {
//					System.out.println(" ==> failed to annotate");
					continue;
				}
//				System.out.println(" ==> annotated");
				if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "statusLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "sensuLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else taxonName.setAttribute("_evidence", "document");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
			}
//			else System.out.println(" ==> annotated before");
		}
		pm.setBaseProgress(86);
		pm.setProgress(0);
		pm.setMaxProgress(87);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"(<genus>|<abbreviated_genus>)?" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" <nameInfix>?" +
				" (<species>|<abbreviated_species>)" +
				" (','? <authority>)?" +
				" <subSpecies>" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" (','? (<newLabel>|<sensuLabel>))?");
		for (int t = 0; t < taxonNameMatches.length; t++) {
//			System.out.println("NL2: " + taxonNameMatches[t].toString("  "));
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], SUBSPECIES_ATTRIBUTE, tableCellEndIndices, pm, "nonLabeled2");
				if (taxonName == null) {
//					System.out.println(" ==> failed to annotate");
					continue;
				}
//				System.out.println(" ==> annotated");
				if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "statusLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "sensuLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else taxonName.setAttribute("_evidence", "document");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
			}
//			else System.out.println(" ==> annotated before");
		}
		pm.setBaseProgress(87);
		pm.setProgress(0);
		pm.setMaxProgress(88);
		taxonNameMatches = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"(<genus>|<abbreviated_genus>)?" +
				" ('(' (<subGenus>|<abbreviated_subGenus>) ')')?" +
				" (" +
					"<nameInfix>?" +
					" (<species>|<abbreviated_species>)" +
					" (','? <authority>)?" +
				")?" +
				" (<subSpecies> (','? <authority>)?)?" +
				" <variety>" +
				" <inNameSymbol>?" +
				" ((','|':')? <authority>)?" +
				" (','? (<newLabel>|<sensuLabel>))?");
		for (int t = 0; t < taxonNameMatches.length; t++) {
//			System.out.println("NL3: " + taxonNameMatches[t].toString("  "));
			if (annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonNameMatches[t]))) {
				Annotation taxonName = this.treeFat.annotateTaxonName(doc, taxonNameMatches[t], VARIETY_ATTRIBUTE, tableCellEndIndices, pm, "nonLabeled3");
				if (taxonName == null) {
//					System.out.println(" ==> failed to annotate");
					continue;
				}
//				System.out.println(" ==> annotated");
				if (taxonName.hasAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "statusLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else if (taxonName.hasAttribute(TreeFAT.SENSU_ATTRIBUTE)) {
					taxonName.setAttribute("_evidence", "sensuLabel");
					annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(taxonName));
				}
				else taxonName.setAttribute("_evidence", "document");
				annotatedTaxonNameStrings.put(taxonName.getValue(), taxonName);
			}
//			else System.out.println(" ==> annotated before");
		}
		
		//	we're done with this one, clean up
		taxonNamePartIndex.dispose(false);
		
		//	extract all occurrences of all taxon names, case insensitive
		pm.setInfo("Expanding to case insensitive occurrences of identified names");
		pm.setBaseProgress(88);
		pm.setProgress(0);
		pm.setMaxProgress(90);
		this.annotateNameOccurrences(doc, annotatedTaxonNameStrings, annotatedTaxonNameKeys, docGenusDict, pm);
		
		//	resolve interleaving names (mostly enumerations with abbreviated genus mistaken for tailing initial of preceding authority)
		Annotation[] taxonNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	- remove inner taxon names that start after containing name (greedy towards start)
		for (int t = 0; t < taxonNames.length; t++) {
			if (taxonNames[t] == null)
				continue;
			for (int lt = (t+1); lt < taxonNames.length; lt++) {
				if (taxonNames[lt] == null)
					continue;
				
				//	starting at same token as current outer name, retain for now
				if (taxonNames[lt].getStartIndex() == taxonNames[t].getStartIndex())
					continue;
				
				//	got beyond end of current outer name, we're done
				if (taxonNames[lt].getStartIndex() >= taxonNames[t].getEndIndex())
					break;
				
				//	reaching beyond end of current outer name, retain for now
				if (taxonNames[lt].getEndIndex() > taxonNames[t].getEndIndex())
					continue;
				
				//	fully nested, and staring after current outer name, clean it up
				doc.removeAnnotation(taxonNames[lt]);
				taxonNames[lt] = null;
			}
		}
		
		//	- remove taxon names interleaved with others
		for (int t = 0; t < taxonNames.length; t++) {
			if (taxonNames[t] == null)
				continue;
			for (int lt = (t+1); lt < taxonNames.length; lt++) {
				if (taxonNames[lt] == null)
					continue;
				
				//	got beyond end of current outer name, we're done
				if (taxonNames[lt].getStartIndex() >= taxonNames[t].getEndIndex())
					break;
				
				//	fully inside current outer name, retain for now
				if (taxonNames[lt].getEndIndex() <= taxonNames[t].getEndIndex())
					continue;
				
				//	reaching beyond end of current outer name, clean up outer name
				doc.removeAnnotation(taxonNames[t]);
				taxonNames[t] = null;
				break;
			}
		}
		
		//	clean up the rest
		AnnotationFilter.removeInner(doc, TAXONOMIC_NAME_ANNOTATION_TYPE);
		AnnotationFilter.removeInner(doc, TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		taxonNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	what have we got?
		pm.setInfo("Marked " + annotatedTaxonNameStrings.size() + " distinct taxonomic names, " + taxonNames.length + " taxonomic names in total");
		
		//	resolve abbreviations only now (scopes abbreviations to most recent occurrence of possible full form in long documents)
		pm.setStep("Expanding abbreviated genera and species");
		pm.setBaseProgress(90);
		pm.setProgress(0);
		pm.setMaxProgress(95);
		TreeFAT.expandAbbreviatedEpithets(taxonNames, catalogSpeciesToGenera, pm);
		
		//	add families for non-catalog genera
		pm.setStep("Linking taxonomic names with non-catalog genera to families");
		pm.setBaseProgress(95);
		pm.setProgress(0);
		pm.setMaxProgress(96);
		this.linkGeneraToFamilies(taxonNames, nonCatalogGeneraToFamilies, pm);
		
		//	add higher taxonomy, using frequencies of epithets for disambiguation
		pm.setStep("Linking taxonomic names to catalog data");
		pm.setBaseProgress(96);
		pm.setProgress(0);
		pm.setMaxProgress(100);
		this.treeFat.linkNamesToHierarchies(taxonNames, epithetsToPotentialHigherTaxonomies, epithetFrequencies, pm);
	}
	
	private void annotateNameOccurrences(MutableAnnotation doc, Map annotatedTaxonNameStrings, Set annotatedTaxonNameKeys, StringVector genusDict, ProgressMonitor pm) {
		
		//	create annotations
		Annotation[] docTaxonNames = Gamta.extractAllContained(doc, new MapDictionary(annotatedTaxonNameStrings));
		for (int t = 0; t < docTaxonNames.length; t++) {
			if (!annotatedTaxonNameKeys.add(TreeFAT.getTaxonNameKey(docTaxonNames[t])))
				continue;
			if ((docTaxonNames[t].size() == 1) && genusDict.containsIgnoreCase(docTaxonNames[t].firstValue()) && (docTaxonNames[t].firstValue().charAt(0) != Character.toUpperCase(docTaxonNames[t].firstValue().charAt(0))))
				continue;
			docTaxonNames[t].changeTypeTo(TAXONOMIC_NAME_ANNOTATION_TYPE);
			Annotation docTaxonName = doc.addAnnotation(docTaxonNames[t]);
			Annotation taxonName = ((Annotation) annotatedTaxonNameStrings.get(docTaxonNames[t].getValue()));
			if ((taxonName != null) && (docTaxonName.getStartIndex() != taxonName.getStartIndex())) {
				docTaxonName.copyAttributes(taxonName);
				docTaxonName.setAttribute("_evidence", "document");
				docTaxonName.setAttribute("_step", "document");
			}
			pm.setInfo(docTaxonName.getValue() + " at " + docTaxonName.getStartIndex());
		}
		
		//	count distinct species epithets, and how many of them explicitly occur in a full name
		TreeSet allDocSpecies = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		TreeSet fullNameDocSpecies = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		Annotation[] taxonNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			String tnSpecies = ((String) taxonNames[t].getAttribute(SPECIES_ATTRIBUTE));
			if (tnSpecies == null)
				continue;
			allDocSpecies.add(tnSpecies);
			String tnGenus = ((String) taxonNames[t].getAttribute(GENUS_ATTRIBUTE));
			if (tnGenus != null)
				fullNameDocSpecies.add(tnSpecies);
		}
		pm.setInfo("Found " + allDocSpecies.size() + " species epithets, " + fullNameDocSpecies.size() + " occurring in explicit combination with genus");
		
		//	if most (two thirds of) species do occur in a full name, exclude those that don't
		if ((allDocSpecies.size() * 2) < (fullNameDocSpecies.size() * 3))
			for (int t = 0; t < taxonNames.length; t++) {
				String tnSpecies = ((String) taxonNames[t].getAttribute(SPECIES_ATTRIBUTE));
				if ((tnSpecies == null) || fullNameDocSpecies.contains(tnSpecies))
					continue;
				pm.setInfo(" - removing standalone species for not occurring in explicit combination with genus: " + taxonNames[t].getValue());
				doc.removeAnnotation(taxonNames[t]);
			}
	}
	
	private void linkGeneraToFamilies(Annotation[] taxonNames, Map nonCatalogGeneraToFamilies, ProgressMonitor pm) {
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			String genus = ((String) taxonNames[t].getAttribute(GENUS_ATTRIBUTE));
			if ((genus != null) && nonCatalogGeneraToFamilies.containsKey(genus))
				taxonNames[t].setAttribute(FAMILY_ATTRIBUTE, nonCatalogGeneraToFamilies.get(genus));
		}
	}
	
	private void updatePrimaryRankDicts(Seed[] seeds, StringVector genusAndAboveEpithetDict, StringVector orderDict, StringVector familyDict, StringVector genusDict, StringVector speciesDict, Map epithetsToPotentialHigherTaxonomies, Properties catalogSpeciesToGenera) {
		for (int s = 0; s < seeds.length; s++) {
			
			//	add seed primary rank epithets and children to dictionaries
			if (GENUS_ATTRIBUTE.equals(seeds[s].rank)) {
				genusDict.addElementIgnoreDuplicates(seeds[s].epithet);
				genusAndAboveEpithetDict.addElementIgnoreDuplicates(seeds[s].epithet);
				if (seeds[s].childEpithets != null)
					for (Iterator ceit = seeds[s].childEpithets.iterator(); ceit.hasNext();) {
						String childSpecies = ((String) ceit.next());
						speciesDict.addElementIgnoreDuplicates(childSpecies);
						if (!catalogSpeciesToGenera.containsKey(childSpecies))
							catalogSpeciesToGenera.setProperty(childSpecies, seeds[s].epithet);
					}
				if (this.nonCatalogSpeciesByGenera.containsKey(seeds[s].epithet)) {
					TreeSet genusSpeciesSet = ((TreeSet) this.nonCatalogSpeciesByGenera.get(seeds[s].epithet));
					for (Iterator gsit = genusSpeciesSet.iterator(); gsit.hasNext();) {
						String genusSpecies = ((String) gsit.next());
						speciesDict.addElementIgnoreDuplicates(genusSpecies);
						if (!catalogSpeciesToGenera.containsKey(genusSpecies))
							catalogSpeciesToGenera.setProperty(genusSpecies, seeds[s].epithet);
					}
				}
			}
			else if (FAMILY_ATTRIBUTE.equals(seeds[s].rank)) {
				familyDict.addElementIgnoreDuplicates(seeds[s].epithet);
				genusAndAboveEpithetDict.addElementIgnoreDuplicates(seeds[s].epithet);
				if (seeds[s].childEpithets != null)
					for (Iterator ceit = seeds[s].childEpithets.iterator(); ceit.hasNext();) {
						String childGenus = ((String) ceit.next());
						genusDict.addElementIgnoreDuplicates(childGenus);
						genusAndAboveEpithetDict.addElementIgnoreDuplicates(childGenus);
						if (!epithetsToPotentialHigherTaxonomies.containsKey(childGenus))
							epithetsToPotentialHigherTaxonomies.put(childGenus, seeds[s].higherTaxonomies);
					}
			}
			else if (ORDER_ATTRIBUTE.equals(seeds[s].rank)) {
				orderDict.addElementIgnoreDuplicates(seeds[s].epithet);
				genusAndAboveEpithetDict.addElementIgnoreDuplicates(seeds[s].epithet);
				if (seeds[s].childEpithets != null)
					for (Iterator ceit = seeds[s].childEpithets.iterator(); ceit.hasNext();) {
						String childFamily = ((String) ceit.next());
						familyDict.addElementIgnoreDuplicates(childFamily);
						genusAndAboveEpithetDict.addElementIgnoreDuplicates(childFamily);
						if (!epithetsToPotentialHigherTaxonomies.containsKey(childFamily))
							epithetsToPotentialHigherTaxonomies.put(childFamily, seeds[s].higherTaxonomies);
					}
			}
			
			//	index epithets of non-primary ranks
			else if (!TreeFAT.primaryChildRanks.containsKey(seeds[s].rank)) {
				genusAndAboveEpithetDict.addElementIgnoreDuplicates(seeds[s].epithet);
				epithetsToPotentialHigherTaxonomies.put(seeds[s].epithet, seeds[s].higherTaxonomies);
			}
			
			//	add higher taxa to dictionaries
			for (int h = 0; h < seeds[s].higherTaxonomies.length; h++)
				for (int r = 0; r < TreeFAT.primaryRankNames.length; r++) {
					if (TreeFAT.primaryRankNames[r].equals(seeds[s].rank))
						break;
					if (!seeds[s].higherTaxonomies[h].containsKey(TreeFAT.primaryRankNames[r]))
						continue;
					if (FAMILY_ATTRIBUTE.equals(TreeFAT.primaryRankNames[r]))
						familyDict.addElementIgnoreDuplicates(seeds[s].higherTaxonomies[h].getProperty(TreeFAT.primaryRankNames[r]));
					else if (ORDER_ATTRIBUTE.equals(TreeFAT.primaryRankNames[r]))
						orderDict.addElementIgnoreDuplicates(seeds[s].higherTaxonomies[h].getProperty(TreeFAT.primaryRankNames[r]));
					genusAndAboveEpithetDict.addElementIgnoreDuplicates(seeds[s].higherTaxonomies[h].getProperty(TreeFAT.primaryRankNames[r]));
				}
		}
	}
	
	private static void updateDocEpithetDicts(Annotation taxonName, StringVector docSpeciesDict, StringVector docSubSpeciesDict, StringVector docVarietyDict) {
		
		//	add species, also as potential lower rank epithet if rank changed
		if (taxonName.hasAttribute(SPECIES_ATTRIBUTE)) {
			docSpeciesDict.addElementIgnoreDuplicates((String) taxonName.getAttribute(SPECIES_ATTRIBUTE));
			String taxonNameStatus = ((String) taxonName.getAttribute(TreeFAT.TAXONOMIC_NAME_STATUS_ATTRIBUTE, "")).toLowerCase();
			if ((taxonNameStatus.indexOf("comb") != -1) || taxonNameStatus.indexOf("stat") != -1) {
				docSubSpeciesDict.addElementIgnoreDuplicates((String) taxonName.getAttribute(SPECIES_ATTRIBUTE)); 
				docVarietyDict.addElementIgnoreDuplicates((String) taxonName.getAttribute(SPECIES_ATTRIBUTE)); 
			}
		}
		
		//	add subspecies and variety
		if (taxonName.hasAttribute(SUBSPECIES_ATTRIBUTE))
			docSubSpeciesDict.addElementIgnoreDuplicates((String) taxonName.getAttribute(SUBSPECIES_ATTRIBUTE)); 
		if (taxonName.hasAttribute(VARIETY_ATTRIBUTE))
			docVarietyDict.addElementIgnoreDuplicates((String) taxonName.getAttribute(VARIETY_ATTRIBUTE)); 
	}
	
	private static void updateDocAuthorityDict(Annotation taxonName, StringVector docAuthotityDict) {
		
		//	insist on authority year for safety
		if (!taxonName.hasAttribute(AUTHORITY_YEAR_ATTRIBUTE))
			return;
		
		//	get authority name
		String authorityName = ((String) taxonName.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
		if (authorityName == null)
			return;
		
		//	collect authority last names only
		String[] authorityNames = authorityName.split("((\\s*\\,\\s*)|(\\s*\\&\\s*)|(\\s+ex\\s+)|(\\s+et\\s+)|(\\s+and\\s+))");
		for (int n = 0; n < authorityNames.length; n++) {
			authorityName = getFirstLastName(authorityNames[n]);
			if (authorityName != null)
				docAuthotityDict.addElementIgnoreDuplicates(authorityName);
		}
	}
	
	private static String getFirstLastName(String authorityName) {
		while (authorityName.indexOf(' ') != -1) {
			if (authorityName.indexOf(',') != -1) // cut at first comma
				authorityName = authorityName.substring(0, authorityName.indexOf(',')).trim();
			else if (authorityName.indexOf('&') != -1) // cut at first ampersand
				authorityName = authorityName.substring(0, authorityName.indexOf('&')).trim();
			else if (authorityName.matches("[A-Z][a-z]?\\.?\\s+.*")) // cut leading initials
				authorityName = authorityName.substring(authorityName.indexOf(' ')).trim();
			else if (authorityName.matches("[a-z]+\\'?\\s+.*")) // cut leading 'von', 'van', 'de', etc.
				authorityName = authorityName.substring(authorityName.indexOf(' ')).trim();
			else authorityName = authorityName.substring(0, authorityName.indexOf(' '));
		}
		return (authorityName.matches("") ? null : authorityName);
	}
	
	private void addHigherTaxonomies(final Seed[] seeds, final TreeSet labeledPositives, final ProgressMonitor pm) {
		final ProgressMonitor spm = new SynchronizedProgressMonitor(pm);
		final Properties epithetRanks = new Properties();
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int s) throws Exception {
				spm.setProgress((s * 100) / seeds.length);
				if (seeds[s].higherTaxonomies == null) try {
					addHigherTaxonomy(seeds[s], epithetRanks, labeledPositives, spm);
				}
				catch (Exception e) {
					e.printStackTrace(System.out);
					throw e;
				}
			}
		}, seeds.length, -1);
	}
	
	private void addHigherTaxonomy(Seed seed, Properties epithetRanks, TreeSet labeledPositives, ProgressMonitor pm) {
		pm.setInfo("Getting catalog data for " + seed.epithet + ((seed.validEpithet == null) ? "" : (" (" + seed.validEpithet + ")")));
		String epithet = ((seed.validEpithet == null) ? seed.epithet : seed.validEpithet);
		
		//	check negatives known to higher taxonomy providers (unless epithet is labeled positive)
		if (!labeledPositives.contains(epithet) && this.treeFat.isKnownNegativeEpithet(epithet)) {
			seed.higherTaxonomies = HIGHER_TAXONOMIES_NON_EXISTENT;
			pm.setInfo(" ==> known negative");
			return;
		}
		
		//	check negatives cache
		if (this.emptyResultLookups.containsKey(epithet)) {
			seed.higherTaxonomies = HIGHER_TAXONOMIES_UNAVAILABLE;
			pm.setInfo(" ==> known lookup miss");
			return;
		}
		
		//	get rank (might be known from prior lookups)
		String rank;
		synchronized (epithetRanks) {
			rank = epithetRanks.getProperty(epithet, ((epithet.toLowerCase().endsWith("idae") || epithet.toLowerCase().endsWith("aceae")) ? FAMILY_ATTRIBUTE : GENUS_ATTRIBUTE));
		}
		pm.setInfo(" - rank is " + rank);
		
		//	do lookup
		Properties epithetHigherTaxonomy = null;
		
		//	do local lookup first
		if (epithetHigherTaxonomy == null)
			epithetHigherTaxonomy = this.treeFat.getHigherTaxonomy(epithet, rank, false);
		
		//	do online lookup
		if (epithetHigherTaxonomy == null)
			epithetHigherTaxonomy = this.treeFat.getHigherTaxonomy(epithet, rank, true);
		
		//	no result, try without rank
		if (epithetHigherTaxonomy == null) {
			
			//	try without definitive rank if assumed rank was genus
			if (GENUS_ATTRIBUTE.equals(rank)) {
				pm.setInfo(" ==> not found as " + GENUS_ATTRIBUTE);
				epithetHigherTaxonomy = this.treeFat.getHigherTaxonomy(epithet, null, true);
				
				//	not an epithet at all
				if (epithetHigherTaxonomy == null) {
					pm.setInfo(" ==> not found at all");
					return;
				}
				
				//	found in other rank
				else {
					rank = epithetHigherTaxonomy.getProperty(RANK_ATTRIBUTE, epithetHigherTaxonomy.getProperty("0." + RANK_ATTRIBUTE));
					if (rank == null) {
						pm.setInfo(" ==> not found at all, rank missing");
						return;
					}
					else {
						pm.setInfo(" ==> found as " + rank);
						synchronized (epithetRanks) {
							epithetRanks.setProperty(epithet, rank);
						}
					}
				}
			}
			
			//	no use for families, they have sufficiently distinctive suffixes
			else {
				pm.setInfo(" ==> not found");
				return;
			}
		}
		
		//	timeout
		if (epithetHigherTaxonomy == HigherTaxonomyProvider.LOOKUP_TIMEOUT) {
			seed.higherTaxonomies = HIGHER_TAXONOMIES_UNAVAILABLE;
			pm.setInfo(" ==> lookup timed out");
			return;
		}
		
		//	extract individual higher taxonomies
		Properties backupHigherTaxonomy = null;
		if (epithetHigherTaxonomy.containsKey(rank) && epithetHigherTaxonomy.containsKey("0." + rank)) {
			backupHigherTaxonomy = new Properties();
			for (Iterator kit = epithetHigherTaxonomy.keySet().iterator(); kit.hasNext();) {
				String key = ((String) kit.next());
				if (key.matches("[0-9]+\\..+"))
					continue;
				backupHigherTaxonomy.setProperty(key, epithetHigherTaxonomy.getProperty(key));
				kit.remove();
			}
		}
		Properties[] epithetHigherTaxonomies = HigherTaxonomyProvider.extractHierarchies(epithetHigherTaxonomy);
		if (backupHigherTaxonomy != null) {
			Properties[] restoreHigherTaxonomies = new Properties[epithetHigherTaxonomies.length+1];
			System.arraycopy(epithetHigherTaxonomies, 0, restoreHigherTaxonomies, 0, epithetHigherTaxonomies.length);
			restoreHigherTaxonomies[epithetHigherTaxonomies.length] = backupHigherTaxonomy;
			epithetHigherTaxonomies = restoreHigherTaxonomies;
		}
		pm.setInfo(" ==> found " + epithetHigherTaxonomies.length + " catalog entries");
		
		//	evaluate higher taxonomies
		int juniorSynonymCount = 0;
		TreeSet seniorSynonyms = null;
		Rank seedRank = ((Rank) this.treeFat.ranksByName.get(rank));
		String childRank = TreeFAT.primaryChildRanks.getProperty(rank);
		for (int h = 0; h < epithetHigherTaxonomies.length; h++) {
			
			//	make sure families and orders are looked up with correct rank (as we're going by decreasing frequency, we can safely assume the first lookup should actually be a genus or family)
			for (int r = 0; r < TreeFAT.primaryRankNames.length; r++) {
				if ((childRank != null) && TreeFAT.primaryRankNames[r].equals(childRank))
					break;
				if (this.treeFat.primaryRanks[r].getRelativeSignificance() > seedRank.getRelativeSignificance())
					break;
				if (epithetHigherTaxonomies[h].containsKey(TreeFAT.primaryRankNames[r])) synchronized (epithetRanks) {
					epithetRanks.setProperty(epithetHigherTaxonomies[h].getProperty(TreeFAT.primaryRankNames[r]), TreeFAT.primaryRankNames[r]);
				}
			}
			
			//	check if we found a senior synonym
			if ((seed.validEpithet == null) && epithetHigherTaxonomies[h].containsKey(rank) && !seed.epithet.equalsIgnoreCase(epithetHigherTaxonomies[h].getProperty(rank))) {
				pm.setInfo(" ==> potential junior synonym of " + epithetHigherTaxonomies[h].getProperty(rank));
				juniorSynonymCount++;
				if (seniorSynonyms == null)
					seniorSynonyms = new TreeSet();
				seniorSynonyms.add(epithetHigherTaxonomies[h].getProperty(rank));
			}
			
			//	get list of (primary rank) children for primary rank epithets
			if ((childRank != null) && epithetHigherTaxonomies[h].containsKey(childRank)) {
				String childEpithetString = epithetHigherTaxonomies[h].getProperty(childRank);
				String[] childEpithets = childEpithetString.split("\\s*\\;\\s*");
				for (int c = 0; c < childEpithets.length; c++)
					seed.addChildEpithet(childEpithets[c]);
			}
		}
		
		//	do we have a junior synonym here? If so, recurse with valid epithet
		if (((juniorSynonymCount * 2) > epithetHigherTaxonomies.length) && (seniorSynonyms.size() == 1)) {
			pm.setInfo(" ==> junior synonym of " + ((String) seniorSynonyms.first()) + ", recursing");
			seed.validEpithet = ((String) seniorSynonyms.first());
			this.addHigherTaxonomy(seed, epithetRanks, labeledPositives, pm);
		}
		
		//	store rank and higher taxonomies
		else {
			seed.rank = rank;
			seed.higherTaxonomies = epithetHigherTaxonomies;
		}
	}
	
	private Seed[] assessHigherTaxonomyLookupResult(Seed[] seeds, CountingSet epithetFrequencies, Map nonPrimaryRankEpithets, Map nonCatalogPotentialGenera, Map epithetsToPotentialHigherTaxonomies, Map ancestorEpithetsToPotentialHigherTaxonomies) {
		ArrayList seedList = new ArrayList();
		for (int s = 0; s < seeds.length; s++) {
			
			//	higher taxonomy not found, index as potential genus if not a family
			if (seeds[s].higherTaxonomies == null) {
				this.emptyResultLookups.put(seeds[s].epithet, new Long(System.currentTimeMillis()));
				if (seeds[s].validEpithet != null)
					this.emptyResultLookups.put(seeds[s].validEpithet, new Long(System.currentTimeMillis()));
				if (!seeds[s].epithet.endsWith("idae") && !seeds[s].epithet.endsWith("aceae"))
					nonCatalogPotentialGenera.put(seeds[s].epithet, seeds[s]);
				continue;
			}
			
			//	higher taxonomy not found, index as potential genus if not a family
			else if (seeds[s].higherTaxonomies == HIGHER_TAXONOMIES_UNAVAILABLE) {
				if (!seeds[s].epithet.endsWith("idae") && !seeds[s].epithet.endsWith("aceae"))
					nonCatalogPotentialGenera.put(seeds[s].epithet, seeds[s]);
				continue;
			}
			
			//	higher taxonomy lookup refused for known negative
			else if (seeds[s].higherTaxonomies == HIGHER_TAXONOMIES_NON_EXISTENT)
				continue;
			
			//	get child rank (null result indicates non-primary rank)
			String childRank = TreeFAT.primaryChildRanks.getProperty(seeds[s].rank);
			if ((childRank == null) && !nonPrimaryRankEpithets.containsKey(seeds[s].epithet))
				nonPrimaryRankEpithets.put(seeds[s].epithet, seeds[s]);
			
			//	higher taxonomy found, count ancestor frequencies
			Rank seedRank = ((Rank) this.treeFat.ranksByName.get(seeds[s].rank));
			for (int h = 0; h < seeds[s].higherTaxonomies.length; h++) {
				Properties higherTaxonomyTrace = new Properties();
				if (seeds[s].higherTaxonomies[h].containsKey(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE))
					higherTaxonomyTrace.setProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, seeds[s].higherTaxonomies[h].getProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE));
				
				//	go through primary ranks, counting frequencies
				for (int r = 0; r < TreeFAT.primaryRankNames.length; r++) {
					if (TreeFAT.primaryRankNames[r].equals(childRank))
						break;
					if (this.treeFat.primaryRanks[r].getRelativeSignificance() > seedRank.getRelativeSignificance())
						break;
					if (seeds[s].higherTaxonomies[h].containsKey(TreeFAT.primaryRankNames[r])) {
						String primaryRankEpithet = seeds[s].higherTaxonomies[h].getProperty(TreeFAT.primaryRankNames[r]);
						epithetFrequencies.add(primaryRankEpithet);
						higherTaxonomyTrace.setProperty(TreeFAT.primaryRankNames[r], primaryRankEpithet);
						Properties ancestorHigherTaxonomy = new Properties();
						ancestorHigherTaxonomy.putAll(higherTaxonomyTrace);
						ArrayList ancestorHigherTaxonomies = ((ArrayList) ancestorEpithetsToPotentialHigherTaxonomies.get(primaryRankEpithet));
						if (ancestorHigherTaxonomies == null) {
							ancestorHigherTaxonomies = new ArrayList();
							ancestorEpithetsToPotentialHigherTaxonomies.put(primaryRankEpithet, ancestorHigherTaxonomies);
						}
						ancestorHigherTaxonomies.add(ancestorHigherTaxonomy);
					}
				}
			}
			seedList.add(seeds[s]);
			epithetsToPotentialHigherTaxonomies.put(seeds[s].epithet, seeds[s].higherTaxonomies);
		}
		return ((Seed[]) seedList.toArray(new Seed[seedList.size()]));
	}
	
	private static boolean checkEpithetStyle(MatchTree mt, String epithetRank, Set boldEmphasisTokenIndices, Set italicsEmphasisTokenIndices, Set emphasisTokenIndices) {
		Annotation epithetAnnot = TreeFAT.findEpithet(mt, epithetRank);
		return ((epithetAnnot != null) && checkEpithetStyle(epithetAnnot, boldEmphasisTokenIndices, italicsEmphasisTokenIndices, emphasisTokenIndices));
	}
	private static boolean checkEpithetStyle(Annotation epithetAnnot, Set boldEmphasisTokenIndices, Set italicsEmphasisTokenIndices, Set emphasisTokenIndices) {
		if ((boldEmphasisTokenIndices != null) && !boldEmphasisTokenIndices.contains(new Integer(epithetAnnot.getEndIndex() - 1)))
			return false;
		if ((italicsEmphasisTokenIndices != null) && !italicsEmphasisTokenIndices.contains(new Integer(epithetAnnot.getEndIndex() - 1)))
			return false;
		if ((emphasisTokenIndices != null) && !emphasisTokenIndices.contains(new Integer(epithetAnnot.getEndIndex() - 1)))
			return false;
		return true;
	}
	
	private static Annotation findAuthority(MatchTree mt) {
		return findAuthority(mt.getChildren());
	}
	private static Annotation findAuthority(MatchTreeNode[] mtns) {
		for (int n = 0; n < mtns.length; n++) {
			if (mtns[n].getPattern().startsWith("<")) {
				Annotation matchAnnot = mtns[n].getMatch();
				String attributeName = mtns[n].getPattern().replaceAll("[\\<\\>\\?]", "");
				if (attributeName.indexOf(' ') != -1)
					attributeName = attributeName.substring(0, attributeName.indexOf(' '));
				if (AUTHORITY_ATTRIBUTE.equals(attributeName))
					return matchAnnot;
			}
			else {
				MatchTreeNode[] cMtns = mtns[n].getChildren();
				if (cMtns != null) { // can happen if optional disjunctive group matches nothing, for instance
					Annotation epithetAnnot = findAuthority(cMtns);
					if (epithetAnnot != null)
						return epithetAnnot;
				}
			}
		}
		return null;
	}
	
	private static boolean checkAuthority(MatchTree mt, StringVector docAuthorityDict) {
		
		//	get authority as a whole
		Annotation authority = findAuthority(mt);
		if (authority == null)
			return false;
		
		//	insist on year for safety
		Annotation[] authorityYears = Gamta.extractAllMatches(authority, "[12][0-9]{3}");
		if (authorityYears.length == 0)
			return false; // TODO do we really need this catch?
		
		//	extract authority name
		String authorityName = TokenSequenceUtils.concatTokens(authority, 0, ((authorityYears.length == 0) ? authority.size() : authorityYears[0].getStartIndex()), true, true);
		
		//	check authority last names only
		String[] authorityNames = authorityName.split("((\\s*\\,\\s*)|(\\s*\\&\\s*)|(\\s+ex\\s+)|(\\s+et\\s+)|(\\s+and\\s+))");
		for (int n = 0; n < authorityNames.length; n++) {
			authorityName = getFirstLastName(authorityNames[n]);
			if ((authorityName != null) && docAuthorityDict.contains(authorityName))
				return true;
		}
		
		//	no positive evidence found
		return false;
	}
	
	private Seed[] getSeedFamilies(MutableAnnotation doc) {
		Map seeds = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < doc.size(); t++) {
			String token = doc.valueAt(t);
			
			//	test for sequence of basic Latin letters with capital start
			if (!token.matches("[A-Z][a-zA-Z]{2,}"))
				continue;
			
			//	skip over 'North', 'South', 'East', 'West', 'Central', etc. plus subsequent word (will be geographic name)
			if (this.traceStopWords.contains(token)) {
				t++; // TODO_maybe skip over 'of', etc., to catch 'Department of XYZ', etc.
				continue;
			}
			
			//	filter suffixes (both ICZN and ICBN)
			if (!token.toLowerCase().endsWith("idae") && !token.toLowerCase().endsWith("aceae"))
				continue;
			
			//	this one looks OK for starters
			storeSeedToken(token, seeds, t);
			
			//	for ICZN super family ('<XYZ>oidae'), also add family ('<XYZ>idae')
			if (token.toLowerCase().endsWith("oidae")) {
				token = (token.substring(0, (token.length() - "oidae".length())) + token.substring(token.length() - "idae".length()));
				storeSeedToken(token, seeds, t);
			}
		}
		return ((Seed[]) seeds.values().toArray(new Seed[seeds.size()]));
	}
	
	private Seed[] getSeedGenera(MutableAnnotation doc, Set seedGenusList) {
		Map seeds = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < doc.size(); t++) {
			String token = doc.valueAt(t);
			
			//	test for sequence of basic Latin letters with capital start
			if (!token.matches("[A-Z][a-zA-Z]{2,}"))
				continue;
			
			//	skip over 'North', 'South', 'East', 'West', 'Central', etc. plus subsequent word (will be geographic name)
			if (this.traceStopWords.contains(token)) {
				t++; // TODO_maybe skip over 'of', etc., to catch 'Department of XYZ', etc.
				continue;
			}
			
			//	filter by genus list
			if (!seedGenusList.contains(token))
				continue;
			
			//	this one looks OK for starters
			storeSeedToken(token, seeds, t);
		}
		return ((Seed[]) seeds.values().toArray(new Seed[seeds.size()]));
	}
	
	private Seed[] getSeedsFromHighlights(MutableAnnotation doc, Set countryRegionIndexes, Set boldEmphasisTokenIndices, Set italicsEmphasisTokenIndices) {
		Map seeds = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		Set ignoreIndexes = new HashSet(countryRegionIndexes);
		this.collectSeeds(doc.getAnnotations(EMPHASIS_TYPE), seeds, ignoreIndexes, boldEmphasisTokenIndices, italicsEmphasisTokenIndices);
		this.collectSeeds(doc.getAnnotations(HEADING_TYPE), seeds, ignoreIndexes, boldEmphasisTokenIndices, italicsEmphasisTokenIndices);
		return ((Seed[]) seeds.values().toArray(new Seed[seeds.size()]));
	}
	
	private void collectSeeds(QueriableAnnotation[] seekAnnots, Map seeds, Set collected, Set boldEmphasisTokenIndices, Set italicsEmphasisTokenIndices) {
		for (int s = seekAnnots.length; s > 0; s--) // go backwards to make sure inner emphases (with more specific properties) are visited first
			this.collectSeeds(seekAnnots[s-1], seeds, collected, boldEmphasisTokenIndices, italicsEmphasisTokenIndices);
	}
	private void collectSeeds(QueriableAnnotation seekAnnot, Map seeds, Set collected, Set boldEmphasisTokenIndices, Set italicsEmphasisTokenIndices) {
		ArrayList seedsSinceLastNegative = new ArrayList();
		for (int t = 0; t < seekAnnot.size(); t++) {
			String token = seekAnnot.valueAt(t);
			
			//	collect token index according to font properties of emphasis
			if (EMPHASIS_TYPE.equals(seekAnnot.getType())) {
				if (seekAnnot.hasAttribute(BOLD_ATTRIBUTE))
					boldEmphasisTokenIndices.add(new Integer(seekAnnot.getAbsoluteStartIndex() + t));
				if (seekAnnot.hasAttribute(ITALICS_ATTRIBUTE))
					italicsEmphasisTokenIndices.add(new Integer(seekAnnot.getAbsoluteStartIndex() + t));
			}
			
			//	skip over 'Society', 'University', etc. plus preceding words (part of institution / organization name noun phrase)
			if (this.backwardStopWords.contains(token)) {
				for (int s = 0; s < seedsSinceLastNegative.size(); s++) {
					Seed seed = ((Seed) seedsSinceLastNegative.get(s));
					System.out.println("De-counting seed '" + seed.epithet + "' for being part of a noun phrase ending in '" + token + "'");
					seed.count--;
					if (seed.count <= 0) {
						seeds.remove(seed.epithet);
						System.out.println(" ==> eliminated");
					}
					else {
						if (seekAnnot.hasAttribute(BOLD_ATTRIBUTE))
							seed.boldCount--;
						if (seekAnnot.hasAttribute(ITALICS_ATTRIBUTE))
							seed.italicsCount--;
						System.out.println(" ==> de-counted");
					}
				}
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	test for sequence of basic Latin letters with capital start
			if (!token.matches("[A-Z][a-zA-Z]{2,}")) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	skip over 'North', 'South', 'East', 'West', 'Central', etc. plus subsequent word (will be geographic name)
			if (this.traceStopWords.contains(token)) {
				seedsSinceLastNegative.clear();
				t++; // TODO_maybe skip over 'of', etc., to catch 'Department of XYZ', etc.
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
			if (noVowel || noConsonant) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	Roman number
			if (Gamta.isRomanNumber(token)) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	we've seen this very one
			if (!collected.add(new Integer(seekAnnot.getAbsoluteStartIndex() + t))) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	this one looks OK for starters
			Seed seed = storeSeedToken(token, seeds, (seekAnnot.getAbsoluteStartIndex() + t));
			
			//	count font style properties
			if (seekAnnot.hasAttribute(BOLD_ATTRIBUTE)) {
				seed.boldCount++;
				if (t == 0)
					seed.boldStartCount++;
			}
			if (seekAnnot.hasAttribute(ITALICS_ATTRIBUTE)) {
				seed.italicsCount++;
				if (t == 0)
					seed.italicsStartCount++;
			}
			if (token.equals(token.toUpperCase()))
				seed.allCapsCount++;
			
			//	remember this one in case we have to roll back
			seedsSinceLastNegative.add(seed);
			
			//	for ICZN super family ('<XYZ>oidae'), also add family ('<XYZ>idae')
			if (token.toLowerCase().endsWith("oidae")) {
				token = (token.substring(0, (token.length() - "oidae".length())) + token.substring(token.length() - "idae".length()));
				seed = storeSeedToken(token, seeds, (seekAnnot.getAbsoluteStartIndex() + t));
				
				//	count font style properties
				if (seekAnnot.hasAttribute(BOLD_ATTRIBUTE)) {
					seed.boldCount++;
					if (t == 0)
						seed.boldStartCount++;
				}
				if (seekAnnot.hasAttribute(ITALICS_ATTRIBUTE)) {
					seed.italicsCount++;
					if (t == 0)
						seed.italicsStartCount++;
				}
				if (token.equals(token.toUpperCase()))
					seed.allCapsCount++;
				
				//	remember this one in case we have to roll back
				seedsSinceLastNegative.add(seed);
			}
		}
	}
	
	private Seed[] getSuffixBasedSeeds(MutableAnnotation doc, Seed[] exSeeds, Set countryRegionIndexes, Set boldEmphasisTokenIndices, Set italicsEmphasisTokenIndices) {
		Map seeds = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int s = 0; s < exSeeds.length; s++)
			seeds.put(exSeeds[s].epithet, exSeeds[s]);
			
		ArrayList seedsSinceLastNegative = new ArrayList();
		for (int t = 0; t < doc.size(); t++) {
			String token = doc.valueAt(t);
			
			//	we've likely seen this very one
			if (boldEmphasisTokenIndices.contains(new Integer(t)) || italicsEmphasisTokenIndices.contains(new Integer(t))) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	this one's a country ...
			if (countryRegionIndexes.contains(new Integer(t))) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	skip over 'Society', 'University', etc. plus preceding words (part of institution / organization name noun phrase)
			if (this.backwardStopWords.contains(token)) {
				for (int s = 0; s < seedsSinceLastNegative.size(); s++) {
					Seed seed = ((Seed) seedsSinceLastNegative.get(s));
					System.out.println("De-counting seed '" + seed.epithet + "' for being part of a noun phrase ending in '" + token + "'");
					seed.count--;
					if (seed.count <= 0) {
						seeds.remove(seed.epithet);
						System.out.println(" ==> eliminated");
					}
				}
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	test for sequence of basic Latin letters with capital start
			if (!token.matches("[A-Z][a-zA-Z]{2,}")) {
				seedsSinceLastNegative.clear();
				continue;
			}
			
			//	skip over 'North', 'South', 'East', 'West', 'Central', etc. plus subsequent word (will be geographic name)
			if (this.traceStopWords.contains(token)) {
				seedsSinceLastNegative.clear();
				t++; // TODO_maybe skip over 'of', etc., to catch 'Department of XYZ', etc.
				continue;
			}
			
			//	test if we have a suffix based epithet
			boolean notAnEpithet = true;
			
			//	family (both ICZN and ICBN)
			if (token.toLowerCase().endsWith("idae") || token.toLowerCase().endsWith("aceae"))
				notAnEpithet = false;
			
			//	sub family (both ICZN and ICBN)
			if (token.toLowerCase().endsWith("inae") || token.toLowerCase().endsWith("oideae"))
				notAnEpithet = false;
			
			//	tribe (both ICZN and ICBN)
			if (token.toLowerCase().endsWith("ini") || token.toLowerCase().endsWith("eae"))
				notAnEpithet = false;
			
			//	sub tribe (both ICZN and ICBN)
			if (token.toLowerCase().endsWith("ina") || token.toLowerCase().endsWith("inae"))
				notAnEpithet = false;
			
			//	not an epithet
			if (notAnEpithet)
				continue;
			
			//	this one looks OK for starters
			Seed seed = storeSeedToken(token, seeds, t);
			
			//	remember this one in case we have to roll back
			seedsSinceLastNegative.add(seed);
			
			//	for ICZN super family ('<XYZ>oidae'), also add family ('<XYZ>idae')
			if (token.toLowerCase().endsWith("oidae")) {
				token = (token.substring(0, (token.length() - "oidae".length())) + token.substring(token.length() - "idae".length()));
				seed = storeSeedToken(token, seeds, t);
				
				//	remember this one in case we have to roll back
				seedsSinceLastNegative.add(seed);
			}
		}
		
		//	found anything new?
		if (seeds.size() == exSeeds.length)
			return exSeeds;
		else return ((Seed[]) seeds.values().toArray(new Seed[seeds.size()]));
	}
	
	private static Seed storeSeedToken(String token, Map seeds, int index) {
		Seed seed = ((Seed) seeds.get(token));
		if (seed == null) {
			seed = new Seed(token, index);
			seeds.put(token, seed);
		}
		else {
			seed.firstOccurrenceIndex = Math.min(seed.firstOccurrenceIndex, index);
			seed.lastOccurrenceIndex = Math.max(seed.lastOccurrenceIndex, index);
		}
		seed.count++;
		return seed;
	}
	
	private void collectCountryRegionTokens(MutableAnnotation doc, StringVector startTokens, Set startTokenIndexes) {
		System.out.println("Getting country names:");
		Annotation[] countries = Gamta.extractAllContained(doc, this.countryHandler);
		Map regionHandlers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int c = 0; c < countries.length; c++) {
			String country = TokenSequenceUtils.concatTokens(countries[c], true, true);
			if ((country.length() < 4) && !country.equals(country.toUpperCase()))
				continue;
			for (int t = 0; t < countries[c].size(); t++) {
				startTokenIndexes.add(new Integer(countries[c].getStartIndex() + t));
				if (countries[c].firstValue().matches("[A-Z][A-Za-z]{2,}"))
					startTokens.addElementIgnoreDuplicates(countries[c].firstValue());
			}
			String eCountry = this.countryHandler.getEnglishName(country);
			if (regionHandlers.containsKey(eCountry))
				continue;
			System.out.println(" --> " + country + " (" + eCountry + ")");
			RegionHandler rh = this.countryHandler.getRegionHandler(eCountry, countryNameLanguages);
			if (rh != null)
				regionHandlers.put(eCountry, rh);
		}
		for (Iterator cit = regionHandlers.keySet().iterator(); cit.hasNext();) {
			String eCountry = ((String) cit.next());
			RegionHandler regionHandler = ((RegionHandler) regionHandlers.get(eCountry));
			System.out.println(" - getting region names from " + eCountry + ":");
			Annotation[] regions = Gamta.extractAllContained(doc, regionHandler);
			for (int r = 0; r < regions.length; r++) {
				for (int t = 0; t < regions[r].size(); t++) {
					startTokenIndexes.add(new Integer(regions[r].getStartIndex() + t));
					if (regions[r].firstValue().matches("[A-Z][A-Za-z]{2,}"))
						startTokens.addElementIgnoreDuplicates(regions[r].firstValue());
				}
				System.out.println("   --> " + TokenSequenceUtils.concatTokens(regions[r], true, true));
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		TreeFatTagger tfat = new TreeFatTagger();
		tfat.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/TreeFATData/")));
		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03911p493.pdf.gs.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Kucera2013.pdf.names.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Kucera2013.pdf.names.norm.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00872.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03131p051.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/11468_Logunov_2010_Bul_15_85-90.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00872.pdf.imf.nonCatalog.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Blepharidatta_revision.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/29867.pdf.names.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03916p083.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt03937p049.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04072p343.pfd.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/15611-5134-1-PB.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt03131p034.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04093p451.pdf.imf.refs.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt02344p016.pdf.refs.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zootaxa.4098.3.4.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04103.3.2.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/MaterialsCitationParserTest/Hemp2016-Pseudotomias_usambaricus.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04147.1.7.pdf.imf.mock.xml")), "utf-8"));
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Projektdaten/testDocs/zt04273p235.xml")), "utf-8"));
		
		AnnotationFilter.removeDuplicates(doc, EMPHASIS_TYPE);
		AnnotationFilter.removeAnnotations(doc, TAXONOMIC_NAME_ANNOTATION_TYPE);
		AnnotationFilter.removeAnnotations(doc, TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
		docStyle.getParameters().setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + ".binomialsAreBold"), "false");
		docStyle.getParameters().setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + ".binomialsAreItalics"), "true");
		tfat.process(doc, new Properties());
	}
//	
//	public static void main(String[] args) throws Exception {
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt00434.pdf.out.log"))));
//		for (String line; (line = br.readLine()) != null;) {
//			if (line.startsWith("ABBREVIATED SPECIES REGEX FOR"))
//				System.out.println(line);
//		}
//	}
}