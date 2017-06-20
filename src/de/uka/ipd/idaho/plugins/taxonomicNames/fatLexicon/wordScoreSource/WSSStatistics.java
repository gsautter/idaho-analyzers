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

package de.uka.ipd.idaho.plugins.taxonomicNames.fatLexicon.wordScoreSource;


import java.sql.SQLException;

import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.SqlQueryResult;
import de.uka.ipd.idaho.stringUtils.StatisticStringIndex;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;

/** Wrapper for 2 persistent StatisticStringIndex-es.
 * Uses Database to keep data up-to-date.
 * @author kristof
 */
public class WSSStatistics {
	
	protected StatisticStringIndex pos = null;
	protected StatisticStringIndex neg = null;
	
	// Modified items
	private StringVector posModifylist = null;
	private StringVector negModifylist = null;
	
	// New items
	private StringVector posInsertlist = null;
	private StringVector negInsertlist = null;
	
	private StringIndex newPosStat = new StringIndex(false);
	private StringVector newPosList = new StringVector(false);
	private StringIndex newNegStat = new StringIndex(false);
	private StringVector newNegList = new StringVector(false);

	private boolean posCleared = false;
	private boolean negCleared = false;
	
	/**
	 * Creats internal StatisticStringIndex-es with given length.
	 * @param mexLength	Maximal sequence length the StatisticStringIndexes are created for.
	 */
	public WSSStatistics(int maxLength) {
		this.pos = new StatisticStringIndex(maxLength);
		this.neg = new StatisticStringIndex(maxLength);
		this.posModifylist = new StringVector();
		this.negModifylist = new StringVector();
		this.posInsertlist = new StringVector();
		this.negInsertlist = new StringVector();
	}
	
	// ---------- Persistency functions ----------
	
	private String buildValueList(StringVector keys, StatisticStringIndex counterSource, int wssID, int isPos) {
		StringBuffer sb = new StringBuffer();
		
		boolean isFirst = true;
		for (int i=0; i<keys.size(); i++) {
			int counter = counterSource.getCount(keys.get(i));
			if (isFirst) {
				isFirst=false;
			} else {
				sb.append(", ");	// Separator between records
			}
			
			// (WSSID,IsPos,Sequence,Count)
			
			sb.append("(");	// Begin of record
			sb.append(wssID);
			sb.append(",");
			sb.append(isPos);	// IsPos status (0:negative, 1:positive)
			sb.append(",'");
			sb.append(keys.get(i));	// Current sequence
			sb.append("',");
			sb.append(counter);	// Counter value
			
			sb.append(")");	// End of record
		}
		
		return sb.toString();
	}
		
	private StringVector getUpdateQueries(String tableName, int wssID) {
		StringVector result = new StringVector();
		
		/* Database table of the statistic
		 * - WSSID (int)	ID of the WSS (assigned at saving time)
		 * - IsPos	(1 or 0 for positive and negative counter)
		 * - Sequence (String)
		 * - Count	(int)	(The times the sequence was added to the StatisticStringIndex)
		 */
		
		//	Need delete?
		if (this.posCleared) {
			StringBuffer sb = new StringBuffer();
			sb.append("DELETE * FROM ");
			sb.append(tableName);
			sb.append(" WHERE WSSID=");
			sb.append(wssID);
			sb.append(" IsPos=1;");
			result.addElement(sb.toString());
		}
		
		if (this.negCleared) {
			StringBuffer sb = new StringBuffer();
			sb.append("DELETE * FROM ");
			sb.append(tableName);
			sb.append(" WHERE WSSID=");
			sb.append(wssID);
			sb.append(" IsPos=0;");
			result.addElement(sb.toString());
		}
		
		
		// Insert list for positive set
		if (this.posInsertlist.size()>0) {
			StringBuffer sb = new StringBuffer();
			sb.append("INSERT INTO "+tableName+" (WSSID,IsPos,Sequence,Count) VALUES ");
			sb.append(buildValueList(this.posModifylist,this.pos,wssID,1)); 
			result.addElement(sb.toString());
		}

		// Insert list for negative set
		if (this.negInsertlist.size()>0) {
			StringBuffer sb = new StringBuffer();
			sb.append("INSERT INTO "+tableName+" (WSSID,IsPos,Sequence,Count) VALUES ");
			sb.append(buildValueList(this.negModifylist,this.neg,wssID,0)); 
			result.addElement(sb.toString());
		}
		
		// Update list for positive set
		if (this.posModifylist.size()>0) {
			for (int i=0; i<this.posModifylist.size(); i++) {
				StringBuffer sb = new StringBuffer();
				String sequence = this.posModifylist.get(i);
				int counter = this.pos.getCount(sequence);
				sb.append("UPDATE (Counter) TO ");
				sb.append(counter);
				sb.append(" FROM ");
				sb.append(tableName);
				sb.append(" WHERE WSSID=");
				sb.append(wssID);
				sb.append(" AND IsPos=1 AND Sequence='");
				sb.append(sequence);
				sb.append("';");
				result.addElement(sb.toString());
			}
		}
		
		// Update list for negative set
		if (this.negModifylist.size()>0) {
			for (int i=0; i<this.negModifylist.size(); i++) {
				StringBuffer sb = new StringBuffer();
				String sequence = this.negModifylist.get(i);
				int counter = this.neg.getCount(sequence);
				sb.append("UPDATE ");
				sb.append(tableName);
				sb.append(" SET Counter=");
				sb.append(counter);
				sb.append(" WHERE WSSID=");
				sb.append(wssID);
				sb.append(" AND IsPos=0 AND Sequence='");
				sb.append(sequence);
				sb.append("';");
				result.addElement(sb.toString());
			}
		}
		return result;
	}
	
