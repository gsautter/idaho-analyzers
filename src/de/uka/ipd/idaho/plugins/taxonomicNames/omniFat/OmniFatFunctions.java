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

package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.DataRule;
import de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.RankGroup;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Functional part of the OmniFAT algorithm.
 * 
 * @author sautter
 */
public class OmniFatFunctions {
	
	//	TODO implement auto-learning in places where candidates are promoted
	
	public static void tagBaseEpithets(MutableAnnotation data, OmniFAT omniFat) {
		tagBaseEpithets(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void tagBaseEpithets(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	collect text tokens
		boolean inSentence = true;
		for (int t = 0; t < data.size(); t++) {
			String token = data.valueAt(t);
			if (Gamta.isWord(token) || Gamta.isNumber(token)) {
				docData.addTextToken(token);
				if (inSentence)
					docData.addInSentenceTextToken(token);
			}
			inSentence = !Gamta.isSentenceEnd(token);
		}
		
		//	get rank groups
		OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
		
		//	prepare filters
		Dictionary stopWords = docData.omniFat.getStopWords();
		OmniFatDictionary[] authorNameDictionaries = docData.omniFat.getAuthorNameDictionaries();
		
		//	annotate epithets for rank group lexicons
		for (int rg = 0; rg < rankGroups.length; rg++) {
			
			//	de-duplicate in rank group
			Set rankGroupMatchKeys = new HashSet();
			
			//	collect possible abbreviations
			Properties rankGroupEpithetStates = new Properties();
			
			//	do individual ranks
			OmniFAT.Rank[] ranks = rankGroups[rg].getRanks();
			for (int r = 0; r < ranks.length; r++) {
				
				//	de-duplicate in rank
				Set rankMatchKeys = new HashSet();
				
				//	collect possible abbreviations
				Properties rankEpithetStates = new Properties();
				
				//	get negative patterns & lexicons
				OmniFatPattern[] negativePatterns = ranks[r].getNegativePatterns();
				OmniFatDictionary[] rNegativeDictionaries = ranks[r].getNegativeDictionaries();
				OmniFatDictionary[] negativeDictionaries = new OmniFatDictionary[authorNameDictionaries.length + rNegativeDictionaries.length];
				System.arraycopy(authorNameDictionaries, 0, negativeDictionaries, 0, authorNameDictionaries.length);
				System.arraycopy(rNegativeDictionaries, 0, negativeDictionaries, authorNameDictionaries.length, rNegativeDictionaries.length);
				
				//	apply precision patterns
				OmniFatPattern[] precisionPatterns = ranks[r].getPrecisionPatterns();
				for (int p = 0; p < precisionPatterns.length; p++) {
					
					//	extract and annotate epithets
					Annotation[] epithets = Gamta.extractAllMatches(data, precisionPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
					for (int e = 0; e < epithets.length; e++) {
						String epithetKey = getKey(epithets[e]);
						String epithetValue = epithets[e].getValue();
						int epithetValueOffset = 0;
						if (epithets[e].size() != 1)
							for (int v = 0; v < epithets[e].size(); v++) {
								String value = epithets[e].valueAt(v);
								if (Gamta.isWord(value) && !ranks[r].getLabels().lookup(value) && !ranks[r].getLabels().lookup(value + ".") && (Gamta.isCapitalizedWord(value) || !ranks[r].getRankGroup().isEpithetsCapitalized()) && (Gamta.isLowerCaseWord(value) || !ranks[r].getRankGroup().isEpithetsLowerCase())) {
									epithetValue = value;
									epithetValueOffset = v;
								}
							}
						
						//	remember match & ignore duplicates
						rankGroupMatchKeys.add(epithetKey);
						if (!rankMatchKeys.add(epithetKey))
							continue;
						
						//	ignore abbreviations (they come later)
						if (epithetValue.length() < 3)
							continue;
						
						//	ignore stop words
						if (stopWords.lookup(epithetValue, false)) {
							docData.log(("Ignoring stop word match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
						if (docData.omniFat.doStemmingNegativeLookup(epithetValue)) {
							docData.log(("Ignoring flected match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						epithetValue = (epithetValue.substring(0,1) + epithetValue.substring(1).toLowerCase());
						
						Annotation epithet = data.addAnnotation(OmniFAT.BASE_EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
						epithet.setAttribute(OmniFAT.RANK_ATTRIBUTE, ranks[r].getName());
						epithet.setAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + ranks[r].getOrderNumber()));
						epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
						epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
						epithet.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.PRECISION_EVIDENCE);
						epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, precisionPatterns[p].name);
						epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, epithetValue);
						epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, epithetValue);
						epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
						
						epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
						rankEpithetStates.setProperty(epithetValue, OmniFAT.POSITIVE_STATE);
						rankGroupEpithetStates.setProperty(epithetValue, OmniFAT.POSITIVE_STATE);
					}
				}
				
				//	apply lexicons (trusted ones first)
				OmniFatDictionary[] epithetDictionaries = ranks[r].getPositiveDictionaries();
				Arrays.sort(epithetDictionaries, trustedFirstDictionaryOrder);
				for (int d = 0; d < epithetDictionaries.length; d++) {
					
					//	extract and annotate epithets
					Annotation[] epithets = Gamta.extractAllContained(data, epithetDictionaries[d], 1, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
					for (int e = 0; e < epithets.length; e++) {
						String epithetKey = getKey(epithets[e]);
						String epithetValue = epithets[e].getValue();
						
						//	remember match & ignore duplicates
						rankGroupMatchKeys.add(epithetKey);
						if (!rankMatchKeys.add(epithetKey))
							continue;
						
						//	ignore abbreviations (they come later)
						if (epithetValue.length() < 3)
							continue;
						
						//	ignore stop words
						if (stopWords.lookup(epithetValue, false)) {
							docData.log(("Ignoring stop word match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	do negative filters for untrusted lexicons
						if (!epithetDictionaries[d].trusted) {
							
							//	do sure negative lookup
							if (!epithetDictionaries[d].trusted && rankGroups[rg].isEpithetsCapitalized() && !docData.isInSentenceTextToken(epithetValue) && docData.isTextToken(epithetValue.toLowerCase())) {
								docData.log(("Ignoring sure negative match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
								continue;
							}
							
							//	do negative patterns
							OmniFatPattern negativePattern = null;
							for (int n = 0; n < negativePatterns.length; n++)
								if (negativePatterns[n].matches(epithetValue)) {
									negativePattern = negativePatterns[n];
									n = negativePatterns.length;
								}
							if (negativePattern != null) {
								docData.log(("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
								continue;
							}
							
							//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
							if (docData.omniFat.doStemmingNegativeLookup(epithetValue)) {
								docData.log(("Ignoring flected match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
								continue;
							}
						}
						
						epithetValue = (epithetValue.substring(0,1) + epithetValue.substring(1).toLowerCase());
						
						Annotation epithet = data.addAnnotation(OmniFAT.BASE_EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
						epithet.setAttribute(OmniFAT.RANK_ATTRIBUTE, ranks[r].getName());
						epithet.setAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + ranks[r].getOrderNumber()));
						epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
						epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
						epithet.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.LEXICON_EVIDENCE);
						epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, epithetValue);
						epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, epithetValue);
						epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + epithets[e].getStartIndex()));
						
						String evidenceDetail = epithetDictionaries[d].name;
						
						boolean caseMatch = true;
						if (!epithetDictionaries[d].lookup(epithetValue, true)) {
							caseMatch = false;
							evidenceDetail += "(CI)";
						}
						
						boolean negative = false;
						for (int n = 0; n < negativeDictionaries.length; n++)
							if (negativeDictionaries[n].lookup(epithetValue, true)) {
								negative = true;
								evidenceDetail += ("/" + negativeDictionaries[n].name);
								n = negativeDictionaries.length;
							}
						
						String state;
						if (negative || !caseMatch)
							state = OmniFAT.AMBIGUOUS_STATE;
						else if (epithetDictionaries[d].trusted)
							state = OmniFAT.POSITIVE_STATE;
						else state = OmniFAT.LIKELY_STATE;
						
						epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, state);
						epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, evidenceDetail);
						if (!rankEpithetStates.containsKey(epithetValue))
							rankEpithetStates.setProperty(epithetValue, state);
						if (!rankGroupEpithetStates.containsKey(epithetValue))
							rankGroupEpithetStates.setProperty(epithetValue, state);
					}
				}
				
				//	apply recall patterns
				OmniFatPattern[] recallPatterns = ranks[r].getRecallPatterns();
				for (int p = 0; p < recallPatterns.length; p++) {
					
					//	extract and annotate epithets
					Annotation[] epithets = Gamta.extractAllMatches(data, recallPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
					for (int e = 0; e < epithets.length; e++) {
						String epithetKey = getKey(epithets[e]);
						String epithetValue = epithets[e].getValue();
						int epithetValueOffset = 0;
						if (epithets[e].size() != 1)
							for (int v = 0; v < epithets[e].size(); v++) {
								String value = epithets[e].valueAt(v);
								if (Gamta.isWord(value) && !ranks[r].getLabels().lookup(value) && !ranks[r].getLabels().lookup(value + ".") && (Gamta.isCapitalizedWord(value) || !ranks[r].getRankGroup().isEpithetsCapitalized()) && (Gamta.isLowerCaseWord(value) || !ranks[r].getRankGroup().isEpithetsLowerCase())) {
									epithetValue = value;
									epithetValueOffset = v;
								}
							}
						
						//	remember match & ignore duplicates
						rankGroupMatchKeys.add(epithetKey);
						if (!rankMatchKeys.add(epithetKey))
							continue;
						
						//	ignore abbreviations (they come later)
						if (epithetValue.length() < 3)
							continue;
						
						//	ignore stop words
						if (stopWords.lookup(epithetValue, false)) {
							docData.log(("Ignoring stop word match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	do sure negative lookup
						if (rankGroups[rg].isEpithetsCapitalized() && !docData.isInSentenceTextToken(epithetValue) && docData.isTextToken(epithetValue.toLowerCase())) {
							docData.log(("Ignoring sure negative match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	do suffix diff
						if (ranks[r].getRankGroup().doSuffixDiffLookup(epithetValue)) {
							docData.log(("Ignoring epithet for suffix diff: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	do negative patterns
						OmniFatPattern negativePattern = null;
						for (int n = 0; n < negativePatterns.length; n++)
							if (negativePatterns[n].matches(epithetValue)) {
								negativePattern = negativePatterns[n];
								n = negativePatterns.length;
							}
						if (negativePattern != null) {
							docData.log(("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
						if (docData.omniFat.doStemmingNegativeLookup(epithetValue)) {
							docData.log(("Ignoring flected match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						epithetValue = (epithetValue.substring(0,1) + epithetValue.substring(1).toLowerCase());
						
						Annotation epithet = data.addAnnotation(OmniFAT.BASE_EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
						epithet.setAttribute(OmniFAT.RANK_ATTRIBUTE, ranks[r].getName());
						epithet.setAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + ranks[r].getOrderNumber()));
						epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
						epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
						epithet.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.RECALL_EVIDENCE);
						epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, epithetValue);
						epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, epithetValue);
						epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
						
						String state = rankEpithetStates.getProperty(epithetValue);
						String evidenceDetail = recallPatterns[p].name;
						if (state == null) {
							state = OmniFAT.UNCERTAIN_STATE;
							for (int n = 0; n < negativeDictionaries.length; n++)
								if (negativeDictionaries[n].lookup(epithetValue, true)) {
									state = OmniFAT.NEGATIVE_STATE;
									evidenceDetail = (recallPatterns[p].name + "/" + negativeDictionaries[n].name);
									n = negativeDictionaries.length;
								}
						}
						epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, state);
						epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, evidenceDetail);
					}
				}
				
				//	DO ABBREVIATIONS ONLY ON RANK GROUP LEVEL - THEY ARE TOO AMBIGUOUS
			}
			
			//	get negative patterns & lexicons
			OmniFatPattern[] negativePatterns = rankGroups[rg].getNegativePatterns();
			OmniFatDictionary[] rgNegativeDictionaries = rankGroups[rg].getNegativeDictionaries();
			OmniFatDictionary[] negativeDictionaries = new OmniFatDictionary[authorNameDictionaries.length + rgNegativeDictionaries.length];
			System.arraycopy(authorNameDictionaries, 0, negativeDictionaries, 0, authorNameDictionaries.length);
			System.arraycopy(rgNegativeDictionaries, 0, negativeDictionaries, authorNameDictionaries.length, rgNegativeDictionaries.length);
			
			//	apply precision patterns
			OmniFatPattern[] precisionPatterns = rankGroups[rg].getPrecisionPatterns();
			for (int p = 0; p < precisionPatterns.length; p++) {
				
				//	extract and annotate epithets
				Annotation[] epithets = Gamta.extractAllMatches(data, precisionPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
				for (int e = 0; e < epithets.length; e++) {
					String epithetKey = getKey(epithets[e]);
					String epithetValue = epithets[e].getValue();
					int epithetValueOffset = 0;
					if (epithets[e].size() != 1)
						for (int v = 0; v < epithets[e].size(); v++) {
							String value = epithets[e].valueAt(v);
							if (Gamta.isWord(value) && (Gamta.isCapitalizedWord(value) || !rankGroups[rg].isEpithetsCapitalized()) && (Gamta.isLowerCaseWord(value) || !rankGroups[rg].isEpithetsLowerCase())) {
								epithetValue = value;
								epithetValueOffset = v;
							}
						}
					
					//	remember match & ignore duplicates
					if (!rankGroupMatchKeys.add(epithetKey))
						continue;
					
					//	ignore abbreviations (they come later)
					if (epithetValue.length() < 3)
						continue;
					
					//	ignore stop words
					if (stopWords.lookup(epithetValue, false)) {
						docData.log(("Ignoring stop word lexicon match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
					if (docData.omniFat.doStemmingNegativeLookup(epithetValue)) {
						docData.log(("Ignoring flected lexicon match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					epithetValue = (epithetValue.substring(0,1) + epithetValue.substring(1).toLowerCase());
					
					Annotation epithet = data.addAnnotation(OmniFAT.BASE_EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
					epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
					epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
					epithet.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.PRECISION_EVIDENCE);
					epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, precisionPatterns[p].name);
					epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, epithetValue);
					epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, epithetValue);
					epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
					
					epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
					rankGroupEpithetStates.setProperty(epithetValue, OmniFAT.POSITIVE_STATE);
				}
			}
			
			//	apply lexicons (trusted ones first)
			OmniFatDictionary[] epithetDictionaries = rankGroups[rg].getPositiveDictionaries();
			Arrays.sort(epithetDictionaries, trustedFirstDictionaryOrder);
			for (int d = 0; d < epithetDictionaries.length; d++) {
				
				//	extract and annotate epithets
				Annotation[] epithets = Gamta.extractAllContained(data, epithetDictionaries[d], 1, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
				for (int e = 0; e < epithets.length; e++) {
					String epithetKey = getKey(epithets[e]);
					String epithetValue = epithets[e].getValue();
					
					//	remember match & ignore duplicates
					if (!rankGroupMatchKeys.add(epithetKey))
						continue;
					
					//	ignore abbreviations (they come later)
					if (epithetValue.length() < 3)
						continue;
					
					//	ignore stop words
					if (stopWords.lookup(epithetValue, false)) {
						docData.log(("Ignoring stop word lexicon match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					//	do negative filters for untrusted lexicons
					if (!epithetDictionaries[d].trusted) {
						
						//	do sure negative lookup
						if (!epithetDictionaries[d].trusted && rankGroups[rg].isEpithetsCapitalized() && !docData.isInSentenceTextToken(epithetValue) && docData.isTextToken(epithetValue.toLowerCase())) {
							docData.log(("Ignoring sure negative match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	do negative patterns
						OmniFatPattern negativePattern = null;
						for (int n = 0; n < negativePatterns.length; n++)
							if (negativePatterns[n].matches(epithetValue)) {
								negativePattern = negativePatterns[n];
								n = negativePatterns.length;
							}
						if (negativePattern != null) {
							docData.log(("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
						
						//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
						if (docData.omniFat.doStemmingNegativeLookup(epithetValue)) {
							docData.log(("Ignoring flected lexicon match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
							continue;
						}
					}
					
					epithetValue = (epithetValue.substring(0,1) + epithetValue.substring(1).toLowerCase());
					
					Annotation epithet = data.addAnnotation(OmniFAT.BASE_EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
					epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
					epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
					epithet.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.LEXICON_EVIDENCE);
					epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, epithetValue);
					epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, epithetValue);
					epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + epithets[e].getStartIndex()));
					
					String evidenceDetail = epithetDictionaries[d].name;
					
					boolean caseMatch = true;
					if (!epithetDictionaries[d].lookup(epithetValue, true)) {
						caseMatch = false;
						evidenceDetail += "(CI)";
					}
					
					boolean negative = false;
					for (int n = 0; n < negativeDictionaries.length; n++)
						if (negativeDictionaries[n].lookup(epithetValue, true)) {
							negative = true;
							evidenceDetail += ("/" + negativeDictionaries[n].name);
							n = negativeDictionaries.length;
						}
					
					String state;
					if (negative || !caseMatch)
						state = OmniFAT.AMBIGUOUS_STATE;
					else if (epithetDictionaries[d].trusted)
						state = OmniFAT.POSITIVE_STATE;
					else state = OmniFAT.LIKELY_STATE;
					
					epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, state);
					epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, evidenceDetail);
					if (!rankGroupEpithetStates.containsKey(epithetValue))
						rankGroupEpithetStates.setProperty(epithetValue, state);
				}
			}
			
			//	apply recall patterns
			OmniFatPattern[] recallPatterns = rankGroups[rg].getRecallPatterns();
			for (int p = 0; p < recallPatterns.length; p++) {
				
				//	extract and annotate epithets
				Annotation[] epithets = Gamta.extractAllMatches(data, recallPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
				for (int e = 0; e < epithets.length; e++) {
					String epithetKey = getKey(epithets[e]);
					String epithetValue = epithets[e].getValue();
					int epithetValueOffset = 0;
					if (epithets[e].size() != 1)
						for (int v = 0; v < epithets[e].size(); v++) {
							String value = epithets[e].valueAt(v);
							if (Gamta.isWord(value) && (Gamta.isCapitalizedWord(value) || !rankGroups[rg].isEpithetsCapitalized()) && (Gamta.isLowerCaseWord(value) || !rankGroups[rg].isEpithetsLowerCase())) {
								epithetValue = value;
								epithetValueOffset = v;
							}
						}
					
					//	remember match & ignore duplicates
					if (!rankGroupMatchKeys.add(epithetKey))
						continue;
					
					//	ignore abbreviations (they come later)
					if (epithetValue.length() < 3)
						continue;
					
					//	ignore stop words
					if (stopWords.lookup(epithetValue, false)) {
						docData.log(("Ignoring stop word lexicon match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					//	do sure negative lookup
					if (rankGroups[rg].isEpithetsCapitalized() && !docData.isInSentenceTextToken(epithetValue) && docData.isTextToken(epithetValue.toLowerCase())) {
						docData.log(("Ignoring sure negative match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					//	do suffix diff
					if (rankGroups[rg].doSuffixDiffLookup(epithetValue)) {
						docData.log(("Ignoring epithet for suffix diff: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					//	do negative patterns
					OmniFatPattern negativePattern = null;
					for (int n = 0; n < negativePatterns.length; n++)
						if (negativePatterns[n].matches(epithetValue)) {
							negativePattern = negativePatterns[n];
							n = negativePatterns.length;
						}
					if (negativePattern != null) {
						docData.log(("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
					if (docData.omniFat.doStemmingNegativeLookup(epithetValue)) {
						docData.log(("Ignoring flected lexicon match: " + epithetValue), OmniFAT.DEBUG_LEVEL_DETAILS);
						continue;
					}
					
					epithetValue = (epithetValue.substring(0,1) + epithetValue.substring(1).toLowerCase());
					
					Annotation epithet = data.addAnnotation(OmniFAT.BASE_EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
					epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
					epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
					epithet.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.RECALL_EVIDENCE);
					epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, recallPatterns[p].name);
					epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, epithetValue);
					epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, epithetValue);
					epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
					
					String state = rankGroupEpithetStates.getProperty(epithetValue);
					String evidenceDetail = recallPatterns[p].name;
					if (state == null) {
						state = OmniFAT.UNCERTAIN_STATE;
						for (int n = 0; n < negativeDictionaries.length; n++)
							if (negativeDictionaries[n].lookup(epithetValue, true)) {
								state = OmniFAT.NEGATIVE_STATE;
								evidenceDetail = (recallPatterns[p].name + "/" + negativeDictionaries[n].name);
								n = negativeDictionaries.length;
							}
					}
					epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, state);
					epithet.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, evidenceDetail);
				}
			}
			
			//	DO ABBREVIATIONS ONLY AFTER EPITHET FILTERING - THERE'S TOO MUCH AMBIGUITY BEFORE THAT
		}
		
		
		//	filter out duplicate base epithets, keeping the one(s) with the most certain state
		Annotation[] epithets = data.getAnnotations(OmniFAT.BASE_EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			int l = 1;
			while (((e+l) < epithets.length) && AnnotationUtils.equals(epithets[e], epithets[e+l], false))
				l++;
			if (l != 1) {
				Annotation[] duplicates = new Annotation[l];
				docData.log(("Inspecting block of duplicate epithets:"), OmniFAT.DEBUG_LEVEL_ALL);
				for (int d = 0; d < l; d++) {
					duplicates[d] = epithets[e+d];
					docData.log(("   " + ((String) epithets[e+d].getAttribute(OmniFAT.STATE_ATTRIBUTE)) + ": " + epithets[e+d].toXML()), OmniFAT.DEBUG_LEVEL_ALL);
				}
				Arrays.sort(duplicates, baseEpithetStateAnnotationOrder);
				for (int d = 0; d < duplicates.length; d++)
					if (baseEpithetStateAnnotationOrder.compare(duplicates[0], duplicates[d]) != 0) {
						docData.log(("  ==> removing inferior duplicate: " + duplicates[d].toXML()), OmniFAT.DEBUG_LEVEL_ALL);
						data.removeAnnotation(duplicates[d]);
					}
				e += (duplicates.length-1);
			}
		}
		
		
		//	collect possible abbreviations
		epithets = data.getAnnotations(OmniFAT.BASE_EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
			String epithetValue = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
			docData.addEpithet(rankGroup, epithetValue);
		}
		
		//	annotate abbreviations
		for (int rg = 0; rg < rankGroups.length; rg++) {
			Dictionary rankGroupAbbreviationList = docData.getAbbreviationList(rankGroups[rg].getName());
			if (rankGroupAbbreviationList == null)
				continue;
			
			//	extract abbreviations
			Annotation[] abbreviatedEpithets = Gamta.extractAllContained(data, rankGroupAbbreviationList, 2, true);
			for (int ae = 0; ae < abbreviatedEpithets.length; ae++) {
				String abbreviatedEpithetValue = abbreviatedEpithets[ae].getValue();
				
				//	ignore stop words
				if ((abbreviatedEpithetValue.length() > 1) && stopWords.lookup(abbreviatedEpithetValue, false))
					continue;
				
				abbreviatedEpithetValue = (abbreviatedEpithetValue.substring(0,1) + abbreviatedEpithetValue.substring(1).toLowerCase());
				
				Annotation epithet = data.addAnnotation(OmniFAT.ABBREVIATED_EPITHET_TYPE, abbreviatedEpithets[ae].getStartIndex(), abbreviatedEpithets[ae].size());
				epithet.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, rankGroups[rg].getName());
				epithet.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + rankGroups[rg].getOrderNumber()));
				epithet.setAttribute(OmniFAT.STRING_ATTRIBUTE, abbreviatedEpithetValue);
				epithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, abbreviatedEpithetValue);
				epithet.setAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE, ("" + abbreviatedEpithets[ae].getStartIndex()));
				
				epithet.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.ABBREVIATED_STATE);
			}
		}
		
		
		//	transfer states to uncertain multi-token pattern matches
		HashMap rankGroupEpithetStateSets = new HashMap();
		ArrayList multiTokenEpithets = new ArrayList();
		
		//	gather data
		epithets = data.getAnnotations(OmniFAT.BASE_EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			if (epithets[e].size() == 1) {
				Properties rankGroupEpithetStateSet = ((Properties) rankGroupEpithetStateSets.get(epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE)));
				if (rankGroupEpithetStateSet == null) {
					rankGroupEpithetStateSet = new Properties();
					rankGroupEpithetStateSets.put(epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE), rankGroupEpithetStateSet);
				}
				rankGroupEpithetStateSet.setProperty(((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE)), ((String) epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.UNCERTAIN_STATE)));
			}
			else multiTokenEpithets.add(epithets[e]);
		}
		
		//	transfer states
		epithets = ((Annotation[]) multiTokenEpithets.toArray(new Annotation[multiTokenEpithets.size()]));
		for (int e = 0; e < epithets.length; e++) {
			Properties rankGroupEpithetStateSet = ((Properties) rankGroupEpithetStateSets.get(epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE)));
			if (rankGroupEpithetStateSet != null) {
				String epithetState = ((String) epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE));
				String valueState = rankGroupEpithetStateSet.getProperty((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				if ((valueState != null) && (baseEpithetStateOrder.compare(valueState, epithetState) < 0)) {
					docData.log(("Promoting value state '" + valueState + "' to multi token match:\n  " + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, valueState);
				}
			}
		}
	}
	
	private static Comparator trustedFirstDictionaryOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			OmniFatDictionary ofd1 = ((OmniFatDictionary) o1);
			OmniFatDictionary ofd2 = ((OmniFatDictionary) o2);
			int c = 0;
			if (ofd1.trusted) c--;
			if (ofd2.trusted) c++;
			return c;
		}
	};
	
	private static Comparator baseEpithetStateOrder = new Comparator() {
		private HashMap stateNumbers = new HashMap();
		{
			this.stateNumbers.put(OmniFAT.POSITIVE_STATE, new Integer(1));
			this.stateNumbers.put(OmniFAT.LIKELY_STATE, new Integer(2));
			this.stateNumbers.put(OmniFAT.AMBIGUOUS_STATE, new Integer(3));
			this.stateNumbers.put(OmniFAT.ABBREVIATED_STATE, new Integer(3)); // treat abbreviations as ambiguous in this early stage
			this.stateNumbers.put(OmniFAT.UNCERTAIN_STATE, new Integer(4));
			this.stateNumbers.put(OmniFAT.NEGATIVE_STATE, new Integer(5));
		}
		public int compare(Object o1, Object o2) {
			int stateNumber1 = ((Integer) this.stateNumbers.get(o1)).intValue();
			int stateNumber2 = ((Integer) this.stateNumbers.get(o2)).intValue();
			return (stateNumber1 - stateNumber2);
		}
	};
	private static Comparator baseEpithetStateAnnotationOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			return baseEpithetStateOrder.compare(((Annotation) o1).getAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.NEGATIVE_STATE), ((Annotation) o2).getAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.NEGATIVE_STATE));
		}
	};
	
	public static void tagAuthorNames(MutableAnnotation data, OmniFAT omniFat) {
		tagAuthorNames(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void tagAuthorNames(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	prepare filters
		Set lexiconMatchKeys = new HashSet();
		
		//	annotate author names from lexicons
		OmniFatDictionary[] authorNameDictionaries = docData.omniFat.getAuthorNameDictionaries();
		for (int d = 0; d < authorNameDictionaries.length; d++) {
			
			Annotation[] authorNames = Gamta.extractAllContained(data, authorNameDictionaries[d], 1, false);
			for (int a = 0; a < authorNames.length; a++) {
				String authorNameKey = getKey(authorNames[a]);
				
				//	remember lexicon match
				lexiconMatchKeys.add(authorNameKey);
				
				//	ignore single letters
				if (authorNames[a].length() < 2)
					continue;
				
				//	get string value
				String authorNameString = authorNames[a].getValue();
				
				//	ignore case insensitive matches that are not all upper case
				if (!authorNameDictionaries[d].lookup(authorNameString, true) && !Gamta.isUpperCaseWord(authorNameString))
					continue;
				
				Annotation authorName = data.addAnnotation(OmniFAT.AUTHOR_NAME_TYPE, authorNames[a].getStartIndex(), authorNames[a].size());
				authorName.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.LEXICON_EVIDENCE);
				authorName.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, authorNameDictionaries[d].name);
				authorName.setAttribute(OmniFAT.STRING_ATTRIBUTE, authorNameString);
				authorName.setAttribute(OmniFAT.VALUE_ATTRIBUTE, authorNameString);
				
				authorName.setAttribute(OmniFAT.STATE_ATTRIBUTE, (authorNameDictionaries[d].trusted ? OmniFAT.POSITIVE_STATE : OmniFAT.LIKELY_STATE));
			}
		}
		
		//	annotate author names from patterns
		OmniFatPattern[] authorNamePatterns = docData.omniFat.getAuthorNamePatterns();
		
		//	avoid duplicates
		Set matchKeys = new HashSet();
		
		//	apply patterns
		for (int p = 0; p < authorNamePatterns.length; p++) {
			Annotation[] authorNames = Gamta.extractAllMatches(data, authorNamePatterns[p].pattern, 0, false, false, true);
			for (int a = 0; a < authorNames.length; a++) {
				String authorNameKey = getKey(authorNames[a]);
				
				//	we've had this one already
				if (!matchKeys.add(authorNameKey))
					continue;
				
				//	do not duplicate lexicon matches (their rank group is clear)
				if (lexiconMatchKeys.contains(authorNameKey))
					continue;
				
				//	ignore single letters
				if (authorNames[a].length() < 2)
					continue;
				
				Annotation authorName = data.addAnnotation(OmniFAT.AUTHOR_NAME_TYPE, authorNames[a].getStartIndex(), authorNames[a].size());
				authorName.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.PATTERN_EVIDENCE);
				authorName.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, authorNamePatterns[p].name);
				String authorNameValue = authorNames[a].getValue();
				for (int v = 0; v < authorNames[a].size(); v++) {
					String value = authorNames[a].valueAt(v);
					if (Gamta.isWord(value))
						authorNameValue = value;
				}
				authorName.setAttribute(OmniFAT.STRING_ATTRIBUTE, authorNameValue);
				authorName.setAttribute(OmniFAT.VALUE_ATTRIBUTE, authorNameValue);
				
				authorName.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.LIKELY_STATE);
			}
		}
		
		
		//	annotate author name stop words from lexicon
		Annotation[] authorNameStopWords = Gamta.extractAllContained(data, docData.omniFat.getAuthorNameStopWords(), 1, true);
		for (int a = 0; a < authorNameStopWords.length; a++)
			data.addAnnotation(OmniFAT.AUTHOR_NAME_STOP_WORD_TYPE, authorNameStopWords[a].getStartIndex(), authorNameStopWords[a].size());
		
		//	join adjacent stop words
		Annotation[] jointAuthorNameStopWords;
		do {
			jointAuthorNameStopWords = joinAdjacent(data, OmniFAT.AUTHOR_NAME_STOP_WORD_TYPE, OmniFAT.AUTHOR_NAME_STOP_WORD_TYPE, null);
			for (int a = 0; a < jointAuthorNameStopWords.length; a++)
				data.addAnnotation(OmniFAT.AUTHOR_NAME_STOP_WORD_TYPE, jointAuthorNameStopWords[a].getStartIndex(), jointAuthorNameStopWords[a].size());
		} while (jointAuthorNameStopWords.length != 0);
		
		
		//	join author names with stop words
		Annotation[] jointAuthorNames;
		do {
			jointAuthorNames = joinAdjacent(data, OmniFAT.AUTHOR_NAME_STOP_WORD_TYPE, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_NAME_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					joint.copyAttributes(rightSource);
					if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
						joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
					return joint;
				}
			});
			for (int a = 0; a < jointAuthorNames.length; a++)
				data.addAnnotation(OmniFAT.AUTHOR_NAME_TYPE, jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
		} while (jointAuthorNames.length != 0);
		
		//	clean up
		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHOR_NAME_STOP_WORD_TYPE);
		
		
		//	annotate author initials from patterns
		OmniFatPattern[] authorInitialPatterns = docData.omniFat.getAuthorInitialPatterns();
		
		//	apply patterns for initials
		for (int p = 0; p < authorInitialPatterns.length; p++) {
			Annotation[] authorInitials = Gamta.extractAllMatches(data, authorInitialPatterns[p].pattern, 2, false, false, true);
			for (int a = 0; a < authorInitials.length; a++) {
				String authorInitialKey = getKey(authorInitials[a]);
				
				//	we've had this one already
				if (!matchKeys.add(authorInitialKey))
					continue;
				
				//	do not duplicate lexicon matches (their rank group is clear)
				if (lexiconMatchKeys.contains(authorInitialKey))
					continue;
				
				Annotation authorInitial = data.addAnnotation(OmniFAT.AUTHOR_INITIAL_TYPE, authorInitials[a].getStartIndex(), authorInitials[a].size());
				authorInitial.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.PATTERN_EVIDENCE);
				authorInitial.setAttribute(OmniFAT.VALUE_ATTRIBUTE, authorInitials[a].getValue());
			}
		}
		
		
		//	join author names with initials
		do {
			jointAuthorNames = joinAdjacent(data, OmniFAT.AUTHOR_INITIAL_TYPE, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_NAME_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					joint.copyAttributes(rightSource);
					if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
						joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.INITIALS_EVIDENCE);
					return joint;
				}
			});
			for (int a = 0; a < jointAuthorNames.length; a++)
				data.addAnnotation(OmniFAT.AUTHOR_NAME_TYPE, jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
		} while (jointAuthorNames.length != 0);
		
		//	clean up
		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHOR_INITIAL_TYPE);
		
		
		//	join author names with author names
		do {
			jointAuthorNames = joinAdjacent(data, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_NAME_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					joint.copyAttributes(rightSource);
					if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
						joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
					
					String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (lsid == null)
						lsid = leftSource.getAnnotationID();
					String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (rsid == null)
						rsid = rightSource.getAnnotationID();
					String source = (lsid + "," + rsid);
					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
					
					return joint;
				}
			});
			for (int a = 0; a < jointAuthorNames.length; a++)
				data.addAnnotation(OmniFAT.AUTHOR_NAME_TYPE, jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
		} while (jointAuthorNames.length != 0);
		
		
		//	collect sure positive base epithets
		Set surePositiveBaseEpithets = new HashSet();
		Annotation[] baseEpithets = data.getAnnotations(OmniFAT.BASE_EPITHET_TYPE);
		for (int e = 0; e < baseEpithets.length; e++) {
			if (OmniFAT.POSITIVE_STATE.equals(baseEpithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE)))
				surePositiveBaseEpithets.add(baseEpithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
		}
		
		//	remove author names whose whole value is also a sure positive epithet 
		Annotation[] authorNames = data.getAnnotations(OmniFAT.AUTHOR_NAME_TYPE);
		for (int a = 0; a < authorNames.length; a++) {
			if (surePositiveBaseEpithets.contains(authorNames[a].getValue()) && !OmniFAT.POSITIVE_STATE.equals(authorNames[a].getAttribute(OmniFAT.STATE_ATTRIBUTE)))
				data.removeAnnotation(authorNames[a]);
		}
		
		
		//	annotate tokens that can separate author names in an enumeration
		Annotation[] authorListSeparators = Gamta.extractAllContained(data, docData.omniFat.getAuthorListSeparators());
		for (int s = 0; s < authorListSeparators.length; s++)
			data.addAnnotation(OmniFAT.AUTHOR_LIST_SEPARATOR_TYPE, authorListSeparators[s].getStartIndex(), authorListSeparators[s].size());
		Annotation[] authorListEndSeparators = Gamta.extractAllContained(data, docData.omniFat.getAuthorListEndSeparators());
		for (int s = 0; s < authorListEndSeparators.length; s++)
			data.addAnnotation(OmniFAT.AUTHOR_LIST_END_SEPARATOR_TYPE, authorListEndSeparators[s].getStartIndex(), authorListEndSeparators[s].size());
		
		
		//	annotate possible ends of author name enumerations
		Annotation[] authorListEnds = joinAdjacent(data, OmniFAT.AUTHOR_LIST_END_SEPARATOR_TYPE, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_LIST_END_TYPE, new JoinTool() {
			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
				joint.copyAttributes(rightSource);
				if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
					joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
				
				String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
				if (rsid == null)
					rsid = rightSource.getAnnotationID();
				joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, rsid);
				
				return joint;
			}
		});
		for (int s = 0; s < authorListEnds.length; s++)
			data.addAnnotation(OmniFAT.AUTHOR_LIST_END_TYPE, authorListEnds[s].getStartIndex(), authorListEnds[s].size()).copyAttributes(authorListEnds[s]);
		
		//	annotate author name enumerations
		Annotation[] authorLists = joinAdjacent(data, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_LIST_END_TYPE, OmniFAT.AUTHOR_LIST_TYPE, new JoinTool() {
			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
				joint.copyAttributes(rightSource);
				if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
					joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
				
				String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
				if (lsid == null)
					lsid = leftSource.getAnnotationID();
				String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
				if (rsid == null)
					rsid = rightSource.getAnnotationID();
				String source = (lsid + "," + rsid);
				joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
				
				return joint;
			}
		});
		for (int s = 0; s < authorLists.length; s++)
			data.addAnnotation(OmniFAT.AUTHOR_LIST_TYPE, authorLists[s].getStartIndex(), authorLists[s].size()).copyAttributes(authorLists[s]);
		
		
		//	annotate author name enumeration parts
		Annotation[] authorListParts;
		authorListParts = joinAdjacent(data, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_LIST_SEPARATOR_TYPE, OmniFAT.AUTHOR_LIST_PART_TYPE, new JoinTool() {
			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
				joint.copyAttributes(rightSource);
				if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
					joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
				
				String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
				if (lsid == null)
					lsid = leftSource.getAnnotationID();
				joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, lsid);
				
				return joint;
			}
		});
		for (int s = 0; s < authorListParts.length; s++)
			data.addAnnotation(OmniFAT.AUTHOR_LIST_PART_TYPE, authorListParts[s].getStartIndex(), authorListParts[s].size()).copyAttributes(authorListParts[s]);
		
		authorListParts = joinAdjacent(data, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHOR_LIST_END_SEPARATOR_TYPE, OmniFAT.AUTHOR_LIST_PART_TYPE, new JoinTool() {
			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
				joint.copyAttributes(rightSource);
				if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
					joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
				
				String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
				if (lsid == null)
					lsid = leftSource.getAnnotationID();
				joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, lsid);
				
				return joint;
			}
		});
		for (int s = 0; s < authorListParts.length; s++)
			data.addAnnotation(OmniFAT.AUTHOR_LIST_PART_TYPE, authorListParts[s].getStartIndex(), authorListParts[s].size()).copyAttributes(authorListParts[s]);
		
		
		//	join author name enumerations
		do {
			authorLists = joinAdjacent(data, OmniFAT.AUTHOR_LIST_PART_TYPE, OmniFAT.AUTHOR_LIST_TYPE, OmniFAT.AUTHOR_LIST_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					joint.copyAttributes(rightSource);
					if (OmniFAT.PATTERN_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
						joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
					
					String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (lsid == null)
						lsid = leftSource.getAnnotationID();
					String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (rsid == null)
						rsid = rightSource.getAnnotationID();
					String source = (lsid + "," + rsid);
					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
					
					return joint;
				}
			});
			for (int s = 0; s < authorLists.length; s++)
				data.addAnnotation(OmniFAT.AUTHOR_LIST_TYPE, authorLists[s].getStartIndex(), authorLists[s].size()).copyAttributes(authorLists[s]);
		} while (authorLists.length != 0);
		
		
		//	clean up
		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHOR_LIST_SEPARATOR_TYPE);
		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHOR_LIST_END_SEPARATOR_TYPE);
		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHOR_LIST_PART_TYPE);
		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHOR_LIST_END_TYPE);
		
		//	treat author name lists as single authors
		AnnotationFilter.renameAnnotations(data, OmniFAT.AUTHOR_LIST_TYPE, OmniFAT.AUTHOR_NAME_TYPE);
