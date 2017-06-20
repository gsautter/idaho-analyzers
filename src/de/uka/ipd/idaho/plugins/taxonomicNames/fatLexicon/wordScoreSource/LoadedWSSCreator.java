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


import java.util.StringTokenizer;

/** This is to create WSS-es from serialized form
 * @author kristof
 */
public class LoadedWSSCreator {
	/**
	 * Creates a WSS from it's string representation
	 * @param string	String representation _without_ the brackets.
	 * @return	The created WSS instance.
	 */
	static public WordScoreSource createWSS(String string) {
		System.out.println("Creating WSS from: ["+string+"]");
		WordScoreSource wss = null;
		StringTokenizer tokenizer = new StringTokenizer(string);
		int id = Integer.parseInt(tokenizer.nextToken());
		int type = Integer.parseInt(tokenizer.nextToken());
		System.out.println("ID="+id+", Type="+type);
		
		// Find out, where the "params" section begins (after ID and TYPE)
		int paramsBegin = string.indexOf(String.valueOf(id));
		paramsBegin = string.indexOf(" ",paramsBegin);
		paramsBegin = string.indexOf(String.valueOf(type),paramsBegin);
		paramsBegin = string.indexOf(" ",paramsBegin) + 1;

		String params = string.substring(paramsBegin);
		System.out.println("Params: ["+params+"]");
		
		switch (type) {
			case 0:
				System.out.println("[Warning] Serialized abstract default WSS?");
				break;
			case 1:	// Sequence
				wss = new SequenceWSS(id,params);
				break;
			case 2:	// End
				wss = new EndWSS(id,params);
				break;
			case 3:	// Block
				wss = new BlockWSS(id,params);
				break;
			case 4:	// Cascade
				wss = new CascadeWSS(id,params);
				break;
		}
		return wss;
	}

}
