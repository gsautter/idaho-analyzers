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

import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class ParagraphStructureNormalizer extends AbstractAnalyzer  implements LiteratureConstants {
	private static final String DASHES = "-­——";
	private StringVector enumerationConjunctions = new StringVector();
	
	/** Constructor
	 */
	public ParagraphStructureNormalizer() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractAnalyzer#setDataProvider(de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider)
	 */
	public void setDataProvider(AnalyzerDataProvider dataProvider) {
		super.setDataProvider(dataProvider);
		this.enumerationConjunctions.parseAndAddElements(
				"and;or;" + // English
				"und;oder;" + // German
				"e;o;ossia;" + // Italian
				"et;ou;" + // French
				"y;e;o;u;" + // Spanish
				"e;ou;" + // Portuguese
				"et;ac;aut;vel;sive;utrum;" + // Latin
				"",
				";");
		this.enumerationConjunctions.removeDuplicateElements();
	}

	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		if ((data == null) || (data.length() == 0)) return;
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		if (paragraphs.length == 0)
			return;
		
		//	normalize dashes to prevent matching problems
		for (int c = 0; c < data.length(); c++) {
			char ch = data.charAt(c);
			if ((ch != '-') && (DASHES.indexOf(ch) != -1))
				data.setChar('-', c);
		}
		
		//	collect tokens in order to assess which joins make sense
		StringVector tokens = new StringVector();
		boolean lastWasBreak = true;
		for (int t = 0; t < data.size(); t++) {
			Token token = data.tokenAt(t);
			if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
				lastWasBreak = true;
			
			else if (data.getWhitespaceAfter(t).indexOf('\n') != -1) {
				token.setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				lastWasBreak = true;
			}
			else if (data.getWhitespaceAfter(t).indexOf('\r') != -1) {
				token.setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				lastWasBreak = true;
			}
			else {
				if (!lastWasBreak)
					tokens.addElementIgnoreDuplicates(token.getValue());
				lastWasBreak = false;
			}
		}
		
		//	add page IDs and numbers to tokens
		Annotation[] pageBreakTokens = data.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
		int nextPageBreakTokenIndex = 0;
		Object pageIdObject = paragraphs[0].getAttribute(PAGE_ID_ATTRIBUTE);
		Object pageNumberObject = paragraphs[0].getAttribute(PAGE_NUMBER_ATTRIBUTE);
		for (int t = paragraphs[0].getStartIndex(); t < data.size(); t++) {
			if ((nextPageBreakTokenIndex < pageBreakTokens.length) && (t >= pageBreakTokens[nextPageBreakTokenIndex].getStartIndex())) {
				pageIdObject = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_ID_ATTRIBUTE);
				pageNumberObject = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE);
				nextPageBreakTokenIndex++;
			}
			data.tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageIdObject);
			data.tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberObject);
		}
		
		//	normalize paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			if (paragraph.size() == 0)
				continue;
			
			int t = 0;
			while ((t + 1) < paragraph.size()) {
				Token t1 = paragraph.tokenAt(t);
				Token t2 = paragraph.tokenAt(t + 1);
				
				//	concatenate word devided Tokens
				if (t1.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
					String jointForm = this.getJointForm(t1, t2, tokens);
					
					//	nothing to join
					if (jointForm == null)
						t++;
					
					//	join tokens
					else {
						
						//	store page break indicator attributes
						Token firstToken = paragraph.tokenAt(t);
						Object ftPageId = firstToken.getAttribute(PAGE_ID_ATTRIBUTE);
						Object ftPageNumber = firstToken.getAttribute(PAGE_NUMBER_ATTRIBUTE);
						Token secondToken = paragraph.tokenAt(t+1);
						Object stPageId = secondToken.getAttribute(PAGE_ID_ATTRIBUTE);
						Object stPageNumber = secondToken.getAttribute(PAGE_NUMBER_ATTRIBUTE);
						
						//	join tokens
						paragraph.setValueAt(jointForm, t);
						paragraph.removeTokensAt((t+1), 1);
						
						//	restore page break
						if (((ftPageId != null) && !ftPageId.equals(stPageId)) || ((ftPageNumber != null) && !ftPageNumber.equals(stPageNumber))) {
							Annotation pageBreak = paragraph.addAnnotation(PAGE_BREAK_TOKEN_TYPE, t+1, 1);
							pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, stPageId);
							pageBreak.setAttribute(PAGE_NUMBER_ATTRIBUTE, stPageNumber);
						}
					}
				}
				else t++;
			}
			
			//	normalize line end property and whitespace
			for (t = 0; t < (paragraph.size() - 1); t++) {
				String whitespace = paragraph.getWhitespaceAfter(t);
				whitespace = whitespace.replaceAll("\\r\\n", " ");
				whitespace = whitespace.replaceAll("\\r\\n", " ");
				whitespace = whitespace.replaceAll("\\r", " ");
				whitespace = whitespace.replaceAll("\\n", " ");
				paragraph.setWhitespaceAfter(whitespace, t);
				paragraph.tokenAt(t).removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
			}
			
			//	set line preak at end of paragraph
			paragraph.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			String whitespace = data.getWhitespaceAfter((paragraph.getEndIndex() - 1));
			if (whitespace.indexOf("\n") == -1)
				data.setWhitespaceAfter("\n", (paragraph.getEndIndex() - 1));
		}
		
		//	restore page starts
		pageBreakTokens = data.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
		HashSet seenPageIds = new HashSet();
		HashSet seenPageNumbers = new HashSet();
		for (int b = 0; b < pageBreakTokens.length; b++) {
			if (seenPageIds.add(pageBreakTokens[b].getAttribute(PAGE_ID_ATTRIBUTE)) | seenPageNumbers.add(pageBreakTokens[b].getAttribute(PAGE_NUMBER_ATTRIBUTE)))
				pageBreakTokens[b].setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
		}
		
		//	restore annotation start and end attributes
		Annotation[] annotations = data.getAnnotations();
		for (int a = 0; a < annotations.length; a++) {
			Object pageId = annotations[a].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
			annotations[a].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			Object pageNumber = annotations[a].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			annotations[a].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			
			Object lastPageId = annotations[a].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if ((lastPageId != null) && !lastPageId.equals(pageId))
				annotations[a].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageId);
			Object lastPageNumber = annotations[a].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if ((lastPageNumber != null) && !lastPageNumber.equals(pageNumber))
				annotations[a].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumber);
		}
		
		//	clean up token attributes
		for (int t = paragraphs[0].getStartIndex(); t < data.size(); t++) {
			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
	}
	
	private String getJointForm(Token token1, Token token2, StringVector tokens) {
		if (token1 == null)
			return null;
		if (token1.length() < 2)
			return null;
		
		//	no hyphen at end of line
		String value1 = token1.getValue();
		if (!value1.endsWith("-"))
			return null;
		
		//	no word to continue with, or part of enumeration
		String value2 = token2.getValue();
		if (!Gamta.isWord(value2) || this.enumerationConjunctions.containsIgnoreCase(value2))
			return null;
//		if (!Gamta.isWord(value2) || "and".equalsIgnoreCase(value2) || "or".equalsIgnoreCase(value2))
//			return null;
		
		//	prepare for lookup
		String nValue2 = value2.toLowerCase();
		if (nValue2.endsWith("'s"))
			nValue2 = nValue2.substring(0, (nValue2.length() - 2));
		else if (nValue2.endsWith("ies"))
			nValue2 = nValue2.substring(0, (nValue2.length() - 3)) + "y";
		else if (nValue2.endsWith("s"))
			nValue2 = nValue2.substring(0, (nValue2.length() - 1));
		
		//	joint value appears with hyphen elsewhere in text ==> keep hyphen
		String hyphenValue = (value1 + value2);
		if (tokens.containsIgnoreCase(hyphenValue))
			return hyphenValue;
		String nHyphenValue = (value1 + nValue2);
		if (tokens.containsIgnoreCase(nHyphenValue))
			return nHyphenValue;
		
		//	joint value appears elsewhere in text ==> join
		String jointValue = (value1.substring(0, (value1.length() - 1)) + value2);
		if (tokens.containsIgnoreCase(jointValue))
			return jointValue;
		String nJointValue = (value1.substring(0, (value1.length() - 1)) + nValue2);
		if (tokens.containsIgnoreCase(nJointValue))
			return jointValue;
		
		//	if lower case letter before hyhen (probably nothing like 'B-carotin') ==> join
		if (Gamta.LOWER_CASE_LETTERS.indexOf(value1.charAt(value1.length() - 2)) != -1)
			return jointValue;
		
		//	no indication, be conservative
		return null;
	}
}
//public class ParagraphStructureNormalizer extends AbstractAnalyzer implements LiteratureConstants {
//	
//	/** Constructor
//	 */
//	public ParagraphStructureNormalizer() {}
//	
//	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		if ((data == null) || (data.length() == 0)) return;
//		
//		//	get paragraphs
//		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
//		if (paragraphs.length == 0)
//			return;
//		
//		//	collect tokens in order to assess which joins make sense
//		StringVector tokens = new StringVector();
//		boolean lastWasBreak = true;
//		for (int t = 0; t < data.size(); t++) {
//			Token token = data.tokenAt(t);
//			if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
//				lastWasBreak = true;
//			
//			else if (data.getWhitespaceAfter(t).indexOf('\n') != -1) {
//				token.setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
//				lastWasBreak = true;
//			}
//			else if (data.getWhitespaceAfter(t).indexOf('\r') != -1) {
//				token.setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
//				lastWasBreak = true;
//			}
//			else {
//				if (!lastWasBreak)
//					tokens.addElementIgnoreDuplicates(token.getValue());
//				lastWasBreak = false;
//			}
//		}
//		
//		//	add page IDs and numbers to tokens
//		Annotation[] pageBreakTokens = data.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
//		int nextPageBreakTokenIndex = 0;
//		Object pageIdObject = paragraphs[0].getAttribute(PAGE_ID_ATTRIBUTE);
//		Object pageNumberObject = paragraphs[0].getAttribute(PAGE_NUMBER_ATTRIBUTE);
//		for (int t = paragraphs[0].getStartIndex(); t < data.size(); t++) {
//			if ((nextPageBreakTokenIndex < pageBreakTokens.length) && (t == pageBreakTokens[nextPageBreakTokenIndex].getStartIndex())) {
//				pageIdObject = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_ID_ATTRIBUTE);
//				pageNumberObject = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE);
//				nextPageBreakTokenIndex++;
//			}
//			data.tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageIdObject);
//			data.tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberObject);
//		}
//		
//		//	normalize paragraphs
//		for (int p = 0; p < paragraphs.length; p++) {
//			MutableAnnotation paragraph = paragraphs[p];
//			if (paragraph.size() != 0) {
//				int t = 0;
//				while ((t + 1) < paragraph.size()) {
//					Token t1 = paragraph.tokenAt(t);
//					Token t2 = paragraph.tokenAt(t + 1);
//					
//					//	concatenate word devided Tokens
//					if (t1.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
//						String jointForm = this.getJointForm(t1, t2, tokens);
//						
//						//	nothing to join
//						if (jointForm == null)
//							t++;
//						
//						//	join tokens
//						else {
//							
//							//	store page break indicator attributes
//							Token firstToken = paragraph.tokenAt(t);
//							Object ftPageId = firstToken.getAttribute(PAGE_ID_ATTRIBUTE);
//							Object ftPageNumber = firstToken.getAttribute(PAGE_NUMBER_ATTRIBUTE);
//							Token secondToken = paragraph.tokenAt(t+1);
//							Object stPageId = secondToken.getAttribute(PAGE_ID_ATTRIBUTE);
//							Object stPageNumber = secondToken.getAttribute(PAGE_NUMBER_ATTRIBUTE);
//							
//							//	join tokens
//							paragraph.setValueAt(jointForm, t);
//							paragraph.removeTokensAt((t+1), 1);
//							
//							//	restore page break
//							if (((ftPageId != null) && !ftPageId.equals(stPageId)) || ((ftPageNumber != null) && !ftPageNumber.equals(stPageNumber))) {
//								Annotation pageBreak = paragraph.addAnnotation(PAGE_BREAK_TOKEN_TYPE, t+1, 1);
//								pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, stPageId);
//								pageBreak.setAttribute(PAGE_NUMBER_ATTRIBUTE, stPageNumber);
//							}
//						}
//					}
//					else t++;
//				}
//				
//				//	normalize line end property and whitespace
//				for (t = 0; t < (paragraph.size() - 1); t++) {
//					String whitespace = paragraph.getWhitespaceAfter(t);
//					whitespace = whitespace.replaceAll("\\r\\n", " ");
//					whitespace = whitespace.replaceAll("\\r\\n", " ");
//					whitespace = whitespace.replaceAll("\\r", " ");
//					whitespace = whitespace.replaceAll("\\n", " ");
//					paragraph.setWhitespaceAfter(whitespace, t);
//					paragraph.tokenAt(t).removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
//				}
//				
//				//	set line preak at end of paragraph
//				paragraph.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
//				String whitespace = data.getWhitespaceAfter((paragraph.getEndIndex() - 1));
//				if (whitespace.indexOf("\n") == -1)
//					data.setWhitespaceAfter("\n", (paragraph.getEndIndex() - 1));
//			}
//		}
//		
//		//	restore page starts
//		pageBreakTokens = data.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
//		HashSet seenPageIds = new HashSet();
//		HashSet seenPageNumbers = new HashSet();
//		for (int b = 0; b < pageBreakTokens.length; b++) {
//			if (seenPageIds.add(pageBreakTokens[b].getAttribute(PAGE_ID_ATTRIBUTE)) | seenPageNumbers.add(pageBreakTokens[b].getAttribute(PAGE_NUMBER_ATTRIBUTE)))
//				pageBreakTokens[b].setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
//		}
//		
//		//	restore annotation start and end attributes
//		Annotation[] annotations = data.getAnnotations();
//		for (int a = 0; a < annotations.length; a++) {
//			Object pageId = annotations[a].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
//			annotations[a].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
//			Object pageNumber = annotations[a].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
//			annotations[a].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
//			
//			Object lastPageId = annotations[a].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
//			if ((lastPageId != null) && !lastPageId.equals(pageId))
//				annotations[a].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageId);
//			Object lastPageNumber = annotations[a].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
//			if ((lastPageNumber != null) && !lastPageNumber.equals(pageNumber))
//				annotations[a].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumber);
//		}
//		
//		//	clean up token attributes
//		for (int t = paragraphs[0].getStartIndex(); t < data.size(); t++) {
//			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
//			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
//		}
//	}
//	
//	private String getJointForm(Token token1, Token token2, StringVector tokens) {
//		if (token1 == null)
//			return null;
//		if (token1.length() < 2)
//			return null;
//		
//		//	no hyphen at end of line
//		String value1 = token1.getValue();
//		if (!value1.endsWith("-"))
//			return null;
//		
//		//	no word to continue with, or part of enumeration
//		String value2 = token2.getValue();
//		if (!Gamta.isWord(value2) || "and".equalsIgnoreCase(value2) || "or".equalsIgnoreCase(value2))
//			return null;
//		
//		//	prepare for lookup
//		String nValue2 = value2.toLowerCase();
//		if (nValue2.endsWith("'s"))
//			nValue2 = nValue2.substring(0, (nValue2.length() - 2));
//		else if (nValue2.endsWith("ies"))
//			nValue2 = nValue2.substring(0, (nValue2.length() - 3)) + "y";
//		else if (nValue2.endsWith("s"))
//			nValue2 = nValue2.substring(0, (nValue2.length() - 1));
//		
//		//	joint value appears with hyphen elsewhere in text ==> keep hyphen
//		String hyphenValue = (value1 + value2);
//		if (tokens.containsIgnoreCase(hyphenValue))
//			return hyphenValue;
//		String nHyphenValue = (value1 + nValue2);
//		if (tokens.containsIgnoreCase(nHyphenValue))
//			return nHyphenValue;
//		
//		//	joint value appears elsewhere in text ==> join
//		String jointValue = (value1.substring(0, (value1.length() - 1)) + value2);
//		if (tokens.containsIgnoreCase(jointValue))
//			return jointValue;
//		String nJointValue = (value1.substring(0, (value1.length() - 1)) + nValue2);
//		if (tokens.containsIgnoreCase(nJointValue))
//			return jointValue;
//		
//		//	if lower case letter before hyhen (probably nothing like 'B-carotin') ==> join
//		if (Gamta.LOWER_CASE_LETTERS.indexOf(value1.charAt(value1.length() - 2)) != -1)
//			return jointValue;
//		
//		//	no indication, be conservative
//		return null;
//	}
//}