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
import java.util.Collections;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

public class LexiconRules extends FatAnalyzer {
	
	private Dictionary lowerCaseDictionary = null;
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get candidates
		Annotation[] candidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	apply lexicon data to candidates
		for (int c = 0; c < candidates.length; c++) {
			TaxonomicName tn = new TaxonomicName(candidates[c]);
			boolean dataMatch = true;
			boolean lastWasAbbreviation = true;
			boolean kill = false;
			String killReason = null;
			
			if (tn.genus != null) {
				if (tn.genus.length() > 2) {
					dataMatch = dataMatch && FAT.genera.contains(tn.genus);
					lastWasAbbreviation = false;
				}
				else {
					dataMatch = dataMatch && this.abbreviationLookup(FAT.genera, tn.genus);
					lastWasAbbreviation = true;
				}
			}
			if (tn.subGenus != null) {
				if (tn.subGenus.length() > 2) {
					dataMatch = dataMatch && FAT.subGenera.contains(tn.subGenus);
					lastWasAbbreviation = false;
				}
				else {
					dataMatch = dataMatch && this.abbreviationLookup(FAT.subGenera, tn.subGenus);
					lastWasAbbreviation = true;
				}
			}
			if (tn.species != null){
				if (tn.species.length() > 2) {
					dataMatch = dataMatch && FAT.lowerCaseParts.contains(tn.species);
					lastWasAbbreviation = false;
				}
				else {
					dataMatch = dataMatch && this.abbreviationLookup(FAT.lowerCaseParts, tn.species);
					lastWasAbbreviation = true;
				}
			}
			if (tn.subSpecies != null) {
				if (tn.subSpecies.length() > 2) {
					dataMatch = dataMatch && FAT.lowerCaseParts.contains(tn.subSpecies);
					lastWasAbbreviation = false;
				}
				else {
					dataMatch = dataMatch && this.abbreviationLookup(FAT.lowerCaseParts, tn.subSpecies);
					lastWasAbbreviation = true;
				}
			}
			if (tn.variety != null) {
				if (tn.variety.length() > 2) {
					dataMatch = dataMatch && FAT.lowerCaseParts.contains(tn.variety);
					lastWasAbbreviation = false;
				}
				else {
					dataMatch = dataMatch && this.abbreviationLookup(FAT.lowerCaseParts, tn.variety);
					lastWasAbbreviation = true;
				}
			}
			
			//	check for known genera and subgenera in invalid positions
			if (tn.speciesAuthor != null) {
				kill = (kill || FAT.genera.contains(tn.speciesAuthor) || FAT.subGenera.contains(tn.speciesAuthor));
				if (killReason == null) killReason = tn.speciesAuthor;
			}
			if (tn.subSpeciesAuthor != null) {
				kill = (kill || FAT.genera.contains(tn.subSpeciesAuthor) || FAT.subGenera.contains(tn.subSpeciesAuthor));
				if (killReason == null) killReason = tn.subSpeciesAuthor;
			}
			if (tn.varietyAuthor != null) {
				kill = (kill || FAT.genera.contains(tn.varietyAuthor) || FAT.subGenera.contains(tn.varietyAuthor));
				if (killReason == null) killReason = tn.varietyAuthor;
			}
			
			if (kill) {
				System.out.println("FAT.LexiconRules: Killing candidate for genus or subGenus '" + killReason + "' in author position: " + candidates[c].getValue());
				data.removeAnnotation(candidates[c]);
			}
			else if (dataMatch && !lastWasAbbreviation) {
				candidates[c].changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
				candidates[c].setAttribute(FAT.EVIDENCE_ATTRIBUTE, "lexicon");
				FAT.addTaxonName(tn);
			}
		}
		
		if (this.lowerCaseDictionary == null) this.initDictionary();
		
		//	extract known data
		ArrayList lexiconMatches = new ArrayList();
		
		//	extract genera
		Annotation[] genera = Gamta.extractAllContained(data, FAT.genera, 1, false);
		for (int g = 0; g < genera.length; g++)
			if (genera[g].length() > 2)
				lexiconMatches.add(genera[g]);
		
		//	extract subGenera
		Annotation[] subGenera = Gamta.extractAllContained(data, FAT.subGenera, 1, true);
		for (int s = 0; s < subGenera.length; s++)
			lexiconMatches.add(subGenera[s]);
		
