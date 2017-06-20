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

package de.uka.ipd.idaho.plugins.modsReferencer.dataSources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsDataSource;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;

/**
 * MODS data source for arbitrary collections stored at archive.org, so far foor
 * whole books only.
 * 
 * @author sautter
 */
public abstract class InternetArchiveModsDataSource extends ModsDataSource {
	
	private String searchBaseUrl = "http://www.archive.org/search.php?query=";
	
	private String archiveBaseUrl = "http://archive.org/download/";
	
	private String metadataFileSuffix = "_meta.xml";
	
	private String collectionName;
	
	/**
	 * Constructor taking the name of the collection in Internet Archive. The
	 * collection name must be specified as a constant string in sub class
	 * constructors, which have to be public and take no arguments to facilitate
	 * class loading.
	 * @param collectionName the name of the backing collection in Internet
	 *            Archive
	 */
	protected InternetArchiveModsDataSource(String collectionName) {
		this.collectionName = collectionName;
	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#init()
//	 */
//	public void init() {
//		// TODO_not_really load URL from config file (they might change it some day ...)
//	}
	
	/**
	 * Check a MODS ID if it matches the ID syntax of the backing source. This
	 * method serves as a filter to redice lookups to the backing source. Its
	 * default implementation simply returns true. Sub classes are welcome to
	 * overwrite it as needed. Filters should be as restrictive as possible.
	 * @param modsId the MODS ID to check
	 * @return true if the speciefied ID might come from the backing source,
	 *         judging by syntax only
	 */
	protected boolean isValidModsID(String modsId) {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#getModsData(java.lang.String)
	 */
	public ModsDataSet getModsData(String modsId) throws IOException {
		
		//	check if ID might come from backing source
		if (!this.isValidModsID(modsId))
			return null;
		
		//	load raw data
		URL url = new URL(this.archiveBaseUrl + modsId + "/" + modsId + metadataFileSuffix);
		HttpURLConnection con = ((HttpURLConnection) url.openConnection());
		InputStreamReader isr = new InputStreamReader(con.getInputStream(), "UTF-8");
		MutableAnnotation modsDataSource = SgmlDocumentReader.readDocument(isr);
		
		StringBuffer sb = new StringBuffer("<mods:mods>");
		this.addTitle(sb, modsDataSource);
		this.addAuthors(sb, modsDataSource);
		
		sb.append("<mods:typeOfResource>text</mods:typeOfResource>");
		
		this.addOriginInfo(sb, modsDataSource);
		
		sb.append("<mods:location>");
		sb.append("<mods:url>" + AnnotationUtils.escapeForXml(this.archiveBaseUrl + modsId + "/" + modsId + this.getPdfFileSuffix()) + "</mods:url>");
		sb.append("</mods:location>");
		
		sb.append("<mods:identifier type=\"IA-PUB\">" + AnnotationUtils.escapeForXml(modsId) + "</mods:identifier>");
		sb.append("</mods:mods>");
		
		MutableAnnotation modsHeader = SgmlDocumentReader.readDocument(new StringReader(sb.toString()));
		ModsUtils.cleanModsHeader(modsHeader);
		return ModsUtils.getModsDataSet(modsHeader);
	}
	
	/**
	 * Provide the suffix to use for the PDF URL. This default implementation
	 * returns '.pdf'. Sub classes wanting to use another suffix are welcome to
	 * overwrite it as needed.
	 * @return the suffix to use for the PDF URL
	 */
	protected String getPdfFileSuffix() {
		return ".pdf";
	}
	
	private void addTitle(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		Annotation[] title = modsDataSource.getAnnotations("title");
		if (title.length == 0)
			throw new IOException("Metadata incomplete - title is missing");
		
		sb.append("<mods:titleInfo><mods:title>");
		sb.append(AnnotationUtils.escapeForXml(TokenSequenceUtils.concatTokens(title[0], true, true)));
		sb.append("</mods:title></mods:titleInfo>");
	}
	
	private void addAuthors(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		Annotation[] authors = modsDataSource.getAnnotations("creator");
		if (authors.length == 0)
			throw new IOException("Metadata incomplete - author is missing");
		
		for (int a = 0; a < authors.length; a++) {
			sb.append("<mods:name type=\"personal\">");
			sb.append("<mods:role>");
			sb.append("<mods:roleTerm>Author</mods:roleTerm>");
			sb.append("</mods:role>");
			sb.append("<mods:namePart>" + TokenSequenceUtils.concatTokens(authors[a], true, true) + "</mods:namePart>");
			sb.append("</mods:name>");
		}
	}
	
	/**
	 * Add the origin info for a publication. This may be either one of book
	 * origin info or journal origin info. The purpose of this method is to
	 * enable sub classes to decide which of addBookOriginInfo() and
	 * addJournalOriginInfo() to use.
	 * @param sb the string buffer to append the origin info to
	 * @param modsDataSource the document to extract the origin info from
	 * @throws IOException
	 */
	protected abstract void addOriginInfo(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException;
	
	/**
	 * Add the origin info for a book.
	 * @param sb the string buffer to append the origin info to
	 * @param modsDataSource the document to extract the origin info from
	 * @throws IOException
	 */
	protected void addBookOriginInfo(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		Annotation[] date = modsDataSource.getAnnotations("date");
		if (date.length == 0)
			throw new IOException("Metadata incomplete - date is missing");
		
		Annotation[] publisher = modsDataSource.getAnnotations("publisher");
		if (publisher.length == 0)
			throw new IOException("Metadata incomplete - publisher is missing");
		
		String publisherRaw = TokenSequenceUtils.concatTokens(publisher[0], true, true);
		String publisherName;
		String publisherLocation;
		int publisherSplit = publisherRaw.indexOf(',');
		if (publisherSplit < 1) {
			publisherName = "UNKNOWN";
			publisherLocation = publisherRaw;
		}
		else {
			publisherName = publisherRaw.substring(publisherSplit + 1).trim();
			publisherLocation = publisherRaw.substring(0, publisherSplit).trim();
		}
		
		sb.append("<mods:originInfo>");
		sb.append("<mods:dateIssued>" + date[0].getValue() + "</mods:dateIssued>");
		sb.append("<mods:publisher>" + AnnotationUtils.escapeForXml(publisherName) + "</mods:publisher>");
		sb.append("<mods:place>");
		sb.append("<mods:placeTerm type=\"text\">" + AnnotationUtils.escapeForXml(publisherLocation) + "</mods:placeTerm>");
		sb.append("</mods:place>");
		sb.append("</mods:originInfo>");
	}
	
	/**
	 * Add the origin info for a journal article.
	 * @param sb the string buffer to append the origin info to
	 * @param modsDataSource the document to extract the origin info from
	 * @throws IOException
	 */
	protected void addJournalOriginInfo(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		Annotation[] date = modsDataSource.getAnnotations("date");
		if (date.length == 0)
			throw new IOException("Metadata incomplete - date is missing");
		
		//	TODO_later this is specific to AntBase for the moment, for lack of other examples
		//	Biological Bulletin, Volume: 13, Pages: 185-202
		
		Annotation[] journalData = modsDataSource.getAnnotations("notes");
		if (journalData.length == 0)
			throw new IOException("Metadata incomplete - journal data is missing");
		
		String journalDataRaw = TokenSequenceUtils.concatTokens(journalData[0], true, true);
		
		String pageDataStartString = "Pages:";
		int pageDataStart = journalDataRaw.lastIndexOf(pageDataStartString);
		if (pageDataStart == -1) {
			pageDataStartString = "Page:";
			pageDataStart = journalDataRaw.lastIndexOf(pageDataStartString);
		}
		int issueNumberStart = journalDataRaw.lastIndexOf("Issue:");
		int seriesNumberStart = journalDataRaw.lastIndexOf("Series");
		int volumeNumberStart = journalDataRaw.lastIndexOf("Volume:");
		
		int startPageNumber;
		int endPageNumber;
		if (pageDataStart == -1) {
			startPageNumber = -1;
			endPageNumber = -1;
		}
		else {
			String pageNumberString = journalDataRaw.substring(pageDataStart + pageDataStartString.length()).trim();
			String[] pageNumbers = pageNumberString.split("\\s*\\-\\s*");
			if (pageNumberString.length() == 0) {
				startPageNumber = -1;
				endPageNumber = -1;
			}
			else if (pageNumbers.length == 1) {
				startPageNumber = parseInt(pageNumbers[0].trim());
				endPageNumber = startPageNumber;
			}
			else if (pageNumbers.length >= 2) {
				startPageNumber = parseInt(pageNumbers[0].trim());
				try {
					endPageNumber = parseInt(pageNumbers[1].trim());
				}
				catch (NumberFormatException nfe) {
					endPageNumber = startPageNumber;
				}
			}
			else throw new IOException("Metadata incomplete - page data is invalid");
			journalDataRaw = journalDataRaw.substring(0, pageDataStart).trim();
			while (journalDataRaw.endsWith(","))
				journalDataRaw = journalDataRaw.substring(0, (journalDataRaw.length()-1)).trim();
		}
		
		//	simply burn issue number for now
		if (issueNumberStart != -1) {
			journalDataRaw = journalDataRaw.substring(0, issueNumberStart).trim();
			while (journalDataRaw.endsWith(","))
				journalDataRaw = journalDataRaw.substring(0, (journalDataRaw.length()-1)).trim();
		}
		
		//	simply burn series number for now
		if ((seriesNumberStart != -1) && (seriesNumberStart > volumeNumberStart)) {
			journalDataRaw = journalDataRaw.substring(0, seriesNumberStart).trim();
			while (journalDataRaw.endsWith(","))
				journalDataRaw = journalDataRaw.substring(0, (journalDataRaw.length()-1)).trim();
		}
		
		int volumeNumber;
		if (volumeNumberStart == -1)
			volumeNumber = -1;
		else {
			String volumeNumberString = journalDataRaw.substring(volumeNumberStart + "Volume:".length()).trim();
			if (!volumeNumberString.matches("[0-9]++")) {
				volumeNumberString = volumeNumberString.replaceAll("[^0-9]+", "_");
				while (volumeNumberString.startsWith("_"))
					volumeNumberString = volumeNumberString.substring(1);
				volumeNumberString = volumeNumberString.replaceAll("\\_.*", "");
			}
			if (volumeNumberString.length() == 0)
				volumeNumber = -1;
			else volumeNumber = parseInt(volumeNumberString);
			journalDataRaw = journalDataRaw.substring(0, volumeNumberStart).trim();
			while (journalDataRaw.endsWith(","))
				journalDataRaw = journalDataRaw.substring(0, (journalDataRaw.length()-1)).trim();
		}
		
		String journalName = journalDataRaw;
		
		sb.append("<mods:relatedItem type=\"host\">");
		sb.append("<mods:titleInfo>");
		sb.append("<mods:title>" + AnnotationUtils.escapeForXml(journalName) + "</mods:title>");
		sb.append("</mods:titleInfo>");
		sb.append("<mods:part>");
		if (volumeNumber != -1) {
			sb.append("<mods:detail type=\"volume\">");
			sb.append("<mods:number>" + volumeNumber + "</mods:number>");
			sb.append("</mods:detail>");
		}
		sb.append("<mods:date>" + date[0].getValue() + "</mods:date>");
		if ((startPageNumber != -1) && (endPageNumber != -1)) {
			sb.append("<mods:extent unit=\"page\">");
			sb.append("<mods:start>" + startPageNumber + "</mods:start>");
			sb.append("<mods:end>" + endPageNumber + "</mods:end>");
			sb.append("</mods:extent>");
		}
		sb.append("</mods:part>");
		sb.append("</mods:relatedItem>");
	}
	
	private static final int parseInt(String theInt) {
		
		//	try the conventional way
		try {
			return Integer.parseInt(theInt);
		}
		
		//	try parsing Roman number
		catch (NumberFormatException nfe) {
			
			//	normalize raw string
			String theRomanInt = theInt.toLowerCase().trim();
			
			//	parse number
			int number = 0;
			for (int n = 0; n < theRomanInt.length(); n++) {
				if (theRomanInt.startsWith("m", n)) number += 1000;
				else if (theRomanInt.startsWith("d", n)) number+= 500;
				else if (theRomanInt.startsWith("cd", n)) {
					number+= 400;
					n++;
				}
				else if (theRomanInt.startsWith("c", n)) number+= 100;
				else if (theRomanInt.startsWith("l", n)) number+= 50;
				else if (theRomanInt.startsWith("xl", n)) {
					number+= 40;
					n++;
				}
				else if (theRomanInt.startsWith("x", n)) number+= 10;
				else if (theRomanInt.startsWith("v", n)) number+= 5;
				else if (theRomanInt.startsWith("iv", n)) {
					number+= 4;
					n++;
				}
				else if (theRomanInt.startsWith("i", n)) number+= 1;
				else throw new NumberFormatException();
			}
			
			//	no value could be parsed, throw exception
			if (number == 0) throw new NumberFormatException(nfe.getMessage());
			
			//	return number
			else return number;
		}
	}
	
	private static Properties searchAttributeMapping = new Properties();
	static {
		searchAttributeMapping.setProperty(MODS_AUTHOR_ATTRIBUTE, "creator");
		searchAttributeMapping.setProperty(MODS_DATE_ATTRIBUTE, "year");
		searchAttributeMapping.setProperty(MODS_TITLE_ATTRIBUTE, "title");
		searchAttributeMapping.setProperty(MODS_ORIGIN_ATTRIBUTE, "publisher");
	}
	
	private static Grammar grammar = new StandardGrammar();
	private static Parser parser = new Parser(grammar);
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#findModsData(java.util.Properties)
	 */
	public ModsDataSet[] findModsData(Properties modsData) throws IOException {
		
		//	build query (restrict to configured collection)
		String query = "collection:(" + this.collectionName + ")";
		boolean emptyQuery = true;
		for (Iterator pit = modsData.keySet().iterator(); pit.hasNext();) {
			String searchAttribute = ((String) pit.next());
			String iaSearchAttribute = searchAttributeMapping.getProperty(searchAttribute);
			if (iaSearchAttribute == null)
				continue;
			String searchValue = modsData.getProperty(searchAttribute);
			query += (" AND " + iaSearchAttribute + ":(" + searchValue + ")");
			emptyQuery = false;
		}
		
		//	check attributes
		if (emptyQuery)
			return null;
		System.out.println("Query is " + query);
		
		//	search document IDs
		ArrayList iaDocIds = new ArrayList();
		String iaDocIdListUrl = this.searchBaseUrl + URLEncoder.encode(query, "UTF-8");
		do {
			iaDocIdListUrl = this.extractModsIds(iaDocIdListUrl, iaDocIds);
			System.out.println(" - proceeding to " + iaDocIdListUrl);
		} while (iaDocIdListUrl != null);
		
		//	fetch data
		ArrayList mdsList = new ArrayList();
		System.out.println("Found  " + iaDocIds.size() + " IDs, fetching data");
		for (int d = 0; d < iaDocIds.size(); d++) {
			String iaDocId = ((String) iaDocIds.get(d));
			try {
				ModsDataSet mds = this.getModsData(iaDocId);
				if (mds != null) {
					System.out.println(" - got data for " + iaDocId);
					mdsList.add(mds);
				}
			}
			catch (IOException ioe) {
				System.out.println("Error getting MODS data for search result " + iaDocId);
				ioe.printStackTrace(System.out);
			}
		}
		return ((ModsDataSet[]) mdsList.toArray(new ModsDataSet[mdsList.size()]));
	}
	
	private String extractModsIds(String listUrl, final List modsIdList) throws IOException {
		final URL url = new URL(listUrl);
		final String[] nextUrl = {null};
		parser.stream(url.openStream(), new TokenReceiver() {
			private String lastHref = null;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token) && "a".equals(grammar.getType(token))) {
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
					String href = tnas.getAttribute("href");
					if (href == null)
						return;
					if ("titleLink".equals(tnas.getAttribute("class"))) {
						href = href.substring(href.lastIndexOf('/') + 1);
						if (href.length() > 0) {
							System.out.println(" - found ID: " + href);
							modsIdList.add(href);
						}
					}
					else this.lastHref = href;
				}
				else if ("Next".equals(token))
					nextUrl[0] = ((this.lastHref.startsWith("/") ? (url.getProtocol() + "://" + url.getAuthority()) : "") + this.lastHref);
			}
		});
		return nextUrl[0];
	}
	
//	http://www.archive.org/search.php?query=title%3A%28Ameisen%20aus%20Rhodesia%29%20AND%20creator%3A%28A%20Forel%29%20AND%20year%3A%281913%29
//	creator:(Forel) AND collection:(biodiversity)
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		ModsDataSource iamds = new InternetArchiveModsDataSource("ant_texts") {
			protected boolean isValidModsID(String modsId) {
				return modsId.matches("ants\\_[0-9]++");
			}
			protected void addOriginInfo(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
				this.addJournalOriginInfo(sb, modsDataSource);
			}
		};
		
//		String[] problems = {
//				"04488",
//				"06224",
//				"06245",
//				"06764",
//				"09729",
//				"10660",
//				"12025",
//				"13007",
//				"13359",
//				"13727",
//				"14617",
//				"14618",
//				"14630",
//				"14640",
//				"14650",
//				"14651",
//				"14658",
//				"20014",
//				"20037",
//				"20286",
//				"20324",
//				"20325",
//				"20327",
//				"20428",
//				"21002",
//				"21063",
//				"21085",
//				"21086",
//				"21087",
//				"21088",
//				"21089",
//				"21119",
//				"21121",
//				"21152",
//			};
//		
//		for (int p = 0; p < problems.length; p++) {
//			System.out.println("Testing ants_" + problems[p] + ":");
//			ModsDataSet mds = iamds.getModsData("ants_" + problems[p]);
//			String[] errors = ModsUtils.getErrorReport(mds);
//			if (errors.length == 0) {
//				System.out.println("Got MODS (valid)");
//			}
//			else {
//				System.out.println("Got MODS (invalid):");
//				for (int e = 0; e < errors.length; e++)
//					System.out.println(errors[e]);
//			}
////			AnnotationUtils.writeXML(mds.getModsHeader(true), new OutputStreamWriter(System.out));
//		}
		
		ModsDataSet mds = iamds.getModsData("ants_04488");
		AnnotationUtils.writeXML(mds.getModsHeader(), new OutputStreamWriter(System.out));
//		
//		Properties modsData = new Properties();
//		modsData.setProperty(MODS_AUTHOR_ATTRIBUTE, "Forel");
//		
//		ModsDataSet[] mdss = iamds.findModsData(modsData);
//		for (int d = 0; d < mdss.length; d++)
//			System.out.println(mdss[d].getTitle());
	}
}
