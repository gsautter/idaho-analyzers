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

package de.uka.ipd.idaho.plugins.materialsCitations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class CollectingCountryFilterer extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants {
	
	private TreeMap watchList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeMap blackList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	
	private String properNamePartPattern = "(([A-Z][a-z]*\\'?)?[A-Z][a-z\\-]+)(\\-([A-Z][a-z]*\\'?)?[A-Z][a-z\\-]+)?";
	private StringVector properNameInfixes = new StringVector(false);
	private StringVector properNameStopWords = new StringVector(false);
	
	//	TODO include this sucker in automated materials citation pipeline, right after tagger
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load stop word list
		if (this.dataProvider.isDataAvailable("properNames.stopwords.list.txt")) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("properNames.stopwords.list.txt")));
			for (String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (line.startsWith("//"))
					continue;
				this.properNameStopWords.addElementIgnoreDuplicates(line);
			}
			br.close();
		} catch (IOException ioe) {}
		
		//	load proper name infix list (most likely take from TreeFAT and/or RefParse)
		if (this.dataProvider.isDataAvailable("properNames.infixes.list.txt")) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("properNames.infixes.list.txt")));
			for (String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (line.startsWith("//"))
					continue;
				this.properNameInfixes.addElementIgnoreDuplicates(line);
			}
			br.close();
		} catch (IOException ioe) {}
		
		/*	load country black list (mapped to explanations):
		 * - 'O' (Iceland in Icelandic)
		 * - 'Male' (Malta in Walloon)
		 * - TODO extend list as cases are reported
		 */
		if (this.dataProvider.isDataAvailable("collectingCountry.remove.list.txt")) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("collectingCountry.remove.list.txt")));
			for (String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (line.startsWith("//"))
					continue;
				int tabIndex = line.indexOf('\t');
				if (tabIndex == -1)
					continue;
				String removeCountry = line.substring(0, tabIndex).trim();
				String removeExplanation = line.substring(tabIndex + "\t".length()).trim();
				this.blackList.put(removeCountry, removeExplanation);
			}
			br.close();
		} catch (IOException ioe) {}
		
		/*	load country watch list (mapped to explanations):
		 * - 'Island' (Iceland in German)
		 * - 'Ilha' (Iceland in Portuguese)
		 * - TODO check country handler base XML file for more examples
		 * - TODO extend list as cases are reported
		 */
		if (this.dataProvider.isDataAvailable("collectingCountry.watch.list.txt")) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("collectingCountry.watch.list.txt")));
			for (String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (line.startsWith("//"))
					continue;
				int tabIndex = line.indexOf('\t');
				if (tabIndex == -1)
					continue;
				String watchCountry = line.substring(0, tabIndex).trim();
				String watchExplanation = line.substring(tabIndex + "\t".length()).trim();
				this.watchList.put(watchCountry, watchExplanation);
			}
			br.close();
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get marked collection countries
		Annotation[] countries = data.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		
		//	anything to do?
		if (countries.length == 0)
			return;
		
		//	remove countries on black list
		ArrayList blackListCountries = new ArrayList();
		ArrayList watchListCountries = new ArrayList();
		for (int c = 0; c < countries.length; c++) {
			String country = countries[c].getValue();
			if (this.blackList.containsKey(country)) {
				System.out.println("Removing black-listed country '" + country + "' (" + this.blackList.get(country) + ")");
				blackListCountries.add(countries[c]);
				data.removeAnnotation(countries[c]);
			}
			else if (country.length() == 1) {
				System.out.println("Removing single-letter country '" + country + "'");
				blackListCountries.add(countries[c]);
				data.removeAnnotation(countries[c]);
			}
			else if ((country.length() < 4) && !country.matches("[A-Z]+")) {
				System.out.println("Removing short non-acronym country '" + country + "'");
				blackListCountries.add(countries[c]);
				data.removeAnnotation(countries[c]);
			}
			else if (this.watchList.containsKey(country)) {
				System.out.println("Double-checking watch-listed country '" + country + "' (" + this.watchList.get(country) + ")");
				watchListCountries.add(countries[c]);
			}
		}
		
		//	anything to check at all?
		if (watchListCountries.size() == 0)
			return;
		
		//	anything removed thus far?
		if (blackListCountries.size() != 0)
			countries = data.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		
		//	index proper name parts
		AnnotationIndex properNamePartIndex = new AnnotationIndex();
		Annotation[] properNameParts = Gamta.extractAllMatches(data, this.properNamePartPattern, this.properNameStopWords, this.properNameStopWords);
		properNamePartIndex.addAnnotations(properNameParts, "part");
		Annotation[] properNameInfixes = Gamta.extractAllContained(data, this.properNameInfixes);
		properNamePartIndex.addAnnotations(properNameInfixes, "infix");
		
		//	use annotation pattern to mark all proper names in full (always extend only by one part to prevent combinatoric explosion)
		ProperNameIndex properNamesByIndices = new ProperNameIndex();
		while (true) {
			boolean properNamesStable = true;
			Annotation[] properNames = AnnotationPatternMatcher.getMatches(data, properNamePartIndex,
					"<part> <infix>* <part>");
			properNamePartIndex.addAnnotations(properNames, "part");
			for (int p = 0; p < properNames.length; p++)
				for (int t = properNames[p].getStartIndex(); t < properNames[p].getEndIndex(); t++) {
					if (properNamesByIndices.getProperNamesAt(t, true).add(properNames[p]))
						properNamesStable = false;
				}
			if (properNamesStable)
				break;
		}
		
		//	remove countries that are embedded in a larger proper name
		for (int c = 0; c < watchListCountries.size(); c++) {
			Annotation country = ((Annotation) watchListCountries.get(c));
			for (int t = country.getStartIndex(); t < country.getEndIndex(); t++) {
				TreeSet properNames = properNamesByIndices.getProperNamesAt(t, false);
				if (properNames == null)
					continue; // should not happen, but some country names might fail to be tagged as proper names (e.g. 'U.S.A.')
				for (Iterator pnit = properNames.iterator(); pnit.hasNext();) {
					Annotation properName = ((Annotation) pnit.next());
					if (properName.size() > country.size()) {
						System.out.println("Removing watch-listed country '" + country + "' (embedded in " + properName.getValue() + ")");
						t = country.getEndIndex(); // breaks token index loop
						data.removeAnnotation(country);
						break; // break iterator loop
					}
				}
			}
		}
		
		//	collect remaining countries (normalized)
		TreeSet countryNames = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		countries = data.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		for (int c = 0; c < countries.length; c++) {
			if (countries[c].hasAttribute(NAME_ATTRIBUTE))
				countryNames.add(countries[c].getAttribute(NAME_ATTRIBUTE));
		}
		
		//	get regions
		Annotation[] regions = data.getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	anything to do?
		if (regions.length == 0)
			return;
		
		//	remove regions whose country is not in the document
		for (int r = 0; r < regions.length; r++) {
			String rCountry = ((String) regions[r].getAttribute(COUNTRY_ATTRIBUTE));
			if (rCountry == null)
				continue; // tagged by something else than our tagger
			if (rCountry.trim().length() == 0)
				continue; // something's hinky, be safe
			if (countryNames.contains(rCountry))
				continue; // looks legitimate
			System.out.println("Removing region '" + regions[r].getValue() + "' (country '" + rCountry + "' not present in document)");
			data.removeAnnotation(regions[r]);
		}
	}
	
	private static class ProperNameIndex extends HashMap {
		TreeSet getProperNamesAt(int index, boolean create) {
			TreeSet properNames = ((TreeSet) this.get(new Integer(index)));
			if ((properNames == null) && create) {
				properNames = new TreeSet(AnnotationUtils.ANNOTATION_NESTING_ORDER);
				this.put(new Integer(index), properNames);
			}
			return properNames;
		}
	}
}