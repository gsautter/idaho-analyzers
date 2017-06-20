package de.uka.ipd.idaho.plugins.materialsCitations;
///*
// * Copyright (c) 2006-2008, IPD Boehm, Universitaet Karlsruhe (TH)
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package de.uka.ipd.idaho.plugin.materialsCitations;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.TreeMap;
//
//import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
//import de.uka.ipd.idaho.htmlXmlUtil.Parser;
//import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
//import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
//import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
//import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
//import de.uka.ipd.idaho.stringUtils.Dictionary;
//import de.uka.ipd.idaho.stringUtils.StringIterator;
//import de.uka.ipd.idaho.stringUtils.StringUtils;
//
///**
// * @author sautter
// *
// */
//public class CountryHandler implements Dictionary {
//	private static Grammar grammar = new StandardGrammar();
//	private static Parser parser = new Parser(grammar);
//	
//	private TreeMap countryNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//	
//	private CountryHandler() {}
//	private CountryHandler(AnalyzerDataProvider adp) throws IOException {
//		BufferedReader br = new BufferedReader(new InputStreamReader(adp.getInputStream("countries.xml"), "UTF-8"));
//		parser.stream(br, new TokenReceiver() {
//			public void close() throws IOException {}
//			private String country = null;
//			private boolean nextIsCountryName = false;
//			public void storeToken(String token, int treeDepth) throws IOException {
//				if (grammar.isTag(token)) {
//					String type = grammar.getType(token);
//					if ("name".equals(type)) {
//						if (grammar.isEndTag(token))
//							this.nextIsCountryName = false;
//						else if (this.country != null) {
//							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
//							String nCountryName = tnas.getAttribute("normalized");
//							if (nCountryName.indexOf(',') == -1) {
//								countryNames.put(nCountryName, this.country);
//								countryNames.put(normalize(nCountryName), this.country);
//								return;
//							}
//							String[] nCountryNames = nCountryName.split("\\s*\\,\\s*");
//							for (int n = 0; n < nCountryNames.length; n++) {
//								countryNames.put(nCountryNames[n], this.country);
//								countryNames.put(normalize(nCountryNames[n]), this.country);
//							}
//						}
//					}
//					else if ("country".equals(type)) {
//						if (grammar.isEndTag(token))
//							this.country = null;
//						else {
//							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
//							this.country = tnas.getAttribute("name");
//							countryNames.put(this.country, this.country);
//						}
//					}
//				}
//				else if (this.nextIsCountryName) {
//					if (this.country != null) {
//						String unCountryName = grammar.unescape(token.trim());
//						if (unCountryName.indexOf(',') == -1) {
//							countryNames.put(unCountryName, this.country);
//							countryNames.put(normalize(unCountryName), this.country);
//							return;
//						}
//						String[] unCountryNames = unCountryName.split("\\s*\\,\\s*");
//						for (int n = 0; n < unCountryNames.length; n++) {
//							countryNames.put(unCountryNames[n], this.country);
//							countryNames.put(normalize(unCountryNames[n]), this.country);
//						}
//					}
//					this.nextIsCountryName = false;
//				}
//			}
//		});
//		br.close();
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
//	 */
//	public StringIterator getEntryIterator() {
//		final Iterator it = this.countryNames.keySet().iterator();
//		return new StringIterator() {
//			public boolean hasNext() {
//				return it.hasNext();
//			}
//			public boolean hasMoreStrings() {
//				return this.hasNext();
//			}
//			public Object next() {
//				return it.next();
//			}
//			public String nextString() {
//				return ((String) it.next());
//			}
//			public void remove() {}
//		};
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
//	 */
//	public boolean isDefaultCaseSensitive() {
//		return false;
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
//	 */
//	public boolean lookup(String string, boolean caseSensitive) {
//		return this.countryNames.containsKey(string);
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
//	 */
//	public boolean lookup(String string) {
//		return this.lookup(string, this.isDefaultCaseSensitive());
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
//	 */
//	public boolean isEmpty() {
//		return (this.size() != 0);
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
//	 */
//	public int size() {
//		return this.countryNames.size();
//	}
//	
//	/**
//	 * Retrieve the English name of a given country. If there is no English name
//	 * for the argument country, this method returns the argument country name
//	 * itself.
//	 * @param country the country to look up
//	 * @return the English name of the country
//	 */
//	public String getEnglishName(String country) {
//		if (country == null)
//			return null;
//		String eCountry = ((String) this.countryNames.get(country));
//		if (eCountry == null)
//			eCountry = ((String) this.countryNames.get(normalize(country)));
//		return ((eCountry == null) ? country : eCountry);
//	}
//	
//	private static String normalize(String s) {
//		StringBuffer sb = new StringBuffer();
//		for (int c = 0; c < s.length(); c++) {
//			char ch = s.charAt(c);
//			if ((ch > 127) && Character.isLetter(ch))
//				sb.append(StringUtils.getNormalForm(ch));
//			else sb.append(ch);
//		}
//		return sb.toString();
//	}
//	
//	private static HashMap instances = new HashMap();
//	
//	/**
//	 * Initialize a country handler form a data provider. If there already is an
//	 * instance for the argument data provider, this instance is returned
//	 * without a new one being created.
//	 * @param adp the data provider to build on
//	 * @return
//	 */
//	public static CountryHandler getCountryHandler(AnalyzerDataProvider adp) {
//		CountryHandler ch = ((CountryHandler) instances.get(adp));
//		if (ch == null) try {
//			if (adp == null)
//				return null;
//			ch = new CountryHandler(adp);
//			instances.put(adp, ch);
//		}
//		catch (IOException ioe) {
//			System.out.println("Could not initialize country handler: " + ioe.getMessage());
//			ioe.printStackTrace(System.out);
//		}
//		return ch;
//	}
//	
////	//	UN-COMMENT THIS MAIN METHOD FOR TESTING
////	public static void main(String[] args) {
////		CountryHandler ch = CountryHandler.getCountryHandler(new AnalyzerDataProviderFileBased(new File("E:/Projektdaten/SMNK-Projekt/")));
////		StringIterator sit = ch.getEntryIterator();
////		while (sit.hasMoreStrings())
////			System.out.println(sit.nextString());
////		System.out.println(ch.getEnglishName("AllemangnE"));
////	}
//	
////	//	UN-COMMENT THIS MAIN METHOD TO GENERATE COUNTRY MAPPINGS
////	public static void main(String[] args) throws Exception {
////		File path = new File("E:/Projektdaten/SMNK-Projekt");
////		File[] files = path.listFiles(new FileFilter() {
////			public boolean accept(File file) {
////				return (file.isFile() && file.getName().startsWith("CountryNameMappings-"));
////			}
////		});
////		if (files.length == 0)
////			return;
////		
////		final Html html = new Html();
////		final Parser parser = new Parser(html);
////		final HashSet countryNameCatch = new HashSet();
////		countryNameCatch.add("Congo");
////		
//////		Writer w = new OutputStreamWriter(System.out);
////		Writer w = new OutputStreamWriter(new FileOutputStream(new File(path, "countries.xml")), "UTF-8");
////		final BufferedWriter bw = new BufferedWriter(w);
////		bw.write("<countries>");
////		bw.newLine();
////		for (int f = 0; f < files.length; f++) {
////			final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(files[f]), "UTF-8"));
////			parser.stream(br, new TokenReceiver() {
////				private final boolean debugParse = false;
////				public void close() throws IOException {}
////				boolean inData = false;
////				boolean countryToCome = false;
////				String country = null;
////				TreeSet countrySynonyms = new TreeSet();
////				boolean countryNameToCome = false;
////				String countryName = null;
////				TreeMap countryNames = new TreeMap();
////				public void storeToken(String token, int treeDepth) throws IOException {
////					if (this.debugParse) {
////						String t = token.trim();
////						if (t.length() != 0)
////							System.out.println(t);
////					}
////					if (html.isTag(token)) {
////						String type = html.getType(token);
////						if ("table".equals(type)) {
////							if (html.isEndTag(token)) {
////								this.inData = false;
////								this.countryToCome = false;
////								this.country = null;
////								this.countrySynonyms.clear();
////								this.countryNameToCome = false;
////								this.countryName = null;
////								this.countryNames.clear();
////							}
////							else {
////								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
////								String cssClass = tnas.getAttribute("class");
////								if ((cssClass == null) || ((cssClass.indexOf("navbox") == -1) && (cssClass.indexOf("nowraplinks") == -1)))
////									this.inData = true;
////							}
////						}
////						else if (!this.inData)
////							return;
////						else if (html.isEndTag(token)) {
////							if ("td".equals(type) && !this.countryNames.isEmpty()) {
////								if (this.debugParse)
////									System.out.println("==> COUNTRY ENDS");
////								this.printData();
////								this.country = null;
////								this.countrySynonyms.clear();
////								this.countryNames.clear();
////							}
////							return;
////						}
////						else if ("b".equals(type)) {
////							if (this.country != null)
////								this.countryNameToCome = true;
////						}
////						else if ("a".equals(type))
////							this.countryToCome = true;
////					}
////					else if (!this.inData)
////						return;
////					else if (this.countryToCome || countryNameCatch.contains(token.trim())) {
////						if (this.country == null) {
////							String country = IoTools.prepareForPlainText(token).trim();
////							if (country.indexOf('(') == -1)
////								this.country = country;
////							else {
////								this.country = country.substring(0, country.indexOf('(')).trim();
////								this.countrySynonyms.add(country);
////								country = country.substring(country.indexOf('(') + 1);
////								if (country.indexOf(')') != -1)
////									this.countrySynonyms.add(country.substring(0, country.indexOf(')')));
////							}
////							if (this.debugParse)
////								System.out.println("==> COUNTRY: " + this.country);
////						}
////						else {
////							this.countrySynonyms.add(IoTools.prepareForPlainText(token).trim());
////							if (this.debugParse)
////								System.out.println("==> COUNTRY SYNONYM: " + IoTools.prepareForPlainText(token).trim());
////						}
////						this.countryToCome = false;
////					}
////					else if (this.countryNameToCome) {
////						this.countryName = IoTools.prepareForPlainText(token).trim();
////						if (this.debugParse)
////							System.out.println("==> COUNTRY NAME: " + this.countryName);
////						this.countryNameToCome = false;
////					}
////					else if ((this.country != null) && (this.countryName != null)) {
////						String plain = IoTools.prepareForPlainText(token).trim();
////						int obi = plain.indexOf('(');
////						int cbi = plain.indexOf(')');
////						if (cbi <= obi)
////							return;
////						String langs = plain.substring(obi+1, cbi);
////						if (this.countryNames.containsKey(this.countryName))
////							langs = (((String) this.countryNames.get(this.countryName)) + ", " + langs);
////						this.countryNames.put(this.countryName, langs);
////					}
////				}
////				private void printData() throws IOException {
////					bw.write("\t<country name=\"" + this.country + "\">");
////					bw.newLine();
////					for (Iterator cnit = this.countrySynonyms.iterator(); cnit.hasNext();) {
////						String cn = ((String) cnit.next());
////						String cnNorm = this.normalize(cn);
////						String cnEnc = this.encode(cn);
////						bw.write("\t\t<name normalized=\"" + cnNorm + "\" languages=\"English\">" + cnEnc + "</name>");
////						bw.newLine();
////					}
////					for (Iterator cnit = this.countryNames.keySet().iterator(); cnit.hasNext();) {
////						String cn = ((String) cnit.next());
////						String cnNorm = this.normalize(cn);
////						String cnEnc = this.encode(cn);
////						String langs = ((String) this.countryNames.get(cn));
////						bw.write("\t\t<name normalized=\"" + cnNorm + "\" languages=\"" + langs + "\">" + cnEnc + "</name>");
////						bw.newLine();
////					}
////					bw.write("\t</country>");
////					bw.newLine();
////					bw.flush();
////				}
////				private String normalize(String s) {
////					StringBuffer sb = new StringBuffer();
////					for (int c = 0; c < s.length(); c++) {
////						char ch = s.charAt(c);
////						if ((ch > 127) && Character.isLetter(ch))
////							ch = StringUtils.getBaseChar(ch);
////						if (ch < 256)
////							sb.append(ch);
////						else sb.append("&#" + ((int) ch) + ";");
////					}
////					return sb.toString();
////				}
////				private String encode(String s) {
////					StringBuffer sb = new StringBuffer();
////					for (int c = 0; c < s.length(); c++) {
////						char ch = s.charAt(c);
////						if ((ch < 256) || !Character.isLetter(ch))
////							sb.append(ch);
////						else sb.append("&#" + ((int) ch) + ";");
////					}
////					return sb.toString();
////				}
////			});
////			br.close();
////		}
////		bw.write("</countries>");
////		bw.flush();
////		bw.close();
////	}
//}