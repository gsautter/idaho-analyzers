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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import de.uka.ipd.idaho.gamta.util.AbstractAnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.locations.CountryHandler.RegionHandler;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * A GeoDataProvider is a wrapper for a source of geo-referencing data. This
 * abstract implementation provides the AnalyzerDataProvider, and caching
 * functionality. Sub classes only have to implement the actual lookup in the
 * backing data source.
 * 
 * @author sautter
 */
public abstract class GeoDataProvider implements LocationConstants {
	
	private static class Base {
		private AnalyzerDataProvider dataProvider;
		private GeoDataProvider[] instances = null;
		Base(AnalyzerDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}
		GeoDataProvider[] getDataProviders() {
			if (this.instances == null)
				this.instances = this.loadDataProviders();
			return this.instances;
		}
		private GeoDataProvider[] loadDataProviders() {
			System.out.println("GeoDataProvider: initializing instances");
			
			//	load geo data providers
			Object[] dataProviderObjects = GamtaClassLoader.loadComponents(
					new AbstractAnalyzerDataProvider() {
						public String[] getDataNames() {
							String[] dataNames = dataProvider.getDataNames();
							StringVector dataNameList = new StringVector();
							for (int d = 0; d < dataNames.length; d++) {
								if (dataNames[d].indexOf("cache/") == -1)
									dataNameList.addElementIgnoreDuplicates(dataNames[d]);
							}
							return dataNameList.toStringArray();
						}
						public boolean isDataAvailable(String dataName) {
							return dataProvider.isDataAvailable(dataName);
						}
						public InputStream getInputStream(String dataName) throws IOException {
							return dataProvider.getInputStream(dataName);
						}
						public URL getURL(String dataName) throws IOException {
							return dataProvider.getURL(dataName);
						}
						public boolean isDataEditable() {
							return dataProvider.isDataEditable();
						}
						public boolean isDataEditable(String dataName) {
							return dataProvider.isDataEditable(dataName);
						}
						public OutputStream getOutputStream(String dataName) throws IOException {
							return dataProvider.getOutputStream(dataName);
						}
						public boolean deleteData(String name) {
							return dataProvider.deleteData(name);
						}
						public String getAbsolutePath() {
							return dataProvider.getAbsolutePath();
						}
					}, 
					null, 
					GeoDataProvider.class, 
					new ComponentInitializer() {
						public void initialize(Object component, String componentJarName) throws Throwable {
							if (componentJarName == null)
								throw new RuntimeException("Cannot determine data path for " + component.getClass().getName());
							GeoDataProvider gdp = ((GeoDataProvider) component);
							componentJarName = componentJarName.substring(0, componentJarName.lastIndexOf('.')) + "Data";
							gdp.setDataProvider(new PrefixDataProvider(dataProvider, componentJarName));
						}
					});
			
			//	store & return geo data providers
			GeoDataProvider[] dataProviders = new GeoDataProvider[dataProviderObjects.length];
			for (int c = 0; c < dataProviderObjects.length; c++)
				dataProviders[c] = ((GeoDataProvider) dataProviderObjects[c]);
			return dataProviders;
		}
	}
	
	private static final HashMap bases = new HashMap();
	private static Base getBase(AnalyzerDataProvider dataProvider) {
		Base base = ((Base) bases.get(dataProvider.getAbsolutePath()));
		if (base == null) {
			base = new Base(dataProvider);
			bases.put(dataProvider.getAbsolutePath(),  base);
		}
		return base;
	}
	
	/**
	 * Retrieve all geo data providers currently available in a given folder,
	 * which is represented by an analyzer data provider.
	 * @param dataProvider the data provider to use for loading the geo data
	 *            providers (if not done yet)
	 * @return all geo data providers currently available
	 */
	public static GeoDataProvider[] getDataProviders(AnalyzerDataProvider dataProvider) {
		Base base = getBase(dataProvider);
		return base.getDataProviders();
	}
	
