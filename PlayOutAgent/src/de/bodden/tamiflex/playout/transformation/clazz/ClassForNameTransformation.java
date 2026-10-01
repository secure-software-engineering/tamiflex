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
package de.bodden.tamiflex.playout.transformation.clazz;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import static org.objectweb.asm.Opcodes.ASM9;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.transformation.AbstractTransformation;


public class ClassForNameTransformation extends AbstractTransformation {
	
	public ClassForNameTransformation() {
		super(Class.class,
				new Method("forName", "(Ljava/lang/String;)Ljava/lang/Class;"),
				new Method("forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"));
	}
	
    @Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
        return new MethodVisitor(ASM9, parent) {
            
            @Override
            public void visitInsn(int opcode) {
                if (opcode == ARETURN) {
                    // Since Class.forName() is a static method the local variable indexed by 0 returns the 1st parameter to this method call
                    // This is used to store the TARGET(of the refln) CLASS NAME for logging purpose
					super.visitVarInsn(ALOAD, 0); // Of type java.lang.String
					super.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "classForName", "(Ljava/lang/String;)V", false);
				}
				super.visitInsn(opcode);
            }
        };
	}
}
