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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;
import de.uka.ipd.idaho.stringUtils.StringVector;


/**
 * REST client for MODS servlet.
 * 
 * @author sautter
 */
public class ModsDataServletClient implements ModsConstants {
	
	private AnalyzerDataProvider urlProvider = null;
	private String modsServletURL;
	
	/**
	 * @param modsServletURL the URL of the MODS servlet to communicate with
	 */
	public ModsDataServletClient(String modsServletURL) {
		this(modsServletURL, null);
	}
	
	/**
	 * @param modsServletURL the URL of the MODS servlet to communicate with
	 * @param urlProvider a data provider to obtain URLs from
	 */
	public ModsDataServletClient(String modsServletURL, AnalyzerDataProvider urlProvider) {
		this.modsServletURL = modsServletURL;
		this.urlProvider = urlProvider;
	}
	
	private URL getUrl(String urlString) throws IOException {
		return ((this.urlProvider == null) ? new URL(urlString) : this.urlProvider.getURL(urlString));
	}
	
	/**
	 * Retrieve a MODS document meta data set from the backing servlet, using
	 * the document ID as the access key. If the backing servlet does not
	 * provide a meta data set for the document with the specified ID, this
	 * method returns null. The returned meta data might be incomplete (the
	 * ModsUtils.getErrorReport() method does report errors).
	 * @param modsId the ID of the document to retrieve the meta data for
	 * @return the meta data of the document with the specified ID
	 * @throws IOException
	 */
	public ModsDataSet getModsData(String modsId) throws IOException {
		URL url = this.getUrl(this.modsServletURL + "?" + MODS_ID_ATTRIBUTE + "=" + URLEncoder.encode(modsId, "UTF-8"));
		
		try {
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
			isr.close();
			
			QueriableAnnotation[] modsHeaders = modsDocument.getAnnotations("mods:mods");
			if (modsHeaders.length == 1)
				return ModsUtils.getModsDataSet(modsHeaders[0]);
			
			else if (modsHeaders.length == 0)
				return null;
			
			else throw new IOException("The specified identifier seems to be ambiguous.");
		}
		catch (FileNotFoundException fnfe) {
			return null; // URL throws this for an HTTP 404
		}
	}
	
	/**
	 * Retrieve the searchable attributes from the backing servlet. These and
	 * only these attributes can be used in findModsData().
	 * @return an array holding the searchable attributes
	 * @throws IOException
	 */
	public String[] getSearchAttributes() throws IOException {
		if (this.searchAttributes == null) {
			URL url = this.getUrl(this.modsServletURL + "?getSearchAttributes=true");
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			StringVector searchAttributes = StringVector.loadList(isr);
			isr.close();
			this.searchAttributes = searchAttributes.toStringArray();
		}
		return this.searchAttributes;
	}
	private String[] searchAttributes = null;
	
	/**
	 * Find the MODS document meta data set in the backing servlet, using known
	 * search attributes the access key, e.g. the document author, parts of the
	 * title, the name of the journal the document appeared in, or the name or
	 * location of the publisher who issued the document. If multiple meta data
	 * sets match, this method returns them all. If the search criteria are
	 * empty, this method returns an empty array. If the backing servlet does
	 * not provide any meta data sets that match the search criteria, this
	 * method returns an empty array. Some of the meta data sets returned might
	 * be incomplete (the ModsUtils.getErrorReport() method does report errors).
	 * @param modsData the known elements of the meta data to retrieve in full
	 * @param cacheOnly restrict search to the backing servlet's cache?
	 * @return the meta data sets matching the specified criteria
	 * @throws IOException
	 */
	public ModsDataSet[] findModsData(Properties modsData, boolean cacheOnly) throws IOException {
		StringBuffer query = new StringBuffer();
		String[] searchAttributes = this.getSearchAttributes();
		for (int a = 0; a < searchAttributes.length; a++) {
			String searchValue = modsData.getProperty(searchAttributes[a]);
			if (searchValue == null)
				continue;
			if (query.length() != 0)
				query.append("&");
			query.append(searchAttributes[a] + "=" + URLEncoder.encode(searchValue, "UTF-8"));
		}
		if (query.length() == 0) {
			for (int a = 0; a < searchAttributes.length; a++) {
				if (query.length() != 0)
					query.append(", ");
				query.append(searchAttributes[a]);
			}
			throw new IOException("Invalid request, specify at least one of the following attributes: " + query.toString());
		}
		
		URL url = this.getUrl(this.modsServletURL + "?" + query.toString());
		try {
			InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
			MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
			isr.close();
			
			QueriableAnnotation[] modsHeaders = modsDocument.getAnnotations("mods:mods");
			ModsDataSet[] mdss = new ModsDataSet[modsHeaders.length];
			for (int d = 0; d < modsHeaders.length; d++)
				mdss[d] = ModsUtils.getModsDataSet(modsHeaders[d]);
			
			return mdss;
		}
		catch (FileNotFoundException fnfe) {
			return new ModsDataSet[0]; // URL throws this for an HTTP 404
		}
	}
	
