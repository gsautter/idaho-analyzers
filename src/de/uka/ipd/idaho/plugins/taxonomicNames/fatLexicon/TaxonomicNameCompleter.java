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


import java.util.ArrayList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * @author sautter
 *
 */
public class TaxonomicNameCompleter extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		Annotation[] taxonomicNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	filter out names with above-genus rank
		ArrayList genusOrBelowList = new ArrayList();
		for (int t = 0; t < taxonomicNames.length; t++) {
			if ((taxonomicNames[t].size() == 1) && Gamta.isFirstLetterUpWord(taxonomicNames[t].firstValue())) {
				String name = Gamta.capitalize(taxonomicNames[t].firstValue());
				if (!name.endsWith("idae") && !name.endsWith("inae") && !name.endsWith("ini"))
					genusOrBelowList.add(taxonomicNames[t]);
			} else genusOrBelowList.add(taxonomicNames[t]);
		}
		taxonomicNames = ((Annotation[]) genusOrBelowList.toArray(new Annotation[genusOrBelowList.size()]));
		
		//	resolve abbreviated parts
		boolean newResolutions = true;
		int outRound = 0;
		while (newResolutions) {
			outRound ++;
			
			//	iteratively search full forms
			int inRound = 0;
			while (newResolutions) {
				inRound ++;
				newResolutions = false;
				
				//	collect matches containing abbreviations and ones containing possible solutions
				ArrayList[] abbreviatedLists = {new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
				ArrayList[] fullLists = {new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
				
				for (int t = 0; t < taxonomicNames.length; t++) {
					for (int p = 0; p < (PART_NAMES.length - 1); p++) {
						if (taxonomicNames[t].hasAttribute(PART_NAMES[p])) {
							String part = taxonomicNames[t].getAttribute(PART_NAMES[p]).toString();
							if (part.length() < 3) abbreviatedLists[p].add(taxonomicNames[t]);
							else fullLists[p].add(taxonomicNames[t]);
						}
					}
				}
				
				//	complete parts
				for (int p = 0; p < (PART_NAMES.length - 1); p++) {
					ArrayList abbreviatedList = abbreviatedLists[p];
					ArrayList fullList = fullLists[p];
					String toComplete = PART_NAMES[p];
					
					//	complete parts
					for (int a = 0; (a < abbreviatedList.size()) && (fullList.size() != 0); a++) {
						Annotation abbreviated = ((Annotation) abbreviatedList.get(a));
//						System.out.println("trying to resolve " + abbreviated.getAttribute(toComplete) + " in " + TokenSequenceUtils.concatTokens(abbreviated));
						
						//	find full form
						String fullForm = null;
						int bestMatchVote = 0;
						int bestMatchDist = 0;
						for (int f = 0; f < fullList.size(); f++) {
							Annotation full = ((Annotation) fullList.get(f));
//							System.out.println("  comparing to " + full.getAttribute(toComplete) + " in " + TokenSequenceUtils.concatTokens(full));
							
							//	possible full form
							if (full.getAttribute(toComplete).toString().startsWith(abbreviated.getAttribute(toComplete).toString())) {
//								System.out.println("  ==> possible match");
								
								//	compare subsequent parts
								int vote = 0;
								for (int ap = (p + 1); ap < PART_NAMES.length; ap++) {
									String aAnchor = abbreviated.getAttribute(PART_NAMES[ap], "").toString();
									String fAnchor = full.getAttribute(PART_NAMES[ap], "").toString();
									if (aAnchor.equals(fAnchor)) vote += ((aAnchor.length() == 0) ? 0 : ((aAnchor.length() < 3) ? 1 : 2));
									else if (aAnchor.startsWith(fAnchor)) vote+= ((fAnchor.length() == 0) ? 0 : 1);
									else if (fAnchor.startsWith(aAnchor)) vote+= ((aAnchor.length() == 0) ? 0 : 1);
									else if (vote < 2) vote = -(PART_NAMES.length * 2);
								}
//								System.out.println("    - vote is " + vote);
								
								//	new top match
								if (vote > bestMatchVote) {
									bestMatchVote = vote;
									bestMatchDist = Math.abs(abbreviated.getStartIndex() - full.getStartIndex()) * ((full.compareTo(abbreviated) < 0) ? 1 : 3);
									fullForm = full.getAttribute(toComplete).toString();
//									System.out.println("    - full form is " + fullForm);
									
								//	same score as current best, compare distance (full form preceeding abbreviation is rated better than full form succeeding abbreviation)
								} else if ((vote != 0) && (vote == bestMatchVote)) {
									int dist = Math.abs(abbreviated.getStartIndex() - full.getStartIndex()) * ((abbreviated.getStartIndex() < full.getStartIndex()) ? 1 : 3);
									if (dist < bestMatchDist) {
										bestMatchDist = dist;
										fullForm = full.getAttribute(toComplete).toString();
//										System.out.println("    - full form is " + fullForm);
									}
								}
							}
						}
						
						//	found full form anchored by other name parts
						if (fullForm != null) {
//							System.out.println("  - found full form as " + fullForm);
							abbreviated.setAttribute(toComplete, fullForm);
//							abbreviated.setAttribute(toComplete + ".outerRound", ("" + outRound));
//							abbreviated.setAttribute(toComplete + ".innerRound", ("" + inRound));
//							abbreviated.setAttribute(toComplete + ".bestMatchVote", ("" + bestMatchVote));
//							abbreviated.setAttribute(toComplete + ".bestMatchDistance", ("" + bestMatchDist));
							newResolutions = true;
						}
					}
				}
			}
			
			//	try to find full forms in preceedence of abbreviations, also try to fill in blank genera and species (may be omitted in abbreviations)
			//	collect matches containing abbreviations and ones containing possible solutions
			ArrayList[] abbreviatedLists = {new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
			ArrayList[] fullLists = {new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
			
			for (int p = 0; p < (PART_NAMES.length - 1); p++) {
				String lastGiven = null;
				for (int t = 0; t < taxonomicNames.length; t++) {
					
					//	part given at least as abbreviation
					if (taxonomicNames[t].hasAttribute(PART_NAMES[p])) {
						String part = taxonomicNames[t].getAttribute(PART_NAMES[p]).toString();
						lastGiven = part;
						if (part.length() < 3) abbreviatedLists[p].add(taxonomicNames[t]);
						else fullLists[p].add(taxonomicNames[t]);
					}
					
					//	genus missing (may occur in enumerations)
					else if ((p == 0) && (lastGiven != null)) {
						taxonomicNames[t].setAttribute(PART_NAMES[p], lastGiven);
//						taxonomicNames[r].setAttribute(PART_NAMES[p] + ".completionEvidence", "missing");
//						taxonomicNames[r].setAttribute(PART_NAMES[p] + ".completionRound", ("" + outRound));
						if (lastGiven.length() < 3) abbreviatedLists[p].add(taxonomicNames[t]);
						else fullLists[p].add(taxonomicNames[t]);
					}
					
					//	species missing (may occur in enumerations)
					else if ((p == 2) && (lastGiven != null)) {
						
						//	do not fill in species in standalone genera
						boolean lowerPartGiven = false;
						for (int l = 3; l < PART_NAMES.length; l++)
							lowerPartGiven = (lowerPartGiven || taxonomicNames[t].hasAttribute(PART_NAMES[l]));
						
						if (lowerPartGiven) {
							taxonomicNames[t].setAttribute(PART_NAMES[p], lastGiven);
//							taxonomicNames[r].setAttribute(PART_NAMES[p] + ".completionEvidence", "missing");
//							taxonomicNames[r].setAttribute(PART_NAMES[p] + ".completionRound", ("" + outRound));
							if (lastGiven.length() < 3) abbreviatedLists[p].add(taxonomicNames[t]);
							else fullLists[p].add(taxonomicNames[t]);
						}
					}
				}
			}
			
			//	complete parts
			for (int p = 0; p < (PART_NAMES.length - 1); p++) {
				ArrayList abbreviatedList = abbreviatedLists[p];
				ArrayList fullList = fullLists[p];
				String toComplete = PART_NAMES[p];
				
				//	complete parts
				for (int a = 0; (a < abbreviatedList.size()) && (fullList.size() != 0); a++) {
					Annotation abbreviated = ((Annotation) abbreviatedList.get(a));
//					System.out.println("Completing " + abbreviated.getStartIndex() + ", " + abbreviated.size() + ": " + abbreviated.toXML());
					
					//	find full form
					String fullForm = null;
					String abbreviation = abbreviated.getAttribute(toComplete, "").toString();
					
					//	find full forms in precedence of abbreviation
					for (int f = 0; f < fullList.size(); f++) {
						Annotation full = ((Annotation) fullList.get(f));
						String fullCandidate = full.getAttribute(toComplete, "").toString();
						if (full.compareTo(abbreviated) < 0) {
							if (fullCandidate.startsWith(abbreviation)) {
//								System.out.println("   - candidate " + full.getStartIndex() + ", " + full.size() + ": " + full.toXML());
								fullForm = fullCandidate;
							}
						} else f = fullList.size();
					}
					
					//	found full form
					if (fullForm != null) {
						abbreviated.setAttribute(toComplete, fullForm);
//						abbreviated.setAttribute(toComplete + ".outerRound", ("" + outRound));
						newResolutions = true;
					}
				}
			}
		}
	}
}
