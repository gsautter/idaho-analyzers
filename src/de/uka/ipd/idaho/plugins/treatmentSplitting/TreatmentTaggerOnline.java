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
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.StartContinueFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.RuleBox;
import de.uka.ipd.idaho.gamta.util.swing.RuleBox.Rule;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This analyzer produces an overlay of treatments and other subSections over
 * the paragraphs of a document. It uses XPath-based rules to pre-classify the
 * paragraphs, and (if allowed so) subsequently displays them in a dialog for
 * correction.
 * 
 * @author sautter
 */
public class TreatmentTaggerOnline extends AbstractConfigurableAnalyzer implements TreatmentConstants {
	
	//	universal categories
	private static final String TREATMENT_CATEGORY = "Start '" + TREATMENT_ANNOTATION_TYPE + "' SubSection";
	
	private static final String CONTINUE_CATEGORY = "Continue SubSection";
	private static final String ARTIFACT_CATEGORY = "Artifact";
	
	//	categories for simple rules
	private static final String OTHER_CATEGORY = "Start Other SubSection";
	
	private static final String[] SIMPLE_CATEGORIES = {
		TREATMENT_CATEGORY,
		OTHER_CATEGORY,
		CONTINUE_CATEGORY
	};
	
//	
//	//	subSection types for complex rules
//	private static final String abstract_SUB_SECTION_TYPE = "abstract";
//	private static final String acknowledgments_SUB_SECTION_TYPE = "acknowledgments";
//	private static final String document_head_SUB_SECTION_TYPE = "document_head";
//	private static final String introduction_SUB_SECTION_TYPE = "introduction";
//	private static final String key_SUB_SECTION_TYPE = "key";
//	private static final String materials_methods_SUB_SECTION_TYPE = "materials_methods";
//	private static final String multiple_SUB_SECTION_TYPE = "multiple";
//	private static final String reference_group_SUB_SECTION_TYPE = "reference_group";
//	private static final String synopsis_SUB_SECTION_TYPE = "synopsis";
//	private static final String synonymic_list_SUB_SECTION_TYPE = "synonymic_list";
//	private static final String taxon_list_SUB_SECTION_TYPE = "taxon_list"; // TODO make sure this gets transformed to valid TaxonX
//	
//	//	categories for complex rules
//	private static final String ABSTRACT_CATEGORY = "Start '" + abstract_SUB_SECTION_TYPE + "' SubSection";
//	private static final String ACKNOWLEDGMENTS_CATEGORY = "Start '" + acknowledgments_SUB_SECTION_TYPE + "' SubSection";
//	private static final String DOCUMENT_HEAD_CATEGORY = "Start '" + document_head_SUB_SECTION_TYPE + "' SubSection";
//	private static final String INTRODUCTION_CATEGORY = "Start '" + introduction_SUB_SECTION_TYPE + "' SubSection";
//	private static final String KEY_CATEGORY = "Start '" + key_SUB_SECTION_TYPE + "' SubSection";
//	private static final String MATERIALS_METHODS_CATEGORY = "Start '" + materials_methods_SUB_SECTION_TYPE + "' SubSection";
//	private static final String MULTIPLE_CATEGORY = "Start '" + multiple_SUB_SECTION_TYPE + "' SubSection";
//	private static final String REFERENCE_GROUP_CATEGORY = "Start '" + reference_group_SUB_SECTION_TYPE + "' SubSection";
//	private static final String SYNOPSIS_CATEGORY = "Start '" + synopsis_SUB_SECTION_TYPE + "' SubSection";
//	private static final String SYNONYMIC_LIST_CATEGORY = "Start '" + synonymic_list_SUB_SECTION_TYPE + "' SubSection";
//	private static final String TAXON_LIST_CATEGORY = "Start '" + taxon_list_SUB_SECTION_TYPE + "' SubSection"; // TODO make sure this gets transformed to valid TaxonX
	
	//	categories for complex rules
	private static final String ABSTRACT_CATEGORY = "Start '" + abstract_TYPE + "' SubSection";
	private static final String ACKNOWLEDGMENTS_CATEGORY = "Start '" + acknowledgments_TYPE + "' SubSection";
	private static final String DOCUMENT_HEAD_CATEGORY = "Start '" + document_head_TYPE + "' SubSection";
	private static final String INTRODUCTION_CATEGORY = "Start '" + introduction_TYPE + "' SubSection";
	private static final String KEY_CATEGORY = "Start '" + key_TYPE + "' SubSection";
	private static final String MATERIALS_METHODS_CATEGORY = "Start '" + materials_methods_TYPE + "' SubSection";
	private static final String MULTIPLE_CATEGORY = "Start '" + multiple_TYPE + "' SubSection";
	private static final String REFERENCE_GROUP_CATEGORY = "Start '" + reference_group_TYPE + "' SubSection";
	private static final String SYNOPSIS_CATEGORY = "Start '" + synopsis_TYPE + "' SubSection";
	private static final String SYNONYMIC_LIST_CATEGORY = "Start '" + synonymic_list_TYPE + "' SubSection";
	private static final String TAXON_LIST_CATEGORY = "Start '" + taxon_list_TYPE + "' SubSection"; // TODO make sure this gets transformed to valid TaxonX
	
