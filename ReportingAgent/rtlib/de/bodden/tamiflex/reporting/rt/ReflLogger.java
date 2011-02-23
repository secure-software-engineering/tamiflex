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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public class ReflLogger {
	
	private static PrintWriter logger;
	private static File logFile;
	private static int entriesWritten = 0;

	public static void classNewInstance(boolean entering, Class<?> c) {
		StackTraceElement frame = getInvokingFrame();
		log(entering, frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassNewInstance,c.getName());
	}

	private static void log(boolean entering, Object... toPrint) {
		logAttemptOrSuccess(entering);
		
		logCurrentThread();

		int i = 0;
		for (Object object : toPrint) {
			logger.print(object);
			if(i<toPrint.length) {
				logger.print(";");
			}						
			i++;
		}
		logger.println();
		entriesWritten++;
	}

	private static void logAttemptOrSuccess(boolean entering) {
		String prefix = entering ? "ATTEMPT" : "SUCCESS";
		logger.print(prefix);
		logger.print(";");
	}

	private static void logCurrentThread() {
		logger.print(Thread.currentThread().getId());
		logger.print("-");
		logger.print(Thread.currentThread().getName());
		logger.print(";");
	}

	public static void classForName(boolean entering, String typeName) {
		StackTraceElement frame = getInvokingFrame();
		log(entering,frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForName,handleArrayTypes(typeName));
	}

	public static void classForNameWithClassLoader(boolean entering, String typeName) {
		StackTraceElement frame = getInvokingFrame();
		log(entering,frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForNameWithClassLoader,handleArrayTypes(typeName));
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
		logger.flush();
		logger.close();
		System.err.println("\n=============================================");
		System.err.println("TamiFlex Reporting Agent Version "+ReflLogger.class.getPackage().getImplementationVersion());
		System.err.println("Found "+entriesWritten+" entries.");
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