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
package de.bodden.tamiflex.playin;
import static de.bodden.tamiflex.normalizer.Hasher.isGeneratedClass;
import static de.bodden.tamiflex.normalizer.ReferencedGeneratedClasses.nameOfGeneratedClassReferenced;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import org.objectweb.asm.ClassVisitor;

import de.bodden.tamiflex.normalizer.ClassRenamer;
import de.bodden.tamiflex.normalizer.Hasher;
import de.bodden.tamiflex.normalizer.NameExtractor;

public class ClassReplacer implements ClassFileTransformer {
	
	private static final String ASM_PKGNAME = ClassVisitor.class.getPackage().getName().replace('.', '/');

	/** If true, the agent will issue warnings. **/
	protected final boolean verbose;

    // Input directory
	protected final String inPath;

	/** A mapping from class names of generated classes to the original class bytes of the respective class, i.e.,
	 *  the bytes as they were just about to be loaded on the current execution.
	 *  See {@link Hasher#isGeneratedClass(String)} to determine if a class is generated in this sense.
	 */
	protected Map<String,byte[]> generatedClassNameToOriginalBytes = new HashMap<String, byte[]>();
	protected Map<String,URLClassLoader> pathToClassLoader = new HashMap<String, URLClassLoader>();
	
	public int numInvoked, numSuccess;
	
	public ClassReplacer(String inPath, boolean verbose) {
		this.verbose = verbose;				
		this.inPath = inPath;		
	}

	public byte[] transform(ClassLoader ldr, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		synchronized (this) {
            numInvoked++;
        }

		if(className==null) {
			className = NameExtractor.extractName(classfileBuffer);
		}
		try{
			if(isGeneratedClass(className))
				storeClassBytesOfGeneratedClass(className, classfileBuffer);			
            String loader_name = "null_loader";
            if (ldr != null) {
                loader_name = ldr.getClass().getName();
            }
			return tryToReplaceClassBytes(className, loader_name);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		} catch (Error e) {
			e.printStackTrace();
			throw e;
		}
	}

    public static byte[] normalizeClass(byte[] originalClassBytes) {
        ClassReader cr = new ClassReader(originalClassBytes);

        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // Deterministically sort fields and methods by name and descriptor
        cn.fields.sort(Comparator.comparing(f -> f.name + f.desc));
        cn.methods.sort(Comparator.comparing(m -> m.name + m.desc));

        // Deterministically set constant pool
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);

