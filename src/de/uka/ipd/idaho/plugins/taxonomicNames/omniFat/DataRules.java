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
public class DataRules extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
//	//	TODOne: remove this after test
//	private OmniFAT.DataRuleSet[] ruleSets = new OmniFAT.DataRuleSet[0];
//	{
//		ArrayList dataRuleSets = new ArrayList();
//		
//		OmniFAT.DataRule[] block1 = {
////				new OmniFAT.DataRule(Pattern.compile("pp+"), "promote", true),
//				new OmniFAT.DataRule(Pattern.compile("p[prau]*p"), "promote", true),
//				new OmniFAT.DataRule(Pattern.compile("n+"), "remove", false),
//				new OmniFAT.DataRule(Pattern.compile("un+"), "remove", false),
//				new OmniFAT.DataRule(Pattern.compile("an+"), "remove", false),
//			};
//		dataRuleSets.add(new OmniFAT.DataRuleSet(block1));
//		
//		OmniFAT.DataRule[] block2 = {
//				new OmniFAT.DataRule(Pattern.compile("p+u"), "promote", true),
//				new OmniFAT.DataRule(Pattern.compile("p+[praun]?p+"), "promote", true),
////				new OmniFAT.DataRule(Pattern.compile("[bau]p[pau]*p"), "promote", true),
//				new OmniFAT.DataRule(Pattern.compile("[bra]p[prau]*p"), "promote", true),
//				new OmniFAT.DataRule(Pattern.compile("[bra]p+"), "promote", true),
//			};
//		dataRuleSets.add(new OmniFAT.DataRuleSet(block2));
//		
//		OmniFAT.DataRule[] block3 = {
//				new OmniFAT.DataRule(Pattern.compile("b+n+"), "remove", false),
//				new OmniFAT.DataRule(Pattern.compile("b+"), "remove", false),
//			};
//		dataRuleSets.add(new OmniFAT.DataRuleSet(block3));
//		
//		OmniFAT.DataRule[] block4 = {
//				new OmniFAT.DataRule(Pattern.compile("[pbrua]*n+[brua]*"), "remove", false),
//			};
//		dataRuleSets.add(new OmniFAT.DataRuleSet(block4));
////		
////		OmniFAT.DataRule[] block3 = {
////				new OmniFAT.DataRule(Pattern.compile("p"), "taxonName"),
////			};
////		dataRuleSets.add(new OmniFAT.DataRuleSet(block1));
//	}
//	//	TODOne: remove this after test
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.applyDataRules(data, this.getDataSet(data, parameters));
		
//		OmniFAT omniFat = this.getOmniFatInstance(parameters);
//		
//		//	store capitalization rules
//		HashSet capitalizedRankGroups = new HashSet();
//		OmniFAT.RankGroup[] rgs = omniFat.getRankGroups();
//		for (int rg = 0; rg < rgs.length; rg++) {
//			if (rgs[rg].isEpithetsCapitalized())
//				capitalizedRankGroups.add(rgs[rg].getName());
//		}
//		
//		//	do not create combinations more than once
//		HashMap combinationCache = new HashMap();
//		
//		//	apply rules block-wise
//		OmniFAT.DataRuleSet[] ruleSets = omniFat.getDataRuleSets();
//		for (int s = 0; s < ruleSets.length; s++) {
//			
//			//	get rule block
//			OmniFAT.DataRule[] ruleBlock = ruleSets[s].getRules();
//			
//			//	apply rules
//			boolean newPositiveEpithets;
//			boolean newAssignments;
//			int round = 0;
//			do {
//				newPositiveEpithets = false;
//				newAssignments = false;
//				round++;
//				
//				//	collect epithets and author names from positives
//				HashMap rankGroupEpithetLists = new HashMap();
//				StringVector authorNames = new StringVector();
//				StringVector authorNameParts = new StringVector();
//				this.fillPositiveLists(data, rankGroupEpithetLists, authorNames, authorNameParts);
//				
//				//	use author names to filter candidates
//				this.filterForAuthorNameEpithets(data, authorNameParts);
//				this.filterForEpithetAuthorNames(data, rankGroupEpithetLists, capitalizedRankGroups);
//				this.filterEpithetAbbreviations(data, rankGroupEpithetLists, capitalizedRankGroups);
//				
//				//	use positive epithets and author names to change states of candidate epithets
//				newPositiveEpithets = (newPositiveEpithets | this.promoteEpithetsForAuthor(data, authorNames, (s+1), round));
//				newPositiveEpithets = (newPositiveEpithets | this.promoteEpithetsForValue(data, rankGroupEpithetLists, (s+1), round));
//				
//				//	apply rules
//				for (int r = 0; r < ruleBlock.length; r++)
//					newAssignments = (newAssignments | this.applyRule(data, ruleBlock[r], (s+1), round));
//				
//				//	promote subsets of known combinations
//				newAssignments = (newAssignments | this.promoteCandidatesForCombination(data, combinationCache, (s+1), round));
//				
//				//	remove dangling epithets after promoting a candidate and removing nested ones
//				newAssignments = (newAssignments | this.filterDanglingEpithets(data));
//				
//				//	remove epithets built on the same base index as a more secure one
//				newAssignments = (newAssignments | this.filterEpithetsByState(data));
//				
//				//	remove candidates who have had an epithet removed
//				newAssignments = (newAssignments | this.filterForMissingEpithets(data));
//				
//				//	remove dangling epithets after removing candidates
//				newAssignments = (newAssignments | this.filterDanglingEpithets(data));
//				
//			} while (newAssignments);
//		}
	}
	