	private static class PrefixDataProvider extends AbstractAnalyzerDataProvider {
		private AnalyzerDataProvider dataProvider;
		private String pathPrefix;
		private PrefixDataProvider(AnalyzerDataProvider dataProvider, String pathPrefix) {
			this.dataProvider = dataProvider;
			this.pathPrefix = (pathPrefix.endsWith("/") ? pathPrefix : (pathPrefix + "/"));
		}
		public boolean deleteData(String name) {
			return this.dataProvider.isDataAvailable(this.addPrefix(name));
		}
		public String[] getDataNames() {
			String[] names = this.dataProvider.getDataNames();
			StringVector list = new StringVector();
			for (int n = 0; n < names.length; n++)
				if (names[n].startsWith(this.pathPrefix))
					list.addElementIgnoreDuplicates(names[n].substring(this.pathPrefix.length()));
			return list.toStringArray();
		}
		public InputStream getInputStream(String dataName) throws IOException {
			return this.dataProvider.getInputStream(this.addPrefix(dataName));
		}
		public OutputStream getOutputStream(String dataName) throws IOException {
			return this.dataProvider.getOutputStream(this.addPrefix(dataName));
		}
		public URL getURL(String dataName) throws IOException {
			return this.dataProvider.getURL((dataName.indexOf("://") == -1) ? this.addPrefix(dataName) : dataName);
		}
		public boolean isDataAvailable(String dataName) {
			return this.dataProvider.isDataAvailable(this.addPrefix(dataName));
		}
		public boolean isDataEditable() {
			return this.dataProvider.isDataEditable();
		}
		public boolean isDataEditable(String dataName) {
			return this.dataProvider.isDataEditable(this.addPrefix(dataName));
		}
		private String addPrefix(String name) {
			return (this.pathPrefix + (name.startsWith("/") ? name.substring(1) : name));
		}
		public String getAbsolutePath() {
			return this.dataProvider.getAbsolutePath() + "/" + this.pathPrefix.substring(0, (this.pathPrefix.length() - 1));
		}
	}
	
	protected AnalyzerDataProvider dataProvider;
	
	private CountryHandler countryHandler;
	
	/**
	 * @return the name of the data provider
	 */
	public String getName() {
		String name = this.getClass().getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		return name;
	}
	
	/**
	 * @return the name of the underlying data source
	 */
	public abstract String getDataSourceName();
	
	/**
	 * give the geo data provider access to its data
	 * @param dataProvider the data provider
	 */
	public void setDataProvider(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
		
		//	initialize cache keys
		this.cacheKeys.addElement(Location.NAME_ATTRIBUTE);
		this.cacheKeys.addElement(Location.COUNTRY_ATTRIBUTE);
		this.cacheKeys.addElement(Location.STATE_PROVINCE_ATTRIBUTE);
		this.cacheKeys.addElement(Location.LONGITUDE_ATTRIBUTE);
		this.cacheKeys.addElement(Location.LATITUDE_ATTRIBUTE);
		this.cacheKeys.addElement(Location.LONG_LAT_PRECISION_ATTRIBUTE);
		this.cacheKeys.addElement(Location.ELEVATION_ATTRIBUTE);
		this.cacheKeys.addElement(Location.POPULATION_ATTRIBUTE);
		
		//	get country handler
		if (this.dataProvider.isDataAvailable("countries.xml"))
			this.countryHandler = CountryHandler.getCountryHandler(this.dataProvider, "countries.xml");
		else this.countryHandler = CountryHandler.getCountryHandler(((AnalyzerDataProvider) null), ((String) null));
		
		//	do custom initialization
		this.init();
	}
	
	/**
	 * Initialize the geo data provider. This method is called after the
	 * <code>setDataProvider()</code> method. This default implementation does
	 * nothing, sub classes are welcome to overwrite it as needed.
	 */
	protected void init() {}
	
	/**
	 * Indicate whether this geo data provider uses a local or a remote (web
	 * based) data source. If this method returns true, local disc-based caching
	 * will be disabled, since it's no use caching data available from a local
	 * database on disc.
	 * @return true if the geo data provider uses a local data source, e.g. a
	 *         relational database, false otherwise
	 */
	protected abstract boolean isLocalDataProvider();
	
