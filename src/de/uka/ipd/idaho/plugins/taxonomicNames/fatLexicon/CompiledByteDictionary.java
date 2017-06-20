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
// * insertions.<BR>
// * Warning: In favor of keeping memory footprint low, this class <B>does not
// * support unicode</B>, but stores strings as sequences of 1-byte ASCII-8
// * characters. In cases where exact lookups for non-ASCII-8 characters are
// * required, use CompiledDictionaty instead.
// * 
// * @author sautter
// */
//public class CompiledByteDictionary implements Dictionary {
//	private static final int chunkSize = 1020;
//	private static final Comparator byteChunkComparator = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			byte[][] b1 = ((byte[][]) o1);
//			byte[][] b2 = ((byte[][]) o2);
//			
//			int c = byteComparator.compare(b1[b1.length-1], b2[0]);
//			if (c < 0) return -1;
//			
//			c = byteComparator.compare(b2[b2.length-1], b1[0]);
//			if (c < 0) return 1;
//			
//			return 0;
//		}
//	};
//	private static final Comparator byteComparator = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			byte[] b1 = ((byte[]) o1);
//			byte[] b2 = ((byte[]) o2);
//			for (int b = 0; b < Math.min(b1.length, b2.length); b++) {
//				if (b1[b] != b2[b])
//					return ((255 & b1[b]) - (255 & b2[b]));
//			}
//			return (b1.length - b2.length);
//		}
//	};
//	
//	
//	private Set entrySet = new TreeSet();
//	
//	private final boolean caseSensitive;
//	private int entryCount = -1;
//	private Map csChunks = new TreeMap(byteChunkComparator);
//	private Map ciChunks = new TreeMap(byteChunkComparator);
//	
//	/**
//	 * Constructor building a case sensitive dictionary
//	 */
//	public CompiledByteDictionary() {
//		this(true);
//	}
//	
//	/**
//	 * Constructor
//	 * @param caseSensitive create a case sensitive dictionary?
//	 */
//	public CompiledByteDictionary(boolean caseSensitive) {
//		this.caseSensitive = caseSensitive;
//	}
//	
//	/**
//	 * Add a string to the dictionary. Note that this method has only an effect
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
//		compile(this.entrySet, this.csChunks, ciEntryCollector);
//		compile(ciEntryCollector, this.ciChunks, null);
//		
//		this.entrySet = null;
//		ciEntryCollector = null;
//		System.gc();
//		
//		System.out.println("Compiled, register sizes are:\n- " + this.csChunks.size() + " CS chunks\n- " + this.ciChunks.size() + " CI chunks");
//	}
//	
//	private static void compile(Set entrySet, Map chunks, Set ciEntryCollector) {
//		ArrayList chunkCollector = new ArrayList(chunkSize);
//		
//		for (Iterator it = entrySet.iterator(); it.hasNext();) {
//			String de = ((String) it.next());
//			chunkCollector.add(de);
//			if (chunkCollector.size() == chunkSize) {
//				String[] chunk = ((String[]) chunkCollector.toArray(new String[chunkCollector.size()]));
//				byte[][] bChunk = new byte[chunk.length][];
//				for (int b = 0; b < bChunk.length; b++)
//					bChunk[b] = encode(chunk[b]);
//				chunks.put(bChunk, bChunk);
//				chunkCollector.clear();
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
//			byte[][] bChunk = new byte[chunk.length][];
//			for (int b = 0; b < bChunk.length; b++)
//				bChunk[b] = encode(chunk[b]);
//			chunks.put(bChunk, bChunk);
//		}
//		
//		chunkCollector.clear();
//		System.gc();
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
//	 */
//	public StringIterator getEntryIterator() {
//		final Iterator it = this.csChunks.values().iterator();
//		return new StringIterator() {
//			Iterator pit = null;
//			public boolean hasNext() {
//				if (this.pit == null) {
//					if (it.hasNext()) {
//						byte[][] pitData = ((byte[][]) it.next());
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
//					return decode((byte[]) this.pit.next());
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
//		if (lookup(this.csChunks, string))
//			return true;
//		else if (caseSensitive)
//			return false;
//		else {
//			string = string.toLowerCase();
//			return (lookup(this.csChunks, string) || lookup(this.ciChunks, string));
//		}
//	}
//	
//	private static boolean lookup(Map chunks, String string) {
//		byte[] bytes = encode(string);
//		byte[][] bLookup = {bytes};
//		byte[][] bChunk = ((byte[][]) chunks.get(bLookup));
//		if (bChunk == null) return false;
//		int index = Arrays.binarySearch(bChunk, bytes, byteComparator);
//		return ((index >= 0) && (index < bChunk.length) && (byteComparator.compare(bChunk[index], bytes) == 0));
//	}
//	
//	// !!! test only !!!
//	public static void main(String[] args) throws Exception {
////		String s = "aäßa";
////		byte[] bs = encode(s);
////		System.out.print(s + " --> ");
////		for (int b = 0; b < bs.length; b++)
////			System.out.print(((b == 0) ? "" : ", ") + bs[b]);
////		s = decode(bs);
////		System.out.println(" --> " + s);
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
//		CompiledByteDictionary cd = new CompiledByteDictionary();
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
//						cd.add(de);
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
//		System.out.println("Loaded, needing " + (rt.totalMemory() - rt.freeMemory()));
//		
//		cd.compile();
//		System.out.println("Compiled " + deCount + " entries (" + cd.size() + " distinct) with " + deSize + " chars, needing " + (rt.totalMemory() - rt.freeMemory()));
//		
//		String test;
//		
//		test = "color";
//		System.out.println("Lookup '" + test + "' --> " + cd.lookup(test));
//		
//		test = "Test";
//		System.out.println("Lookup '" + test + "' --> " + cd.lookup(test));
//		
//		test = "aufzug";
//		System.out.println("Lookup '" + test + "' --> " + cd.lookup(test));
//		
//		test = "aufzug";
//		System.out.println("Lookup '" + test + "' CI --> " + cd.lookup(test, false));
//		
//		test = "Aufzug";
//		System.out.println("Lookup '" + test + "' --> " + cd.lookup(test));
//		
//		test = "wrdlbrmpft";
//		System.out.println("Lookup '" + test + "' --> " + cd.lookup(test));
//	}
//	
//	private static byte[] encode(String s) {
//		byte[] bytes = new byte[s.length()];
//		for (int c = 0; c < s.length(); c++) {
//			char ch = s.charAt(c);
//			if (ch < 255)
//				bytes[c] = ((byte) s.charAt(c));
//			else bytes[c] = ((byte) 255);
//		}
//		return bytes;
//	}
//	
//	private static String decode(byte[] bytes) {
//		StringBuffer sb = new StringBuffer();
//		for (int b = 0; b < bytes.length; b++)
//			sb.append((char) (255 & bytes[b]));
//		return sb.toString();
//	}
//}
