/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.taxonomicNames.treeFat;

import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;

/**
 * @author sautter
 *
 */
public class TreeFatExpander extends AbstractConfigurableAnalyzer implements TaxonomicNameConstants, ImagingConstants, MonitorableAnalyzer {
	
	/** public zero-argument constructor for class loading */
	public TreeFatExpander() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		this.process(data, parameters, ProgressMonitor.dummy);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
		
		//	TODO if taxon names (mainly) in italics, expand catalog genus matches to subsequent non-cataloged potential species epithets if latter in italics as well
		//	==> helps with non-catalog species belonging to catalog genera in recent publications
		
		//	TODO if taxon names (mainly) in italics, mark potential genera and binomials (single-word and two-word italics emphases) if followed by 'new species' label or labeled sub-specific epithet
		//	TODO do this especially in (treatment) headings, which start new original descriptions
		//	==> helps with non-catalog genera in extremely recent publications
		
		//	TODO for latter, try and find family somewhere in the document to obtain higher taxonomy
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//	good test document: zt03131p051.pdf (precision version misses species and subspecies, tagging only genera, if CoL list of children unavailable or incomplete)
		//	another good one: 11468_Logunov_2010_Bul_15_85-90.pdf
		//	another good one: Kucera2013.pdf.names.norm.xml
	}
}
