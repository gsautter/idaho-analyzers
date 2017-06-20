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

package de.uka.ipd.idaho.plugins.taxonomicNames.lsidReferencer;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorPanel;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * Communication interface for a specific LSID provider
 * 
 * @author sautter
 */
public abstract class LsidDataProvider implements TaxonomicNameConstants {
	
	private static class Base {
		private AnalyzerDataProvider dataProvider;
		private LsidDataProvider[] instances = null;
		Base(AnalyzerDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}
		LsidDataProvider[] getDataProviders() {
			if (this.instances == null)
				this.instances = this.loadDataProviders();
			return this.instances;
		}
		private LsidDataProvider[] loadDataProviders() {
			System.out.println("LsidReferencer: initializing data providers");
			
			//	load LSID data providers
			Object[] dataProviderObjects = GamtaClassLoader.loadComponents(
					this.dataProvider, 
					null, 
					LsidDataProvider.class, 
					new ComponentInitializer() {
						public void initialize(Object component, String componentJarName) throws Throwable {
							if (componentJarName == null)
								throw new RuntimeException("Cannot determine data path for " + component.getClass().getName());
							LsidDataProvider ldp = ((LsidDataProvider) component);
							componentJarName = componentJarName.substring(0, componentJarName.lastIndexOf('.')) + "Data";
							ldp.setDataProvider(new PrefixDataProvider(dataProvider, componentJarName));
						}
					});
			
			//	store & return LSID data providers
			LsidDataProvider[] dataProviders = new LsidDataProvider[dataProviderObjects.length];
			for (int c = 0; c < dataProviderObjects.length; c++)
				dataProviders[c] = ((LsidDataProvider) dataProviderObjects[c]);
			return dataProviders;
		}
	}
	
	private static final HashMap bases = new HashMap();
	private static Base getBase(AnalyzerDataProvider dataProvider) {
		Base base = ((Base) bases.get(dataProvider));
		if (base == null) {
			base = new Base(dataProvider);
			bases.put(dataProvider,  base);
		}
		return base;
	}
	
	static TaxonomicRankSystem rankSystem = TaxonomicRankSystem.getRankSystem(null);
	static Rank[] ranks = rankSystem.getRanks();
	static int genusIndex = 1;
	static {
		for (int r = 0; r < ranks.length; r++)
			if (GENUS_ATTRIBUTE.equals(ranks[r].name)) {
				genusIndex = r;
				break;
			}
	}
	
	/**
	 * Retrieve all LSID data providers currently available
	 * @param dataProvider the data provider to use for loading the LSID data
	 *            providers (if not done yet)
	 * @return all LSID data providers currently available
	 */
	public static LsidDataProvider[] getDataProviders(AnalyzerDataProvider dataProvider) {
		Base base = getBase(dataProvider);
		return base.getDataProviders();
	}
	
	private static class PrefixDataProvider extends AbstractAnalyzerDataProvider {
		private AnalyzerDataProvider dataProvider;
		private String pathPrefix;
		private PrefixDataProvider(AnalyzerDataProvider dataProvider, String pathPrefix) {
			this.dataProvider = dataProvider;
			this.pathPrefix = (pathPrefix.endsWith("/") ? pathPrefix : (pathPrefix + "/"));
		}
		public boolean deleteData(String name) {
			return this.dataProvider.isDataAvailable(this.addPrefix(name));
		}
		public String[] getDataNames() {
			String[] names = this.dataProvider.getDataNames();
			StringVector list = new StringVector();
			for (int n = 0; n < names.length; n++)
				if (names[n].startsWith(this.pathPrefix))
					list.addElementIgnoreDuplicates(names[n].substring(this.pathPrefix.length()));
			return list.toStringArray();
		}
		public InputStream getInputStream(String dataName) throws IOException {
			return this.dataProvider.getInputStream(this.addPrefix(dataName));
		}
		public OutputStream getOutputStream(String dataName) throws IOException {
			return this.dataProvider.getOutputStream(this.addPrefix(dataName));
		}
		public URL getURL(String dataName) throws IOException {
			return this.dataProvider.getURL((dataName.indexOf("://") == -1) ? this.addPrefix(dataName) : dataName);
		}
		public boolean isDataAvailable(String dataName) {
			return this.dataProvider.isDataAvailable(this.addPrefix(dataName));
		}
		public boolean isDataEditable() {
			return this.dataProvider.isDataEditable();
		}
		public boolean isDataEditable(String dataName) {
			return this.dataProvider.isDataEditable(this.addPrefix(dataName));
		}
		private String addPrefix(String name) {
			return (this.pathPrefix + (name.startsWith("/") ? name.substring(1) : name));
		} 
		public String getAbsolutePath() {
			return this.dataProvider.getAbsolutePath() + "/" + this.pathPrefix.substring(0, (this.pathPrefix.length() - 1));
		}
	}
	
