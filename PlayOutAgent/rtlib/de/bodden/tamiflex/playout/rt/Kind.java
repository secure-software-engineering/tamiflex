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
