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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;

import de.bodden.tamiflex.Activator;

public class FieldNode extends TreeObject {

	protected final String fieldName;
	protected final String className;

	public FieldNode(String className, String fieldName, String fieldType) {
		super(fieldType+" "+className.replace('$','.')+"."+fieldName, Kind.FIELD);
		this.className = className;
		this.fieldName = fieldName;
	}
	
	@Override
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PUBLIC);
	}
	
	@Override
	public void handleDoubleClick() {
		TreeObject parent = this.parent;
		while(parent.getParent()!=INVISIBLE_ROOT_NODE) {
			parent = parent.getParent();
		}
		IProject project = ((TreeParent)parent).getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IType type = javaProject.findType(className, (IProgressMonitor) null);
			if(type==null) {
				MessageDialog.openError(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Class not found", "Unable to find class "+name+" on classpath of project "+project.getName()+".");
			} else {
				IField field = type.getField(fieldName);
				JavaUI.openInEditor(field);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

}
