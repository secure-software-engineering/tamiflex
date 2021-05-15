/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 *     Andreas Sewe - coverage of array creation and reflective field accesses
 ******************************************************************************/
package de.bodden.tamiflex.playout;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.normalizer.NameExtractor;
import de.bodden.tamiflex.playout.transformation.AbstractTransformation;

public class ReflectionMonitor implements ClassFileTransformer {
	
	private List<AbstractTransformation> transformations = new LinkedList<AbstractTransformation>();
	
	public ReflectionMonitor(String instruments, boolean verbose) {
		List<String> split = new ArrayList<String>(Arrays.asList(instruments.split("[ ]+")));
		Collections.sort(split);
		if(verbose) {
			System.out.println("\nActive instruments:");
		}
		for (String className : split) {
			className = className.trim();
			if(className.isEmpty()) continue;
			try {				
				@SuppressWarnings("unchecked")
				Class<AbstractTransformation> c = (Class<AbstractTransformation>) Class.forName(className);
				AbstractTransformation transform = c.newInstance();
				transformations.add(transform);
				if(verbose) {
					System.out.print(className);
					System.out.println(": ");
					for(Method m: transform.getAffectedMethods()) {
						System.out.print("    ");
						System.out.print(transform.getAffectedClass().getName()+"."+m.getName()+m.getDescriptor()+"\n");
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("There was an error instantiating the instrument "+className, e);
			}
		}
	}

	public List<Class<?>> getAffectedClasses() {
		List<Class<?>> affectedClasses = new ArrayList<Class<?>>();
		
		for (AbstractTransformation transformation : transformations)
			affectedClasses.add(transformation.getAffectedClass());
		
		return affectedClasses;
	}
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className==null)
			className = NameExtractor.extractName(classfileBuffer);
		
		try {
			final ClassReader creader = new ClassReader(classfileBuffer);
			final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			ClassVisitor visitor = writer;
			
			for (AbstractTransformation transformation : transformations)
				visitor = transformation.getClassVisitor(className, visitor);
			
			creader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			return writer.toByteArray();
		} catch (IllegalStateException e) {
			throw new IllegalClassFormatException("Error: " + e.getMessage() + " on class " + className);
		} catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
}
