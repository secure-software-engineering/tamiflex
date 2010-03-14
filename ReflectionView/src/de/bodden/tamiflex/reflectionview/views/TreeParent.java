/**
 * 
 */
package de.bodden.tamiflex.reflectionview.views;

import java.util.ArrayList;

public abstract class TreeParent extends TreeObject {
	private ArrayList<TreeObject> children;
	public TreeParent(String name, TreeObject.Kind kind) {
		super(name,kind);
		children = new ArrayList<TreeObject>();
	}
	public void addChild(TreeObject child) {
		children.add(child);
		child.setParent(this);
	}
	public void removeChild(TreeObject child) {
		children.remove(child);
		child.setParent(null);
	}
	public TreeObject [] getChildren() {
		return (TreeObject [])children.toArray(new TreeObject[children.size()]);
	}
	public boolean hasChildren() {
		return children.size()>0;
	}
}