	private static final String[] COMPLEX_CATEGORIES = {
		ABSTRACT_CATEGORY,
		ACKNOWLEDGMENTS_CATEGORY,
		DOCUMENT_HEAD_CATEGORY,
		INTRODUCTION_CATEGORY,
		KEY_CATEGORY,
		MATERIALS_METHODS_CATEGORY,
		MULTIPLE_CATEGORY,
		REFERENCE_GROUP_CATEGORY,
		SYNOPSIS_CATEGORY,
		SYNONYMIC_LIST_CATEGORY,
		TAXON_LIST_CATEGORY, // TODO make sure this gets transformed to valid TaxonX
		TREATMENT_CATEGORY,
		CONTINUE_CATEGORY,
	};
//	
//	private static String getTypeForCategory(String category) {
//		if (ABSTRACT_CATEGORY.equals(category)) return abstract_SUB_SECTION_TYPE;
//		else if (ACKNOWLEDGMENTS_CATEGORY.equals(category)) return acknowledgments_SUB_SECTION_TYPE;
//		else if (DOCUMENT_HEAD_CATEGORY.equals(category)) return document_head_SUB_SECTION_TYPE;
//		else if (INTRODUCTION_CATEGORY.equals(category)) return introduction_SUB_SECTION_TYPE;
//		else if (KEY_CATEGORY.equals(category)) return key_SUB_SECTION_TYPE;
//		else if (MATERIALS_METHODS_CATEGORY.equals(category)) return materials_methods_SUB_SECTION_TYPE;
//		else if (MULTIPLE_CATEGORY.equals(category)) return multiple_SUB_SECTION_TYPE;
//		else if (REFERENCE_GROUP_CATEGORY.equals(category)) return reference_group_SUB_SECTION_TYPE;
//		else if (SYNOPSIS_CATEGORY.equals(category)) return synopsis_SUB_SECTION_TYPE;
//		else if (SYNONYMIC_LIST_CATEGORY.equals(category)) return synonymic_list_SUB_SECTION_TYPE;
//		else if (TAXON_LIST_CATEGORY.equals(category)) return taxon_list_SUB_SECTION_TYPE;
//		else if (TREATMENT_CATEGORY.equals(category)) return TREATMENT_ANNOTATION_TYPE;
//		else return multiple_SUB_SECTION_TYPE;
//	}
//	
//	private static String getCategoryForType(String type) {
//		if (abstract_SUB_SECTION_TYPE.equals(type)) return ABSTRACT_CATEGORY;
//		else if (acknowledgments_SUB_SECTION_TYPE.equals(type)) return ACKNOWLEDGMENTS_CATEGORY;
//		else if (document_head_SUB_SECTION_TYPE.equals(type)) return DOCUMENT_HEAD_CATEGORY;
//		else if (introduction_SUB_SECTION_TYPE.equals(type)) return INTRODUCTION_CATEGORY;
//		else if (key_SUB_SECTION_TYPE.equals(type)) return KEY_CATEGORY;
//		else if (materials_methods_SUB_SECTION_TYPE.equals(type)) return MATERIALS_METHODS_CATEGORY;
//		else if (multiple_SUB_SECTION_TYPE.equals(type)) return MULTIPLE_CATEGORY;
//		else if (reference_group_SUB_SECTION_TYPE.equals(type)) return REFERENCE_GROUP_CATEGORY;
//		else if (synopsis_SUB_SECTION_TYPE.equals(type)) return SYNOPSIS_CATEGORY;
//		else if (synonymic_list_SUB_SECTION_TYPE.equals(type)) return SYNONYMIC_LIST_CATEGORY;
//		else if (taxon_list_SUB_SECTION_TYPE.equals(type)) return TAXON_LIST_CATEGORY;
//		else if (TREATMENT_ANNOTATION_TYPE.equals(type)) return TREATMENT_CATEGORY;
//		else return MULTIPLE_CATEGORY;
//	}
	
