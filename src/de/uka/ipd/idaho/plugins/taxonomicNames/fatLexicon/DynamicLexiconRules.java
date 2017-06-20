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

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class DynamicLexiconRules extends FatAnalyzer {
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get taxonomic names
		Annotation[] taxonomicNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	put up lexicons for data
		StringVector taxonNameTokens = new StringVector();
		StringVector docGenera = new StringVector();
		StringVector docSubGenera = new StringVector();
		StringVector docSpecies = new StringVector();
		StringVector docSubSpecies = new StringVector();
		StringVector docVarieties = new StringVector();
		StringVector docNameAuthors = new StringVector();
		
		//	parse existing sure positives
		for (int t = 0; t < taxonomicNames.length; t++) {
			taxonNameTokens.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(taxonomicNames[t]));
			TaxonomicName tn = new TaxonomicName(taxonomicNames[t]);
//			System.out.println("Parsing '" + taxonomicNames[t].getValue() + "':");
			
			if (tn.genus != null) {
//				System.out.println(" - genus: '" + tn.genus + "'");
				docGenera.addElementIgnoreDuplicates(tn.genus);
			}
			
			if (tn.subGenus != null) {
//				System.out.println(" - subGenus: '" + tn.subGenus + "'");
				docSubGenera.addElementIgnoreDuplicates(tn.subGenus);
			}
			
			if (tn.species != null) {
//				System.out.println(" - species: '" + tn.species + "'");
				docSpecies.addElementIgnoreDuplicates(tn.species);
			}
			
			if (tn.speciesAuthor != null) {
//				System.out.println(" - speciesAuthor: '" + tn.speciesAuthor + "'");
				docNameAuthors.addElementIgnoreDuplicates(tn.speciesAuthor);
				
				//	add further author names
				if (tn.species != null) {
					int nsIndex = TokenSequenceUtils.indexOf(taxonomicNames[t], tn.species);
					if (nsIndex != -1)
						for (int v = (nsIndex+1); v < taxonomicNames[t].size(); v++) {
							String value = taxonomicNames[t].valueAt(v);
							if (value.equals(tn.speciesAuthor))
								v = taxonomicNames[t].size();
							else if ((value.length() > 2) && Gamta.isFirstLetterUpWord(value))
								docNameAuthors.addElementIgnoreDuplicates(taxonomicNames[t].valueAt(v));
						}
				}
			}
			
			if (tn.subSpecies != null) {
//				System.out.println(" - subSpecies: '" + tn.subSpecies + "'");
				docSubSpecies.addElementIgnoreDuplicates(tn.subSpecies);
			}
			
			if (tn.subSpeciesAuthor != null) {
//				System.out.println(" - subSpeciesAuthor: '" + tn.subSpeciesAuthor + "'");
				docNameAuthors.addElementIgnoreDuplicates(tn.subSpeciesAuthor);
				
				//	add further author names
				if (tn.subSpecies != null) {
					int nsIndex = TokenSequenceUtils.indexOf(taxonomicNames[t], tn.subSpecies);
					if (nsIndex != -1)
						for (int v = (nsIndex+1); v < taxonomicNames[t].size(); v++) {
							String value = taxonomicNames[t].valueAt(v);
							if (value.equals(tn.subSpeciesAuthor))
								v = taxonomicNames[t].size();
							else if ((value.length() > 2) && Gamta.isFirstLetterUpWord(value))
								docNameAuthors.addElementIgnoreDuplicates(taxonomicNames[t].valueAt(v));
						}
				}
			}
			
			if (tn.variety != null) {
//				System.out.println(" - variety: '" + tn.variety + "'");
				docVarieties.addElementIgnoreDuplicates(tn.variety);
			}
			
			if (tn.varietyAuthor != null) {
//				System.out.println(" - varietyAuthor: '" + tn.varietyAuthor + "'");
				docNameAuthors.addElementIgnoreDuplicates(tn.varietyAuthor);
				
				//	add further author names
				if (tn.variety != null) {
					int nsIndex = TokenSequenceUtils.indexOf(taxonomicNames[t], tn.variety);
					if (nsIndex != -1)
						for (int v = (nsIndex+1); v < taxonomicNames[t].size(); v++) {
							String value = taxonomicNames[t].valueAt(v);
							if (value.equals(tn.varietyAuthor))
								v = taxonomicNames[t].size();
							else if ((value.length() > 2) && Gamta.isFirstLetterUpWord(value))
								docNameAuthors.addElementIgnoreDuplicates(taxonomicNames[t].valueAt(v));
						}
				}
			}
		}
		
		System.out.println("DynamicLexiconRules: author names are\n- " + docNameAuthors.concatStrings("\n- "));
		
		//	filter matches residing in sure positives already
		Annotation[] taxonNameTokenAnnotations = Gamta.extractAllContained(data, taxonNameTokens, 1, true, false);
		ArrayList dynamicLexiconMatches = new ArrayList();
		int tni = 0;
		for (int a = 0; a < taxonNameTokenAnnotations.length; a++) {
			
			//	find next sure positive
			while ((tni < taxonomicNames.length) && (taxonomicNames[tni].getStartIndex() < taxonNameTokenAnnotations[a].getStartIndex())) tni++;
			
			//	add annotation to match list if not resident in sure positive
			if ((tni == taxonomicNames.length) || !AnnotationUtils.contains(taxonomicNames[tni], taxonNameTokenAnnotations[a]))
				dynamicLexiconMatches.add(taxonNameTokenAnnotations[a]);
		}
		
		//	join neighboring matches
		int li = 0;
		while (li < (dynamicLexiconMatches.size() - 1)) {
			Annotation dynamicLexiconMatch1 = ((Annotation) dynamicLexiconMatches.get(li));
			Annotation dynamicLexiconMatch2 = ((Annotation) dynamicLexiconMatches.get(li + 1));
			
			//	overlap, remove second annotation
			if (dynamicLexiconMatch1.getStartIndex() == dynamicLexiconMatch2.getStartIndex()) dynamicLexiconMatches.remove(li+1);
			
			//	neighboring annotations & not paragraph end, join them 
			else if ((dynamicLexiconMatch1.getEndIndex() == dynamicLexiconMatch2.getStartIndex()) && !dynamicLexiconMatch1.lastToken().hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				
				//	watch out for author names in position of genera
				if (docNameAuthors.contains(dynamicLexiconMatch1.firstValue())) li++;
				
				//	genus in place of author name
				else if (docGenera.contains(dynamicLexiconMatch2.firstValue())) li++;
				
				//	join is sensible
				else {
					Annotation joint = Gamta.newAnnotation(data, dynamicLexiconMatch1.getType(), dynamicLexiconMatch1.getStartIndex(), (dynamicLexiconMatch1.size() + dynamicLexiconMatch2.size()));
					dynamicLexiconMatches.set(li, joint);
					dynamicLexiconMatches.remove(li + 1);
//					System.out.println("Joined two subsequent token matches: '" + joint.getValue() + "'");
				}
			}
			
			//	proceed to next annotation
			else li++;
		}
		
		//	add dynamic lexicon matches to document
		for (int m = 0; m < dynamicLexiconMatches.size(); m++) {
			Annotation dynamicLexiconMatch = ((Annotation) dynamicLexiconMatches.get(m));
			
			//	avoid adding single abbreviations and name authors annotated as genera
			if ((dynamicLexiconMatch.length() > 3) && !docNameAuthors.contains(dynamicLexiconMatch.getValue())) {
				dynamicLexiconMatch = data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, dynamicLexiconMatch.getStartIndex(), dynamicLexiconMatch.size());
				dynamicLexiconMatch.setAttribute(FAT.EVIDENCE_ATTRIBUTE, "dynamicLexicon");
			}
		}
	}
}