	/**
	 * representation of a single LSID record
	 * 
	 * @author sautter
	 */
	public static class LsidDataSet implements Comparable {
		
		/** the LSID data provider this data set comes from (for obtaining the URN prefix, and provider name and code) */
		public final LsidDataProvider provider;
		
		/** the provider internal number of this LSID (the LSID without the provider specific URN prefix) */
		public final String lsidNumber;
		
		/** the taxon name String the LSID resolved to (for matching purposes) */
		public final String taxonName;
		
		/** the rank assigned to the taxon by the provider (for matching purposes) */
		public final String rank;
		
		// the score of the data set with regard to a specific taxon name, used internally 
		final int score;
		
		/**	Constructor
		 * @param	provider	the LSID data provider this data set comes from (for obtaining the URN prefix, and provider name and code)
		 * @param	lsidNumber	the provider internal number of this LSID (the LSID without the provider specific URN prefix)
		 * @param	taxonName	the taxon name String the LSID resolved to (for matching purposes)
		 * @param	rank		the rank assigned to the taxon by the provider (for matching purposes)
		 */
		public LsidDataSet(LsidDataProvider provider, String lsidNumber, String taxonName, String rank) {
			this(provider, lsidNumber, taxonName, rank, 0);
		}
		
		private LsidDataSet(LsidDataProvider provider, String lsidNumber, String taxonName, String rank, int score) {
			this.provider = provider;
			this.lsidNumber = lsidNumber;
			this.taxonName = taxonName;
			this.rank = ((rank == null) ? "" : rank);
			this.score = score;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o == null) return -1;
			if (o instanceof LsidDataSet) {
				return (((LsidDataSet) o).score - this.score);
			} else return -1;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return (this.lsidNumber + ": " + this.taxonName + " (" + this.provider.getProviderCode() + ", " + this.score + ")");
		}
		
		LsidDataSet getScoredCopy(int score) {
			return new LsidDataSet(this.provider, this.lsidNumber, this.taxonName, this.rank, Math.abs(score)); // make internal score positive
		}
		
