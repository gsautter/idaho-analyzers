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

package de.uka.ipd.idaho.plugins.tableHandling;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;

/**
 * @author sautter
 *
 */
public class TableTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		StringReader sr = new StringReader("<table>" +
//				"<thead>" +
//				"<tr>" +
//				"<th colspan=2>th1</th>" +
//				"<th>th2</th>" +
//				"<th>th3</th>" +
//				"</tr>" +
//				"</thead>" +
//				"<tfoot>" +
//				"<tr>" +
//				"<th colspan=2>tf1</th>" +
//				"<th>tf2</th>" +
//				"<th>tf3</th>" +
//				"</tr>" +
//				"</tfoot>" +
//				"<tr>" +
//				"<td colspan=2>td1.1</td>" +
//				"<td>td1.2</td>" +
//				"<td>td1.3</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td2.1</td>" +
//				"<td colspan=2 rowspan=2>td2.2</td>" +
//				"<td>td2.3</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td3.1</td>" +
//				"<td>td3.2</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td colspan=3 rowspan=2>td4.1</td>" +
//				"<td>td4.2</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td5.1</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td6.1</td>" +
//				"<td>td6.2</td>" +
//				"<td>td6.3</td>" +
//				"<td>td6.4</td>" +
//				"</tr>" +
//				"</table>");
//		StringReader sr2 = new StringReader("<table frame=\"box\" rules=\"all\" border=\"0\"><tr><td><p>abbreviation</p></td><td><p>LXM</p></td><td><p>LXMP</p></td><td><p>LXP</p></td><td><p>LXS</p></td></tr><tr><td><p>location</p></td><td><p>Muellertal,</p><p>Mardelle,</p><p>3 km SW Berdorf,</p><p>7 km WNW Echternach</p></td><td><p>Muellertal,</p><p>Mardelle,</p><p>3 km SW Berdorf,</p><p>7 km WNW Echternach</p></td><td><p>Muellertal,</p><p>Predigtstuhl,</p><p>1 km SW Berdorf,</p><p>6 km WNW Echternach</p></td><td><p>Muellertal,</p><p>„Schluchtwald&quot;,</p><p>3 km SW Berdorf</p><p>7 km WNW Echternach</p></td></tr><tr><td><p>co-ordinates</p></td><td><p>06&deg;19'E 49o48' N</p></td><td><p>06&deg;19'E 49&deg;48'N</p></td><td><p>06&quot;19'E 49o48'N</p></td><td><p>06&deg;19'E 49&deg;48'N</p></td></tr><tr><td><p>exposition</p></td><td><p>&lt;5&deg;</p></td><td><p>-</p></td><td><p>NNW / 5-30&deg;</p></td><td><p>W / 20-40&deg;</p></td></tr><tr><td><p>floristic associations</p></td><td><p>Galio-Fagetum</p></td><td><p>Piceetum</p></td><td><p>Luzulo-Fagetum</p><Galio-Fagetum</p></td></tr><tr><td><p>soil type</p></td><td><p>sandy, somewhat loamy</p></td><td><p>-</p></td><td><p>sandy</p></td><td><p>sandy, somewhat loamy</p></td></tr><tr><td><p>humus type</p></td><td><p>mull-moder to moder</p></td><td><p>-</p></td><td><p>moder</p></td><td><p>mull-moder to moder</p></td></tr><tr><td><p>pH litter (CaCI2)</p></td><td><p>4,2 (3,6-4,0)</p></td><td><p>-</p></td><td><p>3,6 (2,9-4,7)</p></td><td><p>5,3(5,1-5,7)</p></td></tr><tr><td><p>pH upper soil (CaCI2)</p></td><td><p>3,6 (3,3-4,0)</p></td><td><p>-</p></td><td><p>2,9 (2,8-3,2)</p></td><td><p>4,7 (3,8-5,5)</p></td></tr></table>");
//		StringReader sr3 = new StringReader("<table frame=\"box\" rules=\"all\" border=\"0\"><tr><td><p>Site</p></td><td><p>LXM</p></td><td><p>LXMP</p></td><td><p>LXM</p></td><td><p>LXP</p></td><td><p>LXP</p></td><td><p>LXS</p></td><td><p>LXS</p></td></tr><tr><td><p>Date</p></td><td><p>1998</p></td><td><p>1998</p></td><td><p>1999</p></td><td><p>1998</p></td><td><p>1999</p></td><td><p>1998</p></td><td><p>1999</p></td></tr><tr><td><p>Chilopoda</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobiidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius aeruginosas</p></td><td>EmTaCe</td><td><p>0/0/0/1</p></td><td><p>1/0</p></td><td><p>0/1</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius crassipes</p></td><td>EmTaCe</td><td><p>1/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/2</p></td></tr><tr><td><p>Lithobius dentatus</p></td><td>EmTaCe</td><td><p>0/1</p></td><td><p>0/5</p></td><td><p>0/1</p></td><td><p>2/5</p></td><td>EmTaCe</td><td><p>2/0</p></td></tr><tr><td><p>Lithobius macilentus</p></td><td><p>0/1</p></td><td>EmTaCe</td><td><p>2/3/2/0</p></td><td>EmTaCe</td><td><p>1/0/1/0</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius mutabilis</p></td><td>EmTaCe</td><td>EmTaCe</td><td><p>1/0</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius piceus</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/1</p></td></tr><tr><td><p>Lithobius tricuspis</p></td><td><p>2/0</p></td><td>EmTaCe</td><td><p>1/1</p></td><td><p>0/3</p></td><td><p>2/0</p></td><td>EmTaCe</td><td><p>1/0</p></td></tr><tr><td><p>Cryptopidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Cryptops parisi</p></td><td><p>5</p></td><td>EmTaCe</td><td><p>2</p></td><td><p>3</p></td><td><p>6</p></td><td><p>3</p></td><td>EmTaCe</td></tr><tr><td><p>Geophilidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Geophilus alpinus</p></td><td><p>0/0/0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>1/2</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Strigamia acuminata</p></td><td><p>0/1</p></td><td>EmTaCe</td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Strigamia crassipes</p></td><td>EmTaCe</td><td><p>1/0</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Diplopoda</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Glomeridae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Glomeris intermedia</p></td><td><p>4/12</p></td><td><p>1/1</p></td><td><p>2/7</p></td><td><p>2/1</p></td><td><p>0/1</p></td><td><p>0/2</p></td><td><p>1/4</p></td></tr><tr><td><p>Glomeris marginata</p></td><td><p>1/6/0/1</p></td><td><p>0/1</p></td><td><p>2/4 2/2/1</p></td><td><p>3/5</p></td><td><p>2/3/2/0</p></td><td><p>1/8</p></td><td>EmTaCe</td></tr><tr><td><p>Julidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Julus scandinavius</p></td><td><p>0/2</p></td><td>EmTaCe</td><td><p>1/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Leptoiulus simplex</p></td><td><p>0/1</p></td><td>EmTaCe</td><td><p>0/1</p></td><td><p>1/0</p></td><td><p>1/2</p></td><td><p>0/4</p></td><td><p>2/1</p></td></tr><tr><td><p>Allajulus nitidus</p></td><td><p>4/8/0/4</p></td><td><p>4/4</p></td><td><p>10/6/0/1</p></td><td><p>0/3/1/0</p></td><td><p>1/6/0/3</p></td><td><p>3/1</p></td><td>EmTaCe</td></tr><tr><td><p>Cylindroiulus punctatus</p></td><td>EmTaCe</td><td><p>0/1</p></td><td><p>1/0</p></td><td>EmTaCe</td><td><p>1/2/1/0</p></td><td>EmTaCe</td><td><p>0/2/1/0</p></td></tr><tr><td><p>Tachypodoiulus niger</p></td><td><p>1/4/3/0</p></td><td><p>4/8/2/0</p></td><td><p>3/4/1/2</p></td><td><p>3/13/9/1</p></td><td><p>2/5/1/0</p></td><td><p>1/3 0/2</p></td><td>EmTaCe</td></tr><tr><td><p>Craspedosomatidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Craspedosoma rawlinsii alemannicum</p></td><td><p>1/1</p></td><td><p>2/1</p></td><td><p>7/2</p></td><td>EmTaCe</td><td><p>1/4/1</p></td><td><p>1/0</p></td><td>EmTaCe</td></tr><tr><td><p>Chordeumatidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Chordeuma sylvestre</p></td><td><p>1/1</p></td><td>EmTaCe</td><td><p>4/8/2</p></td><td><p>1/3</p></td><td><p>0/2/1</p></td><td><p>0/1</p></td><td>EmTaCe</td></tr><tr><td><p>Melogona gallica</p></td><td><p>1/0/0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Orthochordeumella pallida</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Polydesmidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Polydesmus angustus</p></td><td><p>0/1</p></td><td><p>0/1</p></td><td><p>2/0</p></td><td>EmTaCe</td><td><p>1/1</p></td><td><p>0/1</p></td><td><p>1/1</p></td></tr><tr><td><p>Isopoda</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Ligiidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Ligidium hypnorum</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/2</p></td><td><p>0/1</p></td><td>EmTaCe</td></tr><tr><td><p>Trichoniscidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Trichoniscus pusillus</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/2</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Oniscidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Oniscus asellus</p></td><td>EmTaCe</td><td><p>1/2</p></td><td><p>4/7</p></td><td><p>3/3/1/0</p></td><td><p>3/3 1/1</p></td><td><p>1/0</p></td><td>EmTaCe</td></tr><tr><td><p>Philoscia sp.</p></td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Porcellionidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Porcellio monticola</p></td><td>EmTaCe</td><td><p>1/0</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Porcellium conspersum</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr></table>");
//		final DocumentRoot dr = SgmlDocumentReader.readDocument(sr2);
		final DocumentRoot dr = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/EcologyTestbed/spelda2001.normalized.xml"), "UTF-8"));
		
		AnnotationFilter.removeAnnotations(dr, "p");
//		final AnnotationTable at = AnnotationTable.buildAnnotationTable(AnnotationTable.getMutableAnnotationsIgnoreCase(dr, "table")[0]);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		TableEditor te = new TableEditor();
		TableDataExtractor tde = new TableDataExtractor();
		Properties params = new Properties();
		params.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
		te.process(dr, params);
		tde.process(dr, params);
		
//		AnnotationUtils.writeXML(dr, new PrintWriter(System.out));
		MutableAnnotation[] tableAnnotations = AnnotationTable.getMutableAnnotationsIgnoreCase(dr, "table");
//		AnnotationTable [] tables = new AnnotationTable[tableAnnotations.length];
//		for (int t = 0; t < tableAnnotations.length; t++)
//			tables[t] = AnnotationTable.getAnnotationTable(tableAnnotations[t]);
//		boolean[] isColumnTable = new boolean[tables.length];
//		for (int t = 0; t < tables.length; t++) {
//			isColumnTable[t] = false;
//			for (int c = 0; c < tables[t].getColumnCount(); c++) {
//				if (AnnotationTable.HEADER_CR_TYPE.equals(tables[t].getColumnTypeAt(c)))
//					isColumnTable[t] = true;
//			}
//		}
		for (int t = 0; t < tableAnnotations.length; t++) {
			AnnotationUtils.writeXML(tableAnnotations[t], new PrintWriter(System.out));
//			AnnotationTableCell[] headerCells = tde.getHeaderCells(tables[t], isColumnTable[t]);
//			System.out.println("got " + headerCells.length + " header cells");
//			for (int h = 0; h < headerCells.length; h++) {
//				if (headerCells[h] == null)
//					continue;
////				headerCells[h].getData().copyAttributes(headerCells[h]);
//				System.out.println(headerCells[h].getData().toXML());
//				String[] ans = headerCells[h].getAttributeNames();
//				for (int a = 0; a < ans.length; a++)
//					System.out.println(" - " + ans[a] + " = " + headerCells[h].getAttribute(ans[a]));
//			}
		}
	}
}