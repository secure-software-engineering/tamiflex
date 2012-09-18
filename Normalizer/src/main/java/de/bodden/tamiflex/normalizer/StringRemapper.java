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
