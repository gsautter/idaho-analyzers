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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.goldenGateServer.util.Base64;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsConstants;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils;

/**
 * @author sautter
 *
 */
public class ModsTransferator implements ModsConstants {
	
	static String sourceHostUrl = "http://plazi.cs.umb.edu:8080/exist/rest/db/formicidae/mods"; 
	static String targetHostUrl = "http://plazi2.cs.umb.edu/exist/rest/db/plazi_mods"; 
	static String authentication = null;
	static {
		String auth = ("plazi" + ":" + "taxonx");
		int[] authenticationBytes = new int[auth.length()];
		for (int a = 0; a < auth.length(); a++)
			authenticationBytes[a] = auth.charAt(a);
		authentication = Base64.encode(authenticationBytes);
	}
	
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final Grammar grammar = new StandardGrammar();
		Parser parser = new Parser(grammar);
		final HashSet done = new HashSet();
		final HashSet todo = new HashSet();
		
		parser.stream(new InputStreamReader((new URL(targetHostUrl).openStream()), "UTF-8"), new TokenReceiver() {
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token) && "exist:resource".equals(grammar.getType(token))) {
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
					String name = tnas.getAttribute("name");
					if (name != null) {
						System.out.println(name + " ==> Done");
						done.add(name);
					}
				}
			}
		});
		
		parser.stream(new InputStreamReader((new URL(sourceHostUrl).openStream()), "UTF-8"), new TokenReceiver() {
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token) && "exist:resource".equals(grammar.getType(token))) {
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
					String name = tnas.getAttribute("name");
					if (name != null) {
//						System.out.print(name);
						if (done.contains(name)) {
//							System.out.print(" ==> Already Done");
						}
						else {
							MutableAnnotation modsHeader = getModsHeader(name, Gamta.INNER_PUNCTUATION_TOKENIZER);
							if (modsHeader != null) {
								uploadModsHeader(name, modsHeader);
								done.add(name);
//								System.out.print(" ==> Uploaded");
							}
							else {
								todo.add(name);
//								System.out.print(" ==> Todo");
							}
						}
//						System.out.println();
					}
				}
			}
		});
		
		System.out.println(done.size() + " done, " + todo.size() + " to go.");
	}
	
	private static MutableAnnotation getModsHeader(String modsName, Tokenizer tokenizer) throws IOException {
		
		//	get MODS header
		MutableAnnotation modsHeader = null;
		MutableAnnotation modsHeaderDoc;
		try {
			URL url = new URL(sourceHostUrl + "/" + modsName);
			InputStreamReader isr = new InputStreamReader(url.openStream());
			modsHeaderDoc = Gamta.newDocument(tokenizer);
			SgmlDocumentReader.readDocument(isr, modsHeaderDoc);
			isr.close();
		}
		catch (IOException ioe) {
			if (ioe.getMessage().startsWith("Connection timed out")) {
//				System.out.println(" ==> Timeout");
				try {
					Thread.sleep(2500);
				} catch (InterruptedException ie) {}
//				System.out.print(modsName + " ==> Retry");
				return getModsHeader(modsName, tokenizer);
			}
			else throw ioe;
		}
		
		MutableAnnotation modsHeaders[] = modsHeaderDoc.getMutableAnnotations(MODS_MODS);
		if (modsHeaders.length != 0)
			modsHeader = modsHeaders[0];
		
		//	print error report
		if (modsHeader == null)
			return null;
		
		String[] errors = ModsUtils.getErrorReport(modsHeader);
		System.out.println(modsName + " ==> Incomplete");
		for (int e = 0; e < errors.length; e++) {
			System.out.println("  " + errors[e]);
			if (errors[e].indexOf("volume") != -1) {
				Annotation[] volumeNumber = volumeNumberPath.evaluate(modsHeader, null);
				if (volumeNumber.length != 0)
					System.out.println("    " + volumeNumber[0].getValue());
			}
		}
		
		return null;
//		//	check MODS header
//		if ((modsHeader != null) && ModsUtils.checkModsHeader(modsHeader))
//			return modsHeader;
//		
//		//	create or complete MODS header
//		if (modsHeader == null)
//			modsHeader = ModsUtils.createModsHeader(modsName, "HNS-PUB", "http://antbase.org/ants/publications/@ModsID/@ModsID.pdf");
//		else modsHeader = ModsUtils.editModsHeader(modsHeader, true, "http://antbase.org/ants/publications/@ModsID/@ModsID.pdf");
//		
//		//	finally ...
//		return modsHeader;
	}
	
	private static final GPath volumeNumberPath = new GPath("//mods:detail[./@type = 'volume']/mods:number");
	
	private static void uploadModsHeader(String modsName, MutableAnnotation modsHeader) throws IOException {
		
		//	prepare upload URL
		HttpURLConnection putCon = getConnection(modsName, "PUT");
		
		//	do upload
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(putCon.getOutputStream(), "UTF-8"));
		
		//	no XSLT transformer, send plain data
		AnnotationUtils.writeXML(modsHeader, bw);
		
		//	print server's response
		bw.flush();
		bw.close();
		if (putCon.getResponseCode() > 300)
			throw new IOException("MODS header could not be uploaded: " + putCon.getResponseMessage());
	}
	
	private static HttpURLConnection getConnection(String modsName, String httpMethod) throws IOException {
		URL url = new URL(targetHostUrl + "/" + modsName);
		HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
		connection.setRequestMethod(httpMethod);
		if (authentication != null)
			connection.setRequestProperty("Authorization", ("Basic " + authentication));
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setDoOutput(true);
		return connection;
	}
}
