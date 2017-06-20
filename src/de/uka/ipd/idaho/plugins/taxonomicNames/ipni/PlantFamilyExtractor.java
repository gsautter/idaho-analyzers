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
package de.uka.ipd.idaho.plugins.taxonomicNames.ipni;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringIndex;

/**
 * This utility fetches the higher taxonomy epithets of plant families from
 * http://www.plantsystematics.org/reveal/pbio/fam/allspgnames.html, links them
 * together based on the listed type genera, and stores them for each family. 
 * 
 * @author sautter
 */
public class PlantFamilyExtractor implements TaxonomicNameConstants {
	
	//	TODO grab this: http://botany.csdl.tamu.edu/FLORA/newgate/cron1ang.htm (class, subclass, order, families)
	public static void main(String[] args) throws Exception {
		//mainGet(args);
		mainWiki(args);
	}
	
	public static void mainFilter(String[] args) throws Exception {
		String dataPath = PlantFamilyExtractor.class.getName();
		dataPath = dataPath.substring(0, dataPath.lastIndexOf('.'));
		dataPath = dataPath.replaceAll("\\.", "/");
		File inFile = new File(".", (dataPath + "/Plantae.primaryTaxonomy.xml"));
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
		File outFile = new File(".", (dataPath + "/Plantae.higherTaxonomy.xml"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
		for (String line; (line = br.readLine()) != null;) {
			if (line.trim().startsWith("<species"))
				continue;
			if (line.trim().startsWith("</genus>"))
				continue;
			if (line.trim().startsWith("<genus") && !line.endsWith("/>"))
				line = (line.substring(0, (line.length() - ">".length())) + "/>");
			bw.write(line);
			bw.newLine();
		}
		bw.flush();
		bw.close();
		br.close();
	}
	
	public static void mainGet(String[] args) throws Exception {
		Set handledPageNames = new HashSet();
		Map nodeIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeMap(String.CASE_INSENSITIVE_ORDER);
					this.put(key, value);
				}
				return value;
			}
		};
		TaxonTreeNode root = new TaxonTreeNode(null, "Plantae", KINGDOM_ATTRIBUTE);
		((Map) nodeIndex.get(root.rank)).put(root.epithet, root);
		LinkedList path = new LinkedList();
		LinkedList allPaths = new LinkedList();
		path.add("Plantae");
		addChildrenWiki("Plantae", KINGDOM_ATTRIBUTE, root, nodeIndex, path, false, handledPageNames, allPaths);
		for (int p = 0; p < fixedPathsWiki.length; p++) {
			path.clear();
			path.add("Plantae");
			TaxonTreeNode node = root;
			for (int r = 1; r < fixedPathsWiki[p].length; r++) {
				TaxonTreeNode child = node.getChild(fixedPathsWiki[p][r]);
				if (child == null) {
					child = new TaxonTreeNode(node, fixedPathsWiki[p][r], ranksWiki[r]);
					node.put(child.epithet, child);
					((Map) nodeIndex.get(child.rank)).put(child.epithet, child);
				}
				node = child;
				path.addLast(node.epithet);
			}
			addChildrenWiki(node.epithet, node.rank, node, nodeIndex, path, false, handledPageNames, allPaths);
		}
		addOrdersPlSyst(root, nodeIndex, handledPageNames);
		String dataPath = PlantFamilyExtractor.class.getName();
		dataPath = dataPath.substring(0, dataPath.lastIndexOf('.'));
		dataPath = dataPath.replaceAll("\\.", "/");
		File dataFile = new File(".", (dataPath + "/Plantae.primaryTaxonomy.xml"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile), "UTF-8"));
		root.writeXml(bw);
		bw.flush();
		bw.close();
	}
	private static String baseUrlWiki = "http://en.wikipedia.org/wiki/";
	public static void mainWiki(String[] args) throws Exception {
		Map nodeIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeMap();
					this.put(key, value);
				}
				return value;
			}
		};
		TaxonTreeNode root = new TaxonTreeNode(null, "Plantae", KINGDOM_ATTRIBUTE);
		((Map) nodeIndex.get(root.rank)).put(root.epithet, root);
		LinkedList path = new LinkedList();
		LinkedList allPaths = new LinkedList();
		path.add("Plantae");
		addChildrenWiki("Plantae", KINGDOM_ATTRIBUTE, root, nodeIndex, path, false, new HashSet(), allPaths);
		for (int p = 0; p < fixedPathsWiki.length; p++) {
			path.clear();
			path.add("Plantae");
			TaxonTreeNode node = root;
			for (int r = 1; r < fixedPathsWiki[p].length; r++) {
				TaxonTreeNode child = node.getChild(fixedPathsWiki[p][r]);
				if (child == null) {
					child = new TaxonTreeNode(node, fixedPathsWiki[p][r], ranksWiki[r]);
					node.put(child.epithet, child);
					((Map) nodeIndex.get(child.rank)).put(child.epithet, child);
				}
				node = child;
				path.addLast(node.epithet);
			}
			addChildrenWiki(node.epithet, node.rank, node, nodeIndex, path, false, new HashSet(), allPaths);
		}
		printPaths(root, "");
		LinkedList[] paths = ((LinkedList[]) allPaths.toArray(new LinkedList[allPaths.size()]));
		Arrays.sort(paths, new Comparator() {
			public int compare(Object o1, Object o2) {
				LinkedList ll1 = ((LinkedList) o1);
				LinkedList ll2 = ((LinkedList) o1);
				for (Iterator it1 = ll1.iterator(), it2 = ll2.iterator();;) {
					if (it1.hasNext() && it2.hasNext()) {
						String s1 = ((String) it1.next());
						String s2 = ((String) it2.next());
						int c = s1.compareTo(s2);
						if (c != 0)
							return c;
					}
					else if (it1.hasNext())
						return 1;
					else if (it2.hasNext())
						return -1;
					else return 0;
				}
			}
		});
		for (int p = 0; p < paths.length; p++)
			System.out.println(paths[p].toString());
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"));
		root.writeXml(bw);
		bw.flush();
		bw.close();
