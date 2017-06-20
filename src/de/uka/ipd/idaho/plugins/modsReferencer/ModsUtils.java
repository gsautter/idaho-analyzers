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

package de.uka.ipd.idaho.plugins.modsReferencer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Function library for handling MODS document meta data
 * 
 * @author sautter
 */
public class ModsUtils implements ModsConstants {
	
	//	TODO make validity check more flexible, and include "proceedings" reference types and URLs
	
	/**
	 * Container for the data in a MODS header
	 * 
	 * @author sautter
	 */
	public static class ModsDataSet {
		/**
		 * the identifier of the document this MODS metadata set belongs to
		 */
		public final String id;
		
		/**
		 * the type of identifier of the document this MODS metadata set belongs
		 * to, usually revealing the source
		 */
		public final String idType;
		
		/**
		 * @return the title of the document this MODS metadata set belongs to
		 */
		public String getTitle() {
			return this.title;
		}
		String title = "";
		
		/**
		 * @return an array holding the authors of the document this MODS
		 *         metadata set belongs to
		 */
		public String[] getAuthors() {
			String[] authors = new String[this.authors.length];
			System.arraycopy(this.authors, 0, authors, 0, authors.length);
			return authors;
		}
		/**
		 * @return authors of the document this MODS metadata set belongs to,
		 *         represented as one string with commas separating all but the
		 *         second to last from the last author, the latter two being
		 *         separated by an '&amp;'
		 */
		public String getAuthorString() {
			StringBuffer authors = new StringBuffer();
			for (int a = 0; a < this.authors.length; a++) {
				if (a != 0) {
					if ((a + 1) == this.authors.length)
						authors.append(" & ");
					else authors.append(", ");
				}
				authors.append(this.authors[a]);
			}
			return authors.toString();
		}
		String[] authors = new String[0];
		
		/**
		 * @return the year the document this MODS metadata set belongs to has
		 *         been published
		 */
		public int getYear() {
			return this.year;
		}
		int year = Integer.MIN_VALUE;
		
		/**
		 * @return the url of the PDF version of the document this MODS metadata
		 *         set belongs to
		 */
		public String getUrl() {
			return this.url;
		}
		String url = "";
		
		
		/**
		 * @return true if the document this MODS metadata set belongs to is
		 *         part of a larger publication (an article in a journal or a
		 *         chapter in a book), false otherwise
		 */
		public boolean isPart() {
			return this.isPart;
		}
		boolean isPart = false;
		
		/**
		 * @return the page number of the first page of the document this MODS
		 *         metadata set belongs to, or Integer.MIN_VALUE if isPart()
		 *         returns false
		 */
		public int getStartPageNumber() {
			return this.startPageNumber;
		}
		int startPageNumber = Integer.MIN_VALUE;
		
		/**
		 * @return the page number of the last page of the document this MODS
		 *         metadata set belongs to, or Integer.MIN_VALUE if isPart()
		 *         returns false
		 */
		public int getEndPageNumber() {
			return this.endPageNumber;
		}
		int endPageNumber = Integer.MIN_VALUE;
		
		
		/**
		 * @return the title of the host publication the document this MODS
		 *         metadata set belongs to was published in, or null if isPart()
		 *         returns false
		 */
		public String getHostTitle() {
			return (this.isPart ? this.hostTitle : null);
		}
		String hostTitle = "";
		
		/**
		 * @return the editors of the host publication the document this MODS
		 *         metadata set belongs to was published in (an empty array if
		 *         the editors are not known), or null if isPart() returns false
		 */
		public String[] getHostEditors() {
			if (this.isPart) {
				String[] hostEditors = new String[this.hostEditors.length];
				System.arraycopy(this.hostEditors, 0, hostEditors, 0, hostEditors.length);
				return hostEditors;
			}
			else return null;
		}
		/**
		 * @return the editors of the host publication the document this MODS
		 *         metadata set belongs to was published in (an empty array if
		 *         the editors are not known), represented as one string with
		 *         commas separating all but the second to last from the last
		 *         editor, the latter two being separated by an '&amp;', the
		 *         empty string if the editors are not known, or null if
		 *         isPart() returns false
		 */
		public String getHostEditorString() {
			if (this.isPart) {
				StringBuffer hostEditors = new StringBuffer();
				for (int a = 0; a < this.hostEditors.length; a++) {
					if (a != 0) {
						if ((a + 1) == this.hostEditors.length)
							hostEditors.append(" & ");
						else hostEditors.append(", ");
					}
					hostEditors.append(this.hostEditors[a]);
				}
				return hostEditors.toString();
			}
			else return null;
		}
		String[] hostEditors = new String[0];
		
		
		/**
		 * @return true if the document this MODS metadata set belongs to is a
		 *         journal or a part of one (an article), false otherwise
		 */
		public boolean isJournal() {
			return this.isJournal;
		}
		boolean isJournal = false;
		
		/**
		 * @return the name of the journal the document this MODS metadata set
		 *         belongs to was published in, or null if isPart() returns
		 *         false
		 */
		public String getJournalName() {
			return (this.isJournal ? this.journalName : null);
		}
		String journalName = "";
		
		/**
		 * @return the number of the journal issue the document this MODS
		 *         metadata set belongs to was published in, or
		 *         Integer.MIN_VALUE if isJournal() returns false
		 */
		public int getVolumeNumber() {
			return this.volumeNumber;
		}
		int volumeNumber = Integer.MIN_VALUE;
		
		
		/**
		 * @return true if the document this MODS metadata set belongs to is a
		 *         book or a part of one (eg a chapter), false otherwise
		 */
		public boolean isBook() {
			return this.isBook;
		}
		boolean isBook = false;
		
		/**
		 * @return the publisher of the book the document this MODS metadata set
		 *         belongs to was published in, or null if isBook() returns
		 *         false
		 */
		public String getBookPublisherName() {
			return (this.isBook ? this.bookPublisherName : null);
		}
		String bookPublisherName = "";
		
		/**
		 * @return the publisher location of the book the document this MODS
		 *         metadata set belongs to was published in, or null if isBook()
		 *         returns false
		 */
		public String getBookPublisherLocation() {
			return (this.isBook ? this.bookPublisherLocation : null);
		}
		String bookPublisherLocation = "";
		
		ModsDataSet(String id, String idType) {
			this.id = id;
			this.idType = idType;
		}
		
		/**
		 * Check whether or not this MODS metadata set is valid, i.e. all
		 * required information is given.
		 * @return true if this MODS metadata set is valid, false otherwise
		 */
		public boolean isValid() {
			return (getErrorReport(this).length == 0);
		}
		
		/**
		 * Convert the data back into XML.
		 * @return an annotation representing the MODS metadata set in XML
		 */
		public MutableAnnotation getModsHeader() {
			return this.getModsHeader(false);
		}
		
		/**
		 * Convert the data back into XML.
		 * @param ignoreValidity return MODS header even if not valid?
		 * @return an annotation representing the MODS metadata set in XML
		 */
		public MutableAnnotation getModsHeader(boolean ignoreValidity) {
			if (!ignoreValidity && !this.isValid())
				return null;
			
			try {
				StringWriter sw = new StringWriter();
				this.writeModsHeader(sw, ignoreValidity);
				MutableAnnotation modsHeader = SgmlDocumentReader.readDocument(new StringReader(sw.toString()));
				cleanModsHeader(modsHeader);
				return modsHeader;
			}
			catch (IOException ioe) {
				return null; // won't happen with a StringWriter and StringReader, but Java don't know ...
			}
		}
		
		/**
		 * Write the XML representation of this MODS metadata set to some writer.
		 * @param w the writer to write to
		 * @return an annotation representing the MODS metadata set in XML
		 */
		public void writeModsHeader(Writer w) throws IOException {
			this.writeModsHeader(w, false);
		}
		
