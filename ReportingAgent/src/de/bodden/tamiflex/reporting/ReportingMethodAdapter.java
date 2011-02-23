/*******************************************************************************
 * Copyright (c) 2011 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
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
