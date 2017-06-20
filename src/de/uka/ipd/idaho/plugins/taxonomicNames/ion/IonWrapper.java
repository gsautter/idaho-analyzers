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
package de.uka.ipd.idaho.plugins.taxonomicNames.ion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTreeNode;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameUtils.TaxonomicName;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;

/**
 * Wrapper for Index of Organism Names (ION), page-scraping http://www.organismnames.com
 * into a Java API.
 * 
 * @author sautter
 */
public class IonWrapper implements TaxonomicNameConstants {
	
	private AnalyzerDataProvider dataProvider;
	private TaxonomicRankSystem rankSystem = TaxonomicRankSystem.getRankSystem(null);
	private Rank[] ranks = this.rankSystem.getRanks();
	
	/**
	 * Constructor
	 * @param dataProvider the data provider to use for local caching and for
	 *            obtaining URLs for online lookups
	 */
	public IonWrapper(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}
	
	private HashMap hierarchyCache = new HashMap();
	private HashMap childrenCache = new HashMap();
	
	/**
	 * Obtain the higher taxonomic ranks for a given genus from ION. If ION
	 * does not provide the higher ranks for the specified genus, or if the ION
	 * server is unreachable and the higher ranks for the specified genus are
	 * not cached, this method returns null. This method actually also works
	 * for epithets of ranks above the genus.
	 * @param genus the genus to get the higher ranks for
	 * @param allowWebAccess allow downloading data from ION in case of a file
	 *            cache miss?
	 * @return a Properties object containing the higher taxonomic ranks for the
	 *         argument genus
	 */
	public Properties getHierarchy(String genus, boolean allowWebAccess) {
		Properties hierarchy = ((Properties) this.hierarchyCache.get(genus));
		if (hierarchy != null)
			return hierarchy;
		
		String cacheDataName = ("cache/" + genus + ".h.txt");
		
		if (this.dataProvider.isDataAvailable(cacheDataName)) try {
			BufferedReader cacheReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8"));
			for (String line; (line = cacheReader.readLine()) != null;) {
				int split = line.indexOf('=');
				if (split == -1)
					continue;
				String rank = line.substring(0, split);
				String epithet = line.substring(split + 1);
				if (hierarchy == null)
					hierarchy = new Properties();
				hierarchy.setProperty(rank, epithet);
			}
		}
		catch (IOException ioe) {
			System.out.println("Error loading cached data for genus '" + genus + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			hierarchy = null;
		}
		
		if (hierarchy != null) {
			this.hierarchyCache.put(genus, hierarchy);
			return hierarchy;
		}
		
		if (allowWebAccess) try {
			hierarchy = loadHierarchy(genus);
		}
		catch (IOException ioe) {
			System.out.println("Error loading cached hierarchy for genus '" + genus + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			hierarchy = null;
		}
		catch (Exception e) {
			System.out.println("Error loading cached hierarchy for genus '" + genus + "': " + e.getMessage());
			e.printStackTrace(System.out);
			hierarchy = null;
		}
		
		if (hierarchy != null) {
			if (this.dataProvider.isDataEditable()) try {
				BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(cacheDataName), "UTF-8"));
				for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
					String rank = ((String) rit.next());
					String epithet = hierarchy.getProperty(rank);
					cacheWriter.write(rank + "=" + epithet);
					cacheWriter.newLine();
				}
				cacheWriter.flush();
				cacheWriter.close();
			}
			catch (IOException ioe) {
				System.out.println("Error caching hierarchy for genus '" + genus + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
			this.hierarchyCache.put(genus, hierarchy);
		}
		
		return hierarchy;
	}
	
	private Properties loadHierarchy(String genus) throws IOException {
		if (genus == null)
			return null;
		
		String hUrlString = (baseUrl + hierarchyQueryBase + URLEncoder.encode(genus, "UTF-8"));
		System.out.println(hUrlString);
		URL hUrl = this.dataProvider.getURL(hUrlString);
		BufferedReader hBr = new BufferedReader(new InputStreamReader(hUrl.openStream(), "UTF-8"));
		QueriableAnnotation hierarchyDoc = SgmlDocumentReader.readDocument(hBr);
		hBr.close();
		QueriableAnnotation[] tables = hierarchyDoc.getAnnotations("table");
		String hLink = null;
		for (int t = 0; t < tables.length; t++) {
			if (!"resultstable".equalsIgnoreCase((String) tables[t].getAttribute("class")))
				continue;
			QueriableAnnotation[] rows = tables[t].getAnnotations("tr");
			for (int r = 0; r < rows.length; r++) {
				if (!genus.equalsIgnoreCase(rows[r].firstValue()))
					continue;
				QueriableAnnotation[] links = rows[r].getAnnotations("a");
				for (int l = 0; l < links.length; l++)
					if (genus.equalsIgnoreCase(links[l].getValue())) {
						hLink = ((String) links[l].getAttribute("href"));
						break;
					}
				if (hLink != null)
					break;
			}
			if (hLink != null)
				break;
		}
		System.out.println(hLink);
		if (hLink == null)
			return null;
		
		hUrlString = (baseUrl + hLink);
		System.out.println(hUrlString);
		hUrl = this.dataProvider.getURL(hUrlString);
		hBr = new BufferedReader(new InputStreamReader(hUrl.openStream(), "UTF-8"));
		hierarchyDoc = SgmlDocumentReader.readDocument(hBr);
		hBr.close();
		tables = hierarchyDoc.getAnnotations("table");
		for (int t = 0; t < tables.length; t++) {
			if ("portal-columns".equalsIgnoreCase((String) tables[t].getAttribute("id")))
				continue;
			int tableScore = 0;
			if (TokenSequenceUtils.indexOf(tables[t], "Kingdom") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Phylum") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Class") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Order") != -1)
				tableScore++;
			if (TokenSequenceUtils.indexOf(tables[t], "Family") != -1)
				tableScore++;
			if (tableScore < 4)
				continue;
			Properties hierarchy = new Properties();
			QueriableAnnotation[] divs = tables[t].getAnnotations("div");
			for (int d = 0; d < divs.length; d++) {
				String epithet = divs[d].valueAt(0);
				String rank = divs[d].valueAt(2);
				hierarchy.setProperty(rank, epithet);
			}
			return hierarchy;
		}
		
		return null;
	}
	
	/**
	 * Obtain the children of a given genus from ION. If ION does not provide
	 * know the argument genus, or if the ION server is unreachable and the
	 * children of the specified genus are not cached, this method returns null.
	 * @param genus the genus to get the children for
	 * @param allowWebAccess allow downloading data from ION in case of a file
	 *            cache miss?
	 * @return an array holding the children of the argument genus
	 */
	public TaxonomicName[] getChildren(String genus, boolean allowWebAccess) throws IOException {
		TaxonomicName[] children = ((TaxonomicName[]) this.childrenCache.get(genus));
		if (children != null)
			return children;
		
		String cacheDataName = ("cache/" + genus + ".c.txt");
		
		if (this.dataProvider.isDataAvailable(cacheDataName)) try {
			BufferedReader cacheReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8"));
			ArrayList childrenList = new ArrayList();
			TaxonomicName taxName = null;
			for (String line; (line = cacheReader.readLine()) != null;) {
				int split = line.indexOf('=');
				if (split == -1) {
					if (taxName != null)
						childrenList.add(taxName);
					taxName = new TaxonomicName(this.rankSystem);
					continue;
				}
				String rank = line.substring(0, split).trim();
				String epithet = line.substring(split + 1).trim();
				if (AUTHORITY_ATTRIBUTE.equals(rank)) {
					if (epithet.matches((epithet.length() < 5) ? "[12][0-9]{3}" : ".*\\s[12][0-9]{3}")) {
						taxName.setAuthorityYear(Integer.parseInt(epithet.substring(epithet.lastIndexOf(' ') + 1)));
						if (epithet.lastIndexOf(' ') != -1)
							taxName.setAuthorityName(epithet.substring(0, epithet.lastIndexOf(' ')).trim());
					}
					else taxName.setAuthorityName(epithet);
				}
				else taxName.setEpithet(rank, epithet);
			}
			if (taxName != null)
				childrenList.add(taxName);
			if (childrenList.size() != 0) {
				children = ((TaxonomicName[]) childrenList.toArray(new TaxonomicName[childrenList.size()]));
				this.childrenCache.put(genus, children);
				return children;
			}
		}
		catch (IOException ioe) {
			System.out.println("Error loading cached children for genus '" + genus + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			children = null;
		}
		
		if (allowWebAccess) try {
			children = loadChildren(genus);
		}
		catch (IOException ioe) {
			System.out.println("Error loading cached children for genus '" + genus + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			children = null;
		}
		catch (Exception e) {
			System.out.println("Error loading cached children for genus '" + genus + "': " + e.getMessage());
			e.printStackTrace(System.out);
			children = null;
		}
		
		if (children != null) {
			if (this.dataProvider.isDataEditable()) try {
				BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(cacheDataName), "UTF-8"));
				for (int c = 0; c < children.length; c++) {
					cacheWriter.write(children[c].toString());
					cacheWriter.newLine();
					for (int r = 0; r < this.ranks.length; r++) {
						String epithet = children[c].getEpithet(this.ranks[r].name);
						if (epithet != null) {
							cacheWriter.write("  " + this.ranks[r].name + "=" + epithet);
							cacheWriter.newLine();
						}
					}
					String authority = children[c].getAuthority();
					if (authority != null) {
						cacheWriter.write("  " + AUTHORITY_ATTRIBUTE + "=" + authority);
						cacheWriter.newLine();
					}
				}
				cacheWriter.flush();
				cacheWriter.close();
			}
			catch (IOException ioe) {
				System.out.println("Error caching children for genus '" + genus + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
			this.hierarchyCache.put(genus, children);
		}
		
		return children;
	}
	
	private TaxonomicName[] loadChildren(String genus) throws IOException {
		if (genus == null)
			return null;
		
		TreeMap cMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		String cUrlString = (baseUrl + childrenQueryBase + URLEncoder.encode(genus, "UTF-8"));
		System.out.println(cUrlString);
		URL cUrl = this.dataProvider.getURL(cUrlString);
		BufferedReader cBr = new BufferedReader(new InputStreamReader(cUrl.openStream(), "UTF-8"));
		QueriableAnnotation childrenDoc = SgmlDocumentReader.readDocument(cBr);
		cBr.close();
		QueriableAnnotation[] tables = childrenDoc.getAnnotations("table");
		for (int t = 0; t < tables.length; t++) {
			if (!"resultstable".equalsIgnoreCase((String) tables[t].getAttribute("class")))
				continue;
			
			QueriableAnnotation[] rows = tables[t].getAnnotations("tr");
			for (int r = 0; r < rows.length; r++) {
				System.out.println(rows[r].toXML());
				if (!genus.equalsIgnoreCase(rows[r].firstValue()))
					continue;
				if (!"erow".equals(rows[r].getAttribute("class")) && !"orow".equals(rows[r].getAttribute("class"))) {
					System.out.println(" ==> wrong class");
					continue;
				}
				
				QueriableAnnotation[] cells = rows[r].getAnnotations("td");
				if (cells.length > 2) {
					System.out.println(" ==> wrong cell count: " + cells.length);
					continue;
				}
				if (forbiddenEndings.contains(cells[0].lastValue().substring(cells[0].lastValue().indexOf('-') + 1))) {
					System.out.println(" ==> invalid last epithet");
					continue;
				}
				String taxNameString = cells[0].getValue();
				if (taxNameString.indexOf(" x ") != -1) {
					System.out.println(" ==> hybrid");
					continue;
				}
				TaxonomicName taxName = ((TaxonomicName) cMap.get(taxNameString));
				if (taxName == null) {
					taxName = this.parseTaxName(cells[0], genus);
					if (taxName == null)
						continue;
					cMap.put(taxNameString, taxName);
				}
				if (cells.length < 2)
					continue;
				if ((taxName.getAuthorityName() != null) && (taxName.getAuthorityYear() > 0))
					continue;
				String authorityString = IoTools.prepareForPlainText(cells[1].getValue());
				while (authorityString.startsWith("("))
					authorityString = authorityString.substring("(".length()).trim();
				while (authorityString.endsWith(")"))
					authorityString = authorityString.substring(0, (authorityString.length() - ")".length())).trim();
				if (authorityString.matches(".*\\s[12][0-9]{3}")) {
					if (taxName.getAuthorityYear() < 0)
						taxName.setAuthorityYear(Integer.parseInt(authorityString.substring(authorityString.lastIndexOf(' ')).trim()));
					authorityString = authorityString.substring(0, authorityString.lastIndexOf(' ')).trim();
				}
				if (authorityString.indexOf(" in ") != -1)
					authorityString = authorityString.substring(0, authorityString.indexOf(" in ")).trim();
				if (taxName.getAuthorityName() == null)
					taxName.setAuthorityName(authorityString);
			}
		}
		
		return ((TaxonomicName[]) cMap.values().toArray(new TaxonomicName[cMap.size()]));
	}
	private static final String genusPattern = "(\"[A-Z][a-z]+\")";
	private static final String subGenusPattern = "('(' \"[A-Z][a-z]?(\\\\.|[a-z]+)\" ')')?";
	private static final String speciesPattern = "(\"[A-Z]?[a-z\\\\-]+\")";
	private static final String subSpeciesPattern = "(\"(subspecies|subsp\\\\.|ssp\\\\.|stirps|st\\\\.|race|r\\\\.)\"? \"[A-Z]?[a-z\\\\-]+\")?";
	private static final String varietyPattern = "(\"(variety|var\\\\.|v\\\\.)\" \"[A-Z]?[a-z\\\\-]+\")?";
	private static final String taxNameGenusParsePattern = 
			genusPattern + " " +
			subGenusPattern;
	private static final String taxNameSpeciesParsePattern = 
			genusPattern + " " +
			subGenusPattern + " " +
			speciesPattern + " " +
			subSpeciesPattern + " " +
			varietyPattern;
	//	TODO_for_botany generate patterns from rank system to cover all ranks
	private static final HashMap patternsToRanks = new HashMap();
	static {
		patternsToRanks.put(genusPattern, GENUS_ATTRIBUTE);
		patternsToRanks.put(subGenusPattern, SUBGENUS_ATTRIBUTE);
		patternsToRanks.put(speciesPattern, SPECIES_ATTRIBUTE);
		patternsToRanks.put(subSpeciesPattern, SUBSPECIES_ATTRIBUTE);
		patternsToRanks.put(varietyPattern, VARIETY_ATTRIBUTE);
	}
	private static final TreeSet forbiddenEndings = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	static {
		forbiddenEndings.add("group");
		forbiddenEndings.add("subgroup");
		forbiddenEndings.add("complex");
		forbiddenEndings.add("species");
		forbiddenEndings.add("subspecies");
		forbiddenEndings.add("variety");
		forbiddenEndings.add("section");
		forbiddenEndings.add("subsection");
		forbiddenEndings.add("sp");
		forbiddenEndings.add("spp");
		forbiddenEndings.add("clade");
	}
	
	private TaxonomicName parseTaxName(QueriableAnnotation cell, String genus) {
		MatchTree[] taxNameParses = AnnotationPatternMatcher.getMatchTrees(cell, taxNameSpeciesParsePattern);
		if (taxNameParses.length == 0)
			taxNameParses = AnnotationPatternMatcher.getMatchTrees(cell, taxNameGenusParsePattern);
		if (taxNameParses.length == 0)
			return null;
		
		MatchTree bestTaxNameParse = null;
		for (int p = 0; p < taxNameParses.length; p++) {
			System.out.println(taxNameParses[p].getMatch().getValue());
			if (taxNameParses[p].getMatch().getStartIndex() != 0) {
				System.out.println(" ==> late start");
				continue;
			}
			if (forbiddenEndings.contains(taxNameParses[p].getMatch().lastValue().substring(taxNameParses[p].getMatch().lastValue().indexOf('-') + 1))) {
				System.out.println(" ==> invalid last epithet");
				continue;
			}
			if ((bestTaxNameParse == null) || (bestTaxNameParse.getMatch().size() < taxNameParses[p].getMatch().size())) {
				System.out.println(" ==> new best match");
				bestTaxNameParse = taxNameParses[p];
			}
		}
		if (bestTaxNameParse == null)
			return null;
		
		MatchTreeNode[] taxNameParseParts = bestTaxNameParse.getChildren();
		TaxonomicName taxName = null;
		for (int p = 0; p < taxNameParseParts.length; p++) {
			String rank = ((String) patternsToRanks.get(taxNameParseParts[p].getPattern()));
			if (rank == null)
				continue;
			Annotation parsePartMatch = taxNameParseParts[p].getMatch();
			String epithet = null;
			for (int v = parsePartMatch.size(); v > 0; v--)
				if (Gamta.isWord(parsePartMatch.valueAt(v-1))) {
					epithet = parsePartMatch.valueAt(v-1);
					break;
				}
			if (epithet == null)
				continue;
			if (taxName == null)
				taxName = new TaxonomicName(this.rankSystem);
			if ((epithet.length() < 3) && genus.startsWith(epithet))
				epithet = genus;
			taxName.setEpithet(rank, epithet);
		}
		
		if (taxName != null) {
			String rank = taxName.getRank();
			if (rank == null)
				return null;
			String mostSignificantEpithet = taxName.getEpithet(rank);
			if (mostSignificantEpithet.length() < 2)
				return null;
			boolean noVowels = true;
			for (int c = 0; c < mostSignificantEpithet.length(); c++)
				if ("aeiouyAEIOUY".indexOf(mostSignificantEpithet.substring(c, (c+1))) != -1) {
					noVowels = false;
					break;
				}
			if (noVowels)
				return null;
		}
		
		return taxName;
	}
	
	private static final String baseUrl = "http://www.organismnames.com/";
	private static final String hierarchyQueryBase = "query.htm?searchType=simple&so=a0&pp=100&q=";
	private static final String childrenQueryBase = "query.htm?searchType=simple&so=a0&pp=100000&q=";
//	
//	// !!! TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
//		IonWrapper ionHhp = new IonWrapper(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/IonWrapperData/")));
//		
//		String testGenus = "Anhuistoma";
//		Properties hierarchy = ionHhp.getHierarchy(testGenus, true);
//		if (hierarchy == null)
//			System.out.println("Hierarchy for '" + testGenus + " not found'");
//		else for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
//			String rank = ((String) rit.next());
//			System.out.println(rank + ": " + hierarchy.getProperty(rank));
//		}
//		TaxonomicName[] children = ionHhp.getChildren(testGenus, true);
//		if (children == null)
//			System.out.println("Children for '" + testGenus + " not found'");
//		else for (int c = 0; c < children.length; c++)
//			System.out.println(children[c].toString(true));
////		
////		QueriableAnnotation taxName = Gamta.newDocument(Gamta.newTokenSequence("Crematogaster (Test) longispina species group", Gamta.INNER_PUNCTUATION_TOKENIZER));
////		ionHhp.parseTaxName(taxName);
//	}
}