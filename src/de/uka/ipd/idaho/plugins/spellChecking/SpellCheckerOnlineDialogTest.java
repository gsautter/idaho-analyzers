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

package de.uka.ipd.idaho.plugins.spellChecking;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.spellChecking.SpellCheckerOnlineDialogTest.SpellCheckingPanel.CorrectionData;

/**
 * @author sautter
 *
 */
public class SpellCheckerOnlineDialogTest {
	
	public static void main(String[] args) throws Exception {
//		
//		if (true) {
//			Locale[] ls = Locale.getAvailableLocales();
//			for (int l = 0; l < ls.length; l++)
//				System.out.println(ls[l].getDisplayLanguage(Locale.ENGLISH) + " (" + ls[l].getDisplayCountry(Locale.ENGLISH) + ")");
//			return;
//		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		DocumentRoot doc;// = SgmlDocumentReader.readDocument(new StringReader(""));
		String docString = "10. Hebert PDN, Penton EH, Burns JM" +
				", Janzen DH, Hallwachs W (2004) Ten " +
				"species in one: DNA barcoding reveals cryptic species in " +
				"the neotropical skipper butterfly Astraptes fulgerator. Proceedings ofthe National " +
				"Academy of Sciences of the United States ofAmerica 101: " +
				"14812-14817.";
//		String taxonName = "Genus (Subgenus) species Author sub-species The. van Author variety Another-Author";
		doc = Gamta.newDocument(Gamta.newTokenSequence(docString, null));
//		doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/21330_gg0.xml"));
//		System.out.println("Document loaded");
		
		final Annotation[] misspellings = new Annotation[3];
		String[][] suggestions = new String[misspellings.length][];
		
		misspellings[0] = Gamta.newAnnotation(doc, "misspelling", 35, 1);
		suggestions[0] = new String[1];
		suggestions[0][0] = "full generator";
		
		misspellings[1] = Gamta.newAnnotation(doc, "misspelling", 38, 1);
		suggestions[1] = new String[3];
		suggestions[1][0] = "of the";
		suggestions[1][1] = "other";
		suggestions[1][2] = "often";
		
		misspellings[2] = Gamta.newAnnotation(doc, "misspelling", 47, 1);
		suggestions[2] = new String[2];
		suggestions[2][0] = "of America";
		suggestions[2][1] = "ofamerica";
		
		SpellCheckingPanel scp = buildSpellCheckingPanel(doc, misspellings, suggestions, null);
		StringWriter sw = new StringWriter();
		scp.writeData(sw);
		System.out.println(sw.toString());
		SpellCheckingPanel scp2 = new SpellCheckingPanel();
		scp2.initFields(new StringReader(sw.toString()));
		
		final SpellCheckingPanel dataScp = scp;
		final SpellCheckingPanel displayScp = scp2;
		
//		final JPanel scc = new ScrollablePanel(new GridBagLayout());
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
//		scc.add(displayScp, gbc.clone());
//		gbc.gridy++;
//		gbc.weighty = 1;
//		scc.add(new JPanel(), gbc.clone());
		
//		final JScrollPane tnpBox = new JScrollPane(scc);
//		tnpBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
//		final JFrame frame = new JFrame("Spell Checker Dialog Test");
//		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		frame.addWindowListener(new WindowAdapter() {
//			public void windowClosed(WindowEvent we) {
//				try {
//					StringWriter sw = new StringWriter();
//					displayScp.writeData(sw);
//					System.out.println(sw.toString());
//					dataScp.initFields(new StringReader(sw.toString()));
//					for (int m = 0; m < misspellings.length; m++) {
//						CorrectionData cd = dataScp.getCorrectionData(misspellings[m].getAnnotationID());
//						System.out.println(misspellings[m].getValue() + " ==> " + cd.correction + " (" + cd.rememberLanguage + ")");
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.exit(0);
//			}
//		});
//		frame.setSize(500, 300);
//		frame.setResizable(true);
//		frame.setLocationRelativeTo(null);
//		
//		frame.getContentPane().setLayout(new BorderLayout());
////		frame.getContentPane().add(getPanel(doc), BorderLayout.NORTH);
////		frame.getContentPane().add(getPanel(doc), BorderLayout.SOUTH);
//		frame.getContentPane().add(tnpBox, BorderLayout.CENTER);
//		
//		frame.setVisible(true);
		
		displayScp.setLabel("<HTML>Please<BR>check<BR>spelling</HTML>");
		String f = displayScp.getFeedback();
		
		sw = new StringWriter();
		displayScp.writeData(sw);
		System.out.println(sw.toString());
		dataScp.initFields(new StringReader(sw.toString()));
		for (int m = 0; m < misspellings.length; m++) {
			CorrectionData cd = dataScp.getCorrectionData(misspellings[m].getAnnotationID());
			System.out.println(misspellings[m].getValue() + " ==> " + cd.correction + " (" + cd.rememberLanguage + ")");
		}
	}
	
	static SpellCheckingPanel buildSpellCheckingPanel(MutableAnnotation paragraph, Annotation[] misspellings, String[][] suggestions, String mainLang) {
		SpellCheckingPanel scp = new SpellCheckingPanel();
		scp.setMainLanguage(mainLang);
		
		int misspellingIndex = 0;
		for (int t = 0; t < paragraph.size(); t++) {
			
			//	forward to next misspelling
			while ((misspellingIndex < misspellings.length) && (misspellings[misspellingIndex].getStartIndex() < t))
				misspellingIndex++;
			
			//	if current token starts a misspelling, facilitate corrections
			if ((misspellingIndex < misspellings.length) && (misspellings[misspellingIndex].getStartIndex() == t)) {
				
				//	assemble misspelling, might be more than one token
				String misspelling = paragraph.valueAt(t);
				while ((t+1) < misspellings[misspellingIndex].getEndIndex()) {
					t++;
					if (Gamta.insertSpace(paragraph.valueAt(t-1), paragraph.valueAt(t)))
						misspelling += " ";
					misspelling += paragraph.valueAt(t);
				}
				scp.addMisspelling(misspellings[misspellingIndex].getAnnotationID(), misspelling, suggestions[misspellingIndex]);
			}
			
			//	regular (correctly spelled) token
			else {
				String token = paragraph.valueAt(t);
				while (((t+1) < ((misspellingIndex < misspellings.length) ? misspellings[misspellingIndex].getStartIndex() : paragraph.size())) && !Gamta.insertSpace(paragraph.valueAt(t), paragraph.valueAt(t+1))) {
					t++;
					token += paragraph.valueAt(t);
				}
				scp.addToken(token);
			}
		}
		
		return scp;
	}
	
	
	static class SpellCheckingPanel extends FeedbackPanel {
		
