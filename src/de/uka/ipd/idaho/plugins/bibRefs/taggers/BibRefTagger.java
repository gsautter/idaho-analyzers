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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import javax.swing.JDialog;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameAttributeSet;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.ParameterConfigPanel;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.RegExConfigPanel;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * An Analyzer taggging bibliographic references using a set of regular expression patterns.
 * 
 * @author sautter
 */
public class BibRefTagger extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String REGEX_NAME_EXTENSION = ".regEx.txt"; 
	
	private static final String MAX_BIB_REF_LINES_SETTING = "MaxLines";
	private static final int DEFAULT_MAX_BIB_REF_LINES = 4;
	private int maxBibRefLines = DEFAULT_MAX_BIB_REF_LINES;
	
	private static final String MAX_BIB_REF_WORDS_SETTING = "MaxWords";
	private static final int DEFAULT_MAX_BIB_REF_WORDS = 100;
	private int maxBibRefWords = DEFAULT_MAX_BIB_REF_WORDS;
	
	private static final String BIB_REF_PATTERN_NAME_FILE = "useBibRefPatterns.cnfg";
	private StringVector activeBibRefPatternNames = new StringVector();
	
	private String[] bibRefPatterns;
	private String[] bibRefPatternNames;
	
	/** @see de.uka.ipd.idahe.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		StringVector docTextTokens = TokenSequenceUtils.getTextTokens(data);
		docTextTokens.removeDuplicateElements(false);
		
		//	check individual paragraphs
		Set nonBibRefParagraphIDs = new HashSet();
		Set possibleBibRefParagraphIDs = new HashSet();
		Set bibRefParagraphIDs = new HashSet();
		Set urlParagraphIDs = new HashSet();
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			
			//	check number of lines
			int paragraphLines = 0;
			for (int t = 0; t < paragraph.size(); t++)
				if (paragraph.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) paragraphLines++;
			int paragraphWords = TokenSequenceUtils.getTextTokens(paragraph).size();
			
			//	is this paragraph small enough to be a bibliographic reference?
			if ((paragraphWords <= this.maxBibRefWords) && (paragraphLines <= this.maxBibRefLines)) {
				possibleBibRefParagraphIDs.add(paragraph.getAnnotationID());
				
				//	check other evidence
				String evidence = this.isBibRef(paragraph, docTextTokens);
				if (evidence != null) {
					
					//	according to the patterns, this paragraph's likely to be a bibliographic reference
					bibRefParagraphIDs.add(paragraph.getAnnotationID());
					Annotation bibRef = data.addAnnotation(BIBLIOGRAPHIC_REFERENCE_TYPE, paragraph.getStartIndex(), paragraph.size());
					bibRef.setAttribute("_evidence", evidence);
				}
			}
			
			//	is it small enough to possibly be a bibliographic reference, depending on the surrounding paragraphs?
			else if ((paragraphWords <= (2 * this.maxBibRefWords)) && (paragraphLines <= (2 * this.maxBibRefLines))) {
				possibleBibRefParagraphIDs.add(paragraph.getAnnotationID());
				nonBibRefParagraphIDs.add(paragraph.getAnnotationID());
			}
			
			//	remember possible bibliographic reference blocks
			else nonBibRefParagraphIDs.add(paragraph.getAnnotationID());
			
			//	remember URLs
			if ((paragraphLines == 1) && paragraph.getValue().matches("http\\:\\s*\\/\\/.*"))
				urlParagraphIDs.add(paragraph.getAnnotationID());
		}
		
		//	check paragraphs together
		for (int p = 1; p < (paragraphs.length-1); p++) {
			MutableAnnotation paragraph = paragraphs[p];
			
			//	this one's already dealt with
			if (bibRefParagraphIDs.contains(paragraph.getAnnotationID()))
				continue;
			
			//	check above and below
			boolean bibRefAbove = bibRefParagraphIDs.contains(paragraphs[p-1].getAnnotationID());
			boolean bibRefBelow = bibRefParagraphIDs.contains(paragraphs[p+1].getAnnotationID());
			
			//	URL, see if we can attach it
			if (urlParagraphIDs.contains(paragraph.getAnnotationID()) && bibRefAbove) {
				Annotation bibRef = data.addAnnotation(BIBLIOGRAPHIC_REFERENCE_TYPE, paragraphs[p-1].getStartIndex(), (paragraph.getEndIndex() - paragraphs[p-1].getStartIndex()));
				Annotation[] eBibRef = paragraphs[p-1].getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
				if (eBibRef.length != 0) {
					bibRef.copyAttributes(eBibRef[0]);
					paragraphs[p-1].removeAnnotation(eBibRef[0]);
				}
				bibRefParagraphIDs.add(paragraph.getAnnotationID());
			}
			
			//	this one's not a bibliographic reference yet, but might be one judging its size
			else if (possibleBibRefParagraphIDs.contains(paragraph.getAnnotationID()) && bibRefAbove && bibRefBelow) {
				
				//	surrounded by two bibliographic references, this paragraph's likely to be a bibliographic reference as well
				Annotation bibRef = data.addAnnotation(BIBLIOGRAPHIC_REFERENCE_TYPE, paragraph.getStartIndex(), paragraph.size());
				bibRef.setAttribute("_evidence", "bridged");
			}
		}
		
		//	clean up
		AnnotationFilter.removeDuplicates(data, BIBLIOGRAPHIC_REFERENCE_TYPE);
		AnnotationFilter.removeInner(data, BIBLIOGRAPHIC_REFERENCE_TYPE);
		
		//	found anything?
		if (data.getMutableAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE).length != 0)
			return;
		
		//	go looking for possible bibliographic reference blocks
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			if (!nonBibRefParagraphIDs.contains(paragraph.getAnnotationID()))
				continue;
			
			//	check other evidence
			String evidence = this.isBibRefBlock(paragraph, docTextTokens);
			if (evidence != null) {
				
				//	according to the patterns, this paragraph's likely to be a bibliographic reference
				Annotation bibRef = data.addAnnotation(BIBLIOGRAPHIC_REFERENCE_TYPE, paragraph.getStartIndex(), paragraph.size());
				bibRef.setAttribute("_evidence", evidence);
			}
		}
	}
	
	//	regular expression for finding years (might be publication years)
	private static final String yearRegEx = "([12][0-9]{3})";
	
	private String isBibRef(MutableAnnotation paragraph, StringVector docTextTokens) {
		
		//	use patterns
		for (int p = 0; p < this.bibRefPatterns.length; p++) {
			
			//	get matches
			Annotation[] matches = Gamta.extractAllMatches(paragraph, this.bibRefPatterns[p], 25, false);
			
			for (int m = 0; m < matches.length; m++)
				System.out.println("Pattern match (" + this.bibRefPatternNames[p] + "): " + matches[m].getValue());
			
			//	check matches
			if (matches.length != 0) {
				int matchStart = matches[0].getStartIndex();
				boolean wordBefore = false;
				int wordCount = 0;
				boolean numberBefore = false;
				for (int b = 0; b < matchStart; b++) {
					String value = paragraph.valueAt(b);
					System.out.println("  - checking token before: '" + value + "'");
					
					//	word before match
					if (Gamta.isWord(paragraph.valueAt(b))) {
						System.out.println("    --> got word: '" + value + "'");
						wordCount++;
						
						//	word is not letter-coded numbering, or not first word to occur
						if ((wordCount > 1) || ((b+1) == matchStart) || (")].".indexOf(paragraph.valueAt(b+1)) == -1)) {
							System.out.println("    --> got word before: '" + value + "'");
							wordBefore = true;
							b = matchStart;
						}
					}
					
					//	number before match
					else if (Gamta.isNumber(paragraph.valueAt(b))) {
						System.out.println("    --> got number before: '" + value + "'");
						numberBefore = true;
						b = matchStart;
					}
					else System.out.println("    --> not a word");
				}
				
				if (wordBefore || numberBefore)
					System.out.println("  ==> first match too late in line");
				else {
					System.out.println("  ==> match");
					return this.bibRefPatternNames[p];
				}
			}
		}
//		
//		//	check title case words
//		int wordCount = 0;
//		int capWordCount = 0;
//		for (int t = 0; t < paragraph.size(); t++) {
//			Token token = paragraph.tokenAt(t);
//			if (Gamta.isWord(token)) {
//				wordCount ++;
//				if (Gamta.isFirstLetterUpWord(token) && docTextTokens.contains(token.getValue().toLowerCase()))
//					capWordCount ++;
//			}
//		}
//		
//		//	check for year
//		Annotation[] years = Gamta.extractAllMatches(paragraph, yearRegEx, 1);
//		
//		//	do we have a title case match?
//		if ((years.length != 0) && ((capWordCount * 3) > wordCount))
//			return "TitleCase";
		
		//	we can safely exclude this one
		return null;
	}
	
	private String isBibRefBlock(MutableAnnotation paragraph, StringVector docTextTokens) {
		int paragraphWords = TokenSequenceUtils.getTextTokens(paragraph).size();
		
		//	use patterns
		for (int p = 0; p < this.bibRefPatterns.length; p++) {
			
			//	get matches
			Annotation[] matches = Gamta.extractAllMatches(paragraph, this.bibRefPatterns[p], 25, false);
			
			for (int m = 0; m < matches.length; m++)
				System.out.println("Pattern match (" + this.bibRefPatternNames[p] + "): " + matches[m].getValue());
			
			//	check matches
			if (matches.length != 0) {
				int matchStart = matches[0].getStartIndex();
				boolean wordBefore = false;
				int wordCount = 0;
				boolean numberBefore = false;
				for (int b = 0; b < matchStart; b++) {
					String value = paragraph.valueAt(b);
					System.out.println("  - checking token before: '" + value + "'");
					
					//	word before match
					if (Gamta.isWord(paragraph.valueAt(b))) {
						System.out.println("    --> got word: '" + value + "'");
						wordCount++;
						
						//	word is not letter-coded numbering, or not first word to occur
						if ((wordCount > 1) || ((b+1) == matchStart) || (")].".indexOf(paragraph.valueAt(b+1)) == -1)) {
							System.out.println("    --> got word before: '" + value + "'");
							wordBefore = true;
							b = matchStart;
						}
					}
					
					//	number before match
					else if (Gamta.isNumber(paragraph.valueAt(b))) {
						System.out.println("    --> got number before: '" + value + "'");
						numberBefore = true;
						b = matchStart;
					}
					else System.out.println("    --> not a word");
				}
				
				if (wordBefore || numberBefore)
					System.out.println("  ==> too late in line");
				else if ((paragraphWords / matches.length) < this.maxBibRefWords) {
					System.out.println("  ==> match (" + matches.length + " in " + paragraphWords + " words)");
					return this.bibRefPatternNames[p];
				}
				else System.out.println("  ==> too few matches (" + matches.length + ") for " + paragraphWords + " words");
			}
		}
		
		//	for blocks, we use patterns only
		return null;
	}
	
	/** @see de.uka.ipd.idahe.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			this.maxBibRefLines = Integer.parseInt(this.getParameter(MAX_BIB_REF_LINES_SETTING));
		}
		catch (NumberFormatException nfe) {
			this.storeParameter(MAX_BIB_REF_LINES_SETTING, ("" + this.maxBibRefLines));
		}
		
		try {
			this.maxBibRefWords = Integer.parseInt(this.getParameter(MAX_BIB_REF_WORDS_SETTING));
		}
		catch (NumberFormatException nfe) {
			this.storeParameter(MAX_BIB_REF_WORDS_SETTING, ("" + this.maxBibRefWords));
		}
		
		try {
			this.activeBibRefPatternNames.clear();
			this.activeBibRefPatternNames.addContentIgnoreDuplicates(this.loadList(BIB_REF_PATTERN_NAME_FILE));
		} catch (IOException ioe) {}
		
		this.buildBibRefPatterns();
	}
	
	private void buildBibRefPatterns() {
		StringVector regExes = new StringVector();
		StringVector regExNames = new StringVector();
		
		Properties resolver = this.getSubPatternNameResolver();
		for (int r = 0; r < this.activeBibRefPatternNames.size(); r++) {
			String patternName = this.activeBibRefPatternNames.get(r);
			try {
				InputStream is = this.dataProvider.getInputStream(patternName);
				StringVector rawRegEx = StringVector.loadList(is);
				is.close();
				regExes.addElement(RegExUtils.preCompile(RegExUtils.normalizeRegEx(rawRegEx.concatStrings("\n")), resolver));
				regExNames.addElement(patternName);
			}
			catch (IOException ioe) {
				System.out.println("BibRefFinder: could not load pattern '" + patternName + "':\n  " + ioe.getClass().getName() + " (" + ioe.getMessage() + ")");
			}
			catch (PatternSyntaxException pse) {
				System.out.println("BibRefFinder: could not compile pattern '" + patternName + "':\n  " + pse.getClass().getName() + " (" + pse.getMessage() + ")");
			}
		}
		
		this.bibRefPatterns = regExes.toStringArray();
		this.bibRefPatternNames = regExNames.toStringArray();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigTitle()
	 */
	protected String getConfigTitle() {
		return "Configure Bibliographic Reference Tagger";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		ParameterConfigPanel pcp = new ParameterConfigPanel(this.dataProvider, "de.uka.ipd.idaho.plugins.bibliographicReferences.BibRefTagger.cnfg", "Base Parameters", "Basic parameters for bibliographic reference detection");
		pcp.addParameter(MAX_BIB_REF_LINES_SETTING, ("" + this.maxBibRefLines), "Maximum number of lines in a bibliographic reference", null, AnalyzerParameter.INTEGER_TYPE);
		pcp.addParameter(MAX_BIB_REF_WORDS_SETTING, ("" + this.maxBibRefWords), "Maximum number of words in a bibliographic reference", null, AnalyzerParameter.INTEGER_TYPE);
		NameAttributeSet[] attributes = {
			new NameAttributeSet(BIB_REF_PATTERN_NAME_FILE, "Use", this.dataProvider)
		};
		RegExConfigPanel recp = new RegExConfigPanel(this.dataProvider, RegExConfigPanel.DEFAULT_FILE_EXTENSION, attributes, "BibRef Start Patterns", "Patterns for typical starts of paragraphs that are bibliographic references", true);
		AnalyzerConfigPanel[] acps = {
			pcp,
			recp,
		};
		return acps;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#configureProcessor()
	 */
	public void configureProcessor() {
		
		//	let super class show dialog
		super.configureProcessor();
		
		//	re-initialize with new values
		this.initAnalyzer();
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
		
		BibRefTagger brt = new BibRefTagger();
		brt.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/BibRefHandlerData/")));
		if (true) {
			brt.configureProcessor();
			return;
		}
		
		MutableAnnotation doc = SgmlDocumentReader.readDocument("E:/Projektdaten/TaxonX Corpus/21003_gg1_clean.xml");
//		MutableAnnotation doc = SgmlDocumentReader.readDocument("E:/Projektdaten/AdHocData/21401_gg2.1211905826119.xml");
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<paragraph pageId=\"11\" pageNumber=\"172\">Rotmoos. 8. VIII. 1927.</paragraph>"));
		
		Annotation[] bibRefs = doc.getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		for (int r = 0; r < bibRefs.length; r++) {
			System.out.println("before: " + bibRefs[r].toXML());
			doc.removeAnnotation(bibRefs[r]);
		}
		
		brt.process(doc, new Properties());
		
		bibRefs = doc.getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		for (int r = 0; r < bibRefs.length; r++)
			System.out.println(bibRefs[r].toXML());
		
//		StringVector citationStrins = StringVector.loadList(new File("E:/Projektdaten/Citations.txt"));
//		for (int c = 0; c < citationStrins.size(); c++) {
//			String citationString = citationStrins.get(c);
//			DocumentPart doc = new Document(Gamta.tokenize(citationString));
//			doc.markPart(DocumentPart.PARAGRAPH_TYPE, 0, doc.size());
//			System.out.println();
//			System.out.println();
//			ct.process(doc, false);
//			Annotation[] citationAnnotations = doc.getAnnotations(Citation.CITATION_ANNOTATION_TYPE);
//			System.out.println(citationString);
//			for (int ca = 0; ca < citationAnnotations.length; ca++)
//				System.out.println("  " + citationAnnotations[ca].toXML());
//		}
	}
}