	/**
	 * Retrieve possible locations a given location name can refer to. If the
	 * <code>supportsGlobalSearch()</code> method returns false, this method
	 * returns an empty array.
	 * @param location the name of the location
	 * @return an array of GeoContainer representing the locations with the
	 *         specified name
	 */
	public Location[] getLocations(String location) {
		return this.getLocations(location, true);
	}
	
	/**
	 * Retrieve possible locations a given location name can refer to. If the
	 * <code>supportsGlobalSearch()</code> method returns false, this method
	 * returns an empty array.
	 * @param location the name of the location
	 * @param allowCache if set to false, the cache is not used
	 * @return an array of GeoContainer representing the locations with the
	 *         specified name
	 */
	public Location[] getLocations(String location, boolean allowCache) {
		return (this.supportsGlobalSearch() ? this.getLocations(location, null, null, allowCache) : new Location[0]);
	}
	
	/**
	 * Retrieve possible locations a given location name can refer to.
	 * @param location the name of the location
	 * @param country the country to restrict the search to
	 * @param region the state / province / district to restrict the search to
	 * @return an array of GeoContainer representing the locations with the
	 *         specified name
	 */
	public Location[] getLocations(String location, String country, String region) {
		return this.getLocations(location, country, region, true);
	}
	
	/**
	 * Retrieve possible locations a given location name can refer to.
	 * @param location the name of the location
	 * @param country the country to restrict the search to
	 * @param region the state / province / district to restrict the search to
	 * @param allowCache if set to false, the cache is not used
	 * @return an array of GeoContainer representing the locations with the
	 *         specified name
	 */
	public Location[] getLocations(String location, String country, String region, boolean allowCache) {
		System.out.println("GeoDataProvider (" + this.getClass().getName() + "): Doing lookup for " + location);
		
		//	catch empty location names
		if ((location == null) || (location.trim().length() == 0))
			return new Location[0];
		
		//	normalize parameters
		if ((country != null) && (country.trim().length() == 0))
			country = null;
		if ((region != null) && (region.trim().length() == 0))
			region = null;
		
		//	catch missing country
		if ((country == null) && !this.supportsGlobalSearch())
			return new Location[0];
		
		//	normalize country and region
		if (country != null) {
			country = this.countryHandler.getEnglishName(country);
			if (region != null) {
				RegionHandler regionHandler = this.countryHandler.getRegionHandler(country);
				if (regionHandler != null)
					region = regionHandler.getEnglishName(region);
			}
		}
		
		//	try and get location data
		Location[] locations = null;
		
		//	check memory based cache
		if (allowCache && this.supportsGlobalSearch())
			locations = ((Location[]) this.cache.get(location));
		if (locations != null)
			return this.filterLocations(locations, country, region);
		
		//	check disc based cache (for remote geo data providers only)
		if (allowCache && !this.isLocalDataProvider() && this.supportsGlobalSearch()) {
			locations = this.loadCachedLocationData(location);
			if (locations != null) {
				this.cache.put(location, locations);
				return this.filterLocations(locations, country, region);
			}
		}
		
		//	search and cache globally, and filter locally
		if (this.supportsGlobalSearch()) {
			locations = this.loadLocationData(location, null, null);
			if (locations != null) {
				this.cacheLocationData(location, locations);
				this.cache.put(location, locations);
				return this.filterLocations(locations, country, region);
			}
			else return new Location[0];
		}
		
		//	search restricted to country and region
		else {
			locations = this.loadLocationData(location, country, region);
			return ((locations == null) ? new Location[0] : locations);
		}
	}
	
	private Location[] filterLocations(Location[] locations, String country, String region) {
		
		//	nothing to filter, or to filter with
		if ((locations.length == 0) || (country == null))
			return locations;
		
		//	get region handler
		RegionHandler regionHandler = ((region == null) ? null : this.countryHandler.getRegionHandler(country));
		
		//	filter data
		LinkedList locationList = new LinkedList();
		for (int l = 0; l < locations.length; l++) {
			String lCountry = this.countryHandler.getEnglishName(locations[l].attributes.getProperty(COUNTRY_ATTRIBUTE));
			if (!country.equalsIgnoreCase(lCountry))
				continue;
			if (regionHandler != null) {
				String lRegion = regionHandler.getEnglishName(locations[l].attributes.getProperty(STATE_PROVINCE_ATTRIBUTE));
				if (!region.equalsIgnoreCase(lRegion))
					continue;
			}
			locationList.add(locations[l]);
		}
		
		//	finally ...
		return ((Location[]) locationList.toArray(new Location[locationList.size()]));
	}
	
