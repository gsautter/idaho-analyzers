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

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * @author sautter
 *
 */
public class PageBorderFinder extends AbstractAnalyzer implements LiteratureConstants {
	
	private static final int MIN_PAGE_BORDER_CHARS = 20;
	private static final int MAX_PAGE_BORDER_TOKENS = 40;
	private static final int MAX_PAGE_BORDER_LINES = 1;
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	check individual paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			
			//	check paragraph length
			if (paragraph.size() <= MAX_PAGE_BORDER_TOKENS) {
				
				//	check number of lines
				int paragraphLines = 0;
				for (int t = 0; t < paragraph.size(); t++)
					if (paragraph.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) paragraphLines++;
				
				if (paragraphLines <= MAX_PAGE_BORDER_LINES) {
					
					//	vote other evidence
//					int vote = 0;
					
					int punctuationCount = 0;
					int charCount = 0;
					
					for (int t = 0; t < paragraph.size(); t++) {
						Token token = paragraph.tokenAt(t);
						String value = token.getValue();
						charCount += value.length();
						
						//	check for punctuation
						for (int c = 0; c < value.length(); c++)
							if ((Gamta.PUNCTUATION.indexOf(value.charAt(c)) != -1) && (value.charAt(c) != '.')) punctuationCount++;
					}
					
//					//	check for punctuation
//					if ((punctuationCount * 2) > charCount)
//						vote += (((punctuationCount * 3) > charCount) ? 2 : 1);
//					
//					//	paragraph voted as page border
//					if (vote >= 2) 
//						data.addAnnotation(PAGE_BORDER_TYPE, paragraph.getStartIndex(), paragraph.size());
					
					if (((punctuationCount * 3) > (2 * charCount)) && (charCount >= MIN_PAGE_BORDER_CHARS))
						data.addAnnotation(PAGE_BORDER_TYPE, paragraph.getStartIndex(), paragraph.size());
				}
			}
		}
	}
}
