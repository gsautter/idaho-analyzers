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
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * @author sautter
 *
 */
public class CitationFinder extends AbstractAnalyzer implements LiteratureConstants {
	
	private static final int MAX_CITATION_LINES = 4;
	private static final int MAX_CITATION_LENGHT = 100;
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
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
			
			if ((paragraph.size() <= MAX_CITATION_LENGHT) && (paragraphLines <= MAX_CITATION_LINES)) {
				
				//	check other evidence
				if (this.isCitation(paragraph))
					data.addAnnotation(CITATION_TYPE, paragraph.getStartIndex(), paragraph.size());
			}
		}
	}
	
	//	regular expression for finding number ranges (might be page ranges in citations)
	private static final String numberRangeRegEx = "(([1-9][0-9]*+)\\s\\-\\s([1-9][0-9]*+))";
	private static final String yearRegEx = "([12][0-9]{3})";
	
	private boolean isCitation(MutableAnnotation paragraph) {
		
		//	check capitalized words and punctuation
		int wordCount = 0;
		int capWordCount = 0;
		int punctCount = 0;
		for (int t = 0; t < paragraph.size(); t++) {
			Token token = paragraph.tokenAt(t);
			if (Gamta.isWord(token)) {
				wordCount ++;
				if (Gamta.isFirstLetterUpWord(token))
					capWordCount ++;
			} else if (Gamta.isPunctuation(token)) punctCount ++;
		}
		
		//	title case
		if ((capWordCount * 3) > (wordCount * 2)) return true;
		
		//	check for page number ranges
		Annotation[] numberRanges = Gamta.extractAllMatches(paragraph, numberRangeRegEx, 3);
		
		//	check for page number ranges
		Annotation[] years = Gamta.extractAllMatches(paragraph, yearRegEx, 1);
		
		//	compute result
		return ((numberRanges.length + years.length + (((capWordCount * 2) > wordCount) ? 1 : 0)) > 2);
	}
}
