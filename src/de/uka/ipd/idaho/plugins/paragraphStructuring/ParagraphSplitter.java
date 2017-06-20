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


import java.util.ArrayList;
import java.util.Properties;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Annotation;
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
public class ParagraphSplitter extends AbstractAnalyzer {
	
	private static final String SPLIT = "split";
	private static final String LINE_ANNOTATION_TYPE = "line";
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	collect paragraph and line start words 
		StringVector paragraphStartWords = new StringVector();
		StringVector inLineWords = new StringVector();
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			if (Gamta.isFirstLetterUpWord(paragraph.firstToken()))
				paragraphStartWords.addElementIgnoreDuplicates(paragraph.firstValue());
			
			boolean lastWasLineEnd = true;
			for (int t = 0; t < paragraph.size(); t++) {
				Token token = paragraph.tokenAt(t);
				if (Gamta.isWord(token) && !lastWasLineEnd) inLineWords.addElementIgnoreDuplicates(token.getValue());
				lastWasLineEnd = token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
			}
		}
		
		//	clean up word sets
		paragraphStartWords = paragraphStartWords.without(inLineWords);
		
		//	check individual paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			
			//	obtain paragraph lines
			Annotation[] lines = this.markLines(paragraph);
			
			//	compute average line length
			int lineLength = (paragraph.length() / lines.length);
			
			//	check line by line if split necessary
			lines[0].setAttribute(SPLIT, SPLIT);
			int pCount = 1;
			for (int l = 1; l < lines.length; l++) {
				boolean split = false;
				
				//	split after lines that are sentence ends and signifficantly shorter than surrounding lines
				if (this.isSentenceEnd(lines[l-1]) && ((lines[l-1].length() * 3) < (lineLength * 2)) && this.isSentenceStart(lines[l].firstToken()))
					split = true;
					
				//	split before lines that are sentence starts and are also paragraph starts elsewhere in the document
				if (!split && paragraphStartWords.contains(lines[l].firstValue()))
					split = true;
				
				//	split before current line if any criterion applied
				if (split) {
					lines[l].setAttribute(SPLIT, SPLIT);
					pCount++;
				}
			}
			
			//	perform split if necessary
			if (pCount > 1) {
				int pStart = 0;
				int pSize = lines[0].size();
				for (int l = 1; l < lines.length; l++) {
					if (lines[l].hasAttribute(SPLIT)) {
						MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, (paragraph.getStartIndex() + pStart), pSize);
						newParagraph.copyAttributes(paragraph);
						pStart = lines[l].getStartIndex();
						pSize = 0;
					}
					pSize += lines[l].size();
				}
				if (pSize > 0) {
					MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, (paragraph.getStartIndex() + pStart), pSize);
					newParagraph.copyAttributes(paragraph);
				}
				data.removeAnnotation(paragraph);
			}
		}
	}
	
	/**	annotate all lines in a paragraph
	 * @param	paragraph	the paragraph to annotate lines in
	 */
	private Annotation[] markLines(MutableAnnotation paragraph) {
		int lineStart = 0;
		int lineSize = 0;
		ArrayList lines = new ArrayList();
		
		//	mark up lines
		for (int t = 0; t < paragraph.size(); t++) {
			Token token = paragraph.tokenAt(t);
			lineSize ++;
			if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				lines.add(Gamta.newAnnotation(paragraph, LINE_ANNOTATION_TYPE, lineStart, lineSize));
				lineStart = t+1;
				lineSize = 0;
			}
		}
		
		//	mark up last line
		if (lineSize != 0)
			lines.add(Gamta.newAnnotation(paragraph, LINE_ANNOTATION_TYPE, lineStart, lineSize));
		
		//	return lines
		return ((Annotation[]) lines.toArray(new Annotation[lines.size()]));
	}
	
	/**	determine weather a given line ends with the end of a sentence
	 * @param	line	a TokenSequence containing the line to check
	 * @return true if and only if the specified line is considered to end with the end of a sentence
	 */
	private boolean isSentenceEnd(TokenSequence line) {
		
		//	check if brackets remain open
		if (leavesBracketsOpen(line)) return false;
		
		//	check ending punctuation
		if (Gamta.isSentenceEnd(line.lastToken())) return true;
		
		//	check single-tokened lines
		if (line.size() < 2) return false;
		
		//	check ending punctuation before ending quotas
		return (Gamta.isSentenceEnd(line.tokenAt(line.size() - 2)) && (line.lastToken().equals("'") || line.lastToken().equals("\"")));
	}
	
	/**	determine weather a given line opens brackets which it does not close
	 * @param	line	a TokenSequence containing the line to check
	 * @return true if and only if the specified line opens brackets which it does not close
	 */
	private boolean leavesBracketsOpen(TokenSequence line) {
		
		//	check if brackets remain open
		Token lastOpenBracket = null;
		Stack openBrackets = new Stack();
		for (int t = 0; t < line.size(); t++) {
			Token token = line.tokenAt(t);
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
	
	/** check if a TokenSequence can be the start of a sentence
	 * @param	token	the Token to check
	 * @return true if and only if the specified Token can be the start of a sentence
	 */
	private boolean isSentenceStart(Token token) {
		return (Gamta.isFirstLetterUpWord(token) || Gamta.isNumber(token));
	}
}
