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
package de.bodden.tamiflex.reflectionview.launching.playout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import de.bodden.tamiflex.reflectionview.Activator;
import de.bodden.tamiflex.reflectionview.launching.LaunchUtil;
import de.bodden.tamiflex.reflectionview.views.OnlineMonitorNode;
import de.bodden.tamiflex.reflectionview.views.ReflectionView;
import de.bodden.tamiflex.reflectionview.views.ReflectionViewContentInserter;
import de.bodden.tamiflex.reflectionview.views.TreeParent;


public class PlayOutLaunchDelegate extends JavaLaunchDelegate {
	
	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		StringBuilder vmArguments = new StringBuilder(super.getVMArguments(configuration));
		
		vmArguments.append("-javaagent:");
		vmArguments.append(LaunchUtil.getAgentJarPath("lib/poa.jar"));		
		vmArguments.append("=");
		vmArguments.append(getArgs(configuration));		

		return vmArguments.toString();
	}

	private String getArgs(final ILaunchConfiguration configuration) throws CoreException {
		StringBuilder args = new StringBuilder();
		boolean toFolder = false;
		String outFolder = "";
		boolean count = false;
		boolean verbose = false;
		boolean dontDumpClasses = false;
		boolean dontNormalize = false;
		try {
			 toFolder = configuration.getAttribute(PlayOutLaunchConstants.WRITE_TO_FOLDER, false);
			 outFolder = configuration.getAttribute(PlayOutLaunchConstants.OUT_FOLDER_PATH, "");
			 count = configuration.getAttribute(PlayOutLaunchConstants.COUNT, false);
			 verbose = configuration.getAttribute(PlayOutLaunchConstants.VERBOSE, false);
			 dontDumpClasses = configuration.getAttribute(PlayOutLaunchConstants.DONT_DUMP_CLASSES, false);
			 dontNormalize = configuration.getAttribute(PlayOutLaunchConstants.DONT_NORMALIZE, false);
		} catch (CoreException e) {
		}
		if(dontDumpClasses) args.append("dontDumpClasses,");
		if(dontNormalize) args.append("dontNormalize,");
		if(count) args.append("count,");
		if(verbose) args.append("verbose,");
		if(toFolder) {
			String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().makeAbsolute().toOSString();
			args.append(root);
			args.append(outFolder);
		} else {
			//online monitoring
			args.append("socket,");
			final ServerSocket serverSocket;
			try {
				serverSocket = new ServerSocket(0);
				String host = serverSocket.getInetAddress().getHostAddress();
				int port = serverSocket.getLocalPort();
				args.append(host);
				args.append(":");
				args.append(port);
				
				String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
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
							
							String line;
							while((line=r.readLine())!=null) {
								contentInserter.insertFromTraceFileLine(line);
								Display.getDefault().asyncExec(new Runnable() {			
									public void run() {
										reflView.refresh();
									}
								});
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
			} catch (IOException e) {
				throw new RuntimeException("Problem creating server socket",e);
			} 
		}
		return args.toString();
	}

}
