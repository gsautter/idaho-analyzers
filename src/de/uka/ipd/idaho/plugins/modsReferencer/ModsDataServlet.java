/**
 * 
 */
package de.uka.ipd.idaho.plugins.modsReferencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.SqlQueryResult;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.easyIO.sql.TableDefinition;
import de.uka.ipd.idaho.easyIO.web.WebServlet;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This servlet provides an integrated source of MODS document meta data sets
 * from different original sources and a unified interface to access it. It
 * connects to the different sources by means of ModsDataSource objects.<br>
 * In addition, the servlet can store client provided MODS document meta data
 * sets. It thus provides a central store for meta data sets that none of the
 * integrated providers provides so far. To allow document meta data providers
 * to integrate these client entered document meta data sets in their databases,
 * this servlet writes an RSS feed of new meta data being entered.
 * 
 * @author sautter
 */
public class ModsDataServlet extends WebServlet implements ModsConstants {
	
	private File rootFolder;
	
	private ModsDataCache cache;
	private ModsDataSource[] sources;
	
	private String passPhraseHash = "0";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		
		//	load settings
		this.passPhraseHash = ("" + this.getSetting("passPhrase", "").hashCode());
		
		//	get data source base directory
		final File dataSourceFolder = new File(this.rootFolder, "dataSources");
		if(!dataSourceFolder.exists())
			dataSourceFolder.mkdir();
		
		//	get data sources
		Object[] sourceObject = GamtaClassLoader.loadComponents(
				dataSourceFolder, 
				ModsDataSource.class, 
				new ComponentInitializer() {
					public void initialize(Object component, String componentJarName) throws Exception {
						File dataPath = new File(dataSourceFolder, (componentJarName.substring(0, (componentJarName.length() - 4)) + "Data"));
						if (!dataPath.exists())
							dataPath.mkdir();
						((ModsDataSource) component).setDataPath(dataPath);
						((ModsDataSource) component).init();
					}
				});
		
		//	store data sources
		this.sources = new ModsDataSource[sourceObject.length];
		for (int c = 0; c < sourceObject.length; c++)
			this.sources[c] = ((ModsDataSource) sourceObject[c]);
		
		
		//	initialize cache
		this.cache = new ModsDataCache();
		File dataCachePath = new File(this.rootFolder, "dataCache");
		if (!dataCachePath.exists())
			dataCachePath.mkdir();
		this.cache.setDataPath(dataCachePath);
		this.cache.init();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#exit()
	 */
	protected void exit() {
		//	disconnect from database
		this.cache.io.close();
	}
	
	private static final String[] searchableAttributesRaw = {
		MODS_AUTHOR_ATTRIBUTE,
		MODS_DATE_ATTRIBUTE,
		MODS_TITLE_ATTRIBUTE,
	};
	private static final HashSet searchableAttributes = new HashSet(Arrays.asList(searchableAttributesRaw));
	
