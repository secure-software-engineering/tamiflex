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
 * Entry in the table Locations.
 * @author Ivaylo Petkov and Oleg Manov
 *
 */
public class Location extends DBEntry{
	
	/**
	 * The Location class ID.
	 */
	private int classID;
	
	/**
	 * The reflective call method.
	 */
	private String method;
	
	/**
	 * The reflective call line.
	 */
	private int line;
	
	/**
	 * The reflective call type ID.
	 */
	private int callTypeID;
	
	/**
	 * The constructor.
	 * @param method
	 * 				reflective call method.
	 * @param line
	 * 				reflective call line.
	 * @param callTypeID
	 * 				reflective call type ID.
	 * @param classID
	 * 				location class ID.
	 */
	public Location(String method, int line, int callTypeID, int classID) {
		this.classID = classID;
		this.method = method;
		this.line = line;
		this.callTypeID = callTypeID;
		this.setTableName("Locations");
		this.setIdFieldName("idLocations");
		this.fields = "idlocations,classid,method,line,calltypeid";
	}
	
	/**
	 * The constructor.
	 * @param resultSet
	 * 					resultset from database.
	 * @throws SQLException
	 */
	public Location(ResultSet resultSet) throws SQLException {
		setID(resultSet.getInt(1));
		classID=resultSet.getInt(2);
		method=resultSet.getString(3);
		line=resultSet.getInt(4);
		callTypeID=resultSet.getInt(5);
	}
	
	/**
	 * @return
	 * 			the location class ID.
	 */
	public int getClassID() {
		return classID;
	}
	
	/**
	 * @return
	 * 			the reflective call method.
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * @return
	 * 			the reflective call line.
	 */
	public int getLine() {
		return line;
	}
	
	/**
	 * 	@return
	 * 			the reflective call type ID.
	 */
	public int getCallTypeID() {
		return callTypeID;
	}
	
	public String generateInsertStatement() {
		String statement="insert into ";
		statement+=getTableName();
		statement+=" (classid,method,line,calltypeid) values(";
		statement+=classID;
		statement+=",'"+method+"'";
		statement+=","+line;
		statement+=","+callTypeID;
		statement+=")";
		return statement;
	}
	
	public String generateIDSearchStatement() {
		String statement="select ";
		statement+=getIdFieldName();
		statement+=" from ";
		statement+=getTableName();
		statement+=" where ";
		statement+="classid="+classID;
		statement+=" and ";
		statement+="method='"+method+"'";
		statement+=" and ";
		statement+="line="+line;
		statement+=" and ";
		statement+="calltypeid="+callTypeID;
		return statement;
	}
	
	/**
	 * The reflective call type.
	 */
	public void setCallType(int typeID) {
		callTypeID=typeID;
	}
}