	private static String getTypeForCategory(String category) {
		if (ABSTRACT_CATEGORY.equals(category)) return abstract_TYPE;
		else if (ACKNOWLEDGMENTS_CATEGORY.equals(category)) return acknowledgments_TYPE;
		else if (DOCUMENT_HEAD_CATEGORY.equals(category)) return document_head_TYPE;
		else if (INTRODUCTION_CATEGORY.equals(category)) return introduction_TYPE;
		else if (KEY_CATEGORY.equals(category)) return key_TYPE;
		else if (MATERIALS_METHODS_CATEGORY.equals(category)) return materials_methods_TYPE;
		else if (MULTIPLE_CATEGORY.equals(category)) return multiple_TYPE;
		else if (REFERENCE_GROUP_CATEGORY.equals(category)) return reference_group_TYPE;
		else if (SYNOPSIS_CATEGORY.equals(category)) return synopsis_TYPE;
		else if (SYNONYMIC_LIST_CATEGORY.equals(category)) return synonymic_list_TYPE;
		else if (TAXON_LIST_CATEGORY.equals(category)) return taxon_list_TYPE;
		else if (TREATMENT_CATEGORY.equals(category)) return TREATMENT_ANNOTATION_TYPE;
		else return multiple_TYPE;
	}
	
	private static String getCategoryForType(String type) {
		if (abstract_TYPE.equals(type)) return ABSTRACT_CATEGORY;
		else if (acknowledgments_TYPE.equals(type)) return ACKNOWLEDGMENTS_CATEGORY;
		else if (document_head_TYPE.equals(type)) return DOCUMENT_HEAD_CATEGORY;
		else if (introduction_TYPE.equals(type)) return INTRODUCTION_CATEGORY;
		else if (key_TYPE.equals(type)) return KEY_CATEGORY;
		else if (materials_methods_TYPE.equals(type)) return MATERIALS_METHODS_CATEGORY;
		else if (multiple_TYPE.equals(type)) return MULTIPLE_CATEGORY;
		else if (reference_group_TYPE.equals(type)) return REFERENCE_GROUP_CATEGORY;
		else if (synopsis_TYPE.equals(type)) return SYNOPSIS_CATEGORY;
		else if (synonymic_list_TYPE.equals(type)) return SYNONYMIC_LIST_CATEGORY;
		else if (taxon_list_TYPE.equals(type)) return TAXON_LIST_CATEGORY;
		else if (TREATMENT_ANNOTATION_TYPE.equals(type)) return TREATMENT_CATEGORY;
		else return MULTIPLE_CATEGORY;
	}
	
	private static final String SIMPLE_RULE_DATA_NAME = "treatmentTaggerRules.simple.txt";
	private static final String COMPLEX_RULE_DATA_NAME = "treatmentTaggerRules.complex.txt";
	
	private StringVector numberPrefixes;
	private StringVector artifactAnnotationTypes = new StringVector();
	
	private RuleBox rules;
	private boolean useComplexRules = true;
	private boolean rulesModified = false;
	
