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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AnnotationEditorFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.locations.GeoUtils;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.plugins.quantities.QuantityUtils;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class MaterialsCitationParserOnline extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants, LiteratureConstants {
	
	private StringVector collectingCountyRegExes = new StringVector();
	
//	private String properNamePartRegEx = "[A-Z](([a-z\\']{0,2}\\.)|([a-z\\']{2,}))";
//	private String properNamePartRegEx = "[A-Z]([a-z]{0,2}(\\'?[A-Z])?)?(([a-z]{2,})|([a-z]{0,2}\\.))";
//	private String properNamePartRegEx = "" +
//			"[A-Z]" +
//			"(" +
//				"[a-z]{0,2}" +
//				"(\\'?[A-Z])?" +
//			")?" +
//			"(" +
//				"([a-z]{1,})" +
//				"|" +
//				"([a-z]{0,2}\\.)" +
//			")" +
//			"(" +
//				"\\-[A-Z]" +
//				"(" +
//					"[a-z]{0,2}" +
//					"(\\'?[A-Z])?" +
//				")?" +
//				"(" +
//					"([a-z]{1,})" +
//					"|" +
//					"([a-z]{0,2}\\.)" +
//				")" +
//			")?";
	private String properNamePartBaseRegEx = "" +
			"[A-Z]" +
			"(" +
				"[a-z]{0,2}" +
				"(\\'?[A-Z])?" +
			")?" +
			"(" +
				"([a-z]{1,})" +
				"|" +
				"([a-z]{0,2}\\.)" +
			")";
	private String properNamePartRegEx = this.properNamePartBaseRegEx + "(\\-" + this.properNamePartBaseRegEx + ")*";
	
	private String locationDeviationBearingPattern = "" +
			"([123][0-9]{0,2}\\s*\\°\\s*)?" + // angle bearing
			"(NNE|NNW|NE|NW|N|SSE|SSW|SE|SW|S|ENE|ESE|E|WSW|WNW|W)" + // direction bearing
			"";
	private String locationDeviationPattern = "" +
			"<quantity test=\"(./@metricUnit = 'm' and 3 <= ./@metricMagnitude)\"> " + // distance
			"<bearing> " + // bearing
			"'of'?" +
			"";
	
	private StringVector andList = new StringVector();
	
//	private String specimenCodeRegEx = "[A-Z]{3,6}+\\s?+[0-9]++";
	private String specimenCodeRegEx = "[A-Z]{3,6}+\\s?+(\\-\\s)?+[0-9]++";
	
	private static final Dictionary caseSensitiveNoise = new Dictionary() {
		private StringVector content = Gamta.getNoiseWords();
		public StringIterator getEntryIterator() {
			return this.content.getEntryIterator();
		}
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		public boolean isEmpty() {
			return this.content.isEmpty();
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return this.content.lookup(string, caseSensitive);
		}
		public boolean lookup(String string) {
			return this.content.contains(string);
		}
		public int size() {
			return this.content.size();
		}
	};
	
	private StringVector properNamesForbidden = new StringVector();
	
	private TreeMap typeStatuses = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private Dictionary typeStatusDict = new Dictionary() {
		public boolean lookup(String string) {
			return typeStatuses.containsKey(string);
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return typeStatuses.containsKey(string);
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return typeStatuses.isEmpty();
		}
		public int size() {
			return typeStatuses.size();
		}
		public StringIterator getEntryIterator() {
			final Iterator it = typeStatuses.keySet().iterator();
			return new StringIterator() {
				public void remove() {}
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					return it.next();
				}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
	};
	
	private TreeMap specimenTypes = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private Dictionary specimenTypeDict = new Dictionary() {
		public boolean lookup(String string) {
			return specimenTypes.containsKey(string);
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return specimenTypes.containsKey(string);
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return specimenTypes.isEmpty();
		}
		public int size() {
			return specimenTypes.size();
		}
		public StringIterator getEntryIterator() {
			final Iterator it = specimenTypes.keySet().iterator();
			return new StringIterator() {
				public void remove() {}
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					return it.next();
				}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
	};
	
	private StringVector relevantTypes = new StringVector();
	private Properties relevantTypeMappings = new Properties();
	
	private CountryHandler countryHandler;
	
	private static final String BACK_REFERENCE_TYPE = "backReference";
	private static final Color BACK_REFERENCE_COLOR = Color.GRAY;
	
	private static final String SPECIMEN_TYPE = "specimenType";
	
	private static final String COLLECTED_FROM = "collectedFrom";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	TODO_maybe_later load regex patterns for all types from files, precedence in same order as types specified in config file
		
		try {
			InputStream is = this.dataProvider.getInputStream("relevantTypes.txt");
			StringVector typeLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeLines.size(); t++) {
				String typeLine = typeLines.get(t).trim();
				if ((typeLine.length() != 0) && !typeLine.startsWith("//")) {
					int split = typeLine.indexOf(' ');
					if (split == -1)
						this.relevantTypes.addElement(typeLine);
					else {
						String type = typeLine.substring(0, split).trim();
						Color color = FeedbackPanel.getColor(typeLine.substring(split).trim());
						this.relevantTypes.addElement(type);
						this.highlightAttributeCache.put(type, color);
					}
				}
			}
		} catch (IOException ioe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream("relevantTypeMappings.txt");
			StringVector typeMappingLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeMappingLines.size(); t++) {
				String typeMappingLine = typeMappingLines.get(t).trim();
				if ((typeMappingLine.length() != 0) && !typeMappingLine.startsWith("//")) {
					int split = typeMappingLine.lastIndexOf(' ');
					if (split != -1) {
						String sourceType = typeMappingLine.substring(0, split).trim();
						String relevantType = typeMappingLine.substring(split).trim();
						this.relevantTypeMappings.setProperty(sourceType, relevantType);
						System.out.println("mapped '" + sourceType + "' to '" + relevantType + "'");
					}
				}
			}
		} catch (IOException ioe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream(COLLECTING_COUNTY_ANNOTATION_TYPE + ".txt");
			this.collectingCountyRegExes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream(PROPER_NAME_TYPE + "Part.txt");
			StringVector properNamePartRegExes = StringVector.loadList(is);
			if (properNamePartRegExes.size() != 0)
				this.properNamePartRegEx = properNamePartRegExes.get(0);
			is.close();
		} catch (IOException ioe) {}
		try {
			InputStream is = this.dataProvider.getInputStream(PROPER_NAME_TYPE + "Forbidden.txt");
			this.properNamesForbidden = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
		
		this.andList.parseAndAddElements("and;und;et;y;e;ac", ";");
		
		try {
			InputStream is = this.dataProvider.getInputStream(SPECIMEN_CODE + ".txt");
			StringVector specimenCodeRegExes = StringVector.loadList(is);
			if (specimenCodeRegExes.size() != 0)
				this.specimenCodeRegEx = specimenCodeRegExes.get(0);
			is.close();
		} catch (IOException ioe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream(TYPE_STATUS + ".txt");
			BufferedReader tsr = new BufferedReader(new InputStreamReader(is, "utf-8"));
			for (String tsl; (tsl = tsr.readLine()) != null;) {
				tsl = tsl.trim();
				if (tsl.startsWith("//") || (tsl.length() == 0))
					continue;
				tsl = tsl.toLowerCase();
				if (tsl.indexOf("==>") == -1) {
					this.typeStatuses.put(tsl, tsl);
					continue;
				}
				String ts = tsl.substring(0, tsl.indexOf("==>")).trim();
				String mts = tsl.substring(tsl.indexOf("==>") + "==>".length()).trim();
				this.typeStatuses.put(mts, mts);
				this.typeStatuses.put(ts, mts);
			}
			tsr.close();
		} catch (IOException ioe) {}
		
		this.countryHandler = CountryHandler.getCountryHandler((InputStream) null);
		
		try {
			InputStream is = this.dataProvider.getInputStream("specimenTypes.txt");
			BufferedReader str = new BufferedReader(new InputStreamReader(is, "utf-8"));
			for (String sptl; (sptl = str.readLine()) != null;) {
				sptl = sptl.trim();
				if (sptl.startsWith("//") || (sptl.length() == 0) || (sptl.indexOf("==>") == -1))
					continue;
				sptl = sptl.toLowerCase();
				String spt = sptl.substring(0, sptl.indexOf("==>")).trim();
				String mspt = sptl.substring(sptl.indexOf("==>") + "==>".length()).trim();
				this.specimenTypes.put(mspt, mspt);
				this.specimenTypes.put(spt, mspt);
				if (spt.endsWith("."))
					this.specimenTypes.put(spt.substring(0, (spt.length() - ".".length())).trim(), mspt);
			}
			str.close();
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get materials citations
		MutableAnnotation[] materialsCitations = data.getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
		
		//	invoked on single materials citation
		if (MATERIALS_CITATION_ANNOTATION_TYPE.equals(data.getType())
				||
				(
					(materialsCitations.length == 1)
					&&
					AnnotationUtils.equals(data, materialsCitations[0], false)
				)
			) {
			this.processMaterialsCitation(((materialsCitations.length == 0) ? data : materialsCitations[0]), null, null, null, 0, 0, true, null, null, null, null, null);
			return;
		}
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	store paragraph IDs for materials citations
		Properties mcParagraphIDs = new Properties();
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	get materials citations
			MutableAnnotation[] paragraphMaterialsCitations = paragraphs[p].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
			
			//	store paragraph IDs
			for (int m = 0; m < paragraphMaterialsCitations.length; m++)
				mcParagraphIDs.setProperty(paragraphMaterialsCitations[m].getAnnotationID(), paragraphs[p].getAnnotationID());
		}
		
		//	multi feedback available, do all materials citations in parallel
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			
			//	transfer details annotated externally
			for (int m = 0; m < materialsCitations.length; m++)
				this.transferDetails(materialsCitations[m]);
			
			//	add remaining details
			for (int m = 0; m < materialsCitations.length; m++) // TODO consider learning details across multiple documents
				this.annotateDetails(materialsCitations[m], null, null, null, null, null);
			
			//	watch out for duplicates, though
			for (int m = 0; m < materialsCitations.length; m++)
				AnnotationFilter.removeDuplicates(materialsCitations[m]);
			
			//	build feedback panels
			AnnotationEditorFeedbackPanel[] aefps = new AnnotationEditorFeedbackPanel[materialsCitations.length];
			for (int m = 0; m < materialsCitations.length; m++)
				aefps[m] = this.produceFeedbackPanel(materialsCitations[m], mcParagraphIDs.getProperty(materialsCitations[m].getAnnotationID()));
			
			//	get feedback
			FeedbackPanel.getMultiFeedback(aefps);
			
			//	process all feedback data together
			for (int d = 0; d < aefps.length; d++)
				AnnotationEditorFeedbackPanel.writeChanges(aefps[d].getTokenStatesAt(0), materialsCitations[d]);
			
			//	generate attributes (transfer only within paragraphs, though)
			String previousMcParagraphId = null;
			HashMap mcIndex = new HashMap();
			for (int m = 0; m < materialsCitations.length; m++) {
				
				//	get previous materials citation (for back references)
				String mcParagraphId = mcParagraphIDs.getProperty(materialsCitations[m].getAnnotationID());
				MutableAnnotation previousMaterialsCitation;
				if (m == 0)
					previousMaterialsCitation = null;
				else if ((mcParagraphId != null) && mcParagraphId.equals(previousMcParagraphId))
					previousMaterialsCitation = materialsCitations[m-1];
				else previousMaterialsCitation = null;
				
				this.addMaterialsCitationAttributes(materialsCitations[m], previousMaterialsCitation, mcIndex);
				
				//	switch paragraph ID
				previousMcParagraphId = mcParagraphId;
			}
			
			//	remove duplicates
			for (int t = 0; t < this.relevantTypes.size(); t++)
				AnnotationFilter.removeDuplicates(data, this.relevantTypes.get(t));
		}
		
		//	do sequential processing
		else {
			
			//	capture paragraph ID
			String previousMcParagraphId = null;
			HashMap mcIndex = new HashMap();
			
			//	test for re-run
			boolean isReRun = ((materialsCitations.length != 0) && materialsCitations[materialsCitations.length-1].hasAttribute(LOCATION_NAME_ATTRIBUTE));
			
			//	set up transfer of details likely to repeat
			StringVector collectingRegions = new StringVector();
			StringVector collectingMunicipalities = new StringVector();
			StringVector locations = new StringVector();
			StringVector collectorNames = new StringVector();
			StringVector determinerNames = new StringVector();
			
			//	do processing
			for (int m = 0; m < materialsCitations.length; m++) {
				
				//	get previous materials citation (for back references)
				String mcParagraphId = mcParagraphIDs.getProperty(materialsCitations[m].getAnnotationID());
				MutableAnnotation previousMaterialsCitation;
				if (m == 0)
					previousMaterialsCitation = null;
				else if ((mcParagraphId != null) && mcParagraphId.equals(previousMcParagraphId))
					previousMaterialsCitation = materialsCitations[m-1];
				else {
					previousMaterialsCitation = null;
//					general transfer via index now, as index keys are only (a) specimen codes and (b) type status, which should be (a) unique or (b) safe, respectively
//					mcIndex.clear();
				}
				
				//	process materials citations individually, and capture feedback
				String feedback = this.processMaterialsCitation(materialsCitations[m], previousMaterialsCitation, mcIndex, mcParagraphId, m, materialsCitations.length, isReRun, collectingRegions, collectingMunicipalities, locations, collectorNames, determinerNames);
				
				//	stop if dialog canceled
				if ("Cancel".equals(feedback))
					return;
				
				//	go back to previous
				if ("Previous".equals(feedback)) {
					m--;
					materialsCitations[m].removeAttribute(LOCATION_NAME_ATTRIBUTE);
					m--;
				}
				
				//	proceed to next, switching paragraph ID
				else previousMcParagraphId = mcParagraphId;
			}
		}
	}
	
	private void transferDetails(MutableAnnotation materialsCitation) {
		for (Iterator atit = this.relevantTypeMappings.keySet().iterator(); atit.hasNext();) {
			String typePath = ((String) atit.next());
			String mappedType = this.relevantTypeMappings.getProperty(typePath);
			Annotation[] mappableAnnotations = GPath.evaluatePath(materialsCitation, typePath, null);
			for (int a = 0; a < mappableAnnotations.length; a++)
				materialsCitation.addAnnotation(mappedType, mappableAnnotations[a].getStartIndex(), mappableAnnotations[a].size()).copyAttributes(mappableAnnotations[a]);
		}
		for (int t = 0; t < this.relevantTypes.size(); t++)
			AnnotationFilter.removeDuplicates(materialsCitation, this.relevantTypes.get(t));
	}
	
	private void annotateDetails(MutableAnnotation materialsCitation, StringVector knownCollectingRegions, StringVector knownMunicipalities, StringVector knownLocations, StringVector knownCollectorNames, StringVector knownDeterminerNames) {
		
		//	annotate counties
		for (int re = 0; re < this.collectingCountyRegExes.size(); re++) {
			Annotation[] counties = Gamta.extractAllMatches(materialsCitation, this.collectingCountyRegExes.get(re), 10, caseSensitiveNoise, null, false, false);
			for (int r = 0; r < counties.length; r++)
				materialsCitation.addAnnotation(COLLECTING_COUNTY_ANNOTATION_TYPE, counties[r].getStartIndex(), counties[r].size());
		}
		AnnotationFilter.removeDuplicates(materialsCitation, COLLECTING_COUNTY_ANNOTATION_TYPE);
		
		//	check for duplicates again
		AnnotationFilter.removeDuplicates(materialsCitation, COLLECTING_COUNTY_ANNOTATION_TYPE);
		
		//	remove region annotations that mark a country
		AnnotationFilter.removeByContained(materialsCitation, COLLECTING_COUNTRY_ANNOTATION_TYPE, COLLECTING_COUNTY_ANNOTATION_TYPE, false);
		AnnotationFilter.removeByContained(materialsCitation, COLLECTING_REGION_ANNOTATION_TYPE, COLLECTING_COUNTY_ANNOTATION_TYPE, false);
		
		//	remove counties containing a type status indicator
		Annotation[] counties = materialsCitation.getAnnotations(COLLECTING_COUNTY_ANNOTATION_TYPE);
		for (int c = 0; c < counties.length; c++) {
			for (int v = 0; v < counties[c].size(); v++)
				if (this.typeStatusDict.lookup(counties[c].valueAt(v), false)) {
					v = materialsCitation.removeAnnotation(counties[c]).size();
					materialsCitation.removeAnnotation(counties[c]);
				}
		}
		
		//	mark locations approved by user earlier
		if (knownLocations != null) {
			Annotation[] kLocations = Gamta.extractAllContained(materialsCitation, knownLocations);
			for (int l = 0; l < kLocations.length; l++)
				materialsCitation.addAnnotation(LOCATION_TYPE, kLocations[l].getStartIndex(), kLocations[l].size());
			AnnotationFilter.removeDuplicates(materialsCitation, LOCATION_TYPE);
		}
		
		//	mark municipalities approved by user earlier
		if (knownMunicipalities != null) {
			Annotation[] kMunicipalities = Gamta.extractAllContained(materialsCitation, knownMunicipalities);
			for (int m = 0; m < kMunicipalities.length; m++)
				materialsCitation.addAnnotation(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE, kMunicipalities[m].getStartIndex(), kMunicipalities[m].size());
			AnnotationFilter.removeDuplicates(materialsCitation, COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
		}
		
		//	mark regions approved by user earlier
		if (knownCollectingRegions != null) {
			Annotation[] kRegions = Gamta.extractAllContained(materialsCitation, knownCollectingRegions);
			for (int r = 0; r < kRegions.length; r++)
				materialsCitation.addAnnotation(COLLECTING_REGION_ANNOTATION_TYPE, kRegions[r].getStartIndex(), kRegions[r].size());
			AnnotationFilter.removeDuplicates(materialsCitation, COLLECTING_REGION_ANNOTATION_TYPE);
		}
		
		//	mark location deviations
		Annotation[] locationDeviationBearings = Gamta.extractAllMatches(materialsCitation, this.locationDeviationBearingPattern);
		AnnotationIndex locationDeviationBearingIndex = new AnnotationIndex(materialsCitation, null);
		locationDeviationBearingIndex.addAnnotations(locationDeviationBearings, "bearing");
		Annotation[] locationDeviations = AnnotationPatternMatcher.getMatches(materialsCitation, locationDeviationBearingIndex, this.locationDeviationPattern);
		for (int d = 0; d < locationDeviations.length; d++) {
			MutableAnnotation locationDeviation = materialsCitation.addAnnotation(LOCATION_DEVIATION_ANNOTATION_TYPE, locationDeviations[d].getStartIndex(), locationDeviations[d].size());
			Annotation[] distanceAnnot = locationDeviation.getAnnotations(QUANTITY_TYPE);
			if (distanceAnnot.length == 0)
				continue;
			Annotation bearingAnnot = null;
			for (int b = 0; b < locationDeviationBearings.length; b++)
				if (AnnotationUtils.contains(locationDeviation, locationDeviationBearings[b])) {
					bearingAnnot = locationDeviationBearings[b];
					break;
				}
			if (bearingAnnot == null)
				continue;
			try {
				double distance = QuantityUtils.getMetricValue(distanceAnnot[0]);
				double bearing = -1;
				if (Gamta.isNumber(bearingAnnot.firstValue()))
					bearing = Double.parseDouble(bearingAnnot.firstValue()); // bearing given in degrees
				else bearing = GeoUtils.getBearing(bearingAnnot.lastValue()); // bearing only given verbally
				if (bearing != -1) {
					locationDeviation.setAttribute(DISTANCE_ATTRIBUTE, ("" + distance));
					locationDeviation.setAttribute(BEARING_ATTRIBUTE, ("" + bearing));
				}
			} catch (NumberFormatException nfe) {}
		}
		
		//	annotate type status
		Annotation[] typeStatuses = Gamta.extractAllContained(materialsCitation, this.typeStatusDict, false, false, false);
		for (int t = 0; t < typeStatuses.length; t++)
			typeStatuses[t] = materialsCitation.addAnnotation(TYPE_STATUS, typeStatuses[t].getStartIndex(), typeStatuses[t].size());
		
		//	mark and classify specimen counts 
		Annotation[] specimenTypes = Gamta.extractAllContained(materialsCitation, this.specimenTypeDict, 2, false, false, false);
		if (specimenTypes.length != 0) {
			for (int s = 0; s < specimenTypes.length; s++)
				if ((specimenTypes[s].firstValue().length() > 1) || !Character.isLetter(specimenTypes[s].firstValue().charAt(0))) {
					Annotation specimenCount = materialsCitation.addAnnotation(SPECIMEN_COUNT, specimenTypes[s].getStartIndex(), specimenTypes[s].size());
					specimenCount.setAttribute(TYPE_ATTRIBUTE, this.specimenTypes.get(specimenCount.firstValue().toLowerCase()));
				}
			AnnotationIndex specimenTypeIndex = new AnnotationIndex();
			specimenTypeIndex.addAnnotations(specimenTypes, SPECIMEN_TYPE);
			Annotation[] specimenCounts = AnnotationPatternMatcher.getMatches(materialsCitation, specimenTypeIndex, "\"[1-9][0-9]*\" <" + TYPE_STATUS + ">? <" + SPECIMEN_TYPE + ">");
			for (int s = 0; s < specimenCounts.length; s++) {
				QueriableAnnotation specimenCount = materialsCitation.addAnnotation(SPECIMEN_COUNT, specimenCounts[s].getStartIndex(), specimenCounts[s].size());
				String rawSpecimenType = specimenCount.valueAt(1);
				Annotation[] specimenCountTypeStatuses = specimenCount.getAnnotations(TYPE_STATUS);
				if ((specimenCountTypeStatuses.length != 0) && (specimenCountTypeStatuses[0].getEndIndex() < specimenCount.size()) && !".".equals(specimenCount.valueAt(specimenCountTypeStatuses[0].getEndIndex())))
					rawSpecimenType = specimenCount.valueAt(specimenCountTypeStatuses[0].getEndIndex());
				specimenCount.setAttribute(TYPE_ATTRIBUTE, this.specimenTypes.get(rawSpecimenType.toLowerCase()));
			}
		}
		AnnotationFilter.removeInner(materialsCitation, SPECIMEN_COUNT);
		
		//	resolve type status vs. specimen count conflicts
		MutableAnnotation[] specimenCounts = materialsCitation.getMutableAnnotations(SPECIMEN_COUNT);
		if ((specimenCounts.length != 0) && (typeStatuses.length != 0))
			for (int s = 0; s < specimenCounts.length; s++) {
				Annotation[] scTypeStatus = specimenCounts[s].getAnnotations(TYPE_STATUS);
				if ((scTypeStatus == null) || (scTypeStatus.length == 0))
					continue;
				
				//	make leading number into specimen count (if any), but only if every specimen count has a type status embedded
				if ((scTypeStatus[0].getStartIndex() > 0) && (specimenCounts.length <= typeStatuses.length)) {
					Annotation numberSpecimenCount = specimenCounts[s].addAnnotation(SPECIMEN_COUNT, 0, scTypeStatus[0].getStartIndex());
					numberSpecimenCount.setAttribute(TYPE_ATTRIBUTE, specimenCounts[s].getAttribute(TYPE_ATTRIBUTE));
				}
				
				//	annotate specimen type (solely to avoid confusing users, as specimen type is in attribute of count)
				if (scTypeStatus[0].getEndIndex() < specimenCounts[s].size())
					specimenCounts[s].addAnnotation(SPECIMEN_TYPE, scTypeStatus[0].getEndIndex(), (specimenCounts[s].size() - scTypeStatus[0].getEndIndex()));
				
				//	remove original specimen count
				materialsCitation.removeAnnotation(specimenCounts[s]);
			}
		
		//	mark collectors approved by user earlier
		if (knownCollectorNames != null) {
			Annotation[] kCollectorNames = Gamta.extractAllContained(materialsCitation, knownCollectorNames);
			for (int c = 0; c < kCollectorNames.length; c++)
				materialsCitation.addAnnotation(COLLECTOR_NAME, kCollectorNames[c].getStartIndex(), kCollectorNames[c].size());
			AnnotationFilter.removeDuplicates(materialsCitation, COLLECTOR_NAME);
		}
		
		//	mark determiners approved by user earlier
		if (knownDeterminerNames != null) {
			Annotation[] kDeterminerNames = Gamta.extractAllContained(materialsCitation, knownDeterminerNames);
			for (int d = 0; d < kDeterminerNames.length; d++)
				materialsCitation.addAnnotation(DETERMINER_NAME, kDeterminerNames[d].getStartIndex(), kDeterminerNames[d].size());
			AnnotationFilter.removeDuplicates(materialsCitation, DETERMINER_NAME);
		}
		
		//	sort out proper name parts that overlap with existing detail annotations
		boolean[] isTokenAssigned = new boolean[materialsCitation.size()];
		Arrays.fill(isTokenAssigned, false);
		for (int t = 0; t < this.relevantTypes.size(); t++) {
			Annotation[] tAnnots = materialsCitation.getAnnotations(this.relevantTypes.get(t));
			for (int a = 0; a < tAnnots.length; a++) {
				for (int i = tAnnots[a].getStartIndex(); i < tAnnots[a].getEndIndex(); i++)
					isTokenAssigned[i] = true;
			}
		}
		
		//	annotate proper name parts
		Annotation[] properNameParts = Gamta.extractAllMatches(materialsCitation, this.properNamePartRegEx, 2, caseSensitiveNoise, this.properNamesForbidden);
//		System.out.println("  - got proper name parts:");
//		for (int p = 0; p < properNameParts.length; p++)
//			System.out.println("    - " + properNameParts[p].toXML());
		
		//	connect proper name parts
		if (properNameParts.length != 0) {
			
			//	jump country and region that might be contained in materials citation
			Annotation[] countries = materialsCitation.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
			int cEnd = ((countries.length == 0) ? 0 : countries[0].getEndIndex());
			Annotation[] regions = materialsCitation.getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
			int rEnd = ((regions.length == 0) ? 0 : regions[0].getEndIndex());
			counties = materialsCitation.getAnnotations(COLLECTING_COUNTY_ANNOTATION_TYPE);
			int coEnd = ((counties.length == 0) ? 0 : counties[0].getEndIndex());
			Annotation[] municipalities = materialsCitation.getAnnotations(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
			int mEnd = ((municipalities.length == 0) ? 0 : municipalities[0].getEndIndex());
			
			//	find first proper name part after country and region
			int properNamePartStart = 0;
			while ((properNamePartStart < properNameParts.length) && properNameParts[properNamePartStart].getStartIndex() < Math.max(Math.max(cEnd, rEnd), Math.max(coEnd, mEnd)))
				properNamePartStart++;
			
			//	some proper name parts left to deal with
			if (properNamePartStart < properNameParts.length) {
				
				//	initialize
				int properNameStart = properNameParts[properNamePartStart].getStartIndex();
				int properNameEnd = properNameParts[properNamePartStart].getEndIndex();
				int partsInCurrentName = 0;
				boolean gotUnabbreviatedPart = (properNameParts[properNamePartStart].size() == 1);
				
				//	check part annotation one by one
				for (int p = (properNamePartStart + 1); p < properNameParts.length; p++) {
					
					//	check overlap with other details
					for (int t = properNameParts[p].getStartIndex(); t < properNameParts[p].getEndIndex(); t++)
						if (isTokenAssigned[t]) {
							properNameParts[p] = null;
							break;
						}
					if (properNameParts[p] == null)
						continue;
					
					//	gap too large
					if ((properNameEnd + 2) < properNameParts[p].getStartIndex()) {
						
						//	add annotation if non-abbreviated part given
						if (gotUnabbreviatedPart)
							materialsCitation.addAnnotation(PROPER_NAME_TYPE, properNameStart, (properNameEnd - properNameStart));
						
						//	reset
						gotUnabbreviatedPart = (properNameParts[p].size() == 1);
						properNameStart = properNameParts[p].getStartIndex();
						properNameEnd = properNameParts[p].getEndIndex();
						partsInCurrentName = 1;
					}
					
					//	annotations touch, go on
					else if (properNameEnd == properNameParts[p].getStartIndex()) {
						properNameEnd = properNameParts[p].getEndIndex();
						gotUnabbreviatedPart = (gotUnabbreviatedPart || (properNameParts[p].size() == 1));
						partsInCurrentName++;
					}
					
					//	small gap, check tokens in between
					else {
						boolean canSpan = true;
						
						//	check tokens (allow lower case words and a few punctuation marks) 
						for (int g = properNameEnd; g < properNameParts[p].getStartIndex(); g++)
							if (!Gamta.isLowerCaseWord(materialsCitation.valueAt(g))) {
								if ((partsInCurrentName == 1) && gotUnabbreviatedPart && (properNameParts[p].size() == 2))
									canSpan = (canSpan && ("&/.,".indexOf(materialsCitation.valueAt(g)) != -1)); // allow comma between last name and first initial
								else canSpan = (canSpan && ("&/.".indexOf(materialsCitation.valueAt(g)) != -1));
							}
						
						//	span possible, go on
						if (canSpan) {
							properNameEnd = properNameParts[p].getEndIndex();
							gotUnabbreviatedPart = (gotUnabbreviatedPart || (properNameParts[p].size() == 1));
							partsInCurrentName++;
						}
						
						//	span not possible
						else {
							
							//	add annotation if non-abbreviated part given
							if (gotUnabbreviatedPart)
								materialsCitation.addAnnotation(PROPER_NAME_TYPE, properNameStart, (properNameEnd - properNameStart));
							
							//	reset
							gotUnabbreviatedPart = (properNameParts[p].size() == 1);
							properNameStart = properNameParts[p].getStartIndex();
							properNameEnd = properNameParts[p].getEndIndex();
							partsInCurrentName = 1;
						}
					}
				}
				
				//	add last annotation if non-abbreviated part given
				if (gotUnabbreviatedPart)
					materialsCitation.addAnnotation(PROPER_NAME_TYPE, properNameStart, (properNameEnd - properNameStart));
			}
			
			//	remove proper names containing a type status
			Annotation[] properNames = materialsCitation.getAnnotations(PROPER_NAME_TYPE);
			for (int p = 0; p < properNames.length; p++)
				for (int v = 0; v < properNames[p].size(); v++) {
					if (this.typeStatusDict.lookup(properNames[p].valueAt(v), false))
						v = materialsCitation.removeAnnotation(properNames[p]).size();
				}
		}
		Annotation[] properNames = materialsCitation.getAnnotations(PROPER_NAME_TYPE);
//		System.out.println("  - got proper names:");
//		for (int p = 0; p < properNames.length; p++)
//			System.out.println("    - " + properNames[p].toXML());
		
		//	classify proper names
		for (int p = 0; p < properNames.length; p++) {
			String properName = TokenSequenceUtils.concatTokens(properNames[p], true, true);
			
			//	known collecting region
			if ((knownCollectingRegions != null) && knownCollectingRegions.contains(properName)) {
				properNames[p].changeTypeTo(COLLECTING_REGION_ANNOTATION_TYPE);
				properNames[p].setAttribute("_evidence", "known");
			}
			
			//	known municipality
			else if ((knownMunicipalities != null) && knownMunicipalities.contains(properName)) {
				properNames[p].changeTypeTo(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
				properNames[p].setAttribute("_evidence", "known");
			}
			
			//	known location
			else if ((knownLocations != null) && knownLocations.contains(properName)) {
				properNames[p].changeTypeTo(LOCATION_TYPE);
				properNames[p].setAttribute("_evidence", "known");
			}
			
			//	known collector name
			else if ((knownCollectorNames != null) && knownCollectorNames.contains(properName)) {
				properNames[p].changeTypeTo(COLLECTOR_NAME);
				properNames[p].setAttribute("_evidence", "known");
			}
			
			//	known determiner name
			else if ((knownDeterminerNames != null) && knownDeterminerNames.contains(properName)) {
				properNames[p].changeTypeTo(DETERMINER_NAME);
				properNames[p].setAttribute("_evidence", "known");
			}
			
			//	proper name labeled as collector
			else if ((properNames[p].getStartIndex() > 1) && ("leg".equals(materialsCitation.valueAt(properNames[p].getStartIndex()-1)) || (".".equals(materialsCitation.valueAt(properNames[p].getStartIndex()-1)) && "leg".equals(materialsCitation.valueAt(properNames[p].getStartIndex()-2))))) {
				properNames[p].changeTypeTo(COLLECTOR_NAME);
				properNames[p].setAttribute("_evidence", "label");
			}
			
			//	proper name labeled as determiner
			else if ((properNames[p].getStartIndex() > 1) && ("det".equals(materialsCitation.valueAt(properNames[p].getStartIndex()-1)) || (".".equals(materialsCitation.valueAt(properNames[p].getStartIndex()-1)) && "det".equals(materialsCitation.valueAt(properNames[p].getStartIndex()-2))))) {
				properNames[p].changeTypeTo(DETERMINER_NAME);
				properNames[p].setAttribute("_evidence", "label");
			}
			
			//	person name starting with initial, probably the collector (more frequent than determiner)
			else if ((properNames[p].size() > 2) && properName.matches("[A-Z]\\..*")) {
				properNames[p].changeTypeTo(COLLECTOR_NAME);
				properNames[p].setAttribute("_evidence", "leading initial");
			}
			
			//	person name ending with initial, probably the collector (more frequent than determiner)
			else if ((properNames[p].size() > 2) && properName.matches(".*\\,.*[A-Z]\\.")) {
				properNames[p].changeTypeTo(COLLECTOR_NAME);
				properNames[p].setAttribute("_evidence", "tailing initial");
			}
			
			//	assume location otherwise
			else properNames[p].changeTypeTo(LOCATION_TYPE);
		}
//		System.out.println("  - proper names classified");
//		for (int p = 0; p < properNames.length; p++)
//			System.out.println("    - " + properNames[p].toXML());
		
		//	split collector names around 'and', in multiple languages
		Annotation[] collectorNames = materialsCitation.getAnnotations(COLLECTOR_NAME);
		for (int c = 0; c < collectorNames.length; c++) {
			for (int v = 0; v < collectorNames[c].size(); v++)
				if (this.andList.contains(collectorNames[c].valueAt(v))) {
					if (v != 0)
						materialsCitation.addAnnotation(COLLECTOR_NAME, collectorNames[c].getStartIndex(), v);
					if ((v+1) < collectorNames[c].size())
						materialsCitation.addAnnotation(COLLECTOR_NAME, (collectorNames[c].getStartIndex() + v + 1), (collectorNames[c].size() - v - 1));
					materialsCitation.removeAnnotation(collectorNames[c]);
					collectorNames = materialsCitation.getAnnotations(COLLECTOR_NAME);
				}
		}
		
		//	split determiner names around 'and', in multiple languages
		Annotation[] determinerNames = materialsCitation.getAnnotations(DETERMINER_NAME);
		for (int d = 0; d < determinerNames.length; d++) {
			for (int v = 0; v < determinerNames[d].size(); v++)
				if (this.andList.contains(determinerNames[d].valueAt(v))) {
					if (v != 0)
						materialsCitation.addAnnotation(DETERMINER_NAME, determinerNames[d].getStartIndex(), v);
					if ((v+1) < determinerNames[d].size())
						materialsCitation.addAnnotation(DETERMINER_NAME, (determinerNames[d].getStartIndex() + v + 1), (determinerNames[d].size() - v - 1));
					materialsCitation.removeAnnotation(determinerNames[d]);
					determinerNames = materialsCitation.getAnnotations(DETERMINER_NAME);
				}
		}
		
		//	remove locations nested in dates
		AnnotationFilter.removeContained(materialsCitation, COLLECTING_DATE, LOCATION_TYPE);
//		System.out.println("- locations in collecting dates removed");
		
		//	remove municipalities nested in dates
		AnnotationFilter.removeContained(materialsCitation, COLLECTING_DATE, COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
//		System.out.println("- municipalities in collecting dates removed");
		
		//	if we have no municipality, but more than one location, make generic location closest to region/country the municipality
		Annotation[] locations = materialsCitation.getAnnotations(LOCATION_TYPE);
		Annotation[] municipalities = materialsCitation.getAnnotations(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
		if ((municipalities.length == 0) && (locations.length > 1)) {
			Annotation[] higherLocations = materialsCitation.getAnnotations(COLLECTING_COUNTY_ANNOTATION_TYPE);
			if (higherLocations.length == 0)
				higherLocations = materialsCitation.getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
			if (higherLocations.length == 0)
				higherLocations = materialsCitation.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
			
			int minHlDist = materialsCitation.size();
			Annotation minHlDistLocation = null;
			for (int l = 0; l < locations.length; l++) {
				if (locations[l].hasAttribute("_evidence"))
					continue;
				if (higherLocations.length == 0) {
					locations[l].changeTypeTo(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
					break;
				}
				int hlDist = this.getDistance(higherLocations[0], locations[l]);
				if (hlDist < minHlDist) {
					minHlDist = hlDist;
					minHlDistLocation = locations[l];
				}
			}
			
			if (minHlDistLocation != null)
				minHlDistLocation.changeTypeTo(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
		}
		
		//	annotate specimen codes
		Annotation[] specimenCodes = Gamta.extractAllMatches(materialsCitation, this.specimenCodeRegEx, 2, false);
		for (int s = 0; s < specimenCodes.length; s++)
			materialsCitation.addAnnotation(SPECIMEN_CODE, specimenCodes[s].getStartIndex(), specimenCodes[s].size());
//		System.out.println("- specimen codes added");
	}
	
	private int getDistance(Annotation annot1, Annotation annot2) {
		if (AnnotationUtils.overlaps(annot1, annot2))
			return -1;
		else if (annot1.getEndIndex() <= annot2.getStartIndex())
			return (annot2.getStartIndex() - annot1.getEndIndex());
		return (annot1.getStartIndex() - annot2.getEndIndex());
	}
	
	private AnnotationEditorFeedbackPanel produceFeedbackPanel(MutableAnnotation materialsCitation, String paragraphId) {
		
		//	build feedback panel
		AnnotationEditorFeedbackPanel aefp = new AnnotationEditorFeedbackPanel("Check Materials Citation Details");
		aefp.setLabel("<HTML>Please make sure that all the details of this materials citation are marked." +
				"<BR>If the materials citation refers to the data from the previous one, please do the following:" +
				"<BR>- mark the details given here, eg the type status" +
				"<BR>- mark the remainder of the materials citation as <B>backReference</B>" +
				"<BR>This will import all detail data from the previous materials citation.</HTML>");
		for (int t = 0; t < this.relevantTypes.size(); t++) {
			String type = this.relevantTypes.get(t);
			Color color = this.getAnnotationHighlight(type);
			if (color != null)
				aefp.addDetailType(type, color);
		}
		if (materialsCitation.getAnnotations(SPECIMEN_TYPE).length != 0)
			aefp.addDetailType(SPECIMEN_TYPE, this.getAnnotationHighlight(SPECIMEN_TYPE));
		aefp.addDetailType(BACK_REFERENCE_TYPE, BACK_REFERENCE_COLOR);
		aefp.addAnnotation(materialsCitation);
		
		//	add background information
		aefp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, materialsCitation.getDocumentProperty(LiteratureConstants.DOCUMENT_ID_ATTRIBUTE));
		aefp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, materialsCitation.getAnnotationID());
		aefp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, MATERIALS_CITATION_ANNOTATION_TYPE);
		
		//	return the product
		return aefp;
	}
	
	private String processMaterialsCitation(MutableAnnotation materialsCitation, MutableAnnotation previousMaterialsCitation, HashMap materialsCitationIndex, String paragraphId, int index, int total, boolean isReRun, StringVector collectingRegions, StringVector collectingMunicipalities, StringVector locations, StringVector collectorNames, StringVector determinerNames) {
		
		//	we've seen this one before, and we're not here on purpose
		if (materialsCitation.hasAttribute(LOCATION_NAME_ATTRIBUTE) && !isReRun)
			return "OK";
		
		//	transfer details annotated externally
		this.transferDetails(materialsCitation);
		
		//	add remaining details (only if not done before)
		if (!materialsCitation.hasAttribute(LOCATION_NAME_ATTRIBUTE))
			this.annotateDetails(materialsCitation, collectingRegions, collectingMunicipalities, locations, collectorNames, determinerNames);
		
		//	watch out for duplicates, though
		AnnotationFilter.removeDuplicates(materialsCitation);
		
		//	build feedback panel
		AnnotationEditorFeedbackPanel aefp = this.produceFeedbackPanel(materialsCitation, paragraphId);
		if (index != 0)
			aefp.addButton("Previous");
		aefp.addButton("Cancel");
		if ((total != 0) && ((index+1) < total))
			aefp.addButton("OK & Next");
		else aefp.addButton("OK");
		
//		//	loop through existing flag
//		if (materialsCitation.hasAttribute("_" + FeedbackPanel.FLAG_PROPERTY_NAME))
//			aefp.setProperty(FeedbackPanel.FLAG_PROPERTY_NAME, ((String) materialsCitation.getAttribute("_" + FeedbackPanel.FLAG_PROPERTY_NAME)));
//		
		String title = aefp.getTitle();
		if (total != 0)
			aefp.setTitle(title + " - (" + (index+1) + " of " + total + ")");
		
		String feedback = aefp.getFeedback();
		if (feedback == null)
			feedback = "Cancel";
		
		aefp.setTitle(title);
		
//		//	observe flagging
//		if (aefp.getProperty(FeedbackPanel.FLAG_PROPERTY_NAME) != null)
//			materialsCitation.setAttribute(("_" + FeedbackPanel.FLAG_PROPERTY_NAME), aefp.getProperty(FeedbackPanel.FLAG_PROPERTY_NAME));
//		
		if (!feedback.startsWith("OK"))
			return feedback;
		
		//	dialog submitted, process data
		AnnotationEditorFeedbackPanel.writeChanges(aefp.getTokenStatesAt(0), materialsCitation);
		for (int t = 0; t < this.relevantTypes.size(); t++)
			AnnotationFilter.removeDuplicates(materialsCitation, this.relevantTypes.get(t));
		this.addMaterialsCitationAttributes(materialsCitation, previousMaterialsCitation, materialsCitationIndex);
		
		//	remember collecting regions, locations, and collector names for MCs to come
		if (collectingRegions != null) {
			Annotation[] mcCollectingRegions = materialsCitation.getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
			for (int r = 0; r < mcCollectingRegions.length; r++)
				collectingRegions.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(mcCollectingRegions[r], true, true));
		}
		if (collectingMunicipalities != null) {
			Annotation[] mcCollectingMunicipalities = materialsCitation.getAnnotations(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
			for (int m = 0; m < mcCollectingMunicipalities.length; m++)
				collectingMunicipalities.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(mcCollectingMunicipalities[m], true, true));
		}
		if (locations != null) {
			Annotation[] mcLocations = materialsCitation.getAnnotations(LOCATION_TYPE);
			for (int l = 0; l < mcLocations.length; l++)
				locations.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(mcLocations[l], true, true));
		}
		if (collectorNames != null) {
			Annotation[] mcCollectorNames = materialsCitation.getAnnotations(COLLECTOR_NAME);
			for (int n = 0; n < mcCollectorNames.length; n++)
				collectorNames.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(mcCollectorNames[n], true, true));
		}
		if (determinerNames != null) {
			Annotation[] mcDeterminerNames = materialsCitation.getAnnotations(DETERMINER_NAME);
			for (int n = 0; n < mcDeterminerNames.length; n++)
				determinerNames.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(mcDeterminerNames[n], true, true));
		}
		
		//	classify specimen counts that lack the type attribute
		Annotation[] specimenCounts = materialsCitation.getAnnotations(SPECIMEN_COUNT);
		for (int s = 0; s < specimenCounts.length; s++) {
			if (!specimenCounts[s].hasAttribute("type"))
				specimenCounts[s].setAttribute("type", this.specimenTypes.get(specimenCounts[s].firstValue()));
		}
		
		//	return feedback
		return feedback;
	}
	
	private void addMaterialsCitationAttributes(MutableAnnotation materialsCitation, Annotation previousMaterialsCitation, HashMap materialsCitationIndex) {
//		System.out.println("adding materials citation attributes:");
		
		//	get coordinates
		Annotation[] geoCoordinates = materialsCitation.getAnnotations(GEO_COORDINATE_TYPE);
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
		
		//	try global variables
		if (longitude == null)
			longitude = materialsCitation.getDocumentProperty(LONGITUDE_ATTRIBUTE);
		if (latitude == null)
			latitude = materialsCitation.getDocumentProperty(LATITUDE_ATTRIBUTE);
		if (longLatPrecision == null)
			longLatPrecision = materialsCitation.getDocumentProperty(LONG_LAT_PRECISION_ATTRIBUTE);
		
		//	reconcile coordinates and precision with collecting event
		if (materialsCitation.hasAttribute(LONGITUDE_ATTRIBUTE))
			longitude = materialsCitation.getAttribute(LONGITUDE_ATTRIBUTE);
		else if (longitude != null)
			materialsCitation.setAttribute(LONGITUDE_ATTRIBUTE, longitude);
		
		if (materialsCitation.hasAttribute(LATITUDE_ATTRIBUTE))
			latitude = materialsCitation.getAttribute(LATITUDE_ATTRIBUTE);
		else if (latitude != null)
			materialsCitation.setAttribute(LATITUDE_ATTRIBUTE, latitude);
		
		if (materialsCitation.hasAttribute(LONG_LAT_PRECISION_ATTRIBUTE))
			longLatPrecision = materialsCitation.getAttribute(LONG_LAT_PRECISION_ATTRIBUTE);
		else if (longLatPrecision != null)
			materialsCitation.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, longLatPrecision);
		
		//	get elevation (try getting metric value, and use verbatim value in case of failure)
		Annotation[] elevations = materialsCitation.getAnnotations(ELEVATION_ATTRIBUTE);
		if (elevations.length != 0) try {
			double metricElevation = Double.parseDouble((String) elevations[0].getAttribute(METRIC_VALUE_ATTRIBUTE, "0"));
			int metricMagnitude = Integer.parseInt((String) elevations[0].getAttribute(METRIC_MAGNITUDE_ATTRIBUTE, "0"));
			while (metricMagnitude > 0) {
				metricElevation *= 10;
				metricMagnitude--;
			}
			materialsCitation.setAttribute(ELEVATION_ATTRIBUTE, ("" + ((int) Math.round(metricElevation))));
		}
		catch (NumberFormatException nfe) {
			this.addDetailAttribute(materialsCitation, ELEVATION_ATTRIBUTE, ", ", true);
		}
		
		//	get country, state/province, and county
		Annotation[] collectingCountries = materialsCitation.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		Object country = null;
		if (collectingCountries.length != 0) {
			country = TokenSequenceUtils.concatTokens(collectingCountries[0], true, true);
			if (this.countryHandler != null)
				country = this.countryHandler.getEnglishName((String) country);
			materialsCitation.setAttribute(COUNTRY_ATTRIBUTE, country);
		}
		else if (materialsCitation.hasAttribute(COUNTRY_ATTRIBUTE)) {
			country = materialsCitation.getAttribute(COUNTRY_ATTRIBUTE);
			if (this.countryHandler != null)
				country = this.countryHandler.getEnglishName(country.toString());
		}
		else if (materialsCitation.getDocumentProperty(COLLECTING_COUNTRY_ANNOTATION_TYPE) != null) {
			country = materialsCitation.getDocumentProperty(COLLECTING_COUNTRY_ANNOTATION_TYPE);
			if (this.countryHandler != null)
				country = this.countryHandler.getEnglishName(country.toString());
			materialsCitation.setAttribute(COUNTRY_ATTRIBUTE, country);
		}
		
		Annotation[] collectingRegions = materialsCitation.getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
		Object region = null;
		if (collectingRegions.length != 0) {
			region = TokenSequenceUtils.concatTokens(collectingRegions[0], true, true);
			materialsCitation.setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
		}
		else if (materialsCitation.hasAttribute(STATE_PROVINCE_ATTRIBUTE))
			region = materialsCitation.getAttribute(STATE_PROVINCE_ATTRIBUTE);
		else if (materialsCitation.getDocumentProperty(COLLECTING_REGION_ANNOTATION_TYPE) != null) {
			region = materialsCitation.getDocumentProperty(COLLECTING_REGION_ANNOTATION_TYPE);
			materialsCitation.setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
		}
		
		Annotation[] collectingCounties = materialsCitation.getAnnotations(COLLECTING_COUNTY_ANNOTATION_TYPE);
		Object county = null;
		if (collectingCounties.length != 0) {
			county = TokenSequenceUtils.concatTokens(collectingCounties[0], true, true);
			materialsCitation.setAttribute(COUNTY_ATTRIBUTE, county);
		}
		else if (materialsCitation.hasAttribute(COUNTY_ATTRIBUTE))
			county = materialsCitation.getAttribute(COUNTY_ATTRIBUTE);
		else if (materialsCitation.getDocumentProperty(COLLECTING_COUNTY_ANNOTATION_TYPE) != null) {
			county = materialsCitation.getDocumentProperty(COLLECTING_COUNTY_ANNOTATION_TYPE);
			materialsCitation.setAttribute(COUNTY_ATTRIBUTE, county);
		}
		
		Annotation[] collectingMunicipalities = materialsCitation.getAnnotations(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
		Object municipality = null;
		if (collectingMunicipalities.length != 0) {
			municipality = TokenSequenceUtils.concatTokens(collectingMunicipalities[0], true, true);
			materialsCitation.setAttribute(MUNICIPALITY_ATTRIBUTE, municipality);
		}
		else if (materialsCitation.hasAttribute(MUNICIPALITY_ATTRIBUTE))
			municipality = materialsCitation.getAttribute(MUNICIPALITY_ATTRIBUTE);
		else if (materialsCitation.getDocumentProperty(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE) != null) {
			municipality = materialsCitation.getDocumentProperty(COLLECTING_MUNICIPALITY_ANNOTATION_TYPE);
			materialsCitation.setAttribute(MUNICIPALITY_ATTRIBUTE, municipality);
		}
		
		//	get locations
		Annotation[] locations = materialsCitation.getAnnotations(LOCATION_TYPE);
		for (int l = 0; l < locations.length; l++) {
			if (longitude != null)
				locations[l].setAttribute(LONGITUDE_ATTRIBUTE, longitude);
			if (latitude != null)
				locations[l].setAttribute(LATITUDE_ATTRIBUTE, latitude);
			if (longLatPrecision != null)
				locations[l].setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, longLatPrecision);
			
			if (country != null)
				locations[l].setAttribute(COUNTRY_ATTRIBUTE, country);
			if (region != null)
				locations[l].setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
			if (county != null)
				locations[l].setAttribute(COUNTY_ATTRIBUTE, county);
			if (municipality != null)
				locations[l].setAttribute(MUNICIPALITY_ATTRIBUTE, municipality);
			
			locations[l].setAttribute(NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(locations[l], true, true));
		}
		
		//	set location attribute (substitute with lowest available order geographical name)
		System.out.println(" - got " + locations.length + " locations");
		if (locations.length != 0) {
			materialsCitation.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(locations[0], true, true));
