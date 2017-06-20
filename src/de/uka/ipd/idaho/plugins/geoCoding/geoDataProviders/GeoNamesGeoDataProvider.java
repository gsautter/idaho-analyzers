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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider;
import de.uka.ipd.idaho.plugins.geoCoding.Location;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * GeoDataProvider based on the geonames.org web service.
 * 
 * @author sautter
 */
public class GeoNamesGeoDataProvider extends GeoDataProvider {
	private static final boolean DEBUG = false;
	
	private static String baseUrl = "http://www.geonames.org/advanced-search.html?country=&featureClass=P&continentCode=&q="; // have to move to page scraping, as web service requires user name and has quota limit
	
	private static GPath resPath = new GPath("//table[./@class = 'restable']/tr[./span[./@class = 'geo']]");
	
	private static GPathExpression namePath = GPathParser.parseExpression("string(./td[2]/a)");
	
	private static GPathExpression countryPath = GPathParser.parseExpression("string(./td[3]/a)");
	private static GPathExpression regionPath = GPathParser.parseExpression("normalize-space(substring-before(substring-after(substring-after(string(./td[3]), string(./td[3]/a)), ','), string(./td[3]/small)))");
	
	private static GPathExpression longPath = GPathParser.parseExpression("number(./td[2]/span[./@class = 'longitude'])");
	private static GPathExpression latPath = GPathParser.parseExpression("number(./td[2]/span[./@class = 'latitude'])");
	
	private static Grammar html = new Html();
	private static Parser parser = new Parser(html);
	
	private static StringVector paragraphTags = new StringVector();
	static { paragraphTags.addElement("br"); }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "GeoNames";
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
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#loadLocationData(java.lang.String, java.lang.String, java.lang.String)
	 */
	public Location[] loadLocationData(String location, String nullCountry, String nullRegion) {
		try {
			if (DEBUG) System.out.println("  doing GeoNames lookup for '" + location + "' at '" + baseUrl + URLEncoder.encode(location, "UTF-8") + "'");
			URL dataUrl = this.dataProvider.getURL(baseUrl + URLEncoder.encode(location, "UTF-8"));
			HttpURLConnection dataCon = ((HttpURLConnection) dataUrl.openConnection());
			dataCon.setDoOutput(true);
			dataCon.setDoInput(true);
			dataCon.setRequestProperty("User-Agent", "GoldenGATE");
			SgmlDocumentReader dataDocReader = new SgmlDocumentReader(null, html, null, null, paragraphTags, 0);
			parser.stream(new InputStreamReader(dataCon.getInputStream(), "UTF-8"), dataDocReader);
			dataDocReader.close();
			QueriableAnnotation dataDoc = dataDocReader.getDocument();
			
			QueriableAnnotation[] resRows = resPath.evaluate(dataDoc, null);
			ArrayList locations = new ArrayList();
			for (int r = 0; r < resRows.length; r++) {
				
				String name = GPath.evaluateExpression(namePath, resRows[r], null).asString().value;
				if (!name.equals(location))
					continue;
				
				String country = GPath.evaluateExpression(countryPath, resRows[r], null).asString().value;
				String region = GPath.evaluateExpression(regionPath, resRows[r], null).asString().value;
				
				String longitude = GPath.evaluateExpression(longPath, resRows[r], null).asString().value;
				String latitude = GPath.evaluateExpression(latPath, resRows[r], null).asString().value;
				
				Location loc = new Location(IoTools.prepareForPlainText(name), longitude, latitude, ("" + Location.UNKNOWN_LONG_LAT_PRECISION));
				if (country.length() != 0)
					loc.attributes.setProperty(COUNTRY_ATTRIBUTE, IoTools.prepareForPlainText(country));
				if (region.length() != 0)
					loc.attributes.setProperty(STATE_PROVINCE_ATTRIBUTE, IoTools.prepareForPlainText(region));
				locations.add(loc);
			}
			
			if (DEBUG) System.out.println("  found " + locations.size() + " data sets");
			return ((Location[]) locations.toArray(new Location[locations.size()]));
		}
		catch (Exception e) {
			System.out.println("GeoNamesGeoDataProvider: " + e.getClass().getName() + " (" + e.getMessage() + ") while getting location data for '" + location + "'");
			e.printStackTrace(System.out);
			return null;
		}
	}
	
	public static void main(String[] args) throws Exception {
		GeoNamesGeoDataProvider gngdp = new GeoNamesGeoDataProvider();
		
		File basePath = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/GeoNamesData/");
		basePath.mkdirs();
		gngdp.setDataProvider(new AnalyzerDataProviderFileBased(basePath));
		
		System.out.println("GeoNamesGeoDataProvider initialized");
		
//		Location[] locations = frgdp.getLocations("Karl", false);
		Location[] locations = gngdp.getLocations("Karlsruhe", null, null, false);
		for (int l = 0; l < locations.length; l++)
			System.out.println(locations[l].toString());
	}
}
