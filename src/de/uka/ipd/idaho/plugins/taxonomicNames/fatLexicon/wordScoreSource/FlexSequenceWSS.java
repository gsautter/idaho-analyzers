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


import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 * TODO document this class
 */
public class FlexSequenceWSS extends WordScoreSource {
	
	private int length = 1;
	private int maxLength;
	private int scoreMode = -1;
	private boolean optimizeScoreMode = true;
	
	private WSSStatistics stat;
	private WSSStatistics[] stats;
	
	/**	Constructor
	 * @param	maxLength	the maximum sequence length to be used
	 */
	public FlexSequenceWSS(int maxLength) {
		super();
		this.maxLength = maxLength;
		this.stats = new WSSStatistics[this.maxLength];
		for (int i = 0; i < this.maxLength; i++) {
			this.stats[i] = new WSSStatistics(i+1);
		}
		this.stat = this.stats[0];
	}

	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#remember(java.lang.String, int, boolean, boolean)
	 */
	public void remember(String string, int weight, boolean isPositive, boolean isSecure) {
		if (isPositive) {
			if (isSecure) this.knownPositives.addElementIgnoreDuplicates(string);
			this.positives.addElementIgnoreDuplicates(string);
		} else {
			if (isSecure) this.knownNegatives.addElementIgnoreDuplicates(string);
			this.negatives.addElementIgnoreDuplicates(string);
		}
		for (int l = 1; l <= this.maxLength; l++) {
			for (int i = 0; i <= (string.length() - l); i++) {
				String seq = string.substring(i, (i + l));
				this.stats[l-1].add(seq, weight, isPositive);
			}
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#remember(java.lang.String, double, boolean, boolean)
	 */
	public void remember(String string, double factor, boolean isPositive, boolean isSecure) {
		if (isPositive) {
			if (isSecure) this.knownPositives.addElementIgnoreDuplicates(string);
			this.positives.addElementIgnoreDuplicates(string);
		} else {
			if (isSecure) this.knownNegatives.addElementIgnoreDuplicates(string);
			this.negatives.addElementIgnoreDuplicates(string);
		}
		for (int l = 1; l <= this.maxLength; l++) {
			for (int i = 0; i <= (string.length() - l); i++) {
				String seq = string.substring(i, (i + l));
				this.stats[l-1].multiply(seq, factor, isPositive);
			}
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#commit()
	 */
	protected void commit() {
		for (int i = 0; i < this.stats.length; i++) {
			this.stats[i].commitUpdates();
		}
	}
	
	/**	set the mode scores for singular sequences shall be combined
	 * @param scoreMode	the new ScoreMode
	 * 	use the following values:
	 * 	-2	find best score mode autonomously on optimization
	 * 	-1	for frequency addition
	 * 	0	for Markov chain
	 * 	>0	for Markov chain with smothed factors, the value will be used for smothing
	 * 	the smoothing is done the following way: Let p be the Markov factor of a sequence seq,
	 * 	then it is smoothed to ((p + (0.5 * (scoreMode / 100))) / (1 + (scoreMode / 100)))
	 * 	This avoids 0-scores for word at least one subsequence of which is unknown
	 */
	public void setScoreMode(int scoreMode) {
		if (scoreMode >= -1) {
			this.scoreMode = scoreMode;
			this.optimizeScoreMode = false;
		} else if (scoreMode == -2) {
			this.optimizeScoreMode = true;
			this.optimize();
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#getScore(java.lang.String)
	 */
	public int getScore(String string) {
		return this.getScore(string, this.scoreMode);
	}
	
	//	compute the score of the specified String according to the specified mode
	private int getScore(String string, int scoreMode) {
		//	filter noise words
		if (this.noiseWords.containsIgnoreCase(string.trim())) return -100;
		
		//	compute score
		double posScore = 0;
		double negScore = 0;
		if (string.length() >= this.length) {
			
			//	compute score in additive mode
			if (scoreMode == -1) {
				for (int i = 0; i <= (string.length() - this.length); i++) {
					String seq = string.substring(i, (i + this.length));
					posScore += this.stat.getFactor(seq, true);
					negScore += this.stat.getFactor(seq, false);
				}
				
			//	compute score in (smoothed) Markov mode
			} else {
				double smoothingFactor = (((double) scoreMode) / 100);
				String seq = string.substring(0, this.length);
				posScore = (((0.5 * smoothingFactor) + this.stat.getFactor(seq, true)) / (smoothingFactor + 1));
				negScore = (((0.5 * smoothingFactor) + this.stat.getFactor(seq, false)) / (smoothingFactor + 1));
				for (int i = 0; i <= (string.length() - this.length); i++) {
					seq = string.substring(i, (i+this.length));
					posScore *= (((0.5 * smoothingFactor) + this.stat.getMarkovFactor(seq, true)) / (smoothingFactor + 1));
					negScore *= (((0.5 * smoothingFactor) + this.stat.getMarkovFactor(seq, false)) / (smoothingFactor + 1));
				}
			}
		}
		
		//	unify and return score
		double sum = posScore + negScore;
		int score = ((sum > 0) ? ((int)(100 * ((posScore - negScore) / sum))) : 0);
		return score;
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#optimize(de.idaho.util.StringVector, de.idaho.util.StringVector)
	 */
	public void optimize(StringVector positives, StringVector negatives, ResultEvaluator evaluator) {
		
		int bestScore = 0;
		int chosenLength = this.length;
		
		for (int len = 1; len <= this.maxLength; len++) {
			
			this.stat = this.stats[len-1];
			this.length = len;
			
			//	optimize score mode (and uncertainty parameters if required)
			if (this.optimizeScoreMode) {
				
				//	test the several modes (-1, 0, 1, 2, 4, 8, 16, 32, 64, 128)
				for (int mod = -1; mod < 129; mod += ((mod < 2) ? 1 : mod)) {
					
					//	run test
					int score = this.optimizeUncertainty(positives, negatives, evaluator, mod);
					
					//	store the result if it's best so far
					if (score > bestScore) {
						this.scoreMode = mod;
						chosenLength = len;
						bestScore = score;
					}
					
				}
				
			//	optimize uncertainty parameters only
			} else {
				
				//	run test
				int score = this.optimizeUncertainty(positives, negatives, evaluator, this.scoreMode);
				
				//	store the result if it's best so far
				if (score > bestScore) {
					chosenLength = len;
					bestScore = score;
				}
			}
		}
		
		//	remember best performing length
		this.length = chosenLength;
		this.stat = this.stats[this.length -1 ];
	}
	
	//	optimize the uncertainty parameters with the specified score mode, return the achieved score
	private int optimizeUncertainty(StringVector positives, StringVector negatives, ResultEvaluator evaluator, int scoreMode) {
		
		//	measures for distribution optimization
		int lowestPos = 100;
		int highestNeg = -100;
		
		//	create array to store scores
		int[] posScores = new int[positives.size()];
		int[] negScores = new int[negatives.size()];
		
		int score = 0;
		
		//	compute combined scores and find extremes
		for (int i = 0; i < positives.size(); i++) {
			score = this.getScore(positives.get(i), scoreMode);
			posScores[i] = score;
			if (score < lowestPos) lowestPos = score;
		}
		lowestPos --;	//	avoid <= for including lowestPos to uncertain
		lowestPos = ((lowestPos < 0) ? lowestPos : 0);
		
		for (int i = 0; i < negatives.size(); i++) {
			score = this.getScore(negatives.get(i), scoreMode);
			negScores[i] = score;
			if (score > highestNeg) highestNeg = score;
		}
		highestNeg++;	//	avoid >= for including highestNeg to uncertain
		highestNeg = ((highestNeg > 0) ? highestNeg : 0);
		
		//	optimize corrector
		int bestScore = 0;
		int corrector = ((this.optimizeUncertaintyCorrector) ? ((lowestPos - 1) / 2) : 0);
		
		do {
			
			//	assignment counters
			int pos2pos = 0;
			int pos2neg = 0;
			int pos2uc = 0;
			int neg2pos = 0;
			int neg2neg = 0;
			int neg2uc = 0;
			
			//	compute positives distribution
			for (int i = 0; i < posScores.length; i++) {
				
				//	for uncertainty optimization
				if (this.optimizeUncertainty) {
					if (posScores[i] > highestNeg) {
						pos2pos ++;
					} else if (posScores[i] < lowestPos) {
						pos2neg ++;
					} else {
						pos2uc ++;
					}
					
				//	for fixed uncertainty
				} else {
					score = posScores[i] + corrector;
					if (score > this.ucLimit) {
						pos2pos ++;
					} else if (score < -this.ucLimit) {
						pos2neg ++;
					} else {
						pos2uc ++;
					}
				}
			}
			
			//	compute negatives distribution
			for (int i = 0; i < negScores.length; i++) {
				
				//	for uncertainty optimization
				if (this.optimizeUncertainty) {
					if (negScores[i] > highestNeg) {
						neg2pos ++;
					} else if (negScores[i] < lowestPos) {
						neg2neg ++;
					} else {
						neg2uc ++;
					}
					
				//	for fixed uncertainty
				} else {
					score = negScores[i] + corrector;
					if (score > this.ucLimit) {
						neg2pos ++;
					} else if (score < -this.ucLimit) {
						neg2neg ++;
					} else {
						neg2uc ++;
					}
				}
			}
			//	compute decision quality
			if (evaluator == null) {
				score = this.evaluateTest(pos2pos, pos2neg, pos2uc, neg2pos, neg2neg, neg2uc);
			} else {
				score = evaluator.evaluateTest(pos2pos, pos2neg, pos2uc, neg2pos, neg2neg, neg2uc);
			}
			
			//	compute and store limits if required
			if (this.optimizeUncertainty) {
				this.ucLimit = (((highestNeg - lowestPos) + 1) / 2);
				this.ucCorrector = -(highestNeg - this.ucLimit);
				bestScore = score;
			} else if (this.optimizeUncertaintyCorrector && (score > bestScore)) {
				this.ucCorrector = corrector;
				bestScore = score;
			} else if (score > bestScore) {
				bestScore = score;
			}
			
			//	increment corrector
			corrector += ((Math.abs(corrector) < 10) ? 1 : ((Math.abs(corrector) < 20) ? 2 : 3));
			
		} while (this.optimizeUncertaintyCorrector && (corrector < ((highestNeg + 1) / 2)));
		
		//	return the achieved score
		return bestScore;
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetHelp()
	 */
	protected void resetHelp() {
		this.scoreMode = -1;
		this.optimizeScoreMode = true;
		this.length = 1;
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetStatisticsHelp()
	 */
	protected void resetStatisticsHelp() {
		for (int i = 0; i < this.maxLength; i++) {
			this.stats[i].clear(true);
			this.stats[i].clear(false);
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("(SequenceWSS UCL=");
		sb.append(this.ucLimit);
		sb.append(" UCC=");
		sb.append(this.ucCorrector);
		sb.append(" OptUC=");
		sb.append(this.optimizeUncertainty?"on":"off");
		sb.append(" LEN=");
		sb.append(this.length);
		sb.append(" ScoreMode=");
		sb.append(this.scoreMode);
		sb.append(" OptScoreMode=");
		sb.append(this.optimizeScoreMode?"on":"off");
		sb.append(")");
		return sb.toString();
	}
	

}
