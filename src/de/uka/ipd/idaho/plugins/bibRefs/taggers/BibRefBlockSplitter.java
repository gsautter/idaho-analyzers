/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.bibRefs.taggers;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.StartContinueFeedbackPanel;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class BibRefBlockSplitter extends AbstractConfigurableAnalyzer implements BibRefConstants {
	/*
consider auto-applying this splitter to paragraphs with token sequences looking like person ames spread all over them 
	 */
	
	private static final char MDOT = '\u0095';
	private static final char BULLET = '\u2022';
	private static final char NDASH = '\u0096';
	private static final char NDASH_ALT = '\u2013';
	private static final char MDASH = '\u0097';
	private static final char MDASH_ALT = '\u2014';
	
	private static final String SEPARATORS = (MDASH + "" + MDASH_ALT + "" + NDASH + "" + NDASH_ALT + "-" + MDOT + "" + BULLET);
	
	private static final String CONTINUE_CITATION_CATEGORY = "Continue Citation";
	private static final String NEW_CITATION_COPY_AUTHOR_CATEGORY = "New Citation, Copy Author(s)";
	private static final Color NEW_CITATION_COPY_AUTHOR_COLOR = Color.YELLOW;
	private static final String NEW_CITATION_NEW_AUTHOR_CATEGORY = "New Citation, New Author(s)";
	private static final Color NEW_CITATION_NEW_AUTHOR_COLOR = Color.PINK;
	
	private static final String SPLIT_ATTRIBUTE = "split";
	private static final String NEW_AUTHOR_ATTRIBUTE = "na";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		String nccaColor = this.getParameter("newCitationCopyAuthorColor");
		if (nccaColor != null)
			this.highlightAttributeCache.put(NEW_CITATION_COPY_AUTHOR_CATEGORY, FeedbackPanel.getColor(nccaColor));
		String ncnaColor = this.getParameter("newCitationNewAuthorColor");
		if (ncnaColor != null)
			this.highlightAttributeCache.put(NEW_CITATION_NEW_AUTHOR_CATEGORY, FeedbackPanel.getColor(ncnaColor));
	}
	
	private HashMap highlightAttributeCache = new HashMap();
	private Color getAnnotationHighlight(String type) {
		Color color = ((Color) this.highlightAttributeCache.get(type));
		if (color == null) {
			if (NEW_CITATION_COPY_AUTHOR_CATEGORY.equals(type))
				color = NEW_CITATION_COPY_AUTHOR_COLOR;
			else if (NEW_CITATION_NEW_AUTHOR_CATEGORY.equals(type))
				color = NEW_CITATION_NEW_AUTHOR_COLOR;
			else color = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			this.highlightAttributeCache.put(type, color);
		}
		return color;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(PARAGRAPH_TYPE);
		if ((paragraphs.length != 1) || (paragraphs[0].size() < data.size())) {
			System.out.println("Can only split single paragraphs.");
			return;
		}
		MutableAnnotation paragraph = paragraphs[0];
		if (paragraph.size() < 3) {
			System.out.println("Can only split paragraphs of 3 or more tokens.");
			return;
		}
		
		//	attribute tokens with page IDs and page numbers
		Annotation[] pageBreakTokens = paragraph.getAnnotations(PAGE_BREAK_TOKEN_TYPE);
		int nextPageBreakTokenIndex = 0;
		Object pageId = paragraph.getAttribute(PAGE_ID_ATTRIBUTE);				
		Object pageNumber = paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE);
		for (int t = paragraph.getStartIndex(); t < paragraph.size(); t++) {
			if ((nextPageBreakTokenIndex < pageBreakTokens.length) && (t == pageBreakTokens[nextPageBreakTokenIndex].getStartIndex())) {
				pageId = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_ID_ATTRIBUTE);
				pageNumber = pageBreakTokens[nextPageBreakTokenIndex].getAttribute(PAGE_NUMBER_ATTRIBUTE);
				nextPageBreakTokenIndex++;
			}
			paragraph.tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			paragraph.tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
		}
		
		
		//	find possible separators
		StringVector separators = new StringVector();
		StringIndex separatorFrequencies = new StringIndex();
		TreeMap separatorIndices = new TreeMap();
		String lastValue = null;
		String value = paragraph.valueAt(0);
		String nextValue = paragraph.valueAt(1);
		for (int v = 1; v < (paragraph.size()-1); v++) {
			lastValue = value;
			value = nextValue;
			nextValue = paragraph.valueAt(v+1);
//			System.out.println("Checking " + value);
			
			//	not a possible separator
			if (SEPARATORS.indexOf(value) == -1) {
//				System.out.println(" ==> Not a split char");
				continue;
			}
			
			//	beware of splitting page ranges
			if (Gamta.isNumber(lastValue) && Gamta.isNumber(nextValue)) try {
				int lv = Integer.parseInt(lastValue);
				int nv = Integer.parseInt(nextValue);
				if (lv < nv) {
//					System.out.println(" ==> No split between page numbers");
					continue;
				}
			}
			catch (NumberFormatException nfe) {}
			
			//	remember possible separator
			separators.addElementIgnoreDuplicates(value);
			separatorFrequencies.add(value);
			separatorIndices.put(new Integer(v), value);
			System.out.println(" ==> Got possible split at " + v + " between '" + lastValue  + "' and '" + nextValue + "'.");
		}
		
		//	found anything?
		if (separators.isEmpty()) {
			System.out.println("Cannot split paragraph without separators.");
			return;
		}
		
		//	get most frequent separator
		String separator = null;
		int separatorFrequency = 0;
		for (int s = 0; s < separators.size(); s++) {
			String sep = separators.get(s);
			int sepFreq = separatorFrequencies.getCount(sep);
			System.out.println("Got separator '" + sep + "' " + sepFreq + " times");
			if (sepFreq > separatorFrequency) {
				separator = sep;
				separatorFrequency = sepFreq;
			}
		}
		System.out.println("Got separator: '" + separator + "'");
		
		//	mark parts
		int lastStart = 0;
		for (Iterator sit = separatorIndices.keySet().iterator(); sit.hasNext();) {
			Integer start = ((Integer) sit.next());
			if (!separator.equals(separatorIndices.get(start)))
				continue;
			if ((start.intValue() - lastStart) < 4)
				continue;
			
			//	annotate citation
			Annotation citation = paragraph.addAnnotation(CITATION_TYPE, lastStart, (start.intValue() - lastStart));
			System.out.println("Got citation: " + TokenSequenceUtils.concatTokens(citation, false, true));
			
			//	remember how far we got
			lastStart = start.intValue();
		}
		if (lastStart < paragraph.size()) {
			Annotation citation = paragraph.addAnnotation(CITATION_TYPE, lastStart, (paragraph.size() - lastStart));
			System.out.println("Got citation: " + TokenSequenceUtils.concatTokens(citation, false, true));
			lastStart = paragraph.size();
		}
		
		
		//	collect tokens from ends of citations (as additional splitters)
