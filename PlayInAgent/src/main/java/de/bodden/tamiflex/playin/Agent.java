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

import de.bodden.tamiflex.normalizer.Hasher;

/**
 * This agent registers a {@link ClassReplacer} as a class-file transformer. 
 */
public class Agent {
	
	public final static String PKGNAME = Agent.class.getPackage().getName().replace('.', '/');

	private static String inPath = "out";
	private static boolean verbose = false;

	public static void premain(String agentArgs, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException, URISyntaxException, IllegalClassFormatException {
		
		System.out.println("=======================================================");
		System.out.println("TamiFlex Play-In Agent Version "+Agent.class.getPackage().getImplementationVersion());
		loadProperties();
		
		final ClassReplacer replacer = new ClassReplacer(inPath,verbose);
		inst.addTransformer(replacer,true);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("\n=======================================================");
				System.out.println("TamiFlex Play-In Agent Version "+Agent.class.getPackage().getImplementationVersion());
				System.out.println("Replaced "+replacer.numSuccess+" out of "+replacer.numInvoked+" classes.");
				System.out.println("=======================================================");
			}
		});
		
		System.out.println("=======================================================");

		for (Class<?> c : inst.getAllLoadedClasses()) {
			if(inst.isModifiableClass(c)) {
				inst.retransformClasses(c);
			} else if(verbose) {
				//warn if there is a class that we cannot re-transform, except for classes that resemble primitive types,
				//arrays or are in java.lang
				if(!c.isPrimitive() && !c.isArray() && (c.getPackage()==null || !c.getPackage().getName().startsWith("java.lang"))){
					System.out.println("WARNING: Cannot replace class "+c.getName());
				}
			}
		}
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
