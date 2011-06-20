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
package de.bodden.tamiflex.views;


import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PartInitException;

import de.bodden.tamiflex.Activator;
import de.bodden.tamiflex.resolution.MethodResolver;



public class AmbiguousMethodNode extends MethodNode {

	protected int lineNumber;
	
	protected Set<IMethod> resolvedMethods;
	
	public AmbiguousMethodNode(String className, String methodName, int lineNumber) {
		super(className, methodName, createName(className, methodName, lineNumber));
		this.lineNumber = lineNumber;
	}

	public static String createName(String className, String methodName,
			int lineNumber) {
		return className.replace('$','.')+"."+methodName+" (at line "+lineNumber+")";
	}
	
	public MethodNode tryResolve(IProject project) {
		resolvedMethods = MethodResolver.findMatchingMethods(className, methodName, project, lineNumber);
	
		if(resolvedMethods.size()==1) {
			IMethod method = resolvedMethods.iterator().next();
			String[] paramSigs = method.getParameterTypes();
			String methodSig = "";
			int i=1;
			for(String sig : paramSigs) {
				methodSig += Signature.toString(sig);
				if(i<paramSigs.length) {
					methodSig += ",";
				}
				i++;
			}
			methodSig = "("+methodSig+")";					
			return new ResolvedMethodNode(className, methodName, methodSig);
		} else{			
			return this;
		}
	}
	
	@Override
	public void handleDoubleClick() {
		if(resolvedMethods==null) {
			tryResolve(getProject()).handleDoubleClick();
		} else {
			if(resolvedMethods.isEmpty()) {
				MessageDialog.openError(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Method not found", "Unable to find method/constructor with name"+methodName+" in class "+className+" on classpath of project "+getProject().getName()+".");
			} else if(resolvedMethods.size()==1) {
				try {
					JavaUI.openInEditor(resolvedMethods.iterator().next());
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			} else {
				//multiple methods
				MessageDialog.openInformation(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Method ambiguous", "There are multiple methods/constructors with name"+methodName+" in class "+className+
						" on classpath of project "+getProject().getName()+". We tried to identify the right method by line number but failed.");
				try {
					JavaUI.openInEditor(resolvedMethods.iterator().next().getDeclaringType());
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
