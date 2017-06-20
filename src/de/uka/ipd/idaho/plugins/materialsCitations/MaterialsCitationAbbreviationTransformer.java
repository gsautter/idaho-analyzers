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

import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.plugins.abbreviationHandling.AbbreviationConstants;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class MaterialsCitationAbbreviationTransformer extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants, AbbreviationConstants {
	
	private CountryHandler countryHandler;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		this.countryHandler = CountryHandler.getCountryHandler(this.dataProvider);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	clone abbreviation references as materials citations
		MutableAnnotation[] abbreviationReferences = data.getMutableAnnotations(ABBREVIATION_REFERENCE_ANNOTATION_TYPE);
		for (int a = 0; a < abbreviationReferences.length; a++)
			this.addMaterialsCitation(abbreviationReferences[a]);
	}
	
	private void addMaterialsCitation(MutableAnnotation abbreviationReference) {
		System.out.println("adding materials citation attributes:");
		
		//	build store for attributes
		Attributed materialsCitationAttributes = new AbstractAttributed();
		
		//	get coordinates
		ImpliedAnnotation[] geoCoordinates = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, GEO_COORDINATE_TYPE);
		float fLong = Float.MAX_VALUE;
		int fLongPrec = -1;
		float fLat = Float.MAX_VALUE;
		int fLatPrec = -1;
		for (int g = 0; g < geoCoordinates.length; g++) {
			Object value = geoCoordinates[g].getAttribute(VALUE_ATTRIBUTE);
			if (value != null) try {
				Object precision = geoCoordinates[g].getAttribute(PRECISION_ATTRIBUTE);
				if (LONGITUDE_ORIENTATION.equals(geoCoordinates[g].getAttribute(ORIENTATION_ATTRIBUTE))) {
					fLong = Float.parseFloat(value.toString());
					if (precision != null)
						fLongPrec = Integer.parseInt(precision.toString());
				}
				else if (LATITUDE_ORIENTATION.equals(geoCoordinates[g].getAttribute(ORIENTATION_ATTRIBUTE))) {
					fLat = Float.parseFloat(value.toString());
					if (precision != null)
						fLatPrec = Integer.parseInt(precision.toString());
				}
			} catch (NumberFormatException nfe) {}
		}
		
		//	compute overall precision
		int fLongLatPrec = -1;
		if ((fLongPrec != -1) && (fLatPrec != -1)) {
			
			//	normalize longitude precision based on latitude
			fLongPrec = ((int) Math.round(fLongPrec * Math.cos(fLat * (Math.PI / 2) / 90)));
			
			//	compute radius of circle around bounding box
			fLongLatPrec = ((int) Math.round(Math.sqrt(fLongPrec * fLongPrec + fLatPrec * fLatPrec)));
		}
		
		//	create attribute objects
		Object longitude = ((fLong == Float.MAX_VALUE) ? null : ("" + fLong));
		Object latitude = ((fLat == Float.MAX_VALUE) ? null : ("" + fLat));
		Object longLatPrecision = ((fLongLatPrec == -1) ? null : ("" + fLongLatPrec));
		
		//	reconcile coordinates and precision with collecting event
		if (materialsCitationAttributes.hasAttribute(LONGITUDE_ATTRIBUTE))
			longitude = materialsCitationAttributes.getAttribute(LONGITUDE_ATTRIBUTE);
		else if (longitude != null)
			materialsCitationAttributes.setAttribute(LONGITUDE_ATTRIBUTE, longitude);
		
		if (materialsCitationAttributes.hasAttribute(LATITUDE_ATTRIBUTE))
			latitude = materialsCitationAttributes.getAttribute(LATITUDE_ATTRIBUTE);
		else if (latitude != null)
			materialsCitationAttributes.setAttribute(LATITUDE_ATTRIBUTE, latitude);
		
		if (materialsCitationAttributes.hasAttribute(LONG_LAT_PRECISION_ATTRIBUTE))
			longLatPrecision = materialsCitationAttributes.getAttribute(LONG_LAT_PRECISION_ATTRIBUTE);
		else if (longLatPrecision != null)
			materialsCitationAttributes.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, longLatPrecision);
		
		//	get elevation
		this.addDetailAttribute(abbreviationReference, materialsCitationAttributes, ELEVATION_ATTRIBUTE);
		
		//	get country, state/province, and county
		ImpliedAnnotation[] collectingCountries = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, COLLECTING_COUNTRY_ANNOTATION_TYPE);
		Object country = null;
		if (collectingCountries.length != 0) {
			country = TokenSequenceUtils.concatTokens(collectingCountries[0], true, true);
			if (this.countryHandler != null)
				country = this.countryHandler.getEnglishName((String) country); 
			materialsCitationAttributes.setAttribute(COUNTRY_ATTRIBUTE, country);
			//	TODOne normalize country name to American English
		}
		else if (materialsCitationAttributes.hasAttribute(COUNTRY_ATTRIBUTE)) {
			country = materialsCitationAttributes.getAttribute(COUNTRY_ATTRIBUTE);
			if (this.countryHandler != null)
				country = this.countryHandler.getEnglishName(country.toString()); 
			//	TODOne normalize country name to American English
		}
		
		ImpliedAnnotation[] collectingRegions = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, COLLECTING_REGION_ANNOTATION_TYPE);
		Object region = null;
		if (collectingRegions.length != 0) {
			region = TokenSequenceUtils.concatTokens(collectingRegions[0], true, true);
			materialsCitationAttributes.setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
		}
		else if (materialsCitationAttributes.hasAttribute(STATE_PROVINCE_ATTRIBUTE))
			region = materialsCitationAttributes.getAttribute(STATE_PROVINCE_ATTRIBUTE);
		
		ImpliedAnnotation[] collectingCounties = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, COLLECTING_COUNTY_ANNOTATION_TYPE);
		Object county = null;
		if (collectingCounties.length != 0) {
			county = TokenSequenceUtils.concatTokens(collectingCounties[0], true, true);
			materialsCitationAttributes.setAttribute(COUNTY_ATTRIBUTE, county);
		}
		else if (materialsCitationAttributes.hasAttribute(COUNTY_ATTRIBUTE))
			county = materialsCitationAttributes.getAttribute(COUNTY_ATTRIBUTE);
		
		
		//	get and transfer locations
		ImpliedAnnotation[] locations = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, LOCATION_TYPE);
		for (int l = 0; l < locations.length; l++) {
			Annotation location = abbreviationReference.addAnnotation(LOCATION_TYPE, 0, abbreviationReference.size());
			if (longitude != null)
				location.setAttribute(LONGITUDE_ATTRIBUTE, longitude);
			if (latitude != null)
				location.setAttribute(LATITUDE_ATTRIBUTE, latitude);
			if (longLatPrecision != null)
				location.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, longLatPrecision);
			
			if (country != null)
				location.setAttribute(COUNTRY_ATTRIBUTE, country);
			if (region != null)
				location.setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
			if (county != null)
				location.setAttribute(COUNTY_ATTRIBUTE, county);
			
			location.setAttribute(NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(locations[l], true, true));
		}
		
		//	set location attribute (substitute with lowest available order geographical name)
		System.out.println(" - got " + locations.length + " locations");
		if (locations.length != 0) {
			materialsCitationAttributes.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(locations[0], true, true));
			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(locations[0], true, true) + "'");
		}
		else if (collectingCounties.length != 0) {
			materialsCitationAttributes.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingCounties[0], true, true));
			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingCounties[0], true, true) + "'");
		}
		else if (collectingRegions.length != 0) {
			materialsCitationAttributes.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingRegions[0], true, true));
			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingRegions[0], true, true) + "'");
		}
		else if (collectingCountries.length != 0) {
			materialsCitationAttributes.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingCountries[0], true, true));
			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingCountries[0], true, true) + "'");
		}
		
		//	get collector's name and collecting methods
		this.addDetailAttribute(abbreviationReference, materialsCitationAttributes, COLLECTOR_NAME);
		
		this.addDetailAttribute(abbreviationReference, materialsCitationAttributes, COLLECTING_METHOD);
		
		//	get collecting date
		ImpliedAnnotation[] collectingDates = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, COLLECTING_DATE);
		StringVector collectingDateValues = new StringVector();
		for (int d = 0; d < collectingDates.length; d++) {
			if (collectingDates[d].hasAttribute(VALUE_ATTRIBUTE))
				collectingDateValues.addElementIgnoreDuplicates((String) collectingDates[d].getAttribute(VALUE_ATTRIBUTE));
			if (collectingDates[d].hasAttribute(VALUE_ATTRIBUTE + "Min"))
				collectingDateValues.addElementIgnoreDuplicates((String) collectingDates[d].getAttribute(VALUE_ATTRIBUTE + "Min"));
			if (collectingDates[d].hasAttribute(VALUE_ATTRIBUTE + "Max"))
				collectingDateValues.addElementIgnoreDuplicates((String) collectingDates[d].getAttribute(VALUE_ATTRIBUTE + "Max"));
		}
		collectingDateValues.sortLexicographically(false, false);
		if (collectingDateValues.size() > 0) {
			materialsCitationAttributes.setAttribute(COLLECTING_DATE, collectingDateValues.firstElement());
			if (collectingDateValues.size() > 1) {
				materialsCitationAttributes.setAttribute((COLLECTING_DATE + "Min"), collectingDateValues.firstElement());
				materialsCitationAttributes.setAttribute((COLLECTING_DATE + "Max"), collectingDateValues.lastElement());
			}
		}
		if ((collectingDates.length  != 0) && !materialsCitationAttributes.hasAttribute(COLLECTING_DATE)) {
			materialsCitationAttributes.setAttribute(COLLECTING_DATE, TokenSequenceUtils.concatTokens(collectingDates[0], true, true));
			if (collectingDates.length > 1) {
				materialsCitationAttributes.setAttribute((COLLECTING_DATE + "Min"), TokenSequenceUtils.concatTokens(collectingDates[0], true, true));
				materialsCitationAttributes.setAttribute((COLLECTING_DATE + "Max"), TokenSequenceUtils.concatTokens(collectingDates[collectingDates.length-1], true, true));
			}
		}
		
		//	get collection code and specimen code
		this.addDetailAttribute(abbreviationReference, materialsCitationAttributes, COLLECTION_CODE);
		
		this.addDetailAttribute(abbreviationReference, materialsCitationAttributes, SPECIMEN_CODE);
		
		//	substitute specimen code prefixes for missing collection code
		if (materialsCitationAttributes.hasAttribute(SPECIMEN_CODE) && !materialsCitationAttributes.hasAttribute(COLLECTION_CODE)) {
			StringVector collectionCodes = new StringVector();
			ImpliedAnnotation[] specimenCodes = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, SPECIMEN_CODE);
			for (int c = 0; c < specimenCodes.length; c++) {
				String collectionCode = specimenCodes[c].firstValue();
				while (collectionCode.endsWith("-"))
					collectionCode = collectionCode.substring(0, (collectionCode.length() - 1)).trim();
				if (collectionCode.matches("[A-Z]++"))
					collectionCodes.addElementIgnoreDuplicates(collectionCode);
			}
			if (collectionCodes.size() != 0)
				materialsCitationAttributes.setAttribute(COLLECTION_CODE, collectionCodes.concatStrings(", "));
		}
		
		//	get type status
		ImpliedAnnotation[] typeStatusses = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, TYPE_STATUS);
		if (typeStatusses.length != 0)
			materialsCitationAttributes.setAttribute(TYPE_STATUS, TokenSequenceUtils.concatTokens(typeStatusses[0], true, true));
		
		//	annotate materials citation only if attributes to transfer
		if (materialsCitationAttributes.getAttributeNames().length == 0)
			return;
		
		//	finally ...
		Annotation materialsCitation = abbreviationReference.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, 0, abbreviationReference.size());
		materialsCitation.copyAttributes(materialsCitationAttributes);
	}
	
	private void addDetailAttribute(Annotation abbreviationReference, Attributed materialsCitation, String type) {
		StringVector attribute = new StringVector();
		Annotation[] attributeParts = ImpliedAnnotation.getImpliedAnnotations(abbreviationReference, type);
		for (int p = 0; p < attributeParts.length; p++)
			attribute.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(attributeParts[p], true, true));
		if (attribute.size() != 0)
			materialsCitation.setAttribute(type, attribute.concatStrings(", "));
	}
}
