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
package de.uka.ipd.idaho.plugins.taxonomicNames.authority;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 *
 */
public class AuthorityAugmenter extends AbstractConfigurableAnalyzer implements MonitorableAnalyzer, TaxonomicNameConstants {
	
	private static final String[] lookupRankNames = {
		KINGDOM_ATTRIBUTE,
		PHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
		GENUS_ATTRIBUTE,
		SPECIES_ATTRIBUTE,
	};
	
	private AuthorityProvider authorityProvider = null;
	
	private TaxonomicRankSystem rankSystem = TaxonomicRankSystem.getRankSystem(null);
	private TreeMap ranksByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private Rank[] primaryRanks;
	private Rank genusRank = null;
	private Rank speciesRank = null;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		System.out.println("AuthorityAugmenter: Initializing ...");
		this.authorityProvider = AuthorityProvider.getAuthorityProvider(this.dataProvider);
		System.out.println("AuthorityAugmenter: Initialized");
		
		Rank[] ranks = this.rankSystem.getRanks();
		ArrayList primaryRanks = new ArrayList();
		for (int r = 0; r < ranks.length; r++) {
			this.ranksByName.put(ranks[r].name, ranks[r]);
			if (GENUS_ATTRIBUTE.equals(ranks[r].name))
				this.genusRank = ranks[r];
			if (SPECIES_ATTRIBUTE.equals(ranks[r].name))
				this.speciesRank = ranks[r];
			if (ranks[r].name.equals(ranks[r].getRankGroup().name))
				primaryRanks.add(ranks[r]);
		}
		this.primaryRanks = ((Rank[]) primaryRanks.toArray(new Rank[primaryRanks.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		this.authorityProvider.shutdown();
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
		if (pm == null)
			pm = ProgressMonitor.dummy;
		
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
		
		//	infer authority name and year from document if taxon name has 'new <XYZ>' or '<XYZ> nov' status or label
		String docAuthorityName = this.getDocAuthorityName(data);
		String docAuthorityYear = this.getDocAuthorityYear(data);
		Annotation[] taxonNameLabels = data.getAnnotations(TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE);
		int taxonNameLabelIndex = 0;
		for (int t = 0; t < taxonNames.length; t++) {
			String tnStatus = ((String) taxonNames[t].getAttribute("status"));
			if (tnStatus == null) {
				while ((taxonNameLabelIndex < taxonNameLabels.length) && (taxonNameLabels[taxonNameLabelIndex].getStartIndex() < taxonNames[t].getEndIndex()))
					taxonNameLabelIndex++;
				if ((taxonNameLabelIndex < taxonNameLabels.length) && ((taxonNameLabels[taxonNameLabelIndex].getStartIndex() - taxonNames[t].getEndIndex()) <= 2))
					tnStatus = taxonNameLabels[taxonNameLabelIndex].getValue();
			}
			if (tnStatus == null)
				continue;
			if (tnStatus.toLowerCase().indexOf("syn") != -1)
				continue; // no inference on newly degraded junior synonyms (they tend to come with full authority anyway)
			if ((docAuthorityName != null) && !taxonNames[t].hasAttribute(AUTHORITY_NAME_ATTRIBUTE))
				taxonNames[t].setAttribute(AUTHORITY_NAME_ATTRIBUTE, docAuthorityName);
			if ((docAuthorityYear != null) && !taxonNames[t].hasAttribute(AUTHORITY_YEAR_ATTRIBUTE))
				taxonNames[t].setAttribute(AUTHORITY_YEAR_ATTRIBUTE, docAuthorityYear);
			if ((docAuthorityName != null) && (docAuthorityYear != null) && !taxonNames[t].hasAttribute(AUTHORITY_ATTRIBUTE))
				taxonNames[t].setAttribute(AUTHORITY_ATTRIBUTE, (docAuthorityName + ", " + docAuthorityYear));
		}
		
		//	bucketize taxonomic names
		pm.setBaseProgress(5);
		pm.setProgress(0);
		pm.setMaxProgress(10);
		pm.setStep("Indexing taxonomic names");
		TreeMap taxonNamesByStrings = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNames.length; t++) {
			pm.setProgress((t * 100) / taxonNames.length);
			Rank rank = ((Rank) this.ranksByName.get((String) taxonNames[t].getAttribute(RANK_ATTRIBUTE)));
			if (rank == null)
				continue;
			String tnbKey = ((String) taxonNames[t].getAttribute(rank.name));
			if (tnbKey == null)
				continue;
			if (this.speciesRank.getRelativeSignificance() < rank.getRelativeSignificance())
				tnbKey = (((String) taxonNames[t].getAttribute(SPECIES_ATTRIBUTE)) + " " + tnbKey);
			if (this.genusRank.getRelativeSignificance() < rank.getRelativeSignificance())
				tnbKey = (((String) taxonNames[t].getAttribute(GENUS_ATTRIBUTE)) + " " + tnbKey);
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			if (tnb == null) {
				tnb = new TaxonNameBucket(rank.name, tnbKey);
				taxonNamesByStrings.put(tnbKey, tnb);
			}
			tnb.add(taxonNames[t]);
		}
		
		//	split buckets for conflicting authorities if given (homonyms will always sport one)
		pm.setBaseProgress(10);
		pm.setProgress(0);
		pm.setMaxProgress(15);
		pm.setStep("Resolving in-bucket authority conflicts");
		for (Iterator eit = (new ArrayList(taxonNamesByStrings.keySet())).iterator(); eit.hasNext();) {
			String tnbKey = ((String) eit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			if (tnb.size() < 2)
				continue; // nothing to conflict with one another
			
			//	check for authority conflicts
			boolean authorityConflict = false;
			for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				String tnAuthorityName = ((String) taxonName.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
				String tnAuthorityYear = ((String) taxonName.getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
				if (tnb.authorityName == null)
					tnb.authorityName = tnAuthorityName;
				else if (tnAuthorityName != null) {
					if (tnb.authorityName.equalsIgnoreCase(tnAuthorityName)) {}
					else if (tnb.authorityName.endsWith(".") && tnAuthorityName.endsWith("."))
						authorityConflict = true; // catches 'L.' vs. 'Latr.' on grounds of documents using same and only one abbreviation per authority throughout
					else if (StringUtils.isAbbreviationOf(tnb.authorityName, tnAuthorityName, false)) {}
					else if (StringUtils.isAbbreviationOf(tnAuthorityName, tnb.authorityName, false))
						tnb.authorityName = tnAuthorityName;
					else authorityConflict = true;
				}
				if (tnb.authorityYear == null)
					tnb.authorityYear = tnAuthorityYear;
				else if (tnAuthorityYear != null) {
					if (tnb.authorityYear.endsWith(tnAuthorityYear)) {}
					else if (tnAuthorityYear.endsWith(tnb.authorityYear))
						tnb.authorityYear = tnAuthorityYear;
					else authorityConflict = true;
				}
				if (authorityConflict)
					break;
			}
			
			//	anything to do?
			if (!authorityConflict)
				continue;
			
			//	split buckets, including authority in keys
			TaxonNameBucket splitTnb = new TaxonNameBucket(tnb.rank, tnb.epithetString);
			taxonNamesByStrings.put(tnbKey, splitTnb);
			for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				String tnAuthorityName = ((String) taxonName.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
				String tnAuthorityYear = ((String) taxonName.getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
				
				//	neither authority name nor year given, scope current values
				if ((tnAuthorityName == null) && (tnAuthorityYear == null)) {
					splitTnb.add(taxonName);
					continue;
				}
				
				//	we got authority name or year, check for conflicts
				boolean tnAuthorityConflict = false;
				if (tnAuthorityName != null) {
					if (splitTnb.authorityName == null)
						tnAuthorityConflict = true;
					else if (tnb.authorityName.equalsIgnoreCase(tnAuthorityName)) {}
					else if (tnb.authorityName.endsWith(".") && tnAuthorityName.endsWith("."))
						authorityConflict = true; // catches 'L.' vs. 'Latr.' on grounds of documents using same and only one abbreviation per authority throughout
					else if (StringUtils.isAbbreviationOf(splitTnb.authorityName, tnAuthorityName, false)) {}
					else if (StringUtils.isAbbreviationOf(tnAuthorityName, splitTnb.authorityName, false)) {}
					else tnAuthorityConflict = true;
				}
				if (tnAuthorityYear != null) {
					if (splitTnb.authorityYear == null)
						tnAuthorityConflict = true;
					else if (tnb.authorityYear.equals(tnAuthorityYear)) {}
					else if (tnb.authorityYear.endsWith(tnAuthorityYear)) {}
					else if (tnAuthorityYear.endsWith(tnb.authorityYear)) {}
					else tnAuthorityConflict = true;
				}
				
				//	we have a conflict, start new bucket
				if (tnAuthorityConflict) {
					String tnTnbKey = (tnbKey + ((tnAuthorityName == null) ? "" : (" " + tnAuthorityName)) + ((tnAuthorityYear == null) ? "" : (", " + tnAuthorityYear)));
					splitTnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnTnbKey));
					if (splitTnb == null) {
						splitTnb = new TaxonNameBucket(tnb.rank, tnb.epithetString);
						splitTnb.authorityName = tnAuthorityName;
						splitTnb.authorityYear = tnAuthorityYear;
						taxonNamesByStrings.put(tnTnbKey, splitTnb);
					}
				}
				
				//	no conflict, just possibly augmentation of current bucket data
				else {
					if ((tnAuthorityName != null) && StringUtils.isAbbreviationOf(tnAuthorityName, splitTnb.authorityName, false))
						splitTnb.authorityName = tnAuthorityName;
					if ((tnAuthorityYear != null) && tnAuthorityYear.endsWith(splitTnb.authorityYear))
						splitTnb.authorityYear = tnAuthorityYear;
				}
				
				//	assign current taxon name
				splitTnb.add(taxonName);
			}
			
			//	check if no-authority main bucket has any content at all
			tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			if (tnb.isEmpty())
				taxonNamesByStrings.remove(tnbKey);
		}
		
		//	merge back compatible buckets
		pm.setBaseProgress(15);
		pm.setProgress(0);
		pm.setMaxProgress(20);
		pm.setStep("Merging compatible buckets");
		for (Iterator eit = (new ArrayList(taxonNamesByStrings.keySet())).iterator(); eit.hasNext();) {
			String tnbKey = ((String) eit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			if (tnb == null)
				continue; // this one got merged away earlier
			String mTnbKey = tnbKey;
			for (Iterator ceit = (new ArrayList(taxonNamesByStrings.keySet())).iterator(); ceit.hasNext();) {
				String cTnbKey = ((String) ceit.next());
				TaxonNameBucket cTnb = ((TaxonNameBucket) taxonNamesByStrings.get(cTnbKey));
				if (tnb == cTnb)
					continue; // no use comparing bucket to itself
				if (!tnb.epithetString.equals(cTnb.epithetString))
					continue; // no use comparing these two
				if (tnb.size() < 2)
					continue; // nothing to conflict with one another
				
				//	check for authority conflicts, and ensure we at least have one non-wildcard match
				String authorityName = tnb.authorityName;
				String authorityYear = tnb.authorityName;
				boolean authorityConflict = false;
				boolean authorityMatch = false;
				if (authorityName == null)
					authorityName = cTnb.authorityName;
				else if (cTnb.authorityName != null) {
					if (authorityName.equalsIgnoreCase(cTnb.authorityName))
						authorityMatch = true;
					else if (tnb.authorityName.endsWith(".") && cTnb.authorityName.endsWith("."))
						authorityConflict = true; // catches 'L.' vs. 'Latr.' on grounds of documents using same and only one abbreviation per authority throughout
					else if (StringUtils.isAbbreviationOf(tnb.authorityName, cTnb.authorityName, false))
						authorityMatch = true;
					else if (StringUtils.isAbbreviationOf(cTnb.authorityName, tnb.authorityName, false)) {
						authorityName = cTnb.authorityName;
						authorityMatch = true;
					}
					else authorityConflict = true;
				}
				if (authorityYear == null)
					authorityYear = cTnb.authorityYear;
				else if (cTnb.authorityYear != null) {
					if (authorityYear.equals(cTnb.authorityYear))
						authorityMatch = true;
					else if (authorityYear.endsWith(cTnb.authorityYear))
						authorityMatch = true;
					else if (cTnb.authorityYear.endsWith(authorityYear)) {
						authorityYear = cTnb.authorityYear;
						authorityMatch = true;
					}
					else authorityConflict = true;
				}
				
				//	no way we're merging these two
				if (authorityConflict)
					continue;
				
				//	no grounds for merging these two on
				if (!authorityMatch)
					continue;
				
				//	perform merger
				tnb.addAll(cTnb);
				Collections.sort(tnb, AnnotationUtils.ANNOTATION_NESTING_ORDER);
				tnb.authorityName = authorityName;
				tnb.authorityYear = authorityYear;
				taxonNamesByStrings.remove(cTnbKey);
				mTnbKey = (tnb.epithetString + ((tnb.authorityName == null) ? "" : (" " + tnb.authorityName)) + ((tnb.authorityYear == null) ? "" : (", " + tnb.authorityYear)));
			}
			
			//	did the outer bucket get a new key?
			if (!tnbKey.equals(mTnbKey))
				taxonNamesByStrings.put(mTnbKey, taxonNamesByStrings.remove(tnbKey));
		}
		
		//	do attribute inference based on previously imported attributes
		pm.setBaseProgress(20);
		pm.setProgress(0);
		pm.setMaxProgress(25);
		pm.setStep("Transferring attributes already present in document");
		for (Iterator eit = taxonNamesByStrings.keySet().iterator(); eit.hasNext();) {
			String tnbKey = ((String) eit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			if (tnb.size() < 2)
				continue; // nothing to do transfer between
			
			//	transfer authority attributes
			tnb.writeAuthority(toProcessTaxonNameIDs, pm);
		}
		
		//	do we have a data source for further processing?
		if (this.authorityProvider == null)
			return;
		
		//	sort out buckets we're done with
		pm.setBaseProgress(25);
		pm.setProgress(0);
		pm.setMaxProgress(30);
		pm.setStep("Sorting out done-with buckets");
		for (Iterator eit = (new ArrayList(taxonNamesByStrings.keySet())).iterator(); eit.hasNext();) {
			String tnbKey = ((String) eit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			if (tnb.authorityYear == null)
				continue; // blank year to fill
			if (tnb.authorityName == null)
				continue; // blank name to fill
			if (tnb.authorityYear.endsWith("."))
				continue; // abbreviation to expand
			taxonNamesByStrings.remove(tnbKey);
		}
		
		//	anything left to do?
		if (taxonNamesByStrings.isEmpty())
			return;
		
		//	get document authority names and years
		CountingSet docAuthorities = new CountingSet(String.CASE_INSENSITIVE_ORDER);
		for (int t = 0; t < taxonNames.length; t++) {
			if (taxonNames[t].hasAttribute(AUTHORITY_NAME_ATTRIBUTE) && taxonNames[t].hasAttribute(AUTHORITY_YEAR_ATTRIBUTE))
				docAuthorities.add(taxonNames[t].getAttribute(AUTHORITY_NAME_ATTRIBUTE) + ", " + taxonNames[t].getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
		}
		this.addBibliographicAuthorities(data, docAuthorities);
		
		//	get authority for each bucket
		pm.setBaseProgress(30);
		pm.setProgress(0);
		pm.setMaxProgress(75);
		pm.setStep("Loading authority data for " + taxonNamesByStrings.size() + " taxon names");
		boolean allowWebLookup = parameters.containsKey(ONLINE_PARAMETER);
		boolean canAskAllowWebLookup = true;
		for (Iterator eit = (new ArrayList(taxonNamesByStrings.keySet())).iterator(); eit.hasNext();) {
			String tnbKey = ((String) eit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(tnbKey));
			pm.setInfo(" - for " + tnb.rank + " " + tnbKey);
			Properties authority = this.authorityProvider.getAuthority(tnb.epithetString.split("\\s+"), tnb.rank, tnb.authorityName, ((tnb.authorityYear == null) ? -1 : Integer.parseInt(tnb.authorityYear)), allowWebLookup);
			
			//	ask for permission for provider lookup if in offline mode and no higher taxonomy in cache
			if ((authority == null) && !allowWebLookup && canAskAllowWebLookup && parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal()) {
				int choice = JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("Unable to find higher taxonomy for " + tnb.rank + " '" + tnbKey + "' in local cache.\nAllow provider lookup despite offline mode?"), "Allow Higher Taxonomy Lookup?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				allowWebLookup = (choice == JOptionPane.YES_OPTION);
				canAskAllowWebLookup = false;
				authority = this.authorityProvider.getAuthority(tnb.epithetString.split("\\s+"), tnb.rank, tnb.authorityName, ((tnb.authorityYear == null) ? -1 : Integer.parseInt(tnb.authorityYear)), allowWebLookup);
			}
			
			//	do we have anything to work with?
			if (authority == null) {
				tnb.authorities = new Authority[0];
				continue;
			}
			
			//	extract potentially aggregated authorities
			Properties[] authorities = AuthorityProvider.extractAuthorities(authority);
			
			//	this one's unambiguous
			if (authorities.length == 1) {
				tnb.authorityName = authorities[0].getProperty(AUTHORITY_NAME_ATTRIBUTE);
				tnb.authorityYear = authorities[0].getProperty(AUTHORITY_YEAR_ATTRIBUTE);
				
				//	transfer authority attributes
				tnb.writeAuthority(toProcessTaxonNameIDs, pm);
				
				//	we're done with this one
				taxonNamesByStrings.remove(tnbKey);
				continue;
			}
			
			//	score, wrap, and index authorities
			tnb.authorities = new Authority[authorities.length];
			for (int a = 0; a < authorities.length; a++) {
				
				//	score authority
				int aScore = 0;
				for (Iterator dait = docAuthorities.iterator(); dait.hasNext();) {
					String docAuthority = ((String) dait.next());
					int docAuthorityScore = docAuthorities.getCount(docAuthority);
					if ((tnb.authorityName == null) || !StringUtils.isAbbreviationOf(docAuthority, tnb.authorityName, false))
						continue;
					if ((tnb.authorityYear == null) || !StringUtils.isAbbreviationOf(docAuthority, tnb.authorityYear, false))
						continue;
					aScore += docAuthorityScore;
				}
				
				//	wrap authority
				tnb.authorities[a] = new Authority(authorities[a], tnb.epithetString, tnb.rank, aScore);
			}
			
			//	sort higher taxonomies by score if more than one found
			if (1 < tnb.authorities.length)
				Arrays.sort(tnb.authorities, authorityComparator);
		}
		
		//	we're done here if we're not allowed to prompt user
		if (!parameters.containsKey(Analyzer.INTERACTIVE_PARAMETER))
			return;
		
		//	prepare getting feedback
		pm.setBaseProgress(75);
		pm.setProgress(0);
		pm.setMaxProgress(90);
		pm.setStep("Getting user feedback for ambiguous authorities");
		LinkedList tnbList = new LinkedList();
		for (Iterator leit = taxonNamesByStrings.keySet().iterator(); leit.hasNext();) {
			String tnbKey = ((String) leit.next());
			tnbList.add(taxonNamesByStrings.get(tnbKey));
		}
		LinkedHashSet functionalOptions = new LinkedHashSet();
		functionalOptions.add(IGNORE_NAME_CHOICE);
		functionalOptions.add(SEPARATE_NAMES_CHOICE);
		functionalOptions.add(ENTER_MANUALLY_CHOICE);
		
		//	get feedback (the loop runs at most twice, the second time without the bucket separation options)
		while (tnbList.size() != 0) {
			boolean feedbackCancelled = this.getFeedback(((TaxonNameBucket[]) tnbList.toArray(new TaxonNameBucket[tnbList.size()])), data, functionalOptions); 
			
			//	process feedback
			LinkedList sTnbList = new LinkedList();
			for (Iterator tnbit = tnbList.iterator(); tnbit.hasNext();) {
				TaxonNameBucket tnb = ((TaxonNameBucket) tnbit.next());
				if (tnb.authority == null)
					continue; // can only happen if dialog cancelled, so nevermind ...
				
				//	manually enter authority
				if (tnb.authority == MANUALLY)
					continue;
				
				//	select authority for each individual taxon name
				else if (tnb.authority == SEPARATE_NAMES) {
					TreeMap sTaxonNamesByStrings = new TreeMap();
					for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
						Annotation taxonName = ((Annotation) tnit.next());
						String tnAuthorityName = ((String) taxonName.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
						String tnAuthorityYear = ((String) taxonName.getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
						String sTnbKey = (tnb.epithetString + ((tnAuthorityName == null) ? "" : (" " + tnAuthorityName)) + ((tnAuthorityYear == null) ? "" : (", " + tnAuthorityYear)));
						TaxonNameBucket sTnb = ((TaxonNameBucket) sTaxonNamesByStrings.get(sTnbKey));
						if (sTnb == null) {
							sTnb = new TaxonNameBucket(tnb.rank, tnb.epithetString);
							sTaxonNamesByStrings.put(sTnbKey, sTnb);
							sTnb.authorities = tnb.authorities;
							sTnbList.add(sTnb);
						}
						sTnb.add(taxonName);
					}
				}
				
				//	ignore taxon names epithet for now
				else if (tnb.authority == IGNORE) {
					for (Iterator tnit = tnb.iterator(); tnit.hasNext();)
						toProcessTaxonNameIDs.remove(((Annotation) tnit.next()).getAnnotationID());
				}
				
				//	authority selected
				else tnb.writeAuthority(toProcessTaxonNameIDs, pm);
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
			if (!functionalOptions.remove(SEPARATE_NAMES_CHOICE))
				break;
		}
		
		//	sort out all taxonomic names already processed
		pm.setBaseProgress(90);
		pm.setProgress(0);
		pm.setMaxProgress(95);
		pm.setStep("Collecting remaining taxon names");
		for (Iterator seit = taxonNamesByStrings.keySet().iterator(); seit.hasNext();) {
			String significantEpithet = ((String) seit.next());
			TaxonNameBucket tnb = ((TaxonNameBucket) taxonNamesByStrings.get(significantEpithet));
			for (Iterator tnit = tnb.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				if (!toProcessTaxonNameIDs.contains(taxonName.getAnnotationID()))
					tnit.remove();
			}
			if (tnb.isEmpty())
				seit.remove();
		}
		
		//	build manual input feedback panels for all un-processed taxonomic names
		LinkedList manualInputFeedbackPanelList = new LinkedList();
		for (Iterator seit = taxonNamesByStrings.keySet().iterator(); seit.hasNext();) {
			String tnbKey = ((String) seit.next());
			manualInputFeedbackPanelList.addLast(new EnterAuthorityFeedbackPanel((TaxonNameBucket) taxonNamesByStrings.get(tnbKey)));
		}
		
		//	get feedback
		pm.setBaseProgress(95);
		pm.setProgress(0);
		pm.setMaxProgress(100);
		pm.setStep("Getting user input for unavailable authorities");
		EnterAuthorityFeedbackPanel[] manualEntryFeedbackPanels = ((EnterAuthorityFeedbackPanel[]) manualInputFeedbackPanelList.toArray(new EnterAuthorityFeedbackPanel[manualInputFeedbackPanelList.size()]));
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
			manualEntryFeedbackPanels[d].writeAuthorities();
	}
	
	private class TaxonNameBucket extends ArrayList {
		String rank;
		String epithetString;
		String authorityName;
		String authorityYear;
		Authority[] authorities;
		Authority authority;
		TaxonNameBucket(String significantRank, String significantEpithet) {
			this.rank = significantRank;
			this.epithetString = significantEpithet;
		}
		void writeAuthority(HashSet toProcessTaxonNameIDs, ProgressMonitor pm) {
			if (this.authority != null) {
				this.authorityName = this.authority.getProperty(AUTHORITY_NAME_ATTRIBUTE, this.authorityName);
				this.authorityYear = this.authority.getProperty(AUTHORITY_YEAR_ATTRIBUTE, this.authorityYear);
			}
			if ((this.authorityName == null) || (this.authorityYear == null))
				return;
			pm.setInfo("Setting authority for " + this.epithetString + " to '" + this.authorityName + ", " + this.authorityYear + "'");
			for (Iterator tnit = this.iterator(); tnit.hasNext();) {
				Annotation taxonName = ((Annotation) tnit.next());
				pm.setInfo(" - " + taxonName.toXML());
				if (this.authorityName != null)
					taxonName.setAttribute(AUTHORITY_NAME_ATTRIBUTE, this.authorityName);
				if (this.authorityYear != null)
					taxonName.setAttribute(AUTHORITY_YEAR_ATTRIBUTE, this.authorityYear);
				toProcessTaxonNameIDs.remove(taxonName.getAnnotationID());
			}
		}
	}
	
	private static class Authority extends Properties {
		String epithetString;
		String rank;
		int score;
		Authority(Properties defaults, String epithetString, String rank, int score) {
			super(defaults);
			this.epithetString = epithetString;
			this.rank = rank;
			this.score = score;
		}
		public String toString() {
			if (this.toString == null) {
				StringBuffer sb = new StringBuffer(this.epithetString);
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
				String as = this.getProperty(AuthorityProvider.AUTHORITY_SOURCE_ATTRIBUTE);
				if (as != null)
					sb.append(", from " + as);
				sb.append(")");
				this.scoreSourceString = sb.toString();
			}
			return this.scoreSourceString;
		}
		private String scoreSourceString = null;
	}
	
	private static final Comparator authorityComparator = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			Authority ht1 = ((Authority) obj1);
			Authority ht2 = ((Authority) obj2);
			if (ht1.score == ht2.score)
				return ht1.toString().compareToIgnoreCase(ht2.toString());
			else return (ht2.score - ht1.score);
		}
	};
	
	private String getDocAuthorityName(MutableAnnotation data) {
		String docAuthorityName = ((String) data.getAttribute(DOCUMENT_AUTHOR_ATTRIBUTE, data.getDocumentProperty(DOCUMENT_AUTHOR_ATTRIBUTE)));
		if (docAuthorityName == null)
			return null;
		StringBuffer docAuthorityNames = new StringBuffer();
		this.addDocAuthorityName(docAuthorityName, docAuthorityNames);
		return docAuthorityNames.toString();
	}
	
	private void addDocAuthorityName(String author, StringBuffer docAuthorityNames) {
		if (author.indexOf('&') == -1) {
			if (author.indexOf(',') != -1)
				author = author.substring(0, author.indexOf(',')).trim();
			if (docAuthorityNames.length() != 0)
				docAuthorityNames.append(" & ");
			docAuthorityNames.append(author);
		}
		else {
			String[] authors = author.split("\\s*\\&\\s*");
			for (int a = 0; a < authors.length; a++)
				this.addDocAuthorityName(authors[a], docAuthorityNames);
			//	TODO maybe restrict this to first three or five authors
		}
	}
	
	private String getDocAuthorityYear(MutableAnnotation data) {
		return ((String) data.getAttribute(DOCUMENT_DATE_ATTRIBUTE, data.getDocumentProperty(DOCUMENT_DATE_ATTRIBUTE)));
	}
	
	private void addBibliographicAuthorities(MutableAnnotation doc, CountingSet docAuthorities) {
		String docAuthor = ((String) doc.getAttribute(DOCUMENT_AUTHOR_ATTRIBUTE));
		String docYear = ((String) doc.getAttribute(DOCUMENT_DATE_ATTRIBUTE));
		if ((docAuthor != null) && (docYear != null))
			this.addBibliographicAuthority(docAuthor, docYear, docAuthorities);
		QueriableAnnotation[] bibRefs = doc.getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		if (bibRefs.length == 0)
			return; // BibRefUtils only required from next line, so this should work even without them on classpath
		for (int r = 0; r < bibRefs.length; r++) {
			String refAuthor = ((String) bibRefs[r].getAttribute(BibRefUtils.AUTHOR_ANNOTATION_TYPE));
			String refYear = ((String) doc.getAttribute(BibRefUtils.YEAR_ANNOTATION_TYPE));
			if ((refAuthor != null) && (refYear != null)) {
				this.addBibliographicAuthority(refAuthor, refYear, docAuthorities);
				continue;
			}
			Annotation[] authors = bibRefs[r].getAnnotations(BibRefUtils.AUTHOR_ANNOTATION_TYPE);
			if (authors.length == 0)
				continue;
			Annotation[] year = bibRefs[r].getAnnotations(BibRefUtils.YEAR_ANNOTATION_TYPE);
			if (year.length == 0)
				continue;
			for (int a = 0; a < authors.length; a++)
				this.addBibliographicAuthority(authors[a].getValue(), year[0].getValue(), docAuthorities);
		}
	}
	
	private void addBibliographicAuthority(String author, String year, CountingSet docAuthorities) {
		if (author.indexOf('&') == -1) {
			if (author.indexOf(',') != -1)
				author = author.substring(0, author.indexOf(',')).trim();
			docAuthorities.add(author + "," + year);
		}
		else {
			String[] authors = author.split("\\s*\\&\\s*");
			for (int a = 0; a < authors.length; a++)
				this.addBibliographicAuthority(authors[a], year, docAuthorities);
			//	TODO maybe restrict this to first three or five authors
		}
	}
	
	//	special dummy location data sets to indicate special actions
	private static final Authority IGNORE = new Authority(null, "", "", 0); 
	private static final Authority SEPARATE_NAMES = new Authority(null, "", "", 0); 
	private static final Authority MANUALLY = new Authority(null, "", "", 0);
	
	//	special options to indicate special actions
	private static final String IGNORE_NAME_CHOICE = "Ignore taxon name";
	private static final String SEPARATE_NAMES_CHOICE = "Handle taxon names individually";
	private static final String ENTER_MANUALLY_CHOICE = "Enter Manually";
	
	//	return false only if interrupted
	private boolean getFeedback(TaxonNameBucket[] taxonNameBuckets, TokenSequence text, LinkedHashSet functionalOptions) {
		
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
			adfps[d] = new AssignmentDisambiguationFeedbackPanel("Check Authorities");
			adfps[d].setLabel("<HTML>Please select the appropriate authority for these <B>taxonomic names</B>." +
					"<BR>Select <I>&lt;Ignore epithet&gt;</I> to not assign the name any authority now." +
					"<BR>Select <I>&lt;Enter Manually&gt;</I> to manually enter the authority.</HTML>");
			int dialogOffset = (d * dialogSize);
			for (int b = 0; (b < dialogSize) && ((b + dialogOffset) < taxonNameBuckets.length); b++) {
				
				//	put locations in array
				String[] hierarchyOptions;
				String selectedHierarchyOption = ENTER_MANUALLY_CHOICE;
				
				hierarchyOptions = new String[taxonNameBuckets[b + dialogOffset].authorities.length + functionalOptions.size()];
				System.arraycopy(((String[]) functionalOptions.toArray(new String[functionalOptions.size()])), 0, hierarchyOptions, 0, functionalOptions.size());
				for (int h = 0; h < taxonNameBuckets[b + dialogOffset].authorities.length; h++) {
					hierarchyOptions[h + functionalOptions.size()] = (taxonNameBuckets[b + dialogOffset].authorities[h].toString() + " " + taxonNameBuckets[b + dialogOffset].authorities[h].getScoreSourceString());
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
			adfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].get(0)).getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].get(0)).getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].get(0)).getAttribute(PAGE_ID_ATTRIBUTE, "").toString());
			adfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, ((Annotation) taxonNameBuckets[dialogOffset].get(0)).getAnnotationID());
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
			if (IGNORE_NAME_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].authority = IGNORE;
			
			//	epithet selected for manual handling
			else if (SEPARATE_NAMES_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].authority = SEPARATE_NAMES;
			
			//	epithet selected for manual handling
			else if (ENTER_MANUALLY_CHOICE.equals(selectedOptions[b]))
				taxonNameBuckets[b].authority = MANUALLY;
			
			//	find higher taxonomy for selected option
			else for (int t = 0; t < taxonNameBuckets[b].authorities.length; t++) {
				if (selectedOptions[b].startsWith(taxonNameBuckets[b].authorities[t].toString())) {
					taxonNameBuckets[b].authority = taxonNameBuckets[b].authorities[t];
					t = taxonNameBuckets[b].authorities.length;
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
	
	private class EnterAuthorityFeedbackPanel extends FeedbackPanel {
		private EnterAuthorityPanel[] authorityPanels;
		EnterAuthorityFeedbackPanel(TaxonNameBucket tnb) {
			super("Complete Higher Taxonomy for " + tnb.rank.substring(0, 1).toUpperCase() + tnb.rank.substring(1) + " " + tnb.epithetString);
			this.setLabel("<HTML>Enter/complete the authorities for " + tnb.rank + " <B><I>" + tnb.epithetString + "</I></B>.</HTML>");
			
			JPanel taxonNamePanel = new JPanel(new GridBagLayout(), true);
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
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			taxonNamePanel.add(new JLabel("Verbatim Taxon Name", JLabel.CENTER), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 0;
			taxonNamePanel.add(new JLabel("Authority Name", JLabel.CENTER), gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			taxonNamePanel.add(new JLabel("Authority Year", JLabel.CENTER), gbc.clone());
			gbc.gridy++;
			
			this.authorityPanels = new EnterAuthorityPanel[tnb.size()];
			for (int n = 0; n < tnb.size(); n++) {
				Annotation taxonName = ((Annotation) tnb.get(n));
				this.authorityPanels[n] = new EnterAuthorityPanel(tnb.rank, taxonName);
				gbc.gridx = 0;
				gbc.weightx = 1;
				taxonNamePanel.add(this.authorityPanels[n].label, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				taxonNamePanel.add(this.authorityPanels[n].authorityName, gbc.clone());
				gbc.gridx = 2;
				gbc.weightx = 0;
				taxonNamePanel.add(this.authorityPanels[n].authorityYear, gbc.clone());
				gbc.gridy++;
			}
			
			this.add(taxonNamePanel, BorderLayout.CENTER);
		}
		public int getDecisionCount() {
			return this.authorityPanels.length;
		}
		public int getDecisionComplexity() {
			return 10; // about ... this is about manual data entry, and thus somewhat complex
		}
		public int getComplexity() {
			return (this.getDecisionCount() * this.getDecisionComplexity());
		}
		public Properties getFieldStates() {
			Properties states = new Properties();
			for (int p = 0; p < this.authorityPanels.length; p++) {
				states.setProperty((AUTHORITY_NAME_ATTRIBUTE + p), this.authorityPanels[p].authorityName.getText());
				states.setProperty((AUTHORITY_YEAR_ATTRIBUTE + p), this.authorityPanels[p].authorityYear.getText());
			}
			return states;
		}
		public void setFieldStates(Properties states) {
			for (int p = 0; p < this.authorityPanels.length; p++) {
				this.authorityPanels[p].authorityName.setText(states.getProperty((AUTHORITY_NAME_ATTRIBUTE + p), this.authorityPanels[p].authorityName.getText()));
				this.authorityPanels[p].authorityYear.setText(states.getProperty((AUTHORITY_YEAR_ATTRIBUTE + p), this.authorityPanels[p].authorityYear.getText()));
			}
		}
		void writeAuthorities() {
			for (int p = 0; p < this.authorityPanels.length; p++)
				this.authorityPanels[p].writeAuthority();
		}
		private class EnterAuthorityPanel {
			private String rank;
			private Annotation taxonName;
			JLabel label;
			JTextField authorityName = new JTextField();
			JTextField authorityYear = new JTextField();
			EnterAuthorityPanel(String rank, Annotation taxonName) {
				this.rank = rank;
				this.taxonName = taxonName;
				this.label = new JLabel((this.rank.substring(0, 1).toUpperCase() + this.rank.substring(1) + " " + this.taxonName.getValue()), JLabel.LEFT);
				this.authorityName.setText((String) this.taxonName.getAttribute(AUTHORITY_NAME_ATTRIBUTE, ""));
				this.authorityYear.setText((String) this.taxonName.getAttribute(AUTHORITY_YEAR_ATTRIBUTE, ""));
			}
			void writeAuthority() {
				String authorityName = this.authorityName.getText();
				String authorityYear = this.authorityYear.getText();
				if (authorityName != null)
					this.taxonName.setAttribute(AUTHORITY_NAME_ATTRIBUTE, authorityName);
				if (authorityYear != null)
					this.taxonName.setAttribute(AUTHORITY_YEAR_ATTRIBUTE, authorityYear);
				this.taxonName.setAttribute(AUTHORITY_YEAR_ATTRIBUTE, "Manual Input");
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File docFile = new File("E:/Projektdaten/testDocs/Catapaguroides_bythos.xml");
		BufferedReader docIn = new BufferedReader(new InputStreamReader(new FileInputStream(docFile), "UTF-8"));
		MutableAnnotation doc = SgmlDocumentReader.readDocument(docIn);
		docIn.close();
		AuthorityAugmenter aa = new AuthorityAugmenter();
		aa.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AuthorityAugmenterData/")) {
//			public boolean isDataEditable(String dataName) {
//				return false;
//			}
//			public boolean isDataEditable() {
//				return false;
//			}
		});
		Properties params = new Properties();
		params.setProperty(Analyzer.ONLINE_PARAMETER, Analyzer.ONLINE_PARAMETER);
		aa.process(doc, params);
	}
}