        return cw.toByteArray();
    }

	private byte[] tryToReplaceClassBytes(final String className, final String loader_name) {
		try {
            // Obviously don't modify files belonging to the below packages for correct functioning of PIA
			if(className.startsWith(Agent.PKGNAME)) return null;
			if(className.startsWith(ASM_PKGNAME)) return null;
			
			// Check if the class is generated, and if so generate a hashed class name and use that name in what follows
			String classNameInFileSystem;
			boolean isGeneratedClass;
			byte[] originalBytes;
			synchronized (this) {
				originalBytes = generatedClassNameToOriginalBytes.get(className);
				isGeneratedClass = Hasher.isGeneratedClass(className);
				if (isGeneratedClass) {
                    // This normalization is needed because Proxy classes don't have consistent constantpool and method ordering
                    originalBytes = normalizeClass(originalBytes);
					// Generate hash numbers based on the contents of all generated classes seen so far 
					Hasher.generateHashNumber(className, originalBytes);
					// We will load the class file using the hashed name
					classNameInFileSystem = Hasher.hashedClassNameForGeneratedClassName(className);
				} else {
					classNameInFileSystem = className;
				}
			}
			
			String classFileName = classNameInFileSystem+".class";
			
            // Need multiple loaders, one for each directory of out/
			InputStream is = getOrCreateClassLoader(inPath + loader_name).getResourceAsStream(classFileName);
			if (is==null) {
				if (verbose) {
					if (isGeneratedClass)
						System.err.println("WARNING: Cannot find GENERATED class "+classNameInFileSystem+". Will use original class "+className+" instead.");
					else
						System.err.println("WARNING: Cannot find class "+classNameInFileSystem+". Will use original class instead.");
				}
				// Leave bytecodes unchanged
				return null;
			} else {
				try {
					// Read the class file from disk
					ByteArrayOutputStream bos = new ByteArrayOutputStream(is.available());
					int bytesRead;
					byte[] buffer = new byte[is.available()]; 
					while((bytesRead=is.read(buffer))>-1) {
						bos.write(buffer,0,bytesRead);
					}
					byte[] readBytes = bos.toByteArray();
                    
                    /*
                     * If the class is generated, replace
                     * 1. The hashed name of the class itself by className, the name that the context expects
                     * 2. The hashed names of all other referenced generated classes by the actual names that the context provides
                     */
					if (isGeneratedClass) {
						String refOrig = nameOfGeneratedClassReferenced(className, originalBytes);
						String refHashed = nameOfGeneratedClassReferenced(classNameInFileSystem, readBytes);
						
						Map<String,String> fromTo = new HashMap<String, String>();
						// Rename declaring class
						fromTo.put(classNameInFileSystem, className);
						// Rename referenced class, if any 
						if (refOrig!=null) {
							assert refHashed!=null : "Class "+refOrig+" references "+refOrig+" but hashed class "+classNameInFileSystem+" has no references!?";
							fromTo.put(refHashed,refOrig);
						}
						
						synchronized (this) {
							readBytes = ClassRenamer.replaceClassNamesInBytes(fromTo, readBytes);
						}
					}
					
                    synchronized (this) {
					    numSuccess++;
                    }
					return readBytes;
				} finally {
					is.close();
				}
			}
		} catch (Exception e) {
			//print the exception before we re-throw it because otherwise the transformation
			//framework may just swallow it
			RuntimeException e2 = new RuntimeException("Exception in class dumper",e);
			e2.printStackTrace();
			throw e2;
		}					
	}
	
	/**
	 * Returns a class loader that loads classes <i>only</i> from the
	 * provided <code>srcPath</code>. This path has to be in standard classpath
	 * format, separated by {@link File#pathSeparator}. Note that this class loader
	 * does <i>not</i> delegate! 
	 * @param srcPath The path to load from.
	 * @return the class loader
	 */
	private URLClassLoader getOrCreateClassLoader(String srcPath) {
        if (pathToClassLoader.containsKey(srcPath)) {
            return pathToClassLoader.get(srcPath);
        }
		String[] pathSegments = srcPath.split(File.pathSeparator);
		URL[] urls = new URL[pathSegments.length];
		int i=0;
		for (String segment : pathSegments) {
			try {
				urls[i++] = new File(segment).toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
        URLClassLoader cl = 
            new URLClassLoader(urls, null) {
                @Override
                public URL getResource(String name) {
                    return findResource(name);
                }
            };
        pathToClassLoader.put(srcPath, cl);
        return cl;
	}

	/**
	 * Stores a mapping from <code>className</code> to a copy of <code>classfileBuffer</code> into
	 * {@link #generatedClassNameToOriginalBytes}. In cases where this map already holds a mapping
	 * for <code>className</code> and this mapping points to an array with different contents, then
	 * this method issues a warning. The warning is only issued if {@link #verbose} is true.
	 */
	private synchronized void storeClassBytesOfGeneratedClass(final String className, byte[] classfileBuffer) {
		if(generatedClassNameToOriginalBytes.containsKey(className)) {
            if (verbose && !Arrays.equals(classfileBuffer, generatedClassNameToOriginalBytes.get(className)))
				System.err.println("WARNING: There exist two different GENERATED classes with name "+className);
		} else {
			byte[] copy = new byte[classfileBuffer.length];
			System.arraycopy(classfileBuffer, 0, copy, 0, classfileBuffer.length);
			generatedClassNameToOriginalBytes.put(className, copy);
		}
	}



}

