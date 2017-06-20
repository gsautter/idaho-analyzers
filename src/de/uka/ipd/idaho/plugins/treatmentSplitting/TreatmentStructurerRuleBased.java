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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TreatmentStructurerRuleBased extends AbstractConfigurableAnalyzer implements LiteratureConstants, LocationConstants {
	
	public static final String TREATMENT_ANNOTATION_TYPE = "treatment";
	
	//	in-treatment div-types according to TaxonX
	private static final String biology_ecology_DIV_TYPE = "biology_ecology";
	private static final String description_DIV_TYPE = "description";
	private static final String diagnosis_DIV_TYPE = "diagnosis";
	private static final String discussion_DIV_TYPE = "discussion";
	private static final String distribution_DIV_TYPE = "distribution";
	private static final String etymology_DIV_TYPE = "etymology";
	private static final String key_DIV_TYPE = "key";
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
			key_DIV_TYPE,
			materials_examined_DIV_TYPE,
			nomenclature_DIV_TYPE,
			reference_group_DIV_TYPE,
			synonymic_list_OPTION
		};
	
	private static final String previousRuleNumber = "$rule";
	private static final String previousTypeName = "$type";
	private static final String previousRuleLabel = "<Previous Type>";
	private static final String positionInTreatment = "$position";
	
	private static final String DIV_TYPE_ATTRIBUTE = "divType";
	
	private StringVector properDivTypes = new StringVector();
	private StringVector artifactAnnotationTypes = new StringVector();
	
	private ArrayList rules = new ArrayList();
	
	private String defaultDivType = description_DIV_TYPE;
	
	private boolean configurationModified = false;
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		System.out.println("TreatmentStructurer: doing document ...");
		
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
			
			GPathVariableResolver variables = new GPathVariableResolver();
			variables.setVariable(previousRuleNumber, new GPathString("" + (this.rules.size() + 2)));
			variables.setVariable(previousTypeName, new GPathString(this.defaultDivType));
			
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
					if (isArtifact)
						paragraphs[p].setAttribute(DIV_TYPE_ATTRIBUTE, ARTIFACT_DIV_TYPE);
					
					//	div type already set
					else if (this.properDivTypes.contains(divType)) {
						TsBox box = new TsBox(paragraphs[p]);
						paragraphBoxList.add(box);
						box.setDivType(description_DIV_TYPE); // have to pre-set type so color will be set
						box.setDivType(divType);
					}
					
					//	determine div type from paragraph content
					else {
						TsBox box = new TsBox(paragraphs[p]);
						paragraphBoxList.add(box);
						
						//	classify paragraphs (learns from user's classification)
						box.setDivType(nomenclature_DIV_TYPE); // have to pre-set type so color will be set
						
						//	classify paragraphs only if desired
						box.setDivType(this.classifyTreatmentPart(paragraphs[p], p, variables));
					}
				}
				TsBox[] paragraphBoxes = ((TsBox[]) paragraphBoxList.toArray(new TsBox[paragraphBoxList.size()]));
				
				//	remember existing divs
				HashMap existingDivs = new HashMap();
				for (int s = 0; s < subSubSections.length; s++)
					existingDivs.put((subSubSections[s].getStartIndex() + "-" + subSubSections[s].size()), subSubSections[s]);
				
				//	get feedback if allowed to do so
				if (parameters.containsKey(INTERACTIVE_PARAMETER)) {
					
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
					
					//	check if stop desired
					if (gbc.fill == GridBagConstraints.NONE) stopAfterTreatment = true;
				}
				
				//	assign div type to paragraph & remember classifications
				for (int p = 0; p < paragraphBoxes.length; p++)
					paragraphBoxes[p].paragraph.setAttribute(DIV_TYPE_ATTRIBUTE, paragraphBoxes[p].getDivType());
				
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
	
	private class TsBox extends JPanel {
		
		private MutableAnnotation paragraph;
		
		private JComboBox divTypeSelector;
		private String divType;
		
		private TsBox[] boxes;
		private int position;
		
		public TsBox(MutableAnnotation paragraph) {
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
				TsBox nextBox = this.boxes[this.position + 1];
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
	
	private Color getColorForDivType(String divType) {
		if (biology_ecology_DIV_TYPE.equals(divType))
			return Color.GREEN;
		else if (description_DIV_TYPE.equals(divType))
			return Color.YELLOW;
		else if (diagnosis_DIV_TYPE.equals(divType))
			return Color.PINK;
		else if (discussion_DIV_TYPE.equals(divType))
			return Color.RED;
		else if (distribution_DIV_TYPE.equals(divType))
			return Color.BLUE;
		else if (etymology_DIV_TYPE.equals(divType))
			return Color.MAGENTA;
		else if (key_DIV_TYPE.equals(divType))
			return Color.WHITE;
		else if (materials_examined_DIV_TYPE.equals(divType))
			return Color.CYAN;
		else if (nomenclature_DIV_TYPE.equals(divType))
			return Color.ORANGE;
		else if (reference_group_DIV_TYPE.equals(divType))
			return Color.LIGHT_GRAY;
		else if (synonymic_list_OPTION.equals(divType))
			return Color.ORANGE.brighter();
		else return Color.GRAY;
	}
	
	private String classifyTreatmentPart(MutableAnnotation paragraph, int position, GPathVariableResolver variables) {
		variables.setVariable(positionInTreatment, new GPathNumber(position + 1));
		
		//	apply rules
		for (int r = 0; r < this.rules.size(); r++) {
			Rule rule = ((Rule) this.rules.get(r));
			if (GPath.evaluateExpression(rule.predicate, paragraph, variables).asBoolean().value) {
				variables.setVariable(previousRuleNumber, new GPathString("" + rule.position));
				String type = rule.type;
				if (previousTypeName.equals(type))
					type = variables.getVariable(previousTypeName).asString().value;
				variables.setVariable(previousTypeName, new GPathString(type));
				return type;
			}
		}
		
		//	no rule applied, set variables & return default
		variables.setVariable(previousRuleNumber, new GPathString("" + (this.rules.size() + 1)));
		variables.setVariable(previousTypeName, new GPathString("" + this.defaultDivType));
		return this.defaultDivType;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	avaliable types
		this.properDivTypes.addElement(biology_ecology_DIV_TYPE);
		this.properDivTypes.addElement(description_DIV_TYPE);
		this.properDivTypes.addElement(diagnosis_DIV_TYPE);
		this.properDivTypes.addElement(discussion_DIV_TYPE);
		this.properDivTypes.addElement(distribution_DIV_TYPE);
		this.properDivTypes.addElement(etymology_DIV_TYPE);
		this.properDivTypes.addElement(key_DIV_TYPE);
		this.properDivTypes.addElement(materials_examined_DIV_TYPE);
		this.properDivTypes.addElement(nomenclature_DIV_TYPE);
		this.properDivTypes.addElement(reference_group_DIV_TYPE);
		this.properDivTypes.addElement(synonymic_list_OPTION);
		
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
		
		//	read & parse rules
		try {
			InputStream is = this.dataProvider.getInputStream("treatmentStructurerRules.txt");
			StringVector ruleStrings = StringVector.loadList(is);
			is.close();
			for (int r = 0; r < ruleStrings.size(); r++) {
				String ruleString = ruleStrings.get(r);
				
				//	filter out comments
				if (!ruleString.startsWith("//")) {
					
					//	find split
					int split = ruleString.indexOf("==>");
					if (split != -1) {
						
						//	generate rule
						String predicate = ruleString.substring(0, split).trim();
						String type = ruleString.substring(split + 3).trim();
						Rule rule = new Rule(predicate, type);
						this.rules.add(rule);
						rule.position = this.rules.size();
					}
				}
			}
		} catch (IOException fnfe) {}
		
		//	get default div type
		this.defaultDivType = this.getParameter("defaultDivType", description_DIV_TYPE);
	}
	
	/**
	 * representation of a single classification rule
	 * 
	 * @author sautter
	 */
	private static class Rule {
		private String expressionString;
		private GPathExpression predicate;
		private String type;
		private int position = 0;
		private Rule(String predicate, String type) {
			this.expressionString = predicate;
			this.predicate = GPathParser.parseExpression(predicate);
			this.type = type;
		}
		public String toString() {
			return (this.position + ". " + this.expressionString + " ==> " + this.type);
		}
		public String toDataString() {
			return (this.expressionString + " ==> " + this.type);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#configureProcessor()
	 */
	public void configureProcessor() {
		TsConfigDialog tcd = new TsConfigDialog(this.rules, this.defaultDivType);
		tcd.setVisible(true);
		if (tcd.isDirty()) {
			this.configurationModified = true;
			this.defaultDivType = tcd.getDefaultDivType();
			Rule[] rules = tcd.getRules();
			this.rules.clear();
			for (int r = 0; r < rules.length; r++)
				this.rules.add(rules[r]);
		}
	}
	
	/**
	 * configuration dialog
	 * 
	 * @author sautter
	 */
	private class TsConfigDialog extends JDialog {
		
		private class RuleListModel extends AbstractListModel {
			
			public Object getElementAt(int index) {
				return rules.get(index);
			}
			
			public int getSize() {
				return rules.size();
			}

			public void fireContentsChanged() {
				super.fireContentsChanged(this, 0, this.getSize());
			}
		}
		
		private JComboBox defaultDivTypeSelector;
		private String defaultDivType = description_DIV_TYPE;
		
		private Vector rules = new Vector();
		private JList ruleList;
		private RuleListModel ruleListModel = new RuleListModel();
		
		private boolean dirty = false;
		
		private QueriableAnnotation testDocument;
		private QueriableAnnotation[] testDocumentParagraphs;
		private HashSet treatmentParagraphIDs = new HashSet();
		private HashMap treatmentNumbersByParagraphIDs = new HashMap();
		
		private TsConfigDialog(ArrayList rules, String defaultDivType)  {
			super(((JFrame) null), "Configure Treatment Structurer", true);
			
			//	remember data
			this.rules.addAll(rules);
			this.defaultDivType = defaultDivType;
			
			//	take control of window
			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			
			//	initialize rule list
			this.ruleList = new JList(this.ruleListModel);
			JScrollPane partListBox = new JScrollPane(this.ruleList);
			this.add(partListBox, BorderLayout.CENTER);
			this.ruleList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						if (editRule())
							refreshRuleList();
					}
				}
			});
			
			JButton upButton = new JButton("Up");
			upButton.setBorder(BorderFactory.createRaisedBevelBorder());
			upButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					moveUp();
				}
			});
			JButton downButton = new JButton("Down");
			downButton.setBorder(BorderFactory.createRaisedBevelBorder());
			downButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					moveDown();
				}
			});
			JPanel reorderButtonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 3;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			
			gbc.gridy = 0;
			reorderButtonPanel.add(upButton, gbc.clone());
			gbc.gridy = 1;
			reorderButtonPanel.add(downButton, gbc.clone());
			
			this.add(reorderButtonPanel, BorderLayout.WEST);
			
			JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			button = new JButton("Create Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (createRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			button = new JButton("Clone Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (cloneRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			button = new JButton("Edit Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (editRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			button = new JButton("Remove Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (removeRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			
			//	initialize selector for default div type
			editButtonPanel.add(new JLabel("   Default Div Type"));
			this.defaultDivTypeSelector = new JComboBox(TREATMENT_DIV_TYPES);
			this.defaultDivTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.defaultDivTypeSelector.setSelectedItem(this.defaultDivType);
			editButtonPanel.add(this.defaultDivTypeSelector);
			
			//	initialize test button
			button = new JButton("Test Rules");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testRules();
				}
			});
			editButtonPanel.add(button);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TsConfigDialog.this.dispose();
				}
			});
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dirty = false;
					TsConfigDialog.this.dispose();
				}
			});
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			this.getContentPane().add(editButtonPanel, BorderLayout.NORTH);
			this.getContentPane().add(this.ruleList, BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			this.setSize(800, 600);
			this.setLocationRelativeTo(null);
		}
		
		private boolean isDirty() {
			return (this.dirty || !this.defaultDivType.equals(this.defaultDivTypeSelector.getSelectedItem().toString()));
		}
		
		private Rule[] getRules() {
			Rule[] rules = ((Rule[]) this.rules.toArray(new Rule[this.rules.size()]));
			for (int r = 0; r < rules.length; r++)
				rules[r].position = (r+1);
			return rules;
		}
		
		private String getDefaultDivType() {
			return this.defaultDivTypeSelector.getSelectedItem().toString();
		}
		
		private void moveUp() {
			int index = this.ruleList.getSelectedIndex();
			if (index > 0) {
				this.rules.insertElementAt(this.rules.remove(index - 1), index);
				this.refreshRuleList();
				this.ruleList.setSelectedIndex(index - 1);
				this.dirty = true;
			}
		}
		
		private void moveDown() {
			int index = this.ruleList.getSelectedIndex();
			if ((index != -1) && ((index + 1) != this.rules.size())) {
				this.rules.insertElementAt(this.rules.remove(index), (index + 1));
				this.refreshRuleList();
				this.ruleList.setSelectedIndex(index + 1);
				this.dirty = true;
			}
		}
		
		private boolean createRule() {
			return this.createRule(null);
		}
		
		private boolean cloneRule() {
			int index = this.ruleList.getSelectedIndex();
			if (index == -1) return this.createRule();
			else return this.createRule((Rule) this.rules.get(index));
		}
		
		private boolean createRule(Rule model) {
			RuleEditorDialog cred = new RuleEditorDialog(model, ((model == null) ? "Create Rule" : "Clone Rule"), "Create");
			cred.setVisible(true);
			if (cred.isCommitted() && cred.isDirty()) {
				Rule rule = cred.getRule();
				if (rule != null) {
					this.rules.add(rule);
					this.ruleListModel.fireContentsChanged();
					this.dirty = true;
					return true;
				}
			}
			return false;
		}
		
		private boolean editRule() {
			int index = this.ruleList.getSelectedIndex();
			if (index != -1) {
				RuleEditorDialog cred = new RuleEditorDialog(((Rule) this.rules.get(index)), "Edit Rule", "Edit");
				cred.setVisible(true);
				if (cred.isCommitted() && cred.isDirty()) {
					Rule rule = cred.getRule();
					if (rule != null) {
						this.rules.setElementAt(rule, index);
						this.ruleListModel.fireContentsChanged();
						this.dirty = true;
						return true;
					}
				}
				return false;
			}
			return false;
		}
		
		private boolean removeRule() {
			int index = this.ruleList.getSelectedIndex();
			if (index != -1) {
				this.rules.remove(index);
				this.dirty = true;
				return true;
			}
			return false;
		}
		
		private void refreshRuleList() {
			for (int r = 0; r < this.rules.size(); r++)
				((Rule) this.rules.get(r)).position = (r+1);
			this.ruleListModel.fireContentsChanged();
			this.ruleList.validate();
		}
		
		private String validateRule(String expression) {
			String error = GPathParser.validatePath(expression);
			if (error == null) {
				try {
					GPathParser.parseExpression(expression);
				}
				catch (GPathException gpe) {
					error = gpe.getMessage();
				}
			}
			return error;
		}
		
		private void testRules() {
			QueriableAnnotation data = this.getTestDocument();
			if (data == null) return;
			
			Rule[] rules = this.getRules();
			HashSet[] ruleResultIDs = new HashSet[rules.length];
			
			//	validate & compile rules
			for (int r = 0; r < rules.length; r++) {
				ruleResultIDs[r] = new HashSet();
				
				String expression = rules[r].expressionString;
				expression = GPath.normalizePath(expression);
				String error = this.validateRule(expression);
				
				if (error != null) {
					JOptionPane.showMessageDialog(this, ("The expression of rule " + (r+1) + " is not valid:\n" + error), "Rule Validation", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//	compile expression
				else try {
					rules[r].predicate = GPathParser.parseExpression(expression);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(this, ("Error compiling expression of rule " + (r+1) + ":\n" + e.getMessage()), "Rule Compilation Error", JOptionPane.ERROR_MESSAGE);
					System.out.println(e.getClass().getName() + ": " + e.getMessage());
					e.printStackTrace(System.out);
					return;
				}
			}
			
			int noRuleAppliedPos = (rules.length + 2);
			int[] rulePositions = new int[this.testDocumentParagraphs.length];
			Arrays.fill(rulePositions, noRuleAppliedPos);
			String[] assignedTypes = new String[this.testDocumentParagraphs.length];
			Arrays.fill(assignedTypes, this.defaultDivType);
			
			GPathVariableResolver variables = new GPathVariableResolver();
			variables.setVariable(previousRuleNumber, new GPathString("" + (rules.length + 2)));
			variables.setVariable(previousTypeName, new GPathString(this.defaultDivType));
			
			String[] divTypes = new String[this.testDocumentParagraphs.length];
			Arrays.fill(divTypes, null);
			int position = 0;
			for (int p = 0; p < this.testDocumentParagraphs.length; p++) {
				
				//	do treatment paragraphs only
				if (this.isTreatmentParagraph(this.testDocumentParagraphs[p])) {
					variables.setVariable(positionInTreatment, new GPathNumber(position++ + 1));
					
					String pid = this.testDocumentParagraphs[p].getAnnotationID();
					int matchRulePosition = -1;
					
					//	apply rules
					for (int r = 0; r < rules.length; r++) {
						if (GPath.DEFAULT_ENGINE.evaluateExpression(rules[r].predicate, this.testDocumentParagraphs[p], variables).asBoolean().value) {
							ruleResultIDs[r].add(pid);
							if (matchRulePosition == -1) {
								matchRulePosition = r;
								if (previousTypeName.equals(rules[matchRulePosition].type))
									divTypes[p] = variables.getVariable(previousTypeName).asString().value;
								else divTypes[p] = rules[matchRulePosition].type;
							}
						}
					}
					
					//	no rule applied, set variables & return default
					if (matchRulePosition == -1) {
						variables.setVariable(previousRuleNumber, new GPathString("" + (this.rules.size() + 1)));
						variables.setVariable(previousTypeName, new GPathString(this.defaultDivType));
						divTypes[p] = this.defaultDivType;
					}
					else {
						variables.setVariable(previousRuleNumber, new GPathString("" + rules[matchRulePosition].position));
						variables.setVariable(previousTypeName, new GPathString(divTypes[p]));
					}
				}
				else position = 0;
			}
			
			//	build dialog
			final JDialog td = new JDialog(this, "Rule Application Result", true);
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
			
			//	create result display boxes
			int lastTreatmentNumber = -1;
			for (int p = 0; p < testDocumentParagraphs.length; p++) {
				Annotation paragraph = testDocumentParagraphs[p];
				if (isTreatmentParagraph(paragraph)) {
					String pid = paragraph.getAnnotationID();
					
					//	mark start of new treatment
					int treatmentNumber = getTreatmentNumber(paragraph);
					if (treatmentNumber != lastTreatmentNumber) {
						panel.add(new JLabel("Treatment on page " + paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE)), gbc.clone());
						gbc.gridy++;
						lastTreatmentNumber = treatmentNumber;
					}
					
					//	collect rules that matched current paragraph
					ArrayList matchRules = new ArrayList();
					for (int r = 0; r < rules.length; r++)
						if (ruleResultIDs[r].contains(pid))
							matchRules.add(rules[r]);
					
					//	prepare paragraph for display
					panel.add(new TsTestBox(paragraph, true, rules, ((Rule[]) matchRules.toArray(new Rule[matchRules.size()])), divTypes[p]), gbc.clone());
					gbc.gridy++;
				}
			}
			
			//	add OK button
			JButton continueButton = new JButton("Close");
			continueButton.setBorder(BorderFactory.createRaisedBevelBorder());
			continueButton.setPreferredSize(new Dimension(100, 21));
			continueButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					td.dispose();
				}
			});
			td.getContentPane().add(continueButton, BorderLayout.SOUTH);
			
			//	ensure dialog is closed with button
			td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			//	get feedback
			td.setSize(500, (Math.min(650, (60 * gbc.gridy) + 75)));
			td.setLocationRelativeTo(null);
			td.setVisible(true);
		}
		
		private QueriableAnnotation getTestDocument() {
			if (this.testDocument == null) {
				this.testDocument = Gamta.getTestDocument();
				if (this.testDocument != null) {
					System.out.println("Got test document");
					
					//	get paragraphs
					this.testDocumentParagraphs = this.testDocument.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
					System.out.println(" - got " + this.testDocumentParagraphs.length + " test document paragraphs");
					
					//	get treatments
					QueriableAnnotation[] treatments = this.testDocument.getAnnotations(TREATMENT_ANNOTATION_TYPE);
					System.out.println(" - got " + treatments.length + " treatments");
					for (int t = 0; t < treatments.length; t++) {
						Integer treatmentNumber = new Integer(t);
						
						//	collect non-artifact paragraphs from treatment
						QueriableAnnotation[] paragraphs = treatments[t].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
						System.out.println("   - got " + paragraphs.length + " paragraphs in treatment");
						for (int p = 0; p < paragraphs.length; p++) {
							
							//	check if layout artifact
							boolean isArtifact = false;
							for (int a = 0; a < TreatmentStructurerRuleBased.this.artifactAnnotationTypes.size(); a++) {
								Annotation[] artifacts = paragraphs[p].getAnnotations(TreatmentStructurerRuleBased.this.artifactAnnotationTypes.get(a));
								if ((artifacts.length != 0) && (artifacts[0].size() == paragraphs[p].size()))
									isArtifact = true;
							}
							String pid = paragraphs[p].getAnnotationID();
							this.treatmentNumbersByParagraphIDs.put(pid, treatmentNumber);
							if (!isArtifact)
								this.treatmentParagraphIDs.add(pid);
						}
						System.out.println("   - got " + this.treatmentParagraphIDs.size() + " treatment paragraphs now");
					}
				}
			}
			return this.testDocument;
		}
		
		private boolean isTreatmentParagraph(Annotation paragraph) {
			return this.treatmentParagraphIDs.contains(paragraph.getAnnotationID());
		}
		
		private int getTreatmentNumber(Annotation paragraph) {
			String pid = paragraph.getAnnotationID();
			if (this.treatmentNumbersByParagraphIDs.containsKey(pid))
				return ((Integer) this.treatmentNumbersByParagraphIDs.get(pid)).intValue();
			else return -1;
		}
		
		private class RuleEditorDialog extends JDialog {
			
			private Rule rule;
			private RuleEditorPanel editor;
			
			private RuleEditorDialog(Rule rule, String title, String commitButtonText) {
				super(TsConfigDialog.this, title, true);
				
				//	initialize editor
				this.editor = new RuleEditorPanel(this);
				if (rule != null)
					this.editor.setContent(rule);
				
				//	initialize main buttons
				JButton commitButton = new JButton(commitButtonText);
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						RuleEditorDialog.this.rule = RuleEditorDialog.this.editor.getContent();
						RuleEditorDialog.this.dispose();
					}
				});
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						RuleEditorDialog.this.rule = null;
						RuleEditorDialog.this.dispose();
					}
				});
				JPanel mainButtonPanel = new JPanel(new FlowLayout());
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(this.editor, BorderLayout.CENTER);
				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(600, 400));
				this.setLocationRelativeTo(TsConfigDialog.this);
			}
			
			private boolean isCommitted() {
				return (this.rule != null);
			}
			
			private Rule getRule() {
				return this.rule;
			}
			
			private boolean isDirty() {
				return this.editor.isDirty();
			}
		}
		
		private class RuleEditorPanel extends JPanel implements DocumentListener {
			
			private static final int MAX_SCROLLBAR_WAIT = 200;
			
			private JTextArea editor = new JTextArea();
			private JScrollPane editorBox;
			
			private String content = "";
			private boolean editorDirty = false;
			
			private JComboBox divTypeSelector;
			private String divType = TsConfigDialog.this.defaultDivType;
			
			private JDialog parentDialog;
			
			private RuleEditorPanel(JDialog parentDialog) {
				super(new BorderLayout(), true);
				this.parentDialog = parentDialog;
				
				//	initialize editor
				this.editor.setEditable(true);
				this.editor.getDocument().addDocumentListener(this);
				
				//	wrap editor in scroll pane
				this.editorBox = new JScrollPane(this.editor);
				
				//	initialize div type selector
				this.divTypeSelector = new JComboBox(TREATMENT_DIV_TYPES);
				this.divTypeSelector.addItem(previousRuleLabel);
				this.divTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.divTypeSelector.setSelectedItem(this.divType);
				
				//	initialize buttons
				JButton refreshButton = new JButton("Refresh");
				refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
				refreshButton.setPreferredSize(new Dimension(115, 21));
				refreshButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						refreshPredicate();
					}
				});
				
				JButton validateButton = new JButton("Validate");
				validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
				validateButton.setPreferredSize(new Dimension(115, 21));
				validateButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						validateRule();
					}
				});
				
				JButton testButton = new JButton("Test");
				testButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testButton.setPreferredSize(new Dimension(115, 21));
				testButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						testRule();
					}
				});
				
				JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.weightx = 1;
				gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridy = 0;
				gbc.gridx = 0;
				buttonPanel.add(refreshButton, gbc.clone());
				gbc.gridx ++;
				buttonPanel.add(validateButton, gbc.clone());
				gbc.gridx ++;
				buttonPanel.add(testButton, gbc.clone());
				gbc.gridx ++;
				buttonPanel.add(new JLabel("Div Type", JLabel.RIGHT), gbc.clone());
				gbc.gridx ++;
				buttonPanel.add(this.divTypeSelector, gbc.clone());
				
				//	put the whole stuff together
				this.add(this.editorBox, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
			}
			
			private Rule getContent() {
				if (this.isDirty()) this.content = GPath.normalizePath(this.editor.getText());
				String divType = this.divTypeSelector.getSelectedItem().toString();
				if (previousRuleLabel.equals(divType)) divType = previousTypeName;
				return new Rule(this.content, divType);
			}
			
			private void setContent(Rule rule) {
				this.content = GPath.normalizePath(rule.expressionString);
				this.refreshDisplay();
				this.editorDirty = false;
				this.divTypeSelector.setSelectedItem(previousTypeName.equals(rule.type) ? previousRuleLabel : rule.type);
			}
			
			private boolean isDirty() {
				return (this.editorDirty || !this.divType.equals(this.divTypeSelector.getSelectedItem().toString()));
			}
			
			private void refreshPredicate() {
				String gPath = this.editor.getText();
				if ((gPath != null) && (gPath.length() != 0)) {
					
					final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
					final int scrollPosition = scroller.getValue();
					
					String normalizedGPath = GPath.normalizePath(gPath);
					this.editor.getDocument().removeDocumentListener(this);
					this.editor.setText(GPath.explodePath(normalizedGPath, "  "));
					this.editor.getDocument().addDocumentListener(this);
					
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							int scrollbarWaitCounter = 0;
							while (scroller.getValueIsAdjusting() && (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT)) try {
								Thread.sleep(10);
								scrollbarWaitCounter ++;
							} catch (Exception e) {}
							
							if (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT) {
								scroller.setValueIsAdjusting(true);
								scroller.setValue(scrollPosition);
								scroller.setValueIsAdjusting(false);
							}
							validate();
						}
					});
				}
			}
			
			private void validateRule() {
				boolean selected = true;
				String expression = this.editor.getSelectedText();
				if ((expression == null) || (expression.length() == 0)) {
					expression = this.editor.getText();
					selected = false;
				}
				expression = GPath.normalizePath(expression);
				String error = TsConfigDialog.this.validateRule(expression);
				
				if (error == null)
					JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is valid."), "Rule Validation", JOptionPane.INFORMATION_MESSAGE);
				else JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "Rule Validation", JOptionPane.ERROR_MESSAGE);
			}
			
			private void testRule() {
				boolean selected = true;
				String expression = this.editor.getSelectedText();
				if ((expression == null) || (expression.length() == 0)) {
					expression = this.editor.getText();
					selected = false;
				}
				expression = GPath.normalizePath(expression);
				String error = TsConfigDialog.this.validateRule(expression);
				if (error != null) {
					JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "Rule Validation", JOptionPane.ERROR_MESSAGE);
					return;
				}
				else try {
					QueriableAnnotation data = getTestDocument();
					if (data != null) {
						
						Rule rule = this.getContent();
						
						//	compile expression
						try {
							rule.predicate = GPathParser.parseExpression(expression);
							
						} catch (Exception e) {
							JOptionPane.showMessageDialog(this, ("Error compiling expression of rule:\n" + e.getMessage()), "Rule Compilation Error", JOptionPane.ERROR_MESSAGE);
							System.out.println(e.getClass().getName() + ": " + e.getMessage());
							e.printStackTrace(System.out);
							return;
						}
						
						//	apply rule
						GPathVariableResolver variables = new GPathVariableResolver();
						variables.setVariable(previousRuleNumber, new GPathString("" + (rules.size() + 2)));
						variables.setVariable(previousTypeName, new GPathString(defaultDivType));
						
						String[] divTypes = new String[testDocumentParagraphs.length];
						Arrays.fill(divTypes, null);
						int position = 0;
						if (previousTypeName.equals(rule.type))
							rule.type = defaultDivType;
						for (int p = 0; p < testDocumentParagraphs.length; p++) {
							
							//	do treatment paragraphs only
							if (isTreatmentParagraph(testDocumentParagraphs[p])) {
								variables.setVariable(positionInTreatment, new GPathNumber(position++ + 1));
								
								if (GPath.DEFAULT_ENGINE.evaluateExpression(rule.predicate, testDocumentParagraphs[p], variables).asBoolean().value) {
									divTypes[p] = rule.type;
									
									variables.setVariable(previousRuleNumber, new GPathString("" + rule.position));
									variables.setVariable(previousTypeName, new GPathString(divTypes[p]));
								}
								
								//	no rule applied, set variables & return default
								else {
									variables.setVariable(previousRuleNumber, new GPathString("" + (rules.size() + 2)));
									variables.setVariable(previousTypeName, new GPathString(defaultDivType));
								}
							}
							else position = 0;
						}
						
						//	set up rule arrays
						Rule[] rules = {rule};
						Rule[] matchRules = {rule};
						Rule[] noMatchRules = {};
						
						//	build dialog
						final JDialog td = new JDialog(this.parentDialog, ("Match Result for " + rule.toString()), true);
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
						
						//	create result display boxes
						int lastTreatmentNumber = -1;
						for (int p = 0; p < testDocumentParagraphs.length; p++) {
							Annotation paragraph = testDocumentParagraphs[p];
							if (isTreatmentParagraph(paragraph)) {
								
								//	mark start of new treatment
								int treatmentNumber = getTreatmentNumber(paragraph);
								if (treatmentNumber != lastTreatmentNumber) {
									panel.add(new JLabel("Treatment on page " + paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE)), gbc.clone());
									gbc.gridy++;
									lastTreatmentNumber = treatmentNumber;
								}
								
								//	prepare paragraph for display
								panel.add(new TsTestBox(paragraph, false, rules, ((divTypes[p] == null) ? matchRules : noMatchRules), ((divTypes[p] == null) ? "unmatched" : rule.type)), gbc.clone());
								gbc.gridy++;
							}
						}
						
						//	add OK button
						JButton continueButton = new JButton("Close");
						continueButton.setBorder(BorderFactory.createRaisedBevelBorder());
						continueButton.setPreferredSize(new Dimension(100, 21));
						continueButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								td.dispose();
							}
						});
						td.getContentPane().add(continueButton, BorderLayout.SOUTH);
						
						//	ensure dialog is closed with button
						td.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
						
						//	get feedback
						td.setSize(500, (Math.min(650, (60 * gbc.gridy) + 75)));
						td.setLocationRelativeTo(null);
						td.setVisible(true);
					}
				} catch (GPathException gpe) {
					System.out.println(gpe.getClass().getName() + ": " + gpe.getMessage());
					gpe.printStackTrace(System.out);
				}
			}
			
			private void refreshDisplay() {
				final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
				final int scrollPosition = scroller.getValue();
				
				this.editor.getDocument().removeDocumentListener(this);
				this.editor.setText(GPath.explodePath(this.content, "  "));
				this.editor.getDocument().addDocumentListener(this);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int scrollbarWaitCounter = 0;
						while (scroller.getValueIsAdjusting() && (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT)) try {
							Thread.sleep(10);
							scrollbarWaitCounter ++;
						} catch (Exception e) {}
						
						if (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT) {
							scroller.setValueIsAdjusting(true);
							scroller.setValue(scrollPosition);
							scroller.setValueIsAdjusting(false);
						}
						validate();
					}
				});
			}
			
			/** @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
			 */
			public void changedUpdate(DocumentEvent e) {
				//	attribute changes are not of interest for now
			}
			
			/** @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
			 */
			public void insertUpdate(DocumentEvent e) {
				this.editorDirty = true;
			}
			
			/** @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
			 */
			public void removeUpdate(DocumentEvent e) {
				this.editorDirty = true;
			}
		}
		
		private class TsTestBox extends JPanel {
			private TsTestBox(Annotation paragraph, boolean isMultiRuleTest, Rule[] rules, Rule[] matchRules, String divType) {
				super(new GridBagLayout(), true);
				this.setBorder(BorderFactory.createEtchedBorder());
				
				//	display classfication result
				JLabel divTypeDisplay = new JLabel(divType);
				
				//	prepare match overview for multi rule tests only
				if (isMultiRuleTest) {
					
					StringVector ruleMatchLines = new StringVector();
					int mri = 0;
					
					for (int r = 0; r < rules.length; r++) {
						String ruleString = IoTools.prepareForHtml(rules[r].toString(), new Properties() {
							public String getProperty(String key, String defaultValue) {
								if ("<>\"&".indexOf(key) == -1) return key;
								else return super.getProperty(key, defaultValue);
							}
							public String getProperty(String key) {
								if ("<>\"&".indexOf(key) == -1) return key;
								else return super.getProperty(key);
							}
							public synchronized boolean containsKey(Object key) {
								return ("<>\"&".indexOf(key.toString()) == -1);
							}
						});
						if ((mri < matchRules.length) && rules[r].toString().equals(matchRules[mri].toString())) {
							if (mri == 0) ruleMatchLines.addElement(" - <B>" + ruleString + "</B>");
							else ruleMatchLines.addElement(" - <B><I>" + ruleString + "</I></B>");
							mri++;
						}
						else ruleMatchLines.addElement(" - " + ruleString);
					}
					
					if (mri == 0) ruleMatchLines.addElement(" - <B>==>" + TsConfigDialog.this.defaultDivType + "</B>");
					else ruleMatchLines.addElement(" - ==>" + TsConfigDialog.this.defaultDivType);
					
					final String ruleMatchMessage = ("<HTML>Rule match result for this paragraph (first match bold, other matches in bold italics):<BR>" + ruleMatchLines.concatStrings("<BR>") + "</HTML>");
					
					divTypeDisplay.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent e) {
							JOptionPane.showMessageDialog(TsTestBox.this, ruleMatchMessage, "Rule Match Overview", JOptionPane.INFORMATION_MESSAGE);
						}
					});
				}
				
				String pageNumber = paragraph.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString().trim();
				
				JTextArea display = new JTextArea();
				display.setText(paragraph.getValue());
				display.setLineWrap(true);
				display.setWrapStyleWord(true);
				display.setEditable(false);
				JScrollPane displayBox = new JScrollPane(display);
				
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
				this.add(divTypeDisplay, gbc.clone());
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
				display.setBackground(getColorForDivType(divType));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		
		//	test if configuration changed
		if (this.configurationModified) {
			
			//	store default div type
			this.storeParameter("defaultDivType", this.defaultDivType);
			
			//	collect string representation of rules
			StringVector ruleStrings = new StringVector();
			for (int r = 0; r < this.rules.size(); r++) {
				Rule rule = ((Rule) this.rules.get(r));
				ruleStrings.addElement(rule.toDataString());
			}
			
			//	store rules
			try {
				OutputStream os = dataProvider.getOutputStream("treatmentStructurerRules.txt");
				ruleStrings.storeContent(os);
				os.flush();
				os.close();
			} catch (IOException ioe) {}
		}
	}
}
