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

package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider.LsidDataSet;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * 
 * 
 * @author sautter
 */
public class LsidReferencerOnline extends AbstractConfigurableAnalyzer implements LiteratureConstants, TaxonomicNameConstants {
	
	private static final String LSID_ATTRIBUTE = "LSID";
	private static final String LSID_NAME_ATTRIBUTE = "lsidName";
	
	private static final String LSID_REGEX = "[1-9][0-9]*+";
	private static final Pattern LSID_PATTERN = Pattern.compile(LSID_REGEX);
	
	private static final String IGNORE_TAXON_CHOICE = "Do not assign an LSID to this taxon name now";
	private static final String REMOVE_TAXON_CHOICE = "Remove taxon name Annotation";
	
	private LsidDataProvider[] dataProviders = new LsidDataProvider[0];
	private String defaultProviderCode = "";
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		this.defaultProviderCode = this.getParameter("DefaultLsidProvider", "");
		this.dataProviders = LsidDataProvider.getDataProviders(this.dataProvider);
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	check parameter
		if (data == null) {
			System.out.println("LsidReferencerOnline: got null document, returning");
			return;
		}
		
		//	find page numbers for taxonomic names
		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber != null) {
				Annotation[] taxonomicNames = paragraphs[p].getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
				for (int t = 0; t < taxonomicNames.length; t++)
					taxonomicNames[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			}
			Object pageId = paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE);
			if (pageId != null) {
				Annotation[] taxonomicNames = paragraphs[p].getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
				for (int t = 0; t < taxonomicNames.length; t++)
					taxonomicNames[t].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			}
		}
		
		//	get and bucketize taxonomic names
		StringVector bucketNames = new StringVector();
		HashMap bucketsByName = new HashMap();
		
		Annotation[] taxonomicNames = data.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		for (int t = 0; t < taxonomicNames.length; t++) {
			String taxonName = getNameString(taxonomicNames[t]);
			
			//	attributes missing, or above genus rank name
			if (taxonName.length() == 0) {
				if (taxonomicNames[t].size() == 1) {
					String name = Gamta.capitalize(taxonomicNames[t].firstValue());
					if (!name.endsWith("idae") && !name.endsWith("inae") && !name.endsWith("ini"))
						taxonName = name;
					else taxonName = taxonomicNames[t].firstValue();
				} else taxonName = taxonomicNames[t].getValue();
			}
			
			//	put into bucket
			TaxonNameBucket bucket = ((TaxonNameBucket) bucketsByName.get(taxonName));
			if (bucket == null) {
				bucket = new TaxonNameBucket(taxonomicNames[t]);
				bucketNames.addElementIgnoreDuplicates(taxonName);
				bucketsByName.put(taxonName, bucket);
			}
			bucket.taxonNames.add(taxonomicNames[t]);
		}
		
		//	line up buckets
		ArrayList buckets = new ArrayList();
		for (int b = 0; b < bucketNames.size(); b++)
			buckets.add(bucketsByName.get(bucketNames.get(b)));
		
		//	transfer LSID if one annotation in bucket already has one, and sort out these buckets
		for (int p = 0; p < this.dataProviders.length; p++) {
			String providerSuffix = ("-" + this.dataProviders[p].getProviderCode());
			
			int bucketIndex = 0;
			while (bucketIndex < buckets.size()) {
				TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(bucketIndex));
				String existingLsid = null;
				String existingLsidName = null;
				for (int t = 0; t < tnb.taxonNames.size(); t++) {
					Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
					String lsid = taxonName.getAttribute((LSID_ATTRIBUTE + providerSuffix), "").toString();
					if (LSID_PATTERN.matcher(lsid).matches()) {
						String lsidName = taxonName.getAttribute((LSID_NAME_ATTRIBUTE + providerSuffix), "").toString();
						if ((lsidName.length() != 0)) {
							existingLsid = lsid;
							existingLsidName = lsidName;
						}
					}
				}
				
				//	LSID not found, keep bucket
				if (existingLsid == null) bucketIndex++;
				
				//	found proper LSID in Annotations, bucket done
				else {
					for (int t = 0; t < tnb.taxonNames.size(); t++) {
						Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
						taxonName.setAttribute((LSID_ATTRIBUTE + providerSuffix), existingLsid);
						taxonName.setAttribute((LSID_NAME_ATTRIBUTE + providerSuffix), existingLsidName);
					}
					buckets.remove(bucketIndex);
				}
			}
		}
		
		//	get data provider to use
		LsidDataProvider dataProvider = null;
		for (int p = 0; p < this.dataProviders.length; p++) {
			if (this.defaultProviderCode.equals(this.dataProviders[p].getProviderCode()))
				dataProvider = this.dataProviders[p];
		}
		if ((dataProvider == null) && (this.dataProviders.length == 1)) {
			System.out.println("LsidReferencerOnline: default data provider not found using the only one given");
			dataProvider = this.dataProviders[0];
		}
		
		//	check if data provider given
		if (dataProvider == null) {
			System.out.println("LsidReferencerOnline: data provider not found, returning");
			return;
		}
		
		//	create status dialog
