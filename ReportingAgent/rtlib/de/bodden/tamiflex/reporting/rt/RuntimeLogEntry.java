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
package de.bodden.tamiflex.reporting.rt;

public abstract class RuntimeLogEntry {
	
	protected final String containerMethod;
	
	protected final int lineNumber;

	protected final Kind kind;
	
	protected int count;

	public RuntimeLogEntry(String containerMethod, int lineNumber, Kind kind) {
		if(lineNumber<0) lineNumber = -1;
		this.containerMethod = containerMethod;
		this.lineNumber = lineNumber;
		this.kind = kind;
		this.count = 0;
	}
	
	@Override
	//does NOT take into account "count"
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((containerMethod == null) ? 0 : containerMethod.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + lineNumber;
		return result;
	}

	@Override
	//does NOT take into account "count"
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuntimeLogEntry other = (RuntimeLogEntry) obj;
		if (containerMethod == null) {
			if (other.containerMethod != null)
				return false;
		} else if (!containerMethod.equals(other.containerMethod))
			return false;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (lineNumber != other.lineNumber)
			return false;
		return true;
	}

	public int getCount() {
		return count;
	}

	public String getContainerMethod() {
		return containerMethod;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public Kind getKind() {
		return kind;
	}

	public void incrementCounter() {
		count++;
	}
	
	public abstract PersistedLogEntry toPersistedEntry();
}
