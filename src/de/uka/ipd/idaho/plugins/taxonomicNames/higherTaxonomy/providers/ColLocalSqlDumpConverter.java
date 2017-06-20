/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.providers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import de.uka.ipd.idaho.easyIO.utilities.SqlDumpReader;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This class converts an SQL dump of CoL into a local tab separated lookup
 * file containing the higher taxonomies of genera, as well as a semicolon
 * separated list of species for each genus. Further, it generates index files
 * for quick access to the lines (records) by genus and family.<br>
 * These files are intended to be used by CoLHigherTaxonomyProvider as a local
 * lookup facility, saving a considerable number of web API lookups.
 * 
 * @author sautter
 */
public class ColLocalSqlDumpConverter {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		final boolean exploreDump = ((args.length > 1) && ("-e".equals(args[1]))); // set to false to actually generate files
		File colSqlDump;
		if (args.length > 0)
			colSqlDump = new File(args[0]);
		else colSqlDump = new File("E:/Install/CoL/", "col2015ac.sql.tar.gz");
		InputStream colSqlDumpIn = new GZIPInputStream(new FileInputStream(colSqlDump));
		BufferedReader colIn = new BufferedReader(new InputStreamReader(colSqlDumpIn, "UTF-8"));
		
		//	skip over TAR header
		colIn.skip(512);
		try {
			SqlDumpReader.readSqlDump(colIn, new SqlDumpReader() {
				private String insertTableName = null;
				public void handleNonInsertLine(String line) {
					super.handleNonInsertLine(line);
					if ("_search_scientific".equals(this.insertTableName))
						throw new RuntimeException("GOT_ALL_WE_NEED");
					this.insertTableName = null;
				}
				public void handleInsertRecord(String tableName, String record) {
					super.handleInsertRecord(tableName, record);
					if (this.insertTableName == null)
						this.insertTableName = tableName;
					if ("_search_scientific".equals(tableName)) {
						String[] rdParts = SqlDumpReader.parseRecordData(record);
						if (rdParts.length <= 14)
							System.out.println("INCOMPLETE: " + record + " ==> " + Arrays.toString(rdParts));
						if (!exploreDump)
							indexTaxonNameParts(rdParts);
						if ("'cichlasoma'".equals(rdParts[7]))
							System.out.println("  " + record);
						else if ("2".equals(rdParts[13]))
							System.out.println("  " + record);
					}
				}
				public void handleInsertStatementEnd(String tableName) {
					super.handleInsertStatementEnd(tableName);
					if (this.insertTableName == null)
						this.insertTableName = tableName;
				}
			});
		}
		catch (RuntimeException re) {
			if (!"GOT_ALL_WE_NEED".equals(re.getMessage()))
				throw re;
		}
		colIn.close();
		
		int familyCharCount = 0;
		for (Iterator fit = families.iterator(); fit.hasNext();)
			familyCharCount += ((String) fit.next()).length();
		int genusCharCount = 0;
		for (Iterator git = genera.iterator(); git.hasNext();)
			genusCharCount += ((String) git.next()).length();
		int genusKeyCharCount = 0;
		int speciesCount = 0;
		int speciesCharCount = 0;
		for (Iterator gkit = genusHierarchiesToSpecies.keySet().iterator(); gkit.hasNext();) {
			String genusKey = ((String) gkit.next());
			genusKeyCharCount += genusKey.length();
			if (genusKey.indexOf("     ") != -1) {
				Object genusHierarchy = genusKingdomsToGenusHierarchies.get(genusKey);
				if (genusHierarchy instanceof TreeSet) {
					for (Iterator ghit = ((TreeSet) genusHierarchy).iterator(); ghit.hasNext();) {
						String gh = ((String) ghit.next());
						if ((gh.indexOf("    ") != -1) && (((TreeSet) genusHierarchy).size() > 1))
							ghit.remove();
					}
					for (Iterator ghit = ((TreeSet) genusHierarchy).iterator(); ghit.hasNext();) {
						String gh = ((String) ghit.next());
						if ((gh.indexOf("   ") != -1) && (((TreeSet) genusHierarchy).size() > 1))
							ghit.remove();
					}
					for (Iterator ghit = ((TreeSet) genusHierarchy).iterator(); ghit.hasNext();) {
						String gh = ((String) ghit.next());
						if ((gh.indexOf("  ") != -1) && (((TreeSet) genusHierarchy).size() > 1))
							ghit.remove();
					}
					if (((TreeSet) genusHierarchy).size() == 1)
						genusKingdomsToGenusHierarchies.put(genusKey, ((TreeSet) genusHierarchy).first());
					else {
						System.out.println("Ambiguous genus-kingdom: " + genusKey);
						for (Iterator ghit = ((TreeSet) genusHierarchy).iterator(); ghit.hasNext();)
							System.out.println("  - " + ghit.next());
					}
				}
			}
			TreeSet speciesSet = ((TreeSet) genusHierarchiesToSpecies.get(genusKey));
			if (speciesSet != null) {
				speciesCount += speciesSet.size();
				for (Iterator sit = speciesSet.iterator(); sit.hasNext();)
					speciesCharCount += ((String) sit.next()).length();
			}
		}
		
