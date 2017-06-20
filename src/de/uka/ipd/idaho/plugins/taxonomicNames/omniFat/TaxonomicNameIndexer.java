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
//package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat;
//
//
//import java.net.URLEncoder;
//import java.sql.SQLException;
//import java.util.HashSet;
//
//import de.uka.ipd.idaho.easyIO.EasyIO;
//import de.uka.ipd.idaho.easyIO.IoProvider;
//import de.uka.ipd.idaho.easyIO.SqlQueryResult;
//import de.uka.ipd.idaho.easyIO.sql.TableDefinition;
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.QueriableAnnotation;
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
//import de.uka.ipd.idaho.gamta.util.gPath.GPath;
//import de.uka.ipd.idaho.goldenGateServer.srs.AbstractIndexer;
//import de.uka.ipd.idaho.goldenGateServer.srs.Query;
//import de.uka.ipd.idaho.goldenGateServer.srs.QueryResult;
//import de.uka.ipd.idaho.goldenGateServer.srs.QueryResultElement;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.IndexResult;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.IndexResultElement;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.ThesaurusResult;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.ThesaurusResultElement;
//import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
//import de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.Rank;
//import de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.RankGroup;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//
///**
// * Indexer for taxonomic names. This indexer supports searching by (a) LSID, (b)
// * free text taxon name, and (c) taxon name details, namely the meaningful parts
// * from the genus epithet down to the variety epithet. In addition, it will
// * remember if a taxon name resides in the nomenclatorial part of a treatment,
// * or in the treatment body.
// * 
// * @author sautter
// */
//public class TaxonomicNameIndexer extends AbstractIndexer implements TaxonomicNameConstants {
//	
//	private static final String TAXONOMIC_NAME_EPITHET_TABLE_NAME = TAXONOMIC_NAME_ANNOTATION_TYPE + "Epithet";
//	private static final String RANK_GROUP_NUMBER_COLUMN_NAME = "RankGroupNumber";
//	private static final String EPITHET_COLUMN_NAME = "Epithet";
//	
//	private static final String IS_NOMENCLATURE_NAME_ATTRIBUTE = "isNomenclature";
//	private static final char NOMENCLATURE_NAME_MARKER = 'N';
//	
//	private static final String EXACT_MATCH_OPTION = "exactMatch";
//	
//	private static final String DOCUMENT_POSITION_ATTRIBUTE = "documentPosition";
//	
//	private static final String LSID_SOURCE_ATTRIBUTE = "lsidSource";
//	private static final String LSID_NAME_ATTRIBUTE = "lsidName";
//	
//	private static final String STATUS_ATTRIBUTE = "status";
//	
//	private static final int LSID_LENGTH = 128;
//	private static final int LSID_SOURCE_LENGTH = 5;
//	private static final int LSID_NAME_LENGTH = 255;
//	private static final int NAME_LENGTH = 255;
//	private static final int NAME_PART_LENGTH = 36;
//	
//	private static final String TAXONOMIC_NAME_INDEX_LABEL = "Taxonomic Name Index";
//	private static final String RANK_WEIGHT_ATTRIBUTE = "rankWeight";
//	
//	private IoProvider io;
//	
//	private OmniFAT omniFat;
//	private RankGroup[] rankGroups;
//	private int[] rankGroupLengths;
//	private Rank[] ranks;
//	
//	private String taxonomicNameIndexFields = LSID_ATTRIBUTE;
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.Indexer#getIndexName()
//	 */
//	public String getIndexName() {
//		return TAXONOMIC_NAME_ANNOTATION_TYPE;
//	}
//	
//	/** @see de.goldenGateSrs.AbstractIndexer#init()
//	 */
//	public void init() {
//		
//		//	load OmniFAT instance
//		this.omniFat = OmniFAT.getDefaultInstance(new AnalyzerDataProviderFileBased(this.dataPath));
//		if (this.omniFat == null)
//			throw new RuntimeException("TaxonomicNameIndexer cannot work without OmniFAT instance.");
//		
//		//	get detail data
//		this.rankGroups = this.omniFat.getRankGroups();
//		this.rankGroupLengths = new int[this.rankGroups.length];
//		for (int rg = 0; rg < this.rankGroups.length; rg++) {
//			int rgLength = 0;
//			Rank[] rgRanks = this.rankGroups[rg].getRanks();
//			for (int r = 0; r < rgRanks.length; r++)
//				rgLength += (1 + NAME_PART_LENGTH);
//			this.rankGroupLengths[rg] = rgLength;
//		}
//		this.ranks = this.omniFat.getRanks();
//		this.taxonomicNameIndexFields = LSID_ATTRIBUTE;
//		for (int r = 0; r < this.ranks.length; r++)
//			this.taxonomicNameIndexFields += (" " + this.ranks[r].getName());
//		
//		//	get and check database connection
//		this.io = this.host.getIoProvider();
//		if (!this.io.isJdbcAvailable())
//			throw new RuntimeException("TaxonomicNameIndexer cannot work without database access.");
//		
//		//	assemble index definition
//		TableDefinition td = new TableDefinition(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		td.addColumn(DOC_NUMBER_COLUMN);
//		td.addColumn(LSID_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, LSID_LENGTH);
//		td.addColumn(LSID_SOURCE_ATTRIBUTE, TableDefinition.CHAR_DATATYPE, LSID_SOURCE_LENGTH);
//		td.addColumn(LSID_NAME_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, LSID_NAME_LENGTH);
//		td.addColumn(TAXONOMIC_NAME_ANNOTATION_TYPE, TableDefinition.VARCHAR_DATATYPE, NAME_LENGTH);
//		for (int rg = 0; rg < this.rankGroups.length; rg++)
//			td.addColumn(this.rankGroups[rg].getName(), TableDefinition.VARCHAR_DATATYPE, this.rankGroupLengths[rg]);
//		td.addColumn(STATUS_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, NAME_PART_LENGTH);
//		td.addColumn(RANK_WEIGHT_ATTRIBUTE, TableDefinition.INT_DATATYPE, 0);
//		td.addColumn(IS_NOMENCLATURE_NAME_ATTRIBUTE, TableDefinition.CHAR_DATATYPE, 1);
//		td.addColumn(DOCUMENT_POSITION_ATTRIBUTE, TableDefinition.INT_DATATYPE, 0);
//		if (!this.io.ensureTable(td, true))
//			throw new RuntimeException("TaxonomicNameIndexer cannot work without database access.");
//		
//		//	create column indexes
//		this.io.indexColumn(TAXONOMIC_NAME_ANNOTATION_TYPE, DOC_NUMBER_COLUMN_NAME);
//		this.io.indexColumn(TAXONOMIC_NAME_ANNOTATION_TYPE, LSID_ATTRIBUTE);
//		this.io.indexColumn(TAXONOMIC_NAME_ANNOTATION_TYPE, STATUS_ATTRIBUTE);
//		this.io.indexColumn(TAXONOMIC_NAME_ANNOTATION_TYPE, IS_NOMENCLATURE_NAME_ATTRIBUTE);
//		//	no use indexing epithets here, main table is queried with leading wildcards anyway
//		
//		//	assemble epithet index definition
//		TableDefinition etd = new TableDefinition(TAXONOMIC_NAME_EPITHET_TABLE_NAME);
//		etd.addColumn(DOC_NUMBER_COLUMN);
//		etd.addColumn(RANK_WEIGHT_ATTRIBUTE, TableDefinition.INT_DATATYPE, 0);
//		etd.addColumn(RANK_GROUP_NUMBER_COLUMN_NAME, TableDefinition.INT_DATATYPE, 0);
//		etd.addColumn(EPITHET_COLUMN_NAME, TableDefinition.VARCHAR_DATATYPE, NAME_PART_LENGTH);
//		etd.addColumn(IS_NOMENCLATURE_NAME_ATTRIBUTE, TableDefinition.CHAR_DATATYPE, 1);
//		if (!this.io.ensureTable(etd, true))
//			throw new RuntimeException("TaxonomicNameIndexer cannot work without database access.");
//		
//		//	create column indexes
//		this.io.indexColumn(TAXONOMIC_NAME_EPITHET_TABLE_NAME, RANK_GROUP_NUMBER_COLUMN_NAME);
//		this.io.indexColumn(TAXONOMIC_NAME_EPITHET_TABLE_NAME, EPITHET_COLUMN_NAME);
//		this.io.indexColumn(TAXONOMIC_NAME_EPITHET_TABLE_NAME, IS_NOMENCLATURE_NAME_ATTRIBUTE);
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
//	/** @see de.goldenGateSrs.Indexer#processQuery(de.goldenGateSrs.Query)
//	 */
//	public QueryResult processQuery(Query query) {
//		System.out.println("TaxonomicNameIndexer: processing query ... ");
//		
//		//	check if search restricted to nomenclature
//		String inNomenclature = query.getValue(IS_NOMENCLATURE_NAME_ATTRIBUTE);
//		boolean restrictToNomenclature = ((inNomenclature != null) && (inNomenclature.trim().length() != 0));
//		
//		//	check if search restricted to exact matches
//		String exactMatch = query.getValue(EXACT_MATCH_OPTION);
//		boolean restrictToExactMatch = ((exactMatch != null) && (exactMatch.trim().length() != 0));
//		
//		//	process free text query
//		String name = query.getValue(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		if ((name != null) && (name.length() != 0)) {
//			System.out.println("  - doing free text search");
//			QueryResult qr = this.getNameResult(name, restrictToNomenclature, restrictToExactMatch);
//			if (qr == null) System.out.println("  - got null result");
//			else System.out.println("  - got " + qr.size() + " hits");
//			System.out.println("TaxonomicNameIndexer: query processed");
//			return qr;
//		}
//		
//		//	process LSID query
//		String lsid = query.getValue(LSID_ATTRIBUTE);
//		if ((lsid != null) && (lsid.trim().length() != 0)) {
//			System.out.println("  - doing LSID search");
//			QueryResult qr = this.getLsidResult(lsid, restrictToNomenclature);
//			if (qr == null) System.out.println("  - got null result");
//			else System.out.println("  - got " + qr.size() + " hits");
//			System.out.println("TaxonomicNameIndexer: query processed");
//			return qr;
//		}
//		
//		//	get name part results
//		System.out.println("  - doing name part search");
//		QueryResult result = null;
//		for (int rg = 0; rg < this.rankGroups.length; rg++) {
//			String rgValue = query.getValue(this.rankGroups[rg].getName(), "").trim();
//			if (rgValue.length() > this.rankGroupLengths[rg])
//				rgValue = rgValue.substring(0, this.rankGroupLengths[rg]);
//			if (rgValue.length() == 0)
//				continue;
//			
//			QueryResult rgResult = null;
//			String[] rgValueParts = rgValue.split("\\s++");
//			for (int p = 0; p < rgValueParts.length; p++) {
//				QueryResult rgPartResult = this.getRankGroupPartResult(rg, rgValueParts[p], 1, restrictToNomenclature);
//				if (rgPartResult == null)
//					continue;
//				if (rgResult == null)
//					rgResult = rgPartResult;
//				else rgResult = rgResult.merge(rgPartResult, (restrictToExactMatch ? QueryResult.USE_MIN : QueryResult.USE_AVERAGE), 0);
//			}
//			if (rgResult == null)
//				continue;
//			
//			System.out.println("  - got " + rgResult.size() + " hits in " + this.rankGroups[rg].getName() + " result");
//			if (result == null)
//				result = rgResult;
//			else {
//				result = result.merge(rgResult, (restrictToExactMatch ? QueryResult.USE_MIN : QueryResult.USE_AVERAGE), 0);
//				System.out.println("  - got " + result.size() + " hits in merged result");
//			}
//		}
//		
//		//	sort and return result
//		if (result != null) {
//			System.out.println("  - got " + result.size() + " hits in raw result");
//			result.sortByRelevance(true);
//			result.pruneByRelevance(Double.MIN_VALUE);
//			System.out.println("  - got " + result.size() + " hits in final result");
//		}
//		System.out.println("TaxonomicNameIndexer: query processed");
//		return result;
//	}
//	
//	private QueryResult getRankGroupPartResult(int rankGroupNumber, String rgPredicate, int rankWeight, boolean restrictToNomenclature) {
//		String query = "SELECT " + DOC_NUMBER_COLUMN_NAME + ", " + RANK_WEIGHT_ATTRIBUTE + 
//			" FROM " + TAXONOMIC_NAME_EPITHET_TABLE_NAME +
//			" WHERE " + EPITHET_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(rgPredicate) + "%'" +
//				" AND " + RANK_GROUP_NUMBER_COLUMN_NAME + " = " + rankGroupNumber + 
//				(restrictToNomenclature ? (" AND " + IS_NOMENCLATURE_NAME_ATTRIBUTE + " = '" + NOMENCLATURE_NAME_MARKER + "'") : "") + 
//			";";
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(query);
//			
//			QueryResult result = new QueryResult();
//			double relevance;
//			while (sqr.next()) {
//				
//				//	read data
//				String docNr = sqr.getString(0);
//				double resRankWeight = Double.parseDouble(sqr.getString(1));
//				if (docNr != null) {
//					
//					//	compute RSV for each part
//					relevance = (((double) rankWeight) / resRankWeight);
//					
//					//	store result if relevance high enough
////					if (relevance > 0.0) result.addResultElement(new QueryResultElement(Integer.parseInt(docNr), relevance));
//					if (relevance > 0.0)
//						result.addResultElement(new QueryResultElement(Long.parseLong(docNr), relevance));
//				}
//			}
//			return result;
//		}
//		catch (SQLException sqle) {
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while searching.");
//			System.out.println("  Query was " + query);
//			return null;
//		}
//		finally {
//			if (sqr != null)
//				sqr.close();
//		}
//	}
//	
//	private QueryResult getLsidResult(String lsid, boolean restrictToNomenclature) {
//		lsid = lsid.trim();
//		if (lsid.length() > LSID_LENGTH) lsid = lsid.substring(0, LSID_LENGTH);
//		
//		String lsidQuery = "SELECT DISTINCT " + DOC_NUMBER_COLUMN_NAME +
//			" FROM " + TAXONOMIC_NAME_ANNOTATION_TYPE +
//			" WHERE " + LSID_ATTRIBUTE +
//			" LIKE '" + EasyIO.prepareForLIKE(lsid) + "'" +
//			(restrictToNomenclature ? (" AND " + IS_NOMENCLATURE_NAME_ATTRIBUTE + " = '" + NOMENCLATURE_NAME_MARKER + "'") : "") + 
//			";";
//	
//		QueryResult result = null;
//		SqlQueryResult sqr = null;
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
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while searching.");
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
//	private QueryResult getNameResult(String taxonName, boolean restrictToNomenclature, boolean restrictToExactMatch) {
//		
//		//	tokenize search string
//		taxonName = taxonName.trim();
//		System.out.println("  - doing fuzzy lookup for '" + taxonName + "'");
//		String[] taxonNameParts = taxonName.split("\\s++");
//		
//		//	do query
//		QueryResult result = null;
//		
//		//	look up all parts at once
//		if (restrictToExactMatch) {
//			StringBuffer taxonNameSearchString = new StringBuffer();
//			for (int p = 0; p < taxonNameParts.length; p++)
//				taxonNameSearchString.append("%|" + EasyIO.sqlEscape(taxonNameParts[p]) + "%");
//			result = this.getNameResult(taxonNameSearchString.toString(), restrictToNomenclature);
//		}
//		
//		//	look up individual parts and combine them
//		else {
//			
//			//	get name part results
//			QueryResult[] taxonNamePartResults = new QueryResult[taxonNameParts.length];
//			for (int p = 0; p < taxonNamePartResults.length; p++) {
//				String taxonNameSearchString = ("%|" + EasyIO.sqlEscape(taxonNameParts[p]) + "%");
//				taxonNamePartResults[p] = this.getNameResult(taxonNameSearchString, restrictToNomenclature);
//			}
//			
//			//	combine part results
//			for (int p = 0; p < taxonNamePartResults.length; p++) {
//				if (result == null)
//					result = taxonNamePartResults[p];
//				else result = result.merge(taxonNamePartResults[p], (restrictToExactMatch ? QueryResult.USE_MIN : QueryResult.USE_AVERAGE), 0);
//			}
//		}
//		
//		//	prune and return result
//		if (result != null) {
//			result.sortByRelevance(true);
//			result.pruneByRelevance(Double.MIN_VALUE);
//		}
//		return result;
//	}
//	
//	private QueryResult getNameResult(String searchString, boolean restrictToNomenclature) {
//		if ((searchString == null) || (searchString.trim().length() == 0)) return null;
//		if (searchString.length() > NAME_LENGTH) searchString = searchString.substring(0, NAME_LENGTH);
//		
//		String query = "SELECT " + DOC_NUMBER_COLUMN_NAME + 
//			" FROM " + TAXONOMIC_NAME_ANNOTATION_TYPE +
//			" WHERE " + TAXONOMIC_NAME_ANNOTATION_TYPE +
//			" LIKE '" + searchString + "%'" + 
//			(restrictToNomenclature ? (" AND " + IS_NOMENCLATURE_NAME_ATTRIBUTE + " = '" + NOMENCLATURE_NAME_MARKER + "'") : "") + ";";
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(query);
//			
//			QueryResult result = new QueryResult();
//			while (sqr.next()) {
//				
//				//	read data
//				String docNr = sqr.getString(0);
//				if (docNr != null)
////					result.addResultElement(new QueryResultElement(Integer.parseInt(docNr), 1.0));
//					result.addResultElement(new QueryResultElement(Long.parseLong(docNr), 1.0));
//			}
//			return result;
//		}
//		catch (SQLException sqle) {
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while searching.");
//			System.out.println("  Query was " + query);
//			return null;
//		}
//		finally {
//			if (sqr != null)
//				sqr.close();
//		}
//	}
//	
//	/** @see de.goldenGateSrs.AbstractIndexer#markEssentialDetails(de.gamta.MutableAnnotation)
//	 */
//	public void markEssentialDetails(MutableAnnotation doc) {
//		
//		//	get taxonomic names
//		Annotation[] taxonomicNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		for (int t = 0; t < taxonomicNames.length; t++)
//			doc.addAnnotation(DETAIL_ANNOTATION_TYPE, taxonomicNames[t].getStartIndex(), taxonomicNames[t].size()).setAttribute(DETAIL_TYPE_ATTRIBUTE, TAXONOMIC_NAME_ANNOTATION_TYPE);
//		
//		Annotation[] subSubSections = doc.getAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
//		for (int s = 0; s < subSubSections.length; s++) {
//			if ("nomenclature".equals(subSubSections[s].getAttribute("type")))
//				doc.addAnnotation(DETAIL_ANNOTATION_TYPE, subSubSections[s].getStartIndex(), subSubSections[s].size()).setAttribute(DETAIL_TYPE_ATTRIBUTE, TAXONOMIC_NAME_ANNOTATION_TYPE);
//		}
//	}
//	
//	/** @see de.goldenGateSrs.AbstractIndexer#markSearchables(de.gamta.MutableAnnotation)
//	 */
//	public void markSearchables(MutableAnnotation doc) {
//		
//		//	get taxonomic names
//		Annotation[] taxonomicNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		
//		//	add search link attributes
//		for (int t = 0; t < taxonomicNames.length; t++) {
//			
//			//	create links for name parts & LSID
//			this.addSearchAttributes(taxonomicNames[t]);
//			
//			//	make taxonomic name annotations displayable in result index
//			taxonomicNames[t].setAttribute(RESULT_INDEX_NAME_ATTRIBUTE, TAXONOMIC_NAME_ANNOTATION_TYPE);
//			taxonomicNames[t].setAttribute(RESULT_INDEX_LABEL_ATTRIBUTE, TAXONOMIC_NAME_INDEX_LABEL);
//			taxonomicNames[t].setAttribute(RESULT_INDEX_FIELDS_ATTRIBUTE, this.taxonomicNameIndexFields);
//		}
//		
//		//	highlight nomenclature
//		QueriableAnnotation[] nomenclature = GPath.evaluatePath(doc, nomenclaturePath, null);
//		if (nomenclature.length != 0) {
//			nomenclature[0].setAttribute(BOXED_ATTRIBUTE, BOXED_ATTRIBUTE);
//			nomenclature[0].setAttribute(BOX_TITLE_ATTRIBUTE, "Scientific Name");
//			
//			Annotation[] nomenclatureNames = nomenclature[0].getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//			if (nomenclatureNames.length != 0)
//				nomenclatureNames[0].setAttribute(BOX_PART_LABEL_ATTRIBUTE, "Scientific Name");
//			
//			Annotation[] nomenclatureNameLabels = nomenclature[0].getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
//			if (nomenclatureNameLabels.length != 0)
//				nomenclatureNameLabels[0].setAttribute(BOX_PART_LABEL_ATTRIBUTE, "Taxon Status");
//		}
//	}
//	
//	private static final GPath nomenclaturePath = new GPath("//subSubSection[./@type = 'nomenclature']");
//	
//	/** @see de.goldenGateSrs.Indexer#addSearchAttributes(de.gamta.Annotation)
//	 */
//	public void addSearchAttributes(Annotation annotation) {
//		if (!TAXONOMIC_NAME_ANNOTATION_TYPE.equals(annotation.getType())) return;
//		
//		boolean isGenusOrBelow = false;
//		for (int r = 0; r < this.ranks.length; r++) {
//			String rankName = this.ranks[r].getName();
//			if (annotation.hasAttribute(rankName) && GENUS_ATTRIBUTE.equals(rankName)) {
//				isGenusOrBelow = true;
//				r = this.ranks.length;
//			}
//		}
//		
//		//	create links for name parts
////		String linkQueryValue = (this.getFullFieldName(IS_NOMENCLATURE_NAME_ATTRIBUTE) + "=1");
////		linkQueryValue += ("&" + this.getFullFieldName(EXACT_MATCH_OPTION) + "=1");
//		String linkQuery = (this.getFullFieldName(IS_NOMENCLATURE_NAME_ATTRIBUTE) + "=" + true);
//		linkQuery += ("&" + this.getFullFieldName(EXACT_MATCH_OPTION) + "=" + true);
//		String linkQueryTaxon = "";
//		String linkTitle = "";
//		boolean gotGenus = false;
//		for (int r = 0; r < this.ranks.length; r++) {
//			String rankName = this.ranks[r].getName();
//			if (annotation.hasAttribute(rankName)) try {
//				gotGenus = (gotGenus || GENUS_ATTRIBUTE.equals(rankName));
//				if (isGenusOrBelow ? !gotGenus : !rankName.equals(annotation.getAttribute(RANK_ATTRIBUTE)))
//					continue;
//				
//				String epithet = ((String) annotation.getAttribute(rankName));
//				linkQueryTaxon += (((linkQueryTaxon.length() == 0) ? "" : " ") + epithet);
//				linkTitle += (((linkTitle.length() == 0) ? "" : " ") + this.ranks[r].formatEpithet(epithet));
//				if (rankName.equals(annotation.getAttribute(RANK_ATTRIBUTE))) {
//					linkQuery += ("&" + this.getFullFieldName(TAXONOMIC_NAME_ANNOTATION_TYPE) + "=" + URLEncoder.encode(linkQueryTaxon, "UTF-8"));
//					annotation.setAttribute(SEARCH_LINK_QUERY_ATTRIBUTE, linkQuery);
//					annotation.setAttribute(SEARCH_LINK_TITLE_ATTRIBUTE, ("Search '" + linkTitle.trim() + "'"));
//				}
//			} catch (Exception e) {}
//		}
//		
//		//	create link for LSID
//		String[] attributeNames = annotation.getAttributeNames();
//		for (int a = 0; a < attributeNames.length; a++) {
//			if (attributeNames[a].endsWith(LSID_ATTRIBUTE) || attributeNames[a].startsWith(LSID_ATTRIBUTE)) try {
//				String lsid = annotation.getAttribute(attributeNames[a], "").toString();
//				if (!LSID_ATTRIBUTE.equals(attributeNames[a]))
//					annotation.setAttribute(LSID_ATTRIBUTE, lsid);
//			} catch (Exception e) {}
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
//		
//		HashSet deDuplicator = new HashSet();
//		StringVector docNrs = new StringVector();
//		for (int n = 0; n < docNumbers.length; n++) {
////			if (deDuplicator.add(new Integer(docNumbers[n])))
//			if (deDuplicator.add(new Long(docNumbers[n])))
//				docNrs.addElement("" + docNumbers[n]);
//		}
//		if (docNrs.isEmpty()) return null;
//		
//		String where = DOC_NUMBER_COLUMN_NAME + " IN (" + docNrs.concatStrings(", ") + ")";
//		
//		//	check if search restricted to nomenclature
//		String inNomenclature = query.getValue(IS_NOMENCLATURE_NAME_ATTRIBUTE, "").trim();
//		if (inNomenclature.trim().length() != 0)
//			where += " AND " + IS_NOMENCLATURE_NAME_ATTRIBUTE + "='" + NOMENCLATURE_NAME_MARKER + "'";
//		
//		//	check for exact match
//		String exactMatch = query.getValue(EXACT_MATCH_OPTION);
//		boolean restrictToExactMatch = ((exactMatch != null) && (exactMatch.trim().length() != 0));
//		
//		//	check for name parts
//		String nameWhere = "";
//		for (int rg = 0; rg < this.rankGroups.length; rg++) {
//			String value = query.getValue(this.rankGroups[rg].getName(), "").trim();
//			if (value.length() > this.rankGroupLengths[rg])
//				value = value.substring(0, this.rankGroupLengths[rg]);
//			if (value.length() != 0)
//				nameWhere += ((nameWhere.length() == 0) ? "" : (restrictToExactMatch ? " AND " : " OR ")) + this.rankGroups[rg].getName() + " LIKE '%|" + EasyIO.prepareForLIKE(this.rankGroups[rg].isEpithetsCapitalized() ? Gamta.capitalize(value) : value.toLowerCase()) + "%'";
//		}
//		
//		//	check for whole name
//		if (nameWhere.length() == 0) {
//			String taxonName = query.getValue(TAXONOMIC_NAME_ANNOTATION_TYPE, "").trim();
//			String[] taxonNameParts = taxonName.split("\\s++");
//			
//			//	look up all parts at once
//			if (restrictToExactMatch) {
//				StringBuffer taxonNameSearchString = new StringBuffer();
//				for (int p = 0; p < taxonNameParts.length; p++)
//					taxonNameSearchString.append("%|" + EasyIO.sqlEscape(taxonNameParts[p]) + "%");
//				nameWhere = (TAXONOMIC_NAME_ANNOTATION_TYPE + " LIKE '" + taxonNameSearchString.toString() + "'");
//			}
//			
//			//	look up individual parts and combine them
//			else for (int p = 0; p < taxonNameParts.length; p++)
//				nameWhere += ((nameWhere.length() == 0) ? "" : " OR ") + TAXONOMIC_NAME_ANNOTATION_TYPE + " LIKE '%|" + EasyIO.prepareForLIKE(taxonNameParts[p]) + "%'";
//		}
//		
//		//	any name query?
//		if (nameWhere.length() != 0)
//			where += (" AND (" + nameWhere + ")");
//		
//		String[] displayColumns = {DOCUMENT_POSITION_ATTRIBUTE, IndexResultElement.INDEX_ENTRY_VALUE_ATTRIBUTE};
//		StringVector columnCollector = new StringVector();
//		columnCollector.addElement(DOCUMENT_POSITION_ATTRIBUTE);
//		columnCollector.addElement(LSID_SOURCE_ATTRIBUTE);
//		columnCollector.addElement(LSID_ATTRIBUTE);
//		columnCollector.addElement(LSID_NAME_ATTRIBUTE);
//		columnCollector.addElement(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		columnCollector.addElement(STATUS_ATTRIBUTE);
//		columnCollector.addElement(IS_NOMENCLATURE_NAME_ATTRIBUTE);
//		final String[] columns = columnCollector.toStringArray();
//		
//		//	assemble query and do lookup
//		StringBuffer sqlQuery = new StringBuffer("SELECT " + DOC_NUMBER_COLUMN_NAME); 
//		for (int c = 0; c < columns.length; c++) {
//			sqlQuery.append(", ");
//			sqlQuery.append(columns[c]);
//		}
//		sqlQuery.append(" FROM " + TAXONOMIC_NAME_ANNOTATION_TYPE); 
//		sqlQuery.append(" WHERE " + where);
//		
//		//	sort for listing, use likely display order
//		if (sort) sqlQuery.append(" ORDER BY " + TAXONOMIC_NAME_ANNOTATION_TYPE);
//		
//		//	sort for joining, use order of appearance in document
//		else sqlQuery.append(" ORDER BY " + DOC_NUMBER_COLUMN_NAME + ", " + DOCUMENT_POSITION_ATTRIBUTE);
//		
//		//	terminate query
//		sqlQuery.append(";");
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(sqlQuery.toString());
//			IndexResult ir = new TaxonIndexResult(displayColumns, sqr) {
//				protected IndexResultElement decodeResultElement(String[] elementData) {
////					IndexResultElement ire = new IndexResultElement(Integer.parseInt(elementData[0]), TAXONOMIC_NAME_ANNOTATION_TYPE, "");
//					IndexResultElement ire = new IndexResultElement(Long.parseLong(elementData[0]), TAXONOMIC_NAME_ANNOTATION_TYPE, "");
//					
//					//	read document position
//					ire.setAttribute(DOCUMENT_POSITION_ATTRIBUTE, elementData[1]);
//					
//					//	read LSID data
//					String lsidSource = elementData[2];
//					if ((elementData[2] != null) && (elementData[2].trim().length() == 0))
//						lsidSource = null;
//					
//					if (elementData[3] != null) {
//						if (lsidSource != null)
//							ire.setAttribute((LSID_ATTRIBUTE + "-" + lsidSource), elementData[3]);
//						ire.setAttribute(LSID_ATTRIBUTE, elementData[3]);
//						
//						String lsidParts = elementData[3];
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
//					
//					if (elementData[4] != null) {
//						if (lsidSource != null)
//							ire.setAttribute((LSID_NAME_ATTRIBUTE + "-" + lsidSource), elementData[4]);
//						ire.setAttribute(LSID_NAME_ATTRIBUTE, elementData[4]);
//					}
//					
//					//	read name details
//					String taxonName = elementData[5];
//					StringVector taxonNameParts = new StringVector();
//					taxonNameParts.parseAndAddElements(taxonName, "|");
//					taxonNameParts.remove(0);
//					StringBuffer taxonNameBuilder = new StringBuffer();
//					for (int r = 0; r < ranks.length; r++) {
//						if (taxonNameParts.size() <= r)
//							break;
//						String taxonNamePart = taxonNameParts.get(r);
//						if (taxonNamePart.length() == 0)
//							continue;
//						ire.setAttribute(ranks[r].getName(), taxonNamePart);
//						ire.setAttribute(RANK_ATTRIBUTE, ranks[r].getName());
//						if (taxonNameBuilder.length() != 0)
//							taxonNameBuilder.append(" ");
//						taxonNameBuilder.append(ranks[r].formatEpithet(taxonNamePart));
//					}
//					taxonName = taxonNameBuilder.toString();
//					
//					//	read additional info
//					if (elementData[6] != null)
//						ire.setAttribute(STATUS_ATTRIBUTE, elementData[6]);
//					if (("" + NOMENCLATURE_NAME_MARKER).equals(elementData[7]))
////						ire.setAttribute(IS_NOMENCLATURE_NAME_ATTRIBUTE, IS_NOMENCLATURE_NAME_ATTRIBUTE);
//						ire.setAttribute(IS_NOMENCLATURE_NAME_ATTRIBUTE, ("" + true));
//					
//					//	store lookup result tupel
//					IndexResultElement nIre = new IndexResultElement(ire.docNr, TAXONOMIC_NAME_ANNOTATION_TYPE, taxonName);
//					nIre.copyAttributes(ire);
//					nIre.setAttribute(IndexResultElement.INDEX_ENTRY_VALUE_LABEL_ATTRIBUTE, "Scientific Name");
//					return nIre;
//				}
//			};
//			return ir;
//		}
//		catch (SQLException sqle) {
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while getting index entries.");
//			System.out.println("  query was " + sqlQuery.toString());
////			if (sqr != null)
////				sqr.close();
//			return null;
//		}
//	}
//	
//	private class TaxonIndexResult extends SqlIndexResult {
//		TaxonIndexResult(String[] resultAttributes, SqlQueryResult sqr) {
//			super(resultAttributes, TAXONOMIC_NAME_ANNOTATION_TYPE, "Scientific Name Index", TAXONOMIC_NAME_ANNOTATION_TYPE, sqr);
//		}
//	}
//	
//	/** @see de.goldenGateSrs.Indexer#doThesaurusLookup(de.goldenGateSrs.Query)
//	 */
//	public ThesaurusResult doThesaurusLookup(Query query) {
//		String where;
//		
//		//	process free text query
//		String name = query.getValue(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		if ((name != null) && (name.length() != 0))
//			where = this.getFuzzyThesaurusLookupPredicate(name, query);
//			
//		//	process detail query
//		else where = this.getDetailedThesaurusLookupPredicate(query);
//		
//		//	check if query given
//		if (where.length() < 4) {
//			System.out.println("TaxonomicNameIndexer: no query for doing thesaurus lookup.");
//			return null;
//		}
//		
//		//	check if search restricted to nomenclature
//		String inNomenclature = query.getValue(IS_NOMENCLATURE_NAME_ATTRIBUTE);
//		if ((inNomenclature != null) && (inNomenclature.trim().length() != 0))
//			where += (" AND (" + IS_NOMENCLATURE_NAME_ATTRIBUTE + " = '" + NOMENCLATURE_NAME_MARKER + "')");
//		
//		StringVector columnCollector = new StringVector();
//		for (int r = 0; r < this.ranks.length; r++)
//			columnCollector.addElement(this.ranks[r].getName());
//		columnCollector.addElement(LSID_ATTRIBUTE);
//		String[] columns = columnCollector.toStringArray();
//		
//		//	assemble query and do lookup
//		StringBuffer sqlQuery = new StringBuffer("SELECT DISTINCT " + TAXONOMIC_NAME_ANNOTATION_TYPE + ", " + LSID_ATTRIBUTE);
//		sqlQuery.append(" FROM " + TAXONOMIC_NAME_ANNOTATION_TYPE); 
//		sqlQuery.append(" WHERE " + where + ";");
//		
//		SqlQueryResult sqr = null;
//		try {
//			sqr = this.io.executeSelectQuery(sqlQuery.toString());
//			return new TaxonThesaurusResult(columns, sqr);
//		}
//		catch (SQLException sqle) {
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while doing thesaurus lookup.");
//			System.out.println("  Query was " + sqlQuery.toString());
////			if (sqr != null)
////				sqr.close();
//			return null;
//		}
//	}
//	
//	private class TaxonThesaurusResult extends SqlThesaurusResult {
//		TaxonThesaurusResult(String[] resultFieldNames, SqlQueryResult sqr) {
//			super(resultFieldNames, TAXONOMIC_NAME_ANNOTATION_TYPE, "Taxonomic Name Index", sqr);
//		}
//		protected ThesaurusResultElement decodeResultElement(String[] elementData) {
//			ThesaurusResultElement tre = new ThesaurusResultElement();
//			if ((elementData[1] != null) && (elementData[1].length() != 0))
//				tre.setAttribute(LSID_ATTRIBUTE, elementData[1]);
//			StringVector taxonNameParts = new StringVector();
//			taxonNameParts.parseAndAddElements(elementData[0], "|");
//			taxonNameParts.remove(0);
//			for (int r = 0; r < ranks.length; r++) {
//				if (taxonNameParts.size() <= r)
//					break;
//				String taxonNamePart = taxonNameParts.get(r);
//				if (taxonNamePart.length() == 0)
//					continue;
//				tre.setAttribute(ranks[r].getName(), taxonNamePart);
//			}
//			return tre;
//		}
//	}
//	
//	private String getFuzzyThesaurusLookupPredicate(String taxonName, Query query) {
//		
//		//	tokenize search string
//		taxonName = taxonName.trim();
//		String[] taxonNameParts = taxonName.split("\\s++");
//		
//		//	assemble predicate
//		StringBuffer predicate = new StringBuffer("(" + TAXONOMIC_NAME_ANNOTATION_TYPE + " LIKE '%");
//		for (int p = 0; p < taxonNameParts.length; p++)
//			predicate.append("|" + EasyIO.sqlEscape(taxonNameParts[p]) + "%");
//		predicate.append("')");
//		return predicate.toString();
//	}
//	
//	private String getDetailedThesaurusLookupPredicate(Query query) {
//		
//		String where = "1=1";
//		
//		//	process LSID query
//		String lsid = query.getValue(LSID_ATTRIBUTE);
//		if ((lsid != null) && (lsid.trim().length() != 0))
//			where += (" AND (" + LSID_ATTRIBUTE + " LIKE '" + EasyIO.prepareForLIKE(lsid) + "')");
//		
//		//	get name part conditions only if LSID not set (LSID queries produce precise results, no need for further conditions)
//		else for (int rg = 0; rg < this.rankGroups.length; rg++) {
//			String value = query.getValue(this.rankGroups[rg].getName(), "").trim();
//			if (value.length() > this.rankGroupLengths[rg])
//				value = value.substring(0, this.rankGroupLengths[rg]);
//			if (value.length() != 0)
//				where += (" AND (" + this.rankGroups[rg].getName() + " LIKE '%|" + EasyIO.prepareForLIKE(this.rankGroups[rg].isEpithetsCapitalized() ? Gamta.capitalize(value) : value.toLowerCase()) + "%')");
//		}
//		
//		return where;
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
//		
//		//	check which names are nomenclature
//		HashSet nomencaltureNameIdSet = new HashSet();
//		QueriableAnnotation[] subSubSections = doc.getAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
//		for (int s = 0; s < subSubSections.length; s++) {
//			if ("nomenclature".equals(subSubSections[s].getAttribute("type"))) {
//				Annotation[] taxonomicNames = subSubSections[s].getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//				for (int t = 0; t < taxonomicNames.length; t++)
//					nomencaltureNameIdSet.add(taxonomicNames[t].getAnnotationID());
//				s = subSubSections.length; // index first nomenclature section only
//			}
//		}
//		
//		//	get taxonomic names & labels
//		Annotation[] taxonomicNames = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		Annotation[] taxonomicNameLabels = doc.getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
//		int tnlIndex = 0;
//		
//		//	make sure taxonomic name Annotations have necessary attributes
//		HashSet indexedEpithets = new HashSet();
//		for (int t = 0; t < taxonomicNames.length; t++) {
//			if (nomencaltureNameIdSet.isEmpty()) // substitute first taxon name in treatment for nomenclature name if no other found
//				nomencaltureNameIdSet.add(taxonomicNames[t].getAnnotationID());
//			
//			//	start column strings
//			StringBuffer columns = new StringBuffer(DOC_NUMBER_COLUMN_NAME);
//			StringBuffer values = new StringBuffer("" + docNr);
//			
//			//	add LSID and related data
//			String lsid = "";
//			String lsidSource = "";
//			String lsidName = "";
//			String[] attributeNames = taxonomicNames[t].getAttributeNames();
//			for (int a = 0; a < attributeNames.length; a++) {
//				if (attributeNames[a].startsWith(LSID_ATTRIBUTE + "-")) {
//					lsid = ((String) taxonomicNames[t].getAttribute(attributeNames[a], "")).trim();
//					lsidSource = attributeNames[a].substring(LSID_ATTRIBUTE.length() + 1);
//					lsidName = ((String) taxonomicNames[t].getAttribute((LSID_NAME_ATTRIBUTE + "-" + lsidSource), "")).trim();
//				}
//				else if (attributeNames[a].endsWith("-" + LSID_ATTRIBUTE)) {
//					lsid = ((String) taxonomicNames[t].getAttribute(attributeNames[a], "")).trim();
//					lsidSource = attributeNames[a].substring(0, (attributeNames[a].length() - (LSID_ATTRIBUTE.length() + 1)));
//					lsidName = ((String) taxonomicNames[t].getAttribute((lsidSource + "-" + LSID_NAME_ATTRIBUTE), "")).trim();
//				}
//			}
//			if (lsid.length() > LSID_LENGTH)
//				lsid = lsid.substring(0, LSID_LENGTH);
//			columns.append(", " + LSID_ATTRIBUTE);
//			values.append(", '" + EasyIO.sqlEscape(lsid) + "'");
//			
//			if (lsidSource.length() > LSID_SOURCE_LENGTH)
//				lsidSource = lsidSource.substring(0, LSID_SOURCE_LENGTH);
//			columns.append(", " + LSID_SOURCE_ATTRIBUTE);
//			values.append(", '" + EasyIO.sqlEscape(lsidSource) + "'");
//			
//			if (lsidName.length() > LSID_NAME_LENGTH)
//				lsidName = lsidName.substring(0, LSID_NAME_LENGTH);
//			columns.append(", " + LSID_NAME_ATTRIBUTE);
//			values.append(", '" + EasyIO.sqlEscape(lsidName) + "'");
//			
//			//	add name string
//			StringBuffer nameString = new StringBuffer();
//			for (int r = 0; r < this.ranks.length; r++) {
//				nameString.append("|");
//				nameString.append(taxonomicNames[t].getAttribute(this.ranks[r].getName(), ""));
//			}
//			if (nameString.length() > NAME_LENGTH)
//				nameString = nameString.delete(NAME_LENGTH, nameString.length()); 
//			columns.append(", " + TAXONOMIC_NAME_ANNOTATION_TYPE);
//			values.append(", '" + EasyIO.sqlEscape(nameString.toString()) + "'");
//			
//			//	add attribute values & determine rank weight
//			int rankWeight = 0;
//			for (int rg = 0; rg < this.rankGroups.length; rg++) {
//				Rank[] rgRanks = this.rankGroups[rg].getRanks();
//				StringBuffer rgValue = new StringBuffer();
//				for (int r = 0; r < rgRanks.length; r++) {
//					String rValue = ((String) taxonomicNames[t].getAttribute(rgRanks[r].getName(), ""));
//					rValue = (this.rankGroups[rg].isEpithetsCapitalized() ? Gamta.capitalize(rValue.trim()) : rValue.trim().toLowerCase());
//					if (rValue.length() > NAME_PART_LENGTH)
//						rValue = rValue.substring(0, NAME_PART_LENGTH);
//					rgValue.append("|" + rValue);
//					if (rValue.length() != 0)
//						rankWeight = (this.ranks.length - rgRanks[r].getOrderNumber() + 1);
//				}
//				
//				columns.append(", " + this.rankGroups[rg].getName());
//				values.append(", '" + EasyIO.sqlEscape(rgValue.toString()) + "'");
//			}
//			
//			//	add rank weight
//			columns.append(", " + RANK_WEIGHT_ATTRIBUTE);
//			values.append(", " + rankWeight);
//			
//			//	mark as nomenclature name
//			columns.append(", " + IS_NOMENCLATURE_NAME_ATTRIBUTE);
//			values.append(", '" + (nomencaltureNameIdSet.contains(taxonomicNames[t].getAnnotationID()) ? ("" + NOMENCLATURE_NAME_MARKER) : "T") + "'");
//			
//			//	find label for current name
//			columns.append(", " + STATUS_ATTRIBUTE);
//			while ((tnlIndex < taxonomicNameLabels.length) && (taxonomicNameLabels[tnlIndex].getStartIndex() < taxonomicNames[t].getEndIndex()))
//				tnlIndex++;
//			String status = "";
//			if ((tnlIndex < taxonomicNameLabels.length) && ((taxonomicNameLabels[tnlIndex].getStartIndex() - taxonomicNames[t].getEndIndex()) < 2)) {
//				 // do not partially store some behemoth status information, simply ignore it
//				if (taxonomicNameLabels[tnlIndex].length() <= NAME_PART_LENGTH)
//					status = taxonomicNameLabels[tnlIndex].getValue();
//			}
//			values.append(", '" + EasyIO.sqlEscape(status) + "'");
//			
//			//	add document position
//			columns.append(", " + DOCUMENT_POSITION_ATTRIBUTE);
//			values.append(", " + t);
//			
//			//	write data to index table
//			String query = ("INSERT INTO " + TAXONOMIC_NAME_ANNOTATION_TYPE + " (" + columns + ") VALUES (" + values + ");");
//			try {
//				this.io.executeUpdateQuery(query);
//			}
//			catch (SQLException sqle) {
//				System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while indexing document.");
//				System.out.println("  Query was " + query);
//			}
//			
//			//	fill epithet table
//			for (int rg = 0; rg < this.rankGroups.length; rg++) {
//				Rank[] rgRanks = this.rankGroups[rg].getRanks();
//				for (int r = 0; r < rgRanks.length; r++) {
//					String rEpithet = ((String) taxonomicNames[t].getAttribute(rgRanks[r].getName(), ""));
//					rEpithet = (this.rankGroups[rg].isEpithetsCapitalized() ? Gamta.capitalize(rEpithet.trim()) : rEpithet.trim().toLowerCase());
//					if (rEpithet.length() > NAME_PART_LENGTH)
//						rEpithet = rEpithet.substring(0, NAME_PART_LENGTH);
//					if (rEpithet.length() == 0)
//						continue;
//					if (!indexedEpithets.add(rankWeight + "-" + rg + "-" + rEpithet + "-" + (nomencaltureNameIdSet.contains(taxonomicNames[t].getAnnotationID()) ? ("" + NOMENCLATURE_NAME_MARKER) : "T")))
//						continue;
//					String eQuery = ("INSERT INTO " + TAXONOMIC_NAME_EPITHET_TABLE_NAME + " (" +
//							DOC_NUMBER_COLUMN_NAME + ", " + RANK_WEIGHT_ATTRIBUTE + ", " + RANK_GROUP_NUMBER_COLUMN_NAME + ", " + EPITHET_COLUMN_NAME + "," + IS_NOMENCLATURE_NAME_ATTRIBUTE +
//							") VALUES (" + 
//							docNr + ", " + rankWeight + ", " + rg + ", '" + EasyIO.sqlEscape(rEpithet) + "', '" + (nomencaltureNameIdSet.contains(taxonomicNames[t].getAnnotationID()) ? ("" + NOMENCLATURE_NAME_MARKER) : "T") + "'" +
//							");");
//					try {
//						this.io.executeUpdateQuery(eQuery);
//					}
//					catch (SQLException sqle) {
//						System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while indexing document.");
//						System.out.println("  Query was " + query);
//					}
//				}
//			}
//			
//			//	remove nomenclature name attribute
//			taxonomicNames[t].removeAttribute(IS_NOMENCLATURE_NAME_ATTRIBUTE);
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
//		String query = ("DELETE FROM " + TAXONOMIC_NAME_ANNOTATION_TYPE + " WHERE " + DOC_NUMBER_COLUMN_NAME + "=" + docNr + ";");
//		try {
//			this.io.executeUpdateQuery(query);
//		}
//		catch (SQLException sqle) {
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while deleting document.");
//			System.out.println("  Query was " + query);
//		}
//		
//		String eQuery = ("DELETE FROM " + TAXONOMIC_NAME_EPITHET_TABLE_NAME + " WHERE " + DOC_NUMBER_COLUMN_NAME + "=" + docNr + ";");
//		try {
//			this.io.executeUpdateQuery(eQuery);
//		}
//		catch (SQLException sqle) {
//			System.out.println("TaxonomicNameIndexer: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while deleting document.");
//			System.out.println("  Query was " + eQuery);
//		}
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.AbstractIndexer#getFieldGroup()
//	 */
//	protected SearchFieldGroup getFieldGroup() {
//		SearchFieldRow nameOptionsLsidSfr = new SearchFieldRow();
//		
//		nameOptionsLsidSfr.addField(new SearchField(TAXONOMIC_NAME_ANNOTATION_TYPE, "Name", 2));
//		nameOptionsLsidSfr.addField(new SearchField(IS_NOMENCLATURE_NAME_ATTRIBUTE, "Taxa Only", SearchField.BOOLEAN_TYPE));
//		nameOptionsLsidSfr.addField(new SearchField(EXACT_MATCH_OPTION, "Exact Match", SearchField.BOOLEAN_TYPE));
//		nameOptionsLsidSfr.addField(new SearchField(LSID_ATTRIBUTE, "LSID"));
//		
//		SearchFieldRow namePartsSfr = new SearchFieldRow();
//		for (int rg = 0; rg < this.rankGroups.length; rg++) {
//			String rgName = this.rankGroups[rg].getName();
//			namePartsSfr.addField(new SearchField(rgName, (this.getFieldLabel(rgName) + ((this.rankGroups[rg].getRanks().length > 1) ? ", etc." : ""))));
//		}
//		//	TODO layout fields in two rows in more than 5 rank groups configured (blows apart search field otherwise)
//		SearchFieldGroup sfg = new SearchFieldGroup(this.getIndexName(), "Taxonomic Name Index", "Use these fields to search the taxonomic name index.", "Scientific Name");
//		sfg.addFieldRow(nameOptionsLsidSfr);
//		sfg.addFieldRow(namePartsSfr);
//		
//		return sfg;
//	}
////	
////	/** @see de.goldenGateSrs.Indexer#isQuoted(java.lang.String)
////	 */
////	public boolean isQuoted(String fieldName) {
////		return (!RANK_WEIGHT_ATTRIBUTE.equals(fieldName) && !IS_NOMENCLATURE_NAME_ATTRIBUTE.equals(fieldName));
////	}
////	
////	/** @see de.goldenGateSrs.Indexer#getLength(java.lang.String)
////	 */
////	public int getLength(String fieldName) {
////		if (RANK_WEIGHT_ATTRIBUTE.equals(fieldName)) return 0;
////		else if (IS_NOMENCLATURE_NAME_ATTRIBUTE.equals(fieldName)) return 1;
////		else if (LSID_ATTRIBUTE.equals(fieldName)) return LSID_LENGTH;
////		else if (TAXONOMIC_NAME_ANNOTATION_TYPE.equals(fieldName)) return NAME_LENGTH;
////		return NAME_PART_LENGTH;
////	}
//}