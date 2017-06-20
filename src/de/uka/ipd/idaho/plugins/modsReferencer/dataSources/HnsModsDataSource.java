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
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsDataSource;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;

/**
 * MODS data source for hymenoptera name server.
 * 
 * @author sautter
 */
public class HnsModsDataSource extends ModsDataSource {
	
	private String searchBaseUrl = "http://osuc.biosci.ohio-state.edu/hymDB/hym_utilities.format_ref_mods_extended?";
	private String hnsBaseUrl = "http://osuc.biosci.ohio-state.edu/hymDB/hym_utilities.format_ref?style=MODS&id=";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#init()
	 */
	public void init() {
		// TODO load URLs from config file (they might change some day ...)
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#getModsData(java.lang.String)
	 */
	public ModsDataSet getModsData(String modsId) throws IOException {
		
		//	check if ID may come from HNS
		if (!modsId.matches("[0-9]++"))
			return null;
		
		//	get data
		URL url = new URL(this.hnsBaseUrl + modsId);
		InputStreamReader isr = new InputStreamReader(url.openStream());
		MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
		isr.close();
		
		//	parse data
		QueriableAnnotation[] modsHeader = modsDocument.getAnnotations("mods:mods");
		return ((modsHeader.length == 1) ? ModsUtils.getModsDataSet(modsHeader[0]) : null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#findModsData(java.util.Properties)
	 */
	public ModsDataSet[] findModsData(Properties modsData) throws IOException {
		
		//	build query (restrict to NHS/Antbase)
		String author = modsData.getProperty(MODS_AUTHOR_ATTRIBUTE);
		String title = modsData.getProperty(MODS_TITLE_ATTRIBUTE);
		if ((author == null) || (title == null))
			throw new IOException("Invalid query. Please specify both author name and title.");
		String query = "author_name=" + URLEncoder.encode(("%" + author), "UTF-8") + "&pub_title=" + URLEncoder.encode(("%" + title), "UTF-8");
		
		//	check attributes
		System.out.println("Query is " + query);
		
		//	get data
		URL url = new URL(this.searchBaseUrl + query);
		InputStreamReader isr = new InputStreamReader(url.openStream());
		MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
		isr.close();
		
		//	parse & return data
		QueriableAnnotation[] modsHeaders = modsDocument.getAnnotations("mods:mods");
		ArrayList mdsList = new ArrayList();
		for (int d = 0; d < modsHeaders.length; d++)
			mdsList.add(ModsUtils.getModsDataSet(modsHeaders[d]));
		return ((ModsDataSet[]) mdsList.toArray(new ModsDataSet[mdsList.size()]));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		ModsDataSource hmds = new HnsModsDataSource();
		
		Properties modsData = new Properties();
		modsData.setProperty(MODS_AUTHOR_ATTRIBUTE, "Forel");
		modsData.setProperty(MODS_TITLE_ATTRIBUTE, "Pheidole");
		
		ModsDataSet[] mdss = hmds.findModsData(modsData);
		for (int d = 0; d < mdss.length; d++)
			System.out.println(mdss[d].getTitle());
	}
}