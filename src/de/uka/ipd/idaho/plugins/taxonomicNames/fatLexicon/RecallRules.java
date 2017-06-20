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
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class RecallRules extends FatAnalyzer {
	
//	private StringVector noise;
//	
//	/** @see de.idaho.plugins.fat.FatAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
//		super.initAnalyzer();
//		
//		//	get noise words
//		this.noise = Gamta.getNoiseWords();
//		int n = 0;
//		while (n < this.noise.size()) {
//			if (this.noise.get(n).length() < 2) this.noise.remove(n);
//			else n++;
//		}
//	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		long start = System.currentTimeMillis();
		long intermediate = start;
		long stop;
		System.out.println("FAT.RecallRules: Start processing document with " + data.size() + " tokens ...");
		
		//	extract excludable tokens
		StringVector startExcludeTokens = new StringVector();
//		startExcludeTokens.addContentIgnoreDuplicates(this.noise);
		startExcludeTokens.addContentIgnoreDuplicates(FAT.noiseWords);
		
		StringVector excludeTokens = new StringVector();
//		excludeTokens.addContentIgnoreDuplicates(this.noise);
		excludeTokens.addContentIgnoreDuplicates(FAT.noiseWords);
		
		for (int v = 0; v < data.size(); v++) {
			String value = data.valueAt(v);
			String cleanValue = value.replaceAll("\\-\\_\\'", "");
			if (!Gamta.isWord(value) && ((cleanValue.length() == 0) || !Gamta.isWord(cleanValue))) {
				startExcludeTokens.addElementIgnoreDuplicates(value);
				excludeTokens.addElementIgnoreDuplicates(value);
			}
		}
		
		startExcludeTokens.removeAll("(");
		startExcludeTokens.addContentIgnoreDuplicates(FAT.forbiddenWordsLowerCase);
		
		excludeTokens.removeAll(".");
		excludeTokens.removeAll(",");
		excludeTokens.removeAll("&");
		excludeTokens.removeAll("-");
		excludeTokens.removeAll("(");
		excludeTokens.removeAll(")");
		excludeTokens = excludeTokens.without(FAT.nameNoiseWords, false);
		
		stop = System.currentTimeMillis();
		System.out.println("FAT.RecallRules: Got exclude lists in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
		intermediate = stop;
		
		//	get regular expressions
		String[] regExNames = FAT.getRecallRuleNames();
		
		//	apply regular expression
		Annotation[] taxNameCandidates;
		
		//	apply recall rules
		for (int r = 0; r < regExNames.length; r++) {
			String regEx = FAT.getRegEx(regExNames[r]);
			
			if (regEx != null) {
//				int maxTokens = 22;
//				try {
//					maxTokens = FAT.getMaxTokens(regExNames[r]);
//				} catch (NumberFormatException nfe) {}
				
				//	no need for optimized special purpose clones any longer, since new Gamta.extractAllMatches() is fast enough 
				
//				taxNameCandidates = FAT.extractAllMatches(data, regEx, maxTokens, startExcludeTokens, excludeTokens, true);
//				taxNameCandidates = Gamta.extractAllMatches(data, regEx, maxTokens, startExcludeTokens, excludeTokens, true);
				taxNameCandidates = Gamta.extractAllMatches(data, regEx, 0, startExcludeTokens, excludeTokens, true);
				stop = System.currentTimeMillis();
				System.out.println("FAT.RecallRules: Rule '" + regExNames[r] + "' applied in " + (stop - intermediate) + " ms, " + (stop - start) + " ms in total.");
				intermediate = stop;
				
				for (int c = 0; c < taxNameCandidates.length; c++) {
					Annotation raw = taxNameCandidates[c];
					boolean oneLine = true;
					for (int t = 0; t < (raw.size() - 1); t++)
						oneLine = (oneLine && !raw.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE));
					
					if (oneLine) {
						StringVector textTokens = TokenSequenceUtils.getTextTokens(raw);
						if (!FAT.forbiddenWordsUpperCase.contains(textTokens.firstElement()) && !FAT.forbiddenWordsLowerCase.containsIgnoreCase(textTokens.lastElement()))
							data.addAnnotation(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
					}
//					StringVector textTokens = TokenSequenceUtils.getTextTokens(raw);
//					if (!FAT.forbiddenWordsUpperCase.contains(textTokens.firstElement()) && !FAT.forbiddenWordsLowerCase.containsIgnoreCase(textTokens.lastElement()))
//						data.addAnnotation(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE, raw.getStartIndex(), raw.size());
				}
			}
		}
		
		//	remove duplicates
		AnnotationFilter.removeDuplicates(data, FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
	}
}
