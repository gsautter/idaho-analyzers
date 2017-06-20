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
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * @author sautter
 *
 */
public class TaxonomicNameAttributor extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		Annotation[] taxonomicNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		for (int t = 0; t < taxonomicNames.length; t++) {
			if (taxonomicNames[t].hasAttribute(FAT.RANK_ATTRIBUTE))
				continue;
			
			if ((taxonomicNames[t].size() == 1) && Gamta.isFirstLetterUpWord(taxonomicNames[t].firstValue())) {
				String name = Gamta.capitalize(taxonomicNames[t].firstValue());
				if (name.endsWith("idae")) {
					taxonomicNames[t].setAttribute(FAMILY_ATTRIBUTE, name);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, FAMILY_ATTRIBUTE);
					continue;
				}
				else if (name.endsWith("inae")) {
					taxonomicNames[t].setAttribute(SUBFAMILY_ATTRIBUTE, name);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, SUBFAMILY_ATTRIBUTE);
					continue;
				}
				else if (name.endsWith("ini")) {
					taxonomicNames[t].setAttribute(TRIBE_ATTRIBUTE, name);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, TRIBE_ATTRIBUTE);
					continue;
				}
			}
			
			TaxonomicName tn = new TaxonomicName(taxonomicNames[t]);
			String rank = (taxonomicNames[t].hasAttribute(RANK_ATTRIBUTE) ? taxonomicNames[t].getAttribute(RANK_ATTRIBUTE).toString() : null);
			if (tn.genus != null) {
				taxonomicNames[t].setAttribute(GENUS_ATTRIBUTE, tn.genus);
				taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, GENUS_ATTRIBUTE);
			}
			if (tn.subGenus != null) {
				if (SPECIES_ATTRIBUTE.equals(rank) && (tn.species == null)) {
					taxonomicNames[t].setAttribute(SPECIES_ATTRIBUTE, tn.subGenus.toLowerCase());
					FAT.upperCaseLowerCaseParts.addElementIgnoreDuplicates(tn.subGenus);
				}
				else {
					taxonomicNames[t].setAttribute(SUBGENUS_ATTRIBUTE, tn.subGenus);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, SUBGENUS_ATTRIBUTE);
				}
			}
			if (tn.species != null) {
				taxonomicNames[t].setAttribute(SPECIES_ATTRIBUTE, tn.species);
				taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, SPECIES_ATTRIBUTE);
			}
			if (tn.subSpecies != null) {
				taxonomicNames[t].setAttribute(SUBSPECIES_ATTRIBUTE, tn.subSpecies);
				taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, SUBSPECIES_ATTRIBUTE);
			}
			if (tn.variety != null) {
				taxonomicNames[t].setAttribute(VARIETY_ATTRIBUTE, tn.variety);
				taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, VARIETY_ATTRIBUTE);
			}
		}
	}
}