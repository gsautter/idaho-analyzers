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
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * @author sautter
 *
 */
public class PageMarker extends AbstractAnalyzer implements LiteratureConstants {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get page borders
		Annotation[] pageBorders = data.getAnnotations(PAGE_BORDER_TYPE);
		if (pageBorders.length == 0) return;
		
		//	bundle paragraphs to pages
		int pageStart = 0;
		for (int b = 0; b < pageBorders.length; b++) {
			if (pageBorders[b].getStartIndex() > pageStart) {
				data.addAnnotation(PAGE_TYPE, pageStart, (pageBorders[b].getStartIndex() - pageStart));
				pageStart = pageBorders[b].getEndIndex();
			}
		}
		
		if (pageStart < data.size())
			data.addAnnotation(PAGE_TYPE, pageStart, (data.size() - pageStart));
		
		//	set page IDs (we don't have page numbers so far), and mark page starts
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		for (int p = 0; p < pages.length; p++) {
			pages[p].setAttribute(PAGE_ID_ATTRIBUTE, ("" + p));
			Annotation pageBreak = pages[p].addAnnotation(PAGE_BREAK_TOKEN_TYPE, 0, 1);
			pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, ("" + p));
			pageBreak.setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
		}
	}
}
