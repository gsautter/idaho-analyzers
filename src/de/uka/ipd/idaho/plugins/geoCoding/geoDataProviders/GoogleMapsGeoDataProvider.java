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

package de.uka.ipd.idaho.plugins.geoCoding.geoDataProviders;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.XPath;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNodeSet;
import de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider;
import de.uka.ipd.idaho.plugins.geoCoding.Location;
import de.uka.ipd.idaho.plugins.locations.GeoUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * GeoDataProvider based on the GoogleMaps web service.
 * 
 * @author sautter
 */
public class GoogleMapsGeoDataProvider extends GeoDataProvider {
	
	private static final boolean DEBUG = false;
	
	private static final String baseUrl = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=false&address=";
	
	private static final XPath resultPath = new XPath("/GeocodeResponse/result");
	
	private static final int defaultPointPrecision = 100;
	private int pointPrecision = defaultPointPrecision;
	
/*
# colloquial_area indicates a commonly-used alternative name for the entity.
# sublocality indicates an first-order civil entity below a locality
# neighborhood indicates a named neighborhood
# premise indicates a named location, usually a building or collection of buildings with a common name
# subpremise indicates a first-order entity below a named location, usually a singular building within a collection of buildings with a common name
# postal_code indicates a postal code as used to address postal mail within the country.
# natural_feature indicates a prominent natural feature.
# airport indicates an airport.
# park indicates a named park.
# point_of_interest indicates a named point of interest. Typically, these "POI"s are prominent local entities that don't easily fit in another category such as "Empire State Building" or "Statue of Liberty." * 
# establishment (unlisted in documentation) indicates some zoo, garden, etc.
 */	
	
	
	private static final XPath sublocalityPath = new XPath("/address_component[" +
			"./type='establishment' or " +
			"./type='colloquial_area' or " +
			"./type='sublocality' or " +
			"./type='neighborhood' or " +
			"./type='premise' or " +
			"./type='subpremise' or " +
			"./type='natural_feature' or " +
			"./type='airport' or " +
			"./type='park' or " +
			"./type='point_of_interest'" +
			"]/long_name");
	private static final XPath localityPath = new XPath("/address_component[./type = 'locality']/long_name");
	
	private static final XPath countryPath = new XPath("/address_component[./type = 'country']/long_name");
	private static final XPath regionPath = new XPath("/address_component[./type = 'administrative_area_level_1']/long_name");
	private static final XPath countyPath = new XPath("/address_component[./type = 'administrative_area_level_2']/long_name");
	
	private static final XPath longPath = new XPath("/geometry/location/lng");
	private static final XPath latPath = new XPath("/geometry/location/lat");
	
	private static final XPath boundsSwLongPath = new XPath("/geometry/bounds/southwest/lng");
	private static final XPath boundsSwLatPath = new XPath("/geometry/bounds/southwest/lat");
	private static final XPath boundsNeLongPath = new XPath("/geometry/bounds/northeast/lng");
	private static final XPath boundsNeLatPath = new XPath("/geometry/bounds/northeast/lat");
	
