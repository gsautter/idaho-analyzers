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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This Analzyer parses meaningful parts from taxonomicName Annotations,
 * completes the names (resolves abbreviated parts and fills in missing parts),
 * and displays the result for manual check and correction.<br>
 * <br>
 * This Analyzer uses the GAMTA feedback API and thus can be run inside a markup
 * server using remote user feedback.
 * 
 * @author sautter
 */
public class TaxonomicNameParserOnline extends OmniFatAnalyzer implements LiteratureConstants {
	
	private static final boolean DEBUG = true;
	private static final String RANK_HASH_ATTRIBUTE = "_rankHash";
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	check parameter
		if ((data == null) || !parameters.containsKey(INTERACTIVE_PARAMETER)) return;
		
		//	get base data
		OmniFAT.DocumentDataSet docData = this.getDataSet(data, parameters);
		OmniFAT.Rank[] ranks = docData.omniFat.getRanks();
		
		//	get taxonomic names
		MutableAnnotation[] taxonNames = data.getMutableAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		
		//	check for re-runs
		boolean isReRun = true;
		
		//	strip rank attribute from names modified since rank computed
		for (int t = 0; t < taxonNames.length; t++) {
			String rank = ((String) taxonNames[t].getAttribute(OmniFAT.RANK_ATTRIBUTE));
			if (rank == null)
				isReRun = false;
			else {
				int currentHash = taxonNames[t].getValue().hashCode();
				String rankHash = ((String) taxonNames[t].getAttribute(RANK_HASH_ATTRIBUTE));
				if ((rankHash == null) || !rankHash.equals("" + currentHash)) {
					taxonNames[t].removeAttribute(OmniFAT.RANK_ATTRIBUTE);
					isReRun = false;
				}
			}
		}
		
