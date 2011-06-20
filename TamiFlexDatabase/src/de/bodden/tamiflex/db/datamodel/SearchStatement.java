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

/**
 * A search statement for the database
 * @author Ivaylo Petkov and Oleg Manov
 */
public class SearchStatement {
	/**
	 * The name of the table
	 */
	private String tableName;
	/**
	 * The "where" statement of the search statement
	 */
	private String whereStatement="";
	/**
	 * The result fields of the search statement 
	 */
	private String fields;
	
	/**
	 * The Constructor
	 * @param tableName
	 * 					the name of the table.	
	 * @param fields
	 * 					the result fields.
	 */
	public SearchStatement(String tableName,String fields){
		this.tableName=tableName;
		this.fields=fields;
	}
	
	/**
	 * Adds an equal clause to the search statement. 
	 * @param fieldName
	 * 					the name of the field
	 * @param value
	 * 					the fielda value
	 */
	public void addClauseEqual(String fieldName,String value)	{
		String sValue="'"+value+"'";
		addWhereEqual(fieldName, sValue);
	}
	
	/**
	 * Adds an equal clause to the search statement. 
	 * @param fieldName
	 * 					the name of the field
	 * @param value
	 * 					the fielda value
	 */
	public void addClauseEqual(String fieldName,int value)	{
		String sValue=""+value;
		addWhereEqual(fieldName, sValue);
	}
	
	/**
	 * Adds an equal clause to the search statement.
	 * @param fieldName 
	 * 					the name of the field
	 * @param value
	 * 				the field value
	 */
	private void addWhereEqual(String fieldName,String value)	{
		if(whereStatement.isEmpty())
		{
			whereStatement=" where "+fieldName+"="+value;
		}else
		{
			whereStatement+=" and "+fieldName+"="+value;
		}
	}
	
	/**
	 * @return the final search statement
	 */
	public String generateStatement()	{
		String statement="select ";
		statement+=fields;
		statement+=" from ";
		statement+=tableName;
		statement+=whereStatement;
		return statement;
	}
}
