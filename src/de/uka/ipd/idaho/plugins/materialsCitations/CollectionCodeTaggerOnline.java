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

package de.uka.ipd.idaho.plugins.materialsCitations;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CheckBoxFeedbackPanel;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class CollectionCodeTaggerOnline extends AbstractAnalyzer implements LiteratureConstants, MaterialsCitationConstants {
	
	private Properties collectionCodes = new Properties();
	
	private Properties collectionCodesAdded = new Properties();
	private boolean collectionCodesAddedModified = false;
	
	private StringVector collectionCodesExclude = new StringVector();
	private boolean collectionCodesExcludeModified = false;
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractAnalyzer#setDataProvider(de.gamta.util.AnalyzerDataProvider)
	 */
	public void setDataProvider(AnalyzerDataProvider dataProvider) {
		super.setDataProvider(dataProvider);
		
		InputStream is = null;
		try {
			is = this.dataProvider.getInputStream("collectionCodes.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				int split = line.indexOf(' ');
				if (split != -1) {
					String cc = line.substring(0, split).trim();
					String cn = line.substring(split).trim();
					if ((cc.length() != 0) && (cn.length() != 0))
						this.collectionCodes.setProperty(cc, cn);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading collection codes.");
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {}
		}
		
		try {
			is = this.dataProvider.getInputStream("collectionCodesAdded.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				int split = line.indexOf(' ');
				if (split != -1) {
					String cc = line.substring(0, split).trim();
					String cn = line.substring(split).trim();
					if ((cc.length() != 0) && (cn.length() != 0))
						this.collectionCodesAdded.setProperty(cc, cn);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading additional collection codes.");
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {}
		}
		
		try {
			is = this.dataProvider.getInputStream("collectionCodesExclude.txt");
			this.collectionCodesExclude = StringVector.loadList(is);
		}
		catch (IOException ioe) {
			System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading exclusion list.");
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractAnalyzer#exit()
	 */
	public void exit() {
		OutputStream os = null;
		
		if (this.collectionCodesAddedModified && this.dataProvider.isDataEditable("collectionCodesAdded.txt")) {
			try {
				StringVector ccLines = new StringVector();
				Iterator ccIt = this.collectionCodesAdded.keySet().iterator();
				while (ccIt.hasNext()) {
					String cc = ((String) ccIt.next());
					String cn = this.collectionCodesAdded.getProperty(cc);
					ccLines.addElement(cc + " " + cn);
				}
				ccLines.sortLexicographically(false, false);
				os = this.dataProvider.getOutputStream("collectionCodesAdded.txt");
				ccLines.storeContent(os);
				os.flush();
			}
			catch (IOException ioe) {
				System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while storing modified additional collection codes.");
				ioe.printStackTrace(System.out);
			}
			finally {
				if (os != null) try {
					os.close();
				} catch (IOException e) {}
			}
		}
		
		if (this.collectionCodesExcludeModified && this.dataProvider.isDataEditable("collectionCodesExclude.txt")) {
			try {
				os = this.dataProvider.getOutputStream("collectionCodesExclude.txt");
				this.collectionCodesExclude.storeContent(os);
				os.flush();
			}
			catch (IOException ioe) {
				System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while storing modified exclusion list.");
				ioe.printStackTrace(System.out);
			}
			finally {
				if (os != null) try {
					os.close();
				} catch (IOException e) {}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		ArrayList collectionCodeCandidates = new ArrayList();
		
		//	annotate known collection codes, and collect candidates
		for (int v = 0; v < data.size(); v++) {
			String value = data.valueAt(v);
			
			//	check if collection code
			if (this.collectionCodes.containsKey(value) || this.collectionCodesAdded.containsKey(value)) {
				
				//	annotate
				Annotation collectionCode = data.addAnnotation(COLLECTION_CODE, v, 1);
				
				//	resolve collection code (get name)
				String collectionName = this.resolveCollectionCode(value);
				if (collectionName != null) collectionCode.setAttribute(COLLECTION_NAME_ATTRIBUTE, collectionName);
			}
			
			//	collect candidate collection codes
			else if (!this.collectionCodesExclude.contains(value) && value.matches("[A-Z]{3,5}"))
				collectionCodeCandidates.add(Gamta.newAnnotation(data, COLLECTION_CODE, v, 1));
		}
		
		//	user interaction not allowed, forget about candidates
		if (!parameters.containsKey(INTERACTIVE_PARAMETER) || collectionCodeCandidates.isEmpty())
			return;
		
		//	bucketize candidates
		HashMap bucketsByCollectionCode = new LinkedHashMap();
		for (int c = 0; c < collectionCodeCandidates.size(); c++) {
			Annotation collectionCodeCandidate = ((Annotation) collectionCodeCandidates.get(c));
			CcBucket collectionCodeBucket = ((CcBucket) bucketsByCollectionCode.get(collectionCodeCandidate.getValue()));
			if (collectionCodeBucket == null) {
				collectionCodeBucket = new CcBucket(collectionCodeCandidate.getValue());
				bucketsByCollectionCode.put(collectionCodeBucket.collectionCode, collectionCodeBucket);
			}
			collectionCodeBucket.collectionCodeAnnotations.add(collectionCodeCandidate);
		}
		
		
		//	process buckets
		final CcBucket[] buckets = ((CcBucket[]) bucketsByCollectionCode.values().toArray(new CcBucket[bucketsByCollectionCode.size()]));
		
		//	don't show empty dialog
		if (buckets.length != 0) {
			
			//	data structures for feedback data
			boolean[] isCollectionCode = new boolean[buckets.length];
			for (int r = 0; r < isCollectionCode.length; r++)
				isCollectionCode[r] = false;
			
			//	compute number of buckets per dialog
			int dialogCount = ((buckets.length + 9) / 10);
			int dialogSize = ((buckets.length + (dialogCount / 2)) / dialogCount);
			dialogCount = ((buckets.length + dialogSize - 1) / dialogSize);
			
			
			//	build dialogs
			CheckBoxFeedbackPanel[] cbfps = new CheckBoxFeedbackPanel[dialogCount];
			for (int d = 0; d < cbfps.length; d++) {
				
				cbfps[d] = new CheckBoxFeedbackPanel("Check Collection Codes");
				cbfps[d].setLabel("<HTML>Please review if the acronyms (printed in bold with surrounding context) are <B>collection codes</B>." +
						"<BR>Check the box on their left if they are, un-check it otherwise.</HTML>");
				cbfps[d].setTrueSpacing(10);
				cbfps[d].setTrueColor(Color.GREEN);
				cbfps[d].setFalseSpacing(10);
				cbfps[d].setFalseColor(Color.WHITE);
				
				int dialogOffset = (d * dialogSize);
				
				//	add collection code candidates
				for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < buckets.length); b++) {
					StringVector docNames = new StringVector();
					for (int a = 0; a < buckets[b + dialogOffset].collectionCodeAnnotations.size(); a++) {
						Annotation collectionCode = ((Annotation) buckets[b + dialogOffset].collectionCodeAnnotations.get(a));
						Object pageNumber = collectionCode.getAttribute(PAGE_NUMBER_ATTRIBUTE);
						docNames.addElementIgnoreDuplicates(buildLabel(data, collectionCode, 10) + " (at " + collectionCode.getStartIndex() + ((pageNumber == null) ? ", unknown page" : (" on page " + pageNumber)) + ")");
					}
					cbfps[d].addLine("<HTML>" + docNames.concatStrings("<BR>") + "</HTML>");
				}
				
				//	add backgroung information
				cbfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, ((Annotation) buckets[dialogOffset].collectionCodeAnnotations.get(0)).getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
				cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((Annotation) buckets[dialogOffset].collectionCodeAnnotations.get(0)).getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
				cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((Annotation) buckets[dialogOffset].collectionCodeAnnotations.get(0)).getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
				cbfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, data.getAnnotationID());
				cbfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, COLLECTION_CODE);
				
				//	add target page numbers
				String targetPages = FeedbackPanel.getTargetPageString((Annotation[]) buckets[dialogOffset].collectionCodeAnnotations.toArray(new Annotation[buckets[dialogOffset].collectionCodeAnnotations.size()]));
				if (targetPages != null)
					cbfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
				String targetPageIDs = FeedbackPanel.getTargetPageIdString((Annotation[]) buckets[dialogOffset].collectionCodeAnnotations.toArray(new Annotation[buckets[dialogOffset].collectionCodeAnnotations.size()]));
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
						isCollectionCode[b + dialogOffset] = cbfps[d].getStateAt(b);
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
						isCollectionCode[b + dialogOffset] = cbfps[d].getStateAt(b);
				}
				
				//	back to previous dialog
				else if ("Previous".equals(f)) {
					d-=2;
				}
				
				//	cancel from current dialog on
				else {
					cutoffBucket = (d * dialogSize);
					d = cbfps.length;
				}
//				cbfps[d].addButton("OK");
//				cbfps[d].addButton("Cancel");
//				
//				String f = cbfps[d].getFeedback();
//				
//				//	current dialog submitted, process data
//				if ("OK".equals(f)) {
//					int dialogOffset = (d * dialogSize);
//					for (int b = 0; b < cbfps[d].lineCount(); b++)
//						isCollectionCode[b + dialogOffset] = cbfps[d].getStateAt(b);
//				}
//				
//				//	cancel from current dialog on
//				else {
//					cutoffBucket = (d * dialogSize);
//					d = cbfps.length;
//				}
			}
			
			//	process feedback
			for (int b = 0; b < cutoffBucket; b++) {
				String collectionCode = buckets[b].collectionCode;
				
				//	collection code
				if (isCollectionCode[b]) {
					
					//	remember
					String collectionName = buckets[b].collectionCode;
					if (collectionName.length() == 0)
						collectionName = collectionCode;
					
					this.collectionCodesAdded.setProperty(collectionCode, collectionName);
					this.collectionCodesAddedModified = true;
					
					//	annotate
					for (int c = 0; c < buckets[b].collectionCodeAnnotations.size(); c++) {
						Annotation collectionCodeAnnotation = ((Annotation) buckets[b].collectionCodeAnnotations.get(c));
						collectionCodeAnnotation = data.addAnnotation(COLLECTION_CODE, collectionCodeAnnotation.getStartIndex(), 1);
						collectionCodeAnnotation.setAttribute(COLLECTION_NAME_ATTRIBUTE, collectionName);
					}
				}
				
				//	excluded, remember exclusion
				else {
					this.collectionCodesExclude.addElementIgnoreDuplicates(collectionCode);
					this.collectionCodesExcludeModified = true;
				}
			}
		}
	}
	
	private class CcBucket {
		private String collectionCode;
		private ArrayList collectionCodeAnnotations = new ArrayList();
		private CcBucket(String cc) {
			this.collectionCode = cc;
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
	
	private String resolveCollectionCode(String collectionCode) {
		String collectionName = this.collectionCodes.getProperty(collectionCode);
		
		//	check additional (learned) codes
		if (collectionName == null)
			collectionName = this.collectionCodesAdded.getProperty(collectionCode);
		
		//	catch unknown codes
		if (collectionName == null) return null;
		
		//	pure reference
		if (collectionName.startsWith("see ")) {
			String referencedCollectionCode = collectionName.substring(4).trim();
			return this.resolveCollectionCode(referencedCollectionCode);
		}
		
		//	actual collection name
		else {
			
			//	check if reference included
			int see = collectionName.indexOf(" see ");
			
			//	no reference
			if (see == -1) return collectionName;
			
			//	subordinate reference to resolve
			else {
				String referencedCollectionCode = collectionName.substring(see + 4).trim();
				if (referencedCollectionCode.indexOf(' ') != -1)
					referencedCollectionCode = referencedCollectionCode.substring(0, referencedCollectionCode.indexOf(' '));
				String referencedCollectionName = this.resolveCollectionCode(referencedCollectionCode);
				
				//	reference could not be resolved
				if (referencedCollectionName == null) return collectionName;
				
				//	build new name string
				else return (collectionName.substring(0, see) + " see " + referencedCollectionName);
			}
		}
	}
}