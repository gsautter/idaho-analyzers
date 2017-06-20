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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TaxonomicNameParserTest extends AbstractConfigurableAnalyzer {

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
//		String[] test = "t".split("\\s++");
//		System.out.println(test.length);
//		for (int t = 0; t < test.length; t++)
//			System.out.println("'" + test[t] + "'");
//		if (true) return;
//		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		DocumentRoot doc;// = SgmlDocumentReader.readDocument(new StringReader(""));
		String docString = "This is a test with the Ggenus (Ssubgenus) sspecies Author subsp. ssubspecies The. van Author var. vvarietys Another-Author shit name.";
//		String taxonName = "Genus (Subgenus) species Author sub-species The. van Author variety Another-Author";
		doc = Gamta.newDocument(Gamta.newTokenSequence(docString, null));
		doc.addAnnotation(TaxonomicNameConstants.TAXONOMIC_NAME_ANNOTATION_TYPE, 6, (doc.size() - 9));
		doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/21330_gg0.xml"));
		System.out.println("Document loaded");
		
		Annotation[] taxonomicNames = doc.getAnnotations(TaxonomicNameConstants.TAXONOMIC_NAME_ANNOTATION_TYPE);
		for (int a = 0; a < taxonomicNames.length; a++) taxonomicNames[a].clearAttributes();
		System.out.println("Document cleared");
		
		TaxonomicNameParserOnline tnpo = new TaxonomicNameParserOnline();
		System.out.println("TNP instantiated");
		tnpo.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/OmniFATData/")));
		System.out.println("TNP initialized");
		
		tnpo.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		System.out.println("TNP finished");
		taxonomicNames = doc.getAnnotations(TaxonomicNameConstants.TAXONOMIC_NAME_ANNOTATION_TYPE);
		for (int t = 0; t < taxonomicNames.length; t++)
			System.out.println(taxonomicNames[t].toXML());
		
		tnpo.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		System.out.println("TNP finished");
		taxonomicNames = doc.getAnnotations(TaxonomicNameConstants.TAXONOMIC_NAME_ANNOTATION_TYPE);
		for (int t = 0; t < taxonomicNames.length; t++)
			System.out.println(taxonomicNames[t].toXML());
		
		if (true) return;
		
		
		
		StringVector parts = TokenSequenceUtils.getTextTokens(doc);
		String partString = parts.concatStrings(" ");
		System.out.println(parts.concatStrings(" "));
		
		String authorRegEx = "[^\\s]+" +
				"(?:" +
				"(?:\\s[A-Z]{1}+[^\\s]+\\b)" +
				"|" +
				"(?:\\s[^\\s]{0,3}\\b)" +
				"|" +
				"(?:\\s[^\\s]+\\b){1}?" +
				")*?";
		
		String genusRegEx = "[A-Z][a-z]{2,}";
		String subGenusRegEx = "[A-Z][a-z]{2,}";
		
		String speciesRegEx = "[a-z\\-]{3,}";
//		String speciesAuthorRegEx = "[^\\s]+(?:\\s[^\\s]+)*?";
		String speciesAuthorRegEx = authorRegEx;
		
		String subSpeciesLabelRegEx = "(?:subsp|subspecies)\\b";
		String subSpeciesRegEx = "[a-z\\-]{3,}";
//		String subSpeciesAuthorRegEx = "[^\\s]+(?:\\s[^\\s]+)*?";
		String subSpeciesAuthorRegEx = authorRegEx;
		
		String varietyLabelRegEx = "(?:var|variety)\\b";
		String varietyRegEx = "[a-z\\-]{3,}";
//		String varietyAuthorRegEx = "[^\\s]+(?:\\s[^\\s]+)*?";
		String varietyAuthorRegEx = authorRegEx;
		
		String regEx = ("\\s*+(" + genusRegEx + ")?" +
				"\\s*+(" + subGenusRegEx + ")?" +
				"\\s*+(" + speciesRegEx + ")?" +
				"\\s*+(" + speciesAuthorRegEx + ")?" +
				"\\s*+(" + subSpeciesLabelRegEx + ")?" +
//				"\\s*+(" + "subsp" + ")" + 
				"\\s*+(" + subSpeciesRegEx + ")?" +
				"\\s*+(" + subSpeciesAuthorRegEx + ")?" +
				"\\s*+(" + varietyLabelRegEx + ")?" +
//				"\\s*+(" + "var" + ")" + 
				"\\s*+(" + varietyRegEx + ")?" +
				"\\s*+(" + varietyAuthorRegEx + ")?");
//		String regEx = ("(" + genusRegEx + ")?" +
//				"\\s*+(" + subGenusRegEx + ")?" +
//				"\\s*+(" + speciesRegEx + ")?" +
//				"\\s*+(" + speciesAuthorRegEx + ")?" +
//				"\\s*+(" + subSpeciesLabelRegEx + ")?" +
////				"\\s*+(" + "subsp" + ")" + 
////				"\\s*+(" + subSpeciesRegEx + ")?" +
//				"\\s*+(" + "sub-species" + ")" +
//				"\\s*+(" + subSpeciesAuthorRegEx + ")?" +
//				"\\s*+(" + varietyLabelRegEx + ")?" +
////				"\\s*+(" + "var" + ")" + 
////				"\\s*+(" + varietyRegEx + ")?" +
//				"\\s*+(" + "variety" + ")" +
//				"\\s*+(" + varietyAuthorRegEx + ")?");
//		Pattern p = Pattern.compile(regEx);
//		Matcher m = p.matcher(partString);
//		if (m.matches()) {
//			for (int g = 1; g <= m.groupCount(); g++) {
//				System.out.println(g + ": " + m.group(g));
//			}
//		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final JPanel tnp = new ScrollablePanel(new GridBagLayout());
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
		
//		doc.addAnnotation(TaxonomicNameConstants.GENUS_ATTRIBUTE, 2, 1);
		doc.setAttribute(TaxonomicNameConstants.GENUS_ATTRIBUTE, "ImTheGenus");
		
//		for (int t = 0; t < 10; t++) {
//			tnp.add(new TnPanel(doc, doc), gbc.clone());
//			gbc.gridy++;
//		}
		tnp.add(new TnPanel(doc, doc), gbc.clone());
		gbc.gridy++;
		gbc.weighty = 1;
		tnp.add(new JPanel(), gbc.clone());
		
		final JScrollPane tnpBox = new JScrollPane(tnp);
//		tnpBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		final JFrame frame = new JFrame("Label line wrap test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				for (int p = 0; p < tnp.getComponentCount(); p++) {
					Component tnpc = tnp.getComponent(p);
					if (tnpc instanceof TnPanel) {
						((TnPanel) tnpc).commitChanges();
						if (((TnPanel) tnpc).taxonName != null)
							System.out.println(((TnPanel) tnpc).taxonName.toXML());
					}
				}
				System.exit(0);
			}
		});
		frame.setSize(300, 300);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		
		frame.getContentPane().setLayout(new BorderLayout());
