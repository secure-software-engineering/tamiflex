package de.bodden.tamiflex.playout.transformation;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Array;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class ArrayNewInstanceTransformation extends Transformation {
	
	public ArrayNewInstanceTransformation() {
		super(Array.class);
	}
	
	@Override
	protected MethodVisitor getTransformationVisitor(String name, String desc, MethodVisitor parent) {
		if ("newInstance".equals(name) && "(Ljava/lang/Class;I)Ljava/lang/Object;".equals(desc))
			return new MethodAdapter(parent) {
			
				@Override
				public void visitInsn(int opcode) {
					if (opcode == ARETURN) {
						mv.visitVarInsn(ALOAD, 0); // Load Class instance
						mv.visitVarInsn(ILOAD, 1); // Load dimension
						mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "arrayNewInstance", "(Ljava/lang/Class;I)V");
					}
					super.visitInsn(opcode);
				}
			};
		else
			return parent;
	}
}
