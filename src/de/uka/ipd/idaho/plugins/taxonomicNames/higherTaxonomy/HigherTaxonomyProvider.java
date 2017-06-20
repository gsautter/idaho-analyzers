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
package de.uka.ipd.idaho.plugins.taxonomicNames.higherTaxonomy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AbstractAnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Provider of higher taxonomy based on a third party source. This class takes
 * care of caching, etc. Sub classes have to implement the binding to the third
 * party service.<br>
 * This class further has static convenience methods for loading instances, and
 * for constructing an integrated higher taxonomy provider that wraps these
 * instances for convenience.
 * 
 * @author sautter
 */
public abstract class HigherTaxonomyProvider implements TaxonomicNameConstants {
	
	/** the attribute for storing the source of the higher taxonomy of an annotation */
	public static final String HIGHER_TAXONOMY_SOURCE_ATTRIBUTE = "higherTaxonomySource";
	
	/** the result of a lookup that timed out, to be compared via '==' */
	public static final Properties LOOKUP_TIMEOUT = new Properties() {
		public synchronized Object setProperty(String key, String value) {
			return value;
		}
		public synchronized void load(Reader reader) throws IOException {}
		public synchronized void load(InputStream inStream) throws IOException {}
		public synchronized void loadFromXML(InputStream in) throws IOException {}
		public synchronized Object put(Object key, Object value) {
			return value;
		}
		public synchronized void putAll(Map t) {}
	};
	
	private static class Base {
		private AnalyzerDataProvider dataProvider;
		private HigherTaxonomyProvider[] instances = null;
		Base(AnalyzerDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}
		HigherTaxonomyProvider[] getDataProviders() {
			if (this.instances == null)
				this.instances = this.loadTaxonomyProviders();
			return this.instances;
		}
		private HigherTaxonomyProvider[] loadTaxonomyProviders() {
			System.out.println("HigherTaxonomyProvider: initializing instances");
			
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
					HigherTaxonomyProvider.class, 
					new ComponentInitializer() {
						public void initialize(Object component, String componentJarName) throws Throwable {
							if (componentJarName == null)
								throw new RuntimeException("Cannot determine data path for " + component.getClass().getName());
							HigherTaxonomyProvider gdp = ((HigherTaxonomyProvider) component);
							componentJarName = componentJarName.substring(0, componentJarName.lastIndexOf('.')) + "Data";
							gdp.setDataProvider(new PrefixDataProvider(dataProvider, componentJarName));
						}
					});
			
