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

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import de.bodden.tamiflex.normalizer.ClassRenamer.NoHashedNameException;

public class Hasher {
	
	protected final static Map<String,String> generatedClassNameToHashedClassName = new HashMap<String, String>();	

	protected final static Map<String,byte[]> hashedClassNameToOriginalBytes = new HashMap<String, byte[]>();	

	/**
	 * Classes containing these strings are blacklisted, i.e. calls to these classes will not be written to the log.
	 * Further, these classes will not be written to disk.
	 * This is because the classes are "unstable". They are generated and therefore can change from one run to another.
	 * In particular, the numbered suffixed of the names of these classes can easily change.
	 */
	protected static String[] instableNames = {
		"GeneratedConstructorAccessor",
		"GeneratedMethodAccessor",
		"GeneratedSerializationConstructorAccessor",
		"ByCGLIB",
		"org/apache/derby/exe/",
		"$Proxy" /*,
		"schemaorg_apache_xmlbeans/system/" these names seem to be stable, as they are already hashed */
	};
	
	public synchronized static void generateHashNumber(final String theClassName, byte[] classBytes) throws NoHashedNameException {
		boolean usingAssertions = false; assert usingAssertions = true;		
		
		//if we don't use assertions then simply return if the hash code was alread computed
		if(!usingAssertions && generatedClassNameToHashedClassName.containsKey(theClassName)) return;
		
		assert containsGeneratedClassName(theClassName) : "Class "+theClassName+" contains no generated name.";
		ClassReader creader = new ClassReader(classBytes);
    	ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    	RemappingClassAdapter visitor = new RemappingClassAdapter(writer,new Remapper(){
    		
    		@Override
    		public String map(String typeName) {
    			if(theClassName.equals(typeName)) return "$$$NORMALIZED$$$";
    			String newName = generatedClassNameToHashedClassName.get(typeName);
				if(Hasher.containsGeneratedClassName(typeName) && newName==null) {
    				throw new NoHashedNameException(typeName);
    			}
    			if(newName!=null) typeName = newName;
    			return super.map(typeName);
    		}
    	}) {
    		@Override
    		public void visitSource(String source, String debug) {
    			/* we ignore the source-file attribute during hashing;
    			 * the position at which this attribute is inserted is kind of random,
    			 * and can therefore lead to unwanted noise */
    		}

    		@Override
    		public MethodVisitor visitMethod(int access, String name,
    				String desc, String signature, String[] exceptions) {
    			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    			mv = new RemappingStringConstantAdapter(mv, new StringRemapper() {
    				@Override
    				public String remapStringConstant(String constant) {
    					String slashed = slashed(constant);
		    			if(theClassName.equals(slashed)) return "$$$NORMALIZED$$$";
						String to = generatedClassNameToHashedClassName.get(slashed);
    	    			if(!theClassName.equals(slashed) && Hasher.containsGeneratedClassName(slashed) && to==null) {
    	    				throw new NoHashedNameException(slashed);
    	    			}
    					if(to!=null) constant = dotted(to);
    					return super.remapStringConstant(constant);
    				}
    			});
				return mv;
    		}
    	};
    	creader.accept(visitor, 0);
        byte[] renamed = writer.toByteArray();
		////////
		String hash = SHAHash.SHA1(renamed);
		for(String infix: instableNames) {
			if(theClassName.contains(infix)) {
				String hashedName = theClassName.substring(0, theClassName.indexOf(infix)+infix.length()) + "$HASHED$" + hash;
				
				assert !generatedClassNameToHashedClassName.containsKey(theClassName)
					|| generatedClassNameToHashedClassName.get(theClassName).equals(hashedName) :
					"Hashed names not stable for "+theClassName+": "+generatedClassNameToHashedClassName.get(theClassName)+","+hashedName;
					
				generatedClassNameToHashedClassName.put(theClassName, hashedName);
			}
		}
		assert generatedClassNameToHashedClassName.containsKey(theClassName);
	}
	
	public static boolean containsGeneratedClassName(String className) {
		assert !className.contains(".") : "Class name must contain slashes, not dots: "+className; 
		for(String name: instableNames) {
			if(className.contains(name))
				return true;
		}
		return false;
	}

	public static String hashedClassNameForGeneratedClassName(String className) {
		assert !className.contains(".") : "Class name must contain slashes, not dots: "+className; 
		assert containsGeneratedClassName(className) : "No generated class name: "+className;
		String hashedName = generatedClassNameToHashedClassName.get(className);
		assert hashedName != null : "No hashed class name for generated class: "+className;
		return hashedName;
	}
	
	public static byte[] replaceGeneratedClassNamesByHashedNames(byte[] classBytes) {
		return ClassRenamer.replaceClassNamesInBytes(generatedClassNameToHashedClassName,classBytes);		
	}
	

	public static String dotted(String className) {
		return className.replace('/', '.');
	}
    
	public static String slashed(String className) {
		return className.replace('.', '/');
	}
	
}
