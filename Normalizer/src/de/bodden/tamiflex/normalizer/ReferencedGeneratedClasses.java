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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import static org.objectweb.asm.Opcodes.ASM9;

public class ReferencedGeneratedClasses {
	
	protected static Map<String,String> generatedClassNameToReferencedGeneratedClassName = new HashMap<String, String>();
	
	/**
	 * Returns the slashed class names of all referenced generated classes, except for the declaring class itself.
	 */
	public synchronized static String nameOfGeneratedClassReferenced(String className, byte[] classBytes) {
		String cached = generatedClassNameToReferencedGeneratedClassName.get(className);
		if(cached!=null) return cached;
		
		Set<String> res = namesOfGeneratedClassesReferenced(classBytes);
        if(res.isEmpty()) return null;
        
        // ToDo: Find a fix for the limitation
        if(res.size()>1) 
            throw new RuntimeException("Class "+className+"references more than one other generated class: "+res+"\n (Current known limitation of TamiFlex.)");
        
        // res has exactly one element
        String ref = res.iterator().next();
        
        // Update cache
        generatedClassNameToReferencedGeneratedClassName.put(className, ref);
        
		return ref;
	}

	/**
	 * Returnes the slashed class names of all referenced generated classes, except for the declaring class itself.
	 */
	private static Set<String> namesOfGeneratedClassesReferenced(byte[] classBytes) {
		final Set<String> res = new HashSet<String>();
		ClassReader creader = new ClassReader(classBytes);

        // new ClassVisitor(ASM9) {} is equivalent to an empty class visitor
		ReferencedClassesExtracter visitor = new ReferencedClassesExtracter(new ClassVisitor(ASM9) {}, res);
        
        // Can be optimized with flags (ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
        creader.accept(visitor, 0);
        
        // Remove name of the declaring class
        res.remove(visitor.getClassName());
        return res;
	}
}
