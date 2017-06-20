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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
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
public class SpellChecking {
	
	//	TODO allow for one shared instance per config folder !!!
	
	private static final String SHARED_DICTIONARY_LANGUAGE = "Shared";
	private static final String STATIC_DICTIONARY_SUFFIX = ".static.txt";
	private static final String CUSTOM_DICTIONARY_SUFFIX = ".custom.txt";
	
	static final String MISSPELLING_ANNOTATION_TYPE = "misspelling";
	static final String MISSPELLING_TYPE_ATTRIBUTE = "type";
	static final String MISSPELLED_MISSPELLING_TYPE = "misspelled";
	static final String SPLIT_MISSPELLING_TYPE = "split";
	static final String OUT_OF_LANGUAGE_MISSPELLING_TYPE = "outOfLanguage";
	static final String LANGUAGE_ATTRIBUTE = "language";
	static final String CORRECTIONS_ATTRIBUTE = "corrections";
	
	static final String CORRECTED_MISSPELLING_ANNOTATION_TYPE = "correctedMisspelling";
	static final String ORIGINAL_VALUE_ATTRIBUTE = "originalValue";
	
	static int acceptFrequencyThreshold = 3;
	static int rememberFrequencyThreshold = 5;
	
	static int suggestionDistanceThreshold = 3;
	static int maximumSplitParts = 3;
	
	static String[] languages = new String[0];
	static int languageMatchThreshold = 67;
	
	private static String[] skipRegExes = new String[0];
	
	private static HashMap languageDictionaries = new HashMap();
	private static StringVector mainLanguages = new StringVector();
	static SharedDictionary sharedDictionary;
	
	static class LanguageDictionary {
		static final StringVector lengthSurrogatKeys = new StringVector();
		final String language;
		ByteDictionary staticDictionary = new ByteDictionary();
		ByteDictionary customDictionary = new ByteDictionary();
		private int cdSize = -1;
		LanguageDictionary(String language) {
			this.language = language;
		}
		void addElement(String element) {
			this.customDictionary.add(element);
		}
		boolean lookup(String element) {
			return (this.staticDictionary.lookup(element) || this.customDictionary.lookup(element));
		}
		void compile() {
			this.staticDictionary.compile();
			this.customDictionary.compile();
			if (this.cdSize == -1)
				this.cdSize = this.customDictionary.size();
		}
		boolean isDirty() {
			return (this.cdSize != this.customDictionary.size());
		}
	}
	
