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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Set;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.plugins.taxonomicNames.synonyms.SynonymLookupProvider;

/**
 * Synonym lookup provider using page-scraping NCBI.
 * 
 * @author sautter
 */
public class SynonymLookupNCBI extends SynonymLookupProvider {
	private static final String baseUrl = "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?name=";
	
	/** public zero-argument constructor for class loading */
	public SynonymLookupNCBI() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.synonyms.SynonymLookupProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "NCBI";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.synonyms.SynonymLookupProvider#loadSynonyms(java.lang.String)
	 */
	protected String[] loadSynonyms(String taxName) throws IOException {
		
		//	search taxon name
		URL ncbiUrl = new URL(baseUrl + URLEncoder.encode(taxName, "UTF-8"));
		BufferedReader ncbiBw = new BufferedReader(new InputStreamReader(ncbiUrl.openStream(), "UTF-8"));
		MutableAnnotation ncbiRes = SgmlDocumentReader.readDocument(ncbiBw);
		ncbiBw.close();
//		AnnotationUtils.writeXML(ncbiRes, new OutputStreamWriter(System.out));
		
		//	get synonyms
		QueriableAnnotation[] synonymTrs = GPath.evaluatePath(ncbiRes, "/tr[./#first = 'synonym']", null);
		if (synonymTrs.length == 0)
			return null;
		Set synonyms = new LinkedHashSet();
		for (int s = 0; s < synonymTrs.length; s++) {
			Annotation[] synonymAnnots = synonymTrs[s].getAnnotations("strong");
			if (synonymAnnots.length != 0) {
				synonyms.add(synonymAnnots[0].getValue());
//				System.out.println(synonymAnnots[0].getValue());
			}
		}
		return ((String[]) synonyms.toArray(new String[synonyms.size()]));
	}
	
	//	!!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		String taxName = "Anochetus grandidieri";
		
		//	search taxon name
		URL ncbiUrl = new URL(baseUrl + URLEncoder.encode(taxName, "UTF-8"));
		BufferedReader ncbiBw = new BufferedReader(new InputStreamReader(ncbiUrl.openStream(), "UTF-8"));
		MutableAnnotation ncbiRes = SgmlDocumentReader.readDocument(ncbiBw);
		ncbiBw.close();
		AnnotationUtils.writeXML(ncbiRes, new OutputStreamWriter(System.out));
		
		//	get synonyms
		QueriableAnnotation[] synonymTrs = GPath.evaluatePath(ncbiRes, "/tr[./#first = 'synonym']", null);
		for (int s = 0; s < synonymTrs.length; s++) {
			Annotation[] synonyms = synonymTrs[s].getAnnotations("strong");
			if (synonyms.length != 0)
				System.out.println(synonyms[0].getValue());
		}
	}
}