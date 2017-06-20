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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider.LsidDataSet;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider.StatusDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class LsidReferencer extends AbstractConfigurableAnalyzer implements MonitorableAnalyzer, TaxonomicNameConstants {
	
	private static final String LSID_ATTRIBUTE = "LSID";
	private static final String LSID_NAME_ATTRIBUTE = "lsidName";
	
	private static final String LSID_REGEX = "[1-9][0-9]*+";
	private static final Pattern LSID_PATTERN = Pattern.compile(LSID_REGEX);
	
	private static final String ENTER_LSID_CHOICE = "<Double click to enter LSID manually>";
	private static final String CHANGE_LSID_CHOICE = " <Double click to change LSID>";
	private static final String CHANGE_GENUS_CHOICE = "<Double click to  change genus> ";
	private static final String CHANGE_NAME_CHOICE = "<Double click to change name> ";
	private static final String IGNORE_TAXON_CHOICE = "Ignore taxonomic name on LSID referencing, or get LSID later";
	private static final String REMOVE_TAXON_CHOICE = "Remove taxonomic name Annotation";
	private static final String UPLOAD_NEW_TAXON_CHOICE = "Upload new taxonomic name to provider and obtain LSID";
	
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
		this.process(data, parameters, ProgressMonitor.dummy);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
		
		//	check parameter
		if (data == null) return;
		
		//	find page numbers for taxonomic names
		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		Properties pageNumberByTaxonName = new Properties();
		Properties nomenclaturePageNumberByTaxonName = new Properties();
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber != null) {
				Annotation[] taxonomicNames = paragraphs[p].getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
				Annotation[] taxonomicNameLabels = paragraphs[p].getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
				
				for (int t = 0; t < taxonomicNames.length; t++) {
					taxonomicNames[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
					pageNumberByTaxonName.setProperty(taxonomicNames[t].getAnnotationID(), pageNumber.toString());
					
					//	remember page of taxon name directly preceeding the only one label in the paragraph, might be nomenclature paragraph for that name
					if ((taxonomicNameLabels.length == 1) && (Math.abs(taxonomicNameLabels[0].getStartIndex() - taxonomicNames[t].getEndIndex()) < 2))
						nomenclaturePageNumberByTaxonName.setProperty(taxonomicNames[t].getAnnotationID(), pageNumber.toString());
				}
			}
		}
		
		//	get and bucketize taxonomic names
		StringVector bucketNames = new StringVector();
		HashMap bucketsByName = new HashMap();
		int li = 0;
		
		Annotation[] taxonomicNames = data.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		Annotation[] labels = data.getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		
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
			
			//	mark taxonomic names that are labeled as new
			while ((li < labels.length) && (taxonomicNames[t].getStartIndex() > labels[li].getStartIndex())) li++;
			if (li < labels.length) {
				if (taxonomicNames[t].getEndIndex() == labels[li].getStartIndex()) bucket.isNewTaxon = true;
				else if (((taxonomicNames[t].getEndIndex() + 1) == labels[li].getStartIndex())
						&& ",".equals(data.valueAt(taxonomicNames[t].getEndIndex()))
					) bucket.isNewTaxon = true;
			}
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
		if ((dataProvider == null) && (this.dataProviders.length == 1))
			dataProvider = this.dataProviders[0];
		
		//	select data provider if not clear so far
		if ((dataProvider == null) && parameters.containsKey(INTERACTIVE_PARAMETER)) {
			Object dpo = JOptionPane.showInputDialog(null, "Please select the LSID provider to use", "Select LSID Provider", JOptionPane.PLAIN_MESSAGE, null, this.dataProviders, dataProvider);
			if (dpo != null)
				dataProvider = ((LsidDataProvider) dpo);
		}
		
		//	check if data provider given
		if (dataProvider == null) return;
		
		//	create status dialog
		StatusDialog sd = null;
		if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
			sd = new StatusDialog();
			sd.online.setSelected(parameters.containsKey(ONLINE_PARAMETER));
			sd.popup();
		}
		
		//	TODO set progress
		
		//	process buckets
		StringVector downloadedThisDocument = new StringVector(false);
		while (!buckets.isEmpty()) {
			StringVector genera = new StringVector();
			
			//	try to load data for buckets
			for (int b = 0; b < buckets.size(); b++) {
				TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(b));
				String genus = tnb.taxonName.getAttribute(GENUS_ATTRIBUTE, "").toString();
				
				//	check for interrupt
				if ((sd != null) && sd.interrupted) {
					sd.dispose();
					return;
				}
				
				//	full genus given, load data
				if (genus.length() > 2) {
					tnb.downloadEpithet = genus;
					genera.addElementIgnoreDuplicates(genus);
					
					if (sd != null)
						sd.setStep("Getting LSID data for '" + genus + "' ...");
					
//					LsidDataSet[] genusData = this.getLsidData(genus, true, true, downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER), dataProvider);
					LsidDataSet[] genusData = dataProvider.getLsidData(genus, true, downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER));
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
						if (sd != null)
							sd.setInfo("  - Found " + dataSets.size() + " matches");
						
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
						if (sd != null)
							sd.setInfo("  - Found " + dataSets.size() + " total matches");
						
						//	if nothing found, refresh data set from server
						if (!secureMatch && !downloadedThisDocument.containsIgnoreCase(genus)) {
							if (sd != null)
								sd.setInfo("  ==> Taxon '" + tnb.taxonName.getValue() + "' not found in local genus data.");
							
							//	ask for web access if in offline mode
							if ((sd != null) && !sd.online.isSelected()) {
								if (JOptionPane.showConfirmDialog(sd.getDialog(), ("LSID Referencer is in offline mode, allow fetching LSID data for '" + genus + "' anyway?"), "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
									sd.online.setSelected(true);
							}
							
							//	if web access allowed, get data from LSID server
							if (((sd != null) && sd.online.isSelected()) || ((sd == null) && parameters.containsKey(ONLINE_PARAMETER)))
								genusData = dataProvider.getLsidData(genus, false, downloadedThisDocument, sd, true);
							
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
							if (sd != null)
								sd.setInfo("  - Got " + dataSets.size() + " top matches");
							
							String nameRank = tnb.taxonName.getAttribute(RANK_ATTRIBUTE, "").toString();
							
							//	ranks match, or "subSpecies" on provider side and "variety" in Annotation ("variety" is not a valid rank any more, so providers might not use "variety")
							if (top.rank.equalsIgnoreCase(nameRank) || (SUBSPECIES_ATTRIBUTE.equalsIgnoreCase(top.rank) && VARIETY_ATTRIBUTE.equalsIgnoreCase(nameRank))) {
								
								//	clear choice
								if (dataSets.size() == 1) tnb.dataSet = top;
								
								//	ambiguous choice, ask feedback
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
					
					//	update status display
					else if (sd != null)
						sd.setInfo("  ==> Data not available.");
				} 
				
				//	genus attribute missing, maybe above genus rank name 
				else if ((tnb.taxonName.size() == 1) && Gamta.isFirstLetterUpWord(tnb.taxonName.firstValue())) {
					
					//	get and classify name
					String name = Gamta.capitalize(tnb.taxonName.firstValue());
					if (name.endsWith("idae")) {
						tnb.downloadEpithet = name;
						tnb.downloadEpithetRank = FAMILY_ATTRIBUTE;
					}
					else if (name.endsWith("inae")) {
						tnb.downloadEpithet = name;
						tnb.downloadEpithetRank = SUBFAMILY_ATTRIBUTE;
					}
					else if (name.endsWith("ini")) {
						tnb.downloadEpithet = name;
						tnb.downloadEpithetRank = TRIBE_ATTRIBUTE;
					}
					
					//	above genus taxon name
					if (!GENUS_ATTRIBUTE.equals(tnb.downloadEpithetRank)) {
						if (sd != null)
							sd.setStep("Getting LSID data for '" + name + "' ...");
						
//						LsidDataSet[] lsidData = this.getLsidData(name, true, true, downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER), dataProvider);
						LsidDataSet[] lsidData = dataProvider.getLsidData(name, true, downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER));
						
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
							if (sd != null)
								sd.setInfo("  ==> Taxon '" + tnb.taxonName.getValue() + "' not found in local data.");
							
							//	ask for web access if in offline mode
							if ((sd != null) && !sd.online.isSelected()) {
								if (JOptionPane.showConfirmDialog(sd.getDialog(), ("LSID Referencer is in offline mode, allow fetching LSID data for '" + name + "' anyway?"), "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
									sd.online.setSelected(true);
							}
							
							//	if web access allowed, get data from LSID server
							if (((sd != null) && sd.online.isSelected()) || ((sd == null) && parameters.containsKey(ONLINE_PARAMETER)))
//								lsidData = this.getLsidData(name, false, false, downloadedThisDocument, sd, true, dataProvider);
								lsidData = dataProvider.getLsidData(name, false, downloadedThisDocument, sd, true);
							
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
							if (dataSets.size() == 1)
								tnb.dataSet = top;
							
							//	ambiguous choice, ask for feedback
							else if (parameters.containsKey(INTERACTIVE_PARAMETER))
								tnb.dataSets.addAll(dataSets);
							
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
			this.getFeedback(((TaxonNameBucket[]) buckets.toArray(new TaxonNameBucket[buckets.size()])), genera.toStringArray(), downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER), data, dataProvider);
			
			//	process feedback
			bucketIndex = 0;
			while (bucketIndex < buckets.size()) {
				TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(bucketIndex));
				
				//	remove
				if (tnb.dataSet == REMOVE) {
					for (int t = 0; t < tnb.taxonNames.size(); t++)
						data.removeAnnotation((Annotation) tnb.taxonNames.get(t));
					
					//	bucket done
					buckets.remove(bucketIndex);
				}
				
				//	ignore taxon name for now
				else if (tnb.dataSet == IGNORE)
					buckets.remove(bucketIndex);
				
				//	LSID selected, or upload
				else if (tnb.dataSet != null) {
					for (int t = 0; t < tnb.taxonNames.size(); t++) {
						Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
						taxonName.setAttribute((LSID_ATTRIBUTE + "-" + dataProvider.getProviderCode()), (dataProvider.getLsidUrnPrefix() + tnb.dataSet.lsidNumber));
						taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + dataProvider.getProviderCode()), tnb.dataSet.taxonName);
					}
					
					//	bucket done
					buckets.remove(bucketIndex);
				}
				
				//	LSID entered manually, or upload failed ==> bucket done
				else if (tnb.taxonName.hasAttribute((LSID_NAME_ATTRIBUTE + "-" + dataProvider.getProviderCode())))
					buckets.remove(bucketIndex);
				
				//	genus or entire name changed, keep bucket for next round
				else bucketIndex++;
			}
		}
		
		//	close status dialog
		if (sd != null) sd.dispose();
	}
	
	//	return false only if interrupted
	private void getFeedback(TaxonNameBucket[] buckets, String[] genera, StringVector downloadedThisDocument, StatusDialog sd, boolean online, TokenSequence text, LsidDataProvider dataProvider) {
		
		//	don't show empty dialog
		if (buckets.length == 0) return;
		
		//	assemble dialog
//		final JDialog td = new JDialog(sd, "Please Check Unclear LSIDs", true);
		final JDialog td = new JDialog(sd.getDialog(), "Please Check Unclear LSIDs", true);
		JPanel panel = new JPanel(new GridBagLayout());
		JScrollPane panelBox = new JScrollPane(panel);
		panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panelBox.getVerticalScrollBar().setUnitIncrement(50);
		td.getContentPane().setLayout(new BorderLayout());
		td.getContentPane().add(panelBox, BorderLayout.CENTER);
		
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.top = 2;
		gbc.insets.bottom = 2;
		gbc.insets.left = 5;
		gbc.insets.right = 5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		//	add classification boxes
		Arrays.sort(genera);
		TaxonNameBucketTray[] trays = new TaxonNameBucketTray[buckets.length];
		for (int b = 0; b < buckets.length; b++) {
			trays[b] = new TaxonNameBucketTray(dataProvider, buckets[b], genera, text, sd.online.isSelected());
			panel.add(trays[b], gbc.clone());
			gbc.gridy++;
		}
		
		//	add OK button
		JButton okButton = new JButton("OK");
		okButton.setBorder(BorderFactory.createRaisedBevelBorder());
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				td.dispose();
			}
		});
		
		//	add OK button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				gbc.fill = GridBagConstraints.NONE;
				td.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		buttonPanel.add(okButton, gbc.clone());
		gbc.gridx++;
		buttonPanel.add(cancelButton, gbc.clone());
		td.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		//	ensure dialog is closed with button
		td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		//	get feedback
		td.setSize(500, (Math.min(650, (78 * buckets.length) + 50)));
		td.setLocationRelativeTo(null);
		td.setVisible(true);
		
		//	process feedback
		boolean doUpload = true;
		for (int t = 0; t < trays.length; t++) {
			
			//	upload, check if cancelled
			if (UPLOAD_NEW_TAXON_CHOICE.equals(trays[t].selector.getSelectedItem())) {
				if (doUpload) {
					if (online || (JOptionPane.showConfirmDialog(sd.getDialog(), "LSID Referencer has been invoked in offline mode, allow uploading new taxonomic names anyway?", "Allow Web Access?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
						online = true; // ask only once
						doUpload = uploadName(trays[t].bucket, downloadedThisDocument, sd, dataProvider);
					}
					
					//	stop uploading immediately if web access denied
					else doUpload = false;
				}
			}
			
			//	other option
			else trays[t].commitChange();
			
			//	main dialog cancelled, ignore all buckets not having an LSID (or dummy for other option) selected
			if ((gbc.fill == GridBagConstraints.NONE) && trays[t].bucket.dataSet == null)
				trays[t].bucket.dataSet = IGNORE;
		}
	}
	
//	private static final String MISSING_PART = "####";
//	
	private static String getNameString(Annotation taxonomicName) {
//		String nameString = "";
//		String epithet;
//		
//		epithet = taxonomicName.getAttribute(VARIETY_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(epithet)) nameString = ("var. " + epithet + " " + nameString);
//		
//		epithet = taxonomicName.getAttribute(SUBSPECIES_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(epithet)) nameString = ("subsp. " + epithet + " " + nameString);
//		
//		epithet = taxonomicName.getAttribute(SPECIES_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(epithet)) nameString = (epithet + " " + nameString);
//		
//		epithet = taxonomicName.getAttribute(SUBGENUS_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(epithet)) nameString = ("(" + epithet + ") " + nameString);
//		
//		epithet = taxonomicName.getAttribute(GENUS_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(epithet)) nameString = (epithet + " " + nameString);
//		
//		return nameString.trim();
		StringBuffer nameString = new StringBuffer();
		String epithet;
		
		for (int r = LsidDataProvider.genusIndex; r < LsidDataProvider.ranks.length; r++) {
			epithet = ((String) taxonomicName.getAttribute(LsidDataProvider.ranks[r].name));
			if (epithet != null) {
				if (nameString.length() != 0)
					nameString.append(" ");
				nameString.append(LsidDataProvider.ranks[r].formatEpithet(epithet));
			}
		}
		
		return nameString.toString();
	}
	
//	private LsidDataSet[] findLsid(LsidDataSet[] data, Annotation taxonomicName, boolean allowFuzzyMatch) {
//		//	TODO: use this for getting data, and provide more than top element in interactive mode
//		ArrayList dataSets = new ArrayList();
//		for (int d = 0; d < data.length; d++) {
//			StringTupel dataSet = data.get(d);
//			int score = this.score(dataSet, taxonomicName, allowFuzzyMatch);
//			if (score != 0) dataSets.add(new LsidDataSet(dataSet, score));
//		}
//		return ((LsidDataSet[]) dataSets.toArray(new LsidDataSet[dataSets.size()]));
//	}
//	
	/**
	 * Data container for LSID referencing taxonomic names
	 * 
	 * @author sautter
	 */
	private static class TaxonNameBucket implements Comparable {
		String downloadEpithet = null;
		String downloadEpithetRank = GENUS_ATTRIBUTE;
		
		String nameString;
		
		/** the annotation wrapped in the bucket */
		final Annotation taxonName;
		ArrayList taxonNames = new ArrayList();
		
		LsidDataSet dataSet = null;
		ArrayList dataSets = new ArrayList();
		
		boolean isNewTaxon = false;
		
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
	
	private static class TaxonNameBucketTray extends JPanel {
		private static final int width = 465;
		private static final Dimension noChangeSize = new Dimension(width, 55);
		private static final Dimension changeSize = new Dimension(width, 70);
		
		private LsidDataProvider dataProvider;
		
		private TaxonNameBucket bucket;
		private JLabel label = new JLabel("", JLabel.LEFT);
		
		private LsidDataSet[] choices;
		private JComboBox selector;
		
		private String lsid = null;
		
		private String downloadEpithet = null;
		
		private String name = null;
		private Properties nameParts = new Properties();
		
		TaxonNameBucketTray(LsidDataProvider dataProvider, final TaxonNameBucket bucket, final String[] genera, final TokenSequence text, boolean online) {
			super(new BorderLayout(), true);
			this.dataProvider = dataProvider;
			boolean canUpload = this.dataProvider.isUploadSupported();
			
			this.bucket = bucket;
			this.choices = (bucket.dataSets.isEmpty() ? null : ((LsidDataSet[]) bucket.dataSets.toArray(new LsidDataSet[bucket.dataSets.size()])));
			
//			for (int p = 0; p < PART_NAMES.length; p++) {
//				String attribute = PART_NAMES[p];
//				String value = this.bucket.taxonName.getAttribute(attribute, "").toString();
//				if (value.length() != 0)
//					this.nameParts.setProperty(attribute, value);
//			}
			for (int r = LsidDataProvider.genusIndex; r < LsidDataProvider.ranks.length; r++) {
				String value = this.bucket.taxonName.getAttribute(LsidDataProvider.ranks[r].name, "").toString();
				if (value.length() != 0)
					this.nameParts.setProperty(LsidDataProvider.ranks[r].name, value);
			}
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			String nameString = getNameString(bucket.taxonName);
			if (bucket.nameString.equals(nameString)) {
				label.setText(
					"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
					"<B>" + bucket.nameString + "</B></HTML>"
				);
				this.setPreferredSize(noChangeSize);
			}
			else { 
				label.setText(
					"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
					"<B>" + bucket.nameString + "</B><BR>" +
					"(change to <B>" + nameString + "</B>)</HTML>"
				);
				this.setPreferredSize(changeSize);
			}
			this.label.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					StringVector docNames = new StringVector();
					for (int a = 0; a < bucket.taxonNames.size(); a++) {
						Annotation taxonName = ((Annotation) bucket.taxonNames.get(a));
						Object pageNumber = taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE);
						docNames.addElementIgnoreDuplicates(buildLabel(text, taxonName, 10) + " (at " + taxonName.getStartIndex() + ((pageNumber == null) ? ", unknown page" : (" on page " + pageNumber)) + ")");
					}
					String message = ("<HTML>This taxonomic name appears in the document in the following positions:<BR>&nbsp;&nbsp;&nbsp;");
					message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
					message += "</HTML>";
					JOptionPane.showMessageDialog(TaxonNameBucketTray.this, message, ("Forms Of \"" + bucket.nameString + "\""), JOptionPane.INFORMATION_MESSAGE);
				}
			});
			this.add(this.label, BorderLayout.CENTER);
			
			//	unknown above genus rank name
			if (!GENUS_ATTRIBUTE.equals(this.bucket.downloadEpithetRank)) {
				
				final String[] options = new String[canUpload ? 5 : 4];
				
				options[0] = ENTER_LSID_CHOICE;
				options[1] = (CHANGE_NAME_CHOICE + this.bucket.downloadEpithet);
				options[2] = IGNORE_TAXON_CHOICE;
				options[3] = REMOVE_TAXON_CHOICE;
				if (canUpload) options[4] = UPLOAD_NEW_TAXON_CHOICE;
				
				this.selector = new JComboBox(options);
				this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.selector.setEditable(false);
				this.selector.setSelectedIndex(1);
				
				this.selector.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getClickCount() > 1) {
							
							//	manual LSID input
							if (selector.getSelectedIndex() == 0) {
								Object newLsid = JOptionPane.showInputDialog(null, ("Please enter the LSID for " + bucket.downloadEpithet), "Enter LSID", JOptionPane.QUESTION_MESSAGE, null, null, ((lsid == null) ? "" : lsid));
								if (newLsid != null) {
									lsid = newLsid.toString().trim();
									if (lsid.length() == 0) {
										lsid = null;
										options[0] = ENTER_LSID_CHOICE;
									} else {
										options[0] = (lsid + CHANGE_LSID_CHOICE);
									}
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(0);
									selector.validate();
								}
							}
							
							//	edit name
							else if (selector.getSelectedIndex() == 1) {
								Object newEpithet = JOptionPane.showInputDialog(null, ("Please select or enter the correct form of " + bucket.downloadEpithet), "Correct Name", JOptionPane.QUESTION_MESSAGE, null, null, ((downloadEpithet == null) ? bucket.downloadEpithet : downloadEpithet));
								if (newEpithet != null) {
									downloadEpithet = newEpithet.toString().trim();
									if (downloadEpithet.length() == 0) {
										downloadEpithet = null;
										options[1] = (CHANGE_NAME_CHOICE + bucket.downloadEpithet);
									} else {
										options[1] = (CHANGE_NAME_CHOICE + downloadEpithet);
									}
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(1);
									selector.validate();
								}
							}
						}
					}
				});
			}
			
			//	unknown name, maybe new taxon, or false hit
			else if (this.choices == null) {
				
				final String[] options = new String[canUpload ? 6 : 5];
				
				options[0] = ENTER_LSID_CHOICE;
				options[1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
				options[2] = (CHANGE_NAME_CHOICE + nameString);
				options[3] = IGNORE_TAXON_CHOICE;
				options[4] = REMOVE_TAXON_CHOICE;
				if (canUpload) options[5] = UPLOAD_NEW_TAXON_CHOICE;
				
				this.selector = new JComboBox(options);
				this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.selector.setEditable(false);
				this.selector.setSelectedItem((online && canUpload) ? UPLOAD_NEW_TAXON_CHOICE : IGNORE_TAXON_CHOICE);
				
				this.selector.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getClickCount() > 1) {
							
							//	manual LSID input
							if (selector.getSelectedIndex() == 0) {
								Object newLsid = JOptionPane.showInputDialog(null, ("Please enter the LSID for " + bucket.nameString), "Enter LSID", JOptionPane.QUESTION_MESSAGE, null, null, ((lsid == null) ? "" : lsid));
								if (newLsid != null) {
									lsid = newLsid.toString().trim();
									if (lsid.length() == 0) {
										lsid = null;
										options[0] = ENTER_LSID_CHOICE;
									} else {
										options[0] = (lsid + CHANGE_LSID_CHOICE);
									}
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(0);
									selector.validate();
								}
							}
							
							//	change genus
							else if (selector.getSelectedIndex() == 1) {
								Object newGenus = JOptionPane.showInputDialog(null, ("Please select or enter the new genus for " + bucket.nameString), "Enter Genus", JOptionPane.QUESTION_MESSAGE, null, genera, null);
								if (newGenus != null) {
									downloadEpithet = newGenus.toString().trim();
									if (downloadEpithet.length() == 0) {
										downloadEpithet = null;
										options[1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
									} else {
										options[1] = (CHANGE_GENUS_CHOICE + downloadEpithet);
									}
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(1);
									selector.validate();
								}
							}
							
							//	change whole name
							else if (selector.getSelectedIndex() == 2) {
								String newName = changeName();
								if (newName != null) {
									name = newName.trim();
									if (name.length() == 0) {
										name = null;
										options[2] = CHANGE_NAME_CHOICE;
										label.setText(
												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
												"<B>" + bucket.nameString + "</B></HTML>"
											);
										TaxonNameBucketTray.this.setPreferredSize(noChangeSize);
									}
									else {
										options[2] = (CHANGE_NAME_CHOICE + name);
										if (bucket.nameString.equals(name)) {
											label.setText(
												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
												"<B>" + bucket.nameString + "</B></HTML>"
											);
											TaxonNameBucketTray.this.setPreferredSize(noChangeSize);
										}
										else {
											label.setText(
												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
												"<B>" + bucket.nameString + "</B><BR>" +
												"(change to <B>" + name + "</B>)</HTML>"
											);
											TaxonNameBucketTray.this.setPreferredSize(changeSize);
										}
									}
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(2);
									selector.validate();
									label.validate();
								}
							}
						}
					}
				});
			}
			
			//	ambiguous LSID
			else {
				
				final String[] options = new String[this.choices.length + (canUpload ? 6 : 5)];
				
				for (int o = 0; o < this.choices.length; o++) options[o] = this.choices[o].toString();
				options[this.choices.length] = ENTER_LSID_CHOICE;
				options[this.choices.length + 1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
				options[this.choices.length + 2] = (CHANGE_NAME_CHOICE + nameString);
				options[this.choices.length + 3] = IGNORE_TAXON_CHOICE;
				options[this.choices.length + 4] = REMOVE_TAXON_CHOICE;
				if (canUpload) options[this.choices.length + 5] = UPLOAD_NEW_TAXON_CHOICE;
				
				this.selector = new JComboBox(options);
				this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.selector.setEditable(false);
				this.selector.setSelectedIndex(0);
				
				this.selector.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getClickCount() > 1) {
							
							//	enter LSID manually
							if (selector.getSelectedIndex() == choices.length) {
								Object newLsid = JOptionPane.showInputDialog(null, ("Please enter the LSID for " + bucket.nameString), "Enter LSID", JOptionPane.QUESTION_MESSAGE, null, null, ((lsid == null) ? "" : lsid));
								if (newLsid != null) {
									lsid = newLsid.toString().trim();
									if (lsid.length() == 0) {
										lsid = null;
										options[choices.length] = ENTER_LSID_CHOICE;
									}
									else options[choices.length] = (lsid + CHANGE_LSID_CHOICE);
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(choices.length);
									selector.validate();
								}
							}
							
							//	change genus
							else if (selector.getSelectedIndex() == (choices.length + 1)) {
								Object newGenus = JOptionPane.showInputDialog(null, ("Please select or enter the new genus for " + bucket.nameString), "Enter Genus", JOptionPane.QUESTION_MESSAGE, null, genera, null);
								if (newGenus != null) {
									downloadEpithet = newGenus.toString().trim();
									if (downloadEpithet.length() == 0) {
										downloadEpithet = null;
										options[choices.length + 1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
									}
									else options[choices.length + 1] = (CHANGE_GENUS_CHOICE + downloadEpithet);
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(choices.length + 1);
									selector.validate();
								}
							}
							
							//	change whole name
							else if (selector.getSelectedIndex() == (choices.length + 2)) {
								String newName = changeName();
								if (newName != null) {
									name = newName.trim();
									if (name.length() == 0) {
										name = null;
										options[choices.length + 2] = CHANGE_NAME_CHOICE;
										label.setText(
												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
												"<B>" + bucket.nameString + "</B></HTML>"
											);
										TaxonNameBucketTray.this.setPreferredSize(noChangeSize);
									}
									else {
										options[choices.length + 2] = (CHANGE_NAME_CHOICE + name);
										if (bucket.nameString.equals(name)) {
											label.setText(
												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
												"<B>" + bucket.nameString + "</B></HTML>"
											);
											TaxonNameBucketTray.this.setPreferredSize(noChangeSize);
										}
										else {
											label.setText(
												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
												"<B>" + bucket.nameString + "</B><BR>" +
												"(change to <B>" + name + "</B>)</HTML>"
											);
											TaxonNameBucketTray.this.setPreferredSize(changeSize);
										}
									}
									selector.setModel(new DefaultComboBoxModel(options));
									selector.setSelectedIndex(choices.length + 2);
									selector.validate();
									label.validate();
								}
							}
						}
					}
				});
			}
			this.add(this.selector, BorderLayout.SOUTH);
		}
		
		void commitChange() {
			Object selected = this.selector.getSelectedItem();
			
			if (UPLOAD_NEW_TAXON_CHOICE.equals(selected)) {
				return; // upload is handled externally
				//uploadName(this.bucket, this.downloadedThisDocument, this.sd, this.dataProvider);
			}
			
			else if (selected.toString().startsWith(CHANGE_GENUS_CHOICE)) {
				if (this.downloadEpithet != null)
					for (int t = 0; t < this.bucket.taxonNames.size(); t++)
						((Annotation) this.bucket.taxonNames.get(t)).setAttribute(GENUS_ATTRIBUTE, this.downloadEpithet);
			}
			
			else if (selected.toString().startsWith(CHANGE_NAME_CHOICE)) {
				if (GENUS_ATTRIBUTE.equals(this.bucket.downloadEpithetRank)) {
					if (this.name != null) {
						for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
							Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
//							for (int p = 0; p < PART_NAMES.length; p++) {
//								String attribute = PART_NAMES[p];
//								String value = this.nameParts.getProperty(attribute);
//								if (value == null) taxonName.removeAttribute(attribute);
//								else taxonName.setAttribute(attribute, value);
//							}
							for (int r = LsidDataProvider.genusIndex; r < LsidDataProvider.ranks.length; r++) {
								String value = this.nameParts.getProperty(LsidDataProvider.ranks[r].name);
								if (value == null)
									taxonName.removeAttribute(LsidDataProvider.ranks[r].name);
								else taxonName.setAttribute(LsidDataProvider.ranks[r].name, value);
							}
						}
					}
				}
				else if (this.downloadEpithet != null) {
					for (int t = 0; t < this.bucket.taxonNames.size(); t++)
						((Annotation) this.bucket.taxonNames.get(t)).setAttribute(this.bucket.downloadEpithetRank, this.downloadEpithet);
				}
			}
			
			else if (IGNORE_TAXON_CHOICE.equals(selected))
				this.bucket.dataSet = IGNORE;
			
			else if (REMOVE_TAXON_CHOICE.equals(selected))
				this.bucket.dataSet = REMOVE;
			
			else if ((this.choices == null) || (this.selector.getSelectedIndex() == this.choices.length)) {
				if (this.lsid != null)
					for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
						Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
						taxonName.setAttribute((LSID_ATTRIBUTE + "-" + this.dataProvider.getProviderCode()), (this.dataProvider.getLsidUrnPrefix() + this.lsid));
						taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + this.dataProvider.getProviderCode()), ("Manual - " + this.bucket.nameString));
					}
			}
			
			else this.bucket.dataSet = this.choices[this.selector.getSelectedIndex()];
		}
		
		private String changeName() {
			
			//	assemble dialog
			final JDialog td = new JDialog(((JFrame) null), "Correct Name", true);
			JPanel panel = new JPanel(new GridBagLayout());
			td.getContentPane().setLayout(new BorderLayout());
			td.getContentPane().add(panel, BorderLayout.CENTER);
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			panel.add(new JLabel("<HTML>Please enter the correct form of <B>" + bucket.nameString + "</B></HTML>"), gbc.clone());
			gbc.gridy++;
			gbc.gridwidth = 1;
			
			final HashMap fieldsByName = new HashMap();
//			for (int p = 0; p < PART_NAMES.length; p++) {
//				String attribute = PART_NAMES[p];
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				panel.add(new JLabel(attribute, JLabel.LEFT), gbc.clone());
//				
//				JTextField field = new JTextField();
//				fieldsByName.put(attribute, field);
//				field.setBorder(BorderFactory.createLoweredBevelBorder());
//				field.setText(this.nameParts.getProperty(attribute, ""));
//				
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				panel.add(field, gbc.clone());
//				gbc.gridy++;
//			}
			for (int r = LsidDataProvider.genusIndex; r < LsidDataProvider.ranks.length; r++) {
				gbc.gridx = 0;
				gbc.weightx = 0;
				panel.add(new JLabel(LsidDataProvider.ranks[r].name, JLabel.LEFT), gbc.clone());
				
				JTextField field = new JTextField();
				fieldsByName.put(LsidDataProvider.ranks[r].name, field);
				field.setBorder(BorderFactory.createLoweredBevelBorder());
				field.setText(this.nameParts.getProperty(LsidDataProvider.ranks[r].name, ""));
				
				gbc.gridx = 1;
				gbc.weightx = 1;
				panel.add(field, gbc.clone());
				gbc.gridy++;
			}
			
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.setPreferredSize(new Dimension(100, 21));
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					td.dispose();
				}
			});
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cancelButton.setPreferredSize(new Dimension(100, 21));
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					fieldsByName.clear();
					td.dispose();
				}
			});
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			td.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			
			//	ensure dialog is closed with button
			td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			//	get feedback
			td.setSize(500, ((25 * gbc.gridy) + 50));
			td.setLocationRelativeTo(this);
			td.setVisible(true);
			
			if (fieldsByName.isEmpty())
				return null;