		static Font font = new Font("Verdana", Font.PLAIN, 12);
		
		static class CorrectionData {
			String correction;
			String rememberLanguage;
			CorrectionData(String correction, String rememberLanguage) {
				this.correction = correction;
				this.rememberLanguage = rememberLanguage;
			}
		}
		
		static final String doNotRemember = "X";
		static final String rememberMainLanguage = "M";
		static final String rememberDomainSpecific = "D";
		
		static final String defaultMainLanguage = "English";
		static final String[] allLanguages = {
			"Albanian",
			"Arabic",
			"Belarusian",
			"Bulgarian",
			"Catalan",
			"Chinese",
			"Croatian",
			"Czech",
			"Danish",
			"Dutch",
			"English",
			"Estonian",
			"Finnish",
			"French",
			"German",
			"Greek",
			"Hebrew",
			"Hindi",
			"Hungarian",
			"Icelandic",
			"Italian",
			"Japanese",
			"Korean",
			"Latvian",
			"Lithuanian",
			"Macedonian",
			"Norwegian",
			"Polish",
			"Portuguese",
			"Romanian",
			"Russian",
			"Slovak",
			"Slovenian",
			"Spanish",
			"Swedish",
			"Thai",
			"Turkish",
			"Ukrainian",
			"Vietnamese",
		};
		
		String mainLanguage; 
		JComboBox mainLanguageSelector = new JComboBox(allLanguages);
		JCheckBox needFullTextEdit = new JCheckBox("");
		
		CorrectionPanel cp = new CorrectionPanel();
		
		ArrayList tokenList = new ArrayList();
		ArrayList misspellingList = new ArrayList();
		HashMap misspellingsById = new HashMap();
		
		JPanel tokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		int lineHeight = 0;
		
		JPanel functionPanel = new JPanel(new BorderLayout());
		JPanel tokenFramePanel = new JPanel(new BorderLayout());
		
