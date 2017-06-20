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
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
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
package de.uka.ipd.idaho.plugins.bibRefs.taggers;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;

import javax.swing.JDialog;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.StandaloneAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameAttributeSet;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.RegExConfigPanel;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CategorizationFeedbackPanel;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * @author sautter
 */
public class BibRefCitationTaggerOnline extends AbstractConfigurableAnalyzer implements BibRefConstants {
	
	private static final String YEAR_SUFFIX_ATTRIBUTE = "yearSuffix";

	private static final boolean DEBUG = false;
	
	private static final String BIB_REF_CITATION_TYPE = (BIBLIOGRAPHIC_REFERENCE_TYPE + CITATION_TYPE_SUFFIX);
	
	private static final String NOT_A_CITATION_CATEGORY = "<Not a citation>";
	
	private static final String BIB_REF_CITATION_PATTERN_NAME_FILE = "useBibRefCitationPatterns.cnfg";
	private StringVector activeBibRefCitationPatternNames = new StringVector();
	
	private static final String REGEX_NAME_EXTENSION = ".regEx.cr.txt"; 
	private String[] bibRefCitationPatterns;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get bibliographic references
		QueriableAnnotation[] bibRefs = data.getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		
		//	nothing to reference to
		if (bibRefs.length == 0)
			return;
		
		//	index numbered bibliographic references & collect author name tokens
		HashMap bibRefsByNumber = new HashMap();
		StringVector authorNameTokens = new StringVector();
		StringVector authorYearPhrases = new StringVector();
		HashMap authorsByYear = new HashMap();
		Annotation[] carryAuthors = new Annotation[0];
		for (int r = 0; r < bibRefs.length; r++) {
			
			//	index bibliographic reference by number
			String number = bibRefs[r].firstValue();
			if (Gamta.isOpeningBracket(number) && (bibRefs[r].size() > 1))
				number = bibRefs[r].valueAt(1);
			while (number.startsWith("(") || number.startsWith("[") || number.startsWith("<"))
				number = number.substring(1);
			while (number.endsWith(".") || number.endsWith(",") || number.endsWith(":") || number.endsWith(";") || number.endsWith(")") || number.endsWith("]") || number.endsWith(">"))
				number = number.substring(0, (number.length() - 1));
			if (number.matches("[0-9]++"))
				bibRefsByNumber.put(new Integer(number), bibRefs[r]);
			
			//	get year, plus potential suffix
			String year = this.getYear(bibRefs[r]);
			if (year == null)
				continue;
			String yearSuffix = null;
			Annotation[] years = bibRefs[r].getAnnotations(YEAR_ANNOTATION_TYPE);
			if ((years.length != 0) && (years[0].getEndIndex() < bibRefs[r].size()) && bibRefs[r].valueAt(years[0].getEndIndex()).matches("[a-z]"))
				yearSuffix = bibRefs[r].valueAt(years[0].getEndIndex());
			
			//	get and carry authors
			Annotation[] authors = bibRefs[r].getAnnotations(AUTHOR_ANNOTATION_TYPE);
			if (authors.length == 0)
				authors = carryAuthors;
			else carryAuthors = authors;
			
			//	collect author tokens and create author+year and year phrases
			for (int a = 0; a < authors.length; a++) {
				authorNameTokens.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(authors[a]));
				for (int t = 0; t < authors[a].size(); t++) {
					String authorName = authors[a].valueAt(t);
					if (authorName.matches("((l|d|O)\\')?[A-Z][A-Za-z\\-]++")) {
						authorYearPhrases.addElementIgnoreDuplicates(authorName + " " + year);
						authorYearPhrases.addElementIgnoreDuplicates(authorName + ", " + year);
						if (yearSuffix != null) {
							authorYearPhrases.addElementIgnoreDuplicates(authorName + " " + year + "" + yearSuffix);
							authorYearPhrases.addElementIgnoreDuplicates(authorName + " " + year + " " + yearSuffix);
							authorYearPhrases.addElementIgnoreDuplicates(authorName + ", " + year + "" + yearSuffix);
							authorYearPhrases.addElementIgnoreDuplicates(authorName + ", " + year + " " + yearSuffix);
						}
						if (authors.length > 1) {
							authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al. " + year);
							authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al, " + year);
							authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al., " + year);
							if (yearSuffix != null) {
								authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al. " + year + " " + yearSuffix);
								authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al, " + year + " " + yearSuffix);
								authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al., " + year + " " + yearSuffix);
							}
						}
						
						//	index authors by years (including year suffixes)
						HashSet yearAuthors = ((HashSet) authorsByYear.get(year));
						if (yearAuthors == null) {
							yearAuthors = new HashSet();
							authorsByYear.put(year, yearAuthors);
						}
						yearAuthors.add(authorName);
						if (yearSuffix != null) {
							yearAuthors = ((HashSet) authorsByYear.get(year + " " + yearSuffix));
							if (yearAuthors == null) {
								yearAuthors = new HashSet();
								authorsByYear.put(year + " " + yearSuffix, yearAuthors);
							}
							yearAuthors.add(authorName);
						}
					}
				}
			}
			
