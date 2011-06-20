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

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.lang.reflect.Method;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodInvokeTransformation extends Transformation {
	
	public MethodInvokeTransformation() {
		super(Method.class);
	}
	
	@Override
	protected MethodVisitor getTransformationVisitor(String name, String desc, MethodVisitor parent) {
		if ("invoke".equals(name))
			return new MethodAdapter(parent) {
			
				@Override
				public void visitInsn(int opcode) {
					if (opcode == Opcodes.ARETURN) {
						//load first parameter on the stack, i.e. the designated receiver object
						mv.visitVarInsn(ALOAD, 1);
						// load "this" on stack, i.e. the Method object
						mv.visitVarInsn(ALOAD, 0);
						// call logging method with that Method object as argument
						mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "methodInvoke", "(Ljava/lang/Object;Ljava/lang/reflect/Method;)V");
					}
					super.visitInsn(opcode);
				}
			};
		else
			return parent;
	}
}
