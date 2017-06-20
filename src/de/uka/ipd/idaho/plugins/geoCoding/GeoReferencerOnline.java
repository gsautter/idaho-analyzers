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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;
import de.uka.ipd.idaho.plugins.locations.GeoUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This analyzer fetches geo-coordinates from a variety of external sources and
 * adds it to annotated locations. If a location name is ambiguous, the user
 * gets to pick the actual location.
 * 
 * @author sautter
 */
public class GeoReferencerOnline extends AbstractConfigurableAnalyzer implements LiteratureConstants, LocationConstants, MonitorableAnalyzer {
	
	private GeoDataProvider[] geoDataProviders = null;
	private int geoDataProviderTimeout = 30;
	private int geoDataClusterMinRadius = 10000;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		System.out.println("Geocoder: Initializing ...");
		this.geoDataProviders = GeoDataProvider.getDataProviders(this.dataProvider);
		System.out.println("Geocoder: Initialized");
		
		try {
			this.geoDataProviderTimeout = Integer.parseInt(this.getParameter("geoDataProviderTimeout", ("" + this.geoDataProviderTimeout)));
		} catch (NumberFormatException nfe) {}
		try {
			this.geoDataClusterMinRadius = Integer.parseInt(this.getParameter("geoDataClusterMinRadius", ("" + this.geoDataClusterMinRadius)));
		} catch (NumberFormatException nfe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		this.process(data, parameters, ProgressMonitor.dummy);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
		
		//	prepare progress monitor
		pm.setBaseProgress(0);
		pm.setProgress(0);
		pm.setMaxProgress(5);
		pm.setStep("Lining up locations");
		
		//	add page numbers to locations
		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber == null)
				continue;
			Annotation[] paragraphLocations = paragraphs[p].getAnnotations(LOCATION_TYPE);
			for (int l = 0; l < paragraphLocations.length; l++)
				paragraphLocations[l].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber.toString());
		}
		
		//	get locations
		QueriableAnnotation[] locations = data.getAnnotations(LOCATION_TYPE);
		
		//	bucketize locations by names, countries, and regions
		TreeMap locationSetsByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap locationSetsByNameCountry = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap locationSetsByNameCountryRegion = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		this.bucketizeLocations(locations, locationSetsByName, locationSetsByNameCountry, locationSetsByNameCountryRegion);
		
		//	clean up location sets
		this.cleanupLocationSets(locationSetsByNameCountryRegion);
		this.cleanupLocationSets(locationSetsByNameCountry);
		this.cleanupLocationSets(locationSetsByName);
		pm.setInfo("Locations lined up");
		
		//	transfer attributes already present in document, first with country and region, then with country, and finally with the name alone
		pm.setBaseProgress(5);
		pm.setProgress(0);
		pm.setMaxProgress(10);
		pm.setStep("Transferring attributes already present in document");
		this.transferAttributes(locationSetsByNameCountryRegion);
		this.transferAttributes(locationSetsByNameCountry);
		this.transferAttributes(locationSetsByName);
		pm.setInfo("Attributes transferred");
		
		//	re-bucketize locations to fill up more specific sets after attribute transfer
		this.bucketizeLocations(locations, locationSetsByName, locationSetsByNameCountry, locationSetsByNameCountryRegion);
		pm.setInfo("Transferred attributes evaluated");
		
		//	load geo data for locations sets
		pm.setStep("Fetching geo data");
		TreeSet gotLocationData = new TreeSet(AnnotationUtils.ANNOTATION_NESTING_ORDER);
		pm.setBaseProgress(10);
		pm.setProgress(0);
		pm.setMaxProgress(36);
		this.getLocationData(locationSetsByNameCountryRegion, gotLocationData, pm);
		pm.setBaseProgress(36);
		pm.setProgress(0);
		pm.setMaxProgress(62);
		this.getLocationData(locationSetsByNameCountry, gotLocationData, pm);
		pm.setBaseProgress(62);
		pm.setProgress(0);
		pm.setMaxProgress(89);
		this.getLocationData(locationSetsByName, gotLocationData, pm);
		pm.setInfo("Got geo data");
		
		//	line up location sets for processing
		pm.setStep("Sorting locations and geo data");
		pm.setBaseProgress(89);
		pm.setProgress(0);
		pm.setMaxProgress(92);
		TreeSet locationSets = new TreeSet(new Comparator() {
			public int compare(Object obj1, Object obj2) {
				LocationSet ls1 = ((LocationSet) obj1);
				LocationSet ls2 = ((LocationSet) obj2);
				int c = (ls1.firstStartIndex - ls2.firstStartIndex);
				if (c != 0)
					return c;
				c = this.compareStrings(ls1.name, ls2.name);
				if (c != 0)
					return c;
				c = this.compareStrings(ls1.country, ls2.country);
				if (c != 0)
					return c;
				return this.compareStrings(ls1.region, ls2.region);
			}
			private int compareStrings(String str1, String str2) {
				if ((str1 == null) && (str2 == null))
					return 0;
				else if (str1 == null)
					return -1;
				else if (str2 == null)
					return 1;
				else return str1.compareToIgnoreCase(str2);
			}
		});
		for (Iterator lskit = locationSetsByNameCountryRegion.keySet().iterator(); lskit.hasNext();)
			locationSets.add(locationSetsByNameCountryRegion.get(lskit.next()));
		for (Iterator lskit = locationSetsByNameCountry.keySet().iterator(); lskit.hasNext();)
			locationSets.add(locationSetsByNameCountry.get(lskit.next()));
		for (Iterator lskit = locationSetsByName.keySet().iterator(); lskit.hasNext();)
			locationSets.add(locationSetsByName.get(lskit.next()));
		
		//	sort out location sets whose geo reference is unambiguous (only one geo data set)
		pm.setStep("Handling locations with unambiguous geo data");
		pm.setBaseProgress(92);
		pm.setProgress(0);
		pm.setMaxProgress(95);
		int locNr = 0;
		for (Iterator lsit = locationSets.iterator(); lsit.hasNext();) {
			LocationSet locSet = ((LocationSet) lsit.next());
			if (locSet.geoDataSets.length == 1) {
				for (Iterator lit = locSet.locations.iterator(); lit.hasNext();)
					this.setLocationAttributes(((Annotation) lit.next()), locSet.geoDataSet);
				lsit.remove();
			}
			else locNr++;
			pm.setProgress((locNr * 100) / locationSets.size());
		}
		
		//	process location sets
		pm.setStep("Getting user feedback for locations with ambiguous geo data");
		pm.setBaseProgress(95);
		pm.setProgress(0);
		pm.setMaxProgress(100);
		while (!locationSets.isEmpty()) {
			LocationSet[] locSets = ((LocationSet[]) locationSets.toArray(new LocationSet[locationSets.size()]));
			
			//	get feedback for ambiguous or unknown locations
			boolean continueAfterRound = this.getFeedback(locSets, data);
			
			//	process feedback
			for (int l = 0; l < locSets.length; l++) {
				
				//	remove
				if (locSets[l].geoDataSet == REMOVE) {
					for (Iterator lit = locSets[l].locations.iterator(); lit.hasNext();)
						data.removeAnnotation((Annotation) lit.next());
					locationSets.remove(locSets[l]);
				}
				
				//	ignore location for now
				else if (locSets[l].geoDataSet == IGNORE)
					locationSets.remove(locSets[l]);
				
				//	split location set
				else if (locSets[l].geoDataSet == SPLIT) {
					for (Iterator lit = locSets[l].locations.iterator(); lit.hasNext();) {
						Annotation loc = ((Annotation) lit.next());
						LocationSet splitLocSet = new LocationSet(loc.getStartIndex(), locSets[l].name, locSets[l].country, locSets[l].region);
						splitLocSet.locations.add(loc);
						splitLocSet.geoDataSets = locSets[l].geoDataSets;
						if (splitLocSet.geoDataSets.length != 0)
							splitLocSet.geoDataSet = splitLocSet.geoDataSets[0];
						locationSets.add(splitLocSet);
					}
					locationSets.remove(locSets[l]);
				}
				
				//	ignore location for now
				else if (locSets[l].geoDataSets.length == 0)
					locationSets.remove(locSets[l]);
				
				//	coordinates selected
				else if (locSets[l].geoDataSet != null) {
					for (Iterator lit = locSets[l].locations.iterator(); lit.hasNext();)
						this.setLocationAttributes(((Annotation) lit.next()), locSets[l].geoDataSet);
					locationSets.remove(locSets[l]);
				}
			}
			
			//	cancelled, clear list
			if (!continueAfterRound)
				locationSets.clear();
		}
		
		pm.setStep("Done");
		pm.setBaseProgress(95);
		pm.setProgress(0);
		pm.setMaxProgress(100);
		pm.setProgress(100);
	}

	private void bucketizeLocations(Annotation[] locations, TreeMap locationSetsByName, TreeMap locationSetsByNameCountry, TreeMap locationSetsByNameCountryRegion) {
		for (int l = 0; l < locations.length; l++) {
			String name = ((String) locations[l].getAttribute(NAME_ATTRIBUTE));
			if (name == null) {
				int nameLength = 1;
				for (int t = 1; t < locations[l].size(); t++) {
					if (Gamta.isWord(locations[l].valueAt(t)))
						nameLength ++;
					else t = locations[l].size();
				}
				name = TokenSequenceUtils.concatTokens(locations[l], 0, nameLength, true, true);
				locations[l].setAttribute(NAME_ATTRIBUTE, name);
			}
			
			LocationSet nameSet = ((LocationSet) locationSetsByName.get(name));
			if (nameSet == null) {
				nameSet = new LocationSet(locations[l].getStartIndex(), name);
				locationSetsByName.put(name, nameSet);
			}
			nameSet.locations.add(locations[l]);
			
			String country = ((String) locations[l].getAttribute(COUNTRY_ATTRIBUTE));
			if (country == null)
				continue;
			LocationSet nameCountrySet = ((LocationSet) locationSetsByNameCountry.get(name + " (" + country + ")"));
			if (nameCountrySet == null) {
				nameCountrySet = new LocationSet(locations[l].getStartIndex(), name, country);
				nameSet.addSubSet(nameCountrySet);
				locationSetsByNameCountry.put((name + " (" + country + ")"), nameCountrySet);
			}
			nameCountrySet.locations.add(locations[l]);
			
			String region = ((String) locations[l].getAttribute(STATE_PROVINCE_ATTRIBUTE));
			if (region == null)
				continue;
			LocationSet nameCountryRegionSet = ((LocationSet) locationSetsByNameCountryRegion.get(name + " (" + region + ", " + country + ")"));
			if (nameCountryRegionSet == null) {
				nameCountryRegionSet = new LocationSet(locations[l].getStartIndex(), name, country, region);
				nameCountrySet.addSubSet(nameCountryRegionSet);
				locationSetsByNameCountryRegion.put((name + " (" + region + ", " + country + ")"), nameCountryRegionSet);
			}
			nameCountryRegionSet.locations.add(locations[l]);
		}
	}
	
	private void cleanupLocationSets(Map locationSets) {
		for (Iterator lskit = locationSets.keySet().iterator(); lskit.hasNext();) {
			LocationSet locSet = ((LocationSet) locationSets.get(lskit.next()));
			if (1 < locSet.locations.size())
				continue;
			if (locSet.locations.isEmpty())
				lskit.remove();
			Annotation loc = ((Annotation) locSet.locations.first());
			if (loc.hasAttribute(LONGITUDE_ATTRIBUTE) && loc.hasAttribute(LATITUDE_ATTRIBUTE))
				lskit.remove();
		}
	}
	
	private void transferAttributes(Map locationSets) {
		for (Iterator lskit = locationSets.keySet().iterator(); lskit.hasNext();) {
			LocationSet locSet = ((LocationSet) locationSets.get(lskit.next()));
			
			//	any chance of transferring anything?
			if (locSet.locations.size() < 2)
				continue;
			
			//	transfer attributes (assuming document in reading order, each attribute scopes until overwritten)
			String country = null;
			Set locSetCountries = ((locSet.country == null) ? new HashSet() : null);
			String region = null;
			Set locSetRegions = ((locSet.region == null) ? new HashSet() : null);
			String longitude = null;
			String latitude = null;
			Set locSetCoordinates = new HashSet();
			String longLatPrecision = null;
			String longLatSource = null;
			boolean locationsWithoutCoordinates = false;
			for (Iterator lit = locSet.locations.iterator(); lit.hasNext();) {
				Annotation loc = ((Annotation) lit.next());
				
				//	handle country if not fixed
				if (locSet.country == null) {
					
					//	country given, update it, and void all other attributes on change
					if (loc.hasAttribute(COUNTRY_ATTRIBUTE)) {
						String locCountry = ((String) loc.getAttribute(COUNTRY_ATTRIBUTE));
						if (!locCountry.equalsIgnoreCase(country)) {
							country = locCountry;
							locSetCountries.add(country);
							region = null;
							longitude = null;
							latitude = null;
							longLatPrecision = null;
						}
					}
					
					//	transfer country if any in scope
					else if (country != null)
						loc.setAttribute(COUNTRY_ATTRIBUTE, country);
				}
				
				//	handle region if not fixed
				if (locSet.region == null) {
					
					//	region given, update it, and void all other attributes on change
					if (loc.hasAttribute(STATE_PROVINCE_ATTRIBUTE)) {
						String locRegion = ((String) loc.getAttribute(STATE_PROVINCE_ATTRIBUTE));
						if (!locRegion.equalsIgnoreCase(region)) {
							region = locRegion;
							locSetRegions.add(region);
							longitude = null;
							latitude = null;
							longLatPrecision = null;
						}
					}
					
					//	transfer region if any in scope
					else if (region != null)
						loc.setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
				}
				
				//	change of longitude and latitude
				if (loc.hasAttribute(LONGITUDE_ATTRIBUTE) && loc.hasAttribute(LATITUDE_ATTRIBUTE)) {
					longitude = ((String) loc.getAttribute(LONGITUDE_ATTRIBUTE));
					latitude = ((String) loc.getAttribute(LATITUDE_ATTRIBUTE));
					locSetCoordinates.add(longitude + "/" + latitude);
					longLatPrecision = ((String) loc.getAttribute(LONG_LAT_PRECISION_ATTRIBUTE));
					longLatSource = ((String) loc.getAttribute(GEO_COORDINATE_SOURCE_ATTRIBUTE));
				}
				
				//	transfer longitude and latitude if in any scope
				else if ((longitude != null) && (latitude != null)) {
					loc.setAttribute(LONGITUDE_ATTRIBUTE, longitude);
					loc.setAttribute(LATITUDE_ATTRIBUTE, latitude);
					if ((longLatPrecision != null) && !loc.hasAttribute(LONG_LAT_PRECISION_ATTRIBUTE))
						loc.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, longLatPrecision);
					if ((longLatSource != null) && !loc.hasAttribute(GEO_COORDINATE_SOURCE_ATTRIBUTE))
						loc.setAttribute(GEO_COORDINATE_SOURCE_ATTRIBUTE, longLatSource);
				}
				
				//	remember we have locations without coordinates
				else locationsWithoutCoordinates = true;
			}
			
			//	infer country regardless of scope if unambiguous (overwriting existing values is not an issue in this case)
			if ((locSet.country == null) && (locSetCountries.size() == 1)) {
				for (Iterator lit = locSet.locations.iterator(); lit.hasNext();)
					((Annotation) lit.next()).setAttribute(COUNTRY_ATTRIBUTE, country);
			}
			
			//	infer region regardless of scope if unambiguous (overwriting existing values is not an issue in this case)
			if ((locSet.region == null) && (locSetRegions.size() == 1)) {
				for (Iterator lit = locSet.locations.iterator(); lit.hasNext();)
					((Annotation) lit.next()).setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
			}
			
			//	infer coordinates regardless of scope if unambiguous (overwriting existing values is not an issue in this case)
			if (locationsWithoutCoordinates && (locSetCoordinates.size() == 1)) {
				for (Iterator lit = locSet.locations.iterator(); lit.hasNext();) {
					Annotation loc = ((Annotation) lit.next());
					loc.setAttribute(LONGITUDE_ATTRIBUTE, longitude);
					loc.setAttribute(LATITUDE_ATTRIBUTE, latitude);
					if ((longLatPrecision != null) && !loc.hasAttribute(LONG_LAT_PRECISION_ATTRIBUTE))
						loc.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, longLatPrecision);
					if ((longLatSource != null) && !loc.hasAttribute(GEO_COORDINATE_SOURCE_ATTRIBUTE))
						loc.setAttribute(GEO_COORDINATE_SOURCE_ATTRIBUTE, longLatSource);
				}
				locationsWithoutCoordinates = false;
			}
			
			//	we're done with this bucket
			if (!locationsWithoutCoordinates)
				lskit.remove();
		}
	}
	
	private void getLocationData(Map locationSets, Set gotLocationData, ProgressMonitor pm) {
		int locNr = 0;
		for (Iterator lskit = locationSets.keySet().iterator(); lskit.hasNext();) {
			LocationSet locSet = ((LocationSet) locationSets.get(lskit.next()));
			
			//	anything left to get data for?
			if (gotLocationData.containsAll(locSet.locations)) {
				lskit.remove(); // all locations handled in more specific sets
				continue;
			}
			
			//	try and get location data from connected sources
			pm.setInfo("Getting geo data for '" + locSet.name + ((locSet.country == null) ? "" : (" (" + ((locSet.region == null) ? "" : (locSet.region + ", ")) + locSet.country + ")")) + "'");
			locSet.geoDataSets = this.getGeoData(locSet.name, locSet.country, locSet.region, true);
			pm.setInfo("Got " + locSet.geoDataSets.length + " geo data sets");
			pm.setProgress((locNr++ * 100) / locationSets.size());
			
			//	did we find anything?
			if (locSet.geoDataSets.length != 0) {
				locSet.geoDataSet = locSet.geoDataSets[0];
				gotLocationData.addAll(locSet.locations);
			}
			
			//	no data with country or region specific search, either might be wrong, so we have to try without
			else if ((locSet.country != null) || (locSet.region != null))
				lskit.remove();
		}
	}
	
	private Location[] getGeoData(String location, String country, String region, boolean allowCache) {
		
		//	fetch geo data in multiple threads
		ArrayList geoDataClusters = new ArrayList();
		GeoDataFetcher[] gdpThreads = new GeoDataFetcher[this.geoDataProviders.length];
		for (int p = 0; p < this.geoDataProviders.length; p++)
			gdpThreads[p] = new GeoDataFetcher(location, country, region, allowCache, this.geoDataProviders[p], geoDataClusters);
		
		//	wait for data fetcher threads to finish
		long gdpStartTime = System.currentTimeMillis();
		for (int d = 0; d < this.geoDataProviders.length; d++) try {
			gdpThreads[d].join(Math.max(1, ((1000 * this.geoDataProviderTimeout) - (System.currentTimeMillis() - gdpStartTime))));
		} catch (InterruptedException ie) {}
		
		//	we have to synchronize from here, as delayed data fetcher threads might interfere with code below
		synchronized (geoDataClusters) {
			
			//	rank data sets based on frequency
			Collections.sort(geoDataClusters);
			
			//	convert into locations
			Location[] geoData = new Location[geoDataClusters.size()];
			for (int c = 0; c < geoDataClusters.size(); c++)
				geoData[c] = ((GeoDataCluster) geoDataClusters.get(c)).toLocation();
			
			//	finally ...
			return geoData;
		}
	}
	
	private class GeoDataFetcher extends Thread {
		private String location;
		private String country;
		private String region;
		private boolean allowCache;
		private GeoDataProvider gdp;
		private ArrayList geoDataClusters;
		GeoDataFetcher(String location, String country, String region, boolean allowCache, GeoDataProvider gdp, ArrayList geoDataClusters) {
			this.location = location;
			this.country = country;
			this.region = region;
			this.allowCache = allowCache;
			this.gdp = gdp;
			this.geoDataClusters = geoDataClusters;
			this.start();
		}
		public void run() {
			
			//	fetch geo data
			Location[] geoData = this.gdp.getLocations(this.location, this.country, this.region, this.allowCache);
			if (geoData == null)
				return;
			
			//	add geo data to clusters, setting data provider attribute along the way
			for (int d = 0; d < geoData.length; d++) {
				geoData[d].attributes.setProperty(GEO_COORDINATE_SOURCE_ATTRIBUTE, this.gdp.getDataSourceName());
				synchronized (this.geoDataClusters) {
					for (int c = 0; c < this.geoDataClusters.size(); c++) {
						GeoDataCluster gdc = ((GeoDataCluster) this.geoDataClusters.get(c));
						if (gdc.includesLocation(geoData[d])) {
							gdc.addLocation(geoData[d]);
							geoData[d] = null;
							break;
						}
					}
					if (geoData[d] != null)
						this.geoDataClusters.add(new GeoDataCluster(geoData[d], geoDataClusterMinRadius));
				}
			}
		}
	}
	
	private static class GeoDataCluster implements Comparable {
		double centerLong;
		double centerLat;
		int avgElev;
		int avgPop;
		int radius;
		TreeSet locations = new TreeSet(locationComparator);
		GeoDataCluster(Location loc, int minRadius) {
			this.centerLong = loc.longDeg;
			this.centerLat = loc.latDeg;
			this.avgElev = loc.elevation;
			this.avgPop = loc.population;
			this.radius = Math.max(minRadius, loc.longLatPrecision);
			this.locations.add(loc);
		}
		boolean includesLocation(Location loc) {
			return (GeoUtils.getDistance(this.centerLong, this.centerLat, loc.longDeg, loc.latDeg) < (2 * this.radius));
		}
		void addLocation(Location loc) {
			if (!this.locations.add(loc))
				return;
			
			//	re-compute center
			this.centerLong = 0;
			this.centerLat = 0;
			this.avgElev = 0;
			int elevCount = 0;
			this.avgPop = 0;
			int popCount = 0;
			for (Iterator lit = this.locations.iterator(); lit.hasNext();) {
				loc = ((Location) lit.next());
				this.centerLong += loc.longDeg;
				this.centerLat += loc.latDeg;
				if (loc.elevation != Location.UNKNOWN_ELEVATION) {
					this.avgElev += loc.elevation;
					elevCount++;
				}
				if (loc.population != Location.UNKNOWN_POPULATION) {
					this.avgPop += loc.population;
					popCount++;
				}
			}
			this.centerLong /= this.locations.size();
			this.centerLat /= this.locations.size();
			if (elevCount == 0)
				this.avgElev = Location.UNKNOWN_ELEVATION;
			else this.avgElev /= elevCount;
			if (popCount == 0)
				this.avgPop = Location.UNKNOWN_POPULATION;
			else this.avgPop /= popCount;
			
			//	re-compute radius
			for (Iterator lit = this.locations.iterator(); lit.hasNext();) {
				loc = ((Location) lit.next());
				this.radius = ((int) Math.max(this.radius, GeoUtils.getDistance(this.centerLong, this.centerLat, loc.longDeg, loc.latDeg)));
				this.radius = ((int) Math.max(this.radius, loc.longLatPrecision));
			}
		}
		Location toLocation() {
			if (this.locations.size() == 1)
				return ((Location) this.locations.first());
			
			//	compute basic attributes and create location
			String name = this.getMostFrequentValue(NAME_ATTRIBUTE);
			int radius = Location.UNKNOWN_LONG_LAT_PRECISION;
			for (Iterator lit = this.locations.iterator(); lit.hasNext();) {
				Location loc = ((Location) lit.next());
				int centerDist = ((int) GeoUtils.getDistance(this.centerLong, this.centerLat, loc.longDeg, loc.latDeg));
				
				//	use uncertainty of location less distance to cluster center
				if (loc.longLatPrecision != Location.UNKNOWN_LONG_LAT_PRECISION) {
					if (radius == Location.UNKNOWN_LONG_LAT_PRECISION)
						radius = (loc.longLatPrecision - centerDist);
					else radius = ((int) Math.min(radius, (loc.longLatPrecision - centerDist)));
				}
				
				//	use plain distance as fallback for unknown precision
				else if (centerDist != 0) radius = ((int) Math.max(radius, centerDist));
			}
			Location aLoc = new Location(name, ("" + this.centerLong), ("" + this.centerLat), ("" + radius), ("" + this.avgElev), ("" + this.avgPop));
			
			//	add further attributes
			for (int a = 0; a < Location.ATTRIBUTE_NAMES.length; a++) {
				String value = this.getMostFrequentValue(Location.ATTRIBUTE_NAMES[a]);
				if ((value != null) && (aLoc.attributes.getProperty(Location.ATTRIBUTE_NAMES[a]) == null))
					aLoc.attributes.setProperty(Location.ATTRIBUTE_NAMES[a], value);
			}
			
			//	indicate aggregation
			StringBuffer geoCoordinateSource = new StringBuffer("Aggregate (");
			for (Iterator lit = this.locations.iterator(); lit.hasNext();) {
				String gcs = ((Location) lit.next()).attributes.getProperty(GEO_COORDINATE_SOURCE_ATTRIBUTE);
				if (gcs != null) {
					if (geoCoordinateSource.length() > "Aggregate (".length())
						geoCoordinateSource.append(", ");
					geoCoordinateSource.append(gcs);
				}
			}
			geoCoordinateSource.append(")");
			aLoc.attributes.setProperty(GEO_COORDINATE_SOURCE_ATTRIBUTE, geoCoordinateSource.toString());
			
			//	finally ...
			return aLoc;
		}
		private String getMostFrequentValue(String attribName) {
			StringVector values = new StringVector(false);
			for (Iterator lit = this.locations.iterator(); lit.hasNext();) {
				String value = ((Location) lit.next()).attributes.getProperty(attribName);
				if (value != null)
					values.addElement(value);
			}
			if (values.isEmpty())
				return null;
			else if (values.size() == 1)
				return values.get(0);
			String mfValue = values.get(0);
			for (int v = 1; v < values.size(); v++) {
				String value = values.get(v);
				if (values.getElementCount(value) > values.getElementCount(mfValue))
					mfValue = value;
			}
			return mfValue;
		}
		public int compareTo(Object obj) {
			return (((GeoDataCluster) obj).locations.size() - this.locations.size());
		}
	}
	private static Comparator locationComparator = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			return obj1.toString().compareTo(obj2.toString());
		}
	};
	
	private void setLocationAttributes(Annotation loc, Location geoDataSet) {
		
		//	add coordinates
		loc.setAttribute(LONGITUDE_ATTRIBUTE, ("" + geoDataSet.longDeg));
		loc.setAttribute(LATITUDE_ATTRIBUTE, ("" + geoDataSet.latDeg));
		if (geoDataSet.longLatPrecision != Location.UNKNOWN_LONG_LAT_PRECISION)
			loc.setAttribute(LONG_LAT_PRECISION_ATTRIBUTE, ("" + geoDataSet.longLatPrecision));
		if (geoDataSet.elevation != Location.UNKNOWN_ELEVATION)
			loc.setAttribute(ELEVATION_ATTRIBUTE, ("" + geoDataSet.elevation));
		
		//	add other attributes (if available)
		for (int a = 0; a < Location.ATTRIBUTE_NAMES.length; a++) {
			if (loc.hasAttribute(Location.ATTRIBUTE_NAMES[a]))
				continue;
			String attributeValue = geoDataSet.attributes.getProperty(Location.ATTRIBUTE_NAMES[a]);
			if ((attributeValue != null) && (attributeValue.trim().length() != 0)) {
				TokenSequence avts = Gamta.newTokenSequence(attributeValue, loc.getTokenizer());
				loc.setAttribute(Location.ATTRIBUTE_NAMES[a], TokenSequenceUtils.concatTokens(avts, true, true));
			}
		}
	}
	
	//	special dummy location data sets to indicate special actions
	private static final Location REMOVE = new Location("", 0f, 0f, 0); 
	private static final Location SPLIT = new Location("", 0f, 0f, 0); 
	private static final Location IGNORE = new Location("", 0f, 0f, 0); 
	
	//	special options to indicate special actions
	private static final String IGNORE_LOCATION_CHOICE = "Ignore location for geo referencing";
	private static final String SPLIT_LOCATION_SET_CHOICE = "Geo reference occurrences individually";
	private static final String REMOVE_LOCATION_CHOICE = "Remove location Annotation";
	
	//	return false only if interrupted
	private boolean getFeedback(LocationSet[] locationSets, TokenSequence text) {
		
		//	don't show empty dialog
		if (locationSets.length == 0)
			return false;
		
		//	create store for feedback results
		String[] selectedOptions = new String[locationSets.length];
		
		//	compute number of sets per dialog
		int dialogCount = ((locationSets.length + 9) / 10);
		int dialogSize = ((locationSets.length + (dialogCount / 2)) / dialogCount);
		dialogCount = ((locationSets.length + dialogSize - 1) / dialogSize);
		
		//	build dialogs
		AssignmentDisambiguationFeedbackPanel[] adfps = new AssignmentDisambiguationFeedbackPanel[dialogCount];
		for (int d = 0; d < adfps.length; d++) {
			adfps[d] = new AssignmentDisambiguationFeedbackPanel("Check Location GeoCoordinates");
			adfps[d].setLabel("<HTML>Please select the appropriate geographical coordinates for these <B>location names</B>." +
					"<BR>Select <I>&lt;Ignore location for geo referencing&gt;</I> to not assign the location name any coordinates now." +
					"<BR>Select <I>&lt;Remove location annotations&gt;</I> to indicate that the location name anctually is none." +
					"<BR>Select <I>&lt;Geo reference occurrences individually&gt;</I> to deal with the individual occurrences of the location names individually.</HTML>");
			int dialogOffset = (d * dialogSize);
			for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < locationSets.length); b++) {
				
				//	put locations in array
				String[] locOptions;
				String selectedLocationOption = IGNORE_LOCATION_CHOICE;
				
				//	single location, no need for split option
				if (locationSets[b + dialogOffset].locations.size() == 1) {
					locOptions = new String[locationSets[b + dialogOffset].geoDataSets.length + 2];
					locOptions[0] = IGNORE_LOCATION_CHOICE;
					locOptions[1] = REMOVE_LOCATION_CHOICE;
					for (int l = 0; l < locationSets[b + dialogOffset].geoDataSets.length; l++) {
						locOptions[l + 2] = locationSets[b + dialogOffset].geoDataSets[l].toString();
						if (locationSets[b + dialogOffset].geoDataSets[l].attributes.containsKey(GEO_COORDINATE_SOURCE_ATTRIBUTE))
							locOptions[l + 2] = (locOptions[l + 2] + " (" + locationSets[b + dialogOffset].geoDataSets[l].attributes.getProperty(Location.GEO_COORDINATE_SOURCE_ATTRIBUTE) + ")");
						if (l == 0)
							selectedLocationOption = locOptions[l + 2];
					}
				}
				
				//	include location set splitting option
				else {
					locOptions = new String[locationSets[b + dialogOffset].geoDataSets.length + 3];
					locOptions[0] = IGNORE_LOCATION_CHOICE;
					locOptions[1] = REMOVE_LOCATION_CHOICE;
					locOptions[2] = SPLIT_LOCATION_SET_CHOICE;
					for (int l = 0; l < locationSets[b + dialogOffset].geoDataSets.length; l++) {
						locOptions[l + 3] = locationSets[b + dialogOffset].geoDataSets[l].toString();
						if (l == 0)
							selectedLocationOption = locOptions[l + 3];
					}
				}
				
				//	build location & context display
				StringBuffer locContext = new StringBuffer("<HTML>");
				for (Iterator lit = locationSets[b + dialogOffset].locations.iterator(); lit.hasNext();) {
					Annotation loc = ((Annotation) lit.next());
					locContext.append(buildLabel(text, loc, 10));
					if (loc.hasAttribute(PAGE_NUMBER_ATTRIBUTE))
						locContext.append(" (page " + loc.getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")");
					if (lit.hasNext())
						locContext.append("<BR>");
				}
				locContext.append("</HTML>");
				
				//	add to feedback panel
				adfps[d].addLine(locContext.toString(), locOptions, selectedLocationOption);
			}
			
			//	add background information
			adfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, ((Annotation) locationSets[dialogOffset].locations.first()).getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((Annotation) locationSets[dialogOffset].locations.first()).getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((Annotation) locationSets[dialogOffset].locations.first()).getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, ((Annotation) locationSets[dialogOffset].locations.first()).getAnnotationID());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, LOCATION_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString((Annotation[]) locationSets[dialogOffset].locations.toArray(new Annotation[locationSets[dialogOffset].locations.size()]));
			if (targetPages != null)
				adfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString((Annotation[]) locationSets[dialogOffset].locations.toArray(new Annotation[locationSets[dialogOffset].locations.size()]));
			if (targetPageIDs != null)
				adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		
		//	show feedback dialogs
		int cutoffSet = locationSets.length;
		boolean continueAfterRound = true;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(adfps);
			
			//	process all feedback data together
			for (int d = 0; d < adfps.length; d++) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < adfps[d].lineCount(); b++)
					selectedOptions[b + dialogOffset] = adfps[d].getSelectedOptionAt(b);
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < adfps.length; d++) {
			if (d != 0)
				adfps[d].addButton("Previous");
			adfps[d].addButton("Cancel");
			adfps[d].addButton("OK" + (((d+1) == adfps.length) ? "" : " & Next"));
			
			String title = adfps[d].getTitle();
			adfps[d].setTitle(title + " - (" + (d+1) + " of " + adfps.length + ")");
			
			String f = adfps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			adfps[d].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < adfps[d].lineCount(); b++)
					selectedOptions[b + dialogOffset] = adfps[d].getSelectedOptionAt(b);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog onward
			else {
				cutoffSet = (d * dialogSize);
				continueAfterRound = false;
				break;
			}
		}
		
		
		//	process feedback
		for (int b = 0; b < cutoffSet; b++) {
			
			//	location removed
			if (REMOVE_LOCATION_CHOICE.equals(selectedOptions[b]))
				locationSets[b].geoDataSet = REMOVE;
			
			//	location cannot be handled right now
			else if (IGNORE_LOCATION_CHOICE.equals(selectedOptions[b]))
				locationSets[b].geoDataSet = IGNORE;
			
			//	locations in set need to be geo-referenced individually
			else if (SPLIT_LOCATION_SET_CHOICE.equals(selectedOptions[b]))
				locationSets[b].geoDataSet = SPLIT;
			
			//	find location data set for selected option
			else for (int d = 0; d < locationSets[b].geoDataSets.length; d++) {
				if (selectedOptions[b].startsWith(locationSets[b].geoDataSets[d].toString())) {
					locationSets[b].geoDataSet = locationSets[b].geoDataSets[d];
					d = locationSets[b].geoDataSets.length;
				}
			}
		}
		
		//	finally ...
		return continueAfterRound;
	}
	
	private class LocationSet {
		final int firstStartIndex;
		final String name;
		final String country;
		final String region;
//		ArrayList locations = new ArrayList();
		TreeSet locations = new TreeSet(AnnotationUtils.ANNOTATION_NESTING_ORDER);
		Location geoDataSet = null;
		Location[] geoDataSets = new Location[0];
//		LocationSet parent;
//		ArrayList subSets = null;
		LocationSet(int firstStartIndex, String name) {
			this(firstStartIndex, name, null, null);
		}
		LocationSet(int firstStartIndex, String name, String country) {
			this(firstStartIndex, name, country, null);
		}
		LocationSet(int firstStartIndex, String name, String country, String region) {
			this.firstStartIndex = firstStartIndex;
			this.name = name;
			this.country = country;
			this.region = region;
		}
		void addSubSet(LocationSet subSet) {
//			subSet.parent = this;
//			if (this.subSets == null)
//				this.subSets = new ArrayList();
//			this.subSets.add(subSet);
		}
	}
	
	private String buildLabel(TokenSequence text, Annotation annot, int envSize) {
		int aStart = annot.getStartIndex();
		int aEnd = annot.getEndIndex();
		int start = Math.max(0, (aStart - envSize));
		int end = Math.min(text.size(), (aEnd + envSize));
		StringBuffer sb = new StringBuffer("... ");
		Token lastToken = null;
		Token token = null;
		for (int t = start; t < end; t++) {
			lastToken = token;
			token = text.tokenAt(t);
			
			//	end highlighting value
			if (t == aEnd) sb.append("</B>");
			
			//	add spacer
			if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
			
			//	start highlighting value
			if (t == aStart) sb.append("<B>");
			
			//	append token
			sb.append(token);
		}
		
		return sb.append(" ...").toString();
	}
	
	public static void main(String[] args) throws Exception {
//		System.out.println("Geocoder: Initializing ...");
////		PrintStream sysOut = System.out;
////		System.setOut(new PrintStream(new OutputStream() {
////			public void write(int b) throws IOException {}
////		}));
//		GeoDataProvider[] gdps = GeoDataProvider.getDataProviders(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData")));
////		System.setOut(sysOut);
//		System.out.println("Geocoder: Initialized");
//		String location = "Karlsruhe";
//		String country = null;//"U.S.A.";
//		String region = null;//"Massachusetts";
//		Location[][] gdpsLocations = new Location[gdps.length][];
//		for (int p = 0; p < gdps.length; p++) {
//			System.out.println(gdps[p].getName() + ":");
//			gdpsLocations[p] = gdps[p].getLocations(location, country, region, false);
//			for (int l = 0; l < gdpsLocations[p].length; l++)
//				System.out.println(" - " + gdpsLocations[p][l].toString());
//		}
		GeoReferencerOnline gro = new GeoReferencerOnline();
		gro.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/GeoReferencerData")));
		String location = "Zurich";
		String country = null;//"U.S.A.";
		String region = null;//"Massachusetts";
		Location[] groLocations = gro.getGeoData(location, country, region, false);
		for (int l = 0; l < groLocations.length; l++)
			System.out.println(groLocations[l].attributes.getProperty(Location.GEO_COORDINATE_SOURCE_ATTRIBUTE) + ": " + groLocations[l].toString());
	}
}