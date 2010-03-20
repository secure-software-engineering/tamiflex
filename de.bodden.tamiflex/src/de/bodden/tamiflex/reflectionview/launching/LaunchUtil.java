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
package de.bodden.tamiflex.reflectionview.launching;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.osgi.framework.Bundle;

import de.bodden.tamiflex.reflectionview.Activator;

@SuppressWarnings("restriction")
public class LaunchUtil {
	
	public static class FolderSelectionDialog extends ContainerSelectionDialog {
		private final String SETTINGS_ID = IDebugUIConstants.PLUGIN_ID + ".SHARED_LAUNCH_CONFIGURATON_DIALOG"; //$NON-NLS-1$
		
		public FolderSelectionDialog(Shell parentShell, IContainer initialRoot, String message) {
			super(parentShell, initialRoot, true, message);
		}

		protected IDialogSettings getDialogBoundsSettings() {
			IDialogSettings settings = DebugUIPlugin.getDefault().getDialogSettings();
			IDialogSettings section = settings.getSection(SETTINGS_ID);
			if (section == null) {
				section = settings.addNewSection(SETTINGS_ID);
			} 
			return section;
		}
	}
	
	public static String getAgentJarPath(String localPath) {

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
				cpath.append(localPath); //$NON-NLS-1$
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
