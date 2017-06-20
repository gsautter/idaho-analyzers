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
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.plugins.spellChecking.SpellChecking.ByteDictionary;
import de.uka.ipd.idaho.plugins.spellChecking.SpellChecking.LanguageDictionary;
import de.uka.ipd.idaho.plugins.spellChecking.SpellErrorCorrectorOnline.SpellCheckingPanel.CorrectionData;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * The part of the spell checker that determines possible corrections for
 * misspelled words, get feedback from users, and extends the dictionaries based
 * on that feedback.
 * 
 * @author sautter
 */
public class SpellErrorCorrectorOnline extends SpellCheckingAnalyzer implements LiteratureConstants {
	
	//	TODO observe full text edit request, maybe in separate analyzer
	
	/* TODO
try to learn mis-OCRing probabilities from corrections
- use edit sequences to recognize which character combination was substituted for what OCRed character combination
==> implement more flexible Levenshtein using a context aware cost model
  ==> ask Prof. Sanders regarding developments in this area
==> use this substitution probability based cost model when searching possible correct forms for unknown words

learn frequent character substitutions like "rn" or "ni" for "m" and vice versa:
- for unknown words, try all possible combinations of known frequent substitutions (via String.replaceAll())
- ... look up result(s) in dictionaries (including document dictionary)
- ... and on successful lookup(s) propose words originating from substitution(s) as sole correction suggestions
==> saves _lots_ of edit distance computations!
==> ... and allows for correction suggestions that are beyond general edit distance limit, e.g. "mm" misOCRed as "rnni" (edit distance 4)
	 */
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		/*
		for actual correction:
		- get main dictionary from document's language attribute
		- restore unknownWords by scanning over misspelling annotations
		- restore dynamic dictionary by scanning document, ignoring content of skip annotations, and subtracting unknownWords
		- gather corrections for each misspelling
		  - use misspelling type (eg for splits)
		- get feedback
		- correct spelling & learn from feedback
		 */
		
		//	get misspellings
		Annotation[] misspellings = data.getAnnotations(SpellChecking.MISSPELLING_ANNOTATION_TYPE);
		
		//	don't do all the rest if nothing to correct ...
		if (misspellings.length == 0) return;
		
		
		//	==== Step 1: Word Extraction ==== (abbreviated repetition)
		StringVector unknownWords = new StringVector(); // list of unknown words
		Properties unknownWordLanguages = new Properties(); // mapping of unknown words to non-main languages (for out of language words)
		
		StringVector words = new StringVector();
		StringVector distinctWords = new StringVector();
		StringVector wordParts = new StringVector();
		StringVector distinctWordParts = new StringVector();
		
		//	collect unknown words (misspellings)
		for (int m = 0; m < misspellings.length; m++) {
			String unknownWord = misspellings[m].getValue();
			unknownWords.addElementIgnoreDuplicates(unknownWord);
			if (SpellChecking.OUT_OF_LANGUAGE_MISSPELLING_TYPE.equals(misspellings[m].getAttribute(SpellChecking.MISSPELLING_TYPE_ATTRIBUTE))) {
				String language = ((String) misspellings[m].getAttribute(SpellChecking.LANGUAGE_ATTRIBUTE));
				if (language != null)
					unknownWordLanguages.setProperty(unknownWord, language);
			}
		}
		
