package de.bodden.tamiflex.reporting;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public abstract class ReportingMethodAdapter extends MethodAdapter {

	public ReportingMethodAdapter(MethodVisitor mv) {
		super(mv);
	}

	@Override
	public void visitCode() {
		super.visitCode();
		insertCall(true);			
	}
	
	@Override
	public void visitInsn(int opcode) {
		if(opcode==Opcodes.ARETURN) {
			insertCall(false);
		}
		super.visitInsn(opcode);
	}

	protected abstract void insertCall(boolean beginningOfMethod);
	
}
