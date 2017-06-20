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
public class CandidateTagger extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
//	private String[] rankGroupNames = new String[0];
//	private int[] rankGroupSizes = new int[0];
//	
//	private int speciesRankGroupNumber = Integer.MIN_VALUE;
//	private int speciesRankNumber = Integer.MIN_VALUE;
	
//	private String[] rankGroups = new String[0];
//	private int[] rankGroupSizes = new int[0];
//	
//	private int speciesRankGroupNumber = Integer.MIN_VALUE;
//	private int speciesRankNumber = Integer.MIN_VALUE;
	
	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
//		super.initAnalyzer();
//		
//		//	gather rank statistics
//		OmniFAT.RankGroup[] rankGroups = OmniFAT.getRankGroups();
//		this.rankGroupNames = new String[rankGroups.length];
//		this.rankGroupSizes = new int[rankGroups.length];
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			this.rankGroupNames[rg] = rankGroups[rg].getName();
//			OmniFAT.Rank[] ranks = rankGroups[rg].getRanks();
//			if (ranks == null)
//				this.rankGroupSizes[rg] = 0;
//			else {
//				this.rankGroupSizes[rg] = ranks.length;
//				for (int r = 0; r < ranks.length; r++)
//					if (SPECIES_ATTRIBUTE.equals(ranks[r].getName())) {
//						this.speciesRankGroupNumber = rankGroups[rg].getOrderNumber();
//						this.speciesRankNumber = ranks[r].getOrderNumber();
//					}
//			}
//		}
//	}
	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
//		super.initAnalyzer();
//		
//		//	gather rank statistics
//		this.rankGroups = OmniFAT.getRankGroups();
//		this.rankGroupSizes = new int[this.rankGroups.length];
//		for (int rg = 0; rg < this.rankGroups.length; rg++) {
//			String [] ranks = OmniFAT.getRanks(this.rankGroups[rg]);
//			this.rankGroupSizes[rg] = ((ranks == null) ? 0 : ranks.length);
//		}
//		
//		//	gather rank data
//		this.speciesRankGroupNumber = OmniFAT.getRankGroupOrderNumber(OmniFAT.getRankGroup(SPECIES_ATTRIBUTE));
//		this.speciesRankNumber = OmniFAT.getRankOrderNumber(SPECIES_ATTRIBUTE);
//	}
//
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.tagCandidates(data, this.getDataSet(data, parameters));
		
