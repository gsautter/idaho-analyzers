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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource.BlockWSS;
import de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource.EndWSS;
import de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource.SequenceWSS;
import de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource.WordScoreSource;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TnCandidateScorer extends FatAnalyzer {
	
	private WordScoreSource wss;
	private static final String UNCERTAINTY_LIMIT_SETTING_NAME = "UncertaintyLimit";
	private int uncertaintyLimit = 5;
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get taxonomic name and candidate Annotations
		Annotation[] taxNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		Annotation[] candidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	remove candidates contained in or containing a sure positive
		int tnIndex = 0;
		int candIndex = 0;
		ArrayList candCollector = new ArrayList();
		while ((tnIndex < taxNames.length) && (candIndex < candidates.length)) {
			
			//	candidate overlaps sure positive, ignore it
			if (AnnotationUtils.overlaps(taxNames[tnIndex], candidates[candIndex])) {
				candIndex ++;
				
			//	switch to next sure positive
			} else if (taxNames[tnIndex].getEndIndex() <= candidates[candIndex].getStartIndex()) {
				tnIndex ++;
				
			//	keep candidate and switch to next one
			} else {
				candCollector.add(candidates[candIndex]);
				candIndex ++;
			}
		}
		
		//	store remaining candidates (if any) and re-generate array
		while (candIndex < candidates.length) {
			candCollector.add(candidates[candIndex]);
			candIndex ++;
		}
		candidates = ((Annotation[]) candCollector.toArray(new Annotation[candCollector.size()]));
		
		//	collect example data
		StringVector positives = new StringVector();
		StringVector annotTokens = new StringVector();
		
		//	get sure positive words
		for (int t = 0; t < taxNames.length; t++) {
			positives.addElementIgnoreDuplicates(taxNames[t].getAttribute(GENUS_ATTRIBUTE, "").toString());
			positives.addElementIgnoreDuplicates(taxNames[t].getAttribute(SUBGENUS_ATTRIBUTE, "").toString());
			positives.addElementIgnoreDuplicates(taxNames[t].getAttribute(SPECIES_ATTRIBUTE, "").toString());
			positives.addElementIgnoreDuplicates(taxNames[t].getAttribute(SUBSPECIES_ATTRIBUTE, "").toString());
			positives.addElementIgnoreDuplicates(taxNames[t].getAttribute(VARIETY_ATTRIBUTE, "").toString());
			annotTokens.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(taxNames[t]));
		}
		positives.removeAll("");
		
		//	get non-negative words
		for (int c = 0; c < candidates.length; c++)
			annotTokens.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(candidates[c]));
		
		//	get negatives
		StringVector negatives = TokenSequenceUtils.getTextTokens(data).without(annotTokens);
		
		//	canonize remaining candidates to reduce feedback requests
		StringVector canonicUncertains = new StringVector();
		HashMap uncertainsToAnnotations = new HashMap();
		
		int matchCount = 0;
		int candidateCount = 0;
		int uncertainTokens = 0;
		
		for (int c = 0; c < candidates.length; c++) {
			candidateCount ++;
			uncertainTokens += candidates[c].size();
			TaxonomicName tn = new TaxonomicName(candidates[c]);
			String canonic = tn.toCanonicString();
			canonicUncertains.addElementIgnoreDuplicates(canonic);
			ArrayList canonicList = ((ArrayList) uncertainsToAnnotations.get(canonic));
			if (canonicList == null) {
				canonicList = new ArrayList();
				uncertainsToAnnotations.put(canonic, canonicList);
			}
			canonicList.add(candidates[c]);
		}
		canonicUncertains.sortLexicographically();
		
		//	apply word classifier and user feedback only if necessary
		if (canonicUncertains.size() != 0) {
			
			//	assemble WSS
			WordScoreSource documentWss = null;
			
			//	sufficient examples from document itself, build document local WSS
			if (positives.size() > (uncertainTokens * 5)) {
				
				BlockWSS block = new BlockWSS();
				SequenceWSS sequenceWSS;
				
				sequenceWSS = new SequenceWSS(2);
				sequenceWSS.setScoreMode(-2);
				sequenceWSS.setUncertaintyLimit(uncertaintyLimit);
				block.addWSS(sequenceWSS);
				
				sequenceWSS = new SequenceWSS(3);
				sequenceWSS.setScoreMode(-2);
				sequenceWSS.setUncertaintyLimit(uncertaintyLimit);
				block.addWSS(sequenceWSS);
				
				sequenceWSS = new SequenceWSS(4);
				sequenceWSS.setScoreMode(-2);
				sequenceWSS.setUncertaintyLimit(uncertaintyLimit);
				block.addWSS(sequenceWSS);
				
				EndWSS endWSS;
				
				endWSS = new EndWSS(3);
				endWSS.setUncertaintyLimit(uncertaintyLimit);
				block.addWSS(endWSS);
				
				endWSS = new EndWSS(4);
				endWSS.setUncertaintyLimit(uncertaintyLimit);
				block.addWSS(endWSS);
				
				documentWss = block;
				
				//	train global WSS with data harvested from document
				this.wss.train(positives, negatives);
				this.wss.optimize();
				
			//	too little example data from document, use general WSS
			} else {
				documentWss = this.wss;
			}
			
			//	train WSS with data harvested from document
			documentWss.train(positives, negatives);
			documentWss.optimize();
			
			//	vote candidates
			StringVector feedbackCandidates = new StringVector();
			for (int c = 0; c < canonicUncertains.size(); c++) {
				StringVector tt = new StringVector();
				tt.parseAndAddElements(canonicUncertains.get(c), " ");
				
				int score = 0;
				for (int t = 0; t < tt.size(); t++) 
					if ((tt.get(t).length() > 2) && !tt.get(t).endsWith(".")) score += documentWss.getScore(tt.get(t));
				
				int vote = documentWss.getVote(score);
				
				if (vote == 0) {
					feedbackCandidates.addElementIgnoreDuplicates(canonicUncertains.get(c));
				} else {
					ArrayList canonicList = ((ArrayList) uncertainsToAnnotations.get(canonicUncertains.get(c)));
					for (int a = 0; a < canonicList.size(); a++) {
						((Annotation) canonicList.get(a)).setAttribute(FAT.EVIDENCE_ATTRIBUTE, ("WSS:" + score));
						if (vote == 1) {
							matchCount ++;
							((Annotation) canonicList.get(a)).changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
							FAT.addTaxonName(new TaxonomicName((Annotation) canonicList.get(a)));
						} else if (vote == -1) data.removeAnnotation(((Annotation) canonicList.get(a)));
					}
				}
			}
			
			//	assemble and display feedback dialog
			if (feedbackCandidates.size() != 0) {
				feedbackCandidates.sortLexicographically(false, false);
				JCheckBox[] feedbacks = new JCheckBox[feedbackCandidates.size()];
				JPanel feedbackPanel = new JPanel(new GridLayout(0, 1));
				for (int c = 0; c < feedbackCandidates.size(); c++) {
					feedbacks[c] = new JCheckBox(feedbackCandidates.get(c), false);
					feedbackPanel.add(feedbacks[c]);
				}
				final JDialog feedbackDialog = new JDialog(((JFrame) null), "Please check if these Strings are taxonomic names", true);
				feedbackDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						feedbackDialog.dispose();
					}
				});
				feedbackDialog.getContentPane().setLayout(new BorderLayout());
				JScrollPane feedbackPanelBox = new JScrollPane(feedbackPanel);
				feedbackPanelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				feedbackPanelBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
				feedbackPanelBox.getVerticalScrollBar().setUnitIncrement(50);
				feedbackDialog.getContentPane().add(feedbackPanelBox, BorderLayout.CENTER);
				feedbackDialog.getContentPane().add(okButton, BorderLayout.SOUTH);
				feedbackDialog.setSize(400, (Math.min((feedbacks.length * 25), 650) + 50));
				feedbackDialog.setLocationRelativeTo(null);
				feedbackDialog.setVisible(true);
				
				//	evaluate feedback
				StringVector feedbackPositives = new StringVector();
				StringVector feedbackNegatives = new StringVector();
				for (int f = 0; f < feedbacks.length; f++) {
					ArrayList canonicList = ((ArrayList) uncertainsToAnnotations.get(feedbackCandidates.get(f)));
					for (int a = 0; a < canonicList.size(); a++) {
						((Annotation) canonicList.get(a)).setAttribute("evidence", "feedback");
						if (feedbacks[f].isSelected()) {
							((Annotation) canonicList.get(a)).changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
							FAT.addTaxonName(new TaxonomicName((Annotation) canonicList.get(a)));
							feedbackPositives.parseAndAddElements(feedbackCandidates.get(f), " ");
						} else {
							feedbackNegatives.parseAndAddElements(feedbackCandidates.get(f), " ");
							data.removeAnnotation((Annotation) canonicList.get(a));
						}
					}
				}
				
				//	remove known positives from feedbackNegatives, for user might have excluded negative containing positive tokens
				feedbackNegatives = feedbackNegatives.without(positives);
				feedbackNegatives = feedbackNegatives.without(feedbackPositives);
				
				//	train global WSS with feedback
				feedbackPositives.removeDuplicateElements();
				feedbackNegatives.removeDuplicateElements();
				this.wss.train(feedbackPositives, feedbackNegatives);
			}
			
		//	even if no word classifications, train global WSS with data harvested from document
		} else this.wss.train(positives, negatives);
	}
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		super.exitAnalyzer();
		this.storeParameter(UNCERTAINTY_LIMIT_SETTING_NAME, ("" + uncertaintyLimit));
	}
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		super.initAnalyzer();
		
		//	read uncertainty limit
		try {
			this.uncertaintyLimit = Integer.parseInt(this.getParameter(UNCERTAINTY_LIMIT_SETTING_NAME, ("" + uncertaintyLimit)));
		} catch (Exception e) {}
		
		//	assemble WSS
		BlockWSS blockWSS = new BlockWSS();
		SequenceWSS sequenceWSS;
		
		sequenceWSS = new SequenceWSS(4);
		sequenceWSS.setScoreMode(-2);
		sequenceWSS.setUncertaintyLimit(this.uncertaintyLimit);
		blockWSS.addWSS(sequenceWSS);
		
		sequenceWSS = new SequenceWSS(3);
		sequenceWSS.setScoreMode(-2);
		sequenceWSS.setUncertaintyLimit(this.uncertaintyLimit);
		blockWSS.addWSS(sequenceWSS);
		
		sequenceWSS = new SequenceWSS(2);
		sequenceWSS.setScoreMode(-2);
		sequenceWSS.setUncertaintyLimit(this.uncertaintyLimit);
		blockWSS.addWSS(sequenceWSS);
		
		EndWSS endWSS; 
		endWSS = new EndWSS(4);
		endWSS.setUncertaintyLimit(this.uncertaintyLimit);
		blockWSS.addWSS(endWSS);
		
		endWSS = new EndWSS(3);
		endWSS.setUncertaintyLimit(this.uncertaintyLimit);
		blockWSS.addWSS(endWSS);
		
		blockWSS.setUncertaintyLimit(this.uncertaintyLimit);
		this.wss = blockWSS;
		
		//	train WSS
		StringVector positives = new StringVector(false);
		positives.addContentIgnoreDuplicates(FAT.genera);
		positives.addContentIgnoreDuplicates(FAT.subGenera);
		positives.addContentIgnoreDuplicates(FAT.species);
		positives.addContentIgnoreDuplicates(FAT.subSpecies);
		positives.addContentIgnoreDuplicates(FAT.varieties);
		this.wss.train(positives, FAT.negatives);
	}
}
