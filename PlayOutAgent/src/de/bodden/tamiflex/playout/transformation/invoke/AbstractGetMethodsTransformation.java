/*******************************************************************************
 * Copyright (c) 2026 Gauravsingh Sisodia.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gauravsingh Sisodia - initial implementation
 ******************************************************************************/
package de.bodden.tamiflex.playout.transformation.invoke;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.transformation.AbstractTransformation;
import de.bodden.tamiflex.playout.Agent;


public abstract class AbstractGetMethodsTransformation extends AbstractTransformation {
	
	public AbstractGetMethodsTransformation(Method... methods) throws Exception {
		super(Class.forName("java.lang.Class"), methods);
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) 
    {
        return new MethodVisitor(ASM9, parent) {

            @Override
            public void visitInsn(int opcode) {
                if (opcode == ARETURN) {
                    super.visitMethodInsn(INVOKESTATIC,
                        "de/bodden/tamiflex/playout/rt/ReflLogger",
                        methodName(),
                        methodSignature(),
                        false);
                }
                super.visitInsn(opcode);
            };
		};
	}
	protected abstract String methodName();

	protected abstract String methodSignature();
}