//		StatusDialog sd = null;
		
		//	process buckets
		StringVector downloadedThisDocument = new StringVector(false);
		
		//	try to load data for buckets
		for (int b = 0; b < buckets.size(); b++) {
			TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(b));
			String genus = tnb.taxonName.getAttribute(GENUS_ATTRIBUTE, "").toString();
			
			//	full genus given, load data
			if (genus.length() > 2) {
				LsidDataSet[] genusData = dataProvider.getLsidData(genus, true, downloadedThisDocument, null, parameters.containsKey(ONLINE_PARAMETER));
				if (genusData != null) {
					ArrayList dataSets = new ArrayList();
					boolean secureMatch = false;
					
					//	score data sets
					for (int d = 0; d < genusData.length; d++) {
						int score = genusData[d].getMatchScore(tnb.taxonName, downloadedThisDocument.contains(genus));
						secureMatch = ((score > 0) || secureMatch); // no fuzzy match for local data, unless data is fresh
						if (score != 0)
							dataSets.add(genusData[d].getScoredCopy(score));
					}
					
					//	do fuzzy match if no secure match
					if (!secureMatch) {
						dataSets.clear();
						for (int d = 0; d < genusData.length; d++) {
							int score = genusData[d].getMatchScore(tnb.taxonName, true);
							secureMatch = ((score > 0) || secureMatch); // no fuzzy match for local data, unless data is fresh
							if (score != 0)
								dataSets.add(genusData[d].getScoredCopy(score));
						}
					}
					
					//	if nothing found, refresh data set from HNS
					if (!secureMatch && !downloadedThisDocument.containsIgnoreCase(genus)) {
						if (parameters.containsKey(ONLINE_PARAMETER))
							genusData = dataProvider.getLsidData(genus, false, downloadedThisDocument, null, true);
						
						//	score data sets
						for (int d = 0; (genusData != null) && (d < genusData.length); d++) {
							int score = genusData[d].getMatchScore(tnb.taxonName, true);
							secureMatch = ((score > 0) || secureMatch); // use fuzzy match for fresh data
							if (score != 0)
								dataSets.add(genusData[d].getScoredCopy(score));
						}
					}
					
					//	use top data set
					Collections.sort(dataSets);
					if (!dataSets.isEmpty()) {
						LsidDataSet top = ((LsidDataSet) dataSets.get(0));
						int index = 1;
						while (index < dataSets.size()) {
							if (((LsidDataSet) dataSets.get(index)).score == top.score)
								index++;
							else if (!secureMatch && ((((LsidDataSet) dataSets.get(index)).score * 2) >= top.score))
								index++;
							else dataSets.remove(index);
						}
						
						String nameRank = tnb.taxonName.getAttribute(RANK_ATTRIBUTE, "").toString();
						
						//	ranks match, or "subSpecies" on provider side and "variety" in Annotation ("variety" is not a valid rank any more, so providers might not use "variety")
						if (top.rank.equalsIgnoreCase(nameRank) || (SUBSPECIES_ATTRIBUTE.equalsIgnoreCase(top.rank) && VARIETY_ATTRIBUTE.equalsIgnoreCase(nameRank))) {
							
							//	clear choice
							if (dataSets.size() == 1) tnb.dataSet = top;
							
							//	ambiobuous choice, ask feedback
							else if (parameters.containsKey(INTERACTIVE_PARAMETER))
								tnb.dataSets.addAll(dataSets);
							
							//	feedback not allowed
							else tnb.dataSet = IGNORE;
						}
						
						//	rank not matching, ask for feedback
						else if (parameters.containsKey(INTERACTIVE_PARAMETER))
							tnb.dataSets.addAll(dataSets);
						
						//	feedback not allowed
						else tnb.dataSet = IGNORE;
					}
				}
			} 
			
			//	genus attribute missing, maybe above genus rank name 
			else if ((tnb.taxonName.size() == 1) && Gamta.isFirstLetterUpWord(tnb.taxonName.firstValue())) {
				
				//	get and classify name
				String name = Gamta.capitalize(tnb.taxonName.firstValue());
				
				//	above genus taxon name
				if (name.endsWith("idae") || name.endsWith("inae") || name.endsWith("ini")) {
					LsidDataSet[] lsidData = dataProvider.getLsidData(name, true, downloadedThisDocument, null, parameters.containsKey(ONLINE_PARAMETER));
					
					//	score data sets
					ArrayList dataSets = new ArrayList();
					if (lsidData != null)
						for (int d = 0; d < lsidData.length; d++) {
							String nameString = lsidData[d].taxonName;
							int score = 0;
							
							//	cut author name if given
							if (nameString.indexOf(' ') != -1)
								nameString = nameString.substring(0, nameString.indexOf(' '));
							
							//	match them
							if (name.equals(nameString))
								score = 2;
							else if (name.equalsIgnoreCase(nameString))
								score = 1;
							
							//	store data on match
							if (score != 0)
								dataSets.add(lsidData[d].getScoredCopy(score));
						}
					
					//	if nothing found, refresh data set from provider
					if (dataSets.isEmpty() && !downloadedThisDocument.containsIgnoreCase(name)) {
						if (parameters.containsKey(ONLINE_PARAMETER))
							lsidData = dataProvider.getLsidData(name, false, downloadedThisDocument, null, true);
						
						//	score data sets
						for (int d = 0; (lsidData != null) && (d < lsidData.length); d++) {
							String nameString = lsidData[d].taxonName;
							int score = 0;
							
							//	cut author name if given
							if (nameString.indexOf(' ') != -1)
								nameString = nameString.substring(0, nameString.indexOf(' '));
							
							//	match them
							if (name.equals(nameString))
								score = 2;
							else if (name.equalsIgnoreCase(nameString))
								score = 1;
							
							//	store data on match
							if (score != 0)
								dataSets.add(lsidData[d].getScoredCopy(score));
						}
					}
					
					//	use top data set
					Collections.sort(dataSets);
					if (!dataSets.isEmpty()) {
						LsidDataSet top = ((LsidDataSet) dataSets.get(0));
						int index = 1;
						while (index < dataSets.size()) {
							if (((LsidDataSet) dataSets.get(index)).score < top.score)
								dataSets.remove(index);
							else index++;
						}
						
						//	clear choice
						if (dataSets.size() == 1) tnb.dataSet = top;
						
						//	ambiguous choice, ask for feedback
						else if (parameters.containsKey(INTERACTIVE_PARAMETER)) tnb.dataSets.addAll(dataSets);
						
						//	feedback not allowed
						else tnb.dataSet = IGNORE;
					}
				}
			}
		}
		
		//	process data
		int bucketIndex = 0;
		while (bucketIndex < buckets.size()) {
			TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(bucketIndex));
			
			//	not allowed to bet feedback, ignore name
			if (tnb.dataSet == IGNORE)
				buckets.remove(bucketIndex);
			
			//	found unambiguous LSID
			else if (tnb.dataSet != null) {
				for (int t = 0; t < tnb.taxonNames.size(); t++) {
					Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
					taxonName.setAttribute((LSID_ATTRIBUTE + "-" + dataProvider.getProviderCode()), (dataProvider.getLsidUrnPrefix() + tnb.dataSet.lsidNumber));
					taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + dataProvider.getProviderCode()), tnb.dataSet.taxonName);
				}
				
				//	bucket done
				buckets.remove(bucketIndex);
			}
			
			//	keep bucket for feedback
			else bucketIndex++;
		}
		
		//	get feedback for ambiguous or unknown names
		this.getFeedback(((TaxonNameBucket[]) buckets.toArray(new TaxonNameBucket[buckets.size()])), data);
		
		//	process feedback
		for (int b = 0; b < buckets.size(); b++) {
			TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(b));
			
			//	remove
			if (tnb.dataSet == REMOVE) {
				for (int t = 0; t < tnb.taxonNames.size(); t++)
					data.removeAnnotation((Annotation) tnb.taxonNames.get(t));
			}
			
			//	ignore taxon name for now
			else if (tnb.dataSet == IGNORE) {}
			
			//	LSID selected
			else if (tnb.dataSet != null) {
				for (int t = 0; t < tnb.taxonNames.size(); t++) {
					Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
					taxonName.setAttribute((LSID_ATTRIBUTE + "-" + dataProvider.getProviderCode()), (dataProvider.getLsidUrnPrefix() + tnb.dataSet.lsidNumber));
					taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + dataProvider.getProviderCode()), tnb.dataSet.taxonName);
				}
			}
		}
	}
	
	//	return false only if interrupted
	private void getFeedback(TaxonNameBucket[] buckets, MutableAnnotation data) {
		
		//	don't show empty dialog
		if (buckets.length == 0) return;
		
		//	create store for feedback results
		String[] selectedOptions = new String[buckets.length];
		
		//	compute number of buckets per dialog
		int dialogCount = ((buckets.length + 9) / 10);
		int dialogSize = ((buckets.length + (dialogCount / 2)) / dialogCount);
		dialogCount = ((buckets.length + dialogSize - 1) / dialogSize);
		
		//	build dialogs
		AssignmentDisambiguationFeedbackPanel[] adfps = new AssignmentDisambiguationFeedbackPanel[dialogCount];
		for (int d = 0; d < adfps.length; d++) {
			adfps[d] = new AssignmentDisambiguationFeedbackPanel("Check Unclear LSIDs");
			adfps[d].setLabel("<HTML>Please check which of the available <B>LSIDs</B> (in the drop-downs) are the appropriate ones for the <B>taxon names</B>." +
					"<BR>Select <I>&lt;Do not assign an LSID to this taxon name now&gt;</I> to postpone the assignment." +
					"<BR>Select <I>&lt;Remove taxon name Annotation&gt;</I> to indicate that the taxon name actually is none." +
					"<BR>The LSIDs selectable in the drop-downs bear the following extra information:" +
					"<BR>- the name string as provided by the LSID database" +
					"<BR>- the three-letter code indicating the provider the LSID comes from (in the brackets)" +
					"<BR>- a matching score indicating how well the name string matches the name string in the document (in the brackets)" +
					"<BR>The matching score is relative, comparable only within one drop-down. Its order of magnitude largely depends on the length of the name string.</HTML>");
			int dialogOffset = (d * dialogSize);
			for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < buckets.length); b++) {
				
				//	put LSIDs in array
				String[] lsidOptions = new String[buckets[b + dialogOffset].dataSets.size() + 2];
				for (int l = 0; l < buckets[b + dialogOffset].dataSets.size(); l++)
					lsidOptions[l] = buckets[b + dialogOffset].dataSets.get(l).toString();
				lsidOptions[buckets[b + dialogOffset].dataSets.size()] = IGNORE_TAXON_CHOICE;
				lsidOptions[buckets[b + dialogOffset].dataSets.size() + 1] = REMOVE_TAXON_CHOICE;
				
				//	build name & context display
				StringBuffer nameContext = new StringBuffer("<HTML>");
				for (int o = 0; o < buckets[b + dialogOffset].taxonNames.size(); o++) {
					Annotation taxonName = ((Annotation) buckets[b + dialogOffset].taxonNames.get(o));
					nameContext.append(((o == 0) ? "" : "<BR>") + buildLabel(data, taxonName, 10));
					if (taxonName.hasAttribute(PAGE_NUMBER_ATTRIBUTE))
						nameContext.append(" (page " + taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")");
				}
				nameContext.append("</HTML>");
				
				//	add to feedback panel
				adfps[d].addLine(nameContext.toString(), lsidOptions);
			}
			
			//	add backgroung information
			adfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, buckets[dialogOffset].taxonName.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, buckets[dialogOffset].taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, buckets[dialogOffset].taxonName.getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, buckets[dialogOffset].taxonName.getAnnotationID());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, TAXONOMIC_NAME_ANNOTATION_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString((Annotation[]) buckets[dialogOffset].taxonNames.toArray(new Annotation[buckets[dialogOffset].taxonNames.size()]));
			if (targetPages != null)
				adfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString((Annotation[]) buckets[dialogOffset].taxonNames.toArray(new Annotation[buckets[dialogOffset].taxonNames.size()]));
			if (targetPageIDs != null)
				adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		int cutoffBucket = buckets.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(adfps);
			
			//	process all feedback data together
			for (int d = 0; d < adfps.length; d++) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < adfps[d].lineCount(); b++)
