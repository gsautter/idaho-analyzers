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
package de.uka.ipd.idaho.plugins.taxonomicNames.ipni;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 */
public class IpniAuthorityImporter extends AbstractConfigurableAnalyzer implements MonitorableAnalyzer, TaxonomicNameConstants {
	
	/** the usual zero-argument constructor for class loading */
	public IpniAuthorityImporter() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		this.process(data, parameters, ProgressMonitor.dummy);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
		
		//	get nomenclature names
		QueriableAnnotation[] taxonNames = GPath.evaluatePath(data, taxonNamePath, null);
		
		//	do IPNI lookup
		TreeMap ipniLookupCache = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		pm.setBaseProgress(0);
		pm.setMaxProgress(100);
		for (int n = 0; n < taxonNames.length; n++) {
			pm.setProgress((n * 100) / taxonNames.length);
			pm.setInfo("Getting IPNI data for '" + taxonNames[n].getValue() + "'");
			
			//	check if lookup is of any use
			if (taxonNames[n].hasAttribute("LSID-IPNI")) {
				String taxonName = (((String) taxonNames[n].getAttribute(GENUS_ATTRIBUTE)) + " " + ((String) taxonNames[n].getAttribute(SPECIES_ATTRIBUTE)));
				String taxonAuthor = ((String) taxonNames[n].getAttribute(AUTHORITY_NAME_ATTRIBUTE));
				ipniLookupCache.put((taxonName + " " + taxonAuthor), taxonNames[n]);
				pm.setInfo(" ==> done before");
				continue; // we've handled this one before
			}
			
			//	check if lookup is of any use
			if (!"Plantae".equalsIgnoreCase((String) taxonNames[n].getAttribute(KINGDOM_ATTRIBUTE))) {
				pm.setInfo(" ==> not a plant");
				continue; // unlikely we will find this one in IPNI ...
			}
			if (!SPECIES_ATTRIBUTE.equals(taxonNames[n].getAttribute(RANK_ATTRIBUTE))) {
				pm.setInfo(" ==> not a species");
				continue; // for now ...
			}
			
			//	get binomial and authority
			String taxonName = (((String) taxonNames[n].getAttribute(GENUS_ATTRIBUTE)) + " " + ((String) taxonNames[n].getAttribute(SPECIES_ATTRIBUTE)));
			String taxonAuthor = ((String) taxonNames[n].getAttribute(AUTHORITY_NAME_ATTRIBUTE));
			String taxonYear = ((String) taxonNames[n].getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
			
			//	check cache first
			Annotation dataTaxonName = ((Annotation) ipniLookupCache.get((taxonName + " " + taxonAuthor)));
			if (dataTaxonName != null) {
				taxonNames[n].setAttribute(AUTHORITY_NAME_ATTRIBUTE, dataTaxonName.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
				if (!taxonNames[n].hasAttribute(AUTHORITY_YEAR_ATTRIBUTE))
					taxonNames[n].setAttribute(AUTHORITY_YEAR_ATTRIBUTE, dataTaxonName.getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
				taxonNames[n].setAttribute("LSID-IPNI", dataTaxonName.getAttribute("LSID-IPNI"));
				pm.setInfo(" ==> cache hit");
				continue;
			}
			
			//	do lookup and add missing attributes
			try {
				IpniAttributeSet[] iass = this.getIpniAttributes(taxonName, taxonAuthor);
				if ((iass == null) || (iass.length == 0)) {
					pm.setInfo(" ==> no data found in IPNI");
					continue;
				}
				pm.setInfo(" - got " + iass.length + " IPNI records");
				
				//	resolve any ambiguity (and ensure match)
				IpniAttributeSet ias = null;
				int bestIasScore = 0;
				for (int s = 0; s < iass.length; s++) {
					int iasScore = 0;
					if (StringUtils.isAbbreviationOf(iass[s].authorityName, taxonAuthor, true))
						iasScore += 1;
					if (iass[s].authorityYear.equals(taxonYear))
						iasScore += 2;
					if (iasScore > bestIasScore) {
						ias = iass[s];
						bestIasScore = iasScore;
					}
				}
//				if (iass.length > 1) {
//					int bestIasScore = 0;
//					for (int s = 0; s < iass.length; s++) {
//						int iasScore = 0;
//						if (StringUtils.isAbbreviationOf(iass[s].authorityName, taxonAuthor, true))
//							iasScore += 1;
//						if (iass[s].authorityYear.equals(taxonYear))
//							iasScore += 2;
//						if (iasScore > bestIasScore) {
//							ias = iass[s];
//							bestIasScore = iasScore;
//						}
//					}
//				}
//				else ias = iass[0];
				
				//	do we have a match?
				if (ias == null) {
					pm.setInfo(" ==> no matching IPNI record found");
					continue;
				}
				
				//	set attributes
				taxonNames[n].setAttribute(AUTHORITY_NAME_ATTRIBUTE, ias.authorityName);
				if (!taxonNames[n].hasAttribute(AUTHORITY_YEAR_ATTRIBUTE))
					taxonNames[n].setAttribute(AUTHORITY_YEAR_ATTRIBUTE, ias.authorityYear);
				taxonNames[n].setAttribute("LSID-IPNI", ("urn:lsid:ipni.org:names:" + ias.taxonId));
				ipniLookupCache.put((taxonName + " " + taxonAuthor), taxonNames[n]);
				pm.setInfo(" ==> matched to IPNI record " + ias.authorityName + " " + ias.authorityYear + " (" + ias.taxonId + ")");
			}
			catch (Exception e) {
				System.out.println("Error getting IPNI attributes for '" + taxonNames[n].getValue() + "': " + e.getMessage());
				e.printStackTrace(System.out);
			}
		}
	}
	
	private static final GPath taxonNamePath = new GPath("//subSubSection[./@type = 'nomenclature']//taxonomicName");
	
	private static class IpniAttributeSet {
		final String authorityName;
		final String authorityYear;
		final String taxonId;
		IpniAttributeSet(String authorityName, String authorityYear, String taxonId) {
			this.authorityName = authorityName;
			this.authorityYear = authorityYear;
			this.taxonId = taxonId;
		}
	}
	
	private IpniAttributeSet[] getIpniAttributes(String taxonName, String taxonAuthor) throws IOException {
		URL url = this.dataProvider.getURL("http://www.ipni.org/ipni/simplePlantNameSearch.do?output_format=full&find_wholeName=" + URLEncoder.encode(taxonName, "UTF-8"));
		Reader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		MutableAnnotation ma = SgmlDocumentReader.readDocument(in);
		in.close();
		QueriableAnnotation[] qDataAreas = GPath.evaluatePath(ma, dataAreaPath, null);
		QueriableAnnotation qDataArea = ((qDataAreas.length == 0) ? null : qDataAreas[0]);
		if (qDataArea == null) {
			AnnotationUtils.writeXML(ma, new OutputStreamWriter(System.out));
			return null;
		}
		MutableAnnotation[] dataAreas = ma.getMutableAnnotations(qDataArea.getType());
		MutableAnnotation dataArea = null;
		for (int a = 0; a < dataAreas.length; a++)
			if (dataAreas[a].getAnnotationID().equals(qDataArea.getAnnotationID())){
				dataArea = dataAreas[a];
				break;
			}
		if (dataArea == null) {
			AnnotationUtils.writeXML(qDataArea, new OutputStreamWriter(System.out));
			return null;
		}
		Annotation[] resHeads = dataArea.getAnnotations("h3");
		for (int r = 0; r < resHeads.length; r++) {
			int end = (((r+1) == resHeads.length) ? dataArea.size() : resHeads[r+1].getStartIndex());
			dataArea.addAnnotation("result", resHeads[r].getStartIndex(), (end - resHeads[r].getStartIndex()));
		}
//		AnnotationUtils.writeXML(dataArea, new OutputStreamWriter(System.out));
		QueriableAnnotation[] results = dataArea.getAnnotations("result");
		ArrayList iasList = new ArrayList(results.length);
		for (int r = 0; r < results.length; r++) {
			AnnotationUtils.writeXML(results[r], new OutputStreamWriter(System.out));
//			GPathObject authorAuth = GPath.evaluateExpression(authorPathAuth, results[r], null);
//			System.out.println("Author (from link): " + authorAuth.asString().value);
			QueriableAnnotation[] names = GPath.evaluatePath(results[r], namePath, null);
			String authorsName;
			if (names.length != 0) {
				Annotation[] epithets = names[0].getAnnotations("i");
				if (epithets.length == 0)
					authorsName = names[0].getValue();
				else authorsName = TokenSequenceUtils.concatTokens(names[0], epithets[epithets.length-1].getEndIndex(), (names[0].size() - epithets[epithets.length-1].getEndIndex()), true, true);
			}
			else authorsName = null;
			Annotation[] authorsAuth = GPath.evaluatePath(results[r], authorPathAuth, null);
			Properties authorsAuthFull = new Properties();
			String authorsNameFull = authorsName;
			for (int a = 0; a < authorsAuth.length; a++) {
				String authorId = ((String) authorsAuth[a].getAttribute("href"));
				if (authorId == null)
					continue;
				if (authorId.indexOf("?id=") == -1)
					continue;
				authorId = authorId.substring(authorId.indexOf("?id=") + "?id=".length());
				if (authorId.indexOf("&") == -1)
					continue;
				authorId = authorId.substring(0, authorId.indexOf("&"));
				String authorAuth = this.getIpniAuthor(authorId);
				if (authorAuth != null) {
					authorsAuthFull.setProperty(authorsAuth[a].getValue(), authorAuth);
					if ((authorsNameFull != null) && !authorAuth.equals(authorsAuth[a].getValue())) {
						authorsNameFull = authorsNameFull.replace(authorsAuth[a].getValue(), authorAuth);
						authorsNameFull = authorsNameFull.replace(TokenSequenceUtils.concatTokens(authorsAuth[a], true, true), authorAuth);
					}
				}
			}
			System.out.println("Author (from name): " + authorsName);
			System.out.println("Author (from name, completed): " + authorsNameFull);
			System.out.println("Author (from links):");
			for (int a = 0; a < authorsAuth.length; a++)
				System.out.println(" - " + authorsAuth[a].getValue() + " ==> " + authorsAuthFull.getProperty(authorsAuth[a].getValue(), "<unresolved>"));
			GPathObject authorPub = GPath.evaluateExpression(authorPathPub, results[r], null);
			System.out.println("Author (to extract from publication): " + authorPub.asString().value);
			GPathObject year = GPath.evaluateExpression(yearPath, results[r], null);
			System.out.println("Year: " + year.asString().value);
			GPathObject id = GPath.evaluateExpression(idPath, results[r], null);
			System.out.println("ID: " + id.asString().value);
			if (!year.asString().value.matches("[12][0-9]{3}"))
				continue;
			if (!id.asString().value.matches("[0-9]+\\-[0-9]+"))
				continue;
			if (authorsNameFull != null)
				iasList.add(new IpniAttributeSet(authorsNameFull, year.asString().value, id.asString().value));
			else if (authorsName != null)
				iasList.add(new IpniAttributeSet(authorsName, year.asString().value, id.asString().value));
			else iasList.add(new IpniAttributeSet(taxonAuthor, year.asString().value, id.asString().value));
		}
		return ((IpniAttributeSet[]) iasList.toArray(new IpniAttributeSet[iasList.size()]));
	}
	
	private static final GPath dataAreaPath = new GPath("//td[./#first = 'You']//ul");
	
//	private static GPathExpression authorPathAuth = GPathParser.parseExpression("string(//h3/a[starts-with(./@href, '/ipni/idAuthorSearch')])");
	private static final GPath namePath = new GPath("//h3");
	private static final GPath authorPathAuth = new GPath("//h3/a[starts-with(./@href, '/ipni/idAuthorSearch')]");
	private static final GPathExpression authorPathPub = GPathParser.parseExpression("string(//h4/a[starts-with(./@href, '/ipni/idPublicationSearch')])");
	private static final GPathExpression yearPath = GPathParser.parseExpression("string(//span[./@class = 'ipni_bhl_year'])");
	private static final GPathExpression idPath = GPathParser.parseExpression("normalize-space(substring-after(substring-before(string(//p[./#first = 'Id']), 'Version'), ':'))");
	
	private Properties idsToIpniAuthors = new Properties();
	
	private String getIpniAuthor(String id) throws IOException {
		String authorCached = this.idsToIpniAuthors.getProperty(id);
		if (authorCached != null)
			return authorCached;
		
		URL url = this.dataProvider.getURL("http://www.ipni.org/ipni/idAuthorSearch.do?id=" + URLEncoder.encode(id, "UTF-8"));
		Reader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		MutableAnnotation ma = SgmlDocumentReader.readDocument(in);
		in.close();
//		AnnotationUtils.writeXML(ma, new OutputStreamWriter(System.out));
		GPathObject authorObj = GPath.evaluateExpression(authorPath, ma, null);
		String authorStr = authorObj.asString().value;
		System.out.println("Author (from linked page): " + authorStr);
		if (authorStr.indexOf(',') != -1)
			authorStr = authorStr.substring(0, authorStr.indexOf(','));
		this.idsToIpniAuthors.setProperty(id, authorStr);
		return authorStr;
	}
	
	private static final GPathExpression authorPath = GPathParser.parseExpression("normalize-space(string(//h3))");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//http://www.ipni.org/ipni/simplePlantNameSearch.do?find_wholeName=Epipogium+aphyllum&output_format=full
		IpniAuthorityImporter iai = new IpniAuthorityImporter();
		iai.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/IpniHierarchyData")));
		IpniAttributeSet[] iass = iai.getIpniAttributes("Cypripedium calceolus", "");
//		IpniAttributeSet[] iass = iai.getIpniAttributes("Epipogium aphyllum", null);
//		IpniAttributeSet[] iass = iai.getIpniAttributes("Limodorum abortivum", "L.");
		
		//	resolve any ambiguity
		IpniAttributeSet ias = null;
		if (iass.length > 1) {
			int bestIasScore = 0;
			for (int s = 0; s < iass.length; s++) {
				int iasScore = 0;
				if (StringUtils.isAbbreviationOf(iass[s].authorityName, "Walter", true))
					iasScore += 1;
//				if (iass[s].authorityYear.equals(taxonYear))
//					iasScore += 2;
				if (iasScore > bestIasScore) {
					ias = iass[s];
					bestIasScore = iasScore;
				}
			}
			//	TODO test this !!!
		}
		else ias = iass[0];
		
		System.out.println("Best match:");
		System.out.println(" - " + ias.authorityName);
		System.out.println(" - " + ias.authorityYear);
		System.out.println(" - " + ias.taxonId);
	}
}