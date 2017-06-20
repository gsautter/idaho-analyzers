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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * @author sautter
 *
 */
public class PageNormalizer extends AbstractConfigurableAnalyzer implements ImagingConstants {
	
	private String pageNumberPattern;
	private HashMap pageNumberCharacterTranslations = new HashMap();
	
	private static final String LINE_ANNOTATION_TYPE = "line";
	private static final String PARAGRAPH_START_ATTRIBUTE = "paragraphStart";
	
	private static final String CONTINUE_CATEGORY = "Continue";
	
//	private String localImageProvider = "http://localhost:8888/images/";
	private String remoteImageProvider = (IMAGE_BASE_PATH_VARIABLE + "/");
	
	//	subSection types for complex rules
	private static final String MAIN_TEXT_TYPE = "mainText";
	private static final String RESTART_MAIN_TEXT_TYPE = "restartMainText";
	private static final String RESTART_FOOTNOTE_TYPE = "restartFootnote";
	private static final String HEADING_TYPE = "heading";
	private static final String TABLE_TYPE = "table";
	private static final String ENUMERATION_ITEM_TYPE = "enumerationItem";
	private static final String BULLETIN_LIST_ITEM_TYPE = "bulletinListItem";
	private static final String OCR_ARTIFACT_TYPE = "ocrArtifact";
	
	//	categories for complex rules
	private static final String MAIN_TEXT_CATEGORY = "Start Main Text Paragraph";
	private static final String RESTART_MAIN_TEXT_CATEGORY = "ReStart Interrupted Main Text Paragraph";
	private static final String CAPTION_CATEGORY = "Start Caption";
	private static final String FOOTNOTE_CATEGORY = "Start Footnote";
	private static final String RESTART_FOOTNOTE_CATEGORY = "ReStart Interrupted Footnote";
	private static final String CITATION_CATEGORY = "Start Citation / Bibliographic Reference";
	private static final String PAGE_TITLE_CATEGORY = "Start Page Header";
	private static final String HEADING_CATEGORY = "Start Heading";
	private static final String TABLE_CATEGORY = "Start Heading";
	private static final String ENUMERATION_ITEM_CATEGORY = "Start Enumeration Item";
	private static final String BULLETIN_LIST_ITEM_CATEGORY = "Start Bulletin List Item";
	private static final String OCR_ARTIFACT_CATEGORY = "OCR Artifact";
	
	private static final String[] LINE_CATEGORIES = {
		MAIN_TEXT_CATEGORY,
		RESTART_MAIN_TEXT_CATEGORY,
		CAPTION_CATEGORY,
		TABLE_CATEGORY,
		FOOTNOTE_CATEGORY,
		RESTART_FOOTNOTE_CATEGORY,
		CITATION_CATEGORY,
		PAGE_TITLE_CATEGORY,
		HEADING_CATEGORY,
		ENUMERATION_ITEM_CATEGORY,
		BULLETIN_LIST_ITEM_CATEGORY,
		OCR_ARTIFACT_CATEGORY,
		CONTINUE_CATEGORY,
	};
	
	private static String getTypeForCategory(String category) {
		if (MAIN_TEXT_CATEGORY.equals(category)) return MAIN_TEXT_TYPE;
		else if (RESTART_MAIN_TEXT_CATEGORY.equals(category)) return RESTART_MAIN_TEXT_TYPE;
		else if (CAPTION_CATEGORY.equals(category)) return CAPTION_TYPE;
		else if (FOOTNOTE_CATEGORY.equals(category)) return FOOTNOTE_TYPE;
		else if (RESTART_FOOTNOTE_CATEGORY.equals(category)) return RESTART_FOOTNOTE_TYPE;
		else if (CITATION_CATEGORY.equals(category)) return CITATION_TYPE;
		else if (PAGE_TITLE_CATEGORY.equals(category)) return PAGE_TITLE_TYPE;
		else if (HEADING_CATEGORY.equals(category)) return HEADING_TYPE;
		else if (ENUMERATION_ITEM_CATEGORY.equals(category)) return ENUMERATION_ITEM_TYPE;
		else if (BULLETIN_LIST_ITEM_CATEGORY.equals(category)) return BULLETIN_LIST_ITEM_TYPE;
		else if (OCR_ARTIFACT_CATEGORY.equals(category)) return OCR_ARTIFACT_TYPE;
		else if (TABLE_CATEGORY.equals(category)) return TABLE_TYPE;
		else return null;
	}
	
	private static String getCategoryForType(String type) {
		if (MAIN_TEXT_TYPE.equals(type)) return MAIN_TEXT_CATEGORY;
		else if (RESTART_MAIN_TEXT_TYPE.equals(type)) return RESTART_MAIN_TEXT_CATEGORY;
		else if (CAPTION_TYPE.equals(type)) return CAPTION_CATEGORY;
		else if (FOOTNOTE_TYPE.equals(type)) return FOOTNOTE_CATEGORY;
		else if (RESTART_FOOTNOTE_TYPE.equals(type)) return RESTART_FOOTNOTE_CATEGORY;
		else if (CITATION_TYPE.equals(type)) return CITATION_CATEGORY;
		else if (PAGE_TITLE_TYPE.equals(type)) return PAGE_TITLE_CATEGORY;
		else if (HEADING_TYPE.equals(type)) return HEADING_CATEGORY;
		else if (ENUMERATION_ITEM_TYPE.equals(type)) return ENUMERATION_ITEM_CATEGORY;
		else if (BULLETIN_LIST_ITEM_TYPE.equals(type)) return BULLETIN_LIST_ITEM_CATEGORY;
		else if (OCR_ARTIFACT_TYPE.equals(type)) return OCR_ARTIFACT_CATEGORY;
		else if (TABLE_TYPE.equals(type)) return TABLE_CATEGORY;
		else return CONTINUE_CATEGORY;
	}
	
	private static final String PAGE_NUMBER_CANDIDATE_TYPE = (PAGE_NUMBER_TYPE + "Candidate");
	
