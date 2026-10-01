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
package de.bodden.tamiflex.playin;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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

public class HiddenClassTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader ldr, String className, Class<?> classBeingRedefined, 
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) 
                            throws IllegalClassFormatException {
        try {
            if (className == null) {
                return null;
            }

            // Condition for Hidden Classes
            if (className.startsWith("java/lang/invoke/MethodHandles$Lookup")) {
                ClassReader cr = new ClassReader(classfileBuffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                ClassVisitor cv = new HiddenClassModificationVisitor(ASM9, cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                return cw.toByteArray();
            } 

            // Condition for Class.getMethods instrumentation
            if (className.equals("java/lang/Class")) {
                ClassReader cr = new ClassReader(classfileBuffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                ClassVisitor cv = new ClassModificationVisitor(ASM9, cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                return cw.toByteArray();
            }

            return null; // Return null means "no changes, use original"

        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Hidden Class Instrumentation
    static class HiddenClassModificationVisitor extends ClassVisitor {

        public HiddenClassModificationVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            if ("makeHiddenClassDefiner".equals(name)) {
                return new HiddenClassMethodAdapter(api, mv);
            }
            return mv;
        }
    }

    static class HiddenClassMethodAdapter extends MethodVisitor {
        public HiddenClassMethodAdapter(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == INVOKESPECIAL &&
                name.equals("<init>") &&
                owner.equals("java/lang/invoke/MethodHandles$Lookup$ClassDefiner")) {
                // Stack: [..., ClassFile, int (flags), ClassFileDumper]

                super.visitInsn(POP);
                super.visitInsn(POP);
                // Stack: [..., ClassFile]

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
                    "de/bodden/tamiflex/playin/rt/HiddenClassLoader",
                    "loadHiddenClass",
                    "([BLjava/lang/ClassLoader;)[B",
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
        }
    }

    // Class.getMethods Instrumentation
    static class ClassModificationVisitor extends ClassVisitor {

        public ClassModificationVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            if ("getMethods".equals(name) && "()[Ljava/lang/reflect/Method;".equals(descriptor)) {
                return new ClassGetMethodAdapter(api, mv);
            }

            return mv;
        }
    }

    static class ClassGetMethodAdapter extends MethodVisitor {
        public ClassGetMethodAdapter(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == ARETURN) {
                super.visitMethodInsn(
                    INVOKESTATIC,
                    "de/bodden/tamiflex/playin/rt/HiddenClassLoader",
                    "sortMethods",
                    "([Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;",
                    false
                );
            }
            super.visitInsn(opcode);
        }
    }
}
