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

package de.uka.ipd.idaho.plugins.treatmentSplitting;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * Constant bearer for treatment handling, providing annotation types and
 * classifications for both non-treatment document sections and inner structure
 * of treatments.
 * 
 * @author sautter
 */
public interface TreatmentConstants extends LiteratureConstants {
	
	//	outside-treatment div-types according to TaxonX
	public static final String abstract_TYPE = "abstract";
	public static final String acknowledgments_TYPE = "acknowledgments";
	public static final String document_head_TYPE = "document_head";
	public static final String introduction_TYPE = "introduction";
//	public static final String key_TYPE = "key";
	public static final String materials_methods_TYPE = "materials_methods";
//	public static final String multiple_TYPE = "multiple";
//	public static final String reference_group_TYPE = "reference_group";
	public static final String synopsis_TYPE = "synopsis";
	public static final String synonymic_list_TYPE = "synonymic_list";
	public static final String taxon_list_TYPE = "taxon_list"; // TODO make sure this gets transformed to valid TaxonX
	
	//	in-treatment div-types according to TaxonX
	public static final String biology_ecology_TYPE = "biology_ecology";
	public static final String description_TYPE = "description";
	public static final String diagnosis_TYPE = "diagnosis";
	public static final String discussion_TYPE = "discussion";
	public static final String distribution_TYPE = "distribution";
	public static final String etymology_TYPE = "etymology";
	public static final String key_TYPE = "key";
	public static final String materials_examined_TYPE = "materials_examined";
	public static final String synonymic_list_OPTION = "synonymic_list";
	
	//	additional in-treatment div types that are not in TaxonX
	public static final String conservation_TYPE = "conservation";
	public static final String vernacular_names_TYPE = "vernacular_names";
	
	//	nomenclature is not a div in TaxonX
	public static final String nomenclature_TYPE = "nomenclature";
	
	//	reference group is not a div in TaxonX
	public static final String reference_group_TYPE = "reference_group";
	
	//	multiple div type is not used in treatments, but is helpful for enclosing artifacts
	public static final String multiple_TYPE = "multiple";
	
	
	
	/** the annotation type for marking treatments */
	public static final String TREATMENT_ANNOTATION_TYPE = "treatment";
}
