///*
// * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package de.uka.ipd.idaho.plugins.geoCoding;
//
//
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.Properties;
//
//import javax.swing.BorderFactory;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JDialog;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.QueriableAnnotation;
//import de.uka.ipd.idaho.gamta.Token;
//import de.uka.ipd.idaho.gamta.TokenSequence;
//import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
//import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
//import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
//import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
//import de.uka.ipd.idaho.plugins.geoCoding.geoDataProviders.GeoNamesGeoDataProvider;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//
///**
// * @author sautter
// *
// */
//public class GeoReferencer extends AbstractConfigurableAnalyzer implements LiteratureConstants, LocationConstants {
//	
////	private static final String EXCLUDABLE_ANNOTATION_TYPES_SETTING_NAME = "potentialLocationAnnotationTypes";
////	private static final String POTENTIAL_LOCATION_ANNOTATION_TYPES_SETTING_NAME = "potentialLocationAnnotationTypes";
////	
////	private StringVector excludableAnnotationTypes = new StringVector();
////	private StringVector potentialLocationAnnotationTypes = new StringVector();
//	
//	private GeoDataProvider geoDataProvider = null;
//	
//	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	get and bucketize location names (paragraph-wise for the page numbers)
//		StringVector bucketNames = new StringVector();
//		HashMap bucketsByName = new HashMap();
//		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
//		for (int p = 0; p < paragraphs.length; p++) {
//			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
//			QueriableAnnotation[] locations = data.getAnnotations(LOCATION_TYPE);
//			for (int l = 0; l < locations.length; l++) {
//				String name = locations[l].getAttribute(NAME_ATTRIBUTE, "").toString();
//				if (name.length() == 0) {
//					int nameLength = 1;
//					for (int t = 1; t < locations[l].size(); t++) {
//						if (Gamta.isWord(locations[l].valueAt(t)))
//							nameLength ++;
//						else t = locations[l].size();
//					}
////					name = TokenSequenceUtils.concatTokens(locations[l], 0, nameLength);
//					name = TokenSequenceUtils.concatTokens(locations[l], 0, nameLength, true, true);
//					locations[l].setAttribute(NAME_ATTRIBUTE, name);
//				}
//				if (bucketsByName.containsKey(name))
//					((LocationBucket) bucketsByName.get(name)).locations.add(locations[l]);
//				else {
//					LocationBucket bucket = new LocationBucket(name, locations[l], ((pageNumber == null) ? "" : pageNumber.toString()));
//					bucketNames.addElementIgnoreDuplicates(name);
//					bucketsByName.put(name, bucket);
//				}
//			}
//		}
////		System.out.println("locations bucketized");
//		
//		//	line up buckets
//		ArrayList buckets = new ArrayList();
//		for (int b = 0; b < bucketNames.size(); b++)
//			buckets.add(bucketsByName.get(bucketNames.get(b)));
////		System.out.println("locations lined up");
//		
//		//	transfer coordinates if one annotation in bucket already has them, and sort out these buckets
//		int bucketIndex = 0;
//		while (bucketIndex < buckets.size()) {
//			LocationBucket lb = ((LocationBucket) buckets.get(bucketIndex));
//			String existingLongitude = "";
//			String existingLatitude = "";
//			String existingLongLatPrecision = "";
//			for (int t = 0; t < lb.locations.size(); t++) {
//				QueriableAnnotation location = ((QueriableAnnotation) lb.locations.get(t));
//				if (location.hasAttribute(LONGITUDE_ATTRIBUTE))
//					existingLongitude = location.getAttribute(LONGITUDE_ATTRIBUTE, "").toString();
//				if (location.hasAttribute(LATITUDE_ATTRIBUTE))
//					existingLatitude = location.getAttribute(LATITUDE_ATTRIBUTE, "").toString();
//				if (location.hasAttribute(LONG_LAT_PRECISION_ATTRIBUTE))
//					existingLongLatPrecision = location.getAttribute(LONG_LAT_PRECISION_ATTRIBUTE, "").toString();
//			}
//			
//			//	coordinates not found, keep bucket
//			if ((existingLongitude.length() == 0) || (existingLatitude.length() == 0)) bucketIndex++;
//			
//			//	found proper coordinates in Annotations, bucket done
//			else {
//				for (int l = 0; l < lb.locations.size(); l++) {
//					Annotation location = ((Annotation) lb.locations.get(l));
//					location.setAttribute(LONGITUDE_ATTRIBUTE, existingLongitude);
//					location.setAttribute(LATITUDE_ATTRIBUTE, existingLatitude);
//					if (existingLongLatPrecision.length() != 0)
//						location.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, existingLongLatPrecision);
//				}
//				buckets.remove(bucketIndex);
//			}
//		}
//		System.out.println("coordinates transferred");
//		
//		//	ask if web access allowed if in offline mode
//		if ((parameters != null) && (!parameters.containsKey(ONLINE_PARAMETER)) && (JOptionPane.showConfirmDialog(null, "The GeoReferencer has been invoked in offline mode, allow fetching geo data anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
//			return;
//		
//		//	create status dialog
//		StatusDialog sd = null;
//		if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
//			sd = new StatusDialog();
//			sd.online.setSelected(true);
//			sd.popUp();
//		}
//		System.out.println("status dialog checked");
//		
//		//	try to load data for buckets
//		for (int b = 0; b < buckets.size(); b++) {
//			LocationBucket lb = ((LocationBucket) buckets.get(b));
//			
//			if (sd != null) sd.setLabel("Getting data for '" + lb.name + "' ...");
//			System.out.println("Getting data for '" + lb.name + "' ...");
//			
//			//	check for interrupt
//			if ((sd != null) && sd.interrupted) {
//				sd.dispose();
//				return;
//			}
//			
//			//	get locations for given name
//			lb.dataSets = this.geoDataProvider.getLocations(lb.name);
//			System.out.println("  got " + lb.dataSets.length + " data sets");
//		}
//		System.out.println("got location data");
//		
//		//	compute document center if more than one location given
//		LocationCoordinateComparator docLocComparator =  null;
//		if (buckets.size() > 1) {
//			
//			//	compute document center
//			float docLongitude = 0;
//			float docLatitude = 0;
//			int averageDenominator = 0;
//			for (int n = 0; n < buckets.size(); n++) {
//				LocationBucket lb = ((LocationBucket) buckets.get(n));
//				
//				//	compute ambiguity
//				int longitudeAmbiguity = 0;
//				int latitudeAmbiguity = 0;
//				for (int l = 0; l < lb.dataSets.length; l++) {
//					if (lb.dataSets[l].longDeg != Location.UNKNOWN_LONGITUDE) longitudeAmbiguity++;
//					if (lb.dataSets[l].latDeg != Location.UNKNOWN_LATITUDE) latitudeAmbiguity++;
//				}
//				
//				//	get weight (= document frequency) of current location 
//				int locationWeight = lb.locations.size();
//				
//				//	add weighted longitude and latitude to document center coordinates
//				for (int l = 0; l < lb.dataSets.length; l++) {
//					if (lb.dataSets[l].longDeg != Location.UNKNOWN_LONGITUDE)
//						docLongitude += ((lb.dataSets[l].longDeg / longitudeAmbiguity) * locationWeight);
//					if (lb.dataSets[l].latDeg != Location.UNKNOWN_LATITUDE) 
//						docLatitude += ((lb.dataSets[l].latDeg / latitudeAmbiguity) * locationWeight);
//				}
//				
//				//	increment denominator for weighted average
//				averageDenominator += locationWeight;
//			}
//			
//			//	compute average
//			docLongitude /= averageDenominator;
//			docLatitude /= averageDenominator;
//			
//			//	build comparator
////			docLocComparator = new LocationCoordinateComparator(new Location("docLocation", docLongitude, docLatitude));
//			docLocComparator = new LocationCoordinateComparator(new Location("docLocation", docLongitude, docLatitude, Location.UNKNOWN_LONG_LAT_PRECISION));
//			
//			//	sort locations
//			for (int n = 0; n < buckets.size(); n++) {
//				LocationBucket lb = ((LocationBucket) buckets.get(n));
//				Arrays.sort(lb.dataSets, docLocComparator);
//			}
//		}
//		
//		//	try to find appropriate location for annotations
//		for (int n = 0; n < buckets.size(); n++) {
//			LocationBucket lb = ((LocationBucket) buckets.get(n));
//			
//			//	one location
//			if (lb.dataSets.length == 1)
//				lb.dataSet = lb.dataSets[0];
//			
//			//	multiple locations
//			else if (lb.dataSets.length != 0) {
//				String country = null;
//				String longitude = null;
//				String latitude = null;
//				
//				//	gather given data
//				for (int l = 0; l < lb.locations.size(); l++) {
//					QueriableAnnotation location = ((QueriableAnnotation) lb.locations.get(l));
//					Annotation[] inTextCoordinates = location.getAnnotations(GEO_COORDINATE_TYPE);
//					for (int c = 0; c < inTextCoordinates.length; c++) {
//						if (inTextCoordinates[c].hasAttribute(VALUE_ATTRIBUTE)) {
//							String value = inTextCoordinates[c].getAttribute(VALUE_ATTRIBUTE).toString();
//							if (LONGITUDE_ORIENTATION.equals(inTextCoordinates[c].getAttribute(ORIENTATION_ATTRIBUTE)))
//								longitude = value;
//							else if (LATITUDE_ORIENTATION.equals(inTextCoordinates[c].getAttribute(ORIENTATION_ATTRIBUTE)))
//								latitude = value;
//						}
//					}
//					if (location.hasAttribute(COUNTRY_ATTRIBUTE))
//						country = location.getAttribute(COUNTRY_ATTRIBUTE).toString();
//				}
//				
//				//	found coordinates in text
//				if ((longitude != null) && (latitude != null)) {
////					LocationCoordinateComparator locComparator = new LocationCoordinateComparator(new Location("location", longitude, latitude));
//					LocationCoordinateComparator locComparator = new LocationCoordinateComparator(new Location("location", longitude, latitude, ("" + Location.UNKNOWN_LONG_LAT_PRECISION)));
//					Arrays.sort(lb.dataSets, locComparator);
//				}
//				
//				//	check for country name
//				else if (country != null) {
//					Arrays.sort(lb.dataSets, new Comparator() {
//						public int compare(Object location1, Object location2) {
//							if (Location.UNKNOWN_ATTRIBUTE_VALUE.equals(((Location) location1).attributes.getProperty(COUNTRY_ATTRIBUTE)))
//								return (Location.UNKNOWN_ATTRIBUTE_VALUE.equals(((Location) location2).attributes.getProperty(COUNTRY_ATTRIBUTE)) ? 0 : 1);
//							else if (Location.UNKNOWN_ATTRIBUTE_VALUE.equals(((Location) location2).attributes.getProperty(COUNTRY_ATTRIBUTE)))
//								return -1;
//							else return 0;
//						}
//					});
//				}
//				
//				//	do pre selection
//				lb.dataSet = lb.dataSets[0];
//			}
//		}
//		
//		//	process buckets
//		while (!buckets.isEmpty()) {
//			
//			//	process data
//			bucketIndex = 0;
//			while (bucketIndex < buckets.size()) {
//				LocationBucket lb = ((LocationBucket) buckets.get(bucketIndex));
//				
//				//	not allowed to bet feedback, ignore name
//				if (lb.dataSet == IGNORE)
//					buckets.remove(bucketIndex);
//				
//				//	keep bucket for feedback
//				else bucketIndex++;
//			}
//			
//			//	get feedback for ambiguous or unknown names
//			this.getFeedback(((LocationBucket[]) buckets.toArray(new LocationBucket[buckets.size()])), data, sd);
//			
//			//	process feedback
//			bucketIndex = 0;
//			while (bucketIndex < buckets.size()) {
//				LocationBucket lb = ((LocationBucket) buckets.get(bucketIndex));
//				
//				//	remove
//				if (lb.dataSet == REMOVE) {
//					for (int t = 0; t < lb.locations.size(); t++)
//						data.removeAnnotation((Annotation) lb.locations.get(t));
//					
//					//	bucket done
//					buckets.remove(bucketIndex);
//				}
//				
//				//	ignore location for now
//				else if (lb.dataSet == IGNORE)
//					buckets.remove(bucketIndex);
//				
//				//	ignore location for now
//				else if (lb.dataSets.length == 0)
//					buckets.remove(bucketIndex);
//				
//				//	coordinates selected
//				else if (lb.dataSet != null) {
//					for (int l = 0; l < lb.locations.size(); l++) {
//						Annotation location = ((Annotation) lb.locations.get(l));
//						
//						//	add coordinates
//						location.setAttribute(LONGITUDE_ATTRIBUTE, ("" + lb.dataSet.longDeg));
//						location.setAttribute(LATITUDE_ATTRIBUTE, ("" + lb.dataSet.latDeg));
//						if (lb.dataSet.longLatPrecision != Location.UNKNOWN_LONG_LAT_PRECISION)
//							location.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, ("" + lb.dataSet.longLatPrecision));
//						
//						//	add other attributes (if available)
//						for (int a = 0; a < Location.ATTRIBUTE_NAMES.length; a++) {
//							String attributeValue = lb.dataSet.attributes.getProperty(Location.ATTRIBUTE_NAMES[a], Location.UNKNOWN_ATTRIBUTE_VALUE);
//							if (!Location.UNKNOWN_ATTRIBUTE_VALUE.equals(attributeValue)) {
//								TokenSequence avts = Gamta.newTokenSequence(attributeValue, null);
//								location.setAttribute(Location.ATTRIBUTE_NAMES[a], TokenSequenceUtils.concatTokens(avts, true, true));
//							}
//						}
//					}
//					
//					//	bucket done
//					buckets.remove(bucketIndex);
//				}
//				
//				//	name changed, keep bucket for next round
//				else bucketIndex++;
//			}
//		}
//		
//		//	close status dialog
//		if (sd != null) sd.dispose();
//		
////		if (this.potentialLocationAnnotationTypes.size() != 0) {
////			
////			//	get feedback for genericNE Annotations that are not (part of) excludable Annotations
////			ArrayList annotationList = new ArrayList();
////			for (int t = 0; t < this.excludableAnnotationTypes.size(); t++) {
////				Annotation[] annotations = data.getAnnotations(this.excludableAnnotationTypes.get(t));
////				for (int a = 0; a < annotations.length; a++)
////					annotationList.add(annotations[a]);
////			}
////			for (int t = 0; t < this.potentialLocationAnnotationTypes.size(); t++) {
////				Annotation[] annotations = data.getAnnotations(this.potentialLocationAnnotationTypes.get(t));
////				for (int a = 0; a < annotations.length; a++)
////					annotationList.add(annotations[a]);
////			}
////			Collections.sort(annotationList, new Comparator() {
////				
////				/** @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
////				 */
////				public int compare(Object o1, Object o2) {
////					if (o1 == null) return 1;
////					if (o2 == null) return -1;
////					if (!(o1 instanceof Annotation)) return 1;
////					if (!(o2 instanceof Annotation)) return -1;
////					
////					Annotation a1 = ((Annotation) o1);
////					Annotation a2 = ((Annotation) o2);
////					
////					//	first, order by start index ascending
////					int c = (a1.getStartIndex() - a2.getStartIndex());
////					if (c != 0) return c;
////					
////					//	second, order by length descending in order to preserve depth first order
////					c = (a2.size() - a1.size());
////					if (c != 0) return c;
////					
////					//	push back genericNE
////					if (potentialLocationAnnotationTypes.contains(a1.getType())) return 1;
////					if (potentialLocationAnnotationTypes.contains(a2.getType())) return -1;
////					return a1.getType().compareTo(a2.getType());
////				}
////			});
////			
////			//	remove generic NEs containd in ignorable Annotations
////			int index = 1;
////			Annotation a1 = ((annotationList.size() == 0) ? null : ((Annotation) annotationList.get(0)));
////			while (index < annotationList.size()) {
////				Annotation a2 = ((Annotation) annotationList.get(index));
////				if (a1.overlaps(a2)) annotationList.remove(index);
////				else {
////					a1 = a2;
////					index++;
////				}
////			}
////			
////			//	extract generic NEs
////			index = 0;
////			while (index < annotationList.size()) {
////				a1 = ((Annotation) annotationList.get(index));
////				if ("genericNE".equalsIgnoreCase(a1.getType())) index++;
////				else annotationList.remove(index);
////			}
////			Annotation[] potentialLocations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
////			
////			//	handle potential locations
////			if (potentialLocations.length != 0) {
////				
////				//	get distinct NE values
////				StringVector potentialLocationNames = new StringVector();
////				for (int e = 0; e < potentialLocations.length; e++)
////					potentialLocationNames.addElementIgnoreDuplicates(potentialLocations[e].getValue());
////				
////				//	get user feedback
////				potentialLocationNames.sortLexicographically(false, false);
////				JCheckBox[] feedbacks = new JCheckBox[potentialLocationNames.size()];
////				JPanel feedbackPanel = new JPanel(new GridLayout(0, 1));
////				for (int c = 0; c < potentialLocationNames.size(); c++) {
////					feedbacks[c] = new JCheckBox(potentialLocationNames.get(c), false);
////					feedbackPanel.add(feedbacks[c]);
////				}
////				final JDialog feedbackDialog = new JDialog(((JFrame) null), "Please check if these Strings are location names", true);
////				feedbackDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
////				JButton okButton = new JButton("OK");
////				okButton.addActionListener(new ActionListener() {
////					public void actionPerformed(ActionEvent ae) {
////						feedbackDialog.dispose();
////					}
////				});
////				feedbackDialog.getContentPane().setLayout(new BorderLayout());
////				feedbackDialog.getContentPane().add(feedbackPanel, BorderLayout.CENTER);
////				feedbackDialog.getContentPane().add(okButton, BorderLayout.SOUTH);
////				feedbackDialog.setVisible(true);
////				
////				//	handle user feedback
////				StringVector locationNames = new StringVector();
////				for (int e = 0; e < potentialLocationNames.size(); e++) {
////					if (feedbacks[e].isSelected())
////						locationNames.addElementIgnoreDuplicates(potentialLocationNames.get(e));
////				}
////				
////				//	write Annotation types
////				for (int e = 0; e < potentialLocations.length; e++)
////					if (locationNames.contains(potentialLocations[e].getValue())) potentialLocations[e].changeTypeTo(Location.LOCATION_ANNOTATION_TYPE);
////			}
////		}
////		
////		//	get annotated locations
////		Annotation[] locationAnnotations = data.getAnnotations(Location.LOCATION_ANNOTATION_TYPE);
////		StringVector distinctLocationNames = new StringVector();
////		StringVector locationNames = new StringVector();
////		
////		for (int l = 0; l < locationAnnotations.length; l++) {
////			distinctLocationNames.addElementIgnoreDuplicates(locationAnnotations[l].getValue());
////			locationNames.addElement(locationAnnotations[l].getValue());
////		}
////		
////		//	enumerations to split
////		StringVector locationNamesToSplit = new StringVector();
////		
////		//	look up probable locations in the geo thesaurus
////		if (distinctLocationNames.size() != 0) {
////			HashMap locationNameMappings = new HashMap();
////			
////			for (int l = 0; l < distinctLocationNames.size(); l++) {
////				
////				String locationName = distinctLocationNames.get(l);
////				
////				//	get locations for given name
////				Location[] locations = this.geoDataProvider.getLocations(locationName);
////				
////				//	produce dummy location if gazetteer lookup returned no results
////				if (locations.length == 0) {
////					
////					//	split enumerations if lookup returns no result for complete value
////					if (locationName.indexOf(" and ") != -1) {
////						locationNamesToSplit.addElementIgnoreDuplicates(locationName);
////						String[] parts = locationName.split("\\sand\\s");
////						for (int p = 0; p < parts.length; p++)
////							distinctLocationNames.addElementIgnoreDuplicates(parts[p]);
////						
////					} else {
////						
////						//	generate dummy location
////						locations = new Location[1];
////						locations[0] = new Location(locationName, Location.UNKNOWN_LONGITUDE, Location.UNKNOWN_LATITUDE);
////						
////						//	map location name to locations
////						locationNameMappings.put(locationName, locations);
////					}
////					
////				//	map location name to locations
////				} else locationNameMappings.put(locationName, locations);
////			}
////			
////			//	remove unmapped location names
////			int index = 0;
////			while (index < distinctLocationNames.size()) {
////				if (locationNameMappings.containsKey(distinctLocationNames.get(index))) index++;
////				else distinctLocationNames.remove(index);
////			}
////			
////			//	disambiguate locations
////			
////			//	document locality disambiguation strategy
////			if (locationNameMappings.size() > 1) {
////				
////				//	compute document center
////				float docLongitude = 0;
////				float docLatitude = 0;
////				int averageDenominator = 0;
////				for (int n = 0; n < distinctLocationNames.size(); n++) {
////					Location[] locations = ((Location[]) locationNameMappings.get(distinctLocationNames.get(n)));
////					
////					//	compute ambiguity
////					int longitudeAmbiguity = 0;
////					int latitudeAmbiguity = 0;
////					for (int l = 0; l < locations.length; l++) {
////						if (locations[l].longDeg != Location.UNKNOWN_LONGITUDE) longitudeAmbiguity++;
////						if (locations[l].latDeg != Location.UNKNOWN_LATITUDE) latitudeAmbiguity++;
////					}
////					
////					//	get weight (= document frequency) of current location 
////					int locationWeight = locationNames.getElementCount(distinctLocationNames.get(n));
////					
////					//	add weighted longitude and latitude to document center coordinates
////					for (int l = 0; l < locations.length; l++) {
////						if (locations[l].longDeg != Location.UNKNOWN_LONGITUDE)
////							docLongitude += ((locations[l].longDeg / longitudeAmbiguity) * locationWeight);
////						if (locations[l].latDeg != Location.UNKNOWN_LATITUDE) 
////							docLatitude += ((locations[l].latDeg / latitudeAmbiguity) * locationWeight);
////					}
////					
////					//	increment denominator for weighted average
////					averageDenominator += locationWeight;
////				}
////				
////				//	compute average
////				docLongitude /= averageDenominator;
////				docLatitude /= averageDenominator;
////				
////				//	build comparator
////				Location docLocation = new Location("docLocation", docLongitude, docLatitude);
////				LocationCoordinateComparator docLocComparator = new LocationCoordinateComparator(docLocation);
////				
////				//	sort locations
////				for (int n = 0; n < distinctLocationNames.size(); n++) {
////					Location[] locations = ((Location[]) locationNameMappings.get(distinctLocationNames.get(n)));
////					Arrays.sort(locations, docLocComparator);
////				}
////				
////			//	biggest city disambiguation strategy
////			} else {
////				LocationImportanceComparator docLocComparator = new LocationImportanceComparator();
////				
////				//	sort locations
////				for (int n = 0; n < distinctLocationNames.size(); n++) {
////					Location[] locations = ((Location[]) locationNameMappings.get(distinctLocationNames.get(n)));
////					Arrays.sort(locations, docLocComparator);
////				}
////			}
////			
////			//	add coordinates to Annotations
////			for (int l = 0; l < locationAnnotations.length; l++) {
////				String locationName = locationAnnotations[l].getValue();
////				if (locationNameMappings.containsKey(locationName)) {
////					Location[] locations = ((Location[]) locationNameMappings.get(locationName));
////					if (locations.length != 0) {
////						locationAnnotations[l].setAttribute(Location.LONGITUDE_ATTRIBUTE, locations[0].longitude);
////						locationAnnotations[l].setAttribute(Location.LATITUDE_ATTRIBUTE, locations[0].latitude);
////						if (locations[0].elevation != Location.UNKNOWN_ELEVATION)
////							locationAnnotations[l].setAttribute(Location.ELEVATION_ATTRIBUTE, ("" + locations[0].elevation));
////					}
////				} else if (locationNamesToSplit.contains(locationName)) {
////					int split = TokenSequenceUtils.indexOf(locationAnnotations[l], "and");
////					
////					Annotation part1 = data.addAnnotation(Location.LOCATION_ANNOTATION_TYPE, locationAnnotations[l].getStartIndex(), split);
////					if (locationNameMappings.containsKey(part1.getValue())) {
////						Location[] locations = ((Location[]) locationNameMappings.get(part1.getValue()));
////						if (locations.length != 0) {
////							part1.setAttribute(Location.LONGITUDE_ATTRIBUTE, locations[0].longitude);
////							part1.setAttribute(Location.LATITUDE_ATTRIBUTE, locations[0].latitude);
////							if (locations[0].elevation != Location.UNKNOWN_ELEVATION)
////								part1.setAttribute(Location.ELEVATION_ATTRIBUTE, ("" + locations[0].elevation));
////						}
////					}
////					
////					Annotation part2 = data.addAnnotation(Location.LOCATION_ANNOTATION_TYPE, (locationAnnotations[l].getStartIndex() + split + 1), (locationAnnotations[l].size() - split - 1));
////					if (locationNameMappings.containsKey(part2.getValue())) {
////						Location[] locations = ((Location[]) locationNameMappings.get(part2.getValue()));
////						if (locations.length != 0) {
////							part2.setAttribute(Location.LONGITUDE_ATTRIBUTE, locations[0].longitude);
////							part2.setAttribute(Location.LATITUDE_ATTRIBUTE, locations[0].latitude);
////							if (locations[0].elevation != Location.UNKNOWN_ELEVATION)
////								part2.setAttribute(Location.ELEVATION_ATTRIBUTE, ("" + locations[0].elevation));
////						}
////					}
////					
////					data.removeAnnotation(locationAnnotations[l]);
////				}
////			}
////		}
//	}
//	
//	//	return false only if interrupted
//	private void getFeedback(LocationBucket[] buckets, TokenSequence text, StatusDialog sd) {
//		
//		//	don't show empty dialog
//		if (buckets.length == 0) return;
//		
//		//	assemble dialog
//		final JDialog td = new JDialog(sd, "Please Check Unclear GeoCoordinates", true);
//		JPanel panel = new JPanel(new GridBagLayout());
//		JScrollPane panelBox = new JScrollPane(panel);
//		panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		panelBox.getVerticalScrollBar().setUnitIncrement(50);
//		td.getContentPane().setLayout(new BorderLayout());
//		td.getContentPane().add(panelBox, BorderLayout.CENTER);
//		
//		final GridBagConstraints gbc = new GridBagConstraints();
//		gbc.insets.top = 2;
//		gbc.insets.bottom = 2;
//		gbc.insets.left = 5;
//		gbc.insets.right = 5;
//		gbc.fill = GridBagConstraints.HORIZONTAL;
//		gbc.weightx = 1;
//		gbc.weighty = 0;
//		gbc.gridwidth = 1;
//		gbc.gridheight = 1;
//		gbc.gridx = 0;
//		gbc.gridy = 0;
//		
//		//	count buckets with data
//		int gotData = 0;
//		for (int b = 0; b < buckets.length; b++) {
//			if (buckets[b].dataSets.length != 0)
//				gotData++;
//		}
//		
//		//	add classification boxes
//		LocationBucketTray[] trays = new LocationBucketTray[gotData];
//		gotData = 0;
//		for (int b = 0; b < buckets.length; b++) {
//			if (buckets[b].dataSets.length != 0) {
//				trays[gotData] = new LocationBucketTray(buckets[b], text);
//				panel.add(trays[gotData], gbc.clone());
//				gbc.gridy++;
//				gotData ++;
//			}
//		}
//		
//		//	add OK button
//		JButton okButton = new JButton("OK");
//		okButton.setBorder(BorderFactory.createRaisedBevelBorder());
//		okButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				td.dispose();
//			}
//		});
//		
//		//	add OK button
//		JButton cancelButton = new JButton("Cancel");
//		cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
//		cancelButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				gbc.fill = GridBagConstraints.NONE;
//				td.dispose();
//			}
//		});
//		
//		JPanel buttonPanel = new JPanel(new GridBagLayout());
//		gbc.gridwidth = 1;
//		gbc.gridheight = 1;
//		gbc.gridx = 0;
//		gbc.gridy = 0;
//		buttonPanel.add(okButton, gbc.clone());
//		gbc.gridx++;
//		buttonPanel.add(cancelButton, gbc.clone());
//		td.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//		
//		//	ensure dialog is closed with button
//		td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
//		
//		//	get feedback
//		td.setSize(500, (Math.min(650, (78 * buckets.length) + 50)));
//		td.setLocationRelativeTo(null);
//		td.setVisible(true);
//		
//		//	process feedback
//		for (int t = 0; t < trays.length; t++) {
//			trays[t].commitChange();
//			
//			//	main dialog cancelled, ignore all buckets not having an LSID (or dummy for other option) selected
//			if ((gbc.fill == GridBagConstraints.NONE) && trays[t].bucket.dataSet == null)
//				trays[t].bucket.dataSet = IGNORE;
//		}
//	}
//	
//	private class LocationBucket {
//		private String name;
////		private QueriableAnnotation location;
//		private ArrayList locations = new ArrayList();
//		private Location dataSet = null;
//		private Location[] dataSets = new Location[0];
//		private String pageNumber;
//		LocationBucket(String name, QueriableAnnotation location, String pageNumber) {
//			this.name = name;
////			this.location = location;
//			this.locations.add(location);
//			this.pageNumber = pageNumber;
//		}
//	}
//	
//	//	special dummy location data sets to indicate special actions
//	private static final Location REMOVE = new Location("", 0f, 0f, 0); 
//	private static final Location IGNORE = new Location("", 0f, 0f, 0); 
//	
//	//	special options to indicate special actions
//	private static final String IGNORE_LOCATION_CHOICE = "Ignore location for geo referencing";
//	private static final String REMOVE_LOCATION_CHOICE = "Remove location Annotation";
//	
//	private class LocationBucketTray extends JPanel {
//		private LocationBucket bucket;
//		private JLabel label = new JLabel("", JLabel.LEFT);
//		private JComboBox selector;
//		
//		LocationBucketTray(final LocationBucket bucket, final TokenSequence text) {
//			super(new BorderLayout(), true);
////			System.out.println("LocationBucketTray: got " + bucket.dataSets.length + " choices");
//			this.bucket = bucket;
//			
//			this.setBorder(BorderFactory.createEtchedBorder());
//			
//			label.setText(
//					"<HTML>The coordinates for this location name are ambiguous:<BR>" +
//					"<B>" + bucket.name + "</B></HTML>"
//				);
//			this.label.addMouseListener(new MouseAdapter() {
//				public void mouseClicked(MouseEvent me) {
//					StringVector docNames = new StringVector();
//					for (int a = 0; a < bucket.locations.size(); a++) {
//						Annotation location = ((Annotation) bucket.locations.get(a));
//						docNames.addElementIgnoreDuplicates(buildLabel(text, location, 10) + " (at " + location.getStartIndex() + (" on page " + bucket.pageNumber) + ")");
//					}
//					String message = ("<HTML>This location name appears in the document in the following positions:<BR>&nbsp;&nbsp;&nbsp;");
//					message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
//					message += "</HTML>";
//					JOptionPane.showMessageDialog(LocationBucketTray.this, message, ("Forms Of \"" + bucket.name + "\""), JOptionPane.INFORMATION_MESSAGE);
//				}
//			});
//			
//			final String[] options = new String[this.bucket.dataSets.length + 2];
//			for (int d = 0; d < this.bucket.dataSets.length; d++) options[d] = this.bucket.dataSets[d].toString();
//			options[this.bucket.dataSets.length + 0] = IGNORE_LOCATION_CHOICE;
//			options[this.bucket.dataSets.length + 1] = REMOVE_LOCATION_CHOICE;
//			this.selector = new JComboBox(options);
//			this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.selector.setEditable(false);
//			this.selector.setSelectedItem((this.bucket.dataSet == null) ? IGNORE_LOCATION_CHOICE : this.bucket.dataSet.toString());
//			
//			this.add(this.label, BorderLayout.NORTH);
//			this.add(this.selector, BorderLayout.CENTER);
//		}
//		
//		void commitChange() {
//			Object selected = this.selector.getSelectedItem();
//			if (IGNORE_LOCATION_CHOICE.equals(selected)) {
//				this.bucket.dataSet = IGNORE;
//				
//			} else if (REMOVE_LOCATION_CHOICE.equals(selected)) {
//				this.bucket.dataSet = REMOVE;
//				
//			} else this.bucket.dataSet = this.bucket.dataSets[this.selector.getSelectedIndex()];
//		}
//	}
//	
//	private String buildLabel(TokenSequence text, Annotation annot, int envSize) {
//		int aStart = annot.getStartIndex();
//		int aEnd = annot.getEndIndex();
//		int start = Math.max(0, (aStart - envSize));
//		int end = Math.min(text.size(), (aEnd + envSize));
//		StringBuffer sb = new StringBuffer("... ");
//		Token lastToken = null;
//		Token token = null;
//		for (int t = start; t < end; t++) {
//			lastToken = token;
//			token = text.tokenAt(t);
//			
//			//	end highlighting value
//			if (t == aEnd) sb.append("</B>");
//			
//			//	add spacer
//			if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
//			
//			//	start highlighting value
//			if (t == aStart) sb.append("<B>");
//			
//			//	append token
//			sb.append(token);
//		}
//		
//		return sb.append(" ...").toString();
//	}
//	
//	private class StatusDialog extends JDialog implements Runnable {
//		JLabel label = new JLabel("", JLabel.CENTER);
//		StringVector labelLines = new StringVector();
//		JCheckBox online = new JCheckBox("Allow GeoNames Lookup", true);
//		boolean interrupted = false;
//		Thread thread = null;
//		StatusDialog() {
//			super(((JFrame) null), "GeoReferencer", true);
//			this.getContentPane().setLayout(new BorderLayout());
//			this.getContentPane().add(this.label, BorderLayout.CENTER);
//			
//			JButton stopButton = new JButton("Stop GeoReferencing");
//			stopButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			stopButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (JOptionPane.showConfirmDialog(StatusDialog.this, "Do you really want to stop the geo referencer?", "Confirm Stop GeoReferencer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
//						interrupted = true;
//				}
//			});
//			JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//			controlPanel.add(this.online);
//			controlPanel.add(stopButton);
//			this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
//			
//			this.setSize(400, 120);
//			this.setLocationRelativeTo(null);
//			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//			
//			this.addWindowListener(new WindowAdapter() {
//				boolean isInvoked = false;
//				public void windowClosing(WindowEvent we) {
//					if (this.isInvoked) return; // avoid loops
//					this.isInvoked = true;
//					if (JOptionPane.showConfirmDialog(StatusDialog.this, "Closing this status dialog will disable you to monitor GeoReferencer", "Confirm Close Status Dialog", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
//						dispose();
//					this.isInvoked = false;
//				}
//			});
//		}
//		
//		void setLabel(String text) {
//			this.labelLines.addElement(text);
//			while (this.labelLines.size() > 3)
//				this.labelLines.remove(0);
//			this.label.setText("<HTML>" + this.labelLines.concatStrings("<BR>") + "</HTML>");
//			this.label.validate();
//		}
//		
//		void popUp() {
//			if (this.thread == null) {
//				this.thread = new Thread(this);
//				this.thread.start();
//			}
//		}
//		
//		public void run() {
//			this.setVisible(true);
//			this.thread = null;
//		}
//	}
//	
//	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
//		System.out.println("Geocoder: Initializing ...");
//		
////		//	initialize geo data provider
////		File geoDataCache = new File(this.getParameter(AbstractGeoDataProvider.CACHE_BASE_PATH_PARAMETER, AbstractGeoDataProvider.DEFAULT_CACHE_BASE_PATH));
////		StringVector geoDataPaths = new StringVector();
////		try {
////			geoDataPaths.addContentIgnoreDuplicates(this.loadList(AbstractGeoDataProvider.LOCAL_GAZETTEER_LIST_NAME), false);
////		} catch (IOException e) {}
////		System.out.println("Geocoder: GeoDataProvider created\n - cache dir: " + geoDataCache.getAbsolutePath() + "\n - local gazetteers: " + geoDataPaths.concatStrings(", "));
////		this.geoDataProvider = new FallingRainOnlineProvider(geoDataCache.getAbsolutePath(), geoDataPaths.toStringArray());
//		
//		this.geoDataProvider = new GeoNamesGeoDataProvider();
//		
////		//	read lists of other relavant Annotation types
////		this.excludableAnnotationTypes.parseAndAddElements(this.getParameter(EXCLUDABLE_ANNOTATION_TYPES_SETTING_NAME, ""), ";");
////		this.potentialLocationAnnotationTypes.parseAndAddElements(this.getParameter(POTENTIAL_LOCATION_ANNOTATION_TYPES_SETTING_NAME, ""), ";");
//		
//		System.out.println("Geocoder: Initialized");
//	}
//	
////	/** @see de.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
////	 */
////	public void exitAnalyzer() {
////		this.storeParameter(EXCLUDABLE_ANNOTATION_TYPES_SETTING_NAME, this.excludableAnnotationTypes.concatStrings(";"));
////		this.storeParameter(POTENTIAL_LOCATION_ANNOTATION_TYPES_SETTING_NAME, this.potentialLocationAnnotationTypes.concatStrings(";"));
////	}
////	
//	/**
//	 * Comparator for locations, based on their proximity to some reference point
//	 * 
//	 * @author sautter
//	 */
//	private class LocationCoordinateComparator implements Comparator {
//		
//		private Location reference;
//		
//		private float refLong;
//		private float refLat;
//		
//		/**	Constructor
//		 * @param	reference	the GeoContainer the proximity to which is the reference for comparison
//		 */
//		public LocationCoordinateComparator(Location reference) {
//			this.reference = reference;
//			this.refLong = this.reference.longDeg;
//			this.refLat = this.reference.latDeg;
//		}
//		
//		/** @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
//		 */
//		public int compare(Object o1, Object o2) {
//			if ((o1 instanceof Location) && (o2 instanceof Location)) return this.compare(((Location) o1), ((Location) o2));
//			return 0;
//		}
//		
//		/** @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
//		 */
//		public int compare(Location loc1, Location loc2) {
//			if ((loc1 == null) && (loc2 == null)) return 0;
//			if (loc2 == null) return -1;
//			if (loc1 == null) return 1;
//			
//			float distLong;
//			float distLat;
//			
//			distLong = this.refLong - loc1.longDeg;
//			if (this.refLong == Location.UNKNOWN_LONGITUDE) distLong = 0;	//	longitude not specified in reference
//			distLat = this.refLat - loc1.latDeg;
//			if (this.refLat == Location.UNKNOWN_LATITUDE) distLat = 0;	//	latitude not specified in reference
//			float dist1 = (((distLong * distLong) + (distLat * distLat)));
//			
//			distLong = this.refLong - loc2.longDeg;
//			if (this.refLong == Location.UNKNOWN_LONGITUDE) distLong = 0;	//	longitude not specified in reference
//			distLat = this.refLat - loc2.latDeg;
//			if (this.refLat == Location.UNKNOWN_LATITUDE) distLat = 0;	//	latitude not specified in reference
//			float dist2 = ((distLong * distLong) + (distLat * distLat));
//			
//			return ((dist1 == dist2) ? 0 : ((dist1 < dist2) ? -1 : 1));
//		}
//	}
////	
////	/**
////	 * Comparator for locations, based on type, size, etc.
////	 * 
////	 * @author sautter
////	 */
////	private class LocationImportanceComparator implements Comparator {
////		
////		/** @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
////		 */
////		public int compare(Object o1, Object o2) {
////			if ((o1 instanceof Location) && (o2 instanceof Location)) return this.compare(((Location) o1), ((Location) o2));
////			return 0;
////		}
////		
////		public int compare(Location loc1, Location loc2) {
////			if ((loc1 == null) && (loc2 == null)) return 0;
////			if (loc2 == null) return -1;
////			if (loc1 == null) return 1;
////			
////			if ("city".equalsIgnoreCase(loc1.type) && "city".equalsIgnoreCase(loc2.type))
////				return (loc2.population - loc1.population);
////			else if ("city".equalsIgnoreCase(loc1.type))
////				return -1;
////			else if ("city".equalsIgnoreCase(loc2.type))
////				return 1;
////			else return (loc2.population - loc1.population);
////		}
////	}
//}
