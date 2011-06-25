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




public class PersistedLogEntry {
	
	protected final String containerMethod;
	
	protected final int lineNumber;

	protected final Kind kind;
	
	protected final String targetClassOrMethod;
	
	protected final int count;

	protected final String metadata;

	public PersistedLogEntry(String containerMethod, int lineNumber, Kind kind, String targetClassOrMethod, String metadata, int count) {
		this.metadata = metadata;
		if(lineNumber<0) lineNumber = -1;
		this.containerMethod = containerMethod;
		this.lineNumber = lineNumber;
		this.kind = kind;
		this.targetClassOrMethod = targetClassOrMethod;
		this.count = count;
	}
	
	@Override
	public String toString() {
		return kind.label() + ";" + targetClassOrMethod + ";" + containerMethod + ";" + (lineNumber>-1?lineNumber:"") + ";" + metadata + ";" + (count>0?count:"");
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
		result = prime
				* result
				+ ((targetClassOrMethod == null) ? 0 : targetClassOrMethod
						.hashCode());
		result = prime
				* result
				+ ((metadata == null) ? 0 : metadata
						.hashCode());
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
		PersistedLogEntry other = (PersistedLogEntry) obj;
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
		if (targetClassOrMethod == null) {
			if (other.targetClassOrMethod != null)
				return false;
		} else if (!targetClassOrMethod.equals(other.targetClassOrMethod))
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
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
	
	public String getMetadata() {
		return metadata;
	}

	public String getTargetClassOrMethod() {
		return targetClassOrMethod;
	}

	public static PersistedLogEntry merge(PersistedLogEntry e1, PersistedLogEntry e2) {
		assert e1.containerMethod.equals(e2.containerMethod);
		assert e1.kind.equals(e2.kind);
		assert e1.lineNumber==e2.lineNumber;
		assert e1.targetClassOrMethod.equals(e2.targetClassOrMethod);
		assert e1.metadata.equals(e2.metadata);
		return new PersistedLogEntry(e1.containerMethod, e1.lineNumber, e1.kind, e1.targetClassOrMethod, e1.metadata, e1.count + e2.count);
	}

//	public static LogEntry getEntryWithHashedNames(LogEntry e) {
//		LogEntry res = new LogEntry(
//				dotted(findAndReplaceGeneratedClassNames(slashed(e.containerMethod))),
//				e.lineNumber,
//				e.kind,
//				dotted(findAndReplaceGeneratedClassNames(slashed(e.targetClassOrMethod)))
//			);
//		res.initializeCounter(e.count);
//		return res;
//	}
	
}
