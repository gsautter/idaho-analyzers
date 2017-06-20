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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTreeNode;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Taxonomic name handling facility using online catalogs as well as regular
 * expression patterns and document internal inference.
 * 
 * @author sautter
 */
public class TreeFAT implements TaxonomicNameConstants, ImagingConstants {
	
	static final String[] primaryRankNames = {
		KINGDOM_ATTRIBUTE,
		PHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
		GENUS_ATTRIBUTE,
	};
	static final Properties primaryChildRanks = new Properties();
	static {
		primaryChildRanks.setProperty(KINGDOM_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		primaryChildRanks.setProperty(PHYLUM_ATTRIBUTE, CLASS_ATTRIBUTE);
		primaryChildRanks.setProperty(CLASS_ATTRIBUTE, ORDER_ATTRIBUTE);
		primaryChildRanks.setProperty(ORDER_ATTRIBUTE, FAMILY_ATTRIBUTE);
		primaryChildRanks.setProperty(FAMILY_ATTRIBUTE, GENUS_ATTRIBUTE);
		primaryChildRanks.setProperty(GENUS_ATTRIBUTE, SPECIES_ATTRIBUTE);
	}
	
	static class MapDictionary implements Dictionary {
		private Map content;
		MapDictionary(Map content) {
			this.content = content;
		}
		public boolean lookup(String string) {
			return this.content.containsKey(string);
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return this.content.containsKey(string);
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return this.content.isEmpty();
		}
		public int size() {
			return this.content.size();
		}
		public StringIterator getEntryIterator() {
			final Iterator it = this.content.keySet().iterator();
			return new StringIterator() {
				public void remove() {}
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					return it.next();
				}
				public boolean hasMoreStrings() {
					return it.hasNext();
				}
				public String nextString() {
					return ((String) it.next());
				}
			};
		}
	}
	