	private void mark() {
		this.posCleared = false;
		this.negCleared = false;
		this.posModifylist.clear();
		this.negModifylist.clear();
		this.posInsertlist.clear();
		this.negInsertlist.clear();
	}
	
	/**
	 * Saves the content changes since last update into the database. 
	 * @param io	StandardIoProvider to use for database connection.
	 * @param tableName	The name of table to save the data to. 
	 * @param wssID	The ID of the WSS these data belong to.
	 * @return	List of queries which couldn't be executed. They are needed for a complete update.
	 */
	public StringVector updateDatabase(IoProvider io, String tableName, int wssID) {
		StringVector errorQueries = new StringVector(false);
		
		try {
			StringVector queries = getUpdateQueries(tableName, wssID);
			
			// Execute queries
			for (int i=0; i<queries.size(); i++) {
				try {
					io.executeUpdateQuery(queries.get(i));
				} catch (SQLException ex) {
					errorQueries.addElement(queries.get(i));
					System.out.println(ex);
				}
			}
			
			// Set mark
			mark();
			
		} catch (Exception ex) {
			System.out.println(ex);
		}
		
		return errorQueries;
	}
	
	/**
	 * Load data from database. Previous content is cleared!
	 * @param io	StandardIoProvider to use for accessing the database.
	 * @param tableName	Name of table to retrieve data from.
	 * @param wssID	ID of the WSS the data should be loaded of.
	 * @return	true if successful, false otherwise.
	 */
	public boolean loadFromDatabase(IoProvider io, String tableName, int wssID) {
		mark();
		String query = "SELECT Sequence, Counter FROM "+tableName+" WHERE WSSID="+wssID+" AND IsPos=1";
		try {
			SqlQueryResult result = io.executeSelectQuery(query);
			for (int row = 0; row < result.getRowCount(); row++ ) {
				String sequence = result.getString(row,0);
				String countString = result.getString(row,1);
				int count = Integer.parseInt(countString);
				this.pos.add(sequence,count);
			}
		} catch (SQLException ex) {
			System.out.println(ex);
			return false;
			}
		
		query = "SELECT Sequence, Counter FROM "+tableName+" WHERE WSSID="+wssID+" AND IsPos=0";
		try {
			SqlQueryResult result = io.executeSelectQuery(query);
			for (int row = 0; row < result.getRowCount(); row++ ) {
				String sequence = result.getString(row,0);
				String countString = result.getString(row,1);
				int count = Integer.parseInt(countString);
				this.neg.add(sequence,count);
			}
		} catch (SQLException ex) {
			System.out.println(ex);
			return false;
			}

		return true;
	}
	
	
	// ---------- Wrapper functions ----------
	/**
	 * Wrapper to function @see StatisticStringIndex#getFactor(String)
	 */
	public double getFactor(String string, boolean isPositive) {
		if (isPositive) {
			return this.pos.getFactor(string);
		} else {
			return this.neg.getFactor(string);
		}
	}