		/**
		 * Write the XML representation of this MODS metadata set to some writer.
		 * @param w the writer to write to
		 * @param ignoreValidity return MODS header even if not valid?
		 * @return an annotation representing the MODS metadata set in XML
		 */
		public void writeModsHeader(Writer w, boolean ignoreValidity) throws IOException {
			if (!ignoreValidity && !this.isValid())
				throw new IOException("This MODS header is not valid.");
			
			BufferedWriter bw = ((w instanceof BufferedWriter) ? ((BufferedWriter) w) : new BufferedWriter(new FilterWriter(w) {
				public void flush() throws IOException {} // prevent flushing argument writer
			}));
			
			bw.write("<mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">");
			bw.newLine();
			
			bw.write("<mods:titleInfo>");
			bw.newLine();
			bw.write("<mods:title>" + AnnotationUtils.escapeForXml(this.title) + "</mods:title>");
			bw.newLine();
			bw.write("</mods:titleInfo>");
			bw.newLine();
			
			for (int a = 0; a < this.authors.length; a++) {
				bw.write("<mods:name type=\"personal\">");
				bw.newLine();
				bw.write("<mods:role>");
				bw.newLine();
				bw.write("<mods:roleTerm>Author</mods:roleTerm>");
				bw.newLine();
				bw.write("</mods:role>");
				bw.newLine();
				bw.write("<mods:namePart>" + AnnotationUtils.escapeForXml(this.authors[a]) + "</mods:namePart>");
				bw.newLine();
				bw.write("</mods:name>");
				bw.newLine();
			}
			
			bw.write("<mods:typeOfResource>text</mods:typeOfResource>");
			bw.newLine();
			
			if (this.isPart) {
				bw.write("<mods:relatedItem type=\"host\">");
				bw.newLine();
				if (this.isJournal) {
					bw.write("<mods:titleInfo>");
					bw.newLine();
					bw.write("<mods:title>" + AnnotationUtils.escapeForXml(this.journalName) + "</mods:title>");
					bw.newLine();
					bw.write("</mods:titleInfo>");
					bw.newLine();
				}
				else if (this.isBook) {
					bw.write("<mods:titleInfo>");
					bw.newLine();
					bw.write("<mods:title>" + AnnotationUtils.escapeForXml(this.hostTitle) + "</mods:title>");
					bw.newLine();
					bw.write("</mods:titleInfo>");
					bw.newLine();
					this.writeOriginInfo(bw);
				}
				
				if (this.hostTitle != null)
					for (int e = 0; e < this.hostEditors.length; e++) {
						bw.write("<mods:name type=\"personal\">");
						bw.newLine();
						bw.write("<mods:role>");
						bw.newLine();
						bw.write("<mods:roleTerm>Editor</mods:roleTerm>");
						bw.newLine();
						bw.write("</mods:role>");
						bw.newLine();
						bw.write("<mods:namePart>" + AnnotationUtils.escapeForXml(this.hostEditors[e]) + "</mods:namePart>");
						bw.newLine();
						bw.write("</mods:name>");
						bw.newLine();
					}
				
				bw.write("<mods:part>");
				bw.newLine();
				
				if (this.isJournal) {
					bw.write("<mods:detail type=\"volume\">");
					bw.newLine();
					bw.write("<mods:number>" + this.volumeNumber + "</mods:number>");
					bw.newLine();
					bw.write("</mods:detail>");
					bw.newLine();
					if (this.hostTitle != null) {
						bw.write("<mods:detail type=\"title\">");
						bw.newLine();
						bw.write("<mods:title>" + AnnotationUtils.escapeForXml(this.hostTitle) + "</mods:title>");
						bw.newLine();
						bw.write("</mods:detail>");
						bw.newLine();
					}
					bw.write("<mods:date>" + this.year + "</mods:date>");
					bw.newLine();
				}
				bw.write("<mods:extent unit=\"page\">");
				bw.newLine();
				bw.write("<mods:start>" + this.startPageNumber + "</mods:start>");
				bw.newLine();
				bw.write("<mods:end>" + this.endPageNumber + "</mods:end>");
				bw.newLine();
				bw.write("</mods:extent>");
				bw.newLine();
				
				bw.write("</mods:part>");
				bw.newLine();
				
				bw.write("</mods:relatedItem>");
				bw.newLine();
			}
			else if (this.isJournal) {
				bw.write("<mods:relatedItem type=\"host\">");
				bw.newLine();
				bw.write("<mods:titleInfo>");
				bw.newLine();
				bw.write("<mods:title>" + AnnotationUtils.escapeForXml(this.journalName) + "</mods:title>");
				bw.newLine();
				bw.write("</mods:titleInfo>");
				bw.newLine();
				bw.write("<mods:part>");
				bw.newLine();
				bw.write("<mods:detail type=\"volume\">");
				bw.newLine();
				bw.write("<mods:number>" + this.volumeNumber + "</mods:number>");
				bw.newLine();
				bw.write("</mods:detail>");
				bw.newLine();
				bw.write("<mods:date>" + this.year + "</mods:date>");
				bw.newLine();
				bw.write("</mods:part>");
				bw.newLine();
				bw.write("</mods:relatedItem>");
			}
			else if (this.isBook)
				this.writeOriginInfo(bw);
			
			bw.write("<mods:location>");
			bw.newLine();
			bw.write("<mods:url>" + AnnotationUtils.escapeForXml(this.url) + "</mods:url>");
			bw.newLine();
			bw.write("</mods:location>");
			bw.newLine();
			
			bw.write("<mods:identifier type=\"" + AnnotationUtils.escapeForXml(this.idType) + "\">" + AnnotationUtils.escapeForXml(this.id) + "</mods:identifier>");
			bw.newLine();
			
			bw.write("</mods:mods>");
			bw.newLine();
			
			if (bw != w)
				bw.flush();
		}
		
		private final void writeOriginInfo(BufferedWriter bw) throws IOException {
			bw.write("<mods:originInfo>");
			bw.newLine();
			bw.write("<mods:dateIssued>" + this.year + "</mods:dateIssued>");
			bw.newLine();
			bw.write("<mods:publisher>" + AnnotationUtils.escapeForXml(this.bookPublisherName) + "</mods:publisher>");
			bw.newLine();
			bw.write("<mods:place>");
			bw.newLine();
			bw.write("<mods:placeTerm>" + AnnotationUtils.escapeForXml(this.bookPublisherLocation) + "</mods:placeTerm>");
			bw.newLine();
			bw.write("</mods:place>");
			bw.newLine();
			bw.write("</mods:originInfo>");
			bw.newLine();
		}
		
		/*
Whole book:
<mods>
   <titleInfo>
       <title>The book title</title>
   </titleInfo>
   <name type="personal">
       <namePart>The Author</namePart>
       <role>
           <roleTerm>author</roleTerm>
       </role>
   </name>
   <typeOfResource>text</typeOfResource>
   <originInfo>
       <dateIssued>2010</dateIssued>
       <publisher>The Publisher</publisher>
       <place>
           <placeTerm>Publisher Location</placeTerm>
       </place>
   </originInfo>
   <identifier type="TheType">TheIdentifier</identifier>
   <location>
       <url>http://www.host.org/doc.pdf</url>
   </location>
</mods>


Book chapter:
<mods>
   <titleInfo>
       <title>The chapter title</title>
   </titleInfo>
   <name type="personal">
       <namePart>The Author</namePart>
       <role>
           <roleTerm>author</roleTerm>
       </role>
   </name>
   <typeOfResource>text</typeOfResource>
   <relatedItem type="host">
       <titleInfo>
           <title>The book title</title>
       </titleInfo>
       <originInfo>
           <dateIssued>2010</dateIssued>
           <publisher>The Publisher</publisher>
           <place>
               <placeTerm>Publisher Location</placeTerm>
           </place>
       </originInfo>
       <name>
           <namePart>The Editor (if any)</namePart>
           <role>
               <roleTerm>editor</roleTerm>
           </role>
       </name>
       <part>
           <extent unit="pages">
               <start>1024</start>
               <end>1040</end>
           </extent>
       </part>
   </relatedItem>
   <identifier type="TheType">TheIdentifier</identifier>
   <location>
       <url>http://www.host.org/doc.pdf</url>
   </location>
</mods>


Whole journal volume:
<mods>
   <titleInfo>
       <title>The journal volume title</title>
   </titleInfo>
   <name type="personal">
       <namePart>The Author</namePart>
       <role>
           <roleTerm>author</roleTerm>
       </role>
   </name>
   <typeOfResource>text</typeOfResource>
   <relatedItem type="host">
       <titleInfo>
           <title>The Journal Name</title>
       </titleInfo>
       <part>
           <detail type="volume">
               <number>0815</number>
           </detail>
           <date>2010</date>
       </part>
   </relatedItem>
   <identifier type="TheType">TheIdentifier</identifier>
   <location>
       <url>http://www.host.org/doc.pdf</url>
   </location>
</mods>


Journal article:
<mods>
   <titleInfo>
       <title>The article title</title>
   </titleInfo>
   <name type="personal">
       <namePart>The Author</namePart>
       <role>
           <roleTerm>author</roleTerm>
       </role>
   </name>
   <typeOfResource>text</typeOfResource>
   <relatedItem type="host">
       <titleInfo>
           <title>The Journal Name</title>
       </titleInfo>
       <name>
           <namePart>The Journal Volume Editor (if any)</namePart>
           <role>
               <roleTerm>editor</roleTerm>
           </role>
       </name>
       <part>
           <detail type="title">
               <title>The journal volume title (if any)</title>
           </detail>
           <detail type="volume">
               <number>0815</number>
           </detail>
           <extent unit="pages">
               <start>1024</start>
               <end>1040</end>
           </extent>
           <date>2010</date>
       </part>
   </relatedItem>
   <identifier type="TheType">TheIdentifier</identifier>
   <location>
       <url>http://www.host.org/doc.pdf</url>
   </location>
</mods>
		 */
	}
	