	static class SetDictionary implements Dictionary {
		private Set content;
		SetDictionary(Set content) {
			this.content = content;
		}
		public boolean lookup(String string) {
			return this.content.contains(string);
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return this.content.contains(string);
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return this.content.isEmpty();
		}
		public int size() {
			return this.content.size();
		}
		public StringIterator getEntryIterator() {
			final Iterator it = this.content.iterator();
			return new StringIterator() {
				public void remove() {}
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					return it.next();
				}
				public boolean hasMoreStrings() {
					return it.hasNext();
				}
				public String nextString() {
					return ((String) it.next());
				}
			};
		}
	}
	
	static class CaseHalfSensitiveDictionary implements Dictionary {
		private StringVector content;
		CaseHalfSensitiveDictionary(StringVector content) {
			this.content = content;
		}
		public boolean lookup(String string) {
			if (this.content.lookup(string))
				return true;
			if (string.length() == 0)
				return false;
			if (Character.isUpperCase(string.charAt(0)))
				return this.content.lookup(string, false);
			return false;
		}
		public boolean lookup(String string, boolean caseSensitive) {
			if (this.content.lookup(string, caseSensitive))
				return true;
			if (string.length() == 0)
				return false;
			if (caseSensitive && Character.isUpperCase(string.charAt(0)))
				return this.content.lookup(string, false);
			return false;
		}
		public boolean isDefaultCaseSensitive() {
			return this.content.isDefaultCaseSensitive();
		}
		public boolean isEmpty() {
			return this.content.isEmpty();
		}
		public int size() {
			return this.content.size();
		}
		public StringIterator getEntryIterator() {
			return this.content.getEntryIterator();
		}
	}
	
	static class AuthorityStartFilter implements Dictionary {
		Dictionary docExclude;
		Dictionary countryRegionExclude;
		Set staticExclude;
		AuthorityStartFilter(Dictionary docDict, Dictionary countryRegionDict, Set staticDict) {
			this.docExclude = docDict;
			this.countryRegionExclude = countryRegionDict;
			this.staticExclude = staticDict;
		}
		public boolean lookup(String string) {
			return (this.docExclude.lookup(string) || this.countryRegionExclude.lookup(string) || this.staticExclude.contains(string));
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return (this.docExclude.lookup(string, caseSensitive) || this.countryRegionExclude.lookup(string, caseSensitive) || this.staticExclude.contains(string));
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return (this.docExclude.isEmpty() && this.countryRegionExclude.isEmpty() && this.staticExclude.isEmpty());
		}
		public int size() {
			return (this.docExclude.size() + this.countryRegionExclude.size() + this.staticExclude.size());
		}
		public StringIterator getEntryIterator() {
			final LinkedList its = new LinkedList();
			its.add(this.docExclude.getEntryIterator());
			its.add(this.countryRegionExclude.getEntryIterator());
			its.add(this.staticExclude.iterator());
			return new StringIterator() {
				private Iterator it = null;
				public void remove() {}
				public boolean hasNext() {
					if (this.it == null) {
						if (its.isEmpty())
							return false;
						else {
							this.it = ((Iterator) its.removeFirst());
							return this.hasNext();
						}
					}
					else if (this.it.hasNext())
						return true;
					else {
						this.it = null;
						return this.hasNext();
					}
				}
				public Object next() {
					return (this.hasNext() ? this.it.next() : null);
				}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
	}
	
	static final String NAME_INFIX_ANNOTATION_TYPE = "nameInfix";
	static final String IN_NAME_SYMBOL_ANNOTATION_TYPE = "inNameSymbol";
	static final String NEW_LABEL_ANNOTATION_TYPE = "newLabel";
	static final String SENSU_LABEL_ANNOTATION_TYPE = "sensuLabel";
	static final String POTENTIAL_EPITHET_PREFIX = "potential_";
	static final String ABBREVIATED_EPITHET_PREFIX = "abbreviated_";
	
	static final String TAXONOMIC_NAME_STATUS_ATTRIBUTE = "status";
	
	static final String SENSU_ATTRIBUTE = "sensu";
	
	private HigherTaxonomyProvider higherTaxonomyProvider;
	
	TreeSet stopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//	private TreeSet traceStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//	private TreeSet backwardStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//	private String[] stopSuffixes = new String[0];
//	private GPath[] stopWordSelectors = new GPath[0];
	
	private TreeSet authorityPrefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//	private TreeSet authorityStopWords = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//	private GPath[] authoritySelectors = new GPath[0];
//	
//	private CountryHandler countryHandler;
	
	private TaxonomicRankSystem rankSystem;
	Rank[] ranks;
	Rank[] primaryRanks;
	Rank[] belowSpeciesRanks;
	TreeMap ranksByName = new TreeMap();
	private int speciesRankRelativeSignificance = -1;
	
	private TreeMap labelsToRanks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	Dictionary labelDict = new MapDictionary(this.labelsToRanks);
	private TreeMap newLabelsToRanks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	Dictionary newLabelDict = new MapDictionary(this.newLabelsToRanks);
	private Properties rankLabelMatchers = new Properties();
	private TreeMap sensuLabelsToSensus = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	Dictionary sensuLabelDict = new MapDictionary(this.sensuLabelsToSensus);
	
	private AnalyzerDataProvider dataProvider;
	
	private TreeFAT(AnalyzerDataProvider adp) {
		this.dataProvider = adp;
//		this.initAnalyzer();
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
		
		//	load stop word lists
		this.fillList("stopWords.txt", this.stopWords);
//		this.fillList("stopWords.trace.txt", this.traceStopWords);
//		this.fillList("stopWords.backward.txt", this.backwardStopWords);
//		TreeSet stopSuffixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//		this.fillList("stopWords.suffix.txt", stopSuffixes);
//		this.stopSuffixes = ((String[]) stopSuffixes.toArray(new String[stopSuffixes.size()]));
//		
//		//	load stop word selectors
//		TreeSet stopWordSelectorList = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//		this.fillList("stopWords.selectors.txt", stopWordSelectorList);
//		ArrayList stopWordSelectors = new ArrayList();
//		for (Iterator swsit = stopWordSelectorList.iterator(); swsit.hasNext();) try {
//			stopWordSelectors.add(new GPath((String) swsit.next())); 
//		}
//		catch (RuntimeException re) {
//			re.printStackTrace(System.out);
//		}
//		this.stopWordSelectors = ((GPath[]) stopWordSelectors.toArray(new GPath[stopWordSelectors.size()]));
//		
//		//	load country handler
//		this.countryHandler = CountryHandler.getCountryHandler(((AnalyzerDataProvider) null), countryNameLanguages);
//		
//		//	load authority stop word list
//		this.fillList("authority.stopWords.txt", this.authorityStopWords);
		
		//	load authority prefix list
		this.fillList("authority.prefixes.txt", this.authorityPrefixes);
//		
//		//	load authority selectors
//		TreeSet authoritySelectorList = new TreeSet(String.CASE_INSENSITIVE_ORDER);
//		this.fillList("authority.selectors.txt", authoritySelectorList);
//		ArrayList authoritySelectors = new ArrayList();
//		for (Iterator swsit = authoritySelectorList.iterator(); swsit.hasNext();) try {
//			authoritySelectors.add(new GPath((String) swsit.next())); 
//		}
//		catch (RuntimeException re) {
//			re.printStackTrace(System.out);
//		}
//		this.authoritySelectors = ((GPath[]) authoritySelectors.toArray(new GPath[authoritySelectors.size()]));
		
		//	load taxonomic rank system, and fill index structures
		this.rankSystem = TaxonomicRankSystem.getRankSystem("ICZN");
		this.ranks = this.rankSystem.getRanks();
		boolean genusToCome = true;
		ArrayList belowSpeciesRanks = null;
		for (int r = 0; r < this.ranks.length; r++) {
			this.ranksByName.put(this.ranks[r].name, this.ranks[r]);
			
			//	rank labels are stop words as well
			this.stopWords.add(this.ranks[r].name);
			
			//	work genus and below only
			if (GENUS_ATTRIBUTE.equals(this.ranks[r].name))
				genusToCome = false;
			else if (genusToCome)
				continue;
			
			//	collect spelled-out labels
			this.labelsToRanks.put(this.ranks[r].name, this.ranks[r].name);
			this.newLabelsToRanks.put(("new " + this.ranks[r].name), this.ranks[r].name);
			this.newLabelsToRanks.put((this.ranks[r].name + " nov."), this.ranks[r].name);
			this.newLabelsToRanks.put((this.ranks[r].name + " nov"), this.ranks[r].name);
			this.newLabelsToRanks.put(("nov. " + this.ranks[r].name), this.ranks[r].name);
			this.newLabelsToRanks.put(("nov " + this.ranks[r].name), this.ranks[r].name);
			
			//	collect short labels
			String[] rankLabels = this.ranks[r].getAbbreviations();
			for (int l = 0; l < rankLabels.length; l++)
				this.addRankLabel(rankLabels[l], this.ranks[r].name);
			
			//	collecting below-species ranks
			if (SPECIES_ATTRIBUTE.equals(this.ranks[r].name)) {
				belowSpeciesRanks = new ArrayList();
				this.speciesRankRelativeSignificance = this.ranks[r].getRelativeSignificance();
			}
			else if (belowSpeciesRanks != null)
				belowSpeciesRanks.add(this.ranks[r]);
		}
		this.belowSpeciesRanks = ((Rank[]) belowSpeciesRanks.toArray(new Rank[belowSpeciesRanks.size()]));
		this.primaryRanks = new Rank[primaryRankNames.length];
		for (int r = 0; r < primaryRankNames.length; r++)
			this.primaryRanks[r] = ((Rank) this.ranksByName.get(primaryRankNames[r]));
		
		//	load list of alternative and synonymous rank labels
		if (this.dataProvider.isDataAvailable("rankSynonyms.txt")) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("rankSynonyms.txt"), "UTF-8"));
			for (String rs; (rs = br.readLine()) != null;) {
				rs = rs.trim();
				if (rs.indexOf(' ') == -1)
					continue;
				String rankName = rs.substring(0, rs.indexOf(' '));
				String[] rankLabels = rs.substring(rs.indexOf(' ') + " ".length()).split("\\;");
				for (int l = 0; l < rankLabels.length; l++)
					this.addRankLabel(rankLabels[l], rankName);
			}
		} catch (IOException ioe) {}
		
		//	add labels for recombinations
		this.newLabelsToRanks.put("new combination", "");
		this.newLabelsToRanks.put("new comb.", "");
		this.newLabelsToRanks.put("new comb", "");
		this.newLabelsToRanks.put("new com.", "");
		this.newLabelsToRanks.put("new com", "");
		this.newLabelsToRanks.put("n. comb.", "");
		this.newLabelsToRanks.put("n.comb.", "");
		this.newLabelsToRanks.put("n. comb", "");
		this.newLabelsToRanks.put("n.comb", "");
		this.newLabelsToRanks.put("n comb.", "");
		this.newLabelsToRanks.put("n comb", "");
		this.newLabelsToRanks.put("n. com.", "");
		this.newLabelsToRanks.put("n.com.", "");
		this.newLabelsToRanks.put("n. com", "");
		this.newLabelsToRanks.put("n.com", "");
		this.newLabelsToRanks.put("n com.", "");
		this.newLabelsToRanks.put("n com", "");
		this.newLabelsToRanks.put("comb. nov.", "");
		this.newLabelsToRanks.put("comb.nov.", "");
		this.newLabelsToRanks.put("comb. nov", "");
		this.newLabelsToRanks.put("comb.nov", "");
		this.newLabelsToRanks.put("comb nov.", "");
		this.newLabelsToRanks.put("comb nov", "");
		this.newLabelsToRanks.put("comb. n.", "");
		this.newLabelsToRanks.put("comb.n.", "");
		this.newLabelsToRanks.put("comb. n", "");
		this.newLabelsToRanks.put("comb.n", "");
		this.newLabelsToRanks.put("comb n.", "");
		this.newLabelsToRanks.put("comb n", "");
		this.newLabelsToRanks.put("com. nov.", "");
		this.newLabelsToRanks.put("com.nov.", "");
		this.newLabelsToRanks.put("com. nov", "");
		this.newLabelsToRanks.put("com.nov", "");
		this.newLabelsToRanks.put("com nov.", "");
		this.newLabelsToRanks.put("com nov", "");
		this.newLabelsToRanks.put("com. n.", "");
		this.newLabelsToRanks.put("com.n.", "");
		this.newLabelsToRanks.put("com. n", "");
		this.newLabelsToRanks.put("com.n", "");
		this.newLabelsToRanks.put("com n.", "");
		this.newLabelsToRanks.put("com n", "");
		this.newLabelsToRanks.put("nov. comb.", "");
		this.newLabelsToRanks.put("nov.comb.", "");
		this.newLabelsToRanks.put("nov. comb", "");
		this.newLabelsToRanks.put("nov.comb", "");
		this.newLabelsToRanks.put("nov comb.", "");
		this.newLabelsToRanks.put("nov comb", "");
		this.newLabelsToRanks.put("nov. com.", "");
		this.newLabelsToRanks.put("nov.com.", "");
		this.newLabelsToRanks.put("nov. com", "");
		this.newLabelsToRanks.put("nov.com", "");
		this.newLabelsToRanks.put("nov com.", "");
		this.newLabelsToRanks.put("nov com", "");
		
		//	add labels for replacement names
		this.newLabelsToRanks.put("new replacement name", "");
		this.newLabelsToRanks.put("nom. nov.", "");
		this.newLabelsToRanks.put("nom.nov.", "");
		this.newLabelsToRanks.put("nom. nov", "");
		this.newLabelsToRanks.put("nom.nov", "");
		this.newLabelsToRanks.put("nom nov.", "");
		this.newLabelsToRanks.put("nom nov", "");
		this.newLabelsToRanks.put("nom. n.", "");
		this.newLabelsToRanks.put("nom.n.", "");
		this.newLabelsToRanks.put("nom. n", "");
		this.newLabelsToRanks.put("nom.n", "");
		this.newLabelsToRanks.put("nom n.", "");
		this.newLabelsToRanks.put("nom n", "");
		this.newLabelsToRanks.put("nov. nom.", "");
		this.newLabelsToRanks.put("nov.nom.", "");
		this.newLabelsToRanks.put("nov. nom", "");
		this.newLabelsToRanks.put("nov.nom", "");
		this.newLabelsToRanks.put("nov nom.", "");
		this.newLabelsToRanks.put("nov nom", "");
		
		//	add labels for synonymization
		this.newLabelsToRanks.put("new synonymy", "");
		this.newLabelsToRanks.put("new synonym", "");
		this.newLabelsToRanks.put("n. syn.", "");
		this.newLabelsToRanks.put("n.syn.", "");
		this.newLabelsToRanks.put("n. syn", "");
		this.newLabelsToRanks.put("n.syn", "");
		this.newLabelsToRanks.put("n syn.", "");
		this.newLabelsToRanks.put("n syn", "");
		this.newLabelsToRanks.put("syn. nov.", "");
		this.newLabelsToRanks.put("syn.nov.", "");
		this.newLabelsToRanks.put("syn. nov", "");
		this.newLabelsToRanks.put("syn.nov", "");
		this.newLabelsToRanks.put("syn nov.", "");
		this.newLabelsToRanks.put("syn nov", "");
		this.newLabelsToRanks.put("syn. n.", "");
		this.newLabelsToRanks.put("syn.n.", "");
		this.newLabelsToRanks.put("syn. n", "");
		this.newLabelsToRanks.put("syn.n", "");
		this.newLabelsToRanks.put("syn n.", "");
		this.newLabelsToRanks.put("syn n", "");
		this.newLabelsToRanks.put("nov. syn.", "");
		this.newLabelsToRanks.put("nov.syn.", "");
		this.newLabelsToRanks.put("nov. syn", "");
		this.newLabelsToRanks.put("nov.syn", "");
		this.newLabelsToRanks.put("nov syn.", "");
		this.newLabelsToRanks.put("nov syn", "");
		
		//	add labels for generic status changes, e.g. changes of rank
		this.newLabelsToRanks.put("new status", "");
		this.newLabelsToRanks.put("n. stat.", "");
		this.newLabelsToRanks.put("n.stat.", "");
		this.newLabelsToRanks.put("n stat.", "");
		this.newLabelsToRanks.put("n. stat", "");
		this.newLabelsToRanks.put("n.stat", "");
		this.newLabelsToRanks.put("n stat", "");
		this.newLabelsToRanks.put("stat. nov.", "");
		this.newLabelsToRanks.put("stat.nov.", "");
		this.newLabelsToRanks.put("stat nov.", "");
		this.newLabelsToRanks.put("stat. nov", "");
		this.newLabelsToRanks.put("stat.nov", "");
		this.newLabelsToRanks.put("stat nov", "");
		this.newLabelsToRanks.put("stat. rev.", "");
		this.newLabelsToRanks.put("stat.rev.", "");
		this.newLabelsToRanks.put("stat rev.", "");
		this.newLabelsToRanks.put("stat. rev", "");
		this.newLabelsToRanks.put("stat.rev", "");
		this.newLabelsToRanks.put("stat rev", "");
		this.newLabelsToRanks.put("nov. stat.", "");
		this.newLabelsToRanks.put("nov.stat.", "");
		this.newLabelsToRanks.put("nov stat.", "");
		this.newLabelsToRanks.put("nov. stat", "");
		this.newLabelsToRanks.put("nov.stat", "");
		this.newLabelsToRanks.put("nov stat", "");
		this.newLabelsToRanks.put("rev. stat.", "");
		this.newLabelsToRanks.put("rev.stat.", "");
		this.newLabelsToRanks.put("rev stat.", "");
		this.newLabelsToRanks.put("rev. stat", "");
		this.newLabelsToRanks.put("rev.stat", "");
		this.newLabelsToRanks.put("rev stat", "");
		
		//	add 'sensu' labels
		this.sensuLabelsToSensus.put("sensu lato", "lato");
		this.sensuLabelsToSensus.put("s. lat.", "lato");
		this.sensuLabelsToSensus.put("s.lat.", "lato");
		this.sensuLabelsToSensus.put("s. lat", "lato");
		this.sensuLabelsToSensus.put("s.lat", "lato");
		this.sensuLabelsToSensus.put("s lat.", "lato");
		this.sensuLabelsToSensus.put("s lat", "lato");
		this.sensuLabelsToSensus.put("s. l.", "lato");
		this.sensuLabelsToSensus.put("s.l.", "lato");
		this.sensuLabelsToSensus.put("s. l", "lato");
		this.sensuLabelsToSensus.put("s.l", "lato");
		this.sensuLabelsToSensus.put("s l.", "lato");
		this.sensuLabelsToSensus.put("s l", "lato");
		this.sensuLabelsToSensus.put("sensu stricto", "stricto");
		this.sensuLabelsToSensus.put("s. str.", "stricto");
		this.sensuLabelsToSensus.put("s.str.", "stricto");
		this.sensuLabelsToSensus.put("s. str", "stricto");
		this.sensuLabelsToSensus.put("s.str", "stricto");
		this.sensuLabelsToSensus.put("s str.", "stricto");
		this.sensuLabelsToSensus.put("s str", "stricto");
		this.sensuLabelsToSensus.put("s. s.", "stricto");
		this.sensuLabelsToSensus.put("s.s.", "stricto");
		this.sensuLabelsToSensus.put("s. s", "stricto");
		this.sensuLabelsToSensus.put("s.s", "stricto");
		this.sensuLabelsToSensus.put("s s.", "stricto");
		this.sensuLabelsToSensus.put("s s", "stricto");
		
		//	add 'incertae sedis' labels TODO add abbreviations
		this.sensuLabelsToSensus.put("incertae sedis", "");
		
		//	TODO add 'new gen. and spec.' style labels with rank 'species'
		
		//	TODO add 'new gen., new spec.' style labels with rank 'species' ==> consider using annotation patterns here
		
		//	TODO simply use this down in tagging status labels, for name tagger to simply pick up
		
		//	load higher taxonomy providers
		this.higherTaxonomyProvider = HigherTaxonomyProvider.getTaxonomyProvider(this.dataProvider);
		this.higherTaxonomyProvider.setLookupTimeout(10 * 1000);
	}
	
	void fillList(String name, Set list) {
		try {
			BufferedReader lbr = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(name), "UTF-8"));
			for (String ll; (ll = lbr.readLine()) != null;) {
				ll = ll.trim();
				if ((ll.length() != 0) && !ll.startsWith("//"))
					list.add(ll);
			}
			lbr.close();
		}
		catch (Exception e) {
			System.out.println("Could not load '" + name + "': " + e.getMessage());
			e.printStackTrace(System.out);
		}
	}
	
	private void addRankLabel(String rankLabel, String rankName) {
		
		//	store epithet label
		this.labelsToRanks.put(rankLabel, rankName);
		
		//	store new label
		this.newLabelsToRanks.put(("new " + rankLabel), rankName);
		this.newLabelsToRanks.put(("n. " + rankLabel), rankName);
		this.newLabelsToRanks.put(("n." + rankLabel), rankName);
		this.newLabelsToRanks.put(("n " + rankLabel), rankName);
		this.newLabelsToRanks.put((rankLabel + " nov."), rankName);
		this.newLabelsToRanks.put((rankLabel + " nov"), rankName);
		this.newLabelsToRanks.put((rankLabel + " n."), rankName);
		this.newLabelsToRanks.put((rankLabel + " n"), rankName);
		if (rankLabel.endsWith(".")) {
			this.newLabelsToRanks.put((rankLabel + "nov."), rankName);
			this.newLabelsToRanks.put((rankLabel + "nov"), rankName);
			this.newLabelsToRanks.put((rankLabel + "n."), rankName);
			this.newLabelsToRanks.put((rankLabel + "n"), rankName);
		}
		
		//	store or extend label matcher
		String rankLabelMatcher = this.rankLabelMatchers.getProperty(rankName);
		if (rankLabelMatcher == null)
			rankLabelMatcher = ("'" + rankLabel + "'");
		else rankLabelMatcher += ("|'" + rankLabel + "'");
		this.rankLabelMatchers.setProperty(rankName, rankLabelMatcher);
	}
	
	void exitTreeFAT() {
		if (this.higherTaxonomyProvider == null)
			return;
		
		//	shut down higher taxonomy providers
		this.higherTaxonomyProvider.shutdown();
		this.higherTaxonomyProvider = null;
	}
	
	static void linkGeneraToFamilies(Annotation[] taxonNames, Map nonCatalogGeneraToFamilies, ProgressMonitor pm) {
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			String genus = ((String) taxonNames[t].getAttribute(GENUS_ATTRIBUTE));
			if ((genus != null) && nonCatalogGeneraToFamilies.containsKey(genus))
				taxonNames[t].setAttribute(FAMILY_ATTRIBUTE, nonCatalogGeneraToFamilies.get(genus));
		}
	}
	
	void linkNamesToHierarchies(Annotation[] taxonNames, Map epithetsToPotentialHigherTaxonomies, CountingSet epithetFrequencies, ProgressMonitor pm) {
		TreeMap epithetsToHigherTaxonomies = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			pm.setInfo("Adding higher taxonomy to " + taxonNames[t].getValue());
			
			//	find lowest given primary rank from genus upward
			String primaryRankEpithet = null;
			String primaryRankName = null;
			for (int r = (primaryRankNames.length - 1); r >= 0; r--) {
				primaryRankEpithet = ((String) taxonNames[t].getAttribute(primaryRankNames[r]));
				if (primaryRankEpithet == null)
					continue; // we don't have an epithet for this rank
				if (!epithetsToPotentialHigherTaxonomies.containsKey(primaryRankEpithet))
					continue; // we might have discovered a new, non-catalog genus in any of the fuzzy extensions, and we need to use the family in that case
				primaryRankName = primaryRankNames[r];
				break;
			}
			if (primaryRankEpithet == null) {
				pm.setInfo(" ==> could not determine lowest primary epithet");
				continue;
			}
			
			//	get higher taxonomy
			Properties higherTaxonomy = ((Properties) epithetsToHigherTaxonomies.get(primaryRankEpithet));
			if (higherTaxonomy == null)
				higherTaxonomy = selectHigherTaxonomy(primaryRankEpithet, primaryRankName, ((Properties[]) epithetsToPotentialHigherTaxonomies.get(primaryRankEpithet)), epithetFrequencies);
			if (higherTaxonomy == null) {
				pm.setInfo(" ==> higher taxonomy not found");
				pm.setInfo("   " + AnnotationUtils.produceStartTag(taxonNames[t], false));
				continue;
			}
			epithetsToHigherTaxonomies.put(primaryRankEpithet, higherTaxonomy);
			
			//	add higher taxonomy to taxon name
			Rank taxonNameRank = ((Rank) this.ranksByName.get((String) taxonNames[t].getAttribute(RANK_ATTRIBUTE)));
			for (int r = 0; r < this.primaryRanks.length; r++) {
				if (GENUS_ATTRIBUTE.equals(this.primaryRanks[r].name))
					break;
				if (this.primaryRanks[r].getRelativeSignificance() >= taxonNameRank.getRelativeSignificance())
					break; // we need to stop if current primary rank is more significant than rank of taxon name at hand
				if (higherTaxonomy.containsKey(this.primaryRanks[r].name))
					taxonNames[t].setAttribute(this.primaryRanks[r].name, higherTaxonomy.getProperty(this.primaryRanks[r].name));
			}
			if (higherTaxonomy.containsKey(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE))
				taxonNames[t].setAttribute(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, higherTaxonomy.getProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE));
			pm.setInfo(" ==> higher taxonomy added:");
			pm.setInfo("   " + AnnotationUtils.produceStartTag(taxonNames[t], false));
		}
		pm.setBaseProgress(100);
	}
	
	static void expandAbbreviatedEpithets(Annotation[] taxonNames, Properties catalogSpeciesToGenera, ProgressMonitor pm) {
		Properties abbrevsToGenera = new Properties();
		Properties speciesToGenera = new Properties();
		Properties abbrevsToSubGenera = new Properties() {
//			public synchronized Object setProperty(String key, String value) {
//				System.out.println("a-Mapped " + key + " to " + value);
//				return super.setProperty(key, value);
//			}
		};
		Properties speciesToSubGenera = new Properties() {
//			public synchronized Object setProperty(String key, String value) {
//				System.out.println("sp-Mapped " + key + " to " + value);
//				return super.setProperty(key, value);
//			}
		};
		Properties abbrevsToSpecies = new Properties();
		Properties subSpeciesToSpecies = new Properties();
		ArrayList unresolvedTaxonNames = new ArrayList();
		
		//	fill in abbreviated or omitted genera and species, as well as abbreviated subgenera
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			String tnGenus = ((String) taxonNames[t].getAttribute(GENUS_ATTRIBUTE));
			String tnSubGenus = ((String) taxonNames[t].getAttribute(SUBGENUS_ATTRIBUTE));
			String tnSpecies = ((String) taxonNames[t].getAttribute(SPECIES_ATTRIBUTE));
			String tnSubSpecies = ((String) taxonNames[t].getAttribute(SUBSPECIES_ATTRIBUTE, taxonNames[t].getAttribute(VARIETY_ATTRIBUTE)));
			
			//	species omission with subspecies or variety given, or species abbreviation, resolve it
			if ((tnSpecies == null) ? (tnSubSpecies != null) : ((tnSpecies.length() < 3) || ((tnSpecies.length() == 3) && tnSpecies.endsWith(".")))) {
				String abbrevSpecies = ((tnSpecies == null) ? null : abbrevsToSpecies.getProperty(tnSpecies));
				String subSpeciesSpecies = ((tnSubSpecies == null) ? null : subSpeciesToSpecies.getProperty(tnSubSpecies));
				if (subSpeciesSpecies != null) {
					taxonNames[t].setAttribute(SPECIES_ATTRIBUTE, subSpeciesSpecies);
					pm.setInfo("Expanded (ssp-1) species '" + tnSpecies + "' to '" + subSpeciesSpecies + "' in '" + taxonNames[t].getValue() + "'");
					tnSpecies = subSpeciesSpecies;
				}
				else if (abbrevSpecies != null) {
					taxonNames[t].setAttribute(SPECIES_ATTRIBUTE, abbrevSpecies);
					pm.setInfo("Expanded (a-1) species '" + tnSpecies + "' to '" + abbrevSpecies + "' in '" + taxonNames[t].getValue() + "'");
					tnSpecies = abbrevSpecies;
					if (tnSubSpecies != null)
						subSpeciesToSpecies.setProperty(tnSubSpecies, tnSpecies);
				}
				else {
					pm.setInfo("Could not expand (1) species '" + tnSpecies + "' in '" + taxonNames[t].getValue() + "'");
					unresolvedTaxonNames.add(taxonNames[t]);
				}
			}
			
			//	full species, index it
			else if (tnSpecies != null) {
				
				//	index possible abbreviations
				String fch = tnSpecies.substring(0, 1);
				abbrevsToSpecies.setProperty(fch, tnSpecies);
				abbrevsToSpecies.setProperty((fch + "."), tnSpecies);
				StringBuffer doneChars = new StringBuffer("e");
				for (int c = 1; c < tnSpecies.length(); c++) {
					String ch = tnSpecies.substring(c, (c+1));
					if (doneChars.indexOf(ch) == -1) {
						abbrevsToSpecies.setProperty((fch + ch), tnSpecies);
						abbrevsToSpecies.setProperty((fch + ch + "."), tnSpecies);
						doneChars.append(ch);
					}
				}
				
				//	index species
				if (tnSubSpecies != null)
					subSpeciesToSpecies.setProperty(tnSubSpecies, tnSpecies);
			}
			
			
			//	genus omission with species given, or genus abbreviation, resolve it
			if ((tnGenus == null) ? (tnSpecies != null) : ((tnGenus.length() < 3) || ((tnGenus.length() == 3) && tnGenus.endsWith(".")))) {
				String abbrevGenus = ((tnGenus == null) ? null : abbrevsToGenera.getProperty(tnGenus));
				String speciesGenus = ((tnSpecies == null) ? null : speciesToGenera.getProperty(tnSpecies));
				if (speciesGenus != null) {
					taxonNames[t].setAttribute(GENUS_ATTRIBUTE, speciesGenus);
					pm.setInfo("Expanded (sp-1) genus '" + tnGenus + "' to '" + speciesGenus + "' in '" + taxonNames[t].getValue() + "'");
					tnGenus = speciesGenus;
				}
				else if (abbrevGenus != null) {
					taxonNames[t].setAttribute(GENUS_ATTRIBUTE, abbrevGenus);
					pm.setInfo("Expanded (a-1) genus '" + tnGenus + "' to '" + abbrevGenus + "' in '" + taxonNames[t].getValue() + "'");
					tnGenus = abbrevGenus;
					if (tnSpecies != null)
						speciesToGenera.setProperty(tnSpecies, tnGenus);
				}
				else {
					pm.setInfo("Could not expand (1) genus '" + tnGenus + "' in '" + taxonNames[t].getValue() + "'");
					unresolvedTaxonNames.add(taxonNames[t]);
				}
			}
			
			//	full genus, index it
			else if (tnGenus != null) {
				
				//	index possible abbreviations
				String fch = tnGenus.substring(0, 1);
				abbrevsToGenera.setProperty(fch, tnGenus);
				abbrevsToGenera.setProperty((fch + "."), tnGenus);
				StringBuffer doneChars = new StringBuffer("e");
				for (int c = 1; c < tnGenus.length(); c++) {
					String ch = tnGenus.substring(c, (c+1));
					if (doneChars.indexOf(ch) == -1) {
						abbrevsToGenera.setProperty((fch + ch), tnGenus);
						abbrevsToGenera.setProperty((fch + ch + "."), tnGenus);
						doneChars.append(ch);
					}
				}
				
				//	index species
				if (tnSpecies != null)
					speciesToGenera.setProperty(tnSpecies, tnGenus);
			}
			
			
			//	subgenus abbreviation, try to resolve it
			if ((tnSubGenus != null) && ((tnSubGenus.length() < 3) || ((tnSubGenus.length() == 3) && tnSubGenus.endsWith(".")))) {
//				String abbrevSubGenus = abbrevsToSubGenera.getProperty(tnSubGenus);
				String genusAbbrevSubGenus = abbrevsToSubGenera.getProperty(tnGenus + " " + tnSubGenus);
				String speciesSubGenus = ((tnSpecies == null) ? null : speciesToSubGenera.getProperty(tnSubGenus + " " + tnSpecies));
				String genusSpeciesSubGenus = ((tnSpecies == null) ? null : speciesToSubGenera.getProperty(tnGenus + " " + tnSubGenus + " " + tnSpecies));
				if (genusSpeciesSubGenus != null) {
					taxonNames[t].setAttribute(SUBGENUS_ATTRIBUTE, genusSpeciesSubGenus);
					pm.setInfo("Expanded (gsp-1) subGenus '" + tnSubGenus + "' to '" + genusSpeciesSubGenus + "' in '" + taxonNames[t].getValue() + "'");
					tnSubGenus = genusSpeciesSubGenus;
				}
				else if (speciesSubGenus != null) {
					taxonNames[t].setAttribute(SUBGENUS_ATTRIBUTE, speciesSubGenus);
					pm.setInfo("Expanded (sp-1) subGenus '" + tnSubGenus + "' to '" + speciesSubGenus + "' in '" + taxonNames[t].getValue() + "'");
					tnSubGenus = speciesSubGenus;
				}
				else if (genusAbbrevSubGenus != null) {
					taxonNames[t].setAttribute(SUBGENUS_ATTRIBUTE, genusAbbrevSubGenus);
					pm.setInfo("Expanded (ga-1) subGenus '" + tnSubGenus + "' to '" + genusAbbrevSubGenus + "' in '" + taxonNames[t].getValue() + "'");
					tnSubGenus = genusAbbrevSubGenus;
				}
//				else if (abbrevSubGenus != null) {
//					taxonNames[t].setAttribute(SUBGENUS_ATTRIBUTE, abbrevSubGenus);
//					pm.setInfo("Expanded (a) subGenus '" + tnSubGenus + "' to '" + abbrevSubGenus + "' in '" + taxonNames[t].getValue() + "'");
//					tnSubGenus = abbrevSubGenus;
//				}
				else {
					pm.setInfo("Could not expand (1) subGenus '" + tnSubGenus + "' in '" + taxonNames[t].getValue() + "'");
					unresolvedTaxonNames.add(taxonNames[t]);
				}
			}
			
			//	got or found full subgenus, index it
			if ((tnSubGenus != null) && (tnSubGenus.length() > 3)) {
				
				//	index possible abbreviations
				String fch = tnSubGenus.substring(0, 1);
				abbrevsToSubGenera.setProperty(fch, tnSubGenus);
				if (tnGenus != null)
					abbrevsToSubGenera.setProperty((tnGenus + " " + fch), tnSubGenus);
				if (tnSpecies != null) {
					speciesToSubGenera.setProperty((fch + " " + tnSpecies), tnSubGenus);
					speciesToSubGenera.setProperty((tnGenus + " " + fch + " " + tnSpecies), tnSubGenus);
				}
				abbrevsToSubGenera.setProperty((fch + "."), tnSubGenus);
				if (tnGenus != null)
					abbrevsToSubGenera.setProperty((tnGenus + " " + fch + "."), tnSubGenus);
				if (tnSpecies != null) {
					speciesToSubGenera.setProperty((fch + ". " + tnSpecies), tnSubGenus);
					speciesToSubGenera.setProperty((tnGenus + " " + fch + ". " + tnSpecies), tnSubGenus);
				}
				StringBuffer doneChars = new StringBuffer("e");
				for (int c = 1; c < tnSubGenus.length(); c++) {
					String ch = tnSubGenus.substring(c, (c+1));
					if (doneChars.indexOf(ch) == -1) {
						abbrevsToSubGenera.setProperty((fch + ch), tnSubGenus);
						if (tnGenus != null)
							abbrevsToSubGenera.setProperty((tnGenus + " " + fch + ch), tnSubGenus);
						if (tnSpecies != null) {
							speciesToSubGenera.setProperty((fch + ch + " " + tnSpecies), tnSubGenus);
							speciesToSubGenera.setProperty((tnGenus + " " + fch + ch + " " + tnSpecies), tnSubGenus);
						}
						abbrevsToSubGenera.setProperty((fch + ch + "."), tnSubGenus);
						if (tnGenus != null)
							abbrevsToSubGenera.setProperty((tnGenus + " " + fch + ch + "."), tnSubGenus);
						if (tnSpecies != null) {
							speciesToSubGenera.setProperty((fch + ch + ". " + tnSpecies), tnSubGenus);
							speciesToSubGenera.setProperty((tnGenus + " " + fch + ch + ". " + tnSpecies), tnSubGenus);
						}
						doneChars.append(ch);
					}
				}
			}
		}
		
		//	sort out duplicates (taxon names with multiple abbreviations)
		for (int t = 1; t < unresolvedTaxonNames.size(); t++) {
			if (unresolvedTaxonNames.get(t-1) == unresolvedTaxonNames.get(t))
				unresolvedTaxonNames.remove(t--);
		}
		
		//	handle remaining unresolved taxon names (can happen if species epithets occur standalone, e.g. in a key, before the first occurrence of the full name, or only standalone at all, e.g. in a checklist)
		for (int t = 0; t < unresolvedTaxonNames.size(); t++) {
			pm.setProgress((t * 100) / unresolvedTaxonNames.size());
			Annotation taxonName = ((Annotation) unresolvedTaxonNames.get(t));
			String tnGenus = ((String) taxonName.getAttribute(GENUS_ATTRIBUTE));
			String tnSubGenus = ((String) taxonName.getAttribute(SUBGENUS_ATTRIBUTE));
			String tnSpecies = ((String) taxonName.getAttribute(SPECIES_ATTRIBUTE));
			String tnSubSpecies = ((String) taxonName.getAttribute(SUBSPECIES_ATTRIBUTE, taxonName.getAttribute(VARIETY_ATTRIBUTE)));
			
			//	species omission with subspecies or variety given, or species abbreviation, resolve it
			if ((tnSpecies == null) ? (tnSubSpecies != null) : ((tnSpecies.length() < 3) || ((tnSpecies.length() == 3) && tnSpecies.endsWith(".")))) {
				String subSpeciesSpecies = ((tnSubSpecies == null) ? null : subSpeciesToSpecies.getProperty(tnSubSpecies));
				if (subSpeciesSpecies != null) {
					taxonName.setAttribute(SPECIES_ATTRIBUTE, subSpeciesSpecies);
					pm.setInfo("Expanded (ssp-2) species '" + tnSpecies + "' to '" + subSpeciesSpecies + "' in '" + taxonName.getValue() + "'");
					tnSpecies = subSpeciesSpecies;
				}
				else pm.setInfo("Could not expand (2) species '" + tnSpecies + "' in '" + taxonName.getValue() + "'");
			}
			
			//	genus omission with species given, or genus abbreviation, resolve it
			if ((tnGenus == null) ? (tnSpecies != null) : ((tnGenus.length() < 3) || ((tnGenus.length() == 3) && tnGenus.endsWith(".")))) {
				String speciesGenus = ((tnSpecies == null) ? null : speciesToGenera.getProperty(tnSpecies));
				String catalogSpeciesGenus = ((tnSpecies == null) ? null : catalogSpeciesToGenera.getProperty(tnSpecies));
				if (speciesGenus != null) {
					taxonName.setAttribute(GENUS_ATTRIBUTE, speciesGenus);
					pm.setInfo("Expanded (sp-2) genus '" + tnGenus + "' to '" + speciesGenus + "' in '" + taxonName.getValue() + "'");
					tnGenus = speciesGenus;
				}
				else if (catalogSpeciesGenus != null) {
					taxonName.setAttribute(GENUS_ATTRIBUTE, catalogSpeciesGenus);
					pm.setInfo("Expanded (csp-2) genus '" + tnGenus + "' to '" + catalogSpeciesGenus + "' in '" + taxonName.getValue() + "'");
					tnGenus = catalogSpeciesGenus;
				}
				else pm.setInfo("Could not expand (2) genus '" + tnGenus + "' in '" + taxonName.getValue() + "'");
			}
			
			//	subgenus abbreviation, try to resolve it
			if ((tnSubGenus != null) && ((tnSubGenus.length() < 3) || ((tnSubGenus.length() == 3) && tnSubGenus.endsWith(".")))) {
				String abbrevSubGenus = abbrevsToSubGenera.getProperty(tnSubGenus);
				String genusAbbrevSubGenus = abbrevsToSubGenera.getProperty(tnGenus + " " + tnSubGenus);
				String speciesSubGenus = ((tnSpecies == null) ? null : speciesToSubGenera.getProperty(tnSubGenus + " " + tnSpecies));
				String genusSpeciesSubGenus = ((tnSpecies == null) ? null : speciesToSubGenera.getProperty(tnGenus + " " + tnSubGenus + " " + tnSpecies));
				if (genusSpeciesSubGenus != null) {
					taxonName.setAttribute(SUBGENUS_ATTRIBUTE, genusSpeciesSubGenus);
					pm.setInfo("Expanded (gsp-2) subGenus '" + tnSubGenus + "' to '" + genusSpeciesSubGenus + "' in '" + taxonName.getValue() + "'");
					tnSubGenus = genusSpeciesSubGenus;
				}
				else if (speciesSubGenus != null) {
					taxonName.setAttribute(SUBGENUS_ATTRIBUTE, speciesSubGenus);
					pm.setInfo("Expanded (sp-2) subGenus '" + tnSubGenus + "' to '" + speciesSubGenus + "' in '" + taxonName.getValue() + "'");
					tnSubGenus = speciesSubGenus;
				}
				else if (genusAbbrevSubGenus != null) {
					taxonName.setAttribute(SUBGENUS_ATTRIBUTE, genusAbbrevSubGenus);
					pm.setInfo("Expanded (ga-2) subGenus '" + tnSubGenus + "' to '" + genusAbbrevSubGenus + "' in '" + taxonName.getValue() + "'");
					tnSubGenus = genusAbbrevSubGenus;
				}
				else if (abbrevSubGenus != null) {
					taxonName.setAttribute(SUBGENUS_ATTRIBUTE, abbrevSubGenus);
					pm.setInfo("Expanded (a-2) subGenus '" + tnSubGenus + "' to '" + abbrevSubGenus + "' in '" + taxonName.getValue() + "'");
					tnSubGenus = abbrevSubGenus;
				}
				else pm.setInfo("Could not expand (2) subGenus '" + tnSubGenus + "' in '" + taxonName.getValue() + "'");
			}
		}
	}
	
	void indexLabeledAndPotentialEpithets(MutableAnnotation doc, AnnotationIndex taxonNamePartIndex) {
		Dictionary nonEpithets = new SetDictionary(this.stopWords);
		Annotation[] potentialSpeciesOrLowerEpithets = Gamta.extractAllMatches(doc, "[a-z]{3,}", nonEpithets, nonEpithets);
		taxonNamePartIndex.addAnnotations(potentialSpeciesOrLowerEpithets, (POTENTIAL_EPITHET_PREFIX + SPECIES_ATTRIBUTE));
		for (int r = 0; r < this.belowSpeciesRanks.length; r++) {
			Annotation[] rankEpithets = AnnotationPatternMatcher.getMatches(doc, ("(" + this.rankLabelMatchers.getProperty(this.belowSpeciesRanks[r].name) + ") \"[a-z]{3,}\""));
//			for (int e = 0; e < rankEpithets.length; e++)
//				System.out.println("Epithet of '" + this.belowSpeciesRanks[r].name + "': " + rankEpithets[e].toXML());
			taxonNamePartIndex.addAnnotations(rankEpithets, this.belowSpeciesRanks[r].name);
			taxonNamePartIndex.addAnnotations(potentialSpeciesOrLowerEpithets, (POTENTIAL_EPITHET_PREFIX + this.belowSpeciesRanks[r].name));
		}
	}
	
	void indexAuthorities(MutableAnnotation doc, AnnotationIndex taxonNamePartIndex, StringVector genusAndAboveEpithetDict, StringVector countryRegionStartDict, TreeSet authorityStopWords, boolean filterMultiAuthorities) {
		Dictionary nonAuthorityStartDict = new AuthorityStartFilter(genusAndAboveEpithetDict, countryRegionStartDict, authorityStopWords);
		Dictionary nonAuthorityDict = new AuthorityStartFilter(genusAndAboveEpithetDict, countryRegionStartDict, new TreeSet() {
			public boolean contains(Object obj) {
				//	 filter out family names based on suffixes, even if they are not seeds due to cutoffs
				String objStr = obj.toString().toLowerCase();
				return (objStr.endsWith("idae") || objStr.endsWith("aceae"));
			}
		});
		
		//	index plain authority names
		Annotation[] authorityNames = Gamta.extractAllMatches(doc, "" +
				"(([A-Z][a-z]+)|([A-Za-z]\\'))?" + // optional prefixes like in 'Della Torre', 'O'Neil', or 'McPearson'
				"[A-Z][a-z]{1,}\\.?" + // main last name (got to live with two letters here for names like 'Yu' and 'Ng')
				"(\\-[A-Z][a-z]{2,}\\.?)?" + // second last name
				"", nonAuthorityStartDict, nonAuthorityDict, true);
		taxonNamePartIndex.addAnnotations(authorityNames, AUTHORITY_NAME_ATTRIBUTE);
//		for (int a = 0; a < authorityNames.length; a++)
//			System.out.println("AuthorityName1: " + authorityNames[a].getValue() + " at " + authorityNames[a].getStartIndex());
		
		//	tag authority name prefixes
		Annotation[] authorityNamePrefixes = Gamta.extractAllContained(doc, new SetDictionary(this.authorityPrefixes));
		taxonNamePartIndex.addAnnotations(authorityNamePrefixes, (AUTHORITY_NAME_ATTRIBUTE + "Prefix"));
		
		//	combine authority names with prefixes
		authorityNames = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"<authorityNamePrefix> <authorityName>");
		taxonNamePartIndex.addAnnotations(authorityNames, AUTHORITY_NAME_ATTRIBUTE);
