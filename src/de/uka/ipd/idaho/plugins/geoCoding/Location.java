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


import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;

/**
 * @author sautter
 *
 * TODO document this class
 */
public class Location implements LocationConstants {
	
//	//	DiGIR elements for describing a location
//	public static final String ContinentOcean = "ContinentOcean"; //  The continent or ocean from which a specimen was collected.
//	public static final String Country = "Country"; //  The country or major political unit from which the specimen was collected. ISO 3166-1 values should be used. Full country names are currently in use. A future recommendation is to use ISO3166-1 two letter codes or the full name when searching
//	public static final String StateProvince = "StateProvince"; //  The state, province or region (i.e. next political region smaller than Country) from which the specimen was collected. There is some suggestion to use the values described in ISO 3166-2, however these values are in a continual state of flux and it appears unlikely that an appropriate mechanism (by ISO) will be in place to manage these changes. Hence it is recommended that where possible, the full, unabbreviated name should be used for storing information. The server should optionally handle abbreviations as an access point. Note this is a recurring theme (country and state) abbreviations. Check the existence of an attribute type to deal with abbreviations from the bib-1 profile
//	public static final String County = "County"; //  The county (or shire, or next political region smaller than State / Province) from which the specimen was collected
//	public static final String Locality = "Locality"; //  The locality description (place name plus optionally a displacement from the place name) from which the specimen was collected. Where a displacement from a location is provided, it should be in un-projected units of measurement

//	public static final String Longitude = "Longitude"; //  The longitude of the location from which the specimen was collected. This value should be expressed in decimal degrees with a datum such as WGS-84
//	public static final String Latitude = "Latitude"; //  The latitude of the location from which the specimen was collected. This value should be expressed in decimal degrees with a datum such as WGS-84
//	public static final String CoordinatePrecision = "CoordinatePrecision"; //  An estimate of how tightly the collecting locality was specified; expressed as a distance, in meters, that corresponds to a radius around the latitude-longitude coordinates. Use NULL where precision is unknown, cannot be estimated, or is not applicable.
//	public static final String BoundingBox = "BoundingBox"; //  This access point provides a mechanism for performing searches using a bounding box. A Bounding Box element is not typically present in the database, but rather is derived from the Latitude and Longitude columns by the data provider
//	
//	public static final String MinimumElevation = "MinimumElevation"; //  The minimum distance in meters above (positive) or below sea level of the collecting locality.
//	public static final String MaximumElevation = "MaximumElevation"; //  The maximum distance in meters above (positive) or below sea level of the collecting locality.
//	
//	public static final String MinimumDepth = "MinimumDepth"; //  The minimum distance in meters below the surface of the water at which the collection was made; all material collected was at least this deep. Positive below the surface, negative above (e.g. collecting above sea level in tidal areas).
//	public static final String MaximumDepth = "MaximumDepth"; //  The maximum distance in meters below the surface of the water at which the collection was made; all material collected was at most this deep. Positive below the surface, negative above (e.g. collecting above sea level in tidal areas).
	
//	//	additional elements, which may be out of the scope of DiGIR
//	public static final String NAME = "Name"; // the plain name of the location, without the additional data in the Locality element
//	public static final String ELEVATION = "Elevation"; // the elevation, shorthand for the min/max elevation and depth
//	public static final String POPULATION = "Population"; // the population
//	public static final String LOCATION_TYPE = "Type"; // the location type
	
	//	DiGIR elements for describing a location (not contained in LocationConstants)
//	public static final String CoordinatePrecision = "CoordinatePrecision"; //  An estimate of how tightly the collecting locality was specified; expressed as a distance, in meters, that corresponds to a radius around the latitude-longitude coordinates. Use NULL where precision is unknown, cannot be estimated, or is not applicable.
	public static final String BoundingBox = "BoundingBox"; //  This access point provides a mechanism for performing searches using a bounding box. A Bounding Box element is not typically present in the database, but rather is derived from the Latitude and Longitude columns by the data provider
	
