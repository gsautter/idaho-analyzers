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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource;


import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.easyIO.util.RandomByteSource;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class WSSTester implements FeedbackSource {
	
	private static SimpleDateFormat time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	static String dbName = "CElKnowRevAll1";
	
	public static void main(String[] args) throws Exception {
		
		//	load and shuffle words
		ArrayList posLoad = loadWords("C:/IdahoTest/species_set1.txt");
		Collections.shuffle(posLoad);
		ArrayList negLoad = loadWords("C:/IdahoTest/other_set1.txt");
		Collections.shuffle(negLoad);
		
		//	prepare word lists
		StringVector pos = new StringVector();
		for (int i = 0; i < posLoad.size(); i++) {
			pos.addElement((String) posLoad.get(i));
		}
		StringVector neg = new StringVector();
		for (int i = 0; i < negLoad.size(); i++) {
			neg.addElement((String) negLoad.get(i));
		}
		
		//	initialize WSS and test config
		WSSTester tester = new WSSTester(pos, neg, "C:/IdahoTest/test.idaho.cnfg");
		TestConfiguration conf = new TestConfiguration(null);
		conf.runs = 10;
		
		SequenceWSS seq;
		EndWSS end;
		BlockWSS block;
		//CascadeWSS cascade;
		
		/*
		ScorerSign  UcLimit
		End3		-6
		End3		6
		End3		3
		End2		9
		End2		-3
		End2		-9
		Seq3:SM32	0
		Seq2:SM4	3
		Seq2:SM1	6
		Seq2:SM64	3
					 */
		
		for (int b = (pos.size() / 100); b < pos.size(); b *= 2) {
			conf.blocks = b;
			String measureName = "";
			
			for (int i = 0; i < 1; i++) {
				
				//	create measure
				if (i == 0) {
					conf.measure = new CSMeasure();
					measureName = "-1111";
				} else if (i == 1) {
					conf.measure = new CSMeasure(3, 1);
					measureName = "-3311";
				} else if (i == 2) {
					conf.measure = new CSMeasure(10, 3, 1, 1);
					measureName = "-A311";
				}
				
				//	assemble block
				/*block = new BlockWSS(1, 1);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(8);
				seq.setUncertaintyLimit(-9);
				block.addWSS(seq);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(128);
				seq.setUncertaintyLimit(6);
				block.addWSS(seq);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(128);
				seq.setUncertaintyLimit(9);
				block.addWSS(seq);
				
				seq = new SequenceWSS(3);
				seq.setScoreMode(32);
				seq.setUncertaintyLimit(0);
				block.addWSS(seq);
				
				seq = new SequenceWSS(3);
				seq.setScoreMode(8);
				seq.setUncertaintyLimit(0);
				block.addWSS(seq);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(3);
				block.addWSS(end);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(0);
				block.addWSS(end);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(6);
				block.addWSS(end);
				
				end = new EndWSS(4);
				end.setUncertaintyLimit(0);
				block.addWSS(end);
				
				//	run tests
				conf.wss = block;
				tester.runTestSequence(conf, "Block-Custom" + measureName);*/
				
				//	assemble block
				block = new BlockWSS(1, 1);
				
				seq = new SequenceWSS(1);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(3);
				block.addWSS(seq);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(3);
				block.addWSS(seq);
				
				seq = new SequenceWSS(3);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(3);
				block.addWSS(seq);
				
				seq = new SequenceWSS(4);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(3);
				block.addWSS(seq);
				
				end = new EndWSS(2);
				end.setUncertaintyLimit(3);
				block.addWSS(end);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(3);
				block.addWSS(end);
				
				end = new EndWSS(4);
				end.setUncertaintyLimit(3);
				block.addWSS(end);
				
				//	run tests
				conf.wss = block;
				tester.runTestSequence(conf, "Block-Old" + measureName);
				
				//	assemble cascade
				/*cascade = new CascadeWSS();
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(8);
				seq.setUncertaintyLimit(-9);
				cascade.addWSS(seq);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(128);
				seq.setUncertaintyLimit(6);
				cascade.addWSS(seq);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(128);
				seq.setUncertaintyLimit(9);
				cascade.addWSS(seq);
				
				seq = new SequenceWSS(3);
				seq.setScoreMode(32);
				seq.setUncertaintyLimit(0);
				cascade.addWSS(seq);
				
				seq = new SequenceWSS(3);
				seq.setScoreMode(8);
				seq.setUncertaintyLimit(0);
				cascade.addWSS(seq);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(3);
				cascade.addWSS(end);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(0);
				cascade.addWSS(end);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(6);
				cascade.addWSS(end);
				
				end = new EndWSS(4);
				end.setUncertaintyLimit(0);
				cascade.addWSS(end);
				
				//	run tests
				conf.wss = cascade;
				tester.runTestSequence(conf, "Cascade-Custom" + measureName);*/
				
				
				//	cascade of blocks
				/*cascade = new CascadeWSS();
				
				block = new BlockWSS(1,1);
				
				seq = new SequenceWSS(2);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(0);
				block.addWSS(seq);
				
				end = new EndWSS(2);
				end.setUncertaintyLimit(0);
				block.addWSS(end);
				
				cascade.addWSS(block);
				
				block = new BlockWSS(1,1);
				
				seq = new SequenceWSS(3);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(0);
				block.addWSS(seq);
				
				end = new EndWSS(3);
				end.setUncertaintyLimit(0);
				block.addWSS(end);
				
				cascade.addWSS(block);
				
				block = new BlockWSS(1,1);
				
				seq = new SequenceWSS(4);
				seq.setScoreMode(-2);
				seq.setUncertaintyLimit(0);
				block.addWSS(seq);
				
				end = new EndWSS(4);
				end.setUncertaintyLimit(0);
				block.addWSS(end);
				
				cascade.addWSS(block);
				
				//	run tests
				conf.wss = cascade;
				tester.runTestSequence(conf, "Cascade-Of-Blocks" + measureName);*/
				
				
				//	sequence length
				for (int len = 1; len < 5; len ++) {
					
					if (len > 1) {
						end = new EndWSS(len);
						conf.wss = end;
						tester.runTestSequence(conf, ("End" + len + measureName)); 
					} else {
						FlexEndWSS fend = new FlexEndWSS(4);
						conf.wss = fend;
						tester.runTestSequence(conf, ("EndFlex" + measureName)); 
					}
					
					//	score mode iteration
					for (int s = -2; s < 129; s += ((s < 1) ? 1 : s)) {
						
						if (len == 1) {
							FlexSequenceWSS fseq = new FlexSequenceWSS(4);
							fseq.setScoreMode(s);
							conf.wss = fseq;
							tester.runTestSequence(conf, ("SeqFlex:" + ((s == -2) ? "AUTO" : ((s == -1) ? "ADD" : ((s == 0) ? "MARK" : ("SM" + s)))) + measureName));
						}
						
						seq = new SequenceWSS(len);
						seq.setScoreMode(s);
						conf.wss = seq;
						tester.runTestSequence(conf, ("Seq" + len + ":" + ((s == -2) ? "AUTO" : ((s == -1) ? "ADD" : ((s == 0) ? "MARK" : ("SM" + s)))) + measureName));
					}
				}
			}
		}
	}
	
	//	instance data below
	
	private StringVector positives;
	private StringVector negatives;
	
	private boolean gaveFeedback = false;
	private int feedbackCounter = 0;
	
	private IoProvider io;
	
	//	true --> DB, false --> System.out.println
	private boolean realRun = true;
	
	private WSSTester(StringVector positives, StringVector negatives, String configPath) throws Exception {
		this.positives = positives;
		this.negatives = negatives;
		this.io = EasyIO.getIoProvider(Settings.loadSettings(new File(configPath)));
	}
	
	/**	run a sequence of tests
	 * @param	config	the container carrying the iteration borders and steppings, the WSS, etc
	 * @param 	prefix	the WSS name to store in the DB
	 * @throws	Exception if any exception occurs
	 */
	private void runTestSequence(TestConfiguration config, String prefix) throws Exception {
		
			
		//	feedbackWeight iteration
		for (int fw = config.fwLower; fw < config.fwUpper; fw += config.fwStep) {
			
			//	experianceWeight iteration
			for (int ew = config.exwLower; ew < config.exwUpper; ew += config.exwStep) {
				
				//	ucLimit iteration
				for (int ucl = config.uclLower; ucl < config.uclUpper; ucl += config.uclStep) {
						
					//	experianceThreshold
					int exThreshold = 0;
					int rn = 0;
					int scoreSum = 0;
					int avgScore = 0;
					int lastAvgScore;
					while (exThreshold < 100) {
						
						System.out.println(getTime() + ": Running test " + prefix + ", FbW: " + fw + ", ExW: " + ew + ", UcL: " + ucl + ", ExT: " + exThreshold + " ...");
						
						int mes = this.runTest(config.wss, config.blocks, config.runs, ucl, config.measure, fw, exThreshold, ew, prefix);
						
						//	compute average score
						lastAvgScore = avgScore;
						scoreSum += mes;
						rn++;
						avgScore = ((int) ((((float) scoreSum) / rn) * 1000));
						
						if (ew == 0) {
							exThreshold = 100;
						} else if (exThreshold < mes) {
							exThreshold = (((mes + 9) / 10) * 10);
						} else {
							exThreshold += ((avgScore <= lastAvgScore) ? 20 : 10);
						}
					}
				}
			}
		}
	}
	
	/**	test a WSS
	 * @param wss					the WSS to be tested
	 * @param blocks				the number of blocks the data will be divided into (first block for training, all other blocks for test runs)
	 * @param runs					the number of runs
	 * @param ucLimit				the uncertaintyLimit to use
	 * @param measure				the measure to use for computing the success of a run
	 * @param feedbackWeight		the weight of feedback
	 * @param experianceThreshold	the threshold that a score has to exceed befor a component learns from it's own judgement
	 * @param experianceWeight		the weight of own experiance
	 * @param prefix		the String to identify the DB entries
	 * @return	the worst measure achieved over the runs
	 */
	private synchronized int runTest(WordScoreSource wss, int blocks, int runs, int ucLimit, ResultEvaluator measure, int feedbackWeight, int experianceThreshold, int experianceWeight, String prefix) throws Exception {
		int worstMeasure = 100;
		
		//	set up training sets
		StringVector posTrain = new StringVector();
		for (int i = 0; i < this.positives.size(); i += blocks) {
			posTrain.addElement(this.positives.get(i));
		}
		StringVector negTrain = new StringVector();
		for (int i = 0; i < this.negatives.size(); i += blocks) {
			negTrain.addElement(this.negatives.get(i));
		}
		
		//	reset and train the WSS
		wss.resetStatistics();
		wss.train(posTrain, negTrain);
		if (measure != null) wss.optimize(measure);
		wss.setUncertaintyLimit(ucLimit);
		
		//	initialize test sets
		StringVector posTest = new StringVector();
		StringVector negTest = new StringVector();
		
		//	write test head
		String testID = RandomByteSource.getGUID();
		String query = "INSERT INTO " + dbName + ".dbo.Tests (TestGUID, TrSetSizePos, TrSetSizeNeg, UcLimit, FbWeight, ExThreshold, ExWeight, ScorerSign)" +
						" VALUES (" + testID + ", " + posTrain.size() + ", " + negTrain.size() + ", " + ucLimit + ", " + feedbackWeight + ", " + experianceThreshold + ", " + experianceWeight + ", '" + prefix + "')";
		this.doOutput(query);
		
		//	initialize loggers
		StringVector pos2negLog = new StringVector();
		StringVector pos2ucLog = new StringVector();
		StringVector neg2posLog = new StringVector();
		StringVector neg2ucLog = new StringVector();
		
		//	perform test runs
		for (int run = 0; run < runs; run++) {
			
			//	initialize counters
			int pos2pos = 0;
			int pos2neg = 0;
			int pos2uc = 0;
			int neg2pos = 0;
			int neg2neg = 0;
			int neg2uc = 0;
			
			//	clear loggers
			pos2negLog.clear();
			pos2ucLog.clear();
			neg2posLog.clear();
			neg2ucLog.clear();
			
			posTest.clear();
			negTest.clear();
			
			//	set up test sets
			for (int i = (run % blocks); i < this.positives.size(); i += blocks) {
				posTest.addElement(this.positives.get(i));
			}
			for (int i = (run % blocks); i < this.negatives.size(); i += blocks) {
				negTest.addElement(this.negatives.get(i));
			}
			
			//	combine and shuffle test sets
			ArrayList mixer = new ArrayList();
			for (int i = 0; i < posTest.size(); i++) {
				mixer.add(posTest.get(i));
			}
			for (int i = 0; i < negTest.size(); i++) {
				mixer.add(negTest.get(i));
			}
			Collections.shuffle(mixer);
			String[] testSet = new String[mixer.size()];
			mixer.toArray(testSet);
			
			//	reset feedback counter
			this.feedbackCounter = 0;
			
			//	run test
			for (int i = 0; i < testSet.length; i++) {
				boolean isPositive = posTest.containsIgnoreCase(testSet[i]);
				this.gaveFeedback = false;
				int vote = wss.getVote(testSet[i], experianceThreshold, experianceWeight, ((feedbackWeight == 0) ? null : this), feedbackWeight);
				
				if (isPositive) {
					if (this.gaveFeedback || (vote == 0)) {
						pos2uc++;
						pos2ucLog.addElementIgnoreDuplicates(testSet[i]);
					} else if (vote == 1) {
						pos2pos++;
					} else {
						pos2neg++;
						pos2negLog.addElementIgnoreDuplicates(testSet[i]);
					}
				} else {
					if (this.gaveFeedback || (vote == 0)) {
						neg2uc++;
						neg2ucLog.addElementIgnoreDuplicates(testSet[i]);
					} else if (vote == -1) {
						neg2neg++;
					} else {
						neg2pos++;
						neg2posLog.addElementIgnoreDuplicates(testSet[i]);
					}
				}
			}
			
			//	remember score if it's the worst so far
			int score = measure.evaluateTest(pos2pos, pos2neg, pos2uc, neg2pos, neg2neg, neg2uc);
			worstMeasure = ((score < worstMeasure) ? score : worstMeasure);
			
			System.out.print(" - Run " + (run +1) + " finished: " + score + ", optimizing ... ");
			
			//	optimize WSS with the feedback and experiance from the last test run
			wss.optimize(measure);
			
			System.out.println("Done: " + wss.toString());
			
			//	store test results
			String runID = RandomByteSource.getGUID();
			String falseWords = "WssStat: " + wss.toString() + "; Pos2Neg: " + pos2negLog.concatStrings(",") + ";Pos2Uc: " + pos2ucLog.concatStrings(",") + ";Neg2Pos: " + neg2posLog.concatStrings(",") + ";Neg2Uc: " + neg2ucLog.concatStrings(",");
			query = "INSERT INTO " + dbName + ".dbo.Runs (RunGUID, TestID, RunNr, pos2pos, pos2neg, pos2uc, neg2pos, neg2neg, neg2uc, Words)" +
					" VALUES (" + runID + ", " + testID + ", " + (run+1) + ", " + pos2pos + ", " + pos2neg + ", " + pos2uc + ", " + neg2pos + ", " + neg2neg + ", " + neg2uc + ", '" + falseWords + "')";
			this.doOutput(query);
			
			//	evaluate test results
			float p2p = pos2pos;
			float p2n = pos2neg;
			float p2u = pos2uc;
			float n2p = neg2pos;
			float n2n = neg2neg;
			float n2u = neg2uc;
			
			//	compute decicivenes
			float decPos = (((p2p + p2n + p2u) == 0) ? 0 : ((p2p + p2n) / (p2p + p2n + p2u)));
			float decNeg = (((n2p + n2n + n2u) == 0) ? 0 : ((n2p + n2n) / (n2p + n2n + n2u)));
			float decMes = getFMes(decPos, decNeg);
			
			//	compute correctnes
			float corrPos = (((p2p + p2n) == 0) ? 0 : (p2p / (p2p + p2n)));
			float corrNeg = (((n2p + n2n) == 0) ? 0 : (n2n / (n2p + n2n)));
			float corrMes = getFMes(corrPos, corrNeg);
			
			//	compute overall quality
			float qualPos = decPos * corrPos;
			float qualNeg = decNeg * corrNeg;
			float qualMes = getFMes(qualPos, qualNeg);
			
			//	store evaluation results
			query = "INSERT INTO " + dbName + ".dbo.RunEval (EvalGUID, TestID, decPos, decNeg, decMes, corrPos, corrNeg, corrMes, qualPos, qualNeg, qualMes)" +
					" VALUES (" + runID + ", " + testID + ", " + decPos + ", " + decNeg + ", " + decMes + ", " + corrPos + ", " + corrNeg + ", " + corrMes + ", " + qualPos + ", " + qualNeg + ", " + qualMes + ")";
			this.doOutput(query);
		}
		
		return worstMeasure;
	}
	
	/*	SQL script for creating log table:
	
		create database CsorbaTest

		create table Tests (
		TestGUID varbinary(16) primary key not null,
		TrSetSizePos smallint,
		TrSetSizeNeg smallint,
		UcLimit smallint,
		FbWeight tinyint,
		ExThreshold tinyint,
		ExWeight tinyint,
		ScorerSign varchar(32))
		
		create table Runs (
		RunGUID varbinary(16) primary key not null,
		TestID varbinary(16),
		RunNr smallint,
		pos2pos smallint,
		pos2neg smallint,
		pos2uc smallint,
		neg2pos smallint,
		neg2neg smallint,
		neg2uc smallint,
		Words text)
		
		create table RunEval (
		EvalGUID varbinary(16) primary key not null,
		TestID varbinary(16),
		decPos real,
		decNeg real,
		decMes real,
		corrPos real,
		corrNeg real,
		corrMes real,
		qualPos real,
		qualNeg real,
		qualMes real)
		
	 */
	
	public boolean getFeedback(String string) {
		this.feedbackCounter++;
		this.gaveFeedback = true;
		return this.positives.containsIgnoreCase(string);
	}
	
	private float getFMes(float p, float r) {
		return (((p+r) == 0) ? 0 : ((2*p*r) / (p+r)));
	}
	
	private void doOutput(String query) throws Exception {
		if (this.realRun) this.io.executeUpdateQuery(query);
		if (!this.realRun) System.out.println(query);
	}
	
	//	data loader
	public static ArrayList loadWords(String filename) throws Exception {
		ArrayList wordList = new ArrayList();
		StringBuffer stringBuffer = new StringBuffer();
		FileReader inputReader;
		inputReader = new FileReader(filename);
		while (true) {
			int chInt = inputReader.read();
			if (chInt == -1) break;
			String str = String.valueOf((char)chInt);
			if (str.equals("\n") || str.equals(".") || str.equals(",") || (Gamta.PUNCTUATION.indexOf(str) > -1) || str.equals("?") || str.equals("!") || str.equals("\"") || str.equals("\r")) str=" ";
			stringBuffer.append(str);
			if (str.equals(" ")) { // New sequence after delimiter
				if (stringBuffer.length()>2)	// Do not store empty strings (with 2 spaces on the begining and end).
					wordList.add(stringBuffer.toString().toLowerCase());
				stringBuffer.setLength(0);
				stringBuffer.append(" ");
			}
		}			
		inputReader.close();
		return wordList;
	}
	
	//	get the current time in a nice readable format
	private static String getTime() {
		return time.format(new Date());
	}
}