	/**
	 * Wrapper to function @see StatisticStringIndex#getMarkovFactor(String)
	 */
	public double getMarkovFactor(String string, boolean isPositive) {
		if (isPositive) {
			return this.pos.getMarkovFactor(string);
		} else {
			return this.neg.getMarkovFactor(string);
		}
	}

	/**
	 * Wrapper to function @see StatisticStringIndex#clear()
	 */
	public void clear(boolean isPositive) {
		if (isPositive) {
			this.posCleared = true;
			this.pos.clear();
			this.posModifylist.clear();
			this.posInsertlist.clear();
		} else {
			this.negCleared = true;
			this.neg.clear();
			this.negModifylist.clear();
			this.negInsertlist.clear();
		}
	}
	
	/**	write data stored through the remember methods to the statistics
	 */
	public void commitUpdates() {
		for (int i = 0; i < this.newPosList.size(); i++) {
			if (this.pos.add(this.newPosList.get(i), this.newPosStat.getCount(this.newPosList.get(i)))) {
				this.posInsertlist.addElementIgnoreDuplicates(this.newPosList.get(i));
			} else {
				this.posModifylist.addElementIgnoreDuplicates(this.newPosList.get(i));
			}
		}
		this.newPosList.clear();
		this.newPosStat.clear();
		
		for (int i = 0; i < this.newNegList.size(); i++) {
			if (this.neg.add(this.newNegList.get(i), this.newNegStat.getCount(this.newNegList.get(i)))) {
				this.negInsertlist.addElementIgnoreDuplicates(this.newNegList.get(i));
			} else {
				this.negModifylist.addElementIgnoreDuplicates(this.newNegList.get(i));
			}
		}
		this.newNegList.clear();
		this.newNegStat.clear();
	}
	
	/**
	 * Wrapper to function @see StatisticStringIndex#add(String, int)
	 */
	public void add(String string, int weight, boolean isPositive) {
		if (isPositive) {
			this.newPosList.addElementIgnoreDuplicates(string);
			this.newPosStat.add(string, weight);
		} else {
			this.newNegList.addElementIgnoreDuplicates(string);
			this.newNegStat.add(string, weight);
		}
		
		/*if (isPositive) {
			if (this.pos.add(string, weight)) {
				this.posInsertlist.addElementIgnoreDuplicates(string);
			} else {
				this.posModifylist.addElementIgnoreDuplicates(string);
			}
		} else {
			if (this.neg.add(string, weight)) {
				this.negInsertlist.addElementIgnoreDuplicates(string);
			} else {
				this.negModifylist.addElementIgnoreDuplicates(string);
			}
		}*/
	}
	
	/**
	 * Wrapper to function @see StatisticStringIndex#add(String)
	 */
	public void add(String string, boolean isPositive) {
		this.add(string, 1, isPositive);
	}
	
	/**
	 * Wrapper to function @see StatisticStringIndex#multiply(String, double)
	 */
	public void multiply(String string, double multiplier, boolean isPositive) {
		if (isPositive) {
			int oldCount = this.pos.getCount(string);
			int newCount = (int) (oldCount * multiplier);
			int diff = ((oldCount == newCount) ? 1 : (newCount - oldCount));
			this.add(string, diff, true);
			
			/*if (this.pos.multiply(string, multiplier)) {
				this.posInsertlist.addElementIgnoreDuplicates(string);
			} else {
				this.posModifylist.addElementIgnoreDuplicates(string);
			}*/
		} else {
			int oldCount = this.pos.getCount(string);
			int newCount = (int) (oldCount * multiplier);
			int diff = ((oldCount == newCount) ? 1 : (newCount - oldCount));
			this.add(string, diff, false);
			
			/*if (this.neg.multiply(string, multiplier)) {
				this.negInsertlist.addElementIgnoreDuplicates(string);
			} else {
				this.negModifylist.addElementIgnoreDuplicates(string);
			}*/
		}
	}
}
