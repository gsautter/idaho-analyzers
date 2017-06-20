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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.accessories.RegExEditorPanel;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/** Meta Analyzer applying the other Analyzers according to the FAT algorithm
 */
public class FAT extends FatAnalyzer implements TaxonomicNameConstants {
	
	/** the annotation type for marking phrases that might be a taxon name */
	public static final String TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE = "tnCandidate";
	
	/** the annotation atttribute for providing information regarding based on which evidence a given phrase was classified to be a taxon name (to help debugging rules and lexicons) */
	public static final String EVIDENCE_ATTRIBUTE = "_evidence";
	
	
	private static final String REGEX_NAME_EXTENSION = ".regEx.txt";
	
	private static final String LEXICON_NAME_EXTENSION = ".list.txt";
	private static final String REFERENCE_LEXICON_NAME_EXTENSION = ".reference.list.txt";
	
	
	private static final String PRECISION_REGEX_NAME_LIST_NAME = "UsePrecision.cnfg";
	private static final String RECALL_REGEX_NAME_LIST_NAME = "UseRecall.cnfg";
	
	private static final String REGEX_MAX_TOKENSLIST_NAME = "regExMaxTokens.cnfg";
	private static final int DEFAULT_REGEX_MAX_TOKENS = 22;
	
	private static final String NEGATIVES_DICTIONARY_LIST_NAME = "negativesDictionaryList.cnfg";
	
	/* list naming policy:
	 * - first letter in upper case ==> reference list, changed only via configuration
	 * - first letter in lower case ==> dynamic list, changed through learning
	 */ 
	
	private static final String NOISE_WORDS_LIST_NAME = "NoiseWord" + LEXICON_NAME_EXTENSION;
	
	private static final String SCIENTISTS_NAMES_LIST_NAME = "scientistsName" + LEXICON_NAME_EXTENSION;
	private static final String SCIENTISTS_NAMES_REFERENCE_LIST_NAME = "ScientistsName" + REFERENCE_LEXICON_NAME_EXTENSION;
	
	private static final String SCIENTISTS_NAME_NOISE_WORDS_LIST_NAME = "ScientistsNameNoiseWord" + LEXICON_NAME_EXTENSION;
	
	private static final String NEGATIVES_LIST_NAME = "negatives" + LEXICON_NAME_EXTENSION;
	
	private static final String FORBIDDEN_WORD_LC_LIST_NAME = "ForbiddenWordsLowerCase" + LEXICON_NAME_EXTENSION;
	private static final String FORBIDDEN_WORDS_UC_LIST_NAME = "ForbiddenWordsUpperCase" + LEXICON_NAME_EXTENSION;
	
	private static final String LOWER_CASE_PARTS_LIST_NAME = "lowerCasePart" + LEXICON_NAME_EXTENSION;
	private static final String LOWER_CASE_PARTS_REFERENCE_LIST_NAME = "LowerCasePart" + REFERENCE_LEXICON_NAME_EXTENSION;
	
	private static final String UPPER_CASE_LOWER_CASE_PARTS_LIST_NAME = "upperCaseLowerCasePart" + LEXICON_NAME_EXTENSION;
	private static final String UPPER_CASE_LOWER_CASE_PARTS_REFERENCE_LIST_NAME = "UpperCaseLowerCasePart" + REFERENCE_LEXICON_NAME_EXTENSION;
	
	
	private static StringVector nativeListNames = new StringVector();
	
	//	general noise words
	static StringVector noiseWords = StringUtils.getNoiseWords();
	
	//	lower case infixes of proper names
	static StringVector nameNoiseWords = new StringVector();
	
	//	common English dictionary, also used for stemming lookup
	private static StringVector negativesDictionaryList = new StringVector();
	static Dictionary negativesDictionary = new CompiledByteDictionary();
	
	//	words learned to be negatives
	static StringVector negatives = new StringVector();
	
	//	lower case words that must not be part of taxonomic names, but appear in company of them
	static StringVector forbiddenWordsLowerCase = new StringVector();
	
	//	upper case words that must not be part of taxonomic names, but can be part of proper names
	static StringVector forbiddenWordsUpperCase = new StringVector();
	
	
	//	parts of taxon names by their ranks
	static StringVector genera = new StringVector();
	static StringVector subGenera = new StringVector();
	static StringVector species = new StringVector();
	static StringVector subSpecies = new StringVector();
	static StringVector varieties = new StringVector();
	
	//	lower case parts in general, regardless if species, subspecies, or variety, since the rank of these parts may change over time
	static StringVector lowerCaseParts = new StringVector();
	
	//	parts of taxonomic names that should be in lower case, but are in upper case for historic reasons
	static StringVector upperCaseLowerCaseParts = new StringVector();
	
	//	list of last names of known authors of taxonomic names
	static StringVector scientistsNames = new StringVector();
	
	
	//	reference lists of parts of taxon names by their ranks
	static StringVector knownGenera = new StringVector();
	static StringVector knownSubGenera = new StringVector();
	static StringVector knownSpecies = new StringVector();
	static StringVector knownSubSpecies = new StringVector();
	static StringVector knownVarieties = new StringVector();
	
	//	reference lists of lower case parts in general, regardless if species, subspecies, or variety, since the rank of these parts may change over time
	static StringVector knownLowerCaseParts = new StringVector();
	
	//	reference lists of parts of taxonomic names that should be in lower case, but are in upper case for historic reasons
	static StringVector knownUpperCaseLowerCaseParts = new StringVector();
	
	//	reference lists of list of last names of known authors of taxonomic names
	static StringVector knownScientistsNames = new StringVector();
	
	
	//	list of canonized taxonomic names, i.e. only the meaningful parts, missing ones filled in with a #
	static StringVector normalizedTaxonNames = new StringVector();
	
	//	labels
	static StringVector subGenusLabels = new StringVector();
	static StringVector speciesLabels = new StringVector();
	static StringVector subSpeciesLabels = new StringVector();
	static StringVector varietyLabels = new StringVector();
	
	//	labels for new ...
	static StringVector newGenusLabels = new StringVector();
	static StringVector newSubGenusLabels = new StringVector();
	static StringVector newSpeciesLabels = new StringVector();
	static StringVector newSubSpeciesLabels = new StringVector();
	static StringVector newVarietyLabels = new StringVector();
	static StringVector newOtherLabels = new StringVector();
	
	//	regular expressions
	private static StringVector recallRegExNames = new StringVector();
	private static StringVector precisionRegExNames = new StringVector();
	private static Properties maxTokensByRegExName = new Properties();
	
	/**	do stemming lookup for a word
	 * @param	word	the word to stem and look up
	 * @return true if and only if the stem of the word is contained in the dictionary
	 */
	static boolean doStemmingLookup(String lookup) {
		if (lookup == null) return false;
		int length = lookup.length();
		
		if (length < 2) return false;
//		if (lookup.endsWith("s") && negativesDictionary.contains(lookup.substring(0, (length - 1)))) return true;
		if (lookup.endsWith("s") && negativesDictionary.lookup(lookup.substring(0, (length - 1)))) return true;
		
		if (length < 3) return false;
//		if (lookup.endsWith("ed") && (negativesDictionary.contains(lookup.substring(0, (length - 1))) || negativesDictionary.contains(lookup.substring(0, (length - 2))))) return true;
		if (lookup.endsWith("ed") && (negativesDictionary.lookup(lookup.substring(0, (length - 1))) || negativesDictionary.lookup(lookup.substring(0, (length - 2))))) return true;
		
		if (length < 4) return false;
//		if ((lookup.endsWith("ies") || lookup.endsWith("ied")) && negativesDictionary.contains(lookup.substring(0, (length - 3)) + "y")) return true;
		if ((lookup.endsWith("ies") || lookup.endsWith("ied")) && negativesDictionary.lookup(lookup.substring(0, (length - 3)) + "y")) return true;
//		if (lookup.endsWith("ing") && (negativesDictionary.contains(lookup.substring(0, (length - 3))) || negativesDictionary.contains(lookup.substring(0, (length - 3)) + "e"))) return true;
		if (lookup.endsWith("ing") && (negativesDictionary.lookup(lookup.substring(0, (length - 3))) || negativesDictionary.lookup(lookup.substring(0, (length - 3)) + "e"))) return true;
		
		return false;
	}
	
	/**	store a taxonomic name in the main registers
	 * @param	tn	the TaxonomicName to store
	 */
	static void addTaxonName(TaxonomicName tn) {
		if (tn != null) {
			normalizedTaxonNames.addElementIgnoreDuplicates(tn.toString());
			
			if (tn.genus != null)
				if (tn.genus.length() > 2)
					genera.addElementIgnoreDuplicates(tn.genus);
				
			if (tn.subGenus != null) 
				if (tn.subGenus.length() > 2)
					subGenera.addElementIgnoreDuplicates(tn.subGenus);
			
			if (tn.species != null)
				if (tn.species.length() > 2) {
					species.addElementIgnoreDuplicates(tn.species);
					if (Gamta.isLowerCaseWord(tn.species))
						lowerCaseParts.addElementIgnoreDuplicates(tn.species);
				}
			
			if (tn.speciesAuthor != null) 
				if (tn.speciesAuthor.length() > 2)
					scientistsNames.addElementIgnoreDuplicates(tn.speciesAuthor);
			
			if (tn.subSpecies != null)
				if (tn.subSpecies.length() > 2) {
					subSpecies.addElementIgnoreDuplicates(tn.subSpecies);
					if (Gamta.isLowerCaseWord(tn.subSpecies))
						lowerCaseParts.addElementIgnoreDuplicates(tn.subSpecies);
				}
			
			if (tn.subSpeciesAuthor != null)
				if (tn.subSpeciesAuthor.length() > 2)
					scientistsNames.addElementIgnoreDuplicates(tn.subSpeciesAuthor);
			
			if (tn.variety != null)
				if (tn.variety.length() > 2) {
					varieties.addElementIgnoreDuplicates(tn.variety);
					if (Gamta.isLowerCaseWord(tn.variety))
						lowerCaseParts.addElementIgnoreDuplicates(tn.variety);
				}
			
			if (tn.varietyAuthor != null)
				if (tn.varietyAuthor.length() > 2)
					scientistsNames.addElementIgnoreDuplicates(tn.varietyAuthor);
		}
	}
	
	private static AnalyzerDataProvider DATA_PROVIDER = null;
	