	/**
	 * Find the MODS meta data for a given document. If the specified pass
	 * phrase is null, newly entered or corrected MODS data sets will not be
	 * stored on the backing servlet. The returned MODS data set is valid.
	 * @param doc the document to find the meta data for
	 * @param docName the name of the document (for display purposes)
	 * @param generateIdType the type attribute to use for the MODS ID if it is
	 *            to be generated
	 * @param modsUrlPattern a pattern to generate the URL from the ID
	 * @param modsServerPassPhrase the pass phrase for storing a newly generated
	 *            or modified MODS data set on a backing MODS servlet
	 * @return a MODS meta data set for the specified document
	 */
	public ModsDataSet findModsData(MutableAnnotation doc, String docName, String generateIdType, String modsUrlPattern, String modsServerPassPhrase) {
		
		//	get MODS ID
		String modsDocID = ((String) doc.getAttribute(MODS_ID_ATTRIBUTE));
		JDialog mdd = DialogFactory.produceDialog(("Get MODS Data for Document" + ((docName == null) ? "" : (" " + docName))), true);
		ModsDataPanel mdp;
		try {
			mdp = new ModsDataPanel(mdd, this, modsDocID, doc);
		}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("An error occurred while searching for MODS Data: " + ioe.getMessage()), "Error Searching MODS Data", JOptionPane.ERROR_MESSAGE);
			ioe.printStackTrace(System.out);
			return null;
		}
		mdd.setVisible(true);
		
		//	get result of dialog
		modsDocID = mdp.getModsID();
		ModsDataSet modsData = mdp.getModsData();
		
		//	dialog cancelled
		if (modsDocID == null)
			return null;
		
		//	check MODS header
		if ((modsData == null) || !modsData.isValid()) {
			
			//	create or complete MODS header
			String editMode;
			if (modsData == null) {
				//	TODO transfer title, author, and year from search dialog
				modsData = ModsUtils.createModsData(modsDocID, ("#".equals(modsDocID) ? "GenericHash" : generateIdType), mdp.getModsAttributes(), ("#".equals(modsDocID) ? null : modsUrlPattern));
//				modsData = ModsUtils.createModsData(modsDocID, generateIdType, modsUrlPattern);
				editMode = "newly created";
			}
			else {
				modsData = ModsUtils.editModsData(modsData, true, modsUrlPattern);
				editMode = "updated";
			}
			
			//	check if dialog canceled
			if (modsData == null)
				return null;
			
			//	upload MODS header
//			if ((modsServerPassPhrase != null) && (JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("Upload the " + editMode + " MODS meta data to the backing source?"), "Upload MODS Header", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) try {
			if (!"#".equals(modsDocID) && (modsServerPassPhrase != null) && (JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("Upload the " + editMode + " MODS meta data to the backing source?"), "Upload MODS Header", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) try {
				this.storeModsData(modsData, modsServerPassPhrase);
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.out);
				if (JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("The " + editMode + " MODS meta data could not be uploaded to the backing source;\n" + ioe.getMessage() + "\nUse the MODS header for " + ((docName == null) ? "the document" : ("document '" + docName + "'")) + " anyway?"), "MODS Header Upload Failed", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION)
					return null;
			}
		}
		
		//	return MODS data
		return modsData;
	}
	
	private static class ModsDataPanel extends JPanel {
		
		private ModsDataServletClient modsServerClient;
		
		private String modsId;
		private ModsDataSet modsData;
		
		private JTextField modsIdField = new JTextField();
		private JTextField modsDocAuthorField = new JTextField();
		private JTextField modsDocDateField = new JTextField();
		private JTextField modsDocTitleField = new JTextField();
		
		private JLabel noModsDataLabel = new JLabel("<HTML><CENTER>" +
				"<B>No MODS data available.</B>" +
				"<BR>" +
				"<BR>Use the fields above and <B>Search</B> to find MODS data." +
				"<BR>If you already did this, you might have mis-typed one or more search attributes." +
				"<BR>If you cannot find a MODS header despite correct search attributes," +
				"<BR>enter a new MODS ID above and use <B>Create</B> to enter a new MODS header." +
				"</CENTER></HTML>", JLabel.CENTER);
		private JList modsDataList = new JList();
		private JScrollPane modsDataListBox = new JScrollPane(this.modsDataList);
		
		ModsDataPanel(final JDialog dialog, ModsDataServletClient modsServerClient, String initialModsId, final MutableAnnotation doc) throws IOException {
			super(new BorderLayout(), true);
			this.modsServerClient = modsServerClient;
			if (initialModsId != null)
				this.modsIdField.setText(initialModsId);
			
			this.modsDataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.modsDataListBox.getVerticalScrollBar().setUnitIncrement(25);
			
			JPanel searchPanel = new JPanel(new GridBagLayout());
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.gridy = 0;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			searchPanel.add(new JLabel("MODS ID", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			searchPanel.add(this.modsIdField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			searchPanel.add(new JLabel("Author(s)", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			searchPanel.add(this.modsDocAuthorField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			searchPanel.add(new JLabel("Year", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			searchPanel.add(this.modsDocDateField, gbc.clone());
			gbc.gridy++;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			searchPanel.add(new JLabel("Title", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			searchPanel.add(this.modsDocTitleField, gbc.clone());
			gbc.gridy++;
			
			JButton view = new JButton("View Document");
			view.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					DocumentView docView = new DocumentView(dialog, doc);
					docView.setSize(500, 300);
					docView.setLocationRelativeTo(dialog);
					docView.setVisible(true);
				}
			});
			JButton search = new JButton("Search");
			search.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						searchModsData();
					}
					catch (IOException ioe) {
						JOptionPane.showMessageDialog(ModsDataPanel.this, ("An error occurred while searching for MODS Data: " + ioe.getMessage()), "Error Searching MODS Data", JOptionPane.ERROR_MESSAGE);
						ioe.printStackTrace();
					}
				}
			});
			JButton create = new JButton("Create");
			create.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					String newModsId = modsIdField.getText().trim();
					if (newModsId.length() == 0) {
						int generate = JOptionPane.showConfirmDialog(ModsDataPanel.this, "You did not enter a valid MODS ID. Should it be generated automatically?\nPlease keep in mind that auto-generated MODS IDs are restricted to local\nuse because there is no global authority holding them stable.", "Invalid MODS ID", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
						if (generate == JOptionPane.YES_NO_OPTION)
							newModsId = "#";
					}
					if (newModsId.length() != 0) {
						modsId = newModsId;
						modsData = null;
						dialog.dispose();
						//	TODO transfer title, author, and year as well
					}
//					//	TODOne facilitate hashing MODS ID from other data, maybe set ID to '#' initially
//					if (newModsId.length() == 0)
//						JOptionPane.showMessageDialog(ModsDataPanel.this, "Please enter the MODS ID to use.", "Invalid MODS ID", JOptionPane.ERROR_MESSAGE);
//					else {
//						modsId = newModsId;
//						modsData = null;
//						dialog.dispose();
//					}
				}
			});
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Object mdle = modsDataList.getSelectedValue();
					if (mdle == null)
						JOptionPane.showMessageDialog(ModsDataPanel.this, "Please select the MODS data set to use.\nThe search fields will help you find it.", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
					else {
						modsId = ((ModsDataListElement) mdle).mds.id;
						modsData = ((ModsDataListElement) mdle).mds;
						dialog.dispose();
					}
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					modsId = null;
					modsData = null;
					dialog.dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(view);
			buttonPanel.add(search);
			buttonPanel.add(create);
			buttonPanel.add(ok);
			buttonPanel.add(cancel);
			
			this.add(searchPanel, BorderLayout.NORTH);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			dialog.getContentPane().setLayout(new BorderLayout());
			dialog.getContentPane().add(this, BorderLayout.CENTER);
			dialog.setSize(500, 300);
			dialog.setLocationRelativeTo(DialogFactory.getTopWindow());
			
			if (initialModsId == null)
				this.add(this.noModsDataLabel, BorderLayout.CENTER);
			else this.searchModsData();
		}
		
		private void searchModsData() throws IOException {
			ModsDataSet[] mdss;
			
			String modsId = this.modsIdField.getText().trim();
			if (modsId.length() == 0) {
				Properties search = new Properties();
				String author = this.modsDocAuthorField.getText().trim();
				if (author.length() != 0)
					search.setProperty(MODS_AUTHOR_ATTRIBUTE, author);
				String title = this.modsDocTitleField.getText().trim();
				if (title.length() != 0)
					search.setProperty(MODS_TITLE_ATTRIBUTE, title);
				int date = -1;
				try {
					date = Integer.parseInt(this.modsDocDateField.getText().trim());
				}
				catch (NumberFormatException nfe) {
					this.modsDocDateField.setText("");
				}
				if (date != -1)
					search.setProperty(MODS_DATE_ATTRIBUTE, ("" + date));
				if (search.isEmpty()) {
					JOptionPane.showMessageDialog(this, "Please enter at least one search attribute.", "Invalid Search Data", JOptionPane.ERROR_MESSAGE);
					mdss = new ModsDataSet[0];
				}
				else mdss = this.modsServerClient.findModsData(search, false);
			}
			else {
				ModsDataSet mds = this.modsServerClient.getModsData(modsId);
				if (mds == null)
					mdss = new ModsDataSet[0];
				else {
					mdss = new ModsDataSet[1];
					mdss[0] = mds;
				}
			}
			
			DefaultListModel dlm = new DefaultListModel();
			for (int m = 0; m < mdss.length; m++)
				dlm.addElement(new ModsDataListElement(mdss[m]));
			this.modsDataList.setModel(dlm);
			
			if (mdss.length == 0) {
				this.remove(this.modsDataListBox);
				this.add(this.noModsDataLabel, BorderLayout.CENTER);
			}
			else {
				this.remove(this.noModsDataLabel);
				this.add(this.modsDataListBox, BorderLayout.CENTER);
			}
			
			this.validate();
			this.repaint();
		}
		
		String getModsID() {
			return this.modsId;
		}
		
		Attributed getModsAttributes() {
			Attributed attributes = Gamta.newDocument(Gamta.newTokenSequence(null, null));
			
			String author = this.modsDocAuthorField.getText().trim();
			if (author.length() != 0)
				attributes.setAttribute(MODS_AUTHOR_ATTRIBUTE, author);
			
			String title = this.modsDocTitleField.getText().trim();
			if (title.length() != 0)
				attributes.setAttribute(MODS_TITLE_ATTRIBUTE, title);
			
			int date = -1;
			try {
				date = Integer.parseInt(this.modsDocDateField.getText().trim());
			} catch (NumberFormatException nfe) {}
			if (date != -1)
				attributes.setAttribute(MODS_DATE_ATTRIBUTE, ("" + date));
			
			return attributes;
		}
		
		ModsDataSet getModsData() {
			return this.modsData;
		}
		
		private class ModsDataListElement {
			final ModsDataSet mds;
			final String displayString;
			ModsDataListElement(ModsDataSet mds) {
				this.mds = mds;
				this.displayString = (mds.getAuthorString() + " (" + mds.getYear() + ") " + mds.getTitle());
			}
			public String toString() {
				return this.displayString;
			}
		}
		
		private class DocumentView extends JDialog {
			private MutableAnnotation doc;
			private JTextArea textArea = new JTextArea();
			DocumentView(JDialog owner, MutableAnnotation doc) {
				super(owner, "Document View", true);
				this.doc = doc;
				
				this.textArea.setFont(new Font("Verdana", Font.PLAIN, 12));
				this.textArea.setLineWrap(false);
				this.textArea.setWrapStyleWord(false);
				this.textArea.setText(TokenSequenceUtils.concatTokens(this.doc, 0, 200, false, false));
				JScrollPane xmlAreaBox = new JScrollPane(this.textArea);
				
				JButton author = new JButton("Author");
				author.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String author = textArea.getSelectedText().trim();
						if (author.length() != 0)
							modsDocAuthorField.setText(author);
					}
				});
				JButton year = new JButton("Year");
				year.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String year = textArea.getSelectedText().trim();
						if (year.length() != 0)
							modsDocDateField.setText(year);
					}
				});
				JButton title = new JButton("Title");
				title.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String title = textArea.getSelectedText().trim();
						if (title.length() != 0)
							modsDocTitleField.setText(title);
					}
				});
				JButton close = new JButton("Close");
				close.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
				buttonPanel.add(author);
				buttonPanel.add(year);
				buttonPanel.add(title);
				buttonPanel.add(close);
				
				this.setLayout(new BorderLayout());
				this.add(xmlAreaBox, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
			}
		}
	}
	
	/**
	 * Upload a MODS document meta data set to the backing servlet. The argument
	 * MODS data set must be valid, i.e., its isValid() method must return true
	 * for the upload to succees. If it is not, an IOException will be thrown. 
	 * @param modsData the meta data set to store
	 * @param passPhrase the pass phrase to use for authentication
	 * @return true if the data was created (stored for the first time), false
	 *         if it was updated
	 * @throws IOException
	 */
	public boolean storeModsData(ModsDataSet modsData, String passPhrase) throws IOException {
		if (!modsData.isValid())
			throw new IOException("Cannot upload invalid data set.");
		
		URL url = new URL(this.modsServletURL);
		HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("pass", ("" + passPhrase.hashCode()));
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setDoOutput(true);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
		modsData.writeModsHeader(bw);
		bw.flush();
		
		if (connection.getResponseCode() == 200) // HTTP OK
			return false;
		else if (connection.getResponseCode() == 201) // HTTP CREATED
			return true;
		else throw new IOException(connection.getResponseMessage());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		ModsDataServletClient msc = new ModsDataServletClient("http://plazi2.cs.umb.edu:8080/ModsServer/mods");
		ModsDataSet mds = msc.getModsData("21330");
		Writer w = new OutputStreamWriter(System.out);
		mds.writeModsHeader(w);
		w.flush();
		
		Properties msd = new Properties();
//		msd.setProperty(MODS_DATE_ATTRIBUTE, "2007");
		msd.setProperty("someNonExistingAttribute", "withSomeArbitraryValue");
		ModsDataSet[] mdss = msc.findModsData(msd, true);
		for (int d = 0; d < mdss.length; d++)
			mdss[d].writeModsHeader(w);
		
//		System.out.println(msc.storeModsData(mds, "MODS rules it all!"));
	}
}
