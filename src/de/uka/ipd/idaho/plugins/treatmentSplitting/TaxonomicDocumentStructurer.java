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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

public class TaxonomicDocumentStructurer extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	public static final String TREATMENT_ANNOTATION_TYPE = "treatment";
	public static final String TYPE_ATTRIBUTE = "type";
	
	//	higher level div-types according to TaxonX, treatment options, and TaxonX elements on same hierarchy
	static final String abstract_OPTION = "abstract";
	static final String acknowledgments_OPTION = "acknowledgments";
	static final String catalogue_entry_OPTION = "catalogue entry";
	static final String document_head_OPTION = "document_head";
	static final String introduction_OPTION = "introduction";
	static final String key_OPTION = "key";
	static final String materials_methods_OPTION = "materials_methods";
	static final String multiple_OPTION = "multiple";
	static final String reference_group_OPTION = "reference_group";
	static final String synopsis_OPTION = "synopsis";
	static final String synonymic_list_OPTION = "synonymic_list";
	static final String treatment_start_OPTION = "treatment (start)";
	static final String treatment_continued_OPTION = "treatment (continued)";
	
	//	in-treatment div-types according to TaxonX
	static final String biology_ecology_DIV_TYPE = "biology_ecology";
	static final String description_DIV_TYPE = "description";
	static final String diagnosis_DIV_TYPE = "diagnosis";
	static final String discussion_DIV_TYPE = "discussion";
	static final String distribution_DIV_TYPE = "distribution";
	static final String etymology_DIV_TYPE = "etymology";
	static final String materials_examined_DIV_TYPE = "materials_examined";
	
	//	nomenclature is not a div in TaxonX
	static final String nomenclature_DIV_TYPE = "nomenclature";
	
	//	document head is not a div in TaxonX
	static final String document_title_DIV_TYPE = "document title";
	static final String document_author_DIV_TYPE = "document author";
	
	//	reference group is not a div in TaxonX
	static final String reference_group_DIV_TYPE = "reference_group";
	static final String reference_DIV_TYPE = "reference";
	
	static final String other_DIV_TYPE = "other / general";
	
	static final String[] SECTION_TYPES = {
			treatment_start_OPTION,
			treatment_continued_OPTION,
			catalogue_entry_OPTION,
			abstract_OPTION,
			acknowledgments_OPTION,
			document_head_OPTION,
			introduction_OPTION,
			key_OPTION,
			materials_methods_OPTION,
			multiple_OPTION,
			reference_group_OPTION,
			synopsis_OPTION,
			synonymic_list_OPTION
		};
	
	//	option arrays
	static final String[] TREATMENT_DIV_TYPES = {
			biology_ecology_DIV_TYPE,
			description_DIV_TYPE,
			diagnosis_DIV_TYPE,
			discussion_DIV_TYPE,
			distribution_DIV_TYPE,
			etymology_DIV_TYPE,
			materials_examined_DIV_TYPE,
			nomenclature_DIV_TYPE,
			reference_group_DIV_TYPE,
			synonymic_list_OPTION
		};
	
	static final String[] CATALOGUE_ENTRY_DIV_TYPES = {
			nomenclature_DIV_TYPE
		};

	static final String[] HEAD_DIV_TYPES = {
			document_author_DIV_TYPE,
			document_title_DIV_TYPE,
			other_DIV_TYPE
		};
	
	static final String[] REF_GROUP_DIV_TYPES = {
			reference_DIV_TYPE,
			other_DIV_TYPE
		};
	
	static final String[] DEFAULT_DIV_TYPES = {
			other_DIV_TYPE
		};
	
	static final int MIN_ABSTRACT_SIZE = 50;
	static final int MAX_ABSTRACT_SIZE = 150;
	
	static final String DIV_TYPE_ATTRIBUTE = "divType";
	static final String SECTION_TYPE_ATTRIBUTE = "sectionType";
	
	private StringVector numberPrefixes;
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	group paragraphs to treatments and other contents
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		if (paragraphs.length != 0) {
			
			//	create paragraph trays
			DsBox[] paragraphBoxes = new DsBox[paragraphs.length];
			boolean gotFirstTreatment = false;
			boolean gotAbstract = false;
			for (int p = 0; p < paragraphs.length; p++) {
				DsBox box = new DsBox(paragraphs[p], paragraphBoxes, p);
				paragraphBoxes[p] = box;
				
				//	gather "hard" evidence
				Annotation[] taxonomicNames = paragraphs[p].getAnnotations("taxonomicName");
				Annotation[] taxonomicNameLabels = paragraphs[p].getAnnotations("taxonomicNameLabel");
				Annotation[] citations = paragraphs[p].getAnnotations(CITATION_TYPE);
				
				//	find first word
				int firstWordIndex = 0;
				while ((firstWordIndex < paragraphs[p].size()) && (!Gamta.isWord(paragraphs[p].tokenAt(firstWordIndex)) || this.numberPrefixes.containsIgnoreCase(paragraphs[p].valueAt(firstWordIndex))))
					firstWordIndex++;
				if (firstWordIndex == paragraphs[p].size()) firstWordIndex = -1;
				
				//	nomenclature section
				if ((taxonomicNames.length != 0) && ((taxonomicNames[0].getStartIndex() <= firstWordIndex) || (taxonomicNameLabels.length != 0))) {
					gotFirstTreatment = true;
					
					box.setSectionType(treatment_start_OPTION);
					box.setDivType(nomenclature_DIV_TYPE);
					
					//	set previous paragraph to collection event if it's also nomenclature
					if ((p != 0) && treatment_start_OPTION.equals(paragraphBoxes[p-1].getSectionType()))
						paragraphBoxes[p-1].setSectionType(catalogue_entry_OPTION);
					
				//	reference group
				} else if (citations.length != 0) {
					box.setSectionType(gotFirstTreatment ? treatment_continued_OPTION : reference_group_DIV_TYPE);
					box.setDivType(reference_group_DIV_TYPE);
					
				//	treatment continues
				} else if (gotFirstTreatment) {
					
					//	set option to continued
					box.setSectionType(treatment_continued_OPTION);
					
					//	classify paragraphs (learns from user's classification)
					box.setDivType(description_DIV_TYPE); // have to pre-set type so color will be set
					box.setDivType(this.classifyTreatmentPart(paragraphs[p]));
					
				//	paragraph before start of first treatment
				} else {
					box.setSectionType(document_head_OPTION);
					box.setDivType(multiple_OPTION); // have to pre-set type so color will be set
					
					//	introduction (after abstract)
					if (gotAbstract) {
						box.setSectionType(introduction_OPTION);
						
					//	too small for abstract
					} else if (paragraphs[p].size() < MIN_ABSTRACT_SIZE) {
						box.setSectionType(document_head_OPTION);
						
					//	abstract or introduction, depending on size
					} else {
						box.setDivType((paragraphs[p].size() < MAX_ABSTRACT_SIZE) ? abstract_OPTION : introduction_OPTION);
						gotAbstract = true;
					}
				}
			}
			
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
				
				//	add classification boxes
				for (int p = 0; p < paragraphBoxes.length; p++) {
					panel.add(paragraphBoxes[p], gbc.clone());
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
				td.setSize(500, (Math.min(650, (60 * paragraphs.length) + 50)));
				td.setLocationRelativeTo(null);
				td.setVisible(true);
				
				//	remember classifications
				for (int p = 0; p < paragraphs.length; p++)
					this.rememberClassification(paragraphs[p], paragraphBoxes[p].getDivType());
			}
			
			//	create treatment structure
			int treatmentStart = -1;
			for (int p = 0; p < paragraphs.length; p++) {
				String sectionOption = paragraphBoxes[p].getSectionType();
				
				//	process treatment
				if (treatment_start_OPTION.equals(sectionOption)) {
					
					//	mark treatment if one open
					if (treatmentStart != -1)
						data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (paragraphs[p].getStartIndex() - treatmentStart));
					
					//	remember start of treatment
					treatmentStart = paragraphs[p].getStartIndex();
					
				} else if (treatment_continued_OPTION.equals(sectionOption)) {
					//	just go on
					
				} else if (catalogue_entry_OPTION.equals(sectionOption)) {
					
					//	mark treatment if one open
					if (treatmentStart != -1)
						data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (paragraphs[p].getStartIndex() - treatmentStart));
					
					//	mark collection event treatment
					data.addAnnotation(TREATMENT_ANNOTATION_TYPE, paragraphs[p].getStartIndex(), paragraphs[p].size());
					
					//	remember no treatment open
					treatmentStart = -1;
					
				} else {
					
					//	mark treatment if one open
					if (treatmentStart != -1)
						data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (paragraphs[p].getStartIndex() - treatmentStart));
					
					//	remember no treatment open
					treatmentStart = -1;
					
					//	assign section type to paragraph
					paragraphs[p].setAttribute(SECTION_TYPE_ATTRIBUTE, sectionOption);
				}
				
				//	assign div type to paragraph
				paragraphs[p].setAttribute(DIV_TYPE_ATTRIBUTE, paragraphBoxes[p].getDivType());
			}
			
			//	mark last treatment
			if (treatmentStart != -1)
				data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentStart, (data.size() - treatmentStart));
			
			
			//	create div structure in treatments
			MutableAnnotation[] treatments = data.getMutableAnnotations(TREATMENT_ANNOTATION_TYPE);
			for (int t = 0; t < treatments.length; t++) {
				int divStart = -1;
				String openDivType = null;
				paragraphs = treatments[t].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < paragraphs.length; p++) {
					String divType = paragraphs[p].getAttribute(DIV_TYPE_ATTRIBUTE, "").toString();
					paragraphs[p].removeAttribute(DIV_TYPE_ATTRIBUTE);
					
					//	start of new div
					if (!divType.equals(openDivType)) {
						
						//	mark div if one open
						if (divStart != -1) {
							MutableAnnotation div = treatments[t].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, divStart, (paragraphs[p].getStartIndex() - divStart));
							div.setAttribute(TYPE_ATTRIBUTE, openDivType);
						}
						
						//	remember start and type of new div
						divStart = paragraphs[p].getStartIndex();
						openDivType = divType;
					}
				}
				
				//	mark last div if one open
				if (openDivType != null) {
					MutableAnnotation div = treatments[t].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, divStart, (treatments[t].size() - divStart));
					div.setAttribute(TYPE_ATTRIBUTE, openDivType);
				}
			}
			
			//	create divs between treatments
			paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			int sectionStart = -1;
			String openSectionType = null;
			for (int p = 0; p < paragraphs.length; p++) {
				
				//	paragraph still to assign
				if (paragraphs[p].hasAttribute(SECTION_TYPE_ATTRIBUTE)) {
					String sectionType = paragraphs[p].getAttribute(SECTION_TYPE_ATTRIBUTE, "").toString();
					paragraphs[p].removeAttribute(SECTION_TYPE_ATTRIBUTE);
					
					//	do special section internal markup if given
					String divType = paragraphs[p].getAttribute(DIV_TYPE_ATTRIBUTE, "").toString();
					paragraphs[p].removeAttribute(DIV_TYPE_ATTRIBUTE);
					
					if (document_author_DIV_TYPE.equals(divType)) {
						paragraphs[p].addAnnotation("author", 0, paragraphs[p].size());
						
					} else if (document_title_DIV_TYPE.equals(divType)) {
						paragraphs[p].addAnnotation("title", 0, paragraphs[p].size());
						
					} else if (reference_DIV_TYPE.equals(divType)) {
						Annotation[] citations = paragraphs[p].getAnnotations(CITATION_TYPE);
						if (citations.length == 0)
							paragraphs[p].addAnnotation(CITATION_TYPE, 0, paragraphs[p].size());
					}
					
					//	start of new div
					if (!sectionType.equals(openSectionType)) {
						
						//	mark div if one open
						if (sectionStart != -1) {
							MutableAnnotation div = data.addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, sectionStart, (paragraphs[p].getStartIndex() - sectionStart));
							div.setAttribute(TYPE_ATTRIBUTE, openSectionType);
						}
						
						//	remember start and type of new div
						sectionStart = paragraphs[p].getStartIndex();
						openSectionType = sectionType;
					}
					
				//	paragraph belongs to treatment
				} else {
					
					//	mark div if one open
					if (sectionStart != -1) {
						MutableAnnotation div = data.addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, sectionStart, (paragraphs[p].getStartIndex() - sectionStart));
						div.setAttribute(TYPE_ATTRIBUTE, openSectionType);
					}
					
					//	remember no div open
					sectionStart = -1;
					openSectionType = null;
				}
			}
			
			//	mark last div if one open
			if (openSectionType != null) {
				MutableAnnotation div = data.addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, sectionStart, (data.size() - sectionStart));
				div.setAttribute(TYPE_ATTRIBUTE, openSectionType);
			}
		}
	}
	
	private String classifyTreatmentPart(MutableAnnotation paragraph) {
		
		//	obtain scores
		ArrayList scoreList = new ArrayList();
		for (int t = 0; t < TREATMENT_DIV_TYPES.length; t++)
			scoreList.add(new TreatmentPartScore(TREATMENT_DIV_TYPES[t], this.scoreTreatmentPart(paragraph, TREATMENT_DIV_TYPES[t])));
		
		//	get top scored type
		Collections.sort(scoreList);
		TreatmentPartScore top = ((TreatmentPartScore) scoreList.get(0));
		if (top.score == 0) return description_DIV_TYPE;
		else return top.divType;
	}
	
	//	statistics for div type classification
	private HashMap divTypeTerms = new HashMap();
	private HashMap divTypeTermStatistics = new HashMap();
	
	private double scoreTreatmentPart(MutableAnnotation paragraph, String divType) {
		if (this.divTypeTermStatistics.containsKey(divType)) {
			StringIndex stat = ((StringIndex) this.divTypeTermStatistics.get(divType));
			StringVector terms = TokenSequenceUtils.getTextTokens(paragraph);
			double score = 0;
			for (int t = 0; t < terms.size(); t++)
				score += stat.getCount(terms.get(t));
			return (score / stat.size());
		} else return 0;
	}
	
	private void rememberClassification(MutableAnnotation paragraph, String divType) {
		StringVector terms;
		if (this.divTypeTerms.containsKey(divType))
			terms = ((StringVector) this.divTypeTerms.get(divType));
		else {
			terms = new StringVector(true);
			this.divTypeTerms.put(divType, terms);
		}
		
		StringIndex termStat;
		if (this.divTypeTermStatistics.containsKey(divType))
			termStat = ((StringIndex) this.divTypeTermStatistics.get(divType));
		else {
			termStat = new StringIndex(true);
			this.divTypeTermStatistics.put(divType, termStat);
		}
		
		StringVector newTerms = TokenSequenceUtils.getTextTokens(paragraph);
		terms.addContentIgnoreDuplicates(newTerms);
		for (int t = 0; t < newTerms.size(); t++) termStat.add(newTerms.get(t));
	}
	
	private class TreatmentPartScore implements Comparable {
		final String divType;
		final double score;
		TreatmentPartScore(String divType, double score) {
			this.divType = divType;
			this.score = score;
		}
		public int compareTo(Object o) {
			if (this.score < ((TreatmentPartScore) o).score) return 1;
			else if (this.score > ((TreatmentPartScore) o).score) return -1;
			else return 0;
		}
	}
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load term statistics
		for (int t = 0; t < TREATMENT_DIV_TYPES.length; t++) try {
			
			//	load data
//			File file = new File(this.dataPath, ("TERMS_" + TREATMENT_DIV_TYPES[t] + ".csv"));
			InputStream is = this.dataProvider.getInputStream("TERMS_" + TREATMENT_DIV_TYPES[t] + ".csv");
			StringRelation typeData = StringRelation.readCsvData(new InputStreamReader(is), '"', true, null);
			is.close();
			
			//	read data
			StringVector terms = new StringVector();
			StringIndex termStat = new StringIndex(true);
			for (int d = 0; d < typeData.size(); d++) try {
				StringTupel data = typeData.get(d);
				String term = data.getValue("term");
				int count = Integer.parseInt(data.getValue("count", "1"));
				terms.addElementIgnoreDuplicates(term);
				termStat.add(term, count);
			} catch (NumberFormatException nfe) {}
			
			//	store data
			this.divTypeTerms.put(TREATMENT_DIV_TYPES[t], terms);
			this.divTypeTermStatistics.put(TREATMENT_DIV_TYPES[t], termStat);
		} catch (IOException ioe) {}
		
		//	load number prefixes
		try {
			InputStream is = this.dataProvider.getInputStream("numberPrefixes.txt");
			this.numberPrefixes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {
			this.numberPrefixes = new StringVector();
			this.numberPrefixes.addElement("No");
		}
	}
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		
		//	data not editable, forget it
		if (!this.dataProvider.isDataEditable()) return;
		
		//	save term statistics
		StringVector keys = new StringVector();
		keys.addElement("term");
		keys.addElement("count");
		for (int t = 0; t < TREATMENT_DIV_TYPES.length; t++) try {
			
			//	get data
			StringVector terms = ((StringVector) this.divTypeTerms.get(TREATMENT_DIV_TYPES[t]));
			terms.sortLexicographically(false, false);
			StringIndex termStat = ((StringIndex) this.divTypeTermStatistics.get(TREATMENT_DIV_TYPES[t]));
			
//			//	create storage file
//			File file = new File(this.dataPath, ("TERMS_" + TREATMENT_DIV_TYPES[t] + ".csv"));
//			if (file.exists()) {
//				String fileName = file.toString();
//				File oldFile = new File(fileName + ".old");
//				if (oldFile.exists() && oldFile.delete())
//					oldFile = new File(fileName + ".old");
//				file.renameTo(oldFile);
//				file = new File(fileName);
//			}
			
			//	assemble data
			StringRelation typeData = new StringRelation();
			for (int d = 0; d < terms.size(); d++) {
				String term = terms.get(d);
				StringTupel data = new StringTupel();
				data.setValue("term", term);
				data.setValue("count", ("" + termStat.getCount(term)));
				typeData.addElement(data);
			}
			
			//	write data
			Writer w = new OutputStreamWriter(this.dataProvider.getOutputStream("TERMS_" + TREATMENT_DIV_TYPES[t] + ".csv"));
			StringRelation.writeCsvData(w, typeData, '"', keys);
			w.flush();
			w.close();
		} catch (Exception e) {}
	}
	
	private String[] getDivTypesForSectionType(String sectionType) {
		if (treatment_start_OPTION.equals(sectionType) || treatment_continued_OPTION.equals(sectionType))
			return TREATMENT_DIV_TYPES;
		else if (catalogue_entry_OPTION.equals(sectionType))
			return CATALOGUE_ENTRY_DIV_TYPES;
		else if (document_head_OPTION.equals(sectionType))
			return HEAD_DIV_TYPES;
		else if (reference_group_OPTION.equals(sectionType))
			return REF_GROUP_DIV_TYPES;
		else return DEFAULT_DIV_TYPES;
	}
	
	private Color getColorForDivType(String sectionType, String divType) {
		if (treatment_start_OPTION.equals(sectionType) || treatment_continued_OPTION.equals(sectionType)) {
			if (biology_ecology_DIV_TYPE.equals(divType)) {
				return Color.GREEN;
			} else if (description_DIV_TYPE.equals(divType)) {
				return Color.YELLOW;
			} else if (diagnosis_DIV_TYPE.equals(divType)) {
				return Color.PINK;
			} else if (discussion_DIV_TYPE.equals(divType)) {
				return Color.RED;
			} else if (distribution_DIV_TYPE.equals(divType)) {
				return Color.BLUE;
			} else if (etymology_DIV_TYPE.equals(divType)) {
				return Color.MAGENTA;
			} else if (materials_examined_DIV_TYPE.equals(divType)) {
				return Color.CYAN;
			} else if (nomenclature_DIV_TYPE.equals(divType)) {
				return Color.ORANGE;
			} else if (reference_group_DIV_TYPE.equals(divType)) {
				return Color.LIGHT_GRAY;
			} else if (synonymic_list_OPTION.equals(divType)) {
				return Color.ORANGE.brighter();
			} else return Color.GRAY;
		} else if (catalogue_entry_OPTION.equals(sectionType)) {
			return Color.LIGHT_GRAY;
		} else if (reference_group_OPTION.equals(sectionType) && reference_DIV_TYPE.equals(divType)) {
			return Color.LIGHT_GRAY;
		} else return Color.GRAY;
	}
	
	private class DsBox extends JPanel {
		
		JComboBox sectionTypeSelector;
		String sectionType;
		JComboBox divTypeSelector;
		String divType;
		
		JPanel topLabel = new JPanel();
		JPanel bottomLabel = new JPanel();
		
		private DsBox[] boxes;
		private int position;
		
		public DsBox(final MutableAnnotation paragraph, DsBox[] boxes, int position) {
			super(new GridBagLayout(), true);
			this.boxes = boxes;
			this.position = position;
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			final JTextArea display = new JTextArea();
			display.setText(paragraph.getValue());
			display.setLineWrap(true);
			display.setWrapStyleWord(true);
			display.setEditable(false);
			JScrollPane displayBox = new JScrollPane(display);
			
			this.divTypeSelector = new JComboBox(TREATMENT_DIV_TYPES);
			this.divTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.divTypeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String newDivType = divTypeSelector.getSelectedItem().toString();
					cascadeDivTypeChange(divType, newDivType);
					divType = newDivType;
					display.setBackground(getColorForDivType(sectionType, divType));
				}
			});
			
			this.sectionTypeSelector = new JComboBox(SECTION_TYPES);
			this.sectionTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.sectionTypeSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String newSectionType = sectionTypeSelector.getSelectedItem().toString();
					String[] divTypes = getDivTypesForSectionType(newSectionType);
					divTypeSelector.setModel(new DefaultComboBoxModel(divTypes));
					divTypeSelector.setEnabled(divTypes.length > 1);
					cascadeSectionTypeChange(sectionType, newSectionType);
					
					if (treatment_start_OPTION.equals(newSectionType)) {
						divTypeSelector.setSelectedItem(nomenclature_DIV_TYPE);
						topLabel.setBackground(Color.GRAY);
					} else if (treatment_continued_OPTION.equals(newSectionType)) {
						divTypeSelector.setSelectedItem(classifyTreatmentPart(paragraph));
					} else if (reference_group_OPTION.equals(newSectionType)) {
						Annotation[] citations = paragraph.getAnnotations(CITATION_TYPE);
						if (citations.length == 0)
							divTypeSelector.setSelectedItem(reference_DIV_TYPE);
						else divTypeSelector.setSelectedItem(other_DIV_TYPE);
					} else if (document_head_OPTION.equals(newSectionType)) {
						if (paragraph.size() < 7) divTypeSelector.setSelectedItem(document_author_DIV_TYPE);
						else if (paragraph.size() < 20) divTypeSelector.setSelectedItem(document_title_DIV_TYPE);
						else divTypeSelector.setSelectedItem(other_DIV_TYPE);
					}
					
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
					display.setBackground(getColorForDivType(sectionType, divTypeSelector.getSelectedItem().toString()));
				}
			});
			
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
			this.add(this.divTypeSelector, gbc.clone());
			
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
			this.divType = this.divTypeSelector.getSelectedItem().toString();
			display.setBackground(getColorForDivType(this.sectionType, this.divType));
		}
		
		void cascadeSectionTypeChange(String oldType, String newType) {
			if (((this.position + 1) < this.boxes.length) && (this.boxes[this.position + 1] != null)) {
				DsBox nextBox = this.boxes[this.position + 1];
				String nextType = nextBox.getSectionType();
				if (treatment_start_OPTION.equals(newType)) {
					nextBox.setSectionType(treatment_continued_OPTION);
				} else if (treatment_continued_OPTION.equals(newType)) {
					if (!catalogue_entry_OPTION.equals(nextType) && !treatment_start_OPTION.equals(nextType))
						nextBox.setSectionType(treatment_continued_OPTION);
				} else if (oldType.equals(nextType) || (treatment_start_OPTION.equals(oldType) && treatment_continued_OPTION.equals(nextType))) {
					nextBox.setSectionType(newType);
				}
			}
		}
		
		void cascadeDivTypeChange(String oldType, String newType) {
			if (((this.position + 1) < this.boxes.length) && (this.boxes[this.position + 1] != null)) {
				DsBox nextBox = this.boxes[this.position + 1];
				if (treatment_continued_OPTION.equals(nextBox.getSectionType()) && oldType.equals(nextBox.getDivType()))
					nextBox.setDivType(newType);
			}
		}
		
		String getSectionType() {
			return this.sectionTypeSelector.getSelectedItem().toString();
		}
		
		void setSectionType(String value) {
			this.sectionTypeSelector.setSelectedItem(value);
		}
		
		String getDivType() {
			return this.divTypeSelector.getSelectedItem().toString();
		}
		
		void setDivType(String value) {
			this.divTypeSelector.setSelectedItem(value);
		}
	}
}
