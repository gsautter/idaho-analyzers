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

package de.uka.ipd.idaho.plugins.pageNormalization;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class CaptionReferenceResolver extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String CAPTION_START_STRING = "figure;figures;table;tables;diagram;diagrams";
	private static final String CAPTION_START_ABBREVIATIONS_STRING = "fig;figs;tab;tabs;diag;diags";
	
	private StringVector captionStarts;
	private StringVector captionStartAbbreviations;
	
//	private HashMap captionStartClasses = new HashMap();
//	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load caption starts
		try {
			InputStream is = this.dataProvider.getInputStream("captionStarts.txt");
			this.captionStarts = StringVector.loadList(is);
			is.close();
		}
		catch (IOException ioe) {
			this.captionStarts = new StringVector();
			this.captionStarts.parseAndAddElements(CAPTION_START_STRING, ";");
		}
		
		//	load caption start abbreviations
		try {
			InputStream is = this.dataProvider.getInputStream("captionStartAbbreviations.txt");
			this.captionStartAbbreviations = StringVector.loadList(is);
			is.close();
		}
		catch (IOException ioe) {
			this.captionStartAbbreviations = new StringVector();
			this.captionStartAbbreviations.parseAndAddElements(CAPTION_START_ABBREVIATIONS_STRING, ";");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get captions
		MutableAnnotation[] captions = data.getMutableAnnotations(CAPTION_TYPE);
		if (captions.length == 0)
			return;
		
		//	collect document caption starts
		StringVector captionStarts = new StringVector();
		for (int c = 0; c < captions.length; c++)
			captionStarts.addElementIgnoreDuplicates(captions[c].firstValue().toLowerCase());
		
		//	add known caption starts and abbreviations
		captionStarts.addContentIgnoreDuplicates(this.captionStarts);
		captionStarts.addContentIgnoreDuplicates(this.captionStartAbbreviations);
		
		//	index caption start synonym classes
		captionStarts.sortByLength(false);
		HashMap captionStartClasses = new HashMap();
		for (int c = 0; c < captionStarts.size(); c++) {
			String captionStart = captionStarts.get(c).toLowerCase();
			if (captionStartClasses.containsKey(captionStart))
				continue;
			
			//	create caption start class
			StringVector captionStartClass = new StringVector(false);
			captionStartClass.addElement(captionStart);
			
			//	collect synonyms
			for (int s = (c+1); s < captionStarts.size(); s++) {
				String captionStartSynonymCandidate = captionStarts.get(s).toLowerCase();
				if (captionStartSynonymCandidate.startsWith(captionStart) || Gamta.isAbbreviationOf(captionStartSynonymCandidate, captionStart, true))
					captionStartClass.addElement(captionStartSynonymCandidate);
			}
			
			//	index caption start class by all strings it contains
			for (int s = 0; s < captionStartClass.size(); s++)
				captionStartClasses.put(captionStartClass.get(s), captionStartClass);
		}
		
		//	collect caption starts and associated numbers (observe number ranges !!!)
		HashMap captionClasses = new HashMap();
		for (int c = 0; c < captions.length; c++) {
			String captionStart = captions[c].firstValue().toLowerCase();
			StringVector captionStartClass = ((StringVector) captionStartClasses.get(captionStart));
			if (captionStartClass == null)
				continue;
			CaptionClass captionClass = ((CaptionClass) captionClasses.get(captionStart));
			if (captionClass == null) {
				captionClass = new CaptionClass(captionStartClass);
				for (int s = 0; s < captionClass.starts.size(); s++)
					captionClasses.put(captionClass.starts.get(s), captionClass);
			}
			
			//	get and index numbers
			Annotation[] numberings = Gamta.extractAllMatches(captions[c], "[1-9][0-9]*(\\s*(\\-|\\&)\\s*[1-9][0-9]*)?");
			if (numberings.length != 0) {
				
				//	single number
				if (numberings[0].size() == 1)
					captionClass.numbers.put(new Integer(numberings[0].firstValue()), captions[c]);
				else {
					int fn = Integer.parseInt(numberings[0].firstValue());
					int ln = Integer.parseInt(numberings[0].lastValue());
					
					//	short number enumeration
					if (numberings[0].valueAt(1).equals("&") && ((fn+1) == ln)) {
						captionClass.numbers.put(new Integer(fn), captions[c]);
						captionClass.numbers.put(new Integer(ln), captions[c]);
					}
					
					//	number range
					else if (numberings[0].valueAt(1).equals("-") && (fn < ln)) {
						for (int n = fn; n <= ln; n++)
							captionClass.numbers.put(new Integer(n), captions[c]);
					}
				}
			}
			//	TODO also observe Roman numbers and index letters ==> collect all numberings first, and then use only first one (closest to caption start)
		}
		
		//	collect captions start indices to exclude them while referencing
		HashSet captionStartIndices = new HashSet();
		for (int c = 0; c < captions.length; c++)
			captionStartIndices.add(new Integer(captions[c].getStartIndex()));
		
		//	get numberings
		Annotation[] numberings = Gamta.extractAllMatches(data, "[1-9][0-9]*(\\s*(\\-|\\&)\\s*[1-9][0-9]*)?(\\,\\s*[1-9][0-9]*(\\s*(\\-|\\&)\\s*[1-9][0-9]*)?)*");
		
		//	mark numberings associated with a known caption start
		for (int n = 0; n < numberings.length; n++) {
			int csi = (numberings[n].getStartIndex() - 1);
			
			//	check for caption reference start
			if (csi < 0)
				continue;
			if (data.valueAt(csi).equals("."))
				csi--;
			if (csi < 0)
				continue;
			
			//	exclude starts of captions proper
			if (captionStartIndices.contains(new Integer(csi)))
				continue;
			
			//	find respective caption class
			CaptionClass captionClass = ((CaptionClass) captionClasses.get(data.valueAt(csi).toLowerCase()));
			if (captionClass == null)
				continue;
			
			//	we (likely) have an actual caption reference, or a range or enumeration thereof
			for (int v = numberings[n].getStartIndex(); v < numberings[n].getEndIndex(); v++) {
				
				//	jump punctuation marks
				if (!data.valueAt(v).matches("[1-9][0-9]*"))
					continue;
				
				//	get referenced caption
				Annotation caption = ((Annotation) captionClass.numbers.get(new Integer(data.valueAt(v))));
				if (caption == null)
					continue;
				
				//	TODO figure out what to annotate for middle part of number ranges
				
				//	annotate caption reference and link to caption
				Annotation cra = data.addAnnotation("captionReference", v, 1);
				cra.setAttribute("captionText", TokenSequenceUtils.concatTokens(caption, true, true));
				cra.setAttribute("captionPageId", caption.getAttribute(PAGE_ID_ATTRIBUTE));
				cra.setAttribute("captionPageNumber", caption.getAttribute(PAGE_NUMBER_ATTRIBUTE));
			}
		}
	}
	
	private class CaptionClass {
		StringVector starts;
		HashMap numbers = new HashMap();
		CaptionClass(StringVector starts) {
			this.starts = starts;
		}
	}
}