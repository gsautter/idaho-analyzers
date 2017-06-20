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

package de.uka.ipd.idaho.plugins.quantityTagger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JDialog;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.constants.NamedEntityConstants;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * @author sautter
 *
 */
public class QuantityTagger extends AbstractConfigurableAnalyzer implements NamedEntityConstants {
	
	/* TODOne add quantity ranges:
	 * - match things like '5 mm'
	 * - match things like '3-4 cm'
	 *   - match number ranges in addition to numbers
	 *   - use 'value' + 'valueMin'/'valueMax' attributes, plus respective ones for magnitude
	 * - match things like '8 +/- 1 cm'
	 *   - match as number range in center + radius notation
	 *   - compute boundaries accordingly
	 *   - then handle as above
	 * - match things like '4 cm +/- 5 mm'
	 *   - tag quantities like now
	 *   - join adjacent quantities if
	 *     - metric unit matches
	 *     - metric magnitude of of first greater or equal than of second
	 *     - only '+/-' in between
	 *   - convert into 'value' + 'valueMin'/'valueMax' attributes, plus respective ones for magnitude
	 */
	
	private static final boolean DEBUG = true;
	
	private static final String DECIMAL_DOT_NUMBER_REG_EX = "(" +
				"(" +
					"([1-9][0-9]*(\\,\\s?[0-9]{3})*)" +
					"|" +
					"0" +
				")" +
				"(\\.\\s?[0-9]*[1-9]0?)?" +
			")";
	private static final String DECIMAL_COMMA_NUMBER_REG_EX = "(" +
				"(" +
					"([1-9][0-9]*(\\.\\s?[0-9]{3})*)" +
					"|" +
					"0" +
				")" +
				"(\\,\\s?[0-9]*[1-9]0?)?" +
			")";
	private static final String NUMBER_RANGE_INFIX_REG_EX = "(" +
				"(\\-+|[\\u2012-\\u2015])" +
				"|" +
				"((\\+\\s*\\/\\s*\\-)|\\u00B1)" +
				"|" +
				"(to|bis|a)" +
			")";
	
