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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CategorizationFeedbackPanel;
import de.uka.ipd.idaho.plugins.spellChecking.SpellChecking.ByteDictionary;
import de.uka.ipd.idaho.plugins.spellChecking.SpellChecking.LanguageDictionary;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * The part of the spell checker that determines the document's main language
 * and annotates possibly misspelled words.
 * 
 * @author sautter
 */
public class SpellErrorFinderOnline extends SpellCheckingAnalyzer implements LiteratureConstants {
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get skip annotations
		Annotation[] skipAnnotations = SpellChecking.getSkipAnnotations(data);
		System.out.println("Got " + skipAnnotations.length + " skip annotations:");
		for (int s = 0; s < skipAnnotations.length; s++)
			System.out.println("  '" + skipAnnotations[s] + "'");
		int skipAnnotationIndex;
		Annotation skipAnnotation;
		
		
		//	==== Step 1: Word Extraction ====
		StringVector words = new StringVector();
		StringVector distinctWords = new StringVector();
		StringVector wordParts = new StringVector();
		StringVector distinctWordParts = new StringVector();
		
		//	extract words to check (ignoring ones contained in skip annotations)
		skipAnnotationIndex = 0;
		skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
		for (int v = 0; v < data.size(); v++) {
			
			//	after current skip annotation, move to next
			if ((skipAnnotation != null) && (v >= skipAnnotation.getEndIndex())) {
				
				//	find next relevant skip annotation (they might be nested)
				while ((skipAnnotationIndex < skipAnnotations.length) && (skipAnnotations[skipAnnotationIndex].getEndIndex() <= v))
					skipAnnotationIndex++;
				
				//	get first matching skip annotation (if any)
				skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
			}
			
			//	no skip annotation, or before current skip annotation
			if ((skipAnnotation == null) || (v < skipAnnotation.getStartIndex())) {
				
				//	get value
				String value = Gamta.normalize(data.valueAt(v).toLowerCase());
				if (Gamta.isWord(value)) {
					
					//	split at possible inner punctuation marks
					String[] valueParts = value.split("[^a-z]");
					
					//	store word and parts
					words.addElement(value);
					distinctWords.addElementIgnoreDuplicates(value);
					wordParts.addContent(valueParts);
					distinctWordParts.addContentIgnoreDuplicates(valueParts);
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
		LanguageDictionary mainDictionary = SpellChecking.getMainDictionary(wordParts, distinctWordParts);
		String mainLanguage = ((mainDictionary == null) ? null : mainDictionary.language);
		ArrayList subDictionaries = new ArrayList();
		if (mainDictionary == null) {
			
			//	ask user for main language if allowed to
			if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
				
				/*
				for determining document language:
				- get language as categorization feedback for 10 randomly selected paragraphs
				  - use English as default setting
				- as long as no language gets the 67% majority, keep on getting categorization feedback for 10 additional paragraphs
				  - use language currently having majority as default setting
				- assign language attribute to paragraphs if it differs throughout document
				- if no paragraphs remain, use:
				  - simple majority as main language
				  - all dictionaries above 100-67 = 33% in finding misspellings (at most 3 dictionaries)
				  - the dictionary for the specific languages for paragraphs it was specified for (if available)
				*/
				
				//	get and shuffle paragraphs
				ArrayList paragraphList = new ArrayList(Arrays.asList(data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE)));
				Collections.shuffle(paragraphList);
				int paragraphsSampeled = 0;
				
				//	build statistics data structures
				String topLanguage = "English";
				StringVector paragraphLanguages = new StringVector();
				StringIndex paragraphLanguageVotes = new StringIndex();
				
				//	proceed until language clear or no paragraphs remaining
				while (mainLanguage == null) {
					
					//	get sample of paragraphs
					ArrayList paragraphSampleList = new ArrayList();
					for (int s = 0; (s < 10) && (paragraphList.size() != 0); s++) {
						paragraphSampleList.add(paragraphList.remove(0));
						paragraphsSampeled++;
					}
					Collections.sort(paragraphSampleList);
					MutableAnnotation[] paragraphSample = ((MutableAnnotation[]) paragraphSampleList.toArray(new MutableAnnotation[paragraphSampleList.size()]));
					
					//	get language feedback
					String[] paragraphSampleLanguages = this.askLanguages(paragraphSample, topLanguage);
					
					//	process feedback
					for (int p = 0; p < paragraphSample.length; p++) {
						paragraphSample[p].setAttribute(SpellChecking.LANGUAGE_ATTRIBUTE, paragraphSampleLanguages[p]);
						paragraphLanguages.addElementIgnoreDuplicates(paragraphSampleLanguages[p]);
						paragraphLanguageVotes.add(paragraphSampleLanguages[p]);
					}
					
					//	determine most frequent language
					for (int l = 0; l < paragraphLanguages.size(); l++) {
						String language = paragraphLanguages.get(l);
						if (paragraphLanguageVotes.getCount(language) > paragraphLanguageVotes.getCount(topLanguage))
							topLanguage = language;
					}
					
					//	if we've got a sufficient majority, set main language and assign it to all remaining paragraphs as well
					if ((paragraphLanguageVotes.getCount(topLanguage) * 100) >= (paragraphsSampeled * SpellChecking.languageMatchThreshold)) {
						mainLanguage = topLanguage;
						
						//	set language of remaining paragraphs
						for (int p = 0; p < paragraphList.size(); p++)
							((Annotation) paragraphList.get(p)).setAttribute(SpellChecking.LANGUAGE_ATTRIBUTE, mainLanguage);
					}
					
					//	if no paragraphs remaining to sample, use most-voted language as main language, and add sub languages
					else if (paragraphList.isEmpty()) {
						mainLanguage = topLanguage;
						
						//	test all selected languages
						for (int l = 0; l < paragraphLanguages.size(); l++) {
							String subLanguage = paragraphLanguages.get(l);
							
							//	use only sub languages that have received sufficient votes
							if (!mainLanguage.equals(subLanguage) && ((paragraphLanguageVotes.getCount(subLanguage) * 100) >= (paragraphsSampeled * (100 - SpellChecking.languageMatchThreshold)))) {
								LanguageDictionary subDictionary = SpellChecking.getDictionary(subLanguage, true);
								
								//	store sub dictionary
								if (subDictionary != null)
									subDictionaries.add(subDictionary);
							}
						}
					}
				}
				
				//	get dictionary
				mainDictionary = SpellChecking.getDictionary(mainLanguage, true);
			}
			
			//	must not prompt user, use shared dictionary
			else {
				mainDictionary = SpellChecking.sharedDictionary;
				mainLanguage = SpellChecking.sharedDictionary.language;
			}
		}
		
		//	extract main language
		System.out.println("Got main language: " + mainLanguage);
		
		//	store main language
		data.setAttribute(SpellChecking.LANGUAGE_ATTRIBUTE, mainLanguage);
		
		
		//	==== Step 3: Dictionary Bootstrapping ====
		ByteDictionary dynamicDictionary = new ByteDictionary();
		for (int w = 0; w < distinctWordParts.size(); w++) {
			String word = distinctWordParts.get(w);
			if (wordParts.getElementCount(word) >= SpellChecking.acceptFrequencyThreshold)
				dynamicDictionary.add(word);
		}
		dynamicDictionary.compile();
		System.out.println("Got " + dynamicDictionary.size() + " words in dynamic dictionary");
		
		//	build meta dictionary
		ArrayList dictionaryList = new ArrayList();
		dictionaryList.add(mainDictionary.staticDictionary);
		dictionaryList.add(mainDictionary.customDictionary);
		dictionaryList.add(dynamicDictionary);
		if (mainDictionary != SpellChecking.sharedDictionary) {
			dictionaryList.add(SpellChecking.sharedDictionary.staticDictionary);
			dictionaryList.add(SpellChecking.sharedDictionary.customDictionary);
		}
		for (Iterator dit = subDictionaries.iterator(); dit.hasNext();) {
			LanguageDictionary subDictionary = ((LanguageDictionary) dit.next());
			dictionaryList.add(subDictionary.staticDictionary);
			dictionaryList.add(subDictionary.customDictionary);
		}
		ByteDictionary[] dictionaries = ((ByteDictionary[]) dictionaryList.toArray(new ByteDictionary[dictionaryList.size()]));
		
		
		//	==== Step 4: Incorrect Word Extraction ====
		StringVector unknownWords = new StringVector(); // list of unknown words
		Properties unknownWordLanguages = new Properties(); // mapping of unknown words to non-main languages (for out of language words)
		
		//	check all the distinct words
		for (int w = 0; w < distinctWords.size(); w++) {
			String word = distinctWords.get(w);
			
			//	omit single letters
			if (word.length() > 1) {
				
				//	split word into pure letter blocks
				String[] parts = word.split("[^a-z]");
				
				//	check dictionaries with admissable words
				if (SpellChecking.isContained(word, dictionaries)) word = null;
				
				//	check word parts (check only words with more than one part)
				else if (parts.length != 1) {
					boolean partMatch = true;
					for (int p = 0; (parts != null) && (p < parts.length); p++)
						partMatch = (((parts[p].length() <= 1) || SpellChecking.isContained(parts[p], dictionaries)) && partMatch);
					if (partMatch) word = null;
				}
				
				//	check other language dictionaries
				String[] dictionaryLanguages = SpellChecking.getDictionaryLanguages();
				for (int d = 0; (word != null) && (d < dictionaryLanguages.length); d++) {
					LanguageDictionary ld = SpellChecking.getDictionary(dictionaryLanguages[d], true);
					if (ld != mainDictionary) {
						if (ld.lookup(word)) {
							unknownWords.addElement(word);
							unknownWordLanguages.setProperty(word, ld.language);
							word = null;
						}
						else {
							boolean partMatch = true;
							for (int p = 0; (parts != null) && (p < parts.length); p++)
								partMatch = (SpellChecking.isContained(parts[p], dictionaries) && partMatch);
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
		
		//	extract frequent endings
		StringVector endings = new StringVector();
		for (int u = 0; u < unknownWords.size(); u++) {
			String unknownWord = unknownWords.get(u);
			
			//	try different cutoffs
			for (int l = (unknownWord.length() - SpellChecking.suggestionDistanceThreshold + 1); l < unknownWord.length(); l++) {
				String unknownWordPrefix = unknownWord.substring(0, l);
				if (SpellChecking.isContained(unknownWordPrefix, dictionaries))
					endings.addElement(unknownWord.substring(l));
			}
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
			for (int e = 0; e < distinctEndings.size(); e++) {
				String ending = distinctEndings.get(e);
				if ((endings.getElementCount(ending) >= SpellChecking.rememberFrequencyThreshold) && ((unknownWord.length() - ending.length()) > 1) && unknownWord.endsWith(ending)) {
					System.out.println("Found '" + unknownWord + "' to be a flected form of '" + unknownWord.substring(0, (unknownWord.length() - ending.length())) + "'");
					unknownWords.remove(u--);
					e = distinctEndings.size();
				}
			}
		}
		
		//	annotate misspellings (ignoring ones that lie inside skip annotations)
		skipAnnotationIndex = 0;
		skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
		for (int v = 0; v < data.size(); v++) {
			
			//	after current skip annotation, move to next
			if ((skipAnnotation != null) && (v >= skipAnnotation.getEndIndex())) {
				
				//	find next relevant skip annotation (they might be nested)
				while ((skipAnnotationIndex < skipAnnotations.length) && (skipAnnotations[skipAnnotationIndex].getEndIndex() <= v))
					skipAnnotationIndex++;
				
				//	get first matching skip annotation (if any)
				skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
			}
			
			//	no skip annotation, or before current skip annotation
			if ((skipAnnotation == null) || (v < skipAnnotation.getStartIndex())) {
				
				//	check if value is unknown
				String value = Gamta.normalize(data.valueAt(v).toLowerCase());
				if (unknownWords.contains(value)) {
					Annotation unknownWordAnnotation = data.addAnnotation(SpellChecking.MISSPELLING_ANNOTATION_TYPE, v, 1);
					if (unknownWordLanguages.containsKey(value)) {
						unknownWordAnnotation.setAttribute(SpellChecking.MISSPELLING_TYPE_ATTRIBUTE, SpellChecking.OUT_OF_LANGUAGE_MISSPELLING_TYPE);
						unknownWordAnnotation.setAttribute(SpellChecking.LANGUAGE_ATTRIBUTE, unknownWordLanguages.getProperty(value));
					}
					else unknownWordAnnotation.setAttribute(SpellChecking.MISSPELLING_TYPE_ATTRIBUTE, SpellChecking.MISSPELLED_MISSPELLING_TYPE);
				}
			}
		}
	}
	
	private String[] askLanguages(MutableAnnotation[] paragraphs, String language) {
		CategorizationFeedbackPanel cfp = new CategorizationFeedbackPanel("Check Paragraph Types");
		cfp.setLabel("<HTML>Please check which <B>language(s)</B> these paragraphs are (mainly) written in.</HTML>");
		
		cfp.setPropagateCategoryChanges(false);
		cfp.setChangeSpacing(15);
		cfp.setContinueSpacing(15);
		
		for (int t = 0; t < SpellChecking.languages.length; t++) {
			cfp.addCategory(SpellChecking.languages[t]);
			cfp.setCategoryColor(SpellChecking.languages[t], Color.WHITE);
		}
		System.out.println("  - dialog built");
		
		//	pre-categorize and line up paragraphs
		System.out.println("  - got " + paragraphs.length + " paragraphs");
		for (int p = 0; p < paragraphs.length; p++)
			cfp.addLine(TokenSequenceUtils.concatTokens(paragraphs[p]), language);
		
		//	add backgroung information
		cfp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, paragraphs[0].getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
		cfp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, paragraphs[0].getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
		cfp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, paragraphs[0].getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
		cfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, paragraphs[0].getAnnotationID());
		cfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PARAGRAPH_TYPE);
		
		//	add target page numbers
		String targetPages = FeedbackPanel.getTargetPageString(paragraphs);
		if (targetPages != null)
			cfp.setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
		String targetPageIDs = FeedbackPanel.getTargetPageIdString(paragraphs);
		if (targetPageIDs != null)
			cfp.setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		
		//	display dialog
		String f = null;
//		try {
//			f = FeedbackPanelHtmlTester.testFeedbackPanel(cfps[d], 0);
//		}
//		catch (IOException ioe) {
//			ioe.printStackTrace();
//		}
		if (f == null)
			f = cfp.getFeedback();
		
		//	read feedback
		String[] languages = new String[paragraphs.length];
		for (int p = 0; p < paragraphs.length; p++)
			languages[p] = cfp.getCategoryAt(p);
		
		//	return result
		return languages;
	}
}