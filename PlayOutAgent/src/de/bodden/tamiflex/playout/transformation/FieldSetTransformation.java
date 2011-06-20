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

public class FieldSetTransformation extends Transformation {
	
	public FieldSetTransformation() {
		super(Field.class,
				new Method("set", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
				new Method("setBoolean", "(Ljava/lang/Object;Z)V"),
				new Method("setByte", "(Ljava/lang/Object;B)V"),
				new Method("setChar", "(Ljava/lang/Object;C)V"),
				new Method("setShort", "(Ljava/lang/Object;S)V"),
				new Method("setInt", "(Ljava/lang/Object;I)V"),
				new Method("setLong", "(Ljava/lang/Object;J)V"),
				new Method("setFloat", "(Ljava/lang/Object;F)V"),
				new Method("setDouble", "(Ljava/lang/Object;D)V"));
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
		return new MethodAdapter(parent) {
			
			@Override
			public void visitInsn(int opcode) {
				if (opcode == RETURN) {
					mv.visitVarInsn(ALOAD, 0); // Load Field instance
					mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "fieldSet", "(Ljava/lang/reflect/Field;)V");
				}
				super.visitInsn(opcode);
			}
		};
	}
}
