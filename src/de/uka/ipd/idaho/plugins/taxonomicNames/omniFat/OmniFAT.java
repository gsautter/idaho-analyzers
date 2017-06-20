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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * Data handling part of the OniFAT algorithm.
 * 
 * @author sautter
 */
public class OmniFAT {
	
	/**
	 * The base of instances, shared between equal data providers
	 * 
	 * @author sautter
	 */
	public static class Base {
		private AnalyzerDataProvider adp = null;
		
		static final String DEFAULT_INSTANCE_NAME_SETTING = "DefaultInstanceName";
		private String defaultInstanceName = DEFAULT_INSTANCE_NAME;
		
		private HashMap dictionaryCache = new HashMap();
		
		private TreeMap instances = new TreeMap();
		
		private Base(AnalyzerDataProvider adp) {
			this.adp = adp;
			Properties params = this.loadParameters();
			defaultInstanceName = params.getProperty(DEFAULT_INSTANCE_NAME_SETTING, defaultInstanceName);
		}
		
		void discartCachedDictionary(String dictionaryName) {
			this.dictionaryCache.remove(dictionaryName);
		}
		
		private OmniFatDictionary getDictionary(String name, boolean trusted) {
			OmniFatDictionary ofd = ((OmniFatDictionary) this.dictionaryCache.get(name));
			if (ofd == null) {
				ofd = this.loadDictionary(name, trusted);
				if (ofd != null)
					this.dictionaryCache.put(name, ofd);
			}
			return ofd;
		}
		
		public String getDefaultInstanceName() {
			return this.defaultInstanceName;
		}
		
		public void setDefaultInstanceName(String defaultInstanceName) {
			this.defaultInstanceName = defaultInstanceName;
		}
		
		public String[] getInstanceNames() {
			String[] dataNames = this.adp.getDataNames();
			TreeSet instanceNames = new TreeSet();
			for (int d = 0; d < dataNames.length; d++) {
				if (dataNames[d].endsWith(INSTANCE_FILE_NAME_SUFFIX))
					instanceNames.add(dataNames[d].substring(0, (dataNames[d].length() - INSTANCE_FILE_NAME_SUFFIX.length())));
			}
			return ((String[]) instanceNames.toArray(new String[instanceNames.size()]));
		}
		
		void discartCachedInstance(String instanceName) {
			this.instances.remove(instanceName);
			OmniFatAnalyzer.dropInstanceBasedDataSets(this, instanceName);
		}
		
		public boolean isInstanceLoaded(String instanceName) {
			return this.instances.containsKey(instanceName);
		}
		
		public OmniFAT getDefaultInstance() {
			return this.getInstance(this.defaultInstanceName);
		}
		
		private OmniFAT getOmniFatInstance(Properties parameters) {
			String instanceName = parameters.getProperty("OmniFatInstanceName");
			return this.getOmniFatInstance(instanceName);
		}
		
		private OmniFAT getOmniFatInstance(String instanceName) {
			return ((instanceName == null) ? getDefaultInstance() : getInstance(instanceName));
		}
		
		public void doOmniFAT(MutableAnnotation data, Properties parameters) {
			OmniFAT omniFat = this.getOmniFatInstance(parameters);
			if (omniFat != null)
				OmniFAT.doOmniFAT(data, new DocumentDataSet(omniFat));
		}
		
		public DocumentDataSet doOmniFAT(MutableAnnotation data) {
			OmniFAT omniFat = this.getOmniFatInstance((String) null);
			return OmniFAT.doOmniFAT(data, new DocumentDataSet(omniFat));
		}
		
		public DocumentDataSet doOmniFAT(MutableAnnotation data, String instanceName) {
			OmniFAT omniFat = getOmniFatInstance(instanceName);
			return ((omniFat == null) ? null : OmniFAT.doOmniFAT(data, new DocumentDataSet(omniFat)));
		}
		
