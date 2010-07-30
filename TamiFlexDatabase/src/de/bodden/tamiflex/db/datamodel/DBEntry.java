package de.bodden.tamiflex.db.datamodel;

/**
 * Abstract entry in the database.
 * @author Ivaylo Petkov and Oleg Manov
 */
public abstract class DBEntry {
	
	/**
	 * The fields in the table containing the entry.
	 */
	protected String fields;
	

	/**
	 * Name of the field for the id.
	 */
	private String idFieldName;

	/**
	 * The name of table,in which the entry is.
	 */
	private String tableName;
	
	/**
	 * The unique id of the entry.
	 */
	private int id;
	
	/**
	 * @return The unique id of the entry
	 */
	public int getID(){
		return id;
	}
	
	/**
	 * @param id
	 * 		 	 The unique id of the entry
	 */
	public void setID(int id){
		this.id = id;
	}

	/**
	 * @return Generated statement for the search method.
	 */
	public abstract String generateIDSearchStatement();

	/**
	 * @return Generated statement for the insert method.
	 */
	public abstract String generateInsertStatement();

	/**
	 * @param tableName
	 * 					The name of the table storing the entry.
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	/**
	 * @return the name of the table storing the entry.
	 */
	public String getTableName() {
		return tableName;
	}
	
	/**
	 * @param idFieldName 
	 * 						The name of the id field.
	 */
	public void setIdFieldName(String idFieldName) {
		this.idFieldName = idFieldName;
	}
	
	/**
	 * @return the name of the id field
	 */
	public String getIdFieldName() {
		return idFieldName;
	}

}
