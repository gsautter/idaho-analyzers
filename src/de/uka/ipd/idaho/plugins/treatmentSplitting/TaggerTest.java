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

package de.uka.ipd.idaho.plugins.treatmentSplitting;


import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

public class TaggerTest {
//
//	//	in-treatment div-types according to TaxonX
//	private static final String biology_ecology_SUB_SUB_SECTION_TYPE = "biology_ecology";
//	private static final String description_SUB_SUB_SECTION_TYPE = "description";
//	private static final String diagnosis_SUB_SUB_SECTION_TYPE = "diagnosis";
//	private static final String discussion_SUB_SUB_SECTION_TYPE = "discussion";
//	private static final String distribution_SUB_SUB_SECTION_TYPE = "distribution";
//	private static final String etymology_SUB_SUB_SECTION_TYPE = "etymology";
//	private static final String key_SUB_SUB_SECTION_TYPE = "key";
//	private static final String materials_examined_SUB_SUB_SECTION_TYPE = "materials_examined";
//	private static final String synonymic_list_OPTION = "synonymic_list";
//	
//	//	nomenclature is not a div in TaxonX
//	private static final String nomenclature_SUB_SUB_SECTION_TYPE = "nomenclature";
//	
//	//	reference group is not a div in TaxonX
//	private static final String reference_group_SUB_SUB_SECTION_TYPE = "reference_group";
//	
//	//	multiple div type is not usedin treatments, but is helpful for enlosing artifacts
//	private static final String multiple_SUB_SUB_SECTION_TYPE = "multiple";
//	
//	//	option arrays
//	private static final String[] TREATMENT_SUB_SUB_SECTION_TYPES = {
//			biology_ecology_SUB_SUB_SECTION_TYPE,
//			description_SUB_SUB_SECTION_TYPE,
//			diagnosis_SUB_SUB_SECTION_TYPE,
//			discussion_SUB_SUB_SECTION_TYPE,
//			distribution_SUB_SUB_SECTION_TYPE,
//			etymology_SUB_SUB_SECTION_TYPE,
//			key_SUB_SUB_SECTION_TYPE,
//			materials_examined_SUB_SUB_SECTION_TYPE,
//			nomenclature_SUB_SUB_SECTION_TYPE,
//			reference_group_SUB_SUB_SECTION_TYPE,
//			synonymic_list_OPTION,
//			
//			multiple_SUB_SUB_SECTION_TYPE
//		};
//	
//	private static Color getColorForType(String type) {
//		if (biology_ecology_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.GREEN;
//		else if (description_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.YELLOW;
//		else if (diagnosis_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.PINK;
//		else if (discussion_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.RED;
//		else if (distribution_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.BLUE;
//		else if (etymology_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.MAGENTA;
//		else if (key_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.WHITE;
//		else if (materials_examined_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.CYAN;
//		else if (nomenclature_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.ORANGE;
//		else if (reference_group_SUB_SUB_SECTION_TYPE.equals(type))
//			return Color.LIGHT_GRAY;
//		else if (synonymic_list_OPTION.equals(type))
//			return Color.ORANGE.brighter();
//		else return Color.GRAY;
//	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
//		for (int t = 0; t < TREATMENT_SUB_SUB_SECTION_TYPES.length; t++) {
//			Color c = getColorForType(TREATMENT_SUB_SUB_SECTION_TYPES[t]);
////			System.out.println(TREATMENT_SUB_SUB_SECTION_TYPES[t] + " " + FeedbackPanel.getRGB(c));
//			System.out.println("<span style=\"background-color: " + FeedbackPanel.getRGB(c) + ";\">" + TREATMENT_SUB_SUB_SECTION_TYPES[t] + "</span><br>");
//		}
//		if (true) return;
		
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Projektdaten/TaxonxTest/8538_stumi_malagasy_gg0.xml"));
		AnnotationFilter.removeAnnotations(doc, MutableAnnotation.SUB_SUB_SECTION_TYPE);
		AnnotationFilter.removeAnnotations(doc, "treatment");
		//doc.writeXML(new OutputStreamWriter(System.out));
		TreatmentTagger tds = new TreatmentTagger();
		tds.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv2/Resources/AnalyzerData/TaxonomicDocumentStructurerData/")));
		tds.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}
}
