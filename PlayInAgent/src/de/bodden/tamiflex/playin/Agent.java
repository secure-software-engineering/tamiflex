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
package de.bodden.tamiflex.playin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;
import de.bodden.tamiflex.playin.rt.HiddenClassLoader;

import de.bodden.tamiflex.normalizer.Hasher;

/**
 * This agent registers a {@link ClassReplacer} as a class-file transformer. 
 */
public class Agent {
	
	public final static String PKGNAME = Agent.class.getPackage().getName().replace('.', '/');

	private static String inPath = "out";
	private static boolean verbose = false;
	private static String agentJarFilePath;

	public static void premain(String agentArgs, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException, URISyntaxException, IllegalClassFormatException {
		
		System.out.println("=======================================================");
		System.out.println("TamiFlex Play-In Agent Version "+Agent.class.getPackage().getImplementationVersion());
		loadProperties();
		appendRtJarToBootClassPath(inst);
        HiddenClassLoader.setInPath(inPath);
		
		final ClassReplacer replacer = new ClassReplacer(inPath,verbose);
        final HiddenClassTransformer hiddenClassTransformer = new HiddenClassTransformer();
		inst.addTransformer(replacer,true);
		inst.addTransformer(hiddenClassTransformer,true);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("\n=======================================================");
				System.out.println("TamiFlex Play-In Agent Version "+Agent.class.getPackage().getImplementationVersion());
                int numSuccess = replacer.numSuccess + HiddenClassLoader.numSuccess;
                int numInvoked = replacer.numInvoked + HiddenClassLoader.numInvoked;
				System.out.println("Replaced "+numSuccess+" out of "+numInvoked+" classes.");
				System.out.println("=======================================================");
			}
		});
		
		System.out.println("=======================================================");

        // Transform already loaded classes
		for (Class<?> c : inst.getAllLoadedClasses()) {
			if (inst.isModifiableClass(c)) {
				inst.retransformClasses(c);
			} else if(verbose) {
				// (In order) Cannot modify Primitive classes, Arrays, classes loaded by the bootstrap class loader, Core Java classes
                // Other than those on encountering an unmodifiable class send a warning
				if (!c.isPrimitive() && !c.isArray() && (c.getPackage()==null || !c.getPackage().getName().startsWith("java.lang"))){
                    // Cannot modify some synthetic classes too
                    if (c.isSynthetic()) {
                        System.err.println("WARNING: Cannot replace (unmodifiable) SYNTHETIC class "+c.getName());
                    } else {
                        System.err.println("WARNING: Cannot replace (unmodifiable) NON-SYNTHETIC class "+c.getName());
                    }
				}
			}
		}
        inst.removeTransformer(hiddenClassTransformer);

        // Classes loaded further down the line will be modified via ClassReplacer's transform() method
	}
	
	private static void loadProperties() {
		String propFileName = "pia.properties";
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
			if(props.get("dontNormalize").equals("true"))
				Hasher.dontNormalize();
			if(props.get("verbose").equals("true"))
				verbose = true;
			if(props.containsKey("inDir"))
				inPath = (String) props.get("inDir"); 

		} catch (IOException e) {
			throw new InternalError("Error loading default properties file: "+e.getMessage()); 
		}		
	}
	
	//COPIED from POA
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

	
	public static void main(String[] args) {
		usage();
	}

	private static void usage() {
		System.out.println("============================================================");
		System.out.println("TamiFlex Play-In Agent Version "+Agent.class.getPackage().getImplementationVersion());
		System.out.println(DISCLAIMER);
		System.out.println("============================================================");
		System.exit(1);
	}

	private static void appendRtJarToBootClassPath(Instrumentation inst) throws URISyntaxException, IOException {
		URL locationOfAgent = Agent.class.getResource("/de/bodden/tamiflex/playin/rt/HiddenClassLoader.class");
		if(locationOfAgent==null) {
			System.err.println("Support library for loading hidden classes not found on classpath.");
			System.exit(1);
		}
		agentJarFilePath = locationOfAgent.getPath().substring(0, locationOfAgent.getPath().indexOf("!"));		
		URI uri = new URI(agentJarFilePath);
		JarFile jarFile = new JarFile(new File(uri));
		inst.appendToBootstrapClassLoaderSearch(jarFile);
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
}