//			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(locations[0], true, true) + "'");
		}
		else if (materialsCitation.getDocumentProperty(LOCATION_NAME_ATTRIBUTE) != null) {
			materialsCitation.setAttribute(LOCATION_NAME_ATTRIBUTE, materialsCitation.getDocumentProperty(LOCATION_NAME_ATTRIBUTE));
//			System.out.println(" - MC location is '" + materialsCitation.getDocumentProperty(LOCATION_NAME_ATTRIBUTE) + "'");
		}
		else if (collectingMunicipalities.length != 0) {
			materialsCitation.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingMunicipalities[0], true, true));
//			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingMunicipalities[0], true, true) + "'");
		}
		else if (collectingCounties.length != 0) {
			materialsCitation.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingCounties[0], true, true));
//			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingCounties[0], true, true) + "'");
		}
		else if (collectingRegions.length != 0) {
			materialsCitation.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingRegions[0], true, true));
//			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingRegions[0], true, true) + "'");
		}
		else if (collectingCountries.length != 0) {
			materialsCitation.setAttribute(LOCATION_NAME_ATTRIBUTE, TokenSequenceUtils.concatTokens(collectingCountries[0], true, true));
//			System.out.println(" - MC location is '" + TokenSequenceUtils.concatTokens(collectingCountries[0], true, true) + "'");
		}
		
		//	add collecting environment detail
		this.addDetailAttribute(materialsCitation, COLLECTED_FROM, ", ", true);
		
		//	get collector's name, collecting method, and determiner's name
		this.addDetailAttribute(materialsCitation, COLLECTOR_NAME, " & ", true);
		
		this.addDetailAttribute(materialsCitation, COLLECTING_METHOD, " / ", true);
		
		this.addDetailAttribute(materialsCitation, DETERMINER_NAME, " & ", true);
		
		//	get collecting date
		Annotation[] collectingDates = materialsCitation.getAnnotations(COLLECTING_DATE);
		StringVector collectingDateValues = new StringVector();
		for (int d = 0; d < collectingDates.length; d++) {
			if (collectingDates[d].hasAttribute(VALUE_ATTRIBUTE))
				collectingDateValues.addElementIgnoreDuplicates((String) collectingDates[d].getAttribute(VALUE_ATTRIBUTE));
			if (collectingDates[d].hasAttribute(VALUE_ATTRIBUTE + "Min"))
				collectingDateValues.addElementIgnoreDuplicates((String) collectingDates[d].getAttribute(VALUE_ATTRIBUTE + "Min"));
			if (collectingDates[d].hasAttribute(VALUE_ATTRIBUTE + "Max"))
				collectingDateValues.addElementIgnoreDuplicates((String) collectingDates[d].getAttribute(VALUE_ATTRIBUTE + "Max"));
		}
		if (collectingDateValues.size() == 0) {
			String collectingDate;
			collectingDate = materialsCitation.getDocumentProperty(COLLECTING_DATE);
			if (collectingDate != null)
				collectingDateValues.addElementIgnoreDuplicates(collectingDate);
			collectingDate = materialsCitation.getDocumentProperty(COLLECTING_DATE + "Min");
			if (collectingDate != null)
				collectingDateValues.addElementIgnoreDuplicates(collectingDate);
			collectingDate = materialsCitation.getDocumentProperty(COLLECTING_DATE + "Max");
			if (collectingDate != null)
				collectingDateValues.addElementIgnoreDuplicates(collectingDate);
		}
		if (collectingDateValues.size() == 0) {
			String docTitle = materialsCitation.getDocumentProperty(LiteratureConstants.DOCUMENT_TITLE_ATTRIBUTE);
			if (docTitle != null) {
				TokenSequence dtTokens = Gamta.newTokenSequence(docTitle, materialsCitation.getTokenizer());
				Annotation[] dtYearAnnots = Gamta.extractAllMatches(dtTokens, "((2[01])|(1[4-9]))[0-9]{2}(\\s?\\-\\s?(((2[01])|(1[4-9]))?[0-9]{2}))?");
				TreeSet dtYears = new TreeSet();
				for (int y = 0; y < dtYearAnnots.length; y++) {
					String dtYear = TokenSequenceUtils.concatTokens(dtYearAnnots[y], true, true);
					if (dtYear.indexOf('-') == -1)
						dtYears.add(dtYear);
					else {
						String[] dtYearParts = dtYear.split("\\s?\\-\\s?");
						dtYears.add(dtYearParts[0]);
						if (dtYearParts.length > 1) {
							if (dtYearParts[1].length() < 4)
								dtYearParts[1] = (dtYearParts[0].substring(0, (4 - dtYearParts[1].length())) + dtYearParts[1]);
							dtYears.add(dtYearParts[1]);
						}
					}
				}
				if (dtYears.size() != 0) {
					collectingDateValues.addElementIgnoreDuplicates(((String) dtYears.first()) + "-01-01");
					collectingDateValues.addElementIgnoreDuplicates(((String) dtYears.last()) + "-12-31");
				}
			}
		}
		if (collectingDateValues.size() == 0) {
			String docYear = materialsCitation.getDocumentProperty(LiteratureConstants.DOCUMENT_DATE_ATTRIBUTE);
			if (docYear != null) {
				collectingDateValues.addElementIgnoreDuplicates(docYear + "-01-01");
				collectingDateValues.addElementIgnoreDuplicates(docYear + "-12-31");
			}
		}
		collectingDateValues.sortLexicographically(false, false);
		if (collectingDateValues.size() > 0) {
			materialsCitation.setAttribute(COLLECTING_DATE, collectingDateValues.firstElement());
			if (collectingDateValues.size() > 1) {
				materialsCitation.setAttribute((COLLECTING_DATE + "Min"), collectingDateValues.firstElement());
				materialsCitation.setAttribute((COLLECTING_DATE + "Max"), collectingDateValues.lastElement());
			}
		}
		if ((collectingDates.length  != 0) && !materialsCitation.hasAttribute(COLLECTING_DATE)) {
			materialsCitation.setAttribute(COLLECTING_DATE, TokenSequenceUtils.concatTokens(collectingDates[0], true, true));
			if (collectingDates.length > 1) {
				materialsCitation.setAttribute((COLLECTING_DATE + "Min"), TokenSequenceUtils.concatTokens(collectingDates[0], true, true));
				materialsCitation.setAttribute((COLLECTING_DATE + "Max"), TokenSequenceUtils.concatTokens(collectingDates[collectingDates.length-1], true, true));
			}
		}
		
		//	get type status
		Annotation[] typeStatuses = materialsCitation.getAnnotations(TYPE_STATUS);
		if (typeStatuses.length != 0) {
			String typeStatus = TokenSequenceUtils.concatTokens(typeStatuses[0], true, true).toLowerCase();
			if (materialsCitationIndex != null)
				materialsCitationIndex.put(TokenSequenceUtils.concatTokens(typeStatuses[0], true, true).toLowerCase(), materialsCitation);
			if (this.typeStatuses.containsKey(typeStatus))
				typeStatus = ((String) this.typeStatuses.get(typeStatus));
			materialsCitation.setAttribute(TYPE_STATUS, typeStatus);
		}
		
		//	get specimen counts
		Annotation[] specimenCounts = materialsCitation.getAnnotations(SPECIMEN_COUNT);
		
		//	no specimen counts at all, count 1 for each type status, or 1 constant as a last resort
		if (specimenCounts.length == 0)
			materialsCitation.setAttribute(SPECIMEN_COUNT, ("" + Math.max(1, typeStatuses.length)));
		
		//	implicit specimen counts only
		else {
			TreeSet specimenCountTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			StringIndex specimenTypeCounts = new StringIndex();
			
			//	add up explicit specimen counts
			for (int s = 0; s < specimenCounts.length; s++) {
				String type = ((String) specimenCounts[s].getAttribute(TYPE_ATTRIBUTE, "generic"));
				if (!"generic".equals(type))
					specimenCountTypes.add(type);
				int count = 1;
				try {
					count = Integer.parseInt(specimenCounts[s].firstValue());
				} catch (NumberFormatException nfe) {}
				specimenTypeCounts.add("", count);
				if (!"generic".equals(type))
					specimenTypeCounts.add(type, count);
			}
			
			//	test if we have any explicit specimen counts, or only ones implied by type statuses
			Annotation[] tsSpecimenCounts = AnnotationPatternMatcher.getMatches(materialsCitation, ("<" + SPECIMEN_COUNT + "> <" + TYPE_STATUS + ">"));
			
			//	if we only have specimen counts with associated type status, add 1 for each type status without an associated leading number
			if (specimenCounts.length <= tsSpecimenCounts.length)
				specimenTypeCounts.add("", (typeStatuses.length - specimenCounts.length));
			
			//	set attributes
			materialsCitation.setAttribute(SPECIMEN_COUNT, ("" + specimenTypeCounts.getCount("")));
			for (Iterator sctit = specimenCountTypes.iterator(); sctit.hasNext();) {
				String sct = ((String) sctit.next());
				materialsCitation.setAttribute((SPECIMEN_COUNT + "-" + sct), ("" + specimenTypeCounts.getCount(sct)));
			}
		}
		
		//	get collection code and specimen code
		this.addDetailAttribute(materialsCitation, COLLECTION_CODE, ", ", false);
		
		this.addDetailAttribute(materialsCitation, SPECIMEN_CODE, ", ", false);
		if ((materialsCitationIndex != null) && materialsCitation.hasAttribute(SPECIMEN_CODE)) {
			Annotation[] specimenCodes = materialsCitation.getAnnotations(SPECIMEN_CODE);
			for (int sc = 0; sc < specimenCodes.length; sc++)
				materialsCitationIndex.put(TokenSequenceUtils.concatTokens(specimenCodes[sc], true, true).toLowerCase(), materialsCitation);
		}
		
		//	substitute specimen code prefixes for missing collection code
		if (materialsCitation.hasAttribute(SPECIMEN_CODE) && !materialsCitation.hasAttribute(COLLECTION_CODE)) {
			StringVector collectionCodes = new StringVector();
			Annotation[] specimenCodes = materialsCitation.getAnnotations(SPECIMEN_CODE);
			for (int c = 0; c < specimenCodes.length; c++) {
				String collectionCode = specimenCodes[c].firstValue();
				while (collectionCode.endsWith("-"))
					collectionCode = collectionCode.substring(0, (collectionCode.length() - 1)).trim();
				if (collectionCode.matches("[A-Z]++"))
					collectionCodes.addElementIgnoreDuplicates(collectionCode);
			}
			if (collectionCodes.size() != 0)
				materialsCitation.setAttribute(COLLECTION_CODE, collectionCodes.concatStrings(", "));
		}
		
		//	resolve backward references
		Annotation[] backRefs = materialsCitation.getAnnotations(BACK_REFERENCE_TYPE);
		if (backRefs.length != 0) {
//			System.out.println("Resolving backward reference '" + TokenSequenceUtils.concatTokens(backRefs[0], true, true) + "'");
			Annotation referencedMaterialsCitation = null;
			if (materialsCitationIndex != null)
				for (Iterator mckit = materialsCitationIndex.keySet().iterator(); mckit.hasNext();) {
					String mcKey = ((String) mckit.next());
//					System.out.println(" - testing reference key '" + mcKey + "'");
					if (TokenSequenceUtils.indexOf(backRefs[0], mcKey, false) != -1) {
						referencedMaterialsCitation = ((Annotation) materialsCitationIndex.get(mcKey));
//						System.out.println(" ==> match: " + referencedMaterialsCitation.toXML());
						break;
					}
//					else System.out.println(" ==> no match");
				}
			if (referencedMaterialsCitation == null) {
				referencedMaterialsCitation = previousMaterialsCitation;
//				if (referencedMaterialsCitation != null)
//					System.out.println(" ==> using immediate predecessor");
			}
			if (referencedMaterialsCitation != null) {
				String[] attributesToCopy = referencedMaterialsCitation.getAttributeNames();
				for (int a = 0; a < attributesToCopy.length; a++) {
					if (!attributesToCopy[a].startsWith("_") && !attributesToCopy[a].startsWith(SPECIMEN_COUNT) && !TYPE_STATUS.equals(attributesToCopy[a]) && !SPECIMEN_CODE.equals(attributesToCopy[a]) && !materialsCitation.hasAttribute(attributesToCopy[a]))
						materialsCitation.setAttribute(attributesToCopy[a], referencedMaterialsCitation.getAttribute(attributesToCopy[a]));
				}
			}
		}
		for (int b = 0; b < backRefs.length; b++)
			materialsCitation.removeAnnotation(backRefs[b]);
	}
	
	private void addDetailAttribute(MutableAnnotation materialsCitation, String type, String separator, boolean normalizeValue) {
		StringVector attributeValues = new StringVector();
		Annotation[] attributeParts = materialsCitation.getAnnotations(type);
		for (int p = 0; p < attributeParts.length; p++)
			attributeValues.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(attributeParts[p], normalizeValue, true));
		if (attributeValues.size() == 0) {
			String value = materialsCitation.getDocumentProperty(type);
			if (value != null)
				attributeValues.addElement(value);
		}
		if (attributeValues.size() != 0)
			materialsCitation.setAttribute(type, attributeValues.concatStrings(separator));
	}
	
	private HashMap highlightAttributeCache = new HashMap();
	private Color getAnnotationHighlight(String type) {
		Color color = ((Color) this.highlightAttributeCache.get(type));
		if (color == null) {
			if (COLLECTING_COUNTRY_ANNOTATION_TYPE.equals(type))
				color = Color.ORANGE;
			else if (COLLECTING_REGION_ANNOTATION_TYPE.equals(type))
				color = Color.GREEN.brighter();
			else if (COLLECTING_COUNTY_ANNOTATION_TYPE.equals(type))
				color = Color.PINK;
			else if (MATERIALS_CITATION_ANNOTATION_TYPE.equals(type))
				color = Color.YELLOW;
			else color = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			this.highlightAttributeCache.put(type, color);
		}
		return color;
	}
}