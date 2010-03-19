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
package de.bodden.tamiflex.reflectionview.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorLauncher;
import org.eclipse.ui.IWorkbenchPage;

import de.bodden.tamiflex.reflectionview.Activator;
import de.bodden.tamiflex.reflectionview.views.ReflectionView;


public class TraceFileViewerLauncher implements IEditorLauncher {

	public void open(IPath file) {
		try {
			IWorkbenchPage activePage = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
			activePage.showView(ReflectionView.ID);
			ReflectionView reflView = (ReflectionView) activePage.findView(ReflectionView.ID);
			reflView.addTraceFile(file);
			reflView.refresh();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
