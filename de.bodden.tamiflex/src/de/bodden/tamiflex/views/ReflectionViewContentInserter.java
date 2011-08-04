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
package de.bodden.tamiflex.views;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class ReflectionViewContentInserter {
	
	private final TreeParent root;
	private final ReflectionView reflectionView;
	private final Map<String,CategoryNode> nameToNode = new HashMap<String, CategoryNode>();
	private final Pattern classNamePattern = Pattern.compile("^(([a-z0-9_])+.)*([A-Za-z0-9_])+$");
	
	public ReflectionViewContentInserter(TreeParent root, ReflectionView container) {
		this.reflectionView = container;
		this.root = root;
	}
	
	private CategoryNode insertNodeIfNecessary(String category) {
		CategoryNode node = nameToNode.get(category);
		if(node==null) {
			node = new CategoryNode(category);
			root.addChild(node);
			nameToNode.put(category, node);
		}		
		return node;
	}

	public void insertFromTraceFileLine(String line) {
		try{
			if(line.length()==0) return;
			String[] portions = line.split(";");
			String kind = portions[0];
			String target = portions[1];
			String source = portions[2];
			
			String classNameDotMethodName = source;
			String className= classNameDotMethodName.substring(0, classNameDotMethodName.lastIndexOf('.'));
			if(reflectionView.isHideJREMethods()) {
				if(className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("com.sun.")) {
					return;
				}						
			}		
			
			String methodName= classNameDotMethodName.substring(classNameDotMethodName.lastIndexOf('.')+1);
			int lineNumber=-1;
			if(portions.length>3) {
				lineNumber = Integer.parseInt(portions[3]);
			}					
			
			CategoryNode categoryNode = insertNodeIfNecessary(kind);
			TreeParent sourceMethodNode;
			MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(root.getProject());
			if((sourceMethodNode=(TreeParent) categoryNode.childFor(ambMethodNode.getName()))==null) {
				categoryNode.addChild(sourceMethodNode = ambMethodNode);
			}
			
			if(classNamePattern.matcher(target).matches()) {
				//target is class
				sourceMethodNode.addChild(new ClassNode(target));
			} else if(target.contains("(")) {
				//target is method
				String targetClassName= target.substring(1,target.indexOf(':'));
				String targetSignature= target.substring(target.indexOf('('),target.length()-1);
				String targetMethodName= target.substring(0,target.length()-targetSignature.length()-1);
				targetMethodName = targetMethodName.substring(targetMethodName.lastIndexOf(' ')+1);
				sourceMethodNode.addChild(new ResolvedMethodNode(targetClassName,targetMethodName,targetSignature));
			} else {
				//FIXME: Failed to read line: Array.newInstance;<java.util.Formatter$FormatString[]>;java.util.Arrays.copyOf;2760;
				
				//target is field
				String targetClassName= target.substring(1,target.indexOf(':'));
				String targetFieldName= target.substring(target.lastIndexOf(' ')+1,target.lastIndexOf('>'));
				String targetType = target.substring(target.indexOf(' ')+1,target.lastIndexOf(' '));
				sourceMethodNode.addChild(new FieldNode(targetClassName,targetFieldName,targetType));
			}
		} catch(RuntimeException e) {
			System.err.println("Failed to read line: "+line);
			e.printStackTrace();
		}
	}
	
	public void removeUnusedNodes() {
		for(TreeObject node: root.getChildren()) {
			if(!((TreeParent)node).hasChildren()) {
				root.removeChild(node);
			} 
		}
	}
	

}