	private int maxDialogParagraphs = 20;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load number prefixes
		try {
			InputStream is = this.dataProvider.getInputStream("numberPrefixes.txt");
			this.numberPrefixes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {
			this.numberPrefixes = new StringVector();
			this.numberPrefixes.addElement("No");
		}
		
		//	load artifact div types
		try {
			InputStream is = this.dataProvider.getInputStream("artifactAnnotationTypes.txt");
			this.artifactAnnotationTypes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
		
		
		//	which rule set to use?
		this.useComplexRules = "complex".equals(this.getParameter("ruleSet", "simple"));
		
		
		//	create rule box
		this.rules = new RuleBox((this.useComplexRules ? COMPLEX_CATEGORIES : SIMPLE_CATEGORIES), CONTINUE_CATEGORY);
		try {
			InputStream is = this.dataProvider.getInputStream(this.useComplexRules ? COMPLEX_RULE_DATA_NAME : SIMPLE_RULE_DATA_NAME);
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
		
		
		//	how many paragraphs to display per dialog?
		try {
			this.maxDialogParagraphs = Integer.parseInt(this.getParameter("maxDialogParagraphs", ("" + this.maxDialogParagraphs)));
		} catch (NumberFormatException nfe) {}
	}
	
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		
		//	test if configuration changed
		if (this.rulesModified) {
			
			//	collect string representation of rules
			StringVector ruleStrings = new StringVector();
			Rule[] rules = this.rules.getRules();
			for (int r = 0; r < rules.length; r++)
				ruleStrings.addElement(rules[r].toDataString());
			
			//	store rules
			try {
				OutputStream os = dataProvider.getOutputStream(this.useComplexRules ? COMPLEX_RULE_DATA_NAME : SIMPLE_RULE_DATA_NAME);
				ruleStrings.storeContent(os);
				os.flush();
				os.close();
			} catch (IOException ioe) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get paragraphs first (check if something to classify)
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(PARAGRAPH_TYPE);
		if (paragraphs.length == 0) return;
		
		//	get existing treatments and non-treatment subSections
		ArrayList existingStructureList = new ArrayList();
		
		//	add existing treatments
		Annotation[] treatments = data.getAnnotations(TREATMENT_ANNOTATION_TYPE);
		for (int t = 0; t < treatments.length; t++)
			existingStructureList.add(treatments[t]);
		
		//	add existing sections of other types
		Annotation[] nonTreatments = data.getAnnotations(SUB_SECTION_TYPE);
		for (int nt = 0; nt < nonTreatments.length; nt++)
			existingStructureList.add(nonTreatments[nt]);
		
		//	sort existing sections
		Annotation[] existingStructure = ((Annotation[]) existingStructureList.toArray(new Annotation[existingStructureList.size()]));
		Arrays.sort(existingStructure, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	fill gaps with "multiple" subSections
		int lastEnd = 0;
		existingStructureList.clear();
		for (int e = 0; e < existingStructure.length; e++) {
			if (lastEnd < existingStructure[e].getStartIndex()) {
				Annotation fillingNonTreatment = Gamta.newAnnotation(data, SUB_SECTION_TYPE, lastEnd, (existingStructure[e].getStartIndex() - lastEnd));
//				fillingNonTreatment.setAttribute(TYPE_ATTRIBUTE, multiple_SUB_SECTION_TYPE);
				fillingNonTreatment.setAttribute(TYPE_ATTRIBUTE, multiple_TYPE);
				existingStructureList.add(fillingNonTreatment);
			}
			existingStructureList.add(existingStructure[e]);
		}
		if (lastEnd < data.size()) {
			Annotation fillingNonTreatment = Gamta.newAnnotation(data, SUB_SECTION_TYPE, lastEnd, (data.size() - lastEnd));
//			fillingNonTreatment.setAttribute(TYPE_ATTRIBUTE, multiple_SUB_SECTION_TYPE);
			fillingNonTreatment.setAttribute(TYPE_ATTRIBUTE, multiple_TYPE);
			existingStructureList.add(fillingNonTreatment);
		}
		existingStructure = ((Annotation[]) existingStructureList.toArray(new Annotation[existingStructureList.size()]));
		
		//	get indices and types
		int[] existingStructureStarts = new int[existingStructure.length];
		String[] existingStructureCategories = new String[existingStructure.length];
		for (int e = 0; e < existingStructure.length; e++) {
			existingStructureStarts[e] = existingStructure[e].getStartIndex();
			existingStructureCategories[e] = existingStructure[e].getType();
			if (TREATMENT_ANNOTATION_TYPE.equals(existingStructureCategories[e]))
				existingStructureCategories[e] = TREATMENT_CATEGORY;
//			else existingStructureCategories[e] = (this.useComplexRules ? getCategoryForType(existingStructure[e].getAttribute(TYPE_ATTRIBUTE, multiple_SUB_SECTION_TYPE).toString()) : OTHER_CATEGORY);
			else existingStructureCategories[e] = (this.useComplexRules ? getCategoryForType(existingStructure[e].getAttribute(TYPE_ATTRIBUTE, multiple_TYPE).toString()) : OTHER_CATEGORY);
		}
		
		
		//	initialize data structures
		String[] paragraphCategories = new String[paragraphs.length];
		ArrayList relevantParagraphList = new ArrayList();
		ArrayList relevantParagraphPageNumberList = new ArrayList();
		ArrayList relevantParagraphIndexList = new ArrayList();
		ArrayList relevantParagraphCategoryList = new ArrayList();
		
		//	get paragraphs nested in multi-paragraph artifacts
		HashSet artifactParagraphIDs = new HashSet();
		for (int t = 0; t < this.artifactAnnotationTypes.size(); t++) {
			MutableAnnotation[] artifacts = data.getMutableAnnotations(this.artifactAnnotationTypes.get(t));
			for (int a = 0; a < artifacts.length; a++) {
				Annotation[] artifactParagraphs = artifacts[a].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < artifactParagraphs.length; p++)
					artifactParagraphIDs.add(artifactParagraphs[p].getAnnotationID());
			}
		}
		
		//	pre-classify paragraphs, and sort out artifacts
		int esIndex = 0;
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	check if layout artifact
			boolean isArtifact = artifactParagraphIDs.contains(paragraphs[p].getAnnotationID());
			for (int a = 0; !isArtifact && (a < this.artifactAnnotationTypes.size()); a++) {
				Annotation[] artifacts = paragraphs[p].getAnnotations(this.artifactAnnotationTypes.get(a));
				if ((artifacts.length != 0) && (artifacts[0].size() == paragraphs[p].size()))
					isArtifact = true;
			}
			
			//	layout artifact, exclude from dialog
			if (isArtifact)
				paragraphCategories[p] = ARTIFACT_CATEGORY;
				
			//	determine type of non-artifact paragraph
			else {
				relevantParagraphList.add(paragraphs[p]);
				relevantParagraphPageNumberList.add(new Integer(paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE, "-1").toString()));
				relevantParagraphIndexList.add(new Integer(p));
				String category; // do not initialize so compiler ensures meaningful initialization
				
				//	jump to next start of existing structure
				while ((esIndex < existingStructureStarts.length) && (existingStructureStarts[esIndex] < paragraphs[p].getStartIndex()))
					esIndex++;
				
				//	check if paragraph starts existing structure
				if ((esIndex < existingStructure.length) && (existingStructureStarts[esIndex] == paragraphs[p].getStartIndex()))
					category = existingStructureCategories[esIndex];
				
				//	unclassified paragraph
				else category = CONTINUE_CATEGORY;
				
				//	store category
				paragraphCategories[p] = category;
				relevantParagraphCategoryList.add(category);
			}
		}
		
		//	assemble data
		MutableAnnotation[] relevantParagraphs = ((MutableAnnotation[]) relevantParagraphList.toArray(new MutableAnnotation[relevantParagraphList.size()]));
		int[] relevantParagraphPageNumbers = new int[relevantParagraphPageNumberList.size()];
		for (int i = 0; i < relevantParagraphPageNumberList.size(); i++)
			relevantParagraphPageNumbers[i] = ((Integer) relevantParagraphPageNumberList.get(i)).intValue();
		int[] relevantParagraphIndices = new int[relevantParagraphIndexList.size()];
		for (int i = 0; i < relevantParagraphIndexList.size(); i++)
			relevantParagraphIndices[i] = ((Integer) relevantParagraphIndexList.get(i)).intValue();
		String[] relevantParagraphCategories = ((String[]) relevantParagraphCategoryList.toArray(new String[relevantParagraphCategoryList.size()]));
		
		//	make way for classification
		for (int p = 0; p < relevantParagraphCategories.length; p++)
			if (CONTINUE_CATEGORY.equals(relevantParagraphCategories[p]))
				relevantParagraphCategories[p] = null;
		
		//	classify relevant paragraphs
		relevantParagraphCategories = this.rules.classifyAnnotations(relevantParagraphs, relevantParagraphCategories);
		
		//	get feedback if allowed to
		if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
			String[] feedbackRelevantParagraphCategories = this.getFeedback(relevantParagraphs, relevantParagraphPageNumbers, relevantParagraphCategories, data);
			
			//	process feedback
			for (int p = 0; p < feedbackRelevantParagraphCategories.length; p++) {
				
				//	transfer relevant paragraph categories to data on all paragraphs
				paragraphCategories[relevantParagraphIndices[p]] = feedbackRelevantParagraphCategories[p];
				
				//	TODO: remember corrections
				if (!relevantParagraphCategories[p].equals(feedbackRelevantParagraphCategories[p])) {
					
				}
			}
		}
		
