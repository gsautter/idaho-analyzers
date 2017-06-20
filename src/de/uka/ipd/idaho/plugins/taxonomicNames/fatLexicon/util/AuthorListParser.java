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
 * parser utility for taxon name author lists
 * 
 * @author sautter
 */
public class AuthorListParser {

	static boolean debug = false;
	static File path = new File("E:/Projektdaten/");
	
	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path, "authors.txt"))));
		StringVector names = new StringVector();
		HashSet nameSet = new HashSet();
		int nameCount = 0;
		int c;
		StringBuffer nameBuffer = new StringBuffer();
		while ((c = br.read()) != -1) {
			
			if (c == ';') {
				String name = nameBuffer.toString().trim();
				nameBuffer = new StringBuffer();
				
				if (name.indexOf(',') != -1)
					name = name.substring(0, name.indexOf(',')).trim();
				if (nameSet.add(name)) {
					names.parseAndAddElements(name, " ");
					nameCount++;
				}
			}
			
			else nameBuffer.append((char) c);
			
			if ((nameCount != 0) && ((nameCount % 100) == 0))
				System.out.println(nameCount + " names done, " + names.size() + " names & name parts.");
			if (debug && (nameCount > 20)) break;
		}
		
		if (nameBuffer.length() != 0) {
			String name = nameBuffer.toString().trim();
			
			if (name.indexOf(',') != -1)
				name = name.substring(0, name.indexOf(',')).trim();
			if (nameSet.add(name)) {
				names.parseAndAddElements(name, " ");
				nameCount++;
			}
			
			nameCount++;
		}
		
		br.close();
		
		names.removeDuplicateElements();
		
		for (int n = 0; n < names.size();) {
			String name = names.get(n);
			if ((name.length() < 2) || (StringUtils.LETTERS.indexOf(name.charAt(1)) == -1))
				names.remove(n);
			else n++;
		}
		
		names.sortLexicographically(false, true);
		
		if (debug) names.storeContent(new PrintWriter(System.out));
		
		else {
			OutputStreamWriter osw;
			
			osw = new OutputStreamWriter(new FileOutputStream(new File(path, "taxonNameAuthors.txt")), "UTF-8");
			names.storeContent(osw);
			osw.flush();
			osw.close();
		}
	}
}
