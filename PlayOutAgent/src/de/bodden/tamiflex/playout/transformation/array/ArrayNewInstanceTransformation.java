/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andreas Sewe - initial implementation
 ******************************************************************************/
package de.bodden.tamiflex.playout.transformation.array;

import org.objectweb.asm.commons.Method;

public class ArrayNewInstanceTransformation extends ArrayTransformation {
	
	public ArrayNewInstanceTransformation() {
		super(new Method("newInstance", "(Ljava/lang/Class;I)Ljava/lang/Object;"));
	}
	
	@Override
	protected String methodName() {
		return "arrayNewInstance";
	}

	@Override
	protected String methodSignature() {
		return "(Ljava/lang/Class;I)V";
	}
}
