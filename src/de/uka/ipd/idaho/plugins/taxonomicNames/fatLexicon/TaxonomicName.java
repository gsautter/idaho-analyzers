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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon;


import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Object representing a taxonomic name in the Linean system of nomenclature.
 * Instances of this object are intended to represent names of rank genus and
 * below in the course of recognition, completion, and resolution.
 * 
 * @author sautter
 */
public class TaxonomicName {
	
//	public static final String TAXONOMIC_NAME_ANNOTATION_TYPE = "taxonomicName";
//	public static final String TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE = "taxonomicNameLabel";
//	
//	public static final String FAMILY_ATTRIBUTE = "family";
//	public static final String SUBFAMILY_ATTRIBUTE = "subFamily";
//	public static final String TRIBE_ATTRIBUTE = "tribe";
//	
//	public static final String GENUS_ATTRIBUTE = "genus";
//	public static final String SUBGENUS_ATTRIBUTE = "subGenus";
//	public static final String SPECIES_ATTRIBUTE = "species";
//	public static final String SUBSPECIES_ATTRIBUTE = "subSpecies";
//	public static final String VARIETY_ATTRIBUTE = "variety";
//	
//	public static final String[] PART_NAMES = {GENUS_ATTRIBUTE, SUBGENUS_ATTRIBUTE, SPECIES_ATTRIBUTE, SUBSPECIES_ATTRIBUTE, VARIETY_ATTRIBUTE};
//	
//	public static final String RANK_ATTRIBUTE = "rank";
//	public static final String LSID_ATTRIBUTE = "LSID";
	
	String genus = null;
	String subGenus = null;
	String species = null;
	String speciesAuthor = null;
	
	/**	true if and only if subspecies part is sure to be subspecies part, i.e.
		species abbreviated, or
		species author given, or
		subspecies labelled, or
		subspecies given and variety secure
	 */
	boolean subSpeciesSecure = false;
	String subSpecies = null;
	String subSpeciesAuthor = null;
	
	/**	true if and only if variety part is sure to be variety part, i.e.
		subspecies abbreviated, or
		subspecies author given, or
		variety labelled, or
		subspecies missing and species abbreviated, or
		subspecies missing and species author given
	 */
	boolean varietySecure = false;
	String variety = null;
	String varietyAuthor = null;
	
	//	first non-abbreviated parts
	String firstFullPart = null;
	String firstFullLcPart = null;
	
//	/**	Constructor directly taking the meaningful parts
//	 * @param genus
//	 * @param subGenus
//	 * @param species
//	 * @param subSpecies
//	 * @param variety
//	 */
//	public TaxonomicName(String genus, String subGenus, String species, String subSpecies, String variety) {
//		this.genus = genus;
//		if ((this.genus != null) && (this.firstFullPart == null) && (this.genus.length() > 2)) this.firstFullPart = this.genus;
//		
//		this.subGenus = subGenus;
//		if ((this.subGenus != null) && (this.firstFullPart == null) && (this.subGenus.length() > 2)) this.firstFullPart = this.subGenus;
//		
//		this.species = species;
//		if ((this.species != null) && (this.firstFullPart == null) && (this.species.length() > 2)) this.firstFullPart = this.species;
//		if ((this.species != null) && (this.firstFullLcPart == null) && (this.species.length() > 2)) this.firstFullLcPart = this.species;
//		
//		this.subSpecies = subSpecies;
//		if ((this.subSpecies != null) && (this.firstFullPart == null) && (this.subSpecies.length() > 2)) this.firstFullPart = this.subSpecies;
//		if ((this.subSpecies != null) && (this.firstFullLcPart == null) && (this.subSpecies.length() > 2)) this.firstFullLcPart = this.subSpecies;
//		
//		this.variety = variety;
//		if ((this.variety != null) && (this.firstFullPart == null) && (this.variety.length() > 2)) this.firstFullPart = this.variety;
//		if ((this.variety != null) && (this.firstFullLcPart == null) && (this.variety.length() > 2)) this.firstFullLcPart = this.variety;
//		
//		this.varietySecure = (this.variety != null) && (this.varietySecure || ((this.subSpecies != null) && ((this.subSpecies.length() < 3) || (this.subSpeciesAuthor != null))) || ((this.subSpecies == null) && (this.species != null) && ((this.species.length() < 3) || (this.speciesAuthor != null))));
//		this.subSpeciesSecure = (this.subSpecies != null) && (this.subSpeciesSecure || this.varietySecure || ((this.species != null) && ((this.species.length() < 3) || (this.speciesAuthor != null))));
//	}
	