		//	report findings
		System.out.println("Got " + genera.size() + " genera with " + genusCharCount + " characters in " + families.size() + " families with " + familyCharCount + " characters.");
		System.out.println("Got " + genusHierarchiesToSpecies.size() + " genus hierarchies (" + genusKingdomCounts.elementCount() + " kingdom-genus combinations) with " + genusKeyCharCount + " characters.");
		System.out.println("Got " + missingValidGenusHierarchyIDs.size() + " missing IDs of accepted names from synonyms.");
		for (Iterator mvidit = missingValidGenusHierarchyIDs.keySet().iterator(); mvidit.hasNext();) {
			Integer validTnpId = ((Integer) mvidit.next());
			String[] synTnpRd = ((String[]) missingValidGenusHierarchyIDs.get(validTnpId));
			System.out.println(" MISSING VALID ID: " + Arrays.toString(synTnpRd));
		}
		System.out.println("Got " + speciesCount + " species epithets with " + speciesCharCount + " characters.");
		int authorityNameCharCount = 0;
		for (Iterator anit = authorityNames.iterator(); anit.hasNext();)
			authorityNameCharCount += ((String) anit.next()).length();
		System.out.println("Got " + authorities.size() + " authorities.");
		System.out.println("Got " + authorityNames.size() + " authority names with " + authorityNameCharCount + " characters.");
		
		//	we're only exploring
		if (exploreDump)
			return;
		
		//	check where to write data
		File colHtpPath;
		if (args.length > 1)
			colHtpPath = new File(args[1]);
		else {
			String colHtpFolder = ColLocalSqlDumpConverter.class.getName();
			colHtpFolder = colHtpFolder.substring(0, colHtpFolder.lastIndexOf('.'));
			colHtpFolder = colHtpFolder.replaceAll("\\.", "/");
			colHtpPath = new File(colHtpFolder);
		}
		System.out.println(colHtpPath.getAbsolutePath());
		
		//	TODO_ write this zipped (and then, no need to, as JAR is zipped up anyway)
		File colData = new File(colHtpPath, "col.data.txt");
		IndexTrackingOutputStream cgOut = new IndexTrackingOutputStream(new BufferedOutputStream(new FileOutputStream(colData)));
		BufferedWriter cgBw = new BufferedWriter(new OutputStreamWriter(cgOut, "UTF-8"));
		
		//	add credit to source
		cgBw.write("// Data by Catalogue of Life (http://www.catalogueoflife.org)");
		cgBw.newLine();
		cgBw.write("// SQL Dump downloaded from: http://www.catalogueoflife.org/content/annual-checklist-archive");
		cgBw.newLine();
		cgBw.write("// Data extracted from table '_search_scientific'");
		cgBw.newLine();
		
