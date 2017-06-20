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
public class PrecisionRules extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.applyPrecisionRules(data, this.getDataSet(data, parameters));
		
//		QueriableAnnotation[] taxonNameCandidates;
//		
//		
//		//	remove candidates whose first epithet is negative
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
//			boolean firstEpithet = true;
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					if (firstEpithet && "negative".equals(epithets[e].getAttribute("state"))) {
//						if (DEBUG) System.out.println("Removing for negative first epithet '" + epithets[e].toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//						data.removeAnnotation(taxonNameCandidates[c]);
//						e = epithets.length;
//					}
//					firstEpithet = false;
//				}
//		}
//		
//		
//		//	promote candidates succeeded by a 'new epithet' label
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		Arrays.sort(taxonNameCandidates, endIndexOrder);
//		Annotation[] newEpithetLabels = data.getAnnotations("newEpithetLabel");
//		int neli = 0;
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			while (
//					(neli < newEpithetLabels.length)
//					&&
//					(taxonNameCandidates[c].getEndIndex() > newEpithetLabels[neli].getStartIndex())
//				) neli++;
//			
//			if (
//					(neli < newEpithetLabels.length)
//					&&
//					(
//						(newEpithetLabels[neli].getStartIndex() == taxonNameCandidates[c].getEndIndex())
//						||
//						(
//							((newEpithetLabels[neli].getStartIndex() - taxonNameCandidates[c].getEndIndex()) == 1)
//							&&
//							!Gamta.isWord(data.valueAt(taxonNameCandidates[c].getEndIndex()))
//						)
//					)
//				) {
//				
//				if (this.matches(taxonNameCandidates[c], newEpithetLabels[neli])) {
//					if (DEBUG) System.out.println("Assessing promotion of candidate labeled as '" + newEpithetLabels[neli].toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//					ArrayList optionIndices = new ArrayList();
//					final HashMap options = new HashMap();
//					
//					optionIndices.add(new Integer(c));
//					options.put(new Integer(c), taxonNameCandidates[c]);
//					
//					//	collect possible alternatives
//					int l = 1;
//					while (
//							((c+l) < taxonNameCandidates.length)
//							&&
//							(
//								(taxonNameCandidates[c+l] == null)
//								||
//								(taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex())
//							)
//						) {
//						if (taxonNameCandidates[c+l] == null) {}
//						else if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[c+l], false)) {
//							if (this.matches(taxonNameCandidates[c], newEpithetLabels[neli])) {
//								if (DEBUG) System.out.println("  Keeping alternative candidate: " + taxonNameCandidates[c+l].toXML());
//								optionIndices.add(new Integer(c+l));
//								options.put(new Integer(c+l), taxonNameCandidates[c+l]);
//							}
//							else {
//								if (DEBUG) System.out.println("  Removing candidate for not matching label '" + newEpithetLabels[neli].toXML() + "':\n  " + taxonNameCandidates[c+l].toXML());
//								String source = ((String) taxonNameCandidates[c+l].getAttribute("source", ""));
//								Annotation[] epithets = taxonNameCandidates[c+l].getAnnotations("epithet");
//								for (int e = 0; e < epithets.length; e++) {
//									if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//										if (DEBUG) System.out.println("      " + epithets[e].toXML());
//								}
//								data.removeAnnotation(taxonNameCandidates[c+l]);
//								taxonNameCandidates[c+l] = null;
//							}
//						}
//						else {
//							if (DEBUG) System.out.println("  Removing nested candidate: " + taxonNameCandidates[c+l].toXML());
//							data.removeAnnotation(taxonNameCandidates[c+l]);
//							taxonNameCandidates[c+l] = null;
//						}
//						l++;
//					}
//					
//					//	no alternative epiteht compination for same piece of text
//					if (optionIndices.size() == 1) {
//						if (DEBUG) System.out.println(" ==> Promoting candidate labeled as new: " + taxonNameCandidates[c].toXML());
//						taxonNameCandidates[c].changeTypeTo("taxonName");
//						taxonNameCandidates[c].setAttribute("evidence", "newLabel");
//					}
//					
//					//	epithet combination unclear
//					else {
//						
//						//	sort by number of source annotations, descending, in order to choose candidate with most epithets
//						Comparator sourceLengthComparator = new Comparator() {
//							public int compare(Object i1, Object i2) {
//								Annotation a1 = ((Annotation) options.get(i1));
//								Annotation a2 = ((Annotation) options.get(i2));
//								return (((String) a2.getAttribute("source", "")).length() - ((String) a1.getAttribute("source", "")).length());
//							}
//						};
//						Collections.sort(optionIndices, sourceLengthComparator);
//						
//						//	undecided, keep all candidates for user to choose (should rarely happen)
//						if (sourceLengthComparator.compare(optionIndices.get(0), optionIndices.get(1)) == 0) {
//							int index;
//							
//							//	keep all top candidates
//							while ((optionIndices.size() > 1) && (sourceLengthComparator.compare(optionIndices.get(0), optionIndices.get(1)) == 0)) {
//								index = ((Integer) optionIndices.remove(1)).intValue();
//								if (DEBUG) System.out.println("  Keeping candidate: " + taxonNameCandidates[index].toXML());
//								taxonNameCandidates[index] = null;
//							}
//							index = ((Integer) optionIndices.remove(0)).intValue();
//							if (DEBUG) System.out.println("  Keeping candidate: " + taxonNameCandidates[index].toXML());
//							taxonNameCandidates[index] = null;
//							
//							//	clean up the rest
//							for (int o = 0; o < optionIndices.size(); o++) {
//								index = ((Integer) optionIndices.get(o)).intValue();
//								if (DEBUG) System.out.println("Removing inferior candidate: " + taxonNameCandidates[index].toXML());
//								data.removeAnnotation(taxonNameCandidates[index]);
//								taxonNameCandidates[index] = null;
//							}
//						}
//						
//						//	we do have a clear winner
//						else {
//							int index = ((Integer) optionIndices.get(0)).intValue();
//							if (DEBUG) System.out.println(" ==> Promoting candidate labeled as new: " + taxonNameCandidates[index].toXML());
//							taxonNameCandidates[index].changeTypeTo("taxonName");
//							taxonNameCandidates[index].setAttribute("evidence", "newLabel");
//							
//							//	clean up the rest
//							for (int o = 1; o < optionIndices.size(); o++) {
//								index = ((Integer) optionIndices.get(o)).intValue();
//								if (DEBUG) System.out.println("  Removing inferior candidate: " + taxonNameCandidates[index].toXML());
//								data.removeAnnotation(taxonNameCandidates[index]);
//								taxonNameCandidates[index] = null;
//							}
//						}
//					}
//				}
//				else {
//					if (DEBUG) {
//						System.out.println("Removing candidate for not matching label '" + newEpithetLabels[neli].toXML() + "':\n  " + taxonNameCandidates[c].toXML());
//						String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//						Annotation[] epithets = taxonNameCandidates[c].getAnnotations("epithet");
//						for (int e = 0; e < epithets.length; e++) {
//							if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//								System.out.println("    " + epithets[e].toXML());
//						}
//					}
//					data.removeAnnotation(taxonNameCandidates[c]);
//					taxonNameCandidates[c] = null;
//				}
//			}
//		}
//		
//		//	remove candidates nested in positives
//		AnnotationFilter.removeContained(data, "taxonName", "taxonNameCandidate");
//		
//		
//		//	promote candidates whose last epithet is labeled
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
//			Annotation lastEpithet = null;
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//					lastEpithet = epithets[e];
//			if ((lastEpithet != null) && "label".equals(lastEpithet.getAttribute("evidence"))) {
//				if (DEBUG) System.out.println("Assessing promotion of candidate with labeled last epithet: " + taxonNameCandidates[c].toXML());
//				ArrayList optionIndices = new ArrayList();
//				final HashMap options = new HashMap();
//				optionIndices.add(new Integer(c));
//				options.put(new Integer(c), taxonNameCandidates[c]);
//				
//				//	collect possible alternatives
//				int l = 1;
//				while (((c+l) < taxonNameCandidates.length) && ((taxonNameCandidates[c+l] == null) || (taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex()))) {
//					if (taxonNameCandidates[c+l] == null) {}
//					else if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[c+l], false)) {
//						if (DEBUG) System.out.println("  Considering alternative candidate: " + taxonNameCandidates[c+l].toXML());
//						optionIndices.add(new Integer(c+l));
//						options.put(new Integer(c+l), taxonNameCandidates[c+l]);
//					}
//					else {
//						if (DEBUG) System.out.println("  Removing nested candidate: " + taxonNameCandidates[c+l].toXML());
//						data.removeAnnotation(taxonNameCandidates[c+l]);
//						taxonNameCandidates[c+l] = null;
//					}
//					l++;
//				}
//				
//				//	no alternative epiteht compination for same piece of text
//				if (optionIndices.size() == 1) {
//					if (DEBUG) System.out.println(" ==> Promoting candidate with labeled last epithet: " + taxonNameCandidates[c].toXML());
//					taxonNameCandidates[c].changeTypeTo("taxonName");
//					taxonNameCandidates[c].setAttribute("evidence", "label");
//				}
//				
//				//	epithet combination unclear
//				else {
//					
//					//	sort by number of source annotations, descending, in order to choose candidate with most epithets
//					Comparator sourceLengthComparator = new Comparator() {
//						public int compare(Object i1, Object i2) {
//							Annotation a1 = ((Annotation) options.get(i1));
//							Annotation a2 = ((Annotation) options.get(i2));
//							return (((String) a2.getAttribute("source", "")).length() - ((String) a1.getAttribute("source", "")).length());
//						}
//					};
//					Collections.sort(optionIndices, sourceLengthComparator);
//					
//					//	undecided, keep all candidates for user to choose (should rarely happen)
//					if (sourceLengthComparator.compare(optionIndices.get(0), optionIndices.get(1)) == 0) {
//						int index;
//						
//						//	keep all top candidates
//						while ((optionIndices.size() > 1) && (sourceLengthComparator.compare(optionIndices.get(0), optionIndices.get(1)) == 0)) {
//							index = ((Integer) optionIndices.remove(1)).intValue();
//							if (DEBUG) System.out.println("  Keeping candidate: " + taxonNameCandidates[index].toXML());
//							taxonNameCandidates[index] = null;
//						}
//						index = ((Integer) optionIndices.remove(0)).intValue();
//						if (DEBUG) System.out.println("  Keeping candidate: " + taxonNameCandidates[index].toXML());
//						taxonNameCandidates[index] = null;
//						
//						//	clean up the rest
//						for (int o = 0; o < optionIndices.size(); o++) {
//							index = ((Integer) optionIndices.get(o)).intValue();
//							if (DEBUG) System.out.println("  Removing inferior candidate: " + taxonNameCandidates[index].toXML());
//							data.removeAnnotation(taxonNameCandidates[index]);
//							taxonNameCandidates[index] = null;
//						}
//					}
//					
//					//	we do have a clear winner
//					else {
//						int index = ((Integer) optionIndices.get(0)).intValue();
//						if (DEBUG) System.out.println(" ==> Promoting candidate with labeled last epithet: " + taxonNameCandidates[index].toXML());
//						taxonNameCandidates[index].changeTypeTo("taxonName");
//						taxonNameCandidates[index].setAttribute("evidence", "label");
//						
//						//	clean up the rest
//						for (int o = 1; o < optionIndices.size(); o++) {
//							index = ((Integer) optionIndices.get(o)).intValue();
//							if (DEBUG) System.out.println("  Removing inferior candidate: " + taxonNameCandidates[index].toXML());
//							data.removeAnnotation(taxonNameCandidates[index]);
//							taxonNameCandidates[index] = null;
//						}
//					}
//				}
//			}
//		}
//		
//		//	remove candidates nested in positives
//		AnnotationFilter.removeContained(data, "taxonName", "taxonNameCandidate");
//		
//		
//		//	promote candidates whose second-to-last epithet is the same as the last epithet
//		taxonNameCandidates = data.getAnnotations("taxonNameCandidate");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			if (taxonNameCandidates[c] == null)
//				continue;
//			
//			String source = ((String) taxonNameCandidates[c].getAttribute("source", ""));
//			Annotation[] epithets = ((QueriableAnnotation) taxonNameCandidates[c]).getAnnotations("epithet");
//			Annotation secondToLastEpithet = null;
//			Annotation lastEpithet = null;
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
//					secondToLastEpithet = lastEpithet;
//					lastEpithet = epithets[e];
//				}
//			if ((secondToLastEpithet != null) && (lastEpithet != null)) {
//				String stlEpithet = ((String) secondToLastEpithet.getAttribute("string"));
//				if ((stlEpithet != null) && stlEpithet.endsWith("."))
//					stlEpithet = stlEpithet.substring(0, (stlEpithet.length()-1));
//				String lEpithet = ((String) lastEpithet.getAttribute("string"));
//				if (!lEpithet.startsWith(stlEpithet))
//					continue;
//				
//				if (DEBUG) System.out.println("Assessing promotion of candidate with two equal ending epithets: " + taxonNameCandidates[c].toXML());
//				ArrayList optionIndices = new ArrayList();
//				final HashMap options = new HashMap();
//				optionIndices.add(new Integer(c));
//				options.put(new Integer(c), taxonNameCandidates[c]);
//				
//				//	collect possible alternatives
//				int l = 1;
//				while (((c+l) < taxonNameCandidates.length) && ((taxonNameCandidates[c+l] == null) || (taxonNameCandidates[c+l].getStartIndex() < taxonNameCandidates[c].getEndIndex()))) {
//					if (taxonNameCandidates[c+l] == null) {}
//					else if (AnnotationUtils.equals(taxonNameCandidates[c], taxonNameCandidates[c+l], false)) {
//						if (DEBUG) System.out.println("  Considering alternative candidate: " + taxonNameCandidates[c+l].toXML());
//						optionIndices.add(new Integer(c+l));
//						options.put(new Integer(c+l), taxonNameCandidates[c+l]);
//					}
//					else {
//						if (DEBUG) System.out.println("  Removing nested candidate: " + taxonNameCandidates[c+l].toXML());
//						data.removeAnnotation(taxonNameCandidates[c+l]);
//						taxonNameCandidates[c+l] = null;
//					}
//					l++;
//				}
//				
//				//	no alternative epiteht compination for same piece of text
//				if (optionIndices.size() == 1) {
//					if (DEBUG) System.out.println(" ==> Promoting candidate with two equal ending epithets: " + taxonNameCandidates[c].toXML());
//					taxonNameCandidates[c].changeTypeTo("taxonName");
//					taxonNameCandidates[c].setAttribute("evidence", "label");
//				}
//				
//				//	epithet combination unclear
//				else {
//					
//					//	sort by number of source annotations, descending, in order to choose candidate with most epithets
//					Comparator sourceLengthComparator = new Comparator() {
//						public int compare(Object i1, Object i2) {
//							Annotation a1 = ((Annotation) options.get(i1));
//							Annotation a2 = ((Annotation) options.get(i2));
//							return (((String) a2.getAttribute("source", "")).length() - ((String) a1.getAttribute("source", "")).length());
//						}
//					};
//					Collections.sort(optionIndices, sourceLengthComparator);
//					
//					//	undecided, keep all candidates for user to choose (should rarely happen)
//					if (sourceLengthComparator.compare(optionIndices.get(0), optionIndices.get(1)) == 0) {
//						int index;
//						
//						//	keep all top candidates
//						while ((optionIndices.size() > 1) && (sourceLengthComparator.compare(optionIndices.get(0), optionIndices.get(1)) == 0)) {
//							index = ((Integer) optionIndices.remove(1)).intValue();
//							if (DEBUG) System.out.println("  Keeping candidate: " + taxonNameCandidates[index].toXML());
//							taxonNameCandidates[index] = null;
//						}
//						index = ((Integer) optionIndices.remove(0)).intValue();
//						if (DEBUG) System.out.println("  Keeping candidate: " + taxonNameCandidates[index].toXML());
//						taxonNameCandidates[index] = null;
//						
//						//	clean up the rest
//						for (int o = 0; o < optionIndices.size(); o++) {
//							index = ((Integer) optionIndices.get(o)).intValue();
//							if (DEBUG) System.out.println("  Removing inferior candidate: " + taxonNameCandidates[index].toXML());
//							data.removeAnnotation(taxonNameCandidates[index]);
//							taxonNameCandidates[index] = null;
//						}
//					}
//					
//					//	we do have a clear winner
//					else {
//						int index = ((Integer) optionIndices.get(0)).intValue();
//						if (DEBUG) System.out.println(" ==> Promoting candidate with two equal ending epithets: " + taxonNameCandidates[index].toXML());
//						taxonNameCandidates[index].changeTypeTo("taxonName");
//						taxonNameCandidates[index].setAttribute("evidence", "label");
//						
//						//	clean up the rest
//						for (int o = 1; o < optionIndices.size(); o++) {
//							index = ((Integer) optionIndices.get(o)).intValue();
//							if (DEBUG) System.out.println("  Removing inferior candidate: " + taxonNameCandidates[index].toXML());
//							data.removeAnnotation(taxonNameCandidates[index]);
//							taxonNameCandidates[index] = null;
//						}
//					}
//				}
//			}
//		}
//		
//		//	remove candidates nested in positives
//		AnnotationFilter.removeContained(data, "taxonName", "taxonNameCandidate");
//		
//		//	promote epithets of positives
//		QueriableAnnotation[] taxonNames = data.getAnnotations("taxonName");
//		for (int t = 0; t < taxonNames.length; t++) {
//			String source = ((String) taxonNames[t].getAttribute("source", ""));
//			QueriableAnnotation[] epithets = taxonNames[t].getAnnotations("epithet");
//			for (int e = 0; e < epithets.length; e++)
//				if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//					epithets[e].setAttribute("state", "positive");
//		}
	}
//	
//	private boolean matches(QueriableAnnotation taxonNameCandidate, Annotation newEpithetLabel) {
//		String source = ((String) taxonNameCandidate.getAttribute("source", ""));
//		QueriableAnnotation[] epithets = taxonNameCandidate.getAnnotations("epithet");
//		Annotation lastEpithet = null;
//		for (int e = 0; e < epithets.length; e++)
//			if (source.indexOf(epithets[e].getAnnotationID()) != -1)
//				lastEpithet = epithets[e];
//		
//		if (lastEpithet == null)
//			return false;
//		
//		String lastEpithetRank = ((String) lastEpithet.getAttribute("rank"));
//		if ((lastEpithetRank != null) && !lastEpithetRank.equals(newEpithetLabel.getAttribute("rank")))
//			return false;
//		return lastEpithet.getAttribute("rankGroup").equals(newEpithetLabel.getAttribute("rankGroup"));
//	}
//	
//	private static Comparator endIndexOrder = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			Annotation a1 = ((Annotation) o1);
//			Annotation a2 = ((Annotation) o2);
//			int c = a1.getEndIndex() - a2.getEndIndex();
//			return ((c == 0) ? (a2.size() - a1.size()) : c);
//		}
//	};
}