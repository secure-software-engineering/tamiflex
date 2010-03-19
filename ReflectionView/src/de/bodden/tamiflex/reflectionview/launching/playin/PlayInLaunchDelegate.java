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
package de.bodden.tamiflex.reflectionview.launching.playin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import de.bodden.tamiflex.reflectionview.launching.LaunchUtil;


public class PlayInLaunchDelegate extends JavaLaunchDelegate {
	
	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		StringBuilder vmArguments = new StringBuilder(super.getVMArguments(configuration));
		
		vmArguments.append("-javaagent:");
		vmArguments.append(LaunchUtil.getAgentJarPath("lib/pia.jar"));		
		vmArguments.append("=");
		vmArguments.append(getArgs(configuration));		

		return vmArguments.toString();
	}

	private String getArgs(final ILaunchConfiguration configuration) throws CoreException {
		StringBuilder args = new StringBuilder();
		String inFolder = "";
		boolean verbose = false;
		boolean dontNormalize = false;
		try {
			 inFolder = configuration.getAttribute(PlayInLaunchConstants.IN_FOLDER_PATH, "");
			 verbose = configuration.getAttribute(PlayInLaunchConstants.VERBOSE, false);
			 dontNormalize = configuration.getAttribute(PlayInLaunchConstants.DONT_NORMALIZE, false);
		} catch (CoreException e) {
		}
		if(dontNormalize) args.append("dontNormalize,");
		if(verbose) args.append("verbose,");
		String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().makeAbsolute().toOSString();
		args.append(root);
		args.append(inFolder);
		return args.toString();
	}

}