			//	store & return geo data providers
			HigherTaxonomyProvider[] dataProviders = new HigherTaxonomyProvider[dataProviderObjects.length];
			for (int c = 0; c < dataProviderObjects.length; c++)
				dataProviders[c] = ((HigherTaxonomyProvider) dataProviderObjects[c]);
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
	public static HigherTaxonomyProvider[] getTaxonomyProviders(AnalyzerDataProvider dataProvider) {
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
	public static HigherTaxonomyProvider getTaxonomyProvider(AnalyzerDataProvider dataProvider) {
		return integrateTaxonomyProviders(getTaxonomyProviders(dataProvider), dataProvider);
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
	public static HigherTaxonomyProvider integrateTaxonomyProviders(HigherTaxonomyProvider[] taxonomyProviders) {
		return integrateTaxonomyProviders(taxonomyProviders, null);
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
	public static HigherTaxonomyProvider integrateTaxonomyProviders(HigherTaxonomyProvider[] taxonomyProviders, AnalyzerDataProvider dataProvider) {
		
		//	nothing to integrate at all
		if (taxonomyProviders.length == 0)
			return null;
		
		//	no use integrating a single provider
		if (taxonomyProviders.length == 1)
			return taxonomyProviders[0];
		
		//	use integrating provider
		return new IntegratedHigherTaxonomyProvider(taxonomyProviders, dataProvider);
	}
	
	private static class IntegratedHigherTaxonomyProvider extends HigherTaxonomyProvider {
		private HigherTaxonomyProvider[] taxonomyProviders;
		private String dataSourceString;
		
		IntegratedHigherTaxonomyProvider(HigherTaxonomyProvider[] taxonomyProviders, AnalyzerDataProvider dataProvider) {
			
			//	copy argument array to block external modification
			this.taxonomyProviders = new HigherTaxonomyProvider[taxonomyProviders.length];
			System.arraycopy(taxonomyProviders, 0, this.taxonomyProviders, 0, taxonomyProviders.length);
			
			//	assemble integrated data source name
			StringBuffer dataSourceStringBuilder = new StringBuffer(this.taxonomyProviders[0].getDataSourceName());
			for (int p = 1; p < this.taxonomyProviders.length; p++) {
				dataSourceStringBuilder.append(",");
				dataSourceStringBuilder.append(this.taxonomyProviders[p].getDataSourceName());
			}
			this.dataSourceString = dataSourceStringBuilder.toString();
			
			//	set data provider
			this.dataProvider = dataProvider;
		}
		
		public String getDataSourceName() {
			return this.dataSourceString;
		}
		
		public void setLookupTimeout(int lt) {
			for (int p = 0; p < this.taxonomyProviders.length; p++)
				this.taxonomyProviders[p].setLookupTimeout(lt);
			super.setLookupTimeout(lt);
		}
		
		protected void exit() {
			for (int p = 0; p < this.taxonomyProviders.length; p++)
				this.taxonomyProviders[p].shutdown();
		}
		
		public Properties getHierarchy(String epithet, String rank, boolean allowWebAccess) {
			return this.loadHierarchy(epithet, rank); // ALWAYS bypass own cache (on each lookup, we might get a timeout on _some_ wrapped provider, but not all, so we need to re-get the results each time)
		}
		
		protected Properties loadHierarchy(String epithet, String rank) {
			
			//	try without web access first, basically probing caches of wrapped providers
			final LinkedList htpHierarchyList = new LinkedList();
			for (int p = 0; p < this.taxonomyProviders.length; p++) {
				Properties htpHierarchy = this.taxonomyProviders[p].getHierarchy(epithet, rank, false);
				if ((htpHierarchy != null) && !htpHierarchy.isEmpty())
					htpHierarchyList.addAll(Arrays.asList(extractHierarchies(htpHierarchy)));
			}
			
			//	any luck?
			if (htpHierarchyList.size() != 0)
				return bundleHierarchies(aggregateHierarchies(((Properties[]) htpHierarchyList.toArray(new Properties[htpHierarchyList.size()])), rank));
			
			//	start request for hierarchies to wrapped providers
			HtpRequest[] htpRequests = new HtpRequest[this.taxonomyProviders.length];
			for (int p = 0; p < this.taxonomyProviders.length; p++) {
				htpRequests[p] = new HtpRequest(this.taxonomyProviders[p], epithet, rank);
				this.getHtpRequestHandler().handleHtpRequest(htpRequests[p]);
			}
			
			//	collect lookup results
			boolean lookupTimeout = false;
			for (int p = 0; p < htpRequests.length; p++) {
				Properties htpHierarchy = htpRequests[p].getHigherTaxonomy();
				if (htpHierarchy == LOOKUP_TIMEOUT)
					lookupTimeout = true;
				else if (htpHierarchy != null)
					htpHierarchyList.addAll(Arrays.asList(extractHierarchies(htpHierarchy)));
			}
			
			//	aggregate individual lookup results, bundle aggregates and hand them back
			return ((htpHierarchyList.isEmpty() && lookupTimeout) ? LOOKUP_TIMEOUT : bundleHierarchies(aggregateHierarchies(((Properties[]) htpHierarchyList.toArray(new Properties[htpHierarchyList.size()])), rank)));
		}
		
		public boolean isKnownNegative(String str) {
			for (int p = 0; p < this.taxonomyProviders.length; p++) {
				if (this.taxonomyProviders[p].isKnownNegative(str))
					return true;
			}
			return super.isKnownNegative(str);
		}
		
		private LinkedList htpRequestHandlers = new LinkedList();
		
		private HtpRequestHandler getHtpRequestHandler() {
			synchronized (this.htpRequestHandlers) {
				if (this.htpRequestHandlers.isEmpty())
					return new HtpRequestHandler();
				else return ((HtpRequestHandler) this.htpRequestHandlers.removeFirst());
			}
		}
		
		private class HtpRequestHandler extends Thread {
			private HtpRequest htpr = null;
			HtpRequestHandler() {
				this.start();
			}
			public synchronized void run() {
				while (true) {
					
					//	wait for next request (if not already present, can happen on startup)
					if (this.htpr == null) try {
						this.wait();
					} catch (InterruptedException ie) {}
					
					//	are we shutting down?
					if (this.htpr == null)
						return;
					
					//	handle request
					System.out.println(this.htpr.htp.getDataSourceName() + ": starting asynchronous lookup for " + ((this.htpr.rank == null) ? "" : (this.htpr.rank + " ")) + "'" + this.htpr.epithet + "'" + ((this.htpr.rank == null) ? " (unknown rank)" : ""));
					Properties higherTaxonomy = this.htpr.htp.getHierarchy(this.htpr.epithet, this.htpr.rank, true);
					this.htpr.setHigherTaxonomy(higherTaxonomy);
					
					//	clean up
					this.htpr = null;
					
					//	return to pool (only if we're below thrice the number of wrapped providers)
					synchronized (htpRequestHandlers) {
						if (htpRequestHandlers.size() < (taxonomyProviders.length * 3))
							htpRequestHandlers.addLast(this);
						else return;
					}
				}
			}
			synchronized void handleHtpRequest(HtpRequest htpr) {
				this.htpr = htpr;
				this.notify();
			}
		}
		
		private class HtpRequest {
			final HigherTaxonomyProvider htp;
			final String epithet;
			final String rank;
			private boolean awaitingHigherTaxonomy = true;
			private Properties higherTaxonomy = LOOKUP_TIMEOUT;
			
			HtpRequest(HigherTaxonomyProvider htp, String epithet, String rank) {
				this.htp = htp;
				this.epithet = epithet;
				this.rank = rank;
			}
			
			synchronized void setHigherTaxonomy(Properties higherTaxonomy) {
				this.higherTaxonomy = higherTaxonomy;
				this.awaitingHigherTaxonomy = false;
				this.notify();
			}
			
			synchronized Properties getHigherTaxonomy() {
				if (this.awaitingHigherTaxonomy) try {
					this.wait(lookupTimeout);
				} catch (InterruptedException ie) {}
				if (this.awaitingHigherTaxonomy)
					System.out.println(this.htp.getDataSourceName() + ": asynchronous lookup timed out");
				else System.out.println(this.htp.getDataSourceName() + ": asynchronous lookup done");
				return this.higherTaxonomy;
			}
		}
	}
	
	/**
	 * an array holding the primary ranks that should be contained in any
	 * higher hierarchy returned by the provider (additional intermediate)
	 * rank epithets are also permitted)
	 */
	protected static final String[] rankNames = {
		KINGDOM_ATTRIBUTE,
		PHYLUM_ATTRIBUTE,
		CLASS_ATTRIBUTE,
		ORDER_ATTRIBUTE,
		FAMILY_ATTRIBUTE,
		GENUS_ATTRIBUTE,
	};
	
	/**
	 * a mapping from primary ranks to the next lower primary rank, for higher
	 * taxonomy providers whose lookup results contain lists of child epithets
	 */
	protected static final Properties childRanks = new Properties();
	static {
		childRanks.setProperty(KINGDOM_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		childRanks.setProperty(PHYLUM_ATTRIBUTE, CLASS_ATTRIBUTE);
		childRanks.setProperty(CLASS_ATTRIBUTE, ORDER_ATTRIBUTE);
		childRanks.setProperty(ORDER_ATTRIBUTE, FAMILY_ATTRIBUTE);
		childRanks.setProperty(FAMILY_ATTRIBUTE, GENUS_ATTRIBUTE);
		childRanks.setProperty(GENUS_ATTRIBUTE, SPECIES_ATTRIBUTE);
	}
	
	/** the data provider to use */
	protected AnalyzerDataProvider dataProvider;
	
	/** the timeout for lookups at the wrapped data source, in milliseconds (5 seconds by default) */
	protected int lookupTimeout = (5 * 1000);
	
	private boolean emptyResultLookupsDirty = false;
	private Map emptyResultLookups = Collections.synchronizedMap(new TreeMap(String.CASE_INSENSITIVE_ORDER) {
		public boolean containsKey(Object key) {
			Long llt = ((Long) this.get(key));
			return ((llt != null) && (System.currentTimeMillis() < (llt.longValue() + (1000 * 60 * 60 * 24))));
		}
		public Object put(Object key, Object value) {
			if (value instanceof String)
				value = new Long((String) value);
			emptyResultLookupsDirty = true;
			return super.put(key, value);
		}
	});
	
	/**
	 * @return the name of the higher taxonomy provider
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
	 * @return the name of the underlying data source
	 */
	public abstract String getDataSourceName();
	
	/**
	 * give the higher taxonomy provider access to its data
	 * @param dataProvider the data provider
	 */
	public final void setDataProvider(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
		
		//	load empty-result lookups and their last time
		if (this.dataProvider.isDataAvailable("cache/emptyResultLookups.txt")) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("cache/emptyResultLookups.txt"), "UTF-8"));
			for (String erl; (erl = br.readLine()) != null;) {
				erl = erl.trim();
				if (erl.indexOf(' ') == -1)
					continue;
				String rankAndEpithet = erl.substring(0, erl.indexOf(' '));
				String lastLookup = erl.substring(erl.indexOf(' ') + " ".length());
				this.emptyResultLookups.put(rankAndEpithet, lastLookup);
			}
			this.emptyResultLookupsDirty = false;
		} catch (IOException ioe) {}
		
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
		
		//	store empty-result lookups and their last time
		if (this.emptyResultLookupsDirty && this.dataProvider.isDataEditable("cache/emptyResultLookups.txt")) try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("cache/emptyResultLookups.txt"), "UTF-8"));
			for (Iterator git = this.emptyResultLookups.keySet().iterator(); git.hasNext();) {
				String ranAndEpithet = ((String) git.next());
				Long lastLookup = ((Long) this.emptyResultLookups.get(ranAndEpithet));
				bw.write(ranAndEpithet + " " + lastLookup.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
			this.emptyResultLookupsDirty = false;
		} catch (IOException ioe) {}
		
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
	 * Obtain the higher taxonomic ranks for a given genus. If the backing data
	 * source does not provide the higher ranks for the specified genus, or if
	 * the backing data source is unreachable and the higher ranks for the
	 * specified genus are not cached, this method returns null. Further, if the
	 * genus is a homonym and the higher rank epithets cannot be determined
	 * unambiguously, the returned Properties object contains all available
	 * hierarchies, with the keys prefixed with numbers (e.g.
	 * <code>0.kingdom</code> for the kingdom of the first hierarchy found). In
	 * such cases, it is up to client code to determine which hierarchy is the
	 * correct one. 
	 * @param genus the genus to get the higher ranks for
	 * @param allowWebAccess allow downloading data from IPNI in case of a file
	 *            cache miss?
	 * @return a Properties object containing the higher taxonomic ranks for the
	 *         argument genus
	 */
	public Properties getHierarchy(String genus, boolean allowWebAccess) {
		return this.getHierarchy(genus, GENUS_ATTRIBUTE, allowWebAccess);
	}
	
	/**
	 * Obtain the higher taxonomic ranks for a given taxonomic epithet. If the
	 * backing data source does not provide the higher ranks for the specified
	 * epithet, or if the backing data source is unreachable and the higher
	 * ranks for the specified epithet are not cached, this method returns null.
	 * Further, if the epithet is a homonym and the higher rank epithets cannot
	 * be determined unambiguously, the returned Properties object contains all
	 * available hierarchies, with the keys prefixed with numbers (e.g.
	 * <code>0.kingdom</code> for the kingdom of the first hierarchy found). In
	 * such cases, it is up to client code to determine which hierarchy is the
	 * correct one. If the argument rank is null, the argument epithet can have
	 * any rank, and providers will attempt to determine it from the backing
	 * source. The rank is then stored in the <code>0.rank</code> attribute of
	 * each individual hierarchy returned.
	 * @param epithet the taxonomic epithet to get the higher ranks for
	 * @param rank the rank of the epithet, if known to client code
	 * @param allowWebAccess allow downloading data from the backing source in
	 *            case of a file cache miss?
	 * @return a Properties object containing the higher taxonomic ranks for the
	 *         argument epithet
	 */
	public Properties getHierarchy(String epithet, String rank, boolean allowWebAccess) {
		System.out.println(this.getDataSourceName() + ": getting hierarchy for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "'" + ((rank == null) ? " (unknown rank)" : "") + ", web access is " + allowWebAccess);
		
		//	check in-memory cache first
		Properties hierarchy = this.checkMemoryCache(epithet, rank);
		if (hierarchy != null)
			return hierarchy;
		
		//	check negative cache
		if (this.emptyResultLookups.containsKey(rank + ":" + epithet)) {
			System.out.println(this.getDataSourceName() + ": known empty result " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "'");
			return null;
		}
		
		//	check disc cache
		hierarchy = this.checkDiscCache(epithet, rank);
		if (hierarchy != null)
			return hierarchy;
		
		//	load from backing provider
		if (allowWebAccess) try {
			hierarchy = this.loadHierarchy(epithet, rank);
			if ((hierarchy == null) || hierarchy.isEmpty()) {
				System.out.println(this.getDataSourceName() + ": hierarchy not found for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "'");
				this.emptyResultLookups.put((rank + ":" + epithet), new Long(System.currentTimeMillis()));
				return null;
			}
			else System.out.println(this.getDataSourceName() + ": hierarchy loaded for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "'");
		}
		catch (ConnectException ce) {
			System.out.println(this.getDataSourceName() + ": timeout loading data for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "': " + ce.getMessage());
			ce.printStackTrace(System.out);
			return LOOKUP_TIMEOUT;
		}
		catch (SocketTimeoutException ste) {
			System.out.println(this.getDataSourceName() + ": timeout loading data for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "': " + ste.getMessage());
			ste.printStackTrace(System.out);
			return LOOKUP_TIMEOUT;
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": Error loading data for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		catch (Exception e) {
			System.out.println(this.getDataSourceName() + ": Error loading data for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "': " + e.getMessage());
			e.printStackTrace(System.out);
			return null;
		}
		
		//	little we can do
		if ((hierarchy == null) || hierarchy.isEmpty())
			return hierarchy;
		
		//	cache lookup result on disc
		if (this.dataProvider.isDataEditable()) try {
			BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("cache/" + ((rank == null) ? "" : (rank + "_")) + epithet + ".txt"), "UTF-8"));
			for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
				String dRank = ((String) rit.next());
				String dEpithet = hierarchy.getProperty(dRank);
				cacheWriter.write(dRank + "=" + dEpithet);
				cacheWriter.newLine();
			}
			cacheWriter.flush();
			cacheWriter.close();
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": error disc caching data for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		//	add lookup epithet and provider name (we don't need to store these)
		this.checkAttributes(hierarchy, epithet, rank);
		
		//	put lookup result in in-memory cache
		this.cache.put((((rank == null) ? "" : (rank + ":")) + epithet), hierarchy);
		
		//	finally ...
		return hierarchy;
	}
	
	private Properties checkMemoryCache(String epithet, String rank) {
		
		//	try lookup with rank
		Properties hierarchy = ((Properties) this.cache.get(((rank == null) ? "" : (rank + ":")) + epithet));
		
		//	try lookup without rank, filtering result
		if ((hierarchy == null) && (rank != null)) {
			hierarchy = ((Properties) this.cache.get(epithet));
			if (hierarchy != null)
				hierarchy = this.filterForRank(hierarchy, rank);
		}
		
		//	make sure wa have all the required attributes
		if (hierarchy != null) {
			System.out.println(this.getDataSourceName() + ": memory cache hit for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "'");
			this.checkAttributes(hierarchy, epithet, rank);
		}
		return hierarchy;
	}
	
	private Properties checkDiscCache(String epithet, String rank) {
		try {
			String cacheDataName = null;
			Properties hierarchy = null;
			
			//	try lookup with rank
			cacheDataName = ("cache/" + ((rank == null) ? "" : (rank + "_")) + epithet + ".txt");
			if (this.dataProvider.isDataAvailable(cacheDataName))
				hierarchy = this.readCacheFile(cacheDataName);
			
			//	try lookup without rank, filtering result
			if ((hierarchy == null) && (rank != null)) {
				cacheDataName = ("cache/" + epithet + ".txt");
				if (this.dataProvider.isDataAvailable(cacheDataName))
					hierarchy = this.readCacheFile(cacheDataName);
				
				//	populate in-memory cache with unfiltered result
				if (hierarchy != null)
					this.cache.put(epithet, hierarchy);
				
				//	filter result
				if (hierarchy != null)
					hierarchy = this.filterForRank(hierarchy, rank);
			}
			
			//	populate in-memory cache
			if (hierarchy != null)
				this.cache.put((((rank == null) ? "" : (rank + ":")) + epithet), hierarchy);
			
			//	make sure we have all the required attributes
			if (hierarchy != null) {
				System.out.println(this.getDataSourceName() + ": disc cache hit for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "'");
				this.checkAttributes(hierarchy, epithet, rank);
			}
			return hierarchy;
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": error loading cached data for " + ((rank == null) ? "" : (rank + " ")) + "'" + epithet + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
	}
	
	private Properties readCacheFile(String cacheDataName) throws IOException {
		Properties hierarchy = null;
		BufferedReader cacheReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8"));
		String line;
		while ((line = cacheReader.readLine()) != null) {
			int split = line.indexOf('=');
			if (split == -1)
				continue;
			String dAttribute = line.substring(0, split);
			String dValue = line.substring(split + 1);
			if (hierarchy == null)
				hierarchy = new Properties();
			hierarchy.setProperty(dAttribute, dValue);
		}
		cacheReader.close();
		return hierarchy;
	}
	
	private Properties filterForRank(Properties hierarchy, String rank) {
		if (hierarchy.containsKey(KINGDOM_ATTRIBUTE))
			return (rank.equals(hierarchy.getProperty(RANK_ATTRIBUTE)) ? hierarchy : null);
		
		//	check ranks for all wrapped hierarchies
		int wrappedHierarchyCount = 0;
		int rankWrappedHierarchyCount = 0;
		for (int h = 0;; h++) {
			if (!hierarchy.containsKey(h + "." + KINGDOM_ATTRIBUTE))
				break;
			wrappedHierarchyCount++;
			if (rank.equals(hierarchy.getProperty(h + "." + RANK_ATTRIBUTE)))
				rankWrappedHierarchyCount++;
		}
		
		//	no rank matches at all
		if (rankWrappedHierarchyCount == 0)
			return null;
		
		//	all wrapped hierarchies match
		if (rankWrappedHierarchyCount == wrappedHierarchyCount)
			return hierarchy;
		
		//	copy all wrapped hierarchies with matching rank
		Properties fHierarchy = new Properties();
		int fHierarchyIndex = 0;
		for (int h = 0;; h++) {
			if (!hierarchy.containsKey(h + "." + KINGDOM_ATTRIBUTE))
				break;
			if (!rank.equals(hierarchy.getProperty(h + "." + RANK_ATTRIBUTE)))
				continue;
			for (Iterator pnit = hierarchy.keySet().iterator(); pnit.hasNext();) {
				String pn = ((String) pnit.next());
				if (pn.startsWith(h + ".")) {
					String pv = hierarchy.getProperty(pn);
					pn.substring(pn.indexOf('.') + ".".length());
					if (rankWrappedHierarchyCount > 1)
						pn = (fHierarchyIndex + "." + pn);
					fHierarchy.setProperty(pn, pv);
				}
			}
			fHierarchyIndex++;
		}
		return fHierarchy;
	}
	
	private void checkAttributes(Properties hierarchy, String epithet, String rank) {
		if (hierarchy.containsKey(KINGDOM_ATTRIBUTE)) {
			if ((rank != null) && !hierarchy.containsKey(rank))
				hierarchy.setProperty(rank, (epithet.substring(0, 1).toUpperCase() + epithet.substring(1).toLowerCase()));
			if ((rank != null) && !hierarchy.containsKey(RANK_ATTRIBUTE))
				hierarchy.setProperty(RANK_ATTRIBUTE, rank);
			if (!hierarchy.containsKey(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE))
				hierarchy.setProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, this.getDataSourceName());
		}
		else for (int h = 0;; h++) {
			if (!hierarchy.containsKey(h + "." + KINGDOM_ATTRIBUTE))
				break;
			if ((rank != null) && !hierarchy.containsKey(h + "." + rank))
				hierarchy.setProperty((h + "." + rank), (epithet.substring(0, 1).toUpperCase() + epithet.substring(1).toLowerCase()));
			if ((rank != null) && !hierarchy.containsKey(h + "." + RANK_ATTRIBUTE))
				hierarchy.setProperty((h + "." + RANK_ATTRIBUTE), rank);
			if (!hierarchy.containsKey(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE))
				hierarchy.setProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, this.getDataSourceName());
		}
	}
	
	/**
	 * Obtain the higher hierarchy for an epithet from the backing source. The
	 * result of this method may also contain a semicolon separated list of
	 * child epithets for the argument epithet. Child here means the next lower
	 * primary rank, e.g. 'species' if the argument rank is 'genus'. If the
	 * argument rank is null, the argument epithet can have any rank, and
	 * providers implementations of this method should attempt to determine it
	 * from the backing source. They should the store the rank in an additional
	 * <code>rank</code> attribute of each individual returned hierarchy.
	 * @param epithet the taxonomic epithet to get the higher ranks for
	 * @param rank the rank of the epithet, if specified
	 * @return a Properties object containing the higher taxonomic ranks for the
	 *         argument epithet
	 * @throws IOException
	 */
	protected abstract Properties loadHierarchy(String epithet, String rank) throws IOException;
	
	//	TODO add LookupListener argument, to receive notification of a successful lookup
	//	TODO ==> use that for "give me some more time" notification in transitive CoL lookups
	
	/**
	 * Test if some string is a known negative, i.e., known to NOT be a
	 * taxonomic epithet. This default implementation simply returns false,
	 * sub classes are welcome to overwrite it as needed. Implementations
	 * should not rely on remote lookups; this method is first and foremost
	 * intended to enable local taxonomy providers to grant client code access
	 * to their own lists of known negatives.
	 * @param str the string to test
	 * @return true if the argument string is a known negative
	 */
	public boolean isKnownNegative(String str) {
		return false;
	}
	
	/**
	 * Separate multiple taxonomic hierarchies bundled in on Properties object.
	 * If the argument Properties contains a single taxonomic hierarchy, it is
	 * the only element in the returned array
	 * @param hierarchy the hierarchy to split
	 * @return an array holding the individual hierarchies
	 */
	public static Properties[] extractHierarchies(Properties hierarchy) {
		
		//	this one's unambiguous (we don't have a prefix)
		if (hierarchy.containsKey(KINGDOM_ATTRIBUTE) || hierarchy.containsKey(PHYLUM_ATTRIBUTE) || hierarchy.containsKey(CLASS_ATTRIBUTE) || hierarchy.containsKey(ORDER_ATTRIBUTE) || hierarchy.containsKey(FAMILY_ATTRIBUTE)) {
			Properties[] hierarchies = {hierarchy};
			return hierarchies;
		}
		
		//	count number of hierarchies
		int hierarchyCount = 0;
		for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1) try {
				hierarchyCount = Math.max(hierarchyCount, Integer.parseInt(rank.substring(0, rank.indexOf('.'))));
			} catch (NumberFormatException nfe) {}
		}
		
		//	extract individual hierarchies
		Properties[] hierarchies = new Properties[hierarchyCount+1];
		for (Iterator rit = hierarchy.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1) try {
				int h = Integer.parseInt(rank.substring(0, rank.indexOf('.')));
				if (hierarchies[h] == null)
					hierarchies[h] = new Properties();
				hierarchies[h].setProperty(rank.substring(rank.indexOf('.') + 1), hierarchy.getProperty(rank));
			} catch (NumberFormatException nfe) {}
		}
		
		//	add source name attribute
		String hierarchySource = hierarchy.getProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE);
		if (hierarchySource != null)
			for (int h = 0; h < hierarchies.length; h++) {
				if (!hierarchies[h].containsKey(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE))
					hierarchies[h].setProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, hierarchySource);
			}
		
		//	finally ...
		return hierarchies;
	}
	
	/**
	 * Bundle multiple taxonomic hierarchies in a single Properties object. If
	 * the argument array has length 0, this method returns null, if it has
	 * length 1, the only element is returned. IN all other cases, the keys in
	 * the returned Properties object are prefixed with numbers (e.g.
	 * <code>0.kingdom</code> for the kingdom of the first hierarchy in the
	 * argument array).
	 * @param hierarchies an array holding the hierarchies to bundle
	 * @return a Properties object bundling the argument hierarchies
	 */
	public static Properties bundleHierarchies(Properties[] hierarchies) {
		if (hierarchies.length == 0)
			return null;
		if (hierarchies.length == 1)
			return hierarchies[0];
		Properties hierarchy = new Properties();
		for (int h = 0; h < hierarchies.length; h++) {
			for (Iterator rit = hierarchies[h].keySet().iterator(); rit.hasNext();) {
				String rank = ((String) rit.next());
				String epithet = hierarchies[h].getProperty(rank);
				hierarchy.setProperty((h + "." + rank), epithet);
			}
		}
		return hierarchy;
	}
	
	/**
	 * Aggregate a set of taxonomic hierarchies. This method bundles matching
	 * hierarchies, but keeps non-matching ones separate. If hierarchies are
	 * aggregated, their sources are concatenated, as are any given lists of
	 * child epithets.
	 * @param hierarchies an array holding the hierarchies to aggregate
	 * @param rank the rank of the taxonomic epithet the hierarchies are for
	 * @return an array holding the aggregated hierarchies
	 */
	public static Properties[] aggregateHierarchies(Properties[] hierarchies, String rank) {
		if (hierarchies.length < 2)
			return hierarchies;
		
		//	copy and sort input (we definitely want to start the aggregates with the most complete hierarchies)
		Properties[] sHierarchies = new Properties[hierarchies.length];
		System.arraycopy(hierarchies, 0, sHierarchies, 0, hierarchies.length);
		Arrays.sort(sHierarchies, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				if ((obj1 == null) && (obj2 == null))
					return 0;
				else if (obj1 == null)
					return 1;
				else if (obj2 == null)
					return -1;
				else return (((Properties) obj2).size() - ((Properties) obj1).size());
			}
		});
		
