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
package de.bodden.tamiflex.launching;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.osgi.framework.Bundle;

import de.bodden.tamiflex.Activator;
import de.bodden.tamiflex.views.OnlineMonitorNode;
import de.bodden.tamiflex.views.ReflectionView;
import de.bodden.tamiflex.views.ReflectionViewContentInserter;
import de.bodden.tamiflex.views.TreeParent;

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
	
	public static String[] openSocketAndUpdateEnvironment(
			ILaunchConfiguration configuration, String[] environment) throws CoreException {
		String hostNameAndPort = LaunchUtil.openSocket(configuration);
		if(environment==null) environment = new String[0];
		String[] newEnv = new String[environment.length+1];
		System.arraycopy(environment, 0, newEnv, 1, environment.length);
		newEnv[0] = "TAMIFLEX_ECLIPSE="+hostNameAndPort;
		return newEnv;
	}
	
	public static String appendAgentArgs(String otherArgs, boolean playIn) {
		StringBuilder vmArguments = new StringBuilder(otherArgs);
		if(!otherArgs.isEmpty()) vmArguments.append(" "); //make sure to separate our arguments from any others
		
		vmArguments.append("-javaagent:");
		String localPath = playIn ? "lib/pia.jar" : "lib/poa.jar";
		vmArguments.append(LaunchUtil.getAgentJarPath(localPath));

		return vmArguments.toString();
	}
	
	private static String openSocket(final ILaunchConfiguration configuration) {
		final ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(0);
			String host = serverSocket.getInetAddress().getHostAddress();
			int port = serverSocket.getLocalPort();

			String projectName;
			try {
				projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
			} catch (CoreException e1) {
				e1.printStackTrace();
				projectName = "";
			}
			final IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(projectName);
			
			new Thread("SocketListener") {

				private ReflectionView reflView;
				@Override
				public void run() {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							try {
							IWorkbenchPage activePage = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
								activePage.showView(ReflectionView.ID);
								reflView = (ReflectionView) activePage.findView(ReflectionView.ID);
							} catch (PartInitException e) {
								e.printStackTrace();
							}
						}
					});
					TreeParent root = reflView.getContentProvider().getRoot();
					OnlineMonitorNode socketRoot;
					String rootLabel = configuration.getName()+" - "+new Date().toString();
					socketRoot = new OnlineMonitorNode(project, rootLabel);
					root.addChild(socketRoot);
					
					Socket socket=null;
					InputStream is=null;
					final ReflectionViewContentInserter contentInserter =
						new ReflectionViewContentInserter(socketRoot, reflView);
					try {
						socket = serverSocket.accept();
						is = socket.getInputStream();
						BufferedReader r = new BufferedReader(new InputStreamReader(is));
						
						String line = r.readLine();
						if(line!=null) {
							//POA first sends absolute path of log file; this we then register with the LogFileDatabase 
							File logFile = new File(line);
							Activator.getDefault().getLogFileDatabase().registerLogFile(project, logFile);
							
							while((line=r.readLine())!=null) {
								contentInserter.insertFromTraceFileLine(line);
								Display.getDefault().asyncExec(new Runnable() {			
									public void run() {
										reflView.refresh();
									}
								});
							}
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							if(socket!=null) socket.close();
							if(is!=null) is.close();
							if(serverSocket!=null) serverSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						contentInserter.removeUnusedNodes();
					}
				}
			}.start();
			//allow other thread to get to the point where we
			//accept connections on the server socket before we proceed here
			Thread.yield();
			
			return host + ":" +port;
		} catch (IOException e) {
			throw new RuntimeException("Problem creating server socket",e);
		} 
	}
	
	public static String getAgentJarPath(String localPath) {

		StringBuffer cpath = new StringBuffer();

		// This returns the bundle with the highest version or null if none
		// found
		// - for Eclipse 3.0 compatibility
		Bundle pluginBundle = Platform.getBundle(Activator.PLUGIN_ID);

		URL resolved = null;
		// 3.0 using bundles instead of plugin descriptors
		if (pluginBundle != null) {
			URL installLoc = pluginBundle.getEntry("/"); //$NON-NLS-1$
			try {
				resolved = FileLocator.resolve(installLoc);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (resolved != null) {
			if(resolved.getProtocol().equals("file")) {
				try {
					cpath.append(new File(resolved.toURI()).getAbsolutePath());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				cpath.append(File.separatorChar);
				cpath.append(localPath);
			} else {
				throw new RuntimeException("Cannot handle protocol "+resolved.getProtocol());
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
