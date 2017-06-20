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
//import java.awt.BorderLayout;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.Writer;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Properties;
//import java.util.regex.Pattern;
//
//import javax.swing.BorderFactory;
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JDialog;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTextField;
//
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.QueriableAnnotation;
//import de.uka.ipd.idaho.gamta.Token;
//import de.uka.ipd.idaho.gamta.TokenSequence;
//import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
//import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
//import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
//import de.uka.ipd.idaho.htmlXmlUtil.Parser;
//import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
//import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
//import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
//import de.uka.ipd.idaho.stringUtils.StringUtils;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
//import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;
//
///**
// * @author sautter
// *
// */
//public class TnuluLsidReferencer extends AbstractConfigurableAnalyzer implements LiteratureConstants, TaxonomicNameConstants {
//	
//	private static final String TNULU_REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
//	private static final String TNULU_NEW_UPLOAD_URL_ATTRIBUTE = "NewUploadBaseURL";
//	private static final String TNULU_KNOWN_UPLOAD_URL_ATTRIBUTE = "KnownUploadBaseURL";
//	
//	private static final String UPLOAD_USERNAME_ATTRIBUTE = "UploadUserName";
//	private static final String UPLOAD_PASSWORD_ATTRIBUTE = "UploadPassword";
//	private static final String UPLOAD_STATUS_CODE_ATTRIBUTE = "UploadStatusCode";
////	private static final String UPLOAD_VALIDITY_ATTRIBUTE = "UploadValidity";
//	private static final String DEFAULT_UPLOAD_CONCEPT_STATUS_ATTRIBUTE = "DefaultUploadConceptStatus";
//	private static final String DEFAULT_UPLOAD_RELATION_TYPE_ATTRIBUTE = "DefaultUploadRelationType";
//	
//	private static final String WWW_PROXY_ATTRIBUTE = "WwwProxy";
//	private static final String WWW_PROXY_PORT_ATTRIBUTE = "WwwProxyPort";
//	
//	private static final String TaxonNameUseID = "TaxonNameUseID";
//	private static final String TaxonNameID = "TaxonNameID";
//	private static final String TaxonomicConceptID = "TaxonomicConceptID";
//	private static final String Rank = "Rank";
//	private static final String Valid = "Valid";
//	private static final String StatusCode = "StatusCode";
//	private static final String TaxonName = "TaxonName";
//	
//	private static final String[] AttributeNames = {TaxonNameUseID, TaxonNameID, TaxonomicConceptID, Rank, Valid, StatusCode, TaxonName};
//	
//	private static final String LSID_ATTRIBUTE = "LSID";
//	private static final String LSID_NAME_ATTRIBUTE = "lsidName";
//	
//	//	TODO: put these two HNS specific values in LsidProvide objects, handling the LSID server communication & caching, and create such providers for HNS, ZooBank, ...
//	private static final String LSID_PROVIDER_SUFFIX = "HNS";
//	private static final String LSID_ATTRIBUTE_PREFIX = "urn:lsid:biosci.ohio-state.edu:osuc_concepts:";
//	
//	private static final String UNKNOWN = "Unknown";
//	private static final String LSID_REGEX = "[1-9][0-9]*+";
//	private static final Pattern LSID_PATTERN = Pattern.compile(LSID_REGEX);
//	
//	private static final String ENTER_LSID_CHOICE = "<Double click to enter LSID manually>";
//	private static final String CHANGE_LSID_CHOICE = " <Double click to change LSID>";
//	private static final String CHANGE_GENUS_CHOICE = "<Double click to  change genus> ";
//	private static final String CHANGE_NAME_CHOICE = "<Double click to change name> ";
//	private static final String IGNORE_TAXON_CHOICE = "Ignore taxonomic name on LSID referencing, or get LSID later";
//	private static final String REMOVE_TAXON_CHOICE = "Remove taxonomic name Annotation";
//	private static final String UPLOAD_NEW_TAXON_CHOICE = "Upload new taxonomic name to HNS and obtain LSID";
//	
//	//	http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu?the_name=
//	//	http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu_newname?
//	
//	private String tnuluRequestBaseUrl;
//	private StringVector keys = new StringVector();
//	
//	//	put these parameters in config file
//	private String tnuluKnownUploadBaseUrl = "http://atbi.biosci.ohio-state.edu:210/hymenoptera/Taxon_Holding.addTaxonHolder?";//"http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu_newname?";
//	private String tnuluNewUploadBaseUrl = "http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu_newname?";
//	private String tnuluUserName = "TaxonX";
//	private String tnuluPassword = "TaxonX";
//	private String tnuluUploadStatusCode = "To Be Verified At HNS";
//	private String tnuluUploadValidity = "Valid";
//	private String defaultConceptStatusOption = "Misspelling";
//	private String defaultRelationTypeOption = "Synonym";
//	
//	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
//		this.keys.addContent(AttributeNames);
//		
//		//	get request url
//		this.tnuluRequestBaseUrl = this.getParameter(TNULU_REQUEST_URL_ATTRIBUTE);
//		
//		//	read upload data
//		this.tnuluNewUploadBaseUrl = this.getParameter(TNULU_NEW_UPLOAD_URL_ATTRIBUTE);
//		this.tnuluKnownUploadBaseUrl = this.getParameter(TNULU_KNOWN_UPLOAD_URL_ATTRIBUTE);
//		this.tnuluUserName = this.getParameter(UPLOAD_USERNAME_ATTRIBUTE);
//		this.tnuluPassword = this.getParameter(UPLOAD_PASSWORD_ATTRIBUTE);
//		
//		//	check if upload enabled
//		if ((this.tnuluNewUploadBaseUrl == null) || (this.tnuluKnownUploadBaseUrl == null) || (this.tnuluUserName == null) || (this.tnuluPassword == null)) {
//			this.tnuluNewUploadBaseUrl = null;
//			this.tnuluKnownUploadBaseUrl = null;
//			this.tnuluUserName = null;
//			this.tnuluPassword = null;
//			
//		//	if so, read upload options
//		} else {
//			this.tnuluUploadStatusCode = this.getParameter(UPLOAD_STATUS_CODE_ATTRIBUTE, this.tnuluUploadStatusCode);
//			this.tnuluUploadValidity = this.getParameter(UPLOAD_STATUS_CODE_ATTRIBUTE, this.tnuluUploadValidity);
//			this.defaultConceptStatusOption = this.getParameter(DEFAULT_UPLOAD_CONCEPT_STATUS_ATTRIBUTE);
//			this.defaultRelationTypeOption = this.getParameter(DEFAULT_UPLOAD_RELATION_TYPE_ATTRIBUTE);
//			
//			try {
//				StringVector list = this.loadList(CONCEPT_RANK_OPTIONS_FILE_NAME);
//				this.conceptRankOptions = list.toStringArray();
//			} catch (IOException ioe) {
//				this.conceptRankOptions = DEFAULT_CONCEPT_RANK_OPTIONS;
//			}
//			
//			try {
//				StringVector list = this.loadList(CONCEPT_STATUS_OPTIONS_FILE_NAME);
//				this.conceptStatusOptions = list.toStringArray();
//			} catch (IOException ioe) {
//				this.conceptStatusOptions = DEFAULT_CONCEPT_STATUS_OPTIONS;
//			}
//			
//			try {
//				StringVector list = this.loadList(COUNTRY_OPTIONS_FILE_NAME);
//				this.countryOptions = list.toStringArray();
//			} catch (IOException ioe) {
//				this.countryOptions = DEFAULT_COUNTRY_OPTIONS;
//			}
//			
//			try {
//				StringVector list = this.loadList(RELATION_TYPE_OPTIONS_FILE_NAME);
//				this.relationTypeOptions = list.toStringArray();
//			} catch (IOException ioe) {
//				this.relationTypeOptions = DEFAULT_RELATION_TYPE_OPTIONS;
//			}
//			
//			try {
//				StringVector list = this.loadList(REMARK_PRESET_OPTIONS_FILE_NAME);
//				list.insertElementAt(DEFAULT_REMARK_OPTION, 0);
//				this.remarkPresetOptions = list.toStringArray();
//			} catch (IOException ioe) {
//				String[] options = {DEFAULT_REMARK_OPTION};
//				this.remarkPresetOptions = options;
//			}
//		}
//		
//		String wwwProxy = this.getParameter(WWW_PROXY_ATTRIBUTE);
//		if (wwwProxy != null) {
//			System.getProperties().put("proxySet", "true");
//			System.getProperties().put("proxyHost", wwwProxy);
//			String wwwProxyPort = this.getParameter(WWW_PROXY_PORT_ATTRIBUTE);
//			if (wwwProxyPort != null)
//				System.getProperties().put("proxyPort", wwwProxyPort);
//		}
//	}
//	
//	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	check parameter
//		if (data == null) return;
//		
//		//	find page numbers for taxonomic names
//		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
//		Properties pageNumberByTaxonName = new Properties();
//		Properties nomenclaturePageNumberByTaxonName = new Properties();
//		for (int p = 0; p < paragraphs.length; p++) {
//			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
//			if (pageNumber != null) {
//				Annotation[] taxonomicNames = paragraphs[p].getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//				Annotation[] taxonomicNameLabels = paragraphs[p].getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
//				
//				for (int t = 0; t < taxonomicNames.length; t++) {
//					taxonomicNames[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
//					pageNumberByTaxonName.setProperty(taxonomicNames[t].getAnnotationID(), pageNumber.toString());
//					
//					//	remember page of taxon name directly preceeding the only one label in the paragraph, might be nomenclature paragraph for that name
//					if ((taxonomicNameLabels.length == 1) && (Math.abs(taxonomicNameLabels[0].getStartIndex() - taxonomicNames[t].getEndIndex()) < 2))
//						nomenclaturePageNumberByTaxonName.setProperty(taxonomicNames[t].getAnnotationID(), pageNumber.toString());
//				}
//			}
//		}
//		
//		//	get and bucketize taxonomic names
//		StringVector bucketNames = new StringVector();
//		HashMap bucketsByName = new HashMap();
//		int li = 0;
//		
//		Annotation[] taxonomicNames = data.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
//		Annotation[] labels = data.getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
//		
//		for (int t = 0; t < taxonomicNames.length; t++) {
//			String taxonName = this.getNameString(taxonomicNames[t]);
//			String pageNumber = pageNumberByTaxonName.getProperty(taxonomicNames[t].getAnnotationID());
//			String nomenclaturePageNumber = nomenclaturePageNumberByTaxonName.getProperty(taxonomicNames[t].getAnnotationID());
//			
//			//	attributes missing, or above genus rank name
//			if (taxonName.length() == 0) {
//				if (taxonomicNames[t].size() == 1) {
//					String name = Gamta.capitalize(taxonomicNames[t].firstValue());
//					if (!name.endsWith("idae") && !name.endsWith("inae") && !name.endsWith("ini"))
//						taxonName = name;
//					else taxonName = taxonomicNames[t].firstValue();
//				} else taxonName = taxonomicNames[t].getValue();
//			}
//			
//			//	put into bucket
//			TaxonNameBucket bucket = ((TaxonNameBucket) bucketsByName.get(taxonName));
//			if (bucket == null) {
//				bucket = new TaxonNameBucket(taxonomicNames[t], ((pageNumber == null) ? "" : pageNumber));
//				bucketNames.addElementIgnoreDuplicates(taxonName);
//				bucketsByName.put(taxonName, bucket);
//			}
//			bucket.taxonNames.add(taxonomicNames[t]);
//			if (nomenclaturePageNumber != null)
//				bucket.pageNumber = nomenclaturePageNumber;
//			
//			//	mark taxonomic names that are labeled as new
//			while ((li < labels.length) && (taxonomicNames[t].getStartIndex() > labels[li].getStartIndex())) li++;
//			if (li < labels.length) {
//				if (taxonomicNames[t].getEndIndex() == labels[li].getStartIndex()) bucket.isNewTaxon = true;
//				else if (((taxonomicNames[t].getEndIndex() + 1) == labels[li].getStartIndex())
//						&& ",".equals(data.valueAt(taxonomicNames[t].getEndIndex()))
//					) bucket.isNewTaxon = true;
//			}
//		}
//		
//		//	line up buckets
//		ArrayList buckets = new ArrayList();
//		for (int b = 0; b < bucketNames.size(); b++)
//			buckets.add(bucketsByName.get(bucketNames.get(b)));
//		
//		//	transfer LSID if one annotation in bucket already has one, and sort out these buckets
//		int bucketIndex = 0;
//		while (bucketIndex < buckets.size()) {
//			TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(bucketIndex));
//			String existingLsid = null;
//			String existingLsidName = null;
//			for (int t = 0; t < tnb.taxonNames.size(); t++) {
//				Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
//				String lsid = taxonName.getAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), UNKNOWN).toString();
//				if (LSID_PATTERN.matcher(lsid).matches()) {
//					String lsidName = taxonName.getAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), "").toString();
//					if ((lsidName.length() != 0)) {
//						existingLsid = lsid;
//						existingLsidName = lsidName;
//					}
//				}
//			}
//			
//			//	LSID not found, keep bucket
//			if (existingLsid == null) bucketIndex++;
//			
//			//	found proper LSID in Annotations, bucket done
//			else {
//				for (int t = 0; t < tnb.taxonNames.size(); t++) {
//					Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
//					taxonName.setAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), existingLsid);
//					taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), existingLsidName);
//				}
//				buckets.remove(bucketIndex);
//			}
//		}
//		
//		//	create status dialog
//		StatusDialog sd = null;
//		if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
//			sd = new StatusDialog();
//			sd.online.setSelected(parameters.containsKey(ONLINE_PARAMETER));
//			sd.popUp();
//		}
//		
//		//	process buckets
//		StringVector downloadedThisDocument = new StringVector(false);
//		while (!buckets.isEmpty()) {
//			StringVector genera = new StringVector();
//			
//			//	try to load data for buckets
//			for (int b = 0; b < buckets.size(); b++) {
//				TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(b));
//				String genus = tnb.taxonName.getAttribute(GENUS_ATTRIBUTE, "").toString();
//				
//				//	check for interrupt
//				if ((sd != null) && sd.interrupted) {
//					sd.dispose();
//					return;
//				}
//				
//				//	full genus given, load data
//				if (genus.length() > 2) {
//					tnb.downloadNamePart = genus;
//					genera.addElementIgnoreDuplicates(genus);
//					
//					if (sd != null) sd.setLabel("Getting data for '" + genus + "' ...");
//					
//					StringRelation genusData = this.getLsidData(genus, true, true, downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER));
//					if (genusData != null) {
//						ArrayList dataSets = new ArrayList();
//						
//						//	score data sets
//						for (int d = 0; d < genusData.size(); d++) {
//							StringTupel dataSet = genusData.get(d);
//							int score = this.score(dataSet, tnb.taxonName, downloadedThisDocument.contains(genus)); // no fuzzy match for local data, unless data is fresh
//							if (score != 0) dataSets.add(new LsidDataSet(dataSet, score));
//						}
//						
//						//	if nothing found, refresh data set from HNS
//						if (dataSets.isEmpty() && !downloadedThisDocument.containsIgnoreCase(genus)) {
//							if (sd != null) sd.setLabel("  ==> Taxon '" + tnb.taxonName.getValue() + "' not found in local genus data.");
//							
//							//	ask for web access if in offline mode
//							if ((sd != null) && !sd.online.isSelected()) {
//								if (JOptionPane.showConfirmDialog(sd, ("Tnulu LSID Referencer is in offline mode, allow fetching LSID data for '" + genus + "' anyway?"), "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
//									sd.online.setSelected(true);
//							}
//							
//							//	if web access allowed, get data from LSID server
//							if (((sd != null) && sd.online.isSelected()) || ((sd == null) && parameters.containsKey(ONLINE_PARAMETER)))
//								genusData = this.getLsidData(genus, false, false, downloadedThisDocument, sd, true);
//							
//							//	score data sets
//							for (int d = 0; (genusData != null) && (d < genusData.size()); d++) {
//								StringTupel dataSet = genusData.get(d);
//								int score = this.score(dataSet, tnb.taxonName, true); // use fuzzy match for fresh data
//								if (score != 0) dataSets.add(new LsidDataSet(dataSet, score));
//							}
//						}
//						
//						//	use top data set
//						Collections.sort(dataSets);
//						if (!dataSets.isEmpty()) {
//							LsidDataSet top = ((LsidDataSet) dataSets.get(0));
//							int index = 1;
//							while (index < dataSets.size()) {
//								if (((LsidDataSet) dataSets.get(index)).score < top.score)
//									dataSets.remove(index);
//								else index++;
//							}
//							
//							String nameRank = tnb.taxonName.getAttribute(RANK_ATTRIBUTE, "").toString();
//							
//							//	ranks match, or "subSpecies" on HNS and "variety" in Annotation (HNS doesn't use "variety")
//							if (top.rank.equalsIgnoreCase(nameRank) || (SUBSPECIES_ATTRIBUTE.equalsIgnoreCase(top.rank) && VARIETY_ATTRIBUTE.equalsIgnoreCase(nameRank))) {
//								
//								//	clear choice
//								if (dataSets.size() == 1) tnb.dataSet = top;
//								
//								//	ambiobuous choice, ask feedback
//								else if (parameters.containsKey(INTERACTIVE_PARAMETER)) tnb.dataSets.addAll(dataSets);
//								
//								//	feedback not allowed
//								else tnb.dataSet = IGNORE;
//								
//							//	rank not matching, ask for feedback
//							} else if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
//								tnb.dataSets.addAll(dataSets);
//								
//							//	feedback not allowed
//							} else tnb.dataSet = IGNORE;
//						}
//						
//					//	update status display
//					} else if (sd != null) sd.setLabel("  ==> Data not available.");
//					
//				//	genus attribute missing, maybe above genus rank name 
//				} else if ((tnb.taxonName.size() == 1) && Gamta.isFirstLetterUpWord(tnb.taxonName.firstValue())) {
//					
//					//	get and classify name
//					String name = Gamta.capitalize(tnb.taxonName.firstValue());
//					if (name.endsWith("idae")) {
//						tnb.downloadNamePart = name;
//						tnb.downloadNamePartRank = FAMILY_ATTRIBUTE;
//					} else if (name.endsWith("inae")) {
//						tnb.downloadNamePart = name;
//						tnb.downloadNamePartRank = SUBFAMILY_ATTRIBUTE;
//					} else if (name.endsWith("ini")) {
//						tnb.downloadNamePart = name;
//						tnb.downloadNamePartRank = TRIBE_ATTRIBUTE;
//					}
//					
//					//	above genus taxon name
//					if (!GENUS_ATTRIBUTE.equals(tnb.downloadNamePartRank)) {
//						if (sd != null) sd.setLabel("Getting data for '" + name + "' ...");
//						
//						StringRelation lsidData = this.getLsidData(name, true, true, downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER));
//						
//						//	score data sets
//						ArrayList dataSets = new ArrayList();
//						if (lsidData != null)
//							for (int d = 0; d < lsidData.size(); d++) {
//								StringTupel dataSet = lsidData.get(d);
//								String nameString = dataSet.getValue(TaxonName);
//								
//								//	cut author name if given
//								if (nameString.indexOf(' ') != -1)
//									nameString = nameString.substring(0, nameString.indexOf(' '));
//								
//								//	match them
//								if (name.equals(nameString))
//									dataSets.add(new LsidDataSet(dataSet, 2));
//								else if (name.equalsIgnoreCase(nameString))
//									dataSets.add(new LsidDataSet(dataSet, 1));
//							}
//						
//						//	if nothing found, refresh data set from HNS
//						if (dataSets.isEmpty() && !downloadedThisDocument.containsIgnoreCase(name)) {
//							if (sd != null) sd.setLabel("  ==> Taxon '" + tnb.taxonName.getValue() + "' not found in local data.");
//							
//							//	ask for web access if in offline mode
//							if ((sd != null) && !sd.online.isSelected()) {
//								if (JOptionPane.showConfirmDialog(sd, ("Tnulu LSID Referencer is in offline mode, allow fetching LSID data for '" + name + "' anyway?"), "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
//									sd.online.setSelected(true);
//							}
//							
//							//	if web access allowed, get data from LSID server
//							if (((sd != null) && sd.online.isSelected()) || ((sd == null) && parameters.containsKey(ONLINE_PARAMETER)))
//								lsidData = this.getLsidData(name, false, false, downloadedThisDocument, sd, true);
//							
//							//	score data sets
//							for (int d = 0; (lsidData != null) && (d < lsidData.size()); d++) {
//								StringTupel dataSet = lsidData.get(d);
//								String nameString = dataSet.getValue(TaxonName);
//								if (name.equals(nameString))
//									dataSets.add(new LsidDataSet(dataSet, 2));
//								else if (name.equalsIgnoreCase(nameString))
//									dataSets.add(new LsidDataSet(dataSet, 1));
//							}
//						}
//						
//						//	use top data set
//						Collections.sort(dataSets);
//						if (!dataSets.isEmpty()) {
//							LsidDataSet top = ((LsidDataSet) dataSets.get(0));
//							int index = 1;
//							while (index < dataSets.size()) {
//								if (((LsidDataSet) dataSets.get(index)).score < top.score)
//									dataSets.remove(index);
//								else index++;
//							}
//							
//							//	clear choice
//							if (dataSets.size() == 1) tnb.dataSet = top;
//							
//							//	ambiguous choice, ask for feedback
//							else if (parameters.containsKey(INTERACTIVE_PARAMETER)) tnb.dataSets.addAll(dataSets);
//							
//							//	feedback not allowed
//							else tnb.dataSet = IGNORE;
//						}
//					}
//				}
//			}
//			
//			//	process data
//			bucketIndex = 0;
//			while (bucketIndex < buckets.size()) {
//				TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(bucketIndex));
//				
//				//	not allowed to bet feedback, ignore name
//				if (tnb.dataSet == IGNORE) {
//					buckets.remove(bucketIndex);
//					
//				//	found unambiguous LSID
//				} else if (tnb.dataSet != null) {
//					for (int t = 0; t < tnb.taxonNames.size(); t++) {
//						Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
//						taxonName.setAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), (LSID_ATTRIBUTE_PREFIX + tnb.dataSet.taxonNameUseID));
//						taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), tnb.dataSet.taxonName);
//					}
//					
//					//	bucket done
//					buckets.remove(bucketIndex);
//					
//				//	keep bucket for feedback
//				} else bucketIndex++;
//			}
//			
//			//	get feedback for ambiguous or unknown names
//			this.getFeedback(((TaxonNameBucket[]) buckets.toArray(new TaxonNameBucket[buckets.size()])), genera.toStringArray(), downloadedThisDocument, sd, parameters.containsKey(ONLINE_PARAMETER), data);
//			
//			//	process feedback
//			bucketIndex = 0;
//			while (bucketIndex < buckets.size()) {
//				TaxonNameBucket tnb = ((TaxonNameBucket) buckets.get(bucketIndex));
//				
//				//	remove
//				if (tnb.dataSet == REMOVE) {
//					for (int t = 0; t < tnb.taxonNames.size(); t++)
//						data.removeAnnotation((Annotation) tnb.taxonNames.get(t));
//					
//					//	bucket done
//					buckets.remove(bucketIndex);
//					
//				//	ignore taxon name for now
//				} else if (tnb.dataSet == IGNORE) {
//					buckets.remove(bucketIndex);
//					
//				//	LSID selected, or upload
//				} else if (tnb.dataSet != null) {
//					for (int t = 0; t < tnb.taxonNames.size(); t++) {
//						Annotation taxonName = ((Annotation) tnb.taxonNames.get(t));
//						taxonName.setAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), (LSID_ATTRIBUTE_PREFIX + tnb.dataSet.taxonNameUseID));
//						taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), tnb.dataSet.taxonName);
//					}
//					
//					//	bucket done
//					buckets.remove(bucketIndex);
//					
//				//	LSID entered manually, or upload failed
//				} else if (tnb.taxonName.hasAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX))) {
//					
//					//	bucket done
//					buckets.remove(bucketIndex);
//					
//				//	genus or entire name changed, keep bucket for next round
//				} else bucketIndex++;
//			}
//		}
//		
//		//	close status dialog
//		if (sd != null) sd.dispose();
//	}
//	
//	//	return false only if interrupted
//	private void getFeedback(TaxonNameBucket[] buckets, String[] genera, StringVector downloadedThisDocument, StatusDialog sd, boolean online, TokenSequence text) {
//		
//		//	don't show empty dialog
//		if (buckets.length == 0) return;
//		
//		//	assemble dialog
//		final JDialog td = new JDialog(sd, "Please Check Unclear LSIDs", true);
//		JPanel panel = new JPanel(new GridBagLayout());
//		JScrollPane panelBox = new JScrollPane(panel);
//		panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		panelBox.getVerticalScrollBar().setUnitIncrement(50);
//		td.getContentPane().setLayout(new BorderLayout());
//		td.getContentPane().add(panelBox, BorderLayout.CENTER);
//		
//		final GridBagConstraints gbc = new GridBagConstraints();
//		gbc.insets.top = 2;
//		gbc.insets.bottom = 2;
//		gbc.insets.left = 5;
//		gbc.insets.right = 5;
//		gbc.fill = GridBagConstraints.HORIZONTAL;
//		gbc.weightx = 1;
//		gbc.weighty = 0;
//		gbc.gridwidth = 1;
//		gbc.gridheight = 1;
//		gbc.gridx = 0;
//		gbc.gridy = 0;
//		
//		//	add classification boxes
//		Arrays.sort(genera);
//		TaxonBucketTray[] trays = new TaxonBucketTray[buckets.length];
//		for (int b = 0; b < buckets.length; b++) {
//			trays[b] = new TaxonBucketTray(buckets[b], genera, downloadedThisDocument, sd, text);
//			panel.add(trays[b], gbc.clone());
//			gbc.gridy++;
//		}
//		
//		//	add OK button
//		JButton okButton = new JButton("OK");
//		okButton.setBorder(BorderFactory.createRaisedBevelBorder());
//		okButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				td.dispose();
//			}
//		});
//		
//		//	add OK button
//		JButton cancelButton = new JButton("Cancel");
//		cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
//		cancelButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				gbc.fill = GridBagConstraints.NONE;
//				td.dispose();
//			}
//		});
//		
//		JPanel buttonPanel = new JPanel(new GridBagLayout());
//		gbc.gridwidth = 1;
//		gbc.gridheight = 1;
//		gbc.gridx = 0;
//		gbc.gridy = 0;
//		buttonPanel.add(okButton, gbc.clone());
//		gbc.gridx++;
//		buttonPanel.add(cancelButton, gbc.clone());
//		td.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//		
//		//	ensure dialog is closed with button
//		td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
//		
//		//	get feedback
//		td.setSize(500, (Math.min(650, (78 * buckets.length) + 50)));
//		td.setLocationRelativeTo(null);
//		td.setVisible(true);
//		
//		//	process feedback
//		boolean doUpload = true;
//		for (int t = 0; t < trays.length; t++) {
//			
//			//	upload, check if cancelled
//			if (UPLOAD_NEW_TAXON_CHOICE.equals(trays[t].selector.getSelectedItem())) {
//				if (doUpload) {
//					if (online || (JOptionPane.showConfirmDialog(sd, "LSID Referencer has been invoked in offline mode, allow uploading new taxonomic names anyway?", "Allow Web Access?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
//						online = true; // ask only once
//						
//						//	do upload
//						trays[t].commitChange();
//						
//						//	uploading cancelled for this round
//						if (trays[t].bucket.dataSet == CANCEL_UPLOADING) {
//							doUpload = false;
//							trays[t].bucket.dataSet = null;
//						}
//						
//					//	stop uploading immediately if web access denied
//					} else doUpload = false;
//				}
//				
//			//	other option
//			} else trays[t].commitChange();
//			
//			//	main dialog cancelled, ignore all buckets not having an LSID (or dummy for other option) selected
//			if ((gbc.fill == GridBagConstraints.NONE) && trays[t].bucket.dataSet == null)
//				trays[t].bucket.dataSet = IGNORE;
//		}
//	}
//	
//	private static final String MISSING_PART = "####";
//	
//	private String getNameString(Annotation taxonomicName) {
//		String nameString = "";
//		String attribute;
//		
//		attribute = taxonomicName.getAttribute(VARIETY_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(attribute)) nameString = ("var. " + attribute + " " + nameString);
//		
//		attribute = taxonomicName.getAttribute(SUBSPECIES_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(attribute)) nameString = ("subsp. " + attribute + " " + nameString);
//		
//		attribute = taxonomicName.getAttribute(SPECIES_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(attribute)) nameString = (attribute + " " + nameString);
//		
//		attribute = taxonomicName.getAttribute(SUBGENUS_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(attribute)) nameString = ("(" + attribute + ") " + nameString);
//		
//		attribute = taxonomicName.getAttribute(GENUS_ATTRIBUTE, MISSING_PART).toString();
//		if (!MISSING_PART.equals(attribute)) nameString = (attribute + " " + nameString);
//		
//		return nameString.trim();
//	}
//	
//	private static final boolean DEBUG_MATCH = false;
//	private int score(StringTupel dataSet, Annotation taxonomicName, boolean allowFuzzyMatch) {
//		
//		if (DEBUG_MATCH) System.out.println(taxonomicName.toXML());
//		if (DEBUG_MATCH) System.out.println(dataSet.toString());
//		
//		//	compare rank
//		String dataRank = dataSet.getValue(Rank, UNKNOWN);
//		String nameRank = taxonomicName.getAttribute(RANK_ATTRIBUTE, "").toString();
//		
//		//	ranks match, or "subSpecies" on HNS and "variety" in Annotation (HNS doesn't use "variety")
//		if (true || dataRank.equalsIgnoreCase(nameRank) || (SUBSPECIES_ATTRIBUTE.equalsIgnoreCase(dataRank) && VARIETY_ATTRIBUTE.equalsIgnoreCase(nameRank))) {
//			
//			//	compare name details
//			String nameString = dataSet.getValue(TaxonName);
//			StringVector nameParts = TokenSequenceUtils.getTextTokens(taxonomicName.getTokenizer().tokenize(nameString));
//			if (DEBUG_MATCH) System.out.println(nameParts.concatStrings(", "));
//			
//			int score = 0;
//			boolean gotMostSignificantPart = false;
//			int misses = 0;
//			for (int p = 5; p > 0; p--) {
//				String attribute = taxonomicName.getAttribute(PART_NAMES[p - 1], MISSING_PART).toString();
//				
//				if (DEBUG_MATCH) System.out.println(PART_NAMES[p - 1] + ": " + attribute);
//				
//				if (!MISSING_PART.equals(attribute)) {
//					if (nameParts.contains(attribute)) {
//						if (DEBUG_MATCH) System.out.println("==> match");
//						score += (100 * p);
//						nameParts.remove(attribute);
//					}
//					
//					else if (allowFuzzyMatch) {
//						
//						//	do fuzzy match
//						int minDist = nameString.length();
//						String minDistPart = "";
//						for (int n = 0; n < nameParts.size(); n++) {
//							int dist = StringUtils.getLevenshteinDistance(attribute, nameParts.get(n), 1, true);
//							if (dist < minDist) {
//								minDist = dist;
//								minDistPart = nameParts.get(n);
//							}
//						}
//						
//						if (minDist <= 1) {
//							if (DEBUG_MATCH) System.out.println("==> fuzzy match (" + minDist + ")");
//							score += ((100 * p) / (minDist + 1));
//							nameParts.remove(minDistPart);
//						}
//						
//						else {
//							if (DEBUG_MATCH) System.out.println("==> no match");
//							if (!gotMostSignificantPart) {
//								if (DEBUG_MATCH) System.out.println("==> match impossible");
//								return 0;
//							}
//							else misses++;
//						}
//					}
//					
//					else {
//						if (DEBUG_MATCH) System.out.println("==> no match");
//						if (!gotMostSignificantPart) {
//							if (DEBUG_MATCH) System.out.println("==> match impossible");
//							return 0;
//						}
//						else misses++;
//					}
//					
//					gotMostSignificantPart = true;
//				}
//			}
//			
//			//	in subSpecies & varieties, problems with labels & author names (nameParts.size() too large due to these parts)
////			score = (nameParts.isEmpty() ? (score * 2) : (score / nameParts.size()));
//			
//			//	in species, problems with subSpecies & varieties with same last part (score too high)
////			score = (nameParts.isEmpty() ? (score * 2) : (score));
//			
//			//	in species, problems with HNS subSpecies & varieties (remaining of latter not penalized)
////			score = (score / (misses + 1));
//			
//			//	problems with subSpecies & varieties upgraded to species
////			score = (score / (nameParts.size() + 1));
//			
//			//	scheint zu tun
//			score = (score / (nameParts.size() + misses + 1));
//			
////			System.out.println("==> complete match, score is " + score);
//			return score;
//		} else return 0;
//	}
//	
////	private LsidDataSet[] findLsid(StringRelation data, Annotation taxonomicName, boolean allowFuzzyMatch) {
////		//	TODO: use this for getting data, and provide more than top element in interactive mode 
////		ArrayList dataSets = new ArrayList();
////		for (int d = 0; d < data.size(); d++) {
////			StringTupel dataSet = data.get(d);
////			int score = this.score(dataSet, taxonomicName, allowFuzzyMatch);
////			if (score != 0) dataSets.add(new LsidDataSet(dataSet, score));
////		}
////		return ((LsidDataSet[]) dataSets.toArray(new LsidDataSet[dataSets.size()]));
////	}
//	
//	private class TaxonNameBucket implements Comparable {
//		private String downloadNamePart = null;
//		private String downloadNamePartRank = GENUS_ATTRIBUTE;
//		private String nameString;
//		private Annotation taxonName;
//		private ArrayList taxonNames = new ArrayList();
//		private LsidDataSet dataSet = null;
//		private ArrayList dataSets = new ArrayList();
//		private String pageNumber;
//		private boolean isNewTaxon = false;
//		TaxonNameBucket(Annotation taxonName, String pageNumber) {
//			this.nameString = getNameString(taxonName);
//			if (this.nameString.length() == 0)
//				this.nameString = taxonName.getValue();
//			this.taxonName = taxonName;
//			this.taxonNames.add(taxonName);
//			this.pageNumber = pageNumber;
//		}
//		public int compareTo(Object o) {
//			if (o instanceof TaxonNameBucket) return this.nameString.compareTo(((TaxonNameBucket) o).nameString);
//			else return -1;
//		}
//	}
//	
//	static class LsidDataSet implements Comparable {
//		final String taxonNameUseID;
//		final String taxonNameID;
//		final String taxonomicConceptID;
//		final String taxonName;
//		final String rank;
//		final int score;
//		LsidDataSet(StringTupel data, int score) {
//			this.taxonNameUseID = data.getValue(TaxonNameUseID);
//			this.taxonNameID = data.getValue(TaxonNameID);
//			this.taxonomicConceptID = data.getValue(TaxonomicConceptID);
//			this.taxonName = data.getValue(TaxonName);
//			this.rank = data.getValue(Rank, UNKNOWN);
//			this.score = score;
//		}
//		LsidDataSet(String taxonNameUseID, String taxonNameID, String taxonomicConceptID, String taxonName, String rank, int score) {
//			this.taxonNameUseID = taxonNameUseID;
//			this.taxonNameID = taxonNameID;
//			this.taxonomicConceptID = taxonomicConceptID;
//			this.taxonName = taxonName;
//			this.rank = rank;
//			this.score = score;
//		}
//		public int compareTo(Object o) {
//			if (o == null) return -1;
//			if (o instanceof LsidDataSet) {
//				return (((LsidDataSet) o).score - this.score);
//			} else return -1;
//		}
//		public String toString() {
//			return (this.taxonNameUseID + ": " + this.taxonName);
//		}
//	}
//	
//	//	special dummy LSID data sets to indicate special actions
//	private static final LsidDataSet REMOVE = new LsidDataSet("", "", "", "", "", 0); 
//	private static final LsidDataSet IGNORE = new LsidDataSet("", "", "", "","", 0); 
//	private static final LsidDataSet CANCEL_UPLOADING = new LsidDataSet("", "", "", "", "", 0); 
//	
//	private class TaxonBucketTray extends JPanel {
//		private TaxonNameBucket bucket;
//		private JLabel label = new JLabel("", JLabel.LEFT);
//		
//		private LsidDataSet[] choices;
//		private JComboBox selector;
//		
//		private String lsid = null;
//		
//		private String downloadNamePart = null;
//		
//		private String name = null;
//		private Properties nameParts = new Properties();
//		
//		private StringVector downloadedThisDocument;
//		private StatusDialog sd;
//		
//		TaxonBucketTray(final TaxonNameBucket bucket, final String[] genera, StringVector downloadedThisDocument, StatusDialog sd, final TokenSequence text) {
//			super(new BorderLayout(), true);
//			this.bucket = bucket;
//			this.downloadedThisDocument = downloadedThisDocument;
//			this.sd = sd;
//			this.choices = (bucket.dataSets.isEmpty() ? null : ((LsidDataSet[]) bucket.dataSets.toArray(new LsidDataSet[bucket.dataSets.size()])));
//			
//			for (int p = 0; p < PART_NAMES.length; p++) {
//				String attribute = PART_NAMES[p];
//				String value = this.bucket.taxonName.getAttribute(attribute, "").toString();
//				if (value.length() != 0) this.nameParts.setProperty(attribute, value);
//			}
//			
//			this.setBorder(BorderFactory.createEtchedBorder());
//			
//			String nameString = getNameString(bucket.taxonName);
//			if (bucket.nameString.equals(nameString)) label.setText(
//						"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//						"<B>" + bucket.nameString + "</B></HTML>"
//					);
//			else label.setText(
//						"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//						"<B>" + bucket.nameString + "</B><BR>" +
//						"(change to <B>" + nameString + "</B>)</HTML>"
//					);
//			this.label.addMouseListener(new MouseAdapter() {
//				public void mouseClicked(MouseEvent me) {
//					StringVector docNames = new StringVector();
//					for (int a = 0; a < bucket.taxonNames.size(); a++) {
//						Annotation taxonName = ((Annotation) bucket.taxonNames.get(a));
//						Object pageNumber = taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE);
//						docNames.addElementIgnoreDuplicates(buildLabel(text, taxonName, 10) + " (at " + taxonName.getStartIndex() + ((pageNumber == null) ? ", unknown page" : (" on page " + pageNumber)) + ")");
//					}
//					String message = ("<HTML>This taxonomic name appears in the document in the following positions:<BR>&nbsp;&nbsp;&nbsp;");
//					message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
//					message += "</HTML>";
//					JOptionPane.showMessageDialog(TaxonBucketTray.this, message, ("Forms Of \"" + bucket.nameString + "\""), JOptionPane.INFORMATION_MESSAGE);
//				}
//			});
//			this.add(this.label, BorderLayout.NORTH);
//			
//			//	unknown above genus rank name
//			if (!GENUS_ATTRIBUTE.equals(this.bucket.downloadNamePartRank)) {
//				
//				final String[] options = new String[5];
//				options[0] = ENTER_LSID_CHOICE;
//				options[1] = (CHANGE_NAME_CHOICE + this.bucket.downloadNamePart);
//				options[2] = IGNORE_TAXON_CHOICE;
//				options[3] = REMOVE_TAXON_CHOICE;
//				options[4] = UPLOAD_NEW_TAXON_CHOICE;
//				this.selector = new JComboBox(options);
//				this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.selector.setEditable(false);
//				this.selector.setSelectedIndex(1);
//				this.selector.addMouseListener(new MouseAdapter() {
//					public void mouseClicked(MouseEvent me) {
//						if (me.getClickCount() > 1) {
//							
//							//	manual LSID input
//							if (selector.getSelectedIndex() == 0) {
//								Object newLsid = JOptionPane.showInputDialog(null, ("Please enter the LSID for " + bucket.downloadNamePart), "Enter LSID", JOptionPane.QUESTION_MESSAGE, null, null, ((lsid == null) ? "" : lsid));
//								if (newLsid != null) {
//									lsid = newLsid.toString().trim();
//									if (lsid.length() == 0) {
//										lsid = null;
//										options[0] = ENTER_LSID_CHOICE;
//									} else {
//										options[0] = (lsid + CHANGE_LSID_CHOICE);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(0);
//									selector.validate();
//								}
//							} else if (selector.getSelectedIndex() == 1) {
//								Object newName = JOptionPane.showInputDialog(null, ("Please select or enter the correct form of " + bucket.downloadNamePart), "Correct Name", JOptionPane.QUESTION_MESSAGE, null, null, ((downloadNamePart == null) ? bucket.downloadNamePart : downloadNamePart));
//								if (newName != null) {
//									downloadNamePart = newName.toString().trim();
//									if (downloadNamePart.length() == 0) {
//										downloadNamePart = null;
//										options[1] = (CHANGE_NAME_CHOICE + bucket.downloadNamePart);
//									} else {
//										options[1] = (CHANGE_NAME_CHOICE + downloadNamePart);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(1);
//									selector.validate();
//								}
//							}
//						}
//					}
//				});
//				
//			//	unknown name, maybe new taxon, or false hit
//			} else if (this.choices == null) {
//				
//				final String[] options = new String[6];
//				options[0] = ENTER_LSID_CHOICE;
//				options[1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
//				options[2] = (CHANGE_NAME_CHOICE + nameString);
//				options[3] = IGNORE_TAXON_CHOICE;
//				options[4] = REMOVE_TAXON_CHOICE;
//				options[5] = UPLOAD_NEW_TAXON_CHOICE;
//				this.selector = new JComboBox(options);
//				this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.selector.setEditable(false);
//				this.selector.setSelectedItem(sd.online.isSelected() ? UPLOAD_NEW_TAXON_CHOICE : IGNORE_TAXON_CHOICE);
//				this.selector.addMouseListener(new MouseAdapter() {
//					public void mouseClicked(MouseEvent me) {
//						if (me.getClickCount() > 1) {
//							
//							//	manual LSID input
//							if (selector.getSelectedIndex() == 0) {
//								Object newLsid = JOptionPane.showInputDialog(null, ("Please enter the LSID for " + bucket.nameString), "Enter LSID", JOptionPane.QUESTION_MESSAGE, null, null, ((lsid == null) ? "" : lsid));
//								if (newLsid != null) {
//									lsid = newLsid.toString().trim();
//									if (lsid.length() == 0) {
//										lsid = null;
//										options[0] = ENTER_LSID_CHOICE;
//									} else {
//										options[0] = (lsid + CHANGE_LSID_CHOICE);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(0);
//									selector.validate();
//								}
//							} else if (selector.getSelectedIndex() == 1) {
//								Object newGenus = JOptionPane.showInputDialog(null, ("Please select or enter the new genus for " + bucket.nameString), "Enter Genus", JOptionPane.QUESTION_MESSAGE, null, genera, null);
//								if (newGenus != null) {
//									downloadNamePart = newGenus.toString().trim();
//									if (downloadNamePart.length() == 0) {
//										downloadNamePart = null;
//										options[1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
//									} else {
//										options[1] = (CHANGE_GENUS_CHOICE + downloadNamePart);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(1);
//									selector.validate();
//								}
//							} else if (selector.getSelectedIndex() == 2) {
//								String newName = changeName();
//								if (newName != null) {
//									name = newName.trim();
//									if (name.length() == 0) {
//										name = null;
//										options[2] = CHANGE_NAME_CHOICE;
//										label.setText(
//												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//												"<B>" + bucket.nameString + "</B></HTML>"
//											);
//									} else {
//										options[2] = (CHANGE_NAME_CHOICE + name);
//										if (bucket.nameString.equals(name)) label.setText(
//												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//												"<B>" + bucket.nameString + "</B></HTML>"
//											);
//										else label.setText(
//												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//												"<B>" + bucket.nameString + "</B><BR>" +
//												"(change to <B>" + name + "</B>)</HTML>"
//											);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(2);
//									selector.validate();
//									label.validate();
//								}
//							}
//						}
//					}
//				});
//				
//			//	ambiguous LSID
//			} else {
//				
//				final String[] options = new String[this.choices.length + 6];
//				for (int o = 0; o < this.choices.length; o++) options[o] = this.choices[o].toString();
//				options[this.choices.length] = ENTER_LSID_CHOICE;
//				options[this.choices.length + 1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
//				options[this.choices.length + 2] = (CHANGE_NAME_CHOICE + nameString);
//				options[this.choices.length + 3] = IGNORE_TAXON_CHOICE;
//				options[this.choices.length + 4] = REMOVE_TAXON_CHOICE;
//				options[this.choices.length + 5] = UPLOAD_NEW_TAXON_CHOICE;
//				this.selector = new JComboBox(options);
//				this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.selector.setEditable(false);
//				this.selector.addMouseListener(new MouseAdapter() {
//					public void mouseClicked(MouseEvent me) {
//						if (me.getClickCount() > 1) {
//							if (selector.getSelectedIndex() == choices.length) {
//								Object newLsid = JOptionPane.showInputDialog(null, ("Please enter the LSID for " + bucket.nameString), "Enter LSID", JOptionPane.QUESTION_MESSAGE, null, null, ((lsid == null) ? "" : lsid));
//								if (newLsid != null) {
//									lsid = newLsid.toString().trim();
//									if (lsid.length() == 0) {
//										lsid = null;
//										options[choices.length] = ENTER_LSID_CHOICE;
//									} else {
//										options[choices.length] = (lsid + CHANGE_LSID_CHOICE);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(choices.length);
//									selector.validate();
//								}
//							} else if (selector.getSelectedIndex() == (choices.length + 1)) {
//								Object newGenus = JOptionPane.showInputDialog(null, ("Please select or enter the new genus for " + bucket.nameString), "Enter Genus", JOptionPane.QUESTION_MESSAGE, null, genera, null);
//								if (newGenus != null) {
//									downloadNamePart = newGenus.toString().trim();
//									if (downloadNamePart.length() == 0) {
//										downloadNamePart = null;
//										options[choices.length + 1] = (CHANGE_GENUS_CHOICE + bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, "Unknown Genus"));
//									} else {
//										options[choices.length + 1] = (CHANGE_GENUS_CHOICE + downloadNamePart);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(choices.length + 1);
//									selector.validate();
//								}
//							} else if (selector.getSelectedIndex() == (choices.length + 2)) {
//								String newName = changeName();
//								if (newName != null) {
//									name = newName.trim();
//									if (name.length() == 0) {
//										name = null;
//										options[choices.length + 2] = CHANGE_NAME_CHOICE;
//										label.setText(
//												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//												"<B>" + bucket.nameString + "</B></HTML>"
//											);
//									} else {
//										options[choices.length + 2] = (CHANGE_NAME_CHOICE + name);
//										if (bucket.nameString.equals(name)) label.setText(
//												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//												"<B>" + bucket.nameString + "</B></HTML>"
//											);
//										else label.setText(
//												"<HTML>The LSID for this name does not exist or is ambiguous:<BR>" +
//												"<B>" + bucket.nameString + "</B><BR>" +
//												"(change to <B>" + name + "</B>)</HTML>"
//											);
//									}
//									selector.setModel(new DefaultComboBoxModel(options));
//									selector.setSelectedIndex(choices.length + 2);
//									selector.validate();
//									label.validate();
//								}
//							}
//						}
//					}
//				});
//			}
//			this.add(this.selector, BorderLayout.CENTER);
//		}
//		
//		void commitChange() {
//			Object selected = this.selector.getSelectedItem();
//			if (UPLOAD_NEW_TAXON_CHOICE.equals(selected)) {
//				uploadName(this.bucket, this.downloadedThisDocument, sd);
//				
//			} else if (selected.toString().startsWith(CHANGE_GENUS_CHOICE)) {
//				if (this.downloadNamePart != null)
//					for (int t = 0; t < this.bucket.taxonNames.size(); t++)
//						((Annotation) this.bucket.taxonNames.get(t)).setAttribute(GENUS_ATTRIBUTE, this.downloadNamePart);
//				
//			} else if (selected.toString().startsWith(CHANGE_NAME_CHOICE)) {
//				if (GENUS_ATTRIBUTE.equals(this.bucket.downloadNamePartRank)) {
//					if (this.name != null) {
//						for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
//							Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
//							for (int p = 0; p < PART_NAMES.length; p++) {
//								String attribute = PART_NAMES[p];
//								String value = this.nameParts.getProperty(attribute);
//								if (value == null) taxonName.removeAttribute(attribute);
//								else taxonName.setAttribute(attribute, value);
//							}
//						}
//					}
//				} else if (this.downloadNamePart != null) {
//					for (int t = 0; t < this.bucket.taxonNames.size(); t++)
//						((Annotation) this.bucket.taxonNames.get(t)).setAttribute(this.bucket.downloadNamePartRank, this.downloadNamePart);
//				}
//				
//			} else if (IGNORE_TAXON_CHOICE.equals(selected)) {
//				this.bucket.dataSet = IGNORE;
//				
//			} else if (REMOVE_TAXON_CHOICE.equals(selected)) {
//				this.bucket.dataSet = REMOVE;
//				
//			} else if ((this.choices == null) || (this.selector.getSelectedIndex() == this.choices.length)) {
//				if (this.lsid != null)
//					for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
//						Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
//						taxonName.setAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), (LSID_ATTRIBUTE_PREFIX + this.lsid));
//						taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), ("Manual - " + this.bucket.nameString));
//					}
//				
//			} else this.bucket.dataSet = this.choices[this.selector.getSelectedIndex()];
//		}
//		
//		private String changeName() {
//			
//			//	assemble dialog
//			final JDialog td = new JDialog(((JFrame) null), "Correct Name", true);
//			JPanel panel = new JPanel(new GridBagLayout());
//			td.getContentPane().setLayout(new BorderLayout());
//			td.getContentPane().add(panel, BorderLayout.CENTER);
//			
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 2;
//			gbc.insets.bottom = 2;
//			gbc.insets.left = 5;
//			gbc.insets.right = 5;
//			gbc.fill = GridBagConstraints.HORIZONTAL;
//			gbc.weightx = 1;
//			gbc.weighty = 0;
//			gbc.gridwidth = 1;
//			gbc.gridheight = 1;
//			gbc.gridx = 0;
//			gbc.gridy = 0;
//			
//			gbc.gridwidth = 2;
//			gbc.weightx = 2;
//			panel.add(new JLabel("<HTML>Please enter the correct form of <B>" + bucket.nameString + "</B></HTML>"), gbc.clone());
//			gbc.gridy++;
//			gbc.gridwidth = 1;
//			
//			final HashMap fieldsByName = new HashMap();
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
//			
//			JButton okButton = new JButton("OK");
//			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			okButton.setPreferredSize(new Dimension(100, 21));
//			okButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					td.dispose();
//				}
//			});
//			JButton cancelButton = new JButton("Cancel");
//			cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			cancelButton.setPreferredSize(new Dimension(100, 21));
//			cancelButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					fieldsByName.clear();
//					td.dispose();
//				}
//			});
//			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//			buttonPanel.add(okButton);
//			buttonPanel.add(cancelButton);
//			td.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//			
//			//	ensure dialog is closed with button
//			td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
//			
//			//	get feedback
//			td.setSize(500, ((25 * gbc.gridy) + 50));
//			td.setLocationRelativeTo(this);
//			td.setVisible(true);
//			
//			if (fieldsByName.isEmpty()) return null;
//			else for (int p = 0; p < PART_NAMES.length; p++) {
//				String attribute = PART_NAMES[p];
//				JTextField field = ((JTextField) fieldsByName.get(attribute));
//				String value = field.getText().trim();
//				if (value.length() == 0) this.nameParts.remove(attribute);
//				else this.nameParts.setProperty(attribute, value);
//			}
//			
//			String nameString = "";
//			if (this.nameParts.containsKey(VARIETY_ATTRIBUTE)) nameString = ("var. " + this.nameParts.getProperty(VARIETY_ATTRIBUTE) + " " + nameString);
//			if (this.nameParts.containsKey(SUBSPECIES_ATTRIBUTE)) nameString = ("subsp. " + this.nameParts.getProperty(SUBSPECIES_ATTRIBUTE) + " " + nameString);
//			if (this.nameParts.containsKey(SPECIES_ATTRIBUTE)) nameString = (this.nameParts.getProperty(SPECIES_ATTRIBUTE) + " " + nameString);
//			if (this.nameParts.containsKey(SUBGENUS_ATTRIBUTE)) nameString = ("(" + this.nameParts.getProperty(SUBGENUS_ATTRIBUTE) + ") " + nameString);
//			if (this.nameParts.containsKey(GENUS_ATTRIBUTE)) nameString = (this.nameParts.getProperty(GENUS_ATTRIBUTE) + " " + nameString);
//			return nameString.trim();
//		}
//	}
//	
//	
//	private String buildLabel(TokenSequence text, Annotation annot, int envSize) {
//		int aStart = annot.getStartIndex();
//		int aEnd = annot.getEndIndex();
//		int start = Math.max(0, (aStart - envSize));
//		int end = Math.min(text.size(), (aEnd + envSize));
//		StringBuffer sb = new StringBuffer("... ");
//		Token lastToken = null;
//		Token token = null;
//		for (int t = start; t < end; t++) {
//			lastToken = token;
//			token = text.tokenAt(t);
//			
//			//	end highlighting value
//			if (t == aEnd) sb.append("</B>");
//			
//			//	add spacer
//			if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
//			
//			//	start highlighting value
//			if (t == aStart) sb.append("<B>");
//			
//			//	append token
//			sb.append(token);
//		}
//		
//		return sb.append(" ...").toString();
//	}
//	
//	private HashMap cache = new HashMap();
//	private StringRelation getLsidData(String downloadNamePart, boolean allowCache, boolean allowFile, StringVector downloadedThisDocument, StatusDialog sd, boolean online) {
//		
////		//	TEST ONLY (for feedback dialog)
////		if (true) return null;
//		
//		//	do cache lookup
//		StringRelation lsidData = ((allowCache || downloadedThisDocument.containsIgnoreCase(downloadNamePart))? ((StringRelation) this.cache.get(downloadNamePart.toLowerCase())) : null);
//		
//		//	cache hit
//		if (lsidData != null) {
//			if (sd != null) sd.setLabel(" - Cache Hit");
//			return lsidData;
//		} else if (allowCache && (sd != null)) sd.setLabel(" - Cache Miss");
//		
//		//	do file lookup
//		if (allowFile || downloadedThisDocument.containsIgnoreCase(downloadNamePart)) try {
//			InputStream is = this.dataProvider.getInputStream("LSID_" + downloadNamePart + ".csv");
//			lsidData = StringRelation.readCsvData(new InputStreamReader(is), '"', true, null);
//			is.close();
//			this.cache.put(downloadNamePart.toLowerCase(), lsidData);
//			if (sd != null) sd.setLabel(" - File Hit");
//			return lsidData;
//		} catch (Exception e) {
//			if (allowFile && (sd != null)) sd.setLabel(" - File Miss");
//		}
//		
//		//	check if HNS lookup allowed
//		if (((sd == null) && !online) || ((sd != null) && !sd.online.isSelected())) return new StringRelation();
//		
//		//	download data
//		try {
//			if (sd != null) sd.setLabel(" - Loading data from HNS ...");
//			
//			if (downloadedThisDocument.containsIgnoreCase(downloadNamePart)) {
//				lsidData = new StringRelation();
//			} else {
//				lsidData = this.loadLsidData(downloadNamePart);
//				downloadedThisDocument.addElementIgnoreDuplicates(downloadNamePart);
//			}
//			if (lsidData.isEmpty()) return lsidData;
//			
//			if (this.dataProvider.isDataEditable()) {
//				Writer w = new OutputStreamWriter(this.dataProvider.getOutputStream("LSID_" + downloadNamePart + ".csv"));
//				StringRelation.writeCsvData(w, lsidData, '"', this.keys);
//				w.flush();
//				w.close();
//			}
//			
//			if (sd != null) sd.setLabel("   --> Got HNS Data");
//			
//			this.cache.put(downloadNamePart.toLowerCase(), lsidData);
//			return lsidData;
//		} catch (Exception e) {
//			if (sd != null) sd.setLabel("   --> HNS Lookup Error: " + e.getMessage());
//			return null;
//		} catch (Throwable t) {
//			if (sd != null) sd.setLabel("   --> HNS Lookup Error: " + t.getMessage());
//			t.printStackTrace();
//			return null;
//		}
//
//	}
//	
//	private StringRelation loadLsidData(String downloadNamePart) throws Exception {
//		System.out.println("TnuluLsidReferencer: doing lookup for '" + downloadNamePart + "'");
//		StringRelation lsidData = new StringRelation();
//		
//		if (StringUtils.isUpperCaseWord(downloadNamePart)) {
//			downloadNamePart = StringUtils.capitalize(downloadNamePart);
//			System.out.println("  converted to '" + downloadNamePart + "'");
//		}
//		
//		TreeNode root = IoTools.getAndParsePage(this.tnuluRequestBaseUrl + URLEncoder.encode((downloadNamePart + "%"), "UTF-8"));
//		TreeNode[] tableRows = TreeTools.getAllNodesOfType(root, "tr");
//		
//		for (int t = 1; t < tableRows.length; t++) {
//			TreeNode[] tableCells = TreeTools.getAllNodesOfType(tableRows[t], "td");
//			StringTupel rowData = new StringTupel();
//			for (int c = 0; (c < tableCells.length) && (c < AttributeNames.length); c++) {
//				TreeNode dataNode = tableCells[c].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				rowData.setValue(AttributeNames[c], ((dataNode == null) ? UNKNOWN : IoTools.prepareForPlainText(dataNode.getNodeValue())));
//			}
//			lsidData.addElement(rowData);
//		}
//		return lsidData;
//	}
//	
//	private class StatusDialog extends JDialog implements Runnable {
//		JLabel label = new JLabel("", JLabel.CENTER);
//		StringVector labelLines = new StringVector();
//		JCheckBox online = new JCheckBox("Allow HNS Lookup", true);
//		boolean interrupted = false;
//		Thread thread = null;
//		StatusDialog() {
//			super(((JFrame) null), "LSID Referencer", true);
//			this.getContentPane().setLayout(new BorderLayout());
//			this.getContentPane().add(this.label, BorderLayout.CENTER);
//			
//			JButton stopButton = new JButton("Stop LSID Referencing");
//			stopButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			stopButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (JOptionPane.showConfirmDialog(StatusDialog.this, "Do you really want to stop the LSID referencer?", "Confirm Stop LSID Referencer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
//						interrupted = true;
//				}
//			});
//			JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//			controlPanel.add(this.online);
//			controlPanel.add(stopButton);
//			this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
//			
//			this.setSize(400, 120);
//			this.setLocationRelativeTo(null);
//			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//			
//			this.addWindowListener(new WindowAdapter() {
//				boolean isInvoked = false;
//				public void windowClosing(WindowEvent we) {
//					if (this.isInvoked) return; // avoid loops
//					this.isInvoked = true;
//					if (JOptionPane.showConfirmDialog(StatusDialog.this, "Closing this status dialog will disable you to monitor LsdiReferencer", "Confirm Close Status Dialog", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
//						dispose();
//					this.isInvoked = false;
//				}
//			});
//		}
//		
//		void setLabel(String text) {
////			System.out.println("TnuluLSID: " + text);
//			this.labelLines.addElement(text);
//			while (this.labelLines.size() > 3)
//				this.labelLines.remove(0);
//			this.label.setText("<HTML>" + this.labelLines.concatStrings("<BR>") + "</HTML>");
//			this.label.validate();
//		}
//		
//		void popUp() {
//			if (this.thread == null) {
//				this.thread = new Thread(this);
//				this.thread.start();
//				while (!this.isVisible()) try {
//					Thread.sleep(50);
//				} catch (InterruptedException ie) {}
//			}
//		}
//		
//		public void run() {
//			this.setVisible(true);
//			this.thread = null;
//		}
//	}
//	
//	//	return false only to indicate an interrupt of the upload session, i.e. do not upload further names 
//	private void uploadName(TaxonNameBucket bucket, StringVector downloadedThisDocument, StatusDialog sd) {
//		
//		//	check if upload possible
//		if (sd == null) return;
//		if ((this.tnuluNewUploadBaseUrl == null) || (this.tnuluKnownUploadBaseUrl == null)) {
//			JOptionPane.showMessageDialog(sd, "Taxon name upload is not possible, please check configuration.", "Taxon Name Upload Not Possible", JOptionPane.ERROR_MESSAGE);
//			return;
//		}
//		
//		//	upload name(s) in tray
//		TaxonNameUploadDialog tnud = new TaxonNameUploadDialog(sd, bucket, downloadedThisDocument);
//		tnud.setVisible(true);
//		
//		//	upload dialog or entire upload session cancelled
//		if (tnud.bucket == null) return;
//		
//		//	upload failed
//		if (bucket.dataSet == null) {
//			
//			//	attach failure indication attributes to annotations
//			for (int t = 0; t < bucket.taxonNames.size(); t++) {
//				Annotation taxonName = ((Annotation) bucket.taxonNames.get(t));
//				taxonName.setAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), "Upload Failed");
//				taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), "Not available");
//			}
//			
//		//	upload successful
//		} else {
//			
//			//	attach attributes to annotations
//			for (int t = 0; t < bucket.taxonNames.size(); t++) {
//				Annotation taxonName = ((Annotation) bucket.taxonNames.get(t));
//				taxonName.setAttribute((LSID_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), (LSID_ATTRIBUTE_PREFIX + bucket.dataSet.taxonNameUseID));
//				taxonName.setAttribute((LSID_NAME_ATTRIBUTE + "-" + LSID_PROVIDER_SUFFIX), bucket.dataSet.taxonName);
//			}
//			
//			//	refresh cache file (if exists)
//			if (bucket.downloadNamePart != null) try {
//				
//				StringRelation genusData = ((StringRelation) this.cache.get(bucket.downloadNamePart.toLowerCase()));
//				if (genusData == null) {
//					try {
//						InputStream is = this.dataProvider.getInputStream("LSID_" + bucket.downloadNamePart + ".csv");
//						genusData = StringRelation.readCsvData(new InputStreamReader(is), '"', true, null);
//						is.close();
//					} catch (FileNotFoundException fnfe) {
//						genusData = new StringRelation();
//					}
//					this.cache.put(bucket.downloadNamePart.toLowerCase(), genusData);
//				}
//				
//				genusData.addElement(tnud.newTaxonData);
//				
//				Writer w = new OutputStreamWriter(this.dataProvider.getOutputStream("LSID_" + bucket.downloadNamePart + ".csv"));
//				StringRelation.writeCsvData(w, genusData, '"', this.keys);
//				w.flush();
//				w.close();
//				
//			} catch (IOException ioe) {
//				ioe.printStackTrace(System.out);
//			}
//		}
//	}
//	
//	private static final String CONCEPT_RANK_OPTIONS_FILE_NAME = "conceptRankOptions.txt";
//	private String[] conceptRankOptions = new String[0];
//	private static final String[] DEFAULT_CONCEPT_RANK_OPTIONS = {"Family", "Subfamily", "Tribe", "Genus", "Subgenus", "Species", "Subspecies", "Variety"};
//	
//	private static final String CONCEPT_STATUS_OPTIONS_FILE_NAME = "conceptStatusOptions.txt";
//	private String[] conceptStatusOptions = new String[0];
//	private static final String[] DEFAULT_CONCEPT_STATUS_OPTIONS = {
//			"Common name", 
//			"Dubious", 
//			"Emendation", 
//			"Error", 
//			"Homonym & junior synonym", 
//			"Homonym & synonym", 
//			"Incorrect original spelling", 
//			"Invalid", 
//			"Invalid combination", 
//			"Invalid emendation", 
//			"Invalid name", 
//			"Invalid replacement", 
//			"Junior homonym", 
//			"Junior homonym & synonym", 
//			"Junior synonym", 
//			"Justified emendation", 
//			"Literature misspelling", 
//			"Misspelling", 
//			"Nomen nudum", 
//			"Original name", 
//			"Original name/combination", 
//			"Other", 
//			"Replacement name", 
//			"Subsequent name/combination", 
//			"Syn-subsequent combination", 
//			"Temp. Invalid", 
//			"Suppressed by ruling", 
//			"Unavailable", 
//			"Uncertain", 
//			"Unjustified emendation", 
//			"Unnecessary replacement name", 
//			"Valid"
//		};
//	
//	private static final String COUNTRY_OPTIONS_FILE_NAME = "countryOptions.txt";
//	private static final String[] DEFAULT_COUNTRY_OPTIONS = {"<Enter Country>"};
//	private String[] countryOptions = DEFAULT_COUNTRY_OPTIONS;
//	
//	private static final String RELATION_TYPE_OPTIONS_FILE_NAME = "relationTypeOptions.txt";
//	private static final String[] DEFAULT_RELATION_TYPE_OPTIONS = {"Junior synonym", "Member", "Synonym"};
//	private String[] relationTypeOptions = DEFAULT_RELATION_TYPE_OPTIONS;
//	
//	private static final String REMARK_PRESET_OPTIONS_FILE_NAME = "remarkPresetOptions.txt";
//	private static final String DEFAULT_REMARK_OPTION = "<Enter or Select Remark>";
//	private String[] remarkPresetOptions = {DEFAULT_REMARK_OPTION};
//	
//	private static final Parser parser = new Parser(new Html()); 
//	
//	private class TaxonNameUploadDialog extends JDialog {
//		
//		private JTextField conceptName = new JTextField();
//		private JComboBox conceptRank = new JComboBox(conceptRankOptions);
//		private String rank = "";
//		
//		private JTextField authority = new JTextField();
//		private JComboBox country = new JComboBox(countryOptions);
//		
//		private JComboBox parentLsid = new JComboBox();
//		private JComboBox relationType = new JComboBox(relationTypeOptions);
//		
//		private JComboBox conceptStatus = new JComboBox(conceptStatusOptions);
//		private JCheckBox validFlag = new JCheckBox("Valid ?");
//		
//		private JTextField referencePublicationModsID = new JTextField();
//		private JTextField referencePublicationPageNumber = new JTextField();
//		private JComboBox remark = new JComboBox(remarkPresetOptions);
//		
//		private JCheckBox isNewTaxon = new JCheckBox("New Taxon"); 
//		private JButton uploadButton = new JButton("Upload Taxon");
//		private JButton cancelButton = new JButton("Cancel Taxon");
//		private JButton cancelAllButton = new JButton("Cancel All");
//		
//		private TaxonNameBucket bucket;
//		private StringTupel newTaxonData = null;
//		
//		public TaxonNameUploadDialog(StatusDialog sd, TaxonNameBucket tnb, StringVector downloadedThisDocument) {
//			super(sd, ("Upload Taxon Name '" + tnb.taxonName.getValue() + "' to HNS"), true);
//			
//			this.bucket = tnb;
//			
//			this.conceptName.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.conceptName.setPreferredSize(new Dimension(200, 21));
//			
//			this.uploadButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			this.uploadButton.setPreferredSize(new Dimension(50, 21));
//			this.uploadButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					uploadTaxonName();
//				}
//			});
//			
//			this.cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			this.cancelButton.setPreferredSize(new Dimension(50, 21));
//			this.cancelButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					bucket = null;
//					dispose();
//				}
//			});
//			
//			this.cancelAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
//			this.cancelAllButton.setPreferredSize(new Dimension(50, 21));
//			this.cancelAllButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					bucket.dataSet = CANCEL_UPLOADING;
//					bucket = null;
//					dispose();
//				}
//			});
//			
//			this.isNewTaxon.setSelected(this.bucket.isNewTaxon);
//			this.isNewTaxon.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					layoutInputFields();
//				}
//			});
//			
//			this.authority.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.authority.setPreferredSize(new Dimension(50, 21));
//			
//			this.country.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.country.setEditable(true);
//			
//			String lastPart = null;
//			String genus = null;
//			String parentNameString = "";
//			String parentNamePart = null;
//			String parentRank = null;
//			String attribute;
//			String nameString = "";
//			
//			this.rank = this.bucket.taxonName.getAttribute(RANK_ATTRIBUTE, "").toString();
//			attribute = this.bucket.taxonName.getAttribute(GENUS_ATTRIBUTE, MISSING_PART).toString();
//			if (!MISSING_PART.equals(attribute)) {
//				nameString += attribute;
//				lastPart = attribute;
//				parentNamePart = attribute;
//				genus = attribute;
//				this.rank = GENUS_ATTRIBUTE;
//			}
//			
//			attribute = this.bucket.taxonName.getAttribute(SUBGENUS_ATTRIBUTE, MISSING_PART).toString();
//			if (!MISSING_PART.equals(attribute)) {
//				nameString += (" (" + attribute + ")");
//				lastPart = attribute;
//				if (parentNamePart != null) parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
//				parentNamePart = attribute;
//				parentRank = this.rank;
//				this.rank = SUBGENUS_ATTRIBUTE;
//			}
//			
//			attribute = this.bucket.taxonName.getAttribute(SPECIES_ATTRIBUTE, MISSING_PART).toString();
//			if (!MISSING_PART.equals(attribute)) {
//				nameString += (" " + attribute);
//				lastPart = attribute;
//				if (parentNamePart != null) parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
//				parentNamePart = attribute;
//				parentRank = this.rank;
//				this.rank = SPECIES_ATTRIBUTE;
//			}
//			
//			attribute = this.bucket.taxonName.getAttribute(SUBSPECIES_ATTRIBUTE, MISSING_PART).toString();
//			if (!MISSING_PART.equals(attribute)) {
//				nameString += (" subsp. " + attribute);
//				lastPart = attribute;
//				if (parentNamePart != null) parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
//				parentNamePart = attribute;
//				parentRank = this.rank;
//				this.rank = SUBSPECIES_ATTRIBUTE;
//			}
//			
//			attribute = this.bucket.taxonName.getAttribute(VARIETY_ATTRIBUTE, MISSING_PART).toString();
//			if (!MISSING_PART.equals(attribute)) {
//				nameString += (" var. " + attribute);
//				lastPart = attribute;
//				if (parentNamePart != null) parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
//				parentNamePart = attribute;
//				parentRank = this.rank;
//				this.rank = VARIETY_ATTRIBUTE;
//			}
//			
//			//	no attributes given
//			if (lastPart == null) {
//				this.conceptName.setText(this.bucket.taxonName.getValue());
//				
//			//	use name built from attributes
//			} else this.conceptName.setText(nameString);
//			
//			//	determine name authority
//			String authority = this.bucket.taxonName.getDocumentProperty("ModsDocAuthor");
//			
//			//	document author not set, parse authority from annotation value
//			if (authority == null) {
//				
//				authority = this.bucket.taxonName.getValue();
//				int split = (((lastPart == null) || authority.endsWith(lastPart)) ? -1 : authority.lastIndexOf(lastPart));
//				
//				//	no name given
//				if (split == -1) this.referencePublicationModsID.setText("<Enter Authority>");
//					
//				//	parse authority from annotation value
//				else {
//					split += lastPart.length();
//					this.authority.setText(authority.substring(split).trim());
//				}
//				
//			//	use docuement author
//			} else this.referencePublicationModsID.setText(authority);
//			
//			this.conceptRank.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.conceptRank.setPreferredSize(new Dimension(this.conceptRank.getPreferredSize().width, 21));
//			this.conceptRank.setEditable(conceptRankOptions.length == 0);
//			if (this.bucket.taxonName.hasAttribute(RANK_ATTRIBUTE))
//				this.conceptRank.setSelectedItem(StringUtils.capitalize(this.bucket.taxonName.getAttribute(RANK_ATTRIBUTE).toString()));
//			else if (this.rank != null)
//				this.conceptRank.setSelectedItem(StringUtils.capitalize(rank));
//			
//			this.parentLsid.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.parentLsid.setPreferredSize(new Dimension(50, 21));
//			this.parentLsid.setEditable(true);
//			if (!GENUS_ATTRIBUTE.equals(rank) && (parentNameString.length() != 0) && (parentRank != null) && (genus != null)) {
//				StringRelation genusData = getLsidData(genus, true, true, downloadedThisDocument, sd, false);
//				if (genusData != null) {
//					ArrayList dataSets = new ArrayList();
//					TokenSequence parentTaxonNameTokens = tnb.taxonName.getTokenizer().tokenize(parentNameString);
//					Annotation parentTaxonName = Gamta.newAnnotation(parentTaxonNameTokens, TAXONOMIC_NAME_ANNOTATION_TYPE, 0, parentTaxonNameTokens.size());
//					parentTaxonName.copyAttributes(tnb.taxonName);
//					parentTaxonName.removeAttribute(this.rank);
//					parentTaxonName.setAttribute(RANK_ATTRIBUTE, parentRank);
//					
//					//	score data sets
//					for (int d = 0; d < genusData.size(); d++) {
//						StringTupel dataSet = genusData.get(d);
//						int score = score(dataSet, parentTaxonName, false);
//						if (score != 0) dataSets.add(new LsidDataSet(dataSet, score));
//					}
//					
//					//	use top data set
//					Collections.sort(dataSets);
//					if (!dataSets.isEmpty()) {
//						LsidDataSet top = ((LsidDataSet) dataSets.get(0));
//						int index = 1;
//						while (index < dataSets.size()) {
//							if (((LsidDataSet) dataSets.get(index)).score < top.score)
//								dataSets.remove(index);
//							else index++;
//						}
//						
//						for (int d = 0; d < dataSets.size(); d++)
//							this.parentLsid.addItem(dataSets.get(d));
//						this.parentLsid.setSelectedItem(top);
//					}
//				}
//			}
//			
//			this.relationType.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.relationType.setPreferredSize(new Dimension(this.relationType.getPreferredSize().width, 21));
//			this.relationType.setEditable(relationTypeOptions.length == 0);
//			if (defaultRelationTypeOption != null)
//				this.relationType.setSelectedItem(defaultRelationTypeOption);
//			else if (relationTypeOptions.length != 0)
//				this.relationType.setSelectedItem(relationTypeOptions[0]);
//			else this.relationType.setSelectedItem("");
//			
//			this.conceptStatus.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.conceptStatus.setPreferredSize(new Dimension(this.conceptStatus.getPreferredSize().width, 21));
//			this.conceptStatus.setEditable(conceptStatusOptions.length == 0);
//			if (defaultConceptStatusOption != null)
//				this.conceptStatus.setSelectedItem(defaultConceptStatusOption);
//			else if (conceptStatusOptions.length != 0)
//				this.conceptStatus.setSelectedItem(conceptStatusOptions[0]);
//			else this.conceptStatus.setSelectedItem("");
//			
//			this.validFlag.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.validFlag.setPreferredSize(new Dimension(50, 21));
//			
//			this.referencePublicationModsID.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.referencePublicationModsID.setText(this.bucket.taxonName.getDocumentProperty("ModsDocID", "<Enter MODS ID>"));
//			
//			this.referencePublicationPageNumber.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.referencePublicationPageNumber.setText((this.bucket.pageNumber.length() == 0) ? "<Enter page number>" : this.bucket.pageNumber);
//			
//			this.remark.setBorder(BorderFactory.createLoweredBevelBorder());
//			this.remark.setEditable(true);
//			this.remark.setSelectedItem(DEFAULT_REMARK_OPTION);
//			
//			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//			
//			this.getContentPane().setLayout(new GridBagLayout());
//			this.layoutInputFields();
//			this.setLocationRelativeTo(sd);
//		}
//		
//		private void layoutInputFields() {
//			this.getContentPane().removeAll();
//			if (this.isNewTaxon.isSelected())
//				this.layoutNewTaxon();
//			else this.layoutKnownTaxon();
//			this.validate();
//		}
//		
//		//	initialize for upload to HNS database
//		private void layoutNewTaxon() {
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 2;
//			gbc.insets.bottom = 2;
//			gbc.insets.left = 5;
//			gbc.insets.right = 5;
//			gbc.fill = GridBagConstraints.HORIZONTAL;
//			gbc.weightx = 0;
//			gbc.weighty = 0;
//			gbc.gridwidth = 1;
//			gbc.gridheight = 1;
//			gbc.gridx = 0;
//			gbc.gridy = 0;
//			
//			this.getContentPane().add(new JLabel("Concept Name / Rank", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.gridwidth = 2;
//			gbc.weightx = 2;
//			this.getContentPane().add(this.conceptName, gbc.clone());
//			gbc.gridx+=2;
//			gbc.gridwidth = 1;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.conceptRank, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			gbc.gridwidth = 1;
//			gbc.weightx = 0;
//			this.getContentPane().add(new JLabel("Authority / Country", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.authority, gbc.clone());
//			gbc.gridx++;
//			gbc.gridwidth = 2;
//			gbc.weightx = 2;
//			this.getContentPane().add(this.country, gbc.clone());
//			
//			
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			gbc.gridwidth = 1;
//			gbc.weightx = 0;
//			this.getContentPane().add(new JLabel("Parent LSID / Relation", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.gridwidth = 2;
//			gbc.weightx = 2;
//			this.getContentPane().add(this.parentLsid, gbc.clone());
//			gbc.gridx+=2;
//			gbc.gridwidth = 1;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.relationType, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			this.getContentPane().add(new JLabel("Concept Status", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.gridwidth = 2;
//			this.getContentPane().add(this.conceptStatus, gbc.clone());
//			gbc.gridx+=2;
//			gbc.gridwidth = 1;
//			this.getContentPane().add(this.validFlag, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			gbc.gridwidth = 1;
//			gbc.weightx = 0;
//			this.getContentPane().add(new JLabel("Pub. MODS ID", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.referencePublicationModsID, gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 0;
//			this.getContentPane().add(new JLabel("Page Number", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.referencePublicationPageNumber, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			this.getContentPane().add(new JLabel("Annotation / Remark", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.gridwidth = 3;
//			this.getContentPane().add(this.remark, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			gbc.gridwidth = 1;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.isNewTaxon, gbc.clone());
//			gbc.gridx++;
//			this.getContentPane().add(this.uploadButton, gbc.clone());
//			gbc.gridx++;
//			this.getContentPane().add(this.cancelButton, gbc.clone());
//			gbc.gridx++;
//			this.getContentPane().add(this.cancelAllButton, gbc.clone());
//			
//			this.setSize(500, 200);
//		}
//		
//		//	initialize for upload to HNS holding container
//		private void layoutKnownTaxon() {
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 2;
//			gbc.insets.bottom = 2;
//			gbc.insets.left = 5;
//			gbc.insets.right = 5;
//			gbc.fill = GridBagConstraints.HORIZONTAL;
//			gbc.weightx = 0;
//			gbc.weighty = 0;
//			gbc.gridwidth = 1;
//			gbc.gridheight = 1;
//			gbc.gridx = 0;
//			gbc.gridy = 0;
//			
//			this.getContentPane().add(new JLabel("Concept Name", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.gridwidth = 3;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.conceptName, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			gbc.gridwidth = 1;
//			gbc.weightx = 0;
//			this.getContentPane().add(new JLabel("Pub. MODS ID", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.referencePublicationModsID, gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 0;
//			this.getContentPane().add(new JLabel("Page Number", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.referencePublicationPageNumber, gbc.clone());
//			
//			gbc.gridy++;
//			gbc.gridx = 0;
//			gbc.gridwidth = 1;
//			gbc.weightx = 1;
//			this.getContentPane().add(this.isNewTaxon, gbc.clone());
//			gbc.gridx++;
//			this.getContentPane().add(this.uploadButton, gbc.clone());
//			gbc.gridx++;
//			this.getContentPane().add(this.cancelButton, gbc.clone());
//			gbc.gridx++;
//			this.getContentPane().add(this.cancelAllButton, gbc.clone());
//			
//			this.setSize(500, 100);
//		}
//		
//		private void uploadTaxonName() {
//			if (this.isNewTaxon.isSelected() ? this.uploadNewTaxon() : this.uploadKnownTaxon())
//				this.dispose();
//		}
//		
//		//	upload to HNS database
//		private boolean uploadNewTaxon() {
//			
//			//	gather data
//			String conceptName = this.conceptName.getText().trim();
//			String authority = this.authority.getText().trim();
//			
//			String conceptRank = "";
//			Object cro = this.conceptRank.getSelectedItem();
//			if (cro != null) conceptRank = cro.toString();
//			
//			String parentLsid = "";
//			Object plo = this.parentLsid.getSelectedItem();
//			if (plo instanceof LsidDataSet)
//				parentLsid = ((LsidDataSet) plo).taxonNameUseID;
//			else if (plo != null) parentLsid = plo.toString().trim();
//			
//			String relationType = "";
//			Object rto = this.relationType.getSelectedItem();
//			if (rto != null) relationType = rto.toString();
//			
//			String conceptStatus = this.conceptStatus.getSelectedItem().toString().trim();
//			String validFlag = (this.validFlag.isSelected() ? "Valid" : "Invalid");
//			
//			String refPubId = this.referencePublicationModsID.getText().trim();
//			if (refPubId.startsWith("<")) refPubId = "";
//			String refPubPage = this.referencePublicationPageNumber.getText().trim();
//			if (refPubPage.startsWith("<")) refPubPage = "";
//			String annotation = this.remark.getSelectedItem().toString().trim();
//			if (annotation.startsWith("<")) annotation = "";
//			
//			//	check data
//			if (conceptName.length() == 0) {
//				JOptionPane.showMessageDialog(this, "The specified Concept Name is invalid", "Invalid Concept Name", JOptionPane.ERROR_MESSAGE);
//				this.conceptName.requestFocusInWindow();
//				return false;
//			}
//			if (authority.length() == 0) {
//				JOptionPane.showMessageDialog(this, "The specified Authority is invalid", "Invalid Authority", JOptionPane.ERROR_MESSAGE);
//				this.authority.requestFocusInWindow();
//				return false;
//			}
////			if (!parentLsid.matches("[0-9]++")) {
////				JOptionPane.showMessageDialog(this, "The specified Parent LSID is invalid", "Invalid Parent LSID", JOptionPane.ERROR_MESSAGE);
////				this.parentLsid.requestFocusInWindow();
////				return false;
////			}
////			if (conceptStatus.length() == 0) {
////				JOptionPane.showMessageDialog(this, "The specified Concept Status is invalid", "Invalid Concept Status", JOptionPane.ERROR_MESSAGE);
////				this.conceptStatus.requestFocusInWindow();
////				return false;
////			}
//			if (!refPubId.matches("[0-9]++")) {
//				JOptionPane.showMessageDialog(this, "The specified MODS document ID is invalid", "Invalid MODS Document ID", JOptionPane.ERROR_MESSAGE);
//				this.referencePublicationModsID.requestFocusInWindow();
//				return false;
//			}
//			if (!refPubPage.matches("[0-9]++")) {
//				JOptionPane.showMessageDialog(this, "The specified page number is invalid", "Invalid Page Number", JOptionPane.ERROR_MESSAGE);
//				this.referencePublicationPageNumber.requestFocusInWindow();
//				return false;
//			}
//			if (!this.validFlag.isSelected()) {
//				if (JOptionPane.showConfirmDialog(this, "The taxonomic name to upload is marked as invalid. Proceed anyway?", "Taxonomic Name Marked Invalid", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
//					return false;
//			}
//			
//			//	do upload
//			try {
//				System.out.println("TnuluLsidReferencer: uploading '" + conceptName + "'");
//				
//				String uploadUrlString = tnuluNewUploadBaseUrl +
//					 "concept_name=" + URLEncoder.encode(conceptName, "UTF-8") +
//					((authority.length() == 0) ? "" : "&authority=" + URLEncoder.encode(authority, "UTF-8")) +
//					"&parent_lsid=" + parentLsid +
//					"&concept_rank=" + conceptRank +
//					"&concept_status=" + URLEncoder.encode(conceptStatus, "UTF-8") +
//					((relationType.length() == 0) ? "" : "&reln_type=" + URLEncoder.encode(relationType, "UTF-8")) +
//					"&v_flag=" + validFlag +
//					((refPubId.length() == 0) ? "" : "&ref_id=" + URLEncoder.encode(refPubId, "UTF-8")) +
//					((refPubPage.length() == 0) ? "" : "&pages=" + URLEncoder.encode(refPubPage, "UTF-8")) +
//					((annotation.length() == 0) ? "" : "&annotation=" + URLEncoder.encode(annotation, "UTF-8")) +
//					"&username=" + tnuluUserName +
//					"&password=" + tnuluPassword;
//				
//				//	get result
//				String result = IoTools.getPage(uploadUrlString);
//				TreeNode root = parser.parse(result);
//				TreeNode[] tableRows = TreeTools.getAllNodesOfType(root, "tr");
//				
//				for (int t = 1; (this.newTaxonData == null) && (t < tableRows.length); t++) {
//					TreeNode[] tableCells = TreeTools.getAllNodesOfType(tableRows[t], "td");
//					StringTupel rowData = new StringTupel();
//					for (int c = 0; (c < tableCells.length) && (c < AttributeNames.length); c++) {
//						TreeNode dataNode = tableCells[c].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//						rowData.setValue(AttributeNames[c], ((dataNode == null) ? UNKNOWN : IoTools.prepareForPlainText(dataNode.getNodeValue())));
//					}
//					this.newTaxonData = rowData;
//				}
//				
//				if (this.newTaxonData == null) {
//					System.out.println(uploadUrlString);
//					System.out.println(root.treeToCode("  "));
//					JOptionPane.showMessageDialog(this, ("HNS did not return an LSID. HNS response was\n  " + result), "HNS Upload Error", JOptionPane.ERROR_MESSAGE);
//					return false;
//				} else this.bucket.dataSet = new LsidDataSet(this.newTaxonData, 1);
//				
//			} catch (Exception e) {
//				e.printStackTrace(System.out);
//				if (JOptionPane.showConfirmDialog(this, ("An error occured while uploading the new taxon name to HNS:\n  " + e.getClass().getName() + "\n  " + e.getMessage() + "\n\nRetry uploading name?"), "HNS Upload Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION) {
//					this.conceptName.requestFocusInWindow();
//					return false;
//				}
//			}
//			
//			return true;
//		}
//		
//		//	new 091107: Holding Container for newly uploaded Taxa --> new URL
//		/*	http://atbi.biosci.ohio-state.edu:210/hymenoptera/Taxon_Holding.addTaxonHolder?
//		 *  taxon=
//		 * &pub_id=
//		 * &page_num=
//		 * &username=TaxonX
//		 * &password=TaxonX
//		 */
//		//	upload to HNS holding container
//		private boolean uploadKnownTaxon() {
//			
//			//	gather data
//			String conceptName = this.conceptName.getText().trim();
//			
//			String refPubId = this.referencePublicationModsID.getText().trim();
//			if (refPubId.startsWith("<")) refPubId = "";
//			String refPubPage = this.referencePublicationPageNumber.getText().trim();
//			if (refPubPage.startsWith("<")) refPubPage = "";
//			
//			//	check data
//			if (conceptName.length() == 0) {
//				JOptionPane.showMessageDialog(this, "The specified Concept Name is invalid", "Invalid Concept Name", JOptionPane.ERROR_MESSAGE);
//				this.conceptName.requestFocusInWindow();
//				return false;
//			}
//			if (!refPubId.matches("[0-9]++")) {
//				JOptionPane.showMessageDialog(this, "The specified MODS document ID is invalid", "Invalid MODS Document ID", JOptionPane.ERROR_MESSAGE);
//				this.referencePublicationModsID.requestFocusInWindow();
//				return false;
//			}
//			if (!refPubPage.matches("[0-9]++")) {
//				JOptionPane.showMessageDialog(this, "The specified page number is invalid", "Invalid Page Number", JOptionPane.ERROR_MESSAGE);
//				this.referencePublicationPageNumber.requestFocusInWindow();
//				return false;
//			}
//			
//			//	do upload
//			try {
//				System.out.println("TnuluLsidReferencer: uploading '" + conceptName + "'");
//				
//				String uploadUrlString = tnuluKnownUploadBaseUrl +
//					 "taxon=" + URLEncoder.encode(conceptName, "UTF-8") +
//					((refPubId.length() == 0) ? "" : "&pub_id=" + URLEncoder.encode(refPubId, "UTF-8")) +
//					((refPubPage.length() == 0) ? "" : "&page_num=" + URLEncoder.encode(refPubPage, "UTF-8")) +
//					"&username=" + tnuluUserName +
//					"&password=" + tnuluPassword;
//				
//				/* new response format
//				 SUCCESS: Taxon info was successfully entered!<br><br>New TNUID: 225452
//				 readLine() in a BufferedReader and cut number from end of line
//				*/
//				
//				//	get result
//				BufferedReader resultReader = new BufferedReader(new InputStreamReader(new URL(uploadUrlString).openStream(), "UTF-8"));
//				String resultLine = resultReader.readLine();
//				if (!resultLine.startsWith("SUCCESS:") && !resultLine.startsWith("WARNING:")) {
//					StringVector errorPageLines = new StringVector();
//					
//					errorPageLines.addElement(resultLine);
//					System.out.println(resultLine);
//					
//					resultLine = resultReader.readLine();
//					
//					while (resultLine != null) {
//						errorPageLines.addElement(resultLine);
//						System.out.println(resultLine);
//						
//						resultLine = resultReader.readLine();
//					}
//					
//					resultReader.close();
//					throw new IOException("Upload failed, HNS response was:" + errorPageLines.concatStrings("\n"));
//				}
//				
//				System.out.println(resultLine);
//				resultReader.close();
//				
//				String lsid = resultLine.substring(resultLine.lastIndexOf(' ') + 1);
//				StringTupel newTaxonData = new StringTupel();
//				newTaxonData.setValue(TaxonNameUseID, lsid);
//				newTaxonData.setValue(TaxonNameID, lsid);
//				newTaxonData.setValue(TaxonomicConceptID, lsid);
//				newTaxonData.setValue(Rank, Gamta.capitalize(this.rank));
//				newTaxonData.setValue(Valid, tnuluUploadValidity);
//				newTaxonData.setValue(StatusCode, tnuluUploadStatusCode);
//				newTaxonData.setValue(TaxonName, conceptName);
//				
//				this.newTaxonData = newTaxonData;
//				
//				if (this.newTaxonData == null) {
//					System.out.println(uploadUrlString);
//					JOptionPane.showMessageDialog(this, ("HNS did not return an LSID. HNS response was\n  " + resultLine), "HNS Upload Error", JOptionPane.ERROR_MESSAGE);
//					return false;
//				} else this.bucket.dataSet = new LsidDataSet(this.newTaxonData, 1);
//				
//			} catch (Exception e) {
//				e.printStackTrace(System.out);
//				if (JOptionPane.showConfirmDialog(this, ("An error occured while uploading the new taxon name to HNS:\n  " + e.getClass().getName() + "\n  " + e.getMessage() + "\n\nRetry uploading name?"), "HNS Upload Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION) {
//					this.conceptName.requestFocusInWindow();
//					return false;
//				}
//			}
//			
//			return true;
//		}
//	}
//}
