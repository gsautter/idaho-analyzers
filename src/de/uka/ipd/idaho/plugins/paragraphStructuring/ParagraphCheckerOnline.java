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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CheckBoxFeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * 
 * 
 * @author sautter
 */
public class ParagraphCheckerOnline extends AbstractAnalyzer implements LiteratureConstants {
	
	private static final String LINE_ANNOTATION_TYPE = "line";
	private static final String PARAGRAPH_START_ATTRIBUTE = "paragraphStart";
	
	private StringVector noiseWords = Gamta.getNoiseWords();
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	mark up lines, collect paragraph and line start words, and in line word pairs 
		StringVector paragraphStartWords = new StringVector();
		
		StringVector lineStartWords = new StringVector();
		StringVector inLineWords = new StringVector();
		StringIndex lswIndex = new StringIndex(true);
		StringIndex ilwIndex = new StringIndex(true);
		
		StringVector wordPairs = new StringVector();
		
		for (int p = 0; p < paragraphs.length; p++) {
			if (Gamta.isFirstLetterUpWord(paragraphs[p].firstToken()) || Gamta.isNumber(paragraphs[p].firstToken()))
				paragraphStartWords.addElementIgnoreDuplicates(paragraphs[p].firstValue());
			
			//	mark up lines
			int lineStart = 0;
			for (int t = 0; t < paragraphs[p].size(); t++) {
				if (paragraphs[p].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) || ((t+1) == paragraphs[p].size())) {
					Annotation line = paragraphs[p].addAnnotation(LINE_ANNOTATION_TYPE, lineStart, (t - lineStart + 1));
					if (lineStart == 0)
						line.setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
					lineStart = t+1;
				}
			}
			
			//	collect line internal word pairs
			Annotation[] lines = paragraphs[p].getAnnotations(LINE_ANNOTATION_TYPE);
			for (int l = 0; l < lines.length; l++) {
				if (Gamta.isWord(lines[l].firstValue()) || Gamta.isNumber(lines[l].firstValue())) {
					lineStartWords.addElementIgnoreDuplicates(lines[l].firstValue());
					lswIndex.add(lines[l].firstValue());
				}
				
				for (int t = 1; t < lines[l].size(); t++) {
					if (Gamta.isWord(lines[l].tokenAt(t)) || Gamta.isNumber(lines[l].tokenAt(t))) {
						inLineWords.addElementIgnoreDuplicates(lines[l].valueAt(t));
						ilwIndex.add(lines[l].valueAt(t));
					}
					
					if (Gamta.isWord(lines[l].tokenAt(t-1)) && 
						Gamta.isWord(lines[l].tokenAt(t)) &&
						!this.noiseWords.contains(lines[l].valueAt(t-1)) &&
						!this.noiseWords.contains(lines[l].valueAt(t))
						)
						wordPairs.addElementIgnoreDuplicates(lines[l].valueAt(t-1) + " " + lines[l].valueAt(t));
				}
			}
		}
		
		//	clean up word sets
		paragraphStartWords = paragraphStartWords.without(inLineWords);
