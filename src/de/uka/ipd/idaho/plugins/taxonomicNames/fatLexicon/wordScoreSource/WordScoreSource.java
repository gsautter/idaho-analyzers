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


import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public abstract class WordScoreSource {
	
	protected StringVector noiseWords = Gamta.getNoiseWords();
	
	protected StringVector knownPositives = new StringVector(false);
	protected StringVector knownNegatives = new StringVector(false);
	
	protected StringVector positives = new StringVector(false);
	protected StringVector negatives = new StringVector(false);
	
	protected boolean optimizeUncertainty = true;
	protected boolean optimizeUncertaintyCorrector = false;
	protected int ucLimit = 0;
	protected int ucCorrector = 0;
	
	/**	Constructor
	 */
	protected WordScoreSource() {
	}
	
	/**	add a String to this WSS' statistics additively
	 * @param	string		the String to be added
	 * @param	weight		the weight to add the specified String with
	 * @param	isPositive	add the specified String to the positive or the negative statistics?
	 * @param	isSecure	was the specified String classified by a user, or by the WSS itself? 
	 */
	public abstract void remember(String string, int weight, boolean isPositive, boolean isSecure);
	
	/**	add a set of Strings to this WSS' statistics additively
	 * @param	strings		the StringVector containing the Strings to be added
	 * @param	weight		the weight to add the specified Strings with
	 * @param	isPositive	add the specified Strings to the positive or the negative statistics?
	 * @param	isSecure	was the specified String classified by a user, or by the WSS itself? 
	 */
	public void remember(StringVector strings, int weight, boolean isPositive, boolean isSecure) {
		for (int i = 0; i < strings.size(); i++) {
			this.remember(strings.get(i), weight, isPositive, isSecure);
			if (isPositive) {
				if (isSecure) this.knownPositives.addElementIgnoreDuplicates(strings.get(i));
				this.positives.addElementIgnoreDuplicates(strings.get(i));
			} else {
				if (isSecure) this.knownNegatives.addElementIgnoreDuplicates(strings.get(i));
				this.negatives.addElementIgnoreDuplicates(strings.get(i));
			}
		}
	}
	
	/**	add a String to this WSS' statistics incrementally
	 * @param	string		the String to be added
	 * @param	factor		the incrementation factor to multiply the count of the specified String with
	 * @param	isPositive	add the specified String to the positive or the negative statistics?
	 * @param	isSecure	was the specified String classified by a user, or by the WSS itself? 
	 */
	public abstract void remember(String string, double factor, boolean isPositive, boolean isSecure);
	
	/**	add a set of Strings to this WSS' statistics incrementally
	 * @param	strings		the StringVector containing the Strings to be added
	 * @param	factor		the incrementation factor to multiply the count of the specified Strings with
	 * @param	isPositive	add the specified Strings to the positive or the negative statistics?
	 * @param	isSecure	was the specified String classified by a user, or by the WSS itself? 
	 */
	public void remember(StringVector strings, double factor, boolean isPositive, boolean isSecure) {
		for (int i = 0; i < strings.size(); i++) {
			this.remember(strings.get(i), factor, isPositive, isSecure);
			if (isPositive) {
				if (isSecure) this.knownPositives.addElementIgnoreDuplicates(strings.get(i));
				this.positives.addElementIgnoreDuplicates(strings.get(i));
			} else {
				if (isSecure) this.knownNegatives.addElementIgnoreDuplicates(strings.get(i));
				this.negatives.addElementIgnoreDuplicates(strings.get(i));
			}
		}
	}
	
	/**	train this WSS with a set of positives and a set of negatives, with a subsequent optimization
	 * @param	positives	the StringVector containig the positives to be learned
	 * @param	negatives	the StringVector containig the negatives to be learned
	 */
	public void train(StringVector positives, StringVector negatives) {
		this.remember(positives, 1, true, true);
		this.remember(negatives, 1, false, true);
		this.commit();
		this.optimize();
	}
	
	/**	commit remembered words into statistics
	 */
	protected abstract void commit();
	
	/**	get the raw score for a string, range [-100, 100]
	 * @param	string	the String to get the score for
	 * @return	the score for the specified String, unmodified by any correctors
	 */
	public abstract int getScore(String string);
	
	/**	decide if the specified String is a positive or a negative
	 * If experiance and feedback is performed, uses additive methods.
	 * @param	string					the String to be classified
	 * @param	experianceThreshold		the minimum absolute value of the modified score for auto-learning
	 * @param	experianceWeight		the weight for auto-learning
	 * @param	feedbackSource			the FeedbackSource to ask for it's vote if no secure decision made
	 * @param	feedbackWeight			the weight for learning from feedback
	 * @return	-1, 1 or 0, depending on if the specified String was classified a negative, a positive, or if no decision was made
	 */
	public int getVote(String string, int experianceThreshold, int experianceWeight, FeedbackSource feedbackSource, int feedbackWeight) {
		return this.getVote(string, experianceThreshold, 0, experianceWeight, feedbackSource, 0, feedbackWeight);
	}

	/**	decide if the specified String is a positive or a negative
	 * If experiance and feedback is performed, uses multiplicative methods.
	 * @param	string					the String to be classified
	 * @param	experianceThreshold		the minimum absolute value of the modified score for auto-learning
	 * @param	experianceWeight		the weight for auto-learning
	 * @param	feedbackSource			the FeedbackSource to ask for it's vote if no secure decision made
	 * @param	feedbackWeight			the weight for learning from feedback
	 * @return	-1, 1 or 0, depending on if the specified String was classified a negative, a positive, or if no decision was made
	 */
	public int getVote(String string, int experianceThreshold, double experianceWeight, FeedbackSource feedbackSource, double feedbackWeight) {
		return this.getVote(string, experianceThreshold, experianceWeight, 0, feedbackSource, feedbackWeight, 0);
	}
	
	/**	getVote method with the union of the two public ones as parameters
	 * @param 	string
	 * @param 	experianceThreshold
	 * @param 	experianceMult
	 * @param 	experianceAdd
	 * @param 	feedbackSource
	 * @param 	feedbackMult
	 * @param 	feedbackAdd
	 * @return	-1, 1 or 0, depending on if the specified String was classified a negative, a positive, or if no decision was made
	 */
	protected int getVote(String string, int experianceThreshold, double experianceMult, int experianceAdd, FeedbackSource feedbackSource, double feedbackMult, int feedbackAdd) {
		
		//	filter known words
		if (this.noiseWords.containsIgnoreCase(string.trim())) return -1;
		if (this.positives.containsIgnoreCase(string.trim())) return 1;
		if (this.negatives.containsIgnoreCase(string.trim())) return -1;
		
		//	get score
		int score = this.getScore(string);
		int finalScore = score + this.ucCorrector;
		int vote = ((Math.abs(finalScore) < this.ucLimit) ? 0 : ((finalScore > 0) ? 1 : -1));
		
		//	learn from decision if it's secure enough
		if (Math.abs(finalScore * vote) > experianceThreshold) {
			if (experianceAdd > 0) {
				this.remember(string, experianceAdd, (vote > 0), false);
			} else if (experianceMult > 0) {
				this.remember(string, experianceMult, (vote > 0), false);
			}
		}
			
		//	if no decision at all, get feedback if possible
		if ((vote == 0) && (feedbackSource != null)) {
			boolean isPositive = feedbackSource.getFeedback(string);
			vote = (isPositive ? 1 : -1);
			if (feedbackAdd > 0) {
				this.remember(string, feedbackAdd, (vote > 0), true);
			} else if (experianceMult > 0) {
				this.remember(string, feedbackMult, (vote > 0), true);
			}
		}
		return vote;
	}
	
	/**	decide if the specified String is a positive or a negative
	 * If experiance and feedback is performed, uses additive methods.
	 * @param	string					the String to be classified
	 * @param	experianceThreshold		the minimum absolute value of the modified score for auto-learning
	 * @param	experianceWeight		the weight for auto-learning
	 * @param	feedbackSource			the FeedbackSource to ask for it's vote if no secure decision made
	 * @param	feedbackWeight			the weight for learning from feedback
	 * @return	-1, 1 or 0, depending on if the specified String was classified a negative, a positive, or if no decision was made
	 */
	public int getVote(String[] strings, int experianceThreshold, int experianceWeight, FeedbackSource feedbackSource, int feedbackWeight) {
		return this.getVote(strings, experianceThreshold, 0, experianceWeight, feedbackSource, 0, feedbackWeight);
	}

	/**	decide if the specified String is a positive or a negative
	 * If experiance and feedback is performed, uses multiplicative methods.
	 * @param	string					the String to be classified
	 * @param	experianceThreshold		the minimum absolute value of the modified score for auto-learning
	 * @param	experianceWeight		the weight for auto-learning
	 * @param	feedbackSource			the FeedbackSource to ask for it's vote if no secure decision made
	 * @param	feedbackWeight			the weight for learning from feedback
	 * @return	-1, 1 or 0, depending on if the specified String was classified a negative, a positive, or if no decision was made
	 */
	public int getVote(String[] strings, int experianceThreshold, double experianceWeight, FeedbackSource feedbackSource, double feedbackWeight) {
		return this.getVote(strings, experianceThreshold, experianceWeight, 0, feedbackSource, feedbackWeight, 0);
	}
	
	/**	getVote method with the union of the two public ones as parameters
	 * @param 	string
	 * @param 	experianceThreshold
	 * @param 	experianceMult
	 * @param 	experianceAdd
	 * @param 	feedbackSource
	 * @param 	feedbackMult
	 * @param 	feedbackAdd
	 * @return	-1, 1 or 0, depending on if the specified String was classified a negative, a positive, or if no decision was made
	 */
	protected int getVote(String[] strings, int experianceThreshold, double experianceMult, int experianceAdd, FeedbackSource feedbackSource, double feedbackMult, int feedbackAdd) {
		
		int scoreSum = 0;
		for (int i = 0; i < strings.length; i++) {
			String string = strings[i];
			
			//	filter known words
			if (this.noiseWords.containsIgnoreCase(string.trim())) return -1;
			if (this.positives.containsIgnoreCase(string.trim())) scoreSum += 100;
			if (this.negatives.containsIgnoreCase(string.trim())) scoreSum -= 100;
			
			scoreSum += this.getScore(string);
		}
		
		//	get score
		int score = (scoreSum / strings.length);
		int finalScore = score + this.ucCorrector;
		//int vote = ((Math.abs(finalScore) < this.ucLimit) ? 0 : ((finalScore > 0) ? 1 : -1));
		int vote = this.getVote(score);
		
		//	learn from decision if it's secure enough
		if (Math.abs(finalScore * vote) > experianceThreshold) {
			for (int i = 0; i < strings.length; i++) {
				String string = strings[i];
				if (experianceAdd > 0) {
					this.remember(string, experianceAdd, (vote > 0), false);
				} else if (experianceMult > 0) {
					this.remember(string, experianceMult, (vote > 0), false);
				}
			}
		}
			
		//	if no decision at all, get feedback if possible
		if ((vote == 0) && (feedbackSource != null)) {
			StringBuffer assembler = new StringBuffer();
			for (int i = 0; i < strings.length; i++) assembler.append(((i == 0) ? "" : " ") + strings[i]);
			boolean isPositive = feedbackSource.getFeedback(assembler.toString());
			vote = (isPositive ? 1 : -1);
			for (int i = 0; i < strings.length; i++) {
				String string = strings[i];
				if (feedbackAdd > 0) {
					this.remember(string, feedbackAdd, (vote > 0), true);
				} else if (experianceMult > 0) {
					this.remember(string, feedbackMult, (vote > 0), true);
				}
			}
		}
		return vote;
	}
	
	/**	compute the vote from a score according to this WSS's current configuration
	 * @param	score	the score to judge
	 * @return the vote for the specified score
	 */
	public int getVote(int score) {
		int finalScore = score + this.ucCorrector;
		return ((Math.abs(finalScore) < this.ucLimit) ? 0 : ((finalScore > 0) ? 1 : -1));
	}
	
	/**	set the limit that a score must exceed to be thought of as certain enough to make a vote
	 * @param	ucLimit		the value to set the limit to
	 * 	Note: Setting the limit to a value <= 0 causes the WSS to perform some optimizations:
	 * 	< 0		the limit is set to the absolute of the specified value, the corrector is optimized
	 * 	= 0		both limit and corrector are optimized
	 *  > 0		the limit is set to the specified value, the corrector is set to 0
	 */
	public void setUncertaintyLimit(int ucLimit) {
		if (ucLimit < 0) {
			this.ucLimit = -ucLimit;
			this.optimizeUncertainty = false;
			this.optimizeUncertaintyCorrector = true;
			this.optimize();
		} else if (ucLimit == 0) {
			this.optimizeUncertainty = true;
			this.optimizeUncertaintyCorrector = false;
			this.optimize();
		} else {
			this.ucLimit = ucLimit;
			this.ucCorrector = 0;
			this.optimizeUncertainty = false;
			this.optimizeUncertaintyCorrector = false;
		}
	}
	
	/**	run an optimization using the input and experiance gathered so far
	 */
	public void optimize() {
		this.performKnowledgeRevision();
		this.optimize(this.positives, this.negatives, null);
		this.commit();
	}
	
	/**	run an optimization using the input and experiance gathered so far
	 */
	public void optimize(ResultEvaluator evaluator) {
		this.performKnowledgeRevision();
		this.optimize(this.positives, this.negatives, evaluator);
		this.commit();
	}
	
	/**	run an optimization using the specified sets of positives and negatives
	 * @param	positives	the StringVector containig the positives to optimize on
	 * @param	negatives	the StringVector containig the negatives to optimize on
	 */
	public abstract void optimize(StringVector positives, StringVector negatives, ResultEvaluator evaluator);
	
	/**	check the self learned negatives and positives
	 */
	public void performKnowledgeRevision() {
		
		//System.out.println("Knowledge Revision:");
		
		int score = 0;
		
		//	find lowest secure positive
		int lowestPositive = 100;
		for (int i = 0; i < this.knownPositives.size(); i++) {
			score = this.getScore(this.knownPositives.get(i));
			if (score < lowestPositive) {
				lowestPositive = score;
				//System.out.println("Lowest Positive for " + this.knownPositives.get(i) + ": " + lowestPositive);
			}
		}
		//System.out.println("Lowest Positive: " + lowestPositive);
		lowestPositive /= 2;
		
		//	find highest secure positive
		int highestNegative = -100;
		for (int i = 0; i < this.knownNegatives.size(); i++) {
			score = this.getScore(this.knownNegatives.get(i));
			if (score > highestNegative) {
				highestNegative = score;
				//System.out.println("Highest Negative for " + this.knownNegatives.get(i) + ": " + highestNegative);
			}
		}
		//System.out.println("Highest Negative: " + highestNegative);
		highestNegative /= 2;
		
		//	resort self learned knowledge
		int index = 0;
		while (index < this.positives.size()) {
			if (!this.knownPositives.containsIgnoreCase(this.positives.get(index))) {
				String s = this.positives.get(index);
				score = this.getScore(s);
				if (score < lowestPositive) {
					this.negatives.addElementIgnoreDuplicates(s);
					this.positives.remove(s);
					//System.out.println("  " + s + " transfered to negatives");
				} else {
					index++;
				}
			} else {
				index++;
			}
		}
		
		index = 0;
		while (index < this.negatives.size()) {
			if (!this.knownNegatives.containsIgnoreCase(this.negatives.get(index))) {
				String s = this.negatives.get(index);
				score = this.getScore(s);
				if (score > highestNegative) {
					this.positives.addElementIgnoreDuplicates(s);
					this.negatives.remove(s);
					//System.out.println("  " + s + " transfered to positives");
				} else {
					index++;
				}
			} else {
				index++;
			}
		}
		
		
	}
	
	/**	reset the WordScoreSource, clear all statistics, etc
	 */
	public void reset() {
		this.optimizeUncertainty = true;
		this.optimizeUncertaintyCorrector = false;
		this.ucLimit = 0;
		this.ucCorrector = 0;
		this.resetHelp();
		this.resetStatistics();
	}
	
	/**	the part of the clearing that affects subclass specific structures
	 */
	protected abstract void resetHelp();
	
	/**	reset the WordScoreSources statistics
	 */
	public void resetStatistics() {
		this.positives.clear();
		this.negatives.clear();
		this.knownPositives.clear();
		this.knownNegatives.clear();
		this.resetStatisticsHelp();
	}
	
	/**	the part of the statistics reset that affects subclass specific structures
	 */
	protected abstract void resetStatisticsHelp();
	
	/**	compute the extended FMeasure (ExFMeasure) of the specivied values
	 * @param pos2pos	true positives count
	 * @param pos2neg	false positives count
	 * @param pos2uc	undecided positives count
	 * @param neg2pos	false negatives count
	 * @param neg2neg	true negatives count
	 * @param neg2uc	undecided negatives count
	 * @return			the extended FMeasure (ExFMeasure) of the specivied values
	 */
	protected int evaluateTest(int pos2pos, int pos2neg, int pos2uc, int neg2pos, int neg2neg, int neg2uc) {
		int certain_sumPos = pos2pos + pos2neg + pos2uc;
		int certain_sumNeg = neg2pos + neg2neg + neg2uc;
		
		int p = ((certain_sumNeg > 0) ? ((100*neg2neg)/certain_sumNeg) : 0);
		int r = ((certain_sumPos > 0) ? ((100*pos2pos)/certain_sumPos) : 0);
		if ((p > 0) || (r > 0)) {
			return ((2 * (p * r)) / (p + r));
		} else {
			return 0;
		}
	}
	
	/** Returns string description of the WSS.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("(DefaultWSS UCL=");
		sb.append(this.ucLimit);
		sb.append(" UCC=");
		sb.append(this.ucCorrector);
		sb.append(" OptUC=");
		sb.append(this.optimizeUncertainty?"on":"off");
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Writes the string representation of this WSS into target.
	 * @param firstFreeID	The first available ID this WSS can use.
	 * @param target	The target to write the string representation to.
	 * @return	The new first available (free) ID
	 */
	public int getSerialization(int firstFreeID, StringBuffer target) {	// Just to show how to do it...
		target.append("(");
		target.append(firstFreeID);	// ID
		target.append(" 0 ");	// Type: abstract default
		target.append(this.ucLimit);
		target.append(" ");
		target.append(this.ucCorrector);
		target.append(" ");
		target.append(this.optimizeUncertainty?"1":"0");
		target.append(")");
		return firstFreeID+1;
	}
}
