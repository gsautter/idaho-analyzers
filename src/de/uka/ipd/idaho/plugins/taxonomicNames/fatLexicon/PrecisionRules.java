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


import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class PrecisionRules extends FatAnalyzer {
	
	private StringVector noise;
	
	/** @see de.idaho.plugins.fat.FatAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		super.initAnalyzer();
		
		//	get noise words
		this.noise = Gamta.getNoiseWords();
		int n = 0;
		while (n < this.noise.size()) {
			if (this.noise.get(n).length() < 2) this.noise.remove(n);
			else n++;
		}
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get regular expressions
		String[] regExNames = FAT.getPrecisionRuleNames();
		StringVector regExList = new StringVector();
		for (int n = 0; n < regExNames.length; n++) {
			String regEx = FAT.getRegEx(regExNames[n]);
			if (regEx != null) regExList.addElementIgnoreDuplicates(regEx);
		}
		String[] regExes = regExList.toStringArray();
		
		//	pre-create Pattern matchers
		ArrayList patternList = new ArrayList();
		for (int r = 0; r < regExes.length; r++)
			patternList.add(Pattern.compile(regExes[r]));
		Pattern[] patterns = ((Pattern[]) patternList.toArray(new Pattern[patternList.size()]));
		
		//	get candidates
		Annotation[] taxNameCandidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	rise candidates to matches if they match a precision rule
		for (int t = 0; t < taxNameCandidates.length; t++) {
			boolean match = false;
			for (int p = 0; !match && (p < patterns.length); p++)
				match = patterns[p].matcher(taxNameCandidates[t].getValue()).matches();
			if (match) {
				taxNameCandidates[t].changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
				taxNameCandidates[t].setAttribute(FAT.EVIDENCE_ATTRIBUTE, "precision");
				FAT.addTaxonName(new TaxonomicName(taxNameCandidates[t]));
			}
		}
	}
}