		//	transfer relevant paragraph categories to data on all paragraphs
		else for (int p = 0; p < relevantParagraphCategories.length; p++)
			paragraphCategories[relevantParagraphIndices[p]] = relevantParagraphCategories[p];
		
		//	fill artifact gaps
		int artifactBlockStart = -1;
		for (int p = 0; p < paragraphCategories.length; p++) {
			
			//	artifact
			if (ARTIFACT_CATEGORY.equals(paragraphCategories[p])) {
				
				//	first of possible series
				if (artifactBlockStart == -1)
					artifactBlockStart = p;
			}
			
			//	other paragraph directly after artifact block
			else if (artifactBlockStart != -1) {
				
				//	some section continues ==> continue through artifacts
				if (CONTINUE_CATEGORY.equals(paragraphCategories[p])) {
					for (int a = artifactBlockStart; a < p; a++)
						paragraphCategories[a] = CONTINUE_CATEGORY;
				}
				
				//	new section starts ==> make artifacts separate block
				else {
					paragraphCategories[artifactBlockStart] = (this.useComplexRules ? MULTIPLE_CATEGORY : OTHER_CATEGORY);
					for (int a = (artifactBlockStart + 1); a < p; a++)
						paragraphCategories[a] = CONTINUE_CATEGORY;
				}
				
				//	remember artifact block is over
				artifactBlockStart = -1;
			}
		}
		
		//	handle terminal artifact block (if any)
		if (artifactBlockStart != -1) {
			paragraphCategories[artifactBlockStart] = (this.useComplexRules ? MULTIPLE_CATEGORY : OTHER_CATEGORY);
			for (int a = (artifactBlockStart + 1); a < paragraphCategories.length; a++)
				paragraphCategories[a] = CONTINUE_CATEGORY;
		}
		
		
		//	collect existing annotations
		HashMap oldStructure = new HashMap();
		for (int t = 0; t < treatments.length; t++) {
			String key = (treatments[t].getType() + "-" + treatments[t].getStartIndex() + "-" + treatments[t].size());
			oldStructure.put(key, treatments[t]);
		}
		for (int nt = 0; nt < nonTreatments.length; nt++) {
			String key = (nonTreatments[nt].getType() + "-" + nonTreatments[nt].getStartIndex() + "-" + nonTreatments[nt].size());
			oldStructure.put(key, nonTreatments[nt]);
		}
		
