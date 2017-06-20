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

package de.uka.ipd.idaho.plugins.measures;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.NamedEntityConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CategorizationFeedbackPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Tagger for measurements in taxonomic descriptions, based on classifying
 * quantities.
 * 
 * @author sautter
 */
public class MeasureTagger extends AbstractConfigurableAnalyzer implements LiteratureConstants, NamedEntityConstants {
	
	private class QuantityFilter {
		private class MagnitudeRange {
			int minMagnitude = Integer.MAX_VALUE;
			int maxMagnitude = Integer.MIN_VALUE;
			
			void addMeasure(Measure measure) {
				this.minMagnitude = Math.min(this.minMagnitude, measure.minMagnitude);
				this.maxMagnitude = Math.max(this.maxMagnitude, measure.maxMagnitude);
			}
			
			boolean checkQuantity(Annotation quantity) {
				String mms = ((String) quantity.getAttribute(METRIC_MAGNITUDE_ATTRIBUTE));
				if (mms == null)
					return false;
				try {
					int mm = Integer.parseInt(mms);
					return ((mm >= this.minMagnitude) && (mm <= this.maxMagnitude));
				}
				catch (NumberFormatException nfe) {
					return false;
				}
			}
		}
		
		private HashMap magnitudeRangesByUnit = new HashMap();
		private MagnitudeRange getMagnitudeRange(String unit, boolean create) {
			MagnitudeRange mr = ((MagnitudeRange) this.magnitudeRangesByUnit.get(unit));
			if ((mr == null) && create) {
				mr = new MagnitudeRange();
				this.magnitudeRangesByUnit.put(unit, mr);
			}
			return mr;
		}
		
		void addMeasure(Measure measure) {
			this.getMagnitudeRange(measure.unit, true).addMeasure(measure);
		}
		
		boolean checkQuantity(Annotation quantity) {
			MagnitudeRange mr = this.getMagnitudeRange(((String) quantity.getAttribute(METRIC_UNIT_ATTRIBUTE)), false);
			return ((mr == null) ? false : mr.checkQuantity(quantity));
		}
	}
	
	private class QuantityClassifier {
		
		private class QuantityClass {
			CountingSet leadingHints = new CountingSet();
			CountingSet tailingHints = new CountingSet();
//			String name;
			QuantityClass(String name) {
//				this.name = name;
			}
			void learnHints(Annotation quantity, Annotation context) {
				String lh = this.getLeadingHint(quantity, context);
				if (lh != null)
					this.leadingHints.add(lh);
				
				String th = this.getTailingHint(quantity, context);
				if (th != null)
					this.tailingHints.add(th);
			}
			
			float getScore(Annotation quantity, Annotation context) {
				String lh = this.getLeadingHint(quantity, context);
				float lhs = ((lh == null) ? 0 : (((float) this.leadingHints.getCount(lh)) / this.leadingHints.size()));
				
				String th = this.getTailingHint(quantity, context);
				float ths = ((th == null) ? 0 : (((float) this.tailingHints.getCount(th)) / this.tailingHints.size()));
				
				return (lhs + ths);
			}
			
			private String getLeadingHint(Annotation quantity, Annotation context) {
				int lhi = (quantity.getStartIndex()-1);
				while (lhi >= 0) {
					String lh = context.valueAt(lhi);
					if (Gamta.isWord(lh)) {
						if (leadingHintBridgeableWords.indexOf(lh) != -1)
							lhi--;
						else return lh;
					}
					else if (leadingHintBridgeableNonWords.indexOf(lh) == -1)
						break;
					else lhi--;
				}
				return null;
			}
			
			private String getTailingHint(Annotation quantity, Annotation context) {
				int thi = quantity.getEndIndex();
				while (thi < context.size()) {
					String th = context.valueAt(thi);
					if (Gamta.isWord(th)) {
						if (tailingHintBridgeableWords.indexOf(th) != -1)
							thi++;
						else return th;
					}
					else if (tailingHintBridgeableNonWords.indexOf(th) == -1)
						break;
					else thi++;
				}
				return null;
			}
		}
		
