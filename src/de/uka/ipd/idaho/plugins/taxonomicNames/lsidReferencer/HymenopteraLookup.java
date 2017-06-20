///*
// * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer;
//
//
//import java.util.Properties;
//
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
//import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
//
///**
// * @author sautter
// *
// */
//public class HymenopteraLookup extends AbstractAnalyzer implements TaxonomicNameConstants {
//	
//	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	get taxonomic names
//		Annotation[] taxonomicNames = data.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		
//		//	process taxonomic names
//		for (int t = 0; t < taxonomicNames.length; t++) {
//			Annotation taxonomicName = taxonomicNames[t];
//			
//			//	get attributes
//			String genus = taxonomicName.getAttribute(GENUS_ATTRIBUTE, "").toString();
//			String species = taxonomicName.getAttribute(SPECIES_ATTRIBUTE, "").toString();
//			
//			//	build query
//			if ((genus.length() != 0) && (species.length() != 0)) {
//				String query = "http://atbi.biosci.ohio-state.edu:210/hymenoptera/nomenclator.name_entry?text_entry=" + genus + "+" + species;
//				try {
//					String result = IoTools.getPage(query);
//					boolean nameValid = result.indexOf("<CENTER><STRONG>Results for the ") != -1;
//					if (nameValid) {
//						taxonomicName.setAttribute("hymenoperaCheck", "OK");
//					} else {
//						taxonomicName.setAttribute("hymenoperaCheck", "Failed");
//					}
//				} catch (Exception ioe) {
//					taxonomicName.setAttribute("hymenoperaCheck", "Error");
//				}
//			}
//		}
//	}
//	
//	
//}
