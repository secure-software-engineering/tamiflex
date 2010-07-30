package de.bodden.tamiflex.db.datamodel;

/**
 * Entry in the table RunToCall
 * @author Ivaylo Petkov and Oleg Manov
 */
public class RunToCall extends DBEntry{
	/**
	 * The id of the run.
	 */
	private int runID;
	/**
	 * The id of the call/
	 */
	private int callID;
	
	/**
	 * The Constructor
	 * @param runID		the id of the run
	 * @param callID	the id of the call
	 */
	public RunToCall(int runID,int callID) {
		this.runID=runID;
		this.callID=callID;
		this.setTableName("RunToCall");
		this.setIdFieldName("idRunToCall");
		this.fields="idruntocall,runid,callid";
	}

	public String generateInsertStatement() {
		String statement="insert into ";
		statement+=getTableName();
		statement+=" (runid,callid) values(";
		statement+=runID;
		statement+=",";
		statement+=callID;
		statement+=")";
		return statement;
	}

	public String generateIDSearchStatement() {
		String statement="select ";
		statement+=getIdFieldName();
		statement+=" from ";
		statement+=getTableName();
		statement+=" where ";
		statement+="runid="+runID;
		statement+=" and ";
		statement+="callid='"+callID;
		return statement;
	}
}