		//	write data
		IndexMap genusLineByteIndexes = new IndexMap();
		IndexMap familyLineByteIndexes = new IndexMap();
		for (Iterator gkit = genusHierarchiesToSpecies.keySet().iterator(); gkit.hasNext();) {
			String genusKey = ((String) gkit.next());
			TreeSet gkSpeciesSet = ((TreeSet) genusHierarchiesToSpecies.get(genusKey));
			
			//	incomplete higher taxonomy, merge into full one if unambiguous
			if (genusKey.indexOf("     ") != -1) {
				Object genusHierarchy = genusKingdomsToGenusHierarchies.get(genusKey);
				if (genusHierarchy instanceof String) {
					TreeSet ghSpeciesSet = ((TreeSet) genusHierarchiesToSpecies.get(genusHierarchy));
					if (ghSpeciesSet != null) {
						ghSpeciesSet.addAll(gkSpeciesSet);
						continue;
					}
				}
			}
			
			//	extract genus and family
			String[] gkParts = genusKey.split("\\s");
			String genus = gkParts[5];
			String family = gkParts[4];
			
			//	remove genus and family from authority name list
			authorities.remove(genus);
			authorities.remove(family);
			authorityNames.remove(genus);
			authorityNames.remove(family);
			
			//	remember line offset for index access
			int lineByteIndex = cgOut.getBytesWritten();
			genusLineByteIndexes.addIndex(genus, lineByteIndex);
			if (family.length() != 0)
				familyLineByteIndexes.addIndex(family, lineByteIndex);
			
			//	write genus data
			for (int c = 0; c < genusKey.length(); c++) {
				if (genusKey.charAt(c) == ' ')
					cgBw.write('\t');
				else cgBw.write(genusKey.charAt(c));
			}
			if (gkSpeciesSet != null) {
				cgBw.write('\t');
				for (Iterator sit = gkSpeciesSet.iterator(); sit.hasNext();) {
					String species = ((String) sit.next());
					cgBw.write(species);
					if (sit.hasNext())
						cgBw.write(';');
				}
			}
			cgBw.newLine();
			cgBw.flush(); // need to empty intermediate buffers to get byte offset right
			
		}
		cgBw.close(); // also, finally, flushes tracking output stream
		
