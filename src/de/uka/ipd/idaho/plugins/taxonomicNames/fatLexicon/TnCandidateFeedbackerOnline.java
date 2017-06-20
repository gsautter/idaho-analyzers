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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CheckBoxFeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TnCandidateFeedbackerOnline extends FatAnalyzer implements LiteratureConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(final MutableAnnotation data, Properties parameters) {
		
		//	make sure taxon names have page numbers
		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber != null) {
				Annotation[] pTnCandidates = paragraphs[p].getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
				for (int t = 0; t < pTnCandidates.length; t++)
					pTnCandidates[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			}
			Object pageId = paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE);
			if (pageId != null) {
				Annotation[] pTaxonNames = paragraphs[p].getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
				for (int t = 0; t < pTaxonNames.length; t++) {
					if (!pTaxonNames[t].hasAttribute(PAGE_ID_ATTRIBUTE)) // avoid too many attribute change events
						pTaxonNames[t].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
				}
			}
		}
		
		//	get taxonomic name and candidate Annotations
		Annotation[] taxonNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		Annotation[] tnCandidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	remove candidates contained in or containing a sure positive
		int tnIndex = 0;
		int candIndex = 0;
		ArrayList candCollector = new ArrayList();
		while ((tnIndex < taxonNames.length) && (candIndex < tnCandidates.length)) {
			
			//	candidate overlaps sure positive, remove it
			if (AnnotationUtils.overlaps(taxonNames[tnIndex], tnCandidates[candIndex]))
				data.removeAnnotation(tnCandidates[candIndex++]);
			
			//	switch to next sure positive
			else if (taxonNames[tnIndex].getEndIndex() <= tnCandidates[candIndex].getStartIndex())
				tnIndex ++;
			
			//	keep candidate and switch to next one
			else {
				candCollector.add(tnCandidates[candIndex]);
				candIndex ++;
			}
		}
		
		//	store remaining candidates (if any) and re-generate array
		while (candIndex < tnCandidates.length) {
			candCollector.add(tnCandidates[candIndex]);
			candIndex ++;
		}
		tnCandidates = ((Annotation[]) candCollector.toArray(new Annotation[candCollector.size()]));
		
		//	bucketize taxonomic name candidates
		HashMap bucketsByName = new LinkedHashMap();
		
		for (int t = 0; t < tnCandidates.length; t++) {
			String taxonNameKey = tnCandidates[t].getValue();
			if (bucketsByName.containsKey(taxonNameKey))
				((TnCandidateBucket) bucketsByName.get(taxonNameKey)).tnCandidates.add(tnCandidates[t]);
			
			else {
				TnCandidateBucket bucket = new TnCandidateBucket(tnCandidates[t]);
				bucketsByName.put(taxonNameKey, bucket);
			}
		}
		
		//	process buckets
		final TnCandidateBucket[] buckets = ((TnCandidateBucket[]) bucketsByName.values().toArray(new TnCandidateBucket[bucketsByName.size()]));
		
		//	don't show empty dialog
		if (buckets.length == 0)
			return;
		
		//	data structures for feedback data
		boolean[] isTaxon = new boolean[buckets.length];
		for (int r = 0; r < isTaxon.length; r++)
			isTaxon[r] = false;
		
		//	compute number of buckets per dialog
		int dialogCount = ((buckets.length + 9) / 10);
		int dialogSize = ((buckets.length + (dialogCount / 2)) / dialogCount);
		dialogCount = ((buckets.length + dialogSize - 1) / dialogSize);
		
		
		//	build dialogs
		CheckBoxFeedbackPanel[] cbfps = new CheckBoxFeedbackPanel[dialogCount];
		for (int d = 0; d < cbfps.length; d++) {
			
			cbfps[d] = new CheckBoxFeedbackPanel("Check Taxon Names Candidates");
			cbfps[d].setLabel("<HTML>Please review if the text parts printed in bold are <B>taxon names</B>." +
					"<BR>The surrounding non-bold text is meant to provide their context." +
					"<BR>Check the box left of the the candidate groups if they are.</HTML>");
			cbfps[d].setTrueSpacing(10);
			cbfps[d].setTrueColor(Color.GREEN);
			cbfps[d].setFalseSpacing(10);
			cbfps[d].setFalseColor(Color.WHITE);
			
			int dialogOffset = (d * dialogSize);
			
			//	add taxon name candidates
			for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < buckets.length); b++) {
				StringVector docNames = new StringVector();
				for (int a = 0; a < buckets[b + dialogOffset].tnCandidates.size(); a++) {
					Annotation taxonName = ((Annotation) buckets[b + dialogOffset].tnCandidates.get(a));
					Object pageNumber = taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE);
					docNames.addElementIgnoreDuplicates(buildLabel(data, taxonName, 10) + " (at " + taxonName.getStartIndex() + ((pageNumber == null) ? ", unknown page" : (" on page " + pageNumber)) + ")");
				}
				cbfps[d].addLine("<HTML>" + docNames.concatStrings("<BR>") + "</HTML>");
			}
			
			//	add backgroung information
			cbfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, ((Annotation) buckets[dialogOffset].tnCandidates.get(0)).getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((Annotation) buckets[dialogOffset].tnCandidates.get(0)).getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((Annotation) buckets[dialogOffset].tnCandidates.get(0)).getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			cbfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, data.getAnnotationID());
			cbfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString((Annotation[]) buckets[dialogOffset].tnCandidates.toArray(new Annotation[buckets[dialogOffset].tnCandidates.size()]));
			if (targetPages != null)
				cbfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString((Annotation[]) buckets[dialogOffset].tnCandidates.toArray(new Annotation[buckets[dialogOffset].tnCandidates.size()]));
			if (targetPageIDs != null)
				cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		int cutoffBucket = buckets.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(cbfps);
			
			//	process all feedback data together
			for (int d = 0; d < cbfps.length; d++) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < cbfps[d].lineCount(); b++)
					isTaxon[b + dialogOffset] = cbfps[d].getStateAt(b);
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < cbfps.length; d++) {
			if (d != 0)
				cbfps[d].addButton("Previous");
			cbfps[d].addButton("Cancel");
			cbfps[d].addButton("OK" + (((d+1) == cbfps.length) ? "" : " & Next"));
			
			String title = cbfps[d].getTitle();
			cbfps[d].setTitle(title + " - (" + (d+1) + " of " + cbfps.length + ")");
			
			String f = cbfps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			cbfps[d].setTitle(title); 
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < cbfps[d].lineCount(); b++)
					isTaxon[b + dialogOffset] = cbfps[d].getStateAt(b);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog on
			else {
				cutoffBucket = (d * dialogSize);
				d = cbfps.length;
			}
