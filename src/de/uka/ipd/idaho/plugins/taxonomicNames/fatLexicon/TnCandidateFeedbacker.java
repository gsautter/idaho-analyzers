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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TnCandidateFeedbacker extends FatAnalyzer implements LiteratureConstants {
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(final MutableAnnotation data, Properties parameters) {
		
		//	make sure taxon names have page numbers
		QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
			if (pageNumber != null) {
				Annotation[] pTnCandidates = paragraphs[p].getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
				for (int t = 0; t < pTnCandidates.length; t++)
					pTnCandidates[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			}
		}
		
		//	get taxonomic name and candidate Annotations
		Annotation[] taxNames = data.getAnnotations(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
		Annotation[] candidates = data.getAnnotations(FAT.TAXONOMIC_NAME_CANDIDATE_ANNOTATION_TYPE);
		
		//	remove candidates contained in or containing a sure positive
		int tnIndex = 0;
		int candIndex = 0;
		ArrayList candCollector = new ArrayList();
		while ((tnIndex < taxNames.length) && (candIndex < candidates.length)) {
			
			//	candidate overlaps sure positive, ignore it
			if (AnnotationUtils.overlaps(taxNames[tnIndex], candidates[candIndex]))
				candIndex ++;
			
			//	switch to next sure positive
			else if (taxNames[tnIndex].getEndIndex() <= candidates[candIndex].getStartIndex())
				tnIndex ++;
			
			//	keep candidate and switch to next one
			else {
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
		
		//	bucketize taxonomic name candidates
		StringVector bucketNames = new StringVector();
		HashMap bucketsByName = new HashMap();
		
		for (int t = 0; t < candidates.length; t++) {
			String taxonNameKey = candidates[t].getValue();
			if (bucketsByName.containsKey(taxonNameKey))
				((TnCandidateBucket) bucketsByName.get(taxonNameKey)).tnCandidates.add(candidates[t]);
			
			else {
				TnCandidateBucket bucket = new TnCandidateBucket(candidates[t]);
				bucketNames.addElementIgnoreDuplicates(taxonNameKey);
				bucketsByName.put(taxonNameKey, bucket);
			}
		}
		
		//	process buckets
		ArrayList bucketList = new ArrayList();
		for (int b = 0; b < bucketNames.size(); b++) {
			if (bucketsByName.containsKey(bucketNames.get(b)))
				bucketList.add(bucketsByName.get(bucketNames.get(b)));
		}
		final TnCandidateBucket[] buckets = ((TnCandidateBucket[]) bucketList.toArray(new TnCandidateBucket[bucketList.size()]));
		
		//	don't show empty dialog
		if (buckets.length != 0) {
			
			//	assemble dialog
			final JDialog feedbackDialog = DialogFactory.produceDialog("Please Check if these Candidates are Taxon Names", true);
			
			//	table content
			TableModel candidateTableModel = new TableModel() {
				public Class getColumnClass(int columnIndex) {
					if (columnIndex == 0) return Boolean.class;
					else if (columnIndex == 1) return String.class;
					else return null;
				}
				public int getColumnCount() {
					return 2;
				}
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "Is Taxon?";
					else if (columnIndex == 1) return "Taxon Name Candidate (double-click for context)";
					else return null;
				}
				public int getRowCount() {
					return buckets.length;
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return (columnIndex == 0);
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (columnIndex == 0) return new Boolean(buckets[rowIndex].isTaxonName);
					else if (columnIndex == 1) return buckets[rowIndex].candidateNameString;
					else return null;
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
					if (columnIndex == 0) buckets[rowIndex].isTaxonName = ((Boolean) newValue).booleanValue();
				}
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			};
			
			//	table plus context popup functionality
			final JTable table = new JTable(candidateTableModel);
			table.getColumnModel().getColumn(0).setMaxWidth(100);
			table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						int clickRowIndex = table.rowAtPoint(me.getPoint());
						int clickColumnIndex = table.columnAtPoint(me.getPoint());
						if ((clickRowIndex != -1) && (clickColumnIndex == 1)) {
							StringVector docNames = new StringVector();
							for (int a = 0; a < buckets[clickRowIndex].tnCandidates.size(); a++) {
								Annotation taxonName = ((Annotation) buckets[clickRowIndex].tnCandidates.get(a));
								Object pageNumber = taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE);
								docNames.addElementIgnoreDuplicates(buildLabel(data, taxonName, 10) + " (at " + taxonName.getStartIndex() + ((pageNumber == null) ? ", unknown page" : (" on page " + pageNumber)) + ")");
							}
							String message = ("<HTML>This taxonomic name candidate appears in the following positions in the document :<BR>&nbsp;&nbsp;&nbsp;");
							message += docNames.concatStrings("<BR>&nbsp;&nbsp;&nbsp;");
							message += "</HTML>";
							JOptionPane.showMessageDialog(feedbackDialog, message, ("Occurences Of \"" + buckets[clickRowIndex].candidateNameString + "\""), JOptionPane.INFORMATION_MESSAGE);
						}
					}
				}
			});
			
			//	put table in scroll pane
			JScrollPane tableBox = new JScrollPane(table);
			tableBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			tableBox.getVerticalScrollBar().setUnitIncrement(50);
			feedbackDialog.getContentPane().setLayout(new BorderLayout());
			feedbackDialog.getContentPane().add(tableBox, BorderLayout.CENTER);
			
			//	add OK button
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					feedbackDialog.dispose();
				}
			});
			feedbackDialog.getContentPane().add(okButton, BorderLayout.SOUTH);
			
			//	ensure dialog is closed with button
			feedbackDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			//	get feedback
			feedbackDialog.setSize(450, (Math.min(650, (25 * buckets.length) + 50)));
			feedbackDialog.setLocationRelativeTo(null);
			feedbackDialog.setVisible(true);
			
			//	process feedback
			for (int t = 0; t < buckets.length; t++) {
				if (buckets[t].isTaxonName) {
					for (int a = 0; a < buckets[t].tnCandidates.size(); a++) {
						Annotation taxonName = ((Annotation) buckets[t].tnCandidates.get(a));
						taxonName.changeTypeTo(FAT.TAXONOMIC_NAME_ANNOTATION_TYPE);
						taxonName.setAttribute(FAT.EVIDENCE_ATTRIBUTE, "feedback");
						FAT.addTaxonName(new TaxonomicName(taxonName));
					}
				}
				else for (int a = 0; a < buckets[t].tnCandidates.size(); a++) {
						Annotation taxonName = ((Annotation) buckets[t].tnCandidates.get(a));
						if (taxonName.size() == 1)
							FAT.negatives.addElementIgnoreDuplicates(taxonName.firstValue());
					}
			}
		}
	}
	
	private class TnCandidateBucket implements Comparable {
		private String candidateNameString;
		private ArrayList tnCandidates = new ArrayList();
		private boolean isTaxonName = false;
		TnCandidateBucket(Annotation taxonName) {
			this.candidateNameString = taxonName.getValue();
			this.tnCandidates.add(taxonName);
		}
		public int compareTo(Object o) {
			if (o instanceof TnCandidateBucket) return this.candidateNameString.compareTo(((TnCandidateBucket) o).candidateNameString);
			else return -1;
		}
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
