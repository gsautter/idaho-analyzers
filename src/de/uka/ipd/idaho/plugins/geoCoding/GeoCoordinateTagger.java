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

package de.uka.ipd.idaho.plugins.geoCoding;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Analyzer for finding in-text geographic coordinates and translating them to
 * floating point longitude and latitude values
 * 
 * @author sautter
 */
public class GeoCoordinateTagger extends AbstractConfigurableAnalyzer implements LocationConstants {
	
	//	precision attribute & optimum value
	private static final int OPTIMAL_PRECISION = 1;
	
	//	annotation type for numeric coordinates (without direction indicator)
	private static final String NUMERIC_GEO_COORDINATE_TYPE = "numericGeoCoordinate";
	
	//	attribute for coordinate style
	private static final String NUMERIC_STYLE_ATTRIBUTE = "directionIndicator";
	
	//	direction indicator management attributes
	private static final String DIRECTION_INDICATOR_TYPE = "directionIndicator";
	private static final String DIRECTION_INDICATOR_POSITION_ATTRIBUTE = "directionIndicatorPosition";
	
	//	patterns for numerical degrees
	private StringVector degPatterns = new StringVector();
	
	//	patterns for coordinates in degrees and minutes
	private StringVector degMinPatterns = new StringVector();
	
	//	patters for coordinates in degrees, minutes and seconds
	private StringVector degMinSecPatterns = new StringVector();
	
	//	patterns for direction indicators
	private StringVector directionIndicatorPatterns = new StringVector();
	
