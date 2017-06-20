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

import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * @author sautter
 *
 */
public class AuthorTagger extends OmniFatAnalyzer {
	
//	private static final boolean DEBUG = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFatFunctions.tagAuthorNames(data, this.getDataSet(data, parameters));
		
//		OmniFAT omniFat = this.getOmniFatInstance(parameters);
//		
//		//	prepare filters
//		Set lexiconMatchKeys = new HashSet();
//		
//		//	annotate author names from lexicons
//		OmniFatDictionary[] authorNameDictionaries = omniFat.getAuthorNameDictionaries();
//		for (int d = 0; d < authorNameDictionaries.length; d++) {
//			
//			Annotation[] authorNames = Gamta.extractAllContained(data, authorNameDictionaries[d], 1, true);
//			for (int a = 0; a < authorNames.length; a++) {
//				String authorNameKey = this.getKey(authorNames[a]);
//				
//				//	remember lexicon match
//				lexiconMatchKeys.add(authorNameKey);
//				
//				//	ignore single letters
//				if (authorNames[a].length() < 2)
//					continue;
//				
//				Annotation authorName = data.addAnnotation("authorName", authorNames[a].getStartIndex(), authorNames[a].size());
//				authorName.setAttribute("evidence", "lexicon");
//				authorName.setAttribute("evidenceDetail", authorNameDictionaries[d].name);
//				authorName.setAttribute("string", authorNames[a].getValue());
//				authorName.setAttribute("value", authorNames[a].getValue());
//				
//				authorName.setAttribute("state", (authorNameDictionaries[d].trusted ? "precision" : "recall"));
//			}
//		}
//		
//		//	annotate author names from patterns
//		OmniFatPattern[] authorNamePatterns = omniFat.getAuthorNamePatterns();
//		
//		//	avoid duplicates
//		Set matchKeys = new HashSet();
//		
//		//	apply patterns
//		for (int p = 0; p < authorNamePatterns.length; p++) {
//			Annotation[] authorNames = Gamta.extractAllMatches(data, authorNamePatterns[p].pattern, 0, false, false, true);
//			for (int a = 0; a < authorNames.length; a++) {
//				String authorNameKey = this.getKey(authorNames[a]);
//				
//				//	we've had this one already
//				if (!matchKeys.add(authorNameKey))
//					continue;
//				
//				//	do not duplicate lexicon matches (their rank group is clear)
//				if (lexiconMatchKeys.contains(authorNameKey))
//					continue;
//				
//				//	ignore single letters
//				if (authorNames[a].length() < 2)
//					continue;
//				
//				Annotation authorName = data.addAnnotation("authorName", authorNames[a].getStartIndex(), authorNames[a].size());
//				authorName.setAttribute("evidence", "pattern");
//				authorName.setAttribute("evidenceDetail", authorNamePatterns[p].name);
//				String authorNameValue = authorNames[a].getValue();
//				for (int v = 0; v < authorNames[a].size(); v++) {
//					String value = authorNames[a].valueAt(v);
//					if (Gamta.isWord(value))
//						authorNameValue = value;
//				}
//				authorName.setAttribute("string", authorNameValue);
//				authorName.setAttribute("value", authorNameValue);
//				
//				authorName.setAttribute("state", "recall");
//			}
//		}
//		
//		
//		//	annotate author name stop words from lexicon
//		Annotation[] authorNameStopWords = Gamta.extractAllContained(data, omniFat.getAuthorNameStopWords(), 1, true);
//		for (int a = 0; a < authorNameStopWords.length; a++)
//			data.addAnnotation("authorNameStopWord", authorNameStopWords[a].getStartIndex(), authorNameStopWords[a].size());
//		
//		//	join adjacent stop words
//		Annotation[] jointAuthorNameStopWords;
//		do {
//			jointAuthorNameStopWords = OmniFatFunctions.joinAdjacent(data, "authorNameStopWord", "authorNameStopWord", null);
//			for (int a = 0; a < jointAuthorNameStopWords.length; a++)
//				data.addAnnotation("authorNameStopWord", jointAuthorNameStopWords[a].getStartIndex(), jointAuthorNameStopWords[a].size());
//		} while (jointAuthorNameStopWords.length != 0);
//		
//		
//		//	join author names with stop words
//		Annotation[] jointAuthorNames;
//		do {
//			jointAuthorNames = OmniFatFunctions.joinAdjacent(data, "authorNameStopWord", "authorName", "authorName", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					joint.copyAttributes(rightSource);
//					if ("pattern".equals(joint.getAttribute("evidence")))
//						joint.setAttribute("evidence", "joint");
//					return joint;
//				}
//			});
//			for (int a = 0; a < jointAuthorNames.length; a++)
//				data.addAnnotation("authorName", jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
//		} while (jointAuthorNames.length != 0);
//		
//		//	clean up
//		AnnotationFilter.removeAnnotations(data, "authorNameStopWord");
//		
//		
//		//	annotate author initials from patterns
//		OmniFatPattern[] authorInitialPatterns = omniFat.getAuthorInitialPatterns();
//		
//		//	apply patterns for initials
//		for (int p = 0; p < authorInitialPatterns.length; p++) {
//			Annotation[] authorInitials = Gamta.extractAllMatches(data, authorInitialPatterns[p].pattern, 2, false, false, true);
//			for (int a = 0; a < authorInitials.length; a++) {
//				String authorInitialKey = this.getKey(authorInitials[a]);
//				
//				//	we've had this one already
//				if (!matchKeys.add(authorInitialKey))
//					continue;
//				
//				//	do not duplicate lexicon matches (their rank group is clear)
//				if (lexiconMatchKeys.contains(authorInitialKey))
//					continue;
//				
//				Annotation authorInitial = data.addAnnotation("authorInitial", authorInitials[a].getStartIndex(), authorInitials[a].size());
//				authorInitial.setAttribute("evidence", "pattern");
//				authorInitial.setAttribute("value", authorInitials[a].getValue());
//			}
//		}
//		
//		
//		//	join author names with initials
//		do {
//			jointAuthorNames = OmniFatFunctions.joinAdjacent(data, "authorInitial", "authorName", "authorName", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					joint.copyAttributes(rightSource);
//					if ("pattern".equals(joint.getAttribute("evidence")))
//						joint.setAttribute("evidence", "initials");
//					return joint;
//				}
//			});
//			for (int a = 0; a < jointAuthorNames.length; a++)
//				data.addAnnotation("authorName", jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
//		} while (jointAuthorNames.length != 0);
//		
//		//	clean up
//		AnnotationFilter.removeAnnotations(data, "authorInitial");
//		
//		
//		//	join author names with author names
//		do {
//			jointAuthorNames = OmniFatFunctions.joinAdjacent(data, "authorName", "authorName", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					joint.copyAttributes(rightSource);
//					if ("pattern".equals(joint.getAttribute("evidence")))
//						joint.setAttribute("evidence", "joint");
//					
//					String lsid = ((String) leftSource.getAttribute("source"));
//					if (lsid == null)
//						lsid = leftSource.getAnnotationID();
//					String rsid = ((String) rightSource.getAttribute("source"));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					String source = (lsid + "," + rsid);
//					joint.setAttribute("source", source);
//					
//					return joint;
//				}
//			});
//			for (int a = 0; a < jointAuthorNames.length; a++)
//				data.addAnnotation("authorName", jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size()).copyAttributes(jointAuthorNames[a]);
//		} while (jointAuthorNames.length != 0);
//		
//		
//		//	annotate tokens that can separate author names in an enumeration
//		Annotation[] authorListSeparators = Gamta.extractAllContained(data, omniFat.getAuthorListSeparators());
//		for (int s = 0; s < authorListSeparators.length; s++)
//			data.addAnnotation("authorListSeparator", authorListSeparators[s].getStartIndex(), authorListSeparators[s].size());
//		Annotation[] authorListEndSeparators = Gamta.extractAllContained(data, omniFat.getAuthorListEndSeparators());
//		for (int s = 0; s < authorListEndSeparators.length; s++)
//			data.addAnnotation("authorListEndSeparator", authorListEndSeparators[s].getStartIndex(), authorListEndSeparators[s].size());
//		
//		
//		//	annotate possible ends of author name enumerations
//		Annotation[] authorListEnds = OmniFatFunctions.joinAdjacent(data, "authorListEndSeparator", "authorName", "authorListEnd", new OmniFatFunctions.JoinTool() {
//			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//				joint.copyAttributes(rightSource);
//				if ("pattern".equals(joint.getAttribute("evidence")))
//					joint.setAttribute("evidence", "joint");
//				
//				String rsid = ((String) rightSource.getAttribute("source"));
//				if (rsid == null)
//					rsid = rightSource.getAnnotationID();
//				joint.setAttribute("source", rsid);
//				
//				return joint;
//			}
//		});
//		for (int s = 0; s < authorListEnds.length; s++)
//			data.addAnnotation("authorListEnd", authorListEnds[s].getStartIndex(), authorListEnds[s].size()).copyAttributes(authorListEnds[s]);
//		
//		//	annotate author name enumerations
//		Annotation[] authorLists = OmniFatFunctions.joinAdjacent(data, "authorName", "authorListEnd", "authorList", new OmniFatFunctions.JoinTool() {
//			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//				joint.copyAttributes(rightSource);
//				if ("pattern".equals(joint.getAttribute("evidence")))
//					joint.setAttribute("evidence", "joint");
//				
//				String lsid = ((String) leftSource.getAttribute("source"));
//				if (lsid == null)
//					lsid = leftSource.getAnnotationID();
//				String rsid = ((String) rightSource.getAttribute("source"));
//				if (rsid == null)
//					rsid = rightSource.getAnnotationID();
//				String source = (lsid + "," + rsid);
//				joint.setAttribute("source", source);
//				
//				return joint;
//			}
//		});
//		for (int s = 0; s < authorLists.length; s++)
//			data.addAnnotation("authorList", authorLists[s].getStartIndex(), authorLists[s].size()).copyAttributes(authorLists[s]);
//		
//		
//		//	annotate author name enumeration parts
//		Annotation[] authorListParts;
//		authorListParts = OmniFatFunctions.joinAdjacent(data, "authorName", "authorListSeparator", "authorListPart", new OmniFatFunctions.JoinTool() {
//			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//				joint.copyAttributes(rightSource);
//				if ("pattern".equals(joint.getAttribute("evidence")))
//					joint.setAttribute("evidence", "joint");
//				
//				String lsid = ((String) leftSource.getAttribute("source"));
//				if (lsid == null)
//					lsid = leftSource.getAnnotationID();
//				joint.setAttribute("source", lsid);
//				
//				return joint;
//			}
//		});
//		for (int s = 0; s < authorListParts.length; s++)
//			data.addAnnotation("authorListPart", authorListParts[s].getStartIndex(), authorListParts[s].size()).copyAttributes(authorListParts[s]);
//		
//		authorListParts = OmniFatFunctions.joinAdjacent(data, "authorName", "authorListEndSeparator", "authorListPart", new OmniFatFunctions.JoinTool() {
//			public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//				joint.copyAttributes(rightSource);
//				if ("pattern".equals(joint.getAttribute("evidence")))
//					joint.setAttribute("evidence", "joint");
//				
//				String lsid = ((String) leftSource.getAttribute("source"));
//				if (lsid == null)
//					lsid = leftSource.getAnnotationID();
//				joint.setAttribute("source", lsid);
//				
//				return joint;
//			}
//		});
//		for (int s = 0; s < authorListParts.length; s++)
//			data.addAnnotation("authorListPart", authorListParts[s].getStartIndex(), authorListParts[s].size()).copyAttributes(authorListParts[s]);
//		
//		
//		//	join author name enumerations
//		do {
//			authorLists = OmniFatFunctions.joinAdjacent(data, "authorListPart", "authorList", "authorList", new OmniFatFunctions.JoinTool() {
//				public Annotation getJointAnnotation(Annotation leftSource, Annotation rightSource, Annotation joint) {
//					joint.copyAttributes(rightSource);
//					if ("pattern".equals(joint.getAttribute("evidence")))
//						joint.setAttribute("evidence", "joint");
//					
//					String lsid = ((String) leftSource.getAttribute("source"));
//					if (lsid == null)
//						lsid = leftSource.getAnnotationID();
//					String rsid = ((String) rightSource.getAttribute("source"));
//					if (rsid == null)
//						rsid = rightSource.getAnnotationID();
//					String source = (lsid + "," + rsid);
//					joint.setAttribute("source", source);
//					
//					return joint;
//				}
//			});
//			for (int s = 0; s < authorLists.length; s++)
//				data.addAnnotation("authorList", authorLists[s].getStartIndex(), authorLists[s].size()).copyAttributes(authorLists[s]);
//		} while (authorLists.length != 0);
//		
//		
//		//	clean up
//		AnnotationFilter.removeAnnotations(data, "authorListSeparator");
//		AnnotationFilter.removeAnnotations(data, "authorListEndSeparator");
//		AnnotationFilter.removeAnnotations(data, "authorListPart");
//		AnnotationFilter.removeAnnotations(data, "authorListEnd");
//		
//		//	treat author name lists as single authors
//		AnnotationFilter.renameAnnotations(data, "authorList", "authorName");
	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		
//		//	prepare filters
//		Set lexiconMatchKeys = new HashSet();
//		
//		//	annotate author names from lexicon
//		Annotation[] authorNames = Gamta.extractAllContained(data, OmniFAT.getAuthorDictionary(), 1, true);
//		for (int a = 0; a < authorNames.length; a++) {
//			String authorNameKey = this.getKey(authorNames[a]);
//			
//			//	remember lexicon match
//			lexiconMatchKeys.add(authorNameKey);
//			
//			//	ignore single letters
//			if (authorNames[a].length() < 2)
//				continue;
//			
//			Annotation authorName = data.addAnnotation("authorName", authorNames[a].getStartIndex(), authorNames[a].size());
//			authorName.setAttribute("evidence", "lexicon");
//			authorName.setAttribute("value", authorNames[a].getValue());
//		}
//		
//		//	annotate author names from patterns
//		String[] authorNamePatterns = OmniFAT.getAuthorNamePatterns();
//		
//		//	avoid duplicates
//		Set matchKeys = new HashSet();
//		
//		//	apply patterns
//		for (int p = 0; p < authorNamePatterns.length; p++) {
//			authorNames = Gamta.extractAllMatches(data, authorNamePatterns[p], 0, false, false, true);
//			for (int a = 0; a < authorNames.length; a++) {
//				String authorNameKey = this.getKey(authorNames[a]);
//				
//				//	we've had this one already
//				if (!matchKeys.add(authorNameKey))
//					continue;
//				
//				//	do not duplicate lexicon matches (their rank group is clear)
//				if (lexiconMatchKeys.contains(authorNameKey))
//					continue;
//				
//				//	ignore single letters
//				if (authorNames[a].length() < 2)
//					continue;
//				
//				Annotation authorName = data.addAnnotation("authorName", authorNames[a].getStartIndex(), authorNames[a].size());
//				authorName.setAttribute("evidence", "pattern");
//				authorName.setAttribute("value", authorNames[a].getValue());
//			}
//		}
//		
//		
//		//	annotate author name stop words from lexicon
//		Annotation[] authorNameStopWords = Gamta.extractAllContained(data, OmniFAT.getAuthorNameStopWords(), 1, true);
//		for (int a = 0; a < authorNameStopWords.length; a++) {
//			Annotation authorNameStopWord = data.addAnnotation("authorNameStopWord", authorNameStopWords[a].getStartIndex(), authorNameStopWords[a].size());
//			authorNameStopWord.setAttribute("evidence", "lexicon");
//			authorNameStopWord.setAttribute("value", authorNameStopWords[a].getValue());
//		}
//		
//		//	join adjacent stop words
//		Annotation[] jointAuthorNameStopWords;
//		do {
//			jointAuthorNameStopWords = OmniFAT.joinAdjacent(data, "authorNameStopWord", "authorNameStopWord");
//			for (int a = 0; a < jointAuthorNameStopWords.length; a++) {
//				Annotation jointAuthorNameStopWord = data.addAnnotation("authorNameStopWord", jointAuthorNameStopWords[a].getStartIndex(), jointAuthorNameStopWords[a].size());
//				jointAuthorNameStopWord.setAttribute("evidence", "joint");
//			}
//		} while (jointAuthorNameStopWords.length != 0);
//		
//		
//		//	join author names with stop words
//		Annotation[] jointAuthorNames;
//		do {
//			jointAuthorNames = OmniFAT.joinAdjacent(data, "authorNameStopWord", "authorName", "authorName");
//			for (int a = 0; a < jointAuthorNames.length; a++) {
//				QueriableAnnotation jointAuthorName = data.addAnnotation("authorName", jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size());
//				Annotation[] authorName = jointAuthorName.getAnnotations("authorName");
//				if (authorName.length != 0) {
//					if ("pattern".equals(authorName[0].getAttribute("evidence")))
//						jointAuthorName.setAttribute("evidence", "joint");
//					else jointAuthorName.setAttribute("evidence", authorName[0].getAttribute("evidence"));
//				}
//			}
//		} while (jointAuthorNames.length != 0);
//		
//		
//		//	annotate author initials from patterns
//		String[] authorInitialPatterns = OmniFAT.getAuthorInitialPatterns();
//		
//		//	apply patterns for initials
//		for (int p = 0; p < authorInitialPatterns.length; p++) {
//			Annotation[] authorInitials = Gamta.extractAllMatches(data, authorInitialPatterns[p], 2, false, false, true);
//			for (int a = 0; a < authorInitials.length; a++) {
//				String authorInitialKey = this.getKey(authorInitials[a]);
//				
//				//	we've had this one already
//				if (!matchKeys.add(authorInitialKey))
//					continue;
//				
//				//	do not duplicate lexicon matches (their rank group is clear)
//				if (lexiconMatchKeys.contains(authorInitialKey))
//					continue;
//				
//				Annotation authorInitial = data.addAnnotation("authorInitial", authorInitials[a].getStartIndex(), authorInitials[a].size());
//				authorInitial.setAttribute("evidence", "pattern");
//				authorInitial.setAttribute("value", authorInitials[a].getValue());
//			}
//		}
//		
//		
//		//	join author names with initials
//		do {
//			jointAuthorNames = OmniFAT.joinAdjacent(data, "authorInitial", "authorName", "authorName");
//			for (int a = 0; a < jointAuthorNames.length; a++) {
//				QueriableAnnotation jointAuthorName = data.addAnnotation("authorName", jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size());
//				Annotation[] authorName = jointAuthorName.getAnnotations("authorName");
//				if (authorName.length != 0) {
//					if ("pattern".equals(authorName[0].getAttribute("evidence")))
//						jointAuthorName.setAttribute("evidence", "initials");
//					else jointAuthorName.setAttribute("evidence", authorName[0].getAttribute("evidence"));
//				}
//			}
//		} while (jointAuthorNames.length != 0);
//		
//		
//		//	join author names with author names
//		do {
//			jointAuthorNames = OmniFAT.joinAdjacent(data, "authorName", "authorName");
//			for (int a = 0; a < jointAuthorNames.length; a++) {
//				QueriableAnnotation jointAuthorName = data.addAnnotation("authorName", jointAuthorNames[a].getStartIndex(), jointAuthorNames[a].size());
//				Annotation[] authorName = jointAuthorName.getAnnotations("authorName");
//				String evidence = "pattern";
//				for (int n = 0; (n < authorName.length) && "pattern".equals(evidence); n++)
//					evidence = ((String) authorName[n].getAttribute("evidence", evidence));
//				if ("pattern".equals(evidence))
//					jointAuthorName.setAttribute("evidence", "joint");
//				else jointAuthorName.setAttribute("evidence", evidence);
//			}
//		} while (jointAuthorNames.length != 0);
//		
//		
//		//	annotate tokens that can separate author names in an enumeration
//		Annotation[] authorListSeparators = Gamta.extractAllContained(data, OmniFAT.getAuthorListSeparators());
//		for (int s = 0; s < authorListSeparators.length; s++)
//			data.addAnnotation("authorListSeparator", authorListSeparators[s].getStartIndex(), authorListSeparators[s].size());
//		Annotation[] authorListEndSeparators = Gamta.extractAllContained(data, OmniFAT.getAuthorListEndSeparators());
//		for (int s = 0; s < authorListEndSeparators.length; s++)
//			data.addAnnotation("authorListEndSeparator", authorListEndSeparators[s].getStartIndex(), authorListEndSeparators[s].size());
//		
//		//	annotate possible ends of author name enumerations
//		Annotation[] authorListEnds = OmniFAT.joinAdjacent(data, "authorListEndSeparator", "authorName", "authorListEnd");
//		for (int s = 0; s < authorListEnds.length; s++)
//			data.addAnnotation("authorListEnd", authorListEnds[s].getStartIndex(), authorListEnds[s].size());
//		
//		//	annotate author name enumerations
//		Annotation[] authorLists = OmniFAT.joinAdjacent(data, "authorName", "authorListEnd", "authorList");
//		for (int s = 0; s < authorLists.length; s++)
//			data.addAnnotation("authorList", authorLists[s].getStartIndex(), authorLists[s].size());
//		
//		//	annotate author name enumeration parts
//		Annotation[] authorListParts;
//		authorListParts = OmniFAT.joinAdjacent(data, "authorName", "authorListSeparator", "authorListPart");
//		for (int s = 0; s < authorListParts.length; s++)
//			data.addAnnotation("authorListPart", authorListParts[s].getStartIndex(), authorListParts[s].size());
//		authorListParts = OmniFAT.joinAdjacent(data, "authorName", "authorListEndSeparator", "authorListPart");
//		for (int s = 0; s < authorListParts.length; s++)
//			data.addAnnotation("authorListPart", authorListParts[s].getStartIndex(), authorListParts[s].size());
//		
//		//	join author name enumerations
//		do {
//			authorLists = OmniFAT.joinAdjacent(data, "authorListPart", "authorList", "authorList");
//			for (int s = 0; s < authorLists.length; s++)
//				data.addAnnotation("authorList", authorLists[s].getStartIndex(), authorLists[s].size());
//		} while (authorLists.length != 0);
//		
//		//	clean up
//		AnnotationFilter.removeAnnotations(data, "authorListSeparator");
//		AnnotationFilter.removeAnnotations(data, "authorListEndSeparator");
//		AnnotationFilter.removeAnnotations(data, "authorListPart");
//		AnnotationFilter.removeAnnotations(data, "authorListEnd");
//		
//		//	treat author name lists as single authors
//		AnnotationFilter.renameAnnotations(data, "authorList", "authorName");
//		
//		
//		//	mark author names in brackets as authors
//		Annotation[] openingBrackets = Gamta.extractAllMatches(data, "\\(", 1);
//		for (int b = 0; b < openingBrackets.length; b++)
//			data.addAnnotation("openingBracket", openingBrackets[b].getStartIndex(), openingBrackets[b].size());
//		Annotation[] closingBrackets = Gamta.extractAllMatches(data, "\\)", 1);
//		for (int b = 0; b < closingBrackets.length; b++)
//			data.addAnnotation("closingBracket", closingBrackets[b].getStartIndex(), closingBrackets[b].size());
//		
//		authorNames = OmniFAT.joinAdjacent(data, "openingBracket", "authorName", "openingAuthorName");
//		for (int a = 0; a < authorNames.length; a++)
//			data.addAnnotation("openingAuthorName", authorNames[a].getStartIndex(), authorNames[a].size());
//		authorNames = OmniFAT.joinAdjacent(data, "openingAuthorName", "closingBracket", "authorName");
//		for (int a = 0; a < authorNames.length; a++)
//			data.addAnnotation("authorName", authorNames[a].getStartIndex(), authorNames[a].size());
//		
//		AnnotationFilter.removeAnnotations(data, "openingBracket");
//		AnnotationFilter.removeAnnotations(data, "closingBracket");
//		AnnotationFilter.removeAnnotations(data, "openingAuthorName");
//	}
}