	/**
	 * Create a MODS data set from its XML representation.
	 * @param modsHeader the XML form of the MODS header
	 * @return a ModsDataSet containing the data from the specified XML document
	 */
	public static ModsDataSet getModsDataSet(QueriableAnnotation modsHeader) {
		Annotation[] modsIdentifiers = modsHeader.getAnnotations(MODS_IDENTIFIER);
		if (modsIdentifiers.length == 0)
			return null;
		
		ModsDataSet mds = new ModsDataSet(modsIdentifiers[0].getValue(), ((String) modsIdentifiers[0].getAttribute("type")));
		
		//	read basic data
		Annotation[] title = titlePath.evaluate(modsHeader, null);
		if (title.length != 0)
			mds.title = title[0].getValue();
		Annotation[] url = urlPath.evaluate(modsHeader, null);
		if (url.length != 0)
			mds.url = url[0].getValue();
		Annotation[] authors = authorsPath.evaluate(modsHeader, null);
		StringVector authorCollector = new StringVector();
		for (int a = 0; a < authors.length; a++)
			authorCollector.addElement(authors[a].getValue());
		mds.authors = authorCollector.toStringArray();
		
		//	get origin info to distinguish journals from books
		QueriableAnnotation[] originInfo = originInfoPath.evaluate(modsHeader, null);
		
		//	journal or article
		if (originInfo.length == 0) {
			mds.isJournal = true;
			
			QueriableAnnotation[] hostItem = hostItemPath.evaluate(modsHeader, null);
			if (hostItem.length == 0)
				return mds;
			
			Annotation[] journalName = hostItem_titlePath.evaluate(hostItem[0], null);
			if (journalName.length != 0)
				mds.journalName = journalName[0].getValue();
			
			Annotation[] volumeNumber = hostItem_volumeNumberPath.evaluate(hostItem[0], null);
			if (volumeNumber.length != 0) {
				String vn = null;
				if (Gamta.isNumber(volumeNumber[0].firstValue()))
					vn = volumeNumber[0].firstValue();
				else if (Gamta.isNumber(volumeNumber[0].lastValue()))
					vn = volumeNumber[0].lastValue();
				else for (int v = 0; v < volumeNumber[0].size(); v++) {
					if (Gamta.isNumber(volumeNumber[0].valueAt(v)))
						vn = volumeNumber[0].valueAt(v);
				}
				if (vn != null) try {
					mds.volumeNumber = Integer.parseInt(vn);
				} catch (NumberFormatException nfe) {}
			}
			
			Annotation[] date = hostItem_datePath.evaluate(hostItem[0], null);
			if (date.length != 0) try {
				mds.year = Integer.parseInt(date[0].getValue());
			} catch (NumberFormatException nfe) {}
			
			Annotation[] startPage = hostItem_startPagePath.evaluate(hostItem[0], null);
			Annotation[] endPage = hostItem_endPagePath.evaluate(hostItem[0], null);
			if ((startPage.length * endPage.length) != 0) {
				mds.isPart = true;
				
				try {
					mds.startPageNumber = Integer.parseInt(startPage[0].getValue());
				}
				catch (NumberFormatException nfe) {
					try {
						mds.startPageNumber = StringUtils.parseRomanNumber(startPage[0].getValue());
					} catch (NumberFormatException rnfe) {}
				}
				try {
					mds.endPageNumber = Integer.parseInt(endPage[0].getValue());
				}
				catch (NumberFormatException nfe) {
					try {
						mds.endPageNumber = StringUtils.parseRomanNumber(endPage[0].getValue());
					} catch (NumberFormatException rnfe) {}
				}
				
				Annotation[] volumeTitle = hostItem_volumeTitlePath.evaluate(hostItem[0], null);
				if (volumeTitle.length != 0) {
					mds.hostTitle = volumeTitle[0].getValue();
					
					Annotation[] editors = hostItem_editorsPath.evaluate(hostItem[0], null);
					StringVector editorCollector = new StringVector();
					for (int a = 0; a < editors.length; a++)
						editorCollector.addElement(editors[a].getValue());
					mds.hostEditors = editorCollector.toStringArray();
				}
			}
		}
		
		//	book or chapter
		else {
			mds.isBook = true;
			
			Annotation[] publisherName = originInfo_publisherNamePath.evaluate(originInfo[0], null);
			if (publisherName.length != 0)
				mds.bookPublisherName = publisherName[0].getValue();
			Annotation[] publisherLocation = originInfo_publisherLocationPath.evaluate(originInfo[0], null);
			if (publisherLocation.length != 0)
				mds.bookPublisherLocation = publisherLocation[0].getValue();
			Annotation[] date = originInfo_issueDatePath.evaluate(originInfo[0], null);
			if (date.length != 0) try {
				mds.year = Integer.parseInt(date[0].getValue());
			} catch (NumberFormatException nfe) {}
			
			QueriableAnnotation[] hostItem = hostItemPath.evaluate(modsHeader, null);
			if (hostItem.length != 0) {
				Annotation[] startPage = hostItem_startPagePath.evaluate(hostItem[0], null);
				Annotation[] endPage = hostItem_endPagePath.evaluate(hostItem[0], null);
				if ((startPage.length * endPage.length) != 0) {
					mds.isPart = true;
					
					try {
						mds.startPageNumber = Integer.parseInt(startPage[0].getValue());
					}
					catch (NumberFormatException nfe) {
						try {
							mds.startPageNumber = StringUtils.parseRomanNumber(startPage[0].getValue());
						} catch (NumberFormatException rnfe) {}
					}
					try {
						mds.endPageNumber = Integer.parseInt(endPage[0].getValue());
					}
					catch (NumberFormatException nfe) {
						try {
							mds.endPageNumber = StringUtils.parseRomanNumber(endPage[0].getValue());
						} catch (NumberFormatException rnfe) {}
					}
					
					Annotation[] bookTitle = hostItem_titlePath.evaluate(hostItem[0], null);
					if (bookTitle.length != 0)
						mds.hostTitle = bookTitle[0].getValue();
					
					Annotation[] editors = hostItem_editorsPath.evaluate(hostItem[0], null);
					StringVector editorCollector = new StringVector();
					for (int a = 0; a < editors.length; a++)
						editorCollector.addElement(editors[a].getValue());
					mds.hostEditors = editorCollector.toStringArray();
				}
			}
		}
		
		return mds;
	}
	
