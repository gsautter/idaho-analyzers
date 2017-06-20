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

package de.uka.ipd.idaho.plugins.materialsCitations;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class CollectionCodeTagger extends AbstractAnalyzer implements MaterialsCitationConstants {
	
	private Properties collectionCodes = new Properties();
	
	private Properties collectionCodesAdded = new Properties();
	private boolean collectionCodesAddedModified = false;
	
	private StringVector collectionCodesExclude = new StringVector();
	private boolean collectionCodesExcludeModified = false;
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractAnalyzer#setDataProvider(de.gamta.util.AnalyzerDataProvider)
	 */
	public void setDataProvider(AnalyzerDataProvider dataProvider) {
		super.setDataProvider(dataProvider);
		
		InputStream is = null;
		try {
			is = this.dataProvider.getInputStream("collectionCodes.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				int split = line.indexOf(' ');
				if (split != -1) {
					String cc = line.substring(0, split).trim();
					String cn = line.substring(split).trim();
					if ((cc.length() != 0) && (cn.length() != 0))
						this.collectionCodes.setProperty(cc, cn);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading collection codes.");
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {}
		}
		
		try {
			is = this.dataProvider.getInputStream("collectionCodesAdded.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				int split = line.indexOf(' ');
				if (split != -1) {
					String cc = line.substring(0, split).trim();
					String cn = line.substring(split).trim();
					if ((cc.length() != 0) && (cn.length() != 0))
						this.collectionCodesAdded.setProperty(cc, cn);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading additional collection codes.");
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {}
		}
		
		try {
			is = this.dataProvider.getInputStream("collectionCodesExclude.txt");
			this.collectionCodesExclude = StringVector.loadList(is);
		}
		catch (IOException ioe) {
			System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading exclusion list.");
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractAnalyzer#exit()
	 */
	public void exit() {
		OutputStream os = null;
		
		if (this.collectionCodesAddedModified && this.dataProvider.isDataEditable("collectionCodesAdded.txt")) {
			try {
				StringVector ccLines = new StringVector();
				Iterator ccIt = this.collectionCodesAdded.keySet().iterator();
				while (ccIt.hasNext()) {
					String cc = ((String) ccIt.next());
					String cn = this.collectionCodesAdded.getProperty(cc);
					ccLines.addElement(cc + " " + cn);
				}
				ccLines.sortLexicographically(false, false);
				os = this.dataProvider.getOutputStream("collectionCodesAdded.txt");
				ccLines.storeContent(os);
				os.flush();
			}
			catch (IOException ioe) {
				System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while storing modified additional collection codes.");
				ioe.printStackTrace(System.out);
			}
			finally {
				if (os != null) try {
					os.close();
				} catch (IOException e) {}
			}
		}
		
		if (this.collectionCodesExcludeModified && this.dataProvider.isDataEditable("collectionCodesExclude.txt")) {
			try {
				os = this.dataProvider.getOutputStream("collectionCodesExclude.txt");
				this.collectionCodesExclude.storeContent(os);
				os.flush();
			}
			catch (IOException ioe) {
				System.out.println("CollectionCodeTagger: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while storing modified exclusion list.");
				ioe.printStackTrace(System.out);
			}
			finally {
				if (os != null) try {
					os.close();
				} catch (IOException e) {}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		ArrayList collectionCodeCandidates = new ArrayList();
		
		//	annotate known collection codes, and collect candidates
		for (int v = 0; v < data.size(); v++) {
			String value = data.valueAt(v);
			
			//	check if collection code
			if (this.collectionCodes.containsKey(value) || this.collectionCodesAdded.containsKey(value)) {
				
				//	annotate
				Annotation collectionCode = data.addAnnotation(COLLECTION_CODE, v, 1);
				
				//	resolve collection code (get name)
				String collectionName = this.resolveCollectionCode(value);
				if (collectionName != null) collectionCode.setAttribute(COLLECTION_NAME_ATTRIBUTE, collectionName);
			}
			
			//	collect candidate collection codes
			else if (!this.collectionCodesExclude.contains(value) && value.matches("[A-Z]{3,5}"))
				collectionCodeCandidates.add(Gamta.newAnnotation(data, COLLECTION_CODE, v, 1));
		}
		
		//	user interaction not allowed, forget about candidates
		if (!parameters.containsKey(INTERACTIVE_PARAMETER) || collectionCodeCandidates.isEmpty())
			return;
		
		//	bucketize candidates
		HashMap bucketsByCollectionCode = new LinkedHashMap();
		for (int c = 0; c < collectionCodeCandidates.size(); c++) {
			Annotation collectionCodeCandidate = ((Annotation) collectionCodeCandidates.get(c));
			CcBucket collectionCodeBucket = ((CcBucket) bucketsByCollectionCode.get(collectionCodeCandidate.getValue()));
			if (collectionCodeBucket == null) {
				collectionCodeBucket = new CcBucket(collectionCodeCandidate.getValue());
				bucketsByCollectionCode.put(collectionCodeBucket.collectionCode, collectionCodeBucket);
			}
			collectionCodeBucket.collectionCodeAnnotations.add(collectionCodeCandidate);
		}
		
		//	put buckets on trays
		CcBucketTray[] trays = new CcBucketTray[bucketsByCollectionCode.size()];
		int trayIndex = 0;
		for (Iterator ccIt = bucketsByCollectionCode.values().iterator(); ccIt.hasNext();)
			trays[trayIndex++] = new CcBucketTray((CcBucket) ccIt.next(), data);
		
		JPanel trayPanel = new JPanel(new GridLayout(0, 1, 5, 2), true);
		for (int t = 0; t < trays.length; t++)
			trayPanel.add(trays[t]);
		
		JScrollPane trayPanelBox = new JScrollPane(trayPanel);
		trayPanelBox.getVerticalScrollBar().setUnitIncrement(50);
		trayPanelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		//	get feedback
		final JDialog dialog = DialogFactory.produceDialog("Please Check Collection Codes", true);
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				dialog.dispose();
			}
		});
		
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(trayPanelBox, BorderLayout.CENTER);
		dialog.getContentPane().add(okButton, BorderLayout.SOUTH);
		
		dialog.setSize(500, Math.min(650, Math.max(100, (50 + 30 * trays.length))));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		//	process feedback
		for (int t = 0; t < trays.length; t++) {
			String collectionCode = trays[t].collectionCodeBucket.collectionCode;
			
			//	collection code
			if (trays[t].isCollectionCode.isSelected()) {
				
				//	remember
				String collectionName = trays[t].collectionNameField.getText().trim();
				if (collectionName.length() == 0)
					collectionName = collectionCode;
				
				this.collectionCodesAdded.setProperty(collectionCode, collectionName);
				this.collectionCodesAddedModified = true;
				
				//	annotate
				for (int c = 0; c < trays[t].collectionCodeBucket.collectionCodeAnnotations.size(); c++) {
					Annotation collectionCodeAnnotation = ((Annotation) trays[t].collectionCodeBucket.collectionCodeAnnotations.get(c));
					collectionCodeAnnotation = data.addAnnotation(COLLECTION_CODE, collectionCodeAnnotation.getStartIndex(), 1);
					collectionCodeAnnotation.setAttribute(COLLECTION_NAME_ATTRIBUTE, collectionName);
				}
			}
			
			//	excluded, remember exclusion
			else {
				this.collectionCodesExclude.addElementIgnoreDuplicates(collectionCode);
				this.collectionCodesExcludeModified = true;
			}
		}
	}
	
	private class CcBucket {
		private String collectionCode;
		private ArrayList collectionCodeAnnotations = new ArrayList();
		private CcBucket(String cc) {
			this.collectionCode = cc;
		}
	}
	
	private class CcBucketTray extends JPanel {
		private CcBucket collectionCodeBucket;
		private JCheckBox isCollectionCode = new JCheckBox("Is Coll. Code", true);
		private JTextField collectionNameField = new JTextField();
		public CcBucketTray(final CcBucket collectionCodeBucket, final TokenSequence text) {
			super(new GridBagLayout(), true);
			this.collectionCodeBucket = collectionCodeBucket;
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			
			JLabel ccLabel = new JLabel(collectionCodeBucket.collectionCode, JLabel.LEFT);
			ccLabel.setPreferredSize(new Dimension(35, 21));
			ccLabel.setToolTipText("Click to display occurrences of '" + collectionCodeBucket.collectionCode + "' in document.");
			ccLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					StringVector docNames = new StringVector();
					for (int c = 0; c < collectionCodeBucket.collectionCodeAnnotations.size(); c++) {
						Annotation cc = ((Annotation) collectionCodeBucket.collectionCodeAnnotations.get(c));
						docNames.addElementIgnoreDuplicates(buildLabel(text, cc, 10) + " (at " + cc.getStartIndex());
					}
					String message = ("<HTML>\"" + collectionCodeBucket.collectionCode + "\" appears in the following contexts in the document:<BR>&nbsp;&nbsp;&nbsp;");
					message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
					message += "</HTML>";
					JOptionPane.showMessageDialog(CcBucketTray.this, message, ("Occurrences Of \"" + collectionCodeBucket.collectionCode + "\""), JOptionPane.INFORMATION_MESSAGE);
				}
			});
			this.add(ccLabel, gbc.clone());
			gbc.gridx++;
			
			this.isCollectionCode.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					collectionNameField.setEditable(isCollectionCode.isSelected());
				}
			});
			
			this.add(this.isCollectionCode, gbc.clone());
			gbc.gridx++;
			
			JLabel cnLabel = new JLabel("Collection Name:", JLabel.LEFT);
			cnLabel.setToolTipText("Enter collection name for code '" + collectionCodeBucket.collectionCode + "' here. For a reference to another collection code, type 'see <OtherCode>'.");
			this.add(cnLabel, gbc.clone());
			gbc.gridx++;
			
			this.collectionNameField.setText(collectionCodeBucket.collectionCode);
			gbc.weightx = 1;
			this.add(this.collectionNameField, gbc.clone());
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
	
	private String resolveCollectionCode(String collectionCode) {
		String collectionName = this.collectionCodes.getProperty(collectionCode);
		
		//	check additional (learned) codes
		if (collectionName == null)
			collectionName = this.collectionCodesAdded.getProperty(collectionCode);
		
		//	catch unknown codes
		if (collectionName == null) return null;
		
		//	pure reference
		if (collectionName.startsWith("see ")) {
			String referencedCollectionCode = collectionName.substring(4).trim();
			return this.resolveCollectionCode(referencedCollectionCode);
		}
		
		//	actual collection name
		else {
			
			//	check if reference included
			int see = collectionName.indexOf(" see ");
			
			//	no reference
			if (see == -1) return collectionName;
			
			//	subordinate reference to resolve
			else {
				String referencedCollectionCode = collectionName.substring(see + 4).trim();
				if (referencedCollectionCode.indexOf(' ') != -1)
					referencedCollectionCode = referencedCollectionCode.substring(0, referencedCollectionCode.indexOf(' '));
				String referencedCollectionName = this.resolveCollectionCode(referencedCollectionCode);
				
				//	reference could not be resolved
				if (referencedCollectionName == null) return collectionName;
				
				//	build new name string
				else return (collectionName.substring(0, see) + " see " + referencedCollectionName);
			}
		}
	}
}
