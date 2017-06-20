/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.taxonomicNames.synonyms.providers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.plugins.taxonomicNames.synonyms.SynonymLookupProvider;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Synonym lookup provider using Hymenoptera Name Server API.
 * 
 * @author sautter
 */
public class SynonymLookupHNS extends SynonymLookupProvider {
	private static final String hnsApiKey = "1818582C2C965A7BE0530100007F6220";
	
	/** zero-argument constructor for class loading */
	public SynonymLookupHNS() {}
	
	/* (non-Javadoc)
	 * @see org.plazi.test.hackathon201506.SynonymLookupProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "HNS";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.synonyms.SynonymLookupProvider#loadSynonyms(java.lang.String)
	 */
	protected String[] loadSynonyms(String taxName) throws IOException {
		
		//	get name number
		int taxNameNumber = this.loadTaxNameNumber(taxName);
		if (taxNameNumber < 0)
			return null;
		
		//	get name ID
		URL nameDataUrl = this.dataProvider.getURL("http://xbiod.osu.edu/OJ_Break/getTaxonSynonyms?format=json&key=" + hnsApiKey + "&version=2&tnuid=" + taxNameNumber);
		Reader nameDataReader = new BufferedReader(new InputStreamReader(nameDataUrl.openStream(), "UTF-8"));
		Object nameData = JsonParser.parseJson(nameDataReader);
		nameDataReader.close();
		
		//	get list of synonyms
		if (!(nameData instanceof Map))
			return null;
		Map data = JsonParser.getObject(((Map) nameData), "data");
		if (data == null)
			return null;
		List synonyms = JsonParser.getArray(data, "synonyms");
		if ((synonyms == null) || (synonyms.size() == 0))
			return null;
		
		//	collect synonym strings
		StringVector synonymList = new StringVector();
		for (int s = 0; s < synonyms.size(); s++) {
			Map synonym = JsonParser.getObject(synonyms, s);
			if (synonym == null)
				continue;
			String synStr = JsonParser.getString(synonym, "taxon");
			if (synStr != null)
				synonymList.addElementIgnoreDuplicates(synStr);
		}
		
		//	finally ...
		synonymList.sortLexicographically();
		return synonymList.toStringArray();
	}
	
	/*
	1. Append URL escaped taxon name to http://osuc.biosci.ohio-state.edu/hymenoptera/nomenclator.name_entry?text_entry=
	==> overview of possible meanings
	==> go through top level TD elements (ones starting with 'Results'), find one not containing 'Invalid', and get URL from link texted 'Additional information'
	= nothing found => go through all top level TD elements (ones starting with 'Results'), and get URL from link following 'Valid name:'
	  ==> got valid name string to recurse with
	==> got taxon name ID
	 */
	private int loadTaxNameNumber(String taxName) throws IOException {
		
		//	get name ID data
		URL nameDataUrl = this.dataProvider.getURL("http://xbiod.osu.edu/OJ_Break/getTaxaFromText?format=json&key=" + hnsApiKey + "&version=2&search=" + URLEncoder.encode(taxName, "UTF-8"));
		Reader nameDataReader = new BufferedReader(new InputStreamReader(nameDataUrl.openStream(), "UTF-8"));
		Object nameData = JsonParser.parseJson(nameDataReader);
		nameDataReader.close();
		
		//	get taxa list
		if (!(nameData instanceof Map))
			return -1;
		Map data = JsonParser.getObject(((Map) nameData), "data");
		if (data == null)
			return -1;
		List taxa = JsonParser.getArray(data, "taxa");
		if ((taxa == null) || (taxa.size() == 0))
			return -1;
		
		//	get taxon (prefer valid ones)
		int firstTnuid = -1;
		for (int t = 0; t < taxa.size(); t++) {
			Map taxon = JsonParser.getObject(taxa, t);
			if (taxon == null)
				continue;
			
			//	get TNUID
			Number tnuid = JsonParser.getNumber(taxon, "tnuid");
			if (tnuid == null)
				continue;
//			System.out.println("Got taxon ID " + tnuid);
			
			//	get valid TNUID
			int validTnuid = this.getValidNameTnuid(tnuid.intValue());
//			System.out.println("Valid taxon ID is " + validTnuid);
			
			//	this one's unambiguous
			if (taxa.size() == 1) {
//				System.out.println("==> only choice");
				return validTnuid;
			}
			
			//	use TNUID of valid taxon right away
			if (validTnuid == tnuid.intValue()) {
//				System.out.println("==> valid, we're done");
				return validTnuid;
			}
			else {
//				System.out.println("==> invalid, skipping for now");
				if (firstTnuid == -1)
					firstTnuid = validTnuid;
			}
		}
		
		//	no valid name found, use first in list (if any)
//		System.out.println("Falling back to first taxon");
		return firstTnuid;
	}
	
	private int getValidNameTnuid(int tnuid) throws IOException {
		
		//	get taxon data
		URL taxonDataUrl = this.dataProvider.getURL("http://xbiod.osu.edu/OJ_Break/getTaxonInfo?format=json&key=" + hnsApiKey + "&version=2&tnuid=" + tnuid);
		Reader taxonDataReader = new BufferedReader(new InputStreamReader(taxonDataUrl.openStream(), "UTF-8"));
		Object taxonData = JsonParser.parseJson(taxonDataReader);
		taxonDataReader.close();
		
		//	test validity
		if (!(taxonData instanceof Map))
			return tnuid;
		Map data = JsonParser.getObject(((Map) taxonData), "data");
		if (data == null)
			return tnuid;
		if ("Y".equalsIgnoreCase(JsonParser.getString(data, "valid")))
			return tnuid;
		Map validTaxon = JsonParser.getObject(data, "valid_taxon");
		if (validTaxon == null)
			return tnuid;
		Number validTnuid = JsonParser.getNumber(validTaxon, "tnuid");
		return ((validTnuid == null) ? tnuid : validTnuid.intValue());
	}
	
	//	!!! TEST ONLY !!!
	public static void main(String[] args) throws Exception{
		SynonymLookupProvider slp = new SynonymLookupHNS();
		slp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/TaxonSynonymsData/HNS")));
		String taxName = "Anochetus madecassus";
		String[] synonyms = slp.getSynonyms(taxName, true);
		System.out.println(taxName);
		if (synonyms != null) {
			for (int s = 0; s < synonyms.length; s++)
				System.out.println("  " + synonyms[s]);
		}
	}
}