		//	handle names with rank genus and below iteratively
		ArrayList feedbackBucketList = new ArrayList();
		do {
			
			//	clear list (it's full in the end of the loop if there are names left)
			feedbackBucketList.clear();
			
			//	make sure taxon names have page numbers
			QueriableAnnotation[] paragraphs = data.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			for (int p = 0; p < paragraphs.length; p++) {
				Object pageNumber = paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE);
				if (pageNumber != null) {
					Annotation[] pTaxonNames = paragraphs[p].getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
					for (int t = 0; t < pTaxonNames.length; t++)
						if (!pTaxonNames[t].hasAttribute(PAGE_NUMBER_ATTRIBUTE)) // avoid too many attribute change events
							pTaxonNames[t].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
				}
				Object pageId = paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE);
				if (pageId != null) {
					Annotation[] pTaxonNames = paragraphs[p].getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
					for (int t = 0; t < pTaxonNames.length; t++)
						if (!pTaxonNames[t].hasAttribute(PAGE_ID_ATTRIBUTE)) // avoid too many attribute change events
							pTaxonNames[t].setAttribute(PAGE_ID_ATTRIBUTE, pageId);
				}
			}
			
			//	refresh taxon names
			taxonNames = data.getMutableAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
			
			
			//	parse names (only the ones not parsed so far)
			for (int t = 0; t < taxonNames.length; t++)
				this.extractEpithetAttributes(taxonNames[t], docData);
			
			//	complete names
			OmniFatFunctions.completeAbbreviations(data, docData);
			OmniFatFunctions.inferRanks(data, docData);
			OmniFatFunctions.completeNames(data, docData);
			
			
			//	extract genera, etc
			HashMap rankGroupEpithetListMap = new HashMap();
			for (int t = 0; t < taxonNames.length; t++) {
				for (int r = 0; r < ranks.length; r++) {
					String epithet = ((String) taxonNames[t].getAttribute(ranks[r].getName()));
					if (epithet == null)
						continue;
					if (ranks[r].getRankGroup().isEpithetsCapitalized())
						epithet = (epithet.substring(0,1) + epithet.substring(1).toLowerCase());
					else if (ranks[r].getRankGroup().isEpithetsLowerCase())
						epithet = epithet.toLowerCase();
					
					StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetListMap.get(ranks[r].getRankGroup().getName()));
					if (rankGroupEpithetList == null) {
						rankGroupEpithetList = new StringVector();
						rankGroupEpithetListMap.put(ranks[r].getRankGroup().getName(), rankGroupEpithetList);
					}
					rankGroupEpithetList.addElementIgnoreDuplicates(epithet);
				}
			}
			
			//	build and sort name part arrays
			OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
			String[] rankGroupNames = new String[rankGroups.length];
			String[][] rankGroupRankNames = new String[rankGroups.length][];
			String[][] rankGroupEpithetLists = new String[rankGroups.length][];
			StringVector requiredRankNames = new StringVector();
			for (int rg = 0; rg < rankGroups.length; rg++) {
				rankGroupNames[rg] = rankGroups[rg].getName();
				OmniFAT.Rank[] rankGroupRanks = rankGroups[rg].getRanks();
				rankGroupRankNames[rg] = new String[rankGroupRanks.length];
				for (int r = 0; r < rankGroupRanks.length; r++) {
					rankGroupRankNames[rg][r] = rankGroupRanks[r].getName();
					if (rankGroupRanks[r].isEpithetRequired())
						requiredRankNames.addElement(rankGroupRanks[r].getName());
				}
				StringVector rankGroupEpithetList = ((StringVector) rankGroupEpithetListMap.get(rankGroupNames[rg]));
				if (rankGroupEpithetList == null)
					rankGroupEpithetLists[rg] = new String[0];
				else {
					rankGroupEpithetLists[rg] = rankGroupEpithetList.toStringArray();
					Arrays.sort(rankGroupEpithetLists[rg]);
				}
			}
			
			int splitFromIndex = ranks.length;
			int cutFromIndex = ranks.length + 1;
			
			//	bucketize names (use linked hash map for having buckets in order of first appearance in document)
			HashMap bucketsByNameString = new LinkedHashMap();
			for (int t = 0; t < taxonNames.length; t++) {
				String taxonNameKey = (OmniFatFunctions.getNameString(taxonNames[t], docData.omniFat) + " " + taxonNames[t].getValue());
				if (bucketsByNameString.containsKey(taxonNameKey))
					((TaxonNameBucket) bucketsByNameString.get(taxonNameKey)).taxonNames.add(taxonNames[t]);
				
				else {
					TaxonNameBucket bucket = new TaxonNameBucket(taxonNames[t], taxonNameKey);
					bucketsByNameString.put(taxonNameKey, bucket);
				}
			}
			
			//	sort out buckets that contain a name with a RANK attribute
			for (Iterator bit = bucketsByNameString.values().iterator(); bit.hasNext();) {
				TaxonNameBucket tnb = ((TaxonNameBucket) bit.next());
				
				//	find annotation with rank attribute
				Annotation ranked = null;
				for (int a = 0; a < tnb.taxonNames.size(); a++) {
					Annotation tna = ((Annotation) tnb.taxonNames.get(a));
					if (tna.hasAttribute(OmniFAT.RANK_ATTRIBUTE)) {
						ranked = tna;
						a = tnb.taxonNames.size();
					}
				}
				
				//	no rank given, need to get feedback for bucket
				if (ranked == null)
					feedbackBucketList.add(tnb);
				
				//	got at least one ranked annotation in bucket
				else {
					
					//	transfer rank (all name part attributes match, otherwise names would be in different buckets)
					for (int a = 0; a < tnb.taxonNames.size(); a++) {
						Annotation tna = ((Annotation) tnb.taxonNames.get(a));
						if (!tna.hasAttribute(OmniFAT.RANK_ATTRIBUTE)) {// avoid too many attribute change events
							tna.setAttribute(OmniFAT.RANK_ATTRIBUTE, ranked.getAttribute(OmniFAT.RANK_ATTRIBUTE));
							tna.setAttribute(RANK_HASH_ATTRIBUTE, ("" + tna.getValue().hashCode()));
						}
					}
					
					//	make sure we get feedback on re-runs
					if (isReRun)
						feedbackBucketList.add(tnb);
				}
			}
			
			//	make sure to not prompt infinitely on re-runs
			isReRun = false;
			
			//	make way for splits
			TaxonNameBucket[] buckets = ((TaxonNameBucket[]) feedbackBucketList.toArray(new TaxonNameBucket[feedbackBucketList.size()]));
			feedbackBucketList.clear();
			
			//	don't get feedback if not required
			if (buckets.length != 0) {
				
				//	data structures for feedback data
				boolean[] remove = new boolean[buckets.length];
				for (int r = 0; r < remove.length; r++)
					remove[r] = false;
				int[][] meaningIndices = new int[buckets.length][];
				for (int m = 0; m < meaningIndices.length; m++) {
					meaningIndices[m] = new int[buckets[m].taxonName.size()];
					for (int t = 0; t < meaningIndices[m].length; t++)
						meaningIndices[m][t] = TaxonomicNameParserFeedbackPanel.noMeaningIndex;
				}
				String[][] meaningfulParts = new String[buckets.length][];
				for (int m = 0; m < meaningfulParts.length; m++) {
					meaningfulParts[m] = new String[ranks.length];
					for (int p = 0; p < meaningfulParts[m].length; p++)
						meaningfulParts[m][p] = null;
				}
				String[] bucketRanks = new String[buckets.length];
				for (int r = 0; r < bucketRanks.length; r++)
					bucketRanks[r] = null;
				
				
				//	compute number of buckets per dialog
				int dialogCount = ((buckets.length + 9) / 10);
				int dialogSize = ((buckets.length + (dialogCount / 2)) / dialogCount);
				dialogCount = ((buckets.length + dialogSize - 1) / dialogSize);
				
				
				//	build dialogs
				TaxonomicNameParserFeedbackPanel[] tnpfps = new TaxonomicNameParserFeedbackPanel[dialogCount];
				for (int d = 0; d < tnpfps.length; d++) {
					tnpfps[d] = new TaxonomicNameParserFeedbackPanel(rankGroupNames, rankGroupRankNames, rankGroupEpithetLists, requiredRankNames.toStringArray());
					tnpfps[d].setLabel("<HTML>Please check if the meaningful parts of these taxon names are selected corretly." +
							"<BR>Click on the individual name parts to open a popup menu for selecting the part's meaning." +
							"<BR>You can also specify to cut a name up to a specific part, i.e., to exclude all parts up to the selected one." +
							"<BR>Likewise, you can cut or split a taxon name from a specific part onward (in the latter case marking the cut part as a separate taxon name)." +
							"<BR>User the drop-down boxes below the name to specify missing parts or the full forms of abbreviated ones." +
							"<BR>If a taxon name actually is none at all, use the 'Remove' checkbox to indicate so.</HTML>");
					int dialogOffset = (d * dialogSize);
					
					//	add taxon names
					for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < buckets.length); b++)
						tnpfps[d].addTaxonName(buckets[b + dialogOffset].taxonName, ((Annotation[]) buckets[b + dialogOffset].taxonNames.toArray(new Annotation[buckets[b + dialogOffset].taxonNames.size()])), data);
					
					//	add backgroung information
					tnpfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, buckets[dialogOffset].taxonName.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
					tnpfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, buckets[dialogOffset].taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
					tnpfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, buckets[dialogOffset].taxonName.getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
					tnpfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, buckets[dialogOffset].taxonName.getAnnotationID());
					tnpfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, OmniFAT.TAXONOMIC_NAME_TYPE);
					
					//	add target page numbers
					String targetPages = FeedbackPanel.getTargetPageString((Annotation[]) buckets[dialogOffset].taxonNames.toArray(new Annotation[buckets[dialogOffset].taxonNames.size()]));
					if (targetPages != null)
						tnpfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
					String targetPageIDs = FeedbackPanel.getTargetPageIdString((Annotation[]) buckets[dialogOffset].taxonNames.toArray(new Annotation[buckets[dialogOffset].taxonNames.size()]));
					if (targetPageIDs != null)
						tnpfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
				}
				int cutoffBucket = buckets.length;
				
				//	can we issue all dialogs at once?
				if (FeedbackPanel.isMultiFeedbackEnabled()) {
					FeedbackPanel.getMultiFeedback(tnpfps);
					
					//	process all feedback data together
					for (int d = 0; d < tnpfps.length; d++) {
						int dialogOffset = (d * dialogSize);
						for (int b = 0; b < tnpfps[d].getTaxonNameCount(); b++) {
							remove[b + dialogOffset] = tnpfps[d].removeTaxonNameAt(b);
							for (int t = 0; t < buckets[b + dialogOffset].taxonName.size(); t++)
								meaningIndices[b + dialogOffset][t] = tnpfps[d].getMeaningIndexAt(b, t);
							for (int r = 0; r < ranks.length; r++)
								meaningfulParts[b + dialogOffset][r] = tnpfps[d].getMeaningfulPartAt(b, r);
							bucketRanks[b + dialogOffset] = tnpfps[d].getRankAt(b);
						}
					}
				}
				
				//	display dialogs one by one otherwise (allow cancel in the middle)
				else for (int d = 0; d < tnpfps.length; d++) {
					if (d != 0)
						tnpfps[d].addButton("Previous");
					tnpfps[d].addButton("Cancel");
					tnpfps[d].addButton("OK" + (((d+1) == tnpfps.length) ? "" : " & Next"));
					
					String title = tnpfps[d].getTitle();
					tnpfps[d].setTitle(title + " - (" + (d+1) + " of " + tnpfps.length + ")");
					
					String f = tnpfps[d].getFeedback();
					if (f == null)
						f = "Cancel";
					
					tnpfps[d].setTitle(title); 
					
					//	current dialog submitted, process data
					if (f.startsWith("OK")) {
						int dialogOffset = (d * dialogSize);
						for (int b = 0; b < tnpfps[d].getTaxonNameCount(); b++) {
							remove[b + dialogOffset] = tnpfps[d].removeTaxonNameAt(b);
							for (int t = 0; t < buckets[b + dialogOffset].taxonName.size(); t++)
								meaningIndices[b + dialogOffset][t] = tnpfps[d].getMeaningIndexAt(b, t);
							for (int r = 0; r < ranks.length; r++)
								meaningfulParts[b + dialogOffset][r] = tnpfps[d].getMeaningfulPartAt(b, r);
							bucketRanks[b + dialogOffset] = tnpfps[d].getRankAt(b);
						}
					}
					
					//	back to previous dialog
					else if ("Previous".equals(f))
						d-=2;
					
					//	cancel from current dialog on
					else {
						cutoffBucket = (d * dialogSize);
						d = tnpfps.length;
					}
				}
				
				
				//	process cuts and splits
				for (int b = 0; b < cutoffBucket; b++) {
					
					//	check if cut or split
					int start = 0;
					int split = buckets[b].taxonName.size();
					int end = buckets[b].taxonName.size();
					for (int t = 0; t < buckets[b].taxonName.size(); t++) {
						int meaning = meaningIndices[b][t];
						if (meaning == TaxonomicNameParserFeedbackPanel.cutToIndex)
							start = t+1;
						else if (meaning == splitFromIndex)
							split = t;
						else if (meaning == cutFromIndex)
							end = t;
					}
					
					//	make sure split is at most end
					if (end < split)
						split = end;
					
					//	adjust indices for removal
					if (remove[b]) {
						start = 0;
						split = 0;
						end = 0;
					}
					
					//	do split (if any)
					if ((end - split) != 0) {
						TaxonNameBucket splitTnb = null;
						for (int a = 0; a < buckets[b].taxonNames.size(); a++) {
							Annotation tna = ((Annotation) buckets[b].taxonNames.get(a));
							MutableAnnotation splitTna = data.addAnnotation(tna.getType(), (tna.getStartIndex() + split), (end - split));
							
							if (splitTnb == null) {
								splitTnb = new TaxonNameBucket(splitTna, ((OmniFatFunctions.getNameString(splitTna, docData.omniFat) + " " + splitTna.getValue())));
								feedbackBucketList.add(splitTnb);
							}
							else splitTnb.taxonNames.add(splitTna);
						}
					}
					
					//	do remove
					if (split == start) {
						for (int a = 0; a < buckets[b].taxonNames.size(); a++) {
							Annotation tna = ((Annotation) buckets[b].taxonNames.get(a));
							data.removeAnnotation(tna);
						}
						buckets[b].taxonNames.clear();
						buckets[b] = null;
					}
					
					//	do cut (if any)
					else if ((split - start) < buckets[b].taxonName.size()) {
						TaxonNameBucket cutTnb = null;
						for (int a = 0; a < buckets[b].taxonNames.size(); a++) {
							Annotation tna = ((Annotation) buckets[b].taxonNames.get(a));
							
							MutableAnnotation cutTna = data.addAnnotation(tna.getType(), (tna.getStartIndex() + start), (split - start));
							if (cutTnb == null)
								cutTnb = new TaxonNameBucket(cutTna, ((OmniFatFunctions.getNameString(cutTna, docData.omniFat) + " " + cutTna.getValue())));
							
							else cutTnb.taxonNames.add(cutTna);
							
							data.removeAnnotation(tna);
						}
						buckets[b].taxonNames.clear();
						buckets[b] = cutTnb;
					}
				}
				
				//	process meaningful parts (if bucket not removed)
				for (int b = 0; b < cutoffBucket; b++) {
					if (buckets[b] != null)
						for (int a = 0; a < buckets[b].taxonNames.size(); a++) {
							Annotation tna = ((Annotation) buckets[b].taxonNames.get(a));
							for (int r = 0; r < ranks.length; r++) {
								if (meaningfulParts[b][r] == null)
									tna.removeAttribute(ranks[r].getName());
								else {
									String epithet = meaningfulParts[b][r];
									if (ranks[r].getRankGroup().isEpithetsCapitalized())
										epithet = (epithet.substring(0,1) + epithet.substring(1).toLowerCase());
									else if (ranks[r].getRankGroup().isEpithetsLowerCase())
										epithet = epithet.toLowerCase();
									tna.setAttribute(ranks[r].getName(), epithet);
//									tna.setAttribute(ranks[r].getName(), meaningfulParts[b][r]);
								}
							}
							if (bucketRanks[b] == null)
								tna.removeAttribute(OmniFAT.RANK_ATTRIBUTE);
							else {
								tna.setAttribute(OmniFAT.RANK_ATTRIBUTE, bucketRanks[b]);
								tna.setAttribute(RANK_HASH_ATTRIBUTE, ("" + tna.getValue().hashCode()));
							}
						}
				}
				
				//	WE DON'T NEED THIS ANY MORE BECAUSE THE NEXT ROUND WILL RE-BUCKETIZE:
				//	recollect buckets by name (if bucket not removed)
				//	distribute split names to existing buckets
				//	conflate split name buckets (some will remain for next round of feedback)
			}
		}
		while (feedbackBucketList.size() != 0);
		
		
		//	refresh taxon names one last time
		taxonNames = data.getMutableAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
		
		//	remember user input
		for (int t = 0; t < taxonNames.length; t++) {
			
			//	go part by part (if rank set and thus name feedbacked/secure)
			if (taxonNames[t].hasAttribute(OmniFAT.RANK_ATTRIBUTE))
				for (int r = 0; r < ranks.length; r++) {
					String epithet = ((String) taxonNames[t].getAttribute(ranks[r].getName(), ""));
					
					//	avoid abbreviations (none should be left, but feedback might have been cancelled), and also avoid empty parts
					if (epithet.length() > 2)
						docData.addSureEpithet(ranks[r].getRankGroup().getName(), epithet);
				}
		}
	}
	
	private boolean hasEpithetAttributes(Annotation taxonName, OmniFAT.Rank[] ranks) {
		for (int r = 0; r < ranks.length; r++) {
			if (taxonName.hasAttribute(ranks[r].getName()))
				return true;
		}
		return false;
	}
	
	private void extractEpithetAttributes(MutableAnnotation taxonName, OmniFAT.DocumentDataSet docData) {
		System.out.println("Parsing " + taxonName.toXML());
		
		//	make sure epithets are annnotated, and source attribute set
		if (docData.getEpithets(taxonName).length != 0) {
			OmniFAT.Rank[] ranks = docData.omniFat.getRanks();
			
			//	try plainly setting attributes if not set (works if name already parsed)
			if (!this.hasEpithetAttributes(taxonName, ranks))
				OmniFatFunctions.setRankAttributes(taxonName, docData);
			
			//	check again if attributes set
			if (this.hasEpithetAttributes(taxonName, ranks)) {
				System.out.println("==> attributes extracted from epithets, we're done");
				
				//	remember secure epithets (required for name completion)
				OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
				for (int rg = 0; rg < rankGroups.length; rg++) {
					OmniFAT.Rank[] groupRanks = rankGroups[rg].getRanks();
					for (int r = 0; r < groupRanks.length; r++) {
						String epithet = ((String) taxonName.getAttribute(groupRanks[r].getName()));
						if ((epithet != null) && (epithet.length() > (docData.omniFat.getMaxAbbreviationLength() + (epithet.endsWith(".") ? 1 : 0))))
							docData.addSureEpithet(rankGroups[rg].getName(), epithet);
					}
				}
				
				return;
			}
		}
		
		//	no details annotated, tag candidates to obtain detail annotations
		MutableAnnotation tempTaxonName = Gamta.copyDocument(taxonName);
		System.out.println("==> parsing ...");
		
		OmniFatFunctions.tagBaseEpithets(tempTaxonName, docData);
		OmniFatFunctions.tagAuthorNames(tempTaxonName, docData);
		OmniFatFunctions.tagLabels(tempTaxonName, docData);
		
		OmniFatFunctions.tagEpithets(tempTaxonName, docData);
		OmniFatFunctions.tagCandidates(tempTaxonName, docData);
		
		//	copy detail annotations from first candidate spanning whole taxon name (if any)
		QueriableAnnotation[] taxonNameCandidates = tempTaxonName.getAnnotations(OmniFAT.TAXON_NAME_CANDIDATE_TYPE);
		System.out.println("  - got " + taxonNameCandidates.length + " candidate parses");
//		for (int c = 0; c < taxonNameCandidates.length; c++) {
//			System.out.println("    - " + taxonNameCandidates[c].toXML());
//			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
//			for (int e = 0; e < epithets.length; e++)
//				System.out.println("      - " + epithets[e].toXML());
//		}
		if (taxonNameCandidates.length == 0)
			return;
		
		QueriableAnnotation taxonNameCandidate = null;
		if (taxonNameCandidates[0].size() == taxonName.size())
			taxonNameCandidate = taxonNameCandidates[0];
		else for (int c = 0; c < taxonNameCandidates.length; c++) {
			Annotation[] epithets = docData.getEpithets(taxonNameCandidates[c]);
			String lastState = "";
			for (int e = 0; e < epithets.length; e++)
				lastState = ((String) epithets[e].getAttribute(OmniFAT.STATE_ATTRIBUTE));
			if (OmniFAT.POSITIVE_STATE.equals(lastState) || OmniFAT.LIKELY_STATE.equals(lastState)) {
				taxonNameCandidate = taxonNameCandidates[c];
				c = taxonNameCandidates.length;
			}
		}
		
		if (taxonNameCandidate != null) {
			taxonNameCandidate.changeTypeTo(OmniFAT.TAXONOMIC_NAME_TYPE);
			System.out.println("  - promoted covering candidate parse " + taxonNameCandidate.toXML());
			OmniFatFunctions.inferRanks(tempTaxonName, docData);
			System.out.println("  - ranks infered " + taxonNameCandidate.toXML());
//			OmniFatFunctions.completeNames(tempTaxonName, docData);
//			System.out.println("  - name completed " + taxonNameCandidate.toXML());
			
			System.out.println("  - transfering epithets ...");
			Annotation[] epithets = docData.getEpithets(taxonNameCandidate);
			StringBuffer source = new StringBuffer();
			for (int e = 0; e < epithets.length; e++) {
				Annotation epithet = taxonName.addAnnotation(OmniFAT.EPITHET_TYPE, epithets[e].getStartIndex(), epithets[e].size());
				epithet.copyAttributes(epithets[e]);
				source.append("," + epithet.getAnnotationID());
				System.out.println("    - " + epithet.toXML());
			}
			if (source.length() != 0)
				taxonName.setAttribute(OmniFAT.SOURCE_ATTRIBUTE, source.substring(1));
			
			//	extract attributes
			OmniFatFunctions.setRankAttributes(taxonName, docData);
			System.out.println("  - attributes extracted from epithets");
			System.out.println("  ==> parsed to " + taxonName.toXML());
			
			//	remember secure epithets (required for name completion)
			OmniFAT.RankGroup[] rankGroups = docData.omniFat.getRankGroups();
			for (int rg = 0; rg < rankGroups.length; rg++) {
				OmniFAT.Rank[] ranks = rankGroups[rg].getRanks();
				for (int r = 0; r < ranks.length; r++) {
					String epithet = ((String) taxonName.getAttribute(ranks[r].getName()));
					if ((epithet != null) && (epithet.length() > (docData.omniFat.getMaxAbbreviationLength() + (epithet.endsWith(".") ? 1 : 0))))
						docData.addSureEpithet(rankGroups[rg].getName(), epithet);
				}
			}
		}
	}
	
	private static class TaxonNameBucket implements Comparable {
		private String taxonNameString;
		private MutableAnnotation taxonName;
		private ArrayList taxonNames = new ArrayList();
		private TaxonNameBucket(MutableAnnotation taxonName, String taxonNameString) {
			this.taxonNameString = taxonNameString;
			this.taxonName = taxonName;
			this.taxonNames.add(taxonName);
		}
		public int compareTo(Object o) {
			if (o instanceof TaxonNameBucket) return this.taxonNameString.compareTo(((TaxonNameBucket) o).taxonNameString);
			else return -1;
		}
	}
	
	/**
	 * Feedback panel for taxonomic name parser. This class is public ans static
	 * so it can be class loaded remotely without a surrounding analyzer
	 * available (see also conventions of FeedbackPanel class)
	 * 
	 * @author sautter
	 */
	public static class TaxonomicNameParserFeedbackPanel extends FeedbackPanel {
		
		private String[] rankNames = new String[0];
		
		private String[] rankGroupNames = new String[0];
		private String[][] rankGroupRankNames = new String[0][0];
		private int[] rankGroupIndices = new int[0];
		
		static final int noMeaningIndex = -2;
		private static final int cutToIndex = -1;
		private int splitFromIndex;
		private int cutFromIndex;
		
		private int minRequiredIndex = Integer.MAX_VALUE;
		private int[] requiredIndices = new int[0];
		
		private Color getColor(int meaningIndex) {
			if (meaningIndex == noMeaningIndex)
				return Color.WHITE;
			else if (meaningIndex == cutToIndex)
				return Color.GRAY;
			else if (meaningIndex == splitFromIndex)
				return Color.GREEN;
			else if (meaningIndex == cutFromIndex)
				return Color.GRAY;
			else return new Color(255, ((192 * (meaningIndex+1)) / rankNames.length), 255);
		}
		
		private static final Color LIGHT_GREEN = new Color(128, 255, 128);
		private static final Color LIGHT_BLUE = new Color(128, 128, 255);
		
		private static final String[] emptyEpithetList = new String[0];
		private HashMap epithetLists = new HashMap();
		private String[] getEpithetList(String rankOrRankGroup) {
			String[] epithetList = ((String[]) this.epithetLists.get(rankOrRankGroup));
			return ((epithetList == null) ? emptyEpithetList : epithetList);
		}
		
		private ArrayList taxonNamePanels = new ArrayList();
		
		private Font font = new Font("Verdana", Font.PLAIN, 12);
		private GridBagConstraints gbc = new GridBagConstraints();
		
		public TaxonomicNameParserFeedbackPanel() {
			this.init();
		}
		
		TaxonomicNameParserFeedbackPanel(String[] rankGroupNames, String[][] rankGroupRankNames, String[][] rankGroupEpithetLists, String[] requiredRankNames) {
			super("Please Check Attributes of Taxon Names");
			this.rankGroupNames = new String[rankGroupNames.length];
			this.rankGroupRankNames = new String[rankGroupNames.length][];
			this.rankGroupIndices = new int[rankGroupNames.length];
			StringVector rankNames = new StringVector();
			for (int rg = 0; rg < rankGroupNames.length; rg++) {
				this.rankGroupNames[rg] = rankGroupNames[rg];
				this.rankGroupRankNames[rg] = new String[rankGroupRankNames[rg].length];
				this.rankGroupIndices[rg] = rankNames.size();
				this.epithetLists.put(rankGroupNames[rg], rankGroupEpithetLists[rg]);
				for (int r = 0; r < rankGroupRankNames[rg].length; r++) {
					rankNames.addElement(rankGroupRankNames[rg][r]);
					this.rankGroupRankNames[rg][r] = rankGroupRankNames[rg][r];
					this.epithetLists.put(rankGroupRankNames[rg][r], rankGroupEpithetLists[rg]);
				}
			}
			this.rankNames = rankNames.toStringArray();
			
			this.requiredIndices = new int[requiredRankNames.length];
			for (int i = 0; i < this.requiredIndices.length; i++) {
				for (int r = 0; r < this.rankNames.length; r++)
					if (requiredRankNames[i].equals(this.rankNames[r])) {
						this.requiredIndices[i] = r;
						if (this.minRequiredIndex == Integer.MAX_VALUE)
							this.minRequiredIndex = r;
						r = this.rankNames.length;
					}
			}
			
			this.splitFromIndex = this.rankNames.length;
			this.cutFromIndex = this.rankNames.length + 1;
			
			this.init();
		}
		
		private void init() {
			this.setLayout(new GridBagLayout());
			this.gbc.insets.top = 2;
			this.gbc.insets.bottom = 2;
			this.gbc.insets.left = 5;
			this.gbc.insets.right = 5;
			this.gbc.fill = GridBagConstraints.HORIZONTAL;
			this.gbc.weightx = 1;
			this.gbc.weighty = 0;
			this.gbc.gridwidth = 1;
			this.gbc.gridheight = 1;
			this.gbc.gridx = 0;
			this.gbc.gridy = 0;
		}
		
		void addTaxonName(Annotation taxonName, Annotation[] taxonNameOccurrences, MutableAnnotation doc) {
			
			//	get name tokens
			String[] nameTokens = new String[taxonName.size()];
			Set nameTokenSet = new HashSet();
			for (int t = 0; t < taxonName.size(); t++) {
				nameTokens[t] = taxonName.valueAt(t);
				nameTokenSet.add(taxonName.valueAt(t));
			}
			
			//	get token meanings
			int[] nameTokenMeanings = new int[taxonName.size()];
			for (int m = 0; m < nameTokenMeanings.length; m++)
				nameTokenMeanings[m] = noMeaningIndex;
			
			/*
			 * find meaningful parts in tokens - going backward from the last
			 * token ensures that no false assignments are made (eg species
			 * author mistaken for genus) because if any parts are missing or
			 * abbreviated, it's the higher level parts, not the most specific
			 * ones.
			 */
			int lastMeaningfulPartIndex = nameTokens.length;
			for (int p = (rankNames.length - 1); p >= 0; p--) {
				String partValue = ((String) taxonName.getAttribute(rankNames[p]));
				
				//	if attribute present, find value in tokens
				if (partValue != null) {
					for (int t = (lastMeaningfulPartIndex - 1); t >= 0; t--) {
						
						//	found matching token
						if (nameTokens[t].equals(partValue)) {
							nameTokenMeanings[t] = p;
							nameTokenSet.remove(nameTokens[t]);
							lastMeaningfulPartIndex = t;
							t = -1;
						}
						
						//	found possible abbreviation, and part not explicitly present
						else if (partValue.startsWith(nameTokens[t]) && !nameTokenSet.contains(partValue)) {
							nameTokenMeanings[t] = p;
							nameTokenSet.remove(nameTokens[t]);
							lastMeaningfulPartIndex = t;
							t = -1;
						}
					}
				}
			}

			
			//	get attribute strings
			String[] meaningfulPartStrings = new String[rankNames.length];
			for (int p = 0; p < rankNames.length; p++) {
				String partAttribute = taxonName.getAttribute(rankNames[p], "").toString();
				meaningfulPartStrings[p] = ((partAttribute.length() == 0) ? null : partAttribute);
			}
			
			
			//	get name context(s)
			StringBuffer nameContext = new StringBuffer("<HTML>");
			for (int o = 0; o < taxonNameOccurrences.length; o++) {
				nameContext.append(((o == 0) ? "" : "<BR>") + this.buildLabel(doc, taxonNameOccurrences[o], 10));
				if (taxonNameOccurrences[o].hasAttribute(PAGE_NUMBER_ATTRIBUTE))
					nameContext.append(" (page " + taxonNameOccurrences[o].getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")");
			}
			nameContext.append("</HTML>");
			
			this.addTaxonName(nameTokens, nameTokenMeanings, meaningfulPartStrings, nameContext.toString());
		}
		
		void addTaxonName(String[] nameTokens, int[] nameTokenMeanings, String[] meaningfulPartStrings, String nameContext) {
			TaxonomicNameParsePanel tnpp = new TaxonomicNameParsePanel(nameTokens, nameTokenMeanings, meaningfulPartStrings, nameContext);
			this.gbc.gridy = this.taxonNamePanels.size();
			this.add(tnpp, this.gbc.clone());
			this.taxonNamePanels.add(tnpp);
		}
		
		String buildLabel(TokenSequence text, Annotation annot, int envSize) {
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
		
		int getTaxonNameCount() {
			return this.taxonNamePanels.size();
		}
		
		boolean removeTaxonNameAt(int taxonName) {
			return ((TaxonomicNameParsePanel) this.taxonNamePanels.get(taxonName)).remove.isSelected();
		}
		
		int getMeaningIndexAt(int taxonName, int tokenIndex) {
			return ((TaxonomicNameParsePanel) this.taxonNamePanels.get(taxonName)).taxonNameTokenMeanings[tokenIndex];
		}
		
		String getMeaningfulPartAt(int taxonName, int meaningIndex) {
			return ((TaxonomicNameParsePanel) this.taxonNamePanels.get(taxonName)).meaningfulPartStrings[meaningIndex];
		}
		
		String getRankAt(int taxonName) {
			return ((TaxonomicNameParsePanel) this.taxonNamePanels.get(taxonName)).rank;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			
			//	read parent class data
			super.initFields(br);
			
			//	read font settings
			this.font = new Font(br.readLine(), Font.PLAIN, Integer.parseInt(br.readLine()));
			
			//	read rank groups
			String rankGroupLine = br.readLine().trim();
			this.rankGroupNames = rankGroupLine.split("\\s++");
			this.rankGroupRankNames = new String[this.rankGroupNames.length][];
			this.rankGroupIndices = new int[this.rankGroupNames.length];
			
			//	read required ranks
			String requiredRankString = br.readLine().trim();
			String[] requiredRankNames = requiredRankString.split("\\s++");
			if (requiredRankNames[0].length() == 0)
				requiredRankNames = new String[0];
			
			//	read rank group data
			StringVector rankNames = new StringVector();
			for (int rg = 0; rg < this.rankGroupNames.length; rg++) {
				
				//	compute min index of current rank group
				this.rankGroupIndices[rg] = rankNames.size();
				
//				//	burn rank group name
//				br.readLine();
				
				//	read ranks
				String rankLine = br.readLine().trim();
				this.rankGroupRankNames[rg] = rankLine.split("\\s++");
				
				//	read epithet list
				StringVector dataCollector = new StringVector();
				String dataLine;
				while (((dataLine = br.readLine()) != null) && (dataLine.length() != 0))
					dataCollector.addElement(dataLine);
				String[] epithetList = dataCollector.toStringArray();
				
				//	store in local data structures
				this.epithetLists.put(this.rankGroupNames[rg], epithetList);
				for (int r = 0; r < this.rankGroupRankNames[rg].length; r++) {
					rankNames.addElement(this.rankGroupRankNames[rg][r]);
					this.epithetLists.put(rankGroupRankNames[rg][r], epithetList);
				}
			}
			this.rankNames = rankNames.toStringArray();
			
			this.requiredIndices = new int[requiredRankNames.length];
			this.minRequiredIndex = Integer.MAX_VALUE;
			for (int i = 0; i < this.requiredIndices.length; i++) {
				for (int r = 0; r < this.rankNames.length; r++)
					if (requiredRankNames[i].equals(this.rankNames[r])) {
						this.requiredIndices[i] = r;
						if (this.minRequiredIndex == Integer.MAX_VALUE)
							this.minRequiredIndex = r;
						r = this.rankNames.length;
					}
			}
			
			this.splitFromIndex = this.rankNames.length;
			this.cutFromIndex = this.rankNames.length + 1;
			
			//	prepare for reading name panels
			StringVector dataCollector = new StringVector();
			String dataLine;
			
			//	read panels
			Iterator tnppit = this.taxonNamePanels.iterator();
			while (((dataLine = br.readLine()) != null) && (dataLine.length() != 0)) {
				
				String nameContext = URLDecoder.decode(dataLine, "UTF-8");
				String[] meaningfulPartStrings = br.readLine().split("\\s", -1);
				for (int m = 0; m < meaningfulPartStrings.length; m++)
					if (meaningfulPartStrings[m].length() == 0)
						meaningfulPartStrings[m] = null;
				String rank = br.readLine();
				if (rank.length() == 0) rank = null;
				boolean remove = "R".equals(br.readLine());
				while (((dataLine = br.readLine()) != null) && (dataLine.length() != 0))
					dataCollector.addElement(dataLine);
				
				String[] nameTokens = new String[dataCollector.size()];
				int[] nameTokenMeanings = new int[dataCollector.size()];
				for (int t = 0; t < dataCollector.size(); t++) {
					String[] dataParts = dataCollector.get(t).split("\\s");
					nameTokens[t] = dataParts[0];
					nameTokenMeanings[t] = Integer.parseInt(dataParts[1]);
				}
				dataCollector.clear();
				
				//	got status of existing name panel
				if ((tnppit != null) && tnppit.hasNext()) {
					TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) tnppit.next());
					for (int t = 0; t < nameTokenMeanings.length; t++)
						tnpp.taxonNameTokenMeanings[t] = nameTokenMeanings[t];
//					tnpp.ensureMeaningfulPartOrder(0);
//					for (int m = 0; m < meaningfulPartStrings.length; m++)
//						tnpp.meaningfulPartStrings[m] = meaningfulPartStrings[m];
//					tnpp.rank = rank;
					for (int m = 0; m < meaningfulPartStrings.length; m++) {
						tnpp.meaningfulPartStrings[m] = meaningfulPartStrings[m];
						tnpp.meaningfulPartStringAttributes[m] = meaningfulPartStrings[m]; // TODO check if this works
					}
					tnpp.rank = rank;
					tnpp.ensureMeaningfulPartOrder(0);
					tnpp.remove.setSelected(remove);
				}
				
				//	got new name panel
				else {
					this.addTaxonName(nameTokens, nameTokenMeanings, meaningfulPartStrings, nameContext);
					tnppit = null;
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			//	write parent class data
			super.writeData(bw);
			
			//	write font settings
			bw.write(this.font.getFamily());
			bw.newLine();
			bw.write("" + this.font.getSize());
			bw.newLine();
			
			//	write rank group names
			for (int rg = 0; rg < this.rankGroupNames.length; rg++)
				bw.write(((rg == 0) ? "" : " ") + this.rankGroupNames[rg]);
			bw.newLine();
			
			//	write required ranks
			for (int i = 0; i < this.requiredIndices.length; i++)
				bw.write(((i == 0) ? "" : " ") + this.rankNames[this.requiredIndices[i]]);
			bw.newLine();
			
			//	write rank group data
			for (int rg = 0; rg < this.rankGroupNames.length; rg++) {
				
//				//	write rank group
//				bw.write(this.rankGroupNames[rg]);
//				bw.newLine();
				
				//	write ranks
				for (int r = 0; r < this.rankGroupRankNames[rg].length; r++)
					bw.write(((r == 0) ? "" : " ") + this.rankGroupRankNames[rg][r]);
				bw.newLine();
				
				//	write epithet list
				String[] rankGroupEpithetList = this.getEpithetList(this.rankGroupNames[rg]);
				for (int e = 0; e < rankGroupEpithetList.length; e++) {
					bw.write(rankGroupEpithetList[e]);
					bw.newLine();
				}
				
				//	write separator
				bw.newLine();
			}
			
			//	write panels
			for (int p = 0; p < this.taxonNamePanels.size(); p++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(p));
				
				//	send context
				bw.write(URLEncoder.encode(tnpp.taxonNameContext, "UTF-8"));
				bw.newLine();
				
				//	send meaningful part strings
				StringBuffer mps = new StringBuffer();
				for (int m = 0; m < tnpp.meaningfulPartStrings.length; m++)
					mps.append(((m == 0) ? "" : " ") + ((tnpp.meaningfulPartStrings[m] == null) ? "" : tnpp.meaningfulPartStrings[m]));
				bw.write(mps.toString());
				bw.newLine();
				
				//	send rank
				bw.write((tnpp.rank == null) ? "" : tnpp.rank);
				bw.newLine();
				
				//	send remove status
				bw.write(tnpp.remove.isSelected() ? "R" : "K");
				bw.newLine();
				
				//	send tokens & meanings
				for (int t = 0; t < tnpp.taxonNameTokens.length; t++) {
					bw.write(tnpp.taxonNameTokens[t]);
					bw.write(" " + tnpp.taxonNameTokenMeanings[t]);
					bw.newLine();
				}
				bw.newLine();
			}
			
			bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			Properties fs = new Properties();
			for (int n = 0; n < this.taxonNamePanels.size(); n++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(n));
				fs.setProperty(("name" + n + "_remove"), (tnpp.remove.isSelected() ? "R" : "K"));
				for (int t = 0; t < tnpp.taxonNameTokens.length; t++)
					fs.setProperty(("name" + n + "_token" + t + "_meaning"), ("" + tnpp.taxonNameTokenMeanings[t]));
				for (int p = 0; p < rankNames.length; p++) {
					if (tnpp.meaningfulPartStrings[p] != null)
						fs.setProperty("name" + n + "_meaning" + p + "_string", tnpp.meaningfulPartStrings[p]);
				}
			}
			return fs;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			for (int n = 0; n < this.taxonNamePanels.size(); n++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(n));
				tnpp.remove.setSelected("R".equals(states.getProperty(("name" + n + "_remove"), "K")));
				for (int t = 0; t < tnpp.taxonNameTokens.length; t++)
					tnpp.taxonNameTokenMeanings[t] = Integer.parseInt(states.getProperty(("name" + n + "_token" + t + "_meaning"), ("" + noMeaningIndex)));
				for (int p = 0; p < rankNames.length; p++) {
					tnpp.meaningfulPartStrings[p] = states.getProperty("name" + n + "_meaning" + p + "_string");
					if ("".equals(tnpp.meaningfulPartStrings[p]))
						tnpp.meaningfulPartStrings[p] = null;
//					if (tnpp.meaningfulPartStrings[p] != null)
//						tnpp.rank = rankNames[p];
					if (tnpp.meaningfulPartStrings[p] != null) {
						tnpp.meaningfulPartStringAttributes[p] = tnpp.meaningfulPartStrings[p]; // TODO check if this works
						tnpp.rank = rankNames[p];
					}
				}
				tnpp.ensureMeaningfulPartOrder(0);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return (this.taxonNamePanels.size() * rankNames.length);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return rankNames.length;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.taxonNamePanels.size();
		}
		
		class TaxonomicNameParsePanel extends JPanel {
			
			JPanel meaningfulPartPanel = new JPanel(new GridBagLayout()) {
				public Dimension getPreferredSize() {
					int height = 0;
					for (int p = 0; p < activeMeaningfulParts.length; p++) {
						
						//	meaning given
						if (activeMeaningfulParts[p] != null)
							height += Math.max(activeMeaningfulParts[p].partLabel.getPreferredSize().height, activeMeaningfulParts[p].partOptions.getPreferredSize().height);
					}
					return new Dimension(this.getWidth(), (height + (this.getComponentCount() * 2)));
				}
			};
			
			MeaningfulPart[] meaningfulParts = new MeaningfulPart[rankNames.length];
			MeaningfulPart[] activeMeaningfulParts = new MeaningfulPart[rankNames.length];
			
			String[] meaningfulPartStringAttributes = new String[rankNames.length];
			String[] meaningfulPartStrings = new String[rankNames.length];
			String rank = null;
			
			int minMeaningfulPart = rankNames.length;
			int maxMeaningfulPart = 0;
			
			
			JPanel labelPanel = new JPanel(new BorderLayout());
			JLabel[] labels;
			TokenLabel[] tokenLabels;
			
			String[] taxonNameTokens;
			int[] taxonNameTokenMeanings;
			
			String taxonNameString;
			String taxonNameContext;
			
			JCheckBox remove = new JCheckBox("Remove");
			
			TaxonomicNameParsePanel(String[] nameTokens, int[] nameTokenMeanings, String[] meaningfulPartStrings, String nameContext) {
				super(new BorderLayout());
				
				this.taxonNameTokens = nameTokens;
				this.taxonNameTokenMeanings = new int[this.taxonNameTokens.length];
				
				for (int p = 0; p < rankNames.length; p++) {
					this.meaningfulPartStrings[p] = ((p < meaningfulPartStrings.length) ? meaningfulPartStrings[p] : null);
					this.meaningfulPartStringAttributes[p] = ((p < meaningfulPartStrings.length) ? meaningfulPartStrings[p] : null);
					if (this.meaningfulPartStrings[p] != null)
						this.rank = rankNames[p];
				}
				
				this.taxonNameContext = nameContext;
				
				final JPanel tnp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
					public Dimension getPreferredSize() {
						Dimension size = this.getSize();
						int lHeight = labels[0].getPreferredSize().height;
						int rows = 1;
						int rowLength = 0;
						for (int l = 0; l < labels.length; l++) {
							int lWidth = labels[l].getPreferredSize().width;
							if ((rowLength + lWidth) <= size.width)
								rowLength += lWidth;
							else {
								rows++;
								rowLength = lWidth;
							}
						}
						int height = lHeight * rows;
						return new Dimension(size.width, height);
					}
				};
				final int[] widths = new int[(this.taxonNameTokens.length * 2) - 1];
				
				StringBuffer nameString = new StringBuffer();
				ArrayList labelList = new ArrayList();
				this.tokenLabels = new TokenLabel[this.taxonNameTokens.length];
				for (int t = 0; t < this.taxonNameTokens.length; t++) {
					
					//	add space if required
					if (t != 0) {
						String space = (Gamta.insertSpace(taxonNameTokens[t-1], taxonNameTokens[t]) ? " " : "");
						nameString.append(space);
						JLabel sl = new JLabel(space);
						sl.setFont(font);
						sl.setBorder(null);
						sl.setOpaque(true);
						sl.setBackground(Color.WHITE);
						widths[((t-1) * 2) + 1] = sl.getPreferredSize().width;
						labelList.add(sl);
						tnp.add(sl);
					}
					
					//	add current text token
					nameString.append(taxonNameTokens[t]);
					final TokenLabel tl = new TokenLabel(taxonNameTokens[t]);
					final boolean tlIsWord = Gamta.isWord(taxonNameTokens[t]);
					
					//	gather layout information
					widths[t * 2] = tl.getPreferredSize().width;
					tnp.add(tl);
					
					//	react to clicks
					tl.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent me) {
							JPopupMenu pm = new JPopupMenu();
							JMenuItem mi = null;
							
							//	add remove option for cutting points
							if ((taxonNameTokenMeanings[tl.tokenIndex] == cutToIndex) || (taxonNameTokenMeanings[tl.tokenIndex] == cutFromIndex)) {
								mi = new JMenuItem("Do Not Cut");
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										setTokenMeaningIndex(tl.tokenIndex, noMeaningIndex);
									}
								});
								pm.add(mi);
							}
							
							//	add cutting point option
							else {
								mi = new JMenuItem("Cut Up To Here");
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										setTokenMeaningIndex(tl.tokenIndex, cutToIndex);
									}
								});
								pm.add(mi);
								
								mi = new JMenuItem("Cut From Here");
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										setTokenMeaningIndex(tl.tokenIndex, cutFromIndex);
									}
								});
								pm.add(mi);
							}
							
							//	add remove option for splitting point
							if (taxonNameTokenMeanings[tl.tokenIndex] == splitFromIndex) {
								mi = new JMenuItem("Do Not Split");
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										setTokenMeaningIndex(tl.tokenIndex, noMeaningIndex);
									}
								});
								pm.add(mi);
							}
							
							//	add splitting point option
							else {
								mi = new JMenuItem("Split From Here");
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										setTokenMeaningIndex(tl.tokenIndex, splitFromIndex);
									}
								});
								pm.add(mi);
							}
							
							//	add remove option only if meaning assigned
							if ((taxonNameTokenMeanings[tl.tokenIndex] >= 0) && (taxonNameTokenMeanings[tl.tokenIndex] < rankNames.length)) {
								mi = new JMenuItem("No Meaning");
								mi.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ae) {
										setTokenMeaningIndex(tl.tokenIndex, noMeaningIndex);
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
								for (int p = 0; p < rankNames.length; p++) {
									if (p != taxonNameTokenMeanings[tl.tokenIndex]) {
										final String taxonNamePart = rankNames[p];
										final int taxonNamePartIndex = p;
										mi = new JMenuItem(taxonNamePart);
										mi.addActionListener(new ActionListener() {
											public void actionPerformed(ActionEvent ae) {
												setTokenMeaningIndex(tl.tokenIndex, taxonNamePartIndex);
											}
										});
										pm.add(mi);
									}
								}
							}
							
							pm.show(tl, me.getX(), me.getY());
						}
						public void mouseEntered(MouseEvent me) {
							tl.setBackground(LIGHT_BLUE);
						}
						public void mouseExited(MouseEvent me) {
							tl.setBackground(tl.color);
						}
					});
					
					//	preset meaning
					this.taxonNameTokenMeanings[t] = ((t < nameTokenMeanings.length) ? nameTokenMeanings[t] : noMeaningIndex);
					
					//	store token
					tl.tokenIndex = t;
					this.tokenLabels[t] = tl;
					labelList.add(tl);
				}
				
				//	store name string for context display
				this.taxonNameString = nameString.toString();
				this.labels = ((JLabel[]) labelList.toArray(new JLabel[labelList.size()]));
				
				
				//	initialize functions
				JButton context = new JButton("View Name Context");
				context.setBorder(BorderFactory.createRaisedBevelBorder());
				context.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						JOptionPane.showMessageDialog(TaxonomicNameParsePanel.this, taxonNameContext, ("Context of '" + taxonNameString + "'"), JOptionPane.INFORMATION_MESSAGE);
					}
				});
				context.setPreferredSize(new Dimension(context.getPreferredSize().width, 21));
				
				JPanel functionPanel = new JPanel(new BorderLayout());
				functionPanel.add(context, BorderLayout.CENTER);
				functionPanel.add(this.remove, BorderLayout.EAST);
				
				
				//	put token panel in frame
				this.labelPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
				this.labelPanel.add(tnp, BorderLayout.CENTER);
				
				//	initialize meaningful part panels
				for (int p = 0; p < rankNames.length; p++)
					this.meaningfulParts[p] = new MeaningfulPart(p);
				
				
				this.setBorder(BorderFactory.createEtchedBorder());
				
				this.add(functionPanel, BorderLayout.NORTH);
				this.add(this.labelPanel, BorderLayout.CENTER);
				this.add(this.meaningfulPartPanel, BorderLayout.SOUTH);
				
				this.ensureMeaningfulPartOrder(0);
			}
			
			void setTokenMeaningIndex(int tokenIndex, int meaningIndex) {
				this.taxonNameTokenMeanings[tokenIndex] = meaningIndex;
				this.ensureMeaningfulPartOrder(tokenIndex);
			}
			
			void ensureMeaningfulPartOrder(int anchorIndex) {
				if (this.taxonNameTokenMeanings[anchorIndex] != noMeaningIndex) {
					int lastMeaningIndex;
					
					lastMeaningIndex = this.taxonNameTokenMeanings[anchorIndex];
					for (int t = anchorIndex-1; t >= 0; t--)
						if (this.taxonNameTokenMeanings[t] != noMeaningIndex) {
							if (this.taxonNameTokenMeanings[t] < lastMeaningIndex)
								lastMeaningIndex = this.taxonNameTokenMeanings[t];
							else this.taxonNameTokenMeanings[t] = noMeaningIndex;
						}
					
					lastMeaningIndex = this.taxonNameTokenMeanings[anchorIndex];
					for (int t = anchorIndex+1; t < this.taxonNameTokenMeanings.length; t++)
						if (this.taxonNameTokenMeanings[t] != noMeaningIndex) {
							if (this.taxonNameTokenMeanings[t] > lastMeaningIndex)
								lastMeaningIndex = this.taxonNameTokenMeanings[t];
							else this.taxonNameTokenMeanings[t] = noMeaningIndex;
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
						Color tlc = getColor(this.taxonNameTokenMeanings[tl.tokenIndex]);
						tl.setBackground(tlc);
						tl.color = tlc;
						if (this.taxonNameTokenMeanings[tl.tokenIndex] == cutToIndex)
							cutTo = l;
						else if (this.taxonNameTokenMeanings[tl.tokenIndex] == splitFromIndex)
							splitFrom = l;
						else if (this.taxonNameTokenMeanings[tl.tokenIndex] == cutFromIndex)
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
				Arrays.fill(this.meaningfulPartStrings, null);
				this.rank = null;
				Arrays.fill(this.activeMeaningfulParts, null);
				this.minMeaningfulPart = rankNames.length;
				this.maxMeaningfulPart = 0;
				
				for (int t = 0; t < this.tokenLabels.length; t++)
					if ((this.taxonNameTokenMeanings[t] >= 0) && (this.taxonNameTokenMeanings[t] < rankNames.length)) {
						this.minMeaningfulPart = Math.min(this.minMeaningfulPart, this.taxonNameTokenMeanings[t]);
						this.maxMeaningfulPart = Math.max(this.maxMeaningfulPart, this.taxonNameTokenMeanings[t]);
						this.activeMeaningfulParts[this.taxonNameTokenMeanings[t]] = this.meaningfulParts[this.taxonNameTokenMeanings[t]];
//						this.activeMeaningfulParts[this.taxonNameTokenMeanings[t]].setPartString(this.taxonNameTokens[t]);
						if ((this.taxonNameTokens[t].length() < 3) && (this.meaningfulPartStringAttributes.length > this.taxonNameTokenMeanings[t]) && (this.meaningfulPartStringAttributes[this.taxonNameTokenMeanings[t]] != null) && this.meaningfulPartStringAttributes[this.taxonNameTokenMeanings[t]].startsWith(this.taxonNameTokens[t]))
							this.activeMeaningfulParts[this.taxonNameTokenMeanings[t]].setPartString(this.meaningfulPartStringAttributes[this.taxonNameTokenMeanings[t]]); // TODO check out if this works
						else this.activeMeaningfulParts[this.taxonNameTokenMeanings[t]].setPartString(this.taxonNameTokens[t]);
					}
				
				if (this.minMeaningfulPart > minRequiredIndex) {
					this.minMeaningfulPart = minRequiredIndex;
					this.activeMeaningfulParts[this.minMeaningfulPart] = this.meaningfulParts[this.minMeaningfulPart];
					this.activeMeaningfulParts[this.minMeaningfulPart].setPartString((this.meaningfulPartStringAttributes[this.minMeaningfulPart] == null) ? "" : this.meaningfulPartStringAttributes[this.minMeaningfulPart]);
				}
				
				for (int r = 0; r < requiredIndices.length; r++) {
					if ((this.maxMeaningfulPart > requiredIndices[r]) && (this.activeMeaningfulParts[requiredIndices[r]] == null)) {
						this.activeMeaningfulParts[requiredIndices[r]] = this.meaningfulParts[requiredIndices[r]];
						this.activeMeaningfulParts[requiredIndices[r]].setPartString((this.meaningfulPartStringAttributes[requiredIndices[r]] == null) ? "" : this.meaningfulPartStringAttributes[requiredIndices[r]]);
					}
				}
				
				for (int p = 0; p < rankNames.length; p++) {
					
					//	meaning not given, clear
					if (this.activeMeaningfulParts[p] == null)
						this.meaningfulPartStrings[p] = null;
					
					//	meaning given
					else {
						this.meaningfulPartStrings[p] = this.activeMeaningfulParts[p].getPartString();
						this.rank = rankNames[p];
					}
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
				
				int startMeaningIndex = Math.min(this.minMeaningfulPart, minRequiredIndex);
				int endMeaningIndex = Math.min(this.maxMeaningfulPart, (rankNames.length - 1));
				
				for (int p = startMeaningIndex; p <= endMeaningIndex; p++) {
					
					//	meaning given
					if (this.activeMeaningfulParts[p] != null) {
						gbc.gridx = 0;
						gbc.weightx = 0;
						this.meaningfulPartPanel.add(this.activeMeaningfulParts[p].partLabel, gbc.clone());
						gbc.gridx = 1;
						gbc.weightx = 1;
						this.meaningfulPartPanel.add(this.activeMeaningfulParts[p].partOptions, gbc.clone());
						
						gbc.gridy ++;
					}
				}
				this.meaningfulPartPanel.validate();
				this.updateUI();
			}
			
			class TokenLabel extends JLabel {
				int tokenIndex;
				Color color = Color.WHITE;
				
				TokenLabel(String text) {
					super(text, CENTER);
					this.setBorder(null);
					this.setOpaque(true);
					this.setFont(font);
					this.setBackground(this.color);
				}
			}
			
			class MeaningfulPart {
				int meaningIndex;
				JLabel partLabel;
				JComboBox partOptions;
				
				MeaningfulPart(int mi) {
					this.meaningIndex = mi;
					
					String meaning = rankNames[this.meaningIndex];
					this.partLabel = new JLabel("<HTML><B>" + meaning.substring(0, 1).toUpperCase() + meaning.substring(1) + ":</B> </HTML>");
					
					String[] options = new String[0];
					for (int rg = 0; rg < rankGroupIndices.length; rg++) {
						if (this.meaningIndex >= rankGroupIndices[rg])
							options = getEpithetList(rankGroupNames[rg]);
						else rg = rankGroupIndices.length;
					}
					this.partOptions = new JComboBox(options);
					this.partOptions.setEditable(true);
//					this.partOptions.setPreferredSize(new Dimension(100, 21));
					this.partOptions.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent ie) {
							meaningfulPartStrings[meaningIndex] = getPartString();
							meaningfulPartStringAttributes[meaningIndex] = getPartString(); // TODO check if this works
						}
					});
				}
				
				void setPartString(String part) {
					if ((part.length() > 1) && Gamta.isUpperCaseWord(part))
						part = (part.substring(0, 1) + part.substring(1).toLowerCase());
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
				
				String getPartString() {
					Object selectedOption = this.partOptions.getSelectedItem();
					return ((selectedOption == null) ? null : selectedOption.toString());
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			bw.writeLine("initContext();");
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			bw.writeLine("var _replaceContext = false;    // replace the system context menu?");
			bw.writeLine("var _mouseOverContext = false;    // is the mouse over the context menu?");
			bw.writeLine("var _divContext;  // makes my life easier");
			bw.writeLine("");
			bw.writeLine("var _colors = new Object(); // maps meaning indices to colors");
			bw.writeLine("");
			bw.writeLine("");
			bw.writeLine("var tokenColors = new Object(); // required to restore colors after roll-over effect");
			bw.writeLine("var meaningfulPartStrings = new Object(); // holds the preset part values from name completer");
			bw.writeLine("");
			bw.writeLine("function initContext() {");
			bw.writeLine("  _divContext = $$('divContext');");
			bw.writeLine("  ");
			bw.writeLine("  _divContext.onmouseover = function() { _mouseOverContext = true; };");
			bw.writeLine("  _divContext.onmouseout = function() { _mouseOverContext = false; };");
			bw.writeLine("  ");
			bw.writeLine("  document.body.onmousedown = contextMouseDown;");
			bw.writeLine("  document.body.oncontextmenu = contextShow;");
			bw.writeLine("  ");
			bw.writeLine("  _colors['" + noMeaningIndex + "'] = '" + getRGB(getColor(noMeaningIndex)) + "';");
			bw.writeLine("  _colors['" + cutToIndex + "'] = '" + getRGB(getColor(cutToIndex)) + "';");
			for (int p = 0; p < this.rankNames.length; p++)
				bw.writeLine("  _colors['" + p + "'] = '" + getRGB(getColor(p)) + "';");
			bw.writeLine("  _colors['" + this.splitFromIndex + "'] = '" + getRGB(getColor(splitFromIndex)) + "';");
			bw.writeLine("  _colors['" + this.cutFromIndex + "'] = '" + getRGB(getColor(cutFromIndex)) + "';");
			bw.writeLine("  ");
			for (int n = 0; n < this.taxonNamePanels.size(); n++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(n));
				for (int p = 0; p < tnpp.meaningfulPartStringAttributes.length; p++)
					bw.writeLine("  meaningfulPartStrings['name" + n + "_" + p + "'] = '" + ((tnpp.meaningfulPartStringAttributes[p] == null) ? "" : tnpp.meaningfulPartStringAttributes[p]) + "';");
				bw.writeLine("  ");
			}
			bw.writeLine("  var name = 0;");
			bw.writeLine("  while($$('name' + name + '_token0')) {");
			bw.writeLine("    _clickedName = name;");
			bw.writeLine("    adjustColors();");
			bw.writeLine("    adjustName();");
			bw.writeLine("    _clickedName = null;");
			bw.writeLine("    name++;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function adjustColors() {");
			bw.writeLine("  ");
			bw.writeLine("  var cutTo = -1;");
			bw.writeLine("  var cutFrom = -1;");
			bw.writeLine("  var splitFrom = -1;");
			bw.writeLine("  ");
			bw.writeLine("  var t = 0;");
			bw.writeLine("  var token = $('token' + 0);");
			bw.writeLine("  while (token) {");
			bw.writeLine("    var meaning = getTokenMeaning(t);");
			bw.writeLine("    if (_colors[meaning]) {");
			bw.writeLine("      token.style.backgroundColor = _colors[meaning];");
			bw.writeLine("      tokenColors['name' + _clickedName + '_token' + t] = _colors[meaning];");
			bw.writeLine("      var space = $('space' + t);");
			bw.writeLine("      if (space)");
			bw.writeLine("        space.style.backgroundColor = 'FFFFFF';");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    if (meaning == " + cutToIndex + ") {");
			bw.writeLine("      cutTo = t;");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    else if (meaning == " + splitFromIndex + ") {");
			bw.writeLine("      splitFrom = t;");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    else if (meaning == " + cutFromIndex + ") {");
			bw.writeLine("      cutFrom = t;");
			bw.writeLine("    }");
			bw.writeLine("    t++;");
			bw.writeLine("    token = $('token' + t);");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  if (cutTo != -1) {");
			bw.writeLine("    for (var c = cutTo; c >= 0; c--) {");
			bw.writeLine("      var cToken = $('token' + c);");
			bw.writeLine("      if (cToken && (c != cutTo)) {");
			bw.writeLine("        cToken.style.backgroundColor = '" + getRGB(Color.LIGHT_GRAY) + "';");
			bw.writeLine("        tokenColors['name' + _clickedName + '_token' + c] = '" + getRGB(Color.LIGHT_GRAY) + "';");
			bw.writeLine("      }");
			bw.writeLine("      var cSpace = $('space' + c);");
			bw.writeLine("      if (cSpace)");
			bw.writeLine("        cSpace.style.backgroundColor = '" + getRGB(Color.LIGHT_GRAY) + "';");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  if (splitFrom != -1) {");
			bw.writeLine("    for (var s = (splitFrom+1); $('token' + s); s++) {");
			bw.writeLine("      var sToken = $('token' + s);");
			bw.writeLine("      if (sToken) {");
			bw.writeLine("        sToken.style.backgroundColor = '" + getRGB(LIGHT_GREEN) + "';");
			bw.writeLine("        tokenColors['name' + _clickedName + '_token' + s] = '" + getRGB(LIGHT_GREEN) + "';");
			bw.writeLine("      }");
			bw.writeLine("      var sSpace = $('space' + s);");
			bw.writeLine("      if (sSpace)");
			bw.writeLine("        sSpace.style.backgroundColor = '" + getRGB(LIGHT_GREEN) + "';");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  if (cutFrom != -1) {");
			bw.writeLine("    for (var c = (cutFrom+1); $('token' + c); c++) {");
			bw.writeLine("      var cToken = $('token' + c);");
			bw.writeLine("      if (cToken) {");
			bw.writeLine("        cToken.style.backgroundColor = '" + getRGB(Color.LIGHT_GRAY) + "';");
			bw.writeLine("        tokenColors['name' + _clickedName + '_token' + c] = '" + getRGB(Color.LIGHT_GRAY) + "';");
			bw.writeLine("      }");
			bw.writeLine("      var cSpace = $('space' + c);");
			bw.writeLine("      if (cSpace)");
			bw.writeLine("        cSpace.style.backgroundColor = '" + getRGB(Color.LIGHT_GRAY) + "';");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function adjustContext() {");
			bw.writeLine("  ");
			bw.writeLine("  var meaning = getTokenMeaning(_clickedToken);");
			bw.writeLine("  ");
			bw.writeLine("  $$('noCut').style.display = (((meaning == " + cutToIndex + ") || (meaning == " + cutFromIndex + ")) ? '' : 'none');");
			bw.writeLine("  $$('cutTo').style.display = (((meaning == " + cutToIndex + ") || (meaning == " + cutFromIndex + ")) ? 'none' : '');");
			bw.writeLine("  $$('cutFrom').style.display = (((meaning == " + cutToIndex + ") || (meaning == " + cutFromIndex + ")) ? 'none' : '');");
			bw.writeLine("  ");
			bw.writeLine("  $$('split').style.display = ((meaning == " + splitFromIndex + ") ? 'none' : '');");
			bw.writeLine("  $$('noSplit').style.display = ((meaning == " + splitFromIndex + ") ? '' : 'none');");
			bw.writeLine("  ");
			bw.writeLine("  $$('noMeaning').style.display = ((meaning == " + noMeaningIndex + ") ? 'none' : '');");
			for (int p = 0; p < rankNames.length; p++)
				bw.writeLine("  $$('" + rankNames[p] + "').style.display = ((meaning == " + p + ") ? 'none' : '');");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("// call from the onMouseDown event, passing the event if standards compliant");
			bw.writeLine("function contextMouseDown(event) {");
			bw.writeLine("  if (_mouseOverContext)");
			bw.writeLine("    return;");
			bw.writeLine("  ");
			bw.writeLine("  // IE is evil and doesn't pass the event object");
			bw.writeLine("  if (event == null)");
			bw.writeLine("    event = window.event;");
			bw.writeLine("  ");
			bw.writeLine("  // we assume we have a standards compliant browser, but check if we have IE");
			bw.writeLine("  var target = event.target != null ? event.target : event.srcElement;");
			bw.writeLine("");
			bw.writeLine("  // only show the context menu if the right mouse button is pressed");
			bw.writeLine("  //   and a hyperlink has been clicked (the code can be made more selective)");
			bw.writeLine("  if (event.button == 2)");
			bw.writeLine("    _replaceContext = true;");
			bw.writeLine("  else if (!_mouseOverContext)");
			bw.writeLine("    _divContext.style.display = 'none';");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function closeContext() {");
			bw.writeLine("  _mouseOverContext = false;");
			bw.writeLine("  _divContext.style.display = 'none';");
			bw.writeLine("  ");
			bw.writeLine("  // clean up selection state");
			bw.writeLine("  _clickedName = null;");
			bw.writeLine("  _clickedToken = null;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("// call from the onContextMenu event, passing the event");
			bw.writeLine("// if this function returns false, the browser's context menu will not show up");
			bw.writeLine("function contextShow(event) {");
			bw.writeLine("  if (_mouseOverContext)");
			bw.writeLine("    return;");
			bw.writeLine("  ");
			bw.writeLine("  // IE is evil and doesn't pass the event object");
			bw.writeLine("  if (event == null)");
			bw.writeLine("    event = window.event;");
			bw.writeLine("");
			bw.writeLine("  // we assume we have a standards compliant browser, but check if we have IE");
			bw.writeLine("  var target = event.target != null ? event.target : event.srcElement;");
			bw.writeLine("");
			bw.writeLine("  if (_replaceContext) {");
			bw.writeLine("    getSelectionState();");
			bw.writeLine("    ");
			bw.writeLine("    //  clicked outside tokens ==> ignore it");
			bw.writeLine("    if ((_clickedName == null) || (_clickedToken == null)) return false;");
			bw.writeLine("  ");
			bw.writeLine("    adjustContext(_clickedName, _clickedToken);");
			bw.writeLine("    ");
			bw.writeLine("    // document.body.scrollTop does not work in IE");
			bw.writeLine("    var scrollTop = document.body.scrollTop ? document.body.scrollTop :");
			bw.writeLine("      document.documentElement.scrollTop;");
			bw.writeLine("    var scrollLeft = document.body.scrollLeft ? document.body.scrollLeft :");
			bw.writeLine("      document.documentElement.scrollLeft;");
			bw.writeLine("    ");
			bw.writeLine("    // hide the menu first to avoid an \"up-then-over\" visual effect");
			bw.writeLine("    _divContext.style.display = 'none';");
			bw.writeLine("    _divContext.style.left = event.clientX + scrollLeft + 'px';");
			bw.writeLine("    _divContext.style.top = event.clientY + scrollTop + 'px';");
			bw.writeLine("    _divContext.style.display = 'block';");
			bw.writeLine("    ");
			bw.writeLine("    _replaceContext = false;");
			bw.writeLine("");
			bw.writeLine("    return false;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("// comes from prototype.js; this is simply easier on the eyes and fingers");
			bw.writeLine("function $(id) {");
			bw.writeLine("  return $$('name' + _clickedName + '_' + id);");
			bw.writeLine("}");
			bw.writeLine("function $$(id) {");
			bw.writeLine("  return document.getElementById(id);");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("");
			bw.writeLine("// working variables for capturing active tokens");
			bw.writeLine("var _activeName = null;");
			bw.writeLine("var _activeToken = null;");
			bw.writeLine("");
			bw.writeLine("function activateToken(name, index) {");
			bw.writeLine("  _activeName = name;");
			bw.writeLine("  _activeToken = index;");
			bw.writeLine("  ");
			bw.writeLine("  var token = $$('name' + name + '_token' + index);");
			bw.writeLine("  if (token) token.style.backgroundColor = '" + getRGB(LIGHT_BLUE) + "';");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function inactivateToken(name, index) {");
			bw.writeLine("  _activeName = null;");
			bw.writeLine("  _activeToken = null;");
			bw.writeLine("  ");
			bw.writeLine("  var token = $$('name' + name + '_token' + index);");
			bw.writeLine("  if (token)");
			bw.writeLine("    token.style.backgroundColor = tokenColors['name' + name + '_token' + index];");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("");
			bw.writeLine("var _clickedName = null;");
			bw.writeLine("var _clickedToken = null;");
			bw.writeLine("");
			bw.writeLine("function getSelectionState() {");
			bw.writeLine("  _clickedName = _activeName;");
			bw.writeLine("  _clickedToken = _activeToken;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function getTokenMeaning(index) {");
			bw.writeLine("  return Math.ceil($('token' + index + '_meaning').value); // Math.ceil() converts string to number");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function changeTokenMeaning(index, meaning) {");
			bw.writeLine("  $('token' + index + '_meaning').value = meaning;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function setTokenMeaning(meaning) {");
			bw.writeLine("  if ((_clickedName != null) && (_clickedToken != null)) {");
			bw.writeLine("    changeTokenMeaning(_clickedToken, meaning);");
			bw.writeLine("    ensureMeaningOrder(meaning, _clickedToken);");
			bw.writeLine("    adjustColors();");
			bw.writeLine("    adjustName();");
			bw.writeLine("  }");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function ensureMeaningOrder(setMeaning, setMeaningIndex) {");
			bw.writeLine("  if (setMeaning != " + noMeaningIndex + ") {");
			bw.writeLine("    var lastMeaningIndex;");
			bw.writeLine("    ");
			bw.writeLine("    lastMeaningIndex = setMeaning;");
			bw.writeLine("    for (var t = (setMeaningIndex-1); t >= 0; t--) {");
			bw.writeLine("      var tm = getTokenMeaning(t);");
			bw.writeLine("      if (tm != " + noMeaningIndex + ") {");
			bw.writeLine("        if (tm < lastMeaningIndex)");
			bw.writeLine("          lastMeaningIndex = tm;");
			bw.writeLine("        else changeTokenMeaning(t, " + noMeaningIndex + ");");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    lastMeaningIndex = setMeaning;");
			bw.writeLine("    for (var t = (setMeaningIndex+1); $('token' + t); t++) {");
			bw.writeLine("      var tm = getTokenMeaning(t);");
			bw.writeLine("      if (tm != " + noMeaningIndex + ") {");
			bw.writeLine("        if (tm > lastMeaningIndex)");
			bw.writeLine("          lastMeaningIndex = tm;");
			bw.writeLine("        else changeTokenMeaning(t, " + noMeaningIndex + ");");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function adjustName() {");
			bw.writeLine("  ");
			bw.writeLine("  for (var meaning = " + 0 + "; meaning < " + rankNames.length + "; meaning++) {");
			bw.writeLine("    $('display' + meaning).style.display = 'none';");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  var minMeaningfulPart = " + rankNames.length + ";");
			bw.writeLine("  var maxMeaningfulPart = " + 0 + ";");
			bw.writeLine("  ");
			if (this.requiredIndices.length != 0) {
				for (int r = 0; r < this.requiredIndices.length; r++)
					bw.writeLine("  var " + this.rankNames[this.requiredIndices[r]] + "Missing = true;");
				bw.writeLine("  ");
			}
			bw.writeLine("  var t = 0;");
			bw.writeLine("  var token = $('token' + t);");
			bw.writeLine("  while(token) {");
			bw.writeLine("    var meaning = getTokenMeaning(t);");
			bw.writeLine("    if ((meaning >= " + 0 + ") && (meaning < " + rankNames.length + ")) {");
			bw.writeLine("      minMeaningfulPart = Math.min(minMeaningfulPart, meaning);");
			bw.writeLine("      maxMeaningfulPart = Math.max(maxMeaningfulPart, meaning);");
			bw.writeLine("      ");
			if (this.requiredIndices.length != 0) {
				for (int r = 0; r < this.requiredIndices.length; r++) {
					bw.writeLine("      if (meaning == " + this.requiredIndices[r] + ") " + this.rankNames[this.requiredIndices[r]] + "Missing = false;");
						}
				bw.writeLine("      ");
				}
			bw.writeLine("      var tokenString = token.innerHTML;");
			bw.writeLine("      if (tokenString.length < " + 3 + ") {");
			bw.writeLine("        ");
			bw.writeLine("      }");
			bw.writeLine("      displayMeaningfulPartString(meaning, tokenString);");
			bw.writeLine("      $('display' + meaning).style.display = '';");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    t++;");
			bw.writeLine("    token = $('token' + t);");
			bw.writeLine("  }");
			bw.writeLine("  ");
			if (this.minRequiredIndex != Integer.MAX_VALUE) {
				bw.writeLine("  if (minMeaningfulPart > " + this.minRequiredIndex + ") {");
				bw.writeLine("    minMeaningfulPart = " + this.minRequiredIndex + ";");
				bw.writeLine("    ");
				bw.writeLine("    //  insert preset string from register object");
				bw.writeLine("    displayMeaningfulPartString(" + this.minRequiredIndex + ", (meaningfulPartStrings['name' + _clickedName + '_" + this.minRequiredIndex + "'] ? meaningfulPartStrings['name' + _clickedName + '_" + this.minRequiredIndex + "'] : ''));");
				bw.writeLine("    $('display' + " + this.minRequiredIndex + ").style.display = '';");
				bw.writeLine("  }");
				bw.writeLine("  ");
			}
			if (this.requiredIndices.length != 0)
				for (int r = 0; r < this.requiredIndices.length; r++) {
					bw.writeLine("  if ((maxMeaningfulPart > " + this.requiredIndices[r] + ") && " + this.rankNames[this.requiredIndices[r]] + "Missing) {");
					bw.writeLine("    ");
					bw.writeLine("    //  insert preset string from register object");
					bw.writeLine("    displayMeaningfulPartString(" + this.requiredIndices[r] + ", (meaningfulPartStrings['name' + _clickedName + '_" + this.requiredIndices[r] + "'] ? meaningfulPartStrings['name' + _clickedName + '_" + this.requiredIndices[r] + "'] : ''));");
					bw.writeLine("    $('display' + " + this.requiredIndices[r] + ").style.display = '';");
					bw.writeLine("  }");
					bw.writeLine("  ");
				}
			bw.writeLine("  for (var meaning = " + 0 + "; meaning < " + rankNames.length + "; meaning++) {");
			bw.writeLine("    if ($('display' + meaning).style.display == 'none')");
			bw.writeLine("      $('meaning' + meaning + '_string').value = '';");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function displayMeaningfulPartString(meaning, meaningfulPartString) {");
			bw.writeLine("  ");
			bw.writeLine("  //  store in drop-down (complete if abbreviation)");
			bw.writeLine("  var select = $('select' + meaning);");
			bw.writeLine("  if (select) {");
			bw.writeLine("    select.selectedIndex = -1;");
			bw.writeLine("    if (meaningfulPartString.length < " + 3 + ") {");
			bw.writeLine("      for (var o = 0; o < select.options.length; o++) {");
			bw.writeLine("        if (select.options[o].value.substr(0, meaningfulPartString.length) == meaningfulPartString) {");
			bw.writeLine("          meaningfulPartString = select.options[o].value;");
			bw.writeLine("          o = select.options.length;");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    for (var o = 0; o < select.options.length; o++) {");
			bw.writeLine("      select.options[o].selected = (select.options[o].value == meaningfulPartString);");
			bw.writeLine("      if (select.options[o].selected)");
			bw.writeLine("        select.selectedIndex = o;");
			bw.writeLine("    }");
			bw.writeLine("    if (select.selectedIndex == -1) {");
			bw.writeLine("      var freeText = $('freeText' + meaning);");
			bw.writeLine("      freeText.value = meaningfulPartString;");
			bw.writeLine("      freeText.text = meaningfulPartString;");
			bw.writeLine("      freeText.selected = true;");
			bw.writeLine("      select.selectedIndex = (select.options.length - 1);");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  else if (meaningfulPartString.length < meaningfulPartStrings['name' + _clickedName + '_' + meaning].length) {");
			bw.writeLine("    if (meaningfulPartStrings['name' + _clickedName + '_' + meaning].substr(0, meaningfulPartString.length) == meaningfulPartString)");
			bw.writeLine("      meaningfulPartString = meaningfulPartStrings['name' + _clickedName + '_' + meaning];");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  // keep hidden form field in sync");
			bw.writeLine("  $('meaning' + meaning + '_string').value = meaningfulPartString;");
			bw.writeLine("  ");
			bw.writeLine("  //  store in text input");
			bw.writeLine("  $('input' + meaning).value = meaningfulPartString;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function meaningfulPartStringInput(name, meaning) {");
			bw.writeLine("  var meaningString = $$('name' + name + '_input' + meaning).value;");
			bw.writeLine("  ");
			bw.writeLine("  // keep hidden form field in sync");
			bw.writeLine("  $$('name' + name + '_meaning' + meaning + '_string').value = meaningString;");
			bw.writeLine("  ");
			bw.writeLine("  //  store in drop-down");
			bw.writeLine("  var select = $$('name' + name + '_select' + meaning);");
			bw.writeLine("  if (select) {");
			bw.writeLine("    select.selectedIndex = -1;");
			bw.writeLine("    for (var o = 0; o < select.options.length; o++) {");
			bw.writeLine("      select.options[o].selected = (select.options[o].value == meaningString);");
			bw.writeLine("      if (select.options[o].selected)");
			bw.writeLine("        select.selectedIndex = o;");
			bw.writeLine("    }");
			bw.writeLine("    if (select.selectedIndex == -1) {");
			bw.writeLine("      var freeText = $$('name' + name + '_freeText' + meaning);");
			bw.writeLine("      freeText.value = meaningString;");
			bw.writeLine("      freeText.text = meaningString;");
			bw.writeLine("      freeText.selected = true;");
			bw.writeLine("      select.selectedIndex = (select.options.length - 1);");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function meaningfulPartStringSelected(name, meaning) {");
			bw.writeLine("  var select = $$('name' + name + '_select' + meaning);");
			bw.writeLine("  var meaningString = select.options[select.selectedIndex].value;");
			bw.writeLine("  ");
			bw.writeLine("  // prevent empty free text option from being selected");
			bw.writeLine("  if (meaningString.length == 0) {");
			bw.writeLine("    meaningString = select.options[0].value;");
			bw.writeLine("    select.options[0].selected = true;");
			bw.writeLine("    select.selectedIndex = 0;");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  // keep hidden form field in sync");
			bw.writeLine("  $$('name' + name + '_meaning' + meaning + '_string').value = meaningString;");
			bw.writeLine("  ");
			bw.writeLine("  //  store in text input");
			bw.writeLine("  $$('name' + name + '_input' + meaning).value = meaningString;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("function displayState(name) {");
			bw.writeLine("  var state = '';");
			bw.writeLine("  for (var m = 0; m <= 7; m++) {");
			bw.writeLine("    state = (state + '\\n');");
			bw.writeLine("    state = (state + $$('name' + name + '_meaning' + m + '_string').value);");
			bw.writeLine("  }");
			bw.writeLine("  alert('Status:' + state);");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			
			if (bw != out)
				bw.flush();
		}
/*
var _replaceContext = false;		// replace the system context menu?
var _mouseOverContext = false;		// is the mouse over the context menu?
var _divContext;	// makes my life easier

var _colors = new Object(); // maps meaning indices to colors

var _noMeaningIndex = -2;
var _cutToIndex = -1;
var _familyIndex = 0;
var _subFamilyIndex = 1;
var _tribeIndex = 2;
var _genusIndex = 3;
var _subGenusIndex = 4;
var _speciesIndex = 5;
var _subSpeciesIndex = 6;
var _varietyIndex = 7;
var _splitIndex = 8;
var _cutFromIndex = 9;


var tokenColors = new Object(); // required to restore colors after roll-over effect
var meaningfulPartStrings = new Object(); // holds the preset part values from name completer

function initContext() {
	_divContext = $$('divContext');
	
	_divContext.onmouseover = function() { _mouseOverContext = true; };
	_divContext.onmouseout = function() { _mouseOverContext = false; };
	
	document.body.onmousedown = contextMouseDown;
	document.body.oncontextmenu = contextShow;
	
	_colors['-2'] = 'FFFFFF';
	_colors['-1'] = '888888';
	_colors['0'] = 'FF00FF';
	_colors['1'] = 'FF20FF';
	_colors['2'] = 'FF40FF';
	_colors['3'] = 'FF60FF';
	_colors['4'] = 'FF80FF';
	_colors['5'] = 'FFA0FF';
	_colors['6'] = 'FFC0FF';
	_colors['7'] = 'FFE0FF';
	_colors['8'] = '00FF00';
	_colors['9'] = '888888';
	
	meaningfulPartStrings['name0_0'] = "FamilyTest";
	meaningfulPartStrings['name0_1'] = "";
	meaningfulPartStrings['name0_2'] = "";
	meaningfulPartStrings['name0_3'] = "Euponera";
	meaningfulPartStrings['name0_4'] = "Mesoponera";
	meaningfulPartStrings['name0_5'] = "laevigata";
	meaningfulPartStrings['name0_6'] = "";
	meaningfulPartStrings['name0_7'] = "whelpleyi";
	
	var name = 0;
	while($$('name' + name + '_token0')) {
		_clickedName = name;
		adjustColors();
		adjustName();
		_clickedName = null;
		name++;
	}
}

function adjustColors() {
	
	var cutTo = -1;
	var cutFrom = -1;
	var splitFrom = -1;
	
	var t = 0;
	var token = $('token' + 0);
	while (token) {
		var meaning = getTokenMeaning(t);
		if (_colors[meaning]) {
			token.style.backgroundColor = _colors[meaning];
			tokenColors['name' + _clickedName + '_token' + t] = _colors[meaning];
			var space = $('space' + t);
			if (space)
				space.style.backgroundColor = 'FFFFFF';
		}
		
		if (meaning == _cutToIndex) {
			cutTo = t;
		}
		
		else if (meaning == _splitIndex) {
			splitFrom = t;
		}
		
		else if (meaning == _cutFromIndex) {
			cutFrom = t;
		}
		t++;
		token = $('token' + t);
	}
	
	if (cutTo != -1) {
		for (var c = cutTo; c >= 0; c--) {
			var cToken = $('token' + c);
			if (cToken && (c != cutTo)) {
				cToken.style.backgroundColor = 'CCCCCC';
				tokenColors['token' + c] = 'CCCCCC';
			}
			var cSpace = $('space' + c);
			if (cSpace)
				cSpace.style.backgroundColor = 'CCCCCC';
		}
	}
	
	if (splitFrom != -1) {
		for (var s = (splitFrom+1); $('token' + s); s++) {
			var sToken = $('token' + s);
			if (sToken) {
				sToken.style.backgroundColor = 'AAFFAA';
				tokenColors['token' + s] = 'AAFFAA';
			}
			var sSpace = $('space' + s);
			if (sSpace)
				sSpace.style.backgroundColor = 'AAFFAA';
		}
	}
	
	if (cutFrom != -1) {
		for (var c = (cutFrom+1); $('token' + c); c++) {
			var cToken = $('token' + c);
			if (cToken) {
				cToken.style.backgroundColor = 'CCCCCC';
				tokenColors['token' + c] = 'CCCCCC';
			}
			var cSpace = $('space' + c);
			if (cSpace)
				cSpace.style.backgroundColor = 'CCCCCC';
		}
	}
}

function adjustContext() {
	
	var meaning = getTokenMeaning(_clickedToken);
	
	$$('noCut').style.display = (((meaning == _cutToIndex) || (meaning == _cutFromIndex)) ? '' : 'none');
	$$('cutTo').style.display = (((meaning == _cutToIndex) || (meaning == _cutFromIndex)) ? 'none' : '');
	$$('cutFrom').style.display = (((meaning == _cutToIndex) || (meaning == _cutFromIndex)) ? 'none' : '');
	
	$$('split').style.display = ((meaning == _splitIndex) ? 'none' : '');
	$$('noSplit').style.display = ((meaning == _splitIndex) ? '' : 'none');
	
	$$('noMeaning').style.display = ((meaning == _noMeaningIndex) ? 'none' : '');
	$$('family').style.display = ((meaning == _familyIndex) ? 'none' : '');
	$$('subFamily').style.display = ((meaning == _subFamilyIndex) ? 'none' : '');
	$$('tribe').style.display = ((meaning == _tribeIndex) ? 'none' : '');
	$$('genus').style.display = ((meaning == _genusIndex) ? 'none' : '');
	$$('subGenus').style.display = ((meaning == _subGenusIndex) ? 'none' : '');
	$$('species').style.display = ((meaning == _speciesIndex) ? 'none' : '');
	$$('subSpecies').style.display = ((meaning == _subSpeciesIndex) ? 'none' : '');
	$$('variety').style.display = ((meaning == _varietyIndex) ? 'none' : '');
}

// call from the onMouseDown event, passing the event if standards compliant
function contextMouseDown(event) {
	if (_mouseOverContext)
		return;
	
	// IE is evil and doesn't pass the event object
	if (event == null)
		event = window.event;
	
	// we assume we have a standards compliant browser, but check if we have IE
	var target = event.target != null ? event.target : event.srcElement;

	// only show the context menu if the right mouse button is pressed
	//	 and a hyperlink has been clicked (the code can be made more selective)
	if (event.button == 2)
		_replaceContext = true;
	else if (!_mouseOverContext)
		_divContext.style.display = 'none';
}

function closeContext() {
	_mouseOverContext = false;
	_divContext.style.display = 'none';
	
	// clean up selection state
	_clickedName = null;
	_clickedToken = null;
}

// call from the onContextMenu event, passing the event
// if this function returns false, the browser's context menu will not show up
function contextShow(event) {
	if (_mouseOverContext)
		return;
	
	// IE is evil and doesn't pass the event object
	if (event == null)
		event = window.event;

	// we assume we have a standards compliant browser, but check if we have IE
	var target = event.target != null ? event.target : event.srcElement;

	if (_replaceContext) {
		getSelectionState();
		
		//	clicked outside tokens ==> ignore it
		if ((_clickedName == null) || (_clickedToken == null)) return false;
	
		adjustContext(_clickedName, _clickedToken);
		
		// document.body.scrollTop does not work in IE
		var scrollTop = document.body.scrollTop ? document.body.scrollTop :
			document.documentElement.scrollTop;
		var scrollLeft = document.body.scrollLeft ? document.body.scrollLeft :
			document.documentElement.scrollLeft;
		
		// hide the menu first to avoid an "up-then-over" visual effect
		_divContext.style.display = 'none';
		_divContext.style.left = event.clientX + scrollLeft + 'px';
		_divContext.style.top = event.clientY + scrollTop + 'px';
		_divContext.style.display = 'block';
		
		_replaceContext = false;

		return false;
	}
}

// comes from prototype.js; this is simply easier on the eyes and fingers
function $(id) {
	return $$('name' + _clickedName + '_' + id);
}
function $$(id) {
	return document.getElementById(id);
}


// working variables for capturing active tokens
var _activeName = null;
var _activeToken = null;

function activateToken(name, index) {
	_activeName = name;
	_activeToken = index;
	
	var token = $$('name' + name + '_token' + index);
	if (token) token.style.backgroundColor = '8888FF';
}

function inactivateToken(name, index) {
	_activeName = null;
	_activeToken = null;
	
	var token = $$('name' + name + '_token' + index);
	if (token)
		token.style.backgroundColor = tokenColors['name' + name + '_token' + index];
}


var _clickedName = null;
var _clickedToken = null;

function getSelectionState() {
	_clickedName = _activeName;
	_clickedToken = _activeToken;
}

function getTokenMeaning(index) {
	return $('token' + index + '_meaning').value;
}

function changeTokenMeaning(index, meaning) {
	$('token' + index + '_meaning').value = meaning;
}

function setTokenMeaning(meaning) {
	if ((_clickedName != null) && (_clickedToken != null)) {
		changeTokenMeaning(_clickedToken, meaning);
		ensureMeaningOrder(meaning, _clickedToken);
		adjustColors();
		adjustName();
	}
	closeContext();
	return false;
}

function ensureMeaningOrder(setMeaning, setMeaningIndex) {
	if (setMeaning != _noMeaningIndex) {
		var lastMeaningIndex;
		
		lastMeaningIndex = setMeaning;
		for (var t = (setMeaningIndex-1); t >= 0; t--) {
			var tm = getTokenMeaning(t);
			if (tm != _noMeaningIndex) {
				if (tm < lastMeaningIndex)
					lastMeaningIndex = tm;
				else changeTokenMeaning(t, _noMeaningIndex);
			}
		}
		
		lastMeaningIndex = setMeaning;
		for (var t = (setMeaningIndex+1); $('token' + t); t++) {
			var tm = getTokenMeaning(t);
			if (tm != _noMeaningIndex) {
				if (tm > lastMeaningIndex)
					lastMeaningIndex = tm;
				else changeTokenMeaning(t, _noMeaningIndex);
			}
		}
	}
}

function adjustName() {
	
	for (var meaning = 0; meaning <= 7; meaning++) {
		$('display' + meaning).style.display = 'none';
	}
	
	var minMeaningfulPart = 8;
	var maxMeaningfulPart = 0;
	
	var genusMissing = true;
	var speciesMissing = true;
	
	var t = 0;
	var token = $('token' + t);
	while(token) {
		var meaning = getTokenMeaning(t);
		if ((meaning >= _familyIndex) && (meaning <= _varietyIndex)) {
			minMeaningfulPart = Math.min(minMeaningfulPart, meaning);
			maxMeaningfulPart = Math.max(maxMeaningfulPart, meaning);
			
			if (meaning == _genusIndex) genusMissing = false;
			if (meaning == _speciesIndex) speciesMissing = false;
			
			var tokenString = token.innerHTML;
			if (tokenString.length < 3) {
				
			}
			displayMeaningfulPartString(meaning, tokenString);
			$('display' + meaning).style.display = '';
		}
		
		t++;
		token = $('token' + t);
	}
	
	if (minMeaningfulPart > _genusIndex) {
		minMeaningfulPart = _genusIndex;
		
		//	insert preset string from register object
		displayMeaningfulPartString(_genusIndex, (meaningfulPartStrings['name' + _clickedName + '_3'] ? meaningfulPartStrings['name' + _clickedName + '_3'] : ''));
		$('display' + _genusIndex).style.display = '';
	}
	
	if ((maxMeaningfulPart > _genusIndex) && genusMissing) {
		
		//	insert preset string from register object
		displayMeaningfulPartString(_genusIndex, (meaningfulPartStrings['name' + _clickedName + '_3'] ? meaningfulPartStrings['name' + _clickedName + '_3'] : ''));
		$('display' + _genusIndex).style.display = '';
	}
	
	if ((maxMeaningfulPart > _speciesIndex) && speciesMissing) {
		
		//	insert preset string from register object
		displayMeaningfulPartString(_speciesIndex, (meaningfulPartStrings['name' + _clickedName + '_5'] ? meaningfulPartStrings['name' + _clickedName + '_5'] : ''));
		$('display' + _speciesIndex).style.display = '';
	}
	
	for (var meaning = 0; meaning <= 7; meaning++) {
		if ($('display' + meaning).style.display == 'none')
			$('meaning' + meaning + '_string').value = '';
	}
}

function displayMeaningfulPartString(meaning, meaningfulPartString) {
	
	//	store in drop-down (complete if abbreviation)
	var select = $('select' + meaning);
	if (select) {
		select.selectedIndex = -1;
		if (meaningfulPartString.length < 3) {
			for (var o = 0; o < select.options.length; o++) {
				if (select.options[o].value.substr(0, meaningfulPartString.length) == meaningfulPartString) {
					meaningfulPartString = select.options[o].value;
					o = select.options.length;
				}
			}
		}
		for (var o = 0; o < select.options.length; o++) {
			select.options[o].selected = (select.options[o].value == meaningfulPartString);
			if (select.options[o].selected)
				select.selectedIndex = o;
		}
		if (select.selectedIndex == -1) {
			var freeText = $('freeText' + meaning);
			freeText.value = meaningfulPartString;
			freeText.text = meaningfulPartString;
			freeText.selected = true;
			select.selectedIndex = (select.options.length - 1);
		}
	}
	else if (meaningfulPartString.length < meaningfulPartStrings['name' + _clickedName + '_' + meaning].length) {
		if (meaningfulPartStrings['name' + _clickedName + '_' + meaning].substr(0, meaningfulPartString.length) == meaningfulPartString)
			meaningfulPartString = meaningfulPartStrings['name' + _clickedName + '_' + meaning];
	}
	
	// keep hidden form field in sync
	$('meaning' + meaning + '_string').value = meaningfulPartString;
	
	//	store in text input
	$('input' + meaning).value = meaningfulPartString;
}

function meaningfulPartStringInput(name, meaning) {
	var meaningString = $$('name' + name + '_input' + meaning).value;
	
	// keep hidden form field in sync
	$$('name' + name + '_meaning' + meaning + '_string').value = meaningString;
	
	//	store in drop-down
	var select = $$('name' + name + '_select' + meaning);
	if (select) {
		select.selectedIndex = -1;
		for (var o = 0; o < select.options.length; o++) {
			select.options[o].selected = (select.options[o].value == meaningString);
			if (select.options[o].selected)
				select.selectedIndex = o;
		}
		if (select.selectedIndex == -1) {
			var freeText = $$('name' + name + '_freeText' + meaning);
			freeText.value = meaningString;
			freeText.text = meaningString;
			freeText.selected = true;
			select.selectedIndex = (select.options.length - 1);
		}
	}
}

function meaningfulPartStringSelected(name, meaning) {
	var select = $$('name' + name + '_select' + meaning);
	var meaningString = select.options[select.selectedIndex].value;
	
	// prevent empty free text option from being selected
	if (meaningString.length == 0) {
		meaningString = select.options[0].value;
		select.options[0].selected = true;
		select.selectedIndex = 0;
	}
	
	// keep hidden form field in sync
	$$('name' + name + '_meaning' + meaning + '_string').value = meaningString;
	
	//	store in text input
	$$('name' + name + '_input' + meaning).value = meaningString;
}

function displayState(name) {
	var state = '';
	for (var m = 0; m <= 7; m++) {
		state = (state + '\n');
		state = (state + $$('name' + name + '_meaning' + m + '_string').value);
	}
	alert('Status:' + state);
	return false;
}
 */
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			bw.writeLine(".panel {");
			bw.writeLine("  border-width: 2px;");
			bw.writeLine("  border-color: 444;");
			bw.writeLine("  border-style: solid;");
			bw.writeLine("  border-collapse: collapse;");
			bw.writeLine("  margin: 10px 0px 10px;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".menu {");
			bw.writeLine("  margin: 0;");
			bw.writeLine("  padding: 0.3em;");
			bw.writeLine("  list-style-type: none;");
			bw.writeLine("  background-color: ddd;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".menu li:hover {}");
			bw.writeLine("");
			bw.writeLine(".menu hr {");
			bw.writeLine("  border: 0;");
			bw.writeLine("  border-bottom: 1px solid grey;");
			bw.writeLine("  margin: 3px 0px 3px 0px;");
			bw.writeLine("  width: 10em;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".menu a {");
			bw.writeLine("  border: 0 !important;");
			bw.writeLine("  text-decoration: none;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".menu a:hover {");
			bw.writeLine("  text-decoration: underline !important;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".menuItem {");
			bw.writeLine("  font-family: Arial,Helvetica,Verdana;");
			bw.writeLine("  font-size: 10pt;");
			bw.writeLine("  padding: 2pt 5pt 2pt;");
			bw.writeLine("  text-align; left;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".token {");
			bw.writeLine("  font-family: Verdana,Arial,Helvetica;");
			bw.writeLine("  font-size: 12pt;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".display {");
			bw.writeLine("  background-color: eee;");
			bw.writeLine("  font-family: Arial,Helvetica,Verdana;");
			bw.writeLine("  font-size: 10pt;");
			bw.writeLine("  padding: 2px 5px 2px;");
			bw.writeLine("  border-width: 0px;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine(".meaningfulValue {");
			bw.writeLine("  font-weight: bold;");
			bw.writeLine("}");
			
			if (bw != out)
				bw.flush();
		}
/*
table {
	border-width: 2px;
	border-color: 444;
	border-style: solid;
	border-collapse: collapse;
}

td {
	padding: 10px;
}


.menu {
	margin: 0;
	padding: 0.3em;
	list-style-type: none;
	background-color: ddd;
}

.menu li:hover {}

.menu hr {
	border: 0;
	border-bottom: 1px solid grey;
	margin: 3px 0px 3px 0px;
	width: 10em;
}

.menu a {
	border: 0 !important;
	text-decoration: none;
}

.menu a:hover {
	text-decoration: underline !important;
}

.menuItem {
	font-family: Arial,Helvetica,Verdana;
	font-size: 10pt;
	padding: 2pt 5pt 2pt;
	text-align; left;
}

.token {
	font-family: Verdana,Arial,Helvetica;
	font-size: 12pt;
}

.display {
	background-color: eee;
	font-family: Arial,Helvetica,Verdana;
	font-size: 10pt;
	padding: 2px 5px 2px;
	border-width: 0px;
}

.meaningfulValue {
	font-weight: bold;
}
 */

		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			bw.writeLine("<div id=\"divContext\" style=\"border: 1px solid blue; display: none; position: absolute\">");
			bw.writeLine("<ul class=\"menu\">");
//			bw.writeLine("<li id=\"cutTo\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + cutToIndex + "');\">Cut Up To Here</a></li>");
//			bw.newLine();
//			bw.writeLine("<li id=\"cutFrom\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + cutFromIndex + "');\">Cut From Here</a></li>");
//			bw.newLine();
//			bw.writeLine("<li id=\"noCut\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + noMeaningIndex + "');\">Do Not Cut</a></li>");
//			bw.newLine();
//			bw.writeLine("<li id=\"split\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + splitFromIndex + "');\">Split From Here</a></li>");
//			bw.newLine();
//			bw.writeLine("<li id=\"noSplit\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + noMeaningIndex + "');\">Do Not Split</a></li>");
//			bw.newLine();
//			bw.writeLine("<li class=\"menuItem\"><hr></li>");
//			bw.newLine();
//			bw.writeLine("<li id=\"noMeaning\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + noMeaningIndex + "');\">No Meaning</a></li>");
//			bw.newLine();
//			for (int p = 0; p < rankNames.length; p++) {
//				bw.writeLine("<li id=\"" + rankNames[p] + "\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning('" + p + "');\">" + rankNames[p].substring(0, 1).toUpperCase() + rankNames[p].substring(1) + "</a></li>");
//				bw.newLine();
//			}
			bw.writeLine("<li id=\"cutTo\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + cutToIndex + ");\">Cut Up To Here</a></li>");
			bw.writeLine("<li id=\"cutFrom\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + cutFromIndex + ");\">Cut From Here</a></li>");
			bw.writeLine("<li id=\"noCut\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + noMeaningIndex + ");\">Do Not Cut</a></li>");
			bw.writeLine("<li id=\"split\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + splitFromIndex + ");\">Split From Here</a></li>");
			bw.writeLine("<li id=\"noSplit\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + noMeaningIndex + ");\">Do Not Split</a></li>");
			bw.writeLine("<li class=\"menuItem\"><hr></li>");
			bw.writeLine("<li id=\"noMeaning\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + noMeaningIndex + ");\">No Meaning</a></li>");
			for (int p = 0; p < rankNames.length; p++)
				bw.writeLine("<li id=\"" + rankNames[p] + "\" class=\"menuItem\"><a href=\"#\" onclick=\"return setTokenMeaning(" + p + ");\">" + rankNames[p].substring(0, 1).toUpperCase() + rankNames[p].substring(1) + "</a></li>");
			bw.writeLine("</ul>");
			bw.writeLine("</div>");
			bw.writeLine("");
			
			for (int n = 0; n < this.taxonNamePanels.size(); n++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(n));
				
				bw.writeLine("<table class=\"panel\" width=\"100%\">");
				bw.writeLine("<tr>");
				bw.writeLine("<td class=\"panel\">");
				
				String nameContext = tnpp.taxonNameContext;
				if ("<html>".equals(nameContext.substring(0, 6).toLowerCase()))
					nameContext = nameContext.substring(6);
				if ("</html>".equals(nameContext.substring(nameContext.length() - 7).toLowerCase()))
					nameContext = nameContext.substring(0, (nameContext.length() - 7));
				bw.writeLine("<table id=\"name" + n + "_context\" class=\"display\" style=\"margin-bottom: 10px;\">");
				bw.writeLine("<tr>");
				bw.writeLine("<td class=\"display\" width=\"99%\">" + nameContext + "</td>");
				bw.writeLine("<td class=\"display\" style=\"padding-right: 0px;\"><input type=\"checkbox\" name=\"name" + n + "_remove\" value=\"R\"></td>");
				bw.writeLine("<td class=\"display\" style=\"padding-left: 0px;\">Remove?</td>");
				bw.writeLine("</tr>");
				bw.writeLine("</table>");
				
				for (int t = 0; t < tnpp.taxonNameTokens.length; t++) {
					if (t != 0) bw.write("<span id=\"name" + n + "_space" + t + "\" class=\"token\">" + (Gamta.insertSpace(tnpp.taxonNameTokens[t-1], tnpp.taxonNameTokens[t]) ? " " : "") + "</span>");
					bw.write("<span id=\"name" + n + "_token" + t + "\" onmouseover=\"activateToken(" + n + ", " + t + ");\" onmouseout=\"inactivateToken(" + n + ", " + t + ");\" class=\"token\">" + tnpp.taxonNameTokens[t] + "</span>");
				}
				bw.newLine();
				
				bw.writeLine("<table id=\"name" + n + "_display\" class=\"display\" style=\"margin-top: 10px;\">");
				for (int p = 0; p < rankNames.length; p++) {
					bw.writeLine("<tr id=\"name" + n + "_display" + p + "\" class=\"display\">");
					bw.writeLine("<td class=\"display\">" + rankNames[p].substring(0, 1).toUpperCase() + rankNames[p].substring(1) + ":</td>");
					
					String[] options = new String[0];
					for (int rg = 0; rg < rankGroupIndices.length; rg++) {
						if (p >= rankGroupIndices[rg])
							options = getEpithetList(rankGroupNames[rg]);
						else rg = rankGroupIndices.length;
					}
					if (options.length == 0)
						bw.writeLine("<td class=\"display\" colspan=\"2\"><input type=\"text\" id=\"name" + n + "_input" + p + "\" onkeyup=\"meaningfulPartStringInput(" + n + ", " + p + ");\"></td>");
					else {
						bw.writeLine("<td class=\"display\"><select id=\"name" + n + "_select" + p + "\" onchange=\"meaningfulPartStringSelected(" + n + ", " + p + ")\">");
						for (int o = 0; o < options.length; o++)
							bw.writeLine("<option value=\"" + options[o] + "\">" + options[o] + "</option>");
						bw.writeLine("<option value=\"\" id=\"name" + n + "_freeText" + p + "\"></option>");
						bw.writeLine("</select></td>");
						bw.writeLine("<td class=\"display\"><input type=\"text\" id=\"name" + n + "_input" + p + "\" onkeyup=\"meaningfulPartStringInput(" + n + ", " + p + ");\"></td>");
					}
					bw.writeLine("</tr>");
				}
				bw.writeLine("</table>");
				bw.writeLine("</td>");
				bw.writeLine("</tr>");
				
				if (DEBUG) {
					bw.writeLine("<tr>");
					bw.writeLine("<td>");
					bw.writeLine("<input type=\"submit\" value=\"View Status\" onclick=\"return displayState(" + n + ");\">");
					bw.writeLine("</td>");
					bw.writeLine("</tr>");
				}
				
				bw.writeLine("</table>");
			}
			
			for (int n = 0; n < this.taxonNamePanels.size(); n++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(n));
				for (int t = 0; t < tnpp.taxonNameTokens.length; t++)
					bw.writeLine("<input type=\"hidden\" name=\"name" + n + "_token" + t + "_meaning\" id=\"name" + n + "_token" + t + "_meaning\" value=\"" + tnpp.taxonNameTokenMeanings[t] + "\">");
				for (int p = 0; p < rankNames.length; p++)
					bw.writeLine("<input type=\"hidden\" name=\"name" + n + "_meaning" + p + "_string\" id=\"name" + n + "_meaning" + p + "_string\" value=\"" + ((tnpp.meaningfulPartStrings[p] == null) ? "" : tnpp.meaningfulPartStrings[p]) + "\">");
				bw.writeLine("");
			}
			
			if (bw != out)
				bw.flush();
		}
/*
<div id="divContext" style="border: 1px solid blue; display: none; position: absolute">

<ul class="menu">

<li id="cutTo" class="menuItem"><a href="#" onclick="return setTokenMeaning('-1');">Cut Up To Here</a></li>
<li id="cutFrom" class="menuItem"><a href="#" onclick="return setTokenMeaning('9');">Cut From Here</a></li>
<li id="noCut" class="menuItem"><a href="#" onclick="return setTokenMeaning('-2');">Do Not Cut</a></li>

<li id="split" class="menuItem"><a href="#" onclick="return setTokenMeaning('8');">Split From Here</a></li>
<li id="noSplit" class="menuItem"><a href="#" onclick="return setTokenMeaning('-2');">Do Not Split</a></li>

<li id="noMeaning" class="menuItem"><a href="#" onclick="return setTokenMeaning('-2');">No Meaning</a></li>
<li id="family" class="menuItem"><a href="#" onclick="return setTokenMeaning('0');">Family</a></li>
<li id="subFamily" class="menuItem"><a href="#" onclick="return setTokenMeaning('1');">SubFamily</a></li>
<li id="tribe" class="menuItem"><a href="#" onclick="return setTokenMeaning('2');">Tribe</a></li>
<li id="genus" class="menuItem"><a href="#" onclick="return setTokenMeaning('3');">Genus</a></li>
<li id="subGenus" class="menuItem"><a href="#" onclick="return setTokenMeaning('4');">SubGenus</a></li>
<li id="species" class="menuItem"><a href="#" onclick="return setTokenMeaning('5');">Species</a></li>
<li id="subSpecies" class="menuItem"><a href="#" onclick="return setTokenMeaning('6');">SubSpecies</a></li>
<li id="variety" class="menuItem"><a href="#" onclick="return setTokenMeaning('7');">Variety</a></li>

</ul>

</div>

<table>
<tr>
<td>

<table id="name0_context" class="display" style="margin-bottom: 10px;">
<tr>
<td class="display" width="100%">... newly discovered variety <b>Euponera (Mesoponera) laevigata F. Smith variety whelpleyi</b> differs from other ...</td>
<td class="display" style="padding-right: 0px;"><input type="checkbox" name="name0_remove"></td>
<td class="display" style="padding-left: 0px;">Remove?</td>
</tr>
</table>


<span id="name0_token0" onmouseover="activateToken(0, 0);" onmouseout="inactivateToken(0, 0);" class="token">Euponera</span>
<span id="name0_space1" class="token"> </span>
<span id="name0_token1" onmouseover="activateToken(0, 1);" onmouseout="inactivateToken(0, 1);" class="token">(</span>
<span id="name0_space2" class="token"></span>
<span id="name0_token2" onmouseover="activateToken(0, 2);" onmouseout="inactivateToken(0, 2);" class="token">Mesoponera</span>
<span id="name0_space3" class="token"></span>
<span id="name0_token3" onmouseover="activateToken(0, 3);" onmouseout="inactivateToken(0, 3);" class="token">)</span>
<span id="name0_space4" class="token"> </span>
<span id="name0_token4" onmouseover="activateToken(0, 4);" onmouseout="inactivateToken(0, 4);" class="token">laevigata</span>
<span id="name0_space5" class="token"> </span>
<span id="name0_token5" onmouseover="activateToken(0, 5);" onmouseout="inactivateToken(0, 5);" class="token">F</span>
<span id="name0_space6" class="token"></span>
<span id="name0_token6" onmouseover="activateToken(0, 6);" onmouseout="inactivateToken(0, 6);" class="token">.</span>
<span id="name0_space7" class="token"> </span>
<span id="name0_token7" onmouseover="activateToken(0, 7);" onmouseout="inactivateToken(0, 7);" class="token">Smith</span>
<span id="name0_space8" class="token"> </span>
<span id="name0_token8" onmouseover="activateToken(0, 8);" onmouseout="inactivateToken(0, 8);" class="token">variety</span>
<span id="name0_space9" class="token"> </span>
<span id="name0_token9" onmouseover="activateToken(0, 9);" onmouseout="inactivateToken(0, 9);" class="token">whelpleyi</span>

<!--Euponera (Mesoponera) laevigata F. Smith variety whelpleyi-->

<table id="name0_display" class="display" style="margin-top: 10px;">

<tr id="name0_display0" class="display">
<td class="display">Family:</td>
<td class="display" colspan="2"><input type="text" id="name0_input0" onkeyup="meaningfulPartStringInput(0, 0);"></td>
</tr>

<tr id="name0_display1" class="display">
<td class="display">SubFamily:</td>
<td class="display" colspan="2"><input type="text" id="name0_input1" onkeyup="meaningfulPartStringInput(0, 1);"></td>
</tr>

<tr id="name0_display2" class="display">
<td class="display">Tribe:</td>
<td class="display" colspan="2"><input type="text" id="name0_input2" onkeyup="meaningfulPartStringInput(0, 2);"></td>
</tr>


<tr id="name0_display3" class="display">
<td class="display">Genus:</td>
<td class="display"><select id="name0_select3" onchange="meaningfulPartStringSelected(0, 3)">
<option value="Euponera">Euponera</option>
<option value="OtherGenus">OtherGenus</option>
<option value="ThirdGenus">ThirdGenus</option>
<option value="FourthGenus">FourthGenus</option>
<option value="" id="name0_freeText3"></option>
</select></td>
<td class="display"><input type="text" id="name0_input3" onkeyup="meaningfulPartStringInput(0, 3);"></td>
</tr>


<tr id="name0_display4" class="display">
<td class="display">SubGenus:</td>
<td class="display"><select id="name0_select4" onchange="meaningfulPartStringSelected(0, 4)">
<option value="Euponera">Euponera</option>
<option value="OtherGenus">OtherGenus</option>
<option value="ThirdGenus">ThirdGenus</option>
<option value="FourthGenus">FourthGenus</option>
<option value="" id="name0_freeText4"></option>
</select></td>
<td class="display"><input type="text" id="name0_input4" onkeyup="meaningfulPartStringInput(0, 4);"></td>
</tr>


<tr id="name0_display5" class="display">
<td class="display">Species:</td>
<td class="display"><select id="name0_select5" onchange="meaningfulPartStringSelected(0, 5)">
<option value="laevigata">laevigata</option>
<option value="otherSpecies">otherSpecies</option>
<option value="thirdSpecies">thirdSpecies</option>
<option value="" id="name0_freeText5"></option>
</select></td>
<td class="display"><input type="text" id="name0_input5" onkeyup="meaningfulPartStringInput(0, 5);"></td>
</tr>

<tr id="name0_display6" class="display">
<td class="display">SubSpecies:</td>
<td class="display"><select id="name0_select6" onchange="meaningfulPartStringSelected(0, 6)">
<option value="laevigata">laevigata</option>
<option value="otherSpecies">otherSpecies</option>
<option value="thirdSpecies">thirdSpecies</option>
<option value="" id="name0_freeText6"></option>
</select></td>
<td class="display"><input type="text" id="name0_input6" onkeyup="meaningfulPartStringInput(0, 6);"></td>
</tr>

<tr id="name0_display7" class="display">
<td class="display">Variety:</td>
<td class="display"><select id="name0_select7" onchange="meaningfulPartStringSelected(0, 7)">
<option value="laevigata">laevigata</option>
<option value="otherSpecies">otherSpecies</option>
<option value="thirdSpecies">thirdSpecies</option>
<option value="" id="name0_freeText7"></option>
</select></td>
<td class="display"><input type="text" id="name0_input7" onkeyup="meaningfulPartStringInput(0, 7);"></td>
</tr>

</table>

</td>
</tr>
</table>

<p>Just click around and check out the web based taxonomic name attribute editor ...</p>

<input type="hidden" name="feedbackRequestId" value="EnterFeedbackRequestID">

<input type="hidden" name="name0_token0_meaning" id="name0_token0_meaning" value="3">
<input type="hidden" name="name0_token1_meaning" id="name0_token1_meaning" value="-2">
<input type="hidden" name="name0_token2_meaning" id="name0_token2_meaning" value="4">
<input type="hidden" name="name0_token3_meaning" id="name0_token3_meaning" value="-2">
<input type="hidden" name="name0_token4_meaning" id="name0_token4_meaning" value="5">
<input type="hidden" name="name0_token5_meaning" id="name0_token5_meaning" value="-2">
<input type="hidden" name="name0_token6_meaning" id="name0_token6_meaning" value="-2">
<input type="hidden" name="name0_token7_meaning" id="name0_token7_meaning" value="-2">
<input type="hidden" name="name0_token8_meaning" id="name0_token8_meaning" value="-2">
<input type="hidden" name="name0_token9_meaning" id="name0_token9_meaning" value="7">

<input type="hidden" name="name0_meaning0_string" id="name0_meaning0_string" value="">
<input type="hidden" name="name0_meaning1_string" id="name0_meaning1_string" value="">
<input type="hidden" name="name0_meaning2_string" id="name0_meaning2_string" value="">
<input type="hidden" name="name0_meaning3_string" id="name0_meaning3_string" value="Euponera">
<input type="hidden" name="name0_meaning4_string" id="name0_meaning4_string" value="Mesoponera">
<input type="hidden" name="name0_meaning5_string" id="name0_meaning5_string" value="laevigata">
<input type="hidden" name="name0_meaning6_string" id="name0_meaning6_string" value="">
<input type="hidden" name="name0_meaning7_string" id="name0_meaning7_string" value="whelpleyi">

<!--input type="hidden" name="family_index" value="-1">
<input type="hidden" name="subFamily_index" value="-1">
<input type="hidden" name="tribe_index" value="-1">
<input type="hidden" name="genus_index" value="0">
<input type="hidden" name="subGenus_index" value="2">
<input type="hidden" name="species_index" value="4">
<input type="hidden" name="subSpecies_index" value="-1">
<input type="hidden" name="variety_index" value="9"-->

<input type="submit" value="View Status" onclick="return displayState('0');">
 */
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			for (int n = 0; n < this.taxonNamePanels.size(); n++) {
				TaxonomicNameParsePanel tnpp = ((TaxonomicNameParsePanel) this.taxonNamePanels.get(n));
				tnpp.remove.setSelected("R".equals(response.getProperty(("name" + n + "_remove"), "K")));
				for (int t = 0; t < tnpp.taxonNameTokens.length; t++)
					tnpp.taxonNameTokenMeanings[t] = Integer.parseInt(response.getProperty(("name" + n + "_token" + t + "_meaning"), ("" + noMeaningIndex)));
				for (int p = 0; p < rankNames.length; p++) {
					tnpp.meaningfulPartStrings[p] = response.getProperty("name" + n + "_meaning" + p + "_string");
					if ("".equals(tnpp.meaningfulPartStrings[p]))
						tnpp.meaningfulPartStrings[p] = null;
					if (tnpp.meaningfulPartStrings[p] != null)
						tnpp.rank = rankNames[p];
				}
			}
		}
	}
}