	private static final boolean DEBUG_PAGE_NUMBERS = false;
	private static final void getAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length) {
			StringBuffer value = new StringBuffer();
			for (int d = 0; d < digits.length; d++)
				value.append("" + digits[d]);
			values.add(value.toString());
		}
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			getAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		if (pages.length == 0)
			return;
		
		//	make sure lines are annotated
		for (int p = 0; p < pages.length; p++) {
			
			//	get lines
			Annotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	make sure lines are annotated
			if (lines.length == 0) {
				int lineStart = 0;
				for (int t = 0; t < pages[p].size(); t++)
					if (pages[p].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) || ((t+1) == pages[p].size())) {
						pages[p].addAnnotation(LINE_ANNOTATION_TYPE, lineStart, (t - lineStart + 1));
						lineStart = t+1;
					}
				pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			}
			
			//	mark paragraph starts
			MutableAnnotation[] paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int pp = 0; pp < paragraphs.length; pp++) {
				lines = paragraphs[pp].getAnnotations(LINE_ANNOTATION_TYPE);
				if (lines.length != 0)
					lines[0].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
			}
		}
		
		//	mark page numbers
		this.markPageNumbers(pages);
		
		//	mark caption starts
		this.markCaptionStartLines(pages);
		
		//	mark page titles
		this.markPageTitleLines(pages);
		
		//	mark footnote starts
		this.markFootnoteStartLines(pages);
		
		//	find citations
		this.markCitationStartLines(pages);
		
		
		//	mark remaining paragraph start lines & build feedback panels
		PageNormalizerFeedbackPanel[] scfps = new PageNormalizerFeedbackPanel[pages.length];
		for (int p = 0; p < pages.length; p++) {
			QueriableAnnotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
			for (int l = 0; l < lines.length; l++) {
				
				//	paragraph start or paragraph restart?
				if (!lines[l].hasAttribute(TYPE_ATTRIBUTE) && lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE))
					lines[l].setAttribute(TYPE_ATTRIBUTE, (lines[l].firstValue().matches("[a-z][a-zA-Z\\-\\']++") ? RESTART_MAIN_TEXT_TYPE : MAIN_TEXT_TYPE));
			}
			
			//	build feedback panels
			scfps[p] = this.getFeedbackPanel(pages[p], lines);
		}
		
		
		//	get feedback (watch out for cancellations in desktop use)
		int cutoff = pages.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			
			//	get feedback
			FeedbackPanel.getMultiFeedback(scfps);
			
			//	process all feedback data together
			for (int p = 0; p < pages.length; p++) {
				QueriableAnnotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
				
				//	read data from dialog
				for (int l = 0; l < lines.length; l++) {
					String lineType = getTypeForCategory(scfps[p].getCategoryAt(l));
					lines[l].setAttribute(TYPE_ATTRIBUTE, lineType);
					lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, ((lineType == null) ? null : PARAGRAPH_START_ATTRIBUTE));
				}
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int p = 0; p < pages.length; p++) {
			if (p != 0)
				scfps[p].addButton("Previous");
			scfps[p].addButton("Cancel");
			scfps[p].addButton("OK" + (((p+1) == scfps.length) ? "" : " & Next"));
			
			String title = scfps[p].getTitle();
			scfps[p].setTitle(title + " - (" + (p+1) + " of " + scfps.length + ")");
			
			System.out.println("Getting feedback");
			String f = scfps[p].getFeedback();
			if (f == null) f = "Cancel";
			
			scfps[p].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				QueriableAnnotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
				
				//	read data from dialog
				for (int l = 0; l < lines.length; l++) {
					String lineType = getTypeForCategory(scfps[p].getCategoryAt(l));
					if (lineType == null) {
						lines[l].removeAttribute(TYPE_ATTRIBUTE);
						lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
					}
					else {
						lines[l].setAttribute(TYPE_ATTRIBUTE, lineType);
						lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
					}
				}
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				p-=2;
			
			//	cancel from current dialog on
			else {
				cutoff = p;
				p = pages.length;
			}
		}
		
		//	restore layout paragraphs in pages
		for (int p = 0; p < cutoff; p++) {
			Annotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
			
			//	delete OCR noise
			for (int l = 0; l < lines.length; l++) {
				if (OCR_ARTIFACT_TYPE.equals(lines[l].getAttribute(TYPE_ATTRIBUTE)))
					pages[p].removeTokens(lines[l]);
				else lines[l].lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			}
			lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
			if (lines.length == 0)
				continue;
			
			//	remove old paragraphs
			AnnotationFilter.removeAnnotations(pages[p], MutableAnnotation.PARAGRAPH_TYPE);
			
			//	restore layout paragraphs
			int pStart = 0;
			String pType = ((String) lines[0].getAttribute(TYPE_ATTRIBUTE));
			for (int l = 1; l < lines.length; l++) {
				if (lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE)) {
					Annotation paragraph = pages[p].addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, pStart, (lines[l].getStartIndex() - pStart));
					paragraph.setAttribute(TYPE_ATTRIBUTE, pType);
					pStart = lines[l].getStartIndex();
					pType = ((String) lines[l].getAttribute(TYPE_ATTRIBUTE, pType));
				}
			}
			Annotation paragraph = pages[p].addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, pStart, (pages[p].size() - pStart));
			paragraph.setAttribute(TYPE_ATTRIBUTE, pType);
			
			//	delete page titles
			MutableAnnotation[] paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int g = 0; g < paragraphs.length; g++) {
				if (PAGE_TITLE_TYPE.equals(paragraphs[g].getAttribute(TYPE_ATTRIBUTE)))
					pages[p].removeTokens(paragraphs[g]);
			}
			paragraphs = pages[p].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			
			//	set page ID, page number and bounding box attributes
			String pageId = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
			String pageNumber = ((String) pages[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			for (int g = 0; g < paragraphs.length; g++) {
				if (pageNumber != null)
					paragraphs[g].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
				if (pageId != null)
					paragraphs[g].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
				
				lines = paragraphs[g].getAnnotations(LINE_ANNOTATION_TYPE);
				BoundingBox[] lineBoxes = new BoundingBox[lines.length];
				for (int l = 0; l < lines.length; l++)
					lineBoxes[l] = BoundingBox.getBoundingBox(lines[l]);
				BoundingBox pBox = BoundingBox.aggregate(lineBoxes);
				if (pBox != null)
					paragraphs[g].setAttribute(BOUNDING_BOX_ATTRIBUTE, pBox.toString());
			}
			
			//	remove lines
			AnnotationFilter.removeAnnotations(pages[p], LINE_ANNOTATION_TYPE);
		}
		
		//	dissolve pages (restore interrupted main text paragraphs)
		this.dissolvePages(data);
		AnnotationFilter.removeAnnotations(data, PAGE_TYPE);
		
		//	restore interrupted footnotes (move 'restart footnote' paragraphs after end of preceeding footnote and merge paragraphs)
		this.restoreFootnotes(data);
		
		//	 normalize paragraphs
		Gamta.normalizeParagraphStructure(data);
		
		//	add special annotations (caption, footnote, citation, etc)
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			String type = ((String) paragraphs[p].getAttribute(TYPE_ATTRIBUTE));
			if (CAPTION_TYPE.equals(type) || FOOTNOTE_TYPE.equals(type) || CITATION_TYPE.equals(type))
				data.addAnnotation(type, paragraphs[p].getStartIndex(), paragraphs[p].size()).copyAttributes(paragraphs[p]);
		}
	}
	
	
	private static class PageNumber {
		final int pageId;
		final int value;
		final int fuzzyness;
		final int ambiguity;
		int score = 0;
		//	!!! this constructor is for set lookups only !!!
		PageNumber(int value) {
			this.value = value;
			this.pageId = -1;
			this.fuzzyness = 1;
			this.ambiguity = 1;
		}
		PageNumber(int pageId, int value, int fuzzyness, int ambiguity) {
			this.value = value;
			this.pageId = pageId;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
		}
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
		int getAdjustedScore() {
			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
		}
	}
	
	private void markPageNumbers(MutableAnnotation[] pages) {
		
		//	process lines in each page & collect page number data
		int[] pageIds = new int[pages.length];
		HashSet[] pageNumberSets = new HashSet[pages.length];
		for (int p = 0; p < pages.length; p++) {
			
			//	store page ID
			pageIds[p] = Integer.parseInt((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE, ("" + p)));
			
			//	get lines
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	collect possible page numbers
			ArrayList pageNumberList = new ArrayList();
			for (int l = 0; l < lines.length; l++) {
				
				//	ignore all but the three top- and bottom-most lines
				if ((l >= 3) && (l < (lines.length - 3)))
					continue;
				
				//	get all candidates
				Annotation[] pageNumberCandidates = Gamta.extractAllMatches(lines[l], this.pageNumberPattern, 3, true, false, true);
				for (int n = 0; n < pageNumberCandidates.length; n++) {
					if (DEBUG_PAGE_NUMBERS) System.out.println("Possible page number on page " + pageIds[p] + ": " + pageNumberCandidates[n].getValue());
					
					//	extract possible interpretations, computing fuzzyness and ambiguity along the way
					String pageNumberString = pageNumberCandidates[n].getValue().replaceAll("\\s++", "");
					int [][] pageNumberDigits = new int[pageNumberString.length()][];
					int fuzzyness = 0;
					int ambiguity = 1;
					for (int c = 0; c < pageNumberString.length(); c++) {
						String pnc = pageNumberString.substring(c, (c+1));
						if (Gamta.isNumber(pnc)) {
							pageNumberDigits[c] = new int[1];
							pageNumberDigits[c][0] = Integer.parseInt(pnc);
						}
						else {
							fuzzyness++;
							pageNumberDigits[c] = ((int[]) this.pageNumberCharacterTranslations.get(pnc));
							ambiguity *= pageNumberDigits[c].length;
						}
					}
					
					//	the first digit is never zero ...
					if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0)) {
						if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> ignoring zero start");
						continue;
					}
					
					//	page numbers with six or more digits are rather improbable ...
					if (pageNumberDigits.length >= 6) {
						if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> ignoring for over-length");
						continue;
					}
					
					//	set type and base attributes
					pageNumberCandidates[n].changeTypeTo(PAGE_NUMBER_CANDIDATE_TYPE);
					pageNumberCandidates[n].setAttribute("fuzzyness", ("" + fuzzyness));
					if (DEBUG_PAGE_NUMBERS) System.out.println(" - fuzzyness is " + fuzzyness);
					pageNumberCandidates[n].setAttribute("ambiguity", ("" + ambiguity));
					if (DEBUG_PAGE_NUMBERS) System.out.println(" - ambiguity is " + ambiguity);
					
					//	this one is clear, annotate it right away
					if (ambiguity == 1) {
						StringBuffer pageNumberValue = new StringBuffer();
						for (int d = 0; d < pageNumberDigits.length; d++)
							pageNumberValue.append("" + pageNumberDigits[d][0]);
						lines[l].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue.toString());
						pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
						if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> value is " + pageNumberValue.toString());
					}
					
					//	deal with ambiguity
					else {
						TreeSet values = new TreeSet();
						int[] digits = new int[pageNumberDigits.length];
						getAllPossibleValues(pageNumberDigits, digits, 0, values);
						for (Iterator vit = values.iterator(); vit.hasNext();) {
							String pageNumberValue = ((String) vit.next());
							lines[l].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue);
							pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
							if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> possible value is " + pageNumberValue);
						}
					}
				}
			}
			
			//	aggregate page numbers
			Collections.sort(pageNumberList, new Comparator() {
				public int compare(Object o1, Object o2) {
					PageNumber pn1 = ((PageNumber) o1);
					PageNumber pn2 = ((PageNumber) o2);
					return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
				}
			});
			pageNumberSets[p] = new HashSet();
			for (int n = 0; n < pageNumberList.size(); n++)
				pageNumberSets[p].add(pageNumberList.get(n));
		}
		
		//	score page numbers
		for (int p = 0; p < pages.length; p++) {
			for (Iterator pnit = pageNumberSets[p].iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (DEBUG_PAGE_NUMBERS) System.out.println("Scoring page number " + pn + " for page " + pn.pageId);
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pages.length; l++) {
					if (pageNumberSets[p+l].contains(new PageNumber(pn.value + (pageIds[p+l] - pageIds[p]))))
						pn.score += l;
					else {
						fMisses++;
						if (fMisses == 3)
							l = pages.length;
					}
				}
				
				//	look backward
				int lMisses = 0;
				for (int l = 1; l <= p; l++) {
					if (pageNumberSets[p-l].contains(new PageNumber(pn.value + (pageIds[p-l] - pageIds[p]))))
						pn.score += l;
					else {
						lMisses++;
						if (lMisses == 3)
							l = (p+1);
					}
				}
				
				if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> score is " + pn.score);
			}
		}
		
		//	select page number for each page
		PageNumber[] pageNumbers = new PageNumber[pages.length];
		for (int p = 0; p < pages.length; p++) {
			int bestPageNumberScore = 0;
			for (Iterator pnit = pageNumberSets[p].iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (pn.score > bestPageNumberScore) {
					pageNumbers[p] = pn;
					bestPageNumberScore = pn.score;
				}
			}
			if (DEBUG_PAGE_NUMBERS) {
				if (pageNumbers[p] == null)
					System.out.println("Could not determine page number of page " + pageIds[p] + ".");
				else System.out.println("Determined page number of page " + pageIds[p] + " as " + pageNumbers[p] + " (score " + pageNumbers[p].score + ")");
			}
		}
		
		//	fill in sequence gaps
		for (int p = 0; p < pages.length; p++) {
			if (pageNumbers[p] == null)
				pageNumbers[p] = new PageNumber(pageIds[p], -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		
		//	do sequence base correction
		for (int p = 1; p < (pages.length - 1); p++) {
			if (pageNumbers[p+1].isConsistentWith(pageNumbers[p-1]) && !pageNumbers[p+1].isConsistentWith(pageNumbers[p])) {
				int beforeScore = pageNumbers[p-1].score;
				int ownScore = pageNumbers[p].score;
				int afterScore = pageNumbers[p+1].score;
				if ((beforeScore + afterScore) > (ownScore * 3)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p-1].value + (pageNumbers[p].pageId - pageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = ((beforeScore + afterScore) / 3);
					if (DEBUG_PAGE_NUMBERS) System.out.println("Corrected page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + ((beforeScore + afterScore) / 2) + " over " + ownScore + ")");
				}
			}
		}
		
		//	do backward sequence extrapolation
		for (int p = (pages.length - 2); p >= 0; p--) {
			if (!pageNumbers[p+1].isConsistentWith(pageNumbers[p])) {
				int ownScore = pageNumbers[p].score;
				int afterScore = pageNumbers[p+1].score;
				if (afterScore > (ownScore * 2)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p+1].value - (pageNumbers[p+1].pageId - pageNumbers[p].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = (afterScore / 2);
					if (DEBUG_PAGE_NUMBERS) System.out.println("Extrapolated (backward) page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + afterScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	do forward sequence extrapolation
		for (int p = 1; p < pages.length; p++) {
			if (!pageNumbers[p].isConsistentWith(pageNumbers[p-1])) {
				int beforeScore = pageNumbers[p-1].score;
				int ownScore = pageNumbers[p].score;
				if (beforeScore > (ownScore * 2)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p-1].value + (pageNumbers[p].pageId - pageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = (beforeScore / 2);
					if (DEBUG_PAGE_NUMBERS) System.out.println("Extrapolated (forward) page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + beforeScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	disambiguate page numbers that occur on multiple pages (using fuzzyness and ambiguity)
		HashMap allPageNumbers = new HashMap();
		for (int p = 0; p < pages.length; p++) {
			if (allPageNumbers.containsKey(pageNumbers[p])) {
				PageNumber pn = ((PageNumber) allPageNumbers.get(pageNumbers[p]));
				if (pn.getAdjustedScore() < pageNumbers[p].getAdjustedScore()) {
					if (DEBUG_PAGE_NUMBERS) System.out.println("Ousted page number " + pn.value + " of page " + pn.pageId + " (score " + pn.getAdjustedScore() + " against " + pageNumbers[p].getAdjustedScore() + ")");
					allPageNumbers.put(pageNumbers[p], pageNumbers[p]);
					pn.score = 0;
					pageNumbers[pn.pageId] = new PageNumber(pn.pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
				else {
					if (DEBUG_PAGE_NUMBERS) System.out.println("Ousted page number " + pageNumbers[p].value + " of page " + pageNumbers[p].pageId + " (score " + pageNumbers[p].getAdjustedScore() + " against " + pn.getAdjustedScore() + ")");
					pageNumbers[p].score = 0;
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
			}
			else if (pageNumbers[p].value != -1) allPageNumbers.put(pageNumbers[p], pageNumbers[p]);
		}
		
		if (DEBUG_PAGE_NUMBERS) for (int p = 0; p < pages.length; p++)
			System.out.println("Page number of page " + pageNumbers[p].pageId + " is " + pageNumbers[p] + " (score " + pageNumbers[p].score + ")");
		
		//	annotate page numbers and set attributes
		for (int p = 0; p < pages.length; p++) {
			String pageNumberValue = ("" + pageNumbers[p].value);
			
			//	set page attribute
			pages[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberValue);
			
			//	deal with page number candidates
			Annotation[] pageNumberCandidates = pages[p].getAnnotations(PAGE_NUMBER_CANDIDATE_TYPE);
			
			//	if value is -1, we only need to clean up
			if (pageNumbers[p].value == -1) {
				for (int n = 0; n < pageNumberCandidates.length; n++)
					pages[p].removeAnnotation(pageNumberCandidates[n]);
				continue;
			}
			
			//	find (most probable) original annotation otherwise
			int minFuzzyness = (pageNumberValue.length() + 1);
			Annotation leastFuzzyPageNumber = null;
			for (int n = 0; n < pageNumberCandidates.length; n++) {
				
				//	value matches page number value
				if (pageNumberValue.equals(pageNumberCandidates[n].getAttribute("value"))) {
					int fuzzyness = Integer.parseInt((String) pageNumberCandidates[n].getAttribute("fuzzyness"));
					
					//	too risky, could be some single letter in the middle of a line of text
					if ((fuzzyness == pageNumberValue.length()) && (pageNumberValue.length() == 1))
						pages[p].removeAnnotation(pageNumberCandidates[n]);
					
					//	we have a new best match
					else if (fuzzyness < minFuzzyness) {
						if (leastFuzzyPageNumber != null)
							pages[p].removeAnnotation(leastFuzzyPageNumber);
						leastFuzzyPageNumber = pageNumberCandidates[n];
						minFuzzyness = fuzzyness;
					}
					
					//	clean up
					else pages[p].removeAnnotation(pageNumberCandidates[n]);
				}
				
				//	no match, clean up
				else pages[p].removeAnnotation(pageNumberCandidates[n]);
			}
			
			//	we have found a page number, promote annotation
			if (leastFuzzyPageNumber != null) {
				leastFuzzyPageNumber.changeTypeTo(PAGE_NUMBER_TYPE);
				leastFuzzyPageNumber.setAttribute("score", ("" + pageNumbers[p].score));
				if (DEBUG_PAGE_NUMBERS) System.out.println("Annotated page number of page " + pageNumbers[p].pageId + " as " + leastFuzzyPageNumber.toXML() + " at " + leastFuzzyPageNumber.getStartIndex());
			}
		}
	}
	
	private static final String CAPTION_START_STRING = "figure;figures;table;tables;diagram;diagrams";
	private static final String CAPTION_START_ABBREVIATIONS_STRING = "fig;figs;tab;tabs;diag;diags";
	
	private static final int MAX_PAGE_TITLE_TOKENS = 25;
	
	private static final StringVector noise = StringUtils.getNoiseWords();
	
	private int maxTopTitleLines = 3;
	private static final String maxTopTitleParagraphsSetting = "maxTopTitleParagraphs";
	
	private int maxBottomTitleLines = 3;
	private static final String maxBottomTitleParagraphsSetting = "maxTopTitleParagraphs";
	
	private String[] pageTitlePatterns = new String[0];
	
	private StringVector captionStarts;
	private StringVector captionStartAbbreviations;
	
	private static final boolean DEBUG_PAGE_TITLES = false;
	
	private void markPageTitleLines(MutableAnnotation[] pages) {
		
		//	gather words from paragraphs that might be page titles (distinguish even and odd pages)
		StringIndex topWordStatisticsEven = new StringIndex(false);
		StringIndex bottomWordStatisticsEven = new StringIndex(false);
		StringIndex topWordStatisticsOdd = new StringIndex(false);
		StringIndex bottomWordStatisticsOdd = new StringIndex(false);
		StringVector wordCollector = new StringVector(false);
		
		//	collect words from even pages
		for (int p = 0; p < pages.length; p+= 2) {
			
			//	get lines possibly containing page titles
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	check individual lines from top
			int lIndex = 0;
			while (((lIndex < this.maxTopTitleLines) || (this.maxTopTitleLines < 0)) && (lIndex < lines.length)) {
				if (this.extractWords(lines[lIndex], wordCollector)) lIndex++;
				else lIndex = lines.length;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				topWordStatisticsEven.add(wordCollector.get(c));
			wordCollector.clear();
			
			//	check individual lines from bottom
			lIndex = lines.length - 1;
			while (((pages.length <= (lIndex + this.maxBottomTitleLines)) || (this.maxBottomTitleLines < 0)) && (lIndex > -1)) {
				if (this.extractWords(lines[lIndex], wordCollector)) lIndex--;
				else lIndex = -1;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				bottomWordStatisticsEven.add(wordCollector.get(c));
			wordCollector.clear();
		}
		
		//	collect words from odd pages
		for (int p = 1; p < pages.length; p+= 2) {
			
			//	get lines possibly containing page titles
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	check individual lines from top
			int lIndex = 0;
			while (((lIndex < this.maxTopTitleLines) || (this.maxTopTitleLines < 0)) && (lIndex < lines.length)) {
				if (this.extractWords(lines[lIndex], wordCollector)) lIndex++;
				else lIndex = lines.length;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				topWordStatisticsOdd.add(wordCollector.get(c));
			wordCollector.clear();
			
			//	check individual lines from bottom
			lIndex = lines.length - 1;
			while (((pages.length <= (lIndex + this.maxBottomTitleLines)) || (this.maxBottomTitleLines < 0)) && (lIndex > -1)) {
				if (this.extractWords(lines[lIndex], wordCollector)) lIndex--;
				else lIndex = -1;
			}
			wordCollector.removeDuplicateElements(false);
			for (int c = 0; c < wordCollector.size(); c++)
				bottomWordStatisticsOdd.add(wordCollector.get(c));
			wordCollector.clear();
		}

		
		//	find page title lines
		for (int p = 0; p < pages.length; p++) {
			
			//	get lines possibly containing page titles
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	check individual paragraphs from top
			int lIndex = 0;
			while (((lIndex < this.maxTopTitleLines) || (this.maxTopTitleLines < 0)) && (lIndex < lines.length)) {
				if (this.isPageTitle(lines[lIndex], (((p % 2) == 0) ? topWordStatisticsEven : topWordStatisticsOdd), pages.length)) {
					lines[lIndex].setAttribute(TYPE_ATTRIBUTE, PAGE_TITLE_TYPE);
					lIndex++;
				}
				else lIndex = lines.length;
			}
			
			//	check individual paragraphs from bottom
			lIndex = lines.length - 1;
			while (((lines.length <= (lIndex + this.maxBottomTitleLines)) || (this.maxBottomTitleLines < 0)) && (lIndex > -1)) {
				if (this.isPageTitle(lines[lIndex], (((p % 2) == 0) ? bottomWordStatisticsEven : bottomWordStatisticsOdd), pages.length)) {
					lines[lIndex].setAttribute(TYPE_ATTRIBUTE, PAGE_TITLE_TYPE);
					lIndex--;
				}
				else lIndex = -1;
			}
		}
	}
	
	private boolean extractWords(MutableAnnotation line, StringVector wordCollector) {
		
		//	check line length
		if (line.size() > MAX_PAGE_TITLE_TOKENS) return false;
		
		//	count words
		for (int v = 0; v < line.size(); v++) {
			String value = line.valueAt(v);
			if (!noise.containsIgnoreCase(value) && StringUtils.isWord(value))
				wordCollector.addElementIgnoreDuplicates(value);
		}
		
		return true;
	}
	
	private boolean isPageTitle(MutableAnnotation line, StringIndex wordStatistics, int totalPageCount) {
		if (DEBUG_PAGE_TITLES) System.out.println("\n - checking for page title (page number " + line.getAttribute(PAGE_NUMBER_ATTRIBUTE) + "): " + line.getValue());
		
		//	caption indicator word followed by reference number or letter
		if (CAPTION_TYPE.equals(line.getAttribute(TYPE_ATTRIBUTE))) {
			if (DEBUG_PAGE_TITLES) System.out.println(" - NO, it's a caption");
			return false;
		}
		
		//	check for page numbers
		Annotation[] pageNumbers = line.getAnnotations(PAGE_NUMBER_TYPE);
		if (pageNumbers.length != 0) {
			if (DEBUG_PAGE_TITLES) System.out.println(" - YES, there are page numbers");
			return true;
		}
		
		//	check regular expressions
		String paragraphString = line.getValue();
		for (int p = 0; p < this.pageTitlePatterns.length; p++) {
			if (paragraphString.matches(this.pageTitlePatterns[p])) {
				if (DEBUG_PAGE_TITLES) System.out.println(" - YES, matched '" + this.pageTitlePatterns[p] + "'");
				return true;
			}
		}
		
		//	check number of words frequent near page borders (only for more than five pages, to uncertain for less)
		int minPageFraction = ((totalPageCount > 5) ? ((totalPageCount + 1) / 3) : totalPageCount);
		int lineWordCount = 0;
		int lineKnownWordCount = 0;
		for (int v = 0; v < line.size(); v++) {
			String value = line.valueAt(v);
			if (!noise.containsIgnoreCase(value) && StringUtils.isWord(value)) {
				lineWordCount++;
				if (wordStatistics.getCount(value) >= minPageFraction)
					lineKnownWordCount++;
			}
		}
		if ((lineKnownWordCount * 2) >= lineWordCount) {
			if (DEBUG_PAGE_TITLES) System.out.println(" - YES, there are many words repeating at other page edges");
			return true;
		}
		
		//	vote other evidence
		int vote = 0;
		int upperCaseCount = 0;
		int charCount = 0;
		boolean digitsOnly = true;
		int longestWhitespace = 0;
		int shortestWhitespace = line.length();
		
		for (int t = 0; t < line.size(); t++) {
			Token token = line.tokenAt(t);
			String value = token.getValue();
			charCount += value.length();
			
			for (int c = 0; c < value.length(); c++) {
				//	check for upper case
				if (Gamta.UPPER_CASE_LETTERS.indexOf(value.charAt(c)) != -1) upperCaseCount++;
				
				//	check if digits only (maybe page number)
				if (Gamta.DIGITS.indexOf(value.charAt(c)) == -1) digitsOnly = false;
			}
			
			//	check whitespace blocks (if lenghts too different, may be centered title)
			int whitespaceLength = line.getWhitespaceAfter(t).length();
			if (whitespaceLength > longestWhitespace) longestWhitespace = whitespaceLength;
			if (whitespaceLength < shortestWhitespace) shortestWhitespace = whitespaceLength;
		}
		
		//	check for upper case
		if ((upperCaseCount * 2) > charCount)
			vote += (((upperCaseCount * 5) > (charCount * 2)) ? 2 : 1);
		
		//	check if digits only (maybe page number)
		if (digitsOnly && (line.length() < 5))
			vote += 2; // page numbers with 5 or more digits are very unlikely
		
		//	check whitespace blocks (if lenghts too different, may be centered title)
		if (((shortestWhitespace * 3) < longestWhitespace) && (longestWhitespace > 4))
			vote += (((shortestWhitespace * 5) < longestWhitespace) ? 2 : 1);
		
//		System.out.println(" - char counts are as follows:");
//		System.out.println("   - char count: " + charCount);
//		System.out.println("   - upper case count: " + upperCaseCount);
//		System.out.println("   - digits only: " + digitsOnly);
//		System.out.println("   - shortest whitespace: " + shortestWhitespace);
//		System.out.println("   - longest whitespace: " + longestWhitespace);
//		System.out.println("  ==> vote = " + vote);
		
		//	paragraph voted as page title
		return (vote >= 2);
	}
	
	
	private static final boolean DEBUG_CAPTIONS = false;
	private void markCaptionStartLines(MutableAnnotation[] pages) {
		
		//	check individual pages
		for (int p = 0; p < pages.length; p++) {
			
			//	check individual lines
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			for (int l = 0; l < lines.length; l++) {
				
				//	check for page numbers
				if (lines[l].getAnnotations(PAGE_NUMBER_TYPE).length != 0)
					continue;
				
				//	we only check paragraph starts
				if (lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE) && !lines[l].hasAttribute(TYPE_ATTRIBUTE) && this.isCaptionStart(lines[l])) {
					lines[l].setAttribute(TYPE_ATTRIBUTE, CAPTION_TYPE);
					if (DEBUG_CAPTIONS)
						System.out.println("Got caption start line: " + lines[l].getValue());
				}
			}
		}
	}
	
	private boolean isCaptionStart(TokenSequence line) {
		
		//	catch empty paragraphs
		if (line.size() < 2) return false;
		
		//	indicator word followed by reference number or letter
		if (this.fuzzyContains(this.captionStarts, line.valueAt(0), 1) &&
				(
					Gamta.isNumber(line.tokenAt(1)) ||
					((line.tokenAt(1).length() == 1) && (Gamta.LETTERS.indexOf(line.valueAt(1)) != -1)) ||
					!Gamta.isWord(line.tokenAt(1))
				)
			) return true;
		
		//	catch empty paragraphs
		if (line.size() < 3) return false;
		
		//	indicator word abbreviation followed by reference number or letter
		if (this.fuzzyContains(this.captionStartAbbreviations, line.valueAt(0), 1) &&
				line.valueAt(1).equals(".") &&
					(
						Gamta.isNumber(line.tokenAt(2)) ||
						((line.tokenAt(2).length() == 1) && (Gamta.LETTERS.indexOf(line.valueAt(2)) != -1)) ||
						!Gamta.isWord(line.tokenAt(2))
					)
				) return true;
		
		//	check indexing letters
		StringVector indexLetters = new StringVector();
		for (int t = 0; t < line.size(); t++) {
			Token token = line.tokenAt(t);
			if (Gamta.isWord(token) && (token.length() == 1) && Gamta.isLowerCaseWord(token))
				indexLetters.addElementIgnoreDuplicates(token.getValue());
		}
		
		//	search sequences in index letters
		int indexingLetters = 0;
		for (int s = 0; s < indexLetters.size(); s++) {
			int score = 0;
			for (int o = s; o < indexLetters.size(); o++) {
				if (indexLetters.get(s).charAt(0) == (indexLetters.get(o).charAt(0) + (o - s))) score ++;
				else score--;
			}
			if (score > indexingLetters) indexingLetters = score;
		}
		
		//	caption if more than one indexing letter
		return (indexingLetters > 1);
	}
	
	private boolean fuzzyContains(StringVector sv, String str, int th) {
		for (int s = 0; s < sv.size(); s++) {
			if (StringUtils.getLevenshteinDistance(sv.get(s), str, th, false) <= th)
				return true;
		}
		return false;
	}
	
	
	private static final String CITATION_PART_STRING = "p;pp;fig;figs";
	
	private StringVector noiseWords;
	private StringVector citationParts = new StringVector(false);
	
	private static final boolean DEBUG_FOOTNOTES = false;
	private void markFootnoteStartLines(MutableAnnotation[] pages) {
		
		//	compute average line length throughout document
		int documentLength = 0;
		int documentLineCount = 0;
		for (int p = 0; p < pages.length; p++) {
			documentLength += pages[p].length();
			documentLineCount += pages[p].getAnnotations(LINE_ANNOTATION_TYPE).length;
		}
		int averageLineLength = (documentLength / documentLineCount);
		
		//	check individual pages
		for (int p = 0; p < pages.length; p++) {
			
			//	check individual lines
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	identify footnote starts, checking backward from bottom of page
			for (int l = (lines.length - 1); l > -1; l--) {
				
				//	we are checking starts only
				if (!lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE)) {
					if (DEBUG_FOOTNOTES)
						System.out.println("Skipping over in-paragraph line: " + lines[l].getValue());
					continue;
				}
				
				//	skip over page titles
				if (PAGE_TITLE_TYPE.equals(lines[l].getAttribute(TYPE_ATTRIBUTE))) {
					if (DEBUG_FOOTNOTES)
						System.out.println("Skipping over page title line: " + lines[l].getValue());
					continue;
				}
				
				//	this one's already typed
				if (lines[l].hasAttribute(TYPE_ATTRIBUTE)) {
					if (DEBUG_FOOTNOTES)
						System.out.println("Stopping at pre-typed line: " + lines[l].getValue());
					break;
				}
				
				
				boolean isFootnote = false;
				
				//	compare line length (true if at least 15% longer lines than average)
				if ((lines[l].length() * 17) > (averageLineLength * 20)) {
					isFootnote = true;
					if (DEBUG_FOOTNOTES)
						System.out.println("Got footnote start for length (" + lines[l].length() + " over " + averageLineLength + ", page " + pages[p].getAttribute(PAGE_ID_ATTRIBUTE) + "): " + lines[l].getValue());
				}
				
				//	check textual evidence
				else if (this.isFootnoteStart(lines[l])) {
					isFootnote = true;
					if (DEBUG_FOOTNOTES)
						System.out.println("Got footnote start for vote (page " + pages[p].getAttribute(PAGE_ID_ATTRIBUTE) + "): " + lines[l].getValue());
				}
				
				//	line voted as footnote start
				if (isFootnote)
					lines[l].setAttribute(TYPE_ATTRIBUTE, FOOTNOTE_TYPE);
				else {
					if (DEBUG_FOOTNOTES)
						System.out.println("Stopping at non-footnote line: " + lines[l].getValue());
					break;
				}
			}
		}
	}
	
	private boolean isFootnoteStart(TokenSequence line) {
		
		//	index letter glued to first word
		if (line.valueAt(0).matches("([a-z][A-Z][A-Za-z0-9\\-']++)")) return true;
		
		//	check for copyright notice
		for (int t = 0; t < line.size(); t++) 
			if (line.tokenAt(t).equals("©")) return true;

		//	catch single-tokened paragraphs
		if (line.size() == 1) return this.isCitationFootnoteStart(line);
		
		//	index char (not a capital letter, not an opening bracket) followed by capitalized word
		if ((line.tokenAt(0).length() == 1) && 
				Gamta.isFirstLetterUpWord(line.tokenAt(1)) && 
				!Gamta.isOpeningBracket(line.tokenAt(0)) && 
				(Gamta.UPPER_CASE_LETTERS.indexOf(line.valueAt(0)) == -1)
			) return true;
		
		//	catch two token paragraphs
		if (line.size() == 2) return this.isCitationFootnoteStart(line);
		
		//	index char (not a capital letter, not an opening bracket) followed by start of quotas and capitalized word 
		if (((line.tokenAt(0).length() == 1) && !Gamta.isOpeningBracket(line.tokenAt(0))) && 
				(line.tokenAt(1).equals("'") || line.tokenAt(1).equals("\"")) && 
				Gamta.isFirstLetterUpWord(line.tokenAt(2))
			) return true;
		
		//	check if paragraph is a citation
		return this.isCitationFootnoteStart(line);
	}
	
	private boolean isCitationFootnoteStart(TokenSequence line) {
		
		//	check frequent parts of citations
		int vote = 0;
		for (int t = 1; t < line.size(); t++)
			if (line.tokenAt(t).equals("."))
				if (this.citationParts.containsIgnoreCase(line.valueAt(t))) vote++;
		if (vote > 1) return true;
		
		//	tokenize line without inner punctuation
		TokenSequence strictTokens = new PlainTokenSequence(line, Gamta.NO_INNER_PUNCTUATION_TOKENIZER);
		
		//	search quoted part in title case
		int start = 0;
		while ((start < strictTokens.size()) && !strictTokens.valueAt(start).equals("'") && !strictTokens.valueAt(start).equals("\"")) start++;
		if (start == strictTokens.size()) vote --;
		start++;
		while ((start < strictTokens.size()) && (!Gamta.isPunctuation(strictTokens.tokenAt(start)) || strictTokens.valueAt(start).equals(",") || Gamta.isBracket(strictTokens.tokenAt(start)))) {
			Token token = strictTokens.tokenAt(start);
			if (Gamta.isWord(token) && !Gamta.isCapitalizedWord(token) && !this.noiseWords.contains(token.getValue())) return false;
			start++;
		}
		if (start == strictTokens.size()) vote ++;
		
		//	check character/token mix
		int wordCount = 0;
		int punctuationCount = 0;
		int abbreviationCount = 0;
		int upperCaseCount = 0;
		for (int t = 0; t < strictTokens.size(); t++) {
			Token token = strictTokens.tokenAt(t);
			
			//	abbreviation (word followed by period)
			if (((t + 1) < strictTokens.size()) && Gamta.isFirstLetterUpWord(token) && strictTokens.tokenAt(t + 1).equals(".")) abbreviationCount++;
			
			//	capitalized word in middle of sentence
			if ((t != 0) && Gamta.isFirstLetterUpWord(token) && !Gamta.isSentenceEnd(strictTokens.tokenAt(t - 1))) upperCaseCount++;
			
			//	non-bracket punctuation mark
			if (Gamta.isPunctuation(token))
				if (!Gamta.isBracket(token) && ((t == 0) || !token.equals(strictTokens.tokenAt(t - 1)))) punctuationCount++;
				else if (Gamta.isBracket(token)) punctuationCount--;
			
			//	word, or indexing letter
			if (Gamta.isWord(token))
				if ((token.length() == 1) && Gamta.isLowerCaseWord(token) && !token.equals("a")) punctuationCount += 2;
				else wordCount++;
			
			//	number
			if (Gamta.isNumber(token)) wordCount++;
		}
		
		//	check if high quantum of punctuation and abbreviations in line
		return (((1 + vote) * (punctuationCount + abbreviationCount + upperCaseCount)) > (1 * wordCount));
	}
	
	
	private static final String REGEX_NAME_EXTENSION = ".regEx.txt"; 
	
	private static final String MAX_CITATION_LINES_SETTING = "MAX_CITATION_LINES";
	private static final int DEFAULT_MAX_CITATION_LINES = 4;
	private int maxCitationLines = DEFAULT_MAX_CITATION_LINES;
	
	private static final String MAX_CITATION_WORDS_SETTING = "MAX_CITATION_WORDS";
	private static final int DEFAULT_MAX_CITATION_WORDS = 100;
	private int maxCitationWords = DEFAULT_MAX_CITATION_WORDS;
	
	private static final String CITATION_PATTERN_NAME_FILE = "useCitationPatterns.cnfg";
	private StringVector activeCitationPatternNames = new StringVector();
	
	private String[] citationPatterns;
	private String[] citationPatternNames;
	
	//	regular expression for finding years (might be publication years)
	private static final String yearRegEx = "([12][0-9]{3})";
	private static final boolean DEBUG_CITATIONS = false;
	private void markCitationStartLines(MutableAnnotation[] pages) {
		
		//	collect document words
		StringVector docTextTokens = new StringVector();
		for (int p = 0; p < pages.length; p++)
			docTextTokens.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(pages[p]), false);
		
		//	check individual pages
		for (int p = 0; p < pages.length; p++) {
			
			//	check individual lines
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			
			//	identify and count citation starts
			int citationStartCount = 0;
			for (int l = 0; l < lines.length; l++) {
				
				//	check evidence
				String evidence = this.isCitationStart(lines[l], docTextTokens);
				if (evidence != null) {
					lines[l].setAttribute(TYPE_ATTRIBUTE, CITATION_TYPE);
					lines[l].setAttribute("_evidence", evidence);
					lines[l].setAttribute(PARAGRAPH_START_ATTRIBUTE, PARAGRAPH_START_ATTRIBUTE);
					citationStartCount++;
					if (DEBUG_CITATIONS)
						System.out.println("Got citation start (for " + evidence + "): " + lines[l].getValue());
				}
			}
			
			//	small number of citations (might be misrecognitions) ==> too flimsy for normalization
			if (citationStartCount < 3)
				continue;
			
			//	find first and last citation start line
			int firstCitationStart = lines.length;
			int lastCitationStart = 0;
			for (int l = 0; l < lines.length; l++) {
				
				//	remember citations
				if (CITATION_TYPE.equals(lines[l].getAttribute(TYPE_ATTRIBUTE))) {
					if (firstCitationStart == lines.length)
						firstCitationStart = l;
					lastCitationStart = l;
				}
			}
			
			//	remove paragraph start marker from lines in the midst of citations
			for (int l = 0; l < lines.length; l++) {
				
				//	we are checking starts only
				if (!lines[l].hasAttribute(PARAGRAPH_START_ATTRIBUTE))
					continue;
				
				if ((l > firstCitationStart) && (l < lastCitationStart) && (!lines[l].hasAttribute(TYPE_ATTRIBUTE) || FOOTNOTE_TYPE.equals(lines[l].getAttribute(TYPE_ATTRIBUTE)))) {
					lines[l].removeAttribute(PARAGRAPH_START_ATTRIBUTE);
					lines[l].removeAttribute(TYPE_ATTRIBUTE);
					if (DEBUG_CITATIONS)
						System.out.println("Inlined line between citations: " + lines[l].getValue());
				}
			}
		}
	}
	
	private String isCitationStart(MutableAnnotation paragraph, StringVector docTextTokens) {
		
		//	use patterns
		for (int p = 0; p < this.citationPatterns.length; p++) {
			
			//	get matches
			Annotation[] matches = Gamta.extractAllMatches(paragraph, this.citationPatterns[p], 25, false);
			
			if (DEBUG_CITATIONS) {
				for (int m = 0; m < matches.length; m++)
					System.out.println("Pattern match (" + this.citationPatternNames[p] + "): " + matches[m].getValue());
			}
			
			//	check matches
			if (matches.length != 0) {
				int matchStart = matches[0].getStartIndex();
				boolean wordBefore = false;
				for (int b = 0; b < matchStart; b++) {
					
					//	lower case word before match
					if (Gamta.isLowerCaseWord(paragraph.valueAt(b))) {
						
						//	lower case word is not letter-coded numbering
						if (((b+1) == matchStart) || (")].".indexOf(paragraph.valueAt(b+1)) == -1))
							wordBefore = true;
					}
				}
				
				if (wordBefore) {
					if (DEBUG_CITATIONS)
						System.out.println("  ==> too late in line");
				}
				else {
					if (DEBUG_CITATIONS)
						System.out.println("  ==> match");
					return this.citationPatternNames[p];
				}
			}
		}
		
		//	check title case words
		int wordCount = 0;
		int capWordCount = 0;
		for (int t = 0; t < paragraph.size(); t++) {
			Token token = paragraph.tokenAt(t);
			if (Gamta.isWord(token)) {
				wordCount ++;
				if (Gamta.isFirstLetterUpWord(token) && docTextTokens.contains(token.getValue().toLowerCase()))
					capWordCount ++;
			}
		}
		
		//	check for year
		Annotation[] years = Gamta.extractAllMatches(paragraph, yearRegEx, 1);
		
		//	compute result
		if ((years.length != 0) && ((capWordCount * 3) > wordCount))
			return "TitleCase";
		else return null;
	}
	
	
	private class ParagraphMerger {
		int firstParagraphIndex;
		int secondParagraphIndex;
		ParagraphMerger(int firstParagraphIndex, int secondParagraphIndex) {
			this.firstParagraphIndex = firstParagraphIndex;
			this.secondParagraphIndex = secondParagraphIndex;
		}
	}
	
//	private static final String PAGE_START_TOKEN_TYPE = "pageStartToken";
//	
	private StringVector toMove = new StringVector();
	
	private void dissolvePages(MutableAnnotation data) {
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	collect tokens in order to assess which joins make sense
		StringVector tokens = new StringVector();
		boolean lastWasBreak = true;
		for (int t = 0; t < data.size(); t++) {
			Token token = data.tokenAt(t);
			if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
				lastWasBreak = true;
			
			else {
				if (!lastWasBreak)
					tokens.addElementIgnoreDuplicates(token.getValue());
				lastWasBreak = false;
			}
		}
		
		//	determine for each paragraph whether or not it might have to be moved
		boolean[] isParagraphToBridge = new boolean[paragraphs.length];
		for (int p = 0; p < paragraphs.length; p++)
			isParagraphToBridge[p] = (this.toMove.contains((String) paragraphs[p].getAttribute(TYPE_ATTRIBUTE)));
		
		
		//	attribute tokens with page IDs and numbers
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageId = paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE);
			for (int t = 0; t < paragraphs[p].size(); t++)
				paragraphs[p].tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			for (int t = 0; t < paragraphs[p].size(); t++)
				paragraphs[p].tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
		}
		
		
		//	determine for each paragraph whether or not it has to be moved (interrupts another paragraph)
		ArrayList mergeList = new ArrayList();
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	this one might have to be moved
			if (isParagraphToBridge[p]) {
				
				//	remember start of block
				int mbs = p;
				
				//	find end of block
				int mbe = mbs;
				while ((mbe < paragraphs.length) && isParagraphToBridge[mbe])
					mbe++;
				
				//	moving only sensible if block starts after document start and ends before end of document
				if ((mbs == 0) || (mbe == paragraphs.length)) {
					
					//	remember not to move block
					while (mbs < mbe)
						isParagraphToBridge[mbs++] = false;
				}
				
				//	prepare getting feedback for surrounding paragraphs if block does not touch document boundary
				else {
					ParagraphMerger merger = new ParagraphMerger((mbs-1), mbe);
					
					//	no evidence for a sentence end or start, probably need to be merged
					if (RESTART_MAIN_TEXT_TYPE.equals(paragraphs[mbe].getAttribute(TYPE_ATTRIBUTE)))
						mergeList.add(merger);
				}
				
				//	jump to end of block (counter loop increment, though)
				p = mbe-1;
			}
			
			//	check for consecutive main text paragraphs with different page numbers (might be separated due to page break)
			else if (((p+1) < paragraphs.length) && !isParagraphToBridge[p+1]) {
				MutableAnnotation firstParagraph = paragraphs[p];
				MutableAnnotation secondParagraph = paragraphs[p+1];
				
				try {
					int firstPageNumber = Integer.parseInt((String) firstParagraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + paragraphs.length)));
					int secondPageNumber = Integer.parseInt((String) secondParagraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "-1"));
					
					//	paragraph across page border, enqueue feedback
					if (firstPageNumber < secondPageNumber) {
						ParagraphMerger merger = new ParagraphMerger(p, (p+1));
						if (RESTART_MAIN_TEXT_TYPE.equals(paragraphs[p+1].getAttribute(TYPE_ATTRIBUTE)))
							mergeList.add(merger);
					}
				}
				catch (NumberFormatException nfe) {}
			}
		}
		
		//	put mergers in array for easier processing
		if (mergeList.isEmpty())
			return;
		ParagraphMerger[] merges = ((ParagraphMerger[]) mergeList.toArray(new ParagraphMerger[mergeList.size()]));
		
		//	do mergers (back to front, in order not to interfer with indices)
		for (int m = (merges.length-1); m >= 0; m--) {
			
			//	get paragraphs to merge
			MutableAnnotation firstParagraph = paragraphs[merges[m].firstParagraphIndex];
			MutableAnnotation secondParagraph = paragraphs[merges[m].secondParagraphIndex];
			
			//	insert enclosed parts after second paragraph (if any)
			int movedParagraphStart = secondParagraph.getEndIndex();
			
			for (int p = (merges[m].firstParagraphIndex+1); p < merges[m].secondParagraphIndex; p++) {
				
				//	insert tokens
				data.insertTokensAt(data.subSequence(paragraphs[p].getStartOffset(), paragraphs[p+1].getStartOffset()), movedParagraphStart);
				
				//	annotate paragraph
				MutableAnnotation movedParagraph = data.addAnnotation(PARAGRAPH_TYPE, movedParagraphStart, paragraphs[p].size());
				
				//	transfer token attributes (page numbers!)
				for (int t = 0; (t < paragraphs[p].size()) && (t < movedParagraph.size()); t++)
					movedParagraph.tokenAt(t).copyAttributes(paragraphs[p].tokenAt(t));
				
				//	transfer attributes (including annotation ID)
				movedParagraph.copyAttributes(paragraphs[p]);
				movedParagraph.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, paragraphs[p].getAnnotationID());
				
				//	transfer subordinate annotations
				Annotation[] paragraphAnnotations = paragraphs[p].getAnnotations();
				for (int a = 0; a < paragraphAnnotations.length; a++)
					if (!PARAGRAPH_TYPE.equals(paragraphAnnotations[a].getType())) {
						Annotation movedParagraphAnnotation = movedParagraph.addAnnotation(paragraphAnnotations[a].getType(), paragraphAnnotations[a].getStartIndex(), paragraphAnnotations[a].size());
						movedParagraphAnnotation.copyAttributes(paragraphAnnotations[a]);
						movedParagraphAnnotation.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, paragraphAnnotations[a].getAnnotationID());
					}
				
				//	remember to move next paragraph after the one just moved
				movedParagraphStart += paragraphs[p].size();
			}
			
			//	create new paragraph
			MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, firstParagraph.getStartIndex(), (secondParagraph.getEndIndex() - firstParagraph.getStartIndex()));
			newParagraph.copyAttributes(secondParagraph);
			newParagraph.copyAttributes(firstParagraph);
			
			//	aggregate bounding boxes
			String npBoxesString = this.getAggregateBoundingBoxesString(firstParagraph, secondParagraph);
			if (npBoxesString != null)
				newParagraph.setAttribute(BOUNDING_BOX_ATTRIBUTE, npBoxesString);
			
			//	normalize paragraph seam
			Token firstParagraphEndToken = firstParagraph.lastToken();
			Token secondParagraphsStartToken = secondParagraph.firstToken();
			
			//	check for word division
			String jointForm = this.getJointForm(firstParagraphEndToken, secondParagraphsStartToken, tokens);
			
			//	normalize whitespace ...
			if (jointForm == null)
				data.setWhitespaceAfter((Gamta.insertSpace(firstParagraphEndToken, secondParagraphsStartToken) ? " " : ""), firstParagraph.getEndIndex()-1);
			
			//	... or concatenate devided word
			else {
				data.setValueAt(jointForm, (firstParagraph.getEndIndex() - 1));
				data.removeTokensAt(secondParagraph.getStartIndex(), 1);
			}
			
			//	remove old paragraphs
			firstParagraph.lastToken().removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
			data.removeAnnotation(firstParagraph);
			data.removeAnnotation(secondParagraph);
			
			//	store new paragraph for merges to come
			paragraphs[merges[m].firstParagraphIndex] = newParagraph;
			
			//	remove enclose parts (if any)
			for (int p = (merges[m].secondParagraphIndex-1); p > merges[m].firstParagraphIndex; p--)
				data.removeTokens(paragraphs[p]);
			//	mark merge as done (even if feedback decided against it, for checking possiblility of merges further up the document)
			merges[m] = null;
		}
		
		//	set page IDs and numbers of paragraphs
		paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageId = paragraphs[p].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
			paragraphs[p].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			Object pageNumber = paragraphs[p].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			paragraphs[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			
			Object lastPageId = paragraphs[p].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if ((lastPageId != null) && !lastPageId.equals(pageId))
				paragraphs[p].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageId);
			Object lastPageNumber = paragraphs[p].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if ((lastPageNumber != null) && !lastPageNumber.equals(pageNumber))
				paragraphs[p].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumber);
		}
		
		/*
		 * annotate page breaks, i.e. tokens that have a page number different
		 * from than of the previous token, and remove page numbers from tokens
		 */
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
				pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, ("" + currentPageId));
			}
			
			//	clean up
			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
		
		//	remove duplicate page breaks and continue attribute
		AnnotationFilter.removeDuplicates(data, PAGE_BREAK_TOKEN_TYPE);
