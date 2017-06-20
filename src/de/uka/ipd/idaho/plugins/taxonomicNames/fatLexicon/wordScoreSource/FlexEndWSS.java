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
public class FlexEndWSS extends WordScoreSource {

	private int length = 2;
	private int maxLength;
	
	private WSSStatistics stat;
	private WSSStatistics[] stats;
	
	/**
	 * 
	 */
	public FlexEndWSS(int maxLength) {
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
		for (int l = 2; l <= this.maxLength; l++) {
			if (string.length() >= l) {
				String seq = string.substring(string.length() - l);
				this.stats[l-1].add(seq, weight, isPositive);
			}
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
		for (int l = 2; l <= this.maxLength; l++) {
			if (string.length() >= l) {
				String seq = string.substring(string.length() - l);
				this.stats[l-1].multiply(seq, factor, isPositive);
			}
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
		for (int i = 0; i < this.stats.length; i++) {
			this.stats[i].commitUpdates();
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#getScore(java.lang.String)
	 */
	public int getScore(String string) {
		
		//	filter noise words
		if (this.noiseWords.containsIgnoreCase(string.trim())) return -100;
		
		//	compute and retun score
		double posScore = 0;
		double negScore = 0;
		if (string.length() >= this.length) {
			String seq = string.substring(string.length() - this.length);
			posScore += this.stat.getFactor(seq,true);
			negScore += this.stat.getFactor(seq,false);
		}
		int score = ((int)(100 * (posScore - negScore)));
		
		//	learn if decision secure enough
		return score;
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#optimize(de.idaho.util.StringVector, de.idaho.util.StringVector)
	 */
	public void optimize(StringVector positives, StringVector negatives, ResultEvaluator evaluator) {
		
		int bestLenScore = 0;
		int chosenLength = this.length;
		
		for (int len = 2; len <= this.maxLength; len++) {
			
			this.stat = this.stats[len-1];
			this.length = len;
			
			//	optimize complete uncertainty parameters
			if (this.optimizeUncertainty) {
				int lowestPos = 100;
				int highestNeg = -100;
				
				for (int i = 0; i < positives.size(); i++) {
					int scr = this.getScore(positives.get(i));
					if (scr < lowestPos) lowestPos = scr;
				}
				lowestPos --;	//	avoid <= for including lowestPos to uncertain
				lowestPos = ((lowestPos < 0) ? lowestPos : 0);
				
				for (int i = 0; i < negatives.size(); i++) {
					int scr = this.getScore(negatives.get(i));
					if (scr > highestNeg) highestNeg = scr;
				}
				highestNeg++;	//	avoid >= for including highestNeg to uncertain
				highestNeg = ((highestNeg > 0) ? highestNeg : 0);
				
				this.ucLimit = (((highestNeg - lowestPos) + 1) / 2);
				this.ucCorrector = -(highestNeg - ucLimit);
				
			//	optimize uncertainty corrector only
			} else if (this.optimizeUncertaintyCorrector) {
				
				//	measures for distribution optimization
				int lowestPos = 100;
				int highestNeg = -100;
				
				//	create array to store scores
				int[] posScores = new int[positives.size()];
				int[] negScores = new int[negatives.size()];
				
				int score = 0;
				
				//	compute combined scores and find extremes
				for (int i = 0; i < positives.size(); i++) {
					score = this.getScore(positives.get(i));
					posScores[i] = score;
					if (score < lowestPos) lowestPos = score;
				}
				
				for (int i = 0; i < negatives.size(); i++) {
					score = this.getScore(negatives.get(i));
					negScores[i] = score;
					if (score > highestNeg) highestNeg = score;
				}
				
				//	find best corrector
				int bestScore = 0;
				for (int corrector = ((lowestPos - 1) / 2); corrector < ((highestNeg + 1) / 2); corrector += ((Math.abs(corrector) < 10) ? 1 : ((Math.abs(corrector) < 20) ? 2 : 3))) {
					
					//	assignment counters
					int pos2pos = 0;
					int pos2neg = 0;
					int pos2uc = 0;
					int neg2pos = 0;
					int neg2neg = 0;
					int neg2uc = 0;
					
					//	compute positives distribution
					for (int i = 0; i < posScores.length; i++) {
						score = posScores[i] + corrector;
						if (score > this.ucLimit) {
							pos2pos ++;
						} else if (score < -this.ucLimit) {
							pos2neg ++;
						} else {
							pos2uc ++;
						}
					}
					
					//	compute negatives distribution
					for (int i = 0; i < negScores.length; i++) {
						score = negScores[i] + corrector;
						if (score > this.ucLimit) {
							neg2pos ++;
						} else if (score < -this.ucLimit) {
							neg2neg ++;
						} else {
							neg2uc ++;
						}
					}
					//	compute decision quality
					if (evaluator == null) {
						score = this.evaluateTest(pos2pos, pos2neg, pos2uc, neg2pos, neg2neg, neg2uc);
					} else {
						score = evaluator.evaluateTest(pos2pos, pos2neg, pos2uc, neg2pos, neg2neg, neg2uc);
					}
					
					if (score > bestScore) {
						this.ucCorrector = corrector;
						bestScore = score;
						if (score > bestLenScore) {
							bestLenScore = score;
							chosenLength = len;
						}
					}
				}
			}
		}
		
		//	remember best parameters
		this.length = chosenLength;
		this.stat = this.stats[this.length - 1];
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.WordScoreSource#resetHelp()
	 */
	protected void resetHelp() {
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
		sb.append("(EndWSS UCL=");
		sb.append(this.ucLimit);
		sb.append(" UCC=");
		sb.append(this.ucCorrector);
		sb.append(" OptUC=");
		sb.append(this.optimizeUncertainty?"on":"off");
		sb.append(" LEN=");
		sb.append(this.length);
		sb.append(")");
		return sb.toString();
	}
}
