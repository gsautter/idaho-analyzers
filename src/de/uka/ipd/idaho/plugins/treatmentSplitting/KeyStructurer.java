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

import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;

/**
 * @author sautter
 */
public class KeyStructurer extends AbstractAnalyzer implements TreatmentConstants {
	
	private static final String KEY_STEP_ANNOTATION_TYPE = "keyStep";
	
	private static final String KEY_LEAD_ANNOTATION_TYPE = "keyLead";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	structure keys within treatments
		MutableAnnotation[] treatments = data.getMutableAnnotations(TREATMENT_ANNOTATION_TYPE);
		for (int t = 0; t < treatments.length; t++) {
			
			//	find and structure keys
			MutableAnnotation[] subSubSections = treatments[t].getMutableAnnotations(SUB_SUB_SECTION_TYPE);
			for (int s = 0; s < subSubSections.length; s++) {
				if (key_TYPE.equals(subSubSections[s].getAttribute(TYPE_ATTRIBUTE)))
					this.structureKey(subSubSections[s]);
			}
		}
		
		//	make keys outside treatments into treatments
		MutableAnnotation[] subSections = data.getMutableAnnotations(SUB_SECTION_TYPE);
		for (int s = 0; s < subSections.length; s++) {
			if (!key_TYPE.equals(subSections[s].getAttribute(TYPE_ATTRIBUTE)))
				continue;
			
			//	structure key
			int fss = this.structureKey(subSections[s]);
			if (fss < 1)
				continue;
			
			//	mark nomenclature and key
			MutableAnnotation nomenclature = subSections[s].addAnnotation(SUB_SUB_SECTION_TYPE, 0, fss);
			nomenclature.setAttribute(TYPE_ATTRIBUTE, nomenclature_TYPE);
			nomenclature.setAttribute("_generate", "key");
			Annotation[] nomenclatureTaxonNames = nomenclature.getAnnotations("taxonomicName");
			if (nomenclatureTaxonNames.length == 0) {
				subSections[s].removeAnnotation(nomenclature);
				continue;
			}
			MutableAnnotation key = subSections[s].addAnnotation(SUB_SUB_SECTION_TYPE, fss, (subSections[s].size() - fss));
			key.setAttribute(TYPE_ATTRIBUTE, key_TYPE);
			key.setAttribute("_generate", "key");
			
			//	make it a treatment
			subSections[s].changeTypeTo(TREATMENT_ANNOTATION_TYPE);
			subSections[s].removeAttribute(TYPE_ATTRIBUTE);
			subSections[s].setAttribute("_generate", "key");
		}
	}
	
	private int structureKey(MutableAnnotation key) {
		
		//	sort out non-relevant paragraphs (those in footnotes & captions)
		HashSet nonLeadParagraphIDs = new HashSet();
		MutableAnnotation[] footnotes = key.getMutableAnnotations(FOOTNOTE_TYPE);
		for (int f = 0; f < footnotes.length; f++) {
			Annotation[] paragraphs = footnotes[f].getAnnotations(PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++)
				nonLeadParagraphIDs.add(paragraphs[p].getAnnotationID());
		}
		MutableAnnotation[] captions = key.getMutableAnnotations(CAPTION_TYPE);
		for (int c = 0; c < captions.length; c++) {
			Annotation[] paragraphs = captions[c].getAnnotations(PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++)
				nonLeadParagraphIDs.add(paragraphs[p].getAnnotationID());
		}
		
		//	mark steps, parquetting over the starts of numbered paragraphs
		MutableAnnotation[] paragraphs = key.getMutableAnnotations(PARAGRAPH_TYPE);
		int stepStart = -1;
		int stepNr = -1;
		int firstStepStart = -1;
		for (int p = 0; p < paragraphs.length; p++) {
			if (nonLeadParagraphIDs.contains(paragraphs[p].getAnnotationID()))
				continue;
			
			//	continuation of step
			if (!Gamta.isNumber(paragraphs[p].firstValue()))
				continue;
			
			//	test number
			int pNr;
			try {
				pNr = Integer.parseInt(paragraphs[p].firstValue());
				if ((stepNr != -1) && (pNr != (stepNr + 1)))
					continue;
			}
			catch (NumberFormatException nfe) {
				continue;
			}
			
			//	mark open step
			if (stepStart != -1)
				key.addAnnotation(KEY_STEP_ANNOTATION_TYPE, stepStart, (paragraphs[p].getStartIndex() - stepStart));
			
			//	remember start of step
			stepStart = paragraphs[p].getStartIndex();
			if (firstStepStart == -1)
				firstStepStart = stepStart;
			
			//	remember step number
			stepNr = pNr;
		}
		
		//	mark last open step
		if (stepStart != -1)
			key.addAnnotation(KEY_STEP_ANNOTATION_TYPE, stepStart, (key.size() - stepStart));
		
		//	mark leads (individual paragraphs within steps)
		MutableAnnotation[] steps = key.getMutableAnnotations(KEY_STEP_ANNOTATION_TYPE);
		for (int s = 0; s < steps.length; s++) {
			paragraphs = steps[s].getMutableAnnotations(PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				if (nonLeadParagraphIDs.contains(paragraphs[p].getAnnotationID()))
					continue;
				steps[s].addAnnotation(KEY_LEAD_ANNOTATION_TYPE, paragraphs[p].getStartIndex(), paragraphs[p].size());
			}
		}
		
		//	finally ...
		return firstStepStart;
	}
}