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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * @author sautter
 *
 */
public class LexiconParser {
	private static File dataPath = new File("E:/Testdaten/OmniFATData");
	public static void main(String[] args) throws IOException {
		File rawFolder = new File(dataPath, "rawData");
		File[] rawFiles = rawFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (file.isFile() && file.getName().startsWith("raw.") && file.getName().endsWith(".list.txt"));
			}
		});
		for (int f = 0; f < rawFiles.length; f++)
			normalizeLexicon(rawFiles[f]);
	}
	
	private static void normalizeLexicon(File rawLexicon) throws IOException {
		if (rawLexicon.getName().indexOf("latin") == -1)
			return;
		
		File normalizedLexicon = new File(dataPath, rawLexicon.getName().substring(4));
		if (normalizedLexicon.exists())
			normalizedLexicon.delete();
		TreeSet lexicon = new TreeSet();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(rawLexicon), "UTF-8"));
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(rawLexicon), "ISO8859-1"));
		String s;
		while ((s = br.readLine()) != null) {
			s = s.trim();
			if (s.indexOf(':') != -1) {
				s = s.substring(0, s.indexOf(':'));
				String[] sps = s.split("[^a-z]++");
				lexicon.addAll(Arrays.asList(sps));
			}
//			s = ((rawLexicon.getName().indexOf("author") == -1) ? normalize(s) : normalizeAuthor(s));
//			if (s.length() != 0) {
//				if (normalized.matcher(s).matches())
//					lexicon.add(s);
//				else System.out.println("Problem string: " + s);
//			}
		}
		
		normalizedLexicon.createNewFile();
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(normalizedLexicon), "UTF-8"));
		for (Iterator sit = lexicon.iterator(); sit.hasNext();) {
			s = sit.next().toString();
			bw.write(s);
			bw.newLine();
		}
		bw.flush();
		bw.close();
		br.close();
	}
}