//	private boolean applyRule(MutableAnnotation data, OmniFAT.DataRule rule, int ruleSet, int round) {
//		final HashSet changeIdSet = new HashSet();
//		
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		Arrays.sort(taxonNameCandidates, new Comparator() {
////			public int compare(Object o1, Object o2) {
////				Annotation a1 = ((Annotation) o1);
////				Annotation a2 = ((Annotation) o2);
////				int c = AnnotationUtils.compare(a1, a2);
////				return ((c == 0) ? (((String) a2.getAttribute("source", "")).length() - ((String) a1.getAttribute("source", "")).length()) : c);
////			}
////		});
//		
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			if (taxonNameCandidates[c] == null)
////				continue;
//			
//			String epithetStateString = OmniFAT.DataRule.getEpithetStatusString(taxonNameCandidates[c]);
//			if (rule.matches(epithetStateString)) {
//				changeIdSet.add(taxonNameCandidates[c].getAnnotationID());
////				if ("remove".equals(rule.action)) {
////					if (DEBUG) System.out.println(" ==> Removing candidate for rule match '" + epithetStateString + "': " + taxonNameCandidates[c].toXML());
//////					data.removeAnnotation(taxonNameCandidates[c]);
//////					taxonNameCandidates[c] = null;
////					change = true;
////				}
////				
////				else if ("promote".equals(rule.action)) {
////					if (DEBUG) System.out.println(" ==> Promoting candidate for rule match '" + epithetStateString + "': " + taxonNameCandidates[c].toXML());
//////					taxonNameCandidates[c].changeTypeTo("taxonName");
////					taxonNameCandidates[c].setAttribute("evidence", ("document-" + ruleSet + "-" + round));
////					taxonNameCandidates[c].setAttribute("evidenceDetail", rule.pattern);
////					change = true;
////					
////					//	jump over nested candidates
////					while (((c+1) < taxonNameCandidates.length) && (taxonNameCandidates[c+1].getStartIndex() < taxonNameCandidates[c].getEndIndex()))
////						c++;
////					
//////					//	remove nested candidates?
//////					if (rule.removeNested) {
//////						int l = 1;
//////						while (((c+l) < taxonNameCandidates.length) && ((taxonNameCandidates[c+l] == null) || (taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex()))) {
//////							if (taxonNameCandidates[c+l] == null) {}
//////							else if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[c+l], false)) {
//////								if (DEBUG) System.out.println("  Keeping alternative candidate: " + taxonNameCandidates[c+l].toXML());
//////							}
//////							else if (AnnotationUtils.contains(taxonNameCandidates[c], taxonNameCandidates[c+l])) {
////////								if (DEBUG) System.out.println("  Removing nested candidate: " + taxonNameCandidates[c+l].toXML());
//////								data.removeAnnotation(taxonNameCandidates[c+l]);
//////								taxonNameCandidates[c+l] = null;
//////							}
//////							l++;
//////						}
//////					}
//////					
////					//	promote epithets
////					String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
////					QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
////					for (int e = 0; e < epithets.length; e++)
////						if (source.indexOf(epithets[e].getAnnotationID()) != -1)
////							epithets[e].setAttribute("state", "positive");
////				}
////				
////				else if ("feedback".equals(rule.action)) {
////					
////				}
//			}
//		}
//		
//		//	clean up candidates nested in positives, and unused epithets
//		Arrays.sort(taxonNameCandidates, new Comparator() {
//			public int compare(Object o1, Object o2) {
//				int c;
//				
//				Annotation a1 = ((Annotation) o1);
//				Annotation a2 = ((Annotation) o2);
//				c = AnnotationUtils.compare(a1, a2);
//				if (c != 0) return c;
//				
//				if (changeIdSet.contains(a1.getAnnotationID())) c--;
//				if (changeIdSet.contains(a2.getAnnotationID())) c++;
//				if (c != 0) return c;
//				
//				return (((String) a2.getAttribute("source", "")).length() - ((String) a1.getAttribute("source", "")).length());
//			}
//		});
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			else if (!changeIdSet.contains(taxonNameCandidates[c].getAnnotationID()))
//				continue;
//			
//			if ("remove".equals(rule.action)) {
//				if (DEBUG) System.out.println(" ==> Removing candidate for rule match '" + OmniFAT.DataRule.getEpithetStatusString(taxonNameCandidates[c]) + "': " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//				taxonNameCandidates[c] = null;
//			}
//			
//			else if ("promote".equals(rule.action)) {
//				if (DEBUG) System.out.println(" ==> Promoting candidate for rule match '" + OmniFAT.DataRule.getEpithetStatusString(taxonNameCandidates[c]) + "': " + taxonNameCandidates[c].toXML());
//				taxonNameCandidates[c].changeTypeTo("taxonName");
//				taxonNameCandidates[c].setAttribute("evidence", ("document-" + ruleSet + "-" + round));
//				taxonNameCandidates[c].setAttribute("evidenceDetail", rule.pattern);
//				
//				//	remove nested/overlapping candidates
//				int l = 1;
//				while (((c+l) < taxonNameCandidates.length) && ((taxonNameCandidates[c+l] == null) || (taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex()))) {
//					if (DEBUG) System.out.println("  Removing nested/overlapping candidate: " + taxonNameCandidates[c+l].toXML());
//					data.removeAnnotation(taxonNameCandidates[c+l]);
//					taxonNameCandidates[c+l] = null;
//					l++;
//				}
//				
//				//	promote epithets
//				String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//				QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//				for (int e = 0; e < epithets.length; e++)
//					if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//						epithets[e].setAttribute("state", "positive");
//			}
//			
//			else if ("feedback".equals(rule.action)) {
//				//	TODO: explicitly model feedback candidates
//			}
//		}
//		
//		
//		//	did we exclude or promote some candidate?
//		return (changeIdSet.size() != 0);
//	}
//	
//	private void fillPositiveLists(MutableAnnotation data, HashMap rankGroupEpithetLists, StringVector authorNames, StringVector authorNameParts) {
//		
//		//	collect epithets from positives
//		QueriableAnnotation[] taxonNames = data.getAnnotations("taxonName");
//		for (int t = 0; t < taxonNames.length; t++) {
//			String source = ((String) taxonNames[t].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNames[t].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					if (epithet.length() < 3)
//						continue;
//					
//					String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//					StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//					if (rankGroupEpithetList == null) {
//						rankGroupEpithetList = new StringVector();
//						rankGroupEpithetLists.put(rankGroup, rankGroupEpithetList);
//					}
//					rankGroupEpithetList.addElementIgnoreDuplicates(epithet);
//					for (int a = 1; a <= Math.min((epithet.length()-1), 2); a++) {
//						rankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a));
//						rankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a) + ".");
//					}
//					
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] epithetAuthorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < epithetAuthorNames.length; a++)
//						if (epithetSource.indexOf(epithetAuthorNames[a].getAnnotationID()) != -1) {
//							authorNameParts.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(epithetAuthorNames[a]));
//							authorNames.addElementIgnoreDuplicates(epithetAuthorNames[a].getValue());
//						}
//				}
//		}
//		
//		//	clean author names (anything shorter than 3 letters is too dangerous)
//		for (int a = 0; a < authorNameParts.size(); a++)
//			if (authorNameParts.get(a).length() < 3)
//				authorNameParts.remove(a--);
//		for (int a = 0; a < authorNames.size(); a++)
//			if (authorNames.get(a).length() < 3)
//				authorNames.remove(a--);
//	}
//	
//	private boolean filterForAuthorNameEpithets(MutableAnnotation data, StringVector authorNameParts) {
//		boolean change = false;
//		
//		//	remove candidates with sure positive author name in epithet position
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			Annotation killEpithet = null;
//			for (int e = 0; e < epithets.length; e++)
//				if ((source.indexOf(epithets[e].getAnnotationID()) != -1) && authorNameParts.contains((String) epithets[e].getAttribute("string"))) {
//					killEpithet = epithets[e];
//					e = epithets.length;
//				}
//			
//			if (killEpithet != null) {
//				if (DEBUG) System.out.println("Removing for author name epithet '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//				change = true;
//			}
//		}
//		
//		//	did we exclude some candidate?
//		return change;
//	}
//	
//	private boolean filterForEpithetAuthorNames(MutableAnnotation data, HashMap rankGroupEpithetLists, HashSet capitalizedRankGroups) {
//		boolean change = false;
//		
//		//	remove candidates with sure positive epithets in author name position
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			Annotation killEpithet = null;
//			Annotation killAuthor = null;
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] epithetAuthorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < epithetAuthorNames.length; a++) {
//						if (epithetSource.indexOf(epithetAuthorNames[a].getAnnotationID()) != -1) {
//							StringVector epithetAuthorTokens = TokenSequenceUtils.getTextTokens(epithetAuthorNames[a]);
//							for (int t = 0; t < epithetAuthorTokens.size(); t++)
//								if (epithetAuthorTokens.get(t).length() < 3)
//									epithetAuthorTokens.remove(t--);
//							
//							for (Iterator rgit = rankGroupEpithetLists.keySet().iterator(); (rgit != null) && rgit.hasNext();) {
//								String rankGroup = ((String) rgit.next());
//								if (capitalizedRankGroups.contains(rankGroup)) {
//									StringVector rankGroupEpithets = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//									if ((rankGroupEpithets != null) && (epithetAuthorTokens.intersect(rankGroupEpithets).size() != 0)) {
//										rgit = null;
////										killAuthor = epithetAuthorNames[a];
//										a = epithetAuthorNames.length;
//										killEpithet = epithets[e];
//										e = epithets.length;
//									}
//								}
//							}
//						}
//					}
//				}
//			
//			if (killEpithet != null) {
//				if (DEBUG) System.out.println("Removing for epithet author name '" + killAuthor.toXML() + "' in '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//				change = true;
//			}
//		}
//		
//		//	did we exclude some candidate?
//		return change;
//	}
//	
//	private boolean filterDanglingEpithets(MutableAnnotation data) {
//		boolean change = false;
//		
//		//	collect epithets bound to positives or candidates
//		HashSet epithetIdSet = new HashSet();
//		QueriableAnnotation[] positives;
//		positives = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < positives.length; c++) {
//			String source = ((String) positives[c].getAttribute("source", ""));
//			epithetIdSet.addAll(Arrays.asList(source.split("\\,")));
//		}
//		positives = data.getAnnotations("taxonName");
//		for (int c = 0; c < positives.length; c++) {
//			String source = ((String) positives[c].getAttribute("source", ""));
//			epithetIdSet.addAll(Arrays.asList(source.split("\\,")));
//		}
//		
//		//	check epithets
//		QueriableAnnotation[] epithets = data.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++)
//			if (!epithetIdSet.contains(epithets[e].getAnnotationID())) {
//				if (DEBUG) System.out.println("Removing dangeling epithet '" + epithets[e].toXML());
//				data.removeAnnotation(epithets[e]);
//				change = true;
//			}
//		
//		//	did we exclude some epithet?
//		return change;
//	}
//	
//	private boolean filterEpithetsByState(MutableAnnotation data) {
//		boolean change = false;
//		
//		//	sort epithets by base index
//		Annotation[] epithets = data.getAnnotations("epithet");
//		Arrays.sort(epithets, this.baseIndexOrder);
//		
//		for (int e = 0; e < epithets.length; e++) {
//			int l = 1;
//			while (((e+l) < epithets.length) && (this.baseIndexOrder.compare(epithets[e], epithets[e+l]) == 0))
//				l++;
//			
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
//		//	did we exclude some epithet?
//		return change;
//	}
//	
//	private Comparator baseIndexOrder = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			int bi1 = Integer.parseInt((String) ((Annotation) o1).getAttribute("baseIndex")); 
//			int bi2 = Integer.parseInt((String) ((Annotation) o2).getAttribute("baseIndex"));
//			return ((bi1 == bi2) ? stateAnnotationOrder.compare(o1, o2) : (bi1 - bi2));
//		}
//	};
//	
//	private Comparator stateOrder = new Comparator() {
//		private HashMap stateNumbers = new HashMap();
//		{
//			this.stateNumbers.put("positive", new Integer(1));
//			this.stateNumbers.put("recall", new Integer(2));
//			this.stateNumbers.put("abbreviated", new Integer(2)); // abbreviated is same as recall, as the document is a non-trusted dictionary by now
//			this.stateNumbers.put("ambiguous", new Integer(3));
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
//	private boolean filterForMissingEpithets(MutableAnnotation data) {
//		boolean change = false;
//		
//		//	collect epithets
//		HashSet epithetIdSet = new HashSet();
//		QueriableAnnotation[] epithets = data.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++)
//			epithetIdSet.add(epithets[e].getAnnotationID());
//		
//		//	remove candidates with missing epithets
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			String[] epithetIds = source.split("\\,");
//			for (int e = 0; e < epithetIds.length; e++)
//				if (!epithetIdSet.contains(epithetIds[e])) {
//					if (DEBUG) System.out.println("Removing candidate for missing epithet: " + taxonNameCandidates[c].toXML());
//					data.removeAnnotation(taxonNameCandidates[c]);
//					change = true;
//				}
//		}
//		
//		//	did we exclude some epithet?
//		return change;
//	}
//	
//	private boolean filterEpithetAbbreviations(MutableAnnotation data, HashMap rankGroupEpithetLists, HashSet capitalizedRankGroups) {
//		boolean change = false;
//		
//		//	collect all possible abbreviations (including those from candidates)
//		HashMap localRankGroupEpithetLists = new HashMap();
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					if (epithet.length() > 2) {
//						String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//						StringVector localRankGroupEpithetList = ((StringVector) localRankGroupEpithetLists.get(rankGroup));
//						if (localRankGroupEpithetList == null) {
//							localRankGroupEpithetList = new StringVector();
//							StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//							if (rankGroupEpithetList != null)
//								localRankGroupEpithetList.addContent(rankGroupEpithetList);
//							localRankGroupEpithetLists.put(rankGroup, localRankGroupEpithetList);
//						}
//						for (int a = 1; a <= Math.min((epithet.length()-1), 2); a++) {
//							localRankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a));
//							localRankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a) + ".");
//						}
//					}
//				}
//		}
//		
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			Annotation killEpithet = null;
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					if (epithet.length() > 2)
//						continue;
//					
//					StringVector localRankGroupEpithetList = ((StringVector) localRankGroupEpithetLists.get(epithets[e].getAttribute("rankGroup")));
//					if ((localRankGroupEpithetList == null) || !localRankGroupEpithetList.contains(epithet)) {
//						killEpithet = epithets[e];
//						e = epithets.length;
//					}
//				}
//			
//			if (killEpithet != null) {
////				if (DEBUG) System.out.println("Removing for epithet '" + killEpithet.toXML() + "' with impossible abbreviation:\n  " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//				change = true;
//			}
//		}
//		
//		//	did we exclude some candidate?
//		return change;
//	}
//	
//	private boolean promoteEpithetsForAuthor(MutableAnnotation data, StringVector docAuthorNames, int ruleSet, int round) {
//		boolean change = false;
//		
//		//	promote epithets with known author names
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					if ("positive".equals(epithets[e].getAttribute("state")))
//						continue;
//					
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] authorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < authorNames.length; a++)
//						if ((epithetSource.indexOf(authorNames[a].getAnnotationID()) != -1) && docAuthorNames.contains(authorNames[a].getValue())) {
//							if (DEBUG) System.out.println("Promoting epithet for known author name '" + authorNames[a].toXML() + "':\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("evidence", ("author-" + ruleSet + "-" + round));
//							epithets[e].setAttribute("state", "positive");
//							change = true;
//						}
//				}
//		}
//		
//		//	did we promote some epithet?
//		return change;
//	}
//	
//	private boolean promoteEpithetsForValue(MutableAnnotation data, HashMap rankGroupEpithetLists, int ruleSet, int round) {
//		boolean change = false;
//		
//		//	promote epithets with known values
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					if ("positive".equals(epithets[e].getAttribute("state")))
//						continue;
//					
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//					StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//					if ((rankGroupEpithetList != null) && rankGroupEpithetList.lookup(epithet, true)) {
//						if (epithet.length() < 3) {
//							if (DEBUG) System.out.println("Got document positive abbreviation:\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("evidence", ("document-" + ruleSet + "-" + round));
////							epithets[e].setAttribute("state", "abbreviated");
////							epithets[e].setAttribute("state", "positive"); // TODO TEST
//							epithets[e].setAttribute("state", (epithet.endsWith(".") ? "positive" : "abbreviated")); // TODO TEST
//						}
//						else {
//							if (DEBUG) System.out.println("Promoting epithet for document positive value:\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("evidence", ("document-" + ruleSet + "-" + round));
//							epithets[e].setAttribute("state", "positive");
//						}
//						change = true;
//					}
//				}
//		}
//		
//		//	did we promote some epithet?
//		return change;
//	}
//	
//	private boolean promoteCandidatesForCombination(MutableAnnotation data, HashMap combinationCache, int ruleSet, int round) {
//		
//		//	prepare hash join
//		HashMap combinationBuckets = new HashMap();
//		HashMap candidateCombinationBuckets = new HashMap();
//		HashSet matchedCandidateIDs = new HashSet();
//		
//		//	collect epithet combinations of positives, and bucketize them
//		QueriableAnnotation[] taxonNames = data.getAnnotations("taxonName");
//		for (int c = 0; c < taxonNames.length; c++) {
//			String source = ((String) taxonNames[c].getAttribute("source", ""));
//			if (source.indexOf(',') == -1)
//				continue;
//			
//			Combination comb = this.getCombination(taxonNames[c], combinationCache);
//			for (Iterator esit = comb.epithetStarts.iterator(); esit.hasNext();) {
//				String epithetStart = ((String) esit.next());
//				ArrayList combBucket = ((ArrayList) combinationBuckets.get(epithetStart));
//				if (combBucket == null) {
//					combBucket = new ArrayList();
//					combinationBuckets.put(epithetStart, combBucket);
//				}
//				combBucket.add(comb);
//			}
//		}
//		
//		//	collect epithet combinations of positives, and bucketize them
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			if (source.indexOf(',') == -1)
//				continue;
//			
//			Combination candComb = this.getCombination(taxonNameCandidates[c], combinationCache);
//			for (Iterator esit = candComb.epithetStarts.iterator(); esit.hasNext();) {
//				String epithetStart = ((String) esit.next());
//				ArrayList candCombBucket = ((ArrayList) candidateCombinationBuckets.get(epithetStart));
//				if (candCombBucket == null) {
//					candCombBucket = new ArrayList();
//					candidateCombinationBuckets.put(epithetStart, candCombBucket);
//				}
//				candCombBucket.add(candComb);
//			}
//		}
//		
//		//	do join
//		for (Iterator bit = candidateCombinationBuckets.keySet().iterator(); bit.hasNext();) {
//			String epithetStart = ((String)bit.next());
//			ArrayList combBucket = ((ArrayList) combinationBuckets.get(epithetStart));
//			if (combBucket == null)
//				continue;
//			ArrayList candCombBucket = ((ArrayList) candidateCombinationBuckets.get(epithetStart));
//			for (Iterator ccit = candCombBucket.iterator(); ccit.hasNext();) {
//				Combination candComb = ((Combination) ccit.next());
//				if (matchedCandidateIDs.contains(candComb.annotationId))
//					continue;
//				for (Iterator cit = combBucket.iterator(); (cit != null) && cit.hasNext();) {
//					Combination comb = ((Combination) cit.next());
//					if (comb.covers(candComb, true)) {
//						if (DEBUG) System.out.println("Promoting candidate '" + candComb.toString() + "' as covered by '" + comb.toString() + "'");
//						matchedCandidateIDs.add(candComb.annotationId);
//						cit = null;
//					}
//				}
//			}
//		}
//		
//		//	promote candidates
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			else if (!matchedCandidateIDs.contains(taxonNameCandidates[c].getAnnotationID()))
//				continue;
//			
//			taxonNameCandidates[c].changeTypeTo("taxonName");
//			taxonNameCandidates[c].setAttribute("evidence", ("document-" + ruleSet + "-" + round));
//			taxonNameCandidates[c].setAttribute("evidenceDetail", "combination");
//			
//			//	remove nested/overlapping candidates
//			int l = 1;
//			while (((c+l) < taxonNameCandidates.length) && ((taxonNameCandidates[c+l] == null) || (taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex()))) {
//				if (DEBUG) System.out.println("  Removing nested/overlapping candidate: " + taxonNameCandidates[c+l].toXML());
//				data.removeAnnotation(taxonNameCandidates[c+l]);
//				taxonNameCandidates[c+l] = null;
//				l++;
//			}
//			
//			//	promote epithets
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//					epithets[e].setAttribute("state", "positive");
//		}
//		
//		//	did we promote some candidate?
//		return (matchedCandidateIDs.size() != 0);
//	}
//	
//	private Combination getCombination(QueriableAnnotation taxonName, HashMap combinationCache) {
//		Combination comb = ((Combination) combinationCache.get(taxonName.getAnnotationID()));
//		if (comb == null) {
//			comb = new Combination(taxonName);
//			combinationCache.put(comb.annotationId, comb);
//		}
//		
//		return comb;
//	}
//	
//	private static class Combination {
//		final String annotationId;
//		HashSet epithetStarts = new HashSet();
//		private HashMap epithetMatchSets = new HashMap();
//		private HashMap firstEpithetMatchSets = new HashMap();
//		private LinkedHashMap epithetSets = new LinkedHashMap();
//		Combination(QueriableAnnotation taxonName) {
//			this.annotationId = taxonName.getAnnotationID();
//			String source = ((String) taxonName.getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonName.getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//					this.addEpithet(epithet, rankGroup);
//				}
//		}
//		Combination(String annotationId) {
//			this.annotationId = annotationId;
//		}
//		private void addEpithet(String epithet, String rankGroup) {
//			LinkedHashSet epithetSet = ((LinkedHashSet) this.epithetSets.get(rankGroup));
//			if (epithetSet == null) {
//				epithetSet = new LinkedHashSet();
//				this.epithetSets.put(rankGroup, epithetSet);
//			}
//			epithetSet.add(epithet);
//			
//			this.epithetStarts.add(epithet.substring(0,1).toLowerCase());
//			
//			HashSet epithetMatchSet = ((HashSet) this.epithetMatchSets.get(rankGroup));
//			if (epithetMatchSet == null) {
//				epithetMatchSet = new HashSet();
//				this.epithetMatchSets.put(rankGroup, epithetMatchSet);
//			}
//			epithetMatchSet.add(epithet);
//			if (epithet.length() > 3) {
//				epithetMatchSet.add(epithet.substring(0,1));
//				epithetMatchSet.add(epithet.substring(0,1) + ".");
//				epithetMatchSet.add(epithet.substring(0,2));
//				epithetMatchSet.add(epithet.substring(0,2) + ".");
//			}
//			if (!this.firstEpithetMatchSets.containsKey(rankGroup))
//				this.firstEpithetMatchSets.put(rankGroup, new HashSet(epithetMatchSet));
//		}
//		boolean covers(Combination comb) {
//			return this.covers(comb, false);
//		}
//		boolean covers(Combination comb, boolean allowSuffix) {
//			if (!allowSuffix) {
//				HashSet unmatchedRankGroups = new HashSet(this.epithetMatchSets.keySet());
//				unmatchedRankGroups.removeAll(comb.epithetSets.keySet());
//				if (unmatchedRankGroups.size() != 0)
//					return false;
//			}
//			
//			boolean gotFirstEpithet = !allowSuffix;
//			boolean matchAnchored = false;
//			
//			for (Iterator rgit = comb.epithetSets.keySet().iterator(); rgit.hasNext();) {
//				String rankGroup = ((String) rgit.next());
//				
//				LinkedHashSet epithetSet = ((LinkedHashSet) comb.epithetSets.get(rankGroup));
//				HashSet epithetMatchSet = ((HashSet) this.epithetMatchSets.get(rankGroup));
//				if (epithetMatchSet == null) {
//					if (gotFirstEpithet)
//						return false;
//					else continue;
//				}
//				HashSet firstEpithetMatchSet = (allowSuffix ? null : ((HashSet) this.firstEpithetMatchSets.get(rankGroup)));
//				
//				for (Iterator eit = epithetSet.iterator(); eit.hasNext();) {
//					String epithet = ((String) eit.next());
//					if (firstEpithetMatchSet != null) {
//						if (firstEpithetMatchSet.contains(epithet))
//							firstEpithetMatchSet = null;
//						else return false;
//					}
//					if (epithetMatchSet.contains(epithet)) {
//						gotFirstEpithet = true;
//						matchAnchored = (epithet.length() > 3);
//					}
//					else return false;
//				}
//			}
//			return matchAnchored;
//		}
//		public String toString() {
//			StringBuffer sb = new StringBuffer();
//			for (Iterator rgit = this.epithetSets.keySet().iterator(); rgit.hasNext();) {
//				String rankGroup = ((String) rgit.next());
//				LinkedHashSet epithetSet = ((LinkedHashSet) this.epithetSets.get(rankGroup));
//				for (Iterator eit = epithetSet.iterator(); eit.hasNext();) {
//					String epithet = ((String) eit.next());
//					if (sb.length() != 0)
//						sb.append(" ");
//					sb.append(epithet + "/" + rankGroup);
//				}
//			}
//			return sb.toString();
//		}
//	}
//	
//	public static void main(String[] args) {
//		//Dolichoderus (Monacis) debilis Emery
//		Combination match = new Combination("0815");
//		match.addEpithet("Dolichoderus", "genus");
//		match.addEpithet("Monacis", "genus");
//		match.addEpithet("debilis", "species");
//		if (DEBUG) System.out.println(match.toString());
//		
//		Combination testFull = new Combination("01");
//		testFull.addEpithet("D.", "genus");
//		testFull.addEpithet("debilis", "species");
//		if (DEBUG) System.out.println(testFull.toString());
//		
//		if (DEBUG) System.out.println(match.covers(testFull));
//		if (DEBUG) System.out.println(match.covers(testFull, true));
//		
//		Combination testSuffix = new Combination("81");
//		testSuffix.addEpithet("Monacis", "genus");
//		testSuffix.addEpithet("debilis", "species");
//		if (DEBUG) System.out.println(testSuffix.toString());
//		
//		if (DEBUG) System.out.println(match.covers(testSuffix));
//		if (DEBUG) System.out.println(match.covers(testSuffix, true));
//	}
//	
//	private static class DataRule {
//		final Pattern epithetStatePattern;
//		final String target;
//		final boolean removeNested;
//		DataRule(String epithetStatePattern, String target) {
//			this.epithetStatePattern = Pattern.compile(epithetStatePattern);
//			this.target = target;
//			this.removeNested = "taxonName".equals(target);
//		}
//	}
//	
//	private ArrayList dataRuleBlocks = new ArrayList();
//	
//	private Properties stateStringCharacters = new Properties();
//	{
//		this.stateStringCharacters.setProperty("positive", "p");
//		this.stateStringCharacters.setProperty("recall", "r");
//		this.stateStringCharacters.setProperty("abbreviated", "b");
//		this.stateStringCharacters.setProperty("ambiguous", "a");
//		this.stateStringCharacters.setProperty("uncertain", "u");
//		this.stateStringCharacters.setProperty("negative", "n");
//	}
//	
//	//	TODOne: remove this after test
//	{
//		DataRule[] block1 = {
////				new DataRule("pp+", "taxonName"),
//				new DataRule("p[prau]*p", "taxonName"),
//				new DataRule("n+", "remove"),
//				new DataRule("un+", "remove"),
//				new DataRule("an+", "remove"),
//			};
//		this.dataRuleBlocks.add(block1);
//		
//		DataRule[] block2 = {
//				new DataRule("p+u", "taxonName"),
//				new DataRule("p+[praun]?p+", "taxonName"),
////				new DataRule("[bau]p[pau]*p", "taxonName"),
//				new DataRule("[bra]p[prau]*p", "taxonName"),
//				new DataRule("[bra]p+", "taxonName"),
//			};
//		this.dataRuleBlocks.add(block2);
//		
//		DataRule[] block3 = {
//				new DataRule("b+n+", "remove"),
//				new DataRule("b+", "remove"),
//			};
//		this.dataRuleBlocks.add(block3);
//		
//		DataRule[] block4 = {
//				new DataRule("[pbrua]*n+[brua]*", "remove"),
//			};
//		this.dataRuleBlocks.add(block4);
////		
////		DataRule[] block3 = {
////				new DataRule("p", "taxonName"),
////			};
////		this.dataRuleBlocks.add(block3);
//	}
//	//	TODOne: remove this after test
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	apply rules block-wise
//		for (Iterator rbit = this.dataRuleBlocks.iterator(); rbit.hasNext();) {
//			
//			//	get rule block
//			DataRule[] ruleBlock = ((DataRule[]) rbit.next());
//			
//			//	apply rules
//			boolean newPositiveEpithets;
//			boolean newPositives;
//			int round = 0;
//			do {
//				newPositiveEpithets = false;
//				newPositives = false;
//				round++;
//				
//				//	collect epithets and author names from positives
//				HashMap rankGroupEpithetLists = new HashMap();
//				StringVector authorNames = new StringVector();
//				StringVector authorNameParts = new StringVector();
//				this.fillPositiveLists(data, rankGroupEpithetLists, authorNames, authorNameParts);
//				
//				//	use author names to filter candidates
//				this.filterEpithetsForAuthorNames(data, authorNameParts);
//				this.filterEpithetsByAuthorNames(data, rankGroupEpithetLists);
//				
//				//	use positive epithets and author names to change states of candidate epithets
//				newPositiveEpithets = (newPositiveEpithets | this.promoteEpithetsForAuthor(data, authorNames, round));
//				newPositiveEpithets = (newPositiveEpithets | this.promoteEpithetsForValue(data, rankGroupEpithetLists, round));
//				
//				//	apply rules
//				for (int r = 0; r < ruleBlock.length; r++)
//					newPositives = (newPositives | this.applyRule(data, ruleBlock[r], round));
//				
//			} while (newPositives);
//		}
//	}
//	
//	private boolean applyRule(MutableAnnotation data, DataRule rule, int round) {
//		boolean change = false;
//		
//		//	remove candidates with sure positive author name in epithet position
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		Arrays.sort(taxonNameCandidates, new Comparator() {
//			public int compare(Object o1, Object o2) {
//				Annotation a1 = ((Annotation) o1);
//				Annotation a2 = ((Annotation) o2);
//				int c = AnnotationUtils.compare(a1, a2);
//				return ((c == 0) ? (((String) a2.getAttribute("source", "")).length() - ((String) a1.getAttribute("source", "")).length()) : c);
//			}
//		});
//		
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String epithetStateString = this.getEpithetStateString(taxonNameCandidates[c]);
//			if (rule.epithetStatePattern.matcher(epithetStateString).matches()) {
//				
//				if ("taxonName".equals(rule.target)) {
//					if (DEBUG) System.out.println(" ==> Promoting candidate for rule match '" + epithetStateString + "': " + taxonNameCandidates[c].toXML());
//					taxonNameCandidates[c].changeTypeTo(rule.target);
//					taxonNameCandidates[c].setAttribute("evidence", ("document-" + round));
//					change = true;
//					
//					//	remove nested candidates?
//					if (rule.removeNested) {
//						int l = 1;
//						while (((c+l) < taxonNameCandidates.length) && ((taxonNameCandidates[c+l] == null) || (taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex()))) {
//							if (taxonNameCandidates[c+l] == null) {}
//							else if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[c+l], false)) {
//								if (DEBUG) System.out.println("  Keeping alternative candidate: " + taxonNameCandidates[c+l].toXML());
//							}
//							else if (AnnotationUtils.contains(taxonNameCandidates[c], taxonNameCandidates[c+l])) {
////								if (DEBUG) System.out.println("  Removing nested candidate: " + taxonNameCandidates[c+l].toXML());
//								data.removeAnnotation(taxonNameCandidates[c+l]);
//								taxonNameCandidates[c+l] = null;
//							}
//							l++;
//						}
//					}
//				}
//				
//				else if ("feedback".equals(rule.target)) {
//					
//				}
//				
//				else if ("remove".equals(rule.target)) {
//					if (DEBUG) System.out.println(" ==> Removing candidate for rule match '" + epithetStateString + "': " + taxonNameCandidates[c].toXML());
//					data.removeAnnotation(taxonNameCandidates[c]);
//					taxonNameCandidates[c] = null;
//					change = true;
//				}
//			}
//		}
//		
//		//	did we exclude some candidate?
//		return change;
//	}
//	
//	private String getEpithetStateString(QueriableAnnotation taxonNameCandidate) {
//		StringBuffer stateString = new StringBuffer();
//		String source = ((String) taxonNameCandidate.getAttribute("source", ""));
//		QueriableAnnotation[] epithets = taxonNameCandidate.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//				String state = ((String) epithets[e].getAttribute("state"));
//				if (state == null)
//					state = "uncertain";
//				stateString.append(this.stateStringCharacters.getProperty(state, "u"));
//			}
//		return stateString.toString();
//	}
//	
//	private void fillPositiveLists(MutableAnnotation data, HashMap rankGroupEpithetLists, StringVector authorNames, StringVector authorNameParts) {
//		
//		//	collect epithets from positives
//		QueriableAnnotation[] taxonNames = data.getAnnotations("taxonName");
//		for (int t = 0; t < taxonNames.length; t++) {
//			String source = ((String) taxonNames[t].getAttribute("source", ""));
//			Annotation[] epithets = ((QueriableAnnotation) taxonNames[t]).getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					if (epithet.length() < 3)
//						continue;
//					
//					String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//					StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//					if (rankGroupEpithetList == null) {
//						rankGroupEpithetList = new StringVector();
//						rankGroupEpithetLists.put(rankGroup, rankGroupEpithetList);
//					}
//					rankGroupEpithetList.addElementIgnoreDuplicates(epithet);
//					for (int a = 1; a <= Math.min((epithet.length()-1), 2); a++) {
//						rankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a));
//						rankGroupEpithetList.addElementIgnoreDuplicates(epithet.substring(0,a) + ".");
//					}
//				}
//		}
//		
//		for (int t = 0; t < taxonNames.length; t++) {
//			String source = ((String) taxonNames[t].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNames[t].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] epithetAuthorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < epithetAuthorNames.length; a++)
//						if (epithetSource.indexOf(epithetAuthorNames[a].getAnnotationID()) != -1) {
//							authorNameParts.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(epithetAuthorNames[a]));
//							authorNames.addElementIgnoreDuplicates(epithetAuthorNames[a].getValue());
//						}
//				}
//		}
//		
//		//	clean author names (anything shorter than 3 letters is too dangerous)
//		for (int a = 0; a < authorNameParts.size(); a++)
//			if (authorNameParts.get(a).length() < 3)
//				authorNameParts.remove(a--);
//		for (int a = 0; a < authorNames.size(); a++)
//			if (authorNames.get(a).length() < 3)
//				authorNames.remove(a--);
//	}
//	
//	private boolean filterEpithetsForAuthorNames(MutableAnnotation data, StringVector authorNameParts) {
//		boolean change = false;
//		
//		//	remove candidates with sure positive author name in epithet position
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			Annotation killEpithet = null;
//			for (int e = 0; e < epithets.length; e++)
//				if ((source.indexOf(epithets[e].getAnnotationID()) != -1) && authorNameParts.contains((String) epithets[e].getAttribute("string"))) {
//					killEpithet = epithets[e];
//					e = epithets.length;
//				}
//			
//			if (killEpithet != null) {
////				if (DEBUG) System.out.println("Removing for author name epithet '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//				change = true;
//			}
//		}
//		
//		//	did we exclude some candidate?
//		return change;
//	}
//	
//	private boolean filterEpithetsByAuthorNames(MutableAnnotation data, HashMap rankGroupEpithetLists) {
//		boolean change = false;
//		
//		//	remove candidates with sure positive author name in epithet position
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			Annotation killEpithet = null;
//			Annotation killAuthor = null;
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] epithetAuthorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < epithetAuthorNames.length; a++) {
//						if (epithetSource.indexOf(epithetAuthorNames[a].getAnnotationID()) != -1) {
//							StringVector epithetAuthorTokens = TokenSequenceUtils.getTextTokens(epithetAuthorNames[a]);
//							for (int t = 0; t < epithetAuthorTokens.size(); t++)
//								if (epithetAuthorTokens.get(t).length() < 3)
//									epithetAuthorTokens.remove(t--);
//							
//							for (Iterator rgit = rankGroupEpithetLists.keySet().iterator(); (rgit != null) && rgit.hasNext();) {
//								String rankGroup = ((String) rgit.next());
//								if (OmniFAT.isEpithetsCapitalized(rankGroup)) {
//									StringVector rankGroupEpithets = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//									if ((rankGroupEpithets != null) && (epithetAuthorTokens.intersect(rankGroupEpithets).size() != 0)) {
//										rgit = null;
//										killAuthor = epithetAuthorNames[a];
//										a = epithetAuthorNames.length;
//										killEpithet = epithets[e];
//										e = epithets.length;
//									}
//								}
//							}
//						}
//					}
//				}
//			
//			if (killEpithet != null) {
//				if (DEBUG) System.out.println("Removing for epithet author name '" + killAuthor.toXML() + "' in '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//				change = true;
//			}
//		}
//		
//		//	did we exclude some candidate?
//		return change;
//	}
//	
//	private boolean promoteEpithetsForAuthor(MutableAnnotation data, StringVector docAuthorNames, int round) {
//		boolean change = false;
//		
//		//	promote epithets with known author names
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					if ("positive".equals(epithets[e].getAttribute("state")))
//						continue;
//					
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] authorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < authorNames.length; a++)
//						if ((epithetSource.indexOf(authorNames[a].getAnnotationID()) != -1) && docAuthorNames.contains(authorNames[a].getValue())) {
//							if (DEBUG) System.out.println("Promoting epithet for known author name '" + authorNames[a].toXML() + "':\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("evidence", ("author-" + round));
//							epithets[e].setAttribute("state", "positive");
//							change = true;
//						}
//				}
//		}
//		
//		//	did we promote some epithet?
//		return change;
//	}
//	
//	private boolean promoteEpithetsForValue(MutableAnnotation data, HashMap rankGroupEpithetLists, int round) {
//		boolean change = false;
//		
//		//	promote epithets with known values
//		QueriableAnnotation[] taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					if ("positive".equals(epithets[e].getAttribute("state")))
//						continue;
//					
//					String epithet = ((String) epithets[e].getAttribute("string"));
//					String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//					StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetLists.get(rankGroup));
//					if ((rankGroupEpithetList != null) && rankGroupEpithetList.lookup(epithet, (OmniFAT.isEpithetsCapitalized(rankGroup) || OmniFAT.isEpithetsLowerCase(rankGroup)))) {
//						if (epithet.length() < 3) {
////							if (DEBUG) System.out.println("Got document positive abbreviation:\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("evidence", ("document-" + round));
//							epithets[e].setAttribute("state", "abbreviated");
//						}
//						else {
////							if (DEBUG) System.out.println("Promoting epithet for document positive value:\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("evidence", ("document-" + round));
//							epithets[e].setAttribute("state", "positive");
//						}
//						change = true;
//					}
//				}
//		}
//		
//		//	did we promote some epithet?
//		return change;
//	}
}