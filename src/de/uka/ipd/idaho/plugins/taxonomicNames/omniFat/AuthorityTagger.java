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

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;

/**
 * @author sautter
 */
public class AuthorityTagger extends AbstractAnalyzer {

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	process individual taxonomic names
		MutableAnnotation[] taxonNames = data.getMutableAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		for (int t = 0; t < taxonNames.length; t++) {
			String lowestEpithet = ((String) taxonNames[t].getAttribute(((String) taxonNames[t].getAttribute(OmniFAT.RANK_ATTRIBUTE))));
			if (lowestEpithet == null)
				continue;
			if (taxonNames[t].lastValue().equals(lowestEpithet))
				continue;
			int as = TokenSequenceUtils.lastIndexOf(taxonNames[t], lowestEpithet);
			if (as == -1)
				continue;
			as++;
			while ((as < taxonNames[t].size()) && (Gamta.isOpeningBracket(taxonNames[t].valueAt(as)) || Gamta.isClosingBracket(taxonNames[t].valueAt(as))))
				as++;
			if (as == taxonNames[t].size())
				continue;
			int ae = taxonNames[t].size();
			while ((as < ae) && Gamta.isClosingBracket(taxonNames[t].valueAt(ae-1)))
				ae--;
			if (ae <= as)
				continue;
			Annotation aa = taxonNames[t].addAnnotation(OmniFAT.AUTHORITY_TYPE, as, (ae-as));
			if (aa.lastValue().matches("[12][0-9]{3}")) {
				int ane = aa.size()-1;
				while ((ane != 0) && Gamta.isPunctuation(aa.valueAt(ane-1)))
					ane--;
				taxonNames[t].setAttribute(OmniFAT.AUTHORITY_NAME_TYPE, TokenSequenceUtils.concatTokens(aa, 0, ane, true, true));
				taxonNames[t].setAttribute(OmniFAT.AUTHORITY_YEAR_TYPE, aa.lastValue());
			}
			else taxonNames[t].setAttribute(OmniFAT.AUTHORITY_NAME_TYPE, TokenSequenceUtils.concatTokens(aa, true, true));
		}
	}
}
