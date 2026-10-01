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
package de.bodden.tamiflex.playout;

import de.bodden.tamiflex.normalizer.Hasher;
import static de.bodden.tamiflex.normalizer.Hasher.isGeneratedClass;
import static de.bodden.tamiflex.normalizer.Hasher.generateHashNumber;
import static de.bodden.tamiflex.normalizer.Hasher.hashedClassNameForGeneratedClassName;
import static de.bodden.tamiflex.normalizer.Hasher.replaceGeneratedClassNamesByHashedNames;
import static de.bodden.tamiflex.playout.rt.ShutdownStatus.hasShutDown;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Comparator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import de.bodden.tamiflex.normalizer.NameExtractor;

public class ClassDumper implements ClassFileTransformer {

	protected final File outDir; 
	private static final String ASM_PKGNAME = ClassVisitor.class.getPackage().getName().replace('.', '/');
	private static final String NORMALIZER_PKGNAME = Hasher.class.getPackage().getName().replace('.', '/');
	
	/**
	 * It is important that this be a <i>linked</i> hash map because we need to generate hash numbers
	 * for the classes in the order in which they are loaded. This is because a generated class <i>a</i> may reference
	 * other generated classes, and when determining a hash code for <i>a</i>, the hash code for those
	 * referenced classes must already have been computed.
	 */
	protected final LinkedHashMap<String,byte[]> classNameToBytes = new LinkedHashMap<String, byte[]>();

	private final boolean verbose;

	private final boolean dontReallyDump;
	
	public int newClasses;
	
	public ClassDumper(File outDir, boolean dontReallyDump, boolean verbose) {
		this.outDir = outDir;
		this.dontReallyDump = dontReallyDump;
		this.verbose = verbose;
	}

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className==null) {
			className = NameExtractor.extractName(classfileBuffer);
		}
		if(hasShutDown) return null;
		if(className.startsWith(Agent.PKGNAME)) return null;
		if(className.startsWith(NORMALIZER_PKGNAME)) return null;
        if(className.startsWith("openj9/")) return null;
		
		byte[] oldBytes;

        // Instrument bytebuddy RandomString to prevent randomized field names in generated classes
        if (className != null && className.equals("net/bytebuddy/utility/RandomString")) {
            ClassReader cr = new ClassReader(classfileBuffer);

            ClassWriter cw = new ClassWriter(cr, 0);

            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                    if ("nextString".equals(name) && "()Ljava/lang/String;".equals(descriptor)) {
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode == Opcodes.ARETURN) {
                                    super.visitInsn(Opcodes.POP);
                                    super.visitLdcInsn("TAMIFLEX");
                                }
                                super.visitInsn(opcode);
                            };
                        };
                    }
                    return mv;
                }
            };

            cr.accept(cv, 0);
            byte[] modifiedBytes = cw.toByteArray();

            synchronized (this) {
                String loaderName = "null_loader";
                if (loader != null) {
                    loaderName = loader.getClass().getName();
                }
                classNameToBytes.put(loaderName + " " + className, modifiedBytes);
            }

            return modifiedBytes;
        }

        // Synchronization is necessary as a single static instance of ClassDumper is maintained in Agent.java
        // This instance is passed for class file transformations(which could occur in multiple threads simultaneously)
		synchronized (this) {
            String loaderName = "null_loader";
            if (loader != null) {
                loaderName = loader.getClass().getName();
            }
            oldBytes = classNameToBytes.put(loaderName + " " + className, classfileBuffer);
		}

		if(verbose && oldBytes!=null && !Arrays.equals(classfileBuffer, oldBytes)) {
			System.err.println("WARNING: There exist two different classes with name "+className);
		}

        // We are only interested in reading the class file, no intention of modifying them
		return null;
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
	
	public void writeClassesToDisk() {
        // Synchronization is necessary as the static instance of ClassDumper in Agent.java calls writeClassesToDisk() as part
        // of it's shutdown hook, which could(I think not) run parallely with any call to transform() which can modify classNameToBytes
		synchronized (this) {
			Set<Entry<String, byte[]>> entrySet = classNameToBytes.entrySet();
			for (Map.Entry<String, byte[]> entry: entrySet) {
				String className = entry.getKey();
				byte[] classfileBuffer = entry.getValue();

                int idx = className.indexOf(' ');
                String loaderName = className.substring(0, idx);
                className = className.substring(idx + 1);
		
				if (isGeneratedClass(className)) {
                    // This normalization is needed because Proxy classes don't have consistent constantpool and method ordering
                    classfileBuffer = normalizeClass(classfileBuffer);
					generateHashNumber(className, classfileBuffer);
					className = hashedClassNameForGeneratedClassName(className);
					classfileBuffer = replaceGeneratedClassNamesByHashedNames(classfileBuffer);
				}
	
				if (dontReallyDump) continue; //don't dump
				
				File localOutDir = new File(outDir, loaderName);
				
				localOutDir.mkdirs();
				
				String simpleName = className;
				
				if(className.contains("/")) {
					String packageName = className.substring(0,className.lastIndexOf('/'));
					simpleName = className.substring(className.lastIndexOf('/')+1);
	
					localOutDir = new File(localOutDir,packageName);
					localOutDir.mkdirs();
				}
				
				String fileName = simpleName+".class";
				
				File outFile = new File(localOutDir, fileName);
				if(outFile.exists()) {
					outFile.delete();
				} else {
					newClasses++;
				}
				FileOutputStream fos = null;
				try {
					outFile.createNewFile();
					fos = new FileOutputStream(outFile);
					fos.write(classfileBuffer);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if(fos!=null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

}
