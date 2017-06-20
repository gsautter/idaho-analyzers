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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;

import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * parser utility for BEOLINGUS dictionary files
 * 
 * @author sautter
 */
public class DictionaryParser {
	
	static boolean debug = false;
	static File path = new File("E:/Projektdaten/");
//	static String cleaningRegEx = "(\\;|\\||(\\{[a-z]++\\})|(\\[[a-z]++\\.\\])|\\(|\\)|\\\"\\')";
//	static String cleaningRegEx = "((\\{[^\\}]++\\})|(\\[[^\\]]++\\])|[^A-ZÄÖÜa-zäöü\\-\\'])";
	static String cleaningRegEx = "((\\{[^\\}]++\\})|(\\[[^\\]]++\\])|[^" + StringUtils.LETTERS + "\\-\\'])";
	
	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path, "de-en.txt")), "UTF-8"));
		StringVector germanWords = new StringVector();
		HashSet germanSet = new HashSet();
		StringVector englishWords = new StringVector();
		HashSet englishSet = new HashSet();
		String line;
		StringVector lineParser = new StringVector();
		int lineCount = 0;
		while ((line = br.readLine()) != null) {
			
			int split = line.indexOf("::");
			
			boolean lineOk = (split != -1);
			lineOk = (lineOk && !line.startsWith("#"));
			lineOk = (lineOk && (line.indexOf("[anat.]") == -1));
			lineOk = (lineOk && (line.indexOf("[biol.]") == -1));
			lineOk = (lineOk && (line.indexOf("[bot.]") == -1));
			lineOk = (lineOk && (line.indexOf("[med.]") == -1));
			lineOk = (lineOk && (line.indexOf("[ornith.]") == -1));
			lineOk = (lineOk && (line.indexOf("[zool.]") == -1));
			
			if (lineOk) {
				
				String german = line.substring(0, split).trim();
				
				german = german.replaceAll(cleaningRegEx, " ");
				lineParser.clear();
				lineParser.parseAndAddElements(german, " ");
				lineParser.removeAll("");
				
				for (int i = 0; i < lineParser.size(); i++) {
					german = lineParser.get(i);
					if (germanSet.add(german) && !german.matches("[A-Z]++") && StringUtils.isWord(german))
						germanWords.addElement(german);
				}
				
//				germanSet.addAll(lineParser.asSet());
//				germanWords.addContentIgnoreDuplicates(lineParser);
//				germanWords.addElementIgnoreDuplicates(german);
				
				String english = line.substring(split + 2).trim();
				
				english = english.replaceAll(cleaningRegEx, " ");
				lineParser.clear();
				lineParser.parseAndAddElements(english, " ");
				lineParser.removeAll("");
				
				for (int i = 0; i < lineParser.size(); i++) {
					english = lineParser.get(i);
					if (englishSet.add(english) && !english.matches("[A-Z]++") && StringUtils.isWord(english))
						englishWords.addElement(english);
				}
				
//				englishSet.addAll(lineParser.asSet());
//				englishWords.addContentIgnoreDuplicates(lineParser);
//				englishWords.addElementIgnoreDuplicates(english);
				
				lineCount++;
			}
			if ((lineCount != 0) && ((lineCount % 100) == 0))
				System.out.println(lineCount + " lines done, " + englishWords.size() + " / " + germanWords.size() + " words.");
			if (debug && (lineCount > 20)) break;
		}
		
		br.close();
		
		germanWords.sortLexicographically(false, true);
		englishWords.sortLexicographically(false, true);
		
		if (debug) {
			germanWords.storeContent(new PrintWriter(System.out));
			englishWords.storeContent(new PrintWriter(System.out));
		}
		
		else {
			OutputStreamWriter osw;
			
			osw = new OutputStreamWriter(new FileOutputStream(new File(path, "de.txt")), "UTF-8");
			germanWords.storeContent(osw);
			osw.flush();
			osw.close();
			
			osw = new OutputStreamWriter(new FileOutputStream(new File(path, "en.txt")), "UTF-8");
			englishWords.storeContent(osw);
			osw.flush();
			osw.close();
		}
	}
}