//		lineStartWords = lineStartWords.without(inLineWords);
		
		/*TODO
build explanation page for ParagraphCheckerOnline (test of this is required anyway)
- special cases:
  - main text paragraph (from previous page) continues after upper-edge page title
  - footnote starts after open ending of main text paragraph (that continues on the next page)
  - lower-edge page title starts after open ending of main text paragraph (that continues on the next page)
  - main text paragraph (started above the caption) continues after that caption
  - caption starts after open ending of main text paragraph (that continues below the caption)
		 */
		
		//	judge lines per page, and prepare feedback 
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		System.out.println("Got " + pages.length + " pages");
		CheckBoxFeedbackPanel[] pageFps = new CheckBoxFeedbackPanel[pages.length];
		ArrayList pageFpList = new ArrayList();
		for (int p = 0; p < pages.length; p++) {
			Annotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
			CheckBoxFeedbackPanel pageFp = new CheckBoxFeedbackPanel("Check Paragraphs");
//			pageFp.setLabel("<HTML>Please review if the individual lines continue the paragraph of the line before them." +
//					"<BR>Check the check box if a line does not continue the paragraph the line before it belongs to, un-check it if it does." +
//					"<BR>This might either be starting a new paragraph, or continuing one that was interrupted.</HTML>");
//			
//			pageFp.setTrueColor(new Color(255, 255, 127));
//			pageFp.setTrueSpacing(10);
//			pageFp.setFalseSpacing(0);
			pageFp.setLabel("<HTML>Please review if the individual lines continue the paragraph of the line before them." +
					"<BR>Check the box to the left of a line if the text of the line belongs to the same paragraph as the text of the line immediately above it, uncheck it otherwise.</HTML>");
			
			pageFp.setTrueColor(new Color(255, 255, 255));
			pageFp.setFalseColor(new Color(255, 255, 127));
			pageFp.setTrueSpacing(0);
			pageFp.setFalseSpacing(10);
			
			if (lines.length == 0)
				System.out.println("Got no lines in page " + (p+1));
			
			else {
				System.out.println("Got " + (lines.length) + " lines in page " + (p+1));
				
				//	compute average line length
				int lineLength = (pages[p].length() / lines.length);
				
				//	check lines
				lines[0].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
//				pageFp.addLine(lines[0].getValue(), true);
				pageFp.addLine(lines[0].getValue(), false);
				for (int l = 1; l < lines.length; l++) {
					
					//	check if line might start a new paragraph
					if (!lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE)) {
						
						//	split after lines that are sentence ends and signifficantly shorter than surrounding lines
						if (this.isSentenceEnd(lines[l-1]) && ((lines[l-1].length() * 3) < (lineLength * 2)) && this.isSentenceStart(lines[l].firstToken())) {
							lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
							System.out.println("Short line split:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
						}
						
						//	split before lines that are sentence starts and are also paragraph starts elsewhere in the document
						else if (paragraphStartWords.contains(lines[l].firstValue())) {
							lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
							System.out.println("Line start split:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
						}
					}
					
					//	check if line might actually continue a paragraph
					if (lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE)) {
						
						//	check for devided words
						if (this.isWordDevided(lines[l-1].lastToken(), lines[l].firstToken())) {
							lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
							System.out.println("Word devision merge:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
						}
						
//						//	check for continued sentence (don't merge if line start appears only as line start)
//						else if (!this.isSentenceEnd(lines[l-1]) && (lines[l-1].size() > 5) && this.isSentenceContinued(lines[l]) && !lineStartWords.contains(lines[l].firstValue())) {
//							lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
//							System.out.println("Sentence continuation merge:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
//						}
//						
						//	check for continued sentence (don't merge if line start appears only as line start) TODO validate threshold 5
						else if (!this.isSentenceEnd(lines[l-1]) && (lines[l-1].size() > 5) && this.isSentenceContinued(lines[l]) && ((ilwIndex.getCount(lines[l].firstValue()) * 2) >= lswIndex.getCount(lines[l].firstValue()))) {
							lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
							System.out.println("Sentence continuation merge:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
						}
						
						//	join known word pairs devided by paragraph border
						else if (wordPairs.contains(lines[l-1].lastValue() + " " + lines[l].firstValue())) {
							lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
							System.out.println("Word pair merge:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
						}
						
						//	join lines if first one ends with noise word in lower case (very unlikely to be section title)
						else if (this.noiseWords.contains(lines[l-1].lastValue()) && (lines[l-1].lastValue().length() > 1)) {
							lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
							System.out.println("Stop word end merge:\n  " + lines[l-1].getValue() + "\n  " + lines[l].getValue());
						}
					}
					
					//	add line to feedback dialog
//					pageFp.addLine(lines[l].getValue(), lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE));	
					pageFp.addLine(lines[l].getValue(), !lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE));	
				}
			}
			
			//	empty page, omit it
			if (pageFp.lineCount() == 0)
				pageFps[p] = null;
			
			//	data to check
			else {
				
				//	add backgroung information
				pageFp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, pages[p].getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
				pageFp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, pages[p].getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
				pageFp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, pages[p].getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
				pageFp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, pages[p].getAnnotationID());
				pageFp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PARAGRAPH_TYPE);
				
				//	add target page numbers
				String targetPages = FeedbackPanel.getTargetPageString(pages, p, (p+1));
				if (targetPages != null)
					pageFp.setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
				String targetPageIDs = FeedbackPanel.getTargetPageIdString(pages, p, (p+1));
				if (targetPageIDs != null)
					pageFp.setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
				
				//	schedule feedback
				pageFpList.add(pageFp);
				pageFps[p] = pageFp;
			}
		}
		
		//	get feedback
		int cutoffPage = pages.length;
		if (FeedbackPanel.isMultiFeedbackEnabled())
			FeedbackPanel.getMultiFeedback((FeedbackPanel[]) pageFpList.toArray(new FeedbackPanel[pageFpList.size()]));
		
		else for (int p = 0; p < pageFpList.size(); p++) {
			FeedbackPanel fp = ((FeedbackPanel) pageFpList.get(p));
			if (p != 0)
				fp.addButton("Previous");
			fp.addButton("Cancel");
			fp.addButton("OK" + (((p+1) == pageFpList.size()) ? "" : " & Next"));
			
			String title = fp.getTitle();
			fp.setTitle(title + " - (" + (p+1) + " of " + pageFpList.size() + ")");
			
			String f = fp.getFeedback();
			if (f == null) f = "Cancel";
			
			fp.setTitle(title);
			
			//	current dialog submitted, keep as is
			if (f.startsWith("OK")) {}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				p-=2;
			
			//	cancel from current dialog on
			else {
				cutoffPage = p;
				p = pageFpList.size();
			}
//			fp.addButton("OK");
//			fp.addButton("Cancel");
//			String f = fp.getFeedback();
//			if ("Cancel".equals(f)) {
//				cutoffPage = p;
//				p = pageFpList.size();
//			}
		}
		
		//	process feedback page by page
