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
package de.uka.ipd.idaho.plugins.taxonomicNames.authority;

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
import java.util.ArrayList;
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
 * Provider of taxonomic authority based on a third party source. This class
 * takes care of caching, etc. Sub classes have to implement the binding to the
 * third party service.<br>
 * This class further has static convenience methods for loading instances, and
 * for constructing an integrated authority provider that wraps these instances
 * for convenience.
 * 
 * @author sautter
 */
public abstract class AuthorityProvider  implements TaxonomicNameConstants {
	
	/** the attribute for storing the source of the higher taxonomy of an annotation */
	public static final String AUTHORITY_SOURCE_ATTRIBUTE = "authoritySource";
	
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
		private AuthorityProvider[] instances = null;
		Base(AnalyzerDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}
		AuthorityProvider[] getDataProviders() {
			if (this.instances == null)
				this.instances = this.loadTaxonomyProviders();
			return this.instances;
		}
		private AuthorityProvider[] loadTaxonomyProviders() {
			System.out.println("AuthorityProvider: initializing instances");
			
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
					AuthorityProvider.class, 
					new ComponentInitializer() {
						public void initialize(Object component, String componentJarName) throws Throwable {
							if (componentJarName == null)
								throw new RuntimeException("Cannot determine data path for " + component.getClass().getName());
							AuthorityProvider gdp = ((AuthorityProvider) component);
							componentJarName = componentJarName.substring(0, componentJarName.lastIndexOf('.')) + "Data";
							gdp.setDataProvider(new PrefixDataProvider(dataProvider, componentJarName));
						}
					});
			
