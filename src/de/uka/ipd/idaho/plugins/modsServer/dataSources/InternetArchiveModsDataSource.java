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

package de.uka.ipd.idaho.plugins.modsServer.dataSources;

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
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;
import de.uka.ipd.idaho.plugins.modsServer.ModsDataSource;

/**
 * MODS data source for arbitrary collections stored at archive.org, so far foor
 * whole books only.
 * 
 * @author sautter
 */
public abstract class InternetArchiveModsDataSource extends ModsDataSource {
	
	private String searchBaseUrl = "http://www.archive.org/search.php?query=";
	
	private String archiveBaseUrl = "http://archive.org/download/";
	
	private static String pdfFileSuffix = "_bw.pdf";
	private static String metadataFileSuffix = "_meta.xml";
	
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
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#init()
	 */
	public void init() {
		// TODO load URL from config file (they might change some day ...)
	}
	
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
		addTitle(sb, modsDataSource);
		addAuthors(sb, modsDataSource);
		
		sb.append("<mods:typeOfResource>text</mods:typeOfResource>");
		
		//	it's all whole books in BHL ...
		addOriginInfo(sb, modsDataSource);
		
		sb.append("<mods:location>");
		sb.append("<mods:url>" + AnnotationUtils.escapeForXml(this.archiveBaseUrl + modsId + "/" + modsId + pdfFileSuffix) + "</mods:url>");
		sb.append("</mods:location>");
		
		sb.append("<mods:identifier type=\"BHL-PUB\">" + AnnotationUtils.escapeForXml(modsId) + "</mods:identifier>");
		sb.append("</mods:mods>");
		
		MutableAnnotation modsHeader = SgmlDocumentReader.readDocument(new StringReader(sb.toString()));
		ModsUtils.cleanModsHeader(modsHeader);
		AnnotationUtils.writeXML(modsHeader, new OutputStreamWriter(System.out));
		System.out.println();
		return ModsUtils.getModsDataSet(modsHeader);
	}
	
	private static final void addTitle(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		Annotation[] title = modsDataSource.getAnnotations("title");
		if (title.length == 0)
			throw new IOException("Metadata incomplete - title is missing");
		
		sb.append("<mods:titleInfo><mods:title>");
		sb.append(AnnotationUtils.escapeForXml(TokenSequenceUtils.concatTokens(title[0], true, true)));
		sb.append("</mods:title></mods:titleInfo>");
	}
	
	private static final void addAuthors(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		Annotation[] authors = modsDataSource.getAnnotations("creator");
		if (authors.length == 0)
			throw new IOException("Metadata incomplete - author is missing");
		
		for (int a = 0; a < authors.length; a++) {
			String author = TokenSequenceUtils.concatTokens(authors[a], true, true);
			author = author.replaceAll("\\([^\\)]*\\)", "").trim();
			author = author.replaceAll("\\[[^\\]]*\\]", "").trim();
			author = author.replaceAll("[1-2][0-9]{3}.*", "").trim();
			while (Gamta.isPunctuation(author.substring(author.length()-1)))
				author = author.substring(0, (author.length()-1)).trim();
			sb.append("<mods:name type=\"personal\">");
			sb.append("<mods:role>");
			sb.append("<mods:roleTerm>Author</mods:roleTerm>");
			sb.append("</mods:role>");
			sb.append("<mods:namePart>" + author + "</mods:namePart>");
			sb.append("</mods:name>");
		}
		
		//	TODO use functionality from here in BHL doc source for online editor
	}
	
	
	private static final void addOriginInfo(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
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
		
		ModsDataSource bmds = new InternetArchiveModsDataSource("ant_texts") {
			protected boolean isValidModsID(String modsId) {
				return modsId.matches("ants\\_[0-9]++");
			}
		};
		
		Properties modsData = new Properties();
		modsData.setProperty(MODS_AUTHOR_ATTRIBUTE, "Forel");
		
		ModsDataSet[] mdss = bmds.findModsData(modsData);
		for (int d = 0; d < mdss.length; d++)
			System.out.println(mdss[d].getTitle());
	}
}
