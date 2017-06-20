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

package de.uka.ipd.idaho.plugins.treatmentSplitting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;

/**
 * @author sautter
 *
 */
public class TaxonListHandler extends AbstractConfigurableAnalyzer implements TreatmentConstants {
	
	private static final String taxon_list_entry_TYPE = "taxon_list_entry";

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	index treatments by taxa
		MutableAnnotation[] treatments = data.getMutableAnnotations(TREATMENT_ANNOTATION_TYPE);
		HashMap treatmentsByTaxonName = new HashMap();
		for (int t = 0; t < treatments.length; t++) {
			Annotation[] taxonomicNames = treatments[t].getAnnotations("taxonomicName");
			if (taxonomicNames.length == 0)
				continue;
			String taxonName = this.getTaxonomicNameKey(taxonomicNames[0]);
			if (taxonName != null)
				treatmentsByTaxonName.put(taxonName, treatments[t]);
		}
		
		//	get taxon lists
		MutableAnnotation[] subSections = data.getMutableAnnotations(MutableAnnotation.SUB_SECTION_TYPE);
		ArrayList taxonListList = new ArrayList();
		for (int s = 0; s < subSections.length; s++) {
			if (taxon_list_TYPE.equals(subSections[s].getAttribute(TYPE_ATTRIBUTE)))
				taxonListList.add(subSections[s]);
		}
		MutableAnnotation[] taxonLists = ((MutableAnnotation[]) taxonListList.toArray(new MutableAnnotation[taxonListList.size()]));
		
