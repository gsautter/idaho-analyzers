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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.stringUtils.StringIndex;

/**
 * @author sautter
 *
 */
public class LexiconAnalysis {
	private static File dataPath = new File("E:/Testdaten/OmniFATData");
	public static void main(String[] args) throws Exception {
		HashSet suffixes = new HashSet();
		TreeSet targetWords = new TreeSet();
//		String targetSuffix = "xxx";
//		Pattern targetSuffixPattern = Pattern.compile(".*[^aeioul]le");
//		Pattern targetSuffixPattern = Pattern.compile(".*uit");
//		Pattern targetSuffixPattern = Pattern.compile(".*(nd|like|ts|ibus|ls|nt|th|uit|oid|ed|tic|ds)");
//		Pattern targetSuffixPattern = Pattern.compile(".*(ose|tle|rs|lt|nce)");
		Pattern targetSuffixPattern = Pattern.compile(".*(ore|teo|ceo|ous|ble|ple|gle|fle|pt|rd|ms|rf|af|ge|w|in|let|t|ll|ss|tt)");
		final StringIndex suffixCounts = new StringIndex(true);
		
//		String name = "higher.static.list.txt";
//		String name = "genus.static.list.txt";
//		String name = "species.static.list.txt";
		String name = "EnglishWord.list.txt";
//		String name = "GermanWord.list.txt";
		
		BufferedReader lexiconReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dataPath, name)), "UTF-8"));
		String lexiconEntry;
		while ((lexiconEntry = lexiconReader.readLine()) != null) {
			lexiconEntry = lexiconEntry.trim();
			if ((lexiconEntry.length() != 0) && !lexiconEntry.startsWith("//")) {
				for (int s = 0; s <= Math.min(lexiconEntry.length()-1, 4); s++) {
					String suffix = lexiconEntry.substring(lexiconEntry.length() - s);
					suffixes.add(suffix);
					suffixCounts.add(suffix);
//					if (suffix.equals(targetSuffix))
//						targetWords.add(lexiconEntry);
				}
				if (targetSuffixPattern.matcher(lexiconEntry).matches())
					targetWords.add(lexiconEntry);
			}
		}
		lexiconReader.close();
		
		TreeSet sortedSuffixes = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				int c = (suffixCounts.getCount(o2.toString()) - suffixCounts.getCount(o1.toString()));
				return ((c == 0) ? ((String) o1).compareTo((String) o2) : c);
			}
		});
		sortedSuffixes.addAll(suffixes);
		System.out.println("Got " + sortedSuffixes.size() + " suffixes:");
		for (Iterator sit = sortedSuffixes.iterator(); sit.hasNext();) {
			String suffix = ((String) sit.next());
			System.out.println(suffix + "\t" + suffixCounts.getCount(suffix));
		}
		System.out.println("Got " + targetWords.size() + " target words:");
		for (Iterator twit = targetWords.iterator(); twit.hasNext();) {
			String targetWord = ((String) twit.next());
			System.out.println(targetWord);
		}
	}
}
