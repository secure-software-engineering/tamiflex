package de.bodden.tamiflex.playout.transformation;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class FieldSetTransformation extends Transformation {
	
	private static List<String> fieldSets = Arrays.asList(
			"set",
			"setBoolean",
			"setByte",
			"setChar",
			"setInt",
			"setLong",
			"setFloat",
			"setDouble",
			"setShort");
	
	public FieldSetTransformation() {
		super(Field.class);
	}
	
	@Override
	protected MethodVisitor getTransformationVisitor(String name, String desc, MethodVisitor parent) {
		if (fieldSets.contains(name))
			return new MethodAdapter(parent) {
			
				@Override
				public void visitInsn(int opcode) {
					if (opcode == RETURN) {
						mv.visitVarInsn(ALOAD, 0); // Load Field instance
						mv.visitMethodInsn(INVOKESTATIC, "de/bodden/tamiflex/playout/rt/ReflLogger", "fieldSet", "(Ljava/lang/reflect/Field;)V");
					}
					super.visitInsn(opcode);
				}
			};
		else
			return parent;
	}
}
