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

package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer.dataProviders;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * @author sautter
 *
 */
public class ZbkSoapTest {
	public static void main(String[] args) throws Exception {
		URL url = new URL("http://dev.zoobank.org/srv/lu.asmx");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		con.setRequestProperty("SOAPAction", "http://zoobank.org/srv/luAct2");
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		out.newLine();
		out.write("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		out.newLine();
		out.write("<soap:Body>");
		out.newLine();
		out.write("<luAct2 xmlns=\"http://zoobank.org/srv/\">");
		out.newLine();
		out.write("<tn>Chromis%</tn>");
		out.newLine();
		out.write("</luAct2>");
		out.newLine();
		out.write("</soap:Body>");
		out.newLine();
		out.write("</soap:Envelope>");
		out.newLine();
		out.flush();
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			Parser p = new Parser();
			TreeNode root = p.parse(in);
			root.treeToCode(new TokenReceiver() {
				public void close() throws IOException {}
				public void storeToken(String token, int treeDepth) throws IOException {
					System.out.println(token);
				}
			});
			in.close();
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace(System.out);
		}
		out.close();
		con.disconnect();
	}
	
	/*
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <luAct xmlns="http://zoobank.org/srv/">
      <tn>string</tn>
    </luAct>
  </soap:Body>
</soap:Envelope>
	 * 
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <luAct2 xmlns="http://zoobank.org/srv/">
      <tn>string</tn>
    </luAct2>
  </soap:Body>
</soap:Envelope> 
	 */
}
