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
import java.util.Properties;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsDataSource;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;

/**
 * MODS data source for BHL's data store at archive.org.
 * 
 * @author sautter
 */
public class BhlModsDataSource extends InternetArchiveModsDataSource {
	
	/**
	 * @param collectionName
	 */
	public BhlModsDataSource() {
		super("biodiversity");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsServer.dataSources.InternetArchiveModsDataSource#isValidModsID(java.lang.String)
	 */
	protected boolean isValidModsID(String modsId) {
		
		//	check if ID might come from BHL
		return modsId.matches("[a-z0-9]++");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsReferencer.dataSources.InternetArchiveModsDataSource#addOriginInfo(java.lang.StringBuffer, de.uka.ipd.idaho.gamta.QueriableAnnotation)
	 */
	protected void addOriginInfo(StringBuffer sb, QueriableAnnotation modsDataSource) throws IOException {
		//	it's all books in BHL
		this.addBookOriginInfo(sb, modsDataSource);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.modsReferencer.dataSources.InternetArchiveModsDataSource#getPdfFileSuffix()
	 */
	protected String getPdfFileSuffix() {
		//	use black & white PDFs
		return "_bw.pdf";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		ModsDataSource bmds = new BhlModsDataSource();
		
		Properties modsData = new Properties();
		modsData.setProperty(MODS_AUTHOR_ATTRIBUTE, "Forel");
		
		ModsDataSet[] mdss = bmds.findModsData(modsData);
		for (int d = 0; d < mdss.length; d++)
			System.out.println((mdss[d].isValid() ? "VALID: " : "INVALID: ") + mdss[d].getTitle());
	}
}
