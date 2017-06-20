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
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class KnownDataRules extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get candidates
		Annotation[] taxNameCandidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	apply known data to candidates iteratively
		for (int c = 0; c < taxNameCandidates.length; c++) {
			TaxonomicName tn = new TaxonomicName(taxNameCandidates[c]);
			boolean dataMatch = true;
			boolean kill = false;
			
			StringVector parts = new StringVector();
			
			if (tn.genus != null) {
				dataMatch = dataMatch && FAT.genera.contains(tn.genus);
				parts.addElement(GENUS_ATTRIBUTE + "=" + tn.genus);
			}
			if (tn.subGenus != null) {
				dataMatch = dataMatch && FAT.subGenera.contains(tn.subGenus);
				parts.addElement(SUBGENUS_ATTRIBUTE + "=" + tn.subGenus);
			}
			if (tn.species != null) {
				dataMatch = dataMatch && FAT.species.contains(tn.species);
				parts.addElement(SPECIES_ATTRIBUTE + "=" + tn.species);
			}
			if (tn.subSpecies != null) {
				dataMatch = dataMatch && FAT.subSpecies.contains(tn.subSpecies);
				parts.addElement(SUBSPECIES_ATTRIBUTE + "=" + tn.subSpecies);
			}
			if (tn.variety != null) {
				dataMatch = dataMatch && FAT.varieties.contains(tn.variety);
				parts.addElement(VARIETY_ATTRIBUTE + "=" + tn.variety);
			}
			
			if (tn.speciesAuthor != null) kill = kill || FAT.genera.contains(tn.speciesAuthor) || FAT.subGenera.contains(tn.speciesAuthor);
			if (tn.subSpeciesAuthor != null) kill = kill || FAT.genera.contains(tn.subSpeciesAuthor) || FAT.subGenera.contains(tn.subSpeciesAuthor);
			if (tn.varietyAuthor != null) kill = kill || FAT.genera.contains(tn.varietyAuthor) || FAT.subGenera.contains(tn.varietyAuthor);
			
//			if ((taxNameCandidates[c].size() == 1) && FAT.negativesDictionary.contains(taxNameCandidates[c].getValue()))
			if ((taxNameCandidates[c].size() == 1) && FAT.negativesDictionary.lookup(taxNameCandidates[c].getValue()))
				dataMatch = false;
			
			if (kill) {
//				System.out.println("FAT.KnownDataRules: Killing candidate for genus or subGenus in author position: " + taxNameCandidates[c].getValue());
				data.removeAnnotation(taxNameCandidates[c]);
			}
			else if (dataMatch) {
				taxNameCandidates[c].changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
				taxNameCandidates[c].setAttribute(FAT.EVIDENCE_ATTRIBUTE, "knownData");
				taxNameCandidates[c].setAttribute((FAT.EVIDENCE_ATTRIBUTE + "_details"), parts.concatStrings(", "));
			}
		}
	}
}
