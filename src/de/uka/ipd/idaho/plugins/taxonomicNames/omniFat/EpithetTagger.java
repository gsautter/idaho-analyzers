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
public class EpithetTagger extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.tagEpithets(data, this.getDataSet(data, parameters));
		
//		OmniFAT omniFat = this.getOmniFatInstance(parameters);
//		Annotation[] epithets;
//		
//		//	join labels and base epithets
//		do {
//			epithets = OmniFatFunctions.joinAdjacent(data, "epithetLabel", "baseEpithet", "epithet", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					String rightRank = ((String) rightSource.getAttribute("rank"));
//					if ((rightRank != null) && !rightRank.equals(leftSource.getAttribute("rank")))
//						return null;
//					if (!rightSource.getAttribute("rankGroup").equals(leftSource.getAttribute("rankGroup")))
//						return null;
//					
//					joint.copyAttributes(rightSource);
//					joint.copyAttributes(leftSource); // rank of label overrules rank of base epithet
//					joint.setAttribute("evidence", "label");
//					joint.setAttribute("state", "positive");
//					
//					String rsid = ((String) rightSource.getAttribute("source"));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					joint.setAttribute("source", rsid);
//					
//					return joint;
//				}
//			});
//			for (int e = 0; e < epithets.length; e++)
//				data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size()).copyAttributes(epithets[e]);
//		} while (epithets.length != 0);
//		
//		
//		//	join labels and abbreviated epithets
//		do {
//			epithets = OmniFatFunctions.joinAdjacent(data, "epithetLabel", "abbreviatedEpithet", "epithet", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					String rightRank = ((String) rightSource.getAttribute("rank"));
//					if ((rightRank != null) && !rightRank.equals(leftSource.getAttribute("rank")))
//						return null;
//					if (!rightSource.getAttribute("rankGroup").equals(leftSource.getAttribute("rankGroup")))
//						return null;
//					
//					joint.copyAttributes(rightSource);
//					joint.copyAttributes(leftSource); // rank of label overrules rank of base epithet
//					joint.setAttribute("evidence", "label");
//					joint.setAttribute("state", "positive");
//					
//					String rsid = ((String) rightSource.getAttribute("source"));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					joint.setAttribute("source", rsid);
//					
//					return joint;
//				}
//			});
//			for (int e = 0; e < epithets.length; e++)
//				data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size()).copyAttributes(epithets[e]);
//		} while (epithets.length != 0);
//		
//		
//		//	mark base epithets as epithets
//		epithets = data.getAnnotations("baseEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			Annotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//			epithet.copyAttributes(epithets[e]);
//		}
//		
//		//	mark abbreviated epithets as epithets
//		epithets = data.getAnnotations("abbreviatedEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			Annotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//			epithet.copyAttributes(epithets[e]);
//		}
//		
//		
//		//	join epithets with subesquent authors
//		do {
//			epithets = OmniFatFunctions.joinAdjacent(data, "epithet", "authorName", omniFat.getIntraEpithetPunctuationMarks(), "epithet", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					joint.copyAttributes(leftSource);
//					if ("recall".equals(joint.getAttribute("evidence")))
//						joint.setAttribute("evidence", "joint");
//					
//					String lsid = ((String) leftSource.getAttribute("source"));
//					if (lsid == null)
//						lsid = leftSource.getAnnotationID();
//					String rsid = rightSource.getAnnotationID();
//					String source = (lsid + "," + rsid);
//					joint.setAttribute("source", source);
//					
//					return joint;
//				}
//			});
//			for (int e = 0; e < epithets.length; e++)
//				data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size()).copyAttributes(epithets[e]);
//		} while (epithets.length != 0);
//		
//		
//		//	remove epithets with nested line breaks
//		epithets = data.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++) {
//			for (int t = 0; t < (epithets[e].size()-1); t++)
//				if (epithets[e].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
//					t = epithets[e].size();
//					data.removeAnnotation(epithets[e]);
//				}
//		}
	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		Annotation[] epithets;
