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
//package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat;
//
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Dialog;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.awt.Font;
//import java.awt.Frame;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.LayoutManager;
//import java.awt.Window;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.FocusAdapter;
//import java.awt.event.FocusEvent;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.Reader;
//import java.io.StringReader;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//import java.util.Properties;
//
//import javax.swing.BorderFactory;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JComponent;
//import javax.swing.JDialog;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JSpinner;
//import javax.swing.JTabbedPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JTextField;
//import javax.swing.SpinnerNumberModel;
//import javax.swing.UIManager;
//import javax.swing.border.Border;
//import javax.swing.event.TableModelListener;
//import javax.swing.table.JTableHeader;
//import javax.swing.table.TableModel;
//
//import de.uka.ipd.idaho.gamta.Annotation;
//import de.uka.ipd.idaho.gamta.DocumentRoot;
//import de.uka.ipd.idaho.gamta.Gamta;
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.MutableTokenSequence;
//import de.uka.ipd.idaho.gamta.QueriableAnnotation;
//import de.uka.ipd.idaho.gamta.Token;
//import de.uka.ipd.idaho.gamta.TokenSequence;
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
//import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
//import de.uka.ipd.idaho.gamta.util.TestDocumentProvider;
//import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
//import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
//import de.uka.ipd.idaho.htmlXmlUtil.Parser;
//import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//import de.uka.ipd.idaho.stringUtils.accessories.RegExEditorPanel;
//import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
//import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;
//import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;
//
///**
// * @author sautter
// *
// */
//public class OmniFatInstanceEditor extends JPanel {
//	
//	private static final Dimension buttonSize = new Dimension(100, 21);
//	private static final Dimension tableButtonSize = new Dimension(70, 21);
//	
//	private abstract class OmniFatInstanceEditorDetailPanel extends JPanel {
//		public OmniFatInstanceEditorDetailPanel() {
//			super(new BorderLayout(), true);
//		}
//		public OmniFatInstanceEditorDetailPanel(LayoutManager layout) {
//			super(layout, true);
//		}
//		
//		abstract boolean checkConsistency();
//		
//		abstract TreeNode[] getTreeNodes(HashSet filter);
//	}
//	
//	private static final String[] INTERNAL_LIST_SEPARATORS = {";", ",", "|", "!", "/", "\\", "-"};
//	
//	private class InternalListPanel extends OmniFatInstanceEditorDetailPanel {
//		
//		String type;
//		JComboBox separator;
//		JTextArea listEntries;
//		
//		JPanel topPanel = new JPanel(new BorderLayout(), true);
//		
//		InternalListPanel(String label, String type, String separator, StringVector listEntries) {
//			super(new BorderLayout());
//			this.type = type;
//			
//			this.separator = new JComboBox(INTERNAL_LIST_SEPARATORS);
//			this.separator.setEditable(false);
//			this.separator.setSelectedItem(separator);
//			if (!separator.equals(this.separator.getSelectedItem())) {
//				this.separator.addItem(separator);
//				this.separator.setSelectedItem(separator);
//			}
//			
//			this.listEntries = new JTextArea(listEntries.concatStrings("\n"));
//			
//			this.topPanel.add(new JLabel(label, JLabel.LEFT), BorderLayout.WEST);
//			this.topPanel.add(new JLabel("Separator", JLabel.RIGHT), BorderLayout.CENTER);
//			this.topPanel.add(this.separator, BorderLayout.EAST);
//			
//			this.add(this.topPanel, BorderLayout.NORTH);
//			JScrollPane listEntryBox = new JScrollPane(this.listEntries);
//			listEntryBox.getVerticalScrollBar().setUnitIncrement(25);
//			this.add(listEntryBox, BorderLayout.CENTER);
//		}
//		
//		String getSeparator() {
//			return ("" + this.separator.getSelectedItem());
//		}
//		
//		StringVector getListEntries() {
//			StringVector listEntries = new StringVector();
//			BufferedReader listEntryReader = new BufferedReader(new StringReader(this.listEntries.getText()));
//			try {
//				String listEntry;
//				while ((listEntry = listEntryReader.readLine()) != null)
//					listEntries.addElementIgnoreDuplicates(listEntry);
//			} catch (IOException ioe) {/*will never happen, but Java don't know*/}
////			listEntries.addContentIgnoreDuplicates(this.listEntries.getText().split("[\\n\\r]+"));
//			return listEntries;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			String separator = ("" + this.separator.getSelectedItem());
//			StringVector listEntries = this.getListEntries();
//			String listEntry;
//			for (int e = 0; e < listEntries.size(); e++) {
//				listEntry = listEntries.get(e);
//				if (listEntry.indexOf(separator) != -1) {
//					Object seperatorObject = JOptionPane.showInputDialog(this, ("'" + separator + "' is not a valid seperator for the '" + this.type + "' list,\nsince it occurs in twe list entry '" + listEntry + "'.\nPlease select another separator."), "Invalid Separator", JOptionPane.ERROR_MESSAGE, null, INTERNAL_LIST_SEPARATORS, separator);
//					if (seperatorObject == null)
//						return false;
//					else {
//						this.separator.setSelectedItem(seperatorObject);
//						return this.checkConsistency();
//					}
//				}
//			}
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			String separator = this.getSeparator();
//			StringVector listEntries = this.getListEntries();
//			TreeNode[] nodes = {new TreeNode(null, this.type)};
//			nodes[0].setAttribute("separator", separator);
//			nodes[0].addChildNode(new TreeNode(nodes[0], TreeNode.DATA_NODE_TYPE, listEntries.concatStrings(separator)));
//			return nodes;
//		}
//	}
//	
//	private class BaseDataPanel extends OmniFatInstanceEditorDetailPanel {
//		JTextArea description;
//		
//		JSpinner fuzzyMatchThreshold;
//		
//		JSpinner minAbbreviationLength;
//		JSpinner maxAbbreviationLength;
//		
//		InternalListPanel newEpithetLabels;
//		
//		InternalListPanel intraEpithetPunctuation;
//		InternalListPanel interEpithetPunctuation;
//		
//		DictionaryTablePanel negativeDictionaryList;
//		PatternTablePanel negativePatternList;
//		JTabbedPane stemmingRules;
//		
//		JTabbedPane tabs;
//		
//		BaseDataPanel(TreeNode configNode, String[] dictionaryNames, String[] patternNames) {
//			super();
//			
//			//	load description
//			this.description = new JTextArea();
//			TreeNode[] descriptionNodes = configNode.getChildNodes("description");
//			if (descriptionNodes.length != 0) {
//				TreeNode descriptionNode = descriptionNodes[0].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (descriptionNode != null)
//					this.description.setText(descriptionNode.getNodeValue());
//			}
//			this.description.setLineWrap(true);
//			this.description.setWrapStyleWord(true);
//			
//			//	load fuzzy match threshold
//			this.fuzzyMatchThreshold = new JSpinner(new SpinnerNumberModel(2, 0, 5, 1));
//			TreeNode[] fuzzyMatchNodes = configNode.getChildNodes("fuzzyMatch");
//			if (fuzzyMatchNodes.length == 1)
//				this.fuzzyMatchThreshold.setValue(new Integer(fuzzyMatchNodes[0].getAttribute("threshold", "2")));
//			
//			//	load abbreviation length boundaries
//			this.minAbbreviationLength = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
//			this.maxAbbreviationLength = new JSpinner(new SpinnerNumberModel(2, 1, 5, 1));
//			TreeNode[] abbreviationLengthInLettersNodes = configNode.getChildNodes("abbreviationLengthInLetters");
//			if (abbreviationLengthInLettersNodes.length == 1) {
//				this.minAbbreviationLength.setValue(new Integer(abbreviationLengthInLettersNodes[0].getAttribute("min", "1")));
//				this.maxAbbreviationLength.setValue(new Integer(abbreviationLengthInLettersNodes[0].getAttribute("max", "2")));
//			}
//			
//			//	load new epithet labels
//			TreeNode[] newEpithetLabelExtensionNodes = configNode.getChildNodes("newEpithetLabels");
//			StringVector newEpithetLabelExtensionList = new StringVector();
//			String newEpithetLabelExtensionListSeparator = ";";
//			for (int nl = 0; nl < newEpithetLabelExtensionNodes.length; nl++) {
//				newEpithetLabelExtensionListSeparator = newEpithetLabelExtensionNodes[nl].getAttribute("separator", ";");
//				TreeNode dataNode = newEpithetLabelExtensionNodes[nl].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					newEpithetLabelExtensionList.parseAndAddElements(dataNode.getNodeValue(), newEpithetLabelExtensionListSeparator);
//			}
//			newEpithetLabelExtensionList.removeDuplicateElements();
//			this.newEpithetLabels = new InternalListPanel("Labels for new Epithets", "newEpithetLabels", newEpithetLabelExtensionListSeparator, newEpithetLabelExtensionList);
//			
//			//	load intra epithet punctuation
//			TreeNode[] intraEpithetPunctuationNodes = configNode.getChildNodes("intraEpithetPunctuation");
//			StringVector intraEpithetPunctuationMarks = new StringVector();
//			String intraEpithetPunctuationMarkSeparator = ";";
//			for (int pn = 0; pn < intraEpithetPunctuationNodes.length; pn++) {
//				intraEpithetPunctuationMarkSeparator = intraEpithetPunctuationNodes[pn].getAttribute("separator", ";");
//				TreeNode dataNode = intraEpithetPunctuationNodes[pn].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					intraEpithetPunctuationMarks.parseAndAddElements(dataNode.getNodeValue(), intraEpithetPunctuationMarkSeparator);
//			}
//			intraEpithetPunctuationMarks.removeDuplicateElements();
//			this.intraEpithetPunctuation = new InternalListPanel("Punctuation Marks within Epithets", "intraEpithetPunctuation", intraEpithetPunctuationMarkSeparator, intraEpithetPunctuationMarks);
//			
//			//	load inter epithet punctuation
//			TreeNode[] interEpithetPunctuationNodes = configNode.getChildNodes("interEpithetPunctuation");
//			StringVector interEpithetPunctuationMarks = new StringVector();
//			String interEpithetPunctuationMarkSeparator = ";";
//			for (int pn = 0; pn < interEpithetPunctuationNodes.length; pn++) {
//				interEpithetPunctuationMarkSeparator = interEpithetPunctuationNodes[pn].getAttribute("separator", ";");
//				TreeNode dataNode = interEpithetPunctuationNodes[pn].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					interEpithetPunctuationMarks.parseAndAddElements(dataNode.getNodeValue(), interEpithetPunctuationMarkSeparator);
//			}
//			interEpithetPunctuationMarks.removeDuplicateElements();
//			this.interEpithetPunctuation = new InternalListPanel("Punctuation Marks between Epithets", "interEpithetPunctuation", interEpithetPunctuationMarkSeparator, interEpithetPunctuationMarks);
//			
//			//	load negative & stop word dictionaries
//			TreeNode[] negativeDictionaryNodes = configNode.getChildNodes("dictionary");
//			ArrayList negativeDictionaryNodeList = new ArrayList();
//			for (int nd = 0; nd < negativeDictionaryNodes.length; nd++) {
//				if ("stopWord".equals(negativeDictionaryNodes[nd].getAttribute("type")))
//					negativeDictionaryNodeList.add(negativeDictionaryNodes[nd]);
//				else if ("negative".equals(negativeDictionaryNodes[nd].getAttribute("type")))
//					negativeDictionaryNodeList.add(negativeDictionaryNodes[nd]);
//			}
//			String[] dictionaryTypes = {"negative", "stopWord"};
//			this.negativeDictionaryList = new DictionaryTablePanel(dictionaryNames, dictionaryTypes, true, ((TreeNode[]) negativeDictionaryNodeList.toArray(new TreeNode[negativeDictionaryNodeList.size()])));
//			
//			//	load negative patterns
//			TreeNode[] negativePatternNodes = configNode.getChildNodes("pattern");
//			String[] patternTypes = {"negative"};
//			this.negativePatternList = new PatternTablePanel(patternNames, patternTypes, negativePatternNodes);
//			
//			
//			//	load stemming rules
//			this.stemmingRules = new JTabbedPane();
//			TreeNode[] stemmingRuleNodes = configNode.getChildNodes("stemmingRule");
//			for (int sr = 0; sr < stemmingRuleNodes.length; sr++) {
//				String matchEnding = stemmingRuleNodes[sr].getAttribute("matchEnding");
//				String separator = stemmingRuleNodes[sr].getAttribute("separator", ";");
//				StringVector stemmedEndings = new StringVector();
//				TreeNode dataNode = stemmingRuleNodes[sr].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					stemmedEndings.parseAndAddElements(dataNode.getNodeValue(), separator);
//				final StemmingRulePanel srp = new StemmingRulePanel(matchEnding, separator, stemmedEndings);
//				srp.matchEnding.addFocusListener(new FocusAdapter() {
//					public void focusLost(FocusEvent fe) {
//						int sri = stemmingRules.indexOfComponent(srp);
//						if (sri != -1)
//							stemmingRules.setTitleAt(sri, ("-" + srp.matchEnding.getText()));
//					}
//				});
//				this.stemmingRules.addTab(("-" + matchEnding), srp);
//			}
//			
//			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
//			JButton button;
//			button = new JButton("Add Stemming Rule");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					final StemmingRulePanel srp = new StemmingRulePanel("", ";", new StringVector());
//					srp.matchEnding.addFocusListener(new FocusAdapter() {
//						public void focusLost(FocusEvent fe) {
//							int sri = stemmingRules.indexOfComponent(srp);
//							if (sri != -1)
//								stemmingRules.setTitleAt(sri, ("-" + srp.matchEnding.getText()));
//						}
//					});
//					stemmingRules.addTab(("-"), srp);
//					stemmingRules.setSelectedComponent(srp);
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Remove Stemming Rule");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					int index = stemmingRules.getSelectedIndex();
//					if (index != -1)
//						stemmingRules.removeTabAt(index);
//				}
//			});
//			buttonPanel.add(button);
//			
//			JPanel stemmingRulePanel = new JPanel(new BorderLayout(), true);
//			stemmingRulePanel.add(buttonPanel, BorderLayout.NORTH);
//			stemmingRulePanel.add(this.stemmingRules, BorderLayout.CENTER);
//			
//			
//			//	layout the whole stuff
//			JPanel topPanel = new JPanel(new GridBagLayout(), true);
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 3;
//			gbc.insets.bottom = 3;
//			gbc.insets.left = 3;
//			gbc.insets.right = 3;
//			gbc.weighty = 0;
//			gbc.fill = GridBagConstraints.BOTH;
//			gbc.gridheight = 1;
//			gbc.gridy = 0;
//			
//			gbc.gridx = 0;
//			gbc.gridwidth = 6;
//			topPanel.add(new JLabel("OmniFAT Instance Description", JLabel.LEFT), gbc.clone());
//			gbc.gridy++;
//			
//			gbc.gridx = 0;
//			gbc.gridwidth = 6;
//			JScrollPane descriptionBox = new JScrollPane(this.description);
//			descriptionBox.setPreferredSize(new Dimension(200, 100));
//			topPanel.add(descriptionBox, gbc.clone());
//			gbc.gridy++;
//			
//			gbc.gridwidth = 1;
//			gbc.gridx = 0;
//			gbc.weightx = 1;
//			topPanel.add(new JLabel("Fuzzy Match Threshold", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.fuzzyMatchThreshold, gbc.clone());
//			gbc.gridx++;
//			topPanel.add(new JLabel("Minimum Abbreviation Length", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.minAbbreviationLength, gbc.clone());
//			gbc.gridx++;
//			topPanel.add(new JLabel("Maximum Abbreviation Length", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.maxAbbreviationLength, gbc.clone());
//			
//			this.tabs = new JTabbedPane();
//			this.tabs.setTabPlacement(JTabbedPane.LEFT);
//			this.tabs.addTab("New Epithet Labels", this.newEpithetLabels);
//			
//			this.tabs.addTab("Intra Epithet Punctuation", this.intraEpithetPunctuation);
//			this.tabs.addTab("Inter Epithet Punctuation", this.interEpithetPunctuation);
//			
//			this.tabs.addTab("Negative Dictionaries", this.negativeDictionaryList);
//			this.tabs.addTab("Negative Patterns", this.negativePatternList);
//			this.tabs.addTab("Stemming Lookup Rules", stemmingRulePanel);
//			
//			this.add(topPanel, BorderLayout.NORTH);
//			this.add(this.tabs, BorderLayout.CENTER);
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			if (!this.newEpithetLabels.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.newEpithetLabels);
//				return false;
//			}
//			
//			if (!this.intraEpithetPunctuation.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.intraEpithetPunctuation);
//				return false;
//			}
//			if (!this.interEpithetPunctuation.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.interEpithetPunctuation);
//				return false;
//			}
//			
//			if (!this.negativeDictionaryList.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.negativeDictionaryList);
//				return false;
//			}
//			if (!this.negativePatternList.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.negativePatternList);
//				return false;
//			}
//			
//			int minAbbreviationLength = Integer.parseInt(this.minAbbreviationLength.getValue().toString());
//			int maxAbbreviationLength = Integer.parseInt(this.maxAbbreviationLength.getValue().toString());
//			if (maxAbbreviationLength < minAbbreviationLength) {
//				JOptionPane.showMessageDialog(this, ("Maximum abbreviation length must be equal to or greater than minimum abbreviation length."), "Invalid Abbreviation Length Settings", JOptionPane.ERROR_MESSAGE);
//				return false;
//			}
//			
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			ArrayList nodes = new ArrayList();
//			
//			TreeNode description = new TreeNode(null, "description");
//			description.addChildNode(new TreeNode(description, TreeNode.DATA_NODE_TYPE, this.description.getText()));
//			nodes.add(description);
//			
//			TreeNode fuzzyMatch = new TreeNode(null, "fuzzyMatch");
//			fuzzyMatch.setAttribute("threshold", ("" + this.fuzzyMatchThreshold.getValue()));
//			nodes.add(fuzzyMatch);
//			
//			TreeNode abbreviationLengthInLetters = new TreeNode(null, "abbreviationLengthInLetters");
//			abbreviationLengthInLetters.setAttribute("min", ("" + this.minAbbreviationLength.getValue()));
//			abbreviationLengthInLetters.setAttribute("max", ("" + this.maxAbbreviationLength.getValue()));
//			nodes.add(abbreviationLengthInLetters);
//			
//			nodes.addAll(Arrays.asList(this.newEpithetLabels.getTreeNodes(filter)));
//			
//			nodes.addAll(Arrays.asList(this.intraEpithetPunctuation.getTreeNodes(filter)));
//			nodes.addAll(Arrays.asList(this.interEpithetPunctuation.getTreeNodes(filter)));
//			
//			nodes.addAll(Arrays.asList(this.negativeDictionaryList.getTreeNodes(filter)));
//			nodes.addAll(Arrays.asList(this.negativePatternList.getTreeNodes(filter)));
//			
//			for (int sr = 0; sr < this.stemmingRules.getTabCount(); sr++) {
//				StemmingRulePanel srp = ((StemmingRulePanel) this.stemmingRules.getComponentAt(sr));
//				nodes.addAll(Arrays.asList(srp.getTreeNodes(filter)));
//			}
//			
//			return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
//		}
//	}
//	
//	private class StemmingRulePanel extends InternalListPanel {
//		JTextField matchEnding;
//		StemmingRulePanel(String matchEnding, String separator, StringVector listEntries) {
//			super("", "stemmingRule", separator, listEntries);
//			
//			this.matchEnding = new JTextField(matchEnding);
//			this.matchEnding.setPreferredSize(new Dimension(30, this.matchEnding.getPreferredSize().height));
//			
//			JPanel matchEndingPanel = new JPanel(new BorderLayout());
//			matchEndingPanel.add(new JLabel("Match Ending"), BorderLayout.WEST);
//			matchEndingPanel.add(this.matchEnding, BorderLayout.CENTER);
//			this.topPanel.add(matchEndingPanel, BorderLayout.WEST);
//		}
//		
//		String getMatchEnding() {
//			return this.matchEnding.getText();
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.InternalListPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			if (!super.checkConsistency())
//				return false;
//			
//			StringVector stemmedEndings = this.getListEntries();
//			if (stemmedEndings.isEmpty()) {
//				JOptionPane.showMessageDialog(this, ("The list of stemmed endings must not be empty."), "No Stemmed Endings", JOptionPane.ERROR_MESSAGE);
//				return false;
//			}
//			
//			String matchEnding = this.getMatchEnding();
//			if ((matchEnding.length() == 0) || !matchEnding.matches("[a-zA-Z]++")) {
//				JOptionPane.showMessageDialog(this, ("The match ending must consist of letters only, at least one letter."), "Empty Match Ending", JOptionPane.ERROR_MESSAGE);
//				return false;
//			}
//			
//			if (stemmedEndings.containsIgnoreCase(matchEnding)) {
//				JOptionPane.showMessageDialog(this, ("The list of stemmed endings must not contain the match ending."), "Match Ending in Stemmed Ending List", JOptionPane.ERROR_MESSAGE);
//				return false;
//			}
//			
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.InternalListPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			TreeNode[] nodes = super.getTreeNodes(filter);
//			for (int n = 0; n < nodes.length; n++)
//				nodes[n].setAttribute("matchEnding", this.getMatchEnding());
//			return nodes;
//		}
//	}
//	
//	private static final String[] EPITHET_CAPITALIZATION_MODES = {"always", "mixed", "never"};
//	private static final String[] EPITHET_LEARNING_MODES = {"auto", "teach", "off"};
//	
//	private static final String[] EPITHET_DICTIONARY_TYPES = {"positive", "negative"};
//	private static final String[] EPITHET_PATTERN_TYPES = {"precision", "recall", "negative"};
//	
//	private static final String[] EPITHET_LABELING_MODES = {"always", "sometimes", "never"};
//	
//	private class RankGroupPanel extends OmniFatInstanceEditorDetailPanel {
//		
//		private class RankPanel extends OmniFatInstanceEditorDetailPanel {
//			
//			JTextField name;
//			JComboBox labeled;
//			JCheckBox required;
//			JTextField epithetDisplayPattern;
//			JSpinner probability;
//			
//			InternalListPanel labelList;
//			
//			DictionaryTablePanel dictionaryList;
//			PatternTablePanel patternList;
//			
//			JTabbedPane tabs;
//			
//			RankPanel(TreeNode rankNode, String[] dictionaryNames, String[] patternNames) {
//				
//				//	load name
//				this.name = new JTextField(rankNode.getAttribute("name", ""));
//				
//				//	check epithet labeling mode
//				this.labeled = new JComboBox(EPITHET_LABELING_MODES);
//				this.labeled.setSelectedItem(rankNode.getAttribute("labeled", "sometimes"));
//				
//				//	check if rank is required if epithets above and below this rank are present in a taxon name
//				this.required = new JCheckBox("", "true".equals(rankNode.getAttribute("required", "false")));
//				
//				//	load rank probability
//				this.probability = new JSpinner(new SpinnerNumberModel(5, 0, 10, 1));
//				this.probability.setValue(new Integer(rankNode.getAttribute("probability", "5")));
//				
//				//	load epithet labels
//				TreeNode[] labelNodes;
//				String labelSeparator = ";";
//				StringVector labels = new StringVector();
//				
//				//	individual labels and label lists
//				labelNodes = rankNode.getChildNodes("label");
//				for (int l = 0; l < labelNodes.length; l++) {
//					String label = labelNodes[l].getAttribute("value");
//					if (label != null)
//						labels.addElementIgnoreDuplicates(label);
//				}
//				labelNodes = rankNode.getChildNodes("labels");
//				for (int l = 0; l < labelNodes.length; l++) {
//					labelSeparator = labelNodes[l].getAttribute("separator", ";");
//					TreeNode dataNode =labelNodes[l].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//					if (dataNode != null)
//						labels.parseAndAddElements(dataNode.getNodeValue(), labelSeparator);
//				}
//				labels.removeDuplicateElements();
//				this.labelList = new InternalListPanel("Epithet Labels", "labels", labelSeparator, labels);
//				
//				//	read epithet display pattern
//				this.epithetDisplayPattern = new JTextField(rankNode.getAttribute("epithetDisplayPattern", "@epithet"));
//				
//				//	load dictionaries
//				TreeNode[] dictionaryNodes = rankNode.getChildNodes("dictionary");
//				this.dictionaryList = new DictionaryTablePanel(dictionaryNames, EPITHET_DICTIONARY_TYPES, false, dictionaryNodes);
//				
//				//	load patterns
//				TreeNode[] patternNodes = rankNode.getChildNodes("pattern");
//				this.patternList = new PatternTablePanel(patternNames, EPITHET_PATTERN_TYPES, patternNodes);
//				
//				
//				//	layout the whole stuff
//				JPanel topPanel = new JPanel(new GridBagLayout(), true);
//				GridBagConstraints gbc = new GridBagConstraints();
//				gbc.insets.top = 3;
//				gbc.insets.bottom = 3;
//				gbc.insets.left = 3;
//				gbc.insets.right = 3;
//				gbc.weighty = 0;
//				gbc.fill = GridBagConstraints.BOTH;
//				gbc.gridwidth = 1;
//				gbc.gridheight = 1;
//				gbc.gridy = 0;
//				
//				gbc.gridx = 0;
//				gbc.weightx = 1;
//				topPanel.add(new JLabel("Rank Name", JLabel.RIGHT), gbc.clone());
//				gbc.gridx++;
//				topPanel.add(this.name, gbc.clone());
//				gbc.gridx++;
//				topPanel.add(new JLabel("Labeled", JLabel.RIGHT), gbc.clone());
//				gbc.gridx++;
//				topPanel.add(this.labeled, gbc.clone());
//				gbc.gridx++;
//				topPanel.add(new JLabel("Required", JLabel.RIGHT), gbc.clone());
//				gbc.gridx++;
//				topPanel.add(this.required, gbc.clone());
//				gbc.gridy++;
//				
//				gbc.gridx = 0;
//				JButton baseEpithetTestButton = new JButton("Test to Base Epithets");
//				baseEpithetTestButton.setPreferredSize(buttonSize);
//				baseEpithetTestButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (OmniFatInstanceEditor.this.checkConsistency()) {
//							TreeNode configNode = getConfigNode(new HashSet() {
//								public boolean contains(Object o) {
//									String s = ((String) o);
//									return (!s.startsWith("rank") || ("rankGroup." + getRankGroupName() + "." + getRankName()).startsWith(s));
//								}
//							});
//							testInstance("RankTest", adp, configNode, OmniFAT.BASE_EPITHET_TYPE, ("Rank '" + getRankName() + "'"));
//						}
//					}
//				});
//				topPanel.add(baseEpithetTestButton, gbc.clone());
//				gbc.gridx++;
//				
//				JButton epithetTestButton = new JButton("Test to Epithets");
//				epithetTestButton.setPreferredSize(buttonSize);
//				epithetTestButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (OmniFatInstanceEditor.this.checkConsistency()) {
//							TreeNode configNode = getConfigNode(new HashSet() {
//								public boolean contains(Object o) {
//									String s = ((String) o);
//									return (!s.startsWith("rank") || ("rankGroup." + getRankGroupName() + "." + getRankName()).startsWith(s));
//								}
//							});
//							testInstance("RankTest", adp, configNode, OmniFAT.EPITHET_TYPE, ("Rank '" + getRankName() + "'"));
//						}
//					}
//				});
//				topPanel.add(epithetTestButton, gbc.clone());
//				gbc.gridx++;
//				
//				topPanel.add(new JLabel("Display Pattern", JLabel.RIGHT), gbc.clone());
//				gbc.gridx++;
//				topPanel.add(this.epithetDisplayPattern, gbc.clone());
//				gbc.gridx++;
//				topPanel.add(new JLabel("Probability", JLabel.RIGHT), gbc.clone());
//				gbc.gridx++;
//				topPanel.add(this.probability, gbc.clone());
//				
//				
//				this.tabs = new JTabbedPane();
//				this.tabs.addTab("Epithet Labels", this.labelList);
//				this.tabs.addTab("Epithet Dictionaries", this.dictionaryList);
//				this.tabs.addTab("Patterns", this.patternList);
//				
//				this.add(topPanel, BorderLayout.NORTH);
//				this.add(this.tabs, BorderLayout.CENTER);
//			}
//			
//			String getRankName() {
//				return this.name.getText();
//			}
//			
//			/* (non-Javadoc)
//			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//			 */
//			boolean checkConsistency() {
//				if (!this.labelList.checkConsistency()) {
//					this.tabs.setSelectedComponent(this.labelList);
//					return false;
//				}
//				
//				if (!this.dictionaryList.checkConsistency()) {
//					this.tabs.setSelectedComponent(this.dictionaryList);
//					return false;
//				}
//				if (!this.patternList.checkConsistency()) {
//					this.tabs.setSelectedComponent(this.patternList);
//					return false;
//				}
//				
//				String name = this.getRankName();
//				if ((name.length() == 0) || !name.matches("[a-zA-Z]++")) {
//					JOptionPane.showMessageDialog(this, ("The name of a rank must consist of letters only, at least one letter."), "Invalid Rank Name", JOptionPane.ERROR_MESSAGE);
//					return false;
//				}
//				
//				return true;
//			}
//			
//			/* (non-Javadoc)
//			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//			 */
//			TreeNode[] getTreeNodes(HashSet filter) {
//				TreeNode[] nodes = {new TreeNode(null, "rank")};
//				
//				nodes[0].setAttribute("name", this.name.getText());
//				
//				nodes[0].setAttribute("labeled", ("" + this.labeled.getSelectedItem()));
//				
//				nodes[0].setAttribute("required", (this.required.isSelected() ? "true" : "false"));
//				
//				nodes[0].setAttribute("epithetDisplayPattern", this.epithetDisplayPattern.getText());
//				
//				nodes[0].setAttribute("probability", ("" + this.probability.getValue()));
//				
//				TreeNode[] labelNodes = this.labelList.getTreeNodes(filter);
//				for (int l = 0; l < labelNodes.length; l++) {
//					nodes[0].addChildNode(labelNodes[l]);
//					labelNodes[l].setParent(nodes[0]);
//				}
//				
//				TreeNode[] dictionaryNodes = this.dictionaryList.getTreeNodes(filter);
//				for (int d = 0; d < dictionaryNodes.length; d++) {
//					nodes[0].addChildNode(dictionaryNodes[d]);
//					dictionaryNodes[d].setParent(nodes[0]);
//				}
//				
//				TreeNode[] patternNodes = this.patternList.getTreeNodes(filter);
//				for (int p = 0; p < patternNodes.length; p++) {
//					nodes[0].addChildNode(patternNodes[p]);
//					patternNodes[p].setParent(nodes[0]);
//				}
//				
//				return nodes;
//			}
//		}
//		
//		JTextField name;
//		
//		JComboBox capitalized;
//		JCheckBox repeatedEpithets;
//		
//		JCheckBox suffixDiff;
//		JComboBox learningMode;
//		
//		DictionaryTablePanel dictionaryList;
//		PatternTablePanel patternList;
//		
//		JTabbedPane ranks;
//		JPanel rankPanel;
//		
//		JTabbedPane tabs;
//		
//		RankGroupPanel(TreeNode rankGroupNode, final String[] dictionaryNames, final String[] patternNames) {
//			
//			//	load name
//			this.name = new JTextField(rankGroupNode.getAttribute("name", ""));
//			
//			//	check epithet capitalization mode
//			this.capitalized = new JComboBox(EPITHET_CAPITALIZATION_MODES);
//			this.capitalized.setSelectedItem(rankGroupNode.getAttribute("capitalized", "mixed"));
//			
//			//	check epithet repetitions
//			this.repeatedEpithets = new JCheckBox("", "true".equals(rankGroupNode.getAttribute("repeatedEpithets", "false")));
//			
//			//	check learning mode
//			this.learningMode = new JComboBox(EPITHET_LEARNING_MODES);
//			this.learningMode.setSelectedItem(rankGroupNode.getAttribute("learningMode", "teach"));
//			
//			//	check suffix diff setting & prepare suffixes
//			this.suffixDiff = new JCheckBox("", "true".equals(rankGroupNode.getAttribute("doSuffixDiff", "false")));
//			
//			//	load dictionaries
//			TreeNode[] dictionaryNodes = rankGroupNode.getChildNodes("dictionary");
//			this.dictionaryList = new DictionaryTablePanel(dictionaryNames, EPITHET_DICTIONARY_TYPES, false, dictionaryNodes);
//			
//			//	load patterns
//			TreeNode[] patternNodes = rankGroupNode.getChildNodes("pattern");
//			this.patternList = new PatternTablePanel(patternNames, EPITHET_PATTERN_TYPES, patternNodes);
//			
//			//	add ranks
//			this.ranks = new JTabbedPane();
//			this.ranks.setTabPlacement(JTabbedPane.LEFT);
//			TreeNode[] rankNodes = rankGroupNode.getChildNodes("rank");
//			for (int r = 0; r < rankNodes.length; r++) {
//				final RankPanel rp = new RankPanel(rankNodes[r], dictionaryNames, patternNames);
//				rp.name.addFocusListener(new FocusAdapter() {
//					public void focusLost(FocusEvent fe) {
//						int sri = ranks.indexOfComponent(rp);
//						if (sri != -1)
//							ranks.setTitleAt(sri, rp.name.getText());
//					}
//				});
//				this.ranks.addTab(rp.name.getText(), rp);
//			}
//			
//			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
//			JButton button;
//			button = new JButton("Add Rank");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					final RankPanel rp = new RankPanel(new TreeNode(null, "rank"), dictionaryNames, patternNames);
//					rp.name.addFocusListener(new FocusAdapter() {
//						public void focusLost(FocusEvent fe) {
//							int sri = ranks.indexOfComponent(rp);
//							if (sri != -1)
//								ranks.setTitleAt(sri, rp.name.getText());
//						}
//					});
//					ranks.addTab(rp.name.getText(), rp);
//					ranks.setSelectedComponent(rp);
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Remove Rank");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					int index = ranks.getSelectedIndex();
//					if (index != -1)
//						ranks.removeTabAt(index);
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Move Up");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					int index = ranks.getSelectedIndex();
//					if (index > 0) {
//						RankPanel rp = ((RankPanel) ranks.getComponentAt(index));
//						ranks.removeTabAt(index);
//						ranks.insertTab(rp.name.getText(), null, rp, null, (index-1));
//						ranks.setSelectedIndex(index-1);
//					}
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Move Down");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					int index = ranks.getSelectedIndex();
//					if ((index+1) < ranks.getTabCount()) {
//						RankPanel rp = ((RankPanel) ranks.getComponentAt(index));
//						ranks.removeTabAt(index);
//						ranks.insertTab(rp.name.getText(), null, rp, null, (index+1));
//						ranks.setSelectedIndex(index+1);
//					}
//				}
//			});
//			buttonPanel.add(button);
//			
//			this.rankPanel = new JPanel(new BorderLayout(), true);
//			this.rankPanel.add(buttonPanel, BorderLayout.NORTH);
//			this.rankPanel.add(this.ranks, BorderLayout.CENTER);
//			
//			
//			//	layout the whole stuff
//			JPanel topPanel = new JPanel(new GridBagLayout(), true);
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 3;
//			gbc.insets.bottom = 3;
//			gbc.insets.left = 3;
//			gbc.insets.right = 3;
//			gbc.weighty = 0;
//			gbc.fill = GridBagConstraints.BOTH;
//			gbc.gridwidth = 1;
//			gbc.gridheight = 1;
//			gbc.gridy = 0;
//			
//			gbc.gridx = 0;
//			gbc.weightx = 1;
//			topPanel.add(new JLabel("Rank Group Name", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.name, gbc.clone());
//			gbc.gridx++;
//			topPanel.add(new JLabel("Epithets Capitalized", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.capitalized, gbc.clone());
//			gbc.gridx++;
//			topPanel.add(new JLabel("Repeated Epithets", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.repeatedEpithets, gbc.clone());
//			gbc.gridy++;
//			
//			gbc.gridx = 0;
//			JButton baseEpithetTestButton = new JButton("Test to Base Epithets");
//			baseEpithetTestButton.setPreferredSize(buttonSize);
//			baseEpithetTestButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (OmniFatInstanceEditor.this.checkConsistency()) {
//						TreeNode configNode = getConfigNode(new HashSet() {
//							public boolean contains(Object o) {
//								String s = ((String) o);
//								return (!s.startsWith("rank") || s.startsWith("rankGroup." + getRankGroupName()));
//							}
//						});
//						testInstance("Rank Group Test", adp, configNode, OmniFAT.BASE_EPITHET_TYPE, ("Rank Group '" + getRankGroupName() + "'"));
//					}
//				}
//			});
//			topPanel.add(baseEpithetTestButton, gbc.clone());
//			gbc.gridx++;
//			
//			JButton epithetTestButton = new JButton("Test to Epithets");
//			epithetTestButton.setPreferredSize(buttonSize);
//			epithetTestButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (OmniFatInstanceEditor.this.checkConsistency()) {
//						TreeNode configNode = getConfigNode(new HashSet() {
//							public boolean contains(Object o) {
//								String s = ((String) o);
//								return (!s.startsWith("rank") || s.startsWith("rankGroup." + getRankGroupName()));
//							}
//						});
//						testInstance("RankGroupTest", adp, configNode, OmniFAT.EPITHET_TYPE, ("Rank Group '" + getRankGroupName() + "'"));
//					}
//				}
//			});
//			topPanel.add(epithetTestButton, gbc.clone());
//			gbc.gridx++;
//			
//			topPanel.add(new JLabel("Learning Mode", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.learningMode, gbc.clone());
//			gbc.gridx++;
//			topPanel.add(new JLabel("Do Suffix Diff", JLabel.RIGHT), gbc.clone());
//			gbc.gridx++;
//			topPanel.add(this.suffixDiff, gbc.clone());
//			gbc.gridy++;
//			
//			this.tabs = new JTabbedPane();
//			this.tabs.addTab("Epithet Dictionaries", this.dictionaryList);
//			this.tabs.addTab("Patterns", this.patternList);
//			this.tabs.addTab("Ranks", this.rankPanel);
//			
//			this.add(topPanel, BorderLayout.NORTH);
//			this.add(this.tabs, BorderLayout.CENTER);
//		}
//		
//		String getRankGroupName() {
//			return this.name.getText();
//		}
//		
//		String[] getRankNames() {
//			String[] rankNames = new String[this.ranks.getTabCount()];
//			for (int r = 0; r < this.ranks.getTabCount(); r++) {
//				RankPanel rp = ((RankPanel) this.ranks.getComponentAt(r));
//				rankNames[r] = rp.getRankName();
//			}
//			return rankNames;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			if (!this.dictionaryList.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.dictionaryList);
//				return false;
//			}
//			if (!this.patternList.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.patternList);
//				return false;
//			}
//			
//			for (int r = 0; r < this.ranks.getTabCount(); r++) {
//				RankPanel rp = ((RankPanel) this.ranks.getComponentAt(r));
//				if (!rp.checkConsistency()) {
//					this.tabs.setSelectedComponent(this.rankPanel);
//					this.ranks.setSelectedComponent(rp);
//					return false;
//				}
//			}
//			String[] rankNames = this.getRankNames();
//			HashSet uniqueRankNames = new HashSet();
//			for (int r = 0; r < rankNames.length; r++)
//				if (!uniqueRankNames.add(rankNames[r])) {
//					JOptionPane.showMessageDialog(this, ("The names of the individual ranks must be unique, but '" + rankNames[r] + "' is duplicate."), "Duplicate Rank Name", JOptionPane.ERROR_MESSAGE);
//					return false;
//				}
//			
//			String name = this.getRankGroupName();
//			if ((name.length() == 0) || !name.matches("[a-zA-Z]++")) {
//				JOptionPane.showMessageDialog(this, ("The name of a rank group must consist of letters only, at least one letter."), "Invalid Rank Group Name", JOptionPane.ERROR_MESSAGE);
//				return false;
//			}
//			
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			TreeNode[] nodes = {new TreeNode(null, "rankGroup")};
//			
//			nodes[0].setAttribute("name", this.name.getText());
//			
//			nodes[0].setAttribute("capitalized", ("" + this.capitalized.getSelectedItem()));
//			
//			nodes[0].setAttribute("repeatedEpithets", (this.repeatedEpithets.isSelected() ? "true" : "false"));
//			
//			nodes[0].setAttribute("doSuffixDiff", (this.suffixDiff.isSelected() ? "true" : "false"));
//			
//			nodes[0].setAttribute("learningMode", ("" + this.learningMode.getSelectedItem()));
//			
//			TreeNode[] dictionaryNodes = this.dictionaryList.getTreeNodes(filter);
//			for (int d = 0; d < dictionaryNodes.length; d++) {
//				nodes[0].addChildNode(dictionaryNodes[d]);
//				dictionaryNodes[d].setParent(nodes[0]);
//			}
//			
//			TreeNode[] patternNodes = this.patternList.getTreeNodes(filter);
//			for (int p = 0; p < patternNodes.length; p++) {
//				nodes[0].addChildNode(patternNodes[p]);
//				patternNodes[p].setParent(nodes[0]);
//			}
//			
//			for (int r = 0; r < this.ranks.getTabCount(); r++) {
//				RankPanel rp = ((RankPanel) this.ranks.getComponentAt(r));
//				if (filter.contains("rankGroup." + this.getRankGroupName() + "." + rp.getRankName())) {
//					TreeNode[] rankNodes = rp.getTreeNodes(filter);
//					for (int p = 0; p < rankNodes.length; p++) {
//						nodes[0].addChildNode(rankNodes[p]);
//						rankNodes[p].setParent(nodes[0]);
//					}
//				}
//			}
//			
//			return nodes;
//		}
//	}
//	
//	private static final String[] AUTHOR_NAME_PATTERN_TYPES = {"positive", "part", "negative"};
//	
//	private class AuthorDataPanel extends OmniFatInstanceEditorDetailPanel {
//		
//		DictionaryTablePanel dictionaryList;
//		PatternTablePanel patternList;
//		
//		InternalListPanel nameStopWords;
//		
//		InternalListPanel nameListSeparators;
//		InternalListPanel nameListEndSeparators;
//		
//		JTabbedPane tabs;
//		
//		AuthorDataPanel(TreeNode authorDataNode, String[] dictionaryNames, String[] patternNames) {
//			
//			//	load dictionaries
//			TreeNode[] dictionaryNodes = authorDataNode.getChildNodes("dictionary");
//			this.dictionaryList = new DictionaryTablePanel(dictionaryNames, null, false, dictionaryNodes);
//			
//			//	load patterns
//			TreeNode[] patternNodes = authorDataNode.getChildNodes("pattern");
//			this.patternList = new PatternTablePanel(patternNames, AUTHOR_NAME_PATTERN_TYPES, patternNodes);
//			
//			//	load stop words
//			TreeNode[] authorNameStopWordNodes = authorDataNode.getChildNodes("nameStopWords");
//			StringVector authorNameStopWords = new StringVector();
//			String authorNameStopWordSeparator = ";";
//			for (int sw = 0; sw < authorNameStopWordNodes.length; sw++) {
//				authorNameStopWordSeparator = authorNameStopWordNodes[sw].getAttribute("separator", ";");
//				TreeNode dataNode = authorNameStopWordNodes[sw].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					authorNameStopWords.parseAndAddElements(dataNode.getNodeValue(), authorNameStopWordSeparator);
//			}
//			authorNameStopWords.removeDuplicateElements();
//			this.nameStopWords = new InternalListPanel("Stop Words Appearing in Author Names", "nameStopWords", authorNameStopWordSeparator, authorNameStopWords);
//			
//			//	load list end separators
//			TreeNode[] authorNameListEndSeparatorNodes = authorDataNode.getChildNodes("nameListEndSeparators");
//			StringVector authorNameListEndSeparators = new StringVector();
//			String authorNameListEndSeparatorSeparator = ";";
//			for (int les = 0; les < authorNameListEndSeparatorNodes.length; les++) {
//				authorNameListEndSeparatorSeparator = authorNameListEndSeparatorNodes[les].getAttribute("separator", ";");
//				TreeNode dataNode = authorNameListEndSeparatorNodes[les].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					authorNameListEndSeparators.parseAndAddElements(dataNode.getNodeValue(), authorNameListEndSeparatorSeparator);
//			}
//			authorNameListEndSeparators.removeDuplicateElements();
//			this.nameListEndSeparators = new InternalListPanel("Separators between last two Names in Author Name Lists", "nameListEndSeparators", authorNameListEndSeparatorSeparator, authorNameListEndSeparators);
//			
//			//	load list separators
//			TreeNode[] authorNameListSeparatorNodes = authorDataNode.getChildNodes("nameListSeparators");
//			StringVector authorNameListSeparators = new StringVector();
//			String authorNameListSeparatorSeparator = ";";
//			for (int ls = 0; ls < authorNameListSeparatorNodes.length; ls++) {
//				authorNameListSeparatorSeparator = authorNameListSeparatorNodes[ls].getAttribute("separator", ";");
//				TreeNode dataNode = authorNameListSeparatorNodes[ls].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
//				if (dataNode != null)
//					authorNameListSeparators.parseAndAddElements(dataNode.getNodeValue(), authorNameListSeparatorSeparator);
//			}
//			authorNameListSeparators.removeDuplicateElements();
//			this.nameListSeparators = new InternalListPanel("Separators between Names in Author Name Lists", "nameListSeparators", authorNameListSeparatorSeparator, authorNameListSeparators);
//			
//			
//			//	layout the whole stuff
//			this.tabs = new JTabbedPane();
//			this.tabs.setTabPlacement(JTabbedPane.LEFT);
//			this.tabs.addTab("Dictionaries", this.dictionaryList);
//			this.tabs.addTab("Patterns", this.patternList);
//			this.tabs.addTab("Name Stop Words", this.nameStopWords);
//			this.tabs.addTab("Name List Separators", this.nameListSeparators);
//			this.tabs.addTab("Name List End Separators", this.nameListEndSeparators);
//			
//			this.add(this.tabs, BorderLayout.CENTER);
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			if (!this.dictionaryList.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.dictionaryList);
//				return false;
//			}
//			if (!this.patternList.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.patternList);
//				return false;
//			}
//			
//			if (!this.nameStopWords.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.nameStopWords);
//				return false;
//			}
//			
//			if (!this.nameListSeparators.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.nameListSeparators);
//				return false;
//			}
//			if (!this.nameListEndSeparators.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.nameListEndSeparators);
//				return false;
//			}
//			
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			TreeNode[] nodes = {new TreeNode(null, "authors")};
//			
//			TreeNode[] dictionaryNodes = this.dictionaryList.getTreeNodes(filter);
//			for (int d = 0; d < dictionaryNodes.length; d++) {
//				nodes[0].addChildNode(dictionaryNodes[d]);
//				dictionaryNodes[d].setParent(nodes[0]);
//			}
//			
//			TreeNode[] patternNodes = this.patternList.getTreeNodes(filter);
//			for (int p = 0; p < patternNodes.length; p++) {
//				nodes[0].addChildNode(patternNodes[p]);
//				patternNodes[p].setParent(nodes[0]);
//			}
//			
//			TreeNode[] stopWordNodes = this.nameStopWords.getTreeNodes(filter);
//			for (int sw = 0; sw < stopWordNodes.length; sw++) {
//				nodes[0].addChildNode(stopWordNodes[sw]);
//				stopWordNodes[sw].setParent(nodes[0]);
//			}
//			
//			TreeNode[] listEndSeparatorNodes = this.nameListEndSeparators.getTreeNodes(filter);
//			for (int les = 0; les < listEndSeparatorNodes.length; les++) {
//				nodes[0].addChildNode(listEndSeparatorNodes[les]);
//				listEndSeparatorNodes[les].setParent(nodes[0]);
//			}
//			
//			TreeNode[] listSeparatorNodes = this.nameListSeparators.getTreeNodes(filter);
//			for (int ls = 0; ls < listSeparatorNodes.length; ls++) {
//				nodes[0].addChildNode(listSeparatorNodes[ls]);
//				listSeparatorNodes[ls].setParent(nodes[0]);
//			}
//			
//			return nodes;
//		}
//	}
//	
//	private static final String[] DATA_RULE_ACTIONS = {"promote", "feedback", "remove"};
//	
//	private class DataRuleTablePanel extends OmniFatInstanceEditorDetailPanel {
//		
//		private class DataRuleTableLine {
//			
//			JTextField match;
//			JComboBox action;
//			
//			JCheckBox removeNested;
//			JCheckBox likelyMatch;
//			
//			JButton test;
//			
//			DataRuleTableLine(String match, String action, boolean removeNested, boolean likelyMatch) {
//				this.match = new JTextField(match);
//				this.match.addFocusListener(new FocusAdapter() {
//					public void focusGained(FocusEvent fe) {
//						if (selectedRule != null)
//							selectedRule.match.setBorder(selectedRuleBorder);
//						selectedRule = DataRuleTableLine.this;
//						selectedRuleBorder = DataRuleTableLine.this.match.getBorder();
//						DataRuleTableLine.this.match.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
//						tablePanel.validate();
//					}
//				});
//				
//				this.action = new JComboBox(DATA_RULE_ACTIONS);
//				this.action.setSelectedItem(action);
//				
//				this.removeNested = new JCheckBox("", removeNested);
//				
//				this.likelyMatch = new JCheckBox("", likelyMatch);
//				
//				this.test = new JButton("Test");
//				this.test.setPreferredSize(tableButtonSize);
//				this.test.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (OmniFatInstanceEditor.this.checkConsistency()) {
//							final int ruleNumber = (rules.indexOf(DataRuleTableLine.this) + 1);
//							TreeNode configNode = getConfigNode(new HashSet() {
//								public boolean contains(Object o) {
//									String s = ((String) o);
//									return (!s.startsWith("data") || (s.compareTo("dataRule." + ruleSetNumber + "." + ruleNumber) <= 0));
//								}
//							});
//							testInstance("DataRuleTest", adp, configNode, null, ("Data Rule '" + ruleSetNumber + "." + ruleNumber + "'"));
//						}
//					}
//				});
//			}
//		}
//		
//		ArrayList rules = new ArrayList();
//		
//		int ruleSetNumber = 0;
//		
//		DataRuleTableLine selectedRule = null;
//		Border selectedRuleBorder = null;
//		
//		JPanel tablePanel = new JPanel(new GridBagLayout(), true);
//		
//		boolean dirty = false;
//		
//		DataRuleTablePanel(TreeNode[] ruleNodes) {
//			for (int r = 0; r < ruleNodes.length; r++)
//				this.rules.add(new DataRuleTableLine(ruleNodes[r].getAttribute("match"), ruleNodes[r].getAttribute("action"), "true".equals(ruleNodes[r].getAttribute("removeNested", "false")), "true".equals(ruleNodes[r].getAttribute("likelyMatch", "false"))));
//			
//			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
//			JButton button;
//			button = new JButton("Add Rule");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					DataRuleTableLine drtl = new DataRuleTableLine("", "remove", false, false);
//					rules.add(0, drtl);
//					layoutRuleTable();
//					drtl.match.requestFocusInWindow();
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Remove Rule");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (selectedRule != null) {
//						selectedRule.match.setBorder(selectedRuleBorder);
//						rules.remove(selectedRule);
//						layoutRuleTable();
//						selectedRule = null;
//						selectedRuleBorder = null;
//					}
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Move Up");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (selectedRule == null)
//						return;
//					
//					int index = rules.indexOf(selectedRule);
//					if (index > 0) {
//						DataRuleTableLine rp = ((DataRuleTableLine) rules.remove(index));
//						rules.add((index-1), rp);
//						layoutRuleTable();
//						rp.match.requestFocusInWindow();
//					}
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Move Down");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (selectedRule == null)
//						return;
//					
//					int index = rules.indexOf(selectedRule);
//					if ((index+1) < rules.size()) {
//						DataRuleTableLine rp = ((DataRuleTableLine) rules.remove(index));
//						rules.add((index+1), rp);
//						layoutRuleTable();
//						rp.match.requestFocusInWindow();
//					}
//				}
//			});
//			buttonPanel.add(button);
//			
//			JScrollPane tablePanelBox = new JScrollPane(this.tablePanel);
//			tablePanelBox.getVerticalScrollBar().setUnitIncrement(25);
//			this.add(buttonPanel, BorderLayout.NORTH);
//			this.add(tablePanelBox, BorderLayout.CENTER);
//			this.layoutRuleTable();
//		}
//		
//		private void layoutRuleTable() {
//			this.tablePanel.removeAll();
//			
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 3;
//			gbc.insets.bottom = 3;
//			gbc.insets.left = 3;
//			gbc.insets.right = 3;
//			gbc.weighty = 0;
//			gbc.gridheight = 1;
//			gbc.gridwidth = 1;
//			gbc.fill = GridBagConstraints.BOTH;
//			gbc.gridy = 0;
//			
//			gbc.gridx = 0;
//			gbc.weightx = 1;
//			this.tablePanel.add(new JLabel("Match", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 1;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Action", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 2;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Remove Nested", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 3;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Likely Match", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 4;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Test", JLabel.CENTER), gbc.clone());
//			gbc.gridy++;
//			
//			for (int p = 0; p < this.rules.size(); p++) {
//				DataRuleTableLine drtl = ((DataRuleTableLine) this.rules.get(p));
//				gbc.gridx = 0;
//				gbc.weightx = 1;
//				this.tablePanel.add(drtl.match, gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 0;
//				this.tablePanel.add(drtl.action, gbc.clone());
//				gbc.gridx = 2;
//				gbc.weightx = 0;
//				this.tablePanel.add(drtl.removeNested, gbc.clone());
//				gbc.gridx = 3;
//				gbc.weightx = 0;
//				this.tablePanel.add(drtl.likelyMatch, gbc.clone());
//				gbc.gridx = 4;
//				gbc.weightx = 0;
//				this.tablePanel.add(drtl.test, gbc.clone());
//				gbc.gridy++;
//			}
//			
//			this.tablePanel.validate();
//			this.tablePanel.repaint();
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			HashSet uniqueMatches = new HashSet();
//			for (int p = 0; p < this.rules.size(); p++) {
//				DataRuleTableLine drtl = ((DataRuleTableLine) this.rules.get(p));
//				String match = drtl.match.getText();
//				if ((match.length() == 0) || !match.replaceAll("[^a-zA-Z]", "").matches("[plabun]++")) {
//					JOptionPane.showMessageDialog(this,
//							("The match of a data rule must contain only these letters:" +
//							"\n - 'p' for a sure positive epithet" +
//							"\n - 'l' for a likely positive epithet" +
//							"\n - 'a' for an ambiguous epithet" +
//							"\n - 'b' for an abbreviated epithet" +
//							"\n - 'u' for an unknown epithet" +
//							"\n - 'n' for a negative/excluded epithet"),
//							"Invalid Match", JOptionPane.ERROR_MESSAGE);
//					return false;
//				}
//				
//				if (!uniqueMatches.add(match)) {
//					JOptionPane.showMessageDialog(this, ("The match expressions of data rules must not be duplicated within one rule set, but '" + match + "' is duplicate."), "Duplicate Match Expression", JOptionPane.ERROR_MESSAGE);
//					return false;
//				}
//			}
//			
//			return true;
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			TreeNode[] nodes = {new TreeNode(null, "ruleSet")};
//			
//			for (int dr = 0; dr < this.rules.size(); dr++) {
//				DataRuleTableLine drtl = ((DataRuleTableLine) this.rules.get(dr));
//				if (filter.contains("dataRule." + this.ruleSetNumber + "." + (dr+1))) {
//					TreeNode node = new TreeNode(nodes[0], "rule");
//					node.setAttribute("match", drtl.match.getText());
//					node.setAttribute("action", ("" + drtl.action.getSelectedItem()));
//					node.setAttribute("removeNested", (drtl.removeNested.isSelected() ? "true" : "false"));
//					node.setAttribute("likelyMatch", (drtl.likelyMatch.isSelected() ? "true" : "false"));
//					nodes[0].addChildNode(node);
//				}
//			}
//			
//			return nodes;
//		}
//	}
//	
//	private class DictionaryTablePanel extends OmniFatInstanceEditorDetailPanel {
//		private class DictionaryTableLine {
//			JLabel name;
//			JCheckBox use;
//			JComboBox type;
//			JCheckBox trusted;
//			JSpinner suffixDiffThreshold;
//			DictionaryTableLine(String name, boolean selected, String[] types, String type, boolean trusted, int suffixDiffThreshold) {
//				this.name = new JLabel(name);
//				this.use = new JCheckBox("", selected);
//				if (types != null) {
//					this.type = new JComboBox(types);
//					this.type.setEditable(false);
//					this.type.setSelectedItem(type);
//				}
//				this.trusted = new JCheckBox("", trusted);
//				if (suffixDiffThreshold > -1)
//					this.suffixDiffThreshold = new JSpinner(new SpinnerNumberModel(suffixDiffThreshold, 0, 10, 1));
//			}
//		}
//		
//		DictionaryTableLine[] dictionaries;
//		boolean showTypes;
//		boolean showSuffixDiff;
//		
//		JPanel tablePanel = new JPanel(new GridBagLayout(), true);
//		
//		boolean dirty = false;
//		
//		DictionaryTablePanel(String[] dictionaryNames, String[] types, boolean showSuffixDiff, TreeNode[] dictionaryNodes) {
//			this.dictionaries = new DictionaryTableLine[dictionaryNames.length];
//			this.showTypes = ((types != null) && (types.length > 1));
//			this.showSuffixDiff = showSuffixDiff;
//			
//			HashSet selectedUse = new HashSet();
//			Properties selectedTypes = new Properties();
//			HashSet selectedTrusted = new HashSet();
//			HashMap selectedSuffixDiffThresholds = new HashMap();
//			for (int d = 0; d < dictionaryNodes.length; d++) {
//				String name = dictionaryNodes[d].getAttribute("ref");
//				selectedUse.add(name);
//				if (this.showTypes)
//					selectedTypes.setProperty(name, dictionaryNodes[d].getAttribute("type", types[0]));
//				if ("true".equals(dictionaryNodes[d].getAttribute("trusted", "false")))
//					selectedTrusted.add(name);
//				if (showSuffixDiff) try {
//					Integer sdt = new Integer(dictionaryNodes[d].getAttribute("suffixDiffThreshold", "0"));
//					if (sdt.intValue() != 0)
//						selectedSuffixDiffThresholds.put(name, sdt);
//				} catch (NumberFormatException nfe) {}
//			}
//			
//			for (int d = 0; d < dictionaryNames.length; d++)
//				this.dictionaries[d] = new DictionaryTableLine(
//						dictionaryNames[d],
//						selectedUse.contains(dictionaryNames[d]),
//						types,
//						(this.showTypes ? selectedTypes.getProperty(dictionaryNames[d], types[0]) : null),
//						selectedTrusted.contains(dictionaryNames[d]),
//						(this.showSuffixDiff ? (selectedSuffixDiffThresholds.containsKey(dictionaryNames[d]) ? ((Integer) selectedSuffixDiffThresholds.get(dictionaryNames[d])).intValue() : 0) : -1)
//					);
//			
//			JScrollPane tablePanelBox = new JScrollPane(this.tablePanel);
//			tablePanelBox.getVerticalScrollBar().setUnitIncrement(25);
//			this.add(tablePanelBox, BorderLayout.CENTER);
//			this.layoutDictionaryTable();
//		}
//		
//		private void layoutDictionaryTable() {
//			this.tablePanel.removeAll();
//			
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 3;
//			gbc.insets.bottom = 3;
//			gbc.insets.left = 3;
//			gbc.insets.right = 3;
//			gbc.weighty = 0;
//			gbc.gridheight = 1;
//			gbc.gridwidth = 1;
//			gbc.fill = GridBagConstraints.BOTH;
//			gbc.gridy = 0;
//			
//			gbc.gridx = 0;
//			gbc.weightx = 1;
//			this.tablePanel.add(new JLabel("Name", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 1;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Use", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 2;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Trusted", JLabel.CENTER), gbc.clone());
//			if (this.showTypes) {
//				gbc.gridx = 3;
//				gbc.weightx = 0;
//				this.tablePanel.add(new JLabel("Use As", JLabel.CENTER), gbc.clone());
//			}
//			if (this.showSuffixDiff) {
//				gbc.gridx = 4;
//				gbc.weightx = 0;
//				this.tablePanel.add(new JLabel("Diff", JLabel.CENTER), gbc.clone());
//			}
//			gbc.gridy++;
//			
//			for (int d = 0; d < this.dictionaries.length; d++) {
//				gbc.gridx = 0;
//				gbc.weightx = 1;
//				this.tablePanel.add(this.dictionaries[d].name, gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 0;
//				this.tablePanel.add(this.dictionaries[d].use, gbc.clone());
//				gbc.gridx = 2;
//				gbc.weightx = 0;
//				this.tablePanel.add(this.dictionaries[d].trusted, gbc.clone());
//				if (this.showTypes) {
//					gbc.gridx = 3;
//					gbc.weightx = 0;
//					this.tablePanel.add(this.dictionaries[d].type, gbc.clone());
//				}
//				if (this.showSuffixDiff) {
//					gbc.gridx = 4;
//					gbc.weightx = 0;
//					this.tablePanel.add(this.dictionaries[d].suffixDiffThreshold, gbc.clone());
//				}
//				gbc.gridy++;
//			}
//			
//			this.tablePanel.validate();
//			this.tablePanel.repaint();
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			return true; // nothing to check here
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			ArrayList nodes = new ArrayList();
//			
//			for (int d = 0; d < this.dictionaries.length; d++)
//				if (this.dictionaries[d].use.isSelected()) {
//					TreeNode node = new TreeNode(null, "dictionary");
//					node.setAttribute("ref", this.dictionaries[d].name.getText());
//					if (this.showTypes)
//						node.setAttribute("type", ("" + this.dictionaries[d].type.getSelectedItem()));
//					if (this.dictionaries[d].trusted.isSelected())
//						node.setAttribute("trusted", "true");
//					if (this.showSuffixDiff)
//						node.setAttribute("suffixDiffThreshold", ("" + this.dictionaries[d].suffixDiffThreshold.getValue()));
//					nodes.add(node);
//				}
//			
//			return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
//		}
//	}
//	
//	private class PatternTablePanel extends OmniFatInstanceEditorDetailPanel {
//		
//		private class PatternTableLine {
//			
//			JLabel name;
//			JTextField pattern;
//			JComponent string;
//			
//			JCheckBox isRef;
//			JCheckBox use;
//			JComboBox type;
//			
//			JButton button;
//			
//			JButton removeButton;
//			
//			PatternTableLine(final String string, boolean isRef, boolean use, String[] types, String type) {
//				if (isRef) {
//					this.name = new JLabel(string, JLabel.LEFT);
//					this.string = this.name;
//					
//					this.button = new JButton("Edit");
//					this.button.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							editPattern(string);
//						}
//					});
//					
//					this.removeButton = new JButton("Remove");
//					this.removeButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							PatternTableLine.this.use.setSelected(false);
//						}
//					});
//				}
//				else {
//					this.pattern = new JTextField(string);
//					this.string = this.pattern;
//					
//					this.button = new JButton("Test");
//					this.button.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							String rawPattern = pattern.getText();
//							testPattern(rawPattern, rawPattern, true);
//						}
//					});
//					
//					this.removeButton = new JButton("Remove");
//					this.removeButton.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent ae) {
//							PatternTablePanel.this.patterns.remove(PatternTableLine.this);
//							PatternTablePanel.this.layoutPatternTable();
//						}
//					});
//				}
//				this.button.setPreferredSize(tableButtonSize);
//				this.removeButton.setPreferredSize(tableButtonSize);
//				
//				this.isRef = new JCheckBox("", isRef);
//				this.isRef.setEnabled(false);
//				
//				this.use = new JCheckBox("", (use || !isRef));
//				this.use.setEnabled(isRef);
//				
//				this.type = new JComboBox(types);
//				this.type.setEditable(false);
//				this.type.setSelectedItem(type);
//			}
//		}
//		
//		ArrayList patterns = new ArrayList();
//		
//		JPanel tablePanel = new JPanel(new GridBagLayout(), true);
//		
//		JButton addPattern;
//		
//		boolean dirty = false;
//		
//		PatternTablePanel(String[] patternNames, final String[] types, TreeNode[] patternNodes) {
//			LinkedHashSet selectedUse = new LinkedHashSet();
//			Properties selectedTypes = new Properties();
//			for (int p = 0; p < patternNodes.length; p++) {
//				String string = patternNodes[p].getAttribute("string");
//				selectedUse.add(string);
//				selectedTypes.setProperty(string, patternNodes[p].getAttribute("type", types[0]));
//			}
//			
//			ArrayList globalPatterns = new ArrayList();
//			for (int p = 0; p < patternNames.length; p++)
//				globalPatterns.add(new PatternTableLine(patternNames[p], true, selectedUse.remove(patternNames[p]), types, selectedTypes.getProperty(patternNames[p], types[0])));
//			
//			for (Iterator pit = selectedUse.iterator(); pit.hasNext();) {
//				String pattern = ((String) pit.next());
//				this.patterns.add(new PatternTableLine(pattern, false, true, types, selectedTypes.getProperty(pattern, types[0])));
//			}
//			this.patterns.addAll(globalPatterns);
//			
//			this.addPattern = new JButton("Add Pattern");
//			this.addPattern.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					PatternTableLine ptl = new PatternTableLine("", false, true, types, types[0]);
//					patterns.add(0, ptl);
//					layoutPatternTable();
//				}
//			});
//			
//			JScrollPane tablePanelBox = new JScrollPane(this.tablePanel);
//			tablePanelBox.getVerticalScrollBar().setUnitIncrement(25);
//			this.add(tablePanelBox, BorderLayout.CENTER);
//			this.layoutPatternTable();
//		}
//		
//		private void layoutPatternTable() {
//			this.tablePanel.removeAll();
//			
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.insets.top = 3;
//			gbc.insets.bottom = 3;
//			gbc.insets.left = 3;
//			gbc.insets.right = 3;
//			gbc.weighty = 0;
//			gbc.gridheight = 1;
//			gbc.gridwidth = 1;
//			gbc.fill = GridBagConstraints.BOTH;
//			gbc.gridy = 0;
//			
//			gbc.gridx = 0;
//			gbc.weightx = 1;
//			this.tablePanel.add(new JLabel("Name/Pattern", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 1;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Global", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 2;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Use", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 3;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Use As", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 4;
//			gbc.weightx = 0;
//			this.tablePanel.add(new JLabel("Edit/Test", JLabel.CENTER), gbc.clone());
//			gbc.gridx = 5;
//			gbc.weightx = 0;
//			this.tablePanel.add(this.addPattern, gbc.clone());
//			gbc.gridy++;
//			
//			for (int p = 0; p < this.patterns.size(); p++) {
//				PatternTableLine ptl = ((PatternTableLine) this.patterns.get(p));
//				gbc.gridx = 0;
//				gbc.weightx = 1;
//				this.tablePanel.add(ptl.string, gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 0;
//				this.tablePanel.add(ptl.isRef, gbc.clone());
//				gbc.gridx = 2;
//				gbc.weightx = 0;
//				this.tablePanel.add(ptl.use, gbc.clone());
//				gbc.gridx = 3;
//				gbc.weightx = 0;
//				this.tablePanel.add(ptl.type, gbc.clone());
//				gbc.gridx = 4;
//				gbc.weightx = 0;
//				this.tablePanel.add(ptl.button, gbc.clone());
//				gbc.gridx = 5;
//				gbc.weightx = 0;
//				this.tablePanel.add(ptl.removeButton, gbc.clone());
//				gbc.gridy++;
//			}
//			
//			this.tablePanel.validate();
//			this.tablePanel.repaint();
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
//		 */
//		boolean checkConsistency() {
//			return true; // nothing to check here
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
//		 */
//		TreeNode[] getTreeNodes(HashSet filter) {
//			ArrayList nodes = new ArrayList();
//			
//			for (int p = 0; p < this.patterns.size(); p++) {
//				PatternTableLine ptl = ((PatternTableLine) this.patterns.get(p));
//				if (ptl.use.isSelected()) {
//					TreeNode node = new TreeNode(null, "pattern");
//					if (ptl.isRef.isSelected()) {
//						node.setAttribute("string", ptl.name.getText());
//						node.setAttribute("isRef", "true");
//					}
//					else {
//						node.setAttribute("string", ptl.pattern.getText());
//						node.setAttribute("isRef", "false");
//					}
//					node.setAttribute("type", ("" + ptl.type.getSelectedItem()));
//					nodes.add(node);
//				}
//			}
//			
//			return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
//		}
//	}
//	
//	private class PatternEditorDialog extends JDialog {
//		private RegExEditorPanel editor;
//		PatternEditorDialog(Dialog owner, String patternName) {
//			super(owner, ("Edit Pattern '" + patternName + "'"), true);
//			this.init(patternName);
//		}
//		PatternEditorDialog(Frame owner, String patternName) {
//			super(owner, ("Edit Pattern '" + patternName + "'"), true);
//			this.init(patternName);
//		}
//		private void init(final String patternName) {
//			JButton[] buttons = new JButton[1];
//			buttons[0] = new JButton("Test");
//			buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
//			buttons[0].setPreferredSize(new Dimension(115, 21));
//			buttons[0].addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					String rawPattern = editor.getContent();
//					testPattern(patternName, rawPattern, true);
//				}
//			});
//			this.editor = new RegExEditorPanel(buttons);
//			this.editor.setSubPatternResolver(resolver);
//			this.editor.setContent(loadPattern(patternName, ""));
//			
//			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
//			JButton button;
//			button = new JButton("OK");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					String rawPattern = editor.getContent();
//					
//					String validationError = RegExUtils.validateRegEx(rawPattern, resolver);
//					if (validationError != null) {
//						int choice = JOptionPane.showConfirmDialog(PatternEditorDialog.this, ("The pattern is not valid:\n" + validationError + "\nStore changes and close dialog anyway?"), "Pattern Validation Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
//						if (choice != JOptionPane.YES_OPTION)
//							return;
//					}
//					
//					try {
//						storePattern(patternName, rawPattern);
//					}
//					catch (IOException ioe) {
//						System.out.println("Could not store pattern '" + patternName + "': " + ioe.getMessage());
//						ioe.printStackTrace(System.out);
//						int choice = JOptionPane.showConfirmDialog(PatternEditorDialog.this, ("The pattern could not be stored:\n" + ioe.getMessage() + "\nClose dialog anyway?"), "Pattern Storage Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
//						if (choice!= JOptionPane.YES_OPTION)
//							return;
//					}
//					
//					dispose();
//				}
//			});
//			buttonPanel.add(button);
//			button = new JButton("Cancle");
//			button.setPreferredSize(buttonSize);
//			button.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					dispose();
//				}
//			});
//			buttonPanel.add(button);
//			
//			this.getContentPane().setLayout(new BorderLayout());
//			this.getContentPane().add(this.editor, BorderLayout.CENTER);
//			this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//			
//			this.setResizable(true);
//			this.setSize(new Dimension(600, 400));
//			this.setLocationRelativeTo(this.getOwner());
//		}
//	}
//	
//	private void editPattern(String patternName) {
//		PatternEditorDialog ped;
//		Window top = DialogFactory.getTopWindow();
//		if (top instanceof JDialog)
//			ped = new PatternEditorDialog(((JDialog) top), patternName);
//		else if (top instanceof JFrame)
//			ped = new PatternEditorDialog(((JFrame) top), patternName);
//		else ped = new PatternEditorDialog(((JFrame) null), patternName);
//		
//		ped.setVisible(true);
//	}
//	
//	private void testPattern(String patternName, String rawPattern, boolean isRef) {
//		if (rawPattern == null) return;
//		
//		String validationError = RegExUtils.validateRegEx(rawPattern, this.resolver);
//		if (validationError == null) {
//			TokenSequence testTokens = getTestDocument();
//			if (testTokens != null) {
//				String pattern = (isRef ? RegExUtils.preCompile(rawPattern, this.resolver) : rawPattern);
//				Annotation[] annotations = Gamta.extractAllMatches(testTokens, pattern, 20);
//				AnnotationDisplayDialog add;
//				Window top = DialogFactory.getTopWindow();
//				if (top instanceof JDialog)
//					add = new AnnotationDisplayDialog(((JDialog) top), ("Matches of RegEx '" + patternName + "'"), annotations, true);
//				else if (top instanceof JFrame)
//					add = new AnnotationDisplayDialog(((JFrame) top), ("Matches of RegEx '" + patternName + "'"), annotations, true);
//				else add = new AnnotationDisplayDialog(((JFrame) null), ("Matches of RegEx '" + patternName + "'"), annotations, true);
//				add.setLocationRelativeTo(this);
//				add.setVisible(true);
//			}
//		}
//		else JOptionPane.showMessageDialog(this, ("The pattern is not valid:\n" + validationError), "Pattern Validation", JOptionPane.ERROR_MESSAGE);
//	}
//	
//	private Properties resolver = new Properties() {
//		public String getProperty(String name, String def) {
//			return loadPattern(name, def);
//		}
//		public String getProperty(String name) {
//			return this.getProperty(name, null);
//		}
//	};
//	
//	private String loadPattern(String patternName, String def) {
//		try {
//			if (patternName.endsWith(".regEx.txt")) {
//				InputStream is = this.adp.getInputStream(patternName);
//				StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
//				is.close();
//				return RegExUtils.normalizeRegEx(regEx.concatStrings(""));
//			}
//			else if (this.adp.isDataAvailable(patternName + ".list.txt")) {
//				InputStream is = this.adp.getInputStream(patternName + ".list.txt");
//				StringVector load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
//				is.close();
//				String orGroup = RegExUtils.produceOrGroup(load, true);
//				return orGroup;
//			}
//			else {
//				InputStream is = this.adp.getInputStream(patternName + ".regEx.txt");
//				StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
//				is.close();
//				return RegExUtils.normalizeRegEx(regEx.concatStrings(""));
//			}
//		}
//		catch (IOException ioe) {
//			System.out.println("Could not load pattern '" + patternName + "': " + ioe.getMessage());
//			ioe.printStackTrace(System.out);
//			return def;
//		}
//	}
//	
//	private boolean storePattern(String patternName, String pattern) throws IOException {
//		OutputStreamWriter osw = new OutputStreamWriter(this.adp.getOutputStream(patternName), "UTF-8");
//		osw.write(RegExUtils.normalizeRegEx(pattern));
//		osw.flush();
//		osw.close();
//		return true;
//	}
//	
//	private AnalyzerDataProvider adp;
//	
//	private BaseDataPanel baseData;
//	
//	private JTabbedPane rankGroups;
//	private JPanel rankGroupPanel;
//	
//	private AuthorDataPanel authorData;
//	
//	private JTabbedPane dataRuleSets;
//	private JPanel dataRuleSetPanel;
//	
//	private JTabbedPane tabs;
//	
//	public OmniFatInstanceEditor(String instanceName, AnalyzerDataProvider adp) throws IOException {
//		this(instanceName, adp, loadInstance(instanceName, adp), getDictionaryNames(adp), getPatternNames(adp));
//	}
//	
//	public OmniFatInstanceEditor(final String instanceName, final AnalyzerDataProvider adp, TreeNode configNode, final String[] dictionaryNames, final String[] patternNames) {
//		super(new BorderLayout(), true);
//		this.adp = adp;
//		
//		
//		//	load base data
//		this.baseData = new BaseDataPanel(configNode, dictionaryNames, patternNames);
//		
//		
//		//	load rank groups
//		this.rankGroups = new JTabbedPane();
//		this.rankGroups.setTabPlacement(JTabbedPane.LEFT);
//		TreeNode[] rankGroupNodes = configNode.getChildNodes("rankGroup");
//		for (int rg = 0; rg < rankGroupNodes.length; rg++) {
//			final RankGroupPanel rgp = new RankGroupPanel(rankGroupNodes[rg], dictionaryNames, patternNames);
//			rgp.name.addFocusListener(new FocusAdapter() {
//				public void focusLost(FocusEvent fe) {
//					int sri = rankGroups.indexOfComponent(rgp);
//					if (sri != -1)
//						rankGroups.setTitleAt(sri, rgp.name.getText());
//				}
//			});
//			this.rankGroups.addTab(rgp.name.getText(), rgp);
//		}
//		
//		JPanel rankGroupButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
//		JButton button;
//		button = new JButton("Add Rank Group");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				final RankGroupPanel rgp = new RankGroupPanel(new TreeNode(null, "rankGroup"), dictionaryNames, patternNames);
//				rgp.name.addFocusListener(new FocusAdapter() {
//					public void focusLost(FocusEvent fe) {
//						int sri = rankGroups.indexOfComponent(rgp);
//						if (sri != -1)
//							rankGroups.setTitleAt(sri, rgp.name.getText());
//					}
//				});
//				rankGroups.addTab(rgp.name.getText(), rgp);
//				rankGroups.setSelectedComponent(rgp);
//			}
//		});
//		rankGroupButtonPanel.add(button);
//		button = new JButton("Remove Rank Group");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				int index = rankGroups.getSelectedIndex();
//				if (index != -1)
//					rankGroups.removeTabAt(index);
//			}
//		});
//		rankGroupButtonPanel.add(button);
//		button = new JButton("Move Up");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				int index = rankGroups.getSelectedIndex();
//				if (index > 0) {
//					RankGroupPanel rgp = ((RankGroupPanel) rankGroups.getComponentAt(index));
//					rankGroups.removeTabAt(index);
//					rankGroups.insertTab(rgp.name.getText(), null, rgp, null, (index-1));
//					rankGroups.setSelectedIndex(index-1);
//				}
//			}
//		});
//		rankGroupButtonPanel.add(button);
//		button = new JButton("Move Down");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				int index = rankGroups.getSelectedIndex();
//				if ((index+1) < rankGroups.getTabCount()) {
//					RankGroupPanel rgp = ((RankGroupPanel) rankGroups.getComponentAt(index));
//					rankGroups.removeTabAt(index);
//					rankGroups.insertTab(rgp.name.getText(), null, rgp, null, (index+1));
//					rankGroups.setSelectedIndex(index+1);
//				}
//			}
//		});
//		rankGroupButtonPanel.add(button);
//		
//		this.rankGroupPanel = new JPanel(new BorderLayout(), true);
//		this.rankGroupPanel.add(rankGroupButtonPanel, BorderLayout.NORTH);
//		this.rankGroupPanel.add(this.rankGroups, BorderLayout.CENTER);
//		
//		
//		//	load author data
//		TreeNode authorDataNode = configNode.getChildNode("authors", 0);
//		if (authorDataNode == null)
//			this.authorData = new AuthorDataPanel(new TreeNode(null, "authors"), dictionaryNames, patternNames);
//		else this.authorData = new AuthorDataPanel(authorDataNode, dictionaryNames, patternNames);
//		
//		
//		//	load data rules
//		this.dataRuleSets = new JTabbedPane();
//		this.dataRuleSets.setTabPlacement(JTabbedPane.LEFT);
//		TreeNode rulesNode = configNode.getChildNode("rules", 0);
//		if (rulesNode != null) {
//			TreeNode[] ruleSetNodes = rulesNode.getChildNodes("ruleSet");
//			for (int rs = 0; rs < ruleSetNodes.length; rs++) {
//				DataRuleTablePanel drsp = new DataRuleTablePanel(ruleSetNodes[rs].getChildNodes("rule"));
//				drsp.ruleSetNumber = (rs + 1);
//				this.dataRuleSets.addTab(("" + (rs + 1)), drsp);
//			}
//		}
//		
//		JPanel dataRuleSetButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
//		button = new JButton("Add Rule Set");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				DataRuleTablePanel drsp = new DataRuleTablePanel(new TreeNode[0]);
//				drsp.ruleSetNumber = (dataRuleSets.getTabCount() + 1);
//				dataRuleSets.addTab(("" + (dataRuleSets.getTabCount() + 1)), drsp);
//				dataRuleSets.setSelectedComponent(drsp);
//			}
//		});
//		dataRuleSetButtonPanel.add(button);
//		button = new JButton("Remove Rule Set");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				int index = dataRuleSets.getSelectedIndex();
//				if (index != -1) {
//					dataRuleSets.removeTabAt(index);
//					for (int rs = 0; rs < dataRuleSets.getTabCount(); rs++) {
//						((DataRuleTablePanel) dataRuleSets.getComponentAt(rs)).ruleSetNumber = (rs + 1);
//						dataRuleSets.setTitleAt(rs, ("" + (rs+1)));
//					}
//				}
//			}
//		});
//		dataRuleSetButtonPanel.add(button);
//		button = new JButton("Move Up");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				int index = dataRuleSets.getSelectedIndex();
//				if (index > 0) {
//					DataRuleTablePanel drsp = ((DataRuleTablePanel) dataRuleSets.getComponentAt(index));
//					dataRuleSets.removeTabAt(index);
//					dataRuleSets.insertTab("", null, drsp, null, (index-1));
//					dataRuleSets.setSelectedIndex(index-1);
//					for (int rs = 0; rs < dataRuleSets.getTabCount(); rs++) {
//						((DataRuleTablePanel) dataRuleSets.getComponentAt(rs)).ruleSetNumber = (rs + 1);
//						dataRuleSets.setTitleAt(rs, ("" + (rs+1)));
//					}
//				}
//			}
//		});
//		dataRuleSetButtonPanel.add(button);
//		button = new JButton("Move Down");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				int index = dataRuleSets.getSelectedIndex();
//				if ((index+1) < dataRuleSets.getTabCount()) {
//					DataRuleTablePanel drsp = ((DataRuleTablePanel) dataRuleSets.getComponentAt(index));
//					dataRuleSets.removeTabAt(index);
//					dataRuleSets.insertTab("", null, drsp, null, (index+1));
//					dataRuleSets.setSelectedIndex(index+1);
//					for (int rs = 0; rs < dataRuleSets.getTabCount(); rs++) {
//						((DataRuleTablePanel) dataRuleSets.getComponentAt(rs)).ruleSetNumber = (rs + 1);
//						dataRuleSets.setTitleAt(rs, ("" + (rs+1)));
//					}
//				}
//			}
//		});
//		dataRuleSetButtonPanel.add(button);
//		
//		this.dataRuleSetPanel = new JPanel(new BorderLayout(), true);
//		this.dataRuleSetPanel.add(dataRuleSetButtonPanel, BorderLayout.NORTH);
//		this.dataRuleSetPanel.add(this.dataRuleSets, BorderLayout.CENTER);
//		
//		
//		//	layout the whole stuff
//		this.tabs = new JTabbedPane();
//		this.tabs.addTab("Base Data", this.baseData);
//		this.tabs.addTab("Rank Groups", this.rankGroupPanel);
//		this.tabs.addTab("Author Data", this.authorData);
//		this.tabs.addTab("Data Rules", this.dataRuleSetPanel);
//		
//		
//		//	create buttons
//		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
//		button = new JButton("Validate");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency())
//					JOptionPane.showMessageDialog(OmniFatInstanceEditor.this, ("The OmniFAT instance is valid."), "OmniFAT Instance Valid", JOptionPane.INFORMATION_MESSAGE);
//			}
//		});
//		buttonPanel.add(button);
//		
//		button = new JButton("Base Epithets");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency()) {
//					TreeNode configNode = getConfigNode(ALL_FILTER);
//					testInstance(instanceName, adp, configNode, OmniFAT.BASE_EPITHET_TYPE, ("Base Epithets in OmniFAT Instance '" + instanceName + "'"));
//				}
//			}
//		});
//		buttonPanel.add(button);
//		
//		button = new JButton("Authors");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency()) {
//					TreeNode configNode = getConfigNode(new HashSet() {
//						public boolean contains(Object o) {
//							String s = ((String) o);
//							return (!s.startsWith("rank") && !s.startsWith("data"));
//						}
//					});
//					testInstance(instanceName, adp, configNode, OmniFAT.AUTHOR_NAME_TYPE, ("Authors in OmniFAT Instance '" + instanceName + "'"));
//				}
//			}
//		});
//		buttonPanel.add(button);
//		
//		button = new JButton("Epithets");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency()) {
//					TreeNode configNode = getConfigNode(ALL_FILTER);
//					testInstance(instanceName, adp, configNode, OmniFAT.EPITHET_TYPE, ("Epithets in OmniFAT Instance '" + instanceName + "'"));
//				}
//			}
//		});
//		buttonPanel.add(button);
//		
//		button = new JButton("Candidates");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency()) {
//					TreeNode configNode = getConfigNode(ALL_FILTER);
//					testInstance(instanceName, adp, configNode, OmniFAT.TAXON_NAME_CANDIDATE_TYPE, ("Candidates in OmniFAT Instance '" + instanceName + "'"));
//				}
//			}
//		});
//		buttonPanel.add(button);
//		
//		button = new JButton("Test Instance");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency()) {
//					TreeNode configNode = getConfigNode(ALL_FILTER);
//					testInstance(instanceName, adp, configNode, null, ("OmniFAT Instance '" + instanceName + "'"));
//				}
//			}
//		});
//		buttonPanel.add(button);
//		
//		button = new JButton("Discart Doc");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				testTokens = null;
//			}
//		});
//		buttonPanel.add(button);
//		
//		
//		button = new JButton("Print XML");
//		button.setPreferredSize(buttonSize);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				if (checkConsistency()) {
//					TreeNode configNode = getConfigNode(ALL_FILTER);
//					System.out.println(configNode.treeToCode("  "));
//				}
//			}
//		});
//		buttonPanel.add(button);
//		
//		//	display instance name
//		JLabel instanceNameLabel = new JLabel(instanceName, JLabel.LEFT);
//		instanceNameLabel.setBorder(BorderFactory.createLineBorder(instanceNameLabel.getBackground(), 5));
//		instanceNameLabel.setFont(instanceNameLabel.getFont().deriveFont(Font.BOLD));
//		
//		//	layout functions
//		JPanel topPanel = new JPanel(new BorderLayout(), true);
//		topPanel.add(instanceNameLabel, BorderLayout.WEST);
//		topPanel.add(buttonPanel, BorderLayout.CENTER);
//		
//		//	... finally
//		this.add(topPanel, BorderLayout.NORTH);
//		this.add(this.tabs, BorderLayout.CENTER);
//	}
//	
//	boolean checkConsistency() {
//		if (!this.baseData.checkConsistency()) {
//			this.tabs.setSelectedComponent(this.baseData);
//			return false;
//		}
//		
//		String[] rankGroupNames = new String[this.rankGroups.getTabCount()];
//		String[][] rankNames = new String[this.rankGroups.getTabCount()][];
//		for (int rg = 0; rg < this.rankGroups.getTabCount(); rg++) {
//			RankGroupPanel rgp = ((RankGroupPanel) this.rankGroups.getComponentAt(rg));
//			rankGroupNames[rg] = rgp.getRankGroupName();
//			rankNames[rg] = rgp.getRankNames();
//			if (!rgp.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.rankGroupPanel);
//				return false;
//			}
//		}
//		
//		HashSet uniqueRankGroupNames = new HashSet();
//		HashSet uniqueRankNames = new HashSet();
//		for (int rg = 0; rg < rankGroupNames.length; rg++) {
//			if (!uniqueRankGroupNames.add(rankGroupNames[rg])) {
//				JOptionPane.showMessageDialog(this, ("The names of the individual rank groups must be unique, but '" + rankGroupNames[rg] + "' is duplicate."), "Duplicate Rank Group Name", JOptionPane.ERROR_MESSAGE);
//				this.tabs.setSelectedComponent(this.rankGroupPanel);
//				return false;
//			}
//			for (int r = 0; r < rankNames[rg].length; r++)
//				if (!uniqueRankNames.add(rankNames[rg][r])) {
//					JOptionPane.showMessageDialog(this, ("The names of the individual ranks must be unique, but '" + rankNames[rg][r] + "' is duplicate."), "Duplicate Rank Name", JOptionPane.ERROR_MESSAGE);
//					this.tabs.setSelectedComponent(this.rankGroupPanel);
//					this.tabs.setSelectedIndex(rg);
//					return false;
//				}
//		}
//		
//		if (!this.authorData.checkConsistency()) {
//			this.tabs.setSelectedComponent(this.authorData);
//			return false;
//		}
//		
//		for (int rg = 0; rg < this.rankGroups.getTabCount(); rg++) {
//			DataRuleTablePanel drsp = ((DataRuleTablePanel) this.dataRuleSets.getComponentAt(rg));
//			if (!drsp.checkConsistency()) {
//				this.tabs.setSelectedComponent(this.dataRuleSetPanel);
//				return false;
//			}
//		}
//		
//		return true;
//	}
//	
//	private static final HashSet ALL_FILTER = new HashSet() {
//		public boolean contains(Object obj) {
//			return true;
//		}
//	};
//	
//	TreeNode getConfigNode(HashSet filter) {
//		if (!this.checkConsistency())
//			return null;
//		
//		if (filter == null)
//			filter = ALL_FILTER;
//		
//		TreeNode configNode = new TreeNode(null, "omniFat");
//		
//		TreeNode[] baseDataNodes = this.baseData.getTreeNodes(filter);
//		for (int p = 0; p < baseDataNodes.length; p++) {
//			configNode.addChildNode(baseDataNodes[p]);
//			baseDataNodes[p].setParent(configNode);
//		}
//		
//		for (int rg = 0; rg < this.rankGroups.getTabCount(); rg++) {
//			RankGroupPanel rgp = ((RankGroupPanel) this.rankGroups.getComponentAt(rg));
//			if (filter.contains("rankGroup." + rgp.getRankGroupName())) {
//				TreeNode[] rankGroupNodes = rgp.getTreeNodes(filter);
//				for (int p = 0; p < rankGroupNodes.length; p++) {
//					configNode.addChildNode(rankGroupNodes[p]);
//					rankGroupNodes[p].setParent(configNode);
//				}
//			}
//		}
//		
//		TreeNode[] authorNodes = this.authorData.getTreeNodes(filter);
//		for (int p = 0; p < authorNodes.length; p++) {
//			configNode.addChildNode(authorNodes[p]);
//			authorNodes[p].setParent(configNode);
//		}
//		
//		TreeNode rulesNode = new TreeNode(configNode, "rules");
//		configNode.addChildNode(rulesNode);
//		for (int drs = 0; drs < this.dataRuleSets.getTabCount(); drs++) {
//			DataRuleTablePanel drsp = ((DataRuleTablePanel) this.dataRuleSets.getComponentAt(drs));
//			if (filter.contains("dataRule." + drs)) {
//				TreeNode[] ruleSetNodes = drsp.getTreeNodes(filter);
//				for (int p = 0; p < ruleSetNodes.length; p++) {
//					rulesNode.addChildNode(ruleSetNodes[p]);
//					ruleSetNodes[p].setParent(rulesNode);
//				}
//			}
//		}
//		
//		return configNode;
//	}
//	
//	private MutableTokenSequence testTokens = null;
//	
//	private DocumentRoot getTestDocument() {
//		if (this.testTokens == null) {
//			TokenSequence testTokens = Gamta.getTestDocument();
//			if (testTokens != null) {
//				this.testTokens = Gamta.newTokenSequence(testTokens, testTokens.getTokenizer());
//				for (int t = 0; t < this.testTokens.size(); t++) {
//					if ((t+1) == this.testTokens.size())
//						this.testTokens.tokenAt(t).setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
//					else {
//						String whiteSpace = this.testTokens.getWhitespaceAfter(t);
//						if ((whiteSpace.indexOf('\n') != -1) || (whiteSpace.indexOf('\r') != -1))
//							this.testTokens.tokenAt(t).setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
//					}
//				}
//			}
//		}
//		return ((this.testTokens == null) ? null : Gamta.newDocument(this.testTokens));
//	}
//	
//	private void testInstance(String instanceName, AnalyzerDataProvider adp, TreeNode configNode, String mode, String title) {
//		DocumentRoot testDoc = getTestDocument();
//		if (testDoc == null) {
//			System.out.println(configNode.treeToCode("  "));
//			return;
//		}
//		
//		OmniFAT omniFat = OmniFAT.getInstance(instanceName, adp, configNode);
//		OmniFAT.DocumentDataSet testDocData = new OmniFAT.DocumentDataSet(omniFat, testDoc.getAnnotationID());
//		
//		if (OmniFAT.BASE_EPITHET_TYPE.equals(mode)) {
//			OmniFatFunctions.tagBaseEpithets(testDoc, testDocData);
//			String[] types = {OmniFAT.BASE_EPITHET_TYPE};
//			String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
//			this.displayTestResult(testDoc, types, attributes, title);
//			return;
//		}
//		
//		if (OmniFAT.AUTHOR_NAME_TYPE.equals(mode)) {
//			OmniFatFunctions.tagAuthorNames(testDoc, testDocData);
//			String[] types = {OmniFAT.AUTHOR_NAME_TYPE};
//			String[] attributes = {OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
//			this.displayTestResult(testDoc, types, attributes, title);
//			return;
//		}
//		
//		if (OmniFAT.EPITHET_LABEL_TYPE.equals(mode)) {
//			OmniFatFunctions.tagLabels(testDoc, testDocData);
//			String[] types = {OmniFAT.EPITHET_LABEL_TYPE, OmniFAT.NEW_EPITHET_LABEL_TYPE};
//			String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE};
//			this.displayTestResult(testDoc, types, attributes, title);
//			return;
//		}
//		
//		OmniFatFunctions.tagBaseEpithets(testDoc, testDocData);
//		OmniFatFunctions.tagAuthorNames(testDoc, testDocData);
//		OmniFatFunctions.tagLabels(testDoc, testDocData);
//		
//		OmniFatFunctions.tagEpithets(testDoc, testDocData);
//		if (OmniFAT.EPITHET_TYPE.equals(mode)) {
//			String[] types = {OmniFAT.EPITHET_TYPE};
//			String[] attributes = {OmniFAT.STRING_ATTRIBUTE, OmniFAT.AUTHOR_ATTRIBUTE, OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
//			this.displayTestResult(testDoc, types, attributes, title);
//			return;
//		}
//		
//		OmniFatFunctions.tagCandidates(testDoc, testDocData);
//		if (OmniFAT.TAXON_NAME_CANDIDATE_TYPE.equals(mode)) {
//			String[] types = {OmniFAT.TAXON_NAME_CANDIDATE_TYPE};
//			String[] attributes = {OmniFAT.STRING_ATTRIBUTE, OmniFAT.AUTHOR_ATTRIBUTE, OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
//			this.addEpithetDetailLabelAttribute(testDoc, types, testDocData);
//			this.displayTestResult(testDoc, types, attributes, title);
//			return;
//		}
//		
//		OmniFatFunctions.applyPrecisionRules(testDoc, testDocData);
//		OmniFatFunctions.applyAuthorNameRules(testDoc, testDocData);
//		OmniFatFunctions.applyDataRules(testDoc, testDocData);
//		
//		String[] types = {OmniFAT.TAXON_NAME_CANDIDATE_TYPE, OmniFAT.TAXONOMIC_NAME_TYPE, OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE};
//		String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, "StatusString"};
//		this.addEpithetDetailLabelAttribute(testDoc, types, testDocData);
//		this.addStatusStringAttribute(testDoc, types, testDocData);
//		this.displayTestResult(testDoc, types, attributes, title);
//	}
//	
//	private void addEpithetDetailLabelAttribute(QueriableAnnotation doc, String[] annotationTypes, OmniFAT.DocumentDataSet docData) {
//		for (int t = 0; t < annotationTypes.length; t++) {
//			QueriableAnnotation[] annotations = doc.getAnnotations(annotationTypes[t]);
//			for (int a = 0; a < annotations.length; a++) {
//				StringBuffer dlb = new StringBuffer("<HTML><B>" + annotations[a].getValue() + "</B>:");
//				
//				QueriableAnnotation[] epithets = docData.getEpithets(annotations[a]);
//				for (int e = 0; e < epithets.length; e++) {
//					String epithetString = ((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
//					if ((epithetString != null) && epithetString.endsWith("."))
//						epithetString = epithetString.substring(0, (epithetString.length()-1));
//					
//					int eaStart = -1;
//					int eaEnd = -1;
//					String epithetSource = ((String) epithets[e].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
//					Annotation[] epithetAuthors = epithets[e].getAnnotations(OmniFAT.AUTHOR_NAME_TYPE);
//					for (int ea = 0; ea < epithetAuthors.length; ea++)
//						if (epithetSource.indexOf(epithetAuthors[ea].getAnnotationID()) != -1) {
//							if (eaStart == -1)
//								eaStart = epithetAuthors[ea].getStartIndex();
//							eaEnd = (epithetAuthors[ea].getEndIndex() - 1);
//						}
//					
//					dlb.append("<BR>&nbsp;-&nbsp;");
//					for (int v = 0; v < epithets[e].size(); v++) {
//						String value = epithets[e].valueAt(v);
//						
//						if (v == eaStart)
//							dlb.append("<I>");
//						
//						if (value.equals(epithetString)) {
//							dlb.append("<B>" + value + "</B>");
//							epithetString = null;
//						}
//						else dlb.append(value);
//						
//						if (v == eaEnd)
//							dlb.append("</I>");
//						if (epithets[e].getWhitespaceAfter(v).length() != 0)
//							dlb.append("&nbsp;");
//					}
//				}
//				
//				dlb.append("</HTML>");
//				annotations[a].setAttribute("__detailLabel", dlb.toString());
//			}
//		}
//	}
//	
//	private void addStatusStringAttribute(QueriableAnnotation doc, String[] annotationTypes, OmniFAT.DocumentDataSet docData) {
//		for (int t = 0; t < annotationTypes.length; t++) {
//			QueriableAnnotation[] annotations = doc.getAnnotations(annotationTypes[t]);
//			for (int a = 0; a < annotations.length; a++)
//				annotations[a].setAttribute("StatusString", OmniFAT.DataRule.getEpithetStatusString(docData.getEpithets(annotations[a])));
//		}
//	}
//	
//	private void displayTestResult(QueriableAnnotation testDoc, String[] annotationTypes, String[] attributeNames, String title) {
//		HashSet types = new HashSet(Arrays.asList(annotationTypes));
//		HashSet attributes = new HashSet(Arrays.asList(attributeNames));
//		Annotation[] resultAnnotations = testDoc.getAnnotations();
//		final StringRelation resultData = new StringRelation();
//		final HashMap resultAnnotationsByID = new HashMap();
//		for (int r = 0; r < resultAnnotations.length; r++)
//			if (types.contains(resultAnnotations[r].getType())) {
//				resultAnnotationsByID.put(resultAnnotations[r].getAnnotationID(), resultAnnotations[r]);
//				String[] resultAttributeNames = resultAnnotations[r].getAttributeNames();
//				StringTupel resultTupel = new StringTupel();
//				resultTupel.setValue("Type", resultAnnotations[r].getType());
//				resultTupel.setValue("StartIndex", ("" + resultAnnotations[r].getStartIndex()));
//				resultTupel.setValue("Size", ("" + resultAnnotations[r].size()));
//				resultTupel.setValue("Value", resultAnnotations[r].getValue());
//				resultTupel.setValue("AnnotationID", resultAnnotations[r].getAnnotationID());
//				for (int a = 0; a < resultAttributeNames.length; a++)
//					if (attributes.contains(resultAttributeNames[a])) {
//						if (OmniFAT.VALUE_ATTRIBUTE.equals(resultAttributeNames[a]))
//							resultTupel.setValue(resultAttributeNames[a], resultAnnotations[r].getValue());
//						else resultTupel.setValue(resultAttributeNames[a], ((String) resultAnnotations[r].getAttribute(resultAttributeNames[a], "")));
//					}
//				resultData.addElement(resultTupel);
//			}
//		
//		final JDialog resultDialog;
//		Window top = DialogFactory.getTopWindow();
//		if (top instanceof Dialog)
//			resultDialog = new JDialog(((Dialog) top), ("Test Result for " + title), true);
//		else if (top instanceof Frame)
//			resultDialog = new JDialog(((Frame) top), ("Test Result for " + title), true);
//		else resultDialog = new JDialog(((Frame) null), ("Test Result for " + title), true);
//		resultDialog.getContentPane().setLayout(new BorderLayout());
//		
//		final String[] resultAttributes = new String[attributeNames.length + 4];
//		resultAttributes[0] = "Type";
//		resultAttributes[1] = "StartIndex";
//		resultAttributes[2] = "Size";
//		resultAttributes[3] = "Value";
//		System.arraycopy(attributeNames, 0, resultAttributes, 4, attributeNames.length);
//		
//		final JTable resultTable = new JTable(new TableModel() {
//			public int getColumnCount() {
//				return resultAttributes.length;
//			}
//			public String getColumnName(int columnIndex) {
//				return resultAttributes[columnIndex];
//			}
//			public Class getColumnClass(int columnIndex) {
//				return String.class;
//			}
//			
//			public int getRowCount() {
//				return resultData.size();
//			}
//			public Object getValueAt(int rowIndex, int columnIndex) {
//				return resultData.get(rowIndex).getValue(this.getColumnName(columnIndex), "");
//			}
//			
//			public boolean isCellEditable(int rowIndex, int columnIndex) {
//				return false;
//			}
//			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
//			
//			public void addTableModelListener(TableModelListener tml) {}
//			public void removeTableModelListener(TableModelListener tml) {}
//		});
//		resultTable.addMouseListener(new MouseAdapter() {
//			public void mouseClicked(MouseEvent me) {
//				if (me.getClickCount() < 2)
//					return;
//				
//				int selectedIndex = resultTable.getSelectedRow();
//				if (selectedIndex == -1)
//					return;
//				
//				Annotation annotation = ((Annotation) resultAnnotationsByID.get(resultData.get(selectedIndex).getValue("AnnotationID")));
//				if (annotation == null)
//					return;
//				
//				String annotationDetailLabel = ((String) annotation.getAttribute("__detailLabel"));
//				if (annotationDetailLabel != null)
//					JOptionPane.showMessageDialog(resultDialog, annotationDetailLabel, "Details", JOptionPane.PLAIN_MESSAGE);
//			}
//		});
//		
//		final JTableHeader resultTableHeader = resultTable.getTableHeader();
//		resultTableHeader.addMouseListener(new MouseAdapter() {
//			public void mouseClicked(MouseEvent me) {
//                int column = resultTableHeader.columnAtPoint(me.getPoint());
//                if (column != -1) {
//                	String sortColumnName = resultTable.getModel().getColumnName(column);
//                	resultData.orderBy(sortColumnName);
//                	resultTable.validate();
//                	resultTable.repaint();
//                }
//			}
//		});
//		
//		JScrollPane resultTableBox = new JScrollPane(resultTable);
//		resultTableBox.getVerticalScrollBar().setUnitIncrement(25);
//		resultDialog.getContentPane().add(resultTableBox, BorderLayout.CENTER);
//		
//		JButton button = new JButton("OK");
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				resultDialog.dispose();
//			}
//		});
//		resultDialog.getContentPane().add(button, BorderLayout.SOUTH);
//		
//		resultDialog.setResizable(true);
//		resultDialog.setSize(new Dimension(800, 600));
//		resultDialog.setLocationRelativeTo(this);
//		resultDialog.setVisible(true);
//	}
//	
//	static final TreeNode loadInstance(String instanceName, AnalyzerDataProvider adp) throws IOException {
//		Reader reader = new InputStreamReader(adp.getInputStream(instanceName + ".omniFatInstance.xml"), "UTF-8");
//		Parser p = new Parser();
//		TreeNode configNode = p.parse(reader);
//		if (TreeNode.ROOT_NODE_TYPE.equals(configNode.getNodeType()))
//			configNode = configNode.getChildNode("omniFat", 0);
//		reader.close();
//		return configNode;
//	}
//	
//	static final String[] getDictionaryNames(AnalyzerDataProvider adp) {
//		String[] dataNames = adp.getDataNames();
//		StringVector dictionaryNames = new StringVector();
//		for (int d = 0; d < dataNames.length; d++) {
//			if (dataNames[d].endsWith(".list.txt") && (dataNames[d].indexOf('/') == -1))
//				dictionaryNames.addElementIgnoreDuplicates(dataNames[d]);
//			else if (dataNames[d].endsWith(".list.zip") && (dataNames[d].indexOf('/') == -1))
//				dictionaryNames.addElementIgnoreDuplicates(dataNames[d].substring(0, (dataNames[d].length() - ".list.zip".length())) + ".list.txt");
//		}
//		dictionaryNames.sortLexicographically(false, false);
//		return dictionaryNames.toStringArray();
//	}
//	
//	static final String[] getPatternNames(AnalyzerDataProvider adp) {
//		String[] dataNames = adp.getDataNames();
//		StringVector patternNames = new StringVector();
//		for (int d = 0; d < dataNames.length; d++) {
//			if (dataNames[d].endsWith(".regEx.txt") && (dataNames[d].indexOf('/') == -1))
//				patternNames.addElementIgnoreDuplicates(dataNames[d]);
//		}
//		patternNames.sortLexicographically(false, false);
//		return patternNames.toStringArray();
//	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		Gamta.addTestDocumentProvider(new TestDocumentProvider() {
//			public QueriableAnnotation getTestDocument() {
//				MutableAnnotation doc = null;
//				try {
//					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21330.htm.xml"), "UTF-8"));
////					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/4075_gg1.xml"), "UTF-8"));
////					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/8127_1_gg1.xml"), "UTF-8"));
////					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/Novitates0045/N0045.clean.xml"), "UTF-8"));
////					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/22777.xml"), "UTF-8"));
//				}
//				catch (IOException e) {
//					e.printStackTrace();
//				}
//				return doc;
//			}
//		});
//		
//		AnalyzerDataProvider adp = new AnalyzerDataProviderFileBased(new File("E:/Testdaten/OmniFATData/"));
//		JFrame test = new JFrame();
//		test.add(new OmniFatInstanceEditor("Default", adp));
//		test.setSize(800, 500);
//		test.setLocationRelativeTo(null);
//		test.setVisible(true);
//	}
//}