	private static final GPath titlePath = new GPath("//mods:titleInfo/mods:title");
	
	private static final GPath urlPath = new GPath("//mods:location/mods:url");
	
	private static final GPath authorsPath = new GPath("//mods:name[.//mods:roleTerm = 'Author']/mods:namePart");
	
	private static final GPath hostItemPath = new GPath("//mods:relatedItem[./@type = 'host']");
	private static final GPath hostItem_titlePath = new GPath("//mods:titleInfo/mods:title");
	private static final GPath hostItem_volumeNumberPath = new GPath("//mods:part/mods:detail[./@type = 'volume']/mods:number");
	private static final GPath hostItem_volumeTitlePath = new GPath("//mods:part/mods:detail[./@type = 'title']/mods:title");
	private static final GPath hostItem_startPagePath = new GPath("//mods:part/mods:extent[./@unit = 'page']/mods:start");
	private static final GPath hostItem_endPagePath = new GPath("//mods:part/mods:extent[./@unit = 'page']/mods:end");
	private static final GPath hostItem_datePath = new GPath("//mods:part/mods:date");
	private static final GPath hostItem_editorsPath = new GPath("//mods:name[.//mods:roleTerm = 'Editor']/mods:namePart");
	
	private static final GPath originInfoPath = new GPath("//mods:originInfo");
	private static final GPath originInfo_publisherNamePath = new GPath("//mods:publisher");
	private static final GPath originInfo_publisherLocationPath = new GPath("//mods:place/mods:placeTerm");
	private static final GPath originInfo_issueDatePath = new GPath("//mods:dateIssued");
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType) {
		return createModsHeader(modsId, modsIdType, null, null, null);
	}
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, String urlPattern) {
		return createModsHeader(modsId, modsIdType, null, null, urlPattern);
	}
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param document the document the MODS header is for, for extracting data
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, QueriableAnnotation document) {
		return createModsHeader(modsId, modsIdType, document, document, null);
	}
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param document the document the MODS header is for, for extracting data
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, QueriableAnnotation document, String urlPattern) {
		return createModsHeader(modsId, modsIdType, document, document, urlPattern);
	}

	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, Attributed modsData) {
		return createModsHeader(modsId, modsIdType, modsData, null, null);
	}
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, Attributed modsData, String urlPattern) {
		return createModsHeader(modsId, modsIdType, modsData, null, urlPattern);
	}
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @param document the document the MODS header is for, for extracting data
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, Attributed modsData, QueriableAnnotation document) {
		return createModsHeader(modsId, modsIdType, modsData, document, null);
	}
	
	/**
	 * Create a MODS header, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @param document the document the MODS header is for, for extracting data
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS header
	 */
	public static MutableAnnotation createModsHeader(String modsId, String modsIdType, Attributed modsData, QueriableAnnotation document, String urlPattern) {
		ModsDataSet mds = createModsData(modsId, modsIdType, modsData, document, urlPattern);
		return ((mds == null) ? null : mds.getModsHeader());
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType) {
		return createModsData(modsId, modsIdType, null, null, null);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, String urlPattern) {
		return createModsData(modsId, modsIdType, null, null, urlPattern);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param document the document the MODS header is for, for extracting data
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, QueriableAnnotation document) {
		return createModsData(modsId, modsIdType, document, document, null);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param document the document the MODS header is for, for extracting data
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, QueriableAnnotation document, String urlPattern) {
		return createModsData(modsId, modsIdType, document, document, urlPattern);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, Attributed modsData) {
		return createModsData(modsId, modsIdType, modsData, null, null);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, Attributed modsData, String urlPattern) {
		return createModsData(modsId, modsIdType, modsData, null, urlPattern);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @param document the document the MODS header is for, for extracting data
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, Attributed modsData, QueriableAnnotation document) {
		return createModsData(modsId, modsIdType, modsData, document, null);
	}
	
	/**
	 * Create a MODS data set, i.e., fill in data for a given ID. If the input
	 * dialog is canceled, this method returns null.
	 * @param modsId the identifier string to use
	 * @param modsIdType the type of the specified identifier, e.g. its origin
	 * @param modsData an Attributed object holding attributes to use in the
	 *            newly created MODS header; attribute names are the same as set
	 *            by the setModsAttributes() method
	 * @param document the document the MODS header is for, for extracting data
	 * @param urlPatter a pattern to generate the URL from the ID
	 * @return the newly created MODS data set
	 */
	public static ModsDataSet createModsData(String modsId, String modsIdType, Attributed modsData, QueriableAnnotation document, String urlPattern) {
		if (modsId == null)
			return null;
		
		ModsDataSet mds = new ModsDataSet(modsId, modsIdType);
		
		if (!"#".equals(modsId) && (urlPattern != null))
			mds.url = generateUrl(modsId, urlPattern);
		if (document != null)
			fillInData(mds, document);
		if (modsData != null)
			fillInAttributes(mds, modsData);
		
		return editModsData(mds, null);
	}
	
	private static void fillInData(ModsDataSet mds, QueriableAnnotation document) {
		
		QueriableAnnotation[] paragraphs = document.getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	find page numbers
		int fpn = Integer.MIN_VALUE;
		int lpn = Integer.MIN_VALUE;
		for (int p = 0; p < paragraphs.length; p++)
			if (paragraphs[p].hasAttribute(PAGE_NUMBER_ATTRIBUTE)) try {
				int pn = Integer.parseInt((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
				fpn = ((fpn == Integer.MIN_VALUE) ? pn : Math.min(pn, fpn));
				lpn = Math.max(pn, lpn);
			} catch (NumberFormatException nfe) {}
			
		//	check success
		if (fpn == Integer.MIN_VALUE)
			return;
		
		//	store data
		mds.startPageNumber = fpn;
		mds.endPageNumber = lpn;
		
		//	find other data
		String title = null;
		String titleCandidate = null;
		String author = null;
		int year = Integer.MIN_VALUE;
		for (int p = 0; p < paragraphs.length; p++) {
			if (paragraphs[p].hasAttribute(PAGE_NUMBER_ATTRIBUTE)) try {
				int pn = Integer.parseInt((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
				if (pn != fpn)
					break;
			} catch (NumberFormatException nfe) {}
			
			//	possible author
			if ("by".equalsIgnoreCase(paragraphs[p].firstValue()) && (author == null)) {
				author = TokenSequenceUtils.concatTokens(paragraphs[p], 1, (paragraphs[p].size() - 1), true, true);
				if ((title == null) && (titleCandidate != null))
					title = titleCandidate;
			}
			
			//	possible title
			else if ((title == null) && Gamta.isFirstLetterUpWord(paragraphs[p].firstValue())) {
				if  (Gamta.isTitleCase(paragraphs[p]))
					title = TokenSequenceUtils.concatTokens(paragraphs[p], true, true);
				else titleCandidate = TokenSequenceUtils.concatTokens(paragraphs[p], true, true);
			}
			
			//	search for year
			Annotation[] years = Gamta.extractAllMatches(paragraphs[p], "[12][0-9]{3}", 1);
			for (int y = 0; y < years.length; y++) try {
				year = Math.max(year, Integer.parseInt(years[y].getValue()));
			} catch (NumberFormatException nfe) {}
		}
		
		//	store data
		if (author != null) {
			String[] authors = {author};
			mds.authors = authors;
		}
		if (title != null)
			mds.title = title;
		if (year != Integer.MIN_VALUE)
			mds.year = year;
		
		//	interprete data
		if (mds.startPageNumber > 1)
			mds.isPart = true;
		
//		DON'T DO THIS, DATA NOT COMPLETE
//		if ((mds.endPageNumber - mds.startPageNumber) > 100)
//			mds.isBook = true;
//		else if (mds.endPageNumber > 250)
//			mds.isBook = true;
//		else if (mds.isPart)
//			mds.isJournal = true;
	}
	
	private static void fillInAttributes(ModsDataSet mds, Attributed data) {
		String authorString = ((String) data.getAttribute(MODS_AUTHOR_ATTRIBUTE));
		if (authorString == null)
			authorString = ((String) data.getAttribute(DOCUMENT_AUTHOR_ATTRIBUTE));
		if (authorString != null) {
			StringVector authors = new StringVector();
			if (authorString.indexOf('&') == -1)
				authors.addElement(authorString);
			else if (authorString.indexOf('&') < authorString.lastIndexOf('&'))
				authors.parseAndAddElements(authorString, "&");
			else {
				String last = authorString.substring(authorString.indexOf('&') + 1).trim();
				authorString = authorString.substring(0, authorString.lastIndexOf('&')).trim();
				if (last.indexOf(',') == -1) {
					authors.parseAndAddElements(authorString, ",");
					authors.addElement(last);
				}
				else if (last.endsWith(".")) {
					authors.parseAndAddElements(authorString, ".,");
					for (int a = 0; a < (authors.size() - 1); a++)
						authors.set(a, (authors.get(a) + "."));
					authors.addElement(last);
				}
				else {
					int split = authorString.indexOf(',', (authorString.indexOf(',') + 1));
					while (split != -1) {
						authors.addElement(authorString.substring(0, split));
						authorString = authorString.substring(split+1);
						split = authorString.indexOf(',', (authorString.indexOf(',') + 1));
					}
					if (authorString.length() != 0)
						authors.addElement(authorString);
					authors.addElement(last);
				}
			}
			mds.authors = authors.toStringArray();
			for (int a = 0; a < mds.authors.length; a++)
				mds.authors[a] = mds.authors[a].trim();
		}
		
		String title = ((String) data.getAttribute(MODS_TITLE_ATTRIBUTE));
		if (title == null)
			title = ((String) data.getAttribute(DOCUMENT_TITLE_ATTRIBUTE));
		if (title != null)
			mds.title = title;
		
		String year = ((String) data.getAttribute(MODS_DATE_ATTRIBUTE));
		if (year == null)
			year = ((String) data.getAttribute(DOCUMENT_DATE_ATTRIBUTE));
		if (year != null) try {
			mds.year = Integer.parseInt(year);
		} catch (NumberFormatException nfe) {}
		
		String url = ((String) data.getAttribute(MODS_ORIGIN_ATTRIBUTE));
		if (url == null)
			url = ((String) data.getAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE));
		if (url != null)
			mds.url = url;
	}
	
	/**
	 * Edit a MODS header, i.e., fill in missing data. If the edit dialog is
	 * canceled, this method returns null.
	 * @param modsHeader the MODS header to edit
	 * @param notIfValid edit the MODS header even if the data is complete? If
	 *            set to false and the argument MODS header is complete, this
	 *            method simply returns it.
	 * @return the edited MODS header
	 */
	public static MutableAnnotation editModsHeader(MutableAnnotation modsHeader, boolean notIfValid) {
		return editModsHeader(modsHeader, notIfValid, null);
	}
	
	/**
	 * Edit a MODS header, i.e., fill in missing data. If the edit dialog is
	 * canceled, this method returns null.
	 * @param modsHeader the MODS header to edit
	 * @param notIfValid edit the MODS header even if the data is complete? If
	 *            set to false and the argument MODS header is complete, this
	 *            method simply returns it.
	 * @param urlPatter a pattern to generate the URL from the ID if not given
	 * @return the edited MODS header
	 */
	public static MutableAnnotation editModsHeader(MutableAnnotation modsHeader, boolean notIfValid, String urlPattern) {
		ModsDataSet mds = getModsDataSet(modsHeader);
		mds = editModsData(mds, notIfValid, urlPattern);
		return ((mds == null) ? null : mds.getModsHeader());
	}
	
	/**
	 * Edit a MODS header, i.e., fill in missing data. If the edit dialog is
	 * canceled, this method returns null.
	 * @param mds the MODS data set to edit
	 * @param notIfValid edit the MODS header even if the data is complete? If
	 *            set to false and the argument MODS header is complete, this
	 *            method simply returns it.
	 * @param urlPatter a pattern to generate the URL from the ID if not given
	 * @return the edited MODS header
	 */
	public static ModsDataSet editModsData(ModsDataSet mds, boolean notIfValid, String urlPattern) {
		if (notIfValid && mds.isValid())
			return mds;
		
		if ((mds.url.length() == 0) && (urlPattern != null))
			mds.url = generateUrl(mds.id, urlPattern);
		
		return editModsData(mds, null);
	}
	
	private static final String generateUrl(String modsId, String urlPattern) {
		return urlPattern.replaceAll("\\@ModsID", modsId);
	}
	
	private static ModsDataSet editModsData(ModsDataSet mds, final QueriableAnnotation modsHeader) {
		final JDialog d = DialogFactory.produceDialog("Edit MODS Meta Data Header", true);
		
		final ModsDataPanel mdp = new ModsDataPanel(mds);
		JButton viewXml;
		if (modsHeader == null)
			viewXml = null;
		else {
			viewXml = new JButton("View XML");
			viewXml.addActionListener(new ActionListener() {
				private JDialog xmlDisplay = null;
				public void actionPerformed(ActionEvent ae) {
					if (this.xmlDisplay == null) {
						this.xmlDisplay = new JDialog(d, "Original XML", false);
						d.addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent we) {
								xmlDisplay.dispose();
							}
						});
						
						StringWriter xmlWriter = new StringWriter();
						try {
							AnnotationUtils.writeXML(modsHeader, xmlWriter);
						}
						catch (IOException ioe) {
							// never gonna happen, but Java don't know ...
						}
						
						JTextArea xmlArea = new JTextArea();
						xmlArea.setLineWrap(false);
						xmlArea.setWrapStyleWord(false);
						xmlArea.setText(xmlWriter.toString());
						JScrollPane xmlAreaBox = new JScrollPane(xmlArea);
						
						this.xmlDisplay.getContentPane().setLayout(new BorderLayout());
						this.xmlDisplay.getContentPane().add(xmlAreaBox, BorderLayout.CENTER);
						
						this.xmlDisplay.setSize(500, 500);
						this.xmlDisplay.setLocationRelativeTo(d);
					}
					this.xmlDisplay.setVisible(true);
				}
			});
		}
		JButton validate = new JButton("Validate");
		validate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (mdp.isDataValid())
					JOptionPane.showMessageDialog(mdp, "The MODS meta data is valid.", "Validation Report", JOptionPane.INFORMATION_MESSAGE);
				else displayErrors(mdp.getValidationReport(), mdp);
			}
		});
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (mdp.isDataValid())
					d.dispose();
				else displayErrors(mdp.getValidationReport(), mdp);
			}
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				d.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
		if (viewXml != null)
			buttonPanel.add(viewXml);
		buttonPanel.add(validate);
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		
		d.getContentPane().setLayout(new BorderLayout());
		d.getContentPane().add(mdp, BorderLayout.CENTER);
		d.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		d.setSize(600, 370);
		d.setLocationRelativeTo(DialogFactory.getTopWindow());
		d.setVisible(true);
		
		return mdp.getDataSet();
	}
	
	
	private static final void displayErrors(String[] errors, JPanel parent) {
		StringVector errorMessageBuilder = new StringVector();
		errorMessageBuilder.addContent(errors);
		JOptionPane.showMessageDialog(parent, ("The MODS meta data is not valid. In particular, there are the following errors:\n" + errorMessageBuilder.concatStrings("\n")), "Validation Report", JOptionPane.ERROR_MESSAGE);
	}
	
	private static class ModsDataPanel extends JPanel {
		
		//	TODO add publication type selector
		
		static final int labelOrientation = JLabel.LEFT;
		
		String id;
		
		JTextField idField = new JTextField();
		JTextField idTypeField = new JTextField();
		JTextField titleField = new JTextField();
		JTextField authorsField = new JTextField();
		JTextField yearField = new JTextField();
		JTextField urlField = new JTextField();
		
		JCheckBox isPart = new JCheckBox("Part of Host Publication");
		JTextField startPageNumberField = new JTextField();
		JTextField endPageNumberField = new JTextField();
		
		JCheckBox hostIsTitled = new JCheckBox("Has Title?");
		JLabel hostTitleLabel = new JLabel("Host Title", JLabel.RIGHT);
		JTextField hostTitleField = new JTextField();
		JTextField hostEditorsField = new JTextField();
		
		JRadioButton isJournal = new JRadioButton("Journal (or part of one)");
		JTextField journalNameField = new JTextField();
		JTextField volumeNumberField = new JTextField();
		
		JRadioButton isBook = new JRadioButton("Book (or part of one)");
		JTextField bookPublisherNameField = new JTextField();
		JTextField bookPublisherLocationField = new JTextField();
		
		ModsDataPanel(ModsDataSet data) {
			super(new GridBagLayout(), true);
			
			this.id = data.id;
			
			this.idField.setFont(this.idField.getFont().deriveFont(Font.BOLD));
			this.idField.setEnabled(false);
			this.idField.setText(data.id);
			this.idTypeField.setText(data.idType);
			if (data.idType != null) {
				this.idTypeField.setText(data.idType);
				this.idTypeField.setEnabled(false);
			}
			
			if (data.title.length() != 0)
				this.titleField.setText(data.title);
			if (data.authors.length != 0) {
				StringBuffer authors = new StringBuffer();
				for (int a = 0; a < data.authors.length; a++) {
					if (a != 0)
						authors.append(" & ");
					authors.append(data.authors[a]);
				}
				this.authorsField.setText(authors.toString());
			}
			if (data.year != Integer.MIN_VALUE)
				this.yearField.setText("" + data.year);
			if (data.url.length() != 0)
				this.urlField.setText(data.url);
			
			
			this.isPart.setHorizontalAlignment(labelOrientation);
			this.isPart.setHorizontalTextPosition((labelOrientation == JLabel.RIGHT) ? JLabel.LEFT : JLabel.RIGHT);
			this.isPart.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					startPageNumberField.setEnabled(isPart.isSelected());
					endPageNumberField.setEnabled(isPart.isSelected());
					hostIsTitled.setEnabled(isPart.isSelected() && !isBook.isSelected());
					hostTitleField.setEnabled(isPart.isSelected() && hostIsTitled.isSelected());
					hostEditorsField.setEnabled(isPart.isSelected() && hostIsTitled.isSelected());
				}
			});
			
			
			this.hostIsTitled.setHorizontalAlignment(labelOrientation);
			this.hostIsTitled.setHorizontalTextPosition((labelOrientation == JLabel.RIGHT) ? JLabel.LEFT : JLabel.RIGHT);
			this.hostIsTitled.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					hostTitleField.setEnabled(isPart.isSelected() && hostIsTitled.isSelected());
					hostEditorsField.setEnabled(isPart.isSelected() && hostIsTitled.isSelected());
				}
			});
			
			
			ButtonGroup job = new ButtonGroup();
			this.isJournal.setHorizontalAlignment(labelOrientation);
			this.isJournal.setHorizontalTextPosition((labelOrientation == JLabel.RIGHT) ? JLabel.LEFT : JLabel.RIGHT);
			this.isJournal.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (isJournal.isSelected()) {
						isPart.setSelected(true);
						journalNameField.setEnabled(true);
						volumeNumberField.setEnabled(true);
						hostTitleLabel.setText("Volume Title");
						hostIsTitled.setSelected(hostTitleField.getText().length() != 0);
						hostIsTitled.setEnabled(true);
						bookPublisherNameField.setEnabled(false);
						bookPublisherLocationField.setEnabled(false);
					}
				}
			});
			job.add(this.isJournal);
			
			this.isBook.setHorizontalAlignment(labelOrientation);
			this.isBook.setHorizontalTextPosition((labelOrientation == JLabel.RIGHT) ? JLabel.LEFT : JLabel.RIGHT);
			this.isBook.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (isBook.isSelected()) {
						journalNameField.setEnabled(false);
						volumeNumberField.setEnabled(false);
						hostTitleLabel.setText("Book Title");
						hostIsTitled.setSelected(true);
						hostIsTitled.setEnabled(false);
						bookPublisherNameField.setEnabled(true);
						bookPublisherLocationField.setEnabled(true);
					}
				}
			});
			job.add(this.isBook);
			
			this.journalNameField.setEnabled(false);
			this.volumeNumberField.setEnabled(false);
			this.startPageNumberField.setEnabled(false);
			this.endPageNumberField.setEnabled(false);
			this.hostIsTitled.setEnabled(false);
			this.hostTitleField.setEnabled(false);
			this.hostEditorsField.setEnabled(false);
			this.bookPublisherNameField.setEnabled(false);
			this.bookPublisherLocationField.setEnabled(false);
			
			if (data.isPart) {
				if (data.startPageNumber != Integer.MIN_VALUE)
					this.startPageNumberField.setText("" + data.startPageNumber);
				if (data.endPageNumber != Integer.MIN_VALUE)
					this.endPageNumberField.setText("" + data.endPageNumber);
				if (data.hostTitle.length() != 0) {
					this.hostIsTitled.setSelected(true);
					this.hostTitleField.setText(data.hostTitle);
					if (data.hostEditors.length != 0) {
						StringBuffer hostEditors = new StringBuffer();
						for (int a = 0; a < data.hostEditors.length; a++) {
							if (a != 0)
								hostEditors.append(" & ");
							hostEditors.append(data.hostEditors[a]);
						}
						this.hostEditorsField.setText(hostEditors.toString());
					}
				}
				this.isPart.setSelected(true);
			}
			
			if (data.isBook) {
				this.isBook.setSelected(true);
				this.bookPublisherNameField.setText(data.bookPublisherName);
				this.bookPublisherLocationField.setText(data.bookPublisherLocation);
			}
			else if (data.isJournal) {
				this.isJournal.setSelected(true);
				this.journalNameField.setText(data.journalName);
				this.volumeNumberField.setText("" + data.volumeNumber);
				if (data.startPageNumber != Integer.MIN_VALUE)
					this.startPageNumberField.setText("" + data.startPageNumber);
				if (data.endPageNumber != Integer.MIN_VALUE)
					this.endPageNumberField.setText("" + data.endPageNumber);
			}
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			
			
			JPanel hostFieldPanel = new JPanel(new GridBagLayout(), true);
			hostFieldPanel.setBorder(BorderFactory.createEmptyBorder());
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			hostFieldPanel.add(this.hostIsTitled, gbc.clone());
			gbc.gridx = 1;
			hostFieldPanel.add(this.hostTitleLabel, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 1;
			gbc.gridwidth = 4;
			hostFieldPanel.add(this.hostTitleField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 2;
			hostFieldPanel.add(new JLabel("Host Editor(s) (separate with '&')", labelOrientation), gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 1;
			gbc.gridwidth = 4;
			hostFieldPanel.add(this.hostEditorsField, gbc.clone());
			gbc.gridy++;
			
			
			JPanel partFieldPanel = new JPanel(new GridBagLayout(), true);
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			partFieldPanel.add(new JLabel("Start Page", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			partFieldPanel.add(this.startPageNumberField, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			partFieldPanel.add(new JLabel("   End Page", labelOrientation), gbc.clone());
			gbc.gridx = 3;
			gbc.weightx = 1;
			partFieldPanel.add(this.endPageNumberField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.gridwidth = 4;
			partFieldPanel.add(hostFieldPanel, gbc.clone());
			gbc.gridy++;
			
			
			JPanel journalFieldPanel = new JPanel(new GridBagLayout(), true);
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			journalFieldPanel.add(new JLabel("Journal Name", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 3;
			gbc.gridwidth = 3;
			journalFieldPanel.add(this.journalNameField, gbc.clone());
			gbc.gridx = 4;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			journalFieldPanel.add(new JLabel("   Volume Number", labelOrientation), gbc.clone());
			gbc.gridx = 5;
			gbc.weightx = 1;
			journalFieldPanel.add(this.volumeNumberField, gbc.clone());
			
			
			JPanel bookFieldPanel = new JPanel(new GridBagLayout(), true);
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			bookFieldPanel.add(new JLabel("Publisher Name", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			bookFieldPanel.add(this.bookPublisherNameField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			bookFieldPanel.add(new JLabel("Publisher Location", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			bookFieldPanel.add(this.bookPublisherLocationField, gbc.clone());
			gbc.gridy++;
			
			
			gbc.gridy = 0;
			gbc.gridwidth = 1;
			gbc.gridx = 0;
			gbc.weightx = 0;
			this.add(new JLabel("Identifier", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			this.add(this.idField, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			this.add(new JLabel("   Identifier Type", labelOrientation), gbc.clone());
			gbc.gridx = 3;
			gbc.weightx = 1;
			this.add(this.idTypeField, gbc.clone());
			gbc.gridx = 4;
			gbc.weightx = 0;
			this.add(new JLabel("   Year of Publication", labelOrientation), gbc.clone());
			gbc.gridx = 5;
			gbc.weightx = 1;
			this.add(this.yearField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Title", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(this.titleField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Author(s) (separate with '&')", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(this.authorsField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("URL of PDF Version", labelOrientation), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(this.urlField, gbc.clone());
			gbc.gridy++;
			
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.isPart, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(partFieldPanel, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.isJournal, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(journalFieldPanel, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.isBook, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(bookFieldPanel, gbc.clone());
			gbc.gridy++;
		}
		
		boolean isDataValid() {
			return (this.getValidationReport().length == 0);
		}
		
		private int getNumber(String number) {
			try {
				return Integer.parseInt(number.trim());
			} catch (Exception e) {
				return Integer.MIN_VALUE;
			}
		}
		
		private String[] getAuthors() {
			String authors = this.authorsField.getText().trim();
			if (authors.length() == 0)
				return new String[0];
			else {
				StringVector authorParser = new StringVector();
				authorParser.parseAndAddElements(authors, "&");
				for (int a = 0; a < authorParser.size(); a++)
					authorParser.setElementAt(authorParser.get(a).trim(), a);
				return authorParser.toStringArray();
			}
		}
		
		private String[] getEditors() {
			String editors = this.hostEditorsField.getText().trim();
			if (editors.length() == 0)
				return new String[0];
			else {
				StringVector editorParser = new StringVector();
				editorParser.parseAndAddElements(editors, "&");
				for (int e = 0; e < editorParser.size(); e++)
					editorParser.setElementAt(editorParser.get(e).trim(), e);
				return editorParser.toStringArray();
			}
		}
		
		String[] getValidationReport() {
			return getErrorReport(this.buildDataSet());
		}
		
		ModsDataSet getDataSet() {
			ModsDataSet mds = this.buildDataSet();
			String[] errors = getErrorReport(mds);
			return ((errors.length == 0) ? mds : null);
		}
		
		private ModsDataSet buildDataSet() {
//			ModsDataSet mds = new ModsDataSet(this.id, this.idTypeField.getText().trim());
			ModsDataSet mds = new ModsDataSet(("#".equals(this.id) ? this.hashId() : this.id), this.idTypeField.getText().trim());
			
			mds.title = this.titleField.getText().trim();
			mds.authors = this.getAuthors();
			mds.year = this.getNumber(this.yearField.getText());
			mds.url = this.urlField.getText().trim();
			
			if (this.isPart.isSelected()) {
				mds.isPart = true;
				mds.startPageNumber = this.getNumber(this.startPageNumberField.getText());
				mds.endPageNumber = this.getNumber(this.endPageNumberField.getText());
			}
			
			if (this.isJournal.isSelected()) {
				mds.isJournal = true;
				mds.journalName = this.journalNameField.getText().trim();
				mds.volumeNumber = this.getNumber(this.volumeNumberField.getText());
				
				if (this.isPart.isSelected()) {
					String hostTitle = this.hostTitleField.getText().trim();
					if (hostTitle.length() != 0) {
						mds.hostTitle = hostTitle;
						mds.hostEditors = this.getEditors();
					}
				}
			}
			
			else if (this.isBook.isSelected()) {
				mds.isBook = true;
				mds.bookPublisherName = this.bookPublisherNameField.getText().trim();
				mds.bookPublisherLocation = this.bookPublisherLocationField.getText().trim();
				
				if (this.isPart.isSelected()) {
					mds.hostTitle = this.hostTitleField.getText().trim();
					mds.hostEditors = this.getEditors();
				}
			}
			
			return mds;
		}
		
		private String hashId() {
			
			String title = this.titleField.getText().trim();
			String authors = this.authorsField.getText().trim();
			int year = this.getNumber(this.yearField.getText());
			
			String pagination = null;
			if (this.isPart.isSelected())
				pagination = (this.getNumber(this.startPageNumberField.getText()) + "-" + this.getNumber(this.endPageNumberField.getText()));
			
			String hostString = null;
			if (this.isJournal.isSelected()) {
				hostString = (this.journalNameField.getText().trim() + " " + this.getNumber(this.volumeNumberField.getText()));
				if (this.isPart.isSelected()) {
					String hostTitle = this.hostTitleField.getText().trim();
					if (hostTitle.length() != 0)
						hostString = (hostString + " (" + hostTitle + "): " + pagination);
				}
			}
			
			else if (this.isBook.isSelected()) {
				hostString = (this.bookPublisherNameField.getText().trim() + ", " + this.bookPublisherLocationField.getText().trim());
				if (this.isPart.isSelected()) {
					String hostTitle = this.hostTitleField.getText().trim();
					if (hostTitle.length() != 0)
						hostString = (hostString + " (" + hostTitle + "): " + pagination);
				}
			}
			
			String authorYearHash = ("" + (authors + " (" + year + ")").hashCode());
			String titleHash = ("" + title.hashCode());
			String hostHash = ((hostString == null) ? null : ("" + hostString.hashCode()));
			int partLength = ((hostHash == null) ? 6 : 4); // TODO figure out if 6 and 4 are good cuts
			String hashId = trimHashPart(authorYearHash, partLength) + "-" + trimHashPart(titleHash, partLength) + ((hostHash == null) ? "" : ("-" + trimHashPart(hostHash, partLength)));
			
			this.idField.setText(hashId);
			this.updateUI();
			return hashId;
		}
		private String trimHashPart(String hash, int partLength) {
			while (hash.length() < partLength)
				hash = ("0" + hash);
			return hash.substring(hash.length() - partLength);
		}
	}
	
	/**
	 * Check if the data in a given MODS header is complete
	 * @param modsHeader the MODS header to check
	 * @return true if all required data is given, false otherwise
	 */
	public static boolean checkModsHeader(QueriableAnnotation modsHeader) {
		return getModsDataSet(modsHeader).isValid();
	}
	
	/**
	 * Produce a report regarding which errors are in the data in a given MODS
	 * header. If the data is complete, this method returns an empty array, but
	 * never null.
	 * @param modsHeader the MODS header to check
	 * @return an array of error reports
	 */
	public static String[] getErrorReport(QueriableAnnotation modsHeader) {
		return getErrorReport(getModsDataSet(modsHeader));
	}
	
	/**
	 * Produce a report regarding which errors are in the data in a given MODS
	 * header. If the data is complete, this method returns an empty array, but
	 * never null.
	 * @param mds the MODS data set to check
	 * @return an array of error reports
	 */
	public static final String[] getErrorReport(ModsDataSet mds) {
		StringVector errors = new StringVector();
		
		if (mds.title.length() == 0)
			errors.addElement(" - the title is missing");
		if (mds.authors.length == 0)
			errors.addElement(" - the author(s) are missing");
		if (mds.year < 1)
			errors.addElement(" - the year is invalid or missing, or not a number");
		
		if (mds.url.length() == 0)
			errors.addElement(" - the PDF URL is missing");
		else try {
			new URL(mds.url);
		}
		catch (Exception e) {
			errors.addElement(" - the PDF URL is not a valid URL");
		}
		
		if (mds.isPart) {
			if (mds.startPageNumber < 0)
				errors.addElement(" - the start page number is invalid or missing, or not a valid page number");
			if (mds.endPageNumber < 0)
				errors.addElement(" - the end page number is invalid or missing, or not a valid page number");
			if (mds.endPageNumber < mds.startPageNumber)
				errors.addElement(" - the end page number less than the start page number");
		}
		
		if (mds.isJournal) {
			if (mds.journalName.length() == 0)
				errors.addElement(" - the journal name is missing");
			if (mds.volumeNumber < 0)
				errors.addElement(" - the volume number is invalid or missing, or not a valid volume number");
		}
		else if (mds.isBook) {
			if (mds.isPart && (mds.hostTitle.length() == 0))
				errors.addElement(" - the book title is missing");
			if (mds.bookPublisherName.length() == 0)
				errors.addElement(" - the publisher name is missing");
			if (mds.bookPublisherLocation.length() == 0)
				errors.addElement(" - the publisher location is missing");
		}
		else errors.addElement(" - no publication type is selected");
		
		return errors.toStringArray();
	}
	
	/**
	 * Read meta data from a given MODS header and add them as attributes to a
	 * given document. If the argument document is a DocumentRoot instance, the
	 * meta data is additionally deposited in document properties.
	 * @param doc the documents to add the attributes to
	 * @param mods the MODS header to take the attributes from
	 */
	public static void setModsAttributes(MutableAnnotation doc, MutableAnnotation mods) {
		setModsAttributes(doc, getModsDataSet(mods));
	}
	
	/**
	 * Read meta data from a given MODS header and add them as attributes to a
	 * given document. If the argument document is a DocumentRoot instance, the
	 * meta data is additionally deposited in document properties.
	 * @param doc the documents to add the attributes to
	 * @param mds the MODS data set to take the attributes from
	 */
	public static void setModsAttributes(MutableAnnotation doc, ModsDataSet mds) {
		doc.setAttribute(MODS_ID_ATTRIBUTE, mds.id);
		if (doc instanceof DocumentRoot)
			((DocumentRoot) doc).setDocumentProperty(MODS_ID_ATTRIBUTE, mds.id);
		
		StringBuffer authors = new StringBuffer();
		for (int a = 0; a < mds.authors.length; a++) {
			if (a != 0) {
				if ((a + 1) == mds.authors.length)
					authors.append(" & ");
				else authors.append(", ");
			}
			authors.append(mds.authors[a]);
		}
		if (authors.length() != 0) {
			doc.setAttribute(MODS_AUTHOR_ATTRIBUTE, authors.toString());
			doc.setAttribute(DOCUMENT_AUTHOR_ATTRIBUTE, authors.toString());
			if (doc instanceof DocumentRoot)
				((DocumentRoot) doc).setDocumentProperty(MODS_AUTHOR_ATTRIBUTE, authors.toString());
		}
		
		if (mds.title.length() != 0) {
			doc.setAttribute(MODS_TITLE_ATTRIBUTE, mds.title);
			doc.setAttribute(DOCUMENT_TITLE_ATTRIBUTE, mds.title);
			if (doc instanceof DocumentRoot)
				((DocumentRoot) doc).setDocumentProperty(MODS_TITLE_ATTRIBUTE, mds.title);
		}
		
		if (mds.year > 1) {
			doc.setAttribute(MODS_DATE_ATTRIBUTE, ("" + mds.year));
			doc.setAttribute(DOCUMENT_DATE_ATTRIBUTE, ("" + mds.year));
			if (doc instanceof DocumentRoot)
				((DocumentRoot) doc).setDocumentProperty(MODS_DATE_ATTRIBUTE, ("" + mds.year));
		}
		
		if (mds.url.length() != 0) {
			doc.setAttribute(MODS_ORIGIN_ATTRIBUTE, mds.url);
			doc.setAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE, mds.url);
			if (doc instanceof DocumentRoot)
				((DocumentRoot) doc).setDocumentProperty(MODS_ORIGIN_ATTRIBUTE, mds.url);
		}
		
		if (mds.isPart) {
			StringBuffer editors = new StringBuffer();
			for (int e = 0; e < mds.hostEditors.length; e++) {
				if (e != 0) {
					if ((e + 1) == mds.hostEditors.length)
						editors.append(" & ");
					else editors.append(", ");
				}
				editors.append(mds.hostEditors[e]);
			}
			if (mds.isJournal)
				doc.setAttribute(DOCUMENT_ORIGIN_ATTRIBUTE, (((mds.hostTitle.length() == 0) ? "" : (((editors.length() == 0) ? "" : (editors.toString() + " (Editors). ")) + mds.hostTitle + ". ")) + mds.journalName + " (" + mds.volumeNumber + ")"));
			else if (mds.isBook)
				doc.setAttribute(DOCUMENT_ORIGIN_ATTRIBUTE, (((editors.length() == 0) ? "" : (editors.toString() + " (Editors). ")) + mds.hostTitle + ". " + mds.bookPublisherName + ", " + mds.bookPublisherLocation));
			
			doc.setAttribute(PAGE_NUMBER_ATTRIBUTE, ("" + mds.startPageNumber));
			doc.setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, ("" + mds.endPageNumber));
		}
	}
//	
//	private static int parseRomanNumber(String rawNumberString) {
//		
//		//	normalize raw string
//		String numberString = rawNumberString.toLowerCase().trim();
//		
//		//	parse number
//		int number = 0;
//		for (int n = 0; n < numberString.length(); n++) {
//			if (numberString.startsWith("m", n)) number += 1000;
//			else if (numberString.startsWith("d", n)) number+= 500;
//			else if (numberString.startsWith("cd", n)) {
//				number+= 400;
//				n++;
//			}
//			else if (numberString.startsWith("c", n)) number+= 100;
//			else if (numberString.startsWith("l", n)) number+= 50;
//			else if (numberString.startsWith("xl", n)) {
//				number+= 40;
//				n++;
//			}
//			else if (numberString.startsWith("x", n)) number+= 10;
//			else if (numberString.startsWith("v", n)) number+= 5;
//			else if (numberString.startsWith("iv", n)) {
//				number+= 4;
//				n++;
//			}
//			else if (numberString.startsWith("i", n)) number+= 1;
//			else throw new NumberFormatException();
//		}
//		
//		//	no value could be parsed, throw exception
//		if (number == 0) throw new NumberFormatException();
//		
//		//	return number
//		else return number;
//	}
//	
	/**
	 * Clean out a MODS header, i.e., remove all annotations that do not belong
	 * to the MODS namespace.
	 * @param modsHeader the MODS header to clean
	 */
	public static void cleanModsHeader(MutableAnnotation modsHeader) {
		Annotation[] inMods = modsHeader.getAnnotations();
		for (int i = 0; i < inMods.length; i++) {
			if (!inMods[i].getType().startsWith(MODS_PREFIX) && (inMods[i].size() < modsHeader.size()))
				modsHeader.removeAnnotation(inMods[i]);
			else {
				inMods[i].lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				if (MODS_MODS.equals(inMods[i].getType()))
					inMods[i].setAttribute(MODS_NAMESPACE_URI_ATTRIBUTE, MODS_NAMESPACE_URI);
			}
		}
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		MutableAnnotation mh = createModsHeader("Test-MODS-ID", null, "http://antbase.org/ants/publications/@ModsID/@ModsID.pdf");
		AnnotationUtils.writeXML(mh, new PrintWriter(System.out));
		mh = editModsHeader(mh, false);
		AnnotationUtils.writeXML(mh, new PrintWriter(System.out));
	}
}