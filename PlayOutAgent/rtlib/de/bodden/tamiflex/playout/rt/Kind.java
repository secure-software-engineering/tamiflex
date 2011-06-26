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
package de.bodden.tamiflex.playout.rt;

public enum Kind {
	

	ClassForName("Class.forName"),
	ClassNewInstance("Class.newInstance"),
	ClassGetDeclaredField("Class.getDeclaredField"),
	ClassGetDeclaredFields("Class.getDeclaredFields"),
	ClassGetDeclaredMethod("Class.getDeclaredMethod"),
	ClassGetDeclaredMethods("Class.getDeclaredMethods"),
	ClassGetField("Class.getField"),
	ClassGetFields("Class.getFields"),
	ClassGetMethod("Class.getMethod"),
	ClassGetMethods("Class.getMethods"),
	ClassGetModifiers("Class.getModifiers"),
	ConstructorNewInstance("Constructor.newInstance"),
	ConstructorGetModifiers("Constructor.getModifiers"),
	ConstructorToString("Constructor.toString"),
	MethodInvoke("Method.invoke"),
	MethodGetName("Method.getName"),
	MethodGetDeclaringClass("Method.getDeclaringClass"),
	MethodGetModifiers("Method.getModifiers"),
	MethodToString("Method.toString"),
	ArrayNewInstance("Array.newInstance"),
	FieldSet("Field.set*"),
	FieldGet("Field.get*"),
	FieldGetName("Field.getName"),
	FieldGetDeclaringClass("Field.getDeclaringClass"),
	FieldGetModifiers("Field.getModifiers"),
	FieldToString("Field.toString");
	
	private final String output;

	Kind(String output) {
		this.output = output;		
	}

	public String label() {
		return output;
	}
	
	public static Kind kindForLabel(String label) {
		for(Kind k: Kind.values()) {
			if(k.label().equals(label)) {
				return k;
			}
		}
		throw new RuntimeException("unknown kind: "+label);
	}
	
	@Override
	public String toString() {
		return label();
	}
}