//		MutableAnnotation[] citations = paragraph.getMutableAnnotations(CITATION_TYPE);
		MutableAnnotation[] citations = this.getCitations(paragraph);
		StringVector citationEnds = new StringVector();
		StringIndex citationEndFrequencies = new StringIndex();
//		System.out.println("Got " + citations.length + " citations after separator split:");
		for (int c = 0; c < citations.length; c++) {
//			System.out.println(" - (" + c + "): " + TokenSequenceUtils.concatTokens(citations[c], false, true));
			for (int e = 1; e <= 3; e++) {
				String end = TokenSequenceUtils.concatTokens(citations[c], (citations[c].size()-e), e, true, true);
				if (".".equals(end) || ",".equals(end)) // ignore single periods and commas, they are just too frequent
					continue;
				citationEnds.addElementIgnoreDuplicates(end);
				citationEndFrequencies.add(end);
			}
		}
		
		//	split citations at internal frequent endings
		for (int c = 0; c < citations.length; c++) {
			
			//	get and sort splitting points
			Annotation[] splits = Gamta.extractAllContained(citations[c], citationEnds);
			Arrays.sort(splits, AnnotationUtils.getComparator(""));
			
			//	check splitting points one by one
			for (int s = 0; s < splits.length; s++) {
				
				//	no use splitting at end
				if (splits[s].getEndIndex() == citations[c].size())
					continue;
				
				//	check split confidence TODOne (it seems to) figure out if formula makes sense
				if ((splits[s].size() * citationEndFrequencies.getCount(TokenSequenceUtils.concatTokens(splits[s], true, true))) < citations.length)
					continue;
				
				//	do split
				System.out.println("Splitting citation: " + TokenSequenceUtils.concatTokens(citations[c], false, true));
				Annotation part1 = citations[c].addAnnotation(CITATION_TYPE, 0, splits[s].getEndIndex());
				part1.copyAttributes(citations[c]);
				System.out.println(" - " + TokenSequenceUtils.concatTokens(part1, false, true));
				Annotation part2 = citations[c].addAnnotation(CITATION_TYPE, splits[s].getEndIndex(), (citations[c].size() - splits[s].getEndIndex()));
				part2.setAttribute(SPLIT_ATTRIBUTE, SPLIT_ATTRIBUTE);
				System.out.println(" - " + TokenSequenceUtils.concatTokens(part2, false, true));
				paragraph.removeAnnotation(citations[c]);
				
				//	update ending frequencies
				for (int e = 1; e <= 3; e++) {
					String end = TokenSequenceUtils.concatTokens(part1, (part1.size()-e), e, true, true);
					if (".".equals(end) || ",".equals(end)) // ignore single periods and commas, they are just too frequent
						continue;
					citationEnds.addElementIgnoreDuplicates(end);
					citationEndFrequencies.add(end);
				}
				
				//	refresh citations and start over
//				citations = paragraph.getMutableAnnotations(CITATION_TYPE);
				citations = this.getCitations(paragraph);
				s = splits.length;
			}
		}
		
		//	sort out split citations
		ArrayList feedbackCitationList = new ArrayList();
		for (int c = 0; c < citations.length; c++) {
			Annotation[] innerCitations = citations[c].getAnnotations(CITATION_TYPE);
			if ((innerCitations.length == 0) || ((innerCitations.length == 1) && (innerCitations[0].size() == citations[c].size())))
				feedbackCitationList.add(citations[c]);
		}
		if (feedbackCitationList.size() < citations.length)
			citations = ((MutableAnnotation[]) feedbackCitationList.toArray(new MutableAnnotation[feedbackCitationList.size()]));
		
		//	check whether or not citations specify author(s)
		for (int c = 0; c < citations.length; c++)
			if ((c == 0) || this.specifiesNewAuthors(citations[c], separator)) {
				citations[c].setAttribute(NEW_AUTHOR_ATTRIBUTE, NEW_AUTHOR_ATTRIBUTE);
//				System.out.println("Got new authors in " + citations[c].toXML());
			}
		
		//	set page ID and page number (required for feedback)
		for (int c = 0; c < citations.length; c++) {
			pageId = citations[c].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
			citations[c].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			pageNumber = citations[c].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			citations[c].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			
			Object lastPageId = citations[c].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if ((lastPageId != null) && !lastPageId.equals(pageId))
				citations[c].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageId);
			Object lastPageNumber = citations[c].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if ((lastPageNumber != null) && !lastPageNumber.equals(pageNumber))
				citations[c].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumber);
		}
		
		
		//	prepare getting feedback
		StartContinueFeedbackPanel scfp = new StartContinueFeedbackPanel("Check Citation Block Splitting");
		scfp.setLabel("<HTML>Please check and correct the splitting of this citation block paragraph into individual citations." +
				"<BR>Use the drop-downs to the left of each part to do so. The individual options indicate the following:" +
				"<BR>- <B>" + NEW_CITATION_NEW_AUTHOR_CATEGORY + "</B>: the part starts a new citation, which explicitly specifies its authors" +
				"<BR>- <B>" + NEW_CITATION_COPY_AUTHOR_CATEGORY + "</B>: the part starts a new citation, which inherits its authors from a citation above it" +
				"<BR>- <B>" + CONTINUE_CITATION_CATEGORY + "</B>: the part does not start a new citation, but belongs to the part preceeding it" +
				"</HTML>");
		scfp.setContinueCategory(CONTINUE_CITATION_CATEGORY);
		scfp.addCategory(NEW_CITATION_COPY_AUTHOR_CATEGORY);
		scfp.setCategoryColor(NEW_CITATION_COPY_AUTHOR_CATEGORY, this.getAnnotationHighlight(NEW_CITATION_COPY_AUTHOR_CATEGORY));
		scfp.addCategory(NEW_CITATION_NEW_AUTHOR_CATEGORY);
		scfp.setCategoryColor(NEW_CITATION_NEW_AUTHOR_CATEGORY, this.getAnnotationHighlight(NEW_CITATION_NEW_AUTHOR_CATEGORY));
		scfp.setChangeSpacing(10);
		scfp.setContinueSpacing(0);
		scfp.addButton("OK");
		scfp.addButton("Cancel");
		
		//	add background information
		scfp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, data.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
		scfp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
		scfp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, paragraph.getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
		scfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, paragraph.getAnnotationID());
		scfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, CITATION_TYPE);
		
		//	add target page number
		String targetPageNumbers = FeedbackPanel.getTargetPageString(citations);
		if (targetPageNumbers != null)
			scfp.setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPageNumbers);
		String targetPageIDs = FeedbackPanel.getTargetPageIdString(citations);
		if (targetPageIDs != null)
			scfp.setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		
		//	add actual data