//		for (int a = 0; a < authorityNames.length; a++)
//			System.out.println("AuthorityNameP: " + authorityNames[a].getValue() + " at " + authorityNames[a].getStartIndex());
		
		//	assemble double names
		authorityNames = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"<authorityName> <authorityName>");
		taxonNamePartIndex.addAnnotations(authorityNames, AUTHORITY_NAME_ATTRIBUTE);
//		for (int a = 0; a < authorityNames.length; a++)
//			System.out.println("AuthorityName2: " + authorityNames[a].getValue() + " at " + authorityNames[a].getStartIndex());
		
		//	add leading initials ...
		authorityNames = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"(\"[A-Z][a-z]?\" '.'?)+ <authorityName>");
		taxonNamePartIndex.addAnnotations(authorityNames, AUTHORITY_NAME_ATTRIBUTE);
//		for (int a = 0; a < authorityNames.length; a++)
//			System.out.println("AuthorityName3: " + authorityNames[a].getValue() + " at " + authorityNames[a].getStartIndex());
		
		//	... as well as tailing ones
		authorityNames = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"<authorityName> ','? (\"[A-Z][a-z]?\" '.'?)+");
		taxonNamePartIndex.addAnnotations(authorityNames, AUTHORITY_NAME_ATTRIBUTE);
