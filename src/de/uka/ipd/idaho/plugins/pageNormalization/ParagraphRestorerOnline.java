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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CheckBoxFeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class ParagraphRestorerOnline extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String TO_MOVE_BLOCK_ANNOTATION_TYPE = "toMoveBlock";
	
	private static final String MOVE_TAG_LIST_NAME = "moveTagList.txt";
	
	private StringVector toMove = new StringVector();
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			InputStream is = this.dataProvider.getInputStream(MOVE_TAG_LIST_NAME);
			this.toMove = StringVector.loadList(is);
			is.close();
		}
		catch (IOException e) {
			this.toMove.addElementIgnoreDuplicates(CAPTION_TYPE);
			this.toMove.addElementIgnoreDuplicates(FOOTNOTE_TYPE);
			this.toMove.addElementIgnoreDuplicates("table");
		}
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
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
		
		//	add page IDs and numbers to tokens
		Annotation[] pageBreakTokens = data.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
		int nextPageBreakTokenIndex = 0;
		Object pageIdObject = null;
		Object pageNumberObject = null;
		for (int t = paragraphs[0].getStartIndex(); t < data.size(); t++) {
			if ((nextPageBreakTokenIndex < pageBreakTokens.length) && (t == pageBreakTokens[nextPageBreakTokenIndex].getStartIndex())) {
				pageIdObject = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_ID_ATTRIBUTE);
				pageNumberObject = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE);
				nextPageBreakTokenIndex++;
			}
			data.tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageIdObject);
			data.tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberObject);
		}
		
