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

import java.util.Arrays;

public class TargetMethodLogEntry extends RuntimeLogEntry {

	protected final String declaringClass;
	protected final String returnType;
	protected final String name;
	protected final String[] paramTypes;

	public TargetMethodLogEntry(String containerMethod, int lineNumber, Kind kind, String declaringClass, String returnType, String name, String... paramTypes) {
		super(containerMethod, lineNumber, kind);
		this.declaringClass = declaringClass;
		this.returnType = returnType;
		this.name = name;
		this.paramTypes = paramTypes;
	}

	public PersistedLogEntry toPersistedEntry() {
		String sootSignature = sootSignature(declaringClass, returnType, name, paramTypes);
		return new PersistedLogEntry(containerMethod, lineNumber, kind, sootSignature, count);
	}
	
	private static String sootSignature(String declaringClass, String returnType, String name, String... paramTypes) {
		StringBuilder b = new StringBuilder();
		b.append("<");
		b.append(declaringClass);
		b.append(": ");
		b.append(returnType);
		b.append(" ");
		b.append(name);
		b.append("(");
		int i = 0;
		for (String type : paramTypes) {
			i++;
			b.append(type);
			if(i<paramTypes.length) {
				b.append(",");
			}
		}
		b.append(")>");
		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(paramTypes);
		result = prime * result
				+ ((returnType == null) ? 0 : returnType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TargetMethodLogEntry other = (TargetMethodLogEntry) obj;
		if (declaringClass == null) {
			if (other.declaringClass != null)
				return false;
		} else if (!declaringClass.equals(other.declaringClass))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(paramTypes, other.paramTypes))
			return false;
		if (returnType == null) {
			if (other.returnType != null)
				return false;
		} else if (!returnType.equals(other.returnType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String targetMethod = sootSignature(declaringClass, returnType, name, paramTypes);
		return kind.label() + ";" + targetMethod + ";" + containerMethod + ";" + (lineNumber>-1?lineNumber:"") + ";" + (count>0?count:"");
	}
	
}