//		citations = paragraph.getMutableAnnotations(CITATION_TYPE);
		for (int c = 0; c < citations.length; c++) {
			
			//	compute category
			String category = NEW_CITATION_COPY_AUTHOR_CATEGORY;
			
			//	end-based split, re-merge by default
			if (citations[c].hasAttribute(SPLIT_ATTRIBUTE))
				category = CONTINUE_CITATION_CATEGORY;
			
			//	got author(s), this one's straightforward
			else if ((c == 0) || citations[c].hasAttribute(NEW_AUTHOR_ATTRIBUTE))
				category = NEW_CITATION_NEW_AUTHOR_CATEGORY;
			
			//	check whether previous citation has meaningful ending
			else for (int e = 1; e <= 3; e++) {
				String end = TokenSequenceUtils.concatTokens(citations[c-1], (citations[c-1].size()-e), e, true, true);
				
				//	skip over periods and commas
				if (".".equals(end) || ",".equals(end)) // ignore single periods and commas, they are just too frequent
					continue;
				
				//	this one's somewhat familiar, keep split
				if (citationEndFrequencies.getCount(end) <= (4-e)) {
					category = CONTINUE_CITATION_CATEGORY;
					System.out.println("Rare-end continue for '" + end + "', frequency is " + citationEndFrequencies.getCount(end) + " in " + citations.length + " citations");
				}
				
				//	we're done either way
				e = 4;
			}
			
			//	put it into feedback panel
			scfp.addLine(TokenSequenceUtils.concatTokens(citations[c], false, true), category);
			
		}
		
		//	get feedback
		String feedback = scfp.getFeedback();
		
		//	dialog cancelled, clean up and exit
		if ("Cancel".equals(feedback)) {
			for (int c = 0; c < citations.length; c++)
				paragraph.removeAnnotation(citations[c]);
			return;
		}
		
		//	process feedback
		for (int c = (citations.length - 1); c >= 0; c--) {
			String category = scfp.getCategoryAt(c);
			
			//	copy author(s) ==> keep (or even insert) separator as author repetition sign
			if (NEW_CITATION_COPY_AUTHOR_CATEGORY.equals(category)) {
				if ((c > 0) && !separator.equals(citations[c].firstValue())) {
					citations[c].insertTokensAt(((Gamta.insertSpace(paragraph.valueAt(citations[c].getStartIndex()-1), separator) ? " " : "") + separator + (Gamta.insertSpace(separator, citations[c].firstValue()) ? " " : "")), 0);
					citations[c].firstToken().copyAttributes(citations[c].tokenAt(1));
				}
				continue;
			}
			
			//	author(s) specified explicitly ==> cut separator
			if (NEW_CITATION_NEW_AUTHOR_CATEGORY.equals(category)) {
				
				//	do NOT cut first citation
				if (!separator.equals(citations[c].firstValue()))
					continue;
				
				//	do cut subsequent ones
				MutableAnnotation citation = paragraph.addAnnotation(CITATION_TYPE, (citations[c].getStartIndex()+1), (citations[c].size()-1));
				paragraph.removeAnnotation(citations[c]);
				citations[c] = citation;
				continue;
			}
			
			//	!!!category has to be 'continue' from this point!!!
			
			//	first category is 'continue' ==> remove annotation alltogether
			if (c == 0) {
				paragraph.removeAnnotation(citations[c]);
				continue;
			}
			
			//	merge continue annotations
			MutableAnnotation citation = paragraph.addAnnotation(CITATION_TYPE, citations[c-1].getStartIndex(), (citations[c].getEndIndex() - citations[c-1].getStartIndex()));
			paragraph.removeAnnotation(citations[c-1]);
			paragraph.removeAnnotation(citations[c]);
			citations[c-1] = citation;
			citations[c] = null;
		}
		
		//	remove control attribute, andset page ID and page number
		for (int c = 0; c < citations.length; c++) {
			if (citations[c] == null)
				continue;
			
			citations[c].removeAttribute(NEW_AUTHOR_ATTRIBUTE);
			citations[c].removeAttribute(SPLIT_ATTRIBUTE);
			
			pageId = citations[c].firstToken().getAttribute(PAGE_ID_ATTRIBUTE);
			citations[c].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			pageNumber = citations[c].firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			citations[c].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			
			Object lastPageId = citations[c].lastToken().getAttribute(PAGE_ID_ATTRIBUTE);
			if ((lastPageId != null) && !lastPageId.equals(pageId))
				citations[c].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lastPageId);
			Object lastPageNumber = citations[c].lastToken().getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if ((lastPageNumber != null) && !lastPageNumber.equals(pageNumber))
				citations[c].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lastPageNumber);
		}
		
		//	clean up
		AnnotationFilter.removeOuter(paragraph, CITATION_TYPE);
		for (int t = paragraph.getStartIndex(); t < paragraph.size(); t++) {
			paragraph.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			paragraph.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
	}
	
	private MutableAnnotation[] getCitations(MutableAnnotation paragraph) {
		MutableAnnotation[] citations = paragraph.getMutableAnnotations(CITATION_TYPE);
		if (citations.length < 2)
			return citations;
		ArrayList citationList = new ArrayList();
		for (int c = 0; c < citations.length; c++) {
			if (((c+1) == citations.length) || !AnnotationUtils.overlaps(citations[c], citations[c+1]))
				citationList.add(citations[c]);
		}
		if (citationList.size() < citations.length)
			citations = ((MutableAnnotation[]) citationList.toArray(new MutableAnnotation[citationList.size()]));
		return citations;
	}
	
	private static final String authorLastNameBaseRegEx = 
			"(" +
			"([A-Za-z]+\\'?)?" +
			"[A-Z]([a-z]+|[A-Z]+)" +
			")";
	private static final String authorLastNameRegEx = "(" + authorLastNameBaseRegEx + ")((\\-|\\s)?" + authorLastNameBaseRegEx + ")*";
	
	private static final String authorFirstNameBaseRegEx = 
			"[A-Z][a-z]{2,}";
	private static final String authorFirstNameRegEx = "(" + authorFirstNameBaseRegEx + ")((\\-|\\s)?" + authorFirstNameBaseRegEx + ")*";
	
	private static final String authorInitialsBaseRegEx = 
		"[A-Z]" +
		"(" +
			"([a-z]?\\.\\s?)" +
		")?";
	private static final String authorInitialsRegEx = "(" + authorInitialsBaseRegEx + ")((\\-|\\s)?" + authorInitialsBaseRegEx + ")*";
	
	private static final String authorMixedFirstNameRegEx = "(" + authorInitialsRegEx + "\\s)*(" + authorFirstNameRegEx + ")*(\\s" + authorInitialsRegEx + ")*";
	
	private static final String authorLastNameFirstRegEx = "(" + authorLastNameRegEx + "\\,\\s" + authorMixedFirstNameRegEx + ")";
	
	private boolean specifiesNewAuthors(Annotation citation, String separator) {
		int actualCitationStart = (separator.equals(citation.firstValue()) ? 1 : 0);
//		System.out.println(citation.toXML() + " starts at " + actualCitationStart);
		
		//	get possible reference number
		String number = citation.valueAt(actualCitationStart);
		if (Gamta.isOpeningBracket(number) && (citation.size() > (actualCitationStart+1)))
			number = citation.valueAt(actualCitationStart+1);
		while (number.startsWith("(") || number.startsWith("[") || number.startsWith("<"))
			number = number.substring(1);
		while (number.endsWith(".") || number.endsWith(",") || number.endsWith(":") || number.endsWith(";") || number.endsWith(")") || number.endsWith("]") || number.endsWith(">"))
			number = number.substring(0, (number.length() - 1));
		
//		//	got reference number (not year!) or letter, probably author(s) specified explicitly
//		if (number.matches("([0-9]{1,3}|[A-Za-z])"))
//		DO NOT GO FOR LETTER NUMBERING, CAUSES TROUBLE WITH INTERNAL ABBREVIATIONS
		
		//	got reference number (not year!) or letter, probably author(s) specified explicitly
		if (number.matches("[0-9]{1,3}"))
			return true;
		
		//	check for initial author names (last name first only)
		String citationString = TokenSequenceUtils.concatTokens(citation, actualCitationStart, (citation.size()-actualCitationStart), true, true);
//		System.out.println(citation.toXML() + " as match string is " + citationString);
		return citationString.matches(authorLastNameFirstRegEx + ".*");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		if (true) {
//			System.out.println("Willmann".matches(authorLastNameRegEx + ".*"));
//			System.out.println("C".matches(authorMixedFirstNameRegEx + ".*"));
//			System.out.println("Willmann, C:".matches(authorLastNameFirstRegEx + ".*"));
//			System.out.println("Tuxen:".matches(authorLastNameRegEx + ".*"));
//			System.out.println("S. L.".matches(authorMixedFirstNameRegEx + ".*"));
//			System.out.println("Tuxen, S. L.:".matches(authorLastNameFirstRegEx + ".*"));
//			System.out.println("H., Teil 2), 324 — 351 (1929 a).".matches(authorLastNameFirstRegEx + ".*"));
//			return;
//		}
//		
//		System.out.println("Separators: " + SEPARATORS);
		
//		String docString = "<paragraph pageId=\"1\" pageNumber=\"15\">" +
//				"1. Brehm, V. und Ruttner, F.: Die Biocönosen der Lunzer Gewässer. Internat. Rev. d. ges. Hydrobiol. u. Hydrogr. 16, H. 5/6, 281—391. 1926. " +
//				"— 2. Dampf, A.: Zur Kenntnis der estländischen Moorfauna. II. Beitrag, 12. Oribatiden. (Bearbeitet von Dr. Max Sellnick.) Sitzungsber. d. naturforsch. Ges. b. d. Univ. Dorpat 31, H. 1/2. 65—71. 1924. " +
//				"— 3. Schweizer, Jos.: Beitrag zur Kenntnis der terrestrischen Milbenfauna der Schweiz. Verhandl. d. naturforsch. Ges. Basel 33, 23—112. 1921/22. " +
//				"— 4. Ders.: Beitrag zur Kenntnis der Tierwelt norddeutscher Quellgebiete. Acarina (Landmilben). Arch. f. Hydrobiol. 15, 125—132. 1924. " +
//				"— 5. Ders.: Landmilben aus Salzquellen bzw. Salzwiesen von Oldesloe (Holstein). Mitt. d. geogr. Ges. u. d. nat. Mus. Lübeck 2. R., H. 31, 27—33. 1926. " +
//				"— 6. Sellnick, M.: Oribatiden vom Zwergbirkenmoor bei Neulinum, Kr. Kulm, und vom Moor am kleinen Heidsee bei Heubude unweit Danzig. Schrift. d. naturforsch. Ges. Danzig 15, H. 3, 69—77. 1921. (Mitt. d. Westpr. Provinzialmuseums. II). " +
//				"— 7. Trägardh, I.: Beiträge zur Fauna der Bäreninsel. 5. Acariden. Köngl. Svensk. Akad. Vet. Handl. IV, 26, 1—24. 1901. " +
//				"— 8. Ders.: Acariden aus dem Sarekgebirge. Naturw. Untersuch. des Sarekgebirges in Schwedisch-Lappland. IV. Zool. 1910. " +
//				"— 9. Willimann, C: Oribatiden aus Quellmoosen. Arch. f. Hydrobiol 14, 470—477. 1924." +
//				"</paragraph>";
//		
//		docString = "<paragraph pageId=\"1\" pageNumber=\"15\">" +
//				"Balogh, J.: Grundzüge der Zoozoenologie. Budapest, Separatum, S. 145—248. 1953a. " +
//				"— Caeoli, E., e R. Maffia: Due specie nuove ed una poco nota di Oribatei (Acari) dellaVenezia Tridentina. Ann. Mus. zool. Univ.Napoli 6 (12), 1—12 (1934a). " +
//				"— Cooreman, J.: Notes sur la Faune des Hautes-Fagnes en Belgique. VI (2e partie) Oribatei (Acariens). Bull. Mus. Hist. natur. Belg. 17 (73), 1—12 (1941a). " +
//				"— Acariens, in W. Conrad Sur la faune et la flore d'un ruisseau de l'Ardenne Belge. Mem. Mus. roy. Hist. natur. Belg. 99, 124—127 (1942a). " +
//				"— Evans, G. O.: British mites of the genus Brachychthonius Berl. 1910. Ann. Mag. natur. Hist., Ser. XII, 5 (51), 227—239 (1952a). " +
//				"— Forsslund, K. H.: Schwedische Oribatei (Acari). I. Ark. Zool., (Stockh.) 34 (A, Nr 10), 1—11 (1942a). " +
//				"— Studien ueber die Tierwelt des nordschwedischen Waldbodens. Medd. Stat. Skogsfoersoeksanst. 34 (5), 341—364 (1943 a). " +
//				"— Fbanz, H.: Über die Bedeutung terricoler Kleintiere fuer den Stickstoff— und Humushaushalt des Bodens. Z. Pflanzenernahrg. Dueng. u. Bodenkde. 55, 44—52 (1951 a). " +
//				"— Der Einfluss verschiedener Duengungsmassnahmen auf die Bodenfauna. Angew. Pflanzensoziol. (Wien) 11, 1—50 (1953a). " +
//				"— Grandjean, F.: Observations sur les Oribates. 2. ser. Bull. Mus. Hist. natur. Paris, Ser. II 3 (7), 651—665 (1931a). " +
//				"— Observations sur les Oribates. 3. ser. Bull. Mus. Hist. natur. Paris, Ser. II 4 (3), 292—306 (1932a). " +
//				"— Les Oribates de Jean Frederic Hermann et de son pere. Ann. Soc. Ent. France 105, 27—110 (1936a). " +
//				"— Sur les Hydrozetes (Acariens) de l'Europe occidentale. Bull. Mus. Hist, natur. Paris, Ser. II 20 (4), 328—335 (1948a). " +
//				"— Sur le genre Hydrozetes Berl. Bull. Mus. Hist. natur. Paris, Ser. II 21 (2), 224—231 (1949a). " +
//				"— Comparaison du genre Limnozetes au genre Hydrozetes (Oribates). Bull. Mus. Hist. natur. Paris, Ser. II 23 (2), 200—207 (1951a). " +
//				"— Observations sur les Oribates. 26. ser. Bull. Mus. Hist. natur. Paris, Ser. II 25 (3), 286—293 (1953a). " +
//				"— Observations sur les Oribates. 25. ser. Bull. Mus. Hist. natur. Paris, Ser. II 25 (2), 155—162 (1953 b). " +
//				"— Hammen, L. v. d.: The Oribatei (Acari) of the Netherlands. Zool. Verh. Leiden 17, 1—139 (1952a). " +
//				"— Hammer, M.: The Oribatid and Collembola fauna in some soil samples from Sondre Stromfjord. Entomol. Medd. 26, 404—414 (1952a). " +
//				"— Jacot, A. P.: Les Phthiracaridae de Karl Ludwig Koch. Rev. Suisse Zool. 43, 161—187 (1936a). " +
//				"— Knuelle, W.: Neue Arten der Oribatiden-Gattung Pelops (Acari). Zool. Anz. 153, 215—221 (1954a). " +
//				"— Die Arten der Gattung Tectocepheus Berlese (Acarina: Oribatei). Zool. Anz. 152, 280—305 (1954b). " +
//				"— Differentialdiagnostische Kennzeichnung der Oribatei (Acari). VI. Congr. Internat. de la Science du Sol, Paris 1956, III, 4, 19—21 (1956a). " +
//				"— Morphologische und entwicklungsgeschichtliche Untersuchungen zum Phylogenetischen System der Acari: Acariformes Zachv. I. Oribatei, Malaconothridae. Mitt. zool. Mus. Berlin 33(1), 97—213 (1957a). " +
//				"— Vorkommen und Indikationswert von Oribatiden (Acari: Oribatei) in postglazialen Ablagerungen. Zool. Anz. (1957b). " +
//				"— Kubiena, W. L.: Bestimmungsbuch und Systematik der Boeden Europas. Stuttgart 1956 a. 392 S. " +
//				"— Paoli, G.: Monografia del genere Dameosama Berl. e generi affini. Redia 5, 31—91 (1908a). " +
//				"— Pschorn-Walcher, H.: Zur Biologie und Systematik terricoler Milben. I. Die ostalpinen Arten der Gattung Liacarus Mich. (Oribatei). Bonn. zool. Beitr. 2, 177—183 (1951 a). " +
//				"— Rabeler, W.: Die Fauna des Goeldenitzer Hochmoores in Mecklenburg. Z. Morph. u. Ökol. Tiere 21, 173—315 (1931a). " +
//				"— Schuster, R.: Der Anteil der Oribatiden an den Zersetzungsvorgaengen im Boden. Z. Morph. u. Ökol. Tiere 45, 1—35 (1956a). " +
//				"— Schweizer, J.: Landmilben aus der Umgebung des schweizerischen Nationalparks. Erg. wiss. Unters. Schweiz. Nationalparks, N. F. 2 (20), 1—28 (1948a). " +
//				"— Sellnick, M.: Oribatiden vom Zwergbirkenmoor bei Neulinum, Kr. Kulm, und vom Moor am kleinen Heidsee bei Heubude unweit Danzig. Schr. Ges. Naturforsch. Danzig, N. F. 15, 69—77 (1921a). " +
//				"— Die mir bekannten Arten <pageBreakToken pageId=\"2\" pageNumber=\"16\">der</pageBreakToken> Gattung Tritia Beblese. Acari 3 (1923a). " +
//				"— Oribatiden. In A. Dampf, Zur Kenntnis der estlaendischen Moorfauna. Sitzgsber. naturforsch. Ges. Dorpat 31, 65—71 (1924a). " +
//				"— Die Oribatiden (Hornmilben) des Zehlaubruches. Schr. Phys.-oekonom. Ges. Koenigsberg 66 (2) (Zehlau—H., Teil 2), 324—351 (1929a). " +
//				"— Zoologische Forschungsreise nach den Jonischen Inseln und dem Peleponnes. XVI. Acari. Sitzgsber. Akad. Wiss. Wien, Math.-naturwiss. Kl., Ser. I 140, 693—776 (1931a). " +
//				"— Sellnick, M., u. K. H. Forsslund: Die Gattung Carabodes C. L. Koch 1836 in der schwedischen Bodenfauna (Acar. Oribat.). Ark. Zool. (Stockh.), Ser. II 4 (22), 367—390 (1953a). — Die Camisiidae Schwedens (Acar. Oribat.). Ark. Zool. (Stockh.) Ser. II 8 (4), 473—530 (1955a). " +
//				"— Strenzke, K.: Beitraege zur Systematik landlebender Milben. I. u. II. Arch. f. Hydrobiol. 40, 57—70 (1943a). " +
//				"— Die biozoenotischen Grundlagen der Bodenzoologie. Z. Pflanzenernaehrg. Dueng. u. Bodenkde. 45, 245—262 (1949a). " +
//				"— Oribatella artica litoralis nov. subsp., eine neue Oribatide der Nord— und Ostseekueste (Acarina: Oribatei). Kiel. Meeresforsch. 7 (2), 157—160 (1950 a). " +
//				"— Die norddeutschen Arten der Gattung Brachychthonius und Brachychochthonius (Acarina: Oribatei). Dtsch. zool. Z. 1, 233—249 (1951a). " +
//				"— Some new Central European Moss-mites (Acarina: Oribatei). Ann. Mag. Natur. Hist. London, Ser. XII, 4, 719—726 (1951b). " +
//				"— Die norddeutschen Arten der Oribatiden-Gattung Suctobelba. Zool. Anz. 147, 147—166 (1951c). " +
//				"— Grundfragen der Autoekologie. Acta biotheoretica 9, 163—184 (1951d). " +
//				"— Untersuchungen ueber die Tiergemeinschaften des Bodens: Die Oribatiden und ihre Synusien in den Boeden Norddeutschlands. Zoologica (Stuttgart) 104, 1—173 (1952a). " +
//				"— Zwei neue Arten der Oribatidengattung Nanhermannia. Zool. Anz. 150, 69—75 (1953a). " +
//				"— Oribates (Acariens). In: Expeditions Polaires Francaises. Missions Paul—Emile Victor. VII. Microfauna du sol de l'eqe Groenland. 1 Arachnides p. 1—84. 1955a. " +
//				"— Tischler, W.: Synoekologie der Landtiere. Stuttgart 1955 a. 414 S. " +
//				"— Tuxen, S. L.: Die zeitliche und raeumliche Verteilung der Oribatiden-Fauna (Acar.) bei Maelifell, NordIsland. Entomol. Medd. 23, 321—336 (1943a). " +
//				"— Willmann, C: Oribatiden aus Quellmoosen. Arch. f. Hydrobiol. 14, 470—477 (1923a). " +
//				"— Die Oribatidenfauna norddeutscher und einiger sueddeutscher Moore. Abh. naturw. Ver. Bremen 27, 143—176 (1928a). " +
//				"— Moosmilben oder Oribatiden (Oribatei). In Dahl, Tierwelt Deutschlands, Teil 22, S. 79—200. 1931a. " +
//				"— Neue Acari aus schlesischen Wiesenboeden. Zool. Anz. 113, 273—290 (1936a). " +
//				"— Beitrag zur Kenntnis der Acarofauna der ostfriesischen Inseln. Abh. naturw. Ver. Bremen 30, 152—169 (1937a). " +
//				"— Beitrag zur Kenntnis der Acarofauna des Komitates Bars. Ann. Mus. Nation. Hung. 31, 144—172 (1937/38a). " +
//				"— Die Moorfauna des Glatzer Schneeberges. 3. Die Milben der Schneebergmoore. Beitr. Biol. Glatzer Schneeberges 5, 427—458 (1939a). Acari aus nordwestdeutschen Mooren. Abh. naturw. Ver. Bremen 32, 163—183 (1942a). " +
//				"— Terrestrische Milben aus Schwedisch-Lappland. Arch. f. Hydrobiol. 40, 208—239 (1943a). " +
//				"— Untersuchungen ueber die terrestrische Milbenfauna im pannonischen Klimagebiet Österreichs. Sitzgsber. oesterr. Akad. Wiss. Math.-naturwiss. Kl., Ser. I 160, 91—176 (1951a). " +
//				"— Die hochalpine Milbenfauna der mittleren Hohen Tauern, insbesondere des Grossglockner-Gebietes (Acari). Bonn. zool. Beitr. 2, 141—176 (1951b). " +
//				"— Die Milbenfauna der Nordseeinsel Wangerooge. Veroeff. Inst. Meeresforsch. Bremerhaven 1, 139—186 (1952a). " +
//				"— Maehrische Acari hauptsaechlich aus dem Gebiete des maerkischen Karstes. Ceskoslovenska parasitol. 1, 213—272 (1954a)." +
//				"</paragraph>";
//		docString = "<paragraph lastPageId=\"35\" lastPageNumber=\"432\" pageId=\"34\" pageNumber=\"431\">Balogh, J.: Grundzuege der Zoozoenologie. Budapest, Separatum, S. 145-248. 1953a. - Caeoli, E., e R. Maffia: Due specie nuove ed una poco nota di Oribatei (Acari) dellaVenezia Tridentina. Ann. Mus. zool. Univ.Napoli 6 (12), 1-12 (1934a). Cooreman, J.: Notes sur la Faune des Hautes-Fagnes en Belgique. VI (2e partie) Oribatei (Acariens). Bull. Mus. Hist. natur. Belg. 17 (73), 1-12 (1941a). - Acariens , in W. Conrad Sur la faune et la flore d'un ruisseau de l'Ardenne Belge. Mem. Mus. roy. Hist. natur. Belg. 99, 124-127 (1942a). - Evans, G. O.: British mites of the genus Brachychthonius Berl. 1910. Ann. Mag. natur. Hist., Ser. XII, 5 (51), 227-239 (1952a). - Forsslund, K. H.: Schwedische Oribatei (Acari). I. Ark. Zool., (Stockh.) 34 (A, Nr 10), 1-11 (1942a). - Studien ueber die Tierwelt des nordschwedischen Waldbodens. Medd. Stat. Skogsfoersoeksanst. 34 (5), 341-364 (1943 a). - Fbanz, H.: Ueber die Bedeutung terricoler Kleintiere fuer den Stickstoffund Humushaushalt des Bodens. Z. Pflanzenernaehrg. Dueng. u. Bodenkde. 55, 44-52 (1951 a). - Der Einfluss verschiedener Duengungsmassnahmen auf die Bodenfauna . Angew. Pflanzensoziol. (Wien) 11, 1-50 (1953a). - Grandjean, F.: Observations sur les Oribates. 2. ser. Bull. Mus. Hist. natur. Paris, Ser. II 3 (7), 651-665 (1931a). - Observations sur les Oribates. 3. ser. Bull. Mus. Hist. natur. Paris, Ser. II 4 (3), 292-306 (1932a). - Les Oribates de Jean Frederic Hermann et de son pere. Ann. Soc. Ent. France 105, 27-110 (1936a). - Sur les Hydrozetes (Acariens) de l'Europe occidentale. Bull. Mus. Hist, natur. Paris, Ser. II 20 (4), 328-335 (1948a). - Sur le genre Hydrozetes Berl. Bull. Mus. Hist. natur. Paris, Ser. II 21 (2), 224-231 (1949a). - Comparaison du genre Limnozetes au genre Hydrozetes (Oribates). Bull. Mus. Hist. natur. Paris, Ser. II 23 (2), 200-207 (1951a). - Observations sur les Oribates. 26. ser. Bull. Mus. Hist. natur. Paris, Ser. II 25 (3), 286-293 (1953a). - Observations sur les Oribates . 25. ser. Bull. Mus. Hist. natur. Paris, Ser. II 25 (2), 155-162 (1953 b). - Hammen, L. v. d.: The Oribatei (Acari) of the Netherlands. Zool. Verh. Leiden 17, 1-139 (1952a). - Hammer, M.: The Oribatid and Collembola fauna in some soil samples from Sondre Stromfjord. Entomol. Medd. 26, 404-414 (1952a). - Jacot, A. P.: Les Phthiracaridae de Karl Ludwig Koch. Rev. Suisse Zool. 43, 161-187 (1936a). - Knuelle, W.: Neue Arten der Oribatiden-Gattung Pelops (Acari). Zool. Anz. 153, 215-221 (1954a). - Die Arten der Gattung Tectocepheus Berlese (Acarina: Oribatei). Zool. Anz. 152, 280-305 (1954b). - Differentialdiagnostische Kennzeichnung der Oribatei (Acari). VI. Congr. Internat. de la Science du Sol, Paris 1956, III, 4, 19-21 (1956a).-Morphologische und entwicklungsgeschichtliche Untersuchungen zum Phylogenetischen System der Acari: Acariformes Zachv. I. Oribatei, Malaconothridae. Mitt. zool. Mus. Berlin 33(1), 97-213 (1957a). - Vorkommen und Indikationswert von Oribatiden (Acari: Oribatei) in postglazialen Ablagerungen. Zool. Anz. (1957b). - Kubiena, W. L.: Bestimmungsbuch und Systematik der Boeden Europas. Stuttgart 1956 a. 392 S. - Paoli, G.: Monografia del genere Dameosama Berl. e generi affini. Redia 5, 31-91 (1908a). - Pschorn-Walcher , H.: Zur Biologie und Systematik terricoler Milben. I. Die ostalpinen Arten der Gattung Liacarus Mich. (Oribatei). Bonn. zool. Beitr. 2, 177-183 (1951 a). - Rabeler, W.: Die Fauna des Goeldenitzer Hochmoores in Mecklenburg. Z. Morph. u. Oekol. Tiere 21, 173-315 (1931a). - Schuster, R.: Der Anteil der Oribatiden an den Zersetzungsvorgaengen im Boden. Z. Morph. u. Oekol. Tiere 45, 1-35 (1956a). - Schweizer, J.: Landmilben aus der Umgebung des schweizerischen Nationalparks. Erg. wiss. Unters. Schweiz. Nationalparks, N. F. 2 (20), 1-28 (1948a). - Sellnick, M.: Oribatiden vom Zwergbirkenmoor bei Neulinum, Kr. Kulm, und vom Moor am kleinen Heidsee bei Heubude unweit Danzig. Schr. Ges. Naturforsch. Danzig, N. F. 15, 69-77 (1921a). - Die mir bekannten Arten der Gattung Tritia Beblese. Acari 3 (1923a). - Oribatiden. In A. Dampf, Zur Kenntnis der estlaendischen Moorfauna. Sitzgsber. naturforsch. Ges. Dorpat 31, 65-71 (1924a). - Die Oribatiden (Hornmilben) des Zehlaubruches. Schr. Phys.- oekonom. Ges. Koenigsberg 66 (2) (Zehlau-H., Teil 2), 324-351 (1929a). - Zoologische Forschungsreise nach den Jonischen Inseln und dem Peleponnes. XVI. Acari. Sitzgsber. Akad. Wiss. Wien, Math.-naturwiss. Kl., Ser. I 140, 693-776 (1931a). - Sellnick, M., u. K. H. Forsslund: Die Gattung Carabodes C. L. Koch 1836 in der schwedischen Bodenfauna (Acar. Oribat.). Ark. Zool. (Stockh.), Ser. II 4 (22), 367-390 (1953a). - Die Camisiidae Schwedens (Acar. Oribat.). Ark. Zool. (Stockh.) Ser. II 8 (4), 473-530 (1955a). - Strenzke, K.: Beitraege zur Systematik landlebender Milben. I. u. II. Arch. f. Hydrobiol. 40, 57-70 (1943a). - Die biozoenotischen Grundlagen der Bodenzoologie. Z. Pflanzenernaehrg. Dueng. u. Bodenkde. 45, 245-262 (1949a). - Oribatella artica litoralis nov. subsp., eine neue Oribatide der Nord- und Ostseekueste (Acarina: Oribatei). Kiel. Meeresforsch. 7 (2), 157-160 (1950 a). - Die norddeutschen Arten der Gattung Brachychthonius und Brachychochthonius(Acarina: Oribatei). Dtsch. zool. Z. 1, 233-249 (1951a). - Some new Central European Moss-mites (Acarina: Oribatei). Ann. Mag. Natur. Hist. London, Ser. XII, 4, 719-726 (1951b). - Die norddeutschen Arten der Oribatiden-Gattung Suctobelba. Zool. Anz. 147, 147-166 (1951c). - Grundfragen der Autoekologie. Acta biotheoretica 9, 163-184 (1951d). - Untersuchungen ueber die Tiergemeinschaften des Bodens: Die Oribatiden und ihre Synusien in den Boeden Norddeutschlands . Zoologica (Stuttgart) 104, 1-173 (1952a). - Zwei neue Arten der Oribatidengattung Nanhermannia. Zool. Anz. 150, 69-75 (1953a). - Oribates (Acariens). In: Expeditions Polaires Francaises. Missions Paul-Emile Victor. VII. Microfauna du sol de l'eqe Groenland. 1 Arachnides p. 1-84. 1955a. - Tischler, W.: Synoekologie der Landtiere. Stuttgart 1955 a. 414 S. - Tuxen, S. L.: Die zeitliche und raeumliche Verteilung der Oribatiden-Fauna (Acar.) bei Maelifell, NordIsland . Entomol. Medd. 23, 321-336 (1943a). - Willmann, C: Oribatiden aus Quellmoosen. Arch. f. Hydrobiol. 14, 470-477 (1923a). - Die Oribatidenfauna norddeutscher und einiger sueddeutscher Moore. Abh. naturw. Ver. Bremen 27, 143-176 (1928a). - Moosmilben oder Oribatiden (Oribatei). In Dahl, Tierwelt Deutschlands, Teil 22, S. 79-200. 1931a. - Neue Acari aus schlesischen Wiesenboeden . Zool. Anz. 113, 273-290 (1936a). - Beitrag zur Kenntnis der Acarofauna der ostfriesischen Inseln. Abh. naturw. Ver. Bremen 30, 152-169 (1937a). - Beitrag zur Kenntnis der Acarofauna des Komitates Bars. Ann. Mus. Nation. Hung. 31, 144-172 (1937/38a). - Die Moorfauna des Glatzer Schneeberges. 3. Die Milben der Schneebergmoore. Beitr. Biol. Glatzer Schneeberges 5, 427-458 (1939a). Acari aus nordwestdeutschen Mooren. Abh. naturw. Ver. Bremen 32, 163-183 (1942a). - Terrestrische Milben aus Schwedisch-Lappland. Arch. f. Hydrobiol. 40, 208-239 (1943a). - Untersuchungen ueber die terrestrische Milbenfauna im pannonischen Klimagebiet Oesterreichs. Sitzgsber. oesterr. Akad. Wiss. Math.-naturwiss . Kl., Ser. I 160, 91-176 (1951a). - Die hochalpine Milbenfauna der mittleren Hohen Tauern, insbesondere des Grossglockner-Gebietes (Acari). Bonn. zool. Beitr. 2, 141-176 (1951b). - Die Milbenfauna der Nordseeinsel Wangerooge. Veroeff. Inst. Meeresforsch. Bremerhaven 1, 139-186 (1952a). - Maehrische Acari hauptsaechlich aus dem Gebiete des maerkischen Karstes. Ceskoslovenska parasitol. 1, 213-272 (1954a).</paragraph>";
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader(docString));
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader(new File("E:/TestDaten/CitationBlockTest.xml")));
//		doc.addAnnotation(PARAGRAPH_TYPE, 0, doc.size());
		BibRefBlockSplitter cbs = new BibRefBlockSplitter();
		Properties params = new Properties();
		params.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
		cbs.process(doc, params);
		Annotation[] citations = doc.getAnnotations(CITATION_TYPE);
		for (int c = 0; c < citations.length; c++)
			System.out.println(citations[c].toXML());
	}
}
