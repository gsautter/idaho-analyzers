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
 * @author sautter
 */
public class BlockWSS extends MetaWSS {
	
	protected WordScoreSource[] sources = new WordScoreSource[8];
	protected int[] sourceWeights = new int[8];
	protected int sourceCount = 0;
	
	protected int lowerLimit = 1;
	protected int upperLimit = 1;
	
	//	true --> combine scores, false --> combine votes
	protected boolean combineScores = true;
	
	/**	Constuctor for a BlockWSS without weight optimization
	 */
	public BlockWSS() {
		super();
		for (int i = 0; i < this.sourceWeights.length; i++) {
			this.sourceWeights[i] = this.lowerLimit;
		}
	}
	
	/**	Constructor for a BlockWSS using weight optimization
	 * @param lowerWeightLimit
	 * @param upperWeightLimit
	 */
	public BlockWSS(int lowerWeightLimit, int upperWeightLimit) {
		super();
		this.lowerLimit = lowerWeightLimit;
		this.upperLimit = upperWeightLimit;
		for (int i = 0; i < this.sourceWeights.length; i++) {
			this.sourceWeights[i] = this.lowerLimit;
		}
	}
	
	/**
	 * Creates instance from string representation
	 * @param id
	 * @param stringRepresentation
	 */
	public BlockWSS(int id, String stringRepresentation) {
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
			
			//	double weight array
			int[] wTemp = new int[this.sourceCount * 2];
			for (int i = 0; i < this.sourceCount; i++) {
				wTemp[i] = this.sourceWeights[i];
			}
			this.sourceWeights = wTemp;
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
		
		//	filter noise words
		if (this.noiseWords.containsIgnoreCase(string.trim())) return -100;
		
		//	collect singular scores
		int[] scores = this.getScores(string, this.combineScores);
		
		//	compute combined score and return it
		return this.combineScores(scores, this.sourceWeights);
	}
	
	//	compute the singular scores or votes from the several sources
	private int[] getScores(String string, boolean useScores) {
		int[] scores = new int[this.sourceCount];
		for (int i = 0; i < this.sourceCount; i++) {
			scores[i] = ((useScores) ? this.sources[i].getScore(string) : (this.sources[i].getVote(string, 101, 0, null, 0) * 100));
		}
		return scores;
	}
	
	/**	helper method to multiply two int arrays 
	 */
	protected int combineScores(int[] scores, int[] weights) {
		
		//	compute combined score
		int score = 0;
		int weightSum = 0;
		for (int i = 0; i < this.sourceCount; i++) {
			score += (scores[i] * weights[i]);
			weightSum += this.sourceWeights[i];
		}
		
		//	return score
		return ((weightSum > 0) ? (score / weightSum) : 0);
	}
	
