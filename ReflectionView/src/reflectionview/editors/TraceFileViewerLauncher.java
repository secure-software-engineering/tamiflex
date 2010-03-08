package reflectionview.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorLauncher;
import org.eclipse.ui.IWorkbenchPage;

import reflectionview.Activator;
import reflectionview.views.ReflectionView;

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
