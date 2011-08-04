/*******************************************************************************
 * Copyright (c) 2011 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package de.bodden.tamiflex.views;

public class ArrayNode extends ClassNode {

	protected String fullName;
	
	public ArrayNode(String name) {
		super(elementType(name));
		this.fullName = name;
	}
	
	private static String elementType(String name) {
		while(name.endsWith("[]")) name = name.substring(0, name.length()-2);				
		return name;
	}

	@Override
	public String getName() {
		return fullName;
	}
}
