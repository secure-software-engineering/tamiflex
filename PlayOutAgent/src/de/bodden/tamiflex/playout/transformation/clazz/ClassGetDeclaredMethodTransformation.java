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

import static de.bodden.tamiflex.playout.rt.Kind.ClassGetDeclaredMethod;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.AbstractTransformation;
import de.bodden.tamiflex.playout.transformation.RecursionAvoidingMethodAdapter;

public class ClassGetDeclaredMethodTransformation extends AbstractTransformation {
	
	public ClassGetDeclaredMethodTransformation() {
		super(Class.class, new Method("getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"));
	}

	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
		return new RecursionAvoidingMethodAdapter(parent) {
			
			@Override
			public void visitInsn(int opcode) {
				if (IRETURN <= opcode && opcode <= RETURN) {
					mv.visitInsn(DUP); 			//duplicate return value (the Method instance)
					mv.visitInsn(ACONST_NULL); 	//no receiver
					mv.visitInsn(Opcodes.SWAP); //null constant must go first
					mv.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", ClassGetDeclaredMethod.name(), Type.getDescriptor(Kind.class));
					mv.visitMethodInsn(
						INVOKESTATIC,
						"de/bodden/tamiflex/playout/rt/ReflLogger",
						"methodMethodInvoke",
						"(Ljava/lang/Object;Ljava/lang/reflect/Method;Lde/bodden/tamiflex/playout/rt/Kind;)V"
					);
				}
				super.visitInsn(opcode);
			}
		};
	}
}
