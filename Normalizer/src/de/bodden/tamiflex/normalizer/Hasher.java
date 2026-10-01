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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.ClassRemapper;

import static org.objectweb.asm.Opcodes.ASM9;

import de.bodden.tamiflex.normalizer.ClassRenamer.NoHashedNameException;

public class Hasher {
	
	protected final static Map<String,String> generatedClassNameToHashedClassName = new HashMap<String, String>();	
	protected final static Map<byte[],String> generatedClassBytesToHashedClassName = new HashMap<byte[], String>();	

	protected final static Map<String,byte[]> hashedClassNameToOriginalBytes = new HashMap<String, byte[]>();	

	/**
	 * Classes containing these strings are blacklisted, i.e. calls to these classes will not be written to the log.
	 * Further, these classes will not be written to disk.
	 * This is because the classes are "unstable". They are generated and therefore can change from one run to another.
	 * In particular, the number suffixed of the names of these classes can easily change.
     * 
     * Note: Anything post the first occurance of infix is ignored and replaced with a hash value
     * ToDo: Change if necessary to only change if the infix occurs at the end of the className 
     *  i.e the name of the class and not the path in the interal name
	 */
	protected static String[] instableNames = {
		// "GeneratedConstructorAccessor",
		// "GeneratedMethodAccessor",
		// "GeneratedSerializationConstructorAccessor",
		// "ByCGLIB",
		// "org/apache/derby/exe/",
		// "$Proxy",
  //       "org/apache/activemq/store/journal/JournalPersistenceAdapter",  // DaCapo-9.12 tradebeans and tradesoap benchmarks
  //       "org/apache/activemq/store/journal/JournalTopicMessageStore",   // DaCapo-9.12 tradebeans and tradesoap benchmarks
        "BySpringCGLIB",                                                // DaCapo-23.10 spring benchmark
        // "$HibernateProxy",                                              // DaCapo-23.10 spring benchmark
  //       "org/eclipse/jdt/internal/core/search/indexing/IndexManager",   // DaCapo-23.10 eclipse benchmark
        "jdk\\/proxy\\d+\\/\\$Proxy",
        "net\\/bytebuddy\\/description\\/(?:method|type)\\/\\$Proxy"
        // Note: Yet to address tradebeans and tradesoap benchmarks from DaCapo-23.10
        /*,"schemaorg_apache_xmlbeans/system/" these names seem to be stable, as they are already hashed */
	};
	
	public static void dontNormalize() {
		instableNames = new String[0];
	}
	
    // Synchronization is necessary as PIA/ClassReplacer can run in multiple threads simultaneously
	public synchronized static void generateHashNumber(final String theClassName, byte[] classBytes) throws NoHashedNameException {
		boolean usingAssertions = false; assert usingAssertions = true;
		
		// If we don't use assertions then simply return if the hash code was already computed
		if (!usingAssertions && generatedClassNameToHashedClassName.containsKey(theClassName)) return;
		
		assert isGeneratedClass(theClassName) : "Class "+theClassName+" does not have an instable class name.";
		
        ClassReader creader = new ClassReader(classBytes);
    	ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // I think COMPUTE_MAXS suffices here for hash computation
    	
        ClassRemapper visitor = new ClassRemapper(ASM9, writer, 
            new Remapper(ASM9) {
                // Rename type references to generated classes (for hashing purposes)
                @Override
                public String map(String typeName) {
                    if (theClassName.equals(typeName)) return "$$$NORMALIZED$$$";
                    
                    String newName = generatedClassNameToHashedClassName.get(typeName);
                    if (Hasher.isGeneratedClass(typeName) && newName==null) {
                        // Should not happen because LinkedHashMap is used in POA/ClassDumper 
                        // which maintains the order in which classes are loaded
                        // See section 4.4.1 of Tamiflex's technical report
                        throw new NoHashedNameException(typeName);
                    }

                    if (newName!=null) typeName = newName;
                    return super.map(typeName);
                }
            }
        ) {
            @Override
    		public void visitSource(String source, String debug) {
    			/* we ignore the source-file attribute during hashing;
    			 * the position at which this attribute is inserted is kind of random,
    			 * and can therefore lead to unwanted noise */
    		}

            // Rename type references (in String constants) to generated classes (for hashing purposes)
            // See ClassRenamer for further understanding
            @Override
    		public MethodVisitor visitMethod(int access, String name, 
                    String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new RemappingStringConstantVisitor(mv, 
                    new StringRemapper() {
                        @Override
                        public String remapStringConstant(String constant) {
                            String slashed = slashed(constant);
                            if (theClassName.equals(slashed)) return "$$$NORMALIZED$$$";
                            
                            String to = generatedClassNameToHashedClassName.get(slashed);
                            if (Hasher.isGeneratedClass(slashed) && to==null) {
                                // Should not happen
                                throw new NoHashedNameException(slashed);
                            }

                            if (to!=null) constant = dotted(to);
    					    return super.remapStringConstant(constant);
                        }
                    }
                );
                return mv;
            }
        };

    	creader.accept(visitor, 0);
        byte[] renamed = writer.toByteArray();
		
        // Compute Hash
        // Anything post the first occurance of infix is ignored and replaced with a hash value
		String hash = SHAHash.SHA1(renamed);
		for (String infix: instableNames) {
            Matcher matcher = Pattern.compile(infix).matcher(theClassName);
            if (matcher.find()) {
				String hashedName = theClassName.substring(0, matcher.end()) + "$HASHED$" + hash;
                // Special handling for jdk.proxy
                if (infix.equals("jdk\\/proxy\\d+\\/\\$Proxy")) {
                    hashedName = theClassName.replaceAll("(jdk/proxy)\\d+(/\\$Proxy)\\d+", "$1$2") + "$HASHED$" + hash;
                }
				
                // Useful check to find out previously unknown instable class names
				assert !generatedClassNameToHashedClassName.containsKey(theClassName)
					|| generatedClassNameToHashedClassName.get(theClassName).equals(hashedName) :
					"Hashed names not stable for "+theClassName+" -> "+generatedClassNameToHashedClassName.get(theClassName)+", "+hashedName;
					
                generatedClassNameToHashedClassName.put(theClassName, hashedName);
                break;
			}
		}
        assert generatedClassNameToHashedClassName.containsKey(theClassName);
	}