	private static final Parser parser = new Parser();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "GoogleMaps";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#isLocalDataProvider()
	 */
	protected boolean isLocalDataProvider() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#init()
	 */
	protected void init() {
		Properties parameters = new Properties();
		
		//	load lines
		StringVector rawLines = null;
		try {
			Reader r = new InputStreamReader(this.dataProvider.getInputStream("config.cnfg"), "UTF-8");
			rawLines = StringVector.loadList(r);
			r.close();
		}
		catch (IOException ioe) {
			return;
		}
		
		//	parse lines
		for (int l = 0; l < rawLines.size(); l++) {
			String rawLine = rawLines.get(l);
			int splitIndex = rawLine.indexOf('|');
			
			//	split line and decode value
			String name;
			String value;
			try {
				name = rawLine.substring(0, splitIndex);
				value = URLDecoder.decode(rawLine.substring(splitIndex + 1), "UTF-8");
			}
			catch (Exception e) {
				continue;
			}
			
			//	store parameter if valid
			parameters.setProperty(name, value);
		}
		
		//	load point precision
		try {
			this.pointPrecision = Integer.parseInt(parameters.getProperty("pointPrecision", ("" + this.pointPrecision)));
		} catch (NumberFormatException nfe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#supportsGlobalSearch()
	 */
	protected boolean supportsGlobalSearch() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#loadLocationData(java.lang.String, java.lang.String, java.lang.String)
	 */
	public Location[] loadLocationData(String location, String nullCountry, String nullRegion) {
		try {
			if (DEBUG) System.out.println("  doing GoogleMaps lookup for '" + location + "' at '" + baseUrl + URLEncoder.encode(location, "UTF-8") + "'");
			TreeNode root = parser.parse(new InputStreamReader(this.dataProvider.getURL(baseUrl + URLEncoder.encode(location, "UTF-8")).openStream(), "UTF-8"));
			
			Properties varBindings = new Properties();
			XPathNodeSet resultNodes = resultPath.evaluate(root, varBindings);
			
			ArrayList locations = new ArrayList();
			for (int r = 0; r < resultNodes.size(); r++) {
				TreeNode resultRoot = resultNodes.get(r);
				
				String sublocality = XPath.evaluatePath(sublocalityPath, resultRoot, varBindings).asString().value;
				String locality = XPath.evaluatePath(localityPath, resultRoot, varBindings).asString().value;
				String name;
				String type = "";
				if (sublocality.length() == 0) {
					name = locality;
					type = "locality";
				}
				else if (locality.length() == 0) {
					name = sublocality;
					type = "sublocality";
				}
				else {
					name = (sublocality + ", " + locality);
					type = "sublocality";
				}
				
				String country = XPath.evaluatePath(countryPath, resultRoot, varBindings).asString().value;
				String region = XPath.evaluatePath(regionPath, resultRoot, varBindings).asString().value;
				String county = XPath.evaluatePath(countyPath, resultRoot, varBindings).asString().value;
				
				String longitude = XPath.evaluatePath(longPath, resultRoot, varBindings).asString().value;
				String latitude = XPath.evaluatePath(latPath, resultRoot, varBindings).asString().value;
				
				String wLongitude = XPath.evaluatePath(boundsSwLongPath, resultRoot, varBindings).asString().value;
				String sLatitude = XPath.evaluatePath(boundsSwLatPath, resultRoot, varBindings).asString().value;
				String eLongitude = XPath.evaluatePath(boundsNeLongPath, resultRoot, varBindings).asString().value;
				String nLatitude = XPath.evaluatePath(boundsNeLatPath, resultRoot, varBindings).asString().value;
				int longLatPrecision = computePrecision(eLongitude, wLongitude, nLatitude, sLatitude);
				
				if (longLatPrecision == Location.UNKNOWN_LONG_LAT_PRECISION)
					longLatPrecision = pointPrecision;
				
				Location loc = new Location(IoTools.prepareForPlainText(name), longitude, latitude, ("" + longLatPrecision), null, null);
				if (type.length() != 0)
					loc.attributes.setProperty(LOCATION_TYPE_ATTRIBUTE, type);
				if (country.length() != 0)
					loc.attributes.setProperty(COUNTRY_ATTRIBUTE, IoTools.prepareForPlainText(country));
				if (region.length() != 0)
					loc.attributes.setProperty(STATE_PROVINCE_ATTRIBUTE, IoTools.prepareForPlainText(region));
				if (county.length() != 0)
					loc.attributes.setProperty(COUNTY_ATTRIBUTE, IoTools.prepareForPlainText(county));
				locations.add(loc);
			}
			
			if (DEBUG) System.out.println("  found " + locations.size() + " data sets");
			return ((Location[]) locations.toArray(new Location[locations.size()]));
		}
		catch (Exception e) {
			System.out.println("GoogleMapsGeoDataProvider: " + e.getClass().getName() + " (" + e.getMessage() + ") while getting location data for '" + location + "'");
			e.printStackTrace(System.out);
			return null;
		}
	}
	
	private int computePrecision(String eLong, String wLong, String nLat, String sLat) {
		try {
			double e = Double.parseDouble(eLong);
			double w = Double.parseDouble(wLong);
			double n = Double.parseDouble(nLat);
			double s = Double.parseDouble(sLat);
			return ((int) (GeoUtils.getDistance(w, s, e, n) / 2));
		}
		catch (NumberFormatException nfe) {
			return Location.UNKNOWN_LONG_LAT_PRECISION;
		}
//		float e;
//		float w;
//		float n;
//		float s;
//		try {
//			e = Float.parseFloat(eLong);
//			w = Float.parseFloat(wLong);
//			n = Float.parseFloat(nLat);
//			s = Float.parseFloat(sLat);
//		}
//		catch (NumberFormatException nfe) {
//			return Location.UNKNOWN_LONG_LAT_PRECISION;
//		}
//		
//		float dDegLong = Math.abs(e - w);
////		if (DEBUG) System.out.println("Longitude distance in degrees: " + dDegLong);
//		float dDegLat = Math.abs(n - s);
////		if (DEBUG) System.out.println("Latitude distance in degrees: " + dDegLat);
//		
//		int dLong = ((int) Math.round((EARTH_CIRCUMFERENCE / 360) * dDegLong * Math.cos((((n + s) / 2) / 90) * (Math.PI / 2))));
////		if (DEBUG) System.out.println("Longitude distance in meters: " + dLong);
//		int dLat = Math.round((EARTH_CIRCUMFERENCE / 360) * dDegLat);
////		if (DEBUG) System.out.println("Latitude distance in meters: " + dLat);
//		
//		int dialonalRadius = ((int) Math.round(Math.sqrt(dLong * dLong + dLat * dLat) / 2));
////		if (DEBUG) System.out.println("Bounding box radius in meters: " + dialonalRadius);
//		return dialonalRadius;
	}
//	
//	private static final int EARTH_CIRCUMFERENCE = 40 * 1000 * 1000;
	
/* 
REQUEST (HTTP GET)
http://maps.googleapis.com/maps/api/geocode/xml?address=Botanical+Garden,+Port+of+Spain

RESPONSE
<GeocodeResponse>
 <status>OK</status>
 <result>
  <type>park</type>
  <type>establishment</type>
  <formatted_address>Botanical Gardens, St Ann's, Trinidad &amp; Tobago</formatted_address>

  <address_component>
   <long_name>Botanical Gardens</long_name>
   <short_name>Botanical Gardens</short_name>
   <type>establishment</type>
  </address_component>
  <address_component>
   <long_name>St Ann's</long_name>

   <short_name>St Ann's</short_name>
   <type>locality</type>
   <type>political</type>
  </address_component>
  <address_component>
   <long_name>St George</long_name>
   <short_name>St George</short_name>

   <type>administrative_area_level_1</type>
   <type>political</type>
  </address_component>
  <address_component>
   <long_name>Trinidad &amp; Tobago</long_name>
   <short_name>TT</short_name>

   <type>country</type>
   <type>political</type>
  </address_component>
  <geometry>
  
   <location>
    <lat>10.6745143</lat>
    <lng>-61.5143336</lng>
   </location>
   
   <location_type>APPROXIMATE</location_type>
   
   <viewport>
    <southwest>
     <lat>10.6635492</lat>
     <lng>-61.5303410</lng>
    </southwest>
    <northeast>
     <lat>10.6854790</lat>
     <lng>-61.4983262</lng>
    </northeast>
   </viewport>
   
   <bounds>
    <southwest>
     <lat>10.6727007</lat>
     <lng>-61.5163513</lng>
    </southwest>
    <northeast>
     <lat>10.6781003</lat>
     <lng>-61.5119543</lng>
    </northeast>
   </bounds>

  </geometry>
 </result>
</GeocodeResponse>
*/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GoogleMapsGeoDataProvider gmgdp = new GoogleMapsGeoDataProvider();
//		String wLongitude = "-61.5163513";
//		String sLatitude = "10.6727007";
//		String eLongitude = "-61.5119543";
//		String nLatitude = "10.6781003";
//		System.out.println(gmgdp.computePrecision(eLongitude, wLongitude, nLatitude, sLatitude));
//		if (true)
//			return;
		
		File basePath = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/GoogleMapsData/");
		basePath.mkdirs();
		gmgdp.setDataProvider(new AnalyzerDataProviderFileBased(basePath));
		
		System.out.println("GoogleMapsGeoDataProvider initialized");
		
//		Location[] locations = frgdp.getLocations("Karl", false);
//		Location[] locations = gmgdp.getLocations("Botanical Garden, Port of Spain");
//		Location[] locations = gmgdp.getLocations("Hasenboseroth, Germany");
//		Location[] locations = gmgdp.getLocations("Am Fasanengarten 5, Karlsruhe");
		Location[] locations = gmgdp.getLocations("Karlsruhe", false);
		for (int l = 0; l < locations.length; l++)
			System.out.println(locations[l].toString());
	}
}