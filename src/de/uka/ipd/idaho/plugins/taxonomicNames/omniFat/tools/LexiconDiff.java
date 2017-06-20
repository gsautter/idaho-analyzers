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

package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import de.uka.ipd.idaho.stringUtils.StringIndex;

/**
 * @author sautter
 *
 */
public class LexiconDiff {
	private static final int maxSuffixLength = 4;
	private static final double minFrequency = 0.001;
	private static final int minFrequencyRatio = 10;
	
	private static final boolean filterEpithetsByLanguage = true;
	private static final boolean printComparison = false;
	
	private static final Comparator suffixOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			String s1 = ((String) o1);
			String s2 = ((String) o2);
            for (int i1 = (s1.length()-1), i2 = (s2.length()-1), i = 1; (i1 > -1) && (i2 > -1); i1--, i2--, i++) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (c1 != c2)
                    return c1 - c2;
//                else if (i == maxSuffixLength)
//                	return s1.compareTo(s2);
            }
            return (s1.length() - s2.length());
		}
	};
	
	private static File dataPath = new File("E:/Testdaten/OmniFATData");
	public static void main(String[] args) throws Exception {
		LexiconStat higher = new LexiconStat("higher.static.list.txt");
		LexiconStat genus = new LexiconStat("genus.static.list.txt");
		LexiconStat species = new LexiconStat("species.static.list.txt");
		LexiconStat[] rankGroupEpithets = {higher, genus, species};
		TreeSet epithetFilterSuffixes = new TreeSet();
		TreeSet epithetFilters = new TreeSet(suffixOrder);
		TreeSet[] nonNegativeEpithets = {new TreeSet(suffixOrder), new TreeSet(suffixOrder), new TreeSet(suffixOrder)};
		
		LexiconStat english = new LexiconStat("EnglishWord.list.txt");
		LexiconStat german = new LexiconStat("GermanWord.list.txt");
		LexiconStat[] negative = {english, german};
//		LexiconStat latin = new LexiconStat("LatinWord.list.txt");
//		LexiconStat[] negative = {english, german, latin};
		
		/*
		 * TODO extend this program to:
		 * 
		 * - extract negative endings from dictionary analysis (like now)
		 * - generate respective pattern XML elements for OmniFAT config file
		 * - generate trusted counter dictionaries (positive.<rankGroup>.txt), small enough for manual cleansing
		 * - generate respectice dictionary XML elements for OmniFAT config file
		 * 
		 * ==> find sensible thresholds
		 */ 
		
		if (filterEpithetsByLanguage) {
			HashMap frequencyRatioMap = new HashMap();
			
			for (int n = 0; n < negative.length; n++) {
				System.out.println();
				System.out.println("======= " + negative[n].name + " =========");
				System.out.println();
				
//				final LexiconStat stat = negative[n];
				for (Iterator eit = negative[n].suffixesCountSorted.iterator(); eit.hasNext();) {
					String suffix = ((String) eit.next());
					if (negative[n].getFrequency(suffix) < minFrequency)
						break;
					
					System.out.println("suffix: " + suffix);
					doComparison(suffix, negative[n], rankGroupEpithets, frequencyRatioMap, printComparison);
				}
//				
//				System.out.println("======= GOOD SUFFIXES =========");
//				
//				for (Iterator git = goodSet.iterator(); git.hasNext();) {
//					String goodSuffix = ((String) git.next()).trim();
//					String cString = ("" + goodMap.get(goodSuffix) + " - " + negative[n].suffixCounts.getCount(goodSuffix));
//					for (int r = 0; r < rankGroupEpithets.length; r++)
//						cString += (" / " + rankGroupEpithets[r].suffixCounts.getCount(goodSuffix)); 
//					System.out.println("good suffix: " + prepareSuffix(goodSuffix, maxSuffixLength) + " (" + cString + ")");
//					if (isSuffixEligible(goodSuffix, epithetFilterSuffixes, goodSet, rankGroupEpithets)) {
//						epithetFilterSuffixes.add(goodSuffix);
//						epithetFilters.add("<pattern type=\"negative\" string=\".*" + goodSuffix + "\"/>");
//						System.out.println(" ==> filter: <pattern type=\"negative\" string=\".*" + goodSuffix + "\"/>");
//						for (int r = 0; r < rankGroupEpithets.length; r++) {
//							for (Iterator rit = rankGroupEpithets[r].entries.tailSet(goodSuffix).iterator(); rit.hasNext();) {
//								String suffixed = ((String) rit.next());
//								if (suffixed.endsWith(goodSuffix)) {
//									nonNegativeEpithets[r].add(suffixed);
//								}
//								else break;
//							}
//						}
//					}
////					for (int r = 0; r < rankGroupEpithets.length; r++) {
//////						System.out.println("  " + rankGroupEpithets[r].name);
////						for (Iterator rit = rankGroupEpithets[r].entries.tailSet(goodSuffix).iterator(); rit.hasNext();) {
////							String suffixed = ((String) rit.next());
////							if (suffixed.endsWith(goodSuffix)) {
//////								System.out.println("    " + suffixed);
////								nonNegativeEpithets[r].add(suffixed);
////							}
////							else break;
////						}
//////						System.out.println("    " + rankGroupEpithets[r].suffixCounts.getCount(goodSuffix));
//////						if (ranks[r].suffixCounts.getCount(goodSuffix) <= 25) {
//////							for (Iterator rit = ranks[r].entries.tailSet(goodSuffix).iterator(); rit.hasNext();) {
//////								String suffixed = ((String) rit.next());
//////								if (suffixed.endsWith(goodSuffix))
//////									System.out.println("    " + suffixed);
//////								else break;
//////							}
//////						}
//////						else System.out.println("    " + ranks[r].suffixCounts.getCount(goodSuffix));
////					}
//				}
//				TreeSet goodSet = new TreeSet();
//				for (Iterator git = goodMap.keySet().iterator(); git.hasNext();) {
//					String goodSuffix = prepareSuffix((String) git.next());
//					goodSet.add(goodSuffix);
//				}
//				for (Iterator git = goodSet.iterator(); git.hasNext();) {
//					String goodSuffix = ((String) git.next()).trim();
//					String cString = ("" + goodMap.get(goodSuffix) + " - " + negative[n].suffixCounts.getCount(goodSuffix));
//					for (int r = 0; r < ranks.length; r++)
//						cString += (" / " + ranks[r].suffixCounts.getCount(goodSuffix)); 
//					System.out.println("good suffix: " + prepareSuffix(goodSuffix) + " (" + cString + ")");
//				}
//				TreeSet goodSet = new TreeSet(new Comparator() {
//					public int compare(Object o1, Object o2) {
//						int c = ((Double) goodMap.get(o2)).compareTo((Double) goodMap.get(o1));
//						if (c == 0) c = (stat.suffixCounts.getCount((String) o2) - stat.suffixCounts.getCount((String) o1));
//						return ((c == 0) ? ((String) o1).compareTo((String) o2) : c);
//					}
//				});
//				goodSet.addAll(goodMap.keySet());
//				for (Iterator git = goodSet.iterator(); git.hasNext();) {
//					String goodSuffix = ((String) git.next());
//					String cString = ("" + goodMap.get(goodSuffix) + " - " + negative[n].suffixCounts.getCount(goodSuffix));
//					for (int r = 0; r < ranks.length; r++)
//						cString += (" / " + ranks[r].suffixCounts.getCount(goodSuffix)); 
//					System.out.println("good suffix: " + prepareSuffix(goodSuffix) + " (" + cString + ")");
//				}
			}
			
			
			System.out.println();
			System.out.println("======= GOOD SUFFIXES =========");
			System.out.println();
			
			TreeSet goodSet = new TreeSet(suffixOrder);
			goodSet.addAll(frequencyRatioMap.keySet());
			
			for (Iterator git = goodSet.iterator(); git.hasNext();) {
				String goodSuffix = ((String) git.next()).trim();
				int count = 0;
				for (int n = 0; n < negative.length; n++)
					count += negative[n].suffixCounts.getCount(goodSuffix);
				String cString = ("" + frequencyRatioMap.get(goodSuffix) + " - " + count);
				for (int r = 0; r < rankGroupEpithets.length; r++)
					cString += (" / " + rankGroupEpithets[r].suffixCounts.getCount(goodSuffix)); 
				System.out.println("good suffix: " + prepareSuffix(goodSuffix, maxSuffixLength) + " (" + cString + ")");
				if (isSuffixEligible(goodSuffix, epithetFilterSuffixes, goodSet, rankGroupEpithets)) {
					epithetFilterSuffixes.add(goodSuffix);
					epithetFilters.add("<pattern type=\"negative\" string=\".*" + goodSuffix + "\"/>");
					System.out.println(" ==> filter: <pattern type=\"negative\" string=\".*" + goodSuffix + "\"/>");
					for (int r = 0; r < rankGroupEpithets.length; r++) {
						for (Iterator rit = rankGroupEpithets[r].entries.tailSet(goodSuffix).iterator(); rit.hasNext();) {
							String suffixed = ((String) rit.next());
							if (suffixed.endsWith(goodSuffix)) {
								nonNegativeEpithets[r].add(suffixed);
							}
							else break;
						}
					}
				}
			}
			
			store("testJoint.filters.txt", epithetFilters);
			for (int r = 0; r < rankGroupEpithets.length; r++) {
				String name = rankGroupEpithets[r].name.substring(0, rankGroupEpithets[r].name.indexOf('.'));
				store(("testJoint." + name + ".positive.list.txt"), nonNegativeEpithets[r]);
			}
		}
		
		else {
			for (int r = 0; r < rankGroupEpithets.length; r++) {
				System.out.println();
				System.out.println("======= " + rankGroupEpithets[r].name + " =========");
				System.out.println();
				
				final LexiconStat stat = rankGroupEpithets[r];
				final HashMap goodMap = new HashMap();
				for (Iterator eit = rankGroupEpithets[r].suffixesCountSorted.iterator(); eit.hasNext();) {
					String suffix = ((String) eit.next());
					if (rankGroupEpithets[r].getFrequency(suffix) < minFrequency)
						break;
					
					System.out.println("suffix: " + suffix);
					doComparison(suffix, rankGroupEpithets[r], negative, goodMap, printComparison);
				}
				
				System.out.println("======= GOOD SUFFIXES =========");
				
				TreeSet goodSet = new TreeSet(suffixOrder);
				goodSet.addAll(goodMap.keySet());
//				TreeSet goodSet = new TreeSet();
//				for (Iterator git = goodMap.keySet().iterator(); git.hasNext();) {
//					String goodSuffix = prepareSuffix((String) git.next());
//					goodSet.add(goodSuffix);
//				}
				for (Iterator git = goodSet.iterator(); git.hasNext();) {
					String goodSuffix = ((String) git.next()).trim();
					String cString = ("" + goodMap.get(goodSuffix) + " - " + rankGroupEpithets[r].suffixCounts.getCount(goodSuffix));
					for (int n = 0; n < negative.length; n++)
						cString += (" / " + negative[n].suffixCounts.getCount(goodSuffix)); 
					System.out.println("good suffix: " + prepareSuffix(goodSuffix, maxSuffixLength) + " (" + cString + ")");
				}
//				TreeSet goodSet = new TreeSet(new Comparator() {
//					public int compare(Object o1, Object o2) {
//						int c = ((Double) goodMap.get(o2)).compareTo((Double) goodMap.get(o1));
//						if (c == 0) c = (stat.suffixCounts.getCount((String) o2) - stat.suffixCounts.getCount((String) o1));
//						return ((c == 0) ? ((String) o1).compareTo((String) o2) : c);
//					}
//				});
//				goodSet.addAll(goodMap.keySet());
//				for (Iterator git = goodSet.iterator(); git.hasNext();) {
//					String goodSuffix = ((String) git.next());
//					String cString = ("" + goodMap.get(goodSuffix) + " - " + ranks[r].suffixCounts.getCount(goodSuffix));
//					for (int n = 0; n < negative.length; n++)
//						cString += (" / " + negative[n].suffixCounts.getCount(goodSuffix)); 
//					System.out.println("good suffix: " + prepareSuffix(goodSuffix) + " (" + cString + ")");
//				}
			}
		}
	}
	
	static boolean isSuffixEligible(String suffix, TreeSet suffixesSoFar, TreeSet allSuffixes, LexiconStat[] stats) {
		for (int s = 1; s < suffix.length(); s++)
			if (suffixesSoFar.contains(suffix.substring(s)))
				return false;
		
		int maxCount = 0;
		for (int s = 0; s < stats.length; s++)
			maxCount = Math.max(maxCount, stats[s].suffixCounts.getCount(suffix));
		if (maxCount < 100)
			return true;
		
		int prefixCount = 0;
		for (char ch = 'a'; ch <= 'z'; ch++) {
			if (allSuffixes.contains(ch + suffix))
				prefixCount++;
		}
		return (prefixCount > 13); // more than half the alphabeth
	}
	
	static void doComparison(String suffix, LexiconStat suffixHost, LexiconStat[] comparisons, Map goodMap, boolean print) {
		double sFreq = suffixHost.getFrequency(suffix);
		double minRatio = Double.MAX_VALUE;
		if (print) System.out.println(" - " + suffixHost.name + ": " + sFreq);
		for (int c = 0; c < comparisons.length; c++) {
			double cFreq = comparisons[c].getFrequency(suffix);
			if (print) System.out.println(" - " + comparisons[c].name + ": " + cFreq);
			if (cFreq != 0)
				minRatio = Math.min(minRatio, (sFreq / cFreq));
		}
		if (minFrequencyRatio < minRatio) {
			if (goodMap.containsKey(suffix)) {
				Double minRatioOld = ((Double) goodMap.get(suffix));
				goodMap.put(suffix, new Double(Math.max(minRatio, minRatioOld.doubleValue())));
			}
			else goodMap.put(suffix, new Double(minRatio));
		}
	}
	
	static String prepareSuffix(String suffix, int forLength) {
		while (suffix.length() < forLength)
			suffix = (" " + suffix);
		return suffix;
	}
	
	static void store(String name, TreeSet data) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dataPath, name)), "UTF-8"));
		for (Iterator sit = data.iterator(); sit.hasNext();) {
			String s = ((String) sit.next());
			bw.write(s);
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	
	static class LexiconStat {
		String name;
		TreeSet entries = new TreeSet(suffixOrder);
		int maxEntryLength = 0;
		TreeSet suffixes = new TreeSet();
		StringIndex suffixCounts = new StringIndex();
		TreeSet suffixesCountSorted;
		int wordCount = 0;
		LexiconStat(String name) throws IOException {
			this.name = name;
			BufferedReader lexiconReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dataPath, name)), "UTF-8"));
			String lexiconEntry;
			while ((lexiconEntry = lexiconReader.readLine()) != null) {
				lexiconEntry = lexiconEntry.trim();
				if ((lexiconEntry.length() != 0) && !lexiconEntry.startsWith("//")) {
					this.entries.add(lexiconEntry);
					for (int s = 0; s <= Math.min(lexiconEntry.length()-1, maxSuffixLength); s++) {
						String suffix = lexiconEntry.substring(lexiconEntry.length() - s);
						this.suffixes.add(suffix);
						this.suffixCounts.add(suffix);
					}
					this.maxEntryLength = Math.max(lexiconEntry.length(), this.maxEntryLength);
				}
			}
			lexiconReader.close();
			this.suffixesCountSorted = new TreeSet(new Comparator() {
				public int compare(Object o1, Object o2) {
					int c = (suffixCounts.getCount(o2.toString()) - suffixCounts.getCount(o1.toString()));
					return ((c == 0) ? ((String) o1).compareTo((String) o2) : c);
				}
			});
			this.suffixesCountSorted.addAll(this.suffixes);
			this.wordCount = this.suffixCounts.getCount("");
			System.out.println("dictionary loaded: " + name);
		}
		double getFrequency(String suffix) {
			if (this.wordCount == 0)
				return 0;
			return (((double) this.suffixCounts.getCount(suffix)) / this.wordCount);
		}
	}
}