//		//	TODOne find out if this is really neccessary
//		if ((pageIdObject == null) || (pageNumberObject == null)) {
//			for (int p = 0; p < paragraphs.length; p++) {
//				pageIdObject = paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE);
//				pageNumberObject = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
//				for (int t = 0; t < paragraphs[p].size(); t++) {
//					if (pageIdObject != null)
//						paragraphs[p].tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageIdObject);
//					if (pageNumberObject != null)
//						paragraphs[p].tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberObject);
//				}
//			}
//		}
		
		//	find and index paragraphs nested in to-move annotations
		HashSet toMoveForNesting = new HashSet();
		Properties paragraphMoveBlockIDs = new Properties();
		Properties paragraphMoveReasons = new Properties();
		for (int m = 0; m < this.toMove.size(); m++) {
			MutableAnnotation[] toMove = data.getMutableAnnotations(this.toMove.get(m));
			for (int t = 0; t < toMove.length; t++) {
				Annotation toMoveBlock = data.addAnnotation(TO_MOVE_BLOCK_ANNOTATION_TYPE, toMove[t].getStartIndex(), toMove[t].size());
//				System.out.println("TO MOVE BLOCK: " + toMoveBlock.toXML());
				Annotation[] toMoveParagraphs = toMove[t].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < toMoveParagraphs.length; p++) {
					if (p == 0)
						toMoveBlock.setAttribute(PAGE_NUMBER_ATTRIBUTE, toMoveParagraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
					toMoveForNesting.add(toMoveParagraphs[p].getAnnotationID());
					paragraphMoveBlockIDs.setProperty(toMoveParagraphs[p].getAnnotationID(), toMoveBlock.getAnnotationID());
					paragraphMoveReasons.setProperty(toMoveParagraphs[p].getAnnotationID(), this.toMove.get(m));
				}
			}
		}
		
		//	determine for each paragraph whether or not it might have to be moved
		boolean[] isParagraphToBridge = new boolean[paragraphs.length];
		for (int p = 0; p < paragraphs.length; p++) {
			isParagraphToBridge[p] = false;
			if (toMoveForNesting.contains(paragraphs[p].getAnnotationID())) {
				isParagraphToBridge[p] = true;
				continue;
			}
			Annotation[] paragraphAnnotations = paragraphs[p].getAnnotations();
			for (int a = 0; a < paragraphAnnotations.length; a++)
				if (this.toMove.containsIgnoreCase(paragraphAnnotations[a].getType())) {
					isParagraphToBridge[p] = true;
					Annotation toMoveBlock = data.addAnnotation(TO_MOVE_BLOCK_ANNOTATION_TYPE, paragraphs[p].getStartIndex(), paragraphs[p].size());
					toMoveBlock.setAttribute(PAGE_NUMBER_ATTRIBUTE, paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
					paragraphMoveBlockIDs.setProperty(paragraphs[p].getAnnotationID(), toMoveBlock.getAnnotationID());
					paragraphMoveReasons.setProperty(paragraphs[p].getAnnotationID(), paragraphAnnotations[a].getType());
//					System.out.println("TO MOVE BLOCK: " + toMoveBlock.toXML());
					a = paragraphAnnotations.length;
				}
		}
		
		//	clean up and get to-move blocks
		AnnotationFilter.removeDuplicates(data, TO_MOVE_BLOCK_ANNOTATION_TYPE);
		AnnotationFilter.removeInner(data, TO_MOVE_BLOCK_ANNOTATION_TYPE);
		MutableAnnotation[] toMoveBlocks = data.getMutableAnnotations(TO_MOVE_BLOCK_ANNOTATION_TYPE);
		
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
					
					//	investigate surrounding paragraphs in order to preset merger
					MutableAnnotation paragraphBefore = paragraphs[mbs-1];
					MutableAnnotation paragraphAfter = paragraphs[mbe];
					
					//	no evidence for a sentence end or start, probably need to be merged
					if (!Gamta.isSentenceEnd(paragraphBefore.lastToken()) || Gamta.isLowerCaseWord(paragraphAfter.firstToken()))
						merger.doMerge = true;
					
					//	add to-move blocks to merger
					for (int b = 0; b < toMoveBlocks.length; b++) {
						if ((paragraphBefore.getEndIndex() <= toMoveBlocks[b].getStartIndex()) && (toMoveBlocks[b].getEndIndex() <= paragraphAfter.getStartIndex()))
							merger.toMoveBlocks.add(toMoveBlocks[b]);
					}
					
					//	enqueue for getting feedback
					mergeList.add(merger);
				}
				
				//	jump to end of block (counter loop increment, though)
				p = mbe-1;
			}
			
			//	check for subsequent main text paragraphs with different page numbers (might be separated due to page break)
			else if (((p+1) < paragraphs.length) && !isParagraphToBridge[p+1]) {
				MutableAnnotation firstParagraph = paragraphs[p];
				MutableAnnotation secondParagraph = paragraphs[p+1];
				
				try {
					int firstPageNumber = Integer.parseInt((String) firstParagraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + paragraphs.length)));
					int secondPageNumber = Integer.parseInt((String) secondParagraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "-1"));
					
					//	paragraph across page border, enqueue feedback
					if (firstPageNumber != secondPageNumber) {
						ParagraphMerger merger = new ParagraphMerger(p, (p+1));
						merger.toBridge = ("page break (p. " + firstPageNumber + " / p. " + secondPageNumber + ")");
						
						//	merge paragraphs
						if (!Gamta.isSentenceEnd(firstParagraph.lastToken()) || Gamta.isLowerCaseWord(secondParagraph.firstToken()))
							merger.doMerge = true;
						
						//	enqueue for getting feedback
						mergeList.add(merger);
					}
				}
				catch (NumberFormatException nfe) {}
			}
		}
		
		
		//	put mergers in array for easier processing
		ParagraphMerger[] mergers = ((ParagraphMerger[]) mergeList.toArray(new ParagraphMerger[mergeList.size()]));
		if (mergers.length == 0) {
			AnnotationFilter.removeAnnotations(data, TO_MOVE_BLOCK_ANNOTATION_TYPE);
			return;
		}
		
		//	compute number of buckets per dialog
		int dialogCount = ((mergers.length + 9) / 10);
		int dialogSize = ((mergers.length + (dialogCount / 2)) / dialogCount);
		dialogCount = ((mergers.length + dialogSize - 1) / dialogSize);
		
		//	build dialog label
		StringBuffer dialogLabelBuilder = new StringBuffer("<HTML>Please review if the text parts above and below the interrupting <B>");
		for (int m = 0; m < this.toMove.size(); m++)
			dialogLabelBuilder.append(this.toMove.get(m) + "s" + ((this.toMove.size() < 2) ? " " : ", "));
		dialogLabelBuilder.append((this.toMove.isEmpty() ? "" : "and "));
		dialogLabelBuilder.append("page breaks</B> form a continuous (logical) paragraph and thus need to be marked as one paragraph instead of two.<BR>Check the ckeck box on the left if they form one logical paragraph, un-check it otherwise.</HTML>");
		String dialogLabel = dialogLabelBuilder.toString();
		
		//	build dialogs
		CheckBoxFeedbackPanel[] cbfps = new CheckBoxFeedbackPanel[dialogCount];
		for (int d = 0; d < cbfps.length; d++) {
			cbfps[d] = new CheckBoxFeedbackPanel("Please Check Paragraph Boundaries");
			cbfps[d].setLabel(dialogLabel);
			cbfps[d].setTrueSpacing(20);
			cbfps[d].setFalseSpacing(20);
			
			//	add feedback instances
			int dialogOffset = (d * dialogSize);
			for (int m = 0; (m < dialogSize) && ((m + dialogOffset) < mergers.length); m++)
				cbfps[d].addLine(this.buildParagraphMergerLabel(mergers[m + dialogOffset], paragraphs, paragraphMoveBlockIDs, paragraphMoveReasons), mergers[m + dialogOffset].doMerge);
			
			//	add backgroung information
			cbfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, data.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((String) paragraphs[mergers[dialogOffset].firstParagraphIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE, "")));
			cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((String) paragraphs[mergers[dialogOffset].firstParagraphIndex].getAttribute(PAGE_ID_ATTRIBUTE, "")));
			cbfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, paragraphs[mergers[dialogOffset].firstParagraphIndex].getAnnotationID());
			cbfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PARAGRAPH_TYPE);
			
			//	add target page numbers
			TreeSet pageIdDeduplicator = new TreeSet();
			StringBuffer targetPageIDs = new StringBuffer();
			TreeSet pageNumberDeduplicator = new TreeSet();
			StringBuffer targetPages = new StringBuffer();
			for (int m = 0; (m < dialogSize) && ((m + dialogOffset) < mergers.length); m++) {
				for (int p = mergers[m + dialogOffset].firstParagraphIndex; p <= mergers[m + dialogOffset].secondParagraphIndex; p++) {
					String pageNumber = ((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
					if ((pageNumber != null) && pageNumberDeduplicator.add(pageNumber))
						targetPages.append("," + pageNumber);
					String pageId = ((String) paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE));
					if ((pageId != null) && pageIdDeduplicator.add(pageId))
						targetPageIDs.append("," + pageId);
				}
			}
			if (targetPageIDs.length() != 0)
				cbfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs.substring(1));
			if (targetPages.length() != 0)
				cbfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages.substring(1));
		}
		int cutoffMerger = mergers.length;
		
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(cbfps);
			
			//	process all feedback data together
			for (int d = 0; d < cbfps.length; d++) {
				int dialogOffset = (d * dialogSize);
				for (int m = 0; m < cbfps[d].lineCount(); m++)
					mergers[m + dialogOffset].doMerge = cbfps[d].getStateAt(m);
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
				for (int m = 0; m < cbfps[d].lineCount(); m++)
					mergers[m + dialogOffset].doMerge = cbfps[d].getStateAt(m);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog on
			else {
				cutoffMerger = (d * dialogSize);
				d = cbfps.length;
			}
		}
		
		
		//	process feedback (back to front, in order not to interfer with indices)
		for (int m = (cutoffMerger-1); m >= 0; m--) {
			
			//	test if merge possible (if all feedback was given, it is)
			boolean mergePossible = (cutoffMerger == mergers.length);
			
			//	otherwise, check for possible interference with subsequent merge that lacks feedback
			if (!mergePossible) {
				
				//	subsequent merge was executed, so no interference
				if (mergers[m+1] == null)
					mergePossible = true;
				
				//	secure pargagraph break between current and subsequent merge
				else if (mergers[m].secondParagraphIndex < mergers[m+1].firstParagraphIndex)
					mergePossible = true;
				
				//	subsequent paragraphs with no ones to move in between, no need for having a secure point to insert moved parts
				else if ((mergers[m].firstParagraphIndex+1) == mergers[m].secondParagraphIndex)
					mergePossible = true;
			}
			
			//	finally, can we do the merge?
			if (mergePossible && mergers[m].doMerge) {
				
				//	get paragraphs to merge
				MutableAnnotation firstParagraph = paragraphs[mergers[m].firstParagraphIndex];
				MutableAnnotation secondParagraph = paragraphs[mergers[m].secondParagraphIndex];
				
				//	insert enclosed parts after second paragraph (if any)
//				int movedParagraphStart = secondParagraph.getEndIndex();
				int movedBlockStart = secondParagraph.getEndIndex();
				
				//	find lowest-level annotation enclosing both to-merge paragraphs
				MutableAnnotation movedDataInsertAnnotation = data;
				MutableAnnotation[] mas = data.getMutableAnnotations();
				for (int a = 0; a < mas.length; a++) {
					
					//	check if end coincing with that of second paragraph
					if (mas[a].getEndIndex() == secondParagraph.getEndIndex()) {
						
						//	check if first paragraph included
						if (AnnotationUtils.contains(mas[a], firstParagraph)) {
							
							//	check size
							if (mas[a].size() < movedDataInsertAnnotation.size())
								movedDataInsertAnnotation = mas[a];
						}
					}
				}
				
				for (int b = 0; b < mergers[m].toMoveBlocks.size(); b++) {
					MutableAnnotation toMoveBlock = ((MutableAnnotation) mergers[m].toMoveBlocks.get(b));
					
					/*
					 * insert tokens into lowest structural element containing
					 * both to-merge paragraphs (keeps captions and footnotes
					 * within sections, subSections, and subSubSections they
					 * belong to)
					 */
					if (movedDataInsertAnnotation == data)
						data.insertTokensAt(data.subSequence(toMoveBlock.getStartOffset(), data.tokenAt(toMoveBlock.getEndIndex()).getStartOffset()), movedBlockStart);
					else movedDataInsertAnnotation.addTokens(data.subSequence(toMoveBlock.getStartOffset(), data.tokenAt(toMoveBlock.getEndIndex()).getStartOffset()));
					
					//	annotate block
					MutableAnnotation movedBlock = data.addAnnotation(TO_MOVE_BLOCK_ANNOTATION_TYPE, movedBlockStart, toMoveBlock.size());
					
					//	transfer token attributes (page numbers!)
					for (int t = 0; (t < toMoveBlock.size()) && (t < movedBlock.size()); t++)
						movedBlock.tokenAt(t).copyAttributes(toMoveBlock.tokenAt(t));
					
					//	transfer attributes (including annotation ID)
					movedBlock.copyAttributes(toMoveBlock);
					movedBlock.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, toMoveBlock.getAnnotationID());
					
					//	transfer subordinate annotations
					Annotation[] blockAnnotations = toMoveBlock.getAnnotations();
					for (int a = 0; a < blockAnnotations.length; a++) {
						if (TO_MOVE_BLOCK_ANNOTATION_TYPE.equals(blockAnnotations[a].getType()))
							continue;
						Annotation movedBlockAnnotation = movedBlock.addAnnotation(blockAnnotations[a].getType(), blockAnnotations[a].getStartIndex(), blockAnnotations[a].size());
						movedBlockAnnotation.copyAttributes(blockAnnotations[a]);
						movedBlockAnnotation.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, blockAnnotations[a].getAnnotationID());
					}
					
					//	remember to move next paragraph after the one just moved
					movedBlockStart += toMoveBlock.size();
				}
				
				//	create new (merged) paragraph
				MutableAnnotation newParagraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, firstParagraph.getStartIndex(), (secondParagraph.getEndIndex() - firstParagraph.getStartIndex()));
				newParagraph.copyAttributes(secondParagraph);
				newParagraph.copyAttributes(firstParagraph);
				
				//	remove old (now merged) paragraphs
				firstParagraph.lastToken().removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
				data.removeAnnotation(firstParagraph);
				data.removeAnnotation(secondParagraph);
				
				//	store new paragraph for merges to come
				paragraphs[mergers[m].firstParagraphIndex] = newParagraph;
				
				//	remove enclose parts (if any)
				for (int p = (mergers[m].secondParagraphIndex-1); p > mergers[m].firstParagraphIndex; p--)
					data.removeTokens(paragraphs[p]);
			}
			
			//	mark merge as done (even if feedback decided against it, for checking possiblility of merges further up the document)
			if (mergePossible)
				mergers[m] = null;
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
		
		//	check if higher level structural parts end in the middle of a paragraph that was merged
		this.checkStructure(data, MutableAnnotation.SUB_SUB_SECTION_TYPE);
		this.checkStructure(data, MutableAnnotation.SUB_SECTION_TYPE);
		this.checkStructure(data, MutableAnnotation.SECTION_TYPE);
		
		//	clean up token attributes
		for (int t = 0; t < data.size(); t++) {
			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
	}
	
	private String buildParagraphMergerLabel(ParagraphMerger merger, MutableAnnotation[] paragraphs, Properties paragraphMoveBlockIDs, Properties paragraphMoveReasons) {
		
		MutableAnnotation p1 = paragraphs[merger.firstParagraphIndex];
		MutableAnnotation p2 = paragraphs[merger.secondParagraphIndex];
		
		StringBuffer label = new StringBuffer("<HTML>");
		label.append("<B>(page " + p1.getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")</B> ");
		if (p1.size() > 100)
			label.append("... ");
		label.append(TokenSequenceUtils.concatTokens(p1, Math.max(0, (p1.size() - 100)), Math.min(100, p1.size())));
		label.append("<BR><B>---------- ");
		
		String toBridge = merger.toBridge;
		if (toBridge == null) {
			StringVector toBridgeCollector = new StringVector();
			String moveBlockId = "";
			for (int p = merger.firstParagraphIndex+1; p <= merger.secondParagraphIndex; p++) {
				
				//	check if bridging page break
				try {
					int firstPageNumber = Integer.parseInt((String) paragraphs[p-1].getAttribute(PAGE_NUMBER_ATTRIBUTE, ""));
					int secondPageNumber = Integer.parseInt((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE, ""));
					if (firstPageNumber < secondPageNumber)
						toBridgeCollector.addElement("page break (p. " + firstPageNumber + " / p. " + secondPageNumber + ")");
				}
				catch (NumberFormatException nfe) {}
				
				//	check if bridging annotations
				if (p < merger.secondParagraphIndex) {
					String paragraphMoveBlockId = paragraphMoveBlockIDs.getProperty(paragraphs[p].getAnnotationID());
					if (moveBlockId.equals(paragraphMoveBlockId))
						continue;
					moveBlockId = paragraphMoveBlockId;
					toBridgeCollector.addElement(paragraphMoveReasons.getProperty(paragraphs[p].getAnnotationID()) + " (page " + paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")");
				}
			}
			toBridge = toBridgeCollector.concatStrings(" --- ");
		}
		label.append(toBridge);
		
		label.append(" ----------</B><BR>");
		label.append(TokenSequenceUtils.concatTokens(p2, 0, Math.min(100, p2.size())));
		if (p2.size() > 100)
			label.append(" ...");
		label.append(" <B>(page " + p2.getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")</B>");
		label.append("</HTML>");
		
		return label.toString();
	}
	
	private class ParagraphMerger {
		int firstParagraphIndex;
		int secondParagraphIndex;
		boolean doMerge = false;
		ArrayList toMoveBlocks = new ArrayList();
		String toBridge = null;
		ParagraphMerger(int firstParagraphIndex, int secondParagraphIndex) {
			this.firstParagraphIndex = firstParagraphIndex;
			this.secondParagraphIndex = secondParagraphIndex;
		}
	}
	
	private void checkStructure(MutableAnnotation data, String type) {
		MutableAnnotation[] typeParts = data.getMutableAnnotations(type);
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		int typePartIndex = 0;
		for (int p = 0; p < paragraphs.length; p++) {
			if (paragraphs[p].hasAttribute(LAST_PAGE_NUMBER_ATTRIBUTE)) {
				
				//	find next part that overlaps with paragraph
				while ((typePartIndex < typeParts.length) && (typeParts[typePartIndex].getEndIndex() <= paragraphs[p].getStartIndex()))
					typePartIndex++;
				
				//	part starts before paragraph, and ends before paragraph end
				if ((typePartIndex < typeParts.length) && (typeParts[typePartIndex].getStartIndex() <= paragraphs[p].getStartIndex()) && (typeParts[typePartIndex].getEndIndex() < paragraphs[p].getEndIndex())) {
					
					//	other part following directly or starting in same paragraph
					if (((typePartIndex+1) < typeParts.length) && (typeParts[typePartIndex+1].getStartIndex() >= typeParts[typePartIndex].getEndIndex()) && (typeParts[typePartIndex+1].getStartIndex() < paragraphs[p].getEndIndex())) {
						
						//	merge parts whose border is in paragraph
						MutableAnnotation newPart = data.addAnnotation(type, typeParts[typePartIndex].getStartIndex(), (typeParts[typePartIndex+1].getEndIndex() - typeParts[typePartIndex].getStartIndex()));
						
						//	set attributes
						newPart.copyAttributes(typeParts[typePartIndex]);
						newPart.copyAttributes(typeParts[typePartIndex+1]);
						
						//	remove merged parts
						data.removeAnnotation(typeParts[typePartIndex]);
						data.removeAnnotation(typeParts[typePartIndex+1]);
						
						//	refresh list
						typeParts = data.getMutableAnnotations(type);
					}
					
					//	no other part exists, simply include complete paragraph in part
					else {
						
						//	create part including whole paragraph
						MutableAnnotation newPart = data.addAnnotation(type, typeParts[typePartIndex].getStartIndex(), (paragraphs[p].getEndIndex() - typeParts[typePartIndex].getStartIndex()));
						
						//	set attributes
						newPart.copyAttributes(typeParts[typePartIndex]);
						
						//	remove merged parts
						data.removeAnnotation(typeParts[typePartIndex]);
						
						//	refresh list
						typeParts = data.getMutableAnnotations(type);
					}
				}
			}
		}
	}
