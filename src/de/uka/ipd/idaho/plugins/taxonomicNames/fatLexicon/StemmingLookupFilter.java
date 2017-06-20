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
 *     * Neither the name of the Universit�t Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSIT�T KARLSRUHE (TH) AND CONTRIBUTORS 
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

public class StemmingLookupFilter extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	apply regular expression
		Annotation[] taxNameCandidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	remove candidates containing a stemmed form or ending with a common english word
		for (int t = 0; t < taxNameCandidates.length; t++) {
			Annotation taxNameCandidate = taxNameCandidates[t];
			TaxonomicName tn = new TaxonomicName(taxNameCandidate);
			boolean kill = false;
			String reason = "";
			
			//	do stemmin lookup
			if (!kill && (tn.genus != null) && (tn.genus.length() > 2) && !FAT.genera.contains(tn.genus))
//				if (FAT.doStemmingLookup(tn.genus) || FAT.forbiddenWordsUpperCase.contains(tn.genus) || FAT.negativesDictionary.contains(tn.genus.toLowerCase())) {
				if (FAT.doStemmingLookup(tn.genus) || FAT.forbiddenWordsUpperCase.contains(tn.genus) || FAT.negativesDictionary.lookup(tn.genus.toLowerCase())) {
					kill = true;
					reason += ("genus '" + tn.genus + "'");
				}
			
			if (!kill && (tn.subGenus != null) && (tn.subGenus.length() > 2) && !FAT.subGenera.contains(tn.genus))
//				if (FAT.doStemmingLookup(tn.subGenus) || FAT.forbiddenWordsUpperCase.contains(tn.subGenus) || FAT.negativesDictionary.contains(tn.subGenus.toLowerCase())) {
				if (FAT.doStemmingLookup(tn.subGenus) || FAT.forbiddenWordsUpperCase.contains(tn.subGenus) || FAT.negativesDictionary.lookup(tn.subGenus.toLowerCase())) {
					kill = true;
					reason += ("subGenus '" + tn.subGenus + "'");
				}
			
			if (!kill && (tn.species != null) && (tn.species.length() > 2) && !FAT.species.contains(tn.species))
				if (FAT.doStemmingLookup(tn.species) || FAT.forbiddenWordsLowerCase.contains(tn.species)) {
					kill = true;
					reason += ("species '" + tn.species + "'");
				}
			
			if (!kill && (tn.subSpecies != null) && (tn.subSpecies.length() > 2) && !FAT.subSpecies.contains(tn.subSpecies) && !FAT.lowerCaseParts.contains(tn.subSpecies))
				if (FAT.doStemmingLookup(tn.subSpecies) || FAT.forbiddenWordsLowerCase.contains(tn.subSpecies)) {
					kill = true;
					reason += ("subSpecies '" + tn.subSpecies + "'");
				}
			
			if (!kill && (tn.variety != null) && (tn.variety.length() > 2) && !FAT.varieties.contains(tn.variety) && !FAT.lowerCaseParts.contains(tn.variety))
				if (FAT.doStemmingLookup(tn.variety) || FAT.forbiddenWordsLowerCase.contains(tn.variety)) {
					kill = true;
					reason += ("variety '" + tn.variety + "'");
				}
			
			if (kill) {
//				System.out.println("FAT.StemmingLookupFilter: removing candidate '" + taxNameCandidate + "' for " + reason);
				data.removeAnnotation(taxNameCandidate);
			}
		}
	}
}
