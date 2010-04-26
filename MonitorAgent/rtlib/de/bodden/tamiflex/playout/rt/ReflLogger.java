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
//import de.bodden.tamiflex.playout.*;
import static de.bodden.tamiflex.playout.rt.ShutdownStatus.hasShutDown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
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
import java.util.Map.Entry;

import javax.swing.text.html.HTMLDocument.Iterator;

import de.bodden.tamiflex.playout.Filter;
import de.bodden.tamiflex.playout.ScreenCapture;


public class ReflLogger {
	
	
	//holds hashed names
	protected static Map<PersistedLogEntry,PersistedLogEntry> oldContainerMethodToEntries = new HashMap<PersistedLogEntry,PersistedLogEntry>();

	//holds actual names
	protected static Map<String,Map<RuntimeLogEntry,RuntimeLogEntry>> containerMethodToEntries = new HashMap<String, Map<RuntimeLogEntry,RuntimeLogEntry>>();
	
	//is initialized by the agent
	private static File logFile;
	
	//is initialized by the agent
	private static boolean doCount;
	
	private static ScreenCapture screenCapture;
	
	private static Filter filter;
	
	//is initialized by the agent
	private static PrintWriter newLineWriter = new PrintWriter(new OutputStream() {
		
		@Override
		public void write(int b) throws IOException {
			//by default, do nothing
		}
	});
	
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

	private static void logAndIncrementTargetMethodEntry(String containerMethod, int lineNumber, Kind kind, String declaringClass, String returnType, String name, String... paramTypes) {
		if(hasShutDown) return;
		TargetMethodLogEntry newEntry = new TargetMethodLogEntry(containerMethod, lineNumber, kind, declaringClass, returnType, name, paramTypes);
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
		
		if(previousUserEvent!=null) {
			sameEntry.setUserEventJustBefore(previousUserEvent);
			previousUserEvent=null;
		}
		return sameEntry;
	}
	
	public static String previousUserEvent;