			//	store & return geo data providers
			AuthorityProvider[] dataProviders = new AuthorityProvider[dataProviderObjects.length];
			for (int c = 0; c < dataProviderObjects.length; c++)
				dataProviders[c] = ((AuthorityProvider) dataProviderObjects[c]);
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
	public static AuthorityProvider[] getAuthorityProviders(AnalyzerDataProvider dataProvider) {
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
	public static AuthorityProvider getAuthorityProvider(AnalyzerDataProvider dataProvider) {
		return integrateAuthorityProviders(getAuthorityProviders(dataProvider), dataProvider);
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
	public static AuthorityProvider integrateAuthorityProviders(AuthorityProvider[] taxonomyProviders) {
		return integrateAuthorityProviders(taxonomyProviders, null);
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
	public static AuthorityProvider integrateAuthorityProviders(AuthorityProvider[] taxonomyProviders, AnalyzerDataProvider dataProvider) {
		
		//	nothing to integrate at all
		if (taxonomyProviders.length == 0)
			return null;
		
		//	no use integrating a single provider
		if (taxonomyProviders.length == 1)
			return taxonomyProviders[0];
		
		//	use integrating provider
		return new IntegratedAuthorityProvider(taxonomyProviders, dataProvider);
	}
	
	private static class IntegratedAuthorityProvider extends AuthorityProvider {
		private AuthorityProvider[] taxonomyProviders;
		private String dataSourceString;
		
		IntegratedAuthorityProvider(AuthorityProvider[] taxonomyProviders, AnalyzerDataProvider dataProvider) {
			
			//	copy argument array to block external modification
			this.taxonomyProviders = new AuthorityProvider[taxonomyProviders.length];
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
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.taxonomicNames.authority.AuthorityProvider#getAuthority(java.lang.String[], java.lang.String, java.lang.String, int, boolean)
		 */
		public Properties getAuthority(String[] epithets, String rank, String aName, int aYear, boolean allowWebAccess) {
			return this.loadAuthority(epithets, rank, aName, aYear); // ALWAYS bypass own cache (on each lookup, we might get a timeout on _some_ wrapped provider, but not all, so we need to re-get the results each time)
		}
		
		protected Properties loadAuthority(String[] epithets, String rank, String aName, int aYear) {
			
			//	try without web access first, basically probing caches of wrapped providers
			final LinkedList apAuthorityList = new LinkedList();
			for (int p = 0; p < this.taxonomyProviders.length; p++) {
				Properties htpAuthority = this.taxonomyProviders[p].getAuthority(epithets, rank, aName, aYear, false);
				if ((htpAuthority != null) && !htpAuthority.isEmpty())
					apAuthorityList.addAll(Arrays.asList(extractAuthorities(htpAuthority)));
			}
			
			//	any luck?
			if (apAuthorityList.size() != 0)
				return bundleAuthorities(aggregateAuthorities(((Properties[]) apAuthorityList.toArray(new Properties[apAuthorityList.size()])), rank));
			
			//	start request for authorities to wrapped providers
			ApRequest[] apRequests = new ApRequest[this.taxonomyProviders.length];
			for (int p = 0; p < this.taxonomyProviders.length; p++) {
				apRequests[p] = new ApRequest(this.taxonomyProviders[p], epithets, rank, aName, aYear);
				this.getHtpRequestHandler().handleHtpRequest(apRequests[p]);
			}
			
			//	collect lookup results
			boolean lookupTimeout = false;
			for (int p = 0; p < apRequests.length; p++) {
				Properties apAuthority = apRequests[p].getAuthority();
				if (apAuthority == LOOKUP_TIMEOUT)
					lookupTimeout = true;
				else if (apAuthority != null)
					apAuthorityList.addAll(Arrays.asList(extractAuthorities(apAuthority)));
			}
			
			//	aggregate individual lookup results, bundle aggregates and hand them back
			return ((apAuthorityList.isEmpty() && lookupTimeout) ? LOOKUP_TIMEOUT : bundleAuthorities(aggregateAuthorities(((Properties[]) apAuthorityList.toArray(new Properties[apAuthorityList.size()])), rank)));
		}
		
		//	TODO establish some source independent mechanism to expand 'L. 1753' to 'Linneaus 1753', etc.
		//	TODO use that mechanism for normalization before even populating caches ...
		//	TODO ... most likely right after loadAuthority() returns
		//	TODO put data for that mechanism in config file
		
		private LinkedList apRequestHandlers = new LinkedList();
		
		private ApRequestHandler getHtpRequestHandler() {
			synchronized (this.apRequestHandlers) {
				if (this.apRequestHandlers.isEmpty())
					return new ApRequestHandler();
				else return ((ApRequestHandler) this.apRequestHandlers.removeFirst());
			}
		}
		
		private class ApRequestHandler extends Thread {
			private ApRequest apr = null;
			ApRequestHandler() {
				this.start();
			}
			public synchronized void run() {
				while (true) {
					
					//	wait for next request (if not already present, can happen on startup)
					if (this.apr == null) try {
						this.wait();
					} catch (InterruptedException ie) {}
					
					//	are we shutting down?
					if (this.apr == null)
						return;
					
					//	handle request
					System.out.println(this.apr.ap.getDataSourceName() + ": starting asynchronous lookup for " + ((this.apr.rank == null) ? "" : (this.apr.rank + " ")) + "'" + Arrays.toString(this.apr.epithets) + "'" + ((this.apr.rank == null) ? " (unknown rank)" : ""));
					Properties authority = this.apr.ap.getAuthority(this.apr.epithets, this.apr.rank, this.apr.aName, this.apr.aYear, true);
					this.apr.setAuthority(authority);
					
					//	clean up
					this.apr = null;
					
					//	return to pool (only if we're below thrice the number of wrapped providers)
					synchronized (apRequestHandlers) {
						if (apRequestHandlers.size() < (taxonomyProviders.length * 3))
							apRequestHandlers.addLast(this);
						else return;
					}
				}
			}
			synchronized void handleHtpRequest(ApRequest htpr) {
				this.apr = htpr;
				this.notify();
			}
		}
		
		private class ApRequest {
			final AuthorityProvider ap;
			final String[] epithets;
			final String rank;
			final String aName;
			final int aYear;
			private boolean awaitingAuthority = true;
			private Properties authority = LOOKUP_TIMEOUT;
			
			ApRequest(AuthorityProvider ap, String[] epithets, String rank, String aName, int aYear) {
				this.ap = ap;
				this.epithets = epithets;
				this.rank = rank;
				this.aName = aName;
				this.aYear = aYear;
			}
			
			synchronized void setAuthority(Properties authority) {
				this.authority = authority;
				this.awaitingAuthority = false;
				this.notify();
			}
			
			synchronized Properties getAuthority() {
				if (this.awaitingAuthority) try {
					this.wait(lookupTimeout);
				} catch (InterruptedException ie) {}
				if (this.awaitingAuthority)
					System.out.println(this.ap.getDataSourceName() + ": asynchronous lookup timed out");
				else System.out.println(this.ap.getDataSourceName() + ": asynchronous lookup done");
				return this.authority;
			}
		}
	}
	
	/**
	 * an array holding the primary ranks that should be contained in any
	 * complete authority returned by the provider (additional intermediate)
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
//	
//	/**
//	 * Obtain the higher taxonomic ranks for a given genus. If the backing data
//	 * source does not provide the authority for the specified genus, or if
//	 * the backing data source is unreachable and the authority for the
//	 * specified genus are not cached, this method returns null. Further, if the
//	 * genus is a homonym and the higher rank epithets cannot be determined
//	 * unambiguously, the returned Properties object contains all available
//	 * authorities, with the keys prefixed with numbers (e.g.
//	 * <code>0.kingdom</code> for the kingdom of the first authority found). In
//	 * such cases, it is up to client code to determine which authority is the
//	 * correct one. 
//	 * @param genus the genus to get the authority for
//	 * @param allowWebAccess allow downloading data from IPNI in case of a file
//	 *            cache miss?
//	 * @return a Properties object containing the higher taxonomic ranks for the
//	 *         argument genus
//	 */
//	public Properties getAuthority(String genus, boolean allowWebAccess) {
//		return this.getAuthority(genus, GENUS_ATTRIBUTE, allowWebAccess);
//	}
	
	/**
	 * Obtain the higher taxonomic ranks for a given taxonomic epithet. If the
	 * backing data source does not provide the authority for the specified
	 * epithet, or if the backing data source is unreachable and the higher
	 * ranks for the specified epithet are not cached, this method returns null.
	 * Further, if the epithet is a homonym and the higher rank epithets cannot
	 * be determined unambiguously, the returned Properties object contains all
	 * available authorities, with the keys prefixed with numbers (e.g.
	 * <code>0.kingdom</code> for the kingdom of the first authority found). In
	 * such cases, it is up to client code to determine which authority is the
	 * correct one. If the argument rank is null, the argument epithet can have
	 * any rank, and providers will attempt to determine it from the backing
	 * source. The rank is then stored in the <code>0.rank</code> attribute of
	 * each individual authority returned.
	 * @param epithet the taxonomic epithet to get the authority for
	 * @param rank the rank of the epithet, if known to client code
	 * @param allowWebAccess allow downloading data from the backing source in
	 *            case of a file cache miss?
	 * @return a Properties object containing the higher taxonomic ranks for the
	 *         argument epithet
	 */
	
	/**
	 * Obtain the complete authority for a taxonomic name from the backing
	 * source. Implementations should use any argument authority parts for
	 * disambiguation.
	 * @param epithets the epithets of the taxonomic name to get the authority
	 *         for
	 * @param rank the rank of the name, if specified
	 * @param aName any already given authority name, if specified
	 * @param aYear any already given authority year, if specified
	 * @param allowWebAccess allow downloading data from the backing source in
	 *            case of a file cache miss?
	 * @return a Properties object containing the authority for the argument
	 *         taxonomic name
	 * @throws IOException
	 */
	public Properties getAuthority(String[] epithets, String rank, String aName, int aYear, boolean allowWebAccess) {
		System.out.println(this.getDataSourceName() + ": getting authority for " + this.getLogString(epithets, rank, aName, aYear) + ", web access is " + allowWebAccess);
		
		//	check in-memory cache first
		Properties authority = this.checkMemoryCache(epithets, rank, aName, aYear);
		if (authority != null)
			return authority;
		
		//	check negative cache
		if (this.emptyResultLookups.containsKey(this.getCacheKey(epithets, rank, aName, aYear))) {
			System.out.println(this.getDataSourceName() + ": known empty result " + this.getLogString(epithets, rank, aName, aYear));
			return null;
		}
		
		//	check disc cache
		authority = this.checkDiscCache(epithets, rank, aName, aYear);
		if (authority != null)
			return authority;
		
		//	TODO consider always doing provider lookup without name and year
		
		//	load from backing provider
		if (allowWebAccess) try {
//			authority = this.loadAuthority(epithets, rank, aName, aYear);
//			if ((authority == null) || authority.isEmpty()) {
//				System.out.println(this.getDataSourceName() + ": authority not found for " + this.getLogString(epithets, rank, aName, aYear));
//				this.emptyResultLookups.put(this.getCacheKey(epithets, rank, aName, aYear), new Long(System.currentTimeMillis()));
//				return null;
//			}
			authority = this.loadAuthority(epithets, rank, null, -1);
			if ((authority == null) || authority.isEmpty()) {
				System.out.println(this.getDataSourceName() + ": authority not found for " + this.getLogString(epithets, rank, aName, aYear));
				this.emptyResultLookups.put(this.getCacheKey(epithets, rank, null, -1), new Long(System.currentTimeMillis()));
				return null;
			}
			else System.out.println(this.getDataSourceName() + ": authority loaded for " + this.getLogString(epithets, rank, aName, aYear));
		}
		catch (ConnectException ce) {
			System.out.println(this.getDataSourceName() + ": timeout loading data for " + this.getLogString(epithets, rank, aName, aYear) + ": " + ce.getMessage());
			ce.printStackTrace(System.out);
			return LOOKUP_TIMEOUT;
		}
		catch (SocketTimeoutException ste) {
			System.out.println(this.getDataSourceName() + ": timeout loading data for " + this.getLogString(epithets, rank, aName, aYear) + ": " + ste.getMessage());
			ste.printStackTrace(System.out);
			return LOOKUP_TIMEOUT;
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": Error loading data for " + this.getLogString(epithets, rank, aName, aYear) + ": " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		catch (Exception e) {
			System.out.println(this.getDataSourceName() + ": Error loading data for " + this.getLogString(epithets, rank, aName, aYear) + ": " + e.getMessage());
			e.printStackTrace(System.out);
			return null;
		}
		
		//	little we can do
		if ((authority == null) || authority.isEmpty())
			return authority;
		
		//	cache lookup result on disc
		if (this.dataProvider.isDataEditable()) try {
//			BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("cache/" + this.getCacheKey(epithets, rank, aName, aYear) + ".txt"), "UTF-8"));
			BufferedWriter cacheWriter = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("cache/" + this.getCacheKey(epithets, rank, null, -1) + ".txt"), "UTF-8"));
			for (Iterator rit = authority.keySet().iterator(); rit.hasNext();) {
				String dRank = ((String) rit.next());
				String dEpithet = authority.getProperty(dRank);
				cacheWriter.write(dRank + "=" + dEpithet);
				cacheWriter.newLine();
			}
			cacheWriter.flush();
			cacheWriter.close();
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": error disc caching data for " + this.getLogString(epithets, rank, aName, aYear) + ": " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		//	add lookup epithet and provider name (we don't need to store these)
		this.checkAttributes(authority, epithets, rank);
		
		//	put lookup result in in-memory cache
//		this.cache.put(this.getCacheKey(epithets, rank, aName, aYear), authority);
		this.cache.put(this.getCacheKey(epithets, rank, null, -1), authority);
		
		//	filter by name and year only now
		authority = this.filterForNameAndYear(authority, aName, aYear);
		
		//	finally ...
		return authority;
	}
	
	private Properties checkMemoryCache(String[] epithets, String rank, String aName, int aYear) {
		
		//	try lookup with rank
		Properties authority = ((Properties) this.cache.get(this.getCacheKey(epithets, rank, aName, aYear)));
		
		//	try lookup without name and year, filtering result
		if ((authority == null) && ((aName != null) || (aYear > 0))) {
			authority = ((Properties) this.cache.get(this.getCacheKey(epithets, rank, null, -1)));
			if (authority != null)
				authority = this.filterForNameAndYear(authority, aName, aYear);
		}
		
		//	make sure we have all the required attributes
		if (authority != null) {
			System.out.println(this.getDataSourceName() + ": memory cache hit for " + this.getLogString(epithets, rank, aName, aYear));
			this.checkAttributes(authority, epithets, rank);
		}
		return authority;
	}
	
	private Properties checkDiscCache(String[] epithets, String rank, String aName, int aYear) {
		try {
			String cacheDataName = null;
			Properties authority = null;
			
			//	try lookup with rank
			cacheDataName = ("cache/" + this.getCacheKey(epithets, rank, aName, aYear) + ".txt");
			if (this.dataProvider.isDataAvailable(cacheDataName))
				authority = this.readCacheFile(cacheDataName);
			
			//	try lookup without name and year, filtering result
			if ((authority == null) && ((aName != null) || (aYear > 0))) {
				cacheDataName = ("cache/" + this.getCacheKey(epithets, rank, null, -1) + ".txt");
				if (this.dataProvider.isDataAvailable(cacheDataName))
					authority = this.readCacheFile(cacheDataName);
				
				//	populate in-memory cache with unfiltered result
				if (authority != null)
					this.cache.put(this.getCacheKey(epithets, rank, aName, aYear), authority);
				
				//	filter result
				if (authority != null)
					authority = this.filterForNameAndYear(authority, aName, aYear);
			}
			
			//	populate in-memory cache
			if (authority != null)
				this.cache.put(this.getCacheKey(epithets, rank, aName, aYear), authority);
			
			//	make sure we have all the required attributes
			if (authority != null) {
				System.out.println(this.getDataSourceName() + ": disc cache hit for " + this.getLogString(epithets, rank, aName, aYear));
				this.checkAttributes(authority, epithets, rank);
			}
			return authority;
		}
		catch (IOException ioe) {
			System.out.println(this.getDataSourceName() + ": error loading cached data for " + this.getLogString(epithets, rank, aName, aYear) + ": " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
	}
	
	private String getCacheKey(String[] epithets, String rank, String aName, int aYear) {
		StringBuffer ck = new StringBuffer(rank + "_");
		ck.append(concatEpithets(epithets, "_"));
		if (aName != null) {
			if (aName.endsWith("."))
				aName = aName.substring(0, (aName.length() - ".".length())).trim();
			ck.append("_" + aName);
		}
		if (aYear > 0)
			ck.append("_" + aYear);
		return ck.toString();
	}
	
	private String getLogString(String[] epithets, String rank, String aName, int aYear) {
		StringBuffer ls = new StringBuffer(rank + " '");
		ls.append(concatEpithets(epithets, " "));
		if (aName != null)
			ls.append(" " + aName);
		if (aYear > 0)
			ls.append(" " + aYear);
		ls.append("'");
		return ls.toString();
	}
	
	/**
	 * Concatenate an array of string epithets with a custom separator
	 * @param epithets the array holding the epithets
	 * @param separator the separator to use
	 * @return the concatenated epithets
	 */
	protected static final String concatEpithets(String[] epithets, String separator) {
		StringBuffer es = new StringBuffer();
		for (int e = 0; e < epithets.length; e++) {
			if (e != 0)
				es.append(separator);
			es.append(epithets[e]);
		}
		return es.toString();
	}
	
	/**
	 * Remove any former author names from an authority name.
	 * @param authorityName the full verbatim authority name
	 * @return the current authority name
	 */
	protected static String getCurrentAuthorityName(String authorityName) {
		int parenthesisEnd = authorityName.indexOf("))");
		if ((parenthesisEnd == -1) || (parenthesisEnd > (authorityName.length() - (")) ".length() + 3))))
			parenthesisEnd = authorityName.indexOf(")");
		if ((parenthesisEnd == -1) || (parenthesisEnd > (authorityName.length() - (") ".length() + 3))))
			return authorityName;
		authorityName = authorityName.substring(parenthesisEnd).trim();
		while (authorityName.startsWith(")"))
			authorityName = authorityName.substring(")".length()).trim();
		return authorityName;
	}
	
	private Properties readCacheFile(String cacheDataName) throws IOException {
		Properties authority = null;
		BufferedReader cacheReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(cacheDataName), "UTF-8"));
		String line;
		while ((line = cacheReader.readLine()) != null) {
			int split = line.indexOf('=');
			if (split == -1)
				continue;
			String dAttribute = line.substring(0, split);
			String dValue = line.substring(split + 1);
			if (authority == null)
				authority = new Properties();
			authority.setProperty(dAttribute, dValue);
		}
		cacheReader.close();
		return authority;
	}
	
	private Properties filterForNameAndYear(Properties authority, String aName, int aYear) {
		if (authority.containsKey(AUTHORITY_ATTRIBUTE)) {
			if (!this.isYearMatch(authority.getProperty(AUTHORITY_YEAR_ATTRIBUTE), aYear))
				return null;
			if (this.isNameMatch(authority.getProperty(AUTHORITY_NAME_ATTRIBUTE), aName, false))
				return authority;
			if (this.isNameMatch(aName, authority.getProperty(AUTHORITY_NAME_ATTRIBUTE), false)) {
				authority.setProperty(AUTHORITY_NAME_ATTRIBUTE, aName);
				return authority;
			}
			return null;
		}
		
		//	check ranks for all wrapped authorities
		int wrappedAuthorityCount = 0;
		int yearWrappedAuthorityCount = 0;
		int nameWrappedAuthorityCount = 0;
		for (int h = 0;; h++) {
			if (!authority.containsKey(h + "." + AUTHORITY_ATTRIBUTE))
				break;
			wrappedAuthorityCount++;
			if (this.isYearMatch(authority.getProperty(h + "." + AUTHORITY_YEAR_ATTRIBUTE), aYear))
				yearWrappedAuthorityCount++;
			if (this.isNameMatch(authority.getProperty(h + "." + AUTHORITY_NAME_ATTRIBUTE), aName, true))
				nameWrappedAuthorityCount++;
		}
		
		//	no name or year matches at all
		if ((yearWrappedAuthorityCount == 0) || (nameWrappedAuthorityCount == 0))
			return null;
		
		//	all wrapped authorities match
		if ((yearWrappedAuthorityCount == wrappedAuthorityCount) && (nameWrappedAuthorityCount == wrappedAuthorityCount))
			return authority;
		
		//	copy all wrapped authorities with matching name and year
		Properties fAuthority = new Properties();
		int fAuthorityIndex = 0;
		for (int h = 0;; h++) {
			if (!authority.containsKey(h + "." + AUTHORITY_ATTRIBUTE))
				break;
			if (!this.isYearMatch(authority.getProperty(h + "." + AUTHORITY_YEAR_ATTRIBUTE), aYear))
				continue;
			boolean isReverseNameMatch;
			if (this.isNameMatch(authority.getProperty(h + "." + AUTHORITY_NAME_ATTRIBUTE), aName, false))
				isReverseNameMatch = false;
			else if (this.isNameMatch(aName, authority.getProperty(h + "." + AUTHORITY_NAME_ATTRIBUTE), false))
				isReverseNameMatch = true;
			else continue;
			for (Iterator pnit = authority.keySet().iterator(); pnit.hasNext();) {
				String pn = ((String) pnit.next());
				if (pn.startsWith(h + ".")) {
					String pv = authority.getProperty(pn);
					pn.substring(pn.indexOf('.') + ".".length());
					if (isReverseNameMatch && AUTHORITY_NAME_ATTRIBUTE.equals(pn))
						pv = aName;
					if ((yearWrappedAuthorityCount > 1) && (nameWrappedAuthorityCount > 1))
						pn = (fAuthorityIndex + "." + pn);
					fAuthority.setProperty(pn, pv);
				}
			}
			fAuthorityIndex++;
		}
		return (fAuthority.isEmpty() ? null : fAuthority);
	}
	
	private boolean isNameMatch(String name, String aName, boolean allowReverse) {
		//	TODO push authority matching to TaxonomicNameUtils
		//	TODO (in long(er) haul) create some PersonNameUtils that does such matching
		if (name == null)
			return false; // this is what we're after, after all ...
		if (aName == null)
			return true; // nothing to filter with
		if (StringUtils.getBaseChar(Character.toLowerCase(name.charAt(0))) != StringUtils.getBaseChar(Character.toLowerCase(aName.charAt(0))))
			return false; // first character has to match TODO handle prefixes like 'van'
		if (name.endsWith("."))
			name = name.substring(0, (name.length() - ".".length())).trim();
		if (aName.endsWith("."))
			aName = aName.substring(0, (aName.length() - ".".length())).trim();
		if (name.equalsIgnoreCase(aName))
			return true; // this one's easy
		if (StringUtils.isAbbreviationOf(name, aName, false))
			return true; // the usual case (augmenting a given abbreviation)
		if (allowReverse && StringUtils.isAbbreviationOf(aName, name, false))
			return true; // we have the full name, but need the year, and source only has abbreviation
		//	TODO establish some minimum length for abbreviations (consider 'L.', though)
		//	TODO eliminate accents, etc.
		//	TODO eliminate parentheses enclosing whole name
		//	TODO eliminate parts in parentheses (former authorities in botany)
		return false;
	}
	
	private boolean isYearMatch(String year, int aYear) {
		if (year == null)
			return false; // this is what we're after, after all ...
		if (aYear <= 0)
			return true; // nothing to filter with
		return year.equals("" + aYear);
	}
	
	private void checkAttributes(Properties authority, String[] epithets, String rank) {
		if (authority.containsKey(AUTHORITY_ATTRIBUTE)) {
			if (!authority.containsKey(TAXONOMIC_NAME_ANNOTATION_TYPE))
				authority.setProperty(TAXONOMIC_NAME_ANNOTATION_TYPE, concatEpithets(epithets, " "));
			if (!authority.containsKey(AUTHORITY_SOURCE_ATTRIBUTE))
				authority.setProperty(AUTHORITY_SOURCE_ATTRIBUTE, this.getDataSourceName());
			if ((rank != null) && !authority.containsKey(RANK_ATTRIBUTE))
				authority.setProperty(RANK_ATTRIBUTE, rank);
		}
		else for (int h = 0;; h++) {
			if (!authority.containsKey(h + "." + AUTHORITY_ATTRIBUTE))
				break;
			if (!authority.containsKey(h + "." + TAXONOMIC_NAME_ANNOTATION_TYPE))
				authority.setProperty((h + "." + TAXONOMIC_NAME_ANNOTATION_TYPE), concatEpithets(epithets, " "));
			if ((rank != null) && !authority.containsKey(h + "." + RANK_ATTRIBUTE))
				authority.setProperty((h + "." + RANK_ATTRIBUTE), rank);
			if (!authority.containsKey(AUTHORITY_SOURCE_ATTRIBUTE))
				authority.setProperty(AUTHORITY_SOURCE_ATTRIBUTE, this.getDataSourceName());
		}
	}
	
	/**
	 * Obtain the complete authority for a taxonomic name from the backing
	 * source. Implementations should use any argument authority parts for
	 * disambiguation. The returned properties at the very least have to
	 * contain the following attributes:<ul>
	 * <li>authority: the verbatim authority from the backing source</li>
	 * <li>authorityName: the authority name from the backing source</li>
	 * <li>authorityYear: the authority year from the backing source</li>
	 * </ul>
	 * @param epithets the epithets of the taxonomic name to get the authority
	 *         for
	 * @param rank the rank of the name, if specified
	 * @param aName any already given authority name, if specified
	 * @param aYear any already given authority year, if specified
	 * @return a Properties object containing the authority for the argument
	 *         taxonomic name
	 * @throws IOException
	 */
	protected abstract Properties loadAuthority(String[] epithets, String rank, String aName, int aYear) throws IOException;
	
	//	TODO add LookupListener argument, to receive notification of a successful lookup
	//	TODO ==> use that for "give me some more time" notification in transitive CoL lookups
//	
//	/**
//	 * Test if some string is a known negative, i.e., known to NOT be a
//	 * taxonomic epithet. This default implementation simply returns false,
//	 * sub classes are welcome to overwrite it as needed. Implementations
//	 * should not rely on remote lookups; this method is first and foremost
//	 * intended to enable local taxonomy providers to grant client code access
//	 * to their own lists of known negatives.
//	 * @param str the string to test
//	 * @return true if the argument string is a known negative
//	 */
//	public boolean isKnownNegative(String str) {
//		return false;
//	}
	
	/**
	 * Aggregate the results of a lookup at a remote source
	 * @param authorities
	 * @return
	 */
	protected static Properties aggregateLookupResults(ArrayList authorities) {
		
		//	do we have a clear result already?
		if (authorities.isEmpty())
			return null;
		else if (authorities.size() == 1)
			return ((Properties) authorities.get(0));
		
		//	eliminate duplicates
		for (int a = 0; a < authorities.size(); a++)
			for (int ca = (a+1); ca < authorities.size(); ca++) {
				if (isSubsetOf(((Properties) authorities.get(a)), ((Properties) authorities.get(ca))))
					authorities.remove(ca--);
				else if (isSubsetOf(((Properties) authorities.get(ca)), ((Properties) authorities.get(a)))) {
					authorities.set(a, authorities.get(ca));
					authorities.remove(ca--);
				}
			}
		
		//	do we have a clear result now?
		if (authorities.isEmpty())
			return null;
		else if (authorities.size() == 1)
			return ((Properties) authorities.get(0));
		
		//	bundle results otherwise
		Properties authority = new Properties();
		for (int a = 0; a < authorities.size(); a++) {
			Properties resultAuthority = ((Properties) authorities.get(a));
			for (Iterator pnit = resultAuthority.keySet().iterator(); pnit.hasNext();) {
				String pn = ((String) pnit.next());
				String pv = resultAuthority.getProperty(pn);
				authority.setProperty((a + "." + pn), pv);
			}
		}
		return authority;
	}
	
	private static boolean isSubsetOf(Properties full, Properties subset) {
		for (int r = 0; r < rankNames.length; r++) {
			String fEpithet = full.getProperty(rankNames[r]);
			String sEpithet = subset.getProperty(rankNames[r]);
			if ((fEpithet == null) && (sEpithet == null))
				continue;
			if (fEpithet == null)
				return false;
			if (!fEpithet.equalsIgnoreCase(sEpithet))
				return false;
		}
		String fAuthorityName = full.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		String sAuthorityName = subset.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		if ((fAuthorityName != null) && (sAuthorityName != null)) {
			if (fAuthorityName.endsWith("."))
				fAuthorityName = fAuthorityName.substring(0, (fAuthorityName.length() - ".".length())).trim();
			if (sAuthorityName.endsWith("."))
				sAuthorityName = sAuthorityName.substring(0, (sAuthorityName.length() - ".".length())).trim();
			if (!fAuthorityName.equalsIgnoreCase(sAuthorityName) && !StringUtils.isAbbreviationOf(fAuthorityName, sAuthorityName, false))
				return false;
		}
		else if (fAuthorityName == null)
			return false;
		String fAuthorityYear = full.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		String sAuthorityYear = subset.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		if ((fAuthorityYear != null) && (sAuthorityYear != null) && !fAuthorityYear.equals(sAuthorityYear))
			return false;
		else if ((fAuthorityYear == null) && (sAuthorityYear != null))
			return false;
		return true;
	}
	
	/**
	 * Separate multiple taxonomic authorities bundled in on Properties object.
	 * If the argument Properties contains a single taxonomic authority, it is
	 * the only element in the returned array
	 * @param authority the authority to split
	 * @return an array holding the individual authorities
	 */
	public static Properties[] extractAuthorities(Properties authority) {
		
		//	this one's unambiguous (we don't have a prefix)
		if (authority.containsKey(AUTHORITY_ATTRIBUTE)) {
			Properties[] authorities = {authority};
			return authorities;
		}
		
		//	count number of authorities
		int authorityCount = 0;
		for (Iterator rit = authority.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1) try {
				authorityCount = Math.max(authorityCount, Integer.parseInt(rank.substring(0, rank.indexOf('.'))));
			} catch (NumberFormatException nfe) {}
		}
		
		//	extract individual authorities
		Properties[] authorities = new Properties[authorityCount+1];
		for (Iterator rit = authority.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1) try {
				int h = Integer.parseInt(rank.substring(0, rank.indexOf('.')));
				if (authorities[h] == null)
					authorities[h] = new Properties();
				authorities[h].setProperty(rank.substring(rank.indexOf('.') + 1), authority.getProperty(rank));
			} catch (NumberFormatException nfe) {}
		}
		
		//	add source name attribute
		String authoritySource = authority.getProperty(AUTHORITY_SOURCE_ATTRIBUTE);
		if (authoritySource != null)
			for (int h = 0; h < authorities.length; h++) {
				if (!authorities[h].containsKey(AUTHORITY_SOURCE_ATTRIBUTE))
					authorities[h].setProperty(AUTHORITY_SOURCE_ATTRIBUTE, authoritySource);
			}
		
		//	finally ...
		return authorities;
	}
	
	/**
	 * Bundle multiple taxonomic authorities in a single Properties object. If
	 * the argument array has length 0, this method returns null, if it has
	 * length 1, the only element is returned. IN all other cases, the keys in
	 * the returned Properties object are prefixed with numbers (e.g.
	 * <code>0.kingdom</code> for the kingdom of the first authority in the
	 * argument array).
	 * @param authorities an array holding the authorities to bundle
	 * @return a Properties object bundling the argument authorities
	 */
	public static Properties bundleAuthorities(Properties[] authorities) {
		if (authorities.length == 0)
			return null;
		if (authorities.length == 1)
			return authorities[0];
		Properties authority = new Properties();
		for (int h = 0; h < authorities.length; h++) {
			for (Iterator rit = authorities[h].keySet().iterator(); rit.hasNext();) {
				String rank = ((String) rit.next());
				String epithet = authorities[h].getProperty(rank);
				authority.setProperty((h + "." + rank), epithet);
			}
		}
		return authority;
	}
	
	/**
	 * Aggregate a set of taxonomic authorities. This method bundles matching
	 * authorities, but keeps non-matching ones separate. If authorities are
	 * aggregated, their sources are concatenated, as are any given lists of
	 * child epithets.
	 * @param authorities an array holding the authorities to aggregate
	 * @param rank the rank of the taxonomic epithet the authorities are for
	 * @return an array holding the aggregated authorities
	 */
	public static Properties[] aggregateAuthorities(Properties[] authorities, String rank) {
		if (authorities.length < 2)
			return authorities;
		
		//	copy and sort input (we definitely want to start the aggregates with the most complete authorities)
		Properties[] sAuthorities = new Properties[authorities.length];
		System.arraycopy(authorities, 0, sAuthorities, 0, authorities.length);
		Arrays.sort(sAuthorities, new Comparator() {
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
		
		//	aggregate authorities
		LinkedList aggregateAuthorities = new LinkedList();
		for (int h = 0; h < sAuthorities.length; h++) {
			if (sAuthorities[h] == null)
				continue;
			String authoritySource = sAuthorities[h].getProperty(AUTHORITY_SOURCE_ATTRIBUTE);
			String authorityChildren = ((childRank == null) ? null : sAuthorities[h].getProperty(childRank));
			boolean newAggregateAuthority = true;
			
			//	add authority to all aggregates it fits in with
			for (Iterator hit = aggregateAuthorities.iterator(); hit.hasNext();) {
				Properties aggregateAuthority = ((Properties) hit.next());
				String aggAuthority = null;
				String aggAuthorityName = null;
				String aggAuthorityYear = null;
				if (areAuthoritiesCompatible(aggregateAuthority, sAuthorities[h], rank)) {
					
					//	add epithets
					for (int r = 0; r < rankNames.length; r++) {
						String epithet = sAuthorities[h].getProperty(rankNames[r]);
						if (epithet != null)
							aggregateAuthority.setProperty(rankNames[r], epithet);
						if (rankNames[r].equals(rank))
							break;
					}
					
					//	add authority
					String authority = sAuthorities[h].getProperty(AUTHORITY_ATTRIBUTE);
					if ((authority != null) && ((aggAuthority == null) || (aggAuthority.length() < authority.length())))
						aggAuthority = authority;
					String authorityName = sAuthorities[h].getProperty(AUTHORITY_NAME_ATTRIBUTE);
					if ((authorityName != null) && ((aggAuthorityName == null) || (aggAuthorityName.length() < authorityName.length())))
						aggAuthorityName = authorityName;
					String authorityYear = sAuthorities[h].getProperty(AUTHORITY_YEAR_ATTRIBUTE);
					if ((authorityYear != null) && (aggAuthorityYear == null))
						aggAuthorityYear = authorityYear;
					
					//	aggregate child epithet lists
					if (authorityChildren != null) {
						String aggregateAuthorityChildren = aggregateAuthority.getProperty(childRank);
						if (aggregateAuthorityChildren == null)
							aggregateAuthority.setProperty(childRank, authorityChildren);
						else {
							TreeSet aggregateAuthorityChildSet = new TreeSet();
							aggregateAuthorityChildSet.addAll(Arrays.asList(aggregateAuthorityChildren.split("\\;")));
							aggregateAuthorityChildSet.addAll(Arrays.asList(authorityChildren.split("\\;")));
							StringBuffer aggregateAuthorityChildList = new StringBuffer();
							for (Iterator cit = aggregateAuthorityChildSet.iterator(); cit.hasNext();) {
								String childEpithet = ((String) cit.next());
								aggregateAuthorityChildList.append(childEpithet);
								if (cit.hasNext())
									aggregateAuthorityChildList.append(";");
							}
							aggregateAuthority.setProperty(childRank, aggregateAuthorityChildList.toString());
						}
					}
					
					//	aggregate source
					if (authoritySource != null) {
						String aggregateAuthoritySource = aggregateAuthority.getProperty(AUTHORITY_SOURCE_ATTRIBUTE);
						if (aggregateAuthoritySource == null)
							aggregateAuthoritySource = authoritySource;
						else if (aggregateAuthoritySource.indexOf(authoritySource) == -1)
							aggregateAuthoritySource += ("," + authoritySource);
						aggregateAuthority.setProperty(AUTHORITY_SOURCE_ATTRIBUTE, aggregateAuthoritySource);
					}
					
					//	remember we don't have to start a new aggregate authority
					newAggregateAuthority = false;
				}
			}
			
			//	no matching aggregate authority found, start new one
			if (newAggregateAuthority) {
				Properties aggregateAuthority = new Properties();
				aggregateAuthority.putAll(sAuthorities[h]);
				aggregateAuthorities.add(aggregateAuthority);
			}
		}
		
		//	finally ...
		return ((Properties[]) aggregateAuthorities.toArray(new Properties[aggregateAuthorities.size()]));
	}
	
	/**
	 * Test if two taxonomic authorities are compatible, i.e., contain matching
	 * epithets for the ranks that are present in both. This method does not
	 * compare the source attribute, but it does compare authorities if they
	 * are present in both authorities.
	 * @param authority1 the first authority
	 * @param authority2 the second authority
	 * @param rank the rank of the taxonomic epithet the authorities are for
	 * @return true if the authorities are compatible
	 */
	public static boolean areAuthoritiesCompatible(Properties authority1, Properties authority2, String rank) {
		for (int r = 0; r < rankNames.length; r++) {
			if ((r != 0) && rankNames[r-1].equals(rank))
				break;
			String epithet1 = authority1.getProperty(rankNames[r]);
			String epithet2 = authority2.getProperty(rankNames[r]);
			if ((epithet1 == null) || (epithet2 == null))
				continue;
			if (!epithet1.equalsIgnoreCase(epithet2))
				return false;
		}
		String authorityName1 = authority1.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		String authorityName2 = authority2.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		if ((authorityName1 != null) && (authorityName2 != null)) {
			if (authorityName1.endsWith("."))
				authorityName1 = authorityName1.substring(0, (authorityName1.length() - ".".length())).trim();
			if (authorityName2.endsWith("."))
				authorityName2 = authorityName2.substring(0, (authorityName2.length() - ".".length())).trim();
			if (!authorityName1.equalsIgnoreCase(authorityName2) && !StringUtils.isAbbreviationOf(authorityName1, authorityName2, false) && !StringUtils.isAbbreviationOf(authorityName2, authorityName1, false))
				return false;
		}
		String authorityYear1 = authority1.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		String authorityYear2 = authority2.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		if ((authorityYear1 != null) && (authorityYear2 != null) && !authorityYear1.equals(authorityYear2))
			return false;
		return true;
	}
	
	/**
	 * Test if two taxonomic authorities are equal, i.e., contain epithets for
	 * the same ranks, and the epithets match. This method does not compare the
	 * source attribute, but it does compare authorities if they are present in
	 * both authorities.
	 * @param authority1 the first authority
	 * @param authority2 the second authority
	 * @param rank the rank of the taxonomic epithet the authorities are for
	 * @return true if the authorities are equal
	 */
	public static boolean areAuthoritiesEqual(Properties authority1, Properties authority2, String rank) {
		for (int r = 0; r < rankNames.length; r++) {
			if ((r != 0) && rankNames[r-1].equals(rank))
				break;
			String epithet1 = authority1.getProperty(rankNames[r]);
			String epithet2 = authority2.getProperty(rankNames[r]);
			if ((epithet1 == null) && (epithet2 == null))
				continue;
			if ((epithet1 == null) || (epithet2 == null))
				return false;
			if (!epithet1.equalsIgnoreCase(epithet2))
				return false;
		}
		String authorityName1 = authority1.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		String authorityName2 = authority2.getProperty(AUTHORITY_NAME_ATTRIBUTE);
		if ((authorityName1 != null) && (authorityName2 != null)) {
			if (authorityName1.endsWith("."))
				authorityName1 = authorityName1.substring(0, (authorityName1.length() - ".".length())).trim();
			if (authorityName2.endsWith("."))
				authorityName2 = authorityName2.substring(0, (authorityName2.length() - ".".length())).trim();
			if (!authorityName1.equalsIgnoreCase(authorityName2) && !StringUtils.isAbbreviationOf(authorityName1, authorityName2, false) && !StringUtils.isAbbreviationOf(authorityName2, authorityName1, false))
				return false;
		}
		String authorityYear1 = authority1.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		String authorityYear2 = authority2.getProperty(AUTHORITY_YEAR_ATTRIBUTE);
		if ((authorityYear1 != null) && (authorityYear2 != null) && !authorityYear1.equals(authorityYear2))
			return false;
		return true;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		AuthorityProvider ap = AuthorityProvider.getAuthorityProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/AuthorityAugmenterData/")) {
			public boolean isDataEditable(String dataName) {
				return false;
			}
			public boolean isDataEditable() {
				return false;
			}
		});
		
		/* some test cases:
		 * - tribe Formicini
		 * - species Orchis maculata (need to use data URL "http://www.catalogueoflife.org/col/webservice?response=full&id=fbe4bfa0ed3a1316bf9864b2b3d6871a" for this one to prevent read timeout)
		 * - subspecies Orchis brancifortii maculata
		 * - subspecies Dactylorchis maculata arduennensis
		 */
//		testLookup(ap, "Orchis brancifortii maculata", SUBSPECIES_ATTRIBUTE, null, -1);
//		testLookup(ap, "Orchis maculata", SPECIES_ATTRIBUTE, "(Desf.) Batt.", -1);
//		testLookup(ap, "Orchis maculata", SPECIES_ATTRIBUTE, "L.", -1);
//		testLookup(ap, "Dactylorchis maculata arduennensis", SUBSPECIES_ATTRIBUTE, null, -1);
		
//		testLookup(ap, "Formicini", TRIBE_ATTRIBUTE, null, -1);
//		testLookup(ap, "Formicini", TRIBE_ATTRIBUTE, "Latr.", -1);
//		testLookup(ap, "Formicini", TRIBE_ATTRIBUTE, null, 1809);
		testLookup(ap, "Formicini", TRIBE_ATTRIBUTE, "Latr.", 1802);
	}
	
	private static void testLookup(AuthorityProvider ap, String lEpithets, String lRank, String laName, int laYear) {
		Properties a = ap.getAuthority(lEpithets.split("\\s+"), lRank, laName, laYear, true);
		if (a == null) {
			System.out.println("authority not found");
			return;
		}
		for (Iterator rit = a.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			if (rank.indexOf('.') != -1)
				break;
			System.out.println(rank + ": " + a.getProperty(rank));
			if (!rit.hasNext())
				return;
		}
		for (int r = 0;; r++) {
			String prefix = (r + ".");
			boolean prefixEmpty = true;
			for (Iterator kit = a.keySet().iterator(); kit.hasNext();) {
				String key = ((String) kit.next());
				if (!key.startsWith(prefix))
					continue;
				prefixEmpty = false;
				System.out.println(key + ": " + a.getProperty(key));
			}
			if (prefixEmpty)
				break;
		}
	}
}