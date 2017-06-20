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


import de.uka.ipd.idaho.stringUtils.StringVector;
/**
 * @author sautter
 */
public class CSMeasure implements ResultEvaluator {
	
	private int p2nPen = 1;	//	false positives
	private int n2pPen = 1;	//	false negatives
	private int p2uPen = 1;	//	uncertain positives
	private int n2uPen = 1;	//	uncertain negatives
	
	public CSMeasure() {
	}
	
	public CSMeasure(int fPen, int ucPen) {
		this.p2nPen = fPen;
		this.n2pPen = fPen;
		this.p2uPen = ucPen;
		this.n2uPen = ucPen;
	}
	
	public CSMeasure(int p2n, int n2p, int p2u, int n2u) {
		this.p2nPen = p2n;
		this.n2pPen = n2p;
		this.p2uPen = p2u;
		this.n2uPen = n2u;
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.ResultEvaluator#evaluateTest(de.idaho.documentAnalyzer.postProcessor.specNameFinder.AssignmentListCollector, de.idaho.documentAnalyzer.postProcessor.specNameFinder.AssignmentListCollector)
	 */
	public int evaluateTest(AssignmentListCollector posLog, AssignmentListCollector negLog) {
		return evaluateTest(posLog.posList.size(), posLog.negList.size(), posLog.ucList.size(), negLog.posList.size(), negLog.negList.size(), negLog.ucList.size());
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.ResultEvaluator#evaluateTest(int, int, int, int, int, int)
	 */
	public int evaluateTest(int pos2pos, int pos2neg, int pos2uc, int neg2pos, int neg2neg, int neg2uc) {
		int certain_sumPos = pos2pos + pos2neg*p2nPen + pos2uc*p2uPen;
		int certain_sumNeg = neg2pos*n2pPen + neg2neg + neg2uc*n2uPen;
		
		int p = ((certain_sumNeg > 0) ? ((100*neg2neg)/certain_sumNeg) : 0);
		int r = ((certain_sumPos > 0) ? ((100*pos2pos)/certain_sumPos) : 0);
		if ((p > 0) || r > 0) {
			return ((2 * (p * r)) / (p + r));
		} else {
			return 0;
		}
	}
	
	/** @see de.idaho.plugins.taxonomicNameIndexing.specNameFinder.ResultEvaluator#evaluateTest(int, int, int, int, int, int, de.idaho.util.StringVector, java.lang.String)
	 */
	public int evaluateTest(int pos2pos, int pos2neg, int pos2uc, int neg2pos, int neg2neg, int neg2uc, StringVector protocol, String prefix) {
		int certain_sumPos = pos2pos + pos2neg*p2nPen + pos2uc*p2uPen;
		int certain_sumNeg = neg2pos*n2pPen + neg2neg + neg2uc*n2uPen;
		
		int p = ((certain_sumNeg > 0) ? ((100*neg2neg)/certain_sumNeg) : 0);
		int r = ((certain_sumPos > 0) ? ((100*pos2pos)/certain_sumPos) : 0);
		if ((p > 0) || r > 0) {
			return ((2 * (p * r)) / (p + r));
		} else {
			return 0;
		}
	}
}
