package de.bodden.tamiflex.playout.transformation;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class ClassNewInstanceTransformation extends Transformation {
	
	public ClassNewInstanceTransformation() {
		super(Class.class);
	}
	
	@Override
	protected MethodVisitor getTransformationVisitor(String name, String desc, MethodVisitor parent) {
		if ("newInstance0".equals(name))
			return new MethodAdapter(parent) {
			
				@Override
				public void visitInsn(int opcode) {
					if (opcode == ARETURN) {
						mv.visitVarInsn(ALOAD, 0); // Load Class instance
						mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "classNewInstance", "(Ljava/lang/Class;)V");
					}
					super.visitInsn(opcode);
				}
			};
		else
			return parent;
	}
}
