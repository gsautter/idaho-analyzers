/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.locations.GeoUtils;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.plugins.quantities.QuantityUtils;

/**
 * @author sautter
 */
public class MaterialsCitationGeoReferencer extends AbstractAnalyzer implements MaterialsCitationConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get materials citations
		MutableAnnotation[] materialsCitations = data.getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
		if (materialsCitations.length == 0)
			return;
		
		//	process materials citations
		for (int m = 0; m < materialsCitations.length; m++)
			this.geoReferenceMaterialsCitation(materialsCitations[m]);
	}
	
	private void geoReferenceMaterialsCitation(MutableAnnotation materialsCitation) {
//		System.out.println("Geo-referencing " + materialsCitation.toXML());
		
		//	this one already has coordinates
		if (materialsCitation.hasAttribute(LONGITUDE_ATTRIBUTE) && materialsCitation.hasAttribute(LATITUDE_ATTRIBUTE)) {
//			System.out.println(" ==> already geo-referenced");
			return;
		}
		
		//	get locations
		Annotation[] locations = materialsCitation.getAnnotations(LOCATION_TYPE);
		if (locations.length == 0) {
//			System.out.println(" ==> no location marked");
			return;
		}
		
		//	find geo-referenced location
		Annotation location = null;
		Point2D locationPoint = null;
		for (int l = 0; l < locations.length; l++) {
			Point2D lp = GeoUtils.getPoint(locations[l]);
			if (lp != null) {
				location = locations[l];
				locationPoint = lp;
				break;
			}
		}
		if ((location == null) || (locationPoint == null))
			return;
//		System.out.println(" - got location: " + location.toXML());
		
		//	check if location was geo-referenced ... if not, we're done
		if (!location.hasAttribute(GEO_COORDINATE_SOURCE_ATTRIBUTE)) {
			GeoUtils.setPoint(materialsCitation, locationPoint);
//			System.out.println(" ==> coordinates from text");
			return;
		}
		
		//	find deviation associated with selected location, i.e., immediately preceding it
		QueriableAnnotation[] locationDeviations = materialsCitation.getAnnotations(LOCATION_DEVIATION_ANNOTATION_TYPE);
		QueriableAnnotation locationDeviation = null;
		for (int d = 0; d < locationDeviations.length; d++)
			if ((locationDeviations[d].getEndIndex() <= location.getStartIndex()) && ((location.getStartIndex() - locationDeviations[d].getEndIndex()) < 2)) {
				locationDeviation = locationDeviations[d];
				break;
			}
		
		//	translate location coordinates by associated deviation
		if (locationDeviation != null) {
//			System.out.println(" - got location deviation: " + locationDeviation.toXML());
			locationPoint = this.translate(locationPoint, locationDeviation);
		}
		
		//	pull geo-coordinates from locations up to enclosing materials citations
//		System.out.println(" ==> coordinates added");
		GeoUtils.setPoint(materialsCitation, locationPoint);
	}
	
	private Point2D translate(Point2D locationPoint, QueriableAnnotation locationDeviation) {
		
		//	get distance, trying to compute it if missing
		String distanceStr = ((String) locationDeviation.getAttribute(DISTANCE_ATTRIBUTE));
		if (distanceStr == null) {
			Annotation[] quantities = locationDeviation.getAnnotations(QUANTITY_TYPE);
			for (int q = 0; q < quantities.length; q++)
				if ("m".equalsIgnoreCase((String) quantities[q].getAttribute(METRIC_UNIT_ATTRIBUTE)) && ("345".indexOf((String) quantities[q].getAttribute(METRIC_MAGNITUDE_ATTRIBUTE, "0")) != -1)) {
					distanceStr = ("" + QuantityUtils.getMetricValue(quantities[q]));
					break;
				}
		}
		if (distanceStr == null)
			return locationPoint;
//		System.out.println("Got distance: " + distanceStr);
		
		//	get bearing, trying to compute it if missing
		String bearingStr = ((String) locationDeviation.getAttribute(BEARING_ATTRIBUTE));
		if (bearingStr == null)
			for (int t = 1; t < locationDeviation.size(); t++) {
				if (Gamta.isNumber(locationDeviation.valueAt(t-1)) && ("°o".indexOf(locationDeviation.valueAt(t)) != -1)) {
					bearingStr = locationDeviation.valueAt(t-1);
					break;
				}
				if (GeoUtils.getBearing(locationDeviation.valueAt(t)) != -1) {
					bearingStr = ("" + GeoUtils.getBearing(locationDeviation.valueAt(t)));
					break;
				}
			}
		if (bearingStr == null)
			return locationPoint;
//		System.out.println("Got bearing: " + bearingStr);
		
		//	translate given point
		try {
			double distance = Double.parseDouble(distanceStr);
			double bearing = Double.parseDouble(bearingStr);
			locationPoint = GeoUtils.translate(locationPoint, ((int) distance), bearing);
		} catch (NumberFormatException nfe) {}
		
		//	finally
		return locationPoint;
	}
	
	public static void main(String[] args) throws Exception {
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new BufferedReader(new InputStreamReader((new URL("http://plazi.cs.umb.edu/GgServer/xslt/724471D7C6FB8212801A5FA989872636")).openStream(), "UTF-8")));
		MaterialsCitationGeoReferencer mcgr = new MaterialsCitationGeoReferencer();
		mcgr.process(doc, null);
	}
}