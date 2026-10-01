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
package de.bodden.tamiflex.playout.transformation.method;

import static de.bodden.tamiflex.playout.rt.Kind.MethodInvoke;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import static org.objectweb.asm.Opcodes.ASM9;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;

public class MethodInvokeTransformation extends AbstractMethodTransformation {
	
	public MethodInvokeTransformation() {
		super(new Method("invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"));
	}

    // Have to override as this method has been defined as abstract in AbstractMethodTransformation
	@Override
	protected Kind methodKind() {
		return MethodInvoke;
	}

    @Override
    protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
        return new MethodVisitor(ASM9, parent) {
            
            @Override
            public void visitInsn(int opcode) {
                if (IRETURN <= opcode && opcode <= RETURN) {
					super.visitVarInsn(ALOAD, 1); // Load designated receiver
					super.visitVarInsn(ALOAD, 0); // Load Method instance
					super.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", methodKind().name(), Type.getDescriptor(Kind.class));
					super.visitMethodInsn(
						INVOKESTATIC,
						"de/bodden/tamiflex/playout/rt/ReflLogger",
						"methodMethodInvoke",
						"(Ljava/lang/Object;Ljava/lang/reflect/Method;Lde/bodden/tamiflex/playout/rt/Kind;)V",
                        false
					);
				}
				super.visitInsn(opcode);
            }
        };
    }
}