	public synchronized static void generateHashNumberForHidden(final String theClassName, byte[] classBytes) throws NoHashedNameException {
		boolean usingAssertions = false; assert usingAssertions = true;
		
		// If we don't use assertions then simply return if the hash code was already computed
		if (!usingAssertions && generatedClassNameToHashedClassName.containsKey(theClassName)) return;
		
		assert isGeneratedClass(theClassName) : "Class "+theClassName+" does not have an instable class name.";
		
        ClassReader creader = new ClassReader(classBytes);
    	ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // I think COMPUTE_MAXS suffices here for hash computation

        ClassRemapper visitor = new ClassRemapper(ASM9, writer, 
            new Remapper(ASM9) {
                // Rename type references to generated classes (for hashing purposes)
                @Override
                public String map(String typeName) {
                    if (theClassName.equals(typeName)) return "$$$NORMALIZED$$$";

                    String newName = generatedClassNameToHashedClassName.get(typeName);
                    if (Hasher.isGeneratedClass(typeName) && newName==null) {
                        // Should not happen because LinkedHashMap is used in POA/ClassDumper 
                        // which maintains the order in which classes are loaded
                        // See section 4.4.1 of Tamiflex's technical report
                        throw new NoHashedNameException(typeName);
                    }

                    if (newName!=null) typeName = newName;
                    return super.map(typeName);
                }
            }
        ) {
            @Override
    		public void visitSource(String source, String debug) {
    			/* we ignore the source-file attribute during hashing;
    			 * the position at which this attribute is inserted is kind of random,
    			 * and can therefore lead to unwanted noise */
    		}

            // Rename type references (in String constants) to generated classes (for hashing purposes)
            // See ClassRenamer for further understanding
            @Override
    		public MethodVisitor visitMethod(int access, String name, 
                    String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new RemappingStringConstantVisitor(mv, 
                    new StringRemapper() {
                        @Override
                        public String remapStringConstant(String constant) {
                            String slashed = slashed(constant);
                            if (theClassName.equals(slashed)) return "$$$NORMALIZED$$$";

                            String to = generatedClassNameToHashedClassName.get(slashed);
                            if (Hasher.isGeneratedClass(slashed) && to==null) {
                                // Should not happen
                                throw new NoHashedNameException(slashed);
                            }

                            if (to!=null) constant = dotted(to);
    					    return super.remapStringConstant(constant);
                        }
                    }
                );
                return mv;
            }
        };

    	creader.accept(visitor, 0);
        byte[] renamed = writer.toByteArray();
		
        // Compute Hash
		String hash = SHAHash.SHA1(renamed);
        String hashedName = theClassName + "$HASHED$" + hash;
        generatedClassBytesToHashedClassName.put(classBytes, hashedName);
        assert generatedClassBytesToHashedClassName.containsKey(classBytes);
	}
	
	public static boolean isGeneratedClass(String className) {
		assert !className.contains(".") : "Class name must contain slashes, not dots: "+className; 
		for (String name: instableNames) {
            if (Pattern.compile(name).matcher(className).find()) {
                return true;
            }
		}
		return false;
	}

	public static String hashedClassNameForGeneratedClassName(String className) {
		assert !className.contains(".") : "Class name must contain slashes, not dots: "+className; 
		assert isGeneratedClass(className) : "Not a generated class name: "+className;
		String hashedName = generatedClassNameToHashedClassName.get(className);
		assert hashedName != null : "No hashed class name for generated class: "+className;
		return hashedName;
	}

	public static String hashedClassNameForGeneratedClassBytes(byte[] classBytes) {
		String hashedName = generatedClassBytesToHashedClassName.get(classBytes);
		return hashedName;
	}
	
	public static byte[] replaceGeneratedClassNamesByHashedNames(byte[] classBytes) {
		return ClassRenamer.replaceClassNamesInBytes(generatedClassNameToHashedClassName, classBytes);
	}
	

	public static String dotted(String className) {
		return className.replace('/', '.');
	}
    
	public static String slashed(String className) {
		return className.replace('.', '/');
	}
	
}
