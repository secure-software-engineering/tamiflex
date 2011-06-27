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
package de.bodden.tamiflex.playout.transformation;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class RecursionAvoidingMethodAdapter extends MethodAdapter {

	public RecursionAvoidingMethodAdapter(MethodVisitor mv) {
		super(mv);
	}
	
	@Override
	public void visitCode() {
		mv.visitMethodInsn(INVOKESTATIC,
				"de/bodden/tamiflex/playout/rt/ReflLogger",
				"enteringReflectionAPI",
				"()V");
		super.visitCode();
	}

}
