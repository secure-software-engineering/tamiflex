package de.bodden.tamiflex.action;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class Compare implements IObjectActionDelegate {

	private IFile left;
	private IFile right;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		System.err.println("About to compare "+left+" to "+right);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		IStructuredSelection sl = (IStructuredSelection) selection;
		Iterator<?> iter = sl.iterator();
		left = (IFile) iter.next();
		right = (IFile) iter.next();
	}


}
