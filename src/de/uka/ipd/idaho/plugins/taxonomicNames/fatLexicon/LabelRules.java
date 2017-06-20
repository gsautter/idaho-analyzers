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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;


import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;

/**
 * @author sautter
 *
 */
public class LabelRules extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		long start = System.currentTimeMillis();
		long intermediate = start;
		long stop;
		System.out.println("FAT.LabelRules: Start processing document with " + data.size() + " tokens ...");
		
		//	apply regular expressions
		Annotation[] taxNameLabels;
		
		//	find genus labels
		taxNameLabels = Gamta.extractAllContained(data, FAT.newGenusLabels, false);
		for (int t = 0; t < taxNameLabels.length; t++) {
			Annotation raw = taxNameLabels[t];
			Annotation label = data.addAnnotation(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
			label.setAttribute(RANK_ATTRIBUTE, GENUS_ATTRIBUTE);
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got genus labels in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	find subgenus labels
		taxNameLabels = Gamta.extractAllContained(data, FAT.newSubGenusLabels, false);
		for (int t = 0; t < taxNameLabels.length; t++) {
			Annotation raw = taxNameLabels[t];
			Annotation label = data.addAnnotation(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
			label.setAttribute(RANK_ATTRIBUTE, SUBGENUS_ATTRIBUTE);
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got subGenus labels in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	find species labels
		taxNameLabels = Gamta.extractAllContained(data, FAT.newSpeciesLabels, false);
		for (int t = 0; t < taxNameLabels.length; t++) {
			Annotation raw = taxNameLabels[t];
			Annotation label = data.addAnnotation(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
			label.setAttribute(RANK_ATTRIBUTE, SPECIES_ATTRIBUTE);
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got species labels in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	find subspecies labels
		taxNameLabels = Gamta.extractAllContained(data, FAT.newSubSpeciesLabels, false);
		for (int t = 0; t < taxNameLabels.length; t++) {
			Annotation raw = taxNameLabels[t];
			Annotation label = data.addAnnotation(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
			label.setAttribute(RANK_ATTRIBUTE, SUBSPECIES_ATTRIBUTE);
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got subSpecies labels in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	find variety labels
		taxNameLabels = Gamta.extractAllContained(data, FAT.newVarietyLabels, false);
		for (int t = 0; t < taxNameLabels.length; t++) {
			Annotation raw = taxNameLabels[t];
			Annotation label = data.addAnnotation(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
			label.setAttribute(RANK_ATTRIBUTE, VARIETY_ATTRIBUTE);
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got variety labels in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	find other labels
		taxNameLabels = Gamta.extractAllContained(data, FAT.newOtherLabels, false);
		for (int t = 0; t < taxNameLabels.length; t++) {
			Annotation raw = taxNameLabels[t];
			data.addAnnotation(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got other labels in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	remove duplicates
		AnnotationFilter.removeDuplicates(data, FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Duplicates removed in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	find candidates next to labels
		Annotation[] candidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		Annotation[] labels = data.getAnnotations(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		int li = 0;
		for (int c = 0; (c < candidates.length) && (li < labels.length); c++) {
			while ((li < labels.length) && (candidates[c].getStartIndex() > labels[li].getStartIndex())) li++;
			if (li < labels.length) {
				boolean sure = false;
				if ((candidates[c].getEndIndex() == labels[li].getStartIndex())
						&& !"new".equalsIgnoreCase(labels[li].firstValue())
						&& !"nov".equalsIgnoreCase(labels[li].firstValue())
						&& !"n".equalsIgnoreCase(labels[li].firstValue())
					) sure = true;
				
				if (((candidates[c].getEndIndex() + 1) == labels[li].getStartIndex())
						&& ",".equals(data.valueAt(candidates[c].getEndIndex()))
					) sure = true;
				
				if (sure) {
					candidates[c].changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
					candidates[c].setAttribute(FAT.EVIDENCE_ATTRIBUTE, "label");
					if (labels[li].hasAttribute(RANK_ATTRIBUTE))
						candidates[c].setAttribute(RANK_ATTRIBUTE, labels[li].getAttribute(RANK_ATTRIBUTE));
					FAT.addTaxonName(new TaxonomicName(candidates[c]));
				}
			}
		}
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Got labelled sure positives in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	remove candidates that overlap with labels
		candidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		labels = data.getAnnotations(FAT.TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		li = 0;
		for (int c = 0; (c < candidates.length) && (li < labels.length); c++) {
			while ((li < labels.length) && (candidates[c].getStartIndex() > labels[li].getStartIndex())) li++;
			if ((li < labels.length) && (candidates[c].getEndIndex() > labels[li].getStartIndex()))
				data.removeAnnotation(candidates[c]);
		}
		
		//	remove candidates contained in now sure positives
		AnnotationFilter.removeByContained(data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE, false);
		stop = System.currentTimeMillis();
		System.out.println("FAT.LabelRules: Candidates filtered in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
	}
}
