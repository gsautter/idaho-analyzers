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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class TableCleaner extends AbstractConfigurableAnalyzer implements LiteratureConstants {
	
	private static final String TYPES_FORBIDDEN_IN_TABLES_LIST_NAME = "typesForbiddenInTables.txt";
	private StringVector typesForbiddenInTables = new StringVector();
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			this.typesForbiddenInTables.addContentIgnoreDuplicates(this.loadList(TYPES_FORBIDDEN_IN_TABLES_LIST_NAME));
		} catch (IOException e) {
			this.typesForbiddenInTables.addElement(PAGE_TITLE_TYPE);
			this.typesForbiddenInTables.addElement(FOOTNOTE_TYPE);
			this.typesForbiddenInTables.addElement(CAPTION_TYPE);
			this.typesForbiddenInTables.addElement(PAGE_NUMBER_TYPE);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AbstractConfigurableAnalyzer#exit()
	 */
	public void exit() {
		try {
			this.storeList(this.typesForbiddenInTables, TYPES_FORBIDDEN_IN_TABLES_LIST_NAME);
		} catch (IOException e) {}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.Analyzer#process(de.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get annotations (get all so type can be matched case-insensitively)
		MutableAnnotation[] annotations = data.getMutableAnnotations();
		
		//	process tables
		Set cleanedUpAnnotationIDs = new HashSet();
		for (int a = 0; a < annotations.length; a++)
			
			//	table Annotation found, and not cleaned up so far
			if ("table".equalsIgnoreCase(annotations[a].getType()) && !cleanedUpAnnotationIDs.contains(annotations[a].getAnnotationID())) {
				MutableAnnotation table = annotations[a];
				
				//	remember table processed
				cleanedUpAnnotationIDs.add(table.getAnnotationID());
				
				//	process nested Annotations
				Annotation[] inTable = table.getAnnotations();
				for (int i = 0; i < inTable.length; i++) {
					
					//	remove paragraphs inside tables
					if (MutableAnnotation.PARAGRAPH_TYPE.equals(inTable[i].getType()))
						table.removeAnnotation(inTable[i]);
					
					//	remove other annotations not welcome inside tables
					else if (this.typesForbiddenInTables.containsIgnoreCase(inTable[i].getType()))
						table.removeAnnotation(inTable[i]);
					
					//	remember annotation processed
					cleanedUpAnnotationIDs.add(inTable[i].getAnnotationID());
				}
				
				//	wrap paragraph around table 
				data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, table.getStartIndex(), table.size());
				
				//	refresh Annotations
				annotations = data.getMutableAnnotations(); 
			}
	}
}
