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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider;

/**
 * @author sautter
 *
 */
public class ColLocalHigherTaxonomyProvider extends HigherTaxonomyProvider {
	private ByteGridIndex genusIndex = null;
	private ByteGridIndex familyIndex = null;
	private ByteGridArray data = null;
	private ByteStringSet authorities = null;
	
	/** public zero-argument constructor for class loading */
	public ColLocalHigherTaxonomyProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getDataSourceName()
	 */
	public String getDataSourceName() {
		return "CoL-Local";
	}
	
	private InputStream getInputStream(String dataName) throws IOException {
		if (this.dataProvider.isDataAvailable(dataName))
			return this.dataProvider.getInputStream(dataName);
		String dataPath = ColLocalHigherTaxonomyProvider.class.getName();
		dataPath = dataPath.substring(0, dataPath.lastIndexOf('.'));
		dataPath = dataPath.replaceAll("\\.", "/");
		return this.getClass().getClassLoader().getResourceAsStream(dataPath + "/" + dataName);
	}
	
	private ByteGridIndex readIndex(String dataName) throws IOException {
		ByteGridIndex index = new ByteGridIndex();
		BufferedReader ibr = new BufferedReader(new InputStreamReader(this.getInputStream(dataName), "UTF-8"));
		for (String line; (line = ibr.readLine()) != null;) {
			String[] lpts = line.split("\\t");
			int[] starts = new int[lpts.length-1];
			for (int p = 1; p < lpts.length; p++)
				starts[p-1] = Integer.parseInt(lpts[p], 16);
			index.put(lpts[0], new ByteGridIndexEntry(starts));
		}
		ibr.close();
		return index;
	}
	
