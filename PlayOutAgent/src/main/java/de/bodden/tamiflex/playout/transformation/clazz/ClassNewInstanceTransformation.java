/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial implementation
 ******************************************************************************/
package de.bodden.tamiflex.playout.transformation.clazz;

import static de.bodden.tamiflex.playout.rt.Kind.ClassNewInstance;

import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;

public class ClassNewInstanceTransformation extends AbstractClassTransformation {
	
	public ClassNewInstanceTransformation() {
		super(new Method("newInstance", "()Ljava/lang/Object;"));
	}

	@Override
	protected Kind methodKind() {
		return ClassNewInstance;
	}
	
}
