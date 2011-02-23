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
package de.bodden.tamiflex.reporting;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import de.bodden.tamiflex.normalizer.NameExtractor;

public class ReflectionMonitor implements ClassFileTransformer {
	
	public byte[] transform(ClassLoader loader, String className,
		Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
		byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className==null) {
			className = NameExtractor.extractName(classfileBuffer);
		}
		final String theClassName = className;
		
		if(!className.equals("java/lang/Class") && !className.equals("java/lang/reflect/Method") && !className.equals("java/lang/reflect/Constructor")) return null;		
		
        try {
        	// scan class binary format to find fields for toString() method
        	ClassReader creader = new ClassReader(classfileBuffer);
        	ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        	
            ClassVisitor visitor = new ClassAdapter(writer) {
            	
            	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
            		//delegate
            		MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
            		if(theClassName.equals("java/lang/Class") && methodName.equals("forName")) {
            			mv = new ClassForNameAdapter(mv,signature.contains("ClassLoader"));    
            		} else if(theClassName.equals("java/lang/Class") && methodName.equals("newInstance0")) {
            			mv = new ClassNewInstanceAdapter(mv);            			
            		} else if(theClassName.equals("java/lang/reflect/Method") && methodName.equals("invoke")) {
            			mv = new MethodInvokeAdapter(mv);            			
            		} else if(theClassName.equals("java/lang/reflect/Constructor") && methodName.equals("newInstance")) {
            			mv = new ConstructorNewInstanceAdapter(mv);            			
            		}

            		return mv;
            	};
            	            	
            };
            creader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            byte[] byteArray = writer.toByteArray();
            if(className.equals("java/lang/reflect/Constructor")) {
            	try {
					FileOutputStream fos = new FileOutputStream("Constructor.class");
					fos.write(byteArray);
					fos.close();
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
            }
			return byteArray;
		} catch (IllegalStateException e) {
            throw new IllegalClassFormatException("Error: " + e.getMessage() +
                " on class " + className);
		} catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	static class ClassForNameAdapter extends ReportingMethodAdapter {

		private final boolean hasClassLoaderArgument;

		public ClassForNameAdapter(MethodVisitor mv, boolean hasClassLoaderArgument) {
			super(mv);
			this.hasClassLoaderArgument = hasClassLoaderArgument;
		}
		
		protected void insertCall(boolean beginningOfMethod) {
			String methodToCall;
			if(hasClassLoaderArgument) {
				methodToCall = "classForNameWithClassLoader";
			} else {
				methodToCall = "classForName";     				
			}
			 			
			//at the method entry, we pass "true" as first argument, later-on "false" 
			mv.visitInsn(beginningOfMethod ? ICONST_1 : ICONST_0);    			
			
			//load first argument on stack, i.e. the name of the class to be loaded
			mv.visitVarInsn(ALOAD, 0);
			
			//call logging method with that Class object as argument
			mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/reporting/rt/ReflLogger", methodToCall, "(ZLjava/lang/String;)V");
		}
		
	}

	static class ClassNewInstanceAdapter extends ReportingMethodAdapter {

		public ClassNewInstanceAdapter(MethodVisitor mv) {
			super(mv);
		}

		@Override
		protected void insertCall(boolean beginningOfMethod) {
			//at the method entry, we pass "true" as first argument, later-on "false" 
			mv.visitInsn(beginningOfMethod ? ICONST_1 : ICONST_0);    			

				//load "this" on stack, i.e. the Class object
			mv.visitVarInsn(ALOAD, 0);
			//call logging method with that Class object as argument
			mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/reporting/rt/ReflLogger", "classNewInstance", "(ZLjava/lang/Class;)V");
		}
		
	}

	static class MethodInvokeAdapter extends ReportingMethodAdapter {

		public MethodInvokeAdapter(MethodVisitor mv) {
			super(mv);
		}
		
		@Override
		protected void insertCall(boolean beginningOfMethod) {
			//at the method entry, we pass "true" as first argument, later-on "false" 
			mv.visitInsn(beginningOfMethod ? ICONST_1 : ICONST_0);    			

			//load first parameter on the stack, i.e. the designated receiver object
			mv.visitVarInsn(ALOAD, 1);
			//load "this" on stack, i.e. the Method object
			mv.visitVarInsn(ALOAD, 0);
			//call logging method with that Method object as argument
			mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/reporting/rt/ReflLogger", "methodInvoke", "(ZLjava/lang/Object;Ljava/lang/reflect/Method;)V");
		}
		
	}
	
	static class ConstructorNewInstanceAdapter extends ReportingMethodAdapter {

		public ConstructorNewInstanceAdapter(MethodVisitor mv) {
			super(mv);
		}
		
		@Override
		protected void insertCall(boolean beginningOfMethod) {
			//at the method entry, we pass "true" as first argument, later-on "false" 
			mv.visitInsn(beginningOfMethod ? ICONST_1 : ICONST_0);    			

			//load "this" on stack, i.e. the Constructor object
			mv.visitVarInsn(ALOAD, 0);
			//call logging method with that Method object as argument
			mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/reporting/rt/ReflLogger", "constructorNewInstance", "(ZLjava/lang/reflect/Constructor;)V");
		}
		
	}
}
