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

package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.dataProviders;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * LSID data provider for Hymenoptera Name Server (HNS)
 * 
 * @author sautter
 */
public class HnsDataProvider extends LsidDataProvider implements TaxonomicNameConstants, LiteratureConstants {
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getProviderCode()
	 */
	public String getProviderCode() {
		return "HNS";
	}
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getProviderName()
	 */
	public String getProviderName() {
		return "Hymenoptera Name Server";
	}
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getLsidUrnPrefix()
	 */
	public String getLsidUrnPrefix() {
		return "urn:lsid:biosci.ohio-state.edu:osuc_concepts:";
	}
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#init()
	 */
	protected void init() {
//		this.keys.addContent(AttributeNames);
		
		//	get request url
		this.tnuluRequestBaseUrl = this.getParameter(TNULU_REQUEST_URL_ATTRIBUTE, this.tnuluRequestBaseUrl);
		
		//	read upload data
		this.tnuluNewUploadBaseUrl = this.getParameter(TNULU_NEW_UPLOAD_URL_ATTRIBUTE);
		this.tnuluKnownUploadBaseUrl = this.getParameter(TNULU_KNOWN_UPLOAD_URL_ATTRIBUTE);
		this.tnuluUserName = this.getParameter(UPLOAD_USERNAME_ATTRIBUTE);
		this.tnuluPassword = this.getParameter(UPLOAD_PASSWORD_ATTRIBUTE);
		
		//	check if upload enabled
		if ((this.tnuluNewUploadBaseUrl == null) || (this.tnuluKnownUploadBaseUrl == null)/* || (this.tnuluUserName == null) || (this.tnuluPassword == null)*/) {
			this.tnuluNewUploadBaseUrl = null;
			this.tnuluKnownUploadBaseUrl = null;
			this.tnuluUserName = null;
			this.tnuluPassword = null;
		}
		
		//	if so, read upload options
		else {
			this.tnuluUploadStatusCode = this.getParameter(UPLOAD_STATUS_CODE_ATTRIBUTE, this.tnuluUploadStatusCode);
			this.tnuluUploadValidity = this.getParameter(UPLOAD_STATUS_CODE_ATTRIBUTE, this.tnuluUploadValidity);
			this.defaultConceptStatusOption = this.getParameter(DEFAULT_UPLOAD_CONCEPT_STATUS_ATTRIBUTE);
			this.defaultRelationTypeOption = this.getParameter(DEFAULT_UPLOAD_RELATION_TYPE_ATTRIBUTE);
			
			try {
				StringVector list = this.loadList(CONCEPT_RANK_OPTIONS_FILE_NAME);
				this.conceptRankOptions = list.toStringArray();
			} catch (IOException ioe) {
				this.conceptRankOptions = DEFAULT_CONCEPT_RANK_OPTIONS;
			}
			
			try {
				StringVector list = this.loadList(CONCEPT_STATUS_OPTIONS_FILE_NAME);
				this.conceptStatusOptions = list.toStringArray();
			} catch (IOException ioe) {
				this.conceptStatusOptions = DEFAULT_CONCEPT_STATUS_OPTIONS;
			}
			
			try {
				StringVector list = this.loadList(COUNTRY_OPTIONS_FILE_NAME);
				this.countryOptions = list.toStringArray();
			} catch (IOException ioe) {
				this.countryOptions = DEFAULT_COUNTRY_OPTIONS;
			}
			
			try {
				StringVector list = this.loadList(RELATION_TYPE_OPTIONS_FILE_NAME);
				this.relationTypeOptions = list.toStringArray();
			} catch (IOException ioe) {
				this.relationTypeOptions = DEFAULT_RELATION_TYPE_OPTIONS;
			}
			
			try {
				StringVector list = this.loadList(REMARK_PRESET_OPTIONS_FILE_NAME);
				list.insertElementAt(DEFAULT_REMARK_OPTION, 0);
				this.remarkPresetOptions = list.toStringArray();
			} catch (IOException ioe) {
				String[] options = {DEFAULT_REMARK_OPTION};
				this.remarkPresetOptions = options;
			}
		}
	}
	
	private static final String UNKNOWN = "Unknown";
	
	private static final String TaxonNameUseID = "TaxonNameUseID";
	private static final String TaxonNameID = "TaxonNameID";
	private static final String TaxonomicConceptID = "TaxonomicConceptID";
	private static final String Rank = "Rank";
	private static final String Valid = "Valid";
	private static final String StatusCode = "StatusCode";
	private static final String TaxonName = "TaxonName";
	
	private static final String[] AttributeNames = {TaxonNameUseID, TaxonNameID, TaxonomicConceptID, Rank, Valid, StatusCode, TaxonName};
//	private StringVector keys = new StringVector();
	