	private ByteStringSet readAuthorities(String dataName) {
		ByteStringSet authorities = new ByteStringSet();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getInputStream(dataName), "UTF-8"));
			for (String line; (line = br.readLine()) != null;)
				authorities.add(line);
			br.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return authorities;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#getHierarchy(java.lang.String, java.lang.String, boolean)
	 */
	public Properties getHierarchy(String epithet, String rank, boolean allowWebAccess) {
		
		/* use default mechanism if web access allowed (cache miss will
		 * delegate to loadHierarchy() from super class) */
		if (allowWebAccess)
			return super.getHierarchy(epithet, rank, allowWebAccess);
		
		/* if no web access allowed, let super class do lookup first (caches
		 * might end up saving us from having to load all that data), and then
		 * delegate to loadHierarchy(), as the lookup is actually local */
		else {
			Properties hierarchy = super.getHierarchy(epithet, rank, allowWebAccess);
			if (hierarchy == null) try {
				hierarchy = this.loadHierarchy(epithet, rank);
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.out);
			}
			return hierarchy;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#loadHierarchy(java.lang.String, java.lang.String)
	 */
	protected Properties loadHierarchy(String epithet, String rank) throws IOException {
		
		//	initialize if not already done (we need to synchronize this, as this method might be called by multiple threads concurrently)
		synchronized (this) {
			if (this.data == null) {
				InputStream in = new BufferedInputStream(this.getInputStream("col.data.txt"));
				this.data = new ByteGridArray();
				byte[] buf = new byte[1024];
				for (int r; (r = in.read(buf, 0, buf.length)) != -1;)
					this.data.append(buf, 0, r);
				in.close();
			}
			if (this.genusIndex == null)
				this.genusIndex = this.readIndex("col.genusIndex.txt");
			if (this.familyIndex == null)
				this.familyIndex = this.readIndex("col.familyIndex.txt");
		}
		
		//	get higher taxonomy for family, including list of genera 
		if (FAMILY_ATTRIBUTE.equals(rank)) {
			ByteGridIndexEntry bgi = ((ByteGridIndexEntry) this.familyIndex.get(epithet));
			if (bgi == null)
				return null;
			Properties higherTaxonomy = new Properties();
			StringBuffer genera = null;
			for (int s = 0; s < bgi.starts.length; s++) {
				byte[] line = this.data.readLine(bgi.starts[s]);
				String[] lineParts = this.split(line);
				for (int p = 0; p < Math.min(lineParts.length, rankNames.length); p++) {
					if ("".equals(lineParts[p]))
						continue;
					if (higherTaxonomy.containsKey(rankNames[p]))
						continue;
					if (GENUS_ATTRIBUTE.equals(rankNames[p])) {
						if (genera == null)
							genera = new StringBuffer(lineParts[p]);
						else {
							genera.append(';');
							genera.append(lineParts[p]);
						}
						break;
					}
					else higherTaxonomy.setProperty(rankNames[p], lineParts[p]);
				}
			}
			if (genera != null)
				higherTaxonomy.setProperty(GENUS_ATTRIBUTE, genera.toString());
			return higherTaxonomy;
		}
		
		//	get higher taxonomy for genus
		else if (GENUS_ATTRIBUTE.equals(rank)) {
			ByteGridIndexEntry bgi = ((ByteGridIndexEntry) this.genusIndex.get(epithet));
			if (bgi == null)
				return null;
			Properties higherTaxonomy = new Properties();
			StringBuffer species = null;
			for (int s = 0; s < bgi.starts.length; s++) {
				byte[] line = this.data.readLine(bgi.starts[s]);
				String[] lineParts = this.split(line);
				for (int p = 0; p < Math.min(lineParts.length, rankNames.length); p++) {
					if ("".equals(lineParts[p]))
						continue;
					if (higherTaxonomy.containsKey(rankNames[p]))
						continue;
					higherTaxonomy.setProperty(rankNames[p], lineParts[p]);
				}
				if (lineParts.length > rankNames.length) {
					if (species == null)
						species = new StringBuffer(lineParts[rankNames.length]);
					else {
						species.append(';');
						species.append(lineParts[rankNames.length]);
					}
				}
			}
			if (species != null)
				higherTaxonomy.setProperty(SPECIES_ATTRIBUTE, species.toString());
			return higherTaxonomy;
		}
		
		//	we can only handle families and genera ...
		else return null;
	}
	
	private String[] split(byte[] data) {
		ArrayList dataParts = new ArrayList(7);
		StringBuffer dataPart = null;
		for (int c = 0; c < data.length; c++) {
			char ch = ((char) data[c]);
			if (ch == '\t') {
				dataParts.add((dataPart == null) ? "" : dataPart.toString());
				dataPart = null;
			}
			else {
				if (dataPart == null)
					dataPart = new StringBuffer();
				dataPart.append(ch);
			}
		}
		if (dataPart != null)
			dataParts.add(dataPart.toString());
		return ((String[]) dataParts.toArray(new String[dataParts.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy.HigherTaxonomyProvider#isKnownNegative(java.lang.String)
	 */
	public boolean isKnownNegative(String str) {
		synchronized (this) {
			if (this.authorities == null)
				this.authorities = this.readAuthorities("col.authorNames.txt");
		}
		return this.authorities.contains(str);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ColLocalHigherTaxonomyProvider htp = new ColLocalHigherTaxonomyProvider();
		htp.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/HigherTaxonomyData/ColLocalData")));
		htp.init();
		Properties ht;
		
		ht = htp.loadHierarchy("Formicidae", FAMILY_ATTRIBUTE);
		System.out.println(ht);
		
		ht = htp.loadHierarchy("Camponotus", GENUS_ATTRIBUTE);
		System.out.println(ht);
		
		ht = htp.loadHierarchy("Hymenoptera", GENUS_ATTRIBUTE);
		System.out.println(ht);
		
		System.out.println(htp.isKnownNegative("Agosti"));
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		JOptionPane.showConfirmDialog(null, "Check Memory (start)");
//		File colPath = new File("E:/Install/CoL");
//		
//		//	read data into ByteGridArray
//		File colGenera = new File(colPath, "col.data.txt");
//		InputStream in = new BufferedInputStream(new FileInputStream(colGenera));
//		ByteGridArray bga = new ByteGridArray();
//		byte[] buf = new byte[1024];
//		for (int r; (r = in.read(buf, 0, buf.length)) != -1;)
//			bga.append(buf, 0, r);
//		in.close();
//		JOptionPane.showConfirmDialog(null, "Check Memory (data loaded)");
//		
//		//	read index files
//		ByteGridIndex genusIndex = readIndex(colPath, "col.genusIndex.txt");
//		ByteGridIndex familyIndex = readIndex(colPath, "col.familyIndex.txt");
//		JOptionPane.showConfirmDialog(null, "Check Memory (indexes loaded)");
//		
//		//	TODO test this sucker
//		String family = "Formicidae";
//		ByteGridIndexEntry fBgi = ((ByteGridIndexEntry) familyIndex.get(family));
//		for (int s = 0; s < fBgi.starts.length; s++) {
//			byte[] fLine = bga.readLine(fBgi.starts[s]);
//			System.out.println("FAMILY: " + new String(fLine, "UTF-8"));
//		}
//		
//		String genus = "Formicium";
//		ByteGridIndexEntry gBgi = ((ByteGridIndexEntry) genusIndex.get(genus));
//		for (int s = 0; s < gBgi.starts.length; s++) {
//			byte[] gLine = bga.readLine(gBgi.starts[s]);
//			System.out.println("GENUS: " + new String(gLine, "UTF-8"));
//		}
//		
//		JOptionPane.showConfirmDialog(null, "Check Memory (final)");
//	}
//	
//	private static ByteGridIndex readIndex(File path, String name) throws IOException {
//		ByteGridIndex index = new ByteGridIndex();
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path, name)), "UTF-8"));
//		for (String line; (line = br.readLine()) != null;) {
//			String[] lpts = line.split("\\t");
//			int[] starts = new int[lpts.length-1];
//			for (int p = 1; p < lpts.length; p++)
//				starts[p-1] = Integer.parseInt(lpts[p], 16);
//			index.put(lpts[0], new ByteGridIndexEntry(starts));
//		}
//		br.close();
//		return index;
//	}
//	
//	private static ByteStringSet readAuthorities(File path, String name) throws IOException {
//		ByteStringSet authorities = new ByteStringSet();
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path, name)), "UTF-8"));
//		for (String line; (line = br.readLine()) != null;)
//			authorities.add(line);
//		br.close();
//		return authorities;
//	}
	
	private static class ByteString {
		byte[] data;
		ByteString(byte[] data) {
			this.data = data;
		}
		ByteString(String data) {
			this(data.getBytes());
		}
		public int hashCode() {
			return Arrays.hashCode(this.data);
		}
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof ByteString) {
				if (((ByteString) obj).data.length != this.data.length)
					return false;
				for (int i = 0; i < this.data.length; i++) {
					if (((ByteString) obj).data[i] != this.data[i])
						return false;
				}
				return true;
			}
			else return false;
		}
		public String toString() {
			return new String(this.data);
		}
	}
	
	private static class ByteStringSet {
		private HashSet data = new HashSet();
		public boolean contains(String key) {
			return this.data.contains(new ByteString(key.toUpperCase()));
		}
		public void add(String key) {
			this.data.add(new ByteString(key.toUpperCase()));
		}
	}
	
	private static class ByteGridIndex {
		private HashMap data = new HashMap();
		public ByteGridIndexEntry get(String key) {
			return ((ByteGridIndexEntry) ((key == null) ? null : this.data.get(new ByteString(key.toUpperCase()))));
		}
		public void put(String key, ByteGridIndexEntry value) {
			this.data.put(new ByteString(key.toUpperCase()), value);
		}
	}
	
	private static class ByteGridIndexEntry {
		int[] starts;
		ByteGridIndexEntry(int[] starts) {
			this.starts = starts;
		}
	}
	
	private static class ByteGridArray {
		private static final int dataRowLength = 16192;
		int size = 0;
		byte[][] data = new byte[10][];
		int dataOffset = 0;
		byte[] dataRow = new byte[dataRowLength];
		int dataRowOffset = 0;
		ByteGridArray() {
			this.data[0] = this.dataRow;
		}
		
//		int read(int index) {
//			if (this.size <= index)
//				return -1;
//			int d = (index / dataRowLength);
//			int s = (index % dataRowLength);
//			int b = this.data[d][s++];
//			if (b < 0)
//				b += 256;
//			return b;
//		}
//		
		byte[] readLine(int start) {
			if (this.size <= start)
				return new byte[0];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int d = (start / dataRowLength);
			int s = (start % dataRowLength);
			while (true) {
				if (this.dataOffset < d)
					break;
				if (this.data[d].length <= s) {
					if (this.dataOffset == d)
						break;
					d++;
					s = 0;
				}
				byte b = this.data[d][s++];
				if ((b == '\n') || (b == '\r'))
					break;
				else baos.write(b);
			}
			return baos.toByteArray();
		}
		
		void append(byte[] buffer, int off, int len) {
			while (len > 0) {
				this.ensureCapacity();
				int drLen = Math.min(len, (this.dataRow.length - this.dataRowOffset));
				System.arraycopy(buffer, off, this.dataRow, this.dataRowOffset, drLen);
				off += drLen;
				len -= drLen;
				this.dataRowOffset += drLen;
				this.size += drLen;
			}
		}
		private void ensureCapacity() {
			if (this.dataRowOffset < this.dataRow.length)
				return;
			if ((this.dataOffset + 1) == this.data.length) {
				byte[][] data = new byte[this.data.length * 2][];
				System.arraycopy(this.data, 0, data, 0, this.data.length);
				this.data = data;
			}
			this.dataRowOffset = 0;
			this.dataRow = new byte[dataRowLength];
			this.dataOffset++;
			this.data[this.dataOffset] = this.dataRow;
		}
	}
}