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

import static de.bodden.tamiflex.playout.rt.Kind.FieldGet;

import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.FieldTransformation;

public class FieldGetTransformation extends FieldTransformation {
	
	public FieldGetTransformation() {
		super(  new Method("get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
				new Method("getBoolean", "(Ljava/lang/Object;)Z"),
				new Method("getByte", "(Ljava/lang/Object;)B"),
				new Method("getChar", "(Ljava/lang/Object;)C"),
				new Method("getShort", "(Ljava/lang/Object;)S"),
				new Method("getInt", "(Ljava/lang/Object;)I"),
				new Method("getLong", "(Ljava/lang/Object;)J"),
				new Method("getFloat", "(Ljava/lang/Object;)F"),
				new Method("getDouble", "(Ljava/lang/Object;)D"));
	}
	
	protected Kind methodKind() {
		return FieldGet;
	}
}
