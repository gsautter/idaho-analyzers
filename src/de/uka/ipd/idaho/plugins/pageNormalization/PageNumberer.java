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

package de.uka.ipd.idaho.plugins.pageNormalization;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * @author sautter
 *
 */
public class PageNumberer extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String PAGE_NUMBER_CANDIDATE_TYPE = (PAGE_NUMBER_TYPE + "Candidate");
	private static final boolean DEBUG_PAGE_NUMBERS = true;
	
	private static final int maxPageNumberDigits = 5;
	
	private static final void getAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length) {
			StringBuffer value = new StringBuffer();
			for (int d = 0; d < digits.length; d++)
				value.append("" + digits[d]);
			values.add(value.toString());
		}
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			getAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	private static class PageNumber {
		final int pageId;
		final int value;
		final int fuzzyness;
		final int ambiguity;
		final Annotation annotation;
		int score = 0;
		//	!!! this constructor is for set lookups only !!!
		PageNumber(int value) {
			this.value = value;
			this.pageId = -1;
			this.fuzzyness = 1;
			this.ambiguity = 1;
			this.annotation = null;
		}
		PageNumber(int pageId, int value, int fuzzyness, int ambiguity, Annotation annotation) {
			this.value = value;
			this.pageId = pageId;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
			this.annotation = annotation;
		}
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
		int getAdjustedScore() {
			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
		}
	}
	
	private String pageNumberPattern;
	private HashMap pageNumberCharacterTranslations = new HashMap();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load character mappings
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() != 0) && !characterLine.startsWith("//")) {
					int split = characterLine.indexOf(' ');
					if (split == -1)
						continue;
					String digit = characterLine.substring(0, split).trim();
					String characters = characterLine.substring(split).trim();
					if (Gamta.isNumber(digit)) {
						for (int c = 0; c < characters.length(); c++) {
							String character = characters.substring(c, (c+1));
							pageNumberCharacters.add(character);
							ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(character));
							if (characterTranslations == null) {
								characterTranslations = new ArrayList(2);
								this.pageNumberCharacterTranslations.put(character, characterTranslations);
							}
							characterTranslations.add(digit);
						}
						pageNumberCharacters.add(digit);
						ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(digit));
						if (characterTranslations == null) {
							characterTranslations = new ArrayList(2);
							this.pageNumberCharacterTranslations.put(digit, characterTranslations);
						}
						characterTranslations.add(digit);
					}
				}
			}
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer pncPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String pnc = ((String) cit.next());
				pncPatternBuilder.append(pnc);
				ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(pnc));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.pageNumberCharacterTranslations.put(pnc, cts);
			}
			String pncPattern = ("[" + RegExUtils.escapeForRegEx(pncPatternBuilder.toString()) + "]");
			this.pageNumberPattern = (pncPattern + "+(\\s?" + pncPattern + "+)*");
		}
		catch (IOException ioe) {
			this.pageNumberPattern = "[0-9]++";
		}
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	process each page & collect page number data
		int[] pageIds = new int[pages.length];
		HashMap[] pageNumbers = new HashMap[pages.length];
		for (int p = 0; p < pages.length; p++) {
			
			//	store page ID
			pageIds[p] = Integer.parseInt((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE, ("" + p)));
			
			//	get all candidates
			ArrayList pageNumberList = new ArrayList();
			Annotation[] pageNumberCandidates = Gamta.extractAllMatches(pages[p], this.pageNumberPattern, 3, true, false, true);
			for (int n = 0; n < pageNumberCandidates.length; n++) {
				
				//	use only top and bottom of page
				if ((pageNumberCandidates[n].getStartIndex() > 25) && (pageNumberCandidates[n].getEndIndex() < (pages[p].size() - 25)))
					continue;
				
				if (DEBUG_PAGE_NUMBERS)
					System.out.println("Possible page number on page " + pageIds[p] + ": " + pageNumberCandidates[n].getValue());
				
				//	extract possible interpretations, computing fuzzyness and ambiguity along the way
				String pageNumberString = pageNumberCandidates[n].getValue().replaceAll("\\s++", "");
				int [][] pageNumberDigits = new int[pageNumberString.length()][];
				int ambiguity = 1;
				for (int c = 0; c < pageNumberString.length(); c++) {
					String pnc = pageNumberString.substring(c, (c+1));
					pageNumberDigits[c] = ((int[]) this.pageNumberCharacterTranslations.get(pnc));
					ambiguity *= pageNumberDigits[c].length;
				}
				
				//	the first digit is never zero ...
				if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0)) {
					if (DEBUG_PAGE_NUMBERS)
						System.out.println(" ==> ignoring zero start");
					continue;
				}
				
				//	page numbers with six or more digits are rather improbable ...
				if (pageNumberDigits.length > maxPageNumberDigits) {
					if (DEBUG_PAGE_NUMBERS)
						System.out.println(" ==> ignoring for over-length");
					continue;
				}
				
				//	set type and base attributes
				pageNumberCandidates[n].changeTypeTo(PAGE_NUMBER_CANDIDATE_TYPE);
				
				//	this one is clear, annotate it right away
				if (ambiguity == 1) {
					StringBuffer pageNumberValue = new StringBuffer();
					for (int d = 0; d < pageNumberDigits.length; d++)
						pageNumberValue.append("" + pageNumberDigits[d][0]);
					pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), 0, ambiguity, pageNumberCandidates[n]));
					if (DEBUG_PAGE_NUMBERS)
						System.out.println(" ==> value is " + pageNumberValue.toString());
				}
				
				//	deal with ambiguity
				else {
					TreeSet values = new TreeSet();
					int[] digits = new int[pageNumberDigits.length];
					getAllPossibleValues(pageNumberDigits, digits, 0, values);
					for (Iterator vit = values.iterator(); vit.hasNext();) {
						String pageNumberValue = ((String) vit.next());
						int fuzzyness = 0;
						for (int d = 0; d < pageNumberValue.length(); d++) {
							if (pageNumberValue.charAt(d) != pageNumberString.charAt(d))
								fuzzyness++;
						}
						pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue), fuzzyness, ambiguity, pageNumberCandidates[n]));
						if (DEBUG_PAGE_NUMBERS)
							System.out.println(" ==> possible value is " + pageNumberValue);
					}
				}
			}
			
			//	aggregate page numbers
			Collections.sort(pageNumberList, new Comparator() {
				public int compare(Object o1, Object o2) {
					PageNumber pn1 = ((PageNumber) o1);
					PageNumber pn2 = ((PageNumber) o2);
					return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
				}
			});
			pageNumbers[p] = new HashMap();
			for (Iterator pnit = pageNumberList.iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				PageNumber apn = ((PageNumber) pageNumbers[p].get(pn));
				if ((apn == null) || (pn.fuzzyness < apn.fuzzyness))
					pageNumbers[p].put(pn, pn);
			}
		}
		
		//	score page numbers
		for (int p = 0; p < pages.length; p++) {
			for (Iterator pnit = pageNumbers[p].keySet().iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (DEBUG_PAGE_NUMBERS)
					System.out.println("Scoring page number " + pn + " for page " + pn.pageId);
				
				//	punish own fuzzyness
				pn.score -=pn.fuzzyness;
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pages.length; l++) {
					PageNumber cpn = ((PageNumber) pageNumbers[p+l].get(new PageNumber(pn.value + (pageIds[p+l] - pageIds[p]))));
					if (cpn != null) {
						pn.score += l;
						pn.score -= cpn.fuzzyness;
					}
					else {
						fMisses++;
						if (fMisses == 3)
							l = pages.length;
					}
				}
				
				//	look backward
				int bMisses = 0;
				for (int l = 1; l <= p; l++) {
					PageNumber cpn = ((PageNumber) pageNumbers[p-l].get(new PageNumber(pn.value + (pageIds[p-l] - pageIds[p]))));
					if (cpn != null) {
						pn.score += l;
						pn.score -= cpn.fuzzyness;
					}
					else {
						bMisses++;
						if (bMisses == 3)
							l = (p+1);
					}
				}
				
				if (DEBUG_PAGE_NUMBERS)
					System.out.println(" ==> score is " + pn.score);
			}
		}
		
		//	select page number for each page
		PageNumber[] selectedPageNumbers = new PageNumber[pages.length];
		for (int p = 0; p < pages.length; p++) {
			int bestPageNumberScore = 0;
			for (Iterator pnit = pageNumbers[p].keySet().iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (pn.score > bestPageNumberScore) {
					selectedPageNumbers[p] = pn;
					bestPageNumberScore = pn.score;
				}
			}
			if (DEBUG_PAGE_NUMBERS) {
				if (selectedPageNumbers[p] == null)
					System.out.println("Could not determine page number of page " + pageIds[p] + ".");
				else System.out.println("Determined page number of page " + pageIds[p] + " as " + selectedPageNumbers[p] + " (score " + selectedPageNumbers[p].score + ")");
			}
		}
		
		//	fill in sequence gaps
		for (int p = 0; p < pages.length; p++) {
			if (selectedPageNumbers[p] == null)
				selectedPageNumbers[p] = new PageNumber(pageIds[p], -1, Integer.MAX_VALUE, Integer.MAX_VALUE, null);
		}
		
		//	do sequence base correction
		for (int p = 1; p < (pages.length - 1); p++) {
			if (selectedPageNumbers[p+1].isConsistentWith(selectedPageNumbers[p-1]) && !selectedPageNumbers[p+1].isConsistentWith(selectedPageNumbers[p])) {
				int beforeScore = selectedPageNumbers[p-1].score;
				int ownScore = selectedPageNumbers[p].score;
				int afterScore = selectedPageNumbers[p+1].score;
				if ((beforeScore + afterScore) > (ownScore * 3)) {
					selectedPageNumbers[p] = new PageNumber(selectedPageNumbers[p].pageId, (selectedPageNumbers[p-1].value + (selectedPageNumbers[p].pageId - selectedPageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE, selectedPageNumbers[p].annotation);
					selectedPageNumbers[p].score = ((beforeScore + afterScore) / 3);
					if (DEBUG_PAGE_NUMBERS)
						System.out.println("Corrected page number of page " + pageIds[p] + " to " + selectedPageNumbers[p] + " (score " + ((beforeScore + afterScore) / 2) + " over " + ownScore + ")");
				}
			}
		}
		
		//	do backward sequence extrapolation
		for (int p = (pages.length - 2); p >= 0; p--) {
			if (!selectedPageNumbers[p+1].isConsistentWith(selectedPageNumbers[p])) {
				int ownScore = selectedPageNumbers[p].score;
				int afterScore = selectedPageNumbers[p+1].score;
				if (afterScore > (ownScore * 2)) {
					selectedPageNumbers[p] = new PageNumber(selectedPageNumbers[p].pageId, (selectedPageNumbers[p+1].value - (selectedPageNumbers[p+1].pageId - selectedPageNumbers[p].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE, selectedPageNumbers[p].annotation);
					selectedPageNumbers[p].score = (afterScore / 2);
					if (DEBUG_PAGE_NUMBERS)
						System.out.println("Extrapolated (backward) page number of page " + pageIds[p] + " to " + selectedPageNumbers[p] + " (score " + afterScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	do forward sequence extrapolation
		for (int p = 1; p < pages.length; p++) {
			if (!selectedPageNumbers[p].isConsistentWith(selectedPageNumbers[p-1])) {
				int beforeScore = selectedPageNumbers[p-1].score;
				int ownScore = selectedPageNumbers[p].score;
				if (beforeScore > (ownScore * 2)) {
					selectedPageNumbers[p] = new PageNumber(selectedPageNumbers[p].pageId, (selectedPageNumbers[p-1].value + (selectedPageNumbers[p].pageId - selectedPageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE, selectedPageNumbers[p].annotation);
					selectedPageNumbers[p].score = (beforeScore / 2);
					if (DEBUG_PAGE_NUMBERS)
						System.out.println("Extrapolated (forward) page number of page " + pageIds[p] + " to " + selectedPageNumbers[p] + " (score " + beforeScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	disambiguate page numbers that occur on multiple pages (using fuzzyness and ambiguity)
		HashMap allPageNumbers = new HashMap();
		for (int p = 0; p < pages.length; p++) {
			if (allPageNumbers.containsKey(selectedPageNumbers[p])) {
				PageNumber pn = ((PageNumber) allPageNumbers.get(selectedPageNumbers[p]));
				if (pn.getAdjustedScore() < selectedPageNumbers[p].getAdjustedScore()) {
					if (DEBUG_PAGE_NUMBERS)
						System.out.println("Ousted page number " + pn.value + " of page " + pn.pageId + " (score " + pn.getAdjustedScore() + " against " + selectedPageNumbers[p].getAdjustedScore() + ")");
					allPageNumbers.put(selectedPageNumbers[p], selectedPageNumbers[p]);
					pn.score = 0;
					selectedPageNumbers[pn.pageId] = new PageNumber(pn.pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE, null);
				}
				else {
					if (DEBUG_PAGE_NUMBERS)
						System.out.println("Ousted page number " + selectedPageNumbers[p].value + " of page " + selectedPageNumbers[p].pageId + " (score " + selectedPageNumbers[p].getAdjustedScore() + " against " + pn.getAdjustedScore() + ")");
					selectedPageNumbers[p].score = 0;
					selectedPageNumbers[p] = new PageNumber(selectedPageNumbers[p].pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE, null);
				}
			}
			else if (selectedPageNumbers[p].value != -1) allPageNumbers.put(selectedPageNumbers[p], selectedPageNumbers[p]);
		}
		
		//	annotate page numbers and set attributes
		for (int p = 0; p < pages.length; p++) {
			String pageNumberValue = ("" + selectedPageNumbers[p].value);
			
			//	set page attribute
			pages[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberValue);
			
			//	annotate page number
			if (selectedPageNumbers[p].annotation != null) {
				Annotation pageNumber = pages[p].addAnnotation(PAGE_NUMBER_TYPE, selectedPageNumbers[p].annotation.getStartIndex(), selectedPageNumbers[p].annotation.size());
				pageNumber.setAttribute("value", ("" + selectedPageNumbers[p].value));
				pageNumber.setAttribute("score", ("" + selectedPageNumbers[p].score));
				pageNumber.setAttribute("ambiguity", ("" + selectedPageNumbers[p].ambiguity));
				pageNumber.setAttribute("fuzzyness", ("" + selectedPageNumbers[p].fuzzyness));
				if (DEBUG_PAGE_NUMBERS)
					System.out.println("Annotated page number of page " + selectedPageNumbers[p].pageId + " as " + pageNumber.toXML() + " at " + pageNumber.getStartIndex());
			}
			else if (DEBUG_PAGE_NUMBERS)
				System.out.println("Inferred page number of page " + selectedPageNumbers[p].pageId + " as " + selectedPageNumbers[p] + " (score " + selectedPageNumbers[p].score + ")");
			
		}
	}
}