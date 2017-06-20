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
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

/**
 * @author sautter
 *
 */
public class LexiconDownloader {
//	http://spira.zoology.gla.ac.uk/whouse_names.txt
	private static File dataPath = new File("E:/Testdaten/OmniFATData");
	public static void main(String[] args) throws Exception {
		File rawFolder = new File(dataPath, "rawData");
		
		String lexiconUrl = "http://spira.zoology.gla.ac.uk/whouse_names.txt";
		String name = lexiconUrl.substring(lexiconUrl.lastIndexOf('/') + 1);
		
		BufferedReader lexiconReader = new BufferedReader(new InputStreamReader(new URL(lexiconUrl).openStream(), "UTF-8"));
		BufferedWriter lexiconWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(rawFolder, name)), "UTF-8"));
		String lexiconEntry;
		int lexiconEntryCount = 0;
		while ((lexiconEntry = lexiconReader.readLine()) != null) {
			lexiconEntry = lexiconEntry.trim();
			if ((lexiconEntry.length() != 0) && !lexiconEntry.startsWith("//")) {
				lexiconWriter.write(lexiconEntry);
				lexiconWriter.newLine();
				System.out.println("" + lexiconEntryCount++ + ": " + lexiconEntry);
			}
		}
		lexiconReader.close();
		lexiconWriter.flush();
		lexiconWriter.close();
	}
}
