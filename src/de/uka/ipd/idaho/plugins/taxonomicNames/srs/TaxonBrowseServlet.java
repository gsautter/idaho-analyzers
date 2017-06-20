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
//package de.uka.ipd.idaho.plugins.taxonomicNames.srs;
//
//import java.io.BufferedWriter;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.Properties;
//import java.util.TreeMap;
//import java.util.TreeSet;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import de.uka.ipd.idaho.goldenGateServer.srs.data.SrsSearchResultElement;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.ThesaurusResult;
//import de.uka.ipd.idaho.goldenGateServer.srs.data.ThesaurusResultElement;
//import de.uka.ipd.idaho.goldenGateServer.srs.webPortal.AbstractSrsWebPortalServlet;
//import de.uka.ipd.idaho.goldenGateServer.srs.webPortal.layoutData.BufferedThesaurusResult;
//import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
//import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.ByteOrderMarkFilterInputStream;
//import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
//import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
//
///**
// * This servlet offers a browsing view on an SRS collection of taxonomic
// * treatments. As it uses path info for control, this servlet must be mapped to
// * any request starting with its invocation path.
// * 
// * @author sautter
// */
//public class TaxonBrowseServlet extends AbstractSrsWebPortalServlet implements TaxonomicNameConstants {
//	
//	private static final String[] NAME_PART_ATTRIBUTE_NAMES = {GENUS_ATTRIBUTE, SUBGENUS_ATTRIBUTE, SPECIES_ATTRIBUTE, SUBSPECIES_ATTRIBUTE, VARIETY_ATTRIBUTE};
//	
//	private String cssName = null;
//	private String srsSearchPath = null;
//	
////	private ItisHigherHierarchyProvider itisHhp;
////	private String itisFamilyAttributeName = "Family";
////	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.goldenGateServer.srs.webPortal.AbstractSrsWebPortalServlet#doInit()
//	 */
//	protected void doInit() throws ServletException {
//		super.doInit();
//		
//		//	get SRS search portal access
//		this.srsSearchPath = this.getSetting("srsSearchPath");
////		
////		//	load rank name mappings
////		this.itisFamilyAttributeName = this.getSetting("familyAttributeName", this.itisFamilyAttributeName);
////		
////		//	initialize ITIS lookup
////		File itisHhpCacheFolder = new File(this.webInfFolder, "caches/taxonBrowse/");
////		itisHhpCacheFolder.mkdirs();
////		this.itisHhp = new ItisHigherHierarchyProvider(new AnalyzerDataProviderFileBased(itisHhpCacheFolder));
//		
//		//	initialize family & genus list
//		try {
//			ThesaurusResult tr = this.getTaxonList(null, null, GENUS_ATTRIBUTE).getThesaurusResult();
//			while (tr.hasNextElement())
//				this.getFamily((String) tr.getNextThesaurusResultElement().getAttribute(GENUS_ATTRIBUTE));
//		}
//		catch (IOException ioe) {}
//	}
//	
////	private HashSet itisLookups = new HashSet();
//	private Properties genusFamilies = new Properties();
//	private TreeMap familyGenera = new TreeMap();
//	
//	private String getFamily(String genus) {
//		if (genus == null)
//			return null;
//		
//		String family = this.genusFamilies.getProperty(genus);
////		if (family == null) {
////			Properties hierarchy = this.itisHhp.getHierarchy(genus, this.itisLookups.add(genus));
////			if (hierarchy == null)
////				return null;
////			
////			family = hierarchy.getProperty(this.itisFamilyAttributeName);
////			if (family != null) {
////				this.genusFamilies.setProperty(genus, family);
////				this.familyGenusCounts.add(family);
////				if (this.familyCollector.add(family))
////					this.families = null;
////			}
////		}
//		
//		return family;
//	}
//	
//	private TreeSet familyCollector = new TreeSet();
////	private String[] families = null;
////	private StringIndex familyGenusCounts = new StringIndex();
//	
//	private String[] getFamilies() throws IOException {
//		return ((String[]) this.familyCollector.toArray(new String[this.familyCollector.size()]));
////		if (this.families == null)
////			this.families = ((String[]) this.familyCollector.toArray(new String[this.familyCollector.size()]));
////		return this.families;
//	}
//	
//	/* (non-Javadoc)
//	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
//	 */
//	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		
//		response.setContentType("text/html");
//		response.setCharacterEncoding(ENCODING);
//		response.setHeader("Cache-Control", "no-cache");
//		
//		//	get requested modul
//		String modul = request.getPathInfo();
//		
//		//	request for main page
//		if (modul == null) {
//			InputStream fis = new ByteOrderMarkFilterInputStream(new FileInputStream(this.findFile("portal.html")));
//			TaxonBrowsePageBuilder tbpb = new TaxonBrowsePageBuilder(request, response);
//			try {
//				htmlParser.stream(fis, tbpb);
//			}
//			catch (Exception e) {
//				e.printStackTrace(System.out);
//				throw new IOException(e.getMessage());
//			}
//			finally {
//				fis.close();
//				tbpb.close();
//			}
//			return;
//		}
//		
//		//	cut modul name
//		while (modul.startsWith("/"))
//			modul = modul.substring(1);
//		
//		final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), ENCODING));
//		
//		//	genus page
//		if (GENUS_ATTRIBUTE.equals(modul)) {
//			String family = request.getParameter(FAMILY_ATTRIBUTE);
//			if (family == null) {
//				out.write("<html><head>");
//				out.newLine();
//				
//				if (this.cssName != null) {
//					String cssUrl = request.getContextPath() + this.dataPath + "/" + cssName;
//					out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//				}
//				
//				out.write("</head><body>");
//				out.newLine();
//				
//				out.write("<div class=\"emptyGenusList\">");
//				out.newLine();
//				
//				out.write("<p class=\"emptyGenusListEntry\">Please select a family to start.</p>");
//				out.newLine();
//				
//				out.write("</div>");
//				out.newLine();
//				
//				out.write("</body></html>");
//				out.newLine();
//			}
//			
//			else {
//				ThesaurusResult tr = this.getTaxonList(null, family, SUBGENUS_ATTRIBUTE).getThesaurusResult();
//				
//				out.write("<html><head>");
//				out.newLine();
//				
//				if (this.cssName != null) {
//					String cssUrl = request.getContextPath() + this.dataPath + "/" + cssName;
//					out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//				}
//				
//				out.write("</head><body>");
//				out.newLine();
//				
//				out.write("<div class=\"genusList\">");
//				out.newLine();
//				
//				String lastGenus = "";
//				int number = 0;
//				while (tr.hasNextElement()) {
//					ThesaurusResultElement tre = tr.getNextThesaurusResultElement();
//					String genus = ((String) tre.getAttribute(GENUS_ATTRIBUTE));
//					if (genus == null)
//						continue;
//					
//					
//					String subGenus = ((String) tre.getAttribute(SUBGENUS_ATTRIBUTE));
//					String linkLabel;
//					String indent;
//					if (lastGenus.equals(genus)) {
//						if (subGenus == null)
//							continue;
//						else {
//							linkLabel = ("(" + IoTools.prepareForHtml(subGenus) + ")");
//							indent = "&nbsp;&nbsp;";
//						}
//					}
//					else {
//						linkLabel = IoTools.prepareForHtml(genus);
//						indent = "";
//						if (subGenus != null) 
//							linkLabel += ("&nbsp;(" + IoTools.prepareForHtml(subGenus) + ")");
//					}
//					
//					out.write("<p class=\"genusListEntry\">");
//					out.newLine();
//					
//					out.write(indent + "<a href=\"#" + number++ + "\" onclick=\"parent.setGenus('" + genus + "', " + ((subGenus == null) ? "null" : ("'" + subGenus + "'")) + "); return false;\">" + linkLabel + "</a>");
//					out.newLine();
//					
//					out.write("</p>");
//					out.newLine();
//					
//					lastGenus = genus;
//				}
//				
//				out.write("</div>");
//				out.newLine();
//				
//				out.write("</body></html>");
//				out.newLine();
//			}
//		}
//		
//		//	species page
//		else if (SPECIES_ATTRIBUTE.equals(modul)) {
//			String genus = request.getParameter(GENUS_ATTRIBUTE);
//			if (genus == null) {
//				out.write("<html><head>");
//				out.newLine();
//				
//				if (this.cssName != null) {
//					String cssUrl = request.getContextPath() + this.dataPath + "/" + cssName;
//					out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//				}
//				
//				out.write("</head><body>");
//				out.newLine();
//				
//				out.write("<div class=\"emptySpeciesList\">");
//				out.newLine();
//				
//				out.write("<p class=\"emptySpeciesListEntry\">Please select a genus to start.</p>");
//				out.newLine();
//				
//				out.write("</div>");
//				out.newLine();
//				
//				out.write("</body></html>");
//				out.newLine();
//			}
//			
//			else {
//				Properties filter = new Properties();
//				filter.setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + "." + GENUS_ATTRIBUTE), genus);
//				
//				String subGenus = request.getParameter(SUBGENUS_ATTRIBUTE);
//				if (subGenus != null)
//					filter.setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + "." + SUBGENUS_ATTRIBUTE), subGenus);
//				
//				ThesaurusResult tr = this.getTaxonList(filter, null, VARIETY_ATTRIBUTE).getThesaurusResult();
//				out.write("<html><head>");
//				out.newLine();
//				
//				if (this.cssName != null) {
//					String cssUrl = request.getContextPath() + this.dataPath + "/" + cssName;
//					out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//				}
//				
//				out.write("</head><body>");
//				out.newLine();
//				
//				out.write("<div class=\"speciesList\">");
//				out.newLine();
//				
//				String lastSpecies = "";
//				String lastSubSpecies = "";
//				int number = 0;
//				while (tr.hasNextElement()) {
//					ThesaurusResultElement tre = tr.getNextThesaurusResultElement();
//					String species = ((String) tre.getAttribute(SPECIES_ATTRIBUTE));
//					if (species == null)
//						continue;
//					
//					String subSpecies = ((String) tre.getAttribute(SUBSPECIES_ATTRIBUTE));
//					String variety = ((String) tre.getAttribute(VARIETY_ATTRIBUTE));
//					String linkLabel;
//					String indent;
//					if (lastSpecies.equals(species)) {
//						if ((subSpecies == null) || lastSubSpecies.equals(subSpecies)) {
//							if (variety == null)
//								continue;
//							else {
//								linkLabel = ("var.&nbsp;" + IoTools.prepareForHtml(variety));
//								indent = "&nbsp;&nbsp;&nbsp;&nbsp;";
//							}
//						}
//						else {
//							linkLabel = ("subsp.&nbsp;" + IoTools.prepareForHtml(subSpecies));
//							indent = "&nbsp;&nbsp;";
//							if (variety != null)
//								linkLabel += ("&nbsp;var.&nbsp;" + IoTools.prepareForHtml(variety));
//						}
//					}
//					else {
//						linkLabel = IoTools.prepareForHtml(species);
//						indent = "";
//						
//						if (subSpecies != null)
//							linkLabel += ("&nbsp;subsp.&nbsp;" + IoTools.prepareForHtml(subSpecies));
//						
//						if (variety != null)
//							linkLabel += ("&nbsp;var.&nbsp;" + IoTools.prepareForHtml(variety));
//					}
//					
//					out.write("<p class=\"speciesListEntry\">");
//					out.newLine();
//					
//					out.write(indent + "<a href=\"" + number++ + "\" onclick=\"parent.showTreatments('" + genus + "', " + ((subGenus == null) ? "null" : ("'" + subGenus + "'")) + ", '" + species + "', " + ((subSpecies == null) ? "null" : ("'" + subSpecies + "'")) + ", " + ((variety == null) ? "null" : ("'" + variety + "'")) + "); return false;\">" + linkLabel + "</a>");
//					out.newLine();
//					
//					out.write("</p>");
//					out.newLine();
//					
//					lastSpecies = species;
//					lastSubSpecies = ((subSpecies == null) ? "" : subSpecies);
//				}
//				
//				out.write("</div>");
//				out.newLine();
//				
//				out.write("</body></html>");
//				out.newLine();
//			}
//		}
//		
//		//	treatment list page
//		else {
//			StringBuffer query = new StringBuffer();
//			query.append(TAXONOMIC_NAME_ANNOTATION_TYPE + ".isNomenclature=isNomenclature");
//			query.append("&" + TAXONOMIC_NAME_ANNOTATION_TYPE + ".exactMatch=exactMatch");
//			boolean gotQuery = false;
//			for (int p = 0; p < NAME_PART_ATTRIBUTE_NAMES.length; p++) {
//				String part = request.getParameter(NAME_PART_ATTRIBUTE_NAMES[p]);
//				if (part != null) {
//					query.append("&" + TAXONOMIC_NAME_ANNOTATION_TYPE + "." + NAME_PART_ATTRIBUTE_NAMES[p] + "=" + URLEncoder.encode(part, "UTF-8"));
//					gotQuery = true;
//				}
//			}
//			
//			if (gotQuery) {
//				out.write("<html>");
//				out.newLine();
//				
//				String srsSearchPath = this.srsSearchPath;
//				if (srsSearchPath == null)
//					srsSearchPath = ("http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/search?");
//				
//				TokenReceiver tr = new TokenReceiver() {
//					boolean inHead = false;
//					boolean inData = false;
//					int openTables = 0;
//					String bodyStart = "<body>";
//					public void close() throws IOException {}
//					public void storeToken(String token, int treeDepth) throws IOException {
//						if (html.isTag(token)) {
//							String type = html.getType(token);
//							if ("table".equalsIgnoreCase(type)) {
//								if (html.isEndTag(token)) {
//									if (this.inData) {
//										out.write(token);
//										this.openTables--;
//									}
//									
//									if (this.openTables == 0) {
//										out.write("</div>");
//										out.newLine();
//										out.write("</body>");
//										out.newLine();
//										this.inData = false;
//									}
//								}
//								else {
//									TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
//									if ("mainTable".equals(tnas.getAttribute("class"))) {
//										out.write(this.bodyStart);
//										out.newLine();
//										out.write("<div class=\"treatmentList\">");
//										out.newLine();
//										this.inData = true;
//									}
//									
//									if (this.inData) {
//										out.write(token);
//										this.openTables++;
//									}
//								}
//							}
//							else if ("head".equalsIgnoreCase(type)) {
//								if (html.isEndTag(token)) {
//									out.write(token);
//									this.inHead = false;
//								}
//								else {
//									out.write(token);
//									if (cssName != null) {
//										String cssUrl = request.getContextPath() + dataPath + "/" + cssName;
//										out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//									}
//									this.inHead = true;
//								}
//							}
//							else if ("body".equalsIgnoreCase(type)) {
//								if (!html.isEndTag(token))
//									this.bodyStart = token;
//							}
//							else if (this.inData) {
//								if ("a".equalsIgnoreCase(type) && !html.isEndTag(token)) {
//									TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
//									if (tnas.getAttribute("target") == null)
//										token = ("<a target=\"_blank\"" + token.substring(2));
//								}
//								
//								out.write(token);
//							}
//							else if (this.inHead)
//								out.write(token);
//						}
//						else if (this.inData || this.inHead)
//							out.write(token);
//					}
//				};
//				try {
//					htmlParser.stream(new InputStreamReader((new URL(srsSearchPath + query)).openStream(), "UTF-8"), tr);
//				}
//				catch (Exception e) {
//					e.printStackTrace(System.out);
//					throw new IOException(e.getMessage());
//				}
//				
//				out.write("</html>");
//				out.newLine();
//			}
//			
//			else {
//				out.write("<html><head>");
//				out.newLine();
//				
//				if (this.cssName != null) {
//					String cssUrl = request.getContextPath() + this.dataPath + "/" + cssName;
//					out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//				}
//				
//				out.write("</head><body>");
//				out.newLine();
//				
//				out.write("<div class=\"emptyTreatmentList\">");
//				out.newLine();
//				
//				out.write("<p class=\"emptyTreatmentListEntry\">Please select a genus to start.</p>");
//				out.newLine();
//				
//				out.write("</div>");
//				out.newLine();
//				
//				out.write("</body></html>");
//				out.newLine();
//			}
//		}
//		
//		out.flush();
//		out.close();
//	}
//	
//	/**
//	 * Builder object for HTML pages. This class wraps the output stream of an
//	 * HttpServletResponse. If a servlet wants to set response properties, it
//	 * has to do so before wrapping the response object in an HtmlPageBuidler.
//	 * 
//	 * @author sautter
//	 */
//	private class TaxonBrowsePageBuilder extends HtmlPageBuilder {
//		private String family;
//		private String genus;
//		
//		TaxonBrowsePageBuilder(HttpServletRequest request, HttpServletResponse response) throws IOException {
//			super(TaxonBrowseServlet.this, request, response);
//			this.family = request.getParameter(FAMILY_ATTRIBUTE);
//			this.genus = request.getParameter(GENUS_ATTRIBUTE);
//			if (this.genus != null)
//				this.family = getFamily(this.genus);
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder#include(java.lang.String, java.lang.String)
//		 */
//		protected void include(String type, String tag) throws IOException {
//			if ("includeBody".equals(type)) {
//				this.writeLine("<table width=\"100%\" class=\"taxonBrowseTable\">");
//				this.writeLine("<tr>");
//				this.writeLine("<td id=\"familyDisplay\">");
//				this.writeLine("<select id=\"family\" onchange=\"setFamily(document.getElementById('family').value);\">");
//				this.writeLine("<option value=\"\">&lt;Select a Family to Start&gt;</option>");
//				String[] families = getFamilies();
//				for (int f = 0; f < families.length; f++) {
//					TreeSet genera = ((TreeSet) familyGenera.get(families[f]));
//					this.writeLine("<option value=\"" + families[f] + "\"" + (families[f].equals(this.family) ? " selected=\"selected\"" : "") + ">" + families[f] + ((genera == null) ? "" : (" (" + genera.size() + " genera)")) + "</option>");
//				}
//				this.writeLine("</select>");
//				this.writeLine("</td>"); 
//				this.writeLine("<td id=\"genusDisplay\">Select a genus</td>");
//				this.writeLine("<td id=\"speciesDisplay\">Select a genus</td>");
//				this.writeLine("</tr>");
//				this.writeLine("<tr>");
//				this.writeLine("<td class=\"genusTableCell\"><iframe id=\"genus\" height=\"100%\" width=\"100%\" src=\"" + this.request.getContextPath() + this.request.getServletPath() + "/genus\">This page requires an iframe capable browser to work.</iframe></td>"); 
//				this.writeLine("<td class=\"speciesTableCell\"><iframe id=\"species\" height=\"100%\" width=\"100%\" src=\"" + this.request.getContextPath() + this.request.getServletPath() + "/species\">This page requires an iframe capable browser to work.</iframe></td>");
//				this.writeLine("<td class=\"treatmentTableCell\"><iframe id=\"treatments\" height=\"100%\" width=\"100%\" src=\"" + this.request.getContextPath() + this.request.getServletPath() + "/treatments\">This page requires an iframe capable browser to work.</iframe></td>");
//				this.writeLine("</tr>");
//				this.writeLine("</table>");
//			}
//			else super.include(type, tag);
//		}
//		
//		/* (non-Javadoc)
//		 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder#getOnloadCalls()
//		 */
//		protected String[] getOnloadCalls() {
//			String[] olcs;
//			if (this.genus != null) {
//				olcs = new String[2];
//				olcs[0] = ("  setFamily('" + this.family + "');");
//				olcs[1] = ("  setGenus('" + this.genus + "');");
//			}
//			else if (this.family != null) {
//				olcs = new String[1];
//				olcs[0] = ("  setFamily('" + this.family + "');");
//			}
//			else olcs = new String[0];
//			return olcs;
//		}
//		
//		protected void writePageHeadExtensions() throws IOException {
//			
//			//	include JavaScript
//			this.writeLine("<script type=\"text/javascript\">");
//			this.writeLine("function setFamily(family) {");
//			this.writeLine("  var genus = document.getElementById('genus');");
//			this.writeLine("  genus.src = '" + this.request.getContextPath() + this.request.getServletPath() + "/genus?' + 'family=' + family;");
//			this.writeLine("}");
//			this.writeLine("function setGenus(genus, subGenus) {");
//			this.writeLine("  var species = document.getElementById('species');");
//			this.writeLine("  species.src = '" + this.request.getContextPath() + this.request.getServletPath() + "/species?' + 'genus=' + genus + (subGenus ? ('&subGenus=' + subGenus) : '');");
//			this.writeLine("  var genusDisplay = document.getElementById('genusDisplay');");
//			this.writeLine("  genusDisplay.innerHTML = genus + (subGenus ? ('&nbsp;(' + subGenus + ')') : '');");
//			this.writeLine("  showTreatments(genus, subGenus, null, null, null)");
//			this.writeLine("}");
//			this.writeLine("function showTreatments(genus, subGenus, species, subSpecies, variety) {");
//			this.writeLine("  var treatments = document.getElementById('treatments');");
//			this.writeLine("  var speciesDisplay = document.getElementById('speciesDisplay');");
//			this.writeLine("  speciesDisplay.innerHTML = genus + (subGenus ? ('&nbsp;(' + subGenus + ')') : '') + (species ? ('&nbsp;' + species) : '') + (subSpecies ? ('&nbsp;subsp.&nbsp;' + subSpecies) : '') + (variety ? ('&nbsp;var.&nbsp;' + variety) : '');");
//			this.writeLine("  treatments.src = '" + this.request.getContextPath() + this.request.getServletPath() + "/treatments?' + 'genus=' + genus + (subGenus ? ('&subGenus=' + subGenus) : '') + (species ? ('&species=' + species) : '') + (subSpecies ? ('&subSpecies=' + subSpecies) : '') + (variety ? ('&variety=' + variety) : '');");
//			this.writeLine("}");
//			this.writeLine("</script>");
//		}
//	}
//	
//	private BufferedThesaurusResult getTaxonList(final String family, final String maxRankName) throws IOException {
//		Properties filter = new Properties();
//		final LinkedList btrParts = new LinkedList();
//		ThesaurusResult modelTr = null;
//		for (int i = 'A'; i <= 'Z'; i++) {
//			filter.setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + "." + GENUS_ATTRIBUTE), ("" + ((char) i)));
//			BufferedThesaurusResult btrPart = this.getTaxonList(filter, family, maxRankName);
//			btrParts.add(btrPart);
//			if (modelTr == null)
//				modelTr = btrPart.getThesaurusResult();
//		}
//		ThesaurusResult filteredTr = new ThesaurusResult(modelTr.resultAttributes, modelTr.thesaurusEntryType, modelTr.thesaurusName) {
//			private ThesaurusResult rawTr = null;
//			private ThesaurusResultElement next = null;
//			private HashSet added = new HashSet();
//			public boolean hasNextElement() {
//				if (this.rawTr == null) {
//					if (btrParts.isEmpty())
//						return false;
//					this.rawTr = ((BufferedThesaurusResult) btrParts.removeFirst()).getThesaurusResult();
//				}
//				while ((this.next == null) && this.rawTr.hasNextElement()) {
//					ThesaurusResultElement tre = this.rawTr.getNextThesaurusResultElement();
//					if (this.added.add(getKeyString(tre, maxRankName)) && ((family == null) || family.equals(getFamily((String) tre.getAttribute(GENUS_ATTRIBUTE)))))
//						this.next = tre;
//				}
//				if (this.next == null) {
//					this.rawTr = null;
//					return this.hasNextElement();
//				}
//				return (this.next != null);
//			}
//			public SrsSearchResultElement getNextElement() {
//				if (this.hasNextElement()) {
//					ThesaurusResultElement tre = this.next;
//					this.next = null;
//					return tre;
//				}
//				else return null;
//			}
//		};
//		BufferedThesaurusResult btr = new BufferedThesaurusResult(filteredTr);
//		btr.sort();
//		return btr;
//	}
//	
//	private BufferedThesaurusResult getTaxonList(Properties filter, final String family, final String maxRankName) throws IOException {
//		if (filter == null) {
//			return this.getTaxonList(family, maxRankName);
////			filter = new Properties();
////			filter.setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + "." + GENUS_ATTRIBUTE), "%%%");
//		}
//		filter.setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + ".isNomenclature"), "isNomenclature");
//		filter.setProperty((TAXONOMIC_NAME_ANNOTATION_TYPE + ".exactMatch"), "exactMatch");
//		System.out.println(filter);
//		
//		final ThesaurusResult rawTr = this.srsClient.searchThesaurus(filter);
//		ThesaurusResult filteredTr = new ThesaurusResult(rawTr.resultAttributes, rawTr.thesaurusEntryType, rawTr.thesaurusName) {
//			private ThesaurusResultElement next = null;
//			private HashSet added = new HashSet();
//			public boolean hasNextElement() {
//				while ((this.next == null) && rawTr.hasNextElement()) {
//					ThesaurusResultElement tre = rawTr.getNextThesaurusResultElement();
//					String treFamily = ((String) tre.getAttribute(FAMILY_ATTRIBUTE));
//					String treGenus = ((String) tre.getAttribute(GENUS_ATTRIBUTE));
//					if (treFamily != null) {
//						familyCollector.add(treFamily);
//						if (treGenus != null) {
//							genusFamilies.setProperty(treGenus, treFamily);
//							TreeSet treFamilyGenera = ((TreeSet) familyGenera.get(treFamily));
//							if (treFamilyGenera == null) {
//								treFamilyGenera = new TreeSet();
//								familyGenera.put(treFamily, treFamilyGenera);
//							}
//							treFamilyGenera.add(treGenus);
//						}
//					}
//					if (this.added.add(getKeyString(tre, maxRankName)) && ((family == null) || family.equals(getFamily((String) tre.getAttribute(GENUS_ATTRIBUTE)))))
//						this.next = tre;
//				}
//				return (this.next != null);
//			}
//			public SrsSearchResultElement getNextElement() {
//				if (this.hasNextElement()) {
//					ThesaurusResultElement tre = this.next;
//					this.next = null;
//					return tre;
//				}
//				else return null;
//			}
//		};
//		BufferedThesaurusResult btr = new BufferedThesaurusResult(filteredTr);
//		btr.sort();
//		return btr;
//	}
//	
//	private String getKeyString(ThesaurusResultElement tre, String rankName) {
//		StringBuffer keyString = new StringBuffer();
//		for (int p = 0; p < NAME_PART_ATTRIBUTE_NAMES.length; p++) {
//			String part = ((String) tre.getAttribute(NAME_PART_ATTRIBUTE_NAMES[p]));
//			if (part != null) {
//				if (p != 0)
//					keyString.append(' ');
//				keyString.append(part);
//			}
//			if (NAME_PART_ATTRIBUTE_NAMES[p].equals(rankName))
//				return keyString.toString();
//		}
//		return keyString.toString();
//	}
////	
////	public static void main(String[] args) throws Exception {
////		TaxonBrowseServlet tbs = new TaxonBrowseServlet();
////		tbs.rootFolder = new File("E:/GoldenGATEv3.WebApp/");
////		tbs.webInfFolder = new File("E:/GoldenGATEv3.WebApp/WEB-INF");
////		tbs.dataFolder = new File("E:/GoldenGATEv3.WebApp/WEB-INF/taxonBrowseData");
////		tbs.srsClient = new GoldenGateSrsClient(ServerConnection.getServerConnection("http://plazi.cs.umb.edu/GgServer/proxy"));
////		File itisHhpCacheFolder = new File(tbs.webInfFolder, "caches/taxonBrowse/");
////		itisHhpCacheFolder.mkdirs();
////		tbs.itisHhp = new ItisHigherHierarchyProvider(new AnalyzerDataProviderFileBased(itisHhpCacheFolder));
////		ThesaurusResult tr = tbs.getTaxonList(null, null, GENUS_ATTRIBUTE).getThesaurusResult();
////		while (tr.hasNextElement()) {
////			String genus = ((String) tr.getNextThesaurusResultElement().getAttribute(GENUS_ATTRIBUTE));
////			System.out.println("Doing genus '" + genus + "'");
////			tbs.getFamily(genus);
////		}
////	}
//}
