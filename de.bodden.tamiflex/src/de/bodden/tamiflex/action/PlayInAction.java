package de.bodden.tamiflex.action;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class PlayInAction implements IWorkbenchWindowActionDelegate {

	protected IWorkbenchWindow window;

	@Override
	public void run(IAction action) {
		DebugUITools.openLaunchConfigurationDialogOnGroup(
				window.getShell(), new StructuredSelection(),
				"de.bodden.tamiflex.launchGroups.playin");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
