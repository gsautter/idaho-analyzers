/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.taxonomicNames.synonyms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

import de.uka.ipd.idaho.gamta.util.AbstractAnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public abstract class SynonymLookupProvider implements TaxonomicNameConstants {
	
	/** the result of a lookup that timed out, to be compared via '==' */
	public static final String[] LOOKUP_TIMEOUT = new String[0];
	
	private static class Base {
		private AnalyzerDataProvider dataProvider;
		private SynonymLookupProvider[] instances = null;
		Base(AnalyzerDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}
		SynonymLookupProvider[] getDataProviders() {
			if (this.instances == null)
				this.instances = this.loadSynonymyProviders();
			return this.instances;
		}
		private SynonymLookupProvider[] loadSynonymyProviders() {
			System.out.println("SynonymLookupProvider: initializing instances");
			
			//	load geo data providers
			Object[] dataProviderObjects = GamtaClassLoader.loadComponents(
					new AbstractAnalyzerDataProvider() {
						public String[] getDataNames() {
							String[] dataNames = dataProvider.getDataNames();
							StringVector dataNameList = new StringVector();
							for (int d = 0; d < dataNames.length; d++) {
								if (dataNames[d].indexOf("cache/") == -1)
									dataNameList.addElementIgnoreDuplicates(dataNames[d]);
							}
							return dataNameList.toStringArray();
						}
						public boolean isDataAvailable(String dataName) {
							return dataProvider.isDataAvailable(dataName);
						}
						public InputStream getInputStream(String dataName) throws IOException {
							return dataProvider.getInputStream(dataName);
						}
						public URL getURL(String dataName) throws IOException {
							return dataProvider.getURL(dataName);
						}
						public boolean isDataEditable() {
							return dataProvider.isDataEditable();
						}
						public boolean isDataEditable(String dataName) {
							return dataProvider.isDataEditable(dataName);
						}
						public OutputStream getOutputStream(String dataName) throws IOException {
							return dataProvider.getOutputStream(dataName);
						}
						public boolean deleteData(String name) {
							return dataProvider.deleteData(name);
						}
						public String getAbsolutePath() {
							return dataProvider.getAbsolutePath();
						}
					}, 
					null, 
					SynonymLookupProvider.class, 
					new ComponentInitializer() {
						public void initialize(Object component, String componentJarName) throws Throwable {
							if (componentJarName == null)
								throw new RuntimeException("Cannot determine data path for " + component.getClass().getName());
							SynonymLookupProvider gdp = ((SynonymLookupProvider) component);
							componentJarName = componentJarName.substring(0, componentJarName.lastIndexOf('.')) + "Data";
							gdp.setDataProvider(new PrefixDataProvider(dataProvider, componentJarName));
						}
					});
			
			//	store & return geo data providers
			SynonymLookupProvider[] dataProviders = new SynonymLookupProvider[dataProviderObjects.length];
			for (int c = 0; c < dataProviderObjects.length; c++)
				dataProviders[c] = ((SynonymLookupProvider) dataProviderObjects[c]);
			return dataProviders;
		}
	}
	
	private static final HashMap bases = new HashMap();
	private static Base getBase(AnalyzerDataProvider dataProvider) {
		Base base = ((Base) bases.get(dataProvider.getAbsolutePath()));
		if (base == null) {
			base = new Base(dataProvider);
			bases.put(dataProvider.getAbsolutePath(),  base);
		}
		return base;
	}
	
	/**
	 * Retrieve all higher taxonomy providers currently available in a given
	 * folder, which is represented by an analyzer data provider.
	 * @param dataProvider the data provider to use for loading the higher
	 *            taxonomy providers (if not done yet)
	 * @return all higher taxonomy providers currently available
	 */
	public static SynonymLookupProvider[] getSynonymyProviders(AnalyzerDataProvider dataProvider) {
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
	 * Retrieve an integrated higher taxonomy provider wrapping all higher
	 * taxonomy providers currently available in a given folder, which is
	 * represented by an analyzer data provider.
	 * @param dataProvider the data provider to use for loading the higher
	 *            taxonomy providers (if not done yet)
	 * @return an integrated wrapper around all higher taxonomy providers
	 *            currently available
	 */
	public static SynonymLookupProvider getSynonymyProvider(AnalyzerDataProvider dataProvider) {
		return integrateSynonymyProviders(getSynonymyProviders(dataProvider), dataProvider);
	}
	
	/**
	 * Create an integrated higher taxonomy provider from multiple others. If
	 * the argument array is empty, this method returns null. If the array only
	 * contains a single higher taxonomy provider, it returns the latter.
	 * Otherwise, it returns a higher taxonomy provider that delegates requests
	 * to the argument ones and integrates the results. The integrated higher
	 * taxonomy provider does not do any caching itself.
	 * @param taxonomyProviders the higher taxonomy providers to integrate
	 * @return an integrated higher taxonomy provider
	 */
	public static SynonymLookupProvider integrateSynonymyProviders(SynonymLookupProvider[] taxonomyProviders) {
		return integrateSynonymyProviders(taxonomyProviders, null);
	}
	
	/**
	 * Create an integrated higher taxonomy provider from multiple others. If
	 * the argument array is empty, this method returns null. If the array only
	 * contains a single higher taxonomy provider, it returns the latter.
	 * Otherwise, it returns a higher taxonomy provider that delegates requests
	 * to the argument ones and integrates the results. If the argument data
	 * provider is null, the integrated higher taxonomy provider does not do
	 * any caching itself.
	 * @param taxonomyProviders the higher taxonomy providers to integrate
	 * @param dataProvider the data provider to use for caching
	 * @return an integrated higher taxonomy provider
	 */
	public static SynonymLookupProvider integrateSynonymyProviders(SynonymLookupProvider[] taxonomyProviders, AnalyzerDataProvider dataProvider) {
		
		//	nothing to integrate at all
		if (taxonomyProviders.length == 0)
			return null;
		
		//	no use integrating a single provider
		if (taxonomyProviders.length == 1)
			return taxonomyProviders[0];
		
		//	use integrating provider
		return new IntegratedSynonymLookupProvider(taxonomyProviders, dataProvider);
	}
	
	private static class IntegratedSynonymLookupProvider extends SynonymLookupProvider {
		private SynonymLookupProvider[] synonymyProviders;
		private String dataSourceString;
		
		IntegratedSynonymLookupProvider(SynonymLookupProvider[] synonymyProviders, AnalyzerDataProvider dataProvider) {
			
			//	copy argument array to block external modification
			this.synonymyProviders = new SynonymLookupProvider[synonymyProviders.length];
			System.arraycopy(synonymyProviders, 0, this.synonymyProviders, 0, synonymyProviders.length);
			
			//	assemble integrated data source name
			StringBuffer dataSourceStringBuilder = new StringBuffer(this.synonymyProviders[0].getDataSourceName());
			for (int p = 1; p < this.synonymyProviders.length; p++) {
				dataSourceStringBuilder.append(",");
				dataSourceStringBuilder.append(this.synonymyProviders[p].getDataSourceName());
			}
			this.dataSourceString = dataSourceStringBuilder.toString();
			
			//	set data provider
			this.dataProvider = dataProvider;
		}
		
		public String getDataSourceName() {
			return this.dataSourceString;
		}
		
		public void setLookupTimeout(int lt) {
			for (int p = 0; p < this.synonymyProviders.length; p++)
				this.synonymyProviders[p].setLookupTimeout(lt);
			super.setLookupTimeout(lt);
		}
		
		public void setProbabilisticCacheRefreshEnabled(boolean pcre) {
			for (int p = 0; p < this.synonymyProviders.length; p++)
				this.synonymyProviders[p].setProbabilisticCacheRefreshEnabled(pcre);
			super.setProbabilisticCacheRefreshEnabled(pcre);
		}
		
		protected void exit() {
			for (int p = 0; p < this.synonymyProviders.length; p++)
				this.synonymyProviders[p].shutdown();
		}
		
		public String[] getSynonyms(String epithet, boolean allowWebAccess) {
			return this.loadSynonyms(epithet); // ALWAYS bypass own cache (on each lookup, we might get a timeout on _some_ wrapped provider, but not all, so we need to re-get the results each time)
		}
		
		protected String[] loadSynonyms(String taxName) {
			
			//	try without web access first, basically probing caches of wrapped providers
			final LinkedHashSet synonymsList = new LinkedHashSet();
			for (int p = 0; p < this.synonymyProviders.length; p++) {
				String[] spSynonyms = this.synonymyProviders[p].getSynonyms(taxName, false);
				if ((spSynonyms != null) && (spSynonyms.length != 0))
					synonymsList.addAll(Arrays.asList(spSynonyms));
			}
			
			//	any luck?
			if (synonymsList.size() != 0)
				return ((String[]) synonymsList.toArray(new String[synonymsList.size()]));
			
			//	start request for synonyms to wrapped providers
			SynonymRequest[] htpRequests = new SynonymRequest[this.synonymyProviders.length];
			for (int p = 0; p < this.synonymyProviders.length; p++) {
				htpRequests[p] = new SynonymRequest(this.synonymyProviders[p], taxName);
				this.getHtpRequestHandler().handleHtpRequest(htpRequests[p]);
			}
			
			//	collect lookup results
			boolean lookupTimeout = false;
			for (int p = 0; p < htpRequests.length; p++) {
				String[] spSynonyms = htpRequests[p].getSynonyms();
				if (spSynonyms == LOOKUP_TIMEOUT)
					lookupTimeout = true;
				else if (spSynonyms != null)
					synonymsList.addAll(Arrays.asList(spSynonyms));
			}
			
			//	aggregate individual lookup results, bundle aggregates and hand them back
			return ((synonymsList.isEmpty() && lookupTimeout) ? LOOKUP_TIMEOUT : ((String[]) synonymsList.toArray(new String[synonymsList.size()])));
		}
		
		private LinkedList synonymRequestHandlers = new LinkedList();
		
		private SynonymRequestHandler getHtpRequestHandler() {
			synchronized (this.synonymRequestHandlers) {
				if (this.synonymRequestHandlers.isEmpty())
					return new SynonymRequestHandler();
				else return ((SynonymRequestHandler) this.synonymRequestHandlers.removeFirst());
			}
		}
		
		private class SynonymRequestHandler extends Thread {
			private SynonymRequest sr = null;
			SynonymRequestHandler() {
				this.start();
			}
			public synchronized void run() {
				while (true) {
					
					//	wait for next request (if not already present, can happen on startup)
					if (this.sr == null) try {
						this.wait();
					} catch (InterruptedException ie) {}
					
					//	are we shutting down?
					if (this.sr == null)
						return;
					
					//	handle request
					System.out.println(this.sr.sp.getDataSourceName() + ": starting asynchronous lookup for '" + this.sr.taxName + "'");
					String[] synonyms = this.sr.sp.getSynonyms(this.sr.taxName, true);
					this.sr.setSynonyms(synonyms);
					
					//	clean up
					this.sr = null;
					
					//	return to pool (only if we're below thrice the number of wrapped providers)
					synchronized (synonymRequestHandlers) {
						if (synonymRequestHandlers.size() < (synonymyProviders.length * 3))
							synonymRequestHandlers.addLast(this);
						else return;
					}
				}
			}
			synchronized void handleHtpRequest(SynonymRequest htpr) {
				this.sr = htpr;
				this.notify();
			}
		}
		
		private class SynonymRequest {
			final SynonymLookupProvider sp;
			final String taxName;
			private boolean awaitingSynonyms = true;
			private String[] synonyms = LOOKUP_TIMEOUT;
			
			SynonymRequest(SynonymLookupProvider htp, String taxName) {
				this.sp = htp;
				this.taxName = taxName;
			}
			
			synchronized void setSynonyms(String[] synonyms) {
				this.synonyms = synonyms;
				this.awaitingSynonyms = false;
				this.notify();
			}
			
			synchronized String[] getSynonyms() {
				if (this.awaitingSynonyms) try {
					this.wait(lookupTimeout);
				} catch (InterruptedException ie) {}
				if (this.awaitingSynonyms)
					System.out.println(this.sp.getDataSourceName() + ": asynchronous lookup timed out");
				else System.out.println(this.sp.getDataSourceName() + ": asynchronous lookup done");
				return this.synonyms;
			}
		}
	}
	
	/** the data provider to use */
	protected AnalyzerDataProvider dataProvider;
	
	/** the timeout for lookups at the wrapped data source, in milliseconds (5 seconds by default) */
	protected int lookupTimeout = (5 * 1000);
	
	private boolean probabilisticCacheRefresh = false;
	
	/**
	 * @return the name of the synonym lookup provider
	 */
	public String getName() {
		String name = this.getClass().getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		return name;
	}
	
	/**
	 * @return the lookup timeout (in milliseconds)
	 */
	public int getLookupTimeout() {
		return this.lookupTimeout;
	}
	
	/**
	 * Set the timeout for lookups to the underlying data source. Setting the
	 * timeout to a negative number effectively deactivates lookup timeouts, so
	 * the higher taxonomy provider waits as long as it takes for the underlying
	 * data source to respond.
	 * @param lt the lookup timeout to set (in milliseconds)
	 */
	public void setLookupTimeout(int lt) {
		this.lookupTimeout = lt;
	}
	
	/**
	 * @return is probabilistic cache refresh enabled?
	 */
	public boolean isProbabilisticCacheRefreshEnabled() {
		return this.probabilisticCacheRefresh;
	}
	
	/**
	 * Enable or disable probabilistic cache refreshing. Enabling probabilistic
	 * refreshing will cause the synonym lookup provider to ignore cache hits
	 * with a 5% probability and re-fetch the data from the underlying source.
	 * This is helpful with data sources that are subject to updates themselves
	 * and thus need their cached data updated occasionally. 
	 * @param pcre enable probabilistic cache refresh?
	 */
	public void setProbabilisticCacheRefreshEnabled(boolean pcre) {
		this.probabilisticCacheRefresh = pcre;
	}
	
	/**
	 * @return the name of the underlying data source
	 */
	public abstract String getDataSourceName();
	
	/**
	 * give the synonym lookup provider access to its data
	 * @param dataProvider the data provider
	 */
	public final void setDataProvider(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
		
		//	do implementation specific initialization
		this.init();
	}
	
	/**
	 * Initialize the higher taxonomy provider. This method is called after the
	 * <code>setDataProvider()</code> method. This default implementation does
	 * nothing, sub classes are welcome to overwrite it as needed.
	 */
	protected void init() {}
	
	/**
	 * Shut down the higher taxonomy provider, close connections, etc.
	 */
	public final void shutdown() {
		
		//	clear cache
		this.cache.clear();
		
		//	do implementation specific shutdown
		this.exit();
	}
	
	/**
	 * Shut down the higher taxonomy provider.  This method is called by the
	 * <code>shutdown()</code> method. This default implementation does
	 * nothing, sub classes are welcome to overwrite it as needed.
	 */
	protected void exit() {}
	
	private Map cache = Collections.synchronizedMap(new HashMap());
	
	/**
	 * Obtain the synonyms of a given taxonomic name (binomial or trinomial).
	 * If the backing data source does not provide any synonyms for the
	 * specified taxonomic name, or if the backing data source is unreachable
	 * and the synonyms of the specified taxonomic name are not cached, this
	 * method returns null.
	 * @param taxName the taxonomic name to get the synonyms for
	 * @param allowWebAccess allow downloading data from the backing source in
	 *            case of a file cache miss?
	 * @return an array containing the synonyms of the argument taxonomic name
	 */
	public String[] getSynonyms(String taxName, boolean allowWebAccess) {
		System.out.println(this.getDataSourceName() + ": getting synonyms for '" + taxName + "', web access is " + allowWebAccess);
		
		//	generate cache parameters
		String cacheDataName = ("cache/" + taxName.replaceAll("[^A-Za-z0-9]", "_") + ".txt");
		boolean doCacheLookup = (!this.probabilisticCacheRefresh || (0.05 <= Math.random()));
		
		//	check in-memory cache first
		String[] synonyms = (doCacheLookup ? ((String[]) this.cache.get(taxName)) : null);
		if (synonyms != null) {
			System.out.println(this.getDataSourceName() + ": Memory cache hit for '" + taxName + "'");
			return synonyms;
		}
		
		//	check disc cache
		final LinkedHashSet synonymsList = new LinkedHashSet();
		if (doCacheLookup && this.dataProvider.isDataAvailable(cacheDataName)) try {
			BufferedReader cacheReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8"));
			String line;
			while ((line = cacheReader.readLine()) != null)
				synonymsList.add(line);
			cacheReader.close();
			synonyms = ((String[]) synonymsList.toArray(new String[synonymsList.size()]));
			System.out.println(this.getDataSourceName() + ": Disc cache hit for '" + taxName + "'");
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": Error loading cached data for '" + taxName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			synonyms = null;
		}
		
		//	check disc cache lookup success
		if ((synonyms != null) && (synonyms.length != 0)) {
			this.cache.put(taxName, synonyms);
			return synonyms;
		}
		
		//	load from backing provider
		if (allowWebAccess) try {
			synonyms = this.loadSynonyms(taxName);
			if ((synonyms == null) || (synonyms.length == 0)) {
				System.out.println(this.getDataSourceName() + ": Synonyms not found for '" + taxName + "'");
				return synonyms;
			}
			else System.out.println(this.getDataSourceName() + ": Synonyms loaded for '" + taxName + "'");
		}
		catch (ConnectException ce) {
			System.out.println(this.getDataSourceName() + ": Timeout loading data for '" + taxName + "': " + ce.getMessage());
			ce.printStackTrace(System.out);
			return LOOKUP_TIMEOUT;
		}
		catch (SocketTimeoutException ste) {
			System.out.println(this.getDataSourceName() + ": Timeout loading data for '" + taxName + "': " + ste.getMessage());
			ste.printStackTrace(System.out);
			return LOOKUP_TIMEOUT;
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": Error loading data for '" + taxName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		catch (Exception e) {
			System.out.println(this.getDataSourceName() + ": Error loading data for '" + taxName + "': " + e.getMessage());
			e.printStackTrace(System.out);
			return null;
		}
		
		//	little we can do
		if ((synonyms == null) || (synonyms.length == 0))
			return synonyms;
		
		//	cache lookup result on disc
		if (this.dataProvider.isDataEditable()) try {
			BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(cacheDataName), "UTF-8"));
			for (int s = 0; s < synonyms.length; s++) {
				cacheWriter.write(synonyms[s]);
				cacheWriter.newLine();
			}
			cacheWriter.flush();
			cacheWriter.close();
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": Error caching data for '" + taxName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		
		//	put lookup result in in-memory cache
		this.cache.put(taxName, synonyms);
		
		//	finally ...
		return synonyms;
	}
	
	/**
	 * Obtain the synonyms of a given taxonomic name (binomial or trinomial)
	 * from the backing source.
	 * @param taxName the taxonomic name to get the synonyms for
	 * @return an array containing the synonyms of the argument taxonomic name
	 * @throws IOException
	 */
	protected abstract String[] loadSynonyms(String taxName) throws IOException;
	
	//	TODO add LookupListener argument, to receive notification of a successful lookup
	//	TODO ==> use that for "give me some more time" notification in transitive CoL lookups
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SynonymLookupProvider sp = SynonymLookupProvider.getSynonymyProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/SynonymyData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
	}
}