//		for (int a = 0; a < authorityNames.length; a++)
//			System.out.println("AuthorityName4: " + authorityNames[a].getValue() + " at " + authorityNames[a].getStartIndex());
		
		//	index years, including any subsequent page numbers
		Annotation[] authorityYears = Gamta.extractAllMatches(doc, "[12][0-9]{3}");
		taxonNamePartIndex.addAnnotations(authorityYears, AUTHORITY_YEAR_ATTRIBUTE);
		authorityYears = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"<authorityYear> \"[a-f]\"?':' \"[1-9][0-9]{0,3}\"");
		taxonNamePartIndex.addAnnotations(authorityYears, AUTHORITY_YEAR_ATTRIBUTE);
		
		//	index years with parentheses around them
		authorityYears = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"'(' <authorityYear> \"[a-f]\"? (')'|'))')");
		taxonNamePartIndex.addAnnotations(authorityYears, AUTHORITY_YEAR_ATTRIBUTE);
		
		//	assemble full authorities, including enumerations, as well as ones combined with 'ex'
		Annotation[] authorities = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"<authorityName> (\"(\\\\,|\\\\&|ex|et|and)\" <authorityName>)* 'et al.'? (','? <authorityYear> \"[a-f]\"?)?"); // max value 8 from Donat
		taxonNamePartIndex.addAnnotations(authorities, AUTHORITY_ATTRIBUTE);
