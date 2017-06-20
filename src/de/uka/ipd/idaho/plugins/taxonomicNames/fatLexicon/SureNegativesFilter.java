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
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class SureNegativesFilter extends FatAnalyzer {
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	collect positives
		StringVector positives = new StringVector();
		positives.addContentIgnoreDuplicates(FAT.genera);
		positives.addContentIgnoreDuplicates(FAT.subGenera);
		positives.addContentIgnoreDuplicates(FAT.species);
		positives.addContentIgnoreDuplicates(FAT.subSpecies);
		positives.addContentIgnoreDuplicates(FAT.varieties);
		
		//	get negatives
		StringVector negatives = TokenSequenceUtils.getTextTokens(data);
		
		//	get candidates
		Annotation[] taxNameCandidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		StringVector candidateTokens = new StringVector();
		for (int c = 0; c < taxNameCandidates.length; c++)
			candidateTokens.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(taxNameCandidates[c]));
		negatives = negatives.without(candidateTokens);
		negatives = negatives.without(positives);
		
		//	remember extracted negatives
		FAT.negatives.addContentIgnoreDuplicates(negatives);
		
		//	use known negatives
		for (int c = 0; c < taxNameCandidates.length; c++) {
			StringVector taxNameNegatives = TokenSequenceUtils.getTextTokens(taxNameCandidates[c]).without(FAT.forbiddenWordsLowerCase, false).intersect(FAT.negatives, false);
			int ni = 0;
			while (ni < taxNameNegatives.size()) {
				if (taxNameNegatives.get(ni).length() < 3) taxNameNegatives.removeElementAt(ni);
				else ni++;
			}
			if (taxNameNegatives.size() != 0) data.removeAnnotation(taxNameCandidates[c]);
		}
	}
}