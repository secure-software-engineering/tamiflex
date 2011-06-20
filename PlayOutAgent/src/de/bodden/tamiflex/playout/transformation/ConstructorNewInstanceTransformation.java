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

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Constructor;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

public class ConstructorNewInstanceTransformation extends Transformation {
	
	public ConstructorNewInstanceTransformation() {
		super(Constructor.class, new Method("newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;"));
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
		return new MethodAdapter(parent) {
			
			@Override
			public void visitInsn(int opcode) {
				if (opcode == ARETURN) {
					mv.visitVarInsn(ALOAD, 0); // Load Constructor instance
					mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "constructorNewInstance", "(Ljava/lang/reflect/Constructor;)V");
				}
				super.visitInsn(opcode);
			}
		};
	}
}
