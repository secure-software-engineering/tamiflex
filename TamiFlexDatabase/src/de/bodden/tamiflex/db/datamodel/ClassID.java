package de.bodden.tamiflex.db.datamodel;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Entry in the table Locations.
 * @author Ivaylo Petkov and Oleg Manov
 *
 */
public class ClassID extends DBEntry{
	private String name;
	private long version;
	
	/**
	 * The constructor.
	 * @param name
	 * 				the name of the class
	 * @param version
	 * 				the version of the class
	 */
	public ClassID(String name, long version) {
		this.name = name;
		this.version = version;
		this.fields="idclassid,name,version";
		this.setTableName("ClassID");
		this.setIdFieldName("idClassID");
	}
	/**
	 *  The constructor.
	 * @param resultSet
	 * 					resultset from database.
	 * @throws SQLException
	 */
	public ClassID(ResultSet resultSet) throws SQLException
	{
		loadFromResultSet(resultSet);
	}
	
	/**
	 * Loads data from a resultset from database.
	 * @param resultSet
	 * 					resultset from database.
	 * @throws SQLException
	 */
	public void loadFromResultSet(ResultSet resultSet) throws SQLException
	{
		setID(resultSet.getInt(1));
		name=resultSet.getString(2);
		version=resultSet.getLong(3);
	}
	/**
	 * 
	 * @return
	 * 			the name of the classID
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @return
	 * 			the version of the classID
	 */
	public long getVersion() {
		return version;
	}
	
	public String generateInsertStatement() {
		String statement="insert into ";
		statement+=getTableName();
		statement+=" (name,version) values(";
		statement+="'"+name+"'";
		statement+=","+version;
		statement+=")";
		return statement;
	}
	public String generateIDSearchStatement() {
		String statement="select ";
		statement+=getIdFieldName();
		statement+=" from ";
		statement+=getTableName();
		statement+=" where ";
		statement+="name='"+name+"'";
		statement+=" and ";
		statement+="version='"+version+"'";
		return statement;
	}
}