		/**
		 * Obtain a score indicating how well the taxonomic name represented by
		 * the data set matches the taxonomic name represented by a given
		 * annotation.
		 * @param taxonomicName the taxonomic name annotation to match against
		 * @param allowFuzzyMatch allow fuzzy matching?
		 * @return a score indicating the match success
		 */
		public int getMatchScore(Annotation taxonomicName, boolean allowFuzzyMatch) {
			boolean debug = (DEBUG_MATCH && "brevirostris".equals(taxonomicName.getAttribute(TaxonomicNameConstants.SPECIES_ATTRIBUTE)));
			
			if (debug) System.out.println(taxonomicName.toXML());
			if (debug) System.out.println(this.toString());
			
			//	compare name details
			StringVector nameParts = TokenSequenceUtils.getTextTokens(taxonomicName.getTokenizer().tokenize(this.taxonName));
			if (debug) System.out.println(nameParts.concatStrings(", "));
			
			int score = 0;
			boolean gotMostSignificantPart = false;
			int misses = 0;
//			for (int r = 5; r > 0; r--) {
//				String epithet = ((String) taxonomicName.getAttribute(TaxonomicNameConstants.PART_NAMES[r - 1]));
			for (int r = (ranks.length - genusIndex); r > 0; r--) {
//				String epithet = ((String) taxonomicName.getAttribute(TaxonomicNameConstants.PART_NAMES[r - 1]));
				String epithet = ((String) taxonomicName.getAttribute(ranks[r-1].name));
				if (epithet == null)
					continue;
				
//				if (debug) System.out.println(TaxonomicNameConstants.PART_NAMES[r - 1] + ": " + epithet);
				if (debug) System.out.println(ranks[r-1].name + ": " + epithet);
				
				//	normalize attribute
				if (Gamta.isUpperCaseWord(epithet))
					epithet = (
							(r < 2)
							?
							(epithet.substring(0, 1) + epithet.substring(1).toLowerCase())
							:
							epithet.toLowerCase()
						);
				
				if (nameParts.contains(epithet)) {
					score += (100 * r);
					if (debug) System.out.println("==> match, score is " + score);
					nameParts.remove(epithet);
				}
				
				else if (allowFuzzyMatch) {
					
					//	do fuzzy match
					int minDist = this.taxonName.length();
					String minDistPart = "";
					for (int n = 0; n < nameParts.size(); n++) {
						int dist = StringUtils.getLevenshteinDistance(epithet, nameParts.get(n), 1, true);
						if (dist < minDist) {
							minDist = dist;
							minDistPart = nameParts.get(n);
						}
					}
					
					if (minDist <= 1) {
						score += ((100 * r) / (minDist + 1));
						if (debug) System.out.println("==> fuzzy match (" + minDist + "), score is " + score);
						nameParts.remove(minDistPart);
					}
					
					else {
						if (debug) System.out.println("==> no match");
						if (!gotMostSignificantPart) {
							if (debug) System.out.println("==> match impossible");
							return 0;
						}
						else misses++;
					}
				}
				
				else {
					if (debug) System.out.println("==> no match");
					if (!gotMostSignificantPart) {
						if (debug) System.out.println("==> match impossible");
						return 0;
					}
					else misses++;
				}
				
				gotMostSignificantPart = true;
			}
			
			//	in subSpecies & varieties, problems with labels & author names (nameParts.size() too large due to these parts)
//				score = (nameParts.isEmpty() ? (score * 2) : (score / nameParts.size()));
			
			//	in species, problems with subSpecies & varieties with same last part (score too high)
//				score = (nameParts.isEmpty() ? (score * 2) : (score));
			
			//	in species, problems with HNS subSpecies & varieties (remaining of latter not penalized)
//				score = (score / (misses + 1));
			
			//	problems with subSpecies & varieties upgraded to species
//				score = (score / (nameParts.size() + 1));
			
			//	seems to work
			int finalScore = (score / (nameParts.size() + misses + 1));
			
			return ((misses == 0) ? finalScore : -finalScore); // use negative numbers to indicate insecure matches, dirty but only way to return score and secure match flag without container objects
		}
		private static final boolean DEBUG_MATCH = true;
	}
	
	/**
	 * Monitoring dialog for LSID referencing. Though named as a dialog, this
	 * class extends JPanel in order to be able to produce the actual dialog
	 * internally. This is in favor of smoother integration with other GUI
	 * components.
	 * 
	 * @author sautter
	 */
	public static class StatusDialog implements ProgressMonitor {
		
		private ProgressMonitorPanel pmp;
		
		JCheckBox online = new JCheckBox("Allow Online Lookup", true);
		boolean interrupted = false;
		
		private Thread thread = null;
		private JDialog dialog = DialogFactory.produceDialog("LSID Referencer", true);
		
		StatusDialog() {
			this.pmp = new ProgressMonitorPanel(false, false);
			
			JButton stopButton = new JButton("Stop LSID Referencing");
			stopButton.setBorder(BorderFactory.createRaisedBevelBorder());
			stopButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (JOptionPane.showConfirmDialog(dialog, "Do you really want to stop the LSID referencer?", "Confirm Stop LSID Referencer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
						interrupted = true;
				}
			});
			JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			controlPanel.add(this.online);
			controlPanel.add(stopButton);
			
			this.dialog.getContentPane().setLayout(new BorderLayout());
			this.dialog.getContentPane().add(this.pmp, BorderLayout.CENTER);
			this.dialog.getContentPane().add(controlPanel, BorderLayout.CENTER);
			this.dialog.setSize(400, 160);
			this.dialog.setLocationRelativeTo(null);
			this.dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			this.dialog.addWindowListener(new WindowAdapter() {
				boolean isInvoked = false;
				public void windowClosing(WindowEvent we) {
					if (this.isInvoked) return; // avoid loops
					this.isInvoked = true;
					if (JOptionPane.showConfirmDialog(dialog, "Closing this status dialog will disable you to monitor LsdiReferencer", "Confirm Close Status Dialog", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.YES_OPTION)
						dialog.dispose();
					this.isInvoked = false;
				}
			});
		}
		public void setStep(String step) {
			this.pmp.setStep(step);
		}
		public void setInfo(String info) {
			this.pmp.setInfo(info);
		}
		public void setBaseProgress(int baseProgress) {
			this.pmp.setBaseProgress(baseProgress);
		}
		public void setMaxProgress(int maxProgress) {
			this.pmp.setMaxProgress(maxProgress);
		}
		public void setProgress(int progress) {
			this.pmp.setProgress(progress);
		}
		
		void popup() {
			if (this.thread != null)
				return;
			this.thread = new Thread() {
				public void run() {
					dialog.setVisible(true);
					thread = null;
				}
			};
			this.thread.start();
			while (!this.dialog.isVisible()) try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {}
		}
		
		public JDialog getDialog() {
			return this.dialog;
		}
		
		public void dispose() {
			this.dialog.dispose();
		}
	}
	
	/** exception to throw if a taxon name upload was cancelled by a user */
	protected static final IOException UPLOAD_CANCELLED = new IOException("Upload Cancelled");
	
	/** exception to throw if a taxon name upload session was cancelled by a user, for indicating to make no further upload attempts for the current document */
	protected static final IOException STOP_UPLOADING = new IOException("Upload Session Stopped");
	
	/** the data provider in which to store data (e.g. for configuration or caching) */
	protected AnalyzerDataProvider dataProvider;
	
	/**	give the LSID data provider access to its data
	 * @param	dataProvider	the data provider
	 */
	public void setDataProvider(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
		this.cacheFileKeys.addContent(CacheFileColumnNames);
		this.loadParameters();
		this.init();
	}
	
	//	constants and data structures for parameter handling
	private static final String PARAMETER_VALUE_SEPARATOR = "|";
	private static final String PARAMETER_ENCODING = "UTF-8";
	
	private Properties parameters;
	private StringVector parameterNames;
	
	/**	load a list of data
	 * @param	listName	the name of the list to load
	 * @return a StringVector containing the data loaded
	 * @throws	IOException if the dataPath is not set, or if any IOException occurrs while loading the list
	 */
	protected StringVector loadList(String listName) throws IOException {
		if (this.dataProvider == null) throw new IOException("Data provider missing.");
		else {
			InputStream is = this.dataProvider.getInputStream(listName);
			StringVector list = StringVector.loadList(is);
			is.close();
			return list;
		}
	}
	
	//	load the parameters
	private void loadParameters() {
		this.parameterNames = new StringVector();
		this.parameters = new Properties();
		
		//	load lines
		StringVector rawLines = null;
		try {
			rawLines = this.loadList(this.getClass().getName() + ".cnfg");
		} catch (IOException ioe) {
			rawLines = new StringVector();
		}
		
		//	parse lines
		for (int l = 0; l < rawLines.size(); l++) {
			String rawLine = rawLines.get(l);
			String name = null;
			String value = null;
			int splitIndex = rawLine.indexOf(PARAMETER_VALUE_SEPARATOR);
			
			//	split line and decode value
			try {
				name = rawLine.substring(0, splitIndex);
				value = URLDecoder.decode(rawLine.substring(splitIndex + 1), PARAMETER_ENCODING);
			}
			catch (Exception e) {
				name = null;
				value = null;
			}
				
			//	store parameter if valid
			if ((name != null) && (value != null)) {
				this.parameterNames.addElementIgnoreDuplicates(name);
				this.parameters.setProperty(name, value);
			}
		}
	}
	
	/**	load a parameter (will be available after the data path is set)
	 * @param	name	the name of the parameter
	 * @return the value of the parameter, or null, if the parameter with the specified name is not set
	 */
	protected String getParameter(String name) {
		return this.getParameter(name, null);
	}
	
	/**	load a parameter (will be available after the data path is set)
	 * @param	name	the name of the parameter
	 * @param	def		the default value to be returned if the parameter is not set
	 * @return the value of the parameter, or def, if the parameter with the specified name is not set
	 */
	protected String getParameter(String name, String def) {
		if (this.parameters == null) return def;
		return this.parameters.getProperty(name, def);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return (this.getProviderName() + " (" + this.getProviderCode() + ")");
	}
	
	/**	initialize the LSID data provider (called after setDataProvider())
	 * Note: This default implementation does nothing, sub classes are welcome to overwrite it as needed
	 */
	protected void init() {}
	
	/**	@return	the three letter code of this LSID provider, e.g. 'HNS' for 'Hymenoptera Name Server', 'IFG' for 'Index Fungorum', or 'FSB' for 'FishBase'
	 */
	public abstract String getProviderCode();
	
	/**	@return	the name of this LSID provider, e.g. 'Hymenoptera Name Server', 'Index Fungorum', or 'FishBase'
	 */
	public abstract String getProviderName();
	
	/**	@return	the prefix to add to LSID numbers to make them fully qualified, resolvable URNs
	 */
	public abstract String getLsidUrnPrefix();
	
	
	
	private static final String LsidColumnName = "LSID";
	private static final String TaxonNameColumnName = "TaxonName";
	private static final String RankColumnName = "Rank";
	
	private static final String[] CacheFileColumnNames = {LsidColumnName, TaxonNameColumnName, RankColumnName};
	private StringVector cacheFileKeys = new StringVector();
	
	private HashMap cache = new HashMap();
	
	LsidDataSet[] getLsidData(String epithet, boolean allowCache, StringVector downloadedThisDocument, StatusDialog sd, boolean online) {
		
		//	do cache lookup
		LsidDataSet[] lsidData = ((allowCache || downloadedThisDocument.containsIgnoreCase(epithet))? ((LsidDataSet[]) this.cache.get(this.getProviderCode() + "_" + epithet.toLowerCase())) : null);
		
		//	cache hit
		if (lsidData != null) {
			if (sd != null)
				sd.setInfo(" - Cache Hit");
			return lsidData;
		}
		else if (allowCache && (sd != null))
			sd.setInfo(" - Cache Miss");
		
		//	do file lookup
		if (allowCache || downloadedThisDocument.containsIgnoreCase(epithet)) try {
			lsidData = this.loadLsidData(epithet);
			this.cache.put((this.getProviderCode() + "_" + epithet.toLowerCase()), lsidData);
			if (sd != null)
				sd.setInfo(" - File Hit");
			return lsidData;
		}
		catch (Exception e) {
			if (allowCache && (sd != null))
				sd.setInfo(" - File Miss");
		}
		
		//	check if server lookup allowed
		if (((sd != null) && !sd.online.isSelected()) || ((sd == null) && !online))
			return new LsidDataSet[0];
		
		//	download data
		try {
			if (sd != null)
				sd.setInfo(" - Loading data from provider ...");
			
			if (downloadedThisDocument.containsIgnoreCase(epithet)) {
				lsidData = new LsidDataSet[0];
			}
			else {
				lsidData = this.getLsidData(epithet, true);
				downloadedThisDocument.addElementIgnoreDuplicates(epithet);
			}
			if (lsidData.length == 0) return lsidData;
			
			this.storeLsidData(epithet, lsidData);
			
			if (sd != null)
				sd.setInfo(" --> Got data from provider");
			
			this.cache.put((this.getProviderCode() + "_" + epithet.toLowerCase()), lsidData);
			return lsidData;
		}
		catch (Exception e) {
			if (sd != null)
				sd.setInfo(" --> Provider Lookup Error: " + e.getMessage());
			return null;
		}
		catch (Throwable t) {
			if (sd != null)
				sd.setInfo(" --> Provider Lookup Error: " + t.getMessage());
			t.printStackTrace();
			return null;
		}
	}
	
	private LsidDataSet[] loadLsidData(String epithet) throws IOException {
		InputStream is = this.dataProvider.getInputStream("cache/" + epithet + ".csv");
		StringRelation data = StringRelation.readCsvData(new InputStreamReader(is), '"', true, null);
		is.close();
		
		ArrayList lsidData = new ArrayList();
		for (int r = 0; r < data.size(); r++) {
			StringTupel rowData = data.get(r);
			
			String taxonNameUseID = rowData.getValue(LsidColumnName);
			String taxonName = rowData.getValue(TaxonNameColumnName);
			String rank = rowData.getValue(RankColumnName);
			
			if ((taxonNameUseID != null) && (taxonName != null) && (rank != null))
				lsidData.add(new LsidDataSet(this, taxonNameUseID, taxonName, rank));
		}
		return ((LsidDataSet[]) lsidData.toArray(new LsidDataSet[lsidData.size()]));
	}
	
	private void storeLsidData(String epithet, LsidDataSet[] lsidData) throws IOException {
		if (this.dataProvider.isDataEditable()) {
			StringRelation data = new StringRelation();
			
			for (int d = 0; d < lsidData.length; d++) {
				StringTupel rowData = new StringTupel();
				
				rowData.setValue(LsidColumnName, lsidData[d].lsidNumber);
				rowData.setValue(TaxonNameColumnName, lsidData[d].taxonName);
				if (lsidData[d].rank != null)
					rowData.setValue(RankColumnName, lsidData[d].rank);
				
				data.addElement(rowData);
			}
			
			Writer w = new OutputStreamWriter(this.dataProvider.getOutputStream("cache/" + epithet + ".csv"));
			StringRelation.writeCsvData(w, data, '"', this.cacheFileKeys);
			w.flush();
			w.close();
		}
	}
	
	void updateCache(String keyEpithet, LsidDataSet dataSet) throws IOException {
		
		//	refresh cache file (if exists)
		LsidDataSet[] data = ((LsidDataSet[]) this.cache.get(keyEpithet.toLowerCase()));
		if (data == null) try {
			data = this.loadLsidData(keyEpithet);
		}
		catch (FileNotFoundException fnfe) {
			data = new LsidDataSet[0];
		}
		
		//	add new data set
		LsidDataSet[] newData = new LsidDataSet[data.length + 1];
		System.arraycopy(data, 0, newData, 0, data.length);
		newData[data.length] = dataSet;
		
		//	update cache and file
		this.cache.put(keyEpithet.toLowerCase(), data);
		this.storeLsidData(keyEpithet, newData);
	}
	
	/**
	 * Obtain the LSID data for a taxonomic epithet, like a family or a genus
	 * (epithet will never be below genus level) from the backing server.
	 * @param epithet the epithet to retrieve the data for
	 * @param useWildcard append a wildcard to the serach epithet (if backing
	 *            provider allows it)
	 * @return the LSID data for the specified taxonomic epithet, plus probably
	 *         subordinate taxa in case of wildcard search
	 * @throws IOException if any occurs while communicating with the backing
	 *             data provider service
	 */
	public abstract LsidDataSet[] getLsidData(String epithet, boolean useWildcard) throws IOException;
	
	/**
	 * Test if this data provider allows for uploading new taxon names to the
	 * backing data provider
	 * @return true if and only if taxon name upload is supported
	 */
	public boolean isUploadSupported() {
		return false;
	}
	
	/**
	 * Upload a new taxon name to the backing data provider (may simply return
	 * null or throw an exception if isUploadSupported() returns false).
	 * @param statusDialog the status dialog for informing a user about upload
	 *            progress
	 * @param taxonName the Annotation representing the taxon name to upload
	 * @param isNewTaxon a value of true indicates that the taxon name is
	 *            labeled as a new taxon in the document it comes from
	 * @param parentData the LsidDataSets that possibly represent the parent
	 *            taxon of the taxon to upload
	 * @return the LSID record created for the newly uploaded taxon
	 * @throws IOException if any occurs while uploading the new taxon name to
	 *             the backing provider service
	 */
	public LsidDataSet doUpload(StatusDialog statusDialog, Annotation taxonName, boolean isNewTaxon, LsidDataSet[] parentData) throws IOException {
		return null;
	}
}
