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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Extracts the name of the declared class from a byte array defining that class.
 */
public class NameExtractor {

	public static String extractName(byte[] classfileBuffer) {
		ClassReader reader = new ClassReader(classfileBuffer);
		final String[] res = new String[1];
		reader.accept(new ClassAdapter(new EmptyVisitor()) {
			@Override
			public void visit(int version, int access, String name,
					String signature, String superName, String[] interfaces) {
				res[0] = name;
				super.visit(version, access, name, signature, superName, interfaces);
			}
		},ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
		return res[0];
	}
	
}