	//	rule-based dictionary pretending to contain every word
	private Dictionary allWords = new Dictionary() {
		public StringIterator getEntryIterator() {
			return new StringIterator() {
				public boolean hasMoreStrings() {
					return false;
				}
				public String nextString() {
					return null;
				}
				public boolean hasNext() {
					return false;
				}
				public Object next() {
					return null;
				}
				public void remove() {}
			};
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return true;
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return ((string.length() > 1) && (Gamta.LETTERS.indexOf(string.charAt(0)) != -1));
		}
		public boolean lookup(String string) {
			return ((string.length() > 1) && (Gamta.LETTERS.indexOf(string.charAt(0)) != -1));
		}
		public int size() {
			return 0;
		}
	};
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			this.degPatterns = this.loadList("DegPatterns.txt");
		} catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		try {
			this.degMinPatterns = this.loadList("DegMinPatterns.txt");
		} catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		try {
			this.degMinSecPatterns = this.loadList("DegMinSecPatterns.txt");
		} catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		try {
			this.directionIndicatorPatterns = this.loadList("DirectionIndicatorPatterns.txt");
		} catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#loadList(java.lang.String)
	 */
	protected StringVector loadList(String listName) throws IOException {
		InputStream is = this.dataProvider.getInputStream(listName);
		StringVector list = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
		is.close();
		for (int e = 0; e < list.size(); e++) {
			if (list.get(e).startsWith("//"))
				list.remove(e--);
		}
		return list;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	collect plain numeric coordinates in list first
		ArrayList numericGeoCoordinateList = new ArrayList();
		HashSet numericGeoCoordinateSet = new HashSet();
		
		//	tag coordinates in degrees, minutes and seconds
		for (int p = 0; p < this.degMinSecPatterns.size(); p++) {
			Annotation[] numericGeoCoordinates = Gamta.extractAllMatches(data, this.degMinSecPatterns.get(p), 7, this.allWords, this.allWords);
			for (int c = 0; c < numericGeoCoordinates.length; c++)
				if (numericGeoCoordinateSet.add(numericGeoCoordinates[c].getStartIndex() + "-" + numericGeoCoordinates[c].getEndIndex())) {
					numericGeoCoordinates[c].changeTypeTo(NUMERIC_GEO_COORDINATE_TYPE);
					numericGeoCoordinates[c].setAttribute(NUMERIC_STYLE_ATTRIBUTE, "DMS");
					numericGeoCoordinateList.add(numericGeoCoordinates[c]);
				}
		}
		
		//	tag coordinates in degrees and minutes
		for (int p = 0; p < this.degMinPatterns.size(); p++) {
			Annotation[] numericGeoCoordinates = Gamta.extractAllMatches(data, this.degMinPatterns.get(p), 5, this.allWords, this.allWords);
			for (int c = 0; c < numericGeoCoordinates.length; c++)
				if (numericGeoCoordinateSet.add(numericGeoCoordinates[c].getStartIndex() + "-" + numericGeoCoordinates[c].getEndIndex())) {
					numericGeoCoordinates[c].changeTypeTo(NUMERIC_GEO_COORDINATE_TYPE);
					numericGeoCoordinates[c].setAttribute(NUMERIC_STYLE_ATTRIBUTE, "DM");
					numericGeoCoordinateList.add(numericGeoCoordinates[c]);
				}
		}
		
		//	tag coordinates in plain degrees
		for (int p = 0; p < this.degPatterns.size(); p++) {
			Annotation[] numericGeoCoordinates = Gamta.extractAllMatches(data, this.degPatterns.get(p), 3, this.allWords, this.allWords);
			for (int c = 0; c < numericGeoCoordinates.length; c++)
				if (numericGeoCoordinateSet.add(numericGeoCoordinates[c].getStartIndex() + "-" + numericGeoCoordinates[c].getEndIndex())) {
					numericGeoCoordinates[c].changeTypeTo(NUMERIC_GEO_COORDINATE_TYPE);
					numericGeoCoordinates[c].setAttribute(NUMERIC_STYLE_ATTRIBUTE, "D");
					numericGeoCoordinateList.add(numericGeoCoordinates[c]);
				}
		}
		
		//	add direction indicators
		ArrayList directionIndicatorList = new ArrayList();
		for (int p = 0; p < this.directionIndicatorPatterns.size(); p++) {
			Annotation[] directionIndicators = Gamta.extractAllMatches(data, this.directionIndicatorPatterns.get(p), 1);
			for (int i = 0; i < directionIndicators.length; i++) {
				directionIndicators[i].changeTypeTo(DIRECTION_INDICATOR_TYPE);
				directionIndicatorList.add(directionIndicators[i]);
			}
		}
		
		//	build full geo-coordinates
		AnnotationIndex index = new AnnotationIndex();
		index.addAnnotations((Annotation[]) numericGeoCoordinateList.toArray(new Annotation[numericGeoCoordinateList.size()]));
		index.addAnnotations((Annotation[]) directionIndicatorList.toArray(new Annotation[directionIndicatorList.size()]));
		ArrayList geoCoordinateList = new ArrayList();
		Annotation[] geoCoordinates = null;
		
		//	build coordinates with leading direction indicator
		geoCoordinates = AnnotationPatternMatcher.getMatches(data, index, ("<" + DIRECTION_INDICATOR_TYPE + "> <" + NUMERIC_GEO_COORDINATE_TYPE + ">"));
		for (int c = 0; c < geoCoordinates.length; c++) {
			geoCoordinates[c].changeTypeTo(GEO_COORDINATE_TYPE);
			geoCoordinateList.add(geoCoordinates[c]);
			String directionIndicator = trimDirectionIndicator(geoCoordinates[c].firstValue());
			if (directionIndicator != null) {
				geoCoordinates[c].setAttribute(DIRECTION_INDICATOR_TYPE, directionIndicator);
				geoCoordinates[c].setAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE, "lead");
			}
		}
		
		//	build coordinates with tailing direction indicator
		geoCoordinates = AnnotationPatternMatcher.getMatches(data, index, ("<" + NUMERIC_GEO_COORDINATE_TYPE + "> <" + DIRECTION_INDICATOR_TYPE + ">"));
		for (int c = 0; c < geoCoordinates.length; c++) {
			geoCoordinates[c].changeTypeTo(GEO_COORDINATE_TYPE);
			geoCoordinateList.add(geoCoordinates[c]);
			String directionIndicator = trimDirectionIndicator(geoCoordinates[c].lastValue());
			if (directionIndicator != null) {
				geoCoordinates[c].setAttribute(DIRECTION_INDICATOR_TYPE, directionIndicator);
				geoCoordinates[c].setAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE, "tail");
			}
		}
		
		//	build coordinates without direction indicator
		for (int c = 0; c < numericGeoCoordinateList.size(); c++) {
			Annotation numericGeoCoordinate = ((Annotation) numericGeoCoordinateList.get(c));
			if ("D".equals(numericGeoCoordinate.getAttribute(NUMERIC_STYLE_ATTRIBUTE)))
				continue;
			numericGeoCoordinate.changeTypeTo(GEO_COORDINATE_TYPE);
			geoCoordinateList.add(numericGeoCoordinate);
		}
		
		//	remove coordinates that run across paragraph breaks
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			for (int t = 0; t < (geoCoordinate.size() - 1); t++)
				if (geoCoordinate.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
					geoCoordinateList.remove(c--);
					break;
				}
		}
		
		//	remove duplicates and nested coordinates
		Collections.sort(geoCoordinateList, AnnotationUtils.ANNOTATION_NESTING_ORDER);
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			for (int t = (c+1); t < geoCoordinateList.size(); t++) {
				Annotation testGeoCoordinate = ((Annotation) geoCoordinateList.get(t));
				if (AnnotationUtils.contains(geoCoordinate, testGeoCoordinate))
					geoCoordinateList.remove(t--);
				else if (geoCoordinate.getEndIndex() <= testGeoCoordinate.getStartIndex())
					break;
			}
		}
		
		//	resolve longitude orientations (E/O in French, E/W in English, O/W in German)
		StringIndex directionIndicators = new StringIndex(false);
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			if (geoCoordinate.hasAttribute(DIRECTION_INDICATOR_TYPE))
				directionIndicators.add((String) geoCoordinate.getAttribute(DIRECTION_INDICATOR_TYPE));
		}
		
		//	compute language
		int eCount = directionIndicators.getCount("E");
		int oCount = directionIndicators.getCount("O");
		int wCount = directionIndicators.getCount("W");
		
		int frenchItalianSpanish = (eCount + oCount);
		int german = (oCount + wCount);
		int english = (eCount + wCount);
		int language = Math.max(frenchItalianSpanish, Math.max(german, english));
		
		//	choose direction indicators
		String east = null;
		String west = null;
		
		if (language == frenchItalianSpanish) {
			east = "E";
			west = "O";
		}
		else if (language == german) {
			east = "O";
			west = "W";
		}
		else {
			east = "E";
			west = "W";
		}
		
		//	set attributes
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			
			//	compute numerical value
			GeoCoordinateValue gcValue = getNumericValue(geoCoordinate);
			
			//	set orientation, direction, and signed value attributes
			if ("N".equalsIgnoreCase((String) geoCoordinate.getAttribute(DIRECTION_INDICATOR_TYPE))) {
				geoCoordinate.setAttribute(ORIENTATION_ATTRIBUTE, LATITUDE_ORIENTATION);
				geoCoordinate.setAttribute(DIRECTION_ATTRIBUTE, NORTH_DIRECTION);
				geoCoordinate.setAttribute(VALUE_ATTRIBUTE, ("" + gcValue.value));
				geoCoordinate.setAttribute(PRECISION_ATTRIBUTE, ("" + gcValue.precision));
			}
			else if ("S".equalsIgnoreCase((String) geoCoordinate.getAttribute(DIRECTION_INDICATOR_TYPE))) {
				geoCoordinate.setAttribute(ORIENTATION_ATTRIBUTE, LATITUDE_ORIENTATION);
				geoCoordinate.setAttribute(DIRECTION_ATTRIBUTE, SOUTH_DIRECTION);
				geoCoordinate.setAttribute(VALUE_ATTRIBUTE, ("-" + gcValue.value));
				geoCoordinate.setAttribute(PRECISION_ATTRIBUTE, ("" + gcValue.precision));
			}
			else if (east.equalsIgnoreCase((String) geoCoordinate.getAttribute(DIRECTION_INDICATOR_TYPE))) {
				geoCoordinate.setAttribute(ORIENTATION_ATTRIBUTE, LONGITUDE_ORIENTATION);
				geoCoordinate.setAttribute(DIRECTION_ATTRIBUTE, EAST_DIRECTION);
				geoCoordinate.setAttribute(VALUE_ATTRIBUTE, ("" + gcValue.value));
				geoCoordinate.setAttribute(PRECISION_ATTRIBUTE, ("" + gcValue.precision)); // this is worst case, i.e., at the equator - no way of knowing associated latitude
			}
			else if (west.equalsIgnoreCase((String) geoCoordinate.getAttribute(DIRECTION_INDICATOR_TYPE))) {
				geoCoordinate.setAttribute(ORIENTATION_ATTRIBUTE, LONGITUDE_ORIENTATION);
				geoCoordinate.setAttribute(DIRECTION_ATTRIBUTE, WEST_DIRECTION);
				geoCoordinate.setAttribute(VALUE_ATTRIBUTE, ("-" + gcValue.value));
				geoCoordinate.setAttribute(PRECISION_ATTRIBUTE, ("" + gcValue.precision)); // this is worst case, i.e., at the equator - no way of knowing associated latitude
			}
		}
		
		//	remove annotations with direction at wrong end
		int dirLead = 0;
		int dirTail = 0;
		geoCoordinates = data.getAnnotations(GEO_COORDINATE_TYPE);
		
		//	check at which end of annotations there are more directions
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			if ("lead".equals(geoCoordinate.getAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE)))
				dirLead++;
			if ("tail".equals(geoCoordinate.getAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE)))
				dirTail++;
		}
		
		//	significantly more direction indicators at tail, remove annotations with leading direction indicators
		if ((dirLead * 3) < (dirTail * 2))
			for (int c = 0; c < geoCoordinateList.size(); c++) {
				if ("lead".equals(((Annotation) geoCoordinateList.get(c)).getAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE)))
					geoCoordinateList.remove(c--);
			}
		
		//	significantly more direction indicators at leading, remove annotations with direction indicators at tail
		else if ((dirTail * 3) < (dirLead * 2))
			for (int c = 0; c < geoCoordinateList.size(); c++) {
				if ("tail".equals(((Annotation) geoCoordinateList.get(c)).getAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE)))
					geoCoordinateList.remove(c--);
			}
		
		//	identify coordinate pairs with "crossing" coordinate in the middle (wrong direction indicator and numeric value order), and remove the latter
		AnnotationIndex gcIndex = new AnnotationIndex();
		gcIndex.addAnnotations((Annotation[]) geoCoordinateList.toArray(new Annotation[geoCoordinateList.size()]));
		Annotation[] geoCoordinatePairs = AnnotationPatternMatcher.getMatches(data, gcIndex, ("<" + GEO_COORDINATE_TYPE + "> <" + GEO_COORDINATE_TYPE + ">"));
		int gcpIndex = 0;
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			while ((gcpIndex < geoCoordinatePairs.length) && (geoCoordinatePairs[gcpIndex].getEndIndex() < geoCoordinate.getEndIndex()))
				gcpIndex++;
			if (gcpIndex == geoCoordinatePairs.length)
				break;
			if ((geoCoordinatePairs[gcpIndex].getStartIndex() < geoCoordinate.getStartIndex()) && (geoCoordinate.getEndIndex() < geoCoordinatePairs[gcpIndex].getEndIndex()))
				geoCoordinateList.remove(c--);
		}
		
		//	add remaining annotations to document
		for (int c = 0; c < geoCoordinateList.size(); c++) {
			Annotation geoCoordinate = ((Annotation) geoCoordinateList.get(c));
			geoCoordinate.removeAttribute(DIRECTION_INDICATOR_TYPE);
			geoCoordinate.removeAttribute(DIRECTION_INDICATOR_POSITION_ATTRIBUTE);
			data.addAnnotation(geoCoordinate);
		}
		AnnotationFilter.removeDuplicates(data, GEO_COORDINATE_TYPE);
	}
	
	private static final String trimDirectionIndicator(String directionIndicator) {
		for (int c = 0; c < directionIndicator.length(); c++) {
			if ("NSEOWnseow".indexOf(directionIndicator.charAt(c)) != -1)
				return directionIndicator.substring(c, (c+1));
		}
		return null;
	}
	
	private static final class GeoCoordinateValue {
		final float value;
		final int precision;
		GeoCoordinateValue(float value, int precision) {
			this.value = value;
			this.precision = precision;
		}
	}
	
	private static final GeoCoordinateValue getNumericValue(Annotation geoCoordinate) {
		if (DEBUG) System.out.println("Parsing " + TokenSequenceUtils.concatTokens(geoCoordinate, false, true));
		float numericalValue = 0;
		int position = 0;
		int precisionRange = -1;
		for (int t = 0; t < geoCoordinate.size(); t++) {
			String token = geoCoordinate.valueAt(t);
			if (Gamta.isNumber(token)) {
				
				//	normalize decimal digit separator
				token = token.replaceAll("\\,", ".");
				
				//	add current number
				numericalValue += getNumericDegrees(Float.parseFloat(token), position);
				
				//	compute precision achieved so far
				int decimalStart = token.indexOf('.');
				int decimalDigits = ((decimalStart == -1) ? 0 : (token.length() - token.indexOf('.') - 1));
				precisionRange = Math.round(EARTH_CIRCUMFERENCE / (360 * ((int) Math.pow(60, position)) * ((int) Math.pow(10, decimalDigits))));
				
				//	switch to next detail position
				position ++;
			}
		}
		
		int precision = precisionRange / 2; // assume exact point to be middle of interval
		
		if (DEBUG) System.out.println("Decimal value is " + numericalValue + ", precision is " + precision);
		
		return new GeoCoordinateValue(numericalValue, Math.max(precision, OPTIMAL_PRECISION));
	}
	
	private static final boolean DEBUG = true;
	private static final int EARTH_CIRCUMFERENCE = 40 * 1000 * 1000;
	
	private static final float getNumericDegrees(float value, int position) {
		if (position == 0) // degrees
			return value;
		else if (position == 1) // minutes
			return (value / 60);
		else if (position == 2) // seconds
			return (value / 3600);
		else return 0;
	}
	
	public static void main(String[] args) throws Exception {
//		URL docUrl = new URL("http://plazi.cs.umb.edu/GgServer/xslt/AE18DBF4BBFAF9D0A9B1B64FA6FB3A41");
//		InputStream docIn = docUrl.openStream();
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(docIn, "UTF-8"));
//		docIn.close();
//		AnnotationFilter.removeAnnotations(doc, GEO_COORDINATE_TYPE);
//		
//		GeoCoordinateTagger gct = new GeoCoordinateTagger();
//		gct.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/")));
//		gct.process(doc, new Properties());
//		
//		Annotation[] gcs = doc.getAnnotations(GEO_COORDINATE_TYPE);
//		for (int c = 0; c < gcs.length; c++)
//			System.out.println(gcs[c].toXML());
//		
//		if (true)
//			return;
		
		MutableAnnotation test;
//		test = Gamta.newDocument(Gamta.newTokenSequence("10°21'46.1''", null));
		test = Gamta.newDocument(Gamta.newTokenSequence("10°21'46''", null));
//		test = Gamta.newDocument(Gamta.newTokenSequence("10°21.01'", null));
//		test = Gamta.newDocument(Gamta.newTokenSequence("10°21'", null));
//		test = Gamta.newDocument(Gamta.newTokenSequence("10°", null));
//		test = Gamta.newDocument(Gamta.newTokenSequence("10.3628°", null));
//		test = Gamta.newDocument(Gamta.newTokenSequence("10.35°", null));
		GeoCoordinateValue gcValue = getNumericValue(test);
		System.out.println(gcValue.value + " (~" + gcValue.precision + "m)");
	}
}