//		OmniFAT omniFat = this.getOmniFatInstance(parameters);
//		
//		//	gather rank statistics
//		OmniFAT.RankGroup[] rankGroups = omniFat.getRankGroups();
//		String[] rankGroupNames = new String[rankGroups.length];
//		int[] rankGroupSizes = new int[rankGroups.length];
//		Set requiredRankGroupNumbers = new TreeSet();
//		Set requiredRankNumbers = new TreeSet();
//		for (int rg = 0; rg < rankGroups.length; rg++) {
//			rankGroupNames[rg] = rankGroups[rg].getName();
//			OmniFAT.Rank[] ranks = rankGroups[rg].getRanks();
//			rankGroupSizes[rg] = ranks.length;
//			for (int r = 0; r < ranks.length; r++)
//				if (ranks[r].isEpithetRequired()) {
//					requiredRankGroupNumbers.add(new Integer(rankGroups[rg].getOrderNumber()));
//					requiredRankNumbers.add(new Integer(ranks[r].getOrderNumber()));
//				}
//		}
//
//		
//		//	join epithets to candidates
//		Annotation[] taxonNameCandidates;
//		int added;
//		int maxEpithets = omniFat.getRanks().length;
//		
//		
//		//	join adjacent epithets to taxon names
//		do {
//			added = 0;
//			taxonNameCandidates = OmniFatFunctions.joinAdjacent(data, "epithet", omniFat.getInterEpithetPunctuationMarks(), "taxonNameCandidate", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
////					joint.copyAttributes(rightSource);
////					if ("recall".equals(joint.getAttribute("evidence")))
////						joint.setAttribute("evidence", "joint");
//					
//					String source = (leftSource.getAnnotationID() + "," + rightSource.getAnnotationID());
//					joint.setAttribute("source", source);
//					
//					return joint;
//				}
//				public String getDeDuplicationKey(Annotation annotation) {
//					return (annotation.hasAttribute("source") ? ((String) annotation.getAttribute("source")) : super.getDeDuplicationKey(annotation));
//				}
//			});
//			for (int c = 0; c < taxonNameCandidates.length; c++) {
//				if (this.isLineBroken(taxonNameCandidates[c]))
//					continue;
//				
//				if (this.countEpithetsBySource(taxonNameCandidates[c]) > maxEpithets)
//					continue;
//				
//				QueriableAnnotation taxonNameCandidate = data.addAnnotation("taxonNameCandidate", taxonNameCandidates[c].getStartIndex(), taxonNameCandidates[c].size());
//				taxonNameCandidate.copyAttributes(taxonNameCandidates[c]);
//				
//				if (this.isCandidateBroken(taxonNameCandidate, rankGroupNames, rankGroupSizes, requiredRankGroupNumbers, requiredRankNumbers)) {
//					if (DEBUG) System.out.println("Removing broken candidate: " + taxonNameCandidate.toXML());
//					data.removeAnnotation(taxonNameCandidate);
//				}
//				
//				else added++;
//			}
//			if (DEBUG) System.out.println(added + " new base candidates added");
//		} while (added != 0);
//		
//		
//		//	join epithets to adjacent taxon names
//		do {
//			added = 0;
//			taxonNameCandidates = OmniFatFunctions.joinAdjacent(data, "epithet", "taxonNameCandidate", omniFat.getInterEpithetPunctuationMarks(), "taxonNameCandidate", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
////					joint.copyAttributes(rightSource);
////					if ("recall".equals(joint.getAttribute("evidence")))
////						joint.setAttribute("evidence", "joint");
//					
//					String rsid = ((String) rightSource.getAttribute("source"));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					String source = (leftSource.getAnnotationID() + "," + rsid);
//					joint.setAttribute("source", source);
//					
//					return joint;
//				}
//				public String getDeDuplicationKey(Annotation annotation) {
//					return (annotation.hasAttribute("source") ? ((String) annotation.getAttribute("source")) : super.getDeDuplicationKey(annotation));
//				}
//			});
//			for (int c = 0; c < taxonNameCandidates.length; c++) {
//				if (this.isLineBroken(taxonNameCandidates[c]))
//					continue;
//				
//				if (this.countEpithetsBySource(taxonNameCandidates[c]) > maxEpithets)
//					continue;
//				
//				QueriableAnnotation taxonNameCandidate = data.addAnnotation("taxonNameCandidate", taxonNameCandidates[c].getStartIndex(), taxonNameCandidates[c].size());
//				taxonNameCandidate.copyAttributes(taxonNameCandidates[c]);
//				
//				if (this.isCandidateBroken(taxonNameCandidate, rankGroupNames, rankGroupSizes, requiredRankGroupNumbers, requiredRankNumbers)) {
//					if (DEBUG) System.out.println("Removing broken candidate: " + taxonNameCandidate.toXML());
//					data.removeAnnotation(taxonNameCandidate);
//				}
//				
//				else added++;
//			}
//			if (DEBUG) System.out.println(added + " new extended candidates added");
//		} while (added != 0);
//		
//		
//		//	annotate single epithets as taxon names
//		Annotation[] epithets = data.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++) {
//			if (this.isEpithetAbbreviated(epithets[e]))
//				continue;
//			
//			Annotation taxonNameCandidate = data.addAnnotation("taxonNameCandidate", epithets[e].getStartIndex(), epithets[e].size());
//			taxonNameCandidate.setAttribute("source", epithets[e].getAnnotationID());
//		}
//		if (DEBUG) System.out.println(epithets.length + " new one-epithet candidates added");
//		
//		
//		//	filter out candidates with abbreviated last epithet
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++)
//			if (this.isLastEpithetAbbreviated((QueriableAnnotation) taxonNameCandidates[c]))
//				data.removeAnnotation(taxonNameCandidates[c]);
	}
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		Annotation[] taxonNameCandidates;
//		int added;
//		int maxEpithets = OmniFAT.getRanks().length;
//		
//		//	join adjacent epithets to taxon names
//		do {
//			added = 0;
//			taxonNameCandidates = OmniFAT.joinAdjacent(data, "epithet", OmniFAT.getInTaxonNamePunctuationMarks(), "taxonNameCandidate");
//			for (int c = 0; c < taxonNameCandidates.length; c++) {
//				if (this.isLineBroken(taxonNameCandidates[c]))
//					continue;
//				
//				if (this.countEpithetsBySource(taxonNameCandidates[c]) > maxEpithets)
//					continue;
//				
//				QueriableAnnotation taxonNameCandidate = data.addAnnotation("taxonNameCandidate", taxonNameCandidates[c].getStartIndex(), taxonNameCandidates[c].size());
//				taxonNameCandidate.setAttribute("source", taxonNameCandidates[c].getAttribute("source"));
//				
//				if (this.isCandidateBroken(taxonNameCandidate))
//					data.removeAnnotation(taxonNameCandidate);
//				
//				else added++;
//			}
//			if (DEBUG) System.out.println(added + " new base candidates added");
//		} while (added != 0);
//		
//		//	join epithets to adjacent taxon names
//		do {
//			added = 0;
//			taxonNameCandidates = OmniFAT.joinAdjacent(data, "epithet", "taxonNameCandidate", OmniFAT.getInTaxonNamePunctuationMarks(), "taxonNameCandidate");
//			for (int c = 0; c < taxonNameCandidates.length; c++) {
//				if (this.isLineBroken(taxonNameCandidates[c]))
//					continue;
//				
//				if (this.countEpithetsBySource(taxonNameCandidates[c]) > maxEpithets)
//					continue;
//				
//				QueriableAnnotation taxonNameCandidate = data.addAnnotation("taxonNameCandidate", taxonNameCandidates[c].getStartIndex(), taxonNameCandidates[c].size());
//				taxonNameCandidate.setAttribute("source", taxonNameCandidates[c].getAttribute("source"));
//				
//				if (this.isCandidateBroken(taxonNameCandidate))
//					data.removeAnnotation(taxonNameCandidate);
//				
//				else added++;
//			}
//			if (DEBUG) System.out.println(added + " new extended candidates added");
//		} while (added != 0);
//		
//		
//		//	annotate single epithets as taxon names
//		Annotation[] epithets = data.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++) {
//			if (this.isEpithetAbbreviated(epithets[e]))
//				continue;
//			
//			Annotation taxonNameCandidate = data.addAnnotation("taxonNameCandidate", epithets[e].getStartIndex(), epithets[e].size());
//			taxonNameCandidate.setAttribute("source", epithets[e].getAnnotationID());
//		}
//		if (DEBUG) System.out.println(epithets.length + " new one-epithet candidates added");
//		
////		//	remove line broken candidates
////		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			for (int t = 0; t < (taxonNameCandidates[c].size()-1); t++)
////				if (taxonNameCandidates[c].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
////					t = taxonNameCandidates[c].size();
////					data.removeAnnotation(taxonNameCandidates[c]);
////				}
////		}
////		if (DEBUG) System.out.println("line-broken candidates removed");
////		
////		//	remove candidates whose last epithet is an abbreviation
////		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
////			epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
////			boolean lastEpithetAbbreviated = false;
////			for (int e = 0; e < epithets.length; e++)
////				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
////					String epithet = ((String) epithets[e].getAttribute("string"));
////					lastEpithetAbbreviated = ((epithet.length() < 4) && ((epithet.length() < 3) || epithet.endsWith(".")));
////				}
////			if (lastEpithetAbbreviated) {
//////				if (DEBUG) System.out.println("Removing for abbreviated last epithet: " + taxonNameCandidates[c].toXML());
//////				for (int E = 0; E < epithets.length; E++) {
//////					if (source.indexOf(epithets[E].getAnnotationID()) != -1)
//////						if (DEBUG) System.out.println("  " + epithets[E].toXML());
//////				}
////				data.removeAnnotation(taxonNameCandidates[c]);
////			}
////		}
////		if (DEBUG) System.out.println("candidates with abbreviated last epithet removed");
////		
////		//	remove candidates with improper epithet rank group order
////		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
////			epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
////			int lastRankGroupNumber = Integer.MAX_VALUE;
////			int rankGroupNumber;
////			for (int e = 0; e < epithets.length; e++)
////				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
////					rankGroupNumber = lastRankGroupNumber + 1;
////					try {
////						rankGroupNumber = Integer.parseInt((String) epithets[e].getAttribute("rankGroupNumber"));
////					} catch (NumberFormatException nfe) {}
////					
////					if (rankGroupNumber <= lastRankGroupNumber)
////						lastRankGroupNumber = rankGroupNumber;
////					else {
//////						if (DEBUG) System.out.println("Removing for improper epithet rank group order: " + taxonNameCandidates[c].toXML());
//////						for (int E = 0; E < epithets.length; E++) {
//////							if (source.indexOf(epithets[E].getAnnotationID()) != -1)
//////								if (DEBUG) System.out.println("  " + epithets[E].toXML());
//////						}
////						data.removeAnnotation(taxonNameCandidates[c]);
////						e = epithets.length;
////					}
////				}
////		}
////		if (DEBUG) System.out.println("candidates with improper rank group order removed");
////		
////		//	remove candidates with improper epithet rank order
////		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
////			epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
////			int lastRankNumber = Integer.MAX_VALUE;
////			int rankNumber;
////			for (int e = 0; e < epithets.length; e++)
////				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
////					rankNumber = lastRankNumber;
////					try {
////						rankNumber = Integer.parseInt((String) epithets[e].getAttribute("rankNumber", ""));
////					} catch (NumberFormatException nfe) {}
////					
////					if (rankNumber <= lastRankNumber)
////						lastRankNumber = rankNumber;
////					else {
//////						if (DEBUG) System.out.println("Removing for improper epithet rank order: " + taxonNameCandidates[c].toXML());
//////						for (int E = 0; E < epithets.length; E++) {
//////							if (source.indexOf(epithets[E].getAnnotationID()) != -1)
//////								if (DEBUG) System.out.println("  " + epithets[E].toXML());
//////						}
////						data.removeAnnotation(taxonNameCandidates[c]);
////						e = epithets.length;
////					}
////				}
////		}
////		if (DEBUG) System.out.println("candidates with improper rank order removed");
////		
////		
////		//	gather rank statistics
////		String[] rankGroups = OmniFAT.getRankGroups();
////		int[] rankCountsPrototype = new int[rankGroups.length];
////		for (int rg = 0; rg < rankGroups.length; rg++) {
////			String [] ranks = OmniFAT.getRanks(rankGroups[rg]);
////			rankCountsPrototype[rg] = ((ranks == null) ? 0 : ranks.length);
////		}
////		int[] rankCounts = new int[rankGroups.length];
////		
////		//	remove candidates with improper epithet rank counts
////		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			System.arraycopy(rankCountsPrototype, 0, rankCounts, 0, rankCounts.length);
////			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
////			epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
////			for (int e = 0; e < epithets.length; e++)
////				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
////					String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
////					for (int rg = 0; rg < rankGroups.length; rg++)
////						if (rankGroups[rg].equals(rankGroup)) {
////							rankCounts[rg]--;
////							if (rankCounts[rg] < 0) {
////								rankCounts[0] = Integer.MIN_VALUE;
////								rg = rankGroups.length;
////								e = epithets.length;
////							}
////						}
////				}
////			if (rankCounts[0] < 0) {
//////				if (DEBUG) System.out.println("Removing for improper epithet rank group counts: " + taxonNameCandidates[c].toXML());
//////				for (int E = 0; E < epithets.length; E++) {
//////					if (source.indexOf(epithets[E].getAnnotationID()) != -1)
//////						if (DEBUG) System.out.println("  " + epithets[E].toXML());
//////				}
////				data.removeAnnotation(taxonNameCandidates[c]);
////			}
////		}
////		if (DEBUG) System.out.println("candidates with improper rank counts removed");
////		
////		//	gather rank data
////		int speciesRankGroupNumber = OmniFAT.getRankGroupOrderNumber(OmniFAT.getRankGroup(SPECIES_ATTRIBUTE));
////		int speciesRankNumber = OmniFAT.getRankOrderNumber(SPECIES_ATTRIBUTE);
////		
////		//	remove candidates with missing epithets
////		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
////		for (int c = 0; c < taxonNameCandidates.length; c++) {
////			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
////			epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
////			boolean gotAboveSpeciesRank = false;
////			boolean gotSpeciesRank = false;
////			boolean gotBelowSpeciesRank = false;
////			for (int e = 0; e < epithets.length; e++)
////				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
////					String rank = ((String) epithets[e].getAttribute("rank"));
////					if (rank == null) {
////						int rankGroupNumber = OmniFAT.getRankGroupOrderNumber(((String) epithets[e].getAttribute("rankGroup")));
////						if (rankGroupNumber == speciesRankGroupNumber)
////							gotSpeciesRank = true;
////						else if (rankGroupNumber < speciesRankGroupNumber)
////							gotBelowSpeciesRank = true;
////						else if (rankGroupNumber > speciesRankGroupNumber)
////							gotAboveSpeciesRank = true;
////					}
////					else {
////						int rankNumber = OmniFAT.getRankOrderNumber(rank);
////						if (rankNumber == speciesRankNumber)
////							gotSpeciesRank = true;
////						else if (rankNumber < speciesRankNumber)
////							gotBelowSpeciesRank = true;
////						else if (rankNumber > speciesRankNumber)
////							gotAboveSpeciesRank = true;
////					}
////				}
////			if (gotAboveSpeciesRank && !gotSpeciesRank && gotBelowSpeciesRank) {
//////				if (DEBUG) System.out.println("Removing for missing species epithet: " + taxonNameCandidates[c].toXML());
//////				for (int e = 0; e < epithets.length; e++) {
//////					if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//////						if (DEBUG) System.out.println("  " + epithets[e].toXML());
//////				}
////				data.removeAnnotation(taxonNameCandidates[c]);
////			}
////		}
////		if (DEBUG) System.out.println("candidates with missing epithets removed");
//	}
	