		//	translate selected categories into annotations
		int startIndex = paragraphs[0].getStartIndex();
		String type = null;
		String subSectionType = null;
		for (int p = 0; p < paragraphCategories.length; p++) {
			
			//	start of some section
			if (!CONTINUE_CATEGORY.equals(paragraphCategories[p])) {
				
				//	add annotation ending here (if any)
				if (startIndex < paragraphs[p].getStartIndex()) {
					int size = (paragraphs[p].getStartIndex() - startIndex);
					String key = (type + "-" + startIndex + "-" + size);
					Annotation structure;
					
					//	annotation exists, preserve it
					if (oldStructure.containsKey(key)) {
						structure = ((Annotation) oldStructure.remove(key));
						if (subSectionType != null)
							structure.setAttribute(TYPE_ATTRIBUTE, subSectionType);
						structure.setAttribute("_generate", "retained");
					}
					
					//	add new subSection
					else if (type != null) {
						structure = data.addAnnotation(type, startIndex, size);
						if (structure != null) {
							if (subSectionType != null)
								structure.setAttribute(TYPE_ATTRIBUTE, subSectionType);
							structure.setAttribute("_generate", "added");
						}
					}
				}
				
				//	store start index and type for starting subSection
				startIndex = paragraphs[p].getStartIndex();
				
				//	start of a treatment
				if (TREATMENT_CATEGORY.equals(paragraphCategories[p])) {
					type = TREATMENT_ANNOTATION_TYPE;
					subSectionType = null;
				}
				
				//	start of non-treatment subSection
				else {
					type = SUB_SECTION_TYPE;
//					subSectionType = (this.useComplexRules ? getTypeForCategory(paragraphCategories[p]) : multiple_SUB_SECTION_TYPE);
					subSectionType = (this.useComplexRules ? getTypeForCategory(paragraphCategories[p]) : multiple_TYPE);
				}
			}
		}
		
		//	add last annotation (if any)
		if (startIndex < data.size()) {
			int size = (data.size() - startIndex);
			String key = (type + "-" + startIndex + "-" + size);
			Annotation structure;
			
			//	annotation exists, preserve it
			if (oldStructure.containsKey(key)) {
				structure = ((Annotation) oldStructure.remove(key));
				if (subSectionType != null)
					structure.setAttribute(TYPE_ATTRIBUTE, subSectionType);
				structure.setAttribute("_generate", "retained");
			}
			
			//	add new subSection
			else if (type != null) {
				structure = data.addAnnotation(type, startIndex, size);
				if (structure != null) {
					if (subSectionType != null)
						structure.setAttribute(TYPE_ATTRIBUTE, subSectionType);
					structure.setAttribute("_generate", "added");
				}
			}
		}
		
