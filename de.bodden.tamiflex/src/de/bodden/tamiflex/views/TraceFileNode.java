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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public class TraceFileNode extends TreeParent {

	private final IPath path;

	public TraceFileNode(IPath file) {
		super(file.toString(),TreeObject.Kind.TRACEFILE);
		this.path = file;
	}

	public IPath getFile() {
		return path;
	}
	
	public IProject getProject() {
		IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		return file.getProject();
	}
	
	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FILE);
	}

	@Override
	public void handleDoubleClick() {
	}
}
