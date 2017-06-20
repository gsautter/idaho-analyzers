/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.stringUtils.StringIndex;

/**
 * @author sautter
 */
public class HigherTaxonomyImporter extends AbstractConfigurableAnalyzer implements MonitorableAnalyzer, TaxonomicNameConstants {
	
	private static final String[] lookupRankNames = {
		KINGDOM_ATTRIBUTE,
		PHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
		GENUS_ATTRIBUTE,
	};
	
	private HigherTaxonomyProvider higherTaxonomyProvider = null;
	
	private TaxonomicRankSystem rankSystem = TaxonomicRankSystem.getRankSystem(null);
	private TreeMap ranksByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		System.out.println("HigherTaxonomyImporter: Initializing ...");
		this.higherTaxonomyProvider = HigherTaxonomyProvider.getTaxonomyProvider(this.dataProvider);
		System.out.println("HigherTaxonomyProvider: Initialized");
		
		Rank[] ranks = this.rankSystem.getRanks();
		for (int r = 0; r < ranks.length; r++)
			this.ranksByName.put(ranks[r].name, ranks[r]);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		this.higherTaxonomyProvider.shutdown();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		this.process(data, parameters, ProgressMonitor.dummy);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
	 */
	public void process(MutableAnnotation data, Properties parameters, ProgressMonitor pm) {
		
		//	do we have a data source?
		if (this.higherTaxonomyProvider == null)
			return;
		
		//	prepare progress monitor
		pm.setBaseProgress(0);
		pm.setProgress(0);
		pm.setMaxProgress(5);
		pm.setStep("Getting taxonomic names");
		
		//	get taxonomic names, and remember un-processed ones
		Annotation[] taxonNames = data.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE);
		if (taxonNames.length == 0)
			return;
		HashSet toProcessTaxonNameIDs = new HashSet();
		for (int t = 0; t < taxonNames.length; t++)
			toProcessTaxonNameIDs.add(taxonNames[t].getAnnotationID());
		
