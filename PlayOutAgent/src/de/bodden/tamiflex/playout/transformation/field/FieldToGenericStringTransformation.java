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

import static de.bodden.tamiflex.playout.rt.Kind.FieldToGenericString;

import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;

public class FieldToGenericStringTransformation extends AbstractFieldTransformation {
	
	public FieldToGenericStringTransformation() {
		super( new Method("toGenericString", "()Ljava/lang/String;"));
	}
	
	protected Kind methodKind() {
		return FieldToGenericString;
	}
}
