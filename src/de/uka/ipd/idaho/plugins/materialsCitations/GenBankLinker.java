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

package de.uka.ipd.idaho.plugins.materialsCitations;


import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.goldenGateServer.srs.webPortal.SearchPortalConstants.SearchResultLinker;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;

/**
 * @author sautter
 *
 */
public class GenBankLinker extends SearchResultLinker implements MaterialsCitationConstants {
	
	/* (non-Javadoc)
	 * @see de.goldenGateSrs.webPortal.SearchPortalConstants.SearchResultLinker#getName()
	 */
	public String getName() {
		return "GeneBank Linker";
	}
	
	/* (non-Javadoc)
	 * @see de.goldenGateSrs.webPortal.SearchPortalConstants.SearchResultLinker#getAnnotationLinks(de.gamta.Annotation)
	 */
	public SearchResultLink[] getAnnotationLinks(Annotation annotation) {
		if (MATERIALS_CITATION_ANNOTATION_TYPE.equals(annotation.getType())) {
			ArrayList linkList = new ArrayList();
			String[] ans = annotation.getAttributeNames();
			for (int a = 0; a < ans.length; a++) {
				if ("genBank".equals(ans[a])) {
					String genBankId = annotation.getAttribute(ans[a], "").toString();
					if (genBankId.length() != 0)
						linkList.add(new SearchResultLink(
								EXTERNAL_INFORMATION,
								this.getClass().getName(),
								"GB", 
								null, // TODO: insert name of icon image
								("Lookup " + genBankId),
								("http://view.ncbi.nlm.nih.gov/nucleotide/" + genBankId), 
								""
							));
				}
				else if (ans[a].startsWith("genBank-")) {
					String genBankCode = ans[a].substring(8);
					if (genBankCode.startsWith("D"))
						genBankCode = genBankCode.substring(1);
					if (genBankCode.startsWith("0"))
						genBankCode = genBankCode.substring(1);
					
					String genBankId = annotation.getAttribute(ans[a], "").toString();
					if (genBankId.length() != 0)
						linkList.add(new SearchResultLink(
								EXTERNAL_INFORMATION,
								this.getClass().getName(),
								("GB" + genBankCode), 
								null, // TODO: insert name of icon image
								("Lookup " + genBankId),
								("http://view.ncbi.nlm.nih.gov/nucleotide/" + genBankId), 
								""
							));
				}
				else if ("casent".equals(ans[a])) {
					String genBankId = annotation.getAttribute(ans[a], "").toString();
					if (genBankId.length() != 0)
						linkList.add(new SearchResultLink(
								EXTERNAL_INFORMATION,
								this.getClass().getName(),
								"CASC", 
								null, // TODO: insert name of icon image
								("Lookup " + genBankId),
								("http://antweb.org/specimen.do?name=" + genBankId), 
								""
							));
				}
				else if (ans[a].startsWith("casent-")) {
					String genBankCode = ans[a].substring(8);
					if (genBankCode.startsWith("D"))
						genBankCode = genBankCode.substring(1);
					if (genBankCode.startsWith("0"))
						genBankCode = genBankCode.substring(1);
					
					String genBankId = annotation.getAttribute(ans[a], "").toString();
					if (genBankId.length() != 0)
						linkList.add(new SearchResultLink(
								EXTERNAL_INFORMATION,
								this.getClass().getName(),
								("CASC" + genBankCode), 
								null, // TODO: insert name of icon image
								("Lookup " + genBankId),
								("http://antweb.org/specimen.do?name=" + genBankId), 
								""
							));
				}
				else if ("cas".equals(ans[a])) {
					String genBankId = annotation.getAttribute(ans[a], "").toString();
					if (genBankId.length() != 0)
						linkList.add(new SearchResultLink(
								EXTERNAL_INFORMATION,
								this.getClass().getName(),
								"CAS", 
								null, // TODO: insert name of icon image
								("Lookup " + genBankId),
								("http://research.calacademy.org/research/entomology/typesdb/details.asp?number=" + genBankId), 
								""
							));
				}
			}
			
			return ((SearchResultLink[]) linkList.toArray(new SearchResultLink[linkList.size()]));
		}
		else return new SearchResultLink[0];
		
		//	TODO: make individual providers configurable, using attribute name, letter code and URL prefix for each one. Then, rename to SpecimenCodeLinker
	}
}