//					selectedOptions[b + dialogOffset] = adfps[d].getOptionAt(b);
					selectedOptions[b + dialogOffset] = adfps[d].getSelectedOptionAt(b);
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < adfps.length; d++) {
			if (d != 0)
				adfps[d].addButton("Previous");
			adfps[d].addButton("Cancel");
			adfps[d].addButton("OK" + (((d+1) == adfps.length) ? "" : " & Next"));
			
			String title = adfps[d].getTitle();
			adfps[d].setTitle(title + " - (" + (d+1) + " of " + adfps.length + ")");
			
			String f = adfps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			adfps[d].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < adfps[d].lineCount(); b++)
//					selectedOptions[b + dialogOffset] = adfps[d].getOptionAt(b);
					selectedOptions[b + dialogOffset] = adfps[d].getSelectedOptionAt(b);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog on
			else {
				cutoffBucket = (d * dialogSize);
				d = adfps.length;
			}
//			adfps[d].addButton("OK");
//			adfps[d].addButton("Cancel");
//			String f = null;
////			try {
////				f = FeedbackPanelHtmlTester.testFeedbackPanel(adfps[d], 0);
////			}
////			catch (IOException ioe) {
////				ioe.printStackTrace();
////			}
//			if (f == null)
//				f = adfps[d].getFeedback();
//			
//			//	cancel from current dialog on
//			if ("Cancel".equals(f)) {
//				cutoffBucket = (d * dialogSize);
//				d = adfps.length;
//			}
//			
//			//	current dialog submitted, process data
//			else {
//				int dialogOffset = (d * dialogSize);
//				for (int b = 0; b < adfps[d].lineCount(); b++)
//					selectedOptions[b + dialogOffset] = adfps[d].getOptionAt(b);
//			}
		}
		
		
		//	process feedback
		for (int b = 0; b < cutoffBucket; b++) {
			
			//	name removed
			if (REMOVE_TAXON_CHOICE.equals(selectedOptions[b]))
				buckets[b].dataSet = REMOVE;
			
			//	name cannot be handled right now
			else if (IGNORE_TAXON_CHOICE.equals(selectedOptions[b]))
				buckets[b].dataSet = IGNORE;
			
			//	find LSID data set for selected option
			else for (int d = 0; d < buckets[b].dataSets.size(); d++) {
				LsidDataSet dataSet = ((LsidDataSet) buckets[b].dataSets.get(d));
				if (dataSet.toString().equals(selectedOptions[b])) {
					buckets[b].dataSet = dataSet;
					d = buckets[b].dataSets.size();
				}
			}
		}
	}
	
	private static final String MISSING_PART = "####";
	
	private static String getNameString(Annotation taxonomicName) {
		String nameString = "";
		String attribute;
		
		attribute = taxonomicName.getAttribute(VARIETY_ATTRIBUTE, MISSING_PART).toString();
		if (!MISSING_PART.equals(attribute)) nameString = ("var. " + attribute + " " + nameString);
		
		attribute = taxonomicName.getAttribute(SUBSPECIES_ATTRIBUTE, MISSING_PART).toString();
		if (!MISSING_PART.equals(attribute)) nameString = ("subsp. " + attribute + " " + nameString);
		
		attribute = taxonomicName.getAttribute(SPECIES_ATTRIBUTE, MISSING_PART).toString();
		if (!MISSING_PART.equals(attribute)) nameString = (attribute + " " + nameString);
		
		attribute = taxonomicName.getAttribute(SUBGENUS_ATTRIBUTE, MISSING_PART).toString();
		if (!MISSING_PART.equals(attribute)) nameString = ("(" + attribute + ") " + nameString);
		
		attribute = taxonomicName.getAttribute(GENUS_ATTRIBUTE, MISSING_PART).toString();
		if (!MISSING_PART.equals(attribute)) nameString = (attribute + " " + nameString);
		
		return nameString.trim();
	}
	
	/**
	 * Data container for LSID referencing taxonomic names
	 * 
	 * @author sautter
	 */
	private static class TaxonNameBucket implements Comparable {
		String nameString;
		
		final Annotation taxonName;
		ArrayList taxonNames = new ArrayList();
		
		LsidDataSet dataSet = null;
		ArrayList dataSets = new ArrayList();
		
		TaxonNameBucket(Annotation taxonName) {
			this.nameString = getNameString(taxonName);
			if (this.nameString.length() == 0)
				this.nameString = taxonName.getValue();
			this.taxonName = taxonName;
			this.taxonNames.add(taxonName);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o instanceof TaxonNameBucket) return this.nameString.compareTo(((TaxonNameBucket) o).nameString);
			else return -1;
		}
	}
	
	//	special dummy LSID data sets to indicate special actions
	private static final LsidDataSet REMOVE = new LsidDataSet(null, "", "", ""); 
	private static final LsidDataSet IGNORE = new LsidDataSet(null, "", "", ""); 
	
	private static String buildLabel(TokenSequence text, Annotation annot, int envSize) {
		int aStart = annot.getStartIndex();
		int aEnd = annot.getEndIndex();
		int start = Math.max(0, (aStart - envSize));
		int end = Math.min(text.size(), (aEnd + envSize));
		StringBuffer sb = new StringBuffer("... ");
		Token lastToken = null;
		Token token = null;
		for (int t = start; t < end; t++) {
			lastToken = token;
			token = text.tokenAt(t);
			
			//	end highlighting value
			if (t == aEnd) sb.append("</B>");
			
			//	add spacer
			if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
			
			//	start highlighting value
			if (t == aStart) sb.append("<B>");
			
			//	append token
			sb.append(token);
		}
		
		return sb.append(" ...").toString();
	}
}