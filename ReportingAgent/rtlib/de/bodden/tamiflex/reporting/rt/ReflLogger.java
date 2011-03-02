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
package de.bodden.tamiflex.reporting.rt;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class ReflLogger {
	
	private static PrintWriter logger;
	private static File logFile;
	
	private static class ThreadLocalState {
		List<Entry> logEntries = new LinkedList<Entry>();
		int stackDepth = 0;
	}
	
	private static List<ThreadLocalState> perThreadStates = Collections.synchronizedList(new LinkedList<ReflLogger.ThreadLocalState>());

	private static ThreadLocal<ThreadLocalState> threadLocalState = new ThreadLocal<ReflLogger.ThreadLocalState>() {
		protected ThreadLocalState initialValue() {
			ThreadLocalState state = new ThreadLocalState();
			perThreadStates.add(state);
			return state;
		}
	};
	
	public static void classNewInstance(boolean entering, Class<?> c) {
		StackTraceElement frame = getInvokingFrame();
		log(entering, frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassNewInstance,c.getName());
	}

	private static void log(boolean entering, Object... toPrint) {
		ThreadLocalState state = threadLocalState.get();
		List<Entry> entries = state.logEntries;

		if(entering) {
			Entry entry = new Entry(entries.size(),state.stackDepth,flatten(toPrint));
			entries.add(entry);
		} else {
			Entry entry = new Entry(entries.size(),state.stackDepth,flatten(toPrint));
			boolean found = false;
			for(int i = entries.size()-1; i>=0; i--) {
				Entry other = entries.get(i);
				if(entry.matchesEarlierEntry(other)) {
					other.markAsSucceeded();
					found = true;
					break;
				} else {
					if(other.successUnknown())
						other.markAsFailed();
				}				
			}
			if(!found) {
				throw new IllegalStateException("closing entry without matching opening entry:" +entry);
			} 
		}
		
	}

	private static String flatten(Object[] toPrint) {
		StringWriter sw = new StringWriter();
		int i = 0;
		for(Object o: toPrint) {
			sw.append(o.toString());
			if(i<toPrint.length)
				sw.append(";");
			i++;
		}
		return sw.toString();
	}

	public static void classForName(boolean entering, String typeName) {
		StackTraceElement frame = getInvokingFrame();
		log(entering,frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForName,handleArrayTypes(typeName));
	}

	public static void classForName(boolean entering, String typeName, boolean initialize, ClassLoader classLoader) {
		StackTraceElement frame = getInvokingFrame();
		String classLoaderClassName = classLoader==null ? "null" : classLoader.getClass().getName();
		log(entering,frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForNameWithClassLoader,handleArrayTypes(typeName),initialize,classLoaderClassName);
	}

	public static void constructorNewInstance(boolean entering, Constructor<?> c) {		
		StackTraceElement frame = getInvokingFrame();
		
		String paramTypes = classesToTypeNames(c.getParameterTypes());
		
		log(entering, frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ConstructorNewInstance,"void "+c.getDeclaringClass().getName()+".<init>"+paramTypes);
	}

	public static void methodInvoke(boolean entering, Object receiver, Method m) {
		Class<?> receiverClass = Modifier.isStatic(m.getModifiers()) ? m.getDeclaringClass() : receiver.getClass();
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
			
			StackTraceElement frame = getInvokingFrame();
			String paramTypes = classesToTypeNames(resolved.getParameterTypes());
			log(entering,frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.MethodInvoke,getTypeName(resolved.getReturnType())+" "+resolved.getDeclaringClass().getName()+"."+resolved.getName()+paramTypes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String classesToTypeNames(Class<?>[] params) {
		String paramTypes = "(";
		int i=0;
		for (Class<?> type : params) {
			paramTypes += getTypeName(type);
			i++;
			if(i<params.length)
				paramTypes+= ",";
		}
		return paramTypes + ")";
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
            className= Byte.class.getName();
        if("C".equals(className))
            className= Character.class.getName();
        if("D".equals(className))
            className= Double.class.getName();
        if("F".equals(className))
            className= Float.class.getName();
        if("I".equals(className))
        	className = Integer.class.getName();
        if("J".equals(className))
        	className = Long.class.getName();
        if("S".equals(className))
        	className = Short.class.getName();
        if("Z".equals(className))
        	className = Boolean.class.getName();
        if("V".equals(className))
            className= Void.class.getName();

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
		threadLocalState.get().stackDepth = stackTrace.length;
		StackTraceElement outerFrame = null;
		for (StackTraceElement frame : stackTrace) {
			String c = frame.getClassName();
			if(!c.equals(ReflLogger.class.getName())
			&& !c.equals(Class.class.getName())
			&& !c.equals(Method.class.getName())
			&& !c.equals(Constructor.class.getName())) {
			
				outerFrame = frame;
				break;
			}
		}
		return outerFrame;
	}
	
	public static synchronized void closeLogger() {
		int entriesWritten = 0;
		int threads = 0;
		for(ThreadLocalState state : perThreadStates) {
			for(Entry entry: state.logEntries) {
				if(entry.successUnknown()) entry.markAsFailed();				
				logger.println(entry);
			}
			entriesWritten += state.logEntries.size();
			threads++;
			logger.println();
		}
		
		logger.flush();
		logger.close();
		System.err.println("\n=============================================");
		System.err.println("TamiFlex Reporting Agent Version "+ReflLogger.class.getPackage().getImplementationVersion());
		System.err.println("Found "+entriesWritten+" entries in "+threads+" threads.");
		System.err.println("Log file written to: "+logFile.getAbsolutePath());
	}
	
	public static void setLogFile(File f) {
		logFile = f;
		try {
			logger = new PrintWriter(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}	
}