package reflectionview.views;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;


public abstract class MethodNode extends TreeParent {

	protected final String className;
	protected final String methodName;

	public MethodNode(String className, String methodName,String label) {
		super(label, Kind.METHOD);
		this.className = className;
		this.methodName = methodName;
	}
	
	@Override
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
	}
	
}
