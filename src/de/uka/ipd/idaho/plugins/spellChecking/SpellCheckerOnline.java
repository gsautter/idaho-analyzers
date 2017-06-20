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

package de.uka.ipd.idaho.plugins.spellChecking;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.plugins.spellChecking.SpellCheckerOnline.SpellCheckingPanel.CorrectionData;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Learning component for finding and correcting misspelled words. This
 * component considers a word as spelled correctly if it (a) is contained in one
 * of the dictionaries (see below), (b) is spelled the same way throughout the
 * document at least a specific number of times (default is 3, see
 * acceptFrequencyThreshold).<br>
 * <br>
 * There are five parameters influencing the component's behavior:<br> -
 * <b>acceptFrequencyThreshold</b>: the minimum number of times a word has to
 * appear in a document in the same spelling in order to be considered as
 * spelled correctly. Comparison is case insensitive here, default is 3.<br> -
 * <b>rememberFrequencyThreshold</b>: the minimum number of times a word has to
 * appear in a document in the same spelling in order to be considered as
 * spelled correctly and to be added to the custom part of a dictionary (see
 * below). Comparison is case insensitive here. This value will be at least as
 * higher as acceptFrequencyThreshold, no matter which value is actually
 * configured, default is 5. <br> - <b>suggestionDistanceThreshold</b>: the
 * maximum Levenshtein distance between a word recognized as spelled incorrectly
 * and corrections suggested to the user. The larger the threshold, the more
 * suggestions, and the more effort for extracting the suggestions from the
 * dictionaries, default is 3. <br> - <b>maximumSplitParts</b>: the maximum
 * number of parts an word recognized as spelled incorrectly is split into in
 * order to decompose it into words known to be spelled correctly, and at the
 * same time the maximum number of words concatenated around a word recognized
 * as spelled incorrectly in order to compose a word known to be spelled
 * correctly. Thus, this parameter controls the behavior in splitting or
 * re-joining words whose borders have been messed up, eg by OCR, default is 3.
 * <br>- <b>languageMatchThreshold</b>: the minimum fraction (percent value)
 * of words that have to come from a specific language dictionary (see below) in
 * order to accept this particular dictionary as the one for a document's main
 * language (see below). Default is 67 percent, and the value will always be in
 * the range [51, 100], regardless of the value actually configured.<br>
 * <br>
 * Detection of words with incorrect spelling works in eight steps:<br> -
 * <b>Word Extraction</b>: extract the actual words from the document, ignoring
 * numbers, punctuation marks, URLs, eMail addresses, etc.<br> - <b>Language
 * Classification</b>: check for all the language dictionaries (see below) how
 * many of the document's words are found in that particular dictionary. If the
 * fraction of the words found exceeds languageMatchThreshold for one of the
 * dictionaries, the language represented by this dictionary is considered the
 * document's <i>main language</i>, the dictionary itself the <i>main
 * dictionary</i> for the document. If not, the document possibly belongs to a
 * so far unknown language, therefore the user is asked for the language.<br>-
 * <b>Dictionary Bootstrapping</b>: extract those words from the document that
 * appear at least acceptFrequencyThreshold times and collect them in the
 * <i>dynamic dictionary</i>.<br>- <b>Incorrect Word Extraction</b>: look up
 * all the document words in all the dictionaries available, distinguishing five
 * cases (1) the word is found in the main dictionary of the document ==>
 * spelled correctly, (2) the word is found in the dynamic dictionary ==>
 * spelled correctly, (3) the word is found in the shared dictionary (see below)
 * ==> spelled correctly, (4) the word is found in one of the language
 * dictionaries that is not the main dictionary for the document ==> spelled
 * correctly, but <i>out of language</i>, and (5) the word is not found in any
 * of the dictionaries ==> spelled incorrectly.<br>- <b>Search For Corrections</b>:
 * for those words that are spelled incorrectly, find words from the main
 * dictionary (if any), the dynamic dictionary, the shared dictionary, and from
 * the remaining language dictionaries that have a Levenshtein edit distance of
 * at most suggestionDistanceThreshold to the word in question. Ordered by edit
 * distance and dictionary of origin (order as before), store those words as
 * suggested correct forms of the word spelled incorrectly.<br>- <b>Word
 * Separation Check</b>: as incorrect words may also arise from a word split in
 * two by a space or several words stuck together as a result of lacking
 * space(s), look up incorrect words in the dictionaries (order as above) as
 * follows, trying two things: (1) Find prefixes of words in the dictionaries,
 * and in case of a hit, try splitting the prefix found and continue with the
 * remaining suffix. Continue until (a) the word in question has been decomposed
 * to words from the dictionaries (which are then stored as a suggested
 * correction) or (b) a further split would exceed the limit of
 * maximumSplitParts. (2) Find in the dictionaries the concatenation of a word
 * in question with the words before and after, stopping at non-words (numbers,
 * punctuation), and trying at most maximumSplitParts subsequent words together.
 * In case of a hit, annotate the words whose concatenation has been found as
 * incorrect (all together) and remember the composed word as a suggested
 * correction.<br>- <b>Spelling Correction</b>: For all the words (and word
 * sequences) marked as spelled incorrectly, prompt the user with the suggested
 * correct forms gathered in the previous two steps. Let the user choose the
 * appropriate correction, or mark the word as correct (==> <i>unknown word</i>),
 * or enter an unsuggested correction manually (==> <i>unknown word</i>).<br>-
 * <b>Learning</b>: user input is a valuable source of new information, learn
 * from it as much as possible. (1) while checking the misspelled words, do not
 * only offer suggestions, but also offer options of adding either the word in
 * question ('Keep &amp; ...') or the entered or selected correction ('Correct
 * &amp; ...') to the custom part of a dictionary. For selecting the dictionary,
 * offer three options: '... &amp; Remember Shared' for the shared dictionary,
 * '... &amp; Remember _Language_' (with _Language_ being replaced with the
 * document's main language) for the main dictionary, and '... &amp; Remember as
 * ...' for selecting a language dictionary other than the main dictionary.
 * Remove the words approved in this way from the dynamic dictionary. (2) add
 * the words from the dynamic dictionary whose frequency is at least
 * rememberFrequencyThreshold to the custom part of whichever dictionary the
 * user chooses. (3) have the remaining words of the dynamic dictionary checked
 * by the user and add the approved ones to the custom part of whatever
 * dictionary the user chooses. <br>
 * <br>
 * Spelling Correction uses a series of dictionaries:<br> - <b>Language
 * Dictionary</b>: is specific to a certain language, consists of a static
 * dictionary (see below) and a custom dictionary (see below).<br>- <b>Shared
 * Dictionary</b>: dictionary shared between all the languages known to the
 * spell checker, eg for technical terms, also consists of a static dictionary
 * (see below) and a custom dictionary (see below).<br>- <b>Static Dictionary</b>:
 * a basic dictionary not changed by learning, thus a static list of words
 * specified prior to start of operations.<br>- <b>Custom Dictionary</b>: a
 * dictionary storing the words learned during operation.<br>
 * The distinction of static dictionary and custom dictionary simplifies
 * cleaning the dictionaries in case of errors. In particular, the words added
 * to a custom are only the ones not contained in the corresponding static
 * dictionary. The shared dictionary is for words that are rather domain
 * specific than specific to a particular language, eg technical terms. However,
 * the custom part of the shared dictionary notes the main languages of the
 * documents showing an occurrence of each word contained in it. If a main
 * language and main dictionary cannot be determined, the document might belong
 * to a language not known to the spell checker so far. In this case, the user
 * may choose to instantiate the custom part of a language dictionary for the
 * new language from the dynamic dictionary; in this way, the spell checker is
 * capable of learning new languages from scratch.
 * 
 * @author sautter
 */
public class SpellCheckerOnline extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
//	private static final String BASIC_DICTIONARY_FILE_NAME = "basicDictionary.txt";
//	private static final String CUSTOM_DICTIONARY_FILE_NAME = "customDictionary.txt";
	
//	private boolean initialized = false;
	
//	private StringVector basicDictionary = null;
//	private StringVector customDictionary = null;
	
	private static final String SHARED_DICTIONARY_LANGUAGE = "Shared";
	private static final String STATIC_DICTIONARY_SUFFIX = ".static.txt";
	private static final String CUSTOM_DICTIONARY_SUFFIX = ".custom.txt";
	
	private static final String MISSPELLING_ANNOTATION_TYPE = "misspelling";
	private static final String MISSPELLING_TYPE_ATTRIBUTE = "type";
	private static final String MISSPELLED_MISSPELLING_TYPE = "misspelled";
	private static final String OUT_OF_LANGUAGE_MISSPELLING_TYPE = "outOfLanguage";
	private static final String LANGUAGE_ATTRIBUTE = "language";
	private static final String CORRECTIONS_ATTRIBUTE = "corrections";
	
//	private String[] languages = new String[0];
	
	private int acceptFrequencyThreshold = 3;
	private int rememberFrequencyThreshold = 5;
	
	private int suggestionDistanceThreshold = 3;
	private int maximumSplitParts = 3;
	
	private int languageMatchThreshold = 67;
	
	private String[] skipRegExes = new String[0];
	
	private HashMap languageDictionaries = new HashMap();
	private StringVector useLanguageDictionaries = new StringVector();
	private SharedDictionary sharedDictionary;
	
	public SpellCheckerOnline() {}
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	==== Step 1: Word Extraction ====
		StringVector words = new StringVector();
		StringVector distinctWords = new StringVector();
		StringVector wordParts = new StringVector();
		StringVector distinctWordParts = new StringVector();
		
		//	get skip annotations
		ArrayList skipAnnotationList = new ArrayList();
		for (int s = 0; s < this.skipRegExes.length; s++) {
			Annotation[] skipAnnotations = Gamta.extractAllMatches(data, this.skipRegExes[s], true, false, false);
			for (int a = 0; a < skipAnnotations.length; a++)
				skipAnnotationList.add(skipAnnotations[a]);
		}
		Annotation[] skipAnnotations = ((Annotation[]) skipAnnotationList.toArray(new Annotation[skipAnnotationList.size()]));
		Arrays.sort(skipAnnotations);
		int skipAnnotationIndex = 0;
		Annotation skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
		System.out.println("Got " + skipAnnotations.length + " skip annotations:");
		for (int s = 0; s < skipAnnotations.length; s++)
			System.out.println("  '" + skipAnnotations[s] + "'");
		
		//	extract words to check
		for (int v = 0; v < data.size(); v++) {
			String value = Gamta.normalize(data.valueAt(v).toLowerCase());
			if (Gamta.isWord(value)) {
				
				//	split at possible inner punctuation marks
				String[] valueParts = value.split("[^a-z]");
				
				//	no skip annotation, or before current skip annotation
				if ((skipAnnotation == null) || (v < skipAnnotation.getStartIndex())) {
					words.addElement(value);
					distinctWords.addElementIgnoreDuplicates(value);
					wordParts.addContent(valueParts);
					distinctWordParts.addContentIgnoreDuplicates(valueParts);
				}
				
				//	after current skip annotation, move to next
				else if (v >= skipAnnotation.getEndIndex()) {
					
					//	find next relevant skip annotation (they might be nested)
					while ((skipAnnotationIndex < skipAnnotations.length) && (skipAnnotations[skipAnnotationIndex].getEndIndex() <= v))
						skipAnnotationIndex++;
					
					//	get first matching skip annotation (if any)
					skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
					
					//	no skip annotation found, or before its start
					if ((skipAnnotation == null) || (v < skipAnnotation.getStartIndex())) {
						words.addElement(value);
						distinctWords.addElementIgnoreDuplicates(value);
						wordParts.addContent(valueParts);
						distinctWordParts.addContentIgnoreDuplicates(valueParts);
					}
				}
			}
		}
		
		//	clean up empty strings that might originate from non-words
		words.removeAll("");
		distinctWords.removeAll("");
		wordParts.removeAll("");
		distinctWordParts.removeAll("");
		System.out.println("Got " + words.size() + " words, " + distinctWords.size() + " distinct words, " + wordParts.size() + " word parts, " + distinctWordParts.size() + " distinct word parts");
		
		
		//	==== Step 2: Language Classification ====
		LanguageDictionary mainDictionary = this.getMainDictionary(wordParts, distinctWordParts);
		String mainLanguage = ((mainDictionary == null) ? null : mainDictionary.language);
		
		//	could not determine language
		if (mainDictionary == null) {
			
//			//	ask user for main language if allowed to
//			if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
//				
//				//	don't accept empty answer
//				while (mainLanguage == null) {
//					
//					//	TODO: ask for language using feedback API
//					Object languageObject = null;//JOptionPane.showInputDialog(DialogFactory.getTopWindow(), "Could not determine the language of the document.\nPlease select a language from the list.", "Unknown Language", JOptionPane.QUESTION_MESSAGE, null, this.languages, null);
//					
//					//	dialog not cancelled
//					if (languageObject != null) {
//						mainLanguage = languageObject.toString();
//						mainDictionary = ((LanguageDictionary) this.languageDictionaries.get(mainLanguage));
//						
//						//	so far unknown language, create new dictionary (we're about to start learning a new language :-)
//						if (mainDictionary == null) { 
//							mainDictionary = new LanguageDictionary(mainLanguage);
//							this.languageDictionaries.put(mainLanguage, mainDictionary);
//							this.useLanguageDictionaries.addElementIgnoreDuplicates(mainLanguage);
//						}
//						
//						//	TODO: think of learning a stemmer for new languages !!!
//					}
//				}
//			}
//			
//			//	must not prompt user, use shared dictionary
//			else {
//				mainDictionary = this.sharedDictionary;
//				mainLanguage = this.sharedDictionary.language;
//			}
			mainDictionary = this.sharedDictionary;
//			mainLanguage = this.sharedDictionary.language;
		}
		System.out.println("Got main language: " + mainLanguage);
		
		
		//	==== Step 3: Dictionary Bootstrapping ====
		SortedSet dynamicDictionary = new TreeSet(lengthFirstComparator);
		for (int w = 0; w < distinctWordParts.size(); w++) {
			String word = distinctWordParts.get(w);
			if (wordParts.getElementCount(word) >= this.acceptFrequencyThreshold)
				dynamicDictionary.add(word);
		}
		System.out.println("Got " + dynamicDictionary.size() + " words in dynamic dictionary");
		
		
		//	==== Step 4: Incorrect Word Extraction ====
		StringVector unknownWords = new StringVector(); // list of unknown words
		Properties unknownWordLanguages = new Properties(); // mapping of unknown words to non-main languages (for out of language words)
		
		//	build meta dictionary
		ArrayList dictionaryList = new ArrayList();
		dictionaryList.add(mainDictionary.staticDictionary);
		dictionaryList.add(mainDictionary.customDictionary);
		dictionaryList.add(dynamicDictionary);
		if (mainDictionary != this.sharedDictionary) {
			dictionaryList.add(this.sharedDictionary.staticDictionary);
			dictionaryList.add(this.sharedDictionary.customDictionary);
		}
		SortedSet[] dictionaries = ((SortedSet[]) dictionaryList.toArray(new SortedSet[dictionaryList.size()]));
		
		//	check all the distinct words
		for (int w = 0; w < distinctWords.size(); w++) {
			String word = distinctWords.get(w);
			
			//	omit single letters
			if (word.length() > 1) {
				
				//	split word into pure letter blocks
				String[] parts = word.split("[^a-z]");
				
				//	make sure there is a surrogat key for the longest word
				if (LanguageDictionary.lengthSurrogatKeys.size() <= word.length()) {
					
					//	add seed element if not done so far
					if (LanguageDictionary.lengthSurrogatKeys.isEmpty())
						LanguageDictionary.lengthSurrogatKeys.addElement("");
					
					//	keep on extending last element (size of vector matches last added surrogate's length)
					while (LanguageDictionary.lengthSurrogatKeys.size() <= word.length())
						LanguageDictionary.lengthSurrogatKeys.addElement(LanguageDictionary.lengthSurrogatKeys.lastElement() + "a");
				}
				
				//	check dictionaries with admissable words
				if (isContained(word, dictionaries)) word = null;
				
				//	check word parts (check only words with more than one part)
				else if (parts.length != 1) {
					boolean partMatch = true;
					for (int p = 0; (parts != null) && (p < parts.length); p++)
						partMatch = (((parts[p].length() <= 1) || isContained(parts[p], dictionaries)) && partMatch);
					if (partMatch) word = null;
				}
				
				//	check other language dictionaries
				for (int d = 0; (word != null) && (d < this.useLanguageDictionaries.size()); d++) {
					LanguageDictionary ld = ((LanguageDictionary) this.languageDictionaries.get(this.useLanguageDictionaries.get(d)));
					if (ld != mainDictionary) {
						if (ld.contains(word)) {
							unknownWords.addElement(word);
							unknownWordLanguages.setProperty(word, ld.language);
							word = null;
						}
						else {
							boolean partMatch = true;
							for (int p = 0; (parts != null) && (p < parts.length); p++)
								partMatch = (isContained(parts[p], dictionaries) && partMatch);
							if (partMatch) {
								unknownWords.addElement(word);
								unknownWordLanguages.setProperty(word, ld.language);
								word = null;
							}
						}
					}
				}
				
				//	remember word as misspelled
				if (word != null) {
					unknownWords.addElement(word);
					System.out.println("Got unknwon word '" + word + "'");
				}
			}
		}
		System.out.println("Got " + unknownWords.size() + " unknown words, " + unknownWordLanguages.size() + " of them from other languages");
		
		
		//	==== Step 5: Search For Corrections ====
		HashMap unknownWordCorrections = new HashMap();
		StringVector endings = new StringVector();
		
		//	find corrections for unknown words, and collect suffixes that distinguish a known word from an unknown one
		for (int u = 0; u < unknownWords.size(); u++) {
			String unknownWord = unknownWords.get(u);
			StringVector corrections = new StringVector();
			int maxCorrectionDistance = Math.min(((unknownWord.length() + 1) / 2), this.suggestionDistanceThreshold);
			System.out.print("Getting corrections for '" + unknownWord + "' with max distance " + maxCorrectionDistance + " ...");
			
			//	check main dictionary
			corrections.addContentIgnoreDuplicates(getCorections(unknownWord, mainDictionary.staticDictionary, maxCorrectionDistance, endings));
			corrections.addContentIgnoreDuplicates(getCorections(unknownWord, mainDictionary.customDictionary, maxCorrectionDistance, endings));
			
			//	check dynamic dictionary
			corrections.addContentIgnoreDuplicates(getCorections(unknownWord, dynamicDictionary, maxCorrectionDistance, endings));
			
			//	check shared dictionary
			if (mainDictionary != this.sharedDictionary) {
				corrections.addContentIgnoreDuplicates(getCorections(unknownWord, this.sharedDictionary.staticDictionary, maxCorrectionDistance, endings));
				corrections.addContentIgnoreDuplicates(getCorections(unknownWord, this.sharedDictionary.customDictionary, maxCorrectionDistance, endings));
			}
			
			//	TODO: for multi-part words, get check parts and get corrections for unknown ones
			
			//	store possible corrections
			unknownWordCorrections.put(unknownWord, corrections);
			System.out.println(" " + corrections.size() + " corrections");
		}
		
		//	extract distinct endings
		StringVector distinctEndings = new StringVector();
		distinctEndings.addContentIgnoreDuplicates(endings);
		System.out.println("Got " + distinctEndings.size() + " endings:");
		for (int e = 0; e < distinctEndings.size(); e++)
			System.out.println("  " + distinctEndings.get(e) + " (" + endings.getElementCount(distinctEndings.get(e)) + ")");
		
		//	check unknown words regarding being known words with a frequent ending
		for (int u = 0; u < unknownWords.size(); u++) {
			String unknownWord = unknownWords.get(u);
			StringVector corrections = ((StringVector) unknownWordCorrections.get(unknownWord));
			for (int e = 0; e < distinctEndings.size(); e++) {
				String ending = distinctEndings.get(e);
				if ((endings.getElementCount(ending) >= this.rememberFrequencyThreshold) && ((unknownWord.length() - ending.length()) > 1) && unknownWord.endsWith(ending) && corrections.contains(unknownWord.substring(0, (unknownWord.length() - ending.length())))) {
					System.out.println("Found '" + unknownWord + "' to be a flected form of '" + unknownWord.substring(0, (unknownWord.length() - ending.length())) + "'");
					unknownWords.remove(u--);
					unknownWordCorrections.remove(unknownWord);
					e = distinctEndings.size();
				}
			}
		}
		
		//	==== Step 6: Word Separation Check ====
		
		//	find unknown words that are concatenations of known words
		for (int u = 0; u < unknownWords.size(); u++) {
			String unknownWord = unknownWords.get(u);
			String[] splits = splitConcatenation(unknownWord, dictionaries, false, this.maximumSplitParts);
			StringVector corrections = ((StringVector) unknownWordCorrections.get(unknownWord));
			if (corrections == null) {
				corrections = new StringVector();
				unknownWordCorrections.put(unknownWord, corrections);
			}
			corrections.addContent(splits);
			if (splits.length != 0) {
				System.out.println("Got split corrections for '" + unknownWord + "':");
				for (int s = 0; s < splits.length; s++)
					System.out.println("  '" + splits[s] + "'");
			}
		}
		
		//	find known words split into unknown parts
		for (int v = 0; v < data.size(); v++) {
			String value = data.valueAt(v).toLowerCase();
			if (unknownWords.contains(value)) {
				
				//	add annotation
				Annotation uw = data.addAnnotation(MISSPELLING_ANNOTATION_TYPE, v, 1);
				if (unknownWordLanguages.containsKey(value)) {
					uw.setAttribute(MISSPELLING_TYPE_ATTRIBUTE, OUT_OF_LANGUAGE_MISSPELLING_TYPE);
					uw.setAttribute(LANGUAGE_ATTRIBUTE, unknownWordLanguages.getProperty(value));
				}
				else uw.setAttribute(MISSPELLING_TYPE_ATTRIBUTE, MISSPELLED_MISSPELLING_TYPE);
				
				//	add suggested corrections
				StringVector corrections = ((StringVector) unknownWordCorrections.get(value));
				if (corrections == null)
					corrections = new StringVector();
				
				//	sort corrections by edit distance
				String[] sortedCorrections = corrections.toStringArray();
				final HashMap distances = new HashMap();
				for (int c = 0; c < sortedCorrections.length; c++)
					distances.put(sortedCorrections[c], new Integer(StringUtils.getLevenshteinDistance(sortedCorrections[c], value, suggestionDistanceThreshold, false)));
				Arrays.sort(sortedCorrections, new Comparator() {
					public int compare(Object o1, Object o2) {
						String s1 = ((String) o1);
						String s2 = ((String) o2);
						int d1 = ((Integer) distances.get(s1)).intValue();
						int d2 = ((Integer) distances.get(s2)).intValue();
						return ((d1 == d2) ? s1.toString().compareTo(s2.toString()) : (d1 - d2));
					}
				});
				for (int c = 0; c < sortedCorrections.length; c++)
					sortedCorrections[c] = adjustCapitalization(sortedCorrections[c], data.valueAt(v));
				
				//	add splits for camel case words
				if (data.valueAt(v).matches("[A-Z]+([^A-Z]*[a-z][A-Z]+)+[^A-Z]*")) {
					String fullValue = data.valueAt(v);
					StringBuffer split = new StringBuffer();
					for (int c = 0; c < fullValue.length(); c++) {
						if ((c != 0) && Character.isUpperCase(fullValue.charAt(c)))
							split.append(" ");
						split.append(fullValue.charAt(c));
					}
					String[] newSortedCorrections = new String[sortedCorrections.length + 1];
					newSortedCorrections[0] = split.toString();
					System.arraycopy(sortedCorrections, 0, newSortedCorrections, 1, sortedCorrections.length);
					sortedCorrections = newSortedCorrections;
					System.out.println("Got camel case split correction for '" + fullValue + "': " + split.toString());
				}
				
				//	add high comma / hyphen free versions of words with embedded high comma or hyphen
				String[] parts = value.split("[^a-z]");
				if (parts.length != 1) {
					StringBuffer asOnePart = new StringBuffer();
					for (int p = 0; p < parts.length; p++)
						asOnePart.append(parts[p]);
					
					String fullValue = data.valueAt(v);
					parts = fullValue.split("[^a-zA-Z]");
					StringBuffer asOnePartFull = new StringBuffer();
					for (int p = 0; p < parts.length; p++)
						asOnePartFull.append(parts[p]);
					
					int shift = (asOnePart.toString().equals(asOnePartFull.toString()) ? 1 : 2);
					String[] newSortedCorrections = new String[sortedCorrections.length + shift];
					if (shift == 1)
						newSortedCorrections[0] = adjustCapitalization(asOnePart.toString(), parts[0]);
					else {
						newSortedCorrections[0] = asOnePartFull.toString();
						newSortedCorrections[1] = adjustCapitalization(asOnePart.toString(), parts[0]);
					}
					System.arraycopy(sortedCorrections, 0, newSortedCorrections, shift, sortedCorrections.length);
					sortedCorrections = newSortedCorrections;
					System.out.println("Got high comma removal correction(s) for '" + data.valueAt(v) + "': " + newSortedCorrections[shift-1] + ((shift == 1) ? "" : (", " + asOnePartFull.toString())));
				}
				
				//	add corrections
				uw.setAttribute(CORRECTIONS_ATTRIBUTE, sortedCorrections);
				
				
				//	try concatenation only for clean values
				if (value.matches("[a-z]++")) {
					int minConcatStart = v;
					int maxConcatEnd = v;
					ArrayList concatList = new ArrayList();
					
					//	test concatenating from 2 to maximumSplitParts tokens
					for (int p = 2; p <= this.maximumSplitParts; p++) {
						
						//	start left of current value and move rigth
						for (int s = Math.max(0, (v + 1 - p)); s <= v; s++) {
							
							//	concatinate tokens
							StringBuffer sb = new StringBuffer();
							for (int t = s; t < Math.min(data.size(), (s + p)); t++)
								sb.append(data.valueAt(t));
							
							//	do lookup, remember in case of hit
							if (isContained(sb.toString(), dictionaries)) {
								minConcatStart = Math.min(minConcatStart, s);
								maxConcatEnd = Math.max(maxConcatEnd, (s+p));
								concatList.add(Gamta.newAnnotation(data, MISSPELLING_ANNOTATION_TYPE, s, p));
							}
							
							//	check if concatenation appears somewhere else in document
							else if (words.containsIgnoreCase(sb.toString())) {
								minConcatStart = Math.min(minConcatStart, s);
								maxConcatEnd = Math.max(maxConcatEnd, (s+p));
								concatList.add(Gamta.newAnnotation(data, MISSPELLING_ANNOTATION_TYPE, s, p));
							}
						}
					}
					
					//	remove initial misspelling if concatenation successful
					if (concatList.size() != 0) {
						
						//	remove initial misspelling if concatenation successful
						data.removeAnnotation(uw);
						
						//	assemble possible corrections
						StringVector concatCorrections = new StringVector();
						for (int c = 0; c < concatList.size(); c++) {
							Annotation cc = ((Annotation) concatList.get(c));
							StringBuffer prefix = new StringBuffer();
							for (int t = minConcatStart; t < cc.getStartIndex(); t++) {
								prefix.append(data.valueAt(t));
								if (Gamta.insertSpace(data.valueAt(t), data.valueAt(t+1)))
									prefix.append(" ");
							}
							StringBuffer suffix = new StringBuffer();
							for (int t = cc.getEndIndex(); t < maxConcatEnd; t++) {
								if (Gamta.insertSpace(data.valueAt(t-1), data.valueAt(t)))
									suffix.append(" ");
								suffix.append(data.valueAt(t));
							}
							
							StringBuffer sb = new StringBuffer();
							for (int t = cc.getStartIndex(); t < cc.getEndIndex(); t++)
								sb.append(data.valueAt(t));
							
							concatCorrections.addElementIgnoreDuplicates(prefix.toString() + sb.toString() + suffix.toString());
							System.out.println("Got concatenation correction for '" + value + "': " + prefix.toString() + sb.toString() + suffix.toString());
							concatCorrections.addElementIgnoreDuplicates(prefix.toString() + adjustCapitalization(sb.toString(), cc.firstValue()) + suffix.toString());
						}
						
						//	annotate outer limit of possible concatenations
						Annotation cc = data.addAnnotation(MISSPELLING_ANNOTATION_TYPE, minConcatStart, (maxConcatEnd - minConcatStart));
						cc.setAttribute(CORRECTIONS_ATTRIBUTE, concatCorrections.toStringArray());
						cc.setAttribute(MISSPELLING_TYPE_ATTRIBUTE, "split");
					}
				}
			}
		}
		
		
		//	==== Step 7: Spelling Correction ====
		
		//	remove misspellings that lie inside skip annotations
		skipAnnotationIndex = 0;
		skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
		Annotation[] misspellings = data.getAnnotations(MISSPELLING_ANNOTATION_TYPE);
		for (int m = 0; m < misspellings.length; m++) {
			boolean skipMisspelling = true;
			
			//	no skip annotation, or before current skip annotation
			if ((skipAnnotation == null) || (misspellings[m].getStartIndex() < skipAnnotation.getStartIndex()))
				skipMisspelling = false;
			
			//	after current skip annotation, move to next
			else if (misspellings[m].getStartIndex() >= skipAnnotation.getEndIndex()) {
				
				//	find next relevant skip annotation (they might be nested)
				while ((skipAnnotationIndex < skipAnnotations.length) && (skipAnnotations[skipAnnotationIndex].getEndIndex() <= misspellings[m].getStartIndex()))
					skipAnnotationIndex++;
				
				//	get first matching skip annotation (if any)
				skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
				
				//	no skip annotation found, or before its start
				if ((skipAnnotation == null) || (misspellings[m].getStartIndex() < skipAnnotation.getStartIndex()))
					skipMisspelling = false;
			}
			
			//	skip this one
			if (skipMisspelling)
				data.removeAnnotation(misspellings[m]);
		}
		
		//	get feedback if allowed to
		if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
			
			//	add page numbers to misspellings
			MutableAnnotation[] paragraphs = data.getMutableAnnotations(PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
				if (pageNumber != null) {
					Annotation[] paragraphMisspellings = paragraphs[p].getAnnotations(MISSPELLING_ANNOTATION_TYPE);
					for (int m = 0; m < paragraphMisspellings.length; m++)
						paragraphMisspellings[m].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
				}
			}
			StringVector keptWords = new StringVector();
			StringVector languages = new StringVector();
			
			//	get feedback for all paragraphs simultaneously
			if (FeedbackPanel.isMultiFeedbackEnabled()) {
				Annotation[][] paragraphMisspellings = new Annotation[paragraphs.length][];
				SpellCheckingPanel[] scps = new SpellCheckingPanel[paragraphs.length];
				
				//	build feedback panels
				ArrayList scpList = new ArrayList();
				for (int p = 0; p < paragraphs.length; p++) {
					paragraphMisspellings[p] = paragraphs[p].getAnnotations(MISSPELLING_ANNOTATION_TYPE);
					scps[p] = buildSpellCheckingPanel(paragraphs[p], paragraphMisspellings[p], mainLanguage, keptWords);
					if (scps[p] != null) {
						Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
						scps[p].setLabel("Please check the spelling of the highlighted words in the paragraph below (" + ((pageNumber == null) ? "unknown page" : ("page " + pageNumber)) + ").");
						scpList.add(scps[p]);
					}
				}
				
				//	get feedback
				FeedbackPanel.getMultiFeedback(((SpellCheckingPanel[]) scpList.toArray(new SpellCheckingPanel[scpList.size()])));
				
				//	process feedback for individual paragraphs
				for (int p = 0; p < paragraphs.length; p++) {
					if (scps[p] != null)
						languages.addElement(scps[p].mainLanguage);
					
					//	process individual misspellings
					for (int m = 0; m < paragraphMisspellings[p].length; m++) {
						Annotation misspelling = paragraphMisspellings[p][m];
						CorrectionData cd = scps[p].getCorrectionData(misspelling.getAnnotationID());
						
						//	apply correction (if any)
						if (misspelling.getValue().equals(cd.correction))
							keptWords.addElement(misspelling.getValue());
						else paragraphs[p].setChars(cd.correction, misspelling.getStartOffset(), misspelling.length());
						
						//	learn
						if (!SpellCheckingPanel.doNotRemember.equals(cd.rememberLanguage)) {
							String correction = cd.correction.toLowerCase();
							
							//	learnable word
							if (correction.matches("[a-z]++")) {
								
								//	domain specific term
								if (SpellCheckingPanel.rememberDomainSpecific.equals(cd.rememberLanguage))
									this.sharedDictionary.addElement(correction, scps[p].mainLanguage);
								
								//	language specific dictionary
								else {
									String language = (SpellCheckingPanel.rememberMainLanguage.equals(cd.rememberLanguage) ? scps[p].mainLanguage : cd.rememberLanguage);
									LanguageDictionary ld = ((LanguageDictionary) this.languageDictionaries.get(language));
									if (ld == null) {
										ld = new LanguageDictionary(language);
										this.languageDictionaries.put(ld.language, ld);
										this.useLanguageDictionaries.addElementIgnoreDuplicates(ld.language);
									}
									ld.addElement(correction);
								}
							}
						}
						
						//	remove misspellings as dealt with
						paragraphs[p].removeAnnotation(misspelling);
					}
				}
			}
			
			//	get feedback paragraph by paragraph
			else {
				
				//	count paragraphs with misspellings (for dialog title)
				int misspellingParagraphs = 0;
				for (int p = 0; p < paragraphs.length; p++) {
					if (paragraphs[p].getAnnotations(SpellChecking.MISSPELLING_ANNOTATION_TYPE).length != 0)
						misspellingParagraphs++;
				}
				
				//	display dialogs
				for (int p = 0; p < paragraphs.length; p++) {
					
					//	produce feedback panel
					Annotation[] paragraphMisspellings = paragraphs[p].getAnnotations(MISSPELLING_ANNOTATION_TYPE);
					SpellCheckingPanel scp = buildSpellCheckingPanel(paragraphs[p], paragraphMisspellings, mainLanguage, keptWords);
					if (scp != null) {
						
						//	complete feedback panel
						Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
						scp.setLabel("Please check the spelling of the highlighted words in the paragraph below (" + ((pageNumber == null) ? "unknown page" : ("page " + pageNumber)) + ").");
						scp.addButton("OK");
						scp.addButton("Cancel");
						
						String title = scp.getTitle();
						scp.setTitle(title + " - (" + (p+1) + " of " + misspellingParagraphs + ")");
						
						scp.setTitle(title);
						
						//	get feedback
						String f = null;
//						try {
//							f = FeedbackPanelHtmlTester.testFeedbackPanel(scp, 0);
//						}
//						catch (IOException ioe) {
//							ioe.printStackTrace();
//						}
						if (f == null)
							f = scp.getFeedback();
						
						scp.setTitle(title);
						
						//	dialog cancelled, skip to end
						if ("Cancel".equals(f))
							p = paragraphs.length;
						
						//	process feedback for individual misspellings
						else {
							languages.addElement(scp.mainLanguage);
							for (int m = 0; m < paragraphMisspellings.length; m++) {
								Annotation misspelling = paragraphMisspellings[m];
								
								CorrectionData cd = scp.getCorrectionData(misspelling.getAnnotationID());
								if (cd != null) {
									
									//	apply correction (if any)
									if (misspelling.getValue().equals(cd.correction))
										keptWords.addElement(misspelling.getValue());
									else paragraphs[p].setChars(cd.correction, misspelling.getStartOffset(), misspelling.length());
									
									//	learn
									if (!SpellCheckingPanel.doNotRemember.equals(cd.rememberLanguage)) {
										String correction = cd.correction.toLowerCase();
										
										//	learnable word
										if (correction.matches("[a-z]++")) {
											
											//	domain specific term
											if (SpellCheckingPanel.rememberDomainSpecific.equals(cd.rememberLanguage))
												this.sharedDictionary.addElement(correction, scp.mainLanguage);
											
											//	language specific dictionary
											else {
												String language = (SpellCheckingPanel.rememberMainLanguage.equals(cd.rememberLanguage) ? scp.mainLanguage : cd.rememberLanguage);
												LanguageDictionary ld = ((LanguageDictionary) this.languageDictionaries.get(language));
												if (ld == null) {
													ld = new LanguageDictionary(language);
													this.languageDictionaries.put(ld.language, ld);
													this.useLanguageDictionaries.addElementIgnoreDuplicates(ld.language);
												}
												ld.addElement(correction);
											}
										}
									}
								}
								
								//	remove misspellings as dealt with
								paragraphs[p].removeAnnotation(misspelling);
							}
						}
					}
				}
			}
			
			//	compute main language from feedback if not recognized so far
			if (mainLanguage == null) {
				int maxLanguageFrequency = 0;
				for (int l = 0; l < languages.size(); l++) {
					String language = languages.get(l);
					if (languages.getElementCount(language) > maxLanguageFrequency) {
						mainLanguage = language;
						maxLanguageFrequency = languages.getElementCount(language);
					}
				}
			}
		}
		
		//	==== Step 8: Learning ====
		
		//	store words above auto-remember frequency
		for (Iterator ddit = dynamicDictionary.iterator(); ddit.hasNext();) {
			String word = ddit.next().toString().toLowerCase();
			if (word.matches("[a-z]++") && !mainDictionary.contains(word) && (words.getElementCount(word) >= this.rememberFrequencyThreshold))
				this.sharedDictionary.addElement(word, mainLanguage);
		}
	}
	
	private static boolean isContained(String word, Set[] dictionaries) {
		for (int d = 0; d < dictionaries.length; d++)
			if (dictionaries[d].contains(word)) return true;
		return false;
	}
	
	private static String[] getCorections(String unknownWord, SortedSet dict, int maxEditDistance, StringVector endings) {
		StringVector corrections = new StringVector();
		String lengthSurrogat = LanguageDictionary.lengthSurrogatKeys.get(Math.max(0, (unknownWord.length() - 1))); // try words at most one char shorter
		for (Iterator dit = dict.tailSet(lengthSurrogat).iterator(); (dit != null) && dit.hasNext();) {
			String correction = dit.next().toString();
			if (correction.length() > (unknownWord.length() + 1)) dit = null; // test if current candidate stays within length limit (break if not)
			else if (StringUtils.getLevenshteinDistance(unknownWord, correction, maxEditDistance) <= maxEditDistance) {// compute similarity to word in question
				corrections.addElementIgnoreDuplicates(correction);
				if (unknownWord.startsWith(correction) && (correction.length() > 1)) {
					endings.addElement(unknownWord.substring(correction.length()));
//					System.out.println("Found '" + unknownWord + "' consists of '" + correction + "' with ending '" + unknownWord.substring(correction.length()) + "'");
				}
			}
		}
		
		return corrections.toStringArray();
	}
	
	private static String[] splitConcatenation(String word, Set[] dictionaries, boolean firstSplitOnly, int maxParts) {
		return splitConcatenation(word, dictionaries, firstSplitOnly, maxParts, 1);
	}
	
	private static String[] splitConcatenation(String word, Set[] dictionaries, boolean firstSplitOnly, int maxParts, int parts) {
		StringVector splits = new StringVector();
		
		//	lookup word in dictionaries (not in first level)
		if ((parts != 0) && isContained(word, dictionaries)) {
			splits.addElement(word);
			if (firstSplitOnly) return splits.toStringArray();
		}
		
		//	check if further split allowed
		else if (parts >= maxParts) return splits.toStringArray();
		
		//	try splitting word
		for (int o = 1; o < (word.length() - 1); o++) {
			String prefix = word.substring(0, o);
			
			//	got prefix that is a word
			if (isContained(prefix, dictionaries)) {
				
				//	check remainder of word
				String[] suffixes = splitConcatenation(word.substring(o), dictionaries, firstSplitOnly, maxParts, (parts + 1));
				
				//	produce complete splits
				for (int s = 0; s < suffixes.length; s++)
					splits.addElement(prefix + " " + suffixes[s]);
				
				//	got first split
				if (firstSplitOnly && (splits.size() != 0))
					return splits.toStringArray();
			}
		}
		
		//	no possible split found
		return splits.toStringArray();
	}
	
	private static class LanguageDictionary {
		static final StringVector lengthSurrogatKeys = new StringVector();
		final String language;
		SortedSet staticDictionary = new TreeSet(lengthFirstComparator);
		SortedSet customDictionary = new TreeSet(lengthFirstComparator);
		boolean customDictionaryDirty = false;
		LanguageDictionary(String language) {
			this.language = language;
		}
		void addElement(String element) {
			element = element.toLowerCase();
			if (!this.staticDictionary.contains(element))
				this.customDictionaryDirty = (this.customDictionary.add(element) || this.customDictionaryDirty);
		}
		boolean contains(String element) {
			element = element.toLowerCase();
			return (this.staticDictionary.contains(element) || this.customDictionary.contains(element));
		}
	}
	
	private static class SharedDictionary extends LanguageDictionary {
		HashMap languageSets = new HashMap();
		boolean languageSetsDirty = false;
		SharedDictionary() {
			super(SHARED_DICTIONARY_LANGUAGE); // this is NOT the name of an actual language, so no ambiguity to expect
		}
		void addElement(String element) {
			this.addElement(element, null);
		}
		void addElement(String element, String language) {
			element = element.toLowerCase();
			super.addElement(element);
			if (language != null) {
				Set languageSet = ((Set) this.languageSets.get(element));
				if (languageSet == null) {
					languageSet = new TreeSet();
					this.languageSets.put(element, languageSet);
				}
				this.languageSetsDirty = (languageSet.add(language) || this.languageSetsDirty);
			}
		}
	}
	
	//	order by length first so finding suggestions is faster
	private static final Comparator lengthFirstComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			String s1 = ((String) o1);
			String s2 = ((String) o2);
			int c = s1.length() - s2.length();
			return ((c == 0) ? s1.compareTo(s2) : c);
		}
	};
	
	private LanguageDictionary getMainDictionary(StringVector words, StringVector distinctWords) {
		int maxScore = 0;
		LanguageDictionary maxScoreDictionary = null;
		for (int d = 0; d < this.useLanguageDictionaries.size(); d++) {
			LanguageDictionary ld = ((LanguageDictionary) this.languageDictionaries.get(this.useLanguageDictionaries.get(d)));
			System.out.print("Testing " + ld.language + " dictionary ...");
			int score = 0;
			for (int w = 0; w < distinctWords.size(); w++) {
				String word = distinctWords.get(w);
				if (ld.contains(word))
					score += words.getElementCount(word);
				else if (this.sharedDictionary.languageSets.containsKey(word) && ((Set) this.sharedDictionary.languageSets.get(word)).contains(ld.language))
					score += (words.getElementCount(word) / 2);
			}
			System.out.println(" " + ((score * 100) / words.size()));
			if (score > maxScore) {
				maxScore = score;
				maxScoreDictionary = ld;
			}
		}
		maxScore = ((maxScore * 100) / words.size());
		return ((maxScore < this.languageMatchThreshold) ? null : maxScoreDictionary);
	}
	
	//	correct the capitalization of a word
	private static String adjustCapitalization(String word, String modelWord) {
		
		//	empty or one-lettered words need no correction
		if ((word == null) || (word.length() < 2)) return word;
		
		//	use word itself as model if no model given
		else if (modelWord == null)	modelWord = word;
		
		//	gather data
		int upperCase = 0;
		int lowerCase = 0;
		for (int c = 0; c < modelWord.length(); c++) {
			if (StringUtils.UPPER_CASE_LETTERS.indexOf(modelWord.charAt(c)) != -1) upperCase++;
			else lowerCase ++;
		}
		
		//	build word
		if (upperCase > lowerCase)
			return word.toUpperCase();
		else if (StringUtils.UPPER_CASE_LETTERS.indexOf(modelWord.charAt(0)) != -1)
			return (word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
		else return word.toLowerCase();
	}
	
	private static SpellCheckingPanel buildSpellCheckingPanel(MutableAnnotation paragraph, Annotation[] misspellings, String mainLanguage, StringVector keptWords) {
		if (misspellings.length == 0) return null;
		
		SpellCheckingPanel scp = new SpellCheckingPanel();
		scp.setMainLanguage(mainLanguage);
		
		//	add backgroung information
		scp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, paragraph.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
		scp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
		scp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, paragraph.getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
		scp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, paragraph.getAnnotationID());
		scp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, MISSPELLING_ANNOTATION_TYPE);
		
		//	add target page numbers
		scp.setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, ((String) paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
		scp.setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, ((String) paragraph.getAttribute(PAGE_ID_ATTRIBUTE)));
		
		int misspellingIndex = 0;
		for (int t = 0; t < paragraph.size(); t++) {
			
			//	forward to next misspelling
			while ((misspellingIndex < misspellings.length) && (misspellings[misspellingIndex].getStartIndex() < t))
				misspellingIndex++;
			
			//	if current token starts a misspelling, facilitate corrections
			if ((misspellingIndex < misspellings.length) && (misspellings[misspellingIndex].getStartIndex() == t) && !keptWords.contains(misspellings[misspellingIndex].getValue())) {
				
				//	assemble misspelling, might be more than one token
				String misspelling = paragraph.valueAt(t);
				while ((t+1) < misspellings[misspellingIndex].getEndIndex()) {
					t++;
					if (Gamta.insertSpace(paragraph.valueAt(t-1), paragraph.valueAt(t)))
						misspelling += " ";
					misspelling += paragraph.valueAt(t);
				}
				
				//	get suggestions
				String[] suggestions = ((String[]) misspellings[misspellingIndex].getAttribute(CORRECTIONS_ATTRIBUTE));
				if (suggestions == null) suggestions = new String[0];
				
				//	add misspelling
				scp.addMisspelling(misspellings[misspellingIndex].getAnnotationID(), misspelling, suggestions);
			}
			
			//	regular (correctly spelled) token
			else {
				String token = paragraph.valueAt(t);
				while (((t+1) < ((misspellingIndex < misspellings.length) ? misspellings[misspellingIndex].getStartIndex() : paragraph.size())) && !Gamta.insertSpace(paragraph.valueAt(t), paragraph.valueAt(t+1))) {
					t++;
					token += paragraph.valueAt(t);
				}
				scp.addToken(token);
			}
		}
		
		return (scp.misspellingList.isEmpty() ? null : scp);
	}
	
	/**
	 * Feedback panel for manual corrections. has to be public in order to be
	 * able to be class loaded.
	 * 
	 * @author sautter
	 */
	public static class SpellCheckingPanel extends FeedbackPanel {
		
		static Font font = new Font("Verdana", Font.PLAIN, 12);
		
		static class CorrectionData {
			String correction;
			String rememberLanguage;
			CorrectionData(String correction, String rememberLanguage) {
				this.correction = correction;
				this.rememberLanguage = rememberLanguage;
			}
		}
		
		static final String doNotRemember = "X";
		static final String rememberMainLanguage = "M";
		static final String rememberDomainSpecific = "D";
		
		static final String defaultMainLanguage = "English";
		static final String[] allLanguages = {
			"Albanian",
			"Arabic",
			"Belarusian",
			"Bulgarian",
			"Catalan",
			"Chinese",
			"Croatian",
			"Czech",
			"Danish",
			"Dutch",
			"English",
			"Estonian",
			"Finnish",
			"French",
			"German",
			"Greek",
			"Hebrew",
			"Hindi",
			"Hungarian",
			"Icelandic",
			"Italian",
			"Japanese",
			"Korean",
			"Latvian",
			"Lithuanian",
			"Macedonian",
			"Norwegian",
			"Polish",
			"Portuguese",
			"Romanian",
			"Russian",
			"Slovak",
			"Slovenian",
			"Spanish",
			"Swedish",
			"Thai",
			"Turkish",
			"Ukrainian",
			"Vietnamese",
		};
		
		String mainLanguage; 
		JComboBox mainLanguageSelector = new JComboBox(allLanguages);
		JCheckBox needFullTextEdit = new JCheckBox("");
		
		CorrectionPanel cp = new CorrectionPanel();
		
		ArrayList tokenList = new ArrayList();
		ArrayList misspellingList = new ArrayList();
		HashMap misspellingsById = new HashMap();
		
		JPanel tokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		int lineHeight = 0;
		
		JPanel functionPanel = new JPanel(new BorderLayout());
		JPanel tokenFramePanel = new JPanel(new BorderLayout());
		
		SpellCheckingPanel() {
			super("Check Spelling");
			
			//	initialize language selector components
			this.mainLanguageSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					mainLanguage = mainLanguageSelector.getSelectedItem().toString();
					cp.rememberM.setText("<HTML>Learn as word in <B>main language</B> (" + mainLanguage + ")</HTML>");
					for (int t = 0; t < tokenList.size(); t++) {
						TokenLabel tl = ((TokenLabel) tokenList.get(t));
						if (tl instanceof MisspellingLabel)
							((MisspellingLabel) tl).updateText();
					}
				}
			});
			this.cp.rememberLanguages.setSelectedItem(this.mainLanguage);
			
			//	create token panel and make labels float
			this.tokenPanel.setBackground(Color.WHITE);
			this.tokenPanel.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					Dimension size = tokenPanel.getSize();
					Dimension prefSize = computeTokenPanelSize(size.width);
					if (size.height != prefSize.height) {
						tokenPanel.setPreferredSize(prefSize);
						tokenPanel.setMinimumSize(prefSize);
						tokenPanel.updateUI(); // not elegant, but the only way to push the resizing through
					}
				}
			});
			
			
			//	initialize functions
			JPanel mainLanguagePanel = new JPanel(new BorderLayout());
			mainLanguagePanel.add(new JLabel("This paragraph is (mostly) written in "), BorderLayout.CENTER);
			mainLanguagePanel.add(this.mainLanguageSelector, BorderLayout.EAST);
			
			JPanel freeTextEditPanel = new JPanel(new BorderLayout());
			freeTextEditPanel.add(new JLabel("This paragraph requires full text editing "), BorderLayout.CENTER);
			freeTextEditPanel.add(this.needFullTextEdit, BorderLayout.EAST);
			
			this.functionPanel.add(mainLanguagePanel, BorderLayout.WEST);
			this.functionPanel.add(freeTextEditPanel, BorderLayout.EAST);
			
			//	put token panel in border
			this.tokenFramePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			this.tokenFramePanel.add(this.tokenPanel, BorderLayout.CENTER);
			
			//	put the whole stuff together
			this.add(this.functionPanel, BorderLayout.NORTH);
			this.add(this.tokenFramePanel, BorderLayout.CENTER);
		}
		
		Dimension computeTokenPanelSize(int width) {
			int rows = 1;
			int rowLength = 0;
			for (int t = 0; t < this.tokenList.size(); t++) {
				TokenLabel tl = ((TokenLabel) this.tokenList.get(t));
				int tlWidth = (tl.getPreferredSize().width * ((tl instanceof MisspellingLabel) ? 2 : 1));
				if ((rowLength + tlWidth) <= (width - 6))
					rowLength += tlWidth;
				else {
					rows++;
					rowLength = tlWidth;
				}
			}
			int height = (this.lineHeight * rows) + 6;
			return new Dimension(width, height);
		}
		
		void setMainLanguage(String mainLanguage) {
			this.mainLanguage = ((mainLanguage == null) ? defaultMainLanguage : mainLanguage);
			this.mainLanguageSelector.setSelectedItem(this.mainLanguage);
			this.cp.rememberLanguages.setSelectedItem(this.mainLanguage);
		}
		
		void addToken(String token) {
			TokenLabel tl = new TokenLabel(token);
			
			//	add space if required
			if (this.tokenList.size() != 0) {
				TokenLabel previousToken = ((TokenLabel) this.tokenList.get(this.tokenList.size() - 1));
				if (Gamta.insertSpace(previousToken.tokens.lastValue(), tl.tokens.firstValue()))
					previousToken.addSpace();
			}
			
			//	gather layout information
			this.lineHeight = Math.max(this.lineHeight, tl.getPreferredSize().height);
			this.tokenPanel.add(tl);
			this.tokenList.add(tl);
		}
		
		void addMisspelling(String misspellingId, String misspelling, String[] suggestions) {
			final MisspellingLabel ml = new MisspellingLabel(misspellingId, misspelling, suggestions);
			
			ml.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					cp.showFor(ml);
				}
			});
			
			this.misspellingList.add(ml);
			this.misspellingsById.put(ml.misspellingId, ml);
			
			//	add space if required
			if (this.tokenList.size() != 0) {
				TokenLabel previousToken = ((TokenLabel) this.tokenList.get(this.tokenList.size() - 1));
				if (Gamta.insertSpace(previousToken.tokens.lastValue(), ml.tokens.firstValue()))
					previousToken.addSpace();
			}
			
			//	gather layout information
			this.lineHeight = Math.max(lineHeight, ml.getPreferredSize().height);
			this.tokenPanel.add(ml);
			this.tokenList.add(ml);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return (this.misspellingsById.size() * 5);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return 5; // TODO: adjust this ballpark figure
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.misspellingsById.size();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			super.writeData(bw);
			
			//	write global data
			bw.write(this.mainLanguage);
			bw.newLine();
			bw.write("" + this.needFullTextEdit.isSelected());
			bw.newLine();
			
			//	write tokens
			for (int t = 0; t < this.tokenList.size(); t++) {
				TokenLabel tl = ((TokenLabel) this.tokenList.get(t));
				if (tl instanceof MisspellingLabel) {
					MisspellingLabel ml = ((MisspellingLabel) tl);
					bw.write(ml.misspellingId);
					bw.write(" " + URLEncoder.encode(ml.value, "UTF-8"));
					bw.write(" " + URLEncoder.encode(ml.correction, "UTF-8"));
					bw.write(" " + ml.rememberLanguage);
					StringBuffer suggestions = new StringBuffer();
					for (int s = 0; s < ml.suggestions.length; s++) {
						if (s != 0) suggestions.append(" ");
						suggestions.append(URLEncoder.encode(ml.suggestions[s], "UTF-8"));
					}
					bw.write(" " + URLEncoder.encode(suggestions.toString(), "UTF-8"));
					bw.newLine();
				}
				else {
					bw.write("T " + URLEncoder.encode(tl.value, "UTF-8"));
					bw.newLine();
				}
			}
			bw.newLine();
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			super.initFields(br);
			
			//	read gloabl data
			this.setMainLanguage(br.readLine());
			this.needFullTextEdit.setSelected(Boolean.parseBoolean(br.readLine()));
			
			//	read tokens / token states
			Iterator tokenIterator = this.tokenList.iterator();
			String tokenData;
			while (((tokenData = br.readLine()) != null) && (tokenData.length() != 0)) {
				
				//	regular token
				if (tokenData.startsWith("T ")) {
					
					//	some tokens left, simply leave them alone
					if ((tokenIterator != null) && tokenIterator.hasNext())
						tokenIterator.next();
					
					//	add token
					else {
						tokenIterator = null;
						this.addToken(URLDecoder.decode(tokenData.substring(2), "UTF-8"));
					}
				}
				
				//	misspelling
				else {
					String[] misspellingData = tokenData.split("\\s");
					
					//	some tokens left, simply leave them alone
					if ((tokenIterator != null) && tokenIterator.hasNext()) {
						MisspellingLabel ml = ((MisspellingLabel) tokenIterator.next());
						ml.setCorrection(URLDecoder.decode(misspellingData[2], "UTF-8"));
						ml.setRememberLanguage(misspellingData[3]);
					}
					
					//	add token
					else {
						tokenIterator = null;
						String[] suggestions = URLDecoder.decode(misspellingData[4], "UTF-8").split("\\s");
						for (int s = 0; s < suggestions.length; s++)
							suggestions[s] = URLDecoder.decode(suggestions[s], "UTF-8");
						this.addMisspelling(misspellingData[0], URLDecoder.decode(misspellingData[1], "UTF-8"), suggestions);
					}
				}
			}
		}
		
		CorrectionData getCorrectionData(String misspellingId) {
			MisspellingLabel ml = ((MisspellingLabel) this.misspellingsById.get(misspellingId));
			return ((ml == null) ? null : new CorrectionData(ml.correction, ml.rememberLanguage));
		}
		
		class TokenLabel extends JLabel {
			String value;
			TokenSequence tokens;
			Border spaceBorder = null;
			TokenLabel(String text) {
				super(text, CENTER);
				
				this.value = text;
				this.tokens = Gamta.newTokenSequence(this.value, null);
				
				this.setBorder(null);
				this.setOpaque(true);
				this.setFont(font);
				this.setBackground(Color.WHITE);
				this.setBorder(BorderFactory.createMatteBorder(5, 0, 5, 0, Color.WHITE));
			}
			
			void addSpace() {
				if (this.spaceBorder == null) {
					this.spaceBorder = BorderFactory.createMatteBorder(0, 0, 0, 8, Color.WHITE);
					this.setBorder(this.getBorder());
				}
			}
			
			boolean hasSpace() {
				return (this.spaceBorder != null);
			}
			
			public void setBorder(Border border) {
				if (this.spaceBorder != null)
					border = BorderFactory.createCompoundBorder(this.spaceBorder, border);
				super.setBorder(border);
			}
		}
		
		class MisspellingLabel extends TokenLabel {
			String misspellingId;
			String correction;
			String[] suggestions;
			String rememberLanguage = doNotRemember;
			MisspellingLabel(String misspellingId, String text, String[] suggestions) {
				super(text);
				this.misspellingId = misspellingId;
				
				this.setCorrection(text);
				this.suggestions = suggestions;
				
				this.setBorder(null);
				this.setOpaque(true);
				this.setFont(font);
				this.setBackground(Color.RED);
			}
			
			void setCorrection(String correction) {
				this.correction = correction;
				this.updateText();
			}
			
			void setRememberLanguage(String rl) {
				this.rememberLanguage = rl;
				this.updateText();
			}
			
			void updateText() {
				if (this.value.equals(this.correction))
					this.setText("<HTML><B>" + this.value + "</B>" + this.getRemember() + "</HTML>");
				else this.setText("<HTML><STRIKE>" + this.value + "</STRIKE> <B>" + this.correction + "</B>" + this.getRemember() + "</HTML>");
			}
			
			private String getRemember() {
				if (doNotRemember.equals(this.rememberLanguage))
					return "";
				else if (rememberMainLanguage.equals(this.rememberLanguage))
					return (" <SUP>(" + mainLanguage + ")</SUP>");
				else if (rememberDomainSpecific.equals(this.rememberLanguage))
					return (" <SUP>Dom. Spec.</SUP>");
				else return (" <SUP>" + this.rememberLanguage + "</SUP>");
			}
		}
		
		class CorrectionPanel extends JPanel {
			MisspellingLabel target;
			
			JLabel close = new JLabel("close", JLabel.RIGHT);
			
			JComboBox correction = new JComboBox();
			
			JRadioButton rememberX = new JRadioButton("<HTML><B>Do not learn</B> this word or its correction</HTML>", true);
			JRadioButton rememberM = new JRadioButton("<HTML>Learn as word in <B>main language</B> (English)</HTML>", false);
			JRadioButton rememberD = new JRadioButton("<HTML>Learn as <B>domain specific term</B></HTML>", false);
			JRadioButton rememberL = new JRadioButton("<HTML>Learn as word in</HTML>", false);
			JComboBox rememberLanguages = new JComboBox(allLanguages);
			
			JDialog dialog = null;
			
			CorrectionPanel() {
				super(new GridBagLayout(), true);
				
				this.correction.setEditable(true);
				this.correction.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (correction.getSelectedItem() != null)
							correctionChanged(correction.getSelectedItem().toString());
					}
				});
				final JTextComponent correctionEditor = ((JTextComponent) this.correction.getEditor().getEditorComponent());
				correctionEditor.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent ke) {
						correctionChanged(correctionEditor.getText());
					}
				});
				
				ButtonGroup rememberButtonGroup = new ButtonGroup();
				rememberButtonGroup.add(this.rememberX);
				this.rememberX.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberX.isSelected())
							target.setRememberLanguage(doNotRemember);
					}
				});
				rememberButtonGroup.add(this.rememberM);
				this.rememberM.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberM.isSelected())
							target.setRememberLanguage(rememberMainLanguage);
					}
				});
				rememberButtonGroup.add(this.rememberD);
				this.rememberD.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberD.isSelected())
							target.setRememberLanguage(rememberDomainSpecific);
					}
				});
				rememberButtonGroup.add(this.rememberL);
				this.rememberL.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberL.isSelected())
							target.setRememberLanguage(rememberLanguages.getSelectedItem().toString());
						rememberLanguages.setEnabled(rememberL.isSelected());
					}
				});
				this.rememberLanguages.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberL.isSelected())
							target.setRememberLanguage(rememberLanguages.getSelectedItem().toString());
					}
				});
				
				this.close.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (dialog != null)
							dialog.dispose();
						if (target != null)
							target.setBorder(null);
					}
				});
				
				this.rememberLanguages.setEnabled(false);
				
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridy = 0;
				gbc.weighty = 0;
				gbc.gridheight = 1;
				
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				this.add(this.correction, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				this.add(new JPanel(), gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.add(this.close, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 3;
				this.add(this.rememberX, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 3;
				this.add(this.rememberM, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 3;
				this.add(this.rememberD, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				this.add(this.rememberL, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.add(this.rememberLanguages, gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 1;
				this.add(new JPanel(), gbc.clone());
				gbc.gridy++;
				
				this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.getBackground(), 5)));
			}
			
			void correctionChanged(String correction) {
				this.target.setCorrection(correction);
				boolean noSpace = (correction.indexOf(' ') == -1);
				if (!noSpace)
					this.target.setRememberLanguage(doNotRemember);
				adjustRemember(this.target.rememberLanguage, noSpace);
			}
			
			void adjustRemember(String remember, boolean changeable) {
				if (doNotRemember.equals(remember))
					this.rememberX.setSelected(true);
				else if (rememberMainLanguage.equals(remember))
					this.rememberM.setSelected(true);
				else if (rememberDomainSpecific.equals(remember))
					this.rememberD.setSelected(true);
				else {
					this.rememberLanguages.setSelectedItem(remember);
					this.rememberL.setSelected(true);
				}
				this.rememberX.setEnabled(changeable);
				this.rememberM.setEnabled(changeable);
				this.rememberD.setEnabled(changeable);
				this.rememberL.setEnabled(changeable);
			}
			
			void showFor(MisspellingLabel ml) {
				if (this.target != null)
					this.target.setBorder(null);
				
				this.target = ml;
				this.target.setBackground(Color.GREEN);
				this.target.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
				this.target.validate();
				
				this.correction.removeAllItems();
				this.correction.insertItemAt(this.target.value, 0);
				this.correction.setSelectedItem(this.target.correction);
				for (int s = 0; s < this.target.suggestions.length; s++)
					this.correction.addItem(this.target.suggestions[s]);
				
				this.adjustRemember(this.target.rememberLanguage, (this.target.correction.indexOf(' ') == -1));
				
				if (this.dialog != null)
					this.dialog.dispose();
				this.dialog = DialogFactory.produceDialog("", false);
				this.dialog.setUndecorated(true);
				this.dialog.getContentPane().setLayout(new BorderLayout());
				this.dialog.getContentPane().add(this, BorderLayout.CENTER);
				this.dialog.setSize(this.getPreferredSize());
				
				Point targetPos = this.target.getLocationOnScreen();
				this.dialog.setLocation(targetPos.x, (targetPos.y + this.target.getSize().height + 4));
				this.dialog.setVisible(true);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
//			bw.write("body {");
//			bw.newLine();
//			bw.write("  font-size: 10pt;");
//			bw.newLine();
//			bw.write("  font-family: Verdana, Arial, Helvetica;");
//			bw.newLine();
//			bw.write("}");
//			bw.newLine();
//			bw.write("");
//			bw.newLine();
			bw.write(".mainTable {");
			bw.newLine();
			bw.write("  border: 2pt solid #444444;");
			bw.newLine();
			bw.write("  border-collapse: collapse;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write(".mainTableCell {");
			bw.newLine();
			bw.write("  padding: 5pt;");
			bw.newLine();
			bw.write("  margin: 0pt;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".misspelling {");
			bw.newLine();
			bw.write("  white-space: nowrap;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".misspellingDisplay{}");
			bw.newLine();
			bw.write(".correctionDisplay{");
			bw.newLine();
			bw.write("  font-weight: bold;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write(".rememberDisplay{");
			bw.newLine();
			bw.write("  vertical-align: super;");
			bw.newLine();
			bw.write("  font-size: 7pt;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".rememberLabel {");
			bw.newLine();
			bw.write("  white-space: nowrap;");
			bw.newLine();
			bw.write("  font-size: 8pt;");
			bw.newLine();
			bw.write("  font-family: Verdana, Arial, Helvetica;");
			bw.newLine();
			bw.write("  text-align: left;");
			bw.newLine();
			bw.write("  vertical-align: middle;");
			bw.newLine();
			bw.write("  padding: 0px;");
			bw.newLine();
			bw.write("  margin: 0px;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".closeLink {");
			bw.newLine();
			bw.write("  font-size: 8pt;");
			bw.newLine();
			bw.write("  font-family: Verdana, Arial, Helvetica;");
			bw.newLine();
			bw.write("  color: 000;");
			bw.newLine();
			bw.write("  text-decoration: none;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			for (int misspellingHtmlId = 0; misspellingHtmlId < this.misspellingList.size(); misspellingHtmlId++) {
				MisspellingLabel ml = ((MisspellingLabel) this.misspellingList.get(misspellingHtmlId));
				bw.write("  _originalValues['" + misspellingHtmlId + "'] = '" + ml.value.replaceAll("\\'", "\\\\'") + "';");
				bw.newLine();
			}
			bw.write("  ");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.style.width = (_selectionWidth + 'px');");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var i = 0;");
			bw.newLine();
			bw.write("  var misspelling;");
			bw.newLine();
			bw.write("  while (misspelling = document.getElementById('misspellingDisplay' + i)) {");
			bw.newLine();
			bw.write("    misspelling.style.backgroundColor = _notCorrectedColor;");
			bw.newLine();
			bw.write("    misspelling.style.fontWeight = 'bold';");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    var correction = document.getElementById('correctionDisplay' + i);");
			bw.newLine();
			bw.write("    correction.style.backgroundColor = _correctedColor;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    var remember = document.getElementById('rememberDisplay' + i);");
			bw.newLine();
			bw.write("    remember.style.backgroundColor = _rememberColor;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    i++;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  updateMainLanguage();");
			bw.newLine();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			bw.write("var _selectionWidth = 180;");
			bw.newLine();
			bw.write("var _correctedColor = '88FF88';");
			bw.newLine();
			bw.write("var _notCorrectedColor = 'FF8888';");
			bw.newLine();
			bw.write("var _rememberColor = 'FFBB88';");
			bw.newLine();
			
			bw.write("var _originalValues = new Object();");
			bw.newLine();
			
			
			bw.write("var _mouseOverCorrection = false;");
			bw.newLine();
			bw.write("function mouseOverCorrection() {");
			bw.newLine();
			bw.write("  _mouseOverCorrection = true;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function mouseOutCorrection() {");
			bw.newLine();
			bw.write("  _mouseOverCorrection = false;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("var _editing = -1;");
			bw.newLine();
			bw.write("function editMisspelling(i) {");
			bw.newLine();
			bw.write("  if ((i == -1) && _mouseOverCorrection) return; // click in correction menu");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var correction = document.getElementById('correction');");
			bw.newLine();
			bw.write("  correction.style.display = 'none';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (i == -1) return;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var oldMisspelling = document.getElementById('misspelling' + _editing);");
			bw.newLine();
			bw.write("  if (oldMisspelling)");
			bw.newLine();
			bw.write("    oldMisspelling .style.border = '';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  _editing = i;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  //  update correction box");
			bw.newLine();
			bw.write("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  while(selBox.options.length > 2)");
			bw.newLine();
			bw.write("    selBox.options[selBox.options.length - 1] = null;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var optionHolder;");
			bw.newLine();
			bw.write("  var o = 0;");
			bw.newLine();
			bw.write("  var unSelected = true;");
			bw.newLine();
			bw.write("  while (optionHolder = document.getElementById('option' + _editing + '_' + o++)) {");
			bw.newLine();
			bw.write("    selBox.options[selBox.options.length] = new Option(optionHolder.value, optionHolder.value, false, false);");
			bw.newLine();
			bw.write("    if (correctionStore.value == optionHolder.value) {");
			bw.newLine();
			bw.write("      selBox.options[selBox.options.length - 1].selected = true;");
			bw.newLine();
			bw.write("      unSelected = false;");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var originalText = document.getElementById('originalText');");
			bw.newLine();
			bw.write("  originalText.value = _originalValues[_editing];");
			bw.newLine();
			bw.write("  originalText.text = ('<keep \\'' + _originalValues[_editing] + '\\'>');");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var freeText = document.getElementById('freeText');");
			bw.newLine();
			bw.write("  freeText.text = correctionStore.value;");
			bw.newLine();
			bw.write("  freeText.value = correctionStore.value;");
			bw.newLine();
			bw.write("  freeText.selected = unSelected;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.value = document.getElementById('correction' + _editing).value;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  // update remember data");
			bw.newLine();
			bw.write("  updateRemember();");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  // highlight current correction");
			bw.newLine();
			bw.write("  var misspelling = document.getElementById('misspelling' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var pos = getMenuPosition(misspelling);");
			bw.newLine();
			bw.write("  var pageWidth = document.body.clientWidth;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  correction.style.left = pos.x + 'px';");
			bw.newLine();
			bw.write("  correction.style.top = (pos.y + 2) + 'px';");
			bw.newLine();
			bw.write("  correction.style.display = 'block';");
			bw.newLine();
			bw.write("  if ((pos.x + correction.offsetWidth) > pageWidth)");
			bw.newLine();
			bw.write("    correction.style.left = (pageWidth - correction.offsetWidth) + 'px';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  misspelling.style.border = '2pt solid #FF0000';");
			bw.newLine();
			bw.write("  document.getElementById('misspellingDisplay' + _editing).style.backgroundColor = _correctedColor;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function getMenuPosition(el) {");
			bw.newLine();
			bw.write("  var element = el;");
			bw.newLine();
			bw.write("  var left = 0;");
			bw.newLine();
			bw.write("  var top = el.offsetHeight;");
			bw.newLine();
			bw.write("  while(element) {");
			bw.newLine();
			bw.write("    left += element.offsetLeft;");
			bw.newLine();
			bw.write("    top += element.offsetTop;");
			bw.newLine();
			bw.write("    element = element.offsetParent;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var pos = new Object();");
			bw.newLine();
			bw.write("  pos.x = left;");
			bw.newLine();
			bw.write("  pos.y = top;");
			bw.newLine();
			bw.write("  return pos;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function updateCorrection() {");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.newLine();
			bw.write("  correctionStore.value = textBox.value;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var misspelling = document.getElementById('misspellingDisplay' + _editing);");
			bw.newLine();
			bw.write("  var correction = document.getElementById('correctionDisplay' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (correctionStore.value == _originalValues[_editing]) {");
			bw.newLine();
			bw.write("    misspelling.style.textDecoration = '';");
			bw.newLine();
			bw.write("    misspelling.style.fontWeight = 'bold';");
			bw.newLine();
			bw.write("    correction.innerHTML = '';");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  else {");
			bw.newLine();
			bw.write("    misspelling.style.textDecoration = 'line-through';");
			bw.newLine();
			bw.write("    misspelling.style.fontWeight = '';");
			bw.newLine();
			bw.write("    correction.innerHTML = ('&nbsp;' + correctionStore.value);");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function closeCorrection() {");
			bw.newLine();
			bw.write("  correction.style.display = 'none';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var oldMisspelling = document.getElementById('misspelling' + _editing);");
			bw.newLine();
			bw.write("  if (oldMisspelling)");
			bw.newLine();
			bw.write("    oldMisspelling .style.border = '';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  _editing = -1;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  return false;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write("");
			bw.newLine();
			
			
			bw.write("function freeTextChanged() {");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  var freeText = document.getElementById('freeText');");
			bw.newLine();
			bw.write("  freeText.value = textBox.value;");
			bw.newLine();
			bw.write("  freeText.text = textBox.value;");
			bw.newLine();
			bw.write("  freeText.selected = true;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  updateCorrection();");
			bw.newLine();
			bw.write("  updateRemember();");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function selectionChanged() {");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  if (selBox.selectedIndex == 0) {");
			bw.newLine();
			bw.write("    var originalText = document.getElementById('originalText');");
			bw.newLine();
			bw.write("    var freeText = document.getElementById('freeText');");
			bw.newLine();
			bw.write("    freeText.value = originalText.value;");
			bw.newLine();
			bw.write("    freeText.text = originalText.value;");
			bw.newLine();
			bw.write("    freeText.selected = true;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.value = selBox.options[selBox.selectedIndex].value;");
			bw.newLine();
			bw.write("  hideSelection();");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  updateCorrection();");
			bw.newLine();
			bw.write("  updateRemember();");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			
			bw.write("var _selectionOpen = false;");
			bw.newLine();
			bw.write("function showSelection() {");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  selBox.style.width = ((_selectionWidth + 20) + 'px');");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.style.display = 'none';");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function clickSelection() {");
			bw.newLine();
			bw.write("  _selectionOpen = true;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function exitSelection() {");
			bw.newLine();
			bw.write("  if (_selectionOpen)");
			bw.newLine();
			bw.write("    _selectionOpen = false;");
			bw.newLine();
			bw.write("  else hideSelection();");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function hideSelection() {");
			bw.newLine();
			bw.write("  _selectionOpen = false;");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  selBox.style.width = '20px';");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.style.display = '';");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			
			bw.write("function updateMainLanguage() {");
			bw.newLine();
			bw.write("  var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.newLine();
			bw.write("  var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].text;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var mainLanguageDisplay = document.getElementById('mainLanguageDisplay');");
			bw.newLine();
			bw.write("  mainLanguageDisplay.innerHTML = mainLanguage;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var i = -1;");
			bw.newLine();
			bw.write("  var rememberDisplay;");
			bw.newLine();
			bw.write("  while(rememberDisplay = document.getElementById('rememberDisplay' + ++i)) {");
			bw.newLine();
			bw.write("    var rememberStore = document.getElementById('remember' + i);");
			bw.newLine();
			bw.write("    if (rememberStore.value == 'M')");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			
			bw.write("var _xmdl = 'XMDL';");
			bw.newLine();
			bw.write("function updateRemember() {");
			bw.newLine();
			bw.write("  var remember = document.getElementById('remember');");
			bw.newLine();
			bw.write("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (correctionStore.value.indexOf(' ') == -1) {");
			bw.newLine();
			bw.write("    var rememberStore = document.getElementById('remember' + _editing);");
			bw.newLine();
			bw.write("    var rs = rememberStore.value;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    for (var r = 0; r < _xmdl.length; r++) {");
			bw.newLine();
			bw.write("      var idSuffix = _xmdl.substring(r, (r+1));");
			bw.newLine();
			bw.write("      var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.newLine();
			bw.write("      rememberButton.checked = (idSuffix == rs);");
			bw.newLine();
			bw.write("      rememberButton.disabled = false;");
			bw.newLine();
			bw.write("      var rememberLabel = document.getElementById('rememberLabel' + idSuffix);");
			bw.newLine();
			bw.write("      rememberLabel.style.color = '000000';");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    if (rs.length == 1)");
			bw.newLine();
			bw.write("      rememberChanged(rs);");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    else {");
			bw.newLine();
			bw.write("      document.getElementById('rememberL').checked = true;");
			bw.newLine();
			bw.write("      var rememberLanguage = document.getElementById('rememberLanguage');");
			bw.newLine();
			bw.write("      for (var r = 0; r < rememberLanguage.options.length; r++) {");
			bw.newLine();
			bw.write("        if (rs == rememberLanguage.options[r].value)");
			bw.newLine();
			bw.write("          rememberLanguage.options[r].selected = true;");
			bw.newLine();
			bw.write("      }");
			bw.newLine();
			bw.write("      rememberChanged('L');");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  else {");
			bw.newLine();
			bw.write("    for (var r = 0; r < _xmdl.length; r++) {");
			bw.newLine();
			bw.write("      var idSuffix = _xmdl.substring(r, (r+1));");
			bw.newLine();
			bw.write("      var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.newLine();
			bw.write("      rememberButton.checked = (idSuffix == 'X');");
			bw.newLine();
			bw.write("      rememberButton.disabled = true;");
			bw.newLine();
			bw.write("      var rememberLabel = document.getElementById('rememberLabel' + idSuffix);");
			bw.newLine();
			bw.write("      rememberLabel.style.color = '666666';");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("    rememberChanged('X');");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function rememberChanged(rs) {");
			bw.newLine();
			bw.write("  var rememberStore = document.getElementById('remember' + _editing);");
			bw.newLine();
			bw.write("  var rememberDisplay = document.getElementById('rememberDisplay' + _editing);");
			bw.newLine();
			bw.write("  var rememberLanguage = document.getElementById('rememberLanguage');");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  for (var r = 0; r < _xmdl.length; r++) {");
			bw.newLine();
			bw.write("    var idSuffix = _xmdl.substring(r, (r+1));");
			bw.newLine();
			bw.write("    var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.newLine();
			bw.write("    rememberButton.checked = (idSuffix == rs);");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (rs == 'L') {");
			bw.newLine();
			bw.write("    var language = rememberLanguage.options[rememberLanguage.selectedIndex].value;");
			bw.newLine();
			bw.write("    rememberStore.value = language;");
			bw.newLine();
			bw.write("    rememberDisplay.innerHTML = ('&nbsp;' + language);");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    rememberLanguage.disabled = false;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  else {");
			bw.newLine();
			bw.write("    rememberStore.value = rs;");
			bw.newLine();
			bw.write("    rememberLanguage.disabled = true;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    if (rs == 'X')");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = '';");
			bw.newLine();
			bw.write("    else if (rs == 'D')");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = '&nbsp;Dom. Spec.';");
			bw.newLine();
			bw.write("    else if (rs == 'M') {");
			bw.newLine();
			bw.write("      var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.newLine();
			bw.write("      var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].value;");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			bw.write("<div id=\"correction\" style=\"border: 1px solid #888888; background-color: bbb; display: none; position: absolute; align: right;\" onmouseover=\"mouseOverCorrection();\" onmouseout=\"mouseOutCorrection();\">");
			bw.newLine();
			bw.write("<table style=\"border: 0;\">");
			bw.newLine();
			
			bw.write("<tr>");
			bw.newLine();
			
			bw.write("<td style=\"white-space: nowrap; text-align: left; padding: 0px 0px 8px; margin: 0px;\">");
			bw.write("<input id=\"textInput\" name=\"textInput\" onkeyup=\"freeTextChanged();\" type=\"text\" value=\"\" style=\"margin: 0px; padding 0px;\">");
			bw.write("<select id=\"textSelect\" name=\"textSelect\" onmouseover=\"showSelection();\" onclick=\"clickSelection();\" onmouseout=\"exitSelection();\" onblur=\"hideSelection();\" onchange=\"selectionChanged();\" style=\"margin: 0px; padding: 0px; width: 20px;\">");
			bw.newLine();
			bw.write("  <option value=\"\" id=\"originalText\"></option>");
			bw.newLine();
			bw.write("  <option value=\"\" id=\"freeText\"></option>");
			bw.newLine();
			bw.write("</select>");
			bw.newLine();
			bw.write("</td>");
			bw.newLine();
			
			bw.write("<td style=\"text-align: right; vertical-align: top; padding: 0px 1px 1px; margin: 0px;\">");
			bw.write("<a class=\"closeLink\" href=\"#\" onclick=\"return closeCorrection();\">close&nbsp;[X]</a>");
			bw.write("</td>");
			bw.newLine();
			
			bw.write("</tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberX\" onclick=\"rememberChanged('X');\" value=\"X\"> <span id=\"rememberLabelX\" onclick=\"rememberChanged('X');\"><b>Do not learn</b> this word or its correction</span>");
			bw.newLine();
			bw.write("</td></tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberM\" onclick=\"rememberChanged('M');\" value=\"M\"> <span id=\"rememberLabelM\" onclick=\"rememberChanged('M');\">Learn as word in <b>main language</b> (<span id=\"mainLanguageDisplay\">English</span>)</span>");
			bw.newLine();
			bw.write("</td></tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberD\" onclick=\"rememberChanged('D');\" value=\"D\"> <span id=\"rememberLabelD\" onclick=\"rememberChanged('D');\">Learn as <b>domain specific</b> term</span>");
			bw.newLine();
			bw.write("</td></tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberL\" onclick=\"rememberChanged('L');\" value=\"L\"> <span id=\"rememberLabelL\" onclick=\"rememberChanged('L');\">Learn as <select name=\"rememberLanguage\" id=\"rememberLanguage\" onchange=\"rememberChanged('L');\">");
			bw.newLine();
			for (int l = 0; l < allLanguages.length; l++) {
				bw.write("  <option value=\"" + allLanguages[l] + "\"" + (allLanguages[l].equals(this.mainLanguage) ? " selected" : "") + ">" + allLanguages[l] + "</option>");
				bw.newLine();
			}
			bw.write("</select> word</span></td></tr>");
			bw.newLine();
			bw.write("</table>");
			bw.newLine();
			bw.write("</div>");
			bw.newLine();
			
			
			bw.write("<table class=\"mainTable\">");
			bw.newLine();
			bw.write("  <tr style=\"background-color: bbb;\">");
			bw.newLine();
			bw.write("  <td style=\"font-weight: bold; font-size: 10pt;\" class=\"mainTableCell\">This paragraph is (mostly) written in " );
			bw.write("<select id=\"mainLanguage\" name=\"mainLanguage\" onchange=\"updateMainLanguage();\">");
			bw.newLine();
			for (int l = 0; l < allLanguages.length; l++) {
				bw.write("  <option value=\"" + allLanguages[l] + "\"" + (allLanguages[l].equals(this.mainLanguage) ? " selected" : "") + ">" + allLanguages[l] + "</option>");
				bw.newLine();
			}
			bw.write("  </select></td>");
			bw.newLine();
			bw.write("  <td style=\"font-weight: bold; font-size: 10pt; text-align: right;\" class=\"mainTableCell\">This paragraph requires full text editing <input type=\"checkbox\" name=\"fullTextEdit\" value=\"T\"></td>");
			bw.newLine();
			bw.write("  </tr>");
			bw.newLine();
			bw.write("  <tr>");
			bw.newLine();
			bw.write("    <td class=\"mainTableCell\" colspan=\"2\" style=\"line-height: 1.8;\">");
			
			int misspellingHtmlId = 0;
			for (Iterator tit = this.tokenList.iterator(); tit.hasNext();) {
				TokenLabel tl = ((TokenLabel) tit.next());
				
				if (tl instanceof MisspellingLabel) {
					bw.write("<span class=\"misspelling\">");
						bw.write("<span id=\"misspelling" + misspellingHtmlId + "\" onclick=\"editMisspelling(" + misspellingHtmlId + ");\">");
							bw.write("<span id=\"misspellingDisplay" + misspellingHtmlId + "\" class=\"misspellingDisplay\">" + IoTools.prepareForHtml(tl.value) + "</span>");
							bw.write("<span id=\"correctionDisplay" + misspellingHtmlId + "\" class=\"correctionDisplay\"></span>");
						bw.write("</span>");
						bw.write("<span id=\"rememberDisplay" + misspellingHtmlId + "\" class=\"rememberDisplay\"></span>");
					bw.write("</span>");
					misspellingHtmlId++;
				}
				
				else bw.write(IoTools.prepareForHtml(tl.value));
				
				if (tl.hasSpace()) bw.write(" ");
			}
			bw.write("    </td>");
			bw.newLine();
			bw.write("  </tr>");
			bw.newLine();
			bw.write("</table>");
			bw.newLine();
			
			for (misspellingHtmlId = 0; misspellingHtmlId < this.misspellingList.size(); misspellingHtmlId++) {
				MisspellingLabel ml = ((MisspellingLabel) this.misspellingList.get(misspellingHtmlId));
				bw.write("<input type=\"hidden\" id=\"remember" + misspellingHtmlId + "\" name=\"" + ml.misspellingId + "_remember\" value=\"" + ml.rememberLanguage + "\">");
				bw.newLine();
				bw.write("<input type=\"hidden\" id=\"correction" + misspellingHtmlId + "\" name=\"" + ml.misspellingId + "_correction\" value=\"" + ml.correction + "\">");
				bw.newLine();
				for (int s = 0; s < ml.suggestions.length; s++) {
					bw.write("<input type=\"hidden\" id=\"option" + misspellingHtmlId + "_" + s + "\" value=\"" + ml.suggestions[s] + "\">");
					bw.newLine();
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			this.mainLanguageSelector.setSelectedItem(response.getProperty("mainLanguage", this.mainLanguage));
			this.needFullTextEdit.setSelected("T".equals(response.getProperty("fullTextEdit", "F")));
			
			for (Iterator mit = this.misspellingsById.values().iterator(); mit.hasNext();) {
				MisspellingLabel ml = ((MisspellingLabel) mit.next());
				ml.setCorrection(response.getProperty((ml.misspellingId + "_correction"), ml.correction));
				ml.setRememberLanguage(response.getProperty((ml.misspellingId + "_remember"), ml.rememberLanguage));
			}
		}
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	read parameters
		try {
			this.acceptFrequencyThreshold = Integer.parseInt(this.getParameter("acceptFrequencyThreshold", ("" + this.acceptFrequencyThreshold)));
		} catch (NumberFormatException e) {}
		try {
			this.rememberFrequencyThreshold = Integer.parseInt(this.getParameter("rememberFrequencyThreshold", ("" + this.rememberFrequencyThreshold)));
		} catch (NumberFormatException e) {}
		try {
			this.suggestionDistanceThreshold = Integer.parseInt(this.getParameter("suggestionDistanceThreshold", ("" + this.suggestionDistanceThreshold)));
		} catch (NumberFormatException e) {}
		try {
			this.maximumSplitParts = Integer.parseInt(this.getParameter("maximumSplitParts", ("" + this.maximumSplitParts)));
		} catch (NumberFormatException e) {}
		try {
			this.languageMatchThreshold = Integer.parseInt(this.getParameter("acceptFrequencyThreshold", ("" + this.languageMatchThreshold)));
		} catch (NumberFormatException e) {}
		
//		//	load list of languages
//		try {
//			StringVector load = this.loadList("languages.txt");
//			load.sortLexicographically();
//			this.languages = load.toStringArray();
//		}
//		catch (Exception e) {
//			System.out.println(e.getClass().getName() + " while loading language list: " + e.getMessage());
//			e.printStackTrace(System.out);
//		}
		
		//	load skip regular expressions
		try {
			StringVector load = this.loadList("skipRegEx.txt");
			load.sortLexicographically();
			this.skipRegExes = load.toStringArray();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " while loading skip reg exes: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		
		//	load list of active languages
		try {
			this.useLanguageDictionaries = this.loadList("useLanguageDictionaries.cnfg");
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " while loading active language list: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		
		//	load language dictionaries
		for (int d = 0; d < this.useLanguageDictionaries.size(); d++) {
			String language = this.useLanguageDictionaries.get(d);
			LanguageDictionary languageDictionary = new LanguageDictionary(language);
			if (this.dataProvider.isDataAvailable(language + STATIC_DICTIONARY_SUFFIX)) {
				try {
					InputStream is = this.dataProvider.getInputStream(language + STATIC_DICTIONARY_SUFFIX);
					StringVector load = StringVector.loadList(is);
					is.close();
					for (int e = 0; e < load.size(); e++)
						languageDictionary.staticDictionary.add(load.get(e).toLowerCase());
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " while loading static dictionary for '" + language + "': " + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
			if (this.dataProvider.isDataAvailable(language + CUSTOM_DICTIONARY_SUFFIX)) {
				try {
					InputStream is = this.dataProvider.getInputStream(language + CUSTOM_DICTIONARY_SUFFIX);
					StringVector load = StringVector.loadList(is);
					is.close();
					for (int e = 0; e < load.size(); e++)
						languageDictionary.customDictionary.add(load.get(e).toLowerCase());
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " while loading custom dictionary for '" + language + "': " + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
			this.languageDictionaries.put(language, languageDictionary);
		}
		
		//	load shared dictionary
		this.sharedDictionary = new SharedDictionary();
		if (this.dataProvider.isDataAvailable("Shared.static.txt")) {
			try {
				InputStream is = this.dataProvider.getInputStream("Shared.static.txt");
				StringVector load = StringVector.loadList(is);
				is.close();
				for (int w = 0; w < load.size(); w++)
					this.sharedDictionary.staticDictionary.add(load.get(w).toLowerCase());
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " while loading static shared dictionary: " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
		if (this.dataProvider.isDataAvailable("Shared.custom.txt")) {
			try {
				InputStream is = this.dataProvider.getInputStream("Shared.custom.txt");
				StringVector load = StringVector.loadList(is);
				is.close();
				
				for (int l = 0; l < load.size(); l++) {
					String[] parts = load.get(l).split(",");
					if (parts.length == 1)
						this.sharedDictionary.addElement(parts[0]);
					else {
						for (int p = 1; p < parts.length; p++)
							this.sharedDictionary.addElement(parts[0], parts[p]);
						//	TODO: maybe remember count for individual languages
					}
				}
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " while loading custom shared dictionary: " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		if (!this.dataProvider.isDataEditable()) return;
		
		//	store custom language dictionaries
		for (int d = 0; d < this.useLanguageDictionaries.size(); d++) {
			LanguageDictionary languageDictionary = ((LanguageDictionary) this.languageDictionaries.get(this.useLanguageDictionaries.get(d)));
			if (languageDictionary.customDictionaryDirty) {
				StringVector store = new StringVector();
				for (Iterator it = languageDictionary.customDictionary.iterator(); it.hasNext();)
					store.addElement(it.next().toString());
				try {
					OutputStream os = this.dataProvider.getOutputStream(languageDictionary.language + CUSTOM_DICTIONARY_SUFFIX);
					store.storeContent(os);
					os.flush();
					os.close();
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " while storing custom dictionary for '" + languageDictionary.language + "': " + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
		}
		
		//	store custom shared dictionary
		if (this.sharedDictionary.languageSetsDirty || this.sharedDictionary.customDictionaryDirty) {
			StringVector store = new StringVector();
			for (Iterator it = this.sharedDictionary.customDictionary.iterator(); it.hasNext();) {
				String element = it.next().toString();
				Set languageSet = ((Set) this.sharedDictionary.languageSets.get(element));
				if (languageSet != null) {
					for (Iterator lit = languageSet.iterator(); lit.hasNext();)
						element += ("," + lit.next().toString());
				}
				store.addElement(element);
			}
			try {
				OutputStream os = this.dataProvider.getOutputStream("Shared.custom.txt");
				store.storeContent(os);
				os.flush();
				os.close();
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " while storing shared custom dictionary: " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
		
		//	store parameters maximumSplitparts
		this.storeParameter("acceptFrequencyThreshold", ("" + this.acceptFrequencyThreshold));
		this.storeParameter("rememberFrequencyThreshold", ("" + this.rememberFrequencyThreshold));
		this.storeParameter("suggestionDistanceThreshold", ("" + this.suggestionDistanceThreshold));
		this.storeParameter("maximumSplitParts", ("" + this.maximumSplitParts));
		this.storeParameter("languageMatchThreshold", ("" + this.languageMatchThreshold));
	}
	
	//	!!! for detail test purposes only !!!
	public static void main(String[] args) throws Exception {
		Set[] dicts = new Set[1];
		dicts[0] = new TreeSet(lengthFirstComparator);
		dicts[0].add("the");
		dicts[0].add("them");
		dicts[0].add("master");
		dicts[0].add("a");
		dicts[0].add("star");
		String[] splits = splitConcatenation("themaster", dicts, false, 3);
		for (int s = 0; s < splits.length; s++)
			System.out.println(splits[s]);
	}
}