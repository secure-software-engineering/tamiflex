/*******************************************************************************
 * Copyright (c) 2026 Gauravsingh Sisodia.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gauravsingh Sisodia - initial implementation
 ******************************************************************************/
package de.bodden.tamiflex.playout.transformation.invoke;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

public class GetMethodsTransformation extends AbstractGetMethodsTransformation {
	
	public GetMethodsTransformation() throws Exception {
		super(new Method("getMethods", "()[Ljava/lang/reflect/Method;"));
	}

	@Override
	protected String methodName() {
		return "sortMethods";
	}

	@Override
	protected String methodSignature() {
		return "([Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;";
	}
}
