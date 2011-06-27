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
package de.bodden.tamiflex.playout.transformation.clazz;

import static de.bodden.tamiflex.playout.rt.Kind.ClassGetDeclaredMethod;

import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;

public class ClassGetMethodTransformation extends ClassGetXMethodTransformation {
	
	public ClassGetMethodTransformation() {
		super(new Method("getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"));
	}

	@Override
	protected Kind methodKind() {
		return ClassGetDeclaredMethod;
	}


}