	private HashMap units = new HashMap();
	private Dictionary unitDictionary = new Dictionary() {
		public boolean isDefaultCaseSensitive() {
			return true;
		}
		public boolean lookup(String string) {
			return this.lookup(string, true);
		}
		public boolean lookup(String string, boolean caseSensitive) {
			String str = (caseSensitive ? string : string.toLowerCase());
			return QuantityTagger.this.units.containsKey(str);
		}
		public boolean isEmpty() {
			return QuantityTagger.this.units.isEmpty();
		}
		public int size() {
			return QuantityTagger.this.units.size();
		}
		public StringIterator getEntryIterator() {
			final Iterator uit = QuantityTagger.this.units.keySet().iterator();
			return new StringIterator() {
				public boolean hasNext() {
					return uit.hasNext();
				}
				public boolean hasMoreStrings() {
					return uit.hasNext();
				}
				public Object next() {
					return uit.next();
				}
				public String nextString() {
					return ((String) uit.next());
				}
				public void remove() {}
			};
		}
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			InputStream is = this.dataProvider.getInputStream("Units.csv");
			StringRelation units = StringRelation.readCsvData(new InputStreamReader(is, "UTF-8"), ';', '"');
			is.close();
			for (int u = 0; u < units.size(); u++) {
				StringTupel unitData = units.get(u);
				String symbol = unitData.getValue(Unit.UNIT_SYMBOL_ATTRIBUTE);
				if (symbol == null)
					continue;
				System.out.println("Got unit '" + symbol + "'");
				String metricSymbol = unitData.getValue(Unit.METRIC_UNIT_ATTRIBUTE);
				if (metricSymbol == null) {
					System.out.println(" ==> metric unit not found");
					continue;
				}
				
				String synonymString = unitData.getValue(Unit.SYNONYMS_ATTRIBUTE);
				String[] synonyms = (((synonymString == null) || (synonymString.trim().length() == 0)) ? new String[0] : synonymString.trim().split("\\s*\\,\\s*"));
				
				String ftmString = unitData.getValue(Unit.FACTOR_TO_METRIC_ATTRIBUTE);
				if (ftmString == null) {
					System.out.println(" ==> metric conversion factor not found");
					continue;
				}
				String mtmString = unitData.getValue(Unit.MAGNITUDES_TO_METRIC_ATTRIBUTE);
				if (mtmString == null) {
					System.out.println(" ==> metric conversion magnitudes not found");
					continue;
				}
				
				try {
					double ftm = Double.parseDouble(ftmString.replaceAll("\\,", "."));
					int mtm = Integer.parseInt(mtmString);
					while (ftm >= 10) {
						ftm /= 10;
						mtm++;
					}
					while (ftm < 1) {
						ftm *= 10;
						mtm--;
					}
					Unit uc = new Unit(symbol, metricSymbol, ftm, mtm);
					this.units.put(uc.symbol, uc);
					StringBuffer synonymMessage = new StringBuffer(", synonyms are ");
					for (int s = 0; s < synonyms.length; s++) {
						this.units.put(synonyms[s], uc);
						synonymMessage.append(((s == 0) ? "" : ", ") + synonyms[s]);
						this.units.put(synonyms[s].toLowerCase(), uc);
						synonymMessage.append(", " + synonyms[s].toLowerCase());
					}
					System.out.println(" ==> stored" + ((synonyms.length == 0) ? "" : synonymMessage.toString()));
				}
				catch (NumberFormatException nfe) {
					System.out.println(" ==> could not parse data: " + nfe.getMessage());
				}
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#customizeConfigDialog(javax.swing.JDialog)
	 */
	protected void customizeConfigDialog(JDialog dialog) {
		// TODO Auto-generated method stub
		super.customizeConfigDialog(dialog);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		// TODO Auto-generated method stub
		return super.getConfigPanels(dialog);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#exitAnalyzer()
	 */
	public void exitAnalyzer() {
		// TODO store units and respective data if modified
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	extract numbers
		Annotation[] numbers = this.getNumbers(data);
		int ddCount = 0;
		int dcCount = 0;
		for (int n = 0; n < numbers.length; n++) {
			if (DEBUG) System.out.println("Checking " + TokenSequenceUtils.concatTokens(numbers[n], true, true));
			if ("dc".equals(numbers[n].getAttribute("style")))
				dcCount++;
			else if ("dd".equals(numbers[n].getAttribute("style")))
				ddCount++;
			numbers[n].removeAttribute("style");
		}
		boolean punctuationInverse = (ddCount < dcCount);
		if (DEBUG) {
			if (punctuationInverse)
				System.out.println("Using inverted punctuation scheme (dots grouping digits, decimal comma)");
			else System.out.println("Using regular punctuation scheme (commas grouping digits, decimal dot)");
		}
		
		//	normalize numbers, sort out unparsable ones, and index the others
		ArrayList numberList = new ArrayList();
		HashMap numbersByStartIndex = new HashMap();
		HashMap numbersByEndIndex = new HashMap();
		for (int n = 0; n < numbers.length; n++) {
			if (DEBUG) System.out.println("Parsing " + TokenSequenceUtils.concatTokens(numbers[n], true, true));
			String valueString = TokenSequenceUtils.concatTokens(numbers[n], true, true);
			valueString = valueString.replaceAll("\\s+", "");
			if (DEBUG) System.out.println(" - value string is " + valueString);
			int tsIndex = valueString.lastIndexOf(punctuationInverse ? '.' : ',');
			int dsIndex = valueString.indexOf(punctuationInverse ? ',' : '.');
			if (dsIndex == -1)
				dsIndex = valueString.length();
			if ((tsIndex != -1) && (dsIndex - tsIndex != 4)) {
				if (DEBUG) System.out.println(" ==> invalid thousands-separator position");
				continue;
			}
			if (punctuationInverse) {
				StringBuffer normalizedValueString = new StringBuffer();
				for (int c = 0; c < valueString.length(); c++) {
					char ch = valueString.charAt(c);
					if (ch == ',')
						normalizedValueString.append('.');
					else if (ch == '.')
						normalizedValueString.append(',');
					else normalizedValueString.append(ch);
				}
				valueString = normalizedValueString.toString();
			}
			valueString = valueString.replaceAll("\\,", "");
			try {
				double value = Double.parseDouble(valueString);
				numbers[n].setAttribute(VALUE_ATTRIBUTE, ("" + value));
				numberList.add(numbers[n]);
				numbersByStartIndex.put(new Integer(numbers[n].getStartIndex()), numbers[n]);
				numbersByEndIndex.put(new Integer(numbers[n].getEndIndex()), numbers[n]);
			}
			catch (NumberFormatException nfe) {
				if (DEBUG) System.out.println(" ==> could not parse " + valueString);
			}
		}
		if (numberList.size() < numbers.length)
			numbers = ((Annotation[]) numberList.toArray(new Annotation[numberList.size()]));
		numberList.clear();
		
		//	extract number ranges, sort out unparsable ones
		Annotation[] numberRangeInfixes = Gamta.extractAllMatches(data, NUMBER_RANGE_INFIX_REG_EX);
		ArrayList numberRangeList = new ArrayList();
		for (int r = 0; r < numberRangeInfixes.length; r++) {
			
			//	get numbers left and right of infix
			Annotation leftNumber = ((Annotation) numbersByEndIndex.get(new Integer(numberRangeInfixes[r].getStartIndex())));
			if (leftNumber == null)
				continue;
			Annotation rightNumber = ((Annotation) numbersByStartIndex.get(new Integer(numberRangeInfixes[r].getEndIndex())));
			if (rightNumber == null)
				continue;
			
			//	build range
			Annotation numberRange;
			
			//	number range contains '+/-' ==> center + radius notation
			if ("+".equals(numberRangeInfixes[r].firstValue())) {
				double centerValue = Double.parseDouble((String) leftNumber.getAttribute(VALUE_ATTRIBUTE));
				double radiusValue = Double.parseDouble((String) rightNumber.getAttribute(VALUE_ATTRIBUTE));
				if (centerValue <= radiusValue)
					continue;
				
				numberRange = Gamta.newAnnotation(data, null, leftNumber.getStartIndex(), (rightNumber.getEndIndex() - leftNumber.getStartIndex()));
				numberRange.setAttribute(VALUE_ATTRIBUTE, ("" + centerValue));
				numberRange.setAttribute((VALUE_ATTRIBUTE + "Min"), ("" + (centerValue - radiusValue)));
				numberRange.setAttribute((VALUE_ATTRIBUTE + "Max"), ("" + (centerValue + radiusValue)));
			}
			
			//	minimum + maximum notation
			else {
				double minValue = Double.parseDouble((String) leftNumber.getAttribute(VALUE_ATTRIBUTE));
				double maxValue = Double.parseDouble((String) rightNumber.getAttribute(VALUE_ATTRIBUTE));
				if (minValue >= maxValue)
					continue;
				
				numberRange = Gamta.newAnnotation(data, null, leftNumber.getStartIndex(), (rightNumber.getEndIndex() - leftNumber.getStartIndex()));
				numberRange.setAttribute(VALUE_ATTRIBUTE, ("" + ((minValue + maxValue) / 2)));
				numberRange.setAttribute((VALUE_ATTRIBUTE + "Min"), ("" + minValue));
				numberRange.setAttribute((VALUE_ATTRIBUTE + "Max"), ("" + maxValue));
			}
			
			numberRangeList.add(numberRange);
			numbersByStartIndex.remove(new Integer(leftNumber.getStartIndex()));
			numbersByEndIndex.remove(new Integer(leftNumber.getEndIndex()));
			numbersByStartIndex.remove(new Integer(rightNumber.getStartIndex()));
			numbersByEndIndex.remove(new Integer(rightNumber.getEndIndex()));
		}
		Annotation[] numberRanges = ((Annotation[]) numberRangeList.toArray(new Annotation[numberRangeList.size()]));
		numberRangeList.clear();
		if (numberRanges.length != 0) {
			for (int n = 0; n < numbers.length; n++) {
				
				//	this one was the first part of some range
				if (!numbersByEndIndex.containsKey(new Integer(numbers[n].getEndIndex())))
					continue;
				
				//	this one was the second part of some range
				if (!numbersByStartIndex.containsKey(new Integer(numbers[n].getStartIndex())))
					continue;
				
				//	this one is top level, keep it
				numberList.add(numbers[n]);
			}
			numbers = ((Annotation[]) numberList.toArray(new Annotation[numberList.size()]));
			numberList.clear();
		}
		
		//	extract units, index by start index
		Annotation[] unitAnnots = Gamta.extractAllContained(data, this.unitDictionary);
		HashMap unitAnnotsByStartIndex = new HashMap();
		for (int u = 0; u < unitAnnots.length; u++)
			unitAnnotsByStartIndex.put(new Integer(unitAnnots[u].getStartIndex()), unitAnnots[u]);
		
		//	merge numbers and units into quantities, and index them by start and end index
		ArrayList quantityList = new ArrayList();
		HashMap quantitiesByStartIndex = new HashMap();
		HashMap quantitiesByEndIndex = new HashMap();
		for (int n = 0; n < numbers.length; n++) {
			if (DEBUG) System.out.println("Assigning unit to " + TokenSequenceUtils.concatTokens(numbers[n], true, true));
			
			double value = Double.parseDouble((String) numbers[n].getAttribute(VALUE_ATTRIBUTE));
			if (value == 0) {
				if (DEBUG) System.out.println(" ==> 0 value, not a quantity");
				continue;
			}
			
			Annotation unitAnnot = ((Annotation) unitAnnotsByStartIndex.get(new Integer(numbers[n].getEndIndex())));
			if (unitAnnot == null) {
//				data.addAnnotation("number", numbers[n].getStartIndex(), numbers[n].size()).copyAttributes(numbers[n]);
//				if (DEBUG) System.out.println(" ==> unit not found, annotated as number");
				continue;
			}
			String unitString = TokenSequenceUtils.concatTokens(unitAnnot, true, true);
			Unit unit = ((Unit) this.units.get(unitString));
			if (unit == null) {
//				data.addAnnotation("number", numbers[n].getStartIndex(), numbers[n].size()).copyAttributes(numbers[n]);
//				if (DEBUG) System.out.println(" ==> unit data not found, annotated as number");
				continue;
			}
			
			double metricValue = (value * unit.factorToMetric);
			int metricMagnitude = unit.magnitudesToMetric;
			while ((metricValue >= 10) || (metricValue <= -10)) {
				metricValue /= 10;
				metricMagnitude++;
			}
			while ((metricValue < 1) && (metricValue > -1)) {
				metricValue *= 10;
				metricMagnitude--;
			}
			
			Annotation quantity = Gamta.newAnnotation(data, QUANTITY_TYPE, numbers[n].getStartIndex(), (unitAnnot.getEndIndex() - numbers[n].getStartIndex()));
			quantity.setAttribute(VALUE_ATTRIBUTE, ("" + value));
			quantity.setAttribute(UNIT_ATTRIBUTE, unit.symbol);
			quantity.setAttribute(METRIC_VALUE_ATTRIBUTE, ("" + metricValue));
			quantity.setAttribute(METRIC_MAGNITUDE_ATTRIBUTE, ("" + metricMagnitude));
			quantity.setAttribute(METRIC_UNIT_ATTRIBUTE, unit.metricUnit);
			
			quantityList.add(quantity);
			quantitiesByStartIndex.put(new Integer(quantity.getStartIndex()), quantity);
			quantitiesByEndIndex.put(new Integer(quantity.getEndIndex()), quantity);
			if (DEBUG) System.out.println(" ==> got quantity: " + quantity.toXML());
		}
		
		//	extract '+/-', and merge quantities into quantity ranges
		Annotation[] plusMinus = Gamta.extractAllMatches(data, "\\+\\s*\\/\\s*\\-");
		for (int p = 0; p < plusMinus.length; p++) {
			if (DEBUG) System.out.println("Handling possible range " + TokenSequenceUtils.concatTokens(plusMinus[p], true, true));
			Annotation centerQuantity = ((Annotation) quantitiesByEndIndex.get(new Integer(plusMinus[p].getStartIndex())));
			if (centerQuantity == null) {
				if (DEBUG) System.out.println(" ==> center quantity not found");
				continue;
			}
			Annotation radiusQuantity = ((Annotation) quantitiesByStartIndex.get(new Integer(plusMinus[p].getEndIndex())));
			if (radiusQuantity == null) {
				if (DEBUG) System.out.println(" ==> radius quantity not found");
				continue;
			}
			
			if (!centerQuantity.getAttribute(METRIC_UNIT_ATTRIBUTE).equals(radiusQuantity.getAttribute(METRIC_UNIT_ATTRIBUTE))) {
				if (DEBUG) System.out.println(" ==> metric unit mismatch");
				continue;
			}
			
			double cmv = Double.parseDouble((String) centerQuantity.getAttribute(METRIC_VALUE_ATTRIBUTE));
			int cmm = Integer.parseInt((String) centerQuantity.getAttribute(METRIC_MAGNITUDE_ATTRIBUTE));
			double rmv = Double.parseDouble((String) radiusQuantity.getAttribute(METRIC_VALUE_ATTRIBUTE));
			int rmm = Integer.parseInt((String) radiusQuantity.getAttribute(METRIC_MAGNITUDE_ATTRIBUTE));
			
			if ((cmm < rmm) || ((cmm == rmm) && (cmv < rmv)))
				continue;
			
			while (rmm < cmm) {
				rmm++;
				rmv /= 10;
			}
			
			quantitiesByStartIndex.remove(new Integer(centerQuantity.getStartIndex()));
			quantitiesByEndIndex.remove(new Integer(centerQuantity.getEndIndex()));
			quantitiesByStartIndex.remove(new Integer(radiusQuantity.getStartIndex()));
			quantitiesByEndIndex.remove(new Integer(radiusQuantity.getEndIndex()));
			
			Annotation quantity = Gamta.newAnnotation(data, QUANTITY_TYPE, centerQuantity.getStartIndex(), (radiusQuantity.getEndIndex() - centerQuantity.getStartIndex()));
			quantity.copyAttributes(centerQuantity);
			quantity.setAttribute((METRIC_VALUE_ATTRIBUTE + "Min"), ("" + (cmv - rmv)));
			quantity.setAttribute((METRIC_VALUE_ATTRIBUTE + "Max"), ("" + (cmv + rmv)));
			
			quantityList.add(quantity);
			quantitiesByStartIndex.put(new Integer(quantity.getStartIndex()), quantity);
			quantitiesByEndIndex.put(new Integer(quantity.getEndIndex()), quantity);
			if (DEBUG) System.out.println(" ==> got range quantity: " + quantity.toXML());
		}
		
		//	merge number ranges and units into quantities
		for (int r = 0; r < numberRanges.length; r++) {
			if (DEBUG) System.out.println("Assigning unit to " + TokenSequenceUtils.concatTokens(numberRanges[r], true, true));
			Annotation unitAnnot = ((Annotation) unitAnnotsByStartIndex.get(new Integer(numberRanges[r].getEndIndex())));
			if (unitAnnot == null) {
//				data.addAnnotation("number", numberRanges[r].getStartIndex(), numberRanges[r].size()).copyAttributes(numberRanges[r]);
//				if (DEBUG) System.out.println(" ==> unit not found, annotated as number range");
				continue;
			}
			String unitString = TokenSequenceUtils.concatTokens(unitAnnot, true, true);
			Unit unit = ((Unit) this.units.get(unitString));
			if (unit == null) {
				if (DEBUG) System.out.println(" ==> unit data not found, annotated as number range");
				continue;
			}
			
			double value = Double.parseDouble((String) numberRanges[r].getAttribute(VALUE_ATTRIBUTE));
			double minValue = Double.parseDouble((String) numberRanges[r].getAttribute(VALUE_ATTRIBUTE + "Min"));
			double maxValue = Double.parseDouble((String) numberRanges[r].getAttribute(VALUE_ATTRIBUTE + "Max"));
			double metricValue = (value * unit.factorToMetric);
			double minMetricValue = (minValue * unit.factorToMetric);
			double maxMetricValue = (maxValue * unit.factorToMetric);
			int metricMagnitude = unit.magnitudesToMetric;
			while (metricValue >= 10) {
				metricValue /= 10;
				minMetricValue /= 10;
				maxMetricValue /= 10;
				metricMagnitude++;
			}
			while (metricValue < 1) {
				metricValue *= 10;
				minMetricValue *= 10;
				maxMetricValue *= 10;
				metricMagnitude--;
			}
			
			Annotation quantity = Gamta.newAnnotation(data, QUANTITY_TYPE, numberRanges[r].getStartIndex(), (unitAnnot.getEndIndex() - numberRanges[r].getStartIndex()));
			quantity.setAttribute(VALUE_ATTRIBUTE, ("" + value));
			quantity.setAttribute((VALUE_ATTRIBUTE + "Min"), ("" + minValue));
			quantity.setAttribute((VALUE_ATTRIBUTE + "Max"), ("" + maxValue));
			quantity.setAttribute(UNIT_ATTRIBUTE, unit.symbol);
			quantity.setAttribute(METRIC_VALUE_ATTRIBUTE, ("" + metricValue));
			quantity.setAttribute((METRIC_VALUE_ATTRIBUTE + "Min"), ("" + minMetricValue));
			quantity.setAttribute((METRIC_VALUE_ATTRIBUTE + "Max"), ("" + maxMetricValue));
			quantity.setAttribute(METRIC_MAGNITUDE_ATTRIBUTE, ("" + metricMagnitude));
			quantity.setAttribute(METRIC_UNIT_ATTRIBUTE, unit.metricUnit);
			
			quantityList.add(quantity);
			quantitiesByStartIndex.put(new Integer(quantity.getStartIndex()), quantity);
			quantitiesByEndIndex.put(new Integer(quantity.getEndIndex()), quantity);
			if (DEBUG) System.out.println(" ==> got range quantity: " + quantity.toXML());
		}
		
		//	add annotations to document
		for (int q = 0; q < quantityList.size(); q++) {
			Annotation quantity = ((Annotation) quantityList.get(q));
			
			//	this one was the first part of some merger
			if (!quantitiesByEndIndex.containsKey(new Integer(quantity.getEndIndex())))
				continue;
			
			//	this one was the second part of some merger
			if (!quantitiesByStartIndex.containsKey(new Integer(quantity.getStartIndex())))
				continue;
			
			//	clean up string conversion rounding errors in double attributes
			this.cleanDoubleValues(quantity, VALUE_ATTRIBUTE);
			this.cleanDoubleValues(quantity, METRIC_VALUE_ATTRIBUTE);
			
			//	add annotation to document
			data.addAnnotation(quantity);
		}
	}
	
	private void cleanDoubleValues(Annotation quantity, String valueAttributeName) {
		String value = ((String) quantity.getAttribute(valueAttributeName));
		if (value == null)
			return;
		String valueMin = ((String) quantity.getAttribute(valueAttributeName + "Min"));
		String valueMax = ((String) quantity.getAttribute(valueAttributeName + "Max"));
		if ((valueMin != null) && (valueMax != null)) {
			int minValueLength = Math.min(value.length(), Math.min(valueMin.length(), valueMax.length()));
			quantity.setAttribute(valueAttributeName, this.cleanDoubleValue(value, minValueLength));
			quantity.setAttribute((valueAttributeName + "Min"), this.cleanDoubleValue(valueMin, minValueLength));
			quantity.setAttribute((valueAttributeName + "Max"), this.cleanDoubleValue(valueMax, minValueLength));
		}
		else if (value.matches(".*0{6,}[1-9]?"))
			quantity.setAttribute(valueAttributeName, this.cleanDoubleValue(value, 0));
	}
	
	private String cleanDoubleValue(String dbl, int minLength) {
		if (dbl.length() <= (minLength + 1))
			return dbl;
		int zeroStart = dbl.lastIndexOf("000000");
		if (zeroStart == -1)
			return dbl;
		dbl = dbl.substring(0, zeroStart);
		while (dbl.endsWith("0"))
			dbl = dbl.substring(0, (dbl.length()-1));
		if (dbl.endsWith("."))
			dbl = dbl.substring(0, (dbl.length()-1));
		return dbl;
	}
	
	private Annotation[] getNumbers(MutableAnnotation data) {
		ArrayList numberList = new ArrayList();
		HashSet numberKeys = new HashSet();
		Annotation[] ddNumbers = Gamta.extractAllMatches(data, DECIMAL_DOT_NUMBER_REG_EX);
//		TokenSequence dotTokenSequence = data.getTokenizer().tokenize(".");
		for (int n = 0; n < ddNumbers.length; n++) {
			if (DEBUG) System.out.println("Got decimal dot number: " + TokenSequenceUtils.concatTokens(ddNumbers[n], true, true));
			String dcnKey = ("" + ddNumbers[n].getStartIndex() + "-" + ddNumbers[n].size());
			if (DEBUG) System.out.println(" ==> key is " + dcnKey);
			if (numberKeys.add(dcnKey)) {
				String ddNumber = TokenSequenceUtils.concatTokens(ddNumbers[n], true, true);
				if (ddNumber.indexOf(',') < ddNumber.indexOf('.'))
					ddNumbers[n].setAttribute("style", "dd");
//				if (TokenSequenceUtils.contains(ddNumbers[n], dotTokenSequence))
//					ddNumbers[n].setAttribute("style", "dd");
				numberList.add(ddNumbers[n]);
			}
		}
		Annotation[] dcNumbers = Gamta.extractAllMatches(data, DECIMAL_COMMA_NUMBER_REG_EX);
//		TokenSequence commaTokenSequence = data.getTokenizer().tokenize(",");
		for (int n = 0; n < dcNumbers.length; n++) {
			if (DEBUG) System.out.println("Got decimal comma number: " + TokenSequenceUtils.concatTokens(dcNumbers[n], true, true));
			String ddnKey = ("" + dcNumbers[n].getStartIndex() + "-" + dcNumbers[n].size());
			if (DEBUG) System.out.println(" ==> key is " + ddnKey);
			if (numberKeys.add(ddnKey)) {
				String dcNumber = TokenSequenceUtils.concatTokens(dcNumbers[n], true, true);
				if (dcNumber.indexOf('.') < dcNumber.indexOf(','))
					dcNumbers[n].setAttribute("style", "dc");
//				if (TokenSequenceUtils.contains(dcNumbers[n], commaTokenSequence))
//					dcNumbers[n].setAttribute("style", "dc");
				numberList.add(dcNumbers[n]);
			}
		}
		Annotation[] numbers = ((Annotation[]) numberList.toArray(new Annotation[numberList.size()]));
		Arrays.sort(numbers, AnnotationUtils.getComparator(""));
		numberList.clear();
		Annotation lastNumber = null;
		for (int n = 0; n < numbers.length; n++) {
			if (lastNumber == null) {
				numberList.add(numbers[n]);
				lastNumber = numbers[n];
			}
			else if (!AnnotationUtils.liesIn(numbers[n], lastNumber)) {
				numberList.add(numbers[n]);
				lastNumber = numbers[n];
			}
			else if (DEBUG) System.out.println("Sorting out " + TokenSequenceUtils.concatTokens(numbers[n], true, true));
		}
		return ((Annotation[]) numberList.toArray(new Annotation[numberList.size()]));
	}
	
	private class Unit {
		private static final String UNIT_SYMBOL_ATTRIBUTE = "Unit";
		private static final String METRIC_UNIT_ATTRIBUTE = "MetricUnit";
		private static final String FACTOR_TO_METRIC_ATTRIBUTE = "FactorToMetric";
		private static final String MAGNITUDES_TO_METRIC_ATTRIBUTE = "MagnitudesToMetric";
		private static final String SYNONYMS_ATTRIBUTE = "Synonyms";
		
		final String symbol;
		final String metricUnit;
		final double factorToMetric;
		final int magnitudesToMetric;
		Unit(String symbol, String metricUnit, double factorToMetric, int magnitudesToMetric) {
			this.symbol = symbol;
			this.metricUnit = metricUnit;
			this.factorToMetric = factorToMetric;
			this.magnitudesToMetric = magnitudesToMetric;
		}
	}
}