//		for (int a = 0; a < authorities.length; a++) {
//			System.out.println("Authority1: " + authorities[a].getValue() + " at " + authorities[a].getStartIndex());
////			doc.addAnnotation((TAXONOMIC_NAME_ANNOTATION_TYPE + "AuthorityDebug"), authorities[a].getStartIndex(), authorities[a].size());
//		}
		
		//	add authorities in parentheses
		authorities = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"'(' <authority> (')'|'))')");
		taxonNamePartIndex.addAnnotations(authorities, AUTHORITY_ATTRIBUTE);
//		for (int a = 0; a < authorities.length; a++) {
//			System.out.println("Authority2: " + authorities[a].getValue());
////			doc.addAnnotation((TAXONOMIC_NAME_ANNOTATION_TYPE + "AuthorityDebug"), authorities[a].getStartIndex(), authorities[a].size());
//		}
		
		//	add multi-authorities (four times chain any 2 authorities, rather than once chaining any 8, as that vastly reduces complexity, and chains any 16 authorities nonetheless)
		for (int r = 0; r < 4; r++) {
			authorities = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
					"<authority> <authority>");
			if (filterMultiAuthorities)
				authorities = this.filterDoubleAuthorities(authorities);
			taxonNamePartIndex.addAnnotations(authorities, AUTHORITY_ATTRIBUTE);
