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
package de.bodden.tamiflex.playin.rt;

import static de.bodden.tamiflex.normalizer.Hasher.hashedClassNameForGeneratedClassBytes;
import static de.bodden.tamiflex.normalizer.Hasher.generateHashNumber;
import static de.bodden.tamiflex.normalizer.Hasher.generateHashNumberForHidden;

import org.objectweb.asm.ClassReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public class HiddenClassLoader {
	private static String inPath;
	public static int numSuccess = 0;
	public static int numInvoked = 0;

	private static ThreadLocal<Integer> nestingDepth = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return 0;
		}
	};
	
	public static void enteringReflectionAPI() {
		nestingDepth.set(nestingDepth.get()+1);
	}

	private static void leavingReflectionAPI() {
		nestingDepth.set(nestingDepth.get()-1);
	}

    private static boolean isReentrant() {
    	//this method is called at every entry point to
    	//the TamiFlex runtime; at this point we are entering the reflection API
    	enteringReflectionAPI();

    	//check if we have a recursive call caused by TamiFlex itself;
    	//this is the case if depth is >1
    	Integer depth = nestingDepth.get();
    	if(depth>1) {

    		//by convention, when this method returns true,
    		//we will be leaving the TamiFlex runtime (callers must    		
    		//return immediately); hence we here flag that we leave the API
    		leavingReflectionAPI();
    		return true;
    	} else {
    		return false;
    	}
	}

	public static void setInPath(String path) {
		inPath = path;
	}

    public static byte[] loadHiddenClass(byte[] c, ClassLoader loader) {
        if (isReentrant()) return c;
        byte[] returnBytes = c;
        try {
            synchronized (HiddenClassLoader.class) {
                numInvoked++;
            }
            ClassReader cr = new ClassReader(c);
            String currentRuntimeName = cr.getClassName();
            if (currentRuntimeName.startsWith("java/lang/invoke/LambdaForm$BMH") ||
                currentRuntimeName.startsWith("java/lang/invoke/LambdaForm$DMH") ||
                currentRuntimeName.startsWith("java/lang/invoke/LambdaForm$MH")) {
                leavingReflectionAPI();
                return c;
            }

            String lookupName = currentRuntimeName; 

            String loader_name = "null_loader";
            if (loader != null) {
                loader_name = loader.getClass().getName();
            }
            File localOutDir = new File(inPath, loader_name);
            generateHashNumberForHidden(currentRuntimeName, c);
            String simpleName = hashedClassNameForGeneratedClassBytes(c);;
            if (lookupName.contains("/")) {
                String packageName = lookupName.substring(0, lookupName.lastIndexOf('/'));
                simpleName = simpleName.substring(simpleName.lastIndexOf('/') + 1);

                localOutDir = new File(localOutDir, packageName);
            }

            String fileName = simpleName + ".class";
            File targetFile = new File(localOutDir, fileName);

            if (targetFile.exists()) {
                try {
                    // System.out.println("Loading dumped lambda from: " + targetFile.getAbsolutePath());
                    byte[] diskBytes = Files.readAllBytes(targetFile.toPath());
                    returnBytes = diskBytes;
                    synchronized (HiddenClassLoader.class) {
                        numSuccess++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // System.err.println("Dumped lambda not found: " + targetFile.getAbsolutePath());
                returnBytes = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
			leavingReflectionAPI();
        }
        return returnBytes;
    }

    public static Method[] sortMethods(Method[] m) {
		if(isReentrant()) return m;
		try {
            if (m == null || m.length <= 1) {
                return m;
            }

            Method[] sorted = m.clone();

            Arrays.sort(sorted, Comparator.comparing(t ->
                t.getName() + MethodType.methodType(t.getReturnType(), t.getParameterTypes()).toMethodDescriptorString() + t.getDeclaringClass().getName()
            ));

            return sorted;
		} finally {
			leavingReflectionAPI();
		}
    }
}
