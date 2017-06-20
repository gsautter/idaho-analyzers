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
import java.awt.FlowLayout;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

public class TreatmentStructurer extends AbstractConfigurableAnalyzer implements LiteratureConstants, LocationConstants {
	
	public static final String TREATMENT_ANNOTATION_TYPE = "treatment";
	
	//	in-treatment div-types according to TaxonX
	private static final String biology_ecology_DIV_TYPE = "biology_ecology";
	private static final String description_DIV_TYPE = "description";
	private static final String diagnosis_DIV_TYPE = "diagnosis";
	private static final String discussion_DIV_TYPE = "discussion";
	private static final String distribution_DIV_TYPE = "distribution";
	private static final String etymology_DIV_TYPE = "etymology";
	private static final String materials_examined_DIV_TYPE = "materials_examined";
	private static final String synonymic_list_OPTION = "synonymic_list";
	
	//	nomenclature is not a div in TaxonX
	private static final String nomenclature_DIV_TYPE = "nomenclature";
	
	//	reference group is not a div in TaxonX
	private static final String reference_group_DIV_TYPE = "reference_group";
	
	//	special div type for layout artifacts
	private static final String ARTIFACT_DIV_TYPE = "ARTIFACT";
	
	//	option arrays
	private static final String[] TREATMENT_DIV_TYPES = {
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
	
	private static final String DIV_TYPE_ATTRIBUTE = "divType";
	
	private StringVector numberPrefixes;
	private StringVector properDivTypes = new StringVector();
	private StringVector artifactAnnotationTypes = new StringVector();
	
	public TreatmentStructurer() {
		this.properDivTypes.addElement(biology_ecology_DIV_TYPE);
		this.properDivTypes.addElement(description_DIV_TYPE);
		this.properDivTypes.addElement(diagnosis_DIV_TYPE);
		this.properDivTypes.addElement(discussion_DIV_TYPE);
		this.properDivTypes.addElement(distribution_DIV_TYPE);
		this.properDivTypes.addElement(etymology_DIV_TYPE);
		this.properDivTypes.addElement(materials_examined_DIV_TYPE);
		this.properDivTypes.addElement(nomenclature_DIV_TYPE);
		this.properDivTypes.addElement(reference_group_DIV_TYPE);
		this.properDivTypes.addElement(synonymic_list_OPTION);
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		System.out.println("TreatmentStructurer: doing document ...");
		
//		//	make sure treatments are MutableAnnotations
//		Annotation[] treatmentAnnotations = data.getAnnotations(TREATMENT_ANNOTATION_TYPE);
//		for (int a = 0; a < treatmentAnnotations.length; a++) {
//			if (!(treatmentAnnotations[a] instanceof MutableAnnotation)) {
//				MutableAnnotation treatment = data.addAnnotation(TREATMENT_ANNOTATION_TYPE, treatmentAnnotations[a].getStartIndex(), treatmentAnnotations[a].size());
//				treatment.copyAttributes(treatmentAnnotations[a]);
//				data.removeAnnotation(treatmentAnnotations[a]);
//			}
//		}
//		
		//	ask for paragraph classification scheme (if allowed)
		boolean classifyParagraphs = false;
		if (parameters.containsKey(INTERACTIVE_PARAMETER))
			classifyParagraphs = (JOptionPane.showConfirmDialog(null, (
					"Should the treatment structurer classify paragraphs based on word frequencies in earlier documents?\n" +
					"If you choose NO, pre-classified will work like this:\n" +
					" - labelled taxonomic name present ==> '" + nomenclature_DIV_TYPE + "'\n" +
					" - single taxonomic name at start of paragraph ==> '" + nomenclature_DIV_TYPE + "'\n" +
					" - series of taxonomic names at start of paragraph ==> '" + discussion_DIV_TYPE + "'\n" +
					" - citations / bibliographic references present ==> '" + reference_group_DIV_TYPE + "'\n" +
					" - locations present ==> '" + materials_examined_DIV_TYPE + "'\n" +
					" - otherwise ==> '" + description_DIV_TYPE + "'")
					, "Choose Paragraph Classification Mode", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
		
		//	get treatments
		MutableAnnotation[] treatments = data.getMutableAnnotations(TREATMENT_ANNOTATION_TYPE);
		System.out.println(" - got " + treatments.length + " treatments");
		for (int t = 0; t < treatments.length; t++) {
			System.out.println(" - doing treatment " + (t+1) + "/" + treatments.length + " (size: " + treatments[t].size() + ") ...");
			boolean stopAfterTreatment = false;
			
			//	group paragraphs to treatments and other contents
			MutableAnnotation[] subSubSections = treatments[t].getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
			System.out.println("   - got " + subSubSections.length + " divs");
			int divSum = 0;
			for (int s = 0; s < subSubSections.length; s++) {
				divSum += subSubSections[s].size();
				String divType = subSubSections[s].getAttribute(TYPE_ATTRIBUTE, "").toString();
				if (divType.length() != 0) {
					MutableAnnotation[] paragraphs = subSubSections[s].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
					for (int p = 0; p < paragraphs.length; p++)
						paragraphs[p].setAttribute(DIV_TYPE_ATTRIBUTE, divType);
				}
			}
			System.out.println("   - total div length is " + divSum);
			
			/*	open dialog only if 
			 *  there are paragraphs 
			 *    and 
			 *  (
			 *    there are parts of the treatment not covered by divs 
			 *      or
			 *    there is only one treatment - possibly a correction on a single treatment that is already covered
			 *  )
			 */ 
			MutableAnnotation[] paragraphs = treatments[t].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			System.out.println("   - got " + paragraphs.length + " paragraphs");
			if ((paragraphs.length != 0) && ((divSum < treatments[t].size()) || (treatments.length == 1))) {
				
				//	create paragraph trays
				ArrayList paragraphBoxList = new ArrayList();
				for (int p = 0; p < paragraphs.length; p++) {
					
					//	check if layout artifact
					boolean isArtifact = false;
					for (int a = 0; a < this.artifactAnnotationTypes.size(); a++) {
						Annotation[] artifacts = paragraphs[p].getAnnotations(this.artifactAnnotationTypes.get(a));
						if ((artifacts.length != 0) && (artifacts[0].size() == paragraphs[p].size()))
							isArtifact = true;
					}
					
					//	get existing div type
					String divType = paragraphs[p].getAttribute(DIV_TYPE_ATTRIBUTE, "").toString();
					
					//	layout artifact, exclude from dialog
					if (isArtifact) {
						paragraphs[p].setAttribute(DIV_TYPE_ATTRIBUTE, ARTIFACT_DIV_TYPE);
						
					//	div type already set
					} else if (this.properDivTypes.contains(divType)) {
						DsBox box = new DsBox(paragraphs[p]);
						paragraphBoxList.add(box);
						box.setDivType(description_DIV_TYPE); // have to pre-set type so color will be set
						box.setDivType(divType);
						
					//	determine div type from paragraph content
					} else {
						DsBox box = new DsBox(paragraphs[p]);
						paragraphBoxList.add(box);
						
						//	gather "hard" evidence
						Annotation[] taxonomicNames = paragraphs[p].getAnnotations("taxonomicName");
						Annotation[] taxonomicNameLabels = paragraphs[p].getAnnotations("taxonomicNameLabel");
						Annotation[] citations = paragraphs[p].getAnnotations(CITATION_TYPE);
						Annotation[] locations = paragraphs[p].getAnnotations(LOCATION_TYPE);
						
						//	find first word
						int firstWordIndex = 0;
						while ((firstWordIndex < paragraphs[p].size()) && (!Gamta.isWord(paragraphs[p].tokenAt(firstWordIndex)) || this.numberPrefixes.containsIgnoreCase(paragraphs[p].valueAt(firstWordIndex))))
							firstWordIndex++;
						if (firstWordIndex == paragraphs[p].size()) firstWordIndex = -1;
						
						boolean isNomenclature = false;
						boolean isDiscussion = false;
						
						//	paragraph starts with taxon name from very first word
						if ((taxonomicNames.length != 0) && (taxonomicNames[0].getStartIndex() <= firstWordIndex) && (taxonomicNames[0].firstValue().length() > 2))
							isNomenclature = true;
						
						//	paragraph contains labelled taxon name
						if ((taxonomicNames.length != 0) && (taxonomicNameLabels.length != 0))
							isNomenclature = true;
						
						//	paragraph starts with series of taxon names
						if ((taxonomicNames.length > 2) && (taxonomicNames[2].getStartIndex() < 10)) {
							isNomenclature = false;
							isDiscussion = true;
						}
						
						//	first paragraph, almost always nomenclature
						if (!isDiscussion && (p == 0) && (taxonomicNames.length != 0))
							isNomenclature = true;
						
						//	nomenclature section
						if (isNomenclature) {
							box.setDivType(nomenclature_DIV_TYPE);
							
						//	discussion
						} else if (isDiscussion) {
							box.setDivType(discussion_DIV_TYPE);
							
						//	reference group
						} else if (citations.length != 0) {
							box.setDivType(reference_group_DIV_TYPE);
							
						//	materials examined
						} else if (locations.length != 0) {
							box.setDivType(materials_examined_DIV_TYPE);
							
						//	determine div type from word frequencies
						} else {
							
							//	classify paragraphs (learns from user's classification)
							box.setDivType(nomenclature_DIV_TYPE); // have to pre-set type so color will be set
							
							//	classify paragraphs only if desired
							if (classifyParagraphs)
								box.setDivType(this.classifyTreatmentPart(paragraphs[p]));
							
							//	preset to 'description' otherwise
							else box.setDivType(description_DIV_TYPE);
						}
					}
				}
				
				//	remember existing divs
				HashMap existingDivs = new HashMap();
				for (int s = 0; s < subSubSections.length; s++)
					existingDivs.put((subSubSections[s].getStartIndex() + "-" + subSubSections[s].size()), subSubSections[s]);
				
				//	get feedback if allowed to do so
				if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
					DsBox[] paragraphBoxes = ((DsBox[]) paragraphBoxList.toArray(new DsBox[paragraphBoxList.size()]));
					
					//	build dialog
					final JDialog td = new JDialog(((JFrame) null), "Check Treatment Structure", true);
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
					for (int p = 0; p < paragraphBoxes.length; p++) {
						paragraphBoxes[p].boxes = paragraphBoxes;
						paragraphBoxes[p].position = p;
						panel.add(paragraphBoxes[p], gbc.clone());
						gbc.gridy++;
					}
					
					//	add OK button
					JButton continueButton = new JButton("OK & Continue");
					continueButton.setBorder(BorderFactory.createRaisedBevelBorder());
					continueButton.setPreferredSize(new Dimension(100, 21));
					continueButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							System.out.println("   - dialog closed to continue");
							td.dispose();
						}
					});
					JButton stopButton = new JButton("OK & Stop After");
					stopButton.setBorder(BorderFactory.createRaisedBevelBorder());
					stopButton.setPreferredSize(new Dimension(100, 21));
					stopButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							System.out.println("   - dialog closed to stop");
							gbc.fill = GridBagConstraints.NONE;
							td.dispose();
						}
					});
					JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
					buttonPanel.add(continueButton);
					buttonPanel.add(stopButton);
					td.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
					
