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


import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import de.bodden.tamiflex.db.datamodel.*;




/**
 * Database Controller
 * 
 * @author Ivaylo Petkov and Oleg Manov
 *
 */
public class DBController{
	
	/**
	 * Current run id.
	 */
	private static int runID;
	/**
	 * Connection status.
	 */
	public static boolean connected=false;
	/**
	 * Insert buffer.
	 */
	public static StringBuffer buffer=new StringBuffer();

	
	/**
	 * Connects to the the database
	 * @param input 	
	 * 					if true adds new entry in the table runs
	 * @param url		
	 * 					url address of the database
	 * @param username	
	 * @param password
	 * @return 
	 */
	public static boolean connect(boolean input,String url, String username,String password){
		if(!connected)
			connected=MySQLAccess.connect(url,username, password);
		if(input&&connected){
			String host="noname";
			try{
				host=InetAddress.getLocalHost().getHostName();
			}catch (Exception e){
			}
			java.util.Date today = new java.util.Date();
			Run currentRun=new Run(host,new java.sql.Timestamp(today.getTime()));
		
			String statement=currentRun.generateIDSearchStatement();
			String newstatement=currentRun.generateInsertStatement();
			int i=-1;
			try {
				MySQLAccess.executeUpdate(newstatement);
				i=handleResultSet(MySQLAccess.executeQuery(statement));
			} catch (SQLException e) {
				e.printStackTrace();
			}
			currentRun.setID(i);
			runID=i;
		}
		return connected;
	}
	
	/**
	 *  Closes the database
	 */
	public static void closeDB(){
		MySQLAccess.close();
	}
	
	/**
	 * Checks if all tables exist.
	 * @return
	 */
	public static boolean checkTables(){
		if(tableExists("calls")&&tableExists("calltypes")&&tableExists("classid")&&
				tableExists("locations")&&tableExists("runs")&&tableExists("runtocall"))
					return true;
		else return false;
	}
	
	/**
	 * Checks if a table exists.
	 * @param tableName
	 * @return
	 */
	private static boolean tableExists(String tableName){
		try {
			String statement="show tables like "+'"'+tableName+'"';
			ResultSet resultSet;
			resultSet = MySQLAccess.executeQuery(statement);
			if(resultSet.next())return true;
		} catch (SQLException e) {
			return false;
		}
		return false;
	}
	/**
	 * Tries to insert the entry in the data base if there is a connection. 
	 * If there is no connection, buffers the entry for later insertion.
	 * @param entry 
	 * 					The reflective call, that is going to be inserted in the database.
	 * @throws NumberFormatException
	 * @throws SQLException
	 */
	public static void insert(String entry) throws NumberFormatException, SQLException{
		if(!connected){
			buffer.append(entry+"\n");
		}else
		{
			String[] entryX=entry.split(";");
			insertInDB(entryX[1],entryX[0],entryX[2],Integer.valueOf(entryX[3]),"thread","className",-1);
		}
	}
	
	/**
	 * Inserts the buffered entries in the database.
	 * @throws NumberFormatException
	 * @throws SQLException
	 */
	public static void sendBuffer() throws NumberFormatException, SQLException{
		if(buffer.length()>1){
		String[] entries =buffer.toString().split("\n");
		for(String i:entries){
			String[] entryX=i.split(";");
			insertInDB(entryX[1],entryX[0],entryX[2],Integer.valueOf(entryX[3]),"thread","className",-1);
		}
		}
	}
	
	/**
	 * Inserts data in the database
	 * @param target	target of the reflective call
	 * @param type		type of the reflective call
	 * @param method	the method, from which the call was made
	 * @param line		the line number, from which the call was made 
	 * @param thread	the thread, from which the call was made
	 * @param classname	the name of the class, from which the call was made
	 * @param version	the hash code of the class
	 * @throws SQLException
	 */
	public static void insertInDB(String target,String type,String method,int line,String thread,String classname,long version)throws SQLException{
		ClassID classID=new ClassID(classname,version);

		String statement=classID.generateIDSearchStatement();
		int i=handleResultSet(MySQLAccess.executeQuery(statement));
		if(i==-1){
			String newstatement=classID.generateInsertStatement();
			MySQLAccess.executeUpdate(newstatement);
			i=handleResultSet(MySQLAccess.executeQuery(statement));
		}
		classID.setID(i);
	
		CallType callType=new CallType(type);
		statement=callType.generateIDSearchStatement();
		i=handleResultSet(MySQLAccess.executeQuery(statement));
		if(i==-1){
			String newstatement=callType.generateInsertStatement();
			MySQLAccess.executeUpdate(newstatement);
			i=handleResultSet(MySQLAccess.executeQuery(statement));
		}
		callType.setID(i);
		
		Location location=new Location(method,line,callType.getID(),classID.getID());
		statement=location.generateIDSearchStatement();
		i=handleResultSet(MySQLAccess.executeQuery(statement));
		if(i==-1){
			String newstatement=location.generateInsertStatement();
			MySQLAccess.executeUpdate(newstatement);
			i=handleResultSet(MySQLAccess.executeQuery(statement));
		}
		location.setID(i);
		
		Call call=new Call(target,thread,location.getID());
		String newstatement=call.generateInsertStatement();
		MySQLAccess.executeUpdate(newstatement);
		statement=call.generateIDSearchStatement();
		i=handleResultSet(MySQLAccess.executeQuery(statement));
		call.setID(i);

		RunToCall runToCall=new RunToCall(runID,call.getID());
		statement=runToCall.generateInsertStatement();
		MySQLAccess.executeUpdate(statement);
	}
	
