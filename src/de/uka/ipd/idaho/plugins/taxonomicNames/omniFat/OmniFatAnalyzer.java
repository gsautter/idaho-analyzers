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

package de.uka.ipd.idaho.plugins.taxonomicNames.omniFat;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.TestDocumentProvider;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;

/**
 * @author sautter
 *
 */
public class OmniFatAnalyzer extends AbstractConfigurableAnalyzer {
	
	private static HashMap documentDataSetCachesByBase = new HashMap();
	private static HashMap getDocumentDataSetCache(OmniFAT.Base base) {
		HashMap ddsc = ((HashMap) documentDataSetCachesByBase.get(base));
		if (ddsc == null) {
			ddsc = new HashMap();
			documentDataSetCachesByBase.put(base, ddsc);
		}
		return ddsc;
	}
	
	private static HashMap lastUsedInstanceNamesByBase = new HashMap();
	private static String getLastUsedInstanceName(OmniFAT.Base base) {
		return ((String) lastUsedInstanceNamesByBase.get(base));
	}
	private static void setLastUsedInstanceName(OmniFAT.Base base, String instanceName) {
		lastUsedInstanceNamesByBase.put(base, instanceName);
	}
	
	static void dropInstanceBasedDataSets(OmniFAT.Base base, String instanceName) {
		System.out.println("Cleaning up document data sets for instance '" + instanceName + "'");
		HashMap documentDataSets = getDocumentDataSetCache(base);
		if (documentDataSets == null) {
			System.out.println(" ==> Data set cache not found");
			return;
		}
		for (Iterator ddsit = documentDataSets.values().iterator(); ddsit.hasNext();) {
			OmniFAT.DocumentDataSet dds = ((OmniFAT.DocumentDataSet) ddsit.next());
			if (dds.omniFat.getName().equals(instanceName)) {
				ddsit.remove();
				System.out.println(" ==> Data set removed");
			}
			else System.out.println(" ==> Data set retained");
		}
	}
	
	private OmniFAT.Base omniFatBase;
//	private static HashMap documentDataSets = new HashMap();
//	private static String lastUsedInstanceName;
	