	/**	Constructor building a TaxonomicName from an Annotation
	 * @param data
	 */
	public TaxonomicName(Annotation data) {
		this(TokenSequenceUtils.getTextTokens(data), "(".equals(data.firstValue()));
	}
	
//	/**	Constructor building a TaxonomicName from its parts
//	 * @param nameParts
//	 */
//	public TaxonomicName(StringVector nameParts) {
//		this(nameParts, false);
//	}
	
	private TaxonomicName(StringVector nameParts, boolean firstIsSubGenus) {
		StringVector parts = new StringVector();
		parts.addContent(nameParts);
		String name;
		
		//	fill in missing parts with # (never appears in a taxonomic name)
		//	genus
		if (firstIsSubGenus || (parts.size() == 0) || !StringUtils.isFirstLetterUpWord(parts.get(0)) || FAT.upperCaseLowerCaseParts.contains(parts.get(0))) parts.insertElementAt("#", 0);
		else {
			this.genus = StringUtils.capitalize(parts.get(0));
			if (this.genus.length() > 2) this.firstFullPart = this.genus;
		}
		
		//	subgenus
		if ((parts.size() < 2) || !StringUtils.isCapitalizedWord(parts.get(1)) || FAT.upperCaseLowerCaseParts.contains(parts.get(1))) parts.insertElementAt("#", 1);
		else {
			this.subGenus = parts.get(1);
			if ((this.firstFullPart == null) && (this.subGenus.length() > 2)) this.firstFullPart = this.subGenus;
		}
		
		//	species
		if ((parts.size() < 3) || FAT.subSpeciesLabels.contains(parts.get(2)) || FAT.subSpeciesLabels.contains(parts.get(2) + ".") || FAT.varietyLabels.contains(parts.get(2)) || FAT.varietyLabels.contains(parts.get(2) + ".") || (!StringUtils.isLowerCaseWord(parts.get(2)) && !FAT.upperCaseLowerCaseParts.contains(parts.get(2)))) parts.insertElementAt("#", 2);
		else {
			this.species = parts.get(2);
			if ((this.firstFullPart == null) && (this.species.length() > 2)) this.firstFullPart = this.species;
			if ((this.firstFullLcPart == null) && (this.species.length() > 2)) this.firstFullLcPart = this.species;
		}
		
		//	remove name
		name = null;
		while ((parts.size() > 3) && ((StringUtils.UPPER_CASE_LETTERS.indexOf(parts.get(3).charAt(0)) != -1) || FAT.nameNoiseWords.contains(parts.get(3))))
			name = parts.remove(3);
		if ((name != null) && (name.length() > 1) && StringUtils.isCapitalizedWord(name) && (this.species != null)) this.speciesAuthor = name;
		
		//	subspecies
		if (parts.size() < 4) parts.insertElementAt("#", 3);
		else if (FAT.varietyLabels.contains(parts.get(3)) || FAT.varietyLabels.contains(parts.get(3) + ".")) parts.insertElementAt("#", 3);
		else if (FAT.subSpeciesLabels.contains(parts.get(3)) || FAT.subSpeciesLabels.contains(parts.get(3) + ".")) {
			parts.removeElementAt(3);
			this.subSpeciesSecure = true;
			if (parts.size() < 4) parts.insertElementAt("#", 3);
			else {
				this.subSpecies = parts.get(3);
				if ((this.firstFullPart == null) && (this.subSpecies.length() > 2)) this.firstFullPart = this.subSpecies;
				if ((this.firstFullLcPart == null) && (this.subSpecies.length() > 2)) this.firstFullLcPart = this.subSpecies;
			}
		} else {
			this.subSpecies = parts.get(3);
			if ((this.firstFullPart == null) && (this.subSpecies.length() > 2)) this.firstFullPart = this.subSpecies;
			if ((this.firstFullLcPart == null) && (this.subSpecies.length() > 2)) this.firstFullLcPart = this.subSpecies;
		}
		
		//	remove name
		name = null;
		while ((parts.size() > 4) && ((StringUtils.UPPER_CASE_LETTERS.indexOf(parts.get(4).charAt(0)) != -1) || FAT.nameNoiseWords.contains(parts.get(4))))
			name = parts.remove(4);
		if ((name != null) && (name.length() > 1) && StringUtils.isCapitalizedWord(name) && (this.subSpecies != null)) this.subSpeciesAuthor = name;
		
		//	variety
		if (parts.size() < 5) parts.insertElementAt("#", 4);
		else if (FAT.varietyLabels.contains(parts.get(4)) || FAT.varietyLabels.contains(parts.get(4) + ".")) {
			parts.removeElementAt(4);
			this.varietySecure = true;
			if (parts.size() < 5) parts.insertElementAt("#", 4);
			else {
				this.variety = parts.get(4);
				if ((this.firstFullPart == null) && (this.variety.length() > 2)) this.firstFullPart = this.variety;
				if ((this.firstFullLcPart == null) && (this.variety.length() > 2)) this.firstFullLcPart = this.variety;
			}
		} else {
			this.variety = parts.get(4);
			if ((this.firstFullPart == null) && (this.variety.length() > 2)) this.firstFullPart = this.variety;
			if ((this.firstFullLcPart == null) && (this.variety.length() > 2)) this.firstFullLcPart = this.variety;
		}
		
		//	remove name;
		name = null;
		while ((parts.size() > 5) && ((StringUtils.UPPER_CASE_LETTERS.indexOf(parts.get(5).charAt(0)) != -1) || FAT.nameNoiseWords.contains(parts.get(5))))
			name = parts.remove(5);
		if ((name != null) && (name.length() > 1) && StringUtils.isCapitalizedWord(name) && (this.variety != null)) this.varietyAuthor = name;
		
		//	rate security of subspecies and variety
		this.varietySecure = (this.variety != null) && (this.varietySecure || ((this.subSpecies != null) && ((this.subSpecies.length() < 3) || (this.subSpeciesAuthor != null))) || ((this.subSpecies == null) && (this.species != null) && ((this.species.length() < 3) || (this.speciesAuthor != null))));
		this.subSpeciesSecure = (this.subSpecies != null) && (this.subSpeciesSecure || this.varietySecure || ((this.species != null) && ((this.species.length() < 3) || (this.speciesAuthor != null))));
	}
	