	private static final String getModsIdKey(ModsDataSet mds) {
		return (mds.idType + "-" + mds.id);
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	relay path
		if (request.getParameter("showPath") != null) {
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			PrintWriter out = response.getWriter();
			out.println("'' --> " + new File("").getAbsolutePath().replaceAll("\\\\", "/"));
			out.println("'.' --> " + new File(".").getAbsolutePath().replaceAll("\\\\", "/"));
			out.println("'./' --> " + new File("./").getAbsolutePath().replaceAll("\\\\", "/"));
			out.println("rootFolder --> " + this.rootFolder.getAbsolutePath().replaceAll("\\\\", "/"));
			out.flush();
			return;
		}
		
		//	dump cache index db
		else if (request.getParameter("showCache") != null) {
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			PrintWriter out = response.getWriter();
			this.cache.dumpIndexDB(out);
			out.flush();
			return;
		}
		
		//	request for search attributes
		else if (request.getParameter("getSearchAttributes") != null) {
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			PrintWriter out = response.getWriter();
			for (Iterator pit = searchableAttributes.iterator(); pit.hasNext();)
				out.println((String) pit.next());
			out.flush();
			return;
		}
		
		//	check ID query
		String modsId = request.getParameter(MODS_ID_ATTRIBUTE);
		
		//	search query
		if (modsId == null) {
			
			//	get search parameters
			boolean cacheOnly = (request.getParameter("cacheOnly") != null);
			
			//	get search attributes
			Properties search = new Properties();
			for (Iterator pit = searchableAttributes.iterator(); pit.hasNext();) {
				String searchAttribute = ((String) pit.next());
				String searchValue = request.getParameter(searchAttribute);
				if (searchValue != null)
					search.setProperty(searchAttribute, searchValue);
			}
			
			//	check attributes
			if (search.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No search parameters specified");
				return;
			}
			
			//	prepare search
			HashMap searchResult = new LinkedHashMap();
			ModsDataSet[] mdss;
			
			//	do search
			mdss = this.cache.findModsData(search);
			if (mdss != null) {
				for (int d = 0; d < mdss.length; d++)
					searchResult.put(getModsIdKey(mdss[d]), mdss[d]);
			}
			
			if (!cacheOnly) {
				for (int s = 0; s < this.sources.length; s++) try {
					mdss = this.sources[s].findModsData(search);
					if (mdss == null)
						continue;
					
					for (int d = 0; d < mdss.length; d++) {
						String modsIdKey = getModsIdKey(mdss[d]);
						if (searchResult.containsKey(modsIdKey))
							continue;
						
						searchResult.put(modsIdKey, mdss[d]);
						
						this.cache.storeModsData(mdss[d]);
					}
				}
				catch (IOException ioe) {
					System.out.println("Error getting MODS data from " + this.sources[s].getClass().getName());
					ioe.printStackTrace(System.out);
				}
			}
			
			//	report failure
			if (searchResult.isEmpty())
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			
			//	send MODS data
			else {
				response.setContentType("text/xml");
				response.setCharacterEncoding("utf-8");
				Writer out = response.getWriter();
				for (Iterator rit = searchResult.values().iterator(); rit.hasNext();)
					((ModsDataSet) rit.next()).writeModsHeader(out, true);
				out.flush();
			}
		}
		
		//	ID query
		else {
			
			//	do cache lookup
			ModsDataSet mds = this.cache.getModsData(modsId);
			
			//	cache miss
			if (mds == null) {
				
				//	check data sources
				for (int s = 0; s < this.sources.length; s++) try {
					mds = this.sources[s].getModsData(modsId);
					if (mds != null)
						s = this.sources.length;
				}
				catch (IOException ioe) {
					System.out.println("Exception getting MODS data from " + this.sources[s].getClass().getName() + ": " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
				
				//	cache data if found
				if (mds != null)
					this.cache.storeModsData(mds);
			}
			
			//	report failure
			if (mds == null)
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			
			//	send MODS data
			else {
				response.setContentType("text/xml");
				response.setCharacterEncoding("utf-8");
				Writer out = response.getWriter();
				mds.writeModsHeader(out, true);
				out.flush();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	check authentication (passphrase)
		String pass = request.getHeader("pass");
		if ((pass == null) || (!this.passPhraseHash.equals(pass) && !this.passPhraseHash.equals("" + pass.hashCode()))) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Pass phrase required for storing data.");
			return;
		}
		
		//	receive MODS header
		InputStreamReader isr = new InputStreamReader(request.getInputStream(), "UTF-8");
		MutableAnnotation modsDocument = SgmlDocumentReader.readDocument(isr);
		isr.close();
		
		//	check raw data
		QueriableAnnotation[] modsHeaders = modsDocument.getAnnotations("mods:mods");
		if (modsHeaders.length != 1) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Send one MODS meta data set at a time.");
			return;
		}
		
		//	check MODS data
		ModsDataSet mds = ModsUtils.getModsDataSet(modsHeaders[0]);
		if (!mds.isValid()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This MODS meta data set is not valid.");
			return;
		}
		
		//	store MODS data
		try {
			boolean created = this.cache.storeModsData(mds);
			response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK);
		}
		catch (IOException ioe) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
		}
		response.flushBuffer();
	}
	
	private class ModsDataCache extends ModsDataSource {
		
		private static final String MODS_TABLE_NAME = "ModsData";
		
		private static final String MODS_ID_TYPE_ATTRIBUTE = "ModsIdType";
		private static final String MODS_ID_KEY_ATTRIBUTE = "ModsIdKey";
		