		//	restore data dictionary from known words that do not lie inside skip annotations
		Annotation[] skipAnnotations = SpellChecking.getSkipAnnotations(data);
		int skipAnnotationIndex = 0;
		Annotation skipAnnotation = ((skipAnnotationIndex < skipAnnotations.length) ? skipAnnotations[skipAnnotationIndex++] : null);
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
				if (Gamta.isWord(value) && !unknownWords.contains(value)) {
					
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
		
		
		//	==== Step 2: Language Classification ==== (abbreviated repetition)
		String mainLanguage = data.getAttribute(SpellChecking.LANGUAGE_ATTRIBUTE, SpellChecking.sharedDictionary.language).toString();
		LanguageDictionary mainDictionary = SpellChecking.getDictionary(mainLanguage, true);
		System.out.println("Got main language: " + mainLanguage);
		
		
		//	==== Step 3: Dictionary Bootstrapping ==== (abbreviated repetition)
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
		ByteDictionary[] dictionaries = ((ByteDictionary[]) dictionaryList.toArray(new ByteDictionary[dictionaryList.size()]));
		
		
		//	==== Step 5: Search For Corrections ====
		HashMap unknownWordCorrections = new HashMap();
		
		//	find corrections for unknown words, and collect suffixes that distinguish a known word from an unknown one
		for (int u = 0; u < unknownWords.size(); u++) {
			String unknownWord = unknownWords.get(u);
			StringVector corrections = new StringVector();
			int maxCorrectionDistance = Math.min(((unknownWord.length() + 1) / 2), SpellChecking.suggestionDistanceThreshold);
			System.out.print("Getting corrections for '" + unknownWord + "' with max distance " + maxCorrectionDistance + " ...");
			
			//	check dictionaries
			for (int d = 0; d < dictionaries.length; d++)
				corrections.addContentIgnoreDuplicates(SpellChecking.getCorections(unknownWord, dictionaries[d], maxCorrectionDistance));
			
			//	store possible corrections
			unknownWordCorrections.put(unknownWord, corrections);
			System.out.println(" " + corrections.size() + " corrections");
		}
		
		//	==== Step 6: Word Separation Check ====
		
		//	find unknown words that are concatenations of known words
		for (int u = 0; u < unknownWords.size(); u++) {
			String unknownWord = unknownWords.get(u);
			String[] splits = SpellChecking.splitConcatenation(unknownWord, dictionaries, false, SpellChecking.maximumSplitParts);
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
		for (int m = 0; m < misspellings.length; m++) {
			String csValue = misspellings[m].getValue();
			String value = csValue.toLowerCase();
			
			//	add suggested corrections
			StringVector corrections = ((StringVector) unknownWordCorrections.get(value));
			if (corrections == null)
				corrections = new StringVector();
			
			//	sort corrections by edit distance
			String[] sortedCorrections = corrections.toStringArray();
			final HashMap distances = new HashMap();
			for (int c = 0; c < sortedCorrections.length; c++)
				distances.put(sortedCorrections[c], new Integer(StringUtils.getLevenshteinDistance(sortedCorrections[c], value, SpellChecking.suggestionDistanceThreshold, false)));
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
				sortedCorrections[c] = SpellChecking.adjustCapitalization(sortedCorrections[c], csValue);
			
			//	add splits for camel case words
			if (csValue.matches("[A-Z]+([^A-Z]*[a-z][A-Z]+)+[^A-Z]*")) {
				String fullValue = csValue;
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
				
				String fullValue = csValue;
				parts = fullValue.split("[^a-zA-Z]");
				StringBuffer asOnePartFull = new StringBuffer();
				for (int p = 0; p < parts.length; p++)
					asOnePartFull.append(parts[p]);
				
				int shift = (asOnePart.toString().equals(asOnePartFull.toString()) ? 1 : 2);
				String[] newSortedCorrections = new String[sortedCorrections.length + shift];
				if (shift == 1)
					newSortedCorrections[0] = SpellChecking.adjustCapitalization(asOnePart.toString(), parts[0]);
				else {
					newSortedCorrections[0] = asOnePartFull.toString();
					newSortedCorrections[1] = SpellChecking.adjustCapitalization(asOnePart.toString(), parts[0]);
				}
				System.arraycopy(sortedCorrections, 0, newSortedCorrections, shift, sortedCorrections.length);
				sortedCorrections = newSortedCorrections;
				System.out.println("Got high comma removal correction(s) for '" + csValue + "': " + newSortedCorrections[shift-1] + ((shift == 1) ? "" : (", " + asOnePartFull.toString())));
			}
			
			//	add corrections
			misspellings[m].setAttribute(SpellChecking.CORRECTIONS_ATTRIBUTE, sortedCorrections);
			
			
			//	try concatenation (only for clean values)
			if (value.matches("[a-z]++")) {
				int minConcatStart = misspellings[m].getStartIndex();
				int maxConcatEnd = misspellings[m].getStartIndex();
				ArrayList concatList = new ArrayList();
				
				//	test concatenating from 2 to maximumSplitParts tokens
				for (int p = 2; p <= SpellChecking.maximumSplitParts; p++) {
					
					//	start left of current value and move rigth
					for (int s = Math.max(0, (misspellings[m].getStartIndex() + 1 - p)); s <= misspellings[m].getStartIndex(); s++) {
						
						//	concatinate tokens
						StringBuffer sb = new StringBuffer();
						for (int t = s; t < Math.min(data.size(), (s + p)); t++)
							sb.append(data.valueAt(t));
						
						//	do lookup, remember in case of hit
						if (SpellChecking.isContained(sb.toString(), dictionaries)) {
							minConcatStart = Math.min(minConcatStart, s);
							maxConcatEnd = Math.max(maxConcatEnd, (s+p));
							concatList.add(Gamta.newAnnotation(data, SpellChecking.MISSPELLING_ANNOTATION_TYPE, s, p));
						}
						
						//	check if concatenation appears somewhere else in document
						else if (words.containsIgnoreCase(sb.toString())) {
							minConcatStart = Math.min(minConcatStart, s);
							maxConcatEnd = Math.max(maxConcatEnd, (s+p));
							concatList.add(Gamta.newAnnotation(data, SpellChecking.MISSPELLING_ANNOTATION_TYPE, s, p));
						}
					}
				}
				
				//	remove initial misspelling if concatenation successful
				if (concatList.size() != 0) {
					
					//	remove initial misspelling if concatenation successful
					data.removeAnnotation(misspellings[m]);
					
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
						concatCorrections.addElementIgnoreDuplicates(prefix.toString() + SpellChecking.adjustCapitalization(sb.toString(), cc.firstValue()) + suffix.toString());
					}
					
					//	annotate outer limit of possible concatenations
					misspellings[m] = data.addAnnotation(SpellChecking.MISSPELLING_ANNOTATION_TYPE, minConcatStart, (maxConcatEnd - minConcatStart));
					misspellings[m].setAttribute(SpellChecking.CORRECTIONS_ATTRIBUTE, concatCorrections.toStringArray());
					misspellings[m].setAttribute(SpellChecking.MISSPELLING_TYPE_ATTRIBUTE, SpellChecking.SPLIT_MISSPELLING_TYPE);
				}
			}
			
		}
		
		
		//	==== Step 7: Spelling Correction ====
		
		//	get feedback if allowed to
		if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
			
			//	add page numbers to misspellings
			MutableAnnotation[] paragraphs = data.getMutableAnnotations(PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
				if (pageNumber != null) {
					Annotation[] paragraphMisspellings = paragraphs[p].getAnnotations(SpellChecking.MISSPELLING_ANNOTATION_TYPE);
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
					paragraphMisspellings[p] = paragraphs[p].getAnnotations(SpellChecking.MISSPELLING_ANNOTATION_TYPE);
					scps[p] = buildSpellCheckingPanel(paragraphs[p], paragraphMisspellings[p], mainLanguage, keptWords);
					if (scps[p] != null) {
						Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
						scps[p].setLabel("<HTML>Please check the spelling of the highlighted words in the paragraph below (" + ((pageNumber == null) ? "unknown page" : ("page " + pageNumber)) + ")." +
								"<BR>Possibel misspellings untouched so far have a red highlight, one already inspected have a green one." +
								"<BR>Click on a highlighted word to open the correction pop-up menu." +
								"<BR>In this menu, you can also specify if and how the spell checker should remember the word inquestion to recognize it as spelled correctly from now on." +
								"<BR>If you make a correction, you can choose the spell checker should remember the correction.</HTML>");
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
							keptWords.addElementIgnoreDuplicates(misspelling.getValue());
						
						else {
							
							//	annotate correction (if different from original in something other than whitespace)
							if (!cd.correction.replaceAll("\\s", "").equals(misspelling.getValue().replaceAll("\\s", ""))) {
								Annotation corrected = paragraphs[p].addAnnotation(SpellChecking.CORRECTED_MISSPELLING_ANNOTATION_TYPE, misspelling.getStartIndex(), misspelling.size());
								corrected.setAttribute(SpellChecking.ORIGINAL_VALUE_ATTRIBUTE, misspelling.getValue());
							}
							
							//	apply correction
							paragraphs[p].setChars(cd.correction, misspelling.getStartOffset(), misspelling.length());
						}
						
						//	learn
						if (!SpellCheckingPanel.doNotRemember.equals(cd.rememberLanguage)) {
							String correction = cd.correction.toLowerCase();
							
							//	learnable word
							if (correction.matches("[a-z]++")) {
								
								//	domain specific term
								if (SpellCheckingPanel.rememberDomainSpecific.equals(cd.rememberLanguage))
									SpellChecking.sharedDictionary.addElement(correction, scps[p].mainLanguage);
								
								//	language specific dictionary
								else {
									String language = (SpellCheckingPanel.rememberMainLanguage.equals(cd.rememberLanguage) ? scps[p].mainLanguage : cd.rememberLanguage);
									LanguageDictionary ld = SpellChecking.getDictionary(language, true);
									ld.addElement(correction);
								}
							}
						}
						
						//	remove misspellings as dealt with
						paragraphs[p].removeAnnotation(misspelling);
						
						//	TODO observe requests for full text editing
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
				int dialogNumber = 0;
				for (int p = 0; p < paragraphs.length; p++) {
					
					//	produce feedback panel
					Annotation[] paragraphMisspellings = paragraphs[p].getAnnotations(SpellChecking.MISSPELLING_ANNOTATION_TYPE);
					SpellCheckingPanel scp = buildSpellCheckingPanel(paragraphs[p], paragraphMisspellings, mainLanguage, keptWords);
					
					//	no misspellings, or all contained in kept words ==> clean up
					if (scp == null) {
						for (int m = 0; m < paragraphMisspellings.length; m++)
							paragraphs[p].removeAnnotation(paragraphMisspellings[m]);
					}
					
					//	misspellings to deal with, get feedback
					else {
						dialogNumber++;
						
						//	complete feedback panel
						Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
						scp.setLabel("<HTML>Please check the spelling of the highlighted words in the paragraph below (" + ((pageNumber == null) ? "unknown page" : ("page " + pageNumber)) + ")." +
								"<BR>Possibel misspellings untouched so far have a red highlight, one already inspected have a green one." +
								"<BR>Click on a highlighted word to open the correction pop-up menu." +
								"<BR>In this menu, you can also specify if and how the spell checker should remember the word inquestion to recognize it as spelled correctly from now on." +
								"<BR>If you make a correction, you can choose the spell checker should remember the correction.</HTML>");
						scp.addButton("OK");
						scp.addButton("Cancel");
						
						String title = scp.getTitle();
						scp.setTitle(title + " - (" + dialogNumber + " of " + misspellingParagraphs + ")");
						
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
										keptWords.addElementIgnoreDuplicates(misspelling.getValue());
									
									else {
										
										//	annotate correction (if different from original in something other than whitespace)
										if (!cd.correction.replaceAll("\\s", "").equals(misspelling.getValue().replaceAll("\\s", ""))) {
											Annotation corrected = paragraphs[p].addAnnotation(SpellChecking.CORRECTED_MISSPELLING_ANNOTATION_TYPE, misspelling.getStartIndex(), misspelling.size());
											corrected.setAttribute(SpellChecking.ORIGINAL_VALUE_ATTRIBUTE, misspelling.getValue());
										}
										
										//	apply correction
										paragraphs[p].setChars(cd.correction, misspelling.getStartOffset(), misspelling.length());
									}
									
									//	learn
									if (!SpellCheckingPanel.doNotRemember.equals(cd.rememberLanguage)) {
										String correction = cd.correction.toLowerCase();
										
										//	learnable word
										if (correction.matches("[a-z]++")) {
											
											//	domain specific term
											if (SpellCheckingPanel.rememberDomainSpecific.equals(cd.rememberLanguage))
												SpellChecking.sharedDictionary.addElement(correction, scp.mainLanguage);
											
											//	language specific dictionary
											else {
												String language = (SpellCheckingPanel.rememberMainLanguage.equals(cd.rememberLanguage) ? scp.mainLanguage : cd.rememberLanguage);
												LanguageDictionary ld = SpellChecking.getDictionary(language, true);
												ld.addElement(correction);
											}
										}
									}
								}
								
								//	remove misspellings as dealt with
								paragraphs[p].removeAnnotation(misspelling);
							}
							
							//	TODO observe requests for full text editing
						}
					}
				}
			}
			