//		
//		
//		//	annotate years following authors and author lists
//		Annotation[] years = Gamta.extractAllMatches(data, "(\\,\\s)?[1-2][0-9]{3}", 2);
//		for (int y = 0; y < years.length; y++)
//			data.addAnnotation(OmniFAT.AUTHORSHIP_YEAR_TYPE, years[y].getStartIndex(), years[y].size());
//		
//		
//		//	join author names with following years
//		do {
//			jointAuthorNames = joinAdjacent(data, OmniFAT.AUTHOR_NAME_TYPE, OmniFAT.AUTHORSHIP_YEAR_TYPE, OmniFAT.AUTHOR_NAME_TYPE, new JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					joint.copyAttributes(leftSource);
//					
//					String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
//					if (lsid == null)
//						lsid = leftSource.getAnnotationID();
//					String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					String source = (lsid + "," + rsid);
//					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
//					
//					return joint;
//				}
//			});
//			for (int a = 0; a < jointAuthorNames.length; a++)
//				data.addAnnotation(OmniFAT.AUTHOR_NAME_TYPE, jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
//		} while (jointAuthorNames.length != 0);
//		
//		
//		//	clean up
//		AnnotationFilter.removeAnnotations(data, OmniFAT.AUTHORSHIP_YEAR_TYPE);
	}
	
	public static void tagLabels(MutableAnnotation data, OmniFAT omniFat) {
		tagLabels(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void tagLabels(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	get ranks
		OmniFAT.Rank[] ranks = docData.omniFat.getRanks();
		
		//	annotate epithet labels for each rank
		for (int r = 0; r < ranks.length; r++) {
			
			//	annotate epithet labels
			Annotation[] epithetLabels = Gamta.extractAllContained(data, ranks[r].getLabels(), true);
			for (int e = 0; e < epithetLabels.length; e++) {
				
				//	ignore main ranks (only appear in new epithet labels)
				if (ranks[r].isEpithetsUnlabeled())
					continue;
				
				Annotation epithetLabel = data.addAnnotation(OmniFAT.EPITHET_LABEL_TYPE, epithetLabels[e].getStartIndex(), epithetLabels[e].size());
				epithetLabel.setAttribute(OmniFAT.RANK_ATTRIBUTE, ranks[r].getName());
				epithetLabel.setAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + ranks[r].getOrderNumber()));
				epithetLabel.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, ranks[r].getRankGroup().getName());
				epithetLabel.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + ranks[r].getRankGroup().getOrderNumber()));
			}
			
			//	annotate new epithet labels
			Annotation[] newEpithetLabels = Gamta.extractAllContained(data, ranks[r].getNewEpithetLables(), true);
			for (int e = 0; e < newEpithetLabels.length; e++) {
				Annotation epithetLabel = data.addAnnotation(OmniFAT.NEW_EPITHET_LABEL_TYPE, newEpithetLabels[e].getStartIndex(), newEpithetLabels[e].size());
				epithetLabel.setAttribute(OmniFAT.RANK_ATTRIBUTE, ranks[r].getName());
				epithetLabel.setAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + ranks[r].getOrderNumber()));
				epithetLabel.setAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, ranks[r].getRankGroup().getName());
				epithetLabel.setAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ("" + ranks[r].getRankGroup().getOrderNumber()));
			}
		}
		
		//	filter out epithet labels nested in new epithet labels
		AnnotationFilter.removeContained(data, OmniFAT.NEW_EPITHET_LABEL_TYPE, OmniFAT.EPITHET_LABEL_TYPE);
	}
	
	public static void tagEpithets(MutableAnnotation data, OmniFAT omniFat) {
		tagEpithets(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void tagEpithets(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		Annotation[] epithets;
		
		//	join labels and base epithets
		do {
			epithets = joinAdjacent(data, OmniFAT.EPITHET_LABEL_TYPE, OmniFAT.BASE_EPITHET_TYPE, OmniFAT.EPITHET_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					String rightRank = ((String) rightSource.getAttribute(OmniFAT.RANK_ATTRIBUTE));
					if ((rightRank != null) && !rightRank.equals(leftSource.getAttribute(OmniFAT.RANK_ATTRIBUTE)))
						return null;
					if (!rightSource.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE).equals(leftSource.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE)))
						return null;
					
					joint.copyAttributes(rightSource);
					joint.copyAttributes(leftSource); // rank of label overrules rank of base epithet
					joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.LABEL_EVIDENCE);
					joint.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
					
					String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (rsid == null)
						rsid = rightSource.getAnnotationID();
					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, rsid);
					
					return joint;
				}
			});
			for (int e = 0; e < epithets.length; e++)
				data.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size()).copyAttributes(epithets[e]);
		} while (epithets.length != 0);
		
		
