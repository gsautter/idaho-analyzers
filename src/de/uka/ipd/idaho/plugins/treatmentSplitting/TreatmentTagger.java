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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

public class TreatmentTagger extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	public static final String TREATMENT_ANNOTATION_TYPE = "treatment";
	
	private static final String treatment_start_OPTION = "treatment (start)";
	private static final String treatment_continued_OPTION = "treatment (continued)";
	private static final String no_treatment_OPTION = "not a treatment";
	
	private static final String[] SECTION_TYPES = {
		treatment_start_OPTION,
		treatment_continued_OPTION,
		no_treatment_OPTION
	};
	
	private static final String multiple_DIV_TYPE = "multiple";
	
	private static final String ARTIFACT_OPTION = "ARTIFACT";
	
	private StringVector numberPrefixes;
	private StringVector artifactAnnotationTypes = new StringVector();
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get existing treatments and non-treatment subSubSections
		ArrayList existingStructureCollector = new ArrayList();
		Annotation[] treatments = data.getAnnotations(TREATMENT_ANNOTATION_TYPE);
		for (int t = 0; t < treatments.length; t++) existingStructureCollector.add(treatments[t]);
		Annotation[] nonTreatmentDivs = data.getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
		for (int d = 0; d < nonTreatmentDivs.length; d++) existingStructureCollector.add(nonTreatmentDivs[d]);
		Annotation[] existingStructure = ((Annotation[]) existingStructureCollector.toArray(new Annotation[existingStructureCollector.size()]));
		Arrays.sort(existingStructure, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		int esIndex = 0;
		
		//	group paragraphs to treatments and other contents
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		if (paragraphs.length != 0) {
			Properties paragraphOptions = new Properties();
			
			//	create paragraph trays
			ArrayList paragraphBoxList = new ArrayList();
			boolean gotFirstTreatment = false;
			boolean lastMayBeCatalogueEntry = false;
			for (int p = 0; p < paragraphs.length; p++) {
				
				//	check if layout artifact
				boolean isArtifact = false;
				for (int a = 0; a < this.artifactAnnotationTypes.size(); a++) {
					Annotation[] artifacts = paragraphs[p].getAnnotations(this.artifactAnnotationTypes.get(a));
					if ((artifacts.length != 0) && (artifacts[0].size() == paragraphs[p].size()))
						isArtifact = true;
				}
				
				//	layout artifact, exclude from dialog
				if (isArtifact) {
					paragraphOptions.setProperty(paragraphs[p].getAnnotationID(), ARTIFACT_OPTION);
					
				//	put non-artifact paragraph on tray
				} else {
					
					DsBox box = new DsBox(paragraphs[p]);
					
					//	jump to treatment that might contain current paragraph
					while ((esIndex < existingStructure.length) && (existingStructure[esIndex].getEndIndex() < paragraphs[p].getEndIndex())) esIndex++;
					
					//	check if paragraph lies in existing higher order structure
					if ((esIndex < existingStructure.length) && AnnotationUtils.liesIn(paragraphs[p], existingStructure[esIndex])) {
						lastMayBeCatalogueEntry = false;
						
						//	paragraph lies in existing treatment
						if (TREATMENT_ANNOTATION_TYPE.equals(existingStructure[esIndex].getType())) {
							gotFirstTreatment = true;
							if (existingStructure[esIndex].getStartIndex() == paragraphs[p].getStartIndex())
								box.setSectionType(treatment_start_OPTION);
							else box.setSectionType(treatment_continued_OPTION);
							
						//	paragraph lies in existing non-treatment subSubSection
						} else box.setSectionType(no_treatment_OPTION);
						
					//	paragraph does not lie in existing higher order structure, needs to be classified
					} else {
						
						//	gather "hard" evidence
						Annotation[] taxonomicNames = paragraphs[p].getAnnotations("taxonomicName");
						Annotation[] taxonomicNameLabels = paragraphs[p].getAnnotations("taxonomicNameLabel");
						
						//	find first word
						int firstWordIndex = 0;
						while ((firstWordIndex < paragraphs[p].size()) && (!Gamta.isWord(paragraphs[p].valueAt(firstWordIndex)) || this.numberPrefixes.containsIgnoreCase(paragraphs[p].valueAt(firstWordIndex))))
							firstWordIndex++;
						if (firstWordIndex == paragraphs[p].size()) firstWordIndex = -1;
						
						//	nomenclature section
						if ((taxonomicNames.length != 0) && ((taxonomicNames[0].getStartIndex() <= firstWordIndex) || (taxonomicNameLabels.length != 0)) && !Gamta.isLowerCaseWord(taxonomicNames[0].firstValue())) {
							gotFirstTreatment = true;
							box.setSectionType(treatment_start_OPTION);
							
							//	start of treatment directly after possible catalogue entry
							if (lastMayBeCatalogueEntry && !paragraphBoxList.isEmpty()) {
								DsBox lastBox = ((DsBox) paragraphBoxList.get(paragraphBoxList.size() - 1));
								lastBox.setSectionType(no_treatment_OPTION);
							}
							
							//	determine if current paragraph might be a catalogue entry
							lastMayBeCatalogueEntry = false;
							if (taxonomicNames.length == 1) {
								int taxonomicNameTokenCount = taxonomicNames[0].size();
								
								if (taxonomicNameLabels.length != 0)
									taxonomicNameTokenCount += taxonomicNameLabels[0].size();
								
								if ((3 * taxonomicNameTokenCount) > (2 * paragraphs[p].size()))
									lastMayBeCatalogueEntry = true;
							}
							
						//	treatment continues
						} else if (gotFirstTreatment) {
							lastMayBeCatalogueEntry = false;
							box.setSectionType(treatment_continued_OPTION);
							
						//	paragraph before start of first treatment
						} else {
							lastMayBeCatalogueEntry = false;
							box.setSectionType(no_treatment_OPTION);
						}
					}
					
					paragraphBoxList.add(box);
				}
			}
			
			//	put paragraph boxes in array
			DsBox[] paragraphBoxes = ((DsBox[]) paragraphBoxList.toArray(new DsBox[paragraphBoxList.size()]));
			
			//	get feedback if allowed to do so
			if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
				
				//	build dialog
				final JDialog td = new JDialog(((JFrame) null), "Check Document Structure", true);
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
				
				//	avoid 512-bug in GridBagLayout
				if (paragraphBoxes.length > 500) {
					System.out.println("TreatmentTagger: Got " + paragraphBoxes.length + " paragraphs, using sub panels to avoid Java 512 bug.");
					
					int subPanelSize = ((paragraphBoxes.length / 500) + 1);
					JPanel subPanel = new JPanel(new GridBagLayout());
					GridBagConstraints subPanelGbc = new GridBagConstraints();
					subPanelGbc.insets.top = 2;
					subPanelGbc.insets.bottom = 2;
					subPanelGbc.insets.left = 5;
					subPanelGbc.insets.right = 5;
					subPanelGbc.fill = GridBagConstraints.HORIZONTAL;
					subPanelGbc.weightx = 1;
					subPanelGbc.weighty = 0;
					subPanelGbc.gridwidth = 1;
					subPanelGbc.gridheight = 1;
					subPanelGbc.gridx = 0;
					subPanelGbc.gridy = 0;
					
					//	add classification boxes
					for (int p = 0; p < paragraphBoxes.length; p++) {
						paragraphBoxes[p].position = p;
						paragraphBoxes[p].boxes = paragraphBoxes;
						subPanel.add(paragraphBoxes[p], subPanelGbc.clone());
						subPanelGbc.gridy++;
						
						//	sub panel full
						if (subPanelGbc.gridy == subPanelSize) {
							panel.add(subPanel, gbc.clone());
							gbc.gridy++;
							subPanelGbc.gridy = 0;
							subPanel = new JPanel(new GridBagLayout());
						}
					}
					
					//	add last sub panel
					if (subPanelGbc.gridy != 0) {
						panel.add(subPanel, gbc.clone());
						gbc.gridy++;
					}
					
				} else {
					
					//	add classification boxes
					for (int p = 0; p < paragraphBoxes.length; p++) {
						paragraphBoxes[p].position = p;
						paragraphBoxes[p].boxes = paragraphBoxes;
						panel.add(paragraphBoxes[p], gbc.clone());
						gbc.gridy++;
					}
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
				td.setSize(500, (Math.min(650, (60 * paragraphs.length) + 50)));
				td.setLocationRelativeTo(null);
				td.setVisible(true);
			}
			
			//	process feedback
			for (int p = 0; p < paragraphBoxes.length; p++)
				paragraphOptions.setProperty(paragraphBoxes[p].paragraph.getAnnotationID(), paragraphBoxes[p].getSectionType());
			
			//	inherit section types to artifact paragraphs
			String lastOption = null;
			for (int p = 0; p < paragraphs.length; p++) {
				String option = paragraphOptions.getProperty(paragraphs[p].getAnnotationID());
				if (ARTIFACT_OPTION.equals(option)) {
					if (lastOption == null)
						paragraphOptions.setProperty(paragraphs[p].getAnnotationID(), no_treatment_OPTION);
					
					//	look ahead exclude artifacts at end of treatments
					else {
						String nextOption = ARTIFACT_OPTION;
						int lookahead = 1;
						while (((p + lookahead) < paragraphs.length) && ARTIFACT_OPTION.equals(nextOption)) {
							nextOption = paragraphOptions.getProperty(paragraphs[p + lookahead].getAnnotationID());
							lookahead ++;
						}
						if (treatment_start_OPTION.equals(nextOption))
							paragraphOptions.setProperty(paragraphs[p].getAnnotationID(), no_treatment_OPTION);
						else paragraphOptions.setProperty(paragraphs[p].getAnnotationID(), lastOption);
						
					}
				} else lastOption = option;
			}
			
			//	create treatment structure
			int treatmentStart = -1;
			int nTreatmentStart = -1;
			for (int p = 0; p < paragraphs.length; p++) {
				String sectionOption = paragraphOptions.getProperty(paragraphs[p].getAnnotationID());
				
				//	process treatment
				if (treatment_start_OPTION.equals(sectionOption)) {
					
					//	mark treatment if one open
					if (treatmentStart != -1)
						data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (paragraphs[p].getStartIndex() - treatmentStart));
					
					//	mark non-treatment subSubSection if one open
					if (nTreatmentStart != -1)
						data.addAnnotation(MutableAnnotation.SUB_SECTION_TYPE, nTreatmentStart, (paragraphs[p].getStartIndex() - nTreatmentStart)).setAttribute(TYPE_ATTRIBUTE, multiple_DIV_TYPE);
					
					//	remember start of treatment
					treatmentStart = paragraphs[p].getStartIndex();
					nTreatmentStart = -1;
					
				} else if (treatment_continued_OPTION.equals(sectionOption)) {
					//	just go on
					
				} else {
					
					//	mark treatment if one open
					if (treatmentStart != -1)
						data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (paragraphs[p].getStartIndex() - treatmentStart));
					
					//	remember no treatment open
					treatmentStart = -1;
					nTreatmentStart = paragraphs[p].getStartIndex();
				}
			}
			
			//	mark last treatment
			if (treatmentStart != -1)
				data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (data.size() - treatmentStart));
			
			//	mark non-treatment subSubSection if one open
			if (nTreatmentStart != -1)
				data.addAnnotation(MutableAnnotation.SUB_SECTION_TYPE, nTreatmentStart, (data.size() - nTreatmentStart)).setAttribute(TYPE_ATTRIBUTE, multiple_DIV_TYPE);
			
			//	remove duplicate treatments
			AnnotationFilter.removeDuplicates(data, TREATMENT_ANNOTATION_TYPE);
		}
	}
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load number prefixes
		try {
			InputStream is = this.dataProvider.getInputStream("numberPrefixes.txt");
			this.numberPrefixes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {
			this.numberPrefixes = new StringVector();
			this.numberPrefixes.addElement("No");
		}
		
		//	load artifact div types
		try {
			InputStream is = this.dataProvider.getInputStream("artifactAnnotationTypes.txt");
			this.artifactAnnotationTypes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
	}
	
	private class DsBox extends JPanel {
		
		JComboBox sectionTypeSelector;
		String sectionType;
		
		JPanel topLabel = new JPanel();
		JPanel bottomLabel = new JPanel();
		
		private MutableAnnotation paragraph;
		private DsBox[] boxes;
		private int position;
		
		public DsBox(MutableAnnotation data) {
			super(new GridBagLayout(), true);
			this.paragraph = data;
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			final JTextArea display = new JTextArea();
			display.setText(this.paragraph.getValue());
			display.setLineWrap(true);
			display.setWrapStyleWord(true);
			display.setEditable(false);
			JScrollPane displayBox = new JScrollPane(display);
			
			this.sectionTypeSelector = new JComboBox(SECTION_TYPES);
			this.sectionTypeSelector.setEditable(false);
			this.sectionTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.sectionTypeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String newSectionType = sectionTypeSelector.getSelectedItem().toString();
					cascadeSectionTypeChange(sectionType, newSectionType);
					
					if (treatment_start_OPTION.equals(newSectionType)) {
						topLabel.setBackground(null);
						bottomLabel.setBackground(Color.BLUE);
					} else if (treatment_continued_OPTION.equals(newSectionType)) {
						topLabel.setBackground(Color.BLUE);
						bottomLabel.setBackground(Color.BLUE);
					} else {
						topLabel.setBackground(null);
						bottomLabel.setBackground(null);
					}
					topLabel.validate();
					bottomLabel.validate();
					
					sectionType = newSectionType;
				}
			});
			
			String pageNumber = this.paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString().trim();
			
			this.topLabel.setBackground(null);
			this.bottomLabel.setBackground(Color.BLUE);
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.weighty = 1;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridy = 0;
			this.add(this.sectionTypeSelector, gbc.clone());
			gbc.gridy = 1;
			if (pageNumber.length() == 0)
				this.add(new JLabel("<Unknown Page>"), gbc.clone());
			else this.add(new JLabel("Page " + pageNumber), gbc.clone());
			
			gbc.gridx = 1;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			this.add(this.topLabel, gbc.clone());
			gbc.gridy = 1;
			this.add(this.bottomLabel, gbc.clone());
			
			gbc.gridx = 2;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 3;
			gbc.gridheight = 2;
			gbc.gridy = 0;
			this.add(displayBox, gbc.clone());
			
			this.setPreferredSize(new Dimension(450, 60));
			this.sectionType = this.sectionTypeSelector.getSelectedItem().toString();
		}
		
		void cascadeSectionTypeChange(String oldType, String newType) {
			if (this.boxes == null) return;
			
			if (((this.position + 1) < this.boxes.length) && (this.boxes[this.position + 1] != null)) {
				DsBox nextBox = this.boxes[this.position + 1];
				String nextType = nextBox.getSectionType();
				if (treatment_start_OPTION.equals(newType)) {
					nextBox.setSectionType(treatment_continued_OPTION);
				} else if (treatment_continued_OPTION.equals(newType)) {
					if (!treatment_start_OPTION.equals(nextType))
						nextBox.setSectionType(treatment_continued_OPTION);
				} else if (no_treatment_OPTION.equals(newType)) {
					if (!treatment_start_OPTION.equals(nextType))
						nextBox.setSectionType(no_treatment_OPTION);
				}
			}
		}
		
		String getSectionType() {
			return this.sectionTypeSelector.getSelectedItem().toString();
		}
		
		void setSectionType(String value) {
			this.sectionTypeSelector.setSelectedItem(value);
		}
	}
}
