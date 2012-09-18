/*******************************************************************************
 * Copyright (c) 2011 Andreas Sewe.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andreas Sewe
 ******************************************************************************/
package de.bodden.tamiflex.playout.rt;

import java.util.Arrays;

public class TargetArrayLogEntry extends RuntimeLogEntry {

	protected final String componentType;
	protected final int[] dimensions;
	
	//TODO eliminate Kind?
	public TargetArrayLogEntry(String containerMethod, int lineNumber, Kind kind, String componentType, int... dimensions) {
		super(containerMethod, lineNumber, kind);
		this.componentType = componentType;
		this.dimensions = dimensions;
	}

	//TODO pull up into super class?
	public PersistedLogEntry toPersistedEntry() {
		String hashedContainerMethod = replaceByHashedClassNameAndMethodName(containerMethod);
		String hashedComponentType = replaceByHashedClassName(componentType);
			
		String sootSignature = sootSignature(hashedComponentType, dimensions);
		return new PersistedLogEntry(hashedContainerMethod, lineNumber, kind, sootSignature, ""/*no metdata*/, count);
	}
	
	private static String sootSignature(String componentType, int... dimensions) {
		StringBuilder b = new StringBuilder();
		b.append(componentType);
		for (int i = 0; i < dimensions.length; i++) {
			b.append("[]");
		}
		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((componentType == null) ? 0 : componentType.hashCode());
		result = prime * result + Arrays.hashCode(dimensions);
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
		TargetArrayLogEntry other = (TargetArrayLogEntry) obj;
		if (componentType == null) {
			if (other.componentType != null)
				return false;
		} else if (!componentType.equals(other.componentType))
			return false;
		if (!Arrays.equals(dimensions, other.dimensions))
			return false;
		return true;
	}

	@Override //TODO pull up to super class?
	public String toString() {
		String targetArray = sootSignature(componentType, dimensions);
		return kind.label() + ";" + targetArray + ";" + containerMethod + ";" + (lineNumber>-1?lineNumber:"") + ";" + (count>0?count:"");
	}
	
}
