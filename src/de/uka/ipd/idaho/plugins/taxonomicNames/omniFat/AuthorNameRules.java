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

import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * @author sautter
 *
 */
public class AuthorNameRules extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.applyAuthorNameRules(data, this.getDataSet(data, parameters));
		
//		//	get positives
//		QueriableAnnotation[] taxonNames = data.getAnnotations("taxonName");
//		
//		//	collect author names from positives
//		StringVector docAuthorNameParts = new StringVector();
//		StringVector docAuthorNames = new StringVector();
//		for (int t = 0; t < taxonNames.length; t++) {
//			String source = ((String) taxonNames[t].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNames[t].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
////					String epithet = ((String) epithets[e].getAttribute("string"));
////					Annotation[] authorNames = epithets[e].getAnnotations("authorName");
////					for (int a = 0; a < authorNames.length; a++)
////						if (TokenSequenceUtils.indexOf(authorNames[a], epithet) == -1) {
////							docAuthorNameParts.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(authorNames[a]));
////							docAuthorNames.addElementIgnoreDuplicates(authorNames[a].getValue());
////						}
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] epithetAuthorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < epithetAuthorNames.length; a++)
//						if (epithetSource.indexOf(epithetAuthorNames[a].getAnnotationID()) != -1) {
//							docAuthorNameParts.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(epithetAuthorNames[a]));
//							docAuthorNames.addElementIgnoreDuplicates(epithetAuthorNames[a].getValue());
//						}
//				}
//		}
//		
//		//	clean author names (anything shorter than 3 letters is too dangerous)
//		for (int a = 0; a < docAuthorNameParts.size(); a++)
//			if (docAuthorNameParts.get(a).length() < 3)
//				docAuthorNameParts.remove(a--);
//		for (int a = 0; a < docAuthorNames.size(); a++)
//			if (docAuthorNames.get(a).length() < 3)
//				docAuthorNames.remove(a--);
//		
////		if (DEBUG) System.out.println("Author names:\n - " + docAuthorNames.concatStrings("\n - "));
//		
//		//	expoit author names
//		QueriableAnnotation[] taxonNameCandidates;
//		
//		//	remove candidates with sure positive author name in epithet position
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
//			Annotation killEpithet = null;
//			for (int e = 0; e < epithets.length; e++)
//				if ((source.indexOf(epithets[e].getAnnotationID()) != -1) && docAuthorNameParts.contains((String) epithets[e].getAttribute("string"))) {
//					killEpithet = epithets[e];
//					e = epithets.length;
//				}
//			
//			if (killEpithet != null) {
//				if (DEBUG) System.out.println("Removing for author name epithet '" + killEpithet.toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//				data.removeAnnotation(taxonNameCandidates[c]);
//			}
//		}
//		
//		//	promote epithets with known author names
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					if ("positive".equals(epithets[e].getAttribute("state")))
//						continue;
//					
//					String epithetSource = ((String) epithets[e].getAttribute("source", ""));
//					Annotation[] authorNames = epithets[e].getAnnotations("authorName");
//					for (int a = 0; a < authorNames.length; a++)
//						if ((epithetSource.indexOf(authorNames[a].getAnnotationID()) != -1) && docAuthorNames.contains(authorNames[a].getValue())) {
//							if (DEBUG) System.out.println("Promoting epithet for known author name '" + authorNames[a].toXML() + "':\n  " + epithets[e].toXML());
//							epithets[e].setAttribute("state", "positive");
//							epithets[e].setAttribute("evidence", "author");
//						}
//				}
//		}
	}
}