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
public interface ResultEvaluator {
	
	/**	score the assignments contained in the specified AssignmentListCollector
	 * @param	posLog	the AssignmentListCollector carrying the assignment of positives
	 * @param 	negLog	the AssignmentListCollector carrying the assignment of negatives
	 * @return	the score for the assignments contained in the specified AssignmentListCollector
	 */
	public abstract int evaluateTest(AssignmentListCollector posLog, AssignmentListCollector negLog);
	
	/**	compute the score directly out of the specified values
	 * @param 	pos2pos
	 * @param 	pos2neg
	 * @param 	pos2uc
	 * @param 	neg2pos
	 * @param 	neg2neg
	 * @param 	neg2uc
	 * @return	the score computed out of the specified values
	 */
	public abstract int evaluateTest(int pos2pos, int pos2neg, int pos2uc, int neg2pos, int neg2neg, int neg2uc);
	
	/**	compute the score directly out of the specified values and make entries to the specified protocol
	 * @param 	pos2pos
	 * @param 	pos2neg
	 * @param 	pos2uc
	 * @param 	neg2pos
	 * @param 	neg2neg
	 * @param 	neg2uc
	 * @param	protocol	the StringVector to write the protocol to
	 * @param	prefix		the prefix to put before the protocol entries 
	 * @return	the score computed out of the specified values
	 */
	public abstract int evaluateTest(int pos2pos, int pos2neg, int pos2uc, int neg2pos, int neg2neg, int neg2uc, StringVector protocol, String prefix);
}
