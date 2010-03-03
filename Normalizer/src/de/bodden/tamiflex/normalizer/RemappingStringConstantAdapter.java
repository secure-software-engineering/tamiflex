package de.bodden.tamiflex.normalizer;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

/**
 * A {@link MethodAdapter} that calls the provided {@link StringRemapper} to re-map
 * string constants.
 */
public class RemappingStringConstantAdapter extends MethodAdapter  {
    
	protected final StringRemapper rm;

	public RemappingStringConstantAdapter(MethodVisitor mv, StringRemapper rm) {
		super(mv);
		this.rm = rm;
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		if(cst instanceof String) {
			cst = rm.remapStringConstant((String) cst);
		}
		super.visitLdcInsn(cst);
	}

}
