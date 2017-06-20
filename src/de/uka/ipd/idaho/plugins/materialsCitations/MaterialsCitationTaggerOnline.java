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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AnnotationEditorFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AnnotationEditorFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.plugins.locations.CountryHandler;
import de.uka.ipd.idaho.plugins.locations.CountryHandler.RegionHandler;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class MaterialsCitationTaggerOnline extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants {
	
	/* TODO use _density_ of hinting annotations (coordinates, full dates, countries, regions, etc.) for ranking
	 * - finds the few hits when hints are sparse
	 * - require more than one hint when they are plentiful
	 */
	
	private static final String SEPARATORS = ",;.:";
	
	private StringVector sectionSelectors = new StringVector();
	private StringVector indicatorAnnotationTypes = new StringVector();
	private LinkedHashMap detailAttributePaths = new LinkedHashMap();
	
	private StringVector countries = new StringVector();
	private CountryHandler countryHandler;
	
	private StringVector collectingRegionRegExes = new StringVector();
	private StringVector collectingRegionExcludeRegExes = new StringVector();
	private StringVector collectingRegionExcludeTokens = new StringVector();
	
	private StringVector materialsCitationSeparators = new StringVector();
	
	private static final String collectingRegionRegEx = "[A-Z][^\\:\\.\\;\\[\\]\\(\\)]++\\:";
	private static final Dictionary caseSensitiveNoise = new Dictionary() {
		private StringVector content = Gamta.getNoiseWords();
		public StringIterator getEntryIterator() {
			return this.content.getEntryIterator();
		}
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		public boolean isEmpty() {
			return this.content.isEmpty();
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return this.content.lookup(string, caseSensitive);
		}
		public boolean lookup(String string) {
			return this.content.contains(string);
		}
		public int size() {
			return this.content.size();
		}
	};
	
	private TreeMap typeStatuses = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private Dictionary typeStatusDict = new Dictionary() {
		public boolean lookup(String string) {
			return typeStatuses.containsKey(string);
		}
		public boolean lookup(String string, boolean caseSensitive) {
			return typeStatuses.containsKey(string);
		}
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		public boolean isEmpty() {
			return typeStatuses.isEmpty();
		}
		public int size() {
			return typeStatuses.size();
		}
		public StringIterator getEntryIterator() {
			final Iterator it = typeStatuses.keySet().iterator();
			return new StringIterator() {
				public void remove() {}
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					return it.next();
				}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
	};
	
	private TreeMap specimenTypes = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			InputStream is = this.dataProvider.getInputStream("relevantTypes.txt");
			StringVector typeLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeLines.size(); t++) {
				String typeLine = typeLines.get(t).trim();
				if ((typeLine.length() != 0) && !typeLine.startsWith("//")) {
					int split = typeLine.indexOf(' ');
					if (split != -1) {
						String type = typeLine.substring(0, split).trim();
						Color color = FeedbackPanel.getColor(typeLine.substring(split).trim());
						this.highlightAttributeCache.put(type, color);
					}
				}
			}
		} catch (IOException fnfe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream("sectionSelectors.txt");
			this.sectionSelectors = StringVector.loadList(is);
			is.close();
		} catch (IOException fnfe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream("indicatorAnnotationTypes.txt");
			this.indicatorAnnotationTypes = StringVector.loadList(is);
			is.close();
		} catch (IOException fnfe) {}
		this.indicatorAnnotationTypes.addElementIgnoreDuplicates(GEO_COORDINATE_TYPE);
		this.indicatorAnnotationTypes.addElementIgnoreDuplicates(DATE_TYPE);
		this.indicatorAnnotationTypes.addElementIgnoreDuplicates(QUANTITY_TYPE);
		this.indicatorAnnotationTypes.addElementIgnoreDuplicates(TYPE_STATUS);
		this.indicatorAnnotationTypes.addElementIgnoreDuplicates(COLLECTION_CODE);
		
		try {
			InputStream is = this.dataProvider.getInputStream("detailAttributePaths.txt");
			StringVector detailAttributePaths = StringVector.loadList(is);
			is.close();
			for (int a = 0; a < detailAttributePaths.size(); a++) {
				String detailAttributeLine = detailAttributePaths.get(a).trim();
				if ((detailAttributeLine.length() == 0) || detailAttributeLine.startsWith("//"))
					continue;
				int split = detailAttributeLine.indexOf("=");
				if (split == -1)
					continue;
				String detailAttributeName = detailAttributeLine.substring(0, split).trim();
				GPath detailAttributePath = new GPath(detailAttributeLine.substring(split + "=".length()).trim());
				this.detailAttributePaths.put(detailAttributePath, detailAttributeName);
			}
		} catch (Exception fnfe) {}
		
		this.countryHandler = CountryHandler.getCountryHandler((InputStream) null);
		for (StringIterator cit = this.countryHandler.getEntryIterator(); cit.hasNext();) {
			String country = cit.nextString();
			country = this.countryHandler.getEnglishName(country);
			this.countries.addElementIgnoreDuplicates(country);
		}
		this.countries.sortLexicographically(false, false);
		
		try {
			InputStream is = this.dataProvider.getInputStream(COLLECTING_REGION_ANNOTATION_TYPE + ".regEx.txt");
			this.collectingRegionRegExes = StringVector.loadList(is);
			is.close();
		} catch (IOException fnfe) {}
		if (this.collectingRegionRegExes.isEmpty())
			this.collectingRegionRegExes.addElement(collectingRegionRegEx);
		try {
			InputStream is = this.dataProvider.getInputStream(COLLECTING_REGION_ANNOTATION_TYPE + ".regEx.exclude.txt");
			this.collectingRegionExcludeRegExes = StringVector.loadList(is);
			is.close();
		} catch (IOException fnfe) {}
		try {
			InputStream is = this.dataProvider.getInputStream(COLLECTING_REGION_ANNOTATION_TYPE + ".exclude.txt");
			this.collectingRegionExcludeTokens = StringVector.loadList(is);
			is.close();
		} catch (IOException fnfe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream(MATERIALS_CITATION_ANNOTATION_TYPE + "Separators.txt");
			this.materialsCitationSeparators = StringVector.loadList(is);
			is.close();
		} catch (IOException fnfe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream(TYPE_STATUS + ".txt");
			BufferedReader tsr = new BufferedReader(new InputStreamReader(is, "utf-8"));
			for (String tsl; (tsl = tsr.readLine()) != null;) {
				tsl = tsl.trim();
				if (tsl.startsWith("//") || (tsl.length() == 0))
					continue;
				tsl = tsl.toLowerCase();
				if (tsl.indexOf("==>") == -1) {
					this.typeStatuses.put(tsl, tsl);
					continue;
				}
				String ts = tsl.substring(0, tsl.indexOf("==>")).trim();
				String mts = tsl.substring(tsl.indexOf("==>") + "==>".length()).trim();
				this.typeStatuses.put(mts, mts);
				this.typeStatuses.put(ts, mts);
			}
			tsr.close();
		} catch (IOException ioe) {}
		
		try {
			InputStream is = this.dataProvider.getInputStream("specimenTypes.txt");
			BufferedReader str = new BufferedReader(new InputStreamReader(is, "utf-8"));
			for (String sptl; (sptl = str.readLine()) != null;) {
				sptl = sptl.trim();
				if (sptl.startsWith("//") || (sptl.length() == 0) || (sptl.indexOf("==>") == -1))
					continue;
				sptl = sptl.toLowerCase();
				String spt = sptl.substring(0, sptl.indexOf("==>")).trim();
				String mspt = sptl.substring(sptl.indexOf("==>") + "==>".length()).trim();
				this.specimenTypes.put(mspt, mspt);
				this.specimenTypes.put(spt, mspt);
				if (spt.endsWith("."))
					this.specimenTypes.put(spt.substring(0, (spt.length() - ".".length())).trim(), mspt);
			}
			str.close();
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get treatments
		MutableAnnotation[] treatments = data.getMutableAnnotations("treatment");
		
		//	store taxons for treatments paragraphs
		Properties paragraphTaxons = new Properties();
		for (int t = 0; t < treatments.length; t++) {
			
			//	get taxon
			Annotation[] taxonNames = treatments[t].getAnnotations("taxonomicName");
			
			//	store taxon for treatment paragraphs
			if (taxonNames.length != 0) {
				String taxon = taxonNames[0].getValue();
				MutableAnnotation[] treatmentParagraphs = treatments[t].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				for (int p = 0; p < treatmentParagraphs.length; p++)
					paragraphTaxons.setProperty(treatmentParagraphs[p].getAnnotationID(), taxon);
			}
		}
		
		//	get document paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	single paragraph, may be intentional re-invocation
		if (paragraphs.length == 1) {
			
			//	get existing materials citations
			MutableAnnotation[] materialsCitations = paragraphs[0].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
			
			//	initialize country attribute
			String country = null;
			
			//	no materials citations so far, do country/region tagging
			if (materialsCitations.length == 0) {
				
				//	annotate collecting countries
				this.annotateCollectingCountries(paragraphs[0]);
				
				//	remove duplicates
				AnnotationFilter.removeDuplicates(paragraphs[0], COLLECTING_COUNTRY_ANNOTATION_TYPE);
				
				//	annotate regions
				this.annotateRegions(paragraphs[0], new StringVector(), new StringVector());
				
				//	get feedback
				CountryRegionFeedbackPanel crfp = this.produceCountryRegionFeedbackPanel(paragraphs[0], paragraphTaxons.getProperty(paragraphs[0].getAnnotationID()));
				crfp.addButton("Cancel");
				crfp.addButton("OK");
				String f = crfp.getFeedback();
				
				//	cancelled or excluded
				if ("Cancel".equals(f) || crfp.exclude.isSelected()) {
					AnnotationFilter.removeAnnotations(paragraphs[0], COLLECTING_COUNTRY_ANNOTATION_TYPE);
					AnnotationFilter.removeAnnotations(paragraphs[0], COLLECTING_REGION_ANNOTATION_TYPE);
					return;
				}
				
				//	committed, annotate countries and regions
				crfp.writeChanges(paragraphs[0]);
				
				//	get country
				country = crfp.country.getSelectedItem().toString();
				
				//	annotate materials citations
				this.annotateMaterialsCitations(paragraphs[0]);
				
				//	find frequent punctuation patterns in paragraphs & use for splitting
				this.splitMaterialsCitations(paragraphs[0]);
			}
			
			//	extract country from existing materials citations
			else for (int m = 0; m < materialsCitations.length; m++) {
				if (materialsCitations[m].hasAttribute(COUNTRY_ATTRIBUTE))
					country = ((String) materialsCitations[m].getAttribute(COUNTRY_ATTRIBUTE));
			}
			
			//	show paragraph for correcting materials citations
			MaterialsCitationFeedbackPanel mcfp = this.produceMaterialsCitationFeedbackPanel(paragraphs[0], paragraphTaxons.getProperty(paragraphs[0].getAnnotationID()));
			mcfp.addButton("Cancel");
			mcfp.addButton("OK");
			String f = mcfp.getFeedback();
			
			//	cancelled or excluded
			if ("Cancel".equals(f) || mcfp.exclude.isSelected()) {
				AnnotationFilter.removeAnnotations(paragraphs[0], COLLECTING_COUNTRY_ANNOTATION_TYPE);
				AnnotationFilter.removeAnnotations(paragraphs[0], COLLECTING_REGION_ANNOTATION_TYPE);
				AnnotationFilter.removeAnnotations(paragraphs[0], MATERIALS_CITATION_ANNOTATION_TYPE);
				return;
			}
			
			//	committed, annotate materials citations
			this.writeChanges(mcfp, paragraphs[0]);
			
			//	set country & region attributes
			String region = null;
			materialsCitations = paragraphs[0].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
			for (int m = 0; m < materialsCitations.length; m++) {
				
				//	switch country if any is annotated
				Annotation[] countries = materialsCitations[m].getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
				if (countries.length != 0) {
					country = this.normalizeProperName(countries[0].getValue());
					if (this.countryHandler != null)
						country = this.countryHandler.getEnglishName(country);
				}
				
				//	set country attribute
				if (country != null)
					materialsCitations[m].setAttribute(COUNTRY_ATTRIBUTE, country);
				
				//	switch region if any is annotated
				Annotation[] regions = materialsCitations[m].getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
				if (regions.length != 0)
					region = this.normalizeProperName(regions[0].getValue());
				
				//	set region attribute
				if (region != null)
					materialsCitations[m].setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
				
				//	add further details from marker annotations
				this.setDetailAttributes(materialsCitations[m]);
			}
			
			//	we're done
			return;
		}
		
		
		//	multi feedback available, do all paragraphs in parallel
		if (FeedbackPanel.isMultiFeedbackEnabled() && !FeedbackPanel.isLocal()) {
			
			//	filter out interesting paragraphs
			ArrayList candidateParagraphList = new ArrayList();
			for (int p = 0; p < paragraphs.length; p++) {
				
				//	get evidence (must be annotated before)
				int indicatorAnnotationCount = 0;
				for (int i = 0; (i < this.indicatorAnnotationTypes.size()) && (indicatorAnnotationCount == 0); i++)
					indicatorAnnotationCount += paragraphs[p].getAnnotations(this.indicatorAnnotationTypes.get(i)).length;
				
				//	paragraph that may contain collecting events
				if (indicatorAnnotationCount != 0)
					candidateParagraphList.add(paragraphs[p]);
			}
			
			//	no interesting paragraphs found
			if (candidateParagraphList.isEmpty()) return;
			
			//	get collected candidates
			paragraphs = ((MutableAnnotation[]) candidateParagraphList.toArray(new MutableAnnotation[candidateParagraphList.size()]));
			
			//	annotate collecting countries
			for (int p = 0; p < paragraphs.length; p++)
				this.annotateCollectingCountries(paragraphs[p]);
			
			//	remove duplicates
			for (int p = 0; p < paragraphs.length; p++)
				AnnotationFilter.removeDuplicates(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
			
			//	annotate regions
			for (int p = 0; p < paragraphs.length; p++)
				this.annotateRegions(paragraphs[p], new StringVector(), new StringVector());
			
			//	build feedback panels
			CountryRegionFeedbackPanel[] crfps = new CountryRegionFeedbackPanel[paragraphs.length];
			for (int p = 0; p < paragraphs.length; p++)
				crfps[p] = this.produceCountryRegionFeedbackPanel(paragraphs[p], paragraphTaxons.getProperty(paragraphs[p].getAnnotationID()));
			
			//	display countries and regions for correction
			FeedbackPanel.getMultiFeedback(crfps);
			
			//	process feedback data
			ArrayList remainingParagraphs = new ArrayList();
			for (int p = 0; p < crfps.length; p++) {
				
				//	paragraph excluded, remove details
				if (crfps[p].exclude.isSelected()) {
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
				}
				
				//	continue with feedback
				else {
					crfps[p].writeChanges(paragraphs[p]);
					paragraphs[p].setAttribute(COUNTRY_ATTRIBUTE, crfps[p].country.getSelectedItem().toString());
					remainingParagraphs.add(paragraphs[p]);
				}
			}
			paragraphs = ((MutableAnnotation[]) remainingParagraphs.toArray(new MutableAnnotation[remainingParagraphs.size()]));
			remainingParagraphs.clear();
			
			//	remove duplicates
			for (int p = 0; p < paragraphs.length; p++)
				AnnotationFilter.removeDuplicates(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
			
			//	annotate materials citations
			for (int p = 0; p < paragraphs.length; p++)
				this.annotateMaterialsCitations(paragraphs[p]);
			
			//	find frequent punctuation patterns in paragraphs & use for splitting
			for (int p = 0; p < paragraphs.length; p++)
				this.splitMaterialsCitations(paragraphs[p]);
			
			//	build feedback panels
			MaterialsCitationFeedbackPanel[] mcfps = new MaterialsCitationFeedbackPanel[paragraphs.length];
			for (int p = 0; p < paragraphs.length; p++)
				mcfps[p] = this.produceMaterialsCitationFeedbackPanel(paragraphs[p], paragraphTaxons.getProperty(paragraphs[p].getAnnotationID()));
			
			//	display materials citations for correction
			FeedbackPanel.getMultiFeedback(mcfps);
			
			//	process feedback data
			for (int p = 0; p < mcfps.length; p++) {
				
				//	paragraph excluded, remove details
				if (mcfps[p].exclude.isSelected()) {
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
					AnnotationFilter.removeAnnotations(paragraphs[p], MATERIALS_CITATION_ANNOTATION_TYPE);
				}
				
				//	continue with feedback
				else {
					this.writeChanges(mcfps[p], paragraphs[p]);
					remainingParagraphs.add(paragraphs[p]);
					
					//	initialize country attribute
					String country = ((String) paragraphs[p].getAttribute(COUNTRY_ATTRIBUTE));
					String region = null;
					
					//	set country & region attributes
					MutableAnnotation[] materialsCitations = paragraphs[p].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
					for (int m = 0; m < materialsCitations.length; m++) {
						
						//	switch country if any is annotated
						Annotation[] countries = materialsCitations[m].getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
						if (countries.length != 0) {
							country = this.normalizeProperName(countries[0].getValue());
							if (this.countryHandler != null) {
								country = this.countryHandler.getEnglishName(country);
								if (country != null)
									countries[0].setAttribute(VALUE_ATTRIBUTE, country);
							}
							region = null; // new country, clear region
						}
						
						//	set country attribute
						if (country != null)
							materialsCitations[m].setAttribute(COUNTRY_ATTRIBUTE, country);
						
						//	switch region if any is annotated
						Annotation[] regions = materialsCitations[m].getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
						if (regions.length != 0)
							region = this.normalizeProperName(regions[0].getValue());
						
						//	set region attribute
						if (region != null)
							materialsCitations[m].setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
						
						//	add further details from marker annotations
						this.setDetailAttributes(materialsCitations[m]);
					}
				}
			}
			paragraphs = ((MutableAnnotation[]) remainingParagraphs.toArray(new MutableAnnotation[remainingParagraphs.size()]));
			remainingParagraphs.clear();
			
			//	clean up
			for (int p = 0; p < paragraphs.length; p++) {
				AnnotationFilter.removeDuplicates(paragraphs[p], MATERIALS_CITATION_ANNOTATION_TYPE);
				paragraphs[p].removeAttribute(COUNTRY_ATTRIBUTE);
			}
			
			//	we're done here
			return;
		}
		
		//	set up input propagation ...
		String country = null;
		StringVector feedbackRegions = new StringVector();
		StringVector feedbackNonRegions = new StringVector();
		
		//	... and prevent duplicate processing
		HashSet processedSectionIDs = new HashSet();
		
		//	do sequential processing
		for (int ss = 0; ss < this.sectionSelectors.size(); ss++) {
			
			//	get sections
			Annotation[] sectionAnnots = GPath.evaluatePath(data, this.sectionSelectors.get(ss), null);
			
			//	collect section IDs and types
			HashSet sectionIDs = new HashSet(sectionAnnots.length + 1);
			HashSet sectionTypes = new HashSet(2);
			for (int a = 0; a < sectionAnnots.length; a++) {
				sectionIDs.add(sectionAnnots[a].getAnnotationID());
				sectionTypes.add(sectionAnnots[a].getType());
			}
			
			//	get mutable sections
			ArrayList sectionList = new ArrayList();
			for (Iterator stit = sectionTypes.iterator(); stit.hasNext();) {
				MutableAnnotation[] sections = data.getMutableAnnotations((String) stit.next());
				for (int s = 0; s < sections.length; s++) {
					if (sectionIDs.contains(sections[s].getAnnotationID()))
						sectionList.add(sections[s]);
				}
			}
			MutableAnnotation[] sections = ((MutableAnnotation[]) sectionList.toArray(new MutableAnnotation[sectionList.size()]));
			Arrays.sort(sections);
			
			//	process sections
			country = this.processSections(sections, country, paragraphTaxons, processedSectionIDs, feedbackRegions, feedbackNonRegions);
			
			//	catch cancellation
			if (country == CANCELLED)
				return;
		}
	}
	
	private static final String CANCELLED = "CANCELLED";
	private String processSections(MutableAnnotation[] sections, String country, Properties paragraphTaxons, HashSet processedSectionIDs, StringVector feedbackRegions, StringVector feedbackNonRegions) {
		
		//	get candidate paragraphs (overview allows for better user information)
		MutableAnnotation[][] sectionParagraphs = new MutableAnnotation[sections.length][];
		int totalParagraphs = 0;
		for (int s = 0; s < sections.length; s++) {
			
			//	we've already seen this one
			if (processedSectionIDs.contains(sections[s].getAnnotationID()))
				continue;
			
			//	check for existing materials citations
			MutableAnnotation[] materialsCitations = sections[s].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
			
			//	skip if section if done, unless it's the only section (might be re-invocation)
			if ((sections.length != 1) && (materialsCitations.length != 0))
				continue;
			
			//	filter out interesting paragraphs
			MutableAnnotation[] paragraphs = sections[s].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			ArrayList candidateParagraphList = new ArrayList();
			for (int p = 0; p < paragraphs.length; p++) {
				
				//	get evidence (must be annotated before)
				int indicatorAnnotationCount = 0;
				for (int i = 0; (i < this.indicatorAnnotationTypes.size()) && (indicatorAnnotationCount == 0); i++)
					indicatorAnnotationCount += paragraphs[p].getAnnotations(this.indicatorAnnotationTypes.get(i)).length;
				
				//	paragraph that may contain collecting events
				if (indicatorAnnotationCount != 0)
					candidateParagraphList.add(paragraphs[p]);
			}
			
			//	store collected candidates (if any) and update statistics
			if (candidateParagraphList.size() != 0) {
				sectionParagraphs[s] = ((MutableAnnotation[]) candidateParagraphList.toArray(new MutableAnnotation[candidateParagraphList.size()]));
				totalParagraphs += sectionParagraphs[s].length;
			}
		}
		
		//	work section by section
		int handledParagraphs = 0;
		for (int s = 0; s < sections.length; s++) {
			
			//	no interesting paragraphs found
			if (sectionParagraphs[s] == null)
				continue;
			
			//	get collected candidates
			MutableAnnotation[] paragraphs = sectionParagraphs[s];
			
			//	filter out paragraphs before last one that already has materials citations annotated (happens on resume)
			ArrayList remainingParagraphs = new ArrayList();
			for (int p = paragraphs.length; p > 0; p--) {
				
				//	check for existing materials citations
				Annotation[] mcs = paragraphs[p-1].getAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
				
				//	paragraph not processed so far
				if (mcs.length == 0)
					remainingParagraphs.add(paragraphs[p-1]);
				
				//	up to this one, all paragraphs have been handled
				else {
					handledParagraphs += p;
					break;
				}
			}
			
			//	restore order
			Collections.sort(remainingParagraphs);
			
			//	proceed with un-processed paragraphs
			paragraphs = ((MutableAnnotation[]) remainingParagraphs.toArray(new MutableAnnotation[remainingParagraphs.size()]));
			
			//	process paragraphs one by one
			for (int p = 0; p < paragraphs.length; p++) {
				handledParagraphs++;
				
				//	annotate collecting countries
				this.annotateCollectingCountries(paragraphs[p]);
				
				//	remove duplicates
				AnnotationFilter.removeDuplicates(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
				
				//	annotate regions
				this.annotateRegions(paragraphs[p], feedbackRegions, feedbackNonRegions);
				
				//	build feedback panel
				CountryRegionFeedbackPanel crfp = this.produceCountryRegionFeedbackPanel(paragraphs[p], paragraphTaxons.getProperty(paragraphs[p].getAnnotationID()));
				crfp.addButton("Cancel");
				crfp.addButton("Exclude");
				crfp.addButton("OK");
				
				//	adjust country
				String pCountry = country;
				Annotation[] countries = paragraphs[p].getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
				if (countries.length == 0) {
					if (this.countryHandler != null)
						pCountry = this.countryHandler.getEnglishName(pCountry);
					if (pCountry != null)
						crfp.setCountry(pCountry);
				}
				else {
					pCountry = this.normalizeProperName(countries[0].getValue());
					if (this.countryHandler != null)
						pCountry = this.countryHandler.getEnglishName(pCountry);
					crfp.setCountry(this.normalizeProperName(pCountry));
				}
				
				String title = crfp.getTitle();
				crfp.setTitle(title + " - (" + handledParagraphs + " of " + totalParagraphs + ")");
				
				String crf = crfp.getFeedback();
				if (crf == null)
					crf = "Cancel";
				
				crfp.setTitle(title);
				
				//	cancelled
				if ("Cancel".equals(crf)) {
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
					return CANCELLED;
				}
				
				//	excluded
				else if (crfp.exclude.isSelected() || "Exclude".equals(crf)) {
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
					AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
				}
				
				//	committed, proceed
				else {
					
					//	annotate countries and regions
					crfp.writeChanges(paragraphs[p]);
					
					//	get country attribute
					country = crfp.country.getSelectedItem().toString();
					
					//	annotate materials citations
					this.annotateMaterialsCitations(paragraphs[p]);
					
					//	find frequent punctuation patterns in paragraphs & use for splitting
					this.splitMaterialsCitations(paragraphs[p]);
					
					//	show paragraph for correcting materials citations
					MaterialsCitationFeedbackPanel mcfp = this.produceMaterialsCitationFeedbackPanel(paragraphs[p], paragraphTaxons.getProperty(paragraphs[p].getAnnotationID()));
					mcfp.addButton("Cancel");
					crfp.addButton("Exclude");
					mcfp.addButton("OK");
					
					title = mcfp.getTitle();
					crfp.setTitle(title + " - (" + handledParagraphs + " of " + totalParagraphs + ")");
					
					String mcf = mcfp.getFeedback();
					
					mcfp.setTitle(title);
					
					//	cancelled
					if ("Cancel".equals(mcf)) {
						AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
						AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
						AnnotationFilter.removeAnnotations(paragraphs[p], MATERIALS_CITATION_ANNOTATION_TYPE);
						return CANCELLED;
					}
					
					//	excluded
					else if (mcfp.exclude.isSelected() || "Exclude".equals(mcf)) {
						AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_COUNTRY_ANNOTATION_TYPE);
						AnnotationFilter.removeAnnotations(paragraphs[p], COLLECTING_REGION_ANNOTATION_TYPE);
						AnnotationFilter.removeAnnotations(paragraphs[p], MATERIALS_CITATION_ANNOTATION_TYPE);
					}
					
					//	committed, annotate materials citations
					else {
						
						//	observe which regions are removed
						final ArrayList excludedRegions = new ArrayList();
						AnnotationListener al = new AnnotationListener() {
							public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {}
							public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
								if (COLLECTING_REGION_ANNOTATION_TYPE.equals(annotation.getType()))
									excludedRegions.add(annotation);
							}
							public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {}
							public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {}
						};
						paragraphs[p].addAnnotationListener(al);
						this.writeChanges(mcfp, paragraphs[p]);
						paragraphs[p].removeAnnotationListener(al);
						String region = null;
						
						//	set country & region attributes
						MutableAnnotation[] materialsCitations = paragraphs[p].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
						for (int m = 0; m < materialsCitations.length; m++) {
							
							//	switch country if any is annotated
							countries = materialsCitations[m].getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
							if (countries.length != 0) {
								country = this.normalizeProperName(countries[0].getValue());
								if (this.countryHandler != null) {
									country = this.countryHandler.getEnglishName(country);
									if (country != null)
										countries[0].setAttribute(VALUE_ATTRIBUTE, country);
								}
								region = null; // new country, clear region
							}
							
							//	set country attribute
							if (country != null)
								materialsCitations[m].setAttribute(COUNTRY_ATTRIBUTE, country);
							
							//	switch region if any is annotated
							Annotation[] regions = materialsCitations[m].getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
							if (regions.length != 0)
								region = this.normalizeProperName(regions[0].getValue());
							
							//	remember regions for paragraphs to come
							for (int r = 0; r < regions.length; r++)
								feedbackRegions.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(regions[r], true, true));
							
							//	set region attribute
							if (region != null)
								materialsCitations[m].setAttribute(STATE_PROVINCE_ATTRIBUTE, region);
							
							//	add further details from marker annotations
							this.setDetailAttributes(materialsCitations[m]);
						}
						
						//	remember excluded regions for paragraphs to come
						if (excludedRegions.size() != 0) {
							Annotation[] regions = paragraphs[p].getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
							for (int e = 0; e < excludedRegions.size(); e++) {
								Annotation excludedRegion = ((Annotation) excludedRegions.get(e));
								for (int r = 0; r < regions.length; r++)
									if (AnnotationUtils.overlaps(excludedRegion, regions[r])) {
										excludedRegion = null;
										break;
									}
								if (excludedRegion != null)
									feedbackNonRegions.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(excludedRegion, true, true));
							}
						}
					}
					
					//	we have to stop here
					if ("OK & Stop After".equals(mcf))
						return CANCELLED;
				}
				
				//	remember section is done with
				processedSectionIDs.add(sections[s].getAnnotationID());
			}
		}
		
		//	pass on country
		return country;
	}
	
	private void setDetailAttributes(QueriableAnnotation mc) {
		for (Iterator dapit = this.detailAttributePaths.keySet().iterator(); dapit.hasNext();) try {
			GPath dap = ((GPath) dapit.next());
			String dan = ((String) this.detailAttributePaths.get(dap));
			if (mc.hasAttribute(dan))
				continue;
			Annotation[] davs = dap.evaluate(mc, null);
			if (davs.length != 0)
				mc.setAttribute(dan, davs[0].getValue());
		} catch (RuntimeException re) {}
	}
	
	private Annotation[] getFrequentJoins(Annotation data, Annotation[] base, StringVector phrases) {
		
		//	build joins
		ArrayList joins = new ArrayList();
		for (int j = 0; j < (base.length - 1); j++) {
			if (base[j].getEndIndex() >= base[j+1].getStartIndex()) {
				Annotation join = Gamta.newAnnotation(data, Annotation.DEFAULT_ANNOTATION_TYPE, base[j].getStartIndex(), (base[j+1].getEndIndex() - base[j].getStartIndex()));
				joins.add(join);
				phrases.addElement(join.getValue());
			}
		}
		
		//	assess joins' frequency
		for (int j = 0; j < joins.size();) {
			Annotation join = ((Annotation) joins.get(j));
			if (phrases.getElementCount(join.getValue()) < 3) {
				phrases.removeAll(join.getValue());
				joins.remove(j);
			}
			else j++;
		}
		
		//	return what's left after frequency elimination
		return ((Annotation[]) joins.toArray(new Annotation[joins.size()]));
	}
	
	private String normalizeProperName(String string) {
		if (string == null)
			return null;
		
		StringBuffer nString = new StringBuffer(string.length());
		boolean lastWasWhitespace = true;
		for (int c = 0; c < string.length(); c++) {
			if (string.charAt(c) < 33) {
				lastWasWhitespace = true;
				nString.append(" ");
			}
			else {
				if (lastWasWhitespace)
					nString.append(string.substring(c, (c+1)).toUpperCase());
				else nString.append(string.substring(c, (c+1)).toLowerCase());
				lastWasWhitespace = false;
			}
		}
		return nString.toString();
	}
	
	private void annotateCollectingCountries(MutableAnnotation paragraph) {
		
		//	get collection countries
		Annotation[] countries = Gamta.extractAllContained(paragraph, this.countryHandler, 10, false, false, false); // TODO give GAMTA update (default-off of normalization) time to spread then switch to 3-argument signature
		
		//	annotate collecting countries
		for (int c = 0; c < countries.length; c++) {
			String countryString = TokenSequenceUtils.concatTokens(countries[c], true, true);
			if (this.typeStatuses.containsKey(countryString))
				continue;
			if (this.specimenTypes.containsKey(countryString))
				continue;
			Annotation country = paragraph.addAnnotation(COLLECTING_COUNTRY_ANNOTATION_TYPE, countries[c].getStartIndex(), countries[c].size());
			String countryName = this.countryHandler.getEnglishName(countryString);
			country.setAttribute(NAME_ATTRIBUTE, countryName);
		}
		
		//	remove duplicates
		AnnotationFilter.removeDuplicates(paragraph, COLLECTING_COUNTRY_ANNOTATION_TYPE);
	}
	
	private void annotateRegions(MutableAnnotation paragraph, StringVector feedbackRegions, StringVector feedbackNonRegions) {
		
		//	get collection countries
		Annotation[] countries = paragraph.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		
		//	create region groups from countries
		ArrayList regionGroupList = new ArrayList();
		
		//	no countries at all
		if (countries.length == 0)
			regionGroupList.add(Gamta.newAnnotation(paragraph, COLLECTING_REGION_GROUP_ANNOTATION_TYPE, 0, paragraph.size()));
		
		//	only one country
		else if (countries.length == 1) {
			Annotation regionGroup = Gamta.newAnnotation(paragraph, COLLECTING_REGION_GROUP_ANNOTATION_TYPE, countries[0].getStartIndex(), (paragraph.size() - countries[0].getStartIndex()));
			regionGroup.setAttribute(COUNTRY_ATTRIBUTE, countries[0].getAttribute(NAME_ATTRIBUTE));
			regionGroupList.add(regionGroup);
		}
		
		//	two or more countries
		else {
			
			//	mark groups
			for (int c = 0; c < (countries.length - 1); c++) {
				Annotation regionGroup = Gamta.newAnnotation(paragraph, COLLECTING_REGION_GROUP_ANNOTATION_TYPE, countries[c].getStartIndex(), (countries[c + 1].getStartIndex() - countries[c].getStartIndex()));
				regionGroup.setAttribute(COUNTRY_ATTRIBUTE, countries[0].getAttribute(NAME_ATTRIBUTE));
				regionGroupList.add(regionGroup);
			}
			
			//	mark last group
			Annotation regionGroup = Gamta.newAnnotation(paragraph, COLLECTING_REGION_GROUP_ANNOTATION_TYPE, countries[countries.length - 1].getStartIndex(), (paragraph.size() - countries[countries.length -1].getStartIndex()));
			regionGroup.setAttribute(COUNTRY_ATTRIBUTE, countries[0].getAttribute(NAME_ATTRIBUTE));
			regionGroupList.add(regionGroup);
		}
		
		//	prepare region lexicon
		StringVector effectiveFeedbackNonRegions = feedbackNonRegions.without(feedbackRegions);
		
		//	do regex matches in region groups
		Annotation[] regionGroups = ((Annotation[]) regionGroupList.toArray(new Annotation[regionGroupList.size()]));
		for (int rg = 0; rg < regionGroups.length; rg++) {
			String groupCountry = ((String) regionGroups[rg].getAttribute(COUNTRY_ATTRIBUTE));
			if (groupCountry != null) {
				RegionHandler rh = this.countryHandler.getRegionHandler(groupCountry);
				if (rh != null) {
					Annotation[] regions = Gamta.extractAllContained(regionGroups[rg], rh, 10, false, false, false);
					for (int r = 0; r < regions.length; r++)
						paragraph.addAnnotation(COLLECTING_REGION_ANNOTATION_TYPE, (regionGroups[rg].getStartIndex() + regions[r].getStartIndex()), regions[r].size());
				}
			}
			
			//	get collecting regions from morphological clues
			for (int re = 0; re < this.collectingRegionRegExes.size(); re++) {
				Annotation[] regions = Gamta.extractAllMatches(regionGroups[rg], this.collectingRegionRegExes.get(re), 10, caseSensitiveNoise, null, false, false);
				for (int r = 0; r < regions.length; r++) {
					boolean annotate = true;
					for (int v = 0; annotate && (v < regions[r].size()); v++) {
						String regionPart = regions[r].valueAt(v);
						annotate = (annotate && 
								!this.typeStatusDict.lookup(regionPart, false) && 
								!this.collectingRegionExcludeTokens.containsIgnoreCase(regionPart)
							);
					}
					if (annotate)
						paragraph.addAnnotation(COLLECTING_REGION_ANNOTATION_TYPE, (regionGroups[rg].getStartIndex() + regions[r].getStartIndex()), regions[r].size());
				}
			}
			
			//	get collecting regions previously approved by users
			Annotation[] regions = Gamta.extractAllContained(regionGroups[rg], feedbackRegions, false, false, false);
			for (int r = 0; r < regions.length; r++)
				paragraph.addAnnotation(COLLECTING_REGION_ANNOTATION_TYPE, (regionGroups[rg].getStartIndex() + regions[r].getStartIndex()), regions[r].size());
			
			//	get non collecting regions
			for (int re = 0; re < this.collectingRegionExcludeRegExes.size(); re++) {
				Annotation[] regionsExclude = Gamta.extractAllMatches(regionGroups[rg], this.collectingRegionExcludeRegExes.get(re), 10, caseSensitiveNoise, null, false, false);
				for (int r = 0; r < regionsExclude.length; r++)
					paragraph.addAnnotation((COLLECTING_REGION_ANNOTATION_TYPE + "Exclude"), (regionGroups[rg].getStartIndex() + regionsExclude[r].getStartIndex()), regionsExclude[r].size());
			}
			
			//	get collecting regions previously excluded by users
			Annotation[] regionsExclude = Gamta.extractAllContained(regionGroups[rg], effectiveFeedbackNonRegions, false, false);
			for (int r = 0; r < regionsExclude.length; r++)
				paragraph.addAnnotation((COLLECTING_REGION_ANNOTATION_TYPE + "Exclude"), (regionGroups[rg].getStartIndex() + regionsExclude[r].getStartIndex()), regionsExclude[r].size());
		}
		
		//	clean up
		AnnotationFilter.removeDuplicates(paragraph, COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	truncate punctuation from region annotations (might come from reg ex use)
		this.truncateSeparatorPunctuation(paragraph, COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	check for duplicates again
		AnnotationFilter.removeDuplicates(paragraph, COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	remove regions annotations that mark a country
		AnnotationFilter.removeByContained(paragraph, COLLECTING_COUNTRY_ANNOTATION_TYPE, COLLECTING_REGION_ANNOTATION_TYPE, false);
		AnnotationFilter.removeByContaining(paragraph, COLLECTING_REGION_ANNOTATION_TYPE, COLLECTING_COUNTRY_ANNOTATION_TYPE, false);
		
		//	remove regions containing a type status indicator
		Annotation[] regions = paragraph.getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
		for (int r = 0; r < regions.length; r++)
			for (int v = 0; v < regions[r].size(); v++) {
				if (this.typeStatusDict.lookup(regions[r].valueAt(v), false))
					v = paragraph.removeAnnotation(regions[r]).size();
			}
		
		//	find frequent proper names (that are not countries or part of them) in paragraphs, might be region names
		StringVector phraseList = TokenSequenceUtils.getTextTokens(paragraph);
		
		//	eliminate infrequent and problematic ones
		for (int f = 0; f < phraseList.size(); f++) {
			String phrase = phraseList.get(f);
			if (phraseList.getElementCount(phrase) < 3)
				phraseList.removeAll(phrase);
			else if (this.countryHandler.lookup(phrase))
				phraseList.removeAll(phrase);
			else if (this.typeStatusDict.lookup(phrase, false))
				phraseList.removeAll(phrase);
			else if (this.collectingRegionExcludeTokens.containsIgnoreCase(phrase))
				phraseList.removeAll(phrase);
			else if (Gamta.isUpperCaseWord(phrase))
				phraseList.removeAll(phrase);
		}
		
		//	annotate frequent words
		Annotation[] phrases = Gamta.extractAllContained(paragraph, phraseList);
		
		//	derive frequent phrases
		Stack phraseSets = new Stack();
		while (phrases.length != 0) {
			phraseSets.push(phrases);
			phrases = this.getFrequentJoins(paragraph, phrases, phraseList);
		}
		
		//	annotate frequent phrases
		while (phraseSets.size() != 0) {
			phrases = ((Annotation[]) phraseSets.pop());
			for (int a = 0; a < phrases.length; a++) {
				
				//	check if possible proper name (capitalized start and at least 4 letters)
				if (!Gamta.isCapitalizedWord(phrases[a].firstValue()) || (phrases[a].length() < 4))
					continue;
				
				//	check if phrase still frequent enough on its own (might have been a sub-phrase of some longer phrase)
				if (phraseList.getElementCount(phrases[a].getValue()) < 3)
					continue;
				
				//	add annotation
				paragraph.addAnnotation(COLLECTING_REGION_ANNOTATION_TYPE, phrases[a].getStartIndex(), phrases[a].size());
				
				//	reduce frequency of sub-phrases
				for (int start = 0; start < phrases[a].size(); start++)
					for (int size = 1; (size < phrases[a].size()) && ((start + size) <= phrases[a].size()); size++) {
						String subPhrase = Gamta.newAnnotation(paragraph, Annotation.DEFAULT_ANNOTATION_TYPE, (phrases[a].getStartIndex() + start), size).getValue();
						phraseList.remove(subPhrase);
					}
			}
		}
		
		//	remove duplicates
		AnnotationFilter.removeDuplicates(paragraph, COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	remove nested region annotations
		AnnotationFilter.removeContained(paragraph, COLLECTING_REGION_ANNOTATION_TYPE, COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	remove region annotations nested in negative pattern matches
		AnnotationFilter.removeContained(paragraph, (COLLECTING_REGION_ANNOTATION_TYPE + "Exclude"), COLLECTING_REGION_ANNOTATION_TYPE);
		
		//	remove negative pattern matches
		AnnotationFilter.removeAnnotations(paragraph, (COLLECTING_REGION_ANNOTATION_TYPE + "Exclude"));
	}
	
	private void annotateMaterialsCitations(MutableAnnotation paragraph) {
		
		//	create region groups from countries
		Annotation[] countries = paragraph.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		
		//	no countries at all
		if (countries.length == 0) {
			paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, 0, paragraph.size());
//			Annotation rg = paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, 0, paragraph.size());
//			rg.setAttribute(COUNTRY_ATTRIBUTE, paragraph.getAttribute(COUNTRY_ATTRIBUTE));
		}
		
		//	only one country
		else if (countries.length == 1) {
			paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, 0, paragraph.size());
//			Annotation rg = paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, countries[0].getStartIndex(), (paragraph.size() - countries[0].getStartIndex()));
//			rg.setAttribute(COUNTRY_ATTRIBUTE, this.normalizeProperName(countries[0].getValue()));
		}
		
		//	two or more countries
		else {
			int crGroupStart = 0;
			
			//	mark groups
			for (int c = 0; c < (countries.length - 1); c++) {
				paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, crGroupStart, (countries[c + 1].getStartIndex() - crGroupStart));
				crGroupStart = countries[c + 1].getStartIndex();
//				Annotation rg = paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, countries[c].getStartIndex(), (countries[c + 1].getStartIndex() - countries[c].getStartIndex()));
//				rg.setAttribute(COUNTRY_ATTRIBUTE, this.normalizeProperName(countries[c].getValue()));
			}
			
			//	mark last group
			paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, crGroupStart, (paragraph.size() - crGroupStart));
//			Annotation rg = paragraph.addAnnotation(COLLECTING_REGION_GROUP_ANNOTATION_TYPE, countries[countries.length - 1].getStartIndex(), (paragraph.size() - countries[countries.length -1].getStartIndex()));
//			rg.setAttribute(COUNTRY_ATTRIBUTE, this.normalizeProperName(countries[countries.length - 1].getValue()));
		}
		
		
		//	create materials citation groups from region groups and regions
		MutableAnnotation[] regionGroups = paragraph.getMutableAnnotations(COLLECTING_REGION_GROUP_ANNOTATION_TYPE);
		for (int rg = 0; rg < regionGroups.length; rg++) {
//			System.out.println("Doing Region-Group " + regionGroups[rg].toXML());
			
			//	get collecting regions
			Annotation[] regions = regionGroups[rg].getMutableAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
			
			//	no regions at all
			if (regions.length == 0) {
				regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, 0, regionGroups[rg].size());
//				Annotation mcg = regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, 0, regionGroups[rg].size());
//				mcg.setAttribute(COUNTRY_ATTRIBUTE, regionGroups[rg].getAttribute(COUNTRY_ATTRIBUTE));
			}
			
			//	only one region
			else if (regions.length == 1) {
				regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, 0, regionGroups[rg].size());
//				Annotation mcg = regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, 0, regionGroups[rg].size());
//				mcg.setAttribute(COUNTRY_ATTRIBUTE, regionGroups[rg].getAttribute(COUNTRY_ATTRIBUTE));
//				mcg.setAttribute(STATE_PROVINCE_ATTRIBUTE, this.normalizeProperName(regions[0].getValue()));
			}
			
			//	two or more regions
			else {
				int mcGroupStart = 0;
				
				//	mark groups
				for (int r = 0; r < (regions.length - 1); r++) {
					regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, mcGroupStart, (regions[r + 1].getStartIndex() - mcGroupStart));
					mcGroupStart = regions[r+1].getStartIndex();
//					mcGroupStart = ((r == 0) ? 0 : regions[r].getStartIndex());
//					Annotation mcg = regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, mcGroupStart, (regions[r + 1].getStartIndex() - mcGroupStart));
//					mcg.setAttribute(COUNTRY_ATTRIBUTE, regionGroups[rg].getAttribute(COUNTRY_ATTRIBUTE));
//					mcg.setAttribute(STATE_PROVINCE_ATTRIBUTE, this.normalizeProperName(regions[r].getValue()));
				}
				
				//	mark last group
				regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, mcGroupStart, (regionGroups[rg].size() - mcGroupStart));
