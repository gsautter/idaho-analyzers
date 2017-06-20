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
import java.util.ArrayList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AnnotationEditorFeedbackPanel;

/**
 * @author sautter
 *
 */
public class SmallTreatmentSplitter extends AbstractAnalyzer implements LiteratureConstants {
	
	private static final String TREATMENT_ANNOTATION_TYPE = "treatment";
	private static final String NOMENCLATURE_ANNOTATION_TYPE = "nomenclature";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		if (!parameters.containsKey(INTERACTIVE_PARAMETER)) return;
		
		//	get treatments
		MutableAnnotation[] treatments = data.getMutableAnnotations(TREATMENT_ANNOTATION_TYPE);
		ArrayList smallTreatments = new ArrayList();
		
		//	extract the ones with one paragraph only
		for (int t = 0; t < treatments.length; t++) {
			if ((treatments[t].size() > 1) && (treatments[t].getMutableAnnotations(PARAGRAPH_TYPE).length == 1))
				smallTreatments.add(treatments[t]);
		}
		if (smallTreatments.isEmpty()) return;
		treatments = ((MutableAnnotation[]) smallTreatments.toArray(new MutableAnnotation[smallTreatments.size()]));
		
		//	pre-split single-paragraph treatments after first taxonomic name or taxonomic name label
		for (int t = 0; t < treatments.length; t++) {
			int nomenclatureEnd = 0;
			Annotation[] taxonomicNameLabels = treatments[t].getAnnotations("taxonomicNameLabel");
			if (taxonomicNameLabels.length != 0)
				nomenclatureEnd = taxonomicNameLabels[0].getEndIndex();
			if (nomenclatureEnd == 0) {
				Annotation[] taxonomicNames = treatments[t].getAnnotations("taxonomicName");
				if (taxonomicNames.length != 0)
					nomenclatureEnd = taxonomicNames[0].getEndIndex();
			}
			if ((nomenclatureEnd != 0) && (treatments[t].size() > 1)) {
				while ((nomenclatureEnd < treatments[t].size()) && (Gamta.isSentenceEnd(treatments[t].valueAt(nomenclatureEnd))))
					nomenclatureEnd++;
				treatments[t].addAnnotation(NOMENCLATURE_ANNOTATION_TYPE, 0, nomenclatureEnd);
			}
		}
		
		//	produce feedback panels
		AnnotationEditorFeedbackPanel[] aefps = new AnnotationEditorFeedbackPanel[treatments.length];
		for (int t = 0; t < treatments.length; t++) {
			aefps[t] = new AnnotationEditorFeedbackPanel("Check Materials Citation Details");
			aefps[t].setLabel("<HTML>Please mark the nomenclatorial part of this one-paragraph treatment." +
					"<BR>The treatment will be split into two paragraphs in order to place the nomenclatorial part in a separate paragraph." +
					"<BR>If you do not mark a nomenclatorial part, the treatment will remain as one paragraph.</HTML>");
			aefps[t].addDetailType(NOMENCLATURE_ANNOTATION_TYPE, Color.ORANGE);
			aefps[t].addAnnotation(treatments[t]);
			
			//	add backgroung information
			aefps[t].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, treatments[t].getDocumentProperty(LiteratureConstants.DOCUMENT_ID_ATTRIBUTE));
			aefps[t].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, treatments[t].getAnnotationID());
			aefps[t].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, TREATMENT_ANNOTATION_TYPE);
		}
		
		//	get feedback
		int cutoffIndex = treatments.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(aefps);
			
			//	process all feedback data together
			for (int t = 0; t < aefps.length; t++)
				AnnotationEditorFeedbackPanel.writeChanges(aefps[t].getTokenStatesAt(0), treatments[t]);
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < aefps.length; d++) {
			if (d != 0)
				aefps[d].addButton("Previous");
			aefps[d].addButton("Cancel");
			aefps[d].addButton("OK" + (((d+1) == aefps.length) ? "" : " & Next"));
			
			String title = aefps[d].getTitle();
			aefps[d].setTitle(title + " - (" + (d+1) + " of " + aefps.length + ")");
			
			String f = aefps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			aefps[d].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK"))
				AnnotationEditorFeedbackPanel.writeChanges(aefps[d].getTokenStatesAt(0), treatments[d]);
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog onward
			else {
				cutoffIndex = d;
				d = aefps.length;
			}
		}

		//	process feedback
		for (int t = 0; t < cutoffIndex; t++) {
			MutableAnnotation[] nomenclature = treatments[t].getMutableAnnotations(NOMENCLATURE_ANNOTATION_TYPE);
			if ((nomenclature.length != 0) && (nomenclature[0].size() < treatments[t].size())) {
				MutableAnnotation[] paragraph = treatments[t].getMutableAnnotations(PARAGRAPH_TYPE);
				
				int split;
				if ((treatments[t].size() - nomenclature[0].getEndIndex()) < nomenclature[0].getStartIndex())
					split = nomenclature[0].getStartIndex();
				else split = nomenclature[0].getEndIndex();
				treatments[t].removeAnnotation(nomenclature[0]);
				
				MutableAnnotation firstPart = treatments[t].addAnnotation(PARAGRAPH_TYPE, 0, split);
				firstPart.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				MutableAnnotation secondPart = treatments[t].addAnnotation(PARAGRAPH_TYPE, split, (treatments[t].size() - split));
				secondPart.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				
				if (paragraph.length != 0) {
					firstPart.copyAttributes(paragraph[0]);
					secondPart.copyAttributes(paragraph[0]);
					treatments[t].removeAnnotation(paragraph[0]);
				}
			}
		}
	}
}