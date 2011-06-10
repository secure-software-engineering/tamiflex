package de.bodden.tamiflex.playout.transformation;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public abstract class Transformation {
	
	private Class<?> affectedClass;
	
	public Transformation(Class<?> affectedClass) {
		this.affectedClass = affectedClass;
	}
	
	public Class<?> getAffectedClass() {
		return affectedClass;
	}
	
	public ClassVisitor getClassVisitor(ClassVisitor parent) {
		return new ClassAdapter(parent) {
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
				return getTransformationVisitor(name, desc, parent);
			}
		};
	}
	
	protected abstract MethodVisitor getTransformationVisitor(String name, String desc, MethodVisitor parent);
}
