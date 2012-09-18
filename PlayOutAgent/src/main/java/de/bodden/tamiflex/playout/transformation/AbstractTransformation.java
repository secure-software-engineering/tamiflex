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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public abstract class AbstractTransformation {
	
	private final Class<?> affectedClass;
	
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
	
	public ClassVisitor getClassVisitor(String name, ClassVisitor parent) {
		if (!name.equals(Type.getInternalName(affectedClass)))
			return parent;
		
		return new ClassAdapter(parent) {
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				final MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
				
				if (!affectedMethods.contains(new Method(name, desc)))
					return parent;
				
				return getMethodVisitor(parent);
			}
		};
	}
	
	protected abstract MethodVisitor getMethodVisitor(MethodVisitor parent);
}
