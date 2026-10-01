/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package de.bodden.tamiflex.normalizer;

import static de.bodden.tamiflex.normalizer.Hasher.dotted;
import static de.bodden.tamiflex.normalizer.Hasher.slashed;

import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.ClassRemapper;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * Provides functionality to 
 * 1. Rename references to generated classes
 * 2. Including fully-qualified type names in String constants. (See {@link StringRemapper} as to why)
 */
public class ClassRenamer {
	
	/**
	 * Exception that is thrown if the "fromTo" map contains no entry for the generated class
	 * with name <code>typeName</code>.
	 */
	@SuppressWarnings("serial")
	public static class NoHashedNameException extends RuntimeException{
		public NoHashedNameException(String typeName) {
			super(typeName);
		}
	}

	/**
	 * Renames references to generated classes according to the mapping <code>fromTo</code>
	 * in the bytecode <code>classBytes</code>.
	 * @param fromTo This map must contain, for every generated class c an entry that maps c to
	 * 		some other valid class name. Generated classes are such classes whose name is matched by
	 * 		{@link Hasher#isGeneratedClass(String)}.
	 * @param classBytes The bytecode in which the renaming should take place. This array wil remain
	 * 		unmodified.
	 * @return The bytecode containing the renamed references.
	 */
	public static byte[] replaceClassNamesInBytes(final Map<String, String> fromTo,	byte[] classBytes) {
		ClassReader creader = new ClassReader(classBytes);

    	ClassWriter writer = new ClassWriter(0);

        ClassRemapper visitor = new ClassRemapper(ASM9, writer, 
            new Remapper(ASM9) {
                // Rename type references to generated classes
                @Override
                public String map(String typeName) {
                    String newName = fromTo.get(typeName);

                    if (Hasher.isGeneratedClass(typeName) && newName == null) {
                        // This should not happen as POA/ClassDumper calls generateHashNumber() before calling replaceGeneratedClassNamesByHashedNames()
                        throw new NoHashedNameException(typeName);
                    }

                    if (newName != null) typeName = newName;
                    return super.map(typeName);
                }
            }
        ) {
            // Rename fully-qualified type names in String constants using RemappingStringConstantVisitor
            @Override
            public MethodVisitor visitMethod(int access, String name, 
                    String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new RemappingStringConstantVisitor(mv, 
                    new StringRemapper() {
                        @Override
                        public String remapStringConstant(String constant) {
                            // String constants used for reflection are in dotted format, need to convert to internal name format
                            // For ex: Class<?> clazz = Class.forName("path.to.the.classfile");
                            // Have to convert "path.to.the.classfile" to "path/to/the/classfile"
                            String slashed = slashed(constant);
                            String to = fromTo.get(slashed);

                            if (Hasher.isGeneratedClass(slashed) && to == null) {
                                // Should not happen as replaceClassNamesInBytes will be called in the order in which classes are loaded
                                // See POA/ClassDumper
                                throw new NoHashedNameException(slashed);
                            }

                            if (to != null) constant = dotted(to);
                            return super.remapStringConstant(constant);
                        }
                    }
                );
                return mv;
            }
        };

    	creader.accept(visitor, 0);
        return writer.toByteArray();
	}
}
