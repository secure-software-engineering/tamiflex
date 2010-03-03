package de.bodden.tamiflex.playout.rt;
import static de.bodden.tamiflex.playout.rt.ShutdownStatus.hasShutDown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
	protected static File logFile;
	
	//is initialized by the agent
	public static boolean doCount;
	
	private static synchronized void logAndIncrementTargetClassEntry(String containerMethod, int lineNumber, Kind kind, String targetClass) {
		if(hasShutDown) return;
		RuntimeLogEntry entry = pullOrCreateEntry(containerMethodToEntries,containerMethod, new TargetClassLogEntry(containerMethod, lineNumber, kind, targetClass));
		if(doCount)
			entry.incrementCounter();		
	}

	private static synchronized void logAndIncrementTargetMethodEntry(String containerMethod, int lineNumber, Kind kind, String declaringClass, String returnType, String name, String... paramTypes) {
		if(hasShutDown) return;
		RuntimeLogEntry entry = pullOrCreateEntry(containerMethodToEntries,containerMethod, new TargetMethodLogEntry(containerMethod, lineNumber, kind, declaringClass, returnType, name, paramTypes));
		if(doCount)
			entry.incrementCounter();		
	}

	private static RuntimeLogEntry pullOrCreateEntry(Map<String,Map<RuntimeLogEntry,RuntimeLogEntry>>map, String containerMethod, RuntimeLogEntry newEntry) {
		Map<RuntimeLogEntry,RuntimeLogEntry> entries = map.get(containerMethod);
		if(entries==null) {
			entries = new HashMap<RuntimeLogEntry,RuntimeLogEntry>();
			map.put(containerMethod, entries);
		}
		RuntimeLogEntry sameEntry = entries.get(newEntry);
		if(sameEntry==null) {
			sameEntry = newEntry;
			entries.put(newEntry,newEntry);
		}
		return sameEntry;
	}

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
		} catch(StackOverflowError e) {
			//ignore stack-overflow error in Tomcat
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
	
	public static synchronized void writeLogfileToDisk() {
		ShutdownStatus.hasShutDown = true;
		Set<PersistedLogEntry> mergedEntries = mergeOldAndNewLog();
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
					int count = (split.length<5||split[4].isEmpty()||!doCount)?0:Integer.parseInt(split[4]);
					PersistedLogEntry entry = new PersistedLogEntry(containerMethod, lineNumber, kind, target, count);
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
	
	private static Set<PersistedLogEntry> mergeOldAndNewLog() {
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
		
		System.err.println("New Entries: ");
		for (PersistedLogEntry logEntry : newEntries) {
			System.err.println(logEntry);
		}
		
		return merged;
	}

//	private static synchronized void printStatistics() {
//		System.out.println("======================================");
//		System.out.println();
//
//		Set<LogEntry> allEntries = new HashSet<LogEntry>();
//		{
//			Collection<Map<LogEntry, LogEntry>> entries = containerMethodToEntries.values();
//			for (Map<LogEntry, LogEntry> map : entries) {
//				Set<LogEntry> entriesForMethod = map.keySet();
//				allEntries.addAll(entriesForMethod);
//			}
//		}
//
//		//compute and print the total number of entries per kind
//		{
//			Map<Kind, Integer> kindToCount = countKinds(allEntries);
//			for(Kind k: Kind.values()) {
//				System.out.println("Total entries of kind "+k+": "+kindToCount.get(k));
//			}
//		}
//
//		System.out.println();
//				
//		//compute and print for every i the number of methods that have up to i different call sites of the same kind 
//		Set<String> namesOfCriticalMethods = new HashSet<String>(); //those are method with more than one site of the same kind
//		{
//			Map<Integer,Set<String>> maxNumLocationsOfSameKindToMethods = new HashMap<Integer, Set<String>>();
//			int globalMax = 0;
//			for(Map.Entry<String,Map<LogEntry, LogEntry>> methodEntry: containerMethodToEntries.entrySet()) {
//				String methodName = methodEntry.getKey();
//				Set<LogEntry> entries = methodEntry.getValue().keySet();
//
//				Map<Kind,Set<Integer>> kindToLineNumbers = new HashMap<Kind, Set<Integer>>();
//				
//				for (LogEntry entry : entries) {
//					Kind kind = entry.getKind();
//					Set<Integer> lineNumbers = kindToLineNumbers.get(kind);
//					if(lineNumbers==null) {
//						lineNumbers = new HashSet<Integer>();
//						kindToLineNumbers.put(kind, lineNumbers);
//					}
//					lineNumbers.add(entry.getLineNumber());
//				}
//				
//				int maxCount = 0;
//				for (Set<Integer> lineNumbers : kindToLineNumbers.values()) {
//					maxCount = Math.max(maxCount, lineNumbers.size());
//				}
//				globalMax = Math.max(globalMax, maxCount);
//				
//				Set<String> methodNameSet = maxNumLocationsOfSameKindToMethods.get(maxCount);
//				if(methodNameSet==null) {
//					methodNameSet = new HashSet<String>();
//					maxNumLocationsOfSameKindToMethods.put(maxCount, methodNameSet);
//				}
//				methodNameSet.add(methodName);
//				
//				if(maxCount>1) namesOfCriticalMethods.add(methodName);
//			}
//			for(int i=1; i<=globalMax; i++) {
//				Set<String> methodNames = maxNumLocationsOfSameKindToMethods.get(i);
//				int count = methodNames==null?0:methodNames.size();
//				if(count>0)
//					System.out.println("Number of methods with up to "+i+" different sites of the same kind: "+count);
//			}
//		}		
//		
//		System.out.println();
//		
//		{
//			Set<String> namesOfMethodsForWhichLineNumbersMatter = new HashSet<String>();
//			nextMethod:
//			for(Map.Entry<String,Map<LogEntry, LogEntry>> methodEntry: containerMethodToEntries.entrySet()) {
//				String methodName = methodEntry.getKey();
//				Set<LogEntry> entries = methodEntry.getValue().keySet();
//
//				for (LogEntry entry1 : entries) {
//					Set<String> targetsForSameKindAndLineNumber = new HashSet<String>();
//					for (LogEntry entry2 : entries) {
//						if(entry2.getLineNumber()==entry1.getLineNumber() && entry2.getKind()==entry1.getKind()) {
//							targetsForSameKindAndLineNumber.add(entry2.getTargetClassOrMethod());
//						}
//					}	
//					for (LogEntry entry2 : entries) {
//						if(entry2.getKind()==entry1.getKind() && entry2.getLineNumber()!=entry1.getLineNumber()) {
//							Set<String> targetsNotFound = new HashSet<String>(targetsForSameKindAndLineNumber);
//							for (LogEntry entry3 : entries) {
//								if(entry3.getLineNumber()==entry2.getLineNumber() && entry2.getKind()==entry2.getKind()) {
//									targetsForSameKindAndLineNumber.remove(entry3.getTargetClassOrMethod());
//									if(targetsForSameKindAndLineNumber.isEmpty()) break;
//								}
//							}
//							if(!targetsNotFound.isEmpty()) {
//								namesOfMethodsForWhichLineNumbersMatter.add(methodName);
//								continue nextMethod;
//							}
//						}
//					}
//				}
//			}
//			for (String methodName : namesOfMethodsForWhichLineNumbersMatter) {
//				System.out.println("Line numbers matter for method "+methodName);
//			}
//		}
//		
//		System.out.println();
//		System.out.println("======================================");
//	}
//
//	private static Map<Kind, Integer> countKinds(Set<LogEntry> entries) {
//		Map<Kind,Integer> kindToCount = new HashMap<Kind, Integer>();
//		for(Kind k: Kind.values()) {
//			kindToCount.put(k, 0);
//		}
//		for (LogEntry entry : entries) {
//			Kind k = entry.getKind();
//			Integer oldCount = kindToCount.get(k);
//			kindToCount.put(k,oldCount+1);			
//		}
//		return kindToCount;
//	}
}