		//	get child rank
		String childRank = ((rank == null) ? null : childRanks.getProperty(rank));
		
		//	aggregate hierarchies
		LinkedList aggregateHierarchies = new LinkedList();
		for (int h = 0; h < sHierarchies.length; h++) {
			if (sHierarchies[h] == null)
				continue;
			String hierarchySource = sHierarchies[h].getProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE);
			String hierarchyChildren = ((childRank == null) ? null : sHierarchies[h].getProperty(childRank));
			boolean newAggregateHierarchy = true;
			
			//	add hierarchy to all aggregates it fits in with
			for (Iterator hit = aggregateHierarchies.iterator(); hit.hasNext();) {
				Properties aggregateHierarchy = ((Properties) hit.next());
				String aggregateAuthority = null;
				String aggregateAuthorityName = null;
				String aggregateAuthorityYear = null;
				if (areHierarchiesCompatible(aggregateHierarchy, sHierarchies[h], rank)) {
					
					//	add epithets
					for (int r = 0; r < rankNames.length; r++) {
						String epithet = sHierarchies[h].getProperty(rankNames[r]);
						if (epithet != null)
							aggregateHierarchy.setProperty(rankNames[r], epithet);
						if (rankNames[r].equals(rank))
							break;
					}
					
					//	add authority
					String authority = sHierarchies[h].getProperty(AUTHORITY_ATTRIBUTE);
					if ((authority != null) && ((aggregateAuthority == null) || (aggregateAuthority.length() < authority.length())))
						aggregateAuthority = authority;
					String authorityName = sHierarchies[h].getProperty(AUTHORITY_NAME_ATTRIBUTE);
					if ((authorityName != null) && ((aggregateAuthorityName == null) || (aggregateAuthorityName.length() < authorityName.length())))
						aggregateAuthorityName = authorityName;
					String authorityYear = sHierarchies[h].getProperty(AUTHORITY_YEAR_ATTRIBUTE);
					if ((authorityYear != null) && (aggregateAuthorityYear == null))
						aggregateAuthorityYear = authorityYear;
					
					//	aggregate child epithet lists
					if (hierarchyChildren != null) {
						String aggregateHierarchyChildren = aggregateHierarchy.getProperty(childRank);
						if (aggregateHierarchyChildren == null)
							aggregateHierarchy.setProperty(childRank, hierarchyChildren);
						else {
							TreeSet aggregateHierarchyChildSet = new TreeSet();
							aggregateHierarchyChildSet.addAll(Arrays.asList(aggregateHierarchyChildren.split("\\;")));
							aggregateHierarchyChildSet.addAll(Arrays.asList(hierarchyChildren.split("\\;")));
							StringBuffer aggregateHierarchyChildList = new StringBuffer();
							for (Iterator cit = aggregateHierarchyChildSet.iterator(); cit.hasNext();) {
								String childEpithet = ((String) cit.next());
								aggregateHierarchyChildList.append(childEpithet);
								if (cit.hasNext())
									aggregateHierarchyChildList.append(";");
							}
							aggregateHierarchy.setProperty(childRank, aggregateHierarchyChildList.toString());
						}
					}
					
					//	aggregate source
					if (hierarchySource != null) {
						String aggregateHierarchySource = aggregateHierarchy.getProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE);
						if (aggregateHierarchySource == null)
							aggregateHierarchySource = hierarchySource;
						else if (aggregateHierarchySource.indexOf(hierarchySource) == -1)
							aggregateHierarchySource += ("," + hierarchySource);
						aggregateHierarchy.setProperty(HIGHER_TAXONOMY_SOURCE_ATTRIBUTE, aggregateHierarchySource);
					}
					
					//	remember we don't have to start a new aggregate hierarchy
					newAggregateHierarchy = false;
				}
			}
			
