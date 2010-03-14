package de.bodden.tamiflex.reflectionview.views;

public class ReflectionViewContentInserter {
	
	private final TraceFileNode root;
	private final CategoryNode classForNameNode;
	private final CategoryNode classNewInstanceNode;
	private final CategoryNode methodInvokeNode;
	private final CategoryNode constructorNewInstanceNode;
	private final ReflectionView reflectionView;

	
	public ReflectionViewContentInserter(TraceFileNode root, ReflectionView container) {
		this.reflectionView = container;
		classForNameNode = new CategoryNode("Class.forName");
		root.addChild(classForNameNode);
		classNewInstanceNode = new CategoryNode("Class.newInstance");
		root.addChild(classNewInstanceNode);
		methodInvokeNode = new CategoryNode("Method.invoke");
		root.addChild(methodInvokeNode);
		constructorNewInstanceNode = new CategoryNode("Constructor.newInstance");
		root.addChild(constructorNewInstanceNode);
		this.root = root;
	}
	
	public void insertFromTraceFileLine(String line) {
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
		if(kind.equals("Class.forName")) {
			TreeParent sourceMethodNode;
			MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(root.getProject());
			if((sourceMethodNode=(TreeParent) classForNameNode.childFor(ambMethodNode.getName()))==null) {
				classForNameNode.addChild(sourceMethodNode = ambMethodNode);
			}
			sourceMethodNode.addChild(new ClassNode(target));
		} else if(kind.equals("Class.newInstance")) {
			TreeParent sourceMethodNode;
			MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(root.getProject());
			if((sourceMethodNode=(TreeParent) classNewInstanceNode.childFor(ambMethodNode.getName()))==null) {
				classNewInstanceNode.addChild(sourceMethodNode = ambMethodNode);
			}
			sourceMethodNode.addChild(new ClassNode(target));
		} else if(kind.equals("Method.invoke")) {
			TreeParent sourceMethodNode;
			MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(root.getProject());
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
			MethodNode ambMethodNode = new AmbiguousMethodNode(className,methodName,lineNumber).tryResolve(root.getProject());
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
	}
	

}
