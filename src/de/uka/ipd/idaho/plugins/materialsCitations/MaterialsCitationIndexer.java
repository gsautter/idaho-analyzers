package de.uka.ipd.idaho.plugins.materialsCitations;
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
//package de.uka.ipd.idaho.plugin.materialsCitations;
//
//
//import java.io.File;
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Set;
//
//import de.uka.ipd.idaho.easyIO.EasyIO;
//import de.uka.ipd.idaho.easyIO.IoProvider;
//import de.uka.ipd.idaho.easyIO.SqlQueryResult;
//import de.uka.ipd.idaho.easyIO.sql.TableDefinition;
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.AnnotationUtils;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.QueriableAnnotation;
//import de.uka.ipd.idaho.gamta.TokenSequence;
//import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
//import de.uka.ipd.idaho.goldenGateServer.srs.AbstractIndexer;
//import de.uka.ipd.idaho.goldenGateServer.srs.Query;
//import de.uka.ipd.idaho.goldenGateServer.srs.QueryResult;
//import de.uka.ipd.idaho.goldenGateServer.srs.QueryResultElement;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.IndexResult;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.IndexResultElement;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.ThesaurusResult;
//import de.uka.ipd.idaho.plugins.locations.CountryHandler;
//import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//
///**
// * Indexer for taxonomic materials citations, as well as less elaborate
// * collecting locations.
// * 
// * @author sautter
// */
//public class MaterialsCitationIndexer extends AbstractIndexer implements MaterialsCitationConstants {
//	
//	private static final int NAME_LENGTH = 84;
//	private static final int COUNTRY_LENGTH = 64;
//	private static final int STATE_PROVINCE_LENGTH = 64;
//	
//	private static final String DEGREE_CIRCLE = "degreeCircle";
//	private static final String ELEVATION_CIRCLE = "elevationCircle";
//	
//	private static final String COLLECTING_EVENT_INDEX_LABEL = "Location Index";
//	private static final String COLLECTING_EVENT_INDEX_FIELDS = NAME_ATTRIBUTE + " " + COUNTRY_ATTRIBUTE + " " + LONGITUDE_ATTRIBUTE + " " + LATITUDE_ATTRIBUTE + " " + ELEVATION_ATTRIBUTE;
//	
//	private static final String UNKNOWN_LONGITUDE = "360"; // longitudes range from -180 to 180
//	private static final String UNKNOWN_LATITUDE = "360"; // longitudes range from -90 to 90
//	private static final String UNKNOWN_ELEVATION = "10000"; // impossible elevation on planet earth
//	
//	private static final String LSID_ATTRIBUTE = "LSID";
//	private static final int LSID_LENGTH = 128;
//	
//	private static final int TYPE_STATUS_LENGTH = 16;
//	private static final String ALL_TYPE_STATUS = "All Types";
//	private String[] typeStatus = new String[0];
//	
//	private static final int COLLECTION_CODE_LENGTH = 8;
//	private static final int SPECIMEN_CODE_LENGTH = 16;
//	
//	private IoProvider io;
////	private boolean indexTableOK = false;
//	
//	private CountryHandler countryHandler;
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#getIndexName()
//	 */
//	public String getIndexName() {
//		return MATERIALS_CITATION_ANNOTATION_TYPE;
//	}
//	
//	/** @see de.goldenGateSrs.AbstractIndexer#init()
//	 */
//	public void init() {
//		
//		//	get and check database connection
//		this.io = this.host.getIoProvider();
//		if (!this.io.isJdbcAvailable())
//			throw new RuntimeException("MaterialsCitationIndexer cannot work without database access.");
//		
//		//	assemble index definition
//		TableDefinition td = new TableDefinition(MATERIALS_CITATION_ANNOTATION_TYPE);
//		td.addColumn(DOC_NUMBER_COLUMN);
//		td.addColumn(LSID_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, LSID_LENGTH);
//		td.addColumn(NAME_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, NAME_LENGTH);
//		td.addColumn(NAME_ATTRIBUTE + "Search", TableDefinition.VARCHAR_DATATYPE, NAME_LENGTH);
//		td.addColumn(COUNTRY_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, COUNTRY_LENGTH);
//		td.addColumn(COUNTRY_ATTRIBUTE + "Search", TableDefinition.VARCHAR_DATATYPE, COUNTRY_LENGTH);
//		td.addColumn(STATE_PROVINCE_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, STATE_PROVINCE_LENGTH);
//		td.addColumn(STATE_PROVINCE_ATTRIBUTE + "Search", TableDefinition.VARCHAR_DATATYPE, STATE_PROVINCE_LENGTH);
//		td.addColumn(TYPE_STATUS, TableDefinition.VARCHAR_DATATYPE, TYPE_STATUS_LENGTH);
//		td.addColumn(COLLECTION_CODE, TableDefinition.VARCHAR_DATATYPE, COLLECTION_CODE_LENGTH);
//		td.addColumn(SPECIMEN_CODE, TableDefinition.VARCHAR_DATATYPE, SPECIMEN_CODE_LENGTH);
//		td.addColumn(SPECIMEN_COUNT, TableDefinition.INT_DATATYPE, 0);
//		td.addColumn(LONGITUDE_ATTRIBUTE, TableDefinition.REAL_DATATYPE, 0);
//		td.addColumn(LATITUDE_ATTRIBUTE, TableDefinition.REAL_DATATYPE, 0);
//		td.addColumn(ELEVATION_ATTRIBUTE, TableDefinition.INT_DATATYPE, 0);
//		if (!this.io.ensureTable(td, true))
//			throw new RuntimeException("MaterialsCitationIndexer cannot work without database access.");
////		this.indexTableOK = this.io.ensureTable(td, true);
//		
//		//	create database indexes
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, DOC_NUMBER_COLUMN_NAME);
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, LSID_ATTRIBUTE);
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, (NAME_ATTRIBUTE + "Search"));
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, (COUNTRY_ATTRIBUTE + "Search"));
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, (STATE_PROVINCE_ATTRIBUTE + "Search"));
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, TYPE_STATUS);
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, COLLECTION_CODE);
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, SPECIMEN_CODE);
//		this.io.indexColumn(MATERIALS_CITATION_ANNOTATION_TYPE, ELEVATION_ATTRIBUTE);
//		String[] longLat = {LONGITUDE_ATTRIBUTE, LATITUDE_ATTRIBUTE};
//		this.io.indexColumns(MATERIALS_CITATION_ANNOTATION_TYPE, longLat);
//		
//		//	load type status list
//		try {
//			StringVector tsl = StringVector.loadList(new File(this.dataPath, "typeStatus.txt"));
//			tsl.sortLexicographically(false, false);
//			for (int t = 0; t < tsl.size(); t++)
//				tsl.setElementAt(Gamta.capitalize(tsl.get(t)), t);
//			tsl.insertElementAt(ALL_TYPE_STATUS, 0);
//			this.typeStatus = tsl.toStringArray();
//		}
//		catch (IOException ioe) {
//			System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading type status list.");
//			ioe.printStackTrace(System.out);
//			this.typeStatus = new String[1];
//			this.typeStatus[0] = ALL_TYPE_STATUS;
//		}
//		
//		this.countryHandler = CountryHandler.getCountryHandler(new AnalyzerDataProviderFileBased(this.dataPath));
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.AbstractGoldenGateSrsPlugin#exit()
//	 */
//	public void exit() {
//		//	disconnect from database
//		this.io.close();
//	}
//	
//	private static final float UNKNOWN_FLOAT = Float.MAX_VALUE / 2;
//	private static final int UNKNOWN_INT = Integer.MAX_VALUE / 2;
//	
//	private static class Location {
////		String name;
//		String country;
////		String stateProvince;
//		
//		float longDeg = UNKNOWN_FLOAT;
//		float latDeg = UNKNOWN_FLOAT;
//		float elev = UNKNOWN_FLOAT;
//		
//		Location(String name, String country, String stateProvince, String longitude, String latitude, String elevation) {
////			this.name = name;
//			this.country = country;
////			this.stateProvince = stateProvince;
//			
//			try {
//				this.longDeg = Float.parseFloat(longitude);
//			} catch (NumberFormatException nfe) {}
//			
//			try {
//				this.latDeg = Float.parseFloat(latitude);
//			} catch (NumberFormatException nfe) {}
//			
//			try {
//				this.elev = Integer.parseInt(elevation);
//			} catch (NumberFormatException nfe) {}
//		}
//		
//		Location(ParsedQuery pq) {
////			this.name = pq.name;
//			this.country = pq.country;
////			this.stateProvince = pq.stateProvince;
//			
//			if (pq.useLong) this.longDeg = pq.fLong;
//			if (pq.useLat) this.latDeg = pq.fLat;
//			if (pq.useElev) this.elev = pq.fElev;
//		}
//	}
//	
//	private static class LocationComparator implements Comparator {
//		
//		final String refCountry;
//		final float refLong;
//		final float refLat;
//		final float refElev;
//		
//		LocationComparator(Location reference) {
//			this.refCountry = reference.country;
//			this.refLong = reference.longDeg;
//			this.refLat = reference.latDeg;
//			this.refElev = reference.elev;
//		}
//		
//		/** @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
//		 */
//		public int compare(Object o1, Object o2) {
//			if ((o1 instanceof Location) && (o2 instanceof Location)) return this.compare(((Location) o1), ((Location) o2));
//			return 0;
//		}
//		
//		int compare(Location loc1, Location loc2) {
//			if ((loc1 == null) && (loc2 == null)) return 0;
//			if (loc2 == null) return -1;
//			if (loc1 == null) return 1;
//			
//			float distLong;
//			float distLat;
//			float distElev;
//			
//			if (this.refLong == UNKNOWN_FLOAT) distLong = 0;	//	longitude not specified in reference
//			else distLong = this.refLong - loc1.longDeg;
//			
//			if (this.refLat == UNKNOWN_FLOAT) distLat = 0;	//	latitude not specified in reference
//			else distLat = this.refLat - loc1.latDeg;
//			
//			if (this.refElev == UNKNOWN_INT) distElev = 0;	//	elevation not specified in reference
//			else {
//				distElev = this.refElev - loc1.elev;
//				distElev /= 1000;	//	adjust elevation distance to long/lat distance
//			}
//			
//			float dist1 = ((distLong * distLong) + (distLat * distLat) + (distElev * distElev));
//			if ((this.refCountry != null) && (this.refCountry.length() != 0))
//				dist1 += (this.refCountry.equalsIgnoreCase(loc1.country) ? 0 : 1);
//			
//			
//			if (this.refLong == UNKNOWN_FLOAT) distLong = 0;	//	longitude not specified in reference
//			else distLong = this.refLong - loc2.longDeg;
//			
//			if (this.refLat == UNKNOWN_FLOAT) distLat = 0;	//	latitude not specified in reference
//			else distLat = this.refLat - loc2.latDeg;
//			
//			if (this.refElev == UNKNOWN_INT) distElev = 0;	//	elevation not specified in reference
//			else {
//				distElev = this.refElev - loc2.elev;
//				distElev /= 1000;	//	adjust elevation distance to long/lart distance
//			}
//			
//			float dist2 = ((distLong * distLong) + (distLat * distLat) + (distElev * distElev));
//			if ((this.refCountry != null) && (this.refCountry.length() != 0))
//				dist2 += (this.refCountry.equalsIgnoreCase(loc2.country) ? 0 : 1);
//			
//			
//			return ((dist1 == dist2) ? 0 : ((dist1 < dist2) ? -1 : 1));
//		}
//	}
//	
//	private class ParsedQuery {
//		final String lsid;
//		
//		final String[] locationTokens;
//		
//		final String name;
//		final String country;
//		final String stateProvince;
//		
//		final String typeStatus;
//		final String collectionCode;
//		final String specimenCode;
//		
//		final float fLong;
//		final boolean useLong;
//		final float fLat;
//		final boolean useLat;
//		final int iDegCircle;
//		
//		final float fElev;
//		final boolean useElev;
//		final int iElevCircle;
//		
//		ParsedQuery(Query query) {
//			
//			String lsid = query.getValue(LSID_ATTRIBUTE, "").trim();
//			if (lsid.length() > LSID_LENGTH) lsid = lsid.substring(0, LSID_LENGTH);
//			this.lsid = ((lsid.length() == 0) ? null : lsid);
//			
//			
//			String location = query.getValue(LOCATION_TYPE, "").trim();
//			TokenSequence locationTokenSequences = Gamta.NO_INNER_PUNCTUATION_TOKENIZER.tokenize(location);
//			StringVector locationTokens = TokenSequenceUtils.getTextTokens(locationTokenSequences);
//			this.locationTokens = locationTokens.toStringArray();
//			
//			
//			String name = prepareSearchString(query.getValue(NAME_ATTRIBUTE, "").trim().toLowerCase());
//			if (name.length() > NAME_LENGTH) name = name.substring(0, NAME_LENGTH);
//			this.name = ((name.length() == 0) ? null : name);
//			
//			String country = prepareSearchString(query.getValue(COUNTRY_ATTRIBUTE, "").trim().toLowerCase());
//			if (countryHandler != null)
//				country = countryHandler.getEnglishName(country);
//			if (country.length() > COUNTRY_LENGTH)
//				country = country.substring(0, COUNTRY_LENGTH);
//			this.country = ((country.length() == 0) ? null : country);
//			//	TODOne normalize country name to American English
//			
//			String stateProvince = prepareSearchString(query.getValue(STATE_PROVINCE_ATTRIBUTE, "").trim().toLowerCase());
//			if (stateProvince.length() > STATE_PROVINCE_LENGTH)
//				stateProvince = stateProvince.substring(0, STATE_PROVINCE_LENGTH);
//			this.stateProvince = ((stateProvince.length() == 0) ? null : stateProvince);
//			
//			
//			String typeStatus = prepareSearchString(query.getValue(TYPE_STATUS, "").trim().toLowerCase());
//			if (typeStatus.length() > TYPE_STATUS_LENGTH)
//				typeStatus = typeStatus.substring(0, TYPE_STATUS_LENGTH);
//			if (ALL_TYPE_STATUS.equalsIgnoreCase(typeStatus))
//				typeStatus = "";
//			else typeStatus = Gamta.capitalize(typeStatus);
//			this.typeStatus = ((typeStatus.length() == 0) ? null : typeStatus);
//			
//			String collectionCode = prepareSearchString(query.getValue(COLLECTION_CODE, "").trim().toLowerCase());
//			if (collectionCode.length() > COLLECTION_CODE_LENGTH)
//				collectionCode = collectionCode.substring(0, COLLECTION_CODE_LENGTH);
//			collectionCode = collectionCode.toUpperCase();
//			this.collectionCode = ((collectionCode.length() == 0) ? null : collectionCode);
//			
//			String specimenCode = prepareSearchString(query.getValue(SPECIMEN_CODE, "").trim().toLowerCase());
//			if (specimenCode.length() > SPECIMEN_CODE_LENGTH)
//				specimenCode = specimenCode.substring(0, SPECIMEN_CODE_LENGTH);
//			specimenCode = specimenCode.toUpperCase();
//			this.specimenCode = ((specimenCode.length() == 0) ? null : specimenCode);
//			
//			
//			//	check degrees
//			String longitude = query.getValue(LONGITUDE_ATTRIBUTE, "").trim();
//			String latitude = query.getValue(LATITUDE_ATTRIBUTE, "").trim();
//			String degreeCircle = query.getValue(DEGREE_CIRCLE, "").trim();
//			
//			float fLong = UNKNOWN_FLOAT;
//			if (longitude.length() != 0) try {
//				fLong = Float.parseFloat(longitude);
//			} catch (NumberFormatException nfe) {}
//			this.fLong = fLong;
//			this.useLong = ((fLong >= -180) && (fLong <= 180));
//			
//			float fLat = UNKNOWN_FLOAT;
//			if (latitude.length() != 0) try {
//				fLat = Float.parseFloat(latitude);
//			} catch (NumberFormatException nfe) {}
//			this.fLat = fLat;
//			this.useLat = ((fLat >= -90) && (fLat <= 90));
//			
//			int iDegCircle = 1;
//			if (latitude.length() != 0) try {
//				iDegCircle = Integer.parseInt(degreeCircle);
//			} catch (NumberFormatException nfe) {}
//			this.iDegCircle = iDegCircle;
//			
//			//	 check elevation
//			String elevation = query.getValue(ELEVATION_ATTRIBUTE, "").trim();
//			String elevationCircle = query.getValue(ELEVATION_CIRCLE, "").trim();
//			
//			float fElev = UNKNOWN_FLOAT;
//			if (elevation.length() != 0) try {
//				fElev = Integer.parseInt(elevation);
//			} catch (NumberFormatException nfe) {}
//			this.fElev = fElev;
//			this.useElev = ((fElev >= -11000) && (fElev <= 10000)); // minimum and maximum possible elevation on earth
//			
//			int iElevCircle = 100;
//			if (latitude.length() != 0) try {
//				iElevCircle = Integer.parseInt(elevationCircle);
//			} catch (NumberFormatException nfe) {}
//			this.iElevCircle = iElevCircle;
//		}
//		
//		boolean gotDetailQuery() {
//			return (this.useLong || this.useLat || this.useElev
//					||
//					(this.name != null) || (this.country != null) || (this.stateProvince != null)
//					||
//					(this.typeStatus != null) || (this.collectionCode != null) || (this.specimenCode != null)
//				);
//		}
//		
//		String getDetailPredicate() {
//			
//			StringBuffer where = new StringBuffer("1=1");
//			
//			//	use longitude
//			if (this.useLong) where.append(" AND " + LONGITUDE_ATTRIBUTE + " >= " + (this.fLong - this.iDegCircle) + " AND " + LONGITUDE_ATTRIBUTE + " <= " + (this.fLong + this.iDegCircle));
//			
//			//	use latitude
//			if (this.useLat) where.append(" AND " + LATITUDE_ATTRIBUTE + " >= " + (this.fLat - this.iDegCircle) + " AND " + LATITUDE_ATTRIBUTE + " <= " + (this.fLat + this.iDegCircle));
//			
//			//	use elevation
//			if (this.useElev) where.append(" AND " + ELEVATION_ATTRIBUTE + " >= " + (this.fElev - this.iElevCircle) + " AND " + ELEVATION_ATTRIBUTE + " <= " + (this.fElev + this.iElevCircle));
//			
//			
//			//	use name
//			if (!this.useLong && !this.useLat) {
//				if (this.name != null)
//					where.append(" AND " + NAME_ATTRIBUTE + "Search" + " LIKE '" + EasyIO.prepareForLIKE(this.name) + "%'");
//			}
//			
//			//	use country
//			if (this.country != null)
//				where.append(" AND " + COUNTRY_ATTRIBUTE + "Search" + " LIKE '" + EasyIO.prepareForLIKE(this.country) + "%'");
//			
//			//	use state / province / region
//			if (this.stateProvince != null)
//				where.append(" AND " + STATE_PROVINCE_ATTRIBUTE + "Search" + " LIKE '" + EasyIO.prepareForLIKE(this.stateProvince) + "%'");
//			
//			
//			//	use type status
//			if (this.typeStatus != null)
//				where.append(" AND " + TYPE_STATUS + " LIKE '" + EasyIO.prepareForLIKE(this.typeStatus) + "%'");
//			
//			//	use collection code
//			if (this.collectionCode != null)
//				where.append(" AND " + COLLECTION_CODE + " LIKE '" + EasyIO.prepareForLIKE(this.collectionCode) + "%'");
//			
//			//	use specimen code
//			if (this.specimenCode != null)
//				where.append(" AND " + SPECIMEN_CODE + " LIKE '" + EasyIO.prepareForLIKE(this.specimenCode) + "%'");
//			
//			
//			//	we got it
//			return ((where.length() == 3) ? null : where.toString());
//		}
//		
//		boolean gotFreeTextQuery() {
//			return (this.locationTokens.length != 0);
//		}
//		
//		String getFreeTextPredicate() {
//			if (this.locationTokens.length == 0) return null;
//			
//			StringBuffer where = new StringBuffer("1=1");
//			for (int t = 0; t < this.locationTokens.length; t++) {
//				String locationToken = EasyIO.prepareForLIKE(locationTokens[t].toLowerCase());
//				where.append(" AND (" + NAME_ATTRIBUTE + "Search LIKE '" + locationToken + "%' OR " + COUNTRY_ATTRIBUTE + "Search LIKE '" + locationToken + "%' OR " + STATE_PROVINCE_ATTRIBUTE + "Search LIKE '" + locationToken + "%')");
//			}
//			return where.toString();
//		}
//		
//		boolean gotLsidQuery() {
//			return (this.lsid != null);
//		}
//		
//		String getLsidPredicate() {
//			return ((this.lsid == null) ? null : (LSID_ATTRIBUTE + " LIKE '" + EasyIO.prepareForLIKE(lsid) + "'"));
//		}
//	}
//	
//	/** @see de.goldenGateSrs.Indexer#processQuery(de.goldenGateSrs.Query)
//	 */
//	public QueryResult processQuery(Query query) {
////		if (!this.indexTableOK) return null;
//		QueryResult qr;
//		
//		ParsedQuery pq = new ParsedQuery(query);
//		
//		//	process LSID query
//		if (pq.gotLsidQuery()) {
//			System.out.println("  - doing LSID search");
//			qr = this.getLsidResult(pq);
//		}
//		
//		//	process free text query
//		else if (pq.gotFreeTextQuery()) {
//			System.out.println("  - doing free text location search");
//			qr = this.getFreeTextResult(pq);
//		}
//		
//		//	process detail query
//		else if (pq.gotDetailQuery()) {
//			System.out.println("  - doing detail location search");
//			qr = this.getDetailResult(pq);
//		}
//		
//		//	no query at all
//		else {
//			System.out.println("  - no query given");
//			qr = null;
//		}
//		
//		//	return result
//		if (qr == null) System.out.println("  - got null result");
//		else System.out.println("  - got " + qr.size() + " hits");
//		return qr;
//	}
//	
//	private QueryResult getDetailResult(ParsedQuery pq) {
//		
//		//	geocode name if given
//		Location queryLocation = this.disambiguateLocation(pq);
//		
//		//	assemble query
//		String queryString = ("SELECT " + DOC_NUMBER_COLUMN_NAME + ", " + 
//				NAME_ATTRIBUTE + ", " + COUNTRY_ATTRIBUTE + ", " + STATE_PROVINCE_ATTRIBUTE + ", " + 
//				LONGITUDE_ATTRIBUTE + ", " + LATITUDE_ATTRIBUTE + ", " + ELEVATION_ATTRIBUTE + 
//				" FROM " + MATERIALS_CITATION_ANNOTATION_TYPE + 
//				" WHERE " +  pq.getDetailPredicate() + ";");
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(queryString, (pq.name != null));
//			
//			ArrayList locations = new ArrayList();
//			HashMap docNrsByLocations = new HashMap();
//			
//			while (sqr.next()) {
//				String docNr = sqr.getString(0);
//				String name = sqr.getString(1);
//				String country = sqr.getString(2);
//				String stateProvince = sqr.getString(3);
//				String longDeg = sqr.getString(4);
//				String latDeg = sqr.getString(5);
//				String elev = sqr.getString(6);
//				Location location = new Location(name, country, stateProvince, longDeg, latDeg, elev);
//				locations.add(location);
////				docNrsByLocations.put(location, new Integer(docNr));
//				docNrsByLocations.put(location, new Long(docNr));
//			}
//			sqr.close();
//			
//			Collections.sort(locations, new LocationComparator(queryLocation));
//			
//			//	assemble result
//			QueryResult result = new QueryResult();
//			Location location;
//			float distLong;
//			float distLat;
//			float distElev;
//			double relevance;
//			for (int i = 0; i < locations.size(); i++) {
//				location = ((Location) locations.get(i));
//				
//				distLong = queryLocation.longDeg - location.longDeg;
//				if (!pq.useLong) distLong = 0;	//	longitude not specified in reference
//				distLat = queryLocation.latDeg - location.latDeg;
//				if (!pq.useLat) distLat = 0;	//	latitude not specified in reference
//				distElev = queryLocation.elev - location.elev;
//				if (!pq.useElev) distElev = 0;
//				
//				relevance = 1;
//				if (pq.iDegCircle == 0) relevance -= (((distLong == 0) && (distLat == 0)) ? 0 : 1);
//				else relevance -= (((distLong * distLong) + (distLat * distLat)) / pq.iDegCircle);
//				if (pq.iElevCircle == 0) relevance -= ((distElev == 0) ? 0 : 1);
//				else relevance -= (distElev / pq.iElevCircle);
//				
//				if (relevance > 0) {
////					Integer docNr = ((Integer) docNrsByLocations.get(location));
////					if (docNr != null)
////						result.addResultElement(new QueryResultElement(docNr.intValue(), relevance));
//					Long docNr = ((Long) docNrsByLocations.get(location));
//					if (docNr != null)
//						result.addResultElement(new QueryResultElement(docNr.longValue(), relevance));
//				}
//			}
//			result.sortByRelevance(true);
//			
//			//	add result to query envelope
//			return result;
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ")  while getting document IDs.");
//			System.out.println("  Query was " + queryString);
//			return null;
//		}
//		finally {
//			if (sqr != null)
//				sqr.close();
//		}
//	}
//	
//	private QueryResult getLsidResult(ParsedQuery query) {
//		
//		String lsidQuery = "SELECT DISTINCT " + DOC_NUMBER_COLUMN_NAME +
//			" FROM " + MATERIALS_CITATION_ANNOTATION_TYPE +
//			" WHERE " + query.getLsidPredicate() + ";";
//		
//		SqlQueryResult sqr = null;
//		QueryResult result = null;
//		try {
//			sqr = this.io.executeSelectQuery(lsidQuery);
//			
//			result = new QueryResult();
//			while (sqr.next()) {
//				
//				//	read data
//				String docNr = sqr.getString(0);
//				if (docNr != null)
////					result.addResultElement(new QueryResultElement(Integer.parseInt(docNr), 1.0));
//					result.addResultElement(new QueryResultElement(Long.parseLong(docNr), 1.0));
//			}
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while searching.");
//			System.out.println("  Query was " + lsidQuery);
//		}
//		finally {
//			if (sqr != null)
//				sqr.close();
//		}
//		
//		return result;
//	}
//	
//	private QueryResult getFreeTextResult(ParsedQuery query) {
//		
//		String freeTextQuery = "SELECT DISTINCT " + DOC_NUMBER_COLUMN_NAME +
//			" FROM " + MATERIALS_CITATION_ANNOTATION_TYPE +
//			" WHERE " + query.getFreeTextPredicate() + ";";
//		
//		SqlQueryResult sqr = null;
//		QueryResult result = null;
//		try {
//			sqr = this.io.executeSelectQuery(freeTextQuery);
//			
//			result = new QueryResult();
//			while (sqr.next()) {
//				
//				//	read data
//				String docNr = sqr.getString(0);
//				if (docNr != null)
////					result.addResultElement(new QueryResultElement(Integer.parseInt(docNr), 1.0));
//					result.addResultElement(new QueryResultElement(Long.parseLong(docNr), 1.0));
//			}
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while searching.");
//			System.out.println("  Query was " + freeTextQuery);
//		}
//		finally {
//			if (sqr != null)
//				sqr.close();
//		}
//		
//		return result;
//	}
//	
//	private Location disambiguateLocation(ParsedQuery pq) {
//		if (/*this.indexTableOK && */(pq.name != null) && ((pq.country != null) || pq.useLong || pq.useLat || pq.useElev)) {
//			Location[] locations = this.getLocations(pq.name, pq.country);
//			if (locations.length != 0) {
//				Arrays.sort(locations, new LocationComparator(new Location(pq)));
//				return locations[0];
//			}
//		}
//		return new Location(pq);
//	}
//	
//	private Location[] getLocations(String name, String country) {
////		if (!this.indexTableOK) return new Location[0];
////		
//		if (name.length() > NAME_LENGTH)
//			name = name.substring(0, NAME_LENGTH);
//		
//		String columns = (NAME_ATTRIBUTE +
//				", " + COUNTRY_ATTRIBUTE +
//				", " + STATE_PROVINCE_ATTRIBUTE +
//				", " + LONGITUDE_ATTRIBUTE +
//				", " + LATITUDE_ATTRIBUTE +
//				", " + ELEVATION_ATTRIBUTE);
//		
//		String queryString = ("SELECT DISTINCT " + columns + 
//				" FROM " + MATERIALS_CITATION_ANNOTATION_TYPE + 
//				" WHERE " + NAME_ATTRIBUTE + "Search" + " LIKE '" + EasyIO.prepareForLIKE(name) + "%';");
//		
//		ArrayList locations = new ArrayList();
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(queryString);
//			
//			//	read locations
//			while (sqr.next())
//				locations.add(new Location(sqr.getString(0), sqr.getString(1), sqr.getString(2), sqr.getString(3), sqr.getString(4), sqr.getString(5)));
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while getting locations.");
//			System.out.println("  Query was " + queryString);
//		}
//		finally {
//			if (sqr != null)
//				sqr.close();
//		}
//		
//		return ((Location[]) locations.toArray(new Location[locations.size()]));
//	}
//	
//	/** @see de.goldenGateSrs.AbstractIndexer#markEssentialDetails(de.gamta.MutableAnnotation)
//	 */
//	public void markEssentialDetails(MutableAnnotation doc) {
//		
//		//	get locations
//		Annotation[] locations = doc.getAnnotations(LOCATION_TYPE);
//		for (int l = 0; l < locations.length; l++) {
//			if (locations[l].hasAttribute(LONGITUDE_ATTRIBUTE) && locations[l].hasAttribute(LATITUDE_ATTRIBUTE))
//				doc.addAnnotation(DETAIL_ANNOTATION_TYPE, locations[l].getStartIndex(), locations[l].size()).setAttribute(DETAIL_TYPE_ATTRIBUTE, LOCATION_TYPE);
//		}
//	}
//
//	/** @see de.goldenGateSrs.AbstractIndexer#markSearchables(de.gamta.MutableAnnotation)
//	 */
//	public void markSearchables(MutableAnnotation doc) {
//		
//		//	get locations
//		Annotation[] locations = doc.getAnnotations(LOCATION_TYPE);
//		
//		//	add search link attributes
//		for (int l = 0; l < locations.length; l++) try {
//			this.addSearchAttributes(locations);
//			
//			//	make location annotations displayable in result index
//			locations[l].setAttribute(RESULT_INDEX_NAME_ATTRIBUTE, LOCATION_TYPE);
//			locations[l].setAttribute(RESULT_INDEX_LABEL_ATTRIBUTE, COLLECTING_EVENT_INDEX_LABEL);
//			locations[l].setAttribute(RESULT_INDEX_FIELDS_ATTRIBUTE, COLLECTING_EVENT_INDEX_FIELDS);
//			
//		} catch (Exception e) {}
//	}
//	
//	/** @see de.goldenGateSrs.Indexer#addSearchAttributes(de.gamta.Annotation)
//	 */
//	public void addSearchAttributes(Annotation annotation) {
//		
//		if (LOCATION_TYPE.equals(annotation.getType()) || COLLECTING_COUNTRY_ANNOTATION_TYPE.equals(annotation.getType()) || COLLECTING_REGION_ANNOTATION_TYPE.equals(annotation.getType())) {
//			
//			//	add name as attribute in favor of result index
//			String name = annotation.getAttribute(NAME_ATTRIBUTE, "").toString();
//			if (name.length() == 0) {
//				name = AnnotationUtils.escapeForXml(annotation.getValue().replaceAll("\\\"", "'"), true);
//				annotation.setAttribute(NAME_ATTRIBUTE, name);
//			}
//			
//			
//			//	create name, country, and region links for locations
//			if (LOCATION_TYPE.equals(annotation.getType())) {
//				
//				//	create name search link
//				try {
//					annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + NAME_ATTRIBUTE), this.getFullFieldName(NAME_ATTRIBUTE) + "=" + URLEncoder.encode(name, "UTF-8"));
//					annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + NAME_ATTRIBUTE), ("Search locations named '" + name + "'"));
//				} catch (UnsupportedEncodingException e) {}
//				
//				String country = annotation.getAttribute(COUNTRY_ATTRIBUTE, "").toString();
//				if (country.length() != 0) try {
//					annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + COUNTRY_ATTRIBUTE), this.getFullFieldName(COUNTRY_ATTRIBUTE) + "=" + URLEncoder.encode(country, "UTF-8"));
//					annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + COUNTRY_ATTRIBUTE), ("Search for locations in " + country));
//				} catch (UnsupportedEncodingException e) {}
//				
//				String stateProvince = annotation.getAttribute(STATE_PROVINCE_ATTRIBUTE, "").toString();
//				if (stateProvince.length() != 0) try {
//					annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + STATE_PROVINCE_ATTRIBUTE), this.getFullFieldName(STATE_PROVINCE_ATTRIBUTE) + "=" + URLEncoder.encode(stateProvince, "UTF-8"));
//					annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + STATE_PROVINCE_ATTRIBUTE), ("Search for locations in " + stateProvince));
//				} catch (UnsupportedEncodingException e) {}
//			}
//			
//			//	create name link for countries
//			else if (COLLECTING_COUNTRY_ANNOTATION_TYPE.equals(annotation.getType())) {
//				
//				//	create name search link
//				try {
//					annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + NAME_ATTRIBUTE), this.getFullFieldName(COUNTRY_ATTRIBUTE) + "=" + URLEncoder.encode(name, "UTF-8"));
//					annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + NAME_ATTRIBUTE), ("Search for locations in '" + name + "'"));
//				} catch (UnsupportedEncodingException e) {}
//			}
//			
//			//	create name and country links for regions
//			if (COLLECTING_REGION_ANNOTATION_TYPE.equals(annotation.getType())) {
//				
//				//	create name search link
//				try {
//					annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + NAME_ATTRIBUTE), this.getFullFieldName(NAME_ATTRIBUTE) + "=" + URLEncoder.encode(name, "UTF-8"));
//					annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + NAME_ATTRIBUTE), ("Search locations named '" + name + "'"));
//				} catch (UnsupportedEncodingException e) {}
//				
//				String country = annotation.getAttribute(COUNTRY_ATTRIBUTE, "").toString();
//				if (country.length() != 0) try {
//					annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + COUNTRY_ATTRIBUTE), this.getFullFieldName(COUNTRY_ATTRIBUTE) + "=" + URLEncoder.encode(country, "UTF-8"));
//					annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + COUNTRY_ATTRIBUTE), ("Search for locations in " + country));
//				} catch (UnsupportedEncodingException e) {}
//			}
//			
//			
//			//	create links for degrees
//			String longitude = annotation.getAttribute(LONGITUDE_ATTRIBUTE, UNKNOWN_LONGITUDE).toString();
//			String latitude = annotation.getAttribute(LATITUDE_ATTRIBUTE, UNKNOWN_LATITUDE).toString();
//			
//			if ((longitude.length() != 0) && !UNKNOWN_LONGITUDE.equals(longitude)) try {
//				annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + LONGITUDE_ATTRIBUTE), this.getFullFieldName(LONGITUDE_ATTRIBUTE) + "=" + URLEncoder.encode(longitude, "UTF-8"));
//				annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + LONGITUDE_ATTRIBUTE), ("Search for locations around " + longitude + " degrees longitude"));
//			} catch (UnsupportedEncodingException e) {}
//			
//			if ((latitude.length() != 0) && !UNKNOWN_LATITUDE.equals(latitude)) try {
//				annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + LATITUDE_ATTRIBUTE), this.getFullFieldName(LATITUDE_ATTRIBUTE) + "=" + URLEncoder.encode(latitude, "UTF-8"));
//				annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + LATITUDE_ATTRIBUTE), ("Search for locations around " + latitude + " degrees latitude"));
//			} catch (UnsupportedEncodingException e) {}
//			
//			if ((longitude.length() != 0) && !UNKNOWN_LONGITUDE.equals(longitude) && (latitude.length() != 0) && !UNKNOWN_LATITUDE.equals(latitude)) try {
//				annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE), (this.getFullFieldName(LONGITUDE_ATTRIBUTE) + "=" + URLEncoder.encode(longitude, "UTF-8") + "&" + this.getFullFieldName(LATITUDE_ATTRIBUTE) + "=" + URLEncoder.encode(latitude, "UTF-8")));
//				annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE), ("Search for locations around (long " + longitude + " / lat " + latitude + ")"));
//			} catch (UnsupportedEncodingException e) {}
//			
//			
//			//	create link for elevation
//			String elevation = annotation.getAttribute(ELEVATION_ATTRIBUTE, UNKNOWN_ELEVATION).toString();
//			
//			if ((elevation.length() != 0) && !UNKNOWN_ELEVATION.equals(elevation)) try {
//				annotation.setAttribute((SEARCH_LINK_QUERY_ATTRIBUTE + ELEVATION_ATTRIBUTE), this.getFullFieldName(ELEVATION_ATTRIBUTE) + "=" + URLEncoder.encode(elevation, "UTF-8"));
//				annotation.setAttribute((SEARCH_LINK_TITLE_ATTRIBUTE + ELEVATION_ATTRIBUTE), ("Search for locations elevated around " + elevation + " meters"));
//			} catch (UnsupportedEncodingException e) {}
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#getIndexEntries(de.uka.ipd.idaho.goldenGateServer.srs.Query, long[], boolean)
//	 */
//	public IndexResult getIndexEntries(Query query, long[] docNumbers, boolean sort) {
////		return null;
////	}
////
////	/* (non-Javadoc)
////	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#getIndexEntries(de.uka.ipd.idaho.goldenGateServer.srs.Query, int[], boolean)
////	 */
////	public IndexResult getIndexEntries(Query query, int[] docNumbers, boolean sort) {
////		if (!this.indexTableOK) return null;
//		IndexResult ir;
//		
//		HashSet deDuplicator = new HashSet();
//		StringVector docNrs = new StringVector();
//		for (int n = 0; n < docNumbers.length; n++) {
////			if (deDuplicator.add(new Integer(docNumbers[n])))
//			if (deDuplicator.add(new Long(docNumbers[n])))
//				docNrs.addElement("" + docNumbers[n]);
//		}
//		
//		ParsedQuery pq = new ParsedQuery(query);
//		
//		//	process LSID query
//		if (pq.gotLsidQuery()) {
//			System.out.println("  - doing LSID index search");
//			ir = this.getLsidIndexResult(pq, docNrs);
//		}
//		
//		//	process free text query
//		else if (pq.gotFreeTextQuery()) {
//			System.out.println("  - doing free text location index search");
//			ir = this.getFreeTextIndexResult(pq, docNrs);
//		}
//		
//		//	process detail query
//		else {
//			System.out.println("  - doing detail location index search");
//			ir = this.getDetailIndexResult(pq, docNrs);
//		}
//		
//		return ir;
//	}
//	
//	private IndexResult getDetailIndexResult(ParsedQuery query, StringVector docNrs) {
//		
//		String[] columns = {NAME_ATTRIBUTE,
//				COUNTRY_ATTRIBUTE,
//				STATE_PROVINCE_ATTRIBUTE,
//				LONGITUDE_ATTRIBUTE,
//				LATITUDE_ATTRIBUTE,
//				ELEVATION_ATTRIBUTE};
//		
//		return this.getIndexEntries(columns, query.getDetailPredicate(), docNrs);
//	}
//	
//	private IndexResult getLsidIndexResult(ParsedQuery query, StringVector docNrs) {
//		
//		String[] columns = {NAME_ATTRIBUTE,
//				COUNTRY_ATTRIBUTE,
//				STATE_PROVINCE_ATTRIBUTE,
//				LONGITUDE_ATTRIBUTE,
//				LATITUDE_ATTRIBUTE,
//				ELEVATION_ATTRIBUTE};
//		
//		return this.getIndexEntries(columns, query.getLsidPredicate(), docNrs);
//	}
//	
//	private IndexResult getFreeTextIndexResult(ParsedQuery query, StringVector docNrs) {
//		
//		String[] columns = {NAME_ATTRIBUTE,
//				COUNTRY_ATTRIBUTE,
//				STATE_PROVINCE_ATTRIBUTE,
//				LONGITUDE_ATTRIBUTE,
//				LATITUDE_ATTRIBUTE,
//				ELEVATION_ATTRIBUTE};
//		
//		return this.getIndexEntries(columns, query.getFreeTextPredicate(), docNrs);
//	}
//	
//	private IndexResult getIndexEntries(String[] resultAttributes, String where, StringVector docNrs) {
//		if (docNrs.isEmpty()) return null;
//		
//		StringVector sortedResultAttributes = new StringVector();
//		sortedResultAttributes.addElement(NAME_ATTRIBUTE);
//		for (int a = 0; a < resultAttributes.length; a++)
//			sortedResultAttributes.addElementIgnoreDuplicates(resultAttributes[a]);
//		resultAttributes = sortedResultAttributes.toStringArray();
//		
//		StringBuffer query = new StringBuffer("SELECT DISTINCT " + DOC_NUMBER_COLUMN_NAME);
//		for (int c = 0; c < resultAttributes.length; c++) {
//			query.append(", ");
//			query.append(resultAttributes[c]);
//		}
//		query.append(" FROM " + MATERIALS_CITATION_ANNOTATION_TYPE); 
//		query.append(" WHERE " + DOC_NUMBER_COLUMN_NAME + " IN (" + docNrs.concatStrings(", ") + ")");
//		if (where != null)
//			query.append(" AND " + where);
//		query.append(" ORDER BY ");
//		for (int c = 0; c < resultAttributes.length; c++) {
//			if (c != 0) query.append(", ");
//			query.append(resultAttributes[c]);
//		}
//		query.append(";");
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(query.toString());
//			return new LocationIndexResult(resultAttributes, sqr);
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while doing index lookup.");
//			System.out.println("  Query was " + query.toString());
////			if (sqr != null)
////				sqr.close();
//			return null;
//		}
//	}
//	
//	private static class LocationIndexResult extends SqlIndexResult {
//		LocationIndexResult(String[] resultAttributes,  SqlQueryResult sqr) {
//			super(resultAttributes, LOCATION_TYPE, "Location Index", LOCATION_TYPE, sqr);
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.goldenGateServer.srs.AbstractIndexer.SqlIndexResult#decodeResultElement(java.lang.String[])
//		 */
//		protected IndexResultElement decodeResultElement(String[] elementData) {
//			IndexResultElement ire = new IndexResultElement(Integer.parseInt(elementData[0]), this.entryType, ((elementData[1] == null) ? "" : elementData[1]));
//			if (elementData[1] != null)
//				ire.setAttribute(this.resultAttributes[0], elementData[1]);
//			
//			for (int a = 1; a < this.resultAttributes.length; a++)
//				if (elementData[a + 1] != null) {
//					ire.setAttribute(this.resultAttributes[a], elementData[a + 1]);
//					
//					if (LSID_ATTRIBUTE.equals(this.resultAttributes[a])) {
//						String lsidParts = elementData[a + 1];
//						if (lsidParts.startsWith("urn:"))
//							lsidParts = lsidParts.substring(4);
//						if (lsidParts.startsWith("lsid:"))
//							lsidParts = lsidParts.substring(5);
//						
//						String[] codes = lsidParts.split("\\:");
//						if (codes.length == 3) {
//							ire.setAttribute("institutionCode", codes[0]);
//							ire.setAttribute("collectionCode", codes[1]);
//							ire.setAttribute("catalogNumber", codes[2]);
//						}
//					}
//				}
//			
//			return ire;
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#doThesaurusLookup(de.uka.ipd.idaho.goldenGateServer.srs.Query)
//	 */
//	public ThesaurusResult doThesaurusLookup(Query query) {
////		
////		//	are we able to process a query?
////		if (!this.indexTableOK) {
////			System.out.println("MaterialsCitationIndexer: cannot do thesaurus lookup, table messed up.");
////			return null;
////		}
//		ThesaurusResult tr;
//		
//		ParsedQuery pq = new ParsedQuery(query);
//		
//		//	process free text query if given
//		if (pq.gotFreeTextQuery()) {
//			System.out.println("  - doing free text location index search");
//			tr = this.doFreeTextThesaurusLookup(pq);
//		}
//		
//		//	process detail query
//		else if (pq.gotDetailQuery()) {
//			System.out.println("  - doing detail location index search");
//			tr = this.doDetailThesaurusLookup(pq);
//		}
//		
//		//	no query at all
//		else {
//			System.out.println("  - no query given");
//			tr = null;
//		}
//		
//		return tr;
//	}
//	
//	private ThesaurusResult doDetailThesaurusLookup(ParsedQuery query) {
//		
//		String[] columns = {LSID_ATTRIBUTE,
//				NAME_ATTRIBUTE,
//				COUNTRY_ATTRIBUTE,
//				STATE_PROVINCE_ATTRIBUTE,
//				LONGITUDE_ATTRIBUTE,
//				LATITUDE_ATTRIBUTE,
//				ELEVATION_ATTRIBUTE};
//			
//		return this.doThesaurusLookup(columns, query.getDetailPredicate());
//	}
//	
//	private ThesaurusResult doFreeTextThesaurusLookup(ParsedQuery query) {
//		
//		String[] columns = {LSID_ATTRIBUTE,
//				NAME_ATTRIBUTE,
//				COUNTRY_ATTRIBUTE,
//				STATE_PROVINCE_ATTRIBUTE,
//				LONGITUDE_ATTRIBUTE,
//				LATITUDE_ATTRIBUTE,
//				ELEVATION_ATTRIBUTE};
//		
//		return this.doThesaurusLookup(columns, query.getFreeTextPredicate());
//	}
//	
//	private ThesaurusResult doThesaurusLookup(String[] resultFieldNames, String where) {
//		StringBuffer query = new StringBuffer("SELECT DISTINCT ");
//		for (int c = 0; c < resultFieldNames.length; c++) {
//			if (c != 0) query.append(", ");
//			query.append(resultFieldNames[c]);
//		}
//		query.append(" FROM " + MATERIALS_CITATION_ANNOTATION_TYPE); 
//		query.append(" WHERE " + where + ";");
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(query.toString());
//			return new LocationThesaurusResult(resultFieldNames, sqr);
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while doing thesaurus lookup.");
//			System.out.println("  Query was " + query.toString());
////			if (sqr != null)
////				sqr.close();
//			return null;
//		}
//	}
//	
//	private static class LocationThesaurusResult extends SqlThesaurusResult {
//		LocationThesaurusResult(String[] resultFieldNames, SqlQueryResult sqr) {
//			super(resultFieldNames, LOCATION_TYPE, "Location Index", sqr);
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#index(de.uka.ipd.idaho.gamta.QueriableAnnotation, long)
//	 */
//	public void index(QueriableAnnotation doc, long docNr) {
////	}
////
////	/* (non-Javadoc)
////	 * @see de.goldenGateScf.srs.Indexer#index(de.gamta.QueriableAnnotation, int)
////	 */
////	public void index(QueriableAnnotation doc, int docNr) {
////		if (!this.indexTableOK) return;
//		
//		//	keep track of indexed locations
//		Set indexedLocationIDs = new HashSet();
//		
//		//	get materials citations
//		QueriableAnnotation[] materialsCitations = doc.getAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
//		for (int m = 0; m < materialsCitations.length; m++) {
//			
//			//	get locations, region, and country
//			Annotation[] locations = materialsCitations[m].getAnnotations(LOCATION_TYPE);
//			Annotation[] collectingRegions = materialsCitations[m].getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
//			Annotation[] collectingCountries = materialsCitations[m].getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
//			
//			//	get country
//			String collectingCountry = null;
//			if (collectingCountries.length != 0)
//				collectingCountry = collectingCountries[0].getValue();
////			if (collectingCountry == null) {
////				Object countryObject = materialsCitations[m].getAttribute(COUNTRY_ATTRIBUTE);
////				if (countryObject != null) {
////					collectingCountry = countryObject.toString().trim();
////					if (this.countryHandler != null)
////						collectingCountry = this.countryHandler.getEnglishName(collectingCountry);
////					//	TODOne normalize country name to American English
////				}
////			}
//			if (collectingCountry == null)
//				collectingCountry = ((String) materialsCitations[m].getAttribute(COUNTRY_ATTRIBUTE));
//			if ((collectingCountry != null) && (this.countryHandler != null))
//				collectingCountry = this.countryHandler.getEnglishName(collectingCountry);
//			
//			//	get region
//			String collectingRegion = null;
//			if (collectingRegions.length != 0)
//				collectingRegion = collectingRegions[0].getValue();
////			if (collectingRegion == null) {
////				Object stateProvinceObject = materialsCitations[m].getAttribute(STATE_PROVINCE_ATTRIBUTE);
////				if (stateProvinceObject != null)
////					collectingRegion = stateProvinceObject.toString().trim();
////			}
//			if (collectingRegion == null)
//				collectingRegion = ((String) materialsCitations[m].getAttribute(STATE_PROVINCE_ATTRIBUTE));
//			
//			//	get collection information
//			Annotation[] typeStatuss = materialsCitations[m].getAnnotations(TYPE_STATUS);
//			Annotation[] collectionCodes = materialsCitations[m].getAnnotations(COLLECTION_CODE);
//			Annotation[] specimenCodes = materialsCitations[m].getAnnotations(SPECIMEN_CODE);
//			
//			//	get type status
//			String typeStatus = null;
//			if (typeStatuss.length != 0)
//				typeStatus = typeStatuss[0].getValue();
//			if (typeStatus == null)
//				typeStatus = ((String) materialsCitations[m].getAttribute(TYPE_STATUS));
//			
//			//	get type status
//			String collectionCode = null;
//			if (collectionCodes.length != 0)
//				collectionCode = collectionCodes[0].getValue();
//			if (collectionCode == null)
//				collectionCode = ((String) materialsCitations[m].getAttribute(COLLECTION_CODE));
//			
//			//	get specimen code
//			String specimenCode = null;
//			if (specimenCodes.length != 0)
//				specimenCode = specimenCodes[0].getValue();
//			if (specimenCode == null)
//				specimenCode = ((String) materialsCitations[m].getAttribute(SPECIMEN_CODE));
//			
//			//	get specimen count
//			int specimenCount = 1;
//			try {
//				specimenCount = Integer.parseInt((String) materialsCitations[m].getAttribute(SPECIMEN_COUNT, "1"));
//			} catch (NumberFormatException nfe) {}
//			
//			
//			//	no locations given
//			if (locations.length == 0) {
//				
//				//	no region given
//				if (collectingRegions.length == 0) {
//					
//					//	no country given, use attributes of materials citation
//					if (collectingCountries.length == 0) {
//						String lsid = ((String) materialsCitations[m].getAttribute(LSID_ATTRIBUTE, "")).trim();
//						
//						String location = ((String) materialsCitations[m].getAttribute(LOCATION_TYPE, "")).trim();
//						String country = ((String) materialsCitations[m].getAttribute(COUNTRY_ATTRIBUTE, "")).trim();
//						String stateProvince = ((String) materialsCitations[m].getAttribute(STATE_PROVINCE_ATTRIBUTE, "")).trim();
//						
//						String longStr = ((String) materialsCitations[m].getAttribute(LONGITUDE_ATTRIBUTE, UNKNOWN_LONGITUDE)).trim();
//						String latStr = ((String) materialsCitations[m].getAttribute(LATITUDE_ATTRIBUTE, UNKNOWN_LATITUDE)).trim();
//						String elevStr = ((String) materialsCitations[m].getAttribute(ELEVATION_ATTRIBUTE, UNKNOWN_ELEVATION)).trim();
//						
//						this.writeIndexEntry(docNr, lsid, location, country, stateProvince, typeStatus, collectionCode, specimenCode, specimenCount, longStr, latStr, elevStr);
//					}
//					
//					//	collecting country given, use for indexing
//					else {
//						for (int c = 0; c < collectingCountries.length; c++) {
//							String lsid = ((String) collectingCountries[c].getAttribute(LSID_ATTRIBUTE, "")).trim();
//							if (lsid.length() == 0)
//								lsid = ((String) materialsCitations[m].getAttribute(LSID_ATTRIBUTE, "")).trim();
//							
//							String location = ((String) materialsCitations[m].getAttribute(LOCATION_TYPE, "")).trim();
//							
//							String country = ((String) collectingCountries[c].getAttribute(NAME_ATTRIBUTE, "")).trim();
//							if (country.length() == 0) country = collectingCountries[c].getValue();
//							
//							String stateProvince = ((String) materialsCitations[m].getAttribute(STATE_PROVINCE_ATTRIBUTE, "")).trim();
//							
//							String longStr = ((String) collectingCountries[c].getAttribute(LONGITUDE_ATTRIBUTE, "")).trim();
//							if (longStr.length() == 0)
//								longStr = ((String) materialsCitations[m].getAttribute(LONGITUDE_ATTRIBUTE, UNKNOWN_LONGITUDE)).trim();
//							
//							String latStr = ((String) collectingCountries[c].getAttribute(LATITUDE_ATTRIBUTE, "")).trim();
//							if (latStr.length() == 0)
//								latStr = ((String) materialsCitations[m].getAttribute(LATITUDE_ATTRIBUTE, UNKNOWN_LATITUDE)).trim();
//							
//							String elevStr = ((String) collectingCountries[c].getAttribute(ELEVATION_ATTRIBUTE, ""));
//							if (elevStr.length() == 0)
//								elevStr = ((String) materialsCitations[m].getAttribute(ELEVATION_ATTRIBUTE, UNKNOWN_ELEVATION)).trim();
//							
//							this.writeIndexEntry(docNr, lsid, location, country, stateProvince, typeStatus, collectionCode, specimenCode, specimenCount, longStr, latStr, elevStr);
//						}
//					}
//				}
//				
//				//	collecting region given, use for indexing
//				else {
//					for (int r = 0; r < collectingRegions.length; r++) {
//						String lsid = ((String) collectingRegions[r].getAttribute(LSID_ATTRIBUTE, "")).trim();
//						if (lsid.length() == 0)
//							lsid = ((String) materialsCitations[m].getAttribute(LSID_ATTRIBUTE, "")).trim();
//						
//						String location = ((String) materialsCitations[m].getAttribute(LOCATION_TYPE, "")).trim();
//						
//						String country = collectingCountry;
//						if (country == null) {
//							country = ((String) collectingRegions[r].getAttribute(COUNTRY_ATTRIBUTE, "")).trim();
//							if (this.countryHandler != null)
//								country = this.countryHandler.getEnglishName(country);
//							//	TODOne normalize country name to American English
//						}
//						
//						String stateProvince = ((String) collectingRegions[r].getAttribute(NAME_ATTRIBUTE, "")).trim();
//						if (stateProvince.length() == 0) stateProvince = collectingRegions[r].getValue();
//						
//						String longStr = ((String) collectingRegions[r].getAttribute(LONGITUDE_ATTRIBUTE, "")).trim();
//						if (longStr.length() == 0)
//							longStr = ((String) materialsCitations[m].getAttribute(LONGITUDE_ATTRIBUTE, UNKNOWN_LONGITUDE)).trim();
//						
//						String latStr = ((String) collectingRegions[r].getAttribute(LATITUDE_ATTRIBUTE, "")).trim();
//						if (latStr.length() == 0)
//							latStr = ((String) materialsCitations[m].getAttribute(LATITUDE_ATTRIBUTE, UNKNOWN_LATITUDE)).trim();
//						
//						String elevStr = ((String) collectingRegions[r].getAttribute(ELEVATION_ATTRIBUTE, ""));
//						if (elevStr.length() == 0)
//							elevStr = ((String) materialsCitations[m].getAttribute(ELEVATION_ATTRIBUTE, UNKNOWN_ELEVATION)).trim();
//						
//						this.writeIndexEntry(docNr, lsid, location, country, stateProvince, typeStatus, collectionCode, specimenCode, specimenCount, longStr, latStr, elevStr);
//					}
//				}
//			}
//			
//			//	locations given, use for indexing
//			else {
//				for (int l = 0; l < locations.length; l++) {
//					String lsid = ((String) locations[l].getAttribute(LSID_ATTRIBUTE, "")).trim();
//					if (lsid.length() == 0)
//						lsid = ((String) materialsCitations[m].getAttribute(LSID_ATTRIBUTE, "")).trim();
//					
//					String location = ((String) locations[l].getAttribute(NAME_ATTRIBUTE, "")).trim();
//					if (location.length() == 0) location = locations[l].getValue();
//					
//					String country = collectingCountry;
//					if (country == null) {
//						country = ((String) locations[l].getAttribute(COUNTRY_ATTRIBUTE, "")).trim();
//						if (this.countryHandler != null)
//							country = this.countryHandler.getEnglishName(country);
//					}
//					
//					String stateProvince = collectingRegion;
//					if (stateProvince == null)
//						stateProvince = ((String) locations[l].getAttribute(STATE_PROVINCE_ATTRIBUTE, "")).trim();
//					
//					String longStr = ((String) locations[l].getAttribute(LONGITUDE_ATTRIBUTE, "")).trim();
//					if (longStr.length() == 0)
//						longStr = ((String) materialsCitations[m].getAttribute(LONGITUDE_ATTRIBUTE, UNKNOWN_LONGITUDE)).trim();
//					
//					String latStr = ((String) locations[l].getAttribute(LATITUDE_ATTRIBUTE, "")).trim();
//					if (latStr.length() == 0)
//						latStr = ((String) materialsCitations[m].getAttribute(LATITUDE_ATTRIBUTE, UNKNOWN_LATITUDE)).trim();
//					
//					String elevStr = ((String) locations[l].getAttribute(ELEVATION_ATTRIBUTE, ""));
//					if (elevStr.length() == 0)
//						elevStr = ((String) materialsCitations[m].getAttribute(ELEVATION_ATTRIBUTE, UNKNOWN_ELEVATION)).trim();
//					
//					this.writeIndexEntry(docNr, lsid, location, country, stateProvince, typeStatus, collectionCode, specimenCode, specimenCount, longStr, latStr, elevStr);
//					
//					indexedLocationIDs.add(locations[l].getAnnotationID());
//				}
//			}
//		}
//		
//		//	get locations
//		Annotation[] locations = doc.getAnnotations(LOCATION_TYPE);
//		for (int l = 0; l < locations.length; l++) {
//			
//			//	avoid duplicate index entries
//			if (indexedLocationIDs.add(locations[l].getAnnotationID())) {
//				
//				//	get attributes
//				String lsid = ((String) locations [l].getAttribute(LSID_ATTRIBUTE, "")).trim();
//				if (lsid.length() > LSID_LENGTH) lsid = lsid.substring(0, LSID_LENGTH);
//				
//				String location = ((String) locations [l].getAttribute(NAME_ATTRIBUTE, "")).trim();
//				if (location.length() == 0) location = locations[l].getValue();
//				if (location.length() > NAME_LENGTH) location = location.substring(0, NAME_LENGTH);
//				
//				String country = ((String) locations[l].getAttribute(COUNTRY_ATTRIBUTE, "")).trim();
//				if (this.countryHandler != null)
//					country = this.countryHandler.getEnglishName(country);
//				if (country.length() > COUNTRY_LENGTH)
//					country = country.substring(0, COUNTRY_LENGTH);
//				
//				String stateProvince = ((String) locations[l].getAttribute(STATE_PROVINCE_ATTRIBUTE, "")).trim();
//				if (stateProvince.length() > STATE_PROVINCE_LENGTH)
//					stateProvince = stateProvince.substring(0, STATE_PROVINCE_LENGTH);
//				
//				String longStr = ((String) locations[l].getAttribute(LONGITUDE_ATTRIBUTE, UNKNOWN_LONGITUDE)).trim();
//				
//				String latStr = ((String) locations[l].getAttribute(LATITUDE_ATTRIBUTE, UNKNOWN_LATITUDE)).trim();
//				
//				String elevStr = ((String) locations[l].getAttribute(ELEVATION_ATTRIBUTE, UNKNOWN_ELEVATION));
//				
//				//	write index table entry
//				this.writeIndexEntry(docNr, lsid, location, country, stateProvince, "", "", "", 1, longStr, latStr, elevStr);
//			}
//		}
//	}
//	
////	private void writeIndexEntry(int docNr, String lsid, String name, String country, String stateProvince, String typeStatus, String collectionCode, String specimenCode, String longString, String latString, String elevString) {
//	private void writeIndexEntry(long docNr, String lsid, String name, String country, String stateProvince, String typeStatus, String collectionCode, String specimenCode, int specimenCount, String longString, String latString, String elevString) {
//		
//		//	start column strings
//		StringBuffer columns = new StringBuffer(DOC_NUMBER_COLUMN_NAME);
//		StringBuffer values = new StringBuffer("" + docNr);
//		
//		//	add attributes
//		if (lsid.length() > LSID_LENGTH)
//			lsid = lsid.substring(0, LSID_LENGTH);
//		columns.append(", " + LSID_ATTRIBUTE);
//		values.append(", '" + EasyIO.sqlEscape(lsid) + "'");
//		
//		columns.append(", " + NAME_ATTRIBUTE);
//		if (name.length() > NAME_LENGTH)
//			name = name.substring(0, NAME_LENGTH);
//		values.append(", '" + EasyIO.sqlEscape(name) + "'");
//		
//		columns.append(", " + NAME_ATTRIBUTE + "Search");
//		String sName = prepareSearchString(name.toLowerCase());
//		if (sName.length() > NAME_LENGTH)
//			sName = sName.substring(0, NAME_LENGTH);
//		values.append(", '" + EasyIO.sqlEscape(sName) + "'");
//		
//		columns.append(", " + COUNTRY_ATTRIBUTE);
//		if (country.length() > COUNTRY_LENGTH)
//			country = country.substring(0, COUNTRY_LENGTH);
//		values.append(", '" + EasyIO.sqlEscape(country) + "'");
//		
//		columns.append(", " + COUNTRY_ATTRIBUTE + "Search");
//		String sCountry = prepareSearchString(country.toLowerCase());
//		if (sCountry.length() > COUNTRY_LENGTH)
//			sCountry = sCountry.substring(0, COUNTRY_LENGTH);
//		values.append(", '" + EasyIO.sqlEscape(sCountry) + "'");
//		
//		columns.append(", " + STATE_PROVINCE_ATTRIBUTE);
//		if (stateProvince.length() > STATE_PROVINCE_LENGTH)
//			stateProvince = stateProvince.substring(0, STATE_PROVINCE_LENGTH);
//		values.append(", '" + EasyIO.sqlEscape(stateProvince) + "'");
//		
//		columns.append(", " + STATE_PROVINCE_ATTRIBUTE + "Search");
//		String sStateProvince = prepareSearchString(stateProvince.toLowerCase());
//		if (sStateProvince.length() > STATE_PROVINCE_LENGTH)
//			sStateProvince = sStateProvince.substring(0, STATE_PROVINCE_LENGTH);
//		values.append(", '" + EasyIO.sqlEscape(sStateProvince) + "'");
//		
//		columns.append(", " + TYPE_STATUS);
//		if (typeStatus.length() > TYPE_STATUS_LENGTH)
//			typeStatus = typeStatus.substring(0, TYPE_STATUS_LENGTH);
//		typeStatus = Gamta.capitalize(typeStatus);
//		values.append(", '" + EasyIO.sqlEscape(typeStatus) + "'");
//		
//		columns.append(", " + COLLECTION_CODE);
//		if (collectionCode.length() > COLLECTION_CODE_LENGTH)
//			collectionCode = collectionCode.substring(0, COLLECTION_CODE_LENGTH);
//		collectionCode = collectionCode.toUpperCase();
//		values.append(", '" + EasyIO.sqlEscape(collectionCode) + "'");
//		
//		columns.append(", " + SPECIMEN_CODE);
//		if (specimenCode.length() > SPECIMEN_CODE_LENGTH)
//			specimenCode = specimenCode.substring(0, SPECIMEN_CODE_LENGTH);
//		specimenCode = specimenCode.toUpperCase();
//		values.append(", '" + EasyIO.sqlEscape(specimenCode) + "'");
//		
//		columns.append(", " + SPECIMEN_COUNT);
//		values.append(", " + specimenCount + "");
//		
//		columns.append(", " + LONGITUDE_ATTRIBUTE);
//		try {
//			double longitude = Double.parseDouble(longString);
//			values.append(", " + longitude);
//		} catch (NumberFormatException nfe) {
//			values.append(", " + UNKNOWN_LONGITUDE);
//		}
//		
//		columns.append(", " + LATITUDE_ATTRIBUTE);
//		try {
//			double latitude = Double.parseDouble(latString);
//			values.append(", " + latitude);
//		} catch (NumberFormatException nfe) {
//			values.append(", " + UNKNOWN_LATITUDE);
//		}
//		
//		columns.append(", " + ELEVATION_ATTRIBUTE);
//		try {
//			int elevation = (int) Double.parseDouble(elevString);
//			values.append(", " + elevation);
//		} catch (NumberFormatException nfe) {
//			values.append(", " + UNKNOWN_ELEVATION);
//		}
//		
//		//	write index table entry
//		String query = ("INSERT INTO " + MATERIALS_CITATION_ANNOTATION_TYPE + " (" + columns + ") VALUES (" + values + ");");
//		try {
//			this.io.executeUpdateQuery(query);
//		}
//		catch (SQLException sqle) {
//			System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while indexing document.");
//			System.out.println("  Query was " + query);
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#deleteDocument(long)
//	 */
//	public void deleteDocument(long docNr) {
////	}
////
////	/** @see de.goldenGateSrs.Indexer#deleteDocument(java.lang.String)
////	 */
////	public void deleteDocument(int docNr) {
////		if (this.indexTableOK) {
//			String query = ("DELETE FROM " + MATERIALS_CITATION_ANNOTATION_TYPE + " WHERE " + DOC_NUMBER_COLUMN_NAME + "=" + docNr + ";");
//			try {
//				this.io.executeUpdateQuery(query);
//			}
//			catch (SQLException sqle) {
//				System.out.println("MaterialsCitationIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while deleting document.");
//				System.out.println("  Query was " + query);
//			}
////		}
//	}
//	
//	private static final int[] degreeCircleWidths = {1, 2, 3, 5, 10};
//	private static final int[] elevationCircleWidths = {10, 20, 30, 50, 100, 200, 300, 500, 1000};
//	
//	/** @see de.goldenGateSrs.AbstractIndexer#getFieldGroup()
//	 */
//	protected SearchFieldGroup getFieldGroup() {
//		SearchFieldRow locationSfr = new SearchFieldRow("Location Based Search");
//		
//		locationSfr.addField(new SearchField(LOCATION_TYPE, "Location Text", 2));
//		locationSfr.addField(new SearchField(COUNTRY_ATTRIBUTE, "Country"));
//		locationSfr.addField(new SearchField(STATE_PROVINCE_ATTRIBUTE, "State / Province"));
//		locationSfr.addField(new SearchField(NAME_ATTRIBUTE, "Location Name"));
//		
//		
//		SearchFieldRow textSfr = new SearchFieldRow("Textual Search");
//		SearchField typeStatusField = new SearchField(TYPE_STATUS, "Type Status", ALL_TYPE_STATUS, SearchField.SELECT_TYPE);
//		for (int t = 0; t < this.typeStatus.length; t++)
//			typeStatusField.addOption(this.typeStatus[t]);
//		textSfr.addField(typeStatusField);
//		
//		textSfr.addField(new SearchField(COLLECTION_CODE, "Collection Code", 1));
//		textSfr.addField(new SearchField(SPECIMEN_CODE, "Specimen Code", 1));
//		textSfr.addField(new SearchField(LSID_ATTRIBUTE, "LSID", 2));
//		
//		
//		SearchFieldRow numericalSfr = new SearchFieldRow("Numerical Search");
//		
//		numericalSfr.addField(new SearchField(LONGITUDE_ATTRIBUTE, "Longitude"));
//		numericalSfr.addField(new SearchField(LATITUDE_ATTRIBUTE, "Latitude"));
//		SearchField degreeCircleField = new SearchField(DEGREE_CIRCLE, "Long/Lat Circle", "1", SearchField.SELECT_TYPE);
//		degreeCircleField.addOption("Exact", "0");
//		for (int d = 0; d < degreeCircleWidths.length; d++)
//			degreeCircleField.addOption((degreeCircleWidths[d] + " degree" + ((degreeCircleWidths[d] == 1) ? "" : "s")), ("" + degreeCircleWidths[d]));
//		numericalSfr.addField(degreeCircleField);
//		
//		numericalSfr.addField(new SearchField(ELEVATION_ATTRIBUTE, "Elevation"));
//		SearchField elevationCircleField = new SearchField(ELEVATION_CIRCLE, "Elevation Circle", "100", SearchField.SELECT_TYPE);
//		elevationCircleField.addOption("Exact", "0");
//		for (int e = 0; e < elevationCircleWidths.length; e++)
//			elevationCircleField.addOption((elevationCircleWidths[e] + " meter" + ((elevationCircleWidths[e] == 1) ? "" : "s")), ("" + elevationCircleWidths[e]));
//		numericalSfr.addField(elevationCircleField);
//		
//		SearchFieldGroup sfg = new SearchFieldGroup(this.getIndexName(), "Collecting Location Index", "Use these fields to search the materials citation index.", "Materials Citation");
//		sfg.addFieldRow(locationSfr);
//		sfg.addFieldRow(textSfr);
//		sfg.addFieldRow(numericalSfr);
//		
//		return sfg;
//	}
////	
////	/** @see de.goldenGateSrs.Indexer#isQuoted(java.lang.String)
////	 */
////	public boolean isQuoted(String fieldName) {
////		return (NAME_ATTRIBUTE.equals(fieldName) || COUNTRY_ATTRIBUTE.equals(fieldName));
////	}
////	
////	/** @see de.goldenGateSrs.Indexer#getLength(java.lang.String)
////	 */
////	public int getLength(String fieldName) {
////		if (NAME_ATTRIBUTE.equals(fieldName)) return NAME_LENGTH;
////		else if (COUNTRY_ATTRIBUTE.equals(fieldName)) return COUNTRY_LENGTH;
////		else if (TYPE_STATUS.equals(fieldName)) return TYPE_STATUS_LENGTH;
////		else if (COLLECTION_CODE.equals(fieldName)) return COLLECTION_CODE_LENGTH;
////		else return 0;
////	}
//}