//		frame.getContentPane().add(getPanel(doc), BorderLayout.NORTH);
//		frame.getContentPane().add(getPanel(doc), BorderLayout.SOUTH);
		frame.getContentPane().add(tnpBox, BorderLayout.CENTER);
		
		frame.setVisible(true);
	}
	
	static class ScrollablePanel extends JPanel implements Scrollable {
		public ScrollablePanel() {
			super();
		}
		public ScrollablePanel(boolean isDoubleBuffered) {
			super(isDoubleBuffered);
		}
		public ScrollablePanel(LayoutManager layout, boolean isDoubleBuffered) {
			super(layout, isDoubleBuffered);
		}
		public ScrollablePanel(LayoutManager layout) {
			super(layout);
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
		 */
		public Dimension getPreferredScrollableViewportSize() {
			return this.getPreferredSize();
		}
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
		 */
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 1;
		}
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
		 */
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
		 */
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
		 */
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 20;
		}
	}
	
	static final String[] taxonNameParts = {
			TaxonomicNameConstants.FAMILY_ATTRIBUTE,
			TaxonomicNameConstants.SUBFAMILY_ATTRIBUTE,
			TaxonomicNameConstants.TRIBE_ATTRIBUTE,
		TaxonomicNameConstants.GENUS_ATTRIBUTE,
			TaxonomicNameConstants.SUBGENUS_ATTRIBUTE,
		TaxonomicNameConstants.SPECIES_ATTRIBUTE,
			TaxonomicNameConstants.SUBSPECIES_ATTRIBUTE,
			TaxonomicNameConstants.VARIETY_ATTRIBUTE,
	};
	
	static final int noMeaningIndex = -2;
	
	static final int cutToIndex = -1;
	static final int splitFromIndex = taxonNameParts.length;
	static final int cutFromIndex = taxonNameParts.length + 1;
	
	static final int genusIndex = 3;
	static final int speciesIndex = 5;
	
	static Font font = new Font("Verdana", Font.PLAIN, 12);
	
	static Color getColor(int meaningIndex) {
		if (meaningIndex == noMeaningIndex)
			return Color.WHITE;
		else if (meaningIndex == cutToIndex)
			return Color.GRAY;
		else if (meaningIndex == splitFromIndex)
			return Color.GREEN;
		else if (meaningIndex == cutFromIndex)
			return Color.GRAY;
		else return new Color(255, ((192 * (meaningIndex+1)) / taxonNameParts.length), 255);
	}
	static Color LIGHT_GREEN = new Color(128, 255, 128);
	
	static class TnPanel extends JPanel {
		
		JPanel meaningfulPartPanel = new JPanel(new GridBagLayout());
		
		MeaningfulPart[] meaningfulParts = new MeaningfulPart[taxonNameParts.length];
		MeaningfulPart[] activeMeaningfulParts = new MeaningfulPart[taxonNameParts.length];
		
		int minMeaningfulPart = taxonNameParts.length;
		int maxMeaningfulPart = 0;
		
		
		JPanel tokenLabelPanel = new JPanel(new BorderLayout());
		TokenLabel[] tokenLabels;
		JLabel[] labels;
		
		MutableAnnotation document;
		QueriableAnnotation taxonName;
		
		TnPanel(final QueriableAnnotation taxonName, MutableAnnotation document) {
			super(new BorderLayout());
			
			this.taxonName = taxonName;
			this.document = document;
			
			final JPanel tnp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			final int[] widths = new int[(taxonName.size() * 2) - 1];
			int height = 0;
			
			ArrayList tokenLabelList = new ArrayList();
			ArrayList labelList = new ArrayList();
			for (int t = 0; t < taxonName.size(); t++) {
				
				//	add space if required
				if (t != 0) {
					if (Gamta.insertSpace(taxonName.valueAt(t-1), taxonName.valueAt(t))) {
						JLabel sl = new JLabel(" ");
						sl.setFont(font);
						sl.setBorder(null);
						sl.setOpaque(true);
						sl.setBackground(Color.WHITE);
						widths[((t-1) * 2) + 1] = sl.getPreferredSize().width;
						tnp.add(sl);
						labelList.add(sl);
					}
					else widths[((t-1) * 2) + 1] = 0;
				}
				
				//	add current text token
				final TokenLabel tl = new TokenLabel(taxonName.valueAt(t));
				final boolean tlIsWord = Gamta.isWord(taxonName.valueAt(t));
				
				//	gather layout information
				widths[t * 2] = tl.getPreferredSize().width;
				height = tl.getPreferredSize().height;
				tnp.add(tl);
				
				//	react to clicks
				tl.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						JPopupMenu pm = new JPopupMenu();
						JMenuItem mi = null;
						
						//	add remove option for cutting points
						if ((tl.meaningIndex == cutToIndex) || (tl.meaningIndex == cutFromIndex)) {
							mi = new JMenuItem("Do Not Cut");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									tl.setMeaningIndex(noMeaningIndex, true);
								}
							});
							pm.add(mi);
						}
						
						//	add cutting point option
						else {
							mi = new JMenuItem("Cut Up To Here");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									tl.setMeaningIndex(cutToIndex, true);
								}
							});
							pm.add(mi);
							
							mi = new JMenuItem("Cut From Here");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									tl.setMeaningIndex(cutFromIndex, true);
								}
							});
							pm.add(mi);
						}
						
						//	add remove option for splitting point
						if (tl.meaningIndex == splitFromIndex) {
							mi = new JMenuItem("Do Not Split");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									tl.setMeaningIndex(noMeaningIndex, true);
								}
							});
							pm.add(mi);
						}
						
						//	add splitting point option
						else {
							mi = new JMenuItem("Split From Here");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									tl.setMeaningIndex(splitFromIndex, true);
								}
							});
							pm.add(mi);
						}
						
						//	add remove option only if meaning assigned
						if ((tl.meaningIndex >= 0) && (tl.meaningIndex < taxonNameParts.length)) {
							mi = new JMenuItem("No Meaning");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									tl.setMeaningIndex(noMeaningIndex, true);
								}
							});
							pm.add(mi);
						}
						
						//	allow assigning meanings only to words
						if (tlIsWord) {
							
							//	add separator if there are previous menu items
							if (mi != null)
								pm.addSeparator();
							
							//	add options for taxon name parts
							for (int p = 0; p < taxonNameParts.length; p++) {
								if (p != tl.meaningIndex) {
									final String taxonNamePart = taxonNameParts[p];
									final int taxonNamePartIndex = p;
									mi = new JMenuItem(taxonNamePart);
									mi.addActionListener(new ActionListener() {
										public void actionPerformed(ActionEvent ae) {
											tl.setMeaningIndex(taxonNamePartIndex, true);
										}
									});
									pm.add(mi);
								}
							}
						}
						
						pm.show(tl, me.getX(), me.getY());
					}
					public void mouseEntered(MouseEvent me) {
						tl.setBackground(Color.BLUE);
					}
					public void mouseExited(MouseEvent me) {
						tl.setBackground(tl.color);
					}
				});
				
				//	store selectable token
				tl.tokenIndex = tokenLabelList.size();
				tokenLabelList.add(tl);

				
				labelList.add(tl);
			}
			
			
			//	get quick access arrays
			this.tokenLabels = ((TokenLabel[]) tokenLabelList.toArray(new TokenLabel[tokenLabelList.size()]));
			this.labels = ((JLabel[]) labelList.toArray(new JLabel[labelList.size()]));
			
			
			//	get token values
			HashSet tokenValues = new HashSet();
			for (int t = 0; t < this.tokenLabels.length; t++)
				tokenValues.add(this.tokenLabels[t].tokenText);
			
			/*
			 * find meaningful parts in tokens - going backward from the last
			 * token ensures that no false assignments are made (eg species
			 * author mistaken for genus) because if any parts are missing or
			 * abbreviated, it's the higher level parts, not the most specific
			 * ones.
			 */
			int lastMeaningfulPartIndex = this.tokenLabels.length;
			for (int p = (taxonNameParts.length - 1); p >= 0; p--) {
				String partValue = ((String) this.taxonName.getAttribute(taxonNameParts[p]));
				
				//	if attribute present, find value in tokens
				if (partValue != null) {
					for (int t = (lastMeaningfulPartIndex - 1); t >= 0; t--) {
						
						//	found matching token
						if (this.tokenLabels[t].tokenText.equals(partValue)) {
							this.tokenLabels[t].setMeaningIndex(p, false);
							tokenValues.remove(this.tokenLabels[t].tokenText);
							lastMeaningfulPartIndex = t;
							t = -1;
						}
						
						//	found possible abbreviation, and part not explicitly present
						else if (partValue.startsWith(this.tokenLabels[t].tokenText) && !tokenValues.contains(partValue)) {
							this.tokenLabels[t].setMeaningIndex(p, false);
							tokenValues.remove(this.tokenLabels[t].tokenText);
							lastMeaningfulPartIndex = t;
							t = -1;
						}
					}
				}
			}
			
			
			//	make labels float
			final int lh = height;
			tnp.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					Dimension size = tnp.getSize();
					int rows = 1;
					int rowLength = 0;
					for (int w = 0; w < widths.length; w++) {
						if ((rowLength + widths[w]) <= size.width)
							rowLength += widths[w];
						else {
							rows++;
							rowLength = widths[w];
						}
					}
					final int height = lh * rows;
					if (size.height != height) {
						tnp.setPreferredSize(new Dimension(size.width, height));
						tnp.updateUI(); // not elegant, but the only way to push the resizing through
					}
				}
			});
			
			
			//	initialize functions
			JButton context = new JButton("View Name Context");
			context.setBorder(BorderFactory.createRaisedBevelBorder());
			context.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					JOptionPane.showMessageDialog(TnPanel.this, "Context Info Coming Soon", ("Context of '" + taxonName.getValue() + "'"), JOptionPane.INFORMATION_MESSAGE);
				}
			});
			context.setPreferredSize(new Dimension(context.getPreferredSize().width, 21));
			
			JCheckBox remove = new JCheckBox("Remove");
			
			JPanel functionPanel = new JPanel(new BorderLayout());
			functionPanel.add(context, BorderLayout.CENTER);
			functionPanel.add(remove, BorderLayout.EAST);
			
			
			//	put token panel in frame
			this.tokenLabelPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			this.tokenLabelPanel.add(tnp, BorderLayout.CENTER);
			
			//	initialize meaningful part panels
			for (int p = 0; p < taxonNameParts.length; p++)
				this.meaningfulParts[p] = new MeaningfulPart(taxonNameParts[p]);
			
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			this.add(functionPanel, BorderLayout.NORTH);
			this.add(this.tokenLabelPanel, BorderLayout.CENTER);
			this.add(this.meaningfulPartPanel, BorderLayout.SOUTH);
			
			this.ensureMeaningfulPartOrder(0);
		}
		
		void ensureMeaningfulPartOrder(int anchorIndex) {
			if (this.tokenLabels[anchorIndex].meaningIndex != noMeaningIndex) {
				int lastMeaningIndex;
				
				lastMeaningIndex = this.tokenLabels[anchorIndex].meaningIndex;
				for (int t = anchorIndex-1; t >= 0; t--)
					if (this.tokenLabels[t].meaningIndex != noMeaningIndex) {
						if (this.tokenLabels[t].meaningIndex < lastMeaningIndex)
							lastMeaningIndex = this.tokenLabels[t].meaningIndex;
						else this.tokenLabels[t].setMeaningIndex(noMeaningIndex, false);
					}
				
				lastMeaningIndex = this.tokenLabels[anchorIndex].meaningIndex;
				for (int t = anchorIndex+1; t < this.tokenLabels.length; t++)
					if (this.tokenLabels[t].meaningIndex != noMeaningIndex) {
						if (this.tokenLabels[t].meaningIndex > lastMeaningIndex)
							lastMeaningIndex = this.tokenLabels[t].meaningIndex;
						else this.tokenLabels[t].setMeaningIndex(noMeaningIndex, false);
					}
			}
			
			this.updateColors();
			this.extractMeaningfulParts();
		}
		
		void updateColors() {
			int cutTo = 0;
			int splitFrom = this.labels.length;
			int cutFrom = this.labels.length;
			for (int l = 0; l < this.labels.length; l++) {
				if (labels[l] instanceof TokenLabel) {
					TokenLabel tl = ((TokenLabel) labels[l]);
					Color tlc = getColor(tl.meaningIndex);
					tl.setBackground(tlc);
					tl.color = tlc;
					if (tl.meaningIndex == cutToIndex)
						cutTo = l;
					else if (tl.meaningIndex == splitFromIndex)
						splitFrom = l;
					else if (tl.meaningIndex == cutFromIndex)
						cutFrom = l;
				}
				else labels[l].setBackground(Color.WHITE);
			}
			
			for (int l = 0; l < cutTo; l++) {
				if (labels[l] instanceof TokenLabel) {
					TokenLabel tl = ((TokenLabel) labels[l]);
					tl.setBackground(Color.LIGHT_GRAY);
					tl.color = Color.LIGHT_GRAY;
				}
				else labels[l].setBackground(Color.LIGHT_GRAY);
			}
			
			for (int l = (splitFrom + 1); l < cutFrom; l++) {
				if (labels[l] instanceof TokenLabel) {
					TokenLabel tl = ((TokenLabel) labels[l]);
					tl.setBackground(LIGHT_GREEN);
					tl.color = LIGHT_GREEN;
				}
				else labels[l].setBackground(LIGHT_GREEN);
			}
			
			for (int l = (cutFrom + 1); l < this.labels.length; l++) {
				if (labels[l] instanceof TokenLabel) {
					TokenLabel tl = ((TokenLabel) labels[l]);
					tl.setBackground(Color.LIGHT_GRAY);
					tl.color = Color.LIGHT_GRAY;
				}
				else labels[l].setBackground(Color.LIGHT_GRAY);
			}
		}
		
		void extractMeaningfulParts() {
			Arrays.fill(this.activeMeaningfulParts, null);
			this.minMeaningfulPart = taxonNameParts.length;
			this.maxMeaningfulPart = 0;
			
			for (int t = 0; t < this.tokenLabels.length; t++)
				if ((this.tokenLabels[t].meaningIndex >= 0) && (this.tokenLabels[t].meaningIndex < taxonNameParts.length)) {
					this.minMeaningfulPart = Math.min(this.minMeaningfulPart, this.tokenLabels[t].meaningIndex);
					this.maxMeaningfulPart = Math.max(this.maxMeaningfulPart, this.tokenLabels[t].meaningIndex);
					this.activeMeaningfulParts[this.tokenLabels[t].meaningIndex] = this.meaningfulParts[this.tokenLabels[t].meaningIndex];
					this.activeMeaningfulParts[this.tokenLabels[t].meaningIndex].setPartString(this.tokenLabels[t].tokenText);
				}
			
			if (this.minMeaningfulPart > genusIndex) {
				this.minMeaningfulPart = genusIndex;
				this.activeMeaningfulParts[genusIndex] = this.meaningfulParts[genusIndex];
				this.activeMeaningfulParts[genusIndex].setPartString(this.taxonName.getAttribute(taxonNameParts[genusIndex], "").toString());
			}
			
			if ((this.maxMeaningfulPart > genusIndex) && (this.activeMeaningfulParts[genusIndex] == null)) {
				this.activeMeaningfulParts[genusIndex] = this.meaningfulParts[genusIndex];
				this.activeMeaningfulParts[genusIndex].setPartString(this.taxonName.getAttribute(taxonNameParts[genusIndex], "").toString());
			}
			
			if ((this.maxMeaningfulPart > speciesIndex) && (this.activeMeaningfulParts[speciesIndex] == null)) {
				this.activeMeaningfulParts[speciesIndex] = this.meaningfulParts[speciesIndex];
				this.activeMeaningfulParts[speciesIndex].setPartString(this.taxonName.getAttribute(taxonNameParts[speciesIndex], "").toString());
			}
			
			this.displayMeaningfulParts();
		}
		
		void displayMeaningfulParts() {
			this.meaningfulPartPanel.removeAll();
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 0;
			gbc.insets.bottom = 0;
			gbc.insets.left = 2;
			gbc.insets.right = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridy = 0;
			
			int startMeaningIndex = Math.min(this.minMeaningfulPart, genusIndex);
			int endMeaningIndex = Math.min(this.maxMeaningfulPart, (taxonNameParts.length - 1));
			
			int height = 0;
			for (int p = startMeaningIndex; p <= endMeaningIndex; p++) {
				
				//	meaning given
				if (this.activeMeaningfulParts[p] != null) {
					gbc.gridx = 0;
					gbc.weightx = 0;
					this.meaningfulPartPanel.add(this.activeMeaningfulParts[p].partLabel, gbc.clone());
					gbc.gridx = 1;
					gbc.weightx = 1;
					this.meaningfulPartPanel.add(this.activeMeaningfulParts[p].partOptions, gbc.clone());
					
					height += Math.max(this.activeMeaningfulParts[p].partLabel.getPreferredSize().height, this.activeMeaningfulParts[p].partOptions.getPreferredSize().height);
					gbc.gridy ++;
				}
			}
			this.meaningfulPartPanel.setPreferredSize(new Dimension(this.meaningfulPartPanel.getWidth(), (height + (this.meaningfulPartPanel.getComponentCount() * 2))));
			this.meaningfulPartPanel.validate();
			this.updateUI();
		}
		
		void commitChanges() {
			
			//	check if cut
			int newStart = 0;
			int newEnd = this.taxonName.size();
			for (int t = 0; t < this.tokenLabels.length; t++) {
				if (this.tokenLabels[t].meaningIndex == cutToIndex)
					newStart = t+1;
				else if (this.tokenLabels[t].meaningIndex == cutFromIndex)
					newEnd = t;
			}
			
			//	do cut if necessary
			if ((newStart != 0) || (newEnd != this.taxonName.size())) {
				
				//	check if complete removal
				if (newStart >= newEnd) {
					this.document.removeAnnotation(this.taxonName);
					this.taxonName = null;
					return;
				}
				
				else {
					QueriableAnnotation newTaxonName = this.document.addAnnotation(this.taxonName.getType(), (this.taxonName.getStartIndex() + newStart), (newEnd - newStart));
					newTaxonName.copyAttributes(this.taxonName);
					this.document.removeAnnotation(this.taxonName);
					this.taxonName = newTaxonName;
				}
			}
			
			String rank = null;
			for (int p = 0; p < taxonNameParts.length; p++) {
				
				//	meaning not given, clear
				if (this.activeMeaningfulParts[p] == null)
					this.taxonName.removeAttribute(taxonNameParts[p]);
				
				//	meaning given
				else { 
					this.taxonName.setAttribute(taxonNameParts[p], this.activeMeaningfulParts[p].getPart());
					rank = taxonNameParts[p];
				}
			}
			this.taxonName.setAttribute(TaxonomicNameConstants.RANK_ATTRIBUTE, rank);
		}
		
		class TokenLabel extends JLabel {
			int tokenIndex;
			String tokenText;
			
			int meaningIndex = noMeaningIndex;
			Color color = Color.WHITE;
			
			TokenLabel(String text) {
				super(text, CENTER);
				this.tokenText = text;
				this.setBorder(null);
				this.setOpaque(true);
				this.setFont(font);
				this.setBackground(this.color);
			}
			
			void setMeaningIndex(int meaningIndex, boolean propagateChange) {
				this.meaningIndex = meaningIndex;
				if (propagateChange)
					ensureMeaningfulPartOrder(this.tokenIndex);
			}
		}
		
		class MeaningfulPart {
			final String meaning;
			JLabel partLabel;
			JComboBox partOptions;
			
			MeaningfulPart(String meaning) {
				this.meaning = meaning;
				
				this.partLabel = new JLabel("<HTML><B>" + meaning.substring(0, 1).toUpperCase() + meaning.substring(1) + ":</B> </HTML>");
				
				this.partOptions = new JComboBox();
				this.partOptions.setEditable(true);
				this.partOptions.setPreferredSize(new Dimension(100, 21));
			}
			
			void setPartString(String part) {
				if (part.length() < 3) {
					for (int o = 0; o < this.partOptions.getItemCount(); o++) {
						Object partOption = this.partOptions.getItemAt(o);
						if (partOption.toString().startsWith(part)) {
							part = partOption.toString();
							o = this.partOptions.getItemCount();
						}
					}
				}
				this.partOptions.setSelectedItem(part);
			}
			
			void setPartOptions(String[] partOptions) {
				Object selectedOption = this.partOptions.getSelectedItem();
				this.partOptions.setModel(new DefaultComboBoxModel(partOptions));
				this.partOptions.setSelectedItem(selectedOption);
			}
			
			String getPart() {
				Object selectedOption = this.partOptions.getSelectedItem();
				return ((selectedOption == null) ? null : selectedOption.toString());
			}
		}
	}
}