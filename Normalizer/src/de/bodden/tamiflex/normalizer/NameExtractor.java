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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Extracts the name of the declared class from a byte array defining that class.
 */
public class NameExtractor {

	public static String extractName(byte[] classfileBuffer) {
		ClassReader cr = new ClassReader(classfileBuffer);
		final String[] className = new String[1];
		
		cr.accept(new ClassVisitor(Opcodes.ASM9) {
			// ClassVisitor is an abstract class with no abstract methods
			// Therefore this subclass is essentially an empty visitor (as it does not delegate calls to any further ClassVisitor)
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				className[0] = name;
				
				// Any visitor should strictly follow the visit call order (See section 2.2.1 of ASM documentation)				
				super.visit(version, access, name, signature, superName, interfaces);
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		
		return className[0];
	}
	
}