		private HashMap quantityClassesByName = new HashMap();
		private QuantityClass getQuantityClass(String name, boolean create) {
			QuantityClass qc = ((QuantityClass) this.quantityClassesByName.get(name));
			if ((qc == null) && create) {
				qc = new QuantityClass(name);
				this.quantityClassesByName.put(name, qc);
			}
			return qc;
		}
		
		void learnHints(Measure measure, Annotation quantity, Annotation context) {
			this.getQuantityClass(measure.name, true).learnHints(quantity, context);
		}
		
		float getScore(Measure measure, Annotation quantity, Annotation context) {
			if (!measure.unit.equals(quantity.getAttribute(METRIC_UNIT_ATTRIBUTE)))
				return -1;
			
			String mms = ((String) quantity.getAttribute(METRIC_MAGNITUDE_ATTRIBUTE));
			if (mms == null)
				return -1;
			try {
				int mm = Integer.parseInt(mms);
				if (mm < measure.minMagnitude)
					return -1;
				if (mm > measure.maxMagnitude)
					return -1;
			}
			catch (NumberFormatException nfe) {
				return -1;
			}
			
			QuantityClass qc = this.getQuantityClass(measure.name, false);
			return ((qc == null) ? -1 : qc.getScore(quantity, context));
		}
		
		void loadHints(Measure measure) {
			QuantityClass qc = this.getQuantityClass(measure.name, true);
			
			String leadingHintFileName = ("measure." + measure.name + ".leadingHints.txt");
			try {
				fillHintList(qc.leadingHints, leadingHintFileName);
			}
			catch (IOException ioe) {
				System.out.println("Error loading leading hints for measure '" + measure.name + "' from '" + leadingHintFileName + "':");
				ioe.printStackTrace(System.out);
			}
			
			String tailingHintFileName = ("measure." + measure.name + ".tailingHints.txt");
			try {
				fillHintList(qc.tailingHints, tailingHintFileName);
			}
			catch (IOException ioe) {
				System.out.println("Error loading tailing hints for measure '" + measure.name + "' from '" + tailingHintFileName + "':");
				ioe.printStackTrace(System.out);
			}
		}
		
		void storeHints(Measure measure) {
			QuantityClass qc = this.getQuantityClass(measure.name, false);
			if (qc == null)
				return;
			
			String leadingHintFileName = ("measure." + measure.name + ".leadingHints.txt");
			try {
				storeHintList(qc.leadingHints, leadingHintFileName);
			}
			catch (IOException ioe) {
				System.out.println("Error storing leading hints for measure '" + measure.name + "' from '" + leadingHintFileName + "':");
				ioe.printStackTrace(System.out);
			}
			
			String tailingHintFileName = ("measure." + measure.name + ".tailingHints.txt");
			try {
				storeHintList(qc.tailingHints, tailingHintFileName);
			}
			catch (IOException ioe) {
				System.out.println("Error storing tailing hints for measure '" + measure.name + "' from '" + tailingHintFileName + "':");
				ioe.printStackTrace(System.out);
			}
		}
	}
	
	private class Measure {
		String name;
		String label;
		Color color;
		String unit;
		int minMagnitude;
		int maxMagnitude;
		Measure(String name, String label, String unit, int minMagnitude, int maxMagnitude) {
			this.name = name;
			this.label = label;
			this.unit = unit;
			this.minMagnitude = minMagnitude;
			this.maxMagnitude = maxMagnitude;
		}
	}
	
	private String leadingHintBridgeableWords = "of";
	private String leadingHintBridgeableNonWords = ":";
	private String tailingHintBridgeableWords = "in";
	private String tailingHintBridgeableNonWords = "";
	
	private QuantityFilter quantityFilter = new QuantityFilter();
	
	private QuantityClassifier quantityClassifier = new QuantityClassifier();
	
	private HashMap measuresByName = new LinkedHashMap();
	private HashMap measuresByLabel = new LinkedHashMap();
	
