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
package de.bodden.tamiflex.playout.transformation.field;

import static de.bodden.tamiflex.playout.rt.Kind.FieldGetDeclaringClass;

import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;

public class FieldGetDeclaringClassTransformation extends AbstractFieldTransformation {
	
	public FieldGetDeclaringClassTransformation() {
		super( new Method("getDeclaringClass", "()Ljava/lang/Class;"));
	}
	
	protected Kind methodKind() {
		return FieldGetDeclaringClass;
	}
}
