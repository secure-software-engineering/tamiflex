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
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import de.bodden.tamiflex.normalizer.NameExtractor;
import de.bodden.tamiflex.playout.transformation.ArrayMultiNewInstanceTransformation;
import de.bodden.tamiflex.playout.transformation.ArrayNewInstanceTransformation;
import de.bodden.tamiflex.playout.transformation.ClassForNameTransformation;
import de.bodden.tamiflex.playout.transformation.Transformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetDeclaredFieldTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetDeclaredFieldsTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetDeclaredMethodTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetDeclaredMethodsTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetFieldTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetFieldsTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetMethodTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetMethodsTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassGetModifiersTransformation;
import de.bodden.tamiflex.playout.transformation.clazz.ClassNewInstanceTransformation;
import de.bodden.tamiflex.playout.transformation.constructor.ConstructorGetModifiersTransformation;
import de.bodden.tamiflex.playout.transformation.constructor.ConstructorNewInstanceTransformation;
import de.bodden.tamiflex.playout.transformation.constructor.ConstructorToGenericStringTransformation;
import de.bodden.tamiflex.playout.transformation.constructor.ConstructorToStringTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldGetDeclaringClassTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldGetModifiersTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldGetNameTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldGetTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldSetTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldToGenericStringTransformation;
import de.bodden.tamiflex.playout.transformation.field.FieldToStringTransformation;
import de.bodden.tamiflex.playout.transformation.method.MethodGetDeclaringClassTransformation;
import de.bodden.tamiflex.playout.transformation.method.MethodGetModifiersTransformation;
import de.bodden.tamiflex.playout.transformation.method.MethodGetNameTransformation;
import de.bodden.tamiflex.playout.transformation.method.MethodInvokeTransformation;
import de.bodden.tamiflex.playout.transformation.method.MethodToGenericStringTransformation;
import de.bodden.tamiflex.playout.transformation.method.MethodToStringTransformation;

public class ReflectionMonitor implements ClassFileTransformer {
	
	private List<Transformation> transformations = Arrays.<Transformation>asList(
			new ClassForNameTransformation(),
			new ClassGetDeclaredFieldsTransformation(),
			new ClassGetDeclaredFieldTransformation(),
			new ClassGetDeclaredMethodsTransformation(),
			new ClassGetDeclaredMethodTransformation(),
			new ClassGetFieldTransformation(),
			new ClassGetFieldsTransformation(),
			new ClassGetMethodsTransformation(),
			new ClassGetMethodTransformation(),
			new ClassGetModifiersTransformation(),
			new ClassNewInstanceTransformation(),
			new ArrayNewInstanceTransformation(),
			new ArrayMultiNewInstanceTransformation(),
			new ConstructorGetModifiersTransformation(),
			new ConstructorNewInstanceTransformation(),
			new ConstructorToGenericStringTransformation(),
			new ConstructorToStringTransformation(),
			new FieldGetDeclaringClassTransformation(),
			new FieldGetModifiersTransformation(),
			new FieldGetNameTransformation(),
			new FieldGetTransformation(),
			new FieldSetTransformation(),
			new FieldToGenericStringTransformation(),
			new FieldToStringTransformation(),
			new MethodGetDeclaringClassTransformation(),
			new MethodGetModifiersTransformation(),
			new MethodGetNameTransformation(),
			new MethodInvokeTransformation(),
			new MethodToGenericStringTransformation(),
			new MethodToStringTransformation());
	
	public List<Class<?>> getAffectedClasses() {
		List<Class<?>> affectedClasses = new ArrayList<Class<?>>();
		
		for (Transformation transformation : transformations)
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
			final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor visitor = writer;
			
			for (Transformation transformation : transformations)
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
	
	public String listTransformations() {
		StringBuilder sb = new StringBuilder();
		for (Transformation t : transformations) {
			for(Method m: t.getAffectedMethods()) {
				sb.append(t.getAffectedClass().getName()+"."+m.getName()+m.getDescriptor()+"\n");
			}
		}
		return sb.toString();
	}
}
