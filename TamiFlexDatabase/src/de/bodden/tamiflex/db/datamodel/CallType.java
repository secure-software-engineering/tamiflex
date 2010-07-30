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
