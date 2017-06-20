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

package de.uka.ipd.idaho.plugins.pageNormalization;


import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class FootnoteFinder extends AbstractAnalyzer implements LiteratureConstants {
	
	private static final int MAX_FOOTNOTE_LINES = 4;
	private static final String CITATION_PART_STRING = "p;pp;fig;figs";
	
	private StringVector noiseWords;
	private StringVector citationParts = new StringVector(false);
	
	/**	Constructor
	 */
	public FootnoteFinder() {
		super();
		this.noiseWords = Gamta.getNoiseWords();
		this.citationParts.parseAndAddElements(CITATION_PART_STRING, ";");
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		Annotation[] pageBorders = data.getAnnotations(PAGE_BORDER_TYPE);
		
		//	if page borders not identified, footnotes cannot be found
		if (pageBorders.length == 0) return;
		
		int pageBorderIndex = 0;
		int lastBorderStartIndex = -1;
		int nextBorderStartIndex = pageBorders[pageBorderIndex].getStartIndex();
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	check individual paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			
			int lineCount = 0;
			int charCount = 0;
			
			//	search footnotes only directly before page borders
			if (paragraph.getStartIndex() == nextBorderStartIndex) {
				
				//	check paragraphs backward from page border
				int pIndex = (p - 1);
				while (pIndex != -1) {
					paragraph = paragraphs[pIndex];
					
					//	previous page border reached
					if (paragraph.getStartIndex() == lastBorderStartIndex) pIndex = -1;
					
					//	check if paragraph is footnote
					else {
						
						//	check number of lines
						int pLineCount = 0;
						for (int t = 0; t < paragraph.size(); t++)
							if (paragraph.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) pLineCount++;
						if (pLineCount <= MAX_FOOTNOTE_LINES) {
							
							//	compute average line length
							pLineCount = 0;
							int pCharCount = 0;
							int lineLength = 0;
							for (int t = 0; t < (paragraph.size() - 1); t++) {
								Token token = paragraph.tokenAt(t);
								lineLength += token.length();
								if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
									pLineCount++;
									pCharCount += lineLength;
									lineLength = 0;
								}
							}
							
							boolean isFootnote = false;
							
							//	compare line length (true if at least 10% longer lines than page average)
							isFootnote = (isFootnote || ((pLineCount != 0) && ((pLineCount * charCount * 10) < (lineCount * pCharCount * 9))));
							
							//	check textual evidence
							isFootnote = (isFootnote || this.isFootnote(paragraph));
							
							//	paragraph voted as footnote
							if (isFootnote) {
								data.addAnnotation(FOOTNOTE_TYPE, paragraph.getStartIndex(), paragraph.size());
								pIndex--;
							}
							else pIndex = -1;
						}
						
						//	paragraph too long, stop searching
						else pIndex = -1;
					}
				}
				
				//	switch to next page border
				pageBorderIndex++;
				lastBorderStartIndex = nextBorderStartIndex;
				if (pageBorderIndex < pageBorders.length)
					nextBorderStartIndex = pageBorders[pageBorderIndex].getStartIndex();
				else nextBorderStartIndex = data.size();
				
				//	reset line length
				lineCount = 0;
				charCount = 0;
			}
			
			//	build paragraph statistics
			else {
				
				//	count only inner lines of paragraph, last line is likely to be shorter
				int lineLength = 0;
				for (int t = 0; t < (paragraph.size() - 1); t++) {
					Token token = paragraph.tokenAt(t);
					lineLength += token.length();
					if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
						lineCount ++;
						charCount += lineLength;
						lineLength = 0;
					}
				}
			}
		}
		
		//	remove footnotes containing page numbers
		AnnotationFilter.removeContaining(data, FOOTNOTE_TYPE, PAGE_NUMBER_TYPE);
		
		//	remove page titles in footnotes
		AnnotationFilter.removeContained(data, FOOTNOTE_TYPE, PAGE_TITLE_TYPE);
	}
	
	/**	check weather a paragraph is a footnote
	 * @param	paragraph	a TokenSequence representing the paragraph to check
	 * @return true if and only if the specified paragraph is a footnote
	 */
	private boolean isFootnote(TokenSequence paragraph) {
		
		//	catch empty paragraphs
		if (paragraph.size() == 0) return false;
		
		//	index letter glued to first word
		if (paragraph.valueAt(0).matches("([a-z][A-Z][A-Za-z0-9\\-']++)")) return true;
		
		//	check for copyright notice
		for (int t = 0; t < paragraph.size(); t++) 
			if (paragraph.tokenAt(t).equals("©")) return true;

		//	catch single-tokened paragraphs
		if (paragraph.size() == 1) return this.isCitation(paragraph);
		
		//	index char (not a capital letter, not an opening bracket) followed by capitalized word
		if ((paragraph.tokenAt(0).length() == 1) && 
				Gamta.isFirstLetterUpWord(paragraph.tokenAt(1)) && 
				!Gamta.isOpeningBracket(paragraph.tokenAt(0)) && 
				(Gamta.UPPER_CASE_LETTERS.indexOf(paragraph.valueAt(0)) == -1)
			) return true;
		
		//	catch two token paragraphs
		if (paragraph.size() == 2) return this.isCitation(paragraph);
		
		//	index char (not a capital letter, not an opening bracket) followed by start of quotas and capitalized word 
		if (((paragraph.tokenAt(0).length() == 1) && !Gamta.isOpeningBracket(paragraph.tokenAt(0))) && 
				(paragraph.tokenAt(1).equals("'") || paragraph.tokenAt(1).equals("\"")) && 
				Gamta.isFirstLetterUpWord(paragraph.tokenAt(2))
			) return true;
		
		//	check if paragraph is a citation
		return this.isCitation(paragraph);
	}
	
	/**	check weather a paragraph is a citation
	 * @param	paragraph	a TokenSequence representing the paragraph to check
	 * @return true if and only if the specified paragraph is a citation
	 */
	private boolean isCitation(TokenSequence paragraph) {
		
		//	check frequent parts of citations
		int vote = 0;
		for (int t = 1; t < paragraph.size(); t++)
			if (paragraph.tokenAt(t).equals("."))
				if (this.citationParts.containsIgnoreCase(paragraph.valueAt(t))) vote++;
		if (vote > 1) return true;
		
		//	tokenize line without inner punctuation
		TokenSequence strictTokens = new PlainTokenSequence(paragraph, Gamta.NO_INNER_PUNCTUATION_TOKENIZER);
		
		//	search quoted part in title case
		int start = 0;
		while ((start < strictTokens.size()) && !strictTokens.valueAt(start).equals("'") && !strictTokens.valueAt(start).equals("\"")) start++;
		if (start == strictTokens.size()) vote --;
		start++;
		while ((start < strictTokens.size()) && (!Gamta.isPunctuation(strictTokens.tokenAt(start)) || strictTokens.valueAt(start).equals(",") || Gamta.isBracket(strictTokens.tokenAt(start)))) {
			Token token = strictTokens.tokenAt(start);
			if (Gamta.isWord(token) && !Gamta.isCapitalizedWord(token) && !this.noiseWords.contains(token.getValue())) return false;
			start++;
		}
		if (start == strictTokens.size()) vote ++;
		
		//	check character/token mix
		int wordCount = 0;
		int punctuationCount = 0;
		int abbreviationCount = 0;
		int upperCaseCount = 0;
		for (int t = 0; t < strictTokens.size(); t++) {
			Token token = strictTokens.tokenAt(t);
			
			//	abbreviation (word followed by period)
			if (((t + 1) < strictTokens.size()) && Gamta.isFirstLetterUpWord(token) && strictTokens.tokenAt(t + 1).equals(".")) abbreviationCount++;
			
			//	capitalized word in middle of sentence
			if ((t != 0) && Gamta.isFirstLetterUpWord(token) && !Gamta.isSentenceEnd(strictTokens.tokenAt(t - 1))) upperCaseCount++;
			
			//	non-bracket punctuation mark
			if (Gamta.isPunctuation(token))
				if (!Gamta.isBracket(token) && ((t == 0) || !token.equals(strictTokens.tokenAt(t - 1)))) punctuationCount++;
				else if (Gamta.isBracket(token)) punctuationCount--;
			
			//	word, or indexing letter
			if (Gamta.isWord(token))
				if ((token.length() == 1) && Gamta.isLowerCaseWord(token) && !token.equals("a")) punctuationCount += 2;
				else wordCount++;
			
			//	number
			if (Gamta.isNumber(token)) wordCount++;
		}
		
		//	check if high quantum of punctuation and abbreviations in paragraph
		return (((1 + vote) * (punctuationCount + abbreviationCount + upperCaseCount)) > (1 * wordCount));
	}
}
