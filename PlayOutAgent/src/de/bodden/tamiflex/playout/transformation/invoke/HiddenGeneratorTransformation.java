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

public class HiddenGeneratorTransformation extends AbstractHiddenTransformation {
	
	public HiddenGeneratorTransformation() throws Exception {
		super(new Method("makeHiddenClassDefiner", "(Ljava/lang/invoke/MethodHandles$Lookup$ClassFile;Ljava/util/Set;ZLjdk/internal/util/ClassFileDumper;)Ljava/lang/invoke/MethodHandles$Lookup$ClassDefiner;"));
	}

	@Override
	protected String methodName() {
		return "processHiddenClass";
	}

	@Override
	protected String methodSignature() {
		return "([BLjava/lang/ClassLoader;)[B";
	}
}