	/**	@see java.lang.Object#toString()
	 */
	public String toString() {
		String name = "";
		name += ((this.genus == null) ? "#" : this.genus) + " ";
		name += ((this.subGenus == null) ? "#" : this.subGenus) + " ";
		name += ((this.species == null) ? "#" : this.species) + " ";
		name += ((this.subSpecies == null) ? "#" : this.subSpecies) + " ";
		name += ((this.variety == null) ? "#" : this.variety);
		return name;
	}
	
	/**	@return	a canonic string representation of the taxonomic name (no authors, etc.)
	 */
	public String toCanonicString() {
		String name = "";
		if (this.genus != null) name += this.genus + ((this.genus.length() < 3) ? "." : "") + " ";
		if (this.subGenus != null) name += this.subGenus + ((this.subGenus.length() < 3) ? "." : "") + " ";
		if (this.species != null) name += this.species + ((this.species.length() < 3) ? "." : "") + " ";
		if (this.subSpecies != null) name += this.subSpecies + ((this.subSpecies.length() < 3) ? "." : "") + " ";
		if (this.variety != null) name += this.variety;
		return name.trim();
	}
	
	/**	@return	a full string representation of the taxonomic name (including authors, etc.)
	 */
	public String toFullString() {
		String name = "";
		if (this.genus != null) name += this.genus + ((this.genus.length() < 3) ? "." : "") + " ";
		if (this.subGenus != null) name += this.subGenus + ((this.subGenus.length() < 3) ? "." : "") + " ";
		if (this.species != null) name += this.species + ((this.species.length() < 3) ? "." : ((this.speciesAuthor == null) ? "" : (" " + this.speciesAuthor))) + " ";
		if (this.subSpecies != null) name += ((this.subSpecies.length() < 3) ? "" : "subsp. ") + this.subSpecies + ((this.subSpecies.length() < 3) ? "." : ((this.subSpeciesAuthor == null) ? "" : (" " + this.subSpeciesAuthor))) + " ";
		if (this.variety != null) name += "var. " + this.variety + ((this.varietyAuthor == null) ? "" : (" " + this.varietyAuthor));
		return name.trim();
	}
	
