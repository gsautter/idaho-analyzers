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

import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * @author sautter
 *
 */
public class BaseEpithetTagger extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.tagBaseEpithets(data, this.getDataSet(data, parameters));
		
//		OmniFAT omniFat = this.getOmniFatInstance(parameters);
//		
//		//	collect text tokens
//		Set textTokens = new HashSet();
//		Set inSentenceTextTokens = new HashSet();
//		boolean inSentence = true;
//		for (int t = 0; t < data.size(); t++) {
//			String token = data.valueAt(t);
//			if (Gamta.isWord(token) || Gamta.isNumber(token)) {
//				textTokens.add(token);
//				if (inSentence)
//					inSentenceTextTokens.add(token);
//			}
//			inSentence = !Gamta.isSentenceEnd(token);
//		}
//		
//		//	get rank groups
//		OmniFAT.RankGroup[] rankGroups = omniFat.getRankGroups();
//		
//		//	prepare filters
//		Dictionary stopWords = omniFat.getStopWords();
//		
//		//	annotate epithets for rank group lexicons
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			
//			//	de-duplicate in rank group
//			Set rankGroupMatchKeys = new HashSet();
//			
//			//	collect possible abbreviations
//			Properties rankGroupEpithetStates = new Properties();
//			
//			//	do individual ranks
//			OmniFAT.Rank[] ranks = rankGroups[rg].getRanks();
//			for (int r = 0; r < ranks.length; r++) {
//				
//				//	de-duplicate in rank
//				Set rankMatchKeys = new HashSet();
//				
//				//	collect possible abbreviations
//				Properties rankEpithetStates = new Properties();
//				
//				//	get negative patterns & lexicons
//				OmniFatPattern[] negativePatterns = ranks[r].getNegativePatterns();
//				OmniFatDictionary[] negativeDictionaries = ranks[r].getNegativeDictionaries();
//				
//				//	apply precision patterns
//				OmniFatPattern[] precisionPatterns = ranks[r].getPrecisionPatterns();
//				for (int p = 0; p < precisionPatterns.length; p++) {
//					
//					//	extract and annotate epithets
//					Annotation[] epithets = Gamta.extractAllMatches(data, precisionPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
//					for (int e = 0; e < epithets.length; e++) {
//						String epithetKey = this.getKey(epithets[e]);
//						String epithetValue = epithets[e].getValue();
//						int epithetValueOffset = 0;
//						if (epithets[e].size() != 1)
//							for (int v = 0; v < epithets[e].size(); v++) {
//								String value = epithets[e].valueAt(v);
//								if (Gamta.isWord(value)) {
//									epithetValue = value;
//									epithetValueOffset = v;
//								}
//							}
//						
//						//	remember match & ignore duplicates
//						rankGroupMatchKeys.add(epithetKey);
//						if (!rankMatchKeys.add(epithetKey))
//							continue;
//						
//						//	ignore abbreviations (they come later)
//						if (epithetValue.length() < 3)
//							continue;
//						
//						//	ignore stop words
//						if (stopWords.lookup(epithetValue, false)) {
//							if (DEBUG) System.out.println("Ignoring stop word match: " + epithetValue);
//							continue;
//						}
//						
//						//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//						if (omniFat.doStemmingNegativeLookup(epithetValue)) {
//							if (DEBUG) System.out.println("Ignoring flected match: " + epithetValue);
//							continue;
//						}
//						
//						Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//						epithet.setAttribute("rank", ranks[r].getName());
//						epithet.setAttribute("rankNumber", ("" + ranks[r].getOrderNumber()));
//						epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//						epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//						epithet.setAttribute("evidence", "precision");
//						epithet.setAttribute("evidenceDetail", precisionPatterns[p].name);
//						epithet.setAttribute("string", epithetValue);
//						epithet.setAttribute("value", epithetValue);
//						epithet.setAttribute("baseIndex", ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
//						
//						epithet.setAttribute("state", "positive");
//						rankEpithetStates.setProperty(epithetValue, "positive");
//						rankGroupEpithetStates.setProperty(epithetValue, "positive");
//					}
//				}
//				
//				//	apply lexicons
//				OmniFatDictionary[] epithetDictionaries = ranks[r].getPositiveDictionaries();
//				for (int d = 0; d < epithetDictionaries.length; d++) {
//					
//					//	extract and annotate epithets
//					Annotation[] epithets = Gamta.extractAllContained(data, epithetDictionaries[d], 1, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
//					for (int e = 0; e < epithets.length; e++) {
//						String epithetKey = this.getKey(epithets[e]);
//						String epithetValue = epithets[e].getValue();
//						
//						//	remember match & ignore duplicates
//						rankGroupMatchKeys.add(epithetKey);
//						if (!rankMatchKeys.add(epithetKey))
//							continue;
//						
//						//	ignore abbreviations (they come later)
//						if (epithetValue.length() < 3)
//							continue;
//						
//						//	ignore stop words
//						if (stopWords.lookup(epithetValue, false)) {
//							if (DEBUG) System.out.println("Ignoring stop word match: " + epithetValue);
//							continue;
//						}
//						
//						//	do negative filters for untrusted lexicons
//						if (!epithetDictionaries[d].trusted) {
//							
//							//	do sure negative lookup
//							if (!epithetDictionaries[d].trusted && rankGroups[rg].isEpithetsCapitalized() && !inSentenceTextTokens.contains(epithetValue) && textTokens.contains(epithetValue.toLowerCase())) {
//								if (DEBUG) System.out.println("Ignoring sure negative match: " + epithetValue);
//								continue;
//							}
//							
//							//	do negative patterns
//							OmniFatPattern negativePattern = null;
//							for (int n = 0; n < negativePatterns.length; n++)
//								if (negativePatterns[n].matches(epithetValue)) {
//									negativePattern = negativePatterns[n];
//									n = negativePatterns.length;
//								}
//							if (negativePattern != null) {
//								if (DEBUG) System.out.println("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue);
//								continue;
//							}
//							
//							//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//							if (omniFat.doStemmingNegativeLookup(epithetValue)) {
//								if (DEBUG) System.out.println("Ignoring flected match: " + epithetValue);
//								continue;
//							}
//						}
//						
//						Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//						epithet.setAttribute("rank", ranks[r].getName());
//						epithet.setAttribute("rankNumber", ("" + ranks[r].getOrderNumber()));
//						epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//						epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//						epithet.setAttribute("evidence", "lexicon");
//						epithet.setAttribute("string", epithetValue);
//						epithet.setAttribute("value", epithetValue);
//						epithet.setAttribute("baseIndex", ("" + epithets[e].getStartIndex()));
//						
//						boolean caseMatch = epithetDictionaries[d].lookup(epithetValue, true);
//						String state;
//						String evidenceDetail;
//						if (epithetDictionaries[d].trusted) {
//							state = (caseMatch ? "positive" : "ambiguous");
//							evidenceDetail = (caseMatch ? epithetDictionaries[d].name : (epithetDictionaries[d].name + "/CI"));
//						}
//						else if (caseMatch) {
//							state = "recall";
//							evidenceDetail = epithetDictionaries[d].name;
//							for (int n = 0; n < negativeDictionaries.length; n++)
//								if (negativeDictionaries[n].lookup(epithetValue, true)) {
//									state = "ambiguous";
//									evidenceDetail = (epithetDictionaries[d].name + "/" + negativeDictionaries[n].name);
//									n = negativeDictionaries.length;
//								}
//						}
//						else {
//							state = "ambiguous";
//							evidenceDetail = (epithetDictionaries[d].name + "/CI");
//						}
//						epithet.setAttribute("state", state);
//						epithet.setAttribute("evidenceDetail", evidenceDetail);
//						if (!rankEpithetStates.containsKey(epithetValue))
//							rankEpithetStates.setProperty(epithetValue, state);
//						if (!rankGroupEpithetStates.containsKey(epithetValue))
//							rankGroupEpithetStates.setProperty(epithetValue, state);
//					}
//				}
//				
//				//	apply recall patterns
//				OmniFatPattern[] recallPatterns = ranks[r].getRecallPatterns();
//				for (int p = 0; p < recallPatterns.length; p++) {
//					
//					//	extract and annotate epithets
//					Annotation[] epithets = Gamta.extractAllMatches(data, recallPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
//					for (int e = 0; e < epithets.length; e++) {
//						String epithetKey = this.getKey(epithets[e]);
//						String epithetValue = epithets[e].getValue();
//						int epithetValueOffset = 0;
//						if (epithets[e].size() != 1)
//							for (int v = 0; v < epithets[e].size(); v++) {
//								String value = epithets[e].valueAt(v);
//								if (Gamta.isWord(value)) {
//									epithetValue = value;
//									epithetValueOffset = v;
//								}
//							}
//						
//						//	remember match & ignore duplicates
//						rankGroupMatchKeys.add(epithetKey);
//						if (!rankMatchKeys.add(epithetKey))
//							continue;
//						
//						//	ignore abbreviations (they come later)
//						if (epithetValue.length() < 3)
//							continue;
//						
//						//	ignore stop words
//						if (stopWords.lookup(epithetValue, false)) {
//							if (DEBUG) System.out.println("Ignoring stop word match: " + epithetValue);
//							continue;
//						}
//						
//						//	do sure negative lookup
//						if (rankGroups[rg].isEpithetsCapitalized() && !inSentenceTextTokens.contains(epithetValue) && textTokens.contains(epithetValue.toLowerCase())) {
//							if (DEBUG) System.out.println("Ignoring sure negative match: " + epithetValue);
//							continue;
//						}
//						
//						//	do negative patterns
//						OmniFatPattern negativePattern = null;
//						for (int n = 0; n < negativePatterns.length; n++)
//							if (negativePatterns[n].matches(epithetValue)) {
//								negativePattern = negativePatterns[n];
//								n = negativePatterns.length;
//							}
//						if (negativePattern != null) {
//							if (DEBUG) System.out.println("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue);
//							continue;
//						}
//						
//						//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//						if (omniFat.doStemmingNegativeLookup(epithetValue)) {
//							if (DEBUG) System.out.println("Ignoring flected match: " + epithetValue);
//							continue;
//						}
//						
//						Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//						epithet.setAttribute("rank", ranks[r].getName());
//						epithet.setAttribute("rankNumber", ("" + ranks[r].getOrderNumber()));
//						epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//						epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//						epithet.setAttribute("evidence", "recall");
//						epithet.setAttribute("string", epithetValue);
//						epithet.setAttribute("value", epithetValue);
//						epithet.setAttribute("baseIndex", ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
//						
//						String state = rankEpithetStates.getProperty(epithetValue);
//						String evidenceDetail = recallPatterns[p].name;
//						if (state == null) {
//							state = "uncertain";
//							for (int n = 0; n < negativeDictionaries.length; n++)
//								if (negativeDictionaries[n].lookup(epithetValue, true)) {
//									state = "negative";
//									evidenceDetail = (recallPatterns[p].name + "/" + negativeDictionaries[n].name);
//									n = negativeDictionaries.length;
//								}
//						}
//						epithet.setAttribute("state", state);
//						epithet.setAttribute("evidenceDetail", evidenceDetail);
//					}
//				}
//				
//				//	DO ABBREVIATIONS ONLY ON RANK GROUP LEVEL - THEY ARE TOO AMBIGUOUS
//			}
//			
//			//	get negative patterns & lexicons
//			OmniFatPattern[] negativePatterns = rankGroups[rg].getNegativePatterns();
//			OmniFatDictionary[] negativeDictionaries = rankGroups[rg].getNegativeDictionaries();
//			
//			//	apply precision patterns
//			OmniFatPattern[] precisionPatterns = rankGroups[rg].getPrecisionPatterns();
//			for (int p = 0; p < precisionPatterns.length; p++) {
//				
//				//	extract and annotate epithets
//				Annotation[] epithets = Gamta.extractAllMatches(data, precisionPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
//				for (int e = 0; e < epithets.length; e++) {
//					String epithetKey = this.getKey(epithets[e]);
//					String epithetValue = epithets[e].getValue();
//					int epithetValueOffset = 0;
//					if (epithets[e].size() != 1)
//						for (int v = 0; v < epithets[e].size(); v++) {
//							String value = epithets[e].valueAt(v);
//							if (Gamta.isWord(value)) {
//								epithetValue = value;
//								epithetValueOffset = v;
//							}
//						}
//					
//					//	remember match & ignore duplicates
//					if (!rankGroupMatchKeys.add(epithetKey))
//						continue;
//					
//					//	ignore abbreviations (they come later)
//					if (epithetValue.length() < 3)
//						continue;
//					
//					//	ignore stop words
//					if (stopWords.lookup(epithetValue, false)) {
//						if (DEBUG) System.out.println("Ignoring stop word lexicon match: " + epithetValue);
//						continue;
//					}
//					
//					//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//					if (omniFat.doStemmingNegativeLookup(epithetValue)) {
//						if (DEBUG) System.out.println("Ignoring flected lexicon match: " + epithetValue);
//						continue;
//					}
//					
//					Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//					epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//					epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//					epithet.setAttribute("evidence", "precision");
//					epithet.setAttribute("evidenceDetail", precisionPatterns[p].name);
//					epithet.setAttribute("string", epithetValue);
//					epithet.setAttribute("value", epithetValue);
//					epithet.setAttribute("baseIndex", ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
//					
//					epithet.setAttribute("state", "positive");
//					rankGroupEpithetStates.setProperty(epithetValue, "positive");
//				}
//			}
//			
//			//	apply lexicons
//			OmniFatDictionary[] epithetDictionaries = rankGroups[rg].getPositiveDictionaries();
//			for (int d = 0; d < epithetDictionaries.length; d++) {
//				
//				//	extract and annotate epithets
//				Annotation[] epithets = Gamta.extractAllContained(data, epithetDictionaries[d], 1, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
//				for (int e = 0; e < epithets.length; e++) {
//					String epithetKey = this.getKey(epithets[e]);
//					String epithetValue = epithets[e].getValue();
//					
//					//	remember match & ignore duplicates
//					if (!rankGroupMatchKeys.add(epithetKey))
//						continue;
//					
//					//	ignore abbreviations (they come later)
//					if (epithetValue.length() < 3)
//						continue;
//					
//					//	ignore stop words
//					if (stopWords.lookup(epithetValue, false)) {
//						if (DEBUG) System.out.println("Ignoring stop word lexicon match: " + epithetValue);
//						continue;
//					}
//					
//					//	do negative filters for untrusted lexicons
//					if (!epithetDictionaries[d].trusted) {
//						
//						//	do sure negative lookup
//						if (!epithetDictionaries[d].trusted && rankGroups[rg].isEpithetsCapitalized() && !inSentenceTextTokens.contains(epithetValue) && textTokens.contains(epithetValue.toLowerCase())) {
//							if (DEBUG) System.out.println("Ignoring sure negative match: " + epithetValue);
//							continue;
//						}
//						
//						//	do negative patterns
//						OmniFatPattern negativePattern = null;
//						for (int n = 0; n < negativePatterns.length; n++)
//							if (negativePatterns[n].matches(epithetValue)) {
//								negativePattern = negativePatterns[n];
//								n = negativePatterns.length;
//							}
//						if (negativePattern != null) {
//							if (DEBUG) System.out.println("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue);
//							continue;
//						}
//						
//						//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//						if (omniFat.doStemmingNegativeLookup(epithetValue)) {
//							if (DEBUG) System.out.println("Ignoring flected lexicon match: " + epithetValue);
//							continue;
//						}
//					}
//					
//					Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//					epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//					epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//					epithet.setAttribute("evidence", "lexicon");
//					epithet.setAttribute("string", epithetValue);
//					epithet.setAttribute("value", epithetValue);
//					epithet.setAttribute("baseIndex", ("" + epithets[e].getStartIndex()));
//					
//					boolean caseMatch = epithetDictionaries[d].lookup(epithetValue, true);
//					String state;
//					String evidenceDetail;
//					if (epithetDictionaries[d].trusted) {
//						state = (caseMatch ? "positive" : "ambiguous");
//						evidenceDetail = (caseMatch ? epithetDictionaries[d].name : (epithetDictionaries[d].name + "/CI"));
//					}
//					else if (caseMatch) {
//						state = "recall";
//						evidenceDetail = epithetDictionaries[d].name;
//						for (int n = 0; n < negativeDictionaries.length; n++)
//							if (negativeDictionaries[n].lookup(epithetValue, true)) {
//								state = "ambiguous";
//								evidenceDetail = (epithetDictionaries[d].name + "/" + negativeDictionaries[n].name);
//								n = negativeDictionaries.length;
//							}
//					}
//					else {
//						state = "ambiguous";
//						evidenceDetail = (epithetDictionaries[d].name + "/CI");
//					}
//					epithet.setAttribute("state", state);
//					epithet.setAttribute("evidenceDetail", evidenceDetail);
//					if (!rankGroupEpithetStates.containsKey(epithetValue))
//						rankGroupEpithetStates.setProperty(epithetValue, state);
//				}
//			}
//			
//			//	apply recall patterns
//			OmniFatPattern[] recallPatterns = rankGroups[rg].getRecallPatterns();
//			for (int p = 0; p < recallPatterns.length; p++) {
//				
//				//	extract and annotate epithets
//				Annotation[] epithets = Gamta.extractAllMatches(data, recallPatterns[p].pattern, 0, (rankGroups[rg].isEpithetsCapitalized() || rankGroups[rg].isEpithetsLowerCase()));
//				for (int e = 0; e < epithets.length; e++) {
//					String epithetKey = this.getKey(epithets[e]);
//					String epithetValue = epithets[e].getValue();
//					int epithetValueOffset = 0;
//					if (epithets[e].size() != 1)
//						for (int v = 0; v < epithets[e].size(); v++) {
//							String value = epithets[e].valueAt(v);
//							if (Gamta.isWord(value)) {
//								epithetValue = value;
//								epithetValueOffset = v;
//							}
//						}
//					
//					//	remember match & ignore duplicates
//					if (!rankGroupMatchKeys.add(epithetKey))
//						continue;
//					
//					//	ignore abbreviations (they come later)
//					if (epithetValue.length() < 3)
//						continue;
//					
//					//	ignore stop words
//					if (stopWords.lookup(epithetValue, false)) {
//						if (DEBUG) System.out.println("Ignoring stop word lexicon match: " + epithetValue);
//						continue;
//					}
//					
//					//	do sure negative lookup
//					if (rankGroups[rg].isEpithetsCapitalized() && !inSentenceTextTokens.contains(epithetValue) && textTokens.contains(epithetValue.toLowerCase())) {
//						if (DEBUG) System.out.println("Ignoring sure negative match: " + epithetValue);
//						continue;
//					}
//					
//					//	do negative patterns
//					OmniFatPattern negativePattern = null;
//					for (int n = 0; n < negativePatterns.length; n++)
//						if (negativePatterns[n].matches(epithetValue)) {
//							negativePattern = negativePatterns[n];
//							n = negativePatterns.length;
//						}
//					if (negativePattern != null) {
//						if (DEBUG) System.out.println("Ignoring negative pattern '" + negativePattern.name + "' match: " + epithetValue);
//						continue;
//					}
//					
//					//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//					if (omniFat.doStemmingNegativeLookup(epithetValue)) {
//						if (DEBUG) System.out.println("Ignoring flected lexicon match: " + epithetValue);
//						continue;
//					}
//					
//					Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//					epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//					epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//					epithet.setAttribute("evidence", "recall");
//					epithet.setAttribute("evidenceDetail", recallPatterns[p].name);
//					epithet.setAttribute("string", epithetValue);
//					epithet.setAttribute("value", epithetValue);
//					epithet.setAttribute("baseIndex", ("" + (epithets[e].getStartIndex() + epithetValueOffset)));
//					
//					String state = rankGroupEpithetStates.getProperty(epithetValue);
//					String evidenceDetail = recallPatterns[p].name;
//					if (state == null) {
//						state = "uncertain";
//						for (int n = 0; n < negativeDictionaries.length; n++)
//							if (negativeDictionaries[n].lookup(epithetValue, true)) {
//								state = "negative";
//								evidenceDetail = (recallPatterns[p].name + "/" + negativeDictionaries[n].name);
//								n = negativeDictionaries.length;
//							}
//					}
//					epithet.setAttribute("state", state);
//					epithet.setAttribute("evidenceDetail", evidenceDetail);
//				}
//			}
//			
//			//	DO ABBREVIATIONS ONLY AFTER EPITHET FILTERING - THERE'S TOO MUCH AMBIGUITY BEFORE THAT
//		}
//		
//		
//		//	filter out duplicate base epithets, keeping the ones with the most certain state
//		Annotation[] epithets = data.getAnnotations("baseEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			int l = 1;
//			while (((e+l) < epithets.length) && AnnotationUtils.equals(epithets[e], epithets[e+l], false))
//				l++;
//			if (l != 1) {
//				Annotation[] duplicates = new Annotation[l];
//				if (DEBUG) System.out.println("Inspecting block of duplicate epithets:");
//				for (int d = 0; d < l; d++) {
//					duplicates[d] = epithets[e+d];
//					if (DEBUG) System.out.println("   " + ((String) epithets[e+d].getAttribute("state")) + ": " + epithets[e+d].toXML());
//				}
//				Arrays.sort(duplicates, this.stateAnnotationOrder);
//				for (int d = 0; d < duplicates.length; d++)
//					if (this.stateAnnotationOrder.compare(duplicates[0], duplicates[d]) != 0) {
//						if (DEBUG) System.out.println("  ==> removing inferior duplicate: " + duplicates[d].toXML());
//						data.removeAnnotation(duplicates[d]);
//					}
//				e += (duplicates.length-1);
//			}
//		}
//		
//		
//		//	collect possible abbreviations
//		HashMap rankGroupAbbreviationLists = new HashMap();
//		epithets = data.getAnnotations("baseEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//			StringVector rankGroupAbbreviationList = ((StringVector) rankGroupAbbreviationLists.get(rankGroup));
//			if (rankGroupAbbreviationList == null) {
//				rankGroupAbbreviationList = new StringVector();
//				rankGroupAbbreviationLists.put(rankGroup, rankGroupAbbreviationList);
//			}
//			String epithetValue = ((String) epithets[e].getAttribute("value"));
//			rankGroupAbbreviationList.addElementIgnoreDuplicates(epithetValue.substring(0,1));
//			rankGroupAbbreviationList.addElementIgnoreDuplicates(epithetValue.substring(0,1) + ".");
//			rankGroupAbbreviationList.addElementIgnoreDuplicates(epithetValue.substring(0,2));
//			rankGroupAbbreviationList.addElementIgnoreDuplicates(epithetValue.substring(0,2) + ".");
//		}
//		
//		//	annotate abbreviations
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			StringVector rankGroupAbbreviationList = ((StringVector) rankGroupAbbreviationLists.get(rankGroups[rg].getName()));
//			if (rankGroupAbbreviationList == null)
//				continue;
//			
//			//	extract abbreviations
//			Annotation[] abbreviatedEpithets = Gamta.extractAllContained(data, rankGroupAbbreviationList, 2, true);
//			for (int ae = 0; ae < abbreviatedEpithets.length; ae++) {
////				String abbreviatedEpithetKey = this.getKey(abbreviatedEpithets[ea]);
//				String abbreviatedEpithetValue = abbreviatedEpithets[ae].getValue();
//				
////				//	remember match
////				if (!rankGroupMatchKeys.add(abbreviatedEpithetKey))
////					continue;
////				
//				//	ignore stop words
//				if ((abbreviatedEpithetValue.length() > 1) && stopWords.lookup(abbreviatedEpithetValue, false))
//					continue;
//				
//				Annotation epithet = data.addAnnotation("abbreviatedEpithet", abbreviatedEpithets[ae].getStartIndex(), abbreviatedEpithets[ae].size());
//				epithet.setAttribute("rankGroup", rankGroups[rg].getName());
//				epithet.setAttribute("rankGroupNumber", ("" + rankGroups[rg].getOrderNumber()));
//				epithet.setAttribute("string", abbreviatedEpithetValue);
//				epithet.setAttribute("value", abbreviatedEpithetValue);
//				epithet.setAttribute("baseIndex", ("" + abbreviatedEpithets[ae].getStartIndex()));
//				
//				epithet.setAttribute("state", "abbreviated");
//			}
//		}
//		
//		
//		//	transfer states to uncertain multi-token pattern matches
//		HashMap rankGroupEpithetStateSets = new HashMap();
//		ArrayList multiTokenEpithets = new ArrayList();
//		
//		//	gather data
//		epithets = data.getAnnotations("baseEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			if (epithets[e].size() == 1) {
//				Properties rankGroupEpithetStateSet = ((Properties) rankGroupEpithetStateSets.get(epithets[e].getAttribute("rankGroup")));
//				if (rankGroupEpithetStateSet == null) {
//					rankGroupEpithetStateSet = new Properties();
//					rankGroupEpithetStateSets.put(epithets[e].getAttribute("rankGroup"), rankGroupEpithetStateSet);
//				}
//				rankGroupEpithetStateSet.setProperty(epithets[e].getValue(), ((String) epithets[e].getAttribute("state", "uncertain")));
//			}
//			else multiTokenEpithets.add(epithets[e]);
//		}
//		
//		//	transfer states
//		epithets = ((Annotation[]) multiTokenEpithets.toArray(new Annotation[multiTokenEpithets.size()]));
//		for (int e = 0; e < epithets.length; e++) {
//			Properties rankGroupEpithetStateSet = ((Properties) rankGroupEpithetStateSets.get(epithets[e].getAttribute("rankGroup")));
//			if (rankGroupEpithetStateSet != null) {
//				String epithetState = ((String) epithets[e].getAttribute("state"));
//				String valueState = rankGroupEpithetStateSet.getProperty((String) epithets[e].getAttribute("value"));
//				if ((valueState != null) && (this.stateOrder.compare(valueState, epithetState) < 0)) {
//					if (DEBUG) System.out.println("Promoting value state '" + valueState + "' to multi token match:\n  " + epithets[e].toXML());
//					epithets[e].setAttribute("state", valueState);
//				}
//			}
//		}
	}