	static class SharedDictionary extends LanguageDictionary {
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
	static final Comparator lengthFirstComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			String s1 = ((String) o1);
			String s2 = ((String) o2);
			int c = s1.length() - s2.length();
			return ((c == 0) ? s1.compareTo(s2) : c);
		}
	};
	
	static LanguageDictionary getMainDictionary(StringVector words, StringVector distinctWords) {
		int maxScore = 0;
		LanguageDictionary maxScoreDictionary = null;
		for (int d = 0; d < mainLanguages.size(); d++) {
			LanguageDictionary ld = ((LanguageDictionary) languageDictionaries.get(mainLanguages.get(d)));
			System.out.print("Testing " + ld.language + " dictionary ...");
			int score = 0;
			for (int w = 0; w < distinctWords.size(); w++) {
				String word = distinctWords.get(w);
				if (ld.lookup(word))
					score += words.getElementCount(word);
				else if (sharedDictionary.languageSets.containsKey(word) && ((Set) sharedDictionary.languageSets.get(word)).contains(ld.language))
					score += (words.getElementCount(word) / 2);
			}
			System.out.println(" " + ((score * 100) / words.size()));
			if (score > maxScore) {
				maxScore = score;
				maxScoreDictionary = ld;
			}
		}
		maxScore = ((maxScore * 100) / words.size());
		return ((maxScore < languageMatchThreshold) ? null : maxScoreDictionary);
	}
	
	static String[] getDictionaryLanguages() {
		return ((String[]) languageDictionaries.keySet().toArray(new String[languageDictionaries.size()]));
	}
	
	static LanguageDictionary getDictionary(String language, boolean create) {
		if (SHARED_DICTIONARY_LANGUAGE.equals(language))
			return sharedDictionary;
		
		LanguageDictionary ld = ((LanguageDictionary) languageDictionaries.get(language));
		
		//	so far unknown language, create new dictionary (we're about to start learning a new language :-)
		if ((ld == null) && create) { 
			ld = new LanguageDictionary(language);
			languageDictionaries.put(language, ld);
			mainLanguages.addElementIgnoreDuplicates(language);
		}
		
		return ((ld == null) ? sharedDictionary : ld);
	}
	
	static boolean isContained(String word, ByteDictionary[] dictionaries) {
		for (int d = 0; d < dictionaries.length; d++)
			if (dictionaries[d].lookup(word)) return true;
		return false;
	}
	
	static String[] getCorections(String unknownWord, ByteDictionary dict, int maxEditDistance) {
		StringVector corrections = new StringVector();
		
		byte[] unknownWordCrossSumData = new byte[27];
		Arrays.fill(unknownWordCrossSumData, 0, unknownWordCrossSumData.length, ((byte) 0));
		for (int uwc = 0; uwc < unknownWord.length(); uwc++) {
			char ch = unknownWord.charAt(uwc);
			if (('a' <= ch) && (ch <= 'z')) unknownWordCrossSumData[ch - 'a']++;
			else if (('A' <= ch) && (ch <= 'Z')) unknownWordCrossSumData[ch - 'A']++;
			else unknownWordCrossSumData[26]++;
		}
		
//		int wordsTested = 0;
//		int crossSumChecks = 0;
//		int levenshteinChecks = 0;
//		int correctionsFound = 0;
		
		byte[] correctionCrossSumData = new byte[27];
		int crossSumDistance = 0;
		int levenshteinDistance;
		for (Iterator dit = dict.getIterator(Math.max(0, (unknownWord.length() - 1)), (unknownWord.length() + 2)); dit.hasNext();) {
			String correction = dit.next().toString();
//			wordsTested++;
			
			Arrays.fill(correctionCrossSumData, 0, correctionCrossSumData.length, ((byte) 0));
			crossSumDistance = 0;
			for (int cc = 0; cc < correction.length(); cc++) {
				char ch = correction.charAt(cc);
				if (('a' <= ch) && (ch <= 'z')) correctionCrossSumData[ch - 'a']++;
				else if (('A' <= ch) && (ch <= 'Z')) correctionCrossSumData[ch - 'A']++;
				else correctionCrossSumData[26]++;
			}
			for (int csc = 0; csc < unknownWordCrossSumData.length; csc++)
				crossSumDistance += Math.abs(unknownWordCrossSumData[csc] - correctionCrossSumData[csc]);
//			crossSumChecks++;
			
			if (crossSumDistance <= (2 * maxEditDistance)) {
				levenshteinDistance = StringUtils.getLevenshteinDistance(unknownWord, correction, maxEditDistance);
//				levenshteinChecks++;
				
				if (levenshteinDistance <= maxEditDistance) {// compute similarity to word in question
//					correctionsFound++;
					corrections.addElementIgnoreDuplicates(correction);
				}
			}
		}
		
//		System.out.println();
//		System.out.println(" - words tested: " + wordsTested);
//		System.out.println(" - cross sum checks: " + crossSumChecks);
//		System.out.println(" - levenshtein checks: " + levenshteinChecks);
//		System.out.println(" - corrections found: " + correctionsFound);
//		if (crossSumChecks != 0)
//			System.out.println(" ==> levenshtein checks saved: " + (crossSumChecks - levenshteinChecks) + ", " + ((100*(crossSumChecks - levenshteinChecks)) / crossSumChecks) + "%");
		
		return corrections.toStringArray();
//		LanguageDictionary.ensureLengthSurrogate(unknownWord.length());
//		
//		StringVector corrections = new StringVector();
//		String lengthSurrogat = LanguageDictionary.lengthSurrogatKeys.get(Math.max(0, (unknownWord.length() - 1))); // try words at most one char shorter
//		
//		for (Iterator dit = dict.tailSet(lengthSurrogat).iterator(); (dit != null) && dit.hasNext();) {
//			String correction = dit.next().toString();
//			if (correction.length() > (unknownWord.length() + 1)) dit = null; // test if current candidate stays within length limit (break if not)
//			else if (StringUtils.getLevenshteinDistance(unknownWord, correction, maxEditDistance) <= maxEditDistance)// compute similarity to word in question
//				corrections.addElementIgnoreDuplicates(correction);
//		}
//		
//		return corrections.toStringArray();
	}
	
	static String[] splitConcatenation(String word, ByteDictionary[] dictionaries, boolean firstSplitOnly, int maxParts) {
		return splitConcatenation(word, dictionaries, firstSplitOnly, maxParts, 1);
	}
	
	private static String[] splitConcatenation(String word, ByteDictionary[] dictionaries, boolean firstSplitOnly, int maxParts, int parts) {
		StringVector splits = new StringVector();
		
		//	lookup word in dictionaries (not in first level)
		if ((parts != 0) && SpellChecking.isContained(word, dictionaries)) {
			splits.addElement(word);
			if (firstSplitOnly) return splits.toStringArray();
		}
		
		//	check if further split allowed
		else if (parts >= maxParts) return splits.toStringArray();
		
		//	try splitting word
		for (int o = 1; o < (word.length() - 1); o++) {
			String prefix = word.substring(0, o);
			
			//	got prefix that is a word
			if (SpellChecking.isContained(prefix, dictionaries)) {
				
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
	
	//	correct the capitalization of a word
	static String adjustCapitalization(String word, String modelWord) {
		
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
		else if ((modelWord.length() != 0) && StringUtils.UPPER_CASE_LETTERS.indexOf(modelWord.charAt(0)) != -1)
			return (word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
		else return word.toLowerCase();
	}
	
	static Annotation[] getSkipAnnotations(MutableAnnotation data) {
		ArrayList skipAnnotationList = new ArrayList();
		for (int s = 0; s < SpellChecking.skipRegExes.length; s++) {
			Annotation[] skipAnnotations = Gamta.extractAllMatches(data, SpellChecking.skipRegExes[s], true, false, false);
			for (int a = 0; a < skipAnnotations.length; a++)
				skipAnnotationList.add(skipAnnotations[a]);
		}
		Annotation[] skipAnnotations = ((Annotation[]) skipAnnotationList.toArray(new Annotation[skipAnnotationList.size()]));
		Arrays.sort(skipAnnotations);
		return skipAnnotations;
	}
	
	private static SpellCheckingAnalyzer SCA = null;
	
	static void init(SpellCheckingAnalyzer sca) {
		if (SCA != null) return;
		SCA = sca;
		
		//	read parameters
		try {
			acceptFrequencyThreshold = Integer.parseInt(SCA.getParameter("acceptFrequencyThreshold", ("" + acceptFrequencyThreshold)));
		} catch (NumberFormatException e) {}
		try {
			rememberFrequencyThreshold = Integer.parseInt(SCA.getParameter("rememberFrequencyThreshold", ("" + rememberFrequencyThreshold)));
		} catch (NumberFormatException e) {}
		try {
			suggestionDistanceThreshold = Integer.parseInt(SCA.getParameter("suggestionDistanceThreshold", ("" + suggestionDistanceThreshold)));
		} catch (NumberFormatException e) {}
		try {
			maximumSplitParts = Integer.parseInt(SCA.getParameter("maximumSplitParts", ("" + maximumSplitParts)));
		} catch (NumberFormatException e) {}
		try {
			languageMatchThreshold = Integer.parseInt(SCA.getParameter("acceptFrequencyThreshold", ("" + languageMatchThreshold)));
		} catch (NumberFormatException e) {}
		
		//	load skip regular expressions
		try {
			StringVector load = SCA.loadList("skipRegEx.txt");
			load.sortLexicographically();
			skipRegExes = load.toStringArray();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " while loading skip reg exes: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		
		//	load list of languages
		try {
			StringVector load = SCA.loadList("languages.txt");
			load.sortLexicographically();
			languages = load.toStringArray();
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " while loading language list: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		
		//	load list of active languages
		try {
			mainLanguages = SCA.loadList("mainLanguages.cnfg");
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + " while loading active language list: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		
		//	load language dictionaries
		for (int d = 0; d < languages.length; d++) {
			String language = languages[d];
			LanguageDictionary ld = null;
			
			if (SCA.getDataProvider().isDataAvailable(language + STATIC_DICTIONARY_SUFFIX)) {
				try {
					InputStream is = SCA.getDataProvider().getInputStream(language + STATIC_DICTIONARY_SUFFIX);
					BufferedReader dr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					
					String de;
					while ((de = dr.readLine()) != null) {
						if (de.length() != 0) {
							if (ld == null)
								ld = new LanguageDictionary(language);
							ld.staticDictionary.add(de.toLowerCase());
						}
					}
					
					is.close();
					System.gc();
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " while loading static dictionary for '" + language + "': " + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
			
			if (SCA.getDataProvider().isDataAvailable(language + CUSTOM_DICTIONARY_SUFFIX)) {
				try {
					InputStream is = SCA.getDataProvider().getInputStream(language + CUSTOM_DICTIONARY_SUFFIX);
					BufferedReader dr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					
					String de;
					while ((de = dr.readLine()) != null) {
						if (de.length() != 0) {
							if (ld == null)
								ld = new LanguageDictionary(language);
							ld.customDictionary.add(de.toLowerCase());
						}
					}
					
					is.close();
					System.gc();
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " while loading custom dictionary for '" + language + "': " + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
			System.gc();
			
			if (ld != null) {
				ld.compile();
				languageDictionaries.put(language, ld);
			}
		}
		
		//	load shared dictionary
		sharedDictionary = new SharedDictionary();
		if (SCA.getDataProvider().isDataAvailable("Shared.static.txt")) {
			try {
				InputStream is = SCA.getDataProvider().getInputStream("Shared.static.txt");
				StringVector load = StringVector.loadList(is);
				is.close();
				for (int w = 0; w < load.size(); w++)
					sharedDictionary.staticDictionary.add(load.get(w).toLowerCase());
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " while loading static shared dictionary: " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
		if (SCA.getDataProvider().isDataAvailable("Shared.custom.txt")) {
			try {
				InputStream is = SCA.getDataProvider().getInputStream("Shared.custom.txt");
				StringVector load = StringVector.loadList(is);
				is.close();
				
				for (int l = 0; l < load.size(); l++) {
					String[] parts = load.get(l).split(",");
					if (parts.length == 1)
						sharedDictionary.addElement(parts[0]);
					else {
						for (int p = 1; p < parts.length; p++)
							sharedDictionary.addElement(parts[0], parts[p]);
						//	TODO: maybe remember count for individual languages
					}
				}
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " while loading custom shared dictionary: " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
		sharedDictionary.compile();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	static void exit() {
		if ((SCA == null) || !SCA.getDataProvider().isDataEditable()) return;
		SpellCheckingAnalyzer sca = SCA;
		SCA = null;
		
		//	store custom language dictionaries
		for (Iterator dit = languageDictionaries.values().iterator(); dit.hasNext();) {
			LanguageDictionary languageDictionary = ((LanguageDictionary) dit.next());
			if (languageDictionary.isDirty()) {
				try {
					OutputStream os = sca.getDataProvider().getOutputStream(languageDictionary.language + CUSTOM_DICTIONARY_SUFFIX);
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
					for (Iterator it = languageDictionary.customDictionary.getIterator(0, Integer.MAX_VALUE); it.hasNext();) {
						bw.write((String) it.next());
						bw.newLine();
					}
					bw.flush();
					bw.close();
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " while storing custom dictionary for '" + languageDictionary.language + "': " + e.getMessage());
					e.printStackTrace(System.out);
				}
			}
		}
		
		//	store custom shared dictionary
		if (sharedDictionary.languageSetsDirty || sharedDictionary.isDirty()) {
			try {
				OutputStream os = sca.getDataProvider().getOutputStream("Shared.custom.txt");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				for (Iterator it = sharedDictionary.customDictionary.getIterator(0, Integer.MAX_VALUE); it.hasNext();) {
					String element = ((String) it.next());
					Set languageSet = ((Set) sharedDictionary.languageSets.get(element));
					if (languageSet != null) {
						for (Iterator lit = languageSet.iterator(); lit.hasNext();)
							element += ("," + lit.next().toString());
					}
					bw.write(element);
					bw.newLine();
				}
				bw.flush();
				bw.close();
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " while storing shared custom dictionary: " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
		
		//	store parameters maximumSplitparts
		sca.storeParameter("acceptFrequencyThreshold", ("" + acceptFrequencyThreshold));
		sca.storeParameter("rememberFrequencyThreshold", ("" + rememberFrequencyThreshold));
		sca.storeParameter("suggestionDistanceThreshold", ("" + suggestionDistanceThreshold));
		sca.storeParameter("maximumSplitParts", ("" + maximumSplitParts));
		sca.storeParameter("languageMatchThreshold", ("" + languageMatchThreshold));
	}
	
	static class ByteDictionary implements Dictionary {
		private static final int chunkSize = 1020;
		private static final Comparator byteChunkComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				byte[][] b1 = ((byte[][]) o1);
				byte[][] b2 = ((byte[][]) o2);
				
				int c = lengthFirstbyteComparator.compare(b1[b1.length-1], b2[0]);
				if (c < 0) return -1;
				
				c = lengthFirstbyteComparator.compare(b2[b2.length-1], b1[0]);
				if (c < 0) return 1;
				
				return 0;
			}
		};
		private static final Comparator lengthFirstbyteComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				byte[] b1 = ((byte[]) o1);
				byte[] b2 = ((byte[]) o2);
				if (b1.length != b2.length)
					return (b1.length - b2.length);
				for (int b = 0; b < Math.min(b1.length, b2.length); b++) {
					if (b1[b] != b2[b])
						return ((255 & b1[b]) - (255 & b2[b]));
				}
				return 0;
			}
		};
		
		private static final Comparator lengthFirstComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				String s1 = ((String) o1);
				String s2 = ((String) o2);
				int c = s1.length() - s2.length();
				return ((c == 0) ? s1.compareTo(s2) : c);
			}
		};
		private final Set entrySet = new TreeSet(lengthFirstComparator);
		
		private int entryCount = -1;
		private TreeMap chunks = new TreeMap(byteChunkComparator);
		
		/**
		 * Constructor building a case sensitive dictionary
		 */
		ByteDictionary() {}
		
		/**
		 * Add a string to the dictionary. Any invocation of this method puts the
		 * dicitionary back into its building phase, so no lookups are possible
		 * before the next invocation of compile. Likewise, looking up the argument
		 * string returns true only after the next compilation.
		 * @param string the string to add
		 */
		synchronized void add(String string) {
			if (string != null)
				this.entrySet.add(string);
		}
		
		/**
		 * Check whether the dictionary has been compiled since the last addition.
		 * @return true if has been compiled, false otherwise
		 */
		synchronized boolean isCompiled() {
			return this.entrySet.isEmpty();
		}
		
		/**
		 * Compile the dictionary. This enables lookup until the next string is
		 * added. Invocing this method more than once between additions has no
		 * effect.
		 */
		synchronized void compile() {
			if (this.entrySet.isEmpty()) return;
			
			for(Iterator cit = this.chunks.values().iterator(); cit.hasNext();) {
				byte[][] chunk = ((byte[][]) cit.next());
				cit.remove();
				for (int c = 0; c < chunk.length; c++)
					this.entrySet.add(decode(chunk[c]));
			}
			this.entryCount = this.entrySet.size();
			
			compile(this.entrySet, this.chunks);
			
			this.entrySet.clear();
			System.gc();
			
			System.out.println("Compiled, register sizes are:\n- " + this.chunks.size() + " chunks");
		}
		
		private static void compile(Set entrySet, Map chunks) {
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
		StringIterator getIterator(final int fromLength, final int toLength) {
			final Iterator cit = this.chunks.values().iterator();
			return new StringIterator() {
				Iterator it = null;
				public boolean hasNext() {
					if (this.it == null) {
						if (cit.hasNext()) {
							byte[][] chunk = ((byte[][]) cit.next());
							if (chunk[chunk.length-1].length < fromLength)
								return this.hasNext();
							else if (chunk[0].length > toLength)
								return false;
							else {
								this.it = Arrays.asList(chunk).iterator();
								return this.it.hasNext();
							}
						}
						else return false;
					}
					else if (this.it.hasNext())
						return true;
					else {
						this.it = null;
						return this.hasNext();
					}
				}
				public Object next() {
					if (this.hasNext())
						return decode((byte[]) this.it.next());
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
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
		 */
		public boolean isEmpty() {
			return (this.size() == 0);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
		 */
		public int size() {
			return (this.isCompiled() ? this.entryCount : 0);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
		 */
		public StringIterator getEntryIterator() {
			if (this.isEmpty())
				return this.getIterator(0, 0);
			else {
				byte[][] lastChunk = ((byte[][]) this.chunks.get(this.chunks.lastKey()));
				return this.getIterator(0, lastChunk[lastChunk.length-1].length);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
		 */
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
		 */
		public boolean lookup(String string, boolean caseSensitive) {
			return this.lookup(string);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
		 */
		public boolean lookup(String string) {
			if (!this.isCompiled()) return false;
			byte[] bytes = encode(string);
			byte[][] bLookup = {bytes};
			byte[][] bChunk = ((byte[][]) chunks.get(bLookup));
			if (bChunk == null) return false;
			int index = Arrays.binarySearch(bChunk, bytes, lengthFirstbyteComparator);
			return ((index >= 0) && (index < bChunk.length) && (lengthFirstbyteComparator.compare(bChunk[index], bytes) == 0));
		}
		
		private static byte[] encode(String s) {
			s = s.toLowerCase();
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