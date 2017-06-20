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

package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.TestDocumentProvider;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.accessories.RegExEditorPanel;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * @author sautter
 *
 */
public class OmniFatEditor extends JPanel {
	
	private class DataItemDialog extends JDialog {
		JTextField dataNameField;
		String dataName = null;
		DataItemDialog(JDialog parent, String title, String dataName, boolean isCreateDialog, AnalyzerDataProvider adp) {
			super(parent, title, true);
			this.init(dataName, isCreateDialog, adp);
		}
		DataItemDialog(JFrame parent, String title, String dataName, boolean isCreateDialog, AnalyzerDataProvider adp) {
			super(parent, title, true);
			this.init(dataName, isCreateDialog, adp);
		}
		private void init(String dataName, boolean isCreateDialog, AnalyzerDataProvider adp) {
			this.getContentPane().setLayout(new BorderLayout());
			
			this.dataNameField = new JTextField(dataName);
			this.dataName = dataName;
			
			JPanel namePanel = new JPanel(new BorderLayout());
			namePanel.add(new JLabel("Name: "), BorderLayout.WEST);
			namePanel.add(this.dataNameField, BorderLayout.CENTER);
			if (isCreateDialog)
				this.getContentPane().add(namePanel, BorderLayout.NORTH);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			JButton button;
			button = new JButton(isCreateDialog ? "Create" : "Edit");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DataItemDialog.this.dataName = dataNameField.getText();
					dispose();
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Cancel");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DataItemDialog.this.dataName = null;
					dispose();
				}
			});
			buttonPanel.add(button);
			
			this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
		}
		
		boolean isCommitted() {
			return (this.dataName != null);
		}
		
		String getDataName() {
			return this.dataName;
		}
	}
	
	private abstract class ConfigurationDialogPart extends JPanel {
		OmniFAT.Base base;
		AnalyzerDataProvider adp;
		JDialog parent;
		JList dataNameList;
		ConfigurationDialogPart(OmniFAT.Base base, AnalyzerDataProvider adp, JDialog parent) {
			super(new BorderLayout(), true);
			this.base = base;
			this.adp = adp;
			this.parent = parent;
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			JButton button;
			button = new JButton("New");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					create(null);
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Clone");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					cloneDataItem();
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Edit");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editDataItem();
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Delete");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					deleteInstance();
				}
			});
			buttonPanel.add(button);
			
			this.add(buttonPanel, BorderLayout.NORTH);
			
			this.dataNameList = new JList();
			this.dataNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.dataNameList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() < 2)
						return;
					editDataItem();
				}
			});
			this.refreshList();
			
			JScrollPane dataNameListBox = new JScrollPane(this.dataNameList);
			dataNameListBox.getVerticalScrollBar().setUnitIncrement(25);
			this.add(dataNameListBox, BorderLayout.CENTER);
		}
		
		void refreshList() {
			final String[] dataNames = this.getDataNames();
			this.dataNameList.setModel(new AbstractListModel() {
				public int getSize() {
					return dataNames.length;
				}
				public Object getElementAt(int index) {
					return dataNames[index];
				}
			});
			this.dataNameList.validate();
			this.dataNameList.repaint();
		}
		abstract String[] getDataNames();
		
		private void cloneDataItem() {
			String instanceName = ((String) this.dataNameList.getSelectedValue());
			if (instanceName == null)
				return;
			this.create(instanceName);
		}
		abstract void create(String modelDataName);
		
		private void editDataItem() {
			String instanceName = ((String) this.dataNameList.getSelectedValue());
			if (instanceName == null)
				return;
			this.edit(instanceName);
		}
		abstract void edit(String dataName);
		
		private void deleteInstance() {
			String instanceName = ((String) this.dataNameList.getSelectedValue());
			if (instanceName == null)
				return;
			this.delete(instanceName);
		}
		abstract void delete(String dataName);
	}
	
	
	private static final TreeNode loadInstance(String instanceName, AnalyzerDataProvider adp) throws IOException {
		Reader reader = new InputStreamReader(adp.getInputStream(instanceName + ".omniFatInstance.xml"), "UTF-8");
		Parser p = new Parser();
		TreeNode configNode = p.parse(reader);
		if (TreeNode.ROOT_NODE_TYPE.equals(configNode.getNodeType()))
			configNode = configNode.getChildNode("omniFat", 0);
		reader.close();
		return configNode;
	}
	
	private class InstancePanel extends ConfigurationDialogPart {
		InstancePanel(OmniFAT.Base base, AnalyzerDataProvider adp, JDialog parent) {
			super(base, adp, parent);
		}
		
		String[] getDataNames() {
			return this.base.getInstanceNames();
		}
		
		void create(String modelDataName) {
			TreeNode configNode;
			String dataName;
			if (modelDataName == null) {
				configNode = new TreeNode(null, "omniFat");
				dataName = "NewOmniFatInstance";
			}
			else {
				try {
					configNode = loadInstance(modelDataName, this.adp);
					dataName = ("New " + modelDataName);
				}
				catch (IOException ioe) {
					System.out.println("Error loading OmniFAT instance '" + modelDataName + "': " + ioe.getMessage());
					ioe.printStackTrace(System.out);
					
					configNode = new TreeNode(null, "omniFat");
					dataName = "NewOmniFatInstance";
				}
			}
			InstanceEditor ofie = new InstanceEditor(dataName, this.base, configNode, getDictionaryNames(this.adp), getPatternNames(this.adp));
			
			DataItemDialog did = new DataItemDialog(this.parent, "Create OmniFAT Instance", dataName, true, this.adp);
			did.getContentPane().add(ofie, BorderLayout.CENTER);
			did.setSize(800, 600);
			did.setLocationRelativeTo(this.parent);
			did.setVisible(true);
			
			if (did.isCommitted()) try {
				configNode = ofie.getConfigNode(null);
				dataName = did.getDataName();
				final OutputStreamWriter osw = new OutputStreamWriter(this.adp.getOutputStream(dataName + OmniFAT.INSTANCE_FILE_NAME_SUFFIX), "UTF-8");
				configNode.treeToCode(new TokenReceiver() {
					public void storeToken(String token, int treeDepth) throws IOException {
						osw.write(token);
						osw.write("\r\n");
					}
					public void close() throws IOException {}
				}, "  ");
				osw.flush();
				osw.close();
				this.base.discartCachedInstance(dataName);
				this.refreshList();
			}
			catch (IOException ioe) {
				System.out.println("Error storing OmniFAT instance '" + dataName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		
		void edit(String dataName) {
			try {
				TreeNode configNode = loadInstance(dataName, this.adp);
				InstanceEditor ofie = new InstanceEditor(dataName, this.base, configNode, getDictionaryNames(this.adp), getPatternNames(this.adp));
				
				DataItemDialog did = new DataItemDialog(this.parent, ("Edit OmniFAT Instance '" + dataName + "'"), dataName, false, this.adp);
				did.getContentPane().add(ofie, BorderLayout.CENTER);
				did.setSize(800, 600);
				did.setLocationRelativeTo(this.parent);
				did.setVisible(true);
				
				if (did.isCommitted()) {
					configNode = ofie.getConfigNode(null);
					final OutputStreamWriter osw = new OutputStreamWriter(this.adp.getOutputStream(dataName + OmniFAT.INSTANCE_FILE_NAME_SUFFIX), "UTF-8");
					configNode.treeToCode(new TokenReceiver() {
						public void storeToken(String token, int treeDepth) throws IOException {
							osw.write(token);
						}
						public void close() throws IOException {}
					}, "  ");
					osw.flush();
					osw.close();
					this.base.discartCachedInstance(dataName);
				}
			}
			catch (IOException ioe) {
				System.out.println("Error storing OmniFAT instance '" + dataName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		
		void delete(String dataName) {
			if (this.adp.deleteData(dataName + OmniFAT.INSTANCE_FILE_NAME_SUFFIX)) {
				this.base.discartCachedInstance(dataName);
				this.refreshList();
			}
		}
	}
	
	private static final Dimension buttonSize = new Dimension(100, 21);
	private static final Dimension tableButtonSize = new Dimension(70, 21);
	
	private static final String[] INTERNAL_LIST_SEPARATORS = {";", ",", "|", "!", "/", "\\", "-"};
	
	private static final String[] EPITHET_CAPITALIZATION_MODES = {"always", "mixed", "never"};
	private static final String[] EPITHET_LEARNING_MODES = {"auto", "teach", "off"};
	
	private static final String[] EPITHET_DICTIONARY_TYPES = {"positive", "negative"};
	private static final String[] EPITHET_PATTERN_TYPES = {"precision", "recall", "negative"};
	
	private static final String[] EPITHET_LABELING_MODES = {"always", "sometimes", "never"};
	
	private static final String[] AUTHOR_NAME_PATTERN_TYPES = {"positive", "part", "negative"};
	
	private static final String[] DATA_RULE_ACTIONS = {"promote", "feedback", "remove"};
	
	private static final HashSet ALL_FILTER = new HashSet() {
		public boolean contains(Object obj) {
			return true;
		}
	};
	
	private class InstanceEditor extends JPanel {
		
		private abstract class OmniFatInstanceEditorDetailPanel extends JPanel {
			public OmniFatInstanceEditorDetailPanel() {
				super(new BorderLayout(), true);
			}
			public OmniFatInstanceEditorDetailPanel(LayoutManager layout) {
				super(layout, true);
			}
			
			abstract boolean checkConsistency();
			
			abstract TreeNode[] getTreeNodes(HashSet filter);
		}
		
		private class InternalListPanel extends OmniFatInstanceEditorDetailPanel {
			
			String type;
			JComboBox separator;
			JTextArea listEntries;
			
			JPanel topPanel = new JPanel(new BorderLayout(), true);
			
			InternalListPanel(String label, String type, String separator, StringVector listEntries) {
				super(new BorderLayout());
				this.type = type;
				
				this.separator = new JComboBox(INTERNAL_LIST_SEPARATORS);
				this.separator.setEditable(false);
				this.separator.setSelectedItem(separator);
				if (!separator.equals(this.separator.getSelectedItem())) {
					this.separator.addItem(separator);
					this.separator.setSelectedItem(separator);
				}
				
				this.listEntries = new JTextArea(listEntries.concatStrings("\n"));
				
				this.topPanel.add(new JLabel(label, JLabel.LEFT), BorderLayout.WEST);
				this.topPanel.add(new JLabel("Separator", JLabel.RIGHT), BorderLayout.CENTER);
				this.topPanel.add(this.separator, BorderLayout.EAST);
				
				this.add(this.topPanel, BorderLayout.NORTH);
				JScrollPane listEntryBox = new JScrollPane(this.listEntries);
				listEntryBox.getVerticalScrollBar().setUnitIncrement(25);
				this.add(listEntryBox, BorderLayout.CENTER);
			}
			
			String getSeparator() {
				return ("" + this.separator.getSelectedItem());
			}
			
			StringVector getListEntries() {
				StringVector listEntries = new StringVector();
				BufferedReader listEntryReader = new BufferedReader(new StringReader(this.listEntries.getText()));
				try {
					String listEntry;
					while ((listEntry = listEntryReader.readLine()) != null)
						listEntries.addElementIgnoreDuplicates(listEntry);
				} catch (IOException ioe) {/*will never happen, but Java don't know*/}
//				listEntries.addContentIgnoreDuplicates(this.listEntries.getText().split("[\\n\\r]+"));
				return listEntries;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				String separator = ("" + this.separator.getSelectedItem());
				StringVector listEntries = this.getListEntries();
				String listEntry;
				for (int e = 0; e < listEntries.size(); e++) {
					listEntry = listEntries.get(e);
					if (listEntry.indexOf(separator) != -1) {
						Object seperatorObject = JOptionPane.showInputDialog(this, ("'" + separator + "' is not a valid seperator for the '" + this.type + "' list,\nsince it occurs in twe list entry '" + listEntry + "'.\nPlease select another separator."), "Invalid Separator", JOptionPane.ERROR_MESSAGE, null, INTERNAL_LIST_SEPARATORS, separator);
						if (seperatorObject == null)
							return false;
						else {
							this.separator.setSelectedItem(seperatorObject);
							return this.checkConsistency();
						}
					}
				}
				return true;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				String separator = this.getSeparator();
				StringVector listEntries = this.getListEntries();
				TreeNode[] nodes = {new TreeNode(null, this.type)};
				nodes[0].setAttribute("separator", separator);
				nodes[0].addChildNode(new TreeNode(nodes[0], TreeNode.DATA_NODE_TYPE, listEntries.concatStrings(separator)));
				return nodes;
			}
		}
		
		private class BaseDataPanel extends OmniFatInstanceEditorDetailPanel {
			JTextArea description;
			
			JSpinner fuzzyMatchThreshold;
			
			JSpinner minAbbreviationLength;
			JSpinner maxAbbreviationLength;
			JCheckBox unDottedAbbreviations;
			
			InternalListPanel newEpithetLabels;
			
			InternalListPanel intraEpithetPunctuation;
			InternalListPanel interEpithetPunctuation;
			
			DictionaryTablePanel negativeDictionaryList;
			PatternTablePanel negativePatternList;
			JTabbedPane stemmingRules;
			
			JTabbedPane tabs;
			
			BaseDataPanel(TreeNode configNode, String[] dictionaryNames, String[] patternNames) {
				super();
				
				//	load description
				this.description = new JTextArea();
				TreeNode[] descriptionNodes = configNode.getChildNodes("description");
				if (descriptionNodes.length != 0) {
					TreeNode descriptionNode = descriptionNodes[0].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (descriptionNode != null)
						this.description.setText(descriptionNode.getNodeValue());
				}
				this.description.setLineWrap(true);
				this.description.setWrapStyleWord(true);
				
				//	load fuzzy match threshold
				this.fuzzyMatchThreshold = new JSpinner(new SpinnerNumberModel(2, 0, 5, 1));
				TreeNode[] fuzzyMatchNodes = configNode.getChildNodes("fuzzyMatch");
				if (fuzzyMatchNodes.length == 1)
					this.fuzzyMatchThreshold.setValue(new Integer(fuzzyMatchNodes[0].getAttribute("threshold", "2")));
				
				//	load abbreviation length boundaries
				this.minAbbreviationLength = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
				this.maxAbbreviationLength = new JSpinner(new SpinnerNumberModel(2, 1, 5, 1));
				this.unDottedAbbreviations = new JCheckBox();
				TreeNode[] abbreviationLengthInLettersNodes = configNode.getChildNodes("abbreviationLengthInLetters");
				if (abbreviationLengthInLettersNodes.length == 1) {
					this.minAbbreviationLength.setValue(new Integer(abbreviationLengthInLettersNodes[0].getAttribute("min", "1")));
					this.maxAbbreviationLength.setValue(new Integer(abbreviationLengthInLettersNodes[0].getAttribute("max", "2")));
					this.unDottedAbbreviations.setSelected("true".equals(abbreviationLengthInLettersNodes[0].getAttribute("unDotted", "false")));
				}
				
				//	load new epithet labels
				TreeNode[] newEpithetLabelExtensionNodes = configNode.getChildNodes("newEpithetLabels");
				StringVector newEpithetLabelExtensionList = new StringVector();
				String newEpithetLabelExtensionListSeparator = ";";
				for (int nl = 0; nl < newEpithetLabelExtensionNodes.length; nl++) {
					newEpithetLabelExtensionListSeparator = newEpithetLabelExtensionNodes[nl].getAttribute("separator", ";");
					TreeNode dataNode = newEpithetLabelExtensionNodes[nl].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						newEpithetLabelExtensionList.parseAndAddElements(dataNode.getNodeValue(), newEpithetLabelExtensionListSeparator);
				}
				newEpithetLabelExtensionList.removeDuplicateElements();
				this.newEpithetLabels = new InternalListPanel("Labels for new Epithets", "newEpithetLabels", newEpithetLabelExtensionListSeparator, newEpithetLabelExtensionList);
				
				//	load intra epithet punctuation
				TreeNode[] intraEpithetPunctuationNodes = configNode.getChildNodes("intraEpithetPunctuation");
				StringVector intraEpithetPunctuationMarks = new StringVector();
				String intraEpithetPunctuationMarkSeparator = ";";
				for (int pn = 0; pn < intraEpithetPunctuationNodes.length; pn++) {
					intraEpithetPunctuationMarkSeparator = intraEpithetPunctuationNodes[pn].getAttribute("separator", ";");
					TreeNode dataNode = intraEpithetPunctuationNodes[pn].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						intraEpithetPunctuationMarks.parseAndAddElements(dataNode.getNodeValue(), intraEpithetPunctuationMarkSeparator);
				}
				intraEpithetPunctuationMarks.removeDuplicateElements();
				this.intraEpithetPunctuation = new InternalListPanel("Punctuation Marks within Epithets", "intraEpithetPunctuation", intraEpithetPunctuationMarkSeparator, intraEpithetPunctuationMarks);
				
				//	load inter epithet punctuation
				TreeNode[] interEpithetPunctuationNodes = configNode.getChildNodes("interEpithetPunctuation");
				StringVector interEpithetPunctuationMarks = new StringVector();
				String interEpithetPunctuationMarkSeparator = ";";
				for (int pn = 0; pn < interEpithetPunctuationNodes.length; pn++) {
					interEpithetPunctuationMarkSeparator = interEpithetPunctuationNodes[pn].getAttribute("separator", ";");
					TreeNode dataNode = interEpithetPunctuationNodes[pn].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						interEpithetPunctuationMarks.parseAndAddElements(dataNode.getNodeValue(), interEpithetPunctuationMarkSeparator);
				}
				interEpithetPunctuationMarks.removeDuplicateElements();
				this.interEpithetPunctuation = new InternalListPanel("Punctuation Marks between Epithets", "interEpithetPunctuation", interEpithetPunctuationMarkSeparator, interEpithetPunctuationMarks);
				
				//	load negative & stop word dictionaries
				TreeNode[] negativeDictionaryNodes = configNode.getChildNodes("dictionary");
				ArrayList negativeDictionaryNodeList = new ArrayList();
				for (int nd = 0; nd < negativeDictionaryNodes.length; nd++) {
					if ("stopWord".equals(negativeDictionaryNodes[nd].getAttribute("type")))
						negativeDictionaryNodeList.add(negativeDictionaryNodes[nd]);
					else if ("negative".equals(negativeDictionaryNodes[nd].getAttribute("type")))
						negativeDictionaryNodeList.add(negativeDictionaryNodes[nd]);
				}
				String[] dictionaryTypes = {"negative", "stopWord"};
				this.negativeDictionaryList = new DictionaryTablePanel(dictionaryNames, dictionaryTypes, true, ((TreeNode[]) negativeDictionaryNodeList.toArray(new TreeNode[negativeDictionaryNodeList.size()])));
				
				//	load negative patterns
				TreeNode[] negativePatternNodes = configNode.getChildNodes("pattern");
				String[] patternTypes = {"negative"};
				this.negativePatternList = new PatternTablePanel(patternNames, patternTypes, negativePatternNodes, 1);
				
				
				//	load stemming rules
				this.stemmingRules = new JTabbedPane();
				TreeNode[] stemmingRuleNodes = configNode.getChildNodes("stemmingRule");
				for (int sr = 0; sr < stemmingRuleNodes.length; sr++) {
					String matchEnding = stemmingRuleNodes[sr].getAttribute("matchEnding");
					String separator = stemmingRuleNodes[sr].getAttribute("separator", ";");
					StringVector stemmedEndings = new StringVector();
					TreeNode dataNode = stemmingRuleNodes[sr].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						stemmedEndings.parseAndAddElements(dataNode.getNodeValue(), separator);
					final StemmingRulePanel srp = new StemmingRulePanel(matchEnding, separator, stemmedEndings);
					srp.matchEnding.addFocusListener(new FocusAdapter() {
						public void focusLost(FocusEvent fe) {
							int sri = stemmingRules.indexOfComponent(srp);
							if (sri != -1)
								stemmingRules.setTitleAt(sri, ("-" + srp.matchEnding.getText()));
						}
					});
					this.stemmingRules.addTab(("-" + matchEnding), srp);
				}
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
				JButton button;
				button = new JButton("Add Stemming Rule");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						final StemmingRulePanel srp = new StemmingRulePanel("", ";", new StringVector());
						srp.matchEnding.addFocusListener(new FocusAdapter() {
							public void focusLost(FocusEvent fe) {
								int sri = stemmingRules.indexOfComponent(srp);
								if (sri != -1)
									stemmingRules.setTitleAt(sri, ("-" + srp.matchEnding.getText()));
							}
						});
						stemmingRules.addTab(("-"), srp);
						stemmingRules.setSelectedComponent(srp);
					}
				});
				buttonPanel.add(button);
				button = new JButton("Remove Stemming Rule");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						int index = stemmingRules.getSelectedIndex();
						if (index != -1)
							stemmingRules.removeTabAt(index);
					}
				});
				buttonPanel.add(button);
				
				JPanel stemmingRulePanel = new JPanel(new BorderLayout(), true);
				stemmingRulePanel.add(buttonPanel, BorderLayout.NORTH);
				stemmingRulePanel.add(this.stemmingRules, BorderLayout.CENTER);
				
				
				//	layout the whole stuff
				JPanel topPanel = new JPanel(new GridBagLayout(), true);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridheight = 1;
				gbc.gridy = 0;
				
				gbc.gridx = 0;
				gbc.gridwidth = 8;
				topPanel.add(new JLabel("OmniFAT Instance Description", JLabel.LEFT), gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.gridwidth = 8;
				JScrollPane descriptionBox = new JScrollPane(this.description);
				descriptionBox.setPreferredSize(new Dimension(200, 100));
				topPanel.add(descriptionBox, gbc.clone());
				gbc.gridy++;
				
				gbc.gridwidth = 1;
				gbc.gridx = 0;
				gbc.weightx = 1;
				topPanel.add(new JLabel("Fuzzy Match Threshold", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.fuzzyMatchThreshold, gbc.clone());
				gbc.gridx++;
				topPanel.add(new JLabel("Minimum Abbreviation Length", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.minAbbreviationLength, gbc.clone());
				gbc.gridx++;
				topPanel.add(new JLabel("Maximum Abbreviation Length", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.maxAbbreviationLength, gbc.clone());
				gbc.gridx++;
				topPanel.add(new JLabel("Accept Undotted Abbreviations", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.unDottedAbbreviations, gbc.clone());
				
				this.tabs = new JTabbedPane();
				this.tabs.setTabPlacement(JTabbedPane.LEFT);
				this.tabs.addTab("New Epithet Labels", this.newEpithetLabels);
				
				this.tabs.addTab("Intra Epithet Punctuation", this.intraEpithetPunctuation);
				this.tabs.addTab("Inter Epithet Punctuation", this.interEpithetPunctuation);
				
				this.tabs.addTab("Negative Dictionaries", this.negativeDictionaryList);
				this.tabs.addTab("Negative Patterns", this.negativePatternList);
				this.tabs.addTab("Stemming Lookup Rules", stemmingRulePanel);
				
				this.add(topPanel, BorderLayout.NORTH);
				this.add(this.tabs, BorderLayout.CENTER);
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				if (!this.newEpithetLabels.checkConsistency()) {
					this.tabs.setSelectedComponent(this.newEpithetLabels);
					return false;
				}
				
				if (!this.intraEpithetPunctuation.checkConsistency()) {
					this.tabs.setSelectedComponent(this.intraEpithetPunctuation);
					return false;
				}
				if (!this.interEpithetPunctuation.checkConsistency()) {
					this.tabs.setSelectedComponent(this.interEpithetPunctuation);
					return false;
				}
				
				if (!this.negativeDictionaryList.checkConsistency()) {
					this.tabs.setSelectedComponent(this.negativeDictionaryList);
					return false;
				}
				if (!this.negativePatternList.checkConsistency()) {
					this.tabs.setSelectedComponent(this.negativePatternList);
					return false;
				}
				
				int minAbbreviationLength = Integer.parseInt(this.minAbbreviationLength.getValue().toString());
				int maxAbbreviationLength = Integer.parseInt(this.maxAbbreviationLength.getValue().toString());
				if (maxAbbreviationLength < minAbbreviationLength) {
					JOptionPane.showMessageDialog(this, ("Maximum abbreviation length must be equal to or greater than minimum abbreviation length."), "Invalid Abbreviation Length Settings", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				return true;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				ArrayList nodes = new ArrayList();
				
				TreeNode description = new TreeNode(null, "description");
				description.addChildNode(new TreeNode(description, TreeNode.DATA_NODE_TYPE, this.description.getText()));
				nodes.add(description);
				
				TreeNode fuzzyMatch = new TreeNode(null, "fuzzyMatch");
				fuzzyMatch.setAttribute("threshold", ("" + this.fuzzyMatchThreshold.getValue()));
				nodes.add(fuzzyMatch);
				
				TreeNode abbreviationLengthInLetters = new TreeNode(null, "abbreviationLengthInLetters");
				abbreviationLengthInLetters.setAttribute("min", ("" + this.minAbbreviationLength.getValue()));
				abbreviationLengthInLetters.setAttribute("max", ("" + this.maxAbbreviationLength.getValue()));
				abbreviationLengthInLetters.setAttribute("unDotted", (this.unDottedAbbreviations.isSelected() ? "true" : "false"));
				nodes.add(abbreviationLengthInLetters);
				
				nodes.addAll(Arrays.asList(this.newEpithetLabels.getTreeNodes(filter)));
				
				nodes.addAll(Arrays.asList(this.intraEpithetPunctuation.getTreeNodes(filter)));
				nodes.addAll(Arrays.asList(this.interEpithetPunctuation.getTreeNodes(filter)));
				
				nodes.addAll(Arrays.asList(this.negativeDictionaryList.getTreeNodes(filter)));
				nodes.addAll(Arrays.asList(this.negativePatternList.getTreeNodes(filter)));
				
				for (int sr = 0; sr < this.stemmingRules.getTabCount(); sr++) {
					StemmingRulePanel srp = ((StemmingRulePanel) this.stemmingRules.getComponentAt(sr));
					nodes.addAll(Arrays.asList(srp.getTreeNodes(filter)));
				}
				
				return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
			}
		}
		
		private class StemmingRulePanel extends InternalListPanel {
			JTextField matchEnding;
			StemmingRulePanel(String matchEnding, String separator, StringVector listEntries) {
				super("", "stemmingRule", separator, listEntries);
				
				this.matchEnding = new JTextField(matchEnding);
				this.matchEnding.setPreferredSize(new Dimension(30, this.matchEnding.getPreferredSize().height));
				
				JPanel matchEndingPanel = new JPanel(new BorderLayout());
				matchEndingPanel.add(new JLabel("Match Ending"), BorderLayout.WEST);
				matchEndingPanel.add(this.matchEnding, BorderLayout.CENTER);
				this.topPanel.add(matchEndingPanel, BorderLayout.WEST);
			}
			
			String getMatchEnding() {
				return this.matchEnding.getText();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.InternalListPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				if (!super.checkConsistency())
					return false;
				
				StringVector stemmedEndings = this.getListEntries();
				if (stemmedEndings.isEmpty()) {
					JOptionPane.showMessageDialog(this, ("The list of stemmed endings must not be empty."), "No Stemmed Endings", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				String matchEnding = this.getMatchEnding();
				if ((matchEnding.length() == 0) || !matchEnding.matches("[a-zA-Z]++")) {
					JOptionPane.showMessageDialog(this, ("The match ending must consist of letters only, at least one letter."), "Empty Match Ending", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				if (stemmedEndings.containsIgnoreCase(matchEnding)) {
					JOptionPane.showMessageDialog(this, ("The list of stemmed endings must not contain the match ending."), "Match Ending in Stemmed Ending List", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				return true;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.InternalListPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				TreeNode[] nodes = super.getTreeNodes(filter);
				for (int n = 0; n < nodes.length; n++)
					nodes[n].setAttribute("matchEnding", this.getMatchEnding());
				return nodes;
			}
		}
		
		private class RankGroupPanel extends OmniFatInstanceEditorDetailPanel {
			
			private class RankPanel extends OmniFatInstanceEditorDetailPanel {
				
				JTextField name;
				JComboBox labeled;
				JCheckBox required;
				JTextField epithetDisplayPattern;
				JSpinner probability;
				
				InternalListPanel labelList;
				
				DictionaryTablePanel dictionaryList;
				PatternTablePanel patternList;
				
				JTabbedPane tabs;
				
				RankPanel(TreeNode rankNode, String[] dictionaryNames, String[] patternNames) {
					
					//	load name
					this.name = new JTextField(rankNode.getAttribute("name", ""));
					
					//	check epithet labeling mode
					this.labeled = new JComboBox(EPITHET_LABELING_MODES);
					this.labeled.setSelectedItem(rankNode.getAttribute("labeled", "sometimes"));
					
					//	check if rank is required if epithets above and below this rank are present in a taxon name
					this.required = new JCheckBox("", "true".equals(rankNode.getAttribute("required", "false")));
					
					//	load rank probability
					this.probability = new JSpinner(new SpinnerNumberModel(5, 0, 10, 1));
					this.probability.setValue(new Integer(rankNode.getAttribute("probability", "5")));
					
					//	load epithet labels
					TreeNode[] labelNodes;
					String labelSeparator = ";";
					StringVector labels = new StringVector();
					
					//	individual labels and label lists
					labelNodes = rankNode.getChildNodes("label");
					for (int l = 0; l < labelNodes.length; l++) {
						String label = labelNodes[l].getAttribute("value");
						if (label != null)
							labels.addElementIgnoreDuplicates(label);
					}
					labelNodes = rankNode.getChildNodes("labels");
					for (int l = 0; l < labelNodes.length; l++) {
						labelSeparator = labelNodes[l].getAttribute("separator", ";");
						TreeNode dataNode =labelNodes[l].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
						if (dataNode != null)
							labels.parseAndAddElements(dataNode.getNodeValue(), labelSeparator);
					}
					labels.removeDuplicateElements();
					this.labelList = new InternalListPanel("Epithet Labels", "labels", labelSeparator, labels);
					
					//	read epithet display pattern
					this.epithetDisplayPattern = new JTextField(rankNode.getAttribute("epithetDisplayPattern", "@epithet"));
					
					//	load dictionaries
					TreeNode[] dictionaryNodes = rankNode.getChildNodes("dictionary");
					this.dictionaryList = new DictionaryTablePanel(dictionaryNames, EPITHET_DICTIONARY_TYPES, false, dictionaryNodes);
					
					//	load patterns
					TreeNode[] patternNodes = rankNode.getChildNodes("pattern");
					this.patternList = new PatternTablePanel(patternNames, EPITHET_PATTERN_TYPES, patternNodes, 1);
					
					
					//	layout the whole stuff
					JPanel topPanel = new JPanel(new GridBagLayout(), true);
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.insets.top = 3;
					gbc.insets.bottom = 3;
					gbc.insets.left = 3;
					gbc.insets.right = 3;
					gbc.weighty = 0;
					gbc.fill = GridBagConstraints.BOTH;
					gbc.gridwidth = 1;
					gbc.gridheight = 1;
					gbc.gridy = 0;
					
					gbc.gridx = 0;
					gbc.weightx = 1;
					topPanel.add(new JLabel("Rank Name", JLabel.RIGHT), gbc.clone());
					gbc.gridx++;
					topPanel.add(this.name, gbc.clone());
					gbc.gridx++;
					topPanel.add(new JLabel("Labeled", JLabel.RIGHT), gbc.clone());
					gbc.gridx++;
					topPanel.add(this.labeled, gbc.clone());
					gbc.gridx++;
					topPanel.add(new JLabel("Required", JLabel.RIGHT), gbc.clone());
					gbc.gridx++;
					topPanel.add(this.required, gbc.clone());
					gbc.gridy++;
					
					gbc.gridx = 0;
					JButton baseEpithetTestButton = new JButton("Test to Base Epithets");
					baseEpithetTestButton.setPreferredSize(buttonSize);
					baseEpithetTestButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (InstanceEditor.this.checkConsistency()) {
								TreeNode configNode = getConfigNode(new HashSet() {
									public boolean contains(Object o) {
										String s = ((String) o);
										return (!s.startsWith("rank") || ("rankGroup." + getRankGroupName() + "." + getRankName()).startsWith(s));
									}
								});
								testInstance("RankTest", configNode, OmniFAT.BASE_EPITHET_TYPE, ("Rank '" + getRankName() + "'"));
							}
						}
					});
					topPanel.add(baseEpithetTestButton, gbc.clone());
					gbc.gridx++;
					
					JButton epithetTestButton = new JButton("Test to Epithets");
					epithetTestButton.setPreferredSize(buttonSize);
					epithetTestButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (InstanceEditor.this.checkConsistency()) {
								TreeNode configNode = getConfigNode(new HashSet() {
									public boolean contains(Object o) {
										String s = ((String) o);
										return (!s.startsWith("rank") || ("rankGroup." + getRankGroupName() + "." + getRankName()).startsWith(s));
									}
								});
								testInstance("RankTest", configNode, OmniFAT.EPITHET_TYPE, ("Rank '" + getRankName() + "'"));
							}
						}
					});
					topPanel.add(epithetTestButton, gbc.clone());
					gbc.gridx++;
					
					topPanel.add(new JLabel("Display Pattern", JLabel.RIGHT), gbc.clone());
					gbc.gridx++;
					topPanel.add(this.epithetDisplayPattern, gbc.clone());
					gbc.gridx++;
					topPanel.add(new JLabel("Probability", JLabel.RIGHT), gbc.clone());
					gbc.gridx++;
					topPanel.add(this.probability, gbc.clone());
					
					
					this.tabs = new JTabbedPane();
					this.tabs.addTab("Epithet Labels", this.labelList);
					this.tabs.addTab("Epithet Dictionaries", this.dictionaryList);
					this.tabs.addTab("Patterns", this.patternList);
					
					this.add(topPanel, BorderLayout.NORTH);
					this.add(this.tabs, BorderLayout.CENTER);
				}
				
				String getRankName() {
					return this.name.getText();
				}
				
				/* (non-Javadoc)
				 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
				 */
				boolean checkConsistency() {
					if (!this.labelList.checkConsistency()) {
						this.tabs.setSelectedComponent(this.labelList);
						return false;
					}
					
					if (!this.dictionaryList.checkConsistency()) {
						this.tabs.setSelectedComponent(this.dictionaryList);
						return false;
					}
					if (!this.patternList.checkConsistency()) {
						this.tabs.setSelectedComponent(this.patternList);
						return false;
					}
					
					String name = this.getRankName();
					if ((name.length() == 0) || !name.matches("[a-zA-Z]++")) {
						JOptionPane.showMessageDialog(this, ("The name of a rank must consist of letters only, at least one letter."), "Invalid Rank Name", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					return true;
				}
				
				/* (non-Javadoc)
				 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
				 */
				TreeNode[] getTreeNodes(HashSet filter) {
					TreeNode[] nodes = {new TreeNode(null, "rank")};
					
					nodes[0].setAttribute("name", this.name.getText());
					
					nodes[0].setAttribute("labeled", ("" + this.labeled.getSelectedItem()));
					
					nodes[0].setAttribute("required", (this.required.isSelected() ? "true" : "false"));
					
					nodes[0].setAttribute("epithetDisplayPattern", this.epithetDisplayPattern.getText());
					
					nodes[0].setAttribute("probability", ("" + this.probability.getValue()));
					
					TreeNode[] labelNodes = this.labelList.getTreeNodes(filter);
					for (int l = 0; l < labelNodes.length; l++) {
						nodes[0].addChildNode(labelNodes[l]);
						labelNodes[l].setParent(nodes[0]);
					}
					
					TreeNode[] dictionaryNodes = this.dictionaryList.getTreeNodes(filter);
					for (int d = 0; d < dictionaryNodes.length; d++) {
						nodes[0].addChildNode(dictionaryNodes[d]);
						dictionaryNodes[d].setParent(nodes[0]);
					}
					
					TreeNode[] patternNodes = this.patternList.getTreeNodes(filter);
					for (int p = 0; p < patternNodes.length; p++) {
						nodes[0].addChildNode(patternNodes[p]);
						patternNodes[p].setParent(nodes[0]);
					}
					
					return nodes;
				}
			}
			
			JTextField name;
			
			JComboBox capitalized;
			JCheckBox repeatedEpithets;
			JCheckBox inCombinations;
			
			JCheckBox suffixDiff;
			JComboBox learningMode;
			
			DictionaryTablePanel dictionaryList;
			PatternTablePanel patternList;
			
			JTabbedPane ranks;
			JPanel rankPanel;
			
			JTabbedPane tabs;
			
			RankGroupPanel(TreeNode rankGroupNode, final String[] dictionaryNames, final String[] patternNames) {
				
				//	load name
				this.name = new JTextField(rankGroupNode.getAttribute("name", ""));
				
				//	check epithet capitalization mode
				this.capitalized = new JComboBox(EPITHET_CAPITALIZATION_MODES);
				this.capitalized.setSelectedItem(rankGroupNode.getAttribute("capitalized", "mixed"));
				
				//	check epithet repetitions
				this.repeatedEpithets = new JCheckBox("", "true".equals(rankGroupNode.getAttribute("repeatedEpithets", "false")));
				
				//	check combinability
				this.inCombinations = new JCheckBox("", "true".equals(rankGroupNode.getAttribute("inCombinations", "true")));
				
				//	check learning mode
				this.learningMode = new JComboBox(EPITHET_LEARNING_MODES);
				this.learningMode.setSelectedItem(rankGroupNode.getAttribute("learningMode", "teach"));
				
				//	check suffix diff setting & prepare suffixes
				this.suffixDiff = new JCheckBox("", "true".equals(rankGroupNode.getAttribute("doSuffixDiff", "false")));
				
				//	load dictionaries
				TreeNode[] dictionaryNodes = rankGroupNode.getChildNodes("dictionary");
				this.dictionaryList = new DictionaryTablePanel(dictionaryNames, EPITHET_DICTIONARY_TYPES, false, dictionaryNodes);
				
				//	load patterns
				TreeNode[] patternNodes = rankGroupNode.getChildNodes("pattern");
				this.patternList = new PatternTablePanel(patternNames, EPITHET_PATTERN_TYPES, patternNodes, 1);
				
				//	add ranks
				this.ranks = new JTabbedPane();
				this.ranks.setTabPlacement(JTabbedPane.LEFT);
				TreeNode[] rankNodes = rankGroupNode.getChildNodes("rank");
				for (int r = 0; r < rankNodes.length; r++) {
					final RankPanel rp = new RankPanel(rankNodes[r], dictionaryNames, patternNames);
					rp.name.addFocusListener(new FocusAdapter() {
						public void focusLost(FocusEvent fe) {
							int sri = ranks.indexOfComponent(rp);
							if (sri != -1)
								ranks.setTitleAt(sri, rp.name.getText());
						}
					});
					this.ranks.addTab(rp.name.getText(), rp);
				}
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
				JButton button;
				button = new JButton("Add Rank");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						final RankPanel rp = new RankPanel(new TreeNode(null, "rank"), dictionaryNames, patternNames);
						rp.name.addFocusListener(new FocusAdapter() {
							public void focusLost(FocusEvent fe) {
								int sri = ranks.indexOfComponent(rp);
								if (sri != -1)
									ranks.setTitleAt(sri, rp.name.getText());
							}
						});
						ranks.addTab(rp.name.getText(), rp);
						ranks.setSelectedComponent(rp);
					}
				});
				buttonPanel.add(button);
				button = new JButton("Remove Rank");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						int index = ranks.getSelectedIndex();
						if (index != -1)
							ranks.removeTabAt(index);
					}
				});
				buttonPanel.add(button);
				button = new JButton("Move Up");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						int index = ranks.getSelectedIndex();
						if (index > 0) {
							RankPanel rp = ((RankPanel) ranks.getComponentAt(index));
							ranks.removeTabAt(index);
							ranks.insertTab(rp.name.getText(), null, rp, null, (index-1));
							ranks.setSelectedIndex(index-1);
						}
					}
				});
				buttonPanel.add(button);
				button = new JButton("Move Down");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						int index = ranks.getSelectedIndex();
						if ((index+1) < ranks.getTabCount()) {
							RankPanel rp = ((RankPanel) ranks.getComponentAt(index));
							ranks.removeTabAt(index);
							ranks.insertTab(rp.name.getText(), null, rp, null, (index+1));
							ranks.setSelectedIndex(index+1);
						}
					}
				});
				buttonPanel.add(button);
				
				this.rankPanel = new JPanel(new BorderLayout(), true);
				this.rankPanel.add(buttonPanel, BorderLayout.NORTH);
				this.rankPanel.add(this.ranks, BorderLayout.CENTER);
				
				
				//	layout the whole stuff
				JPanel topPanel = new JPanel(new GridBagLayout(), true);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.gridy = 0;
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				topPanel.add(new JLabel("Rank Group Name", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.name, gbc.clone());
				gbc.gridx++;
				topPanel.add(new JLabel("Epithets Capitalized", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.capitalized, gbc.clone());
				gbc.gridx++;
				topPanel.add(new JLabel("In Combinations", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.inCombinations, gbc.clone());
				gbc.gridx++;
				topPanel.add(new JLabel("Repeated Epithets", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.repeatedEpithets, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				JButton baseEpithetTestButton = new JButton("Test to Base Epithets");
				baseEpithetTestButton.setPreferredSize(buttonSize);
				baseEpithetTestButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (InstanceEditor.this.checkConsistency()) {
							TreeNode configNode = getConfigNode(new HashSet() {
								public boolean contains(Object o) {
									String s = ((String) o);
									return (!s.startsWith("rank") || s.startsWith("rankGroup." + getRankGroupName()));
								}
							});
							testInstance("Rank Group Test", configNode, OmniFAT.BASE_EPITHET_TYPE, ("Rank Group '" + getRankGroupName() + "'"));
						}
					}
				});
				topPanel.add(baseEpithetTestButton, gbc.clone());
				gbc.gridx++;
				
				JButton epithetTestButton = new JButton("Test to Epithets");
				epithetTestButton.setPreferredSize(buttonSize);
				epithetTestButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (InstanceEditor.this.checkConsistency()) {
							TreeNode configNode = getConfigNode(new HashSet() {
								public boolean contains(Object o) {
									String s = ((String) o);
									return (!s.startsWith("rank") || s.startsWith("rankGroup." + getRankGroupName()));
								}
							});
							testInstance("RankGroupTest", configNode, OmniFAT.EPITHET_TYPE, ("Rank Group '" + getRankGroupName() + "'"));
						}
					}
				});
				topPanel.add(epithetTestButton, gbc.clone());
				gbc.gridx++;
				
				topPanel.add(new JLabel("Learning Mode", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.learningMode, gbc.clone());
				gbc.gridx++;
				
				gbc.gridx++;
				gbc.gridx++;
				
				topPanel.add(new JLabel("Do Suffix Diff", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				topPanel.add(this.suffixDiff, gbc.clone());
				gbc.gridy++;
				
				this.tabs = new JTabbedPane();
				this.tabs.addTab("Epithet Dictionaries", this.dictionaryList);
				this.tabs.addTab("Patterns", this.patternList);
				this.tabs.addTab("Ranks", this.rankPanel);
				
				this.add(topPanel, BorderLayout.NORTH);
				this.add(this.tabs, BorderLayout.CENTER);
			}
			
			String getRankGroupName() {
				return this.name.getText();
			}
			
			String[] getRankNames() {
				String[] rankNames = new String[this.ranks.getTabCount()];
				for (int r = 0; r < this.ranks.getTabCount(); r++) {
					RankPanel rp = ((RankPanel) this.ranks.getComponentAt(r));
					rankNames[r] = rp.getRankName();
				}
				return rankNames;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				if (!this.dictionaryList.checkConsistency()) {
					this.tabs.setSelectedComponent(this.dictionaryList);
					return false;
				}
				if (!this.patternList.checkConsistency()) {
					this.tabs.setSelectedComponent(this.patternList);
					return false;
				}
				
				for (int r = 0; r < this.ranks.getTabCount(); r++) {
					RankPanel rp = ((RankPanel) this.ranks.getComponentAt(r));
					if (!rp.checkConsistency()) {
						this.tabs.setSelectedComponent(this.rankPanel);
						this.ranks.setSelectedComponent(rp);
						return false;
					}
				}
				String[] rankNames = this.getRankNames();
				HashSet uniqueRankNames = new HashSet();
				for (int r = 0; r < rankNames.length; r++)
					if (!uniqueRankNames.add(rankNames[r])) {
						JOptionPane.showMessageDialog(this, ("The names of the individual ranks must be unique, but '" + rankNames[r] + "' is duplicate."), "Duplicate Rank Name", JOptionPane.ERROR_MESSAGE);
						return false;
					}
				
				String name = this.getRankGroupName();
				if ((name.length() == 0) || !name.matches("[a-zA-Z]++")) {
					JOptionPane.showMessageDialog(this, ("The name of a rank group must consist of letters only, at least one letter."), "Invalid Rank Group Name", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				return true;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				TreeNode[] nodes = {new TreeNode(null, "rankGroup")};
				
				nodes[0].setAttribute("name", this.name.getText());
				
				nodes[0].setAttribute("capitalized", ("" + this.capitalized.getSelectedItem()));
				
				nodes[0].setAttribute("repeatedEpithets", (this.repeatedEpithets.isSelected() ? "true" : "false"));
				
				nodes[0].setAttribute("inCombinations", (this.inCombinations.isSelected() ? "true" : "false"));
				
				nodes[0].setAttribute("doSuffixDiff", (this.suffixDiff.isSelected() ? "true" : "false"));
				
				nodes[0].setAttribute("learningMode", ("" + this.learningMode.getSelectedItem()));
				
				TreeNode[] dictionaryNodes = this.dictionaryList.getTreeNodes(filter);
				for (int d = 0; d < dictionaryNodes.length; d++) {
					nodes[0].addChildNode(dictionaryNodes[d]);
					dictionaryNodes[d].setParent(nodes[0]);
				}
				
				TreeNode[] patternNodes = this.patternList.getTreeNodes(filter);
				for (int p = 0; p < patternNodes.length; p++) {
					nodes[0].addChildNode(patternNodes[p]);
					patternNodes[p].setParent(nodes[0]);
				}
				
				for (int r = 0; r < this.ranks.getTabCount(); r++) {
					RankPanel rp = ((RankPanel) this.ranks.getComponentAt(r));
					if (filter.contains("rankGroup." + this.getRankGroupName() + "." + rp.getRankName())) {
						TreeNode[] rankNodes = rp.getTreeNodes(filter);
						for (int p = 0; p < rankNodes.length; p++) {
							nodes[0].addChildNode(rankNodes[p]);
							rankNodes[p].setParent(nodes[0]);
						}
					}
				}
				
				return nodes;
			}
		}
		
		private class AuthorDataPanel extends OmniFatInstanceEditorDetailPanel {
			
			private JCheckBox embeddedAuthorNames = new JCheckBox();
			
			DictionaryTablePanel dictionaryList;
			PatternTablePanel patternList;
			
			InternalListPanel nameStopWords;
			
			InternalListPanel nameListSeparators;
			InternalListPanel nameListEndSeparators;
			
			JTabbedPane tabs;
			
			AuthorDataPanel(TreeNode authorDataNode, String[] dictionaryNames, String[] patternNames) {
				
				//	load parameters
				this.embeddedAuthorNames.setSelected("true".equals(authorDataNode.getAttribute("allowEmbedded", "true")));
				JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
				optionPanel.add(new JLabel("Allow Author Names before last Epithet"));
				optionPanel.add(this.embeddedAuthorNames);
				
				//	load dictionaries
				TreeNode[] dictionaryNodes = authorDataNode.getChildNodes("dictionary");
				this.dictionaryList = new DictionaryTablePanel(dictionaryNames, null, false, dictionaryNodes);
				
				//	load patterns
				TreeNode[] patternNodes = authorDataNode.getChildNodes("pattern");
				this.patternList = new PatternTablePanel(patternNames, AUTHOR_NAME_PATTERN_TYPES, patternNodes, 10);
				
				//	load stop words
				TreeNode[] authorNameStopWordNodes = authorDataNode.getChildNodes("nameStopWords");
				StringVector authorNameStopWords = new StringVector();
				String authorNameStopWordSeparator = ";";
				for (int sw = 0; sw < authorNameStopWordNodes.length; sw++) {
					authorNameStopWordSeparator = authorNameStopWordNodes[sw].getAttribute("separator", ";");
					TreeNode dataNode = authorNameStopWordNodes[sw].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						authorNameStopWords.parseAndAddElements(dataNode.getNodeValue(), authorNameStopWordSeparator);
				}
				authorNameStopWords.removeDuplicateElements();
				this.nameStopWords = new InternalListPanel("Stop Words Appearing in Author Names", "nameStopWords", authorNameStopWordSeparator, authorNameStopWords);
				
				//	load list end separators
				TreeNode[] authorNameListEndSeparatorNodes = authorDataNode.getChildNodes("nameListEndSeparators");
				StringVector authorNameListEndSeparators = new StringVector();
				String authorNameListEndSeparatorSeparator = ";";
				for (int les = 0; les < authorNameListEndSeparatorNodes.length; les++) {
					authorNameListEndSeparatorSeparator = authorNameListEndSeparatorNodes[les].getAttribute("separator", ";");
					TreeNode dataNode = authorNameListEndSeparatorNodes[les].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						authorNameListEndSeparators.parseAndAddElements(dataNode.getNodeValue(), authorNameListEndSeparatorSeparator);
				}
				authorNameListEndSeparators.removeDuplicateElements();
				this.nameListEndSeparators = new InternalListPanel("Separators between last two Names in Author Name Lists", "nameListEndSeparators", authorNameListEndSeparatorSeparator, authorNameListEndSeparators);
				
				//	load list separators
				TreeNode[] authorNameListSeparatorNodes = authorDataNode.getChildNodes("nameListSeparators");
				StringVector authorNameListSeparators = new StringVector();
				String authorNameListSeparatorSeparator = ";";
				for (int ls = 0; ls < authorNameListSeparatorNodes.length; ls++) {
					authorNameListSeparatorSeparator = authorNameListSeparatorNodes[ls].getAttribute("separator", ";");
					TreeNode dataNode = authorNameListSeparatorNodes[ls].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
					if (dataNode != null)
						authorNameListSeparators.parseAndAddElements(dataNode.getNodeValue(), authorNameListSeparatorSeparator);
				}
				authorNameListSeparators.removeDuplicateElements();
				this.nameListSeparators = new InternalListPanel("Separators between Names in Author Name Lists", "nameListSeparators", authorNameListSeparatorSeparator, authorNameListSeparators);
				
				
				//	layout the whole stuff
				this.tabs = new JTabbedPane();
				this.tabs.setTabPlacement(JTabbedPane.LEFT);
				this.tabs.addTab("Dictionaries", this.dictionaryList);
				this.tabs.addTab("Patterns", this.patternList);
				this.tabs.addTab("Name Stop Words", this.nameStopWords);
				this.tabs.addTab("Name List Separators", this.nameListSeparators);
				this.tabs.addTab("Name List End Separators", this.nameListEndSeparators);
				
				this.add(optionPanel, BorderLayout.NORTH);
				this.add(this.tabs, BorderLayout.CENTER);
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				if (!this.dictionaryList.checkConsistency()) {
					this.tabs.setSelectedComponent(this.dictionaryList);
					return false;
				}
				if (!this.patternList.checkConsistency()) {
					this.tabs.setSelectedComponent(this.patternList);
					return false;
				}
				
				if (!this.nameStopWords.checkConsistency()) {
					this.tabs.setSelectedComponent(this.nameStopWords);
					return false;
				}
				
				if (!this.nameListSeparators.checkConsistency()) {
					this.tabs.setSelectedComponent(this.nameListSeparators);
					return false;
				}
				if (!this.nameListEndSeparators.checkConsistency()) {
					this.tabs.setSelectedComponent(this.nameListEndSeparators);
					return false;
				}
				
				return true;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				TreeNode[] nodes = {new TreeNode(null, "authors")};
				nodes[0].setAttribute("allowEmbedded", (this.embeddedAuthorNames.isSelected() ? "true" : "false"));
				
				TreeNode[] dictionaryNodes = this.dictionaryList.getTreeNodes(filter);
				for (int d = 0; d < dictionaryNodes.length; d++) {
					nodes[0].addChildNode(dictionaryNodes[d]);
					dictionaryNodes[d].setParent(nodes[0]);
				}
				
				TreeNode[] patternNodes = this.patternList.getTreeNodes(filter);
				for (int p = 0; p < patternNodes.length; p++) {
					nodes[0].addChildNode(patternNodes[p]);
					patternNodes[p].setParent(nodes[0]);
				}
				
				TreeNode[] stopWordNodes = this.nameStopWords.getTreeNodes(filter);
				for (int sw = 0; sw < stopWordNodes.length; sw++) {
					nodes[0].addChildNode(stopWordNodes[sw]);
					stopWordNodes[sw].setParent(nodes[0]);
				}
				
				TreeNode[] listEndSeparatorNodes = this.nameListEndSeparators.getTreeNodes(filter);
				for (int les = 0; les < listEndSeparatorNodes.length; les++) {
					nodes[0].addChildNode(listEndSeparatorNodes[les]);
					listEndSeparatorNodes[les].setParent(nodes[0]);
				}
				
				TreeNode[] listSeparatorNodes = this.nameListSeparators.getTreeNodes(filter);
				for (int ls = 0; ls < listSeparatorNodes.length; ls++) {
					nodes[0].addChildNode(listSeparatorNodes[ls]);
					listSeparatorNodes[ls].setParent(nodes[0]);
				}
				
				return nodes;
			}
		}
		
		private class DataRuleTablePanel extends OmniFatInstanceEditorDetailPanel {
			
			private class DataRuleTableLine {
				
				JTextField match;
				JComboBox action;
				
				JCheckBox removeNested;
				JCheckBox likelyMatch;
				
				JButton test;
				
				DataRuleTableLine(String match, String action, boolean removeNested, boolean likelyMatch) {
					this.match = new JTextField(match);
					this.match.addFocusListener(new FocusAdapter() {
						public void focusGained(FocusEvent fe) {
							if (selectedRule != null)
								selectedRule.match.setBorder(selectedRuleBorder);
							selectedRule = DataRuleTableLine.this;
							selectedRuleBorder = DataRuleTableLine.this.match.getBorder();
							DataRuleTableLine.this.match.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
							tablePanel.validate();
						}
					});
					
					this.action = new JComboBox(DATA_RULE_ACTIONS);
					this.action.setSelectedItem(action);
					
					this.removeNested = new JCheckBox("", removeNested);
					
					this.likelyMatch = new JCheckBox("", likelyMatch);
					
					this.test = new JButton("Test");
					this.test.setPreferredSize(tableButtonSize);
					this.test.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (InstanceEditor.this.checkConsistency()) {
								final int ruleNumber = (rules.indexOf(DataRuleTableLine.this) + 1);
								TreeNode configNode = getConfigNode(new HashSet() {
									public boolean contains(Object o) {
										String s = ((String) o);
										return (!s.startsWith("data") || (s.compareTo("dataRule." + ruleSetNumber + "." + ruleNumber) <= 0));
									}
								});
								testInstance("DataRuleTest", configNode, null, ("Data Rule '" + ruleSetNumber + "." + ruleNumber + "'"));
							}
						}
					});
				}
			}
			
			ArrayList rules = new ArrayList();
			
			int ruleSetNumber = 0;
			
			DataRuleTableLine selectedRule = null;
			Border selectedRuleBorder = null;
			
			JSpinner maxRounds;
			
			JPanel tablePanel = new JPanel(new GridBagLayout(), true);
			
			DataRuleTablePanel(TreeNode[] ruleNodes) {
				for (int r = 0; r < ruleNodes.length; r++)
					this.rules.add(new DataRuleTableLine(ruleNodes[r].getAttribute("match"), ruleNodes[r].getAttribute("action"), "true".equals(ruleNodes[r].getAttribute("removeNested", "false")), "true".equals(ruleNodes[r].getAttribute("likelyMatch", "false"))));
				
				this.maxRounds = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
				JButton button;
				button = new JButton("Add Rule");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						DataRuleTableLine drtl = new DataRuleTableLine("", "remove", false, false);
						rules.add(0, drtl);
						layoutRuleTable();
						drtl.match.requestFocusInWindow();
					}
				});
				buttonPanel.add(button);
				button = new JButton("Remove Rule");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (selectedRule != null) {
							selectedRule.match.setBorder(selectedRuleBorder);
							rules.remove(selectedRule);
							layoutRuleTable();
							selectedRule = null;
							selectedRuleBorder = null;
						}
					}
				});
				buttonPanel.add(button);
				button = new JButton("Move Up");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (selectedRule == null)
							return;
						
						int index = rules.indexOf(selectedRule);
						if (index > 0) {
							DataRuleTableLine rp = ((DataRuleTableLine) rules.remove(index));
							rules.add((index-1), rp);
							layoutRuleTable();
							rp.match.requestFocusInWindow();
						}
					}
				});
				buttonPanel.add(button);
				button = new JButton("Move Down");
				button.setPreferredSize(buttonSize);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (selectedRule == null)
							return;
						
						int index = rules.indexOf(selectedRule);
						if ((index+1) < rules.size()) {
							DataRuleTableLine rp = ((DataRuleTableLine) rules.remove(index));
							rules.add((index+1), rp);
							layoutRuleTable();
							rp.match.requestFocusInWindow();
						}
					}
				});
				buttonPanel.add(button);
				
				JPanel maxRoundPanel = new JPanel(new BorderLayout());
				maxRoundPanel.add(new JLabel("Max Rounds ", JLabel.RIGHT), BorderLayout.CENTER);
				maxRoundPanel.add(this.maxRounds, BorderLayout.EAST);
				
				JPanel topPanel = new JPanel(new BorderLayout());
				topPanel.add(buttonPanel, BorderLayout.CENTER);
				topPanel.add(maxRoundPanel, BorderLayout.EAST);
				
				JScrollPane tablePanelBox = new JScrollPane(this.tablePanel);
				tablePanelBox.getVerticalScrollBar().setUnitIncrement(25);
				this.add(topPanel, BorderLayout.NORTH);
				this.add(tablePanelBox, BorderLayout.CENTER);
				this.layoutRuleTable();
			}
			
			private void layoutRuleTable() {
				this.tablePanel.removeAll();
				
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridy = 0;
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				this.tablePanel.add(new JLabel("Match", JLabel.CENTER), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Action", JLabel.CENTER), gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Remove Nested", JLabel.CENTER), gbc.clone());
				gbc.gridx = 3;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Likely Match", JLabel.CENTER), gbc.clone());
				gbc.gridx = 4;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Test", JLabel.CENTER), gbc.clone());
				gbc.gridy++;
				
				for (int p = 0; p < this.rules.size(); p++) {
					DataRuleTableLine drtl = ((DataRuleTableLine) this.rules.get(p));
					gbc.gridx = 0;
					gbc.weightx = 1;
					this.tablePanel.add(drtl.match, gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 0;
					this.tablePanel.add(drtl.action, gbc.clone());
					gbc.gridx = 2;
					gbc.weightx = 0;
					this.tablePanel.add(drtl.removeNested, gbc.clone());
					gbc.gridx = 3;
					gbc.weightx = 0;
					this.tablePanel.add(drtl.likelyMatch, gbc.clone());
					gbc.gridx = 4;
					gbc.weightx = 0;
					this.tablePanel.add(drtl.test, gbc.clone());
					gbc.gridy++;
				}
				
				this.tablePanel.validate();
				this.tablePanel.repaint();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				HashSet uniqueMatches = new HashSet();
				for (int p = 0; p < this.rules.size(); p++) {
					DataRuleTableLine drtl = ((DataRuleTableLine) this.rules.get(p));
					String match = drtl.match.getText();
					if ((match.length() == 0) || !match.replaceAll("[^a-zA-Z]", "").matches("[plabun]++")) {
						JOptionPane.showMessageDialog(this,
								("The match of a data rule must contain only these letters:" +
								"\n - 'p' for a sure positive epithet" +
								"\n - 'l' for a likely positive epithet" +
								"\n - 'a' for an ambiguous epithet" +
								"\n - 'b' for an abbreviated epithet" +
								"\n - 'u' for an unknown epithet" +
								"\n - 'n' for a negative/excluded epithet"),
								"Invalid Match", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					if (!uniqueMatches.add(match)) {
						JOptionPane.showMessageDialog(this, ("The match expressions of data rules must not be duplicated within one rule set, but '" + match + "' is duplicate."), "Duplicate Match Expression", JOptionPane.ERROR_MESSAGE);
						return false;
					}
				}
				
				return true;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				TreeNode[] nodes = {new TreeNode(null, "ruleSet")};
				
				for (int dr = 0; dr < this.rules.size(); dr++) {
					DataRuleTableLine drtl = ((DataRuleTableLine) this.rules.get(dr));
					if (filter.contains("dataRule." + this.ruleSetNumber + "." + (dr+1))) {
						TreeNode node = new TreeNode(nodes[0], "rule");
						node.setAttribute("match", drtl.match.getText());
						node.setAttribute("action", ("" + drtl.action.getSelectedItem()));
						node.setAttribute("removeNested", (drtl.removeNested.isSelected() ? "true" : "false"));
						node.setAttribute("likelyMatch", (drtl.likelyMatch.isSelected() ? "true" : "false"));
						nodes[0].addChildNode(node);
					}
				}
				
				nodes[0].setAttribute("maxRounds", ("" + this.maxRounds.getValue()));
				
				return nodes;
			}
		}
		
		private class DictionaryTablePanel extends OmniFatInstanceEditorDetailPanel {
			private class DictionaryTableLine {
				JLabel name;
				JCheckBox use;
				JComboBox type;
				JCheckBox trusted;
				JSpinner suffixDiffThreshold;
				DictionaryTableLine(String name, boolean selected, String[] types, String type, boolean trusted, int suffixDiffThreshold) {
					this.name = new JLabel(name);
					this.use = new JCheckBox("", selected);
					if (types != null) {
						this.type = new JComboBox(types);
						this.type.setEditable(false);
						this.type.setSelectedItem(type);
					}
					this.trusted = new JCheckBox("", trusted);
					if (suffixDiffThreshold > -1)
						this.suffixDiffThreshold = new JSpinner(new SpinnerNumberModel(suffixDiffThreshold, 0, 10, 1));
				}
			}
			
			DictionaryTableLine[] dictionaries;
			boolean showTypes;
			boolean showSuffixDiff;
			
			JPanel tablePanel = new JPanel(new GridBagLayout(), true);
			
			DictionaryTablePanel(String[] dictionaryNames, String[] types, boolean showSuffixDiff, TreeNode[] dictionaryNodes) {
				this.dictionaries = new DictionaryTableLine[dictionaryNames.length];
				this.showTypes = ((types != null) && (types.length > 1));
				this.showSuffixDiff = showSuffixDiff;
				
				HashSet selectedUse = new HashSet();
				Properties selectedTypes = new Properties();
				HashSet selectedTrusted = new HashSet();
				HashMap selectedSuffixDiffThresholds = new HashMap();
				for (int d = 0; d < dictionaryNodes.length; d++) {
					String name = dictionaryNodes[d].getAttribute("ref");
					selectedUse.add(name);
					if (this.showTypes)
						selectedTypes.setProperty(name, dictionaryNodes[d].getAttribute("type", types[0]));
					if ("true".equals(dictionaryNodes[d].getAttribute("trusted", "false")))
						selectedTrusted.add(name);
					if (showSuffixDiff) try {
						Integer sdt = new Integer(dictionaryNodes[d].getAttribute("suffixDiffThreshold", "0"));
						if (sdt.intValue() != 0)
							selectedSuffixDiffThresholds.put(name, sdt);
					} catch (NumberFormatException nfe) {}
				}
				
				for (int d = 0; d < dictionaryNames.length; d++)
					this.dictionaries[d] = new DictionaryTableLine(
							dictionaryNames[d],
							selectedUse.contains(dictionaryNames[d]),
							types,
							(this.showTypes ? selectedTypes.getProperty(dictionaryNames[d], types[0]) : null),
							selectedTrusted.contains(dictionaryNames[d]),
							(this.showSuffixDiff ? (selectedSuffixDiffThresholds.containsKey(dictionaryNames[d]) ? ((Integer) selectedSuffixDiffThresholds.get(dictionaryNames[d])).intValue() : 0) : -1)
						);
				
				JScrollPane tablePanelBox = new JScrollPane(this.tablePanel);
				tablePanelBox.getVerticalScrollBar().setUnitIncrement(25);
				this.add(tablePanelBox, BorderLayout.CENTER);
				this.layoutDictionaryTable();
			}
			
			private void layoutDictionaryTable() {
				this.tablePanel.removeAll();
				
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridy = 0;
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				this.tablePanel.add(new JLabel("Name", JLabel.CENTER), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Use", JLabel.CENTER), gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Trusted", JLabel.CENTER), gbc.clone());
				if (this.showTypes) {
					gbc.gridx = 3;
					gbc.weightx = 0;
					this.tablePanel.add(new JLabel("Use As", JLabel.CENTER), gbc.clone());
				}
				if (this.showSuffixDiff) {
					gbc.gridx = 4;
					gbc.weightx = 0;
					this.tablePanel.add(new JLabel("Diff", JLabel.CENTER), gbc.clone());
				}
				gbc.gridy++;
				
				for (int d = 0; d < this.dictionaries.length; d++) {
					gbc.gridx = 0;
					gbc.weightx = 1;
					this.tablePanel.add(this.dictionaries[d].name, gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 0;
					this.tablePanel.add(this.dictionaries[d].use, gbc.clone());
					gbc.gridx = 2;
					gbc.weightx = 0;
					this.tablePanel.add(this.dictionaries[d].trusted, gbc.clone());
					if (this.showTypes) {
						gbc.gridx = 3;
						gbc.weightx = 0;
						this.tablePanel.add(this.dictionaries[d].type, gbc.clone());
					}
					if (this.showSuffixDiff) {
						gbc.gridx = 4;
						gbc.weightx = 0;
						this.tablePanel.add(this.dictionaries[d].suffixDiffThreshold, gbc.clone());
					}
					gbc.gridy++;
				}
				
				this.tablePanel.validate();
				this.tablePanel.repaint();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				return true; // nothing to check here
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				ArrayList nodes = new ArrayList();
				
				for (int d = 0; d < this.dictionaries.length; d++)
					if (this.dictionaries[d].use.isSelected()) {
						TreeNode node = new TreeNode(null, "dictionary");
						node.setAttribute("ref", this.dictionaries[d].name.getText());
						if (this.showTypes)
							node.setAttribute("type", ("" + this.dictionaries[d].type.getSelectedItem()));
						if (this.dictionaries[d].trusted.isSelected())
							node.setAttribute("trusted", "true");
						if (this.showSuffixDiff)
							node.setAttribute("suffixDiffThreshold", ("" + this.dictionaries[d].suffixDiffThreshold.getValue()));
						nodes.add(node);
					}
				
				return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
			}
		}
		
		private class PatternTablePanel extends OmniFatInstanceEditorDetailPanel {
			
			private class PatternTableLine {
				
				JLabel name;
				JTextField pattern;
				JComponent string;
				
				JCheckBox isRef;
				JCheckBox use;
				JComboBox type;
				
				JButton button;
				
				JButton removeButton;
				
				int maxTokens;
				
				PatternTableLine(final String string, boolean isRef, boolean use, String[] types, String type, int maxTokens) {
					this.maxTokens = maxTokens;
					
					if (isRef) {
						this.name = new JLabel(string, JLabel.LEFT);
						this.string = this.name;
						
						this.button = new JButton("Edit");
						this.button.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								editPattern(string, PatternTableLine.this.maxTokens);
							}
						});
						
						this.removeButton = new JButton("Remove");
						this.removeButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								PatternTableLine.this.use.setSelected(false);
							}
						});
					}
					else {
						this.pattern = new JTextField(string);
						this.string = this.pattern;
						
						this.button = new JButton("Test");
						this.button.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String rawPattern = pattern.getText();
								testPattern(rawPattern, rawPattern, true, PatternTableLine.this.maxTokens);
							}
						});
						
						this.removeButton = new JButton("Remove");
						this.removeButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								PatternTablePanel.this.patterns.remove(PatternTableLine.this);
								PatternTablePanel.this.layoutPatternTable();
							}
						});
					}
					this.button.setPreferredSize(tableButtonSize);
					this.removeButton.setPreferredSize(tableButtonSize);
					
					this.isRef = new JCheckBox("", isRef);
					this.isRef.setEnabled(false);
					
					this.use = new JCheckBox("", (use || !isRef));
					this.use.setEnabled(isRef);
					
					this.type = new JComboBox(types);
					this.type.setEditable(false);
					this.type.setSelectedItem(type);
				}
			}
			
			ArrayList patterns = new ArrayList();
			
			int maxTokens;
			
			JPanel tablePanel = new JPanel(new GridBagLayout(), true);
			
			JButton addPattern;
			
			PatternTablePanel(String[] patternNames, final String[] types, TreeNode[] patternNodes, int maxTokens) {
				LinkedHashSet selectedUse = new LinkedHashSet();
				Properties selectedTypes = new Properties();
				for (int p = 0; p < patternNodes.length; p++) {
					String string = patternNodes[p].getAttribute("string");
					selectedUse.add(string);
					selectedTypes.setProperty(string, patternNodes[p].getAttribute("type", types[0]));
				}
				
				this.maxTokens = maxTokens;
				
				ArrayList globalPatterns = new ArrayList();
				for (int p = 0; p < patternNames.length; p++)
					globalPatterns.add(new PatternTableLine(patternNames[p], true, selectedUse.remove(patternNames[p]), types, selectedTypes.getProperty(patternNames[p], types[0]), this.maxTokens));
				
				for (Iterator pit = selectedUse.iterator(); pit.hasNext();) {
					String pattern = ((String) pit.next());
					this.patterns.add(new PatternTableLine(pattern, false, true, types, selectedTypes.getProperty(pattern, types[0]), this.maxTokens));
				}
				this.patterns.addAll(globalPatterns);
				
				this.addPattern = new JButton("Add Pattern");
				this.addPattern.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						PatternTableLine ptl = new PatternTableLine("", false, true, types, types[0], PatternTablePanel.this.maxTokens);
						patterns.add(0, ptl);
						layoutPatternTable();
					}
				});
				
				JScrollPane tablePanelBox = new JScrollPane(this.tablePanel);
				tablePanelBox.getVerticalScrollBar().setUnitIncrement(25);
				this.add(tablePanelBox, BorderLayout.CENTER);
				this.layoutPatternTable();
			}
			
			private void layoutPatternTable() {
				this.tablePanel.removeAll();
				
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 3;
				gbc.insets.bottom = 3;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.weighty = 0;
				gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridy = 0;
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				this.tablePanel.add(new JLabel("Name/Pattern", JLabel.CENTER), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Global", JLabel.CENTER), gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Use", JLabel.CENTER), gbc.clone());
				gbc.gridx = 3;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Use As", JLabel.CENTER), gbc.clone());
				gbc.gridx = 4;
				gbc.weightx = 0;
				this.tablePanel.add(new JLabel("Edit/Test", JLabel.CENTER), gbc.clone());
				gbc.gridx = 5;
				gbc.weightx = 0;
				this.tablePanel.add(this.addPattern, gbc.clone());
				gbc.gridy++;
				
				for (int p = 0; p < this.patterns.size(); p++) {
					PatternTableLine ptl = ((PatternTableLine) this.patterns.get(p));
					gbc.gridx = 0;
					gbc.weightx = 1;
					this.tablePanel.add(ptl.string, gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 0;
					this.tablePanel.add(ptl.isRef, gbc.clone());
					gbc.gridx = 2;
					gbc.weightx = 0;
					this.tablePanel.add(ptl.use, gbc.clone());
					gbc.gridx = 3;
					gbc.weightx = 0;
					this.tablePanel.add(ptl.type, gbc.clone());
					gbc.gridx = 4;
					gbc.weightx = 0;
					this.tablePanel.add(ptl.button, gbc.clone());
					gbc.gridx = 5;
					gbc.weightx = 0;
					this.tablePanel.add(ptl.removeButton, gbc.clone());
					gbc.gridy++;
				}
				
				this.tablePanel.validate();
				this.tablePanel.repaint();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#checkConsistency()
			 */
			boolean checkConsistency() {
				return true; // nothing to check here
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFatInstanceEditor.OmniFatInstanceEditorDetailPanel#getTreeNodes()
			 */
			TreeNode[] getTreeNodes(HashSet filter) {
				ArrayList nodes = new ArrayList();
				
				for (int p = 0; p < this.patterns.size(); p++) {
					PatternTableLine ptl = ((PatternTableLine) this.patterns.get(p));
					if (ptl.use.isSelected()) {
						TreeNode node = new TreeNode(null, "pattern");
						if (ptl.isRef.isSelected()) {
							node.setAttribute("string", ptl.name.getText());
							node.setAttribute("isRef", "true");
						}
						else {
							node.setAttribute("string", ptl.pattern.getText());
							node.setAttribute("isRef", "false");
						}
						node.setAttribute("type", ("" + ptl.type.getSelectedItem()));
						nodes.add(node);
					}
				}
				
				return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
			}
		}
		
		private void editPattern(String patternName, int maxTokens) {
			DataItemDialog did;
			Window top = DialogFactory.getTopWindow();
			if (top instanceof JDialog)
				did = new DataItemDialog(((JDialog) top), ("Edit Pattern '" + patternName + "'"), patternName, false, adp);
			else if (top instanceof JFrame)
				did = new DataItemDialog(((JFrame) top), ("Edit Pattern '" + patternName + "'"), patternName, false, adp);
			else did = new DataItemDialog(((JFrame) null), ("Edit Pattern '" + patternName + "'"), patternName, false, adp);
			
			PatternEditor pe = new PatternEditor(patternName, loadPattern(patternName, ""), maxTokens);
			did.getContentPane().add(pe, BorderLayout.CENTER);
			did.setSize(400, 600);
			did.setLocationRelativeTo(this);
			did.setVisible(true);
			
			if (did.isCommitted()) try {
				storePattern(patternName, pe.editor.getContent());
			}
			catch (IOException ioe) {
				System.out.println("Could not store pattern '" + patternName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		
		private OmniFAT.Base base;
		
		private BaseDataPanel baseData;
		
		private JTabbedPane rankGroups;
		private JPanel rankGroupPanel;
		
		private AuthorDataPanel authorData;
		
		private JTabbedPane dataRuleSets;
		private JPanel dataRuleSetPanel;
		private JCheckBox cleanupAfterDataRules;
		
		private JTabbedPane tabs;
		
		InstanceEditor(final String instanceName, final OmniFAT.Base base, TreeNode configNode, final String[] dictionaryNames, final String[] patternNames) {
			super(new BorderLayout(), true);
			this.base = base;
			
			
			//	load base data
			this.baseData = new BaseDataPanel(configNode, dictionaryNames, patternNames);
			
			
			//	load rank groups
			this.rankGroups = new JTabbedPane();
			this.rankGroups.setTabPlacement(JTabbedPane.LEFT);
			TreeNode[] rankGroupNodes = configNode.getChildNodes("rankGroup");
			for (int rg = 0; rg < rankGroupNodes.length; rg++) {
				final RankGroupPanel rgp = new RankGroupPanel(rankGroupNodes[rg], dictionaryNames, patternNames);
				rgp.name.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent fe) {
						int sri = rankGroups.indexOfComponent(rgp);
						if (sri != -1)
							rankGroups.setTitleAt(sri, rgp.name.getText());
					}
				});
				this.rankGroups.addTab(rgp.name.getText(), rgp);
			}
			
			JPanel rankGroupButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
			JButton button;
			button = new JButton("Add Rank Group");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					final RankGroupPanel rgp = new RankGroupPanel(new TreeNode(null, "rankGroup"), dictionaryNames, patternNames);
					rgp.name.addFocusListener(new FocusAdapter() {
						public void focusLost(FocusEvent fe) {
							int sri = rankGroups.indexOfComponent(rgp);
							if (sri != -1)
								rankGroups.setTitleAt(sri, rgp.name.getText());
						}
					});
					rankGroups.addTab(rgp.name.getText(), rgp);
					rankGroups.setSelectedComponent(rgp);
				}
			});
			rankGroupButtonPanel.add(button);
			button = new JButton("Remove Rank Group");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = rankGroups.getSelectedIndex();
					if (index != -1)
						rankGroups.removeTabAt(index);
				}
			});
			rankGroupButtonPanel.add(button);
			button = new JButton("Move Up");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = rankGroups.getSelectedIndex();
					if (index > 0) {
						RankGroupPanel rgp = ((RankGroupPanel) rankGroups.getComponentAt(index));
						rankGroups.removeTabAt(index);
						rankGroups.insertTab(rgp.name.getText(), null, rgp, null, (index-1));
						rankGroups.setSelectedIndex(index-1);
					}
				}
			});
			rankGroupButtonPanel.add(button);
			button = new JButton("Move Down");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = rankGroups.getSelectedIndex();
					if ((index+1) < rankGroups.getTabCount()) {
						RankGroupPanel rgp = ((RankGroupPanel) rankGroups.getComponentAt(index));
						rankGroups.removeTabAt(index);
						rankGroups.insertTab(rgp.name.getText(), null, rgp, null, (index+1));
						rankGroups.setSelectedIndex(index+1);
					}
				}
			});
			rankGroupButtonPanel.add(button);
			
			this.rankGroupPanel = new JPanel(new BorderLayout(), true);
			this.rankGroupPanel.add(rankGroupButtonPanel, BorderLayout.NORTH);
			this.rankGroupPanel.add(this.rankGroups, BorderLayout.CENTER);
			
			
			//	load author data
			TreeNode authorDataNode = configNode.getChildNode("authors", 0);
			if (authorDataNode == null)
				this.authorData = new AuthorDataPanel(new TreeNode(null, "authors"), dictionaryNames, patternNames);
			else this.authorData = new AuthorDataPanel(authorDataNode, dictionaryNames, patternNames);
			
			
			//	load data rules
			this.dataRuleSets = new JTabbedPane();
			this.dataRuleSets.setTabPlacement(JTabbedPane.LEFT);
			TreeNode rulesNode = configNode.getChildNode("rules", 0);
			if (rulesNode != null) {
				TreeNode[] ruleSetNodes = rulesNode.getChildNodes("ruleSet");
				for (int rs = 0; rs < ruleSetNodes.length; rs++) {
					DataRuleTablePanel drsp = new DataRuleTablePanel(ruleSetNodes[rs].getChildNodes("rule"));
					try {
						drsp.maxRounds.setValue(new Integer(ruleSetNodes[rs].getAttribute("maxRounds", "0")));
					} catch (NumberFormatException nfe) {}
					drsp.ruleSetNumber = (rs + 1);
					this.dataRuleSets.addTab(("" + (rs + 1)), drsp);
				}
			}
			this.cleanupAfterDataRules = new JCheckBox("", ((rulesNode == null) || "true".equals(rulesNode.getAttribute("cleanupAfter", "true"))));
			
			JPanel cleanupAfterPanel = new JPanel(new BorderLayout(), true);
			cleanupAfterPanel.add(new JLabel("Cleanup After Data Rules ", JLabel.RIGHT), BorderLayout.CENTER);
			cleanupAfterPanel.add(this.cleanupAfterDataRules, BorderLayout.EAST);
			
			JPanel dataRuleSetButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT), true);
			button = new JButton("Add Rule Set");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DataRuleTablePanel drsp = new DataRuleTablePanel(new TreeNode[0]);
					drsp.ruleSetNumber = (dataRuleSets.getTabCount() + 1);
					dataRuleSets.addTab(("" + (dataRuleSets.getTabCount() + 1)), drsp);
					dataRuleSets.setSelectedComponent(drsp);
				}
			});
			dataRuleSetButtonPanel.add(button);
			button = new JButton("Remove Rule Set");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = dataRuleSets.getSelectedIndex();
					if (index != -1) {
						dataRuleSets.removeTabAt(index);
						for (int rs = 0; rs < dataRuleSets.getTabCount(); rs++) {
							((DataRuleTablePanel) dataRuleSets.getComponentAt(rs)).ruleSetNumber = (rs + 1);
							dataRuleSets.setTitleAt(rs, ("" + (rs+1)));
						}
					}
				}
			});
			dataRuleSetButtonPanel.add(button);
			button = new JButton("Move Up");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = dataRuleSets.getSelectedIndex();
					if (index > 0) {
						DataRuleTablePanel drsp = ((DataRuleTablePanel) dataRuleSets.getComponentAt(index));
						dataRuleSets.removeTabAt(index);
						dataRuleSets.insertTab("", null, drsp, null, (index-1));
						dataRuleSets.setSelectedIndex(index-1);
						for (int rs = 0; rs < dataRuleSets.getTabCount(); rs++) {
							((DataRuleTablePanel) dataRuleSets.getComponentAt(rs)).ruleSetNumber = (rs + 1);
							dataRuleSets.setTitleAt(rs, ("" + (rs+1)));
						}
					}
				}
			});
			dataRuleSetButtonPanel.add(button);
			button = new JButton("Move Down");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = dataRuleSets.getSelectedIndex();
					if ((index+1) < dataRuleSets.getTabCount()) {
						DataRuleTablePanel drsp = ((DataRuleTablePanel) dataRuleSets.getComponentAt(index));
						dataRuleSets.removeTabAt(index);
						dataRuleSets.insertTab("", null, drsp, null, (index+1));
						dataRuleSets.setSelectedIndex(index+1);
						for (int rs = 0; rs < dataRuleSets.getTabCount(); rs++) {
							((DataRuleTablePanel) dataRuleSets.getComponentAt(rs)).ruleSetNumber = (rs + 1);
							dataRuleSets.setTitleAt(rs, ("" + (rs+1)));
						}
					}
				}
			});
			dataRuleSetButtonPanel.add(button);
			
			JPanel dataRuleSetTopPanel = new JPanel(new BorderLayout(), true);
			dataRuleSetTopPanel.add(dataRuleSetButtonPanel, BorderLayout.CENTER);
			dataRuleSetTopPanel.add(cleanupAfterPanel, BorderLayout.EAST);
			
			this.dataRuleSetPanel = new JPanel(new BorderLayout(), true);
			this.dataRuleSetPanel.add(dataRuleSetTopPanel, BorderLayout.NORTH);
			this.dataRuleSetPanel.add(this.dataRuleSets, BorderLayout.CENTER);
			
			
			//	layout the whole stuff
			this.tabs = new JTabbedPane();
			this.tabs.addTab("Base Data", this.baseData);
			this.tabs.addTab("Rank Groups", this.rankGroupPanel);
			this.tabs.addTab("Author Data", this.authorData);
			this.tabs.addTab("Data Rules", this.dataRuleSetPanel);
			
			
			//	create buttons
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			button = new JButton("Validate");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency())
						JOptionPane.showMessageDialog(InstanceEditor.this, ("The OmniFAT instance is valid."), "OmniFAT Instance Valid", JOptionPane.INFORMATION_MESSAGE);
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Base Epithets");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(ALL_FILTER);
						testInstance(instanceName, configNode, OmniFAT.BASE_EPITHET_TYPE, ("Base Epithets in OmniFAT Instance '" + instanceName + "'"));
					}
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Authors");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(new HashSet() {
							public boolean contains(Object o) {
								String s = ((String) o);
								return (!s.startsWith("rank") && !s.startsWith("data"));
							}
						});
						testInstance(instanceName, configNode, OmniFAT.AUTHOR_NAME_TYPE, ("Authors in OmniFAT Instance '" + instanceName + "'"));
					}
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Epithets");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(ALL_FILTER);
						testInstance(instanceName, configNode, OmniFAT.EPITHET_TYPE, ("Epithets in OmniFAT Instance '" + instanceName + "'"));
					}
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Candidates");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(ALL_FILTER);
						testInstance(instanceName, configNode, OmniFAT.TAXON_NAME_CANDIDATE_TYPE, ("Candidates in OmniFAT Instance '" + instanceName + "'"));
					}
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Precision");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(ALL_FILTER);
						testInstance(instanceName, configNode, OmniFAT.TAXONOMIC_NAME_TYPE, ("Precision Rules in OmniFAT Instance '" + instanceName + "'"));
					}
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Test Instance");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(ALL_FILTER);
						testInstance(instanceName, configNode, null, ("OmniFAT Instance '" + instanceName + "'"));
					}
				}
			});
			buttonPanel.add(button);
			
			button = new JButton("Discart Doc");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testTokens = null;
				}
			});
			buttonPanel.add(button);
			
			
			button = new JButton("Print XML");
			button.setPreferredSize(buttonSize);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (checkConsistency()) {
						TreeNode configNode = getConfigNode(ALL_FILTER);
						System.out.println(configNode.treeToCode("  "));
					}
				}
			});
			buttonPanel.add(button);
			
			//	display instance name
			JLabel instanceNameLabel = new JLabel(instanceName, JLabel.LEFT);
			instanceNameLabel.setBorder(BorderFactory.createLineBorder(instanceNameLabel.getBackground(), 5));
			instanceNameLabel.setFont(instanceNameLabel.getFont().deriveFont(Font.BOLD));
			
			//	layout functions
			JPanel topPanel = new JPanel(new BorderLayout(), true);
			topPanel.add(instanceNameLabel, BorderLayout.WEST);
			topPanel.add(buttonPanel, BorderLayout.CENTER);
			
			//	... finally
			this.add(topPanel, BorderLayout.NORTH);
			this.add(this.tabs, BorderLayout.CENTER);
		}
		
		boolean checkConsistency() {
			if (!this.baseData.checkConsistency()) {
				this.tabs.setSelectedComponent(this.baseData);
				return false;
			}
			
			String[] rankGroupNames = new String[this.rankGroups.getTabCount()];
			String[][] rankNames = new String[this.rankGroups.getTabCount()][];
			for (int rg = 0; rg < this.rankGroups.getTabCount(); rg++) {
				RankGroupPanel rgp = ((RankGroupPanel) this.rankGroups.getComponentAt(rg));
				rankGroupNames[rg] = rgp.getRankGroupName();
				rankNames[rg] = rgp.getRankNames();
				if (!rgp.checkConsistency()) {
					this.tabs.setSelectedComponent(this.rankGroupPanel);
					return false;
				}
			}
			
			HashSet uniqueRankGroupNames = new HashSet();
			HashSet uniqueRankNames = new HashSet();
			for (int rg = 0; rg < rankGroupNames.length; rg++) {
				if (!uniqueRankGroupNames.add(rankGroupNames[rg])) {
					JOptionPane.showMessageDialog(this, ("The names of the individual rank groups must be unique, but '" + rankGroupNames[rg] + "' is duplicate."), "Duplicate Rank Group Name", JOptionPane.ERROR_MESSAGE);
					this.tabs.setSelectedComponent(this.rankGroupPanel);
					return false;
				}
				for (int r = 0; r < rankNames[rg].length; r++)
					if (!uniqueRankNames.add(rankNames[rg][r])) {
						JOptionPane.showMessageDialog(this, ("The names of the individual ranks must be unique, but '" + rankNames[rg][r] + "' is duplicate."), "Duplicate Rank Name", JOptionPane.ERROR_MESSAGE);
						this.tabs.setSelectedComponent(this.rankGroupPanel);
						this.tabs.setSelectedIndex(rg);
						return false;
					}
			}
			
			if (!this.authorData.checkConsistency()) {
				this.tabs.setSelectedComponent(this.authorData);
				return false;
			}
			
			for (int drs = 0; drs < this.dataRuleSets.getTabCount(); drs++) {
				DataRuleTablePanel drsp = ((DataRuleTablePanel) this.dataRuleSets.getComponentAt(drs));
				if (!drsp.checkConsistency()) {
					this.tabs.setSelectedComponent(this.dataRuleSetPanel);
					return false;
				}
			}
			
			return true;
		}
		
		TreeNode getConfigNode(HashSet filter) {
			if (!this.checkConsistency())
				return null;
			
			if (filter == null)
				filter = ALL_FILTER;
			
			TreeNode configNode = new TreeNode(null, "omniFat");
			
			TreeNode[] baseDataNodes = this.baseData.getTreeNodes(filter);
			for (int p = 0; p < baseDataNodes.length; p++) {
				configNode.addChildNode(baseDataNodes[p]);
				baseDataNodes[p].setParent(configNode);
			}
			
			for (int rg = 0; rg < this.rankGroups.getTabCount(); rg++) {
				RankGroupPanel rgp = ((RankGroupPanel) this.rankGroups.getComponentAt(rg));
				if (filter.contains("rankGroup." + rgp.getRankGroupName())) {
					TreeNode[] rankGroupNodes = rgp.getTreeNodes(filter);
					for (int p = 0; p < rankGroupNodes.length; p++) {
						configNode.addChildNode(rankGroupNodes[p]);
						rankGroupNodes[p].setParent(configNode);
					}
				}
			}
			
			TreeNode[] authorNodes = this.authorData.getTreeNodes(filter);
			for (int p = 0; p < authorNodes.length; p++) {
				configNode.addChildNode(authorNodes[p]);
				authorNodes[p].setParent(configNode);
			}
			
			TreeNode rulesNode = new TreeNode(configNode, "rules");
			configNode.addChildNode(rulesNode);
			for (int drs = 0; drs < this.dataRuleSets.getTabCount(); drs++) {
				DataRuleTablePanel drsp = ((DataRuleTablePanel) this.dataRuleSets.getComponentAt(drs));
				if (filter.contains("dataRule." + drs)) {
					TreeNode[] ruleSetNodes = drsp.getTreeNodes(filter);
					for (int p = 0; p < ruleSetNodes.length; p++) {
						rulesNode.addChildNode(ruleSetNodes[p]);
						ruleSetNodes[p].setParent(rulesNode);
					}
				}
			}
			rulesNode.setAttribute("cleanupAfter", (this.cleanupAfterDataRules.isSelected() ? "true" : "false"));
			
			return configNode;
		}
		
		private void testInstance(final String instanceName, final TreeNode configNode, final String mode, final String title) {
			final DocumentRoot testDoc = getTestDocument();
			if (testDoc == null) {
				System.out.println(configNode.treeToCode("  "));
				return;
			}
			
			final TestStatusDialog tsd = new TestStatusDialog(title);
			Thread testThread = new Thread() {
				public void run() {
					tsd.popUp();
					doTestInstance(testDoc, instanceName, configNode, mode, title, tsd);
					tsd.dispose();
				}
			};
			testThread.start();
		}
		
		private class TestStatusDialog extends JPanel {
			
			private JLabel label = new JLabel("", JLabel.CENTER);
			private StringVector labelLines = new StringVector();
			
			boolean interrupted = false;
			
			private Thread thread = null;
			private JDialog dialog;
			
			TestStatusDialog(String title) {
				super(new BorderLayout());
				this.dialog = DialogFactory.produceDialog(title, true);
				
				this.add(this.label, BorderLayout.CENTER);
				
				JButton stopButton = new JButton("Stop Test");
				stopButton.setBorder(BorderFactory.createRaisedBevelBorder());
				stopButton.setPreferredSize(buttonSize);
				stopButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (JOptionPane.showConfirmDialog(TestStatusDialog.this, "Do you really want to stop the test?", "Confirm Stop Test", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
							interrupted = true;
					}
				});
				JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				controlPanel.add(stopButton);
				this.add(controlPanel, BorderLayout.SOUTH);
				
				this.dialog.getContentPane().setLayout(new BorderLayout());
				this.dialog.getContentPane().add(this, BorderLayout.CENTER);
				this.dialog.setSize(400, 160);
				this.dialog.setLocationRelativeTo(null);
				this.dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				
				this.dialog.addWindowListener(new WindowAdapter() {
					boolean isInvoked = false;
					public void windowClosing(WindowEvent we) {
						if (this.isInvoked) return; // avoid loops
						this.isInvoked = true;
						if (JOptionPane.showConfirmDialog(TestStatusDialog.this, "Closing this status dialog will disable you to monitor LsdiReferencer", "Confirm Close Status Dialog", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
							dialog.dispose();
						this.isInvoked = false;
					}
				});
			}
			
			void setLabel(String text) {
				System.out.println(text);
				this.labelLines.addElement(text);
				while (this.labelLines.size() > 5)
					this.labelLines.remove(0);
				this.label.setText("<HTML>" + this.labelLines.concatStrings("<BR>") + "</HTML>");
				this.label.validate();
			}
			
			void popUp() {
				if (this.thread != null)
					return;
				this.thread = new Thread() {
					public void run() {
						dialog.setVisible(true);
						thread = null;
					}
				};
				this.thread.start();
				while (!this.isVisible()) try {
					Thread.sleep(50);
				} catch (InterruptedException ie) {}
			}
			
			public JDialog getDialog() {
				return this.dialog;
			}
			
			public void dispose() {
				this.dialog.dispose();
			}
		}
		
		private void doTestInstance(DocumentRoot testDoc, String instanceName, TreeNode configNode, String mode, String title, TestStatusDialog tsd) {
			tsd.setLabel("Getting OmniFAT Instance ...");
			OmniFAT omniFat = this.base.getInstance(instanceName, configNode);
			tsd.setLabel("... done");
			tsd.setLabel("Getting document data set ...");
			OmniFAT.DocumentDataSet testDocData = new OmniFAT.DocumentDataSet(omniFat, testDoc.getAnnotationID());
			tsd.setLabel("... done");
			
			if (OmniFAT.BASE_EPITHET_TYPE.equals(mode)) {
				tsd.setLabel("Tagging base epithets ...");
				OmniFatFunctions.tagBaseEpithets(testDoc, testDocData);
				tsd.setLabel("... done");
				String[] types = {OmniFAT.BASE_EPITHET_TYPE};
				String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
				this.addDetailLabelAttribute(testDoc, types, testDocData, false);
				tsd.setLabel("Displaying results");
				this.displayTestResult(testDoc, types, attributes, title, tsd);
				return;
			}
			
			if (OmniFAT.AUTHOR_NAME_TYPE.equals(mode)) {
				tsd.setLabel("Tagging author names ...");
				OmniFatFunctions.tagAuthorNames(testDoc, testDocData);
				tsd.setLabel("... done");
				String[] types = {OmniFAT.AUTHOR_NAME_TYPE};
				String[] attributes = {OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
				this.addDetailLabelAttribute(testDoc, types, testDocData, false);
				tsd.setLabel("Displaying results");
				this.displayTestResult(testDoc, types, attributes, title, tsd);
				return;
			}
			
			if (OmniFAT.EPITHET_LABEL_TYPE.equals(mode)) {
				tsd.setLabel("Tagging epithet labels ...");
				OmniFatFunctions.tagLabels(testDoc, testDocData);
				tsd.setLabel("... done");
				String[] types = {OmniFAT.EPITHET_LABEL_TYPE, OmniFAT.NEW_EPITHET_LABEL_TYPE};
				String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE};
				this.addDetailLabelAttribute(testDoc, types, testDocData, false);
				tsd.setLabel("Displaying results");
				this.displayTestResult(testDoc, types, attributes, title, tsd);
				return;
			}
			
			tsd.setLabel("Tagging base epithets ...");
			OmniFatFunctions.tagBaseEpithets(testDoc, testDocData);
			tsd.setLabel("... done");
			if (tsd.interrupted)
				return;
			
			tsd.setLabel("Tagging author names ...");
			OmniFatFunctions.tagAuthorNames(testDoc, testDocData);
			tsd.setLabel("... done");
			if (tsd.interrupted)
				return;
			
			tsd.setLabel("Tagging epithets labels ...");
			OmniFatFunctions.tagLabels(testDoc, testDocData);
			tsd.setLabel("... done");
			if (tsd.interrupted)
				return;
			
			tsd.setLabel("Tagging epithets ...");
			OmniFatFunctions.tagEpithets(testDoc, testDocData);
			tsd.setLabel("... done");
			if (OmniFAT.EPITHET_TYPE.equals(mode)) {
				String[] types = {OmniFAT.EPITHET_TYPE};
				String[] attributes = {OmniFAT.STRING_ATTRIBUTE, OmniFAT.AUTHOR_ATTRIBUTE, OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE};
				this.addDetailLabelAttribute(testDoc, types, testDocData, false);
				tsd.setLabel("Displaying results");
				this.displayTestResult(testDoc, types, attributes, title, tsd);
				return;
			}
			else if (tsd.interrupted)
				return;
			
			tsd.setLabel("Tagging candidates ...");
			OmniFatFunctions.tagCandidates(testDoc, testDocData);
			tsd.setLabel("... done");
			if (OmniFAT.TAXON_NAME_CANDIDATE_TYPE.equals(mode)) {
				String[] types = {OmniFAT.TAXON_NAME_CANDIDATE_TYPE};
				String[] attributes = {"StatusString"};
				this.addDetailLabelAttribute(testDoc, types, testDocData, true);
				this.addStatusStringAttribute(testDoc, types, testDocData);
				tsd.setLabel("Displaying results");
				this.displayTestResult(testDoc, types, attributes, title, tsd);
				return;
			}
			else if (tsd.interrupted)
				return;
			
			tsd.setLabel("Applying precision rules ...");
			OmniFatFunctions.applyPrecisionRules(testDoc, testDocData);
			tsd.setLabel("... done");
			if (tsd.interrupted)
				return;
			
			tsd.setLabel("Applying author name rules ...");
			OmniFatFunctions.applyAuthorNameRules(testDoc, testDocData);
			tsd.setLabel("... done");
			if (OmniFAT.TAXONOMIC_NAME_TYPE.equals(mode)) {
				String[] types = {OmniFAT.TAXON_NAME_CANDIDATE_TYPE, OmniFAT.TAXONOMIC_NAME_TYPE, OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE};
				String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, "StatusString"};
				this.addDetailLabelAttribute(testDoc, types, testDocData, true);
				this.addStatusStringAttribute(testDoc, types, testDocData);
				tsd.setLabel("Displaying results");
				this.displayTestResult(testDoc, types, attributes, title, tsd);
				return;
			}
			else if (tsd.interrupted)
				return;
			
			tsd.setLabel("Applying data rules ...");
			OmniFatFunctions.applyDataRules(testDoc, testDocData);
			tsd.setLabel("... done");
			
			String[] types = {OmniFAT.TAXON_NAME_CANDIDATE_TYPE, OmniFAT.TAXONOMIC_NAME_TYPE, OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE};
			String[] attributes = {OmniFAT.RANK_GROUP_ATTRIBUTE, OmniFAT.RANK_ATTRIBUTE, OmniFAT.STATE_ATTRIBUTE, OmniFAT.EVIDENCE_ATTRIBUTE, OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE, "StatusString"};
			this.addDetailLabelAttribute(testDoc, types, testDocData, true);
			this.addStatusStringAttribute(testDoc, types, testDocData);
			tsd.setLabel("Displaying results");
			this.displayTestResult(testDoc, types, attributes, title, tsd);
		}
		
		private final int contextWidth = 10;
		private void addDetailLabelAttribute(QueriableAnnotation doc, String[] annotationTypes, OmniFAT.DocumentDataSet docData, boolean addEpithetDetails) {
			for (int t = 0; t < annotationTypes.length; t++) {
				QueriableAnnotation[] annotations = doc.getAnnotations(annotationTypes[t]);
				for (int a = 0; a < annotations.length; a++) {
					String leftContext = TokenSequenceUtils.concatTokens(doc, Math.max((annotations[a].getStartIndex() - contextWidth), 0), Math.min(annotations[a].getStartIndex(), contextWidth), true, true);
					String rightContext = TokenSequenceUtils.concatTokens(doc, annotations[a].getEndIndex(), Math.min((doc.size() - annotations[a].getEndIndex()), contextWidth), true, true);
					StringBuffer dlb = new StringBuffer("<HTML>" + AnnotationUtils.escapeForXml(leftContext) + "&nbsp;<B>" + annotations[a].getValue() + "</B>&nbsp;" + AnnotationUtils.escapeForXml(rightContext));
					
					if (addEpithetDetails) {
						QueriableAnnotation[] epithets = docData.getEpithets(annotations[a]);
						for (int e = 0; e < epithets.length; e++) {
							String epithetString = ((String) epithets[e].getAttribute(OmniFAT.STRING_ATTRIBUTE));
							if ((epithetString != null) && epithetString.endsWith("."))
								epithetString = epithetString.substring(0, (epithetString.length()-1));
							
							int eaStart = -1;
							int eaEnd = -1;
							String epithetSource = ((String) epithets[e].getAttribute(OmniFAT.SOURCE_ATTRIBUTE, ""));
							Annotation[] epithetAuthors = epithets[e].getAnnotations(OmniFAT.AUTHOR_NAME_TYPE);
							for (int ea = 0; ea < epithetAuthors.length; ea++)
								if (epithetSource.indexOf(epithetAuthors[ea].getAnnotationID()) != -1) {
									if (eaStart == -1)
										eaStart = epithetAuthors[ea].getStartIndex();
									eaEnd = (epithetAuthors[ea].getEndIndex() - 1);
								}
							
							dlb.append("<BR>&nbsp;-&nbsp;");
							for (int v = 0; v < epithets[e].size(); v++) {
								String value = epithets[e].valueAt(v);
								
								if (v == eaStart)
									dlb.append("<I>");
								
								if (value.equals(epithetString)) {
									dlb.append("<B>" + value + "</B>");
									epithetString = null;
								}
								else dlb.append(value);
								
								if (v == eaEnd)
									dlb.append("</I>");
								if (epithets[e].getWhitespaceAfter(v).length() != 0)
									dlb.append("&nbsp;");
							}
							
							String epithetEvidence = ((String) epithets[e].getAttribute(OmniFAT.EVIDENCE_ATTRIBUTE));
							String epithetEvidenceDetail = ((String) epithets[e].getAttribute(OmniFAT.EVIDENCE_DETAIL_ATTRIBUTE));
							if (epithetEvidence != null)
								dlb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[" + epithetEvidence + ", " + epithetEvidenceDetail + "]");
						}
					}
					
					dlb.append("</HTML>");
					annotations[a].setAttribute("__detailLabel", dlb.toString());
				}
			}
		}
		
		private void addStatusStringAttribute(QueriableAnnotation doc, String[] annotationTypes, OmniFAT.DocumentDataSet docData) {
			for (int t = 0; t < annotationTypes.length; t++) {
				QueriableAnnotation[] annotations = doc.getAnnotations(annotationTypes[t]);
				for (int a = 0; a < annotations.length; a++)
					annotations[a].setAttribute("StatusString", OmniFAT.DataRule.getEpithetStatusString(docData.getEpithets(annotations[a])));
			}
		}
		
		private void displayTestResult(QueriableAnnotation testDoc, String[] annotationTypes, String[] attributeNames, String title, TestStatusDialog tsd) {
			HashSet types = new HashSet(Arrays.asList(annotationTypes));
			HashSet attributes = new HashSet(Arrays.asList(attributeNames));
			Annotation[] resultAnnotations = testDoc.getAnnotations();
			final StringRelation resultData = new StringRelation();
			final HashMap resultAnnotationsByID = new HashMap();
			for (int r = 0; r < resultAnnotations.length; r++)
				if (types.contains(resultAnnotations[r].getType())) {
					resultAnnotationsByID.put(resultAnnotations[r].getAnnotationID(), resultAnnotations[r]);
					String[] resultAttributeNames = resultAnnotations[r].getAttributeNames();
					StringTupel resultTupel = new StringTupel();
					resultTupel.setValue("Type", resultAnnotations[r].getType());
					resultTupel.setValue("StartIndex", ("" + resultAnnotations[r].getStartIndex()));
					resultTupel.setValue("Size", ("" + resultAnnotations[r].size()));
					resultTupel.setValue("Value", resultAnnotations[r].getValue());
					resultTupel.setValue("AnnotationID", resultAnnotations[r].getAnnotationID());
					for (int a = 0; a < resultAttributeNames.length; a++)
						if (attributes.contains(resultAttributeNames[a])) {
							if (OmniFAT.VALUE_ATTRIBUTE.equals(resultAttributeNames[a]))
								resultTupel.setValue(resultAttributeNames[a], resultAnnotations[r].getValue());
							else resultTupel.setValue(resultAttributeNames[a], ((String) resultAnnotations[r].getAttribute(resultAttributeNames[a], "")));
						}
					resultData.addElement(resultTupel);
				}
			
			final JDialog resultDialog = new JDialog(tsd.getDialog(), ("Test Result for " + title), true);
			resultDialog.getContentPane().setLayout(new BorderLayout());
			
			final String[] resultAttributes = new String[attributeNames.length + 4];
			resultAttributes[0] = "Type";
			resultAttributes[1] = "StartIndex";
			resultAttributes[2] = "Size";
			resultAttributes[3] = "Value";
			System.arraycopy(attributeNames, 0, resultAttributes, 4, attributeNames.length);
			
			final JTable resultTable = new JTable(new TableModel() {
				public int getColumnCount() {
					return resultAttributes.length;
				}
				public String getColumnName(int columnIndex) {
					return resultAttributes[columnIndex];
				}
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				
				public int getRowCount() {
					return resultData.size();
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					return resultData.get(rowIndex).getValue(this.getColumnName(columnIndex), "");
				}
				
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
				
				public void addTableModelListener(TableModelListener tml) {}
				public void removeTableModelListener(TableModelListener tml) {}
			});
			
			resultTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() < 2)
						return;
					
					int selectedIndex = resultTable.getSelectedRow();
					if (selectedIndex == -1)
						return;
					
					Annotation annotation = ((Annotation) resultAnnotationsByID.get(resultData.get(selectedIndex).getValue("AnnotationID")));
					if (annotation == null)
						return;
					
					String annotationDetailLabel = ((String) annotation.getAttribute("__detailLabel"));
					if (annotationDetailLabel != null)
						JOptionPane.showMessageDialog(resultDialog, annotationDetailLabel, "Details", JOptionPane.PLAIN_MESSAGE);
				}
			});
			resultDialog.getContentPane().add(new JLabel("Double-click individual results to view context, epithets and their details."), BorderLayout.NORTH);
			
			final JTableHeader resultTableHeader = resultTable.getTableHeader();
			resultTableHeader.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
	                int column = resultTableHeader.columnAtPoint(me.getPoint());
	                if (column != -1) {
	                	String sortColumnName = resultTable.getModel().getColumnName(column);
	                	resultData.orderBy(sortColumnName);
	                	resultTable.validate();
	                	resultTable.repaint();
	                }
				}
			});
			
			
			JScrollPane resultTableBox = new JScrollPane(resultTable);
			resultTableBox.getVerticalScrollBar().setUnitIncrement(25);
			resultDialog.getContentPane().add(resultTableBox, BorderLayout.CENTER);
			
			JButton button = new JButton("OK");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					resultDialog.dispose();
				}
			});
			resultDialog.getContentPane().add(button, BorderLayout.SOUTH);
			
			resultDialog.setResizable(true);
			resultDialog.setSize(new Dimension(800, 600));
			resultDialog.setLocationRelativeTo(this);
			resultDialog.setVisible(true);
		}
	}
	
	
	private static final String[] getDictionaryNames(AnalyzerDataProvider adp) {
		String[] dataNames = adp.getDataNames();
		StringVector dictionaryNames = new StringVector();
		for (int d = 0; d < dataNames.length; d++) {
			if (dataNames[d].endsWith(".list.txt") && (dataNames[d].indexOf('/') == -1))
				dictionaryNames.addElementIgnoreDuplicates(dataNames[d]);
			else if (dataNames[d].endsWith(".list.zip") && (dataNames[d].indexOf('/') == -1))
				dictionaryNames.addElementIgnoreDuplicates(dataNames[d].substring(0, (dataNames[d].length() - ".list.zip".length())) + ".list.txt");
		}
		dictionaryNames.sortLexicographically(false, false);
		return dictionaryNames.toStringArray();
	}
	
	private class DictionaryPanel extends ConfigurationDialogPart {
		DictionaryPanel(OmniFAT.Base base, AnalyzerDataProvider adp, JDialog parent) {
			super(base, adp, parent);
		}
		String[] getDataNames() {
			return getDictionaryNames(this.adp);
		}
		void create(String modelDataName) {
			StringVector dictionary;
			String dataName;
			if (modelDataName == null) {
				dictionary = new StringVector();
				dataName = "New Pattern.regEx.txt";
			}
			else {
				dictionary = loadDictionary(modelDataName, true);
				dataName = ("New " + modelDataName);
			}
			if (dictionary == null) return;
			
			DataItemDialog did = new DataItemDialog(this.parent, "Create Dictionary", dataName, true, this.adp);
			
			DictionaryEditor de = new DictionaryEditor(dataName, dictionary, this.getDataNames(), did);
			did.getContentPane().add(de, BorderLayout.CENTER);
			did.setSize(400, 600);
			did.setLocationRelativeTo(this.parent);
			did.setVisible(true);
			
			if (did.isCommitted()) try {
				dataName = did.getDataName();
				storeDictionary(dataName, de.getDictionary());
				this.base.discartCachedDictionary(dataName);
				this.refreshList();
			}
			catch (IOException ioe) {
				System.out.println("Could not store dictionary '" + dataName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		void edit(String dataName) {
			StringVector dictionary = loadDictionary(dataName, true);
			if (dictionary == null) return;
			
			DataItemDialog did = new DataItemDialog(this.parent, ("Edit Dictionary '" + dataName + "'"), dataName, false, adp);
			
			DictionaryEditor de = new DictionaryEditor(dataName, dictionary, this.getDataNames(), did);
			did.getContentPane().add(de, BorderLayout.CENTER);
			did.setSize(400, 600);
			did.setLocationRelativeTo(this.parent);
			did.setVisible(true);
			
			if (did.isCommitted() && de.isDirty()) try {
				storeDictionary(dataName, de.getDictionary());
				this.base.discartCachedDictionary(dataName);
			}
			catch (IOException ioe) {
				System.out.println("Could not store dictionary '" + dataName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		void delete(String dataName) {
			if (this.adp.deleteData(dataName)) {
				this.base.discartCachedDictionary(dataName);
				this.refreshList();
			}
		}
	}
	
	private class DictionaryEditor extends JPanel implements DocumentListener {
		
		private JTextArea editor;
		private JScrollPane editorBox;
		private JDialog parent;
		
		private String dictionaryName;
		private String[] dictionaryNames;
		private StringVector dictionary = new StringVector();
		
		private boolean dirty = false;
		private boolean contentInSync = true;
		
		DictionaryEditor(String dictionaryName, StringVector dictionary, String[] dictionaryNames, JDialog parent) {
			super(new BorderLayout(), true);
			this.dictionaryName = dictionaryName;
			this.dictionary = dictionary;
			this.dictionaryNames = dictionaryNames;
			
			this.parent = parent;
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	initialize buttons
			JButton sortButton = new JButton("Sort");
			sortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			sortButton.setPreferredSize(new Dimension(70, 21));
			sortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					sortList();
				}
			});
			
			JButton unionButton = new JButton("Union");
			unionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			unionButton.setPreferredSize(new Dimension(70, 21));
			unionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					unionList();
				}
			});
			
			JButton intersectButton = new JButton("Intersect");
			intersectButton.setBorder(BorderFactory.createRaisedBevelBorder());
			intersectButton.setPreferredSize(new Dimension(70, 21));
			intersectButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					intersectList();
				}
			});
			
			JButton subtractButton = new JButton("Subtract");
			subtractButton.setBorder(BorderFactory.createRaisedBevelBorder());
			subtractButton.setPreferredSize(new Dimension(70, 21));
			subtractButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					subtractList();
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
			buttonPanel.add(sortButton, gbc.clone());
			gbc.gridx = 1;
			buttonPanel.add(unionButton, gbc.clone());
			gbc.gridx = 2;
			buttonPanel.add(intersectButton, gbc.clone());
			gbc.gridx = 3;
			buttonPanel.add(subtractButton, gbc.clone());
			
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			this.refreshDisplay();
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		StringVector getDictionary() {
			if (!this.contentInSync)
				this.synchronizeContent();
			return this.dictionary;
		}
		
		private synchronized void synchronizeContent() {
			if (this.contentInSync) return;
			
			this.dictionary.clear();
			
			Document doc = this.editor.getDocument();
			StringBuffer entry = new StringBuffer();
			
			for (int c = 0; c < doc.getLength(); c++) try {
				String s = doc.getText(c, 1);
				if ("\n".equals(s)) {
					this.dictionary.addElement(entry.toString().trim());
					entry = new StringBuffer();
				}
				else entry.append(s);
			} catch (BadLocationException ble) {}
			
			if (entry.length() != 0)
				this.dictionary.addElement(entry.toString().trim());
			
			this.contentInSync = true;
		}
		
		private synchronized void refreshDisplay() {
			Document doc = new PlainDocument();
			for (int c = 0; c < this.dictionary.size(); c++) try {
				if (c != 0) doc.insertString(doc.getLength(), "\n", null);
				doc.insertString(doc.getLength(), this.dictionary.get(c), null);
			} catch (BadLocationException ble) {}
			doc.addDocumentListener(this);
			this.editor.setDocument(doc);
			this.contentInSync = true;
		}
		
		private void sortList() {
			int count = this.dictionary.size();
			
			this.synchronizeContent();
			this.dictionary.removeDuplicateElements(true);
			this.dictionary.sortLexicographically(false, true);
			this.refreshDisplay();
			
			this.dirty = (this.dirty || (count != this.dictionary.size()));
		}
		
		private void unionList() {
			StringVector list = this.getDictionary();
			Object listName = JOptionPane.showInputDialog(this, "Select dictionary to union with", "Union", JOptionPane.PLAIN_MESSAGE, null, this.dictionaryNames, this.dictionaryName);
			if (listName == null) return;
			
			StringVector unionList = loadDictionary(listName.toString(), false);
			if (unionList != null) {
				
				StringVector delta = unionList.without(list);
				ShowDeltaDialog sdd = new ShowDeltaDialog("Select Entries to Add", delta);
				sdd.setVisible(true);
				
				StringVector selected = sdd.getSelectedEntries();
				if (selected != null) {
					this.dictionary.addContentIgnoreDuplicates(selected);
					this.sortList();
					this.dirty = true;
				}
			}
		}
		
		private void intersectList() {
			StringVector list = this.getDictionary();
			Object listName = JOptionPane.showInputDialog(this, "Select dictionary to intersect with", "Intersect", JOptionPane.PLAIN_MESSAGE, null, this.dictionaryNames, this.dictionaryName);
			if (listName == null) return;
			
			StringVector intersectList = loadDictionary(listName.toString(), false);
			if (intersectList != null) {
				
				StringVector delta = list.without(intersectList);
				ShowDeltaDialog sdd = new ShowDeltaDialog("Select Entries to Remove", delta);
				sdd.setVisible(true);
				
				StringVector selected = sdd.getSelectedEntries();
				if (selected != null) {
					this.dictionary = this.dictionary.without(selected, true);
					this.sortList();
					this.dirty = true;
				}
			}
		}
		
		private void subtractList() {
			StringVector list = this.getDictionary();
			Object listName = JOptionPane.showInputDialog(this, "Select dictionary to subtract", "Subtract", JOptionPane.PLAIN_MESSAGE, null, this.dictionaryNames, this.dictionaryName);
			if (listName == null) return;
			
			StringVector subtractList = loadDictionary(listName.toString(), false);
			if (subtractList != null) {
				
				StringVector delta = list.intersect(subtractList);
				ShowDeltaDialog sdd = new ShowDeltaDialog("Select Entries to Remove", delta);
				sdd.setVisible(true);
				
				StringVector selected = sdd.getSelectedEntries();
				if (selected != null) {
					this.dictionary = this.dictionary.without(selected, true);
					this.sortList();
					this.dirty = true;
				}
			}
		}
		
		public void changedUpdate(DocumentEvent de) {
			//	attribute changes are not of interest for now
		}
		public void insertUpdate(DocumentEvent de) {
			this.dirty = true;
			this.contentInSync = false;
		}
		public void removeUpdate(DocumentEvent de) {
			this.dirty = true;
			this.contentInSync = false;
		}
		
		private class ShowDeltaDialog extends JDialog {
			
			private StringVector delta;
			private StringVector selected = new StringVector();
			
			private JTable entryList;
			
			ShowDeltaDialog(String title, StringVector delta) {
				super(parent, title, true);
				this.delta = delta;
				
				this.entryList = new JTable(new EntryListModel());
				this.entryList.getColumnModel().getColumn(0).setMaxWidth(50);
				JScrollPane entryListBox = new JScrollPane(this.entryList);
				
				JButton selectAllButton = new JButton("Select All");
				selectAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
				selectAllButton.setPreferredSize(new Dimension(70, 21));
				selectAllButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						selectAll();
					}
				});
				
				JButton deselectAllButton = new JButton("Deselect All");
				deselectAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
				deselectAllButton.setPreferredSize(new Dimension(70, 21));
				deselectAllButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						deselectAll();
					}
				});
				
				JPanel entryListButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				entryListButtonPanel.add(selectAllButton);
				entryListButtonPanel.add(deselectAllButton);
				
				JPanel entryListPanel = new JPanel(new BorderLayout());
				entryListPanel.add(entryListBox, BorderLayout.CENTER);
				entryListPanel.add(entryListButtonPanel, BorderLayout.SOUTH);
				
				JButton okButton = new JButton("OK");
				okButton.setBorder(BorderFactory.createRaisedBevelBorder());
				okButton.setPreferredSize(new Dimension(100, 21));
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						ShowDeltaDialog.this.dispose();
					}
				});
				
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.setPreferredSize(new Dimension(100, 21));
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selected = null;
						ShowDeltaDialog.this.dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.add(okButton);
				buttonPanel.add(cancelButton);
				
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(entryListPanel, BorderLayout.CENTER);
				this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				
				this.setSize(400, 600);
				this.setLocationRelativeTo(DictionaryEditor.this);
			}
			
			private StringVector getSelectedEntries() {
				return this.selected;
			}
			
			private void selectAll() {
				this.selected.addContentIgnoreDuplicates(this.delta);
				this.entryList.repaint();
				this.validate();
			}
			
			private void deselectAll() {
				this.selected.clear();
				this.entryList.repaint();
				this.validate();
			}
			
			private class EntryListModel implements TableModel {
				public int getColumnCount() {
					return 2;
				}
				public int getRowCount() {
					return delta.size();
				}
				public Class getColumnClass(int columnIndex) {
					return ((columnIndex == 0) ? Boolean.class : String.class);
				}
				public String getColumnName(int columnIndex) {
					return ((columnIndex == 0) ? "Select" : "Entry");
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					String entry = delta.get(rowIndex);
					if (columnIndex == 0) return new Boolean(selected.contains(entry));
					else return entry;
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return (columnIndex == 0);
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if (columnIndex == 0) {
						if (((Boolean) newValue).booleanValue())
							 selected.addElementIgnoreDuplicates(delta.get(rowIndex));
						else selected.removeAll(delta.get(rowIndex));
					}
				}
				
				public void addTableModelListener(TableModelListener tml) {}
				public void removeTableModelListener(TableModelListener tml) {}
			}
		}
	}
	
	private StringVector loadDictionary(final String dictionaryName, boolean checkSize) {
		InputStreamReader isr = null;
		try {
			InputStream is = checkSize ?
				new InputStream() {
					private int warningLimitKB = 128;
					private int stopLimitKB = 1024;
					private int read = 0;
					private InputStream is = adp.getInputStream(dictionaryName);
					public void close() throws IOException {
						this.is.close();
					}
					public int read() throws IOException {
						this.read++;
						if (this.read == (this.warningLimitKB * 1024)) {
							int choice = JOptionPane.showConfirmDialog(OmniFatEditor.this, ("Dictionary '" + dictionaryName + "' is very large, over " + this.warningLimitKB + " KB.\nEditing it in this editor might be inconvenient due to performance issues.\nContinue loading dictionary '" + dictionaryName + "'?"), "Dictionary Very Large", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (choice != JOptionPane.YES_OPTION)
								throw new RuntimeException("Dictionary '" + dictionaryName + "' is too large to edit, over " + this.warningLimitKB + " KB.");
						}
						else if (this.read == (this.stopLimitKB * 1024)) {
							JOptionPane.showMessageDialog(OmniFatEditor.this, ("Dictionary '" + dictionaryName + "' is too large, over " + this.stopLimitKB + " MB.\nEditing it in this editor would be close to impossible due to performance issues.\nPlease use an external text editor to edit dictionary '" + dictionaryName + "'"), "Dictionary Too Large", JOptionPane.ERROR_MESSAGE);
							throw new RuntimeException("Dictionary '" + dictionaryName + "' is too large to edit, over " + this.stopLimitKB + " MB.");
						}
						return this.is.read();
					}
				}
				:
				this.adp.getInputStream(dictionaryName);
			isr = new InputStreamReader(is, "UTF-8");
			return StringVector.loadList(isr);
		}
		catch (RuntimeException re) {
			System.out.println("Could not load dictionary '" + dictionaryName + "': " + re.getMessage());
			re.printStackTrace(System.out);
			return null;
		}
		catch (IOException ioe) {
			System.out.println("Could not load dictionary '" + dictionaryName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		finally {
			if (isr != null) try {
				isr.close();
			} catch (IOException ioe) {}
		}
	}
	
	private boolean storeDictionary(String dictionaryName, StringVector dictionary) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(this.adp.getOutputStream(dictionaryName), "UTF-8");
		dictionary.storeContent(osw);
		osw.flush();
		osw.close();
		return true;
	}
	
	
	private static final String[] getPatternNames(AnalyzerDataProvider adp) {
		String[] dataNames = adp.getDataNames();
		StringVector patternNames = new StringVector();
		for (int d = 0; d < dataNames.length; d++) {
			if (dataNames[d].endsWith(".regEx.txt") && (dataNames[d].indexOf('/') == -1))
				patternNames.addElementIgnoreDuplicates(dataNames[d]);
		}
		patternNames.sortLexicographically(false, false);
		return patternNames.toStringArray();
	}
	
	private class PatternPanel extends ConfigurationDialogPart {
		int maxTokens;
		PatternPanel(OmniFAT.Base base, AnalyzerDataProvider adp, JDialog parent, int maxTokens) {
			super(base, adp, parent);
			this.maxTokens = maxTokens;
		}
		String[] getDataNames() {
			return getPatternNames(this.adp);
		}
		void create(String modelDataName) {
			String pattern;
			String dataName;
			if (modelDataName == null) {
				pattern = "";
				dataName = "New Pattern.regEx.txt";
			}
			else {
				pattern = loadPattern(modelDataName, "");
				dataName = ("New " + modelDataName);
			}
			DataItemDialog did = new DataItemDialog(this.parent, "Create Pattern", dataName, true, adp);
			
			PatternEditor pe = new PatternEditor(dataName, pattern, this.maxTokens);
			did.getContentPane().add(pe, BorderLayout.CENTER);
			did.setSize(400, 600);
			did.setLocationRelativeTo(this.parent);
			did.setVisible(true);
			
			if (did.isCommitted()) try {
				dataName = did.getDataName();
				storePattern(dataName, pe.editor.getContent());
				this.base.discartCachedPattern(dataName);
				this.refreshList();
			}
			catch (IOException ioe) {
				System.out.println("Could not store pattern '" + dataName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		void edit(String dataName) {
			DataItemDialog did = new DataItemDialog(this.parent, ("Edit Pattern '" + dataName + "'"), dataName, false, adp);
			
			PatternEditor pe = new PatternEditor(dataName, loadPattern(dataName, ""), this.maxTokens);
			did.getContentPane().add(pe, BorderLayout.CENTER);
			did.setSize(400, 600);
			did.setLocationRelativeTo(this.parent);
			did.setVisible(true);
			
			if (did.isCommitted() && pe.editor.isDirty()) try {
				storePattern(dataName, pe.editor.getContent());
				this.base.discartCachedPattern(dataName);
			}
			catch (IOException ioe) {
				System.out.println("Could not store pattern '" + dataName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		void delete(String dataName) {
			if (this.adp.deleteData(dataName)) {
				this.base.discartCachedPattern(dataName);
				this.refreshList();
			}
		}
	}
	
	private class PatternEditor extends JPanel {
		RegExEditorPanel editor;
		PatternEditor(final String patternName, String pattern, final int maxTokens) {
			JButton[] buttons = new JButton[1];
			buttons[0] = new JButton("Test");
			buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
			buttons[0].setPreferredSize(new Dimension(115, 21));
			buttons[0].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					String rawPattern = editor.getContent();
					testPattern(patternName, rawPattern, true, maxTokens);
				}
			});
			this.editor = new RegExEditorPanel(buttons);
			this.editor.setSubPatternResolver(resolver);
			this.editor.setContent(pattern);
			
			this.setLayout(new BorderLayout());
			this.add(this.editor, BorderLayout.CENTER);
		}
	}
	
	private void testPattern(String patternName, String rawPattern, boolean isRef, int maxTokens) {
		if (rawPattern == null) return;
		
		String validationError = RegExUtils.validateRegEx(rawPattern, this.resolver);
		if (validationError == null) {
			TokenSequence testTokens = getTestDocument();
			if (testTokens != null) {
				String pattern = (isRef ? RegExUtils.preCompile(rawPattern, this.resolver) : rawPattern);
				Annotation[] annotations = Gamta.extractAllMatches(testTokens, pattern, maxTokens);
				AnnotationDisplayDialog add;
				Window top = DialogFactory.getTopWindow();
				if (top instanceof JDialog)
					add = new AnnotationDisplayDialog(((JDialog) top), ("Matches of RegEx '" + patternName + "'"), annotations, true);
				else if (top instanceof JFrame)
					add = new AnnotationDisplayDialog(((JFrame) top), ("Matches of RegEx '" + patternName + "'"), annotations, true);
				else add = new AnnotationDisplayDialog(((JFrame) null), ("Matches of RegEx '" + patternName + "'"), annotations, true);
				add.setLocationRelativeTo(this);
				add.setVisible(true);
			}
		}
		else JOptionPane.showMessageDialog(this, ("The pattern is not valid:\n" + validationError), "Pattern Validation", JOptionPane.ERROR_MESSAGE);
	}
	
	private Properties resolver = new Properties() {
		public String getProperty(String name, String def) {
			return loadPattern(name, def);
		}
		public String getProperty(String name) {
			return this.getProperty(name, null);
		}
	};
	
	private String loadPattern(String patternName, String def) {
		try {
			if (patternName.endsWith(".regEx.txt")) {
				InputStream is = this.adp.getInputStream(patternName);
				StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				return RegExUtils.normalizeRegEx(regEx.concatStrings(""));
			}
			else if (this.adp.isDataAvailable(patternName + ".list.txt")) {
				InputStream is = this.adp.getInputStream(patternName + ".list.txt");
				StringVector load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				String orGroup = RegExUtils.produceOrGroup(load, true);
				return orGroup;
			}
			else {
				InputStream is = this.adp.getInputStream(patternName + ".regEx.txt");
				StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				return RegExUtils.normalizeRegEx(regEx.concatStrings(""));
			}
		}
		catch (IOException ioe) {
			System.out.println("Could not load pattern '" + patternName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return def;
		}
	}
	
	private boolean storePattern(String patternName, String pattern) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(this.adp.getOutputStream(patternName), "UTF-8");
		osw.write(RegExUtils.normalizeRegEx(pattern));
		osw.flush();
		osw.close();
		return true;
	}
	
	
	
	private MutableTokenSequence testTokens = null;
	
	private DocumentRoot getTestDocument() {
		if (this.testTokens == null) {
			TokenSequence testTokens = Gamta.getTestDocument();
			if (testTokens != null) {
				this.testTokens = Gamta.newTokenSequence(testTokens, testTokens.getTokenizer());
				for (int t = 0; t < this.testTokens.size(); t++) {
					if ((t+1) == this.testTokens.size())
						this.testTokens.tokenAt(t).setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					else {
						String whiteSpace = this.testTokens.getWhitespaceAfter(t);
						if ((whiteSpace.indexOf('\n') != -1) || (whiteSpace.indexOf('\r') != -1))
							this.testTokens.tokenAt(t).setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					}
				}
			}
		}
		return ((this.testTokens == null) ? null : Gamta.newDocument(this.testTokens));
	}
	
	private OmniFAT.Base base;
	private AnalyzerDataProvider adp;
	
	public OmniFatEditor(AnalyzerDataProvider adp, JDialog parent) {
		super(new BorderLayout(), true);
		this.base = OmniFAT.getBase(adp);
		this.adp = adp;
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Instances", new InstancePanel(this.base, this.adp, parent));
		tabs.addTab("Dictionaries", new DictionaryPanel(this.base, this.adp, parent));
		tabs.addTab("Patterns", new PatternPanel(this.base, this.adp, parent, 0));
		this.add(tabs, BorderLayout.CENTER);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		Gamta.addTestDocumentProvider(new TestDocumentProvider() {
			private JFileChooser fileChooser;
			private JFileChooser getFileChooser() {
				if (this.fileChooser == null) try {
					this.fileChooser = new JFileChooser();
					this.fileChooser.addChoosableFileFilter(new FileFilter () {
						public boolean accept(File file) {
							String fileName = file.getName().toLowerCase();
							return (file.isDirectory() || fileName.endsWith(".xml") || fileName.endsWith(".html") || fileName.endsWith(".htm"));
						}
						public String getDescription() {
							return "HTML and XML files";
						}
					});
				} catch (Exception e) {}
				return this.fileChooser;
			}
			public QueriableAnnotation getTestDocument() {
				JFileChooser fileChooser = this.getFileChooser();
				if (fileChooser == null)
					return null;
				if (fileChooser.showOpenDialog(DialogFactory.getTopWindow()) != JFileChooser.APPROVE_OPTION)
					return null;
				File docFile = fileChooser.getSelectedFile();
				MutableAnnotation doc = null;
				try {
					// TODO try to guess encoding, and facilitate selecting it if clues insufficient
					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream(docFile), "UTF-8"));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				return doc;
			}
		});
		
		File root = new File("./OmniFATData/");
		System.out.println(root.getAbsolutePath());
		AnalyzerDataProvider adp = new AnalyzerDataProviderFileBased(root);
		OmniFAT.Base base = OmniFAT.getBase(adp);
		
		final JFrame iconFrame = new JFrame("OmniFAT Editor");
		InputStream iis = adp.getInputStream("OmniFAT.bmp");
		iconFrame.setIconImage(ImageIO.read(iis));
		iis.close();
		final JDialog configDialog = new JDialog(iconFrame, "OmniFAT Editor", true);
		
		JComboBox defaultInstance = new JComboBox(base.getInstanceNames());
		defaultInstance.setSelectedItem(base.getDefaultInstanceName());
		JPanel defaultInstancePanel = new JPanel(new BorderLayout());
		defaultInstancePanel.add(new JLabel("Default Instance: "), BorderLayout.WEST);
		defaultInstancePanel.add(defaultInstance, BorderLayout.CENTER);
		configDialog.getContentPane().add(defaultInstancePanel, BorderLayout.NORTH);
		
		OmniFatEditor ofe = new OmniFatEditor(adp, configDialog);
		configDialog.getContentPane().add(ofe, BorderLayout.CENTER);
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				configDialog.dispose();
				iconFrame.dispose();
			}
		});
		configDialog.getContentPane().add(closeButton, BorderLayout.SOUTH);
		configDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				iconFrame.dispose();
			}
		});
		
		iconFrame.setLocationRelativeTo(null);
		iconFrame.setVisible(true);
		configDialog.setSize(500, 600);
		configDialog.setLocationRelativeTo(iconFrame);
		configDialog.setVisible(true);
		
		base.setDefaultInstanceName((String) defaultInstance.getSelectedItem());
		base.shutdown();
		System.exit(0);
	}
}