	/**	initialize FAT main data registers
	 * @param	dataProvider	the folder to load the data from
	 */
	static synchronized void initFAT(AnalyzerDataProvider dataProvider) {
		if (DATA_PROVIDER == null) try {
			
			DATA_PROVIDER = dataProvider;
			InputStream is;
			
			System.out.println("FAT: Initializing ...");
			
			//	load basic data
			is = DATA_PROVIDER.getInputStream(NOISE_WORDS_LIST_NAME);
			noiseWords = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
			is.close();
			noiseWords.removeAll("");
			int ni = 0;
			while (ni < noiseWords.size()) {
				if (noiseWords.get(ni).length() < 2) noiseWords.removeElementAt(ni);
				else ni++;
			}
			nativeListNames.addElementIgnoreDuplicates(NOISE_WORDS_LIST_NAME);
			
			is = DATA_PROVIDER.getInputStream(SCIENTISTS_NAME_NOISE_WORDS_LIST_NAME);
			nameNoiseWords = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
			is.close();
			nameNoiseWords.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SCIENTISTS_NAME_NOISE_WORDS_LIST_NAME);
			
			CompiledByteDictionary negativesDictionary = new CompiledByteDictionary();
			is = DATA_PROVIDER.getInputStream(NEGATIVES_DICTIONARY_LIST_NAME);
			negativesDictionaryList = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
			System.out.println("FAT: Got Negative Dictionary List");
			is.close();
			for (int n = 0; n < negativesDictionaryList.size(); n++) {
				String negativesDictionaryName = negativesDictionaryList.get(n);
				if (!negativesDictionaryName.startsWith("//")) try {
					
					if (negativesDictionaryName.startsWith("http://"))
						is = DATA_PROVIDER.getURL(negativesDictionaryName).openStream();
					else is = DATA_PROVIDER.getInputStream(negativesDictionaryName);
					
					BufferedReader negativesDictionaryReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					String negativesDictionaryEntry;
					while ((negativesDictionaryEntry = negativesDictionaryReader.readLine()) != null) {
						if (negativesDictionaryEntry.length() != 0)
							negativesDictionary.add(negativesDictionaryEntry);
					}
					
					is.close();
					System.out.println(" ==> added Negative Dictionary '" + negativesDictionaryName + "'");
				}
				catch (IOException ioe) {
					System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading negatives dictionary '" + negativesDictionaryName + "'.");
				}
			}
			negativesDictionary.compile();
			FAT.negativesDictionary = negativesDictionary;
			
			is = DATA_PROVIDER.getInputStream(FORBIDDEN_WORD_LC_LIST_NAME);
			forbiddenWordsLowerCase = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
			is.close();
			forbiddenWordsLowerCase.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(FORBIDDEN_WORD_LC_LIST_NAME);
			
			is = DATA_PROVIDER.getInputStream(FORBIDDEN_WORDS_UC_LIST_NAME);
			forbiddenWordsUpperCase = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
			is.close();
			forbiddenWordsUpperCase.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(FORBIDDEN_WORDS_UC_LIST_NAME);
			
			try {
				is = DATA_PROVIDER.getInputStream(NEGATIVES_LIST_NAME);
				negatives = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			negatives.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(NEGATIVES_LIST_NAME);
			
			System.out.println("FAT: Got Negative Lists");
			
			try {
				//	get 'new' label parts
				is = DATA_PROVIDER.getInputStream("newLabel" + LEXICON_NAME_EXTENSION);
				StringVector newParts = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				newParts.removeAll("");
				nativeListNames.addElementIgnoreDuplicates("newLabel" + LEXICON_NAME_EXTENSION);
				
				//	load position specific data
				StringVector load = new StringVector();
				
				//	get genus labels
				try {
					is = DATA_PROVIDER.getInputStream(GENUS_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					load.removeAll("");
					nativeListNames.addElementIgnoreDuplicates(GENUS_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					for (int n = 0; n < newParts.size(); n++) {
						for (int l = 0; l < load.size(); l++) {
							newGenusLabels.addElementIgnoreDuplicates(newParts.get(n) + " " + load.get(l));
							newGenusLabels.addElementIgnoreDuplicates(load.get(l) + " " + newParts.get(n));
						}
					}
					load.clear();
				} catch (IOException fnfe) {
					load.clear();
				}
				
				//	get sub genus labels
				try {
					is = DATA_PROVIDER.getInputStream(SUBGENUS_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					subGenusLabels = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					subGenusLabels.removeAll("");
					nativeListNames.addElementIgnoreDuplicates(SUBGENUS_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					
					for (int n = 0; n < newParts.size(); n++) {
						for (int l = 0; l < subGenusLabels.size(); l++) {
							newSubGenusLabels.addElementIgnoreDuplicates(newParts.get(n) + " " + subGenusLabels.get(l));
							newSubGenusLabels.addElementIgnoreDuplicates(subGenusLabels.get(l) + " " + newParts.get(n));
						}
					}
				} catch (IOException fnfe) {}
				
				//	get species labels
				try {
					is = DATA_PROVIDER.getInputStream(SPECIES_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					speciesLabels = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					speciesLabels.removeAll("");
					nativeListNames.addElementIgnoreDuplicates(SPECIES_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					
					for (int n = 0; n < newParts.size(); n++) {
						for (int l = 0; l < speciesLabels.size(); l++) {
							newSpeciesLabels.addElementIgnoreDuplicates(newParts.get(n) + " " + speciesLabels.get(l));
							newSpeciesLabels.addElementIgnoreDuplicates(speciesLabels.get(l) + " " + newParts.get(n));
						}
					}
				} catch (IOException fnfe) {}
				
				//	get sub species labels
				try {
					is = DATA_PROVIDER.getInputStream(SUBSPECIES_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					subSpeciesLabels = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					subSpeciesLabels.removeAll("");
					nativeListNames.addElementIgnoreDuplicates(SUBSPECIES_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					
					for (int n = 0; n < newParts.size(); n++) {
						for (int l = 0; l < subSpeciesLabels.size(); l++) {
							newSubSpeciesLabels.addElementIgnoreDuplicates(newParts.get(n) + " " + subSpeciesLabels.get(l));
							newSubSpeciesLabels.addElementIgnoreDuplicates(subSpeciesLabels.get(l) + " " + newParts.get(n));
						}
					}
				} catch (IOException fnfe) {}
				
				//	get variety labels
				try {
					is = DATA_PROVIDER.getInputStream(VARIETY_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					varietyLabels = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					varietyLabels.removeAll("");
					nativeListNames.addElementIgnoreDuplicates(VARIETY_ATTRIBUTE + "Label" + LEXICON_NAME_EXTENSION);
					
					for (int n = 0; n < newParts.size(); n++) {
						for (int l = 0; l < varietyLabels.size(); l++) {
							newVarietyLabels.addElementIgnoreDuplicates(newParts.get(n) + " " + varietyLabels.get(l));
							newVarietyLabels.addElementIgnoreDuplicates(varietyLabels.get(l) + " " + newParts.get(n));
						}
					}
				} catch (IOException fnfe) {}
				
				//	get other labels
				try {
					is = DATA_PROVIDER.getInputStream("otherLabel" + LEXICON_NAME_EXTENSION);
					load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					load.removeAll("");
					nativeListNames.addElementIgnoreDuplicates("otherLabel" + LEXICON_NAME_EXTENSION);
					
					for (int n = 0; n < newParts.size(); n++) {
						for (int l = 0; l < load.size(); l++) {
							newOtherLabels.addElementIgnoreDuplicates(newParts.get(n) + " " + load.get(l));
							newOtherLabels.addElementIgnoreDuplicates(load.get(l) + " " + newParts.get(n));
						}
					}
					load.clear();
				} catch (IOException fnfe) {
					load.clear();
				}
			} catch (IOException fnfe) {}
			
			System.out.println("FAT: Got Label Lists");
			
			try {
				is = DATA_PROVIDER.getInputStream(RECALL_REGEX_NAME_LIST_NAME);
				recallRegExNames = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			recallRegExNames.removeAll("");
			
			try {
				is = DATA_PROVIDER.getInputStream(PRECISION_REGEX_NAME_LIST_NAME);
				precisionRegExNames = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			precisionRegExNames.removeAll("");
			
			try {
				is = DATA_PROVIDER.getInputStream(REGEX_MAX_TOKENSLIST_NAME);
				StringVector regExMaxTokensList = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				for (int i = 0; i < regExMaxTokensList.size(); i++) {
					String line = regExMaxTokensList.get(i);
					int split = line.indexOf(' ');
					if (split != -1) try {
						int maxTokens = Integer.parseInt(line.substring(0, split));
						String regExName = line.substring(split).trim();
						maxTokensByRegExName.setProperty(regExName, ("" + maxTokens));
					} catch (NumberFormatException nfe) {}
				}
			} catch (IOException fnfe) {}
			
			System.out.println("FAT: Got Regular Expression Assignment Lists");
			
			try {
				is = DATA_PROVIDER.getInputStream(SCIENTISTS_NAMES_LIST_NAME);
				scientistsNames = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			scientistsNames.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SCIENTISTS_NAMES_LIST_NAME);
			
			try {
				is = DATA_PROVIDER.getInputStream(GENUS_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				genera = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			genera.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(GENUS_ATTRIBUTE + LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(SUBGENUS_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				subGenera = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			subGenera.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SUBGENUS_ATTRIBUTE + LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(SPECIES_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				species = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			species.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SPECIES_ATTRIBUTE + LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(SUBSPECIES_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				subSpecies = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			subSpecies.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SUBSPECIES_ATTRIBUTE + LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(VARIETY_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				varieties = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			varieties.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(VARIETY_ATTRIBUTE + LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(LOWER_CASE_PARTS_LIST_NAME);
				lowerCaseParts = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			lowerCaseParts.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(LOWER_CASE_PARTS_LIST_NAME);
			
			try {
				is = DATA_PROVIDER.getInputStream(UPPER_CASE_LOWER_CASE_PARTS_LIST_NAME);
				upperCaseLowerCaseParts = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			upperCaseLowerCaseParts.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(UPPER_CASE_LOWER_CASE_PARTS_LIST_NAME);
			
			System.out.println("FAT: Got Learned Lists");
			
			try {
				is = DATA_PROVIDER.getInputStream(SCIENTISTS_NAMES_REFERENCE_LIST_NAME);
				knownScientistsNames = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownScientistsNames.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SCIENTISTS_NAMES_REFERENCE_LIST_NAME);
			
			try {
				is = DATA_PROVIDER.getInputStream(GENUS_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
				knownGenera = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownGenera.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(GENUS_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(SUBGENUS_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
				knownSubGenera = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownSubGenera.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SUBGENUS_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(SPECIES_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
				knownSpecies = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownSpecies.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SPECIES_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(SUBSPECIES_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
				knownSubSpecies = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownSubSpecies.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(SUBSPECIES_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(VARIETY_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
				knownVarieties = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownVarieties.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(VARIETY_ATTRIBUTE + REFERENCE_LEXICON_NAME_EXTENSION);
			
			try {
				is = DATA_PROVIDER.getInputStream(LOWER_CASE_PARTS_REFERENCE_LIST_NAME);
				knownLowerCaseParts = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownLowerCaseParts.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(LOWER_CASE_PARTS_REFERENCE_LIST_NAME);
			
			try {
				is = DATA_PROVIDER.getInputStream(UPPER_CASE_LOWER_CASE_PARTS_REFERENCE_LIST_NAME);
				knownUpperCaseLowerCaseParts = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			knownUpperCaseLowerCaseParts.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(UPPER_CASE_LOWER_CASE_PARTS_REFERENCE_LIST_NAME);
			
			System.out.println("FAT: Got Reference Lists");
			
			try {
				is = DATA_PROVIDER.getInputStream(TAXONOMIC_NAME_ANNOTATION_TYPE + LEXICON_NAME_EXTENSION);
				normalizedTaxonNames = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
			} catch (IOException fnfe) {}
			normalizedTaxonNames.removeAll("");
			nativeListNames.addElementIgnoreDuplicates(TAXONOMIC_NAME_ANNOTATION_TYPE + LEXICON_NAME_EXTENSION);
			
			System.out.println("FAT: Got Positive Lists");
			
			System.out.println("FAT: Initialized");
		} catch (IOException ioe) {
			System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while initializing FAT.");
		}
		
		//	clean loaded lists
		cleanLexicons();
	}
	
	/** store FAT main data registers
	 */
	static void exitFAT() {
		if ((DATA_PROVIDER != null) && DATA_PROVIDER.isDataEditable()) {
			
			AnalyzerDataProvider dataProvider = DATA_PROVIDER;
			DATA_PROVIDER = null;
			
			//	clean lists before storing them
			cleanLexicons();
			
			OutputStream os;
			try {
				recallRegExNames.sortLexicographically();
				os = dataProvider.getOutputStream(RECALL_REGEX_NAME_LIST_NAME);
				recallRegExNames.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			try {
				precisionRegExNames.sortLexicographically();
				os = dataProvider.getOutputStream(PRECISION_REGEX_NAME_LIST_NAME);
				precisionRegExNames.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			try {
				ArrayList regExNames = new ArrayList(maxTokensByRegExName.keySet());
				StringVector regExNameList = new StringVector();
				for (int n = 0; n < regExNames.size(); n++)
					regExNameList.addElement(regExNames.get(n).toString());
				regExNameList.sortLexicographically();
				
				StringVector regExMaxTokensList = new StringVector();
				for (int i = 0; i < regExNameList.size(); i++) {
					String regExName = regExNameList.get(i);
					regExMaxTokensList.addElement(maxTokensByRegExName.getProperty(regExName) + " " + regExName);
				}
				
				os = dataProvider.getOutputStream(REGEX_MAX_TOKENSLIST_NAME);
				regExMaxTokensList.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException fnfe) {}
			
			try {
				negativesDictionaryList.sortLexicographically();
				os = dataProvider.getOutputStream(NEGATIVES_DICTIONARY_LIST_NAME);
				negativesDictionaryList.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			
			try {
				genera.sortLexicographically();
				os = dataProvider.getOutputStream(GENUS_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				genera.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			try {
				subGenera.sortLexicographically();
				os = dataProvider.getOutputStream(SUBGENUS_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				subGenera.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			try {
				species.sortLexicographically();
				os = dataProvider.getOutputStream(SPECIES_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				species.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			try {
				subSpecies.sortLexicographically();
				os = dataProvider.getOutputStream(SUBSPECIES_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				subSpecies.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			try {
				varieties.sortLexicographically();
				os = dataProvider.getOutputStream(VARIETY_ATTRIBUTE + LEXICON_NAME_EXTENSION);
				varieties.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			
			try {
				upperCaseLowerCaseParts.sortLexicographically();
				os = dataProvider.getOutputStream(UPPER_CASE_LOWER_CASE_PARTS_LIST_NAME);
				upperCaseLowerCaseParts.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			
			try {
				normalizedTaxonNames.sortLexicographically();
				os = dataProvider.getOutputStream(TAXONOMIC_NAME_ANNOTATION_TYPE + LEXICON_NAME_EXTENSION);
				normalizedTaxonNames.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
		
			try {
				scientistsNames.sortLexicographically();
				os = dataProvider.getOutputStream(SCIENTISTS_NAMES_LIST_NAME);
				scientistsNames.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
			
			try {
				negatives.sortLexicographically();
				os = dataProvider.getOutputStream(NEGATIVES_LIST_NAME);
				negatives.storeContent(new OutputStreamWriter(os, "UTF-8"));
				os.flush();
				os.close();
			} catch (IOException ioe) {}
		}
	}
	
	private static void cleanLexicons() {
		
		//	clean up known genera
		for (int g = 0; g < knownGenera.size(); g++) {
			String genus = knownGenera.get(g);
			scientistsNames.removeAll(genus);
			negatives.removeAll(genus);
		}
		
		//	clean up known sub genera
		for (int s = 0; s < knownSubGenera.size(); s++) {
			String subGenus = knownSubGenera.get(s);
			scientistsNames.removeAll(subGenus);
			negatives.removeAll(subGenus);
		}
		
		//	clean up known upper case lower case parts
		for (int p = 0; p < knownUpperCaseLowerCaseParts.size(); p++) {
			String uclcPart = knownUpperCaseLowerCaseParts.get(p);
			scientistsNames.removeAll(uclcPart);
			negatives.removeAll(uclcPart);
		}
		
		//	clean up known species
		for (int s = 0; s < knownSpecies.size(); s++)
			negatives.removeAll(knownSpecies.get(s));
		
		//	clean up known sub species
		for (int s = 0; s < knownSubSpecies.size(); s++)
			negatives.removeAll(knownSubSpecies.get(s));
		
		//	clean up known varieties
		for (int v = 0; v < knownVarieties.size(); v++)
			negatives.removeAll(knownVarieties.get(v));
		
		//	clean up known lower case parts
		for (int p = 0; p < knownLowerCaseParts.size(); p++)
			negatives.removeAll(knownLowerCaseParts.get(p));
		
		//	clean up known scientist names
		for (int n = 0; n < knownScientistsNames.size(); n++) {
			String scientistsName = knownScientistsNames.get(n);
			genera.removeAll(scientistsName);
			subGenera.removeAll(scientistsName);
			upperCaseLowerCaseParts.removeAll(scientistsName);
		}
		
		//	clean up forbidden upper case parts
		for (int f = 0; f < forbiddenWordsUpperCase.size(); f++) {
			String forbidden = forbiddenWordsUpperCase.get(f);
			genera.removeAll(forbidden);
			subGenera.removeAll(forbidden);
			upperCaseLowerCaseParts.removeAll(forbidden);
		}
		
		//	clean up forbidden lower case parts
		for (int f = 0; f < forbiddenWordsLowerCase.size(); f++) {
			String forbidden = forbiddenWordsLowerCase.get(f);
			species.removeAll(forbidden);
			subSpecies.removeAll(forbidden);
			varieties.removeAll(forbidden);
			lowerCaseParts.removeAll(forbidden);
		}
	}
	
	static synchronized void configureFAT() {
		AnalyzerDataProvider dataProvider = DATA_PROVIDER;
		exitFAT();
		
		Window top = DialogFactory.getTopWindow();
		FatConfigurarionDialog fcd;
		if (top instanceof JDialog)
			fcd = new FatConfigurarionDialog(((JDialog) top), dataProvider);
		else fcd = new FatConfigurarionDialog(((JFrame) top), dataProvider);
		fcd.setVisible(true);
		
		initFAT(dataProvider);
	}
	
	private static Properties RESOLVER = new Properties() {
		public String getProperty(String name, String def) {
			try {
				try {
					InputStream is = DATA_PROVIDER.getInputStream(name + LEXICON_NAME_EXTENSION);
					StringVector load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					String orGroup = RegExUtils.produceOrGroup(load, true);
					return orGroup;
				} catch (IOException fnfe) {}
				InputStream is = DATA_PROVIDER.getInputStream(name + REGEX_NAME_EXTENSION);
				StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				return RegExUtils.normalizeRegEx(regEx.concatStrings(""));
			} catch (IOException ioe) {
				return def;
			}
		}
		public String getProperty(String name) {
			return this.getProperty(name, null);
		}
	};
	
	static String[] getRecallRuleNames() {
		return recallRegExNames.toStringArray();
	}
	
	static String[] getPrecisionRuleNames() {
		return precisionRegExNames.toStringArray();
	}
	
	static String getRegEx(String regExName) {
		try {
			InputStream is = DATA_PROVIDER.getInputStream(regExName);
			StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
			is.close();
			return RegExUtils.preCompile(RegExUtils.normalizeRegEx(regEx.concatStrings("")), RESOLVER);
		} catch (IOException e) {}
		return null;
	}
	
	static int getMaxTokens(String regExName) {
		try {
			String mtString = maxTokensByRegExName.getProperty(regExName);
			if (mtString != null) return Integer.parseInt(mtString);
		} catch (NumberFormatException nfe) {}
		return DEFAULT_REGEX_MAX_TOKENS;
	}
	
	private Analyzer recallRules;
	private Analyzer stemmingLookupFilter;
	private Analyzer lexiconRules;
	private Analyzer labelRules;
	private Analyzer precisionRules;
	private Analyzer knownDataRules;
	private Analyzer nameRules;
	private Analyzer sureNegRules;
	private Analyzer dataRules;
	private Analyzer dynamicLexiconRules;
//	private Analyzer candidateScorer;
	private Analyzer candidateFeedbacker;
	private Analyzer higherOrderRules;
	
	private Analyzer nameAttributor;
	private Analyzer nameCompleter;
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		long start = System.currentTimeMillis();
		long intermediate = start;
		long stop;
		System.out.println("FAT: Start processing document with " + data.size() + " tokens ...");
		
		//	extract taxonomic names
		this.recallRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Recall Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.stemmingLookupFilter.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Stemming Lookup Filter done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.lexiconRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Lexicon Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.labelRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Label Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.precisionRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Precision Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.knownDataRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Known Data Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.nameRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.sureNegRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Sure Negative Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.dataRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Data Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.dynamicLexiconRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Dynamic Lexicon Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		
		//	tag higher order names
		this.higherOrderRules.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Higher Order Rules done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		
		//	cleanup candidates in sure positives
		AnnotationFilter.removeContained(data, TAXONOMIC_NAME_ANNOTATION_TYPE, TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Nested Candidates removed in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		
		//	vote remaining candidates
		this.nameAttributor.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Attributor done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.nameCompleter.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Completer done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		System.out.println("FAT: " + data.getAnnotations(TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE).length + " candidates remaining.");
		
//		this.candidateScorer.process(data, parameters);
//		stop = System.currentTimeMillis();
//		System.out.println("FAT: Candidate Scorer done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
//		intermediate = stop;
		
		this.candidateFeedbacker.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Candidate Feedbacker done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		
		//	cleanup Annotations
		AnnotationFilter.removeInner(data, TAXONOMIC_NAME_ANNOTATION_TYPE);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Nested Annotations removed in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		AnnotationFilter.removeAnnotations(data, TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Remaining candidate Annotations removed in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		
		//	complete Annotations
		this.nameAttributor.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Attributor done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.nameCompleter.process(data, parameters);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Completer done in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
	}
	
	/** @see de.gamta.util.AbstractAnalyzer#setDataPath(java.lang.String)
	 */
	public void initAnalyzer() {
		super.initAnalyzer();
		long start = System.currentTimeMillis();
		long intermediate = start;
		long stop;
		
		this.recallRules = new RecallRules();
		this.recallRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Recall Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.stemmingLookupFilter = new StemmingLookupFilter();
		this.stemmingLookupFilter.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: StemmingLookup Filter initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.lexiconRules = new LexiconRules();
		this.lexiconRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Lexicon Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.labelRules = new LabelRules();
		this.labelRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Label Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.precisionRules = new PrecisionRules();
		this.precisionRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Precision Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.knownDataRules = new KnownDataRules();
		this.knownDataRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Known Data Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.nameRules = new ScientistsNameRule();
		this.nameRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.sureNegRules = new SureNegativesFilter();
		this.sureNegRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Sure Negative Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.dataRules = new DataRules();
		this.dataRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Data Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.dynamicLexiconRules = new DynamicLexiconRules();
		this.dynamicLexiconRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Dynamic Lexicon Rules initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
//		this.candidateScorer = new TnCandidateScorer();
//		this.candidateScorer.setDataProvider(dataProvider);
//		stop = System.currentTimeMillis();
//		System.out.println("FAT: Candidate Scorer initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
//		intermediate = stop;
		
		this.candidateFeedbacker = new TnCandidateFeedbacker();
		this.candidateFeedbacker.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Candidate Feedbacker initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.higherOrderRules = new HigherOrderNameTagger();
		this.higherOrderRules.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Higher Order Tagger initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.nameAttributor = new TaxonomicNameAttributor();
		this.nameAttributor.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Attributor initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		this.nameCompleter = new TaxonomicNameCompleter();
		this.nameCompleter.setDataProvider(dataProvider);
		stop = System.currentTimeMillis();
		System.out.println("FAT: Name Completer initialized in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
	}
	
	/** @see de.gamta.util.AbstractAnalyzer#exit()
	 */
	public void exitAnalyzer() {
		this.recallRules.exit();
		this.stemmingLookupFilter.exit();
		this.lexiconRules.exit();
		this.labelRules.exit();
		this.precisionRules.exit();
		this.knownDataRules.exit();
		this.nameRules.exit();
		this.dataRules.exit();
		this.dynamicLexiconRules.exit();
//		this.candidateScorer.exit();
		this.candidateFeedbacker.exit();
		this.higherOrderRules.exit();
		this.nameAttributor.exit();
		this.nameCompleter.exit();
		
		exitFAT();
	}
	
	private static class FatConfigurarionDialog extends JDialog {
		private FatRegExConfigurarionPanel regExPanel;
		private FatLexiconConfigurarionPanel lexiconPanel;
		
		FatConfigurarionDialog(JFrame owner, AnalyzerDataProvider dataProvider)  {
			super(owner, "Configure FAT Taxonomic Name Finder", true);
			this.init(dataProvider);
		}
		
		FatConfigurarionDialog(JDialog owner, AnalyzerDataProvider dataProvider)  {
			super(owner, "Configure FAT Taxonomic Name Finder", true);
			this.init(dataProvider);
		}
		
		private void init(AnalyzerDataProvider dataProvider) {
			
			//	take control of window
			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			this.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.out.println("FAT config dialog closed.");
					exit();
				}
				public void windowClosed(WindowEvent e) {
					System.out.println("FAT config dialog closed.");
					exit();
				}
			});
			
			this.regExPanel = new FatRegExConfigurarionPanel(this, dataProvider);
			this.lexiconPanel = new FatLexiconConfigurarionPanel(this, dataProvider);
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Regular Expressions", this.regExPanel);
			tabs.addTab("Lexicons", this.lexiconPanel);
			this.getContentPane().add(tabs, BorderLayout.CENTER);
			
			this.setSize(800, 600);
			this.setLocationRelativeTo(null);
		}
		
		private void exit() {
			if (this.isVisible()) {
				this.regExPanel.exit();
				this.lexiconPanel.exit();
				this.setVisible(false);
				this.dispose();	//	necessary to avoid endless loop with window listener
			}
		}
	}
	
	private static class FatRegExConfigurarionPanel extends JPanel {
		
		private FatConfigurarionDialog parent;
		private AnalyzerDataProvider dataProvider;
		
		private String currentRegExName;
		private RegExEditorPanel editor;
		private RegExNameListPanel regExNameList;
		
		private Properties resolver;
		
		FatRegExConfigurarionPanel(FatConfigurarionDialog parent, final AnalyzerDataProvider dataProvider)  {
			super(new BorderLayout(), true);
			this.parent = parent;
			this.dataProvider = dataProvider;
			
			this.resolver = new Properties() {
				public String getProperty(String name, String def) {
					try {
						try {
							InputStream is = dataProvider.getInputStream(name + LEXICON_NAME_EXTENSION);
							StringVector load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
							is.close();
							String orGroup = RegExUtils.produceOrGroup(load, true);
							return orGroup;
						} catch (IOException fnfe) {}
						InputStream is = dataProvider.getInputStream(name + REGEX_NAME_EXTENSION);
						StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
						is.close();
						return RegExUtils.normalizeRegEx(regEx.concatStrings(""));
					} catch (IOException ioe) {
						return def;
					}
				}
				public String getProperty(String name) {
					return this.getProperty(name, null);
				}
			};
			
			JButton[] buttons = new JButton[1];
			buttons[0] = new JButton("Test RegEx");
			buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
			buttons[0].setPreferredSize(new Dimension(115, 21));
			buttons[0].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testRegEx(regExNameList.getSelectedRegExName());
				}
			});
			
			this.editor = new RegExEditorPanel(buttons);
			this.editor.setSubPatternResolver(this.resolver);
			
			this.regExNameList = new RegExNameListPanel();	
			
			JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			JLabel spacer;
			
			button = new JButton("Create");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (createRegEx())
						regExNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Clone");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (cloneRegEx())
						regExNameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Delete");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (deleteRegEx())
						regExNameList.refresh();
				}
			});
			editButtons.add(button);
			
			spacer = new JLabel("");
			spacer.setPreferredSize(new Dimension(21, 21));
			editButtons.add(spacer);
			
			button = new JButton("Test Recall");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testRegExes(true, false);
				}
			});
			editButtons.add(button);
			button = new JButton("Test Precision");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testRegExes(false, true);
				}
			});
			editButtons.add(button);
			button = new JButton("Test All Rules");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testRegExes(true, true);
				}
			});
			editButtons.add(button);
			
//			spacer = new JLabel("");
//			spacer.setPreferredSize(new Dimension(21, 21));
//			editButtons.add(spacer);
//			//	TODO: re-activate this once test is implemented
//			button = new JButton("Test FAT");
//			button.setBorder(BorderFactory.createRaisedBevelBorder());
//			button.setPreferredSize(new Dimension(70, 21));
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					testFat();
//				}
//			});
//			editButtons.add(button);
			
			this.add(this.editor, BorderLayout.CENTER);
			this.add(this.regExNameList, BorderLayout.EAST);
			this.add(editButtons, BorderLayout.NORTH);
		}
		
		private void exit() {
			if ((this.currentRegExName != null) && this.editor.isDirty()) try {
				storeRegEx(this.currentRegExName, this.editor.getContent());
			} catch (IOException ioe) {}
		}
		
		private void regExNameSelected(String newRegExName) {
			if ((currentRegExName != null) && editor.isDirty()) {
				try {
					storeRegEx(currentRegExName, editor.getContent());
				}
				catch (IOException ioe) {
					if (JOptionPane.showConfirmDialog(null, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving regular expression to " + currentRegExName.toString() + "\nProceed?"), "Could Not Save RegEx", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
						regExNameList.setSelectedRegExName(currentRegExName);
						return;
					}
				}
			}
			currentRegExName = newRegExName;
			if (currentRegExName != null) {
				String regEx = loadRegEx(currentRegExName);
				if (regEx == null) regEx = "";
				editor.setContent(regEx);
			}
			validate();
		}
		
		private String loadRegEx(String regExName) {
			try {
				InputStream is = this.dataProvider.getInputStream(regExName);
				StringVector lines = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				return lines.concatStrings("");
			}
			catch (IOException ioe) {
				return null;
			}
		}
		
		private boolean storeRegEx(String regExName, String regEx) throws IOException {
			StringVector lines = new StringVector();
			lines.parseAndAddElements(regEx, "\n");
			OutputStream os = this.dataProvider.getOutputStream(regExName);
			lines.storeContent(new OutputStreamWriter(os, "UTF-8"));
			os.flush();
			os.close();
			return true;
		}
		
		private boolean createRegEx() {
			return this.createRegEx(null, null);
		}
		
		private boolean cloneRegEx() {
			if (this.currentRegExName == null)
				return this.createRegEx();
			else {
				String name = "New " + this.regExNameList.getSelectedRegExName();
				return this.createRegEx(this.editor.getContent(), name);
			}
		}
		
		private boolean createRegEx(String modelRegEx, String name) {
			CreateRegExDialog cred = new CreateRegExDialog(this.parent, modelRegEx, name);
			cred.setVisible(true);
			if (cred.isCommitted()) {
				String regEx = cred.getRegEx();
				String regExName = cred.getRegExName();
				if (!regExName.endsWith(REGEX_NAME_EXTENSION)) regExName += REGEX_NAME_EXTENSION;
				try {
					this.storeRegEx(regExName, regEx);
					return true;
				} catch (IOException ioe) {}
			}
			return false;
		}
		
		private boolean deleteRegEx() {
			String name = this.regExNameList.getSelectedRegExName();
			if ((name != null) && (JOptionPane.showConfirmDialog(this, ("Really delete " + name), "Confirm Delete RegEx", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
				try {
					if (this.dataProvider.deleteData(name)) {
						this.regExNameList.refresh();
						return true;
					}
					else {
						JOptionPane.showMessageDialog(this, ("Could not delete " + name), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
						return false;
					}
				}
				catch (Exception ioe) {
					JOptionPane.showMessageDialog(this, ("Could not delete " + name), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
					return false;
				}
			}
			else return false;
		}
		
		private void testRegEx(String regExName) {
			if (regExName == null) return;
			
			String rawRegEx = this.editor.getContent();
			String validationError = RegExUtils.validateRegEx(rawRegEx, this.resolver);
			if (validationError == null) {
				QueriableAnnotation data = Gamta.getTestDocument();
				if (data != null) {
					String regEx = RegExUtils.preCompile(rawRegEx, this.resolver);
					Annotation[] annotations = Gamta.extractAllMatches(data, regEx, 20);
					AnnotationDisplayDialog add = new AnnotationDisplayDialog(this.parent, ("Matches of RegEx '" + regExName + "'"), annotations, true);
					add.setLocationRelativeTo(this);
					add.setVisible(true);
				}
			}
			else JOptionPane.showMessageDialog(this, "The pattern is not valid:\n" + validationError, "RegEx Validation", JOptionPane.ERROR_MESSAGE);
		}
		
		private void testRegExes(final boolean doRecall, final boolean doPrecision) {
			String rawRegEx = this.editor.getContent();
			String validationError = RegExUtils.validateRegEx(rawRegEx, this.resolver);
			if (validationError == null) {
				QueriableAnnotation data = Gamta.getTestDocument();
				if (data != null) {
					final TreeMap matchingRegExes = new TreeMap();
					final TreeSet recallMatches = new TreeSet();
					final TreeSet precisionMatches = new TreeSet();
					
					String selectedRegExName = this.regExNameList.getSelectedRegExName();
					for (int r = 0; r < this.regExNameList.getRegExNameCount(); r++) {
						String regExName = this.regExNameList.regExNameAt(r);
						if ((doPrecision && precisionRegExNames.contains(regExName)) || (doRecall && recallRegExNames.contains(regExName))) {
							String regEx = RegExUtils.preCompile((regExName.equals(selectedRegExName) ? rawRegEx : loadRegEx(regExName)), this.resolver);
							Annotation[] annotations = Gamta.extractAllMatches(data, regEx, true, false, true);
							for (int a = 0; a < annotations.length; a++) {
								String value = annotations[a].getValue();
								Set matchingRegExSet = ((Set) matchingRegExes.get(value));
								if (matchingRegExSet == null) {
									matchingRegExSet = new TreeSet();
									matchingRegExes.put(value, matchingRegExSet);
								}
								matchingRegExSet.add(regExName);
								if (recallRegExNames.contains(regExName))
									recallMatches.add(value);
								if (precisionRegExNames.contains(regExName))
									precisionMatches.add(value);
							}
						}
					}
					
					final ArrayList matchList = new ArrayList(matchingRegExes.keySet());
					Collections.sort(matchList, String.CASE_INSENSITIVE_ORDER);
					
					final JDialog matchDialog = DialogFactory.produceDialog(("Matches of " + (doRecall ? (doPrecision ? "Recall & Precision" : "Recall") : "Precision") + " Expressions"), true);
					
					
					final JTable matchTable = new JTable(new TableModel() {
						public void addTableModelListener(TableModelListener l) {}
						public void removeTableModelListener(TableModelListener l) {}
						
						public int getColumnCount() {
							return ((doRecall && doPrecision) ? 3 : 1);
						}
						public Class getColumnClass(int columnIndex) {
							return ((columnIndex == 0) ? String.class : Boolean.class);
						}
						public String getColumnName(int columnIndex) {
							if (columnIndex == 0) return "Matching String";
							else if (columnIndex == 1) return "Recall";
							else if (columnIndex == 2) return "Precision";
							else return null;
						}
						
						public int getRowCount() {
							return matchList.size();
						}
						public Object getValueAt(int rowIndex, int columnIndex) {
							String value = ((String) matchList.get(rowIndex));
							if (columnIndex == 0) return value;
							else if (columnIndex == 1) return new Boolean(recallMatches.contains(value));
							else if (columnIndex == 2) return new Boolean(precisionMatches.contains(value));
							else return null;
						}
						
						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return false;
						}
						public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
					});
					if (doRecall && doPrecision) {
						matchTable.getColumnModel().getColumn(1).setMaxWidth(60);
						matchTable.getColumnModel().getColumn(2).setMaxWidth(60);
					}
					matchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					matchTable.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent me) {
							if (me.getClickCount() > 1) {
								int row = matchTable.getSelectedRow();
								if (row == -1) return;
								
								String value = ((String) matchList.get(row));
								Set matchingRegExSet = ((Set) matchingRegExes.get(value));
								
								StringBuffer message = new StringBuffer("'" + value + "' is matched by the following regular expressions:");
								for (Iterator rit = matchingRegExSet.iterator(); rit.hasNext();)
									message.append("\n- " + rit.next());
								JOptionPane.showMessageDialog(matchDialog, message.toString(), ("Match Details for '" + value + "'"), JOptionPane.PLAIN_MESSAGE);
							}
						}
					});
					
					JScrollPane matchTableBox = new JScrollPane(matchTable);
					matchTableBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					matchTableBox.getVerticalScrollBar().setUnitIncrement(25);
					
					JLabel explanation = new JLabel("<HTML><B>Double-click the individual rows to see which<BR>regular expression matched the row's value.</B></HTML>", JLabel.CENTER);
					explanation.setOpaque(true);
					explanation.setBackground(Color.WHITE);
					explanation.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 3), BorderFactory.createLineBorder(Color.RED, 1)));
					
					JButton closeButton = new JButton("Close");
					closeButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							matchDialog.dispose();
						}
					});
					
					matchDialog.getContentPane().setLayout(new BorderLayout());
					matchDialog.getContentPane().add(explanation, BorderLayout.NORTH);
					matchDialog.getContentPane().add(matchTableBox, BorderLayout.CENTER);
					matchDialog.getContentPane().add(closeButton, BorderLayout.SOUTH);
					
					matchDialog.setSize(400, 600);
					matchDialog.setLocationRelativeTo(this);
					matchDialog.setVisible(true);
				}
			}
			else JOptionPane.showMessageDialog(this, "The current pattern is not valid:\n" + validationError, "RegEx Validation", JOptionPane.ERROR_MESSAGE);
		}
		
//		private void testFat() {
//			//	TODO: implement this
//		}
		
		private class RegExNameListPanel extends JPanel {
			
			private StringVector regExNames;
			
			private JTable regExNameList; 
			private JScrollPane regExNameListBox;
			
			RegExNameListPanel() {
				super(new BorderLayout(), true);
				
				//	 initialize regExName list
				this.regExNameList = new JTable();
				this.regExNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.regExNameList.addMouseListener(new MouseAdapter() {
//					int selectedRow = -1;
					public void mouseClicked(MouseEvent me) {
						int row = regExNameList.rowAtPoint(me.getPoint());
//						if (row != selectedRow) {
//							selectedRow = row;
//							regExNameSelected(getSelectedRegExName());
//						}
						if (row != -1)
							regExNameSelected(getSelectedRegExName());
					}
				});
				this.regExNameListBox = new JScrollPane(this.regExNameList);
				
				this.add(this.regExNameListBox, BorderLayout.CENTER);
				
				this.refresh();
			}
			
			void setSelectedRegExName(String regExName) {
				int index = this.regExNames.indexOf(regExName);
				this.regExNameList.setRowSelectionInterval(index, index);
			}
			
			String getSelectedRegExName() {
				int index = this.regExNameList.getSelectedRow();
				return ((index == -1) ? null : this.regExNames.get(index));
			}
			
			void refresh() {
				this.regExNames = this.readRegExNameList();
				this.regExNameList.setModel(new RegExNameListModel());
				this.regExNameList.getColumnModel().getColumn(0).setMaxWidth(80);
				this.regExNameList.getColumnModel().getColumn(1).setMaxWidth(60);
				this.regExNameList.getColumnModel().getColumn(2).setMaxWidth(60);
				this.regExNameList.validate();
			}
			
			String regExNameAt(int index) {
				return ((this.regExNames == null) ? "" : this.regExNames.get(index));
			}
			
			int getRegExNameCount() {
				return ((this.regExNames == null) ? 0 : this.regExNames.size());
			}
			
			StringVector readRegExNameList() {
				StringVector regExNames = new StringVector();
				String[] names = dataProvider.getDataNames();
				for (int n = 0; n < names.length; n++) {
					if (names[n].endsWith(REGEX_NAME_EXTENSION))
						regExNames.addElementIgnoreDuplicates(names[n]);
				}
				regExNames.sortLexicographically(false, false);
				return regExNames;
			}
			
			private class RegExNameListModel implements TableModel {
				public int getColumnCount() {
					return 4;
				}
				public int getRowCount() {
					return getRegExNameCount();
				}
				public Class getColumnClass(int columnIndex) {
					if (columnIndex == 0) return Integer.class;
					else if (columnIndex == 3) return String.class;
					else return Boolean.class;
				}
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "MaxTokens";
					if (columnIndex == 1) return "Recall";
					if (columnIndex == 2) return "Precision";
					if (columnIndex == 3) return "Name";
					return null;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					String name = regExNameAt(rowIndex);
					if (columnIndex == 0) return new Integer(FAT.getMaxTokens(name));
					if (columnIndex == 1) return new Boolean(FAT.recallRegExNames.contains(name));
					if (columnIndex == 2) return new Boolean(FAT.precisionRegExNames.contains(name));
					if (columnIndex == 3) return name;
					return null;
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return (columnIndex < 3);
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if (columnIndex == 0) {
						int maxTokens = ((Integer) newValue).intValue();
						maxTokensByRegExName.setProperty(regExNameAt(rowIndex), ("" + maxTokens));
					} else if (((Boolean) newValue).booleanValue()) {
						if (columnIndex == 1) FAT.recallRegExNames.addElementIgnoreDuplicates(regExNameAt(rowIndex));
						else if (columnIndex == 2) FAT.precisionRegExNames.addElementIgnoreDuplicates(regExNameAt(rowIndex));
					} else {
						if (columnIndex == 1) FAT.recallRegExNames.removeAll(regExNameAt(rowIndex));
						else if (columnIndex == 2) FAT.precisionRegExNames.removeAll(regExNameAt(rowIndex));
					}
				}
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			}
		}
		
		private class CreateRegExDialog extends JDialog {
			
			private JTextField nameField;
			
			private RegExEditorPanel editor;
			private String regEx = null;
			private String regExName = null;
			
			CreateRegExDialog(FatConfigurarionDialog host, String regEx, String name) {
				super(host, "Create RegEx", true);
				
				this.nameField = new JTextField((name == null) ? "New RegEx" : name);
				this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Create");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						create();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						cancel();
					}
				});
				
				JPanel mainButtonPanel = new JPanel(new FlowLayout());
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	initialize editor
				JButton[] buttons = new JButton[1];
				buttons[0] = new JButton("Test RegEx");
				buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
				buttons[0].setPreferredSize(new Dimension(115, 21));
				buttons[0].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						testRegEx();
					}
				});
				this.editor = new RegExEditorPanel();
				this.editor.setSubPatternResolver(resolver);
				this.editor.setContent((regEx == null) ? "" : regEx);
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(this.nameField, BorderLayout.NORTH);
				this.getContentPane().add(this.editor, BorderLayout.CENTER);
				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(600, 400));
				this.setLocationRelativeTo(host);
			}
			
			private void testRegEx() {
				String rawRegEx = this.editor.getContent();
				String validationError = RegExUtils.validateRegEx(rawRegEx, resolver);
				if (validationError == null) {
					QueriableAnnotation data = Gamta.getTestDocument();
					if (data != null) {
						String regEx = RegExUtils.preCompile(rawRegEx, resolver);
						Annotation[] annotations = Gamta.extractAllMatches(data, regEx, 20);
						AnnotationDisplayDialog add = new AnnotationDisplayDialog(parent, ("Matches of RegEx '" + this.nameField.getText() + "'"), annotations, true);
						add.setLocationRelativeTo(this);
						add.setVisible(true);
					}
				}
				else JOptionPane.showMessageDialog(this, "The pattern is not valid:\n" + validationError, "RegEx Validation", JOptionPane.ERROR_MESSAGE);
			}
			
			private boolean isCommitted() {
				return (this.regEx != null);
			}
			
			private String getRegEx() {
				return this.regEx;
			}
			
			private String getRegExName() {
				return this.regExName;
			}
			
			private void cancel() {
				this.regEx = null;
				this.regExName = null;
				this.dispose();
			}
			
			private void create() {
				this.regEx = this.editor.getContent();
				this.regExName = this.nameField.getText();
				this.dispose();
			}
		}
	}
	
	private static class FatLexiconConfigurarionPanel extends JPanel {
		
		private FatConfigurarionDialog parent;
		private AnalyzerDataProvider dataProvider;
		
		private String currentLexiconName;
		private LexiconEditorPanel editor;
		private LexiconNameListPanel lexiconNameList;
		
		private JButton createButton = new JButton("Create");
		private JButton cloneButton = new JButton("Clone");
		private JButton deleteButton = new JButton("Delete");
		
		FatLexiconConfigurarionPanel(FatConfigurarionDialog parent, AnalyzerDataProvider dataProvider)  {
			super(new BorderLayout(), true);
			this.parent = parent;
			this.dataProvider = dataProvider;
			
			this.editor = new LexiconEditorPanel();
			
			this.lexiconNameList = new LexiconNameListPanel();	
			
			JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			
			this.createButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.createButton.setPreferredSize(new Dimension(100, 21));
			this.createButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (createLexicon())
						lexiconNameList.refresh();
				}
			});
			editButtons.add(this.createButton);
			
			this.cloneButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.cloneButton.setPreferredSize(new Dimension(100, 21));
			this.cloneButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (cloneLexicon())
						lexiconNameList.refresh();
				}
			});
			editButtons.add(this.cloneButton);
			
			this.deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.deleteButton.setPreferredSize(new Dimension(100, 21));
			this.deleteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (deleteLexicon())
						lexiconNameList.refresh();
				}
			});
			editButtons.add(this.deleteButton);
			
			this.add(this.editor, BorderLayout.CENTER);
			this.add(this.lexiconNameList, BorderLayout.EAST);
			this.add(editButtons, BorderLayout.NORTH);
		}
		
		private void exit() {
			if ((this.currentLexiconName != null) && this.editor.isDirty()) try {
				storeLexicon(this.currentLexiconName, this.editor.getContent());
			} catch (IOException ioe) {}
		}
		
		private void lexiconNameSelected(String newLexiconName) {
			if ((this.currentLexiconName != null) && editor.isDirty()) {
				try {
					storeLexicon(this.currentLexiconName, editor.getContent());
				}
				catch (IOException ioe) {
					if (JOptionPane.showConfirmDialog(null, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving regular expression to " + currentLexiconName.toString() + "\nProceed?"), "Could Not Save Lexicon", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
						lexiconNameList.setSelectedLexiconName(this.currentLexiconName);
						return;
					}
				}
			}
			this.currentLexiconName = newLexiconName;
			if (this.currentLexiconName != null) {
				StringVector lexicon = loadLexicon(currentLexiconName);
				if (lexicon == null) lexicon = new StringVector();
				this.editor.setContent(lexicon);
				this.deleteButton.setEnabled(!this.isNativeList(newLexiconName));
			}
			validate();
		}
		
		private StringVector loadLexicon(String lexiconName) {
//			StringVector lexicon = ((StringVector) nativeListsByName.get(lexiconName));
//			if (lexicon != null) return lexicon.union(lexicon);
			try {
				InputStream is = this.dataProvider.getInputStream(lexiconName);
				StringVector lexicon = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				lexicon.sortLexicographically(false, true);
				return lexicon;
			}
			catch (IOException ioe) {
				return null;
			}
		}
		
		private boolean storeLexicon(String lexiconName, StringVector lexicon) throws IOException {
//			nativeListsByName.remove(lexiconName);
			OutputStream os = this.dataProvider.getOutputStream(lexiconName);
			lexicon.storeContent(new OutputStreamWriter(os, "UTF-8"));
			os.flush();
			os.close();
			return true;
		}
		
		private boolean createLexicon() {
			return this.createLexicon(null, null);
		}
		
		private boolean cloneLexicon() {
			if (this.currentLexiconName == null)
				return this.createLexicon();
			else {
				String name = "New " + this.lexiconNameList.getSelectedLexiconName();
				return this.createLexicon(this.editor.getContent(), name);
			}
		}
		
		private boolean createLexicon(StringVector modelLexicon, String name) {
			CreateLexiconDialog cred = new CreateLexiconDialog(this.parent, modelLexicon, name);
			cred.setVisible(true);
			if (cred.isCommitted()) {
				StringVector lexicon = cred.getLexicon();
				String lexiconName = cred.getLexiconName();
				if (!lexiconName.endsWith(LEXICON_NAME_EXTENSION)) lexiconName += LEXICON_NAME_EXTENSION;
				try {
					this.storeLexicon(lexiconName, lexicon);
					return true;
				} catch (IOException ioe) {}
			}
			return false;
		}
		
		private boolean deleteLexicon() {
			String name = this.lexiconNameList.getSelectedLexiconName();
			if ((name != null) && (JOptionPane.showConfirmDialog(this, ("Really delete " + name), "Confirm Delete Lexicon", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
				try {
					if (this.dataProvider.deleteData(name)) {
						this.lexiconNameList.refresh();
						return true;
					}
					else {
						JOptionPane.showMessageDialog(this, ("Could not delete " + name), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
						return false;
					}
				}
				catch (Exception ioe) {
					JOptionPane.showMessageDialog(this, ("Could not delete " + name), "Delete Failed", JOptionPane.INFORMATION_MESSAGE);
					return false;
				}
			}
			else return false;
		}
		
		private boolean isNativeList(String name) {
			return nativeListNames.contains(name);
		}
		
		private class LexiconNameListPanel extends JPanel {
			
			private StringVector lexiconNames;
			
			private JTable lexiconNameList; 
			private JScrollPane lexiconNameListBox;
			
			LexiconNameListPanel() {
				super(new BorderLayout(), true);
				
				//	 initialize lexiconName list
				this.lexiconNameList = new JTable();
				this.lexiconNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.lexiconNameList.addMouseListener(new MouseAdapter() {
					int selectedRow = -1;
					public void mouseClicked(MouseEvent me) {
						int row = lexiconNameList.rowAtPoint(me.getPoint());
						if (row != selectedRow) {
							selectedRow = row;
							lexiconNameSelected(getSelectedLexiconName());
						}
					}
				});
				this.lexiconNameListBox = new JScrollPane(this.lexiconNameList);
				
				this.add(this.lexiconNameListBox, BorderLayout.CENTER);
				
				this.refresh();
			}
			
			private void setSelectedLexiconName(String lexiconName) {
				int index = this.lexiconNames.indexOf(lexiconName);
				this.lexiconNameList.setRowSelectionInterval(index, index);
			}
			
			private String getSelectedLexiconName() {
				int index = this.lexiconNameList.getSelectedRow();
				return this.lexiconNames.get(index);
			}
			
			private void refresh() {
				this.lexiconNames = this.readLexiconNameList();
				this.lexiconNameList.setModel(new LexiconNameListModel());
				this.lexiconNameList.getColumnModel().getColumn(0).setMaxWidth(60);
				this.lexiconNameList.getColumnModel().getColumn(1).setMaxWidth(80);
				this.lexiconNameList.validate();
			}
			
			private String lexiconNameAt(int index) {
				return ((this.lexiconNames == null) ? "" : this.lexiconNames.get(index));
			}
			
			private int getLexiconNameCount() {
				return ((this.lexiconNames == null) ? 0 : this.lexiconNames.size());
			}
			
			private StringVector readLexiconNameList() {
				StringVector lexiconNames = new StringVector();
				String[] names = dataProvider.getDataNames();
				for (int n = 0; n < names.length; n++) {
					if (names[n].endsWith(LEXICON_NAME_EXTENSION) || negativesDictionaryList.contains(names[n]))
						lexiconNames.addElementIgnoreDuplicates(names[n]);
				}
				lexiconNames.sortLexicographically(false, false);
				return lexiconNames;
			}
			
			private class LexiconNameListModel implements TableModel {
				public int getColumnCount() {
					return 3;
				}
				public int getRowCount() {
					return getLexiconNameCount();
				}
				public Class getColumnClass(int columnIndex) {
					if (columnIndex == 2) return String.class;
					else return Boolean.class;
				}
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "Is Native";
					else if (columnIndex == 1) return "Use Negative";
					else return "Name";
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					String name = lexiconNameAt(rowIndex);
					if (columnIndex == 0) return new Boolean(isNativeList(name));
					else if (columnIndex == 1) return new Boolean(negativesDictionaryList.contains(name));
					else return name;
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return ((columnIndex == 1) && !isNativeList(lexiconNameAt(rowIndex)));
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if ((columnIndex == 1) && !isNativeList(lexiconNameAt(rowIndex))) {
						if (((Boolean) newValue).booleanValue())
							negativesDictionaryList.addElementIgnoreDuplicates(lexiconNameAt(rowIndex));
						else negativesDictionaryList.removeAll(lexiconNameAt(rowIndex));
					}
				}
				
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			}
		}

		private class CreateLexiconDialog extends JDialog {
			
			private JTextField nameField;
			
			private LexiconEditorPanel editor;
			private StringVector lexicon = null;
			private String lexiconName = null;
			
			CreateLexiconDialog(FatConfigurarionDialog host, StringVector lexicon, String name) {
				super(host, "Create Lexicon", true);
				
				this.nameField = new JTextField((name == null) ? "New Lexicon" : name);
				this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Create");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						create();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						cancel();
					}
				});
				
				JPanel mainButtonPanel = new JPanel(new FlowLayout());
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	initialize editor
				this.editor = new LexiconEditorPanel();
				this.editor.setContent((lexicon == null) ? new StringVector() : lexicon);
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(this.nameField, BorderLayout.NORTH);
				this.getContentPane().add(this.editor, BorderLayout.CENTER);
				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(600, 400));
				this.setLocationRelativeTo(host);
			}
			
			private boolean isCommitted() {
				return (this.lexicon != null);
			}
			
			private StringVector getLexicon() {
				return this.lexicon;
			}
			
			private String getLexiconName() {
				return this.lexiconName;
			}
			
			private void cancel() {
				this.lexicon = null;
				this.lexiconName = null;
				this.dispose();
			}
			
			private void create() {
				this.lexicon = this.editor.getContent();
				this.lexiconName = this.nameField.getText();
				this.dispose();
			}
		}
		
		private class LexiconEditorPanel extends JPanel implements DocumentListener {
			
			private JTextArea editor;
			private JScrollPane editorBox;
			
			private StringVector content = new StringVector();
			
			private String fontName = "Verdana";
			private int fontSize = 12;
			
			private boolean dirty = false;
			private boolean contentInSync = true;
			
			LexiconEditorPanel() {
				super(new BorderLayout(), true);
				this.init();
			}
			
			private void init() {
				
				//	initialize editor
				this.editor = new JTextArea();
				this.editor.setEditable(true);
				
				//	wrap editor in scroll pane
				this.editorBox = new JScrollPane(this.editor);
				
				//	initialize buttons
				JButton sortButton = new JButton("Sort");
				sortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				sortButton.setPreferredSize(new Dimension(70, 21));
				sortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						sortList();
					}
				});
				
				JButton unionButton = new JButton("Union");
				unionButton.setBorder(BorderFactory.createRaisedBevelBorder());
				unionButton.setPreferredSize(new Dimension(70, 21));
				unionButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						unionList();
					}
				});
				
				JButton intersectButton = new JButton("Intersect");
				intersectButton.setBorder(BorderFactory.createRaisedBevelBorder());
				intersectButton.setPreferredSize(new Dimension(70, 21));
				intersectButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						intersectList();
					}
				});
				