//	private boolean isLineBroken(Annotation taxonNameCandidate) {
//		for (int t = 0; t < (taxonNameCandidate.size()-1); t++) {
//			if (taxonNameCandidate.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
//				return true;
//		}
//		return false;
//	}
//	
//	private int countEpithetsBySource(Annotation taxonNameCandidate) {
//		String source = ((String) taxonNameCandidate.getAttribute("source"));
//		if ((source == null) || (source.length() == 0))
//			return 0;
//		
//		int epithets = 1;
//		for (int c = 0; c < source.length(); c++)
//			if (source.charAt(c) == ',')
//				epithets++;
//		
//		return epithets;
//	}
//	
//	private boolean isCandidateBroken(QueriableAnnotation taxonNameCandidate, String[] rankGroupNames, int[] rankGroupSizes, Set requiredRankGroupNumbers, Set requiredRankNumbers) {
//		String source = ((String) taxonNameCandidate.getAttribute("source", ""));
//		Annotation[] epithets = taxonNameCandidate.getAnnotations("epithet");
//		ArrayList epithetList = new ArrayList();
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//				epithetList.add(epithets[e]);
//		epithets = ((Annotation[]) epithetList.toArray(new Annotation[epithetList.size()]));
//		
////		if (this.isLastEpithetAbbreviated(epithets)) {
////			
////		}
//		
//		if (this.isRankGroupOrderBroken(epithets)) {
//			StringBuffer rankGroupOrder = new StringBuffer();
//			for (int e = 0; e < epithets.length; e++)
//				rankGroupOrder.append("-" + epithets[e].getAttribute("rankGroup", "unknown"));
//			taxonNameCandidate.setAttribute("evidenceDetail", ("rankGroupOrder(" + rankGroupOrder.toString().substring(1) + ")"));
//			return true;
//		}
//		
//		if (this.isRankOrderBroken(epithets)) {
//			StringBuffer rankOrder = new StringBuffer();
//			for (int e = 0; e < epithets.length; e++)
//				rankOrder.append("-" + epithets[e].getAttribute("rank", "unknown"));
//			taxonNameCandidate.setAttribute("evidenceDetail", ("rankOrder(" + rankOrder.toString().substring(1) + ")"));
//			return true;
//		}
//		
//		if (this.isRankGroupSizeBroken(epithets, rankGroupNames, rankGroupSizes)) {
//			StringBuffer rankGroupOrder = new StringBuffer();
//			for (int e = 0; e < epithets.length; e++)
//				rankGroupOrder.append("-" + epithets[e].getAttribute("rankGroup", "unknown"));
//			taxonNameCandidate.setAttribute("evidenceDetail", ("rankGroupSize(" + rankGroupOrder.toString().substring(1) + ")"));
//			return true;
//		}
//		
//		if (this.isEpithetMissing(epithets, requiredRankGroupNumbers, requiredRankNumbers)) {
//			StringBuffer rankOrder = new StringBuffer();
//			for (int e = 0; e < epithets.length; e++)
//				rankOrder.append("-" + epithets[e].getAttribute("rank", "unknown"));
//			taxonNameCandidate.setAttribute("evidenceDetail", ("epithetMissing(" + rankOrder.toString().substring(1) + ")"));
//			return true;
//		}
//		
//		return false;
//	}
//	
//	private boolean isLastEpithetAbbreviated(QueriableAnnotation taxonNameCandidate) {
//		String source = ((String) taxonNameCandidate.getAttribute("source", ""));
//		Annotation[] epithets = taxonNameCandidate.getAnnotations("epithet");
//		boolean lastEpithetAbbreviated = false;
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//				lastEpithetAbbreviated = this.isEpithetAbbreviated(epithets[e]);
//		return lastEpithetAbbreviated;
//	}
//	
//	private boolean isLastEpithetAbbreviated(Annotation[] epithets) {
//		boolean lastEpithetAbbreviated = false;
//		for (int e = 0; e < epithets.length; e++)
//			lastEpithetAbbreviated = this.isEpithetAbbreviated(epithets[e]);
//		return lastEpithetAbbreviated;
//	}
//	
//	private boolean isEpithetAbbreviated(Annotation epithet) {
//		String epithetString = ((String) epithet.getAttribute("string"));
//		return ((epithetString.length() < 4) && ((epithetString.length() < 3) || epithetString.endsWith(".")));
//	}
//	
//	private boolean isRankGroupOrderBroken(Annotation[] epithets) {
//		int lastRankGroupNumber = Integer.MAX_VALUE;
//		int rankGroupNumber;
//		for (int e = 0; e < epithets.length; e++) {
//			rankGroupNumber = lastRankGroupNumber + 1;
//			try {
//				rankGroupNumber = Integer.parseInt((String) epithets[e].getAttribute("rankGroupNumber"));
//			} catch (NumberFormatException nfe) {}
//			
//			if (rankGroupNumber <= lastRankGroupNumber)
//				lastRankGroupNumber = rankGroupNumber;
//			else return true;
//		}
//		return false;
//	}
//	
//	private boolean isRankOrderBroken(Annotation[] epithets) {
//		int lastRankGroupNumber = Integer.MAX_VALUE;
//		int rankGroupNumber;
//		int maxRankNumber = Integer.MAX_VALUE;
//		int lastRankNumber = Integer.MAX_VALUE;
//		int rankNumber;
//		
//		for (int e = 0; e < epithets.length; e++) {
//			
//			rankGroupNumber = Integer.parseInt((String) epithets[e].getAttribute("rankGroupNumber", ""));
//			try {
//				rankNumber = Integer.parseInt((String) epithets[e].getAttribute("rankNumber", ""));
//			}
//			catch (NumberFormatException nfe) {
//				rankNumber = lastRankNumber-1;
//			}
//			
//			//	compare rank numbers of neighboring epithets (be graceful, though, since not all epithets have a rank so far)
//			if (rankNumber < lastRankNumber)
//				lastRankNumber = rankNumber;
//			else return true;
//			
//			//	compare rank number to maximum possible rank number
//			if (rankGroupNumber == lastRankGroupNumber) {
//				if (rankNumber <= maxRankNumber)
//					maxRankNumber--; // spend a rank
//				else return true;
//			}
//			else {
//				maxRankNumber = rankGroupNumber-1; // first number in current group is spent on current epithet
//				lastRankNumber = rankGroupNumber; // set last rank to be the one exactly before the current one
//				lastRankGroupNumber = rankGroupNumber;
//			}
//		}
//		return false;
//	}
//	
//	private boolean isRankGroupSizeBroken(Annotation[] epithets, String[] rankGroupNames, int[] rankGroupSizes) {
//		int[] rankGroupCounts = new int[rankGroupSizes.length];
//		System.arraycopy(rankGroupSizes, 0, rankGroupCounts, 0, rankGroupCounts.length);
//		
//		for (int e = 0; e < epithets.length; e++) {
//			String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//			for (int rg = 0; rg < rankGroupNames.length; rg++)
//				if (rankGroupNames[rg].equals(rankGroup)) {
//					rankGroupCounts[rg]--;
//					if (rankGroupCounts[rg] < 0) {
//						rankGroupCounts[0] = Integer.MIN_VALUE;
//						rg = rankGroupNames.length;
//						e = epithets.length;
//					}
//				}
//		}
//		return (rankGroupCounts[0] < 0);
//	}
//	
//	private boolean isEpithetMissing(Annotation[] epithets, Set requiredRankGroupNumbers, Set requiredRankNumbers) {
//		
//		//	collect epithet rank and rank group numbers
//		ArrayList epithetRankGroupNumbers = new ArrayList();
//		ArrayList epithetRankNumbers = new ArrayList();
//		for (int e = 0; e < epithets.length; e++) {
//			epithetRankGroupNumbers.add(new Integer((String) epithets[e].getAttribute("rankGroupNumber")));
//			String rankNumberString = ((String) epithets[e].getAttribute("rankNumber"));
//			if (rankNumberString != null)
//				epithetRankNumbers.add(new Integer(rankNumberString));
//		}
//		
//		//	got all the ranks
//		if (epithetRankNumbers.size() == epithets.length)
//			return this.isNumberMissing(epithetRankNumbers, requiredRankNumbers);
//		
//		//	ranks missing, check groups only
//		else return this.isNumberMissing(epithetRankGroupNumbers, requiredRankGroupNumbers);
//		
//		
////		boolean gotAboveSpeciesRank = false;
////		boolean gotSpeciesRank = false;
////		boolean gotBelowSpeciesRank = false;
////		for (int e = 0; e < epithets.length; e++) {
////			String rankNumberString = ((String) epithets[e].getAttribute("rankNumber"));
////			if (rankNumberString == null) {
////				int rankGroupNumber = Integer.parseInt(((String) epithets[e].getAttribute("rankGroupNumber")));
////				if (rankGroupNumber == this.speciesRankGroupNumber)
////					gotSpeciesRank = true;
////				else if (rankGroupNumber < this.speciesRankGroupNumber)
////					gotBelowSpeciesRank = true;
////				else if (rankGroupNumber > this.speciesRankGroupNumber)
////					gotAboveSpeciesRank = true;
////			}
////			else {
////				int rankNumber = Integer.parseInt(rankNumberString);
////				if (rankNumber == this.speciesRankNumber)
////					gotSpeciesRank = true;
////				else if (rankNumber < this.speciesRankNumber)
////					gotBelowSpeciesRank = true;
////				else if (rankNumber > this.speciesRankNumber)
////					gotAboveSpeciesRank = true;
////			}
////			
//////			String rank = ((String) epithets[e].getAttribute("rank"));
//////			if (rank == null) {
//////				int rankGroupNumber = OmniFAT.getRankGroupOrderNumber(((String) epithets[e].getAttribute("rankGroup")));
//////				if (rankGroupNumber == this.speciesRankGroupNumber)
//////					gotSpeciesRank = true;
//////				else if (rankGroupNumber < this.speciesRankGroupNumber)
//////					gotBelowSpeciesRank = true;
//////				else if (rankGroupNumber > this.speciesRankGroupNumber)
//////					gotAboveSpeciesRank = true;
//////			}
//////			else {
//////				int rankNumber = OmniFAT.getRankOrderNumber(rank);
//////				if (rankNumber == this.speciesRankNumber)
//////					gotSpeciesRank = true;
//////				else if (rankNumber < this.speciesRankNumber)
//////					gotBelowSpeciesRank = true;
//////				else if (rankNumber > this.speciesRankNumber)
//////					gotAboveSpeciesRank = true;
//////			}
////		}
////		return (gotAboveSpeciesRank && !gotSpeciesRank && gotBelowSpeciesRank);
//	}
//	
//	private boolean isNumberMissing(ArrayList numbers, Set requiredNumbers) {
//		TreeSet numberSet = new TreeSet(numbers);
//		int minNumber = ((Integer) numberSet.first()).intValue();
//		int maxNumber = ((Integer) numberSet.last()).intValue();
//		for (Iterator rnit = requiredNumbers.iterator(); rnit.hasNext();) {
//			Integer requiredNumber = ((Integer) rnit.next());
//			if ((minNumber < requiredNumber.intValue()) && (requiredNumber.intValue() < maxNumber) && !numberSet.contains(requiredNumber))
//				return true;
//		}
//		return false;
//	}
//	
//	private boolean isLastEpithetAbbreviated(QueriableAnnotation annotation) {
//		String source = ((String) annotation.getAttribute("source", ""));
//		Annotation[] epithets = annotation.getAnnotations("epithet");
//		boolean lastEpithetAbbreviated = false;
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//				lastEpithetAbbreviated = this.isEpithetAbbreviated(epithets[e]);
//		return lastEpithetAbbreviated;
//	}
//	
//	private boolean isEpithetAbbreviated(Annotation epithet) {
//		String epithetString = ((String) epithet.getAttribute("string"));
//		return ((epithetString.length() < 4) && ((epithetString.length() < 3) || epithetString.endsWith(".")));
//	}
//	
//	private boolean isRankGroupOrderBroken(QueriableAnnotation annotation) {
//		String source = ((String) annotation.getAttribute("source", ""));
//		Annotation[] epithets = annotation.getAnnotations("epithet");
//		int lastRankGroupNumber = Integer.MAX_VALUE;
//		int rankGroupNumber;
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//				rankGroupNumber = lastRankGroupNumber + 1;
//				try {
//					rankGroupNumber = Integer.parseInt((String) epithets[e].getAttribute("rankGroupNumber"));
//				} catch (NumberFormatException nfe) {}
//				
//				if (rankGroupNumber <= lastRankGroupNumber)
//					lastRankGroupNumber = rankGroupNumber;
//				else return true;
//			}
//		return false;
//	}
//	
//	private boolean isRankOrderBroken(QueriableAnnotation annotation) {
//		String source = ((String) annotation.getAttribute("source", ""));
//		Annotation[] epithets = annotation.getAnnotations("epithet");
//		int lastRankNumber = Integer.MAX_VALUE;
//		int rankNumber;
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//				rankNumber = lastRankNumber;
//				try {
//					rankNumber = Integer.parseInt((String) epithets[e].getAttribute("rankNumber", ""));
//				} catch (NumberFormatException nfe) {}
//				
//				if (rankNumber <= lastRankNumber)
//					lastRankNumber = rankNumber;
//				else return true;
//			}
//		return false;
//	}
//	
//	private boolean isRankGroupSizeBroken(QueriableAnnotation annotation) {
//		int[] rankGroupCounts = new int[this.rankGroupSizes.length];
//		System.arraycopy(this.rankGroupSizes, 0, rankGroupCounts, 0, rankGroupCounts.length);
//		
//		String source = ((String) annotation.getAttribute("source", ""));
//		Annotation[] epithets = annotation.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//				String rankGroup = ((String) epithets[e].getAttribute("rankGroup"));
//				for (int rg = 0; rg < this.rankGroups.length; rg++)
//					if (this.rankGroups[rg].equals(rankGroup)) {
//						rankGroupCounts[rg]--;
//						if (rankGroupCounts[rg] < 0) {
//							rankGroupCounts[0] = Integer.MIN_VALUE;
//							rg = this.rankGroups.length;
//							e = epithets.length;
//						}
//					}
//			}
//		return (rankGroupCounts[0] < 0);
//	}
//	
//	private boolean isEpithetMissing(QueriableAnnotation annotation) {
//		String source = ((String) annotation.getAttribute("source", ""));
//		Annotation[] epithets = annotation.getAnnotations("epithet");
//		boolean gotAboveSpeciesRank = false;
//		boolean gotSpeciesRank = false;
//		boolean gotBelowSpeciesRank = false;
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//				String rank = ((String) epithets[e].getAttribute("rank"));
//				if (rank == null) {
//					int rankGroupNumber = OmniFAT.getRankGroupOrderNumber(((String) epithets[e].getAttribute("rankGroup")));
//					if (rankGroupNumber == this.speciesRankGroupNumber)
//						gotSpeciesRank = true;
//					else if (rankGroupNumber < this.speciesRankGroupNumber)
//						gotBelowSpeciesRank = true;
//					else if (rankGroupNumber > this.speciesRankGroupNumber)
//						gotAboveSpeciesRank = true;
//				}
//				else {
//					int rankNumber = OmniFAT.getRankOrderNumber(rank);
//					if (rankNumber == this.speciesRankNumber)
//						gotSpeciesRank = true;
//					else if (rankNumber < this.speciesRankNumber)
//						gotBelowSpeciesRank = true;
//					else if (rankNumber > this.speciesRankNumber)
//						gotAboveSpeciesRank = true;
//				}
//			}
//		return (gotAboveSpeciesRank && !gotSpeciesRank && gotBelowSpeciesRank);
//	}
}