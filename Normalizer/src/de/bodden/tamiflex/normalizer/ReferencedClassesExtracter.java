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

import static de.bodden.tamiflex.normalizer.Hasher.containsGeneratedClassName;
import static de.bodden.tamiflex.normalizer.Hasher.slashed;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import de.bodden.tamiflex.normalizer.RemappingStringConstantAdapter;
import de.bodden.tamiflex.normalizer.StringRemapper;

public final class ReferencedClassesExtracter extends
		RemappingClassAdapter {
	private final Set<String> res;

	public ReferencedClassesExtracter(ClassVisitor cv, final Set<String> res) {
		super(cv, new Remapper() {
    		@Override
    		public String map(String typeName) {
    			if(containsGeneratedClassName(typeName))
    				res.add(typeName);
   				return super.map(typeName);
    		}
    	});
		this.res = res;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name,
			String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		mv = new RemappingStringConstantAdapter(mv, new StringRemapper() {
			@Override
			public String remapStringConstant(String constant) {
				String slashed = slashed(constant);
				if(containsGeneratedClassName(slashed))
					res.add(slashed);
				return super.remapStringConstant(constant);
			}
		});
		return mv;
	}

	public String getClassName () {
		return className;
	}
}
