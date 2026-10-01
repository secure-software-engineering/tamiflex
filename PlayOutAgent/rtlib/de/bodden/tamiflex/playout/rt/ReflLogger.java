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
package de.bodden.tamiflex.playout.rt;
import static de.bodden.tamiflex.playout.rt.ShutdownStatus.hasShutDown;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

import static de.bodden.tamiflex.normalizer.Hasher.isGeneratedClass;
import static de.bodden.tamiflex.normalizer.Hasher.generateHashNumber;
import static de.bodden.tamiflex.normalizer.Hasher.generateHashNumberForHidden;
import static de.bodden.tamiflex.normalizer.Hasher.hashedClassNameForGeneratedClassBytes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReflLogger {
	
	//holds hashed names
	protected static Map<PersistedLogEntry,PersistedLogEntry> oldContainerMethodToEntries = new HashMap<PersistedLogEntry,PersistedLogEntry>();

	//holds actual names
	protected static Map<String,Map<RuntimeLogEntry,RuntimeLogEntry>> containerMethodToEntries = new HashMap<String, Map<RuntimeLogEntry,RuntimeLogEntry>>();

    // hidden class bytes to dump
	protected static final LinkedHashMap<String, byte[]> hiddenClassBytes = new LinkedHashMap<String, byte[]>();

	public static int newHiddenClasses = 0;
	
	//is initialized by the agent
	private static File logFile;
	
	//is initialized by the agent
	private static boolean doCount;

	//is initialized by the agent
	private static boolean useDeclaredTypes;

	//is initialized by the agent
	private static String outPath;

	//is initialized by the agent
	private static PrintWriter newLineWriter = new PrintWriter(new OutputStream() {
		
		@Override
		public void write(int b) throws IOException {
			//by default, do nothing
		}
	});
	
	/** This field is used to guard against infinite recursion during logging. */
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
	
	private static void logAndIncrementTargetClassEntry(String containerMethod, int lineNumber, Kind kind, String targetClass) {
		if(hasShutDown) return;
		TargetClassLogEntry newEntry = new TargetClassLogEntry(containerMethod, lineNumber, kind, targetClass);
		RuntimeLogEntry entry;
		synchronized (ReflLogger.class) {
			entry = pullOrCreateEntry(containerMethod, newEntry);
			if(doCount)
				entry.incrementCounter();		
		}
	}

	private static void logAndIncrementTargetMethodEntry(String containerMethod, int lineNumber, Kind kind, String declaringClass, String returnType, String name, boolean isAccessible, String... paramTypes) {
		if(hasShutDown) return;
		TargetMethodLogEntry newEntry = new TargetMethodLogEntry(containerMethod, lineNumber, kind, declaringClass, returnType, name, isAccessible, paramTypes);
		RuntimeLogEntry entry;
		synchronized (ReflLogger.class) {
			entry = pullOrCreateEntry(containerMethod, newEntry);
			if(doCount)
				entry.incrementCounter();
		}
	}
	
    private static void logAndIncrementTargetArrayEntry(String containerMethod, int lineNumber, Kind kind, String componentType, int... dimensions) {
        if(hasShutDown) return;
        TargetArrayLogEntry newEntry = new TargetArrayLogEntry(containerMethod, lineNumber, kind, componentType, dimensions);
        RuntimeLogEntry entry;
        synchronized (ReflLogger.class) {
            entry = pullOrCreateEntry(containerMethod, newEntry);
            if(doCount)
                entry.incrementCounter();
        }
    }
    
    private static void logAndIncrementTargetFieldEntry(String containerMethod, int lineNumber, Kind kind, String declaringClass, String fieldType, String name, boolean isAccessible) {
        if(hasShutDown) return;
        TargetFieldLogEntry newEntry = new TargetFieldLogEntry(containerMethod, lineNumber, kind, declaringClass, fieldType, name, isAccessible);
        RuntimeLogEntry entry;
        synchronized (ReflLogger.class) {
            entry = pullOrCreateEntry(containerMethod, newEntry);
            if(doCount)
                entry.incrementCounter();       
        }
    }

	private static RuntimeLogEntry pullOrCreateEntry(String containerMethod, RuntimeLogEntry newEntry) {
		Map<RuntimeLogEntry,RuntimeLogEntry> entries = containerMethodToEntries.get(containerMethod);
		if(entries==null) {
			entries = new HashMap<RuntimeLogEntry,RuntimeLogEntry>();
			containerMethodToEntries.put(containerMethod, entries);
		}
		RuntimeLogEntry sameEntry = entries.get(newEntry);
		if(sameEntry==null) {
			//found a new entry
			sameEntry = newEntry;
			entries.put(newEntry,newEntry);
			newLineWriter.println(newEntry.toString());
			newLineWriter.flush();			
		}
		return sameEntry;
	}

	public static void classMethodInvoke(Class<?> c, Kind classMethodKind) {
		if(isReentrant()) return;
		try {
			StackTraceElement frame = getInvokingFrame();
            String className = tryGetHiddenHashedName(c);
			logAndIncrementTargetClassEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),classMethodKind,className);
		} finally {
			leavingReflectionAPI();
		}
	}

	public static void classForName(String typeName) {
		if(isReentrant()) return;
		try {
			StackTraceElement frame = getInvokingFrame();
			logAndIncrementTargetClassEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForName,handleArrayTypes(typeName));
		} finally {
			leavingReflectionAPI();
		}
	}

	public static void constructorMethodInvoke(Constructor<?> c, Kind constructorMethodKind) {		
		if(isReentrant()) return;
		try {
			StackTraceElement frame = getInvokingFrame();
			String[] paramTypes = classesToTypeNames(c.getParameterTypes());
            String className = tryGetHiddenHashedName(c.getDeclaringClass());
			logAndIncrementTargetMethodEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),constructorMethodKind,className,"void","<init>", c.canAccess(null), paramTypes);

		} finally {
			leavingReflectionAPI();
		}
	}

	private static String[] classesToTypeNames(Class<?>[] params) {
		String[] paramTypes = new String[params.length];
		int i=0;
		for (Class<?> type : params) {
			paramTypes[i]=getTypeName(type);
			i++;
		}
		return paramTypes;
	}

    public static void writeHiddenClassesToDisk() {
        synchronized(ReflLogger.class) {
            try {
                for (Map.Entry<String, byte[]> entry : hiddenClassBytes.entrySet()) {
                    String className = entry.getKey();
                    byte[] c = entry.getValue();
                    ClassReader cr = new ClassReader(c);
                    String originalClassName = cr.getClassName();

                    int idx = className.indexOf(' ');
                    String loaderName = className.substring(0, idx);
                    className = className.substring(idx + 1);

                    final String fullHashedInternalName = className;

                    File localOutDir = new File(outPath, loaderName);
                    String packageName = "";
                    if (originalClassName.contains("/")) {
                        packageName = originalClassName.substring(0, originalClassName.lastIndexOf('/'));
                        localOutDir = new File(localOutDir, packageName);
                        localOutDir.mkdirs();
                    }

                    String hashedSimpleName = fullHashedInternalName.substring(fullHashedInternalName.lastIndexOf('/') + 1);
                    String fileName = hashedSimpleName + ".class";
                    File outFile = new File(localOutDir, fileName);

                    // Replace all references with the hashed name
                    ClassWriter cwDump = new ClassWriter(0);

                    Remapper remapper = new Remapper(Opcodes.ASM9) {
                        @Override
                        public String map(String internalName) {
                            if (internalName.equals(originalClassName)) {
                                return fullHashedInternalName;
                            }
                            return internalName;
                        }
                    };

                    ClassVisitor cvDump = new ClassRemapper(cwDump, remapper);

                    cr.accept(cvDump, 0);
                    byte[] dumpedBytes = cwDump.toByteArray();

                    if (outFile.exists()) {
                        // Compare new file with old file, if they are not the same throw an exception
                        // If they are the same keep the old file
                        byte[] existingBytes = Files.readAllBytes(outFile.toPath());
                        if (!Arrays.equals(dumpedBytes, existingBytes)) {
                            throw new Exception("FATAL: Classfile with same name has different contents on this run: " + outFile.toPath());
                        }
                    } else {
                        // Write the renamed bytes to disk
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            fos.write(dumpedBytes);
                            newHiddenClasses++;
                            // System.out.println("Dumped Lambda: " + outFile.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] processHiddenClass(byte[] c, ClassLoader loader) {
        if (isReentrant()) return c;
        byte[] byteCodeToReturn = c;
        try {
            String loader_name = "null_loader";
            if (loader != null) {
                loader_name = loader.getClass().getName();
            }

            ClassReader cr = new ClassReader(c);
            String originalClassName = cr.getClassName();

            // Ignore LambdaForm classes to converge
            if (originalClassName.startsWith("java/lang/invoke/LambdaForm$BMH") ||
                originalClassName.startsWith("java/lang/invoke/LambdaForm$DMH") ||
                originalClassName.startsWith("java/lang/invoke/LambdaForm$MH")) {
                return byteCodeToReturn;
            }

            generateHashNumberForHidden(originalClassName, c);
            String fullHashedInternalName = hashedClassNameForGeneratedClassBytes(c);

            synchronized (ReflLogger.class) {
                hiddenClassBytes.put(loader_name + " " + fullHashedInternalName, c);
            }

            // Add __TAMIFLEX_HASH to the original(non-renamed) class before returning it to generateInnerClass()
            // This is to associate java.lang.Class object with the hash
            // The hashed name is logged to refl.log
            ClassWriter cw = new ClassWriter(ClassReader.SKIP_DEBUG);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public void visitEnd() {
                    FieldVisitor fv = super.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        "__TAMIFLEX_HASH",
                        "Ljava/lang/String;",
                        null,
                        fullHashedInternalName
                    );
                    if (fv != null) {
                        fv.visitEnd();
                    }
                    super.visitEnd();
                }
            };

            cr.accept(cv, 0);
            byteCodeToReturn = cw.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            leavingReflectionAPI();
        }

        return byteCodeToReturn;
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

    public static String tryGetHiddenHashedName(Class<?> clazz) {
        String className = clazz.getName();
        if (clazz.isArray()) {
            className = clazz.getCanonicalName();
        }
        try {
            Field hashField = clazz.getDeclaredField("__TAMIFLEX_HASH");
            hashField.setAccessible(true);
            String s = (String)hashField.get(null);
            className = s;
        } catch (NoSuchFieldException e) {
            // Skip
        } catch (NoClassDefFoundError e) {
            // Skip because Class depends on libraries not present in the class path
            // Spring throws this
        } catch (Exception e) {
            e.printStackTrace();
        }
        return className;
    }
	

	public static void methodMethodInvoke(Object receiver, Method m, Kind methodKind) {
		methodMethodInvoke(receiver, m, methodKind, null);
	}

    // Resolve default implementation of a method in an interface
    private static Method findDefaultInterfaceMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        try {
            // getMethod automatically resolves the "Most Specific Interface" for public methods
            Method method = clazz.getMethod(name, paramTypes);
            if (method.isDefault()) {
                return method;
            }
        } catch (NoSuchMethodException e) {
            // Method not found
        }
        return null;
    }
	
	public static void methodMethodInvoke(Object receiver, Method m, Kind methodKind, Class<?> getMethodReceiverClass) {
		if(isReentrant()) return;
		StackTraceElement frame = getInvokingFrame();
				
		//There appears to be a call to Method.getModifiers() issued by the
		//VM in order to call the program's main method.
		//For this call there is no calling context and hence no frame.
		//We here simply ignore this call, returning early in this case.
		if(frame==null) {
			leavingReflectionAPI();
			return;		
		}
		
        // Look at POA/transformation/method/* for clear understanding, only when the reflection call made is
        // Kind.MethodInvoke for a non-static method is the "receiver" field not null and it makes sense to log
        // the accurate receiver class
		Class<?> receiverClass = methodKind!=Kind.MethodInvoke || Modifier.isStatic(m.getModifiers())
		  ? m.getDeclaringClass() : receiver.getClass();
		try {
			// Resolve virtual call to find out the class which defines the method "m"
			Method resolved = null;
			Class<?> c = receiverClass;
			do {
				try {
					resolved = c.getDeclaredMethod(m.getName(), m.getParameterTypes());
				} catch(NoSuchMethodException e) {
					c = c.getSuperclass();
				}				
			} while(resolved==null && c!=null);

            // If not found in super classes try to resolve inside interfaces
            if (resolved == null) {
                resolved = findDefaultInterfaceMethod(receiverClass, m.getName(), m.getParameterTypes());
            }
			if(resolved==null) {
                // getUnsafe is not found but resolve anyway
                if (m.toString().equals("public static sun.misc.Unsafe sun.misc.Unsafe.getUnsafe()")) {
                    resolved = m;
                } else {
                    Error error = new Error("Method not found : "+m+" in class "+receiverClass+" and super classes.");
                    error.printStackTrace();
                }
			}
			
			String[] paramTypes = classesToTypeNames(resolved.getParameterTypes());
			
			String className = resolved.getDeclaringClass().getName();
			if (useDeclaredTypes) {
				if (methodKind==Kind.MethodInvoke && !Modifier.isStatic(m.getModifiers()))
					className = receiver.getClass().getName();
				else if(methodKind==Kind.ClassGetMethod) {
					className = getMethodReceiverClass.getName();
				}
			} 

            className = tryGetHiddenHashedName(resolved.getDeclaringClass());
			
			logAndIncrementTargetMethodEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),methodKind,className,getTypeName(resolved.getReturnType()),resolved.getName(), m.isAccessible(), paramTypes);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			leavingReflectionAPI();
		}
	}
	
   public static void arrayNewInstance(Class<?> componentType, int dimension) {
		arrayMultiNewInstance(componentType, dimension);
    }
   
   public static void arrayMultiNewInstance(Class<?> componentType, int... dimensions) {
	   if(isReentrant()) return;
       try {
           StackTraceElement frame = getInvokingFrame();
           logAndIncrementTargetArrayEntry(
                   frame.getClassName()+"."+frame.getMethodName(),
                   frame.getLineNumber(),
                   Kind.ArrayNewInstance,
                   getTypeName(componentType),
                   dimensions);
       } catch (Exception e) {
           e.printStackTrace();
       } finally {
    	   leavingReflectionAPI();
       }
   }
	
	public static void fieldMethodInvoke(Field f, Kind fieldMethodKind) {
		fieldMethodInvoke(f, fieldMethodKind, null);
	}
	
	public static void fieldMethodInvoke(Field f, Kind fieldMethodKind, Class<?> getFieldReceiverClass) {
		if(isReentrant()) return;
	    try {
	        StackTraceElement frame = getInvokingFrame();
	        Class<?> fieldClass = (useDeclaredTypes && fieldMethodKind==Kind.ClassGetField) ?
	        		getFieldReceiverClass : f.getDeclaringClass();
			logAndIncrementTargetFieldEntry(
	                frame.getClassName()+"."+frame.getMethodName(),
	                frame.getLineNumber(),
	                fieldMethodKind,
	                getTypeName(fieldClass),
	                getTypeName(f.getType()),
	                f.getName(),
	                f.isAccessible());
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
			leavingReflectionAPI();
	    }	    
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

	protected static String handleArrayTypes(String className) {
        
        int arrDepth = 0;
        for(int i=0;i<className.length();i++) {
        	if(className.charAt(i)=='[') {
        		arrDepth++;
        	} else {
        		break;
        	}
        }
    	
        className = className.substring(arrDepth);
        
        if(className.endsWith(";")) {
        	//cut of leading "L" and trailing ";"
        	className = className.substring(1,className.indexOf(';'));
        }
        

        if("B".equals(className))
            className= byte.class.getName();
        if("C".equals(className))
            className= char.class.getName();
        if("D".equals(className))
            className= double.class.getName();
        if("F".equals(className))
            className= float.class.getName();
        if("I".equals(className))
        	className = int.class.getName();
        if("J".equals(className))
        	className = long.class.getName();
        if("S".equals(className))
        	className = short.class.getName();
        if("Z".equals(className))
        	className = boolean.class.getName();
        if("V".equals(className))
            className= void.class.getName();

        for(int i=0; i<arrDepth; i++) {
        	className += "[]";
        }
        
        return className;
    }

    // TODO: should we use getCanonicalName here?
	private static String getTypeName(Class<?> type) {
		//copied from java.lang.reflect.Field.getTypeName(Class)
		if (type.isArray()) {
		    try {
			Class<?> cl = type;
			int dimensions = 0;
			while (cl.isArray()) {
			    dimensions++;
			    cl = cl.getComponentType();
			}
			StringBuffer sb = new StringBuffer();
			sb.append(tryGetHiddenHashedName(cl));
			for (int i = 0; i < dimensions; i++) {
			    sb.append("[]");
			}
			return sb.toString();
		    } catch (Throwable e) { /*FALLTHRU*/ }
		}
		return tryGetHiddenHashedName(type);
	}

	/**
	 * Returns the stack frame two frames above any frame related
	 * to this class. (The frame just above is the call we are tracing,
	 * but we want to return the invoking frame, hence two frames above.)
	 */
	private static StackTraceElement getInvokingFrame() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		StackTraceElement outerFrame = null;		
		boolean outside = false;
		for (StackTraceElement frame : stackTrace) {
			String c = frame.getClassName();
			if(!outside) {
				if (!c.equals(ReflLogger.class.getName())) {
					outside = true;
					continue;
				}
			} else {
				outerFrame = frame;
				break;
			}
		}
		return outerFrame;
	}
	
	public static synchronized void writeLogfileToDisk(boolean verbose, int newClasses) {
		Set<PersistedLogEntry> mergedEntries = mergeOldAndNewLog(verbose, newClasses);
		//printStatistics();
		try {
			PrintWriter pw = new PrintWriter(logFile);

			List<String> lines = new ArrayList<String>();
			
			for (PersistedLogEntry entry : mergedEntries) {
				lines.add(entry.toString());
			}
			
			Collections.sort(lines);
			
			for (String line : lines) {
				pw.println(line);
			}
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	public static void setMustCount(boolean mustCount) {
		doCount = mustCount;		
	}
	
	public static void setLogFile(File f) {
		logFile = f;
		
		//send path of log file over Socket (if connected)
		newLineWriter.println(f.getAbsolutePath());
	}

	public static void setOutPath(String path) {
		outPath = path;
	}
	
	public static void setSocket(Socket s) throws IOException {
		newLineWriter = new PrintWriter(s.getOutputStream());
	}
	
	public static void setuseDeclaredTypes(boolean on) {
		useDeclaredTypes = on;
	}

	//is called by the agent
	private static void initializeLogFile() {
		File f = logFile;
		if(f.exists() && f.canRead()) {
			FileInputStream fis = null;
			BufferedReader reader = null;
			try {
				fis = new FileInputStream(f);
				reader = new BufferedReader(new InputStreamReader(fis));
				String line;
				while((line=reader.readLine())!=null) {
					String[] split = line.split(";",-1);
					Kind kind = Kind.kindForLabel(split[0]);
					String target = split[1];
					String containerMethod = split[2];
					int lineNumber = split[3].isEmpty()?-1:Integer.parseInt(split[3]);
					String metadata = split[4];
					int count = (split.length<6||split[5].isEmpty()||!doCount)?0:Integer.parseInt(split[5]);
					PersistedLogEntry entry = new PersistedLogEntry(containerMethod, lineNumber, kind, target, metadata, count);
					oldContainerMethodToEntries.put(entry,entry);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if(reader!=null) reader.close();
					if(fis!=null) fis.close();
				} catch (IOException e) {
				}
			}
		} 
	}
	
	private static Set<PersistedLogEntry> mergeOldAndNewLog(boolean verbose, int newClasses) {
		initializeLogFile();
		Set<RuntimeLogEntry> newLogSet = new HashSet<RuntimeLogEntry>();
		for(Map<RuntimeLogEntry,RuntimeLogEntry> values: containerMethodToEntries.values()) {
			newLogSet.addAll(values.keySet());
		}
		
		Set<PersistedLogEntry> merged = new HashSet<PersistedLogEntry>();
		
		for (RuntimeLogEntry newLogEntry : newLogSet) {
			PersistedLogEntry persistedEntry = newLogEntry.toPersistedEntry();
			PersistedLogEntry correspondingOldEntry = oldContainerMethodToEntries.get(persistedEntry);
			if(correspondingOldEntry!=null) {
				PersistedLogEntry mergedEntry = PersistedLogEntry.merge(persistedEntry, correspondingOldEntry);
				merged.add(mergedEntry);
			} else {
				merged.add(persistedEntry);
			}
		}
		
		for (PersistedLogEntry oldLogEntry : oldContainerMethodToEntries.keySet()) {
			//if no corresponding merged entry contained yet, add the old one
			if(!merged.contains(oldLogEntry)) {
				merged.add(oldLogEntry);
			}
		}
		
		Set<PersistedLogEntry> newEntries = new HashSet<PersistedLogEntry>(merged);
		newEntries.removeAll(oldContainerMethodToEntries.keySet());
		
		System.out.println("\n============================================================");
		System.out.println("TamiFlex Play-Out Agent Version "+ReflLogger.class.getPackage().getImplementationVersion());
		if(newEntries.isEmpty()) {
			System.out.println("Found no new log entries.");
		} else {
			System.out.println("Found "+newEntries.size()+" new log entries.");
		}
		if(newClasses>0) {
			System.out.println("Dumped "+newClasses+" new classes.");
		} else {
			System.out.println("Dumped no new classes.");
		}
		if(verbose) {
			System.out.println("New Entries: ");
			for (PersistedLogEntry logEntry : newEntries) {
				System.out.println(logEntry);
			}
		}
		System.out.println("Log file written to: "+logFile.getAbsolutePath());
		System.out.println("============================================================");
		
		return merged;
	}
}
