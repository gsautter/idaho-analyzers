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
public class DataRules extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get candidates and sure positives
		Annotation[] taxNameCandidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	apply data to candidates iteratively
		boolean newMatch = true;
		int round = 0;
		while (newMatch) {
			newMatch = false;
			round ++;
			
			for (int t = 0; t < taxNameCandidates.length; t++) {
				TaxonomicName tn = new TaxonomicName(taxNameCandidates[t]);
				
				int genusVote = 0;
				int subGenusVote = 0;
				int speciesVote = 0;
				int subSpeciesVote = 0;
				int varietyVote = 0;
				
				boolean kill = false;
				
				int lastVote = 0;
				int nameVote = 0;
				
				StringVector parts = new StringVector();
				
				//	investigate genus
				if (tn.genus != null) {
					if (FAT.genera.contains(tn.genus)) {
						genusVote ++;
						if (tn.genus.length() > 2) genusVote += 2;
						lastVote = genusVote;
						parts.addElement(GENUS_ATTRIBUTE + "=" + tn.genus);
					}
					else if (FAT.scientistsNames.contains(tn.genus)) kill = true;
//					else if ((tn.genus.length() > 2) && FAT.negativesDictionary.contains(tn.genus)) kill = true;
					else if ((tn.genus.length() > 2) && FAT.negativesDictionary.lookup(tn.genus)) kill = true;
				}
				
				//	investigate subgenus
				if (tn.subGenus != null) {
					if (FAT.subGenera.contains(tn.subGenus)) {
						subGenusVote ++;
						if (tn.subGenus.length() > 2) subGenusVote ++;
						lastVote = subGenusVote;
						parts.addElement(SUBGENUS_ATTRIBUTE + "=" + tn.subGenus);
					}
					else if (FAT.genera.contains(tn.subGenus) ||  FAT.scientistsNames.contains(tn.subGenus))
						kill = true;
//					else if ((tn.subGenus.length() > 2) && FAT.negativesDictionary.contains(tn.subGenus)) kill = true;
					else if ((tn.subGenus.length() > 2) && FAT.negativesDictionary.lookup(tn.subGenus)) kill = true;
//					else if (FAT.negativesDictionary.contains(tn.subGenus.toLowerCase())) {
					else if (FAT.negativesDictionary.lookup(tn.subGenus.toLowerCase())) {
						if (lastVote < 2)
							kill = true;
						else subGenusVote = -1;
						lastVote = subGenusVote;
					}
				}
				
				//	investigate species
				if (tn.species != null) {
					if (FAT.species.contains(tn.species)) {
						speciesVote ++;
						if (tn.species.length() > 2) speciesVote += 2;
						lastVote = speciesVote;
						parts.addElement(SPECIES_ATTRIBUTE + "=" + tn.species);
					}
//					else if ((tn.species.length() > 2) && FAT.negativesDictionary.contains(tn.species)) {
					else if ((tn.species.length() > 2) && FAT.negativesDictionary.lookup(tn.species)) {
						if ((tn.species.endsWith("s") && !tn.species.endsWith("is") && !tn.species.endsWith("us")) || tn.species.endsWith("e") || tn.species.endsWith("ed") || tn.species.endsWith("ing") || (lastVote < 0) || ((genusVote + subGenusVote) < 2))
							kill = true;
						else speciesVote = -1;
						lastVote = speciesVote;
					}
				}
				
				if ((tn.speciesAuthor != null) && FAT.scientistsNames.contains(tn.speciesAuthor)) {
					nameVote++;
					lastVote = 1;
					parts.addElement(SPECIES_ATTRIBUTE + "Author=" + tn.speciesAuthor);
				}
				
				//	investigate subspecies
				if (tn.subSpecies != null) {
					if (FAT.subSpecies.contains(tn.subSpecies)) {
						subSpeciesVote ++;
						if (tn.subSpecies.length() > 2) subSpeciesVote += 2;
						lastVote = subSpeciesVote;
						parts.addElement(SUBSPECIES_ATTRIBUTE + "=" + tn.subSpecies);
					}
//					else if ((tn.subSpecies.length() > 2) && FAT.negativesDictionary.contains(tn.subSpecies)) {
					else if ((tn.subSpecies.length() > 2) && FAT.negativesDictionary.lookup(tn.subSpecies)) {
						if ((tn.subSpecies.endsWith("s") && !tn.subSpecies.endsWith("is") && !tn.subSpecies.endsWith("us")) || tn.subSpecies.endsWith("e") || tn.subSpecies.endsWith("ed") || (speciesVote < 0))
							kill = true;
						else subSpeciesVote = -1;
						lastVote = subSpeciesVote;
					}
				}
				
				if ((tn.subSpeciesAuthor != null) && FAT.scientistsNames.contains(tn.subSpeciesAuthor)) {
					nameVote++;
					lastVote = 1;
					parts.addElement(SUBSPECIES_ATTRIBUTE + "Author=" + tn.subSpeciesAuthor);
				}
				
				//	investigate variety
				if (tn.variety != null) {
					if (FAT.varieties.contains(tn.variety)) {
						varietyVote ++;
						if (tn.variety.length() > 2) varietyVote += 2;
						lastVote = varietyVote;
						parts.addElement(VARIETY_ATTRIBUTE + "=" + tn.variety);
					}
//					else if ((tn.variety.length() > 2) && FAT.negativesDictionary.contains(tn.variety)) {
					else if ((tn.variety.length() > 2) && FAT.negativesDictionary.lookup(tn.variety)) {
						if ((tn.variety.endsWith("s") && !tn.variety.endsWith("is") && !tn.variety.endsWith("us")) || tn.variety.endsWith("e") || tn.variety.endsWith("ed") || (subSpeciesVote < 0))
							kill = true;
						else varietyVote = -1;
					}
					lastVote = varietyVote;
				}
				
				if ((tn.varietyAuthor != null) && FAT.scientistsNames.contains(tn.varietyAuthor)) {
					nameVote++;
					lastVote = 1;
					parts.addElement(VARIETY_ATTRIBUTE + "Author=" + tn.varietyAuthor);
				}
				
				int vote = genusVote + subGenusVote + speciesVote + subSpeciesVote + varietyVote + nameVote;
				
				//	check if person name prefix matched for lower case part of taxonomic name
				if (!kill && (tn.species != null) && FAT.nameNoiseWords.contains(tn.species)) kill = true;
				if (!kill && (tn.subSpecies != null) && FAT.nameNoiseWords.contains(tn.subSpecies)) kill = true;
				if (!kill && (tn.variety != null) && FAT.nameNoiseWords.contains(tn.variety)) kill = true;
				
				//	catch ambiguous words
//				if ((taxNameCandidates[t].size() == 1) && FAT.negativesDictionary.contains(taxNameCandidates[t].getValue()))
				if ((taxNameCandidates[t].size() == 1) && FAT.negativesDictionary.lookup(taxNameCandidates[t].getValue()))
					lastVote = 0;
				
				//	sure negative
				if (kill) data.removeAnnotation(taxNameCandidates[t]);
				
				//	sure positive
				else if ((lastVote > 0) && (vote > 2)) {
					
					taxNameCandidates[t].changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
					taxNameCandidates[t].setAttribute(FAT.EVIDENCE_ATTRIBUTE, "data");
					taxNameCandidates[t].setAttribute((FAT.EVIDENCE_ATTRIBUTE + "_details"), ("round: " + round + ", " + parts.concatStrings(", ")));
					FAT.addTaxonName(tn);
				}
			}
		}
	}
}
