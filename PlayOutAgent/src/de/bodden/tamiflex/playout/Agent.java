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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

import de.bodden.tamiflex.normalizer.Hasher;
import de.bodden.tamiflex.playout.rt.ReflLogger;
import de.bodden.tamiflex.playout.rt.ShutdownStatus;

public class Agent {
	
	public final static String PKGNAME = Agent.class.getPackage().getName().replace('.', '/');
	
	private static final boolean CAN_RETRANSFORM = true;
	
	private static ClassDumper classDumper;
	private static boolean dontDump = false;
	private static boolean dontNormalize = false;
	private static boolean count = false;
	private static boolean useDeclaredTypes;
	private static boolean verbose = false;
	private static boolean useSocket = false;
	private static String socketString = null;
	private static String outPath = "out";
	private static String transformations = "";
	private static Socket socket;

	
	public static void premain(String agentArgs, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException, URISyntaxException, InterruptedException {
		if(!inst.isRetransformClassesSupported()) {
			throw new RuntimeException("retransformation not supported");
		}
		
		System.out.println("============================================================");
		System.out.println("TamiFlex Play-Out Agent Version "+Agent.class.getPackage().getImplementationVersion());

		loadProperties();		
		
		appendRtJarToBootClassPath(inst);

		ReflLogger.setMustCount(count);		
		ReflLogger.setuseDeclaredTypes(useDeclaredTypes);		
		if(dontNormalize) Hasher.dontNormalize();

		if(useSocket) {
			//online mode; no need to create any files; just insert instrumentation...
			String hostColonPort = socketString;
			if(!hostColonPort.contains(":")) throw new IllegalArgumentException("Wrong destination "+hostColonPort+" ! Format is host:port.");
			String[] split = hostColonPort.split(":");
			String host = split[0];
			int port = Integer.parseInt(split[1]);
			socket = new Socket(host, port);
			ReflLogger.setSocket(socket);
			instrumentClassesForLogging(inst);
		} else {
			if(outPath==null||outPath.isEmpty()) {
				System.err.println("No outDir given!");
			}
			
			File outDir = new File(outPath);
			if(outDir.exists()) {
				if(!outDir.isDirectory()) {
					System.err.println(outDir+ "is not a directory");
					System.exit(1);
				}
			} else {
				boolean res = outDir.mkdirs();
				if(!res) {
					System.err.println("Cannot create directory "+outDir);
					System.exit(1);
				}
			}
			
			final File logFile = new File(outDir,"refl.log");
			
			dumpLoadedClasses(inst,outDir,dontDump,verbose);
			
			ReflLogger.setLogFile(logFile);
			
			instrumentClassesForLogging(inst);
			
			inst.addTransformer(classDumper, CAN_RETRANSFORM);
			
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
		
		System.out.println("============================================================");		
	}

	private static void loadProperties() {
		String propFileName = "poa.properties";
		String userPropFilePath = System.getProperty("user.home")+File.separator+".tamiflex"+File.separator+propFileName;
		copyPropFileIfMissing(userPropFilePath);
		String[] paths = { propFileName, userPropFilePath };
		InputStream is = null;
		File foundFile= null;
		for (String path : paths) {
			File file = new File(path);
			if(file.exists() && file.canRead()) {
				try {
					is = new FileInputStream(file);
					foundFile = file;
					break;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} 
		}
		if(is==null) throw new InternalError("No properties files found!");
		
		Properties props =  new Properties();
		try {
			props.load(is);

			if(!props.containsKey("quiet") || !props.get("quiet").equals("true")) {
				String path = (foundFile!=null) ? foundFile.getAbsolutePath() : "<JAR FILE>!/"+propFileName;
				System.out.println("Loaded properties from "+path);
			}
			if(props.containsKey("count") && props.get("count").equals("true"))
				count = true;
			if(props.containsKey("dontDumpClasses") && props.get("dontDumpClasses").equals("true"))
				dontDump = true;
			if(props.containsKey("dontNormalize") && props.get("dontNormalize").equals("true"))
				dontNormalize = true;
			if(props.containsKey("verbose") && props.get("verbose").equals("true"))
				verbose = true;
			if(props.containsKey("useDeclaredTypes") && props.get("useDeclaredTypes").equals("true"))
				useDeclaredTypes = true;
			if(props.containsKey("socket")) {
				useSocket = true;
				socketString = (String) props.get("socket");
			}
			if(props.containsKey("outDir"))
				outPath = (String) props.get("outDir"); 
			if(props.containsKey("transformations"))
				transformations = (String) props.get("transformations"); 

		} catch (IOException e) {
			throw new InternalError("Error loading default properties file: "+e.getMessage()); 
		}		
	}

	private static void copyPropFileIfMissing(String userPropFilePath) {
		File f = new File(userPropFilePath);
		if(!f.exists()) {
			File dir = f.getParentFile();
			if(!dir.exists()) dir.mkdirs();
			try {
				FileOutputStream fos = new FileOutputStream(f);
				InputStream is = Agent.class.getClassLoader().getResourceAsStream(f.getName());
				if(is==null) {
					throw new InternalError("No default properties file found in agent JAR file!");
				}
				int i;
				while((i=is.read())!=-1) {
					fos.write(i);
				}
				fos.close();
				is.close();				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void dumpLoadedClasses(Instrumentation inst, File outDir, boolean dontReallyDump, boolean verbose)
			throws UnmodifiableClassException {
		classDumper = new ClassDumper(outDir,dontReallyDump,verbose);
		inst.addTransformer(classDumper, CAN_RETRANSFORM);
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
		ReflectionMonitor reflMonitor = new ReflectionMonitor(transformations, verbose);
		inst.addTransformer(reflMonitor, CAN_RETRANSFORM);
		
		List<Class<?>> affectedClasses = reflMonitor.getAffectedClasses();
		inst.retransformClasses(affectedClasses.toArray(new Class<?>[affectedClasses.size()]));
		
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
	
	public static void main(String[] args) {
		usage();
	}

	private static void usage() {
		System.out.println("============================================================");
		System.out.println("TamiFlex Play-Out Agent Version "+Agent.class.getPackage().getImplementationVersion());
		System.out.println(DISCLAIMER);
		System.out.println("============================================================");
		System.exit(1);
	}
	
	private final static String DISCLAIMER=
		"\n\nCopyright (c) 2010-2011 Eric Bodden and others.\n" +
		"\n" +
		"DISCLAIMER: USE OF THIS SOFTWARE IS AT OWN RISK.\n" +
		"\n" +
		"All rights reserved. This program and the accompanying materials\n" +
		"are made available under the terms of the Eclipse Public License v1.0\n" +
		"which accompanies this distribution, and is available at\n" +
		"http://www.eclipse.org/legal/epl-v10.html";
	private static String agentJarFilePath;

}
