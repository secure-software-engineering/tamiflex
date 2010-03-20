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
package de.bodden.tamiflex.reflectionview.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public class CategoryNode extends TreeParent {

	Map<String,TreeObject> children = new HashMap<String, TreeObject>();
	
	public CategoryNode(String name) {
		super(name,TreeObject.Kind.CATEGORY);
	}
	
	@Override
	public void addChild(TreeObject child) {
		if(children.containsKey(child.getName())) {
			throw new RuntimeException("node with name '"+child.getName()+"' exists already!");
		} else {
			children.put(child.getName(), child);
			child.setParent(this);
		}		
	}
	
	@Override
	public TreeObject[] getChildren() {
		return children.values().toArray(new TreeObject[0]);
	}
	
	public TreeObject childFor(String name) {
		return children.get(name);
	}
	
	@Override
	public boolean hasChildren() {
		return !children.isEmpty();
	}
	
	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER);
	}

	@Override
	public void handleDoubleClick() {
	}
}