	//	get the vote resulting from the specified score according to the actual uncertainty parameter values
	/*private int getVote(int score) {
		int finalScore = score + this.ucCorrector;
		return ((Math.abs(finalScore) < this.ucLimit) ? 0 : ((finalScore > 0) ? 1 : -1));
	}*/
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#optimize(de.idaho.util.StringVector, de.idaho.util.StringVector)
	 */
	public void optimize(StringVector positives, StringVector negatives, ResultEvaluator evaluator) {
		
		// Optimize all sources in this Block
		for (int i = 0; i < sourceCount; i++) {
			sources[i].optimize(positives, negatives, evaluator);
		}

		//	optimize only if there is space for optimization
		if ((this.lowerLimit < (this.upperLimit - 1)) || this.optimizeUncertainty || this.optimizeUncertaintyCorrector) {
			
			//	create and fill arrays to store singular scores and votes
			int[][] posSngScores = new int[positives.size()][this.sourceCount];
			int[][] posSngVotes = new int[positives.size()][this.sourceCount];
			for (int i = 0; i < positives.size(); i++) {
				posSngScores[i] = this.getScores(positives.get(i), true);
				posSngVotes[i] = this.getScores(positives.get(i), false); 
			}
			int[][] negSngScores = new int[negatives.size()][this.sourceCount];
			int[][] negSngVotes = new int[negatives.size()][this.sourceCount];
			for (int i = 0; i < negatives.size(); i++) {
				negSngScores[i] = this.getScores(negatives.get(i), true);
				negSngVotes[i] = this.getScores(negatives.get(i), false);
			}
			
			//	fill the arrays
			for (int s = 0; s < this.sourceCount; s++) {
				for (int i = 0; i < positives.size(); i++) {
					posSngScores[i][s] = this.sources[s].getScore(positives.get(i));
					posSngVotes[i][s] = (this.sources[s].getVote(positives.get(i), 101, 0, null, 0) * 100);
				}
				for (int i = 0; i < negatives.size(); i++) {
					negSngScores[i][s] = this.sources[s].getScore(negatives.get(i));
					negSngVotes[i][s] = (this.sources[s].getVote(negatives.get(i), 101, 0, null, 0) * 100);
				}
			}
			
			//	create array for test weights
			int[] test = new int[this.sourceCount];
			for (int i = 0; i < this.sourceCount; i++) {
				test[i] = this.lowerLimit;
			}
			
			int bestScore = 0;
			boolean finished = false;
			
			//	find best weight combination
			do {
				
				//	measures for distribution optimization on score combination
				int lowestPosS = 100;
				int highestNegS = -100;
				
				//	measures for distribution optimization on vote combination
				int lowestPosV = 100;
				int highestNegV = -100;
				
				//	create array to store scores from score combination
				int[] posScoresS = new int[positives.size()];
				int[] negScoresS = new int[negatives.size()];
				
				//	create array to store scores from vote combination
				int[] posScoresV = new int[positives.size()];
				int[] negScoresV = new int[negatives.size()];
				
				int score = 0;
				
				//	compute combined scores and find extremes
				for (int i = 0; i < positives.size(); i++) {
					score = this.combineScores(posSngScores[i], test);
					posScoresS[i] = score;
					if (score < lowestPosS) lowestPosS = score;
					score = this.combineScores(posSngVotes[i], test);
					posScoresV[i] = score;
					if (score < lowestPosV) lowestPosV = score;
				}
				lowestPosS --;	//	avoid <= for including lowestPos to uncertain
				lowestPosS = ((lowestPosS < 0) ? lowestPosS : 0);
				lowestPosV --;	//	avoid <= for including lowestPos to uncertain
				lowestPosV = ((lowestPosV < 0) ? lowestPosV : 0);
				
				for (int i = 0; i < negatives.size(); i++) {
					score = this.combineScores(negSngScores[i], test);
					negScoresS[i] = score;
					if (score > highestNegS) highestNegS = score;
					score = this.combineScores(negSngVotes[i], test);
					negScoresV[i] = score;
					if (score > highestNegV) highestNegV = score;
				}
				highestNegS++;	//	avoid >= for including highestNeg to uncertain
				highestNegS = ((highestNegS > 0) ? highestNegS : 0);
				highestNegV++;	//	avoid >= for including highestNeg to uncertain
				highestNegV = ((highestNegV > 0) ? highestNegV : 0);
				
				//	optimize uncertainty corrector only
				int corrector = ((this.optimizeUncertaintyCorrector) ? ((lowestPosS - lowestPosV - 3) / 4) : 0);
				do {
					
					//	assignment counters for score combination
					int pos2posS = 0;
					int pos2negS = 0;
					int pos2ucS = 0;
					int neg2posS = 0;
					int neg2negS = 0;
					int neg2ucS = 0;
					
					//	assignment counters for vote combination
					int pos2posV = 0;
					int pos2negV = 0;
					int pos2ucV = 0;
					int neg2posV = 0;
					int neg2negV = 0;
					int neg2ucV = 0;
					
					//	compute positives distribution
					for (int i = 0; i < posScoresS.length; i++) {
						
						//	for uncertainty optimization
						if (this.optimizeUncertainty) {
							
							//	for score combination
							if (posScoresS[i] > highestNegS) {
								pos2posS ++;
							} else if (posScoresS[i] < lowestPosS) {
								pos2negS ++;
							} else {
								pos2ucS ++;
							}
							
							//	for vote combination
							if (posScoresV[i] > highestNegV) {
								pos2posV ++;
							} else if (posScoresV[i] < lowestPosV) {
								pos2negV ++;
							} else {
								pos2ucV ++;
							}
							
						//	for fixed uncertainty
						} else {
							
							//	for score combination
							score = posScoresS[i] + corrector;
							if (score > this.ucLimit) {
								pos2posS ++;
							} else if (score < -this.ucLimit) {
								pos2negS ++;
							} else {
								pos2ucS ++;
							}
							
							//	for vote combination
							score = posScoresV[i] + corrector;
							if (score > this.ucLimit) {
								pos2posV ++;
							} else if (score < -this.ucLimit) {
								pos2negV ++;
							} else {
								pos2ucV ++;
							}
						}
					}
					
					//	compute negatives distribution
					for (int i = 0; i < negScoresS.length; i++) {
						
						//	for uncertainty optimization
						if (this.optimizeUncertainty) {
							
							//	for score combination
							if (negScoresS[i] > highestNegS) {
								neg2posS ++;
							} else if (negScoresS[i] < lowestPosS) {
								neg2negS ++;
							} else {
								neg2ucS ++;
							}
							
							//	for vote combination
							if (negScoresV[i] > highestNegV) {
								neg2posS ++;
							} else if (negScoresV[i] < lowestPosV) {
								neg2negV ++;
							} else {
								neg2ucV ++;
							}
							
						//	for fixed uncertainty
						} else {
							
							//	for score combination
							score = negScoresS[i] + corrector;
							if (score > this.ucLimit) {
								neg2posS ++;
							} else if (score < -this.ucLimit) {
								neg2negS ++;
							} else {
								neg2ucS ++;
							}
							
							//	for vote combination
							score = negScoresV[i] + corrector;
							if (score > this.ucLimit) {
								neg2posV ++;
							} else if (score < -this.ucLimit) {
								neg2negV ++;
							} else {
								neg2ucV ++;
							}
						}
					}
					//	compute decision quality
					int scoreS;
					int scoreV;
					if (evaluator == null) {
						scoreS = this.evaluateTest(pos2posS, pos2negS, pos2ucS, neg2posS, neg2negS, neg2ucS);
						scoreV = this.evaluateTest(pos2posV, pos2negV, pos2ucV, neg2posV, neg2negV, neg2ucV);
					} else {
						scoreS = evaluator.evaluateTest(pos2posS, pos2negS, pos2ucS, neg2posS, neg2negS, neg2ucS);
						scoreV = evaluator.evaluateTest(pos2posV, pos2negV, pos2ucV, neg2posV, neg2negV, neg2ucV);
					}
					score = ((scoreV > scoreS) ? scoreV : scoreS);
					//System.out.println("BlockWSS: " + concatArray(test) + " --> " + score);
					
					//	if best so far, store weight combination
					if (score > bestScore) {
						bestScore = score;
						for (int i = 0; i < this.sourceCount; i++) {
							this.sourceWeights[i] = test[i];
						}
						
						if (score == scoreS) {
							if (this.optimizeUncertainty) {
								this.ucLimit = (((highestNegS - lowestPosS) + 1) / 2);
								this.ucCorrector = -(highestNegS - this.ucLimit);
							} else if (this.optimizeUncertaintyCorrector) {
								this.ucCorrector = corrector;
							}
							this.combineScores = true;
						} else {
							if (this.optimizeUncertainty) {
								this.ucLimit = (((highestNegV - lowestPosV) + 1) / 2);
								this.ucCorrector = -(highestNegV - this.ucLimit);
							} else if (this.optimizeUncertaintyCorrector) {
								this.ucCorrector = corrector;
							}
							this.combineScores = false;
						}
					}
					
					//	increment corrector
					corrector += ((Math.abs(corrector) < 10) ? 1 : ((Math.abs(corrector) < 20) ? 2 : 3));
					
				} while (this.optimizeUncertaintyCorrector && (corrector < ((highestNegS + highestNegV + 3) / 4)));
				
				//	switch to next possible combination, break if all tested
				finished = incrementArray(test, this.lowerLimit, this.upperLimit);
			} while (!finished);
		}
	}
	
