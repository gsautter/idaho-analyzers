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

package de.uka.ipd.idaho.plugins.modsServer;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsConstants;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;
import de.uka.ipd.idaho.stringUtils.StringVector;


/**
 * REST client for MODS servlet.
 * 
 * @author sautter
 */
public class ModsDataServletClient implements ModsConstants {
	
	private String modsServletURL;
	
	/**
	 * @param modsServletURL the URL of the MODS servlet to communicate with
	 */
	public ModsDataServletClient(String modsServletURL) {
		this.modsServletURL = modsServletURL;
	}
	
	/**
	 * Retrieve a MODS document meta data set from the backing servlet, using
	 * the document ID as the access key. If the backing servlet does not
	 * provide a meta data set for the document with the specified ID, this
	 * method returns null. The returned meta data might be incomplete (the
	 * ModsUtils.getErrorReport() method does report errors).
	 * @param modsId the ID of the document to retrieve the meta data for
	 * @return the meta data of the document with the specified ID
	 * @throws IOException
	 */
	public ModsDataSet getModsData(String modsId) throws IOException {
		URL url = new URL(this.modsServletURL + "?" + MODS_ID_ATTRIBUTE + "=" + URLEncoder.encode(modsId, "UTF-8"));
		
		try {
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
			isr.close();
			
			QueriableAnnotation[] modsHeaders = modsDocument.getAnnotations("mods:mods");
			if (modsHeaders.length == 1)
				return ModsUtils.getModsDataSet(modsHeaders[0]);
			
			else if (modsHeaders.length == 0)
				return null;
			
			else throw new IOException("The specified identifier seems to be ambiguous.");
		}
		catch (FileNotFoundException fnfe) {
			return null; // URL throws this for an HTTP 404
		}
	}
	
	/**
	 * Retrieve the searchable attributes from the backing servlet. These and
	 * only these attributes can be used in findModsData().
	 * @return an array holding the searchable attributes
	 * @throws IOException
	 */
	public String[] getSearchAttributes() throws IOException {
		if (this.searchAttributes == null) {
			URL url = new URL(this.modsServletURL + "?getSearchAttributes=true");
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			StringVector searchAttributes = StringVector.loadList(isr);
			isr.close();
			this.searchAttributes = searchAttributes.toStringArray();
		}
		return this.searchAttributes;
	}
	private String[] searchAttributes = null;
	
	/**
	 * Find the MODS document meta data set in the backing servlet, using known
	 * search attributes the access key, e.g. the document author, parts of the
	 * title, the name of the journal the document appeared in, or the name or
	 * location of the publisher who issued the document. If multiple meta data
	 * sets match, this method returns them all. If the search criteria are
	 * empty, this method returns an empty array. If the backing servlet does
	 * not provide any meta data sets that match the search criteria, this
	 * method returns an empty array. Some of the meta data sets returned might
	 * be incomplete (the ModsUtils.getErrorReport() method does report errors).
	 * @param modsData the known elements of the meta data to retrieve in full
	 * @param cacheOnly restrict search to the backing servlet's cache?
	 * @return the meta data sets matching the specified criteria
	 * @throws IOException
	 */
	public ModsDataSet[] findModsData(Properties modsData, boolean cacheOnly) throws IOException {
		StringBuffer query = new StringBuffer();
		String[] searchAttributes = this.getSearchAttributes();
		for (int a = 0; a < searchAttributes.length; a++) {
			String searchValue = modsData.getProperty(searchAttributes[a]);
			if (searchValue == null)
				continue;
			if (query.length() != 0)
				query.append("&");
			query.append(searchAttributes[a] + "=" + URLEncoder.encode(searchValue, "UTF-8"));
		}
		if (query.length() == 0) {
			for (int a = 0; a < searchAttributes.length; a++) {
				if (query.length() != 0)
					query.append(", ");
				query.append(searchAttributes[a]);
			}
			throw new IOException("Invalid request, specify at least one of the following attributes: " + query.toString());
		}
		
		URL url = new URL(this.modsServletURL + "?" + query.toString());
		try {
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
			isr.close();
			
			QueriableAnnotation[] modsHeaders = modsDocument.getAnnotations("mods:mods");
			ModsDataSet[] mdss = new ModsDataSet[modsHeaders.length];
			for (int d = 0; d < modsHeaders.length; d++)
				mdss[d] = ModsUtils.getModsDataSet(modsHeaders[d]);
			
			return mdss;
		}
		catch (FileNotFoundException fnfe) {
			return new ModsDataSet[0]; // URL throws this for an HTTP 404
		}
	}
	
	/**
	 * Upload a MODS document meta data set to the backing servlet. The argument
	 * MODS data set must be valid, i.e., its isValid() method must return true
	 * for the upload to succees. If it is not, an IOException will be thrown. 
	 * @param modsData the meta data set to store
	 * @param passPhrase the pass phrase to use for authentication
	 * @return true if the data was created (stored for the first time), false
	 *         if it was updated
	 * @throws IOException
	 */
	public boolean storeModsData(ModsDataSet modsData, String passPhrase) throws IOException {
		if (!modsData.isValid())
			throw new IOException("Cannot upload invalid data set.");
		
		URL url = new URL(this.modsServletURL);
		HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("pass", ("" + passPhrase.hashCode()));
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setDoOutput(true);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
		modsData.writeModsHeader(bw);
		bw.flush();
		
		if (connection.getResponseCode() == 200) // HTTP OK
			return false;
		else if (connection.getResponseCode() == 201) // HTTP CREATED
			return true;
		else throw new IOException(connection.getResponseMessage());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		ModsDataServletClient msc = new ModsDataServletClient("http://plazi2.cs.umb.edu:8080/ModsServer/mods");
		ModsDataSet mds = msc.getModsData("21330");
		Writer w = new OutputStreamWriter(System.out);
		mds.writeModsHeader(w);
		w.flush();
		
		Properties msd = new Properties();
//		msd.setProperty(MODS_DATE_ATTRIBUTE, "2007");
		msd.setProperty("someNonExistingAttribute", "withSomeArbitraryValue");
		ModsDataSet[] mdss = msc.findModsData(msd, true);
		for (int d = 0; d < mdss.length; d++)
			mdss[d].writeModsHeader(w);
		
//		System.out.println(msc.storeModsData(mds, "MODS rules it all!"));
	}
}