//		
//		QueriableAnnotation kingdomPage = getWikiPage("Plantae");
//		QueriableAnnotation infoBox = getInfoBox(kingdomPage);
////		AnnotationUtils.writeXML(infoBox, new OutputStreamWriter(System.out));
//		QueriableAnnotation[] phylums = GPath.evaluatePath(infoBox, ("//a[matches(., '" + rankEpithetPatterns.getProperty(PHYLUM_ATTRIBUTE) + "')]"), null);
//		for (int p = 0; p < phylums.length; p++) {
//			System.out.println(phylums[p].firstValue());
//		}
	}
	
	private static void printPaths(TaxonTreeNode node, String indent) {
		System.out.println(indent + node.epithet);
		for (Iterator cit = node.keySet().iterator(); cit.hasNext();) {
			TaxonTreeNode child = node.getChild((String) cit.next());
			printPaths(child, ("  " + indent));
		}
	}
	
	private static void addChildrenWiki(String epithet, String rank, TaxonTreeNode node, Map nodeIndex, LinkedList path, boolean inSubRank, Set handledPageNames, LinkedList allPaths) throws Exception {
		if (handledPageNames.contains(epithet))
			return;
//		if (PHYLUM_ATTRIBUTE.equals(rank)) {
//			if ("Anthocerotophyta".equals(path.getLast()))
//				System.out.println("HERE WE ARE");
//			else return;
//		}
		System.out.println(path.toString());
		allPaths.add(new LinkedList(path)); // TODO activate this for live harvesting run
		String childRank = ranksWiki[path.size()];
//		System.out.println("Getting " + childRank + " epithets below (sub)" + rank + " " + epithet);
		QueriableAnnotation page = getWikiPage(epithet);
		if (page == null)
			return;
		Thread.sleep(200); // let's not trigger any DOS warnings
		handledPageNames.add(epithet);
//		if ("Zosterophyllophyta".equals(path.getLast()))
//			return; // we have to filter this phylum, as it also occurs as class Zosterophyllopsida
		
		//	test if rank has become child rank
		QueriableAnnotation[] childRankTitle = GPath.evaluatePath(page, ("/head/title[matches(./#first, '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
		if (childRankTitle.length != 0) {
			String childEpithet = childRankTitle[0].firstValue();
			if (node.containsKey(childEpithet))
				return;
			TaxonTreeNode childNode = new TaxonTreeNode(node, childEpithet, childRank);
			node.put(childEpithet, childNode);
			((Map) nodeIndex.get(childNode.rank)).put(childNode.epithet, childNode);
			path.addLast(childEpithet);
			addChildrenWiki(childEpithet, childRank, childNode, nodeIndex, path, false, handledPageNames, allPaths);
			path.removeLast();
			return;
		}
//		if (inSubRank) {
//			QueriableAnnotation[] childRankTitle = GPath.evaluatePath(page, ("/head/title[matches(./#first, '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
//			if (childRankTitle.length != 0) {
//				String childEpithet = childRankTitle[0].firstValue();
//				if (node.containsKey(childEpithet))
//					return;
//				TaxonTreeNode childNode = new TaxonTreeNode(node, childEpithet, childRank);
//				node.put(childEpithet, childNode);
//				path.addLast(childEpithet);
//				addChildren(childEpithet, childRank, childNode, path, false, handledPageNames, allPaths);
//				path.removeLast();
//				return;
//			}
//		}
		
		QueriableAnnotation infoBox = getInfoBoxWiki(page);
		if (infoBox == null) {
			System.out.println("TaxaBox not found for " + epithet);
			String fallbackEpithet = fallbacksWiki.getProperty(epithet);
			if (fallbackEpithet != null) {
				System.out.println(" ==> using fallback " + fallbackEpithet);
				addChildrenWiki(fallbackEpithet, rank, node, nodeIndex, path, inSubRank, handledPageNames, allPaths);
				return;
			}
		}
		
		//	get data via pages on intermediate rank epithets
		String subRank = ("sub" + rank.substring(0, 1).toUpperCase() + rank.substring(1));
		if (!inSubRank) {
			if (rankEpithetPatterns.containsKey(subRank)) {
				System.out.println(" ==> using sub rank " + subRank);
				QueriableAnnotation[] subEpithets = GPath.evaluatePath(page, ("//dd[./#first = '" + subRank.toLowerCase() + "']/a"), null);
				if (subEpithets.length == 0)
					subEpithets = GPath.evaluatePath(page, ("//li[./#first = '" + subRank.toLowerCase() + "']/a"), null);
				for (int s = 0; s < subEpithets.length; s++) {
					String href = ((String) subEpithets[s].getAttribute("href"));
					if (href == null)
						continue;
					href = href.substring(href.lastIndexOf('/') + 1);
					System.out.println("   - " + subEpithets[s].firstValue() + " (link: " + href + ")");
					addChildrenWiki(href, rank, node, nodeIndex, path, true, handledPageNames, allPaths);
				}
				if (infoBox != null) {
					subEpithets = GPath.evaluatePath(infoBox, ("//a[matches(., '" + rankEpithetPatterns.getProperty(subRank) + "')]"), null);
					for (int s = 0; s < subEpithets.length; s++) {
						String href = ((String) subEpithets[s].getAttribute("href"));
						if (href == null)
							continue;
						href = href.substring(href.lastIndexOf('/') + 1);
						System.out.println("   - " + subEpithets[s].firstValue() + " (link: " + href + ")");
						addChildrenWiki(href, rank, node, nodeIndex, path, true, handledPageNames, allPaths);
					}
				}
			}
		}
		
		//	extract genera (we have to be more strict here, as we are beyond the safe reach of our patterns)
		if (GENUS_ATTRIBUTE.equals(childRank)) {
			QueriableAnnotation[] genera = GPath.evaluatePath(page, ("//ul[./li[./i/a/#first = ./ul/li/i/#first]]/li/i/a[1]"), null);
			for (int c = 0; c < genera.length; c++) {
				String childEpithet = genera[c].firstValue();
				if (node.containsKey(childEpithet))
					continue;
				System.out.println("TEXT GENUS: " + childEpithet + " from " + genera[c].toXML());
//				TaxonTreeNode childNode = new TaxonTreeNode(node, childEpithet, childRank);
//				node.put(childEpithet, childNode);
//				path.addLast(childEpithet);
//				addChildren(childEpithet, childRank, childNode, path, false, handledPageNames, sidePaths);
//				path.removeLast();
			}
			if (infoBox == null)
				return;
			QueriableAnnotation generaTitle = getMatch(infoBox, "//th[contains('Genus Genera', ./#first)]");
			if (generaTitle == null)
				return;
			genera = GPath.evaluatePath(infoBox, ("//li[./@START_INDEX > " + generaTitle.getStartIndex() + "]/i/a"), null);
			for (int c = 0; c < genera.length; c++) {
				String childEpithet = genera[c].firstValue();
				handleChildEpithetWiki(childEpithet, childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
			}
			TaxonTreeNode sideNode = node.getRoot();
			LinkedList sidePath = new LinkedList();
			sidePath.add(sideNode.epithet);
			for (int r = 1; r < (path.size()-1); r++) {
				QueriableAnnotation[] sideEpithets = GPath.evaluatePath(infoBox, ("//a[matches(., '" + rankEpithetPatterns.getProperty(ranksWiki[r]) + "')]"), null);
				sidePath.add((sideEpithets.length == 0) ? "UNDEFINED" : sideEpithets[0].firstValue());
			}
			sidePath.addLast(path.getLast());
			System.out.println("  " + sidePath.toString() + " from " + epithet);
			return;
		}
		
		//	extract genera (we have to be more strict here, as we are beyond the safe reach of our patterns)
		if (SPECIES_ATTRIBUTE.equals(childRank)) {
			//	TODO
			return;
		}
		
		//	get children from page on epithet
		if (infoBox != null) {
			QueriableAnnotation[] childEpithets = GPath.evaluatePath(infoBox, ("//a[matches(., '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
			for (int c = 0; c < childEpithets.length; c++)
				handleChildEpithetWiki(childEpithets[c].firstValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
			//	handle non-linked children by reloading parent page one rank down the tree
			if (childEpithets.length == 0) {
				childEpithets = GPath.evaluatePath(infoBox, ("//b[matches(., '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
				for (int c = 0; c < childEpithets.length; c++)
					handleChildEpithetWiki(childEpithets[c].firstValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
				childEpithets = GPath.evaluatePath(infoBox, ("//dt[matches(., '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
				for (int c = 0; c < childEpithets.length; c++)
					handleChildEpithetWiki(childEpithets[c].firstValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
			}
			TaxonTreeNode sideNode = node.getRoot();
			LinkedList sidePath = new LinkedList();
			sidePath.add(sideNode.epithet);
			for (int r = 1; r < (path.size()-1); r++) {
				QueriableAnnotation[] sideEpithets = GPath.evaluatePath(infoBox, ("//a[matches(., '" + rankEpithetPatterns.getProperty(ranksWiki[r]) + "')]"), null);
				sidePath.add((sideEpithets.length == 0) ? "UNDEFINED" : sideEpithets[0].firstValue());
			}
			sidePath.addLast(path.getLast());
			System.out.println("  " + sidePath.toString() + " from " + epithet);
			allPaths.add(sidePath); // TODO activate this for live harvesting run
		}
		Set filterAnnotationIDs = getFilterAnnotationIDsWiki(page);
		QueriableAnnotation[] childEpithets = GPath.evaluatePath(page, ("//dd[./#first = '" + childRank.toLowerCase() + "']/a[matches(., '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
		for (int c = 0; c < childEpithets.length; c++) {
			if (!filterAnnotationIDs.contains(childEpithets[c].getAnnotationID()))
				handleChildEpithetWiki(childEpithets[c].firstValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
		}
		childEpithets = GPath.evaluatePath(page, ("//li[./#first = '" + childRank.toLowerCase() + "']/a[matches(., '" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
		for (int c = 0; c < childEpithets.length; c++) {
			if (!filterAnnotationIDs.contains(childEpithets[c].getAnnotationID()))
				handleChildEpithetWiki(childEpithets[c].firstValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
		}
		childEpithets = GPath.evaluatePath(page, ("//dd[matches(., '" + childRank.toLowerCase() + "\\s*" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
		for (int c = 0; c < childEpithets.length; c++) {
			if (!filterAnnotationIDs.contains(childEpithets[c].getAnnotationID()))
				handleChildEpithetWiki(childEpithets[c].lastValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
		}
		childEpithets = GPath.evaluatePath(page, ("//li[matches(., '" + childRank.toLowerCase() + "\\s*" + rankEpithetPatterns.getProperty(childRank) + "')]"), null);
		for (int c = 0; c < childEpithets.length; c++) {
			if (!filterAnnotationIDs.contains(childEpithets[c].getAnnotationID()))
				handleChildEpithetWiki(childEpithets[c].lastValue(), childRank, node, nodeIndex, path, (inSubRank ? epithet : null), subRank, handledPageNames, allPaths);
		}
	}
	
	private static void handleChildEpithetWiki(String childEpithet, String childRank, TaxonTreeNode node, Map nodeIndex, LinkedList path, String subEpithet, String subRank, Set handledPageNames, LinkedList allPaths) throws Exception {
		TaxonTreeNode childNode = node.getChild(childEpithet);
		if (childNode == null) {
			childNode = new TaxonTreeNode(node, childEpithet, childRank);
			node.put(childEpithet, childNode);
			if (subEpithet != null) {
				childNode.parentSubEpithet = subEpithet;
				childNode.parentSubRank = subRank;
			}
			((Map) nodeIndex.get(childNode.rank)).put(childNode.epithet, childNode);
		}
		else if ((childNode.parentSubEpithet == null) && (subEpithet != null)) {
			childNode.parentSubEpithet = subEpithet;
			childNode.parentSubRank = subRank;
		}
		path.addLast(childEpithet);
		addChildrenWiki(childEpithet, childRank, childNode, nodeIndex, path, false, handledPageNames, allPaths);
		path.removeLast();
	}
	
	private static Set getFilterAnnotationIDsWiki(QueriableAnnotation page) {
		Set filterIDs = new HashSet();
		
		//	find info boxes
		QueriableAnnotation[] navBoxes = GPath.evaluatePath(page, ("//table[./@class = 'navbox']"), null);
		for (int b = 0; b < navBoxes.length; b++) {
			QueriableAnnotation[] annotations = navBoxes[b].getAnnotations();
			for (int a = 0; a < annotations.length; a++)
				filterIDs.add(annotations[a].getAnnotationID());
		}
		
		//	find 'See Also' heading
		QueriableAnnotation[] seeAlsoHeading = GPath.evaluatePath(page, ("//h2[./span = 'See also']"), null);
		if (seeAlsoHeading.length != 0) {
			QueriableAnnotation[] annotations = page.getAnnotations();
			for (int a = 0; a < annotations.length; a++) {
				if (seeAlsoHeading[0].getAbsoluteStartIndex() < annotations[a].getAbsoluteStartIndex())
					filterIDs.add(annotations[a].getAnnotationID());
			}
		}
		
		//	finally
		return filterIDs;
	}
	
	private static QueriableAnnotation getWikiPage(String name) {
		try {
			URL docUrl = new URL(baseUrlWiki + name);
			Reader docReader = new BufferedReader(new InputStreamReader(docUrl.openStream(), "UTF-8"));
			SgmlDocumentReader docBuilder = new SgmlDocumentReader(null, html, null, null, null, 0);
			parser.stream(docReader, docBuilder);
			docReader.close();
			docBuilder.close();
			return docBuilder.getDocument();
		}
		catch (IOException ioe) {
			System.out.println("Could not get page for " + name + ": " + ioe.getMessage());
			return null;
		}
	}
	
	private static QueriableAnnotation getInfoBoxWiki(QueriableAnnotation page) {
		return getMatch(page, "//table[./@class = 'infobox biota']");
	}
	
	private static QueriableAnnotation getMatch(QueriableAnnotation data, String gPath) {
		if (data == null)
			return null;
		QueriableAnnotation[] match = GPath.evaluatePath(data, gPath, null);
		return ((match.length == 0) ? null : match[0]);
	}
	
	private static String[] ranksWiki = {
		KINGDOM_ATTRIBUTE,
		PHYLUM_ATTRIBUTE,
//		SUBPHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
//		SUBCLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
//		SUBORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
//		SUBFAMILY_ATTRIBUTE,
//		TRIBE_ATTRIBUTE,
//		SUBTRIBE_ATTRIBUTE,
		GENUS_ATTRIBUTE,
//		SUBGENUS_ATTRIBUTE,
		SPECIES_ATTRIBUTE,
	};
	private static Properties fallbacksWiki = new Properties();
	static {
		fallbacksWiki.setProperty("Bryophyta", "Moss");
		fallbacksWiki.setProperty("Chaetosiphon", "Chaetosiphon_(alga)");
	}
	private static String[][] fixedPathsWiki = {
		{"Plantae", "Pinophyta", "Pinopsida"},
		
		{"Plantae", "Progymnospermophyta", "Progymnospermopsida"},
		
		{"Plantae", "Pteridophyta", "Lycopodiopsida", "Lycopodiales", "Lycopodiaceae"},
		{"Plantae", "Pteridophyta", "Lycopodiopsida", "Lycopodiales", "Huperziaceae"},
		
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Calamopityales"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Callistophytales"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Corystospermales"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Gigantopteridaceae"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Arberiales"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Leptostrobales"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Lyginopteridopsida"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Lyginopteris"},
		{"Plantae", "Pteridospermatophyta", "UNDEFINED", "Peltaspermales"},
		
		{"Plantae", "Rhyniophyta", "Rhyniopsida", "UNDEFINED", "Rhyniaceae"},
		
		{"Plantae", "Tracheophyta", "Trimerophytopsida", "Trimerophytales", "UNDEFINED", "Eophyllophyton"},
		{"Plantae", "Tracheophyta", "Trimerophytopsida", "Trimerophytales", "UNDEFINED", "Psilophyton"},
		{"Plantae", "Tracheophyta", "Trimerophytopsida", "Trimerophytales", "UNDEFINED", "Trimerophyton"},
		
		{"Plantae", "Marchantiophyta", "Haplomitriopsida", "Haplomitriales", "Haplomitriaceae", "Gessella"},
		{"Plantae", "Marchantiophyta", "Haplomitriopsida", "Haplomitriales", "Haplomitriaceae", "Haplomitrium"},
		{"Plantae", "Marchantiophyta", "Haplomitriopsida", "Treubiales", "Treubiaceae"},
		{"Plantae", "Marchantiophyta", "Jungermanniopsida", "Metzgeriales"},
		{"Plantae", "Marchantiophyta", "Jungermanniopsida", "Jungermanniales"},
		{"Plantae", "Marchantiophyta", "Marchantiopsida", "Blasiales"},
		{"Plantae", "Marchantiophyta", "Marchantiopsida", "Sphaerocarpales"},
		{"Plantae", "Marchantiophyta", "Marchantiopsida", "Marchantiales"},
		
		{"Plantae", "Zosterophyllophyta", "Zosterophyllopsida"},
		
		{"Plantae", "Anthocerotophyta", "Leiosporocerotopsida", "Leiosporocerotales", "Leiosporocerotaceae", "Leiosporoceros"},
	};
	
	private static class TaxonTreeNode extends TreeMap {
		final TaxonTreeNode parent;
		final String epithet;
		final String rank;
		String parentSubEpithet = null;
		String parentSubRank = null;
		TreeSet synonyms = null;
		TaxonTreeNode(TaxonTreeNode parent, String epithet, String rank) {
			super(String.CASE_INSENSITIVE_ORDER);
			this.parent = parent;
			this.epithet = epithet;
			this.rank = rank;
		}
		TaxonTreeNode getChild(String childEpithet) {
			return ((TaxonTreeNode) this.get(childEpithet));
		}
		TaxonTreeNode getRoot() {
			return ((this.parent == null) ? this : this.parent.getRoot());
		}
		void writeXml(BufferedWriter bw) throws IOException {
			this.writeXml(bw, "");
		}
		void writeXml(BufferedWriter bw, String indent) throws IOException {
			bw.write(indent + "<" + this.rank + " name=\"" + this.epithet + "\"");
			if (this.parentSubEpithet != null)
				bw.write(" " + this.parentSubRank + "=\"" + this.parentSubEpithet + "\"");
			if (this.synonyms != null) {
				bw.write(" synonyms=\"");
				for (Iterator sit = this.synonyms.iterator(); sit.hasNext();) {
					bw.write((String) sit.next());
					if (sit.hasNext())
						bw.write(";");
				}
				bw.write("\"");
			}
			if (this.isEmpty()) {
				bw.write("/>"); bw.newLine();
			}
			else {
				bw.write(">"); bw.newLine();
				for (Iterator cit = this.keySet().iterator(); cit.hasNext();)
					this.getChild((String) cit.next()).writeXml(bw, (indent + "  "));
				bw.write(indent + "</" + this.rank + ">"); bw.newLine();
			}
		}
	}
	
	
	private static String baseUrlPlSyst = "http://www.plantsystematics.org";
	public static void mainPlSyst(String[] args) throws Exception {
		
		//	get data
		MutableAnnotation orderListDoc = getDocumentPlSyst("/cgi-bin/dol/dol_nav_vbar.pl", false);
		
		//	get order names and links
		QueriableAnnotation[] orders = GPath.evaluatePath(orderListDoc, "//a[contains(./@href, '/taxpage/0/order/')]", null);
		for (int o = 0; o < orders.length; o++) {
			System.out.println(orders[o].firstValue());
			getFamiliesPlSyst(orders[o], null, null);
		}
	}
	
	private static void addOrdersPlSyst(TaxonTreeNode root, Map nodeIndex, Set handledPageNames) throws Exception {
		
		//	set up 'side entry' path for Wikipedia
		LinkedList path = new LinkedList();
		path.addLast("Plantae");
		path.addLast("UNDEFINED"); // phylum
		path.addLast("UNDEFINED"); // class
		TaxonTreeNode undefinedPhylumNode = root.getChild("UNDEFINED");
		TaxonTreeNode undefinedClassNode = ((undefinedPhylumNode == null) ? null : undefinedPhylumNode.getChild("UNDEFINED"));
		
		//	get data
		MutableAnnotation orderListDoc = getDocumentPlSyst("/cgi-bin/dol/dol_nav_vbar.pl", false);
		
		//	get order names and links
		QueriableAnnotation[] orders = GPath.evaluatePath(orderListDoc, "//a[contains(./@href, '/taxpage/0/order/')]", null);
		for (int o = 0; o < orders.length; o++) {
			String order = orders[o].firstValue();
			TaxonTreeNode orderNode = ((TaxonTreeNode) ((Map) nodeIndex.get(ORDER_ATTRIBUTE)).get(order));
			if (orderNode == null) {
				if (root == null)
					System.out.println("" + order);
				else {
					if (undefinedClassNode == null) {
						if (undefinedPhylumNode == null) {
							undefinedPhylumNode = new TaxonTreeNode(root, "UNDEFINED", PHYLUM_ATTRIBUTE);
							root.put(undefinedPhylumNode.epithet, undefinedPhylumNode);
						}
						undefinedClassNode = new TaxonTreeNode(undefinedPhylumNode, "UNDEFINED", CLASS_ATTRIBUTE);
						undefinedPhylumNode.put(undefinedClassNode.epithet, undefinedClassNode);
					}
					orderNode = new TaxonTreeNode(undefinedClassNode, order, ORDER_ATTRIBUTE);
					undefinedClassNode.put(orderNode.epithet, orderNode);
					((Map) nodeIndex.get(orderNode.rank)).put(orderNode.epithet, orderNode);
					path.addLast(order);
					addChildrenWiki(order, ORDER_ATTRIBUTE, orderNode, nodeIndex, path, false, handledPageNames, new LinkedList());
					path.removeLast();
				}
			}
			getFamiliesPlSyst(orders[o], orderNode, nodeIndex);
		}
	}
	
	private static void getFamiliesPlSyst(QueriableAnnotation order, TaxonTreeNode orderNode, Map nodeIndex) throws Exception {
		
		//	get data
		MutableAnnotation familyListDoc = getDocumentPlSyst(((String) order.getAttribute("href")), false);
//		
//		//	get synonyms
//		QueriableAnnotation[] synonymLists = GPath.evaluatePath(familyListDoc, "//center/big", null);
//		for (int s = 0; s < synonymLists.length; s++)
//			System.out.println(" SYN: " + synonymLists[s].getValue());
		
		//	get family names and links
		QueriableAnnotation[] families = GPath.evaluatePath(familyListDoc, "//a[contains(./@href, '/taxpage/0/family/')]", null);
		for (int f = 0; f < families.length; f++) {
			String family = families[f].firstValue();
			if (orderNode == null) {
				System.out.println("  " + family);
				getGeneraPlSyst(families[f], null, nodeIndex);
				continue;
			}
			TaxonTreeNode familyNode = orderNode.getChild(family);
			if (familyNode == null) {
				familyNode = new TaxonTreeNode(orderNode, family, FAMILY_ATTRIBUTE);
				orderNode.put(family, familyNode);
				((Map) nodeIndex.get(familyNode.rank)).put(familyNode.epithet, familyNode);
			}
			getGeneraPlSyst(families[f], familyNode, nodeIndex);
		}
	}
	
	private static void getGeneraPlSyst(QueriableAnnotation family, TaxonTreeNode familyNode, Map nodeIndex) throws Exception {
		
		//	get data
		MutableAnnotation genusListDoc = getDocumentPlSyst(((String) family.getAttribute("href")), false);
		
		//	get synonyms
		QueriableAnnotation[] synonymLists = GPath.evaluatePath(genusListDoc, "//center/big", null);
		for (int l = 0; l < synonymLists.length; l++) {
			System.out.println("   SYN: " + synonymLists[l].getValue());
			if (familyNode == null)
				continue;
			Annotation[] synonyms = Gamta.extractAllMatches(synonymLists[l], rankEpithetPatterns.getProperty(FAMILY_ATTRIBUTE));
			if (synonyms.length == 0)
				continue;
			if (familyNode.synonyms == null)
				familyNode.synonyms = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int s = 0; s < synonyms.length; s++)
				familyNode.synonyms.add(synonyms[s].firstValue());
		}
		
		//	get genus names and links
		QueriableAnnotation[] genera = GPath.evaluatePath(genusListDoc, "//a[contains(./@href, '/taxpage/0/genus/')]", null);
		for (int g = 0; g < genera.length; g++) {
			String genus = genera[g].firstValue();
			if (familyNode == null) {
				System.out.println("    " + genus);
				getSpeciesPlSyst(genera[g], null, nodeIndex);
				continue;
			}
			TaxonTreeNode genusNode = familyNode.getChild(genus);
			if (genusNode == null) {
				genusNode = new TaxonTreeNode(familyNode, genus, GENUS_ATTRIBUTE);
				familyNode.put(genus, genusNode);
				((Map) nodeIndex.get(genusNode.rank)).put(genusNode.epithet, genusNode);
			}
			getSpeciesPlSyst(genera[g], genusNode, nodeIndex);
		}
	}
	
	private static void getSpeciesPlSyst(QueriableAnnotation genus, TaxonTreeNode genusNode, Map nodeIndex) throws Exception {
		
		//	get data
		MutableAnnotation speciesListDoc = getDocumentPlSyst(((String) genus.getAttribute("href")), false);
		
		//	get synonyms
		QueriableAnnotation[] synonymLists = GPath.evaluatePath(speciesListDoc, "//center[./#first = 'syn']", null);
		for (int l = 0; l < synonymLists.length; l++) {
			System.out.println("   SYN: " + synonymLists[l].getValue());
			if (genusNode == null)
				continue;
			Annotation[] synonyms = Gamta.extractAllMatches(synonymLists[l], "[A-Z][a-z]+");
			if (synonyms.length == 0)
				continue;
			if (genusNode.synonyms == null)
				genusNode.synonyms = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int s = 0; s < synonyms.length; s++)
				genusNode.synonyms.add(synonyms[s].firstValue());
		}
		
		//	get species names and links
		QueriableAnnotation[] species = GPath.evaluatePath(speciesListDoc, "//OPTION[contains(./@value, '/taxpage/0/binomial/')]", null);
		for (int s = 0; s < species.length; s++) {
			String spec = species[s].lastValue();
			if (genusNode == null) {
				System.out.println("    " + spec);
				continue;
			}
			TaxonTreeNode specNode = genusNode.getChild(spec);
			if (specNode == null) {
				specNode = new TaxonTreeNode(genusNode, spec, SPECIES_ATTRIBUTE);
				genusNode.put(spec, specNode);
				((Map) nodeIndex.get(specNode.rank)).put(specNode.epithet, specNode);
			}
		}
	}
	
	private static Grammar html = new Html();
	private static Parser parser = new Parser(html);
	private static MutableAnnotation getDocumentPlSyst(String link, final boolean debug) throws Exception {
		URL docUrl = new URL(baseUrlPlSyst + link);
		Reader docReader = new BufferedReader(new InputStreamReader(docUrl.openStream(), "Cp1252"));
		SgmlDocumentReader docBuilder = new SgmlDocumentReader(null, html, null, null, null, 0) {
			public void storeToken(String token, int treeDepth) throws IOException {
				if (token.startsWith("<OPTION VALUE="))
					token = ("<OPTION value=" + token.substring("<OPTION VALUE=".length()));
				if (debug) System.out.println("TOKEN: " + token);
				super.storeToken(token, treeDepth);
			}
		};
		parser.stream(docReader, docBuilder);
		docReader.close();
		docBuilder.close();
		return docBuilder.getDocument();
	}
	
	private static String baseUrlLit = "http://www.plantsystematics.org/reveal/pbio/fam/";
	private static String[] ranks = {
		PHYLUM_ATTRIBUTE,
		SUBPHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
		SUBCLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
		SUBORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
		SUBFAMILY_ATTRIBUTE,
		TRIBE_ATTRIBUTE,
		SUBTRIBE_ATTRIBUTE,
	};
	private static Properties rankEpithetPatterns = new Properties();
	static {
		rankEpithetPatterns.setProperty(PHYLUM_ATTRIBUTE, "[A-Z][a-z]+ophyta");
		rankEpithetPatterns.setProperty(SUBPHYLUM_ATTRIBUTE, "[A-Z][a-z]+ophytina");
		rankEpithetPatterns.setProperty(CLASS_ATTRIBUTE, "[A-Z][a-z]+op(sida|hyceae)");
		rankEpithetPatterns.setProperty(SUBCLASS_ATTRIBUTE, "[A-Z][a-z]+idae");
		rankEpithetPatterns.setProperty(ORDER_ATTRIBUTE, "[A-Z][a-z]+ales");
		rankEpithetPatterns.setProperty(SUBORDER_ATTRIBUTE, "[A-Z][a-z]+ineae");
		rankEpithetPatterns.setProperty(FAMILY_ATTRIBUTE, "[A-Z][a-z]+aceae");
		rankEpithetPatterns.setProperty(SUBFAMILY_ATTRIBUTE, "[A-Z][a-z]+oideae");
		rankEpithetPatterns.setProperty(TRIBE_ATTRIBUTE, "[A-Z][a-z]+eae");
		rankEpithetPatterns.setProperty(SUBTRIBE_ATTRIBUTE, "[A-Z][a-z]+inae");
	}
	
	/**
	 * @param args
	 */
	public static void mainLit(String[] args) throws Exception {
		
		//	fetch data
		Map dataByTypeGenus = new TreeMap();
		Map currentFamilyByTypeGenus = new TreeMap();
		for (char i = 'a'; i <= 'z'; i++) {
			System.out.println("Fetching data for '" + i + "'");
			fetchData(i, dataByTypeGenus, currentFamilyByTypeGenus);
		}
		
//		//	see what we got
//		for (Iterator tgit = dataByTypeGenus.keySet().iterator(); tgit.hasNext();) {
//			String typeGenus = ((String) tgit.next());
//			String currentFamily = ((String) currentFamilyByTypeGenus.get(typeGenus));
//			Map tgData = ((Map) dataByTypeGenus.get(typeGenus));
//			System.out.println(typeGenus + " (" + currentFamily + "): " + tgData);
//		}
		
		//	index data by family, aggregating over type genera
		Map dataByFamily = new TreeMap();
		for (Iterator tgit = dataByTypeGenus.keySet().iterator(); tgit.hasNext();) {
			String typeGenus = ((String) tgit.next());
			Map tgData = ((Map) dataByTypeGenus.get(typeGenus));
			String family = ((String) tgData.get(FAMILY_ATTRIBUTE));
			if (family == null)
				continue;
			Map fData = ((Map) dataByFamily.get(family));
			if (fData == null) {
				fData = new TreeMap();
				dataByFamily.put(family, fData);
			}
			for (int r = 0; r < ranks.length; r++) {
				String epithet = ((String) tgData.get(ranks[r]));
				if (epithet != null)
					fData.put(ranks[r], epithet);
				if (ranks[r] == FAMILY_ATTRIBUTE)
					break;
			}
		}
		for (int r = (ranks.length - 1); r > 0; r--) {// nothing to infer from phylum
			System.out.println("Doing inference from " + ranks[r]);
			inferHigherRanks(ranks[r], dataByTypeGenus);
		}
		
//		//	see what we've got
//		for (Iterator fit = dataByFamily.keySet().iterator(); fit.hasNext();) {
//			String family = ((String) fit.next());
//			Map fData = ((Map) dataByFamily.get(family));
//			System.out.println(family + ": " + fData);
//		}
		
		//	if no family given, use current family to infer higher hierarchy
		int hierarchyCount = 0;
		for (Iterator tgit = dataByTypeGenus.keySet().iterator(); tgit.hasNext();) {
			String typeGenus = ((String) tgit.next());
			Map tgData = ((Map) dataByTypeGenus.get(typeGenus));
			if (!tgData.containsKey(FAMILY_ATTRIBUTE)) {
				String currentFamily = ((String) currentFamilyByTypeGenus.get(typeGenus));
				Map fData = ((currentFamily == null) ? null : ((Map) dataByFamily.get(currentFamily)));
				if (fData != null)
					tgData.putAll(fData);
			}
			if (tgData.containsKey(ORDER_ATTRIBUTE)) {
//				System.out.println(typeGenus + ": " + tgData);
				hierarchyCount++;
			}
		}
		System.out.println("Got " + hierarchyCount + " hierarchies");
		
		//	arrange data by family
		dataByFamily.clear();
		for (Iterator tgit = dataByTypeGenus.keySet().iterator(); tgit.hasNext();) {
			String typeGenus = ((String) tgit.next());
			Map tgData = ((Map) dataByTypeGenus.get(typeGenus));
//			if (!tgData.containsKey(ORDER_ATTRIBUTE))
//				continue;
			String family = ((String) tgData.get(FAMILY_ATTRIBUTE));
			if (family == null)
				continue;
			Map fData = new TreeMap();
			dataByFamily.put(family, fData);
			for (int r = 0; r < ranks.length; r++) {
				String epithet = ((String) tgData.get(ranks[r]));
				if (epithet != null)
					fData.put(ranks[r], epithet);
				if (ranks[r] == FAMILY_ATTRIBUTE)
					break;
			}
		}
		
		//	see what we've got
//		Map epithetSetsByRank = new TreeMap();
		Map epithetCountsByRank = new TreeMap();
		for (Iterator fit = dataByFamily.keySet().iterator(); fit.hasNext();) {
			String family = ((String) fit.next());
			Map fData = ((Map) dataByFamily.get(family));
			System.out.println(family + ": " + fData);
			for (int r = 0; r < ranks.length; r++) {
				String epithet = ((String) fData.get(ranks[r]));
				if (epithet == null)
					continue;
//				Set rankEpithets = ((Set) epithetSetsByRank.get(ranks[r]));
				StringIndex rankEpithetCounts = ((StringIndex) epithetCountsByRank.get(ranks[r]));
				if (rankEpithetCounts == null) {
//					rankEpithets = new TreeSet();
//					epithetSetsByRank.put(ranks[r], rankEpithets);
					rankEpithetCounts = new StringIndex();
					epithetCountsByRank.put(ranks[r], rankEpithetCounts);
				}
//				rankEpithets.add(epithet);
				rankEpithetCounts.add(epithet);
				if (ranks[r] == FAMILY_ATTRIBUTE)
					break;
			}
		}
		System.out.println("Got " + dataByFamily.size() + " family hierarchies");
		for (int r = 0; r < ranks.length; r++) {
//			Set rankEpithets = ((Set) epithetSetsByRank.get(ranks[r]));
			StringIndex rankEpithetCounts = ((StringIndex) epithetCountsByRank.get(ranks[r]));
			if (rankEpithetCounts == null)
				continue;
			System.out.println("Got " + rankEpithetCounts.distinctSize() + " distinct " + ranks[r] + " epithets with " + rankEpithetCounts.size() + " occurrences");
			if (ranks[r] == FAMILY_ATTRIBUTE)
				break;
		}
	}
	
	private static void inferHigherRanks(String rank, Map dataByTypeGenus) {
		Map dataByRankEpithets = new TreeMap();
		for (Iterator tgit = dataByTypeGenus.keySet().iterator(); tgit.hasNext();) {
			String typeGenus = ((String) tgit.next());
			Map tgData = ((Map) dataByTypeGenus.get(typeGenus));
			String rankEpithet = ((String) tgData.get(rank));
			if (rankEpithet == null)
				continue;
			Map reData = ((Map) dataByRankEpithets.get(rankEpithet));
			if (reData == null) {
				reData = new TreeMap();
				dataByRankEpithets.put(rankEpithet, reData);
			}
			for (int r = 0; r < ranks.length; r++) {
				String epithet = ((String) tgData.get(ranks[r]));
				if (epithet != null)
					reData.put(ranks[r], epithet);
				if (ranks[r] == rank)
					break;
			}
		}
		for (Iterator tgit = dataByTypeGenus.keySet().iterator(); tgit.hasNext();) {
			String typeGenus = ((String) tgit.next());
			Map tgData = ((Map) dataByTypeGenus.get(typeGenus));
			String rankEpithet = ((String) tgData.get(rank));
			if (rankEpithet == null)
				continue;
			Map reData = ((Map) dataByRankEpithets.get(rankEpithet));
			if (reData == null)
				continue;
			for (int r = 0; r < ranks.length; r++) {
				String epithet = ((String) reData.get(ranks[r]));
				if (epithet != null)
					tgData.put(ranks[r], epithet);
				if (ranks[r] == rank)
					break;
			}
		}
	}
	
	private static void fetchData(char initial, Map dataByTypeGenus, Map currentFamilyByTypeGenus) throws IOException {
		
		//	get data
		URL dataUrl = new URL(baseUrlLit + "allspgfile" + Character.toUpperCase(initial) + ".html");
		Reader dataReader = new BufferedReader(new InputStreamReader(dataUrl.openStream(), "Cp1252"));
		MutableAnnotation dataDoc = SgmlDocumentReader.readDocument(dataReader);
		dataReader.close();
		
		//	do some cleanup
		AnnotationFilter.deleteAnnotations(dataDoc, "head");
		AnnotationFilter.removeAnnotationAttribute(dataDoc, "p", "class");
		AnnotationFilter.removeAnnotationAttribute(dataDoc, "p", "style");
		AnnotationFilter.removeAnnotationAttribute(dataDoc, "span", "class");
		AnnotationFilter.removeAnnotationAttribute(dataDoc, "span", "style");
		AnnotationFilter.renameAnnotations(dataDoc, "p", MutableAnnotation.PARAGRAPH_TYPE);
		
		//	annotate rank epithets
		for (int r = 0; r < ranks.length; r++)
			annotateEpithets(dataDoc, ranks[r]);
		AnnotationFilter.removeContained(dataDoc, SUBORDER_ATTRIBUTE, TRIBE_ATTRIBUTE);
		AnnotationFilter.removeContained(dataDoc, FAMILY_ATTRIBUTE, TRIBE_ATTRIBUTE);
		AnnotationFilter.removeContained(dataDoc, SUBFAMILY_ATTRIBUTE, TRIBE_ATTRIBUTE);
		
		//	annotate type genera and current families
		Annotation[] typeGenera = Gamta.extractAllMatches(dataDoc, "T\\:\\s*[A-Z][a-z]+");
		for (int g = 0; g < typeGenera.length; g++)
			dataDoc.addAnnotation("typeGenus", typeGenera[g].getStartIndex(), typeGenera[g].size());
		Annotation[] currentFamilies = Gamta.extractAllMatches(dataDoc, "[\\-\\u2012-\\u2015\\u2053]\\s*[A-Z][a-z]+aceae");
		for (int f = 0; f < currentFamilies.length; f++)
			dataDoc.addAnnotation("currentFamily", currentFamilies[f].getStartIndex(), currentFamilies[f].size());
		
		//	collect data by type genus
		MutableAnnotation[] paragraphs = dataDoc.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			Annotation[] typeGenus = paragraphs[p].getAnnotations("typeGenus");
			if (typeGenus.length != 1)
				continue;
			String typeGenusKey = TokenSequenceUtils.concatTokens(typeGenus[0], true, true);
			Map data = ((Map) dataByTypeGenus.get(typeGenusKey));
			if (data == null) {
				data = new TreeMap();
				dataByTypeGenus.put(typeGenusKey, data);
			}
			for (int r = 0; r < ranks.length; r++) {
				Annotation[] epithets = paragraphs[p].getAnnotations(ranks[r]);
				if (epithets.length == 0)
					continue;
				if (epithets[0].getStartIndex() == 0)
					data.put(ranks[r], epithets[0].getValue());
			}
			MutableAnnotation[] currentFamily = paragraphs[p].getMutableAnnotations("currentFamily");
			if (currentFamily.length != 1)
				continue;
			currentFamily = currentFamily[0].getMutableAnnotations(FAMILY_ATTRIBUTE);
			if (currentFamily.length != 0) {
				currentFamilyByTypeGenus.put(typeGenusKey, currentFamily[0].getValue());
//				data.put(FAMILY_ATTRIBUTE, currentFamily[0].getValue());
			}
		}
	}
	private static void annotateEpithets(MutableAnnotation dataDoc, String rank) {
		Annotation[] epithets = Gamta.extractAllMatches(dataDoc, rankEpithetPatterns.getProperty(rank));
		for (int e = 0; e < epithets.length; e++)
			dataDoc.addAnnotation(rank, epithets[e].getStartIndex(), epithets[e].size());
	}
}