	private static final String TNULU_REQUEST_URL_ATTRIBUTE = "RequestBaseURL";
	private String tnuluRequestBaseUrl = "http://osuc.biosci.ohio-state.edu/hymenoptera/tnulu?the_name=";
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#getLsidData(java.lang.String, boolean)
	 */
	public LsidDataSet[] getLsidData(String epithet, boolean useWildcard) throws IOException {
		System.out.println("HNS LsidDataProvider: doing lookup for '" + epithet + "'");
		
		if (StringUtils.isUpperCaseWord(epithet)) {
			epithet = StringUtils.capitalize(epithet);
			System.out.println("  converted to '" + epithet + "'");
		}
		
		URL url = this.dataProvider.getURL(this.tnuluRequestBaseUrl + URLEncoder.encode((epithet + (useWildcard ? "%" : "")), "UTF-8"));
		
		final ArrayList lsidData = new ArrayList();
		
		TokenReceiver tr = new TokenReceiver() {
			
			private String data = null;
			
			private boolean firstRecord = true;
			private Properties record = new Properties();
			
			private int attributeIndex = 0;
			
			public void close() throws IOException {}
			
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					
					if (html.isEndTag(token)) {
						
						if ("tr".equals(type)) {
							if (firstRecord) firstRecord = false;
							else {
								String taxonNameUseID = this.record.getProperty(TaxonNameUseID);
								String taxonName = this.record.getProperty(TaxonName);
								String rank = this.record.getProperty(Rank, UNKNOWN);
								
								if ((taxonName != null) && (rank != null) && (taxonNameUseID != null))
									lsidData.add(new LsidDataSet(HnsDataProvider.this, taxonNameUseID, taxonName, rank));
							}
							
							this.record.clear();
							this.attributeIndex = 0;
						}
						
						else if ("td".equals(type)) {
							if ((this.data != null) && (this.attributeIndex < AttributeNames.length))
								this.record.setProperty(AttributeNames[this.attributeIndex], this.data);
							this.attributeIndex++;
						}
					}
					
					else this.data = null;
				}
				
				else this.data = IoTools.prepareForPlainText(token);
			}
		};
		
		InputStream is = null;
		try {
			is = url.openStream();
			parser.stream(is, tr);
		}
		
		finally {
			if (is != null)
				is.close();
		}
		
		return ((LsidDataSet[]) lsidData.toArray(new LsidDataSet[lsidData.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.idaho.plugins.lsidReferencer.LsidDataProvider#isUploadSupported()
	 */
	public boolean isUploadSupported() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider#doUpload(de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider.StatusDialog, de.uka.ipd.idaho.gamta.Annotation, boolean, de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.LsidDataProvider.LsidDataSet[])
	 */
	public LsidDataSet doUpload(StatusDialog statusDialog, Annotation taxonName, boolean isNewTaxon, LsidDataSet[] parentData) throws IOException {
		TaxonNameUploadDialog tnud = new TaxonNameUploadDialog(statusDialog.getDialog(), taxonName, isNewTaxon, parentData);
		tnud.setVisible(true);
		
		if (tnud.dataSet != null) return tnud.dataSet;
		
		else if (tnud.uploadException != null) throw tnud.uploadException;
		
		else return null;
	}
	
	
	private static final String TNULU_NEW_UPLOAD_URL_ATTRIBUTE = "NewUploadBaseURL";
	private String tnuluNewUploadBaseUrl = "http://osuc.biosci.ohio-state.edu/hymenoptera/tnulu_newname?";
	
	private static final String TNULU_KNOWN_UPLOAD_URL_ATTRIBUTE = "KnownUploadBaseURL";
	private String tnuluKnownUploadBaseUrl = "http://osuc.biosci.ohio-state.edu/hymenoptera/Taxon_Holding.addTaxonHolder?";//"http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu_newname?";
	
	
	private static final String UPLOAD_USERNAME_ATTRIBUTE = "UploadUserName";
	private String tnuluUserName = null;//"TaxonX";
	
	private static final String UPLOAD_PASSWORD_ATTRIBUTE = "UploadPassword";
	private String tnuluPassword = null;//"TaxonX";
	
	
	private static final String UPLOAD_STATUS_CODE_ATTRIBUTE = "UploadStatusCode";
	private String tnuluUploadStatusCode = "To Be Verified At HNS";
	
	private String tnuluUploadValidity = "Valid";
	
	private static final String DEFAULT_UPLOAD_CONCEPT_STATUS_ATTRIBUTE = "DefaultUploadConceptStatus";
	private String defaultConceptStatusOption = "Misspelling";
	
	private static final String DEFAULT_UPLOAD_RELATION_TYPE_ATTRIBUTE = "DefaultUploadRelationType";
	private String defaultRelationTypeOption = "Synonym";
	
	
	private static final String CONCEPT_RANK_OPTIONS_FILE_NAME = "conceptRankOptions.txt";
	private String[] conceptRankOptions = new String[0];
	private static final String[] DEFAULT_CONCEPT_RANK_OPTIONS = {"Family", "Subfamily", "Tribe", "Genus", "Subgenus", "Species", "Subspecies", "Variety"};
	
	private static final String CONCEPT_STATUS_OPTIONS_FILE_NAME = "conceptStatusOptions.txt";
	private String[] conceptStatusOptions = new String[0];
	private static final String[] DEFAULT_CONCEPT_STATUS_OPTIONS = {
			"Common name", 
			"Dubious", 
			"Emendation", 
			"Error", 
			"Homonym & junior synonym", 
			"Homonym & synonym", 
			"Incorrect original spelling", 
			"Invalid", 
			"Invalid combination", 
			"Invalid emendation", 
			"Invalid name", 
			"Invalid replacement", 
			"Junior homonym", 
			"Junior homonym & synonym", 
			"Junior synonym", 
			"Justified emendation", 
			"Literature misspelling", 
			"Misspelling", 
			"Nomen nudum", 
			"Original name", 
			"Original name/combination", 
			"Other", 
			"Replacement name", 
			"Subsequent name/combination", 
			"Syn-subsequent combination", 
			"Temp. Invalid", 
			"Suppressed by ruling", 
			"Unavailable", 
			"Unavailable, nomen nudum", 
			"Unavailable, other", 
			"Uncertain", 
			"Unjustified emendation", 
			"Unnecessary replacement name", 
			"Valid"
		};
	
	private static final String COUNTRY_OPTIONS_FILE_NAME = "countryOptions.txt";
	private static final String[] DEFAULT_COUNTRY_OPTIONS = {"<Enter Country>"};
	private String[] countryOptions = DEFAULT_COUNTRY_OPTIONS;
	
	private static final String RELATION_TYPE_OPTIONS_FILE_NAME = "relationTypeOptions.txt";
	private static final String[] DEFAULT_RELATION_TYPE_OPTIONS = {"Homonym", "Junior synonym", "Member", "Synonym"};
	private String[] relationTypeOptions = DEFAULT_RELATION_TYPE_OPTIONS;
	
	private static final String REMARK_PRESET_OPTIONS_FILE_NAME = "remarkPresetOptions.txt";
	private static final String DEFAULT_REMARK_OPTION = "<Enter or Select Remark>";
	private String[] remarkPresetOptions = {DEFAULT_REMARK_OPTION};
	
	private static final Grammar html = new Html();
	private static final Parser parser = new Parser(html); 
	
	private class TaxonNameUploadDialog extends JDialog {
		
		private LsidDataSet dataSet = null;
		
		private JTextField conceptName = new JTextField();
		private JComboBox conceptRank = new JComboBox(conceptRankOptions);
		private String rank = "";
		
		private JTextField authority = new JTextField();
		private JComboBox country = new JComboBox(countryOptions);
		
		private JComboBox parentLsid = new JComboBox();
		private JComboBox relationType = new JComboBox(relationTypeOptions);
		
		private JComboBox conceptStatus = new JComboBox(conceptStatusOptions);
		private JCheckBox validFlag = new JCheckBox("Valid ?");
		
		private JTextField referencePublicationModsID = new JTextField();
		private JTextField referencePublicationPageNumber = new JTextField();
		private JComboBox remark = new JComboBox(remarkPresetOptions);
		
		private JCheckBox isNewTaxon = new JCheckBox("New Taxon"); 
		private JButton uploadButton = new JButton("Upload Taxon");
		private JButton cancelButton = new JButton("Cancel Taxon");
		private JButton cancelAllButton = new JButton("Cancel All");
		
		private Annotation taxonName;
		private IOException uploadException = null;
		
		public TaxonNameUploadDialog(JDialog sd, Annotation taxonName, boolean isNewTaxon, LsidDataSet[] parentData) {
			super(sd, ("Upload Taxon Name '" + taxonName.getValue() + "' to HNS"), true);
			
			//	store data
			this.taxonName = taxonName;
			
			
			//	initialize buttons
			this.uploadButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.uploadButton.setPreferredSize(new Dimension(50, 21));
			this.uploadButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					uploadTaxonName();
				}
			});
			
			this.cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.cancelButton.setPreferredSize(new Dimension(50, 21));
			this.cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					uploadException = UPLOAD_CANCELLED;
					dispose();
				}
			});
			
			this.cancelAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.cancelAllButton.setPreferredSize(new Dimension(50, 21));
			this.cancelAllButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					uploadException = STOP_UPLOADING;
					dispose();
				}
			});
			
			this.isNewTaxon.setSelected(isNewTaxon);
			this.isNewTaxon.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					layoutInputFields();
				}
			});
			
			
			//	build data fields
			this.conceptName.setBorder(BorderFactory.createLoweredBevelBorder());
			
			this.authority.setBorder(BorderFactory.createLoweredBevelBorder());
			
			this.country.setBorder(BorderFactory.createLoweredBevelBorder());
			this.country.setEditable(true);
			
			this.conceptRank.setBorder(BorderFactory.createLoweredBevelBorder());
			this.conceptRank.setEditable(conceptRankOptions.length == 0);
			
			this.parentLsid.setBorder(BorderFactory.createLoweredBevelBorder());
			this.parentLsid.setEditable(true);
			
			this.relationType.setBorder(BorderFactory.createLoweredBevelBorder());
			this.relationType.setEditable(relationTypeOptions.length == 0);
			
			this.conceptStatus.setBorder(BorderFactory.createLoweredBevelBorder());
			this.conceptStatus.setEditable(conceptStatusOptions.length == 0);
			
			this.validFlag.setBorder(BorderFactory.createLoweredBevelBorder());
			
			this.remark.setBorder(BorderFactory.createLoweredBevelBorder());
			this.remark.setEditable(true);
			
			this.referencePublicationModsID.setBorder(BorderFactory.createLoweredBevelBorder());
			
			this.referencePublicationPageNumber.setBorder(BorderFactory.createLoweredBevelBorder());
			
			
			//	fill data fields
			String lastPart = null;
			String genus = null;
			String parentNameString = "";
			String parentNamePart = null;
			String parentRank = null;
			String attribute;
			String nameString = "";
			String rank = ((String) this.taxonName.getAttribute(RANK_ATTRIBUTE));
			
			
			attribute = ((String) this.taxonName.getAttribute(GENUS_ATTRIBUTE));
			if (attribute != null) {
				nameString += attribute;
				lastPart = attribute;
				
				parentNamePart = attribute;
				genus = attribute;
				
				rank = GENUS_ATTRIBUTE;
			}
			
			attribute = ((String) this.taxonName.getAttribute(SUBGENUS_ATTRIBUTE));
			if (attribute != null) {
				nameString += (" (" + attribute + ")");
				lastPart = attribute;
				
				if (parentNamePart != null)
					parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
				parentNamePart = attribute;
				
				parentRank = rank;
				rank = SUBGENUS_ATTRIBUTE;
			}
			
			attribute = ((String) this.taxonName.getAttribute(SPECIES_ATTRIBUTE));
			if (attribute != null) {
				nameString += (" " + attribute);
				lastPart = attribute;
				
				if (parentNamePart != null)
					parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
				parentNamePart = attribute;
				
				parentRank = rank;
				rank = SPECIES_ATTRIBUTE;
			}
			
			attribute = ((String) this.taxonName.getAttribute(SUBSPECIES_ATTRIBUTE));
			if (attribute != null) {
				nameString += (" subsp. " + attribute);
				lastPart = attribute;
				
				if (parentNamePart != null)
					parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
				parentNamePart = attribute;
				
				parentRank = rank;
				rank = SUBSPECIES_ATTRIBUTE;
			}
			
			attribute = ((String) this.taxonName.getAttribute(VARIETY_ATTRIBUTE));
			if (attribute != null) {
				nameString += (" var. " + attribute);
				lastPart = attribute;
				
				if (parentNamePart != null)
					parentNameString += (((parentNameString.length() == 0) ? "" : " ") + parentNamePart);
				parentNamePart = attribute;
				
				parentRank = rank;
				rank = VARIETY_ATTRIBUTE;
			}
			
			
			//	no attributes given
			if (lastPart == null)
				this.conceptName.setText(this.taxonName.getValue());
			
			//	use name built from attributes
			else this.conceptName.setText(nameString);
			
			
			//	store rank
			this.rank = rank;
			
			
			//	determine name authority
			String authority = this.taxonName.getDocumentProperty("ModsDocAuthor");
			
			//	document author not set, parse authority from annotation value
			if (authority == null) {
				
				authority = this.taxonName.getValue();
				int split = (((lastPart == null) || authority.endsWith(lastPart)) ? -1 : authority.lastIndexOf(lastPart));
				
				//	no name given
				if (split == -1) this.referencePublicationModsID.setText("<Enter Authority>");
					
				//	parse authority from annotation value
				else {
					split += lastPart.length();
					this.authority.setText(authority.substring(split).trim());
				}
			}
			
			//	use docuement author
			else this.referencePublicationModsID.setText(authority);
			
			
			//	treat varieties specially
			if (VARIETY_ATTRIBUTE.equals(rank)) {
				if (parentRank != null)
					this.conceptRank.setSelectedItem(StringUtils.capitalize(parentRank));
				
				if ((parentNameString.length() != 0) && (parentRank != null) && (genus != null)) {
					for (int d = 0; d < parentData.length; d++)
						this.parentLsid.addItem(parentData[d]);
					if (parentData.length != 0)
						this.parentLsid.setSelectedIndex(0);
				}
				
				this.relationType.setSelectedItem("Member");
				
				this.conceptStatus.setSelectedItem("Unavailable, other");
				
				this.validFlag.setSelected(false);
			}
			
			//	initialize data for other taxa
			else {
				
				if (!GENUS_ATTRIBUTE.equals(rank) && (parentNameString.length() != 0) && (parentRank != null) && (genus != null)) {
					for (int d = 0; d < parentData.length; d++)
						this.parentLsid.addItem(parentData[d]);
					if (parentData.length != 0)
						this.parentLsid.setSelectedIndex(0);
				}
				
				if (defaultRelationTypeOption != null)
					this.relationType.setSelectedItem(defaultRelationTypeOption);
				else if (relationTypeOptions.length != 0)
					this.relationType.setSelectedItem(relationTypeOptions[0]);
				else this.relationType.setSelectedItem("");
				
				if (defaultConceptStatusOption != null)
					this.conceptStatus.setSelectedItem(defaultConceptStatusOption);
				else if (conceptStatusOptions.length != 0)
					this.conceptStatus.setSelectedItem(conceptStatusOptions[0]);
				else this.conceptStatus.setSelectedItem("Member");
				
				this.validFlag.setSelected(true);
			}
			
			
			this.referencePublicationModsID.setText(this.taxonName.getDocumentProperty("ModsDocID", "<Enter MODS ID>"));
			
			String pageNumber = this.taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString();
			this.referencePublicationPageNumber.setText((pageNumber.length() == 0) ? "<Enter page number>" : pageNumber);
			
			
			this.remark.setSelectedItem(DEFAULT_REMARK_OPTION);
			
			
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			
			this.getContentPane().setLayout(new GridBagLayout());
			this.layoutInputFields();
			this.setLocationRelativeTo(sd);
		}
		
		private void layoutInputFields() {
			this.getContentPane().removeAll();
			if (this.isNewTaxon.isSelected())
				this.layoutNewTaxon();
			else this.layoutKnownTaxon();
			this.validate();
		}
		
		//	initialize for upload to HNS database
		private void layoutNewTaxon() {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			
			JLabel label;
			
			label = new JLabel("Concept Name / Rank", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.conceptName.setMinimumSize(new Dimension(290, 21));
			this.conceptName.setPreferredSize(new Dimension(290, 21));
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			this.getContentPane().add(this.conceptName, gbc.clone());
			gbc.gridx+=2;
			
			this.conceptRank.setMinimumSize(new Dimension(140, 21));
			this.conceptRank.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(this.conceptRank, gbc.clone());
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			label = new JLabel("Authority / Country", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.authority.setMinimumSize(new Dimension(140, 21));
			this.authority.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(this.authority, gbc.clone());
			gbc.gridx++;
			
			this.country.setMinimumSize(new Dimension(290, 21));
			this.country.setPreferredSize(new Dimension(290, 21));
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			this.getContentPane().add(this.country, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			label = new JLabel("Parent LSID", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.parentLsid.setMinimumSize(new Dimension(440, 21));
			this.parentLsid.setPreferredSize(new Dimension(440, 21));
			gbc.gridwidth = 3;
			gbc.weightx = 3;
			this.getContentPane().add(this.parentLsid, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			label = new JLabel("Status / Relation", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.conceptStatus.setMinimumSize(new Dimension(290, 21));
			this.conceptStatus.setPreferredSize(new Dimension(290, 21));
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			this.getContentPane().add(this.conceptStatus, gbc.clone());
			gbc.gridx+=2;
			
			this.relationType.setMinimumSize(new Dimension(140, 21));
			this.relationType.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(this.relationType, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			label = new JLabel("Pub. MODS ID / Page", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.referencePublicationModsID.setMinimumSize(new Dimension(140, 21));
			this.referencePublicationModsID.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(this.referencePublicationModsID, gbc.clone());
			gbc.gridx++;
			
			this.referencePublicationPageNumber.setMinimumSize(new Dimension(140, 21));
			this.referencePublicationPageNumber.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(this.referencePublicationPageNumber, gbc.clone());
			gbc.gridx++;
			
			this.validFlag.setMinimumSize(new Dimension(140, 21));
			this.validFlag.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(this.validFlag, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			label = new JLabel("Annotation / Remark", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.remark.setMinimumSize(new Dimension(440, 21));
			this.remark.setPreferredSize(new Dimension(440, 21));
			gbc.gridwidth = 3;
			this.getContentPane().add(this.remark, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			this.isNewTaxon.setMinimumSize(new Dimension(140, 21));
			this.isNewTaxon.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(this.isNewTaxon, gbc.clone());
			gbc.gridx++;
			
			this.uploadButton.setMinimumSize(new Dimension(140, 21));
			this.uploadButton.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(this.uploadButton, gbc.clone());
			gbc.gridx++;
			
			this.cancelButton.setMinimumSize(new Dimension(140, 21));
			this.cancelButton.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(this.cancelButton, gbc.clone());
			gbc.gridx++;
			
			this.cancelAllButton.setMinimumSize(new Dimension(140, 21));
			this.cancelAllButton.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(this.cancelAllButton, gbc.clone());
			
			
			this.setSize(600, 200);
		}
		
		//	initialize for upload to HNS holding container
		private void layoutKnownTaxon() {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			
			JLabel label;
			
			label = new JLabel("Concept Name", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.conceptName.setMinimumSize(new Dimension(440, 21));
			this.conceptName.setPreferredSize(new Dimension(440, 21));
			gbc.gridwidth = 3;
			gbc.weightx = 3;
			this.getContentPane().add(this.conceptName, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			label = new JLabel("Pub. MODS ID", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.referencePublicationModsID.setMinimumSize(new Dimension(140, 21));
			this.referencePublicationModsID.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(this.referencePublicationModsID, gbc.clone());
			gbc.gridx++;
			
			label = new JLabel("Page Number", JLabel.RIGHT);
			label.setMinimumSize(new Dimension(140, 21));
			label.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(label, gbc.clone());
			gbc.gridx++;
			
			this.referencePublicationPageNumber.setMinimumSize(new Dimension(140, 21));
			this.referencePublicationPageNumber.setPreferredSize(new Dimension(140, 21));
			gbc.weightx = 1;
			this.getContentPane().add(this.referencePublicationPageNumber, gbc.clone());
			
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			this.isNewTaxon.setMinimumSize(new Dimension(140, 21));
			this.isNewTaxon.setPreferredSize(new Dimension(140, 21));
			gbc.gridwidth = 1;
			gbc.weightx = 1;
			this.getContentPane().add(this.isNewTaxon, gbc.clone());
			gbc.gridx++;
			
			this.uploadButton.setMinimumSize(new Dimension(140, 21));
			this.uploadButton.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(this.uploadButton, gbc.clone());
			gbc.gridx++;
			
			this.cancelButton.setMinimumSize(new Dimension(140, 21));
			this.cancelButton.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(this.cancelButton, gbc.clone());
			gbc.gridx++;
			
			this.cancelAllButton.setMinimumSize(new Dimension(140, 21));
			this.cancelAllButton.setPreferredSize(new Dimension(140, 21));
			this.getContentPane().add(this.cancelAllButton, gbc.clone());
			
			this.setSize(600, 100);
		}
		
		private void uploadTaxonName() {
			if (this.isNewTaxon.isSelected() ? this.uploadNewTaxon() : this.uploadKnownTaxon())
				this.dispose();
		}
		
		//	upload to HNS database
		private boolean uploadNewTaxon() {
			
			//	gather data
			String conceptName = this.conceptName.getText().trim();
			String authority = this.authority.getText().trim();
			
			String conceptRank = "";
			Object cro = this.conceptRank.getSelectedItem();
			if (cro != null) conceptRank = cro.toString();
			
			String parentLsid = "";
			Object plo = this.parentLsid.getSelectedItem();
			if (plo instanceof LsidDataSet)
				parentLsid = ((LsidDataSet) plo).lsidNumber;
			else if (plo != null) parentLsid = plo.toString().trim();
			
			String relationType = "";
			Object rto = this.relationType.getSelectedItem();
			if (rto != null) relationType = rto.toString();
			
			String conceptStatus = this.conceptStatus.getSelectedItem().toString().trim();
			String validFlag = (this.validFlag.isSelected() ? "Valid" : "Invalid");
			
			String refPubId = this.referencePublicationModsID.getText().trim();
			if (refPubId.startsWith("<")) refPubId = "";
			String refPubPage = this.referencePublicationPageNumber.getText().trim();
			if (refPubPage.startsWith("<")) refPubPage = "";
			String annotation = this.remark.getSelectedItem().toString().trim();
			if (annotation.startsWith("<")) annotation = "";
			
			//	check data
			if (conceptName.length() == 0) {
				JOptionPane.showMessageDialog(this, "The specified Concept Name is invalid", "Invalid Concept Name", JOptionPane.ERROR_MESSAGE);
				this.conceptName.requestFocusInWindow();
				return false;
			}
			if (authority.length() == 0) {
				JOptionPane.showMessageDialog(this, "The specified Authority is invalid", "Invalid Authority", JOptionPane.ERROR_MESSAGE);
				this.authority.requestFocusInWindow();
				return false;
			}
//			if (!parentLsid.matches("[0-9]++")) {
//				JOptionPane.showMessageDialog(this, "The specified Parent LSID is invalid", "Invalid Parent LSID", JOptionPane.ERROR_MESSAGE);
//				this.parentLsid.requestFocusInWindow();
//				return false;
//			}
//			if (conceptStatus.length() == 0) {
//				JOptionPane.showMessageDialog(this, "The specified Concept Status is invalid", "Invalid Concept Status", JOptionPane.ERROR_MESSAGE);
//				this.conceptStatus.requestFocusInWindow();
//				return false;
//			}
			if (!refPubId.matches("[0-9]++")) {
				JOptionPane.showMessageDialog(this, "The specified MODS document ID is invalid", "Invalid MODS Document ID", JOptionPane.ERROR_MESSAGE);
				this.referencePublicationModsID.requestFocusInWindow();
				return false;
			}
			if (!refPubPage.matches("[0-9]++")) {
				JOptionPane.showMessageDialog(this, "The specified page number is invalid", "Invalid Page Number", JOptionPane.ERROR_MESSAGE);
				this.referencePublicationPageNumber.requestFocusInWindow();
				return false;
			}
			if (!this.validFlag.isSelected()) {
				if (JOptionPane.showConfirmDialog(this, "The taxonomic name to upload is marked as invalid. Proceed anyway?", "Taxonomic Name Marked Invalid", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
					return false;
			}
			
			//	check authentication
			String userName = tnuluUserName;
			String password = tnuluPassword;
			boolean isAdHocAuthentication = false;
			
			if ((userName == null) || (password == null)) {
				AuthenticationDialog ad = new AuthenticationDialog("Log in to HNS", userName);
				ad.setVisible(true);
				
				if ((ad.userNameValue == null) || (ad.passwordValue == null))
					return false;
				
				userName = ad.userNameValue;
				password = ad.passwordValue;
				isAdHocAuthentication = true;
			}
			
			//	do upload
			try {
				System.out.println("TnuluLsidReferencer: uploading '" + conceptName + "'");
				
				String uploadUrlString = tnuluNewUploadBaseUrl +
					 "concept_name=" + URLEncoder.encode(conceptName, "UTF-8") +
					((authority.length() == 0) ? "" : "&authority=" + URLEncoder.encode(authority, "UTF-8")) +
					"&parent_lsid=" + parentLsid +
					"&concept_rank=" + conceptRank +
					"&concept_status=" + URLEncoder.encode(conceptStatus, "UTF-8") +
					((relationType.length() == 0) ? "" : "&reln_type=" + URLEncoder.encode(relationType, "UTF-8")) +
					"&v_flag=" + validFlag +
					((refPubId.length() == 0) ? "" : "&ref_id=" + URLEncoder.encode(refPubId, "UTF-8")) +
					((refPubPage.length() == 0) ? "" : "&pages=" + URLEncoder.encode(refPubPage, "UTF-8")) +
					((annotation.length() == 0) ? "" : "&annotation=" + URLEncoder.encode(annotation, "UTF-8")) +
					"&username=" + userName +
					"&password=" + password;
				
				//	get result
				String result = IoTools.getPage(uploadUrlString);
				TreeNode root = parser.parse(result);
				TreeNode[] tableRows = TreeTools.getAllNodesOfType(root, "tr");
				
				String lsid = null;
				for (int t = 1; (lsid == null) && (t < tableRows.length); t++) {
					TreeNode[] tableCells = TreeTools.getAllNodesOfType(tableRows[t], "td");
					StringTupel rowData = new StringTupel();
					for (int c = 0; (c < tableCells.length) && (c < AttributeNames.length); c++) {
						TreeNode dataNode = tableCells[c].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
						rowData.setValue(AttributeNames[c], ((dataNode == null) ? UNKNOWN : IoTools.prepareForPlainText(dataNode.getNodeValue())));
					}
					lsid = rowData.getValue(TaxonNameUseID);
				}
				
				if (lsid == null) {
					System.out.println(uploadUrlString);
					System.out.println(root.treeToCode("  "));
//					JOptionPane.showMessageDialog(this, ("HNS did not return an LSID. HNS response was\n  " + result), "HNS Upload Error", JOptionPane.ERROR_MESSAGE);
					JOptionPane.showMessageDialog(this, result, "HNS Upload Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				else {
					this.dataSet = new LsidDataSet(HnsDataProvider.this, lsid, conceptName, this.rank);
					if (isAdHocAuthentication) {
						tnuluUserName = userName;
						tnuluPassword = password;
					}
					return true;
				}
			}
			
			catch (final Exception e) {
				e.printStackTrace(System.out);
				
				if (JOptionPane.showConfirmDialog(this, ("An error occured while uploading the new taxon name to HNS:\n  " + e.getClass().getName() + "\n  " + e.getMessage() + "\n\nRetry uploading name?"), "HNS Upload Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION) {
					this.conceptName.requestFocusInWindow();
					return false;
				}
				
				else {
					if (e instanceof IOException)
						uploadException = ((IOException) e);
					
					else uploadException = new IOException() {
						public Throwable getCause() {
							return e;
						}
						public String getMessage() {
							return (e.getClass().getName() + " (" + e.getMessage() + ")");
						}
						public StackTraceElement[] getStackTrace() {
							return e.getStackTrace();
						}
						public void printStackTrace() {
							e.printStackTrace();
						}
						public void printStackTrace(PrintStream s) {
							e.printStackTrace(s);
						}
						public void printStackTrace(PrintWriter s) {
							e.printStackTrace(s);
						}
						public String toString() {
							return e.toString();
						}
					};
					
					return true;
				}
			}
		}
		
		//	new 091107: Holding Container for newly uploaded Taxa --> new URL
		/*	http://atbi.biosci.ohio-state.edu:210/hymenoptera/Taxon_Holding.addTaxonHolder?
		 *  taxon=
		 * &pub_id=
		 * &page_num=
		 * &username=TaxonX
		 * &password=TaxonX
		 */
		//	upload to HNS holding container
		private boolean uploadKnownTaxon() {
			
			//	gather data
			String conceptName = this.conceptName.getText().trim();
			
			String refPubId = this.referencePublicationModsID.getText().trim();
			if (refPubId.startsWith("<")) refPubId = "";
			String refPubPage = this.referencePublicationPageNumber.getText().trim();
			if (refPubPage.startsWith("<")) refPubPage = "";
			
			//	check data
			if (conceptName.length() == 0) {
				JOptionPane.showMessageDialog(this, "The specified Concept Name is invalid", "Invalid Concept Name", JOptionPane.ERROR_MESSAGE);
				this.conceptName.requestFocusInWindow();
				return false;
			}
			if (!refPubId.matches("[0-9]++")) {
				JOptionPane.showMessageDialog(this, "The specified MODS document ID is invalid", "Invalid MODS Document ID", JOptionPane.ERROR_MESSAGE);
				this.referencePublicationModsID.requestFocusInWindow();
				return false;
			}
			if (!refPubPage.matches("[0-9]++")) {
				JOptionPane.showMessageDialog(this, "The specified page number is invalid", "Invalid Page Number", JOptionPane.ERROR_MESSAGE);
				this.referencePublicationPageNumber.requestFocusInWindow();
				return false;
			}
			
			//	check authentication
			String userName = tnuluUserName;
			String password = tnuluPassword;
			boolean isAdHocAuthentication = false;
			
			if ((userName == null) || (password == null)) {
				AuthenticationDialog ad = new AuthenticationDialog("Log in to HNS", userName);
				ad.setVisible(true);
				
				if ((ad.userNameValue == null) || (ad.passwordValue == null))
					return false;
				
				userName = ad.userNameValue;
				password = ad.passwordValue;
				isAdHocAuthentication = true;
			}
			
			//	do upload
			try {
				System.out.println("TnuluLsidReferencer: uploading '" + conceptName + "'");
				
				String uploadUrlString = tnuluKnownUploadBaseUrl +
					 "taxon=" + URLEncoder.encode(conceptName, "UTF-8") +
					((refPubId.length() == 0) ? "" : "&pub_id=" + URLEncoder.encode(refPubId, "UTF-8")) +
					((refPubPage.length() == 0) ? "" : "&page_num=" + URLEncoder.encode(refPubPage, "UTF-8")) +
					"&username=" + userName +
					"&password=" + password;
				
				/* new response format
				 SUCCESS: Taxon info was successfully entered!<br><br>New TNUID: 225452
				 WARNING: <taxonName> is already in the database. TNUID: 225452
				 readLine() in a BufferedReader and cut number from end of line
				*/
				
				//	get result
//				BufferedReader resultReader = new BufferedReader(new InputStreamReader(new URL(uploadUrlString).openStream(), "UTF-8"));
				BufferedReader resultReader = new BufferedReader(new InputStreamReader(dataProvider.getURL(uploadUrlString).openStream(), "UTF-8"));
				String resultLine = resultReader.readLine();
				
				if (resultLine.startsWith("SUCCESS:") || resultLine.startsWith("WARNING:")) {
					
					System.out.println(resultLine);
					resultReader.close();
					
					String lsid = resultLine.substring(resultLine.lastIndexOf(' ') + 1);
					this.dataSet = new LsidDataSet(HnsDataProvider.this, lsid, conceptName, this.rank);
					
					if (isAdHocAuthentication) {
						tnuluUserName = userName;
						tnuluPassword = password;
					}
					
					return true;
				}
				
				else {
					StringVector errorPageLines = new StringVector();
					
					errorPageLines.addElement(resultLine);
					System.out.println(resultLine);
					
					resultLine = resultReader.readLine();
					
					while (resultLine != null) {
						errorPageLines.addElement(resultLine);
						System.out.println(resultLine);
						
						resultLine = resultReader.readLine();
					}
					
					resultReader.close();
					throw new IOException("Upload failed, HNS response was:" + errorPageLines.concatStrings("\n"));
				}
			}
			
			catch (final Exception e) {
				e.printStackTrace(System.out);
				if (JOptionPane.showConfirmDialog(this, ("An error occured while uploading the new taxon name to HNS:\n  " + e.getClass().getName() + "\n  " + e.getMessage() + "\n\nRetry uploading name?"), "HNS Upload Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION) {
					this.conceptName.requestFocusInWindow();
					return false;
				}
				
				else {
					if (e instanceof IOException)
						uploadException = ((IOException) e);
					
					else uploadException = new IOException() {
						public Throwable getCause() {
							return e;
						}
						public String getMessage() {
							return (e.getClass().getName() + " (" + e.getMessage() + ")");
						}
						public StackTraceElement[] getStackTrace() {
							return e.getStackTrace();
						}
						public void printStackTrace() {
							e.printStackTrace();
						}
						public void printStackTrace(PrintStream s) {
							e.printStackTrace(s);
						}
						public void printStackTrace(PrintWriter s) {
							e.printStackTrace(s);
						}
						public String toString() {
							return e.toString();
						}
					};
					
					return true;
				}
			}
		}
		
		private class AuthenticationDialog extends JDialog {
			
			private JTextField userNameField = new JTextField();
			private JTextField passwordField = new JTextField();
			
			private String userNameValue;
			private String passwordValue;
			
			AuthenticationDialog(String title, String userName) {
				super(TaxonNameUploadDialog.this, title, true);
				
				JPanel fieldPanel = new JPanel(new GridBagLayout());
				
				this.userNameField.setBorder(BorderFactory.createLoweredBevelBorder());
				if (userName != null) this.userNameField.setText(userName) ;
				
				this.passwordField.setBorder(BorderFactory.createLoweredBevelBorder());
				this.passwordField.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						commit();
					}
				});
				
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
				
				gbc.gridwidth = 6;
				gbc.weightx = 4;
				fieldPanel.add(new JLabel("Please enter your login data for HNS."), gbc.clone());
				
				gbc.gridy++;
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				fieldPanel.add(new JLabel("User Name", JLabel.LEFT), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 2;
				gbc.gridwidth = 2;
				fieldPanel.add(this.userNameField, gbc.clone());
				gbc.gridx = 3;
				gbc.weightx = 1;
				gbc.gridwidth = 1;
				fieldPanel.add(new JLabel("Password", JLabel.LEFT), gbc.clone());
				gbc.gridx = 4;
				gbc.weightx = 2;
				gbc.gridwidth = 2;
				fieldPanel.add(this.passwordField, gbc.clone());
				
				JButton okButton = new JButton("OK");
				okButton.setBorder(BorderFactory.createRaisedBevelBorder());
				okButton.setPreferredSize(new Dimension(100, 21));
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						commit();
					}
				});
				
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.setPreferredSize(new Dimension(100, 21));
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						userNameValue = null;
						passwordValue = null;
						dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.add(okButton);
				buttonPanel.add(cancelButton);
				
				this.add(fieldPanel, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
				
				//	ensure dialog is closed with button
				this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				this.setSize(500, 100);
				this.setResizable(true);
			}
			
			private void commit() {
				
				//	check data
				String value = this.userNameField.getText().trim();
				if (value.length() == 0)
					JOptionPane.showMessageDialog(this, "Please specify a user name.", "Error in Login Data", JOptionPane.ERROR_MESSAGE);
				
				else {
					//	get data
					this.userNameValue = value;
					this.passwordValue = this.passwordField.getText().trim();
					
					//	we are done here
					dispose();
				}
			}
		}
	}
}