		SpellCheckingPanel() {
			super("Check Spelling");
			
			//	initialize language selector components
			this.mainLanguageSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					mainLanguage = mainLanguageSelector.getSelectedItem().toString();
					cp.rememberM.setText("<HTML>Learn as word in <B>main language</B> (" + mainLanguage + ")</HTML>");
					for (int t = 0; t < tokenList.size(); t++) {
						TokenLabel tl = ((TokenLabel) tokenList.get(t));
						if (tl instanceof MisspellingLabel)
							((MisspellingLabel) tl).updateText();
					}
				}
			});
			this.cp.rememberLanguages.setSelectedItem(this.mainLanguage);
			
			//	create token panel and make labels float
			this.tokenPanel.setBackground(Color.WHITE);
			this.tokenPanel.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					Dimension size = tokenPanel.getSize();
					Dimension prefSize = computeTokenPanelSize(size.width);
					if (size.height != prefSize.height) {
						tokenPanel.setPreferredSize(prefSize);
						tokenPanel.setMinimumSize(prefSize);
						tokenPanel.updateUI(); // not elegant, but the only way to push the resizing through
					}
				}
			});
			
			
			//	initialize functions
			JPanel mainLanguagePanel = new JPanel(new BorderLayout());
			mainLanguagePanel.add(new JLabel("This paragraph is (mostly) written in "), BorderLayout.CENTER);
			mainLanguagePanel.add(this.mainLanguageSelector, BorderLayout.EAST);
			
			JPanel freeTextEditPanel = new JPanel(new BorderLayout());
			freeTextEditPanel.add(new JLabel("This paragraph requires full text editing "), BorderLayout.CENTER);
			freeTextEditPanel.add(this.needFullTextEdit, BorderLayout.EAST);
			
			this.functionPanel.add(mainLanguagePanel, BorderLayout.WEST);
			this.functionPanel.add(freeTextEditPanel, BorderLayout.EAST);
			
			//	put token panel in border
			this.tokenFramePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			this.tokenFramePanel.add(this.tokenPanel, BorderLayout.CENTER);
			
			//	put the whole stuff together
			this.add(this.functionPanel, BorderLayout.NORTH);
			this.add(this.tokenFramePanel, BorderLayout.CENTER);
		}
		
		Dimension computeTokenPanelSize(int width) {
			int rows = 1;
			int rowLength = 0;
			for (int t = 0; t < this.tokenList.size(); t++) {
				TokenLabel tl = ((TokenLabel) this.tokenList.get(t));
				int tlWidth = (tl.getPreferredSize().width * ((tl instanceof MisspellingLabel) ? 2 : 1));
				if ((rowLength + tlWidth) <= (width - 6))
					rowLength += tlWidth;
				else {
					rows++;
					rowLength = tlWidth;
				}
			}
			int height = (this.lineHeight * rows) + 6;
			return new Dimension(width, height);
		}
		
		void setMainLanguage(String mainLanguage) {
			this.mainLanguage = ((mainLanguage == null) ? defaultMainLanguage : mainLanguage);
			this.mainLanguageSelector.setSelectedItem(this.mainLanguage);
			this.cp.rememberLanguages.setSelectedItem(this.mainLanguage);
		}
		
		void addToken(String token) {
			TokenLabel tl = new TokenLabel(token);
			
			//	add space if required
			if (this.tokenList.size() != 0) {
				TokenLabel previousToken = ((TokenLabel) this.tokenList.get(this.tokenList.size() - 1));
				if (Gamta.insertSpace(previousToken.tokens.lastValue(), tl.tokens.firstValue()))
					previousToken.addSpace();
			}
			
			//	gather layout information
			this.lineHeight = Math.max(this.lineHeight, tl.getPreferredSize().height);
			this.tokenPanel.add(tl);
			this.tokenList.add(tl);
		}
		
		void addMisspelling(String misspellingId, String misspelling, String[] suggestions) {
			final MisspellingLabel ml = new MisspellingLabel(misspellingId, misspelling, suggestions);
			
			ml.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					cp.showFor(ml);
				}
			});
			
			this.misspellingList.add(ml);
			this.misspellingsById.put(ml.misspellingId, ml);
			
			//	add space if required
			if (this.tokenList.size() != 0) {
				TokenLabel previousToken = ((TokenLabel) this.tokenList.get(this.tokenList.size() - 1));
				if (Gamta.insertSpace(previousToken.tokens.lastValue(), ml.tokens.firstValue()))
					previousToken.addSpace();
			}
			
			//	gather layout information
			this.lineHeight = Math.max(lineHeight, ml.getPreferredSize().height);
			this.tokenPanel.add(ml);
			this.tokenList.add(ml);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return (this.misspellingsById.size() * 5);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return 5; // TODO: adjust this ballpark figure
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.misspellingsById.size();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			super.writeData(bw);
			
			//	write global data
			bw.write(this.mainLanguage);
			bw.newLine();
			bw.write("" + this.needFullTextEdit.isSelected());
			bw.newLine();
			
			//	write tokens
			for (int t = 0; t < this.tokenList.size(); t++) {
				TokenLabel tl = ((TokenLabel) this.tokenList.get(t));
				if (tl instanceof MisspellingLabel) {
					MisspellingLabel ml = ((MisspellingLabel) tl);
					bw.write(ml.misspellingId);
					bw.write(" " + URLEncoder.encode(ml.value, "UTF-8"));
					bw.write(" " + URLEncoder.encode(ml.correction, "UTF-8"));
					bw.write(" " + ml.rememberLanguage);
					StringBuffer suggestions = new StringBuffer();
					for (int s = 0; s < ml.suggestions.length; s++) {
						if (s != 0) suggestions.append(" ");
						suggestions.append(URLEncoder.encode(ml.suggestions[s], "UTF-8"));
					}
					bw.write(" " + URLEncoder.encode(suggestions.toString(), "UTF-8"));
					bw.newLine();
				}
				else {
					bw.write("T " + URLEncoder.encode(tl.value, "UTF-8"));
					bw.newLine();
				}
			}
			bw.newLine();
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			super.initFields(br);
			
			//	read gloabl data
			this.setMainLanguage(br.readLine());
			this.needFullTextEdit.setSelected(Boolean.parseBoolean(br.readLine()));
			
			//	read tokens / token states
			Iterator tokenIterator = this.tokenList.iterator();
			String tokenData;
			while (((tokenData = br.readLine()) != null) && (tokenData.length() != 0)) {
				
				//	regular token
				if (tokenData.startsWith("T ")) {
					
					//	some tokens left, simply leave them alone
					if ((tokenIterator != null) && tokenIterator.hasNext())
						tokenIterator.next();
					
					//	add token
					else {
						tokenIterator = null;
						this.addToken(URLDecoder.decode(tokenData.substring(2), "UTF-8"));
					}
				}
				
				//	misspelling
				else {
					String[] misspellingData = tokenData.split("\\s");
					
					//	some tokens left, simply leave them alone
					if ((tokenIterator != null) && tokenIterator.hasNext()) {
						MisspellingLabel ml = ((MisspellingLabel) tokenIterator.next());
						ml.setCorrection(URLDecoder.decode(misspellingData[2], "UTF-8"));
						ml.setRememberLanguage(misspellingData[3]);
					}
					
					//	add token
					else {
						tokenIterator = null;
						String[] suggestions = URLDecoder.decode(misspellingData[4], "UTF-8").split("\\s");
						for (int s = 0; s < suggestions.length; s++)
							suggestions[s] = URLDecoder.decode(suggestions[s], "UTF-8");
						this.addMisspelling(misspellingData[0], URLDecoder.decode(misspellingData[1], "UTF-8"), suggestions);
					}
				}
			}
		}
		
		CorrectionData getCorrectionData(String misspellingId) {
			MisspellingLabel ml = ((MisspellingLabel) this.misspellingsById.get(misspellingId));
			return ((ml == null) ? null : new CorrectionData(ml.correction, ml.rememberLanguage));
		}
		
		class TokenLabel extends JLabel {
			String value;
			TokenSequence tokens;
			Border spaceBorder = null;
			TokenLabel(String text) {
				super(text, CENTER);
				
				this.value = text;
				this.tokens = Gamta.newTokenSequence(this.value, null);
				
				this.setBorder(null);
				this.setOpaque(true);
				this.setFont(font);
				this.setBackground(Color.WHITE);
				this.setBorder(BorderFactory.createMatteBorder(5, 0, 5, 0, Color.WHITE));
			}
			
			void addSpace() {
				if (this.spaceBorder == null) {
					this.spaceBorder = BorderFactory.createMatteBorder(0, 0, 0, 8, Color.WHITE);
					this.setBorder(this.getBorder());
				}
			}
			
			boolean hasSpace() {
				return (this.spaceBorder != null);
			}
			
			public void setBorder(Border border) {
				if (this.spaceBorder != null)
					border = BorderFactory.createCompoundBorder(this.spaceBorder, border);
				super.setBorder(border);
			}
		}
		
		class MisspellingLabel extends TokenLabel {
			String misspellingId;
			String correction;
			String[] suggestions;
			String rememberLanguage = doNotRemember;
			MisspellingLabel(String misspellingId, String text, String[] suggestions) {
				super(text);
				this.misspellingId = misspellingId;
				
				this.setCorrection(text);
				this.suggestions = suggestions;
				
				this.setBorder(null);
				this.setOpaque(true);
				this.setFont(font);
				this.setBackground(Color.RED);
			}
			
			void setCorrection(String correction) {
				this.correction = correction;
				this.updateText();
			}
			
			void setRememberLanguage(String rl) {
				this.rememberLanguage = rl;
				this.updateText();
			}
			
			void updateText() {
				if (this.value.equals(this.correction))
					this.setText("<HTML><B>" + this.value + "</B>" + this.getRemember() + "</HTML>");
				else this.setText("<HTML><STRIKE>" + this.value + "</STRIKE> <B>" + this.correction + "</B>" + this.getRemember() + "</HTML>");
			}
			
			private String getRemember() {
				if (doNotRemember.equals(this.rememberLanguage))
					return "";
				else if (rememberMainLanguage.equals(this.rememberLanguage))
					return (" <SUP>(" + mainLanguage + ")</SUP>");
				else if (rememberDomainSpecific.equals(this.rememberLanguage))
					return (" <SUP>Dom. Spec.</SUP>");
				else return (" <SUP>" + this.rememberLanguage + "</SUP>");
			}
		}
		
		class CorrectionPanel extends JPanel {
			MisspellingLabel target;
			
			JLabel close = new JLabel("close", JLabel.RIGHT);
			
			JComboBox correction = new JComboBox();
			
			JRadioButton rememberX = new JRadioButton("<HTML><B>Do not learn</B> this word or its correction</HTML>", true);
			JRadioButton rememberM = new JRadioButton("<HTML>Learn as word in <B>main language</B> (English)</HTML>", false);
			JRadioButton rememberD = new JRadioButton("<HTML>Learn as <B>domain specific term</B></HTML>", false);
			JRadioButton rememberL = new JRadioButton("<HTML>Learn as word in</HTML>", false);
			JComboBox rememberLanguages = new JComboBox(allLanguages);
			
			JDialog dialog = null;
			
			CorrectionPanel() {
				super(new GridBagLayout(), true);
				
				this.correction.setEditable(true);
				this.correction.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (correction.getSelectedItem() != null)
							correctionChanged(correction.getSelectedItem().toString());
					}
				});
				final JTextComponent correctionEditor = ((JTextComponent) this.correction.getEditor().getEditorComponent());
				correctionEditor.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent ke) {
						correctionChanged(correctionEditor.getText());
					}
				});
				
				ButtonGroup rememberButtonGroup = new ButtonGroup();
				rememberButtonGroup.add(this.rememberX);
				this.rememberX.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberX.isSelected())
							target.setRememberLanguage(doNotRemember);
					}
				});
				rememberButtonGroup.add(this.rememberM);
				this.rememberM.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberM.isSelected())
							target.setRememberLanguage(rememberMainLanguage);
					}
				});
				rememberButtonGroup.add(this.rememberD);
				this.rememberD.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberD.isSelected())
							target.setRememberLanguage(rememberDomainSpecific);
					}
				});
				rememberButtonGroup.add(this.rememberL);
				this.rememberL.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberL.isSelected())
							target.setRememberLanguage(rememberLanguages.getSelectedItem().toString());
						rememberLanguages.setEnabled(rememberL.isSelected());
					}
				});
				this.rememberLanguages.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (rememberL.isSelected())
							target.setRememberLanguage(rememberLanguages.getSelectedItem().toString());
					}
				});
				
				this.close.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (dialog != null)
							dialog.dispose();
						if (target != null)
							target.setBorder(null);
					}
				});
				
				this.rememberLanguages.setEnabled(false);
				
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridy = 0;
				gbc.weighty = 0;
				gbc.gridheight = 1;
				
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				this.add(this.correction, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				this.add(new JPanel(), gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				this.add(this.close, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 3;
				this.add(this.rememberX, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 3;
				this.add(this.rememberM, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 3;
				this.add(this.rememberD, gbc.clone());
				gbc.gridy++;
				
				gbc.gridx = 0;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				this.add(this.rememberL, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.add(this.rememberLanguages, gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 1;
				this.add(new JPanel(), gbc.clone());
				gbc.gridy++;
				
				this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLineBorder(this.getBackground(), 5)));
			}
			
			void correctionChanged(String correction) {
				this.target.setCorrection(correction);
				boolean noSpace = (correction.indexOf(' ') == -1);
				if (!noSpace)
					this.target.setRememberLanguage(doNotRemember);
				adjustRemember(this.target.rememberLanguage, noSpace);
			}
			
			void adjustRemember(String remember, boolean changeable) {
				if (doNotRemember.equals(remember))
					this.rememberX.setSelected(true);
				else if (rememberMainLanguage.equals(remember))
					this.rememberM.setSelected(true);
				else if (rememberDomainSpecific.equals(remember))
					this.rememberD.setSelected(true);
				else {
					this.rememberLanguages.setSelectedItem(remember);
					this.rememberL.setSelected(true);
				}
				this.rememberX.setEnabled(changeable);
				this.rememberM.setEnabled(changeable);
				this.rememberD.setEnabled(changeable);
				this.rememberL.setEnabled(changeable);
			}
			
			void showFor(MisspellingLabel ml) {
				if (this.target != null)
					this.target.setBorder(null);
				
				this.target = ml;
				this.target.setBackground(Color.GREEN);
				this.target.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
				this.target.validate();
				
				this.correction.removeAllItems();
				this.correction.insertItemAt(this.target.value, 0);
				this.correction.setSelectedItem(this.target.correction);
				for (int s = 0; s < this.target.suggestions.length; s++)
					this.correction.addItem(this.target.suggestions[s]);
				
				this.adjustRemember(this.target.rememberLanguage, (this.target.correction.indexOf(' ') == -1));
				
				if (this.dialog != null)
					this.dialog.dispose();
				this.dialog = DialogFactory.produceDialog("", false);
				this.dialog.setUndecorated(true);
				this.dialog.getContentPane().setLayout(new BorderLayout());
				this.dialog.getContentPane().add(this, BorderLayout.CENTER);
				this.dialog.setSize(this.getPreferredSize());
				
				Point targetPos = this.target.getLocationOnScreen();
				this.dialog.setLocation(targetPos.x, (targetPos.y + this.target.getSize().height + 4));
				this.dialog.setVisible(true);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
//			bw.write("body {");
//			bw.newLine();
//			bw.write("  font-size: 10pt;");
//			bw.newLine();
//			bw.write("  font-family: Verdana, Arial, Helvetica;");
//			bw.newLine();
//			bw.write("}");
//			bw.newLine();
//			bw.write("");
//			bw.newLine();
			bw.write(".mainTable {");
			bw.newLine();
			bw.write("  border: 2pt solid #444444;");
			bw.newLine();
			bw.write("  border-collapse: collapse;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write(".mainTableCell {");
			bw.newLine();
			bw.write("  padding: 5pt;");
			bw.newLine();
			bw.write("  margin: 0pt;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".misspelling {");
			bw.newLine();
			bw.write("  white-space: nowrap;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".misspellingDisplay{}");
			bw.newLine();
			bw.write(".correctionDisplay{");
			bw.newLine();
			bw.write("  font-weight: bold;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write(".rememberDisplay{");
			bw.newLine();
			bw.write("  vertical-align: super;");
			bw.newLine();
			bw.write("  font-size: 7pt;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".rememberLabel {");
			bw.newLine();
			bw.write("  white-space: nowrap;");
			bw.newLine();
			bw.write("  font-size: 8pt;");
			bw.newLine();
			bw.write("  font-family: Verdana, Arial, Helvetica;");
			bw.newLine();
			bw.write("  text-align: left;");
			bw.newLine();
			bw.write("  vertical-align: middle;");
			bw.newLine();
			bw.write("  padding: 0px;");
			bw.newLine();
			bw.write("  margin: 0px;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write(".closeLink {");
			bw.newLine();
			bw.write("  font-size: 8pt;");
			bw.newLine();
			bw.write("  font-family: Verdana, Arial, Helvetica;");
			bw.newLine();
			bw.write("  color: 000;");
			bw.newLine();
			bw.write("  text-decoration: none;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			for (int misspellingHtmlId = 0; misspellingHtmlId < this.misspellingList.size(); misspellingHtmlId++) {
				MisspellingLabel ml = ((MisspellingLabel) this.misspellingList.get(misspellingHtmlId));
				bw.write("  _originalValues['" + misspellingHtmlId + "'] = '" + ml.value + "';");
				bw.newLine();
			}
			bw.write("  ");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.style.width = (_selectionWidth + 'px');");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var i = 0;");
			bw.newLine();
			bw.write("  var misspelling;");
			bw.newLine();
			bw.write("  while (misspelling = document.getElementById('misspellingDisplay' + i)) {");
			bw.newLine();
			bw.write("    misspelling.style.backgroundColor = _notCorrectedColor;");
			bw.newLine();
			bw.write("    misspelling.style.fontWeight = 'bold';");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    var correction = document.getElementById('correctionDisplay' + i);");
			bw.newLine();
			bw.write("    correction.style.backgroundColor = _correctedColor;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    var remember = document.getElementById('rememberDisplay' + i);");
			bw.newLine();
			bw.write("    remember.style.backgroundColor = _rememberColor;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    i++;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  updateMainLanguage();");
			bw.newLine();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			bw.write("var _selectionWidth = 180;");
			bw.newLine();
			bw.write("var _correctedColor = '88FF88';");
			bw.newLine();
			bw.write("var _notCorrectedColor = 'FF8888';");
			bw.newLine();
			bw.write("var _rememberColor = 'FFBB88';");
			bw.newLine();
			
			bw.write("var _originalValues = new Object();");
			bw.newLine();
			
			
			bw.write("var _mouseOverCorrection = false;");
			bw.newLine();
			bw.write("function mouseOverCorrection() {");
			bw.newLine();
			bw.write("  _mouseOverCorrection = true;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function mouseOutCorrection() {");
			bw.newLine();
			bw.write("  _mouseOverCorrection = false;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("var _editing = -1;");
			bw.newLine();
			bw.write("function editMisspelling(i) {");
			bw.newLine();
			bw.write("  if ((i == -1) && _mouseOverCorrection) return; // click in correction menu");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var correction = document.getElementById('correction');");
			bw.newLine();
			bw.write("  correction.style.display = 'none';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (i == -1) return;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var oldMisspelling = document.getElementById('misspelling' + _editing);");
			bw.newLine();
			bw.write("  if (oldMisspelling)");
			bw.newLine();
			bw.write("    oldMisspelling .style.border = '';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  _editing = i;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  //  update correction box");
			bw.newLine();
			bw.write("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  while(selBox.options.length > 2)");
			bw.newLine();
			bw.write("    selBox.options[selBox.options.length - 1] = null;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var optionHolder;");
			bw.newLine();
			bw.write("  var o = 0;");
			bw.newLine();
			bw.write("  var unSelected = true;");
			bw.newLine();
			bw.write("  while (optionHolder = document.getElementById('option' + _editing + '_' + o++)) {");
			bw.newLine();
			bw.write("    selBox.options[selBox.options.length] = new Option(optionHolder.value, optionHolder.value, false, false);");
			bw.newLine();
			bw.write("    if (correctionStore.value == optionHolder.value) {");
			bw.newLine();
			bw.write("      selBox.options[selBox.options.length - 1].selected = true;");
			bw.newLine();
			bw.write("      unSelected = false;");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var originalText = document.getElementById('originalText');");
			bw.newLine();
			bw.write("  originalText.value = _originalValues[_editing];");
			bw.newLine();
			bw.write("  originalText.text = ('<keep \\'' + _originalValues[_editing] + '\\'>');");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var freeText = document.getElementById('freeText');");
			bw.newLine();
			bw.write("  freeText.text = correctionStore.value;");
			bw.newLine();
			bw.write("  freeText.value = correctionStore.value;");
			bw.newLine();
			bw.write("  freeText.selected = unSelected;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.value = document.getElementById('correction' + _editing).value;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  // update remember data");
			bw.newLine();
			bw.write("  updateRemember();");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  // highlight current correction");
			bw.newLine();
			bw.write("  var misspelling = document.getElementById('misspelling' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var pos = getMenuPosition(misspelling);");
			bw.newLine();
			bw.write("  var pageWidth = document.body.clientWidth;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  correction.style.left = pos.x + 'px';");
			bw.newLine();
			bw.write("  correction.style.top = (pos.y + 2) + 'px';");
			bw.newLine();
			bw.write("  correction.style.display = 'block';");
			bw.newLine();
			bw.write("  if ((pos.x + correction.offsetWidth) > pageWidth)");
			bw.newLine();
			bw.write("    correction.style.left = (pageWidth - correction.offsetWidth) + 'px';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  misspelling.style.border = '2pt solid #FF0000';");
			bw.newLine();
			bw.write("  document.getElementById('misspellingDisplay' + _editing).style.backgroundColor = _correctedColor;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function getMenuPosition(el) {");
			bw.newLine();
			bw.write("  var element = el;");
			bw.newLine();
			bw.write("  var left = 0;");
			bw.newLine();
			bw.write("  var top = el.offsetHeight;");
			bw.newLine();
			bw.write("  while(element) {");
			bw.newLine();
			bw.write("    left += element.offsetLeft;");
			bw.newLine();
			bw.write("    top += element.offsetTop;");
			bw.newLine();
			bw.write("    element = element.offsetParent;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var pos = new Object();");
			bw.newLine();
			bw.write("  pos.x = left;");
			bw.newLine();
			bw.write("  pos.y = top;");
			bw.newLine();
			bw.write("  return pos;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function updateCorrection() {");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.newLine();
			bw.write("  correctionStore.value = textBox.value;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var misspelling = document.getElementById('misspellingDisplay' + _editing);");
			bw.newLine();
			bw.write("  var correction = document.getElementById('correctionDisplay' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (correctionStore.value == _originalValues[_editing]) {");
			bw.newLine();
			bw.write("    misspelling.style.textDecoration = '';");
			bw.newLine();
			bw.write("    misspelling.style.fontWeight = 'bold';");
			bw.newLine();
			bw.write("    correction.innerHTML = '';");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  else {");
			bw.newLine();
			bw.write("    misspelling.style.textDecoration = 'line-through';");
			bw.newLine();
			bw.write("    misspelling.style.fontWeight = '';");
			bw.newLine();
			bw.write("    correction.innerHTML = ('&nbsp;' + correctionStore.value);");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function closeCorrection() {");
			bw.newLine();
			bw.write("  correction.style.display = 'none';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var oldMisspelling = document.getElementById('misspelling' + _editing);");
			bw.newLine();
			bw.write("  if (oldMisspelling)");
			bw.newLine();
			bw.write("    oldMisspelling .style.border = '';");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  _editing = -1;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  return false;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("");
			bw.newLine();
			bw.write("");
			bw.newLine();
			
			
			bw.write("function freeTextChanged() {");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  var freeText = document.getElementById('freeText');");
			bw.newLine();
			bw.write("  freeText.value = textBox.value;");
			bw.newLine();
			bw.write("  freeText.text = textBox.value;");
			bw.newLine();
			bw.write("  freeText.selected = true;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  updateCorrection();");
			bw.newLine();
			bw.write("  updateRemember();");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function selectionChanged() {");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  if (selBox.selectedIndex == 0) {");
			bw.newLine();
			bw.write("    var originalText = document.getElementById('originalText');");
			bw.newLine();
			bw.write("    var freeText = document.getElementById('freeText');");
			bw.newLine();
			bw.write("    freeText.value = originalText.value;");
			bw.newLine();
			bw.write("    freeText.text = originalText.value;");
			bw.newLine();
			bw.write("    freeText.selected = true;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.value = selBox.options[selBox.selectedIndex].value;");
			bw.newLine();
			bw.write("  hideSelection();");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  updateCorrection();");
			bw.newLine();
			bw.write("  updateRemember();");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			
			bw.write("var _selectionOpen = false;");
			bw.newLine();
			bw.write("function showSelection() {");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  selBox.style.width = ((_selectionWidth + 20) + 'px');");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.style.display = 'none';");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function clickSelection() {");
			bw.newLine();
			bw.write("  _selectionOpen = true;");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function exitSelection() {");
			bw.newLine();
			bw.write("  if (_selectionOpen)");
			bw.newLine();
			bw.write("    _selectionOpen = false;");
			bw.newLine();
			bw.write("  else hideSelection();");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			bw.write("function hideSelection() {");
			bw.newLine();
			bw.write("  _selectionOpen = false;");
			bw.newLine();
			bw.write("  var selBox = document.getElementById('textSelect');");
			bw.newLine();
			bw.write("  selBox.style.width = '20px';");
			bw.newLine();
			bw.write("  var textBox = document.getElementById('textInput');");
			bw.newLine();
			bw.write("  textBox.style.display = '';");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			
			bw.write("function updateMainLanguage() {");
			bw.newLine();
			bw.write("  var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.newLine();
			bw.write("  var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].text;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var mainLanguageDisplay = document.getElementById('mainLanguageDisplay');");
			bw.newLine();
			bw.write("  mainLanguageDisplay.innerHTML = mainLanguage;");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  var i = -1;");
			bw.newLine();
			bw.write("  var rememberDisplay;");
			bw.newLine();
			bw.write("  while(rememberDisplay = document.getElementById('rememberDisplay' + ++i)) {");
			bw.newLine();
			bw.write("    var rememberStore = document.getElementById('remember' + i);");
			bw.newLine();
			bw.write("    if (rememberStore.value == 'M')");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			
			bw.write("var _xmdl = 'XMDL';");
			bw.newLine();
			bw.write("function updateRemember() {");
			bw.newLine();
			bw.write("  var remember = document.getElementById('remember');");
			bw.newLine();
			bw.write("  var correctionStore = document.getElementById('correction' + _editing);");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (correctionStore.value.indexOf(' ') == -1) {");
			bw.newLine();
			bw.write("    var rememberStore = document.getElementById('remember' + _editing);");
			bw.newLine();
			bw.write("    var rs = rememberStore.value;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    for (var r = 0; r < _xmdl.length; r++) {");
			bw.newLine();
			bw.write("      var idSuffix = _xmdl.substring(r, (r+1));");
			bw.newLine();
			bw.write("      var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.newLine();
			bw.write("      rememberButton.checked = (idSuffix == rs);");
			bw.newLine();
			bw.write("      rememberButton.disabled = false;");
			bw.newLine();
			bw.write("      var rememberLabel = document.getElementById('rememberLabel' + idSuffix);");
			bw.newLine();
			bw.write("      rememberLabel.style.color = '000000';");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    if (rs.length == 1)");
			bw.newLine();
			bw.write("      rememberChanged(rs);");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    else {");
			bw.newLine();
			bw.write("      document.getElementById('rememberL').checked = true;");
			bw.newLine();
			bw.write("      var rememberLanguage = document.getElementById('rememberLanguage');");
			bw.newLine();
			bw.write("      for (var r = 0; r < rememberLanguage.options.length; r++) {");
			bw.newLine();
			bw.write("        if (rs == rememberLanguage.options[r].value)");
			bw.newLine();
			bw.write("          rememberLanguage.options[r].selected = true;");
			bw.newLine();
			bw.write("      }");
			bw.newLine();
			bw.write("      rememberChanged('L');");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  else {");
			bw.newLine();
			bw.write("    for (var r = 0; r < _xmdl.length; r++) {");
			bw.newLine();
			bw.write("      var idSuffix = _xmdl.substring(r, (r+1));");
			bw.newLine();
			bw.write("      var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.newLine();
			bw.write("      rememberButton.checked = (idSuffix == 'X');");
			bw.newLine();
			bw.write("      rememberButton.disabled = true;");
			bw.newLine();
			bw.write("      var rememberLabel = document.getElementById('rememberLabel' + idSuffix);");
			bw.newLine();
			bw.write("      rememberLabel.style.color = '666666';");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("    rememberChanged('X');");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
			
			bw.write("function rememberChanged(rs) {");
			bw.newLine();
			bw.write("  var rememberStore = document.getElementById('remember' + _editing);");
			bw.newLine();
			bw.write("  var rememberDisplay = document.getElementById('rememberDisplay' + _editing);");
			bw.newLine();
			bw.write("  var rememberLanguage = document.getElementById('rememberLanguage');");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  for (var r = 0; r < _xmdl.length; r++) {");
			bw.newLine();
			bw.write("    var idSuffix = _xmdl.substring(r, (r+1));");
			bw.newLine();
			bw.write("    var rememberButton = document.getElementById('remember' + idSuffix);");
			bw.newLine();
			bw.write("    rememberButton.checked = (idSuffix == rs);");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  ");
			bw.newLine();
			bw.write("  if (rs == 'L') {");
			bw.newLine();
			bw.write("    var language = rememberLanguage.options[rememberLanguage.selectedIndex].value;");
			bw.newLine();
			bw.write("    rememberStore.value = language;");
			bw.newLine();
			bw.write("    rememberDisplay.innerHTML = ('&nbsp;' + language);");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    rememberLanguage.disabled = false;");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("  else {");
			bw.newLine();
			bw.write("    rememberStore.value = rs;");
			bw.newLine();
			bw.write("    rememberLanguage.disabled = true;");
			bw.newLine();
			bw.write("    ");
			bw.newLine();
			bw.write("    if (rs == 'X')");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = '';");
			bw.newLine();
			bw.write("    else if (rs == 'D')");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = '&nbsp;Dom. Spec.';");
			bw.newLine();
			bw.write("    else if (rs == 'M') {");
			bw.newLine();
			bw.write("      var mainLanguageSelector = document.getElementById('mainLanguage');");
			bw.newLine();
			bw.write("      var mainLanguage = mainLanguageSelector.options[mainLanguageSelector.selectedIndex].value;");
			bw.newLine();
			bw.write("      rememberDisplay.innerHTML = ('&nbsp;(' + mainLanguage + ')');");
			bw.newLine();
			bw.write("    }");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
			bw.write("}");
			bw.newLine();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			bw.write("<div id=\"correction\" style=\"border: 1px solid #888888; background-color: bbb; display: none; position: absolute; align: right;\" onmouseover=\"mouseOverCorrection();\" onmouseout=\"mouseOutCorrection();\">");
			bw.newLine();
			bw.write("<table style=\"border: 0;\">");
			bw.newLine();
			
			bw.write("<tr>");
			bw.newLine();
			
			bw.write("<td style=\"white-space: nowrap; text-align: left; padding: 0px 0px 8px; margin: 0px;\">");
			bw.write("<input id=\"textInput\" name=\"textInput\" onkeyup=\"freeTextChanged();\" type=\"text\" value=\"\" style=\"margin: 0px; padding 0px;\">");
			bw.write("<select id=\"textSelect\" name=\"textSelect\" onmouseover=\"showSelection();\" onclick=\"clickSelection();\" onmouseout=\"exitSelection();\" onblur=\"hideSelection();\" onchange=\"selectionChanged();\" style=\"margin: 0px; padding: 0px; width: 20px;\">");
			bw.newLine();
			bw.write("  <option value=\"\" id=\"originalText\"></option>");
			bw.newLine();
			bw.write("  <option value=\"\" id=\"freeText\"></option>");
			bw.newLine();
			bw.write("</select>");
			bw.newLine();
			bw.write("</td>");
			bw.newLine();
			
			bw.write("<td style=\"text-align: right; vertical-align: top; padding: 0px 1px 1px; margin: 0px;\">");
			bw.write("<a class=\"closeLink\" href=\"#\" onclick=\"return closeCorrection();\">close&nbsp;[X]</a>");
			bw.write("</td>");
			bw.newLine();
			
			bw.write("</tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberX\" onclick=\"rememberChanged('X');\" value=\"X\"> <span id=\"rememberLabelX\" onclick=\"rememberChanged('X');\"><b>Do not learn</b> this word or its correction</span>");
			bw.newLine();
			bw.write("</td></tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberM\" onclick=\"rememberChanged('M');\" value=\"M\"> <span id=\"rememberLabelM\" onclick=\"rememberChanged('M');\">Learn as word in <b>main language</b> (<span id=\"mainLanguageDisplay\">English</span>)</span>");
			bw.newLine();
			bw.write("</td></tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberD\" onclick=\"rememberChanged('D');\" value=\"D\"> <span id=\"rememberLabelD\" onclick=\"rememberChanged('D');\">Learn as <b>domain specific</b> term</span>");
			bw.newLine();
			bw.write("</td></tr>");
			bw.newLine();
			
			bw.write("<tr><td colspan=\"2\" class=\"rememberLabel\">");
			bw.newLine();
			bw.write("<input type=\"radio\" name=\"remember\" id=\"rememberL\" onclick=\"rememberChanged('L');\" value=\"L\"> <span id=\"rememberLabelL\" onclick=\"rememberChanged('L');\">Learn as <select name=\"rememberLanguage\" id=\"rememberLanguage\" onchange=\"rememberChanged('L');\">");
			bw.newLine();
			for (int l = 0; l < allLanguages.length; l++) {
				bw.write("  <option value=\"" + allLanguages[l] + "\"" + (allLanguages[l].equals(this.mainLanguage) ? " selected" : "") + ">" + allLanguages[l] + "</option>");
				bw.newLine();
			}
			bw.write("</select> word</span></td></tr>");
			bw.newLine();
			bw.write("</table>");
			bw.newLine();
			bw.write("</div>");
			bw.newLine();
			
			
			bw.write("<table class=\"mainTable\">");
			bw.newLine();
			bw.write("  <tr style=\"background-color: bbb;\">");
			bw.newLine();
			bw.write("  <td style=\"font-weight: bold; font-size: 10pt;\" class=\"mainTableCell\">This paragraph is (mostly) written in " );
			bw.write("<select id=\"mainLanguage\" name=\"mainLanguage\" onchange=\"updateMainLanguage();\">");
			bw.newLine();
			for (int l = 0; l < allLanguages.length; l++) {
				bw.write("  <option value=\"" + allLanguages[l] + "\"" + (allLanguages[l].equals(this.mainLanguage) ? " selected" : "") + ">" + allLanguages[l] + "</option>");
				bw.newLine();
			}
			bw.write("  </select></td>");
			bw.newLine();
			bw.write("  <td style=\"font-weight: bold; font-size: 10pt; text-align: right;\" class=\"mainTableCell\">This paragraph requires full text editing <input type=\"checkbox\" name=\"fullTextEdit\" value=\"T\"></td>");
			bw.newLine();
			bw.write("  </tr>");
			bw.newLine();
			bw.write("  <tr>");
			bw.newLine();
			bw.write("    <td class=\"mainTableCell\" colspan=\"2\" style=\"line-height: 1.8;\">");
			
			int misspellingHtmlId = 0;
			for (Iterator tit = this.tokenList.iterator(); tit.hasNext();) {
				TokenLabel tl = ((TokenLabel) tit.next());
				
				if (tl instanceof MisspellingLabel) {
					bw.write("<span class=\"misspelling\">");
						bw.write("<span id=\"misspelling" + misspellingHtmlId + "\" onclick=\"editMisspelling(" + misspellingHtmlId + ");\">");
							bw.write("<span id=\"misspellingDisplay" + misspellingHtmlId + "\" class=\"misspellingDisplay\">" + tl.value + "</span>");
							bw.write("<span id=\"correctionDisplay" + misspellingHtmlId + "\" class=\"correctionDisplay\"></span>");
						bw.write("</span>");
						bw.write("<span id=\"rememberDisplay" + misspellingHtmlId + "\" class=\"rememberDisplay\"></span>");
					bw.write("</span>");
					misspellingHtmlId++;
				}
				
				else bw.write(tl.value);
				
				if (tl.hasSpace()) bw.write(" ");
			}
			bw.write("    </td>");
			bw.newLine();
			bw.write("  </tr>");
			bw.newLine();
			bw.write("</table>");
			bw.newLine();
			
			for (misspellingHtmlId = 0; misspellingHtmlId < this.misspellingList.size(); misspellingHtmlId++) {
				MisspellingLabel ml = ((MisspellingLabel) this.misspellingList.get(misspellingHtmlId));
				bw.write("<input type=\"hidden\" id=\"remember" + misspellingHtmlId + "\" name=\"" + ml.misspellingId + "_remember\" value=\"" + ml.rememberLanguage + "\">");
				bw.newLine();
				bw.write("<input type=\"hidden\" id=\"correction" + misspellingHtmlId + "\" name=\"" + ml.misspellingId + "_correction\" value=\"" + ml.correction + "\">");
				bw.newLine();
				for (int s = 0; s < ml.suggestions.length; s++) {
					bw.write("<input type=\"hidden\" id=\"option" + misspellingHtmlId + "_" + s + "\" value=\"" + ml.suggestions[s] + "\">");
					bw.newLine();
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			this.mainLanguageSelector.setSelectedItem(response.getProperty("mainLanguage", this.mainLanguage));
			this.needFullTextEdit.setSelected("T".equals(response.getProperty("fullTextEdit", "F")));
			
			for (Iterator mit = this.misspellingsById.values().iterator(); mit.hasNext();) {
				MisspellingLabel ml = ((MisspellingLabel) mit.next());
				ml.setCorrection(response.getProperty((ml.misspellingId + "_correction"), ml.correction));
				ml.setRememberLanguage(response.getProperty((ml.misspellingId + "_remember"), ml.rememberLanguage));
			}
		}
	}
}
