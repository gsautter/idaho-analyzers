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

package de.uka.ipd.idaho.plugins.mcExporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.materialsCitations.MaterialsCitationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * @author sautter
 */
public class MaterialsCitationExporter extends AbstractConfigurableAnalyzer implements MaterialsCitationConstants, BibRefConstants {
	
	private JFileChooser fileChooser;
	private ArrayList exportFields = new ArrayList();
	private class ExportField {
		String name;
		boolean required;
		ArrayList paths = new ArrayList();
		ExportField(String name, boolean required) {
			this.name = name;
			this.required = required;
		}
	}
	private StringVector requiredFields = new StringVector();
	private StringVector exportKeys = new StringVector();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	create selector for export destination
		this.fileChooser = new JFileChooser("Select Export Destination");
		this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.fileChooser.setApproveButtonText("Export");
		this.fileChooser.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return (file.isDirectory() || file.getName().toLowerCase().endsWith(".csv"));
			}
			public String getDescription() {
				return "CSV files";
			}
		});
		
		//	load export definition
		try {
			BufferedReader configReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("export.cnfg")));
			String line;
			ExportField lastField = null;
			while ((line = configReader.readLine()) != null) {
				line = line.trim();
				if ((line.length() == 0) || line.startsWith("//"))
					continue;
				
				//	dummy field to pad CSV columns
				if (line.indexOf('=') == -1) {
					this.exportFields.add(new ExportField(line, false));
					this.exportKeys.addElement(line);
					lastField = null;
					continue;
				}
				
				//	get field data
				String fieldName = line.substring(0, line.indexOf('=')).trim();
				String path = line.substring(line.indexOf('=')+1).trim();
				boolean fieldRequired = false;
				if (fieldName.startsWith("!")) {
					fieldName = fieldName.substring(1).trim();
					fieldRequired = true;
				}
				
				//	fallback path for last field
				if ((lastField != null) && lastField.name.equals(fieldName))
					lastField.paths.add(path);
				
				//	new field
				else {
					lastField = new ExportField(fieldName, false);
					lastField.paths.add(path);
					this.exportFields.add(lastField);
					this.exportKeys.addElement(fieldName);
				}
				
				//	required field
				if (fieldRequired) {
					lastField.required = true;
					this.requiredFields.addElementIgnoreDuplicates(lastField.name);
				}
			}
			configReader.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get reference data
		MutableAnnotation[] mods = data.getMutableAnnotations("mods:mods");
		if (mods.length == 0) {
			JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), "Materials Citations cannot be exported because the document is lacking bibliographic meta data.", "Lacking Bibliographic Meta Data", JOptionPane.ERROR_MESSAGE);
			return;
		}
		MutableAnnotation ref;
		try {
			String refXml = BibRefUtils.modsXmlToRefData(mods[0]).toXML();
			System.out.println(refXml);
			ref = SgmlDocumentReader.readDocument(new StringReader(refXml));
		}
		catch (IOException ioe) {
			return; // never gonna happen with StringReader, but Java don't know
		}
		
		//	have user select export destination
		//	TODO preset file name to '<docName>.csv' once Gamta update is distributed
		File expDest;
		if (parameters.contains(INTERACTIVE_PARAMETER)) {
			if (this.fileChooser.showSaveDialog(DialogFactory.getTopWindow()) != JFileChooser.APPROVE_OPTION)
				return;
			expDest = this.fileChooser.getSelectedFile();
		}
		else if (parameters.containsKey("exportDestination"))
			expDest = new File(parameters.getProperty("exportDestination"));
		else return;
		
		//	make sure wa have a CSV file
		if (!expDest.getName().toLowerCase().endsWith(".csv"))
			expDest = new File(expDest.getAbsolutePath() + ".csv");
		
		//	make way
		if (expDest.exists()) {
			String expDestName = expDest.getAbsolutePath();
			expDest.renameTo(new File(expDestName + "." + System.currentTimeMillis() + ".old"));
			expDest = new File(expDestName);
		}
		
		//	create file
		try {
			expDest.createNewFile();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("The selected export destination file '" + expDest.getAbsolutePath() + "' could not be created:\n" + ioe.getMessage()), "Could Not Create Export Destination File", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		//	prepare export data structures
		StringRelation export = new StringRelation();
		StringRelation errors = new StringRelation();
		HashSet deDuplicator = new HashSet();
		MutableAnnotation dummyTreatment = Gamta.newDocument(data.getTokenizer());
		
		//	add data from materials citations nested in treatments
		MutableAnnotation[] treatments = data.getMutableAnnotations("treatment");
		for (int t = 0; t < treatments.length; t++) {
			MutableAnnotation[] mcs = treatments[t].getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
			for (int m = 0; m < mcs.length; m++) {
				this.addRecords(data, ref, treatments[t], mcs[m], export, errors);
				deDuplicator.add(mcs[m].getAnnotationID());
			}
		}
		
		//	add top level occurrence records
		MutableAnnotation[] mcs = data.getMutableAnnotations(MATERIALS_CITATION_ANNOTATION_TYPE);
		for (int m = 0; m < mcs.length; m++) {
			if (deDuplicator.add(mcs[m].getAnnotationID()))
				this.addRecords(data, ref, dummyTreatment, mcs[m], export, errors);
		}
		
		//	inform user about errors
		if ((errors.size() != 0) && parameters.containsKey(INTERACTIVE_PARAMETER)) {
			ErrorPanel ep = new ErrorPanel(DialogFactory.produceDialog("Some Materials Citations Lack Required Fields", true), errors);
			ep.dialog.setLocationRelativeTo(DialogFactory.getTopWindow());
			ep.dialog.setVisible(true);
			if ("CE".equals(ep.choice))
				return;
			else if ("EA".equals(ep.choice)) {
				for (int e = 0; e < errors.size(); e++)
					export.addElement(errors.get(e));
			}
		}
		
		//	store string relation semicolon separated, filling in unavailable fields (just need field list, empty values are filled in automatically)
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(expDest)));
			StringRelation.writeCsvData(out, export, ';', '"', this.exportKeys);
			out.flush();
			out.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("The materials citations could not be written to the selected export destination file '" + expDest.getAbsolutePath() + "':\n" + ioe.getMessage()), "Could Not Write Materials Citation", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void addRecords(MutableAnnotation doc, MutableAnnotation ref, MutableAnnotation treatment, MutableAnnotation mc, StringRelation export, StringRelation errors) {
		GPathVariableResolver vars = new GPathVariableResolver();
		GPathAnnotationSet dVar = new GPathAnnotationSet();
		dVar.add(doc);
		vars.setVariable("$doc", dVar);
		GPathAnnotationSet rVar = new GPathAnnotationSet();
		rVar.add(ref);
		vars.setVariable("$ref", rVar);
		GPathAnnotationSet tVar = new GPathAnnotationSet();
		tVar.add(treatment);
		vars.setVariable("$treat", tVar);
		GPathAnnotationSet mcVar = new GPathAnnotationSet();
		mcVar.add(mc);
		vars.setVariable("$mc", mcVar);
		StringTupel st = new StringTupel();
		boolean error = false;
		for (int f = 0; f < this.exportFields.size(); f++) {
			ExportField ef = ((ExportField) this.exportFields.get(f));
			if (ef.paths.size() == 0)
				continue;
			for (int p = 0; p < ef.paths.size(); p++) {
				GPathObject valueObj = GPath.evaluateExpression(((String) ef.paths.get(p)), mc, vars);
				if (valueObj == null)
					continue;
				String value = valueObj.asString().value;
				if ((value != null) && (value.length() != 0)) {
					st.setValue(ef.name, value);
					break;
				}
				//	TODO deal with MCs with multiple specimenCounts in them
			}
			if (ef.required && (st.getValue(ef.name) == null))
				error = true;
		}
		if (error)
			errors.addElement(st);
		else export.addElement(st);
	}
	
	private class ErrorPanel extends JPanel {
		JDialog dialog;
		String choice = "CE";
		ErrorPanel(JDialog d, final StringRelation errors) {
			super(new BorderLayout(), true);
			this.dialog = d;
			
			//	put erroneous MCs into JTable
			JTable table = new JTable(new TableModel() {
				public int getRowCount() {
					return errors.size();
				}
				public int getColumnCount() {
					return requiredFields.size();
				}
				public String getColumnName(int ci) {
					return requiredFields.get(ci);
				}
				public Class getColumnClass(int ci) {
					return String.class;
				}
				public boolean isCellEditable(int ri, int ci) {
					return false;
				}
				public Object getValueAt(int ri, int ci) {
					return errors.get(ri).getValue(this.getColumnName(ci));
				}
				public void setValueAt(Object aValue, int ri, int ci) {}
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			});
			JScrollPane tableBox = new JScrollPane(table);
			this.add(tableBox, BorderLayout.CENTER);
			
			//	add buttons
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton ceb = new JButton("Cancel Export");
			ceb.setPreferredSize(new Dimension(100, 21));
			ceb.setBorder(BorderFactory.createRaisedBevelBorder());
			ceb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					choice = "CE";
					dialog.dispose();
				}
			});
			buttons.add(ceb);
			JButton evb = new JButton("Export Valid");
			evb.setPreferredSize(new Dimension(100, 21));
			evb.setBorder(BorderFactory.createRaisedBevelBorder());
			evb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					choice = "EV";
					dialog.dispose();
				}
			});
			buttons.add(evb);
			JButton aeb = new JButton("Export All");
			aeb.setPreferredSize(new Dimension(100, 21));
			aeb.setBorder(BorderFactory.createRaisedBevelBorder());
			aeb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					choice = "EA";
					dialog.dispose();
				}
			});
			buttons.add(aeb);
			this.add(buttons, BorderLayout.SOUTH);
			
			//	add panel to dialog
			this.dialog.getContentPane().setLayout(new BorderLayout());
			this.dialog.getContentPane().add(this, BorderLayout.CENTER);
			
			//	set dialog size
			int pw = Math.min(table.getPreferredSize().width, 800);
			int ph = Math.min((table.getPreferredSize().height + 21 + 75), 700);
			this.dialog.setSize(new Dimension(pw, ph));
		}
	}
}