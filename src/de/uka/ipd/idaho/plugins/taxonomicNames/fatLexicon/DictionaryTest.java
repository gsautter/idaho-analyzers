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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class DictionaryTest {
	private static StringVector negativesDictionaryList = new StringVector();
//	private static StringVector negativesDictionary = new StringVector();
//	private static Set negativesDictionaryEntrySet = new TreeSet();
	
//	private static Set negativesDictionaryPartSet = new TreeSet(new Comparator() {
//		public int compare(Object o1, Object o2) {
//			String[] s1 = ((String[]) o1);
//			String[] s2 = ((String[]) o2);
//			
//			int c = s1[s1.length-1].compareTo(s2[0]);
//			if (c < 0) return -1;
//			
//			c = s2[s2.length-1].compareTo(s1[0]);
//			if (c < 0) return 1;
//			
//			return 0;
//		}
//	});
	private static int negativesDictionarySize = 0;
	private static Map negativesDictionaryPartsCs = new TreeMap(new Comparator() {
		public int compare(Object o1, Object o2) {
			String[] s1 = ((String[]) o1);
			String[] s2 = ((String[]) o2);
			
			int c = s1[s1.length-1].compareTo(s2[0]);
			if (c < 0) return -1;
			
			c = s2[s2.length-1].compareTo(s1[0]);
			if (c < 0) return 1;
			
			return 0;
		}
	});
	private static Dictionary negativesDictionarySet = new Dictionary() {
		public StringIterator getEntryIterator() {
			final Iterator it = negativesDictionaryPartsCs.entrySet().iterator();
			return new StringIterator() {
				Iterator pit = null;
				public boolean hasNext() {
					if (this.pit == null) {
						if (it.hasNext()) {
							String[] pitData = ((String[]) it.next());
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
						return this.pit.next();
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
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		public boolean isEmpty() {
			return negativesDictionaryPartsCs.isEmpty();
		}
		public boolean lookup(String string, boolean caseSensitive) {
			// TODO Auto-generated method stub
			return false;
		}
		public boolean lookup(String string) {
			String[] lookup = {string};
			String[] dChunk = ((String[]) negativesDictionaryPartsCs.get(lookup));
			if (dChunk == null) return false;
			int index = Arrays.binarySearch(dChunk, string);
			return ((index >= 0) && (index < dChunk.length) && dChunk[index].equals(string));
		}
		public int size() {
			return negativesDictionarySize;
		}
	};
//	private static Set negativesDictionaryPartSet = new HashSet();
	
	public static void main(String[] args) throws Exception {
		System.out.println(hash("a"));
		System.out.println(hash("z"));
		System.out.println(hash("aaaaaaaaaaaaaaaa"));
		System.out.println(hash("zzzzzzzzzzzzzzzz"));
//		if (true) return;
		
		Runtime rt = Runtime.getRuntime();
		
		AnalyzerDataProvider dp = new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/LexiconFATData/"));
		InputStream is;
		
		is = dp.getInputStream("negativesDictionaryList.cnfg");
		negativesDictionaryList = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
		System.out.println("FAT: Got Negative Dictionary List, needing " + (rt.totalMemory() - rt.freeMemory()));
		is.close();
		
		int deCount = 0;
		int deSize = 0;
		int deCountNoHash = 0;
		Set negativesDictionaryEntrySet = new TreeSet();
		
		for (int n = 0; n < negativesDictionaryList.size(); n++) {
			String negativesDictionaryName = negativesDictionaryList.get(n);
			if (!negativesDictionaryName.startsWith("//")) try {
				
				if (negativesDictionaryName.startsWith("http://"))
					is = dp.getURL(negativesDictionaryName).openStream();
				else is = dp.getInputStream(negativesDictionaryName);
				
				BufferedReader dr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String de;
				while ((de = dr.readLine()) != null) {
					deCount++;
					deSize += de.length();
					negativesDictionaryEntrySet.add(de);
				}
				
//				StringVector negativesDictionary = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
//				
//				for (StringIterator dit = negativesDictionary.getEntryIterator(); dit.hasMoreStrings();)
//					DictionaryTest.negativesDictionary.add(dit.nextString());
//				
////				DictionaryTest.negativesDictionary.addContentIgnoreDuplicates(negativesDictionary);
				
				is.close();
				
				System.out.println(" ==> added Negative Dictionary '" + negativesDictionaryName + "'");
			}
			catch (IOException ioe) {
				System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading negatives dictionary '" + negativesDictionaryName + "'.");
			}
		}
		negativesDictionaryEntrySet.remove("");
		System.out.println("Loaded " + deCount + " entries (" + negativesDictionaryEntrySet.size() + " distinct) with " + deSize + " chars, needing " + (rt.totalMemory() - rt.freeMemory()));
		
		int dChunkSize = 1020; // a little less than 1024, to leave space for array length
		ArrayList negativesDictionaryEntryCollector = new ArrayList(dChunkSize);
		deCount = 0;
		deSize = 0;
		for (Iterator it = negativesDictionaryEntrySet.iterator(); it.hasNext();) {
			String de = ((String) it.next());
			deCount++;
			deSize += de.length();
			if (hash(de) == -2) {
				deCountNoHash++;
//				System.out.println(de);
			}
			negativesDictionaryEntryCollector.add(de);
			if (negativesDictionaryEntryCollector.size() == dChunkSize) {
				String[] dChunk = ((String[]) negativesDictionaryEntryCollector.toArray(new String[negativesDictionaryEntryCollector.size()]));
				negativesDictionaryPartsCs.put(dChunk, dChunk);
				negativesDictionaryEntryCollector.clear();
//				negativesDictionaryPartSet.add((String[]) negativesDictionaryEntryCollector.toArray(new String[negativesDictionaryEntryCollector.size()]));
			}
			it.remove();
		}
		if (negativesDictionaryEntryCollector.size() != 0) {
			String[] dChunk = ((String[]) negativesDictionaryEntryCollector.toArray(new String[negativesDictionaryEntryCollector.size()]));
			negativesDictionaryPartsCs.put(dChunk, dChunk);
//			negativesDictionaryPartSet.add((String[]) negativesDictionaryEntryCollector.toArray(new String[negativesDictionaryEntryCollector.size()]));
		}
		negativesDictionarySize = deCount;
		
		System.out.println("Loaded " + deCount + " distinct entries with " + deSize + " chars, needing " + (rt.totalMemory() - rt.freeMemory()));
		System.out.println("Loaded " + deCountNoHash + " unhashable entries");
		
		negativesDictionaryEntrySet = null;
		negativesDictionaryEntryCollector.clear();
		negativesDictionaryEntryCollector = null;
		System.gc();
		System.out.println("Cleaned up, needing " + (rt.totalMemory() - rt.freeMemory()));
		
		String test;
		
		test = "color";
		System.out.println("Lookup '" + test + "' --> " + negativesDictionarySet.lookup(test));
		
		test = "Test";
		System.out.println("Lookup '" + test + "' --> " + negativesDictionarySet.lookup(test));
		
		test = "aufzug";
		System.out.println("Lookup '" + test + "' --> " + negativesDictionarySet.lookup(test));
		
		test = "Aufzug";
		System.out.println("Lookup '" + test + "' --> " + negativesDictionarySet.lookup(test));
		
		test = "wrdlbrmpft";
		System.out.println("Lookup '" + test + "' --> " + negativesDictionarySet.lookup(test));
		
		
//		JOptionPane.showMessageDialog(null, ("Loaded " + deCount + " entries"), "Loaded", JOptionPane.PLAIN_MESSAGE);
//		System.out.println("Showed dialog, needing " + (rt.totalMemory() - rt.freeMemory()));
	}
	
//	private static Pattern hashTest = Pattern.compile("[A-Za-z]++");
	private static long hash(String s) {
//		if (!hashTest.matcher(s).matches()) return -1;
		if (s.length() > 16) return -2;
		
		long hash = 0;
		for (int c = 0; c < s.length(); c++) {
			if (c != 0)
				hash *= 52;
			char ch = s.charAt(c);
			if (('a' <= ch) && (ch <= 'z'))
				hash += (26 + (ch - 'a'));
			else if (('A' <= ch) && (ch <= 'Z'))
				hash += (ch - 'A');
			else hash += ('Q' - 'A'); // use 'Q' (very rare) for all characters but basic ASCII letters
//			else hash += 52; // all but basic ASCII letters
		}
		return hash;
	}
}