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
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * @author sautter
 *
 */
public class PageDissolver extends AbstractAnalyzer implements LiteratureConstants {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	remove page borders and titles
		Annotation[] borders = data.getAnnotations(PAGE_BORDER_TYPE);
		for (int b = 0; b < borders.length; b++)
			data.removeTokens(borders[b]);
		
		//	remove page titles
		Annotation[] pageTitles = data.getAnnotations(PAGE_TITLE_TYPE);
		for (int t = 0; t < pageTitles.length; t++)
			data.removeTokens(pageTitles[t]);
		
		//	restore page break tokens
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		for (int p = 0; p < pages.length; p++) {
			Annotation pageBreak = pages[p].addAnnotation(PAGE_BREAK_TOKEN_TYPE, 0, 1);
			pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
			pageBreak.setAttribute(PAGE_NUMBER_ATTRIBUTE, pages[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			pageBreak.setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
		}
		AnnotationFilter.removeDuplicates(data, PAGE_BREAK_TOKEN_TYPE);
		
		//	remove pages
		AnnotationFilter.removeAnnotations(data, PAGE_TYPE);
		
		/*
		 * As of GoldenGATE version 3, re-merging paragraphs around page borders
		 * has been moved to ParagraphRestorer, which also takes care of
		 * captions and footnotes nested in logical main text paragraphs
		 */
	}
}
