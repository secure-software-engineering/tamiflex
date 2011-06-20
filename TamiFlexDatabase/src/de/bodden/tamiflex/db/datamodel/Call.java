/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package de.bodden.tamiflex.db.datamodel;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Entry in the table "Calls".
 * @author Oleg Manov and Ivaylo Petkov
 *
 */
public class Call extends DBEntry{
	/**
	 * Reflective call target.
	 */
	private String target;
	/**
	 * Reflective call thread.
	 */
	private String thread;
	/**
	 * The id in the database of the reflective call location.
	 */
	private int locationID;
	
	/**
	 * The constructor
	 * @param resultSet 
	 * 			resultset from the database.
	 * @throws SQLException
	 */
	public Call(ResultSet resultSet) throws SQLException{
			setID(resultSet.getInt(1));
			locationID=resultSet.getInt(2);
			target=resultSet.getString(3);
			thread=resultSet.getString(4);
	}
	/**
	 * The constructor
	 * @param target 		Reflective call target.
	 * @param thread		Reflective call thread.
	 * @param locationID	The id in the database of the reflective call location.
	 */
	public Call(String target,String thread,int locationID) {
		this.target=target;
		this.thread=thread;
		this.locationID=locationID;
		this.setTableName("Calls");
		this.setIdFieldName("idCalls");
		this.fields="idcalls,locationid,target,thread";
	}
	
	/**
	 * @return	the reflective call target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return the reflective call thread
	 */
	public String getThread() {
		return thread;
	}

	public String generateInsertStatement() {
		String statement="insert into ";
		statement+=getTableName();
		statement+=" (locationid,target,thread) values(";
		statement+=locationID;
		statement+=",'"+target+"'";
		statement+=",'"+thread+"'";
		statement+=")";
		return statement;
	}

	public String generateIDSearchStatement() {
		String statement="select ";
		statement+=getIdFieldName();
		statement+=" from ";
		statement+=getTableName();
		statement+=" where ";
		statement+="locationid="+locationID;
		statement+=" and ";
		statement+="target='"+target+"'";
		statement+=" and ";
		statement+="thread='"+thread+"'";
		return statement;
	}
}