	private String noMeasureOption = "<not a relevant measure>";
	private String[] measureOptions = {this.noMeasureOption};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load exclusion option
		this.noMeasureOption = this.getParameter("noMeasureOption", this.noMeasureOption);
		this.measuresByLabel.put(this.noMeasureOption, null);
		
		//	load measures
		String[] pns = this.getParameterNames();
		for (int p = 0; p < pns.length; p++) {
			if (!pns[p].startsWith("measure."))
				continue;
			if (!pns[p].endsWith(".unit"))
				continue;
			String name = pns[p].substring("measure.".length(), (pns[p].length() - ".unit".length()));
			String unit = this.getParameter(pns[p]);
			if (unit == null)
				continue;
			String label = this.getParameter("measure." + name + ".label");
			if (label == null)
				continue;
			String minMagnitudeString = this.getParameter("measure." + name + ".minMagnitude");
			if (minMagnitudeString == null)
				continue;
			String maxMagnitudeString = this.getParameter("measure." + name + ".maxMagnitude");
			if (maxMagnitudeString == null)
				continue;
			
			Measure ms = new Measure(name, label, unit, Integer.parseInt(minMagnitudeString), Integer.parseInt(maxMagnitudeString));
			this.measuresByName.put(ms.name, ms);
			this.measuresByLabel.put(ms.label, ms);
			
			String colorString = this.getParameter("measure." + name + ".color");
			if (colorString != null)
				ms.color = FeedbackPanel.getColor(colorString);
			
			this.quantityFilter.addMeasure(ms);
			
			this.quantityClassifier.loadHints(ms);
		}
		
