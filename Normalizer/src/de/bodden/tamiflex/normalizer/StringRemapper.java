package de.bodden.tamiflex.normalizer;

import org.objectweb.asm.commons.Remapper;

/**
 * A {@link Remapper} that not only re-maps type names but also string
 * constants.
 */
public class StringRemapper extends Remapper {
	
	public String remapStringConstant(String constant) {
		//by default, don't re-map anything
		return constant;
	}

}