		//	extract lower case parts
		Annotation[] lowerCase = Gamta.extractAllContained(data, this.lowerCaseDictionary, 1, true);
		for (int l = 0; l < lowerCase.length; l++)
			lexiconMatches.add(lowerCase[l]);
		
		//	join neighboured lexicon matches
		Collections.sort(lexiconMatches);
		int li = 0;
		while (li < (lexiconMatches.size() - 1)) {
			Annotation lexiconMatch1 = ((Annotation) lexiconMatches.get(li));
			Annotation lexiconMatch2 = ((Annotation) lexiconMatches.get(li + 1));
			
			//	overlap, remove second annotation
			if (lexiconMatch1.getStartIndex() == lexiconMatch2.getStartIndex())
				lexiconMatches.remove(li+1);
			
			//	neighboring annotations & not paragraph end, join them 
			else if ((lexiconMatch1.getEndIndex() == lexiconMatch2.getStartIndex()) && !lexiconMatch1.lastToken().hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				
				//	genus in place of author name
				if (FAT.genera.contains(lexiconMatch2.firstValue())) li++;
				
				//	join is sensible
				else {
					Annotation joint = Gamta.newAnnotation(data, lexiconMatch1.getType(), lexiconMatch1.getStartIndex(), (lexiconMatch1.size() + lexiconMatch2.size()));
					lexiconMatches.set(li, joint);
					lexiconMatches.remove(li + 1);
				}
			}
			
			//	proceed to next annotation
			else li++;
		}
		
		//	add lexicon matches to document
		for (int m = 0; m < lexiconMatches.size(); m++) {
			Annotation lexiconMatch = ((Annotation) lexiconMatches.get(m));
			
			//	ambigous word, make it a candidate
//			if ((lexiconMatch.size() == 1) && FAT.negativesDictionary.containsIgnoreCase(lexiconMatch.getValue())) {
			if ((lexiconMatch.size() == 1) && FAT.negativesDictionary.lookup(lexiconMatch.getValue(), false)) {
				System.out.println("LexionRules: ambiguous single word match - '" + lexiconMatch.getValue() + "'");
				data.addAnnotation(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE, lexiconMatch.getStartIndex(), lexiconMatch.size());
			}
			
			//	sure positive
			else {
				lexiconMatch = data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, lexiconMatch.getStartIndex(), lexiconMatch.size());
				lexiconMatch.setAttribute(FAT.EVIDENCE_ATTRIBUTE, "lexicon");
			}
		}
		
		//	remove duplicates
		AnnotationFilter.removeInner(data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		AnnotationFilter.removeByContained(data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE, false);
//		
//		//	add lexicon matches to document
//		for (int m = 0; m < lexiconMatches.size(); m++) {
//			Annotation lexiconMatch = ((Annotation) lexiconMatches.get(m));
//			lexiconMatch = data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, lexiconMatch.getStartIndex(), lexiconMatch.size());
//			lexiconMatch.setAttribute(FAT.EVIDENCE_ATTRIBUTE, "lexicon");
//		}
//		
//		//	remove duplicates
//		AnnotationFilter.removeInner(data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
	}
	
	private boolean abbreviationLookup(StringVector data, String abbreviation) {
		for (int d = 0; d < data.size(); d++)
			if (data.get(d).startsWith(abbreviation)) return true;
		return false;
	}
	
	private void initDictionary() {
		this.lowerCaseDictionary = new Dictionary() {
			public StringIterator getEntryIterator() {
				return FAT.lowerCaseParts.getEntryIterator();
			}
			public boolean isDefaultCaseSensitive() {
				return true;
			}
			public boolean isEmpty() {
				return false;
			}
			public boolean lookup(String string, boolean caseSensitive) {
				return this.lookup(string);
			}
			public boolean lookup(String string) {
				if (FAT.forbiddenWordsLowerCase.contains(string)) return false;
				else if (FAT.forbiddenWordsUpperCase.contains(string)) return false;
				else if (FAT.negatives.containsIgnoreCase(string)) return false;
				else return FAT.lowerCaseParts.contains(string);
			}
			public int size() {
				return FAT.lowerCaseParts.size();
			}
		};
	}
}
