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

package de.uka.ipd.idaho.plugins.geoCoding.geoDataProviders;


import java.net.URLEncoder;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.XPath;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNodeSet;

public class GeonamesTest {
	
	static XPath resPath = new XPath("/geonames/geoname");//[./fcl = 'P' or ./fcl = 'A']");
	static XPath namePath = new XPath("/name");
	static XPath longPath = new XPath("/lng");
	static XPath latPath = new XPath("/lat");
	
	public static void main(String[] args) throws Exception {
		String query = "krk";
		String url = "http://ws.geonames.org/search?name_equals=";
		TreeNode root = IoTools.getAndParsePage(url + URLEncoder.encode(query, "UTF-8"));
		System.out.println(root.treeToCode());
		XPathNodeSet resNodes = resPath.evaluate(root, new Properties());
		
		if (resNodes.size() == 0) {
			System.out.print(root.treeToCode());
		} else for (int r = 0; r < resNodes.size(); r++) {
			TreeNode resRoot = resNodes.get(r);
			String name = XPath.evaluatePath(namePath, resRoot, new Properties()).asString().value;
			String longitude = XPath.evaluatePath(longPath, resRoot, new Properties()).asString().value;
			String latitude = XPath.evaluatePath(latPath, resRoot, new Properties()).asString().value;
			System.out.println("Location: '" + name + "' (" + longitude + "/" + latitude + ")");
		}
	}
}
