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
package de.bodden.tamiflex.playout.transformation.field;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import static org.objectweb.asm.Opcodes.ASM9;

import java.lang.reflect.Field;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.rt.Kind;
import de.bodden.tamiflex.playout.transformation.AbstractTransformation;

public abstract class AbstractFieldTransformation extends AbstractTransformation {
	
	public AbstractFieldTransformation(Method... affectedMethods) {
		super(Field.class, affectedMethods);
	}

    @Override
    protected MethodVisitor getMethodVisitor(MethodVisitor parent) {
        return new MethodVisitor(ASM9, parent) {
            
            @Override
            public void visitInsn(int opcode) {
                if (IRETURN <= opcode && opcode <= RETURN) {
					super.visitVarInsn(ALOAD, 0); // Load Field instance
					super.visitFieldInsn(GETSTATIC, "de/bodden/tamiflex/playout/rt/Kind", methodKind().name(), Type.getDescriptor(Kind.class));
					super.visitMethodInsn(INVOKESTATIC,
                        "de/bodden/tamiflex/playout/rt/ReflLogger",
                        "fieldMethodInvoke",
                        "(Ljava/lang/reflect/Field;Lde/bodden/tamiflex/playout/rt/Kind;)V",
                        false
                    );
				}
				super.visitInsn(opcode);
            }
        };
    }

	protected abstract Kind methodKind();
}
