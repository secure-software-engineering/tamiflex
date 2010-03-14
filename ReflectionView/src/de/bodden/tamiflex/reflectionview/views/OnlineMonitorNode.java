package de.bodden.tamiflex.reflectionview.views;

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