//		for (int p = 0; p < pages.length; p++) {
		for (int p = 0; p < cutoffPage; p++) {
			
			//	jump empty pages
			if (pageFps[p] == null)
				continue;
			
			//	remove old paragraphs
			AnnotationFilter.removeAnnotations(pages[p], MutableAnnotation.PARAGRAPH_TYPE);
			
			//	get lines
			Annotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
			
			//	read feedback
//			CheckBoxFeedbackPanel pageFp = pageFps[p];
			for (int l = 0; l < lines.length; l++) {
//				if (pageFp.getStateAt(l))
//					lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
//				else lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
				if (pageFps[p].getStateAt(l))
					lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
				else lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
			}
			
			//	nothing to do
			if (lines.length == 0)
				System.out.println("Got no lines in page " + (p+1));
			
			//	re-create paragraphs
			else {
				System.out.println("Got " + (lines.length) + " lines in page " + (p+1));
				int pStart = 0;
				for (int l = 1; l < lines.length; l++) {
					if (lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE)) {
						pages[p].addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, pStart, (lines[l].getStartIndex() - pStart));
						pStart = lines[l].getStartIndex();
					}
				}
				pages[p].addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, pStart, (pages[p].size() - pStart));
			}
		}
		
		//	clean up lines
		AnnotationFilter.removeAnnotations(data, LINE_ANNOTATION_TYPE);
	}
	
	/**	determine weather a given line ends with the end of a sentence
	 * @param	line	a TokenSequence containing the line to check
	 * @return true if and only if the specified line is considered to end with the end of a sentence
	 */
	private boolean isSentenceEnd(TokenSequence line) {
		
		//	check single-tokened lines
		if (line.size() < 2) return false;
		
		//	check if brackets remain open
		else if (leavesBracketsOpen(line)) return false;
		
		//	check ending punctuation
		else if (Gamta.isSentenceEnd(line.lastToken())) return true;
		
		//	check ending punctuation before ending quotas
		else return (Gamta.isSentenceEnd(line.tokenAt(line.size() - 2)) && (line.lastToken().equals("'") || line.lastToken().equals("\"")));
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
			}
			else if (Gamta.closes(token, lastOpenBracket)) {
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
	
	/**	determine weather two Tokens represent a devided word
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