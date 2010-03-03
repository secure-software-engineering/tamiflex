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

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URISyntaxException;

/**
 * This agent registers a {@link ClassReplacer} as a class-file transformer. 
 */
public class Agent {
	
	public final static String PKGNAME = Agent.class.getPackage().getName().replace('.', '/');

	public static void premain(String agentArgs, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException, URISyntaxException, IllegalClassFormatException {
		if(agentArgs==null) agentArgs = "";
		boolean dontWarn = false;
		if(agentArgs.startsWith("dontwarn,")) {
			dontWarn = true;
			agentArgs = agentArgs.substring("dontwarn,".length());
		}
		if(agentArgs.equals("")) usage();
		inst.addTransformer(new ClassReplacer(agentArgs,dontWarn),true);					
		
		for (Class<?> c : inst.getAllLoadedClasses()) {
			if(inst.isModifiableClass(c)) {
				inst.retransformClasses(c);
			} else if(!dontWarn) {
				//warn if there is a class that we cannot re-transform, except for classes that resemble primitive types,
				//arrays or are in java.lang
				if(!c.isPrimitive() && !c.isArray() && (c.getPackage()==null || !c.getPackage().getName().startsWith("java.lang"))){
					System.err.println("WARNING: Cannot replace class "+c.getName());
				}
			}
		}
	}

	private static void usage() {
		System.out.println("This agent accepts the following options:");
		System.out.println("[dontwarn,]:<path>");
		System.out.println();
		System.out.println("If 'dontwarn' is given, then the replace agent won't issue a warning when a ");
		System.out.println("class cannot be found.");
		System.out.println("");
		System.out.println("For instance, the following command will cause the agent to dump class files into");
		System.out.println("the directory /tmp/out:");
		System.out.println("java -javaagent:agent.jar=/tmp/out ...");
		System.exit(1);
	}
}