		private IoProvider io;
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#init()
		 */
		public void init() {
			
			// load settings
			Settings config = Settings.loadSettings(new File(this.dataPath, "config.cnfg"));
			
			//	try fixing derby's DB path
			System.out.println(new File(".").getAbsolutePath());
			config.setSetting("JDBC.Url", "jdbc:derby:" + this.dataPath.getAbsolutePath().replaceAll("\\\\", "/") + "/ModsDB;create=true");
			
			// get database access
			this.io = EasyIO.getIoProvider(config);
			if (!this.io.isJdbcAvailable())
				throw new RuntimeException("Cannot work without database access.");
			
			// create ensure database (Derby)
			TableDefinition td = new TableDefinition(MODS_TABLE_NAME);
			td.addColumn(MODS_ID_KEY_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, 64);
			td.addColumn(MODS_ID_TYPE_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, 32);
			td.addColumn(MODS_ID_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, 32);
			td.addColumn(MODS_DATE_ATTRIBUTE, TableDefinition.INT_DATATYPE, 0);
			td.addColumn(MODS_AUTHOR_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, 255);
			td.addColumn(MODS_TITLE_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, 511);
			if (!this.io.ensureTable(td, true))
				throw new RuntimeException("Cannot work without database access.");
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#getModsData(java.lang.String)
		 */
		public ModsDataSet getModsData(String modsId) throws IOException {
			return this.loadModsDataFile(modsId);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#findModsData(java.util.Properties)
		 */
		public ModsDataSet[] findModsData(Properties modsData) throws IOException {
			
			//	build query (restrict to configured collection)
			String searchPredicates = "1=1";
			for (Iterator pit = modsData.keySet().iterator(); pit.hasNext();) {
				String searchAttribute = ((String) pit.next());
				if (MODS_AUTHOR_ATTRIBUTE.equals(searchAttribute) || MODS_AUTHOR_ATTRIBUTE.equals(searchAttribute))
					searchPredicates += (" AND " + searchAttribute + " LIKE '%" + EasyIO.prepareForLIKE(modsData.getProperty(searchAttribute)) + "%'");
				else if (MODS_DATE_ATTRIBUTE.equals(searchAttribute)) try {
					searchPredicates += (" AND " + MODS_DATE_ATTRIBUTE + " = " + Integer.parseInt(modsData.getProperty(searchAttribute)));
				} catch (NumberFormatException nfe) {}
			}
			
			//	check attributes
			if (searchPredicates.length() == "1=1".length())
				return null;
			
			// assemble query
			String query = "SELECT " + MODS_ID_ATTRIBUTE + 
					" FROM " + MODS_TABLE_NAME +
					" WHERE " + searchPredicates +
					";";
			
			SqlQueryResult sqr = null;
			try {
				sqr = this.io.executeSelectQuery(query.toString());
				ArrayList mdsList = new ArrayList();
				while (sqr.next()) {
					String modsId = sqr.getString(0);
					try {
						ModsDataSet mds = this.getModsData(modsId);
						if (mds != null)
							mdsList.add(mds);
					}
					catch (IOException ioe) {
						System.out.println("Error getting MODS data for search result " + modsId);
						ioe.printStackTrace(System.out);
					}
				}
				return ((ModsDataSet[]) mdsList.toArray(new ModsDataSet[mdsList.size()]));
			}
			catch (SQLException sqle) {
				System.out.println("ModsDataCache: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while listing documents.");
				System.out.println("  query was " + query);
				if (sqr != null) sqr.close();
				return null;
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#canStoreModsData()
		 */
		public boolean canStoreModsData() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.plugins.modsServer.ModsDataSource#storeModsData(de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet)
		 */
		public boolean storeModsData(ModsDataSet modsData) throws IOException, UnsupportedOperationException {
			
			// store MODS header
			this.storeModsDataFile(modsData);
			
			//	produce database key
			String modsIdKey = getModsIdKey(modsData);
			
			//	collect updates
			StringVector assignments = new StringVector();
			assignments.addElement(MODS_ID_TYPE_ATTRIBUTE + " = '" + EasyIO.sqlEscape(modsData.idType) + "'");
			assignments.addElement(MODS_ID_ATTRIBUTE + " = '" + EasyIO.sqlEscape(modsData.id) + "'");
			assignments.addElement(MODS_DATE_ATTRIBUTE + " = " + modsData.getYear() + "");
			assignments.addElement(MODS_AUTHOR_ATTRIBUTE + " = '" + EasyIO.sqlEscape(modsData.getAuthorString()) + "'");
			assignments.addElement(MODS_TITLE_ATTRIBUTE + " = '" + EasyIO.sqlEscape(modsData.getTitle()) + "'");
			
			// write new values
			String updateQuery = ("UPDATE " + MODS_TABLE_NAME + 
					" SET " + assignments.concatStrings(", ") + 
					" WHERE " + MODS_ID_KEY_ATTRIBUTE + " LIKE '" + modsIdKey + "'" +
					";");

			try {

				// update did not affect any rows ==> new document
				if (this.io.executeUpdateQuery(updateQuery) == 0) {
					
					// gather complete data for creating master table record
					StringBuffer fields = new StringBuffer(MODS_ID_KEY_ATTRIBUTE);
					StringBuffer fieldValues = new StringBuffer("'" + EasyIO.sqlEscape(getModsIdKey(modsData)) + "'");
					fields.append(", " + MODS_ID_TYPE_ATTRIBUTE);
					fieldValues.append(", '" + EasyIO.sqlEscape(modsData.idType) + "'");
					fields.append(", " + MODS_ID_ATTRIBUTE);
					fieldValues.append(", '" + EasyIO.sqlEscape(modsData.id) + "'");
					fields.append(", " + MODS_DATE_ATTRIBUTE);
					fieldValues.append(", " + modsData.getYear() + "");
					fields.append(", " + MODS_AUTHOR_ATTRIBUTE);
					fieldValues.append(", '" + EasyIO.sqlEscape(modsData.getAuthorString()) + "'");
					fields.append(", " + MODS_TITLE_ATTRIBUTE);
					fieldValues.append(", '" + EasyIO.sqlEscape(modsData.getTitle()) + "'");
					
					// store data in collection main table
					String insertQuery = "INSERT INTO " + MODS_TABLE_NAME + 
							" (" + fields.toString() + ") " +
							"VALUES (" + fieldValues.toString() + ")" +
							";";
					try {
						this.io.executeUpdateQuery(insertQuery);
						return true;
					}
					catch (SQLException sqle) {
						System.out.println("ModsDataCache: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while storing meta data set.");
						System.out.println("  query was " + insertQuery);
						throw new IOException(sqle.getMessage());
					}
				}
				else return false;
			}
			catch (SQLException sqle) {
				System.out.println("ModsDataCache: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while updating meta data set.");
				System.out.println("  query was " + updateQuery);
				throw new IOException(sqle.getMessage());
			}
		}
		
		void storeModsDataFile(ModsDataSet mods) throws IOException {
			Writer out = new OutputStreamWriter(new FileOutputStream(this.getStorageFile(mods.id, true)), "UTF-8");
			mods.writeModsHeader(out, true);
			out.flush();
			out.close();
		}
		
		ModsDataSet loadModsDataFile(String modsId) throws IOException {
			File storageFile = this.getStorageFile(modsId, false);
			if (storageFile.exists()) {
				Reader in = new InputStreamReader(new FileInputStream(this.getStorageFile(modsId, false)), "UTF-8");
				try {
					return ModsUtils.getModsDataSet(SgmlDocumentReader.readDocument(in));
				}
				finally {
					in.close();
				}
			}
			else return null;
		}
		
		private File getStorageFile(String modsId, boolean create) throws IOException {
			String storageFileName = this.getStorageFileName(modsId);
			
			File primaryFolder = new File(this.dataPath, storageFileName.substring(0, 2));
			if (create && !primaryFolder.exists())
				primaryFolder.mkdir();
			File secondaryFolder = new File(primaryFolder, storageFileName.substring(2, 4));
			if (create && !secondaryFolder.exists())
				secondaryFolder.mkdir();
			
			File storageFile = new File(secondaryFolder, (storageFileName + ".xml"));
			if (create) {
				if (storageFile.exists()) {
					File previousVersionDocFile = new File(secondaryFolder, (storageFileName + ".xml"));
					storageFile.renameTo(previousVersionDocFile);
					storageFile = new File(secondaryFolder, (storageFileName + ".xml"));
				}
				storageFile.createNewFile();
			}
			return storageFile;
		}
		
		private String getStorageFileName(String modsId) {
			String sfn = ("" + modsId.hashCode());
			while (sfn.length() < 6)
				sfn += (modsId + sfn.length()).hashCode();
			return sfn;
		}
		
		void dumpIndexDB(PrintWriter out) throws IOException {
			StringVector fields = new StringVector();
			fields.addElement(MODS_ID_KEY_ATTRIBUTE);
			fields.addElement(MODS_ID_TYPE_ATTRIBUTE);
			fields.addElement(MODS_ID_ATTRIBUTE);
			fields.addElement(MODS_DATE_ATTRIBUTE);
			fields.addElement(MODS_AUTHOR_ATTRIBUTE);
			fields.addElement(MODS_TITLE_ATTRIBUTE);
			out.println("\"" + fields.concatStrings("\",\"") + "\"");
			
			// assemble query
			String query = "SELECT " + fields.concatStrings(", ") + 
					" FROM " + MODS_TABLE_NAME +
					";";
			
			SqlQueryResult sqr = null;
			try {
				sqr = this.io.executeSelectQuery(query.toString());
				while (sqr.next()) {
					for (int f = 0; f < fields.size(); f++) {
						if (f != 0)
							out.print(",");
						out.print("\"" + sqr.getString(f) + "\"");
					}
					out.println();
				}
			}
			catch (SQLException sqle) {
				out.println("ModsDataCache: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while dumping cache.");
				out.println("  query was " + query);
				if (sqr != null) sqr.close();
			}
		}
	}
}
