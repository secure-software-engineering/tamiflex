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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReflLogger {
	
	//holds hashed names
	protected static Map<PersistedLogEntry,PersistedLogEntry> oldContainerMethodToEntries = new HashMap<PersistedLogEntry,PersistedLogEntry>();

	//holds actual names
	protected static Map<String,Map<RuntimeLogEntry,RuntimeLogEntry>> containerMethodToEntries = new HashMap<String, Map<RuntimeLogEntry,RuntimeLogEntry>>();
	
	//is initialized by the agent
	private static File logFile;
	
	//is initialized by the agent
	private static boolean doCount;
	
	//is initialized by the agent
	private static PrintWriter newLineWriter = new PrintWriter(new OutputStream() {
		
		@Override
		public void write(int b) throws IOException {
			//by default, do nothing
		}
	});
	
	/** This field is used to guard against infinite recursion during logging. */
	private static ThreadLocal<Boolean> insideLogger = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};
	
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
			logAndIncrementTargetClassEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),classMethodKind,c.getName());
		} finally {
			insideLogger.set(false);
		}
	}

	public static void classForName(String typeName) {
		if(isReentrant()) return;
		try {
			StackTraceElement frame = getInvokingFrame();
			logAndIncrementTargetClassEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForName,handleArrayTypes(typeName));
		} finally {
			insideLogger.set(false);
		}
	}

	public static void constructorMethodInvoke(Constructor<?> c, Kind constructorMethodKind) {		
		if(isReentrant()) return;
		try {
			StackTraceElement frame = getInvokingFrame();
			String[] paramTypes = classesToTypeNames(c.getParameterTypes());
			logAndIncrementTargetMethodEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),constructorMethodKind,c.getDeclaringClass().getName(),"void","<init>", c.isAccessible(), paramTypes);
		} finally {
			insideLogger.set(false);
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
	
	public static void methodMethodInvoke(Object receiver, Method m, Kind methodKind) {
		if(isReentrant()) return;
		StackTraceElement frame = getInvokingFrame();
				
		//There appears to be a call to Method.getModifiers() issued by the
		//VM in order to call the program's main method.
		//For this call there is no calling context and hence no frame.
		//We here simply ignore this call, returning early in this case.
		if(frame==null) {
			insideLogger.set(false);
			return;		
		}
		
		Class<?> receiverClass = methodKind!=Kind.MethodInvoke || Modifier.isStatic(m.getModifiers())
		  ? m.getDeclaringClass() : receiver.getClass();
		try {
			//resolve virtual call
			Method resolved = null;
			Class<?> c = receiverClass;
			do {
				try {
					resolved = c.getDeclaredMethod(m.getName(), m.getParameterTypes());
				} catch(NoSuchMethodException e) {
					c = c.getSuperclass();
				}				
			} while(resolved==null && c!=null);
			if(resolved==null) {
				Error error = new Error("Method not found : "+m+" in class "+receiverClass+" and super classes.");
				error.printStackTrace();
			}
			
			String[] paramTypes = classesToTypeNames(resolved.getParameterTypes());
			logAndIncrementTargetMethodEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),methodKind,resolved.getDeclaringClass().getName(),getTypeName(resolved.getReturnType()),resolved.getName(), m.isAccessible(), paramTypes);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			insideLogger.set(false);
		}
	}
	
   public static void arrayNewInstance(Class<?> componentType, int dimension) {
        try {
            StackTraceElement frame = getInvokingFrame();
            logAndIncrementTargetArrayEntry(
                    frame.getClassName()+"."+frame.getMethodName(),
                    frame.getLineNumber(),
                    Kind.ArrayNewInstance,
                    getTypeName(componentType),
                    dimension);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
   public static void arrayMultiNewInstance(Class<?> componentType, int... dimensions) {
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
       }
   }
	
	/**
	 * This method will be invoked by Tamiflex's instrumentation whenever
	 * different calls to the class Field occur. The kind of the
	 * call is described by the fieldMethodKind.
	 */
	public static void fieldMethodInvoke(Field f, Kind fieldMethodKind) {
		if(isReentrant()) return;
	    try {
	        StackTraceElement frame = getInvokingFrame();
	        logAndIncrementTargetFieldEntry(
	                frame.getClassName()+"."+frame.getMethodName(),
	                frame.getLineNumber(),
	                fieldMethodKind,
	                getTypeName(f.getDeclaringClass()),
	                getTypeName(f.getType()),
	                f.getName(),
	                f.isAccessible());
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	    	insideLogger.set(false);
	    }	    
	}
		
    private static boolean isReentrant() {
    	boolean reentrant = insideLogger.get();
    	if(reentrant){
    		return true;
    	} else {
	    	insideLogger.set(true);
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
			sb.append(cl.getName());
			for (int i = 0; i < dimensions; i++) {
			    sb.append("[]");
			}
			return sb.toString();
		    } catch (Throwable e) { /*FALLTHRU*/ }
		}
		return type.getName();
	}

	private static StackTraceElement getInvokingFrame() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		StackTraceElement outerFrame = null;		
		for (StackTraceElement frame : stackTrace) {
			String c = frame.getClassName();
			String m = frame.getMethodName();
			//FIXME this should also refer to general Transformations instead, just as the agent does
			//here we are filtering out frames from Class, Method, etc. because we want to get the *caller* frame 			
			if(!c.equals(ReflLogger.class.getName())
			&& !(c.equals(Class.class.getName()) && m.equals("newInstance")) 	//only filter out calls from newInstance and forName,
			&& !(c.equals(Class.class.getName()) && m.equals("forName"))     	//not others for Class
			&& !(c.equals(Class.class.getName()) && m.equals("searchMethods"))	//
			&& !c.equals(Method.class.getName())
			&& !c.equals(Array.class.getName())
			&& !c.equals(Field.class.getName())
			&& !c.equals(Constructor.class.getName())) {
			
				outerFrame = frame;
				break;
			}
		}
		return outerFrame;
	}
	
	public static synchronized void writeLogfileToDisk(boolean verbose) {
		Set<PersistedLogEntry> mergedEntries = mergeOldAndNewLog(verbose);
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
	}
	
	public static void setSocket(Socket s) throws IOException {
		newLineWriter = new PrintWriter(s.getOutputStream());
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
	
	private static Set<PersistedLogEntry> mergeOldAndNewLog(boolean verbose) {
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
		
		System.err.println("\n=============================================");
		System.err.println("TamiFlex Play-Out Agent Version "+ReflLogger.class.getPackage().getImplementationVersion());
		if(newEntries.isEmpty()) {
			System.err.println("Found no new log entries.");
		} else {
			System.err.println("Found "+newEntries.size()+" new log entries.");
		}
		if(verbose) {
			System.err.println("New Entries: ");
			for (PersistedLogEntry logEntry : newEntries) {
				System.err.println(logEntry);
			}
		}
		System.err.println("Log file written to: "+logFile.getAbsolutePath());
		
		return merged;
	}
}