				JButton subtractButton = new JButton("Subtract");
				subtractButton.setBorder(BorderFactory.createRaisedBevelBorder());
				subtractButton.setPreferredSize(new Dimension(70, 21));
				subtractButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						subtractList();
					}
				});
				
				JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.weightx = 1;
				gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridy = 0;
				gbc.gridx = 0;
				buttonPanel.add(sortButton, gbc.clone());
				gbc.gridx = 1;
				buttonPanel.add(unionButton, gbc.clone());
				gbc.gridx = 2;
				buttonPanel.add(intersectButton, gbc.clone());
				gbc.gridx = 3;
				buttonPanel.add(subtractButton, gbc.clone());
				
				
				//	put the whole stuff together
				this.add(this.editorBox, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
				this.refreshDisplay();
			}
			
			private StringVector getContent() {
				if (!this.contentInSync) {
//					this.content.clear();
//					this.content.parseAndAddElements(this.editor.getText(), "\n");
					this.synchronizeContent();
				}
				return this.content;
			}
			
			private void setContent(StringVector list) {
				this.content = list;
				this.refreshDisplay();
				this.dirty = false;
			}
			
			private synchronized void synchronizeContent() {
				if (this.contentInSync) return;
				
				this.content.clear();
				
				Document doc = this.editor.getDocument();
				StringBuffer entry = new StringBuffer();
				
				for (int c = 0; c < doc.getLength(); c++) try {
					String s = doc.getText(c, 1);
					if ("\n".equals(s)) {
						this.content.addElement(entry.toString().trim());
						entry = new StringBuffer();
					}
					else entry.append(s);
				} catch (BadLocationException ble) {}
				
				if (entry.length() != 0)
					this.content.addElement(entry.toString().trim());
				
				this.contentInSync = true;
			}
			
			private boolean isDirty() {
				return this.dirty;
			}
			
			private synchronized void refreshDisplay() {
				this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
				Document doc = new PlainDocument();
				for (int c = 0; c < this.content.size(); c++) try {
					if (c != 0) doc.insertString(doc.getLength(), "\n", null);
					doc.insertString(doc.getLength(), this.content.get(c), null);
				} catch (BadLocationException ble) {}
//				this.editor.setText(this.content.concatStrings("\n"));
//				this.editor.getDocument().addDocumentListener(this);
				doc.addDocumentListener(this);
				this.editor.setDocument(doc);
				this.contentInSync = true;
			}
			
			private void sortList() {
				int count = this.content.size();
				
				this.synchronizeContent();
				this.content.removeDuplicateElements(true);
				this.content.sortLexicographically(false, true);
				this.refreshDisplay();
				
				this.dirty = (this.dirty || (count != this.content.size()));
			}
			
			private void unionList() {
				StringVector list = this.getContent();
				Object listName = JOptionPane.showInputDialog(parent, "Select Lexicon to union with", "Union", JOptionPane.PLAIN_MESSAGE, null, lexiconNameList.lexiconNames.toStringArray(), currentLexiconName);
				if (listName == null) return;
				
				StringVector unionList = loadLexicon(listName.toString());
				if (unionList != null) {
					
					StringVector delta = unionList.without(list);
					ShowDeltaDialog sdd = new ShowDeltaDialog("Select Entries to Add", delta);
					sdd.setVisible(true);
					
					StringVector selected = sdd.getSelectedEntries();
					if (selected != null) {
//						this.setContent(list.union(selected, true));
						this.content.addContentIgnoreDuplicates(selected);
						this.sortList();
						this.dirty = true;
					}
				}
			}
			
			private void intersectList() {
				StringVector list = this.getContent();
				Object listName = JOptionPane.showInputDialog(parent, "Select Lexicon to intersect with", "Intersect", JOptionPane.PLAIN_MESSAGE, null, lexiconNameList.lexiconNames.toStringArray(), currentLexiconName);
				if (listName == null) return;
				
				StringVector intersectList = loadLexicon(listName.toString());
				if (intersectList != null) {
					
					StringVector delta = list.without(intersectList);
					ShowDeltaDialog sdd = new ShowDeltaDialog("Select Entries to Remove", delta);
					sdd.setVisible(true);
					
					StringVector selected = sdd.getSelectedEntries();
					if (selected != null) {
//						this.setContent(list.without(selected, true));
						this.content = this.content.without(selected, true);
						this.sortList();
						this.dirty = true;
					}
				}
			}
			
			private void subtractList() {
				StringVector list = this.getContent();
				Object listName = JOptionPane.showInputDialog(parent, "Select Lexicon to subtract", "Subtract", JOptionPane.PLAIN_MESSAGE, null, lexiconNameList.lexiconNames.toStringArray(), currentLexiconName);
				if (listName == null) return;
				
				StringVector subtractList = loadLexicon(listName.toString());
				if (subtractList != null) {
					
					StringVector delta = list.intersect(subtractList);
					ShowDeltaDialog sdd = new ShowDeltaDialog("Select Entries to Remove", delta);
					sdd.setVisible(true);
					
					StringVector selected = sdd.getSelectedEntries();
					if (selected != null) {
//						this.setContent(list.without(selected, true));
						this.content = this.content.without(selected, true);
						this.sortList();
						this.dirty = true;
					}
				}
			}
			
			/** @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
			 */
			public void changedUpdate(DocumentEvent e) {
				//	attribute changes are not of interest for now
			}
			
			/** @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
			 */
			public void insertUpdate(DocumentEvent e) {
				this.dirty = true;
				this.contentInSync = false;
			}
			
			/** @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
			 */
			public void removeUpdate(DocumentEvent e) {
				this.dirty = true;
				this.contentInSync = false;
			}
			
			private class ShowDeltaDialog extends JDialog {
				
				private StringVector delta;
				private StringVector selected = new StringVector();
				
				private JTable entryList;
				
				ShowDeltaDialog(String title, StringVector delta) {
					super(parent, title, true);
					this.delta = delta;
					
					this.entryList = new JTable(new EntryListModel());
					this.entryList.getColumnModel().getColumn(0).setMaxWidth(50);
					JScrollPane entryListBox = new JScrollPane(this.entryList);
					
					JButton selectAllButton = new JButton("Select All");
					selectAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
					selectAllButton.setPreferredSize(new Dimension(70, 21));
					selectAllButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							selectAll();
						}
					});
					
					JButton deselectAllButton = new JButton("Deselect All");
					deselectAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
					deselectAllButton.setPreferredSize(new Dimension(70, 21));
					deselectAllButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							deselectAll();
						}
					});
					
					JPanel entryListButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					entryListButtonPanel.add(selectAllButton);
					entryListButtonPanel.add(deselectAllButton);
					
					JPanel entryListPanel = new JPanel(new BorderLayout());
					entryListPanel.add(entryListBox, BorderLayout.CENTER);
					entryListPanel.add(entryListButtonPanel, BorderLayout.SOUTH);
					
					JButton okButton = new JButton("OK");
					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okButton.setPreferredSize(new Dimension(100, 21));
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ShowDeltaDialog.this.dispose();
						}
					});
					
					JButton cancelButton = new JButton("Cancel");
					cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
					cancelButton.setPreferredSize(new Dimension(100, 21));
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							selected = null;
							ShowDeltaDialog.this.dispose();
						}
					});
					
					JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					buttonPanel.add(okButton);
					buttonPanel.add(cancelButton);
					
					this.getContentPane().setLayout(new BorderLayout());
					this.getContentPane().add(entryListPanel, BorderLayout.CENTER);
					this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
					
					this.setSize(400, 600);
					this.setLocationRelativeTo(parent);
				}
				
				private StringVector getSelectedEntries() {
					return this.selected;
				}
				
				private void selectAll() {
					this.selected.addContentIgnoreDuplicates(this.delta);
					this.entryList.repaint();
					this.validate();
				}
				
				private void deselectAll() {
					this.selected.clear();
					this.entryList.repaint();
					this.validate();
				}
				
				private class EntryListModel implements TableModel {
					public int getColumnCount() {
						return 2;
					}
					public int getRowCount() {
						return delta.size();
					}
					public Class getColumnClass(int columnIndex) {
						return ((columnIndex == 0) ? Boolean.class : String.class);
					}
					public String getColumnName(int columnIndex) {
						return ((columnIndex == 0) ? "Select" : "Entry");
					}
					public Object getValueAt(int rowIndex, int columnIndex) {
						String entry = delta.get(rowIndex);
						if (columnIndex == 0) return new Boolean(selected.contains(entry));
						else return entry;
					}
					public boolean isCellEditable(int rowIndex, int columnIndex) {
						return (columnIndex == 0);
					}
					public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
						if (columnIndex == 0) {
							if (((Boolean) newValue).booleanValue())
								 selected.addElementIgnoreDuplicates(delta.get(rowIndex));
							else selected.removeAll(delta.get(rowIndex));
						}
					}
					
					public void addTableModelListener(TableModelListener l) {}
					public void removeTableModelListener(TableModelListener l) {}
				}
			}
		}
	}
	
	/**
	 * A compiled dictionary is aimed at holding large lookup lists of strings in as
	 * little memory as possible. Their lifecycle is divided into two phases: a
	 * building phase and a lookup phase, separated by the compilation. Before the
	 * compilation (during the build phase), new strings can be added to the
	 * dictionary, but lookup operations are undefined. After the compilation, no
	 * new strings can be added, but lookup operations are possible. This is because
	 * during the compilation, the dictionary is compressed into a specialized
	 * lookup data structure that reduces memory requirements, but prohibits further
	 * insertions.<BR>
	 * Warning: In favor of keeping memory footprint low, this class <B>does not
	 * support unicode</B>, but stores strings as sequences of 1-byte ASCII-8
	 * characters. In cases where exact lookups for non-ASCII-8 characters are
	 * required, use CompiledDictionaty instead.
	 * 
	 * @author sautter
	 */
	private static class CompiledByteDictionary implements Dictionary {
		private static final int chunkSize = 1020;
		private static final Comparator byteChunkComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				byte[][] b1 = ((byte[][]) o1);
				byte[][] b2 = ((byte[][]) o2);
				
				int c = byteComparator.compare(b1[b1.length-1], b2[0]);
				if (c < 0) return -1;
				
				c = byteComparator.compare(b2[b2.length-1], b1[0]);
				if (c < 0) return 1;
				
				return 0;
			}
		};
		private static final Comparator byteComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				byte[] b1 = ((byte[]) o1);
				byte[] b2 = ((byte[]) o2);
				for (int b = 0; b < Math.min(b1.length, b2.length); b++) {
					if (b1[b] != b2[b])
						return ((255 & b1[b]) - (255 & b2[b]));
				}
				return (b1.length - b2.length);
			}
		};
		
		
		private Set entrySet = new TreeSet();
		
		private int entryCount = -1;
		private Map csChunks = new TreeMap(byteChunkComparator);
		private Map ciChunks = new TreeMap(byteChunkComparator);
		
		/**
		 * Constructor building a case sensitive dictionary
		 */
		CompiledByteDictionary() {}
		
		/**
		 * Add a string to the dictionary. Note that this method has only an effect
		 * before the compilation. Likewise, looking up the argument string returns
		 * true only after the compilation.
		 * @param string the string to add
		 */
		synchronized void add(String string) {
			if ((this.entrySet != null) && (string != null))
				this.entrySet.add(string);
		}
		
