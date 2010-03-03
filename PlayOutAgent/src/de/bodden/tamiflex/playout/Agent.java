package de.bodden.tamiflex.playout;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

import de.bodden.tamiflex.playout.rt.ReflLogger;
import de.bodden.tamiflex.playout.rt.ShutdownStatus;



public class Agent {
	
	public final static String PKGNAME = Agent.class.getPackage().getName().replace('.', '/');
	private static ClassDumper classDumper;
	
	public static void premain(String agentArgs, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException, URISyntaxException {
		if(!inst.isRetransformClassesSupported()) {
			throw new RuntimeException("retransformation not supported");
		}
		
		if(agentArgs==null||agentArgs.isEmpty()) usage();

		List<String> args = new ArrayList<String>(Arrays.asList(agentArgs.split(",")));
		boolean count = args.remove("count");
		
		String outPath = null;
		for (String arg : args) {
			if(arg.startsWith("outpath=")) {
				outPath = arg.substring("outpath=".length());
				break;
			}
		}
		if(outPath==null) {
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
		
		File logFile = new File(outDir,"refl.log");
		
		appendRtJarToBootClassPath(inst);
		
		dumpLoadedClasses(inst,outDir);
		
		instrumentClassesForLogging(inst, logFile, count);
		
		inst.addTransformer(classDumper,true /* can retransform */);		
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			@Override
			public void run() {
				ShutdownStatus.hasShutDown = true;
				classDumper.writeClassesToDisk();
				ReflLogger.writeLogfileToDisk();
			}
			
		});
}

	private static void dumpLoadedClasses(Instrumentation inst, File outDir)
			throws UnmodifiableClassException {
		classDumper = new ClassDumper(outDir);
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

	private static void instrumentClassesForLogging(Instrumentation inst,
			File logFile, boolean count) throws UnmodifiableClassException {
		ReflectionMonitor reflMonitor = new ReflectionMonitor(logFile,count);
		inst.addTransformer(reflMonitor, true /* can retransform */);				

		//make sure that these classes are instrumented
		inst.retransformClasses(Class.class,Method.class,Constructor.class);
		
		//remove transformer again
		inst.removeTransformer(reflMonitor);
	}

	private static void appendRtJarToBootClassPath(Instrumentation inst) throws URISyntaxException, IOException {
		URL locationOfAgent = Agent.class.getResource("/de/bodden/refllogger/agent/ReflLogger.class");
		if(locationOfAgent==null) {
			System.err.println("Support library for reflection log not found on classpath.");
			System.exit(1);
		}
		String agentJarFile = locationOfAgent.getPath().substring(0, locationOfAgent.getPath().indexOf("!"));		
		URI uri = new URI(agentJarFile);
		JarFile jarFile = new JarFile(new File(uri));
		inst.appendToBootstrapClassLoaderSearch(jarFile);
	}

	private static void usage() {
		System.out.println("This agent accepts the following options:");
		System.out.println("[count,]outpath=<path>");
		System.out.println();
		System.out.println("If 'count' is selected then the agent will add the number of reflective invocations");
		System.out.println("to the end of each line of the trace file.");
		System.out.println("The 'outpath' points to the output directory. The agent will write all class files");
		System.out.println("into this directory. In addition, the agent will write a log file 'refl.log'. If this");
		System.out.println("file already exists in the 'outpath' directory then the agent will add to the log,");
		System.out.println("incrementing the respective counts, etc.");
		System.out.println("");
		System.out.println("For instance, the following command will cause the agent to dump class files into");
		System.out.println("the directory /tmp/out, counting reflective invocations:");
		System.out.println("java -javaagent:agent.jar=count,outpath=/tmp/out ...");
		System.exit(1);
	}


}
