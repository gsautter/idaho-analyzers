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

package de.uka.ipd.idaho.plugins.mcExtender;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * Allows adding attributes from lists to materials citations.
 * 
 * @author sautter
 */
public class MaterialsCitationExtender extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants {
	private static class AttributeList {
		String name;
		String label;
		String group;
		String[] entries;
		String[] attributes;
		HashMap attributeData;
		boolean multiSelect;
		AttributeList(String name, String label, String group, String[] entries, boolean multiSelect) {
			this.name = name;
			this.label = label;
			this.group = group;
			this.entries = entries;
			this.multiSelect = multiSelect;
		}
	}
	
	private String notApplicableOption = "<not applicable>";
	
	private ArrayList attributeLists = new ArrayList();
	private HashMap attributeGroupColors = new HashMap();
	
	
	private static final String EXTENDED_MARKER_ATTRIBUTE = "_extended";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	read 'not applicable' option
		this.notApplicableOption = this.getParameter("notApplicableOption", this.notApplicableOption);
		
		//	read attribute group order
		String attributeGroupOrder = this.getParameter("attributeGroupOrder");
		String[] groupNames = attributeGroupOrder.split("\\s+");
		
		//	read attribute groups
		for (int g = 0; g < groupNames.length; g++)
			this.readAttributeGroup(groupNames[g]);
	}
	
	private void readAttributeGroup(String name) {
		String attributeOrder = this.getParameter("group." + name + ".attributes");
		if (attributeOrder == null)
			return;
		
		String[] attributeNames = attributeOrder.split("\\s+");
		for (int a = 0; a < attributeNames.length; a++)
			this.readAttribute(attributeNames[a], name);
		
		String color = this.getParameter("group." + name + ".color");
		if (color != null)
			this.attributeGroupColors.put(name, FeedbackPanel.getColor(color));
	}
	
	private void readAttribute(String name, String groupName) {
		System.out.println("Loading attribute " + groupName + "." + name);
		String label = this.getParameter("attribute." + name + ".label");
		System.out.println(" - label is " + label);
		if (label == null)
			return;
		String dataFileName = this.getParameter("attribute." + name + ".dataFile");
		System.out.println(" - data file is " + dataFileName);
		if (dataFileName == null)
			return;
		boolean multiSelect = "true".equals(this.getParameter("attribute." + name + ".multiSelect", "false"));
		System.out.println(" - data file is " + dataFileName);
		try {
			StringVector data = new StringVector();
			
			//	load CSV data, possibly with extra attributes 
			if (dataFileName.endsWith(".csv")) {
				String listColumnName = this.getParameter("attribute." + name + ".listColumnName");
				if (listColumnName == null)
					listColumnName = name;
				System.out.println(" - list column is " + listColumnName);
				String attributesString = this.getParameter("attribute." + name + ".attributeColumnNames");
				System.out.println(" - attributes are " + attributesString);
				if (attributesString == null)
					return;
				String[] attributes = attributesString.trim().split("\\s+");
				
				InputStream dis = this.dataProvider.getInputStream(dataFileName);
				StringRelation dataLoader = StringRelation.readCsvData(new InputStreamReader(dis, "UTF-8"), StringRelation.GUESS_SEPARATOR, '"', true, null);
				dis.close();
				if (dataLoader.isEmpty())
					return;
				
				data.clear();
				data.addElement(this.notApplicableOption);
				HashMap attributeData = new HashMap();
				for (int d = 0; d < dataLoader.size(); d++) {
					StringTupel dst = dataLoader.get(d);
					String ds = dst.getValue(listColumnName);
					if ((ds == null) || (ds.trim().length() == 0))
						continue;
					data.addElementIgnoreDuplicates(ds);
					attributeData.put(ds, dst);
				}
				if (data.size() < 2)
					return;
				
				AttributeList al = new AttributeList(name, label, groupName, data.toStringArray(), multiSelect);
				al.attributes = attributes;
				al.attributeData = attributeData;
				this.attributeLists.add(al);
			}
			
			//	load plain TXT data list
			else {
				InputStream dis = this.dataProvider.getInputStream(dataFileName);
				StringVector dataLoader = StringVector.loadList(new InputStreamReader(dis, "UTF-8"));
				dis.close();
				if (dataLoader.isEmpty())
					return;
				
				data.clear();
				data.addElement(this.notApplicableOption);
				for (int d = 0; d < dataLoader.size(); d++) {
					String ds = dataLoader.get(d);
					if ((ds.trim().length() != 0) && !ds.trim().startsWith("//"))
						data.addElementIgnoreDuplicates(ds);
				}
				if (data.size() < 2)
					return;
				
				this.attributeLists.add(new AttributeList(name, label, groupName, data.toStringArray(), multiSelect));
			}
		}
		catch (IOException ioe) {
			System.out.println("Error loading data list for attribute '" + name + "' from '" + dataFileName + "':");
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		//	TODO use config panels
		return super.getConfigPanels(dialog);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	no use doing all the hassle if we cannot ask for feedback
		if (!parameters.containsKey(INTERACTIVE_PARAMETER))
			return;
		
		//	get materials citations
		MutableAnnotation[] materialsCitations = data.getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
		if (materialsCitations.length == 0)
			return;
		
		//	just one materials citation, display it in any case
		if ((materialsCitations.length == 1) && (materialsCitations[0].size() == data.size())) {
			this.processMaterialsCitation(materialsCitations[0], materialsCitations[0], 1, 1);
			return;
		}
		
		//	go one by one
		for (int m = 0; m < materialsCitations.length; m++) {
			
			//	jump materials citations processed before (resumability)
			if (materialsCitations[m].hasAttribute(EXTENDED_MARKER_ATTRIBUTE))
				continue;
			
			String feedback = this.processMaterialsCitation(materialsCitations[m], materialsCitations[(m == 0) ? m : (m-1)], (m+1), materialsCitations.length);
			
			//	dialog cancelled, stop for now
			if ("Cancel".equals(feedback))
				return;
			
			//	step back
			if ("Previous".equals(feedback)) {
				m--; // step back
				materialsCitations[m].removeAttribute(EXTENDED_MARKER_ATTRIBUTE);
				m--; // compensate loop increment
			}
		}
	}
	
	private String processMaterialsCitation(MutableAnnotation materialsCitation, Attributed presets, int number, int total) {
		
		//	create feedback panel
		AssignmentDisambiguationFeedbackPanel adfp = new AssignmentDisambiguationFeedbackPanel("Extend Materials Citation (" + number + " of " + total + ")");
		adfp.setLabel("<html>" +
				"Please select additional attribute values for the materials citation below.<br>" +
				"For attributes that should not be added, select the '" + AnnotationUtils.escapeForXml(this.notApplicableOption, true) + "' option.<br>" +
				"<br>" +
				"<b>" + AnnotationUtils.escapeForXml(TokenSequenceUtils.concatTokens(materialsCitation, true, true)) + "</b><br>" +
				"</html>");
		if (number > 1)
			adfp.addButton("Previous");
		adfp.addButton("Cancel");
		adfp.addButton("OK" + ((number == total) ? "" : " & Next"));
//		adfp.setLineColor(Color.WHITE);
		adfp.setDefaultLineColor(Color.WHITE);
		adfp.setLineSpacing(10);
		adfp.setOptionsLeading(false);
		AttributeList lal = null;
		for (int a = 0; a < this.attributeLists.size(); a++) {
			AttributeList al = ((AttributeList) this.attributeLists.get(a));
			int spacing = (((lal == null) || lal.group.equals(al.group)) ? -1 : 25);
			Color color = ((Color) this.attributeGroupColors.get(al.group));
			if (al.multiSelect)
				adfp.addLine(("<html><b>" + al.label + "</b></html>"), al.entries, ((String) presets.getAttribute(al.name, this.notApplicableOption)).split("\\s*\\&\\s*"), spacing, color);
			else adfp.addLine(("<html><b>" + al.label + "</b></html>"), al.entries, ((String) presets.getAttribute(al.name, this.notApplicableOption)), spacing, color);
			lal = al;
		}
		
		//	set document identifying properties
		adfp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, materialsCitation.getDocumentProperty(LiteratureConstants.DOCUMENT_ID_ATTRIBUTE));
		adfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, materialsCitation.getAnnotationID());
		adfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, MATERIALS_CITATION_ANNOTATION_TYPE);
		
		//	get feedback
		String feedback = adfp.getFeedback();
		if (feedback == null)
			feedback = "Cancel";
		
		//	this one was committed
		if (feedback.startsWith("OK")) {
			
			//	add attributes to materials citation
			for (int a = 0; a < this.attributeLists.size(); a++) {
				AttributeList al = ((AttributeList) this.attributeLists.get(a));
				String value = adfp.getSelectedOptionAt(a);
				
				//	attribute not applicable, or value unclear ==> remove it
				if (this.notApplicableOption.equals(value)) {
					materialsCitation.removeAttribute(al.name);
					if (al.attributes != null) {
						for (int e = 0; e < al.attributes.length; e++)
							materialsCitation.removeAttribute(al.attributes[e]);
					}
					continue;
				}
				
				//	possibly multi-values attribute
				if (al.multiSelect) {
					String[] values = adfp.getSelectedOptionsAt(a);
					StringBuffer valueBuilder = new StringBuffer();
					StringBuffer[] eValueBuilders = ((al.attributes == null) ? null : new StringBuffer[al.attributes.length]);
					for (int v = 0; v < values.length; v++) {
						if (valueBuilder.length() != 0)
							valueBuilder.append(" & ");
						valueBuilder.append(values[v]);
						if (al.attributeData == null)
							continue;
						StringTupel ast = ((StringTupel) al.attributeData.get(value));
						if (ast == null)
							continue;
						for (int e = 0; e < al.attributes.length; e++) {
							String eValue = ast.getValue(al.attributes[e]);
							if (eValue == null)
								continue;
							if (eValueBuilders[e] == null)
								eValueBuilders[e] = new StringBuffer();
							if (eValueBuilders[e].length() != 0)
								eValueBuilders[e].append(" & ");
							eValueBuilders[e].append(eValue);
						}
					}
					materialsCitation.setAttribute(al.name, valueBuilder.toString());
					if (al.attributes == null)
						continue;
					for (int e = 0; e < al.attributes.length; e++) {
						if (eValueBuilders[e] != null)
							materialsCitation.setAttribute(al.attributes[e], eValueBuilders[e].toString());
					}
				}
				
				//	single-valued attribute 
				else {
					materialsCitation.setAttribute(al.name, value);
					if (al.attributeData == null)
						continue;
					StringTupel ast = ((StringTupel) al.attributeData.get(value));
					if (ast == null)
						continue;
					for (int e = 0; e < al.attributes.length; e++) {
						String eValue = ast.getValue(al.attributes[e]);
						if (eValue != null)
							materialsCitation.setAttribute(al.attributes[e], eValue);
					}
				}
			}
			
			//	mark materials citation as processed (for resumability)
			materialsCitation.setAttribute(EXTENDED_MARKER_ATTRIBUTE, "true");
		}
		
		//	return user input
		return feedback;
	}
	
	public static void main(String[] args) throws Exception {
//		Reader srIn = new InputStreamReader(new FileInputStream(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/MaterialsCitationExtenderData/biotopTypes.Germany.filtered.list.csv")), "UTF-8");
//		StringRelation sr = StringRelation.readCsvData(srIn, ';', '"', true, null);
//		srIn.close();
//		for (int b = 0; b < sr.size(); b++) {
//			StringTupel st = sr.get(b);
//			String btc = st.getValue("biotopTypeCode");
//			String bt = st.getValue("biotopType");
//			String bli = btc.replaceAll("[0-9]", "").replaceAll("\\.", "  ");
//			String bl = (bli + bt);
//			st.setValue("biotopTypeLabel", bl);
//		}
//		Writer srOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/MaterialsCitationExtenderData/biotopTypes.Germany.filtered.list2.csv")), "UTF-8"));
//		StringRelation.writeCsvData(srOut, sr, ';', '"', true);
//		srOut.flush();
//		srOut.close();
//		if (true)
//			return;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		MaterialsCitationExtender mce = new MaterialsCitationExtender();
		mce.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/MaterialsCitationExtenderData/")));
		Properties params = new Properties();
		params.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("This is the first MC. This is the second MC."));
		Annotation mc1 = doc.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, 0, 5);
		mc1.setAttribute(EXTENDED_MARKER_ATTRIBUTE, "true");
		mc1.setAttribute("testOne", "Value2");
		doc.addAnnotation(MATERIALS_CITATION_ANNOTATION_TYPE, 6, 5);
		mce.process(doc, params);
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}
}