//			else for (int p = 0; p < PART_NAMES.length; p++) {
//				String attribute = PART_NAMES[p];
//				JTextField field = ((JTextField) fieldsByName.get(attribute));
//				String value = field.getText().trim();
//				if (value.length() == 0) this.nameParts.remove(attribute);
//				else this.nameParts.setProperty(attribute, value);
//			}
			else for (int r = 0; r < LsidDataProvider.ranks.length; r++) {
				JTextField field = ((JTextField) fieldsByName.get(LsidDataProvider.ranks[r].name));
				String value = field.getText().trim();
				if (value.length() == 0)
					this.nameParts.remove(LsidDataProvider.ranks[r].name);
				else this.nameParts.setProperty(LsidDataProvider.ranks[r].name, value);
			}
			
			String nameString = "";
			if (this.nameParts.containsKey(VARIETY_ATTRIBUTE))
				nameString = ("var. " + this.nameParts.getProperty(VARIETY_ATTRIBUTE) + " " + nameString);
			if (this.nameParts.containsKey(SUBSPECIES_ATTRIBUTE))
				nameString = ("subsp. " + this.nameParts.getProperty(SUBSPECIES_ATTRIBUTE) + " " + nameString);
			if (this.nameParts.containsKey(SPECIES_ATTRIBUTE))
				nameString = (this.nameParts.getProperty(SPECIES_ATTRIBUTE) + " " + nameString);
			if (this.nameParts.containsKey(SUBGENUS_ATTRIBUTE))
				nameString = ("(" + this.nameParts.getProperty(SUBGENUS_ATTRIBUTE) + ") " + nameString);
			if (this.nameParts.containsKey(GENUS_ATTRIBUTE))
				nameString = (this.nameParts.getProperty(GENUS_ATTRIBUTE) + " " + nameString);
			return nameString.trim();
		}
	}
	
	
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
	
	//	return false only to indicate an interrupt of the upload session, i.e. do not upload further names 
	private boolean uploadName(TaxonNameBucket bucket, StringVector downloadedThisDocument, StatusDialog sd, LsidDataProvider dataProvider) {
		
		//	check if upload possible
		if (sd == null) return false;
		if (!dataProvider.isUploadSupported()) {
			JOptionPane.showMessageDialog(sd.getDialog(), ("Taxon name upload is not possible for " + dataProvider.getProviderName() + "."), "Taxon Name Upload Not Possible", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		try {
			
			//	compute parent taxon
			String loadKeyEpithet = null;
			String parentNameString = "";
			String parentNamePart = null;
			String parentRank = null;
			String epithet;
			String rank = null;
			
//			for (int p = 0; p < PART_NAMES.length; p++) {
//				attribute = ((String) bucket.taxonName.getAttribute(PART_NAMES[p]));
//				if (attribute != null) {
//					if (parentNamePart != null)
//						parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
//					parentNamePart = attribute;
//					
//					if (GENUS_ATTRIBUTE.equals(PART_NAMES[p]))
//						loadKeyEpithet = attribute;
//					
//					parentRank = rank;
//					rank = PART_NAMES[p];
//				}
//			}
			for (int r = LsidDataProvider.genusIndex; r < LsidDataProvider.ranks.length; r++) {
				epithet = ((String) bucket.taxonName.getAttribute(LsidDataProvider.ranks[r].name));
				if (epithet != null) {
					if (parentNamePart != null)
						parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
					parentNamePart = epithet;
					
					if (r == LsidDataProvider.genusIndex)
						loadKeyEpithet = epithet;
					
					parentRank = rank;
					rank = LsidDataProvider.ranks[r].name;
				}
			}
			
			//	make it an annotation
			TokenSequence parentTaxonNameTokens = bucket.taxonName.getTokenizer().tokenize(parentNameString);
			Annotation parentTaxonName = Gamta.newAnnotation(parentTaxonNameTokens, TAXONOMIC_NAME_ANNOTATION_TYPE, 0, parentTaxonNameTokens.size());
			parentTaxonName.copyAttributes(bucket.taxonName);
			parentTaxonName.removeAttribute(rank); // remove lowest epithet
			parentTaxonName.setAttribute(RANK_ATTRIBUTE, parentRank); // adjust rank
			
			//	compute load key
			if (GENUS_ATTRIBUTE.equals(rank))
				loadKeyEpithet = null;
			
			//	get parent data
			LsidDataSet[] parentData = this.getParentData(loadKeyEpithet, parentTaxonName, dataProvider);
			
			//	do upload
			bucket.dataSet = dataProvider.doUpload(sd, bucket.taxonName, bucket.isNewTaxon, parentData);
			
			//	upload failed
			if (bucket.dataSet == null) {
				
				//	attach failure indication attributes to annotations
				for (int t = 0; t < bucket.taxonNames.size(); t++) {
					Annotation taxonName = ((Annotation) bucket.taxonNames.get(t));
					taxonName.setAttribute((LSID_ATTRIBUTE + "-" + dataProvider.getProviderCode()), "Upload Failed");
					taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + dataProvider.getProviderCode()), "Not available");
				}
			}
			
			//	upload successful
			else {
				
				//	attach attributes to annotations
				for (int t = 0; t < bucket.taxonNames.size(); t++) {
					Annotation taxonName = ((Annotation) bucket.taxonNames.get(t));
					taxonName.setAttribute((LSID_ATTRIBUTE + "-" + dataProvider.getProviderCode()), (dataProvider.getLsidUrnPrefix() + bucket.dataSet.lsidNumber));
					taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + dataProvider.getProviderCode()), bucket.dataSet.taxonName);
				}
				
				//	refresh cache file (if exists)
				if (bucket.downloadEpithet != null) try {
					dataProvider.updateCache(bucket.downloadEpithet, bucket.dataSet);
				}
				
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
			}
			
			//	indicate to continue upload
			return true;
		}
		
		//	upload dialog or entire upload session cancelled
		catch (IOException e) {
			if (e == LsidDataProvider.STOP_UPLOADING) return false;
			
			System.out.println("LsidReferencer: exception uploading '" + bucket.taxonName.getValue() + "' to " + dataProvider.getProviderName() + " (" + dataProvider.getProviderCode() + "): " + e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace(System.out);
			return true;
		}
	}
	
	private LsidDataSet[] getParentData(String loadKeyEpithet, Annotation parentTaxonName, LsidDataProvider dataProvider) {
		ArrayList dataSets = new ArrayList();
		
		//	genus attribute missing, maybe above genus rank name 
		if (loadKeyEpithet == null) {
			
			//	get and classify name
			if ((parentTaxonName.size() == 1) && Gamta.isFirstLetterUpWord(parentTaxonName.firstValue())) {
				String name = Gamta.capitalize(parentTaxonName.firstValue());
				if (name.endsWith("idae")) loadKeyEpithet = name;
				else if (name.endsWith("inae")) loadKeyEpithet = name;
				else if (name.endsWith("ini")) loadKeyEpithet = name;
			}
			
			//	no higher level name either
			if (loadKeyEpithet == null) return new LsidDataSet[0];
			
			//	get data
			LsidDataSet[] lsidData = dataProvider.getLsidData(loadKeyEpithet, true, new StringVector(), null, false);
			
			//	do string match scoring
			String name = Gamta.capitalize(parentTaxonName.firstValue());
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
		}
		
		else {
			
			//	get data
			LsidDataSet[] lsidData = dataProvider.getLsidData(loadKeyEpithet, true, new StringVector(), null, false);
			
			//	score data sets
			for (int d = 0; d < lsidData.length; d++) {
				int score = lsidData[d].getMatchScore(parentTaxonName, false);
				if (score != 0)
					dataSets.add(lsidData[d].getScoredCopy(score));
			}
		}
		
		//	sort and return data sets
		Collections.sort(dataSets);
		return ((LsidDataSet[]) dataSets.toArray(new LsidDataSet[dataSets.size()]));
	}
}