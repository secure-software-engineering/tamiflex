/**
 * 
 */
package reflectionview.views;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;

import reflectionview.Activator;

public class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	private TreeParent invisibleRoot;
		private final IViewSite viewSite;
		private final ReflectionView reflectionView;

		public ViewContentProvider(IViewSite viewSite, ReflectionView reflectionView) {
			this.viewSite = viewSite;
			this.reflectionView = reflectionView;
		}

		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if (parent.equals(viewSite)) {
				if (invisibleRoot==null) initialize();
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			return ((TreeObject)child).getParent();
		}
		public Object [] getChildren(Object parent) {
			if(parent==null) return new Object[0];
			return ((TreeObject)parent).getChildren();
		}
		public boolean hasChildren(Object parent) {
			if(parent==null) return false;
			return ((TreeObject)parent).hasChildren();
		}

		public void initialize() {
			Set<IPath> traceFilePaths = reflectionView.getCurrentTraceFiles();
			for (IPath traceFilePath : traceFilePaths) {
				if(traceFilePath==null) continue;
				IPath relativePath = traceFilePath.makeRelativeTo(ResourcesPlugin.getWorkspace().getRoot().getLocation());
				
				if(invisibleRoot!=null) {
					if (invisibleRoot.hasChildren()) {
						for(TreeObject o: invisibleRoot.getChildren()) {
							TraceFileNode n = (TraceFileNode)o;
							if(n.getName().equals(relativePath.toString())) {
								invisibleRoot.removeChild(n);
								break;
							}					
						}
					}
				} else {
					invisibleRoot = new ResolvedMethodNode("","","");
				}
				TraceFileNode fileNode = new TraceFileNode(relativePath);
				invisibleRoot.addChild(fileNode);
				try {
					InputStream inputStream = traceFilePath.toFile().toURL().openStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					int lines = 0;
					
					CategoryNode classForNameNode = new CategoryNode("Class.forName");
					fileNode.addChild(classForNameNode);
					CategoryNode classNewInstanceNode = new CategoryNode("Class.newInstance");
					fileNode.addChild(classNewInstanceNode);
					CategoryNode methodInvokeNode = new CategoryNode("Method.invoke");
					fileNode.addChild(methodInvokeNode);
					CategoryNode constructorNewInstanceNode = new CategoryNode("Constructor.newInstance");
					fileNode.addChild(constructorNewInstanceNode);
					while((line=reader.readLine())!=null) {
						if(line.length()==0) continue;
						String[] portions = line.split(";");
						String kind = portions[0];
						String target = portions[1];
						String source = portions[2];
						
						String classNameDotMethodName = source.substring(0,source.indexOf('('));
						String className= classNameDotMethodName.substring(0, classNameDotMethodName.lastIndexOf('.'));
						if(reflectionView.isHideJREMethods()) {
							if(className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("com.sun.")) {
								continue;
							}						
						}
						
						
						String methodName= classNameDotMethodName.substring(classNameDotMethodName.lastIndexOf('.')+1);
						int lineNumber=-1;
						if(source.contains(":")) {
							lineNumber = Integer.parseInt(source.substring(source.indexOf(':')+1,source.indexOf(')')));
						}					
						if(kind.equals("Class.forName")) {
							TreeParent sourceMethodNode;
							MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(fileNode.getProject());
							if((sourceMethodNode=(TreeParent) classForNameNode.childFor(ambMethodNode.getName()))==null) {
								classForNameNode.addChild(sourceMethodNode = ambMethodNode);
							}
							sourceMethodNode.addChild(new ClassNode(target));
						} else if(kind.equals("Class.newInstance")) {
							TreeParent sourceMethodNode;
							MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(fileNode.getProject());
							if((sourceMethodNode=(TreeParent) classNewInstanceNode.childFor(ambMethodNode.getName()))==null) {
								classNewInstanceNode.addChild(sourceMethodNode = ambMethodNode);
							}
							sourceMethodNode.addChild(new ClassNode(target));
						} else if(kind.equals("Method.invoke")) {
							TreeParent sourceMethodNode;
							MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(fileNode.getProject());
							if((sourceMethodNode=(TreeParent) methodInvokeNode.childFor(ambMethodNode.getName()))==null) {
								methodInvokeNode.addChild(sourceMethodNode = ambMethodNode);
							}
							String targetClassName= target.substring(1,target.indexOf(':'));
							String targetSignature= target.substring(target.indexOf('('),target.length()-1);
							String targetMethodName= target.substring(0,target.length()-targetSignature.length());
							targetMethodName = targetMethodName.substring(targetMethodName.lastIndexOf(' ')+1);
							sourceMethodNode.addChild(new ResolvedMethodNode(targetClassName,targetMethodName,targetSignature));
						} else if(kind.equals("Constructor.newInstance")) {
							TreeParent sourceMethodNode;
							MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(fileNode.getProject());
							if((sourceMethodNode=(TreeParent) constructorNewInstanceNode.childFor(ambMethodNode.getName()))==null) {
								constructorNewInstanceNode.addChild(sourceMethodNode = ambMethodNode);
							}
							String targetClassName= target.substring(1,target.indexOf(':'));
							String targetSignature= target.substring(target.indexOf('('),target.length()-1);
							String targetMethodName= target.substring(0,(target.length()-targetSignature.length())-1);
							targetMethodName = targetMethodName.substring(targetMethodName.lastIndexOf(' ')+1);
							sourceMethodNode.addChild(new ResolvedMethodNode(targetClassName,targetMethodName,targetSignature));
						} else {
							throw new RuntimeException("Unknown kind: "+kind);
						}
						lines++;
					}
				} catch (FileNotFoundException e) {
					throw new RuntimeException("Trace file not found.",e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				for(TreeObject node: fileNode.getChildren()) {
					if(!((TreeParent)node).hasChildren()) {
						fileNode.removeChild(node);
					} 
				}
			}
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}