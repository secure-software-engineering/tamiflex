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
	ConstructorNewInstance("Constructor.newInstance"),
	MethodInvoke("Method.invoke");

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
		throw new RuntimeException("unknown kind");
	}
	
	@Override
	public String toString() {
		return label();
	}
}
