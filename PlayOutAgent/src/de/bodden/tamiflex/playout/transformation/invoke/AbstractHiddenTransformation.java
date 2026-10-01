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

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.ILOAD;

import java.lang.invoke.LambdaMetafactory;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.playout.transformation.AbstractTransformation;
import de.bodden.tamiflex.playout.Agent;


// Modify InnerClassLambdaMetafactory to dump the class bytes to disk after it is generated
public abstract class AbstractHiddenTransformation extends AbstractTransformation {
	
	public AbstractHiddenTransformation(Method... methods) throws Exception {
		super(Class.forName("java.lang.invoke.MethodHandles$Lookup"), methods);
	}
	
	@Override
	protected MethodVisitor getMethodVisitor(MethodVisitor parent) 
    {
        return new MethodVisitor(ASM9, parent) {

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (opcode == INVOKESPECIAL &&
                    name.equals("<init>") &&
                    owner.equals("java/lang/invoke/MethodHandles$Lookup$ClassDefiner")) {
                    // Current Stack: [..., ClassFile, int (flags), ClassFileDumper]

                    super.visitInsn(POP);
                    super.visitInsn(POP);
                    // Current Stack: [..., ClassFile]

                    super.visitInsn(DUP);
                    // Stack: [ClassFile, ClassFile]

                    super.visitFieldInsn(GETFIELD, "java/lang/invoke/MethodHandles$Lookup$ClassFile", "name", "Ljava/lang/String;");
                    // Stack: [ClassFile, String (name)]

                    super.visitInsn(SWAP);
                    // Stack: [String (name), ClassFile]

                    super.visitFieldInsn(GETFIELD, "java/lang/invoke/MethodHandles$Lookup$ClassFile", "bytes", "[B");
                    // Stack: [String (name), byte[] (oldBytes)]

                    super.visitVarInsn(ALOAD, 0);
                    // Stack: [String (name), byte[] (oldBytes), this]

                    super.visitFieldInsn(GETFIELD, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "Ljava/lang/Class;");
                    // Stack: [String (name), byte[] (oldBytes), this.lookupClass]

                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                    // Stack: [String (name), byte[] (oldBytes), ClassLoader]

                    super.visitMethodInsn(INVOKESTATIC,
                        "de/bodden/tamiflex/playout/rt/ReflLogger",
                        methodName(),
                        methodSignature(),
                        false);
                    // Stack: [String (name), byte[] (newBytes)]

                    super.visitMethodInsn(INVOKESTATIC,
                        "java/lang/invoke/MethodHandles$Lookup$ClassFile",
                        "newInstanceNoCheck",
                        "(Ljava/lang/String;[B)Ljava/lang/invoke/MethodHandles$Lookup$ClassFile;",
                        false);
                    // Stack: [..., NewClassFile]

                    super.visitVarInsn(ILOAD, 5); // Reload flags
                    super.visitVarInsn(ALOAD, 4); // Reload dumper

                    // Stack: [..., NewClassFile, int (flags), ClassFileDumper]
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            };
		};
	}
	protected abstract String methodName();

	protected abstract String methodSignature();
}
