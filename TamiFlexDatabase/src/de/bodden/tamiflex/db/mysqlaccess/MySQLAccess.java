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
package de.bodden.tamiflex.db.mysqlaccess;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * @author Ivaylo Petkov and Oleg Manov
 */
public class MySQLAccess {
	
	private static Connection connect = null;
	private static Statement statement = null;
	private static ResultSet resultSet = null;
	
	private static String dataBaseName="tamiflexDB";
	private static String dbURL;
	private static String userName;
	private static String userPass;
	
	/**
	 * Connects to the database.
	 * @param dbURL_	URL address of the database
	 * @param userName_	User name
	 * @param userPass_	Password
	 */
	public static boolean connect(String dbURL_,String userName_, String userPass_) {
		dbURL = dbURL_;
		userName = userName_;
		userPass = userPass_;
		try {
			return connectToDB();
		} catch (Exception e) {
			return false;
		}
	}
	
	private static boolean connectToDB() 
	{
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect=DriverManager.getConnection(dbURL+dataBaseName+"?user="+userName+"&password="+userPass);
	        statement = connect.createStatement();
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (SQLException e) {
			return false;
		}
	}
	

	/**
	 * Executes query to the database.
	 * @param query		SQL Query
	 * @return			The result of the query
	 * @throws SQLException
	 */
	public static ResultSet executeQuery(String query) throws SQLException
	{
		resultSet=statement.executeQuery(query);
		return resultSet;
	}
	
	/**
	 * Executes update to the database
	 * @param command
	 * @throws SQLException
	 */
	public static void executeUpdate(String command) throws SQLException
	{
		statement.executeUpdate(command);
	}

	/**
	 * Closes the database
	 */
	public static void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
			if (statement != null) {
				statement.close();
			}
			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {
		}
	}

}
