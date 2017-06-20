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

package de.uka.ipd.idaho.plugins.modsReferencer.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * @author sautter
 *
 */
public class ModsGeneratorCsv {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File modsDataFile = new File("E:/Projektdaten/MODS_fish_papers.csv");
		final BufferedReader rawModsDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(modsDataFile)));
		Reader modsDataReader = new Reader() {
			public void close() throws IOException {
				rawModsDataReader.close();
			}
			public int read(char[] cbuf, int off, int len) throws IOException {
				int read = 0;
				char ch;
				while ((read < len) && (ch = this.localRead()) != '\u0000') {
					read++;
					cbuf[off++] = ch;
				}
				return ((read == 0) ? -1 : read);
			}
			private StringBuffer line = new StringBuffer();
			private int lineOff = 0;
			private char localRead() throws IOException {
				if (this.line == null)
					return '\u0000';
				
				else if (this.lineOff < this.line.length())
					return this.line.charAt(this.lineOff++);
				
				String rawLine = rawModsDataReader.readLine();
				if (rawLine == null) {
					this.line = null;
					this.lineOff = 0;
				}
				else {
					StringBuffer line = new StringBuffer();
					StringBuffer field = new StringBuffer();
					boolean quot = false;
					char ch;
					for (int c = 0; c < rawLine.length(); c++) {
						ch = rawLine.charAt(c);
						if (ch == '"')
							quot = !quot;
						else if (ch == ';') {
							if (quot)
								field.append(ch);
							else {
								this.appendField(line, field);
								field.delete(0, field.length());
							}
						}
						else field.append(ch);
					}
					this.appendField(line, field);
					line.append('\n');
					
					this.line = line;
					this.lineOff = 0;
//					System.out.print(line);
				}
				return this.localRead();
			}
			private void appendField(StringBuffer line, StringBuffer field) {
				if (line.length() != 0)
					line.append(',');
				
				if (field.length() == 0)
					line.append("\"\"");
				else if (field.charAt(0) == '"')
					line.append(field);
				else {
					line.append('"');
					line.append(field);
					line.append('"');
				}
			}
		};
		StringRelation modsData = StringRelation.readCsvData(modsDataReader, '"', true, null);
		for (int m = 0; m < modsData.size(); m++) {
			StringTupel data = modsData.get(m);
			
			String authorString = data.getValue("Authors");
			String[] rawAuthors = authorString.split("\\,");
			for (int a = 0; a < rawAuthors.length; a++) {
				rawAuthors[a] = rawAuthors[a].trim();
				if ("Jr.".equals(rawAuthors[a])) {
					rawAuthors[a-1] = (rawAuthors[a-1] + ", " + rawAuthors[a]);
					rawAuthors[a] = null;
				}
			}
			StringVector authors = new StringVector();
			for (int a = 0; a < rawAuthors.length; a++)
				if (rawAuthors[a] != null)
					authors.addElement(rawAuthors[a]);
			
			String year = data.getValue("Year");
			String title = data.getValue("Title");
			String journal = data.getValue("Journal");
			String issue = data.getValue("Issue");
			String firstPage = data.getValue("FirstPage");
			String lastPage = data.getValue("LastPage", firstPage);
			
			String url = getUrl(journal, issue, (firstPage + ((firstPage == lastPage) ? "" : ("-" + lastPage))));
			
			String identifierNumber = issue;
			while (identifierNumber.length() < 5)
				identifierNumber = ("0" + identifierNumber);
			String identifierPage = firstPage;
			while (identifierPage.length() < 3)
				identifierPage = ("0" + identifierPage);
			String identifier = ("z" + identifierNumber + "p" + identifierPage);
//			String identifier = url.substring(url.lastIndexOf(':') + 1);
			
			StringBuffer mods = new StringBuffer("<mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">\n");
				
			mods.append("  <mods:titleInfo>\n" +
				"    <mods:title>" + title + "</mods:title>\n" +
				"  </mods:titleInfo>\n");
			
			for (int a = 0; a < authors.size(); a++)
				mods.append("  <mods:name type=\"personal\">\n" +
						"    <mods:role>\n" +
						"      <mods:roleTerm>Author</mods:roleTerm>\n" +
						"    </mods:role>\n" +
						"    <mods:namePart>" + authors.get(a) + "</mods:namePart>\n" +
						"  </mods:name>\n");
			
			mods.append("  <mods:typeOfResource>text</mods:typeOfResource>\n");
			
			mods.append("  <mods:relatedItem type=\"host\">\n" +
				
				"    <mods:titleInfo>\n" +
				"      <mods:title>" + journal + "</mods:title>\n" +
				"    </mods:titleInfo>\n" +
				
				"    <mods:part>\n" +
				
				"      <mods:detail type=\"volume\">\n" +
				"        <mods:number>" + issue + "</mods:number>\n" +
				"      </mods:detail>\n" +
				
				"      <mods:extent unit=\"page\">\n" +
				"        <mods:start>" + firstPage + "</mods:start>\n" +
				"        <mods:end>" + lastPage + "</mods:end>\n" +
				"      </mods:extent>\n" +
				
				"      <mods:date>" + year + "</mods:date>\n" +
				
				"    </mods:part>\n" +
				
				"  </mods:relatedItem>\n");
				
			mods.append("  <mods:identifier type=\"ZooBank-PUB\">" + identifier + "</mods:identifier>\n");
			
			mods.append("  <mods:location>\n" +
				"    <mods:url>" + url + "</mods:url>\n" +
				"  </mods:location>\n");
				
			mods.append("</mods:mods>\n");
			
			System.out.print(mods);
			
			File cacheFile = new File("E:/GoldenGATEv3/Plugins/AnalyzerData/ModsReferencerData/cache/" + identifier + ".xml");
			if (cacheFile.exists())
				continue;
			cacheFile.createNewFile();
			
			StringVector cacheLines = new StringVector();
			cacheLines.parseAndAddElements(mods.toString(), "\n");
			OutputStreamWriter cacheWriter = new OutputStreamWriter(new FileOutputStream(cacheFile), "UTF-8");
			cacheLines.storeContent(cacheWriter);
			cacheWriter.close();
			System.out.println(" ... cached.");
			System.out.println();
		}
	}
	
	private static final Html html = new Html();
	private static final Parser parser = new Parser(html);
	private static String getUrl(String journal, final String issue, final String pages) throws IOException {
		URL searchUrl = new URL("http://www.zoobank.org/Search.aspx?search=" + journal + "+" + issue);
		final String[] url = new String[1];
		parser.stream(new InputStreamReader(searchUrl.openStream(), "UTF-8"), new TokenReceiver() {
			private boolean inData = false;
			private String href = null;
			private String dataString = null;
			public void close() throws IOException {
				// nothing to do here
			}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					
					if (html.isEndTag(token)) {
						if ("span".equals(type))
							this.inData = false;
						
						else if (this.inData) {
							
							if ("a".equals(type)) {
								String data = IoTools.prepareForPlainText(this.dataString).trim();
								if (data.indexOf("," + issue + ":" + pages) != -1)
									url[0] = this.href;
								
								this.href = null;
								this.dataString = null;
							}
						}
					}
					
					else if (html.isSingularTag(token)) {}
					
					else {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						
						if ("span".equals(type)) {
							if ("ctl00_ContentPlaceHolder_PubResults".equals(tnas.getAttribute("id")))
								this.inData = true;
						}
						
						else if (this.inData) {
							if ("a".equals(type)) {
								String href = tnas.getAttribute("href");
								if (href != null) {
									this.href = href;
									this.dataString = "";
								}
							}
						}
					}
				}
				else if (this.dataString != null) {
					this.dataString += token.trim();
				}
			}
		});
		return ("http://www.zoobank.org" + url[0]);
	}
	
	/*
	Authors	Year	Title	Journal	Issue	FirstPage	LastPage
	
<mods:mods xmlns:mods="http://www.loc.gov/mods/v3">

  <mods:titleInfo>
    <mods:title>Five new species of the damselfish genus Chromis (Perciformes: Labroidei: Pomacentridae) from deep coral reefs in the tropical western Pacific.</mods:title>
  </mods:titleInfo>
  
  <mods:name type="personal">
    <mods:role>
      <mods:roleTerm>Author</mods:roleTerm>
    </mods:role>
    <mods:namePart>Pyle, R. L.</mods:namePart>
  </mods:name>
  <mods:name type="personal">
    <mods:role>
      <mods:roleTerm>Author</mods:roleTerm>
    </mods:role>
    <mods:namePart>Earle, J. L.</mods:namePart>
  </mods:name>
  
  <mods:typeOfResource>text</mods:typeOfResource>
  
  <mods:relatedItem type="host">
  
    <mods:titleInfo>
      <mods:title>Zootaxa</mods:title>
    </mods:titleInfo>
    
    <mods:part>
    
      <mods:detail type="volume">
        <mods:number>1671</mods:number>
      </mods:detail>
      
      <mods:extent unit="page">
        <mods:start>3</mods:start>
        <mods:end>31</mods:end>
      </mods:extent>
      
      <mods:date>2007</mods:date>
      
    </mods:part>
    
  </mods:relatedItem>
  
  <mods:identifier type="HNS-PUB">21356</mods:identifier>
  <mods:location>
    <mods:url>http://hdl.handle.net/10199/15417</mods:url>
  </mods:location>
</mods:mods>

	 */
}