	public static final String MinimumElevation = "MinimumElevation"; //  The minimum distance in meters above (positive) or below sea level of the collecting locality.
	public static final String MaximumElevation = "MaximumElevation"; //  The maximum distance in meters above (positive) or below sea level of the collecting locality.
	
	public static final String MinimumDepth = "MinimumDepth"; //  The minimum distance in meters below the surface of the water at which the collection was made; all material collected was at least this deep. Positive below the surface, negative above (e.g. collecting above sea level in tidal areas).
	public static final String MaximumDepth = "MaximumDepth"; //  The maximum distance in meters below the surface of the water at which the collection was made; all material collected was at most this deep. Positive below the surface, negative above (e.g. collecting above sea level in tidal areas).
	
	//	default values
	public static final String UNKNOWN_ATTRIBUTE_VALUE = "";
	public static final float UNKNOWN_LONGITUDE = Float.POSITIVE_INFINITY;
	public static final float UNKNOWN_LATITUDE = Float.POSITIVE_INFINITY;
	public static final int UNKNOWN_LONG_LAT_PRECISION = -1;
	public static final int UNKNOWN_ELEVATION = Integer.MIN_VALUE;
	public static final int UNKNOWN_POPULATION = -1;
	
	public final String name;
	
	public final float longDeg;
	public final float latDeg;
	public final int longLatPrecision;
	
	public final int elevation;
	public final int population;
	
	public final Properties attributes = new Properties() {
		public String getProperty(String key, String defaultValue) {
			if (NAME_ATTRIBUTE.equals(key)) return name;
			else if (LONGITUDE_ATTRIBUTE.equals(key)) return ("" + longDeg);
			else if (LATITUDE_ATTRIBUTE.equals(key)) return ("" + latDeg);
			else if (LONG_LAT_PRECISION_ATTRIBUTE.equals(key)) return ("" + longLatPrecision);
			else if (POPULATION_ATTRIBUTE.equals(key) && (population != UNKNOWN_POPULATION)) return ("" + population);
			else if (ELEVATION_ATTRIBUTE.equals(key) && (elevation != UNKNOWN_ELEVATION)) return ("" + elevation);
			else return super.getProperty(key, defaultValue);
		}
		public String getProperty(String key) {
			if (NAME_ATTRIBUTE.equals(key)) return name;
			else if (LONGITUDE_ATTRIBUTE.equals(key)) return ("" + longDeg);
			else if (LATITUDE_ATTRIBUTE.equals(key)) return ("" + latDeg);
			else if (LONG_LAT_PRECISION_ATTRIBUTE.equals(key)) return ("" + longLatPrecision);
			else if (POPULATION_ATTRIBUTE.equals(key) && (population != UNKNOWN_POPULATION)) return ("" + population);
			else if (ELEVATION_ATTRIBUTE.equals(key) && (elevation != UNKNOWN_ELEVATION)) return ("" + elevation);
			else return super.getProperty(key);
		}
		public synchronized Object setProperty(String key, String value) {
			Object oldValue = super.setProperty(key, value);
			if (LONG_LAT_PRECISION_ATTRIBUTE.equals(key) || COUNTRY_ATTRIBUTE.equals(key) || STATE_PROVINCE_ATTRIBUTE.equals(key))
				toString = null;
			return oldValue;
		}
	};
	
	public static final String[] ATTRIBUTE_NAMES = {
		CONTINENT_OCEAN_ATTRIBUTE,
		COUNTRY_ATTRIBUTE,
		STATE_PROVINCE_ATTRIBUTE,
		COUNTY_ATTRIBUTE,
		NAME_ATTRIBUTE,

		LONGITUDE_ATTRIBUTE,
		LATITUDE_ATTRIBUTE,
		LONG_LAT_PRECISION_ATTRIBUTE,
		BoundingBox,

		MinimumElevation,
		MaximumElevation,

		MinimumDepth,
		MaximumDepth,
		
//		NAME_ATTRIBUTE,
		ELEVATION_ATTRIBUTE,
		POPULATION_ATTRIBUTE,
		LOCATION_TYPE_ATTRIBUTE,
		
		GEO_COORDINATE_SOURCE_ATTRIBUTE
	};
	