			//	create author+year phrases from author attribute
			String author = ((String) bibRefs[r].getAttribute(AUTHOR_ANNOTATION_TYPE));
			if (author == null)
				continue;
			TokenSequence authorTokens = data.getTokenizer().tokenize(author);
			for (int t = 0; t < authorTokens.size(); t++) {
				String authorName = authorTokens.valueAt(t);
				if (authorName.matches("((l|d|O)\\')?[A-Z][A-Za-z\\-]++")) {
					authorYearPhrases.addElementIgnoreDuplicates(authorName + " " + year);
					if (authors.length > 1)
						authorYearPhrases.addElementIgnoreDuplicates(authorName + " et al. " + year);
					
					//	index authors by years
					HashSet yearAuthors = ((HashSet) authorsByYear.get(year));
					if (yearAuthors == null) {
						yearAuthors = new HashSet();
						authorsByYear.put(year, yearAuthors);
					}
					yearAuthors.add(authorName);
				}
			}
		}
		
		//	find start of references block
		int bibRefStart = bibRefs[0].getStartIndex();
		
		//	first bibliographic reference is standalone, skip over it (articles may have their own bibliographic reference at the top)
		if ((bibRefs.length >= 2) && (bibRefs[0].getEndIndex() < bibRefs[1].getStartIndex()))
			bibRefStart = bibRefs[1].getStartIndex();
		
		//	check citation style (author+year or numbered)
		boolean bibRefCitationsNumeric = (bibRefs.length < (bibRefsByNumber.size() * 2));
		
		//	tag bibliographic reference citations
		Annotation[] bibRefCitations;
		if (bibRefCitationsNumeric)
			bibRefCitations = this.getBibRefCitationsNumbers(data, bibRefStart, new TreeSet(bibRefsByNumber.keySet()));
		else bibRefCitations = this.getBibRefCitationsAuthorYear(data, bibRefStart, authorNameTokens, authorYearPhrases, authorsByYear);
		
		//	collect to-resolve citations both from document and from newly tagged ones
		ArrayList bibRefCitationsToResolve = new ArrayList();
		
		//	get and index bibliographic reference citations already annotated in document
		Annotation[] docBibRefCitations = data.getAnnotations(BIB_REF_CITATION_TYPE);
		HashMap docBibRefCitationsByTokenIndex = new HashMap();
		for (int c = 0; c < docBibRefCitations.length; c++) {
			if (DEBUG) System.out.println("Got existing RC " + (c+1) + " at " + docBibRefCitations[c].getStartIndex() + " - " + docBibRefCitations[c].toXML());
			for (int t = docBibRefCitations[c].getStartIndex(); t < docBibRefCitations[c].getEndIndex(); t++)
				docBibRefCitationsByTokenIndex.put(new Integer(t), docBibRefCitations[c]);
			if (!this.isResolved(docBibRefCitations[c]))
				bibRefCitationsToResolve.add(docBibRefCitations[c]);
		}
		
		//	resolve newly tagged citations against existing ones, and collect to-resolve citations
		for (int c = 0; c < bibRefCitations.length; c++) {
			if (DEBUG) System.out.println("Doc-resolving RC " + (c+1) + " at " + bibRefCitations[c].getStartIndex() + " - " + bibRefCitations[c].toXML());
			
			//	find nested existing citation
			Annotation docBibRefCitation = null;
			for (int t = bibRefCitations[c].getStartIndex(); t < bibRefCitations[c].getEndIndex(); t++) {
				docBibRefCitation = ((Annotation) docBibRefCitationsByTokenIndex.get(new Integer(t)));
				if (this.isResolved(docBibRefCitation))
					break;
			}
			
			//	no existing (resolved) citation found
			if (docBibRefCitation == null) {
				bibRefCitationsToResolve.add(bibRefCitations[c]);
				if (DEBUG) System.out.println(" ==> no existing RC found");
				continue;
			}
			
			//	add annotation to document, transfer attributes, and we're done with this one
			Annotation bibRefCitation = data.addAnnotation(bibRefCitations[c]);
			String[] bibRefAttributeNames = docBibRefCitation.getAttributeNames();
			for (int a = 0; a < bibRefAttributeNames.length; a++) {
				if (PAGE_ID_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (LAST_PAGE_ID_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (PAGE_NUMBER_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (LAST_PAGE_NUMBER_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (!bibRefAttributeNames[a].startsWith("_"))
					bibRefCitation.setAttribute(bibRefAttributeNames[a], docBibRefCitation.getAttribute(bibRefAttributeNames[a]));
			}
			if (DEBUG) System.out.println(" ==> resolved against " + docBibRefCitation.toXML());
		}
		
		//	ignore year-only citations in assessment (too many false positives in that department)
		int bibRefCitationsToResolveCount = 0;
		for (int c = 0; c < bibRefCitationsToResolve.size(); c++) {
			Annotation brc = ((Annotation) bibRefCitationsToResolve.get(c));
			if ((brc.size() > 1) || !brc.firstValue().matches("[12][0-9]{3}"))
				bibRefCitationsToResolveCount++;
		}
		
		//	nothing new to reference or resolve
		if (bibRefCitationsToResolveCount == 0) {
			
			//	no existing citations either, we're done
			if (docBibRefCitations.length == 0)
				return;
			
			//	this looks like a revisit, add what's there
			bibRefCitationsToResolve.addAll(Arrays.asList(docBibRefCitations));
		}
		
		//	sort out duplicates
		Collections.sort(bibRefCitationsToResolve, new Comparator() {
			public int compare(Object o1, Object o2) {
				return AnnotationUtils.compare(((Annotation) o1), ((Annotation) o2));
			}
		});
		for (int c = 1; c < bibRefCitationsToResolve.size(); c++) {
			Annotation brc1 = ((Annotation) bibRefCitationsToResolve.get(c-1));
			Annotation brc2 = ((Annotation) bibRefCitationsToResolve.get(c));
			if (AnnotationUtils.compare(brc1, brc2) == 0) {
				if (brc1 instanceof StandaloneAnnotation)
					bibRefCitationsToResolve.remove(--c);
				else bibRefCitationsToResolve.remove(c--);
			}
		}
		
		//	line up what we have to work on
		bibRefCitations = ((Annotation[]) bibRefCitationsToResolve.toArray(new Annotation[bibRefCitationsToResolve.size()]));
		if (bibRefCitations.length == 0)
			return;
		
		//	map citations to bibliographic references
		QueriableAnnotation[] citedBibRefs = this.getCitedBibRefs(data, bibRefs, bibRefsByNumber, bibRefCitations, parameters.containsKey(INTERACTIVE_PARAMETER));
		
		//	if style is author+year, clean up standalone years that are not part of enumerations
		if (!bibRefCitationsNumeric) {
			int lastBrcEnd = 0;
			for (int c = 0; c < bibRefCitations.length; c++) {
				if (citedBibRefs[c] == null)
					continue;
				
				//	check if there is an explicit author name (4 tokens could be year and suffix range, but above that is safe)
				boolean explicitAuthor = (bibRefCitations[c].size() > 4);
				if (!explicitAuthor)
					for (int t = 0; t < bibRefCitations[c].size(); t++) {
						String token = bibRefCitations[c].valueAt(t);
						if ((token.length() >= 2) && Gamta.isWord(token)) {
							explicitAuthor = true;
							break;
						}
					}
				
				//	this one has an explicit author, should be OK
				if (explicitAuthor)
					lastBrcEnd = Math.max(lastBrcEnd, bibRefCitations[c].getEndIndex());
				
				//	this one is close enough to be part of the enumeration
				else if (bibRefCitations[c].getStartIndex() <= (lastBrcEnd + 2))
					lastBrcEnd = Math.max(lastBrcEnd, bibRefCitations[c].getEndIndex());
				
				//	this one's too insecure
				else citedBibRefs[c] = null;
			}
		}
		
		//	get feedback if allowed to
		int cutoffIndex;
		if (parameters.containsKey(INTERACTIVE_PARAMETER))
			cutoffIndex = this.checkBibRefCitations(data, bibRefs, bibRefCitations, citedBibRefs);
		else cutoffIndex = bibRefCitations.length;
		
		//	transfer detail attributes from cited bibliographic references
		for (int c = 0; c < cutoffIndex; c++) {
			if (citedBibRefs[c] == null)
				continue;
			Annotation bibRefCitation = data.addAnnotation(bibRefCitations[c]);
			String[] bibRefAttributeNames = citedBibRefs[c].getAttributeNames();
			for (int a = 0; a < bibRefAttributeNames.length; a++) {
				if (PAGE_ID_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (LAST_PAGE_ID_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (PAGE_NUMBER_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (LAST_PAGE_NUMBER_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (!bibRefAttributeNames[a].startsWith("_"))
					bibRefCitation.setAttribute(bibRefAttributeNames[a], citedBibRefs[c].getAttribute(bibRefAttributeNames[a]));
			}
			bibRefCitation.setAttribute("refString", TokenSequenceUtils.concatTokens(citedBibRefs[c], true, true));
		}
		
		//	transfer attributes & clean up brackets
		for (int c = 0; c < cutoffIndex; c++) {
			if (citedBibRefs[c] == null)
				continue;
			
			int crs = bibRefCitations[c].getStartIndex();
			int cre = bibRefCitations[c].getEndIndex();
			if ((cre < data.size()) && Gamta.isClosingBracket(data.valueAt(cre))) {
				String closing = data.valueAt(cre);
				for (int t = (bibRefCitations[c].size()-1); t >= 0; t--) {
					if (Gamta.isClosingBracket(bibRefCitations[c].valueAt(t)))
						break;
					else if (Gamta.opens(bibRefCitations[c].valueAt(t), closing)) {
						cre += 1;
						break;
					}
				}
			}
			Annotation bibRefCitation = data.addAnnotation(BIB_REF_CITATION_TYPE, crs, (cre-crs));
			String[] bibRefAttributeNames = citedBibRefs[c].getAttributeNames();
			for (int a = 0; a < bibRefAttributeNames.length; a++) {
				if (PAGE_ID_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (LAST_PAGE_ID_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (PAGE_NUMBER_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (LAST_PAGE_NUMBER_ATTRIBUTE.equals(bibRefAttributeNames[a])) {}
				else if (!bibRefAttributeNames[a].startsWith("_"))
					bibRefCitation.setAttribute(bibRefAttributeNames[a], citedBibRefs[c].getAttribute(bibRefAttributeNames[a]));
			}
			bibRefCitation.setAttribute("refString", TokenSequenceUtils.concatTokens(citedBibRefs[c], true, true));
		}
		
		//	clean up
		AnnotationFilter.removeDuplicates(data, BIB_REF_CITATION_TYPE);
		AnnotationFilter.removeInner(data, BIB_REF_CITATION_TYPE);
	}
	
	private String getAuthor(QueriableAnnotation bibRef) {
		String author = ((String) bibRef.getAttribute(AUTHOR_ANNOTATION_TYPE));
		if (author != null)
			return author;
		
		Annotation[] authorAnnots = bibRef.getAnnotations(AUTHOR_ANNOTATION_TYPE);
		if (authorAnnots.length == 0)
			return null;
		
		StringBuffer authorBuilder = new StringBuffer(TokenSequenceUtils.concatTokens(authorAnnots[0], true, true));
		for (int a = 1; a < authorAnnots.length; a++) {
			authorBuilder.append(" & ");
			authorBuilder.append(TokenSequenceUtils.concatTokens(authorAnnots[0], true, true));
		}
		return authorBuilder.toString();
	}
	
	private String getYear(QueriableAnnotation bibRef) {
		String year = ((String) bibRef.getAttribute(YEAR_ANNOTATION_TYPE));
		if (year != null)
			return year;
		Annotation[] yearAnnots = bibRef.getAnnotations(YEAR_ANNOTATION_TYPE);
		if (yearAnnots.length == 0)
			return null;
		return yearAnnots[0].firstValue();
	}
	
	private QueriableAnnotation[] getCitedBibRefs(MutableAnnotation data, QueriableAnnotation[] bibRefs, Map bibRefsByNumber, Annotation[] bibRefCitations, boolean allowUseFirst) {
		QueriableAnnotation[] citedBibRefs = new QueriableAnnotation[bibRefCitations.length];
		for (int c = 0; c < bibRefCitations.length; c++) {
			if (DEBUG) System.out.println("Doing RC " + (c+1) + " at " + bibRefCitations[c].getStartIndex() + " - " + bibRefCitations[c].toXML());
			
			//	try number lookup
			if (bibRefs.length < (bibRefsByNumber.size() * 2)) {
				try {
					Integer number = new Integer(bibRefCitations[c].getValue());
					citedBibRefs[c] = ((QueriableAnnotation) bibRefsByNumber.get(number));
				}
				catch (NumberFormatException nfe) {
					nfe.printStackTrace(System.out);
				}
			}
			
			//	number lookup successful
			if (citedBibRefs[c] != null)
				continue;
			
			//	filter by year
			ArrayList yearBibRefs = new ArrayList();
			String citedYear = ((String) bibRefCitations[c].getAttribute(YEAR_ANNOTATION_TYPE));
			if ((citedYear == null) && (bibRefCitations[c].firstValue().matches("[0-9]++"))) {
				citedYear = bibRefCitations[c].firstValue();
				bibRefCitations[c].setAttribute(YEAR_ANNOTATION_TYPE, citedYear);
			}
			if (citedYear != null)
				for (int r = 0; r < bibRefs.length; r++) {
					if (citedYear.equals(this.getYear(bibRefs[r])))
						yearBibRefs.add(bibRefs[r]);
				}
			if (DEBUG) System.out.println("  - found " + yearBibRefs.size() + " bibliographic references matching year " + citedYear);
			
			//	filter by author (if given)
			String citedAuthor = ((String) bibRefCitations[c].getAttribute(AUTHOR_ANNOTATION_TYPE));
			if (citedAuthor != null) {
				for (int yr = 0; yr < yearBibRefs.size(); yr++) {
					QueriableAnnotation bibRef = ((QueriableAnnotation) yearBibRefs.get(yr));
					if (DEBUG) System.out.println("    - testing against author " + citedAuthor + ": " + bibRef.toXML());
					
					//	direct match, we're done
					if (citedAuthor.equalsIgnoreCase(this.getAuthor(bibRef)))
						continue;
					
					//	try token wise matching
					Annotation[] authors = bibRef.getAnnotations(AUTHOR_ANNOTATION_TYPE);
					boolean remove = true;
					
					//	no authors annotated, might have been inferred from bibliographic references further up the road ==> use attribute value tokens
					if (authors.length == 0) {
						TokenSequence authorTokens = data.getTokenizer().tokenize((String) bibRef.getAttribute(AUTHOR_ANNOTATION_TYPE, ""));
						for (int t = 0; t < authorTokens.size(); t++)
							if (citedAuthor.equalsIgnoreCase(authorTokens.valueAt(t))) {
								remove = false;
								if (DEBUG) System.out.println("    ==> token match");
								break;
							}
					}
					
					//	authors annotated, test them all
					else for (int a = 0; remove && (a < authors.length); a++) {
						if (DEBUG) System.out.println("      - testing annotated author: " + authors[a].toXML());
						TokenSequence authorTokens;
						if (authors[a].hasAttribute(REPEATED_AUTHORS_ATTRIBUTE))
							authorTokens = data.getTokenizer().tokenize((String) authors[a].getAttribute(REPEATED_AUTHORS_ATTRIBUTE));
						else authorTokens = authors[a];
						for (int t = 0; t < authorTokens.size(); t++) {
							String authorToken = authorTokens.valueAt(t);
							if (citedAuthor.equalsIgnoreCase(authorToken)) {
								remove = false;
								if (DEBUG) System.out.println("      ==> match");
								break;
							}
							else if ((a == 0)  && StringUtils.isAbbreviationOf(authorToken, citedAuthor, false)) {
								remove = false;
								if (DEBUG) System.out.println("      ==> abbreviation match against '" + authorToken + "'");
								break;
							}
						}
					}
					
					//	do we have an author match?
					if (remove) {
						yearBibRefs.remove(yr--);
						if (DEBUG) System.out.println("    ==> removed");
					}
					else if (DEBUG) System.out.println("    ==> retained");
				}
				if (DEBUG) System.out.println("  - " + yearBibRefs.size() + " remain after filtering for author " + citedAuthor);
			}
			
			//	do we have an unambiguous match?
			if (yearBibRefs.size() == 1) {
				citedBibRefs[c] = ((QueriableAnnotation) yearBibRefs.get(0));
				if (DEBUG) System.out.println("  - got unambiguous cited bibliographic reference: " + citedBibRefs[c].toXML());
				continue;
			}
			
			//	try year suffix filter
			String yearSuffix = ((String) bibRefCitations[c].getAttribute(YEAR_SUFFIX_ATTRIBUTE));
			if (yearSuffix != null) {
				ArrayList yearSuffixBibRefs = new ArrayList();
				for (int yr = 0; yr < yearBibRefs.size(); yr++) {
					QueriableAnnotation bibRef = ((QueriableAnnotation) yearBibRefs.get(yr));
					Annotation[] years = bibRef.getAnnotations(YEAR_ANNOTATION_TYPE);
					if ((years.length != 0) && (years[0].getEndIndex() < bibRef.size()) && yearSuffix.equalsIgnoreCase(bibRef.valueAt(years[0].getEndIndex()))) {
						yearSuffixBibRefs.add(bibRef);
						if (DEBUG) System.out.println("  - got match for year suffix '" + yearSuffix + "': " + citedBibRefs[c].toXML());
					}
				}
				if (yearSuffixBibRefs.size() != 0)
					yearBibRefs = yearSuffixBibRefs;
			}
			
			//	do we have an unambiguous match now?
			if (yearBibRefs.size() == 1) {
				citedBibRefs[c] = ((QueriableAnnotation) yearBibRefs.get(0));
				if (DEBUG) System.out.println("  - got match for year suffix '" + yearSuffix + "': " + citedBibRefs[c].toXML());
				continue;
			}
			
			//	try and score by all authors
			CountingSet citedAuthorTokens = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
			for (int t = 0; t < bibRefCitations[c].size(); t++) {
				String token = bibRefCitations[c].valueAt(t);
				if (token.equals(citedYear))
					break;
				if (Gamta.isWord(token))
					citedAuthorTokens.add(token);
			}
			ArrayList yearBibRefScores = new ArrayList();
			int bestYearBibRefScore = 0;
			for (int yr = 0; yr < yearBibRefs.size(); yr++) {
				QueriableAnnotation bibRef = ((QueriableAnnotation) yearBibRefs.get(yr));
				int bibRefScore = 0;
				Annotation[] authors = bibRef.getAnnotations(AUTHOR_ANNOTATION_TYPE);
				
				//	no authors annotated, might have been inferred from bibliographic references further up the road ==> use attribute value tokens
				if (authors.length == 0) {
					TokenSequence authorTokens = data.getTokenizer().tokenize((String) bibRef.getAttribute(AUTHOR_ANNOTATION_TYPE, ""));
					for (int t = 0; t < authorTokens.size(); t++) {
						String authorToken = authorTokens.valueAt(t);
						bibRefScore += (citedAuthorTokens.getCount(authorToken) * authorToken.length());
					}
				}
				
				//	score by annotated authors
				else for (int a = 0; a < authors.length; a++) {
					TokenSequence authorTokens;
					if (authors[a].hasAttribute(REPEATED_AUTHORS_ATTRIBUTE))
						authorTokens = data.getTokenizer().tokenize((String) authors[a].getAttribute(REPEATED_AUTHORS_ATTRIBUTE));
					else authorTokens = authors[a];
					for (int t = 0; t < authorTokens.size(); t++) {
						String authorToken = authorTokens.valueAt(t);
						bibRefScore += (citedAuthorTokens.getCount(authorToken) * authorToken.length());
					}
				}
				
				//	record scores
				yearBibRefScores.add(new Integer(bibRefScore));
				bestYearBibRefScore = Math.max(bestYearBibRefScore, bibRefScore);
			}
			
			//	sort out lower scoring references
			for (int yr = 0; yr < yearBibRefs.size(); yr++)
				if (((Integer) yearBibRefScores.get(yr)).intValue() < bestYearBibRefScore) {
					yearBibRefScores.remove(yr);
					yearBibRefs.remove(yr--);
				}
			
			//	do we finally have an unambiguous match?
			if (yearBibRefs.size() == 1) {
				citedBibRefs[c] = ((QueriableAnnotation) yearBibRefs.get(0));
				if (DEBUG) System.out.println("  - got match for full author matchup " + (new ArrayList(citedAuthorTokens).toString()) + ": " + citedBibRefs[c].toXML());
				continue;
			}
			
			//	nothing found, fall back on first bibliographic reference
			if (citedBibRefs[c] == null) {
				if (allowUseFirst && (yearBibRefs.size() != 0)) {
					citedBibRefs[c] = ((QueriableAnnotation) yearBibRefs.get(0));
					if (DEBUG) System.out.println("  - no match for year suffix '" + yearSuffix + "', using first bibliographic reference: " + citedBibRefs[c].toXML());
				}
				else citedBibRefs[c] = null;
			}
//			
//			//	check year suffixes in case of doubt
//			if (yearBibRefs.size() != 0) {
//				
//				//	no year suffix given, use first bibliographic reference
//				if (yearSuffix == null) {
//					if (allowUseFirst) {
//						citedBibRefs[c] = ((QueriableAnnotation) yearBibRefs.get(0));
//						if (DEBUG) System.out.println("  - no year suffix given, using first bibliographic reference: " + citedBibRefs[c].toXML());
//					}
//					else citedBibRefs[c] = null;
//				}
//				
//				//	test for year suffix
//				else for (int yc = 0; yc < yearBibRefs.size(); yc++) {
//					QueriableAnnotation bibRef = ((QueriableAnnotation) yearBibRefs.get(yc));
//					Annotation[] years = bibRef.getAnnotations(YEAR_ANNOTATION_TYPE);
//					if ((years.length != 0) && (years[0].getEndIndex() < bibRef.size()) && yearSuffix.equalsIgnoreCase(bibRef.valueAt(years[0].getEndIndex()))) {
//						citedBibRefs[c] = bibRef;
//						if (DEBUG) System.out.println("  - got match for year suffix '" + yearSuffix + "': " + citedBibRefs[c].toXML());
//					}
//				}
//				
//				//	nothing found, fall back on first bibliographic reference
//				if (citedBibRefs[c] == null) {
//					if (allowUseFirst) {
//						citedBibRefs[c] = ((QueriableAnnotation) yearBibRefs.get(0));
//						if (DEBUG) System.out.println("  - no match for year suffix '" + yearSuffix + "', using first bibliographic reference: " + citedBibRefs[c].toXML());
//					}
//					else citedBibRefs[c] = null;
//				}
//			}
		}
		
		//	finally ...
		return citedBibRefs;
	}
	
	private int checkBibRefCitations(MutableAnnotation data, QueriableAnnotation[] bibRefs, Annotation[] bibRefCitations, Annotation[] citedBibRefs) {
		
		//	prepare feedback
		Properties bibRefStringsByID = new Properties();
		HashMap bibRefsByCitationStrings = new HashMap();
		String[] bibRefStrings = new String[bibRefs.length + 1];
		bibRefStrings[0] = NOT_A_CITATION_CATEGORY;
		for (int r = 0; r < bibRefs.length; r++) {
			String bibRefString = this.buildBibRefString(bibRefs[r]);
			bibRefStringsByID.setProperty(bibRefs[r].getAnnotationID(), bibRefString);
			bibRefsByCitationStrings.put(bibRefString, bibRefs[r]);
			bibRefStrings[r+1] = bibRefString;
		}
		
		//	preset selected bibliographic references
		String[] selectedCitationStrings = new String[bibRefCitations.length];
		for (int r = 0; r < bibRefCitations.length; r++) {
			if (citedBibRefs[r] == null)
				selectedCitationStrings[r] = NOT_A_CITATION_CATEGORY;
			else {
				String bibRefString = bibRefStringsByID.getProperty(citedBibRefs[r].getAnnotationID());
				selectedCitationStrings[r] = ((bibRefString == null) ? NOT_A_CITATION_CATEGORY : bibRefString);
			}
		}
		
		//	compute number of bibliographic reference references per dialog
		int dialogCount = ((bibRefCitations.length + 9) / 10);
		int dialogSize = ((bibRefCitations.length + (dialogCount / 2)) / dialogCount);
		dialogCount = ((bibRefCitations.length + dialogSize - 1) / dialogSize);
		
		//	build dialogs
		CategorizationFeedbackPanel[] cfps = new CategorizationFeedbackPanel[dialogCount];
		for (int d = 0; d < cfps.length; d++) {
			cfps[d] = new CategorizationFeedbackPanel("Check Bibliographic Reference Citations");
			cfps[d].setLabel("<HTML>Please check which bibliographic references these citations (printed in bold) refer to." +
					"<BR>If an assumed citation actually is none, please select <I>&lt;Not a citation&gt;</I> to indicate so.</HTML>");
			cfps[d].setChangeSpacing(10);
			cfps[d].setContinueSpacing(10);
			cfps[d].setPropagateCategoryChanges(false);
			for (int c = 0; c < bibRefStrings.length; c++) {
				cfps[d].addCategory(bibRefStrings[c]);
				cfps[d].setCategoryColor(bibRefStrings[c], ((c == 0) ? Color.WHITE : Color.YELLOW));
			}
			int dialogOffset = (d * dialogSize);
			
			//	add taxon name candidates
			for (int r = 0; (r < dialogSize) && ((r + dialogOffset) < bibRefCitations.length); r++) {
				cfps[d].addLine("<HTML>" + buildLabel(data, bibRefCitations[r + dialogOffset], 10) + "</HTML>");
				cfps[d].setCategoryAt(r, selectedCitationStrings[r + dialogOffset]);
			}
			
			//	add background information
			cfps[d].setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, data.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
			cfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, data.getAnnotationID());
			cfps[d].setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, BIB_REF_CITATION_TYPE);
		}
		int cutoffIndex = bibRefCitations.length;
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled()) {
			FeedbackPanel.getMultiFeedback(cfps);
			
			//	process all feedback data together
			for (int d = 0; d < cfps.length; d++) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < cfps[d].lineCount(); b++)
					selectedCitationStrings[b + dialogOffset] = cfps[d].getCategoryAt(b);
			}
		}
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int d = 0; d < cfps.length; d++) {
			if (d != 0)
				cfps[d].addButton("Previous");
			cfps[d].addButton("Cancel");
			cfps[d].addButton("OK" + (((d+1) == cfps.length) ? "" : " & Next"));
			
			String title = cfps[d].getTitle();
			cfps[d].setTitle(title + " - (" + (d+1) + " of " + cfps.length + ")");
			
			String f = cfps[d].getFeedback();
			if (f == null) f = "Cancel";
			
			cfps[d].setTitle(title);
			
			//	current dialog submitted, process data
			if (f.startsWith("OK")) {
				int dialogOffset = (d * dialogSize);
				for (int b = 0; b < cfps[d].lineCount(); b++)
					selectedCitationStrings[b + dialogOffset] = cfps[d].getCategoryAt(b);
			}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				d-=2;
			
			//	cancel from current dialog on
			else {
				cutoffIndex = (d * dialogSize);
				d = cfps.length;
			}
		}
		
		//	put feedback result in array
		for (int c = 0; c < cutoffIndex; c++) {
			if (NOT_A_CITATION_CATEGORY.equals(selectedCitationStrings[c]))
				citedBibRefs[c] = null;
			else citedBibRefs[c] = ((QueriableAnnotation) bibRefsByCitationStrings.get(selectedCitationStrings[c]));
		}
		
		//	indicate range of verification
		return cutoffIndex;
	}
	
	private boolean isResolved(Annotation bibRefCitation) {
		return ((bibRefCitation != null) && (bibRefCitation.hasAttribute(AUTHOR_ANNOTATION_TYPE) || bibRefCitation.hasAttribute(YEAR_ANNOTATION_TYPE) || bibRefCitation.hasAttribute(TITLE_ANNOTATION_TYPE)));
	}
	
	private String buildBibRefString(QueriableAnnotation bibRef) {
		StringBuffer cs = new StringBuffer();
		
		Annotation[] authors = bibRef.getAnnotations(AUTHOR_ANNOTATION_TYPE);
		if ((authors.length != 0) && (authors[0].getStartIndex() > 0))
			cs.append(TokenSequenceUtils.concatTokens(bibRef, 0, authors[0].getStartIndex()) + " ");
		else if (authors.length == 0) {
			Annotation[] annotations = bibRef.getAnnotations();
			int ai = 0;
			while ((ai < annotations.length) && (annotations[ai].size() == bibRef.size()))
				ai++;
			if ((ai < annotations.length) && (annotations[ai].getStartIndex() > 0) && Gamta.isNumber(bibRef.firstValue())) {
				cs.append(bibRef.firstValue());
				if ((annotations[ai].getStartIndex() > 1) && Gamta.isPunctuation(bibRef.valueAt(1)))
					cs.append(bibRef.valueAt(1));
				cs.append(" ");
			}
		}
		
		if (authors.length > 0) {
			for (int a = 0; a < Math.min(2, authors.length); a++) {
				if (a == 1) {
					if (authors.length == 2)
						cs.append(" & ");
					else cs.append(", ");
				}
				if (authors[a].hasAttribute(REPEATED_AUTHORS_ATTRIBUTE))
					cs.append((String) authors[a].getAttribute(REPEATED_AUTHORS_ATTRIBUTE));
				else cs.append(authors[a].getValue());
			}
			if (authors.length > 2)
				cs.append(" et al.");
		}
		else {
			String author = ((String) bibRef.getAttribute(AUTHOR_ANNOTATION_TYPE));
			if (author != null)
				cs.append(author);
		}
		
		String year = ((String) bibRef.getAttribute(YEAR_ANNOTATION_TYPE));
		Annotation[] years = bibRef.getAnnotations(YEAR_ANNOTATION_TYPE);
		if (years.length != 0) {
			if (year == null)
				year = years[0].getValue();
			if (years[0].getEndIndex() < bibRef.size()) {
				String yearSuffix = bibRef.valueAt(years[0].getEndIndex());
				if (yearSuffix.matches("[a-z]"))
					year += yearSuffix;
			}
		}
		cs.append(" (" + ((year == null) ? "<unknown>" : year) + ")");
		
		String title = ((String) bibRef.getAttribute(TITLE_ANNOTATION_TYPE));
		if (title == null)
			cs.append(": <untitled>");
		else {
			TokenSequence titleTokens = Gamta.newTokenSequence(title, bibRef.getTokenizer());
			cs.append(": " + TokenSequenceUtils.concatTokens(titleTokens, 0, Math.min(10, titleTokens.size())) + ((titleTokens.size() > 10) ? " ..." : ""));
		}
		
		return cs.toString();
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
	
	private Annotation[] getBibRefCitationsNumbers(MutableAnnotation data, int bibRefStart, SortedSet bibRefNumbers) {
//		System.out.println("Getting numeric ciations");
//		System.out.println(" - got " + bibRefNumbers.size() + " reference numbers");
		
		//	find minimum and maximum number
		int maxCitationNumber = ((Integer) bibRefNumbers.last()).intValue();
//		System.out.println(" - maximum citation number is " + maxCitationNumber);
		
		//	tag round-bracket references in text
//		System.out.println("Getting round bracket citations");
		Annotation[] rawBrcsRound = Gamta.extractAllMatches(data, "\\(++[^\\)]*?[0-9]++[^\\)]*?\\)++");
		TreeSet matchedBrcsRound = new TreeSet();
		ArrayList brcListRound = new ArrayList();
		
		//	extract individual numbers
		for (int r = 0; r < rawBrcsRound.length; r++) {
//			System.out.println(" - checking " + rawBrcsRound[r].getValue());
			if (rawBrcsRound[r].getEndIndex() < bibRefStart)
				for (int t = 0; t < rawBrcsRound[r].size(); t++) {
					String number = rawBrcsRound[r].valueAt(t);
					while (number.endsWith(".") || number.endsWith(",") || number.endsWith(":") || number.endsWith(";"))
						number = number.substring(0, (number.length() - 1));
					if (!number.matches("[1-9][0-9]{0,3}")) // very unlikely we have over 10,000 references ...
						continue;
					int n = Integer.parseInt(number);
					if (n <= maxCitationNumber) {
//						System.out.println(" --> got in bounds number " + n);
						matchedBrcsRound.add(new Integer(n));
						brcListRound.add(Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (rawBrcsRound[r].getStartIndex() + t), 1));
					}
//					else System.out.println(" --> got out of bounds number " + n);
				}
		}
//		System.out.println(" ==> got " + brcListRound.size() + " potential round bracket citations");
		
		
		//	tag square-bracket references in text
//		System.out.println("Getting square bracket citations");
		Annotation[] rawBrcsSquare = Gamta.extractAllMatches(data, "\\[++[^\\]]*?[0-9]++[^\\]]*?\\]++");
		TreeSet matchedBrcsSquare = new TreeSet();
		ArrayList brcListSquare = new ArrayList();
		
		//	extract individual numbers
		for (int r = 0; r < rawBrcsSquare.length; r++) {
//			System.out.println(" - checking " + rawBrcsSquare[r].getValue());
			if (rawBrcsSquare[r].getEndIndex() < bibRefStart)
				for (int t = 0; t < rawBrcsSquare[r].size(); t++) {
					String number = rawBrcsSquare[r].valueAt(t);
					while (number.endsWith(".") || number.endsWith(",") || number.endsWith(":") || number.endsWith(";"))
						number = number.substring(0, (number.length() - 1));
					if (!number.matches("[1-9][0-9]{0,3}")) // very unlikely we have over 10,000 references ...
						continue;
					int n = Integer.parseInt(number);
					if (n <= maxCitationNumber) {
//						System.out.println(" --> got in bounds number " + n);
						matchedBrcsSquare.add(new Integer(n));
						brcListSquare.add(Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (rawBrcsSquare[r].getStartIndex() + t), 1));
					}
//					else System.out.println(" --> got out of bounds number " + n);
				}
		}
//		System.out.println(" ==> got " + brcListSquare.size() + " potential square bracket citations");
		
		
		//	get superscript citations (do this by paragraph, as superscripts have to be smaller than text _surrounding_them_)
//		System.out.println("Getting super script citations");
		TreeSet matchedBrcsSmall = new TreeSet();
		ArrayList brcListSmall = new ArrayList();
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	try to compute average font size
			int fontSizeSum = 0;
			int fontSizeCount = 0;
			for (int t = 0; t < paragraphs[p].size(); t++) try {
				fontSizeSum += Integer.parseInt((String) paragraphs[p].tokenAt(t).getAttribute("fontSize"));
				fontSizeCount++;
			} catch (RuntimeException re) {}
			
			//	do we have a reliable font size?
			if ((fontSizeCount * 3) < (paragraphs[p].size() * 2))
				continue;
			
			//	tag superscript citations if we have a reliable font size
			int avgFontSize = (fontSizeSum / fontSizeCount);
//			System.out.println(" - average font size is " + avgFontSize);
			for (int t = 0; t < paragraphs[p].size(); t++) {
				String number = paragraphs[p].valueAt(t);
				if (!number.matches("[1-9][0-9]{0,3}")) // very unlikely we have over 10,000 references ...
					continue;
				int n = Integer.parseInt(number);
				if (n <= maxCitationNumber) try {
					int fontSize = Integer.parseInt((String) paragraphs[p].tokenAt(t).getAttribute("fontSize"));
//					System.out.println(" - got in bounds number " + n + " sized " + fontSize);
					if (fontSize < avgFontSize) {
//						System.out.println(" --> got in bounds small script number " + n);
						matchedBrcsSmall.add(new Integer(n));
						brcListSmall.add(Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (paragraphs[p].getStartIndex() + t), 1));
					}
//					else System.out.println(" --> got in bounds, but too large number " + n);
				} catch (RuntimeException re) {}
			}
		}
//		System.out.println(" ==> got " + brcListSmall.size() + " potential super script citations");
		
		
		//	test which of round-bracket, square-bracket, and superscript cover existing numbers best
		matchedBrcsRound.retainAll(bibRefNumbers);
//		System.out.println("Got " + matchedBrcsRound.size() + " round bracket citations matching reference numbers");
		matchedBrcsSquare.retainAll(bibRefNumbers);
//		System.out.println("Got " + matchedBrcsSquare.size() + " square bracket citations matching reference numbers");
		matchedBrcsSmall.retainAll(bibRefNumbers);
//		System.out.println("Got " + matchedBrcsSmall.size() + " super script citations matching reference numbers");
		ArrayList brcList = null;
		if (matchedBrcsRound.size() >= Math.max(matchedBrcsSquare.size(), matchedBrcsSmall.size()))
			brcList = brcListRound;
		else if (matchedBrcsSquare.size() >= Math.max(matchedBrcsRound.size(), matchedBrcsSmall.size()))
			brcList = brcListSquare;
		else if (matchedBrcsSmall.size() >= Math.max(matchedBrcsRound.size(), matchedBrcsSquare.size()))
			brcList = brcListSmall;
		
		//	return result
		return ((Annotation[]) brcList.toArray(new Annotation[brcList.size()]));
	}
	
	private Annotation[] getBibRefCitationsAuthorYear(MutableAnnotation data, int bibRefStart, StringVector authorNameTokens, StringVector authorYearPhrases, HashMap authorsByYear) {
		ArrayList brcList = new ArrayList();
		
		//	tag references in text
		for (int p = 0; p < this.bibRefCitationPatterns.length; p++) {
			if (DEBUG) System.out.println("Using pattern " + this.bibRefCitationPatterns[p]);
			Annotation[] rawBrcs = Gamta.extractAllMatches(data, this.bibRefCitationPatterns[p]);
			
			//	split raw references at in-bracket commas
			for (int c = 0; c < rawBrcs.length; c++) {
				if (rawBrcs[c].getEndIndex() >= bibRefStart)
					continue;
				if (DEBUG) System.out.println(" - " + rawBrcs[c].toXML());
				
				String author = null;
				boolean authorSinceLastBrc = false;
				
				for (int t = 0; t < rawBrcs[c].size(); t++) {
					String value = rawBrcs[c].valueAt(t);
					while (value.endsWith(".") || value.endsWith(",") || value.endsWith(":") || value.endsWith(";") || value.endsWith("-") || value.endsWith("'"))
						value = value.substring(0, (value.length() - 1));
					if (value.matches("[1-2][0-9]{3}")) {
						String yearSuffix = null;
						if ((t+1) < rawBrcs[c].size() && rawBrcs[c].valueAt(t+1).matches("[a-z]"))
							yearSuffix = rawBrcs[c].valueAt(t+1);
						Annotation brc = Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (rawBrcs[c].getStartIndex() + t), ((yearSuffix == null) ? 1 : 2));
						if (author != null)
							brc.setAttribute(AUTHOR_ANNOTATION_TYPE, author);
						brc.setAttribute(YEAR_ANNOTATION_TYPE, value);
						if (yearSuffix != null)
							brc.setAttribute(YEAR_SUFFIX_ATTRIBUTE, yearSuffix);
						brcList.add(brc);
						if (DEBUG) System.out.println(" -year-RC-> " + brc.toXML());
						authorSinceLastBrc = false;
					}
					else if (value.matches("[A-Z].++")) {
						if ((!authorSinceLastBrc) || (author == null)) {
							author = value;
							authorSinceLastBrc = true;
						}
						else if ((value.length() > author.length()) || (value.matches("[A-Z]{2,}") && !author.matches("[A-Z]{2,}")) || (!authorNameTokens.containsIgnoreCase(author) && authorNameTokens.containsIgnoreCase(value)))
							author = value;
					}
				}
				
				//	use whole match as well if there's a single year and at most one suffix
				int yearCount = 0;
				String year = null;
				int yearSuffixCount = 0;
				String yearSuffix = null;
				for (int t = 0; t < rawBrcs[c].size(); t++) {
					String value = rawBrcs[c].valueAt(t);
					while (value.endsWith(".") || value.endsWith(",") || value.endsWith(":") || value.endsWith(";") || value.endsWith("-") || value.endsWith("'"))
						value = value.substring(0, (value.length() - 1));
					if (value.matches("[1-2][0-9]{3}")) {
						yearCount++;
						year = value;
					}
					else if ((yearCount != 0) && value.matches("[a-z]")) {
						yearSuffixCount++;
						yearSuffix = value;
					}
				}
				
				//	more than one year, or more than one year suffix, continue
				if ((yearCount > 1) || (yearSuffixCount > 1))
					continue;
				
				//	find last name of (first) author
				author = null;
				for (int t = 0; t < rawBrcs[c].size(); t++) {
					String value = rawBrcs[c].valueAt(t);
					if ((t != 0) && ((",;&;and;und;et;y;[;(;{".indexOf(value) != -1) || value.matches("[12][0-9]{3}.*")))
						break;
					else if (value.matches("[a-z\\']{0,3}[A-Z].*"))
						author = value;
				}
				
				//	author not found
				if (author == null)
					continue;
				
				//	cut off brackets if enclosing whole citation
				int cutoff = 0;
				if (Gamta.isOpeningBracket(rawBrcs[c].firstValue()) && Gamta.closes(rawBrcs[c].lastValue(), rawBrcs[c].firstValue()))
					cutoff++;
				
				//	store candidate citation
				Annotation brc = Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (rawBrcs[c].getStartIndex() + cutoff), (rawBrcs[c].size() - (2 * cutoff)));
				brc.setAttribute(AUTHOR_ANNOTATION_TYPE, author);
				brc.setAttribute(YEAR_ANNOTATION_TYPE, year);
				if (yearSuffix != null)
					brc.setAttribute(YEAR_SUFFIX_ATTRIBUTE, yearSuffix);
				brcList.add(brc);
				if (DEBUG) System.out.println(" -match-RC-> " + brc.toXML());
			}
		}
		
		//	tag author+year phrases
		Annotation[] rawBrcs = Gamta.extractAllContained(data, authorYearPhrases);
		for (int c = 0; c < rawBrcs.length; c++) {
			if (rawBrcs[c].getEndIndex() >= bibRefStart)
				continue;
			
			String author = rawBrcs[c].firstValue();
			String year = null;
			String yearSuffix = null;
			Annotation brc;
			
			if (rawBrcs[c].lastValue().matches("[a-z]")) {
				year = rawBrcs[c].valueAt(rawBrcs[c].size()-2);
				yearSuffix = rawBrcs[c].lastValue();
				brc = Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, rawBrcs[c].getStartIndex(), rawBrcs[c].size());
			}
			else {
				year = rawBrcs[c].lastValue();
				if ((rawBrcs[c].getEndIndex() < data.size()) && data.valueAt(rawBrcs[c].getEndIndex()).matches("[a-z]"))
					yearSuffix = data.valueAt(rawBrcs[c].getEndIndex());
				brc = Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, rawBrcs[c].getStartIndex(), (rawBrcs[c].size() + ((yearSuffix == null) ? 0 : 1)));
			}
			
			brc.setAttribute(AUTHOR_ANNOTATION_TYPE, author);
			brc.setAttribute(YEAR_ANNOTATION_TYPE, year);
			if (yearSuffix != null)
				brc.setAttribute(YEAR_SUFFIX_ATTRIBUTE, yearSuffix);
			brcList.add(brc);
		}
		
		//	tag years (watch paragraph boundaries)
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			if (paragraphs[p].getEndIndex() >= bibRefStart)
				continue;
			int lastCrEnd = 0;
			String lastYear = null;
			String lastAuthor = null;
			for (int t = 0; t < paragraphs[p].size(); t++) {
				if (authorsByYear.containsKey(paragraphs[p].valueAt(t))) {
					String year = paragraphs[p].valueAt(t);
					String yearSuffix = ((((t+1) < paragraphs[p].size()) && authorsByYear.containsKey(year + " " + paragraphs[p].valueAt(t+1))) ? paragraphs[p].valueAt(t+1) : null);
					String author = null;
					
					int crStart = t;
					HashSet yearAuthors = ((HashSet) authorsByYear.get((yearSuffix == null) ? year : (year + " " + yearSuffix)));
					for (int a = t-1; a >= lastCrEnd; a--) {
						String authorCandidate = paragraphs[p].valueAt(a);
						if (yearAuthors.contains(authorCandidate)) {
							author = authorCandidate;
							crStart = a;
						}
						else if ((authorCandidate.length() > 1) && (";and;und;et;al;".indexOf(authorCandidate) == -1))
							break;
					}
					int crSize = (t - crStart + ((yearSuffix == null) ? 1 : 2));
					
					Annotation cr = Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (paragraphs[p].getStartIndex() + crStart), crSize);
					cr.setAttribute(YEAR_ANNOTATION_TYPE, year);
					if (yearSuffix != null)
						cr.setAttribute(YEAR_SUFFIX_ATTRIBUTE, yearSuffix);
					if (author != null)
						cr.setAttribute(AUTHOR_ANNOTATION_TYPE, author);
					brcList.add(cr);
					
					lastCrEnd = crStart + crSize;
					lastYear = year;
					lastAuthor = author;
				}
				else if ((lastYear != null) && (t < (lastCrEnd + 2)) && authorsByYear.containsKey(lastYear + " " + paragraphs[p].valueAt(t))) {
					Annotation cr = Gamta.newAnnotation(data, BIB_REF_CITATION_TYPE, (paragraphs[p].getStartIndex() + t), 1);
					cr.setAttribute(YEAR_ANNOTATION_TYPE, lastYear);
					cr.setAttribute(YEAR_SUFFIX_ATTRIBUTE, paragraphs[p].valueAt(t));
					if (lastAuthor != null)
						cr.setAttribute(AUTHOR_ANNOTATION_TYPE, lastAuthor);
					brcList.add(cr);
					
					lastCrEnd = (t+1);
				}
			}
		}
		
		//	sort & sort out result
		Collections.sort(brcList);
		for (int r = 1; r < brcList.size(); r++) {
			Annotation cr1 = ((Annotation) brcList.get(r - 1));
			Annotation cr2 = ((Annotation) brcList.get(r));
			if (AnnotationUtils.compare(cr1, cr2) == 0) {
				cr1.copyAttributes(cr2);
				brcList.remove(r--);
			}
		}
		
		//	infer authors
		for (int r = 1; r < brcList.size(); r++) {
			Annotation cr = ((Annotation) brcList.get(r));
			if (cr.hasAttribute(AUTHOR_ANNOTATION_TYPE))
				continue;
			Annotation lcr = ((Annotation) brcList.get(r-1));
			if ((lcr.getEndIndex() + 5) < cr.getStartIndex())
				continue;
			if (lcr.hasAttribute(AUTHOR_ANNOTATION_TYPE))
				cr.setAttribute(AUTHOR_ANNOTATION_TYPE, lcr.getAttribute(AUTHOR_ANNOTATION_TYPE));
		}
		
		
		//	sort out nested results with same attributes
		for (int r = 1; r < brcList.size(); r++) {
			Annotation cr1 = ((Annotation) brcList.get(r - 1));
			Annotation cr2 = ((Annotation) brcList.get(r));
			if (AnnotationUtils.contains(cr1, cr2) && AttributeUtils.hasEqualAttributes(cr2, cr1, new HashSet(Arrays.asList(cr2.getAttributeNames()))))
				brcList.remove(r--);
		}
		
		//	return what we got
		return ((Annotation[]) brcList.toArray(new Annotation[brcList.size()]));
	}
	
	/** @see de.uka.ipd.idahe.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			this.activeBibRefCitationPatternNames.clear();
			this.activeBibRefCitationPatternNames.addContentIgnoreDuplicates(this.loadList(BIB_REF_CITATION_PATTERN_NAME_FILE));
		} catch (IOException e) {}
		
		this.buildCitationPatterns();
	}
	
	/** @see de.uka.ipd.idahe.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		try {
			this.storeList(this.activeBibRefCitationPatternNames, BIB_REF_CITATION_PATTERN_NAME_FILE);
		} catch (IOException ioe) {}
	}
	
	private void buildCitationPatterns() {
		StringVector regExes = new StringVector();
		StringVector regExNames = new StringVector();
		
		Properties resolver = this.getSubPatternNameResolver();
		for (int r = 0; r < this.activeBibRefCitationPatternNames.size(); r++) {
			String patternName = this.activeBibRefCitationPatternNames.get(r);
			try {
				InputStream is = this.dataProvider.getInputStream(patternName);
				StringVector rawRegEx = StringVector.loadList(is);
				is.close();
				regExes.addElement(RegExUtils.preCompile(RegExUtils.normalizeRegEx(rawRegEx.concatStrings("\n")), resolver));
				regExNames.addElement(patternName);
			}
			catch (IOException ioe) {
				if (DEBUG) System.out.println("BibRefCitationTagger: could not load pattern '" + patternName + "':\n  " + ioe.getClass().getName() + " (" + ioe.getMessage() + ")");
			}
			catch (PatternSyntaxException pse) {
				if (DEBUG) System.out.println("BibRefCitationTagger: could not compile pattern '" + patternName + "':\n  " + pse.getClass().getName() + " (" + pse.getMessage() + ")");
			}
		}
		
		this.bibRefCitationPatterns = regExes.toStringArray();
	}
	
	/** @see de.uka.ipd.idahe.gamta.util.AbstractConfigurableAnalyzer#configureProcessor()
	 */
	public void configureProcessor() {
		
		//	let super class show dialog
		super.configureProcessor();
		
		//	re-initialize with new values
		this.initAnalyzer();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigTitle()
	 */
	protected String getConfigTitle() {
		return "Configure Bibliographic Reference Citation Tagger";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		NameAttributeSet[] attributes = {
			new NameAttributeSet(BIB_REF_CITATION_PATTERN_NAME_FILE, "Use", this.dataProvider)
		};
		RegExConfigPanel recp = new RegExConfigPanel(this.dataProvider, ".regEx.brc.txt", attributes, "", "", true);
		AnalyzerConfigPanel[] acps = {
			recp,
		};
		return acps;
	}
	
	private Properties getSubPatternNameResolver() {
		return new Resolver();
	}
	
	private class Resolver extends Properties {
		public String getProperty(String name, String def) {
			try {
				if (!name.endsWith(REGEX_NAME_EXTENSION)) name += REGEX_NAME_EXTENSION;
				InputStream is = dataProvider.getInputStream(name);
				StringVector rawRegEx = StringVector.loadList(is);
				is.close();
				return RegExUtils.preCompile(RegExUtils.normalizeRegEx(rawRegEx.concatStrings("\n")), this);
			}
			catch (IOException ioe) {
				return def;
			}
		}
		public String getProperty(String name) {
			return this.getProperty(name, null);
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/20351_gg2d.xml"), "UTF-8"));
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21291_gg2g.xml"), "UTF-8"));
		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument("E:/Projektdaten/TaxonX Corpus/21003_gg1_clean.xml");
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument("E:/Projektdaten/AdHocData/21401_gg2.citations.xml");
		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument("E:/Projektdaten/AdHocData/21401_gg2.1211905826119.xml");
		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/21330.citations.xml"), "UTF-8"));
//		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/SMNK-Projekt/EcologyTestbed/schatz1995a.normalized.xml"), "UTF-8"));
//		AnnotationFilter.renameAnnotations(doc, CITATION_TYPE, BIBLIOGRAPHIC_REFERENCE_TYPE);
		
		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/PdfExtract/Zootaxa/zt00391p001.pdf.imf.xml"), "UTF-8"));
//		
//		System.out.println("Document loaded");
//		Gamta.addTestDocumentProvider(new TestDocumentProvider() {
//			public QueriableAnnotation getTestDocument() {
//				return doc;
//			}
//		});
//		
//		Analyzer ct = new CitationTagger();
//		ct.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/CitationHandlerData/")));
//		
//		Analyzer cpo = new CitationParserOnline();
//		cpo.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/CitationHandlerData/")));
//		
		Analyzer brct = new BibRefCitationTaggerOnline();
		brct.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/BibRefHandlerData/")));
		
		System.out.println("Analyzers initialized");
//		if (true) {
//			brct.configureProcessor();
//			return;
//		}
		
//		MutableAnnotation[] citations = doc.getMutableAnnotations(CITATION_TYPE);
//		for (int c = 0; c < citations.length; c++)
//			doc.removeAnnotation(citations[c]);
//		
//		ct.process(doc, new Properties() {
//			public synchronized boolean containsKey(Object key) {
//				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
//			}
//		});
//		
//		citations = doc.getMutableAnnotations(CITATION_TYPE);
//		for (int c = 0; c < citations.length; c++)
//			System.out.println(citations[c].toXML());
//		
//		
//		cpo.process(doc, new Properties() {
//			public synchronized boolean containsKey(Object key) {
//				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
//			}
//		});
//		cpo.exit();
//		
//		citations = doc.getMutableAnnotations(CITATION_TYPE);
//		BufferedWriter bw = new BufferedWriter(new PrintWriter(System.out));
//		for (int c = 0; c < citations.length; c++) {
//			AnnotationUtils.writeXML(citations[c], bw);
//			bw.newLine();
//		}
//		bw.flush();
//		
		
		brct.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
//		crt.exit();
//		
//		citations = doc.getMutableAnnotations(CITATION_TYPE);
//		bw = new BufferedWriter(new PrintWriter(System.out));
//		for (int c = 0; c < citations.length; c++) {
//			AnnotationUtils.writeXML(citations[c], bw);
//			bw.newLine();
//		}
//		bw.flush();
		
		MutableAnnotation[] bibRefCitations = doc.getMutableAnnotations(BIB_REF_CITATION_TYPE);
		for (int c = 0; c < bibRefCitations.length; c++)
			System.out.println(bibRefCitations[c].getStartIndex() + " - " + bibRefCitations[c].toXML());
	}
}