	private HashMap cache = new LinkedHashMap(256, 0.95f, true) {
		protected boolean removeEldestEntry(Entry entry) {
			return (this.size() >= 256); // TODO: put cache size parameter here
		}
	};
	
	/**
	 * Store a set of locations in the file based cache. This method is intended
	 * for geo data providers whose backing data source returns locations in a
	 * prefix based fashion (like Falling Rain) rather than by strict name
	 * match. It allows for the locations that do not match the actual lookup
	 * name to be cached for later use. However, locations matching the lookup
	 * name must not be cached through this method by sub classes, as this is
	 * done automatically and thus would cause unnecessary disc writes.
	 * @param location the name of the location to store
	 * @param locations the locations to store
	 */
	protected void cacheLocationData(String location, Location[] locations) {
		
		//	sorry, nothing to cache ...
		if (locations.length == 0) return;
		
		//	normalize key
		String key = normalizeKey(location);
		String cacheDataName = getCacheDataName(key);
		
		//	sorry, caching impossible ...
		if (!this.dataProvider.isDataEditable(cacheDataName)) return;
		
		//	load existing data (if any)
		StringRelation data;
		Set existingData = new HashSet();
		if (this.dataProvider.isDataAvailable(cacheDataName)) {
			try {
				Reader r = new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8");
				data = StringRelation.readCsvData(r, '"', true, this.cacheKeys);
				r.close();
				
				for (int d = 0; d < data.size(); d++)
					existingData.add(data.get(d).toCsvString('"', this.cacheKeys));
			}
			catch (IOException ioe) {
				System.out.println("GeoDataProvider: Exception loading existing cache data: " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				data = new StringRelation();
			}
		}
		else data = new StringRelation();
		
		//	add new data
		boolean dataModified = false;
		for (int l = 0; l < locations.length; l++) {
			StringTupel lst = new StringTupel();
			lst.setValue(Location.NAME_ATTRIBUTE, locations[l].name);
			lst.setValue(Location.COUNTRY_ATTRIBUTE, locations[l].attributes.getProperty(Location.COUNTRY_ATTRIBUTE, ""));
			lst.setValue(Location.STATE_PROVINCE_ATTRIBUTE, locations[l].attributes.getProperty(Location.STATE_PROVINCE_ATTRIBUTE, ""));
			lst.setValue(Location.LONGITUDE_ATTRIBUTE, ("" + locations[l].longDeg));
			lst.setValue(Location.LATITUDE_ATTRIBUTE, ("" + locations[l].latDeg));
			lst.setValue(Location.LONG_LAT_PRECISION_ATTRIBUTE, ("" + locations[l].longLatPrecision));
			lst.setValue(Location.ELEVATION_ATTRIBUTE, ("" + locations[l].elevation));
			lst.setValue(Location.POPULATION_ATTRIBUTE, ("" + locations[l].population));
			
			if (existingData.add(lst.toCsvString('"', this.cacheKeys))) {
				data.addElement(lst);
				dataModified = true;
			}
		}
		
		//	store modified cache file
		if (dataModified) try {
			Writer w = new OutputStreamWriter(this.dataProvider.getOutputStream(cacheDataName), "UTF-8");
			StringRelation.writeCsvData(w, data, '"', this.cacheKeys);
			w.flush();
			w.close();
		}
		catch (IOException ioe) {
			System.out.println("GeoDataProvider: Exception writing cache data: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	private Location[] loadCachedLocationData(String locationName) {
		
		//	normalize key
		String key = normalizeKey(locationName);
		String cacheDataName = getCacheDataName(key);
		
		//	load existing data (if any)
		if (this.dataProvider.isDataAvailable(cacheDataName)) {
			try {
				Reader r = new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8");
				StringRelation data = StringRelation.readCsvData(r, '"', true, this.cacheKeys);
				r.close();
				
				ArrayList locationList = new ArrayList();
				for (int d = 0; d < data.size(); d++) {
					StringTupel lst = data.get(d);
					
					//	check name (might be slightly different due to normalization)
					String name = lst.getValue(Location.NAME_ATTRIBUTE, "");
					if (name.equals(locationName)) {
						Location location = new Location(name, lst.getValue(Location.LONGITUDE_ATTRIBUTE), lst.getValue(Location.LATITUDE_ATTRIBUTE), lst.getValue(Location.LONG_LAT_PRECISION_ATTRIBUTE, ("" + Location.UNKNOWN_LONG_LAT_PRECISION)), lst.getValue(Location.ELEVATION_ATTRIBUTE), lst.getValue(Location.POPULATION_ATTRIBUTE));
						location.attributes.setProperty(Location.COUNTRY_ATTRIBUTE, lst.getValue(Location.COUNTRY_ATTRIBUTE, ""));
						location.attributes.setProperty(Location.STATE_PROVINCE_ATTRIBUTE, lst.getValue(Location.STATE_PROVINCE_ATTRIBUTE, ""));
						locationList.add(location);
					}
				}
				return ((Location[]) locationList.toArray(new Location[locationList.size()]));
			}
			catch (IOException ioe) {
				System.out.println("GeoDataProvider: Exception loading cache data: " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		
		//	otherwise
		return null;
	}
	
	private StringVector cacheKeys = new StringVector();
	
	private static final String normalizeKey(String key) {
		StringBuffer normalizedKey = new StringBuffer();
		for (int c = 0; c < key.length(); c++) {
			char ch = key.charAt(c);
			if (('a' <= ch) && (ch <= 'z'))
				normalizedKey.append(ch);
			else if (('A' <= ch) && (ch <= 'Z'))
				normalizedKey.append((char) (ch - ('A' - 'a')));
			else if (('0' <= ch) && (ch <= '9'))
				normalizedKey.append(ch);
			else normalizedKey.append('_');
		}
		return normalizedKey.toString();
	}
	
	private static final String getCacheDataName(String key) {
		StringBuffer cacheDataName = new StringBuffer("cache/");
		for (int cl = 2; cl < Math.min((key.length() - 1), 6); cl+=2)
			cacheDataName.append("l-" + key.substring((cl-2), cl) + "/");
		cacheDataName.append("c-" + key);
		return cacheDataName.toString();
	}
	
	/**
	 * Load the location data for a given location name from the backing geo
	 * data source. Country and region may be null. If the backing geo data
	 * source does not allow country or region based filtering, implementations
	 * of this method are free to ignore the two respective parameters.
	 * @param location the name of the location
	 * @param country the country to restrict the search to
	 * @param region the state / province / district to restrict the search to
	 * @return an array holding all locations with the specified name
	 */
	public abstract Location[] loadLocationData(String location, String country, String region);
	
	/**
	 * Indicate whether or not the geo data source behind this data provider
	 * supports global search for a location name. If this method returns true,
	 * data will be loaded by location name only, i.e., the
	 * <code>country</code> and <code>region</code> arguments to
	 * <code>loadLocationData()</code> will always be null, and any country and
	 * region based filtering will happen locally. This is to facilitate
	 * caching data strictly by location name. If this method returns false,
	 * local disc based caching is disabled, and location search returns an
	 * empty array if the country is null.
	 * @return true to indicate global search, false otherwise
	 */
	protected abstract boolean supportsGlobalSearch();
	
	/**
	 * Normalize a country name to its current English ISO version.
	 * @param country the country name to normalize
	 * @return the normalized country name
	 */
	protected String normalizeCountry(String country) {
		return this.countryHandler.getEnglishName(country);
	}
	
	/**
	 * Normalize a region name to its current English ISO version.
	 * @param country the country the region lies in
	 * @param region the region name to normalize
	 * @return the normalized country name
	 */
	protected String normalizeRegion(String country, String region) {
		RegionHandler regionHandler = this.countryHandler.getRegionHandler(country);
		return ((regionHandler == null) ? region : regionHandler.getEnglishName(region));
	}
}