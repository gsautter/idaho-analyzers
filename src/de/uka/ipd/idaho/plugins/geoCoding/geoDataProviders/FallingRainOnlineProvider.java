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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider;
import de.uka.ipd.idaho.plugins.geoCoding.Location;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * GeoDataProvider for the FallingRain geo thesaurus.
 * 
 * @author sautter
 */
public class FallingRainOnlineProvider extends GeoDataProvider {
	
	private static final boolean DEBUG = false;
	
	private static Parser parser = new Parser(new Html());
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.geoCoding.GeoDataProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "FallingRain";
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
	public Location[] loadLocationData(String location, String country, String region) {
		String normalizedLocationName = normalizeName(location);
		Location[] locationData = null;		
		if (DEBUG) System.out.println("  doing FallingRain lookup for '" + location + "'");
		
		LinkedList urls = new LinkedList();
		urls.addLast("http://www.fallingrain.com/world/a/");
		
		int i = 0;
		
		while ((i <= location.length()) && (locationData == null) && (urls.size() != 0)) {
			LinkedHashSet nextLevelUrls = new LinkedHashSet();
			
			String nextPart = ((i < location.length()) ? ("" + location.charAt(i)) : null);
			i++;
			boolean nextPartIsLetter = ((nextPart != null) && nextPart.matches("[a-zA-Z]"));
			
			//	process all URLs of current level
			while ((urls.size() != 0) && (locationData == null)) {
				
				//	get next URL
				String url = ((String) urls.removeFirst());
				if (DEBUG) System.out.println("  loading page '" + url + "'");
				
				try {
					
					//	get page and extract data
					TreeNode pageRoot = parser.parse(new InputStreamReader(this.dataProvider.getURL(url).openStream(), "UTF-8"));
					Location[][] pageData = this.extractData(pageRoot);
					
					//	search location in current page, and cache other locations
					for (int ln = 0; ln < pageData.length; ln++) {
						if (normalizedLocationName.equalsIgnoreCase(normalizeName(pageData[ln][0].name))) {
							locationData = pageData[ln];
							if (DEBUG) System.out.println("  found data for '" + pageData[ln][0].name + "'");
						}
						else {
							this.cacheLocationData(normalizeName(pageData[ln][0].name), pageData[ln]);
							if (DEBUG) System.out.println("  found side data for '" + normalizeName(pageData[ln][0].name) + "'");
						}
					}
					
					//	location not found in current page, follow links
					if ((nextPart != null) && (locationData == null)) {
						
						//	pick appropriate links
						String[] links = this.extractLinks(pageRoot);
						for (int l = 0; l < links.length; l++) {
							String link = links[l];
							
							//	letter
							if (nextPartIsLetter) {
								if (link.endsWith(nextPart.toLowerCase())) {
									nextLevelUrls.add("http://www.fallingrain.com" + link);
									if (DEBUG) System.out.println("  found link: '" + link + "'");
								}
								else if (link.endsWith(nextPart.toUpperCase())) {
									nextLevelUrls.add("http://www.fallingrain.com" + link);
									if (DEBUG) System.out.println("  found link: '" + link + "'");
								}
//								else if (DEBUG) System.out.println("  ignored link: '" + link + "'");
							}
							
							//	not a letter
							else {
								String lastLinkPart = (link.endsWith("/") ? link.substring(0, (link.length() - 1)) : link);
								lastLinkPart = lastLinkPart.substring(lastLinkPart.lastIndexOf("/") + 1);
								if (!lastLinkPart.matches("[a-zA-Z]")) {
									nextLevelUrls.add("http://www.fallingrain.com" + link);
									if (DEBUG) System.out.println("  found link: '" + link + "'");
								}
//								else if (DEBUG) System.out.println("  ignored link: '" + link + "'");
							}
						}
					}
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			//	process collected URLs
			urls.addAll(nextLevelUrls);
		}
		
		//	return location data
		if (DEBUG) System.out.println("  found " + ((locationData == null) ? 0 : locationData.length) + " data sets");
		return locationData;
	}
	
	private static final String[] COLUMN_NAMES = {
		NAME_ATTRIBUTE, 
		LOCATION_TYPE_ATTRIBUTE,
		STATE_PROVINCE_ATTRIBUTE,
		COUNTRY_ATTRIBUTE,
		LATITUDE_ATTRIBUTE,
		LONGITUDE_ATTRIBUTE,
		ELEVATION_ATTRIBUTE,
		POPULATION_ATTRIBUTE,
	};
	private static final double FOOT_TO_METER_FACTOR = 0.30480;
	
	/*
	 Name	What	Region	Country	Lat	Long	Elev Ft.	Pop Est
	*/
	
	/**	extract the geo data from the data table in the HTML page under the specified root node
	 * @param	pageRoot	the root node of the HTML page to extract the data from
	 * @return an array of GeoContainers representing the locations extracted from the specified HTML page
	 */
	private Location[][] extractData(TreeNode pageRoot) {
		
		ArrayList pageLocations = new ArrayList();
		
		//	extract location data from page
		try {
			TreeNode[] tableNodes = TreeTools.getAllNodesOfType(pageRoot, "table");
			for (int t = 0; t < tableNodes.length; t++) {
				
				TreeNode[] subTableNodes = TreeTools.getAllNodesOfType(tableNodes[t], "table");
				if ((subTableNodes.length == 0) || ((subTableNodes.length == 1) && (subTableNodes[0] == tableNodes[t]))) {
					
					TreeNode[] tableHeaderNodes = TreeTools.getAllNodesOfType(tableNodes[t], "th");
					if (tableHeaderNodes.length != 0) {
						
						TreeNode[] tableRowNodes = TreeTools.getAllNodesOfType(tableNodes[t], "tr");
						for (int r = 0; r < tableRowNodes.length; r++) {
							
							tableHeaderNodes = TreeTools.getAllNodesOfType(tableRowNodes[r], "th");
							if (tableHeaderNodes.length == 0) {
								
								TreeNode[] tableDataNodes = TreeTools.getAllNodesOfType(tableRowNodes[r], "td");
								
								String[] rowData = new String[COLUMN_NAMES.length];
								
								for (int d = 0; d < tableDataNodes.length; d++) {
									TreeNode[] dataNodes = TreeTools.getAllNodesOfType(tableDataNodes[d], TreeNode.DATA_NODE_TYPE);
									rowData[d] = ((dataNodes.length == 0) ? null : IoTools.prepareForPlainText(dataNodes[0].getNodeValue(), IoTools.HTML_CHAR_NORMALIZATION));
								}
								
								if (rowData[0] != null) {
									String elevation = rowData[6];
									if (elevation != null) try {
										int elev = Integer.parseInt(elevation);
										elev = ((int) (elev * FOOT_TO_METER_FACTOR));
										elevation = ("" + elev);
									}
									catch (Exception e) {
										e.printStackTrace();
									}
									Location loc = new Location(rowData[0], rowData[5], rowData[4], ("" + Location.UNKNOWN_LONG_LAT_PRECISION), elevation, rowData[7]);
									if (rowData[3] != null)
										loc.attributes.setProperty(COUNTRY_ATTRIBUTE, rowData[3]);
									if (rowData[2] != null)
										loc.attributes.setProperty(STATE_PROVINCE_ATTRIBUTE, rowData[2]);
									pageLocations.add(loc);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println(e.getClass().getName() + "(" + e.getMessage() + ") while extracting data from page.");
		}
		
		//	group locations by name
		Collections.sort(pageLocations, new Comparator() {
			public int compare(Object o1, Object o2) {
				Location l1 = ((Location) o1);
				Location l2 = ((Location) o2);
				return normalizeName(l1.name).compareTo(normalizeName(l2.name));
			}
		});
		HashMap locationListsByName = new LinkedHashMap();
		for (int l = 0; l < pageLocations.size(); l++) {
			Location location = ((Location) pageLocations.get(l));
			String normalizedLocationName = normalizeName(location.name);
			ArrayList namedLocationList = ((ArrayList) locationListsByName.get(normalizedLocationName));
			if (namedLocationList == null) {
				namedLocationList = new ArrayList();
				locationListsByName.put(normalizedLocationName, namedLocationList);
			}
			namedLocationList.add(location);
		}
		ArrayList locations = new ArrayList();
		for (Iterator lit = locationListsByName.values().iterator(); lit.hasNext();) {
			ArrayList namedLocationList = ((ArrayList) lit.next());
			locations.add((Location[]) namedLocationList.toArray(new Location[namedLocationList.size()]));
		}
		return ((Location[][]) locations.toArray(new Location[locations.size()][]));
	}
	
	/**	extract the links to lower level pages from the HTML page under the specified root node
	 * @param	pageRoot	the root node of the HTML page to extract the links from
	 * @return an array of Strings representing the URLs extracted from the links in the specified HTML page
	 */
	private String[] extractLinks(TreeNode pageRoot) {
		StringVector links = new StringVector();
		TreeNode[] linkNodes = TreeTools.getAllNodesOfType(pageRoot, "a");
		for (int l = 0; l < linkNodes.length; l++) {
			String href = linkNodes[l].getAttribute("href");
			if ((href != null) && href.startsWith("/world/a/") && (href.indexOf("#") == -1) && !href.endsWith("/"))
				links.addElementIgnoreDuplicates(href);
		}
		return links.toStringArray();
	}
	
	private static final String normalizeName(String name) {
		StringBuffer normalizedName = new StringBuffer();
		for (int c = 0; c < name.length(); c++) {
			char ch = name.charAt(c);
			if (('a' <= ch) && (ch <= 'z'))
				normalizedName.append(ch);
			else if (('A' <= ch) && (ch <= 'Z'))
				normalizedName.append((char) (ch - ('A' - 'a')));
			else if (('0' <= ch) && (ch <= '9'))
				normalizedName.append(ch);
			else normalizedName.append('_');
		}
		return normalizedName.toString();
	}
	
	public static void main(String[] args) throws Exception {
		
		
		FallingRainOnlineProvider frgdp = new FallingRainOnlineProvider();
		
		File basePath = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData/FallingRainData/");
		basePath.mkdirs();
		frgdp.setDataProvider(new AnalyzerDataProviderFileBased(basePath));
		
		System.out.println("FallingRainOnlineProvider initialized");
		
//		Location[] locations = frgdp.getLocations("Karlsruhe", false);
//		Location[] locations = frgdp.getLocations("Karlsruhe");
		Location[] locations = frgdp.getLocations("Port of Spain", false);
		for (int l = 0; l < locations.length; l++)
			System.out.println(locations[l].toString());
	}
}
