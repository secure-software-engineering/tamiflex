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

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import de.bodden.tamiflex.normalizer.Hasher;
import de.bodden.tamiflex.playout.rt.ReflLogger;
import de.bodden.tamiflex.playout.rt.ShutdownStatus;

public class Agent {
	
	public final static String PKGNAME = Agent.class.getPackage().getName().replace('.', '/');
	private static ClassDumper classDumper;
	private static Socket socket;
	
	public static void premain(String agentArgs, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException, URISyntaxException, InterruptedException {
		if(!inst.isRetransformClassesSupported()) {
			throw new RuntimeException("retransformation not supported");
		}
		
		if(agentArgs==null) agentArgs = "";
		boolean dontDump = false;
		if(agentArgs.startsWith("dontDumpClasses,")) {
			dontDump = true;
			agentArgs = agentArgs.substring("dontDumpClasses,".length());
		}
		boolean dontNormalize = false;
		if(agentArgs.startsWith("dontNormalize,")) {
			dontNormalize = true;
			agentArgs = agentArgs.substring("dontNormalize,".length());
		}
		boolean count = false;
		if(agentArgs.startsWith("count,")) {
			count = true;
			agentArgs = agentArgs.substring("count,".length());
		}
		boolean verbose = false;
		if(agentArgs.startsWith("verbose,")) {
			verbose = true;
			agentArgs = agentArgs.substring("verbose,".length());
		}
		boolean useSocket = false;
		if(agentArgs.startsWith("socket,")) {
			useSocket = true;
			agentArgs = agentArgs.substring("socket,".length());
		}
		if(agentArgs.equals("")) usage();
		
		appendRtJarToBootClassPath(inst);

		ReflLogger.setMustCount(count);		
		if(dontNormalize) Hasher.dontNormalize();

		if(useSocket) {
			//online mode; no need to create any files; just insert instrumentation...
			String hostColonPort = agentArgs;
			if(!hostColonPort.contains(":")) throw new IllegalArgumentException("Wrong destination "+hostColonPort+" ! Format is host:port.");
			String[] split = hostColonPort.split(":");
			String host = split[0];
			int port = Integer.parseInt(split[1]);
			socket = new Socket(host, port);
			ReflLogger.setSocket(socket);
			instrumentClassesForLogging(inst);
		} else {
			String outPath=agentArgs;
			if(outPath==null||outPath.isEmpty()) {
				System.err.println("No outpath given!");
				usage();
			}
			
			File outDir = new File(outPath);
			if(outDir.exists()) {
				if(!outDir.isDirectory()) {
					System.err.println(outDir+ "is not a directory");
					usage();
				}
			} else {
				boolean res = outDir.mkdirs();
				if(!res) {
					System.err.println("Cannot create directory "+outDir);
					usage();
				}
			}
			
			final File logFile = new File(outDir,"refl.log");
			
			dumpLoadedClasses(inst,outDir,dontDump,verbose);
			
			ReflLogger.setLogFile(logFile);
			
			instrumentClassesForLogging(inst);
			
			inst.addTransformer(classDumper,true /* can retransform */);		
			
			final boolean verboseOutput = verbose;
			Runtime.getRuntime().addShutdownHook(new Thread() {
				
				@Override
				public void run() {
					ShutdownStatus.hasShutDown = true;
					if(socket!=null) {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					classDumper.writeClassesToDisk();
					ReflLogger.writeLogfileToDisk(verboseOutput);
					
					String agentJarDir = agentJarFilePath.substring(0, agentJarFilePath.lastIndexOf('/'));
					String version = Agent.class.getPackage().getImplementationVersion();
					String dbJarPath = agentJarDir+'/'+"dbdumper-"+version+".jar";
					
					try {
						File jarfile = new File(new URI(dbJarPath));
						if(jarfile.exists()) {
							System.out.println("Database JAR file found. Will attempt to dump log file to database.");
							DBDumper.dumpFileToDatabase(jarfile,logFile);
						} 
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
				
			});
		}
}

	private static void dumpLoadedClasses(Instrumentation inst, File outDir, boolean dontReallyDump, boolean verbose)
			throws UnmodifiableClassException {
		classDumper = new ClassDumper(outDir,dontReallyDump,verbose);
		inst.addTransformer(classDumper,true /* can retransform */);			
		//dump all classes that are already loaded
		for (Class<?> c : inst.getAllLoadedClasses()) {
			if(inst.isModifiableClass(c)) {
				inst.retransformClasses(c);
			} else {
				if(!c.isPrimitive() && !c.isArray() && (c.getPackage()==null || !c.getPackage().getName().startsWith("java.lang"))){
					System.err.println("WARNING: Cannot dump class "+c.getName());
				}
			}
		}
		inst.removeTransformer(classDumper);
	}

	private static void instrumentClassesForLogging(Instrumentation inst) throws UnmodifiableClassException {
		ReflectionMonitor reflMonitor = new ReflectionMonitor();
		inst.addTransformer(reflMonitor, true /* can retransform */);				

		//make sure that these classes are instrumented
		inst.retransformClasses(Class.class,Method.class,Constructor.class);
		
		//remove transformer again
		inst.removeTransformer(reflMonitor);
	}

	private static void appendRtJarToBootClassPath(Instrumentation inst) throws URISyntaxException, IOException {
		URL locationOfAgent = Agent.class.getResource("/de/bodden/tamiflex/playout/rt/ReflLogger.class");
		if(locationOfAgent==null) {
			System.err.println("Support library for reflection log not found on classpath.");
			System.exit(1);
		}
		agentJarFilePath = locationOfAgent.getPath().substring(0, locationOfAgent.getPath().indexOf("!"));		
		URI uri = new URI(agentJarFilePath);
		JarFile jarFile = new JarFile(new File(uri));
		inst.appendToBootstrapClassLoaderSearch(jarFile);
	}

	private static void usage() {
		System.out.println("TamiFlex version "+Agent.class.getPackage().getImplementationVersion()+", Play-Out Agent \n");
		System.out.println("This agent accepts the following options:");
		System.out.println("[dontDumpClasses,][dontNormalize,][count,][verbose,]<path>");
		System.out.println();
		System.out.println("If 'dontDumpClasses' is given then the agent only produces a log file but dumps no classes.");
		System.out.println("If 'dontNormalize' is given then the agent will not normalize randomized class names.");
		System.out.println("If 'count' is selected then the agent will add the number of reflective invocations");
		System.out.println("to the end of each line of the trace file.");
		System.out.println("If 'verbose' is selected then the agent will print out all entries that it also added");
		System.out.println("to the log file for the current run.");
		System.out.println("");
		System.out.println("The 'path' points to the output directory. The agent will write all class files");
		System.out.println("into this directory. In addition, the agent will write a log file 'refl.log'. If this");
		System.out.println("file already exists in the 'outpath' directory then the agent will add to the log,");
		System.out.println("incrementing the respective counts, etc.");
		System.out.println("");
		System.out.println("");
		System.out.println("For instance, the following command will cause the agent to dump class files into");
		System.out.println("the directory /tmp/out, counting reflective invocations:");
		System.out.println("java -javaagent:agent.jar=count,outpath=/tmp/out ...");
		System.out.println(DISCLAIMER);
		System.exit(1);
	}
	
	private final static String DISCLAIMER=
		"Copyright (c) 2010 Eric Bodden.\n" +
		"\n" +
		"DISCLAIMER: USE OF THIS SOFTWARE IS AT OWN RISK.\n" +
		"\n" +
		"All rights reserved. This program and the accompanying materials\n" +
		"are made available under the terms of the Eclipse Public License v1.0\n" +
		"which accompanies this distribution, and is available at\n" +
		"http://www.eclipse.org/legal/epl-v10.html";
	private static String agentJarFilePath;

}
