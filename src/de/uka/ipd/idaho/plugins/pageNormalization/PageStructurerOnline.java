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

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.StartContinueFeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class PageStructurerOnline extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String LINE_ANNOTATION_TYPE = "line";

	private static final String TO_MOVE_BLOCK_ANNOTATION_TYPE = "toMoveBlock";
	
	//	subSection types for complex rules
	private static final String MAIN_TEXT_TYPE = "mainText";
	private static final String CONTINUE_MAIN_TEXT_TYPE = "continueMainText";
	private static final String CONTINUE_FOOTNOTE_TYPE = "continueFootnote";
	private static final String HEADING_TYPE = "heading";
	private static final String TABLE_TYPE = "table";
	private static final String OCR_ARTIFACT_TYPE = "ocrArtifact";
	
	//	categories for complex rules
	private static final String MAIN_TEXT_LABEL = "Main Text";
	private static final String CONTINUE_MAIN_TEXT_LABEL = "Continue Main Text";
	private static final String CAPTION_LABEL = "Caption";
	private static final String FOOTNOTE_LABEL = "Footnote";
	private static final String CONTINUE_FOOTNOTE_LABEL = "Continue Footnote";
	private static final String BIB_REF_LABEL = "Bibliographic Reference";
	private static final String PAGE_TITLE_LABEL = "Page Header / Footer";
	private static final String HEADING_LABEL = "Heading (of Chapter, Section, etc)";
	private static final String TABLE_LABEL = "Table";
	private static final String OCR_ARTIFACT_LABEL = "OCR / Layout Artifact";
	
	private static final String[] PARAGRAPH_TYPES = {
		MAIN_TEXT_TYPE,
		CONTINUE_MAIN_TEXT_TYPE,
		HEADING_TYPE,
		PAGE_TITLE_TYPE,
		CAPTION_TYPE,
		TABLE_TYPE,
		FOOTNOTE_TYPE,
		CONTINUE_FOOTNOTE_TYPE,
		BIBLIOGRAPHIC_REFERENCE_TYPE,
		OCR_ARTIFACT_TYPE,
	};
	
	private static HashSet mainTextTypes = new HashSet();
	private static HashSet mainTextBridgeableTypes = new HashSet();
	private static HashSet footnoteTypes = new HashSet();
	private static HashSet footnoteBridgeableTypes = new HashSet();
	static {
		mainTextTypes.add(MAIN_TEXT_TYPE);
		mainTextTypes.add(BIBLIOGRAPHIC_REFERENCE_TYPE);
		mainTextTypes.add(HEADING_TYPE);
		
		mainTextBridgeableTypes.add(FOOTNOTE_TYPE);
		mainTextBridgeableTypes.add(CONTINUE_FOOTNOTE_TYPE);
		mainTextBridgeableTypes.add(CAPTION_TYPE);
		mainTextBridgeableTypes.add(TABLE_TYPE);
		mainTextBridgeableTypes.add(PAGE_TITLE_TYPE);
		mainTextBridgeableTypes.add(OCR_ARTIFACT_TYPE);
		
		footnoteTypes.add(FOOTNOTE_TYPE);
		
		footnoteBridgeableTypes.add(MAIN_TEXT_TYPE);
		footnoteBridgeableTypes.add(CONTINUE_MAIN_TEXT_TYPE);
		footnoteBridgeableTypes.add(BIBLIOGRAPHIC_REFERENCE_TYPE);
		footnoteBridgeableTypes.add(HEADING_TYPE);
		footnoteBridgeableTypes.add(CAPTION_TYPE);
		footnoteBridgeableTypes.add(TABLE_TYPE);
		footnoteBridgeableTypes.add(PAGE_TITLE_TYPE);
		footnoteBridgeableTypes.add(OCR_ARTIFACT_TYPE);
	}
	
	private Properties typeCategories = new Properties();
	private String getTypeCategory(String type) {
		String label = this.typeCategories.getProperty(type);
		if (label != null) return label;
		
		if (MAIN_TEXT_TYPE.equals(type)) return MAIN_TEXT_LABEL;
		else if (CONTINUE_MAIN_TEXT_TYPE.equals(type)) return CONTINUE_MAIN_TEXT_LABEL;
		else if (CAPTION_TYPE.equals(type)) return CAPTION_LABEL;
		else if (FOOTNOTE_TYPE.equals(type)) return FOOTNOTE_LABEL;
		else if (CONTINUE_FOOTNOTE_TYPE.equals(type)) return CONTINUE_FOOTNOTE_LABEL;
		else if (BIBLIOGRAPHIC_REFERENCE_TYPE.equals(type)) return BIB_REF_LABEL;
		else if (PAGE_TITLE_TYPE.equals(type)) return PAGE_TITLE_LABEL;
		else if (HEADING_TYPE.equals(type)) return HEADING_LABEL;
		else if (OCR_ARTIFACT_TYPE.equals(type)) return OCR_ARTIFACT_LABEL;
		else if (TABLE_TYPE.equals(type)) return TABLE_LABEL;
		else return MAIN_TEXT_LABEL;
	}
	
	private Properties categoryTypes = new Properties();
	private String getCategoryType(String category) {
		String type = this.categoryTypes.getProperty(category);
		if (type != null) return type;
		
		if (MAIN_TEXT_LABEL.equals(category)) return MAIN_TEXT_TYPE;
		else if (CONTINUE_MAIN_TEXT_LABEL.equals(category)) return CONTINUE_MAIN_TEXT_TYPE;
		else if (CAPTION_LABEL.equals(category)) return CAPTION_TYPE;
		else if (FOOTNOTE_LABEL.equals(category)) return FOOTNOTE_TYPE;
		else if (CONTINUE_FOOTNOTE_LABEL.equals(category)) return CONTINUE_FOOTNOTE_TYPE;
		else if (BIB_REF_LABEL.equals(category)) return BIBLIOGRAPHIC_REFERENCE_TYPE;
		else if (PAGE_TITLE_LABEL.equals(category)) return PAGE_TITLE_TYPE;
		else if (HEADING_LABEL.equals(category)) return HEADING_TYPE;
		else if (OCR_ARTIFACT_LABEL.equals(category)) return OCR_ARTIFACT_TYPE;
		else if (TABLE_LABEL.equals(category)) return TABLE_TYPE;
		else return MAIN_TEXT_TYPE;
	}
	
	private HashMap typeColors = new HashMap();
	private Color getTypeColor(String type) {
		Color color = ((Color) this.typeColors.get(type));
		if (color == null) {
			if (MAIN_TEXT_TYPE.equals(type))
				color = FeedbackPanel.brighten(Color.LIGHT_GRAY);
			else if (CONTINUE_MAIN_TEXT_TYPE.equals(type))
				color = Color.LIGHT_GRAY;
			else if (CAPTION_TYPE.equals(type))
				color = Color.PINK;
			else if (TABLE_TYPE.equals(type))
				color = FeedbackPanel.brighten(Color.BLUE);
			else if (FOOTNOTE_TYPE.equals(type))
				color = FeedbackPanel.brighten(Color.ORANGE);
			else if (CONTINUE_FOOTNOTE_TYPE.equals(type))
				color = Color.ORANGE;
			else if (BIBLIOGRAPHIC_REFERENCE_TYPE.equals(type))
				color = Color.RED;
			else if (PAGE_TITLE_TYPE.equals(type))
				color = Color.GREEN;
			else if (HEADING_TYPE.equals(type))
				color = Color.CYAN;
			else if (OCR_ARTIFACT_TYPE.equals(type))
				color = Color.DARK_GRAY;
			else color = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			this.typeColors.put(type, color);
		}
		return color;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load type colors
		try {
			InputStream is = this.dataProvider.getInputStream("paragraphTypes.txt");
			StringVector typeLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeLines.size(); t++) {
				String typeLine = typeLines.get(t).trim();
				if ((typeLine.length() != 0) && !typeLine.startsWith("//")) {
					String[] typeData = typeLine.split("\\s", 3);
					if (typeData.length != 3)
						continue;
					String type = typeData[0];
					Color color = FeedbackPanel.getColor(typeData[1]);
					String label = typeData[2];
					this.typeColors.put(type, color);
					this.typeCategories.setProperty(type, label);
					this.categoryTypes.setProperty(label, type);
				}
			}
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		if (pages.length == 0)
			return;
		
		//	classify paragraphs, rememeber which exist, and mark lines
		HashMap[] existingParagraphs = new HashMap[pages.length];
		for (int pg = 0; pg < pages.length; pg++) {
			existingParagraphs[pg] = new HashMap();
			
			//	get page ID and page number to add them to tokens
			Object pageId = pages[pg].getAttribute(PAGE_ID_ATTRIBUTE);
			Object pageNumber = pages[pg].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			
			//	get page paragraphs
			MutableAnnotation[] paragraphs = pages[pg].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				existingParagraphs[pg].put((paragraphs[p].getStartIndex() + "-" + paragraphs[p].size()), paragraphs[p]);
				
				//	classify paragraph
				String paragraphType = this.getParagraphType(paragraphs[p]);
				
				//	add attributes to tokens, and mark lines
				int lineStart = 0;
				for (int t = 0; t < paragraphs[p].size(); t++) {
					paragraphs[p].tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageId);
					paragraphs[p].tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
					if (paragraphs[p].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) && (lineStart <= t)) {
						Annotation line = paragraphs[p].addAnnotation(LINE_ANNOTATION_TYPE, lineStart, (t - lineStart + 1));
						if (lineStart == 0)
							line.setAttribute(TYPE_ATTRIBUTE, paragraphType);
						lineStart = (t+1);
					}
				}
			}
		}
		
		//	build feedback panels
		StartContinueFeedbackPanel[] scfps = new StartContinueFeedbackPanel[pages.length];
		for (int pg = 0; pg < pages.length; pg++) {
			scfps[pg] = new StartContinueFeedbackPanel("Check Page Structure");
			scfps[pg].setLabel("<HTML>Plase make sure the grouping of the lines this page reflect the logical paragraphs, and also select their type:" +
					"<BR>- <B>main text</B> paragraph, the <B>continuation</B> of one, or a <B>heading</B>, e.g. of a section or chapter" +
					"<BR>- <B>footnote</B>, the <B>continuation</B> of one from an earlier page or column, or a <B>bibliographic reference</B>" +
					"<BR>- <B>caption</B>, e.g. of a figure or table, an actual <B>table</B> in itself, a <B>page header or footer</B>" +
					"<BR>- <B>OCR artifact</B> that is not actually text at all, but rather a stain on the page, a hand written mark, etc</HTML>");
			
			for (int t = 0; t < PARAGRAPH_TYPES.length; t++) {
				String typeLabel = this.getTypeCategory(PARAGRAPH_TYPES[t]);
				scfps[pg].addCategory(typeLabel);
				scfps[pg].setCategoryColor(typeLabel, this.getTypeColor(PARAGRAPH_TYPES[t]));
			}
			scfps[pg].setChangeSpacing(10);
			scfps[pg].setContinueSpacing(0);
			
			Annotation[] lines = pages[pg].getAnnotations(LINE_ANNOTATION_TYPE);
			for (int l = 0; l < lines.length; l++) {
				String line = TokenSequenceUtils.concatTokens(lines[l], true, true);
				String type = ((String) lines[l].getAttribute(TYPE_ATTRIBUTE));
				scfps[pg].addLine(line, ((type == null) ? null : this.getTypeCategory(type)));
			}
			
			scfps[pg].setProperty(FeedbackPanel.REQUESTER_CLASS_NAME_PROPERTY, this.getClass().getName());
			scfps[pg].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, pages[pg].getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			scfps[pg].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, pages[pg].getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			scfps[pg].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, pages[pg].getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			scfps[pg].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, pages[pg].getAnnotationID());
			scfps[pg].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PARAGRAPH_TYPE);
		}
		
		//	get feedback
		int cutoffPage = pages.length;
		if (FeedbackPanel.isMultiFeedbackEnabled())
			FeedbackPanel.getMultiFeedback(scfps);
		
		else for (int pg = 0; pg < scfps.length; pg++) {
			if (pg != 0)
				scfps[pg].addButton("Previous");
			scfps[pg].addButton("Cancel");
			scfps[pg].addButton("OK" + (((pg+1) == scfps.length) ? "" : " & Next"));
			
			String title = scfps[pg].getTitle();
			scfps[pg].setTitle(title + " - (" + (pg+1) + " of " + scfps.length + ")");
			
			String f = scfps[pg].getFeedback();
			if (f == null) f = "Cancel";
			
			scfps[pg].setTitle(title);
			
			//	current dialog submitted, keep as is
			if (f.startsWith("OK")) {}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				pg-=2;
			
			//	cancel from current dialog onward
			else {
				cutoffPage = pg;
				pg = scfps.length;
			}
		}
		
		//	process feedback, i.e., mark paragraphs in page
		for (int pg = 0; pg < cutoffPage; pg++) {
			Annotation[] lines = pages[pg].getAnnotations(LINE_ANNOTATION_TYPE);
			if (lines.length == 0)
				continue;
			
			int paragraphStart = lines[0].getStartIndex();
			String paragraphType = this.getCategoryType(scfps[pg].getCategoryAt(0));
			for (int l = 1; l < lines.length; l++) {
				String category = scfps[pg].getCategoryAt(l);
				if ((category == null) || category.equals(scfps[pg].getContinueCategory()))
					continue;
				String type = this.getCategoryType(category);
				if (type == null)
					continue;
				
				//	annotate paragraph
				Annotation paragraph = ((Annotation) existingParagraphs[pg].remove(paragraphStart + "-" + (lines[l].getStartIndex() - paragraphStart)));
				if (paragraph == null)
					paragraph = pages[pg].addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (lines[l].getStartIndex()- paragraphStart));
				paragraph.setAttribute(TYPE_ATTRIBUTE, paragraphType);
				paragraphStart = lines[l].getStartIndex();
				paragraphType = type;
			}
			
			//	mark last paragraph
			Annotation paragraph = ((Annotation) existingParagraphs[pg].remove(paragraphStart + "-" + (pages[pg].size() - paragraphStart)));
			if (paragraph == null)
				paragraph = pages[pg].addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (pages[pg].size()- paragraphStart));
			paragraph.setAttribute(TYPE_ATTRIBUTE, paragraphType);
			
			//	clean up replaced paragraphs
			for (Iterator pit = existingParagraphs[pg].keySet().iterator(); pit.hasNext();)
				pages[pg].removeAnnotation((Annotation) existingParagraphs[pg].get(pit.next()));
			
			//	cleanup artifacts
			Annotation[] paragraphs = pages[pg].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int p = (paragraphs.length-1); p >= 0; p--) {
				String type = ((String) paragraphs[p].getAttribute(TYPE_ATTRIBUTE));
				if (OCR_ARTIFACT_TYPE.equals(type) || PAGE_TITLE_TYPE.equals(type))
					pages[pg].removeTokens(paragraphs[p]);
			}
			
			//	anything left to work with?
			if (pages[pg].size() == 0)
				continue;
			
			//	add page break tokens
			Annotation pageBreakToken = pages[pg].addAnnotation(PAGE_BREAK_TOKEN_TYPE, 0, 1);
			pageBreakToken.setAttribute(PAGE_ID_ATTRIBUTE, pageBreakToken.firstToken().getAttribute(PAGE_ID_ATTRIBUTE));
			pageBreakToken.setAttribute(PAGE_NUMBER_ATTRIBUTE, pageBreakToken.firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE));
			
			//	add page IDs and numbers
			paragraphs = pages[pg].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				paragraphs[p].setAttribute(PAGE_ID_ATTRIBUTE, paragraphs[p].firstToken().getAttribute(PAGE_ID_ATTRIBUTE));
				paragraphs[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, paragraphs[p].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE));
			}
		}
		
		//	clean up lines
		AnnotationFilter.removeAnnotations(data, LINE_ANNOTATION_TYPE);
		
		//	we cannot clean up if we don't have feedback for all pages
		if (cutoffPage < scfps.length) {
			for (int t = 0; t < data.size(); t++) {
				data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
				data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
			}
			return;
		}
		
		//	clean up pages
		AnnotationFilter.removeAnnotations(data, PAGE_TYPE);
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	simply pull up 'continue' paragraphs to next possible predecessor
		for (int p = (paragraphs.length-1); p > 0; p--) {
			
			//	do we have a continuation type?
			if (!CONTINUE_MAIN_TEXT_TYPE.equals(paragraphs[p].getAttribute(TYPE_ATTRIBUTE)) && !CONTINUE_FOOTNOTE_TYPE.equals(paragraphs[p].getAttribute(TYPE_ATTRIBUTE)))
				continue;
			
			//	seek backward for paragraph to attach current one to
			for (int ap = (p-1); ap >= 0; ap--) {
				if (!this.canAttach(paragraphs[p], paragraphs[ap]))
					continue;
				
				//	remember to-merge paragraphs
				MutableAnnotation topParagraph = paragraphs[ap];
				MutableAnnotation bottomParagraph = paragraphs[p];
				
				//	push down the document what's in the way (if anything)
				if (ap < (p-1)) {
					
					//	mark tokens to move
					MutableAnnotation toMoveBlock = data.addAnnotation(TO_MOVE_BLOCK_ANNOTATION_TYPE, paragraphs[ap].getEndIndex(), (paragraphs[p].getStartIndex() - paragraphs[ap].getEndIndex()));
					
					//	insert tokens below current paragraph and mark them
					data.insertTokensAt(toMoveBlock, paragraphs[p].getEndIndex());
					MutableAnnotation movedBlock = data.addAnnotation(TO_MOVE_BLOCK_ANNOTATION_TYPE, paragraphs[p].getEndIndex(), toMoveBlock.size());
					
					//	transfer token attributes (page numbers!)
					for (int t = 0; (t < toMoveBlock.size()) && (t < movedBlock.size()); t++)
						movedBlock.tokenAt(t).copyAttributes(toMoveBlock.tokenAt(t));
					
					//	transfer attributes (including annotation ID)
					movedBlock.copyAttributes(toMoveBlock);
					movedBlock.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, toMoveBlock.getAnnotationID());
					
					//	transfer subordinate annotations
					Annotation[] toMoveBlockAnnotations = toMoveBlock.getAnnotations();
					for (int a = 0; a < toMoveBlockAnnotations.length; a++) {
						if (TO_MOVE_BLOCK_ANNOTATION_TYPE.equals(toMoveBlockAnnotations[a].getType()))
							continue;
						Annotation movedBlockAnnotation = movedBlock.addAnnotation(toMoveBlockAnnotations[a].getType(), toMoveBlockAnnotations[a].getStartIndex(), toMoveBlockAnnotations[a].size());
						movedBlockAnnotation.copyAttributes(toMoveBlockAnnotations[a]);
						movedBlockAnnotation.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, toMoveBlockAnnotations[a].getAnnotationID());
					}
					
					//	clean up
					data.removeTokens(toMoveBlock);
				}
				
				//	paragraphs adjacent, simply merge them
				MutableAnnotation mergedParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, topParagraph.getStartIndex(), (bottomParagraph.getEndIndex() - topParagraph.getStartIndex()));
				mergedParagraph.copyAttributes(bottomParagraph);
				mergedParagraph.copyAttributes(topParagraph);
				data.removeAnnotation(bottomParagraph);
				data.removeAnnotation(topParagraph);
				paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				break;
			}
		}
		
		//	re-type 'continue' paragraphs we could not attach
		for (int p = 0; p < paragraphs.length; p++) {
			if (CONTINUE_MAIN_TEXT_TYPE.equals(paragraphs[p].getAttribute(TYPE_ATTRIBUTE)))
				paragraphs[p].setAttribute(TYPE_ATTRIBUTE, MAIN_TEXT_TYPE);
			else if (CONTINUE_FOOTNOTE_TYPE.equals(paragraphs[p].getAttribute(TYPE_ATTRIBUTE)))
				paragraphs[p].setAttribute(TYPE_ATTRIBUTE, FOOTNOTE_TYPE);
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
		
		//	clean up to-move blocks
		AnnotationFilter.removeAnnotations(data, TO_MOVE_BLOCK_ANNOTATION_TYPE);
		
		//	mark page breaks
		Object lastPageId = null;
		Object lastPageNumber = null;
		HashSet seenPageIds = new HashSet();
		HashSet seenPageNumbers = new HashSet();
		for (int t = 0; t < data.size(); t++) {
			Token token = data.tokenAt(t);
			String pageId = ((String) token.getAttribute(PAGE_ID_ATTRIBUTE, lastPageId));
			String pageNumber = ((String) token.getAttribute(PAGE_NUMBER_ATTRIBUTE, lastPageNumber));
			if (((lastPageId == null) || lastPageId.equals(pageId)) && ((lastPageNumber == null) || lastPageNumber.equals(pageNumber)))
				continue;
			Annotation pageBreak = data.addAnnotation(PAGE_BREAK_TOKEN_TYPE, t, 1);
			pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			lastPageId = pageId;
			pageBreak.setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			lastPageNumber = pageNumber;
			if (seenPageIds.add(pageId) | seenPageNumbers.add(pageNumber))
				pageBreak.setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
		}
		AnnotationFilter.removeDuplicates(data, PAGE_BREAK_TOKEN_TYPE);
		
		//	clean up token attributes
		for (int t = 0; t < data.size(); t++) {
			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
	}
	
	private String getParagraphType(MutableAnnotation paragraph) {
		Annotation[] indicators;
		indicators = paragraph.getAnnotations(FOOTNOTE_TYPE);
		if (indicators.length != 0) {
//			System.out.println("Footnote indicator");
			return (this.isContinued(paragraph) ? CONTINUE_FOOTNOTE_TYPE : FOOTNOTE_TYPE);
		}
		indicators = paragraph.getAnnotations(CAPTION_TYPE);
		if (indicators.length != 0) {
//			System.out.println("Caption indicator");
			return CAPTION_TYPE;
		}
		indicators = paragraph.getAnnotations(PAGE_TITLE_TYPE);
		if (indicators.length != 0) {
//			System.out.println("Page header indicator");
			return PAGE_TITLE_TYPE;
		}
		indicators = paragraph.getAnnotations(PAGE_NUMBER_TYPE);
		if (indicators.length != 0) {
//			System.out.println("Page number indicator");
			return PAGE_TITLE_TYPE;
		}
		indicators = paragraph.getAnnotations(TABLE_TYPE);
		if (indicators.length != 0) {
//			System.out.println("Table indicator");
			return TABLE_TYPE;
		}
		indicators = paragraph.getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		if ((indicators.length != 0) && (indicators[0].size() > 10)) {
			for (int t = 0; t < indicators[0].size(); t++)
				if (indicators[0].valueAt(t).matches("[1-2][0-9]{3}")) {
//					System.out.println("BibRef indicator");
					return BIBLIOGRAPHIC_REFERENCE_TYPE;
				}
		}
		
		return (this.isContinued(paragraph) ? CONTINUE_MAIN_TEXT_TYPE : MAIN_TEXT_TYPE);
		
		//	TODO implement this using rule box (easier to maintain and extend)
	}
	private boolean isContinued(MutableAnnotation paragraph) {
		String firstStr = paragraph.firstValue();
		if ((firstStr == null) || (firstStr.length() == 0))
			return false;
		return Character.isLowerCase(firstStr.charAt(0));
	}
	
	private boolean canAttach(Annotation paragraph, Annotation aParagraph) {
		if (CONTINUE_MAIN_TEXT_TYPE.equals(paragraph.getAttribute(TYPE_ATTRIBUTE))) {
			return (false
					|| MAIN_TEXT_TYPE.equals(aParagraph.getAttribute(TYPE_ATTRIBUTE))
					|| CONTINUE_MAIN_TEXT_TYPE.equals(aParagraph.getAttribute(TYPE_ATTRIBUTE))
					|| BIBLIOGRAPHIC_REFERENCE_TYPE.equals(aParagraph.getAttribute(TYPE_ATTRIBUTE))
					);
		}
		else if (CONTINUE_FOOTNOTE_TYPE.equals(paragraph.getAttribute(TYPE_ATTRIBUTE))) {
			return (false
					|| FOOTNOTE_TYPE.equals(aParagraph.getAttribute(TYPE_ATTRIBUTE))
					|| CONTINUE_FOOTNOTE_TYPE.equals(aParagraph.getAttribute(TYPE_ATTRIBUTE))
					);
		}
		else return false;
	}
}