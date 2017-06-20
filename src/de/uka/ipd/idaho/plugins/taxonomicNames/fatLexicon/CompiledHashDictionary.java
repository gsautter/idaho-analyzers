///*
// * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeMap;
//import java.util.TreeSet;
//
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
//import de.uka.ipd.idaho.stringUtils.Dictionary;
//import de.uka.ipd.idaho.stringUtils.StringIterator;
//import de.uka.ipd.idaho.stringUtils.StringVector;
//
///**
// * A compiled dictionary is aimed at holding large lookup lists of strings in as
// * little memory as possible. Their lifecycle is divided into two phases: a
// * building phase and a lookup phase, separated by the compilation. Before the
// * compilation (during the build phase), new strings can be added to the
// * dictionary, but lookup operations are undefined. After the compilation, no
// * new strings can be added, but lookup operations are possible. This is because
// * during the compilation, the dictionary is compressed into a specialized
// * lookup data structure that reduces memory requirements, but prohibits further
// * insertions.
// * 
// * @author sautter
// */
//public class CompiledHashDictionary implements Dictionary {
//	private static final int chunkSize = 1020;
//	private static final Comparator chunkComparator = new Comparator() {
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
//	};
//	private static final Comparator hashChunkComparator = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			long[] l1 = ((long[]) o1);
//			long[] l2 = ((long[]) o2);
//			
//			if (l1[l1.length-1] < l2[0]) return -1;
//			else if (l2[l2.length-1] < l1[0]) return 1;
//			else return 0;
//		}
//	};
//	
//	private Set entrySet = new TreeSet();
//	
//	private final boolean caseSensitive;
//	private int entryCount = -1;
//	private Map csHashChunks = new TreeMap(hashChunkComparator);
//	private Map ciHashChunks = new TreeMap(hashChunkComparator);
//	private Map csChunks = new TreeMap(chunkComparator);
//	private Map ciChunks = new TreeMap(chunkComparator);
//	
//	/**
//	 * Constructor building a case sensitive dictionary
//	 */
//	public CompiledHashDictionary() {
//		this(true);
//	}
//	
//	/**
//	 * Constructor
//	 * @param caseSensitive create a case sensitive dictionary?
//	 */
//	public CompiledHashDictionary(boolean caseSensitive) {
//		this.caseSensitive = caseSensitive;
//	}
//	
//	/**
//	 * Add a string to the dictionary. Note that this method has only an etfect
//	 * before the compilation. Likewise, looking up the argument string returns
//	 * true only after the compilation.
//	 * @param string the string to add
//	 */
//	public synchronized void add(String string) {
//		if ((this.entrySet != null) && (string != null))
//			this.entrySet.add(string);
//	}
//	
//	/**
//	 * Check whether the dictionary has been compiled.
//	 * @return true if has been compiled, false otherwise
//	 */
//	public synchronized boolean isCompiled() {
//		return (this.entrySet == null);
//	}
//	
//	/**
//	 * Compile the dictionary. This disables adding further strings, and enables
//	 * lookup. Invocing this method more than once has no effect.
//	 */
//	public synchronized void compile() {
//		if (this.entrySet == null) return;
//		
//		this.entryCount = this.entrySet.size();
//		
//		TreeSet ciEntryCollector = new TreeSet();
//		
//		compile(this.entrySet, this.csChunks, this.csHashChunks, ciEntryCollector);
//		compile(ciEntryCollector, this.ciChunks, this.ciHashChunks, null);
//		
//		this.entrySet = null;
//		ciEntryCollector = null;
//		System.gc();
//		
//		System.out.println("Compiled, register sizes are:\n- " + this.csHashChunks.size() + " CSH chunks\n- " + this.csChunks.size() + " CS chunks\n- " + this.ciHashChunks.size() + " CIH chunks\n- " + this.ciChunks.size() + " CI chunks");
//	}
//	
//	private static void compile(Set entrySet, Map chunks, Map hashChunks, Set ciEntryCollector) {
//		ArrayList hashChunkCollector = new ArrayList(chunkSize);
//		ArrayList chunkCollector = new ArrayList(chunkSize);
//		
//		for (Iterator it = entrySet.iterator(); it.hasNext();) {
//			String de = ((String) it.next());
//			long hash = hash(de);
//			if (hash == Long.MAX_VALUE) {
//				chunkCollector.add(de);
//				if (chunkCollector.size() == chunkSize) {
//					String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
//					chunks.put(chunk, chunk);
//					chunkCollector.clear();
//				}
//			}
//			else {
//				hashChunkCollector.add(new Long(hash));
//				if (hashChunkCollector.size() == chunkSize) {
//					Long[] hashChunk = ((Long[]) hashChunkCollector.toArray(new Long[hashChunkCollector.size()]));
//					long[] hChunk = new long[hashChunk.length];
//					for (int l = 0; l < hashChunk.length; l++)
//						hChunk[l] = hashChunk[l].longValue();
//					hashChunks.put(hChunk, hChunk);
//					hashChunkCollector.clear();
//				}
//			}
//			it.remove();
//			
//			if (ciEntryCollector != null) {
//				String ciDe = de.toLowerCase();
//				if (!de.equals(ciDe) && !entrySet.contains(ciDe))
//					ciEntryCollector.add(ciDe);
//			}
//		}
//		
//		if (chunkCollector.size() != 0) {
//			String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
//			chunks.put(chunk, chunk);
//		}
//		if (hashChunkCollector.size() != 0) {
//			Long[] hashChunk = ((Long[]) hashChunkCollector.toArray(new Long[hashChunkCollector.size()]));
//			long[] hChunk = new long[hashChunk.length];
//			for (int l = 0; l < hashChunk.length; l++)
//				hChunk[l] = hashChunk[l].longValue();
//			hashChunks.put(hChunk, hChunk);
//			hashChunkCollector.clear();
//		}
//		
//		chunkCollector.clear();
//		hashChunkCollector.clear();
//		System.gc();
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
//	 */
//	public StringIterator getEntryIterator() {
//		final Iterator it = csChunks.entrySet().iterator();
//		return new StringIterator() {
//			Iterator pit = null;
//			public boolean hasNext() {
//				if (this.pit == null) {
//					if (it.hasNext()) {
//						String[] pitData = ((String[]) it.next());
//						this.pit = Arrays.asList(pitData).iterator();
//						return this.pit.hasNext();
//					}
//					else return false;
//				}
//				else if (this.pit.hasNext())
//					return true;
//				else {
//					this.pit = null;
//					return this.hasNext();
//				}
//			}
//			public Object next() {
//				if (this.hasNext())
//					return this.pit.next();
//				else return null;
//			}
//			public void remove() {}
//			public boolean hasMoreStrings() {
//				return this.hasNext();
//			}
//			public String nextString() {
//				return ((String) this.next());
//			}
//		};
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
//	 */
//	public boolean isDefaultCaseSensitive() {
//		return this.caseSensitive;
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
//	 */
//	public boolean isEmpty() {
//		return this.csChunks.isEmpty();
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
//	 */
//	public int size() {
//		return this.entryCount;
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
//	 */
//	public boolean lookup(String string) {
//		return this.lookup(string, this.caseSensitive);
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
//	 */
//	public boolean lookup(String string, boolean caseSensitive) {
//		if (lookup(this.csChunks, this.csHashChunks, string))
//			return true;
//		else if (caseSensitive)
//			return false;
//		else {
//			string = string.toLowerCase();
//			return (lookup(this.csChunks, this.csHashChunks, string) || lookup(this.ciChunks, this.ciHashChunks, string));
//		}
//	}
//	
//	private static boolean lookup(Map chunks, Map hashChunks, String string) {
//		long hash = hash(string);
//		if (hash == Long.MAX_VALUE) {
//			String[] lookup = {string};
//			String[] chunk = ((String[]) chunks.get(lookup));
//			if (chunk == null) return false;
//			int index = Arrays.binarySearch(chunk, string);
//			return ((index >= 0) && (index < chunk.length) && chunk[index].equals(string));
//		}
//		else {
//			long[] hLookup = {hash};
//			long[] hChunk = ((long[]) hashChunks.get(hLookup));
//			if (hChunk == null) return false;
//			int index = Arrays.binarySearch(hChunk, hash);
//			return ((index >= 0) && (index < hChunk.length) && (hChunk[index] == hash));
//		}
//	}
//	
//	// !!! test only !!!
//	public static void main(String[] args) throws Exception {
//		System.out.println(hash("a"));
//		System.out.println(hash("z"));
//		System.out.println(hash("aaaaaaaaaa"));
//		System.out.println(hash("zzzzzzzzzz"));
////		if (true) return;
//		
//		Runtime rt = Runtime.getRuntime();
//		
//		AnalyzerDataProvider dp = new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/LexiconFATData/"));
//		InputStream is;
//		
//		is = dp.getInputStream("negativesDictionaryList.cnfg");
//		StringVector dictionaryNameList = StringVector.loadList(new InputStreamReader(is, "UTF-8"));
//		System.out.println("Got dictionary list, needing " + (rt.totalMemory() - rt.freeMemory()));
//		is.close();
//		
//		int deCount = 0;
//		int deSize = 0;
//		CompiledHashDictionary chd = new CompiledHashDictionary();
//		
//		for (int n = 0; n < dictionaryNameList.size(); n++) {
//			String dictionaryName = dictionaryNameList.get(n);
//			if (!dictionaryName.startsWith("//")) try {
//				
//				if (dictionaryName.startsWith("http://"))
//					is = dp.getURL(dictionaryName).openStream();
//				else is = dp.getInputStream(dictionaryName);
//				
//				BufferedReader dr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//				String de;
//				while ((de = dr.readLine()) != null) {
//					if (de.length() != 0) {
//						deCount++;
//						deSize += de.length();
//						chd.add(de);
//					}
//				}
//				
//				is.close();
//				
//				System.out.println(" ==> added dictionary list '" + dictionaryName + "'");
//			}
//			catch (IOException ioe) {
//				System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading dictionary '" + dictionaryName + "'.");
//			}
//		}
//		
//		System.out.println("Loaded, needing " + (rt.totalMemory() - rt.freeMemory()));
//		
//		chd.compile();
//		System.out.println("Compiled " + deCount + " entries (" + chd.size() + " distinct) with " + deSize + " chars, needing " + (rt.totalMemory() - rt.freeMemory()));
//		
//		String test;
//		
//		test = "color";
//		System.out.println("Lookup '" + test + "' --> " + chd.lookup(test));
//		
//		test = "Test";
//		System.out.println("Lookup '" + test + "' --> " + chd.lookup(test));
//		
//		test = "aufzug";
//		System.out.println("Lookup '" + test + "' --> " + chd.lookup(test));
//		
//		test = "aufzug";
//		System.out.println("Lookup '" + test + "' CI --> " + chd.lookup(test, false));
//		
//		test = "Aufzug";
//		System.out.println("Lookup '" + test + "' --> " + chd.lookup(test));
//		
//		test = "wrdlbrmpft";
//		System.out.println("Lookup '" + test + "' --> " + chd.lookup(test));
//		
//		System.gc();
//		System.out.println("Cleaned up, needing " + (rt.totalMemory() - rt.freeMemory()));
//	}
//	
//	private static final int hashLength = 10;
////	private static final long hashDim = 53;
//	private static long hash(String s) {
//		if (s.length() > hashLength) return Long.MAX_VALUE;
//		
//		long hash = 0;
//		for (int c = 0; c < hashLength; c++) {
//			if (c != 0)
//				hash <<= 6;
//			
//			if (c < s.length()) {
//				char ch = s.charAt(c);
//				if (ch < 'A') hash += ((long) 1);
//				else if (ch <= 'Z') hash += ((long) (1 + (ch - 'A')));
//				else if (ch < 'a') hash += ((long) (1 + 26));
//				else if (ch <= 'z') hash += ((long) (1 + 26 + 1 + (ch - 'a')));
//				else hash += ((long) (1 + 26 + 1 + 26));
//				
////				if (('a' <= ch) && (ch <= 'z'))
////					hash += ((long) (1 + 26 + (ch - 'a')));
////				else if (('A' <= ch) && (ch <= 'Z'))
////					hash += ((long) (1 + (ch - 'A')));
////				else if (('0' <= ch) && (ch <= '9'))
////					hash += ((long) (1 + 26 + 26 + (ch - '0')));
//			}
//		}
//		return hash;
//	}
//}