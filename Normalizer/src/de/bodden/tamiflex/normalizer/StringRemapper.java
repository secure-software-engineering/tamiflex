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
package de.bodden.tamiflex.normalizer;

import org.objectweb.asm.commons.Remapper;
import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A {@link Remapper} that not only re-maps type names but also string
 * constants. 
 * 
 * Remapping String constants is necessary because reflective calls for 
 * example can be of the following form:
 * Class<?> clazz = Class.forName("path.to.the.classfile");
 * In such a case if the string refers to generated class then it also 
 * needs to be updated for ensuring correctness of the transformation
 */
public class StringRemapper extends Remapper {

    public StringRemapper() {
        super(ASM9);
    }
	
	public String remapStringConstant(String constant) {
		// By default, don't re-map anything
		return constant;
	}

}