	/**
	 * Gets the id from the result set. 
	 * @param resultSet 
	 * @return	id
	 * @throws SQLException
	 */
	public static int handleResultSet(ResultSet resultSet) throws SQLException {
			if(resultSet.next())
				return resultSet.getInt(1);
			else return -1;	
	}
	
	/**
	 * Get the location from a class file.
	 * @param className
	 * 					the name of the class
	 * @param version
	 * 					hashcode of the class file
	 * @return list of locations
	 * @throws SQLException
	 */
	public static Vector<Location> findLocations(String className,long version) throws SQLException{
		Vector<Location> locations=new Vector<Location>();
		if(connected){
			ClassID classID=new ClassID(className,version);
			String statement=classID.generateIDSearchStatement();
			int i=handleResultSet(MySQLAccess.executeQuery(statement));
			if(i==-1){return locations;}
			classID.setID(i);
			
			SearchStatement st=new SearchStatement("locations", "idlocations,classid,method,line,calltypeid");
			st.addClauseEqual("classid", classID.getID());
			statement=st.generateStatement();
			ResultSet resultSet=MySQLAccess.executeQuery(statement);
	
			while(resultSet.next()){
				Location location=new Location(resultSet);
				locations.add(location);
			}
		}
		return locations;
	}

	/**
	 * Finds all reflective calls from a specific location.
	 * @param locationID the unique id in the database of the location.
	 * @return Vector containing all the reflective calls from the location.
	 * @throws SQLException
	 */
	public static Vector<Call> findCalls(int locationID) throws SQLException{
		Vector<Call> list=new Vector<Call>();
		if(connected){
			SearchStatement st=new SearchStatement("calls", "idcalls,locationid,target,thread");
			st.addClauseEqual("locationid", locationID);
			String statement=st.generateStatement();
			ResultSet resultSet=MySQLAccess.executeQuery(statement);
			while(resultSet.next()){
				Call call=new Call(resultSet);
				list.add(call);
			}
		}
		return list;
	}
	
	/**
	 * Finds All Reflective Calls form the last run
	 * @param locationID	the unique id in the database of the location.
	 * @return	Vector containing the reflective calls from the location from the last run.
	 * @throws SQLException
	 */
	public static Vector<Call> findLastRunCalls(int locationID) throws SQLException{
		Vector<Call> list= new Vector<Call>();
		if(connected){
			String statement="select idruns from runs where time=(select max(time) from runs)";
			int i=handleResultSet(MySQLAccess.executeQuery(statement));
			statement="select distinct idcalls,locationid,target,thread from calls,runs,runtocall where runtocall.runid=" +i+
					" and runtocall.callid=calls.idcalls and calls.locationid="+locationID;
			ResultSet resultSet=MySQLAccess.executeQuery(statement);
			while(resultSet.next()){
				Call call=new Call(resultSet);
				list.add(call);
			}
		}
		return list;
	}

	/**
	 * Searches for locations, from which the call was made.
	 * @param callTarget
	 * @return
	 * 			Vector with the locations.
	 * @throws SQLException
	 */
	public static Vector<Location> findLocationsFromCall(String callTarget) throws SQLException{
		Vector<Location> list=new Vector<Location>();
		if(connected){
			String statement="select distinct idlocations,classid,method,line,calltypeid "+
				"from locations,calls where locations.idlocations=calls.locationid and "+
				"calls.target like "+'"'+callTarget+'"';
			ResultSet resultSet=MySQLAccess.executeQuery(statement);
			while(resultSet.next()){
				Location location=new Location(resultSet);
				list.add(location);
			}
		}
		return list;
	}
	
	/**
	 * Gets the entry in the table ClassID with the specified idclassid.
	 * @param idclassid id of the class
	 * @return
	 * @throws SQLException
	 */
	public static ClassID getClassID(int idclassid) throws SQLException{
		String statement="select * from classid where idclassid="+idclassid;
		if(connected){
			ResultSet resultSet=MySQLAccess.executeQuery(statement);
			if(resultSet.next())
				return new ClassID(resultSet);
			else return null;
		} else return null;
	}
}