//		
//		//	join labels and base epithets
//		do {
//			epithets = OmniFAT.joinAdjacent(data, "epithetLabel", "baseEpithet", "epithet");
//			for (int e = 0; e < epithets.length; e++) {
//				QueriableAnnotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//				Annotation[] innerEpithets = epithet.getAnnotations("baseEpithet");
//				Annotation[] labels = epithet.getAnnotations("epithetLabel");
//				for (int i = 0; i < innerEpithets.length; i++)
//					if (innerEpithets[i].getStartIndex() >= ((labels.length == 0) ? 0 : labels[0].getEndIndex())) {
//						epithet.copyAttributes(innerEpithets[i]);
//						i = innerEpithets.length;
//					}
//				if (labels.length != 0)
//					epithet.copyAttributes(labels[0]);
//				epithet.setAttribute("evidence", "label");
//			}
//		} while (epithets.length != 0);
//		
//		//	join labels and abbreviated epithets
//		do {
//			epithets = OmniFAT.joinAdjacent(data, "epithetLabel", "abbreviatedEpithet", "epithet");
//			for (int e = 0; e < epithets.length; e++) {
//				QueriableAnnotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//				Annotation[] innerEpithets = epithet.getAnnotations("abbreviatedEpithet");
//				if (innerEpithets.length != 0)
//					epithet.copyAttributes(innerEpithets[0]);
//				Annotation[] labels = epithet.getAnnotations("epithetLabel");
//				if (labels.length != 0)
//					epithet.copyAttributes(labels[0]);
//				epithet.setAttribute("evidence", "label");
//			}
//		} while (epithets.length != 0);
//		
//		
//		//	mark base epithets in brackets as epithets
//		Annotation[] openingBrackets = Gamta.extractAllMatches(data, "\\(", 1);
//		for (int b = 0; b < openingBrackets.length; b++)
//			data.addAnnotation("openingBracket", openingBrackets[b].getStartIndex(), openingBrackets[b].size());
//		Annotation[] closingBrackets = Gamta.extractAllMatches(data, "\\)", 1);
//		for (int b = 0; b < closingBrackets.length; b++)
//			data.addAnnotation("closingBracket", closingBrackets[b].getStartIndex(), closingBrackets[b].size());
//		epithets = OmniFAT.joinAdjacent(data, "openingBracket", "baseEpithet", "openingEpithet");
//		for (int e = 0; e < epithets.length; e++)
//			data.addAnnotation("openingEpithet", epithets[e].getStartIndex(), epithets[e].size());
//		epithets = OmniFAT.joinAdjacent(data, "openingEpithet", "closingBracket", "epithet");
//		for (int e = 0; e < epithets.length; e++) {
//			QueriableAnnotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//			Annotation[] innerEpithets = epithet.getAnnotations("baseEpithet");
//			if (innerEpithets.length != 0)
//				epithet.copyAttributes(innerEpithets[0]);
//			if (!epithet.hasAttribute("evidence"))
//				epithet.setAttribute("evidence", "pattern");
//		}
//		AnnotationFilter.removeAnnotations(data, "openingBracket");
//		AnnotationFilter.removeAnnotations(data, "closingBracket");
//		AnnotationFilter.removeAnnotations(data, "openingEpithet");
//		
//		//	mark base epithets as epithets
//		epithets = data.getAnnotations("baseEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			Annotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//			epithet.copyAttributes(epithets[e]);
//		}
//		
//		//	mark abbreviated epithets as epithets
//		epithets = data.getAnnotations("abbreviatedEpithet");
//		for (int e = 0; e < epithets.length; e++) {
//			Annotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//			epithet.copyAttributes(epithets[e]);
//		}
//		
//		//	clean up
//		AnnotationFilter.removeDuplicates(data, "epithet");
//		
//		//	join epithets with subesquent authors
//		do {
//			epithets = OmniFAT.joinAdjacent(data, "epithet", "authorName", OmniFAT.getInTaxonNamePunctuationMarks(), "epithet");
//			for (int e = 0; e < epithets.length; e++) {
//				QueriableAnnotation epithet = data.addAnnotation("epithet", epithets[e].getStartIndex(), epithets[e].size());
//				epithet.copyAttributes(epithets[e]);
//				Annotation[] innerEpithets = epithet.getAnnotations("epithet");
//				for (int i = 0; i < innerEpithets.length; i++)
//					if (innerEpithets[i].size() < epithet.size()) {
//						epithet.copyAttributes(innerEpithets[i]);
//						i = innerEpithets.length;
//					}
//			}
//		} while (epithets.length != 0);
//		
//		//	remove epithets with nested line breaks
//		epithets = data.getAnnotations("epithet");
//		for (int e = 0; e < epithets.length; e++) {
//			for (int t = 0; t < (epithets[e].size()-1); t++)
//				if (epithets[e].tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
//					t = epithets[e].size();
//					data.removeAnnotation(epithets[e]);
//				}
//		}
//	}
}