	/**	@see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return ((o instanceof TaxonomicName) && this.equals((TaxonomicName) o));
	}
	
	/**	@see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(TaxonomicName tn) {
		if (tn == null) return false;
		
		int vote = 0;
		boolean firstExplicitPart = false;
		
		if ((this.variety != null) && (tn.variety != null)) {
			if (this.variety.equals(tn.variety)) vote += 2;
			else if ((this.variety.startsWith(tn.variety) && (tn.variety.length() < 3)) || (tn.variety.startsWith(this.variety) && (this.variety.length() < 3))) vote += 1;
			else return false;
			firstExplicitPart = true;
		} else if (this.variety != tn.variety) return false;
		
		if ((this.subSpecies != null) && (tn.subSpecies != null)) {
			if (this.subSpecies.equals(tn.subSpecies)) vote += 2;
			else if ((this.subSpecies.startsWith(tn.subSpecies) && (tn.subSpecies.length() < 3)) || (tn.subSpecies.startsWith(this.subSpecies) && (this.subSpecies.length() < 3))) vote += 1;
			else return false;
			firstExplicitPart = true;
		} else if ((this.subSpecies != tn.subSpecies) && !firstExplicitPart) return false;
		
		if ((this.species != null) && (tn.species != null)) {
			if (this.species.equals(tn.species)) vote += 2;
			else if ((this.species.startsWith(tn.species) && (tn.species.length() < 3)) || (tn.species.startsWith(this.species) && (this.species.length() < 3))) vote += 1;
			else return false;
			firstExplicitPart = true;
		} else if ((this.species != tn.species) && !firstExplicitPart) return false;
		
		if ((this.subGenus != null) && (tn.subGenus != null)) {
			if (this.subGenus.equals(tn.subGenus)) vote += 2;
			else if ((this.subGenus.startsWith(tn.subGenus) && (tn.subGenus.length() < 3)) || (tn.subGenus.startsWith(this.subGenus) && (this.subGenus.length() < 3))) vote += 1;
			else return false;
			firstExplicitPart = true;
		} else if ((this.subGenus != tn.subGenus) && !firstExplicitPart) return false;
		
		if ((this.genus != null) && (tn.genus != null)) {
			if (this.genus.equals(tn.genus)) vote += 2;
			else if ((this.genus.startsWith(tn.genus) && (tn.genus.length() < 3)) || (tn.genus.startsWith(this.genus) && (this.genus.length() < 3))) vote += 1;
			else return false;
		} else if ((this.genus != tn.genus) && !firstExplicitPart) return false;
		
		return (vote > 2);
	}
}
