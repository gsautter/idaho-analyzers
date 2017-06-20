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

package de.uka.ipd.idaho.plugins.pageNormalization;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class PageCleaner extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String MOVE_TAG_LIST_NAME = "moveTagList.txt";
	private StringVector toMove = new StringVector();
	
	/** @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			InputStream is = this.dataProvider.getInputStream(MOVE_TAG_LIST_NAME);
			this.toMove = StringVector.loadList(is);
			is.close();
		}
		catch (IOException e) {
			this.toMove.addElementIgnoreDuplicates(CAPTION_TYPE);
			this.toMove.addElementIgnoreDuplicates(FOOTNOTE_TYPE);
			this.toMove.addElementIgnoreDuplicates("table");
		}
	}
	
	/** @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get annotations
		MutableAnnotation[] annotations = data.getMutableAnnotations();
		
		//	move annotations
		for (int a = 0; a < annotations.length; a++) {
			MutableAnnotation annotationToMove = annotations[a];
			if ((annotationToMove.size() != 0) && this.toMove.containsIgnoreCase(annotationToMove.getType())) {
				int start = data.size();
				data.addTokens(annotationToMove + data.getWhitespaceAfter(annotationToMove.getEndIndex() - 1));
				
				MutableAnnotation moved = data.addAnnotation(annotationToMove.getType(), start, annotationToMove.size());
				moved.copyAttributes(annotationToMove);
				Annotation[] subAnnotations = annotationToMove.getAnnotations();
				for (int sa = 0; sa < subAnnotations.length; sa++)
					if (!subAnnotations[sa].getAnnotationID().equals(annotationToMove.getAnnotationID()))
						moved.addAnnotation(subAnnotations[sa]);
					
				data.removeTokens(annotationToMove);
			}
		}
	}
}
