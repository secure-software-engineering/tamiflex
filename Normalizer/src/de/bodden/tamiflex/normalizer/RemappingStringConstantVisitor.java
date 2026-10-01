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
package de.bodden.tamiflex.normalizer;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A {@link MethodVisitor} that calls the provided {@link StringRemapper} to re-map string constants.
 * ASM's ClassWriter typically constructs Constant Pool through visitxyz() calls of MethodVisitor
 * Therefore modifying the visitLdcInsn effectively transforms the constant pool. This class only transforms String constants
 */
public class RemappingStringConstantVisitor extends MethodVisitor {
    
	protected final StringRemapper rm;

	public RemappingStringConstantVisitor(MethodVisitor parentMV, StringRemapper rm) {
		super(ASM9, parentMV);
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
