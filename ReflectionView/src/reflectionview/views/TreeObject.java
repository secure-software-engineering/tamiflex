/**
 * 
 */
package reflectionview.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;




public abstract class TreeObject {
	public enum Kind {CATEGORY, TRACEFILE, CLASS, METHOD};
	
	protected String name;
	protected TreeParent parent;
	protected Kind kind;
	
	public TreeObject(String name, Kind kind) {
		this.name = name;
		this.kind = kind;
	}
	public String getName() {
		return name;
	}
	public void setParent(TreeParent parent) {
		this.parent = parent;
	}
	public TreeParent getParent() {
		return parent;
	}
	public String toString() {
		return getName();
	}
	public Object[] getChildren() {
		return new Object[0];
	}
	public boolean hasChildren() {
		return false;
	}
	public abstract Image getImage();

	public abstract void handleDoubleClick();
	
	public IProject getProject() {
		TreeObject parent = this.parent;
		while(!(parent instanceof TraceFileNode)) {
			parent = parent.getParent();
		}
		return ((TraceFileNode)parent).getProject();
	}
}