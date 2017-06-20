package de.uka.ipd.idaho.plugins.treatmentSplitting;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.ImmutableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.TestDocumentProvider;

public class TreatmentStructurerOnlineTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new FileReader("E:/Projektdaten/TaxonxTest/20286_gg2.xml"));
		//doc.writeXML(new OutputStreamWriter(System.out));
		Gamta.addTestDocumentProvider(new TestDocumentProvider() {
			public QueriableAnnotation getTestDocument() {
				return new ImmutableAnnotation(doc);
			}
		});
		
		//	eliminate subSubSections because they're not part of TaxonX level 1
		MutableAnnotation[] ssss = doc.getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
		for (int d = 0; d < ssss.length; d++)
			doc.removeAnnotation(ssss[d]);
		
		TreatmentStructurerOnline tds = new TreatmentStructurerOnline();
		tds.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/TaxonomicDocumentStructurerData/")));
//		tds.configureProcessor();
		tds.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		tds.process(doc, new Properties() {
//			public synchronized boolean containsKey(Object key) {
//				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
//			}
//		});
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
	}
}