	protected OmniFAT.DocumentDataSet getDataSet(QueriableAnnotation data, Properties parameters) {
//		OmniFAT.DocumentDataSet docData = ((OmniFAT.DocumentDataSet) documentDataSets.get(data.getAnnotationID()));
		HashMap documentDataSets = getDocumentDataSetCache(this.omniFatBase);
		OmniFAT.DocumentDataSet docData = ((OmniFAT.DocumentDataSet) documentDataSets.get(data.getAnnotationID()));
		if (docData == null) {
			String instanceName = parameters.getProperty(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE.substring(1));
			if (instanceName == null)
				instanceName = parameters.getProperty(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE);
			
			if (instanceName == null) {
				Annotation[] omniFatAnnotations = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_TYPE);
				for (int a = 0; (instanceName == null) && (a < omniFatAnnotations.length); a++)
					instanceName = ((String) omniFatAnnotations[a].getAttribute(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE));
			}
			if (instanceName == null) {
				Annotation[] omniFatAnnotations = data.getAnnotations(OmniFAT.TAXONOMIC_NAME_CANDIDATE_TYPE);
				for (int a = 0; (instanceName == null) && (a < omniFatAnnotations.length); a++)
					instanceName = ((String) omniFatAnnotations[a].getAttribute(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE));
			}
			if (instanceName == null)
				instanceName = data.getDocumentProperty(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE);
			
			if ((instanceName == null) && parameters.containsKey(INTERACTIVE_PARAMETER) && FeedbackPanel.isLocal())
//				instanceName = ((String) JOptionPane.showInputDialog(DialogFactory.getTopWindow(), "Please select the OmniFAT instance to use for this document.", "Select OmniFAT Instance", JOptionPane.QUESTION_MESSAGE, null, this.omniFatBase.getInstanceNames(), lastUsedInstanceName));
				instanceName = ((String) JOptionPane.showInputDialog(DialogFactory.getTopWindow(), "Please select the OmniFAT instance to use for this document.", "Select OmniFAT Instance", JOptionPane.QUESTION_MESSAGE, null, this.omniFatBase.getInstanceNames(), getLastUsedInstanceName(this.omniFatBase)));
			
			if ((instanceName != null) && (data instanceof DocumentRoot))
				((DocumentRoot) data).setDocumentProperty(OmniFAT.OMNIFAT_INSTANCE_ATTRIBUTE, instanceName);
			
			if (instanceName != null)
//				lastUsedInstanceName = instanceName;
				setLastUsedInstanceName(this.omniFatBase, instanceName);
			OmniFAT omniFat = ((instanceName == null) ? this.omniFatBase.getDefaultInstance() : this.omniFatBase.getInstance(instanceName));
			docData = new OmniFAT.DocumentDataSet(omniFat, data.getAnnotationID());
			documentDataSets.put(docData.docId, docData);
		}
		return docData;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		this.omniFatBase = OmniFAT.getBase(this.dataProvider);
//		System.out.println("Data Provider is " + this.dataProvider);
//		OmniFAT.setDefaultDataProvider(this.dataProvider);
		this.omniFatBase.setDefaultInstanceName(this.getParameter(OmniFAT.DEFAULT_INSTANCE_NAME_SETTING, omniFatBase.getDefaultInstanceName()));
//		lastUsedInstanceName = this.omniFatBase.getDefaultInstanceName();
		setLastUsedInstanceName(this.omniFatBase, this.omniFatBase.getDefaultInstanceName());
//		lastUsedInstanceName = OmniFAT.getDefaultInstanceName();
	}
	
	/*
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
//		HashSet docDataSets = new HashSet(documentDataSets.values());
//		documentDataSets.clear();
		HashMap documentDataSets = getDocumentDataSetCache(this.omniFatBase);
		HashSet docDataSets = new HashSet(documentDataSets.values());
		documentDataSets.clear();
		for (Iterator ddit = docDataSets.iterator(); ddit.hasNext();) {
			OmniFAT.DocumentDataSet docData = ((OmniFAT.DocumentDataSet) ddit.next());
			docData.omniFat.storeLearnedData();
		}
		this.storeParameter(OmniFAT.DEFAULT_INSTANCE_NAME_SETTING, this.omniFatBase.getDefaultInstanceName());
	}
	
	protected String getKey(Annotation annotation) {
		return (annotation.getType() + "$" + annotation.getStartIndex() + "$" + annotation.size());
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		OmniFAT.doOmniFAT(data, this.getDataSet(data, parameters));
	}
	
//	/*
//	 * @see de.gamta.util.AbstractAnalyzer#configureProcessor()
//	 */
//	public void configureProcessor() {
//		final JDialog configDialog;
//		Window top = DialogFactory.getTopWindow();
//		if (top instanceof Dialog)
//			configDialog = new JDialog(((Dialog) top), "Configure OmniFAT", true);
//		else if (top instanceof Frame)
//			configDialog = new JDialog(((Frame) top), "Configure OmniFAT", true);
//		else configDialog = new JDialog(((Frame) null), "Configure OmniFAT", true);
//		
//		JComboBox defaultInstance = new JComboBox(omniFatBase.getInstanceNames());
//		defaultInstance.setSelectedItem(omniFatBase.getDefaultInstanceName());
//		JPanel defaultInstancePanel = new JPanel(new BorderLayout());
//		defaultInstancePanel.add(new JLabel("Default Instance: "), BorderLayout.WEST);
//		defaultInstancePanel.add(defaultInstance, BorderLayout.CENTER);
//		configDialog.getContentPane().add(defaultInstancePanel, BorderLayout.NORTH);
//		
//		OmniFatEditor ofe = new OmniFatEditor(this.dataProvider, configDialog);
//		configDialog.getContentPane().add(ofe, BorderLayout.CENTER);
//		
//		JButton closeButton = new JButton("Close");
//		closeButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				configDialog.dispose();
//			}
//		});
//		configDialog.getContentPane().add(closeButton, BorderLayout.SOUTH);
//		
//		configDialog.setSize(500, 600);
//		configDialog.setLocationRelativeTo(top);
//		configDialog.setVisible(true);
//		
//		omniFatBase.setDefaultInstanceName(((String) defaultInstance.getSelectedItem()));
//		documentDataSets.clear();
//	}
//	
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		AnalyzerConfigPanel[] acps = {new OmniFatConfigPanel(this.omniFatBase, this.dataProvider, dialog)};
		return acps;
	}
	
	private class OmniFatConfigPanel extends AnalyzerConfigPanel {
		JComboBox defaultInstance;
		OmniFatConfigPanel(OmniFAT.Base base, AnalyzerDataProvider dataProvider, JDialog dialog) {
			super(new DefaultConfigPanelDataProvider(dataProvider), "Configure OmniFAT", "Configure the instances of the OmniFAT taxonomic name recognizer");
			
			this.defaultInstance = new JComboBox(base.getInstanceNames());
			this.defaultInstance.setSelectedItem(base.getDefaultInstanceName());
			JPanel defaultInstancePanel = new JPanel(new BorderLayout());
			defaultInstancePanel.add(new JLabel("Default Instance: "), BorderLayout.WEST);
			defaultInstancePanel.add(this.defaultInstance, BorderLayout.CENTER);
			this.add(defaultInstancePanel, BorderLayout.NORTH);
			
			OmniFatEditor ofe = new OmniFatEditor(this.dataProvider, dialog);
			this.add(ofe, BorderLayout.CENTER);
		}
		public boolean isDirty() {
			return false; // we are never dirty, as the instance editor handles everything
		}
		public boolean commitChanges() throws IOException {
			return false;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		Gamta.addTestDocumentProvider(new TestDocumentProvider() {
			public QueriableAnnotation getTestDocument() {
				MutableAnnotation doc = null;
				try {
					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21330.htm.xml"), "UTF-8"));
//					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/4075_gg1.xml"), "UTF-8"));
//					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/8127_1_gg1.xml"), "UTF-8"));
//					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/Novitates0045/N0045.clean.xml"), "UTF-8"));
//					doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/22777.xml"), "UTF-8"));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				return doc;
			}
		});
		
		AnalyzerDataProvider adp = new AnalyzerDataProviderFileBased(new File("E:/Testdaten/OmniFATData/"));
		OmniFatAnalyzer ofa = new OmniFatAnalyzer();
		ofa.setDataProvider(adp);
		ofa.configureProcessor();
	}
}
