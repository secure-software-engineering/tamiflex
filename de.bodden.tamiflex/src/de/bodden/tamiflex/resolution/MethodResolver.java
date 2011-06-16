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
package de.bodden.tamiflex.resolution;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.ICodeAttribute;
import org.eclipse.jdt.core.util.ILineNumberAttribute;
import org.eclipse.jdt.core.util.IMethodInfo;

/**
 * Provides functionality to revolve methods by name and line number. 
 */
public class MethodResolver {

	protected static Set<IMethod> resolvedMethods;
	
	/**
	 * Tries to resolve an {@link IMethod} by name and line number.
	 * If the the information is insufficient then a set of all possibly matching methods is returned.
	 * @param containerClassName the name of the class declaring the method
	 * @param methodName the method's name
	 * @param containerProject the project containing the class declaring the method
	 * @param lineNumber potentially a line number contained within the method, may otherwise be -1
	 * @return
	 */
	public static Set<IMethod> findMatchingMethods(String containerClassName, String methodName, IProject containerProject, int lineNumber) {
		resolvedMethods = new HashSet<IMethod>();
		IJavaProject javaProject = JavaCore.create(containerProject);
		try {
			IType type = javaProject.findType(containerClassName, (IProgressMonitor) null);
			if(type!=null) {
				String methodOrConstructorName = methodName.equals("<init>") ? type.getElementName() : methodName;
				
				for(IMethod method: type.getMethods()) {
					if(method.getElementName().equals(methodOrConstructorName)) {
						resolvedMethods.add(method);
					}
				}
				
				if(resolvedMethods.size()>1) {
					//found multiple methods with this name; use line number info to find the right one						
					disambiguateMethodByLineNumber(containerClassName, methodName, containerProject, lineNumber);
				}
			}
			
			return resolvedMethods;
		} catch (JavaModelException e) {
			e.printStackTrace();
			return Collections.emptySet();
		}
	}
	
	private static void disambiguateMethodByLineNumber(String containerClassName, String methodName, IProject containerProject, int lineNumber) {
		IJavaProject javaProject = JavaCore.create(containerProject);
		try {
			IType type = javaProject.findType(containerClassName,(IProgressMonitor)null);
			ICompilationUnit compilationUnit = type.getCompilationUnit();
			
			if(compilationUnit!=null) {
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setSource(compilationUnit);				
				parser.setResolveBindings(true);
	
				CompilationUnit cu = (CompilationUnit) parser.createAST(null);
				final int linePosition = cu.getPosition(lineNumber, 0);
				
				cu.accept(new ASTVisitor() {
					
					public boolean visit(MethodDeclaration method) {
						if(method.getStartPosition()<=linePosition && method.getStartPosition()+method.getLength()>=linePosition) {
							//method is resolved
							resolvedMethods.clear();
							resolvedMethods.add((IMethod) method.resolveBinding().getJavaElement());
						}
						return false;
					}
					
				});		
			} else {
				IClassFile classFile = type.getClassFile();
				if(classFile!=null) {
					IClassFileReader reader = ToolFactory.createDefaultClassFileReader(
							classFile,
							IClassFileReader.METHOD_INFOS | IClassFileReader.METHOD_BODIES
					);
					for(IMethodInfo method : reader.getMethodInfos()) {
						String currMethodName = new String(method.getName());
						if(!currMethodName.equals(methodName)) continue;
						
						ICodeAttribute codeAttribute = method.getCodeAttribute();
						ILineNumberAttribute lineNumberAttribute = codeAttribute.getLineNumberAttribute();
						if(lineNumberAttribute!=null) {
							int[][] lineNumberTable = lineNumberAttribute.getLineNumberTable();
							if(lineNumberTable!=null && lineNumberTable.length>0) {
								int startLine = Integer.MAX_VALUE;
								int endLine = 0;
								for (int[] entry : lineNumberTable) {
									int line = entry[1];
									startLine = Math.min(startLine, line);
									endLine = Math.max(endLine, line);
								}								
								if(startLine >= lineNumber && endLine <= lineNumber) {
									char[][] parameterTypes = Signature.getParameterTypes(method.getDescriptor());
									String[] parameterTypeNames = new String[parameterTypes.length];
									int i=0;
									for (char[] cs : parameterTypes) {
										parameterTypeNames[i] = new String(cs);										
										i++;
									}									
									IMethod iMethod = type.getMethod(currMethodName, parameterTypeNames);
									
									//method resolved
									resolvedMethods.clear();
									resolvedMethods.add(iMethod);
								}
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

}