//		DO NOT LET ABBREVIATED EPITHETS HAVE LABELS
//		
//		//	join labels and abbreviated epithets
//		do {
//			epithets = joinAdjacent(data, OmniFAT.EPITHET_LABEL_TYPE, OmniFAT.ABBREVIATED_EPITHET_TYPE, OmniFAT.EPITHET_TYPE, new JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					String rightRank = ((String) rightSource.getAttribute(OmniFAT.RANK_ATTRIBUTE));
//					if ((rightRank != null) && !rightRank.equals(leftSource.getAttribute(OmniFAT.RANK_ATTRIBUTE)))
//						return null;
//					if (!rightSource.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE).equals(leftSource.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE)))
//						return null;
//					
//					joint.copyAttributes(rightSource);
//					joint.copyAttributes(leftSource); // rank of label overrules rank of base epithet
//					joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.LABEL_EVIDENCE);
//					joint.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
//					
//					String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, rsid);
//					
//					return joint;
//				}
//			});
//			for (int e = 0; e < epithets.length; e++)
//				data.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size()).copyAttributes(epithets[e]);
//		} while (epithets.length != 0);
		
		
		//	mark base epithets as epithets
		epithets = data.getAnnotations(OmniFAT.BASE_EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			Annotation epithet = data.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
			epithet.copyAttributes(epithets[e]);
		}
		
		
