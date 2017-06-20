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

import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.locations.CountryHandler.RegionHandler;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;

/**
 * @author sautter
 *
 */
public class CollectingCountryTagger extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants {
	
	private static final String[] countryNameLanguages = {
		"English",
		"German",
		"French",
		"Italian",
		"Spanish",
		"Portuguese",
		"Russian",
	};
	
	private CountryHandler countryHandler;
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		this.countryHandler = CountryHandler.getCountryHandler(((InputStream) null), countryNameLanguages);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get collection countries
		Annotation[] countries = Gamta.extractAllContained(data, this.countryHandler, false);
		
		//	annotate collecting countries
		TreeSet docCountries = new TreeSet();
		for (int c = 0; c < countries.length; c++) {
			Annotation country = data.addAnnotation(COLLECTING_COUNTRY_ANNOTATION_TYPE, countries[c].getStartIndex(), countries[c].size());
			String countryString = TokenSequenceUtils.concatTokens(countries[c], true, true);
			String countryName = this.countryHandler.getEnglishName(countryString);
			if (countryName != null) {
				country.setAttribute(NAME_ATTRIBUTE, countryName);
				docCountries.add(countryName);
			}
		}
		
		//	remove duplicates
		AnnotationFilter.removeDuplicates(data, COLLECTING_COUNTRY_ANNOTATION_TYPE);
		
		//	annotate collecting regions in detected countries
		for (Iterator cit = docCountries.iterator(); cit.hasNext();) {
			String countryName = ((String) cit.next());
			
			//	get region handler for country
//			RegionHandler cRegionHandler = this.countryHandler.getRegionHandler(countryName, countryNameLanguages);
			RegionHandler cRegionHandler = this.countryHandler.getRegionHandler(countryName);
			
			//	get collection regions
			Annotation[] regions = Gamta.extractAllContained(data, cRegionHandler, false);
			
			//	annotate collecting regions
			for (int c = 0; c < regions.length; c++) {
				Annotation region = data.addAnnotation(COLLECTING_REGION_ANNOTATION_TYPE, regions[c].getStartIndex(), regions[c].size());
				region.setAttribute(COUNTRY_ATTRIBUTE, countryName);
				String regionString = TokenSequenceUtils.concatTokens(regions[c], true, true);
				String regionName = cRegionHandler.getEnglishName(regionString);
				if (regionName != null)
					region.setAttribute(NAME_ATTRIBUTE, regionName);
			}
		}
		
		//	remove duplicates
		AnnotationFilter.removeDuplicates(data, COLLECTING_REGION_ANNOTATION_TYPE);
	}
}