//		/**
//		 * Check whether the dictionary has been compiled.
//		 * @return true if has been compiled, false otherwise
//		 */
//		synchronized boolean isCompiled() {
//			return (this.entrySet == null);
//		}
//		
		/**
		 * Compile the dictionary. This disables adding further strings, and enables
		 * lookup. Invocing this method more than once has no effect.
		 */
		synchronized void compile() {
			if (this.entrySet == null) return;
			
			this.entryCount = this.entrySet.size();
			
			TreeSet ciEntryCollector = new TreeSet();
			
			compile(this.entrySet, this.csChunks, ciEntryCollector);
			compile(ciEntryCollector, this.ciChunks, null);
			
			this.entrySet = null;
			ciEntryCollector = null;
			System.gc();
			
			System.out.println("Compiled, register sizes are:\n- " + this.csChunks.size() + " CS chunks\n- " + this.ciChunks.size() + " CI chunks");
		}
		
		private static void compile(Set entrySet, Map chunks, Set ciEntryCollector) {
			ArrayList chunkCollector = new ArrayList(chunkSize);
			
			for (Iterator it = entrySet.iterator(); it.hasNext();) {
				String de = ((String) it.next());
				chunkCollector.add(de);
				if (chunkCollector.size() == chunkSize) {
					String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
					byte[][] bChunk = new byte[chunk.length][];
					for (int b = 0; b < bChunk.length; b++)
						bChunk[b] = encode(chunk[b]);
					chunks.put(bChunk, bChunk);
					chunkCollector.clear();
				}
				it.remove();
				
				if (ciEntryCollector != null) {
					String ciDe = de.toLowerCase();
					if (!de.equals(ciDe) && !entrySet.contains(ciDe))
						ciEntryCollector.add(ciDe);
				}
			}
			
			if (chunkCollector.size() != 0) {
				String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
				byte[][] bChunk = new byte[chunk.length][];
				for (int b = 0; b < bChunk.length; b++)
					bChunk[b] = encode(chunk[b]);
				chunks.put(bChunk, bChunk);
			}
			
			chunkCollector.clear();
			System.gc();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
		 */
		public StringIterator getEntryIterator() {
			final Iterator it = this.csChunks.values().iterator();
			return new StringIterator() {
				Iterator pit = null;
				public boolean hasNext() {
					if (this.pit == null) {
						if (it.hasNext()) {
							byte[][] pitData = ((byte[][]) it.next());
							this.pit = Arrays.asList(pitData).iterator();
							return this.pit.hasNext();
						}
						else return false;
					}
					else if (this.pit.hasNext())
						return true;
					else {
						this.pit = null;
						return this.hasNext();
					}
				}
				public Object next() {
					if (this.hasNext())
						return decode((byte[]) this.pit.next());
					else return null;
				}
				public void remove() {}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
		 */
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
		 */
		public boolean isEmpty() {
			return this.csChunks.isEmpty();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
		 */
		public int size() {
			return this.entryCount;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
		 */
		public boolean lookup(String string) {
			return this.lookup(string, true);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
		 */
		public boolean lookup(String string, boolean caseSensitive) {
			if (lookup(this.csChunks, string))
				return true;
			else if (caseSensitive)
				return false;
			else {
				string = string.toLowerCase();
				return (lookup(this.csChunks, string) || lookup(this.ciChunks, string));
			}
		}
		
		private static boolean lookup(Map chunks, String string) {
			byte[] bytes = encode(string);
			byte[][] bLookup = {bytes};
			byte[][] bChunk = ((byte[][]) chunks.get(bLookup));
			if (bChunk == null) return false;
			int index = Arrays.binarySearch(bChunk, bytes, byteComparator);
			return ((index >= 0) && (index < bChunk.length) && (byteComparator.compare(bChunk[index], bytes) == 0));
		}
		
		private static byte[] encode(String s) {
			byte[] bytes = new byte[s.length()];
			for (int c = 0; c < s.length(); c++) {
				char ch = s.charAt(c);
				if (ch < 255)
					bytes[c] = ((byte) s.charAt(c));
				else bytes[c] = ((byte) 255);
			}
			return bytes;
		}
		
		private static String decode(byte[] bytes) {
			StringBuffer sb = new StringBuffer();
			for (int b = 0; b < bytes.length; b++)
				sb.append((char) (255 & bytes[b]));
			return sb.toString();
		}
	}
}