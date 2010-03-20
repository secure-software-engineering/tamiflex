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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public class OnlineMonitorNode extends TreeParent {

	private final IProject project;

	public OnlineMonitorNode(IProject project, String label) {
		super(label,TreeObject.Kind.TRACEFILE);
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}
	
	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FILE);
	}

	@Override
	public void handleDoubleClick() {
	}
}