//				Annotation mcg = regionGroups[rg].addAnnotation(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE, regions[regions.length - 1].getStartIndex(), (regionGroups[rg].size() - regions[regions.length -1].getStartIndex()));
//				mcg.setAttribute(COUNTRY_ATTRIBUTE, regionGroups[rg].getAttribute(COUNTRY_ATTRIBUTE));
//				mcg.setAttribute(STATE_PROVINCE_ATTRIBUTE, this.normalizeProperName(regions[regions.length - 1].getValue()));
			}
			
			//	clean up region group
			paragraph.removeAnnotation(regionGroups[rg]);
		}
		
		
		//	split materials citation groups into individual materials citations
		MutableAnnotation[] mcGroups = paragraph.getMutableAnnotations(MATERIALS_CITATION_GROUP_ANNOTATION_TYPE);
		for (int mcg = 0; mcg < mcGroups.length; mcg++) {
//			System.out.println("Doing MC-Group " + mcGroups[mcg].toXML());
			
			//	get separators
			Annotation[] separators = Gamta.extractAllContained(mcGroups[mcg], this.materialsCitationSeparators);
			
			//	jump country and region that might be contained in materials citation
			Annotation[] mcCountry = mcGroups[mcg].getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
			int cEnd = ((mcCountry.length == 0) ? 0 : mcCountry[0].getEndIndex());
			Annotation[] mcRegion = mcGroups[mcg].getAnnotations(COLLECTING_REGION_ANNOTATION_TYPE);
			int rEnd = ((mcRegion.length == 0) ? 0 : mcRegion[0].getEndIndex());
			
			//	find first separator after country and region
			int separatorsStart = 0;
			while ((separatorsStart < separators.length) && separators[separatorsStart].getStartIndex() < Math.max(cEnd, rEnd))
				separatorsStart++;
			
			//	separator found
			if ((separatorsStart < separators.length)) {
				
				//	initialize
				int mcStart = 0;
				
				//	create materials citations
				for (int s = separatorsStart; s < separators.length; s++) {
					Annotation mc = mcGroups[mcg].addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, mcStart, (separators[s].getStartIndex() - mcStart));
					mc.setAttribute(COUNTRY_ATTRIBUTE, mcGroups[mcg].getAttribute(COUNTRY_ATTRIBUTE));
					if (mcGroups[mcg].hasAttribute(STATE_PROVINCE_ATTRIBUTE))
						mc.setAttribute(STATE_PROVINCE_ATTRIBUTE, mcGroups[mcg].getAttribute(STATE_PROVINCE_ATTRIBUTE));
					
					//	switch to next materials citation
					mcStart = separators[s].getEndIndex();
				}
				
				//	one more materials citation left to mark
				if (mcStart < mcGroups[mcg].size()) {
					Annotation mc = mcGroups[mcg].addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, mcStart, (mcGroups[mcg].size() - mcStart));
					mc.setAttribute(COUNTRY_ATTRIBUTE, mcGroups[mcg].getAttribute(COUNTRY_ATTRIBUTE));
					if (mcGroups[mcg].hasAttribute(STATE_PROVINCE_ATTRIBUTE))
						mc.setAttribute(STATE_PROVINCE_ATTRIBUTE, mcGroups[mcg].getAttribute(STATE_PROVINCE_ATTRIBUTE));
				}
			}
			
			//	no separator found, only one materials citation
			else {
				Annotation mc = mcGroups[mcg].addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, 0, mcGroups[mcg].size());
				mc.setAttribute(COUNTRY_ATTRIBUTE, mcGroups[mcg].getAttribute(COUNTRY_ATTRIBUTE));
				if (mcGroups[mcg].hasAttribute(STATE_PROVINCE_ATTRIBUTE))
					mc.setAttribute(STATE_PROVINCE_ATTRIBUTE, mcGroups[mcg].getAttribute(STATE_PROVINCE_ATTRIBUTE));
			}
			
			//	clean up materials citation group
			paragraph.removeAnnotation(mcGroups[mcg]);
		}
	}
	
	private void splitMaterialsCitations(MutableAnnotation paragraph) {
		
		//	punctuation marks
		StringVector punctuationMarks = new StringVector();
		for (int v = 0; v < paragraph.size(); v++)
			if (Gamta.isPunctuation(paragraph.valueAt(v)))
				punctuationMarks.addElement(paragraph.valueAt(v));
		
		//	eliminate unfrequent ones
		for (int m = 0; m < punctuationMarks.size(); m++) {
			if (punctuationMarks.getElementCount(punctuationMarks.get(m)) < 3)
				punctuationMarks.removeAll(punctuationMarks.get(m));
		}
		
		//	annotate frequent words
		Annotation[] marks = Gamta.extractAllContained(paragraph, punctuationMarks);
		Annotation[] markPairs = this.getFrequentJoins(paragraph, marks, punctuationMarks);
		
		//	remove pairs with both or no marks from ',;.:', sort others
		ArrayList splitPairs = new ArrayList();
		for (int m = 0; m < markPairs.length; m++)
			if ((SEPARATORS.indexOf(markPairs[m].firstValue()) == -1) != (SEPARATORS.indexOf(markPairs[m].lastValue()) == -1))
				splitPairs.add(markPairs[m]);
		
		//	split materials citations at pairs
		MutableAnnotation[] materialsCitations = paragraph.getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
		int splitPairIndex = 0;
		for (int c = 0; c < materialsCitations.length; c++) {
			Annotation materialsCitation = materialsCitations[c];
			
			//	find next split inside current materials citation
			while ((splitPairIndex < splitPairs.size()) && (((Annotation) splitPairs.get(splitPairIndex)).getStartIndex() <= materialsCitation.getStartIndex()))
				splitPairIndex++;
			
			//	split current materials citation
			while ((splitPairIndex < splitPairs.size()) && (((Annotation) splitPairs.get(splitPairIndex)).getEndIndex() < materialsCitation.getEndIndex())) {
				Annotation splitPair = ((Annotation) splitPairs.get(splitPairIndex));
				
				//	split after if ',;.:' in second position, but not if first if opening bracket, and only if split pair ends inside materials citation
				if ((SEPARATORS.indexOf(splitPair.firstValue()) == -1) && !Gamta.isOpeningBracket(splitPair.firstValue()) && (splitPair.getEndIndex() < materialsCitation.getEndIndex())) {
					Annotation mc1 = paragraph.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, materialsCitation.getStartIndex(), (splitPair.getEndIndex() - materialsCitation.getStartIndex()));
					mc1.copyAttributes(materialsCitation);
					Annotation mc2 = paragraph.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, splitPair.getEndIndex(), (materialsCitation.getEndIndex() - splitPair.getEndIndex()));
					mc2.copyAttributes(materialsCitation);
					
					paragraph.removeAnnotation(materialsCitation);
					materialsCitation = mc2;
				}
				
				//	split before if ',;.:' in first position, but not if second if closing bracket, and only if split pair starts inside materials citation
				else if ((SEPARATORS.indexOf(splitPair.lastValue()) == -1) && !Gamta.isClosingBracket(splitPair.lastValue()) && (splitPair.getStartIndex() > materialsCitation.getStartIndex())) {
					Annotation mc1 = paragraph.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, materialsCitation.getStartIndex(), (splitPair.getStartIndex() - materialsCitation.getStartIndex()));
					mc1.copyAttributes(materialsCitation);
					Annotation mc2 = paragraph.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, splitPair.getStartIndex(), (materialsCitation.getEndIndex() - splitPair.getStartIndex()));
					mc2.copyAttributes(materialsCitation);
					
					paragraph.removeAnnotation(materialsCitation);
					materialsCitation = mc2;
				}
				
				//	switch to next splitting point
				splitPairIndex++;
			}
		}
		
		//	truncate punctuation from split materials citations
		this.truncateSeparatorPunctuation(paragraph, MATERIALS_CITATION_ANNOTATION_TYPE);
	}
	
	private void truncateSeparatorPunctuation(MutableAnnotation data, String annotationType) {
		Annotation[] annotations = data.getAnnotations(annotationType);
		for (int r = 0; r < annotations.length; r++) {
			int cutStart = 0;
			while ((cutStart < annotations[r].size()) && (SEPARATORS.indexOf(annotations[r].valueAt(cutStart)) != -1))
				cutStart++;
			
			int cutEnd = 0;
			while ((cutEnd < annotations[r].size()) && (SEPARATORS.indexOf(annotations[r].valueAt(annotations[r].size() - cutEnd - 1)) != -1))
				cutEnd++;
			
			if ((cutStart + cutEnd) != 0) {
				if ((cutStart + cutEnd) < annotations[r].size())
					data.addAnnotation(annotationType, (annotations[r].getStartIndex() + cutStart), (annotations[r].size() - (cutStart + cutEnd))).copyAttributes(annotations[r]);
				data.removeAnnotation(annotations[r]);
			}
		}
	}
	
	private CountryRegionFeedbackPanel produceCountryRegionFeedbackPanel(MutableAnnotation paragraph, String taxon) {
		CountryRegionFeedbackPanel crfp = new CountryRegionFeedbackPanel();
		crfp.setTitle("Check Collecting Countries and Regions" + ((taxon == null) ? "" : (" for " + taxon)));
		crfp.setLabel("<HTML>Please annotate all names of <B>countries</B> and <B>regions</B> (and <B>states</B> and <B>provinces</B>) occurring in the materials citations in this paragraph." +
				"<BR>If there is no country given explicitly, please select one from the drop-down box above the text." +
				"<BR>If this paragraph does no contain any materials citations, please check the 'Exclude Paragraph' checkbox to indicate so.</HTML>");
		crfp.addDetailType(COLLECTING_COUNTRY_ANNOTATION_TYPE, this.getAnnotationHighlight(COLLECTING_COUNTRY_ANNOTATION_TYPE));
		crfp.addDetailType(COLLECTING_REGION_ANNOTATION_TYPE, this.getAnnotationHighlight(COLLECTING_REGION_ANNOTATION_TYPE));
		crfp.setCountries(this.countries.toStringArray());
		crfp.setAnnotation(paragraph);
		
		Annotation[] countries = paragraph.getAnnotations(COLLECTING_COUNTRY_ANNOTATION_TYPE);
		String country = ((countries.length == 0) ? paragraph.getDocumentProperty(COLLECTING_COUNTRY_ANNOTATION_TYPE) : this.normalizeProperName(countries[0].getValue()));
		if (country != null) {
			if (this.countryHandler != null)
				country = this.countryHandler.getEnglishName(country);
			crfp.setCountry(country);
		}
		
		return crfp;
	}
	
	private MaterialsCitationFeedbackPanel produceMaterialsCitationFeedbackPanel(MutableAnnotation paragraph, String taxon) {
		MaterialsCitationFeedbackPanel mcfp = new MaterialsCitationFeedbackPanel();
		mcfp.setTitle("Please Check Materials Citations" + ((taxon == null) ? "" : (" for " + taxon)));
		mcfp.setLabel("<HTML>Please make sure that all <B>materials citations</B> in this paragraph are annotated individually." +
				"<BR>Each materials citation starts in a new line to improve overlook." +
				"<BR>If this paragraph does no contain any materials citations, please check the 'Exclude Paragraph' checkbox to indicate so.</HTML>");
		mcfp.setMcColor(this.getAnnotationHighlight(MATERIALS_CITATION_ANNOTATION_TYPE));
		mcfp.setAnnotation(paragraph);
		return mcfp;
	}
	
	private void writeChanges(MaterialsCitationFeedbackPanel mcfp, MutableAnnotation target) {
		Annotation[] annotations = target.getAnnotations();
		
		//	index existing detail annotations
		HashMap existingAnnotations = new HashMap();
		for (int a = 0; a < annotations.length; a++) {
			if (MATERIALS_CITATION_ANNOTATION_TYPE.equals(annotations[a].getType())) {
				String annotationKey = annotations[a].getType() + "-" + annotations[a].getStartIndex() + "-" + annotations[a].getEndIndex();
				existingAnnotations.put(annotationKey, annotations[a]);
			}
		}
		int aStart = -1;
		String aType = null;
		for (int t = 0; t < target.size(); t++) {
			
			//	start of a new detail annotation
			if (AnnotationEditorFeedbackPanel.START.equals(mcfp.annotationEditor.tokens[t].state)) {
				
				//	other detail annotation open, store it
				if (aStart != -1) {
					String annotationKey = aType + "-" + aStart + "-" + t;
					
					//	detail annotation already exists, keep it
					if (existingAnnotations.containsKey(annotationKey))
						existingAnnotations.remove(annotationKey);
					
					//	create it otherwise
					else target.addAnnotation(aType, aStart, (t - aStart));
				}
				
				//	mark start of new detail annotation
				aStart = t;
				aType = MATERIALS_CITATION_ANNOTATION_TYPE;
			}
			
			//	not a detail annotation
			else if (AnnotationEditorFeedbackPanel.OTHER.equals(mcfp.annotationEditor.tokens[t].state)) {
				
				//	detail annotation open, store it
				if (aStart != -1) {
					String annotationKey = aType + "-" + aStart + "-" + t;
					
					//	detail annotation already exists, keep it
					if (existingAnnotations.containsKey(annotationKey))
						existingAnnotations.remove(annotationKey);
					
					//	create it otherwise
					else target.addAnnotation(aType, aStart, (t - aStart));
				}
				
				//	empty registers
				aStart = -1;
				aType = null;
			}
		}
		
		//	detail annotation remains open, store it
		if (aStart != -1) {
			String annotationKey = aType + "-" + aStart + "-" + target.size();
			
			//	detail annotation already exists, keep it
			if (existingAnnotations.containsKey(annotationKey))
				existingAnnotations.remove(annotationKey);
			
			//	create it otherwise
			else target.addAnnotation(aType, aStart, (target.size() - aStart));
		}
		
		//	remove remaining old detail annotations
		for (Iterator ait = existingAnnotations.values().iterator(); ait.hasNext();)
			target.removeAnnotation((Annotation) ait.next());
	}
	
	private HashMap highlightAttributeCache = new HashMap();
	
	private Color getAnnotationHighlight(String type) {
		Color color = ((Color) highlightAttributeCache.get(type));
		if (color == null) {
			if (COLLECTING_COUNTRY_ANNOTATION_TYPE.equals(type))
				color = Color.ORANGE;
			else if (COLLECTING_REGION_ANNOTATION_TYPE.equals(type))
				color = Color.GREEN.brighter();
			else if (COLLECTING_COUNTY_ANNOTATION_TYPE.equals(type))
				color = Color.PINK;
			else if (MATERIALS_CITATION_ANNOTATION_TYPE.equals(type))
				color = Color.YELLOW;
			else color = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			highlightAttributeCache.put(type, color);
		}
		return color;
	}
	
	/**
	 * Feedback panel for correcting country and region annotations. Has to be
	 * public and static due to the class visibility and loadability
	 * requirements of the remote feedback API.
	 * 
	 * @author sautter
	 */
	public static class CountryRegionFeedbackPanel extends FeedbackPanel {
		private static AnnotationEditorFeedbackPanelRenderer aefpRenderer = new AnnotationEditorFeedbackPanelRenderer();
		
		private AnnotationEditorFeedbackPanel annotationEditor = new AnnotationEditorFeedbackPanel();
		private FeedbackPanelHtmlRendererInstance annotationEditorRenderer;
		private JComboBox country = new JComboBox();
		private JCheckBox exclude = new JCheckBox("Exclude Paragraph");
		
		public CountryRegionFeedbackPanel() {
			this.country.setEditable(false);
			
			JPanel functionPanel = new JPanel(new BorderLayout(), true);
			functionPanel.add(this.country, BorderLayout.WEST);
			functionPanel.add(this.exclude, BorderLayout.EAST);
			this.add(functionPanel, BorderLayout.NORTH);
			
			this.annotationEditor.setFontSize(12);
			this.annotationEditor.setFontName("Verdana");
			this.add(this.annotationEditor, BorderLayout.CENTER);
		}
		
		void setAnnotation(MutableAnnotation annotation) {
			this.annotationEditor.addAnnotation(annotation);
		}
		
		void setCountries(String[] countries) {
			this.country.setModel(new DefaultComboBoxModel(countries));
		}
		
		void setCountry(String country) {
			this.country.setSelectedItem(country);
		}
		
		void addDetailType(String detailType, Color color) {
			this.annotationEditor.addDetailType(detailType, color);
		}
		
		void writeChanges(MutableAnnotation target) {
			AnnotationEditorFeedbackPanel.writeChanges(this.annotationEditor.getTokenStatesAt(0), target);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return (this.getDecisionComplexity() + this.getDecisionCount());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return this.annotationEditor.getDecisionComplexity();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.annotationEditor.getDecisionCount();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			super.writeData(bw);
			
			//	add exclusion & countries
			bw.write(this.exclude.isSelected() ? "E" : "K");
			bw.newLine();
			bw.write(this.country.getSelectedItem().toString());
			bw.newLine();
			for (int c = 0; c < this.country.getItemCount(); c++) {
				bw.write(this.country.getItemAt(c).toString());
				bw.newLine();
			}
			bw.newLine();
			
			//	write content
			this.annotationEditor.writeData(bw);
			
			//	send data
			bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			super.initFields(br);
			
			//	read exclusion & selected country
			this.exclude.setSelected("E".equals(br.readLine()));
			String selectedCountry = br.readLine();
			
			//	read country list
			String line;
			ArrayList countryList = new ArrayList();
			while (((line = br.readLine()) != null) && (line.length() != 0))
				countryList.add(line);
			Collections.sort(countryList);
			this.setCountries((String[]) countryList.toArray(new String[countryList.size()]));
			this.setCountry(selectedCountry);
			
			//	read content
			this.annotationEditor.initFields(br);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			Properties fs = this.annotationEditor.getFieldStates();
			if (fs == null)
				return null;
			fs.setProperty(("country" + 0), this.country.getSelectedItem().toString());
			fs.setProperty(("exclude" + 0), (this.exclude.isSelected() ? "E" : "K"));
			return fs;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			this.annotationEditor.setFieldStates(states);
			String country = states.getProperty("country" + 0);
			if (country != null)
				this.setCountry(country);
			this.exclude.setSelected("E".equals(states.getProperty(("exclude" + 0), "K")));
		}
		
		private final FeedbackPanelHtmlRendererInstance getRendererInstance() {
			if (this.annotationEditorRenderer == null)
				this.annotationEditorRenderer = aefpRenderer.getRendererInstance(this.annotationEditor);
			return this.annotationEditorRenderer;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			this.getRendererInstance().writeJavaScriptInitFunctionBody(out);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			this.getRendererInstance().writeJavaScript(out);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			this.getRendererInstance().writeCssStyles(out);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			bw.writeLine("<table class=\"part\" id=\"part" + 0 + "\">");
			bw.writeLine("<tr>");
			
			bw.writeLine("<td>");
			bw.writeLine("Country:");
			bw.writeLine("<select name=\"country" + 0 + "\">");
			for (int c = 0; c < this.country.getItemCount(); c++) {
				bw.writeLine("<option value=\"" + this.country.getItemAt(c).toString() + "\"" + ((c == this.country.getSelectedIndex()) ? " selected" : "") + ">" + this.country.getItemAt(c).toString() + "</option>");
			}
			bw.writeLine("</select>");
			bw.writeLine("</td>");
			
			bw.writeLine("<td>");
			bw.writeLine("</td>");
			
			bw.writeLine("<td>");
			bw.writeLine("Exclude Paragraph ");
			bw.writeLine("<input name=\"exclude" + 0 + "\" type=\"checkbox\" value=\"E\"" + (this.exclude.isSelected() ? " checked" : "") + ">");
			bw.writeLine("</td>");
			
			bw.writeLine("</tr>");
			bw.writeLine("</table>");
			
			this.getRendererInstance().writePanelBody(bw);
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			this.getRendererInstance().readResponse(response);
			
			String country = response.getProperty("country" + 0);
			if (country != null)
				this.setCountry(country);
			this.exclude.setSelected("E".equals(response.getProperty(("exclude" + 0), "K")));
		}
	}
	
	/*
	 * TODOne: use code from AnnotationEditorFeedbackPanel and its renderer to
	 * create a feedback panel for materials citations. Include
	 * truncateSeparators() and splitAll() in the Java and the JavaScript code,
	 * and and line breaking between annotations
	 */
	
	/**
	 * Feedback panel for correcting materials citation annotations. Has to be
	 * public and static due to the class visibility and loadability
	 * requirements of the remote feedback API.
	 * 
	 * @author sautter
	 */
	public static class MaterialsCitationFeedbackPanel extends FeedbackPanel {
		
		private static final String SEPARATORS = ",;.:";
		
		private String fontName = "Verdana";
		private int fontSize = 12;
		private Color mcColor = Color.YELLOW;
		
		private SimpleAttributeSet textStyle = new SimpleAttributeSet();
		private SimpleAttributeSet noMcStyle = new SimpleAttributeSet();
		private SimpleAttributeSet mcStyle = new SimpleAttributeSet();
		
		private JLabel detailTypeLegend;
		
		void setFontName(String fontName) {
			this.fontName = fontName;
			this.textStyle.addAttribute(StyleConstants.FontConstants.Family, this.fontName);
		}
		
		void setFontSize(int fontSize) {
			this.fontSize = fontSize;
			this.textStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(this.fontSize));
		}
		
		void setMcColor(Color mcColor) {
			this.mcColor = mcColor;
			this.mcStyle.addAttribute(StyleConstants.ColorConstants.Background, this.mcColor);
			this.layoutLegend();
		}
		
		private void layoutLegend() {
			if (this.detailTypeLegend != null)
				this.functionPanel.remove(this.detailTypeLegend);
			int rgb = Color.YELLOW.getRGB();
			BufferedImage iconImage = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < iconImage.getWidth(); x++)
				for (int y = 0; y < iconImage.getHeight(); y++)
					iconImage.setRGB(x, y, rgb);
			this.detailTypeLegend = new JLabel(MATERIALS_CITATION_ANNOTATION_TYPE, new ImageIcon(iconImage), JLabel.LEFT);
			this.detailTypeLegend.setBorder(BorderFactory.createEtchedBorder());
			this.functionPanel.add(this.detailTypeLegend, BorderLayout.CENTER);
		}
		
		private JPanel functionPanel;
		private AnnotationDetailEditor annotationEditor;
		private JCheckBox exclude = new JCheckBox("Exclude Paragraph");
		
		public MaterialsCitationFeedbackPanel() {
			
			this.textStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(this.fontSize));
			this.textStyle.addAttribute(StyleConstants.FontConstants.Family, this.fontName);
			this.noMcStyle.addAttribute(StyleConstants.ColorConstants.Foreground, Color.BLACK);
			this.noMcStyle.addAttribute(StyleConstants.ColorConstants.Background, Color.WHITE);
			this.mcStyle.addAttribute(StyleConstants.ColorConstants.Background, this.mcColor);
			
			this.functionPanel = new JPanel(new BorderLayout(), true);
			this.functionPanel.add(this.exclude, BorderLayout.EAST);
			this.add(this.functionPanel, BorderLayout.NORTH);
			
			this.layoutLegend();
		}
		
		void setAnnotation(MutableAnnotation annotation) {
			this.annotationEditor = new AnnotationDetailEditor(annotation);
			this.add(this.annotationEditor, BorderLayout.CENTER);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return (this.getDecisionComplexity() + this.getDecisionCount());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return 2;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.annotationEditor.tokens.length;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			super.writeData(bw);
			
			//	send font layout
			bw.write(this.fontName);
			bw.newLine();
			bw.write("" + this.fontSize);
			bw.newLine();
			bw.write(getRGB(this.mcColor));
			bw.newLine();
			
			//	add exclusion
			bw.write(this.exclude.isSelected() ? "E" : "K");
			bw.newLine();
			
			//	write data
			if (this.annotationEditor != null) {
				for (int t = 0; t < this.annotationEditor.tokens.length; t++) {
					bw.write(URLEncoder.encode((this.annotationEditor.tokens[t].space + this.annotationEditor.tokens[t].value), "UTF-8") + " " + this.annotationEditor.tokens[t].state);
					bw.newLine();
				}
			}
			bw.newLine();
			
			//	send data
			bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			super.initFields(br);
			
			//	read font layout
			this.setFontName(br.readLine());
			this.setFontSize(Integer.parseInt(br.readLine()));
			this.setMcColor(getColor(br.readLine()));
			
			//	read exclusion
			this.exclude.setSelected("E".equals(br.readLine()));
			
			//	read lines
			String line;
			ArrayList elTokenLineBuffer = new ArrayList();
			while ((line = br.readLine()) != null) {
				
				//	separator line, evaluate buffered data lines
				if (line.length() == 0) {
					
					//	initialize from filled buffer only
					if (elTokenLineBuffer.size() != 0) {
						
						//	initialize data structures
						StringBuffer elString = new StringBuffer("");
						String[] elTokenStates = new String[elTokenLineBuffer.size()];
//						String[] elTokenTypes = new String[elTokenLineBuffer.size()];
						
						//	parse buffered data lines
						for (int t = 0; t < elTokenLineBuffer.size(); t++) {
							String[] elTokenData = ((String) elTokenLineBuffer.get(t)).split("\\s");
							elString.append(URLDecoder.decode(elTokenData[0], "UTF-8"));
							elTokenStates[t] = elTokenData[1];
//							elTokenTypes[t] = ((elTokenData.length == 2) ? null : URLDecoder.decode(elTokenData[2], "UTF-8"));
						}
						
//						System.out.println("McFeedbackPanel: string is " + elString);
						
						//	get or create editor line
						if (this.annotationEditor == null) {
							MutableAnnotation elAnnotation = Gamta.newDocument(Gamta.newTokenSequence(elString, null));
//							System.out.println(" - " + elAnnotation.toXML());
							this.setAnnotation(elAnnotation);
						}
						
						//	transfer token states
						for (int t = 0; t < this.annotationEditor.tokens.length; t++)
							this.annotationEditor.tokens[t].state = elTokenStates[t];
						
						this.annotationEditor.applySpans(true);
						
						//	clean up data buffer
						elTokenLineBuffer.clear();
					}
				}
				
				//	normal data line, just buffer it for now
				else elTokenLineBuffer.add(line);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			Properties fs = new Properties();
			for (int t = 0; t < this.annotationEditor.tokens.length; t++)
				fs.setProperty(("part" + 0 + "_token" + t + "_state"), this.annotationEditor.tokens[t].state);
			fs.setProperty(("exclude" + 0), (this.exclude.isSelected() ? "E" : "K"));
			return fs;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			for (int t = 0; t < this.annotationEditor.tokens.length; t++)
				this.annotationEditor.tokens[t].state = states.getProperty(("part" + 0 + "_token" + t + "_state"), AnnotationEditorFeedbackPanel.OTHER);
			this.annotationEditor.applySpans(true);
			this.exclude.setSelected("E".equals(states.getProperty(("exclude" + 0), "K")));
		}
		
		private static class AnnotationDetailEditorToken {
			final int offset;
			final String space;
			final String value;
			String state = AnnotationEditorFeedbackPanel.OTHER;
			AnnotationDetailEditorToken(int offset, String space, String value) {
				this.offset = offset;
				this.space = space;
				this.value = value;
			}
		}
		
		private class AnnotationDetailEditor extends JPanel {
			
			private MutableAnnotation annotation;
			AnnotationDetailEditorToken[] tokens;
			
			private JTextPane annotationDisplay = new JTextPane();
			private StyledDocument annotationDisplayDocument;
			
			AnnotationDetailEditor(MutableAnnotation annotation) {
				super(new BorderLayout(), true);
				this.annotation = annotation;
				
				//	init token data
				this.tokens = new AnnotationDetailEditorToken[this.annotation.size()];
				int tokenOffset = 0;
				for (int t = 0; t < this.annotation.size(); t++) {
					this.tokens[t] = new AnnotationDetailEditorToken(tokenOffset, ((t == 0) ? "" : " "), this.annotation.valueAt(t));
					tokenOffset += (this.tokens[t].value.length() + 1);
				}
				this.initTokenStates();
				
				//	init display
				this.annotationDisplay.setEditable(false);
				
				//	display text
				StringBuffer adText = new StringBuffer();
				for (int t = 0; t < this.tokens.length; t++)
					adText.append(this.tokens[t].space + this.tokens[t].value);
				this.annotationDisplay.setText(adText.toString());
				this.annotationDisplayDocument = this.annotationDisplay.getStyledDocument();
				this.applySpans(true);
				
				//	init context menu
				this.annotationDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1) {
							displayContextMenu(me);
						}
					}
				});
				
				//	display it
				this.add(this.annotationDisplay, BorderLayout.CENTER);
			}
			
			void initTokenStates() {
				Annotation[] annotations = this.annotation.getAnnotations();
				int lastEnd = 0;
				for (int a = 0; a < annotations.length; a++) {
					if (MATERIALS_CITATION_ANNOTATION_TYPE.equals(annotations[a].getType()) && (lastEnd <= annotations[a].getStartIndex())) {
						for (int t = lastEnd; t < annotations[a].getStartIndex(); t++)
							this.tokens[t].state = AnnotationEditorFeedbackPanel.OTHER;
						
						for (int t = annotations[a].getStartIndex(); t < annotations[a].getEndIndex(); t++)
							this.tokens[t].state = ((t == annotations[a].getStartIndex()) ? AnnotationEditorFeedbackPanel.START : AnnotationEditorFeedbackPanel.CONTINUE);
						
						lastEnd = annotations[a].getEndIndex();
					}
				}
			}
			
			void applySpans(boolean text) {
				
				//	adjust line breaks
				this.adjustWhitespace();
				
				//	reset display text
				if (text)
					this.annotationDisplayDocument.setCharacterAttributes(0, this.annotationDisplayDocument.getLength(), textStyle, false);
				
				//	reset highlights
				this.annotationDisplayDocument.setCharacterAttributes(0, this.annotationDisplayDocument.getLength(), noMcStyle, false);
				
				//	highlight annotated tokens
				for (int t = 0; t < this.tokens.length; t++) {
					if (!AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[t].state)) {
						int o = this.tokens[t].offset - (AnnotationEditorFeedbackPanel.START.equals(this.tokens[t].state) ? 0 : this.tokens[t].space.length());
						int l = this.tokens[t].value.length() + (AnnotationEditorFeedbackPanel.START.equals(this.tokens[t].state) ? 0 : this.tokens[t].space.length());
						this.annotationDisplayDocument.setCharacterAttributes(o, l, mcStyle, false);
					}
				}

				//	make changes visible
				this.annotationDisplay.validate();
			}
			
			private void adjustWhitespace() {
				this.annotationDisplay.setEditable(true);
				int cp = this.annotationDisplay.getCaret().getDot();
				for (int t = 1; t < this.tokens.length; t++) {
					String space = " ";
					
					//	always break before start token
					if (AnnotationEditorFeedbackPanel.START.equals(this.tokens[t].state))
						space = "\n";
					
					//	break between continue and other token
					else if (AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[t].state) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t-1].state))
						space = "\n";
					
					//	set space
					this.annotationDisplay.getCaret().setDot(this.tokens[t].offset - 1);
					this.annotationDisplay.getCaret().moveDot(this.tokens[t].offset);
					this.annotationDisplay.replaceSelection(space);
				}
				this.annotationDisplay.getCaret().setDot(cp);
				this.annotationDisplay.setEditable(false);
			}
			
			int indexAtOffset(int offset, boolean isStart) {
				for (int t = 0; t < this.tokens.length; t++)
					if (offset < (this.tokens[t].offset + (isStart ? this.tokens[t].value.length() : 1)))
						return (t - (isStart ? 0 : 1));
				return (isStart ? -1 : (this.tokens.length - 1));
			}
			
			void displayContextMenu(MouseEvent me) {
				
				//	get offsets
				int startOffset = this.annotationDisplay.getSelectionStart();
				int endOffset = this.annotationDisplay.getSelectionEnd();
				
				//	compute indices
				final int start;
				final int end;
				
				//	no selection
				if (startOffset == endOffset) {
					start = this.indexAtOffset(startOffset, true);
					end = this.indexAtOffset(endOffset, false);
					if (start != end) {
//						System.out.println("Click at " + startOffset + " between tokens");
						return;
					}
//					System.out.println("Click at " + startOffset + " in token " + start);
				}
				
				//	some selection
				else {
					
					//	swap offsets if inverted
					if (endOffset < startOffset) {
						int temp = endOffset;
						endOffset = startOffset;
						startOffset = temp;
					}
					
					//	get indices
					start = this.indexAtOffset(startOffset, true);
					end = this.indexAtOffset(endOffset, false);
					if (end < start) {
//						System.out.println("Selection in whitespace at " + startOffset);
						return;
					}
				}
				
				int aCount = 0;
				boolean gotOther = false;
				for (int t = start; t <= end; t++) {
					if (AnnotationEditorFeedbackPanel.START.equals(this.tokens[t].state) || ((t == start) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t].state)))
						aCount++;
					else if (AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[t].state))
						gotOther = true;
				}
				
				JPopupMenu pm = new JPopupMenu();
				JMenuItem mi;
				
				//	truncate separators
				mi = new JMenuItem("Truncate separator chars");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						truncateSeparators();
					}
				});
				pm.add(mi);
				
				//	annotate
				if (aCount == 0) {
					mi = new JMenuItem("Annotate as '" + MATERIALS_CITATION_ANNOTATION_TYPE + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							annotate(start, end);
						}
					});
					pm.add(mi);
				}
				
				//	extend
				if ((aCount == 1) && gotOther) {
					mi = new JMenuItem("Extend '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotation");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							extend(start, end);
						}
					});
					pm.add(mi);
				}
				
				//	remove
				if (aCount == 1) {
					mi = new JMenuItem("Remove '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotation");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							remove(start, end);
						}
					});
					pm.add(mi);
				}
				
				//	merge
				if (aCount > 1) {
					mi = new JMenuItem("Merge '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotations");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							merge(start, end);
						}
					});
					pm.add(mi);
				}
				
				//	split
				if ((aCount == 1) && (start == end)) {
					String value = this.tokens[start].value;
					
					//	on AnnotationEditorFeedbackPanel.CONTINUE token ==> split before possible
					if (AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[start].state)) {
						mi = new JMenuItem("Split '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotation before '" + value + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								split(start, false, true);
							}
						});
						pm.add(mi);
					}
					
					//	next is AnnotationEditorFeedbackPanel.CONTINUE token ==> split around and after possible
					if (((start + 1) < this.tokens.length) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[start + 1].state)) {
						
						//	on AnnotationEditorFeedbackPanel.CONTINUE token ==> split around possible
						if (AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[start].state)) {
							mi = new JMenuItem("Split '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotation around '" + value + "'");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									split(start, false, false);
								}
							});
							pm.add(mi);
						}
						
						mi = new JMenuItem("Split '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotation after '" + value + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								split(start, true, false);
							}
						});
						pm.add(mi);
					}
				}
				
				//	split all
				if (start == end) {
					String value = this.tokens[start].value;
					
					//	on AnnotationEditorFeedbackPanel.CONTINUE token ==> split before possible
					mi = new JMenuItem("Split all '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotation before all '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							splitAll(start, false, true);
						}
					});
					pm.add(mi);
					
					mi = new JMenuItem("Split all all '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotations around all '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							splitAll(start, false, false);
						}
					});
					pm.add(mi);
					
					mi = new JMenuItem("Split all all '" + MATERIALS_CITATION_ANNOTATION_TYPE + "' annotations after all '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							splitAll(start, true, false);
						}
					});
					pm.add(mi);
				}
				
				if (pm.getComponentCount() != 0)
					pm.show(this.annotationDisplay, me.getX(), me.getY());
			}
			
			void annotate(int start, int end) {
				for (int t = start; t <= end; t++)
					this.tokens[t].state = ((t == start) ? AnnotationEditorFeedbackPanel.START : AnnotationEditorFeedbackPanel.CONTINUE);
				this.applySpans(false);
			}
			
			void extend(int start, int end) {
				for (int t = start; t <= end; t++) {
					if (!AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t].state))
						this.tokens[t].state = ((t == start) ? AnnotationEditorFeedbackPanel.START : AnnotationEditorFeedbackPanel.CONTINUE);
				}
				this.applySpans(false);
			}
			
			void merge(int start, int end) {
				while (AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[start].state))
					start++;
				while (!AnnotationEditorFeedbackPanel.START.equals(this.tokens[start].state))
					start--;
				
				while (AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[end].state))
					end--;
				
				for (int t = (start + 1); t <= end; t++)
					this.tokens[t].state = AnnotationEditorFeedbackPanel.CONTINUE;
				
				this.applySpans(false);
			}
			
			void split(int index, boolean includeInFirst, boolean includeInSecond) {
				
				//	split after
				if (includeInFirst)
					this.tokens[index + 1].state = AnnotationEditorFeedbackPanel.START;
				
				//	split before
				else if (includeInSecond)
					this.tokens[index].state = AnnotationEditorFeedbackPanel.START;
				
				//	split around
				else {
					this.tokens[index].state = AnnotationEditorFeedbackPanel.OTHER;
					this.tokens[index + 1].state = AnnotationEditorFeedbackPanel.START;
				}
				
				this.applySpans(false);
			}
			
			void splitAll(int index, boolean includeInFirst, boolean includeInSecond) {
				String value = this.tokens[index].value;
				
				//	check token by token
				for (int t = 0; t < this.tokens.length; t++) {
					
					//	got split point
					if (value.equals(this.tokens[t].value)) {
						
						//	split after
						if (includeInFirst) {
							if (((t+1) < this.tokens.length) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t+1].state))
								this.tokens[t + 1].state = AnnotationEditorFeedbackPanel.START;
						}
						
						//	split before
						else if (includeInSecond) {
							if (AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t].state))
								this.tokens[t].state = AnnotationEditorFeedbackPanel.START;
						}
						
						//	split around
						else {
							if (AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t].state)) {
								this.tokens[t].state = AnnotationEditorFeedbackPanel.OTHER;
								if (((t+1) < this.tokens.length) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t+1].state))
									this.tokens[t + 1].state = AnnotationEditorFeedbackPanel.START;
							}
						}
					}
				}
				
				this.applySpans(false);
			}
			
			void remove(int start, int end) {
				while (AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[start].state))
					start++;
				while (!AnnotationEditorFeedbackPanel.START.equals(this.tokens[start].state))
					start--;
				
				while (AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[end].state))
					end--;
				while (((end + 1) < this.tokens.length) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[end+1].state))
					end++;
				
				for (int t = start; t <= end; t++) {
					this.tokens[t].state = AnnotationEditorFeedbackPanel.OTHER;
				}
				this.applySpans(false);
			}
			
			void truncateSeparators() {
				
				//	check token by token
				for (int t = 0; t < this.tokens.length; t++) {
					
					//	got split point
					if (SEPARATORS.indexOf(this.tokens[t].value) != -1) {
						
						//	start of annotation
						if (AnnotationEditorFeedbackPanel.START.equals(this.tokens[t].state)) {
							this.tokens[t].state = AnnotationEditorFeedbackPanel.OTHER;
							if (((t+1) < this.tokens.length) && AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t+1].state))
								this.tokens[t + 1].state = AnnotationEditorFeedbackPanel.START;
						}
						
						//	end of annotation
						else if (AnnotationEditorFeedbackPanel.CONTINUE.equals(this.tokens[t].state) && ((t+1) < this.tokens.length) && AnnotationEditorFeedbackPanel.OTHER.equals(this.tokens[t+1].state)) {
							this.tokens[t].state = AnnotationEditorFeedbackPanel.OTHER;
							t -= 2; // jump back so further tailing separators can be truncated
						}
					}
				}
				
				this.applySpans(false);
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
			
			bw.writeLine("var _DEBUG = false;");
			bw.writeLine("");
			bw.writeLine("var _replaceContext = false;		// replace the system context menu?");
			bw.writeLine("var _mouseOverContext = false;		// is the mouse over the context menu?");
			bw.writeLine("var _contextHasContent = false;");
			bw.writeLine("");
			bw.writeLine("var _divContext;	// makes my life easier");
			bw.writeLine("var _feedbackForm;");
			bw.writeLine("");
			bw.writeLine("var _colors = new Object();");
			bw.writeLine("");
			
			
			bw.writeLine("function initContext() {");
			bw.writeLine("	_divContext = $$('divContext');");
			bw.writeLine("	_feedbackForm = $$('feedbackForm');");
			bw.writeLine("	");
			bw.writeLine("	_divContext.onmouseover = function() { _mouseOverContext = true; };");
			bw.writeLine("	_divContext.onmouseout = function() { _mouseOverContext = false; };");
			bw.writeLine("	");
			bw.writeLine("	document.body.onmousedown = contextMouseDown;");
			bw.writeLine("	document.body.oncontextmenu = contextShow;");
			bw.writeLine("	");
			
			bw.writeLine("	_colors." + MATERIALS_CITATION_ANNOTATION_TYPE + " = '" + FeedbackPanel.getRGB(this.mcColor) + "';");
			
			bw.writeLine("");
			bw.writeLine("");
			bw.writeLine("	var p = 0;");
			bw.writeLine("	var t = 1;");
			bw.writeLine("	while (t != 0) {");
			bw.writeLine("	  _part = p;");
			bw.writeLine("    t = 0;");
			bw.writeLine("    while ($('token' + t)) {");
			bw.writeLine("      var token = $('token' + t);");
			bw.writeLine("      if (token) {");
			bw.writeLine("        token.onmousedown = startSelection;");
			bw.writeLine("        token.onmouseup = endSelection;");
			bw.writeLine("      }");
			bw.writeLine("      var space = $('space' + t);");
			bw.writeLine("      if (space) {");
			bw.writeLine("        space.onmousedown = startSelection;");
			bw.writeLine("        space.onmouseup = endSelection;");
			bw.writeLine("        if (space.innerText && (space.innerText == ' '))");
			bw.writeLine("          space.original = '\\u0020';");
			bw.writeLine("        else if (space.textContent && (space.textContent == ' '))");
			bw.writeLine("          space.original = '\\u0020';");
			bw.writeLine("        else space.original = '';");
			bw.writeLine("      }");
			bw.writeLine("      if (t == 0) p++;");
			bw.writeLine("      t++;");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    if (t != 0)");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    ");
			bw.writeLine("    _part = null;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function adjustColors() {");
			bw.writeLine("  var lastState = 'O';");
			bw.writeLine("  var t = 0;");
			bw.writeLine("  while ($('token' + t)) {");
			bw.writeLine("    var token = $('token' + t);");
			bw.writeLine("    var space = $('space' + t);");
			bw.writeLine("    if (token) {");
			bw.writeLine("      var state = getState(t);");
			bw.writeLine("      var type = getType(t);");
			bw.writeLine("      if (state && (state == 'S')) {");
			bw.writeLine("        if (type && _colors[type]) {");
			bw.writeLine("          token.style.backgroundColor = _colors[type];");
			bw.writeLine("          if (space) {");
			bw.writeLine("            space.style.backgroundColor = 'FFFFFF';");
			bw.writeLine("            adjustSpace(space, true);");
			bw.writeLine("          }");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("      else if (state && (state == 'C')) {");
			bw.writeLine("        if (type && _colors[type]) {");
			bw.writeLine("          token.style.backgroundColor = _colors[type];");
			bw.writeLine("          if (space) {");
			bw.writeLine("            space.style.backgroundColor = _colors[type];");
			bw.writeLine("            adjustSpace(space, false);");
			bw.writeLine("          }");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("      else {");
			bw.writeLine("        token.style.backgroundColor = 'FFFFFF';");
			bw.writeLine("        if (space) {");
			bw.writeLine("          space.style.backgroundColor = 'FFFFFF';");
			bw.writeLine("          adjustSpace(space, (lastState && (lastState != 'O')));");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("      lastState = state;");
			bw.writeLine("    }");
			bw.writeLine("    t++;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function adjustSpace(space, doLineBreak) {");
			bw.writeLine("  while(space.firstChild) {");
			bw.writeLine("    space.removeChild(space.firstChild);");
			bw.writeLine("  }");
			bw.writeLine("  var node;");
			bw.writeLine("  if (doLineBreak) {");
			bw.writeLine("    node = document.createElement('BR');");
			bw.writeLine("  }");
			bw.writeLine("  else {");
			bw.writeLine("    node = document.createTextNode(space.original);");
			bw.writeLine("  }");
			bw.writeLine("  space.appendChild(node);");
			bw.writeLine("}");
			
			bw.writeLine("function adjustContext() {");
			bw.writeLine("  _contextHasContent = false;");
			bw.writeLine("  if (_part == null) return;");
			bw.writeLine("  ");
			bw.writeLine("  var firstS = -1;");
			bw.writeLine("  var firstC = -1;");
			bw.writeLine("  var firstO = -1;");
			bw.writeLine("  ");
			bw.writeLine("  var aCount = 0;");
			bw.writeLine("  ");
			bw.writeLine("  if ((_start != null) && (_end != null)) {");
			bw.writeLine("       ");
			bw.writeLine("     for (var t = _start; t <= _end; t++) {");
			bw.writeLine("      var token = $('token' + t);");
			bw.writeLine("      if (token) {");
			bw.writeLine("        var state = getState(t);");
			bw.writeLine("        ");
			bw.writeLine("        if (state == 'S') {");
			bw.writeLine("          aCount++;");
			bw.writeLine("          if (firstS == -1)");
			bw.writeLine("            firstS = t;");
			bw.writeLine("        }");
			bw.writeLine("        if (state == 'C') {");
			bw.writeLine("          if (firstC == -1) {");
			bw.writeLine("            firstC = t;");
			bw.writeLine("            if (firstS == -1)");
			bw.writeLine("              aCount++;");
			bw.writeLine("          }");
			bw.writeLine("        }");
			bw.writeLine("        else if (state == 'O') {");
			bw.writeLine("          if (firstO == -1)");
			bw.writeLine("            firstO = t;");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("     }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  if (_DEBUG) alert('part = ' + _part + ', firstS = ' + firstS + ', firstC = ' + firstC + ', firstO = ' + firstO + ', aCount = ' + aCount + ', _type = ' + _type);");
			bw.writeLine("  ");
			bw.writeLine("  $$('showSelectedTokens').style.display = (_DEBUG ? '' : 'none');");
			bw.writeLine("  _contextHasContent = (_selection || _type || (aCount == 1));");
			bw.writeLine("  ");
			bw.writeLine("  //  annotate functions");
			bw.writeLine("  var a = 0;");
			bw.writeLine("  while ($$('annotate' + a)) {");
			bw.writeLine("    $$('annotate' + a).style.display = (((aCount == 0) && _selection) ? '' : 'none');");
			bw.writeLine("    a++;");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  //  remove");
			bw.writeLine("  $$('remove').style.display = ((aCount == 1) ? '' : 'none');");
			bw.writeLine("  ");
			bw.writeLine("  //  extend");
			bw.writeLine("  $$('extend').style.display = (((aCount == 1) && (firstO != -1)) ? '' : 'none');");
			bw.writeLine("  ");
			bw.writeLine("  //  merge");
			bw.writeLine("  $$('merge').style.display = ((_type && (aCount > 1)) ? '' : 'none');");
			bw.writeLine("  ");
			bw.writeLine("  //  adjust splitting functions");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var token = $('token' + _start);");
			bw.writeLine("    var nextToken = $('token' + (_start + 1));");
			bw.writeLine("    ");
			bw.writeLine("    $$('splitAroundItem').innerHTML = ('Split Annotation around \\'' + token.innerHTML + '\\'');");
			bw.writeLine("    $$('splitBeforeItem').innerHTML = ('Split Annotation before \\'' + token.innerHTML + '\\'');");
			bw.writeLine("    $$('splitAfterItem').innerHTML = ('Split Annotation after \\'' + token.innerHTML + '\\'');");
			bw.writeLine("    ");
			bw.writeLine("    $$('splitAroundItemAll').innerHTML = ('Split Annotations around all \\'' + token.innerHTML + '\\'');");
			bw.writeLine("    $$('splitBeforeItemAll').innerHTML = ('Split Annotations before all \\'' + token.innerHTML + '\\'');");
			bw.writeLine("    $$('splitAfterItemAll').innerHTML = ('Split Annotations after all \\'' + token.innerHTML + '\\'');");
			bw.writeLine("    ");
			bw.writeLine("    $$('splitAround').style.display = (((getState(_start) == 'C') && nextToken && (getState(_start + 1) == 'C')) ? '' : 'none');");
			bw.writeLine("    $$('splitBefore').style.display = ((getState(_start) == 'C') ? '' : 'none');");
			bw.writeLine("    $$('splitAfter').style.display = ((nextToken && (getState(_start + 1) == 'C')) ? '' : 'none');");
			bw.writeLine("    ");
			bw.writeLine("    $$('splitAroundAll').style.display = '';");
			bw.writeLine("    $$('splitBeforeAll').style.display = '';");
			bw.writeLine("    $$('splitAfterAll').style.display = '';");
			bw.writeLine("  }");
			bw.writeLine("  else {");
			bw.writeLine("    $$('splitAround').style.display = 'none';");
			bw.writeLine("    $$('splitBefore').style.display = 'none';");
			bw.writeLine("    $$('splitAfter').style.display = 'none';");
			bw.writeLine("    ");
			bw.writeLine("    $$('splitAroundAll').style.display = 'none';");
			bw.writeLine("    $$('splitBeforeAll').style.display = 'none';");
			bw.writeLine("    $$('splitAfterAll').style.display = 'none';");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("// call from the onMouseDown event, passing the event if standards compliant");
			bw.writeLine("function contextMouseDown(event) {");
			bw.writeLine("  if (_mouseOverContext)");
			bw.writeLine("    return;");
			bw.writeLine("");
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
			bw.writeLine("  _part = null;");
			bw.writeLine("  _start = null;");
			bw.writeLine("  _end = null;");
			bw.writeLine("  _selection = null;");
			bw.writeLine("  _type = null;");
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
			bw.writeLine("    adjustContext();");
			bw.writeLine("    ");
			bw.writeLine("    if (_contextHasContent) {");
			bw.writeLine("      ");
			bw.writeLine("      // document.body.scrollTop does not work in IE");
			bw.writeLine("      var scrollTop = document.body.scrollTop ? document.body.scrollTop :");
			bw.writeLine("        document.documentElement.scrollTop;");
			bw.writeLine("      var scrollLeft = document.body.scrollLeft ? document.body.scrollLeft :");
			bw.writeLine("        document.documentElement.scrollLeft;");
			bw.writeLine("      ");
			bw.writeLine("      // hide the menu first to avoid an 'up-then-over' visual effect");
			bw.writeLine("      _divContext.style.display = 'none';");
			bw.writeLine("      _divContext.style.left = event.clientX + scrollLeft + 'px';");
			bw.writeLine("      _divContext.style.top = event.clientY + scrollTop + 'px';");
			bw.writeLine("      _divContext.style.display = 'block';");
			bw.writeLine("      ");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    _contextHasContent = false;");
			bw.writeLine("    _replaceContext = false;");
			bw.writeLine("");
			bw.writeLine("    return false;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("// comes from prototype.js; this is simply easier on the eyes and fingers");
			bw.writeLine("function $(id) {");
			bw.writeLine("  return ((_part == null) ? $$(id) : $$('part' + _part + '_' + id));");
			bw.writeLine("}");
			bw.writeLine("function $$(id) {");
			bw.writeLine("  return document.getElementById(id);");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("");


			bw.writeLine("// working variables for capturing active tokens in Netscape");
			bw.writeLine("var _activePart = null;");
			bw.writeLine("var _activeToken = null;");
			bw.writeLine("");

			bw.writeLine("function activateToken(part, index) {");
			bw.writeLine("  _activePart = part;");
			bw.writeLine("  _activeToken = index;");
			bw.writeLine("}");
			bw.writeLine("");

			bw.writeLine("function inactivateToken(part, index) {");
			bw.writeLine("  _activePart = null;");
			bw.writeLine("  _activeToken = null;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("");
			
			bw.writeLine("// working variables for capturing clicks on tokens");
			bw.writeLine("var _clickedPart = null;");
			bw.writeLine("var _clickedToken = null;");
			bw.writeLine("");

			bw.writeLine("function clickToken(part, index) {");
			bw.writeLine("  _clickedPart = part;");
			bw.writeLine("  _clickedToken = index;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("// working variables for capturing selections");
			bw.writeLine("var _selectionStartPart = null;");
			bw.writeLine("var _selectionStart = null;");
			bw.writeLine("var _selectionEndPart = null;");
			bw.writeLine("var _selectionEnd = null;");
			bw.writeLine("");
			
			bw.writeLine("function startSelection(event) {");
			bw.writeLine("  if (!event || (event == null)) event = window.event;");
			bw.writeLine("  if ((_activeToken != null) && event && isLeftClick(event.button)) {");
			bw.writeLine("    _selectionStartPart = _activePart;");
			bw.writeLine("    _selectionStart = _activeToken;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");

			bw.writeLine("function endSelection(event) {");
			bw.writeLine("  if (!event || (event == null)) event = window.event;");
			bw.writeLine("  if ((_activeToken != null) && event && isLeftClick(event.button)) {");
			bw.writeLine("    _selectionEndPart = _activePart;");
			bw.writeLine("    _selectionEnd = _activeToken;");
			bw.writeLine("  }");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function isLeftClick(button) {");
			bw.writeLine("  return (((navigator.appName == 'Netscape') && (button == 0)) || (button == 1));");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("// state variables to use for context menu");
			bw.writeLine("var _part = null;");
			bw.writeLine("");
			bw.writeLine("var _start = null;");
			bw.writeLine("var _end = null;");
			bw.writeLine("var _selection = null;");
			bw.writeLine("var _type = null;");
			bw.writeLine("");
			
			bw.writeLine("function getSelectionState() {");
			bw.writeLine("  if (_selectionStartPart != _selectionEndPart) {");
			bw.writeLine("    alert('Please work in one part at a time.');");
			bw.writeLine("    ");
			bw.writeLine("    _selectionStartPart = null;");
			bw.writeLine("    _selectionStart = null;");
			bw.writeLine("    _selectionEndPart = null;");
			bw.writeLine("    _selectionEnd = null;");
			bw.writeLine("    ");
			bw.writeLine("    _clickedPart = null;");
			bw.writeLine("    _clickedToken = null;");
			bw.writeLine("    ");
			bw.writeLine("    _part = null;");
			bw.writeLine("     _start = null;");
			bw.writeLine("     _end = null;");
			bw.writeLine("     _selection = null;");
			bw.writeLine("     _type = null;");
			bw.writeLine("     ");
			bw.writeLine("     return;");
			bw.writeLine("  }");
			bw.writeLine("  else {");
			bw.writeLine("    _part = _selectionStartPart;");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  ");
			bw.writeLine("  var selection = '';");
			bw.writeLine("  if (window.getSelection)");
			bw.writeLine("    selection = window.getSelection();");
			bw.writeLine("  else if (document.getSelection)");
			bw.writeLine("    selection = document.getSelection();");
			bw.writeLine("  else if (document.selection)");
			bw.writeLine("    selection = document.selection.createRange().text;");
			bw.writeLine("  ");
			bw.writeLine("  var type = null;");
			bw.writeLine("  var sameType = true;");
			bw.writeLine("  ");
			bw.writeLine("  if ((selection != '') && (_selectionStart != null) && (_selectionEnd != null)) {");
			bw.writeLine("     _start = Math.ceil(Math.min(_selectionStart, _selectionEnd));");
			bw.writeLine("     _end = Math.floor(Math.max(_selectionStart, _selectionEnd));");
			bw.writeLine("    selection = '';");
			bw.writeLine("    for (var t = _start; t <= _end; t++) {");
			bw.writeLine("      if (_start < t)");
			bw.writeLine("        selection = (selection + $('space' + t).innerHTML);");
			bw.writeLine("      ");
			bw.writeLine("      var token = $('token' + t);");
			bw.writeLine("      if (token) {");
			bw.writeLine("        selection = (selection + token.innerHTML);");
			bw.writeLine("        ");
			bw.writeLine("        if (getState(t) == 'S') {");
			bw.writeLine("          if (type == null)");
			bw.writeLine("            type = getType(t);");
			bw.writeLine("          else");
			bw.writeLine("            sameType = (sameType && (getType(t) == type));");
			bw.writeLine("        }");
			bw.writeLine("        else if (getState(t) == 'C') {");
			bw.writeLine("          if (type == null)");
			bw.writeLine("            type = getType(t);");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    _selection = selection;");
			bw.writeLine("    _type = ((type && sameType) ? type : null);");
			bw.writeLine("  }");
			bw.writeLine("  else if ((_clickedToken != null) && (_clickedPart != null)) {");
			bw.writeLine("    _part = _clickedPart;");
			bw.writeLine("     _start = _clickedToken;");
			bw.writeLine("     _end = _clickedToken;");
			bw.writeLine("     _selection = null;");
			bw.writeLine("     _type = null;");
			bw.writeLine("  }");
			bw.writeLine("  else {");
			bw.writeLine("    _part = null;");
			bw.writeLine("     _start = null;");
			bw.writeLine("     _end = null;");
			bw.writeLine("     _selection = null;");
			bw.writeLine("     _type = null;");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  _selectionStartPart = null;");
			bw.writeLine("  _selectionStart = null;");
			bw.writeLine("  _selectionEndPart = null;");
			bw.writeLine("  _selectionEnd = null;");
			bw.writeLine("  ");
			bw.writeLine("  _clickedPart = null;");
			bw.writeLine("  _clickedToken = null;");
			bw.writeLine("}");
			bw.writeLine("");
			bw.writeLine("");
			
			bw.writeLine("// context menu functions");
			bw.writeLine("function showSelectedTokens() {");
			bw.writeLine("  var selection = (_selection ? _selection : '<Nothing Selected>');");
			bw.writeLine("  closeContext();");
			bw.writeLine("  alert(selection);");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function annotate(type) {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null)) {");
			bw.writeLine("    for (var t = _start; t <= _end; t++) {");
			bw.writeLine("      var token = $('token' + t)");
			bw.writeLine("      if (token) {");
			bw.writeLine("        setState(t, ((t == _start) ? 'S' : 'C'));");
			bw.writeLine("        setType(t, type);");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    ");
			bw.writeLine("    var tokenAfter = $('token' + (_end+1));");
			bw.writeLine("    if (tokenAfter && (getState(_end + 1) == 'C'))");
			bw.writeLine("      setState((_end + 1), 'S');");
			bw.writeLine("    adjustColors();");
			bw.writeLine("  }");
			bw.writeLine("  else alert('Cannot annotate nothing !!!');");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function remove() {");
			bw.writeLine("  if ((_start != null) && (_end != null)) {");
			bw.writeLine("    while ($('token' + _start) && (getState(_start) == 'C'))");
			bw.writeLine("      _start--;");
			bw.writeLine("    while ($('token' + (_end+1)) && (getState(_end+1) == 'C'))");
			bw.writeLine("      _end++;");
			bw.writeLine("    for (var t = _start; t <= _end; t++) {");
			bw.writeLine("      var token = $('token' + t)");
			bw.writeLine("      if (token) {");
			bw.writeLine("        setState(t, 'O');");
			bw.writeLine("        setType(t, null);");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    adjustColors();");
			bw.writeLine("  }");
			bw.writeLine("  else alert('Nothing to remove!!!');");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function merge() {");
			bw.writeLine("  if (_selection && _type && (_start != null) && (_end != null)) {");
			bw.writeLine("    while ($('token' + _start) && (getState(_start) == 'O'))");
			bw.writeLine("      _start++;");
			bw.writeLine("    while ($('token' + _start) && (getState(_start) == 'C'))");
			bw.writeLine("      _start--;");
			bw.writeLine("    while ($('token' + _end) && (getState(_end) == 'O'))");
			bw.writeLine("      _end--;");
			bw.writeLine("    while ($('token' + (_end+1)) && (getState(_end+1) == 'C'))");
			bw.writeLine("      _end++;");
			bw.writeLine("    ");
			bw.writeLine("    for (var t = _start; t <= _end; t++) {");
			bw.writeLine("      var token = $('token' + t)");
			bw.writeLine("      if (token) {");
			bw.writeLine("        setState(t, ((t == _start) ? 'S' : 'C'));");
			bw.writeLine("        setType(t, _type);");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    adjustColors();");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function extend() {");
			bw.writeLine("  if (_selection && _type && (_start != null) && (_end != null)) {");
			bw.writeLine("    for (var t = _start; t <= _end; t++) {");
			bw.writeLine("      var token = $('token' + t);");
			bw.writeLine("      if (token) {");
			bw.writeLine("        setState(t, (((t > _start) || (getState(t) == 'C')) ? 'C' : 'S'));");
			bw.writeLine("        setType(t, _type);");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    adjustColors();");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			
			bw.writeLine("function splitAround() {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var token = $('token' + _start);");
			bw.writeLine("    var nextToken = $('token' + (_start+1));");
			bw.writeLine("    if (token && nextToken) {");
			bw.writeLine("       setState(_start, 'O');");
			bw.writeLine("       setType(_start, null);");
			bw.writeLine("       setState((_start + 1), 'S');");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function splitBefore() {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var token = $('token' + _start);");
			bw.writeLine("    if (token) {");
			bw.writeLine("      setState(_start, 'S');");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function splitAfter() {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var nextToken = $('token' + (_start+1));");
			bw.writeLine("    if (nextToken) {");
			bw.writeLine("      setState((_start + 1), 'S');");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;  ");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function splitAroundAll() {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var value = $('token' + _start);");
			bw.writeLine("    if (value) {");
			bw.writeLine("      var t = 0;");
			bw.writeLine("      while($('token' + t)) {");
			bw.writeLine("        var token = $('token' + t);");
			bw.writeLine("        if ((token.innerHTML == value.innerHTML) && (getState(t) == 'C')) {");
			bw.writeLine("          setState(t, 'O');");
			bw.writeLine("          setType(t, null);");
			bw.writeLine("          var nextToken = $('token' + (t+1));");
			bw.writeLine("          if (nextToken) {");
			bw.writeLine("            if (getState(t+1) == 'C')");
			bw.writeLine("              setState((t + 1), 'S');");
			bw.writeLine("          }");
			bw.writeLine("        }");
			bw.writeLine("        t++;");
			bw.writeLine("      }");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function splitBeforeAll() {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var value = $('token' + _start);");
			bw.writeLine("    if (value) {");
			bw.writeLine("      var t = 0;");
			bw.writeLine("      while($('token' + t)) {");
			bw.writeLine("        var token = $('token' + t);");
			bw.writeLine("        if ((token.innerHTML == value.innerHTML) && (getState(t) == 'C')) {");
			bw.writeLine("          setState(t, 'S');");
			bw.writeLine("        }");
			bw.writeLine("        t++;");
			bw.writeLine("      }");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function splitAfterAll() {");
			bw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			bw.writeLine("    var value = $('token' + _start);");
			bw.writeLine("    if (value) {");
			bw.writeLine("      var t = 0;");
			bw.writeLine("      while($('token' + t)) {");
			bw.writeLine("        var token = $('token' + t);");
			bw.writeLine("        if (token.innerHTML == value.innerHTML) {");
			bw.writeLine("          var nextToken = $('token' + (t+1));");
			bw.writeLine("          if (nextToken) {");
			bw.writeLine("            if (getState(t+1) == 'C')");
			bw.writeLine("              setState((t + 1), 'S');");
			bw.writeLine("          }");
			bw.writeLine("        }");
			bw.writeLine("        t++;");
			bw.writeLine("      }");
			bw.writeLine("      adjustColors();");
			bw.writeLine("    }");
			bw.writeLine("  }");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;");
			bw.writeLine("}");
			bw.writeLine("");
			
			
			bw.writeLine("function truncateSeparators() {");
			bw.writeLine("  var t = 0;");
			bw.writeLine("  while ($('token' + t)) {");
			bw.writeLine("    var token = $('token' + t);");
			bw.writeLine("    if (',;.:'.indexOf(token.innerHTML) != -1) {");
			bw.writeLine("      if (getState(t) == 'S') {");
			bw.writeLine("        var nextToken = $('token' + (t+1));");
			bw.writeLine("        if (nextToken && (getState(t+1) == 'C')) {");
			bw.writeLine("          setState((t + 1), 'S');");
			bw.writeLine("          setType((t + 1), getType(t));");
			bw.writeLine("        }");
			bw.writeLine("        setState(t, 'O');");
			bw.writeLine("        setType(t, null);");
			bw.writeLine("      }");
			bw.writeLine("      if (getState(t) == 'C') {");
			bw.writeLine("        var nextToken = $('token' + (t+1));");
			bw.writeLine("        if (nextToken && (getState(t+1) == 'O')) {");
			bw.writeLine("          setState(t, 'O');");
			bw.writeLine("          setType(t, null);");
			bw.writeLine("          t = (t-2);");
			bw.writeLine("        }");
			bw.writeLine("      }");
			bw.writeLine("    }");
			bw.writeLine("    t++");
			bw.writeLine("  }");
			bw.writeLine("  adjustColors();");
			bw.writeLine("  ");
			bw.writeLine("  closeContext();");
			bw.writeLine("  return false;  ");
			bw.writeLine("}");
			bw.writeLine("");
			
			
			bw.writeLine("function getState(index) {");
			bw.writeLine("  return _feedbackForm[('part' + _part + '_token' + index + '_state')].value;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function setState(index, state) {");
			bw.writeLine("  _feedbackForm[('part' + _part + '_token' + index + '_state')].value = state;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function getType(index) {");
			bw.writeLine("  return _feedbackForm[('part' + _part + '_token' + index + '_type')].value;");
			bw.writeLine("}");
			bw.writeLine("");
			
			bw.writeLine("function setType(index, type) {");
			bw.writeLine("  _feedbackForm[('part' + _part + '_token' + index + '_type')].value = ((type == null) ? '' : type);");
			bw.writeLine("}");
			
			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			bw.writeLine(".menu {");
			bw.writeLine("  margin: 0;");
			bw.writeLine("  padding: 0.3em;");
			bw.writeLine("  list-style-type: none;");
			bw.writeLine("  background-color: ddd;");
			bw.writeLine("}");
			
			bw.writeLine(".menu li:hover {}");
			
			bw.writeLine(".menu hr {");
			bw.writeLine("  border: 0;");
			bw.writeLine("  border-bottom: 1px solid grey;");
			bw.writeLine("  margin: 3px 0px 3px 0px;");
			bw.writeLine("  width: 10em;");
			bw.writeLine("}");
			
			bw.writeLine(".menu a {");
			bw.writeLine("  border: 0 !important;");
			bw.writeLine("  text-decoration: none;");
			bw.writeLine("}");
			
			bw.writeLine(".menu a:hover {");
			bw.writeLine("  text-decoration: underline !important;");
			bw.writeLine("}");
			
			bw.writeLine(".part {");
			bw.writeLine("  width: 70%;");
			bw.writeLine("  margin-top: 10px;");
			bw.writeLine("  border-width: 2px;");
			bw.writeLine("  border-color: 666666;");
			bw.writeLine("  border-style: solid;");
			bw.writeLine("  border-collapse: collapse;");
			bw.writeLine("}");
			
			bw.writeLine(".part td {");
			bw.writeLine("  padding: 10px;");
			bw.writeLine("  margin: 0px;");
			bw.writeLine("}");
			
			bw.writeLine(".menuItem {");
			bw.writeLine("  font-family: Arial,Helvetica,Verdana;");
			bw.writeLine("  font-size: 10pt;");
			bw.writeLine("  padding: 2pt 5pt 2pt;");
			bw.writeLine("  text-align: left;");
			bw.writeLine("}");
			
			bw.writeLine(".tokens {");
			bw.writeLine("  line-height: " + (this.fontSize + 5) + "pt;");
			bw.writeLine("}");

			bw.writeLine(".token {");
			bw.writeLine("  font-family: " + this.fontName + ";");
			bw.writeLine("  font-size: " + this.fontSize + "pt;");
			bw.writeLine("}");

			if (bw != out)
				bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			bw.writeLine("<table class=\"part\">");
			bw.writeLine("<tr>");
			
			for (int c = 0; c < 4; c++) {
				bw.writeLine("<td>");
				
				if (c == 0) {
					bw.writeLine("<span style=\"background-color: " + FeedbackPanel.getRGB(this.mcColor) + ";\">&nbsp;&nbsp;</span>&nbsp;");
					bw.writeLine(MATERIALS_CITATION_ANNOTATION_TYPE);
				}
//				else if (c == 3) {
//					bw.writeLine("Exclude Paragraph ");
//					bw.writeLine("<input name=\"exclude" + 0 + "\" type=\"checkbox\" value=\"E\"" + (this.exclude.isSelected() ? " checked" : "") + ">");
//				}
				bw.writeLine("</td>");
			}
			bw.writeLine("</tr>");
			bw.writeLine("</table>");
			
			bw.writeLine("<div id=\"divContext\" style=\"border: 1px solid blue; display: none; position: absolute\">");
			bw.writeLine("<ul class=\"menu\">");
			bw.writeLine("");
			bw.writeLine("<li id=\"showSelectedTokens\" class=\"menuItem\"><a href=\"#\" onclick=\"return showSelectedTokens();\">Show Selected Tokens</a></li>");
			bw.writeLine("");
			
			bw.writeLine("<li id=\"annotate" + 0 + "\" class=\"menuItem\"><a href=\"#\" onclick=\"return annotate('" + MATERIALS_CITATION_ANNOTATION_TYPE + "');\">Annotate as '" + MATERIALS_CITATION_ANNOTATION_TYPE + "'</a></li>");
			bw.writeLine("");
			bw.writeLine("<li id=\"truncateSeparators" + 0 + "\" class=\"menuItem\"><a href=\"#\" onclick=\"return truncateSeparators();\">Truncate Separator Chars</a></li>");
			bw.writeLine("");
			
			bw.writeLine("<li id=\"remove\" class=\"menuItem\"><a href=\"#\" onclick=\"return remove();\">Remove Annotation</a></li>");
			bw.writeLine("<li id=\"extend\" class=\"menuItem\"><a href=\"#\" onclick=\"return extend();\">Extend Annotation</a></li>");
			bw.writeLine("<li id=\"merge\" class=\"menuItem\"><a href=\"#\" onclick=\"return merge();\">Merge Annotations</a></li>");
			bw.writeLine("");
			
			bw.writeLine("<li id=\"splitAround\" class=\"menuItem\"><a id=\"splitAroundItem\" href=\"#\" onclick=\"return splitAround();\">Split Annotation Around</a></li>");
			bw.writeLine("<li id=\"splitBefore\" class=\"menuItem\"><a id=\"splitBeforeItem\" href=\"#\" onclick=\"return splitBefore();\">Split Annotation Before</a></li>");
			bw.writeLine("<li id=\"splitAfter\" class=\"menuItem\"><a id=\"splitAfterItem\" href=\"#\" onclick=\"return splitAfter();\">Split Annotation After</a></li>");
			bw.writeLine("");
			
			bw.writeLine("<li id=\"splitAroundAll\" class=\"menuItem\"><a id=\"splitAroundItemAll\" href=\"#\" onclick=\"return splitAroundAll();\">Split Annotations Around All</a></li>");
			bw.writeLine("<li id=\"splitBeforeAll\" class=\"menuItem\"><a id=\"splitBeforeItemAll\" href=\"#\" onclick=\"return splitBeforeAll();\">Split Annotations Before All</a></li>");
			bw.writeLine("<li id=\"splitAfterAll\" class=\"menuItem\"><a id=\"splitAfterItemAll\" href=\"#\" onclick=\"return splitAfterAll();\">Split Annotations After All</a></li>");
			bw.writeLine("");
			
			bw.writeLine("</ul>");
			bw.writeLine("</div>");
			bw.writeLine("");
			
			bw.writeLine("<table class=\"part\" id=\"part" + 0 + "\">");
			bw.writeLine("<tr>");
			bw.writeLine("<td>");
			bw.writeLine("<div class=\"tokens\">");
			
			for (int t = 0; t < this.annotationEditor.tokens.length; t++) {
				if (t != 0) {
					bw.write("<span" +
							" class=\"token\"" +
							" id=\"part" + 0 + "_space" + t + "\"" +
							" onmouseover=\"activateToken(" + 0 + ", " + (t-1) + ".5);\"" +
							" onmouseout=\"inactivateToken(" + 0 + ", " + (t-1) + ".5);\"" +
							">" +
							this.annotationEditor.tokens[t].space +
							"</span>");
				}
				bw.write("<span" +
						" class=\"token\"" +
						" id=\"part" + 0 + "_token" + t + "\"" +
						" onmouseover=\"activateToken(" + 0 + ", " + t + ");\"" +
						" onmouseout=\"inactivateToken(" + 0 + ", " + t + ");\"" +
						" onclick=\"clickToken(" + 0 + ", " + t + ");\"" +
						">" +
						FeedbackPanelHtmlRenderer.prepareForHtml(this.annotationEditor.tokens[t].value) +
						"</span>");
			}
			bw.newLine();
			
			bw.writeLine("</div>");
			bw.writeLine("</td>");
			bw.writeLine("</tr>");
			bw.writeLine("</table>");
			bw.writeLine("");
			
			for (int t = 0; t < this.annotationEditor.tokens.length; t++) {
				String state = this.annotationEditor.tokens[t].state;
				if (AnnotationEditorFeedbackPanel.START.equals(state))
					state = "S";
				else if (AnnotationEditorFeedbackPanel.CONTINUE.equals(state))
					state = "C";
				else if (AnnotationEditorFeedbackPanel.OTHER.equals(state)) {
					state = "O";
				}
				bw.writeLine("<input type=\"hidden\" name=\"part" + 0 + "_token" + t + "_state\" value=\"" + state + "\">");
				bw.writeLine("<input type=\"hidden\" name=\"part" + 0 + "_token" + t + "_type\" value=\"" + ("O".equals(state) ? "" : MATERIALS_CITATION_ANNOTATION_TYPE) + "\">");
			}
			
			bw.writeLine("");
			
			if (bw != out)
				bw.flush();
/*
<div id="divContext" style="border: 1px solid blue; display: none; position: absolute">
<ul class="menu">

<li id="showSelectedTokens" class="menuItem"><a href="#" onclick="return showSelectedTokens();">Show Selected Tokens</a></li>

<li id="annotate0" class="menuItem"><a href="#" onclick="return annotate('collectionCode');">Annotate as 'collectionCode'</a></li>
<li id="annotate1" class="menuItem"><a href="#" onclick="return annotate('specimenCode');">Annotate as 'specimenCode'</a></li>
<li id="annotate2" class="menuItem"><a href="#" onclick="return annotate('typeStatus');">Annotate as 'typeStatus'</a></li>
<li id="annotate3" class="menuItem"><a href="#" onclick="return annotate('collectingCountry');">Annotate as 'collectingCountry'</a></li>
<li id="annotate4" class="menuItem"><a href="#" onclick="return annotate('collectingRegion');">Annotate as 'collectingRegion'</a></li>
<li id="annotate5" class="menuItem"><a href="#" onclick="return annotate('location');">Annotate as 'location'</a></li>
<li id="annotate6" class="menuItem"><a href="#" onclick="return annotate('locationDeviation');">Annotate as 'locationDeviation'</a></li>
<li id="annotate7" class="menuItem"><a href="#" onclick="return annotate('collectorName');">Annotate as 'collectorName'</a></li>
<li id="annotate8" class="menuItem"><a href="#" onclick="return annotate('collectingDate');">Annotate as 'collectingDate'</a></li>
<li id="annotate9" class="menuItem"><a href="#" onclick="return annotate('collectingMethod');">Annotate as 'collectingMethod'</a></li>
<li id="annotate10" class="menuItem"><a href="#" onclick="return annotate('collectingDetail');">Annotate as 'collectingDetail'</a></li>
<li id="annotate11" class="menuItem"><a href="#" onclick="return annotate('geoCoordinate');">Annotate as 'geoCoordinate'</a></li>

<li id="remove" class="menuItem"><a href="#" onclick="return remove();">Remove Annotation</a></li>
<li id="extend" class="menuItem"><a href="#" onclick="return extend();">Extend Annotation</a></li>
<li id="merge" class="menuItem"><a href="#" onclick="return merge();">Merge Annotations</a></li>

<li id="splitAround" class="menuItem"><a id="splitAroundItem" href="#" onclick="return splitAround();">Split Annotation Around</a></li>
<li id="splitBefore" class="menuItem"><a id="splitBeforeItem" href="#" onclick="return splitBefore();">Split Annotation Before</a></li>
<li id="splitAfter" class="menuItem"><a id="splitAfterItem" href="#" onclick="return splitAfter();">Split Annotation After</a></li>

</ul>
</div>

<!--
<table class="part" id="part2">
<tr>
<td>

<span id="partP_token0" onmouseover="activateToken(P, 0);" onmouseout="inactivateToken(P, 0);" onclick="clickToken(P, 0);" class="token">xyz</span>
<span id="partP_space1" onmouseover="activateToken(P, 0.5);" onmouseout="inactivateToken(P, 0.5);" class="token"></span>
<span id="partP_token1" onmouseover="activateToken(P, 1);" onmouseout="inactivateToken(P, 1);" onclick="clickToken(P, 1);" class="token">xyz</span>
<span id="partP_space2" onmouseover="activateToken(P, 1.5);" onmouseout="inactivateToken(P, 1.5);" class="token"></span>
<span id="partP_token2" onmouseover="activateToken(P, 2);" onmouseout="inactivateToken(P, 2);" onclick="clickToken(P, 2);" class="token">xyz</span>
<span id="partP_space3" onmouseover="activateToken(P, 2.5);" onmouseout="inactivateToken(P, 2.5);" class="token"></span>
<span id="partP_token3" onmouseover="activateToken(P, 3);" onmouseout="inactivateToken(P, 3);" onclick="clickToken(P, 3);" class="token">xyz</span>
<span id="partP_space4" onmouseover="activateToken(P, 3.5);" onmouseout="inactivateToken(P, 3.5);" class="token"></span>
<span id="partP_token4" onmouseover="activateToken(P, 4);" onmouseout="inactivateToken(P, 4);" onclick="clickToken(P, 4);" class="token">xyz</span>
<span id="partP_space5" onmouseover="activateToken(P, 4.5);" onmouseout="inactivateToken(P, 4.5);" class="token"></span>
<span id="partP_token5" onmouseover="activateToken(P, 5);" onmouseout="inactivateToken(P, 5);" onclick="clickToken(P, 5);" class="token">xyz</span>
<span id="partP_space6" onmouseover="activateToken(P, 5.5);" onmouseout="inactivateToken(P, 5.5);" class="token"></span>
<span id="partP_token6" onmouseover="activateToken(P, 6);" onmouseout="inactivateToken(P, 6);" onclick="clickToken(P, 6);" class="token">xyz</span>
<span id="partP_space7" onmouseover="activateToken(P, 6.5);" onmouseout="inactivateToken(P, 6.5);" class="token"></span>
<span id="partP_token7" onmouseover="activateToken(P, 7);" onmouseout="inactivateToken(P, 7);" onclick="clickToken(P, 7);" class="token">xyz</span>
<span id="partP_space8" onmouseover="activateToken(P, 7.5);" onmouseout="inactivateToken(P, 7.5);" class="token"></span>
<span id="partP_token8" onmouseover="activateToken(P, 8);" onmouseout="inactivateToken(P, 8);" onclick="clickToken(P, 8);" class="token">xyz</span>
<span id="partP_space9" onmouseover="activateToken(P, 8.5);" onmouseout="inactivateToken(P, 8.5);" class="token"></span>
<span id="partP_token9" onmouseover="activateToken(P, 9);" onmouseout="inactivateToken(P, 9);" onclick="clickToken(P, 9);" class="token">xyz</span>
<span id="partP_space10" onmouseover="activateToken(P, 9.5);" onmouseout="inactivateToken(P, 9.5);" class="token"></span>

</td>
</tr>
</table>

<input type="hidden" name="partP_token0_state" value="O">
<input type="hidden" name="partP_token0_type" value="">
<input type="hidden" name="partP_token1_state" value="O">
<input type="hidden" name="partP_token1_type" value="">
<input type="hidden" name="partP_token2_state" value="O">
<input type="hidden" name="partP_token2_type" value="">
<input type="hidden" name="partP_token3_state" value="O">
<input type="hidden" name="partP_token3_type" value="">
<input type="hidden" name="partP_token4_state" value="O">
<input type="hidden" name="partP_token4_type" value="">
<input type="hidden" name="partP_token5_state" value="O">
<input type="hidden" name="partP_token5_type" value="">
<input type="hidden" name="partP_token6_state" value="O">
<input type="hidden" name="partP_token6_type" value="">
<input type="hidden" name="partP_token7_state" value="O">
<input type="hidden" name="partP_token7_type" value="">
<input type="hidden" name="partP_token8_state" value="O">
<input type="hidden" name="partP_token8_type" value="">
<input type="hidden" name="partP_token9_state" value="OcollectorName">
<input type="hidden" name="partP_token9_type" value="">
-->	
*/
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			for (int t = 0; t < this.annotationEditor.tokens.length; t++) {
				
				String state = response.getProperty(("part" + 0 + "_token" + t + "_state"), "O");
				
				if ("S".equals(state))
					state = AnnotationEditorFeedbackPanel.START;
				else if ("C".equals(state))
					state = AnnotationEditorFeedbackPanel.CONTINUE;
				else if ("O".equals(state))
					state = AnnotationEditorFeedbackPanel.OTHER;
				
				this.annotationEditor.tokens[t].state = state;
			}
			this.exclude.setSelected("E".equals(response.getProperty(("exclude" + 0), "K")));
		}
	}
//	
//	public static void main(String[] args) throws Exception {
//		FeedbackPanel fp = new MaterialsCitationFeedbackPanel();
//		String[] rawDataLines = {
//				"25",
//				"true",
//				"false",
//				"Please+Check+Materials+Citations+for+Monomorium+dentatum",
//				"%3CHTML%3EPlease+make+sure+that+all+%3CB%3Ematerials+citations%3C%2FB%3E+in+this+paragraph+are+annotated+individually.%3C%2FHTML%3E",
//				"0",
//				"+",
//				"",
//				"RequesterClassName MaterialsCitationTaggerOnline",
//				"",
//				"Verdana",
//				"12",
//				"FFFF00",
//				"K",
//				"Holotype O",
//				"+. O",
//				"+Worker O",
//				"+%2C O",
//				"+Egypt S",
//				"+%2C C",
//				"+Damietta C",
//				"+%2C C",
//				"+20 C",
//				"+. C",
//				"+viii C",
//				"+. C",
//				"+2003 C",
//				"+%2C C",
//				"+31 C",
//				"+%C2%B0 C",
//				"+26 C",
//				"+%27 C",
//				"+N C",
//				"+%2C C",
//				"+31 C",
//				"+%C2%B0 C",
//				"+48 C",
//				"+%27 C",
//				"+E C",
//				"+%2C C",
//				"+leg C",
//				"+. C",
//				"+M C",
//				"+. C",
//				"+R C",
//				"+. C",
//				"+Sharaf C",
//				"+. C",
//				"+- C",
//				"+Paratypes C",
//				"+%3A C",
//				"+17 C",
//				"+workers C",
//				"+%2C C",
//				"+same C",
//				"+series C",
//				"+as C",
//				"+holotype C",
//				"+%2C C",
//				"+13 C",
//				"+workers C",
//				"+%2C C",
//				"+Abu-Swelem C",
//				"+%2C C",
//				"+El-Minyia C",
//				"+%2C C",
//				"+29 C",
//				"+. C",
//				"+vi C",
//				"+. C",
//				"+2003 C",
//				"+%2C C",
//				"+28 C",
//				"+%C2%B0 C",
//				"+06 C",
//				"+%27 C",
//				"+N C",
//				"+%2C C",
//				"+30 C",
//				"+%C2%B0 C",
//				"+45 C",
//				"+%27 C",
//				"+E C",
//				"+%2C C",
//				"+leg C",
//				"+. C",
//				"+M C",
//				"+. C",
//				"+R C",
//				"+. C",
//				"+Sharaf C",
//				"+%3B O",
//				"+3 S",
//				"+workers C",
//				"+%2C C",
//				"+Abuzabal C",
//				"+%2C C",
//				"+Qalyubiya C",
//				"+%2C C",
//				"+21 C",
//				"+. C",
//				"+vi C",
//				"+. C",
//				"+2003 C",
//				"+%2C C",
//				"+30 C",
//				"+%C2%B0 C",
//				"+03 C",
//				"+%27 C",
//				"+N C",
//				"+%2C C",
//				"+31 C",
//				"+%C2%B0 C",
//				"+15 C",
//				"+%27 C",
//				"+E C",
//				"+%2C C",
//				"+leg C",
//				"+. C",
//				"+M C",
//				"+. C",
//				"+R C",
//				"+. C",
//				"+Sharaf C",
//				"+%3B O",
//				"+1 S",
//				"+Worker C",
//				"+%2C C",
//				"+Port C",
//				"+Said C",
//				"+%2C C",
//				"+26 C",
//				"+. C",
//				"+viii C",
//				"+. C",
//				"+2003 C",
//				"+%2C C",
//				"+31 C",
//				"+%C2%B0 C",
//				"+16 C",
//				"+%27 C",
//				"+N C",
//				"+%2C C",
//				"+32 C",
//				"+%C2%B0 C",
//				"+18 C",
//				"+%27 C",
//				"+E C",
//				"+%2C C",
//				"+leg C",
//				"+. C",
//				"+M C",
//				"+. C",
//				"+R C",
//				"+. C",
//				"+Sharaf C",
//				"+. O",
//				"",
//			};
//		StringVector dataLines = new StringVector();
//		dataLines.addContent(rawDataLines);
//		fp.initFields(new BufferedReader(new StringReader(dataLines.concatStrings("\n") + "\n")));
//		fp.getFeedback();
//	}
}