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

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.RecursionAvoidingMethodAdapter;
import de.bodden.tamiflex.playout.transformation.Transformation;

public abstract class ClassGetXFieldTransformation extends Transformation {
	
	public ClassGetXFieldTransformation(Method... methods) {
		super(Class.class, methods);
	}

	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
		return new RecursionAvoidingMethodAdapter(parent) {
			
			@Override
			public void visitInsn(int opcode) {
				if (IRETURN <= opcode && opcode <= RETURN) {
					mv.visitInsn(Opcodes.DUP); 	//duplicate return value (the Field instance)
					mv.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", methodKind().name(), Type.getDescriptor(Kind.class));
					mv.visitMethodInsn(
						INVOKESTATIC,
						"de/bodden/tamiflex/playout/rt/ReflLogger",
						"fieldMethodInvoke",
						"(Ljava/lang/reflect/Field;Lde/bodden/tamiflex/playout/rt/Kind;)V"
					);
				}
				super.visitInsn(opcode);
			}

		};
	}

	protected abstract Kind methodKind();
}
