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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.StandaloneAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TaxonomicNameAttributeParser extends FatAnalyzer implements LiteratureConstants, TaxonomicNameConstants {
	
	private static final boolean DEBUG = false;
	
	private static final String LABEL_EXTENSION = "Label";
	private static final String AUTHOR_EXTENSION = "Author";
	
	private static final String GENUS_PART = GENUS_ATTRIBUTE;
	private static final String GENUS_AUTHOR_PART = GENUS_PART + AUTHOR_EXTENSION;
	private static final String SUBGENUS_PART = SUBGENUS_ATTRIBUTE;
	private static final String SUBGENUS_AUTHOR_PART = SUBGENUS_PART + AUTHOR_EXTENSION;
	private static final String SPECIES_PART = SPECIES_ATTRIBUTE;
	private static final String SPECIES_AUTHOR_PART = SPECIES_PART + AUTHOR_EXTENSION;
	private static final String SUBSPECIES_PART = SUBSPECIES_ATTRIBUTE;
	private static final String SUBSPECIES_LABEL_PART = SUBSPECIES_PART + LABEL_EXTENSION;
	private static final String SUBSPECIES_AUTHOR_PART = SUBSPECIES_PART + AUTHOR_EXTENSION;
	private static final String VARIETY_PART = VARIETY_ATTRIBUTE;
	private static final String VARIETY_LABEL_PART = VARIETY_PART + LABEL_EXTENSION;
	private static final String VARIETY_AUTHOR_PART = VARIETY_PART + AUTHOR_EXTENSION;
	
	private static final String[] PARTS = {
			GENUS_PART,
			GENUS_AUTHOR_PART,
			SUBGENUS_PART,
			SUBGENUS_AUTHOR_PART,
			SPECIES_PART,
			SPECIES_AUTHOR_PART,
			SUBSPECIES_LABEL_PART,
			SUBSPECIES_PART,
			SUBSPECIES_AUTHOR_PART,
			VARIETY_LABEL_PART,
			VARIETY_PART,
			VARIETY_AUTHOR_PART
		};
	
	private static final int GENUS_PART_CODE = 1;
	private static final int GENUS_AUTHOR_PART_CODE = 2;
	
	private static final int SUBGENUS_PART_CODE = 4;
	private static final int SUBGENUS_AUTHOR_PART_CODE = 5;
	
	private static final int SPECIES_PART_CODE = 7;
	private static final int SPECIES_AUTHOR_PART_CODE = 8;
	
	private static final int SUBSPECIES_LABEL_PART_CODE = 9;
	private static final int SUBSPECIES_PART_CODE = 10;
	private static final int SUBSPECIES_AUTHOR_PART_CODE = 11;
	
	private static final int VARIETY_LABEL_PART_CODE = 12;
	private static final int VARIETY_PART_CODE = 13;
	private static final int VARIETY_AUTHOR_PART_CODE = 14;
	
	private static final PartCodePair[] CODES_TO_PARTS = {
		new PartCodePair(null, 0),
		new PartCodePair(GENUS_PART, GENUS_PART_CODE),
		new PartCodePair(GENUS_AUTHOR_PART, GENUS_AUTHOR_PART_CODE),
		new PartCodePair(null, 3),
		new PartCodePair(SUBGENUS_PART, SUBGENUS_PART_CODE),
		new PartCodePair(SUBGENUS_AUTHOR_PART, SUBGENUS_AUTHOR_PART_CODE),
		new PartCodePair(null, 6),
		new PartCodePair(SPECIES_PART, SPECIES_PART_CODE),
		new PartCodePair(SPECIES_AUTHOR_PART, SPECIES_AUTHOR_PART_CODE),
		new PartCodePair(SUBSPECIES_LABEL_PART, SUBSPECIES_LABEL_PART_CODE),
		new PartCodePair(SUBSPECIES_PART, SUBSPECIES_PART_CODE),
		new PartCodePair(SUBSPECIES_AUTHOR_PART, SUBSPECIES_AUTHOR_PART_CODE),
		new PartCodePair(VARIETY_LABEL_PART, VARIETY_LABEL_PART_CODE),
		new PartCodePair(VARIETY_PART, VARIETY_PART_CODE),
		new PartCodePair(VARIETY_AUTHOR_PART, VARIETY_AUTHOR_PART_CODE),
	};
	
	private static final HashMap PARTS_TO_CODES = new HashMap();
	static {
		for (int c = 0; c < CODES_TO_PARTS.length; c++)
			PARTS_TO_CODES.put(CODES_TO_PARTS[c].part, CODES_TO_PARTS[c]);
	}
	
	private static class PartCodePair {
		final String part;
		final int code;
		PartCodePair(String part, int code) {
			this.part = part;
			this.code = code;
		}
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	check parameter
		if ((data == null) || !parameters.containsKey(INTERACTIVE_PARAMETER)) return;
		
		//	make sure taxon names have page numbers
		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber != null) {
				Annotation[] pTaxonomicNames = paragraphs[p].getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
				for (int t = 0; t < pTaxonomicNames.length; t++)
					pTaxonomicNames[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			}
		}
		
		//	get taxonomic names
		MutableAnnotation[] taxonomicNames = data.getMutableAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	filter out names with above-ganus rank
		ArrayList genusOrBelowList = new ArrayList();
		for (int t = 0; t < taxonomicNames.length; t++) {
			if ((taxonomicNames[t].size() == 1) && Gamta.isFirstLetterUpWord(taxonomicNames[t].firstValue())) {
				String name = Gamta.capitalize(taxonomicNames[t].firstValue());
				if (name.endsWith("idae")) {
					taxonomicNames[t].setAttribute(FAMILY_ATTRIBUTE, name);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, FAMILY_ATTRIBUTE);
				} else if (name.endsWith("inae")) {
					taxonomicNames[t].setAttribute(SUBFAMILY_ATTRIBUTE, name);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, SUBFAMILY_ATTRIBUTE);
				} else if (name.endsWith("ini")) {
					taxonomicNames[t].setAttribute(TRIBE_ATTRIBUTE, name);
					taxonomicNames[t].setAttribute(RANK_ATTRIBUTE, TRIBE_ATTRIBUTE);
				} else genusOrBelowList.add(taxonomicNames[t]);
			} else genusOrBelowList.add(taxonomicNames[t]);
		}
		taxonomicNames = ((MutableAnnotation[]) genusOrBelowList.toArray(new MutableAnnotation[genusOrBelowList.size()]));
		
		//	bucketize taxonomic names, and collect parts
		StringVector bucketNames = new StringVector();
		HashMap bucketsByName = new HashMap();
		
		StringVector genusList = new StringVector();
		StringVector genusSubGenusList = new StringVector();
		StringVector lowerCasePartList = new StringVector();
		
		for (int t = 0; t < taxonomicNames.length; t++) {
			String taxonNameKey = getNameString(taxonomicNames[t]);
			taxonNameKey += taxonomicNames[t].getValue();
			if (bucketsByName.containsKey(taxonNameKey))
				((TaxonNameBucket) bucketsByName.get(taxonNameKey)).taxonNames.add(taxonomicNames[t]);
			
			else {
				TaxonNameBucket bucket = new TaxonNameBucket(taxonomicNames[t]);
				bucketNames.addElementIgnoreDuplicates(taxonNameKey);
				bucketsByName.put(taxonNameKey, bucket);
			}
			
			String attribute;
			
			attribute = taxonomicNames[t].getAttribute(VARIETY_ATTRIBUTE, MISSING_PART).toString();
			if (!MISSING_PART.equals(attribute)) lowerCasePartList.addElementIgnoreDuplicates(attribute);
			
			attribute = taxonomicNames[t].getAttribute(SUBSPECIES_ATTRIBUTE, MISSING_PART).toString();
			if (!MISSING_PART.equals(attribute)) lowerCasePartList.addElementIgnoreDuplicates(attribute);
			
			attribute = taxonomicNames[t].getAttribute(SPECIES_ATTRIBUTE, MISSING_PART).toString();
			if (!MISSING_PART.equals(attribute)) lowerCasePartList.addElementIgnoreDuplicates(attribute);
			
			attribute = taxonomicNames[t].getAttribute(SUBGENUS_ATTRIBUTE, MISSING_PART).toString();
			if (!MISSING_PART.equals(attribute)) genusSubGenusList.addElementIgnoreDuplicates(attribute);
			
			attribute = taxonomicNames[t].getAttribute(GENUS_ATTRIBUTE, MISSING_PART).toString();
			if (!MISSING_PART.equals(attribute)) {
				genusList.addElementIgnoreDuplicates(attribute);
				genusSubGenusList.addElementIgnoreDuplicates(attribute);
			}
		}
		
		//	process buckets
		ArrayList bucketList = new ArrayList();
		for (int b = 0; b < bucketNames.size(); b++) {
			if (bucketsByName.containsKey(bucketNames.get(b)))
				bucketList.add(bucketsByName.get(bucketNames.get(b)));
		}
		TaxonNameBucket[] buckets = ((TaxonNameBucket[]) bucketList.toArray(new TaxonNameBucket[bucketList.size()]));
		
		//	don't show empty dialog
		if (buckets.length != 0) {
			
			//	build name part arrays
			String[] genera = genusList.toStringArray();
			Arrays.sort(genera);
			String[] generaSubGenera = genusSubGenusList.toStringArray();
			Arrays.sort(generaSubGenera);
			String[] lowerCaseParts = lowerCasePartList.toStringArray();
			Arrays.sort(lowerCaseParts);
			
			//	assemble dialog
			final JDialog td = DialogFactory.produceDialog("Please Check Attributes of Taxon Names", true);
			
			TaxonNameBucketTrayPanel trayPanel = new TaxonNameBucketTrayPanel();
			JScrollPane panelBox = new JScrollPane(trayPanel);
			panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			panelBox.getVerticalScrollBar().setUnitIncrement(50);
			td.getContentPane().setLayout(new BorderLayout());
			td.getContentPane().add(panelBox, BorderLayout.CENTER);
			
			//	add classification boxes
			for (int b = 0; b < buckets.length; b++)
				trayPanel.trays.add(new TaxonNameBucketTray(td, buckets[b], trayPanel, b, genera, generaSubGenera, lowerCaseParts, data));
			trayPanel.layoutTrays();
			
			//	add OK button
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					td.dispose();
				}
			});
			td.getContentPane().add(okButton, BorderLayout.SOUTH);
			
			//	ensure dialog is closed with button
			td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			//	get feedback
			td.setSize(700, (Math.min(650, ((140 * trayPanel.trayCount()) + 50))));
			td.setLocationRelativeTo(null);
			td.setVisible(true);
			
			//	process feedback
			for (int t = 0; t < trayPanel.trayCount(); t++) {
				TaxonNameBucketTray tray = trayPanel.trayAt(t);
				if (tray.remove.isSelected()) {
					for (int a = 0; a < tray.bucket.taxonNames.size(); a++)
						data.removeAnnotation((Annotation) tray.bucket.taxonNames.get(a));
				} else tray.commitChange();
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
	
	private static class TaxonNameBucket implements Comparable {
		private String taxonNameString;
		private MutableAnnotation taxonName;
		private ArrayList taxonNames = new ArrayList();
		private TaxonNameBucket(MutableAnnotation taxonName) {
			this.taxonNameString = getNameString(taxonName);
			if (this.taxonNameString.length() == 0) this.taxonNameString = taxonName.getValue();
			this.taxonName = taxonName;
			this.taxonNames.add(taxonName);
		}
		public int compareTo(Object o) {
			if (o instanceof TaxonNameBucket) return this.taxonNameString.compareTo(((TaxonNameBucket) o).taxonNameString);
			else return -1;
		}
	}
	
	private static class TaxonNameBucketTrayPanel extends JPanel {
		private ArrayList trays = new ArrayList();
		private TaxonNameBucketTrayPanel() {
			super(new GridBagLayout(), true);
		}
		
		private void layoutTrays() {
			this.removeAll();
			
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
			
			for (int t = 0; t < this.trayCount(); t++) {
				this.add(this.trayAt(t), gbc.clone());
				gbc.gridy++;
			}
			gbc.weighty = 1;
			this.add(new JPanel(), gbc.clone());
		}
		
		private void setTrayIndices() {
			for (int t = 0; t < this.trayCount(); t++)
				this.trayAt(t).trayIndex = t;
		}
		
		private TaxonNameBucketTray trayAt(int index) {
			return ((TaxonNameBucketTray) this.trays.get(index));
		}
		
		private int trayCount() {
			return this.trays.size();
		}
		
		private void insertTrayAt(int index, TaxonNameBucketTray tray) {
			this.trays.add(index, tray);
			this.setTrayIndices();
			this.layoutTrays();
		}
	}
	
	private static class TaxonNameBucketTray extends JPanel {
		
		private JLabel changeLabel = new JLabel("Change to:", JLabel.LEFT);
		private JLabel normalizedNameLabel = new JLabel("", JLabel.LEFT);
		
		private JButton reset = new JButton("Reset");
		private JButton clear = new JButton("Reparse");
		private JButton edit = new JButton("Edit");
		private JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 4, 0));
		
		private JCheckBox remove = new JCheckBox("Remove Taxon Name");
		
		private ArrayList partTrays = new ArrayList();
		
		private Properties attributes = new Properties();
		
		private String subSpeciesLabel = null;
		private String varietyLabel = null;
		
		private JDialog attributeEditDialog;
		private TaxonNameBucketTrayPanel parent;
		private int trayIndex;
		
		private String[] genera;
		private String[] generaSubGenera;
		private String[] lowerCaseParts;
		
		boolean isInOptionChange = true;
		
		private Token[] parts;
		private int[] partMeanings;
		
		private TaxonNameBucket bucket;
		private MutableAnnotation data;
		
		private StandaloneAnnotation taxonName;
		private StandaloneAnnotation[] taxonNames;
		
		private TaxonNameBucketTray(JDialog attributeEditDialog, TaxonNameBucket bucket, TaxonNameBucketTrayPanel parent, int trayIndex, String[] genera, String[] generaSubGenera, String[] lowerCaseParts, MutableAnnotation data) {
			super(new GridBagLayout(), true);
			this.attributeEditDialog = attributeEditDialog;
			this.bucket = bucket;
			this.parent = parent;
			this.trayIndex = trayIndex;
			
			this.genera = genera;
			this.generaSubGenera = generaSubGenera;
			this.lowerCaseParts = lowerCaseParts;
			
			this.data = data;
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			this.reset.setBorder(BorderFactory.createRaisedBevelBorder());
			this.reset.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					reset(true);
				}
			});
			this.buttonPanel.add(this.reset);
			
			this.clear.setBorder(BorderFactory.createRaisedBevelBorder());
			this.clear.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					reset(false);
				}
			});
			this.buttonPanel.add(this.clear);
			
			this.edit.setBorder(BorderFactory.createRaisedBevelBorder());
			this.edit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					edit();
				}
			});
			this.buttonPanel.add(this.edit);
			
			this.copyDataFromBucket();
			this.initTrayData();
			
			this.producePartTrays();
			this.layoutParts();
			this.isInOptionChange = false;
			this.updateNameLabels();
		}
		
		private void copyDataFromBucket() {
			this.taxonName = Gamta.newAnnotation(this.data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, this.bucket.taxonName.getStartIndex(), this.bucket.taxonName.size());
			this.taxonName.copyAttributes(this.bucket.taxonName);
			
			this.taxonNames = new StandaloneAnnotation[this.bucket.taxonNames.size()];
			for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
				Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
				this.taxonNames[t] = Gamta.newAnnotation(this.data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, taxonName.getStartIndex(), taxonName.size());
				this.taxonNames[t].copyAttributes(taxonName);
			}
		}
		
		private void initTrayData() {
			this.attributes.clear();
			for (int p = 0; p < PART_NAMES.length; p++) {
				String attribute = PART_NAMES[p];
				Object value = this.taxonName.getAttribute(attribute);
				if (value != null)
					this.attributes.setProperty(attribute, value.toString());
			}
			
			ArrayList partList = new ArrayList();
			ArrayList partIndexList = new ArrayList();
			for (int t = 0; t < this.taxonName.size(); t++) {
				Token token = this.taxonName.tokenAt(t);
				if (Gamta.isWord(token) || Gamta.isNumber(token)) {
					partList.add(token);
					partIndexList.add(new Integer(t));
				}
			}
			this.parts = ((Token[]) partList.toArray(new Token[partList.size()]));
			this.partMeanings = new int[this.parts.length];
			this.setPartMeanings();
			
			if (DEBUG) {
				System.out.println("Parsed name " + this.taxonName.getValue());
				for (int i = 0; i < this.partMeanings.length; i++)
					System.out.println(this.parts[i] + " - " + this.partMeanings[i] + " (" + CODES_TO_PARTS[this.partMeanings[i]].part + ")");
			}
		}
		
		private void setPartMeanings() {
			this.setPartMeanings(0, GENUS_PART);
		}
		
		private void setPartMeanings(int fromPart, String nextPart) {
			for (int t = fromPart; t < this.parts.length; t++) {
				Token token = this.parts[t];
				
				//	upper case part
				if (Gamta.isFirstLetterUpWord(token) && !FAT.upperCaseLowerCaseParts.contains(token.getValue()) && !token.getValue().equals(this.attributes.getProperty(SPECIES_ATTRIBUTE)) && !token.getValue().equals(this.attributes.getProperty(SUBSPECIES_ATTRIBUTE)) && !token.getValue().equals(this.attributes.getProperty(VARIETY_ATTRIBUTE))) {
					if (GENUS_PART.equals(nextPart)) {
						if ("(".equals(this.taxonName.firstToken()) || (token.getValue().equals(this.attributes.getProperty(SUBGENUS_ATTRIBUTE)) && !token.getValue().equals(this.attributes.getProperty(GENUS_ATTRIBUTE)))) {
							this.partMeanings[t] = SUBGENUS_PART_CODE;
							nextPart = SPECIES_PART;
						}
						else {
							this.partMeanings[t] = GENUS_PART_CODE;
							nextPart = SUBGENUS_PART;
						}
					}
					
					else if (SUBGENUS_PART.equals(nextPart)) {
						this.partMeanings[t] = SUBGENUS_PART_CODE;
						nextPart = SPECIES_PART;
					}
					
					else if (SPECIES_PART.equals(nextPart))
						this.partMeanings[t] = SUBGENUS_AUTHOR_PART_CODE;
					
					else if (SUBSPECIES_PART.equals(nextPart)) {
						
						//	upper case part labelled as subspecies
						if ((t != 0) && (this.partMeanings[t-1] == SUBSPECIES_LABEL_PART_CODE)) {
							this.partMeanings[t] = SUBSPECIES_PART_CODE;
							nextPart = VARIETY_PART;
						}
						else this.partMeanings[t] = SPECIES_AUTHOR_PART_CODE;
					}
					
					else if (VARIETY_PART.equals(nextPart)) {
						
						//	upper case part labelled as variety
						if ((t != 0) && (this.partMeanings[t-1] == VARIETY_LABEL_PART_CODE)) {
							this.partMeanings[t] = VARIETY_PART_CODE;
							nextPart = null;
						}
						else this.partMeanings[t] = SUBSPECIES_AUTHOR_PART_CODE;
					}
					
					else this.partMeanings[t] = VARIETY_AUTHOR_PART_CODE;
				}
				
				//	lower case part
				else if (Gamta.isWord(token)) {
					
					if (GENUS_PART.equals(nextPart)) nextPart = SUBGENUS_PART;
					
					if (SUBGENUS_PART.equals(nextPart)) nextPart = SPECIES_PART;
					
					if (SPECIES_PART.equals(nextPart)) {
						if (token.getValue().equals(this.attributes.getProperty(SPECIES_ATTRIBUTE))) {
							this.partMeanings[t] = SPECIES_PART_CODE;
							nextPart = SUBSPECIES_PART;
						}
						else if (token.getValue().equals(this.attributes.getProperty(SUBSPECIES_ATTRIBUTE))) {
							this.partMeanings[t] = SUBSPECIES_PART_CODE;
							nextPart = VARIETY_PART;
						}
						else if (token.getValue().equals(this.subSpeciesLabel) || (!token.getValue().equals(this.varietyLabel) && (FAT.subSpeciesLabels.contains(token.getValue()) || FAT.subSpeciesLabels.contains(token.getValue() + ".")))) {
							this.partMeanings[t] = SUBSPECIES_LABEL_PART_CODE;
							nextPart = SUBSPECIES_PART;
						}
						else if (token.getValue().equals(this.attributes.getProperty(VARIETY_ATTRIBUTE))) {
							this.partMeanings[t] = VARIETY_PART_CODE;
							nextPart = null;
						}
						else if (token.getValue().equals(this.varietyLabel) || FAT.varietyLabels.contains(token.getValue()) || FAT.varietyLabels.contains(token.getValue() + ".")) {
							this.partMeanings[t] = VARIETY_LABEL_PART_CODE;
							nextPart = VARIETY_PART;
						}
						else {
							this.partMeanings[t] = SPECIES_PART_CODE;
							nextPart = SUBSPECIES_PART;
						}
					}
					
					else if (SUBSPECIES_PART.equals(nextPart)) {
						if (token.getValue().equals(this.attributes.getProperty(SUBSPECIES_ATTRIBUTE))) {
							this.partMeanings[t] = SUBSPECIES_PART_CODE;
							nextPart = VARIETY_PART;
						}
						else if (token.getValue().equals(this.attributes.getProperty(VARIETY_ATTRIBUTE))) {
							this.partMeanings[t] = VARIETY_PART_CODE;
							nextPart = null;
						}
						else if (token.getValue().equals(this.varietyLabel) || FAT.varietyLabels.contains(token.getValue()) || FAT.varietyLabels.contains(token.getValue() + ".")) {
							this.partMeanings[t] = VARIETY_LABEL_PART_CODE;
							nextPart = VARIETY_PART;
						}
						else if (FAT.subSpeciesLabels.contains(token.getValue()) || FAT.subSpeciesLabels.contains(token.getValue() + ".")) {
							this.partMeanings[t] = SUBSPECIES_LABEL_PART_CODE;
							nextPart = SUBSPECIES_PART;
						}
						else {
							this.partMeanings[t] = SUBSPECIES_PART_CODE;
							nextPart = VARIETY_PART;
						}
					}
					
					else if (VARIETY_PART.equals(nextPart)) {
						if (token.getValue().equals(this.varietyLabel) || FAT.varietyLabels.contains(token.getValue()) || FAT.varietyLabels.contains(token.getValue() + ".")) {
							this.partMeanings[t] = VARIETY_LABEL_PART_CODE;
							nextPart = VARIETY_PART;
						}
						else {
							this.partMeanings[t] = VARIETY_PART_CODE;
							nextPart = null;
						}
					}
					
					else this.partMeanings[t] = VARIETY_AUTHOR_PART_CODE;
				}
				
				//	number or other
				else {
					if (SUBGENUS_PART.equals(nextPart)) {
						this.partMeanings[t] = GENUS_AUTHOR_PART_CODE;
						nextPart = SPECIES_PART;
					}
					else if (SPECIES_PART.equals(nextPart))
						this.partMeanings[t] = SUBGENUS_AUTHOR_PART_CODE;
					
					else if (SUBSPECIES_PART.equals(nextPart))
						this.partMeanings[t] = SPECIES_AUTHOR_PART_CODE;
					
					else if (VARIETY_PART.equals(nextPart))
						this.partMeanings[t] = SUBSPECIES_AUTHOR_PART_CODE;
					
					else this.partMeanings[t] = VARIETY_AUTHOR_PART_CODE;
				}
			}
		}
		
		private void reset(boolean restoreData) {
			this.isInOptionChange = true;
			
			this.attributes.clear();
			this.subSpeciesLabel = null;
			this.varietyLabel = null;
			
			if (restoreData) {
				this.copyDataFromBucket();
				this.initTrayData();
			}
			else this.setPartMeanings();
			
			this.producePartTrays();
			this.layoutParts();
			this.isInOptionChange = false;
			this.updateNameLabels();
		}
		
		private void edit() {
			try {
				TaxonEditDialog ted = new TaxonEditDialog(this, this.bucket.taxonName, this.taxonName, this.data);
				ted.setVisible(true);
				
				if (ted.isSplit()) {
					this.isInOptionChange = true;
					
					int splitIndex = (ted.newTaxonName.getStartIndex() - this.taxonName.getStartIndex());
					
					MutableAnnotation firstTaxon = this.data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, this.bucket.taxonName.getStartIndex(), splitIndex);
					firstTaxon.copyAttributes(this.taxonName);
					cleanAttributes(firstTaxon);
					
					MutableAnnotation secondTaxon = this.data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, (this.bucket.taxonName.getStartIndex() + splitIndex), (this.bucket.taxonName.size() - splitIndex));
					secondTaxon.copyAttributes(this.taxonName);
					cleanAttributes(secondTaxon);
					
					TaxonNameBucket firstTnb = new TaxonNameBucket(firstTaxon);
					TaxonNameBucket secondTnb = new TaxonNameBucket(secondTaxon);
					
					this.data.removeAnnotation(this.bucket.taxonName);
					
					for (int t = 1; t < this.bucket.taxonNames.size(); t++) {
						Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
						
						firstTaxon = this.data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, taxonName.getStartIndex(), splitIndex);
						firstTaxon.copyAttributes(taxonName);
						cleanAttributes(firstTaxon);
						
						secondTaxon = this.data.addAnnotation(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, (taxonName.getStartIndex() + splitIndex), (taxonName.size() - splitIndex));
						secondTaxon.copyAttributes(taxonName);
						cleanAttributes(secondTaxon);
						
						firstTnb.taxonNames.add(firstTaxon);
						secondTnb.taxonNames.add(secondTaxon);
						
						this.data.removeAnnotation(taxonName);
					}
					
					this.bucket = firstTnb;
					this.copyDataFromBucket();
					this.initTrayData();
					
					this.producePartTrays();
					this.layoutParts();
					this.isInOptionChange = false;
					
					this.updateNameLabels();
					
					TaxonNameBucketTray tnbt = new TaxonNameBucketTray(this.attributeEditDialog, secondTnb, this.parent, (this.trayIndex + 1), this.genera, this.generaSubGenera, this.lowerCaseParts, this.data);
					this.parent.insertTrayAt((this.trayIndex + 1), tnbt);
				}
				
				else if (ted.isCommitted()) {
					this.isInOptionChange = true;
					
					int startShift = (ted.newTaxonName.getStartIndex() - this.taxonName.getStartIndex());
					for (int t = 0; t < this.taxonNames.length; t++) {
						StandaloneAnnotation newTaxonName = Gamta.newAnnotation(this.data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, (this.taxonNames[t].getStartIndex() + startShift), ted.newTaxonName.size());
						newTaxonName.copyAttributes(this.taxonNames[t]);
						this.taxonNames[t] = newTaxonName;
					}
					
					this.taxonName = ted.newTaxonName;
					cleanAttributes(this.taxonName);
					this.initTrayData();
					
					this.producePartTrays();
					this.layoutParts();
					this.isInOptionChange = false;
					
					this.updateNameLabels();
				}
			} catch (BadLocationException ble) {}
		}
		
		private static void cleanAttributes(Annotation taxon) {
			for (int p = 0; p < PART_NAMES.length; p++)
				taxon.removeAttribute(PART_NAMES[p]);
		}
		
		private class TaxonEditDialog extends JDialog {
			
			private SimpleAttributeSet textFontStyle = new SimpleAttributeSet();
			
			private JTextPane editor = new JTextPane();
			private StyledDocument editorDocument = this.editor.getStyledDocument();
			
			private Annotation originalTaxonName;
			private StandaloneAnnotation taxonName;
			
			private TokenSequence data;
			
			private StandaloneAnnotation newTaxonName = null;
			private boolean split = false;
			
			private TaxonEditDialog(TaxonNameBucketTray tnbt, Annotation originalTaxonName, StandaloneAnnotation taxonName, TokenSequence data) throws BadLocationException {
				super(attributeEditDialog, "Edit Taxon", true);
				
				//	store data
				this.originalTaxonName = originalTaxonName;
				this.taxonName = taxonName;
				this.data = data;
				
				//	add explanation
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(new JLabel("Please select the actual taxon name."), BorderLayout.NORTH);
				
				//	create & protect text
				this.editor.setEditable(false);
				for (int v = 0; v < this.originalTaxonName.size(); v++) {
					if (v != 0) this.editorDocument.insertString(this.editorDocument.getLength(), this.originalTaxonName.getWhitespaceAfter(v - 1), null);
					this.editorDocument.insertString(this.editorDocument.getLength(), this.originalTaxonName.valueAt(v), null);
				}
				
				//	create text style
				this.textFontStyle.addAttribute(StyleConstants.FontConstants.Family, "Verdana");
				this.textFontStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(12));
				this.textFontStyle.addAttribute(StyleConstants.ColorConstants.Foreground, Color.BLACK);
				this.textFontStyle.addAttribute(StyleConstants.ColorConstants.Background, Color.WHITE);
				
				//	reset display
				this.editorDocument.setCharacterAttributes(0, this.editorDocument.getLength(), this.textFontStyle, true);
				
				//	highlight annotation
				this.editor.setSelectionStart(this.taxonName.getStartOffset() - this.originalTaxonName.getStartOffset());
				this.editor.setSelectionEnd(this.editorDocument.getLength() - (this.originalTaxonName.getEndOffset() - this.taxonName.getEndOffset()));
				
				//	make changes visible
				this.editor.validate();
				
				try { //  show caret
					this.editor.getCaret().setVisible(true);
					this.editor.getCaret().setBlinkRate(500);
				} catch (Exception e) {}
				
				//	put editor on display
				this.getContentPane().add(new JScrollPane(this.editor), BorderLayout.CENTER);
				
				//	create buttons
				JButton okButton = new JButton("OK");
				okButton.setBorder(BorderFactory.createRaisedBevelBorder());
				okButton.setPreferredSize(new Dimension(70, 21));
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						StandaloneAnnotation newTaxonName = annotateSelection();
						if (newTaxonName == null) {
							JOptionPane.showMessageDialog(TaxonEditDialog.this, "The selected taxon name is invalid, please select the taxon name.", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
							return;
						}
						TaxonEditDialog.this.newTaxonName = newTaxonName; 
						dispose();
					}
				});
				JButton splitButton = new JButton("Split");
				splitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				splitButton.setPreferredSize(new Dimension(70, 21));
				splitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						StandaloneAnnotation newTaxonName = annotateSelection();
						if (newTaxonName == null) {
							JOptionPane.showMessageDialog(TaxonEditDialog.this, "The selected splitting point is invalid, please select the token to split the taxon at.", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
							return;
						}
						else if (JOptionPane.showConfirmDialog(TaxonEditDialog.this, "Splitting a taxon name cannot be undone with the reset function. Split anyway?", "Confirm Split Taxon", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							TaxonEditDialog.this.newTaxonName = newTaxonName;
							split = true;
							dispose();
						}
					}
				});
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.setPreferredSize(new Dimension(70, 21));
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						newTaxonName = null;
						dispose();
					}
				});
				
				//	put buttons on display
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.add(okButton);
				buttonPanel.add(splitButton);
				buttonPanel.add(cancelButton);
				this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				
				//	set size and position
				this.setSize(400, 100);
				this.setResizable(true);
				this.setLocationRelativeTo(tnbt);
			}
			
			private boolean isCommitted() {
				return (this.newTaxonName != null);
			}
			
			private boolean isSplit() {
				return (this.isCommitted() && this.split);
			}
			
			private StandaloneAnnotation annotateSelection() {
				int start = this.editor.getSelectionStart();
				int end = this.editor.getSelectionEnd();
				
				if (start == end)
					return null;
				
				else if (end < start) {
					int temp = end;
					end = start;
					start = temp;
				}
				
				//	find start index
				int startTokenIndex = 0;
				while (startTokenIndex < this.originalTaxonName.size()) {
					if (this.originalTaxonName.tokenAt(startTokenIndex).getEndOffset() <= start) startTokenIndex++;
					else break;
				}
				
				//	find end index
				int endTokenIndex = this.originalTaxonName.size() - 1;
				while (endTokenIndex > -1) {
					if (this.originalTaxonName.tokenAt(endTokenIndex).getStartOffset() >= end) endTokenIndex--;
					else break;
				}
				
				if (startTokenIndex > endTokenIndex) return null;
				
				return Gamta.newAnnotation(this.data, FAT.TAXONOMIC_NAME_ANNOTATION_TYPE, (this.originalTaxonName.getStartIndex() + startTokenIndex), (endTokenIndex - startTokenIndex + 1));
			}
		}
		
		private void producePartTrays() {
			this.partTrays.clear();
			
			ArrayList partsToComplete = new ArrayList();
			Properties fullParts = new Properties();
			
			int partIndex = 0;
			for (int p = 0; (p < CODES_TO_PARTS.length) && (partIndex < this.parts.length); p++) {
				
				//	skip the unused parts
				if (CODES_TO_PARTS[p].part != null) {
					
					//	insert missing part
					if (CODES_TO_PARTS[p].code < this.partMeanings[partIndex]) {
						
						//	insert genus
						if (CODES_TO_PARTS[p].code == GENUS_PART_CODE) {
							TaxonNamePartTray genusPt = new TaxonNamePartTray("<None>", -1);
							genusPt.partOption.setSelectedItem(GENUS_PART);
							genusPt.completePart.setModel(new DefaultComboBoxModel(this.genera));
							
							String genus = this.attributes.getProperty(GENUS_ATTRIBUTE, "");
							if (genus.length() < 3) {
								partsToComplete.add(genusPt);
//								genus = this.findFullForm(GENUS_ATTRIBUTE, genus, null, null);
							}
							else fullParts.setProperty(GENUS_ATTRIBUTE, genus);
							
							genusPt.completePart.setSelectedItem(genus);
							this.partTrays.add(genusPt);
						}
						
						//	insert subGenus
						else if (CODES_TO_PARTS[p].code == SUBGENUS_PART_CODE) {
							TaxonNamePartTray subGenusPt = new TaxonNamePartTray("<None>", -1);
							subGenusPt.partOption.setSelectedItem(SUBGENUS_PART);
							subGenusPt.completePart.setModel(new DefaultComboBoxModel(this.generaSubGenera));
							
							String subGenus = this.attributes.getProperty(SUBGENUS_ATTRIBUTE, "");
							subGenusPt.completePart.setSelectedItem(subGenus);
							if (subGenus.length() > 2) fullParts.setProperty(SUBGENUS_ATTRIBUTE, subGenus);
							
							this.partTrays.add(subGenusPt);
						}
						
						//	insert species
						else if (CODES_TO_PARTS[p].code == SPECIES_PART_CODE) {
							TaxonNamePartTray speciesPt = new TaxonNamePartTray("<None>", -1);
							speciesPt.partOption.setSelectedItem(SPECIES_PART);
							speciesPt.completePart.setModel(new DefaultComboBoxModel(this.lowerCaseParts));
							
							String species = this.attributes.getProperty(SPECIES_ATTRIBUTE, "");
							if (species.length() < 3) {
								partsToComplete.add(speciesPt);
//								species = this.findFullForm(SPECIES_ATTRIBUTE, species, null, null);
							}
							else fullParts.setProperty(SPECIES_ATTRIBUTE, species);
							
							speciesPt.completePart.setSelectedItem(species);
							this.partTrays.add(speciesPt);
						}
						
						//	insert subSpecies
						else if (CODES_TO_PARTS[p].code == SUBSPECIES_PART_CODE) {
							TaxonNamePartTray subSpeciesPt = new TaxonNamePartTray("<None>", -1);
							subSpeciesPt.partOption.setSelectedItem(SUBSPECIES_PART);
							subSpeciesPt.completePart.setModel(new DefaultComboBoxModel(this.lowerCaseParts));
							
							String subSpecies = this.attributes.getProperty(SUBSPECIES_ATTRIBUTE, "");
							subSpeciesPt.completePart.setSelectedItem(subSpecies);
							if (subSpecies.length() > 2) fullParts.setProperty(SUBSPECIES_ATTRIBUTE, subSpecies);
							
							this.partTrays.add(subSpeciesPt);
						}
					}
					
					//	produce trays for current part
					else while ((partIndex < this.parts.length) && (CODES_TO_PARTS[p].code == this.partMeanings[partIndex])) {
						
						//	put part on tray
						TaxonNamePartTray pt = new TaxonNamePartTray(this.parts[partIndex].getValue(), partIndex);
						pt.partOption.setSelectedItem(CODES_TO_PARTS[p].part);
						this.partTrays.add(pt);
						
						//	fill combo box of name part
						if ((CODES_TO_PARTS[p].code % 3) == 1) {
							if (CODES_TO_PARTS[p].code == GENUS_PART_CODE)
								pt.completePart.setModel(new DefaultComboBoxModel(this.genera));
							else if (CODES_TO_PARTS[p].code == SUBGENUS_PART_CODE)
								pt.completePart.setModel(new DefaultComboBoxModel(this.generaSubGenera));
							else pt.completePart.setModel(new DefaultComboBoxModel(this.lowerCaseParts));
							
							String part = this.attributes.getProperty(CODES_TO_PARTS[p].part, this.parts[partIndex].getValue());
							if (part.length() < 3) {
								partsToComplete.add(pt);
//								part = this.findFullForm(CODES_TO_PARTS[p].part, part, null, null);
							}
							else fullParts.setProperty(CODES_TO_PARTS[p].part, part);
							
							pt.completePart.setSelectedItem(part);
						}
						
						//	switch to next part
						partIndex ++;
					}
				}
			}
			
			//	complete parts
			for (int p = 0; p < partsToComplete.size(); p++) {
				TaxonNamePartTray ptc = ((TaxonNamePartTray) partsToComplete.get(p));
				
				String partName = ptc.option;
				String part = ("<None>".endsWith(ptc.part) ? "" : ptc.part);
				String fullPart = null;
				int partCode = ((PartCodePair) PARTS_TO_CODES.get(partName)).code;
				
				for (int c = (partCode + 1); (c < CODES_TO_PARTS.length) && (fullPart == null); c++) {
					if ((c % 3) == 1) {
						String anchorPart = fullParts.getProperty(CODES_TO_PARTS[c].part);
						if (anchorPart != null)
							fullPart = this.findFullForm(partName, part, CODES_TO_PARTS[c].part, anchorPart);
					}
				}
				
				if (fullPart == null)
					fullPart = this.findFullForm(partName, part, null, null);
				
				if (fullPart != null)
					ptc.completePart.setSelectedItem(fullPart);
			}
		}
		
		private String findFullForm(String partName, String partAbbreviation, String msfPartName, String msfPart) {
			
			//	got evidence
			if ((msfPartName != null) && (msfPart != null)) {
				
				//	try with comparing most significant full part
				for (int t = (this.trayIndex - 1); t != -1; t--) {
					String testMsfPart = this.parent.trayAt(t).getPart(msfPartName);
					if ((testMsfPart != null) && testMsfPart.equals(msfPart)) {
						String part = this.parent.trayAt(t).getPart(partName);
						if ((part != null) && (part.length() > 2) && part.startsWith(partAbbreviation))
							return part;
					}
				}
				
				//	try with comparing most significant full part, allowing it to be abbreviated in tested tray
				for (int t = (this.trayIndex - 1); t != -1; t--) {
					String testMsfPart = this.parent.trayAt(t).getPart(msfPartName);
					if ((testMsfPart != null) && msfPart.startsWith(testMsfPart)) {
						String part = this.parent.trayAt(t).getPart(partName);
						if ((part != null) && (part.length() > 2) && part.startsWith(partAbbreviation))
							return part;
					}
				}
			}
			
			//	try simply with abbreviation
			for (int t = (this.trayIndex - 1); t != -1; t--) {
				String part = this.parent.trayAt(t).getPart(partName);
				if ((part != null) && (part.length() > 2) && part.startsWith(partAbbreviation))
					return part;
			}
			
			//	full form not found, return abbreviation
			return partAbbreviation;
		}
		
		private String getPart(String partName) {
			if (partName == null) return null;
			
			for (int p = 0; p < this.partTrays.size(); p++) {
				TaxonNamePartTray pt = ((TaxonNamePartTray) this.partTrays.get(p));
				if (partName.equals(pt.partOption.getSelectedItem())) {
					Object part = pt.completePart.getSelectedItem();
					if (part != null) return part.toString();
				}
			}
			
			return null;
		}
		
		private void commitChange() {
			
			String rank = null;
			for (int p = 0; p < this.partTrays.size(); p++) {
				TaxonNamePartTray pt = ((TaxonNamePartTray) this.partTrays.get(p));
				String option = pt.partOption.getSelectedItem().toString();
				
				if (!option.endsWith(AUTHOR_EXTENSION) && !option.endsWith(LABEL_EXTENSION)) {
					Object item = pt.completePart.getSelectedItem();
					String value = ((item == null) ? "" : item.toString().trim());
					if (value.length() != 0) {
						if (GENUS_ATTRIBUTE.equals(option)) {
							this.attributes.setProperty(option, value);
							rank = option;
						}
						else if (SUBGENUS_ATTRIBUTE.equals(option)) {
							this.attributes.setProperty(option, value);
							rank = option;
						}
						else if (SPECIES_ATTRIBUTE.equals(option)) {
							this.attributes.setProperty(option, value.toLowerCase());
							if (Gamta.isCapitalizedWord(value)) FAT.upperCaseLowerCaseParts.addElementIgnoreDuplicates(value);
							rank = option;
						}
						else if (SUBSPECIES_ATTRIBUTE.equals(option)) {
							this.attributes.setProperty(option, value.toLowerCase());
							if (Gamta.isCapitalizedWord(value)) FAT.upperCaseLowerCaseParts.addElementIgnoreDuplicates(value);
							rank = option;
						}
						else if (VARIETY_ATTRIBUTE.equals(option)) {
							this.attributes.setProperty(option, value.toLowerCase());
							if (Gamta.isCapitalizedWord(value)) FAT.upperCaseLowerCaseParts.addElementIgnoreDuplicates(value);
							rank = option;
						}
					}
					else this.attributes.remove(option);
				}
			}
			
			//	commit name edits
			if (!AnnotationUtils.equals(this.taxonName, this.bucket.taxonName, false)) {
				if (DEBUG) System.out.println("Changing Annotation " + this.bucket.taxonName.toXML() + " to " + this.taxonName.toXML());
				
				this.data.removeAnnotation(this.bucket.taxonName);
				this.bucket.taxonName = this.data.addAnnotation(this.taxonName);
				this.bucket.taxonNames.set(0, this.bucket.taxonName);
				
				for (int t = 1; t < this.bucket.taxonNames.size(); t++) { // start from 1, taxon name at 0 is bucket.taxonName
					this.data.removeAnnotation((Annotation) this.bucket.taxonNames.get(t));
					this.bucket.taxonNames.set(t, this.data.addAnnotation(this.taxonNames[t]));
				}
			}
			
			//	skip higher level taxon (rank is null)
			if (rank != null) for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
				Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
				taxonName.setAttribute(RANK_ATTRIBUTE, rank);
				
				for (int p = 0; p < PART_NAMES.length; p++) {
					String attribute = PART_NAMES[p];
					String value = this.attributes.getProperty(attribute);
					if (value == null) taxonName.removeAttribute(attribute);
					else taxonName.setAttribute(attribute, value);
				}
			}
		}
		
		private class TaxonNamePartTray {
			JComboBox partOption = new JComboBox(PARTS);
			String option = GENUS_PART;
//			JLabel partDisplay = new JLabel("", JLabel.LEFT);
			JComboBox completePart = new JComboBox();
			String part;
			int partIndex;
			TaxonNamePartTray(String part, int partIndex) {
				this.part = part;
				this.partIndex = partIndex;
				
				this.partOption.setBorder(BorderFactory.createLoweredBevelBorder());
				this.partOption.setEditable(false);
				this.partOption.addItemListener(new ItemListener () {
					public void itemStateChanged(ItemEvent ie) {
						if (!isInOptionChange && !option.equals(partOption.getSelectedItem()))
							doOptionChange(TaxonNamePartTray.this, ((PartCodePair) PARTS_TO_CODES.get(option)).code, ((PartCodePair) PARTS_TO_CODES.get(partOption.getSelectedItem())).code);
						//System.out.println("Part changed set from '" + option + "' to '" + partOption.getSelectedItem().toString() + "'");
						option = partOption.getSelectedItem().toString();
					}
				});
				
				this.completePart.setBorder(BorderFactory.createLoweredBevelBorder());
				this.completePart.setEditable(true);
				this.completePart.addItemListener(new ItemListener () {
					public void itemStateChanged(ItemEvent ie) {
						updateNameLabels();
					}
				});
			}
		}
		
		private void doOptionChange(TaxonNamePartTray source, final int oldOption, final int newOption) {
			this.isInOptionChange = true;
			
			if (DEBUG) System.out.println("Handeling option change from '" + oldOption + "' set to '" + newOption + "'");
			
			if ((oldOption % 3) == 1) { //	meaningful part
				String value = this.attributes.getProperty(CODES_TO_PARTS[oldOption].part);
				if ((value != null) && (newOption % 3) == 1)
					this.attributes.setProperty(CODES_TO_PARTS[newOption].part, value);
			}
			if ((oldOption % 3) == 0) { //	label
				if (oldOption == SUBSPECIES_LABEL_PART_CODE) this.subSpeciesLabel = null;
				else if (oldOption == VARIETY_LABEL_PART_CODE) this.varietyLabel = null;
			}
			this.attributes.remove(CODES_TO_PARTS[oldOption].part);
			
			//	clear attributes
			for (int p = (oldOption + 1); p < CODES_TO_PARTS.length; p++)
				if (CODES_TO_PARTS[p].part != null)
					this.attributes.remove(CODES_TO_PARTS[p].part);
			
			if ((newOption % 3) == 1) { //	meaningful part
				Object selected = source.completePart.getSelectedItem();
				this.attributes.setProperty(CODES_TO_PARTS[newOption].part, ((selected == null) ? source.part : selected.toString()));
			}
			else if ((newOption % 3) == 0) { //	label
				if (newOption == SUBSPECIES_LABEL_PART_CODE) this.subSpeciesLabel = source.part;
				else if (newOption == VARIETY_LABEL_PART_CODE) this.varietyLabel = source.part;
			}
			
			if (source.partIndex != -1) {
				int newOpt = newOption;
				
				//	set code
				this.partMeanings[source.partIndex] = newOpt;
				
				//	propagate change backward
				for (int p = (source.partIndex - 1); p >= 0; p--) {
					if (this.partMeanings[p] >= newOpt) {
						
						//	TODO: make re-assignment more sophisticated
						//	meaningful part or label, can exist only once
						if ((newOpt % 3) != 2) {
							newOpt--;
							if (CODES_TO_PARTS[newOpt].part == null)
								newOpt--; // skip non-existing part
						}
						
						this.partMeanings[p] = newOpt;
						
					} else break;
				}
				newOpt = newOption;
				
				//	propagate change forward
				String nextPart = GENUS_PART;
				if (newOption > 2) nextPart = SPECIES_PART;
				if (newOption > 5) nextPart = SUBSPECIES_PART;
				if (newOption > 8) nextPart = VARIETY_PART;
				
				this.setPartMeanings((source.partIndex + 1), nextPart);
			}
			
			if (DEBUG) {
				for (int i = 0; i < this.partMeanings.length; i++)
					System.out.println(" - " + this.parts[i] + " - " + this.partMeanings[i] + " (" + CODES_TO_PARTS[this.partMeanings[i]].part + ")");
			
				for (Iterator i = this.attributes.keySet().iterator(); i.hasNext();) {
					String attribute = i.next().toString();
					System.out.println(" - " + attribute + " = " + this.attributes.getProperty(attribute));
				}
			}
			
			this.producePartTrays();
			this.layoutParts();
			this.isInOptionChange = false;
			this.updateNameLabels();
		}
		
		private void layoutParts() {
			this.removeAll();
			
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
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 2;
			this.add(new JLabel("In Document:", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 1;
			
			String docNameLabelString;
			if (AnnotationUtils.equals(this.taxonName, this.bucket.taxonName, false))
				docNameLabelString = ("<HTML><B><I>" + this.taxonName.getValue() + "</I></B></HTML>");
			
			else {
				docNameLabelString = "<HTML><I>";
				for (int s = this.bucket.taxonName.getStartIndex(); s < this.taxonName.getStartIndex(); s++) {
					docNameLabelString += this.data.valueAt(s);
					docNameLabelString += this.data.getWhitespaceAfter(s);
				}
				docNameLabelString += ("<B>" + this.taxonName.getValue() + "</B>");
				for (int e = this.taxonName.getEndIndex(); e < this.bucket.taxonName.getEndIndex(); e++) {
					docNameLabelString += this.data.getWhitespaceAfter(e - 1);
					docNameLabelString += this.data.valueAt(e);
				}
				docNameLabelString += "</I></HTML>";
			}
			
			JLabel docNameLabel = new JLabel((docNameLabelString), JLabel.LEFT);
			docNameLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					String taxonNameString = getNameString(taxonName);
					if (taxonNameString.length() == 0)
						taxonNameString = taxonName.getValue();
					
					StringVector docNames = new StringVector();
					for (int t = 0; t < taxonNames.length; t++) {
						Object pageNumber = taxonNames[t].getAttribute(PAGE_NUMBER_ATTRIBUTE);
						docNames.addElementIgnoreDuplicates(buildLabel(data, taxonNames[t], 10) + " (at " + taxonNames[t].getStartIndex() + ((pageNumber == null) ? ", unknown page" : (" on page " + pageNumber)) + ")");
					}
					String message = ("<HTML>These taxonomic names in the document are normalized as \"" + taxonNameString + "\":<BR>&nbsp;&nbsp;&nbsp;");
					message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
					message += "</HTML>";
					JOptionPane.showMessageDialog(TaxonNameBucketTray.this, message, ("Forms Of \"" + taxonNameString + "\""), JOptionPane.INFORMATION_MESSAGE);
				}
			});
			this.add(docNameLabel, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.buttonPanel, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.changeLabel, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 1;
			this.add(this.normalizedNameLabel, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.remove, gbc.clone());
			gbc.gridy++;
			
			for (int p = 0; p < this.partTrays.size(); p++) {
				TaxonNamePartTray pt = ((TaxonNamePartTray) this.partTrays.get(p));
				
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				this.add(pt.partOption, gbc.clone());
				
				String option = pt.partOption.getSelectedItem().toString();
				if (option.endsWith(AUTHOR_EXTENSION) || option.endsWith(LABEL_EXTENSION)) {
					gbc.gridx = 1;
					gbc.weightx = 1;
					gbc.gridwidth = 2;
					this.add(new JLabel(pt.part, JLabel.LEFT), gbc.clone());
				}
				else {
					gbc.gridx = 1;
					gbc.weightx = 1;
					gbc.gridwidth = 1;
					this.add(pt.completePart, gbc.clone());
					
					gbc.gridx = 2;
					gbc.weightx = 0;
					gbc.gridwidth = 1;
					this.add(new JLabel(pt.part, JLabel.LEFT), gbc.clone());
				}
				gbc.gridy++;
			}
			
			this.validate();
			this.parent.validate();
			this.parent.repaint();
		}
		
		private void updateNameLabels() {
			if (this.isInOptionChange) return;
			
			String nameString = "";
			for (int p = 0; p < this.partTrays.size(); p++) {
				TaxonNamePartTray pt = ((TaxonNamePartTray) this.partTrays.get(p));
				
				String option = pt.partOption.getSelectedItem().toString();
				if (!option.endsWith(AUTHOR_EXTENSION) && !option.endsWith(LABEL_EXTENSION)) {
					Object item = pt.completePart.getSelectedItem();
					String value = ((item == null) ? "" : item.toString().trim());
					if (value.length() != 0) {
						if (GENUS_ATTRIBUTE.equals(option)) nameString = (nameString + " " + value);
						else if (SUBGENUS_ATTRIBUTE.equals(option)) nameString = (nameString + " (" + value + ")");
						else if (SPECIES_ATTRIBUTE.equals(option)) nameString = (nameString + " " + value);
						else if (SUBSPECIES_ATTRIBUTE.equals(option)) nameString = (nameString + " subsp. " + value);
						else if (VARIETY_ATTRIBUTE.equals(option)) nameString = (nameString + " var. " + value);
					}
				}
			}
			nameString = nameString.trim();
			
			if (this.bucket.taxonNameString.equals(nameString)){
				changeLabel.setText("Change to:");
				normalizedNameLabel.setText(nameString);
			}
			else {
				changeLabel.setText("<HTML><B>Change to:</B></HTML>");
				normalizedNameLabel.setText("<HTML><B>" + nameString + "</B></HTML>");
			}
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
}