	/*private String concatArray(int[] ints) {
		StringBuffer ret = new StringBuffer("[");
		for (int i = 0; i < ints.length; i++) ret.append(((i == 0) ? "" : "|") + ints[i]);
		return ret.append("]").toString();
	}*/
	
	/** Increments the rightmost int in specified array by one and carries the increment left if specified upper limit is reached
	 * 	This method might be useful for self-optimization purposes 
	 * @param array	the array to be incremented 
	 * @param low	the lower limit for the ints contained in the array
	 * @param up	the upper limit for the ints contained in the array
	 * @return	true if and only if the leftmost int contained in the array has reached the upper limit, false otherwise
	 */
	protected boolean incrementArray(int[] array, int low, int up) {
		if (low == up) return true;
		int len = array.length-1;
		for (int i = len; i >= 0; i--) {
			array[i]++;
			if (array[i] == up) {
				array[i] = low;
			} else {
				return false;
			}
		}
		return true;
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetHelp()
	 */
	protected void resetHelp() {
		this.combineScores = true;
		
		//	reset weights
		for (int i = 0; i < this.sourceWeights.length; i++) {
			this.sourceWeights[i] = this.lowerLimit;
		}
		
		//	reset child WSSs
		for (int i = 0; i < sourceCount; i++) {
			sources[i].reset();
		}
	}

	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetHelp()
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
		sb.append("(Block UCL=");
		sb.append(this.ucLimit);
		sb.append(" UCC=");
		sb.append(this.ucCorrector);
		sb.append(" OptUC=");
		sb.append(this.optimizeUncertainty?"on":"off");
		sb.append(" lowerLimit=");
		sb.append(this.lowerLimit);
		sb.append(" upperLimit=");
		sb.append(this.upperLimit);
		sb.append(" CombineScores=");
		sb.append(this.combineScores?"on":"off");
		for (int i=0; i<this.sourceCount; i++) {
			sb.append(" Weight=");
			sb.append(this.sourceWeights[i]);
			sb.append(":");
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
		target.append(" 3 ");	// Type: block
		// Params
		target.append(this.ucLimit);
		target.append(" ");
		target.append(this.ucCorrector);
		target.append(" ");
		target.append(this.optimizeUncertainty?"1":"0");
		target.append(" ");
		target.append(this.lowerLimit);
		target.append(" ");
		target.append(this.upperLimit);
		target.append(" ");
		target.append(this.combineScores?"1":"0");
		// Element number and weights
		target.append(" ");
		target.append(this.sourceCount);
		for (int i=0; i<sourceCount; i++) {
			target.append(" ");
			target.append(this.sourceWeights[i]);
		}
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
	private BlockWSS loadSerialization(int id, String string) {
		// Read parameters
		StringTokenizer tokenizer = new StringTokenizer(string);
		int UCL = Integer.parseInt(tokenizer.nextToken());
		int UCC = Integer.parseInt(tokenizer.nextToken());
		int OPTUC = Integer.parseInt(tokenizer.nextToken());
		int LWR = Integer.parseInt(tokenizer.nextToken());
		int UPR = Integer.parseInt(tokenizer.nextToken());
		int COMBINE = Integer.parseInt(tokenizer.nextToken());

		System.out.println("Loaded BlockWSS params: UCL="+UCL+", UCC="+UCC+", OPTUC="+OPTUC+", LWR="+LWR+", UPR="+UPR+", COMBINE="+COMBINE);

		BlockWSS wss = new BlockWSS();
		wss.ucLimit = UCL;
		wss.ucCorrector = UCC;
		wss.optimizeUncertainty = (OPTUC==1) ? true : false;
		wss.lowerLimit = LWR;
		wss.upperLimit = UPR;
		wss.combineScores = (COMBINE==1) ? true : false;
		
		// Read number of elements
		int NUM = Integer.parseInt(tokenizer.nextToken());
		System.out.println("N="+NUM);

		if (NUM == 0) {	// No elements
			System.out.println("[Warning] Stored an empty block?");
			return wss;
		}
		
		int[] weights = new int[NUM];
		System.out.print("Weights: ");
		for (int i=0; i<NUM; i++) {
			weights[i] = Integer.parseInt(tokenizer.nextToken());
			System.out.print(" "+weights[i]);
		}
		System.out.println("");
		
		// Create elements
		// Find the first "(" in the string
		int begin = 0;
		for (int i=0; i<NUM; i++) {
			System.out.println("Loading block element #"+i);
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
		
		// Write weights (the array has the right size now, after adding the wss-es)
		for (int i=0; i<NUM; i++) {
			wss.sourceWeights[i] = weights[i];
		}

		System.out.println("BlockWSS ID="+id+" loaded.");
		return wss;
	}

}
