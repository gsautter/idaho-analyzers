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

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.goldenGateServer.client.ServerConnection;
import de.uka.ipd.idaho.goldenGateServer.dio.client.GoldenGateDioClient;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticatedClient;

/**
 * @author sautter
 *
 */
public class PageIdReverseEngineerer extends AbstractAnalyzer implements LiteratureConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	change page start tokens to (the more generic) page break tokens
		AnnotationFilter.renameAnnotations(data, "pageStartToken", PAGE_BREAK_TOKEN_TYPE);
		
		//	get paragraphs to find start
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		if (paragraphs.length == 0)
			return;
		
		//	get first page number (also offset for page IDs)
		//	TODO think of using MODS header as source of page number, and of overwriting page numbers of paragraphs
		int pageNumber = Integer.parseInt((String) data.getAttribute(PAGE_NUMBER_ATTRIBUTE, "0"));
		int pageIdOffset = pageNumber;
		
		//	set token attributes within paragraphs (we know they are there for server data set)
		for (int p = 0; p < paragraphs.length; p++) {
			pageNumber = Integer.parseInt((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageNumber)));
			int pageId = Integer.parseInt((String) paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE, "-1"));
			if (pageId == -1)
				pageId = pageNumber - pageIdOffset;
			System.out.println("Doing paragraph " + (p+1) + " of " + paragraphs.length + ", page number is " + pageNumber + ", page ID is " + pageId);
			
			//	attribute tokens with page IDs and numbers
			Annotation[] pageBreakTokens = paragraphs[p].getAnnotations(PAGE_BREAK_TOKEN_TYPE);
			int nextPageBreakTokenIndex = 0;
			for (int t = 0; t < paragraphs[p].size(); t++) {
				if ((nextPageBreakTokenIndex < pageBreakTokens.length) && (t == pageBreakTokens[nextPageBreakTokenIndex].getStartIndex())) {
					pageNumber = Integer.parseInt((String) pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageNumber)));
					pageId = Integer.parseInt((String) pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_ID_ATTRIBUTE, "-1"));
					if (pageId == -1)
						pageId = pageNumber - pageIdOffset;
					nextPageBreakTokenIndex++;
					System.out.println("  switched page number to " + pageNumber + ", page ID to " + pageId);
				}
				paragraphs[p].tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, ("" + pageId));
				paragraphs[p].tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + pageNumber));
			}
		}
		
		//	set page IDs and numbers of all annotations
		Annotation[] annotations = data.getAnnotations();
		for (int a = 0; a < annotations.length; a++) {
			Object pageIdO = annotations[a].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
			annotations[a].setAttribute(PAGE_ID_ATTRIBUTE, pageIdO);
			Object pageNumberO = annotations[a].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			annotations[a].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberO);
			
			Object lastPageIdO = annotations[a].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if ((lastPageIdO != null) && !lastPageIdO.equals(pageIdO))
				annotations[a].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageIdO);
			Object lastPageNumberO = annotations[a].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if ((lastPageNumberO != null) && !lastPageNumberO.equals(pageNumberO))
				annotations[a].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumberO);
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
	
	static String proxyUrl = "http://plazi2.cs.umb.edu/GgServer/proxy";
	public static void main(String[] args) throws Exception {
		AuthenticatedClient ac = AuthenticatedClient.getAuthenticatedClient(ServerConnection.getServerConnection(proxyUrl));
		ac.login("admin", "taxonx");
		String docId = "56B3F6AF314B0BF1CD4E88C46E9E0FF0";
		GoldenGateDioClient dioc = new GoldenGateDioClient(ac);
		MutableAnnotation doc = dioc.getDocument(docId);
		
		PageIdReverseEngineerer pist = new PageIdReverseEngineerer();
		pist.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/PageNormalizationData/")));
		pist.process(doc, new Properties());
		AnnotationUtils.writeXML(doc, new PrintWriter(System.out));
	}
}