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

import static org.objectweb.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import de.bodden.tamiflex.normalizer.NameExtractor;

public class ReflectionMonitor implements ClassFileTransformer {
	
	public byte[] transform(ClassLoader loader, String className,
		Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
		byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className==null) {
			className = NameExtractor.extractName(classfileBuffer);
		}
		final String theClassName = className;
		
		if(!className.equals("java/lang/Class") && !className.equals("java/lang/reflect/Method") && !className.equals("java/lang/reflect/Constructor") && !className.equals("java/lang/reflect/Array") && !className.equals("java/lang/reflect/Field")) return null;		
		
        try {
        	// scan class binary format to find fields for toString() method
        	ClassReader creader = new ClassReader(classfileBuffer);
        	ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        	
            ClassVisitor visitor = new ClassAdapter(writer) {
            	
            	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
            		//delegate
            		MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
            		if(theClassName.equals("java/lang/Class") && methodName.equals("forName")) {
            			mv = new ClassForNameAdapter(mv);            			
            		} else if(theClassName.equals("java/lang/Class") && methodName.equals("newInstance0")) {
            			mv = new ClassNewInstanceAdapter(mv);            			
            		} else if(theClassName.equals("java/lang/reflect/Method") && methodName.equals("invoke")) {
            			mv = new MethodInvokeAdapter(mv);            			
            		} else if(theClassName.equals("java/lang/reflect/Constructor") && methodName.equals("newInstance")) {
            			mv = new ConstructorNewInstanceAdapter(mv);            			
            		} else if(theClassName.equals("java/lang/reflect/Array") && methodName.equals("newInstance") && desc.equals("(Ljava/lang/Class;I)Ljava/lang/Object;")) {
                        mv = new ArrayNewInstanceAdapter(mv);
                    } else if(theClassName.equals("java/lang/reflect/Array") && methodName.equals("newInstance") && desc.equals("(Ljava/lang/Class;[I)Ljava/lang/Object;")) {
                        mv = new ArrayMultiNewInstanceAdapter(mv);
                    }else if(theClassName.equals("java/lang/reflect/Field") && fieldSets.contains(methodName)) {
                        mv = new FieldSetAdapter(mv);
                    } else if(theClassName.equals("java/lang/reflect/Field") && fieldGets.contains(methodName)) {
                        mv = new FieldGetAdapter(mv);
                    }
            		return mv;
            	};
            	
            };
            creader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return writer.toByteArray();
        } catch (IllegalStateException e) {
            throw new IllegalClassFormatException("Error: " + e.getMessage() +
                " on class " + className);
		} catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private static List<String> fieldSets = Arrays.asList("set", "setBoolean", "setByte", "setChar", "setInt", "setLong", "setFloat", "setDouble", "setShort");
	
	private static List<String> fieldGets = Arrays.asList("get", "getBoolean", "getByte", "getChar", "getInt", "getLong", "getFloat", "getDouble", "getShort");
	
	static class ClassForNameAdapter extends MethodAdapter {

		public ClassForNameAdapter(MethodVisitor mv) {
			super(mv);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if(opcode==Opcodes.ARETURN) {
    			//load first argument on stack, i.e. the name of the class to be loaded
    			mv.visitVarInsn(ALOAD, 0);
    			//call logging method with that Class object as argument
				mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "classForName", "(Ljava/lang/String;)V");
			}
			super.visitInsn(opcode);
		}
		
	}

	static class ClassNewInstanceAdapter extends MethodAdapter {

		public ClassNewInstanceAdapter(MethodVisitor mv) {
			super(mv);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if(opcode==Opcodes.ARETURN) {
    			//load "this" on stack, i.e. the Class object
    			mv.visitVarInsn(ALOAD, 0);
    			//call logging method with that Class object as argument
				mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "classNewInstance", "(Ljava/lang/Class;)V");
			}
			super.visitInsn(opcode);
		}
		
	}

	static class MethodInvokeAdapter extends MethodAdapter {

		public MethodInvokeAdapter(MethodVisitor mv) {
			super(mv);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if(opcode==Opcodes.ARETURN) {
    			//load first parameter on the stack, i.e. the designated receiver object
    			mv.visitVarInsn(ALOAD, 1);
    			//load "this" on stack, i.e. the Method object
    			mv.visitVarInsn(ALOAD, 0);
    			//call logging method with that Method object as argument
				mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "methodInvoke", "(Ljava/lang/Object;Ljava/lang/reflect/Method;)V");
			}
			super.visitInsn(opcode);
		}
		
	}
	
	static class ConstructorNewInstanceAdapter extends MethodAdapter {

		public ConstructorNewInstanceAdapter(MethodVisitor mv) {
			super(mv);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if(opcode==Opcodes.ARETURN) {
    			//load "this" on stack, i.e. the Constructor object
    			mv.visitVarInsn(ALOAD, 0);
    			//call logging method with that Method object as argument
				mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "constructorNewInstance", "(Ljava/lang/reflect/Constructor;)V");
			}
			super.visitInsn(opcode);
		}
		
	}
	
	static class ArrayNewInstanceAdapter extends MethodAdapter {

	    public ArrayNewInstanceAdapter(MethodVisitor mv) {
            super(mv);
        }
        
        @Override
        public void visitInsn(int opcode) {
            if(opcode==Opcodes.ARETURN) {
                //load the class object on stack
                mv.visitVarInsn(ALOAD, 0);
                //load dimension on stack
                mv.visitVarInsn(ILOAD, 1);
                //call logging method
                mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "arrayNewInstance", "(Ljava/lang/Class;I)V");
            }
            super.visitInsn(opcode);
        }
        
    }
	
   static class ArrayMultiNewInstanceAdapter extends MethodAdapter {

        public ArrayMultiNewInstanceAdapter(MethodVisitor mv) {
            super(mv);
        }
        
        @Override
        public void visitInsn(int opcode) {
            if(opcode==Opcodes.ARETURN) {
                //load the class object on stack
                mv.visitVarInsn(ALOAD, 0);
                //load dimension array on stack
                mv.visitVarInsn(ALOAD, 1);
                //call logging method
                mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "arrayMultiNewInstance", "(Ljava/lang/Class;[I)V");
            }
            super.visitInsn(opcode);
        }
        
    }
	
	static class FieldSetAdapter extends MethodAdapter {
	    
	    public FieldSetAdapter(MethodVisitor mv) {
	        super(mv);
	    }
	    
	    @Override
	    public void visitInsn(int opcode) {
	        if(opcode==Opcodes.RETURN) {
                //load "this" on stack, i.e. the Field object
                mv.visitVarInsn(ALOAD, 0);
                //call logging method with that Field object as argument
                mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "fieldSet", "(Ljava/lang/reflect/Field;)V");
	        }
	        super.visitInsn(opcode);
	    }
	}
	
    static class FieldGetAdapter extends MethodAdapter {
        
        public FieldGetAdapter(MethodVisitor mv) {
            super(mv);
        }
        
        @Override
        public void visitInsn(int opcode) {
            if(opcode==Opcodes.ARETURN || opcode==Opcodes.IRETURN || opcode==Opcodes.LRETURN || opcode==Opcodes.FRETURN || opcode==Opcodes.DRETURN) {
                //load "this" on stack, i.e. the Field object
                mv.visitVarInsn(ALOAD, 0);
                //call logging method with that Field object as argument
                mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "fieldGet", "(Ljava/lang/reflect/Field;)V");
            }
            super.visitInsn(opcode);
        }
    }
}
