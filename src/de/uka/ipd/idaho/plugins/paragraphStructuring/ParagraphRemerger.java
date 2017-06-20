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

package de.uka.ipd.idaho.plugins.paragraphStructuring;


import java.util.Properties;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class ParagraphRemerger extends AbstractAnalyzer {
	
	private StringVector noiseWords = Gamta.getNoiseWords();
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	if only one paragraph, nothing to do
		if (paragraphs.length < 2) return;
		
		//	collect in line word pairs
		StringVector wordPairs = new StringVector();
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			for (int t = 1; t < paragraph.size(); t++) {
				if (Gamta.isWord(paragraph.tokenAt(t-1)) && 
					Gamta.isWord(paragraph.tokenAt(t)) &&
					!this.noiseWords.contains(paragraph.valueAt(t-1)) &&
					!this.noiseWords.contains(paragraph.valueAt(t)) &&
					!paragraph.tokenAt(t-1).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)
					) wordPairs.addElementIgnoreDuplicates(paragraph.valueAt(t-1) + " " + paragraph.valueAt(t));
			}
		}
		
		//	check individual paragraphs
		MutableAnnotation paragraph = paragraphs[0];
		MutableAnnotation lastParagraph;
		for (int p = 1; p < paragraphs.length; p++) {
			lastParagraph = paragraph;
			paragraph = paragraphs[p];
			boolean merge = false;
			
			//	check for devided words
			if (this.isWordDevided(lastParagraph.lastToken(), paragraph.firstToken()))
				merge = true;
			
			//	check for continued sentence
			if (!merge && !this.isSentenceEnd(lastParagraph) && (lastParagraph.size() > 5) && this.isSentenceContinued(paragraph))
				merge = true;
			
			//	join known word pairs devided by paragraph border
			if (!merge && wordPairs.contains(lastParagraph.lastValue() + " " + paragraph.firstValue()))
				merge = true;
			
			//	join paragraphs if first one ends with noise word in lower case (very unlikely to be section title)
			if (!merge && !this.noiseWords.contains(lastParagraph.lastValue()) && (lastParagraph.lastValue().length() > 1))
				merge = true;
			
			//	merge paragraphs if one or more criteria applied
			if (merge) {
				MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, lastParagraph.getStartIndex(), (paragraph.getEndIndex() - lastParagraph.getStartIndex()));
				
				newParagraph.copyAttributes(paragraph);
				data.removeAnnotation(paragraph);
				
				newParagraph.copyAttributes(lastParagraph);
				data.removeAnnotation(lastParagraph);
				
				paragraph = newParagraph;
			}
		}
	}
	
	/**	determine whether two Tokens represent a devided word
	 * @param	token1	the first Token
	 * @param	token2	the second Token
	 * @return true if and only if the specified Tokens represent a devided word
	 */
	private boolean isWordDevided(Token token1, Token token2) {
		if (token1.length() < 2) return false;
		String value1 = token1.getValue();
		return (this.isWordContinued(token2) && value1.endsWith("-") && (Gamta.LOWER_CASE_LETTERS.indexOf(value1.charAt(value1.length() - 2)) != -1));
	}
	
	/** check if a Token can be the continuation of a devided word
	 * @param	token	the Token to check
	 * @return true if and only if the specified Token can be the continuation of a devided word
	 */
	private boolean isWordContinued(Token token) {
		return (Gamta.isWord(token) && !token.equals("and") && !token.equals("or"));
	}
	
	/**	determine weather a given paragraph continues a sentence
	 * @param	paragraph	a TokenSequence containing the paragraph to check
	 * @return true if and only if the specified paragraph is considered to continue a sentence
	 */
	private boolean isSentenceContinued(TokenSequence paragraph) {
		//	catch blank lines
		if ((paragraph == null) || (paragraph.size() == 0)) return true;
		
		//	check if foreign brackets closed
		if (closesUnopenedBrackets(paragraph)) return true;
		
		//	check first Token
		return isSentenceContinuation(paragraph.firstToken());
	}
	
	/** check if a Token can be the continuation of a sentence
	 * @param	token	the Token to check
	 * @return true if and only if the specified Token can be the continuation of a sentence
	 */
	private boolean isSentenceContinuation(Token token) {
		return ((Gamta.LOWER_CASE_LETTERS.indexOf(token.getValue().charAt(0)) != -1) || Gamta.isOpeningBracket(token));
	}
	
	/**	determine weather a given paragraph ends with the end of a sentence
	 * @param	paragraph	a TokenSequence containing the paragraph to check
	 * @return true if and only if the specified paragraph is considered to end with the end of a sentence
	 */
	private boolean isSentenceEnd(TokenSequence paragraph) {
		
		//	check if brackets remain open
		if (leavesBracketsOpen(paragraph)) return false;
		
		//	check ending punctuation
		if (Gamta.isSentenceEnd(paragraph.lastToken())) return true;
		
		//	check single-tokened lines
		if (paragraph.size() < 2) return false;
		
		//	check ending punctuation before ending quotas
		return (Gamta.isSentenceEnd(paragraph.tokenAt(paragraph.size() - 2)) && (paragraph.lastToken().equals("'") || paragraph.lastToken().equals("\"")));
	}
	
	/**	determine weather a given paragraph opens brackets which it does not close
	 * @param	paragraph	a TokenSequence containing the paragraph to check
	 * @return true if and only if the specified paragraph opens brackets which it does not close
	 */
	private boolean leavesBracketsOpen(TokenSequence paragraph) {
		
		//	check if brackets remain open
		Token lastOpenBracket = null;
		Stack openBrackets = new Stack();
		for (int t = 0; t < paragraph.size(); t++) {
			Token token = paragraph.tokenAt(t);
			if (Gamta.isOpeningBracket(token)) {
				openBrackets.push(token);
				lastOpenBracket = token;
			} else if (Gamta.closes(token, lastOpenBracket)) {
				openBrackets.pop();
				lastOpenBracket = ((openBrackets.size() == 0) ? null : ((Token) openBrackets.peek()));
			}
		}
		return (openBrackets.size() != 0);
	}
	
	/**	determine weather a given paragraph closes brackets which it does not open
	 * @param	paragraph	a TokenSequence containing the paragraph to check
	 * @return true if and only if the specified paragraph closes brackets which it does not open
	 */
	private boolean closesUnopenedBrackets(TokenSequence paragraph) {
		//	catch blank lines
		if (paragraph == null) return false;
		
		//	check if brackets remain open
		Token lastClosingBracket = null;
		Stack closedBrackets = new Stack();
		for (int t = paragraph.size(); t > 0; t--) {
			Token token = paragraph.tokenAt(t - 1);
			if (Gamta.isClosingBracket(token)) {
				closedBrackets.push(token);
				lastClosingBracket = token;
			} else if (Gamta.opens(token, lastClosingBracket)) {
				closedBrackets.pop();
				lastClosingBracket = ((closedBrackets.size() == 0) ? null : ((Token) closedBrackets.peek()));
			}
		}
		return (closedBrackets.size() != 0);
	}
}
