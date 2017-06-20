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
public class LabelTagger extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.tagLabels(data, this.getDataSet(data, parameters));
		
//		OmniFAT omniFat = this.getOmniFatInstance(parameters);
//		
//		//	get ranks
//		OmniFAT.Rank[] ranks = omniFat.getRanks();
//		
//		//	annotate epithet labels for each rank
//		for (int r = 0; r < ranks.length; r++) {
//			
//			//	annotate epithet labels
//			Annotation[] epithetLabels = Gamta.extractAllContained(data, ranks[r].getLabels(), true);
//			for (int e = 0; e < epithetLabels.length; e++) {
//				
//				//	ignore main ranks (only appear in new epithet labels)
//				if (ranks[r].isEpithetsUnlabeled())
//					continue;
//				
//				Annotation epithetLabel = data.addAnnotation("epithetLabel", epithetLabels[e].getStartIndex(), epithetLabels[e].size());
//				epithetLabel.setAttribute("rank", ranks[r].getName());
//				epithetLabel.setAttribute("rankNumber", ("" + ranks[r].getOrderNumber()));
//				epithetLabel.setAttribute("rankGroup", ranks[r].getRankGroup().getName());
//				epithetLabel.setAttribute("rankGroupNumber", ("" + ranks[r].getRankGroup().getOrderNumber()));
//			}
//			
//			//	annotate new epithet labels
//			Annotation[] newEpithetLabels = Gamta.extractAllContained(data, ranks[r].getNewEpithetLables(), true);
//			for (int e = 0; e < newEpithetLabels.length; e++) {
//				Annotation epithetLabel = data.addAnnotation("newEpithetLabel", newEpithetLabels[e].getStartIndex(), newEpithetLabels[e].size());
//				epithetLabel.setAttribute("rank", ranks[r].getName());
//				epithetLabel.setAttribute("rankNumber", ("" + ranks[r].getOrderNumber()));
//				epithetLabel.setAttribute("rankGroup", ranks[r].getRankGroup().getName());
//				epithetLabel.setAttribute("rankGroupNumber", ("" + ranks[r].getRankGroup().getOrderNumber()));
//			}
//		}
//		
//		//	filter out epithet labels nested in new epithet labels
//		AnnotationFilter.removeContained(data, "newEpithetLabel", "epithetLabel");
	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	get ranks
//		String[] ranks = OmniFAT.getRanks();
//		
//		//	annotate epithet labels for each rank
//		for (int r = 0; r < ranks.length; r++) {
//			
//			//	get dictionary
//			Dictionary epithetLabelDictionary = OmniFAT.getEpithetLabels(ranks[r]);
//			
//			//	get rank group
//			String rankGroup = OmniFAT.getRankGroup(ranks[r]);
//			
//			//	apply dictionary
//			Annotation[] epithetLabels = Gamta.extractAllContained(data, epithetLabelDictionary, true);
//			for (int e = 0; e < epithetLabels.length; e++) {
//				
//				String epithetLabelValue = epithetLabels[e].getValue();
//				
//				//	ignore main ranks (only appear in new epithet labels)
//				if (GENUS_ATTRIBUTE.equals(epithetLabelValue) || SPECIES_ATTRIBUTE.equals(epithetLabelValue))
//					continue;
//				
//				Annotation epithetLabel = data.addAnnotation("epithetLabel", epithetLabels[e].getStartIndex(), epithetLabels[e].size());
//				epithetLabel.setAttribute("rank", ranks[r]);
//				epithetLabel.setAttribute("rankNumber", ("" + OmniFAT.getRankOrderNumber(ranks[r])));
//				epithetLabel.setAttribute("rankGroup", rankGroup);
//				epithetLabel.setAttribute("rankGroupNumber", ("" + OmniFAT.getRankGroupOrderNumber(rankGroup)));
//			}
//		}
//		
//		//	annotate new epithet labels for each rank
//		for (int r = 0; r < ranks.length; r++) {
//			
//			//	get dictionary
//			Dictionary epithetLabelDictionary = OmniFAT.getNewEpithetLabels(ranks[r]);
//			
//			//	get rank group
//			String rankGroup = OmniFAT.getRankGroup(ranks[r]);
//			
//			//	apply dictionary
//			Annotation[] epithetLabels = Gamta.extractAllContained(data, epithetLabelDictionary, true);
//			for (int e = 0; e < epithetLabels.length; e++) {
//				Annotation epithetLabel = data.addAnnotation("newEpithetLabel", epithetLabels[e].getStartIndex(), epithetLabels[e].size());
//				epithetLabel.setAttribute("rank", ranks[r]);
//				epithetLabel.setAttribute("rankNumber", ("" + OmniFAT.getRankOrderNumber(ranks[r])));
//				epithetLabel.setAttribute("rankGroup", rankGroup);
//				epithetLabel.setAttribute("rankGroupNumber", ("" + OmniFAT.getRankGroupOrderNumber(rankGroup)));
//			}
//		}
//		
//		//	filter out epithet labels nested in new epithet labels
//		AnnotationFilter.removeContained(data, "newEpithetLabel", "epithetLabel");
//	}
}