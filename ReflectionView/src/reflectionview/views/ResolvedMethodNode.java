package reflectionview.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PartInitException;

import reflectionview.Activator;


public class ResolvedMethodNode extends MethodNode {

	private final String signature;

	public ResolvedMethodNode(String className, String methodName, String signature) {
		super(className, methodName, className.replace('$','.')+"."+methodName+signature);
		this.signature = signature;
	}
	
	@Override
	public void handleDoubleClick() {
		IProject project = getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IType type = javaProject.findType(className, (IProgressMonitor) null);
			
			if(type==null) {
				MessageDialog.openError(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Class not found", "Unable to find class "+className+" on classpath of project "+project.getName()+".");
			} else {
				String methodOrConstructorName = methodName.equals("<init>") ? type.getElementName() : methodName;
				IMethod method = type.getMethod(methodOrConstructorName, eclipseSignatures(signature));
				if(method==null) {
					MessageDialog.openError(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
							"Method not found", "Unable to find method/constructor "+method+signature+" in class "+className+" on classpath of project "+project.getName()+".");
				} else {
					JavaUI.openInEditor(method);
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	private static String[] eclipseSignatures(String signature) {
		String sig = signature.replace("(", "");
		sig = sig.replace(")","");
		if(sig.length()==0) return new String[0];
		String[] qualifiedNames = sig.split(",");
		String[] res = new String[qualifiedNames.length];
		int i = 0;
		for (String string : qualifiedNames) {
			res[i] = Signature.createTypeSignature(string, true);
			i++;
		}
		return res;
	}

}
