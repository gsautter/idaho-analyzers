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


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class ModsReferencer extends AbstractConfigurableAnalyzer implements ModsConstants {
	
	//	TODO use RefBank
	
	private String modsServerURL = null;//"http://plazi2.cs.umb.edu:8080/ModsServer/mods";
	private ModsDataServletClient modsServerClient = null;
	private String modsServerPassPhrase = null;
	
	private String generateIdType = null;
	private String modsUrlPattern = null;
	
	/* TEMPORARY DEFAULT URL
	 * http://osuc.biosci.ohio-state.edu/hymDB/hym_utilities.format_ref?style=MODS&id=
	 */
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		this.modsServerURL = this.getParameter("modsServerURL");
		this.modsServerClient = new ModsDataServletClient(this.modsServerURL, this.dataProvider);
		this.modsServerPassPhrase = this.getParameter("modsServerPassPhrase");
		
		this.generateIdType = this.getParameter("generateIdType");
		this.modsUrlPattern = this.getParameter("modsUrlPattern");
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	clean up existing header (if given)
		MutableAnnotation[] existingModsHeaders = data.getMutableAnnotations(MODS_MODS);
		MutableAnnotation modsHeader = ((existingModsHeaders.length == 0) ? null : existingModsHeaders[0]);
		if (modsHeader != null) {
			ModsUtils.cleanModsHeader(modsHeader);
			
			StringVector modsErrors = new StringVector();
			modsErrors.addContent(ModsUtils.getErrorReport(modsHeader));
			
			//	no errors, simply set attributes
			if (modsErrors.isEmpty()) {
				ModsUtils.setModsAttributes(data, modsHeader);
				return;
			}
			
			//	offer editing MODS header if incomplete
			else if (parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal() && (JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), ("The MODS header is incomplete:\n" + modsErrors.concatStrings("\n") + "\nCorrect MODS header now?"), "MODS Header Incomplete", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION)) {
				MutableAnnotation newModsHeader = ModsUtils.editModsHeader(modsHeader, false, this.modsUrlPattern);
				
				//	edit dialog cancelled
				if (newModsHeader == null)
					return;
				
				//	remove nested annotations
				Annotation[] inMods = modsHeader.getAnnotations();
				for (int i = 0; i < inMods.length; i++) {
					if (!MODS_MODS.equals(inMods[i].getType()) || (!inMods[i].getType().startsWith(MODS_PREFIX) && (inMods[i].size() < modsHeader.size())))
						modsHeader.removeAnnotation(inMods[i]);
				}
				
				//	transform tokens
				Gamta.doLevenshteinTransformation(modsHeader, newModsHeader);
				
				//	add new annotations
				inMods = newModsHeader.getAnnotations();
				for (int i = 0; i < inMods.length; i++) {
					if (inMods[i].getType().startsWith(MODS_PREFIX) && !MODS_MODS.equals(inMods[i].getType()))
						modsHeader.addAnnotation(inMods[i]);
				}
				
				//	correct whitespace in URL
				MutableAnnotation[] modsUrls = modsHeader.getMutableAnnotations(MODS_URL);
				if (modsUrls.length != 0) {
					for (int w = 0; w < (modsUrls[0].size() - 1); w++)
						modsUrls[0].setWhitespaceAfter("", w);
				}
				
				//	set attributes
				ModsUtils.setModsAttributes(data, modsHeader);
				return;
			}
			
			//	editing cancelled, or no way of asking
			else return;
		}
		
		//	obtain MODS data
		String modsDocID = ((String) data.getAttribute(MODS_ID_ATTRIBUTE));
		ModsDataSet mds;
		
		//	we're in a desktop environment
		if (parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal()) {
			
			//	this document is untouched so far, allow searching, etc.
			if (modsDocID == null) {
				
				//	check if web access allowed
				if (!this.checkOnline(parameters))
					return;
				
				//	go searching for MODS data
				mds = this.findModsData(data);
			}
			
			//	download data
			else mds = this.getModsData(modsDocID, data, parameters);
		}
		
		//	we're in a remote (server based) environment
		else {
			
			//	nothing to work with
			if (modsDocID == null)
				return;
			
			//	get data
			mds = this.getModsData(modsDocID, data, parameters);
		}
		
		//	check success
		if (mds == null)
			return;
		
		//	set attributes
		data.setAttribute(MODS_ID_ATTRIBUTE, mds.id);
		if (data instanceof DocumentRoot)
			((DocumentRoot) data).setDocumentProperty(MODS_ID_ATTRIBUTE, mds.id);
		ModsUtils.setModsAttributes(data, mds);
		
		//	obtain MODS header
		modsHeader = mds.getModsHeader();
		
		//	insert data into document
		data.insertTokensAt(modsHeader, 0);
		Annotation[] modsAnnotations = modsHeader.getAnnotations();
		if ((modsAnnotations.length == 0) || !MODS_MODS.equals(modsAnnotations[0].getType())) {
			Annotation added = data.addAnnotation(modsHeader);
			added.setAttribute(MODS_NAMESPACE_URI_ATTRIBUTE, MODS_NAMESPACE_URI);
		}
		for (int a = 0; a < modsAnnotations.length; a++)
			if (!DocumentRoot.DOCUMENT_TYPE.equals(modsAnnotations[a].getType())) {
				Annotation added = data.addAnnotation(modsAnnotations[a]);
				if (MODS_MODS.equals(added.getType()))
					added.setAttribute(MODS_NAMESPACE_URI_ATTRIBUTE, MODS_NAMESPACE_URI);
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
					(JOptionPane.showConfirmDialog(DialogFactory.getTopWindow(), "MODS Referencer has been invoked in offline mode, allow fetching MODS metadata anyway?", "Allow Web Access", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				)
			);
	}
	
	private void cacheModsData(ModsDataSet mds) {
		if (this.dataProvider.isDataEditable()) try {
			OutputStreamWriter osw = new OutputStreamWriter(this.dataProvider.getOutputStream("cache/" + mds.id + ".xml"), "UTF-8");
			AnnotationUtils.writeXML(mds.getModsHeader(), osw);
			osw.flush();
			osw.close();
			System.out.println("Cache extended");
		}
		catch (IOException ioe) {
			System.out.println("Exception caching MODS header for document '" + mds.id + "':" + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	private ModsDataSet getModsData(String modsId, MutableAnnotation data, Properties parameters) {
		
		//	try to find header in cache
		try {
			InputStreamReader isr = new InputStreamReader(this.dataProvider.getInputStream("cache/" + modsId + ".xml"), "UTF-8");
			MutableAnnotation modsHeaderDoc = Gamta.newDocument(data.getTokenizer());
			SgmlDocumentReader.readDocument(isr, modsHeaderDoc);
			isr.close();
			
			MutableAnnotation modsHeaders[] = modsHeaderDoc.getMutableAnnotations(MODS_MODS);
			if (modsHeaders.length != 0) {
				System.out.println("Cache hit");
				return ModsUtils.getModsDataSet(modsHeaders[0]);
			}
		}
		catch (IOException ioe) {
			System.out.println("Cache miss");
		}
		
		//	ask if web access allowed if in offline mode
		if (!this.checkOnline(parameters))
			return null;
		
		//	try downloading header from remote source
		ModsDataSet mds;
		try {
			mds = this.modsServerClient.getModsData(modsId);
		}
		catch (IOException ioe) {
			if (parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal())
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("Error downloading MODS header for document '" + modsId + "':\n  " + ioe.getMessage()), "Error Downloading MODS Header", JOptionPane.ERROR_MESSAGE);
			System.out.println("Exception downloading MODS header for document '" + modsId + "':\n  " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		
		//	complete header if allowed and necessary
		if ((mds != null) && parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal())
			mds = ModsUtils.editModsData(mds, true, this.modsUrlPattern);
		
		//	check success
		if (mds == null)
			return null;
		
		//	put header into cache if download successful
		this.cacheModsData(mds);
		
		//	finally ...
		return mds;
	}
	
	private ModsDataSet findModsData(MutableAnnotation data) {
		ModsDataSet mds = this.modsServerClient.findModsData(data, null, this.generateIdType, this.modsUrlPattern, this.modsServerPassPhrase);
//		if (mds != null)
		if ((mds != null) && (this.generateIdType != null) && this.generateIdType.equals(mds.idType))
			this.cacheModsData(mds);
		return mds;
	}
}