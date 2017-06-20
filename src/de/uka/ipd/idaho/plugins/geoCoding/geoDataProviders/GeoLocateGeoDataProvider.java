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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider;
import de.uka.ipd.idaho.plugins.geoCoding.Location;

/**
 * GeoDataProvider based on the GeoLocate web service.
 * 
 * @author sautter
 */
public class GeoLocateGeoDataProvider extends GeoDataProvider {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "GeoLocate";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#isLocalDataProvider()
	 */
	protected boolean isLocalDataProvider() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#supportsGlobalSearch()
	 */
	protected boolean supportsGlobalSearch() {
		return false;
	}
	
	private static Grammar grammar = new StandardGrammar();
	private static Parser parser = new Parser(grammar);
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#loadLocationData(java.lang.String, java.lang.String, java.lang.String)
	 */
	public Location[] loadLocationData(final String location, final String country, final String region) {
		final LinkedList locList = new LinkedList();
		try {
			
			//	build query URL
			URL locDataUrl = new URL("http://www.museum.tulane.edu/webservices/geolocatesvcv2/geolocatesvc.asmx/Georef2" +
					"?Country=" + ((country == null) ? "" : URLEncoder.encode(country, "UTF-8")) +
					"&State=" + ((region == null) ? "" : URLEncoder.encode(region, "UTF-8")) +
					"&County=" +
					"&LocalityString=" + URLEncoder.encode(location, "UTF-8") +
					"&HwyX=false" +
					"&FindWaterbody=false" +
					"&RestrictToLowestAdm=false" +
					"&doUncert=true" +
					"&doPoly=false" +
					"&displacePoly=false" +
					"&polyAsLinkID=false" +
					"&LanguageKey=0");
			
			//	connect to backing source
			Reader locDataReader = new BufferedReader(new InputStreamReader(locDataUrl.openStream(), "UTF-8"));
			
			//	read data
			parser.stream(locDataReader, new TokenReceiver() {
				private Properties locAttributes = new Properties();
				private String locAttributeName = null;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (grammar.isTag(token)) {
						String type = grammar.getType(token);
						if (grammar.isEndTag(token)) {
							
							//	end of location data, store location and clear attribute cache
							if ("ResultSet".equals(type)) {
								this.storeLocation();
								this.locAttributes.clear();
							}
							
							//	end of attribute value
							else this.locAttributeName = null;
						}
						
						//	get location attribute corresponding to current element
						else this.locAttributeName = locAttributeMappings.getProperty(type);
					}
					else if (this.locAttributeName != null)
						this.locAttributes.setProperty(this.locAttributeName, grammar.unescape(token.trim()));
				}
				private void storeLocation() {
					
					//	parse numeric attributes
					String longStr = this.locAttributes.getProperty(LONGITUDE_ATTRIBUTE);
					if (longStr == null)
						return;
					String latStr = this.locAttributes.getProperty(LATITUDE_ATTRIBUTE);
					if (latStr == null)
						return;
					String longLatPrecStr = this.locAttributes.getProperty(LONG_LAT_PRECISION_ATTRIBUTE);
					if (longLatPrecStr == null)
						return;
					float longitude;
					float latitude;
					int longLatPrecision;
					try {
						longitude = Float.parseFloat(longStr);
						latitude = Float.parseFloat(latStr);
						longLatPrecision = Integer.parseInt(longLatPrecStr);
					} catch (NumberFormatException nfe) { return; }
					
					//	create & store location object
					Location loc = new Location(location, longitude, latitude, longLatPrecision);
					locList.add(loc);
					
					//	add country
					if (country != null)
						loc.attributes.setProperty(COUNTRY_ATTRIBUTE, country);
					
					//	add region if given
					if (region != null)
						loc.attributes.setProperty(STATE_PROVINCE_ATTRIBUTE, region);
					
					//	extract region otherwise
					else {
						String locData = this.locAttributes.getProperty("data");
						if ((locData != null) && (locData.indexOf("|:Adm=") != -1)) {
							String stateProvince = locData.substring(locData.indexOf("|:Adm=") + "|:Adm=".length()).trim();
							if (stateProvince.indexOf('|') != -1)
								stateProvince = stateProvince.substring(0, stateProvince.indexOf('|')).trim();
							stateProvince = this.normalizeCase(stateProvince);
							stateProvince = normalizeRegion(country, stateProvince);
							loc.attributes.setProperty(STATE_PROVINCE_ATTRIBUTE, stateProvince);
						}
					}
				}
				private String normalizeCase(String str) {
					StringBuffer ncStr = new StringBuffer();
					char lCh = ((char) 0);
					for (int c = 0; c < str.length(); c++) {
						char ch = str.charAt(c);
						if ((lCh < 33) || ("-'&/".indexOf(lCh) != -1))
							ncStr.append(Character.toUpperCase(ch));
						else ncStr.append(Character.toLowerCase(ch));
						lCh = ch;
					}
					return ncStr.toString();
				}
				public void close() throws IOException {}
			});
		}
		catch (IOException ioe) {
			System.out.println("GeoLocate GeoDataProvider: exception getting data for location '" + location + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		//	finally ...
		return ((Location[]) locList.toArray(new Location[locList.size()]));
	}
	
	private static Properties locAttributeMappings = new Properties();
	static {
		locAttributeMappings.setProperty("Latitude", LATITUDE_ATTRIBUTE);
		locAttributeMappings.setProperty("Longitude", LONGITUDE_ATTRIBUTE);
		locAttributeMappings.setProperty("UncertaintyRadiusMeters", LONG_LAT_PRECISION_ATTRIBUTE);
		locAttributeMappings.setProperty("Debug", "data");
	}
	
	/*
<Georef_Result_Set xmlns="http://www.museum.tulane.edu/webservices/">
  <EngineVersion>string</EngineVersion>
  <NumResults>int</NumResults>
  <ExecutionTimems>double</ExecutionTimems>
  <ResultSet>
    <WGS84Coordinate>
      <Latitude>double</Latitude>
      <Longitude>double</Longitude>
    </WGS84Coordinate>
    <ParsePattern>string</ParsePattern>
    <Precision>string</Precision>
    <Score>int</Score>
    <UncertaintyRadiusMeters>string</UncertaintyRadiusMeters>
    <UncertaintyPolygon>string</UncertaintyPolygon>
    <ReferenceLocation>string</ReferenceLocation>
    <DisplacedDistanceMiles>double</DisplacedDistanceMiles>
    <DisplacedHeadingDegrees>double</DisplacedHeadingDegrees>
    <Debug>string</Debug>
  </ResultSet>
</Georef_Result_Set>
	 */
	
	public static void main(String[] args) throws Exception {
		GeoLocateGeoDataProvider glgdp = new GeoLocateGeoDataProvider();
		
		File basePath = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/GeoLocateData/");
		basePath.mkdirs();
		glgdp.setDataProvider(new AnalyzerDataProviderFileBased(basePath));
		System.out.println("GeoLocateGeoDataProvider initialized");
		
		Location[] locations = glgdp.getLocations("Karlsruhe", "Deutschland", null);
		for (int l = 0; l < locations.length; l++)
			System.out.println(locations[l].toString());
	}
}