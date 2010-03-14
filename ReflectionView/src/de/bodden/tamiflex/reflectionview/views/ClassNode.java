package de.bodden.tamiflex.reflectionview.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;

import de.bodden.tamiflex.reflectionview.Activator;



public class ClassNode extends TreeObject {

	public ClassNode(String name) {
		super(name, Kind.CLASS);
	}
	
	@Override
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
	}
	
	@Override
	public void handleDoubleClick() {
		TreeObject parent = this.parent;
		while(!(parent instanceof TraceFileNode)) {
			parent = parent.getParent();
		}
		IProject project = ((TraceFileNode)parent).getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IType type = javaProject.findType(name, (IProgressMonitor) null);
			if(type==null) {
				MessageDialog.openError(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Class not found", "Unable to find class "+name+" on classpath of project "+project.getName()+".");
			} else {
				JavaUI.openInEditor(type);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

}