		//	mark list entries (paragraphs stating with a taxon name plus all subsequent paragraphs not doing so)
		for (int l = 0; l < taxonLists.length; l++) {
			boolean treatmentCreated = false;
			ArrayList taxonListEntries = new ArrayList();
			
			//	index caption and footnote paragraphs
			HashSet skipParagraphIDs = new HashSet();
			MutableAnnotation[] captions = taxonLists[l].getMutableAnnotations(CAPTION_TYPE);
			for (int c = 0; c < captions.length; c++) {
				MutableAnnotation[] paragraphs = captions[c].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < paragraphs.length; p++)
					skipParagraphIDs.add(paragraphs[p].getAnnotationID());
			}
			MutableAnnotation[] footnotes = taxonLists[l].getMutableAnnotations(FOOTNOTE_TYPE);
			for (int f = 0; f < footnotes.length; f++) {
				MutableAnnotation[] paragraphs = footnotes[f].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < paragraphs.length; p++)
					skipParagraphIDs.add(paragraphs[p].getAnnotationID());
			}
			
			//	store data for list entries
			String taxonName = null;
			int taxonListEntryStart = -1;
			
			//	bundle paragraphs into list entries
			MutableAnnotation[] paragraphs = taxonLists[l].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				
				//	skip over caption and footnote paragraphs
				if (skipParagraphIDs.contains(paragraphs[p].getAnnotationID()))
					continue;
				
				//	get taxon names
				Annotation[] taxonomicNames = paragraphs[p].getAnnotations("taxonomicName");
				
				//	no taxon names, this paragraph belongs to previous taxon
				if (taxonomicNames.length == 0)
					continue;
				
				//	many taxon names, unlikely to be start of new list entry
				if (taxonomicNames.length > 2)
					continue;
				
				//	taxon name to late, unlikely to be start of new list entry
				if (taxonomicNames[0].getStartIndex() >= 5)
					continue;
				
				//	mark and index previous list entry
				if (taxonListEntryStart != -1) {
					MutableAnnotation taxonListEntry = taxonLists[l].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, taxonListEntryStart, (paragraphs[p].getStartIndex() - taxonListEntryStart));
					taxonListEntry.setAttribute(TYPE_ATTRIBUTE, taxon_list_entry_TYPE);
					taxonListEntries.add(taxonListEntry);
					treatmentCreated = (treatmentCreated | this.handleTaxonListEntry(taxonListEntry, ((taxonName == null) ? null : ((MutableAnnotation) treatmentsByTaxonName.get(taxonName))), skipParagraphIDs));
				}
				
				//	mark top section, even if it does not have a taxon name
				else if (paragraphs[p].getStartIndex() > 0) {
					MutableAnnotation taxonListEntry = taxonLists[l].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, 0, paragraphs[p].getStartIndex());
					taxonListEntry.setAttribute(TYPE_ATTRIBUTE, taxon_list_entry_TYPE);
					taxonListEntries.add(taxonListEntry);
				}
				
				//	start new list entry
				taxonName = this.getTaxonomicNameKey(taxonomicNames[0]);
				taxonListEntryStart = paragraphs[p].getStartIndex();
			}
			
			//	mark and index last list entry
			if (taxonListEntryStart != -1) {
				MutableAnnotation taxonListEntry = taxonLists[l].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, taxonListEntryStart, (taxonLists[l].size() - taxonListEntryStart));
				taxonListEntry.setAttribute(TYPE_ATTRIBUTE, taxon_list_entry_TYPE);
				taxonListEntries.add(taxonListEntry);
				treatmentCreated = (treatmentCreated | this.handleTaxonListEntry(taxonListEntry, ((taxonName == null) ? null : ((MutableAnnotation) treatmentsByTaxonName.get(taxonName))), skipParagraphIDs));
			}
			
			//	treatment created, merge up non-treatment list entries
			if (treatmentCreated) {
				int entryBlockStart = 0;
				for (int e = 0; e < taxonListEntries.size(); e++) {
					Annotation entry = ((Annotation) taxonListEntries.get(e));
					
					//	this is a generic list entry, remove it
					if (MutableAnnotation.SUB_SUB_SECTION_TYPE.equals(entry.getType()))
						taxonLists[l].removeAnnotation(entry);
					
					//	this one is a treatment, keep it
					else {
						
						//	mark entry block
						if (entryBlockStart < entry.getStartIndex()) {
							Annotation entryBlock = taxonLists[l].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, entryBlockStart, (entry.getStartIndex() - entryBlockStart));
							entryBlock.setAttribute(TYPE_ATTRIBUTE, multiple_TYPE);
						}
						
						//	remember where the next block starts the earliest
						entryBlockStart = entry.getEndIndex();
					}
				}
				
				//	mark last entry block
				if (entryBlockStart < taxonLists[l].size()) {
					Annotation entryBlock = taxonLists[l].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, entryBlockStart, (taxonLists[l].size() - entryBlockStart));
					entryBlock.setAttribute(TYPE_ATTRIBUTE, multiple_TYPE);
				}
			}
			
			//	no treatment created, clean up
			else for (int e = 0; e < taxonListEntries.size(); e++)
				taxonLists[l].removeAnnotation((Annotation) taxonListEntries.get(e));
		}
	}
	
	private boolean handleTaxonListEntry(MutableAnnotation taxonListEntry, MutableAnnotation treatment, HashSet skipParagraphIDs) {
		boolean treatmentCreated = false;
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = taxonListEntry.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	this list entry is too small for any use, simply clean it up
		if (paragraphs.length < 2)
			return treatmentCreated;
		
		//	no regular treatment given for taxon, make it a treatment itself
		if (treatment == null) {
			
			//	make it a treatment in its own right
			taxonListEntry.changeTypeTo(TREATMENT_ANNOTATION_TYPE);
			taxonListEntry.setAttribute("_generate", "list-transformed");
			taxonListEntry.removeAttribute(TYPE_ATTRIBUTE);
			
			//	mark treatment data sections
			Annotation nomenclature = taxonListEntry.addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, paragraphs[0].getStartIndex(), paragraphs[0].size());
			nomenclature.setAttribute("_generate", "list-added");
			nomenclature.setAttribute(TYPE_ATTRIBUTE, nomenclature_TYPE);
			Annotation remainder = taxonListEntry.addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, paragraphs[1].getStartIndex(), (taxonListEntry.size() - paragraphs[1].getStartIndex()));
			remainder.setAttribute("_generate", "list-added");
			remainder.setAttribute(TYPE_ATTRIBUTE, multiple_TYPE);
			
			//	remember what was done
			treatmentCreated = true;
		}
		
		//	regular treatment given, add non-nomanclatural data to treatment
		else {
			
			//	remember original treatment size
			int treatmentExtensionStart = treatment.size();
			
			//	transfer paragraphs, omitting nomenclature
			for (int p = 1; p < paragraphs.length; p++) {
				
				//	skip over caption and footnote paragraphs
				if (skipParagraphIDs.contains(paragraphs[p].getAnnotationID()))
					continue;
				
				//	remember paragraph start
				int paragraphStart = treatment.size();
				
				//	append paeragraph
				treatment.addTokens(TokenSequenceUtils.concatTokens(paragraphs[p], false, true));
				MutableAnnotation paragraph = treatment.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (treatment.size() - paragraphStart));
				paragraph.copyAttributes(paragraphs[p]);
				
				//	transfer annotations
				Annotation[] paragraphAnnotations = paragraphs[p].getAnnotations();
				for (int a = 0; a < paragraphAnnotations.length; a++) {
					Annotation paragraphAnnotation = paragraph.addAnnotation(paragraphAnnotations[a].getType(), paragraphAnnotations[a].getStartIndex(), paragraphAnnotations[a].size());
					paragraphAnnotation.copyAttributes(paragraphAnnotations[a]);
				}
			}
			
			//	restore treatment structure
			if (treatment.size() > treatmentExtensionStart) {
				Annotation remainder = treatment.addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, treatmentExtensionStart, (treatment.size() - treatmentExtensionStart));
				remainder.setAttribute("_generate", "list-transferred");
				remainder.setAttribute(TYPE_ATTRIBUTE, multiple_TYPE);
			}
		}
		
		//	pass on result
		return treatmentCreated;
	}
	
	private String getTaxonomicNameKey(Annotation taxonName) {
		StringBuffer taxonNameKey = new StringBuffer();
		
		//	append main ranks only, others might be given or not, altering throughout the document
		String family = ((String) taxonName.getAttribute("family"));
		if (family != null) {
			if (taxonNameKey.length() != 0)
				taxonNameKey.append(" ");
			taxonNameKey.append(family);
		}
		String genus = ((String) taxonName.getAttribute("genus"));
		if (genus != null) {
			if (taxonNameKey.length() != 0)
				taxonNameKey.append(" ");
			taxonNameKey.append(genus);
		}
		String species = ((String) taxonName.getAttribute("species"));
		if (species != null) {
			if (taxonNameKey.length() != 0)
				taxonNameKey.append(" ");
			taxonNameKey.append(species);
		}
		String subSpecies = ((String) taxonName.getAttribute("subSpecies"));
		if (subSpecies != null) {
			if (taxonNameKey.length() != 0)
				taxonNameKey.append(" ");
			taxonNameKey.append(subSpecies);
		}
		String variety = ((String) taxonName.getAttribute("variety"));
		if (variety != null) {
			if (taxonNameKey.length() != 0)
				taxonNameKey.append(" ");
			taxonNameKey.append(variety);
		}
		
		//	return null if no data given at all
		return ((taxonNameKey.length() == 0) ? null : taxonNameKey.toString());
	}
}