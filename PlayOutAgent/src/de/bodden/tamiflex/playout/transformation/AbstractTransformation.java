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
package de.bodden.tamiflex.playout.transformation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import static org.objectweb.asm.Opcodes.ASM9;

public abstract class AbstractTransformation {
	
    // Any one of (Class | Constructor | Array | Field | Method).class
	private final Class<?> affectedClass;
	
    // Method -> org.objectweb.asm.commons.Method
    // List of methods belonging to `affectedClass` which are transformed by this Transformation
    // Multiple methods can exist with the same name but with different signature for ex: Two different methods named Class.forName
	private final List<Method> affectedMethods;
	
	public AbstractTransformation(Class<?> affectedClass, Method... affectedMethods) {
		this.affectedClass = affectedClass;
		this.affectedMethods = Arrays.asList(affectedMethods);
	}
	
	public Class<?> getAffectedClass() {
		return affectedClass;
	}
	
	public List<Method> getAffectedMethods() {
		return Collections.unmodifiableList(affectedMethods);
	}
	
    // See POA/ReflectionMonitor.java for usage
	public ClassVisitor getClassVisitor(String name, ClassVisitor parent) {
		if (!name.equals(Type.getInternalName(affectedClass)))
			return parent;
		
        return new ClassVisitor(ASM9, parent) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                final MethodVisitor parentMV = super.visitMethod(access, name, desc, signature, exceptions);

                // The method described by `name` and `desc` belongs to the `this.affectedClass` class because of the check
                // org.objectweb.asm.commons.Method overrides Object.equals() method
                if (!affectedMethods.contains(new Method(name, desc)))
                    return parentMV;

                // This method described by `name` and `desc` needs to be transformed
                return getMethodVisitor(parentMV);
            }
        };
	}
	
	protected abstract MethodVisitor getMethodVisitor(MethodVisitor parent);
}
