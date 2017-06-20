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

package de.uka.ipd.idaho.plugins.spellChecking;

import java.io.IOException;

import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Specialized analyzer for the spell checker group. This implementation exposes
 * its data provider and utility methods so the SpellChecking main class can use
 * them. Further, it initializes SpellChecking from the initAnalyzer() method,
 * and shuts it down from the exitAnalyzer() method.
 * 
 * @author sautter
 */
public abstract class SpellCheckingAnalyzer extends AbstractConfigurableAnalyzer {
	public void initAnalyzer() {
		SpellChecking.init(this);
	}
	public void exitAnalyzer() {
		SpellChecking.exit();
	}
	public AnalyzerDataProvider getDataProvider() {
		return this.dataProvider;
	}
	public boolean hasParameter(String name) {
		return super.hasParameter(name);
	}
	public String getParameter(String name, String def) {
		return super.getParameter(name, def);
	}
	public String getParameter(String name) {
		return super.getParameter(name);
	}
	public String[] getParameterNames() {
		return super.getParameterNames();
	}
	public String storeParameter(String name, String value) {
		return super.storeParameter(name, value);
	}
	public String removeParameter(String name) {
		return super.removeParameter(name);
	}
	public void clearParameters() {
		super.clearParameters();
	}
	public StringVector loadList(String listName) throws IOException {
		return super.loadList(listName);
	}
	public void storeList(StringVector list, String listName) throws IOException {
		super.storeList(list, listName);
	}
}