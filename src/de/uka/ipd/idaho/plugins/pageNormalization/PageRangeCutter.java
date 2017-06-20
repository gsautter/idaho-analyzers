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
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore;

/**
 * @author sautter
 */
public class PageRangeCutter extends AbstractAnalyzer implements ImagingConstants {
	
	private static final GPath startPagePath = new GPath("mods:mods/mods:relatedItem[./@type = 'host']/mods:part/mods:extent[./@unit = 'page']/mods:start");
	private static final GPath endPagePath = new GPath("mods:mods/mods:relatedItem[./@type = 'host']/mods:part/mods:extent[./@unit = 'page']/mods:end");
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	check page numbers
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		if (pages.length == 0)
			return;
		
		//	get page numbers
		int[] pns = new int[pages.length];
		for (int p = 0; p < pages.length; p++) {
			if (!pages[p].hasAttribute(PAGE_NUMBER_ATTRIBUTE))
				return;
			try {
				pns[p] = Integer.parseInt((String) pages[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			}
			catch (NumberFormatException nfe) {
				return;
			}
		}
		
		//	extract first and last page from MODS header
		Annotation[] spnas = startPagePath.evaluate(data, null);
		if ((spnas == null) || (spnas.length != 1))
			return;
		Annotation[] epnas = endPagePath.evaluate(data, null);
		if ((epnas == null) || (epnas.length != 1))
			return;
		int spn;
		try {
			spn = Integer.parseInt(spnas[0].firstValue());
		}
		catch (NumberFormatException nfe) {
			return;
		}
		int epn;
		try {
			epn = Integer.parseInt(epnas[0].firstValue());
		}
		catch (NumberFormatException nfe) {
			return;
		}
		
		//	mark pages for cutting
		boolean pagesToCut = false;
		for (int p = 0; p < pages.length; p++)
			if ((pns[p] < spn) || (pns[p] > epn)) {
				pns[p] = -1;
				pagesToCut = true;
			}
		if (!pagesToCut)
			return;
		
		//	copy page images (if any)
		if (data.hasAttribute(DOCUMENT_ID_ATTRIBUTE)) {
			
			//	compute new document ID and name
			String spns = ("" + spn);
			while (spns.length() < 4)
				spns = ("0" + spns);
			String epns = ("" + epn);
			while (epns.length() < 4)
				epns = ("0" + epns);
			String docId = ((String) data.getAttribute(DOCUMENT_ID_ATTRIBUTE));
			String nDocId = (docId.substring(0, (docId.length() - (spns.length() + epns.length() + 1))) + "X" + spns + epns);
			
			//	copy images of retained pages
			for (int p = 0; p < pages.length; p++) {
				
				//	this one goes out
				if (pns[p] == -1)
					continue;
				
				//	get page ID
				String pageIdString = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
				if ((pageIdString == null) || !pageIdString.matches("[0-9]+"))
					continue;
				int pageId = Integer.parseInt(pageIdString);
				
				//	copy image if it exists
				try {
					PageImage pi = PageImage.getPageImage(docId, pageId);
					if (pi == null)
						continue;
					if (!(pi.source instanceof PageImageStore))
						continue;
					//	TODO check existence of copy first
//					String piName = ((PageImageStore) pi.source).storePageImage(nDocId, pageId, pi);
//					pages[p].setAttribute(IMAGE_NAME_ATTRIBUTE, piName);
					String piName = PageImage.getPageImageName(nDocId, pageId);
					((PageImageStore) pi.source).storePageImage(piName, pi);
					pages[p].setAttribute(IMAGE_NAME_ATTRIBUTE, piName);
				}
				catch (IOException ioe) {
					System.out.println("Error copying image for page " + p + ": " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
			}
			
			//	update document ID and name
			data.setAttribute(DOCUMENT_ID_ATTRIBUTE, nDocId);
			if (data.hasAttribute(DOCUMENT_NAME_ATTRIBUTE)) {
				String docName = ((String) data.getAttribute(DOCUMENT_NAME_ATTRIBUTE));
				data.getAttribute(DOCUMENT_NAME_ATTRIBUTE, (docName + "(" + spns + "-" + epns + ")"));
			}
		}
		
		//	cut discarded pages
		for (int p = (pages.length - 1); p >= 0; p--) {
			if (pns[p] != -1)
				continue;
			//	rather cut paragraphs, and then remove pages that have further content
			MutableAnnotation[] paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int r = (paragraphs.length-1); r >= 0; r--)
				pages[p].removeTokens(paragraphs[r]);
			if (pages[p].size() != 0)
				data.removeAnnotation(pages[p]);
		}
	}
}