//		/*
//		 * annotate page starts, i.e. tokens that have a page number higher than
//		 * the previous token, and remove page numbers from tokens
//		 */
//		int currentPageNumber = 0;
//		for (int t = 0; t < data.size(); t++) {
//			
//			//	get page number
//			int tokenPageNumber = currentPageNumber;
//			try {
//				tokenPageNumber = Integer.parseInt((String) data.tokenAt(t).getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + tokenPageNumber)));
//			} catch (NumberFormatException nfe) {}
//			
//			//	we are on a new page, mark it
//			if (tokenPageNumber > currentPageNumber) {
//				currentPageNumber = tokenPageNumber;
//				Annotation pageStart = data.addAnnotation(PAGE_START_TOKEN_TYPE, t, 1);
//				pageStart.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + currentPageNumber));
//			}
//			
//			//	clean up
//			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
//		}
	}
	
	private String getJointForm(Token token1, Token token2, StringVector tokens) {
		if (token1 == null) return null;
		if (token1.length() < 2) return null;
		
		//	no hyphen at end of line
		String value1 = token1.getValue();
		if (!value1.endsWith("-")) return null;
		
		//	no word to continue with, or part of enumeration
		String value2 = token2.getValue();
		if (!Gamta.isWord(value2) || "and".equalsIgnoreCase(value2) || "or".equalsIgnoreCase(value2)) return null;
		
		//	prepare for lookup
		String nValue2 = value2.toLowerCase();
		if (nValue2.endsWith("'s"))
			nValue2 = nValue2.substring(0, (nValue2.length() - 2));
		else if (nValue2.endsWith("ies"))
			nValue2 = nValue2.substring(0, (nValue2.length() - 3)) + "y";
		else if (nValue2.endsWith("s"))
			nValue2 = nValue2.substring(0, (nValue2.length() - 1));
		
		//	joint value appears with hyphen elsewhere in text ==> keep hyphen
		if (tokens.containsIgnoreCase(value1 + value2)) return (value1 + value2);
		else if (tokens.containsIgnoreCase(value1 + nValue2)) return (value1 + value2);
		
		//	joint value appears elsewhere in text ==> join
		if (tokens.containsIgnoreCase(value1.substring(0, (value1.length() - 1)) + value2)) return (value1.substring(0, (value1.length() - 1)) + value2);
		else if (tokens.containsIgnoreCase(value1.substring(0, (value1.length() - 1)) + nValue2)) return (value1.substring(0, (value1.length() - 1)) + value2);
		
		//	if lower case letter before hyphen (probably nothing like 'B-carotin') ==> join
		if (Gamta.LOWER_CASE_LETTERS.indexOf(value1.charAt(value1.length() - 2)) != -1) return (value1.substring(0, (value1.length() - 1)) + value2);
		
		//	no indication, be conservative
		return null;
	}
	
	
	private void restoreFootnotes(MutableAnnotation data) {
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	collect tokens in order to assess which joins make sense
		StringVector tokens = new StringVector();
		boolean lastWasBreak = true;
		for (int t = 0; t < data.size(); t++) {
			Token token = data.tokenAt(t);
			if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
				lastWasBreak = true;
			
			else {
				if (!lastWasBreak)
					tokens.addElementIgnoreDuplicates(token.getValue());
				lastWasBreak = false;
			}
		}
		
		
		//	attribute tokens with page numbers
		for (int p = (paragraphs.length - 1); p > 0; p--) {
			
			//	footnote restart to move
			if (!RESTART_FOOTNOTE_TYPE.equals(paragraphs[p].getAttribute(TYPE_ATTRIBUTE)))
				continue;
			
			if (DEBUG_FOOTNOTES)
				System.out.println("Restoring inrerrupted footnote: " + paragraphs[p].getValue());
			
			//	get page number
			int restartPageNumber = Integer.parseInt((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + paragraphs.length)));
			
			//	find next footnote further up the document
			for (int f = (p-1); f >= 0; f--) {
				
				//	we're exclusively after footnotes
				if (!FOOTNOTE_TYPE.equals(paragraphs[f].getAttribute(TYPE_ATTRIBUTE)))
					continue;
				
				if (DEBUG_FOOTNOTES)
					System.out.println("Found start of footnote: " + paragraphs[f].getValue());
				
				//	check page number
				int footnotePageNumber = Integer.parseInt((String) paragraphs[f].getAttribute(PAGE_NUMBER_ATTRIBUTE, ("0")));
				
				//	interrupted footnotes run across the bottom of two subsequent pages
				if (footnotePageNumber < (restartPageNumber - 1))
					break;
				
				//	neighboring paragraphs, may have been interrupted by artifact
				if (f == (p-1)) {
					
					//	create new paragraph
					MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphs[f].getStartIndex(), (paragraphs[f].size() + paragraphs[p].size()));
					newParagraph.copyAttributes(paragraphs[p]);
					newParagraph.copyAttributes(paragraphs[f]);
					
					if (DEBUG_FOOTNOTES)
						System.out.println("Restored footnote: " + newParagraph.getValue());
					
					//	store last page number if different
					if (restartPageNumber != footnotePageNumber)
						newParagraph.setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, ("" + restartPageNumber));
					
					//	aggregate bounding boxes
					String npBoxesString = this.getAggregateBoundingBoxesString(paragraphs[f], paragraphs[p]);
					if (npBoxesString != null)
						newParagraph.setAttribute(BOUNDING_BOX_ATTRIBUTE, npBoxesString);
					
					//	normalize paragraph seam
					Token firstParagraphEndToken = paragraphs[f].lastToken();
					Token secondParagraphsStartToken = paragraphs[p].firstToken();
					
					//	check for word division
					String jointForm = this.getJointForm(firstParagraphEndToken, secondParagraphsStartToken, tokens);
					
					//	normalize whitespace ...
					if (jointForm == null)
						data.setWhitespaceAfter((Gamta.insertSpace(firstParagraphEndToken, secondParagraphsStartToken) ? " " : ""), (paragraphs[f].getEndIndex()-1));
					
					//	... or concatenate divided word
					else {
						data.setValueAt(jointForm, (paragraphs[f].getEndIndex() - 1));
						data.removeTokensAt(paragraphs[p].getStartIndex(), 1);
					}
					
					//	remove old paragraphs
					paragraphs[f].lastToken().removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
					data.removeAnnotation(paragraphs[p]);
					data.removeAnnotation(paragraphs[f]);
					
					//	store new paragraph for merges to come
					paragraphs[f] = newParagraph;
					paragraphs[p] = null;
				}
				
				//	really something in between
				else {
					
					MutableAnnotation moveParagraph = Gamta.copyDocument(paragraphs[p]);
					
					//	remove moved tokens
					TokenSequence removed = data.removeTokens(paragraphs[p]);
					if (DEBUG_FOOTNOTES)
						System.out.println("Removed old part footnote: " + removed.toString());
					
					//	insert tokens
					data.insertTokensAt(moveParagraph, paragraphs[f].getEndIndex());
					
					//	create new paragraph
					MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphs[f].getStartIndex(), (paragraphs[f].size() + moveParagraph.size()));
					newParagraph.copyAttributes(moveParagraph);
					newParagraph.copyAttributes(paragraphs[f]);
					
					if (DEBUG_FOOTNOTES)
						System.out.println("Restored footnote: " + newParagraph.getValue());
					
					//	move annotations
					Annotation[] moveAnnotations = moveParagraph.getAnnotations();
					for (int m = 0; m < moveAnnotations.length; m++) {
						if (MutableAnnotation.PARAGRAPH_TYPE.equals(moveAnnotations[m].getType()))
							continue;
						newParagraph.addAnnotation(moveAnnotations[m].getType(), (paragraphs[f].getEndIndex() + moveAnnotations[m].getStartIndex()), moveAnnotations[m].size()).copyAttributes(moveAnnotations[m]);
					}
					
					//	store last page number if different
					if (restartPageNumber != footnotePageNumber)
						newParagraph.setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, ("" + restartPageNumber));
					
					//	aggregate bounding boxes
					String npBoxesString = this.getAggregateBoundingBoxesString(paragraphs[f], moveParagraph);
					if (npBoxesString != null)
						newParagraph.setAttribute(BOUNDING_BOX_ATTRIBUTE, npBoxesString);
					
					//	normalize paragraph seam
					Token firstParagraphEndToken = paragraphs[f].lastToken();
					Token secondParagraphsStartToken = data.tokenAt(paragraphs[f].getEndIndex());
					
					//	check for word division
					String jointForm = this.getJointForm(firstParagraphEndToken, secondParagraphsStartToken, tokens);
					
					//	normalize whitespace ...
					if (jointForm == null)
						data.setWhitespaceAfter((Gamta.insertSpace(firstParagraphEndToken, secondParagraphsStartToken) ? " " : ""), (paragraphs[f].getEndIndex()-1));
					
					//	... or concatenate devided word
					else {
						data.setValueAt(jointForm, (paragraphs[f].getEndIndex() - 1));
						data.removeTokensAt(paragraphs[f].getEndIndex(), 1);
					}
					
					//	remove old paragraphs
					paragraphs[f].lastToken().removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
					data.removeAnnotation(paragraphs[f]);
					
					//	store new paragraph for merges to come
					paragraphs[f] = newParagraph;
					paragraphs[p] = null;
				}
				
				//	we're done with this one
				f = -1;
			}
			
			//	could not find start
			if (paragraphs[p] != null)
				paragraphs[p].setAttribute(TYPE_ATTRIBUTE, FOOTNOTE_TYPE);
		}
	}
	
	private String getAggregateBoundingBoxesString(Annotation first, Annotation second) {
		
		//	get base boxes
		BoundingBox fpBox = BoundingBox.getBoundingBox(first);
		BoundingBox[] spBoxes = BoundingBox.getBoundingBoxes(second);
		if ((fpBox == null) || (spBoxes == null))
			return null;
		
		//	get page IDs
		int fpId = Integer.parseInt((String) first.getAttribute(PAGE_ID_ATTRIBUTE));
		int spId = Integer.parseInt((String) second.getAttribute(PAGE_ID_ATTRIBUTE));
		String aggregateBoxesString;
		
		//	aggregate first two boxes
		if (fpId == spId) {
			BoundingBox[] boxAggregator = {fpBox, spBoxes[0]};
			spBoxes[0] = BoundingBox.aggregate(boxAggregator);
			aggregateBoxesString = "";
			for (int b = 0; b < spBoxes.length; b++)
				aggregateBoxesString += ((spBoxes[b] == null) ? "[]" : spBoxes[b].toString());
		}
		
		//	concatenate boxes
		else {
			aggregateBoxesString = fpBox.toString();
			for (int pid = (fpId+1); pid < spId; pid++)
				aggregateBoxesString += "[]";
			for (int b = 0; b < spBoxes.length; b++)
				aggregateBoxesString += ((spBoxes[b] == null) ? "[]" : spBoxes[b].toString());
		}
		
		//	return aggregate
		return aggregateBoxesString;
	}
	
	
	private PageNormalizerFeedbackPanel getFeedbackPanel(MutableAnnotation page, Annotation[] lines) {
		
		//	create and configure feedback panel
		PageNormalizerFeedbackPanel scfp = new PageNormalizerFeedbackPanel("Check Page Structure");
		scfp.setLabel("<HTML>Please check for each line if it" + // TODO keep this text in sync as available types evolve
				"<BR><B>starts</B> a new paragraph (main text, enumeration or bulletin list item, heading, footnote, caption, citation)," +
				"<BR><B>restarts</B> an interrupted paragraph (main text (including enumeration or bulletin list items) or footnote)," +
				"<BR><B>continues</B> the paragraph above it," +
				"<BR>or is an <B>OCR artifact</B> (stain on paper, etc.) that does not contribute to the text at all.</HTML>");
		scfp.setContinueCategory(CONTINUE_CATEGORY);
		for (int c = 0; c < LINE_CATEGORIES.length; c++)
			if (!CONTINUE_CATEGORY.equals(LINE_CATEGORIES[c])) {
				scfp.addCategory(LINE_CATEGORIES[c]);
				scfp.setCategoryColor(LINE_CATEGORIES[c], this.getTypeColor(LINE_CATEGORIES[c]));
			}
		scfp.setChangeSpacing(10);
		scfp.setContinueSpacing(0);
		
		//	compute bounding boxes
		BoundingBox[] lbbs = new BoundingBox[lines.length];
		for (int l = 0; l < lines.length; l++)
			lbbs[l] = BoundingBox.getBoundingBox(lines[l]);
		
		//	expand bounding boxes to page width (reveals indents, etc.)
		BoundingBox pbb = BoundingBox.getBoundingBox(page);
		if (pbb == null)
			pbb = BoundingBox.aggregate(lbbs);
		if (pbb != null)
			for (int l = 0; l < lines.length; l++) {
				if (lbbs[l] == null)
					continue;
				int left = pbb.left;
				int right = pbb.right;
				for (int cl = 0; cl < lines.length; cl++) {
					if (l == cl)
						continue;
					if ((lbbs[l].bottom < lbbs[cl].top) || (lbbs[cl].bottom < lbbs[l].top))
						continue;
					if (lbbs[l].right < lbbs[cl].left)
						right = lbbs[l].right;
					if (lbbs[cl].right < lbbs[l].left)
						left = Math.max((lbbs[cl].right + 1), left);
				}
				lbbs[l] = new BoundingBox(left, right, lbbs[l].top, lbbs[l].bottom);
			}
		
		//	add lines to dialog
		for (int l = 0; l < lines.length; l++) {
			String lineCategory = getCategoryForType((String) lines[l].getAttribute(TYPE_ATTRIBUTE));
			
			//	local feedback service, use local image provider if configured
			if (FeedbackPanel.isLocal()) {
				scfp.addLine(("<HTML>" + AnnotationUtils.escapeForXml(lines[l].getValue()) + "</HTML>"), lineCategory);
//				Swing is too slow at rendering images
//				if ((this.localImageProvider == null) || (bb == null))
//					scfp.addLine(("<HTML>" + IoTools.prepareForHtml(lines[l].getValue()) + "</HTML>"), lineTypes[l]);
//				else scfp.addLine(("<HTML><img src=\"" + (this.localImageProvider + page.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE) + "/" + page.getAttribute(PAGE_ID_ATTRIBUTE) + "." + IMAGE_FORMAT + "?" + BOUNDING_BOX_ATTRIBUTE + "=" + bb.toString()) + "\" alt=\"" + AnnotationUtils.escapeForXml(lines[l].getValue()) + "\"></HTML>"), lineTypes[l]);
			}
			
			//	remote feedback service, use remote image provider if configured
			else {
				if ((this.remoteImageProvider == null) || (lbbs[l] == null))
					scfp.addLine(("<HTML>" + AnnotationUtils.escapeForXml(lines[l].getValue()) + "</HTML>"), lineCategory);
				else scfp.addLine(("<HTML><img class=\"lineImage\" src=\"" + (this.remoteImageProvider + page.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE) + "/" + page.getAttribute(PAGE_ID_ATTRIBUTE) + "." + IMAGE_FORMAT + "?" + BOUNDING_BOX_ATTRIBUTE + "=" + lbbs[l].toString()) + "\" alt=\"" + AnnotationUtils.escapeForXml(lines[l].getValue(), true) + "\"></HTML>"), lineCategory);
			}
		}
		
		//	add backgroung information
		scfp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, page.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
		scfp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((String) page.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
		scfp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
		scfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, page.getAnnotationID());
		scfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PAGE_TYPE);
		scfp.setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, ((String) page.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
		scfp.setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
		
		//	return the pane
		return scfp;
	}
	
	/**
	 * Slightly modified copy of StartContinueFeedbackPanel, altered (a) to
	 * specially treat the OCR artifact category and (b) render itself to HTML.
	 * This class is only public in order for remote feedback engines to be able
	 * to load it.
	 * 
	 * @author sautter
	 */
	public static class PageNormalizerFeedbackPanel extends FeedbackPanel {

		/** Default spacing between two annotations classified differently, 10 pixels */
		private static final int DEFAULT_CHANGE_SPACING = 10;
		int changeSpacing = DEFAULT_CHANGE_SPACING;
		
		/** Default label for the 'Continue' category, namely 'Continue' */
		private static final String DEFAULT_CONTINUE_LABEL = "Continue";
		String continueCategory = DEFAULT_CONTINUE_LABEL;
		
		/** Default spacing between two annotations classified equally, 0 pixels */
		private static final int DEFAULT_CONTINUE_SPACING = 0;
		int continueSpacing = DEFAULT_CONTINUE_SPACING;
		
		/** Default highlight color for annotations, Color.WHITE */
		private static final Color DEFAULT_DEFAULT_COLOR = Color.WHITE;
		Color defaultColor = DEFAULT_DEFAULT_COLOR;
		
		private static final String OCR_ARTIFACT_CATEGORY = "OCR Artifact";
		private static final String ENCODING = "UTF-8";
//		/**
//		 * @return the default highlight color.
//		 */
//		public Color getDefaultColor() {
//			return this.defaultColor;
//		}
//
//		/**
//		 * @param defaultColor the new default highlight color
//		 */
//		public void setDefaultColor(Color defaultColor) {
//			this.defaultColor = defaultColor;
//		}
//		
		/**
		 * @return the spacing between two lines with different categories
		 */
		public int getChangeSpacing() {
			return this.changeSpacing;
		}

		/**
		 * Set the spacing between two lines with different categories
		 * @param changeSpacing the new spacing between two lines with different
		 *            categories
		 */
		public void setChangeSpacing(int changeSpacing) {
			this.changeSpacing = changeSpacing;
			this.updateSpacing();
		}
		
		/**
		 * @return the spacing between two lines with equal categories
		 */
		public int getContinueSpacing() {
			return this.continueSpacing;
		}

		/**
		 * Set the spacing between two lines with equal categories
		 * @param continueSpacing the new spacing between two lines with equal
		 *            categories
		 */
		public void setContinueSpacing(int continueSpacing) {
			this.continueSpacing = continueSpacing;
			this.updateSpacing();
		}
		
		/**
		 * Retrieve the label for the 'Continue' category.
		 * @return the label for the 'Continue' category
		 */
		public String getContinueCategory() {
			return this.continueCategory;
		}
		
		/**
		 * Change the label of the category indicating that a line continues the
		 * entity the line before it belongs to.
		 * @param continueCategory the new label for the 'Continue' category
		 */
		public void setContinueCategory(String continueCategory) {
			String oldContinueCategory = this.continueCategory;
			this.continueCategory = continueCategory;
			for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
				LinePanel lp = ((LinePanel) lit.next());
				((CfpComboBoxModel) lp.categoryBox.getModel()).fireContentsChanged();
				if (oldContinueCategory.equals(lp.category)) {
					lp.categoryBox.setSelectedItem(this.continueCategory);
					lp.categoryBox.repaint();
				}
			}
		}
		
		private HashMap categoryColors = new HashMap();
		
		/**
		 * Retrieve the highlight color for some category
		 * @param category the category to retrieve the highlight color for
		 * @return the highlight color for the specified category
		 */
		public Color getCategoryColor(String category) {
			return (this.categoryColors.containsKey(category) ? ((Color) this.categoryColors.get(category)) : this.defaultColor);
		}
		
		/**
		 * Set the highlight color for some category
		 * @param category the category to set the highlight color for
		 * @param color the new highlight color for the specified category
		 */
		public void setCategoryColor(String category, Color color) {
			Color oldColor = this.getCategoryColor(category);
			if (!oldColor.equals(color)) {
				this.categoryColors.put(category, color);
				this.updateCategoryColor(category, color);
			}
		}
		
		/**
		 * Add an category to this categorization feedback panel. Adding an category
		 * more than once has no effect.
		 * @param category the category to add
		 */
		public void addCategory(String category) {
			if ((category != null) && this.categories.add(category))
				for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
					LinePanel lp = ((LinePanel) lit.next());
					((CfpComboBoxModel) lp.categoryBox.getModel()).fireContentsChanged();
					if (category.equals(lp.category)) {
						lp.categoryBox.setSelectedItem(category);
						lp.categoryBox.repaint();
					}
				}
		}
		
		/**
		 * Retrieve the categories currently available in this feedback panel.
		 * @return an array holding the categories currently available in this feedback
		 *         panel
		 */
		public String[] getCategories() {
			return ((String[]) this.categories.toArray(new String[this.categories.size()]));
		}
		
		private int getSpacing(String previousCategory, String category) {
			if (previousCategory == null) return 0;
			else return ((this.continueCategory.equals(category) || OCR_ARTIFACT_CATEGORY.equals(category)) ? this.continueSpacing : this.changeSpacing);
		}
		
		private void updateSpacing() {
			boolean doLayout = false;
			for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
				LinePanel lp = ((LinePanel) lit.next());
				int previousDist = this.getSpacing(((lp.previous == null) ? null : lp.previous.category), lp.category);
				if (lp.gbc.insets.top != previousDist) {
					lp.gbc.insets.top = previousDist;
					this.gbl.setConstraints(lp, lp.gbc);
					doLayout = true;
				}
			}
			
			if (doLayout)
				this.gbl.layoutContainer(this);
		}
		
		private void updateCategoryColor(String category, Color color) {
			boolean doLayout = false;
			String lastCategory = null;
			for (int l = 0; l < this.lines.size(); l++) {
				LinePanel lp = ((LinePanel) this.lines.get(l));
				if (category.equals(lp.category)) {
					lp.lineLabel.setBackground(color);
					doLayout = true;
				}
				if (this.continueCategory.equals(lp.category)) {
					if (category.equals(lastCategory))
						lp.lineLabel.setBackground(brighten(color));
				}
				else lastCategory = lp.category;
			}
			if (doLayout) this.repaint();
		}
		
		private LinkedHashSet categories = new LinkedHashSet();
		private ArrayList lines = new ArrayList();
		
		private GridBagLayout gbl = new GridBagLayout();
		private GridBagConstraints gbc = new GridBagConstraints();
		
		/**
		 * Constructor
		 */
		public PageNormalizerFeedbackPanel() {
			this(null);
		}

		/**
		 * @param title
		 */
		public PageNormalizerFeedbackPanel(String title) {
			super(title);
			this.setLayout(this.gbl);
			this.gbc.insets.top = 0;
			this.gbc.insets.bottom = 0;
			this.gbc.insets.left = 0;
			this.gbc.insets.right = 0;
			this.gbc.fill = GridBagConstraints.HORIZONTAL;
			this.gbc.weightx = 1;
			this.gbc.weighty = 0;
			this.gbc.gridwidth = 1;
			this.gbc.gridheight = 1;
			this.gbc.gridx = 0;
			this.gbc.gridy = 0;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return (this.lines.size() * (this.categories.size() + 1));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return (this.categories.size() + 1);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.lines.size();
		}
		
