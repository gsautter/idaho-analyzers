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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource;


import java.util.StringTokenizer;

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This is to implement cascade of WSS-es.
 * @author kristof
 */
public class CascadeWSS extends MetaWSS {
	
	// Array of WSS-es. (If full, will be doubled.)
	protected WordScoreSource[] sources = new WordScoreSource[8];
	protected int sourceCount = 0;
	
	// Set always by evaluateWSS
	private AssignmentListCollector lastPosAssignments = null;
	private AssignmentListCollector lastNegAssignments = null;
	
	/**
	 * Creates an empty cascade.
	 */
	public CascadeWSS() {
		super();
	}

	/**
	 * Creates the cascade with the given scorers added to it.
	 * @param scorers
	 */
	public CascadeWSS(WordScoreSource[] scorers) {
		super(scorers);
	}
	
	/**
	 * Creates instance from string representation
	 * @param id
	 * @param stringRepresentation
	 */
	public CascadeWSS(int id, String stringRepresentation) {
		super();
		loadSerialization(id, stringRepresentation);		
	}	


	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.MetaWSS#addWSS(de.idaho.documentAnalyzer.postProcessor.specNameFinder.WordScoreSource)
	 */
	public void addWSS(WordScoreSource wss) {
		
		//	if source array full, double it's size
		if (this.sourceCount == this.sources.length) {
			
			//	double source array
			WordScoreSource[] sTemp = new WordScoreSource[this.sourceCount * 2];
			for (int i = 0; i < this.sourceCount; i++) {
				sTemp[i] = this.sources[i];
			}
			this.sources = sTemp;
		}
		
		//	add new WSS and increment source count
		this.sources[this.sourceCount] = wss;
		this.sourceCount ++;
	}

	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#remember(java.lang.String, int, boolean, boolean)
	 */
	public void remember(String string, int weight, boolean isPositive, boolean isSecure) {
		for (int i = 0; i < this.sourceCount; i++) {
			this.sources[i].remember(string, weight, isPositive, isSecure);
		}
		if (isPositive) {
			if (isSecure) this.knownPositives.addElementIgnoreDuplicates(string);
			this.positives.addElementIgnoreDuplicates(string);
		} else {
			if (isSecure) this.knownNegatives.addElementIgnoreDuplicates(string);
			this.negatives.addElementIgnoreDuplicates(string);
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#remember(java.lang.String, double, boolean, boolean)
	 */
	public void remember(String string, double factor, boolean isPositive, boolean isSecure) {
		for (int i = 0; i < this.sourceCount; i++) {
			this.sources[i].remember(string, factor, isPositive, isSecure);
		}
		if (isPositive) {
			if (isSecure) this.knownPositives.addElementIgnoreDuplicates(string);
			this.positives.addElementIgnoreDuplicates(string);
		} else {
			if (isSecure) this.knownNegatives.addElementIgnoreDuplicates(string);
			this.negatives.addElementIgnoreDuplicates(string);
		}
	}

	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#commit()
	 */
	protected void commit() {
		for (int i = 0; i < this.sourceCount; i++) {
			this.sources[i].commit();
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#getScore(java.lang.String)
	 */
	public int getScore(String string) {
		return (100 * this.getVote(string, 101, 0, null, 0));
	}

	// TODO Use one vote method (override that...)
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#getVote(java.lang.String, int, int, de.idaho.documentAnalyzer.postProcessor.specNameFinder.FeedbackSource, int)
	 */
	public int getVote(String string, int experianceThreshold, int experianceWeight, FeedbackSource feedbackSource, int feedbackWeight) {
		//	filter noise words
		if (this.noiseWords.containsIgnoreCase(string.trim())) return -1;
		
		//	In a cascade we ask for votes and not scores
		//	No feedback is provided here and noone should experiance.
		int wasSure = -1;
		for (int sourceIndex = 0; sourceIndex < sourceCount; sourceIndex++) {
			int ass = sources[sourceIndex].getVote(string, 101, 0, null, 0);
			if (ass != 0) {
				wasSure = sourceIndex;
				// Experiance
				// TODO Can experianceTreshold be applied here?!
				int currentExpWeight = experianceWeight;
				if (wasSure > -1) {
					for (int i = wasSure; i >= 0; i --) {
						sources[i].remember(string, experianceWeight, ((ass == 1) ? true : false), false);
						currentExpWeight /= 2;
					}
				}
				return ass;
			}
		}
		
		// If there wasn't a certain vote, ask for feedback
		int vote = 0;
		if (feedbackSource != null) {
			boolean isPositive = feedbackSource.getFeedback(string);
			vote = (isPositive ? 1 : -1);
			this.remember(string, feedbackWeight, isPositive, true);
		}

		return vote;
	}

	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#getVote(java.lang.String, int, double, de.idaho.documentAnalyzer.postProcessor.specNameFinder.FeedbackSource, double)
	 */
	public int getVote(String string, int experianceThreshold, double experianceWeight, FeedbackSource feedbackSource, double feedbackWeight) {
		//	filter noise words
		if (this.noiseWords.containsIgnoreCase(string.trim())) return -1;
		
		//	In a cascade we ask for votes and not scores
		//	No feedback is provided here and noone should experiance.
		int wasSure = -1;
		for (int sourceIndex = 0; sourceIndex < sourceCount; sourceIndex++) {
			int ass = sources[sourceIndex].getVote(string, 101, 0, null, 0);
			if (ass != 0) {
				wasSure = sourceIndex;
				// Experiance
				double currentExpWeight = experianceWeight;
				if (wasSure>-1) {
					for (int i = wasSure; i >= 0; i --) {
						sources[i].remember(string, experianceWeight, ((ass == 1) ? true : false), false);
						// weight is not allowed to get under 1
						currentExpWeight = 1 + (currentExpWeight-1) / 2.0;
					}
				}
				return ass;
			}
		}
		
		// If there wasn't a certain vote, ask for feedback
		int vote = 0;
		if (feedbackSource != null) {
			boolean isPositive = feedbackSource.getFeedback(string);
			vote = (isPositive ? 1 : -1);
			this.remember(string, feedbackWeight, isPositive, true);
		}

		return vote;
	}

	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#optimize(de.idaho.util.StringVector, de.idaho.util.StringVector)
	 */
	public void optimize(StringVector positives, StringVector negatives, ResultEvaluator evaluator) {
		System.out.println("Cascade order optimization started");
		
		int[] optimizedPosition = new int[ sourceCount ];
		StringVector currentPosTestSet = positives;
		StringVector currentNegTestSet = negatives;
		
		for (int i=0; i<sourceCount; i++)
			optimizedPosition[i]=-1;	// Nothing is placed yet

		// Build optimized cascade
		for (int round = 0; round < sourceCount; round++) {
			
			System.out.println("Optimalization round: #"+round);
			
			// Select WSS with best result
			int bestResult = -1000;
			int bestIndex = -1;
			AssignmentListCollector bestResultPos = null;
			AssignmentListCollector bestResultNeg = null;
			System.out.print("Evaluating:");
			
			for (int wssIndex = 0; wssIndex < sourceCount; wssIndex++) {
				if (optimizedPosition[wssIndex]!=-1) continue;	// Already placed
				System.out.print(" "+wssIndex);
				
				int roundNum = sourceCount;

				// Linear
				int falsePosPenalty = roundNum-round+5;	// Lin. decreasing bis 5
				int falseNegPenalty = roundNum-round+5;	// Lin. decreasing bis 5
				int ucPenalty = round;	// Lin. increasing
				
				ResultEvaluator currentEvaluator = new WeightedExFMeasure(falsePosPenalty, falseNegPenalty, ucPenalty, ucPenalty); 
				sources[wssIndex].optimize(positives,negatives,currentEvaluator);
				int result = evaluateWSS(sources[wssIndex], currentPosTestSet, currentNegTestSet, currentEvaluator);
				
				System.out.print("("+result+")");
				if (result > bestResult) {
					System.out.print("!");
					bestResult = result;
					bestIndex = wssIndex;
					bestResultPos = lastPosAssignments;
					bestResultNeg = lastNegAssignments;
				}
			}
			System.out.println(" OK");
			
			// Set "optimizedPosition" of that WSS to "round"
			System.out.println("Best WSS (#"+bestIndex+") is placed at position "+round);
			optimizedPosition[bestIndex]=round;
			
			// Now use the selected source to produce the training sets for the next round
			double TP = Math.round( (double)bestResultPos.posList.size() / (double)currentPosTestSet.size() * 10000.0 ) / 100.0;
			double FP = Math.round( (double)bestResultPos.negList.size() / (double)currentPosTestSet.size() * 10000.0 ) / 100.0;
			double UP = Math.round( (double)bestResultPos.ucList.size() / (double)currentPosTestSet.size() * 10000.0 ) / 100.0;
			double TN = Math.round( (double)bestResultNeg.negList.size() / (double)currentNegTestSet.size() * 10000.0 ) / 100.0;
			double FN = Math.round( (double)bestResultNeg.posList.size() / (double)currentNegTestSet.size() * 10000.0 ) / 100.0;
			double UN = Math.round( (double)bestResultNeg.ucList.size() / (double)currentNegTestSet.size() * 10000.0 ) / 100.0;
			
			System.out.println(">>> Evaluation of new element (#"+bestIndex+"): TP:"+TP+"%, FP:"+FP+"%, UP:"+UP+"%, TN:"+TN+"%, FN:"+FN+"%, UN:"+UN+"%");
			currentPosTestSet = bestResultPos.ucList;
			currentNegTestSet = bestResultNeg.ucList;
			System.out.println("Remaining uncertainties: "+currentPosTestSet.size()+"/"+currentNegTestSet.size());
		}
		
		// Saving optimized opsitions
		WordScoreSource[] newScoreSourceList = new WordScoreSource[sourceCount];
		for (int wssIndex = 0; wssIndex < sourceCount; wssIndex++)
			newScoreSourceList[ optimizedPosition[wssIndex] ] = sources[wssIndex];
		sources = newScoreSourceList;
		System.out.println("Order optimalization complete.");
	}
	
	//	helper method for cascade optimization
	private int evaluateWSS(WordScoreSource wss, StringVector positives, StringVector negatives, ResultEvaluator evaluator) {
		this.lastPosAssignments = new AssignmentListCollector();
		this.lastNegAssignments = new AssignmentListCollector();
		
		for (int i=0; i<positives.size(); i++) {
			int vote = wss.getVote(positives.get(i),101,0,null,0);
			lastPosAssignments.addToList(positives.get(i),vote);
		}
		
		for (int i=0; i<negatives.size(); i++) {
			int vote = wss.getVote(negatives.get(i),101,0,null,0);
			lastNegAssignments.addToList(negatives.get(i),vote);
		}
		
		return evaluator.evaluateTest(lastPosAssignments,lastNegAssignments);
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetHelp()
	 */
	protected void resetHelp() {
		
		//	reset child WSSs
		for (int i = 0; i < sourceCount; i++) {
			sources[i].reset();
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetStatisticsHelp()
	 */
	protected void resetStatisticsHelp() {
		
		//	reset child WSSs
		for (int i = 0; i < sourceCount; i++) {
			sources[i].resetStatistics();
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("(Cascade");
		for (int i=0; i<this.sourceCount; i++) {
			sb.append(" ");
			sb.append(this.sources[i].toString());
		}
		sb.append(")");
		return sb.toString();
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#getSerialization(int, StringBuffer)
	 */
	public int getSerialization(int firstFreeID, StringBuffer target) {
		target.append("(");
		// ID and type
		target.append(firstFreeID);	// ID
		target.append(" 4 ");	// Type: cascade
		// Params (none)
		// Element number
		target.append(" ");
		target.append(this.sourceCount);
		// Elements
		int freeID = firstFreeID+1;
		for (int i=0; i<sourceCount; i++) {
			target.append(" ");
			freeID = this.sources[i].getSerialization(freeID,target);
		}
		target.append(")");
		return freeID;
	}
	
	/**
	 * Creates an instance from the given string representation.
	 * @param id	ID of this WSS.
	 * @param string	String representation _without_ the brackets.
	 * @return	The new instance.
	 */
	private CascadeWSS loadSerialization(int id, String string) {
		// Read parameters
		StringTokenizer tokenizer = new StringTokenizer(string);
		
		CascadeWSS wss = new CascadeWSS();
		
		// Read number of elements
		int NUM = Integer.parseInt(tokenizer.nextToken());
		System.out.println("Loaded CascadeWSS N="+NUM);

		if (NUM == 0) {	// No elements
			System.out.println("[Warning] Stored an empty cascade?");
			return wss;
		}
		
		// Create elements
		// Find the first "(" in the string
		int begin = 0;
		for (int i=0; i<NUM; i++) {
			System.out.println("Loading cascade element #"+i);
			begin = string.indexOf("(",begin)+1;
			// find end of definition of this element (wss)
			int bracketDepth = 1;
			int currentIndex = begin+1;
			while (bracketDepth>0) {
				switch (string.charAt(currentIndex)) {
					case '(':
						bracketDepth++;
						break;
					case ')':
						bracketDepth--;
						break;
				}
				currentIndex++;
			}
			int end = currentIndex-1;	// currentIndex points after the ")"
			String representation = string.substring(begin,end);
			System.out.println("String representation: ["+representation+"]");
			addWSS(LoadedWSSCreator.createWSS(representation));
			
			begin = currentIndex;
		}

		System.out.println("CascadeWSS ID="+id+" loaded.");
		return wss;
	}

}
