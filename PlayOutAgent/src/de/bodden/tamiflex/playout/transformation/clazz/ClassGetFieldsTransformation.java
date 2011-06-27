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

import static de.bodden.tamiflex.playout.rt.Kind.ClassGetFields;

import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.ClassTransformation;

public class ClassGetFieldsTransformation extends ClassTransformation {
	
	public ClassGetFieldsTransformation() {
		super(new Method("getFields", "()[Ljava/lang/reflect/Field;"));
	}

	@Override
	protected Kind methodKind() {
		return ClassGetFields;
	}

}
