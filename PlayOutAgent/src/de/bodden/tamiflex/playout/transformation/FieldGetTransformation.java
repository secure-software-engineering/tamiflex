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
package de.bodden.tamiflex.playout.transformation;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

public class FieldGetTransformation extends Transformation {
	
	public FieldGetTransformation() {
		super(Field.class,
				new Method("get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
				new Method("getBoolean", "(Ljava/lang/Object;)Z"),
				new Method("getByte", "(Ljava/lang/Object;)B"),
				new Method("getChar", "(Ljava/lang/Object;)C"),
				new Method("getShort", "(Ljava/lang/Object;)S"),
				new Method("getInt", "(Ljava/lang/Object;)I"),
				new Method("getLong", "(Ljava/lang/Object;)J"),
				new Method("getFloat", "(Ljava/lang/Object;)F"),
				new Method("getDouble", "(Ljava/lang/Object;)D"));
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
		return new MethodAdapter(parent) {
			
			@Override
			public void visitInsn(int opcode) {
				if (IRETURN <= opcode && opcode <= ARETURN) {
					mv.visitVarInsn(ALOAD, 0); // Load Field instance
					mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "fieldGet", "(Ljava/lang/reflect/Field;)V");
				}
				super.visitInsn(opcode);
			}
		};
	}
}
