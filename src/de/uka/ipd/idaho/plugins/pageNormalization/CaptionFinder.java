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

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class CaptionFinder extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final int MAX_CAPTION_LINES = 4;
	private static final String CAPTION_START_STRING = "figure;figures;table;tables;diagram;diagrams";
	private static final String CAPTION_START_ABBREVIATIONS_STRING = "fig;figs;tab;tabs;diag;diags";
	
	private StringVector captionStarts;
	private StringVector captionStartAbbreviations;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load caption starts
		try {
			InputStream is = this.dataProvider.getInputStream("captionStarts.txt");
			this.captionStarts = StringVector.loadList(is);
			is.close();
		}
		catch (IOException ioe) {
			this.captionStarts = new StringVector();
			this.captionStarts.parseAndAddElements(CAPTION_START_STRING, ";");
		}
		
		//	load caption start abbreviations
		try {
			InputStream is = this.dataProvider.getInputStream("captionStartAbbreviations.txt");
			this.captionStartAbbreviations = StringVector.loadList(is);
			is.close();
		}
		catch (IOException ioe) {
			this.captionStartAbbreviations = new StringVector();
			this.captionStartAbbreviations.parseAndAddElements(CAPTION_START_ABBREVIATIONS_STRING, ";");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	check individual paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			
			//	check number of lines
			int paragraphLines = 0;
			for (int t = 0; t < paragraph.size(); t++)
				if (paragraph.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) paragraphLines++;
			
			if (paragraphLines <= MAX_CAPTION_LINES)
				
				//	check other evidence
				if (this.isCaption(paragraph))
					data.addAnnotation(CAPTION_TYPE, paragraph.getStartIndex(), paragraph.size());
		}
		
		//	remove footnotes containing page numbers
		AnnotationFilter.removeContaining(data, CAPTION_TYPE, PAGE_NUMBER_TYPE);
		
		//	remove page titles in captions
		AnnotationFilter.removeContained(data, CAPTION_TYPE, PAGE_TITLE_TYPE);
	}
	
	private boolean isCaption(TokenSequence paragraph) {
		
		//	catch empty paragraphs
		if (paragraph.size() < 2)
			return false;
		
		//	indicator word followed by reference number or letter
		if (this.captionStarts.containsIgnoreCase(paragraph.valueAt(0)) &&
				(
					Gamta.isNumber(paragraph.tokenAt(1)) ||
					((paragraph.tokenAt(1).length() == 1) && (Gamta.LETTERS.indexOf(paragraph.valueAt(1)) != -1)) ||
					Gamta.isRomanNumber(paragraph.tokenAt(1))
				)
			) return true;
		
		//	catch empty paragraphs
		if (paragraph.size() < 3)
			return false;
		
		//	indicator word abbreviation followed by reference number or letter
		if (this.captionStartAbbreviations.containsIgnoreCase(paragraph.valueAt(0)) &&
			paragraph.valueAt(1).equals(".") &&
				(
					Gamta.isNumber(paragraph.tokenAt(2)) ||
					((paragraph.tokenAt(2).length() == 1) && (Gamta.LETTERS.indexOf(paragraph.valueAt(2)) != -1)) ||
					Gamta.isRomanNumber(paragraph.tokenAt(2))
				)
			) return true;
		
		//	check indexing letters
		StringVector indexLetters = new StringVector();
		for (int t = 0; t < paragraph.size(); t++) {
			Token token = paragraph.tokenAt(t);
			if (Gamta.isWord(token) && (token.length() == 1) && Gamta.isLowerCaseWord(token))
				indexLetters.addElementIgnoreDuplicates(token.getValue());
		}
		
		//	search sequences in index letters
		int indexingLetters = 0;
		for (int s = 0; s < indexLetters.size(); s++) {
			int score = 0;
			for (int o = s; o < indexLetters.size(); o++) {
				if (indexLetters.get(s).charAt(0) == (indexLetters.get(o).charAt(0) + (o - s)))
					score ++;
				else score--;
			}
			if (score > indexingLetters)
				indexingLetters = score;
		}
		
		//	caption if more than one indexing letter
		return (indexingLetters > 1);
	}
}