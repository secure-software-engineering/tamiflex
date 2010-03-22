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
/**
 * 
 */
package de.bodden.tamiflex.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;




public abstract class TreeObject {

	public final static TreeParent INVISIBLE_ROOT_NODE = new ResolvedMethodNode("","","");

	public enum Kind {CATEGORY, TRACEFILE, ONLINEMONITOR, CLASS, METHOD};
	
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
		while(parent.getParent()!=INVISIBLE_ROOT_NODE) {
			parent = parent.getParent();
		}
		return ((TreeParent)parent).getProject();
	}
	
	public Kind getKind() {
		return kind;
	}
}
