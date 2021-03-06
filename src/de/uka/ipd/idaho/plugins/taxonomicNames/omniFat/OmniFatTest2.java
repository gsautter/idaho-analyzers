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

package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class OmniFatTest2 {
	
	public static void main(String[] args) throws Exception {
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21330.htm.xml"), "UTF-8"));
		System.out.println("Document loaded, got " + doc.size() + " tokens");
		
		AnnotationFilter.removeAnnotations(doc, OmniFAT.TAXONOMIC_NAME_TYPE);
		AnnotationFilter.removeAnnotations(doc, OmniFAT.NEW_EPITHET_LABEL_TYPE);
		
		String dataPath = "E:/GoldenGATEv3/Plugins/AnalyzerData/OmniFATData/";
		Properties parameters = new Properties();
		parameters.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
		parameters.setProperty("_omniFatInstance", "Ants&Spiders-OCR");
		
		BaseEpithetTagger bet = new BaseEpithetTagger();
		bet.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		bet.process(doc, parameters);
		System.out.println("BaseEpithetTagger done");
		
//		Annotation[] baseEpithets = doc.getAnnotations("baseEpithet");
//		for (int be = 0; be < baseEpithets.length; be++)
//			System.out.println(baseEpithets[be].toXML());
//		
//		Annotation[] abbreviatedEpithets = doc.getAnnotations("abbreviatedEpithet");
//		for (int ae = 0; ae < abbreviatedEpithets.length; ae++)
//			System.out.println(abbreviatedEpithets[ae].toXML());
//		if (true) return;
		
		
		AuthorTagger at = new AuthorTagger();
		at.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		at.process(doc, parameters);
		System.out.println("AuthorTagger done");
		
//		Annotation[] auhtorNames = doc.getAnnotations("authorName");
//		for (int an = 0; an < auhtorNames.length; an++)
//			System.out.println(auhtorNames[an].toXML());
//		if (true) return;
//		
//		Annotation[] auhtorNameStopWords = doc.getAnnotations("authorNameStopWord");
//		for (int answ = 0; answ < auhtorNameStopWords.length; answ++)
//			System.out.println(auhtorNameStopWords[answ].toXML());
//		
//		Annotation[] auhtorInitials = doc.getAnnotations("authorInitial");
//		for (int ai = 0; ai < auhtorInitials.length; ai++)
//			System.out.println(auhtorInitials[ai].toXML());
		
		
		LabelTagger lt = new LabelTagger();
		lt.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		lt.process(doc, parameters);
		System.out.println("LabelTagger done");
		
//		Annotation[] epithetLabels = doc.getAnnotations("epithetLabel");
//		for (int el = 0; el < epithetLabels.length; el++)
//			System.out.println(epithetLabels[el].toXML());
//		
//		Annotation[] newEpithetLabels = doc.getAnnotations("newEpithetLabel");
//		for (int nel = 0; nel < newEpithetLabels.length; nel++)
//			System.out.println(newEpithetLabels[nel].toXML());
		
		
		EpithetTagger et = new EpithetTagger();
		et.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		et.process(doc, parameters);
		System.out.println("EpithetTagger done");
//		
//		Annotation[] epithets = doc.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++)
//			System.out.println(epithets[e].toXML());
//		if (true) return;
		
		
		CandidateTagger ct = new CandidateTagger();
		ct.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		ct.process(doc, parameters);
		System.out.println("CandidateTagger done");
		
		
//		QueriableAnnotation[] candidates = doc.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < candidates.length; c++) {
////			if (candidates[c].size() > 2)
//				System.out.println(candidates[c].toXML());
////			Annotation[] nameParts = names[n].getAnnotations();
////			for (int p = 0; p < nameParts.length; p++) {
////				String namePartType = nameParts[p].getType();
//////				if ("baseEpithet".equals(namePartType) || "abbreviatedEpithet".equals(namePartType) || "authorName".equals(namePartType))
//////					System.out.println("  " + nameParts[p].toXML());
////				if ("epithet".equals(namePartType) || "authorName".equals(namePartType))
////					System.out.println("  " + nameParts[p].toXML());
////			}
//		}
//		if (true) return;
		
		
		PrecisionRules pr = new PrecisionRules();
		pr.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		pr.process(doc, parameters);
		System.out.println("PrecisionRules done");
//		if (true) return;
		
		
		AuthorNameRules anr = new AuthorNameRules();
		anr.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		anr.process(doc, parameters);
		System.out.println("AuthorNameRules done");
		
		
		DataRules dr = new DataRules();
		dr.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		dr.process(doc, parameters);
		System.out.println("DataRules done");
//		if (true) return;
		
		
//		QueriableAnnotation[] names = doc.getAnnotations("taxonName");
////		int nextStart = 0;
//		for (int n = 0; n < names.length; n++) {
////			if (true || (names[n].getStartIndex() >= nextStart)) {
//				System.out.println(names[n].toXML());
//				String source = ((String) names[n].getAttribute("source", ""));
//				Annotation[] epithets = names[n].getAnnotations("epithet");
//				for (int e = 0; e < epithets.length; e++) {
//					if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//						System.out.println("  " + epithets[e].toXML());
//				}
////				nextStart = names[n].getEndIndex();
////			}
//		}
		
		EpithetCompleter ec = new EpithetCompleter();
		ec.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		ec.process(doc, parameters);
		System.out.println("EpithetCompleter done");
		
		
		EpithetRanker er = new EpithetRanker();
		er.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		er.process(doc, parameters);
		System.out.println("EpithetRanker done");
		
		
		NameCompleter nc = new NameCompleter();
		nc.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
		nc.process(doc, parameters);
		System.out.println("NameCompleter done");
//		if (true) return;
		
		
//		OmniFatTeacher oft = new OmniFatTeacher();
//		oft.setDataProvider(new AnalyzerDataProviderFileBased(new File(dataPath)));
//		oft.process(doc, new Properties() {
//			public synchronized boolean containsKey(Object key) {
//				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
//			}
//		});
//		oft.exit();
//		System.out.println("OmniFatTeacher done");
//		OmniFAT.shutDownOmniFAT();
		
		
		QueriableAnnotation[] allNames = doc.getAnnotations();
		QueriableAnnotation lastName = null;
		boolean printDetails = true;
		for (int n = 0; n < allNames.length; n++) {
			String type = allNames[n].getType();
			if (OmniFAT.TAXONOMIC_NAME_TYPE.equals(type)) {
				System.out.println("T: " + allNames[n].getValue() + " (" + OmniFAT.DataRule.getEpithetStatusString(allNames[n]) + ")");
				if (printDetails) {
					System.out.println("    " + allNames[n].toXML());
					String source = ((String) allNames[n].getAttribute("source", ""));
					Annotation[] epithets = allNames[n].getAnnotations("epithet");
					for (int e = 0; e < epithets.length; e++) {
						if (source.indexOf(epithets[e].getAnnotationID()) != -1)
							System.out.println("      " + epithets[e].toXML());
					}
				}
			}
			else if (OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE.equals(type)) {
				String ess = OmniFAT.DataRule.getEpithetStatusString(allNames[n]);
				System.out.println("  F: " + allNames[n].getValue() + " (" + ess + ")");
				if (printDetails) {
					System.out.println("    " + allNames[n].toXML());
					String source = ((String) allNames[n].getAttribute("source", ""));
					Annotation[] epithets = allNames[n].getAnnotations("epithet");
					for (int e = 0; e < epithets.length; e++) {
						if (source.indexOf(epithets[e].getAnnotationID()) != -1)
							System.out.println("      " + epithets[e].toXML());
					}
				}
			}
			else type = null;
			
			//	FOR TRACKING OVERLAPPING POSITIVES
			if ((type != null) && (lastName != null) && AnnotationUtils.overlaps(lastName, allNames[n])) {
				System.out.println("    " + lastName.toXML());
				String lastSource = ((String) lastName.getAttribute("source", ""));
				Annotation[] lastEpithets = lastName.getAnnotations("epithet");
				for (int e = 0; e < lastEpithets.length; e++) {
					if (lastSource.indexOf(lastEpithets[e].getAnnotationID()) != -1)
						System.out.println("      " + lastEpithets[e].toXML());
				}
				System.out.println("    " + allNames[n].toXML());
				String source = ((String) allNames[n].getAttribute("source", ""));
				Annotation[] epithets = allNames[n].getAnnotations("epithet");
				for (int e = 0; e < epithets.length; e++) {
					if (source.indexOf(epithets[e].getAnnotationID()) != -1)
						System.out.println("      " + epithets[e].toXML());
				}
			}
			
//			//	THIS IS FOR TARGETING SPECIFIC CASES
//			if ((type != null) && "Solenopsis".equals(allNames[n].firstValue())) {
//				System.out.println("!    " + allNames[n].toXML());
//				String source = ((String) allNames[n].getAttribute("source", ""));
//				Annotation[] epithets = allNames[n].getAnnotations("epithet");
//				for (int e = 0; e < epithets.length; e++) {
//					if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//						System.out.println("!      " + epithets[e].toXML());
//				}
//			}
			
			if (OmniFAT.TAXONOMIC_NAME_TYPE.equals(type))
				lastName = allNames[n];
		}
	}
//	
//	private static  Properties stateStringCharacters = new Properties();
//	static {
//		stateStringCharacters.setProperty("positive", "p");
//		stateStringCharacters.setProperty("abbreviated", "b");
//		stateStringCharacters.setProperty("ambiguous", "a");
//		stateStringCharacters.setProperty("uncertain", "u");
//		stateStringCharacters.setProperty("negative", "n");
//	}
//	private static String getEpithetStateString(QueriableAnnotation taxonNameCandidate) {
//		StringBuffer stateString = new StringBuffer();
//		String source = ((String) taxonNameCandidate.getAttribute("source", ""));
//		QueriableAnnotation[] epithets = taxonNameCandidate.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//				String state = ((String) epithets[e].getAttribute("state"));
//				if (state == null)
//					state = "uncertain";
//				stateString.append(stateStringCharacters.getProperty(state, "u"));
//			}
//		return stateString.toString();
//	}
}