//			cbfps[d].addButton("OK");
//			cbfps[d].addButton("Cancel");
//			
//			String f = cbfps[d].getFeedback();
//			
//			//	current dialog submitted, process data
//			if ("OK".equals(f)) {
//				int dialogOffset = (d * dialogSize);
//				for (int b = 0; b < cbfps[d].lineCount(); b++)
//					isTaxon[b + dialogOffset] = cbfps[d].getStateAt(b);
//			}
//			
//			//	cancel from current dialog on
//			else {
//				cutoffBucket = (d * dialogSize);
//				d = cbfps.length;
//			}
		}
		
		//	process feedback
		for (int t = 0; t < cutoffBucket; t++) {
			if (isTaxon[t]) {
				for (int a = 0; a < buckets[t].tnCandidates.size(); a++) {
					Annotation tnca = ((Annotation) buckets[t].tnCandidates.get(a));
					tnca.changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
					tnca.setAttribute(FAT.EVIDENCE_ATTRIBUTE, "feedback");
					FAT.addTaxonName(new TaxonomicName(tnca));
				}
			}
			else for (int a = 0; a < buckets[t].tnCandidates.size(); a++) {
				Annotation tnca = ((Annotation) buckets[t].tnCandidates.get(a));
				if (tnca.size() == 1)
					FAT.negatives.addElementIgnoreDuplicates(tnca.firstValue());
				data.removeAnnotation(tnca);
			}
		}
	}
	
	private class TnCandidateBucket implements Comparable {
		private String candidateNameString;
		private ArrayList tnCandidates = new ArrayList();
		TnCandidateBucket(Annotation taxonName) {
			this.candidateNameString = taxonName.getValue();
			this.tnCandidates.add(taxonName);
		}
		public int compareTo(Object o) {
			if (o instanceof TnCandidateBucket) return this.candidateNameString.compareTo(((TnCandidateBucket) o).candidateNameString);
			else return -1;
		}
	}
	
	private String buildLabel(TokenSequence text, Annotation annot, int envSize) {
		int aStart = annot.getStartIndex();
		int aEnd = annot.getEndIndex();
		int start = Math.max(0, (aStart - envSize));
		int end = Math.min(text.size(), (aEnd + envSize));
		StringBuffer sb = new StringBuffer("... ");
		Token lastToken = null;
		Token token = null;
		for (int t = start; t < end; t++) {
			lastToken = token;
			token = text.tokenAt(t);
			
			//	end highlighting value
			if (t == aEnd) sb.append("</B>");
			
			//	add spacer
			if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
			
			//	start highlighting value
			if (t == aStart) sb.append("<B>");
			
			//	append token
			sb.append(token);
		}
		
		return sb.append(" ...").toString();
	}
}