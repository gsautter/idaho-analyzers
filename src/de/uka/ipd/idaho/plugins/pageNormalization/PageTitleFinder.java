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


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class PageTitleFinder extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
//	private static final String CAPTION_START_STRING = "figure;figures;table;tables;diagram;diagrams";
//	private static final String CAPTION_START_ABBREVIATIONS_STRING = "fig;figs;tab;tabs;diag;diags";
//	
//	private StringVector captionStarts;
//	private StringVector captionStartAbbreviations;
//	
	private static final int MAX_PAGE_TITLE_TOKENS = 25;
	private static final int MAX_PAGE_TITLE_LINES = 4;
	
	private static final StringVector noise = StringUtils.getNoiseWords();
	
	private int maxTopTitleParagraphs = 3;
	private static final String maxTopTitleParagraphsSetting = "maxTopTitleParagraphs";
	
	private int maxBottomTitleParagraphs = 3;
	private static final String maxBottomTitleParagraphsSetting = "maxTopTitleParagraphs";
	
	private String[] pageTitlePatterns = new String[0];
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load hard limits
		try {
			this.maxTopTitleParagraphs = Integer.parseInt(this.getParameter(maxTopTitleParagraphsSetting));
		} catch (NumberFormatException nfe) {}
		
		try {
			this.maxBottomTitleParagraphs = Integer.parseInt(this.getParameter(maxBottomTitleParagraphsSetting));
		} catch (NumberFormatException nfe) {}
		
//		//	load caption starts
//		try {
//			InputStream is = this.dataProvider.getInputStream("captionStarts.txt");
//			this.captionStarts = StringVector.loadList(is);
//			is.close();
//		}
//		catch (IOException ioe) {
//			this.captionStarts = new StringVector();
//			this.captionStarts.parseAndAddElements(CAPTION_START_STRING, ";");
//		}
		
		//	load patterns
		try {
			InputStream is = this.dataProvider.getInputStream("pageTitlePatterns.txt");
			StringVector pageTitlePatterns = StringVector.loadList(is);
			is.close();
			for (int p = 0; p < pageTitlePatterns.size(); p++) {
				String ptp = pageTitlePatterns.get(p).trim();
				if ((ptp.length() == 0) || ptp.startsWith("//"))
					pageTitlePatterns.remove(p--);
			}
			this.pageTitlePatterns = pageTitlePatterns.toStringArray();
		}
		catch (IOException ioe) {}
		
