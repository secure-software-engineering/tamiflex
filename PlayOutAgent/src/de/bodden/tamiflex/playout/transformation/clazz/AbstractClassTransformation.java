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
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import static org.objectweb.asm.Opcodes.ASM9;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.AbstractTransformation;

public abstract class AbstractClassTransformation extends AbstractTransformation {
	
	public AbstractClassTransformation(Method... methods) {
		super(Class.class, methods);
	}
	
    @Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
        return new MethodVisitor(ASM9, parent) {
            
            @Override
            public void visitInsn(int opcode) {
                // We don't consider opcode == ATHROW coz we only want to log successful calls
                // See Technical report: pg7 1st column last paragraph
				if (IRETURN <= opcode && opcode <= RETURN) {
					// The first index into the local variable array gives "this" pointer only for Non-Static method
                    // All except Class.forName(...) are Non-Static methods
                    // "this" pointer clearly refers to an object of the type Java.lang.Class (under the Non-Static assumption)
                    // Because this is a Non-static method, this Class object must be ge generated from Obj.getClass() or Class.forName() method
                    // "this" pointer is stored in a class <?> c parameter to get the TARGET(of the refln) CLASS NAME using c.getName() method
                    super.visitVarInsn(ALOAD, 0);
					super.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", methodKind().name(), Type.getDescriptor(Kind.class));
					super.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "classMethodInvoke", "(Ljava/lang/Class;Lde/bodden/tamiflex/playout/rt/Kind;)V", false);
				}
				super.visitInsn(opcode);
            }
        };
	}
	
	protected abstract Kind methodKind();
}