//		/**
//		 * Append a line to classify to the feedback panel. The category for the new
//		 * line will be set to the configured 'continue' category.
//		 * @param line the line of text to classify (may contain HTML markup)
//		 */
//		public void addLine(String line) {
//			this.addLine(line, null);
//		}
//		
		/**
		 * Append a line to classify to the feedback panel. Specifying a category
		 * not yet added through the addCategory() method will result in that
		 * category being added. Specifying null as the category will result in the
		 * category being set to the configured 'continue' category.
		 * @param line the line of text to classify (may contain HTML markup)
		 * @param category the initial classification of the line to classify
		 */
		public void addLine(String line, String category) {
			if (category == null)
				category = this.continueCategory;
			else if (!this.continueCategory.equals(category)) this.addCategory(category);
			
			this.gbc.gridy = this.lines.size();
			LinePanel lp = new LinePanel(line, category, ((GridBagConstraints) this.gbc.clone()));
			if (this.lines.size() != 0) {
				LinePanel previousLp = ((LinePanel) this.lines.get(this.lines.size() - 1));
				previousLp.next = lp;
				lp.previous = previousLp;
				lp.gbc.insets.top = this.getSpacing(previousLp.category, lp.category);
			}
			this.lines.add(lp);
			
			if (this.continueCategory.equals(lp.category) && (lp.previous != null)) {
				if (this.continueCategory.equals(lp.previous.category))
					lp.lineLabel.setBackground(lp.previous.lineLabel.getBackground());
				else lp.lineLabel.setBackground(brighten(lp.previous.lineLabel.getBackground()));
			}
			
			this.add(lp, lp.gbc);
		}
		
		/**
		 * Retrieve the lines displayed in this feedback panel for classification.
		 * @return an array holding the contained lines
		 */
		public String[] getLines() {
			String[] lines = new String[this.lines.size()];
			int l = 0;
			for (Iterator lit = this.lines.iterator(); lit.hasNext();)
				lines[l++] = ((LinePanel) lit.next()).lineLabel.getText();
			return lines;
		}
		
		/**
		 * Retrieve the current classification status of each of the lines displayed
		 * in this feedback panel for classification.
		 * @return an array holding the classification status of each of the
		 *         contained lines
		 */
		public String[] getSelectedCategories() {
			String[] categories = new String[this.lines.size()];
			int l = 0;
			for (Iterator lit = this.lines.iterator(); lit.hasNext();)
				categories[l++] = ((LinePanel) lit.next()).category;
			return categories;
		}
		
		/**
		 * Retrieve the text of the index-th line in this feedback panel
		 * @param index the index of the line to look at
		 * @return the text of the index-th line
		 */
		public String getLineAt(int index) {
			return ((LinePanel) this.lines.get(index)).lineLabel.getText();
		}
		
		/**
		 * Retrieve the category currently assigned to the index-th line in this
		 * feedback panel
		 * @param index the index of the line to look at
		 * @return the category assigned to the index-th line
		 */
		public String getCategoryAt(int index) {
			return ((LinePanel) this.lines.get(index)).category;
		}
		
		/**
		 * Set the category assigned to the index-th line in this feedback panel. If
		 * the specified category is not present in the feedback panel, it is added.
		 * @param index the index of the line to assignd the category to
		 * @param category the category to assign to the index-th line
		 */
		public void setCategoryAt(int index, String category) {
			if (!this.continueCategory.equals(category))
				this.addCategory(category);
			((LinePanel) this.lines.get(index)).categoryBox.setSelectedItem(category);
		}
		
		/**
		 * @return the number of lines in this feedback panel
		 */
		public int lineCount() {
			return this.lines.size();
		}
		
		private class LinePanel extends JPanel {
			String category;
			JComboBox categoryBox;
			
			String line;
			JLabel lineLabel;
			
			LinePanel previous;
			LinePanel next;
			
			GridBagConstraints gbc;
			
			LinePanel(String line, String category, GridBagConstraints gbc) {
				super(new BorderLayout(), true);
				
				this.gbc = gbc;
				
				this.line = line;
				if ((this.line.length() < 6) || !"<html>".equals(this.line.substring(0, 6).toLowerCase()))
					line = ("<HTML>" + line + "</HTML>");
				this.lineLabel = new JLabel(line, JLabel.LEFT);
				this.lineLabel.setOpaque(true);
				this.lineLabel.setBackground(getCategoryColor(category));
				
				this.category = category;
				this.categoryBox = new JComboBox(new CfpComboBoxModel());
				this.categoryBox.setMaximumRowCount(Math.min(20, this.categoryBox.getItemCount()));
				this.categoryBox.setSelectedItem(this.category);
				
				this.categoryBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (ie.getStateChange() == ItemEvent.SELECTED) {
							String newCategory = categoryBox.getSelectedItem().toString();
							doCategoryChange(LinePanel.this, newCategory);
						}
					}
				});
				
				//	make sure drop-down is not expanded to text label height 
				JPanel categoryBoxPanel = new JPanel(new BorderLayout(), true);
				categoryBoxPanel.add(this.categoryBox, BorderLayout.NORTH);
				this.add(categoryBoxPanel, BorderLayout.WEST);
				
				this.add(this.lineLabel, BorderLayout.CENTER);
			}
		}
		
		private class CfpComboBoxModel extends AbstractListModel implements ComboBoxModel {
			private String selectedCategory = null;
			public Object getSelectedItem() {
				return this.selectedCategory;
			}
			public void setSelectedItem(Object anItem) {
				this.selectedCategory = ((anItem == null) ? null : anItem.toString());
			}
			public Object getElementAt(int index) {
				String[] categories = getCategories();
				return ((index == 0) ? continueCategory : categories[index - 1]);
			}
			public int getSize() {
				return (categories.size() + 1);
			}
			public void fireContentsChanged() {
				this.fireContentsChanged(this, 0, this.getSize());
			}
		}
		
		private void doCategoryChange(final LinePanel lp, String newCategory) {
			String oldCategory = lp.category;
			
			if (oldCategory.equals(newCategory)) return;
			else lp.category = newCategory;
			
			lp.gbc.insets.top = this.getSpacing(((lp.previous == null) ? null : lp.previous.category), lp.category);
			this.gbl.setConstraints(lp, lp.gbc);
			
			Color categoryContinueColor;
			LinePanel clp = lp;
			if (this.continueCategory.equals(lp.category)) {
				while ((clp.previous != null) && OCR_ARTIFACT_CATEGORY.equals(clp.previous.category))
					clp = clp.previous;
				if (clp.previous == null)
					categoryContinueColor = brighten(this.defaultColor);
				else {
					if (this.continueCategory.equals(clp.previous.category))
						categoryContinueColor = clp.previous.lineLabel.getBackground();
					else categoryContinueColor = brighten(clp.previous.lineLabel.getBackground());
				}
				lp.lineLabel.setBackground(categoryContinueColor);
			}
			else {
				Color categoryColor = this.getCategoryColor(lp.category);
				lp.lineLabel.setBackground(categoryColor);
				categoryContinueColor = brighten(categoryColor);
			}
			
			if (!OCR_ARTIFACT_CATEGORY.equals(lp.category)) {
				clp = lp;
				while ((clp != null) && ((clp = clp.next) != null)) {
					if (this.continueCategory.equals(clp.category))
						clp.lineLabel.setBackground(categoryContinueColor);
					else if (!OCR_ARTIFACT_CATEGORY.equals(clp.category))
						clp = null;
				}
			}
			
			if ((lp != null) && (lp.next != null)) {
				lp.next.gbc.insets.top = this.getSpacing(lp.category, lp.next.category);
				this.gbl.setConstraints(lp.next, lp.next.gbc);
			}
			
			this.gbl.layoutContainer(this);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			super.writeData(bw);
			
			//	write spacing and propagation
			bw.write("" + this.changeSpacing);
			bw.newLine();
			bw.write("" + this.continueSpacing);
			bw.newLine();
			bw.write(URLEncoder.encode(this.continueCategory, ENCODING));
			bw.newLine();
			
			//	get categories
			String[] categories = this.getCategories();
			
			//	write categories and colors (colors first, for fix length)
			for (int o = 0; o < categories.length; o++) {
				bw.write(getRGB(this.getCategoryColor(categories[o])) + " " + URLEncoder.encode(categories[o], ENCODING));
				bw.newLine();
			}
			bw.newLine();
			
			//	write data
			for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
				LinePanel lp = ((LinePanel) lit.next());
				bw.write(URLEncoder.encode(lp.category, ENCODING) + " " + URLEncoder.encode(lp.line, ENCODING));
				bw.newLine();
			}
			bw.newLine();
			
			//	send data
			bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			super.initFields(br);
			
			//	read spacing and propagation
			this.setChangeSpacing(Integer.parseInt(br.readLine()));
			this.setContinueSpacing(Integer.parseInt(br.readLine()));
			this.setContinueCategory(URLDecoder.decode(br.readLine(), ENCODING));
			
			//	read categories and colors (colors first, for fix length)
			String category;
			while (((category = br.readLine()) != null) && (category.length() != 0)) {
				Color categoryColor = getColor(category.substring(0, 6));
				category = URLDecoder.decode(category.substring(7), ENCODING);
				this.addCategory(category);
				this.setCategoryColor(category, categoryColor);
			}
			
			//	read data
			Iterator lit = this.lines.iterator();
			String line;
			while (((line = br.readLine()) != null) && (line.length() != 0)) {
				int split = line.indexOf(' ');
				category = URLDecoder.decode(line.substring(0, split), ENCODING);
				line = URLDecoder.decode(line.substring(split + 1), ENCODING);
				
				//	transferring category status only
				if ((lit != null) && lit.hasNext())
					((LinePanel) lit.next()).categoryBox.setSelectedItem(category);
				
				//	transferring whole content
				else {
					lit = null;
					this.addLine(line, category);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			Properties fs = new Properties();
			for (int l = 0; l < this.lineCount(); l++)
				fs.setProperty(("category" + l), this.getCategoryAt(l));
			return fs;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			for (int l = 0; l < this.lineCount(); l++)
				this.setCategoryAt(l, states.getProperty(("category" + l)));
		}
		
		
		//	HTML RENDERING CODE
		
		private Properties categoryMappings = new Properties();
		private String encodedContinueCategory;
		private String encodedOcrArtifactCategory;
		private static final int borderWidth = 4;
		
		private void initRendering() {
			if (this.encodedContinueCategory != null)
				return;
			String[] categories = this.getCategories();
			for (int c = 0; c < categories.length; c++) {
				String encodedCategory = categories[c].replaceAll("[^A-Za-z]", "");
				this.categoryMappings.setProperty(encodedCategory, categories[c]);
				this.categoryMappings.setProperty(categories[c], encodedCategory);
			}
			this.encodedContinueCategory = this.getContinueCategory().replaceAll("[^A-Za-z]", "");
			this.categoryMappings.setProperty(this.encodedContinueCategory, this.continueCategory);
			this.categoryMappings.setProperty(this.continueCategory, this.encodedContinueCategory);
			this.encodedOcrArtifactCategory = OCR_ARTIFACT_CATEGORY.replaceAll("[^A-Za-z]", "");
			this.categoryMappings.setProperty(this.encodedOcrArtifactCategory, OCR_ARTIFACT_CATEGORY);
			this.categoryMappings.setProperty(OCR_ARTIFACT_CATEGORY, this.encodedOcrArtifactCategory);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			this.initRendering();
			
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			String[] categories = this.getCategories();
			for (int c = 0; c < categories.length; c++)
				blw.writeLine("  borderColors." + this.categoryMappings.getProperty(categories[c]) + " = '#" + FeedbackPanel.getRGB(this.getCategoryColor(categories[c])) + "';");
			
			for (int c = 0; c < categories.length; c++)
				blw.writeLine("  backgroundColors." + this.categoryMappings.getProperty(categories[c]) + " = '#" + FeedbackPanel.getRGB(FeedbackPanel.brighten(this.getCategoryColor(categories[c]))) + "';");
			
			blw.writeLine("  ");
			blw.writeLine("  var selects = document.getElementsByTagName('select');");
			blw.writeLine("  var selectNumber = 0;");
			blw.writeLine("  var lastSelect;");
			blw.writeLine("  for (s = 0; s < selects.length; s++) {");
			blw.writeLine("    selects[s].id = ('category' + selectNumber);");
			blw.writeLine("    selects[s].number = selectNumber;");
			blw.writeLine("    if (lastSelect) {");
			blw.writeLine("      lastSelect.next = selects[s];");
			blw.writeLine("      selects[s].previous = lastSelect;");
			blw.writeLine("    }");
			blw.writeLine("    lastSelect = selects[s];");
			blw.writeLine("    selectNumber++;");
			blw.writeLine("  }");
			blw.writeLine("  change(0);");
			
			if (blw != out)
				blw.flush();
		}
	
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			this.initRendering();
			
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("function change(id) {");
			blw.writeLine("  var category = document.getElementById('category' + 0);");
			blw.writeLine("  var borderColor = ((category.value == '" + this.encodedContinueCategory + "') ? '" + getRGB(this.defaultColor) + "' : borderColors[category.value]);");
			blw.writeLine("  var backgroundColor = ((category.value == '" + this.encodedContinueCategory + "') ? '" + getRGB(this.defaultColor) + "' : backgroundColors[category.value]);");
			blw.writeLine("  while (category) {");
			blw.writeLine("    var line = document.getElementById('line' + category.number);");
			blw.writeLine("    var input = document.getElementById('input' + category.number);");
			blw.writeLine("    ");
			blw.writeLine("    if ((category.value != '" + this.encodedContinueCategory + "') && (category.value != '" + this.encodedOcrArtifactCategory + "')) {");
			blw.writeLine("      borderColor = borderColors[category.value];");
			blw.writeLine("      backgroundColor = backgroundColors[category.value];");
			blw.writeLine("    }");
			blw.writeLine("    if (category.value == '" + this.encodedOcrArtifactCategory + "') {");
			blw.writeLine("      line.style.borderColor = borderColors[category.value];");
			blw.writeLine("      line.style.backgroundColor = backgroundColors[category.value];");
			blw.writeLine("    }");
			blw.writeLine("    else {");
			blw.writeLine("      line.style.borderColor = borderColor;");
			blw.writeLine("      line.style.backgroundColor = backgroundColor;");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    if (category.previous == null) {");
			blw.writeLine("      line.style.marginTop = 0;");
			blw.writeLine("      input.style.marginTop = 0;");
			blw.writeLine("      line.style.borderTopWidth = " + borderWidth + ";");
			blw.writeLine("    }");
			blw.writeLine("    else if ((category.value == '" + this.encodedContinueCategory + "') || ((category.value == '" + this.encodedOcrArtifactCategory + "') && category.next && (category.next.value == '" + this.encodedContinueCategory + "'))) {");
			blw.writeLine("      line.style.marginTop = continueSpacing;");
			blw.writeLine("      input.style.marginTop = continueSpacing;");
			blw.writeLine("      line.style.borderTopWidth = 0;");
			blw.writeLine("    }");
			blw.writeLine("    else {");
			blw.writeLine("      line.style.marginTop = changeSpacing;");
			blw.writeLine("      input.style.marginTop = changeSpacing;");
			blw.writeLine("      line.style.borderTopWidth = " + borderWidth + ";");
			blw.writeLine("    }");
			blw.writeLine("    line.style.borderBottomWidth = ((!category.next || ((category.next.value != '" + this.encodedContinueCategory + "') && (category.next.value != '" + this.encodedOcrArtifactCategory + "'))) ? " + borderWidth + " : 0);");
			blw.writeLine("    category = category.next;");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("var borderColors = new Object();");
			blw.writeLine("var backgroundColors = new Object();");
			blw.writeLine("");
			blw.writeLine("var changeSpacing = " + this.getChangeSpacing() + ";");
			blw.writeLine("var continueSpacing = " + this.getContinueSpacing() + ";");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			this.initRendering();
			
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("table {");
			blw.writeLine("  border-width: 0;");
			blw.writeLine("  border-spacing: 0;");
			blw.writeLine("  border-collapse: collapse;");
			blw.writeLine("}");
			blw.writeLine("td {");
			blw.writeLine("  padding: 0;");
			blw.writeLine("  margin: 0;");
			blw.writeLine("}");
			blw.writeLine(".labelCell {");
			blw.writeLine("  width: 100%;");
			blw.writeLine("  border-spacing: 0;");
			blw.writeLine("}");
			blw.writeLine(".label {");
			blw.writeLine("  white-space: nowrap;");
			blw.writeLine("  border-style: solid;");
			blw.writeLine("  border-spacing: 0;");
			blw.writeLine("  border-left-width: " + borderWidth + ";");
			blw.writeLine("  border-right-width: " + borderWidth + ";");
			blw.writeLine("}");
			blw.writeLine(".lineImage {");
			blw.writeLine("  opacity: 0.6;");
			blw.writeLine("  filter: alpha(opacity=60);");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			this.initRendering();
			
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("<table class=\"inputTable\">");
			
			String[] categories = this.getCategories();
			
			for (int l = 0; l < this.lineCount(); l++) {
				blw.writeLine("<tr>");
				
				/*
				<p id="input0"><select name="category0" onchange="change(0);">
				  <option value="S">Start</option>
				  <option value="C">Continue</option>
				  <option value="O" selected>Other</option>
				</select></p>
        		 */
				blw.writeLine("<td class=\"inputCell\">");
				
				blw.writeLine("<p id=\"input" + l + "\">");
				blw.writeLine("<select type=\"checkbox\" name=\"category" + l + "\" onchange=\"change(" + l + ");\">");
				
				String category = this.getCategoryAt(l);
				blw.write("<option value=\"" + this.categoryMappings.getProperty(this.continueCategory) + "\"" + (category.equals(this.continueCategory) ? " selected" : "") + ">");
				blw.write(prepareForHtml(this.continueCategory));
				blw.writeLine("</option>");
				for (int c = 0; c < categories.length; c++) {
					blw.write("<option value=\"" + this.categoryMappings.getProperty(categories[c]) + "\"" + (category.equals(categories[c]) ? " selected" : "") + ">");
					blw.write(prepareForHtml(categories[c]));
					blw.writeLine("</option>");
				}
				
				blw.writeLine("</select>");
				blw.writeLine("</p>");
				
				blw.writeLine("</td>");
				
				//<p id="line0">Test1</p>
				blw.writeLine("<td class=\"labelCell\">");
				
				blw.write("<p id=\"line" + l + "\" class=\"label\">");
				blw.write(prepareForHtml(this.getLineAt(l)));
				blw.writeLine("</p>");
				
				blw.writeLine("</td>");
				
				blw.writeLine("</tr>");
			}
			
			blw.writeLine("</table>");
			
			if (blw != out)
				blw.flush();
		}
		
		private static final String prepareForHtml(String string) {
			if (string == null) return null;
			else if (
					string.startsWith("<") // quick pre-check
					&&
					(string.length() > 7) // prevent StringIndexOutOfBoundsException for subsequent checks
					&&
					string.substring(0, 6).equalsIgnoreCase("<HTML>") // avoid converting the whole string to upper case
					&&
					string.substring(string.length() - 7).equalsIgnoreCase("</HTML>") // avoid converting the whole string to upper case
				)
				
				//	String with markup, just strip HTML indicator tags
				return string.substring(6, (string.length() - 7));
			
			//	plain String, encode special characters
			else return IoTools.prepareForHtml(string);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			this.initRendering();
			
			for (int l = 0; l < this.lineCount(); l++)
				this.setCategoryAt(l, this.categoryMappings.getProperty(response.getProperty(("category" + l))));
		}
	}
	
	private HashMap highlightAttributeCache = new HashMap();
	private Color getTypeColor(String type) {
		Color color = ((Color) this.highlightAttributeCache.get(type));
		if (color == null) {
			if (MAIN_TEXT_CATEGORY.equals(type))
				color = FeedbackPanel.brighten(Color.LIGHT_GRAY);
			else if (RESTART_MAIN_TEXT_CATEGORY.equals(type))
				color = Color.LIGHT_GRAY;
			else if (CAPTION_CATEGORY.equals(type))
				color = Color.PINK;
			else if (TABLE_CATEGORY.equals(type))
				color = Color.GRAY;
			else if (FOOTNOTE_CATEGORY.equals(type))
				color = FeedbackPanel.brighten(Color.ORANGE);
			else if (RESTART_FOOTNOTE_CATEGORY.equals(type))
				color = Color.ORANGE;
			else if (CITATION_CATEGORY.equals(type))
				color = Color.RED;
			else if (PAGE_TITLE_CATEGORY.equals(type))
				color = Color.GREEN;
			else if (ENUMERATION_ITEM_CATEGORY.equals(type))
				color = Color.CYAN;
			else if (BULLETIN_LIST_ITEM_CATEGORY.equals(type))
				color = FeedbackPanel.brighten(Color.BLUE);
			else if (HEADING_CATEGORY.equals(type))
				color = Color.YELLOW;
			else if (OCR_ARTIFACT_CATEGORY.equals(type))
				color = Color.DARK_GRAY;
			else color = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			this.highlightAttributeCache.put(type, color);
		}
		return color;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load page numbers
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() != 0) && !characterLine.startsWith("//")) {
					int split = characterLine.indexOf(' ');
					if (split == -1)
						continue;
					String digit = characterLine.substring(0, split).trim();
					String characters = characterLine.substring(split).trim();
					if (Gamta.isNumber(digit)) {
						pageNumberCharacters.add(digit);
						for (int c = 0; c < characters.length(); c++) {
							String character = characters.substring(c, (c+1));
							pageNumberCharacters.add(character);
							ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(character));
							if (characterTranslations == null) {
								characterTranslations = new ArrayList(2);
								this.pageNumberCharacterTranslations.put(character, characterTranslations);
							}
							characterTranslations.add(digit);
						}
					}
				}
			}
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer pncPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String pnc = ((String) cit.next());
				pncPatternBuilder.append(pnc);
				ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(pnc));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.pageNumberCharacterTranslations.put(pnc, cts);
			}
			String pncPattern = ("[" + RegExUtils.escapeForRegEx(pncPatternBuilder.toString()) + "]");
			this.pageNumberPattern = (pncPattern + "+(\\s?" + pncPattern + "+)*");
		}
		catch (IOException ioe) {
			this.pageNumberPattern = "[0-9]++";
		}
		
		
		//	load hard limits
		try {
			this.maxTopTitleLines = Integer.parseInt(this.getParameter(maxTopTitleParagraphsSetting));
		} catch (NumberFormatException nfe) {}
		
		try {
			this.maxBottomTitleLines = Integer.parseInt(this.getParameter(maxBottomTitleParagraphsSetting));
		} catch (NumberFormatException nfe) {}
		
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
		
		//	load patterns
		try {
			InputStream is = this.dataProvider.getInputStream("pageTitlePatterns.txt");
			StringVector pageTitlePatterns = StringVector.loadList(is);
			is.close();
			for (int p = 0; p < pageTitlePatterns.size(); p++) {
				String ptp = pageTitlePatterns.get(p).trim();
				if ((ptp.length() == 0) || ptp.startsWith("//"))
					pageTitlePatterns.remove(p--);
			}
			this.pageTitlePatterns = pageTitlePatterns.toStringArray();
		}
		catch (IOException ioe) {}
		
		
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
		
		
		//	load footnote data
		this.noiseWords = Gamta.getNoiseWords();
		this.citationParts.parseAndAddElements(CITATION_PART_STRING, ";");
		
		
		//	load citation patterns
		try {
			this.maxCitationLines = Integer.parseInt(this.getParameter(MAX_CITATION_LINES_SETTING));
		} catch (Exception nfe) {
			this.storeParameter(MAX_CITATION_LINES_SETTING, ("" + this.maxCitationLines));
		}
		
		try {
			this.maxCitationWords = Integer.parseInt(this.getParameter(MAX_CITATION_WORDS_SETTING));
		} catch (Exception nfe) {
			this.storeParameter(MAX_CITATION_WORDS_SETTING, ("" + this.maxCitationWords));
		}
		
		try {
			this.activeCitationPatternNames.addContentIgnoreDuplicates(this.loadList(CITATION_PATTERN_NAME_FILE));
		} catch (IOException e) {}
		
		this.buildCitationPatterns();
		
		
		//	initialize annotations to move on normalization
		this.toMove.addElementIgnoreDuplicates(CAPTION_TYPE);
		this.toMove.addElementIgnoreDuplicates(FOOTNOTE_TYPE);
		this.toMove.addElementIgnoreDuplicates(RESTART_FOOTNOTE_TYPE);
		this.toMove.addElementIgnoreDuplicates(TABLE_TYPE);
		
		
		//	load type colors
		try {
			InputStream is = this.dataProvider.getInputStream("lineTypeColors.txt");
			StringVector typeLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeLines.size(); t++) {
				String typeLine = typeLines.get(t).trim();
				if ((typeLine.length() != 0) && !typeLine.startsWith("//")) {
					int split = typeLine.indexOf(' ');
					if (split == -1)
						continue;
					String type = typeLine.substring(0, split).trim();
					Color color = FeedbackPanel.getColor(typeLine.substring(split).trim());
					this.highlightAttributeCache.put(type, color);
				}
			}
		} catch (IOException ioe) {}
	}
	
	private void buildCitationPatterns() {
		StringVector regExes = new StringVector();
		StringVector regExNames = new StringVector();
		
		Properties resolver = this.getSubPatternNameResolver();
		for (int r = 0; r < activeCitationPatternNames.size(); r++) {
			String patternName = activeCitationPatternNames.get(r);
			try {
				InputStream is = this.dataProvider.getInputStream(patternName);
				StringVector rawRegEx = StringVector.loadList(is);
				is.close();
				regExes.addElement(RegExUtils.preCompile(RegExUtils.normalizeRegEx(rawRegEx.concatStrings("\n")), resolver));
				regExNames.addElement(patternName);
			}
			catch (IOException ioe) {
				System.out.println("CitationFinder: could not load pattern '" + patternName + "':\n  " + ioe.getClass().getName() + " (" + ioe.getMessage() + ")");
			}
			catch (PatternSyntaxException pse) {
				System.out.println("CitationFinder: could not compile pattern '" + patternName + "':\n  " + pse.getClass().getName() + " (" + pse.getMessage() + ")");
			}
		}
		
		this.citationPatterns = regExes.toStringArray();
		this.citationPatternNames = regExNames.toStringArray();
	}
	
	private Properties getSubPatternNameResolver() {
		return new Resolver();
	}
	
	private class Resolver extends Properties {
		public String getProperty(String name, String def) {
			try {
				if (!name.endsWith(REGEX_NAME_EXTENSION)) name += REGEX_NAME_EXTENSION;
				InputStream is = dataProvider.getInputStream(name);
				StringVector rawRegEx = StringVector.loadList(is);
				is.close();
				return RegExUtils.preCompile(RegExUtils.normalizeRegEx(rawRegEx.concatStrings("\n")), this);
			}
			catch (IOException ioe) {
				return def;
			}
		}
		public String getProperty(String name) {
			return this.getProperty(name, null);
		}
	}
}