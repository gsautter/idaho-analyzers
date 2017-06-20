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
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
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
package de.uka.ipd.idaho.plugins.bibRefs.docHeader;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefEditorPanel;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient.BibRef;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient.BibRefIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This analyzer imports document meta data from RefBank and adds it to
 * documents.
 * 
 * @author sautter
 */
public class DocHeaderImporter extends AbstractConfigurableAnalyzer implements BibRefConstants {
	
	private RefBankClient refBankClient = null;
	
	private BibRefTypeSystem refTypeSystem;
	private String[] refIdTypes;
	
	/** @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		String refBankUrl = this.getParameter("refBankUrl");
		if (refBankUrl != null) try {
			this.refBankClient = new RefBankClient(this.dataProvider.getURL(refBankUrl).toString());
		} catch (IOException ioe) {}
		
		this.refTypeSystem = BibRefTypeSystem.getDefaultInstance();
		this.refIdTypes = this.getParameter("refIdTypes", " DOI Handle ISBN ISSN").split("\\s+");
	}
	
	//	TODO use analyzer config API
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	clean up existing header (if given)
		MutableAnnotation[] existingModsHeaders = data.getMutableAnnotations("mods:mods");
		MutableAnnotation modsHeader = ((existingModsHeaders.length == 0) ? null : existingModsHeaders[0]);
		if (modsHeader != null) {
			BibRefUtils.cleanModsXML(modsHeader);
			RefData modsRef = BibRefUtils.modsXmlToRefData(modsHeader);
			
			StringVector modsErrors = new StringVector();
			String[] errors = this.refTypeSystem.checkType(modsRef);
			if (errors != null)
				modsErrors.addContent(errors);
			boolean openForEditingDespiteNoErrors = false;
			
			//	no errors, simply set attributes, and ask if editing desired
			if (modsErrors.isEmpty()) {
				BibRefUtils.setDocAttributes(data, modsRef);
				if (!parameters.containsKey(INTERACTIVE_PARAMETER) || !FeedbackPanel.isLocal() || (JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), "The meta data header is complete. Open for editing?", "Meta Data Complete", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
					return;
				openForEditingDespiteNoErrors = true;
			}
			
			//	offer editing meta data header if incomplete, or if explicitly desired
			if (openForEditingDespiteNoErrors || (parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal() && (JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("The meta data header is incomplete:\n" + modsErrors.concatStrings("\n") + "\nCorrect errors now?"), "Meta Data Incomplete", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION))) {
				RefData newModsRef = BibRefEditorPanel.editRefData(modsRef, this.refTypeSystem, this.refIdTypes);
				
				//	edit dialog cancelled
				if (newModsRef == null)
					return;
				
				//	remove nested annotations from existing data
				Annotation[] inMods = modsHeader.getAnnotations();
				for (int i = 0; i < inMods.length; i++) {
					if (!"mods:mods".equals(inMods[i].getType()) || (!inMods[i].getType().startsWith("mods:") && (inMods[i].size() < modsHeader.size())))
						modsHeader.removeAnnotation(inMods[i]);
				}
				
				//	create new header for transformation
				MutableAnnotation newModsHeader = Gamta.newDocument(Gamta.newTokenSequence("", data.getTokenizer()));
				try {
					SgmlDocumentReader.readDocument(new StringReader(this.refTypeSystem.toModsXML(newModsRef)), newModsHeader);
				}
				catch (IOException ioe) {
					return; // never gonna happen, but Java don't know ...
				}
				
				//	transform tokens
				Gamta.doLevenshteinTransformation(modsHeader, newModsHeader);
				
				//	add new annotations
				inMods = newModsHeader.getAnnotations();
				for (int i = 0; i < inMods.length; i++) {
					if (inMods[i].getType().startsWith("mods:") && !"mods:mods".equals(inMods[i].getType()))
						modsHeader.addAnnotation(inMods[i]);
				}
				
				//	correct whitespace in URL
				MutableAnnotation[] modsUrls = modsHeader.getMutableAnnotations("mods:url");
				for (int u = 0; u < modsUrls.length; u++) {
					for (int w = 0; w < (modsUrls[u].size() - 1); w++)
						modsUrls[u].setWhitespaceAfter("", w);
				}
				
				//	correct whitespace in identifiers
				MutableAnnotation[] modsIdentifiers = modsHeader.getMutableAnnotations("mods:identifier");
				for (int i = 0; i < modsIdentifiers.length; i++) {
					for (int w = 0; w < (modsIdentifiers[i].size() - 1); w++)
						modsIdentifiers[i].setWhitespaceAfter("", w);
				}
				
				//	set attributes
				BibRefUtils.setDocAttributes(data, modsHeader);
				return;
			}
			
			//	editing cancelled, or no way of asking
			else return;
		}
		
		//	search meta data
		RefData modsRef;
		
		//	we're in a desktop environment
		if (parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal())
			modsRef = this.getRefData(data, parameters);
		
		//	we're in a remote (server based) environment
		else {
			
			//	collect identifiers
			RefData idRef = null;
			String[] ans = data.getAttributeNames();
			for (int a = 0; a < ans.length; a++) {
				if (!ans[a].startsWith("ID-"))
					continue;
				if (idRef == null)
					idRef = new RefData();
				idRef.setAttribute(ans[a], ((String) data.getAttribute(ans[a])));
			}
			
			//	nothing to work with
			if (idRef == null)
				return;
			
			//	get data
			modsRef = this.searchRefData(null, idRef, null);
		}
		
		//	check success
		if (modsRef == null)
			return;
		
		//	set attributes
		BibRefUtils.setDocAttributes(data, modsRef);
		
		//	obtain meta data in XML form
		MutableAnnotation loadedModsHeader = Gamta.newDocument(Gamta.newTokenSequence("", data.getTokenizer()));
		try {
			SgmlDocumentReader.readDocument(new StringReader(this.refTypeSystem.toModsXML(modsRef)), loadedModsHeader);
		}
		catch (IOException ioe) {
			return; // never gonna happen, but Java don't know ...
		}
		BibRefUtils.cleanModsXML(loadedModsHeader);
		
		//	insert data into document
		data.insertTokensAt(loadedModsHeader, 0);
		modsHeader = data.addAnnotation("mods:mods", 0, loadedModsHeader.size());
		modsHeader.copyAttributes(loadedModsHeader);
		Annotation[] loadedModsAnnotations = loadedModsHeader.getAnnotations();
		for (int a = 0; a < loadedModsAnnotations.length; a++) {
			if (DocumentRoot.DOCUMENT_TYPE.equals(loadedModsAnnotations[a].getType()))
				continue;
			if ("mods:mods".equals(loadedModsAnnotations[a].getType()))
				continue;
			if (loadedModsAnnotations[a].getType().startsWith("mods:"))
				modsHeader.addAnnotation(loadedModsAnnotations[a]);
		}
	}
	
	private boolean checkOnline(Properties parameters) {
		return (parameters.containsKey(ONLINE_PARAMETER)
				||
				(
					parameters.containsKey(INTERACTIVE_PARAMETER)
					&&
					FeedbackPanel.isLocal()
					&&
					(JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), "You are currently working in offline mode, allow fetching MODS metadata anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				)
			);
	}
	
	private static String[] extractableFieldNames = {
		AUTHOR_ANNOTATION_TYPE,
		TITLE_ANNOTATION_TYPE,
		YEAR_ANNOTATION_TYPE,
		PAGINATION_ANNOTATION_TYPE,
		JOURNAL_NAME_ANNOTATION_TYPE,
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		PUBLISHER_ANNOTATION_TYPE,
		LOCATION_ANNOTATION_TYPE,
		EDITOR_ANNOTATION_TYPE,
		VOLUME_TITLE_ANNOTATION_TYPE,
		PUBLICATION_URL_ANNOTATION_TYPE,
	};
	
	private RefData getRefData(final QueriableAnnotation doc, final Properties params) {
		final String docName = ((String) doc.getAttribute(DOCUMENT_NAME_ATTRIBUTE));
		final JDialog refEditDialog = DialogFactory.produceDialog(("Get Meta Data for Document" + ((docName == null) ? "" : (" " + docName))), true);
		final BibRefEditorPanel refEditorPanel = new BibRefEditorPanel(this.refTypeSystem, this.refIdTypes);
		final boolean[] cancelled = {false};
		
//		//	TODO use this if document contains parsed references close to top		
//		JButton use = new JButton("Use Ref");
//		use.setToolTipText("Use an already parsed reference from the document");
//		use.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				RefData ref = refEditorPanel.getRefData();
//				if (fillFromDocument(refEditDialog, docName, doc, ref))
//					refEditorPanel.setRefData(ref);
//			}
//		});
//		
		JButton extract = new JButton("Extract");
		extract.setToolTipText("Extract meta data from document");
		extract.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				RefData ref = refEditorPanel.getRefData();
				if (fillFromDocument(refEditDialog, docName, doc, ref))
					refEditorPanel.setRefData(ref);
			}
		});
		
		JButton search = new JButton("Search");
		search.setToolTipText("Search RefBank for meta data, using current input as query");
		if (this.refBankClient == null)
			search = null;
		else search.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (!checkOnline(params))
					return;
				RefData ref = refEditorPanel.getRefData();
				ref = searchRefData(refEditDialog, ref, docName);
				if (ref != null)
					refEditorPanel.setRefData(ref);
			}
		});
		
		JButton validate = new JButton("Validate");
		validate.setToolTipText("Check if the meta data is complete");
		validate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					JOptionPane.showMessageDialog(refEditorPanel, "The document meta data is valid.", "Validation Report", JOptionPane.INFORMATION_MESSAGE);
				else displayErrors(errors, refEditorPanel);
			}
		});
		
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					refEditDialog.dispose();
				else displayErrors(errors, refEditorPanel);
			}
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cancelled[0] = true;
				refEditDialog.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
		buttonPanel.add(extract);
		if (search != null)
			buttonPanel.add(search);
		buttonPanel.add(validate);
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		
		refEditDialog.getContentPane().setLayout(new BorderLayout());
		refEditDialog.getContentPane().add(refEditorPanel, BorderLayout.CENTER);
		refEditDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		refEditDialog.setSize(600, 500);
		refEditDialog.setLocationRelativeTo(DialogFactory.getTopWindow());
		refEditDialog.setVisible(true);
		
		return (cancelled[0] ? null : refEditorPanel.getRefData());
	}
	
	private boolean fillFromDocument(JDialog dialog, String docName, QueriableAnnotation doc, RefData ref) {
		DocumentView docView = new DocumentView(dialog, doc, ref);
		docView.setSize(700, 500);
		docView.setLocationRelativeTo(dialog);
		docView.setVisible(true);
		return docView.refModified;
	}
	
	private class DocumentView extends JDialog {
		private QueriableAnnotation doc;
		private int displayLength = 200;
		private RefData ref;
		private JTextArea textArea = new JTextArea();
		boolean refModified = false;
		DocumentView(JDialog owner, QueriableAnnotation doc, RefData rd) {
			super(owner, "Document View", true);
			this.doc = doc;
			this.ref = rd;
			
			this.textArea.setFont(new Font("Verdana", Font.PLAIN, 12));
			this.textArea.setLineWrap(true);
			this.textArea.setWrapStyleWord(true);
			this.textArea.setText(TokenSequenceUtils.concatTokens(this.doc, 0, this.displayLength, false, false));
			JScrollPane xmlAreaBox = new JScrollPane(this.textArea);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			JButton more = new JButton("More ...");
			more.setToolTipText("Show more document text");
			more.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DocumentView.this.displayLength += 100;
					DocumentView.this.textArea.setText(TokenSequenceUtils.concatTokens(DocumentView.this.doc, 0, DocumentView.this.displayLength, false, false));
				}
			});
			buttonPanel.add(more);
			for (int f = 0; f < extractableFieldNames.length; f++)
				buttonPanel.add(new ExtractButton(extractableFieldNames[f]));
			
			//	TODO add button 'Reference' to select whole reference at once, run it through RefParse, and then use all attributes found
			
			JButton closeButton = new JButton("Close");
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			this.setLayout(new BorderLayout());
			this.add(buttonPanel, BorderLayout.NORTH);
			this.add(xmlAreaBox, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
		}
		
		private class ExtractButton extends JButton implements ActionListener {
			String attribute;
			ExtractButton(String attribute) {
				this.setToolTipText("Use selected string as " + attribute);
				this.attribute = attribute;
				StringBuffer label = new StringBuffer();
				for (int c = 0; c < this.attribute.length(); c++) {
					char ch = this.attribute.charAt(c);
					if (c == 0)
						label.append(Character.toUpperCase(ch));
					else {
						if (Character.isUpperCase(ch))
							label.append(' ');
						label.append(ch);
					}
				}
				this.setText(label.toString());
				this.addActionListener(this);
			}
			public void actionPerformed(ActionEvent ee) {
				String value = textArea.getSelectedText().trim();
				if (value.length() != 0) {
					if (AUTHOR_ANNOTATION_TYPE.equals(this.attribute) || EDITOR_ANNOTATION_TYPE.equals(this.attribute))
						ref.addAttribute(this.attribute, value);
					else ref.setAttribute(this.attribute, value);
					refModified = true;
				}
			}
		}
	}
	
	private final void displayErrors(String[] errors, JPanel parent) {
		StringVector errorMessageBuilder = new StringVector();
		errorMessageBuilder.addContent(errors);
		JOptionPane.showMessageDialog(parent, ("The document meta data is not valid. In particular, there are the following errors:\n" + errorMessageBuilder.concatStrings("\n")), "Validation Report", JOptionPane.ERROR_MESSAGE);
	}
	
	private final RefData searchRefData(JDialog dialog, RefData ref, String docName) {
		
		//	can we search?
		if (this.refBankClient == null)
			return null;
		
		//	get search data
		String author = ref.getAttribute(AUTHOR_ANNOTATION_TYPE);
		String title = ref.getAttribute(TITLE_ANNOTATION_TYPE);
		String origin = this.refTypeSystem.getOrigin(ref);
		String year = ref.getAttribute(YEAR_ANNOTATION_TYPE);
		
		//	test year
		if (year != null) try {
			Integer.parseInt(year);
		}
		catch (NumberFormatException nfe) {
			year = null;
		}
		
		//	get identifiers
		String[] extIdTypes = ref.getIdentifierTypes();
		String extIdType = (((extIdTypes == null) || (extIdTypes.length == 0)) ? null : extIdTypes[0]);
		String extId = ((extIdType == null) ? null : ref.getIdentifier(extIdType));
		
		//	got something to search for?
		if ((extId == null) && (author == null) && (title == null) && (year == null) && (origin == null)) {
			if (dialog != null)
				JOptionPane.showMessageDialog(dialog, "Please enter some data to search for.\nYou can also use 'View Doc' to copy some data from the document text.", "Cannot Search Document Meta Data", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//	set up search
		Vector pss = new Vector();
		
		//	perform search
		try {
			BibRefIterator brit = this.refBankClient.findRefs(null, author, title, ((year == null) ? -1 : Integer.parseInt(year)), origin, extId, extIdType, 0, false);
			while (brit.hasNextRef()) {
				BibRef ps = brit.getNextRef();
				String rs = ps.getRefParsed();
				if (rs == null)
					continue;
				try {
					pss.add(new RefDataListElement(BibRefUtils.modsXmlToRefData(SgmlDocumentReader.readDocument(new StringReader(rs)))));
				} catch (IOException ioe) { /* never gonna happen, but Java don't know ... */ }
			}
		} catch (IOException ioe) { /* let's not bother with exceptions for now, just return null ... */ }
		
		//	did we find anything?
		if (pss.isEmpty()) {
			if (dialog != null)
				JOptionPane.showMessageDialog(dialog, "Your search did not return any results.\nYou can still enter the document meta data manually.", "Document Meta Data Not Fount", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//	not allowed to show list dialog, return data only if we have an unambiguous match
		if (dialog == null)
			return ((pss.size() == 1) ? ((RefDataListElement) pss.get(0)).ref : null);
		
		//	display whatever is found in list dialog
		RefDataList refDataList = new RefDataList(dialog, ("Select Meta Data for Document" + ((docName == null) ? "" : (" " + docName))), pss);
		refDataList.setVisible(true);
		return refDataList.ref;
	}
	
	private class RefDataList extends JDialog {
		private JList refList;
		RefData ref = new RefData();
		RefDataList(JDialog owner, String title, final Vector refData) {
			super(owner, title, true);
			
			this.refList = new JList(refData);
			this.refList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane refListBox = new JScrollPane(this.refList);
			refListBox.getVerticalScrollBar().setUnitIncrement(50);
			
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int sri = refList.getSelectedIndex();
					if (sri == -1)
						return;
					ref = ((RefDataListElement) refData.get(sri)).ref;
					dispose();
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(ok);
			buttonPanel.add(cancel);
			
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(refListBox, BorderLayout.CENTER);
			this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			this.setSize(600, 370);
			this.setLocationRelativeTo(owner);
		}
	}
	private class RefDataListElement {
		final RefData ref;
		final String displayString;
		RefDataListElement(RefData ref) {
			this.ref = ref;
			this.displayString = BibRefUtils.toRefString(this.ref);
		}
		public String toString() {
			return this.displayString;
		}
	}
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		System.getProperties().put("proxySet", "true");
//		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
//		System.getProperties().put("proxyPort", "3128");
//		
//		ModsDataServletClient msc = new ModsDataServletClient("http://plazi2.cs.umb.edu:8080/ModsServer/mods");
//		ModsDataSet mds = msc.getModsData("21330");
//		Writer w = new OutputStreamWriter(System.out);
//		mds.writeModsHeader(w);
//		w.flush();
//		
//		Properties msd = new Properties();
////		msd.setProperty(MODS_DATE_ATTRIBUTE, "2007");
//		msd.setProperty("someNonExistingAttribute", "withSomeArbitraryValue");
//		ModsDataSet[] mdss = msc.findModsData(msd, true);
//		for (int d = 0; d < mdss.length; d++)
//			mdss[d].writeModsHeader(w);
//		
////		System.out.println(msc.storeModsData(mds, "MODS rules it all!"));
//	}
//	
//	public static void main(String[] args) throws Exception {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		final BibRefTypeSystem brts = BibRefTypeSystem.getDefaultInstance();
//		String[] idTypes = {"SMNK-Pub", "HNS-PUB", "TEST"};
//		
//		final BibRefEditorPanel brep = new BibRefEditorPanel(brts, idTypes);
//		
//		JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
//		JButton b;
//		b = new JButton("Validate");
//		b.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				String[] errors = brep.getErrors();
//				StringBuffer em = new StringBuffer();
//				int mt;
//				if (errors == null) {
//					em.append("The reference is valid.");
//					mt = JOptionPane.INFORMATION_MESSAGE;
//				}
//				else {
//					em.append("The reference has errors:");
//					for (int e = 0; e < errors.length; e++)
//						em.append("\n - " + errors[e]);
//					mt = JOptionPane.ERROR_MESSAGE;
//				}
//				JOptionPane.showMessageDialog(brep, em.toString(), "Reference Validation", mt);
//			}
//		});
//		bp.add(b);
//		b = new JButton("Print XML");
//		b.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				RefData rd = brep.getRefData();
//				System.out.println(rd.toXML());
//			}
//		});
//		bp.add(b);
//		b = new JButton("Print MODS");
//		b.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				RefData rd = brep.getRefData();
//				System.out.println(brts.toModsXML(rd));
//			}
//		});
//		bp.add(b);
//		b = new JButton("Print Origin");
//		b.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				RefData rd = brep.getRefData();
//				System.out.println(brts.getOrigin(rd));
//			}
//		});
//		bp.add(b);
//		
//		JDialog d = DialogFactory.produceDialog("Test", true);
//		d.getContentPane().setLayout(new BorderLayout());
//		d.getContentPane().add(brep, BorderLayout.CENTER);
//		d.getContentPane().add(bp, BorderLayout.SOUTH);
//		d.setSize(600, 600);
//		d.setLocationRelativeTo(null);
//		d.setVisible(true);
//	}
}