	/**
	 * @param name
	 * @param longitude
	 * @param latitude
	 */
	public Location(String name, String longitude, String latitude, String longLatPrecision) {
		this(name, longitude, latitude, longLatPrecision, ("" + UNKNOWN_ELEVATION), ("" + UNKNOWN_POPULATION));
	}
	
	/**
	 * @param name
	 * @param varType
	 * @param region
	 * @param country
	 * @param longitude
	 * @param latitude
	 * @param elevation
	 * @param population
	 */
	public Location(String name, String longitude, String latitude, String longLatPrecision, String elevation, String population) {
		this.name = name;
		float fLong = UNKNOWN_LONGITUDE;
		try {
			fLong = Float.parseFloat(longitude);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.longDeg = fLong;
		
		float fLat = UNKNOWN_LATITUDE;
		try {
			fLat = Float.parseFloat(latitude);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.latDeg = fLat;
		
		int fLongLatPrec = UNKNOWN_LONG_LAT_PRECISION;
		try {
			fLongLatPrec = Integer.parseInt(longLatPrecision);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.longLatPrecision = fLongLatPrec;
		
		int elevationNumber = UNKNOWN_ELEVATION;
		try {
			elevationNumber = Integer.parseInt(elevation);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.elevation = elevationNumber;
		
		int populationNumber = UNKNOWN_POPULATION;
		try {
			populationNumber = Integer.parseInt(population);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.population = populationNumber;
	}

	/**
	 * @param name
	 * @param longitude
	 * @param latitude
	 */
	public Location(String name, float longitude, float latitude, int longLatPrecision) {
		this(name, longitude, latitude, longLatPrecision, ("" + UNKNOWN_ELEVATION), ("" + UNKNOWN_POPULATION));
	}
	
	/**
	 * @param name
	 * @param varType
	 * @param region
	 * @param country
	 * @param longitude
	 * @param latitude
	 * @param elevation
	 * @param population
	 */
	public Location(String name, float longitude, float latitude, int longLatPrecision, String elevation, String population) {
		this.name = name;
		this.longDeg = longitude;
		this.latDeg = latitude;
		this.longLatPrecision = longLatPrecision;
		
		int elevationNumber = UNKNOWN_ELEVATION;
		try {
			elevationNumber = Integer.parseInt(elevation);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.elevation = elevationNumber;
		
		int populationNumber = UNKNOWN_POPULATION;
		try {
			populationNumber = Integer.parseInt(population);
		}
		catch (NumberFormatException nfe) {}
		catch (NullPointerException npe) {}
		this.population = populationNumber;
	}

	public String toString() {
		if (this.toString != null)
			return this.toString;
		
		StringBuffer sb = new StringBuffer(this.name);
		
		String type = this.attributes.getProperty(LOCATION_TYPE_ATTRIBUTE);
		if (type != null)
			sb.append(" (" + type + ")");
		
		String country = this.attributes.getProperty(COUNTRY_ATTRIBUTE);
		String region = this.attributes.getProperty(STATE_PROVINCE_ATTRIBUTE);
		
		if ((country != null) && (region != null))
			sb.append(" / " + region + ", " + country);
		else if (region != null)
			sb.append(" / " + region);
		else if (country != null)
			sb.append(" / " + country);
		
		sb.append(" [");
		sb.append("long: " + this.longDeg + ", lat: " + this.latDeg + ", precision: " + ((this.longLatPrecision == UNKNOWN_LONG_LAT_PRECISION) ? "unknown" : (this.longLatPrecision + "m")));
		if (this.elevation != UNKNOWN_ELEVATION)
			sb.append(", at " + ((this.elevation == 0) ? "" : (Math.abs(this.elevation) + "m " + ((this.elevation < 0) ? "below " : "above "))) + "see level");
		sb.append("]");
		this.toString = sb.toString();
		
		return this.toString;
	}
	private String toString = null;
}