//			for (int a = 0; a < authorities.length; a++) {
//				System.out.println("Multi-Authority" + (r+1) + ": " + authorities[a].getValue());
////				doc.addAnnotation((TAXONOMIC_NAME_ANNOTATION_TYPE + "AuthorityDebug"), authorities[a].getStartIndex(), authorities[a].size());
//			}
		}
//		authorities = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
//				"<authority>{2,}");
//		taxonNamePartIndex.addAnnotations(authorities, AUTHORITY_ATTRIBUTE);
//		for (int a = 0; a < authorities.length; a++) {
//			System.out.println("Multi-Authority: " + authorities[a].getValue());
////			doc.addAnnotation((TAXONOMIC_NAME_ANNOTATION_TYPE + "AuthorityDebug"), authorities[a].getStartIndex(), authorities[a].size());
//		}
		
		//	add authorities combined with 'sensu', 'emend.'
		authorities = AnnotationPatternMatcher.getMatches(doc, taxonNamePartIndex,
				"<authority>? ('sensu'|'emend.'|'em.') <authority>");
		taxonNamePartIndex.addAnnotations(authorities, AUTHORITY_ATTRIBUTE);
//		for (int a = 0; a < authorities.length; a++) {
//			System.out.println("Sensu-Authority: " + authorities[a].getValue());
////			doc.addAnnotation((TAXONOMIC_NAME_ANNOTATION_TYPE + "AuthorityDebug"), authorities[a].getStartIndex(), authorities[a].size());
//		}
	}
	
	private Annotation[] filterDoubleAuthorities(Annotation[] authorities) {
		if (authorities.length == 0)
			return authorities;
		
		//	tokenize filter values
		TokenSequence backToBackParentheses = Gamta.newTokenSequence(") (", authorities[0].getTokenizer());
		
		//	do filtering
		ArrayList authorityList = new ArrayList();
		for (int a = 0; a < authorities.length; a++) {
			
			//	make sure to not create multi-authorities if both parts are in parentheses
			if (TokenSequenceUtils.indexOf(authorities[a], backToBackParentheses) != -1) {
//				System.out.println("Double authority filtered for back-to-back parentheses: " + doubleAuthorities[a].toXML());
				continue;
			}
			
			//	make sure to not create multi-authorities if first ends with year and second part is in parentheses
			Annotation[] yearBeforeParenthesis = Gamta.extractAllMatches(authorities[a], "[12][0-9]{3}(\\s*[a-f])?\\s*\\(");
			if (yearBeforeParenthesis.length != 0) {
//				System.out.println("Double authority filtered for year before parenthesis: " + doubleAuthorities[a].toXML());
				continue;
			}
			
//			System.out.println("Double authority retained: " + doubleAuthorities[a].toXML());
			authorityList.add(authorities[a]);
		}
		if (authorityList.size() < authorities.length)
			authorities = ((Annotation[]) authorityList.toArray(new Annotation[authorityList.size()]));
		
		//	return what's left
		return authorities;
	}
	
	void indexStatusLabels(MutableAnnotation doc, AnnotationIndex taxonNamePartIndex) {
		
		//	find plain epithet labels first
		Annotation[] taxonNameLabels = Gamta.extractAllContained(doc, this.labelDict, 2);
		for (int l = 0; l < taxonNameLabels.length; l++) {
			String rank = ((String) this.labelsToRanks.get(taxonNameLabels[l].getValue()));
			if ((rank != null) && !"".equals(rank))
				taxonNameLabels[l].setAttribute(RANK_ATTRIBUTE, rank);
		}
		taxonNamePartIndex.addAnnotations(taxonNameLabels, "label");
//		for (int l = 0; l < taxonNameLabels.length; l++)
//			System.out.println("Label: " + taxonNameLabels[l].toXML());
		
		//	find labels for new epithets
		Annotation[] newTaxonNameLabels = Gamta.extractAllContained(doc, this.newLabelDict, 4);
		taxonNamePartIndex.addAnnotations(newTaxonNameLabels, NEW_LABEL_ANNOTATION_TYPE);
		for (int l = 0; l < newTaxonNameLabels.length; l++) {
			String rank = ((String) this.newLabelsToRanks.get(newTaxonNameLabels[l].getValue()));
			if ((rank != null) && !"".equals(rank))
				newTaxonNameLabels[l].setAttribute(RANK_ATTRIBUTE, rank);
		}
//		for (int l = 0; l < newTaxonNameLabels.length; l++)
//			System.out.println("NewLabel: " + newTaxonNameLabels[l].toXML());
		
		//	find labels for new epithets with new parent epithets, 'new' first ('nov. gen. et spec.', etc.)
		MatchTree[] combNewTaxonNameLabelsNf = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<newLabel test=\"matches(./#first, '[nN].*')\"> ('et'|'and'|'und'|'y'|'e') <label>"
				);
		for (int l = 0; l < combNewTaxonNameLabelsNf.length; l++) {
			String rank = this.getRankOfLast(combNewTaxonNameLabelsNf[l], "label");
			if ((rank != null) && !"".equals(rank))
				combNewTaxonNameLabelsNf[l].getMatch().setAttribute(RANK_ATTRIBUTE, rank);
		}
//		for (int l = 0; l < combNewTaxonNameLabelsNf.length; l++)
//			System.out.println("CombNewLabel: " + combNewTaxonNameLabelsNf[l].getMatch().toXML());
		
		//	find labels for new epithets with new parent epithets, 'new' last ('gen. et spec. nov.', etc.)
		MatchTree[] combNewTaxonNameLabelsNl = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				"<label> ('et'|'and'|'und'|'y'|'e')  <newLabel test=\"not(matches(./#first, '[nN].*'))\">"
				);
		for (int l = 0; l < combNewTaxonNameLabelsNl.length; l++) {
			String rank = this.getRankOfLast(combNewTaxonNameLabelsNl[l], "newLabel");
			if ((rank != null) && !"".equals(rank))
				combNewTaxonNameLabelsNl[l].getMatch().setAttribute(RANK_ATTRIBUTE, rank);
		}
//		for (int l = 0; l < combNewTaxonNameLabelsNl.length; l++)
//			System.out.println("CombNewLabel: " + combNewTaxonNameLabelsNl[l].getMatch().toXML());
		
		//	find enumerations of labels for new epithets ('gen. nov. et spec. nov.', etc.)
		MatchTree[] combNewTaxonNameLabelsEnum = AnnotationPatternMatcher.getMatchTrees(doc, taxonNamePartIndex,
				" <newLabel> ('et'|'and'|'und'|'y'|'e'|',') <newLabel>"
				);
		for (int l = 0; l < combNewTaxonNameLabelsEnum.length; l++) {
			String rank = this.getRankOfLast(combNewTaxonNameLabelsEnum[l], "newLabel");
			if ((rank != null) && !"".equals(rank))
				combNewTaxonNameLabelsEnum[l].getMatch().setAttribute(RANK_ATTRIBUTE, rank);
		}
//		for (int l = 0; l < combNewTaxonNameLabelsEnum.length; l++)
//			System.out.println("EnumNewLabel: " + combNewTaxonNameLabelsEnum[l].getMatch().toXML());
		
		//	index combined new epithet labels only now, to prevent sequencing combinations
		taxonNamePartIndex.addAnnotations(this.getMatches(combNewTaxonNameLabelsNf), NEW_LABEL_ANNOTATION_TYPE);
		taxonNamePartIndex.addAnnotations(this.getMatches(combNewTaxonNameLabelsNl), NEW_LABEL_ANNOTATION_TYPE);
		taxonNamePartIndex.addAnnotations(this.getMatches(combNewTaxonNameLabelsEnum), NEW_LABEL_ANNOTATION_TYPE);
		
		Annotation[] sensuLabels = Gamta.extractAllContained(doc, this.sensuLabelDict, 4);
		taxonNamePartIndex.addAnnotations(sensuLabels, SENSU_LABEL_ANNOTATION_TYPE);
		for (int l = 0; l < sensuLabels.length; l++) {
			String sensu = ((String) this.sensuLabelsToSensus.get(sensuLabels[l].getValue()));
			if ((sensu != null) && !"".equals(sensu))
				sensuLabels[l].setAttribute(SENSU_ATTRIBUTE, sensu);
		}
