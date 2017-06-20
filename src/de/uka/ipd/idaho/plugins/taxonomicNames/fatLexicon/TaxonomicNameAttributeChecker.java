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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TaxonomicNameAttributeChecker extends AbstractAnalyzer implements TaxonomicNameConstants {
	private static final String[] PART_NAMES = {GENUS_ATTRIBUTE, SUBGENUS_ATTRIBUTE, SPECIES_ATTRIBUTE, SUBSPECIES_ATTRIBUTE, VARIETY_ATTRIBUTE};
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	check parameter
		if ((data == null) || !parameters.containsKey(INTERACTIVE_PARAMETER)) return;
		
		//	get taxonomic names
		Annotation[] taxonomicNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		
		//	filter out names with above-ganus rank, they are bullet-proof
		ArrayList genusOrBelowList = new ArrayList();
		for (int t = 0; t < taxonomicNames.length; t++) {
			if ((taxonomicNames[t].size() == 1) && Gamta.isFirstLetterUpWord(taxonomicNames[t].firstValue())) {
				String name = Gamta.capitalize(taxonomicNames[t].firstValue());
				if (!name.endsWith("idae") && !name.endsWith("inae") && !name.endsWith("ini"))
					genusOrBelowList.add(taxonomicNames[t]);
			} else genusOrBelowList.add(taxonomicNames[t]);
		}
		taxonomicNames = ((Annotation[]) genusOrBelowList.toArray(new Annotation[genusOrBelowList.size()]));
		
		//	bucketize taxonomic names, and collect parts
		StringVector bucketNames = new StringVector();
		HashMap bucketsByName = new HashMap();
		
		StringVector genusList = new StringVector();
		StringVector genusSubGenusList = new StringVector();
		StringVector lowerCasePartList = new StringVector();
		
		for (int t = 0; t < taxonomicNames.length; t++) {
			String taxonNameKey = this.getNameString(taxonomicNames[t]);
			taxonNameKey += taxonomicNames[t].getValue();
//			if (taxonNameKey.length() == 0) taxonNameKey = taxonomicNames[t].getValue();
			if (bucketsByName.containsKey(taxonNameKey)) {
				((TaxonNameBucket) bucketsByName.get(taxonNameKey)).taxonNames.add(taxonomicNames[t]);
			} else {
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
//		Collections.sort(bucketList);
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
			final JDialog td = new JDialog(((JFrame) null), "Please Check Attributes of Taxon Names", true);
			JPanel panel = new JPanel(new GridBagLayout());
			JScrollPane panelBox = new JScrollPane(panel);
			panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			panelBox.getVerticalScrollBar().setUnitIncrement(50);
			td.getContentPane().setLayout(new BorderLayout());
			td.getContentPane().add(panelBox, BorderLayout.CENTER);
			
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
			
			//	add classification boxes
			TaxonBucketTray[] trays = new TaxonBucketTray[buckets.length];
			for (int b = 0; b < buckets.length; b++) {
				trays[b] = new TaxonBucketTray(buckets[b], genera, generaSubGenera, lowerCaseParts, data);
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
			td.getContentPane().add(okButton, BorderLayout.SOUTH);
			
			//	ensure dialog is closed with button
			td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			//	get feedback
			td.setSize(500, (Math.min(650, (140 * buckets.length) + 50)));
			td.setLocationRelativeTo(null);
			td.setVisible(true);
			
			//	process feedback
			for (int t = 0; t < trays.length; t++) {
				if (trays[t].remove.isSelected()) {
					for (int a = 0; a < trays[t].bucket.taxonNames.size(); a++)
						data.removeAnnotation((Annotation) trays[t].bucket.taxonNames.get(a));
				} else trays[t].commitChange();
			}
		}
	}
	
	private static final String MISSING_PART = "####";
	
	private String getNameString(Annotation taxonomicName) {
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
	
	private class TaxonNameBucket implements Comparable {
		private String nameString;
		private Annotation taxonName;
		private ArrayList taxonNames = new ArrayList();
		TaxonNameBucket(Annotation taxonName) {
			this.nameString = getNameString(taxonName);
			if (this.nameString.length() == 0) this.nameString = taxonName.getValue();
			this.taxonName = taxonName;
			this.taxonNames.add(taxonName);
		}
		public int compareTo(Object o) {
			if (o instanceof TaxonNameBucket) return this.nameString.compareTo(((TaxonNameBucket) o).nameString);
			else return -1;
		}
	}
	
	private class TaxonBucketTray extends JPanel {
		private TaxonNameBucket bucket;
		private Properties nameParts = new Properties();
		private HashMap fieldsByName = new HashMap();
		private JLabel changeLabel;
		private JLabel normalizedNameLabel;
		private JCheckBox remove = new JCheckBox("Remove");
		
		TaxonBucketTray(final TaxonNameBucket bucket, String[] genera, String[] generaSubGenera,  String[] lowerCaseParts, final TokenSequence text) {
			super(new BorderLayout(), true);
			this.bucket = bucket;
			
			for (int p = 0; p < PART_NAMES.length; p++) {
				String attribute = PART_NAMES[p];
				String value = this.bucket.taxonName.getAttribute(attribute, "").toString();
				if (value.length() != 0) this.nameParts.setProperty(attribute, value);
			}
			
			JPanel panel = new JPanel(new GridBagLayout());
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
			panel.add(new JLabel("In Document:", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			JLabel docNameLabel = new JLabel(("<HTML><B><I>" + bucket.taxonName.getValue() + "</I></B></HTML>"), JLabel.LEFT);
			docNameLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					StringVector docNames = new StringVector();
					for (int a = 0; a < bucket.taxonNames.size(); a++) {
						Annotation taxonName = ((Annotation) bucket.taxonNames.get(a));
						docNames.addElementIgnoreDuplicates(buildLabel(text, taxonName, 10) + " (at " + taxonName.getStartIndex() + ")");
					}
					String message = ("<HTML>These taxonomic names in the document are normalized as \"" + bucket.nameString + "\":<BR>&nbsp;&nbsp;&nbsp;");
					message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
					message += "</HTML>";
					JOptionPane.showMessageDialog(TaxonBucketTray.this, message, ("Forms Of \"" + bucket.nameString + "\""), JOptionPane.INFORMATION_MESSAGE);
				}
			});
			panel.add(docNameLabel, gbc.clone());
			gbc.gridy++;
			
			this.changeLabel = new JLabel("Change to:", JLabel.LEFT);
			this.normalizedNameLabel = new JLabel(this.bucket.nameString, JLabel.LEFT);
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			panel.add(this.changeLabel, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 1;
			panel.add(this.normalizedNameLabel, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			panel.add(this.remove, gbc.clone());
			gbc.gridy++;
			
			for (int p = 0; p < PART_NAMES.length; p++) {
				String attribute = PART_NAMES[p];
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				panel.add(new JLabel(attribute, JLabel.LEFT), gbc.clone());
				
				JComboBox field;
				if (GENUS_ATTRIBUTE.equals(attribute)) field = new JComboBox(genera);
				else if (SUBGENUS_ATTRIBUTE.equals(attribute)) field = new JComboBox(generaSubGenera);
				else field = new JComboBox(lowerCaseParts);
				fieldsByName.put(attribute, field);
				field.setBorder(BorderFactory.createLoweredBevelBorder());
				field.setEditable(true);
				field.setSelectedItem(this.nameParts.getProperty(attribute, ""));
				field.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						String nameString = "";
						for (int p = PART_NAMES.length; p > 0; p--) {
							String attribute = PART_NAMES[p - 1];
							JComboBox field = ((JComboBox) fieldsByName.get(attribute));
							Object item = field.getSelectedItem();
							String value = ((item == null) ? "" : item.toString().trim());
							if (value.length() != 0) {
								if (p == 5) nameString = ("var. " + value + " " + nameString);
								else if (p == 4) nameString = ("subsp. " + value + " " + nameString);
								else if (p == 3) nameString = (value + " " + nameString);
								else if (p == 2) nameString = ("(" + value + ") " + nameString);
								else if (p == 1) nameString = (value + " " + nameString);
							}
						}
						nameString = nameString.trim();
						if (bucket.nameString.equals(nameString)){
							changeLabel.setText("Change to:");
							normalizedNameLabel.setText(nameString);
						} else {
							changeLabel.setText("<HTML><B>Change to:</B></HTML>");
							normalizedNameLabel.setText("<HTML><B>" + nameString + "</B></HTML>");
						}
					}
				});
				
				gbc.gridx = 1;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				panel.add(field, gbc.clone());
				gbc.gridy++;
			}
			
			this.setBorder(BorderFactory.createEtchedBorder());
//			this.add(new JLabel("<HTML><B><I>" + bucket.nameString + "</I></B></HTML>"), BorderLayout.NORTH);
			this.add(panel, BorderLayout.CENTER);
		}
		
		void commitChange() {
			
			//	read values and determine rank
			String rank = null;
			for (int p = PART_NAMES.length; p > 0; p--) {
				String attribute = PART_NAMES[p - 1];
				JComboBox field = ((JComboBox) fieldsByName.get(attribute));
				Object item = field.getSelectedItem();
				String value = ((item == null) ? "" : item.toString().trim());
				if (value.length() == 0) this.nameParts.remove(attribute);
				else {
					this.nameParts.setProperty(attribute, value);
					if (rank == null) rank = attribute;
				}
			}
			
			//	skip higher level taxon (rank is null)
			if (rank != null) for (int t = 0; t < this.bucket.taxonNames.size(); t++) {
				Annotation taxonName = ((Annotation) this.bucket.taxonNames.get(t));
				taxonName.setAttribute(RANK_ATTRIBUTE, rank);
				for (int p = 0; p < PART_NAMES.length; p++) {
					String attribute = PART_NAMES[p];
					String value = this.nameParts.getProperty(attribute);
					if (value == null) taxonName.removeAttribute(attribute);
					else taxonName.setAttribute(attribute, value);
				}
			}
		}
	}
	
	private String buildLabel(TokenSequence text, Annotation annot, int envSize) {
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
