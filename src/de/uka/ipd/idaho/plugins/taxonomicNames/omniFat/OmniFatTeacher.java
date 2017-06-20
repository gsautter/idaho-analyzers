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

import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;

/**
 * @author sautter
 *
 */
public class OmniFatTeacher extends OmniFatAnalyzer {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFAT.DocumentDataSet docData = this.getDataSet(data, parameters);
		
		//	collect (possible) epithets ...
		HashSet positives = new HashSet();
		HashSet nonNegatives = new HashSet();
		OmniFAT.Rank[] ranks = docData.omniFat.getRanks();
		
		//	... from positives ...
		QueriableAnnotation[] taxonNames = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			for (int r = 0; r < ranks.length; r++) {
				String rankEpithet = ((String) taxonNames[t].getAttribute(ranks[r].getName()));
				if (rankEpithet != null) {
					positives.add(rankEpithet);
					nonNegatives.add(rankEpithet);
				}
			}
			
			//	- teaching along the way -
			docData.omniFat.teachEpithets(taxonNames[t]);
		}
		
		//	... external candidates ...
		QueriableAnnotation[] taxonNamesCandidates = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < taxonNamesCandidates.length; c++) {
			for (int r = 0; r < ranks.length; r++) {
				String rankEpithet = ((String) taxonNamesCandidates[c].getAttribute(ranks[r].getName()));
				if (rankEpithet != null)
					nonNegatives.add(rankEpithet);
			}
		}
		
		//	... and internal candidates
		QueriableAnnotation[] internalTaxonNamesCandidates = data.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		for (int c = 0; c < internalTaxonNamesCandidates.length; c++) {
			for (int r = 0; r < ranks.length; r++) {
				String rankEpithet = ((String) internalTaxonNamesCandidates[c].getAttribute(ranks[r].getName()));
				if (rankEpithet != null)
					nonNegatives.add(rankEpithet);
			}
		}
		
		//	add author names
		Dictionary authorNames = docData.getSureAuthorNameParts();
		for (StringIterator anit = authorNames.getEntryIterator(); anit.hasMoreStrings();) {
			String authorName = anit.nextString();
			if (!positives.contains(authorName))
				docData.omniFat.addAuthor(authorName);
		}
		
		//	add negatives
		for (StringIterator nit = docData.getTextTokens(); nit.hasMoreStrings();) {
			String negative = nit.nextString();
			if (!nonNegatives.contains(negative))
				docData.omniFat.addNegative(negative);
		}
	}
}
