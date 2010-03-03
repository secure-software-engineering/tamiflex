package de.bodden.tamiflex.playout.rt;

import static de.bodden.tamiflex.normalizer.Hasher.dotted;
import static de.bodden.tamiflex.normalizer.Hasher.slashed;
import de.bodden.tamiflex.normalizer.Hasher;




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
	
	protected static String replaceByHashedClassNameAndMethodName(String classNameAndMethodName) {
		assert classNameAndMethodName.contains("."): "String should have format Class.Name.methodName: "+classNameAndMethodName;
		int divider = classNameAndMethodName.lastIndexOf('.');
		String className = classNameAndMethodName.substring(0,divider);
		String methodName = classNameAndMethodName.substring(divider+1);
		String hashedName = replaceByHashedClassName(className);
		return hashedName + "." + methodName; 
	}

	protected static String replaceByHashedClassName(String className) {
		String slashedClassName = slashed(className);		
		String hashedName = Hasher.containsGeneratedClassName(slashedClassName) ?
			Hasher.hashedClassNameForGeneratedClassName(slashedClassName) : 
			slashedClassName;
		return dotted(hashedName);
	}
	
	public abstract PersistedLogEntry toPersistedEntry();
}