		public OmniFAT getInstance(String name) {
			
			//	do cache lookup
			OmniFAT omniFat = ((OmniFAT) this.instances.get(name));
			if (omniFat != null)
				return omniFat;
			
			//	load base config file
			try {
				Reader reader = new InputStreamReader(this.adp.getInputStream(name + INSTANCE_FILE_NAME_SUFFIX), "UTF-8");
				Parser p = new Parser();
				TreeNode configNode = p.parse(reader);
				if (TreeNode.ROOT_NODE_TYPE.equals(configNode.getNodeType()))
					configNode = configNode.getChildNode("omniFat", 0);
				reader.close();
				return this.getInstance(name, configNode);
			}
			catch (IOException ioe) {
				System.out.println("Exception loading base config file: " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				return null;
			}
		}
		
		public OmniFAT getInstance(String name, TreeNode configNode) {
			
			//	create new instance
			OmniFAT omniFat = new OmniFAT(name, this.adp);
			
			//	load description
			TreeNode[] descriptionNodes = configNode.getChildNodes("description");
			if (descriptionNodes.length != 0) {
				TreeNode descriptionNode = descriptionNodes[0].getChildNode(TreeNode.DATA_NODE_TYPE, 0);
				if (descriptionNode != null)
					omniFat.decsription = descriptionNode.getNodeValue();
			}
			
			//	load fuzzy match threshold
			TreeNode[] fuzzyMatchNodes = configNode.getChildNodes("fuzzyMatch");
			if (fuzzyMatchNodes.length == 1)
				omniFat.fuzzyMatchThreshold = Integer.parseInt(fuzzyMatchNodes[0].getAttribute("threshold", ("" + omniFat.fuzzyMatchThreshold)));
			
			//	load abbreviation length boundaries
			TreeNode[] abbreviationLengthInLettersNodes = configNode.getChildNodes("abbreviationLengthInLetters");
			if (abbreviationLengthInLettersNodes.length == 1) {
				omniFat.minAbbreviationLength = Integer.parseInt(abbreviationLengthInLettersNodes[0].getAttribute("min", ("" + omniFat.minAbbreviationLength)));
				omniFat.maxAbbreviationLength = Integer.parseInt(abbreviationLengthInLettersNodes[0].getAttribute("max", ("" + omniFat.maxAbbreviationLength)));
				omniFat.unDottedAbbreviations = "true".equals(abbreviationLengthInLettersNodes[0].getAttribute("unDotted", ("" + omniFat.unDottedAbbreviations)));
			}
			
			//	load new epithet labels
			TreeNode[] newEpithetLabelExtensionNodes = configNode.getChildNodes("newEpithetLabels");
			StringVector newEpithetLabelExtensionList = new StringVector();
			for (int nl = 0; nl < newEpithetLabelExtensionNodes.length; nl++) {
				String[] newEpithetLabelExtensions = loadInternalList(newEpithetLabelExtensionNodes[nl]);
				newEpithetLabelExtensionList.addContentIgnoreDuplicates(newEpithetLabelExtensions);
			}
			
			
			//	load rank groups
			TreeNode[] rankGroupNodes = configNode.getChildNodes("rankGroup");
			int firstOrderNumber = TreeTools.getAllNodesOfType(configNode, "rank").length;
			ArrayList rankGroups = new ArrayList();
			ArrayList ranks = new ArrayList();
			boolean suffixDiff = false;
			for (int rg = 0; rg < rankGroupNodes.length; rg++) {
				RankGroup rankGroup = loadRankGroup(rankGroupNodes[rg], firstOrderNumber, newEpithetLabelExtensionList, omniFat);
				if (rankGroup != null) {
					rankGroups.add(rankGroup);
					Rank[] rankGroupRanks = rankGroup.getRanks();
					firstOrderNumber -= rankGroupRanks.length;
					for (int r = 0; r < rankGroupRanks.length; r++)
						ranks.add(rankGroupRanks[r]);
					suffixDiff = (suffixDiff || rankGroup.doSuffixDiff());
				}
			}
			omniFat.rankGroups = ((RankGroup[]) rankGroups.toArray(new RankGroup[rankGroups.size()]));
			omniFat.ranks = ((Rank[]) ranks.toArray(new Rank[ranks.size()]));
			
			
			//	load author data
			TreeNode authorDataNode = configNode.getChildNode("authors", 0);
			if (authorDataNode != null)
				loadAuthorData(authorDataNode, omniFat);
			
			
			//	load data rules
			TreeNode ruleNode = configNode.getChildNode("rules", 0);
			if (ruleNode != null) {
				omniFat.cleanUpAfterDataRules = "true".equals(ruleNode.getAttribute("cleanupAfter", "true"));
				loadDataRules(ruleNode, omniFat);
			}
			
			
			//	load negative & stop word dictionaries
			TreeNode[] negativeDictionaryNodes = configNode.getChildNodes("dictionary");
			ArrayList negativeDictionaries = new ArrayList();
			CompiledDictionary stopWords = new CompiledDictionary("stopWords", true);
			Map suffixDiffThresholds = new HashMap();
			for (int nd = 0; nd < negativeDictionaryNodes.length; nd++) {
				String ndName = negativeDictionaryNodes[nd].getAttribute("ref");
				
				//	load stop word list
				if ("stopWord".equals(negativeDictionaryNodes[nd].getAttribute("type"))) {
					try {
						BufferedReader stopWordReader = new BufferedReader(new InputStreamReader(this.adp.getInputStream(ndName), "UTF-8"));
						String stopWord;
						while ((stopWord = stopWordReader.readLine()) != null) {
							stopWord = stopWord.trim();
							if ((stopWord.length() != 0) && !stopWord.startsWith("//"))
								stopWords.add(stopWord);
						}
						stopWordReader.close();
						System.out.println("Loaded stop word file " + ndName);
					}
					catch (IOException ioe) {
						System.out.println("Exception loading stop word file " + ndName + ": " + ioe.getMessage());
						ioe.printStackTrace(System.out);
					}
				}
				
				//	load negative dictionary
				else if ("negative".equals(negativeDictionaryNodes[nd].getAttribute("type"))) {
					OmniFatDictionary negativeDictionary = this.getDictionary(ndName, "true".equals(negativeDictionaryNodes[nd].getAttribute("trusted", "false")));
					if (negativeDictionary != null) {
						negativeDictionaries.add(negativeDictionary);
						int suffixDiffThreshold = Integer.parseInt(negativeDictionaryNodes[nd].getAttribute("suffixDiffThreshold", "0"));
						if (suffixDiffThreshold > 0)
							suffixDiffThresholds.put(negativeDictionary.name, new Integer(suffixDiffThreshold));
					}
				}
			}
			for (int r = 0; r < omniFat.ranks.length; r++)
				for (int l = 0; l < omniFat.ranks[r].epithetLabels.size(); l++) {
					String label = omniFat.ranks[r].epithetLabels.get(l);
					if (!label.endsWith(".") && (label.length() > 2))
						stopWords.add(label);
				}
			stopWords.compile();
			omniFat.stopWords = stopWords;
			omniFat.negativeDictionaries = ((OmniFatDictionary[]) negativeDictionaries.toArray(new OmniFatDictionary[negativeDictionaries.size()]));
			omniFat.negativeDictionary = this.loadExtensibleDictionary((omniFat.name + ".negative"), true);
			if (omniFat.negativeDictionary == null)
				omniFat.negativeDictionary = new ExtensibleDictionary((omniFat.name + ".negative"), true);
			
			
			//	load negative patterns
			TreeNode[] negativePatternNodes = configNode.getChildNodes("pattern");
			HashSet negativePatterns = new HashSet();
			for (int np = 0; np < negativePatternNodes.length; np++) {
				String npString = negativePatternNodes[np].getAttribute("string");
				OmniFatPattern ofp = this.getPattern(npString, "true".equals(negativePatternNodes[np].getAttribute("isRef", "false")));
				if (ofp != null)
					negativePatterns.add(ofp);
			}
			omniFat.negativePatterns = ((OmniFatPattern[]) negativePatterns.toArray(new OmniFatPattern[negativePatterns.size()]));
			
			
			//	load stemming rules
			TreeNode[] stemmingRuleNodes = configNode.getChildNodes("stemmingRule");
			ArrayList stemmingRules = new ArrayList();
			for (int sr = 0; sr < stemmingRuleNodes.length; sr++) {
				String matchEnding = stemmingRuleNodes[sr].getAttribute("matchEnding");
				String[] stemmedEndings = loadInternalList(stemmingRuleNodes[sr]);
				stemmingRules.add(new StemmingRule(matchEnding, stemmedEndings));
			}
			omniFat.stemmingRules = ((StemmingRule[]) stemmingRules.toArray(new StemmingRule[stemmingRules.size()]));
			
			
			//	load intra epithet punctuation
			TreeNode[] intraEpithetPunctuationNodes = configNode.getChildNodes("intraEpithetPunctuation");
			CompiledDictionary intraEpithetPunctuationMarks = new CompiledDictionary("intraEpithetPunctuation", true);
			for (int pn = 0; pn < intraEpithetPunctuationNodes.length; pn++) {
				String[] intraEpithetPunctuation = loadInternalList(intraEpithetPunctuationNodes[pn]);
				for (int p = 0; p < intraEpithetPunctuation.length; p++)
					intraEpithetPunctuationMarks.add(intraEpithetPunctuation[p]);
			}
			intraEpithetPunctuationMarks.compile();
			omniFat.intraEpithetPunctuationMarks = intraEpithetPunctuationMarks;
			
			
			//	load inter epithet punctuation
			TreeNode[] interEpithetPunctuationNodes = configNode.getChildNodes("interEpithetPunctuation");
			CompiledDictionary interEpithetPunctuationMarks = new CompiledDictionary("interEpithetPunctuation", true);
			for (int pn = 0; pn < interEpithetPunctuationNodes.length; pn++) {
				String[] interEpithetPunctuation = loadInternalList(interEpithetPunctuationNodes[pn]);
				for (int p = 0; p < interEpithetPunctuation.length; p++)
					interEpithetPunctuationMarks.add(interEpithetPunctuation[p]);
			}
			interEpithetPunctuationMarks.compile();
			omniFat.interEpithetPunctuationMarks = interEpithetPunctuationMarks;
			
			
			//	prepare suffix diff if necessary & possible
			if (suffixDiff && (suffixDiffThresholds.size() != 0))
				prepareSuffixDiff(omniFat, suffixDiffThresholds);
			
			
			//	finally ...
			this.instances.put(omniFat.getName(), omniFat);
			return omniFat;
		}
		
		private RankGroup loadRankGroup(TreeNode rankGroupNode, int orderNumber, StringVector newEpithetLabelExtensionList, OmniFAT omniFat) {
			
			//	load name
			String rgName = rankGroupNode.getAttribute("name");
			if (rgName == null)
				return null;
			
			//	create rank group
			RankGroup rankGroup = new RankGroup(omniFat, rgName, orderNumber);
			
			//	check epithet capitalization mode
			String rgCapitalized = rankGroupNode.getAttribute("capitalized", "mixed");
			rankGroup.epithetsCapitalized = "always".equals(rgCapitalized);
			rankGroup.epithetsLowerCase = "never".equals(rgCapitalized);
			
			//	check epithet repetitions
			rankGroup.repeatedEpithets = "true".equals(rankGroupNode.getAttribute("repeatedEpithets", "false"));
			
			//	check combinability
			rankGroup.inCombinations = "true".equals(rankGroupNode.getAttribute("inCombinations", "false"));
			
			//	check learning mode
			String rgLearningMode = rankGroupNode.getAttribute("learningMode", "teach");
			rankGroup.isTeachable = "teach".equals(rgLearningMode);
			rankGroup.isAutoLearning = "auto".equals(rgLearningMode);
			
			//	check suffix diff setting & prepare suffixes
			if ("true".equals(rankGroupNode.getAttribute("doSuffixDiff", "false")))
				rankGroup.suffixDiffData = new TreeSet();
			
			//	load patterns
			TreeNode[] patternNodes = rankGroupNode.getChildNodes("pattern");
			HashSet precisionPatterns = new HashSet();
			HashSet recallPatterns = new HashSet();
			HashSet negativePatterns = new HashSet();
			for (int p = 0; p < patternNodes.length; p++) {
				String pString = patternNodes[p].getAttribute("string");
				
				OmniFatPattern ofp = this.getPattern(pString, "true".equals(patternNodes[p].getAttribute("isRef", "false")));
				if (ofp == null) continue;
				
				String pType = patternNodes[p].getAttribute("type");
				if ("precision".equals(pType))
					precisionPatterns.add(ofp);
				else if ("recall".equals(pType))
					recallPatterns.add(ofp);
				else if ("negative".equals(pType))
					negativePatterns.add(ofp);
			}
			((EpithetGroup) rankGroup).precisionPatterns = ((OmniFatPattern[]) precisionPatterns.toArray(new OmniFatPattern[precisionPatterns.size()]));
			((EpithetGroup) rankGroup).recallPatterns = ((OmniFatPattern[]) recallPatterns.toArray(new OmniFatPattern[recallPatterns.size()]));
			((EpithetGroup) rankGroup).negativePatterns = ((OmniFatPattern[]) negativePatterns.toArray(new OmniFatPattern[negativePatterns.size()]));
			
			//	load dictionaries
			TreeNode[] dictionaryNodes = rankGroupNode.getChildNodes("dictionary");
			ArrayList positiveDictionaries = new ArrayList();
			ArrayList negativeDictionaries = new ArrayList();
			for (int d = 0; d < dictionaryNodes.length; d++) {
				String dName = dictionaryNodes[d].getAttribute("ref");
				OmniFatDictionary dictionary = this.getDictionary(dName, "true".equals(dictionaryNodes[d].getAttribute("trusted", "false")));
				
				if (dictionary == null) continue;
				
				String dType = dictionaryNodes[d].getAttribute("type");
				
				if ("positive".equals(dType))
					positiveDictionaries.add(dictionary);
				else if ("negative".equals(dType))
					negativeDictionaries.add(dictionary);
			}
			((EpithetGroup) rankGroup).positiveDictionaries = ((OmniFatDictionary[]) positiveDictionaries.toArray(new OmniFatDictionary[positiveDictionaries.size()]));
			((EpithetGroup) rankGroup).negativeDictionaries = ((OmniFatDictionary[]) negativeDictionaries.toArray(new OmniFatDictionary[negativeDictionaries.size()]));
			
			//	load learned epithets
			rankGroup.epithetDictionary = this.loadExtensibleDictionary((omniFat.name + "." + rgName + ".group"), true);
			if (rankGroup.epithetDictionary == null)
				rankGroup.epithetDictionary = new ExtensibleDictionary((omniFat.name + "." + rgName + ".group"), true);
//			rankGroup.positiveDictionary = loadDictionary((omniFat.name + "." + rgName + ".group"), true, adp);
//			if (rankGroup.positiveDictionary == null)
//				rankGroup.positiveDictionary = new CompiledDictionary((omniFat.name + "." + rgName + ".group"), true);
//			rankGroup.negativeDictionary = loadDictionary((omniFat.name + "." + rgName + ".group"), true, adp);
//			if (rankGroup.negativeDictionary == null)
//				rankGroup.negativeDictionary = new CompiledDictionary((omniFat.name + "." + rgName + ".group"), true);
			
			//	add ranks
			TreeNode[] rankNodes = rankGroupNode.getChildNodes("rank");
			for (int r = 0; r < rankNodes.length; r++) {
				Rank rank = loadRank(rankGroup, rankNodes[r], orderNumber, newEpithetLabelExtensionList, omniFat);
				if (rank != null) {
					rankGroup.addRank(rank);
					orderNumber--;
				}
			}
			
			//	finally ...
			return (rankGroup.ranks.isEmpty() ? null : rankGroup);
		}
		
		private Rank loadRank(RankGroup rankGroup, TreeNode rankNode, int orderNumber, StringVector newEpithetLabelExtensionList, OmniFAT omniFat) {
			
			//	load name
			String rName = rankNode.getAttribute("name");
			if (rName == null)
				return null;
			
			//	create rank group
			Rank rank = new Rank(omniFat, rName, orderNumber, rankGroup);
			
			//	check epithet labeling mode
			String rLabeled = rankNode.getAttribute("labeled");
			rank.epithetsLabeled = "always".equals(rLabeled);
			rank.epithetsUnlabeled = "never".equals(rLabeled);
			
			//	check if rank is required if epithets above and below this rank are present in a taxon name
			rank.required = "true".equals(rankNode.getAttribute("required", "false"));
			
			//	load rank probability
			rank.probability = Integer.parseInt(rankNode.getAttribute("probability", "5"));
			
			//	load epithet labels
			TreeNode[] labelNodes;
			
			//	individual labels ...
			labelNodes = rankNode.getChildNodes("label");
			for (int l = 0; l < labelNodes.length; l++) {
				String label = labelNodes[l].getAttribute("value");
				if (label != null)
					rank.epithetLabels.addElementIgnoreDuplicates(label);
			}
			
			//	... and label lists
			labelNodes = rankNode.getChildNodes("labels");
			for (int l = 0; l < labelNodes.length; l++) {
				String[] labels = loadInternalList(labelNodes[l]);
				if (labels != null)
					rank.epithetLabels.addContentIgnoreDuplicates(labels);
			}
			
			//	produce new epithet labels
			for (int l = 0; l < rank.epithetLabels.size(); l++)
				for (int n = 0; n < newEpithetLabelExtensionList.size(); n++) {
					rank.newEpithetLabels.addElementIgnoreDuplicates(rank.epithetLabels.get(l) + " " + newEpithetLabelExtensionList.get(n));
					rank.newEpithetLabels.addElementIgnoreDuplicates(newEpithetLabelExtensionList.get(n) + " " + rank.epithetLabels.get(l));
				}
			
			//	read epithet display pattern
			rank.epithetDisplayPattern = rankNode.getAttribute("epithetDisplayPattern", "@epithet");
			
			//	load patterns
			TreeNode[] patternNodes = rankNode.getChildNodes("pattern");
			HashSet precisionPatterns = new HashSet();
			HashSet recallPatterns = new HashSet();
			HashSet negativePatterns = new HashSet();
			for (int p = 0; p < patternNodes.length; p++) {
				String pString = patternNodes[p].getAttribute("string");
				
				OmniFatPattern ofp = this.getPattern(pString, "true".equals(patternNodes[p].getAttribute("isRef", "false")));
				if (ofp == null) continue;
				
				String pType = patternNodes[p].getAttribute("type");
				if ("precision".equals(pType))
					precisionPatterns.add(ofp);
				else if ("recall".equals(pType))
					recallPatterns.add(ofp);
				else if ("negative".equals(pType))
					negativePatterns.add(ofp);
			}
			((EpithetGroup) rank).precisionPatterns = ((OmniFatPattern[]) precisionPatterns.toArray(new OmniFatPattern[precisionPatterns.size()]));
			((EpithetGroup) rank).recallPatterns = ((OmniFatPattern[]) recallPatterns.toArray(new OmniFatPattern[recallPatterns.size()]));
			((EpithetGroup) rank).negativePatterns = ((OmniFatPattern[]) negativePatterns.toArray(new OmniFatPattern[negativePatterns.size()]));
			
			//	load dictionaries
			TreeNode[] dictionaryNodes = rankNode.getChildNodes("dictionary");
			ArrayList positiveDictionaries = new ArrayList();
			ArrayList negativeDictionaries = new ArrayList();
			for (int d = 0; d < dictionaryNodes.length; d++) {
				String dName = dictionaryNodes[d].getAttribute("ref");
				OmniFatDictionary dictionary = this.getDictionary(dName, "true".equals(dictionaryNodes[d].getAttribute("trusted", "false")));
				
				if (dictionary == null) continue;
				
				String dType = dictionaryNodes[d].getAttribute("type");
				
				if ("positive".equals(dType))
					positiveDictionaries.add(dictionary);
				else if ("negative".equals(dType))
					negativeDictionaries.add(dictionary);
			}
			((EpithetGroup) rank).positiveDictionaries = ((OmniFatDictionary[]) positiveDictionaries.toArray(new OmniFatDictionary[positiveDictionaries.size()]));
			((EpithetGroup) rank).negativeDictionaries = ((OmniFatDictionary[]) negativeDictionaries.toArray(new OmniFatDictionary[negativeDictionaries.size()]));
			
//			WE ARE NOT LEARNING RANKS, ONLY GROUPS - TO PREVENT TROUBLE IN REVISIONS
//			
//			//	load learned epithets
//			((EpithetGroup) rank).positiveDictionary = loadDictionary((rName + LEXICON_NAME_EXTENSION), true, adp);
//			if (((EpithetGroup) rank).positiveDictionary == null)
//				((EpithetGroup) rank).positiveDictionary = new CompiledDictionary((rName + LEXICON_NAME_EXTENSION), true);
//			((EpithetGroup) rank).negativeDictionary = loadDictionary((rName + NOT_LEXICON_NAME_EXTENSION), true, adp);
//			if (((EpithetGroup) rank).negativeDictionary == null)
//				((EpithetGroup) rank).negativeDictionary = new CompiledDictionary((rName + NOT_LEXICON_NAME_EXTENSION), true);
			
			//	finally ...
			return rank;
		}
		
		private void loadAuthorData(TreeNode authorDataNode, OmniFAT omniFat) {
			
			//	load parameters
			omniFat.embeddedAuthorNames = "true".equals(authorDataNode.getAttribute("allowEmbedded", "true"));
			
			//	load dictionaries
			TreeNode[] dictionaryNodes = authorDataNode.getChildNodes("dictionary");
			ArrayList positiveDictionaries = new ArrayList();
			for (int d = 0; d < dictionaryNodes.length; d++) {
				String dName = dictionaryNodes[d].getAttribute("ref");
				OmniFatDictionary dictionary = this.getDictionary(dName, "true".equals(dictionaryNodes[d].getAttribute("trusted", "false")));
				if (dictionary != null)
					positiveDictionaries.add(dictionary);
			}
			omniFat.authorNameDictionaries = ((OmniFatDictionary[]) positiveDictionaries.toArray(new OmniFatDictionary[positiveDictionaries.size()]));
			
			//	load learned dictionary
			omniFat.authorNameDictionary = this.loadExtensibleDictionary((omniFat.name + ".authors"), true);
			if (omniFat.authorNameDictionary == null)
				omniFat.authorNameDictionary = new ExtensibleDictionary((omniFat.name + ".authors"), true);
			
			//	load patterns
			TreeNode[] patternNodes = authorDataNode.getChildNodes("pattern");
			HashSet authorNamePatterns = new HashSet();
			HashSet authorNamePartPatterns = new HashSet();
			HashSet negativePatterns = new HashSet();
			for (int p = 0; p < patternNodes.length; p++) {
				String pString = patternNodes[p].getAttribute("string");
				
				OmniFatPattern ofp = this.getPattern(pString, "true".equals(patternNodes[p].getAttribute("isRef", "false")));
				if (ofp == null) continue;
				
				String pType = patternNodes[p].getAttribute("type");
				if ("positive".equals(pType))
					authorNamePatterns.add(ofp);
				else if ("part".equals(pType))
					authorNamePartPatterns.add(ofp);
				else if ("negative".equals(pType))
					negativePatterns.add(ofp);
			}
			omniFat.authorNamePatterns = ((OmniFatPattern[]) authorNamePatterns.toArray(new OmniFatPattern[authorNamePatterns.size()]));
			omniFat.authorInitialPatterns = ((OmniFatPattern[]) authorNamePartPatterns.toArray(new OmniFatPattern[authorNamePartPatterns.size()]));
			
			//	load stop words
			StringVector authorNameStopWords = new StringVector();
			TreeNode[] authorNameStopWordNodes = authorDataNode.getChildNodes("nameStopWords");
			for (int sw = 0; sw < authorNameStopWordNodes.length; sw++) {
				String[] stopWords = loadInternalList(authorNameStopWordNodes[sw]);
				if (stopWords != null)
					authorNameStopWords.addContentIgnoreDuplicates(stopWords);
			}
			omniFat.authorNameStopWords = authorNameStopWords;
			
			//	load list end separators
			StringVector authorNameListEndSeparators = new StringVector();
			TreeNode[] authorNameListEndSeparatorNodes = authorDataNode.getChildNodes("nameListEndSeparators");
			for (int les = 0; les < authorNameListEndSeparatorNodes.length; les++) {
				String[] listEndSeparators = loadInternalList(authorNameListEndSeparatorNodes[les]);
				if (listEndSeparators != null)
					authorNameListEndSeparators.addContentIgnoreDuplicates(listEndSeparators);
			}
			omniFat.authorListEndSeparators = authorNameListEndSeparators;
			
			//	load list separators
			StringVector authorNameListSeparators = new StringVector();
			TreeNode[] authorNameListSeparatorNodes = authorDataNode.getChildNodes("nameListSeparators");
			for (int ls = 0; ls < authorNameListSeparatorNodes.length; ls++) {
				String[] listSeparators = loadInternalList(authorNameListSeparatorNodes[ls]);
				if (listSeparators != null)
					authorNameListSeparators.addContentIgnoreDuplicates(listSeparators);
			}
			omniFat.authorListSeparators = authorNameListSeparators;
		}
		
		private void loadDataRules(TreeNode ruleDataNode, OmniFAT omniFat) {
			TreeNode[] ruleSetNodes = ruleDataNode.getChildNodes("ruleSet");
			ArrayList ruleSets = new ArrayList();
			for (int rs = 0; rs < ruleSetNodes.length; rs++) {
				DataRuleSet ruleSet = loadDataRuleSet(ruleSetNodes[rs]);
				if (ruleSet != null)
					ruleSets.add(ruleSet);
			}
			omniFat.dataRuleSets = ((DataRuleSet[]) ruleSets.toArray(new DataRuleSet[ruleSets.size()]));
		}
		
		private DataRuleSet loadDataRuleSet(TreeNode ruleSetNode) {
			TreeNode[] ruleNodes = ruleSetNode.getChildNodes("rule");
			ArrayList rules = new ArrayList();
			for (int r = 0; r < ruleNodes.length; r++) {
				String match = ruleNodes[r].getAttribute("match");
				String action = ruleNodes[r].getAttribute("action");
				if ((match != null) && (action != null)) try {
					rules.add(new DataRule(match, action, "true".equals(ruleNodes[r].getAttribute("removeNested", "false")), "true".equals(ruleNodes[r].getAttribute("likelyMatch", "false"))));
				}
				catch (PatternSyntaxException pse) {
					System.out.println("Invalid match pattern in data rule: " + match);
					pse.printStackTrace(System.out);
				}
			}
//			return (rules.isEmpty() ? null : new DataRuleSet((DataRule[]) rules.toArray(new DataRule[rules.size()])));
			if (rules.isEmpty()) return null;
			
			DataRuleSet drs = new DataRuleSet((DataRule[]) rules.toArray(new DataRule[rules.size()]));
			try {
				drs.maxRounds = Integer.parseInt(ruleSetNode.getAttribute("maxRounds", "0"));
			} catch (NumberFormatException nfe) {}
			return drs;
		}
		
		private CompiledDictionary loadDictionary(String name, boolean trusted) {
			try {
				CompiledDictionary lexicon = new CompiledDictionary(name, trusted);
				
				BufferedReader lexiconReader = null;
				if (name.endsWith(".txt")) {
					String zipName = (name.substring(0, (name.length() - ".txt".length())) + ".zip");
					if (this.adp.isDataAvailable(zipName)) {
						final ZipInputStream zis = new ZipInputStream(this.adp.getInputStream(zipName));
						ZipEntry ze = zis.getNextEntry();
						if (ze == null)
							System.out.println("Zip file is empty");
						
						else if (ze.getName().equals(name)) {
							System.out.println("Loading lexicon from zip file " + zipName);
							lexiconReader = new BufferedReader(new InputStreamReader(zis, "UTF-8")) {
								public void close() throws IOException {
									super.close();
									zis.close();
								}
							};
						}
						else System.out.println("Lexicon not found in zip file");
					}
					else System.out.println("Zip file not available");
				}
				
				if (lexiconReader == null) {
					System.out.println("Loading lexicon from text file " + name);
					lexiconReader = new BufferedReader(new InputStreamReader(this.adp.getInputStream(name), "UTF-8"));
				}
				
				String lexiconEntry;
				while ((lexiconEntry = lexiconReader.readLine()) != null) {
					lexiconEntry = lexiconEntry.trim();
					if ((lexiconEntry.length() != 0) && !lexiconEntry.startsWith("//"))
						lexicon.add(lexiconEntry);
				}
				lexiconReader.close();
				System.out.println("Loaded lexicon file " + name);
				lexicon.compile();
				return lexicon;
			}
			catch (IOException ioe) {
				System.out.println("Exception loading lexicon file " + name + ": " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				return null;
			}
		}
		
		private ExtensibleDictionary loadExtensibleDictionary(String name, boolean trusted) {
			try {
				Dictionary subtractLexicon = this.getDictionary((name + NOT_LEXICON_NAME_EXTENSION), trusted);
				
				CompiledDictionary lexicon = new CompiledDictionary(name, trusted);
				BufferedReader lexiconReader = new BufferedReader(new InputStreamReader(adp.getInputStream(name + LEXICON_NAME_EXTENSION), "UTF-8"));
				String lexiconEntry;
				while ((lexiconEntry = lexiconReader.readLine()) != null) {
					lexiconEntry = lexiconEntry.trim();
					if ((lexiconEntry.length() != 0) && !lexiconEntry.startsWith("//") && ((subtractLexicon == null) || !subtractLexicon.lookup(lexiconEntry, true)))
						lexicon.add(lexiconEntry);
				}
				lexiconReader.close();
				System.out.println("Loaded extensible lexicon " + name);
				lexicon.compile();
				
				return new ExtensibleDictionary(name, true, lexicon);
			}
			catch (IOException ioe) {
				System.out.println("Exception loading extensible lexicon " + name + ": " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				return null;
			}
		}
		
		private String[] loadInternalList(TreeNode listRoot) {
			StringVector listData = new StringVector();
			String separator = listRoot.getAttribute("separator", ";");
			TreeNode dataNode = listRoot.getChildNode(TreeNode.DATA_NODE_TYPE, 0);
			if (dataNode != null)
				listData.parseAndAddElements(dataNode.getNodeValue(), separator);
			return listData.toStringArray();
		}
		
		private HashMap patternCache = new HashMap();
		void discartCachedPattern(String patternName) {
			this.patternCache.remove(patternName);
		}
		
		private OmniFatPattern getPattern(String string, boolean isRef) {
			if (string == null) return null;
			
			OmniFatPattern ofp = ((OmniFatPattern) this.patternCache.get(string));
			if (ofp == null) {
				String pattern = (isRef ? this.loadPatternString(string) : string);
				if (pattern != null) try {
					ofp = new OmniFatPattern(string, pattern);
					this.patternCache.put(string, ofp);
				}
				catch (PatternSyntaxException pse) {
					System.out.println("Exception loading pattern file " + string + ": " + pse.getMessage());
					pse.printStackTrace(System.out);
				}
			}
			return ofp;
		}
		
		private String loadPatternString(String name) {
			try {
				InputStream is = this.adp.getInputStream(name);
				StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
				is.close();
				return RegExUtils.preCompile(RegExUtils.normalizeRegEx(regEx.concatStrings("\n")), this.resolver);
			}
			catch (IOException ioe) {
				System.out.println("Exception loading pattern file " + name + ": " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				return null;
			}
		}
		
		private Properties resolver = new Properties() {
			public String getProperty(String name, String def) {
				try {
					try {
						InputStream is = adp.getInputStream(name + LEXICON_NAME_EXTENSION);
						StringVector load = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
						is.close();
						String orGroup = RegExUtils.produceOrGroup(load, true);
						return orGroup;
					} catch (IOException fnfe) {}
					InputStream is = adp.getInputStream(name + REGEX_NAME_EXTENSION);
					StringVector regEx = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
					is.close();
					return RegExUtils.normalizeRegEx(regEx.concatStrings("\n"));
				}
				catch (IOException ioe) {
					return def;
				}
			}
			public String getProperty(String name) {
				return this.getProperty(name, null);
			}
		};
		
		public void shutdown() {
			HashSet instanceSet = new HashSet(this.instances.values());
			this.instances.clear();
			for (Iterator iit = instanceSet.iterator(); iit.hasNext();)
				((OmniFAT) iit.next()).storeLearnedData();
			storeParameters();
		}
		
		private Properties loadParameters() {
			Properties parameters = new Properties();
			
			//	load lines
			StringVector rawLines = null;
			try {
				rawLines = StringVector.loadList(new InputStreamReader(this.adp.getInputStream(CONFIG_FILE_NAME), "UTF-8"));
			}
			catch (IOException ioe) {
				rawLines = new StringVector();
			}
			
			//	parse lines
			for (int l = 0; l < rawLines.size(); l++) {
				String rawLine = rawLines.get(l);
				String name = null;
				String value = null;
				int splitIndex = rawLine.indexOf(PARAMETER_VALUE_SEPARATOR);
				
				//	split line and decode value
				try {
					name = rawLine.substring(0, splitIndex);
					value = URLDecoder.decode(rawLine.substring(splitIndex + 1), PARAMETER_ENCODING);
				}
				catch (Exception e) {
					name = null;
					value = null;
				}
					
				//	store parameter if valid
				if ((name != null) && (value != null))
					parameters.setProperty(name, value);
			}
			
			//	finally ...
			return parameters;
		}
		private void storeParameters() {
			try {
				StringVector lines = new StringVector();
				lines.addElement(DEFAULT_INSTANCE_NAME_SETTING + PARAMETER_VALUE_SEPARATOR + URLEncoder.encode(defaultInstanceName, PARAMETER_ENCODING));
				OutputStream os = this.adp.getOutputStream(CONFIG_FILE_NAME);
				lines.storeContent(os);
				os.flush();
				os.close();
			} catch (IOException ioe) {}
		}
	}
	
	static final String BASE_EPITHET_TYPE = "baseEpithet";
	static final String ABBREVIATED_EPITHET_TYPE = "abbreviatedEpithet";
	
	static final String EPITHET_LABEL_TYPE = "epithetLabel";
	static final String NEW_EPITHET_LABEL_TYPE = "newEpithetLabel";
	
	static final String AUTHOR_LIST_PART_TYPE = "authorListPart";
	static final String AUTHOR_LIST_TYPE = "authorList";
	static final String AUTHOR_LIST_END_TYPE = "authorListEnd";
	static final String AUTHOR_LIST_END_SEPARATOR_TYPE = "authorListEndSeparator";
	static final String AUTHOR_LIST_SEPARATOR_TYPE = "authorListSeparator";
	static final String AUTHOR_INITIAL_TYPE = "authorInitial";
	static final String AUTHOR_NAME_STOP_WORD_TYPE = "authorNameStopWord";
	static final String AUTHOR_NAME_TYPE = "authorName";
	static final String AUTHORSHIP_YEAR_TYPE = "authorshipYear";
	
	static final String AUTHORITY_TYPE = "authority";
	static final String AUTHORITY_NAME_TYPE = "authorityName";
	static final String AUTHORITY_YEAR_TYPE = "authorityYear";
	
	static final String EPITHET_TYPE = "epithet";
	
	static final String TAXONOMIC_NAME_TYPE = "taxonomicName"; // positive
	static final String TAXONOMIC_NAME_CANDIDATE_TYPE = "taxonomicNameCandidate"; // external candidate for feedback
	static final String TAXON_NAME_CANDIDATE_TYPE = "taxonNameCandidate"; // internal candidate
	
	
	static final String BASE_INDEX_ATTRIBUTE = "baseIndex";
	static final String AUTHOR_ATTRIBUTE = "author";
	static final String SOURCE_ATTRIBUTE = "source";
	static final String STATE_ATTRIBUTE = "state";
	static final String VALUE_ATTRIBUTE = "value";
	static final String STRING_ATTRIBUTE = "string";
	static final String EVIDENCE_DETAIL_ATTRIBUTE = "evidenceDetail";
	static final String EVIDENCE_ATTRIBUTE = "evidence";
	static final String RANK_GROUP_NUMBER_ATTRIBUTE = "rankGroupNumber";
	static final String RANK_GROUP_ATTRIBUTE = "rankGroup";
	static final String RANK_NUMBER_ATTRIBUTE = "rankNumber";
	static final String RANK_ATTRIBUTE = "rank";
	static final String OMNIFAT_INSTANCE_ATTRIBUTE = "_omniFatInstance";
	
	
	static final String ABBREVIATED_STATE = "abbreviated";
	static final String LIKELY_STATE = "likely";
	static final String AMBIGUOUS_STATE = "ambiguous";
	static final String POSITIVE_STATE = "positive";
	static final String NEGATIVE_STATE = "negative";
	static final String UNCERTAIN_STATE = "uncertain";
	
	
	static final String DOCUMENT_EVIDENCE_PREFIX = "document-";
	static final String FUZZY_EVIDENCE_PREFIX = "fuzzy-";
	static final String MORPHOLOGY_EVIDENCE = "morphology";
	static final String INITIALS_EVIDENCE = "initials";
	static final String NEW_LABEL_EVIDENCE = "newLabel";
	static final String LABEL_EVIDENCE = "label";
	static final String EQUAL_EPITHETS_EVIDENCE = "equalEpithets";
	static final String PATTERN_EVIDENCE = "pattern";
	static final String JOINT_EVIDENCE = "joint";
	static final String LEXICON_EVIDENCE = "lexicon";
	static final String AUTHOR_EVIDENCE = "author";
	static final String PRECISION_EVIDENCE = "precision";
	static final String RECALL_EVIDENCE = "recall";
	static final String FEEDBACK_EVIDENCE = "feedback";
	
	
	public static final int DEBUG_LEVEL_ALL = 4;
	public static final int DEBUG_LEVEL_DETAILS = 2;
	public static final int DEBUG_LEVEL_IMPORTANT = 1;
	public static final int DEBUG_LEVEL_NONE = 0;
	
	/**
	 * Container data structure holding all document-wide evidence data for
	 * OmniFAT-processing a document.
	 * 
	 * @author sautter
	 */
	public static class DocumentDataSet {
		public final OmniFAT omniFat;
		public final String docId;
		
		public DocumentDataSet(OmniFAT omniFat) {
			this(omniFat, Gamta.getAnnotationID());
		}
		public DocumentDataSet(OmniFAT omniFat, String docId) {
			this.omniFat = omniFat;
			this.docId = docId;
		}
		
		private Set textTokens = new HashSet();
		private Set inSentenceTextTokens = new HashSet();
		
		private HashMap epithetLists = new HashMap();
		private HashMap abbreviationLists = new HashMap();
		private HashMap abbreviationResolvers = new HashMap();
		
		private StringVector authorNameParts = new StringVector();
		private StringVector authorNames = new StringVector();
		
		
		private StringVector sureAuthorNameParts = new StringVector();
		private StringVector sureAuthorNames = new StringVector();
		
		private HashMap sureEpithetLists = new HashMap();
		private HashMap sureAbbreviationLists = new HashMap();
		private HashMap sureAbbreviationResolvers = new HashMap();
		
		private HashMap sureEpithetRankSets = new HashMap();
		
		private HashMap sureEpithetCooccurrenceLists = new HashMap();
		
		private HashMap sureCombinationBuckets = new HashMap();
		
//		private int debugLevel = DEBUG_LEVEL_IMPORTANT;
		private int debugLevel = DEBUG_LEVEL_NONE;
		
		
		public Dictionary getAuthorNames() {
			return this.authorNames;
		}
		public boolean isAuthorName(String authorName) {
			return this.authorNames.contains(authorName);
		}
		public void addAuthorName(String authorName) {
			if (authorName.length() > 2)
				this.authorNames.addElementIgnoreDuplicates(authorName);
		}
		
		public Dictionary getAuthorNameParts() {
			return this.authorNameParts;
		}
		public void addAuthorNamePart(String authorNamePart) {
			if (authorNamePart.length() > 2)
				this.authorNameParts.addElementIgnoreDuplicates(authorNamePart);
		}
		public void addAuthorNameParts(StringVector authorNameParts) {
			for (int p = 0; p < authorNameParts.size(); p++)
				this.addAuthorNamePart(authorNameParts.get(p));
		}
		
		public Dictionary getSureAuthorNames() {
			return this.sureAuthorNames;
		}
		public boolean isSureAuthorName(String authorName) {
			return this.sureAuthorNames.contains(authorName);
		}
		public void addSureAuthorName(String authorName) {
			if (authorName.length() > 2)
				this.sureAuthorNames.addElementIgnoreDuplicates(authorName);
		}
		
		public Dictionary getSureAuthorNameParts() {
			return this.sureAuthorNameParts;
		}
		public void addSureAuthorNamePart(String authorNamePart) {
			if (authorNamePart.length() > 2)
				this.sureAuthorNameParts.addElementIgnoreDuplicates(authorNamePart);
		}
		public void addSureAuthorNameParts(StringVector authorNameParts) {
			for (int p = 0; p < authorNameParts.size(); p++)
				this.addSureAuthorNamePart(authorNameParts.get(p));
		}
		
		
		public void setDebugLevel(int debugLevel) {
			this.debugLevel = debugLevel;
		}
		
		public StringIterator getTextTokens() {
			final Iterator it = this.textTokens.iterator();
			return new StringIterator() {
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					if (this.hasNext())
						return it.next();
					else return null;
				}
				public void remove() {}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
		public boolean isTextToken(String token) {
			return this.textTokens.contains(token);
		}
		public void addTextToken(String textToken) {
			this.textTokens.add(textToken);
		}
		
		public StringIterator getInSentenceTextTokens() {
			final Iterator it = this.inSentenceTextTokens.iterator();
			return new StringIterator() {
				public boolean hasNext() {
					return it.hasNext();
				}
				public Object next() {
					if (this.hasNext())
						return it.next();
					else return null;
				}
				public void remove() {}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
		public boolean isInSentenceTextToken(String token) {
			return this.inSentenceTextTokens.contains(token);
		}
		public void addInSentenceTextToken(String inSentenceTextToken) {
			this.inSentenceTextTokens.add(inSentenceTextToken);
		}
		
		
		private StringVector getEpithetList(String rankGroup, boolean create) {
			StringVector epithetList = ((StringVector) this.sureEpithetLists.get(rankGroup));
			if ((epithetList == null) && create) {
				epithetList = new StringVector();
				this.epithetLists.put(rankGroup, epithetList);
			}
			return epithetList;
		}
		public Dictionary getEpithetList(String rankGroup) {
			return this.getEpithetList(rankGroup, false);
		}
		public boolean isEpithet(String rankGroup, String epithet) {
			StringVector epithetList = this.getEpithetList(rankGroup, false);
			return ((epithetList != null) && epithetList.contains(epithet));
		}
		public void addEpithet(String rankGroup, String epithet) {
			this.getEpithetList(rankGroup, true).addElementIgnoreDuplicates(epithet);
			for (int a = this.omniFat.minAbbreviationLength; a <= Math.min(epithet.length(), this.omniFat.maxAbbreviationLength); a++) {
				if (this.omniFat.unDottedAbbreviations)
					this.addAbbreviation(rankGroup, epithet.substring(0,a), epithet);
				this.addAbbreviation(rankGroup, (epithet.substring(0,a) + "."), epithet);
			}
		}
		private void addAbbreviation(String rankGroup, String abbreviation, String epithet) {
			this.getAbbreviationList(rankGroup, true).addElementIgnoreDuplicates(abbreviation);
			this.getAbbreviationResolver(rankGroup, abbreviation, true).addElementIgnoreDuplicates(epithet);
		}
		
		
		private StringVector getAbbreviationList(String rankGroup, boolean create) {
			StringVector abbreviationList = ((StringVector) this.abbreviationLists.get(rankGroup));
			if ((abbreviationList == null) && create) {
				abbreviationList = new StringVector();
				this.abbreviationLists.put(rankGroup, abbreviationList);
			}
			return abbreviationList;
		}
		private StringVector getAbbreviationResolver(String rankGroup, String abbreviation, boolean create) {
			StringVector abbreviationResolver = ((StringVector) this.abbreviationResolvers.get(rankGroup + "_" + abbreviation));
			if ((abbreviationResolver == null) && create) {
				abbreviationResolver = new StringVector();
				this.abbreviationResolvers.put((rankGroup + "_" + abbreviation), abbreviationResolver);
			}
			return abbreviationResolver;
		}
		public Dictionary getAbbreviationList(String rankGroup) {
			return this.getAbbreviationList(rankGroup, false);
		}
		public boolean isAbbreviation(String rankGroup, String abbreviation) {
			StringVector abbreviationList = this.getAbbreviationList(rankGroup, false);
			return ((abbreviationList != null) && abbreviationList.contains(abbreviation));
		}
		public Dictionary getAbbreviationResolver(String rankGroup, String abbreviation) {
			return this.getAbbreviationResolver(rankGroup, abbreviation, false);
		}
		
		
		private StringVector getSureEpithetList(String rankGroup, boolean create) {
			StringVector epithetList = ((StringVector) this.sureEpithetLists.get(rankGroup));
			if ((epithetList == null) && create) {
				epithetList = new StringVector();
				this.sureEpithetLists.put(rankGroup, epithetList);
			}
			return epithetList;
		}
		public Dictionary getSureEpithetList(String rankGroup) {
			return this.getSureEpithetList(rankGroup, false);
		}
		public boolean isSureEpithet(String rankGroup, String epithet) {
			StringVector epithetList = this.getSureEpithetList(rankGroup, false);
			return ((epithetList != null) && epithetList.contains(epithet));
		}
		public void addSureEpithet(String rankGroup, String epithet) {
			this.getSureEpithetList(rankGroup, true).addElementIgnoreDuplicates(epithet);
			for (int a = this.omniFat.minAbbreviationLength; a <= Math.min(epithet.length(), this.omniFat.maxAbbreviationLength); a++) {
				this.addSureAbbreviation(rankGroup, epithet.substring(0,a), epithet);
				this.addSureAbbreviation(rankGroup, (epithet.substring(0,a) + "."), epithet);
			}
		}
		private void addSureAbbreviation(String rankGroup, String abbreviation, String epithet) {
			this.getSureAbbreviationList(rankGroup, true).addElementIgnoreDuplicates(abbreviation);
			this.getSureAbbreviationResolver(rankGroup, abbreviation, true).addElementIgnoreDuplicates(epithet);
		}
		
		
		private StringVector getSureAbbreviationList(String rankGroup, boolean create) {
			StringVector abbreviationList = ((StringVector) this.sureAbbreviationLists.get(rankGroup));
			if ((abbreviationList == null) && create) {
				abbreviationList = new StringVector();
				this.sureAbbreviationLists.put(rankGroup, abbreviationList);
			}
			return abbreviationList;
		}
		private StringVector getSureAbbreviationResolver(String rankGroup, String abbreviation, boolean create) {
			StringVector abbreviationResolver = ((StringVector) this.sureAbbreviationResolvers.get(rankGroup + "_" + abbreviation));
			if ((abbreviationResolver == null) && create) {
				abbreviationResolver = new StringVector();
				this.sureAbbreviationResolvers.put((rankGroup + "_" + abbreviation), abbreviationResolver);
			}
			return abbreviationResolver;
		}
		public Dictionary getSureAbbreviationList(String rankGroup) {
			return this.getSureAbbreviationList(rankGroup, false);
		}
		public boolean isSureAbbreviation(String rankGroup, String abbreviation) {
			StringVector abbreviationList = this.getSureAbbreviationList(rankGroup, false);
			return ((abbreviationList != null) && abbreviationList.contains(abbreviation));
		}
		public Dictionary getSureAbbreviationResolver(String rankGroup, String abbreviation) {
			return this.getSureAbbreviationResolver(rankGroup, abbreviation, false);
		}
		
		private Set dummyEpithetRankSet = Collections.unmodifiableSet(new HashSet());
		private Set getEpithetRankSet(String epithet, boolean create) {
			Set epithetRankSet = ((Set) this.sureEpithetRankSets.get(epithet));
			if ((epithetRankSet == null) && create) {
				epithetRankSet = new HashSet();
				this.sureEpithetRankSets.put(epithet, epithetRankSet);
			}
			return epithetRankSet;
		}
		public Set getEpithetRanks(String epithet) {
			Set epithetRankSet = this.getEpithetRankSet(epithet, false);
			return ((epithetRankSet == null) ? dummyEpithetRankSet : epithetRankSet);
		}
		public void addEpithetRank(String epithet, String rank) {
			this.getEpithetRankSet(epithet, true).add(rank);
		}
		
		private StringVector getSureEpithetCooccurrenceList(String epithet, boolean create) {
			StringVector cooccurrenceList = ((StringVector) this.sureEpithetCooccurrenceLists.get(epithet));
			if ((cooccurrenceList == null) && create) {
				cooccurrenceList = new StringVector();
				this.sureEpithetCooccurrenceLists.put(epithet, cooccurrenceList);
			}
			return cooccurrenceList;
		}
		public Dictionary getSureCooccurringEpithets(String epithet) {
			return this.getSureEpithetCooccurrenceList(epithet, false);
		}
		public void addSureEpithetCombination(StringVector epithetCombination) {
			for (int e = 0; e < epithetCombination.size(); e++)
				this.getSureEpithetCooccurrenceList(epithetCombination.get(e), true).addContentIgnoreDuplicates(epithetCombination);
		}
		
		private HashMap epithetOccurrenceIndices = new HashMap();
		private class EpithetOccurrenceIndex {
			private int nextCounter = Integer.MAX_VALUE-1;
			private HashMap occurrences = new HashMap();
			void addEpithet(String epithet) {
				this.occurrences.put(epithet, new Integer(this.nextCounter--));
			}
			int getOccurrenceIndex(String epithet) {
				Integer oi = ((Integer) this.occurrences.get(epithet));
				return ((oi == null) ? Integer.MAX_VALUE : oi.intValue());
			}
		}
		private EpithetOccurrenceIndex getEpithetOccurrenceIndex(String rankGroup, boolean create) {
			EpithetOccurrenceIndex occurrenceIndex = ((EpithetOccurrenceIndex) this.epithetOccurrenceIndices.get(rankGroup));
			if ((occurrenceIndex == null) && create) {
				occurrenceIndex = new EpithetOccurrenceIndex();
				this.epithetOccurrenceIndices.put(rankGroup, occurrenceIndex);
			}
			return occurrenceIndex;
		}
		public void addEpithetOccurrence(String rankGroup, String epithet) {
			this.getEpithetOccurrenceIndex(rankGroup, true).addEpithet(epithet);
		}
		public int compareByLastOccurrence(String rankGroup, String epithet1, String epithet2) {
			EpithetOccurrenceIndex occurrenceIndex = this.getEpithetOccurrenceIndex(rankGroup, false);
			if (occurrenceIndex == null)
				return 0;
			int o1 = occurrenceIndex.getOccurrenceIndex(epithet1);
			int o2 = occurrenceIndex.getOccurrenceIndex(epithet2);
			return (o1 - o2);
		}
		
		private HashMap combinationCache = new HashMap();
		public Combination getCombination(QueriableAnnotation taxonName) {
			Combination comb = ((Combination) this.combinationCache.get(taxonName.getAnnotationID()));
			if (comb == null) {
				comb = new Combination(taxonName, this.omniFat.minAbbreviationLength, this.omniFat.maxAbbreviationLength);
				this.combinationCache.put(comb.annotationId, comb);
			}
			return comb;
		}
		public List getSureCombinationBucket(String key) {
			ArrayList combBucket = ((ArrayList) this.sureCombinationBuckets.get(key));
			if (combBucket == null) {
				combBucket = new ArrayList();
				this.sureCombinationBuckets.put(key, combBucket);
			}
			return combBucket;
		}
		
		private HashMap epithetCache = new HashMap();
		public QueriableAnnotation[] getEpithets(QueriableAnnotation taxonName) {
			QueriableAnnotation[] epithets = ((QueriableAnnotation[]) this.epithetCache.get(taxonName.getAnnotationID()));
			if (epithets == null) {
				String source = ((String) taxonName.getAttribute(SOURCE_ATTRIBUTE));
				if (source == null)
					return new QueriableAnnotation[0];
				epithets = taxonName.getAnnotations(EPITHET_TYPE);
				ArrayList epithetList = new ArrayList();
				for (int e = 0; e < epithets.length; e++)
					if (source.indexOf(epithets[e].getAnnotationID()) != -1)
						epithetList.add(epithets[e]);
				epithets = ((QueriableAnnotation[]) epithetList.toArray(new QueriableAnnotation[epithetList.size()]));
				this.epithetCache.put(taxonName.getAnnotationID(), epithets);
			}
			return epithets;
		}
		
		public void log(String entry, int level) {
			if (level <= this.debugLevel) {
				System.out.println(entry);
				//	TODO store in local list
			}
		}
	}
	
	static class Combination {
		final String annotationId;
		HashSet epithetStarts = new HashSet();
		private HashMap epithetMatchSets = new HashMap();
		private HashMap firstEpithetMatchSets = new HashMap();
		private LinkedHashMap epithetSets = new LinkedHashMap();
		private int minAbbreviationLength;
		private int maxAbbreviationLength;
		private Combination(QueriableAnnotation taxonName, int minAbbreviationLength, int maxAbbreviationLength) {
			this.annotationId = taxonName.getAnnotationID();
			this.minAbbreviationLength = minAbbreviationLength;
			this.maxAbbreviationLength = maxAbbreviationLength;
			String source = ((String) taxonName.getAttribute(SOURCE_ATTRIBUTE, ""));
			QueriableAnnotation[] epithets = taxonName.getAnnotations(EPITHET_TYPE);
			for (int e = 0; e < epithets.length; e++)
				if (source.indexOf(epithets[e].getAnnotationID()) != -1) {
					String epithet = ((String) epithets[e].getAttribute(STRING_ATTRIBUTE));
					String rankGroup = ((String) epithets[e].getAttribute(RANK_GROUP_ATTRIBUTE));
					this.addEpithet(epithet, rankGroup);
				}
		}
		private void addEpithet(String epithet, String rankGroup) {
			LinkedHashSet epithetSet = ((LinkedHashSet) this.epithetSets.get(rankGroup));
			if (epithetSet == null) {
				epithetSet = new LinkedHashSet();
				this.epithetSets.put(rankGroup, epithetSet);
			}
			epithetSet.add(epithet);
			
			this.epithetStarts.add(epithet.substring(0,1).toLowerCase());
			
			HashSet epithetMatchSet = ((HashSet) this.epithetMatchSets.get(rankGroup));
			if (epithetMatchSet == null) {
				epithetMatchSet = new HashSet();
				this.epithetMatchSets.put(rankGroup, epithetMatchSet);
			}
			epithetMatchSet.add(epithet);
			if (epithet.length() > this.maxAbbreviationLength) {
				for (int a = this.minAbbreviationLength; a <= Math.min(epithet.length(), this.maxAbbreviationLength); a++) {
					epithetMatchSet.add(epithet.substring(0,a));
					epithetMatchSet.add(epithet.substring(0,a) + ".");
				}
			}
			if (!this.firstEpithetMatchSets.containsKey(rankGroup))
				this.firstEpithetMatchSets.put(rankGroup, new HashSet(epithetMatchSet));
		}
		boolean covers(Combination comb) {
			return this.covers(comb, false);
		}
		boolean covers(Combination comb, boolean allowSuffix) {
			if (!allowSuffix) {
				HashSet unmatchedRankGroups = new HashSet(this.epithetMatchSets.keySet());
				unmatchedRankGroups.removeAll(comb.epithetSets.keySet());
				if (unmatchedRankGroups.size() != 0)
					return false;
			}
			
			boolean gotFirstEpithet = !allowSuffix;
			boolean matchAnchored = false;
			
			for (Iterator rgit = comb.epithetSets.keySet().iterator(); rgit.hasNext();) {
				String rankGroup = ((String) rgit.next());
				
				LinkedHashSet epithetSet = ((LinkedHashSet) comb.epithetSets.get(rankGroup));
				HashSet epithetMatchSet = ((HashSet) this.epithetMatchSets.get(rankGroup));
				if (epithetMatchSet == null) {
					if (gotFirstEpithet)
						return false;
					else continue;
				}
				HashSet firstEpithetMatchSet = (allowSuffix ? null : ((HashSet) this.firstEpithetMatchSets.get(rankGroup)));
				
				for (Iterator eit = epithetSet.iterator(); eit.hasNext();) {
					String epithet = ((String) eit.next());
					if (firstEpithetMatchSet != null) {
						if (firstEpithetMatchSet.contains(epithet))
							firstEpithetMatchSet = null;
						else return false;
					}
					if (epithetMatchSet.contains(epithet)) {
						gotFirstEpithet = true;
						matchAnchored = (epithet.length() > 3);
					}
					else return false;
				}
			}
			return matchAnchored;
		}
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (Iterator rgit = this.epithetSets.keySet().iterator(); rgit.hasNext();) {
				String rankGroup = ((String) rgit.next());
				LinkedHashSet epithetSet = ((LinkedHashSet) this.epithetSets.get(rankGroup));
				for (Iterator eit = epithetSet.iterator(); eit.hasNext();) {
					String epithet = ((String) eit.next());
					if (sb.length() != 0)
						sb.append(" ");
					sb.append(epithet + "/" + rankGroup);
				}
			}
			return sb.toString();
		}
	}
	
	private int minAbbreviationLength = 1;
	int getMinAbbreviationLength() {
		return this.minAbbreviationLength;
	}

	private int maxAbbreviationLength = 2;
	int getMaxAbbreviationLength() {
		return this.maxAbbreviationLength;
	}
	
	private boolean unDottedAbbreviations = false;
	boolean acceptUnDottedAbbreviations() {
		return this.unDottedAbbreviations;
	}
	
	private int fuzzyMatchThreshold = 2;
	int getFuzzyMatchThreshold() {
		return this.fuzzyMatchThreshold;
	}
	
	private boolean embeddedAuthorNames = true;
	boolean acceptEmbeddedAuthorNames() {
		return this.embeddedAuthorNames;
	}
	
	
	private OmniFatDictionary[] authorNameDictionaries = new OmniFatDictionary[0];
	private ExtensibleDictionary authorNameDictionary;
	OmniFatDictionary[] getAuthorNameDictionaries() {
		OmniFatDictionary[] authorNameDictionaries = new OmniFatDictionary[this.authorNameDictionaries.length + 1];
		System.arraycopy(this.authorNameDictionaries, 0, authorNameDictionaries, 0, this.authorNameDictionaries.length);
		this.authorNameDictionary.compile();
		authorNameDictionaries[this.authorNameDictionaries.length] = this.authorNameDictionary;
		return authorNameDictionaries;
	}
	
	void addAuthor(String author) {
		if (author.length() > this.maxAbbreviationLength)
			this.authorNameDictionary.addElement(author);
	}
	private void removeAuthor(String author) {
		this.authorNameDictionary.removeElement(author);
	}
	
	private OmniFatPattern[] authorNamePatterns = new OmniFatPattern[0];
	OmniFatPattern[] getAuthorNamePatterns() {
		OmniFatPattern[] anPats = new OmniFatPattern[this.authorNamePatterns.length];
		System.arraycopy(this.authorNamePatterns, 0, anPats, 0, anPats.length);
		return anPats;
	}
	
	private OmniFatPattern[] authorInitialPatterns = new OmniFatPattern[0];
	OmniFatPattern[] getAuthorInitialPatterns() {
		OmniFatPattern[] aniPats = new OmniFatPattern[this.authorInitialPatterns.length];
		System.arraycopy(this.authorInitialPatterns, 0, aniPats, 0, aniPats.length);
		return aniPats;
	}
	
	private Dictionary authorNameStopWords;
	Dictionary getAuthorNameStopWords() {
		return this.authorNameStopWords;
	}
	
	private Dictionary authorListSeparators;
	Dictionary getAuthorListSeparators() {
		return this.authorListSeparators;
	}
	
	private Dictionary authorListEndSeparators;
	Dictionary getAuthorListEndSeparators() {
		return this.authorListEndSeparators;
	}
	
	
	private OmniFatDictionary[] negativeDictionaries = new OmniFatDictionary[0];
	private ExtensibleDictionary negativeDictionary;
	OmniFatDictionary[] getNegativeDictionaries() {
		OmniFatDictionary[] negativeDictionaries = new OmniFatDictionary[this.negativeDictionaries.length + 1];
		System.arraycopy(this.negativeDictionaries, 0, negativeDictionaries, 0, this.negativeDictionaries.length);
		this.negativeDictionary.compile();
		negativeDictionaries[this.negativeDictionaries.length] = this.negativeDictionary;
		return negativeDictionaries;
	}
	
	private OmniFatPattern[] negativePatterns;
	OmniFatPattern[] getNegativePatterns() {
		OmniFatPattern[] negPats = new OmniFatPattern[this.negativePatterns.length];
		System.arraycopy(this.negativePatterns, 0, negPats, 0, this.negativePatterns.length);
		return negPats;
	}
	
	void addNegative(String negative) {
		if ((negative.length() > this.maxAbbreviationLength) && Gamta.isWord(negative))
			this.negativeDictionary.addElement(negative);
	}
	private void removeNegative(String negative) {
		this.negativeDictionary.removeElement(negative);
	}
	
	private Set stemmingLookupCache = new HashSet();
	private StemmingRule[] stemmingRules = new StemmingRule[0];
	boolean doStemmingNegativeLookup(String lookup) {
		if (lookup == null) return false;
		else if (this.stemmingLookupCache.contains(lookup))
			return true;
		
		Dictionary[] theNegativeDictionaries = this.getNegativeDictionaries();
		Dictionary[] negativeDictionaries = new Dictionary[theNegativeDictionaries.length + 1];
		System.arraycopy(theNegativeDictionaries, 0, negativeDictionaries, 0, theNegativeDictionaries.length);
		negativeDictionaries[theNegativeDictionaries.length] = this.getStopWords();
		
		for (int sr = 0; sr < this.stemmingRules.length; sr++) {
			if (lookup.endsWith(this.stemmingRules[sr].ending)) {
				String lookupStem = lookup.substring(0, (lookup.length() - this.stemmingRules[sr].ending.length()));
				for (int s = 0; s < this.stemmingRules[sr].stemmedEndings.length; s++) {
					String stemmedLookup = lookupStem + this.stemmingRules[sr].stemmedEndings[s];
					for (int n = 0; n < negativeDictionaries.length; n++)
						if (negativeDictionaries[n].lookup(stemmedLookup)) {
							this.stemmingLookupCache.add(lookup);
							return true;
						}
				}
			}
		}
		
		return false;
	}
	
	
	private Dictionary stopWords;
	Dictionary getStopWords() {
		return this.stopWords;
	}
	
	private Dictionary interEpithetPunctuationMarks;
	Dictionary getInterEpithetPunctuationMarks() {
		return this.interEpithetPunctuationMarks;
	}
	
	private Dictionary intraEpithetPunctuationMarks;
	Dictionary getIntraEpithetPunctuationMarks() {
		return this.intraEpithetPunctuationMarks;
	}
	
	private RankGroup[] rankGroups = new RankGroup[0];
	RankGroup[] getRankGroups() {
		RankGroup[] rgs = new RankGroup[this.rankGroups.length];
		System.arraycopy(this.rankGroups, 0, rgs, 0, rgs.length);
		return rgs;
	}
	
	RankGroup getRankGroup(int orderNumber) {
		return this.getRank(orderNumber).group;
	}
	
	private Rank[] ranks = new Rank[0];
	Rank[] getRanks() {
		Rank[] rs = new Rank[this.ranks.length];
		System.arraycopy(this.ranks, 0, rs, 0, rs.length);
		return rs;
	}
	
	Rank getRank(int orderNumber) {
		return this.ranks[ranks.length - orderNumber];
	}
	
	private DataRuleSet[] dataRuleSets = new DataRuleSet[0];
	DataRuleSet[] getDataRuleSets() {
		DataRuleSet[] drss = new DataRuleSet[this.dataRuleSets.length];
		System.arraycopy(this.dataRuleSets, 0, drss, 0, this.dataRuleSets.length);
		return drss;
	}
	
	private boolean cleanUpAfterDataRules = true;
	boolean cleanUpAfterDataRules() {
		return this.cleanUpAfterDataRules;
	}
	
	void learnEpithets(Annotation[] epithets) {
		for (int e = 0; e < epithets.length; e++) {
			String epithet = ((String) epithets[e].getAttribute(VALUE_ATTRIBUTE));
			RankGroup rankGroup = this.getRankGroup(Integer.parseInt((String) epithets[e].getAttribute(RANK_GROUP_NUMBER_ATTRIBUTE)));
			if (rankGroup.isAutoLearning())
				rankGroup.addEpithet(epithet);
			
			this.removeAuthor(epithet);
			this.removeNegative(epithet);
			
			String authorName = ((String) epithets[e].getAttribute(AUTHOR_ATTRIBUTE));
			if (authorName != null)
				this.addAuthor(authorName);
		}
	}
	
	void learnEpithets(QueriableAnnotation taxonName) {
		for (int r = 0; r < this.ranks.length; r++) {
			String rankEpithet = ((String) taxonName.getAttribute(this.ranks[r].getName()));
			if (rankEpithet == null)
				continue;
			
			if (this.ranks[r].group.isAutoLearning())
				this.ranks[r].getRankGroup().addEpithet(rankEpithet);
			
			this.removeAuthor(rankEpithet);
			this.removeNegative(rankEpithet);
		}
	}
	
	void teachEpithets(QueriableAnnotation taxonName) {
		for (int r = 0; r < this.ranks.length; r++) {
			String rankEpithet = ((String) taxonName.getAttribute(this.ranks[r].getName()));
			if (rankEpithet == null)
				continue;
			
			if (this.ranks[r].group.isTeachable())
				this.ranks[r].getRankGroup().addEpithet(rankEpithet);
			
			this.removeAuthor(rankEpithet);
			this.removeNegative(rankEpithet);
		}
	}
	
	void storeLearnedData() {
		if (this.dataProvider == null) return;
		
		AnalyzerDataProvider adp = this.dataProvider;
		this.dataProvider = null;
		
		//	for each rank group, store list of learned epithets
		for (int rg = 0; rg < this.rankGroups.length; rg++)
			if (this.rankGroups[rg].epithetDictionary.isDirty())
				this.storeExtensibleDictionary(this.rankGroups[rg].epithetDictionary, adp);
		
		//	store list of learned author names
		if (this.authorNameDictionary.isDirty())
			this.storeExtensibleDictionary(this.authorNameDictionary, adp);
		
		//	store dictionary of learned negatives
		if (this.negativeDictionary.isDirty())
			this.storeExtensibleDictionary(this.negativeDictionary, adp);
	}
	
	private void storeExtensibleDictionary(ExtensibleDictionary dictionary, AnalyzerDataProvider adp) {
		try {
			dictionary.compile();
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(adp.getOutputStream(dictionary.name + LEXICON_NAME_EXTENSION), "UTF-8"));
			for (StringIterator eit = dictionary.getEntryIterator(); eit.hasMoreStrings();) {
				String entry = eit.nextString();
				if (entry != null) {
					bw.write(entry);
					bw.newLine();
				}
			}
			bw.flush();
			bw.close();
		}
		catch (IOException ioe) {
			System.out.println("Exception storing extensible lexicon " + dictionary.name + ": " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	private static final String LEXICON_NAME_EXTENSION = ".list.txt";
	private static final String NOT_LEXICON_NAME_EXTENSION = ".not.txt";
	private static final String REGEX_NAME_EXTENSION = ".regEx.txt";
	
	private AnalyzerDataProvider dataProvider;
	private String name;
	private String decsription;
	private OmniFAT(String name, AnalyzerDataProvider dataProvider) {
		this.name = name;
		this.decsription = name;
		this.dataProvider = dataProvider;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDescription() {
		return this.decsription;
	}
	
	private static final HashMap bases = new HashMap();
	public static Base getBase(AnalyzerDataProvider adp) {
		if (adp == null)
			return null;
		Base base = ((Base) bases.get(adp.getAbsolutePath()));
		if (base == null) {
			base = new Base(adp);
			bases.put(adp.getAbsolutePath(), base);
		}
		return base;
	}
//	public static Base getBase(AnalyzerDataProvider adp) {
//		Base base = ((Base) bases.get(adp));
//		if (base == null) {
//			if (adp == null)
//				return null;
//			base = new Base(adp);
//			bases.put(adp, base);
//		}
//		return base;
//	}
	
	public static OmniFAT getDefaultInstance(AnalyzerDataProvider adp) {
		return getBase(adp).getDefaultInstance();
	}
	
	public static OmniFAT getInstance(String name, AnalyzerDataProvider adp) {
		return getBase(adp).getInstance(name);
	}
	
	public static String getDefaultInstanceName(AnalyzerDataProvider adp) {
		return getBase(adp).getDefaultInstanceName();
	}
	
	public static void setDefaultInstanceName(AnalyzerDataProvider adp, String defaultInstanceName) {
		getBase(adp).setDefaultInstanceName(defaultInstanceName);
	}
	
	public static String[] getInstanceNames(AnalyzerDataProvider adp) {
		return getBase(adp).getInstanceNames();
	}
	
	static final String CONFIG_FILE_NAME = "OmniFAT.cnfg";
	
	static final String INSTANCE_FILE_NAME_SUFFIX = ".omniFatInstance.xml";
	
	static final String DEFAULT_INSTANCE_NAME_SETTING = "DefaultInstanceName";
	private static final String DEFAULT_INSTANCE_NAME = "Default";
	
	private static final void prepareSuffixDiff(OmniFAT omniFat, Map thresholds) {
		//	generate statistics over negative lexicons
		ArrayList negativeList = new ArrayList();
		for (int n = 0; n < omniFat.negativeDictionaries.length; n++) {
			if (thresholds.containsKey(omniFat.negativeDictionaries[n].name))
				negativeList.add(new DictionaryStat(omniFat.negativeDictionaries[n]));
		}
		DictionaryStat[] negatives = ((DictionaryStat[]) negativeList.toArray(new DictionaryStat[negativeList.size()]));
		
		//	prepare diff suffixes for individual rank groups
		for (int rg = 0; rg < omniFat.rankGroups.length; rg++)
			if (omniFat.rankGroups[rg].doSuffixDiff()) {
				ArrayList dictList = new ArrayList();
				dictList.addAll(Arrays.asList(omniFat.rankGroups[rg].getPositiveDictionaries()));
				Rank[] ranks = omniFat.rankGroups[rg].getRanks();
				for (int r = 0; r < ranks.length; r++)
					dictList.addAll(Arrays.asList(ranks[r].getPositiveDictionaries()));
				DictionaryStat rankGroupEpithets = new DictionaryStat((OmniFatDictionary[]) dictList.toArray(new OmniFatDictionary[dictList.size()]));
				
				HashMap frequencyRatioMap = new HashMap();
				for (int n = 0; n < negatives.length; n++) {
					int frequencyRatioThreshold = ((Integer) thresholds.get(negatives[n].name)).intValue();
					for (Iterator eit = negatives[n].suffixesCountSorted.iterator(); eit.hasNext();) {
						String suffix = ((String) eit.next());
						if (negatives[n].getFrequency(suffix) < suffixDiff_minSuffixFrequency)
							break;
						
						suffixDiff_computeDiscriminativeness(suffix, negatives[n], rankGroupEpithets, frequencyRatioMap, frequencyRatioThreshold);
					}
				}
				
				TreeSet goodSet = new TreeSet(new Comparator() {
					public int compare(Object o1, Object o2) {
						String s1 = ((String) o1);
						String s2 = ((String) o2);
			            for (int i1 = (s1.length()-1), i2 = (s2.length()-1); (i1 > -1) && (i2 > -1); i1--, i2--) {
			                char c1 = s1.charAt(i1);
			                char c2 = s2.charAt(i2);
			                if (c1 != c2)
			                    return c1 - c2;
			            }
			            return (s1.length() - s2.length());
					}
				});
				goodSet.addAll(frequencyRatioMap.keySet());
				
				for (Iterator git = goodSet.iterator(); git.hasNext();) {
					String goodSuffix = ((String) git.next()).trim();
					if (suffixDiff_isEligibleSuffix(goodSuffix, omniFat.rankGroups[rg].suffixDiffData)) {
						int count = 0;
						for (int n = 0; n < negatives.length; n++)
							count += negatives[n].suffixCounts.getCount(goodSuffix);
						String cString = ("" + frequencyRatioMap.get(goodSuffix) + " - " + count + " / " + rankGroupEpithets.suffixCounts.getCount(goodSuffix)); 
						System.out.println("suffix diff for " + omniFat.rankGroups[rg].getName() + ": " + suffixDiff_prepareSuffixForLog(goodSuffix) + " (" + cString + ")");
						
						omniFat.rankGroups[rg].suffixDiffData.add(goodSuffix);
						omniFat.rankGroups[rg].suffixDiffLength = Math.max(omniFat.rankGroups[rg].suffixDiffLength, goodSuffix.length());
					}
				}
			}
	}
	
	private static final void suffixDiff_computeDiscriminativeness(String suffix, DictionaryStat suffixHost, DictionaryStat comparison, Map goodMap, int frequencyRatioThreshold) {
		double sFreq = suffixHost.getFrequency(suffix);
		double minRatio = Double.MAX_VALUE;
		double cFreq = comparison.getFrequency(suffix);
		if (cFreq != 0)
			minRatio = Math.min(minRatio, (sFreq / cFreq));
		if (frequencyRatioThreshold < minRatio) {
			if (goodMap.containsKey(suffix)) {
				Double minRatioOld = ((Double) goodMap.get(suffix));
				goodMap.put(suffix, new Double(Math.max(minRatio, minRatioOld.doubleValue())));
			}
			else goodMap.put(suffix, new Double(minRatio));
		}
	}
	
	private static final boolean suffixDiff_isEligibleSuffix(String suffix, Set suffixesSoFar) {
		for (int s = 1; s < suffix.length(); s++)
			if (suffixesSoFar.contains(suffix.substring(s)))
				return false;
		return true;
	}
	
	private static final String suffixDiff_prepareSuffixForLog(String suffix) {
		while (suffix.length() < suffixDiff_maxSuffixLength)
			suffix = (" " + suffix);
		return suffix;
	}
	
	private static final int suffixDiff_maxSuffixLength = 4;
	private static final double suffixDiff_minSuffixFrequency = 0.001;
	
	private static class DictionaryStat {
		final String name;
		private TreeSet suffixes = new TreeSet();
		StringIndex suffixCounts = new StringIndex();
		TreeSet suffixesCountSorted;
		private int wordCount = 0;
		DictionaryStat(OmniFatDictionary dict) {
			this.name = dict.name;
			this.addDictionary(dict);
			this.compile();
		}
		DictionaryStat(OmniFatDictionary[] dicts) {
			this.name = "";
			for (int d = 0; d < dicts.length; d++)
				this.addDictionary(dicts[d]);
			this.compile();
		}
		private void addDictionary(OmniFatDictionary dict) {
			for (StringIterator dit = dict.getEntryIterator(); dit.hasMoreStrings();) {
				String dictEntry = dit.nextString();
				for (int s = 0; s <= Math.min(dictEntry.length()-1, suffixDiff_maxSuffixLength); s++) {
					String suffix = dictEntry.substring(dictEntry.length() - s);
					this.suffixes.add(suffix);
					this.suffixCounts.add(suffix);
				}
			}
		}
		private void compile() {
			this.suffixesCountSorted = new TreeSet(new Comparator() {
				public int compare(Object o1, Object o2) {
					int c = (suffixCounts.getCount(o2.toString()) - suffixCounts.getCount(o1.toString()));
					return ((c == 0) ? ((String) o1).compareTo((String) o2) : c);
				}
			});
			this.suffixesCountSorted.addAll(this.suffixes);
			this.wordCount = this.suffixCounts.getCount("");
			this.suffixes.clear();
		}
		double getFrequency(String suffix) {
			if (this.wordCount == 0)
				return 0;
			return (((double) this.suffixCounts.getCount(suffix)) / this.wordCount);
		}
	}
	
	
	private static final String PARAMETER_VALUE_SEPARATOR = "|";
	private static final String PARAMETER_ENCODING = "UTF-8";
	
	public static DocumentDataSet doOmniFAT(MutableAnnotation data, DocumentDataSet docData) {
		return doOmniFAT(data, docData, null);
	}
	
	private static DocumentDataSet doOmniFAT(MutableAnnotation data, DocumentDataSet docData, String instanceName) {
		OmniFatFunctions.tagBaseEpithets(data, docData);
		OmniFatFunctions.tagAuthorNames(data, docData);
		OmniFatFunctions.tagLabels(data, docData);
		
		OmniFatFunctions.tagEpithets(data, docData);
		OmniFatFunctions.tagCandidates(data, docData);
		
		OmniFatFunctions.applyPrecisionRules(data, docData);
		OmniFatFunctions.applyAuthorNameRules(data, docData);
		OmniFatFunctions.applyDataRules(data, docData);
		
		OmniFatFunctions.completeAbbreviations(data, docData);
		OmniFatFunctions.inferRanks(data, docData);
		OmniFatFunctions.completeNames(data, docData);
		
		return docData;
	}
	
	abstract static class EpithetGroup {
		final OmniFAT omniFat;
		private String name;
		private int orderNumber;
		
		private OmniFatDictionary[] positiveDictionaries;
		private OmniFatDictionary[] negativeDictionaries;
		
		private OmniFatPattern[] precisionPatterns;
		private OmniFatPattern[] recallPatterns;
		private OmniFatPattern[] negativePatterns;
		
		protected EpithetGroup(OmniFAT omniFat, String name, int orderNumber) {
			this.omniFat = omniFat;
			this.name = name;
			this.orderNumber = orderNumber;
		}
		
		public String getName() {
			return this.name;
		}
		public int getOrderNumber() {
			return this.orderNumber;
		}
		
		public OmniFatDictionary[] getPositiveDictionaries() {
			OmniFatDictionary[] positiveDictionaries = new OmniFatDictionary[this.positiveDictionaries.length];
			System.arraycopy(this.positiveDictionaries, 0, positiveDictionaries, 0, this.positiveDictionaries.length);
			return positiveDictionaries;
		}
		public OmniFatDictionary[] getNegativeDictionaries() {
			OmniFatDictionary[] negativeDictionaries = new OmniFatDictionary[this.negativeDictionaries.length];
			System.arraycopy(this.negativeDictionaries, 0, negativeDictionaries, 0, this.negativeDictionaries.length);
			return negativeDictionaries;
		}
		
		public Dictionary getStopWords() {
			return this.omniFat.getStopWords();
		}
		
		public OmniFatPattern[] getPrecisionPatterns() {
			OmniFatPattern[] precisionPatterns = new OmniFatPattern[this.precisionPatterns.length];
			System.arraycopy(this.precisionPatterns, 0, precisionPatterns, 0, this.precisionPatterns.length);
			return precisionPatterns;
		}
		public OmniFatPattern[] getRecallPatterns() {
			OmniFatPattern[] recallPatterns = new OmniFatPattern[this.recallPatterns.length];
			System.arraycopy(this.recallPatterns, 0, recallPatterns, 0, this.recallPatterns.length);
			return recallPatterns;
		}
		public OmniFatPattern[] getNegativePatterns() {
			OmniFatPattern[] negativePatterns = new OmniFatPattern[this.negativePatterns.length];
			System.arraycopy(this.negativePatterns, 0, negativePatterns, 0, this.negativePatterns.length);
			return negativePatterns;
		}
	}
	
	static class Rank extends EpithetGroup {
		private RankGroup group;
		private StringVector epithetLabels = new StringVector();
		private StringVector newEpithetLabels = new StringVector();
		private String epithetDisplayPattern = "@epithet";
		private boolean epithetsLabeled;
		private boolean epithetsUnlabeled;
		private boolean required;
		private int probability = 5;
		Rank(OmniFAT omniFat, String name, int orderNumber, RankGroup group) {
			super(omniFat, name, orderNumber);
			this.group = group;
		}
		public OmniFatDictionary[] getNegativeDictionaries() {
			// TODOne negative dictionaries: defined in rank, rank group, global
			ArrayList negativeDictionaries = new ArrayList();
			negativeDictionaries.addAll(Arrays.asList(super.getNegativeDictionaries()));
			negativeDictionaries.addAll(Arrays.asList(this.group.getNegativeDictionaries()));
			return ((OmniFatDictionary[]) negativeDictionaries.toArray(new OmniFatDictionary[negativeDictionaries.size()]));
		}
		public OmniFatPattern[] getNegativePatterns() {
			// TODOne negative patterns: defined in rank, rank group, global
			LinkedHashSet negativePatterns = new LinkedHashSet();
			negativePatterns.addAll(Arrays.asList(super.getNegativePatterns()));
			negativePatterns.addAll(Arrays.asList(this.group.getNegativePatterns()));
			return ((OmniFatPattern[]) negativePatterns.toArray(new OmniFatPattern[negativePatterns.size()]));
		}
		public Dictionary getLabels() {
			return this.epithetLabels;
		}
		public Dictionary getNewEpithetLables() {
			return this.newEpithetLabels;
		}
		public boolean isEpithetsLabeled() {
			return this.epithetsLabeled;
		}
		public boolean isEpithetsUnlabeled() {
			return this.epithetsUnlabeled;
		}
		public boolean isEpithetRequired() {
			return this.required;
		}
		public RankGroup getRankGroup() {
			return this.group;
		}
		public String formatEpithet(String epithet) {
			return this.epithetDisplayPattern.replaceAll("\\@epithet", epithet); 
		}
		public int getProbability() {
			return this.probability;
		}
	}
	
	static class RankGroup extends EpithetGroup {
		private ArrayList ranks = new ArrayList();
		
		private boolean epithetsCapitalized;
		private boolean epithetsLowerCase;
		
		private boolean repeatedEpithets = false;
		private boolean inCombinations = true;
		
		private boolean isTeachable = true;
		private boolean isAutoLearning = false;
		
		private int suffixDiffLength = 0;
		private Set suffixDiffData;
		
		private ExtensibleDictionary epithetDictionary;
		
		RankGroup(OmniFAT omniFat, String name, int orderNumber) {
			super(omniFat, name, orderNumber);
		}
		public OmniFatDictionary[] getPositiveDictionaries() {
			OmniFatDictionary[] staticPositiveDictionaries = super.getPositiveDictionaries();
			this.epithetDictionary.compile();
			OmniFatDictionary[] positiveDictionaries = new OmniFatDictionary[staticPositiveDictionaries.length + 1];
			System.arraycopy(staticPositiveDictionaries, 0, positiveDictionaries, 0, staticPositiveDictionaries.length);
			positiveDictionaries[staticPositiveDictionaries.length] = this.epithetDictionary;
			return positiveDictionaries;
		}
		public OmniFatDictionary[] getNegativeDictionaries() {
			ArrayList negativeDictionaries = new ArrayList();
			negativeDictionaries.addAll(Arrays.asList(super.getNegativeDictionaries()));
			negativeDictionaries.addAll(Arrays.asList(this.omniFat.getNegativeDictionaries()));
			return ((OmniFatDictionary[]) negativeDictionaries.toArray(new OmniFatDictionary[negativeDictionaries.size()]));
		}
		public OmniFatPattern[] getNegativePatterns() {
			// TODOne negative patterns: defined in rank group, global
			LinkedHashSet negativePatterns = new LinkedHashSet();
			negativePatterns.addAll(Arrays.asList(super.getNegativePatterns()));
			negativePatterns.addAll(Arrays.asList(this.omniFat.getNegativePatterns()));
			return ((OmniFatPattern[]) negativePatterns.toArray(new OmniFatPattern[negativePatterns.size()]));
		}
		
		void addRank(Rank rank) {
			this.ranks.add(rank);
		}
		public Rank[] getRanks() {
			return ((Rank[]) this.ranks.toArray(new Rank[this.ranks.size()]));
		}
		
		public boolean isEpithetsCapitalized() {
			return this.epithetsCapitalized;
		}
		public boolean isEpithetsLowerCase() {
			return this.epithetsLowerCase;
		}
		
		public boolean hasRepeatedEpithets() {
			return this.repeatedEpithets;
		}
		
		public boolean isCombinable() {
			return this.inCombinations;
		}
		
		public boolean appearsInCombinations() {
			return this.inCombinations;
		}
		
		public boolean doSuffixDiff() {
			return (this.suffixDiffData != null);
		}
		
		boolean doSuffixDiffLookup(String lookup) {
			if (this.suffixDiffData == null)
				return false;
			for (int s = 0; s <= Math.min(lookup.length()-1, this.suffixDiffLength); s++) {
				if (this.suffixDiffData.contains(lookup.substring(lookup.length() - s)))
					return true;
			}
			return false;
		}
		
		public boolean isTeachable() {
			return this.isTeachable;
		}
		public boolean isAutoLearning() {
			return this.isAutoLearning;
		}
		
		public void addEpithet(String epithet) {
			if (epithet.length() > this.omniFat.maxAbbreviationLength)
				this.epithetDictionary.addElement(epithet);
		}
		public void removeEpithet(String epithet) {
			this.epithetDictionary.removeElement(epithet);
		}
	}
	
	static class StemmingRule {
		private String ending;
		private String[] stemmedEndings;
		StemmingRule(String ending, String[] stemmedEndings) {
			this.ending = ending;
			this.stemmedEndings = stemmedEndings;
		}
		public boolean matches(String word) {
			return ((word != null) && word.endsWith(this.ending));
		}
	}
	
	static class DataRule {
		static final String PROMOTE_ACTION = "promote";
		static final String FEEDBACK_ACTION = "feedback";
		static final String REMOVE_ACTION = "remove";
		
		private Pattern match;
		public final String pattern;
		public final String action;
		public final boolean removeNested;
		public final boolean likelyMatch;
		DataRule(String pattern, String action, boolean removeNested, boolean likelyMatch) {
			this.pattern = pattern;
			this.match = Pattern.compile(pattern);
			this.action = action;
			this.removeNested = removeNested;
			this.likelyMatch = likelyMatch;
		}
		public boolean matches(String epithetStatusString) {
			return ((epithetStatusString != null) && this.match.matcher(epithetStatusString).matches());
		}
		private static Properties stateStringCharacters = new Properties();
		static {
			stateStringCharacters.setProperty(POSITIVE_STATE, "p");
			stateStringCharacters.setProperty(LIKELY_STATE, "l");
			stateStringCharacters.setProperty(ABBREVIATED_STATE, "b");
			stateStringCharacters.setProperty(AMBIGUOUS_STATE, "a");
			stateStringCharacters.setProperty(UNCERTAIN_STATE, "u");
			stateStringCharacters.setProperty(NEGATIVE_STATE, "n");
		}
		
//		private static Properties epithetStatusStringCache = new Properties(); NEVER CACHE STATE STRINGS, THEY CHANGE OVER TIME !!!
		static String getEpithetStatusString(Annotation[] epithets) {
			StringBuffer stateStringBuilder = new StringBuffer();
			for (int e = 0; e < epithets.length; e++) {
				String state = ((String) epithets[e].getAttribute(STATE_ATTRIBUTE));
				if (state == null)
					state = UNCERTAIN_STATE;
				stateStringBuilder.append(stateStringCharacters.getProperty(state, "u"));
			}
			return stateStringBuilder.toString();
		}
		static String getEpithetStatusString(QueriableAnnotation taxonNameCandidate) {
			String source = ((String) taxonNameCandidate.getAttribute("source", ""));
			Annotation[] epithets = taxonNameCandidate.getAnnotations("epithet");
			ArrayList epithetList = new ArrayList();
			for (int e = 0; e < epithets.length; e++)
				if (source.indexOf(epithets[e].getAnnotationID()) != -1)
					epithetList.add(epithets[e]);
			return getEpithetStatusString(((QueriableAnnotation[]) epithetList.toArray(new QueriableAnnotation[epithetList.size()])));
		}
	}
	
	static class DataRuleSet {
		private DataRule[] rules;
		private int maxRounds = 0;
		DataRuleSet(DataRule[] rules) {
			this.rules = rules;
		}
		public DataRule[] getRules() {
			DataRule[] rules = new DataRule[this.rules.length];
			System.arraycopy(this.rules, 0, rules, 0, this.rules.length);
			return rules;
		}
		public int getMaxRounds() {
			return this.maxRounds;
		}
	}
	
	private static class ExtensibleDictionary extends OmniFatDictionary {
		OmniFatDictionary staticDictionary;
		CompiledDictionary dynamicDictionary;
		private int ddSize = -1;
		CompiledDictionary negativeDictionary;
		private int ndSize = -1;
		ExtensibleDictionary(String name, boolean trusted) {
			this(name, trusted, null);
		}
		ExtensibleDictionary(String name, boolean trusted, OmniFatDictionary staticDictionary) {
			super(name, trusted);
			this.staticDictionary = ((staticDictionary == null) ? new CompiledDictionary(name, trusted) : staticDictionary);
			this.dynamicDictionary = new CompiledDictionary(name, trusted);
			this.negativeDictionary = new CompiledDictionary(("-" + name), trusted);
		}
		void addElement(String element) {
			if (!this.staticDictionary.lookup(element) || this.negativeDictionary.lookup(element))
				this.dynamicDictionary.add(element);
		}
		void removeElement(String element) {
			if (this.staticDictionary.lookup(element) || this.dynamicDictionary.lookup(element))
				this.negativeDictionary.add(element);
		}
		void compile() {
			this.dynamicDictionary.compile();
			if (this.ddSize == -1)
				this.ddSize = this.dynamicDictionary.size();
			this.negativeDictionary.compile();
			if (this.ndSize == -1)
				this.ndSize = this.negativeDictionary.size();
		}
		boolean isDirty() {
			return ((this.ddSize != this.dynamicDictionary.size()) || (this.ndSize != this.negativeDictionary.size()));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
		 */
		public StringIterator getEntryIterator() {
			final Dictionary[] partDictionaries = {this.staticDictionary, this.dynamicDictionary};
			return new StringIterator() {
				StringIterator dit = null;
				int dIndex = 0;
				public boolean hasNext() {
//					System.out.println("next()");
					if (this.dit == null) {
//						System.out.println("  - no dit");
						if (this.dIndex < partDictionaries.length) {
							this.dit = partDictionaries[this.dIndex++].getEntryIterator();
//							System.out.println("  - more parts --> dit created");
							return this.hasNext();
						}
						else {
//							System.out.println("  - no more parts --> false");
							return false;
						}
					}
					else if (this.dit.hasNext()) {
//						System.out.println("  - dit.next() --> true");
						return true;
					}
					else {
						this.dit = null;
//						System.out.println("  - no dit.next() --> recurse");
						return this.hasNext();
					}
				}
				public Object next() {
					if (this.hasNext()) {
						String next = ((String) this.dit.next());
						if (partDictionaries[this.dIndex-1] == dynamicDictionary)
							return next;
						else if (negativeDictionary.lookup(next, true))
							return this.next();
						else return next;
					}
					else return null;
				}
				public void remove() {}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
		 */
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
		 */
		public boolean isEmpty() {
			return (this.size() == 0);
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
		 */
		public int size() {
			return ((this.dynamicDictionary.isCompiled() && this.negativeDictionary.isCompiled()) ? (this.staticDictionary.size() + this.dynamicDictionary.size()) : 0);
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
		 */
		public boolean lookup(String string) {
			return this.lookup(string, this.isDefaultCaseSensitive());
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
		 */
		public boolean lookup(String string, boolean caseSensitive) {
			return (this.dynamicDictionary.lookup(string, caseSensitive) || (!this.negativeDictionary.lookup(string, caseSensitive) && this.staticDictionary.lookup(string, caseSensitive)));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.OmniFatDictionary#fuzzyLookup(java.lang.String, int)
		 */
		protected boolean fuzzyLookup(String string, int threshold) {
			return this.fuzzyLookup(string, threshold, this.isDefaultCaseSensitive());
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.OmniFatDictionary#fuzzyLookup(java.lang.String, int, boolean)
		 */
		protected boolean fuzzyLookup(String string, int threshold, boolean caseSensitive) {
			return (this.dynamicDictionary.fuzzyLookup(string, threshold, caseSensitive) || (this.negativeDictionary.lookup(string, caseSensitive) && this.staticDictionary.fuzzyLookup(string, threshold, caseSensitive)));
		}
	}
	
	/**
	 * A compiled dictionary is aimed at holding large lookup lists of strings in as
	 * little memory as possible. Their lifecycle is divided into two phases: a
	 * building phase and a lookup phase, separated by the compilation. Before the
	 * compilation (during the build phase), new strings can be added to the
	 * dictionary, but lookup operations are undefined. After the compilation, no
	 * new strings can be added, but lookup operations are possible. This is because
	 * during the compilation, the dictionary is compressed into a specialized
	 * lookup data structure that reduces memory requirements, but prohibits further
	 * insertions.<BR>
	 * Warning: In favor of keeping memory footprint low, this class <B>does not
	 * support unicode</B>, but stores strings as sequences of 1-byte ASCII-8
	 * characters. In cases where exact lookups for non-ASCII-8 characters are
	 * required, use CompiledDictionaty instead.
	 * 
	 * @author sautter
	 */
	private static class CompiledDictionary extends OmniFatDictionary {
		private static final int chunkSize = 1020;
		private static final Comparator byteChunkComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				byte[][] b1 = ((byte[][]) o1);
				byte[][] b2 = ((byte[][]) o2);
				
				int c = byteComparator.compare(b1[b1.length-1], b2[0]);
				if (c < 0) return -1;
				
				c = byteComparator.compare(b2[b2.length-1], b1[0]);
				if (c < 0) return 1;
				
				return 0;
			}
		};
		private static final Comparator byteComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				byte[] b1 = ((byte[]) o1);
				byte[] b2 = ((byte[]) o2);
				if (b1.length != b2.length)
					return (b1.length - b2.length);
				for (int b = 0; b < Math.min(b1.length, b2.length); b++) {
					if (b1[b] != b2[b])
						return ((255 & b1[b]) - (255 & b2[b]));
				}
				return (b1.length - b2.length);
			}
		};
		
		
		private static final Comparator lengthFirstComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				String s1 = ((String) o1);
				String s2 = ((String) o2);
				int c = s1.length() - s2.length();
				return ((c == 0) ? s1.compareTo(s2) : c);
			}
		};
		private Set entrySet = new TreeSet(lengthFirstComparator);
		
		private int entryCount = -1;
		private Map csChunks = new TreeMap(byteChunkComparator);
		private Map ciChunks = new TreeMap(byteChunkComparator);
		
		/**
		 * Constructor building a case sensitive dictionary
		 */
		CompiledDictionary(String name, boolean trusted) {
			super(name, trusted);
		}
		
		/**
		 * Add a string to the dictionary. Note that this method has only an effect
		 * before the compilation. Likewise, looking up the argument string returns
		 * true only after the compilation.
		 * @param string the string to add
		 */
		synchronized void add(String string) {
			if (string != null)
				this.entrySet.add(string);
		}
		
		/**
		 * Check whether the dictionary has been compiled.
		 * @return true if has been compiled, false otherwise
		 */
		synchronized boolean isCompiled() {
			return (this.entrySet.isEmpty());
		}
		
		/**
		 * Compile the dictionary. This disables adding further strings, and enables
		 * lookup. Invocing this method more than once has no effect.
		 */
		synchronized void compile() {
			if (this.entrySet.isEmpty()) return;
			
			for(Iterator cit = this.csChunks.values().iterator(); cit.hasNext();) {
				byte[][] chunk = ((byte[][]) cit.next());
				cit.remove();
				for (int c = 0; c < chunk.length; c++)
					this.entrySet.add(decode(chunk[c]));
			}
			this.entryCount = this.entrySet.size();
			
			TreeSet ciEntryCollector = new TreeSet(lengthFirstComparator);
			
			compile(this.entrySet, this.csChunks, ciEntryCollector);
			compile(ciEntryCollector, this.ciChunks, null);
			
			this.entrySet.clear();
			ciEntryCollector = null;
			System.gc();
			
			System.out.println("Compiled, register sizes are:\n- " + this.csChunks.size() + " CS chunks\n- " + this.ciChunks.size() + " CI chunks");
		}
		
		private static void compile(Set entrySet, Map chunks, Set ciEntryCollector) {
			ArrayList chunkCollector = new ArrayList(chunkSize);
			
			for (Iterator it = entrySet.iterator(); it.hasNext();) {
				String de = ((String) it.next());
				chunkCollector.add(de);
				if (chunkCollector.size() == chunkSize) {
					String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
					byte[][] bChunk = new byte[chunk.length][];
					for (int b = 0; b < bChunk.length; b++)
						bChunk[b] = encode(chunk[b]);
					chunks.put(bChunk, bChunk);
					chunkCollector.clear();
				}
				it.remove();
				
				if (ciEntryCollector != null) {
					String ciDe = de.toLowerCase();
					if (!de.equals(ciDe) && !entrySet.contains(ciDe))
						ciEntryCollector.add(ciDe);
				}
			}
			
			if (chunkCollector.size() != 0) {
				String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
				byte[][] bChunk = new byte[chunk.length][];
				for (int b = 0; b < bChunk.length; b++)
					bChunk[b] = encode(chunk[b]);
				chunks.put(bChunk, bChunk);
			}
			
			chunkCollector.clear();
			System.gc();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
		 */
		public StringIterator getEntryIterator() {
			final Iterator it = this.csChunks.values().iterator();
			return new StringIterator() {
				Iterator pit = null;
				public boolean hasNext() {
					if (this.pit == null) {
						if (it.hasNext()) {
							byte[][] pitData = ((byte[][]) it.next());
							this.pit = Arrays.asList(pitData).iterator();
							return this.pit.hasNext();
						}
						else return false;
					}
					else if (this.pit.hasNext())
						return true;
					else {
						this.pit = null;
						return this.hasNext();
					}
				}
				public Object next() {
					if (this.hasNext())
						return decode((byte[]) this.pit.next());
					else return null;
				}
				public void remove() {}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
		 */
		public StringIterator getEntryIterator(final int minLength) {
			final Iterator cit = this.csChunks.values().iterator();
			return new StringIterator() {
				Iterator pit = null;
				public boolean hasNext() {
					if (this.pit == null) {
						if (cit.hasNext()) {
							byte[][] pitData = ((byte[][]) cit.next());
							while (pitData[pitData.length-1].length < minLength) {
								if  (cit.hasNext())
									pitData = ((byte[][]) cit.next());
								else return false;
							}
							this.pit = Arrays.asList(pitData).iterator();
							return this.pit.hasNext();
						}
						else return false;
					}
					else if (this.pit.hasNext())
						return true;
					else {
						this.pit = null;
						return this.hasNext();
					}
				}
				public Object next() {
					if (this.hasNext())
						return decode((byte[]) this.pit.next());
					else return null;
				}
				public void remove() {}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public String nextString() {
					return ((String) this.next());
				}
			};
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
		 */
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
		 */
		public boolean isEmpty() {
			return (this.size() == 0);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
		 */
		public int size() {
			return (this.isCompiled() ? this.entryCount : 0);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
		 */
		public boolean lookup(String string) {
			return this.lookup(string, this.isDefaultCaseSensitive());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
		 */
		public boolean lookup(String string, boolean caseSensitive) {
			if (lookup(this.csChunks, string))
				return true;
			else if (caseSensitive)
				return false;
			else {
				string = string.toLowerCase();
				return (lookup(this.csChunks, string) || lookup(this.ciChunks, string));
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.OmniFatDictionary#fuzzyLookup(java.lang.String, int)
		 */
		protected boolean fuzzyLookup(String string, int threshold) {
			return this.fuzzyLookup(string, threshold, this.isDefaultCaseSensitive());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.OmniFAT.OmniFatDictionary#fuzzyLookup(java.lang.String, int, boolean)
		 */
		protected boolean fuzzyLookup(String string, int threshold, boolean caseSensitive) {
			if (this.lookup(string, caseSensitive))
				return true;
			
			byte[] stringCrossSumData = new byte[27];
			Arrays.fill(stringCrossSumData, 0, stringCrossSumData.length, ((byte) 0));
			for (int s = 0; s < string.length(); s++) {
				char ch = string.charAt(s);
				if (('a' <= ch) && (ch <= 'z')) stringCrossSumData[ch - 'a']++;
				else if (('A' <= ch) && (ch <= 'Z')) stringCrossSumData[ch - 'A']++;
				else stringCrossSumData[26]++;
			}
			
//			int wordsTested = 0;
//			int crossSumChecks = 0;
//			int levenshteinChecks = 0;
//			int correctionsFound = 0;
			
			byte[] referenceCrossSumData = new byte[27];
			int crossSumDistance = 0;
			int levenshteinDistance;
			for (StringIterator dit = this.getEntryIterator(Math.max(0, (string.length() - threshold))); dit.hasMoreStrings();) {
				String reference = dit.nextString();
				if (reference.length() > (string.length() + threshold))
					return false;
//				wordsTested++;
				
				Arrays.fill(referenceCrossSumData, 0, referenceCrossSumData.length, ((byte) 0));
				crossSumDistance = 0;
				for (int cc = 0; cc < reference.length(); cc++) {
					char ch = reference.charAt(cc);
					if (('a' <= ch) && (ch <= 'z')) referenceCrossSumData[ch - 'a']++;
					else if (('A' <= ch) && (ch <= 'Z')) referenceCrossSumData[ch - 'A']++;
					else referenceCrossSumData[26]++;
				}
				for (int csc = 0; csc < stringCrossSumData.length; csc++)
					crossSumDistance += Math.abs(stringCrossSumData[csc] - referenceCrossSumData[csc]);
//				crossSumChecks++;
				
				if (crossSumDistance <= (2 * threshold)) {
					levenshteinDistance = StringUtils.getLevenshteinDistance(string, reference, threshold, caseSensitive);
//					levenshteinChecks++;
					
					if (levenshteinDistance <= threshold) {// compute similarity to word in question
//						correctionsFound++;
						return true;
					}
				}
			}
			
			return false;
		}
		
		private static boolean lookup(Map chunks, String string) {
			byte[] bytes = encode(string);
			byte[][] bLookup = {bytes};
			byte[][] bChunk = ((byte[][]) chunks.get(bLookup));
			if (bChunk == null) return false;
			int index = Arrays.binarySearch(bChunk, bytes, byteComparator);
			return ((index >= 0) && (index < bChunk.length) && (byteComparator.compare(bChunk[index], bytes) == 0));
		}
		
		private static byte[] encode(String s) {
			byte[] bytes = new byte[s.length()];
			for (int c = 0; c < s.length(); c++) {
				char ch = s.charAt(c);
				if (ch < 255)
					bytes[c] = ((byte) s.charAt(c));
				else bytes[c] = ((byte) 255);
			}
			return bytes;
		}
		
		private static String decode(byte[] bytes) {
			StringBuffer sb = new StringBuffer();
			for (int b = 0; b < bytes.length; b++)
				sb.append((char) (255 & bytes[b]));
			return sb.toString();
		}
	}
}