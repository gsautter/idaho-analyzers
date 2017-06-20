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

package de.uka.ipd.idaho.plugins.treatmentSplitting;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CategorizationFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.RuleBox;
import de.uka.ipd.idaho.gamta.util.swing.RuleBox.Rule;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TreatmentStructurerOnline extends AbstractConfigurableAnalyzer implements TreatmentConstants, LocationConstants {
//	
//	//	in-treatment div-types according to TaxonX
//	private static final String biology_ecology_SUB_SUB_SECTION_TYPE = "biology_ecology";
//	private static final String description_SUB_SUB_SECTION_TYPE = "description";
//	private static final String diagnosis_SUB_SUB_SECTION_TYPE = "diagnosis";
//	private static final String discussion_SUB_SUB_SECTION_TYPE = "discussion";
//	private static final String distribution_SUB_SUB_SECTION_TYPE = "distribution";
//	private static final String etymology_SUB_SUB_SECTION_TYPE = "etymology";
//	private static final String key_SUB_SUB_SECTION_TYPE = "key";
//	private static final String materials_examined_SUB_SUB_SECTION_TYPE = "materials_examined";
//	private static final String synonymic_list_OPTION = "synonymic_list";
//	
//	//	nomenclature is not a div in TaxonX
//	private static final String nomenclature_SUB_SUB_SECTION_TYPE = "nomenclature";
//	
//	//	reference group is not a div in TaxonX
//	private static final String reference_group_SUB_SUB_SECTION_TYPE = "reference_group";
//	
//	//	multiple div type is not usedin treatments, but is helpful for enlosing artifacts
//	private static final String multiple_SUB_SUB_SECTION_TYPE = "multiple";
	
	//	special div type for layout artifacts
	private static final String ARTIFACT_SUB_SUB_SECTION_TYPE = "ARTIFACT";
	
	//	special div type for unclassified paragraphs
	private static final String UNCLASSIFIED_SUB_SUB_SECTION_TYPE = "";
	
	//	option arrays
	private static final String[] TREATMENT_SUB_SUB_SECTION_TYPES = {
			biology_ecology_TYPE,
			conservation_TYPE,
			description_TYPE,
			diagnosis_TYPE,
			discussion_TYPE,
			distribution_TYPE,
			etymology_TYPE,
			key_TYPE,
			materials_examined_TYPE,
			nomenclature_TYPE,
			reference_group_TYPE,
			synonymic_list_OPTION,
			vernacular_names_TYPE
		};
//	
//	//	option arrays
//	private static final String[] TREATMENT_SUB_SUB_SECTION_TYPES = {
//			biology_ecology_SUB_SUB_SECTION_TYPE,
//			description_SUB_SUB_SECTION_TYPE,
//			diagnosis_SUB_SUB_SECTION_TYPE,
//			discussion_SUB_SUB_SECTION_TYPE,
//			distribution_SUB_SUB_SECTION_TYPE,
//			etymology_SUB_SUB_SECTION_TYPE,
//			key_SUB_SUB_SECTION_TYPE,
//			materials_examined_SUB_SUB_SECTION_TYPE,
//			nomenclature_SUB_SUB_SECTION_TYPE,
//			reference_group_SUB_SUB_SECTION_TYPE,
//			synonymic_list_OPTION
//		};
	
	private StringVector artifactAnnotationTypes = new StringVector();
	
//	private String defaultDivType = description_SUB_SUB_SECTION_TYPE;
	private String defaultDivType = description_TYPE;
	
	private RuleBox rules;
	private boolean rulesModified = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load custom artifact types
		try {
			InputStream is = this.dataProvider.getInputStream("artifactAnnotationTypes.txt");
			this.artifactAnnotationTypes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
		
		//	add fix artifact types
		this.artifactAnnotationTypes.addElementIgnoreDuplicates(PAGE_BORDER_TYPE);
		this.artifactAnnotationTypes.addElementIgnoreDuplicates(PAGE_TITLE_TYPE);
		this.artifactAnnotationTypes.addElementIgnoreDuplicates(PAGE_NUMBER_TYPE);
		
		
		//	get default div type
//		this.defaultDivType = this.getParameter("defaultDivType", description_SUB_SUB_SECTION_TYPE);
		this.defaultDivType = this.getParameter("defaultDivType", description_TYPE);
		
		
		//	create rule box
		this.rules = new RuleBox(TREATMENT_SUB_SUB_SECTION_TYPES, this.defaultDivType);
		try {
			InputStream is = this.dataProvider.getInputStream("treatmentStructurerRules.txt");
			StringVector ruleStrings = StringVector.loadList(is);
			is.close();
			for (int r = 0; r < ruleStrings.size(); r++) {
				String ruleString = ruleStrings.get(r);
				
				//	filter out comments
				if (!ruleString.startsWith("//")) {
					try {
						this.rules.addRule(ruleString);
					}
					catch (Exception e) {
						System.out.println("Exception loading rule: " + e.getMessage());
					}
				}
			}
		} catch (IOException ioe) {}
		
		
		//	get type-specific highlight colors
		try {
			InputStream is = this.dataProvider.getInputStream("treatmentStructurerTypeColors.txt");
			StringVector typeLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeLines.size(); t++) {
				String typeLine = typeLines.get(t).trim();
				if ((typeLine.length() != 0) && !typeLine.startsWith("//")) {
					int split = typeLine.indexOf(' ');
					if (split != -1) {
						String type = typeLine.substring(0, split).trim();
						Color color = FeedbackPanel.getColor(typeLine.substring(split).trim());
						this.typeColors.put(type, color);
					}
				}
			}
		} catch (IOException fnfe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		
		//	test if configuration changed
		if (this.rulesModified) {
			
			//	store default div type
			this.storeParameter("defaultDivType", this.defaultDivType);
			
			//	collect string representation of rules
			StringVector ruleStrings = new StringVector();
			Rule[] rules = this.rules.getRules();
			for (int r = 0; r < rules.length; r++)
				ruleStrings.addElement(rules[r].toDataString());
			
			//	store rules
			try {
				OutputStream os = dataProvider.getOutputStream("treatmentStructurerRules.txt");
				ruleStrings.storeContent(os);
				os.flush();
				os.close();
			} catch (IOException ioe) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		System.out.println("TreatmentStructurer: doing document ...");
		
		//	get treatments
		MutableAnnotation[] treatments = data.getMutableAnnotations(TREATMENT_ANNOTATION_TYPE);
		System.out.println(" - got " + treatments.length + " treatments");
		
		//	select treatments to process
		ArrayList tdbList = new ArrayList();
		for (int t = 0; t < treatments.length; t++) {
			System.out.println(" - doing treatment " + (t+1) + "/" + treatments.length + " (size: " + treatments[t].size() + ") ...");
			
			//	group paragraphs to treatments and other contents
			MutableAnnotation[] subSubSections = treatments[t].getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
			System.out.println("   - got " + subSubSections.length + " sub sections");
			int sssSum = 0;
			for (int s = 0; s < subSubSections.length; s++)
				sssSum += subSubSections[s].size();
			System.out.println("   - total sub section length is " + sssSum);
			
			/*	process treatment only if 
			 *  there are paragraphs 
			 *    and 
			 *  (
			 *    there are parts of the treatment not covered by sub sections 
			 *      or
			 *    there is only one treatment - possibly a correction on a single treatment that is already covered
			 *  )
			 */ 
			MutableAnnotation[] paragraphs = treatments[t].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			System.out.println("   - got " + paragraphs.length + " paragraphs");
			
			//	put treatment in box and fill basic data structures
			if ((paragraphs.length != 0) && ((sssSum < treatments[t].size()) || (treatments.length == 1))) {
				TreatmentDataBox tdb = new TreatmentDataBox(treatments[t]);
				this.preprocessTreatment(tdb);
				tdbList.add(tdb);
			}
		}
		
		//	put relevant treatments in array
		TreatmentDataBox[] tdbs = ((TreatmentDataBox[]) tdbList.toArray(new TreatmentDataBox[tdbList.size()]));
		
		//	get feedback for all treatments together if allowed to
		if (parameters.containsKey(INTERACTIVE_PARAMETER))
			this.getFeedback(tdbs);
		
		//	use rule based result otherwise
		else for (int d = 0; d < tdbs.length; d++)
			tdbs[d].feedbackRelevantParagraphTypes = tdbs[d].relevantParagraphTypes;
		
		//	write feedback result
		for (int t = 0; t < tdbs.length; t++)
			this.postprocessTreatment(tdbs[t]);
	}
	
	private static class TreatmentDataBox {
		
		MutableAnnotation treatment = null;
		
		QueriableAnnotation[] subSubSections = null;
		
		QueriableAnnotation[] paragraphs = null;
		String[] paragraphTypes = null;
		
		QueriableAnnotation[] relevantParagraphs = null;
		int[] relevantParagraphIndices = null;
		String[] relevantParagraphTypes = null;
		
		String[] feedbackRelevantParagraphTypes = null;
		
		TreatmentDataBox(MutableAnnotation treatment) {
			this.treatment = treatment;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	private void preprocessTreatment(TreatmentDataBox tdb) {
		
		//	get paragraphs first (check if something to classify)
		tdb.paragraphs = tdb.treatment.getMutableAnnotations(PARAGRAPH_TYPE);
		
		//	get existing treatments and non-treatment subSections
		ArrayList existingStructureList = new ArrayList();
		
		//	add existing sections of other types
		tdb.subSubSections = tdb.treatment.getAnnotations(SUB_SUB_SECTION_TYPE);
		for (int s = 0; s < tdb.subSubSections.length; s++) existingStructureList.add(tdb.subSubSections[s]);
		
		//	sort existing sections
		Annotation[] existingStructure = ((Annotation[]) existingStructureList.toArray(new Annotation[existingStructureList.size()]));
		Arrays.sort(existingStructure, AnnotationUtils.getComparator(tdb.treatment.getAnnotationNestingOrder()));
		
		//	fill gaps with "multiple" subSubSections
		int lastEnd = 0;
		existingStructureList.clear();
		for (int e = 0; e < existingStructure.length; e++) {
			if (lastEnd < existingStructure[e].getStartIndex()) {
				Annotation fillingNonTreatment = Gamta.newAnnotation(tdb.treatment, SUB_SUB_SECTION_TYPE, lastEnd, (existingStructure[e].getStartIndex() - lastEnd));
				fillingNonTreatment.setAttribute(TYPE_ATTRIBUTE, UNCLASSIFIED_SUB_SUB_SECTION_TYPE);
				existingStructureList.add(fillingNonTreatment);
			}
			existingStructureList.add(existingStructure[e]);
		}
		if (lastEnd < tdb.treatment.size()) {
			Annotation fillingNonTreatment = Gamta.newAnnotation(tdb.treatment, SUB_SUB_SECTION_TYPE, lastEnd, (tdb.treatment.size() - lastEnd));
			fillingNonTreatment.setAttribute(TYPE_ATTRIBUTE, UNCLASSIFIED_SUB_SUB_SECTION_TYPE);
			existingStructureList.add(fillingNonTreatment);
		}
		existingStructure = ((Annotation[]) existingStructureList.toArray(new Annotation[existingStructureList.size()]));
		
		//	get indices and types
		int[] existingStructureStarts = new int[existingStructure.length];
		String[] existingStructureTypes = new String[existingStructure.length];
		for (int e = 0; e < existingStructure.length; e++) {
			existingStructureStarts[e] = existingStructure[e].getStartIndex();
			existingStructureTypes[e] = existingStructure[e].getAttribute(TYPE_ATTRIBUTE, UNCLASSIFIED_SUB_SUB_SECTION_TYPE).toString();
		}
		
		
		//	initialize data structures
		tdb.paragraphTypes = new String[tdb.paragraphs.length];
		ArrayList relevantParagraphList = new ArrayList();
		ArrayList relevantParagraphIndexList = new ArrayList();
		ArrayList relevantParagraphTypeList = new ArrayList();
		
		//	get paragraphs nested in multi-paragraph artifacts
		HashSet artifactParagraphIDs = new HashSet();
		for (int t = 0; t < this.artifactAnnotationTypes.size(); t++) {
			MutableAnnotation[] artifacts = tdb.treatment.getMutableAnnotations(this.artifactAnnotationTypes.get(t));
			for (int a = 0; a < artifacts.length; a++) {
				Annotation[] artifactParagraphs = artifacts[a].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < artifactParagraphs.length; p++)
					artifactParagraphIDs.add(artifactParagraphs[p].getAnnotationID());
			}
		}
		
		//	pre-classify paragraphs, and sort out artifacts
		int esIndex = 0;
		for (int p = 0; p < tdb.paragraphs.length; p++) {
			
			//	check if layout artifact
			boolean isArtifact = artifactParagraphIDs.contains(tdb.paragraphs[p].getAnnotationID());
			for (int a = 0; a < this.artifactAnnotationTypes.size(); a++) {
				Annotation[] artifacts = tdb.paragraphs[p].getAnnotations(this.artifactAnnotationTypes.get(a));
				if ((artifacts.length != 0) && (artifacts[0].size() == tdb.paragraphs[p].size()))
					isArtifact = true;
			}
			
			//	layout artifact, exclude from dialog
			if (isArtifact)
				tdb.paragraphTypes[p] = ARTIFACT_SUB_SUB_SECTION_TYPE;
				
			//	determine type of non-artifact paragraph
			else {
				relevantParagraphList.add(tdb.paragraphs[p]);
				relevantParagraphIndexList.add(new Integer(p));
				String type; // do not initialize so compiler ensures meaningful initialization
				
				//	jump to next start of existing structure
				while ((esIndex < existingStructureStarts.length) && (existingStructureStarts[esIndex] < tdb.paragraphs[p].getStartIndex()))
					esIndex++;
				
				//	check if paragraph starts existing structure
				if ((esIndex < existingStructure.length) && (existingStructureStarts[esIndex] == tdb.paragraphs[p].getStartIndex()))
					type = existingStructureTypes[esIndex];
				
				//	unclassified paragraph
				else type = UNCLASSIFIED_SUB_SUB_SECTION_TYPE;
				
				//	store category
				tdb.paragraphTypes[p] = type;
				relevantParagraphTypeList.add(type);
			}
		}
		
		//	assemble data
		tdb.relevantParagraphs = ((MutableAnnotation[]) relevantParagraphList.toArray(new MutableAnnotation[relevantParagraphList.size()]));
		tdb.relevantParagraphIndices = new int[relevantParagraphIndexList.size()];
		for (int i = 0; i < relevantParagraphIndexList.size(); i++)
			tdb.relevantParagraphIndices[i] = ((Integer) relevantParagraphIndexList.get(i)).intValue();
		tdb.relevantParagraphTypes = ((String[]) relevantParagraphTypeList.toArray(new String[relevantParagraphTypeList.size()]));
		
		//	make way for classification
		for (int p = 0; p < tdb.relevantParagraphTypes.length; p++)
			if (UNCLASSIFIED_SUB_SUB_SECTION_TYPE.equals(tdb.relevantParagraphTypes[p]))
				tdb.relevantParagraphTypes[p] = null;
		
		//	classify relevant paragraphs
		tdb.relevantParagraphTypes = this.rules.classifyAnnotations(tdb.relevantParagraphs, tdb.relevantParagraphTypes);
	}
	
	private void postprocessTreatment(TreatmentDataBox tdb) {
		
		//	feedback cancelled for this treatment
		if (tdb.feedbackRelevantParagraphTypes == null)
			return;
		
		
		//	process feedback
		for (int p = 0; p < tdb.feedbackRelevantParagraphTypes.length; p++) {
			
			//	transfer relevant paragraph categories to data on all paragraphs
			tdb.paragraphTypes[tdb.relevantParagraphIndices[p]] = tdb.feedbackRelevantParagraphTypes[p];
			
			//	TODO: remember corrections
			if (!tdb.relevantParagraphTypes[p].equals(tdb.feedbackRelevantParagraphTypes[p])) {
				
			}
		}
		
		
		//	fill artifact gaps
		int artifactBlockStart = -1;
		String beforeArtifactBlockType = null;
		for (int p = 0; p < tdb.paragraphTypes.length; p++) {
			
			//	artifact
			if (ARTIFACT_SUB_SUB_SECTION_TYPE.equals(tdb.paragraphTypes[p])) {
				
				//	first of possible series
				if (artifactBlockStart == -1) {
					artifactBlockStart = p;
					beforeArtifactBlockType = ((p == 0) ? null : tdb.paragraphTypes[p-1]);
				}
			}
			
			//	other paragraph directly after artifact block
			else if (artifactBlockStart != -1) {
				
				//	some sub section continues ==> continue through artifacts
				if (tdb.paragraphTypes[p].equals(beforeArtifactBlockType)) {
					for (int a = artifactBlockStart; a < p; a++)
						tdb.paragraphTypes[a] = tdb.paragraphTypes[p];
				}
				
				//	new sub section starts ==> make artifacts separate block
				else for (int a = artifactBlockStart; a < p; a++)
//					tdb.paragraphTypes[a] = multiple_SUB_SUB_SECTION_TYPE;
					tdb.paragraphTypes[a] = multiple_TYPE;
				
				//	remember artifact block is over
				artifactBlockStart = -1;
				beforeArtifactBlockType = null;
			}
		}
		
		//	handle terminal artifact block (if any)
		if (artifactBlockStart != -1) {
			for (int a = artifactBlockStart; a < tdb.paragraphTypes.length; a++)
//				tdb.paragraphTypes[a] = multiple_SUB_SUB_SECTION_TYPE;
				tdb.paragraphTypes[a] = multiple_TYPE;
		}
		
		
		//	collect existing annotations
		HashMap oldStructure = new HashMap();
		for (int s = 0; s < tdb.subSubSections.length; s++) {
			String key = (tdb.subSubSections[s].getType() + "-" + tdb.subSubSections[s].getStartIndex() + "-" + tdb.subSubSections[s].size());
			oldStructure.put(key, tdb.subSubSections[s]);
		}
		
		
		//	collect index and type information
		ArrayList sssTypeList = new ArrayList();
		ArrayList sssStartList = new ArrayList();
		for (int p = 0; p < tdb.paragraphTypes.length; p++)
			if ((p == 0) || !tdb.paragraphTypes[p].equals(tdb.paragraphTypes[p-1])) {
				sssTypeList.add(tdb.paragraphTypes[p]);
				sssStartList.add(new Integer(tdb.paragraphs[p].getStartIndex()));
			}
		sssStartList.add(new Integer(tdb.treatment.size()));
		
		//	translate selected types into annotations
		for (int s = 0; s < sssTypeList.size(); s++) {
			
			int start = ((Integer) sssStartList.get(s)).intValue();
			int size = (((Integer) sssStartList.get(s+1)).intValue() - start);
			String type = ((String) sssTypeList.get(s));
			String key = (type + "-" + start + "-" + size);
			Annotation structure;
			
			//	annotation exists, preserve it
			if (oldStructure.containsKey(key)) {
				structure = ((Annotation) oldStructure.remove(key));
				structure.setAttribute(TYPE_ATTRIBUTE, type);
				structure.setAttribute("_generate", "retained");
			}
			
			//	add new sub section
			else {
				structure = tdb.treatment.addAnnotation(SUB_SUB_SECTION_TYPE, start, size);
				if (structure != null) {
					structure.setAttribute(TYPE_ATTRIBUTE, type);
					structure.setAttribute("_generate", "added");
				}
			}
		}
		
		
		//	remove remaining old markup
		for (Iterator osit = oldStructure.values().iterator(); osit.hasNext();)
			tdb.treatment.removeAnnotation((Annotation) osit.next());
	}
	
	private void getFeedback(TreatmentDataBox[] tdbs) {
		
		//	assemble dialogs
		CategorizationFeedbackPanel[] cfps = new CategorizationFeedbackPanel[tdbs.length];
		for (int d = 0; d < cfps.length; d++) {
			cfps[d] = new CategorizationFeedbackPanel("Check Substructure of Treatments");
			cfps[d].setLabel("<HTML>Please select to which <B>data domain</B> (e.g. <I>nomenclature</I> or <I>description</I>) of the treatment these paragraphs belong.</HTML>");
			cfps[d].setChangeSpacing(15);
			cfps[d].setContinueSpacing(5);
			
			String[] categories = this.rules.getCategories();
			for (int c = 0; c < categories.length; c++) {
				cfps[d].addCategory(categories[c]);
				cfps[d].setCategoryColor(categories[c], getColorForType(categories[c]));
			}
			
			//	add paragraphs
			for (int p = 0; p < tdbs[d].relevantParagraphs.length; p++)
				cfps[d].addLine(tdbs[d].relevantParagraphs[p].getValue(), tdbs[d].relevantParagraphTypes[p]);
			
			//	add backgroung information
			cfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, tdbs[d].relevantParagraphs[0].getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			cfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, tdbs[d].relevantParagraphs[0].getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			cfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, tdbs[d].relevantParagraphs[0].getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			cfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, tdbs[d].treatment.getAnnotationID());
			cfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, SUB_SUB_SECTION_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString(tdbs[d].relevantParagraphs);
			if (targetPages != null)
				cfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString(tdbs[d].relevantParagraphs);
			if (targetPageIDs != null)
				cfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(cfps);
			
			//	process all feedback data together
			for (int d = 0; d < cfps.length; d++) {
				tdbs[d].feedbackRelevantParagraphTypes = new String[tdbs[d].relevantParagraphTypes.length];
				for (int p = 0; p < cfps[d].lineCount(); p++)
					tdbs[d].feedbackRelevantParagraphTypes[p] = cfps[d].getCategoryAt(p);
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
				tdbs[d].feedbackRelevantParagraphTypes = new String[tdbs[d].relevantParagraphTypes.length];
				for (int p = 0; p < cfps[d].lineCount(); p++)
					tdbs[d].feedbackRelevantParagraphTypes[p] = cfps[d].getCategoryAt(p);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog on
			else d = cfps.length;
		}
	}
	
	private HashMap typeColors = new HashMap();
	private Color getColorForType(String type) {
		Color color = ((Color) this.typeColors.get(type));
		if (color != null)
			return color;
		
		if (biology_ecology_TYPE.equals(type))
			return Color.GREEN;
		else if (description_TYPE.equals(type))
			return Color.YELLOW;
		else if (diagnosis_TYPE.equals(type))
			return Color.PINK;
		else if (discussion_TYPE.equals(type))
			return Color.RED;
		else if (distribution_TYPE.equals(type))
			return Color.BLUE;
		else if (etymology_TYPE.equals(type))
			return Color.MAGENTA;
		else if (key_TYPE.equals(type))
			return Color.WHITE;
		else if (materials_examined_TYPE.equals(type))
			return Color.CYAN;
		else if (nomenclature_TYPE.equals(type))
			return Color.ORANGE;
		else if (reference_group_TYPE.equals(type))
			return Color.LIGHT_GRAY;
		else if (synonymic_list_OPTION.equals(type))
			return Color.ORANGE.brighter();
		else return Color.GRAY;
	}
//	private Color getColorForType(String type) {
//		Color color = ((Color) this.typeColors.get(type));
//		if (color != null)
//			return color;
//		
//		if (biology_ecology_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.GREEN;
//		else if (description_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.YELLOW;
//		else if (diagnosis_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.PINK;
//		else if (discussion_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.RED;
//		else if (distribution_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.BLUE;
//		else if (etymology_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.MAGENTA;
//		else if (key_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.WHITE;
//		else if (materials_examined_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.CYAN;
//		else if (nomenclature_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.ORANGE;
//		else if (reference_group_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.LIGHT_GRAY;
//		else if (synonymic_list_OPTION.equals(type))
//			return Color.ORANGE.brighter();
//		else return Color.GRAY;
//	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#configureProcessor()
	 */
	public void configureProcessor() {
		this.rulesModified = (this.rulesModified | this.rules.editRules());
	}
}