			//	no matching aggregate hierarchy found, start new one
			if (newAggregateHierarchy) {
				Properties aggregateHierarchy = new Properties();
				aggregateHierarchy.putAll(sHierarchies[h]);
				aggregateHierarchies.add(aggregateHierarchy);
			}
		}
		
		//	finally ...
		return ((Properties[]) aggregateHierarchies.toArray(new Properties[aggregateHierarchies.size()]));
	}
	
	/**
	 * Test if two taxonomic hierarchies are compatible, i.e., contain matching
	 * epithets for the ranks that are present in both. This method does not
	 * compare the source attribute, but it does compare authorities if they
	 * are present in both hierarchies.
	 * @param hierarchy1 the first hierarchy
	 * @param hierarchy2 the second hierarchy
	 * @param rank the rank of the taxonomic epithet the hierarchies are for
	 * @return true if the hierarchies are compatible
	 */
	public static boolean areHierarchiesCompatible(Properties hierarchy1, Properties hierarchy2, String rank) {
		for (int r = 0; r < rankNames.length; r++) {
			if ((r != 0) && rankNames[r-1].equals(rank))
				break;
			String epithet1 = hierarchy1.getProperty(rankNames[r]);
			String epithet2 = hierarchy2.getProperty(rankNames[r]);
			if ((epithet1 == null) || (epithet2 == null))
				continue;
			if (!epithet1.equalsIgnoreCase(epithet2))
				return false;
		}
		String authorityName1 = hierarchy1.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		String authorityName2 = hierarchy2.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		if ((authorityName1 != null) && (authorityName2 != null)) {
			if (authorityName1.endsWith("."))
				authorityName1 = authorityName1.substring(0, (authorityName1.length() - ".".length())).trim();
			if (authorityName2.endsWith("."))
				authorityName2 = authorityName2.substring(0, (authorityName2.length() - ".".length())).trim();
			if (!authorityName1.equalsIgnoreCase(authorityName2) && !StringUtils.isAbbreviationOf(authorityName1, authorityName2, false) && !StringUtils.isAbbreviationOf(authorityName2, authorityName1, false))
				return false;
		}
		String authorityYear1 = hierarchy1.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		String authorityYear2 = hierarchy2.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		if ((authorityYear1 != null) && (authorityYear2 != null) && !authorityYear1.equals(authorityYear2))
			return false;
		return true;
	}
	
	/**
	 * Test if two taxonomic hierarchies are equal, i.e., contain epithets for
	 * the same ranks, and the epithets match. This method does not compare the
	 * source attribute, but it does compare authorities if they are present in
	 * both hierarchies.
	 * @param hierarchy1 the first hierarchy
	 * @param hierarchy2 the second hierarchy
	 * @param rank the rank of the taxonomic epithet the hierarchies are for
	 * @return true if the hierarchies are equal
	 */
	public static boolean areHierarchiesEqual(Properties hierarchy1, Properties hierarchy2, String rank) {
		for (int r = 0; r < rankNames.length; r++) {
			if ((r != 0) && rankNames[r-1].equals(rank))
				break;
			String epithet1 = hierarchy1.getProperty(rankNames[r]);
			String epithet2 = hierarchy2.getProperty(rankNames[r]);
			if ((epithet1 == null) && (epithet2 == null))
				continue;
			if ((epithet1 == null) || (epithet2 == null))
				return false;
			if (!epithet1.equalsIgnoreCase(epithet2))
				return false;
		}
		String authorityName1 = hierarchy1.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		String authorityName2 = hierarchy2.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		if ((authorityName1 != null) && (authorityName2 != null)) {
			if (authorityName1.endsWith("."))
				authorityName1 = authorityName1.substring(0, (authorityName1.length() - ".".length())).trim();
			if (authorityName2.endsWith("."))
				authorityName2 = authorityName2.substring(0, (authorityName2.length() - ".".length())).trim();
			if (!authorityName1.equalsIgnoreCase(authorityName2) && !StringUtils.isAbbreviationOf(authorityName1, authorityName2, false) && !StringUtils.isAbbreviationOf(authorityName2, authorityName1, false))
				return false;
		}
		String authorityYear1 = hierarchy1.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		String authorityYear2 = hierarchy2.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		if ((authorityYear1 != null) && (authorityYear2 != null) && !authorityYear1.equals(authorityYear2))
			return false;
		return true;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		HigherTaxonomyProvider htp = HigherTaxonomyProvider.getTaxonomyProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/HigherTaxonomyData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
		testLookup(htp, "Formica", GENUS_ATTRIBUTE);
		testLookup(htp, "Acacia", GENUS_ATTRIBUTE);
		testLookup(htp, "Formicidae", FAMILY_ATTRIBUTE);
		testLookup(htp, "Nesticus", GENUS_ATTRIBUTE);
		testLookup(htp, "Eciton", GENUS_ATTRIBUTE);
		testLookup(htp, "Camponotus", GENUS_ATTRIBUTE);
		testLookup(htp, "Chromis", GENUS_ATTRIBUTE);
		testLookup(htp, "Hymenoptera", ORDER_ATTRIBUTE);
		testLookup(htp, "Cataglyphis", GENUS_ATTRIBUTE);
		testLookup(htp, "Monomorium", GENUS_ATTRIBUTE);
		testLookup(htp, "Formicadia", GENUS_ATTRIBUTE);
	}
	
	private static void testLookup(HigherTaxonomyProvider htp, String lEpithet, String lRank) {
		Properties ht = htp.getHierarchy(lEpithet, lRank, true);
		if (ht == null) {
			System.out.println("higher taxonomy not found");
			return;
		}
		for (Iterator rit = ht.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1)
				break;
			System.out.println(rank + ": " + ht.getProperty(rank));
			if (!rit.hasNext())
				return;
		}
		for (int r = 0;; r++) {
			String prefix = (r + ".");
			boolean prefixEmpty = true;
			for (Iterator rit = ht.keySet().iterator(); rit.hasNext();) {
				String rank = ((String) rit.next());
				if (!rank.startsWith(prefix))
					continue;
				prefixEmpty = false;
				System.out.println(rank + ": " + ht.getProperty(rank));
			}
			if (prefixEmpty)
				break;
		}
	}
}