			//	compute main language from feedback if not recognized so far
			if (SpellChecking.sharedDictionary.language.equals(mainLanguage)) {
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
		for (Iterator ddit = dynamicDictionary.getIterator(0, Integer.MAX_VALUE); ddit.hasNext();) {
			String word = ddit.next().toString().toLowerCase();
			if (word.matches("[a-z]++") && !mainDictionary.lookup(word) && (words.getElementCount(word) >= SpellChecking.rememberFrequencyThreshold))
				SpellChecking.sharedDictionary.addElement(word, mainLanguage);
		}
		
		//	restore shared dictionary
		SpellChecking.sharedDictionary.compile();
	}
	
	private static SpellCheckingPanel buildSpellCheckingPanel(MutableAnnotation paragraph, Annotation[] misspellings, String mainLanguage, StringVector keptWords) {
		if (misspellings.length == 0) return null;
		
		SpellCheckingPanel scp = new SpellCheckingPanel();
		scp.setMainLanguage(mainLanguage);
		
		//	add backgroung information
		scp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, paragraph.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
		String pageNumber = ((String) paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE));
		scp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((pageNumber == null) ? "" : pageNumber));
		String pageId = ((String) paragraph.getAttribute(PAGE_ID_ATTRIBUTE));
		scp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((pageId == null) ? "" : pageId));
		scp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, paragraph.getAnnotationID());
		scp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, SpellChecking.MISSPELLING_ANNOTATION_TYPE);
		
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
				String[] suggestions = ((String[]) misspellings[misspellingIndex].getAttribute(SpellChecking.CORRECTIONS_ATTRIBUTE));
				if (suggestions == null) suggestions = new String[0];
				
				//	get language for out-of-language words
				String language = null;
				if (SpellChecking.OUT_OF_LANGUAGE_MISSPELLING_TYPE.equals(misspellings[misspellingIndex].getAttribute(SpellChecking.MISSPELLING_TYPE_ATTRIBUTE)))
					language = ((String) misspellings[misspellingIndex].getAttribute(SpellChecking.LANGUAGE_ATTRIBUTE));
				
				//	add misspelling
				scp.addMisspelling(misspellings[misspellingIndex].getAnnotationID(), misspelling, suggestions, language);
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
		
		public SpellCheckingPanel() {
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
		
		void addMisspelling(String misspellingId, String misspelling, String[] suggestions, String language) {
			final MisspellingLabel ml = new MisspellingLabel(misspellingId, misspelling, suggestions);
			
			ml.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					cp.showFor(ml);
				}
			});
			
			this.misspellingList.add(ml);
			this.misspellingsById.put(ml.misspellingId, ml);
			
			//	set non-null language
			if (language != null)
				ml.setRememberLanguage(language);
			
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
					String[] misspellingData = tokenData.split("\\s", -1);
					
					//	some tokens left, transfer status
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
						this.addMisspelling(misspellingData[0], URLDecoder.decode(misspellingData[1], "UTF-8"), suggestions, misspellingData[3]);
					}
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			Properties fs = new Properties();
			fs.setProperty("mainLanguage", this.mainLanguageSelector.getSelectedItem().toString());
			fs.setProperty("fullTextEdit", (this.needFullTextEdit.isSelected() ? "T" : "F"));
			for (Iterator mit = this.misspellingsById.values().iterator(); mit.hasNext();) {
				MisspellingLabel ml = ((MisspellingLabel) mit.next());
				fs.setProperty((ml.misspellingId + "_correction"), ml.correction);
				fs.setProperty((ml.misspellingId + "_remember"), ml.rememberLanguage);
			}
			return fs;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			this.mainLanguageSelector.setSelectedItem(states.getProperty("mainLanguage", this.mainLanguage));
			this.needFullTextEdit.setSelected("T".equals(states.getProperty("fullTextEdit", "F")));
			for (Iterator mit = this.misspellingsById.values().iterator(); mit.hasNext();) {
				MisspellingLabel ml = ((MisspellingLabel) mit.next());
				ml.setCorrection(states.getProperty((ml.misspellingId + "_correction"), ml.correction));
				ml.setRememberLanguage(states.getProperty((ml.misspellingId + "_remember"), ml.rememberLanguage));
			}
		}
		
		CorrectionData getCorrectionData(String misspellingId) {
			MisspellingLabel ml = ((MisspellingLabel) this.misspellingsById.get(misspellingId));
			System.out.println("Label for " + misspellingId + " is " + ((ml == null) ? "null, misspellings are" : ml.value));
			if (ml == null) {
				for (int t = 0; t < this.tokenList.size(); t++) {
					TokenLabel tl = ((TokenLabel) this.tokenList.get(t));
					if (tl instanceof MisspellingLabel)
						System.out.println("  - " + ((MisspellingLabel) tl).value + " (" + ((MisspellingLabel) tl).misspellingId + ")");
				}
			}
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
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
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
			bw.writeLine(".mainTable {");
			bw.writeLine("  border: 2pt solid #444444;");
			bw.writeLine("  border-collapse: collapse;");
			bw.writeLine("}");
			bw.writeLine(".mainTableCell {");
			bw.writeLine("  padding: 5pt;");
			bw.writeLine("  margin: 0pt;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".misspelling {");
			bw.writeLine("  white-space: nowrap;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".misspellingDisplay{}");
			bw.writeLine(".correctionDisplay{");
			bw.writeLine("  font-weight: bold;");
			bw.writeLine("}");
			bw.writeLine(".rememberDisplay{");
			bw.writeLine("  vertical-align: super;");
			bw.writeLine("  font-size: 7pt;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".rememberLabel {");
			bw.writeLine("  white-space: nowrap;");
			bw.writeLine("  font-size: 8pt;");
			bw.writeLine("  font-family: Verdana, Arial, Helvetica;");
			bw.writeLine("  text-align: left;");
			bw.writeLine("  vertical-align: middle;");
			bw.writeLine("  padding: 0px;");
			bw.writeLine("  margin: 0px;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".closeLink {");
			bw.writeLine("  font-size: 8pt;");
			bw.writeLine("  font-family: Verdana, Arial, Helvetica;");
			bw.writeLine("  color: 000;");
			bw.writeLine("  text-decoration: none;");
			bw.writeLine("}");
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			for (int misspellingHtmlId = 0; misspellingHtmlId < this.misspellingList.size(); misspellingHtmlId++) {
				MisspellingLabel ml = ((MisspellingLabel) this.misspellingList.get(misspellingHtmlId));
				bw.writeLine("  _originalValues['" + misspellingHtmlId + "'] = '" + ml.value.replaceAll("\\'", "\\\\'") + "';");
			}
			bw.writeLine("  ");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  textBox.style.width = (_selectionWidth + 'px');");
			bw.writeLine("  ");
			bw.writeLine("  var i = 0;");
			bw.writeLine("  var misspelling;");
			bw.writeLine("  while (misspelling = document.getElementById('misspellingDisplay' + i)) {");
			bw.writeLine("    misspelling.style.backgroundColor = _notCorrectedColor;");
			bw.writeLine("    misspelling.style.fontWeight = 'bold';");
			bw.writeLine("    ");
			bw.writeLine("    var correction = document.getElementById('correctionDisplay' + i);");
			bw.writeLine("    correction.style.backgroundColor = _correctedColor;");
			bw.writeLine("    ");
			bw.writeLine("    var remember = document.getElementById('rememberDisplay' + i);");
			bw.writeLine("    remember.style.backgroundColor = _rememberColor;");
			bw.writeLine("    ");
			
			bw.writeLine("    var rs = document.getElementById('remember' + i).value;");
			bw.writeLine("    if (rs == 'X')");
			bw.writeLine("      remember.innerHTML = '';");
			bw.writeLine("    else if (rs == 'D')");
			bw.writeLine("      remember.innerHTML = '&nbsp;Dom. Spec.';");
			bw.writeLine("    else if (rs == 'M') {");
			bw.writeLine("      var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.writeLine("      var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].value;");
			bw.writeLine("      remember.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.writeLine("    }");
			bw.writeLine("    else remember.innerHTML = ('&nbsp;' + rs);");
			
			bw.writeLine("    ");
			bw.writeLine("    i++;");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  updateMainLanguage();");
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			bw.writeLine("var _selectionWidth = 180;");
			bw.writeLine("var _correctedColor = '88FF88';");
			bw.writeLine("var _notCorrectedColor = 'FF8888';");
			bw.writeLine("var _rememberColor = 'FFBB88';");
			
			bw.writeLine("var _originalValues = new Object();");
			
			
			bw.writeLine("var _mouseOverCorrection = false;");
			bw.writeLine("function mouseOverCorrection() {");
			bw.writeLine("  _mouseOverCorrection = true;");
			bw.writeLine("}");
			bw.writeLine("function mouseOutCorrection() {");
			bw.writeLine("  _mouseOverCorrection = false;");
			bw.writeLine("}");
			
			bw.writeLine("var _editing = -1;");
			bw.writeLine("function editMisspelling(i) {");
			bw.writeLine("  if ((i == -1) && _mouseOverCorrection) return; // click in correction menu");
			bw.writeLine("  ");
			bw.writeLine("  var correction = document.getElementById('correction');");
			bw.writeLine("  correction.style.display = 'none';");
			bw.writeLine("  ");
			bw.writeLine("  if (i == -1) return;");
			bw.writeLine("  ");
			bw.writeLine("  var oldMisspelling = document.getElementById('misspelling' + _editing);");
			bw.writeLine("  if (oldMisspelling)");
			bw.writeLine("    oldMisspelling .style.border = '';");
			bw.writeLine("  ");
			bw.writeLine("  _editing = i;");
			bw.writeLine("  ");
			bw.writeLine("  //  update correction box");
			bw.writeLine("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.writeLine("  ");
			bw.writeLine("  var selBox = document.getElementById('textSelect');");
			bw.writeLine("  while(selBox.options.length > 2)");
			bw.writeLine("    selBox.options[selBox.options.length - 1] = null;");
			bw.writeLine("  ");
			bw.writeLine("  var optionHolder;");
			bw.writeLine("  var o = 0;");
			bw.writeLine("  var unSelected = true;");
			bw.writeLine("  while (optionHolder = document.getElementById('option' + _editing + '_' + o++)) {");
			bw.writeLine("    selBox.options[selBox.options.length] = new Option(optionHolder.value, optionHolder.value, false, false);");
			bw.writeLine("    if (correctionStore.value == optionHolder.value) {");
			bw.writeLine("      selBox.options[selBox.options.length - 1].selected = true;");
			bw.writeLine("      unSelected = false;");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  var originalText = document.getElementById('originalText');");
			bw.writeLine("  originalText.value = _originalValues[_editing];");
			bw.writeLine("  originalText.text = ('<keep \\'' + _originalValues[_editing] + '\\'>');");
			bw.writeLine("  ");
			bw.writeLine("  var freeText = document.getElementById('freeText');");
			bw.writeLine("  freeText.text = correctionStore.value;");
			bw.writeLine("  freeText.value = correctionStore.value;");
			bw.writeLine("  freeText.selected = unSelected;");
			bw.writeLine("  ");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  textBox.value = document.getElementById('correction' + _editing).value;");
			bw.writeLine("  ");
			bw.writeLine("  // update remember data");
			bw.writeLine("  updateRemember();");
			bw.writeLine("  ");
			bw.writeLine("  // highlight current correction");
			bw.writeLine("  var misspelling = document.getElementById('misspelling' + _editing);");
			bw.writeLine("  ");
			bw.writeLine("  var pos = getMenuPosition(misspelling);");
			bw.writeLine("  var pageWidth = document.body.clientWidth;");
			bw.writeLine("  ");
			bw.writeLine("  correction.style.left = pos.x + 'px';");
			bw.writeLine("  correction.style.top = (pos.y + 2) + 'px';");
			bw.writeLine("  correction.style.display = 'block';");
			bw.writeLine("  if ((pos.x + correction.offsetWidth) > pageWidth)");
			bw.writeLine("    correction.style.left = (pageWidth - correction.offsetWidth) + 'px';");
			bw.writeLine("  ");
			bw.writeLine("  misspelling.style.border = '2pt solid #FF0000';");
			bw.writeLine("  document.getElementById('misspellingDisplay' + _editing).style.backgroundColor = _correctedColor;");
			bw.writeLine("}");
			
			bw.writeLine("function getMenuPosition(el) {");
			bw.writeLine("  var element = el;");
			bw.writeLine("  var left = 0;");
			bw.writeLine("  var top = el.offsetHeight;");
			bw.writeLine("  while(element) {");
			bw.writeLine("    left += element.offsetLeft;");
			bw.writeLine("    top += element.offsetTop;");
			bw.writeLine("    element = element.offsetParent;");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  var pos = new Object();");
			bw.writeLine("  pos.x = left;");
			bw.writeLine("  pos.y = top;");
			bw.writeLine("  return pos;");
			bw.writeLine("}");
			
			bw.writeLine("function updateCorrection() {");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.writeLine("  correctionStore.value = textBox.value;");
			bw.writeLine("  ");
			bw.writeLine("  var misspelling = document.getElementById('misspellingDisplay' + _editing);");
			bw.writeLine("  var correction = document.getElementById('correctionDisplay' + _editing);");
			bw.writeLine("  ");
			bw.writeLine("  if (correctionStore.value == _originalValues[_editing]) {");
			bw.writeLine("    misspelling.style.textDecoration = '';");
			bw.writeLine("    misspelling.style.fontWeight = 'bold';");
			bw.writeLine("    correction.innerHTML = '';");
			bw.writeLine("  }");
			bw.writeLine("  else {");
			bw.writeLine("    misspelling.style.textDecoration = 'line-through';");
			bw.writeLine("    misspelling.style.fontWeight = '';");
			bw.writeLine("    correction.innerHTML = ('&nbsp;' + correctionStore.value);");
			bw.writeLine("  }");
			bw.writeLine("}");
			
			bw.writeLine("function closeCorrection() {");
			bw.writeLine("  correction.style.display = 'none';");
			bw.writeLine("  ");
			bw.writeLine("  var oldMisspelling = document.getElementById('misspelling' + _editing);");
			bw.writeLine("  if (oldMisspelling)");
			bw.writeLine("    oldMisspelling .style.border = '';");
			bw.writeLine("  ");
			bw.writeLine("  _editing = -1;");
			bw.writeLine("  ");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("");
			
			
			bw.writeLine("function freeTextChanged() {");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  var freeText = document.getElementById('freeText');");
			bw.writeLine("  freeText.value = textBox.value;");
			bw.writeLine("  freeText.text = textBox.value;");
			bw.writeLine("  freeText.selected = true;");
			bw.writeLine("  ");
			bw.writeLine("  updateCorrection();");
			bw.writeLine("  updateRemember();");
			bw.writeLine("}");
			
			bw.writeLine("function selectionChanged() {");
			bw.writeLine("  var selBox = document.getElementById('textSelect');");
			bw.writeLine("  if (selBox.selectedIndex == 0) {");
			bw.writeLine("    var originalText = document.getElementById('originalText');");
			bw.writeLine("    var freeText = document.getElementById('freeText');");
			bw.writeLine("    freeText.value = originalText.value;");
			bw.writeLine("    freeText.text = originalText.value;");
			bw.writeLine("    freeText.selected = true;");
			bw.writeLine("  }");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  textBox.value = selBox.options[selBox.selectedIndex].value;");
			bw.writeLine("  hideSelection();");
			bw.writeLine("  ");
			bw.writeLine("  updateCorrection();");
			bw.writeLine("  updateRemember();");
			bw.writeLine("}");
			
			
			bw.writeLine("var _selectionOpen = false;");
			bw.writeLine("function showSelection() {");
			bw.writeLine("  var selBox = document.getElementById('textSelect');");
			bw.writeLine("  selBox.style.width = ((_selectionWidth + 20) + 'px');");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  textBox.style.display = 'none';");
			bw.writeLine("}");
			bw.writeLine("function clickSelection() {");
			bw.writeLine("  _selectionOpen = true;");
			bw.writeLine("}");
			bw.writeLine("function exitSelection() {");
			bw.writeLine("  if (_selectionOpen)");
			bw.writeLine("    _selectionOpen = false;");
			bw.writeLine("  else hideSelection();");
			bw.writeLine("}");
			bw.writeLine("function hideSelection() {");
			bw.writeLine("  _selectionOpen = false;");
			bw.writeLine("  var selBox = document.getElementById('textSelect');");
			bw.writeLine("  selBox.style.width = '20px';");
			bw.writeLine("  var textBox = document.getElementById('textInput');");
			bw.writeLine("  textBox.style.display = '';");
			bw.writeLine("}");
			
			
			bw.writeLine("function updateMainLanguage() {");
			bw.writeLine("  var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.writeLine("  var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].text;");
			bw.writeLine("  ");
			bw.writeLine("  var mainLanguageDisplay = document.getElementById('mainLanguageDisplay');");
			bw.writeLine("  mainLanguageDisplay.innerHTML = mainLanguage;");
			bw.writeLine("  ");
			bw.writeLine("  var i = -1;");
			bw.writeLine("  var rememberDisplay;");
			bw.writeLine("  while(rememberDisplay = document.getElementById('rememberDisplay' + ++i)) {");
			bw.writeLine("    var rememberStore = document.getElementById('remember' + i);");
			bw.writeLine("    if (rememberStore.value == 'M')");
			bw.writeLine("      rememberDisplay.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.writeLine("  }");
			bw.writeLine("}");
			
			
			bw.writeLine("var _xmdl = 'XMDL';");
			bw.writeLine("function updateRemember() {");
			bw.writeLine("  var remember = document.getElementById('remember');");
			bw.writeLine("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.writeLine("  ");
			bw.writeLine("  if (correctionStore.value.indexOf(' ') == -1) {");
			bw.writeLine("    var rememberStore = document.getElementById('remember' + _editing);");
			bw.writeLine("    var rs = rememberStore.value;");
			bw.writeLine("    ");
			bw.writeLine("    for (var r = 0; r < _xmdl.length; r++) {");
			bw.writeLine("      var idSuffix = _xmdl.substring(r, (r+1));");
			bw.writeLine("      var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.writeLine("      rememberButton.checked = (idSuffix == rs);");
			bw.writeLine("      rememberButton.disabled = false;");
			bw.writeLine("      var rememberLabel = document.getElementById('rememberLabel' + idSuffix);");
			bw.writeLine("      rememberLabel.style.color = '000000';");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    if (rs.length == 1)");
			bw.writeLine("      rememberChanged(rs);");
			bw.writeLine("    ");
			bw.writeLine("    else {");
			bw.writeLine("      document.getElementById('rememberL').checked = true;");
			bw.writeLine("      var rememberLanguage = document.getElementById('rememberLanguage');");
			bw.writeLine("      for (var r = 0; r < rememberLanguage.options.length; r++) {");
			bw.writeLine("        if (rs == rememberLanguage.options[r].value)");
			bw.writeLine("          rememberLanguage.options[r].selected = true;");
			bw.writeLine("      }");
			bw.writeLine("      rememberChanged('L');");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  else {");
			bw.writeLine("    for (var r = 0; r < _xmdl.length; r++) {");
			bw.writeLine("      var idSuffix = _xmdl.substring(r, (r+1));");
			bw.writeLine("      var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.writeLine("      rememberButton.checked = (idSuffix == 'X');");
			bw.writeLine("      rememberButton.disabled = true;");
			bw.writeLine("      var rememberLabel = document.getElementById('rememberLabel' + idSuffix);");
			bw.writeLine("      rememberLabel.style.color = '666666';");
			bw.writeLine("    }");
			bw.writeLine("    rememberChanged('X');");
			bw.writeLine("  }");
			bw.writeLine("}");
			
			bw.writeLine("function rememberChanged(rs) {");
			bw.writeLine("  var rememberStore = document.getElementById('remember' + _editing);");
			bw.writeLine("  var rememberDisplay = document.getElementById('rememberDisplay' + _editing);");
			bw.writeLine("  var rememberLanguage = document.getElementById('rememberLanguage');");
			bw.writeLine("  ");
			bw.writeLine("  for (var r = 0; r < _xmdl.length; r++) {");
			bw.writeLine("    var idSuffix = _xmdl.substring(r, (r+1));");
			bw.writeLine("    var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.writeLine("    rememberButton.checked = (idSuffix == rs);");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  if (rs == 'L') {");
			bw.writeLine("    var language = rememberLanguage.options[rememberLanguage.selectedIndex].value;");
			bw.writeLine("    rememberStore.value = language;");
			bw.writeLine("    rememberDisplay.innerHTML = ('&nbsp;' + language);");
			bw.writeLine("    ");
			bw.writeLine("    rememberLanguage.disabled = false;");
			bw.writeLine("  }");
			bw.writeLine("  else {");
			bw.writeLine("    rememberStore.value = rs;");
			bw.writeLine("    rememberLanguage.disabled = true;");
			bw.writeLine("    ");
			bw.writeLine("    if (rs == 'X')");
			bw.writeLine("      rememberDisplay.innerHTML = '';");
			bw.writeLine("    else if (rs == 'D')");
			bw.writeLine("      rememberDisplay.innerHTML = '&nbsp;Dom. Spec.';");
			bw.writeLine("    else if (rs == 'M') {");
			bw.writeLine("      var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.writeLine("      var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].value;");
			bw.writeLine("      rememberDisplay.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("}");
			
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			bw.writeLine("<div id=\"correction\" style=\"border: 1px solid #888888; background-color: bbb; display: none; position: absolute; align: right;\" onmouseover=\"mouseOverCorrection();\" onmouseout=\"mouseOutCorrection();\">");
			bw.writeLine("<table style=\"border: 0;\">");
			
			bw.writeLine("<tr>");
			
			bw.write("<td style=\"white-space: nowrap; text-align: left; padding: 0px 0px 8px; margin: 0px;\">");
			bw.write("<input id=\"textInput\" name=\"textInput\" onkeyup=\"freeTextChanged();\" type=\"text\" value=\"\" style=\"margin: 0px; padding 0px;\">");
			bw.write("<select id=\"textSelect\" name=\"textSelect\" onmouseover=\"showSelection();\" onclick=\"clickSelection();\" onmouseout=\"exitSelection();\" onblur=\"hideSelection();\" onchange=\"selectionChanged();\" style=\"margin: 0px; padding: 0px; width: 20px;\">");
			bw.newLine();
			bw.writeLine("  <option value=\"\" id=\"originalText\"></option>");
			bw.writeLine("  <option value=\"\" id=\"freeText\"></option>");
			bw.writeLine("</select>");
			bw.writeLine("</td>");
			
			bw.write("<td style=\"text-align: right; vertical-align: top; padding: 0px 1px 1px; margin: 0px;\">");
			bw.write("<a class=\"closeLink\" href=\"#\" onclick=\"return closeCorrection();\">close&nbsp;[X]</a>");
			bw.write("</td>");
			bw.newLine();
			
			bw.writeLine("</tr>");
			
			bw.writeLine("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.writeLine("<input type=\"radio\" name=\"remember\" id=\"rememberX\" onclick=\"rememberChanged('X');\" value=\"X\"> <span id=\"rememberLabelX\" onclick=\"rememberChanged('X');\"><b>Do not learn</b> this word or its correction</span>");
			bw.writeLine("</td></tr>");
			
			bw.writeLine("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.writeLine("<input type=\"radio\" name=\"remember\" id=\"rememberM\" onclick=\"rememberChanged('M');\" value=\"M\"> <span id=\"rememberLabelM\" onclick=\"rememberChanged('M');\">Learn as word in <b>main language</b> (<span id=\"mainLanguageDisplay\">English</span>)</span>");
			bw.writeLine("</td></tr>");
			
			bw.writeLine("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.writeLine("<input type=\"radio\" name=\"remember\" id=\"rememberD\" onclick=\"rememberChanged('D');\" value=\"D\"> <span id=\"rememberLabelD\" onclick=\"rememberChanged('D');\">Learn as <b>domain specific</b> term</span>");
			bw.writeLine("</td></tr>");
			
			bw.writeLine("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.writeLine("<input type=\"radio\" name=\"remember\" id=\"rememberL\" onclick=\"rememberChanged('L');\" value=\"L\"> <span id=\"rememberLabelL\" onclick=\"rememberChanged('L');\">Learn as <select name=\"rememberLanguage\" id=\"rememberLanguage\" onchange=\"rememberChanged('L');\">");
			for (int l = 0; l < allLanguages.length; l++) {
				bw.writeLine("  <option value=\"" + allLanguages[l] + "\"" + (allLanguages[l].equals(this.mainLanguage) ? " selected" : "") + ">" + allLanguages[l] + "</option>");
			}
			bw.writeLine("</select> word</span></td></tr>");
			bw.writeLine("</table>");
			bw.writeLine("</div>");
			
			
			bw.writeLine("<table class=\"mainTable\">");
			bw.writeLine("  <tr style=\"background-color: bbb;\">");
			bw.write("  <td style=\"font-weight: bold; font-size: 10pt;\" class=\"mainTableCell\">This paragraph is (mostly) written in " );
			bw.write("<select id=\"mainLanguage\" name=\"mainLanguage\" onchange=\"updateMainLanguage();\">");
			bw.newLine();
			for (int l = 0; l < allLanguages.length; l++) {
				bw.writeLine("  <option value=\"" + allLanguages[l] + "\"" + (allLanguages[l].equals(this.mainLanguage) ? " selected" : "") + ">" + allLanguages[l] + "</option>");
			}
			bw.writeLine("  </select></td>");
			bw.writeLine("  <td style=\"font-weight: bold; font-size: 10pt; text-align: right;\" class=\"mainTableCell\">This paragraph requires full text editing <input type=\"checkbox\" name=\"fullTextEdit\" value=\"T\"></td>");
			bw.writeLine("  </tr>");
			bw.writeLine("  <tr>");
			bw.writeLine("    <td class=\"mainTableCell\" colspan=\"2\" style=\"line-height: 1.8;\">");
			
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
				
				if (tl.hasSpace()) bw.writeLine(" ");
			}
			bw.writeLine("    </td>");
			bw.writeLine("  </tr>");
			bw.writeLine("</table>");
			
			for (misspellingHtmlId = 0; misspellingHtmlId < this.misspellingList.size(); misspellingHtmlId++) {
				MisspellingLabel ml = ((MisspellingLabel) this.misspellingList.get(misspellingHtmlId));
				bw.writeLine("<input type=\"hidden\" id=\"remember" + misspellingHtmlId + "\" name=\"" + ml.misspellingId + "_remember\" value=\"" + ml.rememberLanguage + "\">");
				bw.writeLine("<input type=\"hidden\" id=\"correction" + misspellingHtmlId + "\" name=\"" + ml.misspellingId + "_correction\" value=\"" + ml.correction + "\">");
				for (int s = 0; s < ml.suggestions.length; s++) {
					bw.writeLine("<input type=\"hidden\" id=\"option" + misspellingHtmlId + "_" + s + "\" value=\"" + ml.suggestions[s] + "\">");
				}
			}
			
			if (bw != out)
				bw.flush();
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
}