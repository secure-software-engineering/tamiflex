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

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;

public abstract class ClassTransformation extends Transformation {
	
	public ClassTransformation(Method... methods) {
		super(Class.class, methods);
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
		return new MethodAdapter(parent) {
			
			@Override
			public void visitInsn(int opcode) {
				if (IRETURN <= opcode && opcode <= RETURN) {
					mv.visitVarInsn(ALOAD, 0); // Load Class instance
					mv.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", methodKind().name(), Type.getDescriptor(Kind.class));
					mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "classMethodInvoke", "(Ljava/lang/Class;Lde/bodden/tamiflex/playout/rt/Kind;)V");
				}
				super.visitInsn(opcode);
			}
		};
	}
	
	protected abstract Kind methodKind();
}