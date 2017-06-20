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
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class LexiconNormalizer {
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
	
	private static Pattern normalized = Pattern.compile("\\p{ASCII}++");
	private static void normalizeLexicon(File rawLexicon) throws IOException {
		if (rawLexicon.getName().indexOf("author") == -1)
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
			s = ((rawLexicon.getName().indexOf("author") == -1) ? normalize(s) : normalizeAuthor(s));
			if (s.length() != 0) {
				if (normalized.matcher(s).matches())
					lexicon.add(s);
				else System.out.println("Problem string: " + s);
			}
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
	
	private static Properties mappings;
	private static String normalize(String s) {
		if (mappings == null) {
			mappings = new Properties();
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream(new File(dataPath, "charNormalizationMappings.txt")), "UTF-8"));
				while (br.ready()) {
					String line = br.readLine().trim();
					if ((line.length() > 2) && !line.startsWith("//")) {
						String original = line.substring(0, 1).trim();
						String substitute = line.substring(2).trim();
						
						if ((original.length() != 0) && (substitute.length() != 0))
							mappings.setProperty(original, substitute);
					}
				}
				br.close();
			}
			catch (Exception e) {
				System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while initializing CharacterNormalization.");
			}
		}
		
		StringBuffer n = new StringBuffer();
		for (int c = 0; c < s.length(); c++) {
			String str = s.substring(c, (c+1));
			String normalizedStr = mappings.getProperty(str);
			
			if ((normalizedStr == null) || str.equals(normalizedStr))
				n.append(str);
			
			else {
				if (!normalizedStr.equals(normalizedStr.toLowerCase()) && (normalizedStr.length() > 1)) {
					boolean upper = true;
					if (c != 0) {
						String lastNormalizedStr = n.substring(n.length() - 1);
						if (lastNormalizedStr.equals(lastNormalizedStr.toLowerCase()))
							upper = false;
					}
					if ((c+1) < s.length()) {
						String nextStr = s.substring((c+1), (c+2));
						if (nextStr.equals(nextStr.toLowerCase()))
							upper = false;
					}
					if (upper)
						normalizedStr = normalizedStr.toUpperCase();
				}
				n.append(normalizedStr);
			}
		}
		return n.toString();
	}
	
	private static Pattern letter = Pattern.compile("[A-Za-z]");
	private static Pattern initial = Pattern.compile("[A-Z][^\\p{Alnum}]?");
	private static String normalizeAuthor(String s) {
		s = normalize(s);
		
		StringVector parts = new StringVector();
		parts.parseAndAddElements(s, ".");
		for (int p = 0; p < parts.size(); p++)
			parts.setElementAt(parts.get(p).trim(), p);
		s = parts.concatStrings(". ");
		parts.clear();
		
		while ((s.length() != 0) && !letter.matcher(s.substring(0, 1)).matches())
			s = s.substring(1);
		while ((s.length() != 0) && !letter.matcher(s.substring(s.length()-1)).matches())
			s = s.substring(0, (s.length()-1));
		
		if (s.endsWith(" al"))
			s+= ".";
		
		parts.parseAndAddElements(s, " ");
		parts.removeAll("");
		s = parts.concatStrings(" ");
		parts.clear();
		
		parts.parseAndAddElements(s, " -");
		s = parts.concatStrings("-");
		parts.clear();
		parts.parseAndAddElements(s, "- ");
		s = parts.concatStrings("-");
		parts.clear();
		
		parts.parseAndAddElements(s, " ");
		int lastPartIndex = parts.size() - 1;
		while (lastPartIndex != -1) {
			String part = parts.get(lastPartIndex);
			if (part.equals(part.toLowerCase()))
				parts.remove(lastPartIndex--);
			else if (initial.matcher(part).matches())
				parts.remove(lastPartIndex--);
			else lastPartIndex = -1;
		}
		s = parts.concatStrings(" ");
		parts.clear();
		
		parts.parseAndAddElements(s, " ");
		for (int p = 0; p < parts.size(); p++) {
			String part = parts.get(p);
			if (part.length() == 1) {
				char ch = part.charAt(0);
				if (('A' <= ch) && (ch <= 'Z'))
					parts.setElementAt((part + "."), p);
			}
			else if (!initial.matcher(part).matches())
				p = parts.size();
		}
		s = parts.concatStrings(" ");
		parts.clear();
		
		parts.parseAndAddElements(s, " ");
		for (int p = 0; p < parts.size(); p++) {
			String part = parts.get(p);
			StringBuffer pas = new StringBuffer();
			boolean lastWasLetter = false;
			for (int c = 0; c < part.length(); c++) {
				char ch = part.charAt(c);
				if (Character.isLetter(ch)) {
					if (lastWasLetter)
						ch = Character.toLowerCase(ch);
					lastWasLetter = true;
				}
				else lastWasLetter = false;
				pas.append(ch);
			}
			parts.setElementAt(pas.toString(), p);
		}
		s = parts.concatStrings(" ");
		parts.clear();
		
		return s;
	}
}