		//	write genus index file
		File colGenusIndex = new File(colHtpPath, "col.genusIndex.txt");
		BufferedWriter cgiBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colGenusIndex), "UTF-8"));
		for (Iterator git = genusLineByteIndexes.keySet().iterator(); git.hasNext();) {
			String genus = ((String) git.next());
			int[] genusLbis = genusLineByteIndexes.getIndexes(genus);
			cgiBw.write(genus);
			for (int i = 0; i < genusLbis.length; i++)
				cgiBw.write("\t" + Integer.toString(genusLbis[i], 16).toUpperCase());
			cgiBw.newLine();
		}
		cgiBw.flush();
		cgiBw.close();
		
		//	write family index file
		File colFamilyIndex = new File(colHtpPath, "col.familyIndex.txt");
		BufferedWriter cfiBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colFamilyIndex), "UTF-8"));
		for (Iterator fit = familyLineByteIndexes.keySet().iterator(); fit.hasNext();) {
			String family = ((String) fit.next());
			int[] familyLbis = familyLineByteIndexes.getIndexes(family);
			cfiBw.write(family);
			for (int i = 0; i < familyLbis.length; i++)
				cfiBw.write("\t" + Integer.toString(familyLbis[i], 16).toUpperCase());
			cfiBw.newLine();
		}
		cfiBw.flush();
		cfiBw.close();
		
		//	write authority name file
		File colAuthors = new File(colHtpPath, "col.authorNames.txt");
		BufferedWriter caBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colAuthors), "UTF-8"));
		for (Iterator anit = authorityNames.iterator(); anit.hasNext();) {
			caBw.write((String) anit.next());
			caBw.newLine();
		}
		caBw.flush();
		caBw.close();
	}
	
	private static class IndexTrackingOutputStream extends OutputStream {
		private OutputStream out;
		private int bytesWritten = 0;
		IndexTrackingOutputStream(OutputStream out) {
			this.out = out;
		}
		public void write(int b) throws IOException {
			this.out.write(b);
			this.bytesWritten++;
		}
		public void write(byte[] b) throws IOException {
			this.out.write(b);
			this.bytesWritten += b.length;
		}
		public void write(byte[] b, int off, int len) throws IOException {
			this.out.write(b, off, len);
			this.bytesWritten += len;
		}
		public void flush() throws IOException {
			//this.out.flush();
			/* No need to flush wrapped output stream here, it's enough on
			 * close(), and that way we increase the size of the chunks being
			 * actually written through to disc. */
		}
		public void close() throws IOException {
			this.out.flush();
			this.out.close();
		}
		int getBytesWritten() {
			return this.bytesWritten;
		}
	}
	
	private static class IndexMap extends TreeMap {
		void addIndex(String str, int index) {
			Object exStrIndexObj = this.get(str);
			if (exStrIndexObj instanceof LinkedHashSet)
				((LinkedHashSet) exStrIndexObj).add(new Integer(index));
			else if (exStrIndexObj instanceof Integer) {
				LinkedHashSet strIndexObj = new LinkedHashSet();
				strIndexObj.add(exStrIndexObj);
				strIndexObj.add(new Integer(index));
				this.put(str, strIndexObj);
			}
			else this.put(str, new Integer(index));
		}
		int[] getIndexes(String str) {
			int[] indexes;
			Object strIndexObj = this.get(str);
			if (strIndexObj instanceof LinkedHashSet) {
				indexes = new int[((LinkedHashSet) strIndexObj).size()];
				int i = 0;
				for (Iterator iit = ((LinkedHashSet) strIndexObj).iterator(); iit.hasNext();)
					indexes[i++] = ((Integer) iit.next()).intValue();
			}
			else if (strIndexObj instanceof Integer) {
				indexes = new int[1];
				indexes[0] = ((Integer) strIndexObj).intValue();
			}
			else indexes = new int[0];
			return indexes;
		}
	}
	
	private static TreeSet genera = new TreeSet();
	private static TreeSet families = new TreeSet();
	private static HashMap validGenusHierarchiesByID = new HashMap();
	private static HashMap synonymGenusHierarchiesByID = new HashMap();
	private static HashMap missingValidGenusHierarchyIDs = new HashMap();
	private static TreeMap genusHierarchiesToSpecies = new TreeMap();
	private static CountingSet genusKingdomCounts = new CountingSet(new TreeMap());
	private static TreeMap genusKingdomsToGenusHierarchies = new TreeMap();
	private static TreeSet authorities = new TreeSet();
	private static TreeSet authorityNames = new TreeSet();
	private static Pattern authorityNamePattern = Pattern.compile("[a-zA-Z\\'\\-]+");
	private static void indexTaxonNameParts(String[] tnpRd) {
		for (int p = 0; p < tnpRd.length; p++) {
			if ("Not assigned".equals(tnpRd[p]) || "Unassigned".equals(tnpRd[p]))
				tnpRd[p] = "";
		}
		
		String genus = getNormalizedString(tnpRd[7]);
		if (genus.length() < 3)
			return;
		genera.add(genus);
		String family = getNormalizedString(tnpRd[6]);
		if (family.length() != 0)
			families.add(family);
		
		String genusHierarchy = getNormalizedString(tnpRd[1]);
		genusHierarchy += (" " + getNormalizedString(tnpRd[2]));
		genusHierarchy += (" " + getNormalizedString(tnpRd[3]));
		genusHierarchy += (" " + getNormalizedString(tnpRd[4]));
		genusHierarchy += (" " + family);
		genusHierarchy += (" " + genus);
		
		String genusKingdom = getNormalizedString(tnpRd[1]);
		genusKingdom += ("     " + genus);
		genusKingdomCounts.add(genusKingdom);
		
		Integer tnpId = new Integer(tnpRd[0]);
		missingValidGenusHierarchyIDs.remove(tnpId);
		if ("5".equals(tnpRd[13])) {
			Integer validTnpId = new Integer(tnpRd[14]);
			String validGenusHierarchy = ((String) validGenusHierarchiesByID.get(validTnpId));
			if (validGenusHierarchy == null)
				validGenusHierarchy = ((String) synonymGenusHierarchiesByID.get(validTnpId));
			if (validGenusHierarchy == null)
				missingValidGenusHierarchyIDs.put(validTnpId, tnpRd);
			synonymGenusHierarchiesByID.put(tnpId, genusHierarchy);
		}
		else validGenusHierarchiesByID.put(tnpId, genusHierarchy);
		
		if (!genusHierarchy.equals(genusKingdom)) {
			Object genusHierarchyObject = genusKingdomsToGenusHierarchies.get(genusKingdom);
			if (genusHierarchyObject instanceof TreeSet)
				((TreeSet) genusHierarchyObject).add(genusHierarchy);
			else if (genusHierarchy.equals(genusHierarchyObject)) {}
			else if (genusHierarchyObject instanceof String) {
				TreeSet genusHierarchyList = new TreeSet();
				genusHierarchyList.add(genusHierarchyObject);
				genusHierarchyList.add(genusHierarchy);
				genusKingdomsToGenusHierarchies.put(genusKingdom, genusHierarchyList);
			}
			else genusKingdomsToGenusHierarchies.put(genusKingdom, genusHierarchy);
		}
		
		TreeSet speciesSet = ((TreeSet) genusHierarchiesToSpecies.get(genusHierarchy));
		if (speciesSet == null) {
			speciesSet = new TreeSet();
			genusHierarchiesToSpecies.put(genusHierarchy, speciesSet);
		}
		
		String species = getNormalizedString(tnpRd[9]);
		if ((species != null) && (species.trim().length() != 0))
			speciesSet.add(species.trim());
		
		String authority = getNormalizedString(tnpRd[12]);
		if ((authority != null) && (authority.trim().length() != 0)) {
			authorities.add(authority);
			for (Matcher m = authorityNamePattern.matcher(authority); m.find();) {
				String ap = m.group(0);
				if (ap.length() < 3)
					continue;
				if ((m.end() < authority.length()) && (authority.charAt(m.end()) == '.'))
					continue;
				if (!Character.isUpperCase(ap.charAt(0)) && (ap.indexOf('\'') == -1))
					continue;
				if (ap.matches("[A-Z\\'\\-]+")) {
					StringBuffer nAp = new StringBuffer();
					char lch = ((char) 0);
					for (int c = 0; c < ap.length(); c++) {
						char ch = ap.charAt(c);
						if ((lch == '\'') || (lch == '-') || (lch == 0))
							nAp.append(ch);
						else nAp.append(Character.toLowerCase(ch));
						lch = ch;
					}
					ap = nAp.toString();
				}
				authorityNames.add(ap);
			}
		}
	}
	/*
status (13): 1 valid, 5 synonym
accepted_species_id (14): 0 if valid, <validId> if synonym (in-table referencing id (0))
	 * 
  `id` int(10) NOT NULL,
   `kingdom` varchar(15) NOT NULL,
   `phylum` varchar(35) NOT NULL,
   `class` varchar(35) NOT NULL,
   `order` varchar(35) NOT NULL,
  `superfamily` varchar(35) NOT NULL,
   `family` varchar(35) NOT NULL,
   `genus` varchar(35) NOT NULL,
  `subgenus` varchar(35) NOT NULL,
  `species` varchar(75) NOT NULL,
  `infraspecific_marker` varchar(15) NOT NULL,
  `infraspecies` varchar(75) NOT NULL,
  `author` varchar(255) NOT NULL,
  `status` tinyint(1) NOT NULL,
  `accepted_species_id` int(10) NOT NULL,
  `accepted_species_name` varchar(255) NOT NULL,
  `accepted_species_author` varchar(255) NOT NULL,
  `source_database_id` int(10) NOT NULL,
  `source_database_name` varchar(255) NOT NULL,
  `has_preholocene` smallint(1) NOT NULL DEFAULT '0',
  `has_modern` smallint(1) NOT NULL DEFAULT '1',
  `is_extinct` smallint(1) NOT NULL DEFAULT '0',
	 */
	
	private static String getNormalizedString(String str) {
		StringBuffer nStr = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch < 127)
				nStr.append(ch);
			else {
				String nCh = StringUtils.getNormalForm(ch);
				if (nCh.matches("[A-Z]{2,}")) {
					nStr.append(nCh.charAt(0));
					nStr.append(nCh.substring(1).toLowerCase());
				}
				else nStr.append(nCh);
			}
		}
		str = nStr.toString();
		//	fix known data errors
		if ("0psamates".equals(str))
			return "Opsamates";
		else return str;
	}
}