//	
//	private String getJointForm(Token token1, Token token2, StringVector tokens) {
//		if (token1 == null) return null;
//		if (token1.length() < 2) return null;
//		
//		//	no hyphen at end of line
//		String value1 = token1.getValue();
//		if (!value1.endsWith("-")) return null;
//		
//		//	no word to continue with, or part of enumeration
//		String value2 = token2.getValue();
//		if (!Gamta.isWord(value2) || "and".equalsIgnoreCase(value2) || "or".equalsIgnoreCase(value2)) return null;
//		
//		//	prepare for lookup
//		String nValue2 = value2.toLowerCase();
//		if (nValue2.endsWith("'s"))
//			nValue2 = nValue2.substring(0, (nValue2.length() - 2));
//		else if (nValue2.endsWith("ies"))
//			nValue2 = nValue2.substring(0, (nValue2.length() - 3)) + "y";
//		else if (nValue2.endsWith("s"))
//			nValue2 = nValue2.substring(0, (nValue2.length() - 1));
//		
//		//	joint value appears with hyphen elsewhere in text ==> keep hyphen
//		if (tokens.containsIgnoreCase(value1 + value2)) return (value1 + value2);
//		else if (tokens.containsIgnoreCase(value1 + nValue2)) return (value1 + value2);
//		
//		//	joint value appears elsewhere in text ==> join
//		if (tokens.containsIgnoreCase(value1.substring(0, (value1.length() - 1)) + value2)) return (value1.substring(0, (value1.length() - 1)) + value2);
//		else if (tokens.containsIgnoreCase(value1.substring(0, (value1.length() - 1)) + nValue2)) return (value1.substring(0, (value1.length() - 1)) + value2);
//		
//		//	if lower case letter before hyhen (probably nothing like 'B-carotin') ==> join
//		if (Gamta.LOWER_CASE_LETTERS.indexOf(value1.charAt(value1.length() - 2)) != -1) return (value1.substring(0, (value1.length() - 1)) + value2);
//		
//		//	no indication, be conservative
//		return null;
//	}
}