//		for (int l = 0; l < sensuLabels.length; l++)
//			System.out.println("SensuLabel: " + sensuLabels[l].toXML());
	}
	
	private String getRankOfLast(MatchTree combStatusLabel, String subLabelType) {
//		System.out.println(combStatusLabel.toString("  "));
		ArrayList mtnList = new ArrayList(5);
		mtnList.add(combStatusLabel);
		MatchTreeNode lastTypeMtn = null;
		for (int n = 0; n < mtnList.size(); n++) {
			MatchTreeNode mtn = ((MatchTreeNode) mtnList.get(n));
			if ((n != 0) /* pattern is null on match tree root */ && mtn.getPattern().startsWith("<" + subLabelType))
				lastTypeMtn = mtn;
			MatchTreeNode[] cMtns = mtn.getChildren();
			if (cMtns != null)
				mtnList.addAll(Arrays.asList(cMtns));
		}
		return ((lastTypeMtn == null) ? null : ((String) lastTypeMtn.getMatch().getAttribute(RANK_ATTRIBUTE)));
	}
	
	private Annotation[] getMatches(MatchTree[] matchTrees) {
		Annotation[] matches = new Annotation[matchTrees.length];
		for (int m = 0; m < matchTrees.length; m++)
			matches[m] = matchTrees[m].getMatch();
		return matches;
	}
	
	void indexInfixesAndInNameSymbols(MutableAnnotation doc, AnnotationIndex taxonNamePartIndex) {
		Annotation[] inNameSymbols = Gamta.extractAllMatches(doc, "[^a-zA-Z0-9\\,\\.\\:\\;\\#\\(\\)\\[\\]\\{\\}\\\\\\/\\~\\|\\-\\u00AD\\u2010-\\u2015\\u2212]", 1);
		taxonNamePartIndex.addAnnotations(inNameSymbols, IN_NAME_SYMBOL_ANNOTATION_TYPE);
//		for (int s = 0; s < inNameSymbols.length; s++) {
//			System.out.println("Symbol: " + inNameSymbols[s].getValue());
//			doc.addAnnotation("inNameSymbolDebug", inNameSymbols[s].getStartIndex(), inNameSymbols[s].size());
//		}
		Annotation[] nameInfixes = Gamta.extractAllMatches(doc, 
				"((cf|aff|fl)\\.)" +
				"|" +
				"(\\[sic\\!?\\])" +
				"|" +
				"(\\(sic\\!?\\))" +
				"|" +
				"(\\[" +
					"((s\\.?\\s*)|(sensu\\s+))" +
					"((str\\.?)|stricto|(l\\.?)|lato)" +
				"\\])" +
				"|" +
				"(\\(" +
					"((s\\.?\\s*)|(sensu\\s+))" +
					"((str\\.?)|stricto|(l\\.?)|lato)" +
				"\\))" +
				"", 6);
		taxonNamePartIndex.addAnnotations(nameInfixes, NAME_INFIX_ANNOTATION_TYPE);
//		for (int i = 0; i < nameInfixes.length; i++) {
//			System.out.println("Infix: " + nameInfixes[i].getValue());
//			doc.addAnnotation("nameInfixDebug", nameInfixes[i].getStartIndex(), nameInfixes[i].size());
//		}
	}
	
	int getHigherTaxonomyLookupTimeout() {
		return this.higherTaxonomyProvider.getLookupTimeout();
	}
	
	void setHigherTaxonomyLookupTimeout(int timeout) {
		this.higherTaxonomyProvider.setLookupTimeout(timeout);
	}
	
	Properties getHigherTaxonomy(String epithet, String rank, boolean allowWebAccess) {
		return this.higherTaxonomyProvider.getHierarchy(epithet, rank, allowWebAccess);
	}
	
	boolean isKnownNegativeEpithet(String str) {
		return this.higherTaxonomyProvider.isKnownNegative(str);
	}
	
	static Properties selectHigherTaxonomy(String epithet, String rank, Properties[] hierarchies, CountingSet epithetFrequencies) {
		if ((hierarchies == null) || (hierarchies.length == 0))
			return null;
		Properties higherTaxonomy = null;
		int higherTaxonomyScore = 0;
		for (int h = 0; h < hierarchies.length; h++) {
//			System.out.println("Scoring for " + rank + " '" + epithet + "': " + hierarchies[h]);
			int hierarchyScore = 0;
			for (int r = 0; r < primaryRankNames.length; r++) {
				if (primaryRankNames[r].equals(GENUS_ATTRIBUTE))
					break;
				if (hierarchies[h].containsKey(primaryRankNames[r]))
					hierarchyScore += epithetFrequencies.getCount(hierarchies[h].getProperty(primaryRankNames[r]));
			}
			if (hierarchyScore > higherTaxonomyScore) {
//				System.out.println(" ==> score is " + hierarchyScore + ", new top score");
				higherTaxonomy = hierarchies[h];
				higherTaxonomyScore = hierarchyScore;
			}
//			else System.out.println(" ==> score is " + hierarchyScore + ", less than current best");
		}
		return higherTaxonomy;
	}
	
	static String getTaxonNameKey(MatchTree taxonNameMatch) {
		return getTaxonNameKey(taxonNameMatch.getMatch());
	}
	static String getTaxonNameKey(Annotation taxonName) {
		return (taxonName.getStartIndex() + "-" + taxonName.getEndIndex());
	}
	
	Annotation annotateTaxonName(MutableAnnotation doc, MatchTree taxonNameMatch, String checkRankName, Set tableCellEndIndices, ProgressMonitor pm, String step) {
		return this.annotateTaxonName(doc, true, taxonNameMatch, checkRankName, tableCellEndIndices, pm, step);
	}
	
	Annotation annotateTaxonName(MutableAnnotation doc, boolean addToDoc, MatchTree taxonNameMatch, String checkRankName, Set tableCellEndIndices, ProgressMonitor pm, String step) {
		
		//	check if epithets match style-wise
		Annotation firstRankEpithet = null;
		boolean firstRankEpithetBold = false;
		boolean firstRankEpithetItalics = false;
		for (int r = 0; r < this.ranks.length; r++) {
			Annotation rankEpithet = findEpithet(taxonNameMatch, this.ranks[r].name);
			if (rankEpithet == null)
				continue;
			if (firstRankEpithet == null) {
				firstRankEpithet = rankEpithet;
				if (".".equals(firstRankEpithet.lastValue())) {
					firstRankEpithetBold = firstRankEpithet.firstToken().hasAttribute(BOLD_ATTRIBUTE);
					firstRankEpithetItalics = firstRankEpithet.firstToken().hasAttribute(ITALICS_ATTRIBUTE);
				}
				else {
					firstRankEpithetBold = firstRankEpithet.lastToken().hasAttribute(BOLD_ATTRIBUTE);
					firstRankEpithetItalics = firstRankEpithet.lastToken().hasAttribute(ITALICS_ATTRIBUTE);
				}
			}
			else {
				boolean rankEpithetBold = false;
				boolean rankEpithetItalics = false;
				if (".".equals(rankEpithet.lastValue())) {
					rankEpithetBold = rankEpithet.firstToken().hasAttribute(BOLD_ATTRIBUTE);
					rankEpithetItalics = rankEpithet.firstToken().hasAttribute(ITALICS_ATTRIBUTE);
				}
				else {
					rankEpithetBold = rankEpithet.lastToken().hasAttribute(BOLD_ATTRIBUTE);
					rankEpithetItalics = rankEpithet.lastToken().hasAttribute(ITALICS_ATTRIBUTE);
				}
				if (firstRankEpithetBold != rankEpithetBold) {
					System.out.println(" - bold property broken at " + rankEpithet.lastValue());
					return null;
				}
				if (firstRankEpithetItalics != rankEpithetItalics) {
					System.out.println(" - italics property broken at " + rankEpithet.lastValue());
					return null;
				}
			}
		}
		
		//	make sure not to annotate across paragraph or table cell boundaries
		Annotation taxonName = taxonNameMatch.getMatch();
		int taxonNameCutoffIndex = -1;
		for (int t = 0; t < (taxonName.size()-1); t++) {
			if (taxonName.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				taxonNameCutoffIndex = (taxonName.getStartIndex() + t + 1);
				break;
			}
			if (tableCellEndIndices.contains(new Integer(taxonName.getStartIndex() + t + 1))) {
				taxonNameCutoffIndex = (taxonName.getStartIndex() + t + 1);
				break;
			}
		}
//		
//		System.out.println("TaxonName (" + step + "): " + taxonName.getValue() + " at " + taxonName.getStartIndex());
//		System.out.println(taxonNameMatch.toString("  "));
		
		//	make it a live annotation
		taxonName.changeTypeTo(TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	add attributes, and find potential cutoff
		taxonNameCutoffIndex = this.addAttributes(doc, taxonName, taxonNameCutoffIndex, taxonNameMatch.getChildren());
		
		//	nothing to cut
		if (taxonNameCutoffIndex == -1) {
			taxonName.changeTypeTo(TAXONOMIC_NAME_ANNOTATION_TYPE);
			if (addToDoc)
				taxonName = doc.addAnnotation(taxonName);
			pm.setInfo(taxonName.getValue() + " at " + taxonName.getStartIndex());
		}
		
		//	perform cutoff
		else {
			while ((taxonName.getStartIndex() < taxonNameCutoffIndex) && (",:;(".indexOf(doc.valueAt(taxonNameCutoffIndex-1)) != -1))
				taxonNameCutoffIndex--;
			if (taxonNameCutoffIndex <= taxonName.getStartIndex())
				return null;
			if ((checkRankName != null) && !taxonName.hasAttribute(checkRankName))
				return null;
			Annotation cutTaxonNameAnnot;
			if (addToDoc)
				cutTaxonNameAnnot = doc.addAnnotation(TAXONOMIC_NAME_ANNOTATION_TYPE, taxonName.getStartIndex(), (taxonNameCutoffIndex - taxonName.getStartIndex()));
			else cutTaxonNameAnnot = Gamta.newAnnotation(doc, TAXONOMIC_NAME_ANNOTATION_TYPE, taxonName.getStartIndex(), (taxonNameCutoffIndex - taxonName.getStartIndex()));
			cutTaxonNameAnnot.copyAttributes(taxonName);
			taxonName = cutTaxonNameAnnot;
			pm.setInfo(taxonName.getValue() + " at " + taxonName.getStartIndex());
		}
		
		//	make match tractable
		taxonName.setAttribute("_step", step);
		
		//	finally ...
		return taxonName;
	}
	
	private int addAttributes(MutableAnnotation doc, Annotation taxonName, int taxonNameCutoffIndex, MatchTreeNode[] mtns) {
		int cutoffIndex = -1;
		for (int n = 0; n < mtns.length; n++) {
			if (mtns[n].getPattern().startsWith("<")) {
				Annotation matchAnnot = mtns[n].getMatch();
				if (taxonNameCutoffIndex != -1) {
					if (taxonNameCutoffIndex <= matchAnnot.getStartIndex())
						continue;
					if (taxonNameCutoffIndex < matchAnnot.getEndIndex())
						matchAnnot = Gamta.newAnnotation(doc, null, matchAnnot.getStartIndex(), (taxonNameCutoffIndex - matchAnnot.getStartIndex()));
				}
				String attributeName = mtns[n].getPattern().replaceAll("[\\<\\>\\?]", "");
				if (attributeName.indexOf(' ') != -1)
					attributeName = attributeName.substring(0, attributeName.indexOf(' '));
				if (attributeName.startsWith(POTENTIAL_EPITHET_PREFIX))
					attributeName = attributeName.substring(POTENTIAL_EPITHET_PREFIX.length());
				if (attributeName.startsWith(ABBREVIATED_EPITHET_PREFIX)) {
					taxonName.setAttribute(attributeName.substring(ABBREVIATED_EPITHET_PREFIX.length()), matchAnnot.getValue());
					cutoffIndex = -1; // reset cutoff
				}
				else if (this.ranksByName.containsKey(attributeName)) {
					String rankEpithet = matchAnnot.lastValue();
					if (!rankEpithet.matches("[A-Za-z\\-]{3,}")) {
						if (cutoffIndex == -1) // cut before incomplete epithet if not cutting yet
							cutoffIndex = matchAnnot.getStartIndex();
						continue;
					}
					if (((Rank) this.ranksByName.get(attributeName)).getRelativeSignificance() < this.speciesRankRelativeSignificance) {
						if (!rankEpithet.matches("[A-Z][a-z\\-]+"))
							rankEpithet = (rankEpithet.substring(0, 1) + rankEpithet.substring(1).toLowerCase());
					}
					else {
						if (!rankEpithet.matches("[a-z\\-]+"))
							rankEpithet = rankEpithet.toLowerCase();
					}
					taxonName.setAttribute(attributeName, rankEpithet);
					taxonName.setAttribute(RANK_ATTRIBUTE, attributeName);
					cutoffIndex = -1; // reset cutoff
					
					//	authority for taxon name proper has to follow last epithet
					taxonName.removeAttribute(AUTHORITY_ATTRIBUTE);
					taxonName.removeAttribute(AUTHORITY_NAME_ATTRIBUTE);
					taxonName.removeAttribute(AUTHORITY_YEAR_ATTRIBUTE);
					taxonName.removeAttribute("authorityPageNumber");
				}
				else if (AUTHORITY_ATTRIBUTE.equals(attributeName)) {
					String authority = matchAnnot.getValue();
					
					//	truncate leading and closing parentheses, but retain balance
					while (authority.startsWith("("))
						authority = authority.substring("(".length()).trim();
					if (authority.indexOf('(') == -1) {
						while (authority.endsWith(")"))
							authority = authority.substring(0, (authority.length() - ")".length())).trim();
					}
					else if (authority.endsWith("))"))
						authority = authority.substring(0, (authority.length() - ")".length())).trim();
					
					//	balance embedded parentheses (occur in double authorities in botany)
					if ((authority.indexOf(')') != -1) && ((authority.indexOf('(') == -1) || (authority.indexOf('(') > authority.indexOf(')'))))
						authority = ("(" + authority);
					
					//	TODO ask Donat whether or not to restrict authority to part after 'sensu' altogether
					
					//	truncate leading 'sensu'
					if (authority.startsWith("sensu"))
						authority = authority.substring("sensu".length()).trim();
					
					//	store whole authority
					taxonName.setAttribute(AUTHORITY_ATTRIBUTE, authority);
					
					//	truncate any remaining parentheses
					while (authority.endsWith(")"))
						authority = authority.substring(0, (authority.length() - ")".length())).trim();
					
					//	chunk page number (including colon)
					if (authority.matches(".*\\:\\s*[1-9][0-9]{0,3}")) {
						String authorityPageNumber = authority.substring(authority.lastIndexOf(':') + ":".length()).trim();
						taxonName.setAttribute("authorityPageNumber", authorityPageNumber);
						authority = authority.substring(0, authority.lastIndexOf(':')).trim();
					}
					
					//	cut index letter
					if (authority.matches(".*[12][0-9]{3}\\s?[a-f]"))
						authority = authority.substring(0, (authority.length() - 1)).trim();
					
					//	chunk year (including any commas or opening parentheses)
					if (authority.matches(".*[12][0-9]{3}")) {
						String authorityYear = authority.substring(authority.length() - 4).trim();
						taxonName.setAttribute(AUTHORITY_YEAR_ATTRIBUTE, authorityYear);
						authority = authority.substring(0, (authority.length() - 4)).trim();
						while (authority.endsWith("(") || authority.endsWith(","))
							authority = authority.substring(0, (authority.length() - "(".length())).trim();
					}
					
					//	what's left should be the name
					taxonName.setAttribute(AUTHORITY_NAME_ATTRIBUTE, authority);
					cutoffIndex = -1; // reset cutoff to include authority
				}
				else if (NEW_LABEL_ANNOTATION_TYPE.equals(attributeName)) {
					
					//	annotate status label
					Annotation newLabel = matchAnnot;
					newLabel.changeTypeTo(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
					newLabel = doc.addAnnotation(newLabel);
					
					//	mark status
					taxonName.setAttribute(TAXONOMIC_NAME_STATUS_ATTRIBUTE, newLabel.getValue());
					
					if (cutoffIndex == -1) // cut before new label if not cutting yet
						cutoffIndex = newLabel.getStartIndex();
				}
				else if (SENSU_LABEL_ANNOTATION_TYPE.equals(attributeName)) {
					
					//	annotate status label
					Annotation sensuLabel = matchAnnot;
					sensuLabel.changeTypeTo(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
					sensuLabel = doc.addAnnotation(sensuLabel);
					
					//	mark sensu
					taxonName.setAttribute(SENSU_ATTRIBUTE, sensuLabel.getAttribute(SENSU_ATTRIBUTE));
					
					if (cutoffIndex == -1) // cut before sensu label if not cutting yet
						cutoffIndex = sensuLabel.getStartIndex();
				}
				else if (IN_NAME_SYMBOL_ANNOTATION_TYPE.equals(attributeName)) {
					if (cutoffIndex == -1) // cut before symbol if not cutting yet
						cutoffIndex = matchAnnot.getStartIndex();
				}
			}
			else {
				MatchTreeNode[] cMtns = mtns[n].getChildren();
				if (cMtns != null) { // can happen if optional disjunctive group matches nothing, for instance
					int cCutoffIndex = this.addAttributes(doc, taxonName, taxonNameCutoffIndex, cMtns);
					if (cutoffIndex == -1)
						cutoffIndex = cCutoffIndex;
				}
			}
		}
		return ((cutoffIndex == -1) ? taxonNameCutoffIndex : cutoffIndex);
	}
	
	static Annotation findEpithet(MatchTree mt, String rank) {
		return findEpithet(mt.getChildren(), rank);
	}
	private static Annotation findEpithet(MatchTreeNode[] mtns, String rank) {
		for (int n = 0; n < mtns.length; n++) {
			if (mtns[n].getPattern().startsWith("<")) {
				Annotation matchAnnot = mtns[n].getMatch();
				String attributeName = mtns[n].getPattern().replaceAll("[\\<\\>\\?]", "");
				if (attributeName.indexOf(' ') != -1)
					attributeName = attributeName.substring(0, attributeName.indexOf(' '));
				if (attributeName.startsWith(POTENTIAL_EPITHET_PREFIX))
					attributeName = attributeName.substring(POTENTIAL_EPITHET_PREFIX.length());
				if (attributeName.startsWith(ABBREVIATED_EPITHET_PREFIX))
					attributeName = attributeName.substring(ABBREVIATED_EPITHET_PREFIX.length());
				if (rank.equals(attributeName))
					return matchAnnot;
			}
			else {
				MatchTreeNode[] cMtns = mtns[n].getChildren();
				if (cMtns != null) { // can happen if optional disjunctive group matches nothing, for instance
					Annotation epithetAnnot = findEpithet(cMtns, rank);
					if (epithetAnnot != null)
						return epithetAnnot;
				}
			}
		}
		return null;
	}
	
	private static HashMap instancesByPath = new HashMap();
	public static TreeFAT getInstance(AnalyzerDataProvider adp) {
		TreeFAT instance = ((TreeFAT) instancesByPath.get(adp.getAbsolutePath()));
		if (instance == null) {
			instance = new TreeFAT(adp);
			instancesByPath.put(adp.getAbsolutePath(), instance);
		}
		return instance;
	}
//	
//	public static void main(String[] args) throws Exception {
//		TreeFAT tfat = new TreeFAT();
//		tfat.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/TreeFATData/")));
//		
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03911p493.pdf.gs.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Kucera2013.pdf.names.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Kucera2013.pdf.names.norm.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00872.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03131p051.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/11468_Logunov_2010_Bul_15_85-90.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt00872.pdf.imf.nonCatalog.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Blepharidatta_revision.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/29867.pdf.names.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/zt03916p083.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt03937p049.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04072p343.pfd.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/15611-5134-1-PB.pdf.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt03131p034.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04093p451.pdf.imf.refs.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt02344p016.pdf.refs.imf.xml")), "utf-8"));
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zootaxa.4098.3.4.pdf.imf.xml")), "utf-8"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt04103.3.2.pdf.imf.xml")), "utf-8"));
//		
//		AnnotationFilter.removeDuplicates(doc, EMPHASIS_TYPE);
//		AnnotationFilter.removeAnnotations(doc, TAXONOMIC_NAME_ANNOTATION_TYPE);
//		AnnotationFilter.removeAnnotations(doc, TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
//		DocumentStyle docStyle = DocumentStyle.getStyleFor(doc);
//		docStyle.getParameters().setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + ".binomialsAreBold"), "false");
//		docStyle.getParameters().setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + ".binomialsAreItalics"), "true");
//		tfat.process(doc, new Properties());
//	}
//	
//	public static void main(String[] args) throws Exception {
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("E:/Testdaten/PdfExtract/Zootaxa/zt00434.pdf.out.log"))));
//		for (String line; (line = br.readLine()) != null;) {
//			if (line.startsWith("ABBREVIATED SPECIES REGEX FOR"))
//				System.out.println(line);
//		}
//	}
}