	public static void classNewInstance(Class<?> c) {
		StackTraceElement frame = getInvokingFrame();
		logAndIncrementTargetClassEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassNewInstance,c.getName());
	}

	public static void classForName(String typeName) {
		StackTraceElement frame = getInvokingFrame();
		logAndIncrementTargetClassEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ClassForName,handleArrayTypes(typeName));
	}

	public static void constructorNewInstance(Constructor<?> c) {		
		StackTraceElement frame = getInvokingFrame();
		
		String[] paramTypes = classesToTypeNames(c.getParameterTypes());
		
		logAndIncrementTargetMethodEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.ConstructorNewInstance,c.getDeclaringClass().getName(),"void","<init>",paramTypes);
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
	
	public static void methodInvoke(Object receiver, Method m) {
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
			String[] paramTypes = classesToTypeNames(resolved.getParameterTypes());
			logAndIncrementTargetMethodEntry(frame.getClassName()+"."+frame.getMethodName(),frame.getLineNumber(),Kind.MethodInvoke,resolved.getDeclaringClass().getName(),getTypeName(resolved.getReturnType()),resolved.getName(),paramTypes);
		} catch (Exception e) {
			e.printStackTrace();
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
	
	
	public static synchronized void writeLogfileToDisk(boolean verbose) {
		Set<RuntimeLogEntry> newLogSet = new HashSet<RuntimeLogEntry>();
		for(Map<RuntimeLogEntry,RuntimeLogEntry> values: containerMethodToEntries.values()) {
			newLogSet.addAll(values.keySet());
		}
		
		List<RuntimeLogEntry> list = new ArrayList<RuntimeLogEntry>(newLogSet);
		Collections.sort(list);
		HashMap<String, Map<String, Set<String>>> threadTosourceToTargets = new HashMap<String, Map<String, Set<String>>>();
		
		for (RuntimeLogEntry entry : list) {
			PersistedLogEntry persistedEntry = entry.toPersistedEntry();
			String target = persistedEntry.getTargetClassOrMethod(); 
			String thread = persistedEntry.getThreadName();
			String source = persistedEntry.getContainerMethod() + ":"+ persistedEntry.getLineNumber();
			
			Map<String, Set<String>> threadToTargets = threadTosourceToTargets.get(source);
			if(threadToTargets==null) {
				threadToTargets = new HashMap<String,Set<String>>();
				threadTosourceToTargets.put(source, threadToTargets);				
			}
			Set<String> targets = threadToTargets.get(thread);
			if(targets==null) {
				targets = new HashSet<String>();
				threadToTargets.put(thread, targets);
			}
			targets.add(target);
		}
		
		for (Entry<String,Map<String, Set<String>>> entry : threadTosourceToTargets.entrySet()) {
			String source = entry.getKey();
			Map<String, Set<String>> threadToTargets = entry.getValue();
			System.err.println("Source location: "+source);
			for(Entry<String,Set<String>> innerEntry: threadToTargets.entrySet()) {
				String thread = innerEntry.getKey();
				System.err.println("    Thread "+thread+" calls:");
				Set<String> targets = innerEntry.getValue();
				for (String target : targets) {
					System.err.println("        "+target);
				}
			}
			
			
		}
		
HashMap<String, Map<String, Set<String>>> threadToSourceToTargets = new HashMap<String, Map<String, Set<String>>>();
		
		for (RuntimeLogEntry entry : list) {
			PersistedLogEntry persistedEntry = entry.toPersistedEntry();
			String target = persistedEntry.getTargetClassOrMethod(); 
			String thread = persistedEntry.getThreadName();
			String source = persistedEntry.getContainerMethod() + ":"+ persistedEntry.getLineNumber();
			
			Map<String, Set<String>> sourceToTargets = threadToSourceToTargets.get(thread);
			if(sourceToTargets==null) {
				sourceToTargets = new HashMap<String,Set<String>>();
				threadTosourceToTargets.put(thread, sourceToTargets);				
			}
			Set<String> targets = sourceToTargets.get(source);
			if(targets==null) {
				targets = new HashSet<String>();
				sourceToTargets.put(source, targets);
			}
			targets.add(target);
		}
		
		for (Entry<String,Map<String, Set<String>>> entry : threadToSourceToTargets.entrySet()) {
			String thread = entry.getKey();
			Map<String, Set<String>> sourceToTargets = entry.getValue();
			System.err.println("Thread : "+thread +" calls:");
			for(Entry<String,Set<String>> innerEntry: sourceToTargets.entrySet()) {
				String source = innerEntry.getKey();
				System.err.println("    source "+source);
				Set<String> targets = innerEntry.getValue();
				for (String target : targets) {
					System.err.println("        "+target);
				}
			}
			
		}
		
		
		
//		//printStatistics();
//		try {
//			//Set set = map.entrySet();
//			Iterator i = (Iterator) source.iterator();//
//			PrintWriter pw = new PrintWriter(logFile);
//			List<String> lines = new ArrayList<String>();
//			
//			String lastName=null;
//			
//			Set<String> fileNames = new HashSet<String>();
//			
//			for (RuntimeLogEntry entry : list) {
//				String fileName = screenCapture.getFileName(entry.getTime());
//				if(fileName!=null && !fileName.equals(lastName)) {
//					lines.add("# "+fileName);
//					fileNames.add(fileName);				
//				}
//				if(entry.getUserEventJustBefore()!=null) {
//					lines.add(entry.getUserEventJustBefore());
//				}
//				lines.add(entry.toString());
//				
//				//desplay alle the hashmap element
//				while(i.isValid()){
//					//map.entrySet() Entry setE = i.;
//					//lines.add(map.get(target)+ " : " +source+"\n");
//					//lines.add(i.next()); // added
//					printKeys(sourceToTargets);
//				    }
//				lastName = fileName;
//			}
//			
//			for (String line : lines) {
//				pw.println(line);
//			}
//			File outDir = logFile.getParentFile();
//			Filter only = new Filter(fileNames);	
//			for(File f: outDir.listFiles(only))
//			{
//				f.delete();
//			}
//		
//			//screenCapture.getFileName(entry.getTime());
//		//lines.add(globalImageName);
//		//pw.println("lines");
//			pw.flush();
//			pw.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			
//		}		
	}
	
	
	private static void printKeys(HashMap<String, Set<String>> m) {
		// TODO Auto-generated method stub
		System.out.print("Size = " + m.size() +", ");
		System.out.print("Keys: ");
		System.out.println(m.keySet());

		
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
				// I made some change identify the different parts of the log file lines (split etc)
				fis = new FileInputStream(f);
				reader = new BufferedReader(new InputStreamReader(fis));
				String line;
				while((line=reader.readLine())!=null) {
					String[] split = line.split(";",-1);
					Kind kind = Kind.kindForLabel(split[0]);
					String target = split[1];
					String threadName=split[2]; //
					String containerMethod = split[3]; //
					int lineNumber = split[4].isEmpty()?-1:Integer.parseInt(split[4]);//
					int count = (split.length<6||split[5].isEmpty()||!doCount)?0:Integer.parseInt(split[5]); //
					PersistedLogEntry entry = new PersistedLogEntry(containerMethod, lineNumber, kind, target,threadName, count);
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
		
		return merged;
	}

	public static void setScreenCapture(ScreenCapture target) {
		screenCapture = target;
	}

}