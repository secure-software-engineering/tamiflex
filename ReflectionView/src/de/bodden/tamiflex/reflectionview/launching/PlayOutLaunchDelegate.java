package de.bodden.tamiflex.reflectionview.launching;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.osgi.framework.Bundle;

import de.bodden.tamiflex.reflectionview.Activator;


public class PlayOutLaunchDelegate extends JavaLaunchDelegate {
	
	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		StringBuilder vmArguments = new StringBuilder(super.getVMArguments(configuration));
		
		vmArguments.append(" -javaagent:");
		vmArguments.append(getAgentJarPath());		
		vmArguments.append("=");
		vmArguments.append(getArgs(configuration));		
		
		return vmArguments.toString();
	}

	private String getArgs(ILaunchConfiguration configuration) {
		StringBuilder args = new StringBuilder();
		boolean toFolder = false;
		String outFolder = "";
		boolean count = false;
		boolean verbose = false;
		try {
			 toFolder = configuration.getAttribute(PlayOutLaunchConstants.WRITE_TO_FOLDER, false);
			 outFolder = configuration.getAttribute(PlayOutLaunchConstants.OUT_FOLDER_PATH, "");
			 count = configuration.getAttribute(PlayOutLaunchConstants.COUNT, false);
			 verbose = configuration.getAttribute(PlayOutLaunchConstants.VERBOSE, false);
		} catch (CoreException e) {
		}
		if(count) args.append("count,");
		if(verbose) args.append("verbose,");
		if(toFolder) {
			String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().makeAbsolute().toOSString();
			args.append(root);
			args.append(outFolder);
		}
		return args.toString();
	}
	
	private static String getAgentJarPath() {

		StringBuffer cpath = new StringBuffer();

		// This returns the bundle with the highest version or null if none
		// found
		// - for Eclipse 3.0 compatibility
		Bundle pluginBundle = Platform.getBundle(Activator.PLUGIN_ID);

		String pluginLoc = null;
		// 3.0 using bundles instead of plugin descriptors
		if (pluginBundle != null) {
			URL installLoc = pluginBundle.getEntry("/"); //$NON-NLS-1$
			URL resolved = null;
			try {
				resolved = FileLocator.resolve(installLoc);
				pluginLoc = resolved.toExternalForm();
			} catch (IOException e) {
			}
		}
		if (pluginLoc != null) {
			if (pluginLoc.startsWith("file:")) { //$NON-NLS-1$
				cpath.append(pluginLoc.substring("file:".length())); //$NON-NLS-1$
				cpath.append("lib/poa.jar"); //$NON-NLS-1$
			}
		}

		// Verify that the file actually exists at the plugins location
		// derived above. If not then it might be because we are inside
		// a runtime workbench. Check under the workspace directory.
		if (new File(cpath.toString()).exists()) {
			// File does exist under the plugins directory
			return cpath.toString();
		} else {
			throw new InternalError("File "+cpath+" does not exist.");
		}
	}

}