//	
//	private Comparator stateOrder = new Comparator() {
//		private HashMap stateNumbers = new HashMap();
//		{
//			this.stateNumbers.put("positive", new Integer(1));
//			this.stateNumbers.put("recall", new Integer(2));
//			this.stateNumbers.put("ambiguous", new Integer(3));
//			this.stateNumbers.put("abbreviated", new Integer(3)); // treat abbreviations as ambiguous in this early stage
//			this.stateNumbers.put("uncertain", new Integer(4));
//			this.stateNumbers.put("negative", new Integer(5));
//		}
//		public int compare(Object o1, Object o2) {
//			int stateNumber1 = ((Integer) this.stateNumbers.get(o1)).intValue();
//			int stateNumber2 = ((Integer) this.stateNumbers.get(o2)).intValue();
//			return (stateNumber1 - stateNumber2);
//		}
//	};
//	private Comparator stateAnnotationOrder = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			return stateOrder.compare(((Annotation) o1).getAttribute("state", "negative"), ((Annotation) o2).getAttribute("state", "negative"));
//		}
//	};
	
	
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	get rank groups
//		String[] rankGroups = OmniFAT.getRankGroups();
//		
//		//	prepare filters
//		Dictionary stopWords = OmniFAT.getStopWords();
//		Set lexiconMatchKeys = new HashSet();
//		
//		//	annotate epithets for rank group lexicons
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			
//			//	get dictionary
//			Dictionary epithetDictionary = OmniFAT.getEpithetDictionary(rankGroups[rg]);
//			
//			//	apply dictionary
//			Annotation[] epithets = Gamta.extractAllContained(data, epithetDictionary, 1, (OmniFAT.isEpithetsCapitalized(rankGroups[rg]) || OmniFAT.isEpithetsLowerCase(rankGroups[rg])));
//			for (int e = 0; e < epithets.length; e++) {
//				String epithetKey = this.getKey(epithets[e]);
//				
//				//	remember lexicon match
//				lexiconMatchKeys.add(epithetKey);
//				
//				String epithetValue = epithets[e].getValue();
//				
//				//	ignore abbreviations (they come later)
//				if (epithetValue.length() < 3)
//					continue;
//				
//				//	ignore stop words
//				if (stopWords.lookup(epithetValue, false)) {
//					if (DEBUG) System.out.println("Ignoring stop word lexicon match: " + epithetValue);
//					continue;
//				}
//				
//				//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//				if (OmniFAT.doStemmingNegativeLookup(epithetValue)) {
//					if (DEBUG) System.out.println("Ignoring flected lexicon match: " + epithetValue);
//					continue;
//				}
//				
//				Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//				epithet.setAttribute("rankGroup", rankGroups[rg]);
//				epithet.setAttribute("rankGroupNumber", ("" + OmniFAT.getRankGroupOrderNumber(rankGroups[rg])));
//				epithet.setAttribute("evidence", "lexicon");
//				epithet.setAttribute("string", epithetValue);
//				epithet.setAttribute("value", epithetValue);
//			}
//		}
//		
//		//	annotate epithets for rank group patterns
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			
//			//	avoid duplicates
//			Set matchKeys = new HashSet();
//			
//			//	get patterns
//			String[] patterns = OmniFAT.getEpithetPatterns(rankGroups[rg]);
//			
//			//	apply patterns
//			for (int p = 0; p < patterns.length; p++) {
//				Annotation[] epithets = Gamta.extractAllMatches(data, patterns[p], 1, stopWords, stopWords, false, false, true);
//				for (int e = 0; e < epithets.length; e++) {
//					String epithetKey = this.getKey(epithets[e]);
//					
//					//	we've had this one already
//					if (!matchKeys.add(epithetKey))
//						continue;
//					
//					//	do not duplicate lexicon matches (their rank group is clear)
//					if (lexiconMatchKeys.contains(epithetKey))
//						continue;
//					
//					String epithetValue = epithets[e].getValue();
//					
//					//	ignore abbreviations (they come later)
//					if (epithetValue.length() < 3)
//						continue;
//					
//					//	ignore stop words
//					if (stopWords.lookup(epithetValue, false))
//						continue;
//					
//					//	ignore flected forms of negatives (even ambigous words are never flected if they act as epithets)
//					if (OmniFAT.doStemmingNegativeLookup(epithetValue))
//						continue;
//					
//					Annotation epithet = data.addAnnotation("baseEpithet", epithets[e].getStartIndex(), epithets[e].size());
//					epithet.setAttribute("rankGroup", rankGroups[rg]);
//					epithet.setAttribute("rankGroupNumber", ("" + OmniFAT.getRankGroupOrderNumber(rankGroups[rg])));
//					epithet.setAttribute("evidence", "pattern");
//					epithet.setAttribute("string", epithetValue);
//					epithet.setAttribute("value", epithetValue);
//				}
//			}
//		}
//		
//		//	collect text tokens
//		Set textTokens = new HashSet();
//		Set inSentenceTextTokens = new HashSet();
//		boolean inSentence = true;
//		for (int t = 0; t < data.size(); t++) {
//			String token = data.valueAt(t);
//			if (Gamta.isWord(token) || Gamta.isNumber(token)) {
//				textTokens.add(token);
//				if (inSentence)
//					inSentenceTextTokens.add(token);
//			}
//			inSentence = !Gamta.isSentenceEnd(token);
//		}
//		
//		//	filter out upper case epithets that appear in lower case elsewhere
//		Annotation[] epithets = data.getAnnotations("baseEpithet");
//		for (int e = 0; e < epithets.length; e++) {
////			if ("lexicon".equals(epithets[e].getAttribute("evidence")))
////				continue;
//			
//			String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//			if (OmniFAT.isEpithetsCapitalized(rankGroup) && !inSentenceTextTokens.contains(epithets[e].getValue()) && textTokens.contains(epithets[e].getValue().toLowerCase())) {
////				if (DEBUG) System.out.println("Removing as " + rankGroup + " for sure negative: " + epithets[e].toXML());
//				data.removeAnnotation(epithets[e]);
//			}
//		}
//		
//		//	collect possible abbreviations
//		epithets = data.getAnnotations("baseEpithet");
//		Map rankGroupAbbreviationSets = new HashMap();
//		for (int e = 0; e < epithets.length; e++) {
//			String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//			StringVector rankGroupAbbreviationSet = ((StringVector) rankGroupAbbreviationSets.get(rankGroup));
//			if (rankGroupAbbreviationSet == null) {
//				rankGroupAbbreviationSet = new StringVector();
//				rankGroupAbbreviationSets.put(rankGroup, rankGroupAbbreviationSet);
//			}
//			
//			String epithetValue = epithets[e].getValue();
//			rankGroupAbbreviationSet.addElementIgnoreDuplicates(epithetValue.substring(0,1));
//			rankGroupAbbreviationSet.addElementIgnoreDuplicates(epithetValue.substring(0,1) + ".");
//			rankGroupAbbreviationSet.addElementIgnoreDuplicates(epithetValue.substring(0,2));
//			rankGroupAbbreviationSet.addElementIgnoreDuplicates(epithetValue.substring(0,2) + ".");
//		}
//		
//		//	annotate abbreviations for each rank group
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			
//			//	get dictionary
//			Dictionary epithetAbbreviationDictionary = ((StringVector) rankGroupAbbreviationSets.get(rankGroups[rg]));
//			
//			//	apply dictionary
//			Annotation[] abbreviatedEpithets = Gamta.extractAllContained(data, epithetAbbreviationDictionary, 2, true);
//			for (int ea = 0; ea < abbreviatedEpithets.length; ea++) {
//				String abbreviatedEpithetValue = abbreviatedEpithets[ea].getValue();
//				
//				//	ignore stop words
//				if ((abbreviatedEpithetValue.length() > 1) && stopWords.lookup(abbreviatedEpithetValue, false))
//					continue;
//				
//				Annotation epithet = data.addAnnotation("abbreviatedEpithet", abbreviatedEpithets[ea].getStartIndex(), abbreviatedEpithets[ea].size());
//				epithet.setAttribute("rankGroup", rankGroups[rg]);
//				epithet.setAttribute("rankGroupNumber", ("" + OmniFAT.getRankGroupOrderNumber(rankGroups[rg])));
//				epithet.setAttribute("string", abbreviatedEpithetValue);
//				epithet.setAttribute("value", abbreviatedEpithetValue);
//			}
//		}
//	}
}