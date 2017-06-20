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

package de.uka.ipd.idaho.plugins.taxonomicNames.srs;


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.goldenGateServer.srs.webPortal.SearchPortalConstants.SearchResultLinker;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;

/**
 * 
 * @author sautter
 */
public class LsidProviderLinker extends SearchResultLinker implements TaxonomicNameConstants {
	
	private static final String PROVIDER_SETTINGS_PREFIX = "PROVIDER_";
	private static final String PROVIDER_CODE_SETTING = "CODE";
	private static final String PROVIDER_NAME_SETTING = "NAME";
	private static final String PROVIDER_LSID_PREFIX_SETTING = "LSID_PREFIX";
	private static final String CUT_LSID_PREFIX_SETTING = "CUT_LSID_PREFIX";
	private static final String PROVIDER_BASE_URL_SETTING = "BASE_URL";
	
	private class LsidProviderLinkData {
		private String code;
		private String name;
		private String lsidPrefix;
		private boolean cutLsidPrefix = false;
		private String baseUrl;
		
		private LsidProviderLinkData(String code, String name, String lsidPrefix, boolean cutLsidPrefix, String baseUrl) {
			this.code = code;
			this.name = name;
			this.lsidPrefix = lsidPrefix;
			this.cutLsidPrefix = cutLsidPrefix;
			this.baseUrl = baseUrl;
		}
	}
	
	private LsidProviderLinkData[] providers = new LsidProviderLinkData[0];
	
	private LsidProviderLinkData getProviderForLSID(String lsid) {
		int maxPrefixMatchLength = 0;
		LsidProviderLinkData maxPrefixMatchProvider = null;
		for (int p = 0; p < this.providers.length; p++) {
			if (lsid.startsWith(this.providers[p].lsidPrefix) && (this.providers[p].lsidPrefix.length() > maxPrefixMatchLength)) {
				maxPrefixMatchLength = this.providers[p].lsidPrefix.length();
				maxPrefixMatchProvider = this.providers[p];
			}
		}
		return maxPrefixMatchProvider;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.srs.webPortal.SearchPortalConstants.SearchResultLinker#init()
	 */
	protected void init() {
		Settings settings = Settings.loadSettings(new File(this.dataPath, ("config.cnfg")));
		
		ArrayList providerList = new ArrayList();
		for (int p = 0; p < settings.size(); p++) {
			Settings providerSettings = settings.getSubset(PROVIDER_SETTINGS_PREFIX + p);
			if (providerSettings.size() >= 4)
				providerList.add(new LsidProviderLinkData(
					providerSettings.getSetting(PROVIDER_CODE_SETTING),
					providerSettings.getSetting(PROVIDER_NAME_SETTING),
					providerSettings.getSetting(PROVIDER_LSID_PREFIX_SETTING),
					CUT_LSID_PREFIX_SETTING.equals(providerSettings.getSetting(CUT_LSID_PREFIX_SETTING, "")),
					providerSettings.getSetting(PROVIDER_BASE_URL_SETTING)
				));
		}
		this.providers = ((LsidProviderLinkData[]) providerList.toArray(new LsidProviderLinkData[providerList.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.goldenGateSrs.webPortal.SearchResultLinker#getName()
	 */
	public String getName() {
		return "LSID Source Linker";
	}
	
	/* (non-Javadoc)
	 * @see de.goldenGateSrs.webPortal.SearchPortalConstants.SearchResultLinker#getAnnotationLinks(de.gamta.Annotation)
	 */
	public SearchResultLink[] getAnnotationLinks(Annotation annotation) {
		if ((annotation != null) && TAXONOMIC_NAME_ANNOTATION_TYPE.equals(annotation.getType())) {
			ArrayList linkList = new ArrayList();
			HashSet linkedLsids = new HashSet(); 
			String[] ans = annotation.getAttributeNames();
			for (int a = 0; a < ans.length; a++) {
				if (ans[a].equals(LSID_ATTRIBUTE) || ans[a].startsWith(LSID_ATTRIBUTE + "-")) {
					String lsid = annotation.getAttribute(ans[a], "").toString();
					if (lsid.length() != 0) {
						LsidProviderLinkData lpld = this.getProviderForLSID(lsid);
						if ((lpld != null) && linkedLsids.add(lsid))
							linkList.add(new SearchResultLink(
									EXTERNAL_INFORMATION,
									this.getClass().getName(),
									lpld.code, 
									null, // TODO: insert name of icon image
									("Lookup " + annotation.getValue() + " at " + lpld.name),
									(lpld.baseUrl + (lpld.cutLsidPrefix ? lsid.substring(lpld.lsidPrefix.length()) : lsid)), 
									""
								));
					}
				}
			}
			return ((SearchResultLink[]) linkList.toArray(new SearchResultLink[linkList.size()]));
		}
		else return new SearchResultLink[0];
	}
	
	private static final GPath nomenclatureNamePath = new GPath("//subSubSection[./@type = 'nomenclature']//taxonomicName");
	
	/* (non-Javadoc)
	 * @see de.goldenGateSrs.webPortal.SearchPortalConstants.SearchResultLinker#getDocumentLinks(de.gamta.MutableAnnotation)
	 */
	public SearchResultLink[] getDocumentLinks(MutableAnnotation doc) {
		Annotation[] nomenclatureNames = GPath.evaluatePath(doc, nomenclatureNamePath, null);
		return ((nomenclatureNames.length == 0) ? new SearchResultLink[0] : this.getAnnotationLinks(nomenclatureNames[0]));
	}
}