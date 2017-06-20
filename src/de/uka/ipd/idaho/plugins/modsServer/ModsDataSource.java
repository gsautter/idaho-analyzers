/**
 * 
 */
package de.uka.ipd.idaho.plugins.modsServer;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import de.uka.ipd.idaho.plugins.modsReferencer.ModsConstants;
import de.uka.ipd.idaho.plugins.modsReferencer.ModsUtils.ModsDataSet;

/**
 * A MODS data source connects a MODS servlet to a specific source of document
 * meta data. It also has the responsibility to transform the raw data it
 * downloads from its backing source into the standardized representation.
 * Instances of this class are not supposed to do any internal caching, as this
 * is done centrally - this class is meant to be a lightweight wrapper for some
 * meta data source only.
 * 
 * @author sautter
 */
public abstract class ModsDataSource implements ModsConstants {
	
	/**
	 * the folder there the data source can store its data, e.g. configuration
	 * files to access the backing source
	 */
	protected File dataPath;
	
	/**
	 * Make the data source know where its data is located
	 * @param dataPath the data folder for the data source
	 */
	public void setDataPath(File dataPath) {
		this.dataPath = dataPath;
	}
	
	/**
	 * Initialize the data source. This method is invoced after the data path is
	 * set. It is meant to read configuration files, etc. This default
	 * implementation does nothing, sub classes are welcome to overwrite it as
	 * needed.
	 */
	public void init() {}
	
	/**
	 * Retrieve a MODS document meta data set from the source backing the data
	 * source, using the document ID as the access key. If the backing data
	 * source does not provide a meta data set for the document with the
	 * specified ID, this method should return null. If the meta data is
	 * incomplete (the ModsUtils.getErrorReport() method does report errors),
	 * this should not bother the data source. Completing the meta data sets
	 * might require user input and is therefore handled elsewhere.
	 * @param modsId the ID of the document to retrieve the meta data for
	 * @return the meta data of the document with the specified ID
	 * @throws IOException
	 */
	public abstract ModsDataSet getModsData(String modsId) throws IOException;
	
	/**
	 * Find the MODS document meta data set in the source backing the data
	 * source, using known search attributes the access key, e.g. the document
	 * author, parts of the title, the name of the journal the document appeared
	 * in, or the name or location of the publisher who issued the document. If
	 * multiple meta data sets match, this method should return them all. Only
	 * if the search criteria are empty, this method may ignore them and return
	 * either of null or an empty array. If the backing data source does not
	 * provide any meta data sets that match the search criteria, this method
	 * may return null or an empty array. If some of the meta data sets are
	 * incomplete (the ModsUtils.getErrorReport() method does report errors),
	 * this should not bother the data source. Completing the meta data sets
	 * might require user input and is therefore handled elsewhere.
	 * @param modsData the known elements of the meta data to retrieve in full
	 * @return the meta data sets matching the specified criteria
	 * @throws IOException
	 */
	public abstract ModsDataSet[] findModsData(Properties modsData) throws IOException;
	
	/**
	 * Indicate if this MODS data source can store MODS meta data sets, i.e.
	 * write them to the backing source. If this method returns false, the
	 * storeModsData() method should throw an UnsupportedOperationException. If
	 * this method returns true, it must not do so. This default implementation
	 * simply returns false, sub classes are welcome to overwrite it as needed.
	 * @return true if the MODS data source can store meta data sets, false
	 *         otherwise
	 */
	public boolean canStoreModsData() {
		return false;
	}
	
	/**
	 * Store a MODS document meta data set in this data source, i.e. write them
	 * to the backing source. If the backing source cannot be written to, this
	 * method should throw an UnsupportedOperationException, and the
	 * canStoreModsData() method must return false. This method is not meant to
	 * cache meta data sets locally. If it returns true, the meta data set has
	 * to be available from the backing source. This default implementation does
	 * throw an UnsupportedOperationException, sub classes are welcome to
	 * overwrite it as needed.
	 * @param modsData the meta data set to store
	 * @return true if the data was created (stored for the first time), false
	 *         if it was updated
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
	public boolean storeModsData(ModsDataSet modsData) throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException("");
	}
}
