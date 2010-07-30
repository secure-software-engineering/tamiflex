package de.bodden.tamiflex.db.datamodel;

/**
 * Entry in the table CallTypes.
 * @author Ivaylo Petkov and Oleg Manov
 */
public class CallType extends DBEntry{
	/**
	 * Reflective call type.
	 */
	private String type;
	
	/**
	 * The constructor
	 * @param type
	 * 			Reflective call type.
	 */
	public CallType(String type) {
		this.type = type;
		this.setTableName("CallTypes");
		this.setIdFieldName("idCallTypes");
		this.fields="idcalltypes,type";
	}

	public String generateInsertStatement() {
		String statement="insert into ";
		statement+=getTableName();
		statement+=" (type) values(";
		statement+="'"+type+"'";
		statement+=")";
		return statement;
	}
	
	public String generateIDSearchStatement() {
		String statement="select ";
		statement+=getIdFieldName();
		statement+=" from ";
		statement+=getTableName();
		statement+=" where ";
		statement+="type='"+type+"'";
		return statement;
	}
}
