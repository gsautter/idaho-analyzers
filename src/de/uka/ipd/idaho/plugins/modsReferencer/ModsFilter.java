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

package de.uka.ipd.idaho.plugins.modsReferencer;


import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.goldenGateServer.srs.AbstractStorageFilter;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class ModsFilter extends AbstractStorageFilter implements ModsConstants {
	
	/* (non-Javadoc)
	 * @see de.goldenGateScf.srs.StorageFilter#filter(de.gamta.QueriableAnnotation)
	 */
	public String filter(QueriableAnnotation doc) {
		
		//	get MODS meta data header
		QueriableAnnotation[] modsHeaders = doc.getAnnotations(MODS_MODS);
		
		//	header missing
		if (modsHeaders.length == 0)
			return "The MODS meta information is missing.";
		
		//	multiple headers
		else if (modsHeaders.length != 1)
			return "There is more than one set of MODS meta information.";
		
		//	check header content
		else {
			String[] errors = ModsUtils.getErrorReport(modsHeaders[0]);
			if (errors.length == 0)
				return null;
			
			StringVector errorReport = new StringVector();
			errorReport.addContent(errors);
			return ("The MODS meta information is incomplete " + errorReport.concatStrings(""));
		}
	}
	
	/* (non-Javadoc)
	 * @see de.goldenGateScf.srs.StorageFilter#filter(de.gamta.QueriableAnnotation[], de.gamta.QueriableAnnotation)
	 */
	public QueriableAnnotation[] filter(QueriableAnnotation[] parts, QueriableAnnotation doc) {
		QueriableAnnotation[] passingPartCollector = new QueriableAnnotation[parts.length];
		int passCount = 0;
		for (int p = 0; p < parts.length; p++) {
			if (this.filter(parts[p]) == null)
				passingPartCollector[passCount++] = parts[p];
		}
		QueriableAnnotation[] passingParts = new QueriableAnnotation[passCount];
		System.arraycopy(passingPartCollector, 0, passingParts, 0, passingParts.length);
		return passingParts;
	}
}