//		TOO EARLY, DO AUTHORS FIRST ==> DO NOT LET ABBREVIATED EPITHETS HAVE AN AUTHOR
//		
//		//	mark abbreviated epithets as epithets
//		epithets = data.getAnnotations(OmniFAT.ABBREVIATED_EPITHET_TYPE);
//		for (int e = 0; e < epithets.length; e++) {
//			Annotation epithet = data.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
//			epithet.copyAttributes(epithets[e]);
//		}
		
		
		//	join epithets with subesquent authors
		do {
			epithets = joinAdjacent(data, OmniFAT.EPITHET_TYPE, OmniFAT.AUTHOR_NAME_TYPE, docData.omniFat.getIntraEpithetPunctuationMarks(), OmniFAT.EPITHET_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					joint.copyAttributes(leftSource);
					if (OmniFAT.RECALL_EVIDENCE.equals(joint.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
						joint.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.JOINT_EVIDENCE);
					joint.setAttribute(OmniFAT.AUTHOR_ATTRIBUTE, normalizeAuthorName(rightSource));
					
					String lsid = ((String) leftSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (lsid == null)
						lsid = leftSource.getAnnotationID();
					String rsid = rightSource.getAnnotationID();
					String source = (lsid + "," + rsid);
					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
					
					return joint;
				}
			});
			for (int e = 0; e < epithets.length; e++)
				data.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size()).copyAttributes(epithets[e]);
		} while (epithets.length != 0);
		
		
		//	remove epithets with nested line breaks
		epithets = data.getAnnotations(OmniFAT.EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			for (int t = 0; t < (epithets[e].size()-1); t++)
				if (epithets[e].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
					t = epithets[e].size();
					data.removeAnnotation(epithets[e]);
				}
		}
		
		
		//	mark abbreviated epithets as epithets
		epithets = data.getAnnotations(OmniFAT.ABBREVIATED_EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			Annotation epithet = data.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
			epithet.copyAttributes(epithets[e]);
		}
	}
	
	private static final String normalizeAuthorName(Annotation authorName) {
		if (authorName == null) return "";
		
		String rawAuthorName;
		if (authorName.size() == 1)
			rawAuthorName = authorName.getValue();
		else {
			int end = authorName.size();
			while ((end > 0) && !Gamta.isWord(authorName.valueAt(end-1)))
				end--;
			rawAuthorName = TokenSequenceUtils.concatTokens(authorName, 0, end, true, true);
		}
		
		StringBuffer normalizedAuthorName = new StringBuffer(authorName.length());
		boolean lastWasWhitespace = true;
		for (int c = 0; c < rawAuthorName.length(); c++) {
			if (rawAuthorName.charAt(c) < 33) {
				lastWasWhitespace = true;
				normalizedAuthorName.append(" ");
			}
			else {
				if (lastWasWhitespace) normalizedAuthorName.append(rawAuthorName.substring(c, (c+1)));
				else normalizedAuthorName.append(rawAuthorName.substring(c, (c+1)).toLowerCase());
				
				lastWasWhitespace = false;
			}
		}
		return normalizedAuthorName.toString();
	}
	
	public static void tagCandidates(MutableAnnotation data, OmniFAT omniFat) {
		tagCandidates(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void tagCandidates(MutableAnnotation data, final OmniFAT.DocumentDataSet docData) {
		
		//	gather rank statistics
		OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
		String[] rankGroupNames = new String[rankGroups.length];
		int[] rankGroupSizes = new int[rankGroups.length];
		Set requiredRankGroupNumbers = new TreeSet();
		Set requiredRankNumbers = new TreeSet();
		for (int rg = 0; rg < rankGroups.length; rg++) {
			rankGroupNames[rg] = rankGroups[rg].getName();
			OmniFAT.Rank[] ranks = rankGroups[rg].getRanks();
			rankGroupSizes[rg] = ranks.length;
			for (int r = 0; r < ranks.length; r++)
				if (ranks[r].isEpithetRequired()) {
					requiredRankGroupNumbers.add(new Integer(rankGroups[rg].getOrderNumber()));
					requiredRankNumbers.add(new Integer(ranks[r].getOrderNumber()));
				}
		}
		
		
		//	join epithets to candidates
		Annotation[] taxonNameCandidates;
		HashSet taxonNameCandidateKeys = new HashSet();
		int added;
		int maxEpithets = docData.omniFat.getRanks().length;
		
		
		//	join adjacent epithets to taxon names
		do {
			added = 0;
			taxonNameCandidates = joinAdjacent(data, OmniFAT.EPITHET_TYPE, docData.omniFat.getInterEpithetPunctuationMarks(), OmniFAT.TAXON_NAME_CANDIDATE_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					if (!docData.omniFat.acceptEmbeddedAuthorNames()) {
						if (leftSource.hasAttribute(OmniFAT.AUTHOR_ATTRIBUTE))
							return null;
					}
					else if (!this.checkCombinable(leftSource))
						return null;
					else if (!this.checkCombinable(rightSource))
						return null;
					
					String source = (leftSource.getAnnotationID() + "," + rightSource.getAnnotationID());
					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
					
					return joint;
				}
				public String getDeDuplicationKey(Annotation annotation) {
					return (annotation.hasAttribute(OmniFAT.SOURCE_ATTRIBUTE) ? ((String) annotation.getAttribute(OmniFAT.SOURCE_ATTRIBUTE)) : super.getDeDuplicationKey(annotation));
				}
				private boolean checkCombinable(Annotation source) {
					String rgonString = ((String) source.getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE));
					if (rgonString == null)
						return true;
					int rgon;
					try {
						rgon = Integer.parseInt(rgonString);
					} catch (NumberFormatException nfe) {
						return true;
					}
					RankGroup rg = docData.omniFat.getRankGroup(rgon);
					return ((rg == null) ? true : rg.isCombinable());
				}
			});
			for (int c = 0; c < taxonNameCandidates.length; c++) {
				if (isLineBroken(taxonNameCandidates[c]))
					continue;
				
				if (!taxonNameCandidateKeys.add(getCandidateKey(taxonNameCandidates[c])))
					continue;
				
				if (countEpithetsBySource(taxonNameCandidates[c]) > maxEpithets)
					continue;
				
				QueriableAnnotation taxonNameCandidate = data.addAnnotation(OmniFAT.TAXON_NAME_CANDIDATE_TYPE, taxonNameCandidates[c].getStartIndex(), taxonNameCandidates[c].size());
				taxonNameCandidate.copyAttributes(taxonNameCandidates[c]);
				
				if (isCandidateBroken(taxonNameCandidate, rankGroupNames, rankGroupSizes, requiredRankGroupNumbers, requiredRankNumbers, docData)) {
					docData.log(("Removing broken candidate: " + taxonNameCandidate.toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					data.removeAnnotation(taxonNameCandidate);
				}
				
				else added++;
			}
			docData.log((added + " new base candidates added"), OmniFAT.DEBUG_LEVEL_IMPORTANT);
		} while (added != 0);
		
		
		//	join epithets to adjacent taxon names
		do {
			added = 0;
			taxonNameCandidates = joinAdjacent(data, OmniFAT.EPITHET_TYPE, OmniFAT.TAXON_NAME_CANDIDATE_TYPE, docData.omniFat.getInterEpithetPunctuationMarks(), OmniFAT.TAXON_NAME_CANDIDATE_TYPE, new JoinTool() {
				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
					String rsid = ((String) rightSource.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
					if (rsid == null)
						rsid = rightSource.getAnnotationID();
					String source = (leftSource.getAnnotationID() + "," + rsid);
					joint.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source);
					
					return joint;
				}
				public String getDeDuplicationKey(Annotation annotation) {
					return (annotation.hasAttribute(OmniFAT.SOURCE_ATTRIBUTE) ? ((String) annotation.getAttribute(OmniFAT.SOURCE_ATTRIBUTE)) : super.getDeDuplicationKey(annotation));
				}
			});
			for (int c = 0; c < taxonNameCandidates.length; c++) {
				if (isLineBroken(taxonNameCandidates[c]))
					continue;
				
				if (!taxonNameCandidateKeys.add(getCandidateKey(taxonNameCandidates[c])))
					continue;
				
				if (countEpithetsBySource(taxonNameCandidates[c]) > maxEpithets)
					continue;
				
				QueriableAnnotation taxonNameCandidate = data.addAnnotation(OmniFAT.TAXON_NAME_CANDIDATE_TYPE, taxonNameCandidates[c].getStartIndex(), taxonNameCandidates[c].size());
				taxonNameCandidate.copyAttributes(taxonNameCandidates[c]);
				
				if (isCandidateBroken(taxonNameCandidate, rankGroupNames, rankGroupSizes, requiredRankGroupNumbers, requiredRankNumbers, docData)) {
					docData.log(("Removing broken candidate: " + taxonNameCandidate.toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					data.removeAnnotation(taxonNameCandidate);
				}
				
				else added++;
			}
			docData.log((added + " new extended candidates added"), OmniFAT.DEBUG_LEVEL_IMPORTANT);
		} while (added != 0);
		
		
		//	annotate single epithets as taxon names
		Annotation[] epithets = data.getAnnotations(OmniFAT.EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			if (isEpithetAbbreviated(epithets[e]))
				continue;
			
			Annotation taxonNameCandidate = data.addAnnotation(OmniFAT.TAXON_NAME_CANDIDATE_TYPE, epithets[e].getStartIndex(), epithets[e].size());
			taxonNameCandidate.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, epithets[e].getAnnotationID());
		}
		docData.log((epithets.length + " new one-epithet candidates added"), OmniFAT.DEBUG_LEVEL_IMPORTANT);
		
		
		//	filter candidates
		QueriableAnnotation[] tnCandidates;
		
		//	filter out candidates with abbreviated last epithet
		tnCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < tnCandidates.length; c++)
			if (isLastEpithetAbbreviated((QueriableAnnotation) tnCandidates[c], docData))
				data.removeAnnotation(tnCandidates[c]);
		
		//	disambiguate candidates whose epithets mark the same tokens, but with different ranks
		tnCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		HashMap tncBuckets = new HashMap();
		for (int c = 0; c < tnCandidates.length; c++) {
			String tncKey = getCandidateKey(tnCandidates[c], docData);
			ArrayList tncBucket = ((ArrayList) tncBuckets.get(tncKey));
			if (tncBucket == null) {
				tncBucket = new ArrayList();
				tncBuckets.put(tncKey, tncBucket);
			}
			tncBucket.add(tnCandidates[c]);
		}
		
		for (Iterator bit = tncBuckets.values().iterator(); bit.hasNext();) {
			ArrayList tncBucket = ((ArrayList) bit.next());
			if (tncBucket.size() > 1) {
//				System.out.println("Disambiguating rank combination:");
				int maxScore = -1;
				QueriableAnnotation maxScoreCandidate = null;
				for (Iterator cit = tncBucket.iterator(); cit.hasNext();) {
					QueriableAnnotation tnCandidate = ((QueriableAnnotation) cit.next());
					int score = getEpithetRankCombinationScore(tnCandidate, docData);
//					System.out.println("  Score " + score + " for " + tnCandidate.toXML());
					if (score > maxScore) {
						if (maxScoreCandidate != null)
							data.removeAnnotation(maxScoreCandidate);
						maxScore = score;
						maxScoreCandidate = tnCandidate;
					}
					else data.removeAnnotation(tnCandidate);
				}
//				System.out.println(" Keeping for score " + maxScore + ": " + maxScoreCandidate.toXML());
//				Annotation[] maxScoreEpithets = docData.getEpithets(maxScoreCandidate);
//				for (int e = 0; e < maxScoreEpithets.length; e++)
//					System.out.println("   " + maxScoreEpithets[e].toXML());
			}
		}
		
		
		/*
!!! epithet rank assignment is completely secondary for name recognition !!!
==> must not inhibit promotion of a candidate

deduplicate based on annotation key + concatenation of epithet base indices for any duplicates:
- infer ranks
  - get array of Integers to represent combination
  - do NOT set attributes that early
- score rank combinations
  - most required ranks covered
  - highest sum of ranks / rank groups (invalid combinations are excluded before)
  - finally, simply use first combination

!!! even choice of epithet combinations must not inhibit name recognition !!!
==> in fully automated scenario (Servlet !!), report feedback candidates with state 'likely' as names
		 */
	}
	
	private static int getEpithetRankCombinationScore(QueriableAnnotation taxonNameCandidate, OmniFAT.DocumentDataSet docData) {
		
		//	extract epithets and group them in rank groups
		Annotation[] epithets = docData.getEpithets(taxonNameCandidate);
		OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
		Annotation[][] rankGroupEpithets = new Annotation[rankGroups.length][];
		for (int rg = 0; rg < rankGroups.length; rg++) {
			ArrayList rankGroupEpithetList = new ArrayList();
			for (int e = 0; e < epithets.length; e++) {
				if (rankGroups[rg].getOrderNumber() == Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE)))
					rankGroupEpithetList.add(epithets[e]);
			}
			rankGroupEpithets[rg] = ((Annotation[]) rankGroupEpithetList.toArray(new Annotation[rankGroupEpithetList.size()]));
		}
		
		//	gather additional data
		boolean[] isHighGroup = new boolean[rankGroups.length];
		boolean highGroup = true;
		boolean[] isLowGroup = new boolean[rankGroups.length];
		boolean lowGroup = true;
		for (int rg = 0; rg < rankGroups.length; rg++) {
			isHighGroup[rg] = highGroup;
			highGroup = (highGroup && (rankGroupEpithets[rg].length == 0));
			isLowGroup[rankGroups.length - 1 - rg] = lowGroup;
			lowGroup = (lowGroup && (rankGroupEpithets[rankGroups.length - 1 - rg].length == 0));
		}
		
		//	sum up scores per rank group
		int score = 0;
		for (int rg = 0; rg < rankGroups.length; rg++)
			if (rankGroupEpithets[rg].length != 0) {
				Integer[] groupEpithetRanks = getGroupEpithetRanks(rankGroupEpithets[rg], rankGroups[rg], isHighGroup[rg], isLowGroup[rg], docData, false);
				if (groupEpithetRanks != null) {
					for (int e = 0; e < rankGroupEpithets[rg].length; e++)
						score += docData.omniFat.getRank(groupEpithetRanks[e].intValue()).getProbability();
				}
			}
		return score;
	}
	
	private static final String getCandidateKey(Annotation taxonNameCandidate) {
		return (getKey(taxonNameCandidate) + "$" + taxonNameCandidate.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
	}
	
	private static final String getCandidateKey(QueriableAnnotation taxonNameCandidate, OmniFAT.DocumentDataSet docData) {
		String baseKey = getKey(taxonNameCandidate);
		Annotation[] epithets = docData.getEpithets(taxonNameCandidate);
		for (int e = 0; e < epithets.length; e++)
			baseKey += ((String) epithets[e].getAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE));
		return baseKey;
	}
	
	private static final boolean isLineBroken(Annotation taxonNameCandidate) {
		for (int t = 0; t < (taxonNameCandidate.size()-1); t++) {
			if (taxonNameCandidate.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
				return true;
		}
		return false;
	}
	
	private static final int countEpithetsBySource(Annotation taxonNameCandidate) {
		String source = ((String) taxonNameCandidate.getAttribute(OmniFAT.SOURCE_ATTRIBUTE));
		if ((source == null) || (source.length() == 0))
			return 0;
		
		int epithets = 1;
		for (int c = 0; c < source.length(); c++)
			if (source.charAt(c) == ',')
				epithets++;
		
		return epithets;
	}
	
	private static final boolean isCandidateBroken(QueriableAnnotation taxonNameCandidate, String[] rankGroupNames, int[] rankGroupSizes, Set requiredRankGroupNumbers, Set requiredRankNumbers, OmniFAT.DocumentDataSet docData) {
		Annotation[] epithets = docData.getEpithets(taxonNameCandidate);
		
		if (isRankGroupOrderBroken(epithets)) {
			StringBuffer rankGroupOrder = new StringBuffer();
			for (int e = 0; e < epithets.length; e++)
				rankGroupOrder.append("-" + epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, "unknown"));
			taxonNameCandidate.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, ("rankGroupOrder(" + rankGroupOrder.toString().substring(1) + ")"));
			return true;
		}
		
		if (isRankOrderBroken(epithets)) {
			StringBuffer rankOrder = new StringBuffer();
			for (int e = 0; e < epithets.length; e++)
				rankOrder.append("-" + epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE, "unknown"));
			taxonNameCandidate.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, ("rankOrder(" + rankOrder.toString().substring(1) + ")"));
			return true;
		}
		
		if (isRankGroupSizeBroken(epithets, rankGroupNames, rankGroupSizes)) {
			StringBuffer rankGroupOrder = new StringBuffer();
			for (int e = 0; e < epithets.length; e++)
				rankGroupOrder.append("-" + epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE, "unknown"));
			taxonNameCandidate.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, ("rankGroupSize(" + rankGroupOrder.toString().substring(1) + ")"));
			return true;
		}
		
		if (isEpithetMissing(epithets, requiredRankGroupNumbers, requiredRankNumbers)) {
			StringBuffer rankOrder = new StringBuffer();
			for (int e = 0; e < epithets.length; e++)
				rankOrder.append("-" + epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE, "unknown"));
			taxonNameCandidate.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, ("epithetMissing(" + rankOrder.toString().substring(1) + ")"));
			return true;
		}
		
		if (isRankOrderBroken(epithets, docData)) {
			StringBuffer rankOrder = new StringBuffer();
			for (int e = 0; e < epithets.length; e++)
				rankOrder.append("-" + epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE, "unknown"));
			taxonNameCandidate.setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, ("rankOrder(" + rankOrder.toString().substring(1) + ")"));
			return true;
		}
		
		return false;
	}
	
	private static final boolean isLastEpithetAbbreviated(QueriableAnnotation taxonNameCandidate, OmniFAT.DocumentDataSet docData) {
		Annotation[] epithets = docData.getEpithets(taxonNameCandidate);
		boolean lastEpithetAbbreviated = false;
		for (int e = 0; e < epithets.length; e++)
			lastEpithetAbbreviated = isEpithetAbbreviated(epithets[e]);
		return lastEpithetAbbreviated;
	}
	
	private static final boolean isEpithetAbbreviated(Annotation epithet) {
		String epithetString = ((String) epithet.getAttribute(OmniFAT.STRING_ATTRIBUTE));
		return ((epithetString.length() < 4) && ((epithetString.length() < 3) || epithetString.endsWith(".")));
	}
	
	private static final boolean isRankGroupOrderBroken(Annotation[] epithets) {
		int lastRankGroupNumber = Integer.MAX_VALUE;
		int rankGroupNumber;
		for (int e = 0; e < epithets.length; e++) {
			rankGroupNumber = lastRankGroupNumber + 1;
			try {
				rankGroupNumber = Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE));
			} catch (NumberFormatException nfe) {}
			
			if (rankGroupNumber <= lastRankGroupNumber)
				lastRankGroupNumber = rankGroupNumber;
			else return true;
		}
		return false;
	}
	
	private static final boolean isRankOrderBroken(Annotation[] epithets) {
		int lastRankGroupNumber = Integer.MAX_VALUE;
		int rankGroupNumber;
		int maxRankNumber = Integer.MAX_VALUE;
		int lastRankNumber = Integer.MAX_VALUE;
		int rankNumber;
		
		for (int e = 0; e < epithets.length; e++) {
			
			rankGroupNumber = Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE, ""));
			try {
				rankNumber = Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ""));
			}
			catch (NumberFormatException nfe) {
				rankNumber = lastRankNumber-1;
			}
			
			//	compare rank numbers of neighboring epithets (be graceful, though, since not all epithets have a rank so far)
			if (rankNumber < lastRankNumber)
				lastRankNumber = rankNumber;
			else return true;
			
			//	compare rank number to maximum possible rank number
			if (rankGroupNumber == lastRankGroupNumber) {
				if (rankNumber <= maxRankNumber)
					maxRankNumber--; // spend a rank
				else return true;
			}
			else {
				maxRankNumber = rankGroupNumber-1; // first number in current group is spent on current epithet
				lastRankNumber = rankGroupNumber; // set last rank to be the one exactly before the current one
				lastRankGroupNumber = rankGroupNumber;
			}
		}
		return false;
	}
	
	private static final boolean isRankGroupSizeBroken(Annotation[] epithets, String[] rankGroupNames, int[] rankGroupSizes) {
		int[] rankGroupCounts = new int[rankGroupSizes.length];
		System.arraycopy(rankGroupSizes, 0, rankGroupCounts, 0, rankGroupCounts.length);
		
		for (int e = 0; e < epithets.length; e++) {
			String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
			for (int rg = 0; rg < rankGroupNames.length; rg++)
				if (rankGroupNames[rg].equals(rankGroup)) {
					rankGroupCounts[rg]--;
					if (rankGroupCounts[rg] < 0) {
						rankGroupCounts[0] = Integer.MIN_VALUE;
						rg = rankGroupNames.length;
						e = epithets.length;
					}
				}
		}
		return (rankGroupCounts[0] < 0);
	}
	
	private static final boolean isEpithetMissing(Annotation[] epithets, Set requiredRankGroupNumbers, Set requiredRankNumbers) {
		
		//	collect epithet rank and rank group numbers
		ArrayList epithetRankGroupNumbers = new ArrayList();
		ArrayList epithetRankNumbers = new ArrayList();
		for (int e = 0; e < epithets.length; e++) {
			epithetRankGroupNumbers.add(new Integer((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE)));
			String rankNumberString = ((String) epithets[e].getAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE));
			if (rankNumberString != null)
				epithetRankNumbers.add(new Integer(rankNumberString));
		}
		
		//	got all the ranks
		if (epithetRankNumbers.size() == epithets.length)
			return isNumberMissing(epithetRankNumbers, requiredRankNumbers);
		
		//	ranks missing, check groups only
		else return isNumberMissing(epithetRankGroupNumbers, requiredRankGroupNumbers);
	}
	
	private static final boolean isNumberMissing(Collection numbers, Set requiredNumbers) {
		TreeSet numberSet = new TreeSet(numbers);
		int minNumber = ((Integer) numberSet.first()).intValue();
		int maxNumber = ((Integer) numberSet.last()).intValue();
		for (Iterator rnit = requiredNumbers.iterator(); rnit.hasNext();) {
			Integer requiredNumber = ((Integer) rnit.next());
			if ((minNumber < requiredNumber.intValue()) && (requiredNumber.intValue() < maxNumber) && !numberSet.contains(requiredNumber))
				return true;
		}
		return false;
	}
	
	private static boolean isRankOrderBroken(Annotation[] epithets, OmniFAT.DocumentDataSet docData) {
		
		//	extract epithets and group them in rank groups
		OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
		for (int rg = 0; rg < rankGroups.length; rg++) {
			ArrayList rankGroupEpithetList = new ArrayList();
			for (int e = 0; e < epithets.length; e++) {
				if (rankGroups[rg].getOrderNumber() == Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE)))
					rankGroupEpithetList.add(epithets[e]);
			}
			
			if (rankGroupEpithetList.isEmpty())
				continue;
			
			Annotation[] rankGroupEpithets = ((Annotation[]) rankGroupEpithetList.toArray(new Annotation[rankGroupEpithetList.size()]));
			if (isRankOrderBroken(rankGroupEpithets, rankGroups[rg])) 
				return true;
		}
		
		//	no problems found
		return false;
	}
	
	private static final boolean isRankOrderBroken(Annotation[] epithets, OmniFAT.RankGroup rankGroup) {
		
		//	gather base data
		int maxGroupRank = rankGroup.getOrderNumber();
		int minGroupRank = (maxGroupRank - rankGroup.getRanks().length + 1);
		
		//	initialize sets and set existing ranks
		final String[] epithetValues = new String[epithets.length];
		final TreeSet[] epithetRankNumbers = new TreeSet[epithets.length];
		for (int e = 0; e < epithets.length; e++) {
			epithetValues[e] = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
			epithetRankNumbers[e] = new TreeSet();
			if (epithets[e].hasAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE))
				epithetRankNumbers[e].add(new Integer((String) epithets[e].getAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE)));
		}
		
		//	fill in possible ranks for so far un-ranked epithets
		for (int e = 0; e < epithets.length; e++)
			if (epithetRankNumbers[e].isEmpty()) {
				int maxRank = (maxGroupRank - e);
				int minRank = (minGroupRank + (epithets.length - 1) - e);
				for (int r = maxRank; r >= minRank; r--)
					epithetRankNumbers[e].add(new Integer(r));
			}
		
		//	reduce rank numbers per epithet based on rank order consistency
		boolean ranksReduced;
		do {
			ranksReduced = false;
			for (int e = 0; e < epithets.length; e++)
				if (epithetRankNumbers[e].size() > 1) {
					int maxRank;
					if (e == 0)
						maxRank = maxGroupRank;
					else if (epithetRankNumbers[e-1].isEmpty())
						return true;
					else maxRank = (((Integer) epithetRankNumbers[e-1].last()).intValue() - 1);
					
					int minRank;
					if ((e+1) == epithets.length)
						minRank = minGroupRank;
					else if (epithetRankNumbers[e+1].isEmpty())
						return true;
					else minRank = (((Integer) epithetRankNumbers[e+1].first()).intValue() + 1);
					
					for (Iterator rnit = epithetRankNumbers[e].iterator(); rnit.hasNext();) {
						int rank = ((Integer) rnit.next()).intValue();
						if ((rank > maxRank) || (rank < minRank)) {
							rnit.remove();
							ranksReduced = true;
						}
					}
				}
		} while (ranksReduced);
		
		//	if some set is empty, there is no possible rank combination
		return (sizeProduct(epithetRankNumbers) == 0);
	}
	
	public static void applyPrecisionRules(MutableAnnotation data, OmniFAT omniFat) {
		applyPrecisionRules(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void applyPrecisionRules(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	apply rules
		removeForNegativeStartEpithet(data, docData);
		promoteForNewNameLabels(data, docData);
		promoteForLabeledLastEpithet(data, docData);
		promoteForRepeatedEpithet(data, docData);
		
		//	promote epithets of newly found positives
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			Annotation[] epithets = docData.getEpithets(taxonNames[t]);
			for (int e = 0; e < epithets.length; e++)
				epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
		}
	}
	
	private static void removeForNegativeStartEpithet(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	remove candidates whose first epithet is negative
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			boolean firstEpithet = true;
			for (int e = 0; e < epithets.length; e++) {
				if (firstEpithet && OmniFAT.NEGATIVE_STATE.equals(epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE))) {
					docData.log(("Removing for negative first epithet '" + epithets[e].toXML() + "':\n  " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					data.removeAnnotation(taxonNameCandidates[c]);
					e = epithets.length;
				}
				firstEpithet = false;
			}
		}
	}
	
	private static void promoteForNewNameLabels(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	promote candidates succeeded by a 'new epithet' label
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		Arrays.sort(taxonNameCandidates, precisionRuleEndIndexOrder);
		Annotation[] newEpithetLabels = data.getAnnotations(OmniFAT.NEW_EPITHET_LABEL_TYPE);
		int neli = 0;
		
		//	collect IDs of to-promote candidates
		HashSet promoteIdSet = new HashSet();
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			while ((neli < newEpithetLabels.length)
				&&
				(taxonNameCandidates[c].getEndIndex() > newEpithetLabels[neli].getStartIndex()))
					neli++;
			
			if ((neli < newEpithetLabels.length)
				&&
					((newEpithetLabels[neli].getStartIndex() == taxonNameCandidates[c].getEndIndex())
					||
						(((newEpithetLabels[neli].getStartIndex() - taxonNameCandidates[c].getEndIndex()) == 1)
						&&
						!Gamta.isWord(data.valueAt(taxonNameCandidates[c].getEndIndex()))))
				) {
				if (matches(taxonNameCandidates[c], newEpithetLabels[neli], docData))
					promoteIdSet.add(taxonNameCandidates[c].getAnnotationID());
			}
		}
		
		//	remove candidates nested in to-promote annotations
		taxonNameCandidates = removeOverlappingCandidates(data, taxonNameCandidates, promoteIdSet, docData);
		
		//	promote candidates
		promoteCandidates(taxonNameCandidates, promoteIdSet, OmniFAT.NEW_LABEL_EVIDENCE, null, "labeled as new", docData);
	}
	
	private static final boolean matches(QueriableAnnotation taxonNameCandidate, Annotation newEpithetLabel, OmniFAT.DocumentDataSet docData) {
		Annotation[] epithets = docData.getEpithets(taxonNameCandidate);
		Annotation lastEpithet = null;
		for (int e = 0; e < epithets.length; e++)
			lastEpithet = epithets[e];
		
		if (lastEpithet == null)
			return false;
		
		String lastEpithetRank = ((String) lastEpithet.getAttribute(OmniFAT.RANK_ATTRIBUTE));
		if ((lastEpithetRank != null) && !lastEpithetRank.equals(newEpithetLabel.getAttribute(OmniFAT.RANK_ATTRIBUTE)))
			return false;
		return lastEpithet.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE).equals(newEpithetLabel.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
	}
	
	private static Comparator precisionRuleEndIndexOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			Annotation a1 = ((Annotation) o1);
			Annotation a2 = ((Annotation) o2);
			int c = a1.getEndIndex() - a2.getEndIndex();
			return ((c == 0) ? (a2.size() - a1.size()) : c);
		}
	};
	
	private static void addSureEpithetCombination(QueriableAnnotation taxonName, OmniFAT.DocumentDataSet docData) {
		StringVector epithetCombination = new StringVector();
		Annotation[] epithets = docData.getEpithets(taxonName);
		for (int e = 0; e < epithets.length; e++)
			epithetCombination.addElement((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
		docData.addSureEpithetCombination(epithetCombination);
	}
	
	private static void promoteForLabeledLastEpithet(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	promote candidates whose last epithet is labeled
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		HashSet promoteIdSet = new HashSet();
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			Annotation lastEpithet = null;
			for (int e = 0; e < epithets.length; e++)
				lastEpithet = epithets[e];
			if ((lastEpithet != null) && OmniFAT.LABEL_EVIDENCE.equals(lastEpithet.getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE)))
				promoteIdSet.add(taxonNameCandidates[c].getAnnotationID());
		}
		
		//	remove candidates nested in to-promote annotations
		taxonNameCandidates = removeOverlappingCandidates(data, taxonNameCandidates, promoteIdSet, docData);
		
		//	promote candidates
		promoteCandidates(taxonNameCandidates, promoteIdSet, OmniFAT.LABEL_EVIDENCE, null, "with labeled last epithet", docData);
	}
	
	private static void promoteForRepeatedEpithet(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	promote candidates whose second-to-last epithet is the same as the last epithet
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		HashSet promoteIdSet = new HashSet();
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			Annotation secondToLastEpithet = null;
			Annotation lastEpithet = null;
			for (int e = 0; e < epithets.length; e++) {
				secondToLastEpithet = lastEpithet;
				lastEpithet = epithets[e];
			}
			if ((secondToLastEpithet != null) && (lastEpithet != null)) {
				String stlEpithet = ((String) secondToLastEpithet.getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				if ((stlEpithet != null) && stlEpithet.endsWith("."))
					stlEpithet = stlEpithet.substring(0, (stlEpithet.length()-1));
				String lEpithet = ((String) lastEpithet.getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				if (!lEpithet.startsWith(stlEpithet))
					continue;
				
				if (OmniFAT.ABBREVIATED_STATE.equals(lastEpithet.getAttribute(OmniFAT.STATE_ATTRIBUTE))
						||
						(
							OmniFAT.NEGATIVE_STATE.equals(lastEpithet.getAttribute(OmniFAT.STATE_ATTRIBUTE))
							&&
							(
								OmniFAT.ABBREVIATED_STATE.equals(secondToLastEpithet.getAttribute(OmniFAT.STATE_ATTRIBUTE))
								||
								OmniFAT.NEGATIVE_STATE.equals(secondToLastEpithet.getAttribute(OmniFAT.STATE_ATTRIBUTE))
							)
						)
					) continue;
				
				int stlEpithetBase = Integer.parseInt((String) secondToLastEpithet.getAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE));
				int lEpithetBase = Integer.parseInt((String) lastEpithet.getAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE));
				if ((lEpithetBase - stlEpithetBase) > (stlEpithet.endsWith(".") ? 2 : 1))
					continue;
				
				OmniFAT.RankGroup stlRankGroup = docData.omniFat.getRankGroup(Integer.parseInt((String) secondToLastEpithet.getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE)));
				OmniFAT.RankGroup lRankGroup = docData.omniFat.getRankGroup(Integer.parseInt((String) lastEpithet.getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE)));
				if ((stlRankGroup.getOrderNumber() != lRankGroup.getOrderNumber()) || !lRankGroup.hasRepeatedEpithets())
					continue;
				
				if (stlEpithet.length() < docData.omniFat.getMaxAbbreviationLength())
					secondToLastEpithet.setAttribute(OmniFAT.VALUE_ATTRIBUTE, lEpithet);
				promoteIdSet.add(taxonNameCandidates[c].getAnnotationID());
			}
		}
		
		//	remove candidates nested in to-promote annotations
		taxonNameCandidates = removeOverlappingCandidates(data, taxonNameCandidates, promoteIdSet, docData);
		
		//	promote candidates
		promoteCandidates(taxonNameCandidates, promoteIdSet, OmniFAT.EQUAL_EPITHETS_EVIDENCE, null, "with two equal ending epithets", docData);
	}
	
	private static final QueriableAnnotation[] removeOverlappingCandidates(MutableAnnotation data, QueriableAnnotation[] taxonNameCandidates, final Set promoteIdSet, OmniFAT.DocumentDataSet docData) {
		/* 
			remove all candidates that, in relation to a given to-promote candidate C:
			- are also marked for promotion and are fully contained in, but not equal to C (true infixes)
			- are not marked for promotion and
			  - are fully contained in C, but not equal to C (true infixes)
			  - are equal to C (alternative epithet arrangements for the same token sequence)
			  - overlap with the start of C, but do not fully cover C (non-covering predecessors in doc order)
			  - overlap with the end of C, but do not fully cover C (non-covered successors in doc order)
			
			after each rule:
			- remove positives nested in, but not equal to other positives (true infixes)
  		 */
		
		//	collect IDs of candidates to remove
		HashSet removeIdSet = new HashSet();
		
		//	collect IDs of candidates overlapping with the start of to-promote candidates
		Arrays.sort(taxonNameCandidates, new Comparator() {
			public int compare(Object o1, Object o2) {
				Annotation a1 = ((Annotation) o1);
				Annotation a2 = ((Annotation) o2);
				int c;
				
				c = (a1.getEndIndex() - a2.getEndIndex()); // sort by end index ascending
				if (c != 0) return c;
				
				if (promoteIdSet.contains(a1.getAnnotationID())) c++; // sort to-promote candidates last
				if (promoteIdSet.contains(a2.getAnnotationID())) c--;
				if (c != 0) return c;
				
				return (a1.size() - a2.size()); // sort by size ascending
				
				//	==> outmost to-promote candidates last in every end index group
				//	==> backward search finds all to-remove candidates
			}
		});
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			if (!promoteIdSet.contains(taxonNameCandidates[c].getAnnotationID()))
				continue;
			
			//	search to-remove candidates backward from current to-promote candidate
			for (int r = (c-1); r >= 0; r--) {
				
				//	tested candidates ends before start of current to-promote candidate ==> we're done
				if (taxonNameCandidates[r].getEndIndex() <= taxonNameCandidates[c].getStartIndex())
					r = -1;
				
				//	tested candidate has same end index as current to-promote candidate
				else if (taxonNameCandidates[r].getEndIndex() == taxonNameCandidates[c].getEndIndex()) {
					
					//	remove tested candidate if fully covered by current to-promote candidate
					if (taxonNameCandidates[r].size() < taxonNameCandidates[c].size())
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
					
					//	tested candidate is equal to current to-promote candidate ==> remove only if not to-promote itself
					else if ((taxonNameCandidates[r].size() == taxonNameCandidates[c].size()) && !promoteIdSet.contains(taxonNameCandidates[r].getAnnotationID()))
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
				}
				
				//	tested candidate ends somewhere inside current to-promote candidate
				else {
					
					//	remove tested candidate if fully covered by current to-promote candidate
					if (taxonNameCandidates[r].getStartIndex() >= taxonNameCandidates[c].getStartIndex())
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
					
					//	tested candidate is non-covering predecessor of current to-promote candidate ==> remove only if not to-promote itself
					else if (!promoteIdSet.contains(taxonNameCandidates[r].getAnnotationID()))
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
				}
			}
		}
		
		//	collect IDs of candidates overlapping with the end of to-promote candidates
		Arrays.sort(taxonNameCandidates, new Comparator() {
			public int compare(Object o1, Object o2) {
				Annotation a1 = ((Annotation) o1);
				Annotation a2 = ((Annotation) o2);
				int c;
				
				c = (a1.getStartIndex() - a2.getStartIndex()); // sort by start index ascending
				if (c != 0) return c;
				
				if (promoteIdSet.contains(a1.getAnnotationID())) c--; // sort to-promote candidates first
				if (promoteIdSet.contains(a2.getAnnotationID())) c++;
				if (c != 0) return c;
				
				return (a2.size() - a1.size()); // sort by size descending
				
				//	==> outmost to-promote candidates first in every start index group
				//	==> forward search finds all to-remove candidates
			}
		});
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			if (!promoteIdSet.contains(taxonNameCandidates[c].getAnnotationID()))
				continue;
			
			//	search to-remove candidates forward from current to-promote candidate
			for (int r = (c+1); r < taxonNameCandidates.length; r++) {
				
				//	tested candidates starts after end of current to-promote candidate ==> we're done
				if (taxonNameCandidates[r].getStartIndex() >= taxonNameCandidates[c].getEndIndex())
					r = taxonNameCandidates.length;
				
				//	tested candidate has same start index as current to-promote candidate
				else if (taxonNameCandidates[r].getStartIndex() == taxonNameCandidates[c].getStartIndex()) {
					
					//	remove tested candidate if fully covered by current to-promote candidate
					if (taxonNameCandidates[r].size() < taxonNameCandidates[c].size())
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
					
					//	tested candidate is equal to current to-promote candidate ==> remove only if not to-promote itself
					else if ((taxonNameCandidates[r].size() == taxonNameCandidates[c].size()) && !promoteIdSet.contains(taxonNameCandidates[r].getAnnotationID()))
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
				}
				
				//	tested candidate start somewhere inside current to-promote candidate
				else {
					
					//	remove tested candidate if fully covered by current to-promote candidate
					if (taxonNameCandidates[r].getEndIndex() <= taxonNameCandidates[c].getEndIndex())
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
					
					//	tested candidate is non-covered successor of current to-promote candidate ==> remove only if not to-promote itself
					else if (!promoteIdSet.contains(taxonNameCandidates[r].getAnnotationID()))
						removeIdSet.add(taxonNameCandidates[r].getAnnotationID());
				}
			}
		}
		
		//	remove candidates overlapping with to-promote candidates, collect remaining ones along the way
		ArrayList tncList = new ArrayList();
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			if (removeIdSet.contains(taxonNameCandidates[c].getAnnotationID())) {
				docData.log(("  Removing candidate overlapping with to-promote candidate: " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				data.removeAnnotation(taxonNameCandidates[c]);
			}
			else tncList.add(taxonNameCandidates[c]);
		}
		
		
		//	sort and return remaining candidates (spares re-getting them)
		taxonNameCandidates = ((QueriableAnnotation[]) tncList.toArray(new QueriableAnnotation[tncList.size()]));
		Arrays.sort(taxonNameCandidates);
		return taxonNameCandidates;
	}
	
	private static final boolean promoteCandidates(QueriableAnnotation[] taxonNameCandidates, final Set promoteIdSet, String evidence, String evidenceDetail, String logReason, OmniFAT.DocumentDataSet docData) {
		/* 
			if more than one equal candidate remains
			- promote none
			- mark all for user feedback (feedback candidates)
			  - set state to 'likely' for most probable candidate
			  - set state to 'uncertain' for all others
			
			after each rule:
			- remove feedback candidates nested in positives
  		 */
		boolean promoted = false;
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			if (!promoteIdSet.contains(taxonNameCandidates[c].getAnnotationID()))
				continue;
			
			docData.log(("Assessing promotion of candidate " + logReason + ": " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
			ArrayList candidates = new ArrayList();
			candidates.add(taxonNameCandidates[c]);
			
			//	collect possible alternatives and overlapping candidates
			for (int l = (c+1); l < taxonNameCandidates.length; l++) {
				if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[l], false)) {
					if (promoteIdSet.contains(taxonNameCandidates[l].getAnnotationID())) {
						docData.log(("  Found alternative candidate: " + taxonNameCandidates[l].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
						candidates.add(taxonNameCandidates[l]);
					}
				}
				else l = taxonNameCandidates.length;
			}
			
			//	no alternative epithet compination for same piece of text ==> promote candidate
			if (candidates.size() == 1) {
				taxonNameCandidates[c].setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, evidence);
				if (evidenceDetail != null)
					taxonNameCandidates[c].setAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, evidenceDetail);
				taxonNameCandidates[c].setAttribute(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE, docData.omniFat.getName());
				taxonNameCandidates[c].changeTypeTo(OmniFAT.TAXONOMIC_NAME_TYPE);
				
				Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
				for (int e = 0; e < epithets.length; e++)
					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
				
				addSureEpithetCombination(taxonNameCandidates[c], docData);
				docData.omniFat.learnEpithets(docData.getEpithets(taxonNameCandidates[c]));
				
				docData.log(("  Promoting candidate " + logReason + ": " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				promoted = true;
			}
			
			//	epithet combination unclear, jump whole block of candidates and leave them to later rules
			else while (((c+1) < taxonNameCandidates.length) && (taxonNameCandidates[c].getStartIndex() == taxonNameCandidates[c+1].getStartIndex()))
				c++;
		}
		return promoted;
	}
	
	public static void applyAuthorNameRules(MutableAnnotation data, OmniFAT omniFat) {
		applyAuthorNameRules(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void applyAuthorNameRules(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	get positives
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		
		//	collect author names from positives
		for (int t = 0; t < taxonNames.length; t++) {
			QueriableAnnotation[] epithets = docData.getEpithets(taxonNames[t]);
			for (int e = 0; e < epithets.length; e++) {
				String authorName = ((String) epithets[e].getAttribute(OmniFAT.AUTHOR_ATTRIBUTE));
				if (authorName != null) {
					docData.addAuthorNameParts(TokenSequenceUtils.getTextTokens(data.getTokenizer().tokenize(authorName)));
					docData.addAuthorName(authorName);
				}
			}
		}
		
		//	expoit author names
		QueriableAnnotation[] taxonNameCandidates;
		
		//	remove candidates with sure positive author name in epithet position
		taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			if (taxonNameCandidates[c] == null)
				continue;
			
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			Annotation killEpithet = null;
			for (int e = 0; e < epithets.length; e++)
				if (docData.getAuthorNameParts().lookup((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE))) {
					killEpithet = epithets[e];
					e = epithets.length;
				}
			
			if (killEpithet != null) {
				docData.log(("Removing for author name epithet '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				data.removeAnnotation(taxonNameCandidates[c]);
			}
		}
		
		//	promote epithets with known author names
		taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			QueriableAnnotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			for (int e = 0; e < epithets.length; e++) {
				if (OmniFAT.POSITIVE_STATE.equals(epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE)))
					continue;
				
				String authorName = ((String) epithets[e].getAttribute(OmniFAT.AUTHOR_ATTRIBUTE));
				if ((authorName != null) && docData.isAuthorName(authorName)) {
					docData.log(("Promoting epithet for known author name '" + authorName + "':\n  " + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
					epithets[e].setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.AUTHOR_EVIDENCE);
				}
			}
		}
	}
	
	public static void applyDataRules(MutableAnnotation data, OmniFAT omniFat) {
		applyDataRules(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void applyDataRules(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		HashSet promoteIDs = new HashSet();
		
		//	pre-filter epithets
		filterDanglingEpithets(data, docData);
		
		//	store capitalization rules
		HashSet capitalizedRankGroups = new HashSet();
		OmniFAT.RankGroup[] rgs = docData.omniFat.getRankGroups();
		for (int rg = 0; rg < rgs.length; rg++) {
			if (rgs[rg].isEpithetsCapitalized())
				capitalizedRankGroups.add(rgs[rg].getName());
		}
		
		//	apply rules block-wise
		OmniFAT.DataRuleSet[] ruleSets = docData.omniFat.getDataRuleSets();
		for (int s = 0; s < ruleSets.length; s++) {
			
			//	get rule block
			OmniFAT.DataRule[] ruleBlock = ruleSets[s].getRules();
			
			//	apply rules
			boolean newPositiveEpithets;
			boolean newAssignments;
			int round = 0;
			do {
				newPositiveEpithets = false;
				newAssignments = false;
				round++;
				System.out.println("Data Rules " + (s+1) + "-" + round);
				
				//	collect epithets and author names from positives
				fillPositiveLists(data, docData);
				
				//	use author names to filter candidates
				filterForAuthorNameEpithets(data, docData);
				filterForEpithetAuthorNames(data, docData, capitalizedRankGroups);
				filterEpithetAbbreviations(data, docData, capitalizedRankGroups);
				
				//	use positive epithets and author names to change states of candidate epithets
				newPositiveEpithets = (newPositiveEpithets | promoteEpithetsForAuthor(data, docData, (s+1), round));
				newPositiveEpithets = (newPositiveEpithets | promoteEpithetsForValue(data, docData, (s+1), round));
				newPositiveEpithets = (newPositiveEpithets | promoteEpithetsForValueFuzzy(data, docData, (s+1), round));
				newPositiveEpithets = (newPositiveEpithets | promoteEpithetsForMorphology(data, docData, (s+1), round));
				
				//	apply rules
				for (int r = 0; r < ruleBlock.length; r++)
					newAssignments = (newAssignments | applyRule(data, ruleBlock[r], docData, (s+1), round, promoteIDs));
				
				//	promote subsets of known combinations
				newAssignments = (newAssignments | promoteCandidatesForCombination(data, docData, (s+1), round));
				
				//	complete abbreviated epithets based on new positives
				newAssignments = (newAssignments | fillInAbbreviations(data, docData));
				
				//	remove dangling epithets after promoting a candidate and removing nested ones
				newAssignments = (newAssignments | filterDanglingEpithets(data, docData));
				
				//	remove epithets built on the same base index as a more secure one
				newAssignments = (newAssignments | filterEpithetsByState(data, docData));
				
				//	remove candidates who have had an epithet removed
				newAssignments = (newAssignments | filterForMissingEpithets(data, docData));
				
				//	remove dangling epithets after removing candidates
				newAssignments = (newAssignments | filterDanglingEpithets(data, docData));
				
				//	clean up
				removeSelfContained(data, OmniFAT.TAXONOMIC_NAME_TYPE);
				
			} while (newAssignments && (round != ruleSets[s].getMaxRounds()));
		}
		
		//	clean up
		AnnotationFilter.removeContained(data, OmniFAT.TAXONOMIC_NAME_TYPE, OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE);
		AnnotationFilter.removeContained(data, OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE, OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		
		//	turn candidates that were matched by a promoting rule, but could not be promoted due to being duplicates into external candidates
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		Annotation firstCandidateInSet = null;
		for (int c = 0; c < taxonNameCandidates.length; c++)
			if (promoteIDs.contains(taxonNameCandidates[c].getAnnotationID())) {
				docData.log(("Marking feedback candidate for duplicate rule match: " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				String state;
				if ((firstCandidateInSet != null) && AnnotationUtils.equals(firstCandidateInSet, taxonNameCandidates[c]))
					state = OmniFAT.UNCERTAIN_STATE;
				else {
					state = OmniFAT.LIKELY_STATE;
					firstCandidateInSet = taxonNameCandidates[c];
				}
				taxonNameCandidates[c].setAttribute(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE, docData.omniFat.getName());
				taxonNameCandidates[c].setAttribute(OmniFAT.STATE_ATTRIBUTE, state);
				taxonNameCandidates[c].changeTypeTo(OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE);
			}
		
		//	remove any remaining internal candidates
		if (docData.omniFat.cleanUpAfterDataRules())
			AnnotationFilter.removeAnnotations(data, OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		
		//	clean up epithets
		filterDanglingEpithets(data, docData);
	}
	
	private static final void removeSelfContained(MutableAnnotation data, String type) {
		/*
		 * this implementation is cloned from AnnotationFilter and altered so
		 * that it does not removing (index-based) duplicates, which might still
		 * have different epithet combinations.
		 */
		
		//	get IDs of affected annotations
		Annotation[] annotations = data.getAnnotations(type);
		HashSet removeIDs = new HashSet();
		int inner = 0;
		for (int outer = 0; outer < annotations.length; outer++) {
			
			//	find next possibly nested annotation
			while ((inner < annotations.length) && (annotations[inner].getStartIndex() < annotations[outer].getStartIndex()))
				inner++;
			
			//	find all nested inner annotation for current outer annotation
			for (int i = inner; i < annotations.length; i++) {
				if (annotations[i].getStartIndex() >= annotations[outer].getEndIndex())
					i = annotations.length;
				else if (AnnotationUtils.equals(annotations[outer], annotations[i], false)) {}
				else if (AnnotationUtils.contains(annotations[outer], annotations[i]))
					removeIDs.add(annotations[i].getAnnotationID());
			}
		}
		
		//	remove annotations
		for (int r = 0; r < annotations.length; r++) {
			if (removeIDs.contains(annotations[r].getAnnotationID()))
				data.removeAnnotation(annotations[r]);
		}
	}
	
	private static final boolean applyRule(MutableAnnotation data, OmniFAT.DataRule rule, OmniFAT.DocumentDataSet docData, int ruleSet, int round, Set promoteIDs) {
		
		//	collect candidates matching rule
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		final Properties ruleMatchIDs = new Properties();
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			String epithetStatusString = OmniFAT.DataRule.getEpithetStatusString(docData.getEpithets(taxonNameCandidates[c]));
			if (rule.matches(epithetStatusString))
				ruleMatchIDs.setProperty(taxonNameCandidates[c].getAnnotationID(), epithetStatusString);
		}
		
		if (DataRule.REMOVE_ACTION.equals(rule.action)) {
			for (int c = 0; c < taxonNameCandidates.length; c++)
				if (ruleMatchIDs.containsKey(taxonNameCandidates[c].getAnnotationID())) {
					docData.log(("Removing candidate for rule match '" + ruleMatchIDs.getProperty(taxonNameCandidates[c].getAnnotationID()) + "': " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					data.removeAnnotation(taxonNameCandidates[c]);
				}
			
			//	did we exclude some candidate?
			return (ruleMatchIDs.size() != 0);
		}
		
		else if (DataRule.PROMOTE_ACTION.equals(rule.action)) {
			
			//	if there are multiple matching candidates for the same token sequence (alternative epithet arrangements), use only the one(s) with the longest status string
			for (int c = 0; c < taxonNameCandidates.length; c++) {
				if (!ruleMatchIDs.containsKey(taxonNameCandidates[c].getAnnotationID()))
					continue;
				
				//	search to-remove candidates forward from current to-promote candidate
				ArrayList candidates = new ArrayList();
				candidates.add(taxonNameCandidates[c]);
				
				//	collect possible alternatives and overlapping candidates
				for (int l = (c+1); l < taxonNameCandidates.length; l++) {
					if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[l], false)) {
						if (ruleMatchIDs.containsKey(taxonNameCandidates[l].getAnnotationID()))
							candidates.add(taxonNameCandidates[l]);
					}
					else l = taxonNameCandidates.length;
				}
				
				//	found multiple candidates, keep only the one(s) with the longest status string
				if (candidates.size() > 1) {
					int maxStatusStringLength = 0;
					
					//	find maximum status string length
					for (int s = 0; s < candidates.size(); s++) {
						String epithetStatusString = ruleMatchIDs.getProperty(((Annotation) candidates.get(s)).getAnnotationID(), "");
						maxStatusStringLength = Math.max(maxStatusStringLength, epithetStatusString.length());
					}
					
					//	sort out candidates with shorter status strings
					for (int s = 0; s < candidates.size(); s++) {
						String epithetStatusString = ruleMatchIDs.getProperty(((Annotation) candidates.get(s)).getAnnotationID(), "");
						if (epithetStatusString.length() < maxStatusStringLength)
							ruleMatchIDs.remove(((Annotation) candidates.get(s)).getAnnotationID());
					}
				}
			}
			
			//	remove candidates nested in to-promote annotations
			taxonNameCandidates = removeOverlappingCandidates(data, taxonNameCandidates, ruleMatchIDs.keySet(), docData);
			
			//	promote candidates
			return promoteCandidates(taxonNameCandidates, ruleMatchIDs.keySet(), (OmniFAT.DOCUMENT_EVIDENCE_PREFIX + ruleSet + "-" + round), rule.pattern, ("for rule match '" + rule.pattern + "'"), docData);
		}
		
		else if (DataRule.FEEDBACK_ACTION.equals(rule.action)) {
			
			//	mark annotations for feedback
			for (int c = 0; c < taxonNameCandidates.length; c++)
				if (ruleMatchIDs.containsKey(taxonNameCandidates[c].getAnnotationID())) {
					taxonNameCandidates[c].setAttribute(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE, docData.omniFat.getName());
					taxonNameCandidates[c].setAttribute(OmniFAT.STATE_ATTRIBUTE, (rule.likelyMatch ? OmniFAT.LIKELY_STATE : OmniFAT.UNCERTAIN_STATE));
					taxonNameCandidates[c].changeTypeTo(OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE);
					docData.log(("Marking feedback candidate for rule match '" + ruleMatchIDs.getProperty(taxonNameCandidates[c].getAnnotationID()) + "': " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				}
			
			//	remove candidates nested in feedback annotations if configured
			if (rule.removeNested)
				AnnotationFilter.removeContained(data, OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE, OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
			
			//	did we create some feedback candidate?
			return (ruleMatchIDs.size() != 0);
		}
		
		//	theoretically unreachable, but with a non-valid config file, it might happen ...
		else return false;
	}
	
	private static final void fillPositiveLists(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		
		//	collect epithets from positives
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			QueriableAnnotation[] epithets = docData.getEpithets(taxonNames[t]);
			for (int e = 0; e < epithets.length; e++) {
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
				if (epithet.length() < (epithet.endsWith(".") ? 4 : 3))
					continue;
				
				String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
				docData.addSureEpithet(rankGroup, epithet);
				
				String authorName = ((String) epithets[e].getAttribute(OmniFAT.AUTHOR_ATTRIBUTE));
				if (authorName != null) {
					docData.addAuthorNameParts(TokenSequenceUtils.getTextTokens(data.getTokenizer().tokenize(authorName)));
					docData.addAuthorName(authorName);
				}
			}
		}
	}
	
	private static final boolean filterForAuthorNameEpithets(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		Dictionary authorNameParts = docData.getSureAuthorNameParts();
		boolean change = false;
		
		//	remove candidates with sure positive author name in epithet position
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			Annotation killEpithet = null;
			for (int e = 0; e < epithets.length; e++)
				if (authorNameParts.lookup((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE))) {
					killEpithet = epithets[e];
					e = epithets.length;
				}
			
			if (killEpithet != null) {
				docData.log(("Removing for author name epithet '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				data.removeAnnotation(taxonNameCandidates[c]);
				change = true;
			}
		}
		
		//	did we exclude some candidate?
		return change;
	}
	
	private static final boolean filterForEpithetAuthorNames(MutableAnnotation data, OmniFAT.DocumentDataSet docData, HashSet capitalizedRankGroups) {
		OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
		boolean change = false;
		
		//	remove candidates with sure positive epithets in author name position
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			QueriableAnnotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			Annotation killEpithet = null;
			String killAuthor = null;
			for (int e = 0; e < epithets.length; e++) {
				String authorName = ((String) epithets[e].getAttribute(OmniFAT.AUTHOR_ATTRIBUTE));
				if (authorName != null) {
					StringVector authorNameTokens = TokenSequenceUtils.getTextTokens(data.getTokenizer().tokenize(authorName));
					for (int t = 0; t < authorNameTokens.size(); t++)
						if (authorNameTokens.get(t).length() < 3)
							authorNameTokens.remove(t--);
					
					for (int rg = 0; rg < rankGroups.length; rg++) {
						String rankGroup = rankGroups[rg].getName();
						if (capitalizedRankGroups.contains(rankGroup)) {
							for (int t = 0; t < authorNameTokens.size(); t++)
								if (docData.isSureEpithet(rankGroup, authorNameTokens.get(t))) {
									killAuthor = authorName;
									killEpithet = epithets[e];
									t = authorNameTokens.size();
									rg = rankGroups.length;
									e = epithets.length;
								}
						}
					}
				}
			}
			
			if (killEpithet != null) {
				docData.log(("Removing for epithet author name '" + killAuthor + "' in '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				data.removeAnnotation(taxonNameCandidates[c]);
				change = true;
			}
		}
		
		//	did we exclude some candidate?
		return change;
	}
	
	private static final boolean filterDanglingEpithets(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		boolean change = false;
		
		//	collect epithets bound to positives or candidates
		HashSet epithetIdSet = new HashSet();
		QueriableAnnotation[] positives;
		positives = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < positives.length; c++) {
			String source = ((String) positives[c].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
			epithetIdSet.addAll(Arrays.asList(source.split("\\,")));
		}
		positives = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int c = 0; c < positives.length; c++) {
			String source = ((String) positives[c].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
			epithetIdSet.addAll(Arrays.asList(source.split("\\,")));
		}
		positives = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < positives.length; c++) {
			String source = ((String) positives[c].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
			epithetIdSet.addAll(Arrays.asList(source.split("\\,")));
		}
		
		//	check epithets
		QueriableAnnotation[] epithets = data.getAnnotations(OmniFAT.EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++)
			if (!epithetIdSet.contains(epithets[e].getAnnotationID())) {
				docData.log(("Removing dangeling epithet '" + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
				data.removeAnnotation(epithets[e]);
				change = true;
			}
		
		//	did we exclude some epithet?
		return change;
	}
	
	private static final boolean fillInAbbreviations(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		boolean change = false;
		
		//	try to fill in abbreviations based on known positives
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			StringVector fullEpithets = new StringVector();
			ArrayList abbreviatedEpithets = new ArrayList();
			for (int e = 0; e < epithets.length; e++) {
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				if (epithet.length() > (epithet.endsWith(".") ? 3 : 2))
					fullEpithets.addElementIgnoreDuplicates(epithet);
				else abbreviatedEpithets.add(epithets[e]);
			}
			
			//	nothing to resolve, no need for gathering further data
			if (abbreviatedEpithets.isEmpty())
				continue;
			
			//	get epithets appearing in a sure positive together with the ones from the current candidate
			StringVector cooccurringEpithets = new StringVector();
			for (int e = 0; e < fullEpithets.size(); e++) {
				Dictionary cooccurringEpithetDictionary = docData.getSureCooccurringEpithets(fullEpithets.get(e));
				if (cooccurringEpithetDictionary != null)
					for (StringIterator ceit = cooccurringEpithetDictionary.getEntryIterator(); ceit.hasMoreStrings();)
						cooccurringEpithets.addElementIgnoreDuplicates(ceit.nextString());
			}
			
			//	try possible full forms
			for (int e = 0; e < abbreviatedEpithets.size(); e++) {
				String abbreviation = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
				Dictionary abbreviationResolver = docData.getAbbreviationResolver(rankGroup, abbreviation);
				if (abbreviationResolver != null) {
					StringVector fullForms = new StringVector();
					for (StringIterator arit = abbreviationResolver.getEntryIterator(); arit.hasMoreStrings();) {
						String fullForm = arit.nextString();
						if (cooccurringEpithets.contains(fullForm))
							fullForms.addElementIgnoreDuplicates(fullForm);
					}
					
					//	found un-ambiguous full fom
					if (fullForms.size() == 1) {
						docData.log(("Resolved '" + abbreviation + "' to '" + fullForms.get(0) + "' in " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
						epithets[e].setAttribute(OmniFAT.VALUE_ATTRIBUTE, fullForms.get(0));
						change = true;
					}
					
					//	no clear full form available (yet)
					else if (fullForms.size() > 1) {
						docData.log(("Got multiple possible full forms of '" + abbreviation + "' in " + taxonNameCandidates[c].toXML() + "\n  " + fullForms.concatStrings("\n  ")), OmniFAT.DEBUG_LEVEL_DETAILS);
					}
				}
			}
		}
		
		//	did we fill in some abbreviation?
		return change;
	}
	
	private static final boolean filterEpithetsByState(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		boolean change = false;
		
		//	sort epithets by base index
		Annotation[] epithets = data.getAnnotations(OmniFAT.EPITHET_TYPE);
		Arrays.sort(epithets, dataRuleBaseIndexOrder);
		
		for (int e = 0; e < epithets.length; e++) {
			int l = 1;
			while (((e+l) < epithets.length) && (dataRuleBaseIndexOrder.compare(epithets[e], epithets[e+l]) == 0))
				l++;
			
			if (l != 1) {
				Annotation[] duplicates = new Annotation[l];
				docData.log(("Inspecting block of duplicate epithets:"), OmniFAT.DEBUG_LEVEL_ALL);
				for (int d = 0; d < l; d++) {
					duplicates[d] = epithets[e+d];
					docData.log(("   " + ((String) epithets[e+d].getAttribute(OmniFAT.STATE_ATTRIBUTE)) + ": " + epithets[e+d].toXML()), OmniFAT.DEBUG_LEVEL_ALL);
				}
				Arrays.sort(duplicates, dataRuleStateAnnotationOrder);
				for (int d = 0; d < duplicates.length; d++)
					if (dataRuleStateAnnotationOrder.compare(duplicates[0], duplicates[d]) != 0) {
						docData.log(("  ==> removing inferior duplicate: " + duplicates[d].toXML()), OmniFAT.DEBUG_LEVEL_ALL);
						data.removeAnnotation(duplicates[d]);
					}
				e += (duplicates.length-1);
			}
		}
		
		//	did we exclude some epithet?
		return change;
	}
	
	private static Comparator dataRuleBaseIndexOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			int bi1 = Integer.parseInt((String) ((Annotation) o1).getAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE)); 
			int bi2 = Integer.parseInt((String) ((Annotation) o2).getAttribute(OmniFAT.BASE_INDEX_ATTRIBUTE));
			return ((bi1 == bi2) ? dataRuleStateAnnotationOrder.compare(o1, o2) : (bi1 - bi2));
		}
	};
	
	private static Comparator dataRuleStateOrder = new Comparator() {
		private HashMap stateNumbers = new HashMap();
		{
			this.stateNumbers.put(OmniFAT.POSITIVE_STATE, new Integer(1));
			this.stateNumbers.put(OmniFAT.LIKELY_STATE, new Integer(2));
			this.stateNumbers.put(OmniFAT.ABBREVIATED_STATE, new Integer(2)); // abbreviated is same as recall, as the document is a non-trusted dictionary by now
			this.stateNumbers.put(OmniFAT.AMBIGUOUS_STATE, new Integer(3));
			this.stateNumbers.put(OmniFAT.UNCERTAIN_STATE, new Integer(4));
			this.stateNumbers.put(OmniFAT.NEGATIVE_STATE, new Integer(5));
		}
		public int compare(Object o1, Object o2) {
			int stateNumber1 = ((Integer) this.stateNumbers.get(o1)).intValue();
			int stateNumber2 = ((Integer) this.stateNumbers.get(o2)).intValue();
			return (stateNumber1 - stateNumber2);
		}
	};
	private static Comparator dataRuleStateAnnotationOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			return dataRuleStateOrder.compare(((Annotation) o1).getAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.NEGATIVE_STATE), ((Annotation) o2).getAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.NEGATIVE_STATE));
		}
	};
	
	private static final boolean filterForMissingEpithets(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		boolean change = false;
		
		//	collect epithets
		HashSet epithetIdSet = new HashSet();
		QueriableAnnotation[] epithets = data.getAnnotations(OmniFAT.EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++)
			epithetIdSet.add(epithets[e].getAnnotationID());
		
		//	remove candidates with missing epithets
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			String source = ((String) taxonNameCandidates[c].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
			String[] epithetIds = source.split("\\,");
			for (int e = 0; e < epithetIds.length; e++)
				if (!epithetIdSet.contains(epithetIds[e])) {
					docData.log(("Removing candidate for missing epithet: " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					data.removeAnnotation(taxonNameCandidates[c]);
					change = true;
				}
		}
		
		//	did we exclude some epithet?
		return change;
	}
	
	private static final boolean filterEpithetAbbreviations(MutableAnnotation data, OmniFAT.DocumentDataSet docData, HashSet capitalizedRankGroups) {
		boolean change = false;
		
		//	collect all possible abbreviations (including those from candidates)
		HashMap localRankGroupEpithetLists = new HashMap();
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			for (int e = 0; e < epithets.length; e++) {
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
				if (epithet.length() > docData.omniFat.getMaxAbbreviationLength()) {
					String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
					StringVector localRankGroupEpithetList = ((StringVector) localRankGroupEpithetLists.get(rankGroup));
					if (localRankGroupEpithetList == null) {
						localRankGroupEpithetList = new StringVector();
						Dictionary rankGroupEpithetList = docData.getSureEpithetList(rankGroup);
						if (rankGroupEpithetList != null) {
							for (StringIterator sit = rankGroupEpithetList.getEntryIterator(); sit.hasMoreStrings();)
								localRankGroupEpithetList.addElementIgnoreDuplicates(sit.nextString());
						}
						localRankGroupEpithetLists.put(rankGroup, localRankGroupEpithetList);
					}
					for (int a = docData.omniFat.getMinAbbreviationLength(); a <= Math.min((epithet.length()-1), docData.omniFat.getMaxAbbreviationLength()); a++) {
						localRankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a));
						localRankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a) + ".");
					}
				}
			}
		}
		
		//	filter out candidates that include abbreviations with no possible resolution
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			if (taxonNameCandidates[c] == null)
				continue;
			
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			Annotation killEpithet = null;
			for (int e = 0; e < epithets.length; e++) {
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
				if (epithet.length() > (docData.omniFat.getMaxAbbreviationLength() + (epithet.endsWith(".") ? 1 : 0)))
					continue;
				
				StringVector localRankGroupEpithetList = ((StringVector) localRankGroupEpithetLists.get(epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE)));
				if ((localRankGroupEpithetList == null) || !localRankGroupEpithetList.contains(epithet)) {
					killEpithet = epithets[e];
					e = epithets.length;
				}
			}
			
			if (killEpithet != null) {
				docData.log(("Removing for epithet '" + killEpithet.toXML() + "' with impossible abbreviation:\n  " + taxonNameCandidates[c].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
				data.removeAnnotation(taxonNameCandidates[c]);
				change = true;
			}
		}
		
		//	did we exclude some candidate?
		return change;
	}
	
	private static final boolean promoteEpithetsForAuthor(MutableAnnotation data, OmniFAT.DocumentDataSet docData, int ruleSet, int round) {
		boolean change = false;
		
		//	promote epithets with known author names
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			QueriableAnnotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			for (int e = 0; e < epithets.length; e++) {
				if (OmniFAT.POSITIVE_STATE.equals(epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE)))
					continue;
				
				String authorName = ((String) epithets[e].getAttribute(OmniFAT.AUTHOR_ATTRIBUTE));
				if ((authorName != null) && docData.isAuthorName(authorName)) {
					docData.log(("Promoting epithet for known author name '" + authorName + "':\n  " + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_IMPORTANT);
					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
					epithets[e].setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.AUTHOR_EVIDENCE);
					change = true;
				}
			}
		}
		
		//	did we promote some epithet?
		return change;
	}
	
	private static final boolean promoteEpithetsForValue(MutableAnnotation data, OmniFAT.DocumentDataSet docData, int ruleSet, int round) {
		boolean change = false;
		
		//	promote epithets with known values
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			for (int e = 0; e < epithets.length; e++) {
				if (OmniFAT.POSITIVE_STATE.equals(epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE)))
					continue;
				
				if (docData.omniFat.getFuzzyMatchThreshold() != 0) {
					String evidence = ((String) epithets[e].getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE));
					if ((evidence != null) && evidence.startsWith(OmniFAT.FUZZY_EVIDENCE_PREFIX))
						continue;
				}
				
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
				if (docData.isSureAbbreviation(rankGroup, epithet)) {
					docData.log(("Got document positive abbreviation:\n  " + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
					epithets[e].setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, (OmniFAT.DOCUMENT_EVIDENCE_PREFIX + ruleSet + "-" + round));
//					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, (epithet.endsWith(".") ? OmniFAT.POSITIVE_STATE : OmniFAT.ABBREVIATED_STATE));
					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.ABBREVIATED_STATE); // TODOne TEST ==> TOO DANGEROUS, plus, intermediate abbreviation completion during data rules does the job better
					change = true;
				}
				else if (docData.isSureEpithet(rankGroup, epithet)) {
					docData.log(("Promoting epithet for document positive value:\n  " + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
					epithets[e].setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, (OmniFAT.DOCUMENT_EVIDENCE_PREFIX + ruleSet + "-" + round));
					epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.POSITIVE_STATE);
					change = true;
				}
			}
		}
		
		//	did we promote some epithet?
		return change;
	}
	
	private static final boolean promoteEpithetsForValueFuzzy(MutableAnnotation data, OmniFAT.DocumentDataSet docData, int ruleSet, int round) {
		int maxThreshold = docData.omniFat.getFuzzyMatchThreshold();
		if (maxThreshold == 0) return false;
		
		boolean change = false;
		
		//	promote epithets with known values
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			for (int e = 0; e < epithets.length; e++) {
				if (!OmniFAT.UNCERTAIN_STATE.equals(epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE)))
					continue;
				
				String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
				Dictionary rankGroupEpithets = docData.getSureEpithetList(rankGroup);
				if (rankGroupEpithets == null)
					continue;
				
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				int epithetMaxThreshold = maxThreshold;
				while (epithetMaxThreshold >= ((epithet.length()+1)/2))
					epithetMaxThreshold--;
				
				for (int threshold = 1; threshold <= epithetMaxThreshold; threshold++) {
					String fuzzyMatch = findFuzzyMatch(epithet, rankGroupEpithets, threshold, true);
					if (fuzzyMatch != null) {
						epithets[e].setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, (OmniFAT.FUZZY_EVIDENCE_PREFIX + "(" + threshold + ")-" + ruleSet + "-" + round));
						epithets[e].setAttribute(OmniFAT.VALUE_ATTRIBUTE, fuzzyMatch);
						epithets[e].setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.LIKELY_STATE);
						docData.log(("Promoting epithet for document fuzzy-positive value:\n  " + epithets[e].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("Promoting epithet for document fuzzy-positive value:\n  " + epithets[e].toXML());
						change = true;
					}
					// TODO consider doing fuzzy lookup in document text tokens to exclude negatives
				}
			}
		}
		
		//	did we promote some epithet?
		return change;
	}
	
	private static final boolean promoteEpithetsForMorphology(MutableAnnotation data, OmniFAT.DocumentDataSet docData, int ruleSet, int round) {
		boolean change = false;
		
		//	collect suffixes of sure positive epithets
		HashMap positiveSuffixes = new HashMap();
		ArrayList promotableEpithets = new ArrayList();
		HashSet nonNegativeTokens = new HashSet();
		Annotation[] epithets = data.getAnnotations(OmniFAT.EPITHET_TYPE);
		for (int e = 0; e < epithets.length; e++) {
			String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
			if (epithet.length() < ((epithet.endsWith(".") ? 4 : 3)))
				continue;
			
			String state = ((String) epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.NEGATIVE_STATE));
			
			if (OmniFAT.POSITIVE_STATE.equals(state)) {
				String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
				HashSet rankGroupEpithetSuffixes = ((HashSet) positiveSuffixes.get(rankGroup));
				if (rankGroupEpithetSuffixes == null) {
					rankGroupEpithetSuffixes = new HashSet();
					positiveSuffixes.put(rankGroup, rankGroupEpithetSuffixes);
				}
				for (int s = 0; s < (epithet.length()/2); s++) {
					if (!rankGroupEpithetSuffixes.add(epithet.substring(s)))
						s = epithet.length();
				}
			}
			else if (OmniFAT.UNCERTAIN_STATE.equals(state))
				promotableEpithets.add(epithets[e]);
			
			if (!OmniFAT.NEGATIVE_STATE.equals(state))
				nonNegativeTokens.add(epithet);
		}
		
		//	create negative suffixes
		HashSet negativeSuffixes = new HashSet();
		for (StringIterator ttit = docData.getTextTokens(); ttit.hasMoreStrings();) {
			String textToken = ttit.nextString();
			if (nonNegativeTokens.contains(textToken))
				continue;
			for (int s = 0; s < (textToken.length()-1); s++) {
				if (!negativeSuffixes.add(textToken.substring(s)))
					s = textToken.length();
			}
		}
		
		//	promote 'recall' and 'uncertain' epithets
		for (int e = 0; e < promotableEpithets.size(); e++) {
			Annotation epithetAnnotation = ((Annotation) promotableEpithets.get(e));
			String rankGroup = ((String) epithetAnnotation.getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
			HashSet rankGroupEpithetSuffixes = ((HashSet) positiveSuffixes.get(rankGroup));
			if (rankGroupEpithetSuffixes == null)
				continue;
			
			String epithet = ((String) epithetAnnotation.getAttribute(OmniFAT.VALUE_ATTRIBUTE));
			for (int s = 0; s < (epithet.length()/2); s++) {
				String suffix = epithet.substring(s);
				if (rankGroupEpithetSuffixes.contains(suffix) && !negativeSuffixes.contains(suffix)) {
					epithetAnnotation.setAttribute(OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.MORPHOLOGY_EVIDENCE);
					epithetAnnotation.setAttribute(OmniFAT.STATE_ATTRIBUTE, OmniFAT.LIKELY_STATE);
					change = true;
					docData.log(("Promoting epithet '" + epithetAnnotation.toXML() + "' for " + rankGroup + " suffix '" + suffix + "'"), OmniFAT.DEBUG_LEVEL_DETAILS);
//					if (s != 0)
//						System.out.println("Promoting epithet '" + epithetAnnotation.toXML() + "' for " + rankGroup + " suffix '" + suffix + "'");
					s = epithet.length();
				}
			}
		}
		
		//	did we promote some epithet?
		return change;
	}
	
	private static final boolean promoteCandidatesForCombination(MutableAnnotation data, OmniFAT.DocumentDataSet docData, int ruleSet, int round) {
		
		//	prepare hash join
		HashMap candidateCombinationBuckets = new HashMap();
		HashSet matchedCandidateIDs = new HashSet();
		
		//	collect epithet combinations of positives, and bucketize them
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			String source = ((String) taxonNames[t].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
			if (source.indexOf(',') == -1)
				continue;
			
			OmniFAT.Combination comb = docData.getCombination(taxonNames[t]);
			for (Iterator esit = comb.epithetStarts.iterator(); esit.hasNext();) {
				String epithetStart = ((String) esit.next());
				docData.getSureCombinationBucket(epithetStart).add(comb);
			}
		}
		
		//	collect epithet combinations of positives, and bucketize them
		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNameCandidates.length; c++) {
			String source = ((String) taxonNameCandidates[c].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
			if (source.indexOf(',') == -1)
				continue;
			
			OmniFAT.Combination candComb = docData.getCombination(taxonNameCandidates[c]);
			for (Iterator esit = candComb.epithetStarts.iterator(); esit.hasNext();) {
				String epithetStart = ((String) esit.next());
				ArrayList candCombBucket = ((ArrayList) candidateCombinationBuckets.get(epithetStart));
				if (candCombBucket == null) {
					candCombBucket = new ArrayList();
					candidateCombinationBuckets.put(epithetStart, candCombBucket);
				}
				candCombBucket.add(candComb);
			}
		}
		
		//	do join
		for (Iterator bit = candidateCombinationBuckets.keySet().iterator(); bit.hasNext();) {
			String epithetStart = ((String)bit.next());
			List combBucket = docData.getSureCombinationBucket(epithetStart);
			ArrayList candCombBucket = ((ArrayList) candidateCombinationBuckets.get(epithetStart));
			for (Iterator ccit = candCombBucket.iterator(); ccit.hasNext();) {
				OmniFAT.Combination candComb = ((OmniFAT.Combination) ccit.next());
				if (matchedCandidateIDs.contains(candComb.annotationId))
					continue;
				for (Iterator cit = combBucket.iterator(); (cit != null) && cit.hasNext();) {
					OmniFAT.Combination comb = ((OmniFAT.Combination) cit.next());
					if (comb.covers(candComb, true)) {
						docData.log(("Promoting candidate '" + candComb.toString() + "' as covered by '" + comb.toString() + "'"), OmniFAT.DEBUG_LEVEL_IMPORTANT);
						matchedCandidateIDs.add(candComb.annotationId);
						cit = null;
					}
				}
			}
		}
		
		//	remove candidates nested in to-promote annotations
		taxonNameCandidates = removeOverlappingCandidates(data, taxonNameCandidates, matchedCandidateIDs, docData);
		
		//	promote candidates
		return promoteCandidates(taxonNameCandidates, matchedCandidateIDs, (OmniFAT.DOCUMENT_EVIDENCE_PREFIX + ruleSet + "-" + round), "combination", "for epithet combination", docData);
	}
	
	private static final String getKey(Annotation annotation) {
		return (annotation.getType() + "$" + annotation.getStartIndex() + "$" + annotation.size());
	}
	public static abstract class JoinTool {
		public String getDeDuplicationKey(Annotation annotation) {
			return (annotation.getType() + "$" + annotation.getStartIndex() + "$" + annotation.size());
		}
		public abstract Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint);
	}
	private static final JoinTool defaultJoinTool = new JoinTool() {
		public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
			return joint;
		}
	};
	
	public static Annotation[] joinAdjacent(QueriableAnnotation data, String sourceType, String targetType, JoinTool joinTool) {
		return joinAdjacent(data, data.getAnnotations(sourceType), data.getAnnotations(sourceType), null, targetType, joinTool);
	}
	
	public static Annotation[] joinAdjacent(QueriableAnnotation data, String sourceType, Dictionary bridgeable, String targetType, JoinTool joinTool) {
		return joinAdjacent(data, data.getAnnotations(sourceType), data.getAnnotations(sourceType), bridgeable, targetType, joinTool);
	}
	
	public static Annotation[] joinAdjacent(QueriableAnnotation data, String sourceType1, String sourceType2, String targetType, JoinTool joinTool) {
		return joinAdjacent(data, data.getAnnotations(sourceType1), data.getAnnotations(sourceType2), null, targetType, joinTool);
	}
	
	public static Annotation[] joinAdjacent(QueriableAnnotation data, String sourceType1, String sourceType2, Dictionary bridgeable, String targetType, JoinTool joinTool) {
		return joinAdjacent(data, data.getAnnotations(sourceType1), data.getAnnotations(sourceType2), bridgeable, targetType, joinTool);
	}
	
	public static Annotation[] joinAdjacent(QueriableAnnotation data, Annotation[] source1, Annotation[] source2, Dictionary bridgeable, String targetType, JoinTool joinTool) {
		if (joinTool == null)
			joinTool = defaultJoinTool;
		
		Set jointKeys = new HashSet();
		Annotation[] existingJoints = data.getAnnotations(targetType);
		for (int e = 0; e < existingJoints.length; e++)
			jointKeys.add(joinTool.getDeDuplicationKey(existingJoints[e]));
		
		Annotation[] bridges = ((bridgeable == null) ? new Annotation[0] : Gamta.extractAllContained(data, bridgeable));
		Arrays.sort(bridges, joinBridgeOrder);
		int bi = 0;
		
		Arrays.sort(source1, joinEndIndexOrder);
		Arrays.sort(source2, joinStartIndexOrder);
		int s2 = 0;
		ArrayList jointList = new ArrayList();
		for (int s1 = 0; s1 < source1.length; s1++) {
			while ((s2 < source2.length) && (source2[s2].getStartIndex() < source1[s1].getEndIndex()))
				s2++;
			
			while ((bi < bridges.length) && (bridges[bi].getStartIndex() < source1[s1].getEndIndex()))
				bi++;
			
			if ((s2 < source2.length) && (source1[s1].getEndIndex() == source2[s2].getStartIndex()))
				for (int s2l = 0; ((s2 + s2l) < source2.length) && (source1[s1].getEndIndex() == source2[s2 + s2l].getStartIndex()); s2l++) {
					Annotation joint = Gamta.newAnnotation(data, targetType, source1[s1].getStartIndex(), (source2[s2 + s2l].getEndIndex() - source1[s1].getStartIndex()));
					
					joint = joinTool.getJointAnnotation(source1[s1], source2[s2 + s2l], joint);
					if ((joint != null) && jointKeys.add(joinTool.getDeDuplicationKey(joint)))
						jointList.add(joint);
				}
			
			if (bi < bridges.length) {
				int s2l = 0;
				for (int bil = 0; ((bi + bil) < bridges.length) && (source1[s1].getEndIndex() == bridges[bi + bil].getStartIndex()); bil++) {
					while (((s2 + s2l) < source2.length) && (source2[s2 + s2l].getStartIndex() < bridges[bi + bil].getEndIndex()))
						s2l++;
					
//					if (((s2 + s2l) < source2.length) && ((bi + bil) < bridges.length) && (bridges[bi + bil].getEndIndex() == source2[s2 + s2l].getStartIndex()))
//						for (int s2l2 = 0; ((s2 + s2l + s2l2) < source2.length) && (bridges[bi + bil].getEndIndex() == source2[s2 + s2l + s2l2].getStartIndex()); s2l2++) {
//							Annotation joint = Gamta.newAnnotation(data, targetType, source1[s1].getStartIndex(), (source2[s2 + s2l + s2l2].getEndIndex() - source1[s1].getStartIndex()));
//							
//							joint = joinTool.getJointAnnotation(source1[s1], source2[s2 + s2l + s2l2], joint);
//							if ((joint != null) && jointKeys.add(joinTool.getDeDuplicationKey(joint)))
//								jointList.add(joint);
//						}
					
					while (((s2 + s2l) < source2.length) && ((bi + bil) < bridges.length) && (bridges[bi + bil].getEndIndex() == source2[s2 + s2l].getStartIndex())) {
						Annotation joint = Gamta.newAnnotation(data, targetType, source1[s1].getStartIndex(), (source2[s2 + s2l].getEndIndex() - source1[s1].getStartIndex()));
						
						joint = joinTool.getJointAnnotation(source1[s1], source2[s2 + s2l], joint);
						if ((joint != null) && jointKeys.add(joinTool.getDeDuplicationKey(joint)))
							jointList.add(joint);
						
						s2l++;
					}
					
//					if (((s2 + s2l) < source2.length) && ((bi + bil) < bridges.length) && (bridges[bi + bil].getEndIndex() == source2[s2 + s2l].getStartIndex())) {
//						Annotation joint = Gamta.newAnnotation(data, targetType, source1[s1].getStartIndex(), (source2[s2 + s2l].getEndIndex() - source1[s1].getStartIndex()));
//						
//						joint = joinTool.getJointAnnotation(source1[s1], source2[s2 + s2l], joint);
//						if ((joint != null) && jointKeys.add(joinTool.getDeDuplicationKey(joint)))
//							jointList.add(joint);
//					}
				}
			}
		}
		
		return ((Annotation[]) jointList.toArray(new Annotation[jointList.size()]));
	}
	
	// ascending by start index, descending by size
	private static Comparator joinStartIndexOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			Annotation a1 = ((Annotation) o1);
			Annotation a2 = ((Annotation) o2);
			int c = a1.getStartIndex() - a2.getStartIndex();
			return ((c == 0) ? (a2.size() - a1.size()) : c);
		}
	};
	
	// ascending by end index, descending by size
	private static Comparator joinEndIndexOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			Annotation a1 = ((Annotation) o1);
			Annotation a2 = ((Annotation) o2);
			int c = a1.getEndIndex() - a2.getEndIndex();
			return ((c == 0) ? (a2.size() - a1.size()) : c);
		}
	};
	
	// ascending by start index, ascending by size
	private static Comparator joinBridgeOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			Annotation a1 = ((Annotation) o1);
			Annotation a2 = ((Annotation) o2);
			int c = a1.getStartIndex() - a2.getStartIndex();
			return ((c == 0) ? (a1.size() - a2.size()) : c);
		}
	};
	
	public static void completeAbbreviations(MutableAnnotation data, OmniFAT omniFat) {
		completeAbbreviations(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void completeAbbreviations(MutableAnnotation data, final OmniFAT.DocumentDataSet docData) {
		
		//	try to fill in abbreviations based on known positives
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			Annotation[] epithets = docData.getEpithets(taxonNames[t]);
			StringVector fullEpithets = new StringVector();
			ArrayList abbreviatedEpithets = new ArrayList();
			for (int e = 0; e < epithets.length; e++) {
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				if (epithet.length() > (epithet.endsWith(".") ? 3 : 2)) {
					fullEpithets.addElementIgnoreDuplicates(epithet);
					String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
					docData.addEpithetOccurrence(rankGroup, epithet);
				}
				else abbreviatedEpithets.add(epithets[e]);
			}
			
			//	nothing to resolve, no need for gathering further data
			if (abbreviatedEpithets.isEmpty()) {
//				System.out.println("Nothing to complete in " + taxonNames[t].toXML());
				continue;
			}
			
//			System.out.println("Completing " + taxonNames[t].toXML());
			
			//	get epithets appearing in a sure positive together with the ones from the current candidate
			StringVector cooccurringEpithets = new StringVector();
			for (int e = 0; e < fullEpithets.size(); e++) {
				Dictionary cooccurringEpithetDictionary = docData.getSureCooccurringEpithets(fullEpithets.get(e));
				if (cooccurringEpithetDictionary != null)
					for (StringIterator ceit = cooccurringEpithetDictionary.getEntryIterator(); ceit.hasMoreStrings();)
						cooccurringEpithets.addElementIgnoreDuplicates(ceit.nextString());
			}
//			System.out.println("Cooccurring epithets are:\n  " + cooccurringEpithets.concatStrings("\n  "));
			
			//	try possible full forms
			for (int e = 0; e < abbreviatedEpithets.size(); e++) {
				String abbreviation = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				final String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
				Dictionary abbreviationResolver = docData.getSureAbbreviationResolver(rankGroup, abbreviation);
				if (abbreviationResolver != null) {
					StringVector likelyFullForms = new StringVector();
					StringVector positiveFullForms = new StringVector();
//					System.out.println("Possible full forms of '" + abbreviation + "' are:");
					for (StringIterator arit = abbreviationResolver.getEntryIterator(); arit.hasMoreStrings();) {
						String fullForm = arit.nextString();
						if (cooccurringEpithets.contains(fullForm)) {
							likelyFullForms.addElementIgnoreDuplicates(fullForm);
//							System.out.println("  + " + fullForm);
						}
						else if (docData.isSureEpithet(rankGroup, fullForm)) {
							positiveFullForms.addElementIgnoreDuplicates(fullForm);
//							System.out.println("  x " + fullForm);
						}
//						else System.out.println("  - " + fullForm);
					}
					
					//	use all available positive full forms if no cooccurring ones are available
					if (likelyFullForms.isEmpty())
						likelyFullForms.addContent(positiveFullForms);
					
					//	found un-ambiguous full fom
					if (likelyFullForms.size() == 1) {
						docData.log(("Resolved '" + abbreviation + "' to '" + likelyFullForms.get(0) + "' in " + taxonNames[t].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("Resolved '" + abbreviation + "' to '" + likelyFullForms.get(0) + "' in " + taxonNames[t].toXML());
						epithets[e].setAttribute(OmniFAT.VALUE_ATTRIBUTE, likelyFullForms.get(0));
					}
					
					//	no clear full form available (yet)
					else if (likelyFullForms.size() > 1) {
						docData.log(("Got multiple possible full forms of '" + abbreviation + "' in " + taxonNames[t].toXML() + "\n  " + likelyFullForms.concatStrings("\n  ")), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("Got multiple possible full forms of '" + abbreviation + "' in " + taxonNames[t].toXML() + "\n  " + likelyFullForms.concatStrings("\n  "));
						String[] fullForms = likelyFullForms.toStringArray();
						Arrays.sort(fullForms, new Comparator() {
							public int compare(Object o1, Object o2) {
								String e1 = ((String) o1);
								String e2 = ((String) o2);
								return docData.compareByLastOccurrence(rankGroup, e1, e2);
							}
						});
						likelyFullForms.clear();
						likelyFullForms.addContent(fullForms);
//						System.out.println("Ordered by last occurrence:\n  " + likelyFullForms.concatStrings("\n  "));
						docData.log(("Resolved '" + abbreviation + "' to '" + likelyFullForms.get(0) + "' in " + taxonNames[t].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("==> Resolved '" + abbreviation + "' to '" + likelyFullForms.get(0) + "' in " + taxonNames[t].toXML());
						epithets[e].setAttribute(OmniFAT.VALUE_ATTRIBUTE, likelyFullForms.get(0));
					}
				}
			}
		}
	}
	
	
	public static void inferRanks(MutableAnnotation data, OmniFAT omniFat) {
		inferRanks(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void inferRanks(MutableAnnotation data, OmniFAT.DocumentDataSet docData) {
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		
		//	extract existing rank assignments
		for (int t = 0; t < taxonNames.length; t++) {
			Annotation[] epithets = docData.getEpithets(taxonNames[t]);
			for (int e = 0; e < epithets.length; e++)
				if (epithets[e].hasAttribute(OmniFAT.RANK_ATTRIBUTE)) {
					String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
					String rank = ((String) epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE));
					docData.addEpithetRank(epithet, rank);
				}
		}
		
		boolean newRanks;
		do {
			newRanks = false;
			for (int t = 0; t < taxonNames.length; t++) {
				if (taxonNames[t] == null)
					continue;
				
				if (inferEpithetRanks(taxonNames[t], docData)) {
					Annotation[] epithets = docData.getEpithets(taxonNames[t]);
					for (int e = 0; e < epithets.length; e++) {
						String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
						String rank = ((String) epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE));
						docData.addEpithetRank(epithet, rank);
					}
					setRankAttributes(taxonNames[t], docData);
					newRanks = true;
					taxonNames[t] = null;
				}
			}
		} while (newRanks);
	}
	
	public static boolean inferEpithetRanks(QueriableAnnotation taxonName, OmniFAT omniFat) {
		return inferEpithetRanks(taxonName, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static boolean inferEpithetRanks(QueriableAnnotation taxonName, OmniFAT.DocumentDataSet docData) {
		Annotation[] epithets = docData.getEpithets(taxonName);
		ArrayList epithetList = new ArrayList();
		for (int e = 0; e < epithets.length; e++)
			epithetList.add(epithets[e]);
		epithets = ((Annotation[]) epithetList.toArray(new Annotation[epithetList.size()]));
		return inferEpithetRanks(epithets, docData);
	}
	
	public static boolean inferEpithetRanks(Annotation[] epithets, OmniFAT.DocumentDataSet docData) {
		
		//	extract epithets and group them in rank groups
		OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
		Annotation[][] rankGroupEpithets = new Annotation[rankGroups.length][];
		for (int rg = 0; rg < rankGroups.length; rg++) {
			ArrayList rankGroupEpithetList = new ArrayList();
			for (int e = 0; e < epithets.length; e++) {
				if (rankGroups[rg].getOrderNumber() == Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_NUMBER_ATTRIBUTE)))
					rankGroupEpithetList.add(epithets[e]);
			}
			rankGroupEpithets[rg] = ((Annotation[]) rankGroupEpithetList.toArray(new Annotation[rankGroupEpithetList.size()]));
		}
		
		//	gather additional data
		boolean[] isHighGroup = new boolean[rankGroups.length];
		boolean highGroup = true;
		boolean[] isLowGroup = new boolean[rankGroups.length];
		boolean lowGroup = true;
		for (int rg = 0; rg < rankGroups.length; rg++) {
			isHighGroup[rg] = highGroup;
			highGroup = (highGroup && (rankGroupEpithets[rg].length == 0));
			isLowGroup[rankGroups.length - 1 - rg] = lowGroup;
			lowGroup = (lowGroup && (rankGroupEpithets[rankGroups.length - 1 - rg].length == 0));
		}
		
		//	assign ranks per rank group
		for (int rg = 0; rg < rankGroups.length; rg++)
			if (rankGroupEpithets[rg].length != 0) {
				Integer[] groupEpithetRanks = getGroupEpithetRanks(rankGroupEpithets[rg], rankGroups[rg], isHighGroup[rg], isLowGroup[rg], docData, true);
				if (groupEpithetRanks == null) 
					return false;
				else for (int e = 0; e < rankGroupEpithets[rg].length; e++) {
					OmniFAT.Rank rank = docData.omniFat.getRank(groupEpithetRanks[e].intValue());
					rankGroupEpithets[rg][e].setAttribute(OmniFAT.RANK_ATTRIBUTE, rank.getName());
					rankGroupEpithets[rg][e].setAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + rank.getOrderNumber()));
					String epithet = ((String) rankGroupEpithets[rg][e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
					docData.addEpithetRank(epithet, rank.getName());
				}
			}
		return true;
	}
	
	private static Integer[] getGroupEpithetRanks(Annotation[] epithets, OmniFAT.RankGroup rankGroup, boolean highGroup, boolean lowGroup, final OmniFAT.DocumentDataSet docData, boolean forceUnique) {
		
		//	gather base data
		int maxGroupRank = rankGroup.getOrderNumber();
		int minGroupRank = (maxGroupRank - rankGroup.getRanks().length + 1);
		OmniFAT.Rank[] ranks = rankGroup.getRanks();
		
		//	initialize sets and set existing ranks
		final String[] epithetValues = new String[epithets.length];
		final TreeSet[] epithetRankNumbers = new TreeSet[epithets.length];
		for (int e = 0; e < epithets.length; e++) {
			epithetValues[e] = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
			epithetRankNumbers[e] = new TreeSet();
			if (epithets[e].hasAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE))
				epithetRankNumbers[e].add(new Integer((String) epithets[e].getAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE)));
		}
		
		//	fill in possible ranks for so far un-ranked epithets
		for (int e = 0; e < epithets.length; e++)
			if (epithetRankNumbers[e].isEmpty()) {
				int maxRank = (maxGroupRank - e);
				int minRank = (minGroupRank + (epithets.length - 1) - e);
				for (int r = maxRank; r >= minRank; r--)
					epithetRankNumbers[e].add(new Integer(r));
			}
		
		//	reduce rank numbers per epithet based on rank order consistency
		boolean ranksReduced;
		do {
			ranksReduced = false;
			for (int e = 0; e < epithets.length; e++)
				if (epithetRankNumbers[e].size() > 1) {
					int maxRank;
					if (e == 0)
						maxRank = maxGroupRank;
					else if (epithetRankNumbers[e-1].isEmpty())
						return null;
					else maxRank = (((Integer) epithetRankNumbers[e-1].last()).intValue() - 1);
					
					int minRank;
					if ((e+1) == epithets.length)
						minRank = minGroupRank;
					else if (epithetRankNumbers[e+1].isEmpty())
						return null;
					else minRank = (((Integer) epithetRankNumbers[e+1].first()).intValue() + 1);
					
					for (Iterator rnit = epithetRankNumbers[e].iterator(); rnit.hasNext();) {
						int rank = ((Integer) rnit.next()).intValue();
						if ((rank > maxRank) || (rank < minRank)) {
							rnit.remove();
							ranksReduced = true;
						}
					}
				}
		} while (ranksReduced);
		
		//	combination is un-ambuguous, assign ranks
		if (sizeProduct(epithetRankNumbers) == 1) {
			Integer[] combination = new Integer[epithets.length];
			for (int e = 0; e < epithets.length; e++)
				combination[e] = ((Integer) epithetRankNumbers[e].first());
			return combination;
		}
		
		//	ranks are still ambiguous, generate and score combinations
		Integer[] combinationStub = new Integer[epithets.length];
		ArrayList combinations = new ArrayList();
		produceCombinations(epithetRankNumbers, 0, combinationStub, combinations);
		
		//	sort out broken combinations
		Set requiredRankNumbers = new HashSet();
		for (int r = 0; r < ranks.length; r++)
			if (ranks[r].isEpithetRequired())
				requiredRankNumbers.add(new Integer(ranks[r].getOrderNumber()));
		HashSet combinationRankNumbers = new HashSet(epithets.length);
		for (Iterator cit = combinations.iterator(); cit.hasNext();) {
			combinationRankNumbers.clear();
			combinationRankNumbers.addAll(Arrays.asList((Integer[]) cit.next()));
			if (!highGroup) combinationRankNumbers.add(new Integer(maxGroupRank+1));
			if (!lowGroup) combinationRankNumbers.add(new Integer(minGroupRank-1));
			if (isNumberMissing(combinationRankNumbers, requiredRankNumbers))
				cit.remove();
		}
		
		//	no combination found
		if (combinations.isEmpty()) {
//			System.out.println("No possible rank combinations found for epithet set:");
//			for (int e = 0; e < epithets.length; e++)
//				System.out.println("  " + epithets[e].toXML());
			return null;
		}
		
		//	combination is un-ambuguous now, assign ranks
		if (combinations.size() == 1)
			return ((Integer[]) combinations.get(0));
		
		//	score rank combinations by ranks occurring elsewhere in document
		final HashMap combinationScores = new HashMap();
		for (Iterator cit = combinations.iterator(); cit.hasNext();) {
			Integer[] combination = ((Integer[]) cit.next());
			int score = 0;
			for (int e = 0; e < combination.length; e++) {
				if (epithetRankNumbers[e].size() > 1) {
					OmniFAT.Rank rank = docData.omniFat.getRank(combination[e].intValue());
					if (docData.getEpithetRanks(epithetValues[e]).contains(rank.getName()))
						score++;
				}
			}
			combinationScores.put(combination, new Integer(score));
		}
		Collections.sort(combinations, new Comparator() {
			public int compare(Object o1, Object o2) {
				Integer s1 = ((Integer) combinationScores.get(o1));
				Integer s2 = ((Integer) combinationScores.get(o2));
				return s2.compareTo(s1); // descending order !!!
			}
		});
		Integer topScore = ((Integer) combinationScores.get(combinations.get(0)));
		
		//	we still do not have an un-ambiguous top combination, augment score with probability of ranks the combinations cover 
		if (topScore.equals(combinationScores.get(combinations.get(1)))) {
			for (Iterator cit = combinations.iterator(); cit.hasNext();) {
				Integer[] combination = ((Integer[]) cit.next());
				int score = ((Integer) combinationScores.get(combination)).intValue();
				for (int e = 0; e < combination.length; e++) {
					if (epithetRankNumbers[e].size() > 1) {
						score += docData.omniFat.getRank(combination[e].intValue()).getProbability();
//						if (requiredRankNumbers.contains(combination[e]))
//							score++;
					}
				}
				combinationScores.put(combination, new Integer(score));
			}
			Collections.sort(combinations, new Comparator() {
				public int compare(Object o1, Object o2) {
					Integer s1 = ((Integer) combinationScores.get(o1));
					Integer s2 = ((Integer) combinationScores.get(o2));
					return s2.compareTo(s1); // descending order !!!
				}
			});
			topScore = ((Integer) combinationScores.get(combinations.get(0)));
		}
		
		//	we still do not have an un-ambiguous top combination, give up for now
		if (forceUnique && topScore.equals(combinationScores.get(combinations.get(1))))
			return null;
		
		//	finally, we've made it, assign ranks
		return ((Integer[]) combinations.get(0));
	}
	
	private static final int sizeProduct(Collection[] collections) {
		int sizeProduct = 1;
		for (int c = 0; c < collections.length; c++)
			sizeProduct *= collections[c].size();
		return sizeProduct;
	}
	
	private static final void produceCombinations(TreeSet[] combinationBase, int baseIndex, Integer[] combinationStub, ArrayList combinations) {
		if (baseIndex == combinationBase.length) {
			Integer[] combination = new Integer[combinationStub.length];
			System.arraycopy(combinationStub, 0, combination, 0, combination.length);
			combinations.add(combination);
		}
		else for (Iterator bit = combinationBase[baseIndex].iterator(); bit.hasNext();) {
			combinationStub[baseIndex] = ((Integer) bit.next());
			if ((baseIndex == 0) || (combinationStub[baseIndex].intValue() < combinationStub[baseIndex-1].intValue()))
				produceCombinations(combinationBase, (baseIndex+1), combinationStub, combinations);
		}
	}
	
	
	public static void setRankAttributes(QueriableAnnotation taxonName, OmniFAT.DocumentDataSet docData) {
		Annotation[] epithets = docData.getEpithets(taxonName);
		for (int e = 0; e < epithets.length; e++) {
			String rank = ((String) epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE));
			String value = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
			if ((rank != null) && (value != null))
				taxonName.setAttribute(rank, value);
		}
	}
	
	
	public static void completeNames(MutableAnnotation data, OmniFAT omniFat) {
		completeNames(data, new OmniFAT.DocumentDataSet(omniFat));
	}
	
	public static void completeNames(MutableAnnotation data, final OmniFAT.DocumentDataSet docData) {
		OmniFAT.Rank[] ranks = docData.omniFat.getRanks();
		HashMap requiredRanks = new HashMap();
		int maxRequiredRank = 0;
		for (int r = 0; r < ranks.length; r++)
			if (ranks[r].isEpithetRequired()) {
				requiredRanks.put(ranks[r].getName(), ranks[r]);
				maxRequiredRank = Math.max(maxRequiredRank, ranks[r].getOrderNumber());
			}
		
		//	try to fill in missing epithets based on known positives
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			Annotation[] epithets = docData.getEpithets(taxonNames[t]);
			StringVector givenEpithets = new StringVector(); 
			HashMap missingRanks = new HashMap();
			missingRanks.putAll(requiredRanks);
			int minRank = Integer.MAX_VALUE;
			for (int e = 0; e < epithets.length; e++) {
				missingRanks.remove(epithets[e].getAttribute(OmniFAT.RANK_ATTRIBUTE));
				minRank = Math.min(minRank, Integer.parseInt((String) epithets[e].getAttribute(OmniFAT.RANK_NUMBER_ATTRIBUTE, ("" + minRank))));
				String epithet = ((String) epithets[e].getAttribute(OmniFAT.VALUE_ATTRIBUTE));
				givenEpithets.addElementIgnoreDuplicates(epithet);
				if (epithet.length() > (epithet.endsWith(".") ? 3 : 2)) {
					String rankGroup = ((String) epithets[e].getAttribute(OmniFAT.RANK_GROUP_ATTRIBUTE));
					docData.addEpithetOccurrence(rankGroup, epithet);
				}
			}
			
			//	remove missing ranks below name rank
			for (Iterator rit = missingRanks.values().iterator(); rit.hasNext();) {
				OmniFAT.Rank rank = ((OmniFAT.Rank) rit.next());
				if (rank.getOrderNumber() < minRank)
					rit.remove();
			}
			
			//	nothing to complete
			if (missingRanks.isEmpty() || (minRank >= maxRequiredRank)) {
//				System.out.println("No gaps to fill in " + taxonNames[t].toXML());
				continue;
			}
			
//			System.out.println("Filling gaps in " + taxonNames[t].toXML());
			
			//	get epithets appearing in a sure positive together with the ones from the current candidate
			StringVector cooccurringEpithets = new StringVector();
			for (int e = 0; e < givenEpithets.size(); e++) {
				Dictionary cooccurringEpithetDictionary = docData.getSureCooccurringEpithets(givenEpithets.get(e));
				if (cooccurringEpithetDictionary != null)
					for (StringIterator ceit = cooccurringEpithetDictionary.getEntryIterator(); ceit.hasMoreStrings();)
						cooccurringEpithets.addElementIgnoreDuplicates(ceit.nextString());
			}
//			System.out.println("Cooccurring epithets are:\n  " + cooccurringEpithets.concatStrings("\n  "));
			
			//	try to fill gaps
			for (Iterator rit = missingRanks.values().iterator(); rit.hasNext();) {
				OmniFAT.Rank rank = ((OmniFAT.Rank) rit.next());
				final String rankGroup = rank.getRankGroup().getName();
				Dictionary existingEpithets = docData.getSureEpithetList(rankGroup);
				if (existingEpithets != null) {
					StringVector likelyEpithets = new StringVector();
					StringVector positiveEpithets = new StringVector();
//					System.out.println("Possible epithets are:");
					for (StringIterator arit = existingEpithets.getEntryIterator(); arit.hasMoreStrings();) {
						String existingEpithet = arit.nextString();
						if (givenEpithets.contains(existingEpithet)) {
							positiveEpithets.addElementIgnoreDuplicates(existingEpithet);
//							System.out.println("  x " + existingEpithet);
						}
						else if (cooccurringEpithets.contains(existingEpithet)) {
							likelyEpithets.addElementIgnoreDuplicates(existingEpithet);
//							System.out.println("  + " + existingEpithet);
						}
						else {
							positiveEpithets.addElementIgnoreDuplicates(existingEpithet);
//							System.out.println("  x " + existingEpithet);
						}
					}
					
					//	use all available positive full forms if no cooccurring ones are available
					if (likelyEpithets.isEmpty())
						likelyEpithets.addContent(positiveEpithets);
					
					//	found un-ambiguous full fom
					if (likelyEpithets.size() == 1) {
						docData.log(("Resolved " + rank.getName() + " to '" + likelyEpithets.get(0) + "' in " + taxonNames[t].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("Resolved " + rankGroup + " to '" + likelyEpithets.get(0) + "' in " + taxonNames[t].toXML());
						taxonNames[t].setAttribute(rank.getName(), likelyEpithets.get(0));
					}
					
					//	no clear full form available (yet)
					else if (likelyEpithets.size() > 1) {
						docData.log(("Got multiple possible epithets for " + rank.getName() + " in " + taxonNames[t].toXML() + "\n  " + likelyEpithets.concatStrings("\n  ")), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("Got multiple possible epithets for " + rankGroup + " in " + taxonNames[t].toXML() + "\n  " + likelyEpithets.concatStrings("\n  "));
						String[] fullForms = likelyEpithets.toStringArray();
						Arrays.sort(fullForms, new Comparator() {
							public int compare(Object o1, Object o2) {
								String e1 = ((String) o1);
								String e2 = ((String) o2);
								return docData.compareByLastOccurrence(rankGroup, e1, e2);
							}
						});
						likelyEpithets.clear();
						likelyEpithets.addContent(fullForms);
//						System.out.println("Ordered by last occurrence:\n  " + likelyEpithets.concatStrings("\n  "));
						docData.log(("Resolved " + rank.getName() + " to '" + likelyEpithets.get(0) + "' in " + taxonNames[t].toXML()), OmniFAT.DEBUG_LEVEL_DETAILS);
//						System.out.println("==> Resolved " + rankGroup + " to '" + likelyEpithets.get(0) + "' in " + taxonNames[t].toXML());
						taxonNames[t].setAttribute(rank.getName(), likelyEpithets.get(0));
					}
				}
			}
		}
	}
	
	
	public static String getNameString(Annotation taxonName, OmniFAT omniFat) {
		OmniFAT.Rank[] ranks = omniFat.getRanks();
		StringBuffer nameString = new StringBuffer();
		for (int r = 0; r < ranks.length; r++) {
			String epithet = ((String) taxonName.getAttribute(ranks[r].getName()));
			if (epithet != null)
				nameString.append(" " + ranks[r].formatEpithet(epithet));
		}
		return nameString.toString().trim();
	}
	
	public static boolean fuzzyLookup(String string, Dictionary dictionary, int threshold, boolean caseSensitive) {
		if (dictionary.lookup(string, caseSensitive))
			return true;
		
		if (dictionary instanceof OmniFatDictionary)
			return ((OmniFatDictionary) dictionary).fuzzyLookup(string, threshold, caseSensitive);
		else return (findFuzzyMatch(string, dictionary, threshold, caseSensitive) != null);
	}
	
	private static String findFuzzyMatch(String string, Dictionary dictionary, int threshold, boolean caseSensitive) {
		if (dictionary.lookup(string, caseSensitive))
			return string;
		
		byte[] unknownWordCrossSumData = new byte[27];
		Arrays.fill(unknownWordCrossSumData, 0, unknownWordCrossSumData.length, ((byte) 0));
		for (int uwc = 0; uwc < string.length(); uwc++) {
			char ch = string.charAt(uwc);
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
		for (StringIterator dit = dictionary.getEntryIterator(); dit.hasMoreStrings();) {
			String dictionaryEntry = dit.nextString();
//			wordsTested++;
			
			if (Math.abs(dictionaryEntry.length() - string.length()) > threshold)
				continue;
			
			Arrays.fill(correctionCrossSumData, 0, correctionCrossSumData.length, ((byte) 0));
			crossSumDistance = 0;
			for (int cc = 0; cc < dictionaryEntry.length(); cc++) {
				char ch = dictionaryEntry.charAt(cc);
				if (('a' <= ch) && (ch <= 'z')) correctionCrossSumData[ch - 'a']++;
				else if (('A' <= ch) && (ch <= 'Z')) correctionCrossSumData[ch - 'A']++;
				else correctionCrossSumData[26]++;
			}
			for (int csc = 0; csc < unknownWordCrossSumData.length; csc++)
				crossSumDistance += Math.abs(unknownWordCrossSumData[csc] - correctionCrossSumData[csc]);
//			crossSumChecks++;
			
			if (crossSumDistance > (2 * threshold))
				continue;
			
			levenshteinDistance = StringUtils.getLevenshteinDistance(string, dictionaryEntry, threshold, caseSensitive);
//			levenshteinChecks++;
			
			if (levenshteinDistance <= threshold) {// compute similarity to word in question
//				correctionsFound++;
				return dictionaryEntry;
			}
		}
		return null;
	}
}