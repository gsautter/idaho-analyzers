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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CategorizationFeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * The paragraph classifyer displays the paragraphs of the document page by
 * page, and allows for distinguishing which paragraphs belong to the document's
 * main text, and which are artifacts, like footnotes, cations, or page titels.
 * The artifacts need to be tagged beforehand by other means than this Analyzer.
 * Which types of annotations are regarded as artifacts can be specified in this
 * Analyzer's 'artifactAnnotationTypes.txt' configuration file, one type per
 * line, with the highlight color for the types appended at the end of the
 * respective lines, separated by a space, and given in six digit hexadecimal
 * RGB notation (as known from HTML/CSS). The line 'footnote FF0000', for
 * instance, specifies that paragraphs containing 'footnote' annotations shoud
 * be regarded as artifacts, and should be highlighted in red (FF0000 decodes to
 * red).
 * 
 * @author sautter
 */
public class ParagraphClassifyerOnline extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String MAIN_TEXT_CATEGORY = "Main Text";
	private static final Color MAIN_TEXT_COLOR = Color.WHITE;
	
	private String[] artifactAnnotationTypes = new String[0];
	private String[] artifactAnnotationTypeCategories = new String[0];
	
	private String feedbackDialogLabel = "";
	
	private Properties typeCategoryMappings = new Properties();
	private String getCategoryForType(String type) {
		return this.typeCategoryMappings.getProperty(type);
	}
	
	private Properties categoryTypeMappings = new Properties();
	private String getTypeForCategory(String category) {
		return this.categoryTypeMappings.getProperty(category);
	}
	
	private HashMap categoryColors = new HashMap();
	private Color getCategoryColor(String category) {
		Color categoryColor = ((Color) this.categoryColors.get(category));
		if (categoryColor == null) {
			categoryColor = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			this.categoryColors.put(category, categoryColor);
		}
		return categoryColor;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load custom artifact types
		StringVector artifactAnnotationTypes = new StringVector();
		try {
			InputStream is = this.dataProvider.getInputStream("artifactAnnotationTypes.txt");
			artifactAnnotationTypes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
		artifactAnnotationTypes.removeAll("");
		
		//	filter and sort types
		for (int t = 0; t < artifactAnnotationTypes.size(); t++) {
			if (artifactAnnotationTypes.get(t).startsWith("//"))
				artifactAnnotationTypes.remove(t--);
		}
		artifactAnnotationTypes.sortLexicographically(false, false);
		
		//	store types
		this.artifactAnnotationTypes = artifactAnnotationTypes.toStringArray();
		
		//	produce labels and extract colors
		this.artifactAnnotationTypeCategories = new String[this.artifactAnnotationTypes.length];
		StringBuffer dialogLabel = new StringBuffer("<HTML>Please check if the paragraphs of this page are" + 
				"<BR>- Parts of the document main text");
		for (int t = 0; t < this.artifactAnnotationTypes.length; t++) {
			String type = this.artifactAnnotationTypes[t].trim();
			Color color = null;
			
			//	parse color if given
			if (type.indexOf(' ') != -1) {
				color = FeedbackPanel.getColor(type.substring(type.indexOf(' ') + 1));
				type = type.substring(0, type.indexOf(' '));
			}
			
			//	build category
			StringBuffer category = new StringBuffer();
			category.append(Character.toUpperCase(type.charAt(0)));
			for (int c = 1; c < type.length(); c++) {
				if (Character.isUpperCase(type.charAt(c)))
					category.append(' ');
				category.append(type.charAt(c));
			}
			
			//	store data
			this.artifactAnnotationTypes[t] = type;
			this.artifactAnnotationTypeCategories[t] = category.toString();
			
			this.typeCategoryMappings.setProperty(this.artifactAnnotationTypes[t], this.artifactAnnotationTypeCategories[t]);
			this.categoryTypeMappings.setProperty(this.artifactAnnotationTypeCategories[t], this.artifactAnnotationTypes[t]);
			
			if (color != null)
				this.categoryColors.put(this.artifactAnnotationTypeCategories[t], color);
			
			//	extend dialog label
			if (this.artifactAnnotationTypeCategories[t].endsWith("y"))
				dialogLabel.append("<BR>- " + this.artifactAnnotationTypeCategories[t].substring(0, (this.artifactAnnotationTypeCategories[t].length() - 1)) + "ies");
			else dialogLabel.append("<BR>- " + this.artifactAnnotationTypeCategories[t] + "s");
		}
		
		//	finish and store label
		dialogLabel.append("</HTML>");
		this.feedbackDialogLabel = dialogLabel.toString();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		/*
		 * identify main text, page titles, footnotes, captions, and citations
		 * (check for existing ones => plus, use specialized CitationTagger
		 * beforehand)
		 */
		
		//	sort out pages already checked
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		ArrayList pageList = new ArrayList();
		for (int pg = 0; pg < pages.length; pg++) {
			boolean hasCheckedArtifacts = false;
			boolean hasUnCheckedArtifacts = false;
			
			//	find artifacts
			for (int t = 0; t < this.artifactAnnotationTypes.length; t++) {
				Annotation[] artifacts = pages[pg].getAnnotations(this.artifactAnnotationTypes[t]);
				if (artifacts.length != 0) {
					for (int a = 0; a < artifacts.length; a++) {
						System.out.println(this.artifactAnnotationTypes[t] + " on page " + (pg+1) + ", " + (artifacts[a].hasAttribute("_artifactCleanup") ? "checked" : "unchecked"));
						if (artifacts[a].hasAttribute("_artifactCleanup"))
							hasCheckedArtifacts = true;
						else hasUnCheckedArtifacts = true;
					}
				}
			}
			
			//	checked artifacts would prove we've seen this one before, or unchecked artifacts present
			if (!hasCheckedArtifacts || hasUnCheckedArtifacts)
				pageList.add(pages[pg]);
		}
		pages = ((MutableAnnotation[]) pageList.toArray(new MutableAnnotation[pageList.size()]));
		
		
		//	get paragraphs from remaining pages, and create dialogs
		MutableAnnotation[][] pageParagraphs = new MutableAnnotation[pages.length][];
		CategorizationFeedbackPanel[] cfps = new CategorizationFeedbackPanel[pages.length];
		for (int pg = 0; pg < pages.length; pg++) {
			
			//	prepare dialog
			cfps[pg] = new CategorizationFeedbackPanel("Check Paragraph Types");
			cfps[pg].setLabel(this.feedbackDialogLabel);
			
			cfps[pg].setPropagateCategoryChanges(false);
			cfps[pg].setChangeSpacing(20);
			cfps[pg].setContinueSpacing(10);
			
			cfps[pg].addCategory(MAIN_TEXT_CATEGORY);
			cfps[pg].setCategoryColor(MAIN_TEXT_CATEGORY, MAIN_TEXT_COLOR);
			
			for (int t = 0; t < this.artifactAnnotationTypeCategories.length; t++) {
				cfps[pg].addCategory(this.artifactAnnotationTypeCategories[t]);
				cfps[pg].setCategoryColor(this.artifactAnnotationTypeCategories[t], this.getCategoryColor(this.artifactAnnotationTypeCategories[t]));
			}
			System.out.println("  - dialog built");
			
			//	pre-categorize and line up paragraphs
			pageParagraphs[pg] = pages[pg].getMutableAnnotations(PARAGRAPH_TYPE);
			System.out.println("  - got " + pageParagraphs[pg].length + " paragraphs");
			for (int p = 0; p < pageParagraphs[pg].length; p++) {
				String category = MAIN_TEXT_CATEGORY;
				for (int t = 0; t < this.artifactAnnotationTypes.length; t++) {
					Annotation[] artifacts = pageParagraphs[pg][p].getAnnotations(this.artifactAnnotationTypes[t]);
					if (artifacts.length != 0) {
						category = this.getCategoryForType(this.artifactAnnotationTypes[t]);
						System.out.println("    - artifact in paragraph " + p + ": " + category);
						t = this.artifactAnnotationTypes.length;
					}
				}
				
				cfps[pg].addLine(TokenSequenceUtils.concatTokens(pageParagraphs[pg][p]), category);
			}
			
			//	add background information
			cfps[pg].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, pages[pg].getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			cfps[pg].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, pages[pg].getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			cfps[pg].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, pages[pg].getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			cfps[pg].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, pages[pg].getAnnotationID());
			cfps[pg].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PARAGRAPH_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString(pages, pg, (pg+1));
			if (targetPages != null)
				cfps[pg].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString(pages, pg, (pg+1));
			if (targetPageIDs != null)
				cfps[pg].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		
		//	prepare data structures
		String[][] pageParagraphTypes = new String[pages.length][];
		int cutoffPage = pages.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(cfps);
			
			//	collect data
			for (int d = 0; d < cfps.length; d++) {
				
				//	initialize type store
				pageParagraphTypes[d] = new String[pageParagraphs[d].length];
				
				//	store types (main text type will result in null)
				for (int p = 0; p < pageParagraphs[d].length; p++)
					pageParagraphTypes[d][p] = this.getTypeForCategory(cfps[d].getCategoryAt(p));
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < cfps.length; d++) {
			if (d != 0)
				cfps[d].addButton("Previous");
			cfps[d].addButton("Cancel");
			cfps[d].addButton("OK" + (((d+1) == cfps.length) ? "" : " & Next"));
			
			String title = cfps[d].getTitle();
			cfps[d].setTitle(title + " - (" + (d+1) + " of " + cfps.length + ")");
			
			String f = cfps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			cfps[d].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				
				//	initialize type store
				pageParagraphTypes[d] = new String[pageParagraphs[d].length];
				
				//	store types (main text type will result in null)
				for (int p = 0; p < pageParagraphs[d].length; p++)
					pageParagraphTypes[d][p] = this.getTypeForCategory(cfps[d].getCategoryAt(p));
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog on
			else {
				cutoffPage = d;
				d = cfps.length;
			}
//			cfps[d].addButton("OK");
//			cfps[d].addButton("Cancel");
//			
//			String f = null;
////			try {
////				f = FeedbackPanelHtmlTester.testFeedbackPanel(cfps[d], 0);
////			}
////			catch (IOException ioe) {
////				ioe.printStackTrace();
////			}
//			if (f == null)
//				f = cfps[d].getFeedback();
//			
//			//	cancel from current dialog on
//			if ("Cancel".equals(f)) {
//				cutoffPage = d;
//				d = cfps.length;
//			}
//			
//			//	current dialog submitted, process data
//			else {
//				
//				//	initialize type store
//				pageParagraphTypes[d] = new String[pageParagraphs[d].length];
//				
//				//	store types (main text type will result in null)
//				for (int p = 0; p < pageParagraphs[d].length; p++)
//					pageParagraphTypes[d][p] = this.getTypeForCategory(cfps[d].getCategoryAt(p));
//			}
		}
		
		
		//	process feedback
		for (int pg = 0; pg < cutoffPage; pg++) {
			
			//	do the paragraphs individually
			for (int p = 0; p < pageParagraphs[pg].length; p++) {
				
				//	get old artifacts
				HashMap oldArtifacts = new HashMap();
				for (int t = 0; t < this.artifactAnnotationTypes.length; t++) {
					Annotation[] artifacts = pageParagraphs[pg][p].getAnnotations(this.artifactAnnotationTypes[t]);
					for (int a = 0; a < artifacts.length; a++) {
						String key = (artifacts[a].getType() + "-" + artifacts[a].getStartIndex() + "-" + artifacts[a].size());
						oldArtifacts.put(key, artifacts[a]);
					}
				}
				
				//	paragraph contains artifact artifact
				if (pageParagraphTypes[pg][p] != null) {
					String key = (pageParagraphTypes[pg][p] + "-" + 0 + "-" + pageParagraphs[pg][p].size());
					Annotation artifact;
					
					//	annotation exists, preserve it
					if (oldArtifacts.containsKey(key)) {
						artifact = ((Annotation) oldArtifacts.remove(key));
						artifact.setAttribute("_artifactCleanup", "retained");
					}
					
					//	add new sub section
					else {
						artifact = pageParagraphs[pg][p].addAnnotation(pageParagraphTypes[pg][p], 0, pageParagraphs[pg][p].size());
						artifact.setAttribute("_artifactCleanup", "added");
					}
				}
				
				//	remove remaining old artifacts
				for (Iterator osit = oldArtifacts.values().iterator(); osit.hasNext();)
					pageParagraphs[pg][p].removeAnnotation((Annotation) osit.next());
			}
		}
	}
}