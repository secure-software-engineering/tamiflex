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

public class TargetClassLogEntry extends RuntimeLogEntry {

	protected final String targetClass;

	public TargetClassLogEntry(String containerMethod, int lineNumber, Kind kind, String targetClass) {
		super(containerMethod, lineNumber, kind);
		this.targetClass = targetClass;
	}

	public PersistedLogEntry toPersistedEntry() {
		return new PersistedLogEntry(containerMethod, lineNumber, kind, targetClass, count);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((targetClass == null) ? 0 : targetClass.hashCode());
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
		TargetClassLogEntry other = (TargetClassLogEntry) obj;
		if (targetClass == null) {
			if (other.targetClass != null)
				return false;
		} else if (!targetClass.equals(other.targetClass))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return kind.label() + ";" + targetClass + ";" + containerMethod + ";" + (lineNumber>-1?lineNumber:"") + ";" + (count>0?count:"");
	}
}
