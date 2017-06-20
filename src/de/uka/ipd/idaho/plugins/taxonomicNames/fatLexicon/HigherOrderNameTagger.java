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

public class HigherOrderNameTagger extends FatAnalyzer {
	
	private static final String FAMILY_PATTERN = "(([A-Z][a-z]{2,}(idae))|([A-Z]{3,}(IDAE)))";
	private static final String SUB_FAMILY_PATTERN = "(([A-Z][a-z]{2,}(inae))|([A-Z]{3,}(INAE)))";
	private static final String TRIBE_PATTERN = "(([A-Z][a-z]{2,}(ini))|([A-Z]{3,}(INI)))";
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get family names
		Annotation[] families = Gamta.extractAllMatches(data, FAMILY_PATTERN, 1);
		for (int f = 0; f < families.length; f++) {
			Annotation family = data.addAnnotation(families[f]);
			family.changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
			family.setAttribute(FAMILY_ATTRIBUTE, Gamta.capitalize(family.getValue()));
			family.setAttribute(RANK_ATTRIBUTE, FAMILY_ATTRIBUTE);
		}
		
		//	get sub family names
		Annotation[] subFamilies = Gamta.extractAllMatches(data, SUB_FAMILY_PATTERN, 1);
		for (int s = 0; s < subFamilies.length; s++) {
			Annotation subFamily = data.addAnnotation(subFamilies[s]);
			subFamily.changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
			subFamily.setAttribute(SUBFAMILY_ATTRIBUTE, Gamta.capitalize(subFamily.getValue()));
			subFamily.setAttribute(RANK_ATTRIBUTE, SUBFAMILY_ATTRIBUTE);
		}
		
		//	get tribe names
		Annotation[] tribes = Gamta.extractAllMatches(data, TRIBE_PATTERN, 1);
		for (int t = 0; t < tribes.length; t++) {
			Annotation tribe = data.addAnnotation(tribes[t]);
			tribe.changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
			tribe.setAttribute(TRIBE_ATTRIBUTE, Gamta.capitalize(tribe.getValue()));
			tribe.setAttribute(RANK_ATTRIBUTE, TRIBE_ATTRIBUTE);
		}
	}
}