		//	bucketize taxonomic names by significant epithet (the genus as the lowest)
		pm.setBaseProgress(5);
		pm.setProgress(0);
		pm.setMaxProgress(10);
		pm.setStep("Indexing taxonomic names");
		TreeMap taxonNamesBySignificantEpithet = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			String significantRank = GENUS_ATTRIBUTE;
			String significantEpithet = ((String) taxonNames[t].getAttribute(GENUS_ATTRIBUTE));
			if ((significantEpithet == null) && taxonNames[t].hasAttribute(RANK_ATTRIBUTE)) {
				significantRank = ((String) taxonNames[t].getAttribute(RANK_ATTRIBUTE));
				significantEpithet = ((String) taxonNames[t].getAttribute(significantRank));
			}
			if (significantEpithet == null)
				continue;
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesBySignificantEpithet.get(significantEpithet));
			if (tnb == null) {
				tnb = new TaxonNameBucket(significantRank, significantEpithet);
				taxonNamesBySignificantEpithet.put(significantEpithet, tnb);
			}
			tnb.add(taxonNames[t]);
		}
		
		//	do attribute inference based on previously imported attributes
		pm.setBaseProgress(10);
		pm.setProgress(0);
		pm.setMaxProgress(15);
		pm.setStep("Transferring attributes already present in document");
		for (Iterator eit = taxonNamesBySignificantEpithet.keySet().iterator(); eit.hasNext();) {
			String significantEpithet = ((String) eit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesBySignificantEpithet.get(significantEpithet));
			if (tnb.size() < 2)
				continue; // nothing to do inference with
			
			//	collect higher epithets
			Properties higherEpithets = new Properties();
			for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				for (int r = 0; r < lookupRankNames.length; r++) {
					String epithet = ((String) taxonName.getAttribute(lookupRankNames[r]));
					if (epithet != null)
						higherEpithets.setProperty(lookupRankNames[r], epithet);
				}
			}
			
			//	transfer higher epithets
			for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				for (int r = 0; r < lookupRankNames.length; r++) {
					if (taxonName.hasAttribute(lookupRankNames[r]))
						continue;
					String epithet = higherEpithets.getProperty(lookupRankNames[r]);
					if (epithet != null)
						taxonName.setAttribute(lookupRankNames[r], epithet);
				}
			}
		}
		
		//	bucketize taxonomic names by genus or higher primary rank
		pm.setBaseProgress(15);
		pm.setProgress(0);
		pm.setMaxProgress(20);
		pm.setStep("Bucketizing taxonomic names for lookup");
		TreeMap taxonNameBucketsByLookupEpithet = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			String lookupEpithet = null;
			String lookupRank = null;
			for (int r = 0; r < lookupRankNames.length; r++) {
				lookupEpithet = ((String) taxonNames[t].getAttribute(lookupRankNames[r]));
				if (lookupEpithet != null) {
					lookupRank = lookupRankNames[r];
					break;
				}
			}
			if (KINGDOM_ATTRIBUTE.equals(lookupRank)) {
				toProcessTaxonNameIDs.remove(taxonNames[t].getAnnotationID());
				continue; // this one's been handled before
			}
			if (lookupEpithet == null)
				continue;
			LookupTaxonNameBucket tnb = ((LookupTaxonNameBucket) taxonNameBucketsByLookupEpithet.get(lookupEpithet));
			if (tnb == null) {
				tnb = new LookupTaxonNameBucket(lookupRank, lookupEpithet);
				taxonNameBucketsByLookupEpithet.put(lookupEpithet, tnb);
			}
			tnb.addLast(taxonNames[t]);
		}
		
		//	either nothing we could do anything about, or everything handled by inference
		if (toProcessTaxonNameIDs.isEmpty()) {
			pm.setBaseProgress(100);
			pm.setMaxProgress(100);
			pm.setProgress(0);
			return;
		}
		
		//	collect higher rank epithets already present in document
		pm.setBaseProgress(20);
		pm.setProgress(0);
		pm.setMaxProgress(25);
		pm.setStep("Collecting taxonomic epithets for scoring");
		TreeMap epithetSetsByRank = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			for (int r = 0; r < lookupRankNames.length; r++) {
				String epithet = ((String) taxonNames[t].getAttribute(lookupRankNames[r]));
				if (epithet == null)
					continue;
				TreeSet epithetSet = ((TreeSet) epithetSetsByRank.get(lookupRankNames[r]));
				if (epithetSet == null) {
					epithetSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
					epithetSetsByRank.put(lookupRankNames[r], epithetSet);
				}
				epithetSet.add(epithet);
			}
		}
		
		//	get higher taxonomy for each bucket, and index higher taxonomies
		pm.setBaseProgress(25);
		pm.setProgress(0);
		pm.setMaxProgress(75);
		pm.setStep("Loading higher taxonomy data");
		boolean allowWebLookup = parameters.containsKey(ONLINE_PARAMETER);
		boolean canAskAllowWebLookup = true;
		TreeMap epithetIndexesByRank = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		String[] lookupEpithets = ((String[]) taxonNameBucketsByLookupEpithet.keySet().toArray(new String[taxonNameBucketsByLookupEpithet.size()]));
		for (int l = 0; l < lookupEpithets.length; l++) {
			pm.setProgress((l * 100) / lookupEpithets.length);
			LookupTaxonNameBucket tnb = ((LookupTaxonNameBucket) taxonNameBucketsByLookupEpithet.get(lookupEpithets[l]));
			pm.setInfo(" - for " + tnb.lookupRank + " '" + tnb.lookupEpithet + "'");
			Properties higherTaxonomy = this.higherTaxonomyProvider.getHierarchy(tnb.lookupEpithet, tnb.lookupRank, allowWebLookup);
			
			//	ask for permission for provider lookup if in offline mode and no higher taxonomy in cache
			if ((higherTaxonomy == null) && !allowWebLookup && canAskAllowWebLookup && parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal()) {
				int choice = JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("Unable to find higher taxonomy for " + tnb.lookupRank + " '" + tnb.lookupEpithet + "' in local cache.\nAllow provider lookup despite offline mode?"), "Allow Higher Taxonomy Lookup?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				allowWebLookup = (choice == JOptionPane.YES_OPTION);
				canAskAllowWebLookup = false;
				higherTaxonomy = this.higherTaxonomyProvider.getHierarchy(tnb.lookupEpithet, tnb.lookupRank, allowWebLookup);
			}
			
			//	do we have anything to work with?
			if (higherTaxonomy == null) {
				tnb.higherTaxonomies = new HigherTaxonomy[0];
				continue;
			}
			Properties[] higherTaxonomies = HigherTaxonomyProvider.extractHierarchies(higherTaxonomy);
			
			//	score, wrap, and index hierarchies
			tnb.higherTaxonomies = new HigherTaxonomy[higherTaxonomies.length];
			for (int h = 0; h < higherTaxonomies.length; h++) {
				
				//	score hierarchy
				int hhScore = 0;
				for (int r = 0; r < lookupRankNames.length; r++) {
					if ((r != 0) && lookupRankNames[r-1].equals(tnb.lookupRank))
						break; // do score lookup epithet (yes, as it always matches, but it also scores up projected hierarchies for higher up epithets)
					String epithet = higherTaxonomies[h].getProperty(lookupRankNames[r]);
					if (epithet == null)
						continue;
					TreeSet epithetSet = ((TreeSet) epithetSetsByRank.get(lookupRankNames[r]));
					if (epithetSet == null)
						continue;
					if (epithetSet.contains(epithet))
						hhScore += (r+1);
				}
				
				//	wrap hierarchy
				tnb.higherTaxonomies[h] = new HigherTaxonomy(higherTaxonomies[h], tnb.lookupEpithet, tnb.lookupRank, hhScore);
				
				//	index hierarchy (no use indexing by kingdom, though)
				for (int r = 1; r < lookupRankNames.length; r++) {
					String epithet = tnb.higherTaxonomies[h].getProperty(lookupRankNames[r]);
					if (epithet == null)
						continue;
					TreeMap epithetIndex = ((TreeMap) epithetIndexesByRank.get(lookupRankNames[r]));
					if (epithetIndex == null) {
						epithetIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
						epithetIndexesByRank.put(lookupRankNames[r], epithetIndex);
					}
					TreeSet epithetIndexEntry = ((TreeSet) epithetIndex.get(epithet));
					if (epithetIndexEntry == null) {
						epithetIndexEntry = new TreeSet(higherTaxonomyComparator);
						epithetIndex.put(epithet, epithetIndexEntry);
					}
					epithetIndexEntry.add(tnb.higherTaxonomies[h].projectTo(lookupRankNames[r]));
					if (lookupRankNames[r].equals(tnb.lookupRank))
						break; // no use indexing beyond lookup rank
				}
			}
			
			//	sort higher taxonomies by score if more than one found
			if (1 < tnb.higherTaxonomies.length)
				Arrays.sort(tnb.higherTaxonomies, higherTaxonomyComparator);
		}
		
		//	prepare getting feedback
		pm.setBaseProgress(75);
		pm.setProgress(0);
		pm.setMaxProgress(90);
		pm.setStep("Getting user feedback for higher taxonomies");
		LinkedList tnbList = new LinkedList();
		for (Iterator leit = taxonNameBucketsByLookupEpithet.keySet().iterator(); leit.hasNext();) {
			String lookupEpithet = ((String) leit.next());
			tnbList.addLast(taxonNameBucketsByLookupEpithet.get(lookupEpithet));
		}
		LinkedHashSet functionalOptions = new LinkedHashSet();
		functionalOptions.add(IGNORE_EPITHET_CHOICE);
		functionalOptions.add(SEPARATE_TAXA_CHOICE);
		functionalOptions.add(SEPARATE_NAMES_CHOICE);
		functionalOptions.add(ENTER_MANUALLY_CHOICE);
		
		//	get feedback (the loop runs at most twice, the second time without the bucket separation options)
		while (tnbList.size() != 0) {
			boolean feedbackCancelled = this.getFeedback(((LookupTaxonNameBucket[]) tnbList.toArray(new LookupTaxonNameBucket[tnbList.size()])), data, functionalOptions); 
			
			//	process feedback
			LinkedList sTnbList = new LinkedList();
			for (Iterator tnbit = tnbList.iterator(); tnbit.hasNext();) {
				LookupTaxonNameBucket tnb = ((LookupTaxonNameBucket) tnbit.next());
				if (tnb.higherTaxonomy == null)
					continue; // can only happen if dialog cancelled, so nevermind ...
				
				//	manually enter higher taxonomy
				if (tnb.higherTaxonomy == MANUALLY)
					continue;
				
				//	select higher taxonomy for each individual taxon name
				else if (tnb.higherTaxonomy == SEPARATE_TAXA) {
					TreeMap sTnbsBySignificantEpithet = new TreeMap();
					for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
						Annotation taxonName = ((Annotation) tnit.next());
						String significantRank = GENUS_ATTRIBUTE;
						String significantEpithet = ((String) taxonName.getAttribute(GENUS_ATTRIBUTE));
						if (taxonName.hasAttribute(RANK_ATTRIBUTE)) {
							significantRank = ((String) taxonName.getAttribute(RANK_ATTRIBUTE));
							significantEpithet = ((String) taxonName.getAttribute(significantRank));
						}
						LookupTaxonNameBucket sTnb = ((LookupTaxonNameBucket) sTnbsBySignificantEpithet.get(significantRank + "=" + significantEpithet));
						if (sTnb == null) {
							sTnb = new LookupTaxonNameBucket(tnb.lookupRank, tnb.lookupEpithet);
							sTnbsBySignificantEpithet.put((significantRank + "=" + significantEpithet), sTnb);
							sTnb.higherTaxonomies = tnb.higherTaxonomies;
							sTnbList.add(sTnb);
						}
						sTnb.add(taxonName);
					}
				}
				
				//	select higher taxonomy for each individual taxon name annotation
				else if (tnb.higherTaxonomy == SEPARATE_NAMES) {
					for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
						Annotation taxonName = ((Annotation) tnit.next());
						LookupTaxonNameBucket sTnb = new LookupTaxonNameBucket(tnb.lookupRank, tnb.lookupEpithet);
						sTnb.higherTaxonomies = tnb.higherTaxonomies;
						sTnbList.add(sTnb);
						sTnb.add(taxonName);
					}
				}
				
				//	ignore taxonomic epithet for now
				else if (tnb.higherTaxonomy == IGNORE) {
					for (Iterator tnit = tnb.iterator(); tnit.hasNext();)
						toProcessTaxonNameIDs.remove(((Annotation) tnit.next()).getAnnotationID());
				}
				
				//	higher taxonomy selected
				else tnb.writeHigherTaxonomy(toProcessTaxonNameIDs);
			}
			
			//	cancelled and thus not allowed to open further dialogs, or nothing left to do, or not local
			if (feedbackCancelled || toProcessTaxonNameIDs.isEmpty() || !FeedbackPanel.isLocal()) {
				pm.setBaseProgress(100);
				pm.setMaxProgress(100);
				pm.setProgress(0);
				return;
			}
			
			//	prepare processing split buckets
			tnbList = sTnbList;
			if (!functionalOptions.remove(SEPARATE_TAXA_CHOICE) | !functionalOptions.remove(SEPARATE_NAMES_CHOICE))
				break;
		}
		
		//	sort out all taxonomic names already processed
		pm.setBaseProgress(90);
		pm.setProgress(0);
		pm.setMaxProgress(95);
		pm.setStep("Collecting remaining taxonomic names");
		for (Iterator seit = taxonNamesBySignificantEpithet.keySet().iterator(); seit.hasNext();) {
			String significantEpithet = ((String) seit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesBySignificantEpithet.get(significantEpithet));
			for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				if (!toProcessTaxonNameIDs.contains(taxonName.getAnnotationID()))
					tnit.remove();
			}
			if (tnb.isEmpty())
				seit.remove();
		}
		
		//	build manual input feedback panels for all un-processed taxonomic names (also covers standalone tribes, etc., which we couldn't find a lookup epithet for)
		LinkedList manualInputFeedbackPanelList = new LinkedList();
		for (Iterator seit = taxonNamesBySignificantEpithet.keySet().iterator(); seit.hasNext();) {
			String significantEpithet = ((String) seit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesBySignificantEpithet.get(significantEpithet));
			
			//	collect lookup ranks
			int tnbRankSignificance = ((Rank) this.ranksByName.get(tnb.significantRank)).getRelativeSignificance();
			LinkedList tnbLookupRankList = new LinkedList();
			for (int r = 0; r < lookupRankNames.length; r++) {
				int lookupRankSignificance = ((Rank) this.ranksByName.get(lookupRankNames[r])).getRelativeSignificance();
				if (lookupRankSignificance < tnbRankSignificance)
					tnbLookupRankList.addLast(lookupRankNames[r]);
				else break;
			}
			if (tnbLookupRankList.size() != 0)
				manualInputFeedbackPanelList.addLast(new EnterHigherTaxonomyFeedbackPanel(tnb, ((String[]) tnbLookupRankList.toArray(new String[tnbLookupRankList.size()])), epithetIndexesByRank));
		}
		
		//	get feedback
		pm.setBaseProgress(95);
		pm.setProgress(0);
		pm.setMaxProgress(100);
		pm.setStep("Getting user input for unavailable higher taxonomies");
		EnterHigherTaxonomyFeedbackPanel[] manualEntryFeedbackPanels = ((EnterHigherTaxonomyFeedbackPanel[]) manualInputFeedbackPanelList.toArray(new EnterHigherTaxonomyFeedbackPanel[manualInputFeedbackPanelList.size()]));
		int cutoffIndex = manualEntryFeedbackPanels.length;
		for (int d = 0; d < manualEntryFeedbackPanels.length; d++) {
			pm.setProgress((d * 100) / manualEntryFeedbackPanels.length);
			if (d != 0)
				manualEntryFeedbackPanels[d].addButton("Previous");
			manualEntryFeedbackPanels[d].addButton("Cancel");
			manualEntryFeedbackPanels[d].addButton("OK" + (((d+1) == manualEntryFeedbackPanels.length) ? "" : " & Next"));
			
			String title = manualEntryFeedbackPanels[d].getTitle();
			manualEntryFeedbackPanels[d].setTitle(title + " - (" + (d+1) + " of " + manualEntryFeedbackPanels.length + ")");
			
			String f = manualEntryFeedbackPanels[d].getFeedback();
			if (f == null) f = "Cancel";
			
			manualEntryFeedbackPanels[d].setTitle(title);
			
			//	back to previous dialog
			if ("Previous".equals(f))
				d-=2;
			
			//	current dialog submitted, process data
			else if (!f.startsWith("OK")) {
				cutoffIndex = d;
				break;
			}
		}
		
		//	process feedback
		for (int d = 0; d < cutoffIndex; d++)
			manualEntryFeedbackPanels[d].writeHigherTaxonomy();
	}
	
	private class TaxonNameBucket extends LinkedList {
		String significantRank;
		String significantEpithet;
		TaxonNameBucket(String significantRank, String significantEpithet) {
			this.significantRank = significantRank;
			this.significantEpithet = significantEpithet;
		}
	}
	
	private class LookupTaxonNameBucket extends LinkedList {
		String lookupRank;
		String lookupEpithet;
		HigherTaxonomy[] higherTaxonomies = null;
		HigherTaxonomy higherTaxonomy = null;
		LookupTaxonNameBucket(String lookupRank, String lookupEpithet) {
			this.lookupRank = lookupRank;
			this.lookupEpithet = lookupEpithet;
		}
		void writeHigherTaxonomy(HashSet toProcessTaxonNameIDs) {
			for (Iterator tnit = this.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				for (int r = 0; r < lookupRankNames.length; r++) {
					if (lookupRankNames[r].equals(this.higherTaxonomy.rank))
						break;
					if (taxonName.hasAttribute(lookupRankNames[r]))
						continue;
					String epithet = this.higherTaxonomy.getProperty(lookupRankNames[r]);
					if (epithet != null)
						taxonName.setAttribute(lookupRankNames[r], epithet);
				}
				String authority = this.higherTaxonomy.getProperty(AUTHORITY_ATTRIBUTE);
				if ((authority != null) && !taxonName.hasAttribute(AUTHORITY_ATTRIBUTE))
					taxonName.setAttribute(AUTHORITY_ATTRIBUTE, authority);
				String authorityName = this.higherTaxonomy.getProperty(AUTHORITY_NAME_ATTRIBUTE);
				if ((authorityName != null) && !taxonName.hasAttribute(AUTHORITY_NAME_ATTRIBUTE))
					taxonName.setAttribute(AUTHORITY_NAME_ATTRIBUTE, authorityName);
				String authorityYear = this.higherTaxonomy.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
				if ((authorityYear != null) && !taxonName.hasAttribute(AUTHORITY_YEAR_ATTRIBUTE))
					taxonName.setAttribute(AUTHORITY_YEAR_ATTRIBUTE, authorityYear);
				String hts = this.higherTaxonomy.getProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE);
				if (hts != null)
					taxonName.setAttribute(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, hts);
				toProcessTaxonNameIDs.remove(taxonName.getAnnotationID());
			}
		}
	}
	
	private static class HigherTaxonomy extends Properties {
		String epithet;
		String rank;
		int score;
		HigherTaxonomy(Properties defaults, String epithet, String rank, int score) {
			super(defaults);
			this.epithet = epithet;
			this.rank = rank;
			this.score = score;
		}
		public String toString() {
			if (this.toString == null) {
				StringBuffer sb = new StringBuffer();
				for (int r = 0; r < lookupRankNames.length; r++) {
					String epithet = this.getProperty(lookupRankNames[r]);
					if (epithet == null)
						continue;
					if (sb.length() != 0)
						sb.append(" - ");
					sb.append(epithet);
					if (lookupRankNames[r].equals(this.rank))
						break;
				}
				String authorityName = this.getProperty(AUTHORITY_NAME_ATTRIBUTE);
				if (authorityName != null)
					sb.append(", " + authorityName);
				String authorityYear = this.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
				if (authorityYear != null)
					sb.append(" " + authorityYear);
				this.toString = sb.toString();
			}
			return this.toString;
		}
		private String toString = null;
		String getScoreSourceString() {
			if (this.scoreSourceString == null) {
				StringBuffer sb = new StringBuffer("(score ");
				sb.append(this.score);
				String hts = this.getProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE);
				if (hts != null)
					sb.append(", from " + hts);
				sb.append(")");
				this.scoreSourceString = sb.toString();
			}
			return this.scoreSourceString;
		}
		private String scoreSourceString = null;
		HigherTaxonomy projectTo(String rank) {
			if (this.rank.equals(rank))
				return this;
			String rankEpithet = this.getProperty(rank);
			if (rankEpithet == null)
				return null;
			HigherTaxonomy projection = new HigherTaxonomy(this, rankEpithet, rank, this.score);
			for (int r = 0; r < lookupRankNames.length; r++) {
				String epithet = this.getProperty(lookupRankNames[r]);
				if (epithet != null)
					projection.setProperty(lookupRankNames[r], epithet);
				if (lookupRankNames[r].equals(rank))
					break;
			}
			return projection;
		}
	}
	
	private static final Comparator higherTaxonomyComparator = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			HigherTaxonomy ht1 = ((HigherTaxonomy) obj1);
			HigherTaxonomy ht2 = ((HigherTaxonomy) obj2);
			if (ht1.score == ht2.score)
				return ht1.toString().compareToIgnoreCase(ht2.toString());
			else return (ht2.score - ht1.score);
		}
	};
	
	//	special dummy location data sets to indicate special actions
	private static final HigherTaxonomy IGNORE = new HigherTaxonomy(null, "", "", 0); 
	private static final HigherTaxonomy SEPARATE_TAXA = new HigherTaxonomy(null, "", "", 0); 
	private static final HigherTaxonomy SEPARATE_NAMES = new HigherTaxonomy(null, "", "", 0); 
	private static final HigherTaxonomy MANUALLY = new HigherTaxonomy(null, "", "", 0);
	
	//	special options to indicate special actions
	private static final String IGNORE_EPITHET_CHOICE = "Ignore epithet";
	private static final String SEPARATE_TAXA_CHOICE = "Handle taxa individually";
	private static final String SEPARATE_NAMES_CHOICE = "Handle taxon names individually";
	private static final String ENTER_MANUALLY_CHOICE = "Enter Manually";
	
	//	return false only if interrupted
	private boolean getFeedback(LookupTaxonNameBucket[] taxonNameBuckets, TokenSequence text, LinkedHashSet functionalOptions) {
		
		//	don't show empty dialog
		if (taxonNameBuckets.length == 0)
			return false;
		
		//	create store for feedback results
		String[] selectedOptions = new String[taxonNameBuckets.length];
		
		//	compute number of sets per dialog
		int dialogCount = ((taxonNameBuckets.length + 9) / 10);
		int dialogSize = ((taxonNameBuckets.length + (dialogCount / 2)) / dialogCount);
		dialogCount = ((taxonNameBuckets.length + dialogSize - 1) / dialogSize);
		
		//	build dialogs
		AssignmentDisambiguationFeedbackPanel[] adfps = new AssignmentDisambiguationFeedbackPanel[dialogCount];
		for (int d = 0; d < adfps.length; d++) {
			adfps[d] = new AssignmentDisambiguationFeedbackPanel("Check Higher Taxonomy of Genera");
			adfps[d].setLabel("<HTML>Please select the appropriate higher taxonomy for these <B>taxonomic epithets</B>." +
					"<BR>Select <I>&lt;Ignore epithet&gt;</I> to not assign the epithet any higher taxonomy now." +
					"<BR>Select <I>&lt;Enter Manually&gt;</I> to manually enter the higher taxonomy.</HTML>");
			int dialogOffset = (d * dialogSize);
			for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < taxonNameBuckets.length); b++) {
				
				//	put locations in array
				String[] hierarchyOptions;
				String selectedHierarchyOption = ENTER_MANUALLY_CHOICE;
				
				hierarchyOptions = new String[taxonNameBuckets[b + dialogOffset].higherTaxonomies.length + functionalOptions.size()];
				System.arraycopy(((String[]) functionalOptions.toArray(new String[functionalOptions.size()])), 0, hierarchyOptions, 0, functionalOptions.size());
				for (int h = 0; h < taxonNameBuckets[b + dialogOffset].higherTaxonomies.length; h++) {
					hierarchyOptions[h + functionalOptions.size()] = (taxonNameBuckets[b + dialogOffset].higherTaxonomies[h].toString() + " " + taxonNameBuckets[b + dialogOffset].higherTaxonomies[h].getScoreSourceString());
					if (h == 0)
						selectedHierarchyOption = hierarchyOptions[h + functionalOptions.size()];
				}
				
				//	count occurrences of distinct name strings
				StringIndex taxonNameStringCounts = new StringIndex();
				for (Iterator tnit = taxonNameBuckets[b + dialogOffset].iterator(); tnit.hasNext();) {
					Annotation taxonName = ((Annotation) tnit.next());
					taxonNameStringCounts.add(TokenSequenceUtils.concatTokens(taxonName, true, true));
				}
				
				//	build epithet & context display (showing each name string only once, though)
				StringBuffer epithetContext = new StringBuffer("<HTML>");
				for (Iterator tnit = taxonNameBuckets[b + dialogOffset].iterator(); tnit.hasNext();) {
					Annotation taxonName = ((Annotation) tnit.next());
					String taxonNameString = TokenSequenceUtils.concatTokens(taxonName, true, true);
					int taxonNameStringCount = taxonNameStringCounts.getCount(taxonNameString);
					if (taxonNameStringCount == 0)
						continue;
					if (epithetContext.length() > "<HTML>".length())
						epithetContext.append("<BR>");
					epithetContext.append(buildLabel(text, taxonName, 10));
					if (taxonName.hasAttribute(PAGE_NUMBER_ATTRIBUTE)) {
						if (taxonNameStringCount > 1)
							epithetContext.append(" (" + taxonNameStringCount + " occurrences, first on page " + taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")");
						else epithetContext.append(" (page " + taxonName.getAttribute(PAGE_NUMBER_ATTRIBUTE) + ")");
					}
					else if (taxonNameStringCount > 1)
						epithetContext.append(" (" + taxonNameStringCount + " occurrences)");
					taxonNameStringCounts.removeAll(taxonNameString);
				}
				epithetContext.append("</HTML>");
				
				//	add to feedback panel
				adfps[d].addLine(epithetContext.toString(), hierarchyOptions, selectedHierarchyOption);
			}
			
			//	add background information
			adfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].getFirst()).getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].getFirst()).getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].getFirst()).getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].getFirst()).getAnnotationID());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, TAXONOMIC_NAME_ANNOTATION_TYPE);
			
			//	add target page numbers
			String targetPages = FeedbackPanel.getTargetPageString((Annotation[]) taxonNameBuckets[dialogOffset].toArray(new Annotation[taxonNameBuckets[dialogOffset].size()]));
			if (targetPages != null)
				adfps[d].setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, targetPages);
			String targetPageIDs = FeedbackPanel.getTargetPageIdString((Annotation[]) taxonNameBuckets[dialogOffset].toArray(new Annotation[taxonNameBuckets[dialogOffset].size()]));
			if (targetPageIDs != null)
				adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, targetPageIDs);
		}
		
		//	show feedback dialogs
		int cutoffSet = taxonNameBuckets.length;
		boolean feedbackCancelled = false;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(adfps);
			
			//	process all feedback data together
			for (int d = 0; d < adfps.length; d++) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < adfps[d].lineCount(); b++)
					selectedOptions[b + dialogOffset] = adfps[d].getSelectedOptionAt(b);
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < adfps.length; d++) {
			if (d != 0)
				adfps[d].addButton("Previous");
			adfps[d].addButton("Cancel");
			adfps[d].addButton("OK" + (((d+1) == adfps.length) ? "" : " & Next"));
			
			String title = adfps[d].getTitle();
			adfps[d].setTitle(title + " - (" + (d+1) + " of " + adfps.length + ")");
			
			String f = adfps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			adfps[d].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < adfps[d].lineCount(); b++)
					selectedOptions[b + dialogOffset] = adfps[d].getSelectedOptionAt(b);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog onward
			else {
				cutoffSet = (d * dialogSize);
				feedbackCancelled = true;
				break;
			}
		}
		
		//	process feedback
		for (int b = 0; b < cutoffSet; b++) {
			
			//	epithet cannot be handled right now
			if (IGNORE_EPITHET_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].higherTaxonomy = IGNORE;
			
			//	epithet selected for manual handling
			else if (SEPARATE_TAXA_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].higherTaxonomy = SEPARATE_TAXA;
			
			//	epithet selected for manual handling
			else if (SEPARATE_NAMES_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].higherTaxonomy = SEPARATE_NAMES;
			
			//	epithet selected for manual handling
			else if (ENTER_MANUALLY_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].higherTaxonomy = MANUALLY;
			
			//	find higher taxonomy for selected option
			else for (int t = 0; t < taxonNameBuckets[b].higherTaxonomies.length; t++) {
				if (selectedOptions[b].startsWith(taxonNameBuckets[b].higherTaxonomies[t].toString())) {
					taxonNameBuckets[b].higherTaxonomy = taxonNameBuckets[b].higherTaxonomies[t];
					t = taxonNameBuckets[b].higherTaxonomies.length;
				}
			}
		}
		
		//	finally ...
		return feedbackCancelled;
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
	
	private class EnterHigherTaxonomyFeedbackPanel extends FeedbackPanel {
		private EnterHigherEpithetPanel[] epithetPanels;
		private TaxonNameBucket taxonNameBucket;
		private TreeMap epithetIndexesByRank;
		private String higherTaxonomySource = "Manual Input";
		EnterHigherTaxonomyFeedbackPanel(TaxonNameBucket tnb, String[] ranksToAdd, TreeMap epithetIndexesByRank) {
			super("Complete Higher Taxonomy for " + tnb.significantRank.substring(0, 1).toUpperCase() + tnb.significantRank.substring(1) + " " + tnb.significantEpithet);
			this.taxonNameBucket = tnb;
			this.epithetIndexesByRank = epithetIndexesByRank;
			this.setLabel("<HTML>Enter/complete the higher taxonomy for " + tnb.significantRank + " <B><I>" + tnb.significantEpithet + "</I></B>." +
					"<BR>If you enter at least three characters in any of the fields and hit <I>Enter</I>," +
					"<BR>your entry will be matched against all higher taxonomies imported before," +
					"<BR>in an effort to automatically fill in the remaining fields.</HTML>");
			
			JPanel epithetPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.gridy = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			
			this.epithetPanels = new EnterHigherEpithetPanel[ranksToAdd.length];
			for (int r = 0; r < ranksToAdd.length; r++) {
				this.epithetPanels[r] = new EnterHigherEpithetPanel(ranksToAdd[r]);
				gbc.gridx = 0;
				gbc.weightx = 0;
				epithetPanel.add(this.epithetPanels[r].rankLabel, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				epithetPanel.add(this.epithetPanels[r].epithetInput, gbc.clone());
				gbc.gridy++;
			}
			
			this.add(epithetPanel, BorderLayout.CENTER);
		}
		
		private void attemptAutoComplete(String rank, String epithetPrefix) {
			
			//	get higher taxonomy index for rank
			TreeMap epithetIndex = ((TreeMap) this.epithetIndexesByRank.get(rank));
			if (epithetIndex == null)
				return;
			
			//	collect matching index entries
			epithetPrefix = epithetPrefix.toLowerCase();
			TreeSet epithetIndexEntries = new TreeSet(higherTaxonomyComparator);
			for (Iterator eit = epithetIndex.keySet().iterator(); eit.hasNext();) {
				String epithet = ((String) eit.next());
				if (epithet.toLowerCase().startsWith(epithetPrefix))
					epithetIndexEntries.addAll((TreeSet) epithetIndex.get(epithet));
			}
			
			//	do we have anything to work with
			if (epithetIndexEntries.isEmpty()) {
				this.higherTaxonomySource = "Manual Input";
				return;
			}
			HigherTaxonomy ht = ((HigherTaxonomy) epithetIndexEntries.first());
			
			//	fill in fields with best match
			for (int e = 0; e < this.epithetPanels.length; e++) {
				String htEpithet = ht.getProperty(this.epithetPanels[e].rank);
				if (htEpithet != null)
					this.epithetPanels[e].epithetInput.setText(htEpithet);
				if (rank.equals(this.epithetPanels[e].rank))
					break;
			}
			
			//	update source name
			if (ht.getProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE) != null)
				this.higherTaxonomySource = ("Manual Selection from " + ht.getProperty(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE));
		}
		
		public int getDecisionCount() {
			return this.epithetPanels.length;
		}
		public int getDecisionComplexity() {
			return 10; // about ... this is about manual data entry, and thus somewhat complex
		}
		public int getComplexity() {
			return (this.getDecisionCount() * this.getDecisionComplexity());
		}
		
		public Properties getFieldStates() {
			Properties states = new Properties();
			for (int e = 0; e < this.epithetPanels.length; e++)
				states.setProperty(this.epithetPanels[e].rank, this.epithetPanels[e].epithetInput.getText());
			return states;
		}
		public void setFieldStates(Properties states) {
			for (int e = 0; e < this.epithetPanels.length; e++)
				this.epithetPanels[e].epithetInput.setText(states.getProperty(this.epithetPanels[e].rank, this.epithetPanels[e].epithetInput.getText()));
		}
		
		void writeHigherTaxonomy() {
			for (Iterator tnit = this.taxonNameBucket.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				for (int e = 0; e < this.epithetPanels.length; e++) {
					if (taxonName.hasAttribute(this.epithetPanels[e].rank))
						continue;
					String epithet = this.epithetPanels[e].epithetInput.getText().trim();
					if (epithet.length() != 0)
						taxonName.setAttribute(this.epithetPanels[e].rank, epithet);
				}
				taxonName.setAttribute(HigherTaxonomyProvider.HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, this.higherTaxonomySource);
			}
		}
		
		private class EnterHigherEpithetPanel {
			private String rank;
			JLabel rankLabel;
			JTextField epithetInput = new JTextField();
			EnterHigherEpithetPanel(String rank) {
				this.rank = rank;
				this.rankLabel = new JLabel((this.rank.substring(0, 1).toUpperCase() + this.rank.substring(1)), JLabel.LEFT);
				this.epithetInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String epithetPrefix = epithetInput.getText();
						if (epithetPrefix.length() >= 3)
							attemptAutoComplete(EnterHigherEpithetPanel.this.rank, epithetPrefix);
					}
				});
			}
		}
	}
}