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
package de.bodden.tamiflex.playout.transformation.array;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import static org.objectweb.asm.Opcodes.ASM9;

import java.lang.reflect.Array;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.transformation.AbstractTransformation;


public abstract class AbstractArrayTransformation extends AbstractTransformation {
	
	public AbstractArrayTransformation(Method... methods) {
		super(Array.class, methods);
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
        return new MethodVisitor(ASM9, parent) {
            
            @Override
            public void visitInsn(int opcode) {
                if (opcode == ARETURN) {
                    // Only 2 methods are under consideration
                    // 1. Multi-D Array: newInstance(Class<?> componentType, int... dimensions)
                    // 2. Single-D Array: newInstance(Class<?> componentType, int length)
                    // Both the Methods are static, therefore the local variable at index 0 is `componentType` 
                    // and the local variable at index 1 is either an Int or an Array
					super.visitVarInsn(ALOAD, 0);
					super.visitVarInsn(loadDimensionOpcode(), 1); // Load dimension
					super.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", methodName(), methodSignature(), false);
				}
                super.visitInsn(opcode);
            }
        };
	}
	
	protected abstract String methodName();

	protected abstract String methodSignature();
	
	protected abstract int loadDimensionOpcode();

}