		//	remove remaining old markup
		for (Iterator osit = oldStructure.values().iterator(); osit.hasNext();)
			data.removeAnnotation((Annotation) osit.next());
	}
	
	private String[] getFeedback(MutableAnnotation[] relevantParagraphs, int[] relevantParagraphPageNumbers, String[] relevantParagraphCategories, MutableAnnotation context) {
		System.out.println("Building feedback panels from paragraphs:");
		for (int p = 0; p < relevantParagraphs.length; p++)
			System.out.println("  " + p + ". " + relevantParagraphs[p].firstValue() + " " + relevantParagraphPageNumbers[p]);
		
		/*
		 * compute block offsets - if paragraphs have page numbers, add the
		 * paragraphs of two pages to each dialog, overlapping by one
		 * page; if no page numbers are given, use about 20 paragraphs per
		 * dialog, overlapping by about 10 paragraphs
		 */
		int[] scfpBoundaries;
		
		//	no limit for paragraphs per dialog
		if (this.maxDialogParagraphs <= 10) {
			scfpBoundaries = new int[3];
			scfpBoundaries[0] = 0;
			scfpBoundaries[1] = (relevantParagraphs.length / 2);
			scfpBoundaries[2] = relevantParagraphs.length;
		}
		
		//	no page number, or document with one page
		else if (relevantParagraphPageNumbers[0] == relevantParagraphPageNumbers[relevantParagraphPageNumbers.length - 1]) {
			
			//	minimum of two blocks, so a dialog has always two to cover
			int blockCount = Math.max(2, ((relevantParagraphs.length + ((this.maxDialogParagraphs / 2) - 1)) / (this.maxDialogParagraphs / 2)));
			
			//	compute boundaries
			scfpBoundaries = new int[blockCount + 1];
			for (int d = 0; d <= blockCount; d++)
				scfpBoundaries[d] = ((d * relevantParagraphs.length) / blockCount);
		}
		
		//	page numbers given
		else {
			
//			minimum of two pages, so a dialog has always two to cover
//			int pageCount = Math.max(2, (relevantParagraphPageNumbers[relevantParagraphPageNumbers.length - 1] - relevantParagraphPageNumbers[0] + 1));
			
			//	count pages (empty ones do NOT count, so difference of first and last page number does not help)
			int pageCount = 0;
			int lastPageNumber = -1;
			for (int p = 0; p < relevantParagraphPageNumbers.length; p++) {
				if (relevantParagraphPageNumbers[p] != lastPageNumber) {
					pageCount++;
					lastPageNumber = relevantParagraphPageNumbers[p];
				}
			}
			
			//	minimum of two pages, so a dialog has always two to cover
			pageCount = Math.max(2, pageCount);
			
			System.out.println("Computing boundaries for " + pageCount + " pages:");
			
			//	compute boundaries
			scfpBoundaries = new int[pageCount + 1];
			int pageIndex = 0;
			for (int p = 0; p < relevantParagraphPageNumbers.length; p++) {
				if (((p == 0) || (relevantParagraphPageNumbers[p-1] != relevantParagraphPageNumbers[p])) && (pageIndex < scfpBoundaries.length)) {
					System.out.println("  " + pageIndex + " at " + p + ": " + relevantParagraphs[p].firstValue() + " " + relevantParagraphPageNumbers[p]);
					scfpBoundaries[pageIndex++] = p;
				}
			}
			scfpBoundaries[pageCount] = relevantParagraphs.length;
		}
		
		//	assemble dialogs
		StartContinueFeedbackPanel[] scfps = new StartContinueFeedbackPanel[scfpBoundaries.length - 2];
		for (int d = 0; d < scfps.length; d++) {
			scfps[d] = new StartContinueFeedbackPanel("Check Document Structure and Treatments");
			scfps[d].setLabel("<HTML>Please check for each paragraph if it starts a new <B>Treatment</B> or a new <B>SubSection</B> of a different type, or if it continues the previous one." +
					"<BR>If the top paragraph continues a Treatment or SubSection started on the previous page, just leave its state a continuing Treatment or SubSection of the previous paragraph." +
					"<BR>The background color of such paragraphs will remain white, but this does not prevent them from being properly assigned to a Treatment or SubSection.</HTML>");
			scfps[d].setContinueCategory(CONTINUE_CATEGORY);
			
			String[] categories = this.rules.getCategories();
			for (int c = 0; c < categories.length; c++)
				if (!CONTINUE_CATEGORY.equals(categories[c])) {
					scfps[d].addCategory(categories[c]);
					scfps[d].setCategoryColor(categories[c], (TREATMENT_CATEGORY.equals(categories[c]) ? Color.GREEN : Color.GRAY));
				}
			
			scfps[d].setChangeSpacing(15);
			scfps[d].setContinueSpacing(5);
			
			boolean noStartCategoryYet = true;
			for (int p = scfpBoundaries[d]; p < scfpBoundaries[d+2]; p++) {
				
				//	compute local category (start of section may be before start of feedback panel)
				String category = relevantParagraphCategories[p];
				
				//	got start category
				if (!CONTINUE_CATEGORY.equals(category))
					noStartCategoryYet = false;
				
				//	no starting category so far in current dialog
				else if (noStartCategoryYet) {
					
					//	search backward
					for (int c = (p-1); c >= 0; c--)
						if (!CONTINUE_CATEGORY.equals(relevantParagraphCategories[c])) {
							category = relevantParagraphCategories[c];
							noStartCategoryYet = false;
							c = -1;
						}
					
					//	still no starting category found (can happen especially in first dialog)
					if (noStartCategoryYet) {
						category = (this.useComplexRules ? MULTIPLE_CATEGORY : OTHER_CATEGORY);
						noStartCategoryYet = false;
					}
				}
				
				//	add paragraph to current dialog
//				scfps[d].addLine(("<HTML>" + relevantParagraphs[p].getValue() + " <B>(page&nbsp;" + relevantParagraphPageNumbers[p] + ")</B></HTML>"), category);
				scfps[d].addLine(("<HTML>" + this.buildParagraphLabel(relevantParagraphs[p]) + " <B>(page&nbsp;" + relevantParagraphPageNumbers[p] + ")</B></HTML>"), category);
			}
			
			//	add backgroung information
			scfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, relevantParagraphs[scfpBoundaries[d]].getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			scfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, relevantParagraphs[scfpBoundaries[d]].getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			scfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, relevantParagraphs[scfpBoundaries[d]].getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			scfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, context.getAnnotationID());
			scfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, TREATMENT_ANNOTATION_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString(relevantParagraphs, scfpBoundaries[d], scfpBoundaries[d+2]);
			if (targetPages != null)
				scfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString(relevantParagraphs, scfpBoundaries[d], scfpBoundaries[d+2]);
			if (targetPageIDs != null)
				scfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		
		//	get feedback
		String[] feedbackRelevantParagraphCategories = new String[relevantParagraphCategories.length];
		int cutoffParagraph = feedbackRelevantParagraphCategories.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(scfps);
			
			//	process all feedback data together
			for (int d = 0; d < scfps.length; d++) {
				
				//	read data from each dialog
				for (int p = scfpBoundaries[d]; p < scfpBoundaries[d+2]; p++) {
					
					//	get (local) category for current paragraph
					String category = scfps[d].getCategoryAt(p - scfpBoundaries[d]);
					
					//	after overlap with previous dialog (previous category overrules in order to annul artificial starts)
					if ((d == 0) || (p >= scfpBoundaries[d + 1]))
						feedbackRelevantParagraphCategories[p] = category;
				}
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else {
			String[] lastStartCategories = new String[scfps.length];
			
			for (int d = 0; d < scfps.length; d++) {
				if (d != 0)
					scfps[d].addButton("Previous");
				scfps[d].addButton("Cancel");
				scfps[d].addButton("OK" + (((d+1) == scfps.length) ? "" : " & Next"));
				
				String title = scfps[d].getTitle();
				scfps[d].setTitle(title + " - (" + (d+1) + " of " + scfps.length + ")");
				
				String f = scfps[d].getFeedback();
				if (f == null) f = "Cancel";
				
				scfps[d].setTitle(title);
				
				//	current dialog submitted, process data
				if (f.startsWith("OK")) {
					
					//	reset last start
					if (d != 0)
						lastStartCategories[d] = lastStartCategories[d-1];
					
					//	read data from each dialog
					for (int p = scfpBoundaries[d]; p < scfpBoundaries[d+2]; p++) {
						
						//	get (local) category for current paragraph
						String category = scfps[d].getCategoryAt(p - scfpBoundaries[d]);
						
						//	remember last start not shown in next dialog (for continuing appropriately)
						if ((p < scfpBoundaries[d + 1]) && !CONTINUE_CATEGORY.equals(category))
							lastStartCategories[d] = category;
						
						//	after overlap with previous dialog (previous category overrules in order to annul artificial starts)
						if ((d == 0) || (p >= scfpBoundaries[d + 1]))
							feedbackRelevantParagraphCategories[p] = category;
						
						//	transfer input of overlapping block to next dialog
						if (((d+1) < scfps.length) && (p >= scfpBoundaries[d + 1])) {
							if (p == scfpBoundaries[d + 1]) {
								scfps[d+1].setDefaultColor((lastStartCategories[d] == null) ? Color.WHITE : (TREATMENT_CATEGORY.equals(lastStartCategories[d]) ? Color.GREEN : Color.GRAY));
								scfps[d+1].setCategoryAt((p - scfpBoundaries[d + 1]), (TREATMENT_CATEGORY.equals(category) ? CONTINUE_CATEGORY : TREATMENT_CATEGORY));
							}
							scfps[d+1].setCategoryAt((p - scfpBoundaries[d + 1]), category);
						}
					}
				}
				
				//	back to previous dialog
				else if ("Previous".equals(f))
					d-=2;
				
				//	cancel from current dialog on
				else {
					cutoffParagraph = (d * scfpBoundaries[d+2]);
					d = scfps.length;
				}
			}
		}
		
		//	deal with cutoff (make unchecked part one large non-treatment subSection)
		for (int c = cutoffParagraph; c < feedbackRelevantParagraphCategories.length; c++) {
			
			//	start of skipped paragraphs
			if (c == cutoffParagraph)
				feedbackRelevantParagraphCategories[c] = (this.useComplexRules ? MULTIPLE_CATEGORY : OTHER_CATEGORY);
			
			//	the rest
			else feedbackRelevantParagraphCategories[c] = CONTINUE_CATEGORY;
		}
		
		//	return feedback
		return feedbackRelevantParagraphCategories;
	}
	
	private String buildParagraphLabel(MutableAnnotation paragraph) {
		if (paragraph.size() < 75)
			return TokenSequenceUtils.concatTokens(paragraph, true, true);
		
		StringBuffer label = new StringBuffer();
		label.append(TokenSequenceUtils.concatTokens(paragraph, 0, 25, true, true));
		label.append("<BR>[...]<BR>");
		label.append(TokenSequenceUtils.concatTokens(paragraph, (paragraph.size() - 25), 25, true, true));
		return label.toString();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#configureProcessor()
	 */
	public void configureProcessor() {
		this.rulesModified = (this.rulesModified | this.rules.editRules());
	}
}