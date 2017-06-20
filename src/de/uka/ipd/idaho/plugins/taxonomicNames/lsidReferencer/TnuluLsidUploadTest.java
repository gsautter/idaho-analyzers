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
//import java.awt.Dimension;
//import java.awt.Frame;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.HeadlessException;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLEncoder;
//
//import javax.swing.BorderFactory;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JDialog;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JTextField;
//import javax.swing.UIManager;
//
///**
// * @author sautter
// *
// */
//public class TnuluLsidUploadTest {
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		
//		//	set platform L&F
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		System.getProperties().put( "proxySet", "true" );
//		System.getProperties().put( "proxyHost", "proxy.rz.uni-karlsruhe.de" );
//		System.getProperties().put( "proxyPort", "3128" );
//		
////		String queryString = "http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu?the_name=Camponotus%20lateralis%25";
////		URL url = new URL(queryString);
//		
////		String urlString = "http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu_newname?" +
////				 "concept_name=Zzz1camponotus" +
////				"&authority=(Olivier)" +
////				"&parent_lsid=2414" +
////				"&concept_rank=Genus" +
////				"&concept_status=Misspelling" +
////				"&reln_type=Synonym" +
////				"&v_flag=Invalid" +
////				"&username=TaxonX" +
////				"&password=TaxonX";
////		URL url = new URL(urlString);
////		
////		InputStream is = url.openStream();
////		StringBuffer res = new StringBuffer();
////		int i;
////		while ((i = is.read()) != -1) res.append((char) i);
////		is.close();
////		System.out.println(res.toString());
//		
//		TnuluUploadDialog tud = new TnuluUploadDialog(null);
//		tud.setVisible(true);
//	}
//}
//
//class TnuluUploadDialog extends JDialog {
//	
//	private static final String[] CONCEPT_RANK_OPTIONS = {"Family", "Subfamily", "Tribe", "Genus", "Subgenus", "Species", "Subspecies", "Variety"};
//	private static final String[] CONCEPT_STATUS_OPTIONS = {
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
//	private static final String[] RELATION_TYPE_OPTIONS = {"Junior synonym", "Member", "Synonym"};
//	
//	private JTextField conceptName = new JTextField();
//	private JTextField authority = new JTextField();
//	
//	private JComboBox conceptRank = new JComboBox(CONCEPT_RANK_OPTIONS);
//	private JTextField parentLsid = new JTextField();
//	private JComboBox relationType = new JComboBox(RELATION_TYPE_OPTIONS);
//	
//	private JComboBox conceptStatus = new JComboBox(CONCEPT_STATUS_OPTIONS);
//	private JCheckBox validFlag = new JCheckBox("Valid ?");
//	
//	//	TODO: put these parameters in config file
//	private String tnuluUploadBaseUrl = "http://atbi.biosci.ohio-state.edu:210/hymenoptera/tnulu_newname?";
//	private String tnuluUserName = "TaxonX";
//	private String tnuluPassword = "TaxonX";
//	private String defaultConceptStatusOption = "Misspelling";
//	private String defaultRelationTypeOption = "Synonym";
//	
//	public TnuluUploadDialog(Frame owner) throws HeadlessException {
//		super(owner, "Upload new Taxon Name", true);
//		
//		this.conceptName.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.conceptName.setPreferredSize(new Dimension(200, 21));
//		
//		this.authority.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.authority.setPreferredSize(new Dimension(50, 21));
//		
//		this.conceptRank.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.conceptRank.setPreferredSize(new Dimension(this.conceptRank.getPreferredSize().width, 21));
//		this.conceptRank.setEditable(false);
//		
//		this.parentLsid.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.parentLsid.setPreferredSize(new Dimension(50, 21));
//		
//		this.relationType.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.relationType.setPreferredSize(new Dimension(this.relationType.getPreferredSize().width, 21));
//		this.relationType.setEditable(false);
//		if (this.defaultRelationTypeOption != null) this.relationType.setSelectedItem(this.defaultRelationTypeOption);
//		
//		this.conceptStatus.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.conceptStatus.setPreferredSize(new Dimension(this.conceptStatus.getPreferredSize().width, 21));
//		this.conceptStatus.setEditable(true);
//		if (this.defaultConceptStatusOption != null) this.conceptStatus.setSelectedItem(this.defaultConceptStatusOption);
//		
//		this.validFlag.setBorder(BorderFactory.createLoweredBevelBorder());
//		this.validFlag.setPreferredSize(new Dimension(50, 21));
//		
//		JButton button = new JButton("Upload Taxon Name");
//		button.setBorder(BorderFactory.createRaisedBevelBorder());
//		button.setPreferredSize(new Dimension(50, 21));
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				uploadTaxonName();
//			}
//		});
//		
//		this.getContentPane().setLayout(new GridBagLayout());
//		GridBagConstraints gbc = new GridBagConstraints();
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
//		this.getContentPane().add(new JLabel("Concept Name", JLabel.RIGHT), gbc.clone());
//		gbc.gridx++;
//		gbc.gridwidth = 3;
//		this.getContentPane().add(this.conceptName, gbc.clone());
//		gbc.gridwidth = 1;
//		
//		gbc.gridy++;
//		gbc.gridx = 0;
//		this.getContentPane().add(new JLabel("Concept Rank", JLabel.RIGHT), gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(this.conceptRank, gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(new JLabel("Authority", JLabel.RIGHT), gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(this.authority, gbc.clone());
//		
//		gbc.gridy++;
//		gbc.gridx = 0;
//		this.getContentPane().add(new JLabel("Parent LSID", JLabel.RIGHT), gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(this.parentLsid, gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(new JLabel("Parent Relation", JLabel.RIGHT), gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(this.relationType, gbc.clone());
//		
//		gbc.gridy++;
//		gbc.gridx = 0;
//		this.getContentPane().add(this.validFlag, gbc.clone());
//		gbc.gridx++;
//		this.getContentPane().add(new JLabel("Concept Status", JLabel.RIGHT), gbc.clone());
//		gbc.gridx++;
//		gbc.gridwidth = 2;
//		this.getContentPane().add(this.conceptStatus, gbc.clone());
//		
//		gbc.gridy++;
//		gbc.gridx = 0;
//		gbc.gridwidth = 4;
//		this.getContentPane().add(button, gbc.clone());
//		
//		this.setSize(450, 160);
//		this.setLocationRelativeTo(owner);
//	}
//	
//	private void uploadTaxonName() {
//		
//		//	gather data
//		String conceptName = this.conceptName.getText().trim();
//		String authority = this.authority.getText().trim();
//		
//		String conceptRank = this.conceptRank.getSelectedItem().toString();
//		String parentLsid = this.parentLsid.getText().trim();
//		String relationType = this.relationType.getSelectedItem().toString();
//		
//		String conceptStatus = this.conceptStatus.getSelectedItem().toString().trim();
//		String validFlag = (this.validFlag.isSelected() ? "Valid" : "Invalid");
//		
//		//	check data
//		if (conceptName.length() == 0) {
//			JOptionPane.showMessageDialog(this, "The specified Concept Name is invalid", "Invalid Concept Name", JOptionPane.ERROR_MESSAGE);
//			this.conceptName.requestFocusInWindow();
//			return;
//		}
//		if (!parentLsid.matches("[0-9]++")) {
//			JOptionPane.showMessageDialog(this, "The specified Parent LSID is invalid", "Invalid Parent LSID", JOptionPane.ERROR_MESSAGE);
//			this.parentLsid.requestFocusInWindow();
//			return;
//		}
//		if (conceptStatus.length() == 0) {
//			JOptionPane.showMessageDialog(this, "The specified Concept Status is invalid", "Invalid Concept Status", JOptionPane.ERROR_MESSAGE);
//			this.conceptStatus.requestFocusInWindow();
//			return;
//		}
//		
//		//	do upload
//		try {
//			String urlString = this.tnuluUploadBaseUrl +
//				 "concept_name=" + URLEncoder.encode(conceptName, "UTF-8") +
//				((authority.length() == 0) ? "" : "&authority=" + URLEncoder.encode(authority, "UTF-8")) +
//				"&parent_lsid=" + parentLsid +
//				"&concept_rank=" + conceptRank +
//				"&concept_status=" + URLEncoder.encode(conceptStatus, "UTF-8") +
//				"&reln_type=" + URLEncoder.encode(relationType, "UTF-8") +
//				"&v_flag=" + validFlag +
//				"&username=" + this.tnuluUserName +
//				"&password=" + this.tnuluPassword;
//			
//			URL url = new URL(urlString);
//			InputStream is = url.openStream();
//			StringBuffer res = new StringBuffer();
//			int i;
//			while ((i = is.read()) != -1) res.append((char) i);
//			is.close();
//			System.out.println(res.toString());
//			
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//}
