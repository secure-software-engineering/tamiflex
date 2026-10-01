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
/**
 * 
 */
package de.bodden.tamiflex.normalizer;

import static de.bodden.tamiflex.normalizer.Hasher.isGeneratedClass;
import static de.bodden.tamiflex.normalizer.Hasher.slashed;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.ClassRemapper;

import static org.objectweb.asm.Opcodes.ASM9;

import de.bodden.tamiflex.normalizer.RemappingStringConstantVisitor;
import de.bodden.tamiflex.normalizer.StringRemapper;

// Stores a set of all generated class names(slashed '/' format) that appear (as Typenames and String constants) 
// in a classfile via the constructor parameter "res"
public final class ReferencedClassesExtracter extends ClassRemapper {
	private final Set<String> res;

	public ReferencedClassesExtracter(ClassVisitor cv, final Set<String> res) {
		super(ASM9, cv, 
            new Remapper(ASM9) {
                @Override
                public String map(String typeName) {
                    if (isGeneratedClass(typeName))
                        res.add(typeName);
                    return super.map(typeName);
                }
    	    }
        );
		this.res = res;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name,
			String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		mv = new RemappingStringConstantVisitor(mv, 
            new StringRemapper() {
                @Override
                public String remapStringConstant(String constant) {
                    String slashed = slashed(constant);
                    if (isGeneratedClass(slashed))
                        res.add(slashed);
                    return super.remapStringConstant(constant);
                }
		    }
        );
		return mv;
	}

	public String getClassName () {
        // className comes from ClassRemapper
		return className;
	}
}