					//	ensure dialog is closed with button
					td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					
					//	get feedback
					td.setSize(500, (Math.min(650, (60 * paragraphBoxes.length) + 75)));
					td.setLocationRelativeTo(null);
					System.out.println("   - showing dialog");
					td.setVisible(true);
					
					//	assign div type to paragraph & remember classifications
					for (int p = 0; p < paragraphBoxes.length; p++) {
						paragraphBoxes[p].paragraph.setAttribute(DIV_TYPE_ATTRIBUTE, paragraphBoxes[p].getDivType());
						this.rememberClassification(paragraphBoxes[p].paragraph, paragraphBoxes[p].getDivType());
					}
					
					//	check if stop desired
					if (gbc.fill == GridBagConstraints.NONE) stopAfterTreatment = true;
				}
				
				//	inherit div types to artifact paragraphs
				String lastValidDivType = null;
				paragraphs = treatments[t].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < paragraphs.length; p++) {
					String divType = paragraphs[p].getAttribute(DIV_TYPE_ATTRIBUTE, "").toString();
					if (ARTIFACT_DIV_TYPE.equals(divType)) {
						if (lastValidDivType == null)
							paragraphs[p].setAttribute(DIV_TYPE_ATTRIBUTE, description_DIV_TYPE);
						else paragraphs[p].setAttribute(DIV_TYPE_ATTRIBUTE, lastValidDivType);
					} else lastValidDivType = divType;
				}
				
				//	create div structure in treatment
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
							MutableAnnotation div = ((MutableAnnotation) existingDivs.remove(divStart + "-" + (paragraphs[p].getStartIndex() - divStart)));
							if (div == null) div = treatments[t].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, divStart, (paragraphs[p].getStartIndex() - divStart));
							div.setAttribute(TYPE_ATTRIBUTE, openDivType);
						}
						
						//	remember start and type of new div
						divStart = paragraphs[p].getStartIndex();
						openDivType = divType;
					}
				}
				
				//	mark last div if one open
				if (openDivType != null) {
					MutableAnnotation div = ((MutableAnnotation) existingDivs.remove(divStart + "-" + (treatments[t].size() - divStart)));
					if (div == null) div = treatments[t].addAnnotation(MutableAnnotation.SUB_SUB_SECTION_TYPE, divStart, (treatments[t].size() - divStart));
					div.setAttribute(TYPE_ATTRIBUTE, openDivType);
				}
				
				//	remove old divs that are no longer used
				ArrayList oldDivKeys = new ArrayList(existingDivs.keySet());
				for (int k = 0; k < oldDivKeys.size(); k++)
					treatments[t].removeAnnotation((MutableAnnotation) existingDivs.remove(oldDivKeys.get(k)));
				
				System.out.println("   - treatment done");
			}
			
			//	clean up temporary attributes
			for (int p = 0; p < paragraphs.length; p++)
				paragraphs[p].removeAttribute(DIV_TYPE_ATTRIBUTE);
			
			//	stop if desired
			if (stopAfterTreatment) return;
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
		
		//	load custom artifact types
		try {
			InputStream is = this.dataProvider.getInputStream("artifactAnnotationTypes.txt");
			this.artifactAnnotationTypes = StringVector.loadList(is);
			is.close();
		} catch (IOException ioe) {}
		
		//	add fix artifact types
		this.artifactAnnotationTypes.addElementIgnoreDuplicates(PAGE_BORDER_TYPE);
		this.artifactAnnotationTypes.addElementIgnoreDuplicates(PAGE_TITLE_TYPE);
		this.artifactAnnotationTypes.addElementIgnoreDuplicates(PAGE_NUMBER_TYPE);
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
	
	private Color getColorForDivType(String divType) {
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
	}
	
	private class DsBox extends JPanel {
		
		private MutableAnnotation paragraph;
		
		private JComboBox divTypeSelector;
		private String divType;
		
		private DsBox[] boxes;
		private int position;
		
		public DsBox(MutableAnnotation paragraph) {
			super(new GridBagLayout(), true);
			this.paragraph = paragraph;
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			final JTextArea display = new JTextArea();
			display.setText(this.paragraph.getValue());
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
					display.setBackground(getColorForDivType(divType));
				}
			});
			
			String pageNumber = this.paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString().trim();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridy = 0;
			this.add(this.divTypeSelector, gbc.clone());
			gbc.gridy = 1;
			gbc.weighty = 1;
			if (pageNumber.length() == 0)
				this.add(new JLabel("<Unknown Page>"), gbc.clone());
			else this.add(new JLabel("Page " + pageNumber), gbc.clone());
			
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 3;
			gbc.gridheight = 2;
			gbc.gridy = 0;
			this.add(displayBox, gbc.clone());
			
			this.setPreferredSize(new Dimension(450, 60));
			this.divType = this.divTypeSelector.getSelectedItem().toString();
			display.setBackground(getColorForDivType(this.divType));
		}
		
		void cascadeDivTypeChange(String oldType, String newType) {
			//	do not cascade during initialization
			if (this.boxes == null) return;
			
			//	propagate change
			if (((this.position + 1) < this.boxes.length) && (this.boxes[this.position + 1] != null)) {
				DsBox nextBox = this.boxes[this.position + 1];
				if (oldType.equals(nextBox.getDivType()))
					nextBox.setDivType(newType);
			}
		}
		
		String getDivType() {
			return this.divTypeSelector.getSelectedItem().toString();
		}
		
		void setDivType(String value) {
			this.divTypeSelector.setSelectedItem(value);
		}
	}
}
