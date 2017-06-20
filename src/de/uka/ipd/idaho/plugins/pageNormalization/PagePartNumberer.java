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
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * @author sautter
 *
 */
public class PagePartNumberer extends AbstractAnalyzer implements LiteratureConstants {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	no pages given, use page breaks
		if (pages.length == 0) {
			
			//	get and check page breaks
			Annotation[] pageBreakTokens = data.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
			if (pageBreakTokens.length == 0)
				return;
			
			//	get paragraphs to find start
			Annotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			if (paragraphs.length == 0)
				return;
			
			//	compute start values
			int nextPageBreakTokenIndex = 0;
			String pageId = ((String) pageBreakTokens[0].getAttribute(PAGE_ID_ATTRIBUTE));				
			String pageNumber = ((String) pageBreakTokens[0].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			if (pageBreakTokens[0].getStartIndex() > paragraphs[0].getStartIndex()) {
				if (pageId != null) try {
					pageId = ("" + (Integer.parseInt(pageId) - 1));
				} catch (NumberFormatException nfe) {}
				if (pageNumber != null) try {
					pageNumber = ("" + (Integer.parseInt(pageNumber) - 1));
				} catch (NumberFormatException nfe) {}
			}
			
			//	attribute tokens with page IDs and numbers
			for (int t = paragraphs[0].getStartIndex(); t < data.size(); t++) {
				if ((nextPageBreakTokenIndex < pageBreakTokens.length) && (t == pageBreakTokens[nextPageBreakTokenIndex].getStartIndex())) {
					pageId = ((String) pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_ID_ATTRIBUTE));
					pageNumber = ((String) pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE));
					nextPageBreakTokenIndex++;
				}
				data.tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, ("" + pageId));
				data.tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageNumber));
			}
		}
		
		//	attribute tokens with page IDs and numbers of pages
		else for (int p = 0; p < pages.length; p++) {
			String pageId = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
			String pageNumber = ((String) pages[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			for (int t = 0; t < pages[p].size(); t++) {
				pages[p].tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageId);
				pages[p].tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			}
		}
		
		//	set page IDs and numbers of all annotations
		Annotation[] annotations = data.getAnnotations();
		for (int a = 0; a < annotations.length; a++) {
			Object pageId = annotations[a].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if (pageId != null)
				annotations[a].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			Object pageNumber = annotations[a].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber != null)
				annotations[a].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			
			Object lastPageId = annotations[a].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if ((lastPageId != null) && !lastPageId.equals(pageId))
				annotations[a].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageId);
			Object lastPageNumber = annotations[a].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if ((lastPageNumber != null) && !lastPageNumber.equals(pageNumber))
				annotations[a].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumber);
		}
		
		//	restore page breaks (just in case)
		HashSet seenPageIds = new HashSet();
		HashSet seenPageNumbers = new HashSet();
		int currentPageId = Integer.MIN_VALUE;
		int currentPageNumber = Integer.MIN_VALUE;
		for (int t = 0; t < data.size(); t++) {
			
			//	get page number
			int tokenPageId = currentPageId;
			try {
				tokenPageId = Integer.parseInt((String) data.tokenAt(t).getAttribute(PAGE_ID_ATTRIBUTE, ("" + tokenPageId)));
			} catch (NumberFormatException nfe) {}
			int tokenPageNumber = currentPageNumber;
			try {
				tokenPageNumber = Integer.parseInt((String) data.tokenAt(t).getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + tokenPageNumber)));
			} catch (NumberFormatException nfe) {}
			
			//	we are on a new page, mark it
			if ((tokenPageId != currentPageId) || (tokenPageNumber != currentPageNumber)) {
				currentPageId = tokenPageId;
				currentPageNumber = tokenPageNumber;
				Annotation pageBreak = data.addAnnotation(PAGE_BREAK_TOKEN_TYPE, t, 1);
				if (currentPageId != Integer.MIN_VALUE)
					pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, ("" + currentPageId));
				if (currentPageNumber != Integer.MIN_VALUE)
					pageBreak.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + currentPageNumber));
				if (seenPageIds.add(new Integer(currentPageId)) | seenPageNumbers.add(new Integer(currentPageNumber)))
					pageBreak.setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
			}
			
			//	clean up
			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
		
		//	remove duplicate page breaks and continue attribute
		AnnotationFilter.removeDuplicates(data, PAGE_BREAK_TOKEN_TYPE);
	}
}
