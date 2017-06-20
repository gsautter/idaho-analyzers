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

package de.uka.ipd.idaho.plugins.modsReferencer;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * Container for the constants required for dealing with MODS meta data
 * 
 * @author sautter
 */
public interface ModsConstants extends LiteratureConstants {
	
	/** attribute for holding the MODS identifier, namely ModsDocID */
	public static final String MODS_ID_ATTRIBUTE = "ModsDocID";
	
	/** attribute for holding the author specified by the MODS meta data, namely ModsDocAuthor */
	public static final String MODS_AUTHOR_ATTRIBUTE = "ModsDocAuthor";
	
	/** attribute for holding the publication data specified by the MODS meta data, namely ModsDocDate */
	public static final String MODS_DATE_ATTRIBUTE = "ModsDocDate";
	
	/** attribute for holding the tite specified by the MODS meta data, namely ModsDocTitle */
	public static final String MODS_TITLE_ATTRIBUTE = "ModsDocTitle";
	
	/** attribute for holding the document origin (e.g. a URL) specified by the MODS meta data, namely ModsDocOrigin */
	public static final String MODS_ORIGIN_ATTRIBUTE = "ModsDocOrigin";
	
	/** attribute for holding the first page number of a document specified by the MODS meta data, namely ModsPageNumber */
	public static final String MODS_PAGE_NUMBER_ATTRIBUTE = "ModsPageNumber";
	
	/** attribute for holding the last page naumber of a document specified by the MODS meta data, namely ModsLastPageNumber */
	public static final String MODS_LAST_PAGE_NUMBER_ATTRIBUTE = "ModsLastPageNumber";
	
	
	/** default XML namespace atttribute for MODS meta data, namely xmlns:mods */
	public static final String MODS_NAMESPACE_URI_ATTRIBUTE = "xmlns:mods";
	
	/** the URL of the MODS schema, namely http://www.loc.gov/mods/v3 */
	public static final String MODS_NAMESPACE_URI = "http://www.loc.gov/mods/v3";
	
	
	/** default XML namespace prefix, namely mods: */
	public static final String MODS_PREFIX = "mods:";
	
	/** XML element name for the root element of a MODS meta data set, in the default namespace, namely mods:mods */
	public static final String MODS_MODS = MODS_PREFIX + "mods";
	
	/** XML element name for the element specifying the publication date of a document, in the default namespace, namely mods:date */
	public static final String MODS_DATE = MODS_PREFIX + "date";
	
	/** XML element name for the element specifying the issue date of a document, in the default namespace, namely mods:dateIssued */
	public static final String MODS_DATE_ISSUED = MODS_PREFIX + "dateIssued";
	
	/** XML element name for the element specifying the number of the last page of a document, in the default namespace, namely mods:end */
	public static final String MODS_END = MODS_PREFIX + "end";
	
	/** XML element name for the element specifying the identifier of a document, in the default namespace, namely mods:identifier */
	public static final String MODS_IDENTIFIER = MODS_PREFIX + "identifier";
	
	/** XML element name for the element specifying the location of a document, e.g. a URL, in the default namespace, namely mods:location */
	public static final String MODS_LOCATION = MODS_PREFIX + "location";
	
	/** XML element name for the element specifying a single person associated with a document, including a role, e.g. as an author, in the default namespace, namely mods:name */
	public static final String MODS_NAME = MODS_PREFIX + "name";
	
	/** XML element name for the element specifying a the name of a single person associated with a document, in the default namespace, namely mods:namePart */
	public static final String MODS_NAME_PART = MODS_PREFIX + "namePart";
	
	/** XML element name for the element specifying the book a document appeared in, in the default namespace, namely mods:number */
	public static final String MODS_NUMBER = MODS_PREFIX + "number";
	
	/** XML element name for the element specifying the issue number of a document, in the default namespace, namely mods:originInfo */
	public static final String MODS_ORIGIN_INFO = MODS_PREFIX + "originInfo";
	
	/** XML element name for the element specifying the name of the place a document was published, in the default namespace, namely mods:placeTerm */
	public static final String MODS_PLACE_TERM = MODS_PREFIX + "placeTerm";
	
	/** XML element name for the element specifying the publisher of a document, in the default namespace, namely mods:publisher */
	public static final String MODS_PUBLISHER = MODS_PREFIX + "publisher";
	
	/** XML element name for the element specifying the journal a document appeared in, in the default namespace, namely mods:relatedItem */
	public static final String MODS_RELATED_ITEM = MODS_PREFIX + "relatedItem";
	
	/** XML element name for the element specifying the role of a single person associated with a document, in the default namespace, namely mods:roleTerm */
	public static final String MODS_ROLE_TERM = MODS_PREFIX + "roleTerm";
	
	/** XML element name for the element specifying the number of the first page of a document, in the default namespace, namely mods:start */
	public static final String MODS_START = MODS_PREFIX + "start";
	
	/** XML element name for the element specifying the title of a document, in the default namespace, namely mods:title */
	public static final String MODS_TITLE = MODS_PREFIX + "title";
	
	/** XML element name for the element specifying the URL of a document, in the default namespace, namely mods:url */
	public static final String MODS_URL = MODS_PREFIX + "url";
}