		//	build option array
		this.measureOptions = new String[this.measuresByLabel.size() + 1];
		int moi = 0;
		this.measureOptions[moi++] = this.noMeasureOption;
		for (Iterator mit = this.measuresByLabel.keySet().iterator(); mit.hasNext();)
			this.measureOptions[moi++] = ((String) mit.next());
	}
	
	private void fillHintList(CountingSet hints, String hintDataName) throws IOException {
		if (!this.dataProvider.isDataAvailable(hintDataName))
			return;
		InputStream hin = this.dataProvider.getInputStream(hintDataName);
		StringVector hss = StringVector.loadList(new InputStreamReader(hin, "UTF-8"));
		hin.close();
		for (int h = 0; h < hss.size(); h++) {
			String hs = hss.get(h).trim();
			if ((hs.length() == 0) || hs.startsWith("//"))
				continue;
			String[] hd = hs.split("\\s*\\|\\s*");
			if (hd.length != 2)
				continue;
			hints.add(hd[0], Integer.parseInt(hd[1]));
		}
		hints.setClean();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		for (Iterator mit = this.measuresByLabel.values().iterator(); mit.hasNext();) {
			Measure ms = ((Measure) mit.next());
			if (ms != null)
				this.quantityClassifier.storeHints(ms);
		}
	}
	
	private void storeHintList(CountingSet hints, String hintDataName) throws IOException {
		if (!hints.isDirty())
			return;
		if (!this.dataProvider.isDataEditable(hintDataName))
			return;
		OutputStream hon = this.dataProvider.getOutputStream(hintDataName);
		BufferedWriter hw = new BufferedWriter(new OutputStreamWriter(hon, "UTF-8"));
		for (Iterator hit = hints.keyIterator(); hit.hasNext();) {
			String hint = ((String) hit.next());
			hw.write(hint + "|" + hints.getCount(hint));
			hw.newLine();
		}
		hw.flush();
		hw.close();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	collect data
		ArrayList dataList = new ArrayList();
		
		//	process 'description' and 'materials_examined' subSubSections
		MutableAnnotation[] ssss = data.getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
		for (int s = 0; s < ssss.length; s++) {
			if ("description".equals(ssss[s].getAttribute(TYPE_ATTRIBUTE)))
				dataList.add(ssss[s]);
			else if ("materials_examined".equals(ssss[s].getAttribute(TYPE_ATTRIBUTE)))
				dataList.add(ssss[s]);
		}
		
		//	process 'materials_methods' subSections containing quantities
		MutableAnnotation[] sss = data.getMutableAnnotations(MutableAnnotation.SUB_SECTION_TYPE);
		for (int s = 0; s < sss.length; s++) {
			if ("materials_methods".equals(sss[s].getAttribute(TYPE_ATTRIBUTE)))
				dataList.add(sss[s]);
		}
		
		//	check for single-section or single-paragraph invokation
		if (dataList.size() == 0) {
			if ((ssss.length == 1) && (ssss[0].size() == data.size()))
				dataList.add(ssss[0]);
			else if ((sss.length == 1) && (sss[0].size() == data.size()))
				dataList.add(sss[0]);
			else {
				MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				if ((paragraphs.length == 1) && (paragraphs[0].size() == data.size()))
					dataList.add(paragraphs[0]);
			}
		}
		
		//	anything to work on?
		if (dataList.size() == 0)
			return;
		
		//	sort out data without interesting quantities
		MutableAnnotation[] datas = ((MutableAnnotation[]) dataList.toArray(new MutableAnnotation[dataList.size()]));
		dataList.clear();
		for (int d = 0; d < datas.length; d++) {
			
			//	get quantities
			Annotation[] quantities = datas[d].getAnnotations(QUANTITY_TYPE);
			if (quantities.length == 0)
				continue;
			
			//	check if interesting quantities present
			for (int q = 0; q < quantities.length; q++)
				if (this.quantityFilter.checkQuantity(quantities[q])) {
					dataList.add(datas[d]);
					break;
				}
		}
		
		//	anything to work on?
		if (dataList.size() == 0)
			return;
		
		//	process single-section or single-paragraph invokation
		if (dataList.size() == 1) {
			data = ((MutableAnnotation) dataList.get(0));
			this.tagMeasures(data, data.getAnnotations(QUANTITY_TYPE), 1, 1, null);
			return;
		}
		
		//	sort data
		datas = ((MutableAnnotation[]) dataList.toArray(new MutableAnnotation[dataList.size()]));
		Arrays.sort(datas, new Comparator() {
			public int compare(Object data1, Object data2) {
				return AnnotationUtils.compare(((Annotation) data1), ((Annotation) data2));
			}
		});
		
		/*
		 * set up local classification (assuming representation of measures is
		 * somewhat consistent throughout a document, a lot more consistent than
		 * across documents, anyway)
		 */
		QuantityClassifier localClassifier = new QuantityClassifier();
		
		//	process data one by one
		boolean jumpClassified = true;
		int firstFeedback = 0;
		int lastFeedback = datas.length;
		for (int d = 0; d < datas.length; d++) {
			
			//	get quantities
			Annotation[] quantities = datas[d].getAnnotations(QUANTITY_TYPE);
			
			//	stimm moving strictly forward
			if (jumpClassified) {
				
				//	check if quantities already classified (for resumability), and train local classifier
				boolean quantitiesClassified = false;
				for (int q = 0; q < quantities.length; q++) {
					Measure ms = ((Measure) this.measuresByName.get(quantities[q].getAttribute(TYPE_ATTRIBUTE)));
					if (ms != null) {
						localClassifier.learnHints(ms, quantities[q], datas[d]);
						quantitiesClassified = true; // only now, as there might be other classifiers that look for different measures
					}
				}
				
				//	this one's done
				if (quantitiesClassified)
					continue;
			}
			
			//	classify quantities into measures
			String feedback = this.tagMeasures(datas[d], quantities, (d+1), datas.length, localClassifier);
			
			//	dialog cancelled, stop for now
			if ("Cancel".equals(feedback)) {
				lastFeedback = d;
				break;
			}
			
			//	step back
			if ("Previous".equals(feedback)) {
				d--; // step back
				jumpClassified = false;
				d--; // compensate loop increment
			}
			
			//	remember where we have feedback from
			else firstFeedback = d;
		}
		
		//	train global model only now (errors might have been corrected via stepping back)
		for (int d = firstFeedback; d < lastFeedback; d++) {
			Annotation[] quantities = datas[d].getAnnotations(QUANTITY_TYPE);
			for (int q = 0; q < quantities.length; q++) {
				Measure ms = ((Measure) this.measuresByName.get(quantities[q].getAttribute(TYPE_ATTRIBUTE)));
				if (ms != null)
					this.quantityClassifier.learnHints(ms, quantities[q], datas[d]);
			}
		}
	}
	
	private String tagMeasures(MutableAnnotation data, Annotation[] quantities, int number, int total, QuantityClassifier localClassifier) {
		
		//	build feedback panel
		CategorizationFeedbackPanel cfp = new CategorizationFeedbackPanel("Identify Relevant Measures (" + number + " of " + total + ")");
		cfp.setLabel("<html>" +
				"For the quantities listed below, please select which measure they represent.<br>" +
				"If a quantity does not represent any of the measures, please select '" + AnnotationUtils.escapeForXml(this.noMeasureOption, true) + "'." +
				"</html>");
		if (number > 1)
			cfp.addButton("Previous");
		cfp.addButton("Cancel");
		cfp.addButton("OK" + ((number == total) ? "" : " & Next"));
		for (int m = 0; m < this.measureOptions.length; m++) {
			cfp.addCategory(this.measureOptions[m]);
			Measure ms = ((Measure) this.measuresByLabel.get(this.measureOptions[m]));
			if ((ms != null) && (ms.color != null))
				cfp.setCategoryColor(this.measureOptions[m], ms.color);
			else cfp.setCategoryColor(this.measureOptions[m], Color.WHITE);
		}
		
		//	TODO set document identifying properties
		
		//	classify quantities and add them to feedback panel
		ArrayList cfpQuantities = new ArrayList();
		for (int q = 0; q < quantities.length; q++) {
			
			//	this one's not interesting
			if (!this.quantityFilter.checkQuantity(quantities[q]))
				continue;
			
			//	get existing classification
			String quantityType = ((String) quantities[q].getAttribute(TYPE_ATTRIBUTE));
			String selectedOption = null;
			if (quantityType != null) {
				Measure ms = ((Measure) this.measuresByName.get(quantityType));
				if (ms != null)
					selectedOption = ms.label;
			}
			
			//	do classification
			if (selectedOption == null) {
				selectedOption = this.noMeasureOption;
				float bestScore = -1;
				for (Iterator mit = this.measuresByName.values().iterator(); mit.hasNext();) {
					Measure measure = ((Measure) mit.next());
					if (measure == null)
						continue;
					float globalScore = this.quantityClassifier.getScore(measure, quantities[q], data);
					if (globalScore > bestScore) {
						bestScore = globalScore;
						selectedOption = measure.label;
					}
					if (localClassifier == null)
						continue;
					float localScore = localClassifier.getScore(measure, quantities[q], data);
					if (localScore > bestScore) {
						bestScore = localScore;
						selectedOption = measure.label;
					}
				}
			}
			
			//	add quantity to feedback panel and store in processing list
			cfpQuantities.add(quantities[q]);
			cfp.addLine(this.buildLabel(quantities[q], data, 10), selectedOption);
		}
		
		//	ask for user feedback
		String feedback = cfp.getFeedback();
		if (feedback == null)
			feedback = "Cancel";
		
		//	cancelled or step back
		if (!feedback.startsWith("OK"))
			return feedback;
		
		//	process feedback
		for (int q = 0; q < cfpQuantities.size(); q++) {
			Annotation qt = ((Annotation) cfpQuantities.get(q));
			Measure ms = ((Measure) this.measuresByLabel.get(cfp.getCategoryAt(q)));
			
			//	this one was excluded
			if (ms == null) {
				qt.removeAttribute(TYPE_ATTRIBUTE);
				continue;
			}
			
			//	add classes to quantities
			qt.setAttribute(TYPE_ATTRIBUTE, ms.name);
			
			//	remember word before and word after each quantity, and assign it to hints for type (only if no number and no paragraph boundary in between)
			((localClassifier == null) ? this.quantityClassifier : localClassifier).learnHints(ms, qt, data);
		}
		
		//	finally
		return feedback;
	}
	
	private String buildLabel(Annotation quantity, TokenSequence context, int envSize) {
		int aStart = quantity.getStartIndex();
		int aEnd = quantity.getEndIndex();
		int start = Math.max(0, (aStart - envSize));
		int end = Math.min(context.size(), (aEnd + envSize));
		StringBuffer sb = new StringBuffer((start == 0) ? "" : "... ");
		Token lastToken = null;
		Token token = null;
		for (int t = start; t < end; t++) {
			lastToken = token;
			token = context.tokenAt(t);
			
			//	end highlighting value
			if (t == aEnd) sb.append("</B>");
			
			//	add spacer
			if ((lastToken != null) && Gamta.insertSpace(lastToken, token)) sb.append(" ");
			
			//	start highlighting value
			if (t == aStart) sb.append("<B>");
			
			//	append token
			sb.append(token);
		}
		
		return ((end == context.size()) ? sb : sb.append(" ...")).toString();
	}
	
	private static class CountingSet {
		private HashMap content = new HashMap();
		private int size = 0;
		private int cleanSize = 0;
		CountingSet() {}
		boolean isDirty() {
			return (this.cleanSize < this.size);
		}
		void setClean() {
			this.cleanSize = this.size;
		}
		Iterator keyIterator() {
			return this.content.keySet().iterator();
		}
		boolean contains(String string) {
			String s = this.getKeyString(string);
			return this.content.containsKey(s);
		}
		int getCount(String string) {
			String s = this.getKeyString(string);
			Int i = ((Int) this.content.get(s));
			return ((i == null) ? 0 : i.intValue());
		}
		boolean add(String string) {
			String s = this.getKeyString(string);
			Int i = ((Int) this.content.get(s));
			this.size++;
			if (i == null) {
				this.content.put(s, new Int(1));
				return true;
			}
			else {
				i.increment();
				return false;
			}
		}
		boolean add(String string, int count) {
			String s = this.getKeyString(string);
			Int i = ((Int) this.content.get(s));
			this.size += count;
			if (i == null) {
				this.content.put(s, new Int(count));
				return true;
			}
			else {
				i.increment(count);
				return false;
			}
		}
		boolean remove(String string) {
			String s = this.getKeyString(string);
			Int i = ((Int) this.content.get(s));
			if (i == null)
				return false;
			this.size--;
			if (i.intValue() > 1) {
				i.decrement();
				return false;
			}
			else {
				this.content.remove(s);
				return true;
			}
		}
		boolean remove(String string, int count) {
			String s = this.getKeyString(string);
			Int i = ((Int) this.content.get(s));
			if (i == null)
				return false;
			if (i.intValue() > count) {
				this.size -= count;
				i.decrement(count);
				return false;
			}
			else {
				this.size -= i.intValue();
				this.content.remove(s);
				return true;
			}
		}
		void removeAll(String string) {
			String s = this.getKeyString(string);
			Int i = ((Int) this.content.get(s));
			if (i != null) {
				this.size -= i.intValue();
				this.content.remove(s);
			}
		}
		void clear() {
			this.content.clear();
			this.size = 0;
		}
		int size() {
			return this.size;
		}
		int distinctSize() {
			return this.content.size();
		}
		private String getKeyString(String string) {
			return string.toLowerCase();
		}
		private class Int {
			private int value;
			Int(int val) {
				this.value = val;
			}
			int intValue() {
				return this.value;
			}
			void increment() {
				this.value ++;
			}
			void increment(int i) {
				this.value += i;
			}
			void decrement() {
				this.value --;
			}
			void decrement(int i) {
				this.value = ((this.value > i) ? (this.value - i) : 0);
			}
		}
	}
}