//		//	load caption start abbreviations
//		try {
//			InputStream is = this.dataProvider.getInputStream("captionStartAbbreviations.txt");
//			this.captionStartAbbreviations = StringVector.loadList(is);
//			is.close();
//		}
//		catch (IOException ioe) {
//			this.captionStartAbbreviations = new StringVector();
//			this.captionStartAbbreviations.parseAndAddElements(CAPTION_START_ABBREVIATIONS_STRING, ";");
//		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		this.storeParameter(maxTopTitleParagraphsSetting, ("" + this.maxTopTitleParagraphs));
		this.storeParameter(maxBottomTitleParagraphsSetting, ("" + this.maxBottomTitleParagraphs));
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	gather words from paragraphs that might be page titles (distinguish even and odd pages)
		StringIndex topWordStatisticsEven = new StringIndex(false);
		StringIndex bottomWordStatisticsEven = new StringIndex(false);
		StringIndex topWordStatisticsOdd = new StringIndex(false);
		StringIndex bottomWordStatisticsOdd = new StringIndex(false);
		StringVector wordCollector = new StringVector(false);
		
		//	collect words from even pages
		for (int p = 0; p < pages.length; p+= 2) {
			
			//	get paragraphs possibly containing page titles
			MutableAnnotation[] paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			
			//	check individual paragraphs from top
			int pIndex = 0;
			while (((pIndex < this.maxTopTitleParagraphs) || (this.maxTopTitleParagraphs < 0)) && (pIndex < paragraphs.length)) {
				MutableAnnotation paragraph = paragraphs[pIndex];
				if (this.extractWords(paragraph, wordCollector)) pIndex++;
				else pIndex = paragraphs.length;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				topWordStatisticsEven.add(wordCollector.get(c));
			wordCollector.clear();
			
			//	check individual paragraphs from bottom
			pIndex = paragraphs.length - 1;
			while (((pages.length <= (pIndex + this.maxBottomTitleParagraphs)) || (this.maxBottomTitleParagraphs < 0)) && (pIndex > -1)) {
				MutableAnnotation paragraph = paragraphs[pIndex];
				if (this.extractWords(paragraph, wordCollector)) pIndex--;
				else pIndex = -1;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				bottomWordStatisticsEven.add(wordCollector.get(c));
			wordCollector.clear();
		}
		
		//	collect words from odd pages
		for (int p = 1; p < pages.length; p+= 2) {
			
			//	get paragraphs possibly containing page titles
			MutableAnnotation[] paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			
			//	check individual paragraphs from top
			int pIndex = 0;
			while (((pIndex < this.maxTopTitleParagraphs) || (this.maxTopTitleParagraphs < 0)) && (pIndex < paragraphs.length)) {
				MutableAnnotation paragraph = paragraphs[pIndex];
				if (this.extractWords(paragraph, wordCollector)) pIndex++;
				else pIndex = paragraphs.length;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				topWordStatisticsOdd.add(wordCollector.get(c));
			wordCollector.clear();
			
			//	check individual paragraphs from bottom
			pIndex = paragraphs.length - 1;
			while (((pages.length <= (pIndex + this.maxBottomTitleParagraphs)) || (this.maxBottomTitleParagraphs < 0)) && (pIndex > -1)) {
				MutableAnnotation paragraph = paragraphs[pIndex];
				if (this.extractWords(paragraph, wordCollector)) pIndex--;
				else pIndex = -1;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				bottomWordStatisticsOdd.add(wordCollector.get(c));
			wordCollector.clear();
		}

		
		//	find page title paragraphs
		for (int p = 0; p < pages.length; p++) {
			
			//	get paragraphs possibly containing page titles
			MutableAnnotation[] paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			
			//	check individual paragraphs from top
			int pIndex = 0;
			while (((pIndex < this.maxTopTitleParagraphs) || (this.maxTopTitleParagraphs < 0)) && (pIndex < paragraphs.length)) {
				MutableAnnotation paragraph = paragraphs[pIndex];
				if (this.isPageTitle(paragraph, (((p % 2) == 0) ? topWordStatisticsEven : topWordStatisticsOdd), pages.length)) {
					pages[p].addAnnotation(PAGE_TITLE_TYPE, paragraph.getStartIndex(), paragraph.size());
					pIndex++;
				}
				else pIndex = paragraphs.length;
			}
			
			//	check individual paragraphs from bottom
			pIndex = paragraphs.length - 1;
			while (((paragraphs.length <= (pIndex + this.maxBottomTitleParagraphs)) || (this.maxBottomTitleParagraphs < 0)) && (pIndex > -1)) {
				MutableAnnotation paragraph = paragraphs[pIndex];
				if (this.isPageTitle(paragraph, (((p % 2) == 0) ? bottomWordStatisticsEven : bottomWordStatisticsOdd), pages.length)) {
					pages[p].addAnnotation(PAGE_TITLE_TYPE, paragraph.getStartIndex(), paragraph.size());
					pIndex--;
				}
				else pIndex = -1;
			}
			
			//	remove duplicates
			AnnotationFilter.removeDuplicates(pages[p], PAGE_TITLE_TYPE);
			
			//	remove mis-recognized captions
			AnnotationFilter.removeContained(pages[p], CAPTION_TYPE, PAGE_TITLE_TYPE);
		}
	}
	
	private boolean extractWords(MutableAnnotation paragraph, StringVector wordCollector) {
		
		//	check paragraph length
		if (paragraph.size() > MAX_PAGE_TITLE_TOKENS) return false;
		
		//	check number of lines
		int paragraphLines = 0;
		for (int t = 0; t < paragraph.size(); t++)
			if (paragraph.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) paragraphLines++;
		if (paragraphLines > MAX_PAGE_TITLE_LINES) return false;
		
		//	count words
		for (int v = 0; v < paragraph.size(); v++) {
			String value = paragraph.valueAt(v);
			if (!noise.containsIgnoreCase(value) && StringUtils.isWord(value))
				wordCollector.addElementIgnoreDuplicates(value);
		}
		
		return true;
	}
	
	private static final boolean DEBUG = false;
	private boolean isPageTitle(MutableAnnotation paragraph, StringIndex wordStatistics, int totalPageCount) {
		if (DEBUG) System.out.println("\n - checking for page title (page number " + paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE) + "): " + paragraph.getValue());
		
		//	check paragraph length
		if (paragraph.size() > MAX_PAGE_TITLE_TOKENS) {
			if (DEBUG) System.out.println(" - NO, too long");
			return false;
		}
		
		//	check number of lines
		int paragraphLines = 0;
		for (int t = 0; t < paragraph.size(); t++)
			if (paragraph.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) paragraphLines++;
		if (paragraphLines > MAX_PAGE_TITLE_LINES) {
			if (DEBUG) System.out.println(" - NO, too many lines");
			return false;
		}
		
//		//	caption indicator word followed by reference number or letter
//		if ((paragraph.size() >= 2) &&
//			this.captionStarts.containsIgnoreCase(paragraph.valueAt(0)) &&
//				(
//					Gamta.isNumber(paragraph.valueAt(1)) ||
//					((paragraph.valueAt(1).length() == 1) && (Gamta.LETTERS.indexOf(paragraph.valueAt(1)) != -1)) ||
//					!Gamta.isWord(paragraph.valueAt(1))
//				)
//			) {
//			if (DEBUG) System.out.println(" - NO, it's a caption");
//			return false;
//		}
//		
//		//	caption indicator word abbreviation followed by reference number or letter
//		if ((paragraph.size() >= 3) &&
//			this.captionStartAbbreviations.containsIgnoreCase(paragraph.valueAt(0)) &&
//			paragraph.valueAt(1).equals(".") &&
//				(
//					Gamta.isNumber(paragraph.tokenAt(2)) ||
//					((paragraph.tokenAt(2).length() == 1) && (Gamta.LETTERS.indexOf(paragraph.valueAt(2)) != -1)) ||
//					!Gamta.isWord(paragraph.tokenAt(2))
//				)
//			) {
//			if (DEBUG) System.out.println(" - NO, it's an abbreviated caption");
//			return false;
//		}
//		
		//	check for page numbers
		Annotation[] pageNumbers = paragraph.getAnnotations(PAGE_NUMBER_TYPE);
		if (pageNumbers.length != 0) {
			if (DEBUG) System.out.println(" - YES, there are page numbers");
			return true;
		}
		
		//	check regular expressions
		String paragraphString = paragraph.getValue();
		for (int p = 0; p < this.pageTitlePatterns.length; p++) {
			if (paragraphString.matches(this.pageTitlePatterns[p])) {
				if (DEBUG) System.out.println(" - YES, matched '" + this.pageTitlePatterns[p] + "'");
				return true;
			}
		}
		
		//	check number of words frequent near page borders (only for more than five pages, to uncertain for less)
		int minPageFraction = ((totalPageCount > 5) ? ((totalPageCount + 1) / 3) : totalPageCount);
		int paragraphWordCount = 0;
		int paragraphKnownWordCount = 0;
		for (int v = 0; v < paragraph.size(); v++) {
			String value = paragraph.valueAt(v);
			if (!noise.containsIgnoreCase(value) && StringUtils.isWord(value)) {
				paragraphWordCount++;
				if (wordStatistics.getCount(value) >= minPageFraction)
					paragraphKnownWordCount++;
			}
		}
		if ((paragraphKnownWordCount * 2) >= paragraphWordCount) {
			if (DEBUG) System.out.println(" - YES, there are many words repeating at other page edges");
			return true;
		}
		
		//	vote other evidence
		int vote = 0;
		int upperCaseCount = 0;
		int charCount = 0;
		boolean digitsOnly = true;
		int longestWhitespace = 0;
		int shortestWhitespace = paragraph.length();
		
		for (int t = 0; t < paragraph.size(); t++) {
			Token token = paragraph.tokenAt(t);
			String value = token.getValue();
			charCount += value.length();
			
			for (int c = 0; c < value.length(); c++) {
				//	check for upper case
				if (Gamta.UPPER_CASE_LETTERS.indexOf(value.charAt(c)) != -1) upperCaseCount++;
				
				//	check if digits only (maybe page number)
				if (Gamta.DIGITS.indexOf(value.charAt(c)) == -1) digitsOnly = false;
			}
			
			//	check whitespace blocks (if lenghts too different, may be centered title)
			int whitespaceLength = paragraph.getWhitespaceAfter(t).length();
			if (whitespaceLength > longestWhitespace) longestWhitespace = whitespaceLength;
			if (whitespaceLength < shortestWhitespace) shortestWhitespace = whitespaceLength;
		}
		
		//	check for upper case
		if ((upperCaseCount * 2) > charCount)
			vote += (((upperCaseCount * 5) > (charCount * 2)) ? 2 : 1);
		
		//	check if digits only (maybe page number)
		if (digitsOnly && (paragraph.length() < 5))
			vote += 2; // page numbers with 5 or more digits are very unlikely
		
		//	check whitespace blocks (if lengths too different, may be centered title)
		if (((shortestWhitespace * 3) < longestWhitespace) && (longestWhitespace > 4))
			vote += (((shortestWhitespace * 5) < longestWhitespace) ? 2 : 1);
		
//		System.out.println(" - char counts are as follows:");
//		System.out.println("   - char count: " + charCount);
//		System.out.println("   - upper case count: " + upperCaseCount);
//		System.out.println("   - digits only: " + digitsOnly);
//		System.out.println("   - shortest whitespace: " + shortestWhitespace);
//		System.out.println("   - longest whitespace: " + longestWhitespace);
//		System.out.println("  ==> vote = " + vote);
		
		//	paragraph voted as page title
		return (vote >= 2);
	}
}