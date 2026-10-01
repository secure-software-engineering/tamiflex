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
package de.bodden.tamiflex.playout.transformation.constructor;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import static org.objectweb.asm.Opcodes.ASM9;

import java.lang.reflect.Constructor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.AbstractTransformation;

public abstract class AbstractConstructorTransformation extends AbstractTransformation {
	
	public AbstractConstructorTransformation(Method... methods) {
		super(Constructor.class, methods);
	}

    @Override
    protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
        return new MethodVisitor(ASM9, parent) {
            
            @Override
            public void visitInsn(int opcode) {
                if (IRETURN <= opcode && opcode <= RETURN) {
                    // All 4 reflection calls under consideration for Constructor.class are Non-Static
                    // Therefore the local variable indexed by 0 is the `this` pointer which is of type "Constructor<?>"
					super.visitVarInsn(ALOAD, 0); // Load Constructor instance
					super.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", methodKind().name(), Type.getDescriptor(Kind.class));
					super.visitMethodInsn(
						INVOKESTATIC,
						"de/bodden/tamiflex/playout/rt/ReflLogger",
						"constructorMethodInvoke",
						"(Ljava/lang/reflect/Constructor;Lde/bodden/tamiflex/playout/rt/Kind;)V",
                        false
                    );
				}
				super.visitInsn(opcode);
            }
        };
    }
	
	protected abstract Kind methodKind();
}
